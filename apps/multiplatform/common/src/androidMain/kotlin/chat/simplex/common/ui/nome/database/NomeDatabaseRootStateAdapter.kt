package chat.simplex.common.ui.nome.database

import chat.simplex.common.views.database.AndroidDatabaseKeyReadState
import chat.simplex.common.views.database.DatabaseRootRouteInput
import chat.simplex.common.views.database.NomeDatabaseRootFacts
import chat.simplex.common.views.helpers.DBMigrationResult
import chat.simplex.common.views.helpers.MigrationError
import java.util.concurrent.atomic.AtomicLong

data class NomeDatabaseRootTruthInput(
  val facts: NomeDatabaseRootFacts,
  val submitInFlight: Boolean = false,
  val recoveryActionResult: DatabaseRecoveryActionResult? = null,
)

enum class DatabaseRecoveryActionResult {
  BACKUP_PAIR_COPIED,
  BACKUP_PAIR_COPY_FAILED,
}

sealed interface NomeDatabaseRootState {
  val submitting: Boolean
  val matchedBackupAvailable: Boolean

  data class Opening(
    override val submitting: Boolean,
  ) : NomeDatabaseRootState {
    override val matchedBackupAvailable: Boolean = false
  }

  data class Migrating(
    override val submitting: Boolean,
  ) : NomeDatabaseRootState {
    override val matchedBackupAvailable: Boolean = false
  }

  data class AlternateKeyRequired(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
    val storedKeyMaterialPresent: Boolean,
    val storedKeyUseRequested: Boolean,
  ) : NomeDatabaseRootState

  data class StoredManualKeyUnreadable(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
  ) : NomeDatabaseRootState

  data class StoredRandomKeyUnavailable(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
  ) : NomeDatabaseRootState

  data class UpgradeConsent(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
  ) : NomeDatabaseRootState

  data class DowngradeConsent(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
    val warningCount: Int,
  ) : NomeDatabaseRootState

  data class IncompatibleVersion(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
  ) : NomeDatabaseRootState

  data class DatabaseOpenFailed(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
  ) : NomeDatabaseRootState

  data class KeyStoreUnavailable(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
    val storedKeyMaterialPresent: Boolean,
  ) : NomeDatabaseRootState

  data class InvalidConfirmation(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
  ) : NomeDatabaseRootState

  data class UnknownFailure(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
  ) : NomeDatabaseRootState

  data class RestoredPairReadyToOpen(
    override val submitting: Boolean,
  ) : NomeDatabaseRootState {
    override val matchedBackupAvailable: Boolean = false
  }

  data class BackupPairCopyFailed(
    override val submitting: Boolean,
    override val matchedBackupAvailable: Boolean,
  ) : NomeDatabaseRootState
}

object NomeDatabaseRootStateAdapter {
  fun derive(input: NomeDatabaseRootTruthInput): NomeDatabaseRootState {
    val facts = input.facts
    val submitting =
      input.submitInFlight ||
        facts.ctrlInitInProgress ||
        facts.dbMigrationInProgress

    if (facts.dbMigrationInProgress || facts.route is DatabaseRootRouteInput.Migrating) {
      return NomeDatabaseRootState.Migrating(submitting = submitting)
    }
    when (input.recoveryActionResult) {
      DatabaseRecoveryActionResult.BACKUP_PAIR_COPIED ->
        return NomeDatabaseRootState.RestoredPairReadyToOpen(
          submitting = submitting,
        )

      DatabaseRecoveryActionResult.BACKUP_PAIR_COPY_FAILED ->
        return NomeDatabaseRootState.BackupPairCopyFailed(
          submitting = submitting,
          matchedBackupAvailable = facts.matchedBackupAvailable,
        )

      null -> Unit
    }

    return when (val route = facts.route) {
      DatabaseRootRouteInput.Opening ->
        if (facts.androidKeyReadState?.initialRandomDBPassphrase == true) {
          NomeDatabaseRootState.StoredRandomKeyUnavailable(
            submitting = submitting,
            matchedBackupAvailable = facts.matchedBackupAvailable,
          )
        } else {
          NomeDatabaseRootState.Opening(submitting = submitting)
        }

      DatabaseRootRouteInput.Migrating ->
        NomeDatabaseRootState.Migrating(submitting = submitting)

      is DatabaseRootRouteInput.Error ->
        route.status.toDisplayState(
          facts = facts,
          submitting = submitting,
        )
    }
  }
}

class NomeDatabaseRecoveryPresentationStore {
  private data class Entry(
    val attemptGeneration: Long,
    val sourceToken: Long,
    val sourceState: NomeDatabaseRootState,
    val result: DatabaseRecoveryActionResult,
  )

  private val current = java.util.concurrent.atomic.AtomicReference<Entry?>(null)

  fun publish(
    attemptGeneration: Long,
    sourceToken: Long,
    sourceState: NomeDatabaseRootState,
    result: DatabaseRecoveryActionResult,
  ) {
    current.set(
      Entry(
        attemptGeneration = attemptGeneration,
        sourceToken = sourceToken,
        sourceState = sourceState,
        result = result,
      ),
    )
  }

  fun resultFor(
    attemptGeneration: Long?,
    sourceToken: Long,
    sourceState: NomeDatabaseRootState,
  ): DatabaseRecoveryActionResult? {
    while (true) {
      val entry = current.get() ?: return null
      if (
        entry.attemptGeneration == attemptGeneration &&
        entry.sourceToken == sourceToken &&
        entry.sourceState == sourceState
      ) {
        return entry.result
      }
      if (current.compareAndSet(entry, null)) return null
    }
  }

  fun clear() {
    current.set(null)
  }
}

class NomeDatabaseAttemptGate {
  private val nextGeneration = AtomicLong(1L)
  private val activeGeneration = AtomicLong(0L)
  private val completedGeneration = AtomicLong(0L)

  fun beginAttempt(): Long? {
    while (true) {
      if (activeGeneration.get() != 0L) return null
      val generation = nextGeneration.getAndIncrement()
      if (activeGeneration.compareAndSet(0L, generation)) {
        return generation
      }
    }
  }

  fun activeGenerationOrNull(): Long? =
    activeGeneration.get().takeIf { it != 0L }

  fun completeAttempt(generation: Long): Boolean {
    if (!activeGeneration.compareAndSet(generation, 0L)) return false
    completedGeneration.set(generation)
    return true
  }

  fun completedGenerationOrNull(): Long? =
    completedGeneration.get().takeIf { it != 0L }
}

private fun DBMigrationResult.toDisplayState(
  facts: NomeDatabaseRootFacts,
  submitting: Boolean,
): NomeDatabaseRootState {
  val matchedBackupAvailable = facts.matchedBackupAvailable
  return when (this) {
    DBMigrationResult.OK ->
      NomeDatabaseRootState.Opening(submitting = submitting)

    DBMigrationResult.InvalidConfirmation ->
      NomeDatabaseRootState.InvalidConfirmation(
        submitting = submitting,
        matchedBackupAvailable = matchedBackupAvailable,
      )

    is DBMigrationResult.ErrorNotADatabase -> {
      val keyReadState = facts.androidKeyReadState
      when {
        keyReadState?.initialRandomDBPassphrase == true ->
          NomeDatabaseRootState.StoredRandomKeyUnavailable(
            submitting = submitting,
            matchedBackupAvailable = matchedBackupAvailable,
          )

        keyReadState != null ->
          NomeDatabaseRootState.StoredManualKeyUnreadable(
            submitting = submitting,
            matchedBackupAvailable = matchedBackupAvailable,
          )

        else ->
          NomeDatabaseRootState.AlternateKeyRequired(
            submitting = submitting,
            matchedBackupAvailable = matchedBackupAvailable,
            storedKeyMaterialPresent = facts.storedKeyMaterialPresent,
            storedKeyUseRequested = facts.storedKeyUseRequested,
          )
      }
    }

    is DBMigrationResult.ErrorMigration ->
      when (migrationError) {
        is MigrationError.Upgrade ->
          NomeDatabaseRootState.UpgradeConsent(
            submitting = submitting,
            matchedBackupAvailable = matchedBackupAvailable,
          )

        is MigrationError.Downgrade ->
          NomeDatabaseRootState.DowngradeConsent(
            submitting = submitting,
            matchedBackupAvailable = matchedBackupAvailable,
            warningCount = facts.downgradeWarningCount.coerceAtLeast(0),
          )

        is MigrationError.Error ->
          NomeDatabaseRootState.IncompatibleVersion(
            submitting = submitting,
            matchedBackupAvailable = matchedBackupAvailable,
          )
      }

    is DBMigrationResult.ErrorSQL ->
      NomeDatabaseRootState.DatabaseOpenFailed(
        submitting = submitting,
        matchedBackupAvailable = matchedBackupAvailable,
      )

    DBMigrationResult.ErrorKeychain ->
      NomeDatabaseRootState.KeyStoreUnavailable(
        submitting = submitting,
        matchedBackupAvailable = matchedBackupAvailable,
        storedKeyMaterialPresent = facts.storedKeyMaterialPresent,
      )

    is DBMigrationResult.Unknown ->
      NomeDatabaseRootState.UnknownFailure(
        submitting = submitting,
        matchedBackupAvailable = matchedBackupAvailable,
      )
  }
}
