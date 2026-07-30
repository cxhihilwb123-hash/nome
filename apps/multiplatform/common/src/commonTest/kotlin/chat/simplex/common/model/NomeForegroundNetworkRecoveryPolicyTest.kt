package chat.simplex.common.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NomeForegroundNetworkRecoveryPolicyTest {
  @Test
  fun reconstructsAtLongBackgroundBoundary() {
    assertFalse(shouldReconstruct(backgroundDurationMillis = 59_999L))
    assertTrue(shouldReconstruct(backgroundDurationMillis = 60_000L))
  }

  @Test
  fun skipsUnsafeOrUselessReconstruction() {
    assertFalse(shouldReconstruct(backgroundDurationMillis = null))
    assertFalse(shouldReconstruct(chatRunning = false))
    assertFalse(shouldReconstruct(online = false))
    assertFalse(shouldReconstruct(callInProgress = true))
  }

  @Test
  fun enforcesCooldownAndAllowsElapsedClockReset() {
    val last = 100_000L
    assertFalse(
      shouldReconstruct(
        resumedAtMillis = last + NomeForegroundNetworkRecoveryPolicy.RECONSTRUCTION_COOLDOWN_MILLIS - 1L,
        lastReconstructionAtMillis = last,
      )
    )
    assertTrue(
      shouldReconstruct(
        resumedAtMillis = last + NomeForegroundNetworkRecoveryPolicy.RECONSTRUCTION_COOLDOWN_MILLIS,
        lastReconstructionAtMillis = last,
      )
    )
    assertTrue(shouldReconstruct(resumedAtMillis = 10L, lastReconstructionAtMillis = last))
  }

  private fun shouldReconstruct(
    backgroundDurationMillis: Long? = NomeForegroundNetworkRecoveryPolicy.MIN_BACKGROUND_DURATION_MILLIS,
    resumedAtMillis: Long = 1_000_000L,
    lastReconstructionAtMillis: Long? = null,
    chatRunning: Boolean = true,
    online: Boolean = true,
    callInProgress: Boolean = false,
  ): Boolean = NomeForegroundNetworkRecoveryPolicy.shouldReconstruct(
    backgroundDurationMillis = backgroundDurationMillis,
    resumedAtMillis = resumedAtMillis,
    lastReconstructionAtMillis = lastReconstructionAtMillis,
    chatRunning = chatRunning,
    online = online,
    callInProgress = callInProgress,
  )
}
