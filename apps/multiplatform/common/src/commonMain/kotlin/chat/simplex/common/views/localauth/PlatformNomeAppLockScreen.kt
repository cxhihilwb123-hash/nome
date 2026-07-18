package chat.simplex.common.views.localauth

import androidx.compose.runtime.Composable
import chat.simplex.common.views.usersettings.LAMode

@Composable
internal expect fun PlatformNomeAppLockScreen(
  enabled: Boolean,
  displayName: String?,
  profileImage: String?,
  usingLAMode: LAMode,
  onUnlock: () -> Unit,
  legacyContent: @Composable () -> Unit,
)
