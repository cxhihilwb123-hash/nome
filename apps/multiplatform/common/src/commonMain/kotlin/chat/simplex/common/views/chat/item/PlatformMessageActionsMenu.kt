package chat.simplex.common.views.chat.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

/**
 * Android presents the existing chat-item actions in the P18 bottom-sheet structure.
 * Desktop keeps the official anchored menu. Action eligibility and callbacks remain owned by
 * [ChatItemView].
 */
@Composable
expect fun PlatformMessageActionsMenu(
  showMenu: MutableState<Boolean>,
  content: @Composable () -> Unit,
)
