package chat.simplex.common.views.chatlist

import androidx.compose.runtime.Composable
import chat.simplex.common.model.ChatModel
import chat.simplex.common.views.helpers.AnimatedViewState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Narrow platform seam at the existing home-route selection point.
 *
 * The declaration deliberately contains no Nome UI and does not own navigation or model state.
 * Desktop delegates [defaultContent] unchanged; Android may render its platform-specific home
 * while preserving the root gates that selected this route.
 */
@Composable
expect fun PlatformHomeRoute(
  chatModel: ChatModel,
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  stopped: Boolean,
  defaultContent: @Composable () -> Unit,
)
