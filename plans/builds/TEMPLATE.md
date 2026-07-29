# Nome <platform> <version> build <number>

## Identity

- Date/time and timezone:
- Platform / architecture:
- Marketing version / build number:
- Configuration:
- Bundle ID / Application ID:
- Git repository:
- Authorized consolidated branch:
- Full source commit:
- Pre-build `git status --short`: `clean`
- Controlled dirty patch: `no` (mandatory for a formal release)
- Source-gate evidence path:

## Configuration and dependencies

- Configuration source path(s):
- Non-secret SHA-256 fingerprint(s):
- Core/Libraries/dependency versions or aggregate hashes:
- Toolchain versions:

Never copy secret configuration values into this record.

## Artifact

- Standard filename: `Nome-<platform>-<version>-build<number>-<YYYYMMDD>-<configuration>.<extension>`
- Output path:
- Bytes:
- SHA-256:
- Signature/signing state and verification command:
- Post-build source-gate evidence path:

## Installation

- Device / OS:
- Installation method:
- Existing container: `preserved` / `replaced` / `not applicable` / `unknown`

## Verification matrix

| Gate | Status | Evidence |
|---|---|---|
| Activation/invitation policy | NOT RUN | |
| Messaging | NOT RUN | |
| Channel create/join/presentation | NOT RUN | |
| Call rejection synchronization | NOT RUN | |
| Upgrade | NOT RUN | |
| Reinstall/recovery | NOT RUN | |

Use only `PASS`, `FAIL`, `BLOCKED`, or `NOT RUN`.

## Distribution boundary

- State: `local build` / `internal test` / `public release`
- Push/deploy/upload authorization reference:
- Public release performed: `no` unless separately authorized and evidenced

## Evidence index

- Build log:
- Test logs:
- `SHA256SUMS.txt`:
- Release/deployment readback:
