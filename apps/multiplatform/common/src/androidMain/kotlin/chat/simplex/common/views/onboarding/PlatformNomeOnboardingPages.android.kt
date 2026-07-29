package chat.simplex.common.views.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.R
import chat.simplex.common.model.NotificationsMode
import chat.simplex.common.model.ServerOperator
import chat.simplex.common.ui.nome.components.NomeBrandLockup
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeTheme

@Composable
internal actual fun PlatformNomeWelcomePage(
  enabled: Boolean,
  onCreate: () -> Unit,
  onMigrate: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  if (!enabled) {
    legacyContent()
    return
  }

  NomeOnboardingScaffold(
    footer = {
      NomeButton(
        text = stringResource(R.string.nome_p03_create),
        onClick = onCreate,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        semanticsLabel = stringResource(R.string.nome_p03_create),
        shape = NomeTheme.shapes.pill,
        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
      )
      Spacer(Modifier.height(8.dp))
      NomeButton(
        text = stringResource(R.string.nome_p03_migrate),
        onClick = onMigrate,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        variant = NomeButtonVariant.SECONDARY,
        semanticsLabel = stringResource(R.string.nome_p03_migrate),
        shape = NomeTheme.shapes.pill,
        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null) },
      )
      Text(
        text = stringResource(R.string.nome_p03_language),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.accent,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(42.dp))
    },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = NomeTheme.dimensions.screenHorizontalInset),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(46.dp))
      NomeBrandLogo()
      Spacer(Modifier.height(20.dp))
      Text(
        text = stringResource(R.string.nome_p03_title),
        modifier = Modifier.semantics { heading() },
        style = NomeTheme.typography.display,
        color = NomeTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.nome_p03_body),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(18.dp))
      NomeInformationList(
        rows = listOf(
          NomeInformationRow(
            icon = Icons.Rounded.Security,
            title = stringResource(R.string.nome_p03_point_phone_title),
            body = stringResource(R.string.nome_p03_point_phone_body),
          ),
          NomeInformationRow(
            icon = Icons.Rounded.Storage,
            title = stringResource(R.string.nome_p03_point_device_title),
            body = stringResource(R.string.nome_p03_point_device_body),
          ),
          NomeInformationRow(
            icon = Icons.Rounded.Contacts,
            title = stringResource(R.string.nome_p03_point_confirm_title),
            body = stringResource(R.string.nome_p03_point_confirm_body),
          ),
        ),
      )
    }
  }
}

@Composable
internal actual fun PlatformNomeCreateIdentityPage(
  displayName: MutableState<String>,
  createEnabled: Boolean,
  creating: Boolean,
  onBack: () -> Unit,
  onCreate: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  NomeOnboardingScaffold(
    title = stringResource(R.string.nome_p04_title),
    onBack = onBack,
    footer = {
      NomeButton(
        text = stringResource(if (creating) R.string.nome_p04_creating else R.string.nome_p04_create),
        onClick = onCreate,
        modifier = Modifier.fillMaxWidth(),
        enabled = createEnabled && !creating,
        semanticsLabel = stringResource(if (creating) R.string.nome_p04_creating else R.string.nome_p04_create),
        shape = NomeTheme.shapes.pill,
        leadingIcon = { Icon(Icons.Rounded.PersonOutline, contentDescription = null) },
      )
    },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = NomeTheme.dimensions.screenHorizontalInset),
    ) {
      Text(
        text = stringResource(R.string.nome_p04_intro),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
      Spacer(Modifier.height(16.dp))
      NomeSectionLabel(stringResource(R.string.nome_p04_preview))
      Spacer(Modifier.height(8.dp))
      NomeSurface(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, if (createEnabled) NomeTheme.colors.success else NomeTheme.colors.border),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            modifier = Modifier.size(76.dp),
            shape = CircleShape,
            color = NomeTheme.colors.surfaceSubtle,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = displayName.value.trim().take(1).uppercase().ifEmpty { "N" },
                style = NomeTheme.typography.titleLarge,
                color = NomeTheme.colors.action,
              )
            }
          }
          Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) {
            Text(
              text = displayName.value.trim().ifEmpty {
                stringResource(R.string.nome_p04_display_name_placeholder)
              },
              style = NomeTheme.typography.bodyStrong,
              color = if (displayName.value.isBlank()) NomeTheme.colors.textSecondary else NomeTheme.colors.textPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = stringResource(R.string.nome_p04_preview_supporting),
              style = NomeTheme.typography.supporting,
              color = NomeTheme.colors.textSecondary,
            )
          }
          if (createEnabled) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = NomeTheme.colors.success,
            )
          }
        }
      }
      Spacer(Modifier.height(16.dp))
      NomeSectionLabel(stringResource(R.string.nome_p04_display_name))
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = displayName.value,
        onValueChange = { displayName.value = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !creating,
        placeholder = {
          Text(
            text = stringResource(R.string.nome_p04_display_name_placeholder),
            color = NomeTheme.colors.textSecondary,
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.PersonOutline,
            contentDescription = null,
            tint = NomeTheme.colors.textSecondary,
          )
        },
        shape = NomeTheme.shapes.control,
        colors = TextFieldDefaults.outlinedTextFieldColors(
          textColor = NomeTheme.colors.textPrimary,
          cursorColor = NomeTheme.colors.action,
          focusedBorderColor = NomeTheme.colors.action,
          unfocusedBorderColor = NomeTheme.colors.border,
          backgroundColor = NomeTheme.colors.input,
        ),
      )
      Text(
        text = stringResource(R.string.nome_p04_hint),
        modifier = Modifier.padding(top = 6.dp),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
      Spacer(Modifier.height(18.dp))
      NomePlainInformationRow(
        icon = Icons.Rounded.Storage,
        title = stringResource(R.string.nome_p04_local_title),
        body = stringResource(R.string.nome_p04_local_body),
      )
    }
  }
}

@Composable
internal actual fun PlatformNomeNetworkPage(
  serverOperators: State<List<ServerOperator>>,
  selectedOperatorIds: MutableState<Set<Long>>,
  notificationMode: MutableState<NotificationsMode>,
  onConfigureOperators: () -> Unit,
  onConfigureNotifications: () -> Unit,
  onContinue: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val notificationBody = stringResource(
    when (notificationMode.value) {
      NotificationsMode.SERVICE -> R.string.nome_p05_notifications_service
      NotificationsMode.PERIODIC -> R.string.nome_p05_notifications_periodic
      NotificationsMode.OFF -> R.string.nome_p05_notifications_off
    }
  )

  NomeOnboardingScaffold(
    title = stringResource(R.string.nome_p05_title),
    footer = {
      NomeButton(
        text = stringResource(R.string.nome_p05_continue),
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
        semanticsLabel = stringResource(R.string.nome_p05_continue),
        shape = NomeTheme.shapes.pill,
      )
    },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = NomeTheme.dimensions.screenHorizontalInset),
    ) {
      Text(
        text = stringResource(R.string.nome_p05_intro),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
      Spacer(Modifier.height(14.dp))
      NomeSurface(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, NomeTheme.colors.border),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 19.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          NomeIconTile(Icons.Rounded.Security)
          Spacer(Modifier.width(12.dp))
          Column(Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.nome_p05_official_servers),
              style = NomeTheme.typography.bodyStrong,
              color = NomeTheme.colors.textPrimary,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = stringResource(R.string.nome_p05_official_servers_body),
              style = NomeTheme.typography.supporting,
              color = NomeTheme.colors.textSecondary,
            )
          }
          Box(
            modifier = Modifier
              .size(10.dp)
              .background(
                NomeTheme.colors.success,
                CircleShape,
              ),
          )
        }
      }
      Spacer(Modifier.height(10.dp))
      NomeInfoStrip(
        icon = Icons.Rounded.Info,
        text = stringResource(R.string.nome_p05_scope_note),
      )
      Spacer(Modifier.height(18.dp))
      NomeSectionLabel(stringResource(R.string.nome_p05_preferences))
      Spacer(Modifier.height(6.dp))
      Column {
        NomeSettingsRow(
          icon = Icons.Rounded.Notifications,
          title = stringResource(R.string.nome_p05_notifications),
          body = notificationBody,
          onClick = onConfigureNotifications,
        )
        Divider(color = NomeTheme.colors.divider)
        NomeSettingsRow(
          icon = Icons.Rounded.Settings,
          title = stringResource(R.string.nome_p05_advanced),
          body = stringResource(R.string.nome_p05_advanced_body),
        )
      }
    }
  }
}

@Composable
internal actual fun PlatformNomeCommitmentPage(
  acceptEnabled: Boolean,
  onBack: () -> Unit,
  onViewTerms: () -> Unit,
  onAccept: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  var consentChecked by rememberSaveable { mutableStateOf(false) }
  val canAccept = acceptEnabled && consentChecked

  NomeOnboardingScaffold(
    title = stringResource(R.string.nome_p06_title),
    onBack = onBack,
    footer = {
      NomeButton(
        text = stringResource(R.string.nome_p06_accept),
        onClick = onAccept,
        modifier = Modifier.fillMaxWidth(),
        enabled = canAccept,
        semanticsLabel = stringResource(R.string.nome_p06_accept),
        leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
      )
    },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = NomeTheme.dimensions.screenHorizontalInset),
    ) {
      Text(
        text = stringResource(R.string.nome_p06_intro),
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
      )
      Spacer(Modifier.height(14.dp))
      NomeSurface(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, NomeTheme.colors.border),
      ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
          NomeCommitmentRow(
            icon = Icons.Rounded.Security,
            title = stringResource(R.string.nome_p06_private_title),
            body = stringResource(R.string.nome_p06_private_body),
          )
          Divider(color = NomeTheme.colors.divider)
          NomeCommitmentRow(
            icon = Icons.Rounded.Contacts,
            title = stringResource(R.string.nome_p06_connection_title),
            body = stringResource(R.string.nome_p06_connection_body),
          )
          Divider(color = NomeTheme.colors.divider)
          NomeCommitmentRow(
            icon = Icons.Rounded.Info,
            title = stringResource(R.string.nome_p06_data_title),
            body = stringResource(R.string.nome_p06_data_body),
          )
        }
      }
      Spacer(Modifier.height(16.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .toggleable(
            value = consentChecked,
            role = Role.Checkbox,
            onValueChange = { consentChecked = it },
          )
          .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(
          checked = consentChecked,
          onCheckedChange = null,
          colors = CheckboxDefaults.colors(
            checkedColor = NomeTheme.colors.action,
            uncheckedColor = NomeTheme.colors.textSecondary,
            checkmarkColor = NomeTheme.colors.onAction,
          ),
        )
        Spacer(Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.nome_p06_consent),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textPrimary,
        )
      }
      TextButton(
        onClick = onViewTerms,
        modifier = Modifier.align(Alignment.CenterHorizontally),
      ) {
        Text(
          text = stringResource(R.string.nome_p06_terms),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.accent,
        )
      }
    }
  }
}

@Composable
private fun NomeOnboardingScaffold(
  title: String? = null,
  onBack: (() -> Unit)? = null,
  footer: @Composable ColumnScope.() -> Unit,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(NomeTheme.colors.background)
      .windowInsetsPadding(WindowInsets.statusBars),
  ) {
    title?.let {
      NomeTopBar(title = it, onBack = onBack)
    }
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
    ) {
      content()
      Spacer(Modifier.height(20.dp))
    }
    Divider(color = NomeTheme.colors.divider)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(NomeTheme.colors.background)
        .padding(horizontal = NomeTheme.dimensions.screenHorizontalInset, vertical = 10.dp)
        .windowInsetsPadding(WindowInsets.navigationBars),
      content = footer,
    )
  }
}

@Composable
private fun NomeTopBar(title: String, onBack: (() -> Unit)?) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (onBack != null) {
      IconButton(
        onClick = onBack,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.nome_back),
          tint = NomeTheme.colors.textPrimary,
        )
      }
    } else {
      Spacer(Modifier.width(8.dp))
    }
    Text(
      text = title,
      modifier = Modifier
        .padding(start = if (onBack == null) 4.dp else 6.dp)
        .semantics { heading() },
      style = NomeTheme.typography.title,
      color = NomeTheme.colors.textPrimary,
    )
  }
  Divider(color = NomeTheme.colors.divider)
}

@Composable
private fun NomeBrandLogo() {
  NomeBrandLockup(
    contentDescription = stringResource(R.string.nome_brand_logo_description),
    modifier = Modifier.width(104.dp),
  )
}

private data class NomeInformationRow(
  val icon: ImageVector,
  val title: String,
  val body: String,
)

@Composable
private fun NomeInformationList(rows: List<NomeInformationRow>) {
  Column(Modifier.fillMaxWidth()) {
    rows.forEachIndexed { index, row ->
      NomePlainInformationRow(row.icon, row.title, row.body)
      if (index != rows.lastIndex) {
        Divider(
          modifier = Modifier.padding(start = 52.dp),
          color = NomeTheme.colors.divider,
        )
      }
    }
  }
}

@Composable
private fun NomePlainInformationRow(
  icon: ImageVector,
  title: String,
  body: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NomeIconTile(icon)
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
private fun NomeIconTile(icon: ImageVector) {
  Surface(
    modifier = Modifier.size(40.dp),
    shape = NomeTheme.shapes.compact,
    color = NomeTheme.colors.successContainer,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
        tint = NomeTheme.colors.action,
      )
    }
  }
}

@Composable
private fun NomeSectionLabel(text: String) {
  Text(
    text = text,
    style = NomeTheme.typography.supporting,
    color = NomeTheme.colors.textSecondary,
  )
}

@Composable
private fun NomeInfoStrip(
  icon: ImageVector,
  text: String,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = NomeTheme.shapes.compact,
    color = NomeTheme.colors.successContainer,
    border = BorderStroke(1.dp, NomeTheme.colors.sentMessageBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = NomeTheme.colors.onSuccessContainer,
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = text,
        style = NomeTheme.typography.supporting.copy(
          fontSize = 12.sp,
          lineHeight = 17.sp,
        ),
        color = NomeTheme.colors.onSuccessContainer,
      )
    }
  }
}

@Composable
private fun NomeSettingsRow(
  icon: ImageVector,
  title: String,
  body: String,
  onClick: (() -> Unit)? = null,
) {
  val clickModifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
  Row(
    modifier = clickModifier
      .fillMaxWidth()
      .padding(vertical = 13.dp, horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NomeIconTile(icon)
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
    if (onClick != null) {
      Icon(
        imageVector = Icons.Rounded.ArrowForward,
        contentDescription = null,
        tint = NomeTheme.colors.textSecondary,
      )
    }
  }
}

@Composable
private fun NomeCommitmentRow(
  icon: ImageVector,
  title: String,
  body: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 19.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(26.dp),
      tint = NomeTheme.colors.action,
    )
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
