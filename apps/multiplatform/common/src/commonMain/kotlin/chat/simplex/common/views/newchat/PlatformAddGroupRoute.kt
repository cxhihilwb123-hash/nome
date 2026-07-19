package chat.simplex.common.views.newchat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester

/**
 * Android presentation seam for the official private-group creation owner.
 *
 * The common caller retains profile validation, incognito preference ownership, image processing,
 * group creation, controller/model mutation, and the next official route. Desktop renders
 * [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformAddGroupRoute(
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
)
