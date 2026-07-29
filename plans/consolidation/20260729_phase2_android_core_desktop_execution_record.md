# Phase 2 Android/Core/Desktop consolidation execution record

Date: 2026-07-29 (Asia/Shanghai)

Status: **PASS**

## Authority and safety boundary

- Worktree: `/Users/forkman03/project/nome/simplex-chat-android-consolidated`
- Branch: `codex/nome-v656-consolidated-android`
- Source-verification commit: `654ebc27ea3629c4f58613bfc7c8d9e8f0b2b659`
- Base: `33b97d66155c`
- The dirty main worktree at `/Users/forkman03/project/nome/simplex-chat` was read only.
- No reset, clean, stash, rebase, bulk add, push, force push, file deletion, backup deletion, or worktree deletion was performed.

## Consolidation commits

| Commit | Responsibility |
|---|---|
| `5a2b2a486` | Merge Android activation and call-rejection histories with semantic conflict resolution |
| `efa6791b5` | Channel, home, search, labels, and Chinese resources |
| `26da7c393` | Nome server configuration and Android/Desktop startup split |
| `cb71bfffb` | Haskell Core, Terminal, and Nome relay preset behavior |
| `90c0d5fa0` | Android/Core tests and normalized regenerated query plans |
| `654ebc27e` | Five required source/test files recovered from the frozen Phase 0B archive set |

The merge retains both required ancestors:

- `git merge-base --is-ancestor ca6cf8e30 HEAD` -> `0`
- `git merge-base --is-ancestor 46f2b5bf3 HEAD` -> `0`

The two conflict resolutions preserved activation fail-closed semantics, call-rejection termination, and the independent Desktop stopped-core pre-network gate. No conflict was resolved by accepting an entire side.

## Frozen dirty-tree recovery

The Phase 0A patch was applied in the four planned responsibility groups. The frozen snapshot listed 55 selected untracked source/document entries. Five referenced Kotlin source/test files were restored byte-for-byte and committed; their SHA-256 values matched the original frozen working tree. The unused `nome_lockup_dark@4x.png` had no source reference and was excluded. Historical plans and local scripts remain protected by the Phase 0B archive and were not included in feature commits.

`chat_query_plans.txt` was regenerated content from the frozen patch and then normalized by removing trailing spaces. `git diff --check` passes.

## Verification

| Gate | Result | Evidence |
|---|---|---|
| Android JVM tests + Debug APK assemble | PASS; 70 tests, 0 failures/errors/skips; 72 Gradle tasks | `android_unit_assemble.log` |
| Activation instrumentation | PASS; API 35 emulator; 6/6 | `android_activation_instrumentation.log` |
| Channel/member presentation instrumentation | PASS; API 35 emulator; 5/5 | `android_channel_instrumentation.log` |
| Desktop complete tests | PASS; 207 tests, 0 failures/errors/skips | `desktop_tests.log` |
| Core Nome directed tests | PASS; 14 examples, 0 failures | `core_nome_tests.log` |
| Core call-rejection two-party flow | PASS; 1 example, 0 failures; receiver rejection ends caller wait state | `core_call_reject.log` |
| Core channel create/join/delivery | PASS; single- and multi-relay; 2 examples, 0 failures | `core_channel_e2e.log` |
| Worktree integrity | PASS; clean tracked/untracked status and `git diff --check` | `git_acceptance.txt` |

The activation suites cover local-only browsing, explicit network-command classification, successful entitlement retention, restart/recovery credential persistence, server expiry, authoritative denial, migration, and live revocation. The call invitation rejection path in `CallManager` is media-independent; the end-to-end Core test uses video, while the same `apiEndCall` path is used for audio invitations.

The first Android build attempt failed before testing because a clean worktree does not carry ignored native libraries. The retained failure log is `android_unit_assemble_attempt1_missing_native.log`. The official v6.5.6 arm64 and armv7 `libsimplex.so`/`libsupport.so` inputs were copied into the ignored local build-input directory and matched the previously documented hashes before the successful rebuild. The first activation instrumentation attempt was also retained; it failed only because the offline Gradle cache lacked UTP 31.7.0. The required test runtime was fetched, then the isolated emulator run passed.

## Android artifacts

- `android-arm64-v8a-debug.apk`: 332,903,214 bytes; SHA-256 `54439938b3a640806453777b21a04daaa28663c6333422df578525cba23c5b78`
- `android-armeabi-v7a-debug.apk`: 318,844,318 bytes; SHA-256 `1ebf0523c93c4c1b18f94890336bcc4b498e2f77c0561299216687da9beefdba`
- Preserved arm64 evidence copy: `/Users/forkman03/project/nome/consolidation-evidence/20260729_phase2_android/Nome-Android-6.5.6-code359-20260729-arm64-debug.apk`

The Android APK intentionally uses the recorded official v6.5.6 native libraries. The Haskell/Core acceptance was independently cold-built from the consolidated source commit in `/private/tmp/nome-phase2-cabal-dist`; current Haskell changes are therefore verified as source but are not claimed to be embedded in the Android APK.

## Acceptance verdict

Phase 2 is accepted. Android/Core/Desktop now have one local authority branch with preserved activation and call-rejection ancestry, reviewable responsibility commits, passing automated gates, reproducible artifact hashes, and a clean worktree. No remote publication occurred.
