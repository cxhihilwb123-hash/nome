package chat.simplex.common.views.usersettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors

@Composable
internal actual fun PlatformSettingsDetailRoute(
  title: String,
  onClose: (() -> Unit)?,
  groupedContent: Boolean,
  legacyContent: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  if (onClose == null) {
    legacyContent()
    return
  }
  BackHandler(onBack = onClose)
  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeFullPageScaffold(
      title = title,
      backLabel = androidx.compose.ui.res.stringResource(R.string.nome_back),
      onClose = onClose,
      content = {
        if (groupedContent) {
          NomeSurface(
            modifier = Modifier.fillMaxWidth(),
            border =
              BorderStroke(
                1.dp,
                NomeTheme.colors.border,
              ),
          ) {
            Column(
              modifier =
                Modifier.padding(
                  horizontal = 14.dp,
                  vertical = 14.dp,
                ),
            ) {
              content()
            }
          }
        } else {
          content()
        }
      },
    )
  }
}
