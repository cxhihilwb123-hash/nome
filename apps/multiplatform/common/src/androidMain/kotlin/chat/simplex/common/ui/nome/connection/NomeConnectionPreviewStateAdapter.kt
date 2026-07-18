package chat.simplex.common.ui.nome.connection

import chat.simplex.common.views.newchat.ConnectionPreviewFailureKind
import chat.simplex.common.views.newchat.ConnectionPreviewIdentity

enum class NomeConnectionPreviewPhase {
  Ready,
  Connecting,
  Replanning,
  Pending,
  Failure,
}

data class NomeConnectionPreviewState(
  val identity: ConnectionPreviewIdentity,
  val phase: NomeConnectionPreviewPhase,
  val failureKind: ConnectionPreviewFailureKind? = null,
  val existingContactName: String? = null,
)

sealed class NomeConnectionPreviewEvent {
  data class SelectIdentity(
    val identity: ConnectionPreviewIdentity,
  ) : NomeConnectionPreviewEvent()

  data object Submit : NomeConnectionPreviewEvent()
  data object Retry : NomeConnectionPreviewEvent()
  data object Pending : NomeConnectionPreviewEvent()

  data class Failed(
    val kind: ConnectionPreviewFailureKind,
    val existingContactName: String? = null,
  ) : NomeConnectionPreviewEvent()
}

object NomeConnectionPreviewStateAdapter {
  fun initial(
    identity: ConnectionPreviewIdentity,
  ): NomeConnectionPreviewState =
    NomeConnectionPreviewState(
      identity = identity,
      phase = NomeConnectionPreviewPhase.Ready,
    )

  fun reduce(
    state: NomeConnectionPreviewState,
    event: NomeConnectionPreviewEvent,
  ): NomeConnectionPreviewState = when (event) {
    is NomeConnectionPreviewEvent.SelectIdentity ->
      if (state.phase == NomeConnectionPreviewPhase.Ready) {
        state.copy(identity = event.identity)
      } else {
        state
      }
    NomeConnectionPreviewEvent.Submit ->
      if (state.phase == NomeConnectionPreviewPhase.Ready) {
        state.copy(
          phase = NomeConnectionPreviewPhase.Connecting,
          failureKind = null,
          existingContactName = null,
        )
      } else {
        state
      }
    NomeConnectionPreviewEvent.Retry ->
      if (state.phase == NomeConnectionPreviewPhase.Failure) {
        state.copy(
          phase = NomeConnectionPreviewPhase.Replanning,
          failureKind = null,
          existingContactName = null,
        )
      } else {
        state
      }
    NomeConnectionPreviewEvent.Pending ->
      if (state.phase == NomeConnectionPreviewPhase.Connecting) {
        state.copy(
          phase = NomeConnectionPreviewPhase.Pending,
          failureKind = null,
          existingContactName = null,
        )
      } else {
        state
      }
    is NomeConnectionPreviewEvent.Failed ->
      if (
        state.phase == NomeConnectionPreviewPhase.Connecting ||
        state.phase == NomeConnectionPreviewPhase.Replanning
      ) {
        state.copy(
          phase = NomeConnectionPreviewPhase.Failure,
          failureKind = event.kind,
          existingContactName = event.existingContactName,
        )
      } else {
        state
      }
  }
}
