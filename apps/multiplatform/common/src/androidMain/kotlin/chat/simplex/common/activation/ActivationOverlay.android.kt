package chat.simplex.common.activation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.model.ChatModel
import chat.simplex.common.views.chatlist.connectIfOpenedViaUri
import kotlinx.coroutines.launch

@Composable
actual fun PlatformActivationOverlay() {
  val visible by ActivationGate.sheetVisible.collectAsState()
  val state by ActivationGate.state.collectAsState()
  val pendingReviewVisible by ActivationGate.pendingReviewVisible.collectAsState()
  val pendingIntent by ActivationGate.pendingIntent.collectAsState()
  if (!visible || !state.shouldShowActivation) {
    if (
      pendingReviewVisible &&
      state.permitsChatNetworking &&
      pendingIntent?.capability == ActivationCapability.DEEP_LINK
    ) {
      PendingDeepLinkReview()
    }
    return
  }

  var inviteCode by rememberSaveable { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  val migrationRequired = state.access == ActivationAccess.MIGRATION_REQUIRED
  val dismiss = {
    inviteCode = ""
    errorMessage = null
    ActivationGate.dismissActivation()
  }
  AlertDialog(
    onDismissRequest = dismiss,
    title = { Text(stringResource(R.string.nome_activation_sheet_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          stringResource(
            when {
              migrationRequired -> R.string.nome_activation_migration_body
              state.access == ActivationAccess.CHECK_REQUIRED -> R.string.nome_activation_check_required
              else -> R.string.nome_activation_sheet_body
            },
          ),
        )
        if (!migrationRequired) {
          OutlinedTextField(
            value = inviteCode,
            onValueChange = {
              inviteCode = it
              errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.operationInProgress,
            singleLine = true,
            label = { Text(stringResource(R.string.nome_activation_invite_code)) },
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Characters,
              imeAction = ImeAction.Done,
            ),
          )
        }
        errorMessage?.let { Text(it, color = androidx.compose.material.MaterialTheme.colors.error) }
        if (state.usingOfflineGrace) Text(stringResource(R.string.nome_activation_offline_grace))
      }
    },
    confirmButton = {
      Button(
        enabled = (migrationRequired || inviteCode.isNotBlank()) && !state.operationInProgress,
        onClick = {
          scope.launch {
            val operation = if (migrationRequired) {
              ActivationGate.migrateInstallation()
            } else {
              ActivationGate.redeem(inviteCode)
            }
            when (operation) {
              is ActivationOperationResult.Success -> {
                inviteCode = ""
                errorMessage = null
              }
              is ActivationOperationResult.Failure -> errorMessage = operation.message
            }
          }
        },
      ) {
        if (state.operationInProgress) {
          CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
        } else {
          Text(
            stringResource(
              if (migrationRequired) R.string.nome_activation_migrate else R.string.nome_activation_activate,
            ),
          )
        }
      }
    },
    dismissButton = {
      Column {
        TextButton(
          enabled = !state.operationInProgress,
          onClick = {
            scope.launch {
              val policyResult = ActivationGate.refreshPolicy(force = true)
              if (policyResult is ActivationOperationResult.Failure) {
                errorMessage = policyResult.message
              } else if (ActivationGate.state.value.entitlement.status != ActivationEntitlementStatus.UNACTIVATED) {
                val entitlementResult = ActivationGate.refreshEntitlement()
                if (entitlementResult is ActivationOperationResult.Failure) errorMessage = entitlementResult.message
              }
            }
          },
        ) { Text(stringResource(R.string.nome_activation_retry)) }
        Spacer(Modifier.height(2.dp))
        TextButton(enabled = !state.operationInProgress, onClick = dismiss) {
          Text(stringResource(R.string.nome_activation_not_now))
        }
      }
    },
  )
}

@Composable
private fun PendingDeepLinkReview() {
  val pendingUrl = ChatModel.appOpenUrl.value
  val scope = rememberCoroutineScope()
  if (pendingUrl == null) {
    LaunchedEffect(Unit) { ActivationGate.clearPendingIntent() }
    return
  }
  AlertDialog(
    onDismissRequest = ActivationGate::deferPendingReview,
    title = { Text(stringResource(R.string.nome_activation_pending_link_title)) },
    text = { Text(stringResource(R.string.nome_activation_pending_link_body)) },
    confirmButton = {
      Button(
        onClick = {
          scope.launch {
            if (ActivationGate.guardFresh(ActivationCapability.DEEP_LINK, "pending_link_confirm")) {
              ChatModel.appOpenUrl.value = null
              ActivationGate.clearPendingIntent()
              connectIfOpenedViaUri(
                rhId = pendingUrl.remoteHostId,
                uri = pendingUrl.uri,
                chatModel = ChatModel,
                source = pendingUrl.source,
              )
            }
          }
        },
      ) { Text(stringResource(R.string.nome_activation_review_link)) }
    },
    dismissButton = {
      TextButton(onClick = ActivationGate::deferPendingReview) {
        Text(stringResource(R.string.nome_activation_keep_pending))
      }
    },
  )
}
