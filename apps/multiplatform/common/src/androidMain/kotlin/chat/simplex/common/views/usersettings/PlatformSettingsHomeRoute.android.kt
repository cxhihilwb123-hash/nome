package chat.simplex.common.views.usersettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.model.User
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
import dev.icerock.moko.resources.compose.painterResource

private data class NomeSettingsEntry(
  val title: String,
  val body: String,
  val icon: Painter,
  val infoAccent: Boolean = false,
  val enabled: Boolean,
  val onClick: () -> Unit,
)

@Composable
internal actual fun PlatformSettingsHomeRoute(
  currentUser: User?,
  stopped: Boolean,
  notificationsEnabled: Boolean,
  languageCode: String,
  onOpenIdentity: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenBackupMigration: () -> Unit,
  onOpenDesktop: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onOpenNetwork: () -> Unit,
  onOpenLanguage: () -> Unit,
  onOpenAppearance: () -> Unit,
  onOpenHelp: () -> Unit,
  onOpenAbout: () -> Unit,
  onOpenDeveloper: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenContacts: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeSettingsHomeContent(
      currentUser = currentUser,
      stopped = stopped,
      notificationsEnabled = notificationsEnabled,
      languageCode = languageCode,
      onOpenIdentity = onOpenIdentity,
      onOpenNotifications = onOpenNotifications,
      onOpenBackupMigration = onOpenBackupMigration,
      onOpenDesktop = onOpenDesktop,
      onOpenPrivacy = onOpenPrivacy,
      onOpenNetwork = onOpenNetwork,
      onOpenLanguage = onOpenLanguage,
      onOpenAppearance = onOpenAppearance,
      onOpenHelp = onOpenHelp,
      onOpenAbout = onOpenAbout,
      onOpenDeveloper = onOpenDeveloper,
      onOpenHome = onOpenHome,
      onOpenContacts = onOpenContacts,
    )
  }
}

@Composable
fun NomeSettingsHomeContent(
  currentUser: User?,
  stopped: Boolean,
  notificationsEnabled: Boolean,
  languageCode: String,
  onOpenIdentity: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenBackupMigration: () -> Unit,
  onOpenDesktop: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onOpenNetwork: () -> Unit,
  onOpenLanguage: () -> Unit,
  onOpenAppearance: () -> Unit,
  onOpenHelp: () -> Unit,
  onOpenAbout: () -> Unit,
  onOpenDeveloper: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenContacts: () -> Unit,
) {
  var searchVisible by remember { mutableStateOf(false) }
  var query by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val notificationBody =
    androidx.compose.ui.res.stringResource(
      if (notificationsEnabled) {
        R.string.nome_p23_notifications_on
      } else {
        R.string.nome_p23_notifications_off
      },
    )
  val languageLabel =
    androidx.compose.ui.res.stringResource(
      when {
        languageCode.startsWith("zh") ->
          R.string.nome_p23_language_zh
        languageCode == "en" ->
          R.string.nome_p23_language_en
        Locale.current.language.startsWith("zh") ->
          R.string.nome_p23_language_zh
        Locale.current.language == "en" ->
          R.string.nome_p23_language_en
        else ->
          R.string.nome_p23_language_system
      },
    )
  val generalEntries =
    listOf(
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_notifications,
          ),
        body = notificationBody,
        icon = painterResource(MR.images.ic_notifications),
        enabled = !stopped,
        onClick = onOpenNotifications,
      ),
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_backup,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_backup_body,
          ),
        icon = painterResource(MR.images.ic_ios_share),
        // DatabaseView owns the official stopped-chat restart control.
        // Keep this route reachable while the chat core is stopped.
        enabled = true,
        onClick = onOpenBackupMigration,
      ),
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_desktop,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_desktop_body,
          ),
        icon = painterResource(MR.images.ic_desktop),
        enabled = !stopped,
        onClick = onOpenDesktop,
      ),
    )
  val privacyEntries =
    listOf(
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_privacy,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_privacy_body,
          ),
        icon = painterResource(MR.images.ic_lock),
        enabled = !stopped,
        onClick = onOpenPrivacy,
      ),
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_network,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_network_body,
          ),
        icon = painterResource(MR.images.ic_settings_ethernet),
        enabled = !stopped,
        onClick = onOpenNetwork,
      ),
    )
  val displayEntries =
    listOf(
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_language,
          ),
        body = languageLabel,
        icon = painterResource(MR.images.ic_translate),
        infoAccent = true,
        enabled = true,
        onClick = onOpenLanguage,
      ),
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_help,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_help_body,
          ),
        icon = painterResource(MR.images.ic_help),
        enabled = !stopped,
        onClick = onOpenHelp,
      ),
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_about,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_about_body,
          ),
        icon = painterResource(MR.images.ic_info),
        enabled = true,
        onClick = onOpenAbout,
      ),
    )
  val searchableAdvancedEntries =
    listOf(
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_appearance,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_appearance_body,
          ),
        icon = painterResource(MR.images.ic_light_mode),
        enabled = true,
        onClick = onOpenAppearance,
      ),
      NomeSettingsEntry(
        title =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_developer,
          ),
        body =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_developer_body,
          ),
        icon = painterResource(MR.images.ic_code),
        enabled = true,
        onClick = onOpenDeveloper,
      ),
    )

  BackHandler(enabled = searchVisible) {
    query = ""
    searchVisible = false
    focusManager.clearFocus()
  }
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(NomeTheme.colors.background)
        .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    NomeSettingsHeader(
      searchVisible = searchVisible,
      query = query,
      onQueryChange = {
        query = it
      },
      focusRequester = focusRequester,
      onSearch = {
        searchVisible = true
      },
      onCloseSearch = {
        query = ""
        searchVisible = false
        focusManager.clearFocus()
      },
    )
    Divider(color = NomeTheme.colors.divider)
    Column(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(
            horizontal = 18.dp,
            vertical = 12.dp,
          ),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (query.isBlank()) {
        NomeSettingsIdentityCard(
          currentUser = currentUser,
          onClick = onOpenIdentity,
        )
      }
      val normalized = query.trim().lowercase()
      var visibleCount = 0
      visibleCount +=
        NomeSettingsSection(
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p23_general,
            ),
          entries = generalEntries,
          query = normalized,
        )
      visibleCount +=
        NomeSettingsSection(
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p23_privacy_section,
            ),
          entries = privacyEntries,
          query = normalized,
        )
      visibleCount +=
        NomeSettingsSection(
          title =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p23_language_section,
            ),
          entries =
            if (normalized.isBlank()) {
              displayEntries
            } else {
              displayEntries + searchableAdvancedEntries
            },
          query = normalized,
        )
      if (normalized.isNotBlank() && visibleCount == 0) {
        NomeSurface(
          modifier = Modifier.fillMaxWidth(),
          color = NomeTheme.colors.surfaceSubtle,
        ) {
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p23_no_results,
              ),
            modifier = Modifier.padding(18.dp),
            style = NomeTheme.typography.body,
            color = NomeTheme.colors.textSecondary,
          )
        }
      }
    }
    NomeSettingsBottomNavigation(
      onOpenHome = onOpenHome,
      onOpenContacts = onOpenContacts,
    )
  }
}

@Composable
private fun NomeSettingsHeader(
  searchVisible: Boolean,
  query: String,
  onQueryChange: (String) -> Unit,
  focusRequester: FocusRequester,
  onSearch: () -> Unit,
  onCloseSearch: () -> Unit,
) {
  if (searchVisible) {
    LaunchedEffect(Unit) {
      focusRequester.requestFocus()
    }
    OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier =
        Modifier
          .fillMaxWidth()
          .heightIn(min = 64.dp)
          .padding(
            horizontal = 14.dp,
            vertical = 6.dp,
          )
          .focusRequester(focusRequester),
      placeholder = {
        Text(
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_search,
          ),
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = null,
        )
      },
      trailingIcon = {
        IconButton(
          onClick = onCloseSearch,
          modifier =
            Modifier.nomeTalkBackSemantics(
              label =
                androidx.compose.ui.res.stringResource(
                  R.string.nome_p23_clear_search,
                ),
              role = Role.Button,
            ),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = null,
          )
        }
      },
      singleLine = true,
      keyboardOptions =
        KeyboardOptions(
          imeAction = ImeAction.Search,
        ),
      keyboardActions =
        KeyboardActions(
          onSearch = {},
        ),
      colors =
        TextFieldDefaults.outlinedTextFieldColors(
          backgroundColor = NomeTheme.colors.input,
          focusedBorderColor = NomeTheme.colors.action,
          unfocusedBorderColor = NomeTheme.colors.border,
          textColor = NomeTheme.colors.textPrimary,
          cursorColor = NomeTheme.colors.action,
        ),
    )
  } else {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(64.dp)
          .padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        painter =
          androidx.compose.ui.res.painterResource(
            R.drawable.nome_header_logo,
          ),
        contentDescription = null,
        modifier =
          Modifier
            .width(66.dp)
            .height(28.dp),
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p23_title,
          ),
        modifier =
          Modifier
            .weight(1f)
            .padding(start = 14.dp)
            .semantics { heading() },
        style = NomeTheme.typography.title,
        color = NomeTheme.colors.textPrimary,
      )
      IconButton(
        onClick = onSearch,
        modifier =
          Modifier
            .nomeMinimumTouchTarget()
            .nomeTalkBackSemantics(
              label =
                androidx.compose.ui.res.stringResource(
                  R.string.nome_p23_search_action,
                ),
              role = Role.Button,
            ),
      ) {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = null,
          modifier = Modifier.size(28.dp),
          tint = NomeTheme.colors.textPrimary,
        )
      }
    }
  }
}

@Composable
private fun NomeSettingsIdentityCard(
  currentUser: User?,
  onClick: () -> Unit,
) {
  val title =
    currentUser?.displayName
      ?: androidx.compose.ui.res.stringResource(
        R.string.nome_p23_no_identity,
      )
  val body =
    androidx.compose.ui.res.stringResource(
      R.string.nome_p23_primary_identity_body,
    )
  NomeSurface(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(
          role = Role.Button,
          onClick = onClick,
        )
        .nomeTalkBackSemantics(
          label = "$title. $body",
          role = Role.Button,
        ),
    color = NomeTheme.colors.surface,
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.border,
      ),
    shape = NomeTheme.shapes.grouped,
  ) {
    Row(
      modifier =
        Modifier.padding(
          horizontal = 14.dp,
          vertical = 12.dp,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ProfileImage(
        size = 58.dp,
        image = currentUser?.image,
      )
      Column(
        modifier =
          Modifier
            .weight(1f)
            .padding(horizontal = 14.dp),
      ) {
        Text(
          text =
            androidx.compose.ui.res.stringResource(
              R.string.nome_p23_primary_identity,
            ),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
        Text(
          text = title,
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = body,
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
          maxLines = 1,
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
  }
}

@Composable
private fun NomeSettingsSection(
  title: String,
  entries: List<NomeSettingsEntry>,
  query: String,
): Int {
  val visibleEntries =
    if (query.isBlank()) {
      entries
    } else {
      entries.filter {
        it.title.lowercase().contains(query) ||
          it.body.lowercase().contains(query)
      }
    }
  if (visibleEntries.isEmpty()) return 0
  Text(
    text = title,
    modifier =
      Modifier
        .padding(
          top = 2.dp,
          bottom = 2.dp,
        )
        .semantics { heading() },
    style = NomeTheme.typography.supporting,
    color = NomeTheme.colors.textSecondary,
  )
  Column(modifier = Modifier.fillMaxWidth()) {
    visibleEntries.forEachIndexed { index, entry ->
      NomeSettingsRow(entry)
      if (index != visibleEntries.lastIndex) {
        Divider(
          modifier = Modifier.padding(start = 54.dp),
          color = NomeTheme.colors.divider,
        )
      }
    }
  }
  return visibleEntries.size
}

@Composable
private fun NomeSettingsRow(
  entry: NomeSettingsEntry,
) {
  val contentColor =
    if (entry.enabled) {
      NomeTheme.colors.textPrimary
    } else {
      NomeTheme.colors.textTertiary
    }
  val accentColor =
    if (entry.infoAccent) {
      NomeTheme.colors.info
    } else {
      NomeTheme.colors.success
    }
  val accentContainer =
    if (entry.infoAccent) {
      NomeTheme.colors.infoContainer
    } else {
      NomeTheme.colors.successContainer
    }
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 50.dp)
        .clickable(
          enabled = entry.enabled,
          role = Role.Button,
          onClick = entry.onClick,
        )
        .nomeTalkBackSemantics(
          label = "${entry.title}. ${entry.body}",
          role = Role.Button,
          enabled = entry.enabled,
        )
        .padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(38.dp)
          .background(
            if (entry.enabled) {
              accentContainer
            } else {
              NomeTheme.colors.surfaceSubtle
            },
            NomeTheme.shapes.control,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = entry.icon,
        contentDescription = null,
        modifier = Modifier.size(21.dp),
        tint =
          if (entry.enabled) {
            accentColor
          } else {
            NomeTheme.colors.textTertiary
          },
      )
    }
    Column(
      modifier =
        Modifier
          .weight(1f)
          .padding(horizontal = 12.dp),
      verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
      Text(
        text = entry.title,
        style = NomeTheme.typography.label,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = entry.body,
        style = NomeTheme.typography.supporting,
        color =
          if (entry.enabled) {
            NomeTheme.colors.textSecondary
          } else {
            NomeTheme.colors.textTertiary
          },
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
}

@Composable
private fun NomeSettingsBottomNavigation(
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
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NomeSettingsNavItem(
      label =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p23_home,
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
    NomeSettingsNavItem(
      label =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p23_contacts,
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
    NomeSettingsNavItem(
      label =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p23_settings,
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
private fun NomeSettingsNavItem(
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
                R.string.nome_p23_selected,
              )
            } else {
              null
            },
          role = Role.Tab,
          enabled = !active,
        )
        .padding(horizontal = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Box(
      modifier =
        Modifier
          .background(
            if (active) {
              NomeTheme.colors.successContainer
            } else {
              Color.Transparent
            },
            CircleShape,
          )
          .padding(
            horizontal = 12.dp,
            vertical = 3.dp,
          ),
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
      fontWeight =
        if (active) {
          FontWeight.SemiBold
        } else {
          FontWeight.Normal
        },
      color =
        if (active) {
          NomeTheme.colors.success
        } else {
          NomeTheme.colors.textSecondary
        },
    )
  }
}

@Composable
internal actual fun PlatformBackupMigrationRoute(
  migrationEnabled: Boolean,
  onClose: () -> Unit,
  onOpenArchive: () -> Unit,
  onOpenMigration: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeBackupMigrationContent(
      migrationEnabled = migrationEnabled,
      onClose = onClose,
      onOpenArchive = onOpenArchive,
      onOpenMigration = onOpenMigration,
    )
  }
}

@Composable
fun NomeBackupMigrationContent(
  migrationEnabled: Boolean = true,
  onClose: () -> Unit,
  onOpenArchive: () -> Unit,
  onOpenMigration: () -> Unit,
) {
  chat.simplex.common.platform.BackHandler(
    onBack = onClose,
  )
  NomeFullPageScaffold(
    title =
      androidx.compose.ui.res.stringResource(
        R.string.nome_p24_title,
      ),
    backLabel =
      androidx.compose.ui.res.stringResource(
        R.string.nome_p24_back,
      ),
    onClose = onClose,
  ) {
    NomeBackupInfoStrip()
    NomeArchiveCard(onOpenArchive)
    NomeBackupRouteRow(
      title =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p24_restore_title,
        ),
      body =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p24_restore_body,
        ),
      icon = painterResource(MR.images.ic_download),
      infoAccent = true,
      onClick = onOpenArchive,
    )
    NomeBackupRouteRow(
      title =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p24_migrate_title,
        ),
      body =
        androidx.compose.ui.res.stringResource(
          R.string.nome_p24_migrate_body,
        ),
      icon = painterResource(MR.images.ic_qr_code),
      infoAccent = false,
      enabled = migrationEnabled,
      onClick = onOpenMigration,
    )
    NomeMigrationStages()
  }
}

@Composable
private fun NomeBackupInfoStrip() {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.successContainer,
    contentColor = NomeTheme.colors.onSuccessContainer,
    shape = NomeTheme.shapes.control,
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        painter = painterResource(MR.images.ic_database),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = NomeTheme.colors.success,
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p24_info,
          ),
        modifier = Modifier.weight(1f),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.onSuccessContainer,
      )
    }
  }
}

@Composable
private fun NomeArchiveCard(
  onOpenArchive: () -> Unit,
) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.surface,
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.border,
      ),
    shape = NomeTheme.shapes.grouped,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(40.dp)
              .background(
                NomeTheme.colors.successContainer,
                NomeTheme.shapes.control,
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter =
              painterResource(
                MR.images.ic_upload_file,
              ),
            contentDescription = null,
            tint = NomeTheme.colors.success,
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p24_archive_title,
              ),
            style = NomeTheme.typography.title,
            color = NomeTheme.colors.textPrimary,
          )
          Text(
            text =
              androidx.compose.ui.res.stringResource(
                R.string.nome_p24_archive_body,
              ),
            style = NomeTheme.typography.supporting,
            color = NomeTheme.colors.textSecondary,
          )
        }
      }
      NomeButton(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p24_archive_action,
          ),
        onClick = onOpenArchive,
        modifier = Modifier.fillMaxWidth(),
        semanticsLabel =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p24_archive_action,
          ),
      )
    }
  }
}

@Composable
private fun NomeBackupRouteRow(
  title: String,
  body: String,
  icon: Painter,
  infoAccent: Boolean,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  val titleColor =
    if (enabled) {
      NomeTheme.colors.textPrimary
    } else {
      NomeTheme.colors.textTertiary
    }
  val bodyColor =
    if (enabled) {
      NomeTheme.colors.textSecondary
    } else {
      NomeTheme.colors.textTertiary
    }
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .clickable(
          enabled = enabled,
          role = Role.Button,
          onClick = onClick,
        )
        .nomeTalkBackSemantics(
          label = "$title. $body",
          role = Role.Button,
          enabled = enabled,
        )
        .padding(
          horizontal = 8.dp,
          vertical = 6.dp,
        ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(40.dp)
          .background(
            if (infoAccent) {
              NomeTheme.colors.infoContainer
            } else {
              NomeTheme.colors.successContainer
            },
            NomeTheme.shapes.control,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = icon,
        contentDescription = null,
        tint =
          if (infoAccent) {
            NomeTheme.colors.info
          } else {
            NomeTheme.colors.success
          },
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
        color = titleColor,
      )
      Text(
        text = body,
        style = NomeTheme.typography.supporting,
        color = bodyColor,
      )
    }
    Icon(
      imageVector =
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
      contentDescription = null,
      tint =
        if (enabled) {
          NomeTheme.colors.textTertiary
        } else {
          NomeTheme.colors.surfaceSubtle
        },
    )
  }
  Divider(
    modifier = Modifier.padding(start = 56.dp),
    color = NomeTheme.colors.divider,
  )
}

@Composable
private fun NomeMigrationStages() {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.surfaceSubtle,
    shape = NomeTheme.shapes.grouped,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p24_stages_title,
          ),
        style = NomeTheme.typography.title,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p24_stages_body,
          ),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
      NomeMigrationStageRow(
        number = "1",
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p24_stage_prepare,
          ),
      )
      NomeMigrationStageRow(
        number = "2",
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p24_stage_archive,
          ),
      )
      NomeMigrationStageRow(
        number = "3",
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p24_stage_link,
          ),
      )
    }
  }
}

@Composable
private fun NomeMigrationStageRow(
  number: String,
  text: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(28.dp)
          .background(
            NomeTheme.colors.surfaceContainer,
            CircleShape,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = number,
        style = NomeTheme.typography.supporting,
        fontWeight = FontWeight.SemiBold,
        color = NomeTheme.colors.textSecondary,
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = text,
        style = NomeTheme.typography.body,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text =
          androidx.compose.ui.res.stringResource(
            R.string.nome_p24_not_started,
          ),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
  }
}
