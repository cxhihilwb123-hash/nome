# Nome project authority index

Updated: 2026-07-29 (Asia/Shanghai)

This is the first document to read for current Nome repository status. Older plans and handoffs remain historical evidence; they do not override this index.

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

## Unified authority and retained recovery lines

| Scope | Worktree / repository | Branch | Recorded local source state |
|---|---|---|---|
| Client authority (Core/Android/iOS/Desktop) | `/Users/forkman03/project/nome/nome-client` | `codex/nome-v656-unified` | accepted merge `124863fe607eb84c177f8ccce7c76793f1ceeb55`; parents `6cd16da945bc45683eab23f82dc58ba996109960` and `fb4a81780aa7518735f5d1a3c245808bd3b81f0b`; remote branch protected through Phase 7 commit `cad37e62ef183c3d929496b054b64b45a29bd863` |
| Website/control plane authority | `/Users/forkman03/project/nome/website` | `main` | local, personal GitHub, and LAN tip `568d47dde9d81465cad1139f3b9c90c0f1c9f041` |
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
- Latest locally verified Android artifacts and hashes are in the Phase 2 record; they are not public-release claims.
- Latest reproducibly identified iOS artifact: [`builds/20260729_nome_ios_6.5.6_build349_consolidated.md`](./builds/20260729_nome_ios_6.5.6_build349_consolidated.md), internal Simulator diagnostic only.
- Website acceptance: `/Users/forkman03/project/nome/website/docs/consolidation/20260729_phase3_website_execution_record.md`.
- Build record rules/template: [`builds/README.md`](./builds/README.md) and [`builds/TEMPLATE.md`](./builds/TEMPLATE.md).

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
