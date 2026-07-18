package chat.simplex.common.views.database

import androidx.compose.runtime.Composable
import chat.simplex.common.views.helpers.DBMigrationResult

sealed interface DatabaseRootRouteInput {
  object Opening : DatabaseRootRouteInput

  object Migrating : DatabaseRootRouteInput

  data class Error(
    val status: DBMigrationResult,
  ) : DatabaseRootRouteInput
}

sealed interface AndroidDatabaseKeyReadState {
  val initialRandomDBPassphrase: Boolean

  data class MissingAlias(
    override val initialRandomDBPassphrase: Boolean,
  ) : AndroidDatabaseKeyReadState

  data class UnreadableMaterial(
    override val initialRandomDBPassphrase: Boolean,
  ) : AndroidDatabaseKeyReadState
}

data class NomeDatabaseRootFacts(
  val route: DatabaseRootRouteInput,
  val ctrlInitInProgress: Boolean,
  val dbMigrationInProgress: Boolean,
  val storedKeyUseRequested: Boolean,
  val storedKeyMaterialPresent: Boolean,
  val androidKeyReadState: AndroidDatabaseKeyReadState?,
  val matchedBackupAvailable: Boolean,
  val downgradeWarningCount: Int,
)

@Composable
expect fun PlatformDatabaseRootRoute(
  facts: NomeDatabaseRootFacts,
  allowSensitiveContent: Boolean,
  legacyContent: @Composable () -> Unit,
)

expect fun platformDatabaseKeyReadState(): AndroidDatabaseKeyReadState?

expect fun clearPlatformDatabaseKeyReadState()
