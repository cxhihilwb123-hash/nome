package chat.simplex.app.nome.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatListLoadGeneration
import chat.simplex.common.model.ChatListLoadState
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.User
import chat.simplex.common.model.UserNetworkInfo
import chat.simplex.common.model.UserNetworkType
import chat.simplex.common.model.getTimestampText
import chat.simplex.common.ui.nome.home.NomeHomeConnectivityState
import chat.simplex.common.ui.nome.home.NomeHomeContentState
import chat.simplex.common.ui.nome.home.NomeHomeCoreState
import chat.simplex.common.ui.nome.home.NomeHomeState
import chat.simplex.common.ui.nome.home.NomeHomeStateAdapter
import chat.simplex.common.ui.nome.home.NomeHomeTruthInput
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.chatlist.NomeHomeRouteContent
import chat.simplex.common.views.chatlist.nomeHomeRouteProjection
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeHomeComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @After
  fun clearPendingDeletionChats() {
    ChatModel.deletedChats.value = emptyList()
  }

  @Test
  fun loadingAndOfflineExposePoliteLiveRegions() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val activeState = mutableStateOf(
      state(content = NomeHomeContentState.LOADING),
    )
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeHomeRouteContent(ChatModel, activeState.value)
      }
    }

    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        target.getString(R.string.nome_home_loading_state),
      ),
    )
      .assertIsDisplayed()
      .assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.LiveRegion,
          LiveRegionMode.Polite,
        ),
      )

    composeRule.runOnIdle {
      activeState.value = state(
        content = NomeHomeContentState.POPULATED,
        connectivity = NomeHomeConnectivityState.DEVICE_OFFLINE,
        chats = listOf(Chat.sampleData),
      )
    }

    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        target.getString(R.string.nome_home_offline_state),
      ),
    )
      .assertIsDisplayed()
      .assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.LiveRegion,
          LiveRegionMode.Polite,
        ),
      )
  }

  @Test
  fun adapterTruthKeepsSameGenerationRowsHidesMismatchAndExposesUnknownNetwork() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val generation = ChatListLoadGeneration(remoteHostId = null, userId = 1L)
    val chat = Chat.sampleData
    val activeState = mutableStateOf(
      NomeHomeStateAdapter.derive(
        NomeHomeTruthInput(
          currentGeneration = generation,
          localUserCreated = true,
          chatRunning = true,
          switchingUsersAndHosts = false,
          loadState = ChatListLoadState.Loading(generation, hideRows = false),
          baseChats = listOf(chat),
          visibleChats = listOf(chat),
          hasActiveFilter = false,
          platformNetworkInfo = null,
        ),
      ),
    )
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeHomeRouteContent(ChatModel, activeState.value)
      }
    }

    composeRule.onNodeWithText(chat.chatInfo.chatViewName).assertIsDisplayed()
    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        target.getString(R.string.nome_home_network_unknown_state),
      ),
    )
      .assertIsDisplayed()
      .assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.LiveRegion,
          LiveRegionMode.Polite,
        ),
      )

    composeRule.runOnIdle {
      activeState.value = NomeHomeStateAdapter.derive(
        NomeHomeTruthInput(
          currentGeneration = generation,
          localUserCreated = true,
          chatRunning = true,
          switchingUsersAndHosts = false,
          loadState = ChatListLoadState.Loaded(generation.copy(userId = 2L)),
          baseChats = listOf(chat),
          visibleChats = listOf(chat),
          hasActiveFilter = false,
          platformNetworkInfo = UserNetworkInfo(UserNetworkType.WIFI, online = true),
        ),
      )
    }

    composeRule.onAllNodesWithText(chat.chatInfo.chatViewName).assertCountEquals(0)
    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        target.getString(R.string.nome_home_loading_state),
      ),
    ).assertIsDisplayed()
  }

  @Test
  fun runningReadyChatIsTouchableAndStoppedChatIsReadOnly() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val chat = Chat.sampleData
    val activeState = mutableStateOf(
      state(
        content = NomeHomeContentState.POPULATED,
        chats = listOf(chat),
      ),
    )
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeHomeRouteContent(
          chatModel = ChatModel,
          state = activeState.value,
          showChatPreviews = true,
        )
      }
    }
    val openLabel = target.getString(
      R.string.nome_home_open_chat,
      chat.chatInfo.chatViewName,
      chat.chatItems.lastOrNull()?.text(chat.chatInfo.isChannel),
      chat.chatItems.lastOrNull()?.let {
        getTimestampText(it.meta.itemTs)
      } ?: getTimestampText(chat.chatInfo.chatTs),
    )

    composeRule.onNodeWithContentDescription(openLabel)
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.Role,
          Role.Button,
        ),
      )

    composeRule.runOnIdle {
      activeState.value = state(
        content = NomeHomeContentState.POPULATED,
        core = NomeHomeCoreState.STOPPED,
        chats = listOf(chat),
      )
    }
    val preview = chat.chatItems.lastOrNull()?.text(chat.chatInfo.isChannel)
      ?.takeIf { it.isNotBlank() }
      ?: target.getString(R.string.nome_home_direct_chat)
    val readOnlyLabel = target.getString(
      R.string.nome_home_read_only_chat,
      chat.chatInfo.chatViewName,
      preview,
      chat.chatItems.lastOrNull()?.let {
        getTimestampText(it.meta.itemTs)
      } ?: getTimestampText(chat.chatInfo.chatTs),
    )
    composeRule.onNodeWithContentDescription(readOnlyLabel)
      .assertIsDisplayed()
      .assertHeightIsAtLeast(48.dp)
      .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
  }

  @Test
  fun pendingDeletionChatIsReadOnlyWhileCoreRuns() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val chat = Chat.sampleData
    ChatModel.deletedChats.value = listOf(chat.remoteHostId to chat.chatInfo.id)
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeHomeRouteContent(
          chatModel = ChatModel,
          state = state(
            content = NomeHomeContentState.POPULATED,
            chats = listOf(chat),
          ),
          showChatPreviews = true,
        )
      }
    }
    val preview = chat.chatItems.lastOrNull()?.text(chat.chatInfo.isChannel)
      ?.takeIf { it.isNotBlank() }
      ?: target.getString(R.string.nome_home_direct_chat)
    val timestamp = chat.chatItems.lastOrNull()?.let {
      getTimestampText(it.meta.itemTs)
    } ?: getTimestampText(chat.chatInfo.chatTs)

    composeRule.onNodeWithContentDescription(
      target.getString(
        R.string.nome_home_read_only_chat,
        chat.chatInfo.chatViewName,
        preview,
        timestamp,
      ),
    )
      .assertIsDisplayed()
      .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
  }

  @Test
  fun contactCardsRemainVisibleReadOnlyAndDeletedRowsLeaveTheRouteProjection() {
    val direct = Chat.sampleData.chatInfo as ChatInfo.Direct
    val contactCard = Chat.sampleData.copy(
      chatInfo = direct.copy(
        contact = direct.contact.copy(
          activeConn = null,
          profile = direct.contact.profile.copy(contactLink = "simplex:/contact#test"),
          preparedContact = null,
          contactRequestId = null,
          chatDeleted = false,
        ),
      ),
    )
    val deleted = Chat.sampleData.copy(
      chatInfo = direct.copy(contact = direct.contact.copy(chatDeleted = true)),
    )

    val (baseChats, visibleChats) = nomeHomeRouteProjection(
      allChats = listOf(contactCard, deleted),
      upstreamVisibleChats = emptyList(),
      hasActiveFilter = false,
    )
    val (_, filteredVisibleChats) = nomeHomeRouteProjection(
      allChats = listOf(contactCard, deleted),
      upstreamVisibleChats = emptyList(),
      hasActiveFilter = true,
    )

    assertEquals(listOf(contactCard), baseChats)
    assertEquals(listOf(contactCard), visibleChats)
    assertEquals(emptyList<Chat>(), filteredVisibleChats)
  }

  @Test
  fun contactCardAndConnectionRowsExposeNoNavigationAction() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val direct = Chat.sampleData.chatInfo as ChatInfo.Direct
    val contactCard = Chat(
      remoteHostId = null,
      chatInfo = direct.copy(
        contact = direct.contact.copy(
          activeConn = null,
          profile = direct.contact.profile.copy(contactLink = "simplex:/contact#test"),
          preparedContact = null,
          contactRequestId = null,
        ),
      ),
      chatItems = emptyList(),
    )
    val contactConnection = Chat(
      remoteHostId = null,
      chatInfo = ChatInfo.ContactConnection.getSampleData(),
      chatItems = emptyList(),
    )
    val cases = listOf(
      contactCard to R.string.nome_home_direct_chat,
      contactConnection to R.string.nome_home_connection_pending,
    )

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeHomeRouteContent(
          chatModel = ChatModel,
          state = state(
            content = NomeHomeContentState.POPULATED,
            chats = cases.map { it.first },
          ),
          showChatPreviews = true,
        )
      }
    }

    for ((chat, previewResource) in cases) {
      composeRule.onNodeWithContentDescription(
        target.getString(
          R.string.nome_home_read_only_chat,
          chat.chatInfo.chatViewName,
          target.getString(previewResource),
          getTimestampText(chat.chatInfo.chatTs),
        ),
      )
        .assertHeightIsAtLeast(48.dp)
        .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }
  }

  @Test
  fun legacyAndDirectPendingContactRequestsExposeNavigationAction() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val direct = Chat.sampleData.chatInfo as ChatInfo.Direct
    val pendingDirect = Chat(
      remoteHostId = null,
      chatInfo = direct.copy(
        contact = direct.contact.copy(
          activeConn = null,
          contactRequestId = 101L,
        ),
      ),
      chatItems = emptyList(),
    )
    val contactRequest = Chat(
      remoteHostId = null,
      chatInfo = ChatInfo.ContactRequest.sampleData,
      chatItems = emptyList(),
    )
    val cases = listOf(
      pendingDirect to R.string.nome_home_contact_request,
      contactRequest to R.string.nome_home_contact_request,
    )

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeHomeRouteContent(
          chatModel = ChatModel,
          state = state(
            content = NomeHomeContentState.POPULATED,
            chats = cases.map { it.first },
          ),
          showChatPreviews = true,
        )
      }
    }

    val expectedDescriptions =
      cases.map { (chat, previewResource) ->
        target.getString(
          R.string.nome_home_open_chat,
          chat.chatInfo.chatViewName,
          target.getString(previewResource),
          getTimestampText(chat.chatInfo.chatTs),
        )
      }
    for (
      (description, count) in
      expectedDescriptions.groupingBy { it }.eachCount()
    ) {
      val matchingNodes =
        composeRule.onAllNodesWithContentDescription(
          description,
        )
      matchingNodes.assertCountEquals(count)
      repeat(count) { index ->
        matchingNodes[index]
          .assertHeightIsAtLeast(48.dp)
          .assertHasClickAction()
          .assert(
            SemanticsMatcher.expectValue(
              SemanticsProperties.Role,
              Role.Button,
            ),
          )
      }
    }
  }

  @Test
  fun clickRechecksTheLatestSameIdRowBeforeNavigation() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val capturedReadyChat = Chat.sampleData
    val direct = capturedReadyChat.chatInfo as ChatInfo.Direct
    val currentNotReadyChat = capturedReadyChat.copy(
      chatInfo = direct.copy(
        contact = direct.contact.copy(activeConn = null),
      ),
    )
    val savedUser = ChatModel.currentUser.value
    val savedRemoteHost = ChatModel.currentRemoteHost.value
    val savedChatRunning = ChatModel.chatRunning.value
    val savedSwitching = ChatModel.switchingUsersAndHosts.value
    val savedLoadState = ChatModel.chatListLoadState.value
    val savedChats = ChatModel.chats.value.toList()
    var openedChat: Chat? = null
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeHomeRouteContent(
          chatModel = ChatModel,
          state = state(
            content = NomeHomeContentState.POPULATED,
            chats = listOf(capturedReadyChat),
          ),
          showChatPreviews = true,
          onOpenChat = { openedChat = it },
        )
      }
    }
    try {
      composeRule.runOnIdle {
        ChatModel.currentRemoteHost.value = null
        ChatModel.currentUser.value = User.sampleData
        ChatModel.chatRunning.value = true
        ChatModel.switchingUsersAndHosts.value = false
        ChatModel.chatsContext.updateChats(listOf(currentNotReadyChat))
        ChatModel.chatListLoadState.value = ChatListLoadState.Loaded(
          requireNotNull(ChatModel.currentChatListGeneration()),
        )
      }
      val openLabel = target.getString(
        R.string.nome_home_open_chat,
        capturedReadyChat.chatInfo.chatViewName,
        capturedReadyChat.chatItems.lastOrNull()?.text(capturedReadyChat.chatInfo.isChannel),
        capturedReadyChat.chatItems.lastOrNull()?.let {
          getTimestampText(it.meta.itemTs)
        } ?: getTimestampText(capturedReadyChat.chatInfo.chatTs),
      )
      composeRule.onNodeWithContentDescription(openLabel).performClick()
      composeRule.runOnIdle { assertNull(openedChat) }
    } finally {
      composeRule.runOnIdle {
        ChatModel.currentRemoteHost.value = savedRemoteHost
        ChatModel.currentUser.value = savedUser
        ChatModel.chatRunning.value = savedChatRunning
        ChatModel.switchingUsersAndHosts.value = savedSwitching
        ChatModel.chatsContext.updateChats(savedChats)
        ChatModel.chatListLoadState.value = savedLoadState
      }
    }
  }

  @Test
  fun disabledPreviewPreferenceHidesMessageTextAndSpokenPreview() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val chat = Chat.sampleData
    val messageText = chat.chatItems.last().text(chat.chatInfo.isChannel)
    val timestamp = getTimestampText(
      chat.chatItems.last().meta.itemTs,
    )
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeHomeRouteContent(
          chatModel = ChatModel,
          state = state(
            content = NomeHomeContentState.POPULATED,
            chats = listOf(chat),
          ),
          showChatPreviews = false,
        )
      }
    }

    composeRule.onAllNodesWithText(messageText).assertCountEquals(0)
    composeRule.onNodeWithContentDescription(
      target.getString(
        R.string.nome_home_open_chat,
        chat.chatInfo.chatViewName,
        target.getString(R.string.nome_home_direct_chat),
        timestamp,
      ),
    ).assertIsDisplayed()
  }

  @Test
  fun favoriteAndUnreadFactsAreExposedAsRowState() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val direct = Chat.sampleData.chatInfo as ChatInfo.Direct
    val chat = Chat.sampleData.copy(
      chatInfo = direct.copy(
        contact = direct.contact.copy(
          chatSettings = direct.contact.chatSettings.copy(favorite = true),
        ),
      ),
      chatStats = Chat.ChatStats(unreadCount = 2),
    )
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeHomeRouteContent(
          chatModel = ChatModel,
          state = state(
            content = NomeHomeContentState.POPULATED,
            chats = listOf(chat),
          ),
          showChatPreviews = true,
        )
      }
    }

    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        listOf(
          target.getString(R.string.nome_home_unread_count, 2),
          target.getString(R.string.nome_home_favorite),
        ).joinToString(),
      ),
    ).assertIsDisplayed()
  }

  @Test
  fun allPrimaryStatesRemainVisibleAtTwoHundredPercent() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val cases = listOf(
      NomeHomeContentState.LOADING to R.string.nome_home_loading_state,
      NomeHomeContentState.FIRST_USE to R.string.nome_home_first_use_state,
      NomeHomeContentState.TRUE_EMPTY to R.string.nome_home_empty_state,
      NomeHomeContentState.FILTERED_NO_RESULT to R.string.nome_home_filtered_empty_state,
      NomeHomeContentState.UNAVAILABLE to R.string.nome_home_unavailable_state,
    )
    val activeState = mutableStateOf(state(content = cases.first().first))

    composeRule.setContent {
      val density = LocalDensity.current
      CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 2f),
      ) {
        NomeAndroidTheme(darkTheme = true) {
          NomeHomeRouteContent(ChatModel, activeState.value)
        }
      }
    }

    for ((content, stateDescription) in cases) {
      composeRule.runOnIdle {
        activeState.value = state(content = content)
      }
      val node = composeRule.onNode(
        SemanticsMatcher.expectValue(
          SemanticsProperties.StateDescription,
          target.getString(stateDescription),
        ),
      )
      node.assertIsDisplayed()
      if (content != NomeHomeContentState.LOADING) {
        node.assertTextContains(
          when (content) {
            NomeHomeContentState.LOADING -> error("Loading uses skeleton semantics")
            NomeHomeContentState.FIRST_USE -> target.getString(R.string.nome_home_first_use_title)
            NomeHomeContentState.TRUE_EMPTY -> target.getString(R.string.nome_home_empty_title)
            NomeHomeContentState.FILTERED_NO_RESULT -> target.getString(R.string.nome_home_filtered_empty_title)
            NomeHomeContentState.UNAVAILABLE -> target.getString(R.string.nome_home_unavailable_title)
            NomeHomeContentState.POPULATED -> error("Populated is not a panel case")
          },
        )
      }
    }
  }

  private fun state(
    content: NomeHomeContentState,
    connectivity: NomeHomeConnectivityState = NomeHomeConnectivityState.ONLINE,
    core: NomeHomeCoreState = NomeHomeCoreState.RUNNING,
    chats: List<Chat> = emptyList(),
  ) = NomeHomeState(
    content = content,
    connectivity = connectivity,
    core = core,
    visibleChats = chats,
    hasCachedChats = chats.isNotEmpty(),
  )
}
