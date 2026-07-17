package chat.simplex.common.ui.nome.tokens

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
class NomeShapeTokens internal constructor(
  val compact: CornerBasedShape,
  val control: CornerBasedShape,
  val grouped: CornerBasedShape,
  val large: CornerBasedShape,
  val pill: CornerBasedShape,
)

internal val NomeShapes = NomeShapeTokens(
  compact = RoundedCornerShape(10.dp),
  control = RoundedCornerShape(12.dp),
  grouped = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(20.dp),
  pill = RoundedCornerShape(percent = 50),
)

internal val NomeMaterialShapes = Shapes(
  small = NomeShapes.compact,
  medium = NomeShapes.control,
  large = NomeShapes.large,
)
