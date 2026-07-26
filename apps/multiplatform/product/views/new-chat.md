# New Chat / Connection

> **Related spec:** [spec/client/navigation.md](../../spec/client/navigation.md)

## Purpose

Create new contacts, groups, or connect with others via Nome one-time invitations or by scanning/pasting Nome invitations. This is the primary entry point for establishing new E2E encrypted connections.

## Route / Navigation

- **Entry point**: Tap the new chat button (pencil icon) in `ChatListView` toolbar or FAB
- **Presented by**: `NewChatSheet` modal from `ChatListView` via `showNewChatSheet()`; Android
  renders the Nome P10 hub and macOS renders a compact four-row Nome action panel
- **Internal navigation**: `NewChatSheet` owns 4 existing action callbacks:
  - "Create 1-time link" -- opens `NewChatView` with `INVITE` tab (generate and share a one-time invitation link)
  - "Scan / paste link" -- opens `NewChatView` with `CONNECT` tab (scan QR code or paste a received link)
  - "Create group" -- opens `AddGroupView`
  - "Create channel" -- opens `AddChannelView`
- **Routes within NewChatView**: the official `NewChatOption.INVITE` and
  `NewChatOption.CONNECT` owners remain shared. Android presents them as the Nome P11/P12 page
  pair through `PlatformNewChatRoute`; macOS keeps those owners but enters them from the compact
  Nome hub and uses a compact Nome empty-state action list.
- **Swipe gesture**: Android's P11/P12 presentation retains left/right paging between the two
  official options.
- **Dismiss behavior**: On dispose, a `DisposableEffect` shows an alert dialog (via `AlertManager.shared.showAlertDialog`) asking whether to keep an unused invitation link or delete it via `controller.deleteChat()`

## Nome Android P10 Hub

The Android P10 presentation shows the actual current local identity and dispatches each visible
row to the existing one-time invitation, scan/paste, create-group, or create-channel callback.
When entered from Nome Home, the identity card may close the hub and open the already-owned
`UserPicker`. The hub does not create a connection, claim that a route succeeded, publish a
contact address, or infer network health.

The P10 visual-acceptance baseline governs composition, hierarchy, spacing, typography, icon and
action sizing. Copy follows the actual callback where the baseline is semantically inaccurate:
the group row starts group creation and does not claim that an existing group was joined. macOS
uses the same four real callbacks in a desktop-density panel without large blue cards.

## Nome Android P11/P12 Pages

Android P11 keeps `NewChatView.createInvitation()` and `apiAddContact()` as the sole invitation
producer. The Nome page presents the resulting official `CreatedConnLink`, uses the existing
clipboard/share actions, and lets the compact identity header invoke the existing profile picker
when that owner is available. Generated, copied, shared, and connected are distinct facts. The
page does not claim a TTL, expiry, peer use, safe invalidation, or atomic regeneration.

Android P12 keeps the existing parser and `planAndConnect(..., Legacy)` path. The camera is
activated only by an explicit scan action, and clipboard text is read only by an explicit
clipboard action. Manual entry, camera output, and clipboard output all enter the same official
parse/plan owner. Camera hardware is optional in the production manifest. A missing camera,
first denial, permanent denial, Settings recovery, scanner disposal, and frame closure stay
platform-owned and do not become connection-success facts.

P11 and P12 use their effect images as page-level visual acceptance baselines. Their Android
presentation follows the baseline composition, region placement, hierarchy, density, type,
spacing, corners, icons, and action geometry while replacing unsupported reference facts with
truthful official/client/platform states. The shared seam changes no Desktop UI.

## Page Sections

### Tab Selector

| Tab | Icon | Label | Description |
|---|---|---|---|
| 1-time link | `ic_repeat_one` | "1-time link" | Generate and share a one-time invitation link |
| Connect via link | `ic_qr_code` | "Connect via link" | Scan QR code or paste a received link |

### Invite Tab (1-time Link) -- `PrepareAndInviteView`

Displayed when `selection == INVITE`:

| Element | Description |
|---|---|
| QR code display | Generated QR code for the invitation link (`SimpleXLinkQRCode`) |
| Short/full link toggle | Switch between short and full link display |
| Share button | System share for the invitation link |
| Copy button | Copy link to clipboard |
| Incognito toggle | Option to connect with a random profile |
| Loading state | `CreatingLinkProgressView` with "Creating link" text while `creatingConnReq` is true |
| Retry button | `RetryButton` shown if link creation fails; calls `createInvitation()` |

Link creation calls `apiAddContact` which returns a `CreatedConnLink` with both `connFullLink` and optional `connShortLink`. The invitation is tracked via `chatModel.showingInvitation`.

### Connect Tab -- `ConnectView`

Displayed when `selection == CONNECT`:

| Element | Description |
|---|---|
| QR code scanner | Camera-based scanner, activated only after the explicit scan action (`showQRCodeScanner` state) |
| Paste link field | Text field for manually entering a SimpleX link (`pastedLink`) |
| Clipboard action | Explicitly reads the current clipboard and submits that text through the same parser |
| Connect button | Initiates connection via `planAndConnect()` |

When a valid SimpleX link is detected:
1. `planAndConnect()` is called with the link URI
2. If the link matches a known contact, filters to that chat
3. If the link matches a known group, filters to that group
4. Otherwise, creates a new connection

### Android External-Link Preview Boundary

Opening a supported connection URI through Android `ACTION_VIEW` is distinct from this page's
scan/paste flow. After the unchanged root and core-running gates, that one ingress explicitly
requests the Nome P13 full-screen preview for eligible core plan branches. It shows a safe
invitation/address/group consequence, current-profile vs new-incognito choice, exact warning or
owner-proof status, and response-driven connecting/pending/failure states.

The New Chat sheet, Connect tab, scanner, paste field, P09 loaded-chat search, message links, chat
preview links, and group-member links do not opt in to P13 and keep their existing connection
planning ownership. P10 is only the route hub; P13 remains restricted to Android external
`ACTION_VIEW`. P13 does not implement P12 camera behavior or absorb P16 group details, and never
displays or persists the raw connection URI.

### Create Group (`AddGroupView`)

| Element | Description |
|---|---|
| Group name field | Required display name input with `FocusRequester` |
| Profile image picker | `GetImageBottomSheet` for selecting/cropping a group avatar |
| Incognito toggle | Option to create group with random profile (`incognitoPref`) |
| Create button | Calls `apiNewGroup()`, then opens `AddGroupMembersView` (normal) or `GroupLinkView` (incognito) |

Group creation flow:
1. User enters group name and optionally selects an image
2. `apiNewGroup()` creates the group and returns `GroupInfo`
3. `openGroupChat()` navigates to the new group chat
4. `setGroupMembers()` preloads member data
5. `AddGroupMembersView` opens for inviting contacts (or `GroupLinkView` for incognito groups)

### QR Code Components (`QRCode.kt`)

| Component | Description |
|---|---|
| `SimpleXLinkQRCode` | Renders a QR code for a SimpleX connection link |
| QR scanner | Platform camera scanner for reading QR codes |
| Short link display | Compact link text with copy/share actions |

## Source Files

| File | Path |
|---|---|
| `NewChatView.kt` | `views/newchat/NewChatView.kt` |
| `AddGroupView.kt` | `views/newchat/AddGroupView.kt` |
| `QRCode.kt` | `views/newchat/QRCode.kt` |
| `NewChatSheet.kt` | `views/newchat/NewChatSheet.kt` |
| `PlatformNewChatHub.kt` | `commonMain/.../views/newchat/PlatformNewChatHub.kt` |
| `PlatformNewChatHub.android.kt` | `androidMain/.../views/newchat/PlatformNewChatHub.android.kt` |
| `PlatformNewChatHub.desktop.kt` | `desktopMain/.../views/newchat/PlatformNewChatHub.desktop.kt` |
| `PlatformNewChatRoute.kt` | `commonMain/.../views/newchat/PlatformNewChatRoute.kt` |
| `PlatformNewChatRoute.android.kt` | `androidMain/.../views/newchat/PlatformNewChatRoute.android.kt` |
| `PlatformNewChatRoute.desktop.kt` | `desktopMain/.../views/newchat/PlatformNewChatRoute.desktop.kt` |
| `ConnectPlan.kt` | `views/newchat/ConnectPlan.kt` |
| `QRCodeScanner.kt` | `views/newchat/QRCodeScanner.kt` (expect/actual) |
| `QRCodeScanner.android.kt` | `androidMain/.../views/newchat/QRCodeScanner.android.kt` |
| `NomeNewChatRouteComposeTest.kt` | `android/src/androidTest/.../nome/newchat/NomeNewChatRouteComposeTest.kt` |
| `ContactConnectionInfoView.kt` | `views/newchat/ContactConnectionInfoView.kt` |
| `PlatformConnectionPreview.kt` | `views/newchat/PlatformConnectionPreview.kt` (safe model, explicit policy, platform seam) |
| `PlatformConnectionPreview.android.kt` | `androidMain/.../views/newchat/PlatformConnectionPreview.android.kt` |
| `NomeConnectionPreviewRoute.android.kt` | `androidMain/.../ui/nome/connection/NomeConnectionPreviewRoute.android.kt` |
| `PlatformConnectionPreview.desktop.kt` | `desktopMain/.../views/newchat/PlatformConnectionPreview.desktop.kt` (legacy fallback) |
