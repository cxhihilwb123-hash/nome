package chat.simplex.common.views.chat.group

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformGroupChatInfoRoute(
  title: String,
  onClose: () -> Unit,
  content: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()
