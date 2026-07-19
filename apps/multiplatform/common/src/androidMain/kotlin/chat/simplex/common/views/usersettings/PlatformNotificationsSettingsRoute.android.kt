package chat.simplex.common.views.usersettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.model.NotificationPreviewMode
import chat.simplex.common.model.NotificationsMode
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors

@Composable
internal actual fun PlatformNotificationsSettingsRoute(
  title: String,
  modeTitle: String,
  modeValue: String,
  modeDescription: String,
  previewTitle: String,
  previewValue: String,
  previewDescription: String,
  onClose: () -> Unit,
  onOpenMode: () -> Unit,
  onOpenPreview: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  NomeNotificationsTheme {
    NomeNotificationsSettingsContent(
      title = title,
      backLabel = androidx.compose.ui.res.stringResource(R.string.nome_back),
      modeTitle = modeTitle,
      modeValue = modeValue,
      modeDescription = modeDescription,
      previewTitle = previewTitle,
      previewValue = previewValue,
      previewDescription = previewDescription,
      onClose = onClose,
      onOpenMode = onOpenMode,
      onOpenPreview = onOpenPreview,
    )
  }
}

@Composable
internal actual fun PlatformNotificationsModeRoute(
  title: String,
  choices: List<NomeNotificationsModeChoice>,
  selected: NotificationsMode,
  onClose: () -> Unit,
  onSelected: (NotificationsMode) -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  NomeNotificationsTheme {
    NomeNotificationsModeContent(
      title = title,
      backLabel = androidx.compose.ui.res.stringResource(R.string.nome_back),
      choices = choices,
      selected = selected,
      onClose = onClose,
      onSelected = onSelected,
    )
  }
}

@Composable
internal actual fun PlatformNotificationPreviewRoute(
  title: String,
  choices: List<NomeNotificationPreviewChoice>,
  selected: NotificationPreviewMode,
  onClose: () -> Unit,
  onSelected: (NotificationPreviewMode) -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  NomeNotificationsTheme {
    NomeNotificationPreviewContent(
      title = title,
      backLabel = androidx.compose.ui.res.stringResource(R.string.nome_back),
      choices = choices,
      selected = selected,
      onClose = onClose,
      onSelected = onSelected,
    )
  }
}

@Composable
private fun NomeNotificationsTheme(content: @Composable () -> Unit) {
  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(
    darkTheme = darkTheme,
    content = content,
  )
}

@Composable
fun NomeNotificationsSettingsContent(
  title: String,
  backLabel: String,
  modeTitle: String,
  modeValue: String,
  modeDescription: String,
  previewTitle: String,
  previewValue: String,
  previewDescription: String,
  onClose: () -> Unit,
  onOpenMode: () -> Unit,
  onOpenPreview: () -> Unit,
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
      Column {
        NomeNotificationSettingsRow(
          title = modeTitle,
          value = modeValue,
          description = modeDescription,
          icon = {
            Icon(
              imageVector = Icons.Rounded.Notifications,
              contentDescription = null,
            )
          },
          onClick = onOpenMode,
        )
        Divider(
          modifier = Modifier.padding(horizontal = 14.dp),
          color = NomeTheme.colors.divider,
        )
        NomeNotificationSettingsRow(
          title = previewTitle,
          value = previewValue,
          description = previewDescription,
          icon = {
            Icon(
              imageVector = Icons.Rounded.Visibility,
              contentDescription = null,
            )
          },
          onClick = onOpenPreview,
        )
      }
    }
  }
}

@Composable
private fun NomeNotificationSettingsRow(
  title: String,
  value: String,
  description: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .nomeMinimumTouchTarget()
        .clickable(
          onClickLabel = title,
          onClick = onClick,
        )
        .padding(
          horizontal = 14.dp,
          vertical = 14.dp,
        ),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NomeNotificationIcon(icon)
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.textPrimary,
        )
        Text(
          text = value,
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
      }
      Text(
        text = description,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
    Icon(
      imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint = NomeTheme.colors.textSecondary,
    )
  }
}

@Composable
private fun NomeNotificationIcon(content: @Composable () -> Unit) {
  NomeSurface(
    shape = NomeTheme.shapes.control,
    color = NomeTheme.colors.successContainer,
    contentColor = NomeTheme.colors.success,
  ) {
    Box(
      modifier = Modifier.size(44.dp),
      contentAlignment = Alignment.Center,
    ) {
      content()
    }
  }
}

@Composable
fun NomeNotificationsModeContent(
  title: String,
  backLabel: String,
  choices: List<NomeNotificationsModeChoice>,
  selected: NotificationsMode,
  onClose: () -> Unit,
  onSelected: (NotificationsMode) -> Unit,
) {
  NomeNotificationChoicesContent(
    title = title,
    backLabel = backLabel,
    onClose = onClose,
  ) {
    choices.forEach { choice ->
      NomeNotificationChoice(
        title = choice.title,
        description = choice.description,
        selected = choice.value == selected,
        onClick = { onSelected(choice.value) },
      )
    }
  }
}

@Composable
fun NomeNotificationPreviewContent(
  title: String,
  backLabel: String,
  choices: List<NomeNotificationPreviewChoice>,
  selected: NotificationPreviewMode,
  onClose: () -> Unit,
  onSelected: (NotificationPreviewMode) -> Unit,
) {
  NomeNotificationChoicesContent(
    title = title,
    backLabel = backLabel,
    onClose = onClose,
  ) {
    choices.forEach { choice ->
      NomeNotificationChoice(
        title = choice.title,
        description = choice.description,
        selected = choice.value == selected,
        onClick = { onSelected(choice.value) },
      )
    }
  }
}

@Composable
private fun NomeNotificationChoicesContent(
  title: String,
  backLabel: String,
  onClose: () -> Unit,
  content: @Composable () -> Unit,
) {
  NomeFullPageScaffold(
    title = title,
    backLabel = backLabel,
    onClose = onClose,
  ) {
    content()
  }
}

@Composable
private fun NomeNotificationChoice(
  title: String,
  description: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  NomeSurface(
    modifier =
      Modifier
        .fillMaxWidth()
        .nomeMinimumTouchTarget()
        .selectable(
          selected = selected,
          role = Role.RadioButton,
          onClick = onClick,
        )
        .semantics(mergeDescendants = true) {
          this.selected = selected
        },
    color =
      if (selected) {
        NomeTheme.colors.successContainer
      } else {
        NomeTheme.colors.surface
      },
    border =
      BorderStroke(
        1.dp,
        if (selected) {
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
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = title,
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.textPrimary,
        )
        Text(
          text = description,
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
      }
      if (selected) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = null,
          tint = NomeTheme.colors.success,
        )
      }
    }
  }
}
