package chat.simplex.common.views.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors

@Composable
internal actual fun PlatformContactDetailRoute(
  title: String,
  onClose: () -> Unit,
  headerContent: @Composable () -> Unit,
  quickActionsContent: @Composable () -> Unit,
  detailsContent: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeContactDetailContent(
      title = title,
      backLabel =
        androidx.compose.ui.res.stringResource(
          R.string.nome_back,
        ),
      onClose = onClose,
      headerContent = headerContent,
      quickActionsContent = quickActionsContent,
      detailsContent = detailsContent,
    )
  }
}

@Composable
fun NomeContactDetailContent(
  title: String,
  backLabel: String,
  onClose: () -> Unit,
  headerContent: @Composable () -> Unit,
  quickActionsContent: @Composable () -> Unit,
  detailsContent: @Composable () -> Unit,
) {
  NomeFullPageScaffold(
    title = title,
    backLabel = backLabel,
    onClose = onClose,
  ) {
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.divider,
        ),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(
              horizontal = 16.dp,
              vertical = 14.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        headerContent()
      }
    }
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.divider,
        ),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(
              horizontal = 8.dp,
              vertical = 8.dp,
            ),
      ) {
        quickActionsContent()
      }
    }
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.divider,
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
      ) {
        Divider(
          color = NomeTheme.colors.divider,
        )
        detailsContent()
      }
    }
  }
}
