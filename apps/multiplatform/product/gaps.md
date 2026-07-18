# Known Gaps & Recommendations -- SimpleX Chat (Android & Desktop, Kotlin Multiplatform)

This document catalogs open gaps in the multiplatform codebase (Android and Desktop) with severity, impact, and recommendations. Numbering remains stable for cross-references; GAP-06 is retained as an explicitly closed historical audit entry after verification against v6.5.6.

---

## Table of Contents

1. [UI: Error Feedback](#gap-01-ui-error-feedback)
2. [UI: Loading States](#gap-02-ui-loading-states)
3. [Security: Database Passphrase Not Enforced](#gap-03-security-database-passphrase-not-enforced)
4. [Security: No Forward Secrecy Indicator](#gap-04-security-no-forward-secrecy-indicator)
5. [Documentation: Haskell Store Layer Not Fully Specified](#gap-05-documentation-haskell-store-layer-not-fully-specified)
6. [Resolved in v6.5.6: Desktop Recording Implemented](#gap-06-resolved-in-v656-desktop-recording-implemented)
7. [Desktop: Cryptor Not Implemented](#gap-07-desktop-cryptor-not-implemented)
8. [Nome Android: Startup and Empty-State Truth](#gap-08-nome-android-startup-and-empty-state-truth)
9. [Nome Android: Local Authentication Fail-Safe and Throttling](#gap-09-nome-android-local-authentication-fail-safe-and-throttling)
10. [Nome Android: Async Operation and Permission States](#gap-10-nome-android-async-operation-and-permission-states)
11. [Nome Android: Global and Settings Search Model](#gap-11-nome-android-global-and-settings-search-model)
12. [Nome Android: One-Time Invitation Lifecycle](#gap-12-nome-android-one-time-invitation-lifecycle)
13. [Nome Android: Request and Public-Address Lifecycle](#gap-13-nome-android-request-and-public-address-lifecycle)
14. [Nome Android: Group, Report, and Channel Claims](#gap-14-nome-android-group-report-and-channel-claims)
15. [Nome Android: Anonymous Identity Semantics](#gap-15-nome-android-anonymous-identity-semantics)
16. [Nome Android: Archive and Migration Recovery Contract](#gap-16-nome-android-archive-and-migration-recovery-contract)
17. [Nome Android: Network Evidence Levels](#gap-17-nome-android-network-evidence-levels)
18. [Android API 26: Official v6.5.6 Native Artifact Cannot Load](#gap-18-android-api-26-official-v656-native-artifact-cannot-load)

---

## GAP-01: UI Error Feedback

**Severity:** Medium
**Category:** UI / UX
**Platforms:** Android, Desktop

### Description

Many API calls through `ChatController.sendCmd()` return `API.Error` responses that are logged but not surfaced to the user. The general pattern is:

```kotlin
val r = sendCmd(rh, cmd)
if (r is API.Result && r.res is CR.ExpectedResponse) return r.res.value
Log.e(TAG, "someFunction bad response: ${r.responseType} ${r.details}")
return null
```

When the call fails, the caller receives `null` and either silently does nothing or shows a generic error. The specific `ChatError` details (which may contain actionable information like quota exceeded, server unreachable, or store errors) are lost to the user.

### Affected Locations

- `SimpleXAPI.kt` -- `getAgentSubsTotal()`, `getAgentServersSummary()`, and dozens of similar `api*` functions
- Throughout the codebase wherever `sendCmd` results are pattern-matched

### Impact

Users experience silent failures with no indication of what went wrong. This is particularly problematic for:
- Connection attempts that fail due to network issues
- File transfer failures
- Group operations that fail due to role permissions
- Server configuration errors

### Recommendation

1. Introduce a structured error-handling utility that maps `ChatError` subtypes to user-visible messages, similar to how `retryableNetworkErrorAlert` already handles a subset of `AgentErrorType.BROKER` errors.
2. At minimum, surface a dismissible snackbar/toast with a summary when an API call fails unexpectedly.
3. For critical operations (send message, join group, create connection), show a dialog with retry/cancel options (the `sendCmdWithRetry` pattern already exists for some cases -- extend it).

---

## GAP-02: UI Loading States

**Severity:** Low-Medium
**Category:** UI / UX
**Platforms:** Android, Desktop

### Description

Several long-running operations lack loading indicators, leaving the user uncertain whether the action is in progress. The `ComposeState.inProgress` flag and `progressByTimeout` mechanism exist for the compose area, and `ConnectProgressManager` handles connection progress, but many other flows have no visual feedback.

### Affected Locations

- Group member list loading (`ChatModel.membersLoaded` exists but is not always checked before displaying stale data)
- Server configuration validation (`ApiValidateServers` can take several seconds with no indicator)
- Database export/import (`ApiExportArchive`, `ApiImportArchive`)
- Profile switching (`changeActiveUser_` acquires `changingActiveUserMutex` but the UI may appear frozen)

### Impact

Users may tap actions multiple times, causing duplicate requests, or assume the app is frozen and force-quit during a long operation like database export.

### Recommendation

1. Introduce a centralized `ProgressOverlay` composable that can be shown/hidden via a `ChatModel` flag.
2. Wrap all operations that acquire `changingActiveUserMutex` or take > 1 second with a visible loading state.
3. Use `ChatModel.switchingUsersAndHosts` (which already exists) more consistently as a gate for showing a blocking progress indicator.

### Nome P07/P08 bounded mitigation

Batch 2 mitigates and verifies the stale-home subset without closing this general gap. [`beginChatListLoad` and `applyChatListLoadResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L264-L311) bind chat-list loads to an attempt ID and `(remoteHostId, userId)`, rejecting a result after a newer attempt or active-identity change. [`changeActiveUser_` and `getUserChatData`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L643-L678), plus [`switchUIRemoteHost`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L3527-L3555), begin row-hiding load state before replacement data is applied, while the Android home adapter also hides rows on switching or generation mismatch. The profile-switch control itself and the broader operations listed above remain outside Batch 2.

---

## GAP-03: Security: Database Passphrase Not Enforced

**Severity:** High
**Category:** Security
**Platforms:** Android, Desktop

### Description

When the app is first installed, a random database passphrase is generated and stored in encrypted preferences. The user is never required to set a custom passphrase. The `initialRandomDBPassphrase` flag tracks this state, and a setup prompt exists in onboarding (`SetupDatabasePassphrase`), but the user can skip it.

On Android, the encrypted passphrase key is stored via the OS `AndroidKeyStore`. The v6.5.6 implementation does not request StrongBox or inspect `KeyInfo.isInsideSecureHardware`/`securityLevel`, so hardware backing depends on the device and generated key and must not be claimed unconditionally. On Desktop, the `Cryptor` is a **placeholder** (see GAP-07), meaning the passphrase is stored in plaintext.

### Affected Locations

- `SimpleXAPI.kt` -- `AppPreferences.storeDBPassphrase`, `AppPreferences.initialRandomDBPassphrase`, `AppPreferences.encryptedDBPassphrase`
- `common/src/commonMain/kotlin/chat/simplex/common/views/helpers/DatabaseUtils.kt`
- `common/src/commonMain/kotlin/chat/simplex/common/views/onboarding/SetupDatabasePassphrase.kt`

### Impact

- Users who skip passphrase setup rely entirely on device security. If the device is compromised, the database can be decrypted using the stored passphrase.
- On Desktop, the passphrase is effectively stored in plaintext (see GAP-07), meaning anyone with filesystem access can read the database.

### Recommendation

1. Consider making passphrase setup mandatory during onboarding (or at least prominently warn users who skip it).
2. On Desktop, implement proper key storage (GAP-07) before any passphrase enforcement is meaningful.
3. Add a periodic reminder for users who still have `initialRandomDBPassphrase == true`.

---

## GAP-04: Security: No Forward Secrecy Indicator

**Severity:** Medium
**Category:** Security / UI
**Platforms:** Android, Desktop

### Description

The double-ratchet algorithm provides forward secrecy per message, and PQ key exchange provides resistance to quantum attacks. The `Connection` type tracks `pqSupport`, `pqEncryption`, `pqSndEnabled`, and `pqRcvEnabled`. However, the UI does not prominently display the current forward secrecy state or PQ encryption status for a given conversation.

### Affected Locations

- `ChatModel.kt` -- `Connection.pqSupport`, `Connection.pqEncryption`, `Connection.pqSndEnabled`, `Connection.pqRcvEnabled`
- Contact info views, group member info views

### Impact

Users cannot easily verify whether their conversations are using PQ-enhanced encryption. Security-conscious users have no visual indicator of the ratchet state or whether PQ key exchange was successful.

### Recommendation

1. Add a security badge/icon in the chat header or contact info screen showing:
   - Whether PQ key exchange is active (both peers support it)
   - Whether the connection has been verified (security code comparison)
   - The ratchet state (in-sync vs. needs re-sync)
2. The `connectionCode` field on `Connection` can be used to show verification status.
3. The `Call.encryptionStatus` pattern (used in call views) could be adapted for the chat view.

---

## GAP-05: Documentation: Haskell Store Layer Not Fully Specified

**Severity:** Medium
**Category:** Documentation / Architecture
**Platforms:** Android, Desktop

### Description

The Kotlin client communicates with the Haskell core via a text-based command protocol (`CC.cmdString` -> FFI -> Haskell). The Haskell store layer (SQLite operations, migration logic, and the exact semantics of `StoreError` variants) is not documented from the Kotlin side. The `ChatErrorStore` error type wraps a `StoreError` whose variants are defined in Haskell and deserialized by the Kotlin client, but the conditions under which each error occurs are not specified.

### Affected Locations

- `SimpleXAPI.kt:7256` -- `ChatErrorStore(storeError: StoreError)`
- `SimpleXAPI.kt` -- `StoreError` sealed class (deserialized from Haskell responses)
- `SimpleXAPI.kt` -- `ChatErrorDatabase(databaseError: DatabaseError)` for migration errors

### Impact

- Developers cannot predict which `StoreError` will occur for a given operation without reading the Haskell source.
- Error handling in the Kotlin layer is necessarily generic since the error semantics are not specified.
- Migration failures (`ChatErrorDatabase`) are particularly opaque.

### Recommendation

1. Create a specification document mapping each `CC` command to its possible `StoreError` / `DatabaseError` responses.
2. Document the database migration versioning scheme and the conditions under which `confirmDBUpgrades` is triggered.
3. Add inline documentation to the `StoreError` sealed class variants explaining their trigger conditions.

---

## GAP-06: Resolved in v6.5.6: Desktop Recording Implemented

**Status:** Closed in the audited v6.5.6 baseline
**Severity:** None (historical entry)
**Category:** Feature / Platform
**Platform:** Desktop only

### Resolution Evidence

The earlier placeholder assessment is no longer true. `RecorderNative` is implemented in `common/src/desktopMain/kotlin/chat/simplex/common/platform/RecAndPlay.desktop.kt:20-85`:

- `start()` creates a temporary voice file, selects `qtsound://`, `pulse://`, or `dshow://` for macOS, Linux, or Windows, and starts VLC capture with progress updates (`:26-67`);
- `stop()` stops and releases the player, joins the progress job, clears state, and returns the measured duration (`:70-84`);
- the same file contains the VLC-backed desktop audio player beginning at line 87.

This source audit closes the “recording is a stub” claim. It is not a cross-platform microphone hardware test; any later runtime/device defect must be filed as a new gap with native evidence rather than reopening this stale source claim.

### Remaining Unrelated Desktop Placeholders

These v6.5.6 placeholders remain separate from the closed recording item:

- **QR Code Scanner** (`common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/QRCodeScanner.desktop.kt:12`) -- desktop QR scanning is not implemented;
- **Animated Drawables** (`common/src/desktopMain/kotlin/chat/simplex/common/views/helpers/Utils.desktop.kt:188`) -- animated drawable support is incomplete;
- **Animated Chat Images** (`common/src/desktopMain/kotlin/chat/simplex/common/views/chat/item/CIImageView.desktop.kt:20`) -- chat-item animation is incomplete;
- **isImage detection** (`common/src/desktopMain/kotlin/chat/simplex/common/platform/Images.desktop.kt:189-190`) -- extension-based detection is implemented but still marked incomplete.

---

## GAP-07: Desktop: Cryptor Not Implemented

**Severity:** Critical
**Category:** Security / Platform
**Platform:** Desktop only

### Description

The `CryptorInterface` implementation on Desktop is a non-functional placeholder. All three methods are stubbed:

```kotlin
// common/src/desktopMain/kotlin/chat/simplex/common/platform/Cryptor.desktop.kt
actual val cryptor: CryptorInterface = object : CryptorInterface {
  override fun decryptData(data: ByteArray, iv: ByteArray, alias: String): String? {
    return String(data) // LALAL
  }

  override fun encryptText(text: String, alias: String): Pair<ByteArray, ByteArray> {
    return text.toByteArray() to text.toByteArray() // LALAL
  }

  override fun deleteKey(alias: String) {
    // LALAL
  }
}
```

- `decryptData` returns the data as-is (no decryption)
- `encryptText` returns the plaintext as both "encrypted data" and "IV"
- `deleteKey` is a no-op

### Affected Locations

- `common/src/desktopMain/kotlin/chat/simplex/common/platform/Cryptor.desktop.kt`
- `common/src/commonMain/kotlin/chat/simplex/common/platform/Cryptor.kt` -- `CryptorInterface`
- `common/src/commonMain/kotlin/chat/simplex/common/views/helpers/DatabaseUtils.kt` -- uses `cryptor` for passphrase encryption

### Impact

**This is a critical security gap.** On Desktop:
- The database passphrase is stored **in plaintext** in the preferences file. Anyone with read access to the user's home directory can extract the passphrase and decrypt the database.
- The self-destruct passphrase is similarly stored in plaintext.
- The app passphrase (for local authentication) provides no real protection.
- Key deletion is a no-op, so "deleting" a key has no effect.

This directly undermines RULE-02 (Database Encryption at Rest) and RULE-04 (Self-Destruct Profile) on the Desktop platform.

### Recommendation

1. **Priority: Critical.** Implement proper key storage on Desktop using one of:
   - **OS Keychain integration:** macOS Keychain, Windows Credential Manager, Linux Secret Service (via `libsecret`/GNOME Keyring/KWallet)
   - **Java Cryptography Architecture (JCA)** with a PKCS#12 keystore file protected by a master password
   - **Bouncy Castle** library for platform-independent key management
2. Until a real implementation exists, display a prominent warning to Desktop users that their database passphrase is not securely stored.
3. Consider requiring the user to enter their passphrase on each app launch (do not store it) as an interim measure.

### Related

- GAP-03 (Database Passphrase Not Enforced) is compounded by this gap on Desktop.
- The `testCrypto()` function referenced in `AppCommon.desktop.kt:39` is commented out with a `// LALAL` marker, suggesting crypto testing was planned but never completed.

---

## GAP-08: Nome Android: Startup and Empty-State Truth

**Severity:** High
**Category:** Product truth / Recovery
**Platform:** Android
**Pages:** P01, P08
**Decision status:** **[DECIDED 2026-07-18 — P08 FROZEN; P01 BOUNDED SOURCE CONNECTED, EXECUTION GATES ACTIVE]**

### Recorded Decision

Preserve the official root-state priority and render only existing branches and results. Use indeterminate progress unless a real stage or numerator/denominator exists. Remove staged percentages, generic restore/rollback, and core-timeout guarantees. A client waiting threshold may offer diagnostics or retry, but it must not claim that the core failed. P08 must distinguish loading, first use, true empty, filtered empty, device offline, core stopped, and network-unknown; the optimistic/default network sentinel is not online evidence. The re-encryption `.bak` path may be described as recovery only when its existing timestamp/file preconditions are satisfied.

### Description

The approved Nome pages show distinct startup, migration, restore, timeout, loading, offline, and first-use states. v6.5.6 exposes database opening/migration/error branches, onboarding state, chat data, core-running state, and device network state, but it does not expose a trustworthy general restore operation, staged migration percentage, or a product timeout guarantee. Initial network state can also be optimistic before the observer reports.

### Current P08 implementation boundary

The Batch 2 source adds typed chat-list outcomes instead of changing the core. [`apiGetChatsResult`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081) distinguishes a successful `CR.ApiChats`, command/parse failure, and no current user. [`ChatModel`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106) carries the user/host generation, and [`NomeHomeStateAdapter`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L49-L121) permits true empty only after same-generation success. Loading, first use, filtered no result, unavailable, connectivity, and core stopped remain distinct in the adapter; identity mismatch suppresses stale rows. First use and filtered no result are not currently production-route reachable because the root consumes no-user in onboarding and this bounded home has no filter producer, so those P08 gates remain open.

Android's first platform network observation is exposed separately from the legacy optimistic model sentinel. Before that observation P08 renders network unknown; observed absence of a validated network renders device offline only. Batch 2 is frozen under its evidence root.

### Current P01 implementation boundary

Batch 1A preserves the official root order, one-second opening presentation delay, and
authentication ownership. Android maps only exact `DBMigrationResult` subtypes, in-progress flags,
no-secret database-key read class, stored-key Boolean facts, and the exact matched backup pair.
Manual open-once does not persist a key; save-and-open remains separately explicit. Backup copy
success is attempt/source-bound and still requires a fresh explicit database open. Raw native,
SQL, path, JSON, and Keystore throwable text are not replayed by the P01 UI or database-key
Logcat branch. Unsupported percentage, stages, timeout, cancel, generic restore/rollback, identity
loaded, and messaging-restored claims remain absent. GAP-08 stays open until the P01 evidence root
closes its real fixtures, accessibility, release, and two-review gates.

### Product Impact

A fabricated percentage, “restored” result, or ordinary empty list shown while the core is stopped could cause a user to interrupt migration, misunderstand data recovery, or believe messaging is ready when it is not.

### Candidate Decisions

1. **Recommended:** keep the approved layout but use indeterminate progress and exact existing branch labels; remove unsupported restore and percentage claims; after a client timeout offer diagnostics/retry without claiming core failure.
2. Defer P01/P08 until a separately scoped capability supplies real stage/timeout facts.
3. Authorize a core/database capability project outside this Android UI scope.

### Core Boundary

This Android effort must not add a native progress API or alter migration ordering. Under the recorded route, P01/P08 may implement only existing branch truth; every unprovable progress, restore, timeout, or readiness fact must be removed or remain blocked.

---

## GAP-09: Nome Android: Local Authentication Fail-Safe and Throttling

**Severity:** Critical
**Category:** Security / Authentication
**Platform:** Android
**Page:** P02
**Decision status:** **[DECIDED 2026-07-17 — RECOMMENDED ROUTE ADOPTED; IMPLEMENTATION PENDING]**

### Recorded Decision

When a user-enabled lock is active, authentication fails closed. `LAResult.Unavailable` must not authorize the user or silently disable protection. Nome may offer only an already configured device-credential or app-passcode recovery path. App-passcode backoff is Android/client state and must remain independent of self-destruct behavior. Until attempt thresholds, clock behavior, and restart persistence are specified in `spec/state.md`, the UI must not show an invented lockout countdown; the P02 dark representative uses the unavailable/recovery state.

### Description

Biometric, device-credential, and app-passcode paths exist, but an unavailable local-auth path can currently disable `performLA` and continue. There is no independent product state for hardware temporarily unavailable versus not enrolled, and repeated app-passcode attempts do not have a documented throttling/backoff contract.

### Product Impact

Treating unavailable authentication as success can weaken a user-enabled lock. Invented lockout text would be equally unsafe if it is not enforced.

### Candidate Decisions

1. **Recommended:** when the user has enabled a lock, fail closed and offer an existing configured app-passcode/device-credential recovery path; add client-enforced attempt backoff with explicit tests and no destructive self-destruct coupling.
2. Preserve current fail-open behavior but disclose it explicitly in settings and the unavailable state.
3. Defer the unavailable/lockout Nome states.

### Core Boundary

This is Android/client authentication behavior. It must not be implemented by changing the chat core, database key derivation, or self-destruct semantics.

---

## GAP-10: Nome Android: Async Operation and Permission States

**Severity:** High
**Category:** UI state / Error recovery / Permissions
**Platform:** Android
**Pages:** P04, P05, P06, P11, P12, P20, P22, P24
**Decision status:** **[DECIDED 2026-07-17 — RECOMMENDED ROUTE ADOPTED; IMPLEMENTATION PENDING]**

### Recorded Decision

Add Android/client operation state around existing APIs: single submission, typed result mapping, reconciliation with the returned model/core fact, input and failure retention, and safe retry. Permanent Android permission denial must offer the system Settings route and re-read platform state on resume. After a destructive boundary, Nome must preserve the exact phase and partial-failure outcome; it must not clear UI or claim success from a local Boolean, timer, or optimistic default. This decision does not authorize a new native command or a change to archive/migration semantics.

### Description

The underlying create-user, notification-permission, accept-conditions, invitation-generation, QR parsing, channel deletion, user deletion, archive, and migration operations exist, but several pages lack durable page-level in-flight/failure/retry state. Android notification/camera handling also lacks consistently modelled denial/recovery behavior; camera handling specifically lacks a reachable permanent-denial/settings route and explicit camera-unavailable state. Some APIs return `null` after logging rather than a typed user-facing error (related to GAP-01). The current channel-creation cancel path ignores the Boolean delete outcome and removes local state anyway; active-user deletion is a non-atomic switch/delete/stop sequence whose failure does not necessarily preserve the previous active model. Ordinary archive export produces an internal file before opening the SAF destination chooser and does not retain a typed destination-copy result; ordinary import catches URI-copy exceptions but can let delete/import `Exception` paths escape to log-only outer handling after the destructive boundary.

### Product Impact

Users may submit twice, see a frozen form, lose the recovery action, or be trapped after permanent permission denial. A generic success animation cannot substitute for a confirmed API result.

### Candidate Decisions

1. **Recommended:** add Android/client operation state around existing APIs, disable duplicate submission, reconcile success with the real model result, show only returned or safely mapped errors, and provide Settings routing when Android reports permanent denial.
2. Simplify the approved pages to only the states currently surfaced by upstream UI.
3. Defer affected states until typed API errors exist.

### Core Boundary

Client operation state may wrap existing calls. It cannot manufacture an API result or change archive/migration protocol semantics.

---

## GAP-11: Nome Android: Global and Settings Search Model

**Severity:** Medium
**Category:** Search / Information architecture
**Platform:** Android
**Pages:** P09, P23
**Decision status:** **[DECIDED 2026-07-17 — RECOMMENDED ROUTE ADOPTED; IMPLEMENTATION PENDING]**

### Recorded Decision

Use an explicitly scoped client search coordinator. Contacts, groups, and channels may be grouped from loaded local chat truth. The first release does not claim complete global message aggregation; the unsupported grouped-message state is removed or reworded as a clearly limited single-conversation scope. Settings search uses an allowlisted local index with stable route IDs. The first release stores no recent-query history in preferences, logs, or production fixtures.

### Description

v6.5.6 has chat-list filtering and conversation/message search paths, but no single global result model that reliably groups contacts, groups, channels, and messages, no persisted recent-query model for the approved P09 layout, and no top-level settings search index.

### Product Impact

Combining unrelated local filters and API calls into a visually unified “global” result can imply completeness that the client did not query. Fake recent searches are production fixture leakage.

### Candidate Decisions

1. **Recommended:** implement an explicit client search coordinator with separately labelled scopes and a local settings-route index; omit recent searches until a deliberate local history policy is approved.
2. Keep search limited to current upstream capabilities and revise P09/P23 copy/layout.
3. Add persisted local query history with a retention/deletion privacy decision.

### Core Boundary

Local indexing and coordination are client features. They must not add a native search command without separate authorization.

---

## GAP-12: Nome Android: One-Time Invitation Lifecycle

**Severity:** High
**Category:** Connection security / Product truth
**Platform:** Android
**Page:** P11
**Decision status:** **[DECIDED 2026-07-17 — RECOMMENDED ROUTE ADOPTED; IMPLEMENTATION PENDING]**

### Recorded Decision

Remove TTL, countdown, and expired claims. Keep generated, copied, shared, waiting, peer-use-observed, connected, and failed as separate facts. Copy/share is only a local action; remote use and connection require the corresponding core event. The first release does not present an atomic regenerate operation. A new link is labelled “create new invitation,” and the previous link remains potentially valid until a real invalidation result proves otherwise.

### Description

v6.5.6 can create a one-time connection invitation and later receives connection events, but the client has no authoritative TTL/expiry timestamp. Current “showing invitation used” behavior can mean the user copied/shared the link, not that a remote party consumed it. Regeneration semantics are not an atomic replace guarantee.

### Product Impact

A “24 hours left,” “used,” or “regenerated safely” label can give false security and cause users to share a link they believe invalidated.

### Candidate Decisions

1. **Recommended:** remove the countdown/expiry claim; distinguish generated, copied/shared, waiting, connected/consumed-by-event, and failed; describe regeneration as creating a new invitation and only claim old-link invalidation when confirmed.
2. Defer P11 until the backend supplies TTL and invalidation facts.
3. Authorize a separate protocol/core lifecycle feature.

### Core Boundary

No client timer may be presented as server/core expiry. The Android UI must consume existing events only.

---

## GAP-13: Nome Android: Request and Public-Address Lifecycle

**Severity:** High
**Category:** Connection state / Destructive actions
**Platform:** Android
**Pages:** P14, P15
**Decision status:** **[DECIDED 2026-07-17 — RECOMMENDED ROUTE ADOPTED; IMPLEMENTATION PENDING]**

### Recorded Decision

Remove the unsupported request-message control. Reject must use a discriminated Android/client result: only a typed `ContactRequestRejected` response, including its valid `contact_ == null` form, may remove the request; an API error retains it. A withdrawn/stale request is described only as unavailable after authoritative disappearance or not-found, without inventing a cause. A bare `userAddress == null` is loading/unknown; only a preserved `UserContactLinkNotFound` result is “off.” Address replacement is an explicit delete-then-create sequence with separate outcomes and recovery, never an atomic or rollback-guaranteed action.

### Description

Accept, incognito accept, reject, address creation, sharing, and deletion have core commands. There is no explicit stable “request withdrawn” UI state in all paths, and replacing a public contact address is not an atomic operation with a guaranteed rollback. The current `apiRejectContactRequest(): Contact?` wrapper returns null both for a successful `CR.ContactRequestRejected(contact_=null)` and for an error, while the current chat-list handler removes the request in either case; Nome therefore needs a discriminated client result before it may claim reject success. The P14 approved optional request-message field also has no `UserContactRequest` field or accept-command parameter. `CR.ReceivedContactRequest.chat_` means the request was created with an existing contact chat; it is not proof of a request message and cannot be repurposed as one.

### Product Impact

Showing a stale request as actionable can produce failure loops. Presenting an unsupported request message can misattribute existing conversation content to the request. Presenting address replacement as atomic can hide a window where no address exists or the old link remains relevant.

### Candidate Decisions

1. **Recommended:** omit the optional request-message control; treat disappearance/event refresh as the only withdrawn evidence; label address replacement as a confirmed destructive disable/delete followed by create, with separate outcomes and recovery text.
2. Remove optional-message, withdrawn, and replace states from the first Nome release.
3. Authorize separately specified request-message and atomic-replace capabilities.

### Core Boundary

The Android shell cannot add transactional guarantees around multiple core commands.

---

## GAP-14: Nome Android: Group, Report, and Channel Claims

**Severity:** High
**Category:** Safety / Moderation / Public channels
**Platform:** Android
**Pages:** P16, P18, P20
**Decision status:** **[DECIDED 2026-07-17 — RECOMMENDED ROUTE ADOPTED; IMPLEMENTATION PENDING]**

### Recorded Decision

Remove the verified-admin-contact claim; link/owner verification is not contact security-code verification. Remove direct-chat reporting; expose reporting only under the existing exact group-message eligibility gate and route. Keep official compatible link output and do not create a Nome slug or domain until a separate ownership/release decision and capability exist.

### Description

The approved design includes a verified group-admin contact, a direct-chat report action, and a custom Nome channel slug/domain. Current group-link preview data cannot prove a verified admin contact; the existing report command is group moderation oriented; v6.5.6 has no Nome domain/slug contract.

### Product Impact

These claims can cause a user to trust a malicious group/channel or believe a report went to a moderator when no supported route exists.

### Candidate Decisions

1. **Recommended:** remove “verified admin” unless the current data proves it; expose reports only where the real group command exists; keep official compatible link output until a domain release decision.
2. Defer affected controls/pages.
3. Authorize separate reporting, verification, and Nome-domain capability projects.

### Core Boundary

This UI effort cannot change report routing, group-link metadata, relay protocol, or link-domain ownership.

---

## GAP-15: Nome Android: Anonymous Identity Semantics

**Severity:** High
**Category:** Identity / Privacy wording
**Platform:** Android
**Page:** P22
**Decision status:** **[DECIDED 2026-07-17 — RECOMMENDED ROUTE ADOPTED; IMPLEMENTATION PENDING]**

### Recorded Decision

Persistent entries are local identities. Incognito is described only as an independent identity for this connection. It is an explanation/connection choice, not a durable list row, reusable identifier, account, or new user type.

### Description

v6.5.6 supports multiple persistent local user profiles and a generated incognito profile for an individual connection. The approved identity-center wording can be read as a persistent, reusable anonymous identity, which is a different product model.

### Product Impact

Users may believe one anonymous profile is isolated or reused across connections when the actual privacy boundary is per connection.

### Candidate Decisions

1. **Recommended:** keep persistent entries as local identities and describe incognito only as “use an independent incognito identity for this connection”; do not list it as a durable account.
2. Remove incognito from the identity center and show it only in connection preview.
3. Authorize a separate persistent-anonymous-profile product feature.

### Core Boundary

The Android shell must not simulate a new user/profile type or silently persist generated incognito identities.

---

## GAP-16: Nome Android: Archive and Migration Recovery Contract

**Severity:** Critical
**Category:** Data safety / Migration
**Platform:** Android
**Pages:** P03, P24
**Decision status:** **[DECIDED 2026-07-17 — RECOMMENDED ROUTE ADOPTED; IMPLEMENTATION PENDING]**

### Recorded Decision

Use determinate progress only for real byte/stage events and indeterminate presentation otherwise. Cancellation is exposed only where an exact API and handle support it, and is disabled after an irreversible boundary. Resume is named only for persisted checkpoints that can actually be reconstructed; an interrupted receiver download restarts, and sender process-death resume is unsupported. Advise backup before migration without promising rollback. Later high-risk client work must also correct secret `rememberSaveable`, premature `YesUp`, `InvalidConfirmation` success handling, corrupt-checkpoint crashes, and checkpoint/onboarding completion ordering before those states can ship. None of those corrections belongs to the Phase 2 foundation batch.

### Description

Archive export/import and device migration exist, but ordinary archive operations do not expose the determinate progress/cancel contract shown by the design. P03's migration entry/back/resume presentation inherits the same stage-specific leave/recovery boundary. Send-side process-death resume and a general rollback guarantee are not proven by the v6.5.6 client API. The v6.5.6 Kotlin flow also has safety-relevant presentation/control gaps that Nome must not inherit silently: link acceptance begins with a prefix-only test and advances even when standalone metadata is null; receiver import unconditionally deletes storage before import; the passphrase probe supplies `YesUp` before an upgrade confirmation UI; sender cancel cleanup differs by stage; corrupt persisted checkpoint JSON is not caught; sender and receiver passphrase fields use plaintext `rememberSaveable`; and receiver completion can clear the checkpoint or complete onboarding after a failed init/settings/start step.

### Product Impact

An unsupported cancel, resume, or rollback promise can cause partial migration, duplicate actions, or data loss at the most sensitive point in the app.

### Candidate Decisions

1. **Recommended:** use indeterminate progress unless a real migration stage exists; disable cancellation after the irreversible boundary; show resume only for paths that persist enough state; describe backup-before-migration rather than promise rollback.
2. Simplify P24 to the exact upstream operation states.
3. Authorize a separate archive/migration protocol and recovery project.

### Core Boundary

Do not alter the archive format, migration protocol, database order, or native APIs in this Android UI scope.

---

## GAP-17: Nome Android: Network Evidence Levels

**Severity:** High
**Category:** Network truth / Privacy wording
**Platform:** Android
**Pages:** P05, P06, P08, P20, P21, P23
**Decision status:** **[DECIDED 2026-07-17 — P08 DEVICE-CONNECTIVITY SOURCE CONNECTED; OTHER PAGES AND GATES PENDING]**

### Recorded Decision

Keep device connectivity, configuration, one validation result, operator/server state, per-group/per-relay state, and item-level security events as separate evidence levels. Default or unavailable sentinels render as unknown. Do not aggregate them into private-routing health, global network/relay health, presence, or persistent E2EE. The P21 public-channel non-E2EE disclosure derives from channel mode; `E2EEInfo.public` is rendered only for the event item that carries it.

### Description

The client can know Android connectivity, configured servers/operators, server-validation results, and some group relay statuses. These are separate evidence levels. P06 can use device connectivity to explain offline terms behavior, but that does not prove cached terms are current. The approved wording can collapse these facts into a single “private routing/healthy network” statement that no single v6.5.6 field proves. For P21, typed `SndGroupE2EEInfo`/`RcvGroupE2EEInfo` items carry `E2EEInfo.public` and may be rendered as the security event they represent, but neither that historical item nor a relay status proves a persistent aggregate E2EE or end-to-end relay-health badge.

### Current P08 implementation boundary

[`NetworkObserver.platformNetworkInfo`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L16-L87) is nullable until Android connectivity is observed, and marks online only when the active network has both Internet and validated capabilities. The P08 adapter maps that fact to unknown/online/device-offline without changing `ChatModel.networkInfo` or inferring relay/server/global health. This resolves and verifies the missing first-observation source for the bounded P08 renderer only. It does not implement or validate the broader P05/P06/P20/P21/P23 evidence levels.

### Product Impact

Users may interpret local connectivity or successful syntax validation as proof that traffic is private, every relay is reachable, or the channel is secure.

### Candidate Decisions

1. **Recommended:** label device connectivity, configuration validation, operator/server status, and group relay status separately; never infer private routing or E2EE from a lower layer.
2. Remove high-level health claims and show configuration only.
3. Authorize a separately specified end-to-end diagnostic capability.

### Core Boundary

The Android shell may format existing evidence but cannot probe or certify routing beyond existing APIs.

---

## GAP-18: Android API 26: Official v6.5.6 Native Artifact Cannot Load

**Severity:** Critical
**Category:** Native compatibility / Minimum SDK
**Platform:** Android API 26–27
**Decision status:** **[DECIDED AND VERIFIED 2026-07-17 — MIN SDK 28]**

### Recorded Decision

Nome freezes its release minimum at Android API 28; API 26–27 are unsupported. No exact-commit native rebuild, compatibility shim, or Haskell/native source change is authorized. Both Android Gradle defaults now declare 28; debug and release merged manifests and the arm64 debug APK badging report 28. A clean API 28 device cold-started the unchanged official core, and the same-package API 35 upgrade preserved the non-empty chat database and preferences before cold-starting the real server/receiver. The agent database is runtime-mutable and was therefore recorded, not misreported as byte-identical. The Phase 1 minSdk 26 build and API 26 crash evidence remain immutable historical facts.

### Description

The v6.5.6 manifest declares minSdk 26, but its official arm64 `libsimplex.so` has an undefined `getentropy` dependency. Android NDK 23.1.7779620 declares `getentropy` as introduced in API 28. On a dedicated API 26 arm64 AVD, both the unchanged-source `.nome.dev` debug APK and the official signed v6.5.6 arm64 release APK install successfully and then crash during `SimplexApp.onCreate` with the same `UnsatisfiedLinkError`.

Both official v6.5.6 ABI libraries contain a `getentropy` reference, so changing to the official armv7 APK is not accepted as a proven API 26 solution.

### Product Impact

Keeping minSdk 26 without a compatible native artifact advertises installability on devices where the app cannot start. This is a release-blocking compatibility and data-access risk, independent of the Nome UI.

### Historical Alternatives Considered

1. **Recommended if API 26 support is required:** build the exact unmodified v6.5.6 native source with an API 26-compatible toolchain/configuration, prove that both ABI artifacts no longer require post-26 symbols, and repeat full provenance, API 26, API 35, and database-upgrade verification.
2. If that controlled build cannot meet the requirement without native source changes, explicitly raise Nome's minSdk to 28 and update product/release support claims.
3. Defer public release while keeping source minSdk 26; this is not acceptable as a completed release decision.

### Core Boundary

No Haskell/native source patch or compatibility artifact rebuild is authorized by the current Nome Android scope. Reopening API 26 support would be a separate product/security decision with its own provenance, dual-ABI, API 26/API 35, and non-empty-upgrade gates; it cannot be inferred from this minSdk 28 implementation.

### Evidence

See `plans/evidence/20260716_nome_android_phase1/api26-native-compatibility.txt` for the immutable failure baseline and `plans/evidence/20260717_nome_android_phase2_foundation/` for the minSdk 28 manifests, APK badging, API 28 launch, and API 35 non-empty upgrade record.
