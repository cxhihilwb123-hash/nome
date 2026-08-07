# Nome release-foundation local merge record

Date: 2026-08-07 (Asia/Shanghai)

## Result

PASS for local authority integration. This record does not authorize or claim a
remote push, Git tag, deployment, artifact upload, store submission or public
release.

## Client repository

- Authority branch: `codex/nome-v656-unified`
- Before: `ad88fb829b5a55c33558aad16158fed4217e464f`
- Integrated branch: `codex/nome-release-foundation-20260807`
- Fast-forward result before this record:
  `7dd58dfe80569bd9ca3d533738ba6b65d299ec15`
- Divergence before integration: authority `0`, foundation `4`
- Merge mode: `git merge --ff-only`
- Conflicts: none

The authority worktree already contained an unstaged two-line experiment that
changed Android `compileSdk` and `targetSdk` from 35 to 36. It was temporarily
removed only to permit the fast-forward, then restored on top of the integrated
signing configuration. Its final unstaged diff remains exactly those two SDK
values. It was not staged or committed.

## Website repository

- Authority branch: `main`
- Before: `568d47dde9d81465cad1139f3b9c90c0f1c9f041`
- Integrated branch: `codex/nome-website-release-foundation-20260807`
- Fast-forward result: `0f889e9ea13e018f5180081dbe729492b7dde5bf`
- Divergence before integration: authority `0`, foundation `1`
- Merge mode: `git merge --ff-only`
- Conflicts: none

The clean release-foundation worktree temporarily checked out `main`, performed
the fast-forward and switched back. The primary Website worktree remained on
`codex/nome-square-service-web`; its four tracked Square JSON modifications were
not staged, altered or included.

## Preserved feature work

- Client Square worktree: `codex/nome-square-client`, clean and unchanged.
- Website Square worktree: `codex/nome-square-service-web`, four pre-existing
  tracked Square JSON modifications unchanged.
- Old `release/android-v6.5.6`: not merged wholesale. Its still-valid code was
  already incorporated selectively in the release-foundation commits.

## Verification inherited from the integrated commits

- Android `assembleDebug`, Kotlin compile, unit tests and lint: PASS.
- Shared desktop tests: PASS.
- Website root tests: 57/57 PASS.
- Website targeted ESLint and `tsc --noEmit`: PASS.
- Independent final diff review: PASS.

## Remote boundary

- Client GitHub/LAN tracking refs remained at
  `ad88fb829b5a55c33558aad16158fed4217e464f` during this local merge.
- Website GitHub `origin/main` and LAN `lan/main` tracking refs remained at
  `568d47dde9d81465cad1139f3b9c90c0f1c9f041`.
- No push, tag, deploy, upload or store action was performed.
