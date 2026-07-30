package chat.simplex.common.model

/**
 * Decides when Android should reconstruct the logical network after a long background gap.
 * The monotonic timestamps are supplied by the Android lifecycle so the policy stays testable.
 */
internal object NomeForegroundNetworkRecoveryPolicy {
  const val MIN_BACKGROUND_DURATION_MILLIS = 60_000L
  const val RECONSTRUCTION_COOLDOWN_MILLIS = 5 * 60_000L

  fun shouldReconstruct(
    backgroundDurationMillis: Long?,
    resumedAtMillis: Long,
    lastReconstructionAtMillis: Long?,
    chatRunning: Boolean,
    online: Boolean,
    callInProgress: Boolean,
  ): Boolean {
    if (backgroundDurationMillis == null || backgroundDurationMillis < MIN_BACKGROUND_DURATION_MILLIS) return false
    if (!chatRunning || !online || callInProgress) return false
    return lastReconstructionAtMillis == null ||
      resumedAtMillis < lastReconstructionAtMillis ||
      resumedAtMillis - lastReconstructionAtMillis >= RECONSTRUCTION_COOLDOWN_MILLIS
  }
}
