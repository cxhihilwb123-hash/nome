# Chat List (Home Screen)

> **Related spec:** [spec/client/chat-list.md](../../spec/client/chat-list.md)

## Purpose

Main screen of the SimpleX Chat Android and Desktop apps. Displays all conversations sorted by last activity and serves as the navigation root. Desktop continues to use the complete official shared chat list described below. Nome Android currently connects a deliberately bounded P07/P08 production slice at the same root route; that slice does not yet expose the full profile/settings/new-chat/search/mutation surface.

## Route / Navigation

- **Entry point**: App launch (root view), or back-navigation from any chat
- **Presented by**: `PlatformHomeRoute` when `chatModel.chatId == null`; Android uses the bounded Nome renderer and Desktop invokes `ChatListView` as `defaultContent`
- **Navigation**: Android Nome uses the existing ready-chat helpers with core/deletion guards; Desktop `ChatListNavLinkView` retains the complete upstream routing
- **UserPicker**: the overlay remains composed on Android but this bounded renderer adds no profile-switch trigger; Desktop retains the upstream avatar/sidebar behavior

## Platform Layout

| Platform | Layout |
|---|---|
| Android | Bounded Nome single-column P07/P08 header/status/list; no upstream toolbar, filter, settings, or new-chat control in this batch |
| Desktop | 3-column layout: chat list (left), chat view (center), info/detail panel (right via `ModalManager.end`) |

## Nome Android P07/P08 Production Slice

> **Status:** The authorized Batch 2 production-home slice passed build, API 28/API 35 device and real-core, same-package upgrade, screenshot, accessibility, and release-isolation execution gates. Formal frozen-review status is owned exclusively by the batch evidence root's `review-rounds.md`; this product view does not assert that outcome. First use is still consumed by the unchanged onboarding gate and the bounded Android home still has no active-filter producer, so those two renderer branches are not claimed as production-reachable. This section does not mark all P07/P08 product scope complete.

### Host and route split

| Layer | Responsibility |
|---|---|
| Android Activity host | [`MainActivity`](../../android/src/main/java/chat/simplex/app/MainActivity.kt#L60-L65) wraps the existing `AppScreen` in the thin [`NomeProductionShell`](../../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L9-L23). The shell synchronizes window appearance; it does not own root state, navigation, authentication, calls, intents, overlays, safe areas, or back dispatch. |
| Shared route seam | [`StartPartOfScreen`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L366-L393) keeps delivery-receipt and share-intent branches intact, runs the original WhatsNew/updated-conditions notice effect above the seam, and delegates only the existing home selection to [`PlatformHomeRoute`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L8-L22). The declaration contains no Nome UI or duplicate model/controller. |
| Android actual | [`NomeHomeRoute.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L63-L123) applies the Nome theme inside the selected home route and consumes the existing `ChatModel`, user picker state, navigation helpers, and notification addition. |
| Desktop actual | [`PlatformHomeRoute.desktop.kt`](../../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt#L8-L17) invokes `defaultContent()` unchanged, so the official Desktop chat list remains the fallback. |

The common seam exists only because an app-module overlay cannot safely replace a home route owned inside the shared root. Nome presentation remains in `androidMain`; the shared change is the narrow platform selection contract plus typed load provenance needed by every caller of the existing chat-list load.

### Same-generation list truth

[`ChatListLoadGeneration`, `ChatListLoadState`, and `ChatListLoadResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106) distinguish initial/loading, same-generation loaded, unavailable, and no-current-user outcomes. [`apiGetChatsResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081) wraps the existing `CC.ApiGetChats` / `CR.ApiChats` path and returns a typed result instead of collapsing command failure and a successful empty result into the same list. [`applyChatListLoadResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L264-L311) updates rows only while `(remoteHostId, userId)` still matches.

The Android [`NomeHomeStateAdapter`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L49-L121) derives three independent axes:

| Axis | States | Required truth |
|---|---|---|
| Content | Loading, first use, true empty, filtered no result, populated, unavailable | The adapter keeps all six facts distinct. True empty requires a typed same-generation success with zero base rows and failure is unavailable. First use and filtered no result remain defensive renderer contracts only: the production root consumes no-user in onboarding, and this home exposes no filter producer. |
| Connectivity | Unknown, online, device offline | [`NetworkObserver.platformNetworkInfo`](../../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L16-L87) is `null` before Android's first platform observation. Online requires a validated observed network; offline describes only device connectivity. |
| Core | Starting, running, stopped | `chatRunning` is independent of content and connectivity. Stopped may coexist with same-generation cached rows, but actions requiring the core are disabled. |

Any user/remote-host switch, generation mismatch, or loading state that requires row suppression renders loading and exposes no stale rows. The legacy optimistic `ChatModel.networkInfo = OTHER/online=true` default is not accepted as platform-observed online evidence.

### Included P07/P08 behavior

- P07 projects the existing core order and displays chat name, latest summary, timestamp, unread count, and favorite state without mutating those facts. The visible and spoken latest-message summary follows the existing `showChatPreviews` privacy preference; when disabled, Nome exposes only the chat type.
- Ready direct, group, and note-folder rows may open through the existing navigation helpers only while the core is running.
- Contact-request, pending-connection, invalid, not-ready, pending-deletion, and core-stopped rows are presented without a chat-open or home-row mutation action.
- Production-reachable P08 source paths render skeleton loading, true empty, typed unavailable, network unknown, device offline, and core stopped. The first-use and active-filter-no-result renderer branches remain defensive/test-only until a separately scoped production route or filter producer exists.
- Same-generation cached rows may remain visible with an unavailable or stopped state. Unavailable stays explicit but does not itself disable existing chat-open navigation; stopped does.
- Nome light/dark selection and the Activity system bars observe the existing `CurrentColors` result, including an explicit in-app theme that differs from the system theme.

### Explicit Batch 2 exclusions

- favorite toggle or any other chat-list mutation;
- profile-switching UI (the generation guard is included; the switch control is not);
- accept/reject/delete/retry mutation for connection or request rows;
- search field, pasted-link handling, global search, or search no-result derivation;
- tag/filter controls or clear-filter action (therefore the defensive filtered-no-result branch has no fresh-production producer in this batch);
- new-chat/FAB, settings routing, composer/send, locale marker, and archive/migration work.

## Official Shared Fallback Surface

The remaining sections describe the complete official `ChatListView` surface. They continue to apply to the Desktop fallback and serve as capability inventory for later Nome Android batches; controls explicitly excluded above are not currently reachable from the bounded Nome P07/P08 home.

## Page Sections

### Toolbar (`ChatListToolbar`)

| Element | Location | Behavior |
|---|---|---|
| User avatar button | Leading | Opens `UserPicker` sheet (profile switcher, address, settings, preferences, connect to desktop/mobile) |
| "Your chats" title | Center | Tappable to scroll list to top |
| Connection status indicator (`SubscriptionStatusIndicator`) | Adjacent to title | Shows SMP server subscription status; taps open `ServersSummaryView` |
| New chat button (pencil icon) | Trailing (one-hand UI) or FAB (standard) | Opens `NewChatSheet` modal via `showNewChatSheet()` |
| Active call indicator | Trailing (Desktop, one-hand UI) | `ActiveCallInteractiveArea` shown when a call is active |
| Updating progress | Trailing | Shows progress circle/indicator during database updates |
| Stopped indicator | Trailing | Red warning icon when chat engine is stopped |

The toolbar supports two layout modes controlled by `appPrefs.oneHandUI`:
- **Standard (top)**: `DefaultAppBar` at top with `NavigationButtonMenu` leading, title center, buttons trailing. FAB at bottom-right for new chat.
- **One-hand UI (bottom)**: Toolbar at bottom of screen with `Column(Modifier.align(Alignment.BottomCenter))`; list rendered with `reverseLayout = true`; no FAB (new chat button is inline in toolbar).

### Search Bar (`ChatListSearchBar`)

| Element | Description |
|---|---|
| Search icon | Magnifying glass icon at leading edge |
| Text field | `SearchTextField` with placeholder "Search or paste SimpleX link" |
| Filter button | `ToggleFilterEnabledButton` (filter icon) toggles unread-only filter; shown when search text is empty |
| Clear button | Appears when text is entered; `BackHandler` clears search on back |

Behavior:
- Filters chat list in real-time by contact/group name via `filteredChats()`
- Detects pasted SimpleX links (`strHasSingleSimplexLink`) and triggers `planAndConnect()` connection dialogue
- In one-hand UI mode, search bar appears below tag filters with IME spacer; in standard mode, above tag filters

### Chat Filter Tags (`TagsView`)

Managed by `chatModel.userTags`, `chatModel.presetTags`, and `chatModel.activeChatTagFilter`:

| Filter | `PresetTagKind` | Icon | Description |
|---|---|---|---|
| Group Reports | `GROUP_REPORTS` | Flag | Chats with moderation reports (non-collapsible) |
| Favorites | `FAVORITES` | Star | User-favorited chats |
| Contacts | `CONTACTS` | Person | Direct contacts and contact requests |
| Groups | `GROUPS` | Group | Group conversations (non-business) |
| Business | `BUSINESS` | Work | Business chat conversations |
| Notes | `NOTES` | Folder | Notes to self |
| Custom tags | `UserTag(ChatTag)` | Label/emoji | User-created tags with custom emoji and name |
| Unread | `ActiveFilter.Unread` | Filter list icon | Chats with unread messages (toggle via filter button) |

Display logic:
- When collapsible preset tags exceed 3 total (with user tags), they collapse into a `CollapsedTagsFilterView` dropdown menu
- Non-collapsible tags (`GROUP_REPORTS`) always show expanded
- User tags show with emoji or label icon; long-press opens `TagsDropdownMenu` (edit, delete, change order)
- "+" button at end opens `TagListEditor` for creating new tags

### Chat Preview Rows (`ChatPreviewView`)

Each row rendered by `ChatPreviewView` inside `ChatListNavLinkView`:

| Element | Description |
|---|---|
| Avatar | `ProfileImage` with overlay icons (inactive contact, left/removed group member) |
| Chat name | Display name with verified icon for verified contacts; colored for pending/connecting states |
| Last message preview | Truncated text of most recent message; draft indicator with edit icon; attachment icons |
| Timestamp | Relative time of last activity |
| Unread badge | Numeric count badge; distinct styling for mentions |
| Muted indicator | Bell-off icon when notifications are muted |
| Favorite indicator | Star icon for favorited chats |
| Incognito indicator | Shows when connected via incognito profile |
| Connection status | Shows connecting/pending state for incomplete connections |

Chat types handled by `ChatListNavLinkView`:
- `ChatInfo.Direct` -- direct contact chat
- `ChatInfo.Group` -- group chat (with in-progress indicator for joining)
- `ChatInfo.Local` -- note-to-self folder
- `ChatInfo.ContactRequest` -- incoming contact request (tap shows accept/reject alert)
- `ChatInfo.ContactConnection` -- pending connection (tap opens `ContactConnectionView`)

### Context Menu (Long Press / Right Click)

Each chat type provides specific dropdown menu items:

| Chat Type | Menu Items |
|---|---|
| Direct contact | Mark read/unread, toggle favorite, toggle notify, tag list, clear chat, delete contact |
| Group | Mark read/unread, toggle favorite, toggle notify, tag list, clear chat, archive all reports (moderator, when reports exist), leave group, delete group |
| Note folder | Mark read/unread, clear notes |
| Contact request | Accept, reject |
| Contact connection | Set name/alias, delete |

### Floating Elements

| Element | Condition | Description |
|---|---|---|
| One-hand UI card (`ToggleChatListCard`) | `oneHandUICardShown == false` | Dismissible card introducing bottom toolbar mode with toggle switch |
| Address creation card (`AddressCreationCard`) | `addressCreationCardShown == false` | Prompts user to create a SimpleX address; tappable card opens `UserAddressLearnMore` |
| FAB (new chat button) | Standard mode, search empty, chat running | `FloatingActionButton` at bottom-right, pencil icon, opens `NewChatSheet` |

### Empty States

| State | Display |
|---|---|
| Loading | "Loading chats..." centered text |
| No chats | "You have no chats" centered text |
| No filtered chats | "No chats in list [tag name]" or "No unread chats" with clickable filter reset |
| No search results | "No chats found" centered text |

## Source Files

| File | Path |
|---|---|
| `ChatListView.kt` | `views/chatlist/ChatListView.kt` |
| `ChatListNavLinkView.kt` | `views/chatlist/ChatListNavLinkView.kt` |
| `ChatPreviewView.kt` | `views/chatlist/ChatPreviewView.kt` |
| `UserPicker.kt` | `views/chatlist/UserPicker.kt` |
| `TagListView.kt` | `views/chatlist/TagListView.kt` |
| `PlatformHomeRoute.kt` | `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt` |
| `NomeHomeRoute.android.kt` | `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt` |
| `NomeHomeStateAdapter.kt` | `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt` |
