package chat.simplex.common.ui.nome.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.theme.NomeTheme

enum class NomeStatePanelState {
  NORMAL,
  LOADING,
  EMPTY,
  OFFLINE,
  ERROR,
  PERMISSION,
  DANGER,
}

@Immutable
class NomeStatePanelAction(
  val label: String,
  val onClick: () -> Unit,
  val variant: NomeButtonVariant = NomeButtonVariant.PRIMARY,
  val enabled: Boolean = true,
  val semanticsLabel: String? = null,
)

private data class NomeStatePanelVisuals(
  val container: Color,
  val content: Color,
  val icon: ImageVector?,
)

@Composable
fun NomeStatePanel(
  state: NomeStatePanelState,
  title: String,
  modifier: Modifier = Modifier,
  description: String? = null,
  stateDescription: String? = null,
  primaryAction: NomeStatePanelAction? = null,
  secondaryAction: NomeStatePanelAction? = null,
  icon: (@Composable () -> Unit)? = null,
) {
  val colors = NomeTheme.colors
  val dimensions = NomeTheme.dimensions
  val visuals = when (state) {
    NomeStatePanelState.NORMAL -> NomeStatePanelVisuals(
      container = colors.surfaceSubtle,
      content = colors.textPrimary,
      icon = Icons.Rounded.Info,
    )
    NomeStatePanelState.LOADING -> NomeStatePanelVisuals(
      container = colors.infoContainer,
      content = colors.onInfoContainer,
      icon = null,
    )
    NomeStatePanelState.EMPTY -> NomeStatePanelVisuals(
      container = colors.surfaceSubtle,
      content = colors.textSecondary,
      icon = Icons.Rounded.Inbox,
    )
    NomeStatePanelState.OFFLINE -> NomeStatePanelVisuals(
      container = colors.warningContainer,
      content = colors.onWarningContainer,
      icon = Icons.Rounded.CloudOff,
    )
    NomeStatePanelState.ERROR -> NomeStatePanelVisuals(
      container = colors.dangerContainer,
      content = colors.onDangerContainer,
      icon = Icons.Rounded.ErrorOutline,
    )
    NomeStatePanelState.PERMISSION -> NomeStatePanelVisuals(
      container = colors.infoContainer,
      content = colors.onInfoContainer,
      icon = Icons.Rounded.Security,
    )
    NomeStatePanelState.DANGER -> NomeStatePanelVisuals(
      container = colors.dangerContainer,
      content = colors.onDangerContainer,
      icon = Icons.Rounded.Warning,
    )
  }
  val liveRegionMode = when (state) {
    NomeStatePanelState.ERROR,
    NomeStatePanelState.DANGER -> LiveRegionMode.Assertive
    NomeStatePanelState.LOADING,
    NomeStatePanelState.OFFLINE,
    NomeStatePanelState.PERMISSION -> LiveRegionMode.Polite
    else -> null
  }

  NomeSurface(
    modifier = modifier.nomeTalkBackSemantics(
      state = stateDescription,
      liveRegionMode = liveRegionMode,
    ),
    shape = NomeTheme.shapes.grouped,
    color = visuals.container,
    contentColor = visuals.content,
    border = BorderStroke(dimensions.divider, visuals.content.copy(alpha = 0.42f)),
  ) {
    Column(
      modifier = Modifier.padding(dimensions.space20),
      verticalArrangement = Arrangement.spacedBy(dimensions.space12),
    ) {
      when {
        icon != null -> icon()
        state == NomeStatePanelState.LOADING -> CircularProgressIndicator(
          modifier = Modifier.size(dimensions.icon),
          color = visuals.content,
          strokeWidth = dimensions.space2,
        )
        visuals.icon != null -> Icon(
          imageVector = visuals.icon,
          contentDescription = null,
          modifier = Modifier.size(dimensions.icon),
          tint = visuals.content,
        )
      }

      Text(
        text = title,
        style = NomeTheme.typography.title,
        color = visuals.content,
      )
      description?.let {
        Text(
          text = it,
          style = NomeTheme.typography.body,
          color = visuals.content,
        )
      }
      primaryAction?.let { action ->
        NomeButton(
          text = action.label,
          onClick = action.onClick,
          modifier = Modifier.fillMaxWidth(),
          variant = action.variant,
          enabled = action.enabled,
          semanticsLabel = action.semanticsLabel,
        )
      }
      secondaryAction?.let { action ->
        NomeButton(
          text = action.label,
          onClick = action.onClick,
          modifier = Modifier.fillMaxWidth(),
          variant = action.variant,
          enabled = action.enabled,
          semanticsLabel = action.semanticsLabel,
        )
      }
    }
  }
}
