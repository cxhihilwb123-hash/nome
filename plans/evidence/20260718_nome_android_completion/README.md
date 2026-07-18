# Nome Android Completion Ledger

Date: 2026-07-18
Status: active
Authority: `plans/20260718_01.md`

This is the sole execution ledger for the active Nome Android completion Goal. Resume from the
unique next action below; do not re-plan the whole route or repeat frozen batches.

## Current milestone and batch

- Milestone: create the local P13 checkpoint before starting P01.
- Gate: G — two consecutive same-digest `ZERO ISSUES` reviews and the final manifest are complete;
  checkpoint creation remains.
- Scope: Android-only, local-only; no push, release, production signing, native/core, protocol,
  database/archive, message-state-machine, or iOS changes.
- Frozen inputs: the v6.5.6 baseline, Phase 1, Phase 2 foundation, and Batch 2 evidence remain
  read-only.
- `FIRST_USE` remains official onboarding/root owned.
- `FILTERED_NO_RESULT` remains deferred until P09 has a real producer.

## Repository recovery

- Branch: `codex/nome-android-v656`
- HEAD: `fa95d96c7c24370c01b2707b9e2b0ad52a7a5112`
- Baseline: tag `v6.5.6` at `59fce95d3cd08897b4ef742447b785cf2e56c7ce`, an ancestor of HEAD.
- Origin: `https://github.com/simplex-chat/simplex-chat.git`
- HEAD commit: `checkpoint(nome-android): freeze phase 2 batch 2`
- Staged/conflicted: zero at recovery.
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
- Free disk at recovery: approximately 146 GiB.

## P13 closure-plan state

- Authoritative closure-plan digest:
  `1ba4f790c35df45e2cbf08913870c386f2b1f6c76ca5c5d6d6df023b3fba44cc`
- Gate B review round 1: `ZERO ISSUES`.
- Gate B review round 2: `ZERO ISSUES`.
- Review record:
  `plans/evidence/20260717_nome_android_phase2_batch3_p13/closure-plan-review.md`
- Earlier candidate digests are invalidated in that record.

## Current source state

- P13 source, tests, renderer evidence, and real-device traces are present but not frozen.
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

1. Create the local checkpoint without pushing. All product/runtime/review/manifest gates are
   closed.

## Tests and evidence

- Recovery checks: branch/HEAD/tag ancestry/origin, precise dirty/staged/conflict counts, historical
  manifests, toolchain, AVD inventory, and remaining gates.
- Gate B: two independent reviews of identical plan bytes.
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
  - historical manifests remain 48/48, 131/131, and 254/254.
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

- Last frozen checkpoint: `fa95d96c7c24370c01b2707b9e2b0ad52a7a5112` (Batch 2).
- P13 checkpoint: authorized and pending; Gate G is complete.
- Remote mutations: none.

## Blocker

- No unavoidable blocker. The P13 defects and evidence gaps have safe local paths.

## Unique next action

Create the authorized local checkpoint from the exact 141 staged paths with message
`checkpoint(nome-android): freeze phase 2 batch 3 p13`; do not push. Then record the new HEAD here
and enter P01 Gate A. Do not modify P01 before the checkpoint exists.
