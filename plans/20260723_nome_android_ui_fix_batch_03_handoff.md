# Nome Android UI Fix Batch 03 — Handoff

Date: 2026-07-23

## Verdict

The approved Home/Contacts/custom-list redesign is implemented and verified. The list feature is restored as a first-class Home surface, Home/Contacts/Settings share one measured navigation component, the authoritative Nome lockup is theme-safe, and the adaptive launcher mark has more breathing room.

Build, unit, lint, emulator interaction, visual matrix, and two-device real-click gates pass. The work remains local and uncommitted for user review.

## User-visible behavior

- Home: brand header, page title/subtitle, compact search, All/user-list/Add-list shelf, grouped conversations, FAB, and bottom navigation.
- Contacts: the same hierarchy and navigation, with contact-focused filtering and content.
- Custom list: selecting `111` shows the list as the page state and exposes Manage list.
- Navigation: Home, Contacts, and Settings are real destinations; continuous switching, top Back, Android Back, and Settings return do not trap or duplicate pages.
- Header logo: authoritative transparent 700x285 resource, 104dp wide, aspect ratio preserved; approved dark asset selected explicitly.
- Launcher icon: authoritative Nome mark centered at 60dp high on the standard transparent 108dp adaptive foreground canvas.

## Intentional Batch 03 source areas

- `NomeHomeRoute.android.kt`
- `NomePrimaryBottomNavigation.kt`
- `NomeBrandLockup.kt`
- `PlatformTagEditorRoute.android.kt`
- English/Chinese Home and primary-navigation strings
- explicit approved dark lockup alias
- Android adaptive foreground PNGs at mdpi through xxxhdpi, default and dark-blue
- Android Appearance preview foregrounds
- `NomeHomeComposeTest.kt`
- `NomeHomeBatch03ScreenshotTest.kt`
- `NomeHomeEvidenceActivity.kt`

No Haskell/native core, `Core.kt`, protocol, database, server/account, message-state, communication behavior, or iOS source was changed.

## Build and emulator evidence

- Consolidated `assembleDebugAndroidTest + testDebugUnitTest + lintRelease`: PASS, 144 tasks, 2m24s.
- JVM: 54 tests, 0 failures/errors/skips.
- Release lint: 0 Error/Fatal.
- API 35 interaction instrumentation: 15/15 PASS in 20.665s.
- API 35 screenshot instrumentation: 1/1 PASS in 22.413s.
- Twelve 1080x2400 native renders cover Home, Contacts, and custom list in English/Chinese and light/dark.
- Root `design-qa.md`: `final result: passed`.

Final artifacts:

- debug APK SHA256: `730408743fe4b5059edd23048afa6207b63fbf8a26f638b57d321d997f6c76a4`
- Android test APK SHA256: `3c5c6c18a2648da22d7c6b2df48b7d43b07c2edb0e8fc00ad615498b5e3c7d3e`

## Two Vivo devices

V2048A, 420dpi:

- final package 6.5.6/358 installed at 2026-07-23 19:59:07
- Home -> Contacts -> Home -> 111 -> Manage list -> top Back -> Settings -> Android Back -> Home: PASS
- navigation item: 200px / 2.625 = 76.19dp

V2047A, 480dpi:

- final package 6.5.6/358 installed at 2026-07-23 19:49:48
- Home -> Contacts -> Home -> 111 -> Manage list -> Android Back -> Settings -> Android Back -> Home: PASS
- navigation item: 228px / 3.0 = 76.0dp

Only fixed accessibility labels and bounds were read. Temporary device UI XML files were deleted after the checks. No message was sent, no contact/list was deleted, and no account data was changed.

## Evidence gaps

- Screenshot protection stayed enabled on real accounts, so no physical-device pixel capture exists.
- The devices remained in their existing Chinese/light state. English/Chinese and light/dark pixel coverage is supplied by the non-sensitive production-component renderer rather than mutating user device settings.
- Physical UI trees prove real destination changes and bounds but do not expose selected tint pixels. Compose assertions cover selected semantics.
- Android Home automation is prohibited, so the smaller launcher icon is verified by source/packaged geometry and still needs the user's visual launcher check.

## Repository boundary

- Branch: `codex/nome-android-v656`
- HEAD: `6a01efa5e6d10dc0743cdf82dfb69e09cc459862`
- Official baseline: `59fce95d3cd08897b4ef742447b785cf2e56c7ce`, still an ancestor
- Preserve the historical dirty tree and debug manifest.
- Keep the index empty and `.gradle-review` untracked.
- No commit, push, signature, release, or frozen-evidence overwrite has occurred.

Evidence directory: `plans/evidence/20260723_nome_android_ui_fix_batch_03/`.
