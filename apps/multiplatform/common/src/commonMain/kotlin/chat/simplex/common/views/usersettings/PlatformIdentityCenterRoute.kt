package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import chat.simplex.common.model.User
import chat.simplex.common.model.UserInfo

/**
 * Android presentation seam for the official local-profile manager.
 *
 * The common owner retains user creation, profile editing, activation, authentication,
 * hide/unhide, notification privacy, deletion, and all controller/model mutations. Desktop
 * renders [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformIdentityCenterRoute(
  users: List<User>,
  filteredUsers: List<UserInfo>,
  searchTextOrPassword: MutableState<String>,
  profileHidden: Boolean,
  visibleUsersCount: Int,
  incognitoDefault: Boolean,
  socksProxyEnabled: Boolean,
  onClose: () -> Unit,
  onRevealPasswordEntry: () -> Unit,
  onAddUser: () -> Unit,
  onEditCurrentUser: () -> Unit,
  onActivateUser: (User) -> Unit,
  onRemoveUser: (User) -> Unit,
  onUnhideUser: (User) -> Unit,
  onMuteUser: (User) -> Unit,
  onUnmuteUser: (User) -> Unit,
  onHideUser: (User) -> Unit,
  onSetIncognitoDefault: (Boolean) -> Unit,
  onOpenIncognitoInfo: () -> Unit,
  onOpenNetworkSettings: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenContacts: () -> Unit,
  legacyContent: @Composable () -> Unit,
)
