package chat.simplex.common.views.newchat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.CreatedConnLink
import chat.simplex.common.platform.shareText
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

internal enum class DesktopInvitationPhase {
  Creating,
  Retry,
  Ready,
}

internal fun desktopInvitationPhase(
  fullLink: String,
  creating: Boolean,
): DesktopInvitationPhase =
  when {
    fullLink.isNotEmpty() -> DesktopInvitationPhase.Ready
    creating -> DesktopInvitationPhase.Creating
    else -> DesktopInvitationPhase.Retry
  }

internal fun desktopConnectionSubmitEnabled(link: String): Boolean =
  link.isNotBlank()

@Suppress("UNUSED_PARAMETER")
@Composable
internal actual fun PlatformNewChatRoute(
  selection: MutableState<NewChatOption>,
  invitation: CreatedConnLink,
  invitationCreating: Boolean,
  currentProfileName: String,
  hostDeviceName: String?,
  hostDeviceIsRemote: Boolean,
  onOpenProfile: (() -> Unit)?,
  pastedLink: MutableState<String>,
  showQRCodeScanner: MutableState<Boolean>,
  onRetryInvitation: () -> Unit,
  onInvitationLocalAction: () -> Unit,
  onSubmitPastedLink: (String) -> Unit,
  onScannedLink: suspend (String) -> Boolean,
  onClose: () -> Unit,
) {
  LaunchedEffect(Unit) {
    // Desktop has no camera scanner implementation. Never leave a stale scanner request active.
    showQRCodeScanner.value = false
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(NomeDesktopNewChatColors.Background),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 920.dp)
          .align(Alignment.TopCenter)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 44.dp, vertical = 30.dp),
    ) {
      NomeDesktopRouteHeader(
        hostDeviceName = hostDeviceName,
        hostDeviceIsRemote = hostDeviceIsRemote,
        onClose = onClose,
      )
      NomeDesktopRouteTabs(
        selection = selection.value,
        onSelect = { selection.value = it },
      )
      Spacer(Modifier.height(24.dp))

      when (selection.value) {
        NewChatOption.INVITE ->
          NomeDesktopInvitationPage(
            invitation = invitation,
            invitationCreating = invitationCreating,
            currentProfileName = currentProfileName,
            onOpenProfile = onOpenProfile,
            onRetryInvitation = onRetryInvitation,
            onInvitationLocalAction = onInvitationLocalAction,
          )
        NewChatOption.CONNECT ->
          NomeDesktopPasteInvitationPage(
            pastedLink = pastedLink,
            onSubmitPastedLink = onSubmitPastedLink,
          )
      }
    }
  }
}

@Composable
private fun NomeDesktopRouteHeader(
  hostDeviceName: String?,
  hostDeviceIsRemote: Boolean,
  onClose: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(MR.strings.nome_desktop_back),
      color = NomeDesktopNewChatColors.Action,
      fontSize = 13.sp,
      fontWeight = FontWeight.SemiBold,
      modifier =
        Modifier
          .heightIn(min = 44.dp)
          .clickable(role = Role.Button, onClick = onClose)
          .padding(horizontal = 10.dp, vertical = 8.dp),
    )
    Spacer(Modifier.weight(1f))
    Column(horizontalAlignment = Alignment.End) {
      Text(
        text = stringResource(MR.strings.nome_desktop_new_connection_eyebrow),
        color = NomeDesktopNewChatColors.Action,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
      )
      if (hostDeviceName != null) {
        Row(
          modifier = Modifier.padding(top = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            painter =
              painterResource(
                if (hostDeviceIsRemote) {
                  MR.images.ic_smartphone_300
                } else {
                  MR.images.ic_desktop
                },
              ),
            contentDescription = null,
            tint = NomeDesktopNewChatColors.TextMuted,
            modifier = Modifier.size(14.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = hostDeviceName,
            color = NomeDesktopNewChatColors.TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun NomeDesktopRouteTabs(
  selection: NewChatOption,
  onSelect: (NewChatOption) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    color = NomeDesktopNewChatColors.SurfaceSubtle,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, NomeDesktopNewChatColors.Border),
    elevation = 0.dp,
  ) {
    Row(Modifier.padding(4.dp)) {
      NomeDesktopRouteTab(
        text = stringResource(MR.strings.share_one_time_link),
        icon = MR.images.ic_add_link,
        selected = selection == NewChatOption.INVITE,
        modifier = Modifier.weight(1f),
        onClick = { onSelect(NewChatOption.INVITE) },
      )
      Spacer(Modifier.width(4.dp))
      NomeDesktopRouteTab(
        text = stringResource(MR.strings.connect_via_link),
        icon = MR.images.ic_content_paste,
        selected = selection == NewChatOption.CONNECT,
        modifier = Modifier.weight(1f),
        onClick = { onSelect(NewChatOption.CONNECT) },
      )
    }
  }
}

@Composable
private fun NomeDesktopRouteTab(
  text: String,
  icon: dev.icerock.moko.resources.ImageResource,
  selected: Boolean,
  modifier: Modifier,
  onClick: () -> Unit,
) {
  Surface(
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .clickable(role = Role.Tab, onClick = onClick)
        .semantics { this.selected = selected },
    color =
      if (selected) {
        NomeDesktopNewChatColors.Surface
      } else {
        Color.Transparent
      },
    shape = RoundedCornerShape(10.dp),
    elevation = if (selected) 1.dp else 0.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint =
          if (selected) {
            NomeDesktopNewChatColors.Action
          } else {
            NomeDesktopNewChatColors.TextMuted
          },
        modifier = Modifier.size(19.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = text,
        color =
          if (selected) {
            NomeDesktopNewChatColors.Text
          } else {
            NomeDesktopNewChatColors.TextMuted
          },
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun NomeDesktopInvitationPage(
  invitation: CreatedConnLink,
  invitationCreating: Boolean,
  currentProfileName: String,
  onOpenProfile: (() -> Unit)?,
  onRetryInvitation: () -> Unit,
  onInvitationLocalAction: () -> Unit,
) {
  Text(
    text = stringResource(MR.strings.share_one_time_link),
    color = NomeDesktopNewChatColors.Text,
    fontSize = 30.sp,
    lineHeight = 37.sp,
    fontWeight = FontWeight.SemiBold,
    modifier = Modifier.semantics { heading() },
  )
  Text(
    text = stringResource(MR.strings.onboarding_send_1_time_link),
    color = NomeDesktopNewChatColors.TextMuted,
    fontSize = 14.sp,
    lineHeight = 21.sp,
    modifier = Modifier.padding(top = 8.dp),
  )
  Spacer(Modifier.height(24.dp))

  when (
    desktopInvitationPhase(
      fullLink = invitation.connFullLink,
      creating = invitationCreating,
    )
  ) {
    DesktopInvitationPhase.Creating ->
      NomeDesktopInvitationPending(
        title = stringResource(MR.strings.creating_link),
      )
    DesktopInvitationPhase.Retry ->
      NomeDesktopInvitationRetry(onRetryInvitation)
    DesktopInvitationPhase.Ready ->
      NomeDesktopInvitationReady(
        invitation = invitation,
        currentProfileName = currentProfileName,
        onOpenProfile = onOpenProfile,
        onInvitationLocalAction = onInvitationLocalAction,
      )
  }
}

@Composable
private fun NomeDesktopInvitationPending(
  title: String,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().heightIn(min = 210.dp),
    color = NomeDesktopNewChatColors.Surface,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, NomeDesktopNewChatColors.Border),
    elevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      CircularProgressIndicator(
        color = NomeDesktopNewChatColors.Action,
        strokeWidth = 2.dp,
        modifier = Modifier.size(28.dp),
      )
      Spacer(Modifier.height(16.dp))
      Text(
        text = title,
        color = NomeDesktopNewChatColors.Text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun NomeDesktopInvitationRetry(onRetryInvitation: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth().heightIn(min = 210.dp),
    color = NomeDesktopNewChatColors.Surface,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, NomeDesktopNewChatColors.Border),
    elevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = stringResource(MR.strings.error_creating_address),
        color = NomeDesktopNewChatColors.TextMuted,
        fontSize = 14.sp,
      )
      Spacer(Modifier.height(14.dp))
      NomeDesktopPrimaryButton(
        text = stringResource(MR.strings.retry_verb),
        onClick = onRetryInvitation,
      )
    }
  }
}

@Composable
private fun NomeDesktopInvitationReady(
  invitation: CreatedConnLink,
  currentProfileName: String,
  onOpenProfile: (() -> Unit)?,
  onInvitationLocalAction: () -> Unit,
) {
  val clipboard = LocalClipboardManager.current
  val link = invitation.simplexChatUri(short = false)

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeDesktopNewChatColors.Surface,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, NomeDesktopNewChatColors.Border),
    elevation = 0.dp,
  ) {
    BoxWithConstraints(Modifier.padding(24.dp)) {
      val compact = maxWidth < 680.dp
      if (compact) {
        Column {
          NomeDesktopQrPanel(invitation, onInvitationLocalAction)
          Spacer(Modifier.height(22.dp))
          NomeDesktopInvitationDetails(
            link = link,
            currentProfileName = currentProfileName,
            onOpenProfile = onOpenProfile,
            onCopyLink = {
              clipboard.shareText(link)
              onInvitationLocalAction()
            },
          )
        }
      } else {
        Row(verticalAlignment = Alignment.Top) {
          Box(Modifier.width(290.dp)) {
            NomeDesktopQrPanel(invitation, onInvitationLocalAction)
          }
          Spacer(Modifier.width(28.dp))
          Box(Modifier.weight(1f)) {
            NomeDesktopInvitationDetails(
              link = link,
              currentProfileName = currentProfileName,
              onOpenProfile = onOpenProfile,
              onCopyLink = {
                clipboard.shareText(link)
                onInvitationLocalAction()
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NomeDesktopQrPanel(
  invitation: CreatedConnLink,
  onInvitationLocalAction: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Surface(
      modifier = Modifier.fillMaxWidth(),
      color = Color.White,
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, NomeDesktopNewChatColors.Border),
      elevation = 0.dp,
    ) {
      SimpleXCreatedLinkQRCode(
        connLink = invitation,
        short = false,
        padding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        tintColor = NomeDesktopNewChatColors.Qr,
        onShare = onInvitationLocalAction,
      )
    }
    Text(
      text = stringResource(MR.strings.onboarding_or_show_qr_code),
      color = NomeDesktopNewChatColors.TextMuted,
      fontSize = 12.sp,
      lineHeight = 17.sp,
      modifier = Modifier.padding(top = 9.dp),
    )
  }
}

@Composable
private fun NomeDesktopInvitationDetails(
  link: String,
  currentProfileName: String,
  onOpenProfile: (() -> Unit)?,
  onCopyLink: () -> Unit,
) {
  Text(
    text = stringResource(MR.strings.one_time_link),
    color = NomeDesktopNewChatColors.Text,
    fontSize = 17.sp,
    fontWeight = FontWeight.SemiBold,
  )
  Text(
    text = stringResource(MR.strings.a_link_for_one_person),
    color = NomeDesktopNewChatColors.TextMuted,
    fontSize = 13.sp,
    lineHeight = 19.sp,
    modifier = Modifier.padding(top = 6.dp),
  )
  Spacer(Modifier.height(18.dp))
  Text(
    text = stringResource(MR.strings.simplex_link_mode_full),
    color = NomeDesktopNewChatColors.TextMuted,
    fontSize = 12.sp,
    fontWeight = FontWeight.SemiBold,
  )
  Surface(
    modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
    color = NomeDesktopNewChatColors.SurfaceSubtle,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, NomeDesktopNewChatColors.Border),
    elevation = 0.dp,
  ) {
    SelectionContainer {
      Text(
        text = link,
        color = NomeDesktopNewChatColors.Text,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(12.dp),
      )
    }
  }
  Spacer(Modifier.height(12.dp))
  NomeDesktopPrimaryButton(
    text = stringResource(MR.strings.copy_verb),
    onClick = onCopyLink,
    icon = MR.images.ic_content_copy,
  )
  Spacer(Modifier.height(18.dp))
  Divider(color = NomeDesktopNewChatColors.Border)
  NomeDesktopProfileRow(
    currentProfileName = currentProfileName,
    onOpenProfile = onOpenProfile,
  )
}

@Composable
private fun NomeDesktopProfileRow(
  currentProfileName: String,
  onOpenProfile: (() -> Unit)?,
) {
  val modifier =
    Modifier
      .fillMaxWidth()
      .heightIn(min = 62.dp)
      .let { base ->
        if (onOpenProfile == null) {
          base
        } else {
          base.clickable(role = Role.Button, onClick = onOpenProfile)
        }
      }
      .padding(vertical = 10.dp)
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(38.dp)
          .background(NomeDesktopNewChatColors.Mint, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = currentProfileName.firstOrNull()?.uppercase() ?: "N",
        color = NomeDesktopNewChatColors.Action,
        fontWeight = FontWeight.Bold,
      )
    }
    Spacer(Modifier.width(11.dp))
    Column(Modifier.weight(1f)) {
      Text(
        text = currentProfileName,
        color = NomeDesktopNewChatColors.Text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = stringResource(MR.strings.profile_will_be_sent_to_contact_sending_link),
        color = NomeDesktopNewChatColors.TextMuted,
        fontSize = 12.sp,
      )
    }
    if (onOpenProfile != null) {
      Icon(
        painter = painterResource(MR.images.ic_chevron_right),
        contentDescription = null,
        tint = NomeDesktopNewChatColors.TextMuted,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun NomeDesktopPasteInvitationPage(
  pastedLink: MutableState<String>,
  onSubmitPastedLink: (String) -> Unit,
) {
  val clipboard = LocalClipboardManager.current
  val submitEnabled = desktopConnectionSubmitEnabled(pastedLink.value)

  Text(
    text = stringResource(MR.strings.simplex_link_invitation),
    color = NomeDesktopNewChatColors.Text,
    fontSize = 30.sp,
    lineHeight = 37.sp,
    fontWeight = FontWeight.SemiBold,
    modifier = Modifier.semantics { heading() },
  )
  Text(
    text = stringResource(MR.strings.paste_the_link_you_received),
    color = NomeDesktopNewChatColors.TextMuted,
    fontSize = 14.sp,
    lineHeight = 21.sp,
    modifier = Modifier.padding(top = 8.dp),
  )
  Spacer(Modifier.height(24.dp))

  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeDesktopNewChatColors.Surface,
    shape = RoundedCornerShape(18.dp),
    border = BorderStroke(1.dp, NomeDesktopNewChatColors.Border),
    elevation = 0.dp,
  ) {
    Column(Modifier.padding(24.dp)) {
      Text(
        text = stringResource(MR.strings.one_time_link),
        color = NomeDesktopNewChatColors.Text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
      )
      OutlinedTextField(
        value = pastedLink.value,
        onValueChange = { pastedLink.value = it },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        placeholder = {
          Text(
            text = stringResource(MR.strings.search_or_paste_simplex_link),
            color = NomeDesktopNewChatColors.TextSubtle,
          )
        },
        minLines = 4,
        maxLines = 7,
        shape = RoundedCornerShape(12.dp),
        colors =
          TextFieldDefaults.outlinedTextFieldColors(
            textColor = NomeDesktopNewChatColors.Text,
            cursorColor = NomeDesktopNewChatColors.Action,
            focusedBorderColor = NomeDesktopNewChatColors.Action,
            unfocusedBorderColor = NomeDesktopNewChatColors.Border,
            backgroundColor = NomeDesktopNewChatColors.SurfaceSubtle,
          ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
          KeyboardActions(
            onDone = {
              if (submitEnabled) {
                onSubmitPastedLink(pastedLink.value)
              }
            },
          ),
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            clipboard.getText()?.text?.let { text ->
              pastedLink.value = text
              onSubmitPastedLink(text)
            }
          },
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(11.dp),
          border = BorderStroke(1.dp, NomeDesktopNewChatColors.Border),
          colors =
            ButtonDefaults.outlinedButtonColors(
              backgroundColor = NomeDesktopNewChatColors.Surface,
              contentColor = NomeDesktopNewChatColors.Text,
            ),
        ) {
          Icon(
            painter = painterResource(MR.images.ic_content_paste),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(stringResource(MR.strings.paste_button))
        }
        Box(Modifier.weight(1f)) {
          NomeDesktopPrimaryButton(
            text = stringResource(MR.strings.connect_via_link_verb),
            onClick = { onSubmitPastedLink(pastedLink.value) },
            enabled = submitEnabled,
            icon = MR.images.ic_arrow_forward,
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .size(30.dp)
              .background(NomeDesktopNewChatColors.Mint, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter = painterResource(MR.images.ic_lock),
            contentDescription = null,
            tint = NomeDesktopNewChatColors.Action,
            modifier = Modifier.size(16.dp),
          )
        }
        Text(
          text = stringResource(MR.strings.nome_desktop_connect_privacy),
          color = NomeDesktopNewChatColors.TextMuted,
          fontSize = 12.sp,
          lineHeight = 17.sp,
          modifier = Modifier.padding(start = 10.dp),
        )
      }
    }
  }
}

@Composable
private fun NomeDesktopPrimaryButton(
  text: String,
  onClick: () -> Unit,
  enabled: Boolean = true,
  icon: dev.icerock.moko.resources.ImageResource? = null,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth().height(48.dp),
    shape = RoundedCornerShape(11.dp),
    colors =
      ButtonDefaults.buttonColors(
        backgroundColor = NomeDesktopNewChatColors.Action,
        contentColor = Color.White,
        disabledBackgroundColor = NomeDesktopNewChatColors.Disabled,
        disabledContentColor = NomeDesktopNewChatColors.DisabledText,
      ),
    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
  ) {
    if (icon != null) {
      Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
    }
    Text(
      text = text,
      fontSize = 14.sp,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

private object NomeDesktopNewChatColors {
  val Background = Color(0xFFF5F7FA)
  val Surface = Color(0xFFFFFFFF)
  val SurfaceSubtle = Color(0xFFF8FAFB)
  val Border = Color(0xFFE0E6EB)
  val Text = Color(0xFF0E1B2D)
  val TextMuted = Color(0xFF607084)
  val TextSubtle = Color(0xFF8794A3)
  val Action = Color(0xFF0A874D)
  val Mint = Color(0xFFE8F6F0)
  val Qr = Color(0xFF0E1B2D)
  val Disabled = Color(0xFFC8D3CF)
  val DisabledText = Color(0xFFF4F7F6)
}
