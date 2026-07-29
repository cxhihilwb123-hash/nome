# Nome Design Record

Date: 2026-07-08

## Purpose

Nome is the working brand and UX direction for a Chinese-friendly version of a SimpleX-like privacy messenger.

The goal is not to make a cosmetic skin. The goal is to make the difficult privacy model easier to understand:

- no phone number, username, or global user ID;
- one-time private invitations for direct connections;
- reusable public contact address for discovery;
- local identities/profiles instead of cloud accounts;
- local-first backup and migration;
- advanced network privacy such as Tor kept separate from identity privacy.

## Approved Logo Direction

The selected logo is the navy/green shield-chat mark with a white private-connection/broken-link symbol.

Meaning:

- shield shape: safety and protection;
- chat/ribbon shape: communication;
- broken-link/infinity-like white symbol: no permanent identity tracking;
- navy + green: trust, privacy, and calm utility.

Naming rule:

- Use `Nome`.
- Do not use `NoMe`.
- Do not use `SimpleX` on branded Nome surfaces.

Approved files:

- `brand/nome-logo.png` - primary horizontal logo.
- `brand/nome-app-icon.png` - app icon source crop.
- `brand/nome-app-icon-1024.png` - app icon export.
- `brand/nome-mark.png` - standalone mark.
- `brand/nome-lockup-dark.png` - dark background lockup.
- `brand/nome-logo-mono.png` - one-color navy lockup.
- `brand/nome-logo-assets-sheet.png` - approved brand asset board.

Exploration files kept for reference:

- `brand/nome-logo-assets-sheet-medium-backup.png` - lighter wordmark exploration.
- `brand/nome-logo-assets-sheet-bold-backup.png` - backup of the selected bold wordmark asset board.

Palette:

- Deep navy: `#0E1B2D`
- Privacy green: `#16AE66`
- Trust blue accent: `#276BFF`

## Product UX Artifacts

Current product overview:

- `product/nome-product-ux-redesign.png`

Current corrected page set:

- `product/pages-v2/nome-pages-v2-overview.png`
- `product/pages-v2/01-home-inbox.png`
- `product/pages-v2/02-add-friend-one-time.png`
- `product/pages-v2/03-join-group.png`
- `product/pages-v2/04-public-contact.png`
- `product/pages-v2/05-identity-center.png`
- `product/pages-v2/06-conversation.png`
- `product/pages-v2/07-settings-safety.png`

Earlier page exploration:

- `product/pages/`

Use `pages-v2` as the current product discussion baseline. The earlier `pages` set is useful for comparison but has known product-semantics issues.

## Main Page Model

### 1. Home / Inbox

Purpose:

- make Nome feel like a normal messenger first;
- expose the three most important actions: add friend, join group, public contact address.

Important rule:

- do not show online/presence status by default.

### 2. Add Friend

Purpose:

- direct private connection with a known person.

Correct language:

- `一次性链接`
- `一次性二维码`
- `仅可使用一次`
- `连接成功后失效`

Do not mix this with the reusable public address.

### 3. Join Group

Purpose:

- handle group invitation links and QR codes.

Important rules:

- show group preview before joining;
- make admin review / approval visible when relevant;
- warn users to confirm group source for public groups.

### 4. Public Contact Address

Purpose:

- let many people request contact through a reusable address.

Correct language:

- `公开联系方式`
- `需要确认`
- `更换地址`
- `关闭地址`

Important rule:

- closing or changing this address must not imply existing contacts are lost;
- it is only used to establish contact, not for later message delivery.

### 5. Identity Center

Purpose:

- make profiles/identity understandable without exposing protocol jargon.

Important distinction:

- `匿名资料` / incognito means new contacts do not receive the user's main profile.
- `使用 Tor` means network address protection.

Do not claim anonymous profile mode hides IP address.

### 6. Conversation

Purpose:

- normal chat experience with privacy reassurance.

Important rules:

- avoid `在线` by default;
- prefer `安全会话`, `已连接`, `已加密`;
- include security-code verification state where useful, for example `安全码已验证`.

### 7. Settings / Safety

Purpose:

- expose practical maintenance paths for a local-first messenger.

Correct language:

- `备份与迁移`
- `连接桌面`
- `隐私与安全`
- `服务器与 Tor`

Important rule:

- do not imply there is a normal cloud account login.

## Key Conversation Decisions

- The product name is `Nome`, not `NoMe`.
- The approved logo is the bolder wordmark version from the selected asset board.
- The shield mark bottom must remain closed; no white seam or accidental gap.
- The product should not teach users the protocol first. It should route them through everyday tasks.
- UX complexity should be reduced by mapping protocol concepts to plain user language:
  - one-time invite -> `一次性链接 / 一次性二维码`;
  - reusable contact address -> `公开联系方式`;
  - profiles/incognito -> `身份中心 / 匿名资料`;
  - Tor/network routing -> `网络隐私 / 使用 Tor`;
  - database export/import -> `备份与迁移`.
- Online/presence should not be shown casually in a privacy-first messenger.
- The current `pages-v2` set is the corrected baseline because it fixes identity/Tor confusion, one-time vs reusable contact confusion, and the missing group-join flow.

## Implementation Pass 1

Date: 2026-07-08

Scope applied to the real project:

- replaced iOS app icons, dark app icons, onboarding logos, and QR/logo icon assets with Nome raster exports;
- replaced Android launcher icons, adaptive foreground icons, Play Store icons, drawable logos, and shared Compose logo assets with Nome raster exports;
- changed visible app name resources to `Nome` for Android/Desktop and the iOS main app display name;
- updated Simplified Chinese and default English copy on the main entry paths:
  - `New chat` -> `Add or join` / `添加 / 加入`;
  - `Create 1-time link` -> one-time friend link / `一次性加好友`;
  - `SimpleX address` -> public contact address / `公开联系方式`;
  - chat profiles -> identity center / `身份中心`;
  - settings and desktop connection labels simplified.
- updated iOS onboarding headline to the Nome framing: private connections, no phone number, no public ID.
- kept the underlying `simplex:` link scheme, package IDs, server references, and protocol terminology where needed for compatibility.

Verification completed:

- Android/Desktop MR XML resources pass `xmllint`.
- iOS `Localizable.strings`, `SimpleX--iOS--InfoPlist.strings`, and `project.pbxproj` pass `plutil -lint`.
- Key exported app icon and logo dimensions were checked with `sips`.

Verification not completed in this environment:

- Gradle build/resource generation, because Java Runtime is not installed.
- iOS build settings/build, because the active developer directory is Command Line Tools rather than full Xcode.

## Open Follow-Ups

- Convert the approved logo mark and lockups to clean SVG/vector sources.
- Export production app icon sizes for iOS, Android, desktop, and website favicon.
- Decide whether Nome is a full fork/redistribution, a Chinese-friendly edition, or a UX/brand concept only.
- Review trademark/license language before public release.
- Replace generated UI mockups with deterministic Figma or code-based design files if the direction is approved.
- Validate Chinese copy with real users, especially around `公开联系方式`, `匿名资料`, `备份与迁移`, and `服务器与 Tor`.

## iOS Coverage Gate

Date: 2026-07-09

The approved `product/pages-v2/` set is now tracked against the native iOS
implementation in `plans/20260709_nome_ios_design_coverage_matrix.md`.

Repeatable checks:

- `scripts/ios/check-nome-design-coverage.sh` verifies that all seven page
  mockups exist and that the coverage matrix/smoke script contain the expected
  coverage IDs and screenshot labels.
- `scripts/ios/smoke-nome-ui.sh` now captures nine previewable iOS surfaces,
  including `09-identity-center` through the Debug-only
  `-NomeIdentityCenterPreview` launch argument.

Boundary:

- This proves visual/navigation coverage for the main Nome iOS surfaces.
- It does not prove real invitation, public address, group, messaging, server,
  Tor, migration, or desktop-linking behavior until real iOS core libraries are
  installed and tested.
