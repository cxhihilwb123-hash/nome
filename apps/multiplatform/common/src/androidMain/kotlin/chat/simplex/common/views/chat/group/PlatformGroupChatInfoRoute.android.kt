package chat.simplex.common.views.chat.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
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
internal actual fun PlatformGroupChatInfoRoute(
  title: String,
  onClose: () -> Unit,
  content: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  BackHandler(onBack = onClose)
  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeGroupChatInfoContent(
      title = title,
      onClose = onClose,
      content = content,
    )
  }
}

@Composable
fun NomeGroupChatInfoContent(
  title: String,
  onClose: () -> Unit,
  content: @Composable () -> Unit,
) {
  NomeFullPageScaffold(
    title = title,
    backLabel = androidx.compose.ui.res.stringResource(R.string.nome_back),
    onClose = onClose,
    scrollable = false,
  ) {
    NomeSurface(
      modifier = Modifier.fillMaxSize(),
      border = BorderStroke(1.dp, NomeTheme.colors.divider),
    ) {
      content()
    }
  }
}
