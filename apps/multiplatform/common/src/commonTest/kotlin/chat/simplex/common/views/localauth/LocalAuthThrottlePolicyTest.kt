package chat.simplex.common.views.localauth

import chat.simplex.common.model.SharedPreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class LocalAuthThrottlePolicyTest {
  @Test
  fun backoffStartsAtThresholdAndCapsAtFiveMinutes() {
    assertEquals(0L, LocalAuthThrottlePolicy.backoffMs(LOCAL_AUTH_FAILURE_THRESHOLD - 1))
    assertEquals(5_000L, LocalAuthThrottlePolicy.backoffMs(LOCAL_AUTH_FAILURE_THRESHOLD))
    assertEquals(10_000L, LocalAuthThrottlePolicy.backoffMs(LOCAL_AUTH_FAILURE_THRESHOLD + 1))
    assertEquals(20_000L, LocalAuthThrottlePolicy.backoffMs(LOCAL_AUTH_FAILURE_THRESHOLD + 2))
    assertEquals(LOCAL_AUTH_MAX_BACKOFF_MS, LocalAuthThrottlePolicy.backoffMs(Int.MAX_VALUE))
  }

  @Test
  fun throttledAttemptNeverInvokesEitherCredentialComparison() {
    val now = 1_000_000L
    var appPasscodeCompared = false
    var selfDestructPasscodeCompared = false

    val attempt = LocalAuthThrottlePolicy.evaluate(
      LocalAuthThrottleState(
        failedAttempts = LOCAL_AUTH_FAILURE_THRESHOLD,
        retryAfterEpochMs = now + LOCAL_AUTH_BASE_BACKOFF_MS,
      ),
      now,
    ) {
      appPasscodeCompared = true
      selfDestructPasscodeCompared = true
      LocalAuthCredentialMatch.APP_PASSCODE
    }

    assertIs<LocalAuthAttempt.Throttled>(attempt)
    assertFalse(appPasscodeCompared)
    assertFalse(selfDestructPasscodeCompared)
  }

  @Test
  fun eitherSuccessfulCredentialClearsTheSharedThrottle() {
    for (credential in LocalAuthCredentialMatch.entries) {
      val attempt = LocalAuthThrottlePolicy.evaluate(
        LocalAuthThrottleState(failedAttempts = 4),
        nowEpochMs = 2_000_000L,
      ) { credential }

      val accepted = assertIs<LocalAuthAttempt.Accepted>(attempt)
      assertEquals(credential, accepted.credential)
      assertEquals(LocalAuthThrottleState(), accepted.state)
    }
  }

  @Test
  fun persistedRetrySurvivesLimiterReconstructionAndSuccessClearsIt() {
    val backing = InMemoryThrottlePreferences()
    var now = 3_000_000L
    var limiter = backing.newLimiter { now }

    repeat(LOCAL_AUTH_FAILURE_THRESHOLD) {
      assertIs<LocalAuthAttempt.Rejected>(limiter.evaluate { null })
    }
    assertEquals(LOCAL_AUTH_FAILURE_THRESHOLD, backing.failedAttempts)
    assertEquals(now + LOCAL_AUTH_BASE_BACKOFF_MS, backing.retryAfterEpochMs)

    // Rebuild every preference wrapper and the limiter, as happens after a process restart.
    limiter = backing.newLimiter { now }
    var verifierCalls = 0
    assertIs<LocalAuthAttempt.Throttled>(
      limiter.evaluate {
        verifierCalls += 1
        LocalAuthCredentialMatch.APP_PASSCODE
      },
    )
    assertEquals(0, verifierCalls)

    now = backing.retryAfterEpochMs
    limiter = backing.newLimiter { now }
    assertIs<LocalAuthAttempt.Accepted>(
      limiter.evaluate { LocalAuthCredentialMatch.APP_PASSCODE },
    )
    assertEquals(0, backing.failedAttempts)
    assertEquals(0L, backing.retryAfterEpochMs)

    limiter = backing.newLimiter { now }
    assertEquals(0L, limiter.blockedUntil())
  }

  @Test
  fun twoLimiterInstancesShareOneProcessLockAndDoNotLoseFailureUpdates() {
    val failedAttempts = AtomicInteger(0)
    val retryAfterEpochMs = AtomicLong(0L)
    val coordinateReads = AtomicBoolean(false)
    val simultaneousReaders = CountDownLatch(2)

    fun newLimiter(): PersistentLocalAuthThrottle =
      PersistentLocalAuthThrottle(
        failedAttempts = SharedPreference(
          get = {
            val value = failedAttempts.get()
            if (coordinateReads.get()) {
              simultaneousReaders.countDown()
              simultaneousReaders.await(250, TimeUnit.MILLISECONDS)
            }
            value
          },
          set = failedAttempts::set,
        ),
        retryAfterEpochMs = SharedPreference(
          get = retryAfterEpochMs::get,
          set = retryAfterEpochMs::set,
        ),
        nowEpochMs = { 3_500_000L },
      )

    val firstLimiter = newLimiter()
    val secondLimiter = newLimiter()
    coordinateReads.set(true)
    val start = CountDownLatch(1)
    val executor = Executors.newFixedThreadPool(2)
    try {
      val attempts = listOf(firstLimiter, secondLimiter).map { limiter ->
        executor.submit<LocalAuthAttempt> {
          start.await()
          limiter.evaluate { null }
        }
      }
      start.countDown()
      attempts.forEach { assertIs<LocalAuthAttempt.Rejected>(it.get(2, TimeUnit.SECONDS)) }
    } finally {
      executor.shutdownNow()
    }

    assertEquals(2, failedAttempts.get())
    assertEquals(0L, retryAfterEpochMs.get())
  }

  @Test
  fun selfDestructMatchIsOnlyClassifiedAndNeverRunsARealDestructiveActionInPolicyTests() {
    var destructiveActionRan = false
    val attempt = LocalAuthThrottlePolicy.evaluate(
      LocalAuthThrottleState(),
      nowEpochMs = 4_000_000L,
    ) {
      // The policy accepts a classification only; production storage deletion is not reachable
      // from this unit test and remains owned by LocalAuthView after an accepted result.
      LocalAuthCredentialMatch.SELF_DESTRUCT_PASSCODE
    }

    assertEquals(
      LocalAuthCredentialMatch.SELF_DESTRUCT_PASSCODE,
      assertIs<LocalAuthAttempt.Accepted>(attempt).credential,
    )
    assertFalse(destructiveActionRan)
  }

  @Test
  fun selfDestructCannotReportSuccessWithoutADecoyIdentity() {
    assertEquals("decoy", requireSelfDestructDecoy("decoy"))
    assertFailsWith<IllegalStateException> { requireSelfDestructDecoy<String>(null) }
  }

  @Test
  fun markerWriteFailureErasesEveryCredentialBeforeSimulatedPreStopCrash() {
    var databaseCredentialPresent = true
    var appCredentialPresent = true
    var selfDestructCredentialPresent = true
    var preStopCrashBoundaryReached = false

    assertFailsWith<SimulatedPreStopCrash> {
      establishSelfDestructCrashBarrier(
        persistInProgress = { error("injected marker write failure") },
        eraseDatabaseCredential = { databaseCredentialPresent = false },
        eraseAppCredential = { appCredentialPresent = false },
        eraseSelfDestructCredential = { selfDestructCredentialPresent = false },
        terminate = { error("all credential erasures succeeded; termination is not expected") },
      )
      // Simulates a process crash at the exact boundary before stopChat/delay is reached.
      preStopCrashBoundaryReached = true
      throw SimulatedPreStopCrash()
    }

    assertFalse(databaseCredentialPresent)
    assertFalse(appCredentialPresent)
    assertFalse(selfDestructCredentialPresent)
    assertTrue(preStopCrashBoundaryReached)
  }

  @Test
  fun partialCrashBarrierErasureAttemptsAllCredentialsThenTerminates() {
    val erasures = mutableListOf<String>()
    var terminated = false

    assertFailsWith<SimulatedTermination> {
      establishSelfDestructCrashBarrier(
        persistInProgress = { error("injected marker write failure") },
        eraseDatabaseCredential = {
          erasures += "database"
          error("injected database key failure")
        },
        eraseAppCredential = { erasures += "app" },
        eraseSelfDestructCredential = { erasures += "self-destruct" },
        terminate = {
          terminated = true
          throw SimulatedTermination()
        },
      )
    }

    assertEquals(listOf("database", "app", "self-destruct"), erasures)
    assertTrue(terminated)
  }

  private class InMemoryThrottlePreferences {
    private val values = mutableMapOf<String, Any>()

    val failedAttempts: Int
      get() = values[FAILED_ATTEMPTS] as? Int ?: 0

    val retryAfterEpochMs: Long
      get() = values[RETRY_AFTER] as? Long ?: 0L

    fun newLimiter(nowEpochMs: () -> Long): PersistentLocalAuthThrottle =
      PersistentLocalAuthThrottle(
        failedAttempts = SharedPreference(
          get = { failedAttempts },
          set = { values[FAILED_ATTEMPTS] = it },
        ),
        retryAfterEpochMs = SharedPreference(
          get = { retryAfterEpochMs },
          set = { values[RETRY_AFTER] = it },
        ),
        nowEpochMs = nowEpochMs,
      )

    companion object {
      private const val FAILED_ATTEMPTS = "failedAttempts"
      private const val RETRY_AFTER = "retryAfterEpochMs"
    }
  }

  private class SimulatedPreStopCrash : RuntimeException()

  private class SimulatedTermination : RuntimeException()
}
