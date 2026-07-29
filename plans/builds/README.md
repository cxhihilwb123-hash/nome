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

A new client package is valid only when it is built from the clean unified
client worktree on an explicitly authorized branch. The current unified branch
is `codex/nome-v656-unified`; it must pass consolidation acceptance before it is
used as a release authority. The build gate must fail on detached HEAD, wrong
branch, tracked changes, staged changes, or non-ignored untracked files. Ignored
caches and ignored local configuration may remain. Record the pre-build status,
commit and resulting artifact hash before installation.

Use `scripts/release/check-nome-build-source.sh` before and after packaging.
The executable gate fingerprints configuration without printing its values and
binds an optional artifact size/hash to the same clean commit. Copy
`TEMPLATE.md` for each new build.

Build numbers are monotonic per platform and distribution identity. Android,
iOS, macOS Desktop, and Windows Desktop results remain separate; success on one
platform never implies another platform built, installed, or passed runtime
verification. Core and packaged native Libraries require their own version or
hash evidence. Current macOS acceptance does not establish Windows support.

Website/control-plane packages use the independent Website repository, its own
source gate, and its own build record. Website results are not client-repository
build results.

A formal release requires `Controlled dirty patch: no` and separate explicit
Phase 7 authorization for distribution, deployment, store action, or public
release.
