package chat.simplex.common.views.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.res.MR

@Composable
internal actual fun PlatformChannelPostCard(
  visible: Boolean,
  authorName: String,
  authorImage: String?,
  timestamp: String,
  text: NomeChannelPostText,
  fileName: String?,
  fileSize: String?,
  fileContent: (@Composable () -> Unit)?,
  legacyContent: @Composable () -> Unit,
) {
  if (!visible) {
    legacyContent()
    return
  }
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.surface,
    contentColor = NomeTheme.colors.textPrimary,
    shape = NomeTheme.shapes.control,
    border =
      BorderStroke(
        1.dp,
        NomeTheme.colors.border,
      ),
  ) {
    Column(
      modifier =
        Modifier.padding(
          horizontal = 14.dp,
          vertical = 13.dp,
        ),
      verticalArrangement =
        Arrangement.spacedBy(9.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ProfileImage(
          size = 34.dp,
          image = authorImage,
          icon =
            MR.images
              .ic_bigtop_updates_circle_filled,
          backgroundColor =
            NomeTheme.colors.surfaceSubtle,
        )
        Spacer(
          Modifier.width(9.dp),
        )
        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = authorName,
            style =
              NomeTheme.typography
                .bodyStrong,
            color =
              NomeTheme.colors
                .textPrimary,
            maxLines = 1,
            overflow =
              TextOverflow.Ellipsis,
          )
          Text(
            text =
              stringResource(
                R.string.nome_p21_broadcast,
              ),
            style =
              NomeTheme.typography
                .supporting,
            color =
              NomeTheme.colors
                .textSecondary,
          )
        }
        Text(
          text = timestamp,
          style =
            NomeTheme.typography
              .supporting,
          color =
            NomeTheme.colors
              .textSecondary,
        )
      }
      if (text.title.isNotEmpty()) {
        Text(
          text = text.title,
          style =
            NomeTheme.typography
              .bodyStrong
              .copy(
                fontWeight =
                  FontWeight.SemiBold,
              ),
          color =
            NomeTheme.colors
              .textPrimary,
        )
      }
      if (text.body.isNotEmpty()) {
        Text(
          text = text.body,
          style =
            NomeTheme.typography
              .supporting,
          color =
            NomeTheme.colors
              .textPrimary,
        )
      }
      if (fileContent != null) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(
                NomeTheme.colors
                  .infoContainer,
                NomeTheme.shapes.compact,
              )
              .padding(
                horizontal = 6.dp,
                vertical = 4.dp,
              ),
        ) {
          fileContent()
        }
      } else if (!fileName.isNullOrBlank()) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(
                NomeTheme.colors
                  .infoContainer,
                NomeTheme.shapes.compact,
              )
              .padding(
                horizontal = 12.dp,
                vertical = 10.dp,
              ),
          verticalAlignment =
            Alignment.CenterVertically,
          horizontalArrangement =
            Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector =
              Icons.Rounded
                .InsertDriveFile,
            contentDescription = null,
            modifier =
              Modifier.size(19.dp),
            tint =
              NomeTheme.colors.info,
          )
          Text(
            text =
              if (fileSize.isNullOrBlank()) {
                fileName
              } else {
                "$fileName · $fileSize"
              },
            modifier = Modifier.weight(1f),
            style =
              NomeTheme.typography
                .supporting,
            color =
              NomeTheme.colors.info,
            maxLines = 1,
            overflow =
              TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
