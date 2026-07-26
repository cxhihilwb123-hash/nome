# Nome iOS v6.5.6.1 official prebuilt Core integration

Date: 2026-07-22 (Asia/Shanghai)

## Verdict

PASS for the iOS v6.5.6.1 Core integration batch.

The official prebuilt arm64 Swift JSON Core can compile and link for both the
generic iOS device destination and the Apple Silicon iOS Simulator after its
Darwin Mach-O platform metadata is converted with the upstream-pinned
`mac2ios` helper. Two preserved simulator profiles then completed three live
messages in each direction with the exact v6.5.6.1 library pair.

This result does not claim a physical iPhone installation, background
notification delivery, TestFlight readiness, or App Store signing.

## Artifact provenance

- Official Hydra build: <https://ci.zw3rk.com/build/1460001>
- Product: `pkg-ios-aarch64-swift-json.zip`
- Official product size: `118366211` bytes
- Product SHA-256:
  `87a763c55b205585f306a25855972e2ab313a5c7349e72f239125b1fbb81b245`
- Core package version: `6.5.6.1`
- Build source: `686437d2c7ecb6423679367bf7d2bcb90ac7d867`
- Release source parent:
  `59fce95d3cd08897b4ef742447b785cf2e56c7ce` (`v6.5.6`)
- GHC runtime archive version: `8.10.7`

The build source is an official merge containing the v6.5.6 release commit as
a parent. Its changes relative to that parent are build, CI, desktop and Nix
files; no Haskell Core, protocol, database, or message state-machine source is
changed. This makes the artifact release-equivalent at the Core source level,
although it is not byte-identical to the GHC 9.6.3 x86_64 release artifact.

The downloaded ZIP was independently checked for size, SHA-256 and ZIP
integrity before extraction. Both Core archives export the required Swift FFI
entry points, including `chat_migrate_init`, `chat_send_cmd`,
`chat_recv_msg_wait` and `chat_json_length`.

## Platform conversion and installed hashes

The official archive members initially reported Mach-O platform `1` (macOS).
The project-pinned `zw3rk/mobile-core-tools` commit
`4dcb77d5ea896d749381806dfab5358851b08951` was used as follows:

- device copies: `mac2ios FILE`, resulting platform `2` (`IOS`);
- simulator copies: `mac2ios -s FILE`, resulting platform `7`
  (`IOSSIMULATOR`).

All installed archives are arm64. Device hashes:

| Archive | SHA-256 |
|---|---|
| `libHSsimplex-chat-6.5.6.1-9AkeJLQXnDo2qMAZrPTWYx-ghc8.10.7.a` | `37f5435b74ec50ede81ee2be5649ae2911f70afc1b5d38481a73486b36a4b2ee` |
| `libHSsimplex-chat-6.5.6.1-9AkeJLQXnDo2qMAZrPTWYx.a` | `cd97854c4c411cf752085c158f88c4667923f037dd1d35735e78e215ac5a7ba1` |
| `libffi.a` | `c709f448e0435851466dff0ce92c45e073c5a59aeabe7942259ebf8cef0d11e6` |
| `libgmp.a` | `71b1406be252a09ee0661cd05b319712c40477de5771604fdbdbdc7d71f8f62c` |
| `libgmpxx.a` | `b15b8849b259887886f1ea5abdd96528b74e3161daaaa98e11dac883c21a2a25` |

Simulator hashes:

| Archive | SHA-256 |
|---|---|
| `libHSsimplex-chat-6.5.6.1-9AkeJLQXnDo2qMAZrPTWYx-ghc8.10.7.a` | `804231140fb225e3f984a7744bdb295e3a8be48cca755cc1d80c181f1ea7211f` |
| `libHSsimplex-chat-6.5.6.1-9AkeJLQXnDo2qMAZrPTWYx.a` | `748c8e7a5890f20c654b1ad0902e3b6320fad3b70386926357ce0bb4b322542b` |
| `libffi.a` | `65f8fad506eac7583d5a33e7094bd10b0deaafbcf327f713795f14f0d4f8bd28` |
| `libgmp.a` | `7b01aecbfdd54fe98a89e69566b6926b6d649f72fb74b2488d851d8fc3858c69` |
| `libgmpxx.a` | `0cd4671297fa0b05e41bb858aec83d65632e22c445b363c4c20e1b0864ac5788` |

The previous v6.5.5 simulator libraries were preserved outside the repository
at `/tmp/nome-ios-sim-libs-backup-v655-before-v6561-build1460001` during the
integration run. Large Core archives remain under the ignored
`apps/ios/Libraries/` path and are not committed.

## Tooling changes and tests

The Core preparation and Xcode reference helpers now recognize the actual
`-ghc<version>.a` suffix instead of hard-coding GHC 9.6.3. The device prepare
helper also:

- checks every archive for platform `IOS` (`2`);
- rejects raw Darwin platform `1` archives;
- supports `--convert-darwin` using a staged copy;
- never converts the caller's source directory in place.

The read-only installed-Core preflight now checks device platform `2` and
simulator platform `7` in addition to architecture, size and preview markers.

Passing helper tests:

- `scripts/ios/test-check-real-core-arch.sh`
- `scripts/ios/test-check-real-core-xcode-sync.sh`
- `scripts/ios/test-alias-device-real-core-project-libs.sh`
- `scripts/ios/test-sync-real-core-xcode-project.sh`
- `scripts/ios/test-prepare-device-real-core.sh`
- `scripts/ios/test-run-real-core-batch0.sh`
- `scripts/ios/sync-real-core-xcode-project.sh --check`
- `scripts/ios/check-real-core.sh --target all`

The final aggregate readiness audit completed with `51 PASS`, `4 WARN`,
`6 BLOCKED` and `0 FAIL`. The remaining blockers are the declared manual QA,
physical-device, release-identifier and final App Store boundaries rather than
Core integration regressions. Its report is stored outside Git at
`/tmp/nome-ios-v6561-build1460001-readiness-v2`.

## Xcode build evidence

Generic device build, with signing disabled:

- status: PASS;
- app: `Nome.app`;
- extensions: `SimpleX NSE.appex`, `SimpleX SE.appex`;
- evidence root: `/tmp/nome-ios-v6561-build1460001-generic-device`;
- DerivedData: `/tmp/nome-ios-v6561-build1460001-derived-device`.

An arm64 Simulator Debug build against dedicated simulator
`539249D8-761C-4DB0-966C-75E083869D91` also completed successfully. The build
reported existing upstream Swift concurrency and deprecation warnings, with no
Core symbol, architecture, platform, or linker errors.

## Bidirectional real-message diagnostic

- run id: `v6561-b1460001-20260722a`;
- evidence root:
  `/tmp/nome-ios-real-core-v6561-build1460001-diagnostic`;
- Lab to Peer: 3 local sends PASS, 3 live receives PASS;
- Peer to Lab: 3 local sends PASS, 3 live receives PASS;
- local send completion range: 0.424-0.503 seconds;
- live receive range: 4.178-17.239 seconds, including independent XCTest/app
  startup on the first message in each direction;
- all composer-cleared assertions: true;
- all exact message assertions: PASS.

Four independent result bundles passed:

- `lab-to-peer-send.xcresult`
- `lab-to-peer-receive.xcresult`
- `peer-to-lab-send.xcresult`
- `peer-to-lab-receive.xcresult`

Final screenshot hashes:

- Lab: `9a1244502a0d0450e30cee7ac6aaafc499278359d812b8b707d8f7c59b14e405`
- Peer: `8b2e31ec628239ddc2abf91908bc0569e9b6cc8b613c5afad24d3fa11cffcd88`

The screenshots visibly show the last expected diagnostic message on both
Nome chat lists. They remain outside Git because they contain local simulator
conversation data.

Both dedicated simulators began in `Shutdown` and were confirmed in `Shutdown`
again after the script exited. The ordinary iPhone 17 Pro simulator and the
Android worktree/test process were not targeted.

## Connection-recovery conclusion

The v6.5.5 compatibility batch had once observed a receiver that recovered
after an app-process restart. That stale condition did not reproduce with the
v6.5.6.1 Core. In this run, each independent receiver test launched/reconnected,
entered a live wait before its sender, and received all three exact messages.

This validates app-process restart/reconnect and live subscription recovery for
the tested simulator state. It does not replace a physical-device
background/foreground notification matrix.

## Remaining boundary

- Connect, unlock and trust a physical iPhone before claiming device runtime.
- Run physical-device launch, keychain, notification, camera, share extension
  and background/foreground checks.
- Keep App Store identity/signing changes separate from this Core integration.
- Do not push to the official `origin` remote.
