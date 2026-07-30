package chat.simplex.common.views.chat

import androidx.compose.runtime.Composable
import chat.simplex.common.model.MsgContent

internal data class NomeChannelPostText(
  val title: String,
  val body: String,
)

internal fun nomeChannelPostText(
  text: String,
): NomeChannelPostText {
  val firstLineBreak =
    text.indexOf('\n')
  return NomeChannelPostText(
    title =
      if (firstLineBreak < 0) {
        text
      } else {
        text.substring(
          0,
          firstLineBreak,
        )
      },
    body =
      if (firstLineBreak < 0) {
        ""
      } else {
        text.substring(
          firstLineBreak + 1,
        )
      },
  )
}

internal fun nomeChannelUsesPostCard(
  msgContent: MsgContent?,
): Boolean =
  msgContent is MsgContent.MCText ||
    msgContent is MsgContent.MCFile

internal fun nomeChannelUsesOfficialFileRenderer(
  msgContent: MsgContent?,
): Boolean =
  msgContent is MsgContent.MCFile

@Composable
internal expect fun PlatformChannelPostCard(
  visible: Boolean,
  authorName: String,
  authorImage: String?,
  timestamp: String,
  text: NomeChannelPostText,
  fileName: String?,
  fileSize: String?,
  fileContent: (@Composable () -> Unit)?,
  legacyContent: @Composable () -> Unit,
)
