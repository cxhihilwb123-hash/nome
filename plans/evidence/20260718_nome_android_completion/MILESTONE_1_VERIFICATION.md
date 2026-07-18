# Nome Android Milestone 1 verification

Date: 2026-07-18
Milestone: startup and trust, P01–P06
Branch: `codex/nome-android-v656`
Input HEAD: `b714f78efc7b3cc8c4e252d6351c4b629f60e277`
Verdict: `PASS — ELIGIBLE FOR LOCAL MILESTONE CHECKPOINT`
Distribution status: `NOT FOR DISTRIBUTION`

This report applies the amended execution contract in the sole completion ledger. It does not
repeat the frozen Foundation, P07/P08, or P13 gates, and it does not mark the overall completion
Goal complete.

## Product and visual result

- P01 retains the official database-root branch order and result ownership. It does not display a
  fabricated percentage, timeout, rollback, restore, or successful-open fact.
- P02 remains fail-closed for unavailable, cancelled, and failed authentication. The lock screen
  uses the actual local identity and configured authentication mode; underlying Home semantics are
  absent while locked.
- P03–P06 retain the official onboarding, migration-entry, create-user, operator/notification,
  conditions, and accept owners. `FIRST_USE` remains the official onboarding/root state.
- The approved raster Nome header logo is used in production. The P03 page no longer relies on a
  text approximation of the logo.
- The final API 35 Chinese/light/reference-size side-by-side files are:
  - `P02-side-by-side-system-unavailable-390x844-v2.png`,
    SHA-256 `44b62d441c0a00e7f95271868eb94eed73e62968215c8ac69152ad339f7f668d`;
  - `P03-side-by-side-390x844-v2.png`,
    SHA-256 `18d8f3f4b0522f9fb91709e968438e934738ad57b1251b26886ec017fd038f61`;
  - `P04-side-by-side-390x844-v4.png`,
    SHA-256 `348c7056fc0700458f55631836c795c290503ead5f95210977bfde232161d432`;
  - `P05-side-by-side-390x844-v3.png`,
    SHA-256 `92edeee1b005414d2cc5aaf447d9e77c6f39ab4e11e3e367298be182ad1c7c5d`;
  - `P06-side-by-side-390x844-v5.png`,
    SHA-256 `2da698ba5e2b4e532842cd35e738d8af080d94c69d9a4bab2e59e36585f3ba18`.
- P05 intentionally shows real operator configuration without reproducing the reference's
  unsupported connectivity/server-health claim or fake toggles. P06 retains the real
  public-channel non-E2EE disclosure. Those are truth-preserving differences, not omitted visual
  work.

## Build and regression

The repository's Gradle configuration excludes a Desktop emoji accessor whenever any requested
task name contains `assemble` or `release`. Android and Desktop were therefore run as two clean
invocations; no build setting was changed.

- Android:
  `:android:testDebugUnitTest :android:lintRelease :android:assembleDebug
  :android:assembleDebugAndroidTest :android:assembleRelease`
  — `BUILD SUCCESSFUL in 3m 5s`, 196 tasks.
- Desktop: `:common:desktopTest` — `BUILD SUCCESSFUL in 44s`.
- Focused locale and unlock-policy tests passed before the aggregate runs.
- `git diff --check` passed.

## API 28/33/35 device matrix

All results below use disposable emulators; frozen API 28/API 35 clients and the v6.5.4 upgrade AVD
were not cleared.

- API 28: clean Nome first start reached the Chinese production onboarding and created real
  `files_chat.db` and `files_agent.db`; zero target fatal/database terminal was observed.
  The 30 API-applicable instrumentation tests passed. Four API-35-only screenshot tests were
  excluded by exact class name after the initial unfiltered run proved their explicit API guard.
- API 33: clean first start created both databases, real onboarding created `NomeM1Api33`, the
  notification notice and OS permission-denied path were exercised, and Home was reached with
  `NotificationsMode=OFF`. The 30 API-applicable instrumentation tests passed.
- API 35: clean first start created both databases and reached production onboarding. The complete
  instrumentation suite passed `OK (34 tests)` in 455.836 seconds.
- P01 high-risk API 28/API 35 real database/key/backup/lifecycle fixtures and the authentic
  same-package v6.5.4-to-current upgrade remain passed from the implementation gate.
- P02 high-risk API 28/API 35 system-unavailable lifecycle checks and API 35 official-passcode
  wrong/cancel/success/background/cold/rotation checks remain passed.

## Language, theme, scaling, touch, and TalkBack

- P01's 14 renderer states were captured on API 35 for `zh-CN` and English, light and dark, and
  100% and 200% font scale: 112 unique captures. Its 200% password/action, live-region, and 48dp
  contracts passed.
- Each P02–P06 production family was then exercised in all eight combinations of `zh-CN`/English,
  light/dark, and 100%/200% font scale.
- P02 retained title, actual identity, primary system-auth action, retry action, and local-data
  note in every combination. Underlying Home matches were zero in every accessibility tree.
- P03 retained title, both official entry actions, and a scrollable body when 200% required it.
- P04 retained back, identity preview, display-name input, local-only explanation, and primary
  action in every combination.
- P05 retained real operators, evidence-level disclosure, all configuration rows, and the primary
  action; its body became scrollable at 200%.
- P06 retained the non-E2EE note and disabled accept action at initial render. At English 200%, a
  real scroll exposed the consent checkbox and terms action; checking consent changed the accept
  action to enabled.
- Reference-size primary buttons measured 126–127 px high at 420 dpi, satisfying the 48dp minimum;
  200% controls were taller and remained reachable.
- The installed TalkBack service
  `com.google.android.marvin.talkback/.TalkBackService` was bound with touch exploration enabled.
  P02–P06 real production accessibility trees exposed their headings, controls, state/selection
  semantics, and expected reading order; P02's first focus highlight was visible on the Nome logo.
  P01's four focused accessibility tests also passed while TalkBack was bound.
- Cleanup restored TalkBack/accessibility off, font scale `1.0`, light mode, and no enabled
  accessibility service. The screenshot-only privacy override was absent after clean install; a
  post-cleanup production screenshot had RGBA mean `0,0,0,1`, proving screenshot protection was
  active.

## Historical evidence

The four frozen evidence roots have zero worktree status lines. Their current `SHA256SUMS` files
are byte-identical to the P13 checkpoint. Every listed object was independently read from
`b714f78efc7b3cc8c4e252d6351c4b629f60e277` and rehashed without a checkout:

- Phase 1: 48/48;
- Phase 2 Foundation: 131/131;
- Phase 2 Batch 2 Home: 254/254;
- Phase 2 Batch 3 P13: 139/139.

No historical evidence file was changed.

## Release isolation and security

- ARM64 unsigned release:
  `301832769` bytes,
  SHA-256 `71abbe846472b8ab38fa3c5b9d269248ce2f16c27678e3f998a48cc675fa1260`.
- ARMv7 unsigned release:
  `287773873` bytes,
  SHA-256 `2f5f9394ae7b05b607ae7b6cd55dea634bdd1173c198b23047a0aab2d2ae249a`.
- Actual release manifest: package `chat.simplex.app`, version `6.5.6` / 358, min API 28, target
  API 35, non-debuggable, non-test-only, unsigned.
- Release archive and seven DEX files contain zero Nome evidence activity, Android test class,
  `.nome.dev` package, workspace/evidence path, side-by-side/screenshot filename, bearer literal,
  private-key marker, token, or JWT-shaped value.
- The two `android.permission.DUMP` exported receivers are the stock permission-protected
  AndroidX WorkManager diagnostics and ProfileInstaller receivers. The removed Nome lifecycle
  receiver and all Nome debug evidence activities are absent.
- DEX contains bare `localhost` and `127.0.0.1` library strings but zero HTTP/WebSocket local-debug
  URL.
- Dirty-text scans found zero private key, GitHub token, JWT, bearer value, secret assignment,
  Android keystore blob, QR payload, or owner-signature value.
- Forbidden dirty paths are zero: no Haskell/native core, `Core.kt`, protocol, database schema,
  archive format, or iOS source is changed.
- The excluded unknown `apps/multiplatform/Screenshot_1784275771.png` remains unmodified and must
  not enter the checkpoint.

## Release-readiness finding retained for later milestones

The current unsigned release still uses package `chat.simplex.app`, launcher label `SimpleX`, the
`simplex` URI scheme, and official SimpleX App Link hosts. This is the measured artifact state, not
a Nome distribution identity. No production signing key, Nome domain ownership, App Link
verification, or store record is authorized in this Goal. The artifact therefore remains
`NOT FOR DISTRIBUTION`.

This finding does not invalidate the Milestone 1 startup/trust implementation or release
debug-fixture isolation. It is an explicit downstream blocker for the final application-identity,
App Links, brand/legal, privacy, and distribution audit and must be resolved or carried as an
external release condition before final RC.

## Review and checkpoint decision

The ordinary implementation review's two findings—the inaccessible Home profile control and
wrapped P06 disclosure—were fixed before this gate. The milestone risk review found no remaining
P01–P06 product, truth, lifecycle, accessibility, build, historical-evidence, or release-isolation
issue. The measured release identity/App Links state above is retained as a downstream
release-readiness item rather than misreported as complete.

Milestone 1 is eligible for one local checkpoint. No push, publication, production signature, or
remote mutation is authorized.
