package chat.simplex.common.views.chatlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.model.GroupInfo
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.helpers.ProfileImage
import kotlinx.coroutines.launch

private enum class NomeGroupInvitationAction {
  NONE,
  JOIN,
  DELETE,
}

internal actual fun showPlatformGroupInvitationRoute(
  groupInfo: GroupInfo,
  inviterName: String?,
  inviterVerified: Boolean,
  onJoin: suspend () -> Boolean,
  onDelete: suspend () -> Boolean,
): Boolean {
  ModalManager.start.showCustomModal(
    keyboardCoversBar = false,
  ) { close ->
    val darkTheme =
      !CurrentColors.collectAsState().value.colors.isLight
    NomeAndroidTheme(darkTheme = darkTheme) {
      NomeGroupInvitationContent(
        groupName = groupInfo.displayName,
        groupFullName = groupInfo.fullName,
        groupDescription =
          groupInfo.groupProfile.description
            ?.takeIf { it.isNotBlank() }
            ?: groupInfo.shortDescr,
        groupImage = groupInfo.image,
        currentMembers =
          groupInfo.groupSummary.currentMembers,
        isPublic =
          groupInfo.groupProfile.publicGroup != null,
        isChannel = groupInfo.isChannel,
        requiresReview =
          groupInfo.groupProfile.memberAdmission?.review !=
            null,
        joinsIncognito =
          groupInfo.membership.memberIncognito,
        inviterName = inviterName,
        inviterVerified = inviterVerified,
        onJoin = onJoin,
        onDelete = onDelete,
        onClose = close,
      )
    }
  }
  return true
}

@Composable
fun NomeGroupInvitationContent(
  groupName: String,
  groupFullName: String,
  groupDescription: String?,
  groupImage: String?,
  currentMembers: Long,
  isPublic: Boolean,
  isChannel: Boolean,
  requiresReview: Boolean,
  joinsIncognito: Boolean,
  inviterName: String?,
  inviterVerified: Boolean,
  onJoin: suspend () -> Boolean,
  onDelete: suspend () -> Boolean,
  onClose: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  var action by rememberSaveable {
    mutableStateOf(NomeGroupInvitationAction.NONE)
  }
  var deleteConfirmation by rememberSaveable {
    mutableStateOf(false)
  }
  var failed by rememberSaveable {
    mutableStateOf(false)
  }
  val busy = action != NomeGroupInvitationAction.NONE

  fun runAction(
    next: NomeGroupInvitationAction,
    block: suspend () -> Boolean,
  ) {
    if (action != NomeGroupInvitationAction.NONE) return
    action = next
    failed = false
    scope.launch {
      if (block()) {
        onClose()
      } else {
        action = NomeGroupInvitationAction.NONE
        failed = true
      }
    }
  }

  NomeFullPageScaffold(
    title =
      androidx.compose.ui.res.stringResource(
        R.string.nome_p16_title,
      ),
    backLabel =
      androidx.compose.ui.res.stringResource(
        R.string.nome_p16_back,
      ),
    onClose = if (busy) ({}) else onClose,
  ) {
    NomeGroupPreviewStrip(
      icon = Icons.Rounded.Warning,
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p16_source_warning,
        ),
      container = NomeTheme.colors.warningContainer,
      content = NomeTheme.colors.onWarningContainer,
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          NomeGroupPreviewAvatar(
            image = groupImage,
            isChannel = isChannel,
          )
          Spacer(Modifier.width(14.dp))
          Column(Modifier.weight(1f)) {
            Text(
              text =
                androidx.compose.ui.res.stringResource(
                  if (isChannel) {
                    R.string.nome_p16_channel
                  } else if (isPublic) {
                    R.string.nome_p16_public_group
                  } else {
                    R.string.nome_p16_private_group
                  },
                ),
              style = NomeTheme.typography.label,
              color = NomeTheme.colors.success,
            )
            Text(
              text = groupName,
              style = NomeTheme.typography.title,
              color = NomeTheme.colors.textPrimary,
            )
            val description =
              groupDescription
                ?.takeIf { it.isNotBlank() }
                ?: groupFullName.takeIf {
                  it.isNotBlank() &&
                    it != groupName
                }
            if (description != null) {
              Text(
                text = description,
                style = NomeTheme.typography.supporting,
                color = NomeTheme.colors.textSecondary,
              )
            }
          }
        }

        Row(
          horizontalArrangement =
            Arrangement.spacedBy(8.dp),
        ) {
          NomeGroupPreviewChip(
            icon = Icons.Rounded.Groups,
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p16_member_count,
                currentMembers,
              ),
            modifier = Modifier.weight(1f),
          )
          NomeGroupPreviewChip(
            icon = Icons.Rounded.Security,
            text =
              androidx.compose.ui.res.stringResource(
                if (requiresReview) {
                  R.string.nome_p16_review_required
                } else {
                  R.string.nome_p16_direct_invitation
                },
              ),
            modifier = Modifier.weight(1f),
          )
        }

        NomeGroupPreviewFactRow(
          icon = Icons.Rounded.Link,
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p16_invitation_source,
            ),
          body =
            androidx.compose.ui.res.stringResource(
              if (inviterName == null) {
                R.string.nome_p16_source_unavailable
              } else {
                R.string.nome_p16_existing_contact
              },
            ),
        )
        NomeGroupPreviewFactRow(
          icon =
            if (inviterVerified) {
              Icons.Rounded.VerifiedUser
            } else {
              Icons.Rounded.PersonOutline
            },
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p16_inviter,
            ),
          body =
            when {
              inviterName == null ->
                androidx.compose.ui.res.stringResource(
                  R.string.nome_p16_inviter_unavailable,
                )
              inviterVerified ->
                androidx.compose.ui.res.stringResource(
                  R.string.nome_p16_verified_contact,
                  inviterName,
                )
              else -> inviterName
            },
        )

        NomeButton(
          text =
            androidx.compose.ui.res.stringResource(
              if (joinsIncognito) {
                R.string.nome_p16_join_incognito
              } else {
                R.string.nome_p16_join
              },
            ),
          onClick = {
            runAction(NomeGroupInvitationAction.JOIN) {
              onJoin()
            }
          },
          modifier = Modifier.fillMaxWidth(),
          enabled = !busy,
          shape = NomeTheme.shapes.pill,
          leadingIcon =
            if (action == NomeGroupInvitationAction.JOIN) {
              {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  color = NomeTheme.colors.onAction,
                  strokeWidth = 2.dp,
                )
              }
            } else {
              null
            },
        )
        NomeButton(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p16_delete_invitation,
            ),
          onClick = {
            deleteConfirmation = true
          },
          modifier = Modifier.fillMaxWidth(),
          variant =
            NomeButtonVariant.DESTRUCTIVE_SECONDARY,
          enabled = !busy,
          shape = NomeTheme.shapes.pill,
        )
      }
    }

    NomeGroupPreviewStrip(
      icon = Icons.Rounded.Security,
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p16_connection_note,
        ),
      container = NomeTheme.colors.successContainer,
      content = NomeTheme.colors.onSuccessContainer,
    )

    if (busy || failed) {
      NomeGroupPreviewStrip(
        icon = Icons.Rounded.Warning,
        text =
          androidx.compose.ui.res.stringResource(
            if (failed) {
              R.string.nome_p16_action_not_completed
            } else {
              R.string.nome_p16_action_in_progress
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

  if (deleteConfirmation) {
    AlertDialog(
      onDismissRequest = {
        if (!busy) deleteConfirmation = false
      },
      title = {
        Text(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p16_delete_title,
            ),
        )
      },
      text = {
        Text(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p16_delete_warning,
            ),
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            deleteConfirmation = false
            runAction(NomeGroupInvitationAction.DELETE) {
              onDelete()
            }
          },
        ) {
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p16_delete_confirm,
              ),
            color = NomeTheme.colors.danger,
          )
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            deleteConfirmation = false
          },
        ) {
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p16_keep_invitation,
              ),
          )
        }
      },
    )
  }

  BackHandler(enabled = busy) {}
}

@Composable
private fun NomeGroupPreviewAvatar(
  image: String?,
  isChannel: Boolean,
) {
  if (image != null) {
    ProfileImage(
      size = 72.dp,
      image = image,
    )
    return
  }
  NomeSurface(
    modifier = Modifier.size(72.dp),
    shape = CircleShape,
    color = NomeTheme.colors.success,
    contentColor = NomeTheme.colors.onAction,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector =
          if (isChannel) {
            Icons.Rounded.Campaign
          } else {
            Icons.Rounded.Groups
          },
        contentDescription = null,
        modifier = Modifier.size(38.dp),
        tint = NomeTheme.colors.onAction,
      )
    }
  }
}

@Composable
private fun NomeGroupPreviewChip(
  icon: ImageVector,
  text: String,
  modifier: Modifier = Modifier,
) {
  NomeSurface(
    modifier = modifier.heightIn(min = 48.dp),
    color = NomeTheme.colors.surfaceSubtle,
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = NomeTheme.colors.success,
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = text,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
  }
}

@Composable
private fun NomeGroupPreviewFactRow(
  icon: ImageVector,
  title: String,
  body: String,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 62.dp)
        .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NomeSurface(
      modifier = Modifier.size(42.dp),
      color = NomeTheme.colors.successContainer,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = NomeTheme.colors.success,
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(
        text = title,
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text = body,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
  }
}

@Composable
private fun NomeGroupPreviewStrip(
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
        Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = content,
      )
      Spacer(Modifier.width(10.dp))
      Text(
        text = text,
        modifier = Modifier.weight(1f),
        style = NomeTheme.typography.supporting,
        color = content,
      )
    }
  }
}
