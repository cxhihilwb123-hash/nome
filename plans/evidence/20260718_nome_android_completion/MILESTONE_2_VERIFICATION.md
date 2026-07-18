# Nome Android Milestone 2 verification

Date: 2026-07-18
Milestone: Home and discovery, frozen P07/P08 plus P09/P10 and the P02–P06 visual correction
Branch: `codex/nome-android-v656`
Input HEAD: `4260545d96d84055efb281cb5a9b3226a498cb07`
Verdict: `PASS — ELIGIBLE FOR LOCAL MILESTONE CHECKPOINT`
Distribution status: `NOT FOR DISTRIBUTION`

This report applies the amended execution contract in the sole completion ledger. It concentrates
the API, language/theme/scaling, TalkBack, regression, historical-evidence, and release checks once
for Milestone 2. It does not repeat frozen functional/security gates and does not mark the overall
completion Goal complete.

## Product and visual result

- Frozen Foundation and P07/P08 behavior remains the Home state/data baseline. The small adjacent
  Home correction makes the already-owned current-profile control open the official `UserPicker`;
  it introduces no new state or data owner.
- P09 is a production route over the official loaded-chat name filter. The baseline message-result
  and recent-query regions are retained with truthful scope and retention copy; no global message
  aggregation, persisted recent query, synthetic contact, online state, or success fact is shown.
  `FILTERED_NO_RESULT` is reachable only for a nonblank query over an available loaded base whose
  official filtered result is empty. It does not rewrite P08 or `FIRST_USE`.
- P10 projects the actual current local profile and dispatches one-time invitation, scan/paste,
  create-group, and create-channel actions one-to-one to their official callbacks. It adds no
  cloud-account, connection-health, or success claim.
- Accepted API 35 Chinese/light/reference-size production and side-by-side evidence:
  - P09 production SHA-256
    `529d500e8c00f6ad95236d7dff0791e15c1184ae8cae6ccdc22794bf1903d2c9`;
    side-by-side
    `cca3f1452e234c0c7d910045559354e5dbe8464e99d075b5e9cb6bdd2a8e0b5c`;
  - P10 production SHA-256
    `f50ab2bf45c6d55fe48112de3caaafc0d504590f8457c8928913b7eddb97dff1`;
    side-by-side
    `812bd9e2c3152a34596617e5843bc0bfec01572d5819f03e14920015964ec87f`.
- The combined comparison images were the visual QA input. P09 matches the baseline composition,
  header/search/result-region order, information hierarchy, density, typography, spacing, corners,
  icons, and result action sizing while retaining truthful official scope. P10 matches the
  baseline header, profile card, disclosure, and four-action geometry; the actual controlled
  profile has no image, so its truthful initial fallback replaces the reference portrait.
- The post-clarification P02–P06 comparison is also closed in this milestone:
  P02 v4, P03 v5, P04 v6, P05 v6, and P06 v8. The production wordmark, page composition, region
  placement, hierarchy, typography, spacing, corners, icons, action sizing, and density were
  corrected against the page-level visual acceptance baselines without changing authentication,
  onboarding, operator, conditions, migration, or Desktop owners.

## Build, lint, and regression

Android and Desktop were run in separate sequential invocations because the repository build
configuration excludes a Desktop emoji accessor whenever requested task names contain `assemble`
or `release`. No project build setting changed.

- Android:
  `:android:testDebugUnitTest :android:lintRelease :android:assembleDebug
  :android:assembleDebugAndroidTest :android:assembleRelease`
  — `BUILD SUCCESSFUL in 4m 8s`, 196 tasks.
- Android JVM suite: 39 tests, zero failure/error/skip.
- Release lint: zero errors, 40 warnings. The warnings are the existing resource, SDK, and
  deprecation set; no fatal issue was introduced.
- Desktop: `:common:desktopTest` — `BUILD SUCCESSFUL in 7s`; 24 tests, zero
  failure/error/skip.
- P09/P10 focused instrumentation passed 15/15 before the aggregate matrix. P02–P06 shared
  foundation coverage passed 5/5 after the visual correction.
- `git diff --check`, forbidden-path, and changed-text sensitive-value scans passed.

## API 28/33/35 device matrix

The disposable matrix used current debug and instrumentation APKs. The four screenshot-only
classes were excluded by exact class name on API 28/API 33; the complete suite ran on API 35.

- API 28 `emulator-5562`, Android 9 / API 28, ARM64:
  `OK (35 tests)` in 197.043 seconds.
- API 33 `emulator-5564`, Android 13 / API 33, ARM64:
  `OK (35 tests)` in 52.434 seconds.
- API 35 `emulator-5560`, Android 15 / API 35, ARM64:
  complete suite `OK (39 tests)` in 674.819 seconds.
- The API 35 suite includes the Foundation, Home, connection-preview, and database-root screenshot
  classes plus the P09/P10 production boundary tests.
- Argument-gated destructive database and real Home core-cycle routes were not activated by the
  aggregate command. Their already-frozen high-risk fixture evidence was reused rather than
  manufacturing a new result.

## Language, theme, scaling, touch, and TalkBack

- The controlled populated API 35 production client exercised Home, P09, and P10 in all eight
  combinations of Chinese/English, light/dark, and 100%/200% font scale.
- Home retained its current-profile, search, list, and new-connection controls in every
  combination. P09 used actual loaded `AddressSender28` or `Sender` matches and retained its
  truthful message-scope and recent-retention regions. P10 retained all four official actions,
  including the necessary scroll at 200%.
- Real TalkBack
  `com.google.android.marvin.talkback/.TalkBackService` was bound with touch exploration enabled.
  Its focus/double-tap gesture opened P09 from Home and activated the P09 close, query, and idle
  controls. It then opened P10 and activated close, profile, add-contact, scan/paste,
  create-group, and public-channel controls.
- Cleanup restored accessibility and touch exploration to `0`, enabled accessibility services to
  `null`, font scale to `1.0`, light mode, and empty per-app locale override. TalkBack's temporary
  notification permission is revoked with its original prompt-sensitive flag restored.
- The production Home tree after cleanup exposes the actual identity, Home heading, search, and
  new-connection controls in Chinese. The current production window reports `SECURE`; an ADB
  framebuffer capture is a single `srgba(0,0,0,1)` color. Exact screenshot-fixture preference
  hits are zero.

## Historical evidence

Every manifest-listed frozen object was read from the P13 checkpoint
`b714f78efc7b3cc8c4e252d6351c4b629f60e277` and independently rehashed without checkout:

- Phase 1: 48/48;
- Phase 2 Foundation: 131/131;
- Phase 2 Batch 2 Home: 254/254;
- Phase 2 Batch 3 P13: 139/139.

The worktree contains legitimate later product/source/report changes, so historical verification
uses the checkpoint object rather than comparing those objects with current bytes. No historical
evidence file was edited.

## Release isolation and security

- ARMv7 unsigned release:
  `287881969` bytes,
  SHA-256 `c95b640ac95eef529d7d233738066f7b41ffa9928c8bc98ce35a65b3fa4bad3b`.
- ARM64 unsigned release:
  `301940865` bytes,
  SHA-256 `b41bc7f80991cbf78b2cc53f2b75b1286e07001a025740acd56214b98b9ffc91`.
- Actual ARM64 manifest/badging remains package `chat.simplex.app`, version `6.5.6` / 358,
  min API 28, target API 35, launcher label `SimpleX`, ARM64 native libraries, and unsigned.
  `apksigner verify` correctly rejects the unsigned artifact; no signing was attempted.
- Both APK archives contain zero evidence/workspace/screenshot/private-key/token-shaped filename
  hits. Manifest scans contain zero test/evidence/workspace hits. DEX exact scans contain zero
  evidence path, workspace path, private-key marker, bearer, JWT-shaped value, or case-sensitive
  GitHub token.
- The ARM64 archive contains 1,722 entries and seven DEX files. No debug/test/evidence/dev
  manifest reference was found.
- Dirty text contains zero case-sensitive GitHub token, bearer, JWT-shaped value, or private-key
  marker. Forbidden changed paths are zero: no Haskell/native core, `Core.kt`, protocol, database
  schema/semantics, archive format, or iOS source is changed.
- The excluded unknown `apps/multiplatform/Screenshot_1784275771.png` remains untouched with
  SHA-256 `d2463deaf565b7fe11e96661a79d6e5a340a8568d86302e81f2fecc981ab5514`
  and must not enter the checkpoint.

## Release-readiness finding retained

The current unsigned release still uses package `chat.simplex.app`, launcher label `SimpleX`,
the `simplex` URI scheme, and official SimpleX App Link hosts. This is measured artifact state,
not a completed Nome distribution identity. No production signing key, Nome domain ownership,
App Link verification, store publication, or remote mutation is authorized. The artifact remains
`NOT FOR DISTRIBUTION`.

This finding does not invalidate the Milestone 2 presentation, routing, regression, or isolation
result. It remains a downstream application-identity, App Links, brand/legal, privacy, and local
distribution-audit item for final RC.

## Review and checkpoint decision

The ordinary P09/P10 review initially identified three presentation/test findings. Truthful P09
structural regions, the real P10 profile-image path, assertions, and existing route-boundary tests
resolved all three; its final result is `ZERO ISSUES / APPROVE`.

The post-clarification P02–P06 targeted review then inspected the final rendered-page corrections,
accepted comparison images, official-owner boundaries, and synchronized product/spec/evidence
records. It returned `ZERO ISSUES / APPROVE` with no source or evidence change.

Milestone 2 is eligible for one local checkpoint. No push, publication, production signature, or
remote mutation is authorized.
