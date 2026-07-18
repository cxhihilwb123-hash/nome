package chat.simplex.common.views.onboarding

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformOnboardingBrandLogo(
  legacyContent: @Composable () -> Unit,
) {
  legacyContent()
}

internal actual fun platformOnboardingBrandText(legacyText: String): String = legacyText
