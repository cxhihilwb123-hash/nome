# Changeset and findings

## Root cause

The two Vivo devices expose different bottom-window behavior:

- V2048A reports a live three-button navigation inset.
- V2047A consumes the gesture inset before the shared navigation child reads it, so live child sources report zero even though the system keeps a visible bottom region.

The old 76dp item plus a 48dp maximum reserve placed content too high on V2048A. Removing the reserve entirely clipped or hid the items. The final implementation keeps one shared layout and:

1. reduces the actual item/touch height from 76dp to 64dp;
2. reads `safeDrawing`, Compose `systemGestures`, and platform navigation/gesture insets;
3. falls back to Android's authoritative `android:dimen/navigation_bar_height` only when all live values are zero;
4. caps the reserve at 36dp.

The framework resource was read-only verified as `42.0dip` on both devices. The 36dp cap produced a complete item with a small bottom margin on each device.

## Files changed in this batch

- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomePrimaryBottomNavigation.kt`
  - 64dp shared items.
  - live inset sources plus zero-live-inset framework fallback.
  - 36dp reserve maximum.
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt`
  - Contacts-root system Back returns to Home.
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/platform/Resources.android.kt`
  - Android-only Nome branding map for legacy app-brand resources.
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/SettingsView.kt`
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/PrivacySettings.kt`
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/RTCServers.kt`
  - route affected strings through the Android branding map.
- `apps/multiplatform/common/src/androidMain/res/values/nome_settings_strings.xml`
- `apps/multiplatform/common/src/androidMain/res/values-zh-rCN/nome_settings_strings.xml`
  - matching English and Simplified Chinese Nome strings.
- `apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeComposeTest.kt`
- `apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/settings/NomeSettingsBackupComposeTest.kt`
  - exact 64dp navigation assertions and Android Nome-brand mapping assertions.

## Branding policy

Changed as application branding:

- SimpleX Lock -> Nome Lock / Nome 锁定.
- SimpleX support heading -> Nome support heading.
- SimpleX terminal-client wording -> neutral desktop/terminal wording.
- SimpleX default call-server wording -> neutral default call-server wording.
- legacy SimpleX theme label -> Nome.
- generic About application title -> Nome.

Intentionally retained as factual/technical proper nouns:

- SimpleX links and protocol terminology.
- `simplexmq`.
- official upstream source attribution.
- real server operator names, including SimpleX Chat where it identifies the actual operator rather than this app.
- the statement that this Nome build preserves the official SimpleX Chat v6.5.6 routes/core behavior.

No protocol, database, core, account, server, message, call, or iOS behavior was changed.

