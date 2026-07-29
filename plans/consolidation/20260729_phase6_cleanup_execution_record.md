# Phase 6 iOS cleanup execution record

Date: 2026-07-29 (Asia/Shanghai)

## Source and cleanup authority

- Branch: `codex/nome-v656-consolidated-ios`.
- Cleanup-rule commit and post-clean build source: `5f8e97c08c238ec85e6d5451be22ae9e1fd2cbe9`.
- The root and multiplatform ignore rules now cover the local Codex environment, Gradle review/temp caches, repository-local AVD state, and `Screenshot_*.png` without masking source or formal assets.
- The original mixed dirty worktree was not rebased, reset, stashed, cleaned, or used as the authority line.

## Recovery and deletion gates

- Both Phase 0 Git bundles passed `git bundle verify` as complete histories before worktree removal.
- The complete Phase 0B `SHA256SUMS` manifest passed again after cleanup, including both bundles, all three source archives, patches, and private iOS configuration copies.
- The build374 archive passed its dedicated SHA-256 and archive-read checks. Its five content-different iOS files and build records remain recoverable.
- A read-only process scan reported zero open files under every selected removal target.
- The two removed temporary worktrees were clean, their commits were ancestors of the authority lines, their branches remain, and `git worktree prune --dry-run --verbose` is empty after pruning five already-missing metadata entries.

## iOS preservation and verification

- `/Users/forkman03/project/nome/simplex-chat-ios-integration/apps/ios/Libraries` was preserved. The iOS authority's `Libraries/ios` and `Libraries/sim` symlinks still resolve there.
- The 1.1G Libraries copy in the removed non-Git temp source checksum-matched the preserved integration Libraries before removal.
- A fresh arm64 Simulator `clean build` ran against iPhone 17 Pro with isolated DerivedData and finished `** BUILD SUCCEEDED **`.
- The verification DerivedData was removed after the passing log was sealed; it is not a release artifact.
- The Phase 4 source gate passed on the clean `5f8e97c08` state with unchanged `Local.xcconfig` and `Debug.xcconfig` fingerprints.

## Shared cleanup result

- Three AVDs were moved, not deleted, to `/Users/forkman03/project/nome/runtime-cache/android-avd`; all three are enumerated with that directory as `ANDROID_AVD_HOME`.
- Official v6.5.6 APK/signature inputs were moved, not deleted, to `/Users/forkman03/project/nome/runtime-cache/android-upstream-artifacts/v6.5.6` and hashed.
- Exact disposable worktrees, DerivedData, Gradle/Cabal/build caches, Phase 0B verification clones, and Website outputs were removed. Backups, device data, deliverables, evidence, release inputs, and design audit assets were preserved.
- `df` changed from 342Gi used / 89Gi available / 80% to 310Gi used / 121Gi available / 72%, a rounded 32Gi improvement in both used and available space.
- Evidence: `/Users/forkman03/project/nome/consolidation-evidence/20260729_phase6_cleanup`.

## Acceptance boundary

Phase 6 local cleanup passed. No remote push, deployment, production mutation, TestFlight/App Store action, or public release was performed. Those actions remain Phase 7 and require separate explicit authorization.
