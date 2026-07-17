package chat.simplex.app.nome.fixtures

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.simplex.app.R
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeStatePanel
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme

@Composable
internal fun NomeFoundationFixture(spec: NomeFixtureSpec) {
  NomeAndroidTheme(darkTheme = spec.theme == NomeFixtureTheme.DARK) {
    val debugDescription = stringResource(
      R.string.nome_fixture_debug_semantics,
      spec.page.name,
    )
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(NomeTheme.colors.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .semantics { contentDescription = debugDescription },
    ) {
      DebugReferenceBadge(spec.page)
      if (spec.state == NomeFixtureState.NORMAL && !spec.renderStatePanel) {
        when (spec.page) {
          NomeFixturePage.P02 -> AppLockFixture()
          NomeFixturePage.P07 -> HomeFixture()
          NomeFixturePage.P17 -> DirectConversationFixture()
          NomeFixturePage.P21 -> ChannelConversationFixture()
          NomeFixturePage.P23 -> SettingsFixture(spec.theme)
        }
      } else {
        StateReferenceFixture(spec)
      }
    }
  }
}

@Composable
private fun DebugReferenceBadge(page: NomeFixturePage) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.compact,
    color = NomeTheme.colors.surfaceContainer,
    border = BorderStroke(NomeTheme.dimensions.divider, NomeTheme.colors.border),
  ) {
    Text(
      text = stringResource(R.string.nome_fixture_debug_badge, page.name),
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textSecondary,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun StateReferenceFixture(spec: NomeFixtureSpec) {
  val title = stringResource(stateTitle(spec.state))
  val description = stringResource(stateDescription(spec.state))
  val semanticStateDescription = stringResource(stateSemanticDescription(spec.state))
  FixtureScrollableColumn {
    FixtureTopBar(
      title = pageTitle(spec.page),
      subtitle = stringResource(R.string.nome_fixture_brand_tagline),
      showBack = spec.page != NomeFixturePage.P07,
    )
    NomeStatePanel(
      state = spec.state.panelState,
      title = title,
      description = description,
      stateDescription = semanticStateDescription,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun AppLockFixture() {
  FixtureScrollableColumn(horizontalAlignment = Alignment.CenterHorizontally) {
    Spacer(Modifier.height(8.dp))
    Icon(
      imageVector = Icons.Rounded.Lock,
      contentDescription = null,
      modifier = Modifier.size(48.dp),
      tint = NomeTheme.colors.action,
    )
    Text(
      text = stringResource(R.string.nome_fixture_p02_title),
      style = NomeTheme.typography.display,
      color = NomeTheme.colors.textPrimary,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(R.string.nome_fixture_p02_subtitle),
      style = NomeTheme.typography.body,
      color = NomeTheme.colors.textSecondary,
      textAlign = TextAlign.Center,
    )
    FixtureIdentityCard(
      title = stringResource(R.string.nome_fixture_p02_identity),
      supporting = stringResource(R.string.nome_fixture_p02_identity_supporting),
    )
    NomeStatePanel(
      state = NomeFixtureState.PERMISSION.panelState,
      title = stringResource(R.string.nome_fixture_p02_unavailable_title),
      description = stringResource(R.string.nome_fixture_p02_unavailable_body),
      stateDescription = stringResource(R.string.nome_fixture_state_permission_semantics),
      modifier = Modifier.fillMaxWidth(),
    )
    NomeButton(
      text = stringResource(R.string.nome_fixture_p02_device_credential),
      onClick = {},
      modifier = Modifier.fillMaxWidth(),
      semanticsLabel = stringResource(R.string.nome_fixture_p02_device_credential),
    )
    NomeButton(
      text = stringResource(R.string.nome_fixture_p02_app_passcode),
      onClick = {},
      modifier = Modifier.fillMaxWidth(),
      variant = NomeButtonVariant.SECONDARY,
      semanticsLabel = stringResource(R.string.nome_fixture_p02_app_passcode),
    )
    Text(
      text = stringResource(R.string.nome_fixture_p02_security_note),
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textTertiary,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun HomeFixture() {
  FixtureScrollableColumn {
    FixtureTopBar(
      title = stringResource(R.string.nome_fixture_p07_title),
      subtitle = stringResource(R.string.nome_fixture_brand_tagline),
    )
    FixtureSearchField(
      label = stringResource(R.string.nome_fixture_p07_search),
      semanticsLabel = stringResource(R.string.nome_fixture_p07_search_semantics),
    )
    FixtureSectionLabel(R.string.nome_fixture_p07_section_recent)
    FixtureChatRow(
      title = stringResource(R.string.nome_fixture_p07_contact_one),
      body = stringResource(R.string.nome_fixture_p07_preview_one),
      meta = stringResource(R.string.nome_fixture_p07_time_one),
      initial = "A",
    )
    FixtureChatRow(
      title = stringResource(R.string.nome_fixture_p07_contact_two),
      body = stringResource(R.string.nome_fixture_p07_preview_two),
      meta = stringResource(R.string.nome_fixture_p07_time_two),
      initial = "D",
    )
    FixtureSectionLabel(R.string.nome_fixture_p07_connection_section)
    FixtureConnectionButton(
      title = stringResource(R.string.nome_fixture_p07_new_contact),
      body = stringResource(R.string.nome_fixture_p07_new_contact_body),
      icon = Icons.Rounded.Contacts,
    )
    FixtureConnectionButton(
      title = stringResource(R.string.nome_fixture_p07_new_group),
      body = stringResource(R.string.nome_fixture_p07_new_group_body),
      icon = Icons.Rounded.Group,
    )
    FixtureConnectionButton(
      title = stringResource(R.string.nome_fixture_p07_invitation),
      body = stringResource(R.string.nome_fixture_p07_invitation_body),
      icon = Icons.Rounded.Add,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
    ) {
      NomeButton(
        text = stringResource(R.string.nome_fixture_add),
        onClick = {},
        semanticsLabel = stringResource(R.string.nome_fixture_add),
        leadingIcon = {
          Icon(Icons.Rounded.Add, contentDescription = null)
        },
      )
    }
    FixtureBottomNavigation(selectedIndex = 0)
  }
}

@Composable
private fun DirectConversationFixture() {
  FixtureScrollableColumn {
    FixtureTopBar(
      title = stringResource(R.string.nome_fixture_p17_title),
      subtitle = stringResource(R.string.nome_fixture_p17_subtitle),
      showBack = true,
      showMore = true,
    )
    FixtureInfoStrip(
      text = stringResource(R.string.nome_fixture_p17_security),
      icon = Icons.Rounded.Security,
    )
    Text(
      text = stringResource(R.string.nome_fixture_p17_date),
      modifier = Modifier.fillMaxWidth(),
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textTertiary,
      textAlign = TextAlign.Center,
    )
    FixtureMessageBubble(
      text = stringResource(R.string.nome_fixture_p17_received),
      sent = false,
    )
    FixtureMessageBubble(
      text = stringResource(R.string.nome_fixture_p17_sent),
      supporting = stringResource(R.string.nome_fixture_p17_delivery_unknown),
      sent = true,
    )
    FixtureAttachmentRow(
      title = stringResource(R.string.nome_fixture_p17_file),
      body = stringResource(R.string.nome_fixture_p17_file_meta),
      icon = Icons.Rounded.AttachFile,
    )
    FixtureAttachmentRow(
      title = stringResource(R.string.nome_fixture_p17_voice),
      body = stringResource(R.string.nome_fixture_p17_voice_meta),
      icon = Icons.Rounded.Mic,
    )
    FixtureComposer()
  }
}

@Composable
private fun ChannelConversationFixture() {
  FixtureScrollableColumn {
    FixtureTopBar(
      title = stringResource(R.string.nome_fixture_p21_title),
      subtitle = stringResource(R.string.nome_fixture_p21_subtitle),
      showBack = true,
      showMore = true,
    )
    NomeStatePanel(
      state = NomeFixtureState.DANGER.panelState,
      title = stringResource(R.string.nome_fixture_p21_warning_title),
      description = stringResource(R.string.nome_fixture_p21_warning_body),
      stateDescription = stringResource(R.string.nome_fixture_state_danger_semantics),
      modifier = Modifier.fillMaxWidth(),
    )
    FixturePostCard(
      title = stringResource(R.string.nome_fixture_p21_post_one_title),
      body = stringResource(R.string.nome_fixture_p21_post_one_body),
    )
    FixturePostCard(
      title = stringResource(R.string.nome_fixture_p21_post_two_title),
      body = stringResource(R.string.nome_fixture_p21_post_two_body),
    )
    FixtureInfoStrip(
      text = "${stringResource(R.string.nome_fixture_p21_evidence_title)} — " +
        stringResource(R.string.nome_fixture_p21_evidence_body),
      icon = Icons.Rounded.Info,
    )
    NomeButton(
      text = stringResource(R.string.nome_fixture_p21_observer),
      onClick = {},
      modifier = Modifier.fillMaxWidth(),
      enabled = false,
      semanticsLabel = stringResource(R.string.nome_fixture_p21_observer),
    )
  }
}

@Composable
private fun SettingsFixture(theme: NomeFixtureTheme) {
  FixtureScrollableColumn {
    FixtureTopBar(
      title = stringResource(R.string.nome_fixture_p23_title),
      subtitle = stringResource(R.string.nome_fixture_brand_tagline),
    )
    FixtureSearchField(
      label = stringResource(R.string.nome_fixture_p23_search),
      semanticsLabel = stringResource(R.string.nome_fixture_p23_search_semantics),
    )
    FixtureIdentityCard(
      title = stringResource(R.string.nome_fixture_p23_identity_title),
      supporting = stringResource(R.string.nome_fixture_p23_identity_body),
    )
    FixtureSectionLabel(R.string.nome_fixture_p23_section_account)
    FixtureSettingsRow(
      title = stringResource(R.string.nome_fixture_p23_privacy),
      body = stringResource(R.string.nome_fixture_p23_privacy_body),
      icon = Icons.Rounded.Security,
    )
    FixtureSettingsRow(
      title = stringResource(R.string.nome_fixture_p23_notifications),
      body = stringResource(R.string.nome_fixture_p23_notifications_body),
      icon = Icons.Rounded.Notifications,
    )
    FixtureSectionLabel(R.string.nome_fixture_p23_section_preferences)
    FixtureSettingsRow(
      title = stringResource(R.string.nome_fixture_p23_appearance),
      body = stringResource(
        if (theme == NomeFixtureTheme.DARK) {
          R.string.nome_fixture_p23_appearance_dark
        } else {
          R.string.nome_fixture_p23_appearance_light
        },
      ),
      icon = Icons.Rounded.Palette,
    )
    FixtureSettingsRow(
      title = stringResource(R.string.nome_fixture_p23_data),
      body = stringResource(R.string.nome_fixture_p23_data_body),
      icon = Icons.Rounded.Storage,
    )
    FixtureSettingsRow(
      title = stringResource(R.string.nome_fixture_p23_network),
      body = stringResource(R.string.nome_fixture_p23_network_body),
      icon = Icons.Rounded.Info,
    )
    FixtureBottomNavigation(selectedIndex = 2)
  }
}

@Composable
private fun FixtureScrollableColumn(
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = NomeTheme.dimensions.screenHorizontalInset, vertical = 12.dp),
    horizontalAlignment = horizontalAlignment,
    verticalArrangement = Arrangement.spacedBy(NomeTheme.dimensions.space12),
    content = content,
  )
}

@Composable
private fun FixtureTopBar(
  title: String,
  subtitle: String,
  showBack: Boolean = false,
  showMore: Boolean = false,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (showBack) {
      IconButton(onClick = {}) {
        Icon(
          Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.nome_fixture_back),
          tint = NomeTheme.colors.textPrimary,
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = NomeTheme.typography.title,
        color = NomeTheme.colors.textPrimary,
      )
      Text(
        text = subtitle,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
    }
    if (showMore) {
      IconButton(onClick = {}) {
        Icon(
          Icons.Rounded.MoreVert,
          contentDescription = stringResource(R.string.nome_fixture_more),
          tint = NomeTheme.colors.textPrimary,
        )
      }
    }
  }
}

@Composable
private fun FixtureSearchField(label: String, semanticsLabel: String) {
  NomeSurface(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clickable(role = Role.Button, onClick = {})
      .nomeTalkBackSemantics(label = semanticsLabel, role = Role.Button),
    shape = NomeTheme.shapes.pill,
    color = NomeTheme.colors.input,
    border = BorderStroke(1.dp, NomeTheme.colors.textTertiary),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        Icons.Rounded.Search,
        contentDescription = null,
        tint = NomeTheme.colors.textSecondary,
      )
      Text(
        text = label,
        style = NomeTheme.typography.body,
        color = NomeTheme.colors.textSecondary,
      )
    }
  }
}

@Composable
private fun FixtureIdentityCard(title: String, supporting: String) {
  NomeSurface(
    modifier = Modifier
      .fillMaxWidth()
      .nomeTalkBackSemantics(label = "$title, $supporting"),
    color = NomeTheme.colors.surfaceContainer,
    border = BorderStroke(1.dp, NomeTheme.colors.border),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(NomeTheme.colors.brandGreen, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.nome_fixture_p02_identity_initial),
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.brandNavy,
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = NomeTheme.typography.bodyStrong, color = NomeTheme.colors.textPrimary)
        Text(supporting, style = NomeTheme.typography.supporting, color = NomeTheme.colors.textSecondary)
      }
    }
  }
}

@Composable
private fun FixtureSectionLabel(@StringRes labelRes: Int) {
  Text(
    text = stringResource(labelRes),
    style = NomeTheme.typography.label,
    color = NomeTheme.colors.textSecondary,
  )
}

@Composable
private fun FixtureChatRow(title: String, body: String, meta: String, initial: String) {
  val spokenLabel = "$title, $body, $meta"
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clickable(role = Role.Button, onClick = {})
      .nomeTalkBackSemantics(label = spokenLabel, role = Role.Button)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .background(NomeTheme.colors.surfaceContainer, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Text(initial, style = NomeTheme.typography.bodyStrong, color = NomeTheme.colors.textPrimary)
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = NomeTheme.typography.bodyStrong, color = NomeTheme.colors.textPrimary)
      Text(body, style = NomeTheme.typography.supporting, color = NomeTheme.colors.textSecondary)
    }
    Text(meta, style = NomeTheme.typography.supporting, color = NomeTheme.colors.textTertiary)
  }
}

@Composable
private fun FixtureConnectionButton(title: String, body: String, icon: ImageVector) {
  NomeSurface(
    modifier = Modifier
      .fillMaxWidth()
      .nomeMinimumTouchTarget()
      .clickable(role = Role.Button, onClick = {})
      .nomeTalkBackSemantics(label = "$title, $body", role = Role.Button),
    color = NomeTheme.colors.surfaceContainer,
    border = BorderStroke(1.dp, NomeTheme.colors.textTertiary),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(icon, contentDescription = null, tint = NomeTheme.colors.action)
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = NomeTheme.typography.bodyStrong, color = NomeTheme.colors.textPrimary)
        Text(body, style = NomeTheme.typography.supporting, color = NomeTheme.colors.textSecondary)
      }
    }
  }
}

@Composable
private fun FixtureInfoStrip(text: String, icon: ImageVector) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.infoContainer,
    contentColor = NomeTheme.colors.onInfoContainer,
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(icon, contentDescription = null, tint = NomeTheme.colors.onInfoContainer)
      Text(
        text = text,
        modifier = Modifier.weight(1f),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.onInfoContainer,
      )
    }
  }
}

@Composable
private fun FixtureMessageBubble(text: String, supporting: String? = null, sent: Boolean) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (sent) Arrangement.End else Arrangement.Start,
  ) {
    val background = if (sent) NomeTheme.colors.sentMessage else NomeTheme.colors.receivedMessage
    val border = if (sent) NomeTheme.colors.sentMessageBorder else NomeTheme.colors.receivedMessageBorder
    NomeSurface(
      modifier = Modifier.widthIn(max = 300.dp),
      color = background,
      border = BorderStroke(1.dp, border),
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(text, style = NomeTheme.typography.body, color = NomeTheme.colors.textPrimary)
        supporting?.let {
          Text(it, style = NomeTheme.typography.supporting, color = NomeTheme.colors.textSecondary)
        }
      }
    }
  }
}

@Composable
private fun FixtureAttachmentRow(title: String, body: String, icon: ImageVector) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.surfaceContainer,
    border = BorderStroke(1.dp, NomeTheme.colors.border),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(icon, contentDescription = null, tint = NomeTheme.colors.info)
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = NomeTheme.typography.bodyStrong, color = NomeTheme.colors.textPrimary)
        Text(body, style = NomeTheme.typography.supporting, color = NomeTheme.colors.textSecondary)
      }
    }
  }
}

@Composable
private fun FixtureComposer() {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.pill,
    color = NomeTheme.colors.input,
    border = BorderStroke(1.dp, NomeTheme.colors.textTertiary),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = {}) {
        Icon(
          Icons.Rounded.Mic,
          contentDescription = stringResource(R.string.nome_fixture_microphone),
          tint = NomeTheme.colors.textSecondary,
        )
      }
      Text(
        text = stringResource(R.string.nome_fixture_p17_composer),
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .clickable(role = Role.Button, onClick = {})
          .nomeTalkBackSemantics(
            label = stringResource(R.string.nome_fixture_p17_composer_semantics),
            role = Role.Button,
          ),
        style = NomeTheme.typography.body,
        color = NomeTheme.colors.textSecondary,
      )
      IconButton(onClick = {}) {
        Icon(
          Icons.AutoMirrored.Rounded.Send,
          contentDescription = stringResource(R.string.nome_fixture_send),
          tint = NomeTheme.colors.action,
        )
      }
    }
  }
}

@Composable
private fun FixturePostCard(title: String, body: String) {
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.surfaceContainer,
    border = BorderStroke(1.dp, NomeTheme.colors.border),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Rounded.ChatBubbleOutline,
          contentDescription = null,
          tint = NomeTheme.colors.action,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = title,
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.textPrimary,
        )
      }
      Text(body, style = NomeTheme.typography.body, color = NomeTheme.colors.textSecondary)
    }
  }
}

@Composable
private fun FixtureSettingsRow(title: String, body: String, icon: ImageVector) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clickable(role = Role.Button, onClick = {})
      .nomeTalkBackSemantics(label = "$title, $body", role = Role.Button)
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .background(NomeTheme.colors.surfaceContainer, NomeTheme.shapes.control),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = NomeTheme.colors.action)
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = NomeTheme.typography.bodyStrong, color = NomeTheme.colors.textPrimary)
      Text(body, style = NomeTheme.typography.supporting, color = NomeTheme.colors.textSecondary)
    }
  }
}

@Composable
private fun FixtureBottomNavigation(selectedIndex: Int) {
  val items = listOf(
    Triple(R.string.nome_fixture_nav_chats, Icons.Rounded.ChatBubbleOutline, 0),
    Triple(R.string.nome_fixture_nav_contacts, Icons.Rounded.Contacts, 1),
    Triple(R.string.nome_fixture_nav_settings, Icons.Rounded.Settings, 2),
  )
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.grouped,
    color = NomeTheme.colors.surfaceContainer,
    border = BorderStroke(1.dp, NomeTheme.colors.border),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 76.dp)
        .padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      items.forEach { (labelRes, icon, index) ->
        val label = stringResource(labelRes)
        val selected = index == selectedIndex
        Column(
          modifier = Modifier
            .widthIn(min = 72.dp)
            .nomeMinimumTouchTarget()
            .selectable(
              selected = selected,
              role = Role.Tab,
              onClick = {},
            )
            .nomeTalkBackSemantics(label = label, role = Role.Tab),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Icon(
            icon,
            contentDescription = null,
            tint = if (selected) NomeTheme.colors.action else NomeTheme.colors.textSecondary,
          )
          Text(
            text = label,
            style = NomeTheme.typography.supporting,
            color = if (selected) NomeTheme.colors.textPrimary else NomeTheme.colors.textSecondary,
          )
        }
      }
    }
  }
}

@StringRes
private fun stateTitle(state: NomeFixtureState): Int = when (state) {
  NomeFixtureState.LOADING -> R.string.nome_fixture_state_loading_title
  NomeFixtureState.EMPTY -> R.string.nome_fixture_state_empty_title
  NomeFixtureState.OFFLINE -> R.string.nome_fixture_state_offline_title
  NomeFixtureState.ERROR -> R.string.nome_fixture_state_error_title
  NomeFixtureState.PERMISSION -> R.string.nome_fixture_state_permission_title
  NomeFixtureState.DANGER -> R.string.nome_fixture_state_danger_title
  NomeFixtureState.NORMAL -> R.string.nome_fixture_state_normal_title
}

@StringRes
private fun stateDescription(state: NomeFixtureState): Int = when (state) {
  NomeFixtureState.LOADING -> R.string.nome_fixture_state_loading_description
  NomeFixtureState.EMPTY -> R.string.nome_fixture_state_empty_description
  NomeFixtureState.OFFLINE -> R.string.nome_fixture_state_offline_description
  NomeFixtureState.ERROR -> R.string.nome_fixture_state_error_description
  NomeFixtureState.PERMISSION -> R.string.nome_fixture_state_permission_description
  NomeFixtureState.DANGER -> R.string.nome_fixture_state_danger_description
  NomeFixtureState.NORMAL -> R.string.nome_fixture_state_normal_description
}

@StringRes
private fun stateSemanticDescription(state: NomeFixtureState): Int = when (state) {
  NomeFixtureState.NORMAL -> R.string.nome_fixture_state_normal_semantics
  NomeFixtureState.LOADING -> R.string.nome_fixture_state_loading_semantics
  NomeFixtureState.EMPTY -> R.string.nome_fixture_state_empty_semantics
  NomeFixtureState.OFFLINE -> R.string.nome_fixture_state_offline_semantics
  NomeFixtureState.ERROR -> R.string.nome_fixture_state_error_semantics
  NomeFixtureState.PERMISSION -> R.string.nome_fixture_state_permission_semantics
  NomeFixtureState.DANGER -> R.string.nome_fixture_state_danger_semantics
}

@Composable
private fun pageTitle(page: NomeFixturePage): String = stringResource(
  when (page) {
    NomeFixturePage.P02 -> R.string.nome_fixture_p02_title
    NomeFixturePage.P07 -> R.string.nome_fixture_p07_title
    NomeFixturePage.P17 -> R.string.nome_fixture_p17_title
    NomeFixturePage.P21 -> R.string.nome_fixture_p21_title
    NomeFixturePage.P23 -> R.string.nome_fixture_p23_title
  },
)
