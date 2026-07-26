package chat.simplex.common.views.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource

@Composable
internal actual fun PlatformOnboardingBrandLogo(
  legacyContent: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(MR.images.nome_mark),
      contentDescription = stringResource(MR.strings.image_descr_simplex_logo),
      modifier = Modifier.width(76.dp),
    )
  }
}

internal actual fun platformOnboardingBrandText(legacyText: String): String =
  legacyText.replace("SimpleX Chat", "Nome").replace("SimpleX", "Nome")
