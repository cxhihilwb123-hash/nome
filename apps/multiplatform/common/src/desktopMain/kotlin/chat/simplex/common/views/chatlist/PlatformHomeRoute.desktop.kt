package chat.simplex.common.views.chatlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.ChatModel
import chat.simplex.common.ui.theme.NOME_DESKTOP_NAV_RAIL_WIDTH
import chat.simplex.common.views.helpers.AnimatedViewState
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.common.views.usersettings.SettingsView
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.MutableStateFlow

internal enum class NomeDesktopDestination {
  Messages,
  Contacts,
  Settings,
}

internal fun nomeDesktopDestination(activeFilter: ActiveFilter?): NomeDesktopDestination =
  if (activeFilter is ActiveFilter.PresetTag && activeFilter.tag == PresetTagKind.CONTACTS) {
    NomeDesktopDestination.Contacts
  } else {
    NomeDesktopDestination.Messages
  }

@Composable
actual fun PlatformHomeRoute(
  chatModel: ChatModel,
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  stopped: Boolean,
  defaultContent: @Composable () -> Unit,
) {
  val selected = remember {
    mutableStateOf(nomeDesktopDestination(chatModel.activeChatTagFilter.value))
  }
  val activeFilter = chatModel.activeChatTagFilter.value

  LaunchedEffect(activeFilter) {
    if (selected.value != NomeDesktopDestination.Settings) {
      selected.value = nomeDesktopDestination(activeFilter)
    }
  }

  fun showMessages() {
    ModalManager.start.closeModals()
    chatModel.activeChatTagFilter.value = null
    selected.value = NomeDesktopDestination.Messages
  }

  fun showContacts() {
    ModalManager.start.closeModals()
    chatModel.activeChatTagFilter.value = ActiveFilter.PresetTag(PresetTagKind.CONTACTS)
    selected.value = NomeDesktopDestination.Contacts
  }

  fun showSettings() {
    val returnDestination = nomeDesktopDestination(chatModel.activeChatTagFilter.value)
    ModalManager.start.closeModals()
    selected.value = NomeDesktopDestination.Settings
    ModalManager.start.showModalCloseable { close ->
      DisposableEffect(returnDestination) {
        onDispose {
          selected.value =
            nomeDesktopDestination(chatModel.activeChatTagFilter.value)
        }
      }
      SettingsView(
        chatModel = chatModel,
        setPerformLA = setPerformLA,
        close = {
          close()
          selected.value = returnDestination
        },
      )
    }
  }

  fun showUserPicker() {
    ModalManager.start.closeModals()
    selected.value = nomeDesktopDestination(chatModel.activeChatTagFilter.value)
    userPickerState.value = AnimatedViewState.VISIBLE
  }

  Row(Modifier.fillMaxSize().background(NomeDesktopColors.Workspace)) {
    NomeDesktopNavigationRail(
      chatModel = chatModel,
      selected = selected,
      onMessages = ::showMessages,
      onContacts = ::showContacts,
      onSettings = ::showSettings,
      onUserPicker = ::showUserPicker,
    )
    Box(
      Modifier
        .width(1.dp)
        .fillMaxHeight()
        .background(NomeDesktopColors.RailDivider),
    )
    Box(
      Modifier
        .weight(1f)
        .fillMaxHeight()
        .background(MaterialTheme.colors.background),
    ) {
      defaultContent()
    }
  }
}

@Composable
private fun NomeDesktopNavigationRail(
  chatModel: ChatModel,
  selected: MutableState<NomeDesktopDestination>,
  onMessages: () -> Unit,
  onContacts: () -> Unit,
  onSettings: () -> Unit,
  onUserPicker: () -> Unit,
) {
  val userPickerLabel = stringResource(MR.strings.your_chat_profiles)
  Column(
    modifier =
      Modifier
        .width(NOME_DESKTOP_NAV_RAIL_WIDTH)
        .fillMaxHeight()
        .background(NomeDesktopColors.Rail)
        .padding(vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Image(
      painter = painterResource(MR.images.nome_app_icon),
      contentDescription = "Nome",
      modifier =
        Modifier
          .size(60.dp)
          .clip(RoundedCornerShape(14.dp)),
    )
    Spacer(Modifier.height(30.dp))
    Column(
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      NomeDesktopDestinationButton(
        label = stringResource(MR.strings.nome_desktop_nav_messages),
        icon = painterResource(MR.images.ic_chat),
        selected = selected.value == NomeDesktopDestination.Messages,
        onClick = onMessages,
      )
      NomeDesktopDestinationButton(
        label = stringResource(MR.strings.nome_desktop_nav_contacts),
        icon = painterResource(MR.images.ic_person),
        selected = selected.value == NomeDesktopDestination.Contacts,
        onClick = onContacts,
      )
      NomeDesktopDestinationButton(
        label = stringResource(MR.strings.nome_desktop_nav_settings),
        icon = painterResource(MR.images.ic_settings),
        selected = selected.value == NomeDesktopDestination.Settings,
        onClick = onSettings,
      )
    }
    Spacer(Modifier.weight(1f))
    Box(
      Modifier
        .width(54.dp)
        .height(1.dp)
        .background(NomeDesktopColors.RailDivider),
    )
    Spacer(Modifier.height(18.dp))
    Box(
      Modifier
        .size(42.dp)
        .clip(CircleShape)
        .semantics { contentDescription = userPickerLabel }
        .clickable(role = Role.Button, onClick = onUserPicker),
      contentAlignment = Alignment.Center,
    ) {
      ProfileImage(
        size = 40.dp,
        image = chatModel.currentUser.value?.image,
        color = NomeDesktopColors.RailMuted,
        backgroundColor = NomeDesktopColors.RailSelected,
      )
      Box(
        Modifier
          .align(Alignment.BottomEnd)
          .size(10.dp)
          .background(
            if (chatModel.chatRunning.value == true) {
              NomeDesktopColors.RailOnline
            } else {
              NomeDesktopColors.RailMuted
            },
            CircleShape,
          )
          .border(2.dp, NomeDesktopColors.Rail, CircleShape),
      )
    }
  }
}

@Composable
private fun NomeDesktopDestinationButton(
  label: String,
  icon: Painter,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val contentColor =
    if (selected) {
      NomeDesktopColors.RailSelectedContent
    } else {
      NomeDesktopColors.RailMuted
    }
  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(70.dp)
        .background(
          if (selected) {
            NomeDesktopColors.RailSelected
          } else {
            Color.Transparent
          },
        )
        .semantics(mergeDescendants = true) { this.selected = selected }
        .clickable(role = Role.Button, onClick = onClick)
  ) {
    if (selected) {
      Box(
        Modifier
          .align(Alignment.CenterStart)
          .width(4.dp)
          .height(48.dp)
          .background(
            NomeDesktopColors.RailActiveIndicator,
            RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
          ),
      )
    }
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        painter = icon,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = contentColor,
      )
      Text(
        text = label,
        color = contentColor,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
      )
    }
  }
}

private object NomeDesktopColors {
  val Rail = Color(0xFF0E1B2D)
  val RailDivider = Color(0xFF182A3F)
  val RailMuted = Color(0xFFA9B8C9)
  val RailSelected = Color(0xFF1B3448)
  val RailSelectedContent = Color(0xFFD7F6E8)
  val RailActiveIndicator = Color(0xFF16AE66)
  val RailOnline = Color(0xFF35C778)
  val Workspace = Color(0xFFF5F7FA)
}
