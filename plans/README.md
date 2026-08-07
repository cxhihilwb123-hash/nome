# Nome project authority index

Updated: 2026-08-07 (Asia/Shanghai)

This is the first document to read for current Nome repository status. Older plans and handoffs remain historical evidence; they do not override this index.

New coding tasks must also follow the repository-wide instructions in
[`../AGENTS.md`](../AGENTS.md). A copy-paste prompt and start-of-task checks for
future tasks are maintained in
[`20260729_nome_next_thread_development_entry.md`](./20260729_nome_next_thread_development_entry.md).

## Current plan and phase status

- Governing execution plan: [`20260729_nome_project_consolidation_plan.md`](./20260729_nome_project_consolidation_plan.md)
- Phase 0A/0B: PASS; frozen snapshot and verified recovery backup remain under `/Users/forkman03/project/nome/project-backups/20260729T072000+0800_nome_phase0`.
- Phase 1 iOS: PASS.
- Phase 2 Android/Core/Desktop: PASS.
- Phase 3 Website/control plane: PASS.
- Phase 4 build discipline: PASS.
- Phase 5 documentation/evidence governance: PASS after the execution record in this branch.
- Phase 6 deletion/cleanup: PASS; exact cache/worktree removal, AVD migration, backup re-verification, and post-clean builds are recorded below.
- Post-Phase-6 local unified-client consolidation: PASS; both client authority heads are preserved, local build inputs are normalized, and the platform-specific gates are recorded below.
- Phase 7 remote branch protection: PASS after explicit authorization; the
  unified client branch and Website `main` were ordinarily pushed to their
  personal GitHub and LAN remotes and read back. Deployment and public release
  remain not authorized and were not performed.
- Post-Phase-7 authorized worktree/cache cleanup: PASS for the safe subset;
  three clean obsolete worktrees and exact rebuildable caches were removed,
  post-clean Android/iOS/Website gates passed, and unsafe retained worktrees
  remain explicitly documented.
- 2026-08-07 local release-foundation merge: PASS; client unified advanced
  through `7dd58dfe80569bd9ca3d533738ba6b65d299ec15` and Website `main`
  advanced to `0f889e9ea13e018f5180081dbe729492b7dde5bf`. The existing local SDK 36
  patch and Square work remain uncommitted and excluded. No remote push, tag,
  deployment or store action was performed.
- 2026-08-07 authorized remote synchronization: PASS; client unified
  `codex/nome-v656-unified` was fast-forwarded and read back from GitHub and
  LAN at `7e94b5512c8dd6c1362a5c79e9deb6b6b11b5ea7`, and Website `main` was
  fast-forwarded and read back from both remotes at
  `0f889e9ea13e018f5180081dbe729492b7dde5bf`. No tag, deployment, store action
  or official SimpleX remote mutation was performed.
- 2026-08-07 Android API 36 migration branch: local build gates PASS at source
  commit `ae23430762157cf38945e4ca3b0ab24c5ea06933`; compile/target SDK 36, AGP
  8.9.1, Kotlin 2.2.10, Android assemble/unit/lint and shared Desktop tests
  passed. Isolated Android 16 fresh-install, onboarding, notification,
  background-service, cold-relaunch, same-signed API 35 to API 36
  preserved-data upgrade and reboot-recovery gates also passed. The short-lived
  branch was accepted into local unified via merge commit
  `60cb74fc401bfd4cc839e9ae472a7595626c007d`. After separate authorization,
  the merged unified branch was pushed to GitHub and LAN and both were read
  back at `769e4058ba5df86fdb9bd36f2f002eca65626e5c`; it was not tagged or
  published, and two-client messaging, channels and calls were not run.

## Unified authority and retained recovery lines

| Scope | Worktree / repository | Branch | Recorded local source state |
|---|---|---|---|
| Client unified authority (Core/Android/iOS/Desktop) | `/Users/forkman03/project/nome/nome-client-publish-20260802` | `codex/nome-v656-unified` | Android API 36 accepted locally via merge `60cb74fc401bfd4cc839e9ae472a7595626c007d`; the merge, runtime evidence and local-merge closure were pushed to GitHub and LAN and read back at `769e4058ba5df86fdb9bd36f2f002eca65626e5c`; this authority-index publication closure follows that readback |
| Android API 36 temporary migration | branch retained in the shared client Git repository | `codex/nome-android-api36-20260807` | implementation source `ae23430762157cf38945e4ca3b0ab24c5ea06933`; build and isolated Android 16 runtime/upgrade/reboot gates PASS; source head `9c5ae7afb82424ad86f68d2a823c6fa6580d359a` merged locally via `60cb74fc401bfd4cc839e9ae472a7595626c007d`; not pushed |
| Website/control plane authority | `/Users/forkman03/project/nome/website` repository | `main` | local `main`, GitHub `origin/main` and LAN `lan/main` were read back at `0f889e9ea13e018f5180081dbe729492b7dde5bf`; the primary Website worktree remains on the isolated Square branch |
| Shared Git administrative/dirty evidence worktree | `/Users/forkman03/project/nome/simplex-chat` | `codex/nome-android-v656` | `33b97d66155c80dd01956a438b483b13660f331e`; retained because it owns the shared `.git` directory and is dirty |
| Retained iOS recovery source | `/Users/forkman03/project/nome/simplex-chat-ios-consolidated` | `codex/nome-v656-consolidated-ios` | `fb4a81780aa7518735f5d1a3c245808bd3b81f0b`; retained while live Xcode services use it as cwd |
| Retained iOS dirty evidence source | `/Users/forkman03/project/nome/simplex-chat-ios-integration` | `codex/nome-ios-v656-integration` | `e50f32fcec027029c95d4556cceaf600f8a83edb`; retained with tracked and untracked changes |
| Branch-only Android/Core/Desktop recovery ref | no worktree | `codex/nome-v656-consolidated-android` | `6cd16da945bc45683eab23f82dc58ba996109960`; worktree removed after backup/process gates |
| Branch-only old iOS recovery ref | no worktree | `codex/nome-ios-v656` | `3f75e9840a5046697e21d6e4c5a87afb59deae09`; worktree removed after backup/process gates |
| Branch-only real-core diagnostic ref | no worktree | `codex/nome-ios-v655-realcore-lab` | `711076b9cde8ecc9be4a0b97bb1ee5f4698caa15`; worktree removed after backup/process gates |

The unified client is the local authority after its history-preserving merge,
dependency normalization, platform verification, documentation update,
post-unification restore test, authorized remote protection, and bounded
worktree/cache cleanup. Retained dirty, administrative, or active-process
worktrees are recovery/evidence sources, not parallel development authorities.

## Worktree model

- `nome-client` is the single long-lived client worktree. Core, Android, iOS, and Desktop are modules in this one Git history, not separate permanent worktree authorities.
- Release, hotfix, and large platform-migration worktrees are temporary and require an explicit purpose and cleanup gate.
- Website/control plane remains an independent Git repository and is not part of the client merge.

## Current execution and release records

- iOS consolidation: [`consolidation/20260729_phase1_ios_execution_record.md`](./consolidation/20260729_phase1_ios_execution_record.md)
- Android/Core/Desktop consolidation: [`consolidation/20260729_phase2_android_core_desktop_execution_record.md`](./consolidation/20260729_phase2_android_core_desktop_execution_record.md)
- Cross-platform build discipline: [`consolidation/20260729_phase4_build_discipline_execution_record.md`](./consolidation/20260729_phase4_build_discipline_execution_record.md)
- Documentation/evidence governance: [`consolidation/20260729_phase5_docs_evidence_governance_execution_record.md`](./consolidation/20260729_phase5_docs_evidence_governance_execution_record.md)
- Phase 6 cleanup: [`consolidation/20260729_phase6_cleanup_execution_record.md`](./consolidation/20260729_phase6_cleanup_execution_record.md)
- Authorized worktree/cache cleanup addendum: [`consolidation/20260729_authorized_worktree_cache_cleanup_record.md`](./consolidation/20260729_authorized_worktree_cache_cleanup_record.md)
- Unified client acceptance: [`consolidation/20260729_unified_client_execution_record.md`](./consolidation/20260729_unified_client_execution_record.md)
- Local release-foundation merge: [`consolidation/20260807_release_foundation_local_merge_record.md`](./consolidation/20260807_release_foundation_local_merge_record.md)
- Android API 36 local unified merge: [`consolidation/20260807_android_api36_local_merge_record.md`](./consolidation/20260807_android_api36_local_merge_record.md)
- Latest directly installed Android artifact: [`builds/20260730_nome_android_6.5.6_code372_channel_timeline_order_e2e.md`](./builds/20260730_nome_android_6.5.6_code372_channel_timeline_order_e2e.md), internal Debug arm64 APK that retains the default Nome channel relay and invitation-link routing while restoring normal conversation order: old messages above, newest messages at the bottom. Common/Android gates, preserved-data upgrades, real A-to-B channel delivery, read-only subscriber policy, and two-device UI-coordinate order checks PASS. The formal clean-source gate remains blocked by preserved unrelated iOS dirty paths, so this is not a public-release claim.
- Latest local release-foundation Android build: [`builds/20260807_nome_android_6.5.6_code373_release_foundation_debug.md`](./builds/20260807_nome_android_6.5.6_code373_release_foundation_debug.md), clean-commit Debug APKs for arm64-v8a and armeabi-v7a. Compile, unit, lint, desktop and APK assembly gates pass; no device installation, production signing, upload or public release was performed.
- Latest Android API 36 toolchain validation: [`builds/20260807_nome_android_6.5.6_code373_api36_debug.md`](./builds/20260807_nome_android_6.5.6_code373_api36_debug.md), clean-commit Debug APKs for arm64-v8a and armeabi-v7a with compile/target SDK 36, AGP 8.9.1 and Kotlin 2.2.10. Android assemble, unit, lint, Desktop regression, APK metadata and Debug signature gates pass. Isolated Android 16 fresh-install, onboarding, notification, foreground-service, cold-relaunch, same-signed API 35 to API 36 preserved-data upgrade and reboot-recovery gates pass; two-client messaging, channels and calls were not run, and no production signing, upload or public release was performed.
- Latest directly installed iOS device artifact: [`builds/20260729_nome_ios_6.5.6_build375_cryhandsome.md`](./builds/20260729_nome_ios_6.5.6_build375_cryhandsome.md), Personal Team internal build from the unified source plus a recorded temporary signing patch; installation PASS, automatic launch BLOCKED only because the phone was locked.
- Latest consolidated Simulator artifact: [`builds/20260729_nome_ios_6.5.6_build349_consolidated.md`](./builds/20260729_nome_ios_6.5.6_build349_consolidated.md), internal Simulator diagnostic only.
- Website acceptance: `/Users/forkman03/project/nome/website/docs/consolidation/20260729_phase3_website_execution_record.md`.
- Build record rules/template: [`builds/README.md`](./builds/README.md) and [`builds/TEMPLATE.md`](./builds/TEMPLATE.md).
- Coordinated release rules/template: [`releases/README.md`](./releases/README.md)
  and [`releases/TEMPLATE.md`](./releases/TEMPLATE.md). A release uses one
  temporary unified client branch and separate per-platform build evidence; the
  records and exact source tags remain after the branch is retired.

## Historical entry

The frozen pre-consolidation handoff is preserved byte-for-byte at [`20260727_nome_project_status_next_thread_handoff.md`](./20260727_nome_project_status_next_thread_handoff.md). It explains the earlier mixed dirty state but is not the current status document.

Other dated files under `plans/` and the existing `plans/evidence/` tree remain in place during the first governance pass. No bulk archive or rename was performed.

## Evidence and sensitive-data zones

- Consolidation logs and SHA manifests: `/Users/forkman03/project/nome/consolidation-evidence` (not application source).
- Deliverable binaries/screenshots: `/Users/forkman03/project/nome/deliverables` (not source commits).
- Sensitive device recovery data: `/Users/forkman03/project/nome/device-backups` (never commit; do not delete until two encrypted, readable copies are independently verified).
- Phase 0 recovery backup: `/Users/forkman03/project/nome/project-backups/20260729T072000+0800_nome_phase0` (never delete during consolidation).
- Post-Phase-6 recovery backup: `/Users/forkman03/project/nome/project-backups/20260729T144307+0800_nome_post_phase6` (restore-tested before the unified merge).

New Git evidence should be concise text, SHA256SUMS, and only a few necessary screenshots. Large or reproducible artifacts belong under `deliverables`, with one authoritative build record per artifact identity.

## Authorization boundary

Local consolidation, the explicitly authorized Phase 7 GitHub/LAN branch
protection, and the safe subset of the explicitly authorized worktree/cache
cleanup are complete. No official SimpleX remote was changed. Further deletion
of the retained dirty, administrative, or active-process worktrees is forbidden
until their recorded safety gates are cleared and the action is revisited.
Sites or production deployment, policy mutation, TestFlight/App Store action,
public release, tag, or publication outside the recorded refs requires separate
explicit authorization.
