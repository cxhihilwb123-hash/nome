package chat.simplex.common.views.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.theme.NomeTheme

@Composable
internal actual fun PlatformChannelDisclosureBanner(
  visible: Boolean,
) {
  if (!visible) return
  NomeChannelDisclosureContent()
}

@Composable
fun NomeChannelDisclosureContent() {
  Spacer(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(nomeChannelDisclosureHeight),
  )
}

@Composable
internal actual fun PlatformChannelObserverBar(
  visible: Boolean,
) {
  if (!visible) return
  NomeChannelObserverContent()
}

@Composable
fun NomeChannelObserverContent() {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(NomeTheme.colors.surfaceContainer)
        .padding(
          horizontal = 18.dp,
          vertical = 10.dp,
        )
        .nomeTalkBackSemantics(
          label =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p21_observer,
            ) +
              ". " +
              androidx.compose.ui.res.stringResource(
                R.string.nome_p21_observer_body,
              ),
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement =
      Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      imageVector = Icons.Rounded.RemoveRedEye,
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = NomeTheme.colors.textSecondary,
    )
    Column(
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p21_observer,
          ),
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p21_observer_body,
          ),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
  }
}
