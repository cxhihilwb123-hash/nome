package chat.simplex.common.views.chat.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import chat.simplex.common.views.helpers.DefaultDropdownMenu

@Composable
actual fun PlatformMessageActionsMenu(
  showMenu: MutableState<Boolean>,
  content: @Composable () -> Unit,
) {
  DefaultDropdownMenu(showMenu, dropdownMenuItems = content)
}
