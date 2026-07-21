# Nome iOS manual QA checklist

Date: 2026-07-09

## Purpose

This checklist is the manual verification gate for the Nome iOS UX pass.

Current status: visual simulator QA is available, but real invitation, group, database, and two-account messaging checks are blocked until real iOS core libraries replace the current preview libraries.

## Environment gate

- [ ] Real iOS core libraries are installed in `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`.
- [ ] `apps/ios/Libraries/sim` libraries are production-sized artifacts, not 9.9K preview placeholders.
- [x] Real-core readiness can be checked with `scripts/ios/check-real-core.sh`.
- [x] Real-core source availability, including recent GitHub stable and beta/prerelease assets, can be audited with `scripts/ios/check-real-core-sources.sh`; it requires a complete aarch64 + x86_64 artifact pair.
- [x] Local real-core Nix build readiness can be checked with `scripts/ios/check-real-core-build-env.sh`; it confirms the flake iOS targets and reports missing Nix / disk-space constraints without running a build.
- [x] Local arm64 device artifact input in `~/Downloads/pkg-ios-aarch64-swift-json` is production-sized, `arm64`, and free of preview-core markers for the physical-device path.
- [x] Real-core source-audit logic is covered by `scripts/ios/test-real-core-source-audit.sh`, including missing, partial, and complete local artifact fixture cases.
- [x] Real-core preparation safety is covered by `scripts/ios/test-prepare-real-core-safety.sh`, using a temporary sandbox to verify incomplete artifact sets fail before library replacement and Hydra `--job-repo` checks happen before download.
- [x] Real-core artifact directories or zips can be prepared through `scripts/ios/prepare-real-core.sh` once Hydra or local artifact inputs are available.
- [x] Nix result symlinks or downloaded artifact locations can be normalized with `scripts/ios/stage-real-core-artifacts.sh` before running `scripts/ios/prepare-real-core.sh --downloads-dir`.
- [x] Batch 0 real-core staging and optional preparation can be run through `scripts/ios/run-real-core-batch0.sh`; it stages local, Hydra, or Nix artifacts by default and only replaces app libraries when `--prepare` is supplied.
- [x] Real-core acquisition and functional QA handoff is saved in `plans/20260709_nome_ios_real_core_testing_plan.md`.
- [x] The app builds with `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild`.
- [x] The app installs and launches on an iOS simulator.
- [x] Repeatable preview/UI smoke coverage can be run with `scripts/ios/smoke-nome-ui.sh`.
- [x] Latest preview/UI smoke pass captured home, onboarding, existing-chat-list, conversation, safety-details, identity center, add friend, join group, public contact, settings, and contacts-tab screenshots under `/tmp/nome-ios-smoke-contacts-tab/` without a new Nome crash report.
- [x] UI smoke also checks screenshot file size, consistent dimensions, duplicate hashes, expected case count, and new crash reports.
- [x] UI smoke can auto-boot a selected shutdown simulator and retry transient `simctl` install, launch, and screenshot failures.
- [x] Current combined iOS readiness can be checked with `scripts/ios/check-nome-ios-readiness.sh`, including script syntax, local source/preparation safety unit tests, diff hygiene, localization lint, screenshot gates, real-core preflight, optional real-core source audit, and optional UI smoke.
- [x] Goal-level completion evidence can be checked with `scripts/ios/check-nome-ios-goal-audit.sh`; `--source-audit` also checks real-core source availability, and the audit fails normally until final screenshots and real core are resolved.
- [x] Manual QA completion status can be summarized with `scripts/ios/check-nome-manual-qa-status.sh`; it can also write `manual_qa_unchecked.tsv` for execution grouping, and it currently blocks completion while unchecked real-core, physical-device, and final-screenshot items remain.
- [x] Remaining manual QA blocker groups are mapped into execution batches in `plans/20260709_nome_ios_remaining_qa_execution_plan.md` and checked by `scripts/ios/check-nome-qa-execution-plan.sh`.
- [x] Remaining manual QA batches can be exported into TSV queues and Markdown evidence templates with `scripts/ios/export-nome-qa-batches.sh`.
- [x] The local x86_64 simulator real-core artifact can be probed with `scripts/ios/probe-x86_64-simulator-real-core.sh`; current evidence says it builds an x86_64 Nome simulator app and restores the original preview libraries, but the current iOS 26.5 simulator refuses to install it because no matching runtime architecture is available.
- [x] Current real-core route readiness can be exported with `scripts/ios/export-real-core-route-state.sh`; it records arm64 simulator blocked, x86_64 simulator build-only, physical device blocked, and local Nix build blocked as separate routes.
- [x] Nome-facing brand copy can be checked with `scripts/ios/check-nome-brand-copy.sh`, and the check is included in the combined readiness gate.
- [x] Approved `pages-v2` mockup coverage can be checked with `scripts/ios/check-nome-design-coverage.sh`, and the check is included in the combined readiness gate.
- [x] All 14 deterministic Nome preview states pass light and dark `accessibility-large` screenshot validation with consistent dimensions, unique hashes and simulator-setting restoration.
- [x] Seven critical preview states pass light and dark `accessibility-extra-extra-extra-large` stress capture; the accepted conversation evidence has no safety-card/message overlap.
- [x] The accessibility capture helper supports full, critical and focused-conversation case sets and restores appearance, content size and increased contrast through its exit trap.
- [ ] Physical-device VoiceOver focus order, announcements and activation are verified for onboarding, conversation safety controls and the disappearing-message prompt.
- [ ] The app installs and launches on a physical iPhone, if available.
- [ ] Two independent accounts/devices are available for end-to-end messaging.
- [ ] SMP/XFTP server access is reachable under the selected network settings.

## Onboarding and first setup

- [x] Debug onboarding welcome preview renders Nome logo, no-phone/no-public-ID framing, one-time-link explanation, and local-device storage explanation.
- [x] Debug local identity preview explains that the first profile is local device identity, not a cloud account.
- [x] Debug network setup preview separates message servers, notifications, and network privacy from identity privacy.
- [x] Debug network-use confirmation preview explains operator and user commitments in Chinese.
- [x] Welcome explanation sheet opens and uses Nome privacy language instead of the upstream long English story.
- [x] Message-server sheet handles an empty preview-core operator list without trapping the user.
- [x] Clean simulator first launch no longer shows the inherited upstream SimpleX `v6.5 的新内容` sheet before Nome UI.
- [x] Clean simulator first launch shows the Nome delivery-receipts decision page without a system notification permission alert covering it.
- [x] After choosing a delivery-receipts option, the notification permission prompt appears at the intended time and returns safely.
- [x] Clean simulator `启用送达回执` path advances to the notification permission prompt and returns safely to the Nome home screen when notifications are denied.
- [ ] Clean first-run flow creates a real profile and advances to network setup.
- [ ] Clean first-run flow accepts network conditions and reaches the Nome home screen.
- [x] First-run migration entry still opens the real migration path and returns safely.

## Home and navigation

- [x] Nome logo and visible app name render on the home screen.
- [x] Empty home explains add friend, join group, and public contact address in Chinese.
- [x] Bottom navigation switches between home, contacts, and settings without opening duplicate sheets.
- [x] Bottom `联系人` opens a real in-place contacts page with counts, quick actions, and empty state.
- [x] Legacy one-hand bottom toolbar is hidden when Nome bottom navigation is active.
- [x] Returning from each previewable primary flow restores the expected home, contacts, settings, or add-list state.
- [x] Debug seeded existing chats render in the redesigned list without losing unread, report/archive, muted, or group state.

## Add friend

- [x] Home `添加朋友` opens the one-time link flow directly.
- [x] Preview-core simulator shows a clear readiness message instead of pretending to create a real link.
- [x] Preview-core `添加朋友` screen keeps the full one-time link and QR layout visible instead of collapsing to a blank retry page.
- [x] Add-friend back path returns to the add list, and the add list has an explicit `关闭` action that returns to the Nome home screen.
- [ ] Real core creates a valid one-time link.
- [ ] Real core creates a scannable one-time QR code.
- [ ] Sharing the link uses the native share sheet.
- [ ] A second account can open the link and create a connection.
- [ ] The first account receives and accepts the connection request.
- [ ] Failed, expired, or already-used links show understandable errors.

## Join group

- [x] Home `加入群组` opens the group-specific join page directly.
- [x] The scanner card does not request camera permission before the user taps it.
- [x] Invalid pasted links show a clear `链接无效` style error.
- [x] Join-group back path returns to the add list, and the add list `关闭` action returns to the Nome home screen.
- [ ] Real group invitation links show a preview before joining.
- [ ] Joining a valid group succeeds.
- [ ] Request-approval groups explain the pending state.
- [ ] Bad, expired, or non-group links are rejected without crashing.
- [ ] Live QR scanning works after camera permission is accepted.

## Public contact address

- [x] Home public-contact entry opens the redesigned `公开联系方式` page.
- [x] Preview-core copy/share/change/disable actions are guarded with a clear message.
- [x] Public-contact sheet has an explicit `关闭` action that returns to the Nome home screen.
- [ ] Real core creates or loads a reusable public contact address.
- [ ] Copy copies the actual reusable address.
- [ ] Share opens the native share sheet with the actual address.
- [ ] Request confirmation toggle maps to the real setting.
- [ ] Change, disable, and settings actions work or show exact existing constraints.
- [ ] A second account can request contact via the public address.

## Conversation

- [x] Debug launch with `-NomeConversationPreview` opens a seeded native conversation page.
- [x] Seeded conversation renders Chinese messages, a file item, a voice item, the safety subtitle, the Nome safety card, the disappearing-message prompt, and the compose bar.
- [x] Seeded conversation uses the Nome clean safety background instead of the upstream school wallpaper.
- [x] Normal launch without `-NomeConversationPreview` still opens the regular Nome home screen.
- [ ] A real one-to-one conversation sends and receives text.
- [ ] A real group conversation sends and receives text.
- [ ] File sending and receiving work.
- [ ] Voice messages record, send, play, and display duration correctly.
- [ ] Delivery receipts, edit, reply, delete, and reaction actions remain reachable.
- [ ] Safety banner reflects verified and unverified states accurately.
- [x] Debug preview disappearing-message prompt opens a preview-safe safety/settings sheet and does not overclaim deletion guarantees.
- [ ] Real disappearing-message prompt opens the existing details/settings path and persists the selected setting.

## Identity and privacy settings

- [x] Home avatar opens the profile menu.
- [x] `身份中心` opens the redesigned identity center sheet.
- [x] `身份中心` closes back to the Nome home screen and explains identities as local profiles, not cloud accounts.
- [x] Debug launch with `-NomeIdentityCenterPreview` captures the redesigned identity center in the repeatable UI smoke pass.
- [x] `从桌面端使用` shows a preview-core readiness message instead of a technical `%@` error, and closes back to Nome home.
- [ ] Profile switching works with multiple real profiles.
- [ ] Hidden profile entry, unlock, and exit paths still respect existing passcode behavior.
- [ ] Incognito/new-profile explanations do not imply account anonymity beyond the underlying SimpleX model.

## Settings, backup, and network

- [x] Bottom `设置` tab opens the redesigned embedded settings page.
- [x] Settings tab can return to home.
- [x] `数据与存储` opens the real database/storage management screen from the Nome settings tab.
- [x] `隐私与安全` opens the real privacy settings screen from the Nome settings tab.
- [x] `服务器与 Tor` opens the real network/server settings screen from the Nome settings tab without crashing.
- [x] In the preview-core simulator, `服务器与 Tor` shows a clear real-core-readiness message instead of a technical `unexpected result: cmdOk` alert.
- [x] Notification settings open their real existing screen.
- [x] Returning from notification settings restores the embedded settings tab, then bottom `首页` returns to the Nome home screen.
- [x] `备份与迁移` opens a Nome safety hub instead of immediately starting device migration or stopping chat.
- [x] Backup/export language matches the actual backup behavior by routing `导出或导入数据库` to the existing database export/import screen.
- [x] Returning from data/storage export/import restores the backup hub, then returns to the settings tab.
- [ ] Migration/device-transfer paths still work.
- [ ] Server, Tor, and private-routing controls work against real core/server data.
- [x] Settings rows do not hide critical upstream safety controls.

## Release gate

- [x] Safe `SimpleX Lock`, hidden-notification placeholder, and old public-address onboarding/share labels are converted to Nome/public-contact-address copy.
- [x] Settings `关于 Nome` opens a dedicated Nome about page with version, privacy-boundary, protocol-compatibility, and upstream-attribution information.
- [x] Settings `帮助与反馈` opens a dedicated Chinese Nome help page instead of the upstream English `ChatHelp` screen.
- [x] Remaining user-facing SimpleX names are classified as either compatibility labels or should become Nome copy.
- [x] Bundle id, URL schemes, app groups, notification extension, and share extension identifiers are reviewed before TestFlight.
- [x] Notification service and share extension display names are Nome-facing in Xcode build settings and localized InfoPlist strings.
- [x] `scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed` passes for the current compatibility-first development pass.
- [ ] `scripts/ios/check-ios-release-identifiers.sh` passes for the final TestFlight or public distribution configuration.
- [ ] Nome-owned bundle identifiers are selected for the app, tests, notification service extension, share extension, and internal framework, or compatibility exceptions are explicitly approved for release.
- [ ] Nome-owned App Group and keychain access group names are selected with an explicit data-migration decision.
- [ ] Associated domains and background task identifiers are either Nome-owned or documented compatibility exceptions.
- [x] AGPL/source availability packet and upstream attribution are prepared for release review; final public source URL remains required before distribution.
- [x] App Store screenshot candidates come from real app screens, not generated mockups.
- [x] App Store screenshot manifest/checker records dimensions, hashes, preview/debug caveats, and final replacement requirements.
- [x] Final screenshot checker also scans `MANIFEST.md`, so renaming preview/debug files without updating evidence cannot pass strict mode.
- [x] A 10-slot flattened JPEG screenshot draft can be generated with `scripts/ios/export-app-store-screenshot-draft.sh --force`.
- [x] The generated screenshot draft passes candidate checking with a manifest-level non-final warning and four explicit `needs-real-core` replacement warnings.
- [x] App Store final screenshot replacement state can be exported with `scripts/ios/export-app-store-final-screenshot-state.sh`; it records candidate PASS, strict final BLOCKED, and the four `needs-real-core` rows with manual QA anchors.
- [ ] Final ten-or-fewer App Store screenshot package passes `scripts/ios/check-app-store-screenshots.sh --final`.
- [x] Privacy claims are checked against current implementation boundaries and server/operator copy; final App Store privacy/export review remains required.
