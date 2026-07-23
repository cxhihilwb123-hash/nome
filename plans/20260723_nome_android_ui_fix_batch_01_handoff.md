# Nome Android UI Fix Batch 01 — Handoff

Date: 2026-07-23

## Current verdict

The Android UI/navigation implementation is complete and builds cleanly. The full-regression production APK passed two-device cold launch, real Home/Contacts/Settings taps, P22/P23 routing, top/system back, and Simplified Chinese/English plus light/dark configuration checks. After user review, P07/P23 header Logo width was increased from 66 dp to 84 dp; that final APK passed fresh cold P07 plus real P07 -> P23 -> P07 smoke checks on both devices. Both devices are on system Chinese/light and were left at P07 Home for inspection.

Functional two-device production-route acceptance is closed. Two evidence limits remain explicit: screenshot protection prevents pixel-level Logo capture, and both Vivo devices reproducibly block the Compose instrumentation test Activity after the runner enters the requested navigation method. Do not convert either gap into a false pixel-sharpness or direct Compose-boundary PASS.

## Frozen repository baseline

- Repository: `/Users/forkman03/project/nome/simplex-chat`
- Branch: `codex/nome-android-v656`
- HEAD: `6a01efa5e6d10dc0743cdf82dfb69e09cc459862`
- Official v6.5.6 baseline: `59fce95d3cd08897b4ef742447b785cf2e56c7ce`
- Baseline remains an ancestor of HEAD.
- Preserve the pre-existing dirty tree.
- Keep the index empty and `.gradle-review` untracked.
- Preserve the historical dirty debug manifest; expected SHA256 is `b6aa278d102675e777a4a0eac66cb9fd049209bb1998cdab247dfc7b56d792f3`.

## Implemented behavior

### P07 Home

- A shared 76 dp Home/Contacts/Settings bottom bar is stable under the main content and carries its own navigation-bar inset fallback.
- Home clears the active filter.
- Contacts selects the existing `PresetTagKind.CONTACTS` route.
- Settings opens the existing Android `SettingsView` modal.
- The top-left brand now uses `NomeBrandLockup`, not `ChatBubbleOutline`, and is 84 dp wide.

### P22/P23

- Both reuse `NomePrimaryBottomNavigation`; Settings is selected.
- P23 Home/Contacts update the existing filter and close its settings modal.
- Android P22 is opened with real Home/Contacts callbacks.
- P22 Home/Contacts close the modal stack before returning to P07.
- P22 top back and Android system back both invoke the same close callback.
- P23 uses the same 84 dp header lockup as P07.

### Touch and accessibility

- Each tab uses `Role.Tab`, localized content description, and selected state description.
- Each item fills the 76 dp navigation row and retains the project 48 dp minimum.
- `selectable`/semantics are intentionally outside the size modifiers. Do not reorder them back.
- Vivo UIAutomator clips bottom nodes against its reported root bound, yielding 112 px on V2048A and 135 px on V2047A. Real taps succeed and source/test assertions require 76/48 dp, but direct Compose device proof is unavailable because both phones block the test Activity.
- P22 Back is independently measured at exactly 48 dp on both devices: 126 px at 420 dpi and 144 px at 480 dpi.

## Logo authority and output

Authority: `/Users/forkman03/project/nome/design/design/brand/README.md` and its approved local assets. No network asset and no invented geometry were used.

- day: `drawable-nodpi/nome_header_logo.png`, 700 x 285 RGBA, SHA256 `ffe97ea23fa157ad77e2e6f2054cf998108f75e22d2c1a31d7558ffdf7153cac`
- night: `drawable-night-nodpi/nome_header_logo.png`, 700 x 285 RGBA, SHA256 `4b346151e26cc03d48c0c8d8d99957e1ab3d8514afb150dc50d4ba379ed863e9`
- renderer: 700:285 aspect ratio, `ContentScale.Fit`, width-only call sites
- P07/P23 presentation: 84 dp wide and approximately 34 dp high; onboarding/About/lock-screen sizes unchanged
- non-sensitive light/dark composites are in the new evidence directory

The resources have transparent corners and no opaque rectangular plate. Protected device screenshots were not taken; user-visible high-density pixel inspection remains required.

## Build evidence

Successful gates include:

- Android debug compile/assemble and Android test APK assembly
- Desktop compile boundary
- 54 JVM tests with zero failures/errors/skips
- release lint with zero Error/Fatal issues and no changed-file warnings
- final APK SHA256 `a71382e0c8af08143acf5b7efcaea0f087185684ad74a5a1d2f9787a4def757b`
- Android test APK SHA256 `969d4ad6c0786b4f79ca9b03aab29292f7a284b1d74c5b9ac343fb0af6dabb4b`

The first aggregate run hit a Kotlin GC OOM in unchanged `ChatItemInfoView.kt`. The exact same gates passed after using a process-only 6 GB heap and one worker; no Gradle configuration file was changed.

## Final real-device evidence

V2048A `3106403166006XM`, 420 dpi override:

- final package last update: `2026-07-23 14:11:19`
- cold launch: PASS
- P07 Home/Contacts/Settings real taps and selected states: PASS
- P23 fixed rows, bottom Home, and Android system Back: PASS
- P22 fixed content, 48 dp top Back, and bottom Contacts: PASS
- zh-CN/en plus light/dark fixed labels and Logo node: PASS
- final 84 dp Logo targeted smoke: P07 221 x 90 px; real P07 -> P23 -> P07 PASS
- restored to system Chinese, empty per-app locale override, and light: PASS

V2047A `9590146717002S1`, 480 dpi:

- final package last update: `2026-07-23 14:12:07`
- cold launch: PASS
- P07 Home/Contacts/Settings real taps and selected states: PASS
- P23 fixed rows, bottom Home, and Android system Back: PASS
- P22 fixed content, 48 dp top Back, and bottom Contacts: PASS
- zh-CN/en plus light/dark fixed labels and Logo node: PASS
- final 84 dp Logo targeted smoke: P07 252 x 103 px; real P07 -> P23 -> P07 PASS
- restored to system Chinese, empty per-app locale override, and light: PASS

The test APK installed on both. On each phone the runner entered `primaryNavigationExposesThreeTabsAndDispatchesRealDestinations`, but no test Activity became focused and no assertion result was emitted. Both identical stalls were manually stopped.

All `/sdcard/nome_batch01_*.xml` temporary UI trees were deleted from both devices.

## Remaining decision / evidence gap

1. The user may visually inspect Logo sharpness, transparency, and lack of a white plate on both protected screens. Automation must keep screenshot protection enabled.
2. If direct Compose-boundary proof becomes mandatory, use a non-Vivo device/emulator or diagnose Vivo instrumentation launch policy in a separate infrastructure scope. Do not report the current blocked runs as PASS or FAIL.
3. If the user accepts the protected-screen visual result, the next action is a carefully scoped local commit containing only this batch. Do not push without separate authorization.

## Prohibited continuation

Do not commit, push, sign, publish, reset, clean, stash, track `.gradle-review`, change frozen evidence, touch native/Haskell/core/protocol/database/iOS/communication behavior, bypass screenshot protection, or automate lock/password/sensitive permission operations.

## Evidence

See `plans/evidence/20260723_nome_android_ui_fix_batch_01/`.
