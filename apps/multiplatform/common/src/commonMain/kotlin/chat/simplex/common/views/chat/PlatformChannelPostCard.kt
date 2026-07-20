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

/**
 * Nome public-channel feeds use forward layout, so their initial position cannot reuse the
 * negative offset used by the legacy reversed conversation. A normal open starts at the newest
 * item (index 0); an explicit open-around request starts on the real merged-item index.
 */
internal fun nomeChannelInitialListPosition(
  openAroundItemId: Long?,
  targetIndex: Int,
): Pair<Int, Int> =
  if (openAroundItemId == null) {
    0 to 0
  } else {
    targetIndex.coerceAtLeast(0) to 0
  }

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
