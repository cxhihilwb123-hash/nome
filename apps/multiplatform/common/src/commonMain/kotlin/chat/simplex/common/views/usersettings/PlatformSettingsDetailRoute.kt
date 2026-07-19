package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable

/**
 * Android full-page presentation boundary for existing settings-detail owners.
 *
 * Callers keep their official preferences, commands, validation, links, and child routes. Android
 * supplies only the shared Nome scaffold; Desktop and callers without an explicit close owner use
 * the legacy composition.
 */
@Composable
internal expect fun PlatformSettingsDetailRoute(
  title: String,
  onClose: (() -> Unit)?,
  groupedContent: Boolean = false,
  legacyContent: @Composable () -> Unit,
  content: @Composable () -> Unit,
)
