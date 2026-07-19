package chat.simplex.common.views.usersettings

import SectionBottomSpacer
import SectionTextFooter
import SectionView
import SectionViewSelectable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.AnnotatedString
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import kotlin.collections.ArrayList

@Composable
fun NotificationsSettingsView(
  chatModel: ChatModel,
  onClose: () -> Unit = { ModalManager.start.closeModal() },
) {
  val onNotificationPreviewModeSelected = { mode: NotificationPreviewMode ->
    chatModel.controller.appPrefs.notificationPreviewMode.set(mode.name)
    chatModel.notificationPreviewMode.value = mode
  }

  NotificationsSettingsLayout(
    notificationsMode = remember { chatModel.controller.appPrefs.notificationsMode.state },
    notificationPreviewMode = chatModel.notificationPreviewMode,
    onClose = onClose,
    showPage = { page ->
      if (appPlatform == AppPlatform.ANDROID) {
        ModalManager.start.showCustomModal { close ->
          when (page) {
            CurrentPage.NOTIFICATIONS_MODE ->
              NotificationsModeView(
                chatModel.controller.appPrefs.notificationsMode.state,
                close,
              ) {
                changeNotificationsMode(it, chatModel)
              }
            CurrentPage.NOTIFICATION_PREVIEW_MODE ->
              NotificationPreviewView(
                chatModel.notificationPreviewMode,
                close,
                onNotificationPreviewModeSelected,
              )
          }
        }
      } else {
        ModalManager.start.showModalCloseable(true) { close ->
          when (page) {
            CurrentPage.NOTIFICATIONS_MODE ->
              NotificationsModeView(
                chatModel.controller.appPrefs.notificationsMode.state,
                close,
              ) {
                changeNotificationsMode(it, chatModel)
              }
            CurrentPage.NOTIFICATION_PREVIEW_MODE ->
              NotificationPreviewView(
                chatModel.notificationPreviewMode,
                close,
                onNotificationPreviewModeSelected,
              )
          }
        }
      }
    },
  )
}

enum class CurrentPage {
  NOTIFICATIONS_MODE, NOTIFICATION_PREVIEW_MODE
}

@Composable
fun NotificationsSettingsLayout(
  notificationsMode: State<NotificationsMode>,
  notificationPreviewMode: State<NotificationPreviewMode>,
  onClose: () -> Unit,
  showPage: (CurrentPage) -> Unit,
) {
  val modes = remember { notificationModes() }
  val previewModes = remember { notificationPreviewModes() }
  val selectedMode = modes.firstOrNull { it.value == notificationsMode.value }
  val selectedPreview = previewModes.firstOrNull { it.value == notificationPreviewMode.value }

  PlatformNotificationsSettingsRoute(
    title = stringResource(MR.strings.notifications),
    modeTitle = stringResource(MR.strings.settings_notifications_mode_title),
    modeValue = selectedMode?.title ?: "",
    modeDescription = selectedMode?.description?.text ?: "",
    previewTitle = stringResource(MR.strings.settings_notification_preview_mode_title),
    previewValue = selectedPreview?.title ?: "",
    previewDescription = selectedPreview?.description?.text ?: "",
    onClose = onClose,
    onOpenMode = { showPage(CurrentPage.NOTIFICATIONS_MODE) },
    onOpenPreview = { showPage(CurrentPage.NOTIFICATION_PREVIEW_MODE) },
    legacyContent = {
      ColumnWithScrollBar {
        AppBarTitle(stringResource(MR.strings.notifications))
        SectionView(null) {
          if (appPlatform == AppPlatform.ANDROID) {
            SettingsActionItemWithContent(null, stringResource(MR.strings.settings_notifications_mode_title), { showPage(CurrentPage.NOTIFICATIONS_MODE) }) {
              Text(
                selectedMode?.title ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.secondary
              )
            }
          }
          SettingsActionItemWithContent(null, stringResource(MR.strings.settings_notification_preview_mode_title), { showPage(CurrentPage.NOTIFICATION_PREVIEW_MODE) }) {
            Text(
              selectedPreview?.title ?: "",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colors.secondary
            )
          }
          if (platform.androidIsXiaomiDevice() && (notificationsMode.value == NotificationsMode.PERIODIC || notificationsMode.value == NotificationsMode.SERVICE)) {
            SectionTextFooter(annotatedStringResource(MR.strings.xiaomi_ignore_battery_optimization))
          }
        }
        SectionBottomSpacer()
      }
    },
  )
}

@Composable
fun NotificationsModeView(
  notificationsMode: State<NotificationsMode>,
  onClose: () -> Unit,
  onNotificationsModeSelected: (NotificationsMode) -> Unit,
) {
  val modes = remember { notificationModes() }
  val title = stringResource(MR.strings.settings_notifications_mode_title).lowercase().capitalize(Locale.current)
  PlatformNotificationsModeRoute(
    title = title,
    choices = modes.map { NomeNotificationsModeChoice(it.value, it.title, it.description.text) },
    selected = notificationsMode.value,
    onClose = onClose,
    onSelected = onNotificationsModeSelected,
    legacyContent = {
      ColumnWithScrollBar {
        AppBarTitle(title)
        SectionViewSelectable(null, notificationsMode, modes, onNotificationsModeSelected)
        if (platform.androidIsXiaomiDevice() && (notificationsMode.value == NotificationsMode.PERIODIC || notificationsMode.value == NotificationsMode.SERVICE)) {
          SectionTextFooter(annotatedStringResource(MR.strings.xiaomi_ignore_battery_optimization))
        }
      }
    },
  )
}

@Composable
fun NotificationPreviewView(
  notificationPreviewMode: State<NotificationPreviewMode>,
  onClose: () -> Unit,
  onNotificationPreviewModeSelected: (NotificationPreviewMode) -> Unit,
) {
  val previewModes = remember { notificationPreviewModes() }
  val title = stringResource(MR.strings.settings_notification_preview_title)
  PlatformNotificationPreviewRoute(
    title = title,
    choices = previewModes.map { NomeNotificationPreviewChoice(it.value, it.title, it.description.text) },
    selected = notificationPreviewMode.value,
    onClose = onClose,
    onSelected = onNotificationPreviewModeSelected,
    legacyContent = {
      ColumnWithScrollBar {
        AppBarTitle(title)
        SectionViewSelectable(null, notificationPreviewMode, previewModes, onNotificationPreviewModeSelected)
      }
    },
  )
}

// mode, name, description
private fun notificationModes(): List<ValueTitleDesc<NotificationsMode>> {
  val res = ArrayList<ValueTitleDesc<NotificationsMode>>()
  res.add(
    ValueTitleDesc(
      NotificationsMode.OFF,
      generalGetString(MR.strings.notifications_mode_off),
      AnnotatedString(generalGetString(MR.strings.notifications_mode_off_desc)),
    )
  )
  res.add(
    ValueTitleDesc(
      NotificationsMode.PERIODIC,
      generalGetString(MR.strings.notifications_mode_periodic),
      AnnotatedString(generalGetString(MR.strings.notifications_mode_periodic_desc)),
    )
  )
  res.add(
    ValueTitleDesc(
      NotificationsMode.SERVICE,
      generalGetString(MR.strings.notifications_mode_service),
      AnnotatedString(generalGetString(MR.strings.notifications_mode_service_desc)),
    )
  )
  return res
}

// preview mode, name, description
fun notificationPreviewModes(): List<ValueTitleDesc<NotificationPreviewMode>> {
  val res = ArrayList<ValueTitleDesc<NotificationPreviewMode>>()
  res.add(
    ValueTitleDesc(
      NotificationPreviewMode.MESSAGE,
      generalGetString(MR.strings.notification_preview_mode_message),
      AnnotatedString(generalGetString(MR.strings.notification_preview_mode_message_desc)),
    )
  )
  res.add(
    ValueTitleDesc(
      NotificationPreviewMode.CONTACT,
      generalGetString(MR.strings.notification_preview_mode_contact),
      AnnotatedString(generalGetString(MR.strings.notification_preview_mode_contact_desc)),
    )
  )
  res.add(
    ValueTitleDesc(
      NotificationPreviewMode.HIDDEN,
      generalGetString(MR.strings.notification_preview_mode_hidden),
      AnnotatedString(generalGetString(MR.strings.notification_display_mode_hidden_desc)),
    )
  )
  return res
}

fun changeNotificationsMode(mode: NotificationsMode, chatModel: ChatModel) {
  chatModel.controller.appPrefs.notificationsMode.set(mode)
  platform.androidNotificationsModeChanged(mode)
}
