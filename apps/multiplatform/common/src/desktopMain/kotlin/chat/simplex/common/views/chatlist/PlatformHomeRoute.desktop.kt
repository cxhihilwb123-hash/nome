package chat.simplex.common.views.chatlist

import androidx.compose.runtime.Composable
import chat.simplex.common.model.ChatModel
import chat.simplex.common.views.helpers.AnimatedViewState
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
actual fun PlatformHomeRoute(
  chatModel: ChatModel,
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  stopped: Boolean,
  defaultContent: @Composable () -> Unit,
) {
  defaultContent()
}
