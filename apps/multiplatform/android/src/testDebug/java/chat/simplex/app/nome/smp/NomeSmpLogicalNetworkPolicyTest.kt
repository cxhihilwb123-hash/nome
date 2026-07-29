package chat.simplex.app.nome.smp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class NomeSmpLogicalNetworkPolicyTest {
  @Test
  fun firstAttemptIsAllowed() {
    assertTrue(NomeSmpLogicalNetworkPolicy.cooldownElapsed(nowMs = 100L, lastAttemptMs = -1L))
  }

  @Test
  fun cooldownIsStrictUntilBoundary() {
    val last = 1_000L
    assertFalse(
      NomeSmpLogicalNetworkPolicy.cooldownElapsed(
        nowMs = last + NomeSmpLogicalNetworkPolicy.COOLDOWN_MS - 1L,
        lastAttemptMs = last,
      )
    )
    assertTrue(
      NomeSmpLogicalNetworkPolicy.cooldownElapsed(
        nowMs = last + NomeSmpLogicalNetworkPolicy.COOLDOWN_MS,
        lastAttemptMs = last,
      )
    )
  }

  @Test
  fun elapsedRealtimeResetAllowsAttempt() {
    assertTrue(NomeSmpLogicalNetworkPolicy.cooldownElapsed(nowMs = 10L, lastAttemptMs = 50_000L))
  }

  @Test
  fun scheduleDelayIsBounded() {
    assertEquals(0L, NomeSmpLogicalNetworkPolicy.clampDelay(-1L))
    assertEquals(12_345L, NomeSmpLogicalNetworkPolicy.clampDelay(12_345L))
    assertEquals(
      NomeSmpLogicalNetworkPolicy.MAX_DELAY_MS,
      NomeSmpLogicalNetworkPolicy.clampDelay(Long.MAX_VALUE),
    )
  }

  @Test
  fun rejectedNoneClearsPreparedRecovery() = runBlocking {
    val events = mutableListOf<String>()
    val outcome = transition(events).run(
      prepareRecovery = { events += "prepare"; true },
      clearRecovery = { events += "clear" },
      applyNone = { events += "none"; false },
      currentValidated = { "wifi" },
      applyCurrent = { events += "current"; true },
    )

    assertEquals(NomeSmpLogicalNetworkTransition.Outcome.NONE_REJECTED, outcome)
    assertEquals(listOf("prepare", "none", "clear"), events)
  }

  @Test
  fun lostValidatedNetworkNeverReplaysStaleSnapshot() = runBlocking {
    val events = mutableListOf<String>()
    val outcome = transition(events).run(
      prepareRecovery = { events += "prepare"; true },
      clearRecovery = { events += "clear" },
      applyNone = { events += "none"; true },
      currentValidated = { null },
      applyCurrent = { events += "current"; true },
    )

    assertEquals(NomeSmpLogicalNetworkTransition.Outcome.RECOVERY_REQUIRED, outcome)
    assertEquals(listOf("prepare", "none", "gap"), events)
  }

  @Test
  fun rejectedCurrentKeepsRecoveryPending() = runBlocking {
    val events = mutableListOf<String>()
    val outcome = transition(events).run(
      prepareRecovery = { events += "prepare"; true },
      clearRecovery = { events += "clear" },
      applyNone = { events += "none"; true },
      currentValidated = { "wifi" },
      applyCurrent = { events += "current:$it"; false },
    )

    assertEquals(NomeSmpLogicalNetworkTransition.Outcome.RECOVERY_REQUIRED, outcome)
    assertEquals(listOf("prepare", "none", "gap", "current:wifi"), events)
  }

  @Test
  fun acceptedCurrentClearsRecovery() = runBlocking {
    val events = mutableListOf<String>()
    val outcome = transition(events).run(
      prepareRecovery = { events += "prepare"; true },
      clearRecovery = { events += "clear" },
      applyNone = { events += "none"; true },
      currentValidated = { "wifi" },
      applyCurrent = { events += "current:$it"; true },
    )

    assertEquals(NomeSmpLogicalNetworkTransition.Outcome.RESTORED, outcome)
    assertEquals(listOf("prepare", "none", "gap", "current:wifi", "clear"), events)
  }

  @Test
  fun exceptionAfterPreparationDoesNotClearRecovery() {
    val events = mutableListOf<String>()
    try {
      runBlocking {
        transition(events).run(
          prepareRecovery = { events += "prepare"; true },
          clearRecovery = { events += "clear" },
          applyNone = { events += "none"; error("boom") },
          currentValidated = { "wifi" },
          applyCurrent = { true },
        )
      }
    } catch (_: IllegalStateException) {
      // Expected: the receiver schedules recovery from its outer failure handler.
    }
    assertEquals(listOf("prepare", "none"), events)
  }

  @Test
  fun failedRecoveryPreparationNeverAppliesNone() = runBlocking {
    val events = mutableListOf<String>()
    val outcome = transition(events).run(
      prepareRecovery = { events += "prepare"; false },
      clearRecovery = { events += "clear" },
      applyNone = { events += "none"; true },
      currentValidated = { "wifi" },
      applyCurrent = { events += "current"; true },
    )

    assertEquals(NomeSmpLogicalNetworkTransition.Outcome.PREPARATION_FAILED, outcome)
    assertEquals(listOf("prepare"), events)
  }

  @Test
  fun exhaustedApiRecoveryBatchStartsANewFutureBatch() {
    assertEquals(
      1,
      NomeSmpLogicalNetworkPolicy.nextApiRecoveryAttempt(
        NomeSmpLogicalNetworkPolicy.MAX_API_RECOVERY_ATTEMPTS,
      ),
    )
    assertEquals(
      NomeSmpLogicalNetworkReceiver.RECOVERY_LONG_BACKOFF_MS,
      NomeSmpLogicalNetworkPolicy.apiRetryDelay(
        NomeSmpLogicalNetworkPolicy.MAX_API_RECOVERY_ATTEMPTS,
      ),
    )
  }

  @Test
  fun unavailableNetworkChecksUseBoundedBurstsWithoutTerminalState() {
    assertEquals(1, NomeSmpLogicalNetworkPolicy.nextDeferredCheck(Int.MAX_VALUE))
    assertEquals(
      NomeSmpLogicalNetworkReceiver.RECOVERY_LONG_BACKOFF_MS,
      NomeSmpLogicalNetworkPolicy.deferredRetryDelay(
        NomeSmpLogicalNetworkPolicy.MAX_DEFERRED_CHECKS_PER_BURST,
      ),
    )
    assertEquals(
      NomeSmpLogicalNetworkReceiver.RECOVERY_RETRY_DELAY_MS,
      NomeSmpLogicalNetworkPolicy.deferredRetryDelay(
        NomeSmpLogicalNetworkPolicy.MAX_DEFERRED_CHECKS_PER_BURST + 1,
      ),
    )
  }

  private fun transition(events: MutableList<String>) = NomeSmpLogicalNetworkTransition<String>(
    waitForGap = { events += "gap" },
  )
}
