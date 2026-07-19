package chat.simplex.common.views.usersettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.model.UserContactLinkRec
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.platform.shareText
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.newchat.SimpleXCreatedLinkQRCode
import kotlinx.coroutines.launch

private enum class NomeAddressAction {
  NONE,
  RELOAD,
  CREATE,
  CONFIRMATION,
  DELETE,
  REPLACE_DELETE,
  REPLACE_CREATE,
}

private enum class NomeAddressConfirmation {
  NONE,
  DELETE,
  REPLACE,
}

@Composable
internal actual fun PlatformUserAddressRoute(
  userAddress: UserContactLinkRec?,
  loadState: NomeUserAddressLoadState,
  onReload: suspend () -> Unit,
  onCreate: suspend () -> Boolean,
  onSetRequiresConfirmation: suspend (Boolean) -> Boolean,
  onDelete: suspend () -> Boolean,
  onAddShortLink: () -> Unit,
  onShareAddress: (String) -> Unit,
  onOpenAdvanced: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomePublicContactMethodContent(
      userAddress = userAddress,
      loadState = loadState,
      onReload = onReload,
      onCreate = onCreate,
      onSetRequiresConfirmation =
        onSetRequiresConfirmation,
      onDelete = onDelete,
      onAddShortLink = onAddShortLink,
      onShareAddress = onShareAddress,
      onOpenAdvanced = onOpenAdvanced,
      onClose = onClose,
    )
  }
}

@Composable
fun NomePublicContactMethodContent(
  userAddress: UserContactLinkRec?,
  loadState: NomeUserAddressLoadState,
  onReload: suspend () -> Unit,
  onCreate: suspend () -> Boolean,
  onSetRequiresConfirmation: suspend (Boolean) -> Boolean,
  onDelete: suspend () -> Boolean,
  onAddShortLink: (() -> Unit)? = null,
  onShareAddress: ((String) -> Unit)? = null,
  onOpenAdvanced: () -> Unit,
  onClose: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val clipboard = LocalClipboardManager.current
  var action by rememberSaveable {
    mutableStateOf(NomeAddressAction.NONE)
  }
  var confirmation by rememberSaveable {
    mutableStateOf(NomeAddressConfirmation.NONE)
  }
  var failed by rememberSaveable {
    mutableStateOf(false)
  }
  var oldAddressDeleted by rememberSaveable {
    mutableStateOf(false)
  }
  val busy = action != NomeAddressAction.NONE

  fun runBooleanAction(
    next: NomeAddressAction,
    block: suspend () -> Boolean,
  ) {
    if (action != NomeAddressAction.NONE) return
    action = next
    failed = false
    scope.launch {
      val succeeded = block()
      action = NomeAddressAction.NONE
      failed = !succeeded
    }
  }

  fun replaceAddress() {
    if (action != NomeAddressAction.NONE) return
    confirmation = NomeAddressConfirmation.NONE
    failed = false
    action = NomeAddressAction.REPLACE_DELETE
    scope.launch {
      if (!onDelete()) {
        action = NomeAddressAction.NONE
        failed = true
        return@launch
      }
      oldAddressDeleted = true
      action = NomeAddressAction.REPLACE_CREATE
      if (onCreate()) {
        oldAddressDeleted = false
        action = NomeAddressAction.NONE
      } else {
        action = NomeAddressAction.NONE
        failed = true
      }
    }
  }

  fun reloadAddress() {
    if (action != NomeAddressAction.NONE) return
    action = NomeAddressAction.RELOAD
    failed = false
    scope.launch {
      onReload()
      action = NomeAddressAction.NONE
    }
  }

  NomeFullPageScaffold(
    title =
      androidx.compose.ui.res.stringResource(
        R.string.nome_p15_title,
      ),
    backLabel =
      androidx.compose.ui.res.stringResource(
        R.string.nome_p15_back,
      ),
    onClose = if (busy) ({}) else onClose,
  ) {
    NomeAddressStrip(
      icon = Icons.Rounded.Language,
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p15_intro,
        ),
      container = NomeTheme.colors.surfaceSubtle,
      content = NomeTheme.colors.textSecondary,
    )

    if (
      busy ||
        failed ||
        loadState == NomeUserAddressLoadState.FAILURE
    ) {
      NomeAddressStrip(
        icon = Icons.Rounded.Security,
        text =
          androidx.compose.ui.res.stringResource(
            if (
              failed ||
                loadState ==
                  NomeUserAddressLoadState.FAILURE
            ) {
              R.string.nome_p15_action_failed
            } else {
              R.string.nome_p15_action_in_progress
            },
          ),
        container =
          if (
            failed ||
              loadState ==
                NomeUserAddressLoadState.FAILURE
          ) {
            NomeTheme.colors.dangerContainer
          } else {
            NomeTheme.colors.infoContainer
          },
        content =
          if (
            failed ||
              loadState ==
                NomeUserAddressLoadState.FAILURE
          ) {
            NomeTheme.colors.onDangerContainer
          } else {
            NomeTheme.colors.onInfoContainer
          },
        live = true,
      )
    }

    when {
      userAddress != null -> {
        NomeReadyAddressCard(
          userAddress = userAddress,
          busy = busy,
          shouldBeUpgraded = userAddress.shouldBeUpgraded,
          onAddShortLink = onAddShortLink,
          onCopy = {
            clipboard.setText(
              AnnotatedString(
                userAddress.connLinkContact
                  .simplexChatUri(short = true),
              ),
            )
          },
          onShare = {
            val address =
              userAddress.connLinkContact
                .simplexChatUri(short = true)
            if (onShareAddress == null) {
              clipboard.shareText(address)
            } else {
              onShareAddress(address)
            }
          },
        )

        val requiresConfirmation =
          userAddress.addressSettings.autoAccept == null
        NomeAddressToggleRow(
          checked = requiresConfirmation,
          enabled =
            !busy &&
              !userAddress.addressSettings.businessAddress,
          onCheckedChange = { required ->
            runBooleanAction(
              NomeAddressAction.CONFIRMATION,
            ) {
              onSetRequiresConfirmation(required)
            }
          },
        )

        Text(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_management,
            ),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
        NomeAddressManagementRow(
          icon = Icons.Rounded.Sync,
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_replace,
            ),
          body =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_replace_body,
            ),
          tint = NomeTheme.colors.accent,
          enabled = !busy,
          onClick = {
            confirmation =
              NomeAddressConfirmation.REPLACE
          },
        )
        NomeAddressManagementRow(
          icon = Icons.Rounded.DeleteOutline,
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_disable,
            ),
          body =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_disable_body,
            ),
          tint = NomeTheme.colors.danger,
          enabled = !busy,
          onClick = {
            confirmation =
              NomeAddressConfirmation.DELETE
          },
        )
        NomeAddressManagementRow(
          icon = Icons.Rounded.Settings,
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_advanced,
            ),
          body =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_advanced_body,
            ),
          tint = NomeTheme.colors.textSecondary,
          enabled = !busy,
          onClick = onOpenAdvanced,
        )
      }
      oldAddressDeleted -> {
        NomeAddressRecoveryCard(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_replace_deleted,
            ),
          actionText =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_replace_retry,
            ),
          enabled = !busy,
          onClick = {
            runBooleanAction(
              NomeAddressAction.REPLACE_CREATE,
            ) {
              if (onCreate()) {
                oldAddressDeleted = false
                true
              } else {
                false
              }
            }
          },
        )
      }
      loadState == NomeUserAddressLoadState.LOADING -> {
        NomeAddressLoading()
      }
      loadState == NomeUserAddressLoadState.OFF -> {
        NomeAddressRecoveryCard(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_off,
            ),
          actionText =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_create,
            ),
          enabled = !busy,
          onClick = {
            runBooleanAction(NomeAddressAction.CREATE) {
              onCreate()
            }
          },
        )
      }
      else -> {
        NomeAddressRecoveryCard(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_unknown,
            ),
          actionText =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_retry,
            ),
          enabled = !busy,
          onClick = {
            reloadAddress()
          },
        )
      }
    }

    NomeAddressStrip(
      icon = Icons.Rounded.Info,
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p15_scope_note,
        ),
      container = NomeTheme.colors.infoContainer,
      content = NomeTheme.colors.onInfoContainer,
    )

    if (
      loadState == NomeUserAddressLoadState.FAILURE &&
        userAddress != null &&
        !busy
    ) {
      NomeButton(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p15_retry,
          ),
        onClick = ::reloadAddress,
        modifier = Modifier.fillMaxWidth(),
        variant = NomeButtonVariant.SECONDARY,
        shape = NomeTheme.shapes.pill,
      )
    }

  }

  when (confirmation) {
    NomeAddressConfirmation.NONE -> Unit
    NomeAddressConfirmation.DELETE -> {
      NomeAddressConfirmationDialog(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p15_disable_title,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p15_disable_warning,
          ),
        confirm =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p15_disable,
          ),
        destructive = true,
        onConfirm = {
          confirmation = NomeAddressConfirmation.NONE
          runBooleanAction(NomeAddressAction.DELETE) {
            onDelete()
          }
        },
        onDismiss = {
          confirmation = NomeAddressConfirmation.NONE
        },
      )
    }
    NomeAddressConfirmation.REPLACE -> {
      NomeAddressConfirmationDialog(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p15_replace_title,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p15_replace_warning,
          ),
        confirm =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p15_replace_confirm,
          ),
        destructive = true,
        onConfirm = ::replaceAddress,
        onDismiss = {
          confirmation = NomeAddressConfirmation.NONE
        },
      )
    }
  }

  BackHandler(enabled = busy) {}
}

@Composable
private fun NomeReadyAddressCard(
  userAddress: UserContactLinkRec,
  busy: Boolean,
  shouldBeUpgraded: Boolean,
  onAddShortLink: (() -> Unit)?,
  onCopy: () -> Unit,
  onShare: () -> Unit,
) {
  val link =
    userAddress.connLinkContact.simplexChatUri(
      short = true,
    )
  val addressLabel =
    androidx.compose.ui.res.stringResource(
      R.string.nome_p15_address_label,
    )
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.border,
      ),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.size(118.dp),
          contentAlignment = Alignment.Center,
        ) {
          SimpleXCreatedLinkQRCode(
            connLink = userAddress.connLinkContact,
            short = true,
            modifier = Modifier.size(112.dp),
            padding = PaddingValues(0.dp),
            tintColor = NomeTheme.colors.textPrimary,
          )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
          Text(
            text = addressLabel,
            style = NomeTheme.typography.bodyStrong,
            color = NomeTheme.colors.textPrimary,
          )
          Text(
            text = link,
            modifier =
              Modifier.clearAndSetSemantics {
                contentDescription = addressLabel
              },
            style =
              NomeTheme.typography.supporting.copy(
                fontFamily = FontFamily.Monospace,
              ),
            color = NomeTheme.colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p15_scan_or_copy,
              ),
            style = NomeTheme.typography.supporting,
            color = NomeTheme.colors.textSecondary,
          )
        }
      }
      Row(
        horizontalArrangement =
          Arrangement.spacedBy(8.dp),
      ) {
        NomeButton(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_copy,
            ),
          onClick = onCopy,
          modifier = Modifier.weight(1f),
          variant = NomeButtonVariant.SECONDARY,
          enabled = !busy,
          shape = NomeTheme.shapes.pill,
          leadingIcon = {
            Icon(
              Icons.Rounded.ContentCopy,
              contentDescription = null,
            )
          },
        )
        NomeButton(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_share,
            ),
          onClick = onShare,
          modifier = Modifier.weight(1f),
          enabled = !busy,
          shape = NomeTheme.shapes.pill,
          leadingIcon = {
            Icon(
              Icons.Rounded.Share,
              contentDescription = null,
            )
          },
        )
      }
      if (shouldBeUpgraded && onAddShortLink != null) {
        NomeButton(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_add_short_link,
            ),
          onClick = onAddShortLink,
          modifier = Modifier.fillMaxWidth(),
          variant = NomeButtonVariant.SECONDARY,
          enabled = !busy,
          shape = NomeTheme.shapes.pill,
        )
      }
    }
  }
}

@Composable
private fun NomeAddressToggleRow(
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .clickable(enabled = enabled) {
          onCheckedChange(!checked)
        }
        .nomeMinimumTouchTarget()
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NomeAddressIconTile(
      icon = Icons.Rounded.Security,
      background = NomeTheme.colors.successContainer,
      tint = NomeTheme.colors.success,
    )
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p15_confirmation,
          ),
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p15_confirmation_body,
          ),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      enabled = enabled,
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = NomeTheme.colors.onAction,
          checkedTrackColor = NomeTheme.colors.action,
          uncheckedThumbColor = NomeTheme.colors.surface,
          uncheckedTrackColor =
            NomeTheme.colors.disabledContent,
        ),
    )
  }
}

@Composable
private fun NomeAddressManagementRow(
  icon: ImageVector,
  title: String,
  body: String,
  tint: Color,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .clickable(
          enabled = enabled,
          onClick = onClick,
        )
        .nomeMinimumTouchTarget()
        .nomeTalkBackSemantics(
          label = "$title. $body",
          role = Role.Button,
          enabled = enabled,
        )
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NomeAddressIconTile(
      icon = icon,
      background =
        if (tint == NomeTheme.colors.danger) {
          NomeTheme.colors.dangerContainer
        } else {
          NomeTheme.colors.infoContainer
        },
      tint = tint,
    )
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(
        text = title,
        style = NomeTheme.typography.bodyStrong,
        color =
          if (tint == NomeTheme.colors.danger) {
            NomeTheme.colors.danger
          } else {
            NomeTheme.colors.textPrimary
          },
      )
      Text(
        text = body,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
    Icon(
      imageVector =
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint = NomeTheme.colors.textTertiary,
    )
  }
}

@Composable
private fun NomeAddressRecoveryCard(
  text: String,
  actionText: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.border,
      ),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = text,
        style = NomeTheme.typography.body,
        color = NomeTheme.colors.textSecondary,
      )
      NomeButton(
        text = actionText,
        onClick = onClick,
        enabled = enabled,
        shape = NomeTheme.shapes.pill,
      )
    }
  }
}

@Composable
private fun NomeAddressLoading() {
  NomeSurface(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 190.dp),
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.border,
      ),
  ) {
    Box(
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(
        color = NomeTheme.colors.action,
      )
    }
  }
}

@Composable
private fun NomeAddressStrip(
  icon: ImageVector,
  text: String,
  container: Color,
  content: Color,
  live: Boolean = false,
) {
  NomeSurface(
    modifier =
      Modifier
        .fillMaxWidth()
        .then(
          if (live) {
            Modifier.semantics {
              liveRegion = LiveRegionMode.Polite
            }
          } else {
            Modifier
          },
        ),
    shape = NomeTheme.shapes.compact,
    color = container,
  ) {
    Row(
      modifier =
        Modifier.padding(
          horizontal = 12.dp,
          vertical = 10.dp,
        ),
      horizontalArrangement = Arrangement.spacedBy(9.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = content,
      )
      Text(
        text = text,
        style = NomeTheme.typography.supporting,
        color = content,
      )
    }
  }
}

@Composable
private fun NomeAddressIconTile(
  icon: ImageVector,
  background: Color,
  tint: Color,
) {
  NomeSurface(
    modifier = Modifier.size(40.dp),
    shape = NomeTheme.shapes.compact,
    color = background,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = tint,
      )
    }
  }
}

@Composable
private fun NomeAddressConfirmationDialog(
  title: String,
  body: String,
  confirm: String,
  destructive: Boolean,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = title,
        style = NomeTheme.typography.title,
      )
    },
    text = {
      Text(
        text = body,
        style = NomeTheme.typography.body,
      )
    },
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(
          text = confirm,
          color =
            if (destructive) {
              NomeTheme.colors.danger
            } else {
              NomeTheme.colors.action
            },
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p15_cancel,
            ),
          color = NomeTheme.colors.textSecondary,
        )
      }
    },
    backgroundColor = NomeTheme.colors.surfaceRaised,
    contentColor = NomeTheme.colors.textPrimary,
    shape = NomeTheme.shapes.large,
  )
}
