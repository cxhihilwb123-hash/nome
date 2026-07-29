# Phase 4 cross-platform build-discipline execution record

Date: 2026-07-29 (Asia/Shanghai)

## Shared implementation

- Android/Core/Desktop evidence source: branch `codex/nome-v656-consolidated-android`, gate/template commit `bd63eaf77d10bc44cc85031de57320eb1f38958f`.
- iOS evidence source: branch `codex/nome-v656-consolidated-ios`, gate/template commit `94e4b9d7c226024b7a4e056037e932b38c73986f`.
- Executable gate: `scripts/release/check-nome-build-source.sh`
- Gate tests: `scripts/release/test-check-nome-build-source.sh`
- Mandatory record template: `plans/builds/TEMPLATE.md`

The source gate accepts only an explicitly authorized branch at one clean commit. It rejects tracked, staged, detached-HEAD, wrong-branch, and non-ignored untracked states. Ignored caches may remain. It emits paths and SHA-256 fingerprints, never configuration values, and can bind one artifact's path, size, and SHA-256 to that same commit.

## Shared verification

- Shell syntax: PASS.
- Gate fixtures: PASS for clean commit and ignored cache.

## Android/Core/Desktop verification

- Negative fixtures: PASS; wrong branch, tracked change, staged change, and untracked Kotlin source were rejected.
- Artifact fixture: PASS; byte size and SHA-256 were emitted against the clean fixture commit.
- First actual gate attempt: diagnostic FAIL because `gradle/libs.versions.toml` does not exist; retained as `android-source-gate.log`.
- Corrected actual consolidated source gate: PASS at `bd63eaf77d10bc44cc85031de57320eb1f38958f`.
- Actual configuration fingerprints: Android `build.gradle.kts` `0506bf94f2edb8968d85066134989e2690053b544af18f6f1afca941d05ceed5`; multiplatform `gradle.properties` `7ae33b7f94abdaa1c4134d99e05fe48f9ecd1c083bb8e410b668b97942bf6311`.
- Evidence: `/Users/forkman03/project/nome/consolidation-evidence/20260729_phase4_build_discipline`.

No new Android/Core/Desktop package was built in Phase 4. The Phase 2 APKs remain mapped only to source-verification commit `654ebc27ea3629c4f58613bfc7c8d9e8f0b2b659` and their recorded hashes; they are not relabeled as artifacts of this documentation/gate commit. Core cold-build evidence remains separate from APK embedding claims.

## iOS verification

- Negative fixtures: PASS; wrong branch, tracked change, staged change, and untracked Swift source were rejected.
- Artifact fixture: PASS; byte size and SHA-256 were emitted against the clean fixture commit.
- Actual consolidated source gate: PASS at `94e4b9d7c226024b7a4e056037e932b38c73986f`.
- Actual configuration fingerprints: `Local.xcconfig` `f323b5e761c9417bbe238f79d7918449a52b65582fcb170723d971917cec5491`; `Debug.xcconfig` `4afac2221a6d30b8a86c35b9b924e79a4cffba068fe5c6f90212ab45f349527d`.
- Evidence: `/Users/forkman03/project/nome/consolidation-evidence/20260729_phase4_build_discipline`.

No new iOS package was built in Phase 4. The existing build 349 diagnostic remains mapped only to source commit `9d0a52b84beebedab126b791ae71352bd94b768f` and artifact SHA-256 `4678125a45cb92efa2c1c059cb9435167259eea2f3a441564638884a1cbe22e6`; it is not relabeled as an artifact of this documentation/gate commit.

## Artifact provenance boundary

Phase 4 did not create a new Android or iOS package. Historical artifacts retain their recorded source commit and hash; they must not be relabeled as products of the gate/template commits or of a later unified merge.

## Acceptance and authorization boundary

The cross-platform template requires identity, full commit, clean status, controlled-dirty state, configuration/dependency fingerprints, artifact/signing identity, installation state, functional matrix, distribution boundary, and evidence index. Build numbers are monotonic per platform/distribution identity. Android, Core, iOS, macOS Desktop, and Windows Desktop verification states remain distinct.

- Phase 4: PASS.
- Public distribution: NOT AUTHORIZED and requires separate Phase 7 authorization.
