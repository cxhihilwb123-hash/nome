package chat.simplex.common.views.usersettings

import SectionBottomSpacer
import SectionDivider
import SectionItemView
import SectionItemViewSpaceBetween
import SectionItemViewWithoutMinPadding
import SectionSpacer
import SectionTextFooter
import SectionView
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatModel.controller
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.chat.item.ItemAction
import chat.simplex.common.views.chatlist.UserProfilePickerItem
import chat.simplex.common.views.chatlist.UserProfileRow
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.CreateProfile
import chat.simplex.common.views.database.*
import chat.simplex.common.views.onboarding.OnboardingStage
import chat.simplex.common.views.usersettings.networkAndServers.NetworkAndServersView
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.*

@Composable
fun UserProfilesView(
  m: ChatModel,
  search: MutableState<String>,
  profileHidden: MutableState<Boolean>,
  onClose: () -> Unit = {},
  onOpenHome: () -> Unit = {},
  onOpenContacts: () -> Unit = {},
  withAuth: (block: () -> Unit) -> Unit,
) {
  val searchTextOrPassword = rememberSaveable { search }
  val users by remember { derivedStateOf { m.users.map { it.user } } }
  val filteredUsers by remember { derivedStateOf { filteredUsers(m, searchTextOrPassword.value) } }
  val showHiddenProfilesNotice = m.controller.appPrefs.showHiddenProfilesNotice
  val visibleUsersCount = visibleUsersCount(m)
  val incognitoPreference = m.controller.appPrefs.incognito
  val incognitoDefault by remember { incognitoPreference.state }
  val socksProxyEnabled by remember {
    m.controller.appPrefs.networkUseSocksProxy.state
  }
  val addUser: () -> Unit = {
    withAuth {
      ModalManager.center.showModalCloseable { close ->
        CreateProfile(m, close)
      }
    }
  }
  val activateUser: (User) -> Unit = { user ->
    if (appPlatform.isDesktop) {
      ModalManager.center.closeModals()
      ModalManager.end.closeModals()
    }
    withBGApi {
      controller.showProgressIfNeeded {
        m.controller.changeActiveUser(user.remoteHostId, user.userId, userViewPassword(user, searchTextOrPassword.value.trim()))
      }
    }
  }
  val removeUser: (User) -> Unit = { user ->
    withAuth {
      val text = buildAnnotatedString {
        append(generalGetString(MR.strings.users_delete_all_chats_deleted) + "\n\n" + generalGetString(MR.strings.users_delete_profile_for) + " ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
          append(user.displayName)
        }
        append(":")
      }
      AlertManager.shared.showAlertDialogButtonsColumn(
        title = generalGetString(MR.strings.users_delete_question),
        text = text,
        buttons = {
          Column {
            SectionItemView({
              AlertManager.shared.hideAlert()
              removeUser(m, user, users, true, searchTextOrPassword.value.trim())
            }) {
              Text(stringResource(MR.strings.users_delete_with_connections), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Red)
            }
            SectionItemView({
              AlertManager.shared.hideAlert()
              removeUser(m, user, users, false, searchTextOrPassword.value.trim())
            }
            ) {
              Text(stringResource(MR.strings.users_delete_data_only), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Red)
            }
          }
        }
      )
    }
  }
  val unhideUser: (User) -> Unit = { user ->
    withAuth {
      if (passwordEntryRequired(user, searchTextOrPassword.value)) {
        ModalManager.start.showModalCloseable(true) { close ->
          ProfileActionView(UserProfileAction.UNHIDE, user) { pwd ->
            withBGApi {
              setUserPrivacy(m) { m.controller.apiUnhideUser(user, pwd) }
              close()
            }
          }
        }
      } else {
        withBGApi { setUserPrivacy(m) { m.controller.apiUnhideUser(user, searchTextOrPassword.value.trim()) } }
      }
    }
  }
  val muteUser: (User) -> Unit = { user ->
    withAuth {
      withBGApi {
        setUserPrivacy(m, onSuccess = {
          if (m.controller.appPrefs.showMuteProfileAlert.get()) showMuteProfileAlert(m.controller.appPrefs.showMuteProfileAlert)
        }) { m.controller.apiMuteUser(user) }
      }
    }
  }
  val unmuteUser: (User) -> Unit = { user ->
    withAuth {
      withBGApi { setUserPrivacy(m) { m.controller.apiUnmuteUser(user) } }
    }
  }
  val showHiddenProfile: (User) -> Unit = { user ->
    withAuth {
      ModalManager.start.showModalCloseable(true) { close ->
        HiddenProfileView(m, user) {
          profileHidden.value = true
          withBGApi {
            delay(10_000)
            profileHidden.value = false
          }
          close()
        }
      }
    }
  }
  LaunchedEffect(Unit) {
    if (showHiddenProfilesNotice.state.value && users.size > 1) {
      AlertManager.shared.showAlertDialog(
        title = generalGetString(MR.strings.make_profile_private),
        text = generalGetString(MR.strings.you_can_hide_or_mute_user_profile),
        confirmText = generalGetString(MR.strings.ok),
        dismissText = generalGetString(MR.strings.dont_show_again),
        onDismiss = {
          showHiddenProfilesNotice.set(false)
        },
      )
    }
  }
  PlatformIdentityCenterRoute(
    users = users,
    filteredUsers = filteredUsers,
    searchTextOrPassword = searchTextOrPassword,
    profileHidden = profileHidden.value,
    visibleUsersCount = visibleUsersCount,
    incognitoDefault = incognitoDefault,
    socksProxyEnabled = socksProxyEnabled,
    onClose = onClose,
    onRevealPasswordEntry = {
      profileHidden.value = false
    },
    onAddUser = addUser,
    onEditCurrentUser = {
      withAuth {
        ModalManager.center.showModalCloseable { close ->
          UserProfileView(m, close)
        }
      }
    },
    onActivateUser = activateUser,
    onRemoveUser = removeUser,
    onUnhideUser = unhideUser,
    onMuteUser = muteUser,
    onUnmuteUser = unmuteUser,
    onHideUser = showHiddenProfile,
    onSetIncognitoDefault = {
      incognitoPreference.set(it)
    },
    onOpenIncognitoInfo = {
      ModalManager.end.showModal {
        IncognitoView()
      }
    },
    onOpenNetworkSettings = {
      ModalManager.start.showCustomModal { close ->
        NetworkAndServersView(close)
      }
    },
    onOpenHome = onOpenHome,
    onOpenContacts = onOpenContacts,
    legacyContent = {
      UserProfilesLayout(
        filteredUsers = filteredUsers,
        profileHidden = profileHidden,
        searchTextOrPassword = searchTextOrPassword,
        visibleUsersCount = visibleUsersCount,
        addUser = addUser,
        activateUser = activateUser,
        removeUser = removeUser,
        unhideUser = unhideUser,
        muteUser = muteUser,
        unmuteUser = unmuteUser,
        showHiddenProfile = showHiddenProfile,
      )
    },
  )
  KeyChangeEffect(remember { m.currentRemoteHost }.value) {
    ModalManager.start.closeModal()
  }
}

@Composable
private fun UserProfilesLayout(
  filteredUsers: List<UserInfo>,
  searchTextOrPassword: MutableState<String>,
  profileHidden: MutableState<Boolean>,
  visibleUsersCount: Int,
  addUser: () -> Unit,
  activateUser: (User) -> Unit,
  removeUser: (User) -> Unit,
  unhideUser: (User) -> Unit,
  muteUser: (User) -> Unit,
  unmuteUser: (User) -> Unit,
  showHiddenProfile: (User) -> Unit,
) {
  ColumnWithScrollBar {
    if (profileHidden.value) {
      SectionView {
        SettingsActionItem(painterResource(MR.images.ic_lock_open_right), stringResource(MR.strings.enter_password_to_show), click = {
          profileHidden.value = false
        })
      }
      SectionSpacer()
    }
    AppBarTitle(stringResource(MR.strings.your_chat_profiles), hostDevice(remember { chatModel.remoteHostId() }))

    SectionView {
      for (user in filteredUsers) {
        UserView(user, visibleUsersCount, activateUser, removeUser, unhideUser, muteUser, unmuteUser, showHiddenProfile)
        SectionDivider()
      }
      if (searchTextOrPassword.value.trim().isEmpty()) {
        SectionItemView(addUser, minHeight = 68.dp) {
          Icon(painterResource(MR.images.ic_add), stringResource(MR.strings.users_add), tint = MaterialTheme.colors.primary)
          Spacer(Modifier.padding(horizontal = 4.dp))
          Text(stringResource(MR.strings.users_add), color = MaterialTheme.colors.primary)
        }
      }
    }
    SectionTextFooter(stringResource(MR.strings.tap_to_activate_profile))
    SectionBottomSpacer()
  }
}

@Composable
private fun UserView(
  userInfo: UserInfo,
  visibleUsersCount: Int,
  activateUser: (User) -> Unit,
  removeUser: (User) -> Unit,
  unhideUser: (User) -> Unit,
  muteUser: (User) -> Unit,
  unmuteUser: (User) -> Unit,
  showHiddenProfile: (User) -> Unit,
) {
  val showMenu = remember { mutableStateOf(false) }
  val user = userInfo.user
  UserProfilePickerItem(user, onLongClick = { showMenu.value = true }, unreadCount = userInfo.unreadCount) {
    activateUser(user)
  }
  Box(Modifier.padding(horizontal = DEFAULT_PADDING)) {
    DefaultDropdownMenu(showMenu) {
      if (user.hidden) {
        ItemAction(stringResource(MR.strings.user_unhide), painterResource(MR.images.ic_lock_open_right), onClick = {
          showMenu.value = false
          unhideUser(user)
        })
      } else {
        if (visibleUsersCount > 1) {
          ItemAction(stringResource(MR.strings.user_hide), painterResource(MR.images.ic_lock), onClick = {
            showMenu.value = false
            showHiddenProfile(user)
          })
        }
        if (user.showNtfs) {
          ItemAction(stringResource(MR.strings.user_mute), painterResource(MR.images.ic_notifications_off), onClick = {
            showMenu.value = false
            muteUser(user)
          })
        } else {
          ItemAction(stringResource(MR.strings.user_unmute), painterResource(MR.images.ic_notifications), onClick = {
            showMenu.value = false
            unmuteUser(user)
          })
        }
      }
      ItemAction(stringResource(MR.strings.delete_verb), painterResource(MR.images.ic_delete), color = Color.Red, onClick = {
        removeUser(user)
        showMenu.value = false
      })
    }
  }
}

enum class UserProfileAction {
  DELETE,
  UNHIDE
}

@Composable
private fun ProfileActionView(action: UserProfileAction, user: User, doAction: (String) -> Unit) {
  ColumnWithScrollBar {
    val actionPassword = rememberSaveable { mutableStateOf("") }
    val passwordValid by remember { derivedStateOf { actionPassword.value == actionPassword.value.trim() } }
    val actionEnabled by remember { derivedStateOf { actionPassword.value != "" && passwordValid && correctPassword(user, actionPassword.value) } }

    @Composable fun ActionHeader(title: StringResource) {
      AppBarTitle(stringResource(title))
      SectionView(contentPadding = PaddingValues(start = 8.dp, end = DEFAULT_PADDING)) {
        UserProfileRow(user)
      }
      SectionSpacer()
    }

    @Composable fun PasswordAndAction(label: StringResource, color: Color = MaterialTheme.colors.primary) {
      SectionView() {
        SectionItemViewWithoutMinPadding {
          PassphraseField(actionPassword, generalGetString(MR.strings.profile_password), isValid = { passwordValid }, showStrength = true)
        }
        SectionItemViewSpaceBetween({ doAction(actionPassword.value) }, disabled = !actionEnabled, minHeight = TextFieldDefaults.MinHeight) {
          Text(generalGetString(label), color = if (actionEnabled) color else MaterialTheme.colors.secondary)
        }
      }
    }

    when (action) {
      UserProfileAction.DELETE -> {
        ActionHeader(MR.strings.delete_profile)
        PasswordAndAction(MR.strings.delete_chat_profile, color = Color.Red)
        if (actionEnabled) {
          SectionTextFooter(stringResource(MR.strings.users_delete_all_chats_deleted))
        }
      }
      UserProfileAction.UNHIDE -> {
        ActionHeader(MR.strings.unhide_profile)
        PasswordAndAction(MR.strings.unhide_chat_profile)
      }
    }
    SectionBottomSpacer()
  }
}

fun filteredUsers(m: ChatModel, searchTextOrPassword: String): List<UserInfo> {
  val s = searchTextOrPassword.trim()
  val lower = s.lowercase()
  return m.users.filter { u ->
    if ((u.user.activeUser || !u.user.hidden) && (s == "" || u.user.anyNameContains(lower))) {
      true
    } else {
      correctPassword(u.user, s)
    }
  }
}

private fun visibleUsersCount(m: ChatModel): Int = m.users.filter { u -> !u.user.hidden }.size

fun correctPassword(user: User, pwd: String): Boolean {
  val ph = user.viewPwdHash
  return ph != null && pwd != "" && chatPasswordHash(pwd, ph.salt) == ph.hash
}

private fun userViewPassword(user: User, searchTextOrPassword: String): String? =
  if (user.hidden) searchTextOrPassword.trim() else null

private fun passwordEntryRequired(user: User, searchTextOrPassword: String): Boolean =
  user.hidden && user.activeUser && !correctPassword(user, searchTextOrPassword.trim())

private fun removeUser(m: ChatModel, user: User, users: List<User>, delSMPQueues: Boolean, searchTextOrPassword: String) {
  if (passwordEntryRequired(user, searchTextOrPassword)) {
    ModalManager.start.showModalCloseable(true) { close ->
      ProfileActionView(UserProfileAction.DELETE, user) { pwd ->
        withBGApi {
          doRemoveUser(m, user, users, delSMPQueues, pwd)
          close()
        }
      }
    }
  } else {
    withBGApi { doRemoveUser(m, user, users, delSMPQueues, userViewPassword(user, searchTextOrPassword.trim())) }
  }
}

private suspend fun doRemoveUser(m: ChatModel, user: User, users: List<User>, delSMPQueues: Boolean, viewPwd: String?) {
  val fallbackUser =
    users.firstOrNull { candidate ->
      !candidate.activeUser && !candidate.hidden
    }
  val result =
    runUserDeletionLifecycle(
      targetWasActive = user.activeUser,
      fallbackUserId = fallbackUser?.userId,
      stopAfterLastVisibleUser = appPlatform.isAndroid,
      switchToFallback = { fallbackUserId ->
        m.controller.changeActiveUser_(
          user.remoteHostId,
          fallbackUserId,
          null,
        )
      },
      deleteTarget = {
        m.controller.apiDeleteUser(
          user,
          delSMPQueues,
          viewPwd,
        )
      },
      clearActiveUser = {
        m.controller.changeActiveUser_(
          user.remoteHostId,
          null,
          null,
        )
      },
      stopChat = {
        m.controller.apiStopChat()
      },
    )

  if (result.targetDeleted) {
    finalizeDeletedUser(m, user)
  }

  when (result) {
    is UserDeletionResult.Completed -> {
      if (result.chatStopped) {
        controller.appPrefs.onboardingStage.set(
          OnboardingStage.Step1_SimpleXInfo,
        )
        ModalManager.closeAllModalsEverywhere()
      }
    }
    is UserDeletionResult.Failed -> {
      Log.e(
        TAG,
        "User deletion failed at ${result.stage}",
      )
      AlertManager.shared.showAlertMsg(
        generalGetString(
          MR.strings.error_deleting_user,
        ),
        userDeletionFailureMessage(result),
      )
    }
  }
}

private fun finalizeDeletedUser(
  m: ChatModel,
  user: User,
) {
  if (user.activeUser) {
    removeWallpaperFilesFromAllChats(user)
  }
  removeWallpaperFilesFromTheme(user.uiThemes)
  m.removeUser(user)
  ntfManager.cancelNotificationsForUser(user.userId)
}

private fun userDeletionFailureMessage(
  result: UserDeletionResult.Failed,
): String =
  when (result.stage) {
    UserDeletionStage.SWITCH_TO_FALLBACK ->
      generalGetString(
        MR.strings.identity_delete_switch_failed,
      )
    UserDeletionStage.DELETE_TARGET ->
      generalGetString(
        if (result.fallbackUserId != null) {
          MR.strings.identity_delete_after_switch_failed
        } else {
          MR.strings.identity_delete_target_failed
        },
      )
    UserDeletionStage.CLEAR_ACTIVE_USER ->
      generalGetString(
        MR.strings.identity_delete_refresh_failed,
      )
    UserDeletionStage.STOP_CHAT ->
      generalGetString(
        MR.strings.identity_delete_stop_failed,
      )
  }

private suspend fun setUserPrivacy(m: ChatModel, onSuccess: (() -> Unit)? = null, api: suspend () -> User) {
  try {
    m.updateUser(api())
    onSuccess?.invoke()
  } catch (e: Exception) {
    AlertManager.shared.showAlertMsg(
      title = generalGetString(MR.strings.error_updating_user_privacy),
      text = e.stackTraceToString()
    )
  }
}

private fun showMuteProfileAlert(showMuteProfileAlert: SharedPreference<Boolean>) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(MR.strings.muted_when_inactive),
    text = generalGetString(MR.strings.you_will_still_receive_calls_and_ntfs),
    confirmText = generalGetString(MR.strings.ok),
    dismissText = generalGetString(MR.strings.dont_show_again),
    onDismiss = {
      showMuteProfileAlert.set(false)
    },
  )
}
