package chat.simplex.common.views.chatlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Button
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.activation.ActivationAccess
import chat.simplex.common.activation.ActivationCapability
import chat.simplex.common.activation.ActivationGate
import chat.simplex.common.helpers.NetworkObserver
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatListLoadState
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.ChatController
import chat.simplex.common.model.ChatTag
import chat.simplex.common.model.getTimestampText
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.accessibility.NomeFocusRestoration
import chat.simplex.common.ui.nome.components.NomeBrandLockup
import chat.simplex.common.ui.nome.components.NomePrimaryBottomNavigation
import chat.simplex.common.ui.nome.components.NomePrimaryDestination
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
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.common.views.helpers.tryOrShowError
import chat.simplex.common.views.onboarding.SetNotificationsModeAdditions
import chat.simplex.common.views.contacts.onRequestAccepted
import chat.simplex.common.views.usersettings.SettingsView
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
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
  val primaryDestination =
    if (
      activeFilter is ActiveFilter.PresetTag &&
        activeFilter.tag == PresetTagKind.CONTACTS
    ) {
      NomePrimaryDestination.CONTACTS
    } else {
      NomePrimaryDestination.HOME
    }
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
        userLists = chatModel.userTags.value,
        allListsSelected = activeFilter == null,
        selectedUserListId =
          (activeFilter as? ActiveFilter.UserTag)?.tag?.chatTagId,
        onOpenProfile = {
          userPickerState.value = AnimatedViewState.VISIBLE
        },
        onSearch = {
          searchQuery.value = ""
          searchOpen.value = true
        },
        onNewConnection = {
          scope.launch {
            if (ActivationGate.guardFresh(ActivationCapability.CONTACT, "home_new_connection")) {
              showNewChatSheet(
                oneHandUI = oneHandUI,
                onOpenProfile = {
                  userPickerState.value = AnimatedViewState.VISIBLE
                },
              )
            }
          }
        },
        selectedDestination = primaryDestination,
        onOpenHome = {
          chatModel.activeChatTagFilter.value = null
        },
        onOpenContacts = {
          chatModel.activeChatTagFilter.value =
            ActiveFilter.PresetTag(PresetTagKind.CONTACTS)
        },
        onOpenSettings = {
          ModalManager.start.showCustomModal { close ->
            SettingsView(chatModel, setPerformLA, close)
          }
        },
        onSelectAllLists = {
          chatModel.activeChatTagFilter.value = null
        },
        onSelectUserList = { tag ->
          chatModel.activeChatTagFilter.value =
            if (
              chatModel.activeChatTagFilter.value ==
                ActiveFilter.UserTag(tag)
            ) {
              null
            } else {
              ActiveFilter.UserTag(tag)
            }
        },
        onAddUserList = {
          ModalManager.start.showCustomModal { close ->
            TagListEditor(
              rhId = chatModel.remoteHostId(),
              close = close,
            )
          }
        },
        onManageUserList = { tag ->
          ModalManager.start.showCustomModal { close ->
            TagListEditor(
              rhId = chatModel.remoteHostId(),
              tagId = tag.chatTagId,
              close = close,
              emoji = tag.chatTagEmoji,
              name = tag.chatTagText,
            )
          }
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
  selectedDestination: NomePrimaryDestination = NomePrimaryDestination.HOME,
  onOpenHome: () -> Unit = {},
  onOpenContacts: () -> Unit = {},
  onOpenSettings: () -> Unit = {},
  userLists: List<ChatTag> = emptyList(),
  allListsSelected: Boolean = true,
  selectedUserListId: Long? = null,
  onSelectAllLists: () -> Unit = {},
  onSelectUserList: (ChatTag) -> Unit = {},
  onAddUserList: () -> Unit = {},
  onManageUserList: (ChatTag) -> Unit = {},
) {
  val dimensions = NomeTheme.dimensions
  val activationState = ActivationGate.state.collectAsState().value
  val pendingActivationIntent = ActivationGate.pendingIntent.collectAsState().value
  val scope = rememberCoroutineScope()
  val pendingDeletionChats = chatModel.deletedChats.value.toSet()
  val selectedUserList =
    userLists.firstOrNull { it.chatTagId == selectedUserListId }
  val isContacts =
    selectedDestination == NomePrimaryDestination.CONTACTS
  BackHandler(
    enabled = isContacts,
    onBack = onOpenHome,
  )
  val pageTitle =
    when {
      isContacts -> stringResource(R.string.nome_contacts_title)
      selectedUserList != null -> selectedUserList.chatTagText
      else -> stringResource(R.string.nome_home_title)
    }
  val pageSubtitle =
    when {
      isContacts -> stringResource(R.string.nome_contacts_subtitle)
      selectedUserList != null ->
        stringResource(R.string.nome_home_selected_list_subtitle)
      else -> stringResource(R.string.nome_home_subtitle)
    }
  val searchLabel =
    when {
      isContacts -> stringResource(R.string.nome_contacts_search)
      selectedUserList != null ->
        stringResource(R.string.nome_home_selected_list_search)
      else -> stringResource(R.string.nome_home_search)
    }
  val sectionTitle =
    if (isContacts) {
      stringResource(R.string.nome_contacts_section)
    } else {
      stringResource(R.string.nome_home_section_chats)
    }
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

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(NomeTheme.colors.background)
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth(),
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = dimensions.screenHorizontalInset,
          top = dimensions.space12,
          end = dimensions.screenHorizontalInset,
          bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.Top,
      ) {
        item {
          Column(
            verticalArrangement =
              Arrangement.spacedBy(dimensions.space12),
          ) {
            NomeHomeHeader(
              chatModel = chatModel,
              profileNameOverride = profileNameOverride,
              onOpenProfile = onOpenProfile,
            )
            Column(
              verticalArrangement =
                Arrangement.spacedBy(dimensions.space2),
            ) {
              Text(
                text = pageTitle,
                modifier = Modifier.semantics { heading() },
                style = NomeTheme.typography.display,
                color = NomeTheme.colors.textPrimary,
              )
              Text(
                text = pageSubtitle,
                style = NomeTheme.typography.body,
                color = NomeTheme.colors.textSecondary,
              )
            }
            NomeHomeSearchBar(
              label = searchLabel,
              onSearch = onSearch,
            )
            if (!isContacts) {
              NomeHomeListFilters(
                userLists = userLists,
                allListsSelected = allListsSelected,
                selectedUserListId = selectedUserListId,
                onSelectAll = onSelectAllLists,
                onSelectList = onSelectUserList,
                onAddList = onAddUserList,
                onManageList = onManageUserList,
              )
            }
          }
        }

        if (activationState.shouldShowActivation) {
          item {
            NomeActivationCard(
              checking = activationState.access == ActivationAccess.CHECK_REQUIRED,
              migrationRequired = activationState.access == ActivationAccess.MIGRATION_REQUIRED,
              modifier = Modifier.padding(top = dimensions.space12),
            )
          }
        } else if (pendingActivationIntent?.capability == ActivationCapability.DEEP_LINK) {
          item {
            NomePendingLinkCard(
              modifier = Modifier.padding(top = dimensions.space12),
            )
          }
        }

        when (state.connectivity) {
          NomeHomeConnectivityState.UNKNOWN -> item {
            NomeStatusPanel(
              state = NomeStatePanelState.LOADING,
              title = R.string.nome_home_network_unknown_title,
              body = R.string.nome_home_network_unknown_body,
              stateDescription = R.string.nome_home_network_unknown_state,
              modifier = Modifier.padding(top = dimensions.space12),
            )
          }
          NomeHomeConnectivityState.DEVICE_OFFLINE -> item {
            NomeStatusPanel(
              state = NomeStatePanelState.OFFLINE,
              title = R.string.nome_home_offline_title,
              body = R.string.nome_home_offline_body,
              stateDescription = R.string.nome_home_offline_state,
              modifier = Modifier.padding(top = dimensions.space12),
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
              modifier = Modifier.padding(top = dimensions.space12),
            )
          }
        }

        when (state.content) {
          NomeHomeContentState.LOADING -> item {
            NomeHomeSkeleton(
              modifier = Modifier.padding(top = dimensions.space12),
            )
          }
          NomeHomeContentState.FIRST_USE -> item {
            NomeStatusPanel(
              state = NomeStatePanelState.EMPTY,
              title = R.string.nome_home_first_use_title,
              body = R.string.nome_home_first_use_body,
              stateDescription = R.string.nome_home_first_use_state,
              modifier = Modifier.padding(top = dimensions.space12),
            )
          }
          NomeHomeContentState.TRUE_EMPTY -> item {
            NomeStatusPanel(
              state = NomeStatePanelState.EMPTY,
              title = R.string.nome_home_empty_title,
              body = R.string.nome_home_empty_body,
              stateDescription = R.string.nome_home_empty_state,
              modifier = Modifier.padding(top = dimensions.space12),
            )
          }
          NomeHomeContentState.FILTERED_NO_RESULT -> item {
            NomeStatusPanel(
              state = NomeStatePanelState.EMPTY,
              title = R.string.nome_home_filtered_empty_title,
              body = R.string.nome_home_filtered_empty_body,
              stateDescription = R.string.nome_home_filtered_empty_state,
              modifier = Modifier.padding(top = dimensions.space12),
            )
          }
          NomeHomeContentState.UNAVAILABLE -> item {
            NomeStatusPanel(
              state = NomeStatePanelState.ERROR,
              title = R.string.nome_home_unavailable_title,
              body = R.string.nome_home_unavailable_body,
              stateDescription = R.string.nome_home_unavailable_state,
              modifier = Modifier.padding(top = dimensions.space12),
            )
          }
          NomeHomeContentState.POPULATED -> Unit
        }

        val showChatList =
          state.content == NomeHomeContentState.POPULATED ||
            (
              state.content == NomeHomeContentState.UNAVAILABLE &&
                state.visibleChats.isNotEmpty()
            )
        if (showChatList) {
          item {
            NomeChatsHeading(
              title = sectionTitle,
              modifier =
                Modifier.padding(
                  top = dimensions.space16,
                  bottom = dimensions.space8,
                ),
            )
          }
          itemsIndexed(
            items = state.visibleChats,
            key = { _, chat -> chat.remoteHostId to chat.id },
          ) { index, chat ->
            NomeChatRow(
              chatModel = chatModel,
              chat = chat,
              coreState = state.core,
              pendingDeletion = pendingDeletionChats.contains(chat.remoteHostId to chat.chatInfo.id),
              showChatPreviews = showChatPreviews,
              openChat = openChat,
              shape =
                nomeGroupedRowShape(
                  index = index,
                  count = state.visibleChats.size,
                ),
              showDivider = index < state.visibleChats.lastIndex,
            )
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
    NomePrimaryBottomNavigation(
      selected = selectedDestination,
      onDestinationSelected = { destination ->
        when (destination) {
          NomePrimaryDestination.HOME -> onOpenHome()
          NomePrimaryDestination.CONTACTS -> onOpenContacts()
          NomePrimaryDestination.SETTINGS -> onOpenSettings()
        }
      },
    )
  }
}

@Composable
private fun NomeActivationCard(
  checking: Boolean,
  migrationRequired: Boolean,
  modifier: Modifier = Modifier,
) {
  NomeSurface(
    modifier = modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.control,
    color = NomeTheme.colors.input,
    border = BorderStroke(1.dp, NomeTheme.colors.action),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = stringResource(R.string.nome_activation_card_title),
        style = NomeTheme.typography.title,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text = stringResource(
          when {
            migrationRequired -> R.string.nome_activation_card_migration
            checking -> R.string.nome_activation_card_checking
            else -> R.string.nome_activation_card_body
          },
        ),
        style = NomeTheme.typography.body,
        color = NomeTheme.colors.textSecondary,
      )
      Button(onClick = { ActivationGate.showActivation("home_activation_card") }) {
        Text(
          stringResource(
            if (migrationRequired) R.string.nome_activation_migrate else R.string.nome_activation_activate,
          ),
        )
      }
    }
  }
}

@Composable
private fun NomePendingLinkCard(modifier: Modifier = Modifier) {
  NomeSurface(
    modifier = modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.control,
    color = NomeTheme.colors.input,
    border = BorderStroke(1.dp, NomeTheme.colors.divider),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = stringResource(R.string.nome_activation_pending_link_title),
        style = NomeTheme.typography.title,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text = stringResource(R.string.nome_activation_pending_link_body),
        style = NomeTheme.typography.body,
        color = NomeTheme.colors.textSecondary,
      )
      Button(onClick = ActivationGate::showPendingReview) {
        Text(stringResource(R.string.nome_activation_review_link))
      }
    }
  }
}

@Composable
internal fun NomeHomeListFilters(
  userLists: List<ChatTag>,
  allListsSelected: Boolean,
  selectedUserListId: Long?,
  onSelectAll: () -> Unit,
  onSelectList: (ChatTag) -> Unit,
  onAddList: () -> Unit,
  onManageList: (ChatTag) -> Unit,
) {
  val selectedState = stringResource(R.string.nome_home_list_selected)
  val manageListLabel = stringResource(R.string.nome_home_manage_lists)
  val selectedList =
    userLists.firstOrNull { it.chatTagId == selectedUserListId }
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.control,
    color = NomeTheme.colors.input,
    border = BorderStroke(1.dp, NomeTheme.colors.divider),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NomeHomeListFilterChip(
        label = stringResource(R.string.nome_home_list_all),
        selected = allListsSelected,
        selectedState = selectedState,
        onClick = onSelectAll,
      )
      userLists.forEach { tag ->
        NomeHomeListFilterChip(
          label = tag.chatTagText,
          emoji = tag.chatTagEmoji,
          selected = tag.chatTagId == selectedUserListId,
          selectedState = selectedState,
          onClick = { onSelectList(tag) },
          manageLabel = manageListLabel,
          onManage = { onManageList(tag) },
        )
      }
      NomeHomeListAction(
        label =
          stringResource(
            if (selectedList == null) {
              R.string.nome_home_add_list
            } else {
              R.string.nome_home_manage_lists
            },
          ),
        managing = selectedList != null,
        onClick = {
          if (selectedList == null) {
            onAddList()
          } else {
            onManageList(selectedList)
          }
        },
      )
    }
  }
}

@Composable
private fun NomeHomeListFilterChip(
  label: String,
  selected: Boolean,
  selectedState: String,
  onClick: () -> Unit,
  emoji: String? = null,
  manageLabel: String? = null,
  onManage: (() -> Unit)? = null,
) {
  val accessibilityLabel =
    stringResource(R.string.nome_home_list_filter, label)
  NomeSurface(
    modifier =
      Modifier
        .heightIn(min = 48.dp)
        .combinedClickable(
          onClick = onClick,
          onLongClick = {
            onManage?.invoke()
          },
        )
        .nomeMinimumTouchTarget()
        .semantics {
          this.selected = selected
          if (manageLabel != null && onManage != null) {
            customActions =
              listOf(
                CustomAccessibilityAction(
                  label = manageLabel,
                  action = {
                    onManage()
                    true
                  },
                ),
              )
          }
        }
        .nomeTalkBackSemantics(
          label = accessibilityLabel,
          state = selectedState.takeIf { selected },
          role = Role.Tab,
        ),
    shape = NomeTheme.shapes.pill,
    color =
      if (selected) {
        NomeTheme.colors.successContainer
      } else {
        androidx.compose.ui.graphics.Color.Transparent
      },
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (emoji != null) {
        Text(
          text = emoji,
          style = NomeTheme.typography.body,
        )
      }
      Text(
        text = label,
        style = NomeTheme.typography.label,
        color =
          if (selected) {
            NomeTheme.colors.success
          } else {
            NomeTheme.colors.textPrimary
          },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun NomeHomeListAction(
  label: String,
  managing: Boolean,
  onClick: () -> Unit,
) {
  NomeSurface(
    modifier =
      Modifier
        .heightIn(min = 48.dp)
        .clickable(onClick = onClick)
        .nomeMinimumTouchTarget()
        .nomeTalkBackSemantics(
          label = label,
          role = Role.Button,
        ),
    shape = NomeTheme.shapes.pill,
    color = androidx.compose.ui.graphics.Color.Transparent,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (managing) {
        Icon(
          painter = painterResource(MR.images.ic_label),
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = NomeTheme.colors.action,
        )
      } else {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = NomeTheme.colors.action,
        )
      }
      Text(
        text = label,
        style = NomeTheme.typography.label,
        color = NomeTheme.colors.action,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun NomeHomeSearchBar(
  label: String,
  onSearch: () -> Unit,
) {
  NomeSurface(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clickable(onClick = onSearch)
      .nomeTalkBackSemantics(
        label = label,
        role = Role.Button,
      ),
    shape = NomeTheme.shapes.control,
    color = NomeTheme.colors.input,
    border = BorderStroke(1.dp, NomeTheme.colors.divider),
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
        text = label,
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
        info.contactCard ||
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
    NomeBrandLockup(
      contentDescription = stringResource(R.string.nome_home_brand),
      modifier = Modifier.width(104.dp),
    )
    Spacer(
      modifier = Modifier.weight(1f),
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
}

@Composable
private fun NomeChatsHeading(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    modifier = modifier.semantics { heading() },
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
  modifier: Modifier = Modifier,
) {
  NomeStatePanel(
    state = state,
    title = stringResource(title),
    description = stringResource(body),
    stateDescription = stringResource(stateDescription),
    modifier = modifier.fillMaxWidth(),
  )
}

@Composable
private fun NomeHomeSkeleton(
  modifier: Modifier = Modifier,
) {
  val dimensions = NomeTheme.dimensions
  NomeSurface(
    modifier = modifier
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
  shape: Shape,
  showDivider: Boolean,
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
        info.contactCard ||
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
  val canOpenReadOnlyMenu =
    info is ChatInfo.ContactConnection &&
      !pendingDeletion
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
    } else if (canOpenReadOnlyMenu) {
      Modifier
        .pointerInput(chat.id) {
          detectTapGestures(
            onLongPress = {
              showMenu.value = true
            },
          )
        }
        .nomeMinimumTouchTarget()
        .nomeTalkBackSemantics(
          label = spokenLabel,
          state = spokenState.takeIf { it.isNotBlank() },
        )
        .semantics {
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
      shape = shape,
      color = NomeTheme.colors.surfaceContainer,
    ) {
      Column {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 72.dp)
              .padding(
                horizontal = dimensions.space12,
                vertical = dimensions.space8,
              ),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          NomeChatAvatar(info)
          Spacer(Modifier.width(dimensions.space12))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = info.chatViewName,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = NomeTheme.typography.bodyStrong,
              color = NomeTheme.colors.textPrimary,
            )
            Text(
              text = preview,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = NomeTheme.typography.supporting,
              color = NomeTheme.colors.textSecondary,
            )
          }
          Spacer(Modifier.width(dimensions.space8))
          Column(horizontalAlignment = Alignment.End) {
            Row(
              horizontalArrangement =
                Arrangement.spacedBy(dimensions.space4),
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
                  text =
                    stringResource(
                      R.string.nome_home_unread_marker,
                      unreadCount,
                    ),
                  modifier =
                    Modifier.padding(horizontal = dimensions.space4),
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
        if (showDivider) {
          Divider(
            modifier =
              Modifier.padding(
                start = 68.dp,
                end = dimensions.space12,
              ),
            color = NomeTheme.colors.divider,
          )
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
private fun NomeChatAvatar(info: ChatInfo) {
  val image = info.image
  if (image != null) {
    ProfileImage(
      size = 44.dp,
      image = image,
      color = NomeTheme.colors.action,
      backgroundColor = NomeTheme.colors.surface,
    )
  } else {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(NomeTheme.colors.surface),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = info.chatViewName.firstOrNull()?.uppercase() ?: "?",
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.action,
      )
    }
  }
}

private fun nomeGroupedRowShape(
  index: Int,
  count: Int,
): Shape =
  when {
    count <= 1 -> RoundedCornerShape(16.dp)
    index == 0 ->
      RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
      )
    index == count - 1 ->
      RoundedCornerShape(
        bottomStart = 16.dp,
        bottomEnd = 16.dp,
      )
    else -> RectangleShape
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
