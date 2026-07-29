# Phase 4 Android/Core/Desktop build-discipline execution record

Date: 2026-07-29 (Asia/Shanghai)

## Implementation

- Branch: `codex/nome-v656-consolidated-android`
- Gate/template commit: `bd63eaf77d10bc44cc85031de57320eb1f38958f`
- Executable gate: `scripts/release/check-nome-build-source.sh`
- Gate tests: `scripts/release/test-check-nome-build-source.sh`
- Mandatory record template: `plans/builds/TEMPLATE.md`

The source gate accepts only an explicitly authorized branch at one clean commit. It rejects tracked, staged, detached-HEAD, wrong-branch, and non-ignored untracked states. Ignored caches may remain. It emits paths and SHA-256 fingerprints, never configuration values, and can bind one artifact's path, size, and SHA-256 to that same commit.

## Verification

- Shell syntax: PASS.
- Gate fixtures: PASS for clean commit and ignored cache.
- Negative fixtures: PASS; wrong branch, tracked change, staged change, and untracked Kotlin source were rejected.
- Artifact fixture: PASS; byte size and SHA-256 were emitted against the clean fixture commit.
- First actual gate attempt: diagnostic FAIL because `gradle/libs.versions.toml` does not exist; retained as `android-source-gate.log`.
- Corrected actual consolidated source gate: PASS at `bd63eaf77d10bc44cc85031de57320eb1f38958f`.
- Actual configuration fingerprints: Android `build.gradle.kts` `0506bf94f2edb8968d85066134989e2690053b544af18f6f1afca941d05ceed5`; multiplatform `gradle.properties` `7ae33b7f94abdaa1c4134d99e05fe48f9ecd1c083bb8e410b668b97942bf6311`.
- Evidence: `/Users/forkman03/project/nome/consolidation-evidence/20260729_phase4_build_discipline`.

No new Android/Core/Desktop package was built in Phase 4. The Phase 2 APKs remain mapped only to source-verification commit `654ebc27ea3629c4f58613bfc7c8d9e8f0b2b659` and their recorded hashes; they are not relabeled as artifacts of this documentation/gate commit. Core cold-build evidence remains separate from APK embedding claims.

## Acceptance

The Android/Core/Desktop template now requires identity, full commit, clean status, controlled-dirty state, configuration/dependency fingerprints, artifact/signing identity, installation state, functional matrix, distribution boundary, and evidence index. Build numbers are explicitly monotonic per platform/distribution identity. Android, Core, and Desktop verification states remain distinct. Public distribution still requires separate authorization.
