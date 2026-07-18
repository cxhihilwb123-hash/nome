package chat.simplex.common.views.database

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import chat.simplex.common.model.ChatModel
import chat.simplex.common.platform.platform
import chat.simplex.common.ui.nome.database.DatabaseRecoveryActionResult
import chat.simplex.common.ui.nome.database.NomeDatabaseAttemptGate
import chat.simplex.common.ui.nome.database.NomeDatabaseRecoveryPresentationStore
import chat.simplex.common.ui.nome.database.NomeDatabaseRootActions
import chat.simplex.common.ui.nome.database.NomeDatabaseRootRoute
import chat.simplex.common.ui.nome.database.NomeDatabaseRootState
import chat.simplex.common.ui.nome.database.NomeDatabaseRootStateAdapter
import chat.simplex.common.ui.nome.database.NomeDatabaseRootTruthInput
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.DBMigrationResult
import chat.simplex.common.views.helpers.MigrationConfirmation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val androidDatabaseKeyReadState =
  mutableStateOf<AndroidDatabaseKeyReadState?>(null)

private object NomeDatabaseRecoveryProcess {
  val gate = NomeDatabaseAttemptGate()
  val presentationStore = NomeDatabaseRecoveryPresentationStore()
  val revision = mutableStateOf(0L)
  val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  fun publish() {
    revision.value = if (revision.value == Long.MAX_VALUE) 1L else revision.value + 1L
  }
}

@Composable
actual fun PlatformDatabaseRootRoute(
  facts: NomeDatabaseRootFacts,
  allowSensitiveContent: Boolean,
  legacyContent: @Composable () -> Unit,
) {
  if (!allowSensitiveContent) {
    NomeDatabaseRecoveryProcess.presentationStore.clear()
    Box(
      Modifier
        .fillMaxSize()
        .clearAndSetSemantics {},
    )
    return
  }

  NomeDatabaseRecoveryProcess.revision.value
  val passphrase = remember { mutableStateOf("") }
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) {
        passphrase.value = ""
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      passphrase.value = ""
    }
  }

  val sourceState = NomeDatabaseRootStateAdapter.derive(
    NomeDatabaseRootTruthInput(facts = facts),
  )
  val sourceToken = facts.sourceToken()
  val derivedState = NomeDatabaseRootStateAdapter.derive(
    NomeDatabaseRootTruthInput(
      facts = facts,
      submitInFlight =
        NomeDatabaseRecoveryProcess.gate.activeGenerationOrNull() != null,
      recoveryActionResult =
        NomeDatabaseRecoveryProcess.presentationStore.resultFor(
          attemptGeneration =
            NomeDatabaseRecoveryProcess.gate.completedGenerationOrNull(),
          sourceToken = sourceToken,
          sourceState = sourceState,
        ),
    ),
  )
  val actions = NomeDatabaseRootActions(
    openOnce = { key ->
      submitDatabaseAttempt(
        dbKey = key,
        clearKeyReadFailure = true,
      )
      passphrase.value = ""
    },
    saveAndOpen = { key ->
      submitDatabaseAttempt(
        dbKey = key,
        saveKey = true,
        clearKeyReadFailure = true,
      )
      passphrase.value = ""
    },
    confirmUpgrade = {
      submitDatabaseAttempt(
        confirmMigrations = MigrationConfirmation.YesUp,
      )
    },
    confirmDowngrade = {
      submitDatabaseAttempt(
        confirmMigrations = MigrationConfirmation.YesUpDown,
      )
    },
    copyMatchedBackup = {
      copyMatchedBackup(
        sourceState = sourceState,
        sourceToken = sourceToken,
      )
    },
    openRestored = {
      submitDatabaseAttempt(
        clearKeyReadFailure = true,
        clearRecoveryPresentation = true,
      )
    },
  )

  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeDatabaseRootRoute(
      state = derivedState,
      passphrase = passphrase,
      actions = actions,
    )
  }
}

actual fun platformDatabaseKeyReadState(): AndroidDatabaseKeyReadState? =
  androidDatabaseKeyReadState.value

actual fun clearPlatformDatabaseKeyReadState() {
  androidDatabaseKeyReadState.value = null
}

fun reportPlatformDatabaseKeyReadState(state: AndroidDatabaseKeyReadState) {
  androidDatabaseKeyReadState.value = state
}

private fun submitDatabaseAttempt(
  dbKey: String? = null,
  confirmMigrations: MigrationConfirmation? = null,
  saveKey: Boolean = false,
  clearKeyReadFailure: Boolean = false,
  clearRecoveryPresentation: Boolean = false,
) {
  val generation = NomeDatabaseRecoveryProcess.gate.beginAttempt() ?: return
  if (clearRecoveryPresentation) {
    NomeDatabaseRecoveryProcess.presentationStore.clear()
  }
  NomeDatabaseRecoveryProcess.publish()
  NomeDatabaseRecoveryProcess.scope.launch {
    var status: DBMigrationResult? = null
    try {
      if (clearKeyReadFailure) {
        clearPlatformDatabaseKeyReadState()
      }
      if (saveKey) {
        requireNotNull(dbKey)
        saveDatabaseKeyForRecovery(
          dbKey,
          ChatModel.controller.appPrefs,
        )
      }
      status = openDatabaseForRecovery(
        dbKey = dbKey,
        confirmMigrations = confirmMigrations,
        chatDbStatus = ChatModel.chatDbStatus,
      )
      if (
        status is DBMigrationResult.OK &&
        NomeDatabaseRecoveryProcess.gate.activeGenerationOrNull() == generation
      ) {
        clearPlatformDatabaseKeyReadState()
        NomeDatabaseRecoveryProcess.presentationStore.clear()
        platform.androidChatStartedAfterBeingOff()
      }
    } catch (_: Throwable) {
      // The model/core terminal subtype remains the only database truth source.
    } finally {
      if (NomeDatabaseRecoveryProcess.gate.completeAttempt(generation)) {
        NomeDatabaseRecoveryProcess.publish()
      }
    }
  }
}

private fun copyMatchedBackup(
  sourceState: NomeDatabaseRootState,
  sourceToken: Long,
) {
  val generation = NomeDatabaseRecoveryProcess.gate.beginAttempt() ?: return
  NomeDatabaseRecoveryProcess.presentationStore.clear()
  NomeDatabaseRecoveryProcess.publish()
  NomeDatabaseRecoveryProcess.scope.launch {
    val result = copyMatchedDatabaseBackupPair(
      ChatModel.controller.appPrefs,
    )
    if (NomeDatabaseRecoveryProcess.gate.completeAttempt(generation)) {
      NomeDatabaseRecoveryProcess.presentationStore.publish(
        attemptGeneration = generation,
        sourceToken = sourceToken,
        sourceState = sourceState,
        result = if (result == DatabaseBackupCopyResult.COPIED) {
          DatabaseRecoveryActionResult.BACKUP_PAIR_COPIED
        } else {
          DatabaseRecoveryActionResult.BACKUP_PAIR_COPY_FAILED
        },
      )
      NomeDatabaseRecoveryProcess.publish()
    }
  }
}

private fun NomeDatabaseRootFacts.sourceToken(): Long =
  when (val currentRoute = route) {
    DatabaseRootRouteInput.Opening -> Long.MIN_VALUE + 1L
    DatabaseRootRouteInput.Migrating -> Long.MIN_VALUE + 2L
    is DatabaseRootRouteInput.Error -> {
      val subtype = when (currentRoute.status) {
        DBMigrationResult.OK -> 1L
        DBMigrationResult.InvalidConfirmation -> 2L
        is DBMigrationResult.ErrorNotADatabase -> 3L
        is DBMigrationResult.ErrorMigration -> 4L
        is DBMigrationResult.ErrorSQL -> 5L
        DBMigrationResult.ErrorKeychain -> 6L
        is DBMigrationResult.Unknown -> 7L
      }
      (subtype shl 32) or
        (System.identityHashCode(currentRoute.status).toLong() and 0xffffffffL)
    }
  }
