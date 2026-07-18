package chat.simplex.common.ui.nome.database

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import chat.simplex.common.R
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeStatePanel
import chat.simplex.common.ui.nome.components.NomeStatePanelAction
import chat.simplex.common.ui.nome.components.NomeStatePanelState
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.views.database.validKey
import kotlinx.coroutines.delay

data class NomeDatabaseRootActions(
  val openOnce: (String) -> Unit,
  val saveAndOpen: (String) -> Unit,
  val confirmUpgrade: () -> Unit,
  val confirmDowngrade: () -> Unit,
  val copyMatchedBackup: () -> Unit,
  val openRestored: () -> Unit,
)

@Composable
fun NomeDatabaseRootRoute(
  state: NomeDatabaseRootState,
  passphrase: MutableState<String>,
  actions: NomeDatabaseRootActions,
  modifier: Modifier = Modifier,
) {
  val dimensions = NomeTheme.dimensions
  val colors = NomeTheme.colors
  val focusRequester = remember { FocusRequester() }
  var confirmBackup by remember { mutableStateOf(false) }
  val showKeyEntry =
    state is NomeDatabaseRootState.AlternateKeyRequired ||
      state is NomeDatabaseRootState.StoredManualKeyUnreadable
  val keyValid = validKey(passphrase.value)
  val primaryAction = state.primaryAction(actions, passphrase.value, keyValid)
  val secondaryAction = state.secondaryAction(actions, passphrase.value, keyValid)

  DisposableEffect(Unit) {
    onDispose {
      passphrase.value = ""
    }
  }
  LaunchedEffect(showKeyEntry) {
    if (!showKeyEntry) {
      passphrase.value = ""
    }
  }
  LaunchedEffect(state::class) {
    if (
      state !is NomeDatabaseRootState.Opening &&
      state !is NomeDatabaseRootState.Migrating
    ) {
      delay(100)
      focusRequester.requestFocus()
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .imePadding()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = dimensions.screenHorizontalInset)
      .padding(vertical = dimensions.space24),
    verticalArrangement = Arrangement.spacedBy(dimensions.space16),
  ) {
    Text(
      text = stringResource(R.string.nome_database_root_title),
      modifier = Modifier
        .focusRequester(focusRequester)
        .focusable()
        .semantics { heading() },
      style = NomeTheme.typography.display,
      color = colors.textPrimary,
    )
    Text(
      text = stringResource(R.string.nome_database_root_security_note),
      style = NomeTheme.typography.body,
      color = colors.textSecondary,
    )

    NomeStatePanel(
      state = state.panelState(),
      title = state.title(),
      description = state.description(),
      stateDescription = state.stateDescription(),
      primaryAction = primaryAction,
      secondaryAction = secondaryAction,
    )

    if (showKeyEntry) {
      OutlinedTextField(
        value = passphrase.value,
        onValueChange = { passphrase.value = it },
        modifier = Modifier
          .fillMaxWidth()
          .semantics { password() },
        label = {
          Text(stringResource(R.string.nome_database_root_passphrase_label))
        },
        enabled = !state.submitting,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Password,
          imeAction = ImeAction.Done,
          autoCorrect = false,
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            if (keyValid && !state.submitting) {
              actions.openOnce(passphrase.value)
            }
          },
        ),
      )
      Text(
        text = stringResource(R.string.nome_database_root_passphrase_private),
        style = NomeTheme.typography.supporting,
        color = colors.textSecondary,
      )
    }

    if (state.matchedBackupAvailable) {
      NomeButton(
        text = stringResource(R.string.nome_database_root_copy_backup),
        onClick = { confirmBackup = true },
        modifier = Modifier.fillMaxWidth(),
        variant = NomeButtonVariant.DESTRUCTIVE,
        enabled = !state.submitting,
      )
      Text(
        text = stringResource(R.string.nome_database_root_backup_limit),
        style = NomeTheme.typography.supporting,
        color = colors.textSecondary,
      )
    }
    Spacer(Modifier.height(dimensions.space8))
  }

  if (confirmBackup) {
    AlertDialog(
      onDismissRequest = { confirmBackup = false },
      title = {
        Text(stringResource(R.string.nome_database_root_backup_confirm_title))
      },
      text = {
        Text(stringResource(R.string.nome_database_root_backup_confirm_body))
      },
      confirmButton = {
        NomeButton(
          text = stringResource(R.string.nome_database_root_backup_confirm),
          onClick = {
            confirmBackup = false
            actions.copyMatchedBackup()
          },
          variant = NomeButtonVariant.DESTRUCTIVE,
          enabled = !state.submitting,
        )
      },
      dismissButton = {
        NomeButton(
          text = stringResource(R.string.nome_database_root_not_now),
          onClick = { confirmBackup = false },
          variant = NomeButtonVariant.SECONDARY,
        )
      },
    )
  }
}

@Composable
private fun NomeDatabaseRootState.title(): String = stringResource(
  when (this) {
    is NomeDatabaseRootState.Opening -> R.string.nome_database_root_opening_title
    is NomeDatabaseRootState.Migrating -> R.string.nome_database_root_migrating_title
    is NomeDatabaseRootState.AlternateKeyRequired -> R.string.nome_database_root_key_title
    is NomeDatabaseRootState.StoredManualKeyUnreadable ->
      R.string.nome_database_root_stored_key_title
    is NomeDatabaseRootState.StoredRandomKeyUnavailable ->
      R.string.nome_database_root_random_key_title
    is NomeDatabaseRootState.UpgradeConsent -> R.string.nome_database_root_upgrade_title
    is NomeDatabaseRootState.DowngradeConsent -> R.string.nome_database_root_downgrade_title
    is NomeDatabaseRootState.IncompatibleVersion ->
      R.string.nome_database_root_incompatible_title
    is NomeDatabaseRootState.DatabaseOpenFailed -> R.string.nome_database_root_failed_title
    is NomeDatabaseRootState.KeyStoreUnavailable -> R.string.nome_database_root_keystore_title
    is NomeDatabaseRootState.InvalidConfirmation ->
      R.string.nome_database_root_confirmation_title
    is NomeDatabaseRootState.UnknownFailure -> R.string.nome_database_root_unknown_title
    is NomeDatabaseRootState.RestoredPairReadyToOpen ->
      R.string.nome_database_root_restored_title
    is NomeDatabaseRootState.BackupPairCopyFailed ->
      R.string.nome_database_root_backup_failed_title
  },
)

@Composable
private fun NomeDatabaseRootState.description(): String = when (this) {
  is NomeDatabaseRootState.Opening ->
    stringResource(R.string.nome_database_root_opening_body)
  is NomeDatabaseRootState.Migrating ->
    stringResource(R.string.nome_database_root_migrating_body)
  is NomeDatabaseRootState.AlternateKeyRequired ->
    stringResource(R.string.nome_database_root_key_body)
  is NomeDatabaseRootState.StoredManualKeyUnreadable ->
    stringResource(R.string.nome_database_root_stored_key_body)
  is NomeDatabaseRootState.StoredRandomKeyUnavailable ->
    stringResource(R.string.nome_database_root_random_key_body)
  is NomeDatabaseRootState.UpgradeConsent ->
    stringResource(R.string.nome_database_root_upgrade_body)
  is NomeDatabaseRootState.DowngradeConsent ->
    stringResource(
      R.string.nome_database_root_downgrade_body,
      warningCount,
    )
  is NomeDatabaseRootState.IncompatibleVersion ->
    stringResource(R.string.nome_database_root_incompatible_body)
  is NomeDatabaseRootState.DatabaseOpenFailed ->
    stringResource(R.string.nome_database_root_failed_body)
  is NomeDatabaseRootState.KeyStoreUnavailable ->
    stringResource(R.string.nome_database_root_keystore_body)
  is NomeDatabaseRootState.InvalidConfirmation ->
    stringResource(R.string.nome_database_root_confirmation_body)
  is NomeDatabaseRootState.UnknownFailure ->
    stringResource(R.string.nome_database_root_unknown_body)
  is NomeDatabaseRootState.RestoredPairReadyToOpen ->
    stringResource(R.string.nome_database_root_restored_body)
  is NomeDatabaseRootState.BackupPairCopyFailed ->
    stringResource(R.string.nome_database_root_backup_failed_body)
}

@Composable
private fun NomeDatabaseRootState.stateDescription(): String =
  stringResource(
    if (submitting) {
      R.string.nome_database_root_submitting
    } else {
      R.string.nome_database_root_waiting
    },
  )

private fun NomeDatabaseRootState.panelState(): NomeStatePanelState = when (this) {
  is NomeDatabaseRootState.Opening,
  is NomeDatabaseRootState.Migrating -> NomeStatePanelState.LOADING
  is NomeDatabaseRootState.UpgradeConsent,
  is NomeDatabaseRootState.RestoredPairReadyToOpen -> NomeStatePanelState.PERMISSION
  is NomeDatabaseRootState.DowngradeConsent,
  is NomeDatabaseRootState.StoredRandomKeyUnavailable -> NomeStatePanelState.DANGER
  is NomeDatabaseRootState.AlternateKeyRequired,
  is NomeDatabaseRootState.StoredManualKeyUnreadable,
  is NomeDatabaseRootState.IncompatibleVersion,
  is NomeDatabaseRootState.DatabaseOpenFailed,
  is NomeDatabaseRootState.KeyStoreUnavailable,
  is NomeDatabaseRootState.InvalidConfirmation,
  is NomeDatabaseRootState.UnknownFailure,
  is NomeDatabaseRootState.BackupPairCopyFailed -> NomeStatePanelState.ERROR
}

@Composable
private fun NomeDatabaseRootState.primaryAction(
  actions: NomeDatabaseRootActions,
  passphrase: String,
  keyValid: Boolean,
): NomeStatePanelAction? = when (this) {
  is NomeDatabaseRootState.AlternateKeyRequired,
  is NomeDatabaseRootState.StoredManualKeyUnreadable -> NomeStatePanelAction(
    label = stringResource(R.string.nome_database_root_open_once),
    onClick = { actions.openOnce(passphrase) },
    enabled = keyValid && !submitting,
  )
  is NomeDatabaseRootState.UpgradeConsent -> NomeStatePanelAction(
    label = stringResource(R.string.nome_database_root_upgrade_action),
    onClick = actions.confirmUpgrade,
    enabled = !submitting,
  )
  is NomeDatabaseRootState.DowngradeConsent -> NomeStatePanelAction(
    label = stringResource(R.string.nome_database_root_downgrade_action),
    onClick = actions.confirmDowngrade,
    variant = NomeButtonVariant.DESTRUCTIVE,
    enabled = !submitting,
  )
  is NomeDatabaseRootState.RestoredPairReadyToOpen -> NomeStatePanelAction(
    label = stringResource(R.string.nome_database_root_open_restored),
    onClick = actions.openRestored,
    enabled = !submitting,
  )
  else -> null
}

@Composable
private fun NomeDatabaseRootState.secondaryAction(
  actions: NomeDatabaseRootActions,
  passphrase: String,
  keyValid: Boolean,
): NomeStatePanelAction? = when (this) {
  is NomeDatabaseRootState.AlternateKeyRequired,
  is NomeDatabaseRootState.StoredManualKeyUnreadable -> NomeStatePanelAction(
    label = stringResource(R.string.nome_database_root_save_and_open),
    onClick = { actions.saveAndOpen(passphrase) },
    variant = NomeButtonVariant.SECONDARY,
    enabled = keyValid && !submitting,
  )
  else -> null
}
