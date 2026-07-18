package chat.simplex.common.views.localauth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.common.views.usersettings.LAMode

@Composable
internal actual fun PlatformNomeAppLockScreen(
  enabled: Boolean,
  displayName: String?,
  profileImage: String?,
  usingLAMode: LAMode,
  onUnlock: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  if (!enabled) {
    legacyContent()
    return
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(NomeTheme.colors.background)
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = NomeTheme.dimensions.screenHorizontalInset),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.height(4.dp))
    Image(
      painter = painterResource(R.drawable.nome_header_logo),
      contentDescription = stringResource(R.string.nome_brand_logo_description),
      modifier = Modifier.width(104.dp).height(44.dp),
    )
    Spacer(Modifier.height(34.dp))
    Surface(
      modifier = Modifier.size(84.dp),
      shape = NomeTheme.shapes.large,
      color = NomeTheme.colors.successContainer,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Rounded.Lock,
          contentDescription = null,
          modifier = Modifier.size(42.dp),
          tint = NomeTheme.colors.action,
        )
      }
    }
    Spacer(Modifier.height(24.dp))
    Text(
      text = stringResource(R.string.nome_p02_title),
      modifier = Modifier.semantics { heading() },
      style = NomeTheme.typography.titleLarge,
      color = NomeTheme.colors.textPrimary,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = stringResource(
        if (usingLAMode == LAMode.SYSTEM) {
          R.string.nome_p02_system_body
        } else {
          R.string.nome_p02_passcode_body
        },
      ),
      style = NomeTheme.typography.supporting,
      color = NomeTheme.colors.textSecondary,
      textAlign = TextAlign.Center,
    )
    if (!displayName.isNullOrBlank()) {
      Spacer(Modifier.height(18.dp))
      Surface(
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, NomeTheme.colors.action),
        color = NomeTheme.colors.surfaceSubtle,
      ) {
        Box(contentAlignment = Alignment.Center) {
          ProfileImage(
            size = 68.dp,
            image = profileImage,
            color = NomeTheme.colors.action,
            backgroundColor = NomeTheme.colors.successContainer,
          )
        }
      }
      Spacer(Modifier.height(6.dp))
      Text(
        text = displayName,
        style = NomeTheme.typography.bodyStrong,
        color = NomeTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
      )
    }
    Spacer(Modifier.height(24.dp))
    NomeButton(
      text = stringResource(
        if (usingLAMode == LAMode.SYSTEM) {
          R.string.nome_p02_system_unlock
        } else {
          R.string.nome_p02_passcode_unlock
        },
      ),
      onClick = onUnlock,
      modifier = Modifier.fillMaxWidth(),
      semanticsLabel = stringResource(
        if (usingLAMode == LAMode.SYSTEM) {
          R.string.nome_p02_system_unlock
        } else {
          R.string.nome_p02_passcode_unlock
        },
      ),
      leadingIcon = {
        Icon(
          imageVector = if (usingLAMode == LAMode.SYSTEM) Icons.Rounded.Fingerprint else Icons.Rounded.Key,
          contentDescription = null,
        )
      },
    )
    if (usingLAMode == LAMode.SYSTEM) {
      Spacer(Modifier.height(8.dp))
      NomeButton(
        text = stringResource(R.string.nome_p02_device_lock),
        onClick = onUnlock,
        modifier = Modifier.fillMaxWidth(),
        variant = NomeButtonVariant.SECONDARY,
        semanticsLabel = stringResource(R.string.nome_p02_device_lock),
        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
      )
    }
    Spacer(Modifier.height(14.dp))
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = NomeTheme.shapes.compact,
      color = NomeTheme.colors.successContainer,
      border = BorderStroke(1.dp, NomeTheme.colors.sentMessageBorder),
    ) {
      androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Security,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = NomeTheme.colors.onSuccessContainer,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.nome_p02_local_note),
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.onSuccessContainer,
        )
      }
    }
  }
}
