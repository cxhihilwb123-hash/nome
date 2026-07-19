package chat.simplex.common.views.newchat

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
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
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal actual fun PlatformAddGroupRoute(
  displayName: MutableState<String>,
  profileImage: String?,
  focusRequester: FocusRequester,
  incognito: MutableState<Boolean>,
  canCreate: Boolean,
  profileDisclosure: String,
  onEditImage: () -> Unit,
  onDeleteImage: () -> Unit,
  onShowInvalidName: () -> Unit,
  onShowIncognitoInfo: () -> Unit,
  onIncognitoChange: (Boolean) -> Unit,
  onCreateGroup: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme = !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeAddGroupContent(
      displayName = displayName,
      profileImage = profileImage,
      focusRequester = focusRequester,
      incognito = incognito,
      canCreate = canCreate,
      profileDisclosure = profileDisclosure,
      onEditImage = onEditImage,
      onDeleteImage = onDeleteImage,
      onShowInvalidName = onShowInvalidName,
      onShowIncognitoInfo = onShowIncognitoInfo,
      onIncognitoChange = onIncognitoChange,
      onCreateGroup = onCreateGroup,
      onClose = onClose,
    )
  }
}

@Composable
fun NomeAddGroupContent(
  displayName: MutableState<String>,
  profileImage: String?,
  focusRequester: FocusRequester,
  incognito: MutableState<Boolean>,
  canCreate: Boolean,
  profileDisclosure: String,
  onEditImage: () -> Unit,
  onDeleteImage: () -> Unit,
  onShowInvalidName: () -> Unit,
  onShowIncognitoInfo: () -> Unit,
  onIncognitoChange: (Boolean) -> Unit,
  onCreateGroup: () -> Unit,
  onClose: () -> Unit,
) {
  val title = stringResource(MR.strings.create_secret_group_title)
  val backLabel = androidx.compose.ui.res.stringResource(R.string.nome_back)
  BackHandler(onBack = onClose)
  NomeFullPageScaffold(
    title = title,
    backLabel = backLabel,
    onClose = onClose,
  ) {
    Text(
      text = stringResource(MR.strings.group_display_name_field).trimEnd(':', '：'),
      modifier = Modifier.semantics { heading() },
      style = NomeTheme.typography.label,
      color = NomeTheme.colors.textSecondary,
    )
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border = BorderStroke(1.dp, NomeTheme.colors.divider),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(contentAlignment = Alignment.BottomEnd) {
          ProfileImage(
            size = 56.dp,
            image = profileImage,
            icon = MR.images.ic_supervised_user_circle_filled,
          )
          IconButton(
            onClick = onEditImage,
            modifier =
              Modifier
                .size(32.dp)
                .nomeTalkBackSemantics(
                  label = stringResource(MR.strings.edit_image),
                ),
          ) {
            NomeSurface(
              shape = CircleShape,
              color = NomeTheme.colors.action,
            ) {
              Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = null,
                modifier = Modifier.padding(7.dp).size(18.dp),
                tint = NomeTheme.colors.onAction,
              )
            }
          }
        }
        OutlinedTextField(
          value = displayName.value,
          onValueChange = { displayName.value = it },
          modifier =
            Modifier
              .weight(1f)
              .heightIn(min = 56.dp)
              .focusRequester(focusRequester),
          singleLine = true,
          leadingIcon = {
            Icon(
              imageVector = Icons.Rounded.Groups,
              contentDescription = null,
              tint = NomeTheme.colors.textSecondary,
            )
          },
          trailingIcon =
            if (displayName.value.isNotBlank() && !canCreate) {
              {
                IconButton(onClick = onShowInvalidName) {
                  Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = stringResource(MR.strings.invalid_name),
                    tint = NomeTheme.colors.danger,
                  )
                }
              }
            } else {
              null
            },
          placeholder = {
            Text(
              text = title,
              color = NomeTheme.colors.textTertiary,
            )
          },
          colors =
            TextFieldDefaults.outlinedTextFieldColors(
              focusedBorderColor = NomeTheme.colors.action,
              unfocusedBorderColor = NomeTheme.colors.border,
              textColor = NomeTheme.colors.textPrimary,
              cursorColor = NomeTheme.colors.action,
            ),
          shape = NomeTheme.shapes.control,
        )
        if (profileImage != null) {
          IconButton(
            onClick = onDeleteImage,
            modifier =
              Modifier
                .nomeMinimumTouchTarget()
                .nomeTalkBackSemantics(
                  label = stringResource(MR.strings.delete_image),
                ),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = null,
              tint = NomeTheme.colors.danger,
            )
          }
        }
      }
    }

    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      color = NomeTheme.colors.successContainer,
      border = BorderStroke(1.dp, NomeTheme.colors.success.copy(alpha = 0.24f)),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Security,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = NomeTheme.colors.success,
        )
        Text(
          text = stringResource(MR.strings.group_is_decentralized),
          modifier = Modifier.weight(1f),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.success,
        )
      }
    }

    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border = BorderStroke(1.dp, NomeTheme.colors.divider),
    ) {
      Column {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable(
                role = Role.Switch,
                onClick = {
                  onIncognitoChange(!incognito.value)
                },
              )
              .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Security,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = NomeTheme.colors.textSecondary,
          )
          Column(Modifier.weight(1f)) {
            Text(
              text = stringResource(MR.strings.incognito),
              style = NomeTheme.typography.body,
              color = NomeTheme.colors.textPrimary,
            )
            Text(
              text = profileDisclosure,
              style = NomeTheme.typography.supporting,
              color = NomeTheme.colors.textSecondary,
            )
          }
          IconButton(
            onClick = onShowIncognitoInfo,
            modifier =
              Modifier
                .nomeMinimumTouchTarget()
                .nomeTalkBackSemantics(
                  label = stringResource(MR.strings.incognito),
                ),
          ) {
            Icon(
              imageVector = Icons.Rounded.Info,
              contentDescription = null,
              tint = NomeTheme.colors.action,
            )
          }
          Switch(
            checked = incognito.value,
            onCheckedChange = onIncognitoChange,
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = NomeTheme.colors.onAction,
                checkedTrackColor = NomeTheme.colors.action,
                uncheckedThumbColor = NomeTheme.colors.textTertiary,
                uncheckedTrackColor = NomeTheme.colors.surfaceSubtle,
              ),
          )
        }
      }
    }

    Spacer(Modifier.height(2.dp))
    NomeButton(
      text = stringResource(MR.strings.create_group_button),
      onClick = onCreateGroup,
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp),
      enabled = canCreate,
      shape = NomeTheme.shapes.pill,
      semanticsLabel = stringResource(MR.strings.create_group_button),
      leadingIcon = {
        Icon(
          imageVector = Icons.Rounded.Groups,
          contentDescription = null,
          modifier = Modifier.size(NomeTheme.dimensions.icon),
        )
      },
    )
  }
}
