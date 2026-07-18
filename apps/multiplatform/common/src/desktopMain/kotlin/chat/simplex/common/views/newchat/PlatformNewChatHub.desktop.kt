package chat.simplex.common.views.newchat

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformNewChatHub(
  currentProfileName: String,
  currentProfileImage: String?,
  onOpenProfile: (() -> Unit)?,
  onAddContact: () -> Unit,
  onScanOrPaste: () -> Unit,
  onCreateGroup: () -> Unit,
  onCreateChannel: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()
