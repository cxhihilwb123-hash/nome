# Nome unified client authority execution record

Date: 2026-07-29 (Asia/Shanghai)

Status: **PASS**

Remote protection status: **PASS** for the explicitly authorized GitHub/LAN
branch backup. No deployment or public release was performed.

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

Before the explicitly authorized Phase 7 branch backup, no reset, Git clean,
stash, rebase, force push, remote push, backup deletion, worktree deletion, or
bulk add was performed. Phase 7 used only ordinary, named-ref pushes to the
confirmed personal GitHub and LAN repositories.

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

## Phase 7 remote protection

The user explicitly authorized the bounded remote operation with `推送` on
2026-07-29. The authorization covered the unified client branch and independent
Website main branch on the already configured personal GitHub and LAN remotes.
It did not cover the official SimpleX `origin`, tags, pull requests, deployment,
production mutation, TestFlight/App Store actions, or public release.

Preflight acceptance:

- Client and Website worktrees were clean.
- `codex/nome-v656-unified` did not exist on either target remote, so publication
  created a new branch without overwriting an existing ref.
- Both previously published Android/iOS heads were ancestors of the unified
  client history.
- Website `main` was 10 commits ahead and 0 behind both remote `main` refs, so
  both updates were ordinary fast-forwards.
- The post-unification backup checksum passed again immediately before the
  remote operation.
- No newly introduced blob of 10 MiB or more was found. The obvious-credential
  scan found no Website match and only the pre-existing upstream TLS test
  fixture `tests/fixtures/tls/server.key` in the client tree.

Initial remote readback:

| Authority | Remote ref | Read-back SHA |
|---|---|---|
| Client personal GitHub (`github`) | `refs/heads/codex/nome-v656-unified` | `73ea48894ca37294aac43394b9eb94831829c9c4` |
| Client LAN Gitea (`lan`) | `refs/heads/codex/nome-v656-unified` | `73ea48894ca37294aac43394b9eb94831829c9c4` |
| Website personal GitHub (`origin`) | `refs/heads/main` | `568d47dde9d81465cad1139f3b9c90c0f1c9f041` |
| Website LAN Gitea (`lan`) | `refs/heads/main` | `568d47dde9d81465cad1139f3b9c90c0f1c9f041` |

The first client GitHub HTTPS attempt was rejected because the active GitHub
OAuth credential lacked the `workflow` scope required for the already committed
`.github/workflows/build.yml`. GitHub created no branch during that failed
attempt. The existing `cxhihilwb123-hash` login was refreshed through GitHub's
official device authorization flow with the additional `workflow` scope, and
the same ordinary push then succeeded. No workflow file or Git history was
altered to bypass the protection.

This Phase 7 evidence update changes documentation only. Product source and
previously accepted build trees are unchanged, so the complete platform test
matrix above remains bound to the accepted merge history. The documentation
diff and staged diff must pass `git diff --check` before this evidence commit is
pushed. The final client branch-tip SHA is read back from both remotes and stored
in the external Phase 7 evidence packet.

## Authority and publication boundary

The unified client worktree is accepted as the single local client authority.
Normal work should branch from it; temporary release, hotfix, or large migration
worktrees require a specific purpose and later cleanup approval.

The client authority branch is protected on the personal GitHub and LAN remotes;
the independent Website `main` is protected on its personal GitHub and LAN
remotes. No pull request, tag, deployment, production mutation, TestFlight/App
Store action, or public release was performed.

Post-unification bundle/restore evidence is stored outside source Git under
`/Users/forkman03/project/nome/consolidation-evidence/20260729_unified_client`.
Deleting retained worktrees or caches remains a destructive Phase 6 action and
still requires explicit authorization. Any deployment, production mutation,
TestFlight/App Store action, public release, or additional remote publication
outside the refs recorded above also requires separate explicit authorization.
