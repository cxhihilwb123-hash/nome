package chat.simplex.common.views.chatlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
internal actual fun PlatformTagEditorRoute(
  title: String,
  submitLabel: String,
  errorText: String,
  showError: Boolean,
  submitEnabled: Boolean,
  onClose: () -> Unit,
  onSubmit: () -> Unit,
  inputContent: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeTagEditorContent(
      title = title,
      backLabel = androidx.compose.ui.res.stringResource(R.string.nome_back),
      submitLabel = submitLabel,
      errorText = errorText,
      showError = showError,
      submitEnabled = submitEnabled,
      onClose = onClose,
      onSubmit = onSubmit,
      inputContent = inputContent,
    )
  }
}

@Composable
internal actual fun PlatformTagListRoute(
  title: String,
  createLabel: String,
  reorderMode: Boolean,
  choices: List<NomeTagListChoice>,
  saving: Boolean,
  onClose: () -> Unit,
  onCreate: () -> Unit,
  onChoice: (Long) -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  if (reorderMode) {
    legacyContent()
    return
  }
  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeTagListContent(
      title = title,
      backLabel = androidx.compose.ui.res.stringResource(R.string.nome_back),
      createLabel = createLabel,
      choices = choices,
      saving = saving,
      onClose = onClose,
      onCreate = onCreate,
      onChoice = onChoice,
    )
  }
}

@Composable
fun NomeTagListContent(
  title: String,
  backLabel: String,
  createLabel: String,
  choices: List<NomeTagListChoice>,
  saving: Boolean,
  onClose: () -> Unit,
  onCreate: () -> Unit,
  onChoice: (Long) -> Unit,
) {
  NomeFullPageScaffold(
    title = title,
    backLabel = backLabel,
    onClose = onClose,
  ) {
    choices.forEach { choice ->
      NomeSurface(
        modifier =
          Modifier
            .fillMaxWidth()
            .nomeMinimumTouchTarget()
            .clickable(
              enabled = !saving,
              onClickLabel = choice.name,
              onClick = { onChoice(choice.id) },
            ),
        color =
          if (choice.selected) {
            NomeTheme.colors.successContainer
          } else {
            NomeTheme.colors.surface
          },
        border =
          BorderStroke(
            1.dp,
            if (choice.selected) {
              NomeTheme.colors.success
            } else {
              NomeTheme.colors.divider
            },
          ),
      ) {
        Row(
          modifier =
            Modifier.padding(
              horizontal = 16.dp,
              vertical = 14.dp,
            ),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (choice.emoji != null) {
            Text(
              text = choice.emoji,
              style = NomeTheme.typography.title,
            )
          } else {
            Icon(
              painter = painterResource(MR.images.ic_label),
              contentDescription = null,
              modifier = Modifier.size(24.dp),
              tint = NomeTheme.colors.textSecondary,
            )
          }
          Text(
            text = choice.name,
            modifier = Modifier.weight(1f),
            style = NomeTheme.typography.bodyStrong,
            color = NomeTheme.colors.textPrimary,
          )
          if (choice.selected) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = NomeTheme.colors.success,
            )
          }
        }
      }
    }
    NomeButton(
      text = createLabel,
      onClick = onCreate,
      modifier = Modifier.fillMaxWidth(),
      variant = NomeButtonVariant.SECONDARY,
      enabled = !saving,
      semanticsLabel = createLabel,
      leadingIcon = {
        Icon(
          painter = painterResource(MR.images.ic_add),
          contentDescription = null,
        )
      },
    )
  }
}

@Composable
fun NomeTagEditorContent(
  title: String,
  backLabel: String,
  submitLabel: String,
  errorText: String,
  showError: Boolean,
  submitEnabled: Boolean,
  onClose: () -> Unit,
  onSubmit: () -> Unit,
  inputContent: @Composable () -> Unit,
) {
  NomeFullPageScaffold(
    title = title,
    backLabel = backLabel,
    onClose = onClose,
  ) {
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border = BorderStroke(1.dp, NomeTheme.colors.divider),
    ) {
      Column(
        modifier =
          Modifier.padding(
            horizontal = 12.dp,
            vertical = 8.dp,
          ),
      ) {
        inputContent()
      }
    }
    if (showError) {
      NomeSurface(
        modifier = Modifier.fillMaxWidth(),
        color = NomeTheme.colors.dangerContainer,
        contentColor = NomeTheme.colors.onDangerContainer,
      ) {
        androidx.compose.foundation.layout.Row(
          modifier = Modifier.padding(12.dp),
        ) {
          Icon(
            painter = painterResource(MR.images.ic_error),
            contentDescription = null,
          )
          Text(
            text = errorText,
            modifier = Modifier.padding(start = 8.dp),
            style = NomeTheme.typography.supporting,
          )
        }
      }
    }
    NomeButton(
      text = submitLabel,
      onClick = onSubmit,
      modifier = Modifier.fillMaxWidth(),
      enabled = submitEnabled,
      semanticsLabel = submitLabel,
    )
  }
}
