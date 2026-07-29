# Nome authorized worktree/cache cleanup record

Date: 2026-07-29 (Asia/Shanghai)

Status: **PASS for the safe authorized subset**

## Authorization and protection boundary

The user explicitly authorized deletion with `授权删除`. The operation remained
bounded to clean obsolete worktrees whose refs were independently recoverable,
and to exact rebuildable build/verification caches with no open files.

The following were protected throughout:

- `/Users/forkman03/project/nome/nome-client` and Website source.
- All `project-backups`, `device-backups`, deliverables, and consolidation
  evidence.
- `runtime-cache`, including iOS Libraries, Android native libraries and AVDs,
  the local JDK, and upstream release inputs.
- Dirty or untracked worktrees, the main shared Git administrative directory,
  active-process worktrees, formal evidence, and unclassified temporary audit
  assets.

No reset, Git clean, stash, rebase, force removal, worktree prune, branch
deletion, backup deletion, remote push, deployment, or public release was used.

## Recovery and process gates

- The final post-unification `SHA256SUMS` passed before and after deletion.
- `git bundle verify` passed for the client all-refs bundle and reported complete
  history.
- The bundle contains the exact heads of every removed worktree branch.
- Every removed worktree was clean with zero untracked files.
- Every removed cache and worktree had zero matching open-file records.
- Removal used ordinary `git worktree remove` without `--force`, or exact
  path-bounded `find ... -xdev -depth -delete` for rebuildable caches.

## Removed worktrees

The directories were removed, but the branch refs and commits remain in the
shared repository and verified bundle.

| Removed worktree | Retained branch | Retained SHA | Pre-delete KiB |
|---|---|---:|---:|
| `simplex-chat-android-consolidated` | `codex/nome-v656-consolidated-android` | `6cd16da945bc45683eab23f82dc58ba996109960` | 745020 |
| `simplex-chat-ios` | `codex/nome-ios-v656` | `3f75e9840a5046697e21d6e4c5a87afb59deae09` | 395680 |
| `simplex-chat-ios-realcore-lab` | `codex/nome-ios-v655-realcore-lab` | `711076b9cde8ecc9be4a0b97bb1ee5f4698caa15` | 1118120 |

## Removed rebuildable caches

Initial completed build/restore caches removed from `/private/tmp`:

- `nome-unified-ios-derived-20260729`
- `nome-unified-cabal-dist`
- `nome-unified-cabal-dist2`
- `nome-unified-sourcepackages`
- `nome-post-phase6-restore.O2IRHM`
- `nome-post-unification-restore.W3rsme`

Post-clean acceptance outputs were removed after their successful checks:

- `nome-post-authorized-cleanup-ios-build-20260729`
- `nome-client/apps/multiplatform/android/build`
- `nome-client/apps/multiplatform/common/build`
- `website/dist`
- `website/.wrangler`

The deleted targets represented 10218612 logical KiB. APFS sharing and
compression meant the measured filesystem change was smaller: used space fell
from 335698224 KiB to 330063088 KiB and available space rose from 116208512 KiB
to 121843648 KiB, an actual improvement of 5635136 KiB (about 5.37 GiB).

## Intentionally retained worktrees

| Worktree | Reason not deleted |
|---|---|
| `simplex-chat` | It owns the shared `.git` directory used by `nome-client` and still has 22 tracked and 298 untracked entries. |
| `simplex-chat-ios-integration` | It still has 22 tracked and 4 untracked entries. |
| `simplex-chat-ios-consolidated` | It is clean and its head is in unified history, but two live Xcode `DTServiceHub` processes still use it as their current working directory. |

The retained worktrees are not parallel product authorities. `nome-client`
remains the only client development authority.

## Post-clean acceptance

- Android/Core/Desktop source gate: PASS at
  `cad37e62ef183c3d929496b054b64b45a29bd863`.
- iOS source gate: PASS at the same client commit.
- Website source gate: PASS at
  `568d47dde9d81465cad1139f3b9c90c0f1c9f041`.
- Website lint, production build, and 46/46 tests: PASS.
- Android `:android:testDebugUnitTest :android:assembleDebug`: PASS; 72 tasks,
  with the accepted test task up to date and both native ABI build paths
  completed.
- iOS arm64 Simulator `clean build`: PASS; Nome app, NSE, SE, local signing, and
  embedded-binary validation completed with `** BUILD SUCCEEDED **`.
- Acceptance-generated outputs were removed and all three source gates passed
  again afterward.
- Final `nome-client` and Website Git status: clean.

## Remaining boundary

No further deletion is safe while the three retained gates above remain. The
remaining worktrees must not be forced away. This cleanup record is local until
a separate remote-push authorization is provided.
