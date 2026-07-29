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
- Post-Phase-6 local unified-client consolidation: IN PROGRESS. The candidate must preserve both client authority heads and pass the platform-specific gates before it becomes authoritative.
- Phase 7 push/deploy/public release: NOT AUTHORIZED and not performed.

## Unified candidate and retained recovery lines

| Scope | Worktree / repository | Branch | Recorded local source state |
|---|---|---|---|
| Client candidate (Core/Android/iOS/Desktop) | `/Users/forkman03/project/nome/nome-client` | `codex/nome-v656-unified` | merge source heads `6cd16da945bc45683eab23f82dc58ba996109960` and `fb4a81780aa7518735f5d1a3c245808bd3b81f0b`; acceptance pending |
| Website/control plane authority | `/Users/forkman03/project/nome/website` | `main` | current local tip `568d47dde9d81465cad1139f3b9c90c0f1c9f041` |
| Retained iOS recovery source | `/Users/forkman03/project/nome/simplex-chat-ios-consolidated` | `codex/nome-v656-consolidated-ios` | `fb4a81780aa7518735f5d1a3c245808bd3b81f0b` |
| Retained Android/Core/Desktop recovery source | `/Users/forkman03/project/nome/simplex-chat-android-consolidated` | `codex/nome-v656-consolidated-android` | `6cd16da945bc45683eab23f82dc58ba996109960` |

The unified client candidate is not authoritative until the merge, dependency normalization, platform-specific verification, documentation update, and post-unification restore test pass. The retained consolidated worktrees remain recovery sources and must not be deleted during this work.

## Worktree model

- `nome-client` is the candidate single long-lived client worktree. Core, Android, iOS, and Desktop are modules in this one Git history, not separate permanent worktree authorities.
- Release, hotfix, and large platform-migration worktrees are temporary and require an explicit purpose and cleanup gate.
- Website/control plane remains an independent Git repository and is not part of the client merge.

## Current execution and release records

- iOS consolidation: [`consolidation/20260729_phase1_ios_execution_record.md`](./consolidation/20260729_phase1_ios_execution_record.md)
- Android/Core/Desktop consolidation: [`consolidation/20260729_phase2_android_core_desktop_execution_record.md`](./consolidation/20260729_phase2_android_core_desktop_execution_record.md)
- Cross-platform build discipline: [`consolidation/20260729_phase4_build_discipline_execution_record.md`](./consolidation/20260729_phase4_build_discipline_execution_record.md)
- Documentation/evidence governance: [`consolidation/20260729_phase5_docs_evidence_governance_execution_record.md`](./consolidation/20260729_phase5_docs_evidence_governance_execution_record.md)
- Phase 6 cleanup: [`consolidation/20260729_phase6_cleanup_execution_record.md`](./consolidation/20260729_phase6_cleanup_execution_record.md)
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

New Git evidence should be concise text, SHA256SUMS, and only a few necessary screenshots. Large or reproducible artifacts belong under `deliverables`, with one authoritative build record per artifact identity.

## Authorization boundary

Local commits and authorized cleanup are complete through Phase 6. Any GitHub/LAN push, Sites or production deployment, policy mutation, TestFlight/App Store action, or public release requires separate explicit Phase 7 authorization.
