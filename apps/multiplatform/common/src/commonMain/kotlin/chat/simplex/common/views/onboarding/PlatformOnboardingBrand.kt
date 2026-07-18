package chat.simplex.common.views.onboarding

import androidx.compose.runtime.Composable

/**
 * Replaces only the Android product mark. Desktop retains the official compatibility brand.
 */
@Composable
internal expect fun PlatformOnboardingBrandLogo(
  legacyContent: @Composable () -> Unit,
)

internal expect fun platformOnboardingBrandText(legacyText: String): String
