package chat.simplex.common.views.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
internal actual fun PlatformOnboardingBrandLogo(
  legacyContent: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "Nome",
      color = MaterialTheme.colors.onBackground,
      fontWeight = FontWeight.Bold,
      fontSize = 36.sp,
      lineHeight = 42.sp,
    )
  }
}

internal actual fun platformOnboardingBrandText(legacyText: String): String =
  legacyText.replace("SimpleX Chat", "Nome").replace("SimpleX", "Nome")
