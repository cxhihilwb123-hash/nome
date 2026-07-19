package chat.simplex.common.views.usersettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.model.User
import chat.simplex.common.model.UserInfo
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource

@Composable
internal actual fun PlatformIdentityCenterRoute(
  users: List<User>,
  filteredUsers: List<UserInfo>,
  searchTextOrPassword: MutableState<String>,
  profileHidden: Boolean,
  visibleUsersCount: Int,
  incognitoDefault: Boolean,
  socksProxyEnabled: Boolean,
  onClose: () -> Unit,
  onRevealPasswordEntry: () -> Unit,
  onAddUser: () -> Unit,
  onEditCurrentUser: () -> Unit,
  onActivateUser: (User) -> Unit,
  onRemoveUser: (User) -> Unit,
  onUnhideUser: (User) -> Unit,
  onMuteUser: (User) -> Unit,
  onUnmuteUser: (User) -> Unit,
  onHideUser: (User) -> Unit,
  onSetIncognitoDefault: (Boolean) -> Unit,
  onOpenIncognitoInfo: () -> Unit,
  onOpenNetworkSettings: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenContacts: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeIdentityCenterContent(
      users = users,
      filteredUsers = filteredUsers,
      searchTextOrPassword = searchTextOrPassword,
      profileHidden = profileHidden,
      visibleUsersCount = visibleUsersCount,
      incognitoDefault = incognitoDefault,
      socksProxyEnabled = socksProxyEnabled,
      onClose = onClose,
      onRevealPasswordEntry = onRevealPasswordEntry,
      onAddUser = onAddUser,
      onEditCurrentUser = onEditCurrentUser,
      onActivateUser = onActivateUser,
      onRemoveUser = onRemoveUser,
      onUnhideUser = onUnhideUser,
      onMuteUser = onMuteUser,
      onUnmuteUser = onUnmuteUser,
      onHideUser = onHideUser,
      onSetIncognitoDefault = onSetIncognitoDefault,
      onOpenIncognitoInfo = onOpenIncognitoInfo,
      onOpenNetworkSettings = onOpenNetworkSettings,
      onOpenHome = onOpenHome,
      onOpenContacts = onOpenContacts,
    )
  }
}

@Composable
fun NomeIdentityCenterContent(
  users: List<User>,
  filteredUsers: List<UserInfo>,
  searchTextOrPassword: MutableState<String>,
  profileHidden: Boolean,
  visibleUsersCount: Int,
  incognitoDefault: Boolean,
  socksProxyEnabled: Boolean,
  onClose: () -> Unit,
  onRevealPasswordEntry: () -> Unit,
  onAddUser: () -> Unit,
  onEditCurrentUser: () -> Unit,
  onActivateUser: (User) -> Unit,
  onRemoveUser: (User) -> Unit,
  onUnhideUser: (User) -> Unit,
  onMuteUser: (User) -> Unit,
  onUnmuteUser: (User) -> Unit,
  onHideUser: (User) -> Unit,
  onSetIncognitoDefault: (Boolean) -> Unit,
  onOpenIncognitoInfo: () -> Unit,
  onOpenNetworkSettings: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenContacts: () -> Unit,
) {
  var passwordEntryVisible by
    remember(profileHidden) {
      mutableStateOf(
        profileHidden ||
          searchTextOrPassword.value.isNotBlank(),
      )
    }
  val active =
    filteredUsers.firstOrNull {
      it.user.activeUser
    }
  val inactive =
    filteredUsers.filterNot {
      it.user.activeUser
    }
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(NomeTheme.colors.background)
        .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    NomeIdentityHeader(
      onClose = onClose,
      onAddUser = onAddUser,
    )
    Divider(color = NomeTheme.colors.divider)
    Column(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(
            horizontal =
              NomeTheme.dimensions.screenHorizontalInset,
            vertical = NomeTheme.dimensions.space20,
          ),
      verticalArrangement =
        Arrangement.spacedBy(
          NomeTheme.dimensions.space12,
        ),
    ) {
      NomeSectionLabel(
        androidx.compose.ui.res.stringResource(
          R.string.nome_p22_local_identities,
        ),
      )
      active?.let {
        NomeActiveIdentityCard(
          user = it.user,
          visibleUsersCount = visibleUsersCount,
          onEdit = onEditCurrentUser,
          onRemove = onRemoveUser,
          onMute = onMuteUser,
          onUnmute = onUnmuteUser,
          onHide = onHideUser,
        )
      }
      if (active == null) {
        NomeSurface(
          modifier = Modifier.fillMaxWidth(),
          color = NomeTheme.colors.surfaceSubtle,
        ) {
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_no_active_identity,
              ),
            modifier =
              Modifier.padding(
                NomeTheme.dimensions.space16,
              ),
            style = NomeTheme.typography.body,
            color = NomeTheme.colors.textSecondary,
          )
        }
      }
      inactive.forEach { userInfo ->
        NomeIdentityRow(
          user = userInfo.user,
          visibleUsersCount = visibleUsersCount,
          onActivate = onActivateUser,
          onRemove = onRemoveUser,
          onUnhide = onUnhideUser,
          onMute = onMuteUser,
          onUnmute = onUnmuteUser,
          onHide = onHideUser,
        )
      }
      if (inactive.isEmpty()) {
        NomeActionRow(
          icon = painterResource(MR.images.ic_work),
          iconTint = NomeTheme.colors.info,
          iconBackground =
            NomeTheme.colors.infoContainer,
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p22_add_identity,
            ),
          body =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p22_add_identity_body,
            ),
          onClick = onAddUser,
        )
      }

      val hasHiddenProfiles =
        users.any {
          it.hidden
        }
      if (hasHiddenProfiles) {
        NomeActionRow(
          icon = painterResource(MR.images.ic_lock),
          iconTint = NomeTheme.colors.textSecondary,
          iconBackground =
            NomeTheme.colors.surfaceSubtle,
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p22_hidden_identities,
            ),
          body =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p22_hidden_identities_body,
            ),
          onClick = {
            onRevealPasswordEntry()
            passwordEntryVisible = true
          },
        )
      }
      if (passwordEntryVisible) {
        NomeIdentityPasswordEntry(
          searchTextOrPassword,
        )
      }

      NomeToggleRow(
        icon =
          painterResource(
            MR.images.ic_theater_comedy_filled,
          ),
        iconTint = NomeTheme.colors.success,
        iconBackground =
          NomeTheme.colors.successContainer,
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p22_incognito,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p22_incognito_body,
          ),
        checked = incognitoDefault,
        onCheckedChange = onSetIncognitoDefault,
        onBodyClick = onOpenIncognitoInfo,
      )

      Spacer(
        Modifier.height(
          NomeTheme.dimensions.space8,
        ),
      )
      NomeSectionLabel(
        androidx.compose.ui.res.stringResource(
          R.string.nome_p22_connection_privacy,
        ),
      )
      NomeToggleRow(
        icon =
          painterResource(
            MR.images.ic_settings_ethernet,
          ),
        iconTint = NomeTheme.colors.info,
        iconBackground =
          NomeTheme.colors.infoContainer,
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p22_network_settings,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p22_network_settings_body,
          ),
        checked = socksProxyEnabled,
        onCheckedChange = {
          onOpenNetworkSettings()
        },
        onBodyClick = onOpenNetworkSettings,
      )
    }
    NomeIdentityBottomNavigation(
      onOpenHome = onOpenHome,
      onOpenContacts = onOpenContacts,
    )
  }
}

@Composable
private fun NomeIdentityHeader(
  onClose: () -> Unit,
  onAddUser: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(64.dp)
        .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(
      onClick = onClose,
      modifier =
        Modifier
          .nomeMinimumTouchTarget()
          .nomeTalkBackSemantics(
            label =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_back,
              ),
            role = Role.Button,
          ),
    ) {
      Icon(
        imageVector =
          Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = null,
        tint = NomeTheme.colors.textPrimary,
      )
    }
    Text(
      text =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p22_title,
        ),
      modifier =
        Modifier
          .weight(1f)
          .padding(horizontal = 8.dp)
          .semantics { heading() },
      style = NomeTheme.typography.titleLarge,
      color = NomeTheme.colors.textPrimary,
    )
    IconButton(
      onClick = onAddUser,
      modifier =
        Modifier
          .nomeMinimumTouchTarget()
          .nomeTalkBackSemantics(
            label =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_add_identity,
              ),
            role = Role.Button,
          ),
    ) {
      Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = null,
        modifier =
          Modifier.size(
            NomeTheme.dimensions.iconLarge,
          ),
        tint = NomeTheme.colors.textPrimary,
      )
    }
  }
}

@Composable
private fun NomeActiveIdentityCard(
  user: User,
  visibleUsersCount: Int,
  onEdit: () -> Unit,
  onRemove: (User) -> Unit,
  onMute: (User) -> Unit,
  onUnmute: (User) -> Unit,
  onHide: (User) -> Unit,
) {
  NomeSurface(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(
          role = Role.Button,
          onClick = onEdit,
        )
        .nomeTalkBackSemantics(
          label =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p22_edit_current,
              user.displayName,
            ),
          role = Role.Button,
        ),
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.success,
      ),
    shape = NomeTheme.shapes.grouped,
    color = NomeTheme.colors.surface,
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ProfileImage(
        size = 68.dp,
        image = user.image,
      )
      Column(
        modifier =
          Modifier
            .weight(1f)
            .padding(horizontal = 14.dp),
        verticalArrangement =
          Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p22_current,
            ),
          style = NomeTheme.typography.supporting,
          fontWeight = FontWeight.SemiBold,
          color = NomeTheme.colors.success,
        )
        Text(
          text = user.displayName,
          style = NomeTheme.typography.title,
          color = NomeTheme.colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            user.shortDescr
              ?: androidx.compose.ui.res.stringResource(
                R.string.nome_p22_current_body,
              ),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = NomeTheme.colors.success,
      )
      NomeIdentityActions(
        user = user,
        visibleUsersCount = visibleUsersCount,
        onRemove = onRemove,
        onUnhide = {},
        onMute = onMute,
        onUnmute = onUnmute,
        onHide = onHide,
      )
    }
  }
}

@Composable
private fun NomeIdentityRow(
  user: User,
  visibleUsersCount: Int,
  onActivate: (User) -> Unit,
  onRemove: (User) -> Unit,
  onUnhide: (User) -> Unit,
  onMute: (User) -> Unit,
  onUnmute: (User) -> Unit,
  onHide: (User) -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 68.dp)
        .clickable(
          role = Role.Button,
          onClick = {
            onActivate(user)
          },
        )
        .nomeTalkBackSemantics(
          label =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p22_activate,
              user.displayName,
            ),
          role = Role.Button,
        )
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ProfileImage(
      size = 48.dp,
      image = user.image,
    )
    Column(
      modifier =
        Modifier
          .weight(1f)
          .padding(horizontal = 12.dp),
    ) {
      Text(
        text = user.displayName,
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text =
          when {
            user.hidden ->
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_hidden,
              )
            !user.showNtfs ->
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_muted,
              )
            else ->
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_local_identity,
              )
          },
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
    NomeIdentityActions(
      user = user,
      visibleUsersCount = visibleUsersCount,
      onRemove = onRemove,
      onUnhide = onUnhide,
      onMute = onMute,
      onUnmute = onUnmute,
      onHide = onHide,
    )
  }
  Divider(
    modifier = Modifier.padding(start = 68.dp),
    color = NomeTheme.colors.divider,
  )
}

@Composable
private fun NomeIdentityActions(
  user: User,
  visibleUsersCount: Int,
  onRemove: (User) -> Unit,
  onUnhide: (User) -> Unit,
  onMute: (User) -> Unit,
  onUnmute: (User) -> Unit,
  onHide: (User) -> Unit,
) {
  var expanded by
    remember(user.userId) {
      mutableStateOf(false)
    }
  Box {
    IconButton(
      onClick = {
        expanded = true
      },
      modifier =
        Modifier
          .nomeMinimumTouchTarget()
          .nomeTalkBackSemantics(
            label =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_identity_actions,
                user.displayName,
              ),
            role = Role.Button,
          ),
    ) {
      Icon(
        painter =
          painterResource(
            MR.images.ic_more_horiz,
          ),
        contentDescription = null,
        tint = NomeTheme.colors.textSecondary,
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = {
        expanded = false
      },
    ) {
      if (user.hidden) {
        NomeIdentityMenuItem(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p22_unhide,
            ),
          icon = painterResource(MR.images.ic_lock_open_right),
        ) {
          expanded = false
          onUnhide(user)
        }
      } else {
        if (visibleUsersCount > 1) {
          NomeIdentityMenuItem(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_hide,
              ),
            icon = painterResource(MR.images.ic_lock),
          ) {
            expanded = false
            onHide(user)
          }
        }
        if (user.showNtfs) {
          NomeIdentityMenuItem(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_mute,
              ),
            icon =
              painterResource(
                MR.images.ic_notifications_off,
              ),
          ) {
            expanded = false
            onMute(user)
          }
        } else {
          NomeIdentityMenuItem(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_unmute,
              ),
            icon =
              painterResource(
                MR.images.ic_notifications,
              ),
          ) {
            expanded = false
            onUnmute(user)
          }
        }
      }
      NomeIdentityMenuItem(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p22_delete,
          ),
        icon = painterResource(MR.images.ic_delete),
        color = NomeTheme.colors.danger,
      ) {
        expanded = false
        onRemove(user)
      }
    }
  }
}

@Composable
private fun NomeIdentityMenuItem(
  text: String,
  icon: Painter,
  color: Color = NomeTheme.colors.textPrimary,
  onClick: () -> Unit,
) {
  DropdownMenuItem(
    onClick = onClick,
    modifier =
      Modifier.heightIn(
        min = NomeTheme.dimensions.minimumTouchTarget,
      ),
  ) {
    Icon(
      painter = icon,
      contentDescription = null,
      modifier =
        Modifier.size(
          NomeTheme.dimensions.icon,
        ),
      tint = color,
    )
    Spacer(Modifier.size(12.dp))
    Text(
      text = text,
      color = color,
      style = NomeTheme.typography.body,
    )
  }
}

@Composable
private fun NomeIdentityPasswordEntry(
  searchTextOrPassword: MutableState<String>,
) {
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }
  OutlinedTextField(
    value = searchTextOrPassword.value,
    onValueChange = {
      searchTextOrPassword.value = it
    },
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .focusRequester(focusRequester),
    textStyle = NomeTheme.typography.body,
    singleLine = true,
    label = {
      Text(
        androidx.compose.ui.res.stringResource(
          R.string.nome_p22_hidden_password,
        ),
      )
    },
    visualTransformation =
      PasswordVisualTransformation(),
    keyboardOptions =
      androidx.compose.foundation.text.KeyboardOptions(
        imeAction = ImeAction.Done,
      ),
    keyboardActions =
      KeyboardActions(
        onDone = {
          focusManager.clearFocus()
        },
      ),
    colors =
      TextFieldDefaults.outlinedTextFieldColors(
        textColor = NomeTheme.colors.textPrimary,
        cursorColor = NomeTheme.colors.action,
        focusedBorderColor = NomeTheme.colors.action,
        unfocusedBorderColor = NomeTheme.colors.border,
        focusedLabelColor = NomeTheme.colors.action,
        unfocusedLabelColor =
          NomeTheme.colors.textSecondary,
      ),
  )
}

@Composable
private fun NomeToggleRow(
  icon: Painter,
  iconTint: Color,
  iconBackground: Color,
  title: String,
  body: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  onBodyClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 68.dp)
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(40.dp)
          .background(
            color = iconBackground,
            shape = NomeTheme.shapes.control,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = icon,
        contentDescription = null,
        modifier =
          Modifier.size(
            NomeTheme.dimensions.icon,
          ),
        tint = iconTint,
      )
    }
    Column(
      modifier =
        Modifier
          .weight(1f)
          .clickable(
            role = Role.Button,
            onClick = onBodyClick,
          )
          .nomeTalkBackSemantics(
            label = "$title. $body",
            role = Role.Button,
          )
          .padding(horizontal = 12.dp),
    ) {
      Text(
        text = title,
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text = body,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier =
        Modifier
          .nomeMinimumTouchTarget()
          .nomeTalkBackSemantics(
            label = title,
            state =
              androidx.compose.ui.res.stringResource(
                if (checked) {
                  R.string.nome_p22_on
                } else {
                  R.string.nome_p22_off
                },
              ),
            role = Role.Switch,
          ),
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor =
            NomeTheme.colors.success,
          uncheckedThumbColor = Color.White,
          uncheckedTrackColor =
            NomeTheme.colors.border,
        ),
    )
  }
  Divider(
    modifier = Modifier.padding(start = 60.dp),
    color = NomeTheme.colors.divider,
  )
}

@Composable
private fun NomeActionRow(
  icon: Painter,
  iconTint: Color,
  iconBackground: Color,
  title: String,
  body: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 68.dp)
        .clickable(
          role = Role.Button,
          onClick = onClick,
        )
        .nomeTalkBackSemantics(
          label = "$title. $body",
          role = Role.Button,
        )
        .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(40.dp)
          .background(
            color = iconBackground,
            shape = NomeTheme.shapes.control,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = icon,
        contentDescription = null,
        modifier =
          Modifier.size(
            NomeTheme.dimensions.icon,
          ),
        tint = iconTint,
      )
    }
    Column(
      modifier =
        Modifier
          .weight(1f)
          .padding(horizontal = 12.dp),
    ) {
      Text(
        text = title,
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text = body,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Icon(
      imageVector =
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint = NomeTheme.colors.textTertiary,
    )
  }
  Divider(
    modifier = Modifier.padding(start = 60.dp),
    color = NomeTheme.colors.divider,
  )
}

@Composable
private fun NomeSectionLabel(
  text: String,
) {
  Text(
    text = text,
    modifier = Modifier.semantics { heading() },
    style = NomeTheme.typography.supporting,
    color = NomeTheme.colors.textSecondary,
  )
}

@Composable
private fun NomeIdentityBottomNavigation(
  onOpenHome: () -> Unit,
  onOpenContacts: () -> Unit,
) {
  Divider(color = NomeTheme.colors.divider)
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .height(76.dp)
        .background(
          NomeTheme.colors.surfaceContainer,
        )
        .padding(horizontal = 28.dp),
    horizontalArrangement =
      Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NomeIdentityNavItem(
      label =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p22_home,
        ),
      icon = {
        Icon(
          imageVector = Icons.Rounded.Home,
          contentDescription = null,
        )
      },
      active = false,
      onClick = onOpenHome,
    )
    NomeIdentityNavItem(
      label =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p22_contacts,
        ),
      icon = {
        Icon(
          imageVector = Icons.Rounded.People,
          contentDescription = null,
        )
      },
      active = false,
      onClick = onOpenContacts,
    )
    NomeIdentityNavItem(
      label =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p22_settings,
        ),
      icon = {
        Icon(
          imageVector = Icons.Rounded.Settings,
          contentDescription = null,
        )
      },
      active = true,
      onClick = {},
    )
  }
}

@Composable
private fun NomeIdentityNavItem(
  label: String,
  icon: @Composable () -> Unit,
  active: Boolean,
  onClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .nomeMinimumTouchTarget()
        .clickable(
          enabled = !active,
          role = Role.Tab,
          onClick = onClick,
        )
        .nomeTalkBackSemantics(
          label = label,
          state =
            if (active) {
              androidx.compose.ui.res.stringResource(
                R.string.nome_p22_selected,
              )
            } else {
              null
            },
          role = Role.Tab,
          enabled = !active,
        )
        .padding(horizontal = 12.dp, vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Box(
      modifier =
        if (active) {
          Modifier
            .background(
              NomeTheme.colors.successContainer,
              CircleShape,
            )
            .padding(horizontal = 14.dp, vertical = 4.dp)
        } else {
          Modifier.padding(
            horizontal = 14.dp,
            vertical = 4.dp,
          )
        },
      contentAlignment = Alignment.Center,
    ) {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material.LocalContentColor provides
          if (active) {
            NomeTheme.colors.success
          } else {
            NomeTheme.colors.textSecondary
          },
      ) {
        icon()
      }
    }
    Text(
      text = label,
      style = NomeTheme.typography.supporting,
      color =
        if (active) {
          NomeTheme.colors.success
        } else {
          NomeTheme.colors.textSecondary
        },
    )
  }
}
