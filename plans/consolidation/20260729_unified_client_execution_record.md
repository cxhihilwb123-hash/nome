# Nome unified client authority execution record

Date: 2026-07-29 (Asia/Shanghai)

Status: **PASS**

## Result

Core, Android, iOS, and Desktop now share one long-lived local client authority:

- Worktree: `/Users/forkman03/project/nome/nome-client`
- Branch: `codex/nome-v656-unified`
- History-preserving merge commit: `124863fe607eb84c177f8ccce7c76793f1ceeb55`
- Android/Core/Desktop parent: `6cd16da945bc45683eab23f82dc58ba996109960`
- iOS parent: `fb4a81780aa7518735f5d1a3c245808bd3b81f0b`

Both parents are ancestors of the merge commit. The Website/control plane remains
the independent repository `/Users/forkman03/project/nome/website` on `main` at
`568d47dde9d81465cad1139f3b9c90c0f1c9f041`.

The former consolidated client worktrees are retained as read-only recovery
sources. They are no longer parallel build authorities and were not deleted.

## Safety and recovery

The post-Phase-6 backup was created before unification at:

`/Users/forkman03/project/nome/project-backups/20260729T144307+0800_nome_post_phase6`

- Client all-refs bundle: 515,186,054 bytes; SHA-256
  `fe40295795c8d56993e226d0eaa6c0cbbfe8ccd7af6a604e593b5b18624f5ca5`.
- Website all-refs bundle: 6,015,622 bytes; SHA-256
  `0797ae90631f8c5210c2acea8ae00ebdf1adab88c84addae095e4a986b9e4347`.
- Both bundles passed checksum, `git bundle verify`, full-ref restore into fresh
  bare repositories, exact ref comparison, exact authority-head checks, and
  `git fsck --full --strict`.

No reset, Git clean, stash, rebase, force push, remote push, backup deletion,
worktree deletion, or bulk add was performed.

## Merge resolution

The merge produced six conflicts, all in authority/build-discipline documents
or the build-source gate fixture. They were resolved semantically and staged one
path at a time. Product source merged without conflict.

- Conflict-marker scan: PASS; zero markers.
- Shell syntax for the source gate and its fixture: PASS.
- Source-gate fixture: PASS, including the unified authority, compatible legacy
  authorities, cross-platform misuse rejection, dirty/staged/untracked and
  detached-HEAD rejection, configuration fingerprints, and artifact binding.
- `git diff --check`: PASS.

Tree equivalence proves that the merge did not rewrite either platform tree:

- `apps/multiplatform` differs by zero paths from the Android/Core/Desktop parent.
- `apps/ios` differs by zero paths from the iOS parent.

## Local ignored build inputs

Ignored build inputs were restored into stable local runtime caches and linked
into the unified worktree. They are not Git source and contain no production
credentials.

- iOS libraries: 10 files, 1.1 GB under
  `/Users/forkman03/project/nome/runtime-cache/ios-libraries/v6.5.6`; source and
  target manifests match exactly.
- Android native libraries: 4 files, 384 MB under
  `/Users/forkman03/project/nome/runtime-cache/android-native/v6.5.6`; source and
  target manifests match exactly.
- Local JDK: Eclipse Temurin 17.0.19+10 arm64 under
  `/Users/forkman03/project/nome/runtime-cache/jdks`; the downloaded archive is
  185,818,964 bytes with SHA-256
  `8fa1eff40bb637a33613b2ccb8b12c70dc3661cc22cf8e784943715769a05336`.
- Ignored `Local.xcconfig`, `local.properties`, and `cabal.project.local` were
  restored from previously passing local authorities and fingerprinted. Their
  values were not committed or written to evidence.

## Unified acceptance

| Scope | Result |
|---|---|
| Build-source discipline | PASS for Android, Core, macOS Desktop, and iOS Simulator at merge commit `124863fe607eb84c177f8ccce7c76793f1ceeb55` |
| Android JVM tests | PASS; 70 tests, 0 failures/errors/skips |
| Android Debug assemble | PASS; 72 tasks; arm64 and armv7 APKs produced |
| Android arm64 APK | 332,903,218 bytes; SHA-256 `792a7ced820264e68a7e9eb31c11f59bfe200a3e94bfca10cfa35cc89951610b` |
| Android armv7 APK | 318,844,322 bytes; SHA-256 `a3e1209517cdaadbaadd04d29b0be6e1b569b015949442f1dce5eff03cf78fff` |
| Android instrumentation inheritance | PASS boundary; unified Android tree is byte-identical to the Phase 2 parent whose isolated API 35 suites passed 6 activation and 5 channel/member tests |
| Desktop tests | PASS; 207 tests, 0 failures/errors/skips |
| Core Nome directed tests | PASS; 14 examples, 0 failures |
| Core call-rejection flow | PASS; 1 example, 0 failures |
| Core channel relay delivery | PASS; 7 examples, 0 failures |
| iOS activation reducer | PASS; 28 cases |
| iOS arm64 Simulator build | PASS; Nome app, extensions, framework, signing and embedded-binary validation completed |
| iOS fail-closed UI test | PASS; 1 XCTest, 0 failures; no endpoint or production invite required |
| Website source gate and lint | PASS on independent `main` at `568d47dde9d81465cad1139f3b9c90c0f1c9f041` |
| Website production build and tests | PASS; 46 tests, 0 failures/skips |
| Client and Website worktree integrity | PASS; tracked and untracked status clean; ignored build inputs remain ignored |

The iOS Simulator executable produced from the merge commit is 73,278,016 bytes
with SHA-256
`7eea1c60685819df2fae7ca655293a9b84b1358d59c17d1d253b9f00e1eb5602`.
It is an internal Simulator diagnostic, not an IPA or public release.

Windows Desktop source remains in the unified history, but no Windows host,
installer, signing, or runtime acceptance was available in this macOS pass.
Windows is therefore **NOT RUN**, not implied by the macOS Desktop result.

## Authority and publication boundary

The unified client worktree is accepted as the single local client authority.
Normal work should branch from it; temporary release, hotfix, or large migration
worktrees require a specific purpose and later cleanup approval.

Post-unification bundle/restore evidence is stored outside source Git under
`/Users/forkman03/project/nome/consolidation-evidence/20260729_unified_client`.
Deleting retained worktrees or caches remains a destructive Phase 6 action.
Any GitHub/LAN push, deployment, production mutation, TestFlight/App Store
action, or public release remains Phase 7 and requires explicit authorization.
