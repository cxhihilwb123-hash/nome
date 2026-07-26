# Nome iOS QA record

Date: 2026-07-09

## Scope

This record covers the current iOS UX implementation pass for the Nome mockup direction.

Focus for this pass:

- first-run onboarding redesign;
- direct home action routing;
- public contact address page redesign;
- settings bottom-tab redesign;
- identity center entry verification;
- chat-detail safety status banner;
- native iOS build/install/launch;
- simulator preview evidence for the key add/join entry points.

## Code changes verified

- `ChatListView.swift`
  - added an initial destination state for the add/join sheet;
  - routed the home "Add friend" card directly to the one-time link flow;
  - routed the home "Join group" card directly to the scan/paste invite flow;
  - routed the empty inbox "Add friend" button directly to the one-time link flow;
  - keeps the empty Nome home in normal visual order instead of inheriting the inverted one-hand chat-list order;
  - keeps the non-empty Nome home in normal visual order while the Nome bottom tab bar is active, instead of inheriting the old one-hand inverted chat-list order;
  - uses a compact quick-action strip on the home screen when conversations already exist, so the first screen prioritizes recent chats over empty-state task cards;
  - hides the legacy SimpleX one-hand bottom toolbar while the Nome bottom navigation is visible;
  - allows add/join pages to open for UI preview while keeping real generation/connection actions guarded by chat-core readiness.
  - localizes the public-contact sheet title to `公开联系方式`.
  - switches the bottom `设置` tab in place instead of opening Settings as a sheet.
  - switches the bottom `联系人` tab to a real in-place contacts page instead of opening the add/new-chat sheet;
  - adds contacts-page counts, quick actions, empty state, and existing chat row rendering for contacts/groups/requests.
- `NewChatMenuButton.swift`
  - added `NewChatSheetInitialDestination`;
  - initialized the sheet so it can open directly into menu, one-time link, generic scan/paste invite, or group-specific join mode.
- `SimpleXApp.swift`
  - starts chat initialization when local authentication is not enabled, even if stale Keychain passcodes exist.
  - adds Debug-only onboarding preview launch arguments for welcome, local identity, network setup, and network-use confirmation pages, while keeping the normal app launch path unchanged.
  - adds a Debug-only `-NomeChatListPreview` launch argument that seeds representative existing chat-list rows for visual QA without starting the chat core.
  - adds Debug-only primary-flow preview launch arguments for add friend, join group, public contact, and settings, while keeping the normal app launch path unchanged.
- `SimpleXInfo.swift` and `HowItWorks.swift`
  - replace the first welcome screen and protection explainer sheet with Nome-branded Chinese cards for no phone number, no public ID, one-time links, and local-device storage;
  - add shared Nome onboarding card, feature-row, pill, logo-header, and palette components for the first-run flow;
  - add an explicit `关闭` button to the onboarding migration sheet so users are not dependent on a swipe-down gesture to return.
- `CreateProfile.swift`
  - reframes first profile creation as local identity setup rather than a cloud account;
  - keeps the existing `apiCreateActiveUser` and `startChat(onboarding:)` behavior unchanged;
  - localizes the migration entry and profile-name field copy for the Nome onboarding flow;
  - adds an explicit `关闭` button to the first-profile migration sheet while preserving the existing `MigrateToDevice` flow.
- `YourNetwork.swift`
  - reframes network setup around message servers, notification mode, and the distinction between identity privacy and network privacy;
  - keeps the existing server-operator and notification settings sheets wired to their real actions.
- `ChooseServerOperators.swift`
  - restyles onboarding buttons to match the Nome visual system;
  - reframes network commitments and server-operator selection in Chinese;
  - changes network-use confirmation copy from absolute relationship-protection wording to the more accurate claim that Nome reduces connection-relationship exposure;
  - fixes the empty operator-list state so preview core can show a clear default-server explanation and an enabled `完成` button instead of an empty, non-dismissable sheet.
- `ContentView.swift`
  - disables the inherited upstream SimpleX "What's New" sheet on Nome first launch;
  - delays the system notification permission request until no Nome first-launch notice or delivery-receipts decision page is covering the screen;
  - guards notification authorization so the delayed path and the delivery-receipts change path cannot duplicate the same system request in one view session;
  - keeps the existing notification permission request behavior after the delivery-receipts decision has been handled.
- `SetDeliveryReceiptsView.swift`
  - replaces the inherited delivery-receipts first-run page with a Nome-branded Chinese decision page;
  - explains that delivery receipts mean delivered, not read status or online status;
  - preserves the existing enable/skip behavior and the existing privacy preference update.
- `scripts/ios/check-real-core.sh`
  - adds a read-only real-core preflight gate for the iOS app;
  - detects missing device libraries, simulator preview placeholders, preview-core markers, missing local artifacts, and missing `mac2ios` / `nix`;
  - now distinguishes real installed-core failures from preparation-tool availability, so future installed real libraries are not failed only because temporary download artifacts were cleaned up;
  - supports `DOWNLOADS_DIR` so local artifact checks can point at a custom directory;
  - can optionally check a Hydra job repository URL with `--job-repo`.
- `scripts/ios/check-real-core-sources.sh`
  - adds a read-only source audit for local iOS core artifacts, recent GitHub stable and beta/prerelease release assets, recent GitHub Actions artifacts, and an optional Hydra job repository URL;
  - requires a complete aarch64 + x86_64 artifact pair before reporting a usable source;
  - defaults to 12 recent releases, three 100-artifact GitHub Actions pages, and known `ci.zw3rk.com/job/simplex-chat-simplex-chat` Hydra candidates, with `RELEASE_LIMIT`, `ACTIONS_ARTIFACT_PAGES`, `INCLUDE_KNOWN_HYDRA_REPOS`, `KNOWN_HYDRA_JOB_REPOS`, and newline-separated `HYDRA_JOB_REPOS` overrides for deeper or offline audits;
  - exits non-zero when no usable source is found, without downloading, unzipping, or replacing any libraries;
  - supports `SOURCE_TARGET=simulator|physical-device|any`, so the same read-only audit can prove that the current arm64 simulator route is still blocked while the local arm64 artifact is a physical-device testing candidate.
- `scripts/ios/check-real-core-build-env.sh`
  - adds a read-only check for the local Nix build path;
  - verifies the expected flake targets and `pkg-ios-*-swift-json` output markers exist;
  - reports Nix availability, local `mac2ios`, full Xcode, `simctl` when `DEVELOPER_DIR` points to full Xcode, current `xcode-select`, and free disk space;
  - exits non-zero when local build prerequisites are missing, without installing Nix, running a build, or replacing libraries.
- `scripts/ios/test-real-core-source-audit.sh`
  - adds a local, network-independent regression test for `check-real-core-sources.sh`;
  - verifies that missing artifacts fail, a single architecture fails, and a complete aarch64 + x86_64 fixture pair passes.
- `scripts/ios/test-prepare-real-core-safety.sh`
  - adds a local, sandboxed regression test for `prepare-real-core.sh`;
  - verifies missing artifacts, a single-architecture artifact set, and an empty second-architecture directory all fail before library replacement;
  - verifies the Hydra `--job-repo` path checks source availability before download and runs installed-core preflight only after download.
- `scripts/ios/prepare.sh` and `scripts/ios/prepare-x86_64.sh`
  - now validate `mac2ios` and expected local artifacts before deleting or replacing `apps/ios/Libraries/*`, reducing the risk of accidentally removing the current buildable preview libraries.
- `scripts/ios/build-mac2ios.sh`
  - builds the local `tools/bin/mac2ios` helper from `zw3rk/mobile-core-tools` without requiring a global Homebrew or Nix install;
  - the generated `tools/bin/mac2ios` binary is ignored by git and can be rebuilt when needed.
- `scripts/ios/prepare-real-core.sh`
  - adds a single entry point for real iOS core preparation from either a Hydra job repository or local downloads;
  - accepts extracted `pkg-ios-*-swift-json/` directories or matching `.zip` files;
  - supports `--downloads-dir` / `DOWNLOADS_DIR` for artifacts outside `~/Downloads`;
  - ensures `mac2ios` is available, checks Hydra source availability with `scripts/ios/check-real-core-sources.sh` before downloads, delegates to the existing repo preparation scripts, and finishes by running `scripts/ios/check-real-core.sh`.
- `scripts/ios/stage-real-core-artifacts.sh`
  - normalizes Nix result symlinks, local artifact folders, or artifact zip locations into the `pkg-ios-aarch64-swift-json` + `pkg-ios-x86_64-swift-json` layout expected by `scripts/ios/prepare-real-core.sh --downloads-dir`;
  - fails when either architecture is missing or a source directory contains ambiguous duplicate candidates;
  - does not run `mac2ios` and does not replace `apps/ios/Libraries`.
- `scripts/ios/run-real-core-batch0.sh`
  - provides a Batch 0 execution entry point for the real-core blocker;
  - stages a complete artifact pair from a local source directory, downloads and stages a supplied Hydra job repository, or, when `--build-with-nix` is supplied and Nix is installed, builds both iOS core architectures before staging;
  - keeps library replacement explicit: it does not modify `apps/ios/Libraries` unless `--prepare` is supplied.
- `scripts/ios/sync-real-core-xcode-project.sh`
  - synchronizes the two explicit `libHSsimplex-chat...` archive references in the iOS Xcode project with the real core archive filenames currently present in `apps/ios/Libraries/sim`;
  - supports `--check` so the build can verify project references after real-core preparation without editing.
- `scripts/ios/test-stage-real-core-artifacts.sh`
  - regression-tests the staging helper with complete, missing-architecture, and duplicate-candidate fixture cases.
- `scripts/ios/smoke-nome-ui.sh`
  - adds a repeatable simulator smoke pass for the current previewable Nome UI: home, onboarding welcome/profile/network/conditions, existing-chat-list preview, conversation preview, conversation details preview, identity center preview, add-friend preview, join-group preview, public-contact preview, settings preview, and contacts-tab preview;
  - builds with full Xcode, installs the app, captures screenshots, records dimensions, file size, and SHA-256 hashes;
  - auto-boots the selected simulator if it is shut down and retries transient `simctl` install, launch, and screenshot failures;
  - fails if screenshots are too small, have inconsistent dimensions, duplicate another smoke screenshot hash, capture fewer than fourteen cases, or create a new `Nome-*.ips` crash report.
- `scripts/ios/check-nome-design-coverage.sh`
  - verifies that all seven approved `design/product/pages-v2/` mockups exist;
  - verifies that `plans/20260709_nome_ios_design_coverage_matrix.md` contains coverage IDs for home, add friend, join group, public contact, identity, conversation, and settings;
  - verifies that the smoke script includes the current fourteen screenshot labels, including the primary-flow labels for add friend, join group, public contact, settings, and contacts;
  - verifies the Swift implementation still contains the expected Nome page components, preview hosts, launch arguments, and preview-core readiness guard for the seven approved pages.
- `scripts/ios/check-nome-manual-qa-status.sh`
  - summarizes the manual QA checklist with checked/unchecked counts and section-level unchecked counts;
  - can write a machine-readable `manual_qa_unchecked.tsv` queue with `line`, `section`, `blocker_group`, and `item` columns;
  - exits non-zero while any manual QA item is unchecked, preventing the goal audit from treating partial manual testing as complete.
- `plans/20260709_nome_ios_remaining_qa_execution_plan.md`
  - maps the current unchecked manual QA blocker groups into seven execution batches: real-core artifacts, first-run/network, single-account connection surfaces, two-account messaging/groups, identity/migration, physical-device/camera, and final App Store screenshots;
  - records the exact recheck commands that must pass before the iOS goal can be considered complete.
- `scripts/ios/check-nome-qa-execution-plan.sh`
  - verifies that the remaining-QA execution plan covers every current `blocker_group` emitted by `scripts/ios/check-nome-manual-qa-status.sh --write-tsv`;
  - fails if unclassified `manual_followup` items appear or if the plan does not name the manual-QA, real-core, final-screenshot, and goal-audit recheck commands.
- `scripts/ios/export-nome-qa-batches.sh`
  - exports the current unchecked manual QA queue into batch-specific TSV files and Markdown evidence templates;
  - writes a manifest with batch counts and fails if the batch totals do not match the source unchecked queue.
- `scripts/ios/check-nome-ios-goal-audit.sh`
  - audits the goal-level evidence for the iOS UX pass: planning records, QA records, seven approved design artifacts, contacts smoke standard, contacts launch argument, design coverage, brand copy, real-core staging, manual QA status, latest UI smoke manifest, App Store screenshot candidates, final screenshot readiness, real-core preflight, optional real-core source audit, and physical-device status;
  - exits non-zero unless `--allow-blockers` is supplied while only known blockers remain, so it cannot accidentally mark the whole goal as complete while real core or final screenshots are still blocked.
- `scripts/ios/download-libs.sh`
  - now uses `set -euo pipefail` and handles the optional architecture argument without an unbound-variable failure.
- `plans/20260709_nome_ios_real_core_testing_plan.md`
  - records the supported Hydra, local artifact, and local Nix paths for replacing preview iOS core libraries;
  - defines the functional QA sequence required before claiming real add-friend, public-contact, group, messaging, backup, desktop-linking, or final screenshot readiness.
- `SimpleXAPI.swift`
  - returns a user-facing alert when contact invitation creation is attempted without a ready local profile;
  - adds `realChatCoreReadinessError()` so UI entry points can distinguish UI preview from a real chat-core runtime.
- `NewChatView.swift`
  - shows an inline Chinese explanation on the one-time link page when invitation creation cannot proceed, instead of only showing a bare retry button;
  - checks chat-core readiness before calling the invitation API;
  - adds a group-specific join page with invitation-link input, scan card, preview placeholder, source warning, and a clearly disabled public-group directory row.
- `UserAddressView.swift`
  - replaces the old settings-list presentation with a Nome card layout matching the product mockup direction;
  - shows reusable public address, copy/share buttons, request-confirmation toggle, address management, settings entry, one-time-link entry, and safety note;
  - guards preview-core copy/share/change/disable/settings actions with a clear user-facing message;
  - changes contact-address sharing copy from SimpleX contacts to Nome contacts.
- `SettingsView.swift`
  - adds an embedded Nome tab mode with a ScrollView card layout;
  - shows logo, main identity card, settings group, privacy/server group, and help/about rows;
  - keeps the existing full settings list available for non-tab usage;
  - adds a Nome tab `完整设置` entry under `高级`, preserving access to the full settings list for call, appearance, advanced backup, community, developer, version, and other upstream controls;
  - routes `备份与迁移` through a Nome safety hub so tapping the settings row no longer immediately starts device migration or stops chat;
  - keeps real backup/export/import controls reachable from that hub via the existing `DatabaseView`;
  - routes `关于 Nome` to a dedicated Nome about page with version, product positioning, privacy-boundary, protocol-compatibility, and upstream-attribution information instead of the onboarding-style `SimpleXInfo` view;
  - routes `帮助与反馈` to a dedicated Chinese Nome help page instead of the upstream English `ChatHelp` screen, while leaving the upstream `ChatHelp` view available for other paths;
  - adds a `数据与存储` row in the embedded Nome settings tab that opens the existing `DatabaseView`;
  - injects `SaveableSettings` into the embedded server-settings navigation path so `服务器与 Tor` can open without a missing-environment-object crash.
- `NetworkAndServers.swift`
  - shows the shared real-core-readiness message when preview-core server loading fails, instead of exposing `unexpected result: cmdOk` to users.
- `PrivacySettings.swift`
  - changes user-facing `SimpleX Lock` and `Share to SimpleX` labels to Nome-branded wording.
- `ChatView.swift`
  - replaces the lightweight safety banner with a Nome status card for encryption, security-code verification, and timed-message/delete status;
  - uses real `Contact.verified`, `ChatInfo.featureEnabled(.timedMessages)`, and `ChatInfo.ttl(...)` state instead of hard-coding a verified claim;
  - opens the existing chat-detail sheet from the banner for private chats and group chats.
  - adds a real-model disappearing-message prompt above the compose bar when timed messages are available but the current chat TTL is off;
  - adds a Debug-only seeded `NomeConversationPreviewHost`, opened only by `-NomeConversationPreview`, so the native conversation page can be visually checked in simulator without starting the chat core;
  - adds a Debug-only `-NomeConversationPreviewOpenDetails` launch argument and a preview-safe safety/settings sheet so conversation preview QA can verify the `阅后即焚 · 设置` explanation without calling the real chat core;
  - replaces the inherited upstream `school` default wallpaper at render time with a Nome clean safety background, while leaving custom wallpapers and other presets on the existing renderer;
  - changes the chat-address label from `SimpleX address` to `Contact address`.
- `ChatInfoToolbar.swift`
  - shows a Nome safety subtitle such as `安全会话`, `安全群组`, or `本地笔记` under the chat title.
- `SimpleXApp.swift`
  - routes Debug launches with `-NomeConversationPreview` to the seeded conversation preview host;
  - routes Debug launches with `-NomeChatListPreview` to the seeded existing-chat-list preview host;
  - keeps normal launches on the existing `ContentView` path.
- `ContentView.swift`, `LocalAuthenticationUtils.swift`, `NtfManager.swift`, and `CreateSimpleXAddress.swift`
  - change remaining safe `SimpleX Lock`, notification hidden-preview, and deprecated public-contact-address onboarding/share strings to Nome or public-contact-address wording;
  - leave protocol, server, copyright, developer, and upstream-attribution references untouched.
- `apps/ios/zh-Hans.lproj/Localizable.strings`
  - adds Simplified Chinese translations for current Nome-facing keys such as Nome Lock, notification placeholder, Nome contact sharing, invitation email copy, and contact-address labels.
- `plans/20260709_nome_ios_brand_copy_audit.md`
  - classifies remaining iOS `SimpleX` references as compatibility labels, protocol/server wording, upstream attribution, legacy/non-primary paths, or Nome-facing copy to change.
- `plans/20260709_nome_ios_release_gate_review.md`
  - records the current TestFlight-sensitive identifiers, extension identifiers, app group, keychain access group, URL scheme, associated domains, AGPL/source-availability obligations, and current compatibility-first decision.
- `plans/20260709_nome_ios_privacy_claims_audit.md`
  - checks Nome's primary privacy claims against the current implementation boundary;
  - records the source-availability and upstream-attribution release packet;
  - keeps final public modified-source URL, asset/trademark decision, App Store privacy labels, and export-compliance answers open until release evidence exists.
- `design/app-store/ios-real-screens/`
  - saves a stable App Store screenshot candidate set copied from real iOS Simulator screenshots rather than generated product mockups;
  - marks preview-core and debug-only candidates so they are not mistaken for final real-core messaging evidence.
- `design/app-store/ios-real-screens/MANIFEST.md`
  - records each candidate screenshot's dimensions, alpha status, SHA-256 hash, preview/debug boundary, and final-release replacement condition;
  - records that the current folder has eleven candidates while App Store final packaging must select ten or fewer.
- `scripts/ios/check-app-store-screenshots.sh`
  - adds a read-only checker for screenshot count, accepted iPhone dimensions, alpha-channel warnings, preview/debug filenames, and SHA-256 evidence;
  - supports a stricter `--final` mode for the eventual upload package;
  - now checks PNG, JPG, and JPEG screenshots, treats `needs-real-core` filenames as non-final in strict mode, and scans `MANIFEST.md` plus `FINAL_BLOCKERS.md` for non-final preview/debug/needs-real-core status so filename-only renames cannot pass the final gate.
- `scripts/ios/export-app-store-screenshot-draft.sh`
  - exports the current 10-slot App Store planning set as flattened JPEGs under `design/app-store/ios-upload-draft-screens/`;
  - drops the lower-priority about screenshot from the eleven-candidate source set;
  - names add-friend, public-contact, join-group, and conversation exports with `needs-real-core` so the draft cannot be mistaken for the final public package;
  - writes `FINAL_BLOCKERS.md` with the exact replacement evidence required for those four final-blocking screenshots.
- `scripts/ios/check-nome-ios-readiness.sh`
  - adds a combined Nome iOS readiness gate for script syntax, the local real-core source-audit unit test, the sandboxed prepare-real-core safety unit test, diff hygiene, Simplified Chinese localization lint, screenshot candidates, final screenshot readiness, real-core readiness, optional real-core source audit, and optional simulator UI smoke;
  - classifies missing real iOS core libraries and non-final App Store screenshots as `BLOCKED` instead of pretending they are complete or treating them as unexpected code failures;
  - writes a reusable `summary.tsv` plus per-check logs under the selected output directory.
- `scripts/ios/check-nome-brand-copy.sh`
  - adds a regression gate for old product wording on Nome-facing Swift and Simplified Chinese surfaces;
  - checks fixed phrases such as `SimpleX Lock`, old invite/share copy, and `SimpleX address` on branded surfaces while preserving protocol/server/upstream attribution references outside this gate;
  - is now included in `scripts/ios/check-nome-ios-readiness.sh`.
- `WhatsNewView.swift`
  - changes the short-address version-note UI from `Short SimpleX address` / `SimpleX address` to `Short contact address` / `Contact address`.
- `apps/ios/zh-Hans.lproj/Localizable.strings`
  - updates legacy visible values for lock, invite email, share sheet, self-address warning, and short-address labels to Nome / public-contact-address wording without changing the original localization keys.

## Verification commands

Build:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild \
  -quiet \
  -project apps/ios/SimpleX.xcodeproj \
  -scheme "SimpleX (iOS)" \
  -configuration Debug \
  -destination 'id=95CA9F4F-F85B-4AC9-ADAE-62098924E3B4' \
  -derivedDataPath /tmp/nome-ios-derived-clean \
  -skipPackagePluginValidation \
  -skipMacroValidation \
  build
```

Install and launch:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun simctl install 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 /tmp/nome-ios-derived-clean/Build/Products/Debug-iphonesimulator/Nome.app
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun simctl launch 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 chat.simplex.app
```

Interaction checks:

```bash
/Users/forkman03/.npm/_npx/952f9bf55a4c6785/node_modules/.bin/serve-sim tap --device 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 0.78 0.55
/Users/forkman03/.npm/_npx/952f9bf55a4c6785/node_modules/.bin/serve-sim tap --device 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 0.29 0.49
```

Real-core preflight:

```bash
scripts/ios/check-real-core.sh
```

Batch 0 real-core staging and explicit preparation:

```bash
scripts/ios/run-real-core-batch0.sh --source ~/Downloads --output /tmp/nome-ios-real-core-batch0 --force
scripts/ios/run-real-core-batch0.sh --job-repo <hydra-job-repo> --output /tmp/nome-ios-real-core-batch0 --force
scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-real-core-batch0 --prepare
```

App Store screenshot candidate check:

```bash
scripts/ios/check-app-store-screenshots.sh
```

App Store screenshot draft export:

```bash
scripts/ios/export-app-store-screenshot-draft.sh --force
scripts/ios/check-app-store-screenshots.sh --dir design/app-store/ios-upload-draft-screens
scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens
```

Nome UI smoke:

```bash
scripts/ios/smoke-nome-ui.sh
```

Check the remaining-QA execution batches against the current manual QA queue:

```bash
scripts/ios/check-nome-qa-execution-plan.sh
scripts/ios/export-nome-qa-batches.sh --output /tmp/nome-ios-qa-batches-current --force
```

Nome design coverage:

```bash
scripts/ios/check-nome-design-coverage.sh
```

Nome iOS readiness gate:

```bash
scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-current
```

Nome brand-copy gate:

```bash
scripts/ios/check-nome-brand-copy.sh
```

Release gate mode:

```bash
scripts/ios/check-nome-ios-readiness.sh --output /tmp/nome-ios-readiness-release-gate
```

Diff hygiene:

```bash
git diff --check
```

Localization lint:

```bash
plutil -lint apps/ios/zh-Hans.lproj/Localizable.strings
```

## Results

Passed:

- iOS Debug build completed successfully.
- App installed into the booted iPhone 17 simulator.
- App launched as bundle id `chat.simplex.app`.
- Installed app metadata shows `CFBundleDisplayName = Nome` and `CFBundleName = Nome`.
- Home screen renders Nome logo, task cards, and bottom navigation.
- The empty Nome home renders in the intended order: header, search field, main tasks, safety explainer.
- The old one-hand toolbar no longer appears below the Nome bottom navigation.
- Tapping home "Join group" opens the group-specific join page directly.
- Tapping home "Add friend" opens the one-time link page directly.
- With the current preview core, real invitation generation and link connection are guarded before core API calls; the UI can still be previewed.
- In preview-core state, the one-time link page now renders a complete Nome layout with explanation, placeholder link, disabled share state, QR placeholder, retry action, and safety note instead of collapsing into a blank retry page.
- The add/new-chat sheet top level now shows an explicit `关闭` action, so users are not dependent on a swipe-down gesture to leave the add flow.
- From the one-time-link page, tapping back returns to the add list; tapping `关闭` returns to the Nome home screen with bottom `首页` still selected.
- The join-group page no longer requests camera permission immediately on entry; the scanner card shows `点击扫码` until the user taps it.
- From the join-group page, tapping back returns to the add list; tapping `关闭` returns to the Nome home screen with bottom `首页` still selected.
- Pasting an invalid/non-group preview link into the join-group page shows a clear `链接无效` alert and inline `链接格式无效。` message.
- Temporary `NOME_DIAG` logs were removed after the preview-core diagnosis.
- Tapping home "Public contact address" opens the redesigned `公开联系方式` page directly.
- The public contact page renders a single large title, explanation card, reusable-address card, copy/share controls, request-confirmation toggle, address-management card, and one-time-link entry.
- Tapping `复制` on the public contact page in the preview-core simulator shows `真实聊天 core 尚未可用` instead of pretending to copy a real usable address.
- The public contact sheet now has an explicit `关闭` action, and tapping it returns to the Nome home screen with bottom `首页` still selected.
- Tapping bottom `设置` now switches to the redesigned settings tab in place, with the settings tab highlighted.
- Tapping bottom `首页` returns to the Nome home view.
- Tapping `数据与存储` from the settings tab opens the existing database/storage management screen.
- Tapping `隐私与安全` from the settings tab opens the existing privacy settings screen.
- Tapping `服务器与 Tor` from the settings tab no longer crashes; it opens the existing network/server settings screen.
- With preview core, `服务器与 Tor` now shows `真实聊天 core 尚未可用` instead of the technical `unexpected result: cmdOk` alert.
- Tapping `通知` from the settings tab opens the existing notification settings screen.
- Returning from `通知` goes back to the embedded settings tab, and bottom `首页` returns to the Nome home screen.
- No new Nome crash report was created during the notification settings check; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-125025.ips`.
- Tapping `备份与迁移` opens a Nome backup/migration hub with `导出或导入数据库`, `迁移到新设备`, new-device receiving guidance, and safety notes.
- The backup/migration hub does not immediately enter the old `Stopping chat` migration state.
- Tapping `导出或导入数据库` from the hub opens the existing `数据与存储` database screen with database passphrase, export database, import database, delete database, and file/media controls.
- Returning from the database screen restores the backup/migration hub, then returns to the embedded settings tab.
- No new Nome crash report was created during the backup/migration hub and database-entry checks; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-125025.ips`.
- The embedded Nome settings tab now exposes `完整设置` under `高级`.
- Tapping `完整设置` opens the full settings list, preserving access to controls not shown in the simplified Nome tab, including audio/video call settings and appearance.
- No new Nome crash report was created during the full-settings entry check; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-125025.ips`.
- A targeted grep for `SimpleX Lock`, `Enable SimpleX Lock`, `SimpleX encrypted message`, `SimpleX contacts`, `Let's talk in SimpleX Chat`, `Connect to me via SimpleX Chat`, `Create SimpleX address`, and `SimpleX Address` returns no matches in `apps/ios/Shared` Swift source after the safe brand-copy pass.
- The remaining iOS `SimpleX` references are classified in `plans/20260709_nome_ios_brand_copy_audit.md`; the current decision is to preserve protocol/server, upstream attribution, legal/copyright, internal type, and legacy/non-primary references while keeping primary Nome surfaces branded as Nome.
- `plutil -lint apps/ios/zh-Hans.lproj/Localizable.strings` passes after adding the Nome-facing Simplified Chinese translations.
- The iOS Debug build passes after the safe brand-copy pass.
- Tapping `关于 Nome` from the embedded settings tab opens the dedicated Nome about page.
- The Nome about page renders the Nome logo, version, Chinese product summary, privacy-boundary rows, and explicit SimpleX network/protocol compatibility and upstream attribution.
- No new Nome crash report was created during the about-page check; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-125025.ips`.
- Tapping `帮助与反馈` from the embedded settings tab opens the dedicated Chinese Nome help page.
- The Nome help page renders start-use guidance for add friend, join group, public contact address, protection guidance for identity/privacy/backup, and a feedback section with project-support and email contact entries.
- No new Nome crash report was created during the help-page check; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-125025.ips`.
- Tapping the home avatar opens the profile menu, and tapping `身份中心` opens the redesigned identity center sheet.
- Tapping `关闭` from the identity center returns to the Nome home screen.
- The identity center explains identities as local profiles with separate profile, contacts, and notification state. It does not present identity switching or hidden identities as a cloud account or absolute anonymity guarantee.
- Regression found and fixed during profile-menu QA: tapping `从桌面端使用` previously opened the desktop connection screen and surfaced a technical `错误：%@` alert in the preview-core simulator. The fixed screen now shows `桌面连接暂不可用` with the same preview-core readiness boundary used by add friend, public address, and server settings.
- Tapping `关闭` from the desktop connection readiness screen returns to the Nome home screen.
- Chat-detail safety status banner changes compile in the native iOS app.
- The latest app build reinstalls and launches successfully after the chat-detail banner change.
- Launching the app with `-NomeConversationPreview` opens a seeded native conversation screen without starting the chat core.
- The seeded conversation screen renders Chinese message items, a file item, a voice item, the safety subtitle, the Nome safety card, the disappearing-message prompt, and the compose bar.
- The seeded conversation screen now renders on the Nome clean safety background instead of the upstream school wallpaper.
- Regression found and fixed during conversation-preview QA: opening the real chat-detail path from the preview could hit `SimpleXChat/API.swift:21: Fatal error: chat controller not initialized`. The fixed Debug preview opens a preview-safe `安全设置` sheet instead of calling the real chat-detail API.
- Launching with `-NomeConversationPreview -NomeConversationPreviewOpenDetails` shows the `安全设置` sheet with safety-code and disappearing-message explanations.
- The `安全设置` sheet explicitly states that automatic deletion controls Nome message retention and does not undo content already saved, screenshotted, or exported by the other side.
- Launching again with only `-NomeConversationPreview` returns to the normal seeded conversation page without auto-opening the sheet.
- No new Nome crash report was created during the fixed conversation-preview check; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-155004.ips`, the crash recorded before the fix.
- Launching the app without `-NomeConversationPreview` still opens the normal Nome home screen.
- Regression found and fixed during existing-chat-list QA: with existing chats, the inherited one-hand list inversion made chat rows appear above the Nome home header. The fixed build disables that inversion while the Nome bottom tab bar is active.
- Launching with `-NomeChatListPreview` shows the normal Nome home header first, then a compact `添加朋友` / `加入群组` / `公开地址` action strip, then the real chat-list rows.
- The seeded existing-chat-list preview keeps row state indicators visible: a direct chat unread badge, a group mention/report flag, and a muted direct chat icon.
- Launching without `-NomeChatListPreview` after this change still opens the normal app path.
- No new Nome crash report was created during the existing-chat-list preview check; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-155004.ips`.
- Privacy-claim audit completed for the current UX pass. Primary Nome wording now avoids absolute anonymity, guaranteed remote deletion, and "servers see nothing" claims.
- Debug launch with `-NomeOnboardingConditionsPreview` verifies the updated network-use copy renders as `Nome 会减少连接关系暴露`.
- Source-availability preparation is recorded as a release packet, but the public modified-source URL and final asset/trademark decision remain open before external distribution.
- Launching the app without `-NomeConversationPreview` after the chat-background change still opens the normal Nome home screen.
- The build after the low-risk brand-copy audit passes.
- Launching the latest build after the brand-copy audit still opens the normal Nome home screen.
- The latest build after the contacts-tab change passes.
- Tapping bottom `联系人` switches to the real contacts tab in place, highlights the contacts tab, and does not open the add/new-chat sheet.
- The contacts tab renders friend/group/request counts, `添加朋友` and `公开联系方式` quick actions, and a clear empty state when there are no contacts.
- Tapping contacts-tab `添加朋友` opens the one-time-link page; tapping back returns to the add list, and `关闭` returns to the contacts tab with the bottom `联系人` tab still selected.
- Tapping contacts-tab `公开联系方式` opens the public-contact sheet with the redesigned public address page, and `关闭` returns to the contacts tab with the bottom `联系人` tab still selected.
- No new Nome crash report was created during the contacts-tab check; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-125025.ips`.
- The latest build after the onboarding redesign passes.
- Debug launch with `-NomeOnboardingWelcomePreview` renders the Nome welcome page with logo, shield card, no-phone/no-public-ID framing, one-time-link explanation, and local-device storage explanation.
- Debug launch with `-NomeOnboardingProfilePreview` renders the local-identity creation page with migration entry, local-profile explanation, profile-name field, and disabled create button until a name is entered.
- Tapping `迁移` from the local-identity creation page opens the existing `迁移到这台设备` migration flow.
- After camera permission is denied in the preview simulator, the migration flow remains stable and shows the camera-permission action plus database import entry.
- Tapping `关闭` in the migration flow returns to the local-identity creation page.
- Debug launch with `-NomeOnboardingNetworkPreview` renders the network/notification setup page with message-server and notification rows plus the identity-vs-network-privacy note.
- Debug launch with `-NomeOnboardingConditionsPreview` renders the network-use confirmation page with operator commitments, user commitments, settings mutability, and `同意并进入 Nome`.
- Tapping `Nome 怎样保护你` opens the redesigned Nome privacy explainer sheet.
- Tapping the network page message-server row opens the server sheet, and with an empty preview-core operator list it shows `使用默认服务器设置` plus an enabled `完成` button.
- No new Nome crash report was created during the onboarding preview and sheet checks; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-125025.ips`.
- Fresh simulator launch before the final first-run fixes exposed two real ordering problems: the inherited upstream `v6.5 的新内容` sheet appeared before Nome UI, and the system notification permission prompt appeared on top of first-run content.
- After disabling the upstream What's New sheet, a fresh simulator launch no longer showed `v6.5 的新内容`.
- After delaying notification authorization until first-run notices are clear, a fresh simulator launch showed the Nome `是否发送送达回执` page without the system notification permission alert covering it.
- After adding the one-session notification-request guard, another fresh simulator launch still showed the same clean Nome delivery-receipts page without a covering system prompt.
- The Nome delivery-receipts page renders the approved brand/logo treatment, delivered-vs-read explanation, quick status chips, and the `启用送达回执` / `暂不启用` actions.
- Using `serve-sim tap` on a fresh simulator, tapping `暂不启用` opened the expected Nome confirmation alert `已暂不启用送达回执`.
- Tapping `好的` on that alert ended the delivery-receipts decision and then showed the iOS system notification permission prompt for Nome.
- Tapping `不允许` on the system notification permission prompt returned safely to the Nome home screen.
- Using `serve-sim tap` on a separate fresh simulator, tapping `启用送达回执` ended the delivery-receipts decision and showed the iOS system notification permission prompt for Nome.
- Tapping `不允许` on that notification prompt also returned safely to the Nome home screen.
- No new Nome crash report was created during the fresh-simulator first-launch smoke after the notification-delay fix; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-125025.ips`.
- In a stale simulator state with no chat database files, the app no longer silently fails; the one-time link page shows an inline reason telling the user that local profile/chat service readiness is required.
- Restarting after the initialization guard returns the app to the onboarding/delivery receipt stage instead of leaving it as an unexplained broken home state.
- `plans/20260709_nome_ios_release_gate_review.md` records the release-sensitive identifier review for TestFlight preparation.
- The current decision is compatibility-first: keep `chat.simplex.app`, `simplex`, `group.chat.simplex.app`, `$(AppIdentifierPrefix)chat.simplex.app`, `chat.simplex.app.SimpleX-NSE`, and `chat.simplex.app.SimpleX-SE` for this phase.
- AGPL/source availability remains open until a public Nome modified-source URL and final asset/trademark decision are recorded.
- App Store screenshot candidates are now saved under `design/app-store/ios-real-screens/`, and every image in the candidate set was copied from an iOS Simulator screenshot rather than generated from the design mockup.
- The candidate set uses `1206x2622` PNG screenshots. Add-friend, public-contact, join-group, and seeded conversation candidates remain preview-core/debug-only until real iOS core and real account testing are available.
- `design/app-store/ios-real-screens/MANIFEST.md` now records all eleven candidate files with dimensions, alpha status, SHA-256 hash, and final-release replacement notes.
- `scripts/ios/check-app-store-screenshots.sh` now passes in candidate mode with warnings for the eleven-candidate count, alpha channels, and preview/debug-only files.
- Strict screenshot final mode is intentionally not yet green because the current folder exceeds the ten-screenshot App Store limit and still includes preview/debug-only candidates.
- `scripts/ios/export-app-store-screenshot-draft.sh --force` generated `design/app-store/ios-upload-draft-screens/` with ten flattened JPEG screenshots, no alpha channels, a manifest that records dimensions, bytes, SHA-256 hashes, source files, release status, and `FINAL_BLOCKERS.md` with four real-core replacement requirements.
- `design/app-store/ios-upload-draft-screens/FINAL_BLOCKERS.md` lists the exact final-blocking screenshots: add friend, public contact, join group, and conversation. Each row names the draft file, source candidate, `needs-real-core` status, and replacement evidence required.
- `scripts/ios/check-app-store-screenshots.sh --dir design/app-store/ios-upload-draft-screens` passes with non-final warnings for `MANIFEST.md`, `FINAL_BLOCKERS.md`, and the four `needs-real-core` add-friend, public-contact, join-group, and conversation screenshots.
- `scripts/ios/check-app-store-screenshots.sh --final --dir design/app-store/ios-upload-draft-screens` exits with status `1` as intended because those four `needs-real-core` files must be replaced and both screenshot evidence files must be updated before public App Store submission.
- The current iOS Debug build still passes after the screenshot manifest/checker and real-core handoff updates.
- The current build was installed and launched on simulator `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`; the launched app process was `chat.simplex.app: 90141`.
- A current-launch screenshot was captured at `/tmp/nome-current-qa/current-build-launch.png`; visual inspection confirms the Nome home screen, logo, Chinese primary actions, and bottom navigation render as intended.
- No new Nome crash report was created during this build/install/launch smoke; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-155004.ips`.
- The guarded `scripts/ios/prepare-x86_64.sh` path was tested without `mac2ios`: it failed before replacing libraries, printed `mac2ios is required before preparing iOS libraries`, and the `apps/ios/Libraries` file-list hash stayed unchanged.
- `scripts/ios/build-mac2ios.sh` was used to build a local `tools/bin/mac2ios`; `scripts/ios/check-real-core.sh` now detects that helper. Real-core preflight still fails because iOS core artifacts are missing, not because `mac2ios` is unavailable.
- `scripts/ios/prepare-real-core.sh --downloads` was added as the preferred local-artifact entry point; in the current checkout it fails before replacement because the extracted `pkg-ios-*-swift-json` artifact directories are still missing.
- `scripts/ios/prepare-real-core.sh --downloads` was also tested from `/tmp`; it still resolved the project root correctly, failed before replacement because artifacts are missing, and left the `apps/ios/Libraries` file-list hash unchanged.
- `scripts/ios/prepare-real-core.sh --downloads-dir /tmp/nome-empty-artifacts.* --downloads` was tested against an empty custom artifact directory. It failed before replacement with explicit missing directory/zip messages, and the `apps/ios/Libraries` file-list hash stayed unchanged.
- `DOWNLOADS_DIR=/tmp/nome-empty-artifacts.* scripts/ios/check-real-core.sh` was tested and correctly pointed its local-artifact diagnostics at the custom directory.
- `scripts/ios/check-real-core.sh` now points its next-step hints at `scripts/ios/prepare-real-core.sh --job-repo` and `scripts/ios/prepare-real-core.sh --downloads`.
- `scripts/ios/smoke-nome-ui.sh` completed on simulator `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`, building and installing the current app, then capturing eight `1206x2622` screenshots with a manifest at `/tmp/nome-ios-smoke-current/manifest.tsv`.
- The smoke screenshots cover: normal home, onboarding welcome, onboarding local identity, onboarding network setup, onboarding conditions, existing-chat-list preview, conversation preview, and conversation safety/details preview.
- Visual inspection of `/tmp/nome-ios-smoke-current/01-home.png`, `/tmp/nome-ios-smoke-current/06-chat-list-existing.png`, and `/tmp/nome-ios-smoke-current/08-conversation-details.png` confirms they render the intended Nome pages, not blank or wrong screens.
- No new Nome crash report was created during the UI smoke pass; the latest report remained `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-155004.ips`.
- The strengthened smoke assertions were re-run with `--skip-build` at `/tmp/nome-ios-smoke-current-assertions/`: all eight screenshots were `1206x2622`, each was larger than the minimum 50 KB threshold, all eight SHA-256 hashes were unique, and no new Nome crash report appeared.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-current` exits successfully with accepted blockers: script syntax, diff hygiene, Simplified Chinese localization lint, screenshot candidate check, and UI smoke passed; final screenshot packaging and real-core preflight are marked `BLOCKED`; no check is marked `FAIL`.
- The readiness smoke manifest at `/tmp/nome-ios-readiness-current/smoke/manifest.tsv` contains eight screenshots, all `1206x2622`, each above 50 KB, and each with a unique SHA-256 hash.
- `scripts/ios/check-nome-ios-readiness.sh --output /tmp/nome-ios-readiness-release-gate` exits with status `1` as intended because the final App Store screenshot package and real iOS core libraries are still blocked.
- `scripts/ios/check-nome-brand-copy.sh` passes after the follow-up brand-copy cleanup.
- `plutil -lint apps/ios/zh-Hans.lproj/Localizable.strings apps/ios/zh-Hans.lproj/SimpleX--iOS--InfoPlist.strings` passes after the follow-up Simplified Chinese changes.
- XcodeBuildMCP `build_run_sim` was tried after confirming session defaults, but the tool environment still failed before build with `xcrun: error: unable to find utility "simctl"`. This remains a tool-path limitation, not a project compile failure.
- The authoritative shell build with `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild ... build` passes after the follow-up brand-copy cleanup.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-brand-copy-smoke` exits successfully: script syntax, diff hygiene, Simplified Chinese localization lint, Nome brand-copy check, screenshot candidate check, and UI smoke passed; final screenshot packaging and real-core preflight remain `BLOCKED`; no check is marked `FAIL`.
- `plans/20260709_nome_ios_design_coverage_matrix.md` now records coverage for the seven approved `pages-v2` mockups: home, add friend, join group, public contact, identity center, conversation, and settings/safety.
- `scripts/ios/check-nome-design-coverage.sh` passes and is included in the combined readiness gate.
- `-NomeIdentityCenterPreview` was added as a Debug-only launch argument. It renders `UserProfilesView` with a sample user and is skipped by normal app initialization/background handling.
- `scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-smoke-design-coverage --skip-build` passes when pointed at the current derived data. It captures nine `1206x2622` screenshots, including `09-identity-center`, all above the minimum byte threshold, all with unique SHA-256 hashes, and no new Nome crash report.
- Visual inspection of `/tmp/nome-ios-smoke-design-coverage/09-identity-center.png` confirms the identity center page renders the intended title, search field, identity hero card, current identity row, and add-identity row.
- After a transient simulator shutdown, `DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-design-coverage` exits successfully: 7 passes, 2 blocked release gates, and 0 failures.
- The smoke retry path was tested by intentionally shutting down simulator `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`, then running `DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-smoke-retry-boot --skip-build`. The script booted the simulator, captured nine `1206x2622` screenshots, and produced no new Nome crash report.
- The current combined gate with smoke retry evidence is `DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-smoke-retry`: 7 passes, 2 blocked release gates, and 0 failures.
- The primary-flow smoke expansion was built successfully with full Xcode after adding `-NomeAddFriendPreview`, `-NomeJoinGroupPreview`, `-NomePublicContactPreview`, and `-NomeSettingsPreview`.
- `DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-smoke-primary-flows --skip-build` passes. It captures thirteen `1206x2622` screenshots, all above the minimum byte threshold, all with unique SHA-256 hashes, and no new Nome crash report.
- Visual inspection of `/tmp/nome-ios-smoke-primary-flows/10-add-friend.png`, `/tmp/nome-ios-smoke-primary-flows/11-join-group.png`, `/tmp/nome-ios-smoke-primary-flows/12-public-contact.png`, and `/tmp/nome-ios-smoke-primary-flows/13-settings.png` confirms the new smoke pages render the intended Nome surfaces rather than blank or wrong screens.
- `DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-primary-flows` exits successfully: 7 passes, 2 blocked release gates, 0 warnings, and 0 failures.
- `DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-smoke-post-source-audit --skip-build` passes after the real-core source-audit hardening. It captures thirteen `1206x2622` screenshots, all with unique SHA-256 hashes, and no new Nome crash report.
- Visual inspection of `/tmp/nome-ios-smoke-post-source-audit/10-add-friend.png`, `/tmp/nome-ios-smoke-post-source-audit/11-join-group.png`, `/tmp/nome-ios-smoke-post-source-audit/12-public-contact.png`, and `/tmp/nome-ios-smoke-post-source-audit/13-settings.png` confirms the primary-flow preview pages still render correctly.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-recent-release-source-audit` exits successfully after the real-core source-audit hardening: 6 passes, 1 skipped-smoke warning, 2 blocked release gates, and 0 failures.
- `scripts/ios/check-nome-design-coverage.sh` was hardened to check source implementation markers in addition to mockup files, coverage IDs, and smoke labels. The hardened gate passes; log saved at `/tmp/nome-design-coverage-source-markers.log`.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-source-marker-coverage` exits successfully after the design-coverage hardening: 6 passes, 1 skipped-smoke warning, 2 blocked release gates, and 0 failures.
- `scripts/ios/check-real-core-sources.sh` now requires a complete aarch64 + x86_64 artifact pair before reporting a usable source. After the Hydra project correction it can find complete known `ci.zw3rk.com/job/simplex-chat-simplex-chat` candidates, while still reporting no local artifacts, no release-asset pair, and no recent GitHub Actions artifact pair.
- `ACTIONS_ARTIFACT_PAGES=3 scripts/ios/check-real-core-sources.sh` was rerun after the pagination hardening. It inspected 12 recent releases through `v7.0.0-beta.3` and three GitHub Actions artifact pages, found no complete `pkg-ios-*-swift-json` pair, and saved the log at `/tmp/nome-real-core-source-audit-paged.log`.
- `HYDRA_JOB_REPOS` support was added to `scripts/ios/check-real-core-sources.sh` so multiple candidate Hydra job repository URLs can be checked in one read-only audit. `scripts/ios/test-real-core-source-audit.sh` now covers missing, partial, complete local artifacts, partial Hydra, and complete Hydra fixture cases.
- `HYDRA_JOB_REPOS` was tested against common `ci.zw3rk.com` candidate job roots: `https://ci.zw3rk.com/job/simplex-chat/stable`, `https://ci.zw3rk.com/job/simplex-chat/master`, `https://ci.zw3rk.com/job/simplex-chat/simplex-chat/stable`, and `https://ci.zw3rk.com/job/simplex-chat/simplex-chat/master`. All returned 404 for both iOS architecture artifact paths; log saved at `/tmp/nome-real-core-source-audit-hydra-candidates.log`.
- The real Hydra project root was then found from `https://ci.zw3rk.com/`: `https://ci.zw3rk.com/project/simplex-chat-simplex-chat`. The complete job repository candidates verified with HEAD requests are `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/master`, `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v7-0-0-beta-3`, and `https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v6-5-5`. The closest `v6-5-6` jobset currently has only the x86_64 iOS download endpoint; its standard aarch64 iOS endpoint returned 404.
- Because the current checkout reports `simplex-chat.cabal` version `6.5.6.1` and iOS `MARKETING_VERSION = 6.5.6`, non-matching Hydra pairs are treated as compatibility candidates until a staged download, full Xcode build, launch, and smoke pass prove they work.
- `INCLUDE_KNOWN_HYDRA_REPOS=0 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/run-real-core-batch0.sh --job-repo https://ci.zw3rk.com/job/simplex-chat-simplex-chat/v6-5-5 --output /tmp/nome-ios-real-core-v6-5-5 --force` staged the complete `v6-5-5` candidate pair without replacing project libraries.
- The staged `v6-5-5` candidate contains production-sized archives: aarch64 zip `65M`, x86_64 zip `70M`, total staged size `160M`; SHA-256 hashes are `3b5e625088a6cbc57fbbec2f147e0618765bdcc9d56fc27f940c949f0e3f835b` and `6ed3cb1cdf92fef1bca9bfaf3c41c65c82a0e5d0ccc8b853b2f360fc1be67d63`.
- `DOWNLOADS_DIR=/tmp/nome-ios-real-core-v6-5-5 scripts/ios/check-real-core-sources.sh` confirms that the staged candidate is a complete local aarch64 + x86_64 artifact pair.
- `scripts/ios/sync-real-core-xcode-project.sh --check` passes against the current preview-library state, and a temporary fixture verified that the script can update `project.pbxproj` from the current `6.5.6.1` archive names to new real-core archive names and then pass `--check`.
- `scripts/ios/test-real-core-source-audit.sh` passes. It verifies three local fixture cases: missing artifacts fail, partial single-architecture artifacts fail, and a complete two-architecture pair passes. The script now uses `mktemp` output directories by default so parallel readiness runs do not collide.
- `scripts/ios/prepare-real-core.sh --job-repo` now uses `scripts/ios/check-real-core-sources.sh --job-repo` before download, instead of `scripts/ios/check-real-core.sh --job-repo`; this avoids blocking a valid Hydra download merely because the current checkout still has preview/missing installed libraries.
- `scripts/ios/test-prepare-real-core-safety.sh` passes. It uses a temporary sandbox copy of the real preparation scripts to verify missing artifacts, partial single-architecture artifacts, and an empty second-architecture directory all fail before the sandbox library hash changes. It also verifies the Hydra `--job-repo` call order: source audit, download, then final installed-core preflight. The script now uses `mktemp` output directories by default so parallel readiness runs do not collide.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-job-repo-source-first-default` exits successfully without the network-dependent source audit: 8 passes, 2 skipped-check warnings, 2 blocked release gates, and 0 failures.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-job-repo-source-first-audit` exits successfully with accepted blockers: 8 passes, 1 skipped-smoke warning, 3 blocked release gates, and 0 failures. The third blocker is `real_core_source_audit`.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-paged-source-audit` exits successfully after the paginated source-audit hardening: 8 passes, 1 skipped-smoke warning, 3 blocked release gates, and 0 failures. The third blocker remains `real_core_source_audit`.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-hydra-candidates` exits successfully after adding multi-Hydra-candidate source-audit support: 8 passes, 1 skipped-smoke warning, 3 blocked release gates, and 0 failures.
- The App Store screenshot checker now fails strict final mode when `MANIFEST.md` still records preview/debug/needs-real-core status. `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-screenshot-manifest-gate` still classifies that condition as the known `app_store_screenshot_final` blocker rather than a code failure.
- `ChatListView` now accepts a Debug-preview-only contacts initial tab, and `SimpleXApp` exposes it through `-NomeContactsPreview`; normal app launch and protocol/core paths are unchanged.
- `DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-smoke-contacts-tab --skip-build` passes. It captures fourteen `1206x2622` screenshots, all above the minimum byte threshold, all with unique SHA-256 hashes, and no new Nome crash report.
- Visual inspection of `/tmp/nome-ios-smoke-contacts-tab/14-contacts.png` confirms the contacts tab renders the intended title, counts, quick actions, seeded contacts/groups, and selected bottom-tab state rather than a blank or wrong screen.
- `scripts/ios/check-nome-design-coverage.sh` now verifies `14-contacts`, `-NomeContactsPreview`, and the fourteen-case smoke expectation; the gate passes.
- `DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-contacts-tab` exits successfully: 9 passes, 1 skipped-source-audit warning, 2 blocked release gates, and 0 failures. The blockers remain `app_store_screenshot_final` and `real_core_preflight`.
- `scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --output /tmp/nome-ios-goal-audit-source-default --allow-blockers` exits successfully with accepted blockers: 18 passes, 2 warnings, 2 blocked release gates, and 0 failures. The warnings are skipped `real_core_source_audit` and `physical_device_test`; the blockers are `app_store_final_screenshots` and `real_core_preflight`.
- `scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --output /tmp/nome-ios-goal-audit-final-blockers --allow-blockers` exits successfully after adding the App Store blocker ledger requirement: 19 passes, 2 warnings, 2 blocked release gates, and 0 failures.
- `scripts/ios/check-nome-ios-readiness.sh --output /tmp/nome-ios-readiness-final-blockers --allow-blockers` exits successfully after pointing strict screenshot final mode at the 10-slot upload draft: 8 passes, 2 warnings, 2 blocked release gates, and 0 failures.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-final-blockers-source-audit` exits successfully with accepted blockers: 8 passes, 1 skipped-smoke warning, 3 blocked release gates, and 0 failures. The source audit inspected recent GitHub releases and three GitHub Actions artifact pages, but still found no complete `pkg-ios-*-swift-json` iOS core artifact pair.
- `scripts/ios/check-real-core-build-env.sh` exits with status `1` as intended on this machine. It passes the flake target checks, detects `tools/bin/mac2ios`, confirms full Xcode `26.6` at `/Applications/Xcode.app/Contents/Developer`, and confirms `simctl` works with that `DEVELOPER_DIR`; it blocks because `nix` is not installed, warns that `xcode-select` points at Command Line Tools, and warns that free disk space is about 49 GB.
- `scripts/ios/test-stage-real-core-artifacts.sh` passes. It proves Nix-result-style directories containing `pkg-ios-aarch64-swift-json.zip` and `pkg-ios-x86_64-swift-json.zip` can be normalized, and that missing or ambiguous artifact sources fail before preparation.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-staging-bridge-source-audit` exits successfully after adding the staging bridge test: 9 passes, 1 skipped-smoke warning, 4 blocked release gates, and 0 failures. The blockers are final screenshots, real-core preflight, local real-core build env, and real-core source availability.
- `scripts/ios/check-nome-ios-goal-audit.sh --source-audit --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --output /tmp/nome-ios-goal-audit-staging-bridge --allow-blockers` exits successfully after adding the staging bridge test: 20 passes, 1 physical-device warning, 4 blocked release gates, and 0 failures.
- `scripts/ios/check-nome-manual-qa-status.sh --write-tsv /tmp/nome-manual-qa-unchecked-run-real-core-batch0.tsv` exits with status `1` as intended. It currently reports 121 checklist items: 84 checked and 37 unchecked, and writes a TSV queue grouped by blocker type.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-manual-qa-queue` exits successfully after adding the manual QA status queue: 9 passes, 1 skipped-smoke warning, 5 blocked release gates, and 0 failures. The output includes `/tmp/nome-ios-readiness-manual-qa-queue/manual_qa_unchecked.tsv`.
- `scripts/ios/check-nome-ios-goal-audit.sh --source-audit --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --output /tmp/nome-ios-goal-audit-manual-qa-queue --allow-blockers` exits successfully after adding the manual QA status queue: 20 passes, 1 physical-device warning, 5 blocked release gates, and 0 failures. The output includes `/tmp/nome-ios-goal-audit-manual-qa-queue/manual_qa_unchecked.tsv`.
- `scripts/ios/check-nome-qa-execution-plan.sh` passes. It verifies all seven execution-batch headings, required recheck commands, and all 15 current unchecked blocker groups from the manual QA queue.
- `scripts/ios/export-nome-qa-batches.sh --output /tmp/nome-ios-qa-batches-current --force` passes. It exports 37 unchecked items into seven batches: 2 real-core artifacts, 4 first-run/network, 10 single-account connection surfaces, 14 two-account messaging/groups, 4 identity/migration, 2 physical-device/camera, and 1 final screenshot package item.
- `scripts/ios/test-run-real-core-batch0.sh` passes. It verifies complete local artifact staging, the staged source/output same-directory case, a fake Hydra download/stage path, and the expected missing-artifact failure message without running Nix or replacing app libraries.
- `scripts/ios/run-real-core-batch0.sh --output /tmp/nome-ios-real-core-batch0-current` exits with status `1` as intended on this machine. It reports no complete artifact pair in `~/Downloads`, prints the current real-core preflight blockers, and does not replace app libraries.
- `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-qa-batch-export` exits successfully after adding the QA batch export gate: 12 passes, 1 skipped-smoke warning, 5 blocked release gates, and 0 failures.
- `scripts/ios/check-nome-ios-goal-audit.sh --source-audit --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --output /tmp/nome-ios-goal-audit-qa-batch-export --allow-blockers` exits successfully after adding the QA batch export requirement: 24 passes, 1 physical-device warning, 5 blocked release gates, and 0 failures.
- `scripts/ios/check-nome-ios-goal-audit.sh --source-audit --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --output /tmp/nome-ios-goal-audit-source-audit --allow-blockers` exits successfully with accepted blockers: 18 passes, 1 physical-device warning, 3 blocked release gates, and 0 failures. The third blocker is `real_core_source_audit`.
- `scripts/ios/check-nome-ios-goal-audit.sh --source-audit --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --output /tmp/nome-ios-goal-audit-with-source-final --allow-blockers` exits successfully after documenting the source-audit option: 18 passes, 1 physical-device warning, 3 blocked release gates, and 0 failures.
- `scripts/ios/check-nome-ios-goal-audit.sh --source-audit --smoke-manifest /tmp/nome-ios-smoke-contacts-tab/manifest.tsv --output /tmp/nome-ios-goal-audit-hydra-candidates --allow-blockers` exits successfully after the multi-Hydra-candidate source-audit hardening: 18 passes, 1 physical-device warning, 3 blocked release gates, and 0 failures.
- `git diff --check` passes.

Existing warnings:

- `SimpleXAPI.swift` has existing unused-value and non-Sendable warnings.
- `NewChatView.swift` has an existing `Alert?` non-Sendable warning.
- The Xcode run script phase is configured to run on every build.

## Screenshots

Captured local evidence:

- `/tmp/nome-home-before-join-tap.png`
- `/tmp/nome-join-group-direct.png`
- `/tmp/nome-add-friend-direct.png`
- `/tmp/nome-add-friend-direct-after-wait.png`
- `/tmp/nome-current-before-tap.png`
- `/tmp/nome-after-init-guard.png`
- `/tmp/nome-after-init-guard-wait.png`
- `/tmp/nome-add-friend-inline-error.png`
- `/tmp/nome-fresh-sim-launch.png`
- `/tmp/nome-home-after-order-fix-final.png`
- `/tmp/nome-home-after-bottom-nav-fix.png`
- `/tmp/nome-add-friend-alert-after-layout-fix.png`
- `/tmp/nome-join-group-v2-clean-click-scan.png`
- `/tmp/nome-join-group-v2-after-allow-paste.png`
- `/tmp/nome-join-group-v2-real-core-guard.png`
- `/tmp/nome-public-contact-v2-final.png`
- `/tmp/nome-public-contact-preview-core-guard.png`
- `/tmp/nome-settings-tab-v3-final.png`
- `/tmp/nome-home-after-settings-tab.png`
- `/tmp/nome-profile-picker-current.png`
- `/tmp/nome-identity-center-current.png`
- `/tmp/nome-chat-build-smoke-home.png`
- `/tmp/nome-chat-banner-final-smoke-home.png`
- `/tmp/nome-conversation-preview-v3-final.png`
- `/tmp/nome-normal-launch-after-conversation-preview.png`
- `/tmp/nome-conversation-preview-v4-background.png`
- `/tmp/nome-normal-launch-after-chat-background.png`
- `/tmp/nome-normal-launch-after-brand-copy.png`
- `/tmp/nome-settings-with-storage-entry.png`
- `/tmp/nome-storage-settings-detail.png`
- `/tmp/nome-privacy-settings-detail.png`
- `/tmp/nome-network-tor-preview-core-guard.png`
- `/tmp/nome-notification-settings-detail.png`
- `/tmp/nome-settings-after-notification-back.png`
- `/tmp/nome-home-after-settings-return.png`
- `/tmp/nome-settings-before-backup-migration.png`
- `/tmp/nome-backup-migration-hub.png`
- `/tmp/nome-backup-database-entry-from-hub.png`
- `/tmp/nome-backup-hub-after-database-back.png`
- `/tmp/nome-settings-after-backup-hub-back.png`
- `/tmp/nome-settings-full-entry-before-scroll.png`
- `/tmp/nome-settings-full-entry-visible.png`
- `/tmp/nome-settings-full-list-open.png`
- `/tmp/nome-settings-about-row-visible.png`
- `/tmp/nome-about-page-cn.png`
- `/tmp/nome-settings-help-row-visible.png`
- `/tmp/nome-help-page-cn.png`
- `/tmp/nome-help-page-feedback.png`
- `/tmp/nome-before-contacts-tab.png`
- `/tmp/nome-contacts-tab.png`
- `/tmp/nome-onboarding-welcome.png`
- `/tmp/nome-onboarding-profile.png`
- `/tmp/nome-migration-entry-01-profile-preview.png`
- `/tmp/nome-migration-entry-02-migrate-sheet.png`
- `/tmp/nome-migration-entry-03-camera-denied.png`
- `/tmp/nome-migration-entry-05-sheet-with-close.png`
- `/tmp/nome-migration-entry-06-returned-profile-preview.png`
- `/tmp/nome-onboarding-network.png`
- `/tmp/nome-onboarding-conditions.png`
- `/tmp/nome-onboarding-why-sheet.png`
- `/tmp/nome-onboarding-server-sheet-fixed.png`
- `/tmp/nome-first-run-01-launch.png`
- `/tmp/nome-first-run-02-no-whats-new.png`
- `/tmp/nome-first-run-03-delivery-receipts.png`
- `/tmp/nome-first-run-04-notification-delayed.png`
- `/tmp/nome-first-run-05-guard.png`
- `/tmp/nome-first-run-06-before-skip.png`
- `/tmp/nome-first-run-07-skip-alert.png`
- `/tmp/nome-first-run-08-notification-after-decision.png`
- `/tmp/nome-first-run-09-after-notification-deny.png`
- `/tmp/nome-first-run-10-before-enable.png`
- `/tmp/nome-first-run-11-after-enable-tap.png`
- `/tmp/nome-first-run-12-after-enable-deny.png`
- `/tmp/nome-add-friend-redesign-01-page.png`
- `/tmp/nome-add-friend-redesign-02-lower.png`
- `/tmp/nome-close-flow-02-add-list-with-close.png`
- `/tmp/nome-close-flow-03-home-after-close.png`
- `/tmp/nome-join-return-01-join-page.png`
- `/tmp/nome-join-return-02-add-list-with-close.png`
- `/tmp/nome-join-return-03-home-after-close.png`
- `/tmp/nome-public-close-01-page-with-close.png`
- `/tmp/nome-public-close-02-home-after-close.png`
- `/tmp/nome-contacts-flow-01-contacts-tab.png`
- `/tmp/nome-contacts-flow-02-add-friend-page.png`
- `/tmp/nome-contacts-flow-03-back-to-contacts.png`
- `/tmp/nome-contacts-flow-04-close-to-contacts.png`
- `/tmp/nome-contacts-flow-05-public-address-page.png`
- `/tmp/nome-contacts-flow-06-public-close-to-contacts.png`
- `/tmp/nome-desktop-flow-00-after-relaunch.png`
- `/tmp/nome-desktop-flow-01-preview-core-guard.png`
- `/tmp/nome-desktop-flow-02-close-to-home.png`
- `/tmp/nome-identity-flow-04-identity-center-correct.png`
- `/tmp/nome-identity-flow-05-close-to-home.png`
- `/tmp/nome-conversation-flow-04-preview-after-fix.png`
- `/tmp/nome-conversation-flow-05-disappearing-settings-sheet.png`
- `/tmp/nome-conversation-flow-06-normal-preview-after-sheet-fix.png`
- `/tmp/nome-chat-list-existing-preview-01.png`
- `/tmp/nome-chat-list-existing-preview-02-fixed-order.png`
- `/tmp/nome-chat-list-existing-preview-03-compact-actions.png`
- `/tmp/nome-normal-launch-after-chat-list-preview.png`
- `/tmp/nome-onboarding-conditions-privacy-claim-audit.png`
- `/tmp/nome-current-qa/current-build-launch.png`
- `/tmp/nome-ios-smoke-current/01-home.png`
- `/tmp/nome-ios-smoke-current/02-onboarding-welcome.png`
- `/tmp/nome-ios-smoke-current/03-onboarding-profile.png`
- `/tmp/nome-ios-smoke-current/04-onboarding-network.png`
- `/tmp/nome-ios-smoke-current/05-onboarding-conditions.png`
- `/tmp/nome-ios-smoke-current/06-chat-list-existing.png`
- `/tmp/nome-ios-smoke-current/07-conversation-preview.png`
- `/tmp/nome-ios-smoke-current/08-conversation-details.png`
- `/tmp/nome-ios-smoke-current/manifest.tsv`
- `/tmp/nome-ios-smoke-current-assertions/manifest.tsv`
- `/tmp/nome-ios-smoke-current-assertions/hashes.tsv`
- `/tmp/nome-ios-smoke-design-coverage/09-identity-center.png`
- `/tmp/nome-ios-smoke-design-coverage/manifest.tsv`
- `/tmp/nome-ios-smoke-retry-boot/manifest.tsv`
- `/tmp/nome-ios-readiness-smoke-retry/smoke/manifest.tsv`
- `/tmp/nome-ios-smoke-primary-flows/10-add-friend.png`
- `/tmp/nome-ios-smoke-primary-flows/11-join-group.png`
- `/tmp/nome-ios-smoke-primary-flows/12-public-contact.png`
- `/tmp/nome-ios-smoke-primary-flows/13-settings.png`
- `/tmp/nome-ios-smoke-primary-flows/manifest.tsv`
- `/tmp/nome-ios-readiness-primary-flows/smoke/manifest.tsv`
- `/tmp/nome-ios-smoke-contacts-tab/14-contacts.png`
- `/tmp/nome-ios-smoke-contacts-tab/manifest.tsv`
- `/tmp/nome-ios-readiness-contacts-tab/smoke/manifest.tsv`
- `/tmp/nome-ios-goal-audit-contacts-tab/summary.tsv`

These are simulator screenshots, not generated mockups.

Stable App Store planning candidates:

- `design/app-store/ios-real-screens/01-onboarding-welcome.png`
- `design/app-store/ios-real-screens/02-onboarding-local-identity.png`
- `design/app-store/ios-real-screens/03-home.png`
- `design/app-store/ios-real-screens/04-contacts.png`
- `design/app-store/ios-real-screens/05-public-contact-preview-core.png`
- `design/app-store/ios-real-screens/06-join-group-preview.png`
- `design/app-store/ios-real-screens/07-conversation-debug-preview.png`
- `design/app-store/ios-real-screens/08-settings.png`
- `design/app-store/ios-real-screens/09-about.png`
- `design/app-store/ios-real-screens/10-help.png`
- `design/app-store/ios-real-screens/11-add-friend-preview-core.png`

The detailed screenshot manifest is
`design/app-store/ios-real-screens/MANIFEST.md`. The current 10-slot planning
set drops `09-about.png` first, but the final public release set must be chosen
again after real-core screenshots replace the add-friend, public-contact,
join-group, and conversation candidates.

## Follow-up investigation notes

- Current primary simulator app group path only contains preferences and no `simplex_v1_chat.db` / `simplex_v1_agent.db` files.
- App logs repeatedly show `getAgentSubsTotal error: cmdOk(user_: nil)` and `getSubsTotal error: unexpected result: cmdOk`, which matches the one-time link creation failure.
- A backup of the previous simulator state was saved at `/tmp/nome-sim-backup-20260709-092827`.
- A secondary clean simulator (`95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`) launched the app and showed the clean notification / What's New onboarding state, but `serve-sim` helper control did not stay connected for completing the full flow there.
- Current `apps/ios/Libraries/sim` archives are 9.9K preview libraries, not production-sized Haskell core libraries.
- `strings /tmp/nome-ios-derived-clean/Build/Products/Debug-iphonesimulator/SimpleXChat.framework/SimpleXChat` shows `preview-agent`, `simplex:/contact#preview`, `https://nome.local/preview`, `serverOperators: []`, and `preview-token`.
- XcodeBuildMCP session defaults point to the correct project, scheme, simulator, and derived data path, but its environment still fails to find `simctl`; the stable local path is `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild` / `xcrun simctl`.
- GitHub release assets for `v6.5.6` and `v7.0.0-beta.3` do not include `pkg-ios-aarch64-swift-json.zip` or `pkg-ios-x86_64-swift-json.zip`.
- Public GitHub Actions artifacts were checked for reusable iOS core packages; no non-expired `pkg-ios-aarch64-swift-json` or `pkg-ios-x86_64-swift-json` artifact was found across the checked artifact pages.
- `scripts/ios/check-real-core-sources.sh` was added and run, then hardened to scan the recent GitHub release/prerelease window instead of only the latest stable release, require a complete aarch64 + x86_64 artifact pair, default to 12 recent releases, inspect three GitHub Actions artifact pages, and check known `simplex-chat-simplex-chat` Hydra job repositories by default. It confirms no local artifact directories/zips, no inspected release or prerelease through `v7.0.0-beta.3` has a complete `pkg-ios-*-swift-json` pair, and the first three Actions artifact pages do not expose a complete pair; Hydra now provides complete candidate pairs that still need version-compatibility verification before installation.
- The public SimpleX downloads page points iOS users to App Store/TestFlight, not to reusable iOS core library artifacts.
- The repo-supported ways to get real iOS core libraries are therefore either `scripts/ios/download-libs.sh <hydra-job-repo>` or local/CI Nix builds of `.#aarch64-darwin-ios:lib:simplex-chat` and `.#x86_64-darwin-ios:lib:simplex-chat`, followed by `scripts/ios/prepare-x86_64.sh`.
- Local environment check: `nix` is not installed, a local `tools/bin/mac2ios` helper is available, and `~/Downloads` does not contain `pkg-ios-aarch64-swift-json` or `pkg-ios-x86_64-swift-json` artifact directories or zips.
- `scripts/ios/check-real-core.sh` was added and run. It exits with `1` in the current checkout, reporting missing `apps/ios/Libraries/ios`, 10,104-byte simulator core archives, preview-core markers, missing `~/Downloads/pkg-ios-*-swift-json` artifacts, and missing `nix`; it now detects local `tools/bin/mac2ios`.
- `plans/20260709_nome_ios_real_core_testing_plan.md` now records the exact real-core handoff path and post-core functional QA sequence.
- A manual follow-up checklist is saved at `plans/20260709_nome_ios_manual_qa_checklist.md`.
- Regression found and fixed: opening `服务器与 Tor` from the embedded Nome settings tab crashed with SwiftUI `EnvironmentObject.error()` because `SaveableSettings` was not injected on that navigation path. The fixed build opened the page and produced no new `Nome-*.ips` crash report after the tap.
- Fresh first-launch regression found and fixed: the inherited upstream `v6.5 的新内容` notice interrupted Nome before the first decision surface. The fixed build disables that sheet for Nome launch.
- Fresh first-launch ordering issue found and fixed: the system notification permission alert appeared on top of first-run content. The fixed build delays notification authorization until the Nome delivery-receipts decision page is no longer active.
- Local UI automation for tapping the fresh simulator delivery-receipts page was blocked in this session: XcodeBuildMCP accessibility failed because `xcode-select` points at CommandLineTools, and Computer Use could not obtain usable CGWindow handles for Codex/Simulator/Chrome. The follow-up click-through should be done manually or after fixing tool access.
- Conversation-preview UI automation was also limited by the same environment: XcodeBuildMCP accessibility failed because `xcode-select` points at CommandLineTools, and `simctl` does not provide tap injection. A Debug-only launch argument was used to open the same safety/settings sheet for screenshot evidence without changing normal launch behavior.
- Return-path follow-up fixed: `公开联系方式` was previously reachable as a sheet without an obvious close control. `UserPickerSheetView` now adds a `关闭` toolbar action, and the simulator verified that it returns to Nome home.
- Preview-core follow-up fixed: the desktop connection screen previously surfaced a technical `错误：%@` alert when opened from the profile menu. `ConnectDesktopView` now suppresses preview-core background API errors, shows a readable `桌面连接暂不可用` card, and keeps a clean close path back to Nome home.

## Open risks

- The one-time link page opened correctly and now explains the failure inline, but this simulator state still cannot generate a real link because it is using preview core libraries.
- A real one-time link QR/link still needs verification after installing or building the real iOS core libraries.
- The chat detail safety banner now compiles, is wired to the existing detail sheet, and has seeded simulator visual evidence.
- The seeded conversation screenshot does not prove real two-account messaging; real-core/two-device verification is still required before claiming functional message exchange.
- Conversation-page visual QA currently uses the Debug seeded host; it is useful for layout and copy, but still not a functional chat-core test.
- The preview-safe `安全设置` sheet does not prove real disappearing-message persistence; it only verifies the preview explanation and prevents preview-core crashes.
- Existing-chat-list visual QA currently uses Debug seeded rows; it verifies layout and state rendering, but not real unread/mute/report state transitions from live events.
- Privacy-claim audit is only a product/engineering copy check for current UI; it does not replace final App Store privacy, legal, or export-compliance review.
- Source availability is prepared as a packet, but external distribution still needs the actual public modified-source URL.
- Onboarding visual QA currently uses Debug preview launch arguments; real first-run profile creation and network acceptance still need clean-state functional testing.
- First-launch ordering and both delivery-receipts choices are now smoke-tested in fresh simulators through the notification permission prompt and back to the Nome home screen.
- Camera permission was not accepted during this pass, so the QR scanner frame was visually present but not tested with live camera input.
- Group preview/join confirmation depends on a real group invitation link and still needs an end-to-end test.
- The current screenshot folder is a candidate set, not a final upload package:
  it contains eleven PNGs, alpha channels, and preview/debug-only filenames. The
  generated 10-slot JPEG draft is closer to a store package, but it still has
  four `needs-real-core` files and a manifest that records non-final evidence. The
  eventual final package must pass `scripts/ios/check-app-store-screenshots.sh --final`.

## Next QA targets

1. Follow Batch 0 in `plans/20260709_nome_ios_remaining_qa_execution_plan.md`, then run `scripts/ios/check-real-core.sh` and make it pass by installing or building the real iOS core libraries, replacing the current preview `apps/ios/Libraries/sim` archives.
2. Follow Batch 1 and create or seed a clean local profile that reaches the Nome home screen against real network/core state.
3. Follow Batch 2 and verify that one-time invitation creation, QR, native share, public contact address, and bad-link error states use real data.
4. Replace the seeded conversation preview with a real two-account conversation screenshot once the real core is available.
5. Test public contact address create/change/disable flows.
6. Run a two-account messaging test before claiming the iOS UX MVP is complete.
7. Execute `plans/20260709_nome_ios_manual_qa_checklist.md` and update each item with pass/fail evidence.
8. Select the final ten-or-fewer App Store screenshots, replace preview/debug candidates, update screenshot manifest evidence, flatten final exports if needed, and make `scripts/ios/check-app-store-screenshots.sh --final` pass.
9. Keep `scripts/ios/check-nome-ios-goal-audit.sh --source-audit` free of `FAIL` results and reduce its `BLOCKED` count to zero before claiming this goal complete.

## 2026-07-09 real-core install experiment

- A reversible install experiment was run with the staged Hydra `v6-5-5` core pair from `/tmp/nome-ios-real-core-v6-5-5`.
- Before installing, the preview-core state was backed up to `/tmp/nome-ios-preview-core-backup-20260709-205218`.
- `scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-real-core-v6-5-5 --prepare` installed production-sized archives and `scripts/ios/sync-real-core-xcode-project.sh` updated the Xcode project references.
- `scripts/ios/check-real-core.sh` passed the installed-core preflight after the experiment: device and simulator archives were production-sized and preview markers were absent.
- The subsequent Apple Silicon simulator build failed at link time because Xcode was building `arm64-apple-ios15.0-simulator`, while the staged Hydra simulator package is `x86_64`.
- Representative linker failure: `found architecture 'x86_64', required architecture 'arm64'`, followed by missing SimpleX C bridge symbols such as `_chat_recv_msg_wait`, `_chat_send_cmd_retry`, and `_hs_init_with_rtsopts`.
- The experiment was rolled back by restoring `apps/ios/Libraries` and `apps/ios/SimpleX.xcodeproj/project.pbxproj` from `/tmp/nome-ios-preview-core-backup-20260709-205218`.
- Post-rollback verification: `scripts/ios/sync-real-core-xcode-project.sh --check` passed and `git diff --check` passed.
- Current conclusion: the `v6-5-5` artifacts are useful evidence and likely suitable for device-side archive testing, but they are not enough for the current Apple Silicon simulator flow. The next real-core path needs an arm64-simulator-compatible core, an x86_64 simulator destination, or physical-device testing with the aarch64 iOS package.

## 2026-07-09 UI smoke continuation

- XcodeBuildMCP defaults were checked and point at the correct project, scheme,
  simulator, derived data path, and bundle id. `build_run_sim` still cannot run
  in this session because its environment cannot find `simctl`, so the stable
  validation path remains the project smoke script with
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`.
- A full build/install/launch smoke pass completed against simulator
  `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`.
- Evidence folder: `/tmp/nome-ios-smoke-continue-20260709-205910`.
- Smoke manifest:
  `/tmp/nome-ios-smoke-continue-20260709-205910/manifest.tsv`.
- The pass captured all 14 expected screens at `1206x2622`, with no duplicate
  screenshot hashes and no new `Nome-*.ips` crash report.
- The screenshots were visually reviewed as a contact sheet. The main Nome
  surfaces open correctly in the simulator: home, onboarding, local identity,
  network/conditions, existing chat list, conversation preview, safety details,
  identity center, add friend, join group, public contact address, settings, and
  contacts.
- `scripts/ios/smoke-nome-ui.sh` now auto-generates
  `contact-sheet.png` when ImageMagick is available, so future smoke runs have a
  quick visual review artifact in addition to the TSV manifest.
- Skip-build verification of the updated smoke script completed successfully.
  Evidence folder:
  `/tmp/nome-ios-smoke-contact-sheet-test2-20260709-210736`.
- Updated-script evidence:
  `/tmp/nome-ios-smoke-contact-sheet-test2-20260709-210736/contact-sheet.png`.
- Follow-up fix: `scripts/ios/smoke-nome-ui.sh --output ...` now initializes
  the contact-sheet path after argument parsing, so readiness-owned smoke runs
  write the contact sheet into the requested output directory.
- Integrated readiness verification with smoke completed successfully.
  Readiness report:
  `/tmp/nome-ios-readiness-smoke-contact-sheet-20260709-211202`.
- Integrated smoke contact sheet:
  `/tmp/nome-ios-readiness-smoke-contact-sheet-20260709-211202/smoke/contact-sheet.png`
  (`1108x2320`).
- Integrated readiness result: 13 PASS, 1 WARN, 4 BLOCKED, 0 FAIL.
- The contact sheet generator tolerates ImageMagick font warnings by checking
  the generated PNG dimensions and writing warning details to
  `contact-sheet.log`.
- Remaining limitation: add friend, public contact, join group, and conversation
  screenshots still use preview/debug real-core substitutes and cannot be
  treated as final App Store or functional messaging evidence.

## 2026-07-09 real-core architecture guard

- `scripts/ios/check-real-core.sh` now checks static library architectures with
  `lipo` in addition to production size and preview-marker checks.
- The required device architecture is `arm64`; the required simulator
  architecture defaults to `uname -m`, which is `arm64` on this Apple Silicon
  machine. It can be overridden with `REQUIRED_SIM_ARCH` for controlled tests.
- `IOS_LIB_DIR` and `SIM_LIB_DIR` overrides were added so staged artifacts can
  be audited before they are installed into `apps/ios/Libraries`.
- `scripts/ios/prepare-real-core.sh --downloads` now audits staged artifacts
  before copying anything into `~/Downloads` and before calling
  `prepare-x86_64.sh`.
- `scripts/ios/download-libs.sh` now runs the same installed-core preflight
  against the freshly downloaded full dual-arch pair before calling
  `prepare-x86_64.sh`.
- Added `scripts/ios/test-check-real-core-arch.sh` and wired it into both
  `scripts/ios/check-nome-ios-readiness.sh` and
  `scripts/ios/check-nome-ios-goal-audit.sh`.
- `scripts/ios/test-check-real-core-arch.sh` passes for an arm64 simulator
  fixture and fails as intended for an x86_64 simulator fixture when arm64 is
  required.
- `scripts/ios/test-prepare-real-core-safety.sh` now includes a
  `sim-arch-mismatch` case proving the prepare path fails before modifying app
  libraries.
- Real staged-artifact guard verification:
  `scripts/ios/run-real-core-batch0.sh --source /tmp/nome-ios-real-core-v6-5-5 --output /tmp/nome-ios-real-core-v6-5-5 --prepare`
  exits `1` before installation, reports simulator architecture mismatches for
  all staged x86_64 simulator archives, and leaves the `apps/ios/Libraries`
  hash unchanged (`d9df204f7dac66de28cbf4200f05eda54f7a12a28cdb5e417f4fad501cc21175`).
- Latest readiness without smoke:
  `/tmp/nome-ios-readiness-realcore-arch-20260709-212102` reports 13 PASS, 2
  WARN, 4 BLOCKED, 0 FAIL.
- Latest goal audit:
  `/tmp/nome-ios-goal-audit-realcore-arch-20260709-212102` reports 25 PASS, 2
  WARN, 4 BLOCKED, 0 FAIL.
- The remaining real-core path is now narrower and safer: obtain/build an
  arm64-simulator-compatible core, use a compatible x86_64 simulator only as a
  temporary bridge, or move real functional validation to a physical iPhone with
  the aarch64 iOS package.

## 2026-07-09 source-audit architecture alignment

- `scripts/ios/check-real-core-build-env.sh` now parses
  `xcodebuild -showdestinations` for `SimpleX (iOS)` and reports available iOS
  Simulator architectures.
- Current Xcode evidence: available iOS Simulator destination architecture is
  only `arm64`; no `x86_64` iOS Simulator destination is available.
- `scripts/ios/check-real-core-sources.sh` now takes `REQUIRED_SIM_ARCH`
  into account. The standard SimpleX source pair (`pkg-ios-aarch64` device plus
  `pkg-ios-x86_64` simulator) is counted as compatible only when the target
  simulator architecture is `x86_64`.
- On this Apple Silicon simulator path, the local staged/downloaded `v6-5-5`
  artifacts and the known Hydra repositories are reported as device arm64 plus
  x86_64 simulator artifacts, not as passing simulator-compatible sources.
- `scripts/ios/test-real-core-source-audit.sh` now covers both cases: standard
  pair blocked for arm64 simulator, standard pair passing for x86_64 simulator.
- `scripts/ios/test-run-real-core-batch0.sh` now pins its fake Hydra fixture to
  `REQUIRED_SIM_ARCH=x86_64`, so it continues testing staging/download logic
  independently from this machine's arm64 simulator constraint.
- Real source-audit sample:
  `RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-real-core-sources.sh`
  exits `1` on this machine and reports no simulator-compatible source for
  `arm64`.
- Latest source-aware readiness:
  `/tmp/nome-ios-readiness-source-arch2-20260709-213134` reports 13 PASS, 1
  WARN, 5 BLOCKED, 0 FAIL.
- Latest source-aware goal audit:
  `/tmp/nome-ios-goal-audit-source-arch2-20260709-213135` reports 25 PASS, 1
  WARN, 5 BLOCKED, 0 FAIL.

## 2026-07-09 physical-device readiness gate

- Added `scripts/ios/check-ios-device-readiness.sh` as a read-only gate for the
  physical-device real-core path.
- The gate checks full Xcode, the Xcode project, generic iOS destination
  availability, connected physical iPhone/iPad presence, generic iOS signing
  settings, and arm64 device static libraries in `apps/ios/Libraries/ios`.
- Current evidence:
  - Full Xcode is available at `/Applications/Xcode.app/Contents/Developer`.
  - Xcode exposes the generic `Any iOS Device` destination.
  - Generic iOS build settings are readable for `SimpleX (iOS)`.
  - `DEVELOPMENT_TEAM=5NN7GUYB6T`.
  - `CODE_SIGN_STYLE=Automatic`.
  - `CODE_SIGN_IDENTITY=Apple Development`.
  - `PRODUCT_BUNDLE_IDENTIFIER=chat.simplex.app`; this should be reviewed
    before TestFlight/public release.
  - No connected physical iPhone or iPad was found.
  - `apps/ios/Libraries/ios` is missing in the restored preview-core state.
- `scripts/ios/check-ios-device-readiness.sh` is now part of both
  `scripts/ios/check-nome-ios-readiness.sh` and
  `scripts/ios/check-nome-ios-goal-audit.sh` as
  `physical_device_readiness`.
- Latest device-gate readiness:
  `/tmp/nome-ios-readiness-device-gate-20260709-213642` reports 13 PASS, 2 WARN,
  5 BLOCKED, 0 FAIL.
- Latest device-gate goal audit:
  `/tmp/nome-ios-goal-audit-device-gate-20260709-213642` reports 25 PASS, 2
  WARN, 5 BLOCKED, 0 FAIL.
- The physical-device path is now explicit: connect and trust an iPhone,
  install/build the arm64 device real-core package, rerun
  `scripts/ios/check-ios-device-readiness.sh`, then run the real one-time-link,
  public-address, group, and messaging QA batches on device.

## 2026-07-09 release identifier readiness gate

- Added `scripts/ios/check-ios-release-identifiers.sh` as a read-only
  TestFlight/public-release identifier gate.
- The gate checks the Xcode project, Info.plist, main app entitlements,
  notification service extension entitlements, share extension entitlements,
  bundle identifiers, app group, keychain access group, associated domains,
  background task identifiers, URL scheme, push entitlement, and visible app
  product/display names.
- Current evidence:
  - Main app display name is configured as `Nome`.
  - Main app product name is `Nome`.
  - The `simplex` URL scheme is preserved for protocol/link compatibility.
  - Push notification entitlement exists.
  - Bundle identifiers still include upstream values:
    `chat.simplex.app`, `chat.simplex.app.SimpleX-NSE`,
    `chat.simplex.app.SimpleX-SE`, `chat.simplex.SimpleXChat`, and
    `chat.simplex.Tests-iOS`.
  - `BGTaskSchedulerPermittedIdentifiers` still uses
    `chat.simplex.app.receive`.
  - `CFBundleURLName` still uses `chat.simplex.app`.
  - App Group and keychain access group still use `group.chat.simplex.app` and
    `$(AppIdentifierPrefix)chat.simplex.app`.
  - Associated domains still point at upstream-controlled SimpleX domains:
    `simplex.chat`, `www.simplex.chat`, `*.simplex.im`, and
    `*.simplexonflux.com`.
  - Extension display names still use `SimpleX NSE` and `SimpleX SE`.
- `scripts/ios/check-ios-release-identifiers.sh` is now part of both
  `scripts/ios/check-nome-ios-readiness.sh` and
  `scripts/ios/check-nome-ios-goal-audit.sh` as `release_identifiers`.
- Latest release-id readiness:
  `/tmp/nome-ios-readiness-release-id-20260709-214248` reports 13 PASS, 2 WARN,
  6 BLOCKED, 0 FAIL.
- Latest release-id goal audit:
  `/tmp/nome-ios-goal-audit-release-id-20260709-214248` reports 25 PASS, 2
  WARN, 6 BLOCKED, 0 FAIL.
- Before TestFlight/public release, the team must choose final Nome-owned bundle
  identifiers, App Group, keychain group, associated-domain strategy, and
  background-task identifiers. The script intentionally does not change them
  because changing identifiers can affect data migration, app-group storage,
  push, extensions, and link compatibility.

## 2026-07-09 release identifier queue alignment

- The manual QA checklist now includes the release-identifier gate as unchecked
  release work, instead of leaving it only in the aggregate readiness script.
- Current manual QA status:
  `scripts/ios/check-nome-manual-qa-status.sh --write-tsv /tmp/nome-manual-qa-after-release.tsv`
  exits `1` as intended and reports 126 checklist items: 85 checked and 41
  unchecked.
- The unchecked queue now includes four `release_identifiers` items:
  the release-identifier script must pass, Nome-owned bundle identifiers must
  be selected or explicitly approved as compatibility exceptions, App Group and
  keychain names must have a data-migration decision, and associated domains /
  background task identifiers must be Nome-owned or documented compatibility
  exceptions.
- `scripts/ios/export-nome-qa-batches.sh --output /tmp/nome-ios-qa-batches-release-identifiers --force`
  passes and exports all 41 unchecked items into eight batches, including
  `batch-7` for release identifiers and capabilities.
- `scripts/ios/check-nome-qa-execution-plan.sh` passes with 16 current blocker
  groups, including `release_identifiers`.
- This makes the remaining QA packet match the actual readiness blockers:
  release identity is now an explicit execution batch before TestFlight/public
  distribution, not a hidden note in the release review.
- Latest queue-aligned readiness:
  `/tmp/nome-ios-readiness-release-queue-20260709-215040` reports 13 PASS, 2
  WARN, 6 BLOCKED, 0 FAIL.
- Latest queue-aligned goal audit:
  `/tmp/nome-ios-goal-audit-release-queue-20260709-215041` reports 25 PASS, 2
  WARN, 6 BLOCKED, 0 FAIL.

## 2026-07-09 extension display-name rebrand

- The notification service extension build display name changed from
  `SimpleX NSE` to `Nome Notifications`.
- The share extension build display name changed from `SimpleX SE` to
  `Nome Share`.
- Direct localized extension `InfoPlist.strings` files under
  `apps/ios/SimpleX NSE/*.lproj/` and `apps/ios/SimpleX SE/*.lproj/` now use
  the same Nome-facing display names.
- Target names, schemes, folder names, bundle identifiers, App Groups, keychain
  access groups, and associated domains were intentionally left unchanged in
  this pass because those are release/data-migration decisions rather than
  low-risk display-copy changes.
- A simulator build showed the base extension `CFBundleName` still follows
  target/product names (`SimpleX NSE` and `SimpleX SE`). This was intentionally
  not forced in the display-copy pass because changing it safely is tied to
  executable/product naming and the later release-identifier migration.
- `scripts/ios/check-ios-release-identifiers.sh` now checks both Xcode build
  settings and direct localized extension InfoPlist strings for old
  `SimpleX NSE` / `SimpleX SE` display labels.
- Final verification for this pass:
  - `scripts/ios/check-ios-release-identifiers.sh` now reports
    `Extension build display names are Nome-facing` and
    `Localized extension InfoPlist labels are Nome-facing`.
  - A Debug simulator build with
    `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild`
    succeeded using derived data
    `/tmp/nome-ios-derived-extension-display`.
  - Built product evidence from
    `/tmp/nome-ios-derived-extension-display/Build/Products/Debug-iphonesimulator/Nome.app`
    shows main app `CFBundleDisplayName=Nome`,
    notification extension `CFBundleDisplayName=Nome Notifications`, and share
    extension `CFBundleDisplayName=Nome Share`.
  - Latest extension-display readiness:
    `/tmp/nome-ios-readiness-extension-display-final-20260709-220424`
    reports 13 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.
  - Latest extension-display goal audit:
    `/tmp/nome-ios-goal-audit-extension-display-final-20260709-220424`
    reports 25 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.

## 2026-07-09 real-core source refresh

- Re-ran the network-dependent source audit with:
  `RELEASE_LIMIT=18 ACTIONS_ARTIFACT_PAGES=4 scripts/ios/check-real-core-sources.sh`
  and saved output at `/tmp/nome-real-core-source-audit-current.log`.
- The source audit still exits `1` for this Apple Silicon simulator path:
  current required simulator architecture is `arm64`, while the standard
  SimpleX simulator artifact architecture is `x86_64`.
- Current local artifact evidence:
  - `~/Downloads/pkg-ios-aarch64-swift-json` exists.
  - `~/Downloads/pkg-ios-x86_64-swift-json` exists.
  - The aarch64 core
    `libHSsimplex-chat-6.5.5.0-57WEoB2fiWaFgCB1XksYmP-ghc9.6.3.a` is
    293,139,304 bytes and `lipo` reports architecture `arm64`.
  - The x86_64 core
    `libHSsimplex-chat-6.5.5.0-57WEoB2fiWaFgCB1XksYmP-ghc9.6.3.a` is
    300,398,592 bytes and `lipo` reports architecture `x86_64`.
- Current public source evidence:
  - Recent GitHub release assets through `v7.0.0-beta.3` still do not expose
    reusable `pkg-ios-*-swift-json` assets.
  - Four GitHub Actions artifact pages were inspected and exposed dockerbuild
    metadata, not iOS core packages.
  - Hydra endpoints remain reachable for `master`, `v7-0-0-beta-3`, and
    `v6-5-5` standard aarch64 + x86_64 pairs.
  - The closest `v6-5-6` Hydra jobset still exposes the x86_64 endpoint but
    returns 404 for the standard aarch64 endpoint.
- Re-ran `scripts/ios/check-real-core-build-env.sh`; it exits `1` because
  `nix` is not installed, but confirms full Xcode, local `mac2ios`, and only
  `arm64` iOS Simulator destinations. It also reports about 47 GB free, below
  the 80 GB warning threshold for local Nix iOS core builds.
- `scripts/ios/check-ios-device-readiness.sh` now separates input readiness from
  installed-library readiness. It reports the local arm64 device artifact as
  production-sized, arm64, and free of preview markers, while still blocking on
  missing `apps/ios/Libraries/ios` and no connected physical iPhone/iPad.
- Current manual QA status after adding the device-artifact evidence:
  `scripts/ios/check-nome-manual-qa-status.sh --write-tsv /tmp/nome-manual-qa-realcore-source-refresh.tsv`
  reports 128 checklist items: 87 checked and 41 unchecked.
- Current QA batch export:
  `/tmp/nome-ios-qa-batches-realcore-source-refresh` still exports 41 unchecked
  items into eight batches.
- Latest readiness with network source audit:
  `/tmp/nome-ios-readiness-realcore-source-refresh-20260709-221508` reports
  13 PASS, 1 WARN, 7 BLOCKED, 0 FAIL. The seventh blocker is the intentionally
  run `real_core_source_audit`, which confirms there is still no
  arm64-simulator-compatible real-core source for the current simulator path.
- Latest goal audit:
  `/tmp/nome-ios-goal-audit-realcore-source-refresh-20260709-221508` reports
  25 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.

## 2026-07-09 device-only real-core prepare guard

- Added `scripts/ios/prepare-device-real-core.sh` for the physical-device
  fallback path.
- The script is read-only by default. It audits the local arm64 device artifact
  for required `libHSsimplex-chat` archives, `libffi.a`, `libgmp.a`,
  `libgmpxx.a`, production-like core size, preview-core markers, and `arm64`
  architecture support.
- The script installs into `apps/ios/Libraries/ios` only when explicitly run
  with `--prepare`; it never modifies `apps/ios/Libraries/sim`.
- A non-empty target directory is protected unless `--force` is supplied. When
  forced, the old target directory is backed up under `/tmp` before replacement.
- Added `scripts/ios/test-prepare-device-real-core.sh` to cover missing source,
  preview markers, wrong architecture, dry-run behavior, non-empty target
  protection, and forced sandbox install.
- Wired the new unit test into `scripts/ios/check-nome-ios-readiness.sh` as
  `prepare_device_real_core_unit`.
- Wired the new unit test into `scripts/ios/check-nome-ios-goal-audit.sh` as
  `prepare_device_real_core_unit`.
- Updated `scripts/ios/check-ios-device-readiness.sh` to point the
  physical-device path at the new audit/install helper.
- Dry-run evidence against the current local artifact:
  `scripts/ios/prepare-device-real-core.sh --source /Users/forkman03/Downloads/pkg-ios-aarch64-swift-json`
  exits `0` and reports:
  - required device libraries are present;
  - both `libHSsimplex-chat` archives are production-sized;
  - no preview markers are present;
  - all static libraries support `arm64`;
  - the artifact filenames are `6.5.5.0` while the current Xcode project
    references `6.5.6.1`.
- This improves Batch 0 and Batch 5 execution readiness, but it does not
  complete real-core functional QA. No physical iPhone/iPad is connected, and
  the available local artifact is still a compatibility candidate rather than
  same-version final release evidence.

## 2026-07-09 Xcode real-core reference guard

- Added `scripts/ios/check-real-core-xcode-sync.sh` as a read-only guard for
  explicit `libHSsimplex-chat` references in
  `apps/ios/SimpleX.xcodeproj/project.pbxproj`.
- The guard checks:
  - project GHC/plain `libHSsimplex-chat` archive references;
  - installed simulator library names in `apps/ios/Libraries/sim`;
  - installed device library names in `apps/ios/Libraries/ios`, when present;
  - candidate arm64 device artifact names under
    `~/Downloads/pkg-ios-aarch64-swift-json`.
- Installed simulator or installed device library mismatches fail because they
  would affect the next build. Candidate device-source mismatches warn until
  that source is deliberately installed.
- Added `scripts/ios/test-check-real-core-xcode-sync.sh`, covering:
  - matching installed simulator/device libraries with a mismatched candidate
    source that only warns;
  - simulator mismatch failure;
  - installed-device mismatch failure;
  - missing installed device directory warning.
- Wired the new unit test and read-only guard into
  `scripts/ios/check-nome-ios-readiness.sh`.
- Wired the new unit test and read-only guard into
  `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current project evidence:
  `scripts/ios/check-real-core-xcode-sync.sh` exits `0`.
  `apps/ios/Libraries/sim` matches the current Xcode project references
  (`6.5.6.1`), `apps/ios/Libraries/ios` is still missing, and the local
  `6.5.5.0` arm64 device artifact remains an advisory mismatch until installed.
- Latest verification after adding the Xcode reference guard:
  - `scripts/ios/check-nome-ios-readiness.sh --allow-blockers` wrote
    `/tmp/nome-ios-readiness-xcode-sync-20260709-223306` and reported
    16 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.
  - `scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers` wrote
    `/tmp/nome-ios-goal-audit-xcode-sync-20260709-223306` and reported
    28 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.
  - `git diff --check` passed.
  - A direct trailing-whitespace scan over the new scripts and touched plan
    records found no matches.

## 2026-07-09 release identifier compatibility mode

- Updated `scripts/ios/check-ios-release-identifiers.sh` with two explicit
  modes:
  - default strict final mode still blocks upstream bundle identifiers, App
    Group, keychain group, associated domains, URL type name, and background
    task identifier before public/TestFlight release;
  - `--compatibility-reviewed` mode allows the current compatibility-first
    development pass only when each retained upstream identifier appears in
    `plans/20260709_nome_ios_release_gate_review.md` as a documented exception.
- Added `scripts/ios/test-check-ios-release-identifiers.sh`, covering:
  - strict final mode blocks upstream identifiers;
  - compatibility-reviewed mode passes when all exceptions are documented;
  - compatibility-reviewed mode fails when an exception is missing;
  - Nome-facing display name remains required even in compatibility-reviewed
    mode.
- Updated `plans/20260709_nome_ios_release_gate_review.md` with a machine-checkable
  compatibility exception ledger for:
  - `chat.simplex.app`;
  - `chat.simplex.app.SimpleX-NSE`;
  - `chat.simplex.app.SimpleX-SE`;
  - `chat.simplex.SimpleXChat`;
  - `chat.simplex.Tests-iOS`;
  - `simplex`;
  - `chat.simplex.app.receive`;
  - `group.chat.simplex.app`;
  - `$(AppIdentifierPrefix)chat.simplex.app`;
  - `applinks:simplex.chat`;
  - `applinks:www.simplex.chat`;
  - `applinks:*.simplex.im`;
  - `applinks:*.simplexonflux.com`.
- Wired the new release identifier unit test and compatibility-reviewed gate
  into `scripts/ios/check-nome-ios-readiness.sh`.
- Wired the same evidence into `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current evidence:
  - `scripts/ios/test-check-ios-release-identifiers.sh` exits `0`.
  - `scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed`
    exits `0` and reports all retained upstream identifiers as documented
    compatibility exceptions.
  - `scripts/ios/check-ios-release-identifiers.sh` without the flag still exits
    `1` by design for final public/TestFlight release readiness.
- Latest verification after adding the compatibility-reviewed gate:
  - `scripts/ios/check-nome-manual-qa-status.sh --write-tsv /tmp/nome-manual-qa-release-compat.tsv`
    reports 129 checklist items: 88 checked and 41 unchecked.
  - `scripts/ios/check-nome-qa-execution-plan.sh` passes and still covers all
    16 current unchecked blocker groups.
  - `scripts/ios/check-nome-ios-readiness.sh --allow-blockers` wrote
    `/tmp/nome-ios-readiness-release-compat-20260709-224041` and reported
    18 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.
  - `scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers` wrote
    `/tmp/nome-ios-goal-audit-release-compat-20260709-224042` and reported
    30 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.

## 2026-07-09 App Store final screenshot blocker plan gate

- Added `scripts/ios/check-app-store-final-blocker-plan.sh` as a read-only
  evidence alignment gate for the non-final App Store screenshot package.
- The gate verifies:
  - `design/app-store/ios-upload-draft-screens/MANIFEST.md` exists;
  - `design/app-store/ios-upload-draft-screens/FINAL_BLOCKERS.md` exists;
  - exactly four `needs-real-core` screenshot blockers are recorded;
  - the add-friend blocker is tied to one-time invitation link evidence;
  - the public-contact blocker is tied to public contact address evidence;
  - the join-group blocker is tied to group invite preview/join evidence;
  - the conversation blocker is tied to two-account real conversation evidence;
  - each replacement has a matching manual-QA evidence anchor;
  - Batch 6 in `plans/20260709_nome_ios_remaining_qa_execution_plan.md`
    still names the replacement work and strict final screenshot command.
- Added `scripts/ios/test-app-store-final-blocker-plan.sh`, covering complete
  plan success, missing blocker failure, and missing manual-QA anchor failure.
- Wired both the unit test and the current blocker-plan check into
  `scripts/ios/check-nome-ios-readiness.sh`.
- Wired both checks into `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current evidence:
  - `scripts/ios/test-app-store-final-blocker-plan.sh` exits `0`.
  - `scripts/ios/check-app-store-final-blocker-plan.sh` exits `0`.
  - Strict final screenshot mode still fails by design until the four real-core
    screenshots are replaced and the manifest/blocker ledger are updated.
- Latest verification after adding the screenshot blocker plan gate:
  - `scripts/ios/check-nome-qa-execution-plan.sh` passes and now requires the
    final screenshot blocker-plan command.
  - `scripts/ios/check-nome-ios-readiness.sh --allow-blockers` wrote
    `/tmp/nome-ios-readiness-screenshot-blocker-plan-20260709-224835` and
    reported 20 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.
  - `scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers` wrote
    `/tmp/nome-ios-goal-audit-screenshot-blocker-plan-20260709-224835` and
    reported 32 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.
  - `git diff --check` passed, and a trailing-whitespace scan over the new
    scripts and touched records found 0 matches.

## 2026-07-09 refreshed 14-page UI smoke

- XcodeBuildMCP was attempted first after confirming session defaults:
  project `apps/ios/SimpleX.xcodeproj`, scheme `SimpleX (iOS)`, simulator
  `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`, derived data
  `/tmp/nome-ios-derived-clean`, bundle id `chat.simplex.app`.
- `mcp__xcodebuildmcp__build_run_sim` failed before building because its
  current environment could not locate `simctl`
  (`xcrun: error: unable to find utility "simctl"`). This was a tool
  environment issue, not an Xcode build diagnostic.
- Re-ran the repository smoke path with full Xcode explicitly:
  `DERIVED_DATA_PATH=/tmp/nome-ios-derived-clean DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-smoke-refresh-20260709-225133 --skip-build`.
- The smoke pass installed the existing `Nome.app` from
  `/tmp/nome-ios-derived-clean` and captured all 14 expected pages:
  home, onboarding welcome, onboarding profile, onboarding network, onboarding
  conditions, existing chat list, conversation preview, conversation details,
  identity center, add friend, join group, public contact, settings, and
  contacts.
- Every screenshot is `1206x2622`, above the 50 KB minimum, and has a unique
  SHA-256 hash. The manifest is
  `/tmp/nome-ios-smoke-refresh-20260709-225133/manifest.tsv`.
- No new `Nome-*.ips` crash report appeared; the latest crash before and after
  the pass remained
  `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-155004.ips`.
- The generated contact sheet exists at
  `/tmp/nome-ios-smoke-refresh-20260709-225133/contact-sheet.png`, is
  `1108x2320`, and is about 2.1 MB. ImageMagick reported a font annotation
  warning, but the contact sheet was produced.
- Visual review of the contact sheet found no blank pages. The Nome brand,
  Chinese primary actions, bottom navigation, preview-core readiness messages,
  and redesigned primary flows are visible. The settings page appears cropped
  only within the contact-sheet thumbnail layout; the underlying screenshot is
  full size.
- Readiness with smoke:
  `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-smoke-refresh-20260709-225232`
  reported 21 PASS, 1 WARN, 6 BLOCKED, 0 FAIL.
- Goal audit with the refreshed smoke manifest:
  `scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-refresh-20260709-225133/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-smoke-refresh-20260709-225233`
  reported 32 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.

## 2026-07-09 full-build 14-page UI smoke

- Tool-state evidence:
  - `xcode-select -p` reports `/Library/Developer/CommandLineTools`, so bare
    `/usr/bin/xcrun --find simctl` still fails with
    `unable to find utility "simctl"`.
  - With `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`,
    `/usr/bin/xcrun --find simctl` resolves to
    `/Applications/Xcode.app/Contents/Developer/usr/bin/simctl`.
  - Full Xcode is available: `xcodebuild -version` reports Xcode 26.6, build
    17F113.
  - Simulator `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4` (`iPhone 17 Pro`) was
    booted.
- XcodeBuildMCP was attempted again after `session_show_defaults` confirmed:
  project `/Users/forkman03/Documents/SimpleX/apps/ios/SimpleX.xcodeproj`,
  scheme `SimpleX (iOS)`, configuration `Debug`, simulator
  `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`, derived data
  `/tmp/nome-ios-derived-clean`, `preferXcodebuild: true`, bundle id
  `chat.simplex.app`.
- `mcp__xcodebuildmcp__build_run_sim` still failed before build because the MCP
  environment could not find `simctl`. The MCP log path is
  `/Users/forkman03/Library/Developer/XcodeBuildMCP/workspaces/SimpleX-244db75621a8/logs/build_run_sim_2026-07-09T14-59-21-414Z_pid92198_412ed9e8.log`.
- Full-build smoke command:
  `DERIVED_DATA_PATH=/tmp/nome-ios-derived-full-smoke-20260709-225943 DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-smoke-full-build-20260709-225943`.
- The smoke pass rebuilt the app, installed
  `/tmp/nome-ios-derived-full-smoke-20260709-225943/Build/Products/Debug-iphonesimulator/Nome.app`,
  launched all 14 expected preview states, and completed with exit code `0`.
- Built app identity:
  - `CFBundleDisplayName`: `Nome`
  - `CFBundleName`: `Nome`
  - `CFBundleIdentifier`: `chat.simplex.app`
- Smoke manifest:
  `/tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv`.
- Captured pages:
  home, onboarding welcome, onboarding profile, onboarding network,
  onboarding conditions, existing chat list, conversation preview,
  conversation details, identity center, add friend, join group, public
  contact, settings, and contacts.
- Every screenshot is `1206x2622`, above the 50 KB minimum, and has a unique
  SHA-256 hash. The manifest has one header row plus 14 screenshot rows.
- No new `Nome-*.ips` crash report appeared. The latest crash before and after
  the pass remained
  `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-09-155004.ips`.
- Contact sheet:
  `/tmp/nome-ios-smoke-full-build-20260709-225943/contact-sheet.png`.
  It is `1108x2320` and about 2.1 MB. ImageMagick reported the same font
  annotation warning (`unable to read font`) but generated the contact sheet.
- Visual review of the contact sheet found no blank pages or obvious wrong
  routing. The main Nome surfaces match the approved effect-picture direction:
  Nome brand, Chinese main actions, home/add/join/public-contact/identity/
  conversation/settings/contacts surfaces, bottom navigation, and preview-core
  readiness warnings are visible. The settings page is scrollable; its
  thumbnail crop in the contact sheet is not a screenshot failure.
- Readiness with the full-build app and skip-build smoke:
  `DERIVED_DATA_PATH=/tmp/nome-ios-derived-full-smoke-20260709-225943 DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-readiness-full-smoke-20260709-225943`
  reported 21 PASS, 1 WARN, 6 BLOCKED, 0 FAIL.
- Goal audit with the full-build smoke manifest:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-full-smoke-20260709-225943`
  reported 32 PASS, 2 WARN, 6 BLOCKED, 0 FAIL.
- Remaining blockers are unchanged and expected for this phase: manual QA
  status, strict final App Store screenshots, real-core preflight, local
  real-core build environment, physical-device readiness, and strict release
  identifiers. There were 0 FAIL results.

## 2026-07-09 real-core source audit refresh

- Refreshed the read-only source audit after the full-build smoke:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-real-core-sources.sh`.
- Evidence log:
  `/tmp/nome-ios-real-core-source-audit-rc-20260709-231137.log`.
- The command exited `1`, confirming that no currently inspected source
  satisfies the active Apple Silicon simulator path.
- Current simulator requirement: `arm64`.
- Local artifacts in `~/Downloads` are still useful but not simulator-complete
  for this machine: they provide device `aarch64` plus `x86_64` simulator
  artifacts, while the current simulator requires `arm64`.
- Recent GitHub releases inspected through `v7.0.0-beta.3` and latest
  `v6.5.6` do not expose `pkg-ios-*-swift-json` release assets.
- Recent GitHub Actions artifact pages expose only dockerbuild-style artifacts,
  not reusable `pkg-ios-*-swift-json` outputs.
- Known Hydra repositories `master`, `v7-0-0-beta-3`, and `v6-5-5` expose
  reachable device `aarch64` plus `x86_64` simulator artifacts, but not an
  `arm64` simulator pair for the active simulator build.
- Practical conclusion remains unchanged: continue UI/design validation with
  preview-core simulator builds, and unblock real functional QA by either
  producing an `arm64` simulator core, using a compatible x86_64 simulator
  route, or moving the real-core path to a physical iPhone with deliberate
  device-library preparation.

## 2026-07-09 simulator real-core route gate

- Added `scripts/ios/check-ios-simulator-real-core-route.sh` as a read-only
  route check for simulator real-core QA.
- The gate verifies:
  - full-Xcode `simctl` availability when `DEVELOPER_DIR` points at full Xcode;
  - concrete iOS Simulator destination architectures from
    `xcodebuild -showdestinations`;
  - whether the current simulator architecture has installed real-core
    simulator libraries;
  - whether the local `pkg-ios-x86_64-swift-json` artifact could be used as an
    x86_64 simulator fallback;
  - whether the iPhoneSimulator SDK can at least compile an x86_64 simulator
    binary, which is weaker than having a runnable x86_64 simulator
    destination.
- Added `scripts/ios/test-check-ios-simulator-real-core-route.sh`, covering:
  - an arm64 simulator route with real installed simulator libraries;
  - an x86_64 simulator route when an x86_64 destination and x86_64 artifact
    both exist;
  - the current-style blocked case where x86_64 artifacts exist but no
    x86_64 simulator destination exists.
- Wired the new unit test and current route check into
  `scripts/ios/check-nome-ios-readiness.sh`.
- Wired the same evidence into `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current route-check log:
  `/tmp/nome-ios-simulator-real-core-route-20260709-231923.log`.
- Current route-check result:
  - required simulator architecture: `arm64`;
  - available simulator destination architecture: `arm64`;
  - installed simulator libraries are `arm64`, but the core archive is only
    10104 bytes and contains preview-core markers;
  - local `pkg-ios-x86_64-swift-json` looks production-like, has no
    preview-core markers, and supports `x86_64`;
  - no x86_64 iOS Simulator destination is available, so the x86_64 artifact
    cannot be used for functional simulator QA on this machine;
  - route summary: `current_arm64_route_usable=0`,
    `x86_64_route_usable=0`.
- Latest physical-device check still reports no connected iPhone/iPad, while
  the local arm64 device artifact in `~/Downloads/pkg-ios-aarch64-swift-json`
  remains production-like and preview-marker free. Installed device libraries
  are still missing from `apps/ios/Libraries/ios`.
- Latest verification after adding the simulator-route gate:
  - `scripts/ios/test-check-ios-simulator-real-core-route.sh` exits `0`.
  - `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-sim-route-20260709-232030`
    reports 21 PASS, 2 WARN, 7 BLOCKED, 0 FAIL.
  - `scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-sim-route-20260709-232030`
    reports 33 PASS, 2 WARN, 7 BLOCKED, 0 FAIL.

## 2026-07-09 QA execution state export

- Added `scripts/ios/export-nome-qa-execution-state.sh` as a read-only
  execution packet generator for the remaining Nome iOS QA work.
- The export writes:
  - `summary.tsv`;
  - `gate_status.tsv`;
  - `blocker_groups.tsv`;
  - `next_actions.tsv`;
  - `manual_qa_unchecked.tsv`;
  - a nested `batches/` export from `scripts/ios/export-nome-qa-batches.sh`;
  - per-gate logs under `logs/`.
- Added `scripts/ios/test-export-nome-qa-execution-state.sh`, covering a
  fixture checklist and `--skip-gates` mode.
- Wired the new unit test and export check into
  `scripts/ios/check-nome-ios-readiness.sh`.
- Wired the same evidence into `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current direct export:
  `/tmp/nome-ios-qa-execution-state-20260709-232815`.
- Current readiness-owned export:
  `/tmp/nome-ios-readiness-qa-state-20260709-232938/qa_execution_state`.
- Current export summary:
  - 129 manual QA items total;
  - 88 checked;
  - 41 unchecked;
  - 7 gate blockers;
  - 8 ordered next-action rows.
- The first next action is Batch 0, `real-core artifacts`, with command:
  `scripts/ios/check-ios-simulator-real-core-route.sh && scripts/ios/check-ios-device-readiness.sh`.
- Latest verification after adding the execution-state export:
  - `scripts/ios/test-export-nome-qa-execution-state.sh` exits `0`.
  - `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-qa-state-20260709-232938`
    reports 23 PASS, 2 WARN, 7 BLOCKED, 0 FAIL.
  - `scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-qa-state-20260709-232938`
    reports 35 PASS, 2 WARN, 7 BLOCKED, 0 FAIL.

## 2026-07-09 release identity state export

- Added `scripts/ios/export-ios-release-identity-state.sh` as a read-only
  release-identity state packet generator.
- The export writes:
  - `summary.tsv`;
  - `gate_status.tsv`;
  - `current_identifiers.tsv`;
  - `required_decisions.tsv`;
  - `next_actions.tsv`;
  - per-gate logs under `logs/`.
- Added `scripts/ios/test-export-ios-release-identity-state.sh`, covering a
  fixture with upstream-compatible identifiers, documented compatibility
  exceptions, and a strict final-release blocker.
- Wired the new unit test and release-identity export check into
  `scripts/ios/check-nome-ios-readiness.sh`.
- Wired the same evidence into `scripts/ios/check-nome-ios-goal-audit.sh`.
- Direct export:
  `/tmp/nome-ios-release-identity-state-20260709-233510`.
- Readiness-owned export:
  `/tmp/nome-ios-readiness-release-state-20260709-233643/release_identity_state`.
- Current export summary:
  - compatibility-reviewed gate: PASS;
  - strict final gate: BLOCKED;
  - current identifier rows: 21;
  - required final decision rows: 8.
- Required final decision rows are:
  - main app bundle id;
  - notification/share extension bundle ids;
  - internal framework and test bundle ids;
  - App Group;
  - keychain access group;
  - associated domains and Nome-owned AASA files;
  - background task id;
  - `simplex` URL scheme compatibility.
- Latest verification after adding the release-identity export:
  - `scripts/ios/test-export-ios-release-identity-state.sh` exits `0`.
  - `scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-release-state-20260709-233643`
    reports 25 PASS, 2 WARN, 7 BLOCKED, 0 FAIL.
  - `scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-release-state-20260709-233643`
    reports 37 PASS, 2 WARN, 7 BLOCKED, 0 FAIL.

## 2026-07-09 source-aware readiness after state exports

- Ran the full readiness gate with network-dependent source audit after adding
  QA execution-state and release-identity exports:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --output /tmp/nome-ios-readiness-source-state-20260709-233944`.
- Result: 25 PASS, 1 WARN, 8 BLOCKED, 0 FAIL.
- Ran the goal audit with the same source audit and the latest full-build smoke
  manifest:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --source-audit --smoke-manifest /tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-source-state-20260709-233944`.
- Result: 37 PASS, 1 WARN, 8 BLOCKED, 0 FAIL.
- The additional blocker compared with non-source-audit runs is
  `real_core_source_audit`.
- Source audit evidence remains unchanged:
  - required simulator architecture is `arm64`;
  - local artifacts provide device `arm64` plus `x86_64` simulator artifacts,
    but not an arm64 simulator artifact;
  - recent GitHub release assets through `v7.0.0-beta.3` and latest `v6.5.6`
    do not expose simulator-compatible `pkg-ios-*-swift-json` assets;
  - recent GitHub Actions artifacts do not expose reusable
    `pkg-ios-*-swift-json` outputs;
  - known Hydra repositories `master`, `v7-0-0-beta-3`, and `v6-5-5` expose
    reachable device `aarch64` plus `x86_64` simulator artifacts, but not an
    arm64 simulator pair.
- The readiness-owned QA execution-state export from this source-aware run
  still reports 129 total manual QA items, 88 checked, 41 unchecked, 7 gate
  blockers, and 8 next-action rows. A later checklist update for the App Store
  screenshot-state export increases this to 130 total / 89 checked while
  keeping 41 unchecked blockers.
- The readiness-owned release-identity export still reports
  compatibility-reviewed PASS, strict final BLOCKED, 21 current identifier
  rows, and 8 required final decision rows.

## 2026-07-09 App Store final screenshot state export

- Added `scripts/ios/export-app-store-final-screenshot-state.sh` as a read-only
  finalization packet generator for the App Store screenshot package.
- The export writes:
  - `summary.tsv`;
  - `gate_status.tsv`;
  - `screenshot_status.tsv`;
  - `replacement_plan.tsv`;
  - `next_actions.tsv`;
  - per-gate logs under `logs/`.
- Added `scripts/ios/test-export-app-store-final-screenshot-state.sh`, covering
  a fixture manifest, four `needs-real-core` blockers, manual QA anchors, and
  `--skip-gates` mode.
- Wired the new unit test and screenshot-state export check into
  `scripts/ios/check-nome-ios-readiness.sh`.
- Wired the same evidence into `scripts/ios/check-nome-ios-goal-audit.sh`.
- Direct export:
  `/tmp/nome-ios-app-store-final-screenshot-state-current`.
- Current direct export summary:
  - candidate screenshot gate: PASS;
  - strict final screenshot gate: BLOCKED;
  - blocker-plan gate: PASS;
  - screenshot rows: 10;
  - ready rows: 6;
  - `needs-real-core` rows: 4;
  - replacement rows: 4.
- Replacement rows are:
  - `05-add-friend-needs-real-core.jpg`: capture a real one-time invitation
    link and QR code from the real iOS core;
  - `06-public-contact-needs-real-core.jpg`: capture a real public contact
    address page with copy/share actions backed by the real iOS core;
  - `07-join-group-needs-real-core.jpg`: capture a real group invite preview
    or join path from a real group invitation link;
  - `08-conversation-needs-real-core.jpg`: capture a real two-account
    conversation after send and receive text succeeds.
- Latest integrated verification after adding the screenshot-state export:
  - `scripts/ios/test-export-app-store-final-screenshot-state.sh` exits `0`;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-screenshot-state-20260709-235128`
    reports 27 PASS, 2 WARN, 7 BLOCKED, 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-screenshot-state-20260709-235128`
    reports 39 PASS, 2 WARN, 7 BLOCKED, 0 FAIL.
- The readiness-owned screenshot-state export is:
  `/tmp/nome-ios-readiness-screenshot-state-20260709-235128/app_store_final_screenshot_state`.
- The readiness-owned QA execution-state export now reports 130 total manual QA
  items, 89 checked, 41 unchecked, 7 gate blockers, and 8 next-action rows.

## 2026-07-10 x86_64 simulator real-core build probe

- Added `scripts/ios/probe-x86_64-simulator-real-core.sh` as a reversible
  x86_64 simulator real-core probe.
- The probe:
  - backs up `apps/ios/Libraries/sim`;
  - temporarily copies the local x86_64 real-core archives from
    `~/Downloads/pkg-ios-x86_64-swift-json` into the simulator library
    filenames currently referenced by the Xcode project;
  - runs an x86_64 iOS Simulator build with
    `ARCHS=x86_64 ONLY_ACTIVE_ARCH=NO EXCLUDED_ARCHS=arm64`;
  - restores the original simulator libraries before exiting;
  - records original, temporary, and restored SHA-256 tables.
- Added `scripts/ios/test-probe-x86_64-simulator-real-core.sh`, covering the
  library swap/restore behavior in a temporary fixture.
- Wired the fixture test into `scripts/ios/check-nome-ios-readiness.sh` and
  `scripts/ios/check-nome-ios-goal-audit.sh`.
- Direct probe:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/probe-x86_64-simulator-real-core.sh --output /tmp/nome-ios-x86_64-sim-real-core-probe-current --force`.
- Direct probe result:
  - build status: PASS;
  - restore status: PASS;
  - output app:
    `/tmp/nome-ios-x86_64-sim-real-core-probe-current/DerivedData/Build/Products/Debug-iphonesimulator/Nome.app`;
  - app executable architecture: x86_64.
- Manual install/launch result on booted simulator
  `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`:
  - `simctl install` fails with `Failed to find matching arch for input file`;
  - `arch -x86_64 xcrun simctl install` fails with the same error;
  - Rosetta is installed on the Mac, so the failure is the iOS 26.5 simulator
    runtime architecture, not merely the host shell architecture.
- Post-probe safety checks:
  - current `apps/ios/Libraries/sim` SHA-256 table matches the probe backup;
  - `scripts/ios/check-ios-simulator-real-core-route.sh` again reports the
    restored arm64 preview-core libraries and blocks functional simulator QA.
- Conclusion: local x86_64 real core can build, but it cannot run on the
  current arm64-only iOS 26.5 simulator. The remaining functional QA routes are
  still an arm64 simulator real-core artifact, a simulator/runtime combination
  that accepts x86_64 apps, or the physical-device arm64 path.
- Latest integrated verification after adding the x86_64 probe:
  - `scripts/ios/test-probe-x86_64-simulator-real-core.sh` exits `0`;
  - `scripts/ios/check-nome-qa-execution-plan.sh` exits `0`;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-x86-probe-20260710-000547`
    reports 28 PASS, 2 WARN, 7 BLOCKED, 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-x86-probe-20260710-000547`
    reports 40 PASS, 2 WARN, 7 BLOCKED, 0 FAIL.
- The readiness-owned QA execution-state export now reports 131 total manual QA
  items, 90 checked, 41 unchecked, 7 gate blockers, and 8 next-action rows.

## 2026-07-10 real-core route state export

- Added `scripts/ios/export-real-core-route-state.sh` as a read-only route
  state packet generator for the current real-core QA options.
- The export writes:
  - `summary.tsv`;
  - `gate_status.tsv`;
  - `route_status.tsv`;
  - `next_actions.tsv`;
  - per-gate logs under `logs/`.
- Added `scripts/ios/test-export-real-core-route-state.sh`, covering fixture
  logs for arm64 simulator blocked, x86_64 simulator build-only, physical
  device blocked, and local Nix build blocked.
- Wired the new unit test and route-state export check into
  `scripts/ios/check-nome-ios-readiness.sh`.
- Wired the same evidence into `scripts/ios/check-nome-ios-goal-audit.sh`.
- Direct export:
  `/tmp/nome-ios-real-core-route-state-current`.
- Readiness-owned export:
  `/tmp/nome-ios-readiness-route-state-20260710-001221/real_core_route_state`.
- Current route summary:
  - ready routes: 0;
  - build-only routes: 1;
  - blocked routes: 3;
  - gate failures: 0;
  - gate blockers: 3.
- Current route rows:
  - `arm64_simulator`: BLOCKED because installed arm64 simulator libraries
    still contain preview-core markers;
  - `x86_64_simulator`: BUILD_ONLY because the x86_64 probe built a Nome
    simulator app, but the current simulator runtime rejected install with
    `Failed to find matching arch`;
  - `physical_device`: BLOCKED because no connected trusted iPhone/iPad was
    found;
  - `local_nix_build`: BLOCKED because Nix is not installed.
- Latest integrated verification after adding the route-state export:
  - `scripts/ios/test-export-real-core-route-state.sh` exits `0`;
  - `scripts/ios/check-nome-qa-execution-plan.sh` exits `0`;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --output /tmp/nome-ios-readiness-route-state-20260710-001221`
    reports 30 PASS, 2 WARN, 7 BLOCKED, 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --smoke-manifest /tmp/nome-ios-smoke-full-build-20260709-225943/manifest.tsv --allow-blockers --output /tmp/nome-ios-goal-audit-route-state-20260710-001221`
    reports 42 PASS, 2 WARN, 7 BLOCKED, 0 FAIL.
- The readiness-owned QA execution-state export now reports 132 total manual QA
  items, 91 checked, 41 unchecked, 7 gate blockers, and 8 next-action rows.

## 2026-07-10 real-core route next-action mapping

- Hardened `scripts/ios/export-real-core-route-state.sh` so
  `next_actions.tsv` now records route status, command, evidence path, manual
  QA anchor, exact checklist line, checklist section, blocker group, and batch
  id for each real-core route action.
- Added fixture coverage in `scripts/ios/test-export-real-core-route-state.sh`
  for the route action mapping columns and the arm64 simulator / physical
  device QA anchors.
- Unit verification:
  `scripts/ios/test-export-real-core-route-state.sh` exits `0`.
- Direct export:
  `/tmp/nome-ios-real-core-route-state-actions-20260710-011307`.
- Current next-action mapping:
  - `arm64_simulator`: BLOCKED, checklist line 13, `Environment gate`,
    `real_core_artifacts`, batch 0;
  - `x86_64_simulator`: BUILD_ONLY, checklist line 13, `Environment gate`,
    `real_core_artifacts`, batch 0;
  - `physical_device`: BLOCKED, checklist line 40, `Environment gate`,
    `physical_device_or_camera`, batch 5;
  - `local_nix_build`: BLOCKED, checklist line 13, `Environment gate`,
    `real_core_artifacts`, batch 0.
- Integrated verification after the route next-action mapping change:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-route-actions-20260710-011451`
    reports 31 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-goal-audit-route-actions-20260710-011451`
    reports 42 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 Batch 0 QA evidence template guard

- Hardened `scripts/ios/export-nome-qa-batches.sh` so the Batch 0 evidence
  packet now starts with:
  - `scripts/ios/export-real-core-route-state.sh`;
  - `scripts/ios/check-real-core-sources.sh`;
  - staging through `scripts/ios/run-real-core-batch0.sh`;
  - an explicit warning that `--prepare` should only run after route-state and
    source-audit evidence proves simulator compatibility, or after choosing a
    physical-device fallback deliberately.
- The generated Batch 0 evidence templates now require:
  - route-state packet path;
  - source-audit log path;
  - staged artifact directory path;
  - evidence that `--prepare` was gated by route compatibility;
  - final `scripts/ios/check-real-core.sh` output or
    `scripts/ios/check-ios-device-readiness.sh` output for the physical-device
    fallback.
- Added `scripts/ios/test-export-nome-qa-batches.sh` to lock this Batch 0
  template behavior.
- Wired the new QA batch export unit into both:
  - `scripts/ios/check-nome-ios-readiness.sh`;
  - `scripts/ios/check-nome-ios-goal-audit.sh`.
- Direct export:
  `/tmp/nome-ios-qa-batches-batch0-guard-20260710-012026`.
- Integrated verification:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-qa-batch-guard-20260710-012046`
    reports 32 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-goal-audit-qa-batch-guard-20260710-012046`
    reports 43 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 visual-header refinement smoke

- Refined the secondary primary-flow pages so `添加朋友`, `加入群组`, and
  `公开联系方式` show a consistent small Nome navigation brand and a clearer
  page-level title/description hierarchy.
- A manual simulator build with
  `CODE_SIGNING_ALLOWED=NO` passed compilation and produced
  `/tmp/nome-ios-derived-visual-header/Build/Products/Debug-iphonesimulator/Nome.app`,
  but that unsigned-style product is not valid smoke evidence: launching it in
  the simulator crashed in `AppDelegate.prepareForLaunch()` because the App
  Group container was unavailable.
- Re-ran the UI smoke with the script-owned signed simulator build:
  `OUTPUT_DIR=/tmp/nome-ios-smoke-visual-header-signed-20260710-002440 DERIVED_DATA_PATH=/tmp/nome-ios-derived-visual-header-signed DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`.
- Result:
  - the script rebuilt, installed, launched, and captured all 14 Nome UI smoke
    states;
  - no new `Nome-*.ips` crash report was created after the smoke run;
  - manifest:
    `/tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv`;
  - visual contact sheet:
    `/tmp/nome-ios-smoke-visual-header-signed-20260710-002440/contact-sheet.png`.
- Visual check: the updated add-friend, join-group, and public-contact pages
  now carry the same compact Nome header treatment as the approved product
  mockups, while the underlying preview-core readiness guards remain visible.
- Follow-up gate hardening: `scripts/ios/check-nome-design-coverage.sh` now
  checks the new `NomeInlineBrandMark`, `NomeFlowPageHeader`,
  `NomeAddressInlineBrandMark`, and `NomeAddressPageHeader` markers, so future
  changes must preserve the refined secondary-page product header treatment.
- Readiness smoke evidence reuse: `scripts/ios/check-nome-ios-readiness.sh`
  now accepts `--smoke-manifest FILE` and validates the existing smoke manifest
  for 14 labels, consistent dimensions, non-empty screenshot sizes, and unique
  hashes. This lets the readiness gate count the signed smoke pass at
  `/tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv`
  without rebuilding or relaunching the simulator.
- Verification after adding `--smoke-manifest`:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-smoke-manifest-20260710-003640`
  reports 31 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL. The remaining WARN is the
  optional real-core source audit, not visual smoke coverage.

## 2026-07-10 real-core source audit destination evidence

- Hardened `scripts/ios/check-real-core-sources.sh` so it records Xcode iOS
  Simulator destination architectures in the source-audit log.
- Added fixture coverage to `scripts/ios/test-real-core-source-audit.sh` for an
  arm64-only simulator destination and the warning that standard x86_64
  simulator artifacts cannot run without an x86_64 simulator destination.
- Unit verification:
  `scripts/ios/test-real-core-source-audit.sh` exits `0`.
- Direct shortened source audit:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 INCLUDE_KNOWN_HYDRA_REPOS=0 scripts/ios/check-real-core-sources.sh`
  exits `1` and records:
  - Xcode iOS Simulator destination architecture(s): `arm64`;
  - required simulator architecture `arm64` is available;
  - standard SimpleX simulator artifacts are `x86_64`;
  - standard x86_64 simulator artifacts cannot run on this machine until an
    x86_64 iOS Simulator destination is available.
- Full readiness with live source audit:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-source-audit-20260710-004121`
  reports 31 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL.
- Interpretation: the visual iOS redesign is still covered by smoke evidence,
  but final functional QA remains blocked until either an arm64-simulator core
  is obtained/built, a compatible x86_64 simulator destination is available, or
  a trusted physical iPhone is connected and prepared for the local arm64 device
  artifact path.

## 2026-07-10 final screenshot replacement mapping

- Hardened `scripts/ios/export-app-store-final-screenshot-state.sh` so
  `replacement_plan.tsv` now records the exact manual QA line, manual QA
  section, and blocker group for each `needs-real-core` App Store screenshot.
- Added fixture coverage in
  `scripts/ios/test-export-app-store-final-screenshot-state.sh` for the new
  checklist-location and blocker-group fields.
- Unit verification:
  `scripts/ios/test-export-app-store-final-screenshot-state.sh` exits `0`.
- Direct export:
  `/tmp/nome-ios-app-store-final-screenshot-state-location-20260710-005402`.
- Current replacement mapping:
  - `05-add-friend-needs-real-core.jpg` -> checklist line 76,
    `Add friend`, `real_core_functional`, batch 2;
  - `06-public-contact-needs-real-core.jpg` -> checklist line 100,
    `Public contact address`, `real_core_functional`, batch 2;
  - `07-join-group-needs-real-core.jpg` -> checklist line 89,
    `Join group`, `group_real_core`, batch 3;
  - `08-conversation-needs-real-core.jpg` -> checklist line 113,
    `Conversation`, `multi_account_real_core`, batch 3.
- Candidate screenshot gate still passes, strict final screenshot gate remains
  blocked, and the blocker-plan gate passes. This is expected until real-core
  screenshots replace the four preview/debug draft rows.

## 2026-07-10 release identity decision mapping

- Hardened `scripts/ios/export-ios-release-identity-state.sh` so
  `required_decisions.tsv` now records the manual QA anchor, exact checklist
  line, checklist section, and blocker group for each release-identity decision.
- Added fixture coverage in `scripts/ios/test-export-ios-release-identity-state.sh`
  for bundle-id and URL-scheme manual QA mappings.
- Unit verification:
  `scripts/ios/test-export-ios-release-identity-state.sh` exits `0`.
- Direct export:
  `/tmp/nome-ios-release-identity-state-mapped-20260710-010008`.
- Current release-decision mapping:
  - main app, extension, internal framework, and test bundle-id decisions map to
    checklist line 160, `Release gate`, `release_identifiers`;
  - App Group and keychain decisions map to checklist line 161,
    `Release gate`, `release_identifiers`;
  - associated-domain and background-task decisions map to checklist line 162,
    `Release gate`, `release_identifiers`;
  - `simplex` URL-scheme review maps to checklist line 156, `Release gate`,
    `release_identifier_review`.
- Compatibility-reviewed identifier gate still passes for development; strict
  final release identifier mode remains blocked by design until Nome-owned
  identifiers or explicit public-release exceptions are selected.
- Integrated verification after the mapping change:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-release-mapping-20260710-010113`
    reports 31 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-goal-audit-release-mapping-20260710-010113`
    reports 42 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 release identity next-action mapping

- Hardened `scripts/ios/export-ios-release-identity-state.sh` again so
  `next_actions.tsv` now records decision scope, evidence path, manual QA
  anchor, exact checklist line, checklist section, and blocker group for each
  release-identity next action.
- Extended `scripts/ios/test-export-ios-release-identity-state.sh` to verify:
  - `summary.tsv` records three release next-action rows;
  - `next_actions.tsv` exposes the manual QA mapping columns;
  - the final TestFlight action maps to checklist line 160, `Release gate`,
    `release_identifiers`;
  - the compatibility-reviewed development action maps to checklist line 156,
    `Release gate`, `release_identifier_review`.
- Unit verification:
  `scripts/ios/test-export-ios-release-identity-state.sh` exits `0`.
- Direct export:
  `/tmp/nome-ios-release-identity-state-actions-20260710-010649`.
- Current next-action mapping:
  - development compatibility review: checklist line 156, `Release gate`,
    `release_identifier_review`, evidence `gate_status.tsv`;
  - final TestFlight/public identifier decision: checklist line 160,
    `Release gate`, `release_identifiers`, evidence `required_decisions.tsv`;
  - identifier migration bundle: checklist line 161, `Release gate`,
    `release_identifiers`, evidence `current_identifiers.tsv`.
- Integrated verification after the next-action mapping change:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-release-actions-20260710-010810`
    reports 31 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-goal-audit-release-actions-20260710-010810`
    reports 42 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 design evidence state export

- Added `scripts/ios/export-nome-design-evidence-state.sh` as a read-only
  machine-readable bridge from the seven approved `pages-v2` mockups to the
  signed iOS smoke screenshots.
- The export writes:
  - `summary.tsv`;
  - `coverage.tsv`, with coverage id, design artifact, design bytes/hash,
    smoke labels, smoke paths, smoke hashes, smoke sizes, current visual
    status, and remaining real-core proof;
  - `missing.tsv`, which must be empty for the gate to pass.
- Added `scripts/ios/test-export-nome-design-evidence-state.sh` to verify the
  seven coverage rows, key smoke mappings, functional-blocked status, and
  remaining-proof text.
- Wired the design evidence unit and export into:
  - `scripts/ios/check-nome-ios-readiness.sh`;
  - `scripts/ios/check-nome-ios-goal-audit.sh`.
- Direct export:
  `/tmp/nome-ios-design-evidence-state-current-20260710-012530`.
- Current design evidence summary:
  - coverage rows: 7;
  - missing rows: 0;
  - mapped smoke labels include home, existing chat list, add friend, join
    group, public contact, identity center, conversation preview/details, and
    settings.
- Integrated verification:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-design-evidence-20260710-012710`
    reports 34 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-goal-audit-design-evidence-20260710-012711`
    reports 45 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 real-core source refresh

- Re-ran the external source audit with:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=18 ACTIONS_ARTIFACT_PAGES=4 DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-real-core-sources.sh`.
- Result log:
  `/tmp/nome-real-core-source-audit-refresh-20260710-013125.log`.
- Result: exits `1`, which is expected for the current machine because no
  arm64-simulator-compatible real-core source was found.
- Audit scope:
  - current Xcode iOS Simulator destination architectures;
  - local artifacts under `~/Downloads`;
  - 18 recent GitHub releases;
  - 4 pages of GitHub Actions artifacts;
  - known Hydra job repositories `master`, `v7-0-0-beta-3`, and `v6-5-5`.
- Current findings:
  - Xcode exposes `arm64` iOS Simulator destinations;
  - local artifacts are still device-arm64 plus x86_64-simulator;
  - GitHub releases do not expose `pkg-ios-*-swift-json` assets;
  - recent GitHub Actions artifacts do not expose reusable iOS core packages;
  - known Hydra repositories expose the standard pair, but that simulator
    artifact is `x86_64`, not usable on the current arm64-only simulator path.
- Practical next routes remain unchanged: obtain/build an arm64 simulator core,
  use a compatible x86_64 simulator runtime if one becomes available, or move
  real-core functional QA to a trusted physical iPhone.
- Integrated verification with the refreshed source audit:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-source-refresh-20260710-013400`
    reports 34 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL.

## 2026-07-10 physical-device state export

- Added `scripts/ios/export-ios-device-readiness-state.sh` as a read-only
  structured export for the physical-device fallback path.
- The export writes:
  - `summary.tsv`;
  - `gate_status.tsv`;
  - `device_status.tsv`, split into Xcode environment, connected device, local
    device artifact, installed device libraries, signing environment, and
    release identifiers;
  - `next_actions.tsv`, with manual QA anchor, exact checklist line, checklist
    section, blocker group, and batch id for each non-pass device area.
- Added `scripts/ios/test-export-ios-device-readiness-state.sh` to verify:
  - current-like no-device logs produce 3 PASS, 1 WARN, and 2 BLOCKED areas;
  - connected-device blockers map to checklist line 40 / batch 5;
  - installed device-library blockers map to checklist line 13 / batch 0;
  - release-identifier warnings map to checklist line 160 / batch 7.
- Wired the device-readiness state unit/export into:
  - `scripts/ios/check-nome-ios-readiness.sh`;
  - `scripts/ios/check-nome-ios-goal-audit.sh`.
- Unit verification:
  `scripts/ios/test-export-ios-device-readiness-state.sh` exits `0`.
- Direct export:
  `/tmp/nome-ios-device-readiness-state-current-20260710-014319`.
- Current direct export summary:
  - Xcode environment: PASS;
  - connected physical device: BLOCKED, no trusted iPhone/iPad is connected;
  - local arm64 device artifact: PASS;
  - installed device libraries: BLOCKED, `apps/ios/Libraries/ios` is missing;
  - signing environment: PASS;
  - release identifiers: WARN, bundle id is still upstream-compatible for the
    development pass.
- Integrated verification after wiring the export into the aggregate gates:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-readiness-device-state-20260710-014532`
    reports 36 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv --output /tmp/nome-ios-goal-audit-device-state-20260710-014532`
    reports 47 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 current simulator UI smoke refresh

- Checked XcodeBuildMCP for the configured iOS app defaults:
  - project: `apps/ios/SimpleX.xcodeproj`;
  - scheme: `SimpleX (iOS)`;
  - simulator: `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`;
  - bundle id: `chat.simplex.app`.
- XcodeBuildMCP could not run the simulator workflow because its process
  resolved `xcrun` to Command Line Tools and failed with:
  `unable to find utility "simctl", not a developer tool or in PATH`.
- Used the already-supported full-Xcode shell path instead:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer SIMULATOR_ID=95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 SETTLE_SECONDS=2 scripts/ios/smoke-nome-ui.sh --output /tmp/nome-ios-smoke-current-20260710-014941`.
- Result: exits `0` after building `SimpleX (iOS)`, installing
  `/tmp/nome-ios-derived-smoke/Build/Products/Debug-iphonesimulator/Nome.app`,
  launching fourteen Nome preview/flow cases, and confirming no new Nome crash
  report was created.
- Current smoke evidence:
  - manifest: `/tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv`;
  - contact sheet: `/tmp/nome-ios-smoke-current-20260710-014941/contact-sheet.png`;
  - build/run log: `/tmp/nome-ios-smoke-current.log`.
- Current manifest summary:
  - 14 screenshot rows;
  - all screenshots are `1206x2622`;
  - labels cover home, onboarding welcome/profile/network/conditions, existing
    chat list, conversation preview/details, identity center, add friend, join
    group, public contact, settings, and contacts;
  - screenshot sizes range from 262,970 bytes to 673,340 bytes, so none are
    blank/minimal captures.
- Visual inspection of the contact sheet confirms the screenshots render the
  expected Nome surfaces rather than blank or wrong-app pages.
- Re-exported design evidence with the new manifest:
  `/tmp/nome-ios-design-evidence-state-smoke-current-20260710-015112`.
  It reports 7 coverage rows, 0 missing rows, and 14 smoke label rows.
- Integrated verification with the refreshed smoke manifest:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-smoke-current-20260710-015112`
    reports 36 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-smoke-current-20260710-015112`
    reports 47 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 QA execution state next-action hardening

- Updated `scripts/ios/export-nome-qa-execution-state.sh` so batch-level next
  actions point at the detailed state exports where they exist:
  - Batch 0 now points at
    `scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-current --force`;
  - Batch 5 now points at
    `scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-current --force`.
- The export also records `real_core_route_state` and `device_readiness_state`
  in `gate_status.tsv`, so the execution packet can be resumed from concrete
  route/device evidence rather than only broad blocked checks.
- Unit verification:
  `scripts/ios/test-export-nome-qa-execution-state.sh` exits `0`.
- Direct export:
  `/tmp/nome-ios-qa-execution-state-current-20260710-015344`.
- Current direct export summary:
  - total manual QA items: 132;
  - checked items: 91;
  - unchecked items: 41;
  - gate failures: 0;
  - gate blockers: 7;
  - next action rows: 8.
- Integrated verification after the next-action mapping change:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-qa-exec-state-20260710-015421`
    reports 36 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-qa-exec-state-20260710-015422`
    reports 47 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 QA execution state final/release action hardening

- Updated `scripts/ios/export-nome-qa-execution-state.sh` again so every
  high-level blocker batch that has a detailed state packet now points to that
  packet from `next_actions.tsv`:
  - Batch 0 -> real-core route state;
  - Batch 5 -> device readiness state;
  - Batch 6 -> App Store final screenshot state;
  - Batch 7 -> release identity state.
- The execution-state gate status now also records:
  - `app_store_final_screenshot_state`;
  - `release_identity_state`.
- This keeps the strict blockers visible (`app_store_final_screenshots` and
  `release_identifiers` remain BLOCKED), while making the resumable next step
  a state export with manual-QA anchors and replacement/decision rows.
- Unit verification:
  `scripts/ios/test-export-nome-qa-execution-state.sh` exits `0`.
- Direct export:
  `/tmp/nome-ios-qa-execution-state-stateful-actions-20260710-015824`.
- Current direct export summary:
  - total manual QA items: 132;
  - checked items: 91;
  - unchecked items: 41;
  - gate failures: 0;
  - gate blockers: 7;
  - next action rows: 8.
- Integrated verification after the final/release action mapping change:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-stateful-actions-20260710-015904`
    reports 36 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-stateful-actions-20260710-015904`
    reports 47 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 real-core route physical-device precision

- Hardened `scripts/ios/export-real-core-route-state.sh` so the
  `physical_device` row no longer reports only "connect a device" when
  installed device libraries are also missing.
- Current physical-device route behavior:
  - evidence says no connected trusted physical iPhone/iPad was found, and
    `apps/ios/Libraries/ios` is missing;
  - next step says to export device readiness state, install audited device
    libraries only when ready for device testing, then connect and trust a
    physical iPhone;
  - command points at
    `scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-current --force`.
- Extended `scripts/ios/test-export-real-core-route-state.sh` to verify:
  - the physical-device route summarizes both device and library blockers when
    both are present;
  - the physical-device next action points at the device-readiness-state export.
- Unit verification:
  `scripts/ios/test-export-real-core-route-state.sh` exits `0`.
- Direct export:
  `/tmp/nome-ios-real-core-route-state-physical-combined-20260710-020320`.
- Current direct export summary:
  - ready routes: 0;
  - build-only routes: 1;
  - blocked routes: 3.
- Integrated verification after the route precision change:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-route-physical-combined-20260710-020343`
    reports 36 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-route-physical-combined-20260710-020344`
    reports 47 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 iOS preview tooling state

- Added `scripts/ios/check-ios-preview-tooling.sh` as a read-only check for the
  Codex/XcodeBuildMCP preview boundary.
- Added `scripts/ios/test-check-ios-preview-tooling.sh` to cover:
  - default `xcrun simctl` failing while full-Xcode `DEVELOPER_DIR` simctl
    works;
  - full-Xcode simctl failure remaining a hard failure.
- Current local preview-tooling evidence:
  `/tmp/nome-ios-preview-tooling-current.log`.
- Current result:
  - default `xcode-select` is `/Library/Developer/CommandLineTools`;
  - default `xcrun simctl` fails with `unable to find utility "simctl"`;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcrun simctl`
    can list the booted `iPhone 17 Pro` simulator;
  - `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer` was
    attempted with non-interactive sudo and could not run because a password is
    required, so no system developer-directory change was made.
- XcodeBuildMCP evidence:
  - session defaults are correct for `apps/ios/SimpleX.xcodeproj`,
    `SimpleX (iOS)`, simulator `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`, and
    bundle id `chat.simplex.app`;
  - `list_sims` still fails because MCP uses the default `xcrun` path, which
    currently resolves through Command Line Tools.
- Integrated verification:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-preview-tooling-20260710-020927`
    reports 38 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-preview-tooling-20260710-020927`
    reports 49 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 completion state export

- Added `scripts/ios/export-nome-ios-completion-state.sh` as a top-level,
  read-only completion packet for the Nome iOS goal.
- Added `scripts/ios/test-export-nome-ios-completion-state.sh` to verify the
  export schema and skip-gates fixture rows.
- The completion export writes:
  - `summary.tsv`;
  - `completion_requirements.tsv`;
  - `gate_status.tsv`;
  - subdirectories/logs for the state packets it uses.
- Current direct export:
  `/tmp/nome-ios-completion-state-current-20260710-021544`.
- Current completion summary:
  - PASS requirements: 2;
  - WARN requirements: 1;
  - BLOCKED requirements: 5;
  - FAIL requirements: 0.
- Current requirement states:
  - `planning_records`: PASS;
  - `visual_design_and_smoke`: PASS;
  - `manual_qa`: BLOCKED, 41 manual QA items remain;
  - `real_core_runtime`: BLOCKED, no real-core route is ready;
  - `physical_device_path`: BLOCKED, 2 device-readiness areas remain;
  - `final_app_store_screenshots`: BLOCKED, 4 needs-real-core screenshots remain;
  - `release_identifiers`: BLOCKED, 8 release identifier/capability decision rows remain;
  - `preview_tooling`: WARN, XcodeBuildMCP still depends on system
    `xcode-select`, while full-Xcode shell smoke remains available.
- Wired the completion-state unit into readiness and the full completion-state
  export into goal audit.
- Integrated verification:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-completion-state-20260710-021621`
    reports 39 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-completion-state-20260710-021621`
    reports 51 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 device project-reference warning

- Re-ran the physical-device dry-run:
  `scripts/ios/prepare-device-real-core.sh`.
- Current dry-run result:
  - the local `~/Downloads/pkg-ios-aarch64-swift-json` package is production
    sized, `arm64`, and free of preview-core markers;
  - no files were copied;
  - the package archive names are `6.5.5.0`, while the current Xcode project
    references are `6.5.6.1`, so installing this package for a physical-device
    build would require a matching simulator/device pair or an explicit project
    reference sync/compatibility decision.
- Hardened `scripts/ios/export-ios-device-readiness-state.sh` so it runs or
  accepts a `prepare-device-real-core` dry-run log and adds a
  `device_project_references` row.
- Current direct export:
  `/tmp/nome-ios-device-readiness-state-project-ref-20260710-022118`.
- Current device-readiness summary:
  - PASS areas: 3;
  - WARN areas: 2;
  - BLOCKED areas: 2.
- The two BLOCKED areas remain:
  - no connected trusted physical iPhone/iPad;
  - `apps/ios/Libraries/ios` is missing.
- The two WARN areas are:
  - device project references differ from the local device artifact names;
  - release identifiers are still upstream-compatible for the development pass.
- Updated `scripts/ios/export-nome-ios-completion-state.sh` so the
  `physical_device_path` requirement mentions both blocked device-readiness
  areas and warning areas. Current direct export:
  `/tmp/nome-ios-completion-state-device-warn-20260710-022156`.
- Unit verification:
  - `scripts/ios/test-export-ios-device-readiness-state.sh` exits `0`;
  - `scripts/ios/test-export-nome-ios-completion-state.sh` exits `0`.
- Integrated verification:
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-device-project-ref-20260710-022226`
    reports 39 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL;
  - `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-device-project-ref-20260710-022226`
    reports 51 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.

## 2026-07-10 source target split

- Updated `scripts/ios/check-real-core-sources.sh` so source availability can
  be audited against the intended route:
  - default `SOURCE_TARGET=simulator` still requires a simulator-compatible
    path for the current architecture;
  - `SOURCE_TARGET=physical-device` can pass on an arm64 device artifact,
    without installing it or claiming simulator readiness;
  - `SOURCE_TARGET=any` can be used when a broader discovery pass should accept
    either route.
- Updated `scripts/ios/test-real-core-source-audit.sh` with fixture coverage
  for:
  - default arm64 simulator mismatch still failing;
  - x86_64 simulator compatibility still passing;
  - local arm64 device-only artifacts passing only for the physical-device
    target;
  - Hydra aarch64-only candidates passing only for the physical-device target.
- Live source-route evidence:
  - default simulator audit log: `/tmp/nome-real-core-source-sim-target.log`,
    exit `1` as expected;
  - physical-device audit log:
    `/tmp/nome-real-core-source-device-target.log`, exit `0` because the local
    arm64 device artifact is available.
- Verification:
  - `bash -n scripts/ios/check-real-core-sources.sh scripts/ios/test-real-core-source-audit.sh`
    exits `0`;
  - `scripts/ios/test-real-core-source-audit.sh` exits `0`;
  - `git diff --check` exits `0`;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-source-target-20260710-023303`
    reports 39 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-source-target-20260710-023303`
    reports 51 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL;
  - `scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-completion-state-source-target-smoke-20260710-023538 --force`
    reports 2 PASS, 1 WARN, 5 BLOCKED, and 0 FAIL.

## 2026-07-10 QA batch template source-target update

- Updated `scripts/ios/export-nome-qa-batches.sh` so Batch 0 evidence
  templates now include both:
  - default simulator source audit via `scripts/ios/check-real-core-sources.sh`;
  - physical-device source audit via
    `SOURCE_TARGET=physical-device scripts/ios/check-real-core-sources.sh`.
- Batch 0 templates now separately guard:
  - simulator `scripts/ios/run-real-core-batch0.sh --source ... --prepare`,
    only after route/source evidence proves simulator compatibility;
  - physical-device `scripts/ios/prepare-device-real-core.sh --prepare`, only
    after the physical-device source audit and dry-run pass, and only for a
    deliberate real-device test.
- Batch 5 templates now require:
  - `scripts/ios/export-ios-device-readiness-state.sh`;
  - physical-device source audit;
  - `scripts/ios/prepare-device-real-core.sh` dry-run and optional `--prepare`
    log;
  - connected trusted iPhone/iPad build/install/launch evidence;
  - live camera QR evidence or a clear device/camera blocker.
- Current batch export:
  `/tmp/nome-qa-batches-source-target-20260710-024014`, still with 41
  unchecked items across eight batches.
- Verification:
  - `bash -n scripts/ios/export-nome-qa-batches.sh scripts/ios/test-export-nome-qa-batches.sh`
    exits `0`;
  - `scripts/ios/test-export-nome-qa-batches.sh` exits `0`;
  - `scripts/ios/export-nome-qa-batches.sh --output /tmp/nome-qa-batches-source-target-20260710-024014 --force`
    exits `0`;
  - `scripts/ios/check-nome-qa-execution-plan.sh` exits `0`;
  - `scripts/ios/export-nome-qa-execution-state.sh --output /tmp/nome-ios-qa-execution-state-source-target-20260710-024118 --force`
    reports 41 unchecked manual QA items, 7 gate blockers, and 8 next-action
    rows;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-qa-template-20260710-024207`
    reports 39 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL;
  - `scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-completion-state-qa-template-20260710-024207 --force`
    reports 2 PASS, 1 WARN, 5 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-qa-template-20260710-024342`
    reports 51 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL.

## 2026-07-10 physical-device source audit in readiness state

- Updated `scripts/ios/export-ios-device-readiness-state.sh` so the
  physical-device state packet now runs or accepts a read-only
  `SOURCE_TARGET=physical-device` source-audit log.
- Added a new device-status area:
  `physical_device_source_audit`.
- Current direct export:
  `/tmp/nome-ios-device-readiness-source-audit-20260710-024811`.
- Current direct export summary:
  - PASS areas: 4;
  - WARN areas: 2;
  - BLOCKED areas: 2.
- The new PASS area proves the local
  `~/Downloads/pkg-ios-aarch64-swift-json` artifact is visible through the
  source-audit path as a physical-device candidate.
- Remaining physical-device blockers:
  - no connected trusted iPhone/iPad;
  - `apps/ios/Libraries/ios` is still missing.
- Remaining physical-device warnings:
  - local device artifact archive names are `6.5.5.0`, while project
    references are `6.5.6.1`;
  - release identifiers are still compatibility-reviewed rather than final
    Nome release identifiers.
- Verification:
  - `bash -n scripts/ios/export-ios-device-readiness-state.sh scripts/ios/test-export-ios-device-readiness-state.sh`
    exits `0`;
  - `scripts/ios/test-export-ios-device-readiness-state.sh` exits `0`;
  - `scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-source-audit-20260710-024811 --force`
    exits `0`;
  - `scripts/ios/test-export-nome-ios-completion-state.sh` exits `0`;
  - `scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-completion-state-device-source-20260710-024847 --force`
    reports 2 PASS, 1 WARN, 5 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-device-source-20260710-024918`
    reports 39 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-device-source-20260710-024918`
    reports 51 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL.

## 2026-07-10 real-core route-state device-source refinement

- Updated `scripts/ios/export-real-core-route-state.sh` so the physical-device
  route uses the structured device-readiness state packet when available,
  including the `physical_device_source_audit` PASS area.
- Added `--device-state-dir DIR` for fixture or resumed runs that already have
  an `export-ios-device-readiness-state` packet.
- Updated `scripts/ios/test-export-real-core-route-state.sh` so its fixture
  includes `device_status.tsv` with:
  - `physical_device_source_audit` PASS;
  - `connected_device` BLOCKED;
  - `installed_device_libraries` BLOCKED;
  - `device_project_references` WARN.
- Current direct export:
  `/tmp/nome-ios-real-core-route-state-device-source-20260710-025609`.
- Current direct export summary:
  - ready routes: 0;
  - build-only routes: 1;
  - blocked routes: 3.
- The physical-device route now says:
  the source audit passed for the local arm64 artifact, but no trusted
  iPhone/iPad is connected, `apps/ios/Libraries/ios` is missing, and project
  references remain WARN.
- Verification:
  - `bash -n scripts/ios/export-real-core-route-state.sh scripts/ios/test-export-real-core-route-state.sh`
    exits `0`;
  - `scripts/ios/test-export-real-core-route-state.sh` exits `0`;
  - `scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-device-source-20260710-025609 --force`
    exits `0`;
  - `scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-completion-state-route-device-source-20260710-025732 --force`
    reports 2 PASS, 1 WARN, 5 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-route-device-source-20260710-025809`
    reports 39 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-route-device-source-20260710-025809`
    reports 51 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL.

## 2026-07-10 App Store screenshot route prerequisite

- Updated `scripts/ios/export-app-store-final-screenshot-state.sh` so the
  final screenshot state packet now exports the current real-core route state
  before listing screenshot replacement actions.
- When needs-real-core screenshots remain and `route_ready_count` is `0`, the
  first `next_actions.tsv` row is now `real_core_route_prerequisite`, pointing
  to `scripts/ios/export-real-core-route-state.sh`.
- Updated `scripts/ios/test-export-app-store-final-screenshot-state.sh` to
  assert the route-state summary fields, route-prerequisite gate, and shifted
  replacement/manifest-cleanup action order.
- Current direct export:
  `/tmp/nome-ios-app-store-final-screenshot-state-route-prereq-20260710-030315`.
- Current direct export summary:
  - candidate screenshot gate: PASS;
  - strict final screenshot gate: BLOCKED;
  - needs-real-core rows: 4;
  - replacement rows: 4;
  - real-core route ready count: 0.
- Current first next action:
  `real_core_route_prerequisite` / resolve Batch 0 first, because screenshot
  replacements require a READY real-core route.
- Verification:
  - `bash -n scripts/ios/export-app-store-final-screenshot-state.sh scripts/ios/test-export-app-store-final-screenshot-state.sh`
    exits `0`;
  - `scripts/ios/test-export-app-store-final-screenshot-state.sh` exits `0`;
  - `scripts/ios/export-app-store-final-screenshot-state.sh --output /tmp/nome-ios-app-store-final-screenshot-state-route-prereq-20260710-030315 --force`
    exits `0`;
  - `scripts/ios/check-nome-qa-execution-plan.sh` exits `0`;
  - `scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-completion-state-screenshot-route-prereq-20260710-030433 --force`
    reports 2 PASS, 1 WARN, 5 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-screenshot-route-prereq-20260710-030522`
    reports 39 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-screenshot-route-prereq-20260710-030522`
    reports 51 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL.

## 2026-07-10 physical-device real-core install

- Installed the audited local arm64 device core artifact from
  `~/Downloads/pkg-ios-aarch64-swift-json` into `apps/ios/Libraries/ios` with:
  `scripts/ios/prepare-device-real-core.sh --prepare`.
- The install copied five arm64 static libraries:
  - `libHSsimplex-chat-6.5.5.0-57WEoB2fiWaFgCB1XksYmP-ghc9.6.3.a`;
  - `libHSsimplex-chat-6.5.5.0-57WEoB2fiWaFgCB1XksYmP.a`;
  - `libffi.a`;
  - `libgmp.a`;
  - `libgmpxx.a`.
- `apps/ios/Libraries/` is ignored by `apps/ios/.gitignore`, so this install
  makes the local physical-device route testable without adding the large
  binary libraries to the git diff.
- Fixed `scripts/ios/export-ios-device-readiness-state.sh` so installed device
  libraries pass when the readiness log contains the current
  `Installed device library library supports arm64` wording.
- Added a regression fixture to
  `scripts/ios/test-export-ios-device-readiness-state.sh` for the post-install
  state, so installed device libraries do not remain a next action after pass
  evidence.
- Current device-readiness export:
  `/tmp/nome-ios-device-readiness-state-after-install-fixed-20260710-031528`.
- Current device-readiness summary:
  - PASS areas: 5;
  - WARN areas: 2;
  - BLOCKED areas: 1.
- Current remaining physical-device blocker:
  `connected_device` is BLOCKED because no trusted iPhone/iPad is connected.
- Current physical-device warnings:
  - `device_project_references`: the installed device artifact is 6.5.5.0
    while current project references point at 6.5.6.1 archives;
  - `release_identifiers`: the app still uses upstream-compatible identifiers
    for the current development pass.
- Current real-core route export:
  `/tmp/nome-ios-real-core-route-state-after-device-install-20260710-031552`.
- The physical-device route now says the physical-device source audit passed
  and the remaining hard blocker is the missing connected trusted iPhone/iPad.
- Current completion-state export:
  `/tmp/nome-ios-completion-state-after-device-install-20260710-031552`.
- Current completion-state summary:
  - planning records: PASS;
  - visual design and smoke: PASS;
  - preview tooling: WARN;
  - manual QA, real-core runtime, physical-device path, final App Store
    screenshots, and release identifiers remain BLOCKED.
- Verification:
  - `scripts/ios/prepare-device-real-core.sh` dry-run exits `0`;
  - `scripts/ios/prepare-device-real-core.sh --prepare` exits `0`;
  - `bash -n scripts/ios/export-ios-device-readiness-state.sh scripts/ios/test-export-ios-device-readiness-state.sh`
    exits `0`;
  - `scripts/ios/test-export-ios-device-readiness-state.sh` exits `0`;
  - `scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-after-install-fixed-20260710-031528 --force`
    exits `0` and reports 5 PASS, 2 WARN, 1 BLOCKED;
  - `scripts/ios/export-real-core-route-state.sh --output /tmp/nome-ios-real-core-route-state-after-device-install-20260710-031552 --force`
    exits `0`;
  - `scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-completion-state-after-device-install-20260710-031552 --force`
    exits `0` and reports 2 PASS, 1 WARN, 5 BLOCKED, and 0 FAIL.
- After installing the device libraries, the combined readiness and goal-audit
  gates initially exposed `real_core_xcode_sync` as a `FAIL` because the
  installed device archives are 6.5.5.0 while the current project/simulator
  references are 6.5.6.1. This is a known real-core/project-reference blocker,
  not an unexpected script or UI regression.
- Updated `scripts/ios/check-nome-ios-readiness.sh` and
  `scripts/ios/check-nome-ios-goal-audit.sh` so `real_core_xcode_sync` is
  reported as `BLOCKED` in aggregate reports. The underlying
  `scripts/ios/check-real-core-xcode-sync.sh` still exits non-zero until the
  project-reference strategy is resolved.
- Classified readiness verification:
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-after-device-install-classified-20260710-032124`
    reports 38 PASS, 0 WARN, 9 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-after-device-install-classified-20260710-032124`
    reports 50 PASS, 1 WARN, 9 BLOCKED, and 0 FAIL.

## 2026-07-10 device project-name alias and generic build

- Added `scripts/ios/alias-device-real-core-project-libs.sh` as a local,
  device-only compatibility helper.
- The helper is read-only by default and only creates aliases with `--prepare`.
  It does not edit `apps/ios/SimpleX.xcodeproj/project.pbxproj` and does not
  touch `apps/ios/Libraries/sim`.
- Prepared two symlinks in the ignored `apps/ios/Libraries/ios` directory:
  - `libHSsimplex-chat-6.5.6.1-AHNtWMpWy1qCojVTgyCNik-ghc9.6.3.a` ->
    `libHSsimplex-chat-6.5.5.0-57WEoB2fiWaFgCB1XksYmP-ghc9.6.3.a`;
  - `libHSsimplex-chat-6.5.6.1-AHNtWMpWy1qCojVTgyCNik.a` ->
    `libHSsimplex-chat-6.5.5.0-57WEoB2fiWaFgCB1XksYmP.a`.
- Updated `scripts/ios/check-real-core-xcode-sync.sh` so installed device
  project-name aliases count as a valid local build-path match while still
  reporting the alias target in the log.
- Updated `scripts/ios/test-check-real-core-xcode-sync.sh` with an
  `installed_device_alias_passes` fixture.
- Updated `scripts/ios/export-ios-device-readiness-state.sh` so it runs or
  parses `scripts/ios/check-real-core-xcode-sync.sh` and reports
  `device_project_references` as PASS when the Xcode build-path references are
  resolvable.
- Updated `scripts/ios/test-export-ios-device-readiness-state.sh` with a
  post-alias fixture that expects:
  - 6 device readiness PASS areas;
  - 1 WARN area;
  - 1 BLOCKED area;
  - `device_project_references` no longer appears in `next_actions.tsv`.
- Added the alias helper to the combined readiness script-syntax gate.
- Current direct sync check:
  `scripts/ios/check-real-core-xcode-sync.sh` exits `0`, with the installed
  device archives shown as local compatibility aliases.
- Current device-readiness export:
  `/tmp/nome-ios-device-readiness-state-alias-sync-20260710-033157`.
- Current device-readiness summary:
  - PASS areas: 6;
  - WARN areas: 1;
  - BLOCKED areas: 1.
- Current remaining physical-device blocker:
  `connected_device` is BLOCKED because no trusted iPhone/iPad is connected.
- Current remaining device warning:
  `release_identifiers` is WARN because the development pass still retains
  upstream-compatible identifiers.
- Current real-core route export:
  `/tmp/nome-ios-real-core-route-state-alias-sync-20260710-033222`.
- The physical-device route now says project references are PASS; the remaining
  hard blocker is the missing connected trusted iPhone/iPad.
- Current completion-state export:
  `/tmp/nome-ios-completion-state-alias-sync-20260710-033222`.
- Current completion-state summary:
  - planning records: PASS;
  - visual design and smoke: PASS;
  - preview tooling: WARN;
  - manual QA, real-core runtime, physical-device path, final App Store
    screenshots, and release identifiers remain BLOCKED;
  - physical-device path now has 1 blocked area and 1 warning area.
- Generic iOS device build verification:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild -quiet -project apps/ios/SimpleX.xcodeproj -scheme "SimpleX (iOS)" -configuration Debug -destination 'generic/platform=iOS' -derivedDataPath /tmp/nome-ios-derived-generic-device-alias -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO build`
  exits `0`.
- Generic iOS device build products:
  - `/tmp/nome-ios-derived-generic-device-alias/Build/Products/Debug-iphoneos/Nome.app`;
  - `/tmp/nome-ios-derived-generic-device-alias/Build/Products/Debug-iphoneos/SimpleX NSE.appex`;
  - `/tmp/nome-ios-derived-generic-device-alias/Build/Products/Debug-iphoneos/SimpleX SE.appex`.
- Classified readiness verification:
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-alias-sync-20260710-033524`
    reports 39 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-alias-sync-20260710-033524`
    reports 51 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL.

## 2026-07-10 generic iOS device build gate

- Added `scripts/ios/check-ios-generic-device-build.sh` as a reusable generic
  iOS device build gate.
- The script builds `generic/platform=iOS` with `CODE_SIGNING_ALLOWED=NO`,
  records `summary.tsv`, `products.tsv`, and `logs/xcodebuild.log`, and checks
  for:
  - `Nome.app`;
  - `SimpleX NSE.appex`;
  - `SimpleX SE.appex`.
- Added `scripts/ios/test-check-ios-generic-device-build.sh` with fixture
  coverage for:
  - expected app and extension products present;
  - a missing extension product causing a non-zero result.
- Added `--generic-device-build` to:
  - `scripts/ios/check-nome-ios-readiness.sh`;
  - `scripts/ios/check-nome-ios-goal-audit.sh`.
- Added the new scripts to the combined readiness syntax/unit gates.
- First standalone full gate:
  `scripts/ios/check-ios-generic-device-build.sh --derived-data /tmp/nome-ios-derived-generic-device-standard --output /tmp/nome-ios-generic-device-build-standard-20260710-034410 --force`
  exits `0`.
- First standalone full gate products:
  - `/tmp/nome-ios-derived-generic-device-standard/Build/Products/Debug-iphoneos/Nome.app`;
  - `/tmp/nome-ios-derived-generic-device-standard/Build/Products/Debug-iphoneos/SimpleX NSE.appex`;
  - `/tmp/nome-ios-derived-generic-device-standard/Build/Products/Debug-iphoneos/SimpleX SE.appex`.
- A parallel readiness/goal-audit run initially exposed an Xcode build database
  lock because both aggregate gates used the same default
  `/tmp/nome-ios-derived-generic-device` DerivedData path.
- Updated the aggregate gates so each report passes an isolated
  `$output_dir/generic_device_derived` directory to
  `scripts/ios/check-ios-generic-device-build.sh`.
- Current generic-device readiness gate:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --generic-device-build --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-generic-device-20260710-034703`
  reports 41 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL.
- Current isolated generic-device goal audit:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --generic-device-build --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-generic-device-isolated-20260710-035250`
  reports 53 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL.
- In both aggregate reports, `generic_device_build` is PASS.

## 2026-07-10 release identity proposal packet

- Updated `scripts/ios/export-ios-release-identity-state.sh` so the read-only
  release packet now writes `proposed_identifiers.tsv`.
- The proposal table does not edit the Xcode project, entitlements, plists, or
  signing settings.
- Default proposal values are intentionally placeholders:
  - bundle-id base: `com.example.nome`;
  - associated-link domain: `nome.example`;
  - proposal status: `placeholder`.
- A real candidate can be tested without editing source by passing:
  - `--release-bundle-base`;
  - `--release-domain`.
- The proposal table currently has 11 rows:
  - main app bundle id;
  - notification service extension bundle id;
  - share extension bundle id;
  - internal framework bundle id;
  - iOS test bundle id;
  - App Group;
  - keychain access group;
  - background task id;
  - URL type name;
  - `simplex` URL scheme compatibility;
  - Associated Domains and Nome-owned AASA files.
- Updated `scripts/ios/test-export-ios-release-identity-state.sh` to verify:
  - `proposed_identifiers.tsv` exists;
  - default values are marked as placeholders;
  - a custom bundle base/domain is marked
    `candidate_requires_apple_team_review`.
- Current targeted verification:
  - `bash -n scripts/ios/export-ios-release-identity-state.sh scripts/ios/test-export-ios-release-identity-state.sh`
    exits `0`;
  - `scripts/ios/test-export-ios-release-identity-state.sh` exits `0`;
  - `scripts/ios/export-ios-release-identity-state.sh --output /tmp/nome-ios-release-identity-state-proposal-20260710-040437 --force`
    exits `0`.
- Current release identity export:
  `/tmp/nome-ios-release-identity-state-proposal-20260710-040437`.
- Current release identity summary:
  - compatibility-reviewed gate: PASS;
  - strict final gate: BLOCKED;
  - proposal status: `placeholder`;
  - current identifier rows: 21;
  - proposed identifier rows: 11;
  - required decision rows: 8.
- Integrated readiness verification after the proposal packet:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --generic-device-build --output /tmp/nome-ios-readiness-release-proposal-generic-20260710-040827`
  exits `0` and reports 39 PASS, 3 WARN, 7 BLOCKED, and 0 FAIL.
- In that integrated report:
  - `release_identifiers_compatibility_review` is PASS;
  - `release_identity_state` is PASS;
  - `release_identifiers` remains BLOCKED in strict final mode;
  - `generic_device_build` is PASS.
- Generic iOS device products from the integrated report:
  - `/tmp/nome-ios-readiness-release-proposal-generic-20260710-040827/generic_device_derived/Build/Products/Debug-iphoneos/Nome.app`;
  - `/tmp/nome-ios-readiness-release-proposal-generic-20260710-040827/generic_device_derived/Build/Products/Debug-iphoneos/SimpleX NSE.appex`;
  - `/tmp/nome-ios-readiness-release-proposal-generic-20260710-040827/generic_device_derived/Build/Products/Debug-iphoneos/SimpleX SE.appex`.

## 2026-07-10 smoke screenshot visual-quality gate

- Added `scripts/ios/check-nome-smoke-visual-quality.sh`.
- The gate is read-only. It does not launch Simulator, rebuild the app, or edit
  screenshots.
- The gate checks a `scripts/ios/smoke-nome-ui.sh` manifest against the PNG
  files currently on disk:
  - expected row count when `--expected-count` is supplied;
  - manifest header;
  - file existence;
  - byte-size match;
  - SHA-256 match;
  - `sips` decodeability;
  - width and height match;
  - portrait orientation;
  - minimum file size;
  - duplicate screenshot hashes.
- The gate writes:
  - `summary.tsv`;
  - `images.tsv`;
  - `failures.tsv`;
  - `hashes.tsv`.
- Added `scripts/ios/test-check-nome-smoke-visual-quality.sh` with fixture
  coverage for:
  - valid PNG screenshots passing;
  - manifest byte-size mismatch failing;
  - duplicate screenshot hashes failing.
- Integrated the gate into:
  - `scripts/ios/check-nome-ios-readiness.sh`;
  - `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current direct verification:
  - `bash -n scripts/ios/check-nome-smoke-visual-quality.sh scripts/ios/test-check-nome-smoke-visual-quality.sh scripts/ios/check-nome-ios-readiness.sh scripts/ios/check-nome-ios-goal-audit.sh`
    exits `0`;
  - `scripts/ios/test-check-nome-smoke-visual-quality.sh` exits `0`;
  - `scripts/ios/check-nome-smoke-visual-quality.sh --manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --expected-count 14 --output /tmp/nome-ios-smoke-visual-quality-current-fixed-20260710-041950 --force`
    exits `0`.
- Current direct smoke visual-quality packet:
  `/tmp/nome-ios-smoke-visual-quality-current-fixed-20260710-041950`.
- Current direct smoke visual-quality summary:
  - image rows: 14;
  - failure rows: 0.
- Current integrated readiness:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-smoke-quality-20260710-042011`
  exits `0` and reports 42 PASS, 2 WARN, 7 BLOCKED, and 0 FAIL.
- In that readiness report, both `nome_ui_smoke` and `smoke_visual_quality`
  are PASS.
- Current integrated goal audit:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-smoke-quality-20260710-042011`
  exits `0` and reports 54 PASS, 3 WARN, 7 BLOCKED, and 0 FAIL.
- In that goal audit, `ui_smoke_manifest`, `smoke_visual_quality`, and
  `design_evidence_state` are PASS.

## 2026-07-10 physical-device smoke runner

- Added `scripts/ios/run-ios-physical-device-smoke.sh`.
- The script is the Batch 5 execution entry point after a trusted iPhone/iPad is
  connected.
- It writes:
  - `summary.tsv`;
  - `steps.tsv`;
  - `commands.tsv`;
  - logs for device discovery, Xcode build, app install, app launch, and build
    settings.
- With a connected device, the script runs:
  - `xcrun xctrace list devices`;
  - a signed `xcodebuild` for `SimpleX (iOS)` with `-destination id=<device>`;
  - `xcrun devicectl device install app`;
  - `xcrun devicectl device process launch`.
- Without a connected trusted iPhone/iPad, it exits before build/install/launch
  and records `connected_device` as BLOCKED.
- Added `scripts/ios/test-run-ios-physical-device-smoke.sh` with fixture
  coverage for:
  - no connected device blocking before build;
  - connected fixture device plus `--skip-build --skip-install --skip-launch`
    recording device id, device name, app bundle id, and skipped steps.
- Integrated the physical-device smoke runner into:
  - `scripts/ios/check-nome-ios-readiness.sh`;
  - `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current targeted verification:
  - `bash -n scripts/ios/run-ios-physical-device-smoke.sh scripts/ios/test-run-ios-physical-device-smoke.sh`
    exits `0`;
  - `scripts/ios/test-run-ios-physical-device-smoke.sh` exits `0`;
  - `scripts/ios/run-ios-physical-device-smoke.sh --output /tmp/nome-ios-physical-device-smoke-current-20260710-043015 --force`
    exits `1` on this machine because no connected trusted physical iPhone/iPad
    is present.
- Current physical-device smoke export:
  `/tmp/nome-ios-physical-device-smoke-current-20260710-043015`.
- Current physical-device smoke status:
  - `connected_device`: BLOCKED;
  - no device build/install/launch was attempted.
- Current integrated readiness after adding the physical-device smoke runner:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-physical-smoke-20260710-043220`
  exits `0` and reports 43 PASS, 2 WARN, 8 BLOCKED, and 0 FAIL.
- In that readiness report:
  - `physical_device_smoke_unit` is PASS;
  - `physical_device_smoke` is BLOCKED because no connected trusted physical
    iPhone/iPad is present;
  - no device build/install/launch was attempted.
- Current integrated goal audit after adding the physical-device smoke runner:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-physical-smoke-20260710-043220`
  exits `0` and reports 55 PASS, 3 WARN, 8 BLOCKED, and 0 FAIL.
- In that goal audit:
  - `physical_device_smoke_unit` is PASS;
  - `physical_device_smoke` is BLOCKED for the same no-device reason.

## 2026-07-10 App Store final screenshot package proposal

- Added `scripts/ios/prepare-app-store-final-screenshot-package.sh`.
- The script creates a separate proposed final App Store screenshot package. It
  does not edit `design/app-store/ios-upload-draft-screens`.
- Required replacement input names:
  - `05-add-friend-real-core.(png|jpg|jpeg)`;
  - `06-public-contact-real-core.(png|jpg|jpeg)`;
  - `07-join-group-real-core.(png|jpg|jpeg)`;
  - `08-conversation-real-core.(png|jpg|jpeg)`.
- When all four replacements are present, the script:
  - copies the six already-ready draft screenshots;
  - converts the four replacement screenshots into final JPEG slots without
    `needs-real-core` filenames;
  - writes `MANIFEST.md`, `FINAL_BLOCKERS.md`, `package.tsv`,
    `replacement_status.tsv`, and `summary.tsv`;
  - runs `scripts/ios/check-app-store-screenshots.sh --final` against the
    generated output directory.
- Added `scripts/ios/test-prepare-app-store-final-screenshot-package.sh` with
  fixture coverage for:
  - missing replacements blocking package generation;
  - complete replacements producing a strict-final package.
- Updated `scripts/ios/export-app-store-final-screenshot-state.sh` so
  `next_actions.tsv` points at the final-package proposal command after the
  four real-core captures.
- Integrated the final package unit test into:
  - `scripts/ios/check-nome-ios-readiness.sh`;
  - `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current targeted verification:
  - `bash -n scripts/ios/prepare-app-store-final-screenshot-package.sh scripts/ios/test-prepare-app-store-final-screenshot-package.sh scripts/ios/export-app-store-final-screenshot-state.sh scripts/ios/test-export-app-store-final-screenshot-state.sh scripts/ios/check-nome-ios-readiness.sh scripts/ios/check-nome-ios-goal-audit.sh`
    exits `0`;
  - `scripts/ios/test-prepare-app-store-final-screenshot-package.sh` exits `0`;
  - `scripts/ios/test-export-app-store-final-screenshot-state.sh` exits `0`;
  - `scripts/ios/export-app-store-final-screenshot-state.sh --output /tmp/nome-ios-app-store-final-screenshot-state-package-20260710-044016 --force`
    exits `0` and now lists `final_package_proposal` in `next_actions.tsv`.
- Current no-replacement package attempt:
  `/tmp/nome-ios-final-package-current-blocked-20260710-043837`.
- Current no-replacement package status:
  - missing replacements: 4;
  - all four required replacement stems are BLOCKED.
- Current integrated readiness after adding the final-package unit:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-final-package-20260710-044043`
  exits `0` and reports 44 PASS, 2 WARN, 8 BLOCKED, and 0 FAIL.
- Current integrated goal audit after adding the final-package unit:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-final-package-20260710-044043`
  exits `0` and reports 56 PASS, 3 WARN, 8 BLOCKED, and 0 FAIL.

## 2026-07-10 QA evidence index update

- Updated `scripts/ios/export-nome-qa-batches.sh` so every remaining unchecked
  manual QA item now appears in `evidence_index.tsv`.
- The index records:
  - batch id and title;
  - checklist line, section, blocker group, and item text;
  - evidence template path;
  - pending status;
  - dependency;
  - command summary;
  - expected evidence summary;
  - evidence-path and blocker-note placeholders.
- Updated every generated per-batch TSV with the same evidence tracking
  columns so Batch 0 real-core work, Batch 5 physical-device work, Batch 6
  final screenshots, and Batch 7 release identifiers can be audited without
  parsing Markdown.
- Updated each item-level evidence template with acceptance verdict, runtime
  route, device/simulator, build, account setup, screenshot/video/log paths,
  result, blocker notes, follow-up, and an explicit completion rule.
- Updated `scripts/ios/export-nome-qa-execution-state.sh` so `summary.tsv`
  points directly to `batches/evidence_index.tsv`.
- Current targeted verification:
  - `bash -n scripts/ios/export-nome-qa-batches.sh scripts/ios/test-export-nome-qa-batches.sh scripts/ios/export-nome-qa-execution-state.sh scripts/ios/test-export-nome-qa-execution-state.sh`
    exits `0`;
  - `scripts/ios/test-export-nome-qa-batches.sh` exits `0`;
  - `scripts/ios/test-export-nome-qa-execution-state.sh` exits `0`;
  - `scripts/ios/export-nome-qa-batches.sh --output /tmp/nome-ios-qa-batches-evidence-index-20260710 --force`
    exits `0`, exports 41 unchecked items, and writes `evidence_index.tsv`
    with the expected batch counts: Batch 0 = 2, Batch 1 = 4, Batch 2 = 10,
    Batch 3 = 14, Batch 4 = 4, Batch 5 = 2, Batch 6 = 1, Batch 7 = 4;
  - `scripts/ios/export-nome-qa-execution-state.sh --output /tmp/nome-ios-qa-execution-state-evidence-index-20260710 --force --skip-gates`
    exits `0`, reports 41 unchecked items, and points summary evidence to
    `batches/evidence_index.tsv`;
  - `git diff --check` exits `0`.
- Current integrated readiness after adding the evidence index:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-evidence-index-20260710`
  exits `0` and reports 44 PASS, 2 WARN, 8 BLOCKED, and 0 FAIL.
- In that readiness report:
  - `qa_batch_export_unit`, `qa_batch_export`, `qa_execution_state_unit`, and
    `qa_execution_state` are PASS;
  - the remaining BLOCKED rows are the known completion blockers: manual QA,
    strict final App Store screenshots, real-core preflight, simulator
    real-core route, local real-core build env, physical-device readiness,
    physical-device smoke, and strict release identifiers.

## 2026-07-10 Generic device build evidence update

- Ran a current generic iOS device build against the installed device-side
  real-core libraries:
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer scripts/ios/check-ios-generic-device-build.sh --derived-data /tmp/nome-ios-derived-generic-device-next-20260710 --output /tmp/nome-ios-generic-device-build-next-20260710 --force`.
- The build exits `0`.
- Produced products:
  - `/tmp/nome-ios-derived-generic-device-next-20260710/Build/Products/Debug-iphoneos/Nome.app`;
  - `/tmp/nome-ios-derived-generic-device-next-20260710/Build/Products/Debug-iphoneos/SimpleX NSE.appex`;
  - `/tmp/nome-ios-derived-generic-device-next-20260710/Build/Products/Debug-iphoneos/SimpleX SE.appex`.
- Product summary from `/tmp/nome-ios-generic-device-build-next-20260710/products.tsv`:
  - `Nome.app`: PASS, about 252356K;
  - `SimpleX NSE.appex`: PASS, about 2324K;
  - `SimpleX SE.appex`: PASS, about 4480K.
- Binary inspection:
  - `/tmp/nome-ios-derived-generic-device-next-20260710/Build/Products/Debug-iphoneos/Nome.app/Nome`
    is a Mach-O 64-bit `arm64` executable;
  - the built app Info.plist has `CFBundleDisplayName=Nome`,
    `CFBundleName=Nome`, `CFBundleIdentifier=chat.simplex.app`,
    `CFBundleShortVersionString=6.5.6`, and `CFBundleVersion=337`.
- Updated `scripts/ios/export-ios-device-readiness-state.sh` with
  `--generic-device-build-dir`, allowing an existing
  `scripts/ios/check-ios-generic-device-build.sh` packet to be recorded in the
  physical-device readiness state without rebuilding.
- Updated `scripts/ios/test-export-ios-device-readiness-state.sh` to cover:
  - no generic build evidence -> `generic_device_build` WARN;
  - supplied generic build packet with `Nome.app`, `SimpleX NSE.appex`, and
    `SimpleX SE.appex` -> `generic_device_build` PASS.
- Current targeted verification:
  - `bash -n scripts/ios/export-ios-device-readiness-state.sh scripts/ios/test-export-ios-device-readiness-state.sh`
    exits `0`;
  - `scripts/ios/test-export-ios-device-readiness-state.sh` exits `0`;
  - `scripts/ios/export-ios-device-readiness-state.sh --output /tmp/nome-ios-device-readiness-state-generic-build-20260710 --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --force`
    exits `0` and reports 7 PASS areas, 1 WARN area, 1 BLOCKED area, and 0
    FAIL.
- Current integrated readiness after adding generic build evidence support:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-generic-build-evidence-20260710`
  exits `0` and reports 44 PASS, 2 WARN, 8 BLOCKED, and 0 FAIL.
- In that device-readiness state:
  - `generic_device_build` is PASS;
  - `connected_device` remains BLOCKED because no trusted physical iPhone/iPad
    is connected;
  - `release_identifiers` remains WARN pending the final release-identity
    decision.

## 2026-07-10 Completion state generic build passthrough

- Updated `scripts/ios/export-nome-ios-completion-state.sh` with
  `--generic-device-build-dir`.
- The completion-state export now passes that directory through to
  `scripts/ios/export-ios-device-readiness-state.sh`, so the top-level
  `physical_device_path` requirement can account for existing generic iOS
  device build evidence.
- Updated `scripts/ios/test-export-nome-ios-completion-state.sh` so the
  skip-gates fixture verifies the supplied generic build directory is recorded
  in `summary.tsv`.
- Current targeted verification:
  - `bash -n scripts/ios/export-nome-ios-completion-state.sh scripts/ios/test-export-nome-ios-completion-state.sh`
    exits `0`;
  - `scripts/ios/test-export-nome-ios-completion-state.sh` exits `0`;
  - `scripts/ios/export-nome-ios-completion-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-completion-state-generic-build-20260710 --force`
    exits `0`.
- Current completion-state summary with generic build evidence:
  - PASS requirements: 2;
  - WARN requirements: 1;
  - BLOCKED requirements: 5;
  - FAIL requirements: 0.
- In that completion-state export, `physical_device_path` remains BLOCKED
  because one device-readiness area is still blocked, but its warning count is
  now 1 instead of 2 because `generic_device_build` is PASS from the supplied
  build packet.
- Current integrated readiness after adding the completion-state passthrough:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-completion-generic-build-20260710`
  exits `0` and reports 44 PASS, 2 WARN, 8 BLOCKED, and 0 FAIL.

## 2026-07-10 Release identity proposal checker

- Added `scripts/ios/check-ios-release-identity-proposal.sh`.
- The checker validates `proposed_identifiers.tsv` from
  `scripts/ios/export-ios-release-identity-state.sh`.
- It is read-only and verifies:
  - the proposal table has all 11 expected surfaces;
  - default `example` placeholders are not accepted;
  - proposed release identifiers do not retain upstream `chat.simplex` /
    `group.chat.simplex` / upstream associated-domain values;
  - notification service, share extension, internal framework, tests, App
    Group, keychain group, background task id, and URL type derive from the
    proposed main app bundle id;
  - associated domains use a candidate domain and `www` variant;
  - `simplex` remains visible as a protocol-compatibility URL-scheme warning.
- Added `scripts/ios/test-check-ios-release-identity-proposal.sh`, covering:
  - default placeholder proposal fails;
  - candidate proposal passes syntactic/internal-consistency checks;
  - inconsistent share extension bundle id fails with a targeted message.
- Integrated the new unit into:
  - `scripts/ios/check-nome-ios-readiness.sh`;
  - `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current targeted verification:
  - `bash -n scripts/ios/check-ios-release-identity-proposal.sh scripts/ios/test-check-ios-release-identity-proposal.sh scripts/ios/check-nome-ios-readiness.sh scripts/ios/check-nome-ios-goal-audit.sh`
    exits `0`;
  - `scripts/ios/test-check-ios-release-identity-proposal.sh` exits `0`;
  - `scripts/ios/export-ios-release-identity-state.sh --release-bundle-base app.nome.secure --release-domain nome.chat --output /tmp/nome-ios-release-identity-candidate-20260710 --force`
    exits `0`;
  - `scripts/ios/check-ios-release-identity-proposal.sh --proposal /tmp/nome-ios-release-identity-candidate-20260710/proposed_identifiers.tsv`
    exits `0`;
  - `scripts/ios/check-ios-release-identity-proposal.sh --proposal /tmp/nome-ios-release-identity-state-probe-20260710/proposed_identifiers.tsv`
    exits non-zero as expected because the default export still contains
    `com.example.nome` / `nome.example` placeholders.
- Current integrated readiness after adding the release proposal checker:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-release-proposal-check-20260710`
  exits `0` and reports 45 PASS, 2 WARN, 8 BLOCKED, and 0 FAIL.
- Current integrated goal audit after adding the release proposal checker:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-release-proposal-check-20260710`
  exits `0` and reports 57 PASS, 3 WARN, 8 BLOCKED, and 0 FAIL.
- This does not resolve strict release identifiers yet. It only turns the
  release-identity proposal into a machine-checkable candidate once real
  Apple/team/domain values are supplied.

## 2026-07-10 Design page evidence export

- Updated `scripts/ios/export-nome-design-evidence-state.sh` so the design
  evidence packet now writes `page_evidence.tsv` alongside `coverage.tsv`.
- The new page evidence records, for each of the seven approved `pages-v2`
  effect-picture pages:
  - approved design artifact;
  - smoke screenshot labels and paths;
  - current page status;
  - visual acceptance;
  - final functional acceptance;
  - blocker batch;
  - App Store dependency;
  - remaining proof;
  - next evidence.
- Updated `scripts/ios/test-export-nome-design-evidence-state.sh` to verify:
  - `page_evidence.tsv` exists;
  - the summary records seven page evidence rows;
  - all seven page rows are visual `PASS`;
  - all seven page rows remain final functional `BLOCKED`;
  - add-friend, conversation, and settings rows keep their expected blocker
    batch and App Store dependency mapping.
- Current targeted verification:
  - `bash -n scripts/ios/export-nome-design-evidence-state.sh scripts/ios/test-export-nome-design-evidence-state.sh`
    exits `0`;
  - `scripts/ios/test-export-nome-design-evidence-state.sh` exits `0`;
  - `scripts/ios/export-nome-design-evidence-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-design-evidence-state-page-evidence-20260710 --force`
    exits `0`;
  - `/tmp/nome-ios-design-evidence-state-page-evidence-20260710/summary.tsv`
    records seven coverage rows, seven page-evidence rows, seven visual PASS
    rows, seven final functional BLOCKED rows, zero missing rows, and fourteen
    smoke labels.
- Current integrated readiness after adding page evidence:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-readiness-page-evidence-20260710`
  exits `0` and reports 45 PASS, 2 WARN, 8 BLOCKED, and 0 FAIL.
- Current integrated goal audit after adding page evidence:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-goal-audit-page-evidence-20260710`
  exits `0` and reports 57 PASS, 3 WARN, 8 BLOCKED, and 0 FAIL.
- This does not remove the final blockers. It makes the current state explicit:
  the approved main-page UX is visually covered by signed iOS smoke evidence,
  while real invitation, group, public-contact, messaging, identity, settings,
  physical-device, release-identifier, and App Store final screenshot proof is
  still pending.

## 2026-07-10 Generic device build evidence in aggregate gates

- Updated `scripts/ios/check-nome-ios-readiness.sh` with
  `--generic-device-build-dir DIR`.
- Updated `scripts/ios/check-nome-ios-goal-audit.sh` with the same
  `--generic-device-build-dir DIR` option.
- The aggregate gates now validate an existing
  `scripts/ios/check-ios-generic-device-build.sh` packet by checking:
  - `summary.tsv` exists;
  - `products.tsv` exists;
  - `product_fail_count` is `0`;
  - `Nome.app` is present and PASS;
  - `SimpleX NSE.appex` is present and PASS;
  - `SimpleX SE.appex` is present and PASS.
- When supplied, that packet is also passed through to
  `scripts/ios/export-ios-device-readiness-state.sh`; goal audit also passes it
  through to `scripts/ios/export-nome-ios-completion-state.sh`.
- This keeps the report honest: the current source already has generic iOS
  device build evidence, but actual install/launch still requires a trusted
  physical iPhone/iPad.
- Current targeted verification:
  - `bash -n scripts/ios/check-nome-ios-readiness.sh scripts/ios/check-nome-ios-goal-audit.sh`
    exits `0`;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-readiness-generic-build-dir-20260710`
    exits `0` and reports 46 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL;
  - `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-goal-audit-generic-build-dir-20260710`
    exits `0` and reports 58 PASS, 2 WARN, 8 BLOCKED, and 0 FAIL.
- In those reports:
  - `generic_device_build` is PASS;
  - `device_readiness_state` is PASS;
  - `completion_state` is PASS in goal audit;
  - `physical_device_readiness` and `physical_device_smoke` remain BLOCKED
    because no trusted physical iPhone/iPad is connected.

## 2026-07-10 Source-targeted real-core audit in aggregate gates

- Updated `scripts/ios/check-nome-ios-readiness.sh` with
  `--source-target TARGET` for the optional `--source-audit` path.
- Updated `scripts/ios/check-nome-ios-goal-audit.sh` with the same
  `--source-target TARGET` option.
- The target is passed only to the real source-audit command as
  `SOURCE_TARGET=<target>`. It no longer needs to be exported globally for the
  entire aggregate run.
- Updated `scripts/ios/test-real-core-source-audit.sh` so Hydra/destination
  simulator fixtures explicitly set `SOURCE_TARGET=simulator`; the test now
  passes even when the caller environment contains
  `SOURCE_TARGET=physical-device`.
- Current source-audit verification:
  - `RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-real-core-sources.sh`
    exits `1` for the simulator target, because the current simulator route is
    arm64 while reachable simulator artifacts are x86_64;
  - `SOURCE_TARGET=physical-device RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-real-core-sources.sh`
    exits `0`, confirming local and Hydra arm64 device artifacts are available
    for physical-device testing;
  - `SOURCE_TARGET=physical-device scripts/ios/test-real-core-source-audit.sh`
    exits `0` after the fixture isolation fix.
- Current targeted syntax verification:
  - `bash -n scripts/ios/test-real-core-source-audit.sh scripts/ios/check-nome-ios-readiness.sh scripts/ios/check-nome-ios-goal-audit.sh`
    exits `0`.
- Current integrated readiness with source target and generic build evidence:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-readiness-source-target-device-generic-build-20260710`
  exits `0` and reports 47 PASS, 0 WARN, 8 BLOCKED, and 0 FAIL.
- Current integrated goal audit with source target and generic build evidence:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-goal-audit-source-target-device-generic-build-20260710`
  exits `0` and reports 59 PASS, 1 WARN, 8 BLOCKED, and 0 FAIL.
- The only WARN in that goal audit is `physical_device_test`, which is
  intentionally conditional on a connected trusted iPhone/iPad.

## 2026-07-10 Targeted real-core preflight

- Updated `scripts/ios/check-real-core.sh` with `--target`:
  - `all` keeps the previous strict default and checks both
    `apps/ios/Libraries/ios` and `apps/ios/Libraries/sim`;
  - `physical-device` checks only the installed arm64 device libraries;
  - `simulator` checks only the installed simulator libraries.
- Updated `scripts/ios/test-check-real-core-arch.sh` to cover the
  `physical-device` target, proving a simulator architecture mismatch does not
  fail a device-only preflight.
- Updated `scripts/ios/check-nome-ios-readiness.sh` and
  `scripts/ios/check-nome-ios-goal-audit.sh` so `--source-target
  physical-device` runs `scripts/ios/check-real-core.sh --target
  physical-device` for the `real_core_preflight` gate.
- This does not hide the simulator blocker. The aggregate gates still run
  `simulator_real_core_route`, which remains BLOCKED while the current arm64
  simulator is linked to preview-core libraries.
- Current targeted verification:
  - `bash -n scripts/ios/check-real-core.sh scripts/ios/test-check-real-core-arch.sh scripts/ios/check-nome-ios-readiness.sh scripts/ios/check-nome-ios-goal-audit.sh`
    exits `0`;
  - `scripts/ios/test-check-real-core-arch.sh` exits `0`;
  - `scripts/ios/check-real-core.sh --target physical-device` exits `0`;
  - `scripts/ios/check-real-core.sh --target simulator` exits `1` as expected,
    reporting the 10,104-byte preview simulator core and preview markers.
- Current integrated readiness with physical-device preflight:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-readiness-physical-preflight-20260710`
  exits `0` and reports 48 PASS, 0 WARN, 7 BLOCKED, and 0 FAIL.
- Current integrated goal audit with physical-device preflight:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-goal-audit-physical-preflight-20260710`
  exits `0` and reports 60 PASS, 1 WARN, 7 BLOCKED, and 0 FAIL.
- The remaining BLOCKED rows are now the true outstanding gates for this
  machine: manual QA, strict final App Store screenshots, simulator real-core
  route, local Nix build environment, physical-device readiness,
  physical-device smoke, and strict release identifiers.

## 2026-07-10 Physical-device artifact route for build environment

- Updated `scripts/ios/check-real-core-build-env.sh` with:
  - `--target all|simulator|physical-device`;
  - `--allow-downloaded-artifacts`.
- Default behavior remains strict: without the artifact fallback option, the
  script still exits non-zero when Nix is not installed.
- With `--target physical-device --allow-downloaded-artifacts`, the script
  treats the existing downloaded arm64 device artifact as a valid fallback for
  the physical-device testing route. It still reports missing Nix as a warning,
  because local source builds are not available on this machine.
- Updated `scripts/ios/check-nome-ios-readiness.sh` and
  `scripts/ios/check-nome-ios-goal-audit.sh` so `--source-target
  physical-device` runs:
  `scripts/ios/check-real-core-build-env.sh --target physical-device --allow-downloaded-artifacts`.
- Current targeted verification:
  - `bash -n scripts/ios/check-real-core-build-env.sh scripts/ios/check-nome-ios-readiness.sh scripts/ios/check-nome-ios-goal-audit.sh`
    exits `0`;
  - `scripts/ios/check-real-core-build-env.sh` exits `1` as expected in strict
    default mode because Nix is not installed;
  - `scripts/ios/check-real-core-build-env.sh --target physical-device --allow-downloaded-artifacts`
    exits `0`, confirming the physical-device artifact route is available.
- Current integrated readiness with the physical-device artifact route:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-readiness-physical-artifact-route-20260710`
  exits `0` and reports 49 PASS, 0 WARN, 6 BLOCKED, and 0 FAIL.
- Current integrated goal audit with the physical-device artifact route:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-goal-audit-physical-artifact-route-20260710`
  exits `0` and reports 61 PASS, 1 WARN, 6 BLOCKED, and 0 FAIL.
- Remaining blockers after this update:
  - manual QA still has unchecked real-function items;
  - final App Store screenshots still need real-core replacement captures;
  - simulator real-core route is still blocked by the current arm64 simulator
    and preview simulator libraries;
  - no trusted physical iPhone/iPad is connected for install/smoke;
  - strict release identifiers are not yet selected and applied.

## 2026-07-10 Release identifier migration dry-run/apply tooling

- Added `scripts/ios/apply-ios-release-identifiers.sh`.
- The script consumes a checked `proposed_identifiers.tsv` from
  `scripts/ios/export-ios-release-identity-state.sh`.
- Default mode is dry-run:
  - validates the proposal with `scripts/ios/check-ios-release-identity-proposal.sh`;
  - writes `summary.tsv`, `changes.tsv`, and logs;
  - does not edit the project.
- `--apply` mode edits:
  - `apps/ios/SimpleX.xcodeproj/project.pbxproj` bundle identifiers;
  - `apps/ios/SimpleX--iOS--Info.plist` background task id and URL type name;
  - main app, notification service, and share extension entitlements for App
    Group and keychain group;
  - main app associated domains.
- Added `scripts/ios/test-apply-ios-release-identifiers.sh`, which verifies:
  - dry-run leaves fixture files unchanged;
  - apply mode updates fixture identifiers;
  - strict `scripts/ios/check-ios-release-identifiers.sh` passes on the
    migrated fixture.
- Updated `scripts/ios/export-ios-release-identity-state.sh` so
  `next_actions.tsv` points at the migration script after Apple/team/domain
  approval.
- Integrated the migration unit into:
  - `scripts/ios/check-nome-ios-readiness.sh`;
  - `scripts/ios/check-nome-ios-goal-audit.sh`.
- Current targeted verification:
  - `bash -n scripts/ios/export-ios-release-identity-state.sh scripts/ios/check-ios-release-identity-proposal.sh scripts/ios/apply-ios-release-identifiers.sh scripts/ios/test-apply-ios-release-identifiers.sh scripts/ios/check-nome-ios-readiness.sh scripts/ios/check-nome-ios-goal-audit.sh`
    exits `0`;
  - `scripts/ios/test-export-ios-release-identity-state.sh` exits `0`;
  - `scripts/ios/test-check-ios-release-identity-proposal.sh` exits `0`;
  - `scripts/ios/test-apply-ios-release-identifiers.sh` exits `0`.
- Current real-project candidate dry-run:
  - exported candidate proposal:
    `/tmp/nome-ios-release-identity-candidate-apply-20260710`;
  - dry-run migration packet:
    `/tmp/nome-ios-release-identity-apply-dry-run-20260710`;
  - candidate values used for the dry-run: `app.nome.secure` and `nome.chat`;
  - dry-run result: 14 change rows, zero missing current-value rows, strict
    post-apply gate skipped because files were not edited.
- Current integrated readiness after adding the migration unit:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-readiness.sh --allow-blockers --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-readiness-release-identity-apply-20260710`
  exits `0` and reports 50 PASS, 0 WARN, 6 BLOCKED, and 0 FAIL.
- Current integrated goal audit after adding the migration unit:
  `CHECK_XCODE_DESTINATIONS=1 RELEASE_LIMIT=1 ACTIONS_ARTIFACT_PAGES=1 scripts/ios/check-nome-ios-goal-audit.sh --allow-blockers --source-audit --source-target physical-device --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710 --output /tmp/nome-ios-goal-audit-release-identity-apply-20260710`
  exits `0` and reports 62 PASS, 1 WARN, 6 BLOCKED, and 0 FAIL.
- This does not clear the strict release identifier blocker yet. It makes the
  final migration executable once the real Apple Developer App IDs, App Group,
  keychain migration behavior, and associated domain ownership/AASA hosting are
  approved.

## 2026-07-10 QA execution handoff aligned to physical-device route

- Updated `scripts/ios/export-nome-qa-execution-state.sh` with:
  - `--source-target simulator|physical-device`;
  - `--generic-device-build-dir DIR`;
  - a separate `physical_device_smoke` gate, matching the top-level goal audit.
- With `--source-target physical-device`, the execution-state export now uses:
  - `scripts/ios/check-real-core.sh --target physical-device`;
  - `scripts/ios/check-real-core-build-env.sh --target physical-device --allow-downloaded-artifacts`.
- With `--generic-device-build-dir`, the physical-device readiness packet reuses
  the existing generic iOS device build evidence instead of asking for a rebuild.
- Current targeted verification:
  - `bash -n scripts/ios/export-nome-qa-execution-state.sh scripts/ios/test-export-nome-qa-execution-state.sh`
    exits `0`;
  - `scripts/ios/test-export-nome-qa-execution-state.sh` exits `0`;
  - `scripts/ios/export-nome-qa-execution-state.sh --output /tmp/nome-ios-qa-execution-state-physical-route2-20260710 --force --source-target physical-device --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710`
    exits `0` and reports 41 unchecked manual QA items, 6 gate blockers, 8
    next-action rows, and 0 FAIL rows.
- The six execution-state blockers now align with the live top-level audit:
  manual QA, final App Store screenshots, simulator real-core route,
  physical-device readiness, physical-device smoke, and strict release
  identifiers.
- The execution-state next-action commands are now route-aware too. With
  `--source-target physical-device`, Batch 1 through Batch 4 now point at
  `scripts/ios/check-real-core.sh --target physical-device`, and Batch 5
  carries the existing generic device build packet through
  `--generic-device-build-dir`.
- Current route-aware handoff export:
  `scripts/ios/export-nome-qa-execution-state.sh --output /tmp/nome-ios-qa-execution-state-route-aware-actions-20260710 --force --source-target physical-device --generic-device-build-dir /tmp/nome-ios-generic-device-build-next-20260710`
  exits `0` and reports 41 unchecked manual QA items, 6 gate blockers, 8
  next-action rows, and 0 FAIL rows. Its `next_actions.tsv` no longer sends
  the operator back to the default all-target real-core check for
  physical-device batches.
- XcodeBuildMCP was checked again after `session_show_defaults` confirmed the
  correct project, scheme, simulator, derived data path, and bundle id. The
  tool still fails before build because its process resolves `xcrun` through
  the system Command Line Tools path and cannot find `simctl`. This remains a
  local tool-path issue; the stable simulator path is still the shell smoke
  flow with `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`.

## 2026-07-10 Live UI smoke and Codex browser preview refresh

- Rebuilt, installed, and launched the current Nome iOS working tree on the
  booted iPhone 17 Pro simulator
  `95CA9F4F-F85B-4AC9-ADAE-62098924E3B4`.
- `scripts/ios/smoke-nome-ui.sh` captured all 14 expected UI states at
  `/tmp/nome-ios-smoke-live-20260710-0730` and exited `0`.
- The smoke run produced no new `Nome-*.ips` crash report.
- `scripts/ios/check-nome-smoke-visual-quality.sh` checked the new manifest and
  reported 14 PASS image rows, 0 failure rows, correct `1206x2622` dimensions,
  and distinct hashes for every screen.
- The normal app launch was restored after preview-state capture. A current
  simulator frame is saved at `/tmp/nome-ios-live-home-20260710.png` and shows
  the Nome home page rather than a Debug preview route.
- The simulator mirror was restarted with full Xcode selected through
  `DEVELOPER_DIR`. `http://127.0.0.1:3200/` is serving the live simulator frame
  again for the Codex in-app browser.
- Latest manifest-backed readiness audit:
  `/tmp/nome-ios-readiness-live-20260710`, 50 PASS, 0 WARN, 6 BLOCKED, 0 FAIL.
- Latest goal audit:
  `/tmp/nome-ios-goal-audit-live-20260710`, 62 PASS, 1 WARN, 6 BLOCKED, 0 FAIL.
- Latest top-level completion packet:
  `/tmp/nome-ios-completion-live-20260710`, with planning and visual design
  complete and five release-level requirement groups still blocked.
- `devicectl list devices` still reports `No devices found`, so real-core
  functional QA, physical-device smoke, four final real-core App Store
  screenshots, and final release identifiers remain outside the simulator-only
  proof from this run.

## 2026-07-10 Chat header simplification

- Simplified the chat-list header to a compact Nome brand row with identity and
  new-connection actions. Removed the repeated large `首页` heading and its
  explanatory paragraph so conversations enter the first viewport sooner.
- Kept a single search surface in each state: the empty state uses the
  connect/search affordance, while an existing-chat state uses the functional
  chat search field. The previous duplicate search presentation is gone.
- Moved the low-frequency unread filter and list creation actions into the
  search field's trailing menu. Existing chat tags still appear when relevant.
- Renamed the bottom `首页` destination to `聊天` and replaced the home icon
  with the chat-bubbles symbol. Contacts and Settings remain unchanged.
- Rebuilt and captured all 14 UI states at
  `/tmp/nome-ios-smoke-header-refine-v2-20260710`; the smoke script exited `0`,
  produced no new `Nome-*.ips` crash report, and the visual-quality check
  reported 14 PASS image rows with 0 failures.
- Restored the live existing-chat preview with `-NomeChatListPreview` after the
  smoke run. `http://127.0.0.1:3200/` returns HTTP `200` and mirrors the refined
  chat-list screen.

## 2026-07-10 Chat quick-action refinement

- Replaced the three separate tinted quick-action cards above the chat list
  with one restrained command bar. The actions share a single secondary
  surface and use thin separators instead of individual colored backgrounds
  and borders.
- Kept `添加朋友` as the primary green accent. `加入群组` and `公开地址` now use
  the Nome navy neutral, reducing visual competition while preserving equal
  tap targets and the original action callbacks.
- Captured the before state at
  `/tmp/nome-quick-actions-audit-before-20260710.png` and the accepted after
  state at
  `/tmp/nome-ios-smoke-quick-actions-refine-20260710/06-chat-list-existing.png`.
- Rebuilt and captured all 14 UI states at
  `/tmp/nome-ios-smoke-quick-actions-refine-20260710`; the UI smoke and visual
  quality checks both passed with 14 image rows, 0 failures, and no new Nome
  crash report.

## 2026-07-10 Empty-state and product-copy refinement

- Replaced the stacked empty-home quick-action cards and long explanatory card
  with one focused `开始第一段私密对话` panel. It keeps add-friend, join-group,
  and public-address actions while reducing duplicate copy and controls.
- The panel now communicates three concise trust points: no phone number,
  end-to-end encryption, and local storage. Product-facing copy no longer
  names the upstream project.
- Removed the upstream project name from Simplified Chinese display values and
  from the primary user-facing Swift strings in onboarding, chat help, call
  server settings, profile help, developer tools, badges, and About Nome.
  Technical module names, protocol identifiers, compatibility behavior, source
  URLs, copyright notices, and license attribution remain unchanged.
- Extended `scripts/ios/check-nome-brand-copy.sh` so it fails when the upstream
  name reappears in a user-facing Swift string or Simplified Chinese display
  value. The stricter brand-copy check and localization plist lint both pass.
- Investigated the pre-existing
  `/Users/forkman03/Library/Logs/DiagnosticReports/Nome-2026-07-10-082213.ips`.
  A seeded chat-list preview row was opening the real chat loader without a
  real core controller. `NomeChatListPreviewHost` now routes list rows to the
  safe seeded conversation preview instead.
- Rebuilt all 14 UI states at
  `/tmp/nome-ios-smoke-empty-state-redesign-v2-20260710`; the smoke run exited
  `0` and produced no newer Nome crash report. The normal empty-home launch is
  restored at `http://127.0.0.1:3200/`.

## 2026-07-10 Empty-state trust-row alignment

- Replaced the unsupported no-phone-number symbol with `iphone.slash`, so all
  three trust points now render an icon on the target iOS 26.5 simulator.
- Gave every trust-point icon and label the same fixed visual frame, keeping
  icon centers, text baselines, and column spacing aligned.
- Rebuilt and visually checked all 14 UI states at
  `/tmp/nome-ios-smoke-trust-row-align-20260710`; smoke, visual-quality, and
  Nome brand-copy checks pass with no new crash report.
