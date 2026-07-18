package chat.simplex.common.ui.nome.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.theme.NomeTheme

enum class NomeButtonVariant {
  PRIMARY,
  SECONDARY,
  DESTRUCTIVE,
}

@Composable
fun NomeButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  variant: NomeButtonVariant = NomeButtonVariant.PRIMARY,
  enabled: Boolean = true,
  semanticsLabel: String? = null,
  stateDescription: String? = null,
  leadingIcon: (@Composable () -> Unit)? = null,
) {
  val colors = NomeTheme.colors
  val dimensions = NomeTheme.dimensions
  val backgroundColor = when (variant) {
    NomeButtonVariant.PRIMARY -> colors.action
    NomeButtonVariant.SECONDARY -> colors.surfaceSubtle
    NomeButtonVariant.DESTRUCTIVE -> colors.danger
  }
  val contentColor = when (variant) {
    NomeButtonVariant.PRIMARY -> colors.onAction
    NomeButtonVariant.SECONDARY -> colors.textPrimary
    NomeButtonVariant.DESTRUCTIVE -> colors.onDanger
  }
  Button(
    onClick = onClick,
    modifier = modifier
      .nomeMinimumTouchTarget()
      .nomeTalkBackSemantics(
        label = semanticsLabel,
        state = stateDescription,
        role = Role.Button,
        enabled = enabled,
      ),
    enabled = enabled,
    shape = NomeTheme.shapes.control,
    border = null,
    colors = ButtonDefaults.buttonColors(
      backgroundColor = backgroundColor,
      contentColor = contentColor,
      disabledBackgroundColor = colors.disabledContainer,
      disabledContentColor = colors.disabledContent,
    ),
    contentPadding = PaddingValues(
      horizontal = dimensions.space20,
      vertical = dimensions.space12,
    ),
    elevation = ButtonDefaults.elevation(
      defaultElevation = NomeTheme.elevation.none,
      pressedElevation = NomeTheme.elevation.low,
      disabledElevation = NomeTheme.elevation.none,
    ),
  ) {
    leadingIcon?.let {
      it()
      Spacer(Modifier.width(dimensions.space8))
    }
    androidx.compose.material.Text(
      text = text,
      style = NomeTheme.typography.label,
    )
  }
}
