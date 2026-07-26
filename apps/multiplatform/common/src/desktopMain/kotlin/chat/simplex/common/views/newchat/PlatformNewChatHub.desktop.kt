package chat.simplex.common.views.newchat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal actual fun PlatformNewChatHub(
  currentProfileName: String,
  currentProfileImage: String?,
  onOpenProfile: (() -> Unit)?,
  onAddContact: () -> Unit,
  onScanOrPaste: () -> Unit,
  onCreateGroup: () -> Unit,
  onCreateChannel: () -> Unit,
  onClose: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(NomeNewChatColors.Background),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxHeight()
          .widthIn(max = 880.dp)
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 44.dp, vertical = 36.dp),
    ) {
      Text(
        text = stringResource(MR.strings.nome_desktop_new_connection_eyebrow),
        color = NomeNewChatColors.Action,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
      )
      Text(
        text = stringResource(MR.strings.nome_desktop_new_connection_title),
        color = NomeNewChatColors.Text,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
      )
      Text(
        text = stringResource(MR.strings.nome_desktop_new_connection_body),
        color = NomeNewChatColors.TextMuted,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        modifier = Modifier.padding(top = 10.dp),
      )

      Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
        color = NomeNewChatColors.Surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NomeNewChatColors.Border),
        elevation = 0.dp,
      ) {
        Column {
          NomeNewChatAction(
            icon = painterResource(MR.images.ic_add_link),
            title = stringResource(MR.strings.nome_desktop_new_connection_invite_title),
            body = stringResource(MR.strings.nome_desktop_new_connection_invite_body),
            onClick = onAddContact,
          )
          Divider(
            modifier = Modifier.padding(start = 84.dp),
            color = NomeNewChatColors.Border,
          )
          NomeNewChatAction(
            icon = painterResource(MR.images.ic_qr_code),
            title = stringResource(MR.strings.nome_desktop_new_connection_open_title),
            body = stringResource(MR.strings.nome_desktop_new_connection_open_body),
            onClick = onScanOrPaste,
          )
          Divider(
            modifier = Modifier.padding(start = 84.dp),
            color = NomeNewChatColors.Border,
          )
          NomeNewChatAction(
            icon = painterResource(MR.images.ic_group),
            title = stringResource(MR.strings.nome_desktop_new_connection_group_title),
            body = stringResource(MR.strings.nome_desktop_new_connection_group_body),
            onClick = onCreateGroup,
          )
          Divider(
            modifier = Modifier.padding(start = 84.dp),
            color = NomeNewChatColors.Border,
          )
          NomeNewChatAction(
            icon = painterResource(MR.images.ic_bigtop_updates),
            title = stringResource(MR.strings.nome_desktop_new_connection_channel_title),
            body = stringResource(MR.strings.nome_desktop_new_connection_channel_body),
            onClick = onCreateChannel,
          )
        }
      }

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .size(30.dp)
              .background(NomeNewChatColors.Mint, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter = painterResource(MR.images.ic_lock),
            contentDescription = null,
            tint = NomeNewChatColors.Action,
            modifier = Modifier.size(16.dp),
          )
        }
        Text(
          text =
            stringResource(
              MR.strings.nome_desktop_new_connection_privacy,
              currentProfileName,
            ),
          color = NomeNewChatColors.TextMuted,
          fontSize = 13.sp,
          lineHeight = 18.sp,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(start = 10.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
          text = stringResource(MR.strings.nome_desktop_back),
          color = NomeNewChatColors.Action,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          modifier =
            Modifier
              .clickable(onClick = onClose)
              .padding(horizontal = 10.dp, vertical = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun NomeNewChatAction(
  icon: Painter,
  title: String,
  body: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 92.dp)
        .clickable(onClick = onClick)
        .padding(horizontal = 22.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(44.dp)
          .background(NomeNewChatColors.Mint, RoundedCornerShape(13.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = icon,
        contentDescription = null,
        tint = NomeNewChatColors.Action,
        modifier = Modifier.size(23.dp),
      )
    }
    Column(
      modifier = Modifier.padding(start = 18.dp).weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = title,
        color = NomeNewChatColors.Text,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = body,
        color = NomeNewChatColors.TextMuted,
        fontSize = 13.sp,
        lineHeight = 19.sp,
      )
    }
    Icon(
      painter = painterResource(MR.images.ic_chevron_right),
      contentDescription = null,
      tint = NomeNewChatColors.Action,
      modifier = Modifier.padding(start = 16.dp).size(20.dp),
    )
  }
}

private object NomeNewChatColors {
  val Background = Color(0xFFF5F7FA)
  val Surface = Color(0xFFFFFFFF)
  val Border = Color(0xFFE0E6EB)
  val Text = Color(0xFF0E1B2D)
  val TextMuted = Color(0xFF607084)
  val Action = Color(0xFF0A874D)
  val Mint = Color(0xFFE8F6F0)
}
