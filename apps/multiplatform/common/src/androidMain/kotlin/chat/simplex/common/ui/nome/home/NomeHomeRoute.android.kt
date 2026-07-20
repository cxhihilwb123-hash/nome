package chat.simplex.common.views.chatlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.helpers.NetworkObserver
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatListLoadState
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.ChatController
import chat.simplex.common.model.getTimestampText
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.accessibility.NomeFocusRestoration
import chat.simplex.common.ui.nome.components.NomeStatePanel
import chat.simplex.common.ui.nome.components.NomeStatePanelState
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.home.NomeHomeConnectivityState
import chat.simplex.common.ui.nome.home.NomeHomeContentState
import chat.simplex.common.ui.nome.home.NomeHomeCoreState
import chat.simplex.common.ui.nome.home.NomeHomeState
import chat.simplex.common.ui.nome.home.NomeHomeStateAdapter
import chat.simplex.common.ui.nome.home.NomeHomeTruthInput
import chat.simplex.common.ui.nome.home.classifyNomeSearchResults
import chat.simplex.common.ui.nome.home.NomeSearchRouteContent
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.AnimatedViewState
import chat.simplex.common.views.helpers.DefaultDropdownMenu
import chat.simplex.common.views.helpers.tryOrShowError
import chat.simplex.common.views.onboarding.SetNotificationsModeAdditions
import chat.simplex.common.views.contacts.onRequestAccepted
import chat.simplex.res.MR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
actual fun PlatformHomeRoute(
  chatModel: ChatModel,
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  stopped: Boolean,
  defaultContent: @Composable () -> Unit,
) {
  val searchOpen = rememberSaveable { mutableStateOf(false) }
  val searchQuery = rememberSaveable { mutableStateOf("") }
  val allChats = chatModel.chats.value.toList()
  val activeFilter = chatModel.activeChatTagFilter.value
  val noLinkSearch: State<Boolean> = remember { mutableStateOf(false) }
  val noLinkChat: State<String?> = remember { mutableStateOf(null) }
  val upstreamVisibleChats = filteredChats(
    searchShowingSimplexLink = noLinkSearch,
    searchChatFilteredBySimplexLink = noLinkChat,
    searchText = searchQuery.value,
    chats = allChats,
    activeFilter = activeFilter,
  )
  val hasActiveFilter =
    activeFilter != null || searchQuery.value.isNotBlank()
  val (baseChats, visibleChats) = nomeHomeRouteProjection(
    allChats = allChats,
    upstreamVisibleChats = upstreamVisibleChats,
    hasActiveFilter = hasActiveFilter,
  )
  val state = NomeHomeStateAdapter.derive(
    NomeHomeTruthInput(
      currentGeneration = chatModel.currentChatListGeneration(),
      localUserCreated = chatModel.localUserCreated.value,
      chatRunning = chatModel.chatRunning.value,
      switchingUsersAndHosts = chatModel.switchingUsersAndHosts.value,
      loadState = chatModel.chatListLoadState.value,
      baseChats = baseChats,
      visibleChats = visibleChats,
      hasActiveFilter = hasActiveFilter,
      platformNetworkInfo = NetworkObserver.shared.platformNetworkInfo.value,
    ),
  )

  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  val scope = rememberCoroutineScope()
  val openSearchResult: (Chat) -> Unit = { capturedChat ->
    resolveOpenableNomeHomeChat(chatModel, capturedChat)?.let { currentChat ->
      when (val info = currentChat.chatInfo) {
        is ChatInfo.Direct ->
          if (
            info.contact.nextAcceptContactRequest &&
              info.contact.contactRequestId != null
          ) {
            contactRequestAlertDialog(
              currentChat.remoteHostId,
              info.contact,
              chatModel,
            ) {
              onRequestAccepted(it)
            }
          } else {
            // groupDirectInv acceptance is owned by the official in-chat
            // ComposeContextMemberContactActionsView, not by the standalone P14 request route.
            scope.launch {
              directChatAction(
                currentChat.remoteHostId,
                info.contact,
                chatModel,
              )
            }
          }
        is ChatInfo.Group ->
          scope.launch {
            groupChatAction(
              currentChat.remoteHostId,
              info.groupInfo,
              chatModel,
            )
          }
        is ChatInfo.Local ->
          scope.launch {
            noteFolderChatAction(
              currentChat.remoteHostId,
              info.noteFolder,
            )
          }
        is ChatInfo.ContactRequest ->
          contactRequestAlertDialog(
            currentChat.remoteHostId,
            info,
            chatModel,
          ) {
            onRequestAccepted(it)
          }
        is ChatInfo.ContactConnection,
        is ChatInfo.InvalidJSON ->
          run {
            chatModel.chatId.value = currentChat.id
          }
      }
    }
  }
  val oneHandUI = remember { ChatController.appPrefs.oneHandUI.state }
  NomeAndroidTheme(darkTheme = darkTheme) {
    if (searchOpen.value) {
      NomeSearchRouteContent(
        query = searchQuery.value,
        results = classifyNomeSearchResults(state.visibleChats),
        contentState = state.content,
        coreState = state.core,
        onQueryChange = { searchQuery.value = it },
        onOpenChat = openSearchResult,
        onClose = {
          searchQuery.value = ""
          searchOpen.value = false
        },
      )
    } else {
      NomeHomeRouteContent(
        chatModel = chatModel,
        state = state,
        onOpenProfile = {
          userPickerState.value = AnimatedViewState.VISIBLE
        },
        onSearch = {
          searchQuery.value = ""
          searchOpen.value = true
        },
        onNewConnection = {
          showNewChatSheet(
            oneHandUI = oneHandUI,
            onOpenProfile = {
              userPickerState.value = AnimatedViewState.VISIBLE
            },
          )
        },
      )
    }
  }
  val wasAllowedToSetupNotifications = rememberSaveable { mutableStateOf(false) }
  val canEnableNotifications = remember {
    derivedStateOf { chatModel.chatRunning.value == true }
  }
  if (wasAllowedToSetupNotifications.value || canEnableNotifications.value) {
    SetNotificationsModeAdditions()
    LaunchedEffect(Unit) { wasAllowedToSetupNotifications.value = true }
  }
  tryOrShowError("NomeUserPicker", error = {}) {
    UserPicker(
      chatModel = chatModel,
      userPickerState = userPickerState,
      setPerformLA = setPerformLA,
    )
  }
}

@Composable
fun NomeHomeRouteContent(
  chatModel: ChatModel,
  state: NomeHomeState<Chat>,
  showChatPreviews: Boolean = chatModel.showChatPreviews.value,
  profileNameOverride: String? = null,
  onOpenChat: ((Chat) -> Unit)? = null,
  onOpenProfile: () -> Unit = {},
  onSearch: () -> Unit = {},
  onNewConnection: () -> Unit = {},
) {
  val dimensions = NomeTheme.dimensions
  val scope = rememberCoroutineScope()
  val pendingDeletionChats = chatModel.deletedChats.value.toSet()
  val openChat: (Chat) -> Unit = { capturedChat ->
    resolveOpenableNomeHomeChat(chatModel, capturedChat)?.let { currentChat ->
      if (onOpenChat != null) {
        onOpenChat(currentChat)
      } else when (val info = currentChat.chatInfo) {
        is ChatInfo.Direct ->
          if (
            info.contact.nextAcceptContactRequest &&
              info.contact.contactRequestId != null
          ) {
            contactRequestAlertDialog(
              currentChat.remoteHostId,
              info.contact,
              chatModel,
            ) {
              onRequestAccepted(it)
            }
          } else {
            // Keep groupDirectInv on the official in-chat member-invitation acceptance owner.
            scope.launch {
              directChatAction(
                currentChat.remoteHostId,
                info.contact,
                chatModel,
              )
            }
          }
        is ChatInfo.Group ->
          scope.launch {
            groupChatAction(
              currentChat.remoteHostId,
              info.groupInfo,
              chatModel,
            )
          }
        is ChatInfo.Local ->
          scope.launch { noteFolderChatAction(currentChat.remoteHostId, info.noteFolder) }
        is ChatInfo.ContactRequest ->
          contactRequestAlertDialog(
            currentChat.remoteHostId,
            info,
            chatModel,
          ) {
            onRequestAccepted(it)
          }
        is ChatInfo.ContactConnection,
        is ChatInfo.InvalidJSON ->
          run {
            chatModel.chatId.value = currentChat.id
          }
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(NomeTheme.colors.background)
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = dimensions.screenHorizontalInset,
        top = dimensions.space12,
        end = dimensions.screenHorizontalInset,
        bottom = 96.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(dimensions.space12),
    ) {
      item {
        NomeHomeHeader(
          chatModel = chatModel,
          profileNameOverride = profileNameOverride,
          onOpenProfile = onOpenProfile,
        )
      }
      item {
        Text(
          text = stringResource(R.string.nome_home_title),
          modifier = Modifier.semantics { heading() },
          style = NomeTheme.typography.display,
          color = NomeTheme.colors.textPrimary,
        )
        Text(
          text = stringResource(R.string.nome_home_subtitle),
          style = NomeTheme.typography.body,
          color = NomeTheme.colors.textSecondary,
        )
      }
      item {
        NomeHomeSearchBar(onSearch)
      }

      when (state.connectivity) {
        NomeHomeConnectivityState.UNKNOWN -> item {
          NomeStatusPanel(
            state = NomeStatePanelState.LOADING,
            title = R.string.nome_home_network_unknown_title,
            body = R.string.nome_home_network_unknown_body,
            stateDescription = R.string.nome_home_network_unknown_state,
          )
        }
        NomeHomeConnectivityState.DEVICE_OFFLINE -> item {
          NomeStatusPanel(
            state = NomeStatePanelState.OFFLINE,
            title = R.string.nome_home_offline_title,
            body = R.string.nome_home_offline_body,
            stateDescription = R.string.nome_home_offline_state,
          )
        }
        NomeHomeConnectivityState.ONLINE -> Unit
      }

      if (state.core == NomeHomeCoreState.STOPPED) {
        item {
          NomeStatusPanel(
            state = NomeStatePanelState.ERROR,
            title = R.string.nome_home_stopped_title,
            body = R.string.nome_home_stopped_body,
            stateDescription = R.string.nome_home_stopped_state,
          )
        }
      }

      when (state.content) {
        NomeHomeContentState.LOADING -> item {
          NomeHomeSkeleton()
        }
        NomeHomeContentState.FIRST_USE -> item {
          NomeStatusPanel(
            state = NomeStatePanelState.EMPTY,
            title = R.string.nome_home_first_use_title,
            body = R.string.nome_home_first_use_body,
            stateDescription = R.string.nome_home_first_use_state,
          )
        }
        NomeHomeContentState.TRUE_EMPTY -> item {
          NomeStatusPanel(
            state = NomeStatePanelState.EMPTY,
            title = R.string.nome_home_empty_title,
            body = R.string.nome_home_empty_body,
            stateDescription = R.string.nome_home_empty_state,
          )
        }
        NomeHomeContentState.FILTERED_NO_RESULT -> item {
          NomeStatusPanel(
            state = NomeStatePanelState.EMPTY,
            title = R.string.nome_home_filtered_empty_title,
            body = R.string.nome_home_filtered_empty_body,
            stateDescription = R.string.nome_home_filtered_empty_state,
          )
        }
        NomeHomeContentState.UNAVAILABLE -> {
          item {
            NomeStatusPanel(
              state = NomeStatePanelState.ERROR,
              title = R.string.nome_home_unavailable_title,
              body = R.string.nome_home_unavailable_body,
              stateDescription = R.string.nome_home_unavailable_state,
            )
          }
          if (state.visibleChats.isNotEmpty()) {
            item { NomeChatsHeading() }
            items(state.visibleChats, key = { it.remoteHostId to it.id }) { chat ->
              NomeChatRow(
                chatModel = chatModel,
                chat = chat,
                coreState = state.core,
                pendingDeletion = pendingDeletionChats.contains(chat.remoteHostId to chat.chatInfo.id),
                showChatPreviews = showChatPreviews,
                openChat = openChat,
              )
            }
          }
        }
        NomeHomeContentState.POPULATED -> {
          item { NomeChatsHeading() }
          items(state.visibleChats, key = { it.remoteHostId to it.id }) { chat ->
            NomeChatRow(
              chatModel = chatModel,
              chat = chat,
              coreState = state.core,
              pendingDeletion = pendingDeletionChats.contains(chat.remoteHostId to chat.chatInfo.id),
              showChatPreviews = showChatPreviews,
              openChat = openChat,
            )
          }
        }
      }
    }
    if (state.core == NomeHomeCoreState.RUNNING) {
      FloatingActionButton(
        onClick = onNewConnection,
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = dimensions.screenHorizontalInset, bottom = dimensions.space20)
          .size(56.dp)
          .nomeTalkBackSemantics(
            label = stringResource(R.string.nome_home_new_connection),
            role = Role.Button,
          ),
        backgroundColor = NomeTheme.colors.action,
        contentColor = NomeTheme.colors.onAction,
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(28.dp),
        )
      }
    }
  }
}

@Composable
private fun NomeHomeSearchBar(onSearch: () -> Unit) {
  NomeSurface(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clickable(onClick = onSearch)
      .nomeTalkBackSemantics(
        label = stringResource(R.string.nome_home_search),
        role = Role.Button,
      ),
    shape = NomeTheme.shapes.pill,
    color = NomeTheme.colors.surfaceSubtle,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = NomeTheme.colors.textSecondary,
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = stringResource(R.string.nome_p09_search_placeholder),
        style = NomeTheme.typography.body,
        color = NomeTheme.colors.textSecondary,
      )
    }
  }
}

fun nomeHomeRouteProjection(
  allChats: List<Chat>,
  upstreamVisibleChats: List<Chat>,
  hasActiveFilter: Boolean,
): Pair<List<Chat>, List<Chat>> {
  val baseChats = allChats.filterNot { it.chatInfo.chatDeleted }
  val upstreamVisibleKeys = upstreamVisibleChats.mapTo(mutableSetOf()) {
    it.remoteHostId to it.id
  }
  val visibleChats = baseChats.filter {
    (it.remoteHostId to it.id) in upstreamVisibleKeys ||
      (!hasActiveFilter && it.chatInfo.contactCard)
  }
  return baseChats to visibleChats
}

internal fun resolveOpenableNomeHomeChat(chatModel: ChatModel, capturedChat: Chat): Chat? {
  if (
    chatModel.chatRunning.value != true ||
    chatModel.switchingUsersAndHosts.value
  ) {
    return null
  }
  val currentGeneration = chatModel.currentChatListGeneration()
  val rowsBelongToCurrentGeneration = when (val loadState = chatModel.chatListLoadState.value) {
    ChatListLoadState.Initial,
    is ChatListLoadState.NoCurrentUser -> false
    is ChatListLoadState.Loaded ->
      currentGeneration != null && loadState.generation == currentGeneration
    is ChatListLoadState.Loading ->
      !loadState.hideRows &&
        currentGeneration != null &&
        loadState.generation == currentGeneration
    is ChatListLoadState.Unavailable ->
      currentGeneration != null && loadState.generation == currentGeneration
  }
  if (!rowsBelongToCurrentGeneration) return null
  val currentChat = chatModel.chats.value.firstOrNull {
    it.remoteHostId == capturedChat.remoteHostId && it.chatInfo.id == capturedChat.chatInfo.id
  } ?: return null
  if (
    chatModel.deletedChats.value.contains(currentChat.remoteHostId to currentChat.chatInfo.id) ||
    currentChat.chatInfo.chatDeleted
  ) {
    return null
  }
  return when (val info = currentChat.chatInfo) {
    is ChatInfo.Direct ->
      currentChat.takeIf {
        !info.contactCard &&
          (
            info.ready ||
              (
                info.contact.nextAcceptContactRequest &&
                  info.contact.contactRequestId != null
              )
          )
      }
    is ChatInfo.Group,
    is ChatInfo.Local -> currentChat.takeIf { info.ready }
    is ChatInfo.ContactRequest -> currentChat
    is ChatInfo.ContactConnection,
    is ChatInfo.InvalidJSON -> null
  }
}

@Composable
private fun NomeHomeHeader(
  chatModel: ChatModel,
  profileNameOverride: String?,
  onOpenProfile: () -> Unit,
) {
  val dimensions = NomeTheme.dimensions
  val currentProfileFocusRequester =
    remember { FocusRequester() }
  val currentProfileFocusRequest =
    NomeFocusRestoration.currentProfileRequests
      .collectAsState()
      .value
  LaunchedEffect(currentProfileFocusRequest) {
    if (currentProfileFocusRequest == 0L) {
      return@LaunchedEffect
    }
    delay(500)
    repeat(3) {
      if (currentProfileFocusRequester.requestFocus()) {
        return@LaunchedEffect
      }
      delay(400)
    }
  }
  val profileName =
    profileNameOverride
      ?.takeIf { it.isNotBlank() }
      ?: chatModel.currentUser.value?.profile?.displayName
      ?.takeIf { it.isNotBlank() }
      ?: stringResource(R.string.nome_home_unknown_profile)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = dimensions.minimumRowHeight),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Rounded.ChatBubbleOutline,
      contentDescription = null,
      modifier = Modifier.size(dimensions.icon),
      tint = NomeTheme.colors.action,
    )
    Spacer(Modifier.width(dimensions.space8))
    Text(
      text = stringResource(R.string.nome_home_brand),
      modifier = Modifier.weight(1f),
      style = NomeTheme.typography.title,
      color = NomeTheme.colors.textPrimary,
      fontWeight = FontWeight.Bold,
    )
    Box(
      modifier = Modifier
        .size(dimensions.minimumTouchTarget)
        .clickable(onClick = onOpenProfile)
        .focusRequester(currentProfileFocusRequester)
        .focusable()
        .nomeTalkBackSemantics(
          label = stringResource(R.string.nome_home_current_profile, profileName),
          role = Role.Button,
        ),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(NomeTheme.colors.surfaceContainer),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = profileName.firstOrNull()?.uppercase() ?: "N",
          style = NomeTheme.typography.title,
          color = NomeTheme.colors.action,
        )
      }
    }
  }
  Divider(color = NomeTheme.colors.divider)
}

@Composable
private fun NomeChatsHeading() {
  Text(
    text = stringResource(R.string.nome_home_section_chats),
    modifier = Modifier.semantics { heading() },
    style = NomeTheme.typography.label,
    color = NomeTheme.colors.textSecondary,
  )
}

@Composable
private fun NomeStatusPanel(
  state: NomeStatePanelState,
  title: Int,
  body: Int,
  stateDescription: Int,
) {
  NomeStatePanel(
    state = state,
    title = stringResource(title),
    description = stringResource(body),
    stateDescription = stringResource(stateDescription),
    modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
private fun NomeHomeSkeleton() {
  val dimensions = NomeTheme.dimensions
  NomeSurface(
    modifier = Modifier
      .fillMaxWidth()
      .nomeTalkBackSemantics(
        label = stringResource(R.string.nome_home_skeleton_semantics),
        state = stringResource(R.string.nome_home_loading_state),
        liveRegionMode = LiveRegionMode.Polite,
      ),
    color = NomeTheme.colors.surface,
    border = BorderStroke(dimensions.divider, NomeTheme.colors.border),
  ) {
    Column(
      modifier = Modifier.padding(dimensions.space16),
      verticalArrangement = Arrangement.spacedBy(dimensions.space12),
    ) {
      repeat(3) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensions.minimumRowHeight),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(NomeTheme.colors.surfaceSubtle),
          )
          Spacer(Modifier.width(dimensions.space12))
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensions.space8),
          ) {
            Box(
              Modifier
                .fillMaxWidth(0.55f)
                .height(dimensions.space12)
                .clip(NomeTheme.shapes.compact)
                .background(NomeTheme.colors.surfaceSubtle),
            )
            Box(
              Modifier
                .fillMaxWidth(0.86f)
                .height(dimensions.space8)
                .clip(NomeTheme.shapes.compact)
                .background(NomeTheme.colors.surfaceSubtle),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NomeChatRow(
  chatModel: ChatModel,
  chat: Chat,
  coreState: NomeHomeCoreState,
  pendingDeletion: Boolean,
  showChatPreviews: Boolean,
  openChat: (Chat) -> Unit,
) {
  val dimensions = NomeTheme.dimensions
  val showMenu = remember(chat.id) {
    mutableStateOf(false)
  }
  val inProgress = remember(chat.id) {
    mutableStateOf(false)
  }
  val info = chat.chatInfo
  val canOpen = coreState == NomeHomeCoreState.RUNNING &&
    !pendingDeletion &&
    when (info) {
      is ChatInfo.Direct ->
        !info.contactCard &&
          (
            info.ready ||
              (
                info.contact.nextAcceptContactRequest &&
                  info.contact.contactRequestId != null
              )
          )
      is ChatInfo.Group,
      is ChatInfo.Local -> info.ready
      is ChatInfo.ContactRequest -> true
      is ChatInfo.ContactConnection,
      is ChatInfo.InvalidJSON -> false
    }
  val hasMenu = info !is ChatInfo.InvalidJSON
  val menuLabel =
    dev.icerock.moko.resources.compose.stringResource(
      MR.strings.icon_descr_more_button,
    )
  val latestItem = chat.chatItems.lastOrNull()
  val preview =
    if (showChatPreviews) {
      latestItem?.text(info.isChannel)
        ?.takeIf { it.isNotBlank() }
        ?: nomeChatTypeDescription(info)
    } else {
      nomeChatTypeDescription(info)
    }
  val timestamp = getTimestampText(latestItem?.meta?.itemTs ?: info.chatTs)
  val unreadCount = chat.chatStats.unreadCount
  val favorite = info.chatSettings?.favorite == true
  val unreadState =
    if (unreadCount > 0) {
      stringResource(R.string.nome_home_unread_count, unreadCount)
    } else if (chat.chatStats.unreadChat) {
      stringResource(R.string.nome_home_unread)
    } else {
      null
    }
  val favoriteState =
    if (favorite) stringResource(R.string.nome_home_favorite) else null
  val spokenState = listOfNotNull(unreadState, favoriteState).joinToString()
  val spokenLabel =
    if (canOpen) {
      stringResource(
        R.string.nome_home_open_chat,
        info.chatViewName,
        preview,
        timestamp,
      )
    } else {
      stringResource(
        R.string.nome_home_read_only_chat,
        info.chatViewName,
        preview,
        timestamp,
      )
    }
  val interactionModifier =
    if (canOpen) {
      Modifier
        .combinedClickable(
          onClick = {
            if (!inProgress.value) {
              openChat(chat)
            }
          },
          onLongClick = {
            if (hasMenu) {
              showMenu.value = true
            }
          },
        )
        .nomeMinimumTouchTarget()
        .nomeTalkBackSemantics(
          label = spokenLabel,
          state = spokenState.takeIf { it.isNotBlank() },
          role = Role.Button,
        )
        .semantics {
          if (hasMenu) {
            customActions =
              listOf(
                CustomAccessibilityAction(
                  label = menuLabel,
                  action = {
                    showMenu.value = true
                    true
                  },
                ),
              )
          }
        }
    } else {
      Modifier
        .nomeMinimumTouchTarget()
        .nomeTalkBackSemantics(
          label = spokenLabel,
          state = spokenState.takeIf { it.isNotBlank() },
        )
    }

  Box {
    NomeSurface(
      modifier = interactionModifier.fillMaxWidth(),
      color = NomeTheme.colors.surface,
      border = BorderStroke(dimensions.divider, NomeTheme.colors.border),
    ) {
      Row(
        modifier = Modifier.padding(
          horizontal = dimensions.space12,
          vertical = dimensions.space8,
        ),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(NomeTheme.colors.surfaceContainer),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = info.chatViewName.firstOrNull()?.uppercase() ?: "?",
            style = NomeTheme.typography.title,
            color = NomeTheme.colors.action,
          )
        }
        Spacer(Modifier.width(dimensions.space12))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = info.chatViewName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = NomeTheme.typography.title,
            color = NomeTheme.colors.textPrimary,
          )
          Text(
            text = preview,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = NomeTheme.typography.body,
            color = NomeTheme.colors.textSecondary,
          )
        }
        Spacer(Modifier.width(dimensions.space8))
        Column(horizontalAlignment = Alignment.End) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(dimensions.space4),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (favorite) {
              Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = NomeTheme.colors.accent,
              )
            }
            Text(
              text = timestamp,
              style = NomeTheme.typography.supporting,
              color = NomeTheme.colors.textTertiary,
            )
          }
          if (unreadCount > 0) {
            Box(
              modifier = Modifier
                .padding(top = dimensions.space4)
                .defaultMinSize(
                  minWidth = dimensions.icon,
                  minHeight = dimensions.icon,
                )
                .clip(CircleShape)
                .background(NomeTheme.colors.action),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = stringResource(R.string.nome_home_unread_marker, unreadCount),
                modifier = Modifier.padding(horizontal = dimensions.space4),
                style = NomeTheme.typography.supporting,
                color = NomeTheme.colors.onAction,
              )
            }
          } else if (chat.chatStats.unreadChat) {
            Box(
              modifier = Modifier
                .padding(top = dimensions.space4)
                .size(dimensions.space8)
                .clip(CircleShape)
                .background(NomeTheme.colors.action),
            )
          }
        }
      }
    }
    if (hasMenu) {
      NomeChatDropdownMenu(
        chatModel = chatModel,
        chat = chat,
        showMenu = showMenu,
        inProgress = inProgress,
      )
    }
  }
}

@Composable
private fun NomeChatDropdownMenu(
  chatModel: ChatModel,
  chat: Chat,
  showMenu: androidx.compose.runtime.MutableState<Boolean>,
  inProgress: androidx.compose.runtime.MutableState<Boolean>,
) {
  val showMarkRead =
    chat.chatStats.unreadCount > 0 ||
      chat.chatStats.unreadChat
  DefaultDropdownMenu(
    showMenu = showMenu,
  ) {
    when (val info = chat.chatInfo) {
      is ChatInfo.Direct ->
        ContactMenuItems(
          chat,
          info.contact,
          chatModel,
          showMenu,
          showMarkRead,
        )
      is ChatInfo.Group ->
        GroupMenuItems(
          chat,
          info.groupInfo,
          chatModel,
          showMenu,
          inProgress,
          showMarkRead,
        )
      is ChatInfo.Local ->
        NoteFolderMenuItems(
          chat,
          showMenu,
          showMarkRead,
        )
      is ChatInfo.ContactRequest ->
        ContactRequestMenuItems(
          chat.remoteHostId,
          info.apiId,
          chatModel,
          showMenu,
        )
      is ChatInfo.ContactConnection ->
        ContactConnectionMenuItems(
          chat.remoteHostId,
          info,
          chatModel,
          showMenu,
        )
      is ChatInfo.InvalidJSON -> Unit
    }
  }
}

@Composable
private fun nomeChatTypeDescription(info: ChatInfo): String =
  stringResource(
    when (info) {
      is ChatInfo.Direct ->
        if (
          info.contact.nextAcceptContactRequest &&
          info.contact.contactRequestId != null
        ) {
          R.string.nome_home_contact_request
        } else {
          R.string.nome_home_direct_chat
        }
      is ChatInfo.Group -> R.string.nome_home_group_chat
      is ChatInfo.Local -> R.string.nome_home_notes_chat
      is ChatInfo.ContactRequest -> R.string.nome_home_contact_request
      is ChatInfo.ContactConnection -> R.string.nome_home_connection_pending
      is ChatInfo.InvalidJSON -> R.string.nome_home_invalid_chat
    },
  )
