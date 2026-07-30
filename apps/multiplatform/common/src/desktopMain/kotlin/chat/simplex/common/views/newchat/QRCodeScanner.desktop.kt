package chat.simplex.common.views.newchat

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Suppress("UNUSED_PARAMETER")
@Composable
actual fun QRCodeScanner(
  showQRCodeScanner: MutableState<Boolean>,
  padding: PaddingValues,
  onBarcode: suspend (String) -> Boolean
) {
  // Desktop does not request camera access. Its connection route exposes paste-only input.
}
