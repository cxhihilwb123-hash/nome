package chat.simplex.common.views.newchat

import androidx.compose.runtime.Composable

/**
 * Platform presentation seam for the existing New Chat route.
 *
 * Android renders the Nome P10 hub while every action remains owned by the
 * official v6.5.6 New Chat flow. Desktop delegates [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformNewChatHub(
  currentProfileName: String,
  currentProfileImage: String?,
  onOpenProfile: (() -> Unit)?,
  onAddContact: () -> Unit,
  onScanOrPaste: () -> Unit,
  onCreateGroup: () -> Unit,
  onCreateChannel: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
)
