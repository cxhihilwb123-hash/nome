package chat.simplex.common.views.usersettings

import kotlinx.coroutines.CancellationException

/**
 * Presentation-layer stages for the existing non-atomic local-identity deletion sequence.
 *
 * The core commands and their order stay unchanged. This type makes partial completion explicit so
 * the UI can retain or reconcile local state without reporting a deletion that did not happen.
 */
internal enum class UserDeletionStage {
  SWITCH_TO_FALLBACK,
  DELETE_TARGET,
  CLEAR_ACTIVE_USER,
  STOP_CHAT,
}

internal sealed interface UserDeletionResult {
  val targetDeleted: Boolean

  data class Completed(
    override val targetDeleted: Boolean,
    val fallbackUserId: Long?,
    val chatStopped: Boolean,
  ) : UserDeletionResult

  data class Failed(
    val stage: UserDeletionStage,
    override val targetDeleted: Boolean,
    val fallbackUserId: Long?,
    val cause: Throwable,
  ) : UserDeletionResult
}

internal suspend fun runUserDeletionLifecycle(
  targetWasActive: Boolean,
  fallbackUserId: Long?,
  stopAfterLastVisibleUser: Boolean,
  switchToFallback: suspend (Long) -> Unit,
  deleteTarget: suspend () -> Unit,
  clearActiveUser: suspend () -> Unit,
  stopChat: suspend () -> Unit,
): UserDeletionResult {
  var stage = UserDeletionStage.DELETE_TARGET
  var targetDeleted = false

  return try {
    if (targetWasActive && fallbackUserId != null) {
      stage = UserDeletionStage.SWITCH_TO_FALLBACK
      switchToFallback(fallbackUserId)
    }

    stage = UserDeletionStage.DELETE_TARGET
    deleteTarget()
    targetDeleted = true

    if (targetWasActive && fallbackUserId == null) {
      stage = UserDeletionStage.CLEAR_ACTIVE_USER
      clearActiveUser()

      if (stopAfterLastVisibleUser) {
        stage = UserDeletionStage.STOP_CHAT
        stopChat()
      }
    }

    UserDeletionResult.Completed(
      targetDeleted = true,
      fallbackUserId = fallbackUserId,
      chatStopped =
        targetWasActive &&
          fallbackUserId == null &&
          stopAfterLastVisibleUser,
    )
  } catch (cause: Exception) {
    if (cause is CancellationException) {
      throw cause
    }
    UserDeletionResult.Failed(
      stage = stage,
      targetDeleted = targetDeleted,
      fallbackUserId = fallbackUserId,
      cause = cause,
    )
  }
}
