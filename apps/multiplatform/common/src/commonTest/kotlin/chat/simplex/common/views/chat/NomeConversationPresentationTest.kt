package chat.simplex.common.views.chat

import androidx.compose.ui.unit.dp
import chat.simplex.common.model.CIContent
import chat.simplex.common.model.ChatItem
import chat.simplex.common.model.E2EEInfo
import chat.simplex.common.model.MsgContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NomeConversationPresentationTest {
  @Test
  fun encryptionBannerUsesNewestRealDirectChatFact() {
    val oldInfo = E2EEInfo(pqEnabled = false)
    val newInfo = E2EEInfo(pqEnabled = true)
    val items = listOf(
      ChatItem.getSampleData(id = 1).copy(
        content = CIContent.SndDirectE2EEInfo(oldInfo),
      ),
      ChatItem.getSampleData(id = 2),
      ChatItem.getSampleData(id = 3).copy(
        content = CIContent.RcvDirectE2EEInfo(newInfo),
      ),
    )

    assertEquals(newInfo, nomeDirectE2EEInfo(items))
  }

  @Test
  fun encryptionBannerFailsClosedWithoutARealProducerItem() {
    assertNull(
      nomeDirectE2EEInfo(
        listOf(ChatItem.getSampleData()),
      ),
    )
  }

  @Test
  fun encryptionBannerReservesScaledTextHeight() {
    assertEquals(52.dp, nomeDirectE2EEBannerHeight(0.85f))
    assertEquals(52.dp, nomeDirectE2EEBannerHeight(1f))
    assertEquals(67.6.dp, nomeDirectE2EEBannerHeight(1.3f))
    assertEquals(104.dp, nomeDirectE2EEBannerHeight(2f))
  }

  @Test
  fun channelPostUsesFirstRealLineAsTitle() {
    assertEquals(
      NomeChannelPostText(
        title = "Release note",
        body = "First detail\nSecond detail",
      ),
      nomeChannelPostText(
        "Release note\nFirst detail\nSecond detail",
      ),
    )
  }

  @Test
  fun channelPostDoesNotInventMissingBody() {
    assertEquals(
      NomeChannelPostText(
        title = "Release note",
        body = "",
      ),
      nomeChannelPostText(
        "Release note",
      ),
    )
  }

  @Test
  fun channelPostPreservesSourceWhitespaceAndBlankLines() {
    assertEquals(
      NomeChannelPostText(
        title = "  Release note  ",
        body = " First detail \n\nSecond detail\n",
      ),
      nomeChannelPostText(
        "  Release note  \n First detail \n\nSecond detail\n",
      ),
    )
  }

  @Test
  fun channelFeedUsesCardsOnlyForMessageContent() {
    assertTrue(
      nomeChannelUsesPostCard(
        MsgContent.MCText("Controlled post"),
      ),
    )
    assertTrue(
      nomeChannelUsesPostCard(
        MsgContent.MCFile("Controlled file caption"),
      ),
    )
    assertTrue(
      nomeChannelUsesOfficialFileRenderer(
        MsgContent.MCFile("Downloadable file caption"),
      ),
    )
    assertFalse(
      nomeChannelUsesOfficialFileRenderer(
        MsgContent.MCText("Controlled post"),
      ),
    )
    assertFalse(
      nomeChannelUsesPostCard(null),
    )
  }

  @Test
  fun channelOpenAroundKeepsTargetIndexInForwardLayout() {
    assertEquals(
      0 to 0,
      nomeChannelInitialListPosition(
        openAroundItemId = null,
        targetIndex = 17,
      ),
    )
    assertEquals(
      17 to 0,
      nomeChannelInitialListPosition(
        openAroundItemId = 99L,
        targetIndex = 17,
      ),
    )
    assertEquals(
      0 to 0,
      nomeChannelInitialListPosition(
        openAroundItemId = 99L,
        targetIndex = -1,
      ),
    )
  }

  @Test
  fun channelDisclosureReservesHistoryMarkerAndScaledText() {
    assertEquals(
      120.dp,
      nomeChannelDisclosureHeight(1f),
    )
    assertEquals(
      240.dp,
      nomeChannelDisclosureHeight(2f),
    )
  }
}
