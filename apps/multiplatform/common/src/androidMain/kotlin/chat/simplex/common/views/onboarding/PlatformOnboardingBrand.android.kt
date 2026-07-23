package chat.simplex.common.views.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.ui.nome.components.NomeBrandLockup

@Composable
internal actual fun PlatformOnboardingBrandLogo(
  legacyContent: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    NomeBrandLockup(
      contentDescription = stringResource(R.string.nome_brand_logo_description),
      modifier = Modifier.width(104.dp),
    )
  }
}

internal actual fun platformOnboardingBrandText(legacyText: String): String =
  legacyText.replace("SimpleX Chat", "Nome").replace("SimpleX", "Nome")
