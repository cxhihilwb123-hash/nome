# Nome <version> coordinated release

## Release identity

- Status: `DRAFT`
- Marketing version:
- Temporary release branch: `release/nome-v<version>`
- Branch point from unified authority:
- Declared platform set: Android / iOS / macOS / Windows
- Website/control-plane change required: `yes` / `no`
- Release owner:
- Started at (time and timezone):
- Finalized at (time and timezone):

## Scope

- Included changes:
- Explicitly excluded unfinished features:
- Known limitations:
- Release-only fixes that must be merged back:

## Platform ledger

| Platform | Build | Source commit | Build record | Source tag | Artifact SHA-256 | Publication state | Readback evidence |
|---|---:|---|---|---|---|---|---|
| Android | | | | `nome-v<version>-android-b<build>` | | NOT RUN | |
| iOS | | | | `nome-v<version>-ios-b<build>` | | NOT RUN | |
| macOS | | | | `nome-v<version>-macos-b<build>` | | NOT RUN | |
| Windows | | | | `nome-v<version>-windows-b<build>` | | NOT RUN | |

Use only `PASS`, `FAIL`, `BLOCKED` or `NOT RUN` for individual gates. Use the
release-level states defined in [`README.md`](./README.md) for the overall
status. Remove platforms that are not in the declared release set; do not mark
them implicitly passed.

## Website/control-plane ledger

- Website repository commit:
- Website manifest path:
- Public artifact object/key:
- Website/API deployment identity:
- Download readback result:

## Tag authorization and readback

- Platform tag authorization reference:
- Platform tags created:
- GitHub tag readback:
- LAN tag readback:
- Optional umbrella tag: `not created`
- Umbrella tag authorization/readback:

## Merge-back and retirement

- Release-only commits merged into unified authority:
- Unified authority commit after merge-back:
- Temporary branch retirement gate: `BLOCKED`
- Temporary worktree/branch deletion authorization:
- Cleanup result:

## Final statement

State exactly which platforms were built, signed, tested, uploaded and publicly
available. Do not generalize one platform's evidence to another platform.
