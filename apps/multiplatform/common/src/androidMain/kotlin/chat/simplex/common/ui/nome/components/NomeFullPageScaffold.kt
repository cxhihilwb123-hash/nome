package chat.simplex.common.ui.nome.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.theme.NomeTheme

@Composable
fun NomeFullPageScaffold(
  title: String,
  backLabel: String,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
  scrollable: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  val bodyModifier =
    if (scrollable) {
      Modifier.verticalScroll(rememberScrollState())
    } else {
      Modifier
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(NomeTheme.colors.background)
        .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(64.dp)
          .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onClose,
        modifier =
          Modifier
            .size(NomeTheme.dimensions.minimumTouchTarget)
            .nomeTalkBackSemantics(
              label = backLabel,
            ),
      ) {
        Icon(
          imageVector =
            Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = null,
          tint = NomeTheme.colors.textPrimary,
        )
      }
      Text(
        text = title,
        modifier =
          Modifier
            .weight(1f)
            .semantics { heading() },
        style = NomeTheme.typography.title,
        color = NomeTheme.colors.textPrimary,
      )
      Spacer(
        Modifier.size(
          NomeTheme.dimensions.minimumTouchTarget,
        ),
      )
    }
    Divider(
      color = NomeTheme.colors.divider,
    )
    Column(
      modifier =
        bodyModifier
          .then(
            if (scrollable) {
              Modifier
            } else {
              Modifier.weight(1f)
            },
          )
          .fillMaxWidth()
          .padding(
            horizontal = 18.dp,
            vertical = 12.dp,
          ),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      content = content,
    )
  }
}
