package chat.simplex.common.views.newchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester

/**
 * Android presentation seam for the official public-channel profile/setup step.
 *
 * The caller retains relay discovery and automatic selection, profile construction, creation,
 * progress/link ownership, cancellation/deletion, and all controller/model mutations. Desktop
 * renders [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformChannelSetupRoute(
  displayName: MutableState<String>,
  profileImage: String?,
  focusRequester: FocusRequester,
  enabledRelayCount: Int,
  hasRelays: Boolean,
  creationInProgress: Boolean,
  canCreate: Boolean,
  currentProfileName: String,
  onEditImage: () -> Unit,
  onDeleteImage: () -> Unit,
  onShowInvalidName: () -> Unit,
  onConfigureRelays: () -> Unit,
  onCreateChannel: () -> Unit,
  onOpenJoinChannel: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
)

internal enum class ChannelCancellationResult {
  DELETED,
  NOT_DELETED,
  FAILED,
}

internal suspend fun cancelCreatedChannel(
  delete: suspend () -> Boolean,
  onDeleted: suspend () -> Unit,
): ChannelCancellationResult =
  try {
    if (delete()) {
      onDeleted()
      ChannelCancellationResult.DELETED
    } else {
      ChannelCancellationResult.NOT_DELETED
    }
  } catch (_: Exception) {
    ChannelCancellationResult.FAILED
  }
