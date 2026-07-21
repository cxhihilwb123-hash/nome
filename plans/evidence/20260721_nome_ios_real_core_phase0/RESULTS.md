# Nome iOS real-core Phase 0 results

Captured: 2026-07-21 (Asia/Shanghai)

## Verdict

No real-core route is ready for mutating preparation or functional QA.

| Route | Classification | Evidence-backed reason |
|---|---|---|
| Apple Silicon simulator | `BLOCKED` | Xcode exposes arm64 simulators, but installed arm64 archives are 10,104-byte preview libraries with preview markers. The local production-sized simulator candidate is x86_64 and reports `6.5.5.0`. |
| Physical iPhone/iPad | `BLOCKED` | `xctrace list devices` places `CryHandsome` under `Devices Offline`. The local arm64 candidate reports `6.5.5.0`, lacks the required GHC companion archive, and the dry-run preparation fails. `apps/ios/Libraries/ios` is absent. |
| Exact local source build | `BLOCKED` | Nix is not installed and no executable `mac2ios` exists anywhere under `/Users/forkman03/project/nome`. A local build was not started. |

The next safe independent product batch is accessibility and large Dynamic Type
verification against the existing preview simulator. It must remain clearly
separate from real communication proof.

## Repository protection

- iOS worktree: `/Users/forkman03/project/nome/simplex-chat-ios`
- iOS branch: `codex/nome-ios-v656`
- iOS HEAD: `27eb6e4100ec9caa9a9a755d8222e95f9e12c2b3`
- Android worktree: `/Users/forkman03/project/nome/simplex-chat`
- Android HEAD: `6a01efa5e6d10dc0743cdf82dfb69e09cc459862`
- Android staged paths: none
- Android tracked diff paths: four pre-existing paths
  (`build.gradle.kts`, `SimplexApp.kt`, `SimplexService.kt`, and
  `NetworkObserver.kt`)
- Android tracked diff SHA-256 before and after Phase 0:
  `32a560f93ec6393e0806e7182b7a2e6a577eff1ad67313279b445bf6496f37f0`
- Android untracked snapshot before and after Phase 0: 22,127 files, of which
  22,111 are under Android-owned `.gradle-review/` runtime output

No Phase 0 command targeted an Android path for mutation. No reset, clean,
stash, stage, commit, or push was run.

## Installed simulator libraries

- Checkout core version: `simplex-chat.cabal` = `6.5.6.1`
- Xcode marketing/build version: `6.5.6` / `337`
- Xcode archive references: `6.5.6.1`
- Installed archives: five files, each 10,104 bytes, each arm64
- Preview marker scan: positive in all five installed archives
- `scripts/ios/check-real-core.sh`: expected failure
- `scripts/ios/check-ios-simulator-real-core-route.sh`: both arm64 and x86_64
  routes unusable; live full-Xcode check confirms available simulator
  destination architecture is arm64

## Local downloaded candidates

Both candidate folders are flat four-archive sets and contain no matching GHC
companion archive.

| Package | Core version | Core size | Architecture | Preview markers | Core SHA-256 |
|---|---:|---:|---|---|---|
| `pkg-ios-aarch64-swift-json` | `6.5.5.0` | 75,809,608 bytes | arm64 | none found | `93af81497bf00b6f2963060630f5e4193890d170a3f79cd34ee2a206b8e63270` |
| `pkg-ios-x86_64-swift-json` | `6.5.5.0` | 77,520,472 bytes | x86_64 | none found | `38f5df480d287ea08407f4c68532650045a571dd3b0f3b63e43ba83bd08dba9b` |

`scripts/ios/prepare-device-real-core.sh` remained in dry-run mode and failed
before any copy with `Expected exactly one device GHC libHS archive, found 0`.
This is stricter evidence than the high-level source audit's production-size
check: the arm64 folder is an incomplete and version-mismatched compatibility
candidate, not an installable v6.5.6 device package.

## Xcode and device state

- Full Xcode and generic `Any iOS Device` destination: available
- Signing snapshot: team configured, automatic signing, Apple Development
- Current compatibility bundle identifier: `chat.simplex.app`
- Physical device: `CryHandsome` appears only under `Devices Offline`
- Device libraries: `apps/ios/Libraries/ios` missing
- Available disk: 81 GiB

The Phase 0 version of `check-ios-device-readiness.sh` incorrectly included the
`Devices Offline` section in its connected-device result because it only
stopped parsing at `== Simulators ==`. Raw `xctrace` output was authoritative
for this packet. The parser was then fixed under `plans/20260721_06.md`; its
fixture test and a live full-Xcode rerun now correctly report no connected
physical iPhone/iPad.

## Safety outcome

- No Core archive was copied, patched, downloaded, built, or replaced.
- No Xcode project, signing setting, release identifier, Swift/Haskell source,
  Android source, or installed app was changed.
- No heavy Xcode build ran.
- No remote mutation occurred.
