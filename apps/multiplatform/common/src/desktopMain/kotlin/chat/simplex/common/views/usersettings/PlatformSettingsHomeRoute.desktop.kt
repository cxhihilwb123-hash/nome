package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable
import chat.simplex.common.model.User

@Composable
internal actual fun PlatformSettingsHomeRoute(
  currentUser: User?,
  stopped: Boolean,
  notificationsEnabled: Boolean,
  languageCode: String,
  onOpenIdentity: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenBackupMigration: () -> Unit,
  onOpenDesktop: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onOpenNetwork: () -> Unit,
  onOpenLanguage: () -> Unit,
  onOpenAppearance: () -> Unit,
  onOpenHelp: () -> Unit,
  onOpenAbout: () -> Unit,
  onOpenDeveloper: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenContacts: () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()

@Composable
internal actual fun PlatformBackupMigrationRoute(
  migrationEnabled: Boolean,
  onClose: () -> Unit,
  onOpenArchive: () -> Unit,
  onOpenMigration: () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()
