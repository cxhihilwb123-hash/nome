package chat.simplex.common.views.newchat

import androidx.compose.runtime.collectAsState
import chat.simplex.common.ui.nome.accessibility.NomeFocusRestoration
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewRoute
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.ModalManager

actual fun presentPlatformConnectionPreview(
  model: ConnectionPreviewUiModel,
  callbacks: ConnectionPreviewCallbacks,
): Boolean {
  ModalManager.fullscreen.showCustomModal { close ->
    val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
    NomeAndroidTheme(darkTheme = darkTheme) {
      NomeConnectionPreviewRoute(
        model = model,
        callbacks = callbacks,
        close = {
          NomeFocusRestoration.requestCurrentProfileFocus()
          close()
        },
      )
    }
  }
  return true
}
