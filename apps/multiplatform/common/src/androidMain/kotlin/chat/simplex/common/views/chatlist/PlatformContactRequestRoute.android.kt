package chat.simplex.common.views.chatlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.User
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.IncognitoImage
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.helpers.ProfileImage
import kotlinx.coroutines.launch

private enum class NomeContactRequestAction {
  NONE,
  ACCEPT_CURRENT,
  ACCEPT_INCOGNITO,
  REJECT,
}

internal actual fun showPlatformContactRequestRoute(
  contactRequest: ChatInfo.ContactRequest,
  currentUser: User?,
  canAcceptIncognito: Boolean,
  onAccept: suspend (incognito: Boolean) -> Boolean,
  onReject: suspend () -> Boolean,
): Boolean {
  ModalManager.start.showCustomModal(
    keyboardCoversBar = false,
  ) { close ->
    NomeContactRequestRoute(
      contactRequest = contactRequest,
      currentUser = currentUser,
      canAcceptIncognito = canAcceptIncognito,
      onAccept = onAccept,
      onReject = onReject,
      onClose = close,
    )
  }
  return true
}

@Composable
private fun NomeContactRequestRoute(
  contactRequest: ChatInfo.ContactRequest,
  currentUser: User?,
  canAcceptIncognito: Boolean,
  onAccept: suspend (incognito: Boolean) -> Boolean,
  onReject: suspend () -> Boolean,
  onClose: () -> Unit,
) {
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeContactRequestContent(
      requestName = contactRequest.displayName,
      requestFullName = contactRequest.fullName,
      requestImage = contactRequest.image,
      currentProfileName =
        currentUser?.displayName.orEmpty(),
      currentProfileImage = currentUser?.image,
      canAcceptIncognito = canAcceptIncognito,
      onAccept = onAccept,
      onReject = onReject,
      onClose = onClose,
    )
  }
}

@Composable
fun NomeContactRequestContent(
  requestName: String,
  requestFullName: String,
  requestImage: String?,
  currentProfileName: String,
  currentProfileImage: String?,
  canAcceptIncognito: Boolean,
  onAccept: suspend (incognito: Boolean) -> Boolean,
  onReject: suspend () -> Boolean,
  onClose: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  var action by rememberSaveable {
    mutableStateOf(NomeContactRequestAction.NONE)
  }
  var failed by rememberSaveable {
    mutableStateOf(false)
  }
  val busy = action != NomeContactRequestAction.NONE

  fun runAction(
    next: NomeContactRequestAction,
    block: suspend () -> Boolean,
  ) {
    if (action != NomeContactRequestAction.NONE) return
    action = next
    failed = false
    scope.launch {
      val succeeded = block()
      if (succeeded) {
        onClose()
      } else {
        action = NomeContactRequestAction.NONE
        failed = true
      }
    }
  }

  NomeFullPageScaffold(
    title = androidx.compose.ui.res.stringResource(
      R.string.nome_p14_title,
    ),
    backLabel = androidx.compose.ui.res.stringResource(
      R.string.nome_p14_back,
    ),
    onClose = if (busy) ({}) else onClose,
  ) {
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.border,
        ),
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ProfileImage(
          size = 72.dp,
          image = requestImage,
          color = NomeTheme.colors.textTertiary,
        )
        Spacer(Modifier.width(14.dp))
        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = requestName,
            style = NomeTheme.typography.title,
            color = NomeTheme.colors.textPrimary,
          )
          if (
            requestFullName.isNotBlank() &&
              requestFullName != requestName
          ) {
            Text(
              text = requestFullName,
              style = NomeTheme.typography.supporting,
              color = NomeTheme.colors.textSecondary,
            )
          }
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p14_wants_contact,
                requestName,
              ),
            style = NomeTheme.typography.supporting,
            color = NomeTheme.colors.textSecondary,
          )
        }
      }
    }

    NomeContactRequestStrip(
      icon = Icons.Rounded.ChatBubbleOutline,
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p14_no_message,
        ),
      container = NomeTheme.colors.surfaceSubtle,
      content = NomeTheme.colors.textSecondary,
    )
    NomeContactRequestStrip(
      icon = Icons.Rounded.Security,
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p14_identity_note,
        ),
      container = NomeTheme.colors.successContainer,
      content = NomeTheme.colors.onSuccessContainer,
    )

    Text(
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p14_accept_method,
        ),
      modifier = Modifier.padding(top = 2.dp),
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textSecondary,
    )

    NomeContactRequestChoice(
      title =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p14_accept_current,
        ),
      body =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p14_accept_current_body,
          currentProfileName,
        ),
      enabled = !busy && currentProfileName.isNotBlank(),
      loading =
        action ==
          NomeContactRequestAction.ACCEPT_CURRENT,
      image = {
        ProfileImage(
          size = 46.dp,
          image = currentProfileImage,
          color = NomeTheme.colors.textTertiary,
        )
      },
      onClick = {
        runAction(
          NomeContactRequestAction.ACCEPT_CURRENT,
        ) {
          onAccept(false)
        }
      },
    )
    Divider(color = NomeTheme.colors.divider)
    if (canAcceptIncognito) {
      NomeContactRequestChoice(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p14_accept_incognito,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p14_accept_incognito_body,
          ),
        enabled = !busy,
        loading =
          action ==
            NomeContactRequestAction.ACCEPT_INCOGNITO,
        image = {
          IncognitoImage(
            size = 46.dp,
            iconColor = NomeTheme.colors.textSecondary,
          )
        },
        onClick = {
          runAction(
            NomeContactRequestAction.ACCEPT_INCOGNITO,
          ) {
            onAccept(true)
          }
        },
      )
      Divider(color = NomeTheme.colors.divider)
    }

    Text(
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p14_reject_note,
        ),
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp),
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textSecondary,
    )
    NomeButton(
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p14_reject,
        ),
      onClick = {
        runAction(NomeContactRequestAction.REJECT) {
          onReject()
        }
      },
      modifier = Modifier.fillMaxWidth(),
      variant =
        NomeButtonVariant.DESTRUCTIVE_SECONDARY,
      enabled = !busy,
      shape = NomeTheme.shapes.pill,
      leadingIcon =
        if (
          action == NomeContactRequestAction.REJECT
        ) {
          {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = NomeTheme.colors.onDangerContainer,
              strokeWidth = 2.dp,
            )
          }
        } else {
          null
        },
    )

    if (busy || failed) {
      NomeContactRequestStrip(
        icon =
          if (failed) {
            Icons.Rounded.ChatBubbleOutline
          } else {
            Icons.Rounded.Security
          },
        text =
          androidx.compose.ui.res.stringResource(
            if (failed) {
              R.string.nome_p14_action_failed
            } else {
              R.string.nome_p14_action_in_progress
            },
          ),
        container =
          if (failed) {
            NomeTheme.colors.dangerContainer
          } else {
            NomeTheme.colors.infoContainer
          },
        content =
          if (failed) {
            NomeTheme.colors.onDangerContainer
          } else {
            NomeTheme.colors.onInfoContainer
          },
        live = true,
      )
    }
  }

  BackHandler(enabled = busy) {}
}

@Composable
private fun NomeContactRequestChoice(
  title: String,
  body: String,
  enabled: Boolean,
  loading: Boolean,
  image: @Composable () -> Unit,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 66.dp)
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
    image()
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(
        text = title,
        style = NomeTheme.typography.bodyStrong,
        color =
          if (enabled) {
            NomeTheme.colors.textPrimary
          } else {
            NomeTheme.colors.disabledContent
          },
      )
      Text(
        text = body,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
    if (loading) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = NomeTheme.colors.action,
        strokeWidth = 2.dp,
      )
    } else {
      Icon(
        imageVector =
          Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = NomeTheme.colors.textTertiary,
      )
    }
  }
}

@Composable
private fun NomeContactRequestStrip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
  container: androidx.compose.ui.graphics.Color,
  content: androidx.compose.ui.graphics.Color,
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
