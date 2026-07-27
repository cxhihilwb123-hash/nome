package chat.simplex.common.activation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
  if (!visible) {
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
  val proactiveRenewal = state.access == ActivationAccess.FULL
  val dismiss = {
    inviteCode = ""
    errorMessage = null
    ActivationGate.dismissActivation()
  }
  Dialog(onDismissRequest = dismiss) {
    Surface(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .widthIn(max = 420.dp),
      shape = RoundedCornerShape(24.dp),
      elevation = 12.dp,
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier
                .size(40.dp)
                .background(
                  MaterialTheme.colors.primary.copy(alpha = 0.1f),
                  RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.ConfirmationNumber,
              contentDescription = null,
              modifier = Modifier.size(21.dp),
              tint = MaterialTheme.colors.primary,
            )
          }
          Spacer(Modifier.width(12.dp))
          Text(
            text =
              stringResource(
                if (proactiveRenewal) {
                  R.string.nome_activation_renew_title
                } else {
                  R.string.nome_activation_sheet_title
                },
              ),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.h6,
          )
          IconButton(
            enabled = !state.operationInProgress,
            onClick = dismiss,
            modifier = Modifier.size(40.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = stringResource(R.string.nome_activation_close),
              tint = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
            )
          }
        }
        Text(
          text =
            stringResource(
              when {
                migrationRequired -> R.string.nome_activation_migration_body
                proactiveRenewal -> R.string.nome_activation_renew_body
                state.access == ActivationAccess.CHECK_REQUIRED -> R.string.nome_activation_check_required
                state.entitlement.status == ActivationEntitlementStatus.EXPIRED ||
                  state.reason == "entitlement_expired" -> R.string.nome_activation_expired_body
                else -> R.string.nome_activation_sheet_body
              },
            ),
          style = MaterialTheme.typography.body2,
          color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
        )
        if (!migrationRequired) {
          OutlinedTextField(
            value = inviteCode,
            onValueChange = {
              inviteCode = it.trim().uppercase()
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
        errorMessage?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.error,
          )
        }
        if (state.usingOfflineGrace) {
          Text(
            text = stringResource(R.string.nome_activation_offline_grace),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
          )
        }
        Button(
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp),
          enabled = (migrationRequired || inviteCode.isNotBlank()) && !state.operationInProgress,
          onClick = {
            scope.launch {
              val operation =
                if (migrationRequired) {
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
            CircularProgressIndicator(
              modifier = Modifier.height(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colors.onPrimary,
            )
          } else {
            Text(
              stringResource(
                when {
                  migrationRequired -> R.string.nome_activation_migrate
                  proactiveRenewal -> R.string.nome_activation_renew
                  else -> R.string.nome_activation_activate
                },
              ),
            )
          }
        }
      }
    }
  }
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
