package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable
import chat.simplex.common.model.NotificationPreviewMode
import chat.simplex.common.model.NotificationsMode

data class NomeNotificationsModeChoice(
  val value: NotificationsMode,
  val title: String,
  val description: String,
)

data class NomeNotificationPreviewChoice(
  val value: NotificationPreviewMode,
  val title: String,
  val description: String,
)

/**
 * Android presentation seams for the official notification preferences.
 *
 * The common owner continues to read and mutate the official app preferences and to dispatch the
 * platform notification-mode transition. Android only changes the page composition.
 */
@Composable
internal expect fun PlatformNotificationsSettingsRoute(
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
)

@Composable
internal expect fun PlatformNotificationsModeRoute(
  title: String,
  choices: List<NomeNotificationsModeChoice>,
  selected: NotificationsMode,
  onClose: () -> Unit,
  onSelected: (NotificationsMode) -> Unit,
  legacyContent: @Composable () -> Unit,
)

@Composable
internal expect fun PlatformNotificationPreviewRoute(
  title: String,
  choices: List<NomeNotificationPreviewChoice>,
  selected: NotificationPreviewMode,
  onClose: () -> Unit,
  onSelected: (NotificationPreviewMode) -> Unit,
  legacyContent: @Composable () -> Unit,
)
