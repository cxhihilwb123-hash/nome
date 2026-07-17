package chat.simplex.app.nome.home

import chat.simplex.common.model.ChatListLoadResult
import chat.simplex.common.model.ChatListLoadState
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatListLoadStateTest {
  @Test
  fun onlyLatestSameGenerationAttemptMayMutateLoadTruth() {
    val savedUser = ChatModel.currentUser.value
    val savedRemoteHost = ChatModel.currentRemoteHost.value
    val savedChats = ChatModel.chats.value.toList()
    val savedLoadState = ChatModel.chatListLoadState.value
    try {
      ChatModel.currentRemoteHost.value = null
      ChatModel.currentUser.value = User.sampleData
      ChatModel.chatsContext.updateChats(emptyList())
      val generation = requireNotNull(ChatModel.currentChatListGeneration())

      val olderAttempt = ChatModel.beginChatListLoad(null, hideRows = true)
      val newestAttempt = ChatModel.beginChatListLoad(null, hideRows = false)

      assertFalse(
        ChatModel.applyChatListLoadResult(
          ChatListLoadResult.Failure(generation),
          olderAttempt,
        ),
      )
      assertEquals(
        ChatListLoadState.Loading(generation, hideRows = false),
        ChatModel.chatListLoadState.value,
      )
      assertTrue(
        ChatModel.applyChatListLoadResult(
          ChatListLoadResult.Success(generation, emptyList()),
          newestAttempt,
        ),
      )
      assertEquals(ChatListLoadState.Loaded(generation), ChatModel.chatListLoadState.value)

      val staleGenerationAttempt = ChatModel.beginChatListLoad(null, hideRows = false)
      ChatModel.currentUser.value = User.sampleData.copy(userId = generation.userId + 1)
      assertFalse(
        ChatModel.applyChatListLoadResult(
          ChatListLoadResult.Success(generation, emptyList()),
          staleGenerationAttempt,
        ),
      )
      assertEquals(
        ChatListLoadState.Loading(generation, hideRows = false),
        ChatModel.chatListLoadState.value,
      )
    } finally {
      ChatModel.currentRemoteHost.value = savedRemoteHost
      ChatModel.currentUser.value = savedUser
      ChatModel.chatsContext.updateChats(savedChats)
      ChatModel.chatListLoadState.value = savedLoadState
    }
  }

  @Test
  fun noCurrentUserCannotReplaceAnExistingUserAttempt() {
    val savedUser = ChatModel.currentUser.value
    val savedRemoteHost = ChatModel.currentRemoteHost.value
    val savedChats = ChatModel.chats.value.toList()
    val savedLoadState = ChatModel.chatListLoadState.value
    try {
      ChatModel.currentRemoteHost.value = null
      ChatModel.currentUser.value = User.sampleData
      val attemptId = ChatModel.beginChatListLoad(null, hideRows = true)

      assertFalse(
        ChatModel.applyChatListLoadResult(
          ChatListLoadResult.NoCurrentUser(remoteHostId = null),
          attemptId,
        ),
      )
      assertTrue(ChatModel.chatListLoadState.value is ChatListLoadState.Loading)
    } finally {
      ChatModel.currentRemoteHost.value = savedRemoteHost
      ChatModel.currentUser.value = savedUser
      ChatModel.chatsContext.updateChats(savedChats)
      ChatModel.chatListLoadState.value = savedLoadState
    }
  }
}
