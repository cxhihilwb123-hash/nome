# Nome iOS remaining QA execution plan

Date: 2026-07-09

## Purpose

This plan turns the remaining unchecked Nome iOS manual QA items into an execution order.

The source queue is generated from:

```bash
scripts/ios/check-nome-manual-qa-status.sh --write-tsv /tmp/nome-manual-qa-unchecked.tsv
scripts/ios/export-nome-qa-batches.sh --output /tmp/nome-ios-qa-batches-current --force
scripts/ios/export-nome-qa-execution-state.sh --output /tmp/nome-ios-qa-execution-state-current --force --source-target physical-device --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710
scripts/ios/export-app-store-final-screenshot-state.sh --output /tmp/nome-ios-app-store-final-screenshot-state-current --force
scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-current --force
scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-current --force --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710
scripts/ios/export-ios-release-identity-state.sh --output /tmp/nome-ios-release-identity-state-current --force
scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-completion-state-current --force
```

The current queue has 41 unchecked items. They are real-core, multi-account, physical-device, network, release-identifier, and final-screenshot checks. They are not mockup-design tasks.

The batch export writes one TSV and one evidence-template directory per batch,
plus `evidence_index.tsv`, so the remaining tests can be executed and recorded
without re-reading the whole checklist. The index keeps every unchecked item
tied to its batch, dependency, command summary, expected evidence summary,
evidence-path placeholders, and blocker notes.

The batch export templates are now source-target aware: Batch 0 records both
the default simulator source audit and
`SOURCE_TARGET=physical-device scripts/ios/check-real-core-sources.sh`, and it
separates simulator `run-real-core-batch0.sh --prepare` from the guarded
physical-device `prepare-device-real-core.sh --prepare` path. Batch 5 repeats
the physical-device source audit, device-readiness export, device dry-run, and
connected-device/camera evidence requirements.

The execution-state export writes a higher-level packet with `summary.tsv`,
`gate_status.tsv`, `blocker_groups.tsv`, `next_actions.tsv`, the current
unchecked queue, and batch evidence templates. Its summary points directly to
`batches/evidence_index.tsv`. Use it when resuming work to see the next real
blocker without re-reading every plan file.

The App Store screenshot-state export writes `summary.tsv`,
`gate_status.tsv`, `screenshot_status.tsv`, `replacement_plan.tsv`, and
`next_actions.tsv`. Use it for Batch 6 so the four `needs-real-core` screenshot
replacements stay tied to manual QA anchors and the strict final screenshot
gate remains visibly blocked until real-core captures replace the drafts. It
also exports the current real-core route state and puts a Batch 0 prerequisite
ahead of screenshot replacement actions when there is no READY real-core route.

The real-core route-state export writes `summary.tsv`, `gate_status.tsv`,
`route_status.tsv`, and `next_actions.tsv`. Use it before Batch 0 work to
separate a route that is ready for functional QA from a route that merely
builds or remains blocked by the current machine/device state.

The device-readiness-state export writes `summary.tsv`, `gate_status.tsv`,
`device_status.tsv`, and `next_actions.tsv`. Use it before Batch 5 work to
separate the physical-device blocker into Xcode/signing environment, connected
device availability, local arm64 device artifact input, installed device
libraries, physical-device source-audit proof, generic device-build evidence,
and release-identifier review. When a generic build packet already exists,
pass it with `--generic-device-build-dir` so device readiness records whether
the current source produced `Nome.app` plus the notification and share
extensions before a real iPhone is connected.

The release-identity-state export writes `summary.tsv`, `current_identifiers.tsv`,
`required_decisions.tsv`, and `next_actions.tsv`. Use it before Batch 7 work so
bundle ids, App Groups, keychain groups, URL scheme, associated domains, and
background task decisions stay tied to manual QA anchors instead of becoming a
free-form release checklist.

The completion-state export writes `summary.tsv`, `completion_requirements.tsv`,
and `gate_status.tsv`. Use it as the top-level "can we claim the iOS goal is
done?" packet. When a generic iOS device build packet exists, pass it with
`--generic-device-build-dir` so the top-level physical-device requirement
reflects the current `Nome.app`/extension build evidence. It should remain
blocked until manual QA, real core, physical-device readiness, final
screenshots, and release identifiers all have passing evidence.

Completion is not claimed until:

- `scripts/ios/check-nome-manual-qa-status.sh` exits 0;
- `scripts/ios/check-real-core.sh` exits 0;
- `scripts/ios/check-ios-release-identifiers.sh` exits 0 before TestFlight or
  public distribution;
- `scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens` exits 0;
- `scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest <latest-manifest> --generic-device-build-dir <generic-device-build-dir> --output <completion-state-dir> --force`
  reports zero `BLOCKED` or `FAIL` requirements;
- `scripts/ios/check-nome-ios-goal-audit.sh` exits 0 without `--allow-blockers`.

## Batch 0: real-core artifacts

Blocker groups:

- `real_core_artifacts`

Goal:

- replace preview iOS core libraries with real production-sized iOS and simulator libraries;
- require the simulator libraries to match the active simulator build
  architecture; on this Apple Silicon machine that means `arm64`, not only
  production-sized `x86_64` archives;
- use the currently available local arm64 device artifact for the physical-device
  path only after explicitly installing it into `apps/ios/Libraries/ios`;
- keep the current UI changes intact while enabling real invitation, public-address, group, messaging, database, and server behavior.

Current physical-device fallback state:

- the local arm64 device artifact has been installed into
  `apps/ios/Libraries/ios`;
- `scripts/ios/export-ios-device-readiness-state.sh` now reports
  `installed_device_libraries` as PASS;
- local project-name aliases have been created in `apps/ios/Libraries/ios` so
  the current Xcode project references resolve to the installed arm64 device
  libraries without changing `project.pbxproj` or `apps/ios/Libraries/sim`;
- `scripts/ios/check-real-core-xcode-sync.sh` now passes for the local
  simulator/device build paths;
- a generic iOS device Debug build with `CODE_SIGNING_ALLOWED=NO` completed
  successfully and produced `Nome.app`, `SimpleX NSE.appex`, and
  `SimpleX SE.appex` under
  `/tmp/nome-ios-derived-generic-device-alias/Build/Products/Debug-iphoneos`;
- `scripts/ios/check-ios-generic-device-build.sh` now records this build as a
  reusable gate, with an optional `--skip-build` path for inspecting existing
  DerivedData products;
- `scripts/ios/check-nome-ios-readiness.sh --generic-device-build` and
  `scripts/ios/check-nome-ios-goal-audit.sh --generic-device-build` include the
  generic iOS device build gate, using per-report DerivedData directories so
  parallel audits do not lock the same Xcode build database;
- the remaining physical-device route blocker is a connected trusted
  iPhone/iPad.

Execution:

1. Find or build a complete `pkg-ios-aarch64-swift-json` and simulator package
   pair that is compatible with the target simulator/device architecture. The
   current `~/Downloads` pair is useful for device + x86_64 simulator, but it
   is not enough for the current arm64 simulator.
2. Run `scripts/ios/check-real-core-sources.sh` to check local files, public release/action artifacts, and known Hydra candidates. The currently known complete Hydra candidates are `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/master`, `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v7-0-0-beta-3`, and `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v6-5-5`; the closest `v6-5-6` jobset is partial because its standard aarch64 iOS endpoint returns 404.
   On the current machine this audit is architecture-aware and blocks those
   standard Hydra pairs for simulator QA because Xcode only exposes `arm64`
   iOS Simulator destinations, while the standard simulator artifact is
   `x86_64`.
3. Export the current route packet with
   `scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-current --force`.
   Its `next_actions.tsv` maps arm64 simulator, x86_64 simulator,
   physical-device, and local Nix routes to their current status, command,
   evidence file, manual QA checklist line, blocker group, and batch id.
   For the physical-device route, this packet should point onward to
   `scripts/ios/export-ios-device-readiness-state.sh` so installed device
   libraries, connected hardware, and project-reference compatibility remain
   separately visible.
4. If the installed physical-device artifact uses different archive filenames
   from the current simulator/project references, prepare local device-only
   aliases with `scripts/ios/alias-device-real-core-project-libs.sh --prepare`.
   This keeps `project.pbxproj` and the simulator libraries unchanged while
   letting the generic iOS device build resolve the project archive names.
5. Probe the local x86_64 simulator artifact with
   `scripts/ios/probe-x86_64-simulator-real-core.sh --output /tmp/nome-ios-x86_64-sim-real-core-probe-current --force` if the team wants to re-check the x86_64 route. Current evidence: the x86_64 app builds, the original preview libraries restore cleanly, but the iOS 26.5 arm64 simulator refuses to install the x86_64 app with `Failed to find matching arch`.
6. If a compatible Hydra pair is selected, stage it with `scripts/ios/run-real-core-batch0.sh --job-repo <hydra-job-repo> --output /tmp/nome-ios-real-core-batch0 --force`.
7. Stage local artifacts or Nix results with `scripts/ios/run-real-core-batch0.sh --source <artifact-dir> --output /tmp/nome-ios-real-core-batch0 --force`.
8. If artifacts must be built locally and Nix is available, run `scripts/ios/run-real-core-batch0.sh --build-with-nix --output /tmp/nome-ios-real-core-batch0 --force`.
9. For the physical-device fallback only, audit the local arm64 artifact with `scripts/ios/prepare-device-real-core.sh`. This dry-run confirms the device archive is production-sized, arm64, and free of preview markers without touching `apps/ios/Libraries/sim`.
10. If a physical iPhone is available and the team accepts the artifact-version mismatch risk for a compatibility test, install the device artifact with `scripts/ios/prepare-device-real-core.sh --prepare`; use `--force` only when intentionally replacing an existing `apps/ios/Libraries/ios` directory.
11. Install a complete staged simulator+device artifact pair with `scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-real-core-batch0 --prepare`. This now audits staged artifacts before replacing `apps/ios/Libraries`, so the known `v6-5-5` x86_64 simulator pair fails safely on the current Apple Silicon simulator path.
12. Check project/library-name consistency with `scripts/ios/check-real-core-xcode-sync.sh`. A candidate device-source mismatch can remain a warning while uninstalled; installed device libraries must either match project references directly or expose local device-only project-name aliases before a device build.
13. Sync the explicit Xcode archive references with `scripts/ios/sync-real-core-xcode-project.sh` after a complete pair is installed, or make a deliberate project-reference decision before physical-device-only builds.
14. Verify with `scripts/ios/check-real-core.sh` for full simulator+device readiness, or `scripts/ios/check-ios-device-readiness.sh` for the physical-device fallback.
15. Rebuild and launch the app with full Xcode.

Exit evidence:

- `apps/ios/Libraries/ios` exists;
- `apps/ios/Libraries/sim` no longer contains tiny preview placeholders;
- `apps/ios/Libraries/sim` supports the active simulator architecture;
- `scripts/ios/check-real-core-xcode-sync.sh` passes for installed simulator
  and device libraries;
- `scripts/ios/check-real-core.sh` passes.
- for the physical-device fallback, `scripts/ios/prepare-device-real-core.sh`
  passes before install and `scripts/ios/check-ios-device-readiness.sh`
  explains any remaining blockers such as no connected iPhone or release
  identifier review.

## Batch 1: first-run real app setup

Blocker groups:

- `first_run_real_core`
- `network_server_real_core`

Goal:

- prove a clean real-core app can create a local profile, accept network conditions, and reach the Nome home screen under selected server settings.

Execution:

1. Erase or use a fresh simulator/device profile.
2. Launch the real-core build without debug preview arguments.
3. Complete profile creation.
4. Complete network/server-use confirmation.
5. Check SMP/XFTP reachability under the selected server, Tor, and private-routing settings.

Exit evidence:

- first profile exists;
- Nome home renders after onboarding;
- server/network settings load real data rather than preview-core readiness messages.

## Batch 2: single-account connection surfaces

Blocker groups:

- `real_core_functional`
- `native_share_real_core`
- `public_contact_real_core`
- `error_state_real_core`

Goal:

- prove real one-time links, QR codes, native share sheets, public-contact address actions, and understandable error states.

Execution:

1. Generate a real one-time invite link.
2. Generate and display a scannable QR code.
3. Open native share from the invite surface.
4. Create or load the reusable public contact address.
5. Copy and share the actual public address.
6. Toggle request confirmation and exercise change, disable, and settings actions.
7. Try failed, expired, already-used, bad, non-group, and rejected-link cases.

Exit evidence:

- invite and public-contact actions use real values;
- share sheet payloads contain the expected actual addresses;
- invalid states show user-readable errors and do not crash.

## Batch 3: two-account messaging and groups

Blocker groups:

- `multi_account_real_core`
- `group_real_core`
- `messaging_feature_real_core`
- `conversation_safety_real_core`

Goal:

- prove the redesigned Nome UX still supports real two-account connection, group join, messaging, media, delivery receipts, reactions, deletion, replies, edits, and safety-state display.

Execution:

1. Prepare two independent accounts/devices.
2. Account B opens Account A's one-time link.
3. Account A receives and accepts the request.
4. Send and receive one-to-one text.
5. Create or use a real group invite, preview it, join it, and test request-approval state.
6. Send and receive group text.
7. Send and receive files.
8. Record, send, play, and inspect voice messages.
9. Verify delivery receipts, edit, reply, delete, and reaction actions remain reachable.
10. Verify the safety banner reflects verified and unverified states.
11. Open disappearing-message settings and persist a selected setting.

Exit evidence:

- both accounts see expected message state transitions;
- group and one-to-one paths work against real core;
- safety banner values match real verification and timed-message state.

## Batch 4: identity, migration, and data behavior

Blocker groups:

- `identity_real_data`
- `migration_real_data`

Goal:

- prove profile switching, hidden profile passcode behavior, incognito/new-profile copy, and migration/device-transfer paths still match the upstream data model.

Execution:

1. Create multiple real profiles.
2. Switch profiles from the identity center.
3. Enter, unlock, and exit a hidden profile using existing passcode behavior.
4. Confirm incognito/new-profile wording does not imply anonymity stronger than the SimpleX model.
5. Run migration/device-transfer flows with a real source profile.

Exit evidence:

- profile state changes persist;
- hidden profile locking is not weakened;
- migration completes or fails with exact existing constraints.

## Batch 5: physical device and camera

Blocker groups:

- `physical_device_or_camera`

Goal:

- prove flows that cannot be fully trusted from simulator-only evidence: physical-device launch and live QR scanning after camera permission.

Execution:

1. Run `scripts/ios/check-ios-device-readiness.sh`.
2. Export the structured device state with
   `scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-current --force`.
   Its `next_actions.tsv` maps the current no-device blocker, project-reference
   warning, and release-identifier warning rows back to the manual QA checklist
   and execution batches.
3. Connect and trust a physical iPhone if the gate reports no connected device.
4. Audit the local arm64 device package with
   `scripts/ios/prepare-device-real-core.sh` if `apps/ios/Libraries/ios` is
   missing.
5. Install or build the arm64 device real-core package with
   `scripts/ios/prepare-device-real-core.sh --prepare` only when ready for a
   device test; use `--force` only when intentionally replacing an existing
   device-library directory.
6. Re-run `scripts/ios/check-ios-device-readiness.sh` until it passes or only
   explicitly accepted release-identifier blockers remain.
7. Before connecting hardware, verify the generic iOS device build with
   `scripts/ios/check-ios-generic-device-build.sh`. This proves the iPhoneOS
   target can compile/link against the installed device libraries with signing
   disabled.
8. Build/install/launch the current real-core build on the physical iPhone with
   `scripts/ios/run-ios-physical-device-smoke.sh --output /tmp/nome-ios-physical-device-smoke-current --force`.
   The script writes `summary.tsv`, `steps.tsv`, `commands.tsv`, and logs for
   device discovery, signed device build, app install, and bundle launch. If no
   trusted iPhone/iPad is connected it exits before build/install/launch and
   records `connected_device` as BLOCKED.
9. Launch and complete basic navigation.
10. Accept camera permission.
11. Scan a live one-time invite QR and a group invite QR.

Exit evidence:

- `scripts/ios/check-ios-device-readiness.sh` passes;
- `scripts/ios/export-ios-device-readiness-state.sh` shows no blocked device
  areas except explicitly accepted release-distribution decisions;
- `scripts/ios/check-ios-generic-device-build.sh` passes and records
  `Nome.app`, `SimpleX NSE.appex`, and `SimpleX SE.appex` products;
- `scripts/ios/run-ios-physical-device-smoke.sh` passes on a connected trusted
  physical device, proving signed build, install, and launch;
- app launches on device;
- live camera scanner works after permission;
- denied/accepted permission paths remain understandable.

## Batch 6: final App Store screenshot package

Blocker groups:

- `app_store_final_screenshots`

Goal:

- replace preview/debug screenshot candidates with final real-core evidence and prepare a ten-or-fewer upload package.

Execution:

1. Replace the add-friend, public-contact, join-group, and conversation `needs-real-core` draft screenshots with real-core screenshots.
2. Keep the package at ten or fewer screenshots.
3. Update `MANIFEST.md` with final hashes, dimensions, and evidence notes.
4. Clear or rewrite `FINAL_BLOCKERS.md` so it no longer records preview/debug/needs-real-core blockers.
5. While blockers remain, export the current replacement state with
   `scripts/ios/export-app-store-final-screenshot-state.sh --output /tmp/nome-ios-app-store-final-screenshot-state-current --force`.
   The exported `replacement_plan.tsv` includes the draft file, source
   candidate, required evidence, manual QA anchor, exact checklist line,
   checklist section, blocker group, batch id, and capture action for each
   `needs-real-core` row.
6. After capturing the four real-core replacements, put them in one directory
   with these stems:
   - `05-add-friend-real-core`;
   - `06-public-contact-real-core`;
   - `07-join-group-real-core`;
   - `08-conversation-real-core`.
7. Generate a strict-final package proposal without editing the current draft
   directory:
   `scripts/ios/prepare-app-store-final-screenshot-package.sh --replacement-dir /tmp/nome-ios-real-core-final-screens --output /tmp/nome-ios-app-store-final-package-current --force`.
8. Use `scripts/ios/export-nome-qa-execution-state.sh --output /tmp/nome-ios-qa-execution-state-current --force --source-target physical-device --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710`
   to confirm Batch 6 points at the final screenshot-state export, not only the
   strict final screenshot checker.
9. While blockers remain, verify the replacement plan with `scripts/ios/check-app-store-final-blocker-plan.sh`.
10. Verify the generated package with
   `scripts/ios/check-app-store-screenshots.sh --final --dir /tmp/nome-ios-app-store-final-package-current`,
   or verify the in-repo draft directly with
   `scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens`
   if the draft directory itself is deliberately updated.

Exit evidence:

- strict final screenshot check passes;
- screenshot blocker plan passes while the real-core replacement rows remain;
- screenshots are real app screens and no longer rely on generated mockups or debug-only preview states.

## Batch 7: release identifiers and capabilities

Blocker groups:

- `release_identifiers`

Goal:

- make the Nome iOS package safe for TestFlight/public distribution without
  accidentally shipping upstream bundle ids, App Groups, keychain groups,
  associated domains, extension display names, or background-task identifiers.
- extension display names are already Nome-facing; this batch keeps them
  verified while resolving the remaining release identifiers.

Execution:

1. Run `scripts/ios/check-ios-release-identifiers.sh`.
   For the current compatibility-first development pass, also run
   `scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed` to
   prove every retained upstream identifier has a documented exception.
   Export the current decision packet with
   `scripts/ios/export-ios-release-identity-state.sh --output /tmp/nome-ios-release-identity-state-current --force`;
   its `required_decisions.tsv` maps each release identifier decision to the
   manual QA checklist line, section, and blocker group, and its
   `next_actions.tsv` maps each release next action to the same checklist
   location and evidence file. Its `proposed_identifiers.tsv` records the
   placeholder or candidate Nome-owned replacement values, update targets,
   migration dependencies, and verification commands without editing the Xcode
   project.
2. Use `scripts/ios/export-nome-qa-execution-state.sh --output /tmp/nome-ios-qa-execution-state-current --force --source-target physical-device --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710`
   to confirm Batch 7 points at the release-identity-state export, not only the
   strict release identifier checker.
3. Choose final Nome-owned bundle identifiers for the app, tests, notification
   service extension, share extension, and internal framework.
4. Choose final Nome-owned App Group and keychain access group names, with an
   explicit data-migration decision.
5. Decide whether the `simplex` URL scheme remains required for protocol/link
   compatibility.
6. Replace, remove, or explicitly preserve associated domains.
7. Confirm extension display names remain Nome-facing and update background task
   identifiers if the bundle-id strategy changes.
8. Re-run `scripts/ios/check-ios-release-identifiers.sh`.

Exit evidence:

- release identifier gate passes;
- compatibility-reviewed identifier gate passes for the current development
  pass while strict final mode still blocks public/TestFlight distribution;
- App Store/TestFlight identifiers are owned by the Nome release team or are
  explicitly documented compatibility exceptions.

## Batch order

The order is intentional:

1. Batch 0 must come first because preview core cannot prove real messaging behavior.
2. Batch 1 proves the real app can start cleanly before connection tests.
3. Batch 2 exercises single-account creation, sharing, and error surfaces.
4. Batch 3 uses the second account and groups after the basic surfaces are proven.
5. Batch 4 checks local identity and data behavior after messaging is stable.
6. Batch 5 adds hardware-only evidence.
7. Batch 6 waits until final screenshots can come from proven real-core flows.
8. Batch 7 is last because identifier changes can affect signing, push,
   app-group storage, extensions, links, and migration.

## Recheck commands

After each batch, run:

```bash
scripts/ios/check-nome-manual-qa-status.sh --write-tsv /tmp/nome-manual-qa-unchecked.tsv
scripts/ios/export-nome-qa-batches.sh --output /tmp/nome-ios-qa-batches-current --force
scripts/ios/export-nome-qa-execution-state.sh --output /tmp/nome-ios-qa-execution-state-current --force --source-target physical-device --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710
scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-readiness-current
scripts/ios/check-nome-ios-goal-audit.sh --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --allow-blockers --output /tmp/nome-ios-goal-audit-current
```

For the final release-readiness attempt, remove `--allow-blockers`:

```bash
scripts/ios/check-nome-ios-readiness.sh --output /tmp/nome-ios-readiness-release-gate
scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --output /tmp/nome-ios-goal-audit-release-gate
```
