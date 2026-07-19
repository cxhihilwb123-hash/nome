package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformAboutSettingsRoute(
  onClose: (() -> Unit)?,
  onOpenVersion: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}
