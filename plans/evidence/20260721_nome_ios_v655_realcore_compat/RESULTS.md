# Nome iOS v6.5.5 Real-Core Compatibility Smoke

Date: 2026-07-21

## Scope and verdict

This is a compatibility experiment that runs the official SimpleX Chat Core
v6.5.5 artifacts against the Nome/SimpleX v6.5.6.1 Swift UI. It is not proof
for an exact v6.5.6.1 Core build.

Verdict: **PASS with a restart-recovery caveat.** Two isolated iOS simulators
created independent profiles, established a real contact through an SMP relay,
exchanged messages in both directions, and preserved the conversation across
app restarts. One peer send background task stalled for more than 30 seconds;
restarting only that peer app preserved its database and completed delivery in
both directions. This caveat prevents treating the result as production-ready.

## Core artifact and simulator conversion

- Official archive: `pkg-ios-aarch64-swift-json.zip`
- Official Hydra download:
  `https://ci.zw3rk.com/build/1424072/download/1/pkg-ios-aarch64-swift-json.zip`
- SHA-256:
  `3b5e625088a6cbc57fbbec2f147e0618765bdcc9d56fc27f940c949f0e3f835b`
- The five static libraries were converted from Darwin arm64 platform metadata
  to `IOSSIMULATOR` metadata using
  `scripts/ios/convert-darwin-archive-to-ios-simulator.py`.
- `xcrun vtool -show-build` validation passed for converted Mach-O members.
- Converted libraries used by the build:
  - `libHSsimplex-chat-6.5.5.0-57WEoB2fiWaFgCB1XksYmP-ghc9.6.3.a`
  - `libHSsimplex-chat-6.5.5.0-57WEoB2fiWaFgCB1XksYmP.a`
  - `libffi.a`
  - `libgmp.a`
  - `libgmpxx.a`

The generated libraries remain excluded by `apps/ios/.gitignore` and are not
part of this commit. On a clean checkout, download and verify the archive,
extract it into a temporary input directory, and convert each of its five `.a`
members into `apps/ios/Libraries/sim`:

```bash
artifact_dir=/private/tmp/nome-ios-real-core-v655-official
input_dir="$artifact_dir/input"
sim_dir=apps/ios/Libraries/sim
archive_url=https://ci.zw3rk.com/build/1424072/download/1/pkg-ios-aarch64-swift-json.zip
expected_sha=3b5e625088a6cbc57fbbec2f147e0618765bdcc9d56fc27f940c949f0e3f835b
sdk_version="$(DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun --sdk iphonesimulator --show-sdk-version)"

mkdir -p "$input_dir" "$sim_dir"
curl --fail --location \
  --output "$artifact_dir/pkg-ios-aarch64-swift-json.zip" \
  "$archive_url"
actual_sha="$(shasum -a 256 "$artifact_dir/pkg-ios-aarch64-swift-json.zip" | awk '{print $1}')"
test "$actual_sha" = "$expected_sha"
unzip -oq "$artifact_dir/pkg-ios-aarch64-swift-json.zip" -d "$input_dir"
for input_archive in "$input_dir"/*.a; do
  python3 scripts/ios/convert-darwin-archive-to-ios-simulator.py \
    "$input_archive" "$sim_dir/$(basename "$input_archive")" \
    --sdk "$sdk_version"
done
```

The converter intentionally refuses to overwrite existing output archives.

## Build evidence

- Xcode build result: `BUILD SUCCEEDED`
- Toolchain: Xcode 26.6 (`17F113`), iOS Simulator SDK 26.5
- App:
  `Nome.app` from the Debug iPhone Simulator products directory
- The build products and full Xcode log were retained as machine-local test
  artifacts; the reproducible command and result are recorded here instead of
  committing user-specific absolute paths.
- Xcode was selected per command with
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`; the machine-wide
  `xcode-select` setting was not changed.

Reproducible simulator build command:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  /Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild \
  -project apps/ios/SimpleX.xcodeproj \
  -scheme "SimpleX (iOS)" \
  -configuration Debug \
  -destination "generic/platform=iOS Simulator" \
  -derivedDataPath /private/tmp/nome-ios-v655-realcore-deriveddata \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=YES \
  build
```

The same branch and converted libraries passed a fresh incremental build during
the 2026-07-22 07:13 CST (+0800) pre-commit review. The explicit architecture
constraint is required: these compatibility artifacts are arm64-only and do not
support an `x86_64` simulator destination.

## Converter regression checks

- `python3 scripts/ios/test_convert_darwin_archive_to_ios_simulator.py -v`
  passed 5 tests.
- Coverage includes duplicate archive members, BSD extended names, symbol-table
  exclusion, invalid/truncated archives, and rejecting non-Mach-O members before
  invoking Xcode tools.
- A real `libffi.a` smoke conversion completed with 8 members, and a converted
  object member reported `platform IOSSIMULATOR` through `vtool -show-build`.

## Isolated simulator identities and persisted databases

1. `Nome RealCore Lab v655`
   - Profile: `nometest` / `Nome Test`
   - `simplex_v1_chat.db`: 1,265,664 bytes
   - `simplex_v1_agent.db`: 430,080 bytes

2. `Nome RealCore Peer v655`
   - Profile: `nomepeer`
   - `simplex_v1_chat.db`: 1,265,664 bytes
   - `simplex_v1_agent.db`: 430,080 bytes

Both profiles and databases survived app termination and relaunch.

## Relay and contact flow

- `smp10.simplex.im`: server test timed out.
- `smp8.simplex.im`: server test passed, but the contact handshake repeatedly
  failed with a connection error.
- `smp17.simplex.im`: contact handshake succeeded; the peer changed from the
  Connect action to a live message composer and both clients gained the contact.
- Temporary native XCUITest automation configured both `smp8`-only and
  `smp17`-only server selections. Each configuration run reported 1 test and
  0 failures. The temporary test code was removed after the smoke test.
- A real invitation was created, parsed, and accepted. No invite bearer or QR
  payload is retained in this evidence record.
- The notification server `ntf3.simplex.im` timed out and simulator notification
  permission was disabled; Core messaging still completed.

## Real two-way messaging

- Peer sent: `peer-smoke-2326`.
- Main sent: `ain-reply-2334m` (the simulator typing automation clipped the
  intended leading/trailing characters; this is the exact delivered payload).
- Main's send task completed in about one second. Peer's first send task remained
  active for more than 30 seconds, and neither side initially displayed the
  remote message.
- Only the Peer app process was terminated and relaunched. Its identity, contact,
  and database remained intact. Peer then showed unread `ain-reply-2334m`, while
  Main received `peer-smoke-2326`.
- Final Main state showed outgoing `ain-reply-2334m` with double checks and
  incoming `peer-smoke-2326`.
- Final Peer state showed outgoing `peer-smoke-2326` with double checks and
  incoming `ain-reply-2334m`.
- Main was subsequently terminated and relaunched. Both messages remained in
  the conversation, confirming local database persistence.

Screenshots were retained locally outside the repository. Their stable file
names and hashes are recorded without machine-specific paths:

- `nome-v655-realcore-main-message-received.png`
  - SHA-256: `642121bcc6f240147f21ed7b15648ebd1324b106583d1007e4f3bfde171729b6`
- `nome-v655-realcore-peer-message-received.png`
  - SHA-256: `ab92ca4ed201bd3d32e6270310610f1dab1b1bb7bb32d446bbc5c8bbe6c09281`
- `nome-v655-realcore-main-relaunch-persistence.png`
  - SHA-256: `f5676de771fab9d7d5fd58b263df936b4fd12235ef6f3370e62dacb065c3d36c`

## Remaining boundary

- This passes a v6.5.5 Core / v6.5.6.1 Swift UI compatibility smoke only.
- The exact v6.5.6.1 Core simulator artifact is still unproved. Building it
  locally requires Nix, which is not installed and requires administrator
  authorization on this machine.
- The stalled peer send/recovery path needs focused diagnosis before a production
  readiness verdict.
- No Android source, build, process, or simulator was targeted by this work.
