package chat.simplex.app.nome.database

import chat.simplex.common.views.database.AndroidDatabaseKeyReadState
import chat.simplex.common.views.database.DatabaseRootRouteInput
import chat.simplex.common.views.database.NomeDatabaseRootFacts
import chat.simplex.common.views.database.clearPlatformDatabaseKeyReadState
import chat.simplex.common.views.database.platformDatabaseKeyReadState
import chat.simplex.common.views.database.reportPlatformDatabaseKeyReadState
import chat.simplex.common.ui.nome.database.NomeDatabaseRootState
import chat.simplex.common.ui.nome.database.NomeDatabaseRootStateAdapter
import chat.simplex.common.ui.nome.database.NomeDatabaseRootTruthInput
import chat.simplex.common.ui.nome.database.DatabaseRecoveryActionResult
import chat.simplex.common.ui.nome.database.NomeDatabaseRecoveryPresentationStore
import chat.simplex.common.views.helpers.DBMigrationResult
import chat.simplex.common.views.helpers.MTRError
import chat.simplex.common.views.helpers.MigrationError
import chat.simplex.common.views.helpers.UpMigration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NomeDatabaseRootStateAdapterTest {
  @Test
  fun processKeyReadStatePublishesAndClearsWithoutPayload() {
    clearPlatformDatabaseKeyReadState()
    val failure = AndroidDatabaseKeyReadState.UnreadableMaterial(
      initialRandomDBPassphrase = true,
    )

    reportPlatformDatabaseKeyReadState(failure)

    assertEquals(failure, platformDatabaseKeyReadState())
    clearPlatformDatabaseKeyReadState()
    assertEquals(null, platformDatabaseKeyReadState())
  }

  @Test
  fun randomKeyFailureOverridesOpeningAndAlternateKey() {
    val opening = derive(
      route = DatabaseRootRouteInput.Opening,
      androidKeyReadState = AndroidDatabaseKeyReadState.UnreadableMaterial(
        initialRandomDBPassphrase = true,
      ),
      matchedBackupAvailable = true,
    )
    val terminal = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorNotADatabase("/tmp/chat.db"),
      ),
      androidKeyReadState = AndroidDatabaseKeyReadState.MissingAlias(
        initialRandomDBPassphrase = true,
      ),
      matchedBackupAvailable = true,
    )

    assertEquals(
      NomeDatabaseRootState.StoredRandomKeyUnavailable(
        submitting = false,
        matchedBackupAvailable = true,
      ),
      opening,
    )
    assertEquals(
      NomeDatabaseRootState.StoredRandomKeyUnavailable(
        submitting = false,
        matchedBackupAvailable = true,
      ),
      terminal,
    )
  }

  @Test
  fun manualKeyReadFailuresRequireTerminalErrorNotADatabase() {
    val opening = derive(
      route = DatabaseRootRouteInput.Opening,
      androidKeyReadState = AndroidDatabaseKeyReadState.MissingAlias(
        initialRandomDBPassphrase = false,
      ),
    )
    val missingAlias = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorNotADatabase("/tmp/chat.db"),
      ),
      androidKeyReadState = AndroidDatabaseKeyReadState.MissingAlias(
        initialRandomDBPassphrase = false,
      ),
    )
    val decryptFailure = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorNotADatabase("/tmp/chat.db"),
      ),
      androidKeyReadState = AndroidDatabaseKeyReadState.UnreadableMaterial(
        initialRandomDBPassphrase = false,
      ),
      matchedBackupAvailable = true,
    )

    assertEquals(NomeDatabaseRootState.Opening(submitting = false), opening)
    assertEquals(
      NomeDatabaseRootState.StoredManualKeyUnreadable(
        submitting = false,
        matchedBackupAvailable = false,
      ),
      missingAlias,
    )
    assertEquals(
      NomeDatabaseRootState.StoredManualKeyUnreadable(
        submitting = false,
        matchedBackupAvailable = true,
      ),
      decryptFailure,
    )
  }

  @Test
  fun storedKeyPresenceChangesNeutralFactsNotTheFailureClass() {
    val noStoredMaterial = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorNotADatabase("/tmp/chat.db"),
      ),
      storedKeyMaterialPresent = false,
      storedKeyUseRequested = false,
    )
    val storedMaterial = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorNotADatabase("/tmp/chat.db"),
      ),
      storedKeyMaterialPresent = true,
      storedKeyUseRequested = true,
    )

    assertEquals(
      NomeDatabaseRootState.AlternateKeyRequired(
        submitting = false,
        matchedBackupAvailable = false,
        storedKeyMaterialPresent = false,
        storedKeyUseRequested = false,
      ),
      noStoredMaterial,
    )
    assertEquals(
      NomeDatabaseRootState.AlternateKeyRequired(
        submitting = false,
        matchedBackupAvailable = false,
        storedKeyMaterialPresent = true,
        storedKeyUseRequested = true,
      ),
      storedMaterial,
    )
  }

  @Test
  fun everyMigrationAndTerminalVariantMapsToSanitizedState() {
    val upgrade = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorMigration(
          "/tmp/chat.db",
          MigrationError.Upgrade(listOf(UpMigration("20260718_sensitive_name"))),
        ),
      ),
    )
    val downgrade = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorMigration(
          "/tmp/chat.db",
          MigrationError.Downgrade(
            listOf("migration_a", "migration_b", "migration_c"),
          ),
        ),
      ),
      downgradeWarningCount = 2,
      matchedBackupAvailable = true,
    )
    val incompatible = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorMigration(
          "/tmp/chat.db",
          MigrationError.Error(
            MTRError.Different(
              appMigration = "app-sensitive",
              dbMigration = "db-sensitive",
            ),
          ),
        ),
      ),
    )
    val sql = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorSQL(
          "/tmp/chat.db",
          "SELECT * FROM sensitive_table",
        ),
      ),
      matchedBackupAvailable = true,
    )
    val keychain = derive(
      route = DatabaseRootRouteInput.Error(DBMigrationResult.ErrorKeychain),
      storedKeyMaterialPresent = true,
    )
    val invalid = derive(
      route = DatabaseRootRouteInput.Error(DBMigrationResult.InvalidConfirmation),
    )
    val unknown = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.Unknown("""{"stack":"raw-secret"}"""),
      ),
    )

    assertEquals(
      NomeDatabaseRootState.UpgradeConsent(
        submitting = false,
        matchedBackupAvailable = false,
      ),
      upgrade,
    )
    assertEquals(
      NomeDatabaseRootState.DowngradeConsent(
        submitting = false,
        matchedBackupAvailable = true,
        warningCount = 2,
      ),
      downgrade,
    )
    assertEquals(
      NomeDatabaseRootState.IncompatibleVersion(
        submitting = false,
        matchedBackupAvailable = false,
      ),
      incompatible,
    )
    assertEquals(
      NomeDatabaseRootState.DatabaseOpenFailed(
        submitting = false,
        matchedBackupAvailable = true,
      ),
      sql,
    )
    assertEquals(
      NomeDatabaseRootState.KeyStoreUnavailable(
        submitting = false,
        matchedBackupAvailable = false,
        storedKeyMaterialPresent = true,
      ),
      keychain,
    )
    assertEquals(
      NomeDatabaseRootState.InvalidConfirmation(
        submitting = false,
        matchedBackupAvailable = false,
      ),
      invalid,
    )
    assertEquals(
      NomeDatabaseRootState.UnknownFailure(
        submitting = false,
        matchedBackupAvailable = false,
      ),
      unknown,
    )

    val rawFragments = listOf(
      "/tmp/chat.db",
      "20260718_sensitive_name",
      "migration_a",
      "app-sensitive",
      "SELECT * FROM sensitive_table",
      "raw-secret",
    )
    listOf(upgrade, downgrade, incompatible, sql, keychain, invalid, unknown)
      .forEach { state ->
        rawFragments.forEach { fragment ->
          assertFalse(state.toString().contains(fragment))
        }
      }
  }

  @Test
  fun migrationAndSubmittingFlagsOverrideIdlePresentation() {
    val migrating = derive(
      route = DatabaseRootRouteInput.Error(DBMigrationResult.ErrorKeychain),
      dbMigrationInProgress = true,
      submitInFlight = true,
    )
    val ctrlInitSubmitting = derive(
      route = DatabaseRootRouteInput.Error(DBMigrationResult.InvalidConfirmation),
      ctrlInitInProgress = true,
    )
    val restoredReady = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorSQL("/tmp/chat.db", "ignored"),
      ),
      recoveryActionResult = DatabaseRecoveryActionResult.BACKUP_PAIR_COPIED,
    )
    val copyFailed = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorSQL("/tmp/chat.db", "ignored"),
      ),
      matchedBackupAvailable = true,
      recoveryActionResult = DatabaseRecoveryActionResult.BACKUP_PAIR_COPY_FAILED,
    )

    assertEquals(NomeDatabaseRootState.Migrating(submitting = true), migrating)
    assertEquals(
      NomeDatabaseRootState.InvalidConfirmation(
        submitting = true,
        matchedBackupAvailable = false,
      ),
      ctrlInitSubmitting,
    )
    assertEquals(
      NomeDatabaseRootState.RestoredPairReadyToOpen(submitting = false),
      restoredReady,
    )
    assertEquals(
      NomeDatabaseRootState.BackupPairCopyFailed(
        submitting = false,
        matchedBackupAvailable = true,
      ),
      copyFailed,
    )
  }

  @Test
  fun recoveryPresentationIsBoundToTheAttemptSourceState() {
    val store = NomeDatabaseRecoveryPresentationStore()
    val copySource = derive(
      route = DatabaseRootRouteInput.Error(
        DBMigrationResult.ErrorSQL("/tmp/chat.db", "ignored"),
      ),
      matchedBackupAvailable = true,
    )
    val newerModelState = derive(
      route = DatabaseRootRouteInput.Error(DBMigrationResult.ErrorKeychain),
      matchedBackupAvailable = true,
    )

    store.publish(
      attemptGeneration = 7L,
      sourceToken = 71L,
      sourceState = copySource,
      result = DatabaseRecoveryActionResult.BACKUP_PAIR_COPIED,
    )

    assertEquals(
      DatabaseRecoveryActionResult.BACKUP_PAIR_COPIED,
      store.resultFor(
        attemptGeneration = 7L,
        sourceToken = 71L,
        sourceState = copySource,
      ),
    )
    assertEquals(
      null,
      store.resultFor(
        attemptGeneration = 7L,
        sourceToken = 72L,
        sourceState = copySource,
      ),
    )
    assertEquals(
      null,
      store.resultFor(
        attemptGeneration = 7L,
        sourceToken = 71L,
        sourceState = copySource,
      ),
    )

    store.publish(
      attemptGeneration = 8L,
      sourceToken = 81L,
      sourceState = copySource,
      result = DatabaseRecoveryActionResult.BACKUP_PAIR_COPY_FAILED,
    )
    assertEquals(
      null,
      store.resultFor(
        attemptGeneration = 9L,
        sourceToken = 81L,
        sourceState = copySource,
      ),
    )
    assertEquals(
      null,
      store.resultFor(
        attemptGeneration = 8L,
        sourceToken = 81L,
        sourceState = newerModelState,
      ),
    )
  }

  @Test
  fun unsupportedClaimsStayOutOfTheSealedSurface() {
    val names = listOf(
      derive(route = DatabaseRootRouteInput.Opening),
      derive(route = DatabaseRootRouteInput.Migrating),
      derive(
        route = DatabaseRootRouteInput.Error(
          DBMigrationResult.ErrorNotADatabase("/tmp/chat.db"),
        ),
      ),
      derive(
        route = DatabaseRootRouteInput.Error(DBMigrationResult.InvalidConfirmation),
      ),
    ).map { it.javaClass.simpleName }

    assertTrue("Opening" in names)
    assertFalse("Timeout" in names)
    assertFalse("RollbackAvailable" in names)
    assertFalse("DeterminateProgress" in names)
    assertFalse("MessagingRestored" in names)
    assertFalse("IdentityLoaded" in names)
    assertFalse("WrongPassphrase" in names)
    assertFalse("CorruptionDetected" in names)
  }

  private fun derive(
    route: DatabaseRootRouteInput,
    ctrlInitInProgress: Boolean = false,
    dbMigrationInProgress: Boolean = false,
    storedKeyUseRequested: Boolean = false,
    storedKeyMaterialPresent: Boolean = false,
    androidKeyReadState: AndroidDatabaseKeyReadState? = null,
    matchedBackupAvailable: Boolean = false,
    downgradeWarningCount: Int = 0,
    submitInFlight: Boolean = false,
    recoveryActionResult: DatabaseRecoveryActionResult? = null,
  ): NomeDatabaseRootState =
    NomeDatabaseRootStateAdapter.derive(
      NomeDatabaseRootTruthInput(
        facts = NomeDatabaseRootFacts(
          route = route,
          ctrlInitInProgress = ctrlInitInProgress,
          dbMigrationInProgress = dbMigrationInProgress,
          storedKeyUseRequested = storedKeyUseRequested,
          storedKeyMaterialPresent = storedKeyMaterialPresent,
          androidKeyReadState = androidKeyReadState,
          matchedBackupAvailable = matchedBackupAvailable,
          downgradeWarningCount = downgradeWarningCount,
        ),
        submitInFlight = submitInFlight,
        recoveryActionResult = recoveryActionResult,
      ),
    )
}
