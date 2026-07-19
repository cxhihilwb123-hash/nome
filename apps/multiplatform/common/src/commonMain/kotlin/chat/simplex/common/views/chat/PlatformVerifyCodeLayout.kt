package chat.simplex.common.views.chat

import androidx.compose.runtime.Composable

/**
 * Android-only presentation boundary for the official contact/member verification flow.
 *
 * The caller retains the exact security code, verification command/result, model update, scanner,
 * sharing, and modal lifecycle. Desktop renders [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformVerifyCodeLayout(
  displayName: String,
  profileImage: String?,
  connectionCode: String,
  connectionVerified: Boolean,
  onScanCode: () -> Unit,
  onMarkVerified: () -> Unit,
  onClearVerification: () -> Unit,
  onShareCode: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
)

internal fun formatNomeSecurityCode(code: String): String {
  val compact = code.filterNot(Char::isWhitespace)
  if (compact.isEmpty()) return code
  return compact.chunked(4).joinToString(" ")
}
