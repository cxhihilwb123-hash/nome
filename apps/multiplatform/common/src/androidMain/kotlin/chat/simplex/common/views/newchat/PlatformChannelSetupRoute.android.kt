package chat.simplex.common.views.newchat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.common.views.isValidDisplayName
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
internal actual fun PlatformChannelSetupRoute(
  displayName: MutableState<String>,
  profileImage: String?,
  focusRequester: FocusRequester,
  enabledRelayCount: Int,
  hasRelays: Boolean,
  creationInProgress: Boolean,
  canCreate: Boolean,
  currentProfileName: String,
  onEditImage: () -> Unit,
  onDeleteImage: () -> Unit,
  onShowInvalidName: () -> Unit,
  onConfigureRelays: () -> Unit,
  onCreateChannel: () -> Unit,
  onOpenJoinChannel: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeChannelSetupContent(
      displayName = displayName,
      profileImage = profileImage,
      focusRequester = focusRequester,
      enabledRelayCount = enabledRelayCount,
      hasRelays = hasRelays,
      creationInProgress = creationInProgress,
      canCreate = canCreate,
      currentProfileName = currentProfileName,
      onEditImage = onEditImage,
      onDeleteImage = onDeleteImage,
      onShowInvalidName = onShowInvalidName,
      onConfigureRelays = onConfigureRelays,
      onCreateChannel = onCreateChannel,
      onOpenJoinChannel = onOpenJoinChannel,
      onClose = onClose,
    )
  }
}

@Composable
fun NomeChannelSetupContent(
  displayName: MutableState<String>,
  profileImage: String?,
  focusRequester: FocusRequester,
  enabledRelayCount: Int,
  hasRelays: Boolean,
  creationInProgress: Boolean,
  canCreate: Boolean,
  currentProfileName: String,
  onEditImage: () -> Unit,
  onDeleteImage: () -> Unit,
  onShowInvalidName: () -> Unit,
  onConfigureRelays: () -> Unit,
  onCreateChannel: () -> Unit,
  onOpenJoinChannel: () -> Unit,
  onClose: () -> Unit,
) {
  val backLabel =
    androidx.compose.ui.res.stringResource(
      R.string.nome_p20_back,
    )
  BackHandler(
    enabled = !creationInProgress,
    onBack = onClose,
  )
  NomeFullPageScaffold(
    title =
      androidx.compose.ui.res.stringResource(
        R.string.nome_p20_title,
      ),
    backLabel = backLabel,
    onClose =
      if (creationInProgress) {
        {}
      } else {
        onClose
      },
  ) {
    NomeChannelModeTabs(
      onOpenJoinChannel = onOpenJoinChannel,
    )

    NomeChannelNameField(
      displayName = displayName,
      profileImage = profileImage,
      focusRequester = focusRequester,
      onEditImage = onEditImage,
      onDeleteImage = onDeleteImage,
      onShowInvalidName = onShowInvalidName,
    )

    NomeChannelLinkPlaceholder()

    Text(
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p20_relays_label,
        ),
      modifier = Modifier.semantics { heading() },
      style = NomeTheme.typography.label,
      color = NomeTheme.colors.textSecondary,
    )

    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.divider,
        ),
    ) {
      Column {
        NomeRelaySummaryRow(
          enabledRelayCount = enabledRelayCount,
          hasRelays = hasRelays,
        )
        Box(
          Modifier
            .fillMaxWidth()
            .padding(start = 62.dp),
        ) {
          Spacer(
            Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(
                NomeTheme.colors.divider,
              ),
          )
        }
        NomeRelayConfigurationRow(
          onConfigureRelays = onConfigureRelays,
        )
      }
    }

    NomeChannelWarning()

    if (currentProfileName.isNotBlank()) {
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p20_profile_shared,
            currentProfileName,
          ),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }

    NomeButton(
      text =
        androidx.compose.ui.res.stringResource(
          if (creationInProgress) {
            R.string.nome_p20_creating
          } else {
            R.string.nome_p20_create
          },
        ),
      onClick = onCreateChannel,
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp),
      enabled = canCreate,
      shape = NomeTheme.shapes.pill,
      semanticsLabel =
        androidx.compose.ui.res.stringResource(
          if (creationInProgress) {
            R.string.nome_p20_creating
          } else {
            R.string.nome_p20_create
          },
        ),
      leadingIcon = {
        Icon(
          painter =
            painterResource(
              MR.images.ic_bigtop_updates,
            ),
          contentDescription = null,
          modifier =
            Modifier.size(
              NomeTheme.dimensions.icon,
            ),
        )
      },
    )

    if (!hasRelays) {
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p20_no_relays,
          ),
        modifier =
          Modifier
            .fillMaxWidth()
            .nomeTalkBackSemantics(
              label =
                androidx.compose.ui.res.stringResource(
                  R.string.nome_p20_no_relays,
                ),
            ),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.warning,
      )
    }
  }
}

@Composable
private fun NomeChannelModeTabs(
  onOpenJoinChannel: () -> Unit,
) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.pill,
    color = NomeTheme.colors.surfaceSubtle,
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .padding(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NomeSurface(
        modifier =
          Modifier
            .weight(1f)
            .heightIn(min = 48.dp),
        shape = NomeTheme.shapes.pill,
        color = NomeTheme.colors.surface,
        elevation = NomeTheme.elevation.low,
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .semantics { heading() },
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p20_create_tab,
              ),
            style = NomeTheme.typography.label,
            color = NomeTheme.colors.textPrimary,
          )
        }
      }
      Box(
        modifier =
          Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clickable(
              role = Role.Button,
              onClick = onOpenJoinChannel,
            )
            .nomeTalkBackSemantics(
              label =
                androidx.compose.ui.res.stringResource(
                  R.string.nome_p20_join_tab,
                ),
              role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p20_join_tab,
            ),
          style = NomeTheme.typography.label,
          color = NomeTheme.colors.textSecondary,
        )
      }
    }
  }
}

@Composable
private fun NomeChannelNameField(
  displayName: MutableState<String>,
  profileImage: String?,
  focusRequester: FocusRequester,
  onEditImage: () -> Unit,
  onDeleteImage: () -> Unit,
  onShowInvalidName: () -> Unit,
) {
  val valid =
    displayName.value.isBlank() ||
      isValidDisplayName(
        displayName.value.trim(),
      )
  Column(
    verticalArrangement =
      Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p20_name_label,
        ),
      style = NomeTheme.typography.label,
      color = NomeTheme.colors.textSecondary,
    )
    OutlinedTextField(
      value = displayName.value,
      onValueChange = {
        displayName.value = it
      },
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 56.dp)
          .focusRequester(focusRequester),
      textStyle = NomeTheme.typography.body,
      singleLine = true,
      isError = !valid,
      leadingIcon = {
        IconButton(
          onClick = onEditImage,
          modifier =
            Modifier
              .nomeMinimumTouchTarget()
              .nomeTalkBackSemantics(
                label =
                  androidx.compose.ui.res.stringResource(
                    R.string.nome_p20_edit_image,
                  ),
                role = Role.Button,
              ),
        ) {
          if (profileImage == null) {
            Icon(
              painter =
                painterResource(
                  MR.images.ic_bigtop_updates,
                ),
              contentDescription = null,
              tint = NomeTheme.colors.textSecondary,
            )
          } else {
            ProfileImage(
              size = 34.dp,
              image = profileImage,
            )
          }
        }
      },
      trailingIcon = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (!valid) {
            IconButton(
              onClick = onShowInvalidName,
              modifier =
                Modifier.nomeMinimumTouchTarget(),
            ) {
              Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription =
                  androidx.compose.ui.res.stringResource(
                    R.string.nome_p20_invalid_name,
                  ),
                tint = NomeTheme.colors.danger,
              )
            }
          }
          if (profileImage != null) {
            IconButton(
              onClick = onDeleteImage,
              modifier =
                Modifier.nomeMinimumTouchTarget(),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription =
                  androidx.compose.ui.res.stringResource(
                    R.string.nome_p20_delete_image,
                  ),
                tint = NomeTheme.colors.textSecondary,
              )
            }
          }
        }
      },
      shape = NomeTheme.shapes.control,
      colors =
        TextFieldDefaults.outlinedTextFieldColors(
          textColor = NomeTheme.colors.textPrimary,
          backgroundColor = NomeTheme.colors.input,
          focusedBorderColor = NomeTheme.colors.action,
          unfocusedBorderColor = NomeTheme.colors.border,
          errorBorderColor = NomeTheme.colors.danger,
          cursorColor = NomeTheme.colors.action,
        ),
    )
  }
}

@Composable
private fun NomeChannelLinkPlaceholder() {
  Column(
    verticalArrangement =
      Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p20_link_label,
        ),
      style = NomeTheme.typography.label,
      color = NomeTheme.colors.textSecondary,
    )
    NomeSurface(
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 56.dp),
      shape = NomeTheme.shapes.control,
      color = NomeTheme.colors.input,
      border =
        BorderStroke(
          1.dp,
          NomeTheme.colors.border,
        ),
    ) {
      Row(
        modifier =
          Modifier.padding(
            horizontal = 14.dp,
            vertical = 12.dp,
          ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
          Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Link,
          contentDescription = null,
          tint = NomeTheme.colors.textSecondary,
        )
        Text(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p20_link_after_creation,
            ),
          modifier = Modifier.weight(1f),
          style = NomeTheme.typography.body,
          color = NomeTheme.colors.textSecondary,
        )
      }
    }
  }
}

@Composable
private fun NomeRelaySummaryRow(
  enabledRelayCount: Int,
  hasRelays: Boolean,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .padding(
          horizontal = 14.dp,
          vertical = 8.dp,
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement =
      Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(40.dp)
          .background(
            if (hasRelays) {
              NomeTheme.colors.successContainer
            } else {
              NomeTheme.colors.warningContainer
            },
            CircleShape,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector =
          if (hasRelays) {
            Icons.Rounded.CheckCircle
          } else {
            Icons.Rounded.WarningAmber
          },
        contentDescription = null,
        tint =
          if (hasRelays) {
            NomeTheme.colors.success
          } else {
            NomeTheme.colors.warning
          },
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement =
        Arrangement.spacedBy(1.dp),
    ) {
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p20_enabled_relays,
            enabledRelayCount,
          ),
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p20_relay_selection_note,
          ),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
  }
}

@Composable
private fun NomeRelayConfigurationRow(
  onConfigureRelays: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .clickable(
          role = Role.Button,
          onClick = onConfigureRelays,
        )
        .nomeTalkBackSemantics(
          label =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p20_configure_relays,
            ),
          role = Role.Button,
        )
        .padding(
          horizontal = 14.dp,
          vertical = 8.dp,
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement =
      Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(40.dp)
          .background(
            NomeTheme.colors.infoContainer,
            CircleShape,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Settings,
        contentDescription = null,
        tint = NomeTheme.colors.info,
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement =
        Arrangement.spacedBy(1.dp),
    ) {
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p20_configure_relays,
          ),
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p20_configure_relays_body,
          ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
private fun NomeChannelWarning() {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.control,
    color = NomeTheme.colors.warningContainer,
    contentColor = NomeTheme.colors.onWarningContainer,
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.warning.copy(
          alpha = 0.18f,
        ),
      ),
  ) {
    Row(
      modifier =
        Modifier.padding(
          horizontal = 12.dp,
          vertical = 9.dp,
        ),
      verticalAlignment = Alignment.Top,
      horizontalArrangement =
        Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.WarningAmber,
        contentDescription = null,
        modifier =
          Modifier.size(
            NomeTheme.dimensions.iconSmall,
          ),
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p20_non_e2ee,
          ),
        style = NomeTheme.typography.supporting,
      )
    }
  }
}
