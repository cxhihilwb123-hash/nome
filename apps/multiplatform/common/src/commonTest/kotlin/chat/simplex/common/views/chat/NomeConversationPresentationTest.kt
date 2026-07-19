package chat.simplex.common.views.chat

import chat.simplex.common.model.CIContent
import chat.simplex.common.model.ChatItem
import chat.simplex.common.model.E2EEInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
