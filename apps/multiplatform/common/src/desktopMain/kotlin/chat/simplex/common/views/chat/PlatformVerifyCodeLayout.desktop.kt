package chat.simplex.common.views.chat

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformVerifyCodeLayout(
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
) = legacyContent()
