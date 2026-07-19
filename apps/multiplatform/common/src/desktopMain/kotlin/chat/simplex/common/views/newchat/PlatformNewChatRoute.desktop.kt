package chat.simplex.common.views.newchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import chat.simplex.common.model.CreatedConnLink

@Composable
internal actual fun PlatformNewChatRoute(
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
) = legacyContent()
