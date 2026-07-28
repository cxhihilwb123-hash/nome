package chat.simplex.common.views.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.views.helpers.fontSizeSqrtMultiplier
import chat.simplex.common.views.helpers.toDp
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.painterResource

@Composable
internal fun NomeDesktopChatListItem(
  icon: ImageResource,
  iconTint: Color,
  title: String,
  timestamp: String,
  preview: @Composable () -> Unit,
) {
  val iconBoxSize = 48.dp * fontSizeSqrtMultiplier
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp * fontSizeSqrtMultiplier),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(iconBoxSize)
          .clip(RoundedCornerShape(14.dp))
          .background(iconTint.copy(alpha = 0.1f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(24.dp * fontSizeSqrtMultiplier),
        tint = iconTint,
      )
    }
    Spacer(Modifier.width(12.dp))
    Column(
      modifier = Modifier.weight(1f).heightIn(min = iconBoxSize),
      verticalArrangement = Arrangement.Center,
    ) {
      Row(Modifier.fillMaxWidth()) {
        Text(
          text = title,
          modifier = Modifier.weight(1f).alignByBaseline(),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.h3.copy(fontSize = 16.sp, lineHeight = 20.sp),
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(10.dp))
        Text(
          text = timestamp,
          modifier = Modifier.alignByBaseline(),
          color = MaterialTheme.colors.secondary,
          style = MaterialTheme.typography.body2.copy(fontSize = 12.sp, lineHeight = 16.sp),
        )
      }
      Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 18.sp.toDp()),
        contentAlignment = Alignment.CenterStart,
      ) {
        preview()
      }
    }
  }
}
