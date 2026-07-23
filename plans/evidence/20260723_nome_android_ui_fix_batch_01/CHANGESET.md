# Changeset

## Shared UI

- `NomePrimaryBottomNavigation.kt`: shared Home/Contacts/Settings tab bar, 76 dp height, full-height targets, selected pill, localized accessibility semantics, and a shared `navigationBarsPadding()` fallback.
- `NomeBrandLockup.kt`: shared 700:285 `ContentScale.Fit` lockup renderer.
- `nome_navigation_strings.xml`: English and Simplified Chinese navigation labels/state.

## Routes

- `NomeHomeRoute.android.kt`: replaces `ChatBubbleOutline`, restores P07 bottom navigation, maps Contacts to the existing contacts filter, opens existing settings UI, and renders the header Logo at 84 dp width.
- `PlatformIdentityCenterRoute.android.kt`: replaces private P22 navigation with the shared component and unifies top/system back.
- `PlatformSettingsHomeRoute.android.kt`: replaces private P23 navigation with the shared component and shared Logo renderer at the same 84 dp header width.
- `SettingsView.kt`: Android-only P23 to P22 modal wiring supplies real Home/Contacts callbacks and closes the modal stack; non-Android behavior remains on the prior path.

## Logo call sites

- `PlatformOnboardingBrand.android.kt`
- `PlatformNomeOnboardingPages.android.kt`
- `PlatformNomeAppLockScreen.android.kt`
- `PlatformSettingsHomeRoute.android.kt`
- `PlatformAboutSettingsRoute.android.kt`

All use width-only sizing plus the shared 700:285 aspect-ratio renderer. P07 and P23 use 84 dp after user review; the existing onboarding, About, and lock-screen sizes remain unchanged.

## Tests

- `NomeHomeComposeTest.kt`
- `NomeSettingsBackupComposeTest.kt`
- `NomeChannelIdentityComposeTest.kt`

The tests assert shared labels, selected state, real callback dispatch, and at least 48 dp target height. They compiled into the Android test APK and were installed on both devices. Vivo blocked the Compose test Activity from launching on both phones after the runner entered the requested method, so no direct device assertion result was produced.

## Out-of-scope boundary

No Haskell/native core, `Core.kt`, protocol, database, message state machine, iOS, communication behavior, server configuration, or account data was changed.
