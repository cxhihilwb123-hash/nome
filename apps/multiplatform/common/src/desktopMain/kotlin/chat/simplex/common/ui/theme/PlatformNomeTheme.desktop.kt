package chat.simplex.common.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun PlatformNomeTheme(
  darkTheme: Boolean,
  content: @Composable () -> Unit,
) {
  val inherited = MaterialTheme.colors
  val colors =
    if (darkTheme) {
      inherited.copy(
        primary = Color(0xFF73D1B4),
        primaryVariant = Color(0xFF16AE66),
        secondary = Color(0xFFA9B8C9),
        secondaryVariant = Color(0xFF26384B),
        background = Color(0xFF0F1721),
        surface = Color(0xFF17212E),
        onPrimary = Color(0xFF0E1B2D),
        onSecondary = Color(0xFFF4F7FA),
        onBackground = Color(0xFFF4F7FA),
        onSurface = Color(0xFFF4F7FA),
      )
    } else {
      inherited.copy(
        primary = Color(0xFF0A874D),
        primaryVariant = Color(0xFF087342),
        secondary = Color(0xFF667085),
        secondaryVariant = Color(0xFFE7ECF1),
        background = Color(0xFFF5F7FA),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color(0xFF0E1B2D),
        onBackground = Color(0xFF0E1B2D),
        onSurface = Color(0xFF0E1B2D),
      )
    }
  MaterialTheme(
    colors = colors,
    typography = MaterialTheme.typography,
    shapes = MaterialTheme.shapes,
    content = content,
  )
}
