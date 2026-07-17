# Chat API Reference

## Table of Contents

1. [Overview](#1-overview)
2. [Command Categories](#2-command-categories)
   - 2.1 [User Management](#21-user-management)
   - 2.2 [Chat Lifecycle](#22-chat-lifecycle)
   - 2.3 [Message Operations](#23-message-operations)
   - 2.4 [Group Operations](#24-group-operations)
   - 2.5 [Contact Operations](#25-contact-operations)
   - 2.6 [File Operations](#26-file-operations)
   - 2.7 [Call Operations](#27-call-operations)
   - 2.8 [Settings & Network](#28-settings--network)
   - 2.9 [Chat Tags](#29-chat-tags)
   - 2.10 [Server Operators](#210-server-operators)
   - 2.11 [Archive](#211-archive)
3. [Response Types](#3-response-types)
4. [Event Types](#4-event-types)
5. [Error Types](#5-error-types)
6. [Source Files](#6-source-files)

---

## 1. Overview

The SimpleX Chat API bridge connects Kotlin/Compose UI code to the Haskell core via JNI. All communication follows a **command/response JSON protocol**:

```
Kotlin suspend fun api*()
  -> ChatController.sendCmd(rhId, CC.*, ctrl)
       -> serialize CC to cmdString (JSON)
       -> chatSendCmdRetry(ctrl, cmdString, retryNum)   [JNI / external fun]
            -> Haskell core processes command
            -> returns JSON response string
       -> json.decodeFromString<API>(responseString)
            -> API.Result(rhId, CR.*) or API.Error(rhId, ChatError)
  -> pattern-match on CR subclass -> update ChatModel / return data to UI
```

**Key types in the pipeline:**

| Type | Role | Location |
|------|------|----------|
| `CC` (sealed class) | Command definitions (~165 subclasses) | [`SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L3689) |
| `API` (sealed class) | Top-level response wrapper (`Result` / `Error`) | [`SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L6253) |
| `CR` (sealed class) | Chat response variants (~180 subclasses) | [`SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L6392) |
| `ChatError` (sealed class) | Error hierarchy | [`SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L7290) |
| `ChatController` (object) | Singleton hosting all `api*` functions | [`SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L521) |

**JNI bridge functions** (declared in [`Core.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L25)):

```kotlin
external fun chatMigrateInit(dbPath: String, dbKey: String, confirm: String): Array<Any>
external fun chatCloseStore(ctrl: ChatCtrl): String
external fun chatSendCmdRetry(ctrl: ChatCtrl, msg: String, retryNum: Int): String
external fun chatSendRemoteCmdRetry(ctrl: ChatCtrl, rhId: Int, msg: String, retryNum: Int): String
external fun chatRecvMsg(ctrl: ChatCtrl): String
external fun chatRecvMsgWait(ctrl: ChatCtrl, timeout: Int): String
```

<a id="sendCmd"></a>

**`sendCmd` flow** ([`sendCmd`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L845)):

1. Obtains the `ChatCtrl` handle (or uses the provided `otherCtrl`).
2. Serializes the `CC` command to its `cmdString`.
3. Dispatches to `Dispatchers.IO`; calls `chatSendCmdRetry` (local) or `chatSendRemoteCmdRetry` (remote host).
4. Decodes the returned JSON string into `API`.
5. Logs the result to the terminal item list.

<a id="startReceiver"></a>
<a id="recvMsg"></a>
<a id="processReceivedMsg"></a>

**Asynchronous event receiver** ([`startReceiver`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L695)):

A long-running coroutine on `Dispatchers.IO` repeatedly calls `chatRecvMsgWait` (blocking JNI). Each received `API` message is dispatched to [`processReceivedMsg`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2708), which pattern-matches on `CR` subclasses to update `ChatModel` state and trigger notifications.

---

<a id="CC"></a>

## 2. Command Categories

All functions below are `suspend fun` members of [`ChatController`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L521). The `rh` / `rhId` parameter is `Long?` identifying a remote host (`null` = local device).

### 2.1 User Management

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `apiGetActiveUser` | `rh: Long?, ctrl: ChatCtrl?` | Fetch the currently active user profile | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L882) |
| `apiCreateActiveUser` | `rh: Long?, p: Profile?, pastTimestamp: Boolean, ctrl: ChatCtrl?` | Create a new user profile and set it as active | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L892) |
| `listUsers` | `rh: Long?` | List all user profiles sorted by display name | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L912) |
| `apiSetActiveUser` | `rh: Long?, userId: Long, viewPwd: String?` | Switch the active user to a different profile | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L922) |
| `apiSetAllContactReceipts` | `rh: Long?, enable: Boolean` | Enable/disable delivery receipts for all contacts globally | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L929) |
| `apiSetUserContactReceipts` | `u: User, userMsgReceiptSettings: UserMsgReceiptSettings` | Set delivery receipt settings for user contacts | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L935) |
| `apiSetUserGroupReceipts` | `u: User, userMsgReceiptSettings: UserMsgReceiptSettings` | Set delivery receipt settings for user groups | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L941) |
| `apiSetUserAutoAcceptMemberContacts` | `u: User, enable: Boolean` | Toggle auto-accept for member contact requests | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L947) |
| `apiHideUser` | `u: User, viewPwd: String` | Hide a user profile behind a password | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L953) |
| `apiUnhideUser` | `u: User, viewPwd: String` | Unhide a previously hidden user profile | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L956) |
| `apiMuteUser` | `u: User` | Mute all notifications for a user profile | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L959) |
| `apiUnmuteUser` | `u: User` | Unmute notifications for a user profile | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L962) |
| `apiDeleteUser` | `u: User, delSMPQueues: Boolean, viewPwd: String?` | Delete a user profile and optionally its SMP queues | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L971) |
| `apiUpdateProfile` | `rh: Long?, profile: Profile` | Update the active user's display profile | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1788) |
| `apiSetProfileAddress` | `rh: Long?, on: Boolean` | Enable/disable including address in user profile | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1800) |
| `apiSetUserUIThemes` | `rh: Long?, userId: Long, themes: ThemeModeOverrides?` | Set UI theme overrides for a user | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1838) |

### 2.2 Chat Lifecycle

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `apiStartChat` | `ctrl: ChatCtrl?` | Start the chat engine (returns `true` if newly started) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L978) |
| `apiStopChat` | _(none)_ | Stop the chat engine | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L996) |
| `apiSetAppFilePaths` | `filesFolder, tempFolder, assetsFolder, remoteHostsFolder: String, ctrl: ChatCtrl?` | Configure file-system paths for the Haskell core | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1002) |
| `apiSetEncryptLocalFiles` | `enable: Boolean` | Enable/disable encryption of locally stored files | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1008) |
| `apiSaveAppSettings` | `settings: AppSettings` | Persist application settings to the core | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1010) |
| `apiGetAppSettings` | `settings: AppSettings` | Retrieve application settings from the core | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1016) |
| `apiGetChatsResult` | `rh: Long?` | Return typed success, failure, or no-current-user with a validated request generation | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081) |
| `apiGetChats` | `rh: Long?` | Compatibility wrapper that folds non-success outcomes to an empty list | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1083-L1088) |
| `apiGetChat` | `rh, type, id, scope, contentTag, pagination, search` | Fetch a single chat with paginated messages | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1099) |
| `apiGetChatContentTypes` | `rh: Long?, type: ChatType, id: Long, scope: GroupChatScope?` | Get available content type filters for a chat | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1112) |
| `apiClearChat` | `rh: Long?, type: ChatType, id: Long` | Delete all messages in a chat | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1781) |
| `apiDeleteChat` | `rh: Long?, type: ChatType, id: Long, chatDeleteMode: ChatDeleteMode` | Delete a chat (contact, group, connection, etc.) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1726) |
| `apiChatRead` | `rh: Long?, type: ChatType, id: Long` | Mark a chat as read | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1994) |
| `apiChatItemsRead` | `rh, type, id, scope, itemIds` | Mark specific chat items as read | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2008) |
| `apiChatUnread` | `rh: Long?, type: ChatType, id: Long, unreadChat: Boolean` | Toggle a chat's unread flag | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2015) |
| `getChatItemTTL` | `rh: Long?` | Get the auto-delete TTL for chat items | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1369) |
| `setChatItemTTL` | `rh: Long?, chatItemTTL: ChatItemTTL` | Set the auto-delete TTL for chat items | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1382) |
| `setChatTTL` | `rh: Long?, chatType, id, chatItemTTL` | Set TTL for a specific chat | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1389) |

#### Typed chat-list result contract

[`apiGetChatsResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081) captures the active `(remoteHostId, userId)` generation before issuing the existing `CC.ApiGetChats` command. `CR.ApiChats` becomes `Success` only when its user matches that request generation; a mismatched response, command error, or decoding failure becomes `Failure`; absence of a current user becomes `NoCurrentUser`. Coroutine cancellation is rethrown rather than converted to failure.

Callers that require authoritative list truth first call [`ChatModel.beginChatListLoad`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L262-L270), then pass the typed result to [`ChatModel.applyChatListLoadResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L272-L303). The model rejects stale generations. A success updates chats before publishing `Loaded`; a matching failure publishes `Unavailable` without clearing cached chats; guarded no-current-user clears chats before publishing its state. This is Kotlin-side result/state bookkeeping around the existing command and response: no Haskell core, JNI declaration, `CC`, `CR`, or wire-protocol variant was added.

### 2.3 Message Operations

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `apiSendMessages` | `rh, type, id, scope, live, ttl, composedMessages` | Send one or more messages to a chat | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1142) |
| `apiCreateChatItems` | `rh: Long?, noteFolderId: Long, composedMessages: List<ComposedMessage>` | Create items in a private notes folder | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1179) |
| `apiReportMessage` | `rh, groupId, chatItemId, reportReason, reportText` | Report a message in a group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1187) |
| `apiGetChatItemInfo` | `rh, type, id, scope, itemId` | Get delivery info for a specific chat item | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1194) |
| `apiForwardChatItems` | `rh, toChatType, toChatId, toScope, fromChatType, fromChatId, fromScope, itemIds, ttl` | Forward messages between chats | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1201) |
| `apiPlanForwardChatItems` | `rh, fromChatType, fromChatId, fromScope, chatItemIds` | Check forward feasibility before forwarding | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1213) |
| `apiUpdateChatItem` | `rh, type, id, scope, itemId, updatedMessage, live` | Edit an existing message | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1220) |
| `apiChatItemReaction` | `rh, type, id, scope, itemId, add, reaction` | Add or remove a reaction to a message | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1243) |
| `apiGetReactionMembers` | `rh: Long?, groupId: Long, itemId: Long, reaction: MsgReaction` | List members who reacted with a specific emoji | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1250) |
| `apiDeleteChatItems` | `rh, type, id, scope, itemIds, mode` | Delete messages (for self or for everyone) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1258) |
| `apiDeleteMemberChatItems` | `rh: Long?, groupId: Long, itemIds: List<Long>` | Moderate: delete another member's messages | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1265) |
| `apiArchiveReceivedReports` | `rh: Long?, groupId: Long` | Archive all received reports in a group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1272) |
| `apiDeleteReceivedReports` | `rh: Long?, groupId: Long, itemIds: List<Long>, mode: CIDeleteMode` | Delete specific received reports | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1279) |

### 2.4 Group Operations

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `apiNewGroup` | `rh: Long?, incognito: Boolean, groupProfile: GroupProfile` | Create a new group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2198) |
| `apiAddMember` | `rh: Long?, groupId: Long, contactId: Long, memberRole: GroupMemberRole` | Invite a contact to a group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2239) |
| `apiJoinGroup` | `rh: Long?, groupId: Long` | Accept a group invitation | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2248) |
| `apiAcceptMember` | `rh: Long?, groupId: Long, groupMemberId: Long, memberRole: GroupMemberRole` | Accept a member joining via group link | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2274) |
| `apiDeleteMemberSupportChat` | `rh: Long?, groupId: Long, groupMemberId: Long` | Delete a member's support chat | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2283) |
| `apiRemoveMembers` | `rh: Long?, groupId: Long, memberIds: List<Long>, withMessages: Boolean` | Remove members from a group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2290) |
| `apiMembersRole` | `rh: Long?, groupId: Long, memberIds: List<Long>, memberRole: GroupMemberRole` | Change the role of group members | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2299) |
| `apiBlockMembersForAll` | `rh: Long?, groupId: Long, memberIds: List<Long>, blocked: Boolean` | Block/unblock members for all group participants | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2308) |
| `apiLeaveGroup` | `rh: Long?, groupId: Long` | Leave a group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2317) |
| `apiListMembers` | `rh: Long?, groupId: Long` | List all members of a group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2324) |
| `apiUpdateGroup` | `rh: Long?, groupId: Long, groupProfile: GroupProfile` | Update group profile (name, image, etc.) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2331) |
| `apiCreateGroupLink` | `rh: Long?, groupId: Long, memberRole: GroupMemberRole` | Create a group invitation link | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2351) |
| `apiGroupLinkMemberRole` | `rh: Long?, groupId: Long, memberRole: GroupMemberRole` | Update the default role for group link joins | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2366) |
| `apiDeleteGroupLink` | `rh: Long?, groupId: Long` | Delete the group invitation link | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2375) |
| `apiGetGroupLink` | `rh: Long?, groupId: Long` | Retrieve the current group invitation link | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2385) |
| `apiAddGroupShortLink` | `rh: Long?, groupId: Long` | Create a short link for the group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2392) |
| `apiCreateMemberContact` | `rh: Long?, groupId: Long, groupMemberId: Long` | Create a direct contact from a group member | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2402) |
| `apiSendMemberContactInvitation` | `rh: Long?, contactId: Long, mc: MsgContent` | Send a direct message invitation to a group member | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2411) |
| `apiAcceptMemberContact` | `rh: Long?, contactId: Long` | Accept a member's direct contact invitation | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2420) |
| `apiSetMemberSettings` | `rh: Long?, groupId: Long, groupMemberId: Long, memberSettings: GroupMemberSettings` | Configure per-member settings (e.g., mentions) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1426) |
| `apiGroupMemberInfo` | `rh: Long?, groupId: Long, groupMemberId: Long` | Get a group member's info and connection stats | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1442) |
| `apiSetGroupAlias` | `rh: Long?, groupId: Long, localAlias: String` | Set a local alias for a group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1824) |

### 2.5 Contact Operations

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `apiAddContact` | `rh: Long?, incognito: Boolean` | Create a one-time invitation link for a new contact | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1533) |
| `apiSetConnectionIncognito` | `rh: Long?, connId: Long, incognito: Boolean` | Toggle incognito on a pending connection | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1544) |
| `apiChangeConnectionUser` | `rh: Long?, connId: Long, userId: Long` | Change the user profile on a pending connection | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1553) |
| `apiConnectPlan` | `rh: Long?, connLink: String, inProgress: MutableState<Boolean>` | Analyze a connection link before connecting | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1563) |
| `apiConnect` | `rh: Long?, incognito: Boolean, connLink: CreatedConnLink` | Connect via an invitation or address link | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1571) |
| `apiPrepareContact` | `rh, connLink, contactShortLinkData` | Prepare a contact chat from a short link (before connecting) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1652) |
| `apiPrepareGroup` | `rh, connLink, groupShortLinkData` | Prepare a group chat from a short link (before connecting) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1661) |
| `apiConnectPreparedContact` | `rh, contactId, incognito, msg` | Connect to a previously prepared contact | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1686) |
| `apiConnectPreparedGroup` | `rh, groupId, incognito, msg` | Join a previously prepared group | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1696) |
| `apiConnectContactViaAddress` | `rh: Long?, incognito: Boolean, contactId: Long` | Connect to a contact using their public address | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1706) |
| `apiDeleteContact` | `rh: Long?, id: Long, chatDeleteMode: ChatDeleteMode` | Delete a contact and return the deleted Contact | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1750) |
| `apiContactInfo` | `rh: Long?, contactId: Long` | Get a contact's connection stats and custom profile | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1435) |
| `apiSetContactAlias` | `rh: Long?, contactId: Long, localAlias: String` | Set a local display alias for a contact | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1817) |
| `apiSetConnectionAlias` | `rh: Long?, connId: Long, localAlias: String` | Set a local display alias for a pending connection | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1831) |
| `apiSetContactPrefs` | `rh: Long?, contactId: Long, prefs: ChatPreferences` | Update feature preferences for a contact | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1810) |
| `apiCreateUserAddress` | `rh: Long?` | Create a long-term public contact address | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1852) |
| `apiDeleteUserAddress` | `rh: Long?` | Delete the user's public contact address | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1868) |
| `apiAddMyAddressShortLink` | `rh: Long?` | Create a short link for the user's address | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1890) |
| `apiSetUserAddressSettings` | `rh: Long?, settings: AddressSettings` | Configure auto-accept for incoming contact requests | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1901) |
| `apiAcceptContactRequest` | `rh: Long?, incognito: Boolean, contactReqId: Long` | Accept an incoming contact request | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1915) |
| `apiRejectContactRequest` | `rh: Long?, contactReqId: Long` | Reject an incoming contact request | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1938) |
| `apiSwitchContact` | `rh: Long?, contactId: Long` | Initiate SMP server switch for a contact | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1463) |
| `apiAbortSwitchContact` | `rh: Long?, contactId: Long` | Abort an in-progress server switch | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1477) |
| `apiSyncContactRatchet` | `rh: Long?, contactId: Long, force: Boolean` | Force ratchet synchronization with a contact | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1491) |
| `apiGetContactCode` | `rh: Long?, contactId: Long` | Get the security verification code for a contact | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1505) |
| `apiVerifyContact` | `rh: Long?, contactId: Long, connectionCode: String?` | Verify a contact's security code | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1519) |

### 2.6 File Operations

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `receiveFiles` | `rhId, user, fileIds, userApprovedRelays, auto` | Accept and download one or more files (handles relay approval) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2052) |
| `receiveFile` | `rhId, user, fileId, userApprovedRelays, auto` | Accept and download a single file (convenience wrapper) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2168) |
| `cancelFile` | `rh: Long?, user: User, fileId: Long` | Cancel an in-progress file transfer and clean up | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2178) |
| `apiCancelFile` | `rh: Long?, fileId: Long, ctrl: ChatCtrl?` | Cancel a file transfer (low-level, returns updated chat item) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2186) |
| `uploadStandaloneFile` | `user: UserLike, file: CryptoFile, ctrl: ChatCtrl?` | Upload a standalone file (used for migration) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2022) |
| `downloadStandaloneFile` | `user: UserLike, url: String, file: CryptoFile, ctrl: ChatCtrl?` | Download a standalone file by URL | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2032) |
| `standaloneFileInfo` | `url: String, ctrl: ChatCtrl?` | Retrieve metadata for a standalone file link | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2042) |

### 2.7 Call Operations

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `apiGetCallInvitations` | `rh: Long?` | Retrieve pending call invitations | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1948) |
| `apiSendCallInvitation` | `rh: Long?, contact: Contact, callType: CallType` | Initiate a call by sending an invitation | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1955) |
| `apiRejectCall` | `rh: Long?, contact: Contact` | Reject an incoming call | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1960) |
| `apiSendCallOffer` | `rh, contact, rtcSession, rtcIceCandidates, media, capabilities` | Send a WebRTC call offer | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1965) |
| `apiSendCallAnswer` | `rh: Long?, contact: Contact, rtcSession: String, rtcIceCandidates: String` | Send a WebRTC call answer | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1972) |
| `apiSendCallExtraInfo` | `rh: Long?, contact: Contact, rtcIceCandidates: String` | Send additional ICE candidates during a call | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1978) |
| `apiEndCall` | `rh: Long?, contact: Contact` | End an active call | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1984) |
| `apiCallStatus` | `rh: Long?, contact: Contact, status: WebRTCCallStatus` | Report call status updates to the core | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1989) |

### 2.8 Settings & Network

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `apiSetNetworkConfig` | `cfg: NetCfg, showAlertOnError: Boolean, ctrl: ChatCtrl?` | Apply network configuration (SOCKS proxy, timeouts, etc.) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1396) |
| `apiSetNetworkInfo` | `networkInfo: UserNetworkInfo` | Update network reachability information | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1423) |
| `apiSetSettings` | `rh: Long?, type: ChatType, id: Long, settings: ChatSettings` | Update per-chat settings (notifications, favorites) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1416) |
| `apiStorageEncryption` | `currentKey: String, newKey: String` | Change the database encryption passphrase | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1040) |
| `testStorageEncryption` | `key: String, ctrl: ChatCtrl?` | Verify a database encryption key is correct | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1047) |
| `testProtoServer` | `rh: Long?, server: String` | Test connectivity to a protocol server | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1286) |
| `reconnectServer` | `rh: Long?, server: String` | Reconnect to a specific server | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1409) |
| `reconnectAllServers` | `rh: Long?` | Reconnect to all servers | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1414) |
| `apiSetChatUIThemes` | `rh: Long?, chatId: ChatId, themes: ThemeModeOverrides?` | Set per-chat UI theme overrides | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1845) |
| `apiContactQueueInfo` | `rh: Long?, contactId: Long` | Get server queue diagnostics for a contact | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1449) |
| `apiGroupMemberQueueInfo` | `rh: Long?, groupId: Long, groupMemberId: Long` | Get server queue diagnostics for a group member | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1456) |

### 2.9 Chat Tags

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `apiCreateChatTag` | `rh: Long?, tag: ChatTagData` | Create a new chat tag (folder/label) | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1120) |
| `apiSetChatTags` | `rh: Long?, type: ChatType, id: Long, tagIds: List<Long>` | Assign tags to a chat | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1128) |
| `apiDeleteChatTag` | `rh: Long?, tagId: Long` | Delete a chat tag | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1136) |
| `apiUpdateChatTag` | `rh: Long?, tagId: Long, tag: ChatTagData` | Update a chat tag's name or emoji | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1138) |
| `apiReorderChatTags` | `rh: Long?, tagIds: List<Long>` | Set the display order of chat tags | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1140) |

### 2.10 Server Operators

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `getServerOperators` | `rh: Long?` | Get server operator conditions detail | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1302) |
| `setServerOperators` | `rh: Long?, operators: List<ServerOperator>` | Update the list of server operators | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1309) |
| `getUserServers` | `rh: Long?` | Get the user's configured servers per operator | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1316) |
| `setUserServers` | `rh: Long?, userServers: List<UserOperatorServers>` | Save user's configured servers per operator | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1324) |
| `validateServers` | `rh: Long?, userServers: List<UserOperatorServers>` | Validate server configuration for errors | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1336) |
| `getUsageConditions` | `rh: Long?` | Get current and accepted usage conditions | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1344) |
| `setConditionsNotified` | `rh: Long?, conditionsId: Long` | Mark conditions as shown to user | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1351) |
| `acceptConditions` | `rh: Long?, conditionsId: Long, operatorIds: List<Long>` | Accept usage conditions for operators | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1358) |

### 2.11 Archive

| Command | Parameters | Description | Source |
|---------|-----------|-------------|------|
| `apiExportArchive` | `config: ArchiveConfig` | Export chat database to a ZIP archive | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1022) |
| `apiImportArchive` | `config: ArchiveConfig` | Import chat database from a ZIP archive | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1028) |
| `apiDeleteStorage` | _(none)_ | Delete all chat database storage | [source](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1034) |

<a id="ArchiveConfig"></a>

[`ArchiveConfig`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L4346):

```kotlin
class ArchiveConfig(
  val archivePath: String,
  val disableCompression: Boolean? = null,
  val parentTempDirectory: String? = null
)
```

---

<a id="API"></a>

## 3. Response Types

All command responses are deserialized into the [`API` sealed class](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L6253):

```kotlin
sealed class API {
  class Result(val remoteHostId: Long?, val res: CR) : API()
  class Error(val remoteHostId: Long?, val err: ChatError) : API()
}
```

<a id="CR"></a>

The [`CR` sealed class](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L6392) contains approximately 180 response variants. Key categories:

| Category | Examples |
|----------|---------|
| User | `ActiveUser`, `UsersList`, `UserPrivacy`, `UserProfileUpdated` |
| Chat state | `ChatStarted`, `ChatRunning`, `ChatStopped`, `ApiChats`, `ApiChat` |
| Tags | `ChatTags`, `TagsUpdated` |
| Contacts | `Invitation`, `SentConfirmation`, `SentInvitation`, `ContactConnected`, `ContactDeleted` |
| Messages | `NewChatItems`, `ChatItemUpdated`, `ChatItemsDeleted`, `ChatItemReaction`, `ForwardPlan` |
| Groups | `GroupCreated`, `SentGroupInvitation`, `UserAcceptedGroupSent`, `GroupUpdated`, `GroupMembers` |
| Files (receive) | `RcvFileAccepted`, `RcvFileStart`, `RcvFileComplete`, `RcvFileCancelled`, `RcvFileError` |
| Files (send) | `SndFileStart`, `SndFileComplete`, `SndFileCancelled`, `SndFileCompleteXFTP` |
| Calls | `CallInvitation`, `CallOffer`, `CallAnswer`, `CallExtraInfo`, `CallEnded` |
| Remote host | `RemoteHostList`, `RemoteHostStarted`, `RemoteHostConnected`, `RemoteHostStopped` |
| Remote ctrl | `RemoteCtrlList`, `RemoteCtrlFound`, `RemoteCtrlConnected`, `RemoteCtrlStopped` |
| Encryption | `ContactPQAllowed`, `ContactPQEnabled` |
| Misc | `CmdOk`, `ArchiveExported`, `ArchiveImported`, `AppSettingsR`, `VersionInfo` |
| Fallback | `Response` (unknown type + raw JSON), `Invalid` (unparseable) |

Each `CR` subclass is annotated with `@Serializable @SerialName("jsonTag")` for polymorphic JSON deserialization.

---

## 4. Event Types

The chat core pushes asynchronous events through the same `CR` type hierarchy. The [`startReceiver`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L695) coroutine continuously calls `chatRecvMsgWait` (blocking JNI), then dispatches each message to [`processReceivedMsg`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2708).

Events handled in `processReceivedMsg` include:

| Event | Description |
|-------|-------------|
| `ContactConnected` | A contact has completed the connection handshake |
| `ContactConnecting` | A contact connection is in progress |
| `ContactSndReady` | Contact's sending channel is ready |
| `ContactDeletedByContact` | A contact deleted their side of the conversation |
| `ReceivedContactRequest` | An incoming contact request arrived |
| `NewChatItems` | New messages received |
| `ChatItemUpdated` | A message was edited |
| `ChatItemsDeleted` | Messages were deleted |
| `ChatItemReaction` | A reaction was added/removed |
| `ChatItemsStatusesUpdated` | Delivery statuses updated |
| `GroupCreated` | A new group was created |
| `ReceivedGroupInvitation` | An invitation to join a group |
| `JoinedGroupMember` | A new member joined |
| `DeletedMember` / `DeletedMemberUser` | A member was removed |
| `LeftMember` | A member left voluntarily |
| `GroupUpdated` | Group profile changed |
| `GroupRelayUpdated` | Owner-side: a relay's `relayStatus` and/or the member's status changed. Fires on `XGrpRelayReject` with `relayStatus = RsRejected` and `GroupMember.memberStatus = MemLeft` — final on owner side until cleared by the relay operator's `/group allow #<channel>` (no event emitted to the owner for that clear). |
| `MemberRole` | A member's role changed |
| `MemberBlockedForAll` | A member was blocked for all |
| `RcvFileStart` / `RcvFileComplete` / `RcvFileError` | File receive progress |
| `SndFileStart` / `SndFileComplete` / `SndFileError` | File send progress |
| `CallInvitation` / `CallOffer` / `CallAnswer` / `CallEnded` | Call signaling events |
| `ContactPQEnabled` | Post-quantum encryption status changed |
| `RemoteHostStopped` / `RemoteCtrlStopped` | Remote access session ended |
| `SubscriptionStatusEvt` | Connection subscription status changed |

Each event triggers updates to `ChatModel` (reactive Compose state) and optionally fires platform notifications via `ntfManager`.

---

<a id="ChatError"></a>

## 5. Error Types

### [`ChatError`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L7290)

```kotlin
sealed class ChatError {
  class ChatErrorChat(val errorType: ChatErrorType)       // Application-level errors
  class ChatErrorAgent(val agentError: AgentErrorType)     // SMP/XFTP agent errors
  class ChatErrorStore(val storeError: StoreError)         // Database store errors
  class ChatErrorDatabase(val databaseError: DatabaseError)// Database engine errors
  class ChatErrorRemoteHost(val remoteHostError: ...)      // Remote host errors
  class ChatErrorRemoteCtrl(val remoteCtrlError: ...)      // Remote controller errors
  class ChatErrorInvalidJSON(val json: String)             // JSON parsing failure
}
```

### [`ChatErrorType`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L7320)

Common application error codes (~70 variants):

| Error | Meaning |
|-------|---------|
| `NoActiveUser` | No user profile is set as active |
| `UserExists` | Attempted to create a duplicate user |
| `InvalidDisplayName` | Display name contains invalid characters |
| `ChatNotStarted` / `ChatNotStopped` | Chat engine in wrong state |
| `InvalidConnReq` / `UnsupportedConnReq` | Bad or incompatible connection link |
| `ContactNotReady` / `ContactDisabled` | Contact in unusable state |
| `GroupUserRole` | Insufficient group permissions |
| `GroupNotJoined` | User has not joined the group |
| `FileNotFound` / `FileCancelled` / `FileAlreadyReceiving` | File transfer errors |
| `FileNotApproved` | File from unapproved relay server |
| `HasCurrentCall` / `NoCurrentCall` | Call state conflicts |
| `CommandError` / `InternalError` / `CEException` | Generic/internal errors |

### [`StoreError`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L7488)

Database-level errors: `DuplicateName`, `UserNotFound`, `GroupNotFound`, `ChatItemNotFound`, `LargeMsg`, `UserContactLinkNotFound`, etc.

### [`ArchiveError`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L7988)

```kotlin
sealed class ArchiveError {
  class ArchiveErrorImport(val importError: String)
  class ArchiveErrorFile(val file: String, val fileError: String)
}
```

---

## 6. Source Files

| File | Purpose | Path |
|------|---------|------|
| SimpleXAPI.kt | API bridge: all `api*` functions, `CC`, `CR`, `ChatError` | `common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt` |
| Core.kt | JNI externals, `initChatController`, `chatMigrateInit` | `common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt` |
| ChatModel.kt | Reactive UI state plus generation-scoped chat-list result application | `common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt` |
| DatabaseUtils.kt | `DBMigrationResult`, `MigrationError`, DB password helpers | `common/src/commonMain/kotlin/chat/simplex/common/views/helpers/DatabaseUtils.kt` |
| Files.kt | Platform-expect file path declarations | `common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt` |
| Files.android.kt | Android actual file paths | `common/src/androidMain/kotlin/chat/simplex/common/platform/Files.android.kt` |
| Files.desktop.kt | Desktop actual file paths | `common/src/desktopMain/kotlin/chat/simplex/common/platform/Files.desktop.kt` |
| Cryptor.kt | Platform-expect encryption interface | `common/src/commonMain/kotlin/chat/simplex/common/platform/Cryptor.kt` |
| Cryptor.android.kt | Android: AndroidKeyStore AES-GCM encryption | `common/src/androidMain/kotlin/chat/simplex/common/platform/Cryptor.android.kt` |
| Cryptor.desktop.kt | Desktop: placeholder (no-op) encryption | `common/src/desktopMain/kotlin/chat/simplex/common/platform/Cryptor.desktop.kt` |

All paths are relative to `apps/multiplatform/`.
