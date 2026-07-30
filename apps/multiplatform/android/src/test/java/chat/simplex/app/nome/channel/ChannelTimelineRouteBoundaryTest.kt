package chat.simplex.app.nome.channel

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelTimelineRouteBoundaryTest {
  @Test
  fun relayChannelsUseTheSharedBottomAnchoredConversationTimeline() {
    val chatView = commonSource(
      "commonMain/kotlin/chat/simplex/common/views/chat/ChatView.kt",
    ).readText()
    val timeline = chatView.chatItemsTimeline()

    assertTrue(
      chatView.contains(
        "MergedItems.create(chatsCtx.chatItems.value.asReversed(),",
      ),
    )
    assertTrue(timeline.contains("modifier.align(Alignment.BottomCenter)"))
    assertTrue(timeline.contains("reverseLayout = true"))
    assertFalse(chatView.contains("nomeChannelFeed"))
  }

  @Test
  fun relayChannelsKeepSharedDateSeparatorsAndNoForwardLayoutAnchor() {
    val chatView = commonSource(
      "commonMain/kotlin/chat/simplex/common/views/chat/ChatView.kt",
    ).readText()
    val timeline = chatView.chatItemsTimeline()
    val channelPostCard = commonSource(
      "commonMain/kotlin/chat/simplex/common/views/chat/PlatformChannelPostCard.kt",
    ).readText()

    assertTrue(timeline.contains("if (last != null)"))
    assertTrue(timeline.contains("DateSeparator(last.meta.itemTs)"))
    assertFalse(chatView.contains("itemSeparation.copy(date = null)"))
    assertFalse(chatView.contains("!nomeChannelFeed"))
    assertFalse(channelPostCard.contains("nomeChannelInitialListPosition"))
  }

  private fun commonSource(relativePath: String): File {
    var current = File(
      requireNotNull(System.getProperty("user.dir")),
    ).absoluteFile
    repeat(8) {
      val candidate = File(current, "../common/src/$relativePath").canonicalFile
      if (candidate.isFile) return candidate
      current = current.parentFile
        ?: error("Unable to locate common source root")
    }
    error("Unable to locate $relativePath")
  }

  private fun String.chatItemsTimeline(): String {
    val start = indexOf("val manager = LocalSelectionManager.current")
    check(start >= 0) { "Missing chat timeline start" }
    val end = indexOf("\n  FloatingButtons(", start)
    check(end > start) { "Missing chat timeline end" }
    return substring(start, end)
  }
}
