package chat.simplex.common.views.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.simplex.common.R

@Composable
internal actual fun PlatformOnboardingBrandLogo(
  legacyContent: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(R.drawable.nome_header_logo),
      contentDescription = stringResource(R.string.nome_brand_logo_description),
      contentScale = ContentScale.Fit,
      modifier = Modifier.width(104.dp).height(44.dp),
    )
  }
}

internal actual fun platformOnboardingBrandText(legacyText: String): String =
  legacyText.replace("SimpleX Chat", "Nome").replace("SimpleX", "Nome")
