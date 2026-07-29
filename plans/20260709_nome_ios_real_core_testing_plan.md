# Nome iOS real-core testing plan

Date: 2026-07-09

## Purpose

The current Nome iOS simulator build is useful for UI and navigation QA, but it
still links against preview iOS core libraries. Real invitation generation,
public contact addresses, group joining, database behavior, desktop linking, and
two-account messaging cannot be claimed complete until the real iOS core
libraries are installed and verified.

This file is the handoff for the next testing phase.

## Current evidence

Current command:

```bash
scripts/ios/check-real-core.sh
```

Source audit command:

```bash
scripts/ios/check-real-core-sources.sh
```

Physical-device source audit command:

```bash
SOURCE_TARGET=physical-device scripts/ios/check-real-core-sources.sh
```

Current result: fails, as expected.

Confirmed blockers:

- `apps/ios/Libraries/ios` is missing.
- `apps/ios/Libraries/sim` contains very small preview archives.
- The simulator archive contains preview markers such as `preview-agent`,
  `simplex:/contact#preview`, `https://nome.local/preview`, and `preview-token`.
- `~/Downloads` now contains `pkg-ios-aarch64-swift-json` and
  `pkg-ios-x86_64-swift-json` artifact directories. They are a `v6.5.5.0`
  device + x86_64-simulator pair, not an arm64-simulator pair for this machine.
- A local `tools/bin/mac2ios` helper is available, so downloaded archives can be
  patched once the iOS core artifacts are available.
- `nix` is not installed, so local flake builds of the iOS core are unavailable
  on this machine.
- `scripts/ios/check-real-core-build-env.sh` confirms the repo has the expected
  flake iOS build targets and `pkg-ios-*-swift-json` output names. It also
  confirms full Xcode is available at `/Applications/Xcode.app/Contents/Developer`
  and `simctl` is available when `DEVELOPER_DIR` points there.
- The system default `xcode-select` path can still be
  `/Library/Developer/CommandLineTools`, where bare `xcrun simctl` fails. Keep
  using `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer` in
  repeatable commands so the iOS build and simulator tooling use full Xcode.
- Current free disk space on the project volume is about 47 GB; the build-env
  checker warns because local Nix iOS core builds may need more than 80 GB.

Updated 2026-07-09 evidence:

- `~/Downloads/pkg-ios-aarch64-swift-json` and
  `~/Downloads/pkg-ios-x86_64-swift-json` now exist.
- These artifacts are a `v6.5.5.0` pair, while this checkout reports
  `6.5.6.1`, so they remain compatibility candidates rather than final proof.
- The aarch64 device artifact looks usable for the physical-device path:
  `libHSsimplex-chat-6.5.5.0-57WEoB2fiWaFgCB1XksYmP-ghc9.6.3.a` is
  293,139,304 bytes, has architecture `arm64`, and has no preview-core markers.
- The x86_64 simulator artifact is production-sized but cannot be used by the
  current Apple Silicon simulator workflow because Xcode exposes only `arm64`
  iOS Simulator destinations.
- `scripts/ios/check-ios-simulator-real-core-route.sh` now checks this
  distinction directly. Current result: installed arm64 simulator libraries are
  still preview-core placeholders, the local x86_64 simulator artifact looks
  production-like, and no x86_64 simulator destination is available to run it.
- `scripts/ios/check-ios-device-readiness.sh` now reports this distinction:
  the local arm64 device artifact is present and production-like, but
  `apps/ios/Libraries/ios` is still missing and no physical iPhone/iPad is
  connected.
- `scripts/ios/prepare-device-real-core.sh` now provides a guarded bridge from
  the local arm64 device artifact into `apps/ios/Libraries/ios`. Its default
  mode is read-only, and `--prepare` is required before it copies files.
- The current dry-run against `~/Downloads/pkg-ios-aarch64-swift-json` passes
  the device artifact checks, but warns that the artifact filenames are
  `6.5.5.0` while the Xcode project references `6.5.6.1`, so this remains a
  physical-device compatibility candidate rather than final build evidence.
- `scripts/ios/check-real-core-xcode-sync.sh` now checks that the explicit
  Xcode archive references match installed simulator libraries and any installed
  device libraries. It also reports the current local arm64 device artifact as
  an advisory mismatch until that artifact is deliberately installed.
- `scripts/ios/export-ios-device-readiness-state.sh` now writes a structured
  physical-device packet whose `device_status.tsv` separates Xcode/signing,
  connected device, local arm64 artifact, installed device libraries, and
  release-identifier review, while `next_actions.tsv` maps each non-pass area
  to a checklist line, blocker group, and QA batch. It also runs the
  `scripts/ios/prepare-device-real-core.sh` dry-run so project-reference
  warnings are visible before any device libraries are installed.
- The physical-device packet now also runs or accepts a
  `SOURCE_TARGET=physical-device` source-audit log. Current direct export
  `/tmp/nome-ios-device-readiness-source-audit-20260710-024811` reports
  `physical_device_source_audit` as PASS, confirming the local arm64 device
  artifact is visible through the same source-audit path used by Batch 5.
- `scripts/ios/export-real-core-route-state.sh` now writes a route packet whose
  `next_actions.tsv` maps each real-core route to its current status, command,
  evidence file, manual QA checklist line, blocker group, and execution batch.
  Current route action evidence is
  `/tmp/nome-ios-real-core-route-state-actions-20260710-011307`:
  arm64 simulator, x86_64 simulator, and local Nix build map to checklist line
  13 / batch 0; the physical-device route maps to checklist line 40 / batch 5.
- The route packet now summarizes physical-device readiness with both relevant
  blockers when they coexist: no trusted iPhone/iPad is connected, and
  `apps/ios/Libraries/ios` is missing. Its physical-device next action points
  at `scripts/ios/export-ios-device-readiness-state.sh` so the detailed device
  evidence can split connected-device, installed-library, signing, and release
  identifier rows.
- `scripts/ios/export-real-core-route-state.sh` now consumes the structured
  device-readiness packet when it runs the physical-device route. Current
  direct export `/tmp/nome-ios-real-core-route-state-device-source-20260710-025609`
  reports the physical-device route as blocked with a more precise reason:
  the physical-device source audit passed for the local arm64 artifact, but no
  trusted iPhone/iPad is connected, `apps/ios/Libraries/ios` is missing, and
  project references are still WARN.
- `scripts/ios/export-nome-qa-batches.sh` now generates Batch 0 evidence
  templates that require route-state, source-audit, staged-artifact, guarded
  `--prepare`, and final preflight evidence. This behavior is covered by
  `scripts/ios/test-export-nome-qa-batches.sh`.
- `scripts/ios/check-real-core-sources.sh` now supports
  `SOURCE_TARGET=simulator|physical-device|any`, so the audit can distinguish
  the current arm64 simulator blocker from the physical-device candidate path.
  Current live check: the default simulator target still exits non-zero because
  no arm64 simulator real-core artifact is available, while
  `SOURCE_TARGET=physical-device` passes against the local
  `~/Downloads/pkg-ios-aarch64-swift-json` artifact as a true-device testing
  candidate.
- `scripts/ios/test-export-ios-device-readiness-state.sh` now verifies that
  the physical-device source audit is recorded as a PASS area and mapped to
  checklist line 18 / Batch 0.

Checked but not sufficient:

- Homebrew does not provide a `mac2ios` formula.
- `zw3rk/mobile-core-tools` has no GitHub release assets; the helper must be
  built from source.
- The public `simplex-chat/simplex-chat` GitHub Actions artifact pages checked so far did
  not expose reusable `pkg-ios-aarch64-swift-json` or
  `pkg-ios-x86_64-swift-json` artifacts. The visible artifacts were
  dockerbuild metadata or Linux library artifacts, not iOS core packages.
- `scripts/ios/check-real-core-sources.sh` confirms the current public-source
  picture: local artifacts exist but are not compatible with the current arm64
  simulator path, the recent GitHub release/prerelease window through
  `v7.0.0-beta.3` has no complete `pkg-ios-*-swift-json` aarch64 + x86_64
  pair, and recent GitHub Actions artifact pages do not expose a complete pair.
- The working `ci.zw3rk.com` Hydra project is
  `simplex-chat-simplex-chat`, not the earlier guessed `simplex-chat` paths.
  `master`, `v7-0-0-beta-3`, and `v6-5-5` currently expose both required iOS
  architectures. The same-version `v6-5-6` jobset exposes
  `x86_64-darwin.x86_64-darwin-ios:lib:simplex-chat`, but its standard
  `aarch64-darwin.aarch64-darwin-ios:lib:simplex-chat` endpoint returned 404.
  Because this checkout reports `simplex-chat.cabal` version `6.5.6.1` and iOS
  marketing version `6.5.6`, any non-matching Hydra pair must be treated as a
  compatibility candidate until a full build and smoke pass proves it works.

Safety note:

- `scripts/ios/prepare.sh` and `scripts/ios/prepare-x86_64.sh` now validate
  `mac2ios` and the expected local artifacts before deleting or replacing
  `apps/ios/Libraries/*`.
- This guard has been tested on the current machine: without `mac2ios`,
  `scripts/ios/prepare-x86_64.sh` exits before replacing libraries and leaves
  the existing preview library file list unchanged.
- `scripts/ios/build-mac2ios.sh` can build the helper from
  `https://github.com/zw3rk/mobile-core-tools.git` into `tools/bin/mac2ios`.
  The built binary is local machine output and is ignored by git.
- `scripts/ios/prepare-real-core.sh --downloads` now accepts either extracted
  artifact directories or matching `.zip` files, and `--downloads-dir` /
  `DOWNLOADS_DIR` can point to a custom artifact directory.
- `scripts/ios/stage-real-core-artifacts.sh` can normalize Nix result symlinks,
  local artifact folders, or artifact zip locations into the exact
  `pkg-ios-aarch64-swift-json` + `pkg-ios-x86_64-swift-json` layout expected by
  `scripts/ios/prepare-real-core.sh --downloads-dir`.
- `scripts/ios/run-real-core-batch0.sh` is the preferred Batch 0 entry point.
  It stages a complete artifact pair from a local source directory, downloads
  and stages a supplied Hydra job repository via `--job-repo`, or runs the two
  Nix iOS builds when `--build-with-nix` is supplied. It does not replace
  `apps/ios/Libraries` unless `--prepare` is supplied.
- `scripts/ios/sync-real-core-xcode-project.sh` synchronizes the explicit
  `libHSsimplex-chat...` archive filenames in
  `apps/ios/SimpleX.xcodeproj/project.pbxproj` with the files currently present
  in `apps/ios/Libraries/sim`. This is required after installing real core
  artifacts if the Hydra/Nix library version or hash differs from the preview
  library filenames already recorded in the Xcode project.
- The custom-download guard was tested with an empty temp directory: preparation
  exited before replacement, and the `apps/ios/Libraries` file-list hash stayed
  unchanged.
- `scripts/ios/check-real-core-sources.sh` is read-only. It audits possible
  sources, including recent stable and beta/prerelease GitHub assets plus
  paginated GitHub Actions artifacts, but does not download, unzip, or replace
  libraries. For the default simulator target, it only reports success when it
  finds a complete artifact path compatible with the current simulator
  architecture. Set `SOURCE_TARGET=physical-device` when the next pass is
  explicitly preparing a real iPhone/iPad test; that target succeeds on an
  arm64 device artifact but still does not install it.
- `scripts/ios/prepare-device-real-core.sh` is device-only and opt-in. It
  audits the arm64 package for required archives, production-like core size,
  preview markers, and arm64 support, and installs into
  `apps/ios/Libraries/ios` only with `--prepare`. It never modifies
  `apps/ios/Libraries/sim`, so the current simulator preview path stays intact.
- `scripts/ios/check-real-core-xcode-sync.sh` is read-only. It fails installed
  simulator/device library filename mismatches before a build can accidentally
  use stale `project.pbxproj` references, while treating uninstalled candidate
  device artifacts as warnings.
- By default the source audit includes known SimpleX Hydra candidates:
  `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/master`,
  `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v7-0-0-beta-3`, and
  `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v6-5-5`.
  Set `INCLUDE_KNOWN_HYDRA_REPOS=0` for offline fixture tests.
- `HYDRA_JOB_REPOS` can be set to newline-separated candidate Hydra job
  repository URLs to check multiple candidates in one read-only audit. A single
  `--job-repo URL` still works for one-off checks.
- `scripts/ios/test-real-core-source-audit.sh` tests that source-audit
  success condition locally: no artifacts and one-architecture artifacts fail,
  while a complete aarch64 + x86_64 fixture pair passes.
- `scripts/ios/test-prepare-real-core-safety.sh` tests the preparation guard in
  a temporary sandbox: missing, partial, and empty-architecture artifact sets
  fail before any sandbox library replacement. It also verifies the Hydra
  `--job-repo` path checks source availability, then downloads, then runs
  installed-core preflight.

## Supported acquisition paths

Use one of these paths. Do not mix them in the same pass.

Preferred wrapper:

```bash
scripts/ios/prepare-real-core.sh --job-repo <hydra-job-repo>
```

or, if artifacts are already downloaded:

```bash
scripts/ios/prepare-real-core.sh --downloads
```

The wrapper ensures `mac2ios` is available, prepares the libraries through the
repo's existing scripts, and finishes by running `scripts/ios/check-real-core.sh`.

### Path A: Hydra artifacts

Input needed: a Hydra job repository URL that exposes these jobs:

- `aarch64-darwin.aarch64-darwin-ios:lib:simplex-chat`
- `x86_64-darwin.x86_64-darwin-ios:lib:simplex-chat`

Known candidates verified on 2026-07-09:

- `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/master` exposes both
  architectures, but may drift with upstream master.
- `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v7-0-0-beta-3` exposes
  both architectures, but is newer than this `6.5.6.1` checkout.
- `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v6-5-5` exposes both
  architectures, but is older than this `6.5.6.1` checkout.
- `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v6-5-6` is closest to
  the checkout version, but currently lacks the standard aarch64 iOS artifact
  endpoint and is therefore not a complete pair.

Preflight the URL without replacing files:

```bash
scripts/ios/check-real-core-sources.sh --job-repo <hydra-job-repo>
```

Preflight multiple candidate URLs without replacing files:

```bash
HYDRA_JOB_REPOS="$(printf '%s\n%s\n' \
  'https://example.invalid/job/project/jobset-a' \
  'https://example.invalid/job/project/jobset-b')" \
  scripts/ios/check-real-core-sources.sh
```

Download and stage libraries without replacing app files:

```bash
scripts/ios/run-real-core-batch0.sh \
  --job-repo <hydra-job-repo> \
  --output /tmp/nome-ios-real-core-batch0 \
  --force
```

Then explicitly prepare and re-run:

```bash
scripts/ios/run-real-core-batch0.sh \
  --source /tmp/nome-ios-real-core-batch0 \
  --prepare
scripts/ios/sync-real-core-xcode-project.sh
scripts/ios/check-real-core.sh
```

### Path B: local downloaded artifacts

Input needed:

- `~/Downloads/pkg-ios-aarch64-swift-json/`
- `~/Downloads/pkg-ios-x86_64-swift-json/`

or:

- `~/Downloads/pkg-ios-aarch64-swift-json.zip`
- `~/Downloads/pkg-ios-x86_64-swift-json.zip`

Ensure `mac2ios` is available:

```bash
scripts/ios/build-mac2ios.sh
```

Prepare libraries:

```bash
scripts/ios/prepare-real-core.sh --downloads
scripts/ios/sync-real-core-xcode-project.sh
```

If the artifacts are elsewhere:

```bash
scripts/ios/prepare-real-core.sh --downloads-dir /path/to/artifacts --downloads
scripts/ios/sync-real-core-xcode-project.sh
```

Then re-run:

```bash
scripts/ios/check-real-core.sh
```

### Path B2: local device-only artifact for physical iPhone testing

Input needed:

- `~/Downloads/pkg-ios-aarch64-swift-json/`

This path does not solve Apple Silicon simulator QA. It only prepares the
`iphoneos` library directory for a physical-device build/test once a trusted
iPhone is available.

Audit without replacing files:

```bash
SOURCE_TARGET=physical-device scripts/ios/check-real-core-sources.sh
scripts/ios/prepare-device-real-core.sh
```

Install the audited device libraries only when ready for physical-device
testing:

```bash
scripts/ios/prepare-device-real-core.sh --prepare
scripts/ios/check-real-core-xcode-sync.sh
scripts/ios/check-ios-device-readiness.sh
```

If `apps/ios/Libraries/ios` already contains libraries, re-run with `--force`
only after confirming the automatic backup path printed by the script is
acceptable.

### Path C: local Nix build

Input needed: `nix` installed and able to build this repository's iOS targets.

Build:

```bash
scripts/ios/check-real-core-build-env.sh
mkdir -p /tmp/nome-ios-core-nix-results
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer nix build \
  -o /tmp/nome-ios-core-nix-results/result-aarch64 \
  '.#aarch64-darwin-ios:lib:simplex-chat'
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer nix build \
  -o /tmp/nome-ios-core-nix-results/result-x86_64 \
  '.#x86_64-darwin-ios:lib:simplex-chat'
```

Normalize the resulting artifacts into the same layout expected by Path B, then
prepare:

```bash
scripts/ios/run-real-core-batch0.sh \
  --source /tmp/nome-ios-core-nix-results \
  --output /tmp/nome-ios-core-artifacts \
  --force
scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-core-artifacts --prepare
scripts/ios/sync-real-core-xcode-project.sh
scripts/ios/check-real-core.sh
```

## Pass criteria before real functional QA

Do not begin real add-friend, public-contact, group, or messaging claims until:

- `scripts/ios/check-real-core.sh` exits `0`;
- device libraries exist in `apps/ios/Libraries/ios`;
- simulator libraries exist in `apps/ios/Libraries/sim`;
- `libHSsimplex-chat*.a` files are production-sized, not preview placeholders;
- preview markers are absent from both device and simulator core libraries;
- installed static libraries support the required architectures: `arm64` for
  device and the current simulator architecture, `arm64` on this Apple Silicon
  machine;
- the iOS app builds cleanly with full Xcode;
- the app launches on a clean simulator without a new `Nome-*.ips` crash.

## Real functional QA sequence

After the real-core gate passes:

1. Build and launch a clean simulator app.
2. Create the first local profile through the Nome onboarding flow.
3. Accept network conditions and reach the Nome home screen.
4. Generate a real one-time invitation link and QR code.
5. Use a second account/device/simulator to open that link and establish contact.
6. Send and receive text messages in a one-to-one conversation.
7. Verify the conversation safety banner reflects unverified and verified states.
8. Create or load a real public contact address, then copy/share it.
9. Use a second account to request contact through the public address.
10. Use a real group invitation link to preview and join a group.
11. Send and receive a group message.
12. Open backup/export/import from the Nome backup hub and verify the real screen
    still performs the existing database actions.
13. Test desktop linking with a compatible desktop session.
14. Re-capture real-core screenshots for add friend, public contact, join group,
    and conversation.
15. Make `scripts/ios/check-app-store-screenshots.sh --final` pass after
    selecting ten-or-fewer final screenshots and replacing preview/debug files.

## Evidence to update

Update these files as each gate passes:

- `plans/20260709_nome_ios_qa_record.md`
- `plans/20260709_nome_ios_manual_qa_checklist.md`
- `plans/20260709_nome_ios_release_gate_review.md`
- `design/app-store/ios-real-screens/MANIFEST.md`

## Latest simulator compatibility finding

The staged Hydra `v6-5-5` pair was installed experimentally and passed the
installed-core preflight, but it did not link for the current Apple Silicon
simulator destination. Xcode built for `arm64-apple-ios15.0-simulator`; the
available Hydra simulator package contains `x86_64` archives, so the linker
ignored those libraries and reported missing SimpleX bridge symbols.

The project was restored to the previous preview-core state from
`/tmp/nome-ios-preview-core-backup-20260709-205218` after the failed build.
`scripts/ios/sync-real-core-xcode-project.sh --check` and `git diff --check`
both passed after the rollback.

This means Batch 0 should now prioritize one of these paths:

1. Locate or build an arm64-simulator-compatible SimpleX iOS core for the
   current checkout.
2. Run real-core functional QA on a physical iPhone with the aarch64 iOS
   package.
3. If a compatible x86_64 simulator destination is available, test that path as
   a temporary bridge, but do not treat it as the main Apple Silicon simulator
   workflow.

Current Xcode evidence rules out that temporary bridge on this machine:
`xcodebuild -showdestinations` for `SimpleX (iOS)` reports only `arm64` iOS
Simulator destinations. There is no available `x86_64` iOS Simulator
destination here, so the practical simulator path requires an
arm64-simulator-compatible core.

The scripts now guard this explicitly. `scripts/ios/check-real-core.sh` checks
static archive architecture with `lipo`, `scripts/ios/prepare-real-core.sh
--downloads` audits staged artifacts before replacing `apps/ios/Libraries`, and
`scripts/ios/download-libs.sh` runs the same preflight before calling
`prepare-x86_64.sh` for full dual-arch downloads. The staged `v6-5-5` pair now
fails before install on this machine because its simulator archives are
`x86_64`, while the active simulator build needs `arm64`.

`scripts/ios/check-real-core-sources.sh` now uses the same architecture-aware
readiness model. Public GitHub release assets, recent GitHub Actions artifacts,
and the known Hydra job repositories are not counted as passing sources for
this Apple Silicon simulator when they only provide the standard x86_64
simulator archive.

The simulator route itself now has a repeatable gate:
`scripts/ios/check-ios-simulator-real-core-route.sh`. It is read-only and
checks available simulator destination architectures, installed simulator
library size/markers/architecture, and whether the local x86_64 simulator
artifact has a runnable destination. Current status: `current_arm64_route_usable=0`
and `x86_64_route_usable=0`.

The physical-device fallback now has a repeatable gate:
`scripts/ios/check-ios-device-readiness.sh`. It is read-only and checks whether
Xcode can see a generic iOS device destination, whether a physical iPhone/iPad
is connected and trusted, whether generic iOS signing settings are readable,
and whether arm64 device libraries exist in `apps/ios/Libraries/ios`. Current
status: generic iOS signing settings are readable with automatic signing and
team `5NN7GUYB6T`, but no physical iPhone/iPad is connected and the restored
preview-core checkout does not have `apps/ios/Libraries/ios`.

The physical-device state can now be exported with
`scripts/ios/export-ios-device-readiness-state.sh`. The latest direct packet is
`/tmp/nome-ios-device-readiness-state-project-ref-20260710-022118`: 3 PASS
areas, 2 WARN areas, and 2 BLOCKED areas. It confirms the local arm64 device
artifact is production-like and automatic signing is configured, while the
current blockers are no connected physical iPhone/iPad and no installed
`apps/ios/Libraries/ios`. Its warnings are the upstream-compatible release id
and the fact that the available `6.5.5.0` device artifact archive names differ
from the current `6.5.6.1` Xcode project references.

The real-core route export now points the physical-device route at that device
state export instead of only repeating the broad device-readiness checker. The
latest direct route packet is
`/tmp/nome-ios-real-core-route-state-physical-combined-20260710-020320`: 0
ready routes, 1 build-only route, and 3 blocked routes. Its physical-device
row explicitly says both no trusted iPhone/iPad is connected and
`apps/ios/Libraries/ios` is missing.

The device-only preparation guard is also repeatable:
`scripts/ios/prepare-device-real-core.sh` passes in dry-run mode against the
local arm64 artifact and is covered by `scripts/ios/test-prepare-device-real-core.sh`.
It intentionally does not update `project.pbxproj`; because the available
device artifact is `6.5.5.0` and the current project references are `6.5.6.1`,
that sync remains a separate physical-device build decision.

The Xcode reference guard is now repeatable too:
`scripts/ios/check-real-core-xcode-sync.sh` passes for the current installed
simulator path because `apps/ios/Libraries/sim` matches the project references.
It warns that the local device candidate has different archive names and will
fail if a mismatched device library is installed without a deliberate project
sync.

## Latest source audit with Xcode destination evidence

`scripts/ios/check-real-core-sources.sh` now includes an Xcode simulator
destination architecture audit. This makes the source-audit output self
contained: it no longer only says that the required simulator architecture is
`arm64`; it also records whether Xcode exposes a matching simulator destination
and whether the standard `x86_64` simulator artifacts can run on this machine.

Direct verification with a shortened network scan:

```bash
CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 INCLUDE_KNOWN_HYDRA_REPOS=0 scripts/ios/check-real-core-sources.sh
```

Current result:

- Xcode exposes iOS Simulator destination architecture(s): `arm64`.
- The required simulator architecture `arm64` is available.
- The standard SimpleX simulator artifact architecture is still `x86_64`.
- Standard x86_64 simulator artifacts cannot run here until an x86_64 iOS
  Simulator destination is available.
- Local `pkg-ios-aarch64-swift-json` and `pkg-ios-x86_64-swift-json` artifacts
  exist, but they remain a device-arm64 plus x86_64-simulator pair, not an
  arm64-simulator pair.

Full integrated verification:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-source-audit-20260710-004121
```

This reports 31 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL. The extra blocker is the
explicit real-core source audit: current public releases, recent GitHub Actions
artifacts, known Hydra job repositories, and local Downloads do not provide an
arm64-simulator-compatible core for this machine.

## Latest source audit refresh

Updated 2026-07-10 command:

```bash
CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=18 ACTIONS_ARTIFACT_PAGES=4 DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-real-core-sources.sh
```

Result log: `/tmp/nome-real-core-source-audit-refresh-20260710-013125.log`.

Current result: exits `1`, as expected, because no arm64-simulator-compatible
real-core source is available for this machine.

Evidence from the refreshed audit:

- Xcode exposes only `arm64` iOS Simulator destinations for `SimpleX (iOS)`.
- The required simulator architecture is `arm64`.
- The standard SimpleX simulator artifact architecture remains `x86_64`.
- Local `~/Downloads/pkg-ios-aarch64-swift-json` and
  `~/Downloads/pkg-ios-x86_64-swift-json` are present, but they are still a
  device-arm64 plus x86_64-simulator pair.
- 18 recent GitHub releases were inspected; none exposes
  `pkg-ios-*-swift-json` release assets.
- 4 GitHub Actions artifact pages were inspected; the visible artifacts are
  dockerbuild metadata, not reusable iOS core packages.
- The known Hydra job repositories `master`, `v7-0-0-beta-3`, and `v6-5-5`
  expose the standard device-arm64 plus x86_64-simulator pair, but not an
  arm64-simulator-compatible source for the current simulator path.

Practical consequence: the simulator path still needs either an
arm64-simulator-compatible core build/source, a simulator runtime that can run
the x86_64 simulator archive, or a deliberate move to the physical-device
fallback with a trusted iPhone.

Integrated readiness with the refreshed source audit:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-source-refresh-20260710-013400
```

Result: 34 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL. The source audit is the
additional blocker beyond the usual known release blockers.

## Source target split for simulator vs physical device

Updated 2026-07-10 command for the default simulator route:

```bash
CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 INCLUDE_KNOWN_HYDRA_REPOS=0 scripts/ios/check-real-core-sources.sh
```

Result log: `/tmp/nome-real-core-source-sim-target.log`.

Current result: exits `1`, as expected. The current iOS Simulator destination
architecture is `arm64`, while the available local and standard upstream
artifacts remain a device-arm64 plus x86_64-simulator pair.

Updated 2026-07-10 command for the physical-device source route:

```bash
SOURCE_TARGET=physical-device CHECK_XCODE_DESTINATIONS=0 INCLUDE_KNOWN_HYDRA_REPOS=0 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-real-core-sources.sh
```

Result log: `/tmp/nome-real-core-source-device-target.log`.

Current result: exits `0`. The local
`~/Downloads/pkg-ios-aarch64-swift-json` artifact is a physical-device testing
candidate, but this does not install it, connect a device, or prove real
messaging behavior.

Integrated verification after this source-target split:

```bash
CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-source-target-20260710-023303
CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-source-target-20260710-023303
scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-completion-state-source-target-smoke-20260710-023538 --force
```

Results:

- Readiness: 39 PASS, 0 WARN, 8 BLOCKED, 0 FAIL.
- Goal audit: 51 PASS, 1 WARN, 8 BLOCKED, 0 FAIL.
- Completion state: 2 PASS, 1 WARN, 5 BLOCKED, 0 FAIL.

## Physical-device source audit in device readiness packet

Updated 2026-07-10 command:

```bash
scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-source-audit-20260710-024811 --force
```

Current result:

- PASS areas: 4;
- WARN areas: 2;
- BLOCKED areas: 2.

New PASS area:

- `physical_device_source_audit`: the read-only
  `SOURCE_TARGET=physical-device` source audit sees an arm64 device candidate
  in `~/Downloads/pkg-ios-aarch64-swift-json`.

Remaining BLOCKED areas:

- `connected_device`: no trusted physical iPhone/iPad is connected;
- `installed_device_libraries`: `apps/ios/Libraries/ios` is still missing.

Remaining WARN areas:

- `device_project_references`: local device artifact names are still
  `6.5.5.0`, while the project currently references `6.5.6.1`;
- `release_identifiers`: identifiers are still compatibility-reviewed rather
  than final Nome release identifiers.

Integrated verification after this device-source update:

```bash
scripts/ios/test-export-ios-device-readiness-state.sh
scripts/ios/test-export-nome-ios-completion-state.sh
scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-completion-state-device-source-20260710-024847 --force
CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-device-source-20260710-024918
CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-device-source-20260710-024918
```

Results:

- Completion state: 2 PASS, 1 WARN, 5 BLOCKED, 0 FAIL.
- Readiness: 39 PASS, 0 WARN, 8 BLOCKED, 0 FAIL.
- Goal audit: 51 PASS, 1 WARN, 8 BLOCKED, 0 FAIL.

## Route-state physical-device evidence refinement

Updated 2026-07-10 command:

```bash
scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-device-source-20260710-025609 --force
```

Current route result:

- READY routes: 0;
- BUILD_ONLY routes: 1;
- BLOCKED routes: 3.

Route details:

- `arm64_simulator`: BLOCKED, installed simulator libraries still contain
  preview-core markers.
- `x86_64_simulator`: BUILD_ONLY, the x86_64 app builds but the current arm64
  simulator runtime rejects it with `Failed to find matching arch`.
- `physical_device`: BLOCKED, the physical-device source audit passed for the
  local arm64 artifact, but no trusted iPhone/iPad is connected,
  `apps/ios/Libraries/ios` is missing, and project references remain WARN.
- `local_nix_build`: BLOCKED, Nix is not installed.

This route-state packet is now the preferred first read for Batch 0 because it
combines simulator, x86_64 simulator, physical-device, and local build routes
without flattening the physical-device source-audit PASS into a generic
blocker.
