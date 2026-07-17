package chat.simplex.app.nome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import chat.simplex.common.platform.platform
import chat.simplex.common.ui.theme.CurrentColors

/**
 * Android-owned production host for Nome.
 *
 * The shared root still owns navigation, authentication, calls, overlays, safe-area content,
 * and back dispatch. This host only keeps the Activity window appearance synchronized while the
 * Android home-route actual installs the Nome theme inside the existing root seam.
 */
@Composable
fun NomeProductionShell(content: @Composable () -> Unit) {
  val colors = CurrentColors.collectAsState().value.colors
  SideEffect {
    platform.androidSetStatusAndNavigationBarAppearance(colors.isLight, colors.isLight)
  }
  content()
}
