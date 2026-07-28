package chat.simplex.common.views.database

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
actual fun PlatformDatabaseUnlockView(
  dbKey: MutableState<String>,
  buttonEnabled: Boolean,
  progress: Boolean,
  storedKeyRejected: Boolean,
  backupAvailable: Boolean,
  onOpen: () -> Unit,
  onRestore: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}
