# Phase 5 cross-platform documentation and evidence-governance record

Date: 2026-07-29 (Asia/Shanghai)

## Changes

- Added `plans/README.md` as the current authority index.
- Added a non-moving history entry at `plans/archive/README.md`.
- Preserved the governing consolidation plan and the 2026-07-27 handoff in Git without editing their historical content.
- Recorded cross-platform authority, build records, evidence zones, and Phase 6/7 authorization boundaries.

The imported files match both the protected dirty main and the Phase 0B recovery copy:

- `20260729_nome_project_consolidation_plan.md`: SHA-256 `2d50fb4457c049e33edf461fc060e00f3ebf1ab2bf2e969c13e5f5324ebf66bb`.
- `20260727_nome_project_status_next_thread_handoff.md`: SHA-256 `4a30d5a5e6866e7ed0805b8e0aed2c77886ad2f46743a60bca134c0db1ae129c`.

## Evidence classification and sensitive backup

- Original dirty main `plans/evidence`: 774 tracked files / 96,313,136 bytes; 241 untracked files / 12,809,588 bytes. It was classified, not moved.
- iOS consolidated `plans/evidence`: 64 tracked files / 15,004,626 bytes; no untracked files.
- Android consolidated `plans/evidence`: 774 tracked files / 96,313,136 bytes; no untracked files.
- Current `device-backups`: 116 files; 116/116 match the Phase 0 frozen SHA-256 list.
- Current `deliverables`: 52 files; 52/52 match the Phase 0 frozen SHA-256 list.
- The Phase 0 backup contains manifests for `device-backups`, not a second physical copy. No encrypted backup medium was configured in this scope, so the original remains under an explicit no-delete hold.
- Private device file manifests are stored with mode `600` under `/Users/forkman03/project/nome/consolidation-evidence/20260729_phase5_governance` and are not committed.

Tracked-path audit found no `device-backups`, `Local.xcconfig`, `.dev.vars`, raw `.env`, SQLite, or device database files in the iOS, Android, or Website authority lines. Static full-format invitation patterns occur only in test source files; no production invitation value was added.

## Acceptance

- A new contributor can find current authority, status, release records, evidence, history, and authorization boundaries from `plans/README.md`.
- The old handoff is retained but explicitly non-authoritative.
- Existing 106 MB historical evidence was not bulk moved.
- No device data, secret configuration, full production invitation code, large screenshot batch, or reproducible cache was added to Git.
- No file, backup, worktree, cache, or evidence was deleted.
- Phase 5 governance: PASS.
- Deletion or publication authority was not granted by this phase.
