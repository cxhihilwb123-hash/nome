package chat.simplex.common.views.chat

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformChannelPostCard(
  visible: Boolean,
  authorName: String,
  authorImage: String?,
  timestamp: String,
  text: NomeChannelPostText,
  fileName: String?,
  fileSize: String?,
  fileContent: (@Composable () -> Unit)?,
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}
