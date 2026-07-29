package chat.simplex.common.views.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

internal enum class NomeChatKind {
  Group,
  Channel,
}

internal fun nomeChatKind(info: ChatInfo): NomeChatKind? =
  when (info) {
    is ChatInfo.Group ->
      when {
        info.groupInfo.isChannel -> NomeChatKind.Channel
        info.groupInfo.businessChat == null -> NomeChatKind.Group
        else -> null
      }
    else -> null
  }

@Composable
internal fun nomeChatKindLabel(kind: NomeChatKind): String =
  stringResource(
    when (kind) {
      NomeChatKind.Group -> MR.strings.info_row_group
      NomeChatKind.Channel -> MR.strings.info_row_channel
    },
  )

@Composable
internal fun NomeChatKindBadge(kind: NomeChatKind) {
  val actionColor = NomeTheme.colors.action
  Row(
    modifier =
      Modifier
        .height(18.dp)
        .clip(CircleShape)
        .background(actionColor.copy(alpha = 0.11f))
        .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Icon(
      painter =
        painterResource(
          when (kind) {
            NomeChatKind.Group -> MR.images.ic_group_filled
            NomeChatKind.Channel -> MR.images.ic_bigtop_updates
          },
        ),
      contentDescription = null,
      modifier = Modifier.size(11.dp),
      tint = actionColor,
    )
    Text(
      text = nomeChatKindLabel(kind),
      style =
        NomeTheme.typography.supporting.copy(
          fontSize = 10.sp,
          lineHeight = 12.sp,
          fontWeight = FontWeight.SemiBold,
        ),
      color = actionColor,
      maxLines = 1,
    )
  }
}
