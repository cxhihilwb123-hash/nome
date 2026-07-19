package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable
import chat.simplex.common.model.NotificationPreviewMode
import chat.simplex.common.model.NotificationsMode

@Composable
internal actual fun PlatformNotificationsSettingsRoute(
  title: String,
  modeTitle: String,
  modeValue: String,
  modeDescription: String,
  previewTitle: String,
  previewValue: String,
  previewDescription: String,
  onClose: () -> Unit,
  onOpenMode: () -> Unit,
  onOpenPreview: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}

@Composable
internal actual fun PlatformNotificationsModeRoute(
  title: String,
  choices: List<NomeNotificationsModeChoice>,
  selected: NotificationsMode,
  onClose: () -> Unit,
  onSelected: (NotificationsMode) -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}

@Composable
internal actual fun PlatformNotificationPreviewRoute(
  title: String,
  choices: List<NomeNotificationPreviewChoice>,
  selected: NotificationPreviewMode,
  onClose: () -> Unit,
  onSelected: (NotificationPreviewMode) -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}
