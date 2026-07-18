# New Chat / Connection

> **Related spec:** [spec/client/navigation.md](../../spec/client/navigation.md)

## Purpose

Create new contacts, groups, or connect with others via one-time invitation links or by scanning/pasting SimpleX links. This is the primary entry point for establishing new E2E encrypted connections.

## Route / Navigation

- **Entry point**: Tap the new chat button (pencil icon) in `ChatListView` toolbar or FAB
- **Presented by**: `NewChatSheet` modal from `ChatListView` via `showNewChatSheet()`; Android
  renders the Nome P10 hub and Desktop delegates the official legacy content unchanged
- **Internal navigation**: `NewChatSheet` owns 4 existing action callbacks:
  - "Create 1-time link" -- opens `NewChatView` with `INVITE` tab (generate and share a one-time invitation link)
  - "Scan / paste link" -- opens `NewChatView` with `CONNECT` tab (scan QR code or paste a received link)
  - "Create group" -- opens `AddGroupView`
  - "Create channel" -- opens `AddChannelView`
- **Tabs within NewChatView**: `HorizontalPager` with `TabRow` toggles between `NewChatOption.INVITE` (1-time link) and `NewChatOption.CONNECT` (connect via link)
- **Swipe gesture**: Left/right swipe switches between tabs (Android only; `userScrollEnabled = appPlatform.isAndroid`)
- **Dismiss behavior**: On dispose, a `DisposableEffect` shows an alert dialog (via `AlertManager.shared.showAlertDialog`) asking whether to keep an unused invitation link or delete it via `controller.deleteChat()`

## Nome Android P10 Hub

The Android P10 presentation shows the actual current local identity and dispatches each visible
row to the existing one-time invitation, scan/paste, create-group, or create-channel callback.
When entered from Nome Home, the identity card may close the hub and open the already-owned
`UserPicker`. The hub does not create a connection, claim that a route succeeded, publish a
contact address, or infer network health.

The P10 visual-acceptance baseline governs composition, hierarchy, spacing, typography, icon and
action sizing. Copy follows the actual callback where the baseline is semantically inaccurate:
the group row starts group creation and does not claim that an existing group was joined. Desktop
calls `legacyContent()` and receives no Nome presentation.

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
| QR code scanner | Camera-based QR code scanner (`showQRCodeScanner` state) |
| Paste link field | Text field for pasting a SimpleX link (`pastedLink`) |
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
| `ConnectPlan.kt` | `views/newchat/ConnectPlan.kt` |
| `QRCodeScanner.kt` | `views/newchat/QRCodeScanner.kt` (expect/actual) |
| `ContactConnectionInfoView.kt` | `views/newchat/ContactConnectionInfoView.kt` |
| `PlatformConnectionPreview.kt` | `views/newchat/PlatformConnectionPreview.kt` (safe model, explicit policy, platform seam) |
| `PlatformConnectionPreview.android.kt` | `androidMain/.../views/newchat/PlatformConnectionPreview.android.kt` |
| `NomeConnectionPreviewRoute.android.kt` | `androidMain/.../ui/nome/connection/NomeConnectionPreviewRoute.android.kt` |
| `PlatformConnectionPreview.desktop.kt` | `desktopMain/.../views/newchat/PlatformConnectionPreview.desktop.kt` (legacy fallback) |
