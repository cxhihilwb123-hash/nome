package chat.simplex.common.views.usersettings

import androidx.compose.runtime.Composable
import chat.simplex.common.model.User

/**
 * Android presentation seam for the official settings routes.
 *
 * The common owner supplies the current user, stored preferences, and existing route callbacks.
 * Android may reorganize those routes to match P23; Desktop renders [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformSettingsHomeRoute(
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
)

/**
 * Android P24 landing page over existing archive/database and device-migration routes.
 *
 * It owns no archive state or operation result. Desktop keeps the caller-provided legacy route.
 */
@Composable
internal expect fun PlatformBackupMigrationRoute(
  migrationEnabled: Boolean,
  onClose: () -> Unit,
  onOpenArchive: () -> Unit,
  onOpenMigration: () -> Unit,
  legacyContent: @Composable () -> Unit,
)
