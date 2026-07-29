# Nome build records

Every device or distributable build must record the following fields. Unknown
historical values must be marked `not recorded`; they must never be inferred
from a later commit or artifact.

## Required identity

- Platform, marketing version, build number, date and configuration.
- Bundle ID or Application ID.
- Git repository, branch and full commit SHA.
- Exact `git status --short` output at build time.
- Whether a controlled dirty patch was used. Public releases must use `no`.
- Configuration source and non-secret fingerprint. Never record secret values.
- Core, Libraries and other packaged dependency versions or hashes.

## Required artifact and installation evidence

- Standard artifact filename and SHA-256.
- Installed device, installation method and whether the existing container was
  preserved, replaced or unavailable.
- Verification matrix covering activation/invite policy, messaging, channels,
  calls, upgrade and reinstall. Mark every row `PASS`, `FAIL`, `BLOCKED` or
  `NOT RUN`, with an evidence path.
- Distribution boundary: local install, internal test or public release.

Use this artifact naming convention when an archive is exported:

```text
Nome-<platform>-<version>-build<build>-<YYYYMMDD>-<configuration>.<extension>
```

Diagnostic builds must include `Debug` or `Diagnostic` in both the artifact name
and the build record. A successful compile alone is not an installation or
runtime verification result.

## Build gate

A new package is valid only when it is built from a consolidated worktree. The
build gate must fail on tracked changes or unrecorded untracked source files;
ignored caches may remain. Build numbers increase monotonically. Record the
pre-build status, commit and resulting artifact hash before installation.

Use `scripts/release/check-nome-build-source.sh` before and after packaging.
The executable gate requires an authorized consolidated branch, rejects any
tracked, staged or non-ignored untracked state, fingerprints configuration
without printing its values, and binds an optional artifact size/hash to the
same clean commit. Copy `TEMPLATE.md` for each new build.

Build numbers are monotonic per platform and distribution identity. Android,
iOS, Desktop and Web results remain separate; success on one platform never
implies that another platform built, installed, or passed runtime verification.
