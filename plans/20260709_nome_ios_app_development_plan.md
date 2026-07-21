# Nome iOS app development plan

Date: 2026-07-09

## Current conclusion

We now have an explicit Apple app development plan for Nome.

Before this file, the plan existed across three places:

- the Chinese-friendly fork launch boundary in `plans/20260707_01.md`;
- the approved Nome brand and product UX record in `design/NOME_DESIGN_RECORD.md`;
- the actual iOS UI and branding implementation work already applied in the app source.

This document turns that scattered context into one execution plan for the native iOS app.

## Product goal

Build a native iOS version of Nome based on the existing SimpleX iOS app, while preserving the upstream protocol and security model.

The app should feel understandable to normal users:

- use `Nome` as the visible product brand;
- explain direct connections as one-time links or one-time QR codes;
- explain reusable discovery as a public contact address;
- explain profiles and incognito mode as identity choices;
- explain backup, migration, servers, and Tor as practical safety settings;
- avoid teaching protocol internals before the user can complete everyday tasks.

## Safety boundary

This plan keeps the risky parts stable unless a later technical review explicitly changes them.

Allowed in the current phase:

- iOS SwiftUI screen layout and interaction polish;
- app icon, logo, splash, onboarding, and visible brand copy;
- Chinese and English user-facing text on the main entry paths;
- settings organization and plain-language safety explanations;
- simulator preview support and visual QA artifacts.

Not changed in the current phase:

- SimpleX protocol behavior;
- Haskell core, queue logic, encryption, key negotiation, and database semantics;
- `simplex:` link compatibility;
- server routing, push notification trust model, or Tor behavior;
- release signing, bundle identifier, app group, and notification extension identifiers unless reviewed as a release task.

Practical rule: the first iOS workstream is a UX and brand layer over the existing app, not a new messenger protocol.

## Current implementation status

Done in the current working tree:

- Nome logo and app icon assets are applied to the iOS asset catalog.
- The iOS display/product name has been changed to Nome while preserving compatibility-sensitive internals.
- Main inbox/home has been redesigned around everyday tasks.
- Add/join entry sheet has been redesigned.
- One-time friend link, scan, and paste connection paths have been simplified.
- Public contact address page has been reframed into the Nome card layout: explanation, reusable address, copy/share, confirmation toggle, address management, and one-time-link entry.
- Identity center and hidden profile pages have been reframed.
- Settings has been simplified around safety, backup, desktop connection, privacy, servers, and Tor, and now opens as an in-place Nome bottom-tab page instead of a settings sheet.
- The embedded Nome settings tab now exposes a real `数据与存储` entry for database password, export/import, and local files.
- The embedded Nome settings tab now includes `完整设置` under `高级`, so the simplified Nome page does not hide call, appearance, developer, version, or other upstream controls.
- Chat view has a Nome safety status banner compiled into the app: it summarizes encryption, security-code verification state, and timed-message/delete status without changing message transport.
- Chat view toolbar now shows a plain-language Nome safety subtitle such as `安全会话`.
- Chat view now replaces the inherited upstream `school` default wallpaper with a Nome-specific clean safety background at render time, while preserving custom wallpapers and other presets.
- A Debug-only seeded conversation preview can be launched with `-NomeConversationPreview` to visually test the conversation screen without requiring a live chat core.
- A manual iOS QA checklist is saved at `plans/20260709_nome_ios_manual_qa_checklist.md`.
- A TestFlight release-gate identifier review is saved at `plans/20260709_nome_ios_release_gate_review.md`.
- A first low-risk brand-copy audit changed user-facing `SimpleX Lock`, share-extension, contact-address sharing, and chat-address labels to Nome-neutral wording, while preserving protocol/server/attribution uses of SimpleX.
- A follow-up brand-copy audit changed remaining safe iOS UI strings for Nome Lock, notification hidden-preview placeholders, and deprecated public-contact-address onboarding/share copy, while continuing to preserve protocol/server/attribution uses of SimpleX.
- A brand-copy regression gate now checks Nome-facing Swift and Simplified Chinese strings for old product wording such as `SimpleX Lock`, `Share to SimpleX`, old invite copy, and `SimpleX address` on branded surfaces.

Verified recently:

- iOS simulator build passed with full Xcode by setting `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`.
- The app installed and launched in the simulator.
- Browser-based simulator previews were captured for home, add sheet, settings, public address, identity center, and connect flow.
- The latest build also verifies the empty Nome home layout in the iPhone 17 simulator: the home sections now render in the intended order, and the old SimpleX one-hand bottom toolbar is hidden when the Nome bottom navigation is shown.
- Add friend / join group pages can now be opened for UI preview, while real invitation generation and link connection detect the current preview chat core before calling the core APIs.
- The join-group path now has a dedicated page with invitation-link input, scanner card, source warning, preview placeholder, and an explicitly disabled public-group directory row.
- The public contact address page now opens from the Nome home card, renders in the intended card layout, and blocks copy/share/change/disable/settings actions with a clear preview-core message when the simulator is linked to preview libraries.
- The settings bottom tab now switches in place, shows the Nome logo, main identity card, grouped settings rows, privacy/server rows, and the active settings tab state.
- The settings tab verifies `数据与存储`, `隐私与安全`, and `服务器与 Tor` open their existing native settings screens in the simulator.
- The `服务器与 Tor` page no longer crashes from a missing `SaveableSettings` environment object when opened from the embedded Nome settings tab.
- The `服务器与 Tor` page now shows a clear preview-core readiness message instead of `unexpected result: cmdOk` when real server data cannot be loaded in the current preview-core simulator.
- The settings tab now verifies `完整设置` opens the full native settings list, preserving access to controls outside the simplified Nome settings page.
- The settings tab verifies `通知` opens the existing native notification settings screen, and returning from it restores the embedded settings tab before bottom `首页` returns to the Nome home screen.
- `备份与迁移` now opens a Nome safety hub before any migration action starts, preserving real export/import and device-migration controls while avoiding accidental chat-stop behavior.
- The backup/migration hub verifies that `导出或导入数据库` opens the existing data/storage export/import screen, and the back path returns cleanly to the hub and settings tab.
- The latest iOS brand-copy build verifies the safe user-facing `SimpleX Lock`, notification placeholder, and old address-onboarding strings no longer remain in the Swift UI source.
- A follow-up brand-copy pass changed `WhatsNewView` short-address copy and legacy Simplified Chinese lock/share/invite/address translations to Nome / public-contact-address wording.
- The remaining iOS `SimpleX` references are classified in `plans/20260709_nome_ios_brand_copy_audit.md` as Nome-facing copy, compatibility labels, upstream attribution, protocol/server wording, or legacy/non-primary paths.
- Simplified Chinese translations were added for current Nome-facing keys such as Nome Lock, notification placeholders, Nome contact sharing, invitation email copy, and contact-address labels.
- The settings `关于 Nome` entry now opens a dedicated Nome about page instead of the onboarding-style `SimpleXInfo` screen. It shows version, product positioning, privacy boundaries, SimpleX network/protocol compatibility, and upstream attribution.
- The settings `帮助与反馈` entry now opens a dedicated Chinese Nome help page instead of the upstream English `ChatHelp` screen. It covers add friend, join group, public contact address, identity, privacy/security, backup/migration, and feedback channels.
- The bottom `联系人` tab now opens a real in-place contacts page instead of reusing the add/new-chat sheet.
- A Debug-only existing-chat-list preview can be launched with `-NomeChatListPreview` to verify the redesigned Nome home with real chat-list rows.
- When existing chats are present, the Nome home now uses a compact quick-action strip instead of the large empty-state task cards, so recent conversations remain visible in the first screen.
- The Nome bottom-tab home now disables the inherited one-hand chat-list inversion while the Nome tab bar is active, fixing the previous non-empty list order where chat rows appeared above the home header.
- The first-run onboarding path has been reframed into Nome pages for welcome, local identity creation, network/notification setup, and network-use confirmation.
- The first-run migration entry now opens the existing migrate-to-device flow with an explicit `关闭` action, so users can safely return to local identity creation.
- The first real clean-launch pass now suppresses the inherited upstream SimpleX "What's New" sheet before Nome surfaces appear.
- The first real clean-launch pass now shows a Nome-branded delivery-receipts decision page instead of the inherited upstream prompt.
- The system notification permission request is delayed until the delivery-receipts decision page is not covering the first experience.
- `scripts/ios/check-real-core.sh` now provides a repeatable read-only gate for whether the iOS build is still using preview core libraries or has real iOS core artifacts installed.
- `plans/20260709_nome_ios_real_core_testing_plan.md` records the next real-core acquisition and functional QA sequence for invitation, public address, group, messaging, backup, desktop linking, and final screenshot replacement.
- `scripts/ios/run-ios-physical-device-smoke.sh` now provides the physical-device
  execution entry point for Batch 5: detect a trusted iPhone/iPad, build the
  signed device app, install it with `devicectl`, launch the bundle, and record
  machine-readable evidence. On this machine it currently blocks before build
  because no physical iPhone/iPad is connected.
- Debug-only onboarding preview launch arguments are available for visual QA without erasing simulator data: `-NomeOnboardingWelcomePreview`, `-NomeOnboardingProfilePreview`, `-NomeOnboardingNetworkPreview`, and `-NomeOnboardingConditionsPreview`.
- The identity center can be opened from the home profile menu and renders the Nome identity-management card layout in the simulator.
- The chat-detail safety status banner now builds in the native app and is wired to the existing private-chat/group-chat detail sheet for real chat states.
- The seeded conversation preview renders the conversation page with Chinese text messages, a file item, a voice item, the Nome safety card, and the disappearing-message prompt.
- The seeded conversation preview now renders against the Nome default conversation background instead of the upstream school wallpaper.
- The seeded conversation preview now has a preview-safe safety/settings sheet for the safety card and `阅后即焚 · 设置` prompt, avoiding preview-core calls to the real chat-detail API.
- The preview safety/settings sheet explicitly says automatic deletion only controls Nome message retention and does not undo content already saved, screenshotted, or exported by the other side.
- A normal launch without `-NomeConversationPreview` still opens the regular Nome home screen.
- The latest build after the brand-copy audit passes and still opens the regular Nome home screen.
- The bottom `联系人` tab was verified in the simulator: it highlights the active contacts tab, shows contact/group/request counts, quick actions, and the empty contacts state without opening a duplicate sheet.
- The Debug existing-chat-list preview was verified in the simulator: the home header remains at the top, compact add/join/public-address actions fit in one row, and existing chat rows retain unread count, group mention/report flag, and muted-notification indicators.
- The redesigned onboarding welcome, local identity, network/notification, and network-use confirmation pages were verified in the simulator through Debug-only preview launch arguments.
- The onboarding `Nome 怎样保护你` explanation sheet and message-server sheet open without crashing; the message-server sheet now handles an empty preview-core operator list with a clear default-settings state and an enabled `完成` button.
- The onboarding profile migration entry was verified in the simulator: tapping `迁移` opens the existing `迁移到这台设备` flow, camera denial leaves the migration UI stable, and tapping `关闭` returns to the local identity page.
- A real fresh-simulator first launch was tested after the onboarding work. It no longer shows the inherited upstream `v6.5 的新内容` sheet before Nome UI.
- A real fresh-simulator first launch now shows the Nome `是否发送送达回执` decision page without a system notification permission alert covering it.
- The `暂不启用` delivery-receipts path was tested in a fresh simulator: it shows the Nome confirmation alert, then the iOS system notification permission prompt, then returns safely to the Nome home screen when notifications are denied.
- The `启用送达回执` delivery-receipts path was tested in a separate fresh simulator: it advances to the iOS system notification permission prompt, then returns safely to the Nome home screen when notifications are denied.
- The first-launch smoke after the notification-delay fix produced no new `Nome-*.ips` crash report.
- The TestFlight identifier review records the current compatibility-first decision to keep `chat.simplex.app`, `simplex`, `group.chat.simplex.app`, the existing keychain access group, and the current notification/share extension identifiers for this phase.
- Notification service and share extension display names are now Nome-facing (`Nome Notifications` and `Nome Share`) in Xcode build settings and localized extension InfoPlist strings, while target names, executable names, bundle identifiers, App Groups, and keychain access groups remain unchanged pending the release-identity decision.
- App Store screenshot candidates are saved under `design/app-store/ios-real-screens/`, copied from real iOS Simulator screenshots rather than generated design mockups.
- App Store screenshot candidate packaging is now tracked by `design/app-store/ios-real-screens/MANIFEST.md` and `scripts/ios/check-app-store-screenshots.sh`: the current set has eleven `1206x2622` PNG candidates, App Store final packaging must select ten or fewer, and preview/debug candidates are explicitly marked for real-core replacement.
- `scripts/ios/export-app-store-screenshot-draft.sh` now generates a repeatable 10-slot flattened JPEG draft package under `design/app-store/ios-upload-draft-screens/`. The draft removes alpha and the eleventh candidate while keeping `needs-real-core` filenames for screenshots that still require real invitation, public-address, group, or messaging evidence. It also writes `FINAL_BLOCKERS.md`, which lists the exact four screenshots and replacement evidence required before App Store final mode can pass.
- `scripts/ios/prepare-app-store-final-screenshot-package.sh` now generates a
  separate strict-final package proposal after the four real-core replacement
  screenshots are captured. It does not edit the current draft directory; it
  produces a 10-slot package, clears final blockers in the generated package,
  and runs `scripts/ios/check-app-store-screenshots.sh --final` against the
  output.
- The preview-core `添加朋友` / one-time-link page now renders a full Nome card layout with link placeholder, disabled share state, QR placeholder, retry action, and an explicit readiness warning instead of a blank retry surface.
- The add/new-chat sheet now has an explicit top-level `关闭` action. The verified add-friend return path is: one-time-link page back to add list, then `关闭` back to Nome home.
- The verified join-group return path is: group-specific join page back to the add list, then `关闭` back to Nome home.
- `UserPickerSheetView` now gives public-contact/address and other user-picker sheet flows an explicit `关闭` action. The public-contact close path was verified back to Nome home.
- The contacts tab quick actions now have simulator evidence: `添加朋友` returns through the add list back to the contacts tab, and `公开联系方式` closes back to the contacts tab.
- The profile menu now has simulator evidence for `身份中心` opening the redesigned local-identity sheet and closing back to Nome home.
- `连接桌面` / `从桌面端使用` now detects preview-core state and renders a Nome-readable readiness card instead of surfacing the technical `%@` alert.
- Privacy claims and source-availability preparation are recorded in `plans/20260709_nome_ios_privacy_claims_audit.md`, including bounded wording for disappearing messages, server privacy, Tor, local data, upstream attribution, and AGPL/source release requirements.
- Network-use onboarding copy now says Nome reduces connection-relationship exposure instead of promising absolute relationship protection.
- `plans/20260709_nome_ios_design_coverage_matrix.md` now maps every approved `pages-v2` mockup to the native iOS implementation surface, simulator evidence, current status, and remaining real-core proof.
- `scripts/ios/check-nome-design-coverage.sh` now verifies the seven approved `pages-v2` artifacts, coverage IDs, smoke-script evidence labels including the contacts tab, and the expected Swift source markers for Nome page components, preview hosts, launch arguments, and preview-core readiness guards.
- Debug-only primary-flow preview launch arguments are available for visual QA of the main effect-picture pages: `-NomeAddFriendPreview`, `-NomeJoinGroupPreview`, `-NomePublicContactPreview`, and `-NomeSettingsPreview`.
- Debug-only contacts-tab preview is available through `-NomeContactsPreview`, so the shipped bottom `联系人` page is covered by repeatable screenshot smoke rather than only manual simulator evidence.
- `scripts/ios/smoke-nome-ui.sh` now provides repeatable build/install/launch/screenshot smoke coverage for the previewable Nome iOS UI states. The latest contacts-tab run captured fourteen `1206x2622` screenshots under `/tmp/nome-ios-smoke-contacts-tab/`, including add friend, join group, public contact, settings, contacts, identity center, conversation, onboarding, and home, and produced no new `Nome-*.ips` crash report.
- `scripts/ios/check-nome-smoke-visual-quality.sh` now validates the smoke
  manifest against the screenshot PNGs on disk, including image decodeability,
  dimensions, byte sizes, SHA-256 values, portrait orientation, and duplicate
  hashes. It is included in the combined readiness and goal-audit gates.
- `scripts/ios/export-nome-design-evidence-state.sh` now emits
  `page_evidence.tsv` in addition to `coverage.tsv`. The page evidence records
  the seven approved effect-picture pages, their signed smoke screenshot
  labels/paths, page status, visual acceptance, final functional acceptance,
  blocker batch, App Store dependency, remaining proof, and next evidence.
- The UI smoke script now auto-boots the selected simulator if it is shut down and retries transient `simctl` install/launch/screenshot failures. This was verified by shutting down simulator `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`, then running the smoke pass into `/tmp/nome-ios-smoke-retry-boot/`.
- A refreshed 14-page UI smoke pass on 2026-07-09 installed the existing
  `/tmp/nome-ios-derived-clean` app and captured home, onboarding, existing
  chat list, conversation, identity, add friend, join group, public contact,
  settings, and contacts under
  `/tmp/nome-ios-smoke-refresh-20260709-225133/`, with no new `Nome-*.ips`
  crash report.
- A full-build 14-page UI smoke pass on 2026-07-09 rebuilt the app with full
  Xcode into `/tmp/nome-ios-derived-full-smoke-20260709-225943`, installed
  `Nome.app`, and captured home, onboarding, existing chat list, conversation,
  identity, add friend, join group, public contact, settings, and contacts
  under `/tmp/nome-ios-smoke-full-build-20260709-225943/`, with no new
  `Nome-*.ips` crash report.
- `scripts/ios/check-nome-brand-copy.sh` now verifies the current safe Nome-facing brand-copy boundary and is included in the combined readiness gate.
- `scripts/ios/test-real-core-source-audit.sh` now regression-tests the local source-audit logic with missing, one-architecture, and complete two-architecture fixture directories.
- `scripts/ios/check-real-core-sources.sh` now defaults to 12 recent releases, three GitHub Actions artifact pages, and known SimpleX Hydra candidates under `ci.zw3rk.com/job/simplex-chat-simplex-chat`, while still allowing `RELEASE_LIMIT`, `ACTIONS_ARTIFACT_PAGES`, `INCLUDE_KNOWN_HYDRA_REPOS`, `KNOWN_HYDRA_JOB_REPOS`, and newline-separated `HYDRA_JOB_REPOS` overrides for deeper or offline audits.
- `scripts/ios/check-real-core-build-env.sh` now audits the local Nix build path for real iOS core libraries. It checks the flake iOS targets, `pkg-ios-*-swift-json` output markers, Nix availability, local `mac2ios`, full Xcode, `simctl` with `DEVELOPER_DIR`, `xcode-select`, and free disk space without installing anything or running a build.
- `scripts/ios/stage-real-core-artifacts.sh` now bridges Nix result symlinks or downloaded artifact locations into the exact two-architecture `pkg-ios-*-swift-json` layout expected by `scripts/ios/prepare-real-core.sh --downloads-dir`, and `scripts/ios/test-stage-real-core-artifacts.sh` covers complete, missing, and ambiguous fixture cases.
- `scripts/ios/run-real-core-batch0.sh` now provides the Batch 0 execution entry point: stage an existing artifact pair, download and stage a supplied Hydra job repository, optionally build both iOS core architectures with Nix, and only replace `apps/ios/Libraries` when explicitly run with `--prepare`.
- `scripts/ios/sync-real-core-xcode-project.sh` now covers the Xcode project side of real-core replacement: after libraries are installed, it updates the two explicit `libHSsimplex-chat...` archive references in `project.pbxproj` to match the actual library filenames.
- `scripts/ios/test-prepare-real-core-safety.sh` now regression-tests `prepare-real-core.sh` in a temporary sandbox, verifying missing, partial, and empty-architecture artifacts fail before any library replacement, and verifying the Hydra `--job-repo` path checks source availability before download and runs installed-core preflight only after download.
- `scripts/ios/prepare-device-real-core.sh` now provides a guarded physical-device fallback path: by default it only audits the local arm64 device artifact, and it installs into `apps/ios/Libraries/ios` only when explicitly run with `--prepare`.
- `scripts/ios/test-prepare-device-real-core.sh` regression-tests the device-only path for missing source, preview markers, wrong architecture, dry-run behavior, non-empty target protection, and forced sandbox copy.
- `scripts/ios/check-real-core-xcode-sync.sh` now checks the explicit `libHSsimplex-chat` archive references in `project.pbxproj` against installed simulator libraries, installed device libraries, and the candidate local device artifact. Installed simulator/device mismatches fail; candidate device-source mismatches warn until installed.
- `scripts/ios/test-check-real-core-xcode-sync.sh` regression-tests matching, simulator-mismatch, installed-device-mismatch, and candidate-source-warning cases.
- `scripts/ios/check-ios-release-identifiers.sh` now has two explicit modes: strict final mode, which still blocks upstream identifiers, and `--compatibility-reviewed`, which passes only when each retained upstream identifier is documented in `plans/20260709_nome_ios_release_gate_review.md`.
- `scripts/ios/test-check-ios-release-identifiers.sh` regression-tests strict blocking, documented compatibility exceptions, missing exception failures, and the rule that Nome-facing display names remain required even in compatibility-reviewed mode.
- `scripts/ios/check-nome-manual-qa-status.sh` now summarizes the manual QA checklist and exits non-zero while unchecked functional/release items remain, so manual testing cannot be accidentally treated as complete.
- `plans/20260709_nome_ios_remaining_qa_execution_plan.md` now maps the remaining unchecked manual QA blocker groups into seven execution batches: real-core artifacts, first-run/network, single-account connection surfaces, two-account messaging/groups, identity/migration, physical-device/camera, and final App Store screenshots.
- `scripts/ios/check-nome-qa-execution-plan.sh` now verifies that the remaining-QA execution plan covers every current unchecked `blocker_group` emitted by `scripts/ios/check-nome-manual-qa-status.sh --write-tsv`, and checks that the plan names the required manual-QA, real-core, final-screenshot, and goal-audit commands.
- `scripts/ios/export-nome-qa-batches.sh` now exports the current unchecked manual QA queue into batch-specific TSV files, Markdown evidence templates, and a machine-readable `evidence_index.tsv` that ties each remaining item to its batch, dependency, command summary, expected evidence summary, evidence-path placeholders, and blocker notes. A count check ensures the batch totals still match the source queue.
- `scripts/ios/check-nome-ios-readiness.sh` now aggregates the current iOS gates: script syntax, source-audit unit test, Xcode/core reference sync unit test, release-identifier unit test, prepare-real-core safety unit test, device-only prepare unit test, real-core artifact staging unit test, Batch 0 helper unit test, diff hygiene, Simplified Chinese localization lint, Nome brand-copy check, Nome design coverage check, remaining-QA execution-plan coverage, QA batch export, manual QA status, App Store screenshot candidate/final checks, Xcode/core reference sync, release-identifier compatibility review, real-core preflight, real-core local build-env, optional real-core source audit, and optional simulator UI smoke. The latest source-audit run produced 13 passes, 1 skipped-smoke warning, 4 known blockers, and 0 failures under `/tmp/nome-ios-readiness-sync-hydra/`; the `real_core_source_audit` gate now passes because known Hydra candidates are reachable.
- `scripts/ios/check-nome-ios-goal-audit.sh` now turns the overall "planned, redesigned, and tested" goal into a repeatable evidence audit. It includes the generated App Store final blocker ledger alongside the planning records, QA records, remaining-QA execution plan, QA batch export, design coverage, brand copy, Xcode/core reference sync, release-identifier compatibility review, device-only prepare test, real-core staging test, Batch 0 helper unit test, manual QA status, smoke evidence, screenshot checks, real-core preflight, real-core local build-env, optional source audit, and physical-device status. With source audit and the latest contacts-tab smoke manifest it reports 25 passes, 1 warning, 4 known blockers, and 0 failures under `/tmp/nome-ios-goal-audit-sync-hydra/`.
- The exported screenshot draft check passes in candidate mode for `design/app-store/ios-upload-draft-screens/`: 10 JPEG screenshots, all `1206x2622`, alpha-free, with non-final warnings for the manifest, `FINAL_BLOCKERS.md`, and four `needs-real-core` files. Strict final mode intentionally fails until those four screenshots are replaced and `MANIFEST.md` / `FINAL_BLOCKERS.md` no longer record preview/debug/needs-real-core status.
- `scripts/ios/check-app-store-final-blocker-plan.sh` now verifies the four final screenshot blockers are explicitly tied to manual QA evidence and Batch 6 replacement instructions, so the final screenshot work cannot drift away from real-core proof.
- `scripts/ios/test-app-store-final-blocker-plan.sh` regression-tests complete, missing-blocker, and missing-manual-QA-anchor cases for that screenshot replacement plan.
- `scripts/ios/export-ios-device-readiness-state.sh`,
  `scripts/ios/check-nome-ios-readiness.sh`, and
  `scripts/ios/check-nome-ios-goal-audit.sh` now accept an existing generic iOS
  device build packet with `--generic-device-build-dir`, so the physical-device
  route can record whether the current source has already produced `Nome.app`,
  `SimpleX NSE.appex`, and `SimpleX SE.appex` before a trusted iPhone/iPad is
  connected.
- `scripts/ios/export-nome-ios-completion-state.sh` now also accepts `--generic-device-build-dir`, passing that evidence through to the device-readiness packet so the top-level completion state can distinguish "device package already builds" from "trusted physical iPhone/iPad still missing."
- `scripts/ios/check-ios-release-identity-proposal.sh` now validates the proposed release identity table from `export-ios-release-identity-state.sh`: it rejects default example placeholders and upstream release identifiers, checks bundle/App Group/keychain/background-task derivation, and keeps the `simplex` URL scheme as an explicit protocol-compatibility warning rather than a silent final-release pass.
- The latest page-level design evidence export is
  `/tmp/nome-ios-design-evidence-state-page-evidence-20260710`: seven coverage
  rows, seven page-evidence rows, seven visual PASS rows, seven final functional
  BLOCKED rows, zero missing rows, and fourteen smoke labels. The integrated
  readiness report at `/tmp/nome-ios-readiness-page-evidence-20260710` reports
  45 PASS, 2 WARN, 8 BLOCKED, and 0 FAIL; the integrated goal audit at
  `/tmp/nome-ios-goal-audit-page-evidence-20260710` reports 57 PASS, 3 WARN, 8
  BLOCKED, and 0 FAIL.
- Re-running the integrated gates with existing generic device build evidence
  (`--generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710`)
  now records `generic_device_build` as PASS without recompiling: readiness at
  `/tmp/nome-ios-readiness-generic-build-dir-20260710` reports 46 PASS, 1 WARN,
  8 BLOCKED, and 0 FAIL; goal audit at
  `/tmp/nome-ios-goal-audit-generic-build-dir-20260710` reports 58 PASS, 2 WARN,
  8 BLOCKED, and 0 FAIL.
- The aggregate gates also accept `--source-target physical-device` for the
  optional real-core source audit. With that target and the generic build
  evidence above, readiness at
  `/tmp/nome-ios-readiness-source-target-device-generic-build-20260710` reports
  47 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL; goal audit at
  `/tmp/nome-ios-goal-audit-source-target-device-generic-build-20260710`
  reports 59 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL. The remaining goal-audit WARN
  is only `physical_device_test`, because no trusted physical iPhone/iPad is
  connected.
- `scripts/ios/check-real-core.sh` now supports `--target all`,
  `--target simulator`, and `--target physical-device`. The default remains the
  strict combined check. With `--source-target physical-device`, the aggregate
  gates now use the physical-device preflight for `real_core_preflight`, while
  `simulator_real_core_route` continues to track the arm64 simulator preview-core
  blocker separately. Current physical-device-target readiness at
  `/tmp/nome-ios-readiness-physical-preflight-20260710` reports 48 PASS, 0 WARN,
  7 BLOCKED, and 0 FAIL; goal audit at
  `/tmp/nome-ios-goal-audit-physical-preflight-20260710` reports 60 PASS, 1
  WARN, 7 BLOCKED, and 0 FAIL.
- `scripts/ios/check-real-core-build-env.sh` now supports
  `--allow-downloaded-artifacts` with a target. The default still fails when Nix
  is missing, but the physical-device route can pass when the required
  downloaded arm64 device artifact is present. Current physical-device artifact
  route readiness at `/tmp/nome-ios-readiness-physical-artifact-route-20260710`
  reports 49 PASS, 0 WARN, 6 BLOCKED, and 0 FAIL; goal audit at
  `/tmp/nome-ios-goal-audit-physical-artifact-route-20260710` reports 61 PASS,
  1 WARN, 6 BLOCKED, and 0 FAIL.
- `scripts/ios/apply-ios-release-identifiers.sh` now provides the release
  identifier migration execution path. It validates a checked
  `proposed_identifiers.tsv`, dry-runs by default, writes a `changes.tsv`, and
  only edits the Xcode project, app Info.plist, and entitlements when
  explicitly run with `--apply`. A real-project candidate dry-run using
  `app.nome.secure` and `nome.chat` is saved at
  `/tmp/nome-ios-release-identity-apply-dry-run-20260710`; it found 14
  migration points and zero missing current values without editing files. The
  latest readiness report at
  `/tmp/nome-ios-readiness-release-identity-apply-20260710` reports 50 PASS, 0
  WARN, 6 BLOCKED, and 0 FAIL; goal audit at
  `/tmp/nome-ios-goal-audit-release-identity-apply-20260710` reports 62 PASS, 1
  WARN, 6 BLOCKED, and 0 FAIL.

Known gap:

- The current simulator build links against preview iOS core libraries. The linked framework contains `preview-agent`, `simplex:/contact#preview`, `https://nome.local/preview`, and `preview-token`, so it can validate UI but cannot validate real invitation, group, messaging, database, or server behavior.
- Public GitHub release assets, paginated recent GitHub Actions artifacts, and the SimpleX downloads page do not currently expose reusable iOS core library zip artifacts for this checkout. The working Hydra project is now known: `ci.zw3rk.com/job/simplex-chat-simplex-chat`. `master`, `v7-0-0-beta-3`, and `v6-5-5` expose complete aarch64 + x86_64 iOS core pairs, while the same-version `v6-5-6` jobset currently exposes only the x86_64 iOS endpoint and returns 404 for the standard aarch64 iOS endpoint. Because this checkout is `6.5.6.1`, using a non-matching Hydra pair is a compatibility candidate that still needs build, launch, and smoke verification before replacing preview-core blockers.
- A complete `v6-5-5` candidate pair is staged at `/tmp/nome-ios-real-core-v6-5-5` for controlled experimentation. It has SHA-256 hashes `3b5e625088a6cbc57fbbec2f147e0618765bdcc9d56fc27f940c949f0e3f835b` for aarch64 and `6ed3cb1cdf92fef1bca9bfaf3c41c65c82a0e5d0ccc8b853b2f360fc1be67d63` for x86_64. It is not installed into `apps/ios/Libraries` yet.
- This machine currently has no `nix`, but local `pkg-ios-aarch64-swift-json`
  and `pkg-ios-x86_64-swift-json` artifact directories now exist under
  `~/Downloads`. They are a `v6.5.5.0` device + x86_64-simulator pair, so they
  help the physical-device path but do not satisfy the current arm64 simulator
  workflow. A local `tools/bin/mac2ios` helper and full Xcode are available,
  `xcode-select` points at full Xcode, and the project volume has about 47 GB
  free.
- The repeatable preflight command is `scripts/ios/check-real-core.sh`; it
  currently exits with `1` and reports missing `apps/ios/Libraries/ios`,
  preview markers in `apps/ios/Libraries/sim`, local artifact inputs in
  `~/Downloads`, and missing `nix`; it now detects local `tools/bin/mac2ios`.
- The repeatable source-audit command is `scripts/ios/check-real-core-sources.sh`;
  it now requires a simulator-compatible artifact pair before reporting a usable
  source for this machine. It currently finds local v6.5.5 device + x86_64
  simulator artifacts, no complete `pkg-ios-*-swift-json` pair across the
  recent GitHub release/prerelease window, no matching package in four pages of
  recent Actions artifacts, and complete known Hydra candidates that are still
  x86_64-simulator based and need a compatible simulator path or physical-device
  validation.
- The chat-detail safety banner is compiled and wired, and the conversation page now has seeded visual QA evidence. It still needs a two-device or real-core test before claiming real messaging behavior.
- The preview-safe conversation details sheet proves the debug preview can show the intended explanation without crashing, but it does not prove real chat-detail settings persistence.
- The existing-chat-list preview proves visual preservation of seeded chat-list row states, but real unread/archive/muted state transitions still need real-core testing.
- The redesigned onboarding screens have visual QA evidence, but real first-run profile creation, network-operator acceptance, and transition into the main app still need real-core or clean-state functional testing.
- The first-run migration entry has visual QA evidence, but complete device-to-device migration still needs a real migration source, camera permission acceptance, and real-core/data testing.
- The App Store screenshot candidate folder is not the final store submission package: the manifest currently records eleven candidates, so the public package must select ten or fewer, flatten final exports if needed, replace preview-core/debug-only add-friend/public-contact/join-group/conversation candidates with real-core screenshots, update manifest and `FINAL_BLOCKERS.md` evidence, and finish captions, crops, and store export sizing.
- The clean-launch smoke verifies first visible Nome surfaces, permission-prompt ordering, and both delivery-receipts click-through choices. Real multi-contact delivery-receipt behavior, profile creation, and network acceptance still need broader real-core testing.
- Desktop linking remains visual/readiness-only in the current simulator. Real mobile-desktop pairing still needs real iOS core libraries and a compatible desktop app session.
- AGPL/source availability is prepared as a release packet only; it is not complete until the public Nome modified-source URL and final asset/trademark decision are recorded.
- Privacy claims are checked against current implementation boundaries for this UX pass, but final App Store privacy/export-compliance answers still need real-core behavior evidence and legal/product review.

## Milestones

### Phase 0: baseline freeze

Target: now to 1 day.

Tasks:

- Save this plan as the iOS execution reference.
- Keep the current diff small and reviewable.
- Preserve the upstream compatibility boundary.
- Collect the latest preview screenshots as visual baseline.
- Run `git diff --check` before any commit.

Exit criteria:

- The plan is committed with the UI changes.
- Existing SimpleX-compatible links and internals are not intentionally broken.

### Phase 1: iOS UX MVP completion

Target: 2 to 4 days.

Tasks:

- Make the home cards route directly to the right flows instead of generic sheets where possible.
- Install or build real iOS core libraries before claiming invitation, group, or messaging behavior is functionally complete.
- Finish the join group flow as its own user path: paste link, scan QR, preview group, join or request approval.
- Visually verify the conversation page with real chat data.
- Polish profile switching, identity center, and hidden profile interactions.
- Keep every button mapped to a real existing action, or make it clearly disabled.
- Audit copy so one-time links, public contact address, incognito profile, and Tor are never confused.

Exit criteria:

- A Chinese-speaking new user can add a friend, join a group, find their public contact address, and understand backup and identity settings without reading external docs.

### Phase 2: real app QA

Target: 3 to 5 days.

Tasks:

- Test on at least one iOS simulator and one real iPhone if available.
- Test two-account messaging, one-time link creation, QR scan, public contact request, group join, media sending, and settings.
- Test backup and migration language against the actual behavior.
- Check all permission prompts and InfoPlist copy.
- Record a manual QA checklist with pass/fail notes.

Exit criteria:

- The main Nome flows are proven against the real app, not only static mockups.
- No critical action is decorative or misleading.

### Phase 3: branding, localization, and packaging

Target: 2 to 4 days.

Tasks:

- Audit all iOS strings for leftover user-facing SimpleX naming on branded surfaces.
- Keep protocol references where compatibility requires them.
- Export and verify all required app icon sizes.
- Prepare App Store style screenshots from real app screens.
- Review trademark, AGPL, attribution, and source availability language.
- Decide whether bundle identifiers stay compatibility-first or move to a dedicated Nome release track.

Exit criteria:

- Nome branding is consistent.
- Upstream attribution and license obligations are clear.
- Release identifiers are a conscious decision, not accidental leftovers.

### Phase 4: technical release readiness

Target: 1 to 2 weeks after UX MVP.

Tasks:

- Replace any simulator-only or local preview assumptions with a real release build path.
- Build with production signing settings.
- Prepare TestFlight distribution.
- Confirm notification extension, share extension, URL scheme, app group, and entitlements.
- Add a repeatable build checklist for future upstream rebases.
- Decide crash/log collection policy without weakening privacy promises.

Exit criteria:

- A TestFlight build can be produced and installed.
- Release-sensitive identifiers and entitlements are reviewed.
- The fork can still take upstream security fixes.

### Phase 5: productization after TestFlight

Target: after internal TestFlight.

Tasks:

- Gather feedback from Chinese users on onboarding friction.
- Improve invitation and group growth loops without adding global IDs or phone-number dependency.
- Decide whether to add a hosted server/operator recommendation layer.
- Decide commercial model only after the trust and distribution boundary is clear.

Exit criteria:

- The app has real user feedback, not only internal design confidence.

## Workstreams

### iOS SwiftUI UX

Owns:

- home/inbox;
- add friend and join group;
- public contact address;
- identity center;
- conversation reassurance;
- settings and safety.

### Protocol compatibility

Owns:

- keeping core behavior unchanged;
- link compatibility;
- upstream rebase safety;
- review of any protocol-adjacent copy or behavior.

### QA and preview

Owns:

- simulator build;
- browser preview;
- screenshot baselines;
- manual two-device test plan;
- regression checklist.

### Brand and localization

Owns:

- Nome naming consistency;
- Chinese copy;
- app icons and logo assets;
- App Store screenshots and metadata.

### Compliance and release

Owns:

- AGPL/source availability;
- upstream attribution;
- trademark language;
- Apple privacy labels;
- signing, entitlements, and TestFlight.

## Acceptance criteria

The iOS app is not considered ready until:

- the native iOS build passes;
- the app launches in the simulator;
- core messaging still works;
- every primary CTA maps to a real working path;
- Chinese main flows are understandable without external documentation;
- no UI claims stronger privacy than the underlying implementation provides;
- release-sensitive identifiers and entitlements have been reviewed before public distribution.

## Verification commands

Use full Xcode explicitly if the machine still points `xcode-select` at Command Line Tools:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild \
  -quiet \
  -project apps/ios/SimpleX.xcodeproj \
  -scheme "SimpleX (iOS)" \
  -configuration Debug \
  -destination 'id=0785AA21-8681-443A-95C9-D4BAB0BCC87A' \
  -derivedDataPath /tmp/nome-ios-derived \
  -skipPackagePluginValidation \
  -skipMacroValidation \
  build
```

Install and launch the simulator build:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun simctl install booted /tmp/nome-ios-derived/Build/Products/Debug-iphonesimulator/Nome.app
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun simctl launch booted chat.simplex.app
```

Check diff hygiene:

```bash
git diff --check
```

Check App Store screenshot candidates:

```bash
scripts/ios/check-app-store-screenshots.sh
scripts/ios/check-app-store-screenshots.sh --final
```

Generate and check the 10-slot App Store screenshot draft:

```bash
scripts/ios/export-app-store-screenshot-draft.sh --force
scripts/ios/check-app-store-screenshots.sh --dir design/app-store/ios-upload-draft-screens
scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens
```

Run the repeatable Nome UI smoke pass:

```bash
scripts/ios/smoke-nome-ui.sh
```

Run the smoke pass against an already-built current app bundle:

```bash
DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-smoke-primary-flows --skip-build
```

Check approved mockup coverage:

```bash
scripts/ios/check-nome-design-coverage.sh
```

Run the combined Nome iOS readiness gate:

```bash
scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-current
```

Run the same gate with the network-dependent real-core source audit:

```bash
scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-with-source-audit
```

When skipping the smoke build, point the gate at the current DerivedData path:

```bash
DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-primary-flows
```

Run the goal-level completion audit against the latest UI smoke evidence:

```bash
scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-current
```

Run the same goal audit with the network-dependent real-core source audit:

```bash
scripts/ios/check-nome-ios-goal-audit.sh --source-audit --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-with-source-audit
```

Run it without `--allow-blockers` when checking whether the app is actually release-ready:

```bash
scripts/ios/check-nome-ios-readiness.sh --output /tmp/nome-ios-readiness-release-gate
```

## Immediate next tasks

1. Follow `plans/20260709_nome_ios_real_core_testing_plan.md` to install or build real iOS core libraries so invitation, group, database, and messaging behavior can be tested beyond preview UI.
2. Execute the remaining manual QA batches in `plans/20260709_nome_ios_remaining_qa_execution_plan.md`, starting with Batch 0 real-core artifacts and then first-run/network, single-account connection surfaces, two-account messaging/groups, identity/migration, physical-device/camera, and final screenshots.
3. Replace the `needs-real-core` add-friend, public-contact, join-group, and conversation screenshots in `design/app-store/ios-upload-draft-screens/` once real iOS core libraries are available, then update `MANIFEST.md` and `FINAL_BLOCKERS.md` so strict final mode no longer sees preview/debug/needs-real-core status.
4. Prepare AGPL/source availability, upstream attribution, privacy labels, and export-compliance answers before TestFlight.
5. Review release identifiers, app groups, URL schemes, and notification extension settings before any TestFlight build.
6. Keep `scripts/ios/check-nome-ios-readiness.sh` and `scripts/ios/check-nome-ios-goal-audit.sh` green except for explicitly accepted `BLOCKED` items while real-core and final screenshot work is still open.

## Decision log

- Decision: keep this as a native iOS app workstream, not PWA-first. The product depends on native mobile behaviors such as notifications, QR scanning, share extension, local secure storage, URL handling, and the existing SimpleX mobile core.
- Decision: keep the first phase as UI, brand, and onboarding work. Do not change protocol, encryption, server routing, or database behavior in the same pass.
- Decision: keep compatibility-sensitive identifiers until the release track is explicitly reviewed.

## Current development-plan status

The Apple app plan is active and split into three tracks:

1. Product/UI implementation: keep applying the approved Nome design to the
   native iOS app while preserving SimpleX protocol, database, and encryption
   behavior.
2. Verification: keep simulator UI smoke checks, screenshot checks, brand-copy
   checks, readiness checks, and goal-audit scripts running as repeatable gates.
3. Real-core readiness: replace the preview core before claiming invitation,
   public-contact, group, or two-account messaging behavior.

Latest verification checkpoint: a full Xcode rebuild plus the 14-screen Nome
smoke pass now succeeds on simulator `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`.
The smoke manifest is
`/tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv`; integrated
readiness reports 21 PASS, 1 WARN, 6 BLOCKED, 0 FAIL under
`/tmp/nome-ios-readiness-full-smoke-20260709-225943`; the goal audit reports
32 PASS, 2 WARN, 6 BLOCKED, 0 FAIL under
`/tmp/nome-ios-goal-audit-full-smoke-20260709-225943`.

The remaining QA queue now has a machine-readable execution-state export:
`scripts/ios/export-nome-qa-execution-state.sh`. The latest readiness-owned
export is
`/tmp/nome-ios-readiness-screenshot-state-20260709-235128/qa_execution_state`;
it reports 130 total manual QA items, 89 checked, 41 unchecked, 7 gate
blockers, and 8 ordered next-action rows.

Release-sensitive identifiers now have a machine-readable state export too:
`scripts/ios/export-ios-release-identity-state.sh`. The latest readiness-owned
export is
`/tmp/nome-ios-readiness-release-state-20260709-233643/release_identity_state`;
it reports 21 current identifier rows, 8 required final decision rows,
compatibility-reviewed status PASS, and strict final release status BLOCKED.

The App Store screenshot package now has a machine-readable finalization-state
export: `scripts/ios/export-app-store-final-screenshot-state.sh`. The current
readiness-owned export is
`/tmp/nome-ios-readiness-screenshot-state-20260709-235128/app_store_final_screenshot_state`;
it reports candidate screenshot status PASS, blocker-plan status PASS, strict
final screenshot status BLOCKED, 10 screenshot rows, 6 ready rows, and 4
`needs-real-core` replacement rows for add-friend, public-contact, join-group,
and conversation.

Latest non-source total audit after adding the screenshot-state export:
readiness reports 27 PASS, 2 WARN, 7 BLOCKED, 0 FAIL under
`/tmp/nome-ios-readiness-screenshot-state-20260709-235128`; the goal audit with
the full-build smoke manifest reports 39 PASS, 2 WARN, 7 BLOCKED, 0 FAIL under
`/tmp/nome-ios-goal-audit-screenshot-state-20260709-235128`.

The x86_64 simulator real-core route now has a reversible build probe:
`scripts/ios/probe-x86_64-simulator-real-core.sh`. The direct probe under
`/tmp/nome-ios-x86_64-sim-real-core-probe-current` temporarily swapped
`apps/ios/Libraries/sim` to the local x86_64 real-core artifact, built a
`Nome.app` x86_64 simulator binary successfully, and restored the original
preview libraries. Manual install tests on the booted iOS 26.5 simulator,
including an `arch -x86_64 xcrun simctl install` retry, both failed with
`Failed to find matching arch`. This means x86_64 local real core is useful as
build evidence, but it does not clear functional simulator QA on the current
arm64-only simulator runtime.

The real-core route summary is now exported by
`scripts/ios/export-real-core-route-state.sh`. The latest readiness-owned
export is
`/tmp/nome-ios-readiness-route-state-20260710-001221/real_core_route_state`;
it reports 0 ready routes, 1 build-only route, and 3 blocked routes:
arm64 simulator is blocked by preview-core libraries, x86_64 simulator is
build-only because the current runtime rejects the x86_64 app, physical-device
QA is blocked by no connected trusted iPhone/iPad, and local Nix build is
blocked because Nix is not installed.

Latest source-aware total audit: readiness with `--source-audit` reports
25 PASS, 1 WARN, 8 BLOCKED, 0 FAIL under
`/tmp/nome-ios-readiness-source-state-20260709-233944`; the goal audit with the
full-build smoke manifest and `--source-audit` reports 37 PASS, 1 WARN,
8 BLOCKED, 0 FAIL under
`/tmp/nome-ios-goal-audit-source-state-20260709-233944`.

The latest real-core experiment found an important simulator constraint. The
staged Hydra `v6-5-5` core pair can pass installed-core preflight, but its
simulator package is `x86_64`; the current Apple Silicon simulator build needs
`arm64-apple-ios15.0-simulator`. After the failed link attempt, the project was
restored to the prior preview-core state from
`/tmp/nome-ios-preview-core-backup-20260709-205218`, and
`scripts/ios/sync-real-core-xcode-project.sh --check` plus `git diff --check`
both passed.

The next development checkpoint is therefore not another broad redesign. It is
to obtain a simulator-compatible real core or run a physical-device real-core
test, then replace the remaining preview/debug screenshot evidence with real
functional screenshots.

The real-core gate now includes architecture compatibility, not only file size
and preview-marker checks. `scripts/ios/check-real-core.sh` verifies `arm64`
device libraries and the current simulator architecture, and the prepare path
audits staged artifacts before replacing app libraries. A repeat attempt to
prepare the staged `v6-5-5` pair now fails before installation and leaves
`apps/ios/Libraries` unchanged, because the simulator archives are `x86_64` and
the Apple Silicon simulator build requires `arm64`.

The source-audit path is now architecture-aware too. On this machine, Xcode
reports only `arm64` iOS Simulator destinations, so `scripts/ios/check-real-core-sources.sh`
no longer treats Hydra's standard device-arm64 plus x86_64-simulator pair as
enough for the active simulator workflow. The next executable real-core options
are therefore: produce an arm64-simulator core, install a compatible simulator
runtime/destination that can use x86_64 archives, or use a physical iPhone for
real-core functional QA.

The latest source audit on 2026-07-09 still exits `1` for the active simulator
path. Local downloads, recent GitHub releases, recent GitHub Actions artifacts,
and the known Hydra repositories do not currently provide an arm64-simulator
core pair; the audit log is
`/tmp/nome-ios-real-core-source-audit-rc-20260709-231137.log`.

The simulator real-core route is now checked directly by
`scripts/ios/check-ios-simulator-real-core-route.sh` and included in both the
readiness gate and goal audit. Current evidence says the installed arm64
simulator libraries are still preview-core placeholders, while the local
x86_64 simulator artifact looks production-like but cannot be used because this
Xcode installation exposes no x86_64 iOS Simulator destination.

The physical-device path is now partly unblocked: local
`~/Downloads/pkg-ios-aarch64-swift-json` contains a production-sized arm64
device core artifact with no preview markers. It is not installed into
`apps/ios/Libraries/ios` yet, and no physical iPhone/iPad is connected, so this
is available input evidence rather than completed real-device QA.

`scripts/ios/prepare-device-real-core.sh` now makes that fallback executable in
a controlled way. Its dry-run verifies the local device artifact first and does
not touch `apps/ios/Libraries/sim`. The current dry-run passes, but it warns
that the local artifact filenames are `6.5.5.0` while the Xcode project still
references `6.5.6.1`; a physical-device build therefore still needs a matching
core pair or a deliberate project-reference sync decision before it can be
treated as functional release evidence.

`scripts/ios/check-real-core-xcode-sync.sh` now keeps that reference risk
visible in every readiness pass. Current evidence says the simulator libraries
match the Xcode project references, `apps/ios/Libraries/ios` is still missing,
and the local device candidate remains a warning-only mismatch until it is
installed for a deliberate physical-device test.

The physical-iPhone path now has a dedicated readiness gate:
`scripts/ios/check-ios-device-readiness.sh`. Current evidence says Xcode and
automatic signing settings are readable, but no physical iPhone/iPad is
connected and the restored preview-core checkout lacks
`apps/ios/Libraries/ios`, so device real-core QA remains blocked until those
two prerequisites are satisfied.

Release identifiers now have their own read-only gate:
`scripts/ios/check-ios-release-identifiers.sh`. It confirms the visible app name
is Nome, while intentionally blocking TestFlight/public-readiness until the
upstream `chat.simplex.*` bundle ids, App Group, keychain group, associated
domains, extension bundle identifiers/product names, and background-task
identifiers are reviewed and replaced or explicitly preserved.

The release-identifier gate now separates those two states. Default strict mode
still blocks final TestFlight/public distribution. `--compatibility-reviewed`
passes for the current development pass only because every retained upstream
identifier is listed in the release review as a documented compatibility
exception.

The secondary primary-flow pages have now received a visual-header refinement:
`添加朋友`, `加入群组`, and `公开联系方式` use a consistent compact Nome
navigation brand and clearer page-level hierarchy. The signed simulator smoke
run at `/tmp/nome-ios-smoke-visual-header-signed-20260710-002440/` captured all
14 UI states and produced no new crash report. A compile-only manual build with
`CODE_SIGNING_ALLOWED=NO` is explicitly not accepted as launch evidence because
it crashes at startup without the App Group entitlement.

The readiness gate can now reuse a previously captured smoke manifest with
`--smoke-manifest`. The latest manifest-backed readiness pass is
`/tmp/nome-ios-readiness-smoke-manifest-20260710-003640`: 31 PASS, 1 WARN,
7 BLOCKED, 0 FAIL. This makes the current visual UI evidence count in the
combined readiness report without requiring another simulator launch.

The QA execution handoff now matches the physical-device artifact route used by
the current aggregate gates. `scripts/ios/export-nome-qa-execution-state.sh`
accepts `--source-target physical-device` and
`--generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710`,
passes the physical-device target through to the real-core preflight and build
environment checks, reuses the existing generic device build packet, and records
`physical_device_smoke` as its own blocker. The latest execution-state export
is `/tmp/nome-ios-qa-execution-state-physical-route2-20260710`: 41 unchecked
manual QA items, 6 gate blockers, 8 next-action rows, and 0 FAIL rows.

The execution handoff's `next_actions.tsv` is now route-aware as well. The
latest packet at `/tmp/nome-ios-qa-execution-state-route-aware-actions-20260710`
keeps the same 41 unchecked manual QA items, 6 gate blockers, 8 next-action
rows, and 0 FAIL rows, but Batch 1 through Batch 4 now point at
`scripts/ios/check-real-core.sh --target physical-device`, and Batch 5 carries
`--generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710`
into the physical-device readiness export.

The latest current-tree simulator proof is
`/tmp/nome-ios-smoke-live-20260710-0730`: all 14 expected Nome UI states built,
launched, and captured successfully, with 14 PASS visual-quality rows and no
new crash report. The normal app launch has been restored and the live
simulator mirror is available at `http://127.0.0.1:3200/`. The corresponding
readiness report is `/tmp/nome-ios-readiness-live-20260710` (50 PASS, 0 WARN,
6 BLOCKED, 0 FAIL), and the goal audit is
`/tmp/nome-ios-goal-audit-live-20260710` (62 PASS, 1 WARN, 6 BLOCKED, 0 FAIL).
This proves the planned visual iOS pass and simulator interaction surface; it
does not replace the remaining real-core, physical-device, final screenshot,
or release-identifier evidence.
