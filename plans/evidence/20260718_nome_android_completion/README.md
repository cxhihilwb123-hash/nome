# Nome Android Completion Ledger

Date: 2026-07-18
Status: active
Authority: user execution-contract amendment of 2026-07-18, then `plans/20260718_01.md` where not
superseded

This is the sole execution ledger for the active Nome Android completion Goal. Resume from the
unique next action below; do not re-plan the whole route or repeat frozen batches.

## Execution-contract amendment — 2026-07-18

The user's current instruction supersedes the earlier requirement that every page/batch run a full
device matrix, produce a large standalone evidence bundle, receive two `ZERO ISSUES` rounds, and
create its own checkpoint.

- Product delivery is global Android reskin plus official v6.5.6 feature fidelity. P01–P24
  designs are page-level **visual acceptance baselines**. The official v6.5.6 product remains the
  functional, data, route, state, and interaction-logic baseline; unsupported facts in a design
  still cannot become production state.
- Foundation, P07/P08, P13, and their checkpoints remain frozen and are not repeated.
- Shared theme, tokens, components, and layouts are the primary implementation path for color,
  typography, spacing, buttons, inputs, dialogs, lists, navigation, icons, and brand consistency.
- Startup, Home, connection, chat, identity, and settings are the priority flows. Other reachable
  surfaces are closed through shared components in merged groups of two to four page families.
- An ordinary UI group requires focused tests, compile, API 35 production smoke, affected
  visual/accessibility inspection, and one review. Each completed page family must also capture at
  least one primary production state on API 35 at the reference language and viewport, compare it
  side by side with the corresponding design, and correct obvious differences in composition,
  region placement, hierarchy, color, typography, spacing, corners, icons, action sizing, and
  content density.
- Database, authentication, security verification, backup/migration, file, and call work remains
  high risk and retains real fixtures, lifecycle checks, and API 28/API 35 depth.
- API 28/33/35, full bilingual light/dark, 200%, TalkBack, historical manifests, release build, and
  full regression are concentrated at milestones and final RC.
- Ordinary batches do not create large evidence roots or checkpoints. This ledger, the page
  completion matrix, and milestone/final verification reports are the durable records.
- Local checkpoint commits are milestone-owned. Final RC alone receives the final same-summary two
  consecutive `ZERO ISSUES` reviews.
- Unsupported percentage, restore, timeout, online, security, or success facts remain forbidden.
- No push, release, production signing, real-user-data destruction, Haskell/native core, `Core.kt`,
  protocol, database semantics, archive format, or iOS change is authorized.

`plans/20260718_02.md` and its Gate B review record are retained as historical high-risk analysis
and marked `SUPERSEDED BY USER EXECUTION-CONTRACT AMENDMENT`. Their repeated review loop,
per-state screenshot completeness, standalone Gate G review pair, and per-batch checkpoint no
longer block the route. Findings returned by the old-plan reviewer after this amendment are
historical input only. P01 remains high risk, so its real database/key/backup/migration fixtures and
lifecycle/API 28/API 35 depth are still required.

### Visual-baseline clarification — 2026-07-18

The user clarified that shared theme/components are an implementation accelerator, not a substitute
for page-level fidelity. A production page may restructure its Android presentation to match the
corresponding P01–P24 baseline while preserving the official route, state, action, data, and core
owners. A baseline structure cannot be skipped merely because the upstream screen is composed
differently. Pixel-perfect reproduction and the former per-state heavy matrix are not required.
P01 and all completed security/database/official-fidelity work remain intact. Starting with active
P02–P06, no page family can be marked ready until its principal API 35 production capture has been
compared with the reference at the matching language/viewport and obvious differences have been
corrected. The earlier P02–P06 onboarding screenshots predate this clarification and are diagnostic
inputs only, not closure evidence.

## Current milestone and batch

- Milestone: 1 — startup and trust.
- Batch: Milestone 1 checkpoint preparation — the concentrated P01–P06 gate passed.
- Validation tier: milestone complete — P01/P02 high-risk depth checks, P03–P06 page-level visual
  acceptance, full Android/Desktop regression and release build, disposable API 28/33/35,
  bilingual light/dark, 100%/200%, 48dp, real TalkBack binding, historical manifests, release
  isolation, privacy restoration, and one risk review all passed. Exact results are in
  `MILESTONE_1_VERIFICATION.md`.
- Scope: Android-only, local-only; no push, release, production signing, native/core, protocol,
  database/archive, message-state-machine, or iOS changes.
- Frozen inputs: the v6.5.6 baseline, Phase 1, Phase 2 foundation, Batch 2, and P13 checkpoint
  evidence remain read-only.
- `FIRST_USE` remains official onboarding/root owned.
- `FILTERED_NO_RESULT` remains deferred until P09 has a real producer.
- Page completion matrix:
  `plans/evidence/20260718_nome_android_completion/PAGE_COMPLETION_MATRIX.md`.

## Repository recovery

- Branch: `codex/nome-android-v656`
- HEAD: `b714f78efc7b3cc8c4e252d6351c4b629f60e277`
- Baseline: tag `v6.5.6` at `59fce95d3cd08897b4ef742447b785cf2e56c7ce`, an ancestor of HEAD.
- Origin: `https://github.com/simplex-chat/simplex-chat.git`
- HEAD commit: `checkpoint(nome-android): freeze phase 2 batch 3 p13`
- Current reconciliation before Milestone 1 staging: staged `0`, dirty tracked `31`, untracked
  files `102`, conflicts `0`. The set consists of current Milestone 1
  source/product/spec/test/ledger/report and compact visual-baseline work plus the excluded unknown
  `apps/multiplatform/Screenshot_1784275771.png`; that unknown file remains untouched and excluded.
- Current Gate C tree: the allowlisted P01 Android/common/Desktop production, resource, and test
  paths plus this ledger, the P01 plan, and its evidence root are modified or new; staged/conflicted
  remain zero and the excluded screenshot remains untouched.
- Historical SHA manifests: Phase 1 (48 entries), Phase 2 foundation (131 entries), and Batch 2
  (254 entries) each verified from its declared working-directory convention.
- Forbidden dirty-scope audit: no Haskell/native, `Core.kt`, iOS, native binary, archive/schema, or
  protocol changes found at recovery.

## Toolchain and device surface

- Android Studio JBR 21.0.10 and Gradle 8.12 are usable.
- SDK platforms 26/33/35, build-tools 34/35, NDK 23.1.7779620, CMake 3.22.1, and ADB 37.0.0 are
  present.
- AVDs: API 28, API 33, API 33 clean RC, API 35, and API 35 clean RC.
- API 35 debug package cold-started successfully on `emulator-5554`.
- Free disk at P01 Gate A recovery: approximately 140 GiB.

## P13 closure-plan state

- Authoritative closure-plan digest:
  `1ba4f790c35df45e2cbf08913870c386f2b1f6c76ca5c5d6d6df023b3fba44cc`
- Gate B review round 1: `ZERO ISSUES`.
- Gate B review round 2: `ZERO ISSUES`.
- Review record:
  `plans/evidence/20260717_nome_android_phase2_batch3_p13/closure-plan-review.md`
- Earlier candidate digests are invalidated in that record.

## Current source state

- P13 source, tests, renderer evidence, and real-device traces are frozen at
  `b714f78efc7b3cc8c4e252d6351c4b629f60e277`.
- Both defects found during recovery are corrected:
  1. queued URI ingress now carries typed external/internal provenance, so only Android external
     `ACTION_VIEW` enters P13 and Desktop/internal verified routes retain the legacy path;
  2. each external attempt binds `(attemptId, userId, remoteHostId)`, supplies that immutable
     identity to typed plan/connect delegates, and rejects stale context before and after
     suspension.
- The Android P13 presentation now retains display-safe reducer state and its owning coroutine
  scope across configuration change, ignores rapid duplicate submit atomically, cleans up on
  terminal dismissal, and does not retain bearer/profile/server/response data.
- A payload-free Compose focus-restoration contract now returns focus to Home's existing
  current-profile control after P13 modal teardown. It changes no Home state producer and stores no
  label, profile, URI, bearer, host, server, or response.
- The temporary `android.permission.DUMP`-protected lifecycle receiver and its manifest entry were
  removed after the real switch/cancellation traces closed.
- A concurrent task added a process-global `commonMain` command counter and claimed tests in an
  overwritten ledger. The code was audited and rejected: it entered release/Desktop, lacked an
  attempt identity and terminal lifecycle, exposed a mutable sink, counted before the native
  command boundary, and its tests called only the counter itself.
- The rejected counter and its direct tests were removed with targeted patches. Its observed build
  artifacts are not accepted as Goal verification.

## Open P13 gates

- None. P13 is frozen at `b714f78efc7b3cc8c4e252d6351c4b629f60e277`; all
  product/runtime/review/manifest/checkpoint gates are closed.

## P01 Batch 1A state

- Gate A read-only recovery is complete against the P13 checkpoint.
- Historical heavy batch plan: `plans/20260718_02.md`, now
  `SUPERSEDED BY USER EXECUTION-CONTRACT AMENDMENT`.
- Evidence root: `plans/evidence/20260718_nome_android_batch1a_p01/`.
- The first 542-line plan digest `e6d4378e…` was rejected in Round 1 for an unclassified pre-result
  Keystore failure, contradictory backup copy/open semantics plus raw legacy alerts, and an
  unstated auth guard. It is invalidated.
- The 665-line digest `0d919fe1…` was then rejected because a missing Keystore alias returns null
  and can reach `ErrorNotADatabase`; initial-random ownership must still override alternate-key UI.
  It is invalidated.
- The 687-line digest
  `862aee5665687bd679514d49ea4913305e0d5a19777c58fd1f8233d5caf4ff1c`
  was rejected because the sanitized Android route omitted the existing success hook and allowed an
  impossible manual-key null-status branch. It is invalidated.
- Current frozen plan input: one 703-line plan, SHA-256
  `428e6c646d9e7fdba303dd7e926250c406f19147b7795550aa135d02bdc01068`
  was rejected for contradictory pre-delay Android opening and a missing entered-key
  no-persistence open helper. It is invalidated.
- Current frozen plan input: one 728-line plan, SHA-256
  `1599d958dd24322234c58f39ba87584d1c12f0c485c49e111a56063bf741dca8`;
  manifest verification `1/1 OK`.
- The current plan also freezes the minimal real migration producer as official v6.5.4
  `e92afb68…`, with hash-verified v6.5.4 native libraries and the same local debug signer; current
  v6.5.6 libraries are explicitly forbidden for that fixture.
- P01 reference: 470 × 936, SHA-256
  `d9b6524b4db29e6dd8b3dd464564c1fc665700c43d6f49a8eab3ffe860685e6c`.
- The plan preserves the reference's security-focused hierarchy but removes the unsupported 68%,
  staged completion, generic restore/rollback/timeout, and “messaging restored” claims.
- Gate C production integration is present but not frozen:
  - the three existing root branches delegate through one expect/actual route without changing
    their priority, auth guard, or existing one-second opening delay;
  - Desktop delegates the legacy renderer exactly once and installs no Nome behavior;
  - Android reduces fixed display-safe opening/migration/error/key/backup facts and never carries
    raw native payloads or key text;
  - the database-key platform seam publishes missing-alias/decrypt-failure class only for the
    database alias before the existing return/throw and clears after a successful read;
  - manual open-once and save-and-open are separate low-level actions, matched backup copy is
    revalidated and copy-only, and fresh `OK` retains the official Android success hook;
  - the renderer has bilingual fixed resources, safe insets/IME/scrolling, 48dp actions, heading
    and live-region semantics, and a non-saveable masked passphrase field.
- Product/spec/CODE reverse maps now describe the bounded P01 source, tests, debug reference, and
  unchanged Desktop fallback.
- A reviewer changed the 714-line candidate during its own review by threading the already-declared
  facts carrier into the route call. The resulting 715-line plan is coherent, but that in-review
  edit invalidates the reviewer's own approval. A new independent current-digest Round 1 changed
  no file and returned `ZERO ISSUES / APPROVE`; Round 2 then rejected the digest because its exact
  debug signer SHA was machine-derived rather than source-authoritative. The revised plan requires
  freshly computed old/current signer equality, treats the recovered SHA only as non-normative
  evidence, and aborts on mismatch without altering signing. Both revised-digest reviews are
  pending. The following 719-line revision was then rejected because its temporary v6.5.4 archive
  lacked the ignored `.nome.dev` package-suffix input and its matrix omitted authority-required
  API 33 smoke. The current plan adds a hash-recorded, temporary-only local-properties copy with
  suffix verification and exact-serial API 33 system smoke. Current-digest Round 1 returned
  `ZERO ISSUES / APPROVE`; independent current-digest Round 2 also returned
  `ZERO ISSUES / APPROVE`. No plan byte changed between them. Gate B is complete.
- The Gate B loop and its standalone per-batch Gate G/checkpoint contract are historical after the
  user amendment. The reviewed risk analysis still informs P01 fixture and lifecycle coverage.

### Current allowed source scope

The completed P01 high-risk work stayed within:

- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/App.kt`;
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/database/`;
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/{platform/Cryptor.android.kt,views/database/,ui/nome/database/}`;
- `apps/multiplatform/common/src/androidMain/res/values*/nome_database_root_strings.xml`;
- the exact Desktop fallback actual/test, Android P01 focused tests, and debug-only P01 evidence
  host/resources;
- synchronized `apps/multiplatform/{CODE.md,product/,spec/}`, this ledger, and the retained P01
  historical plan/evidence records.

The active fast reskin lane may modify the existing Android Nome tokens/theme/components,
the Android production shell, Android-owned presentation/layout/resources, and the smallest shared
presentation seams needed to reuse official route/state/action owners. Any `commonMain` seam must
retain an unchanged Desktop fallback and cannot add model/core/protocol/database truth. Forbidden
native/core/`Core.kt`/protocol/database/archive/iOS paths remain outside scope.

## P02–P06 Batch 1B result

- Status: `READY FOR MILESTONE`.
- P02 uses the real current local identity, actual configured `LAMode`, and existing
  `AppLock.runAuthenticate` result handling. System-unavailable, cancel, wrong passcode, and retry
  remain locked; only a real success authorizes. No reference portrait, fallback credential path,
  or success fact is fabricated.
- P03–P06 use Android presentation actuals over the official onboarding stage, create-user,
  operator/notification configuration, conditions, and migration-entry owners. Desktop actuals
  delegate the legacy renderer unchanged.
- The approved Nome header asset is hash-identical to the supplied prototype asset
  (`SHA-256 1c306154…`). The onboarding pages now match their visual acceptance baselines in
  composition, hierarchy, region placement, typography, density, controls, and footer placement.
- P05 displays actual selected operators and explicitly labels the card as configuration rather
  than connectivity/server health. It does not reproduce the design's unsupported connected/ready
  status or fake toggles. The missing back action is intentional because the official post-create
  route has no safe back transition that avoids creating a second user.
- P06 keeps the official conditions/accept operation, adds an explicit local consent checkbox, and
  preserves the fixed public-channel non-E2EE disclosure. Its compact disclosure typography was
  corrected after the first comparison so the production strip matches the baseline's single-line
  density at the reference viewport.
- The frozen P07/P08 implementation was not redone. A production-route review found one adjacent
  presentation regression: its current-profile control had a TalkBack label but no click action.
  The existing control now opens the already-owned `UserPicker`, restoring the official
  identity/settings route without changing Home state or data.
- Lock overlay semantics now hide underlying Home/fullscreen content from accessibility while
  authentication is active. Real API 28/API 35 lifecycle checks found no underlying Home nodes
  while locked, and active-call visibility ownership remains unchanged.
- One risk-oriented implementation review was completed. Its two actionable findings—the
  non-clickable Home profile control and the wrapped P06 disclosure—were fixed and reverified;
  no remaining issue was found in the ordinary-batch scope.

## Tests and evidence

- Recovery checks: branch/HEAD/tag ancestry/origin, precise dirty/staged/conflict counts, historical
  manifests, toolchain, AVD inventory, and remaining gates.
- P01 Gate A repeated those checks after the P13 checkpoint: Phase 1 48/48, foundation 131/131,
  Batch 2 254/254, and P13 139/139; JBR 21.0.10, Gradle 8.12, ADB 37.0.0, and all five API
  28/33/35 AVD definitions are present. API 35 `emulator-5554` and API 28 `emulator-5556` were
  online during recovery.
- Gate B: two independent reviews of identical plan bytes.
- P01 Gate C focused verification on the current implementation:
  - reducer and atomic attempt-gate unit tests — pass;
  - Android root/action/key-read boundary test — pass;
  - Desktop route contract for all three roots with both sensitive-content values — pass;
  - Android debug Kotlin compilation — pass;
  - Android instrumentation APK assembly — pass.
- Current P01 verification:
  - full Android unit suite — 36 tests, zero failure/error/skip;
  - full Desktop suite — 23 tests, zero failure/error/skip;
  - `:android:compileReleaseKotlin`, `:android:assembleDebug`, and
    `:android:assembleDebugAndroidTest` — pass;
  - exact-serial P01 Compose tests — API 28 `OK (4 tests)`, API 35 `OK (4 tests)`;
  - debug-only 14-state renderer reference matrix — API 35 `OK (1 test)`, 112 captures. Under the
    amended contract this is affected-visual input, not a completeness gate or real-database proof.
  - the old-plan reviewer later requested every layered backup/submitting visual variant and a
    dedicated packaging guard. Those are preserved as historical findings and do not block the
    amended rapid route. The lightweight non-exported debug packaging guard was adopted; exhaustive
    renderer expansion was not, and release absence is verified at the milestone.
  - disposable API 28 fresh production install/cold launch — real chat/agent databases created,
    official onboarding reached, zero fatal/database-error terminal counts;
  - disposable API 35 argument-gated production-route fixture — random and manual database-key
    alias missing/unreadable classes, wrong/correct entered key, root lifecycle passphrase clearing,
    exact matched chat/agent backup-pair copy, and explicit fresh reopen all pass in `OK (1 test)`;
    no key value is logged or retained as evidence;
  - the API 35 fixture left both current databases valid and byte-equal in size to their matched
    backups, reached official onboarding after reopen, emitted exactly two fixed
    `database key material unreadable` diagnostics, and emitted zero fatal target-process events.
  - authentic same-package upgrade fixture — official v6.5.4 source at
    `e92afb68d52bff0d2a035b3594d16badb3f58649` was built outside the worktree with the matching
    official v6.5.4 arm64/armv7 native libraries; its local debug APK was version 353, the current
    APK is 358, and both APKs had the same freshly measured Android debug signer SHA-256
    `b2fbf7616a337889d9aba6b4f3ad2680c8e83b8d4e25108f82068e873e7ae5b0`;
  - a brand-new API 35 `nome-p01-v654-upgrade` AVD completed the official v6.5.4 onboarding with
    synthetic local identity `NomeUpgradeFixture`, enabled `Confirm database upgrades` through the
    in-app Developer tools, and created real chat/agent databases before `adb install -r`;
  - the first current-version cold start displayed the production `Database update available`
    route. One activation of `Update and open` reached the current Nome Home with the same synthetic
    identity, a second cold launch did not repeat the consent route, and fatal/database-terminal
    Logcat counts were both zero;
  - focused packaging/release closure — the non-exported debug-host guard passed `OK (1 test)`;
    `:android:assembleRelease` passed in 2m 5s; release manifest, DEX, and resource scans each found
    zero P01 debug-host/test/fixture hits; `git diff --check`, forbidden-path, complete-bearer, and
    literal-secret scans passed.
- P02 high-risk production verification:
  - API 35 real SYSTEM mode with no enrolled device credential returned the platform
    unavailable outcome; cold start, retry, background/foreground, landscape recreation, and
    portrait restore remained locked with protected semantics isolated;
  - API 28 repeated the real SYSTEM-unavailable, retry, background/foreground, and rotation
    lifecycle paths and remained fail-closed with protected semantics isolated;
  - API 35 configured PASSCODE through the official Settings route, then verified wrong passcode,
    cancel, correct unlock, immediate background lock, cold process start, and rotation
    recreation. Wrong/cancel stayed locked and only the correct synthetic passcode unlocked;
  - the API 28/API 35 test preference keys and rotation overrides were precisely removed/restored;
    no test credential or passcode was written to repository evidence.
- P03–P06 focused production verification:
  - a clean API 35 install selected conservative `zh-CN`, traversed the real P03 welcome,
    P04 local-user creation, P05 operator/notification configuration, P06 conditions acceptance,
    Android notification denial recovery, and reached the real Home with the created synthetic
    identity;
  - reference viewport was 390 × 844 dp, Chinese, light theme. Final side-by-side acceptance
    images are `P03-side-by-side-390x844-v2.png`,
    `P04-side-by-side-390x844-v4.png`,
    `P05-side-by-side-390x844-v3.png`, and
    `P06-side-by-side-390x844-v5.png`; P02 uses
    `P02-side-by-side-system-unavailable-390x844-v2.png`;
  - final side-by-side SHA-256 prefixes are respectively `18d8f3f4…`, `348c7056…`,
    `92edeee1…`, `2da698ba…`, and P02 `44b62d44…`;
  - the screenshot-only `PrivacyProtectScreen=false` fixture existed only on the disposable
    API 35 app data during capture. It was removed exactly; the app returned to Home and a
    post-cleanup secure screenshot had one color with RGBA mean `0,0,0,1` (all black).
- Current focused build/test closure:
  - `:android:testDebugUnitTest --tests chat.simplex.app.nome.NomeLocalePolicyTest` — pass;
  - `:common:desktopTest --tests chat.simplex.common.AppUnlockPolicyTest` — pass;
  - `:common:compileKotlinDesktop`, `:common:compileDebugKotlinAndroid`, and
    `:android:assembleDebug` — pass;
  - `git diff --check`, forbidden-path probe, and synthetic-passcode repository scan — pass.
- Milestone 1 concentrated closure:
  - Android unit/lint/debug/androidTest/release aggregate — `BUILD SUCCESSFUL in 3m 5s`, 196
    tasks; Desktop suite — `BUILD SUCCESSFUL in 44s`;
  - disposable API 28 and API 33 — `OK (30 tests)` each after exact exclusion of the four
    API-35-only screenshot classes; disposable API 35 — `OK (34 tests)` in 455.836 seconds;
  - all three clean starts created real chat/agent databases and reached production onboarding;
    API 33 additionally completed real onboarding, notification notice/permission denial, and
    Home;
  - P01 — 112 unique API 35 captures across 14 states, two languages, two themes, and
    100%/200%; P02–P06 — eight production combinations per family;
  - P02–P06 actual trees retained their primary controls and 48dp minimums; P06 English 200%
    required and passed a real scroll-to-consent/enabled-action check;
  - real TalkBack service was bound with touch exploration on P02–P06; P01's four accessibility
    tests passed while it was bound; cleanup restored accessibility off, font `1.0`, light mode,
    and screenshot protection (secure screenshot RGBA mean `0,0,0,1`);
  - frozen evidence is unchanged and every historical object rehashed from the P13 checkpoint:
    Phase 1 48/48, Foundation 131/131, Batch 2 254/254, and P13 139/139;
  - release has zero Nome evidence/test/dev-package/workspace/bearer/token fixture hits. Actual
    unsigned identity is package `chat.simplex.app`, label `SimpleX`, version 358 / 6.5.6,
    min/target 28/35; this measured non-Nome identity and current SimpleX App Links are retained as
    a downstream final distribution blocker, not misreported as complete;
  - full report:
    `plans/evidence/20260718_nome_android_completion/MILESTONE_1_VERIFICATION.md`.
- Gate C focused verification:
  - `:common:desktopTest --tests ConnectionPreviewPolicyTest` — pass;
  - `:android:testDebugUnitTest --tests ConnectionPreviewRouteBoundaryTest` — pass;
  - Android debug Kotlin compilation — pass as part of the unit-test task;
  - `:android:compileReleaseKotlin` — pass.
- After retained-lifecycle and debug-seam changes:
  - `:android:testDebugUnitTest --tests ConnectionPreviewRouteBoundaryTest` — pass;
  - `:android:compileReleaseKotlin` — pass;
  - `:android:assembleDebug` — pass, most recently at 10:55 local time;
  - debug APK installed in place on API 28/API 35 without clearing either controlled client's
    data.
- The first combined build exhausted the 2 GiB Kotlin daemon while compiling three targets in
  parallel. Re-running one target at a time with one worker and an ephemeral 6 GiB in-process
  compiler passed; no project build setting changed.
- Host-only JDI/JDWP observes only class/method entry and cancellation class; it never reads
  arguments, locals, fields, return values, payloads, or bearer data. Exact real attempts recorded:
  - cancel before submit: `PLAN=1`, `CONNECT=0`;
  - eight rapid activations: `PLAN=1`, `CONNECT=1`;
  - incognito failure: `PLAN=1`, `CONNECT=1`;
  - retry: fresh replan `PLAN=1`, followed by `CONNECT=1`;
  - invitation/address success/failure/reuse attempts: each `PLAN=1`, `CONNECT=1`;
  - active-user switch after Ready: `PLAN=1`, `CONNECT=0`, terminal `ContextChanged`;
  - accepted cancellation attempt `api35-invite-cancel-complete-015`: `PLAN=1`, `CONNECT=1`,
    one active command-owning child before cancellation, owner inactive afterward,
    `kotlinx.coroutines.JobCancellationException`, one cancel signal, and zero preview/terminal
    nodes after dismissal;
  - existing-address attempt `api35-already-exists-address-014`: after real controlled
    auto-accept established the same address contact, `PLAN=1`, `CONNECT=0`, and the official
    existing-contact dialog. The paired harness restored the fixture's default address settings.
- Successful lifecycle/context results:
  - actual rotation plus Home/background/resume retained the same attempt and ended in real
    `Pending` with `PLAN=1`, `CONNECT=1`;
  - real user switch between Ready and submit ended in `ContextChanged` with `CONNECT=0`, then the
    original user was restored;
  - two controlled clients on API 28/API 35 observed current and incognito invitation paths, and
    reusable-address attempts reached real Failure and Pending outcomes.
- Owner verified/failed is closed as production `NOT REACHABLE` for P13 on v6.5.6:
  external `ACTION_VIEW` is the only P13 producer and cannot carry `LinkOwnerSig`; the signed
  in-chat producers retain `Legacy`, and the core returns no owner fact for a null signature.
  The bounded proof is recorded in
  `plans/evidence/20260717_nome_android_phase2_batch3_p13/owner-proof-boundary.md`.
- A clean API 35 capture found zero complete-bearer hits in the app PID, `SIMPLEX` tag, UI tree,
  and full Logcat. Four OS-owned dispatch descriptors retained only the public
  `https://simplex.chat/` root, with zero route/bearer bytes. The platform residual is recorded
  separately in
  `plans/evidence/20260717_nome_android_phase2_batch3_p13/logcat-boundary.md`.
- Package-scoped networking deny/restore was verified as `deny/chain enabled` during the probe and
  `allow/chain disabled` afterward; external ping then passed.
- The API 28 controlled address was regenerated only into the debug app's private cache by a
  one-shot instrumentation harness. Evidence records only its 320-byte length and URI shape.
- Exclude `apps/multiplatform/Screenshot_1784275771.png` from evidence and checkpoints.
- API 35 real TalkBack closure:
  - the focus-only Ready dismissal route passed `OK (1 test)`;
  - the combined real accessibility click, real core/network Failure, announcement, and dismissal
    route passed `OK (1 test)`;
  - an androidTest-only TTS engine retained only three fixed public failure-copy segments, observed
    in two synthesis requests of lengths 42 and 48;
  - focus returned to Home's current-profile control after dismissal;
  - temporary product trace logging was removed before the final passing runs;
  - controls and exact allowlisted transcript are recorded in
    `plans/evidence/20260717_nome_android_phase2_batch3_p13/talkback-transcript.md`.
- Final post-cleanup verification:
  - Android compile/unit/lint/debug/androidTest/release aggregate —
    `BUILD SUCCESSFUL in 2m 32s`; Android unit 22/0/0;
  - Desktop — `BUILD SUCCESSFUL in 27s`; 21/0/0;
  - standard P13 instrumentation — API 28 `OK (4 tests)`, API 35 `OK (4 tests)`;
  - final API 35 72-image matrix — `OK (1 test)`, structured visual score 99 `pass`;
  - release package/version/min/target — `chat.simplex.app`, `6.5.6` / 358, API 28 / API 35;
  - release/debug/standard-test temporary receiver/TTS/harness/trace scan — zero;
  - lint 0 errors and 0 P13-scope findings; resources 44/44 and debug resources 4/4;
  - changed-text secret scan 0; Markdown local links/anchors 0 failures;
  - historical manifests reverified 48/48, 131/131, and 254/254.
- Gate G restart:
  - `081e935b…` received two independent `ZERO ISSUES` reports, but checkpoint-only
    `git diff --cached --check` then exposed Markdown trailing-space/EOF errors in previously
    untracked evidence;
  - eight Markdown files were normalized without semantic change, invalidating that digest and
    its generated 139-entry final manifest;
  - the next Round 1 found `build-verification.md` still mixed historical 28/438/34 link metrics
    with the final 35/552/402 metric; Round 2's `ZERO ISSUES` cannot count after that finding;
  - the build record now contains one explicit current Markdown audit definition, changing only
    that evidence byte set;
  - current review input — 137 entries, 137/137 `OK`, SHA-256
    `25bf6f4575b910ff649e9683334f71a9f4bd409307ceadc0009498f1943eb233`;
  - the first `25bf6f45…` Round 1 found only that the excluded review history did not quote the
    superseded metric wording exactly; its same-attempt Round 2 `ZERO ISSUES` cannot count;
    `review-rounds.md` now preserves the exact old/current definitions, and the digest is
    unchanged because that file is excluded;
  - final current-digest Round 1 — independent code/product/spec/test/evidence reviewer,
    `ZERO ISSUES`, `APPROVE`;
  - final current-digest Round 2 — independent security/release/evidence reviewer,
    `ZERO ISSUES`;
  - no manifest-listed byte changed between those two current-digest reviews.
  - final `SHA256SUMS` — 139 entries, 139/139 `OK`, file SHA-256
    `211d2d9c7b69808dd3bf8f1ceaf7dff5dd0298ab09fb4ed9c0af9752a8549a37`;
  - final checkpoint input — 141 exact staged paths, 0 path delta, 0 unstaged tracked paths, one
    excluded unknown screenshot, 66 staged text files, 0 secret or forbidden-path hits,
    `git diff --cached --check` pass;
  - historical manifests remain 48/48, 131/131, and 254/254; 35 Markdown files / 552 local links /
    402 line anchors still have zero failures.
- Cleanup and restoration:
  - three known private-cache bearer files removed by exact name; unknown cache/data preserved;
  - one-shot, acceptance, TalkBack/TTS androidTest sources and lifecycle receiver removed;
  - API 35 accessibility and touch exploration disabled, service/default TTS null, temporary
    notification permission revoked;
  - standard test APK restored on API 28/API 35;
  - temporary immutable flags removed.

## Checkpoints

- Last frozen checkpoint: `b714f78efc7b3cc8c4e252d6351c4b629f60e277` (P13).
- Prior frozen checkpoint: `fa95d96c7c24370c01b2707b9e2b0ad52a7a5112` (Batch 2).
- Next checkpoint: the single Milestone 1 checkpoint; the gate passed and exact staging is the
  only remaining action. There is no separate P01 checkpoint under the amended contract.
- Remote mutations: none.

## Blocker

- No Milestone 1 blocker. The current unsigned release package/label/App Links are still the
  official SimpleX distribution identity and are explicitly `NOT FOR DISTRIBUTION`; that is a
  declared downstream application-identity/App Links/brand audit item for final RC, not a hidden
  Milestone 1 pass.

## Unique next action

Stage the exact Milestone 1 source/product/spec/test/ledger/report and compact visual-baseline set,
exclude `apps/multiplatform/Screenshot_1784275771.png`, rerun staged-path/diff/secret/forbidden
checks, and create the single local Milestone 1 checkpoint. Do not push.
