package chat.simplex.common.views.newchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester

@Composable
internal actual fun PlatformChannelSetupRoute(
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
) = legacyContent()
