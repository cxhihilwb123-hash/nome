package chat.simplex.common.views.usersettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.simplex.common.BuildConfigCommon
import chat.simplex.common.R
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeBrandLockup
import chat.simplex.common.ui.nome.components.NomeFullPageScaffold
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.theme.CurrentColors
import chat.simplex.common.views.helpers.openExternalLink

private const val SIMPLEX_SOURCE_URL =
  "https://github.com/simplex-chat/simplex-chat"
private const val SIMPLEX_LICENSE_URL =
  "https://github.com/simplex-chat/simplex-chat/blob/v6.5.6/LICENSE"

@Composable
internal actual fun PlatformAboutSettingsRoute(
  onClose: (() -> Unit)?,
  onOpenVersion: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  if (onClose == null) {
    legacyContent()
    return
  }
  val uriHandler = LocalUriHandler.current
  val darkTheme =
    !CurrentColors.collectAsState().value.colors.isLight
  NomeAndroidTheme(darkTheme = darkTheme) {
    NomeAboutSettingsContent(
      appVersionName = BuildConfigCommon.ANDROID_VERSION_NAME,
      appVersionCode = BuildConfigCommon.ANDROID_VERSION_CODE,
      onClose = onClose,
      onOpenVersion = onOpenVersion,
      onOpenSource = {
        uriHandler.openExternalLink(SIMPLEX_SOURCE_URL)
      },
      onOpenLicense = {
        uriHandler.openExternalLink(SIMPLEX_LICENSE_URL)
      },
    )
  }
}

@Composable
fun NomeAboutSettingsContent(
  appVersionName: String,
  appVersionCode: Int,
  onClose: () -> Unit,
  onOpenVersion: () -> Unit,
  onOpenSource: () -> Unit,
  onOpenLicense: () -> Unit,
) {
  BackHandler(onBack = onClose)
  NomeFullPageScaffold(
    title = stringResource(R.string.nome_about_title),
    backLabel = stringResource(R.string.nome_back),
    onClose = onClose,
  ) {
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border = BorderStroke(1.dp, NomeTheme.colors.border),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(
              horizontal = 18.dp,
              vertical = 20.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        NomeBrandLockup(
          contentDescription =
            stringResource(
              R.string.nome_about_logo_description,
            ),
          modifier =
            Modifier
              .width(112.dp),
        )
        Text(
          text =
            stringResource(
              R.string.nome_about_product_name,
            ),
          modifier = Modifier.semantics { heading() },
          style = NomeTheme.typography.titleLarge,
          color = NomeTheme.colors.textPrimary,
          textAlign = TextAlign.Center,
        )
        Text(
          text =
            stringResource(
              R.string.nome_about_version,
              appVersionName,
              appVersionCode,
            ),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
          textAlign = TextAlign.Center,
        )
      }
    }
    Text(
      text =
        stringResource(
          R.string.nome_about_foundation,
        ),
      modifier = Modifier.padding(vertical = 4.dp),
      style = NomeTheme.typography.body,
      color = NomeTheme.colors.textSecondary,
    )
    Text(
      text =
        stringResource(
          R.string.nome_about_project_section,
        ),
      modifier =
        Modifier
          .padding(top = 4.dp)
          .semantics { heading() },
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textSecondary,
    )
    NomeSurface(
      modifier = Modifier.fillMaxWidth(),
      border = BorderStroke(1.dp, NomeTheme.colors.border),
    ) {
      Column(Modifier.fillMaxWidth()) {
        NomeAboutActionRow(
          icon = Icons.Rounded.Info,
          title =
            stringResource(
              R.string.nome_about_version_title,
            ),
          body =
            stringResource(
              R.string.nome_about_version_body,
            ),
          onClick = onOpenVersion,
        )
        Divider(
          modifier = Modifier.padding(start = 60.dp),
          color = NomeTheme.colors.divider,
        )
        NomeAboutActionRow(
          icon = Icons.Rounded.Code,
          title =
            stringResource(
              R.string.nome_about_source_title,
            ),
          body =
            stringResource(
              R.string.nome_about_source_body,
            ),
          onClick = onOpenSource,
        )
        Divider(
          modifier = Modifier.padding(start = 60.dp),
          color = NomeTheme.colors.divider,
        )
        NomeAboutActionRow(
          icon = Icons.Rounded.Policy,
          title =
            stringResource(
              R.string.nome_about_license_title,
            ),
          body =
            stringResource(
              R.string.nome_about_license_body,
            ),
          onClick = onOpenLicense,
        )
      }
    }
  }
}

@Composable
private fun NomeAboutActionRow(
  icon: ImageVector,
  title: String,
  body: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .heightIn(min = 64.dp)
        .clickable(
          role = Role.Button,
          onClick = onClick,
        )
        .nomeTalkBackSemantics(
          label = "$title. $body",
          role = Role.Button,
        )
        .padding(
          horizontal = 12.dp,
          vertical = 8.dp,
        ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier
          .size(38.dp)
          .background(
            NomeTheme.colors.successContainer,
            NomeTheme.shapes.control,
          ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(21.dp),
        tint = NomeTheme.colors.success,
      )
    }
    Spacer(Modifier.width(10.dp))
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = NomeTheme.typography.label,
        color = NomeTheme.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = body,
        style = NomeTheme.typography.supporting,
        color = NomeTheme.colors.textSecondary,
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
