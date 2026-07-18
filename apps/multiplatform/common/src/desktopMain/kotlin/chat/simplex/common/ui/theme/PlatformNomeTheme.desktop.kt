package chat.simplex.common.ui.theme

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformNomeTheme(
  darkTheme: Boolean,
  content: @Composable () -> Unit,
) {
  content()
}
