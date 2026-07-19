package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable

/**
 * Android presentation boundary for the settings About page.
 *
 * The Android actual may change only presentation and external navigation. App/core version
 * details remain owned by [onOpenVersion], which is backed by the official `apiGetVersion` route.
 * Desktop keeps the legacy information page.
 */
@Composable
internal expect fun PlatformAboutSettingsRoute(
  onClose: (() -> Unit)?,
  onOpenVersion: () -> Unit,
  legacyContent: @Composable () -> Unit,
)
