package chat.simplex.common.views.usersettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.BuildConfigCommon
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal actual fun PlatformAboutSettingsRoute(
  onClose: (() -> Unit)?,
  onOpenVersion: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  val versionName =
    String.format(
      stringResource(MR.strings.app_version_name),
      BuildConfigCommon.DESKTOP_VERSION_NAME,
    )
  val versionCode =
    String.format(
      stringResource(MR.strings.app_version_code),
      BuildConfigCommon.DESKTOP_VERSION_CODE,
    )
  val versionTitle = stringResource(MR.strings.app_version_title)
  val productName = "${stringResource(MR.strings.app_name)} Desktop"

  NomeDesktopSettingsShell(
    title = stringResource(MR.strings.about_simplex_chat),
    onClose = onClose,
  ) { palette ->
    NomeDesktopSettingsCard(palette = palette) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          modifier = Modifier.size(76.dp),
          color = palette.accentContainer,
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, palette.border),
          elevation = 0.dp,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Image(
              painter = painterResource(MR.images.nome_mark),
              contentDescription =
                stringResource(MR.strings.image_descr_simplex_logo),
              modifier = Modifier.size(58.dp),
            )
          }
        }
        Column(
          modifier = Modifier.padding(start = 20.dp),
        ) {
          Text(
            text = productName,
            color = palette.textPrimary,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text = versionName,
            color = palette.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 7.dp),
          )
          Text(
            text = versionCode,
            color = palette.textSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 1.dp),
          )
        }
      }
    }

    Text(
      text = stringResource(MR.strings.nome_desktop_identity_local_title),
      color = palette.accent,
      fontSize = 11.sp,
      lineHeight = 15.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.8.sp,
      modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
    Text(
      text = stringResource(MR.strings.nome_usage_information_local_data_body),
      color = palette.textSecondary,
      fontSize = 14.sp,
      lineHeight = 22.sp,
      modifier = Modifier.fillMaxWidth(),
    )

    Text(
      text = versionTitle,
      color = palette.textSecondary,
      fontSize = 11.sp,
      lineHeight = 15.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.8.sp,
      modifier = Modifier.padding(top = 26.dp, bottom = 8.dp),
    )
    Surface(
      color = palette.surface,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, palette.border),
      elevation = 0.dp,
      modifier =
        Modifier
          .fillMaxWidth()
          .semantics(mergeDescendants = true) {
            contentDescription =
              "$versionTitle. $versionName. $versionCode"
          }
          .clickable(
            role = Role.Button,
            onClick = onOpenVersion,
          ),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .size(40.dp)
              .background(
                palette.accentContainer,
                RoundedCornerShape(12.dp),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter = painterResource(MR.images.ic_info),
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(20.dp),
          )
        }
        Column(
          modifier = Modifier.padding(start = 14.dp).weight(1f),
        ) {
          Text(
            text = versionTitle,
            color = palette.textPrimary,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "$versionName · $versionCode",
            color = palette.textSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 3.dp),
          )
        }
        Spacer(Modifier.width(16.dp))
        Icon(
          painter = painterResource(MR.images.ic_chevron_right),
          contentDescription = null,
          tint = palette.accent,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    Divider(
      color = palette.divider,
      modifier = Modifier.padding(top = 26.dp, bottom = 16.dp),
    )
    Text(
      text = stringResource(MR.strings.app_name),
      color = palette.textSecondary,
      fontSize = 12.sp,
      lineHeight = 18.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
