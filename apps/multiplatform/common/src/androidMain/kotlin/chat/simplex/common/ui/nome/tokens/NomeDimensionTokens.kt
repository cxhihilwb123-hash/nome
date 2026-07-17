package chat.simplex.common.ui.nome.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class NomeDimensionTokens internal constructor(
  val space2: Dp,
  val space4: Dp,
  val space8: Dp,
  val space12: Dp,
  val space16: Dp,
  val space20: Dp,
  val space24: Dp,
  val space32: Dp,
  val space40: Dp,
  val screenHorizontalInset: Dp,
  val minimumTouchTarget: Dp,
  val minimumButtonHeight: Dp,
  val minimumRowHeight: Dp,
  val iconSmall: Dp,
  val icon: Dp,
  val iconLarge: Dp,
  val divider: Dp,
)

internal val NomeDimensions = NomeDimensionTokens(
  space2 = 2.dp,
  space4 = 4.dp,
  space8 = 8.dp,
  space12 = 12.dp,
  space16 = 16.dp,
  space20 = 20.dp,
  space24 = 24.dp,
  space32 = 32.dp,
  space40 = 40.dp,
  screenHorizontalInset = 20.dp,
  minimumTouchTarget = 48.dp,
  minimumButtonHeight = 48.dp,
  minimumRowHeight = 56.dp,
  iconSmall = 18.dp,
  icon = 24.dp,
  iconLarge = 32.dp,
  divider = 1.dp,
)
