package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformSettingsDetailRoute(
  title: String,
  onClose: (() -> Unit)?,
  groupedContent: Boolean,
  legacyContent: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  legacyContent()
}
