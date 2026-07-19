package chat.simplex.common.views.newchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import chat.simplex.common.model.CreatedConnLink

/**
 * Platform presentation seam for the official one-time invitation and scan/paste route.
 *
 * [NewChatView] retains link creation, pending-connection disposal, parsing, planning, and
 * navigation ownership. Android renders the Nome P11/P12 presentation. Desktop delegates
 * [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformNewChatRoute(
  selection: MutableState<NewChatOption>,
  invitation: CreatedConnLink,
  invitationCreating: Boolean,
  currentProfileName: String,
  onOpenProfile: (() -> Unit)?,
  pastedLink: MutableState<String>,
  showQRCodeScanner: MutableState<Boolean>,
  onRetryInvitation: () -> Unit,
  onInvitationLocalAction: () -> Unit,
  onSubmitPastedLink: (String) -> Unit,
  onScannedLink: suspend (String) -> Boolean,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
)
