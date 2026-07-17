package chat.simplex.common.ui.nome.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.sizeIn
import chat.simplex.common.ui.nome.theme.NomeTheme

@Composable
fun Modifier.nomeMinimumTouchTarget(
  minimumSize: Dp = NomeTheme.dimensions.minimumTouchTarget,
): Modifier = sizeIn(minWidth = minimumSize, minHeight = minimumSize)

fun Modifier.nomeTalkBackSemantics(
  label: String? = null,
  state: String? = null,
  role: Role? = null,
  enabled: Boolean = true,
  liveRegionMode: LiveRegionMode? = null,
  mergeDescendants: Boolean = true,
): Modifier = semantics(mergeDescendants = mergeDescendants) {
  label?.let { contentDescription = it }
  state?.let { stateDescription = it }
  role?.let { this.role = it }
  liveRegionMode?.let { liveRegion = it }
  if (!enabled) disabled()
}
