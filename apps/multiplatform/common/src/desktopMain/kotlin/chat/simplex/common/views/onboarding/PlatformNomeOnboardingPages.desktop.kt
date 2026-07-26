package chat.simplex.common.views.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.NotificationsMode
import chat.simplex.common.model.ServerOperator
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

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

  NomeDesktopOnboardingShell(
    step = 1,
    title = stringResource(MR.strings.nome_desktop_welcome_title),
    body = stringResource(MR.strings.nome_desktop_welcome_body),
    footer = {
      NomeSecondaryButton(
        text = stringResource(MR.strings.nome_desktop_welcome_migrate),
        onClick = onMigrate,
      )
      Spacer(Modifier.width(12.dp))
      NomePrimaryButton(
        text = stringResource(MR.strings.nome_desktop_welcome_create),
        onClick = onCreate,
      )
    },
  ) {
    Column(
      modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
    ) {
      NomeCommitmentRow(
        icon = painterResource(MR.images.ic_person),
        title = stringResource(MR.strings.nome_desktop_welcome_phone_title),
        body = stringResource(MR.strings.nome_desktop_welcome_phone_body),
      )
      Divider(color = NomeDesktopOnboardingColors.Divider)
      NomeCommitmentRow(
        icon = painterResource(MR.images.ic_database),
        title = stringResource(MR.strings.nome_desktop_welcome_device_title),
        body = stringResource(MR.strings.nome_desktop_welcome_device_body),
      )
      Divider(color = NomeDesktopOnboardingColors.Divider)
      NomeCommitmentRow(
        icon = painterResource(MR.images.ic_security),
        title = stringResource(MR.strings.nome_desktop_welcome_route_title),
        body = stringResource(MR.strings.nome_desktop_welcome_route_body),
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
  val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
  val displayNameDescription =
    stringResource(MR.strings.nome_desktop_identity_display_name)

  NomeDesktopOnboardingShell(
    step = 2,
    title = stringResource(MR.strings.nome_desktop_identity_title),
    body = stringResource(MR.strings.nome_desktop_identity_body),
    onBack = onBack,
    footer = {
      NomePrimaryButton(
        text = stringResource(
          if (creating) {
            MR.strings.nome_desktop_identity_creating
          } else {
            MR.strings.nome_desktop_identity_create
          }
        ),
        onClick = onCreate,
        enabled = createEnabled && !creating,
        loading = creating,
      )
    },
  ) {
    NomeSectionLabel(stringResource(MR.strings.nome_desktop_identity_preview))
    Spacer(Modifier.height(9.dp))
    NomeIdentityPreview(displayName.value)
    Spacer(Modifier.height(28.dp))
    NomeSectionLabel(stringResource(MR.strings.nome_desktop_identity_display_name))
    Spacer(Modifier.height(9.dp))
    OutlinedTextField(
      value = displayName.value,
      onValueChange = { displayName.value = it },
      modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .semantics {
          contentDescription = displayNameDescription
        },
      enabled = !creating,
      singleLine = true,
      placeholder = {
        Text(
          text = stringResource(MR.strings.nome_desktop_identity_placeholder),
          color = NomeDesktopOnboardingColors.TextMuted,
        )
      },
      leadingIcon = {
        Icon(
          painter = painterResource(MR.images.ic_person),
          contentDescription = null,
          tint = NomeDesktopOnboardingColors.TextMuted,
        )
      },
      shape = RoundedCornerShape(12.dp),
      colors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = NomeDesktopOnboardingColors.Text,
        cursorColor = NomeDesktopOnboardingColors.Action,
        focusedBorderColor = NomeDesktopOnboardingColors.Action,
        unfocusedBorderColor = NomeDesktopOnboardingColors.Border,
        backgroundColor = NomeDesktopOnboardingColors.Surface,
      ),
      keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = androidx.compose.foundation.text.KeyboardActions(
        onDone = {
          if (createEnabled && !creating) onCreate()
        }
      ),
    )
    Text(
      text = stringResource(MR.strings.nome_desktop_identity_hint),
      color = NomeDesktopOnboardingColors.TextMuted,
      fontSize = 12.sp,
      modifier = Modifier.padding(top = 7.dp),
    )
    Spacer(Modifier.height(20.dp))
    NomeInlineNotice(
      icon = painterResource(MR.images.ic_lock),
      title = stringResource(MR.strings.nome_desktop_identity_local_title),
      body = stringResource(MR.strings.nome_desktop_identity_local_body),
    )
  }

  LaunchedEffect(Unit) {
    delay(160)
    focusRequester.requestFocus()
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
  val selectedOperators =
    serverOperators.value.filter { selectedOperatorIds.value.contains(it.operatorId) }
  val operatorTitle =
    selectedOperators.joinToString(", ") { it.tradeName }
      .ifEmpty { stringResource(MR.strings.nome_desktop_network_none) }
  val notificationText =
    stringResource(
      when (notificationMode.value) {
        NotificationsMode.SERVICE -> MR.strings.nome_desktop_network_notifications_service
        NotificationsMode.PERIODIC -> MR.strings.nome_desktop_network_notifications_periodic
        NotificationsMode.OFF -> MR.strings.nome_desktop_network_notifications_off
      }
    )

  NomeDesktopOnboardingShell(
    step = 3,
    title = stringResource(MR.strings.nome_desktop_network_title),
    body = stringResource(MR.strings.nome_desktop_network_body),
    footer = {
      NomePrimaryButton(
        text = stringResource(MR.strings.nome_desktop_network_continue),
        onClick = onContinue,
        enabled = selectedOperators.isNotEmpty(),
      )
    },
  ) {
    NomeSectionLabel(stringResource(MR.strings.nome_desktop_network_operators))
    Spacer(Modifier.height(9.dp))
    NomeSettingsCard(
      icon = painterResource(MR.images.ic_dns),
      title = operatorTitle,
      body = stringResource(MR.strings.nome_desktop_network_selected, selectedOperators.size),
      action = stringResource(MR.strings.nome_desktop_network_configure),
      onClick = onConfigureOperators,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        selectedOperators.take(4).forEach { operator ->
          Surface(
            shape = CircleShape,
            color = NomeDesktopOnboardingColors.SurfaceSubtle,
            modifier = Modifier.size(30.dp),
          ) {
            Image(
              painter = painterResource(operator.logo),
              contentDescription = operator.tradeName,
              modifier = Modifier.padding(5.dp),
            )
          }
        }
      }
    }
    Spacer(Modifier.height(22.dp))
    NomeSectionLabel(stringResource(MR.strings.nome_desktop_network_notifications))
    Spacer(Modifier.height(9.dp))
    NomeSettingsCard(
      icon = painterResource(
        when (notificationMode.value) {
          NotificationsMode.SERVICE -> MR.images.ic_bolt
          NotificationsMode.PERIODIC -> MR.images.ic_timer
          NotificationsMode.OFF -> MR.images.ic_bolt_off
        }
      ),
      title = stringResource(MR.strings.nome_desktop_network_notifications),
      body = notificationText,
      action = stringResource(MR.strings.nome_desktop_network_configure_notifications),
      onClick = onConfigureNotifications,
    )
    Spacer(Modifier.height(20.dp))
    NomeInlineNotice(
      icon = painterResource(MR.images.ic_info),
      body = stringResource(MR.strings.nome_desktop_network_scope),
    )
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

  NomeDesktopOnboardingShell(
    step = 4,
    title = stringResource(MR.strings.nome_desktop_commitment_title),
    body = stringResource(MR.strings.nome_desktop_commitment_body),
    onBack = onBack,
    footer = {
      NomePrimaryButton(
        text = stringResource(MR.strings.nome_desktop_commitment_accept),
        onClick = onAccept,
        enabled = acceptEnabled && consentChecked,
      )
    },
  ) {
    Surface(
      color = NomeDesktopOnboardingColors.Surface,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, NomeDesktopOnboardingColors.Border),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
        NomeCommitmentRow(
          icon = painterResource(MR.images.ic_shield),
          title = stringResource(MR.strings.nome_desktop_commitment_privacy_title),
          body = stringResource(MR.strings.nome_desktop_commitment_privacy_body),
        )
        Divider(color = NomeDesktopOnboardingColors.Divider)
        NomeCommitmentRow(
          icon = painterResource(MR.images.ic_person),
          title = stringResource(MR.strings.nome_desktop_commitment_connect_title),
          body = stringResource(MR.strings.nome_desktop_commitment_connect_body),
        )
        Divider(color = NomeDesktopOnboardingColors.Divider)
        NomeCommitmentRow(
          icon = painterResource(MR.images.ic_database),
          title = stringResource(MR.strings.nome_desktop_commitment_data_title),
          body = stringResource(MR.strings.nome_desktop_commitment_data_body),
        )
      }
    }
    Spacer(Modifier.height(18.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .toggleable(
          value = consentChecked,
          role = Role.Checkbox,
          onValueChange = { consentChecked = it },
        )
        .padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = consentChecked,
        onCheckedChange = null,
        colors = CheckboxDefaults.colors(
          checkedColor = NomeDesktopOnboardingColors.Action,
          uncheckedColor = NomeDesktopOnboardingColors.TextMuted,
          checkmarkColor = Color.White,
        ),
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = stringResource(MR.strings.nome_desktop_commitment_consent),
        color = NomeDesktopOnboardingColors.Text,
        fontSize = 14.sp,
      )
    }
  }
}

@Composable
private fun NomeDesktopOnboardingShell(
  step: Int,
  title: String,
  body: String,
  onBack: (() -> Unit)? = null,
  footer: @Composable RowScope.() -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  val backDescription = stringResource(MR.strings.nome_desktop_back)
  Row(
    modifier = Modifier
      .fillMaxSize()
      .background(NomeDesktopOnboardingColors.Background),
  ) {
    Column(Modifier.weight(1f).fillMaxHeight()) {
      Row(
        modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (onBack != null) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.semantics {
              contentDescription = backDescription
            },
          ) {
            Icon(
              painter = painterResource(MR.images.ic_arrow_back_ios_new),
              contentDescription = stringResource(MR.strings.nome_desktop_back),
              tint = NomeDesktopOnboardingColors.Text,
              modifier = Modifier.size(19.dp),
            )
          }
        }
        Image(
          painter = painterResource(MR.images.nome_mark),
          contentDescription = "Nome",
          modifier = Modifier.size(26.dp),
        )
        Text(
          text = "Nome",
          color = NomeDesktopOnboardingColors.Text,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
          text = stringResource(MR.strings.nome_desktop_onboarding_step, step),
          color = NomeDesktopOnboardingColors.TextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.8.sp,
        )
      }
      Divider(color = NomeDesktopOnboardingColors.Divider)
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 44.dp),
        ) {
          Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = NomeDesktopOnboardingColors.Text,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.7).sp,
          )
          Text(
            text = body,
            color = NomeDesktopOnboardingColors.TextMuted,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            modifier = Modifier.padding(top = 12.dp).widthIn(max = 650.dp),
          )
          Spacer(Modifier.height(34.dp))
          content()
        }
      }
      Divider(color = NomeDesktopOnboardingColors.Divider)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(NomeDesktopOnboardingColors.Surface)
          .padding(horizontal = 34.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        content = footer,
      )
    }
  }
}

@Composable
private fun NomeIdentityPreview(displayName: String) {
  Surface(
    color = NomeDesktopOnboardingColors.Surface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(
      1.dp,
      if (displayName.isBlank()) {
        NomeDesktopOnboardingColors.Border
      } else {
        NomeDesktopOnboardingColors.Action
      },
    ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier = Modifier.size(62.dp),
        shape = CircleShape,
        color = NomeDesktopOnboardingColors.MintPale,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = displayName.trim().take(1).uppercase().ifEmpty { "N" },
            color = NomeDesktopOnboardingColors.Action,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }
      Spacer(Modifier.width(15.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text =
            displayName.trim().ifEmpty {
              stringResource(MR.strings.nome_desktop_identity_placeholder)
            },
          color =
            if (displayName.isBlank()) {
              NomeDesktopOnboardingColors.TextMuted
            } else {
              NomeDesktopOnboardingColors.Text
            },
          fontSize = 16.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = stringResource(MR.strings.nome_desktop_identity_local_title),
          color = NomeDesktopOnboardingColors.TextMuted,
          fontSize = 12.sp,
          modifier = Modifier.padding(top = 3.dp),
        )
      }
      if (displayName.isNotBlank()) {
        Icon(
          painter = painterResource(MR.images.ic_check_circle_filled),
          contentDescription = null,
          tint = NomeDesktopOnboardingColors.Action,
          modifier = Modifier.size(23.dp),
        )
      }
    }
  }
}

@Composable
private fun NomeSettingsCard(
  icon: Painter,
  title: String,
  body: String,
  action: String,
  onClick: () -> Unit,
  trailing: @Composable (() -> Unit)? = null,
) {
  Surface(
    color = NomeDesktopOnboardingColors.Surface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, NomeDesktopOnboardingColors.Border),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .semantics {
        contentDescription = "$title. $action"
      },
  ) {
    Row(
      modifier = Modifier.padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NomeIconTile(icon)
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text = title,
          color = NomeDesktopOnboardingColors.Text,
          fontSize = 15.sp,
          lineHeight = 20.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = body,
          color = NomeDesktopOnboardingColors.TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp,
          modifier = Modifier.padding(top = 4.dp),
        )
        Text(
          text = action,
          color = NomeDesktopOnboardingColors.Action,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      trailing?.let {
        Spacer(Modifier.width(14.dp))
        it()
      }
    }
  }
}

@Composable
private fun NomeCommitmentRow(
  icon: Painter,
  title: String,
  body: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    NomeIconTile(icon)
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
      Text(
        text = title,
        color = NomeDesktopOnboardingColors.Text,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = body,
        color = NomeDesktopOnboardingColors.TextMuted,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(top = 3.dp),
      )
    }
  }
}

@Composable
private fun NomeInlineNotice(
  icon: Painter,
  title: String? = null,
  body: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(NomeDesktopOnboardingColors.MintPale, RoundedCornerShape(12.dp))
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = icon,
      contentDescription = null,
      tint = NomeDesktopOnboardingColors.Action,
      modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(11.dp))
    Column {
      title?.let {
        Text(
          text = it,
          color = NomeDesktopOnboardingColors.Text,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Text(
        text = body,
        color = NomeDesktopOnboardingColors.TextMuted,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(top = if (title == null) 0.dp else 2.dp),
      )
    }
  }
}

@Composable
private fun NomeIconTile(icon: Painter) {
  Surface(
    modifier = Modifier.size(40.dp),
    shape = RoundedCornerShape(11.dp),
    color = NomeDesktopOnboardingColors.MintPale,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        painter = icon,
        contentDescription = null,
        tint = NomeDesktopOnboardingColors.Action,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun NomeSectionLabel(text: String) {
  Text(
    text = text,
    color = NomeDesktopOnboardingColors.TextMuted,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.8.sp,
  )
}

@Composable
private fun NomePrimaryButton(
  text: String,
  onClick: () -> Unit,
  enabled: Boolean = true,
  loading: Boolean = false,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier
      .height(46.dp)
      .widthIn(min = 190.dp)
      .semantics {
        contentDescription = text
      },
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      backgroundColor = NomeDesktopOnboardingColors.Action,
      contentColor = Color.White,
      disabledBackgroundColor = NomeDesktopOnboardingColors.Disabled,
      disabledContentColor = Color.White.copy(alpha = 0.8f),
    ),
    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
    contentPadding = PaddingValues(horizontal = 22.dp),
  ) {
    if (loading) {
      CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        color = Color.White,
        strokeWidth = 2.dp,
      )
      Spacer(Modifier.width(9.dp))
    }
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun NomeSecondaryButton(
  text: String,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    modifier = Modifier.height(46.dp).semantics {
      contentDescription = text
    },
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, NomeDesktopOnboardingColors.Border),
    colors = ButtonDefaults.buttonColors(
      backgroundColor = NomeDesktopOnboardingColors.Surface,
      contentColor = NomeDesktopOnboardingColors.Text,
    ),
    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
    contentPadding = PaddingValues(horizontal = 20.dp),
  ) {
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
  }
}

private object NomeDesktopOnboardingColors {
  val MintPale = Color(0xFFE8F6F0)
  val Action = Color(0xFF0A874D)
  val Background = Color(0xFFF5F7FA)
  val Surface = Color(0xFFFFFFFF)
  val SurfaceSubtle = Color(0xFFF2F5F7)
  val Text = Color(0xFF0E1B2D)
  val TextMuted = Color(0xFF667085)
  val Border = Color(0xFFDCE3E9)
  val Divider = Color(0xFFE7ECF1)
  val Disabled = Color(0xFF9FB4AA)
}
