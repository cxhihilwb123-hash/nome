package chat.simplex.common.ui.nome.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class NomeElevationTokens internal constructor(
  val none: Dp,
  val low: Dp,
  val medium: Dp,
  val high: Dp,
)

internal val NomeElevations = NomeElevationTokens(
  none = 0.dp,
  low = 1.dp,
  medium = 4.dp,
  high = 8.dp,
)
