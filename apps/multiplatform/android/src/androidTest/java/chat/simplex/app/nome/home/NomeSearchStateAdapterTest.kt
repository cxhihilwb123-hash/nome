package chat.simplex.app.nome.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.GroupProfile
import chat.simplex.common.model.GroupType
import chat.simplex.common.model.PublicGroupProfile
import chat.simplex.common.ui.nome.home.classifyNomeSearchResults
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeSearchStateAdapterTest {
  @Test
  fun matchedChatsAreGroupedInStableSupportedOrder() {
    val contact = Chat.sampleData
    val group = Chat(
      remoteHostId = null,
      chatInfo = ChatInfo.Group.sampleData,
      chatItems = emptyList(),
    )
    val channelInfo = ChatInfo.Group.sampleData.groupInfo.copy(
      groupId = 2L,
      groupProfile = GroupProfile.sampleData.copy(
        displayName = "news",
        publicGroup = PublicGroupProfile(
          groupType = GroupType.Channel,
          groupLink = "redacted-test-link",
          publicGroupId = "channel-test",
        ),
      ),
    )
    val channel = Chat(
      remoteHostId = null,
      chatInfo = ChatInfo.Group(
        groupInfo = channelInfo,
        groupChatScope = null,
      ),
      chatItems = emptyList(),
    )
    val notes = Chat(
      remoteHostId = null,
      chatInfo = ChatInfo.Local.sampleData,
      chatItems = emptyList(),
    )

    val results = classifyNomeSearchResults(
      listOf(channel, notes, group, contact),
    )

    assertEquals(listOf(contact), results.contacts)
    assertEquals(listOf(group), results.groups)
    assertEquals(listOf(channel), results.channels)
    assertEquals(listOf(notes), results.notes)
    assertEquals(
      listOf(contact, group, channel, notes),
      results.ordered,
    )
  }

  @Test
  fun emptyProducerDoesNotCreateSearchResults() {
    val results = classifyNomeSearchResults(emptyList())

    assertEquals(true, results.isEmpty)
    assertEquals(emptyList<Chat>(), results.ordered)
  }
}
