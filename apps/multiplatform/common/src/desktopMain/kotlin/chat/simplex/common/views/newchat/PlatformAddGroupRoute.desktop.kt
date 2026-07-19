package chat.simplex.common.views.newchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester

@Composable
internal actual fun PlatformAddGroupRoute(
  displayName: MutableState<String>,
  profileImage: String?,
  focusRequester: FocusRequester,
  incognito: MutableState<Boolean>,
  canCreate: Boolean,
  profileDisclosure: String,
  onEditImage: () -> Unit,
  onDeleteImage: () -> Unit,
  onShowInvalidName: () -> Unit,
  onShowIncognitoInfo: () -> Unit,
  onIncognitoChange: (Boolean) -> Unit,
  onCreateGroup: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()
