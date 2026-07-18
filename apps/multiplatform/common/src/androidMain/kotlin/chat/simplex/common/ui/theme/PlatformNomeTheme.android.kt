package chat.simplex.common.ui.theme

import androidx.compose.runtime.Composable
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme

@Composable
internal actual fun PlatformNomeTheme(
  darkTheme: Boolean,
  content: @Composable () -> Unit,
) {
  NomeAndroidTheme(
    darkTheme = darkTheme,
    content = content,
  )
}
