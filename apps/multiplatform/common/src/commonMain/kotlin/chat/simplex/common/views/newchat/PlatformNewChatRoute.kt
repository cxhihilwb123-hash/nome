package chat.simplex.common.views.newchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import chat.simplex.common.model.CreatedConnLink

/**
 * Platform presentation seam for the official one-time invitation and paste/scan route.
 *
 * [NewChatView] retains link creation, pending-connection disposal, parsing, planning, and
 * navigation ownership. Each platform renders its Nome presentation around those callbacks.
 */
@Composable
internal expect fun PlatformNewChatRoute(
  selection: MutableState<NewChatOption>,
  invitation: CreatedConnLink,
  invitationCreating: Boolean,
  currentProfileName: String,
  hostDeviceName: String?,
  hostDeviceIsRemote: Boolean,
  onOpenProfile: (() -> Unit)?,
  pastedLink: MutableState<String>,
  showQRCodeScanner: MutableState<Boolean>,
  onRetryInvitation: () -> Unit,
  onInvitationLocalAction: () -> Unit,
  onSubmitPastedLink: (String) -> Unit,
  onScannedLink: suspend (String) -> Boolean,
  onClose: () -> Unit,
)
