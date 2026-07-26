# Business Rules -- SimpleX Chat (Android & Desktop, Kotlin Multiplatform)

This document specifies invariants enforced by the Android and Desktop (Kotlin/Compose Multiplatform) clients.

---

## Table of Contents

1. [Security (RULE-01 through RULE-05)](#1-security)
2. [Message Integrity (RULE-06 through RULE-09)](#2-message-integrity)
3. [Group Integrity (RULE-10 through RULE-13)](#3-group-integrity)
4. [File Transfer (RULE-14 through RULE-15)](#4-file-transfer)
5. [Notification Delivery (RULE-16 through RULE-17)](#5-notification-delivery)
6. [Call Integrity (RULE-18)](#6-call-integrity)
7. [Nome Android Home Truth (RULE-19)](#7-nome-android-home-truth)
8. [Nome Android External Connection Truth (RULE-20)](#8-nome-android-external-connection-truth)
9. [Nome Android Database Root Truth (RULE-21)](#9-nome-android-database-root-truth)
10. [Nome Android Internal Invitation and Scan Truth (RULE-22)](#10-nome-android-internal-invitation-and-scan-truth)
11. [Nome Android Request and Public-Address Truth (RULE-23)](#11-nome-android-request-and-public-address-truth)
12. [Nome Android Group Invitation Truth (RULE-24)](#12-nome-android-group-invitation-truth)
13. [Nome macOS Official Routing Truth (RULE-29)](#13-nome-macos-official-routing-truth)

---

## 1. Security

### RULE-01: End-to-End Encryption is Mandatory

**Invariant:** Every message, file chunk, and call signaling payload MUST be encrypted end-to-end before transmission. The app MUST NOT transmit plaintext content to any relay server.

**Enforcement:** The Haskell core library handles all encryption. The Kotlin layer never constructs raw SMP messages. All communication flows through `ChatController.sendCmd()` which delegates to the FFI, ensuring the encryption layer cannot be bypassed.

**Location:** `common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt` -- `ChatController.sendCmd()`, `chatSendCmd()` FFI call

---

### RULE-02: Database Encryption at Rest

**Invariant:** The local SQLite database MUST be encrypted. A passphrase (either user-chosen or randomly generated) MUST be set before the database is operational.

**Enforcement:** On first launch, a random passphrase is generated and stored encrypted via the platform keystore (`CryptorInterface.encryptText`). The `initialRandomDBPassphrase` preference tracks whether the user has set a custom passphrase. Database encryption state is tracked in `ChatModel.chatDbEncrypted`. Encryption/re-encryption is performed via `CC.ApiStorageEncryption(config: DBEncryptionConfig)`.

**Caveat:** The user is not forced to set a custom passphrase -- the random passphrase is stored in app-accessible encrypted preferences. See GAP: "Database passphrase not enforced."

**Location:**
- `common/src/commonMain/kotlin/chat/simplex/common/views/helpers/DatabaseUtils.kt`
- `common/src/commonMain/kotlin/chat/simplex/common/platform/Cryptor.kt` -- `CryptorInterface`
- Android: `common/src/androidMain/kotlin/chat/simplex/common/platform/Cryptor.android.kt` -- Android Keystore
- Desktop: `common/src/desktopMain/kotlin/chat/simplex/common/platform/Cryptor.desktop.kt` -- **placeholder, not implemented**

---

### RULE-03: Local Authentication Gating

**Invariant:** When local authentication is enabled (`AppPreferences.performLA == true`), the app MUST require biometric/PIN authentication before displaying any chat content. The lock engages after `laLockDelay` seconds of inactivity.

**Enforcement:** `AppLock.setPerformLA` controls the lock state. The lock delay is configurable via `AppPreferences.laLockDelay` (default 30 seconds). Authentication mode is set via `AppPreferences.laMode` (system biometric or passcode).

**Location:**
- `common/src/commonMain/kotlin/chat/simplex/common/AppLock.kt`
- `SimpleXAPI.kt` -- `AppPreferences.performLA`, `AppPreferences.laMode`, `AppPreferences.laLockDelay`

---

### RULE-04: Self-Destruct Profile

**Invariant:** When self-destruct is enabled (`AppPreferences.selfDestruct == true`), entering the self-destruct passphrase instead of the real passphrase MUST wipe the database and present a clean profile with `selfDestructDisplayName`.

**Enforcement:** The self-destruct passphrase is stored separately (`encryptedSelfDestructPassphrase` / `initializationVectorSelfDestructPassphrase`). On Android, `SimplexService` checks for self-destruct on initialization. The comparison happens during the local authentication flow.

**Location:**
- `SimpleXAPI.kt` -- `AppPreferences.selfDestruct`, `AppPreferences.selfDestructDisplayName`
- `android/src/main/java/chat/simplex/app/SimplexService.kt` -- initialization check

---

### RULE-05: Screen Protection

**Invariant:** When `AppPreferences.privacyProtectScreen == true` (default), the app MUST prevent screenshots and screen recording. On Android this uses `FLAG_SECURE`; on Desktop this is advisory only.

**Enforcement:** The preference defaults to `true`. The Android activity applies `FLAG_SECURE` to its window based on this preference. The Desktop app cannot enforce this at the OS level.

**Location:** `SimpleXAPI.kt` -- `AppPreferences.privacyProtectScreen`

---

## 2. Message Integrity

### RULE-06: Message Ordering Verification

**Invariant:** The app MUST detect and surface message integrity violations (gaps, duplicates, out-of-order delivery) to the user.

**Enforcement:** The Haskell core tracks message sequence numbers per connection. When a gap or integrity error is detected, a `CIContent.RcvIntegrityError(msgError: MsgErrorType)` chat item is inserted into the conversation. The UI renders these as system messages indicating the integrity issue.

**Location:** `ChatModel.kt:3565` -- `CIContent.RcvIntegrityError`

---

### RULE-07: Decryption Error Surfacing

**Invariant:** When a message cannot be decrypted, the app MUST display a `RcvDecryptionError` item showing the error type and count of affected messages. The app MUST NOT silently drop undecryptable messages.

**Enforcement:** The Haskell core emits `CIContent.RcvDecryptionError(msgDecryptError, msgCount)` which the UI renders with an explanation and count. Ratchet re-synchronization can be triggered via `APISyncContactRatchet` / `APISyncGroupMemberRatchet`.

**Location:** `ChatModel.kt:3566` -- `CIContent.RcvDecryptionError`

---

### RULE-08: Delivery Receipt Consistency

**Invariant:** Delivery receipt settings MUST be consistent: when a user enables/disables receipts globally, the change MUST propagate to all contacts/groups (optionally clearing per-chat overrides via `clearOverrides`).

**Enforcement:** Global receipt toggle triggers `CC.SetAllContactReceipts(enable)`. Per-type settings use `CC.ApiSetUserContactReceipts` / `CC.ApiSetUserGroupReceipts` with `UserMsgReceiptSettings(enable, clearOverrides)`. The `privacyDeliveryReceiptsSet` preference gates the initial setup prompt shown during onboarding.

**Location:**
- `SimpleXAPI.kt` -- `CC.SetAllContactReceipts`, `CC.ApiSetUserContactReceipts`, `CC.ApiSetUserGroupReceipts`
- `SimpleXAPI.kt` -- `ChatController.startChat()` -- triggers `setDeliveryReceipts` prompt

---

### RULE-09: Chat Item TTL Enforcement

**Invariant:** When a chat item TTL (time-to-live) is set globally or per-chat, expired messages MUST be deleted by the core. The app MUST NOT display expired items.

**Enforcement:** Global TTL set via `CC.APISetChatItemTTL(userId, seconds)`. Per-chat TTL set via `CC.APISetChatTTL(userId, chatType, id, seconds)`. The Haskell core performs periodic cleanup. The current global TTL is stored in `ChatModel.chatItemTTL`.

**Location:** `SimpleXAPI.kt` -- `CC.APISetChatItemTTL`, `CC.APISetChatTTL`

---

## 3. Group Integrity

### RULE-10: Role-Based Access Control

**Invariant:** Group operations MUST respect the member's role. Only members with sufficient role level can perform privileged operations:
- **Owner:** can delete group, change any member's role, transfer ownership
- **Admin:** can add/remove members, change roles (up to Admin), create/delete group links
- **Moderator:** can delete other members' messages, block members
- **Member / Author / Observer:** cannot perform administrative actions

**Enforcement:** The Haskell core validates role permissions server-side. The Kotlin UI layer uses `GroupMemberRole` comparisons (the enum is ordered: Observer < Author < Member < Moderator < Admin < Owner) to show/hide action buttons.

**Location:** `ChatModel.kt:2369` -- `enum class GroupMemberRole`; various group management views

---

### RULE-11: Group Member Removal Atomicity

**Invariant:** When removing members from a group, the removal command MUST specify all member IDs atomically. Partial removal MUST NOT leave the group in an inconsistent state.

**Enforcement:** `CC.ApiRemoveMembers(groupId, memberIds: List<Long>, withMessages: Boolean)` sends all member IDs in a single command. The `withMessages` flag controls whether the removed members' messages are also deleted.

**Location:** `SimpleXAPI.kt` -- `CC.ApiRemoveMembers`

---

### RULE-12: Group Link Role Default

**Invariant:** When creating a group link, the default member role for joiners MUST be explicitly specified. The role can be updated after creation without regenerating the link.

**Enforcement:** `CC.APICreateGroupLink(groupId, memberRole)` requires a role. `CC.APIGroupLinkMemberRole(groupId, memberRole)` updates it. The link itself remains stable.

**Location:** `SimpleXAPI.kt` -- `CC.APICreateGroupLink`, `CC.APIGroupLinkMemberRole`

---

### RULE-13: Member Blocking Scope

**Invariant:** Blocking a member (`ApiBlockMembersForAll`) MUST apply the block for all group members (not just the requester). The `blocked` flag is visible to all members. Only roles >= Moderator can block.

**Enforcement:** `CC.ApiBlockMembersForAll(groupId, memberIds, blocked)` sends the block/unblock to the core, which propagates it to all group members.

**Location:** `SimpleXAPI.kt` -- `CC.ApiBlockMembersForAll`; `ChatModel.kt` -- `GroupMember.blockedByAdmin`

---

## 4. File Transfer

### RULE-14: File Encryption in Transit and at Rest

**Invariant:** Files sent via XFTP MUST be encrypted before upload. Files received MUST be decrypted only after download. When `privacyEncryptLocalFiles` is enabled (default `true`), files stored locally MUST be encrypted with per-file keys (`CryptoFile.cryptoArgs`).

**Enforcement:** The Haskell core handles XFTP encryption. Local file encryption is toggled via `CC.ApiSetEncryptLocalFiles(enable)`. The `CryptoFile` type carries optional `CryptoFileArgs` (key + nonce) for local decryption. Files are decrypted on-demand for display via `decryptCryptoFile()`.

**Location:**
- `SimpleXAPI.kt` -- `CC.ApiSetEncryptLocalFiles`, `AppPreferences.privacyEncryptLocalFiles`
- `ChatModel.kt` -- `CryptoFile`, `CryptoFileArgs`
- `RecAndPlay.desktop.kt` -- `decryptCryptoFile()` usage in audio playback

---

### RULE-15: Relay Approval for File Transfer

**Invariant:** When `privacyAskToApproveRelays` is enabled (default `true`), the app MUST prompt the user before using XFTP relay servers suggested by contacts (as opposed to the user's own configured servers). The `userApprovedRelays` flag on `CC.ReceiveFile` records the user's consent.

**Enforcement:** `CC.ReceiveFile(fileId, userApprovedRelays, encrypt, inline)` passes the approval flag. The UI prompts the user when the file is from an unapproved relay.

**Location:** `SimpleXAPI.kt` -- `CC.ReceiveFile`, `AppPreferences.privacyAskToApproveRelays`

---

## 5. Notification Delivery

### RULE-16: Background Message Delivery (Android)

**Invariant:** On Android, when `NotificationsMode.SERVICE` is selected (default), the app MUST maintain a foreground service (`SimplexService`) to ensure continuous message delivery. The service MUST survive app backgrounding and device sleep. When `NotificationsMode.PERIODIC` is selected, `MessagesFetcherWorker` MUST periodically wake and fetch messages. When `NotificationsMode.OFF`, no background delivery occurs.

**Enforcement:**
- `SimplexService` runs as a foreground service with `START_STICKY` and a `WakeLock`. It displays a persistent notification on the `SIMPLEX_SERVICE_NOTIFICATION` channel.
- `MessagesFetcherWorker` is a `PeriodicWorkRequest` scheduled via `WorkManager`.
- The mode is stored in `AppPreferences.notificationsMode` and checked at app startup.

**Location:**
- `android/src/main/java/chat/simplex/app/SimplexService.kt`
- `android/src/main/java/chat/simplex/app/MessagesFetcherWorker.kt`
- `SimpleXAPI.kt:7739` -- `enum class NotificationsMode`

---

### RULE-17: Notification Preview Privacy

**Invariant:** Notification content MUST respect `notificationPreviewMode`:
- `HIDDEN` -- notification shows no sender or message content
- `CONTACT` -- notification shows sender name only
- `MESSAGE` -- notification shows sender name and message preview

**Enforcement:** `NtfManager` (Android) reads the preview mode from `AppPreferences.notificationPreviewMode` and constructs notifications accordingly. The `CallService` also respects this mode for call notifications (showing or hiding caller identity).

**Location:**
- `android/src/main/java/chat/simplex/app/model/NtfManager.android.kt` -- `displayNotification()`, `notifyCallInvitation()`
- `android/src/main/java/chat/simplex/app/CallService.kt` -- `updateNotification()`
- `SimpleXAPI.kt` -- `AppPreferences.notificationPreviewMode`

---

## 6. Call Integrity

### RULE-18: Call Lifecycle Management

**Invariant:** An active call MUST be properly managed across the full lifecycle:
1. **Incoming calls** MUST be reported via `CallManager.reportNewIncomingCall()` which triggers a notification (and on Android, a full-screen intent for lock-screen display).
2. **Only one call** can be active at a time. Accepting a new call MUST end any existing call first (`CallManager.acceptIncomingCall` checks `activeCall` and calls `endCall` if needed, guarded by `switchingCall` flag).
3. **Call state** MUST progress through defined states: `WaitCapabilities` -> `InvitationSent`/`InvitationAccepted` -> `OfferSent`/`OfferReceived` -> `Negotiated` -> `Connected` -> `Ended`.
4. **Call end** MUST clean up all resources: send `WCallCommand.End`, call `apiEndCall`, clear `activeCall`, cancel call notifications, and release platform resources.

**Android enforcement:**
- `CallService` (foreground service) keeps the call alive in background with a `WakeLock` and ongoing notification on `CALL_SERVICE_NOTIFICATION` channel.
- `CallActivity` hosts the WebRTC WebView.
- Lock-screen behavior controlled by `AppPreferences.callOnLockScreen` (DISABLE / SHOW / ACCEPT).

**Desktop enforcement:**
- Calls run in the system browser via the NanoWSD WebSocket server on `localhost:50395`.
- The `WebRTCController` composable manages the WebSocket lifecycle.
- On dispose, `WCallCommand.End` is sent and the server is stopped.

**Location:**
- `common/src/commonMain/kotlin/chat/simplex/common/views/call/CallManager.kt`
- `common/src/commonMain/kotlin/chat/simplex/common/views/call/WebRTC.kt`
- Android: `android/src/main/java/chat/simplex/app/CallService.kt`, `android/src/main/java/chat/simplex/app/views/call/CallActivity.kt`
- Desktop: `common/src/desktopMain/kotlin/chat/simplex/common/views/call/CallView.desktop.kt`

---

## 7. Nome Android Home Truth

### RULE-19: Home-State Truth and Identity Isolation

**Invariant:** The Nome Android P07/P08 home MUST bind loaded chat rows to the exact `(remoteHostId, userId)` generation that produced them. A user/host change, an in-flight generation mismatch, or an initial unknown load MUST hide old rows. The UI MUST NOT infer “true empty” from `chats.isEmpty()` alone.

The following facts remain independent:

1. **Content:** true empty requires a typed successful `CR.ApiChats` result applied to the current generation with zero rows. `NoCurrentUser` is first use; command/parse failure is unavailable; filtered no result requires a non-empty same-generation base plus an active existing filter.
2. **Connectivity:** `null` before Android's first platform observation is unknown. Device offline requires an observed unvalidated/absent device network and MUST NOT be described as relay, server, global-service, or delivery failure.
3. **Core:** starting/running/stopped is derived separately from `chatRunning`. Core stopped does not erase a same-generation cached list; rows remain visible but actions requiring the core are read-only.

**Enforcement:** [`ChatListLoadGeneration`, `ChatListLoadState`, and `ChatListLoadResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106) encode provenance. [`beginChatListLoad` and `applyChatListLoadResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L264-L311) suppress stale attempts/generations and apply rows only when identity still matches. [`apiGetChatsResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081) discriminates success, failure, and no-current-user while retaining the existing controller/command path. Android exposes a nullable first-observation fact through [`NetworkObserver.platformNetworkInfo`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L16-L87). [`NomeHomeStateAdapter`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L49-L121) derives the three axes without collapsing them.

**Reachability boundary:** The unchanged root consumes no-current-user in onboarding before the
home route, so `FIRST_USE` remains a defensive renderer/test branch there. P09 now supplies a real
nonblank loaded-chat query. `FILTERED_NO_RESULT` is production-reachable only while that producer
is active over an available same-generation base with zero matches; it is not a retroactive P08
claim.

**Batch 2 boundary:** The frozen production slice projects name, timestamp, unread, and favorite
facts and permits navigation only into already-ready, non-deleting direct/group/local
conversations while the core is running. Visible and spoken message summaries remain gated by the
existing `showChatPreviews` privacy preference.

**Milestone 2 extension:** P09 may group only official loaded-chat filter rows and reuse the same
navigation guards. It MUST NOT persist recent queries or claim complete global-message results.
P10 may present only the existing invitation, scan/paste, create-group, and create-channel
callbacks plus actual current-profile display data. Neither page may manufacture connection,
network, delivery, identity, or success facts.

---

## 8. Nome Android External Connection Truth

### RULE-20: External Preview Identity and Command Truth

**Invariant:** Nome P13 may replace the identity-choice presentation only for Android external
`ACTION_VIEW` links and only for the seven explicitly eligible `ConnectionPlan` branches. All
other callers and plan variants MUST retain the established legacy path.

The preview MUST:

1. bind the plan to the active `(remoteHostId, userId)` and refuse to send after that context
   changes;
2. retain the bearer URI and owner signature only in an ephemeral controller closure, never UI
   state, saved state, semantics, evidence, or P13 command logs;
3. treat incognito as a new profile for this connection only;
4. permit exactly one connect submission per preview session;
5. enter pending only from a typed `SentConfirmation` or `SentInvitation` response carrying a real
   `PendingContactConnection`;
6. retain already-existing, failure, no-user, context-changed, and cancellation as distinct
   non-success outcomes;
7. re-plan against the current user/host before retrying; and
8. make cancel, back, dismiss, and handoff cleanup idempotent and command-free.

**Enforcement:** [`connectIfOpenedViaUri`](../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt#L760-L791) is the only opt-in. [`planAndConnect`](../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/ConnectPlan.kt#L25-L607) retains a `Legacy` default and guards context/single-submit/cleanup. [`apiConnectPlanResult` and `apiConnectResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1571-L1647) preserve typed core truth with P13 command logging disabled. [`ConnectionPreviewPlanBranch`](../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.kt#L91-L187) exhaustively defines the seven eligible and fourteen fallback branches.

**Protected boundary:** P13 does not own the P10 entry or its internal actions, does not implement
scanner/paste behavior, and does not change P14–P24. P09/P10 do not broaden P13's external
`ACTION_VIEW` opt-in.

---

## 9. Nome Android Database Root Truth

### RULE-21: P01 Database State, Secret, and Recovery Truth

**Invariant:** Nome P01 MUST preserve the official root priority, delayed opening selection,
authentication ownership, and `DBMigrationResult` semantics. `UnknownFailure` may come only from
the exact native `Unknown` subtype. A local copy or persistence failure MUST NOT be relabelled as a
native database result.

The Android route MUST:

1. keep passphrase text in non-saveable composition state and clear it after submit, on terminal
   or root change, background stop, rotation/disposal, and failed persistence;
2. expose only a no-secret missing-alias/decrypt-failure class for the database Keystore alias;
3. let an initial-random-key failure override opening/alternate-key presentation while offering no
   manual-key claim;
4. keep “Open once” non-persistent and “Save and open” separately explicit with the existing write
   order;
5. single-submit every open/confirm/copy action and reject stale attempt completion;
6. offer backup copy only for the exact timestamp-matched chat/agent pair; copy success is bound to
   its attempt and source result and requires a separate fresh open;
7. call the existing Android post-open hook exactly once for an accepted fresh `OK`; and
8. omit raw path, migration name, SQL, JSON, stack, key text, percentage, stages, timeout, cancel,
   rollback, identity-loaded, and messaging-restored claims.

**Desktop boundary:** The shared expect/actual seam invokes the existing Desktop content exactly
once and installs no Nome database behavior.

**Location:**

- `common/src/commonMain/kotlin/chat/simplex/common/App.kt`
- `common/src/commonMain/kotlin/chat/simplex/common/views/database/{DatabaseErrorView,PlatformDatabaseRootRoute}.kt`
- `common/src/androidMain/kotlin/chat/simplex/common/{platform/Cryptor.android.kt,views/database/PlatformDatabaseRootRoute.android.kt,ui/nome/database/**}`
- `common/src/desktopMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.desktop.kt`

---

## 10. Nome Android Internal Invitation and Scan Truth

### RULE-22: P11/P12 Producer, Secret, and Permission Truth

**Invariant:** Nome P11/P12 may replace only Android presentation around the official
`NewChatView` owners. Invitation creation remains `apiAddContact()`. Manual, clipboard, and
camera input remain the existing parser plus `planAndConnect(..., Legacy)`. Internal P12 input
MUST NOT opt in to the external-`ACTION_VIEW`-only P13 route.

The Android route MUST:

1. render a link or QR code only from a real `CreatedConnLink`;
2. keep generated, copied, shared, peer-used, pending, connected, and failed as distinct facts;
3. omit TTL, expiry, atomic replacement, invalidation, network health, and success claims without
   an authoritative result/event;
4. retain invitation payloads only in the existing ephemeral route/action closures and exclude
   them from logs, saved state, semantics, filenames, and retained screenshots;
5. read the clipboard only after an explicit user action;
6. request camera permission only after an explicit scan action, offer Settings only after
   permanent denial, and expose camera-unavailable as a platform fact;
7. close every analyzer frame and dispose its executor on failure or route disposal; and
8. preserve an unchanged Desktop legacy delegate.

**Enforcement:** `PlatformNewChatRoute` owns presentation selection only. `NewChatView` retains
creation, disposal, parsing, planning, and navigation. `QRCodeScanner.android.kt` owns the Android
camera/permission lifecycle. Focused Compose tests assert one-to-one local callbacks, explicit
camera/clipboard activation, 48dp actions, and absence of unsupported expiry/regeneration copy.

---

## 11. Nome Android Request and Public-Address Truth

### RULE-23: P14/P15 Typed Result and Destructive-Phase Truth

**Invariant:** Nome P14/P15 may replace only Android presentation around the official
contact-request and user-address owners. A request row may be removed only after typed reject
success. Address OFF, READY, lookup failure, deletion, and replacement creation are distinct
states.

The Android routes MUST:

1. mutate the request row after accept only when the official API returns a real `Contact`;
2. remove a rejected request only from `APIRejectContactRequestResult.Rejected`, including its
   valid null-contact form, and retain it on `Failure`;
3. omit the baseline request message because v6.5.6 has no request-message producer;
4. derive address OFF only from exact `APIUserAddressResult.NotFound`, never from a bare null or
   generic failure;
5. preserve a confirmed cached address when a refresh fails;
6. retain official short-link upgrade, copy/share, post-create profile-sharing, advanced
   settings, and onboarding behavior;
7. confirm delete and replacement, model replacement as delete then create, and after confirmed
   deletion retry only create without claiming atomicity or rollback;
8. clear the address model only after a real delete result, single-submit actions, and block back
   while an operation is in flight; and
9. preserve unchanged Desktop request/address presentation.

**Enforcement:** `PlatformContactRequestRoute` and `PlatformUserAddressRoute` select presentation
only. `ChatListNavLinkView` and `UserAddressView` retain model mutation and official action
ownership. `apiRejectContactRequestResult` and `apiGetUserAddressResult` project existing core
responses without adding commands or response types. Focused Compose tests cover failure
retention, destructive confirmation/retry, bearer-safe semantics, 48dp targets, and busy-back
behavior.

---

## 12. Nome Android Group Invitation Truth

### RULE-24: P16 Typed Group, Inviter, and Join Truth

**Invariant:** Nome P16 may replace only Android presentation from the existing
`GroupMemberStatus.MemInvited` owner. Group type, profile, membership, inviter, join result, and
deletion result MUST remain the official typed facts and commands.

The Android route MUST:

1. derive public/private and channel presentation from `GroupProfile.publicGroup` and
   `GroupInfo.isChannel`, never from relay use or design copy;
2. label an inviter as a verified contact only when `InvitedBy.IBContact` resolves to an existing
   direct `Contact` whose official verification fact is true;
3. never relabel that contact as a verified administrator or reconstruct an unavailable source;
4. treat `APIJoinGroupResult.Accepted` as the only accepted join result, expired/not-found as
   terminal unavailable, and every other response as not completed and retryable;
5. avoid joined, connected, online, relay-health, review-approved, or success claims until their
   official result/event/model state exists;
6. retain the invitation on a generic join/delete failure, single-submit actions, and block back
   while an operation is in flight;
7. require explicit confirmation before the existing local invitation deletion and keep ordinary
   back command-free; and
8. preserve unchanged Desktop group-invitation presentation.

**Enforcement:** `PlatformGroupInvitationRoute` selects Android presentation only.
`ChatListNavLinkView.acceptGroupInvitationAlertDialog` retains the invited-group owner and model
mutation. `apiJoinGroupResult` projects the existing join response without adding a command or
response type. Focused Compose tests cover success/failure callbacks, single-submit, busy-back,
destructive confirmation, failure retention, and 48dp actions.

### RULE-25: P17/P18 Conversation Presentation and Action Eligibility

**Invariant:** Nome may replace the Android conversation composition and message-action
presentation, but the loaded chat, chat items, composer state, attachment/voice actions, delivery
facts, and every action callback and eligibility gate MUST remain official v6.5.6 owners.

The Android presentation MUST:

1. show a fixed direct-chat encryption banner only when an actual
   `SndDirectE2EEInfo` or `RcvDirectE2EEInfo` item supplies the fact, and remain fail-closed when no
   such item exists;
2. retain the official message list and composer callbacks while aligning the toolbar, banner,
   message surfaces, spacing, attachment affordance, and input shape with the P17 visual baseline;
3. open P18 from a real chat-item long press and invoke every official action callback at most
   once;
4. preserve the existing reaction, reply, share, copy, edit, forward, info, delete, and selection
   eligibility rules;
5. expose report only under the existing exact group-message Reports/member-role gate, never in a
   direct conversation merely because the P18 baseline depicts it;
6. keep extra official actions available in a scrollable sheet instead of deleting behavior to
   mimic a shorter reference;
7. keep Desktop on the established anchored action menu; and
8. leave file transfer, voice recording/playback, and call lifecycle on their separate high-risk
   real-fixture validation tier.

**Enforcement:** `ChatView` owns the loaded timeline, composer, and real E2EE item projection.
`ChatItemView` owns action eligibility and callbacks. `PlatformMessageActionsMenu` changes only
the Android container; its Desktop actual delegates to the official menu. Focused common and
Android Compose tests cover fail-closed encryption facts, one-to-one action callbacks, dismiss
behavior, and 48dp action rows.

### RULE-26: P19 Security Verification and P20 Channel Truth

**Invariant:** Nome may replace the Android P19/P20 presentation, but contact verification and
public-channel creation MUST remain owned by the official v6.5.6 codes, commands, results, models,
relay configuration, link progression, and destructive outcomes.

The Android presentation MUST:

1. render and share the complete real contact security code without adding, dropping, hashing, or
   substituting visible digits;
2. keep scanner match, scanner mismatch, scanner/API unavailable, manual attestation, verified,
   and cleared outcomes distinct;
3. apply verification state only from the contact returned by the existing verify command and
   refresh stale code/state from the existing get-code result;
4. keep group-member verification and Desktop on their official presentation routes;
5. create a channel only through the existing enabled-relay selection and public-group command,
   then use its returned `GroupInfo`, `GroupLink`, and relay results;
6. show no custom Nome slug/domain, generic relay availability/health, connection, encryption, or
   success fact before an authoritative producer returns it;
7. route join to the existing scan/paste flow and relay configuration to the existing Settings
   owner;
8. finalize local cancellation/removal only after the existing delete command returns true; false
   or exception MUST retain local state; and
9. keep submit/back disabled only while the official creation or cancellation action is actually
   in flight; and
10. allow controlled `androidTest` channel mutation only when the caller supplies a canonical
    per-run UUID and a test-written fixture record matches that UUID exactly together with the
    active user, remote host, and group id. Tests MUST NOT fall back to display-name matching,
    accept a missing/malformed/mismatched nonce, or expose arbitrary channel names through
    read-only status output; status may expose only counts and controlled-record/channel
    booleans; and
11. fail API-35-only screenshot evidence tests on every other API by default. A lower-API full
    regression may skip those capture bodies only through the explicit
    `nomeCrossApiScreenshotSkip=true` instrumentation argument; and
12. fail argument-driven controlled producer, bridge, network, archive, call, file, media, group,
    public-channel, and remote lifecycle tests when their primary action/role is absent or invalid.
    A general device regression may bypass those separately verified harness bodies only through
    the explicit `nomeControlledProducerSkip=true` instrumentation argument. Such a regression
    proves the ordinary UI/device suite and the fail-closed bypass boundary; it MUST NOT be cited
    as execution evidence for the controlled producer lifecycle itself; and
13. fail the guarded P14 production capture on API 35 when its exact fixed-fixture token is absent
    or invalid. Only the explicit `nomeControlledProducerSkip=true` general-regression invocation
    may bypass that capture body, and that bypass MUST NOT be cited as capture evidence.

**Enforcement:** `VerifyCodeView` retains code/scanner/API/model ownership while
`PlatformVerifyCodeLayout` changes only Android presentation. `AddChannelView` retains relay,
create/progress/link/delete ownership while `PlatformChannelSetupRoute` changes only its Android
profile/setup step. Focused common and Compose tests cover typed result separation, digit
preservation, distinct scan/manual callbacks, 48dp actions, link/relay truth, and
delete-confirmed-only local finalization. The controlled public-channel producer and two-client
tests persist and resolve only the exact test-owned fixture record described above.

### RULE-27: P21 Channel Facts and P22 Identity Lifecycle

**Invariant:** Nome may replace the Android P21/P22 presentation, but public-channel disclosure,
member role, local identities, current-user selection, hidden-profile authentication, incognito
default, network configuration, and destructive lifecycle MUST remain owned by official v6.5.6
models, preferences, routes, commands, and returned outcomes.

The Android presentation MUST:

1. show the fixed public-channel non-E2EE disclosure only for a real base channel with the
   official public-channel relay fact, never for an ordinary group or direct chat;
2. show observer/read-only treatment only for the real current-member observer role and retain the
   official timeline, profile, history, relay, member, and moderation behavior;
3. render identities only from the official users list and delegate add, edit, activate,
   hide/unhide, mute/unmute, and delete exactly once to existing callbacks;
4. treat incognito as the actual per-connection default preference, never as a persistent
   anonymous account;
5. display SOCKS only as the actual stored configuration and route changes through official
   Network settings/confirmation, without inferring private routing, Tor, health, or connectivity;
6. preserve switch-before-delete when another visible identity exists and
   delete/clear/Android-stop ordering when it does not;
7. perform local wallpaper/profile/notification cleanup only after confirmed target deletion;
8. keep switch/delete failure retryable with the target identity intact, while a post-delete
   clear/stop failure reports restart reconciliation without claiming rollback; and
9. rethrow coroutine cancellation rather than converting it into an identity failure.

**Enforcement:** `ChatView`/`ComposeView` retain channel and member truth while
`PlatformChannelConversationChrome` changes only Android chrome. `UserProfilesView` retains
profile/preference/route/controller ownership while `PlatformIdentityCenterRoute` changes only
Android composition. `UserDeletionLifecycle` types the existing non-atomic stages without adding a
command or transaction. Focused common and Android tests cover channel chrome, one-to-one action
dispatch, 48dp controls, and deletion ordering/failure truth; destructive lifecycle is repeated
with disposable production identities on API 28 and API 35.

### RULE-28: P23 Settings Ownership and P24 Archive/Migration Truth

**Invariant:** Nome may replace the Android P23/P24 composition, but settings routes, database
keys, archive export/import, destination copy, device migration, cleanup, and restart MUST remain
owned by official v6.5.6 models, preferences, platform contracts, commands, and returned state.

The Android presentation MUST:

1. index and dispatch only existing settings routes, without inventing health, permission,
   connectivity, version, or success facts;
2. keep Desktop on its established settings/database presentation;
3. show P24 not-started until a real archive or migration owner produces a later stage;
4. omit synthetic percentage, completion, generic cancel, resume, restore, and rollback claims;
5. restart chat after an export snapshot completes or its destination chooser is cancelled, while
   leaving copy and snapshot deletion solely to the chooser result;
6. register created database archives as `application/zip` on Android without changing bytes,
   naming, archive format, import order, database semantics, or Desktop behavior;
7. preserve the official destructive-import confirmation, key re-entry, replacement, and restart
   lifecycle; and
8. treat a migration upload stage with zero uploaded bytes as neither connectivity nor success,
   and use only the existing Back cleanup/restart path; and
9. disable outbound migration while chat is stopped, matching the official settings owner, while
   keeping archive/database recovery reachable so its established start control is not stranded.

**Enforcement:** `SettingsView` retains route ownership while `PlatformSettingsHomeRoute` and
`PlatformBackupMigrationRoute` change only Android presentation. `DatabaseView` retains archive
and key operations; `MigrateFromDeviceView` retains outbound transfer ownership. Focused tests
cover existing-route dispatch, 48dp controls, stopped-chat migration gating, and forbidden
synthetic copy. Disposable API 28/API 35 fixtures cover export cancellation/save, import
round-trip/key re-entry, platform MIME selectability, migration abort, cleanup, and cold-start
data preservation.

## 13. Nome macOS Official Routing Truth

### RULE-29: Official Defaults Must Not Reintroduce Upstream Routing or Terms

**Invariant:** A fresh Nome macOS profile and the bundled Nome terminal configuration MUST use
only the Nome official SMP/XFTP trust anchors. Loading an upgraded profile MUST remove legacy
SimpleX/Flux preset operator, SMP, XFTP, relay, and operator-condition rows from active
configuration while preserving user-added servers, all real user contacts, and group-relay
referential integrity. The two exact upstream seed cards MAY be removed only when they are
disconnected and have no messages, group membership, or contact request. Nome MUST NOT display or
require the embedded upstream operator conditions. Server administrator credentials, TLS private
keys, and APNs keys MUST NOT be compiled into the client. Any shared client access credential
embedded for zero-configuration routing MUST be treated as publicly distributed and MUST NOT be
the server's only abuse-control boundary.

**Enforcement:** The Haskell preset configuration owns the exact Nome endpoints and certificate
fingerprints. `getUpdateServerOperators` performs the bounded preset cleanup before reconciling
operators; a preset chat relay referenced by a group is disabled and soft-deleted instead of
cascading through `group_relays`. The seed-card cleanup uses exact profile/link fingerprints plus
negative connection/history/group/request guards. `getOperatorConditions_` represents Nome as not
gated by upstream conditions. `terminalChatConfig` reuses the same Nome presets. Unit and
integration tests cover default selection, upgrade cleanup, custom-server and real-contact
preservation, conditions filtering, and terminal/core regression. Server diagnostics summarize
the notification servers from the active `ChatConfig`; they do not inject a separate upstream
display list. Desktop settings and chat-list startup suppress the upstream release/support/update
surfaces until Nome owns equivalent destinations. The Nome macOS onboarding commitment completes
through `setServerOperators` and never calls the upstream `acceptConditions` path.

**Operational boundary:** A reachable TCP/TLS endpoint is not proof that the service can create a
queue or upload a file. If the official SMP/XFTP service requires a creation password that is not
securely provisioned to the client, the release must report that blocker and MUST NOT claim
end-to-end service readiness.
