package chat.simplex.common.ui.theme

import androidx.compose.runtime.Composable

/**
 * Installs the Android-only Nome presentation theme inside the official shared theme owner.
 *
 * The Desktop actual is an exact pass-through. Route, state, action, wallpaper, and preference
 * ownership stay with [SimpleXTheme].
 */
@Composable
internal expect fun PlatformNomeTheme(
  darkTheme: Boolean,
  content: @Composable () -> Unit,
)
