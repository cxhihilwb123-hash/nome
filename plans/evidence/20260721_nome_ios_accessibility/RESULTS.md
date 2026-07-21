# Nome iOS Dynamic Type and accessibility preview results

Date: 2026-07-21
Branch: `codex/nome-ios-v656`
Starting HEAD: `27eb6e4100ec9caa9a9a755d8222e95f9e12c2b3`
Simulator: iPhone 17 Pro, iOS 26.5,
`95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`
App: `chat.simplex.app`, Nome build 337

## Verdict

PASS for preview-only Dynamic Type layout. This does not prove real-core
communication, physical-device VoiceOver, signing or App Store readiness.

## Confirmed and resolved defects

1. Onboarding trust pills truncated their visible meaning at
   `accessibility-large`. They now stack and use intrinsic multi-line height.
2. The conversation trust card was compressed while children and the custom
   message viewport drew outside their slots. The card now preserves intrinsic
   height and the message viewport is clipped to its assigned layout boundary.
3. The disappearing-message prompt truncated at maximum size and used an
   unlabeled 28-point close target. It now wraps and exposes a labeled 44-point
   dismiss target.

## Accepted screenshot evidence

- `design/migration/accessibility-large-light-fixed-20260721`: 14 light
  `accessibility-large` states.
- `design/migration/accessibility-large-dark-fixed-20260721`: 14 dark
  `accessibility-large` states.
- `design/migration/accessibility-max-light-fixed-20260721`: seven light
  `accessibility-extra-extra-extra-large` critical states.
- `design/migration/accessibility-max-dark-fixed-20260721`: seven dark maximum-
  size critical states.
- The conversation row in each matrix is superseded by the matching
  `design/migration/accessibility-conversation-*-accepted-20260721` capture.
- `design/migration/accessibility-conversation-normal-light-accepted-20260721`
  proves the normal `large` conversation layout remains intact.

Every accepted manifest records 1206x2622 screenshots above the byte floor.
Each matrix has unique screenshot hashes. Every settings file restores:

- appearance: `light`;
- content size: `large`;
- increased contrast: `disabled`.

Key before-fix images are in `before-fix/`. Full failed intermediate capture
directories were moved without deletion to
`/private/tmp/nome-ios-accessibility-intermediate-20260721` to keep the
worktree reviewable.

## Build and runtime proof

- Three focused incremental simulator builds were required because full-
  resolution visual evidence disproved two incomplete layout assumptions.
- Each final Xcode invocation used full Xcode, existing DerivedData and no Core
  preparation or dependency installation; all three successful compile passes
  exited 0.
- The final build was installed on the selected simulator and all accepted
  captures came from that installed app.
- `serve-sim` reported `running:true` on `http://127.0.0.1:3200`; the preview
  page returned HTTP 200 and a live MJPEG frame was saved as
  `serve-sim-live-frame.png` (SHA-256
  `2ebb1497e52f8191a480cea5d18a12791be98b62e778b3e37487353e7685b603`).
- The scoped `serve-sim` stream was disconnected after capture.

## Automated verification

- `scripts/ios/test-capture-nome-accessibility-previews.sh`: PASS.
- `scripts/ios/test-check-ios-device-readiness.sh`: PASS.
- `scripts/ios/check-nome-brand-copy.sh`: PASS.
- `scripts/ios/check-nome-design-coverage.sh`: PASS.
- `git diff --check`: PASS.
- Live `scripts/ios/check-nome-ios-readiness.sh --allow-blockers`: 46 PASS,
  5 WARN, 8 known BLOCKED, 0 FAIL. Report:
  `/private/tmp/nome-ios-readiness-accessibility-live-20260721`.
- Visual verdict: 94, PASS. See `visual-verdict-after.json`.
- Final adversarial review: two consecutive `ZERO_ISSUES` passes.

## Boundaries

- Android worktree and active test state were not mutated. Each iOS build was
  preceded by a process check showing only idle 0.0% Gradle/Kotlin daemons.
- No Haskell/native Core, protocol, database or message-state source changed.
- Physical-device VoiceOver and real two-account messaging remain unchecked.
- Evidence was finalized before the local checkpoint commit; no remote push was
  made.
