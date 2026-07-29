package chat.simplex.app.nome.smp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Log
import chat.simplex.app.BuildConfig
import chat.simplex.common.model.ChatController
import chat.simplex.common.model.UserNetworkInfo
import chat.simplex.common.model.UserNetworkType
import chat.simplex.common.platform.getWakeLock
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Debug-only probe for the stale Android SMP session observed on the Vivo V2047A.
 *
 * The probe never changes physical Wi-Fi. It reports a bounded logical NONE state to the
 * existing network-info API, waits briefly, and then reports the currently validated Android
 * network. A success return is diagnostic evidence only; real message delivery remains the pass
 * criterion.
 */
class NomeSmpLogicalNetworkReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (!BuildConfig.DEBUG) return
    when (intent.action) {
      ACTION_SCHEDULE -> schedule(context, intent.getLongExtra(EXTRA_DELAY_MS, DEFAULT_DELAY_MS))
      ACTION_RUN -> runAsync(context)
      ACTION_RECOVER -> recoverAsync(context)
      else -> writeStatus(context, "event=ignored reason=unknown_action", reset = false)
    }
  }

  private fun schedule(context: Context, requestedDelayMs: Long) {
    val delayMs = NomeSmpLogicalNetworkPolicy.clampDelay(requestedDelayMs)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE,
      Intent(context, NomeSmpLogicalNetworkReceiver::class.java)
        .setAction(ACTION_RUN)
        .setPackage(context.packageName),
      PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    alarmManager.setAndAllowWhileIdle(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime() + delayMs,
      pendingIntent,
    )
    writeStatus(context, "event=scheduled delay_ms=$delayMs", reset = true)
  }

  private fun runAsync(context: Context) {
    if (!isRunning.compareAndSet(false, true)) {
      writeStatus(context, "event=skipped reason=already_running", reset = false)
      return
    }
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      var releaseWakeLock: (() -> Unit)? = null
      try {
        releaseWakeLock = getWakeLock(WAKELOCK_TIMEOUT_MS)
        withTimeout(RECEIVER_WORK_TIMEOUT_MS) {
          val appContext = context.applicationContext
          if (isRecoveryPending(appContext)) {
            writeStatus(appContext, "event=run_redirected reason=recovery_pending", reset = false)
            runRecoveryAttempt(appContext)
          } else {
            runLogicalReconstruction(appContext)
          }
        }
      } catch (_: TimeoutCancellationException) {
        safeWriteStatus(context, "event=failed type=Timeout", reset = false)
        ensureRecoveryScheduledIfPending(context.applicationContext)
      } catch (e: Throwable) {
        safeWriteStatus(context, "event=failed type=${e.javaClass.simpleName}", reset = false)
        ensureRecoveryScheduledIfPending(context.applicationContext)
      } finally {
        try {
          releaseWakeLock?.invoke()
        } catch (e: Throwable) {
          safeWriteStatus(context, "event=wakelock_release_failed type=${e.javaClass.simpleName}", reset = false)
        } finally {
          isRunning.set(false)
          try {
            pendingResult.finish()
          } catch (e: Throwable) {
            Log.e(TAG, "Unable to finish debug diagnostic broadcast", e)
          }
        }
      }
    }
  }

  private fun recoverAsync(context: Context) {
    if (!isRunning.compareAndSet(false, true)) {
      writeStatus(context, "event=recovery_skipped reason=already_running", reset = false)
      scheduleRecovery(context.applicationContext, RECOVERY_RETRY_DELAY_MS)
      return
    }
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      var releaseWakeLock: (() -> Unit)? = null
      try {
        releaseWakeLock = getWakeLock(WAKELOCK_TIMEOUT_MS)
        withTimeout(RECEIVER_WORK_TIMEOUT_MS) {
          runRecoveryAttempt(context.applicationContext)
        }
      } catch (_: TimeoutCancellationException) {
        safeWriteStatus(context, "event=recovery_failed type=Timeout", reset = false)
        ensureRecoveryScheduledIfPending(context.applicationContext)
      } catch (e: Throwable) {
        safeWriteStatus(context, "event=recovery_failed type=${e.javaClass.simpleName}", reset = false)
        ensureRecoveryScheduledIfPending(context.applicationContext)
      } finally {
        try {
          releaseWakeLock?.invoke()
        } catch (e: Throwable) {
          safeWriteStatus(context, "event=wakelock_release_failed type=${e.javaClass.simpleName}", reset = false)
        } finally {
          isRunning.set(false)
          try {
            pendingResult.finish()
          } catch (e: Throwable) {
            Log.e(TAG, "Unable to finish debug recovery broadcast", e)
          }
        }
      }
    }
  }

  private suspend fun runLogicalReconstruction(context: Context) {
    val now = SystemClock.elapsedRealtime()
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastAttempt = prefs.getLong(PREF_LAST_ATTEMPT_MS, NO_ATTEMPT)
    if (!NomeSmpLogicalNetworkPolicy.cooldownElapsed(now, lastAttempt)) {
      writeStatus(context, "event=skipped reason=cooldown", reset = false)
      return
    }

    if (currentValidatedNetwork(context) == null) {
      writeStatus(context, "event=skipped reason=no_validated_network", reset = false)
      return
    }
    if (!ChatController.hasChatCtrl()) {
      writeStatus(context, "event=skipped reason=no_chat_controller", reset = false)
      return
    }

    prefs.edit().putLong(PREF_LAST_ATTEMPT_MS, now).apply()
    val noneInfo = UserNetworkInfo(UserNetworkType.NONE, online = false)
    writeStatus(context, "event=started", reset = false)
    val outcome = NomeSmpLogicalNetworkTransition<UserNetworkInfo>(
      waitForGap = { delay(LOGICAL_NONE_GAP_MS) },
    ).run(
      prepareRecovery = {
        prepareRecovery(context)
      },
      clearRecovery = { clearRecoveryState(context) },
      applyNone = {
        ChatController.apiSetNetworkInfo(noneInfo).also { applied ->
          if (applied) ChatController.chatModel.networkInfo.value = noneInfo
          writeStatus(context, "event=none_applied success=$applied", reset = false)
        }
      },
      currentValidated = { currentValidatedNetwork(context) },
      applyCurrent = { refreshedInfo ->
        ChatController.apiSetNetworkInfo(refreshedInfo).also { applied ->
          if (applied) ChatController.chatModel.networkInfo.value = refreshedInfo
          writeStatus(context, "event=current_applied success=$applied", reset = false)
        }
      },
    )
    when (outcome) {
      NomeSmpLogicalNetworkTransition.Outcome.PREPARATION_FAILED ->
        writeStatus(context, "event=completed result=preparation_failed", reset = false)
      NomeSmpLogicalNetworkTransition.Outcome.NONE_REJECTED ->
        writeStatus(context, "event=completed result=none_rejected", reset = false)
      NomeSmpLogicalNetworkTransition.Outcome.RESTORED ->
        writeStatus(context, "event=completed result=restored", reset = false)
      NomeSmpLogicalNetworkTransition.Outcome.RECOVERY_REQUIRED -> {
        writeStatus(context, "event=completed result=recovery_required", reset = false)
        scheduleRecovery(context, RECOVERY_RETRY_DELAY_MS)
      }
    }
  }

  private suspend fun runRecoveryAttempt(context: Context) {
    if (!isRecoveryPending(context)) {
      writeStatus(context, "event=recovery_skipped reason=not_pending", reset = false)
      cancelRecovery(context)
      return
    }
    if (!ChatController.hasChatCtrl()) {
      deferRecovery(context, "no_chat_controller")
      return
    }
    val refreshedInfo = currentValidatedNetwork(context)
    if (refreshedInfo == null) {
      deferRecovery(context, "no_validated_network")
      return
    }
    resetDeferredChecks(context)
    val attempt = nextApiRecoveryAttempt(context)
    val applied = ChatController.apiSetNetworkInfo(refreshedInfo)
    if (applied) {
      ChatController.chatModel.networkInfo.value = refreshedInfo
      clearRecoveryState(context)
      writeStatus(context, "event=recovery_completed success=true attempt=$attempt", reset = false)
    } else {
      writeStatus(context, "event=recovery_deferred reason=api_rejected attempt=$attempt", reset = false)
      scheduleNextApiRecovery(context, attempt)
    }
  }

  private fun ensureRecoveryScheduledIfPending(context: Context) {
    if (isRecoveryPending(context)) scheduleRecovery(context, RECOVERY_RETRY_DELAY_MS)
  }

  private fun prepareRecovery(context: Context): Boolean {
    val committed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(PREF_RECOVERY_PENDING, true)
      .putInt(PREF_RECOVERY_API_ATTEMPTS, 0)
      .putInt(PREF_RECOVERY_DEFERRED_CHECKS, 0)
      .commit()
    if (!committed) {
      safeWriteStatus(context, "event=preparation_failed reason=persistence", reset = false)
      return false
    }
    return try {
      scheduleRecovery(context, SAFETY_RECOVERY_DELAY_MS)
      true
    } catch (e: Throwable) {
      context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(PREF_RECOVERY_PENDING, false)
        .commit()
      safeWriteStatus(context, "event=preparation_failed reason=alarm type=${e.javaClass.simpleName}", reset = false)
      false
    }
  }

  private fun clearRecoveryState(context: Context) {
    val cleared = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(PREF_RECOVERY_PENDING, false)
      .putInt(PREF_RECOVERY_API_ATTEMPTS, 0)
      .putInt(PREF_RECOVERY_DEFERRED_CHECKS, 0)
      .commit()
    if (cleared) {
      cancelRecovery(context)
    } else {
      safeWriteStatus(context, "event=recovery_clear_failed reason=persistence", reset = false)
      scheduleRecovery(context, RECOVERY_LONG_BACKOFF_MS)
    }
  }

  private fun isRecoveryPending(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(PREF_RECOVERY_PENDING, false)

  private fun nextApiRecoveryAttempt(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val previous = prefs.getInt(PREF_RECOVERY_API_ATTEMPTS, 0)
    val next = NomeSmpLogicalNetworkPolicy.nextApiRecoveryAttempt(previous)
    prefs.edit().putInt(PREF_RECOVERY_API_ATTEMPTS, next).commit()
    return next
  }

  private fun deferRecovery(context: Context, reason: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val checks = NomeSmpLogicalNetworkPolicy.nextDeferredCheck(
      prefs.getInt(PREF_RECOVERY_DEFERRED_CHECKS, 0),
    )
    prefs.edit().putInt(PREF_RECOVERY_DEFERRED_CHECKS, checks).commit()
    val burstCheck = NomeSmpLogicalNetworkPolicy.deferredBurstCheck(checks)
    val delayMs = NomeSmpLogicalNetworkPolicy.deferredRetryDelay(checks)
    writeStatus(context, "event=recovery_deferred reason=$reason check=$burstCheck", reset = false)
    scheduleRecovery(context, delayMs)
  }

  private fun resetDeferredChecks(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putInt(PREF_RECOVERY_DEFERRED_CHECKS, 0)
      .commit()
  }

  private fun scheduleNextApiRecovery(context: Context, attempt: Int) {
    val delayMs = NomeSmpLogicalNetworkPolicy.apiRetryDelay(attempt)
    if (delayMs == RECOVERY_LONG_BACKOFF_MS) {
      writeStatus(context, "event=recovery_backoff reason=api_attempts_exhausted", reset = false)
    }
    scheduleRecovery(context, delayMs)
  }

  private fun scheduleRecovery(context: Context, delayMs: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setAndAllowWhileIdle(
      AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime() + delayMs,
      recoveryPendingIntent(context),
    )
  }

  private fun cancelRecovery(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(recoveryPendingIntent(context))
  }

  private fun recoveryPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context,
    RECOVERY_REQUEST_CODE,
    Intent(context, NomeSmpLogicalNetworkReceiver::class.java)
      .setAction(ACTION_RECOVER)
      .setPackage(context.packageName),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
  )

  private fun currentValidatedNetwork(context: Context): UserNetworkInfo? {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return null
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
      ?: return null
    val online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    if (!online) return null
    val type = when {
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> UserNetworkType.ETHERNET
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> UserNetworkType.WIFI
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> UserNetworkType.CELLULAR
      else -> UserNetworkType.OTHER
    }
    return UserNetworkInfo(type, online = true)
  }

  companion object {
    const val ACTION_SCHEDULE = "chat.simplex.app.NOME_SMP_LOGICAL_NETWORK_SCHEDULE"
    const val ACTION_RUN = "chat.simplex.app.NOME_SMP_LOGICAL_NETWORK_RUN"
    const val ACTION_RECOVER = "chat.simplex.app.NOME_SMP_LOGICAL_NETWORK_RECOVER"
    const val EXTRA_DELAY_MS = "delay_ms"

    private const val TAG = "NomeSmpLogicalNetwork"
    private const val STATUS_FILE = "nome_smp_logical_network_status.txt"
    private const val PREFS_NAME = "nome_smp_logical_network_diagnostic"
    private const val PREF_LAST_ATTEMPT_MS = "last_attempt_elapsed_ms"
    private const val PREF_RECOVERY_PENDING = "recovery_pending"
    private const val PREF_RECOVERY_API_ATTEMPTS = "recovery_api_attempts"
    private const val PREF_RECOVERY_DEFERRED_CHECKS = "recovery_deferred_checks"
    private const val NO_ATTEMPT = -1L
    private const val REQUEST_CODE = 65622
    private const val RECOVERY_REQUEST_CODE = 65623
    private const val DEFAULT_DELAY_MS = 120_000L
    private const val LOGICAL_NONE_GAP_MS = 3_000L
    private const val SAFETY_RECOVERY_DELAY_MS = 15_000L
    internal const val RECOVERY_RETRY_DELAY_MS = 10_000L
    internal const val RECOVERY_LONG_BACKOFF_MS = 60_000L
    private const val RECEIVER_WORK_TIMEOUT_MS = 8_000L
    private const val WAKELOCK_TIMEOUT_MS = 12_000L
    private val statusLock = Any()
    private val isRunning = AtomicBoolean(false)

    private fun writeStatus(context: Context, event: String, reset: Boolean) {
      val line = "$event elapsed_ms=${SystemClock.elapsedRealtime()}\n"
      synchronized(statusLock) {
        val file = File(context.filesDir, STATUS_FILE)
        if (reset) file.writeText(line) else file.appendText(line)
      }
      Log.i(TAG, event)
    }

    private fun safeWriteStatus(context: Context, event: String, reset: Boolean) {
      try {
        writeStatus(context, event, reset)
      } catch (e: Throwable) {
        Log.e(TAG, "Unable to write debug diagnostic status: $event", e)
      }
    }
  }
}

internal class NomeSmpLogicalNetworkTransition<T>(
  private val waitForGap: suspend () -> Unit,
) {
  enum class Outcome { PREPARATION_FAILED, NONE_REJECTED, RESTORED, RECOVERY_REQUIRED }

  suspend fun run(
    prepareRecovery: () -> Boolean,
    clearRecovery: () -> Unit,
    applyNone: suspend () -> Boolean,
    currentValidated: () -> T?,
    applyCurrent: suspend (T) -> Boolean,
  ): Outcome {
    if (!prepareRecovery()) return Outcome.PREPARATION_FAILED
    if (!applyNone()) {
      clearRecovery()
      return Outcome.NONE_REJECTED
    }
    waitForGap()
    val current = currentValidated() ?: return Outcome.RECOVERY_REQUIRED
    return if (applyCurrent(current)) {
      clearRecovery()
      Outcome.RESTORED
    } else {
      Outcome.RECOVERY_REQUIRED
    }
  }
}

internal object NomeSmpLogicalNetworkPolicy {
  const val COOLDOWN_MS = 5 * 60_000L
  const val MAX_DELAY_MS = 10 * 60_000L
  const val MAX_API_RECOVERY_ATTEMPTS = 6
  const val MAX_DEFERRED_CHECKS_PER_BURST = 6

  fun cooldownElapsed(nowMs: Long, lastAttemptMs: Long): Boolean =
    lastAttemptMs < 0L || nowMs < lastAttemptMs || nowMs - lastAttemptMs >= COOLDOWN_MS

  fun clampDelay(requestedDelayMs: Long): Long = requestedDelayMs.coerceIn(0L, MAX_DELAY_MS)

  fun nextApiRecoveryAttempt(previous: Int): Int =
    if (previous >= MAX_API_RECOVERY_ATTEMPTS || previous < 0) 1 else previous + 1

  fun nextDeferredCheck(previous: Int): Int =
    if (previous == Int.MAX_VALUE || previous < 0) 1 else previous + 1

  fun deferredBurstCheck(checks: Int): Int =
    ((checks.coerceAtLeast(1) - 1) % MAX_DEFERRED_CHECKS_PER_BURST) + 1

  fun deferredRetryDelay(checks: Int): Long =
    if (deferredBurstCheck(checks) < MAX_DEFERRED_CHECKS_PER_BURST) {
      NomeSmpLogicalNetworkReceiver.RECOVERY_RETRY_DELAY_MS
    } else {
      NomeSmpLogicalNetworkReceiver.RECOVERY_LONG_BACKOFF_MS
    }

  fun apiRetryDelay(attempt: Int): Long =
    if (attempt < MAX_API_RECOVERY_ATTEMPTS) {
      NomeSmpLogicalNetworkReceiver.RECOVERY_RETRY_DELAY_MS
    } else {
      NomeSmpLogicalNetworkReceiver.RECOVERY_LONG_BACKOFF_MS
    }
}
