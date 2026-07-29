# Phase 4 iOS build-discipline execution record

Date: 2026-07-29 (Asia/Shanghai)

## Implementation

- Branch: `codex/nome-v656-consolidated-ios`
- Gate/template commit: `94e4b9d7c226024b7a4e056037e932b38c73986f`
- Executable gate: `scripts/release/check-nome-build-source.sh`
- Gate tests: `scripts/release/test-check-nome-build-source.sh`
- Mandatory record template: `plans/builds/TEMPLATE.md`

The source gate accepts only an explicitly authorized branch at one clean commit. It rejects tracked, staged, detached-HEAD, wrong-branch, and non-ignored untracked states. Ignored caches may remain. It emits paths and SHA-256 fingerprints, never configuration values, and can bind one artifact's path, size, and SHA-256 to that same commit.

## Verification

- Shell syntax: PASS.
- Gate fixtures: PASS for clean commit and ignored cache.
- Negative fixtures: PASS; wrong branch, tracked change, staged change, and untracked Swift source were rejected.
- Artifact fixture: PASS; byte size and SHA-256 were emitted against the clean fixture commit.
- Actual consolidated source gate: PASS at `94e4b9d7c226024b7a4e056037e932b38c73986f`.
- Actual configuration fingerprints: `Local.xcconfig` `f323b5e761c9417bbe238f79d7918449a52b65582fcb170723d971917cec5491`; `Debug.xcconfig` `4afac2221a6d30b8a86c35b9b924e79a4cffba068fe5c6f90212ab45f349527d`.
- Evidence: `/Users/forkman03/project/nome/consolidation-evidence/20260729_phase4_build_discipline`.

No new iOS package was built in Phase 4. The existing build 349 diagnostic remains mapped only to source commit `9d0a52b84beebedab126b791ae71352bd94b768f` and artifact SHA-256 `4678125a45cb92efa2c1c059cb9435167259eea2f3a441564638884a1cbe22e6`; it is not relabeled as an artifact of this documentation/gate commit.

## Acceptance

The iOS template now requires identity, full commit, clean status, controlled-dirty state, configuration/dependency fingerprints, artifact/signing identity, installation state, functional matrix, distribution boundary, and evidence index. Build numbers are explicitly monotonic per platform/distribution identity. Public distribution still requires separate authorization.
