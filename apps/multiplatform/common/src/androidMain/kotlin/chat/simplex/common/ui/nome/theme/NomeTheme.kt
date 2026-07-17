package chat.simplex.common.ui.nome.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import chat.simplex.common.ui.nome.tokens.NomeColorTokens
import chat.simplex.common.ui.nome.tokens.NomeDarkColorTokens
import chat.simplex.common.ui.nome.tokens.NomeDimensionTokens
import chat.simplex.common.ui.nome.tokens.NomeDimensions
import chat.simplex.common.ui.nome.tokens.NomeElevationTokens
import chat.simplex.common.ui.nome.tokens.NomeElevations
import chat.simplex.common.ui.nome.tokens.NomeLightColorTokens
import chat.simplex.common.ui.nome.tokens.NomeMaterialShapes
import chat.simplex.common.ui.nome.tokens.NomeMaterialTypography
import chat.simplex.common.ui.nome.tokens.NomeShapeTokens
import chat.simplex.common.ui.nome.tokens.NomeShapes
import chat.simplex.common.ui.nome.tokens.NomeTypography
import chat.simplex.common.ui.nome.tokens.NomeTypographyTokens

@Immutable
internal class NomeThemeValues(
  val colors: NomeColorTokens,
  val typography: NomeTypographyTokens,
  val dimensions: NomeDimensionTokens,
  val shapes: NomeShapeTokens,
  val elevation: NomeElevationTokens,
)

private val NomeLightThemeValues = NomeThemeValues(
  colors = NomeLightColorTokens,
  typography = NomeTypography,
  dimensions = NomeDimensions,
  shapes = NomeShapes,
  elevation = NomeElevations,
)

private val NomeDarkThemeValues = NomeThemeValues(
  colors = NomeDarkColorTokens,
  typography = NomeTypography,
  dimensions = NomeDimensions,
  shapes = NomeShapes,
  elevation = NomeElevations,
)

internal val LocalNomeTheme = staticCompositionLocalOf { NomeLightThemeValues }

object NomeTheme {
  val colors: NomeColorTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalNomeTheme.current.colors

  val typography: NomeTypographyTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalNomeTheme.current.typography

  val dimensions: NomeDimensionTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalNomeTheme.current.dimensions

  val shapes: NomeShapeTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalNomeTheme.current.shapes

  val elevation: NomeElevationTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalNomeTheme.current.elevation
}

@Composable
fun NomeAndroidTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val theme = if (darkTheme) NomeDarkThemeValues else NomeLightThemeValues
  val materialColors = if (darkTheme) {
    darkColors(
      primary = theme.colors.action,
      primaryVariant = theme.colors.action,
      secondary = theme.colors.accent,
      secondaryVariant = theme.colors.accent,
      background = theme.colors.background,
      surface = theme.colors.surface,
      error = theme.colors.danger,
      onPrimary = theme.colors.onAction,
      onSecondary = theme.colors.onAccent,
      onBackground = theme.colors.textPrimary,
      onSurface = theme.colors.textPrimary,
      onError = theme.colors.onDanger,
    )
  } else {
    lightColors(
      primary = theme.colors.action,
      primaryVariant = theme.colors.action,
      secondary = theme.colors.accent,
      secondaryVariant = theme.colors.accent,
      background = theme.colors.background,
      surface = theme.colors.surface,
      error = theme.colors.danger,
      onPrimary = theme.colors.onAction,
      onSecondary = theme.colors.onAccent,
      onBackground = theme.colors.textPrimary,
      onSurface = theme.colors.textPrimary,
      onError = theme.colors.onDanger,
    )
  }

  MaterialTheme(
    colors = materialColors,
    typography = NomeMaterialTypography,
    shapes = NomeMaterialShapes,
  ) {
    CompositionLocalProvider(
      LocalNomeTheme provides theme,
      LocalContentColor provides theme.colors.textPrimary,
      content = content,
    )
  }
}
