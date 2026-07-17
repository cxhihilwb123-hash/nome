package chat.simplex.common.ui.nome.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import chat.simplex.common.ui.nome.theme.NomeTheme

@Composable
fun NomeSurface(
  modifier: Modifier = Modifier,
  shape: Shape = NomeTheme.shapes.grouped,
  color: Color = NomeTheme.colors.surface,
  contentColor: Color = NomeTheme.colors.textPrimary,
  border: BorderStroke? = null,
  elevation: Dp = NomeTheme.elevation.none,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = modifier,
    shape = shape,
    color = color,
    contentColor = contentColor,
    border = border,
    elevation = elevation,
    content = content,
  )
}
