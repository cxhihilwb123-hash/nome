# Chat List Specification

Sources: `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt`, `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt`, and `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/`

---

## Table of Contents

1. [Overview](#1-overview)
2. [ChatListView Composable](#2-chatlistview-composable)
3. [Data Sources](#3-data-sources)
4. [Filter System](#4-filter-system)
5. [Chat Preview](#5-chat-preview)
6. [ChatListNavLinkView](#6-chatlistnavlinkview)
7. [Tag System](#7-tag-system)
8. [UserPicker](#8-userpicker)
9. [Source Files](#9-source-files)
10. [Nome Android P07/P08 Production Home](#10-nome-android-p07p08-production-home)

---

## Executive Summary

The Chat List is the landing screen of SimpleX Chat, rendering conversations for the active user. The upstream `ChatListView` provides searchable, filterable chat previews, tag controls, and a user-switching side panel. Phase 2 Batch 2 adds a platform seam at the existing home selection point: Android renders a Nome P07/P08 read-only home over typed load/connectivity/core facts, while Desktop delegates the upstream `ChatListView` content unchanged.

---

## 1. Overview

```
ChatListView
|-- ChatListToolbar               (top or bottom app bar)
|   |-- UserProfileButton         (opens UserPicker)
|   |-- Title ("Your chats")
|   |-- SubscriptionStatusIndicator
|   +-- NewChatButton / StoppedIndicator
|-- ChatListWithLoadingScreen
|   |-- ChatList (LazyColumnWithScrollBar)
|   |   |-- Spacer (top/bottom padding)
|   |   |-- stickyHeader
|   |   |   |-- ChatListSearchBar (search input + filter toggle)
|   |   |   +-- TagsView          (preset + custom tag chips)
|   |   |-- ChatListNavLinkView[] (per-chat row items)
|   |   +-- ChatListFeatureCards  (one-hand UI card, address card)
|   +-- EmptyState text
|-- NewChatSheetFloatingButton    (FAB, standard mode only)
|-- UserPicker                    (slide-in panel, Android)
+-- ActiveCallInteractiveArea     (desktop, in-call banner)
```

---

<a id="ChatListView"></a>

## 2. ChatListView Composable

**Location:** [`ChatListView()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt#L189-L254)

```kotlin
fun ChatListView(
  chatModel: ChatModel,
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit,
  stopped: Boolean
)
```

### Route-level notice and initialization

- [`ChatListNoticeEffect`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt) runs from `StartPartOfScreen` above the platform seam. Android preserves the historical "What's New"/updated-conditions modal and 1-second delay. Nome macOS exits before opening that upstream-owned surface.
- On desktop, closing a chat resets audio/video players.

### Layout Modes

The `oneHandUI` preference (`appPrefs.oneHandUI.state`) controls the layout:

| Mode | Toolbar Position | List Direction | FAB | Search/Tags Position |
|---|---|---|---|---|
| **Standard** (`oneHandUI = false`) | Top | Top-to-bottom | Bottom-right FAB | Below toolbar |
| **One-hand** (`oneHandUI = true`) | Bottom | Bottom-to-top (reversed) | Integrated in toolbar | Above toolbar |

### State

| State | Type | Purpose |
|---|---|---|
| `searchText` | `MutableState<TextFieldValue>` | Search query (saved across recomposition) |
| `listState` | `LazyListState` | Scroll position (persisted in `lazyListState` var) |
| `oneHandUI` | `State<Boolean>` | One-hand UI mode toggle |

### Android-specific

- `SetNotificationsModeAdditions`: Notification permission setup.
- `UserPicker`: Overlay side panel for user switching.

---

## 3. Data Sources

| Source | Location | Description |
|---|---|---|
| `chatModel.chats` | `ChatModel.chatsContext.chats` | Full list of `Chat` objects for the active user |
| `chatModel.activeChatTagFilter` | `ChatModel.activeChatTagFilter` | Currently active filter (`PresetTag`, `UserTag`, or `Unread`) |
| `chatModel.userTags` | `ChatModel.userTags` | User-created custom tags |
| `chatModel.presetTags` | `ChatModel.presetTags` | Map of `PresetTagKind` to count |
| `chatModel.unreadTags` | `ChatModel.unreadTags` | Map of tag ID to unread count |
| `chatModel.chatId` | `ChatModel.chatId` | Currently selected chat ID (highlights row) |
| `chatModel.currentUser` | `ChatModel.currentUser` | Active user profile |
| `chatModel.users` | `ChatModel.users` | All user profiles (for UserPicker) |
| `chatModel.showChatPreviews` | `ChatModel.showChatPreviews` | Privacy toggle for message previews |
| `chatModel.chatListLoadState` | `ChatListLoadState` | Generation-scoped `Initial`, `Loading`, `Loaded`, `Unavailable`, or `NoCurrentUser` fact |
| `chatModel.currentChatListGeneration()` | active remote host + current user ID | Identity/host generation that a load result must match before rows can be replaced or exposed |
| `NetworkObserver.shared.platformNetworkInfo` | nullable Android `UserNetworkInfo` | `null` before first platform observation; later online/offline describes device connectivity only |
| `chatModel.chatRunning` | `Boolean?` | `null` starting, `true` running, `false` stopped; independent of list content and device connectivity |

---

## 4. Filter System

### Active Filter Types

Defined as sealed class `ActiveFilter`:

```kotlin
sealed class ActiveFilter {
  data class PresetTag(val tag: PresetTagKind) : ActiveFilter()
  data class UserTag(val tag: ChatTag) : ActiveFilter()
  data object Unread : ActiveFilter()
}
```

### PresetTagKind Enum

| Value | Description |
|---|---|
| `GROUP_REPORTS` | Groups with active reports (moderator-visible) |
| `FAVORITES` | Chats marked as favorite |
| `CONTACTS` | Direct (1:1) chats |
| `GROUPS` | Group chats |
| `BUSINESS` | Business-type chats |
| `NOTES` | Local note folders |

### Search Filtering

The `filteredChats` function applies filters in this order:

1. **SimpleX link match:** If a pasted link resolved to a known contact/group, show only that chat.
2. **Text search:** Case-insensitive match against `chat.chatInfo.chatViewName`, `chat.chatInfo.fullName`, and `chat.chatInfo.localAlias`.
3. **Active filter:**
   - `PresetTag`: Matches chat type and characteristics (e.g., `CONTACTS` filters `ChatInfo.Direct`, `GROUPS` filters `ChatInfo.Group`).
   - `UserTag`: Matches chats whose `chatTags` contain the tag ID.
   - `Unread`: Matches chats with `unreadCount > 0` or `unreadChat == true`.

### Search Bar

`ChatListSearchBar` provides:
- Text input with search icon.
- SimpleX link detection: When a pasted string contains a single SimpleX link, it triggers `planAndConnect` for connection, suppressing normal search.
- Unread filter toggle button (right side, when search is empty).

---

<a id="ChatPreviewView"></a>

## 5. Chat Preview

**Location:** [`ChatPreviewView()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatPreviewView.kt#L41-L52)

```kotlin
fun ChatPreviewView(
  chat: Chat,
  showChatPreviews: Boolean,
  chatModelDraft: ComposeState?,
  chatModelDraftChatId: ChatId?,
  currentUserProfileDisplayName: String?,
  disabled: Boolean,
  linkMode: SimplexLinkMode,
  inProgress: Boolean,
  progressByTimeout: Boolean,
  defaultClickAction: () -> Unit
)
```

### Layout

Each chat preview row contains:

| Element | Position | Content |
|---|---|---|
| Profile image | Left | `ChatInfoImage` with overlay icons for inactive contacts/groups |
| Title row | Top-right of image | Chat name (bold), verified shield (direct), timestamp |
| Preview row | Below title | Last message preview or draft indicator, unread badge |
| Unread badge | Right | Circular badge with count, or dot for muted chats |

### Draft Display

When `chatModelDraftChatId` matches the chat ID, the preview shows a draft indicator (pencil icon) with the draft message text instead of the last chat item.

### Inactive Indicators

- Inactive contacts: cancel icon overlay on profile image.
- Left/removed/deleted groups: cancel icon overlay.

---

<a id="ChatListNavLinkView"></a>

## 6. ChatListNavLinkView

**Location:** [`ChatListNavLinkView()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListNavLinkView.kt#L37)

Routes each chat to the appropriate click action and context menu based on `chat.chatInfo`:

| ChatInfo Type | Click Action | Context Menu |
|---|---|---|
| `ChatInfo.Direct` | `directChatAction` (opens chat) | `ContactMenuItems`: mark read/unread, mute, favorite, tag, clear, delete |
| `ChatInfo.Group` | `groupChatAction` (opens chat or joins) | `GroupMenuItems`: mark read/unread, mute, favorite, tag, clear, leave, delete |
| `ChatInfo.Local` | `noteFolderChatAction` (opens notes) | `NoteFolderMenuItems`: mark read, clear, delete |
| `ChatInfo.ContactRequest` | `contactRequestAlertDialog` (accept/reject) | `ContactRequestMenuItems`: reject |
| `ChatInfo.ContactConnection` | Sets `chatModel.chatId` (opens connection info) | `ContactConnectionMenuItems`: delete |
| `ChatInfo.InvalidJSON` | Sets `chatModel.chatId` | No menu |

### Selection Highlight

On desktop, the currently selected chat (`chatModel.chatId.value == chat.id`) receives a highlight background. `nextChatSelected` state is used to suppress the bottom divider when the next chat in the list is selected.

---

## 7. Tag System

### TagsView

**Location:** [`TagsView()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt#L1055)

Renders a horizontally scrollable row of tag chips (via `TagsRow`, which is a platform-specific `expect` composable).

Layout logic:
- If there are more than 1 collapsible preset tags and the total tag count exceeds 3, preset tags collapse into a `CollapsedTagsFilterView` dropdown.
- Otherwise, each preset tag renders as an `ExpandedTagFilterView` chip.
- User tags render as individual chips with emoji or label icon, bold when active.
- A "+" button at the end opens `TagListEditor` for creating new tags.

### Tag Interactions

- **Single tap:** Toggles the tag filter on `chatModel.activeChatTagFilter`.
- **Long press / right-click (user tags):** Opens dropdown menu with edit/delete/reorder options.
- **Unread dot:** Shown on tags that have chats with unread messages.

<a id="TagListView"></a>

### TagListView

**Location:** [`TagListView()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/TagListView.kt#L48)

Full-screen tag management view opened from the "+" button or long-press menu.

```kotlin
fun TagListView(rhId: Long?, chat: Chat? = null, close: () -> Unit, reorderMode: Boolean)
```

- Displays all user tags in a `LazyColumnWithScrollBar`.
- Supports drag-and-drop reordering via `rememberDragDropState` (calls `apiReorderChatTags`).
- Each tag row shows emoji/icon, name, chat count, and a checkbox if opened for a specific chat (to assign/unassign tags).
- "Create list" button opens `TagListEditor` modal.

---

<a id="UserPicker"></a>

## 8. UserPicker

**Location:** [`UserPicker()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/UserPicker.kt#L46-L50)

```kotlin
fun UserPicker(
  chatModel: ChatModel,
  userPickerState: MutableStateFlow<AnimatedViewState>,
  setPerformLA: (Boolean) -> Unit
)
```

### Behavior

- **Android:** Renders as a slide-up overlay panel on the chat list, triggered by tapping the user profile button in the toolbar.
- **Desktop:** Rendered inline in the left column of `DesktopScreen`, always accessible.
- Closes automatically when any `ModalManager.start` modal opens.

### Content

| Section | Content |
|---|---|
| **Active user** | Profile image, display name, "active" indicator |
| **Other users** | List of non-hidden user profiles sorted by `activeOrder`; tapping switches user |
| **Remote hosts** | Connected remote devices (desktop linking) |
| **Settings** | Opens `SettingsView` modal |
| **Color mode** | `ColorModeSwitcher` for theme toggle |
| **Add profile** | Opens `CreateProfile` flow |
| **Lock** | Locks app (calls `AppLock.setPerformLA`) |

### State Machine

Uses `AnimatedViewState` (`GONE`, `VISIBLE`, `HIDING`) with a `MutableStateFlow` to coordinate animation between the parent screen and the picker overlay.

---

## 9. Source Files

| File | Description |
|---|---|
| `ChatListView.kt` | Main chat list view, toolbar, search, tags, filtering |
| `ChatListNavLinkView.kt` | Per-chat row routing and context menus |
| `ChatPreviewView.kt` | Chat preview row layout (image, title, last message) |
| `ChatHelpView.kt` | Empty-state help content |
| `ContactConnectionView.kt` | Pending connection preview row |
| `ContactRequestView.kt` | Contact request preview row |
| `ServersSummaryView.kt` | Server connection status summary |
| `ShareListNavLinkView.kt` | Share target list row (forwarding) |
| `ShareListView.kt` | Share target list (forwarding flow) |
| `TagListView.kt` | Tag management and assignment view |
| `UserPicker.kt` | User switching side panel |
| `PlatformHomeRoute.kt` | Shared platform seam at the existing normal-home selection point |
| `NomeHomeStateAdapter.kt` | Android-only pure P07/P08 truth derivation |
| `NomeHomeRoute.android.kt` | Android actual, P07/P08 renderer, and permitted existing chat-open navigation |
| `PlatformHomeRoute.desktop.kt` | Desktop actual that invokes upstream `defaultContent` unchanged |

---

## 10. Nome Android P07/P08 Production Home

### Source-set split

[`PlatformHomeRoute()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L16-L22) is deliberately only an `expect` seam. `StartPartOfScreen` keeps the notice effect above that seam, with an explicit desktop return that prevents upstream release/conditions content from opening. The [Android actual](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L63-L123) renders Nome, and the [Desktop actual](../../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt) owns the Nome rail while retaining shared chat/model owners.

### Typed truth and stale-row boundary

[`ChatListLoadGeneration`, `ChatListLoadState`, and `ChatListLoadResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106) distinguish a response for a specific `(remoteHostId, userId)` from empty data. [`beginChatListLoad()` and `applyChatListLoadResult()`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L264-L311) hide rows when requested and accept a success/failure only while its attempt ID and generation still match the active identity and host. [`apiGetChatsResult()`](../../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081) preserves `NoCurrentUser`, `Failure`, and `Success` instead of collapsing all three to `emptyList()`.

Android connectivity has a separate nullable fact: [`platformNetworkInfo`](../../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L18-L24) is `null` until Android has made its first observation. It is not reconstructed from the legacy optimistic `ChatModel.networkInfo` default and it does not imply relay, service, or private-routing health.

[`NomeHomeStateAdapter.derive()`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L49-L112) produces three orthogonal dimensions:

| Dimension | Values | Truth rule |
|---|---|---|
| content | `LOADING`, `FIRST_USE`, `TRUE_EMPTY`, `FILTERED_NO_RESULT`, `POPULATED`, `UNAVAILABLE` | Empty requires a matching-generation typed load success; switching or generation mismatch never exposes stale rows. |
| connectivity | `UNKNOWN`, `ONLINE`, `DEVICE_OFFLINE` | Derived only from the nullable Android platform observation. |
| core | `STARTING`, `RUNNING`, `STOPPED` | Derived only from `chatRunning`; stopped is not rewritten as empty or offline. |

Content precedence is fail-closed: switching/unknown readiness/in-flight hidden rows first, explicit first-use next, then generation validity and typed failure, then populated/filter/true-empty classification. Cached rows are exposed only when their generation matches; a matching-generation `UNAVAILABLE` may retain those rows alongside the error panel.

The pure adapter is intentionally broader than current route reachability. The unchanged root
routes no-current-user into onboarding before `PlatformHomeRoute`, so `FIRST_USE` is currently a
defensive renderer/test branch. The bounded Android home also exposes no filter producer or clear
control, so `FILTERED_NO_RESULT` is likewise defensive on a fresh production process. Neither is
claimed as a passed production P08 route until a separately scoped route/filter change exists.

### P07 populated, read-only home

[`NomeHomeRouteContent()`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L125-L280) preserves the order returned by the existing `filteredChats` projection and renders profile name, chat name/type, timestamp, unread count, and favorite status. Message preview text and its spoken equivalent are included only while the existing `showChatPreviews` privacy preference permits them. Batch 2 adds no home mutation affordance: no favorite toggle, mark-read action, mute, delete, tag editor, profile-switch redesign, connection mutation, search field, or filter control.

Opening an already-ready direct, group, or local chat is existing navigation, not a new home mutation. It is enabled only while the core is `RUNNING`; contact requests, pending connections, invalid data, not-ready rows, contact cards, pending-deletion rows, and all rows while stopped remain non-clickable/read-only. Composer and send behavior remain owned by the existing chat destination and are outside Batch 2.

### P08 loading and non-populated truth

The renderer keeps these states distinct:

| State | Renderer presentation and current route reachability |
|---|---|
| `LOADING` | production-reachable skeleton with polite live-region/state semantics |
| `FIRST_USE` | explicit no-current-user/first-use panel; defensive renderer/test branch because onboarding currently consumes no-user first |
| `TRUE_EMPTY` | production-reachable successful matching-generation empty list |
| `FILTERED_NO_RESULT` | loaded base list plus active filter hides every row; defensive renderer/test branch because this bounded home has no filter producer |
| `UNAVAILABLE` | load result cannot be verified; never presented as true empty |
| `UNAVAILABLE` with matching cached rows | error panel plus cached rows; never success/empty, and existing navigation remains available when the core/readiness/deletion guards allow it |
| initial connectivity unknown | separate “checking device connection” panel |
| device offline | separate device-only offline panel; matching cached rows remain readable |
| core stopped | separate stopped panel; matching cached rows remain readable but cannot open |

The approved P08 image is a visual hierarchy reference for the non-populated family; the production adapter does not render contradictory skeleton and true-empty facts simultaneously.

### Tests and exclusions

The exact Batch 2 contract tests are [`NomeHomeStateAdapterTest`](../../android/src/test/java/chat/simplex/app/nome/home/NomeHomeStateAdapterTest.kt#L16-L242), [`NomeHomeComposeTest`](../../android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeComposeTest.kt#L53-L562), [`NomeHomePackagingTest`](../../android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomePackagingTest.kt#L13-L38), and [`PlatformHomeRouteDesktopTest`](../../common/src/desktopTest/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRouteDesktopTest.kt#L14-L55). The separate argument-gated [`NomeHomeCoreCycleTest`](../../android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeCoreCycleTest.kt#L1-L90) is a real-core evidence gate: it waits for a current user, running core, and non-empty rows, then asserts the exact cached chat-ID sequence across the official stop and start paths.

The debug-only [`NomeHomeEvidenceActivity`](../../android/src/debug/java/chat/simplex/app/nome/home/NomeHomeEvidenceActivity.kt#L45-L253) hosts the production renderer with an explicit “not live core” badge, localized synthetic profile, case locale, and stable cross-year row timestamp. [`NomeHomeScreenshotTest`](../../android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeScreenshotTest.kt#L18-L177) defines and executed an API 35/Asia-Shanghai matrix of 10 states × 2 locales × 2 themes × 2 font scales = 80 captures, including unavailable-with-cached, and asserts a device year newer than the fixture. The batch evidence root records the capture, comparison, real-core, and release-isolation execution artifacts without treating the fixture as live core. Formal review status is owned only by that root's `review-rounds.md` and is not asserted by this specification.

This batch does not implement the one-time locale marker, favorite/profile/connection mutations, search, filter controls, composer/send, Haskell/native core changes, database-format changes, or protocol/command/event additions. Its first-use and filtered-no-result renderer tests therefore do not substitute for production-route evidence.

---

## Nome macOS presentation

The later authorized macOS ARM64 batch uses the same `ChatModel`, `ChatListView`, chat-open
helpers, and modal owners inside `PlatformHomeRoute.desktop`. It adds the Nome rail, neutral
desktop surfaces, and compact empty/new-connection actions. Mobile-only one-hand-layout cards and
the blue promotional feature card are not rendered on Desktop. Empty state still means an
authoritative loaded empty list; the presentation change does not rewrite failure as empty.
