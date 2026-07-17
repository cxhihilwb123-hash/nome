package chat.simplex.common.ui.nome.tokens

import androidx.compose.material.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
class NomeTypographyTokens internal constructor(
  val display: TextStyle,
  val titleLarge: TextStyle,
  val title: TextStyle,
  val body: TextStyle,
  val bodyStrong: TextStyle,
  val label: TextStyle,
  val supporting: TextStyle,
  val code: TextStyle,
)

private val NomeSystemFont = FontFamily.SansSerif

internal val NomeTypography = NomeTypographyTokens(
  display = TextStyle(
    fontFamily = NomeSystemFont,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = NomeSystemFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
  ),
  title = TextStyle(
    fontFamily = NomeSystemFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
  ),
  body = TextStyle(
    fontFamily = NomeSystemFont,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
  ),
  bodyStrong = TextStyle(
    fontFamily = NomeSystemFont,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 24.sp,
  ),
  label = TextStyle(
    fontFamily = NomeSystemFont,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  supporting = TextStyle(
    fontFamily = NomeSystemFont,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  code = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  ),
)

internal val NomeMaterialTypography = Typography(
  h1 = NomeTypography.display,
  h2 = NomeTypography.titleLarge,
  h3 = NomeTypography.title,
  h4 = NomeTypography.bodyStrong,
  h5 = NomeTypography.bodyStrong,
  h6 = NomeTypography.label,
  subtitle1 = NomeTypography.bodyStrong,
  subtitle2 = NomeTypography.label,
  body1 = NomeTypography.body,
  body2 = NomeTypography.supporting,
  button = NomeTypography.label,
  caption = NomeTypography.supporting,
  overline = NomeTypography.label,
)
