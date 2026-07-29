# Phase 6 Android/Core/Desktop cleanup execution record

Date: 2026-07-29 (Asia/Shanghai)

## Source and cleanup authority

- Branch: `codex/nome-v656-consolidated-android`.
- Cleanup-rule commit and post-clean build source: `0ac96edaef352445d64a9cda9dc5329d8247c1ce`.
- The root and multiplatform ignore rules now cover the local Codex environment, Gradle review/temp caches, repository-local AVD state, and `Screenshot_*.png` without masking source or formal assets.
- The original mixed dirty worktree was not rebased, reset, stashed, cleaned, or used as the authority line.

## Recovery and deletion gates

- Both Phase 0 Git bundles passed `git bundle verify` as complete histories before worktree removal.
- The complete Phase 0B `SHA256SUMS` manifest passed again after cleanup.
- A read-only process scan reported zero open files under every selected removal target.
- The two removed temporary worktrees were clean and integrated. Their branches remain, and the final worktree prune dry-run is empty.

## Android/Core/Desktop preservation and verification

- Three AVDs were moved, not deleted, to `/Users/forkman03/project/nome/runtime-cache/android-avd`. All three names are enumerated when that path is used as `ANDROID_AVD_HOME`.
- Official v6.5.6 APK/signature inputs were moved, not deleted, to `/Users/forkman03/project/nome/runtime-cache/android-upstream-artifacts/v6.5.6`; their six SHA-256 values are in the external evidence packet.
- After deleting the authority worktree's generated `common/build` and `android/build`, `:android:testDebugUnitTest :android:assembleDebug` was run offline.
- The first attempt retained a diagnostic `GC overhead limit exceeded` caused by the default Kotlin heap. The established 6GB, single-worker retry passed: `BUILD SUCCESSFUL in 1m 42s`, 72 actionable tasks.
- The rebuilt arm64 and armv7 debug APKs were hashed in the evidence packet, then the generated build directories were removed again.
- The Phase 4 source gate passed on the clean `0ac96edae` state with unchanged Android and Gradle configuration fingerprints.

## Shared cleanup result

- Exact disposable worktrees, DerivedData, Gradle/Cabal/build caches, Phase 0B verification clones, and Website outputs were removed. Backups, device data, deliverables, evidence, release inputs, iOS Libraries, and design audit assets were preserved.
- `df` changed from 342Gi used / 89Gi available / 80% to 310Gi used / 121Gi available / 72%, a rounded 32Gi improvement in both used and available space.
- Evidence: `/Users/forkman03/project/nome/consolidation-evidence/20260729_phase6_cleanup`.

## Acceptance boundary

Phase 6 local cleanup passed. No remote push, deployment, production mutation, store action, or public release was performed. Those actions remain Phase 7 and require separate explicit authorization.
