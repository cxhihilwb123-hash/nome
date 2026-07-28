package chat.simplex.common.views.localauth

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatModel.controller
import dev.icerock.moko.resources.compose.stringResource
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.helpers.DatabaseUtils.ksSelfDestructPassword
import chat.simplex.common.views.helpers.DatabaseUtils.ksAppPassword
import chat.simplex.common.views.onboarding.OnboardingStage
import chat.simplex.common.platform.*
import chat.simplex.common.views.database.*
import chat.simplex.res.MR
import kotlinx.coroutines.delay
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

internal const val LOCAL_AUTH_FAILURE_THRESHOLD = 5
internal const val LOCAL_AUTH_BASE_BACKOFF_MS = 5_000L
internal const val LOCAL_AUTH_MAX_BACKOFF_MS = 5 * 60_000L

private val localAuthThrottleProcessLock = Any()

internal data class LocalAuthThrottleState(
  val failedAttempts: Int = 0,
  val retryAfterEpochMs: Long = 0L,
)

internal enum class LocalAuthCredentialMatch {
  APP_PASSCODE,
  SELF_DESTRUCT_PASSCODE,
}

internal sealed class LocalAuthAttempt(open val state: LocalAuthThrottleState) {
  data class Accepted(
    val credential: LocalAuthCredentialMatch,
    override val state: LocalAuthThrottleState,
  ) : LocalAuthAttempt(state)

  data class Rejected(override val state: LocalAuthThrottleState) : LocalAuthAttempt(state)

  data class Throttled(override val state: LocalAuthThrottleState) : LocalAuthAttempt(state)
}

/**
 * Pure policy for the app and self-destruct passcodes. The credential verifier is deliberately a
 * callback so a throttled attempt returns before either credential is read or compared.
 */
internal object LocalAuthThrottlePolicy {
  fun evaluate(
    persistedState: LocalAuthThrottleState,
    nowEpochMs: Long,
    verifyCredential: () -> LocalAuthCredentialMatch?,
  ): LocalAuthAttempt {
    val state = normalize(persistedState, nowEpochMs)
    if (state.retryAfterEpochMs > nowEpochMs) {
      return LocalAuthAttempt.Throttled(state)
    }

    val credential = verifyCredential()
    if (credential != null) {
      return LocalAuthAttempt.Accepted(credential, LocalAuthThrottleState())
    }

    val failedAttempts =
      if (state.failedAttempts == Int.MAX_VALUE) Int.MAX_VALUE else state.failedAttempts + 1
    val delayMs = backoffMs(failedAttempts)
    val retryAfterEpochMs =
      if (delayMs == 0L) 0L else safeAdd(nowEpochMs, delayMs)
    return LocalAuthAttempt.Rejected(LocalAuthThrottleState(failedAttempts, retryAfterEpochMs))
  }

  fun blockedUntil(persistedState: LocalAuthThrottleState, nowEpochMs: Long): Long =
    normalize(persistedState, nowEpochMs).retryAfterEpochMs

  internal fun backoffMs(failedAttempts: Int): Long {
    if (failedAttempts < LOCAL_AUTH_FAILURE_THRESHOLD) return 0L
    val exponent = (failedAttempts - LOCAL_AUTH_FAILURE_THRESHOLD).coerceAtMost(30)
    var backoff = LOCAL_AUTH_BASE_BACKOFF_MS
    repeat(exponent) {
      if (backoff >= LOCAL_AUTH_MAX_BACKOFF_MS / 2) return LOCAL_AUTH_MAX_BACKOFF_MS
      backoff *= 2
    }
    return backoff.coerceAtMost(LOCAL_AUTH_MAX_BACKOFF_MS)
  }

  private fun normalize(state: LocalAuthThrottleState, nowEpochMs: Long): LocalAuthThrottleState {
    val failedAttempts = state.failedAttempts.coerceAtLeast(0)
    val retryAfter = state.retryAfterEpochMs.coerceAtLeast(0L)
    val boundedRetryAfter =
      when {
        retryAfter <= nowEpochMs -> 0L
        else -> minOf(retryAfter, safeAdd(nowEpochMs, LOCAL_AUTH_MAX_BACKOFF_MS))
      }
    return LocalAuthThrottleState(failedAttempts, boundedRetryAfter)
  }

  private fun safeAdd(value: Long, increment: Long): Long =
    if (value > Long.MAX_VALUE - increment) Long.MAX_VALUE else value + increment
}

/** Reads and writes the throttle through app preferences, so reconstruction retains the lockout. */
internal class PersistentLocalAuthThrottle(
  private val failedAttempts: SharedPreference<Int>,
  private val retryAfterEpochMs: SharedPreference<Long>,
  private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
  fun evaluate(verifyCredential: () -> LocalAuthCredentialMatch?): LocalAuthAttempt =
    synchronized(localAuthThrottleProcessLock) {
      val persistedState = readState()
      val attempt = LocalAuthThrottlePolicy.evaluate(persistedState, nowEpochMs(), verifyCredential)
      if (attempt.state != persistedState) {
        // Write the fail-closed field first. An interrupted failure cannot briefly expose an
        // unrestricted verifier, while an interrupted success can only retain a higher counter.
        retryAfterEpochMs.set(attempt.state.retryAfterEpochMs)
        failedAttempts.set(attempt.state.failedAttempts)
      }
      attempt
    }

  fun blockedUntil(): Long =
    synchronized(localAuthThrottleProcessLock) {
      LocalAuthThrottlePolicy.blockedUntil(readState(), nowEpochMs())
    }

  private fun readState(): LocalAuthThrottleState =
    LocalAuthThrottleState(failedAttempts.get(), retryAfterEpochMs.get())
}

private fun passcodesMatch(first: String, second: String): Boolean =
  MessageDigest.isEqual(
    first.toByteArray(StandardCharsets.UTF_8),
    second.toByteArray(StandardCharsets.UTF_8),
  )

internal fun <T : Any> requireSelfDestructDecoy(createdUser: T?): T =
  checkNotNull(createdUser) { "Unable to create the self-destruct decoy identity" }

/**
 * Establishes a durable barrier before the first suspension point of self-destruct. Normally the
 * IN_PROGRESS marker provides crash recovery. If that marker cannot be written, all three
 * credentials are synchronously erased instead; a partial erasure terminates the process rather
 * than continuing into an await/delay window with recoverable old data.
 */
internal fun establishSelfDestructCrashBarrier(
  persistInProgress: () -> Unit,
  eraseDatabaseCredential: () -> Unit,
  eraseAppCredential: () -> Unit,
  eraseSelfDestructCredential: () -> Unit,
  terminate: () -> Nothing,
) {
  try {
    persistInProgress()
    return
  } catch (_: Throwable) {
    val failures = mutableListOf<Throwable>()
    listOf(eraseDatabaseCredential, eraseAppCredential, eraseSelfDestructCredential).forEach { erase ->
      try {
        erase()
      } catch (e: Throwable) {
        failures += e
      }
    }
    if (failures.isNotEmpty()) terminate()
  }
}

@Composable
fun LocalAuthView(m: ChatModel, authRequest: LocalAuthRequest) {
  val passcode = remember { mutableStateOf("") }
  val allowToReact = rememberSaveable { mutableStateOf(true) }
  val throttle = remember(m.controller.appPrefs) {
    PersistentLocalAuthThrottle(
      m.controller.appPrefs.localAuthFailedAttempts,
      m.controller.appPrefs.localAuthRetryAfterEpochMs,
    )
  }
  var retryAfterEpochMs by rememberSaveable { mutableStateOf(throttle.blockedUntil()) }
  var failureFeedback by rememberSaveable { mutableStateOf<String?>(null) }
  val retryLaterText = stringResource(MR.strings.local_auth_retry_later)
  val incorrectPasscodeText = stringResource(MR.strings.incorrect_passcode)
  val verificationUnavailableText = stringResource(MR.strings.la_could_not_be_verified)

  LaunchedEffect(retryAfterEpochMs) {
    if (retryAfterEpochMs > 0L) {
      val remaining =
        (retryAfterEpochMs - System.currentTimeMillis()).coerceIn(0L, LOCAL_AUTH_MAX_BACKOFF_MS)
      if (remaining > 0L) delay(remaining)
      retryAfterEpochMs = 0L
    }
  }

  if (!allowToReact.value) {
    BackHandler {
      // do nothing until submit action finishes to prevent concurrent removing of storage
    }
  }
  PasscodeView(
    passcode,
    authRequest.title ?: stringResource(MR.strings.la_enter_app_passcode),
    if (retryAfterEpochMs > 0L) retryLaterText else failureFeedback ?: authRequest.reason,
    stringResource(MR.strings.submit_passcode),
    submitEnabled = { retryAfterEpochMs == 0L },
    buttonsEnabled = allowToReact,
    submit = submit@{
      if (!allowToReact.value || retryAfterEpochMs > 0L) return@submit
      allowToReact.value = false
      var selfDestructPassword: String? = null
      val attempt =
        try {
          throttle.evaluate {
            // This block is never invoked while throttled. Both comparisons use the same verifier
            // and neither result is surfaced to the UI.
            selfDestructPassword =
              if (authRequest.selfDestruct) ksSelfDestructPassword.get() else null
            val selfDestructMatches =
              selfDestructPassword?.let { passcodesMatch(passcode.value, it) } == true
            val appPasscodeMatches = passcodesMatch(passcode.value, authRequest.password)
            when {
              selfDestructMatches -> LocalAuthCredentialMatch.SELF_DESTRUCT_PASSCODE
              appPasscodeMatches -> LocalAuthCredentialMatch.APP_PASSCODE
              else -> null
            }
          }
        } catch (_: Exception) {
          // Credential storage failures are not guesses. Fail closed without incrementing the
          // throttle or logging credential/error payloads, but leave cancellation and retry usable.
          Log.e(TAG, "Unable to access local authentication credentials")
          passcode.value = ""
          allowToReact.value = true
          failureFeedback = verificationUnavailableText
          return@submit
        }

      when (attempt) {
        is LocalAuthAttempt.Accepted -> {
          failureFeedback = null
          retryAfterEpochMs = 0L
          when (attempt.credential) {
            LocalAuthCredentialMatch.SELF_DESTRUCT_PASSCODE -> {
              val password = selfDestructPassword
              if (password == null) {
                // Defensive fail-closed branch; the verifier cannot produce this match without it.
                allowToReact.value = true
                passcode.value = ""
                failureFeedback = incorrectPasscodeText
              } else {
                deleteStorageAndRestart(m, password) { result -> authRequest.completed(result) }
              }
            }
            LocalAuthCredentialMatch.APP_PASSCODE -> {
              if (authRequest.selfDestruct && selfDestructPassword != null && controller.getChatCtrl() == -1L) {
                initChatControllerOnStart()
              }
              authRequest.completed(LAResult.Success)
            }
          }
        }
        is LocalAuthAttempt.Rejected -> {
          passcode.value = ""
          allowToReact.value = true
          retryAfterEpochMs = attempt.state.retryAfterEpochMs
          failureFeedback =
            if (retryAfterEpochMs > 0L) null else incorrectPasscodeText
        }
        is LocalAuthAttempt.Throttled -> {
          // No credential was read or compared. Keep the prompt open with one generic message.
          passcode.value = ""
          allowToReact.value = true
          retryAfterEpochMs = attempt.state.retryAfterEpochMs
          failureFeedback = null
        }
      }
    },
    cancel = cancel@{
      if (!allowToReact.value) return@cancel
      allowToReact.value = false
      authRequest.completed(LAResult.Error(generalGetString(MR.strings.authentication_cancelled)))
    },
  )
}

private fun deleteStorageAndRestart(m: ChatModel, password: String, completed: (LAResult) -> Unit) {
  val completionSent = AtomicBoolean(false)
  fun completeOnce(result: LAResult) {
    if (completionSent.compareAndSet(false, true)) completed(result)
  }
  fun failClosed(cause: Throwable) {
    val failure = DatabaseUtils.markSelfDestructWipeIncomplete(cause)
    // If physical/key deletion itself failed, remove all unlock credentials as an independent
    // fail-closed barrier. Startup recovery will retry the full wipe when the marker is available.
    runCatching { DatabaseUtils.ksDatabasePassword.removeAndVerify() }.exceptionOrNull()?.let(failure::addSuppressed)
    runCatching { ksAppPassword.removeAndVerify() }.exceptionOrNull()?.let(failure::addSuppressed)
    runCatching { ksSelfDestructPassword.removeAndVerify() }.exceptionOrNull()?.let(failure::addSuppressed)
    Log.e(TAG, "Self-destruct cleanup did not complete; recovery marker retained")
    completeOnce(
      LAResult.Error(
        generalGetString(MR.strings.self_destruct_wipe_incomplete),
        fatalRecovery = true,
      )
    )
  }

  // This call is intentionally outside withLongRunningApi: it finishes synchronously before the
  // coroutine can be queued or reach any suspension point.
  establishSelfDestructCrashBarrier(
    persistInProgress = {
      DatabaseUtils.setSelfDestructWipeStateVerified(SelfDestructWipeState.IN_PROGRESS)
    },
    eraseDatabaseCredential = DatabaseUtils.ksDatabasePassword::removeAndVerify,
    eraseAppCredential = ksAppPassword::removeAndVerify,
    eraseSelfDestructCredential = ksSelfDestructPassword::removeAndVerify,
    terminate = ::terminateForUnsafeNetworkState,
  )

  withLongRunningApi {
    try {
      /** Waiting until [initChatController] finishes */
      while (m.ctrlInitInProgress.value) {
        delay(50)
      }
      if (m.chatRunning.value == true) {
        stopChatAsync(m)
      }
      val ctrl = m.controller.getChatCtrl()
      if (ctrl != null && ctrl != -1L) {
        /**
         * The following sequence can bring a user here:
         * the user opened the app, entered app passcode, went to background, returned back, entered self-destruct code.
         * In this case database should be closed to prevent possible situation when OS can deny database removal command
         * */
        chatCloseStore(ctrl)
      }
      deleteChatDatabaseFilesAndState()
      ksAppPassword.setAndVerify(password)
      ksSelfDestructPassword.removeAndVerify()
      ntfManager.cancelAllNotifications()
      val selfDestructPref = m.controller.appPrefs.selfDestruct
      val displayNamePref = m.controller.appPrefs.selfDestructDisplayName
      val displayName = displayNamePref.get()
      setPreferenceVerified(selfDestructPref, false, "self-destruct enabled")
      setPreferenceVerified(displayNamePref, null, "self-destruct display name")
      reinitChatController()
      check(m.currentUser.value == null) { "Fresh database unexpectedly contains an active user" }
      var profile: Profile? = null
      if (!displayName.isNullOrEmpty()) {
        profile = Profile(displayName = displayName, fullName = "", shortDescr = null)
      }
      val createdUser = requireSelfDestructDecoy(
        m.controller.apiCreateActiveUser(null, profile, pastTimestamp = true)
      )
      m.currentUser.value = createdUser
      setPreferenceVerified(
        m.controller.appPrefs.onboardingStage,
        OnboardingStage.OnboardingComplete,
        "onboarding state",
      )
      val onStarted: suspend () -> Unit = onStarted@{
        try {
          // Clear the durable marker only after the decoy controller has completed its configured
          // startup callback. A crash before here is recovered before any database is reopened.
          DatabaseUtils.setSelfDestructWipeStateVerified(SelfDestructWipeState.IDLE)
        } catch (e: Throwable) {
          failClosed(e)
          return@onStarted
        }
        try {
          ModalManager.closeAllModalsEverywhere()
          AlertManager.shared.hideAllAlerts()
          AlertManager.privacySensitive.hideAllAlerts()
        } catch (e: Throwable) {
          // UI cleanup is not part of the security transaction after the durable state is IDLE.
          Log.e(TAG, "Self-destruct completed but UI cleanup failed")
        }
        completeOnce(LAResult.Success)
      }
      m.controller.startChat(createdUser, onStarted)
    } catch (e: Throwable) {
      failClosed(e)
    }
  }
}

suspend fun reinitChatController() {
  chatModel.chatDbChanged.value = true
  chatModel.chatDbStatus.value = null
  try {
    initChatController(recoverInterruptedSelfDestruct = false)
  } catch (e: Exception) {
    Log.d(TAG, "initializeChat ${e.stackTraceToString()}")
    throw e
  } finally {
    chatModel.chatDbChanged.value = false
  }
}
