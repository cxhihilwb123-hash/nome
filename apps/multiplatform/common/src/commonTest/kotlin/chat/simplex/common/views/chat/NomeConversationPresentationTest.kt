package chat.simplex.common.views.chat

import androidx.compose.ui.unit.dp
import chat.simplex.common.model.CIContent
import chat.simplex.common.model.ChatItem
import chat.simplex.common.model.E2EEInfo
import chat.simplex.common.model.MsgContent
import chat.simplex.common.views.chat.item.nomeHidePublicChannelE2EEHistory
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
  fun androidPublicChannelHidesEncryptionHistoryWithoutChangingOtherFacts() {
    val publicInfo = E2EEInfo(pqEnabled = false, public = true)
    val privateInfo = E2EEInfo(pqEnabled = false, public = false)

    assertTrue(nomeHidePublicChannelE2EEHistory(isAndroid = true, isChannel = true, publicInfo))
    assertFalse(nomeHidePublicChannelE2EEHistory(isAndroid = false, isChannel = true, publicInfo))
    assertFalse(nomeHidePublicChannelE2EEHistory(isAndroid = true, isChannel = false, publicInfo))
    assertFalse(nomeHidePublicChannelE2EEHistory(isAndroid = true, isChannel = true, privateInfo))
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

}
