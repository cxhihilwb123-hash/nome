# Nome Android UI Fix Batch 01 — Summary

Date: 2026-07-23

Scope: Android-only UI/navigation fixes for UI-001, UI-002, UI-003, UI-005, and UI-006.

## Outcome

- Added one shared `NomePrimaryBottomNavigation` for Home, Contacts, and Settings.
- Restored the bottom navigation on P07 and reused the same implementation on P22 and P23.
- Home and Contacts use the existing chat-list filter route; Settings opens the existing Android settings modal.
- P22 Home/Contacts close the settings modal stack before returning to the requested destination.
- P22 now handles the top back button and Android system back through the same close callback.
- Replaced the 91 x 37 opaque header bitmap with 700 x 285 RGBA day/night resources derived only from approved local Nome assets.
- Reused one `NomeBrandLockup` composable for all existing Android header/onboarding/about lockup call sites.
- After protected-screen user review, increased only the P07/P23 header lockup width from 66 dp to 84 dp. The shared 700:285 renderer keeps the displayed height proportional at about 34 dp; onboarding, About, and lock-screen sizes were not changed.
- Navigation items fill the 76 dp bar, retain a 48 dp minimum touch boundary, use `Role.Tab`, expose localized labels and selected state, and apply a shared navigation-bar inset fallback.

## Verification status

- Android compile, unit tests, release lint, debug assembly, desktop compile, and Android test APK assembly passed against final source.
- The same final production APK was installed on both Vivo devices. Cold launch, Home/Contacts/Settings real taps, P22/P23 routing, top back, bottom navigation, and Android system back reached the expected visible target state on both devices.
- The post-review 84 dp Logo APK was then rebuilt and installed on both devices. A fresh cold P07 launch and real P07 -> P23 -> P07 taps passed. P07 Logo bounds measured 221 x 90 px on V2048A at 420 dpi and 252 x 103 px on V2047A at 480 dpi, matching approximately 84 x 34 dp without aspect-ratio distortion.
- Simplified Chinese/English and light/dark were exercised on both devices. Fixed labels and Logo nodes were present in every configuration, and each device was restored to system Chinese plus light mode.
- The Android test APK was installed on both devices. On each device the runner entered the requested single test, but Vivo kept a system application in front and never launched the Compose test Activity; no assertion result was produced and the hung run was manually stopped.
- UIAutomator clips bottom-node bounds against an OEM root bound and reports less than 48 dp even though the component declares a 76 dp full-height selectable target and real taps succeed. Because the Compose Activity could not launch, direct on-device Compose-boundary proof remains an infrastructure evidence gap.
- Screenshot/recording privacy protection was not disabled. Pixel-level on-device screenshot evidence for Logo sharpness/background is therefore intentionally absent; approved RGBA resources and non-sensitive light/dark composites passed offline inspection.

## Evidence boundary

This directory is new. No file under `plans/evidence/20260723_nome_android_full_ui_audit/` or `plans/evidence/20260722_nome_android_feature_validation/` was modified or regenerated.

All `/sdcard/nome_batch01_*.xml` temporary UI-tree files created by this batch were deleted from both devices after fixed-label/redacted extraction. No raw device UI tree was added to this evidence directory.
