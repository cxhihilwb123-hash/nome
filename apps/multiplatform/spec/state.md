# State Management

## Table of Contents

1. [Overview](#1-overview)
2. [ChatModel](#2-chatmodel)
3. [ChatsContext](#3-chatscontext)
4. [Chat](#4-chat)
5. [AppPreferences](#5-apppreferences)
6. [Nome P13 Ephemeral Connection State](#6-nome-p13-ephemeral-connection-state)
7. [Source Files](#7-source-files)

---

## 1. Overview

SimpleX Chat uses a **singleton-based, Compose-reactive state model**. The primary state holder is `ChatModel`, a Kotlin `object` annotated with `@Stable`. All mutable fields are Compose `MutableState`, `MutableStateFlow`, or `SnapshotStateList`/`SnapshotStateMap` instances, which trigger Compose recomposition on mutation.

There is no ViewModel layer, no dependency injection framework, and no Redux/MVI pattern. The architecture is:

```
ChatModel (singleton, global Compose state)
    |
    +-- ChatController (command dispatch + event processing)
    |       |
    |       +-- sendCmd() -> chatSendCmdRetry() [JNI]
    |       +-- recvMsg() -> chatRecvMsgWait() [JNI]
    |       +-- processReceivedMsg() -> mutates ChatModel fields
    |
    +-- AppPreferences (150+ SharedPreferences via multiplatform-settings)
    |
    +-- ChatsContext (primary) -- chat list + current chat items
    +-- ChatsContext? (secondary) -- optional second context for dual-pane/support chat
```

State mutations originate from two sources:
1. **User actions**: Compose UI handlers call `api*()` suspend functions on `ChatController`, which send commands to the Haskell core, receive responses, and update `ChatModel`.
2. **Core events**: The receiver coroutine (`startReceiver`) calls `processReceivedMsg()`, which updates `ChatModel` fields on `Dispatchers.Main`.

Chat-list loading additionally uses a typed generation contract in the same singleton. The contract distinguishes a real empty result from failure, no-current-user, and stale results without adding a second model, core bridge, or protocol.

---

<a id="ChatModel"></a>

## 2. ChatModel

Defined as [`@Stable object ChatModel`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L135-L137).

### Controller Reference

| Field | Type | Purpose |
|---|---|---|
| [`controller`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L138) | `ChatController` | Reference to the `ChatController` singleton |

### User State

| Field | Type | Purpose |
|---|---|---|
| [`currentUser`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L140) | `MutableState<User?>` | Currently active user profile |
| [`users`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L141) | `SnapshotStateList<UserInfo>` | All user profiles (multi-account) |
| [`localUserCreated`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L142) | `MutableState<Boolean?>` | Whether a local user has been created (null = unknown during init) |
| [`setDeliveryReceipts`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L139) | `MutableState<Boolean>` | Trigger for delivery receipts setup dialog |
| [`switchingUsersAndHosts`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L151) | `MutableState<Boolean>` | True while switching active user/remote host |
| [`changingActiveUserMutex`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L249) | `Mutex` | Prevents concurrent user switches |

### Chat Runtime State

| Field | Type | Purpose |
|---|---|---|
| [`chatRunning`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L143) | `MutableState<Boolean?>` | `null` = initializing, `true` = running, `false` = stopped |
| [`chatDbChanged`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L144) | `MutableState<Boolean>` | Database was changed externally (needs restart) |
| [`chatDbEncrypted`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L145) | `MutableState<Boolean?>` | Whether database is encrypted |
| [`chatDbStatus`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L146) | `MutableState<DBMigrationResult?>` | Result of database migration attempt |
| [`ctrlInitInProgress`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L147) | `MutableState<Boolean>` | Controller initialization in progress |
| [`dbMigrationInProgress`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L148) | `MutableState<Boolean>` | Database migration in progress |
| [`incompleteInitializedDbRemoved`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L149) | `MutableState<Boolean>` | Tracks if incomplete DB files were removed (prevents infinite retry) |

### Nome Android P01 process-only state

P01 adds no `ChatModel`, preference, saved-instance, protocol, or database state. Android owns only:

| State | Lifetime | Payload boundary |
|---|---|---|
| database key read class | process-local, cleared by successful decrypt or an accepted manual/restored open generation | missing alias or unreadable material plus existing initial-random Boolean; no key/exception text |
| atomic attempt generation | process-local across rotation/background | monotonically increasing number only; one active action |
| backup-copy presentation | process-local and source-bound | copied/copy-failed enum plus accepted generation, no file/path/error payload |
| entered passphrase | composable `remember`, never saveable | cleared on submit, terminal/root change, background stop, and disposal |

`DBMigrationResult`, `ctrlInitInProgress`, and `dbMigrationInProgress` remain the authoritative
model/core truth. Local action state cannot fabricate or replace a native subtype.

### Current Chat State

| Field | Type | Purpose |
|---|---|---|
| [`chatId`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L154) | `MutableState<String?>` | ID of the currently open chat (null = chat list shown) |
| [`chatAgentConnId`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L155) | `MutableState<String?>` | Agent connection ID for current chat |
| [`chatSubStatus`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L156) | `MutableState<SubscriptionStatus?>` | Subscription status for current chat |
| [`openAroundItemId`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L157) | `MutableState<Long?>` | Item ID to scroll to when opening chat |
| [`chatsContext`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L158) | `ChatsContext` | Primary chat context (see [ChatsContext](#3-chatscontext)) |
| [`secondaryChatsContext`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L159) | `MutableState<ChatsContext?>` | Optional secondary context for dual-pane views |
| [`chats`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L161) | `State<List<Chat>>` | Derived from `chatsContext.chats` |
| [`deletedChats`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L164) | `MutableState<List<Pair<Long?, String>>>` | Recently deleted chats (rhId, chatId) |

### Chat List Load Contract

The shared [`ChatListLoadGeneration`, `ChatListLoadState`, and `ChatListLoadResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106) types make chat-list truth explicit:

- A generation is the `(remoteHostId, userId)` identity for one authoritative list.
- `Loading(generation, hideRows)` records an in-flight request and whether old rows may remain visible.
- `Loaded(generation)` proves that the current generation has been applied.
- `Unavailable(generation)` records a failed current-generation load without turning it into an empty list.
- `NoCurrentUser(remoteHostId)` is separate from both failure and a successful empty list.
- Results are correspondingly typed as `Success(generation, chats)`, `Failure(generation)`, or `NoCurrentUser(remoteHostId)`.

[`beginChatListLoad` and `applyChatListLoadResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L264-L311) enforce the application contract. A success or failure is accepted only when its attempt ID is current and its generation still matches the active user and remote host; stale completions return `false` and do not mutate truth. On success, `ChatsContext.updateChats` runs **before** `Loaded` is published. A matching failure publishes `Unavailable` but retains the last chat rows. `NoCurrentUser` clears rows only if there is still no current user on the same host, and clears them before publishing the state.

[`apiGetChatsResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081) is the typed producer. It rejects an `ApiChats` response whose user does not match the requested generation. The compatibility [`apiGetChats`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1083-L1088) still folds non-success outcomes to an empty list for legacy callers, so code that needs authoritative loading truth must use the typed function and the application contract above. This wraps the existing `CC.ApiGetChats` / `CR.ApiChats` path; it does not change native core or wire protocol.

The Android Nome [`NomeHomeStateAdapter`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L29-L112) is a pure presentation derivation over this shared state. It does not cache a second chat list or own navigation. Its network input comes from [`NetworkObserver.platformNetworkInfo`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L16-L24), whose `null` value means Android has not made its first observation; the adapter renders that as `UNKNOWN`, not online. Its first-use and filtered-no-result outputs are defensive renderer contracts in the current route graph because onboarding consumes no-user and the bounded home has no filter producer. Desktop does not consume this Android-only presentation adapter and keeps its existing home content fallback.

### Group Members

| Field | Type | Purpose |
|---|---|---|
| [`groupMembers`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L166) | `MutableState<List<GroupMember>>` | Members of currently viewed group |
| [`groupMembersIndexes`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L167) | `MutableState<Map<Long, Int>>` | Index lookup by `groupMemberId` |
| [`membersLoaded`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L168) | `MutableState<Boolean>` | Whether group members have been loaded |

### Chat Tags and Filters

| Field | Type | Purpose |
|---|---|---|
| [`userTags`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L174) | `MutableState<List<ChatTag>>` | User-defined chat tags |
| [`activeChatTagFilter`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L175) | `MutableState<ActiveFilter?>` | Currently active filter in chat list |
| [`presetTags`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L176) | `SnapshotStateMap<PresetTagKind, Int>` | Counts for preset tag categories (favorites, groups, contacts, etc.) |
| [`unreadTags`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L177) | `SnapshotStateMap<Long, Int>` | Unread counts per user-defined tag |

### Terminal and Developer

| Field | Type | Purpose |
|---|---|---|
| [`terminalsVisible`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L181) | `Set<Boolean>` | Tracks which terminal views are visible (default vs floating) |
| [`terminalItems`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L182) | `MutableState<List<TerminalItem>>` | Command/response log for developer terminal |

### Calls (WebRTC)

| Field | Type | Purpose |
|---|---|---|
| [`callManager`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L217) | `CallManager` | WebRTC call lifecycle manager |
| [`callInvitations`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L218) | `SnapshotStateMap<String, RcvCallInvitation>` | Pending incoming call invitations keyed by chatId |
| [`activeCallInvitation`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L219) | `MutableState<RcvCallInvitation?>` | Currently displayed incoming call invitation |
| [`activeCall`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L220) | `MutableState<Call?>` | Currently active call |
| [`activeCallViewIsVisible`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L221) | `MutableState<Boolean>` | Whether call UI is showing |
| [`activeCallViewIsCollapsed`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L222) | `MutableState<Boolean>` | Whether call UI is in PiP/collapsed mode |
| [`callCommand`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L223) | `SnapshotStateList<WCallCommand>` | Pending WebRTC commands |
| [`showCallView`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L224) | `MutableState<Boolean>` | Call view visibility toggle |
| [`switchingCall`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L225) | `MutableState<Boolean>` | True during call switching |

### Compose Draft and Sharing

| Field | Type | Purpose |
|---|---|---|
| [`draft`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L232) | `MutableState<ComposeState?>` | Saved compose draft for current chat |
| [`draftChatId`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L233) | `MutableState<String?>` | Chat ID the draft belongs to |
| [`sharedContent`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L236) | `MutableState<SharedContent?>` | Content received via share intent or internal forwarding |

### Remote Hosts

| Field | Type | Purpose |
|---|---|---|
| [`remoteHosts`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L255) | `SnapshotStateList<RemoteHostInfo>` | Connected remote hosts (for desktop-mobile pairing) |
| [`currentRemoteHost`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L256) | `MutableState<RemoteHostInfo?>` | Currently selected remote host |
| [`remoteHostPairing`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L259) | `MutableState<Pair<RemoteHostInfo?, RemoteHostSessionState>?>` | Remote host pairing state |
| [`remoteCtrlSession`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L260) | `MutableState<RemoteCtrlSession?>` | Remote controller session |

### Miscellaneous UI State

| Field | Type | Purpose |
|---|---|---|
| [`userAddress`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L183) | `MutableState<UserContactLinkRec?>` | User's public contact address |
| [`chatItemTTL`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L184) | `MutableState<ChatItemTTL>` | Chat item time-to-live setting |
| [`clearOverlays`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L187) | `MutableState<Boolean>` | Signal to close all overlays/modals |
| [`appOpenUrl`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L195) | `MutableState<AppOpenUrl?>` | Queued contact/invitation URI with immutable [`remoteHostId`, `uri`, and external/internal ingress `source`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L1287-L1296); provenance survives onboarding so only Android external `ACTION_VIEW` ingress opts into P13 |
| [`appOpenUrlConnecting`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L196) | `MutableState<Boolean>` | Whether a deep link connection is in progress |
| [`newChatSheetVisible`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L197) | `MutableState<Boolean>` | Whether new chat bottom sheet is visible |
| [`fullscreenGalleryVisible`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L200) | `MutableState<Boolean>` | Fullscreen gallery mode |
| [`notificationPreviewMode`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L203) | `MutableState<NotificationPreviewMode>` | Notification content preview level |
| [`showAuthScreen`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L212) | `MutableState<Boolean>` | Whether to show authentication screen |
| [`showChatPreviews`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L214) | `MutableState<Boolean>` | Whether to show chat preview text in list |
| [`clipboardHasText`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L241) | `MutableState<Boolean>` | System clipboard has text content |
| [`networkInfo`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L242) | `MutableState<UserNetworkInfo>` | Last network fact acknowledged by the core path; Android's direct first-observation state is separate |
| [`conditions`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L244) | `MutableState<ServerOperatorConditionsDetail>` | Server operator terms/conditions |
| [`updatingProgress`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L246) | `MutableState<Float?>` | Progress indicator for app updates |
| [`simplexLinkMode`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L239) | `MutableState<SimplexLinkMode>` | How SimpleX links are displayed |
| [`migrationState`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L230) | `MutableState<MigrationToState?>` | Database migration to new device state |
| [`showingInvitation`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L228) | `MutableState<ShowingInvitation?>` | Currently displayed invitation |
| [`desktopOnboardingRandomPassword`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L190) | `MutableState<Boolean>` | Desktop: user skipped password setup |
| [`filesToDelete`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L238) | `MutableSet<File>` | Temporary files pending cleanup |

---

<a id="ChatsContext"></a>

## 3. ChatsContext

Defined as the inner [`ChatsContext`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L438):

```kotlin
class ChatsContext(val secondaryContextFilter: SecondaryContextFilter?)
```

`ChatsContext` holds the chat list and current chat items for a given context. The `ChatModel` maintains a **primary** context (`chatsContext`) and an optional **secondary** context (`secondaryChatsContext`). The secondary context is used for:
- **Group support chat scope** (`SecondaryContextFilter.GroupChatScopeContext`) -- viewing member support threads alongside the main group chat
- **Message content tag filtering** (`SecondaryContextFilter.MsgContentTagContext`) -- filtering messages by content type

### Fields

| Field | Type | Purpose |
|---|---|---|
| [`secondaryContextFilter`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L438) | `SecondaryContextFilter?` | Filter type: null = primary, GroupChatScope or MsgContentTag |
| [`chats`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L439) | `MutableState<SnapshotStateList<Chat>>` | List of all chats in this context |
| [`chatItems`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L444) | `MutableState<SnapshotStateList<ChatItem>>` | Items for the currently open chat in this context |
| [`chatState`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L446) | `ActiveChatState` | Tracks unread counts, splits, scroll state |

### Derived Properties

| Property | Purpose |
|---|---|
| [`contentTag`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L452) | `MsgContentTag?` -- content filter tag if context is MsgContentTag |
| [`groupScopeInfo`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L459) | `GroupChatScopeInfo?` -- group scope if context is GroupChatScope |
| [`isUserSupportChat`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L466) | True when viewing own support chat (no specific member) |

### Key Operations

- `addChat(chat)` -- adds chat at index 0, triggers pop animation
- `reorderChat(chat, toIndex)` -- reorders chat list (e.g., when a chat receives a new message)
- `updateChatInfo(rhId, cInfo)` -- updates chat metadata while preserving connection stats
- `hasChat(rhId, id)` / `getChat(id)` -- lookup methods

### ActiveChatState

Defined as [`ActiveChatState`](../common/src/commonMain/kotlin/chat/simplex/common/views/chat/ChatItemsMerger.kt#L196):

```kotlin
data class ActiveChatState(
    val splits: MutableStateFlow<List<Long>> = MutableStateFlow(emptyList()),
    val unreadAfterItemId: MutableStateFlow<Long> = MutableStateFlow(-1L),
    val totalAfter: MutableStateFlow<Int> = MutableStateFlow(0),
    val unreadTotal: MutableStateFlow<Int> = MutableStateFlow(0),
    val unreadAfter: MutableStateFlow<Int> = MutableStateFlow(0),
    val unreadAfterNewestLoaded: MutableStateFlow<Int> = MutableStateFlow(0)
)
```

This tracks the scroll position and unread item accounting for the lazy-loaded chat item list:

| Field | Purpose |
|---|---|
| `splits` | List of item IDs where pagination gaps exist (items not yet loaded) |
| `unreadAfterItemId` | The item ID that marks the boundary of "read" vs "unread after" |
| `totalAfter` | Total items after the unread boundary |
| `unreadTotal` | Total unread items in the chat |
| `unreadAfter` | Unread items after the boundary (exclusive) |
| `unreadAfterNewestLoaded` | Unread items after the newest loaded batch |

---

<a id="Chat"></a>

## 4. Chat

Defined as [`Chat`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L1437-L1444):

```kotlin
@Serializable @Stable
data class Chat(
    val remoteHostId: Long?,
    val chatInfo: ChatInfo,
    val chatItems: List<ChatItem>,
    val chatStats: ChatStats = ChatStats()
)
```

### Fields

| Field | Type | Purpose |
|---|---|---|
| `remoteHostId` | `Long?` | Remote host ID (null = local) |
| `chatInfo` | `ChatInfo` | Sealed class: `Direct`, `Group`, `Local`, `ContactRequest`, `ContactConnection`, `InvalidJSON` |
| `chatItems` | `List<ChatItem>` | Latest chat items (summary; full list is in `ChatsContext.chatItems`) |
| `chatStats` | `ChatStats` | Unread counts and stats |

<a id="ChatStats"></a>

### ChatStats

Defined as [`ChatStats`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L1479-L1489):

```kotlin
data class ChatStats(
    val unreadCount: Int = 0,
    val unreadMentions: Int = 0,
    val reportsCount: Int = 0,
    val minUnreadItemId: Long = 0,
    val unreadChat: Boolean = false
)
```

### Derived Properties

| Property | Source | Purpose |
|---|---|---|
| `id` | [`Chat.id`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L1460) | Chat ID derived from `chatInfo.id` |
| `unreadTag` | [`Chat.unreadTag`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L1454) | Whether chat counts as "unread" for tag filtering (considers notification settings) |
| `supportUnreadCount` | [`Chat.supportUnreadCount`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L1462) | Unread count in support/moderation context |
| `nextSendGrpInv` | [`Chat.nextSendGrpInv`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L1448) | Whether next message should send group invitation |

<a id="ChatInfo"></a>

### ChatInfo Variants

[`ChatInfo`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L1500-L1502) is a sealed class:

| Variant | SerialName | Key Data |
|---|---|---|
| `ChatInfo.Direct` | `"direct"` | `contact: Contact` |
| `ChatInfo.Group` | `"group"` | `groupInfo: GroupInfo, groupChatScope: GroupChatScopeInfo?` |
| `ChatInfo.Local` | `"local"` | `noteFolder: NoteFolder` |
| `ChatInfo.ContactRequest` | `"contactRequest"` | `contactRequest: UserContactRequest` |
| `ChatInfo.ContactConnection` | `"contactConnection"` | `contactConnection: PendingContactConnection` |
| `ChatInfo.InvalidJSON` | `"invalidJSON"` | `json: String` |

### RelayStatus (Channels)

[`RelayStatus`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L2454) is an `enum class` modelling a relay's lifecycle for a channel on the owner's side. Serialized as a lowercase string via `@SerialName`.

| Case | SerialName | Meaning |
|---|---|---|
| `RsNew` | `"new"` | Allocated locally; not yet sent |
| `RsInvited` | `"invited"` | `XGrpRelayInv` sent, awaiting `XGrpRelayAcpt` |
| `RsAccepted` | `"accepted"` | Accepted, link-data update pending |
| `RsActive` | `"active"` | Listed in channel link data; forwarding |
| `RsInactive` | `"inactive"` | No longer in link data or backend reports it removed |
| `RsRejected` | `"rejected"` | Relay sent `XGrpRelayReject` for the channel link; final on the owner side. Clearable only by the relay operator running `/group allow #<channel>`. The owner-side `GroupMember.memberStatus` is also set to `MemLeft` so the relay renders identically to one that explicitly left (`MemRejected` is reserved for the knocking-admission flow). |

The `text` extension on the enum returns the localized status string (resource key `relay_status_*`, with `relay_status_rejected` = "rejected").

---

<a id="AppPreferences"></a>
<a id="appPrefs"></a>

## 5. AppPreferences

Defined as [`class AppPreferences`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L102).

Uses the `multiplatform-settings` library (`com.russhwolf.settings.Settings`) for cross-platform key-value storage (Android `SharedPreferences` / Desktop `java.util.prefs.Preferences`).

The `AppPreferences` instance is created lazily as [`ChatController.appPrefs`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L513):
```kotlin
val appPrefs: AppPreferences by lazy { AppPreferences() }
```

### Nome Android one-time locale initialization

[`NomeLocaleInitializer`](../android/src/main/java/chat/simplex/app/nome/NomeLocaleInitializer.kt)
is an Android/client-only adapter around the existing nullable `appLanguage` preference. Before
`initHaskell()` or `initMultiplatform()` can create process state, `SimplexApp` freezes only these
non-personal installation facts:

- the versioned `nome_product/locale_policy_version` marker;
- whether package first-install and last-update timestamps are equal;
- whether either official chat/agent database file already exists;
- whether the upstream application-preference store already contains any value.

After `initMultiplatform()` makes the established `ChatController.appPrefs.appLanguage` owner
available, the adapter reads that preference once. It writes `zh-CN` only when the marker is
absent, the language is null, package metadata identifies a non-updated install, and both database
and upstream-preference evidence were absent before initialization. Upgrade, restored,
contradictory, existing-data, explicit-language, or already-marked cases never overwrite the
language. A marker is committed after the first decision, including conservative preserve
decisions. Unsupported stored language values are preserved and produce only a fixed,
non-personal warning; the existing Android resource fallback remains authoritative.

This adapter adds no user/account state, identity switch hook, database migration, Desktop/iOS
behavior, or second locale preference.

### Preference Categories

#### Notifications (lines 96-103)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `notificationsMode` | `NotificationsMode` | `SERVICE` (if previously enabled) | OFF / SERVICE / PERIODIC |
| `notificationPreviewMode` | `String` | `"message"` | message / contact / hidden |
| `canAskToEnableNotifications` | `Boolean` | `true` | Whether to show notification enable prompt |
| `backgroundServiceNoticeShown` | `Boolean` | `false` | Background service notice already shown |
| `backgroundServiceBatteryNoticeShown` | `Boolean` | `false` | Battery notice already shown |
| `autoRestartWorkerVersion` | `Int` | `0` | Worker version for periodic restart |

#### Calls (lines 105-111)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `webrtcPolicyRelay` | `Boolean` | `true` | Use TURN relay for WebRTC |
| `callOnLockScreen` | `CallOnLockScreen` | `SHOW` | DISABLE / SHOW / ACCEPT |
| `webrtcIceServers` | `String?` | `null` | Custom ICE servers |
| `experimentalCalls` | `Boolean` | `false` | Enable experimental call features |

#### Authentication (lines 107-110)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `performLA` | `Boolean` | `false` | Enable local authentication |
| `laMode` | `LAMode` | default | Authentication mode |
| `laLockDelay` | `Int` | `30` | Seconds before re-auth required |
| `laNoticeShown` | `Boolean` | `false` | LA notice shown |

#### Privacy (lines 112-128)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `privacyProtectScreen` | `Boolean` | `true` | FLAG_SECURE on Android |
| `privacyAcceptImages` | `Boolean` | `true` | Auto-accept images |
| `privacyLinkPreviews` | `Boolean` | `true` | Generate link previews |
| `privacySanitizeLinks` | `Boolean` | `false` | Remove tracking params from links |
| `simplexLinkMode` | `SimplexLinkMode` | `DESCRIPTION` | DESCRIPTION / FULL / BROWSER |
| `privacyShowChatPreviews` | `Boolean` | `true` | Show chat previews in list |
| `privacySaveLastDraft` | `Boolean` | `true` | Save compose draft |
| `privacyDeliveryReceiptsSet` | `Boolean` | `false` | Delivery receipts configured |
| `privacyEncryptLocalFiles` | `Boolean` | `true` | Encrypt local files |
| `privacyAskToApproveRelays` | `Boolean` | `true` | Ask before using relays |
| `privacyMediaBlurRadius` | `Int` | `0` | Blur radius for media |

#### Network (lines 140-175)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `networkUseSocksProxy` | `Boolean` | `false` | Enable SOCKS proxy |
| `networkProxy` | `NetworkProxy` | localhost:9050 | Proxy host/port |
| `networkSessionMode` | `TransportSessionMode` | default | Session mode |
| `networkSMPProxyMode` | `SMPProxyMode` | default | SMP proxy mode |
| `networkSMPProxyFallback` | `SMPProxyFallback` | default | Proxy fallback policy |
| `networkHostMode` | `HostMode` | default | Host mode (onion routing) |
| `networkRequiredHostMode` | `Boolean` | `false` | Enforce host mode |
| `networkSMPWebPortServers` | `SMPWebPortServers` | default | Web port server config |
| `networkShowSubscriptionPercentage` | `Boolean` | `false` | Show subscription stats |
| `networkTCPConnectTimeout*` | `Long` | varies | TCP connect timeouts (background/interactive) |
| `networkTCPTimeout*` | `Long` | varies | TCP operation timeouts |
| `networkTCPTimeoutPerKb` | `Long` | varies | Per-KB timeout |
| `networkRcvConcurrency` | `Int` | default | Receive concurrency |
| `networkSMPPingInterval` | `Long` | default | SMP ping interval |
| `networkSMPPingCount` | `Int` | default | SMP ping count |
| `networkEnableKeepAlive` | `Boolean` | default | TCP keep-alive |
| `networkTCPKeepIdle` | `Int` | default | Keep-alive idle time |
| `networkTCPKeepIntvl` | `Int` | default | Keep-alive interval |
| `networkTCPKeepCnt` | `Int` | default | Keep-alive count |

#### Appearance (lines 213-233)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `currentTheme` | `String` | `"SYSTEM"` | Active theme name |
| `systemDarkTheme` | `String` | `"SIMPLEX"` | Theme for system dark mode |
| `currentThemeIds` | `Map<String, String>` | empty | Theme ID per base theme |
| `themeOverrides` | `List<ThemeOverrides>` | empty | Custom theme overrides |
| `profileImageCornerRadius` | `Float` | `22.5f` | Avatar corner radius |
| `chatItemRoundness` | `Float` | `0.75f` | Message bubble roundness |
| `chatItemTail` | `Boolean` | `true` | Show bubble tail |
| `fontScale` | `Float` | `1f` | Font scale factor |
| `densityScale` | `Float` | `1f` | UI density scale |
| `inAppBarsAlpha` | `Float` | varies | Bar transparency |
| `appearanceBarsBlurRadius` | `Int` | 50 or 0 | Bar blur radius (device-dependent) |

#### Developer (lines 135-139)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `developerTools` | `Boolean` | `false` | Enable developer tools |
| `logLevel` | `LogLevel` | `WARNING` | Log level |
| `showInternalErrors` | `Boolean` | `false` | Show internal errors to user |
| `showSlowApiCalls` | `Boolean` | `false` | Alert on slow API calls |
| `terminalAlwaysVisible` | `Boolean` | `false` | Floating terminal window (desktop) |

#### Database (lines 188-208)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `onboardingStage` | `OnboardingStage` | `OnboardingComplete` | Current onboarding step |
| `storeDBPassphrase` | `Boolean` | `true` | Store DB passphrase in keystore |
| `initialRandomDBPassphrase` | `Boolean` | `false` | DB was created with random passphrase |
| `encryptedDBPassphrase` | `String?` | null | Encrypted DB passphrase |
| `confirmDBUpgrades` | `Boolean` | `false` | Confirm DB migrations |
| `chatStopped` | `Boolean` | `false` | Chat was explicitly stopped |
| `chatLastStart` | `Instant?` | null | Last chat start timestamp |
| `newDatabaseInitialized` | `Boolean` | `false` | DB successfully initialized at least once |
| `shouldImportAppSettings` | `Boolean` | `false` | Import settings after DB import |
| `selfDestruct` | `Boolean` | `false` | Self-destruct enabled |
| `selfDestructDisplayName` | `String?` | null | Display name for self-destruct profile |

#### UI Preferences (lines 255-257)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `oneHandUI` | `Boolean` | `true` | One-hand mode |
| `chatBottomBar` | `Boolean` | `true` | Bottom bar in chat |

#### Remote Access (lines 238-243)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `deviceNameForRemoteAccess` | `String` | device model | Device name shown to paired devices |
| `confirmRemoteSessions` | `Boolean` | `false` | Confirm remote sessions |
| `connectRemoteViaMulticast` | `Boolean` | `false` | Use multicast for discovery |
| `connectRemoteViaMulticastAuto` | `Boolean` | `true` | Auto-connect via multicast |
| `offerRemoteMulticast` | `Boolean` | `true` | Offer multicast connection |

#### Migration (lines 189-190)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `migrationToStage` | `String?` | null | Migration-to-device progress |
| `migrationFromStage` | `String?` | null | Migration-from-device progress |

#### Updates and Versioning (lines 184-186, 235-237)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `appUpdateChannel` | `AppUpdatesChannel` | `DISABLED` | DISABLED / STABLE / BETA |
| `appSkippedUpdate` | `String` | `""` | Skipped update version |
| `appUpdateNoticeShown` | `Boolean` | `false` | Update notice shown |
| `whatsNewVersion` | `String?` | null | Last "What's New" version seen |
| `lastMigratedVersionCode` | `Int` | `0` | Last app version code for data migrations |
| `customDisappearingMessageTime` | `Int` | `300` | Custom disappearing message time (seconds) |

### Preference Utility Types

The `SharedPreference<T>` wrapper (defined in SimpleXAPI.kt) provides:
- `get(): T` -- read current value
- `set(value: T)` -- write value
- `state: MutableState<T>` -- Compose-observable state (derived lazily)

Factory methods: `mkBoolPreference`, `mkIntPreference`, `mkLongPreference`, `mkFloatPreference`, `mkStrPreference`, `mkEnumPreference`, `mkSafeEnumPreference`, `mkDatePreference`, `mkMapPreference`, `mkTimeoutPreference`.

---

## 6. Nome P13 Ephemeral Connection State

P13 does not add a field to `ChatModel`, a ViewModel, saved instance state, or a second controller.
The raw external URI, resolved `CreatedConnLink`, and `LinkOwnerSig` remain in an ephemeral
callback closure owned by [`planAndConnect`](../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/ConnectPlan.kt#L25-L607).
The platform seam receives only
[`ConnectionPreviewUiModel`](../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.kt#L52-L89),
which contains display-safe type, warning, owner-proof, current-profile, and selected-identity
facts.

Android holds one non-saveable presentation reducer state:

| Phase | Entry | Allowed exit |
|---|---|---|
| `Ready` | successful real connection plan in one of seven eligible branches | identity change, one submit, or cancel |
| `Connecting` | reducer accepts the first submit | typed pending or typed failure; duplicate/out-of-order actions are ignored |
| `Failure` | already-existing, API failure, no user, or changed user/host | cancel or retry |
| `Replanning` | retry handoff | a new plan creates a new preview or delegates to the authoritative legacy branch |
| `Pending` | only `SentConfirmation` / `SentInvitation` with a real pending connection | done/cancel cleanup |

[`NomeConnectionPreviewStateAdapter`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewStateAdapter.kt#L6-L99)
is a pure reducer. The controller closure supplies an additional atomic single-submit guard across
recomposition and an idempotent cleanup guard. Before connect, the closure compares the planned
`(remoteHostId, userId)` with the active context. Retry resolves the current remote host and asks
the core for a fresh plan; it never reuses the old `ConnectionPlan`. While that request is active,
the existing route remains in `Replanning`. A typed planning failure, including no current user,
returns it to `Failure` without dropping the selected identity; a successful or changed
authoritative branch performs one modal handoff.

The existing `ChatModel.appOpenUrlConnecting` remains the external-ingress progress guard.
Cancel/dismiss/configuration cleanup resets it through the original cleanup callback. P13 writes
the shared model only when a typed successful connect returns a real
`PendingContactConnection`, which is applied through the existing `ChatsContext` update.

This state is unrelated to the P08 content adapter. `FIRST_USE` and `FILTERED_NO_RESULT` retain
their existing renderer-only production-reachability boundary.

---

## 7. Source Files

| File | Path | Key Contents |
|---|---|---|
| ChatModel.kt | [`common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt) | `ChatModel`, typed chat-list load contract, `ChatsContext`, `Chat`, `ChatInfo`, `ChatStats`, helper methods |
| SimpleXAPI.kt | [`common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt) | `AppPreferences`, `ChatController`, typed chat-list producer, typed P13 delegates, receiver and command bridge |
| ChatItemsMerger.kt | [`common/src/commonMain/kotlin/chat/simplex/common/views/chat/ChatItemsMerger.kt`](../common/src/commonMain/kotlin/chat/simplex/common/views/chat/ChatItemsMerger.kt) | `ActiveChatState`, chat item merge/diff logic |
| Core.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt) | `initChatController`, state initialization flow |
| App.kt | [`common/src/commonMain/kotlin/chat/simplex/common/App.kt`](../common/src/commonMain/kotlin/chat/simplex/common/App.kt) | `AppScreen`, `MainScreen`, top-level UI state reads |
| NomeHomeStateAdapter.kt | [`common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt) | Android-only pure presentation-state derivation |
| NetworkObserver.kt | [`common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt) | Direct Android network observation with explicit unknown-before-first-observation state |
| PlatformConnectionPreview.kt | [`common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.kt`](../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.kt) | Safe P13 model, branch policy, callbacks, and platform seam |
| NomeConnectionPreviewStateAdapter.kt | [`common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewStateAdapter.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewStateAdapter.kt) | Android-only pure P13 presentation reducer |

## 8. Nome server-operator state

For a fresh user, the core persists the Nome operator plus its SMP/XFTP user-server rows. A stored
server with `preset=True` that no longer matches an enabled operator is omitted from the grouped
settings presentation; it is not reclassified as a user custom server. A stored server with
`preset=False` remains in the custom group. Existing contacts and chats are not deleted by this
state transition.
