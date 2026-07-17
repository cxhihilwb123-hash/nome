# System Architecture

## Table of Contents

1. [Overview](#1-overview)
2. [Module Structure](#2-module-structure)
3. [JNI Bridge](#3-jni-bridge)
4. [App Lifecycle](#4-app-lifecycle)
5. [Event Streaming](#5-event-streaming)
6. [Platform Abstraction](#6-platform-abstraction)
7. [Source Files](#7-source-files)

---

## 1. Overview

The application is a three-layer system:

```
+------------------------------------------------------------------+
|                     Compose UI (Views)                           |
|  ChatListView, ChatView, ComposeView, SettingsView, CallView    |
+------------------------------------------------------------------+
        |                    ^
        | user actions       | Compose MutableState recomposition
        v                    |
+------------------------------------------------------------------+
|              Application Logic Layer                             |
|  ChatModel (state)   ChatController (command dispatch)           |
|  AppPreferences      NtfManager      ThemeManager                |
+------------------------------------------------------------------+
        |                    ^
        | sendCmd()          | recvMsg() / processReceivedMsg()
        v                    |
+------------------------------------------------------------------+
|                    JNI Bridge (Core.kt)                          |
|  external fun chatSendCmdRetry()    external fun chatRecvMsgWait()|
+------------------------------------------------------------------+
        |                    ^
        | C FFI              | C FFI
        v                    |
+------------------------------------------------------------------+
|              Haskell Core (libsimplex / libapp-lib)              |
|  chat_ctrl handle    SMP/XFTP protocols    SQLite/PostgreSQL     |
+------------------------------------------------------------------+
```

**Data flow summary:**
1. User interacts with Compose UI.
2. View calls a `suspend fun api*()` method on `ChatController`.
3. `ChatController.sendCmd()` serializes the command to a JSON string and calls `chatSendCmdRetry()` (JNI).
4. The Haskell core processes the command and returns a JSON response string.
5. The response is deserialized to an `API` sealed class and returned to the caller.
6. Asynchronous events from the core (incoming messages, connection updates, call invitations) are delivered via a receiver coroutine that calls `chatRecvMsgWait()` in a loop and dispatches each event through `processReceivedMsg()`.

---

## 2. Module Structure

### Gradle Configuration

Root: [`settings.gradle.kts`](../settings.gradle.kts#L21)
```
include(":android", ":desktop", ":common")
```

### `:common` Module

Build file: [`common/build.gradle.kts`](../common/build.gradle.kts#L39)

```
kotlin {
    androidTarget()
    jvm("desktop")
}
```

Source sets:

| Source Set | Path | Purpose |
|---|---|---|
| `commonMain` | `common/src/commonMain/kotlin/` | All shared UI, models, platform abstractions |
| `androidMain` | `common/src/androidMain/kotlin/` | Android `actual` implementations |
| `desktopMain` | `common/src/desktopMain/kotlin/` | Desktop `actual` implementations |

Key dependencies (from `commonMain`):
- `kotlinx-serialization-json` -- JSON codec for Haskell core communication
- `kotlinx-datetime` -- cross-platform date/time
- `multiplatform-settings` (russhwolf) -- `SharedPreferences` abstraction
- `kaml` -- YAML parsing (theme import/export)
- `boofcv-core` -- QR code scanning
- `jsoup` -- HTML parsing for link previews
- `moko-resources` -- cross-platform string/image resources
- `multiplatform-markdown-renderer` -- Markdown rendering in chat

### `:android` Module

Build file: [`android/build.gradle.kts`](../android/build.gradle.kts)

Contains:
- `SimplexApp` (Application subclass)
- `MainActivity` (FragmentActivity)
- `SimplexService` (foreground Service)
- `NtfManager` (Android NotificationManager wrapper)
- `CallActivity` (dedicated activity for calls)

### `:desktop` Module

Build file: [`desktop/build.gradle.kts`](../desktop/build.gradle.kts)

Contains:
- `main()` entry point
- `initHaskell()` -- loads native library and calls `initHS()`
- Window management (VLC library loading on Windows)

---

## 3. JNI Bridge

All JNI declarations reside in [`Core.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt).

<a id="initHS"></a>
<a id="chatMigrateInit"></a>

### External Native Functions

| # | Function | Signature | Purpose |
|---|---|---|---|
| 1 | [`initHS()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L18) | `external fun initHS()` | Initialize GHC runtime system |
| 2 | [`pipeStdOutToSocket()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L20) | `external fun pipeStdOutToSocket(socketName: String): Int` | Redirect Haskell stdout to Android local socket for logging |
| 3 | [`chatMigrateInit()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L25) | `external fun chatMigrateInit(dbPath: String, dbKey: String, confirm: String): Array<Any>` | Initialize database with migration; returns `[jsonResult, chatCtrl]` |
| 4 | [`chatCloseStore()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L26) | `external fun chatCloseStore(ctrl: ChatCtrl): String` | Close database store |
| 5 | [`chatSendCmdRetry()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L27) | `external fun chatSendCmdRetry(ctrl: ChatCtrl, msg: String, retryNum: Int): String` | Send command to core with retry count |
| 6 | [`chatSendRemoteCmdRetry()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L28) | `external fun chatSendRemoteCmdRetry(ctrl: ChatCtrl, rhId: Int, msg: String, retryNum: Int): String` | Send command to remote host |
| 7 | [`chatRecvMsg()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L29) | `external fun chatRecvMsg(ctrl: ChatCtrl): String` | Receive message (non-blocking) |
| 8 | [`chatRecvMsgWait()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L30) | `external fun chatRecvMsgWait(ctrl: ChatCtrl, timeout: Int): String` | Receive message with timeout (blocking up to `timeout` microseconds) |
| 9 | [`chatParseMarkdown()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L31) | `external fun chatParseMarkdown(str: String): String` | Parse markdown formatting |
| 10 | [`chatParseServer()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L32) | `external fun chatParseServer(str: String): String` | Parse SMP/XFTP server address |
| 11 | [`chatParseUri()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L33) | `external fun chatParseUri(str: String, safe: Int): String` | Parse SimpleX connection URI |
| 12 | [`chatPasswordHash()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L34) | `external fun chatPasswordHash(pwd: String, salt: String): String` | Hash password with salt |
| 13 | [`chatValidName()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L35) | `external fun chatValidName(name: String): String` | Validate/sanitize display name |
| 14 | [`chatJsonLength()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L36) | `external fun chatJsonLength(str: String): Int` | Get JSON-encoded string length |
| 15 | [`chatWriteFile()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L37) | `external fun chatWriteFile(ctrl: ChatCtrl, path: String, buffer: ByteBuffer): String` | Write encrypted file via core |
| 16 | [`chatReadFile()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L38) | `external fun chatReadFile(path: String, key: String, nonce: String): Array<Any>` | Read and decrypt file |
| 17 | [`chatEncryptFile()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L39) | `external fun chatEncryptFile(ctrl: ChatCtrl, fromPath: String, toPath: String): String` | Encrypt file on disk |
| 18 | [`chatDecryptFile()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L40) | `external fun chatDecryptFile(fromPath: String, key: String, nonce: String, toPath: String): String` | Decrypt file on disk |

**Total: 18 external native functions** (the [`ChatCtrl`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L23) type alias is `Long`, representing the Haskell-side controller pointer).

<a id="initChatControllerOnStart"></a>
<a id="chatInitTemporaryDatabase"></a>
<a id="chatInitControllerRemovingDatabases"></a>

### Key Kotlin Functions in Core.kt

| Function | Purpose |
|---|---|
| [`initChatControllerOnStart()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L51) | Entry point called during app startup; launches `initChatController` in a long-running coroutine |
| [`initChatController()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L62) | Main initialization: DB migration via `chatMigrateInit`, error recovery (incomplete DB removal), sets file paths, loads active user, starts chat |
| [`chatInitTemporaryDatabase()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L186) | Creates a temporary database for migration scenarios |
| [`chatInitControllerRemovingDatabases()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L198) | Removes existing DBs and creates fresh controller (used during re-initialization) |
| [`showStartChatAfterRestartAlert()`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L218) | Shows confirmation dialog when chat was stopped and DB passphrase is stored |

<a id="initChatController"></a>

### initChatController Flow

```
initChatController(useKey, confirmMigrations, startChat)
  |
  +-- chatMigrateInit(dbPath, dbKey, confirm)     // JNI -> Haskell
  |     returns [jsonResult, chatCtrl]
  |
  +-- if migration error and rerunnable:
  |     chatMigrateInit(dbPath, dbKey, confirm)    // retry with user confirmation
  |
  +-- setChatCtrl(ctrl)                            // store controller handle
  +-- apiSetAppFilePaths(...)                       // tell core about file dirs
  +-- apiSetEncryptLocalFiles(...)
  +-- apiGetActiveUser() -> currentUser
  +-- getServerOperators() -> conditions
  +-- if shouldImportAppSettings: apiGetAppSettings + importIntoApp
  +-- if user exists and startChat confirmed:
  |     startChat(user)                            // starts receiver, API commands
  +-- else if no user:
       set onboarding stage, optionally startChatWithoutUser()
```

---

## 4. App Lifecycle

### Android

Entry: [`SimplexApp.onCreate()`](../android/src/main/java/chat/simplex/app/SimplexApp.kt#L47)

```
SimplexApp.onCreate()
  +-- initHaskell(packageName)           // Load native lib, pipe stdout, call initHS()
  |     +-- System.loadLibrary("app-lib")
  |     +-- pipeStdOutToSocket(packageName)
  |     +-- initHS()
  +-- initMultiplatform()                // Set up ntfManager, platform callbacks
  +-- reconfigureBroadcastReceivers()
  +-- runMigrations()                    // Theme migration, version code tracking
  +-- initChatControllerOnStart()        // -> initChatController() -> chatMigrateInit -> startChat
```

Activity: [`MainActivity.onCreate()`](../android/src/main/java/chat/simplex/app/MainActivity.kt#L33)

```
MainActivity.onCreate()
  +-- processNotificationIntent(intent)  // Handle OpenChat/AcceptCall from notifications
  +-- processIntent(intent)              // Handle VIEW intents (deep links)
  +-- processExternalIntent(intent)      // Handle SEND/SEND_MULTIPLE (share sheet)
  +-- setContent
        +-- NomeProductionShell          // Android window/system-bar synchronization only
              +-- AppScreen()            // Existing shared root remains authoritative
```

Lifecycle callbacks in `SimplexApp` (implements `LifecycleEventObserver`):
- `ON_START`: if chat is running, begin a generation-scoped load, request a typed result, preserve current-chat stats, then apply the result only if it still belongs to the active user/host generation ([`SimplexApp.onStateChanged()`](../android/src/main/java/chat/simplex/app/SimplexApp.kt#L89-L120))
- `ON_RESUME`: show background service notice, start `SimplexService` if configured

[`NomeProductionShell`](../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L17-L23) does not replace the application root. It synchronizes Android system-bar appearance and delegates immediately to `AppScreen`. The shared root still owns authentication, onboarding, calls, overlays, safe-area behavior, back dispatch, the delivery-receipts gate, and share-intent routing. Inside that root, [`StartPartOfScreen`](../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L366-L393) selects the narrow `PlatformHomeRoute` seam only where the ordinary chat-list route was already selected.

### Desktop

Entry: [`main()`](../desktop/src/jvmMain/kotlin/chat/simplex/desktop/Main.kt#L22)

```
main()
  +-- initHaskell()                      // Load native lib from resources dir, call initHS()
  |     +-- System.load(libapp-lib.so/dll/dylib)
  |     +-- initHS()
  +-- runMigrations()
  +-- setupUpdateChecker()
  +-- initApp()                          // Set ntfManager, applyAppLocale, initChatControllerOnStart
  +-- showApp()                          // Compose window with AppScreen()
```

[`showApp()`](../common/src/desktopMain/kotlin/chat/simplex/common/DesktopApp.kt#L34) creates a Compose `Window` with error recovery -- if a crash occurs, it closes the offending modal/view and re-opens the window.

[`initApp()`](../common/src/desktopMain/kotlin/chat/simplex/common/platform/AppCommon.desktop.kt#L21) sets the `ntfManager` implementation (desktop notifications via `NtfManager` in `common/model/`) and calls `initChatControllerOnStart()`.

---

## 5. Event Streaming

### Receiver Coroutine

[`ChatController.startReceiver()`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L681) launches a coroutine on `Dispatchers.IO` that continuously polls for events from the Haskell core:

```kotlin
private fun startReceiver() {
    if (receiverJob != null || chatCtrl == null) return  // guard against double-start
    receiverJob = CoroutineScope(Dispatchers.IO).launch {
        var releaseLock: (() -> Unit) = {}
        while (isActive) {
            val ctrl = chatCtrl
            if (ctrl == null) { stopReceiver(); break }  // chatCtrl became null
            try {
                val release = releaseLock
                launch { delay(30000); release() }       // release previous wake lock after 30s
                val msg = recvMsg(ctrl)                   // calls chatRecvMsgWait with 300s timeout
                releaseLock = getWakeLock(timeout = 60000) // acquire wake lock (60s timeout)
                if (msg != null) {
                    val finished = withTimeoutOrNull(60_000L) {
                        processReceivedMsg(msg)
                        messagesChannel.trySend(msg)
                    }
                    if (finished == null) {
                        Log.e(TAG, "Timeout processing: " + msg.responseType)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "recvMsg/processReceivedMsg exception: " + e.stackTraceToString())
            } catch (e: Throwable) {
                Log.e(TAG, "recvMsg/processReceivedMsg throwable: " + e.stackTraceToString())
            }
        }
    }
}
```

### Message Reception

[`recvMsg()`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L856) calls `chatRecvMsgWait(ctrl, MESSAGE_TIMEOUT)` where `MESSAGE_TIMEOUT = 300_000_000` microseconds (300 seconds). Returns `null` on timeout (empty string from Haskell), otherwise deserializes the JSON response to an `API` instance.

### Command Sending

[`sendCmd()`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L831) runs on `Dispatchers.IO`, serializes the command via `CC.cmdString`, calls `chatSendCmdRetry()` (or `chatSendRemoteCmdRetry()` for remote hosts), deserializes the response, and logs terminal items.

### Event Processing

[`processReceivedMsg()`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L2695) is a large `when` block that dispatches on the `CR` (ChatResponse) type:

- `CR.ContactConnected` -- update contact in `ChatModel`
- `CR.NewChatItems` -- add items to chat, trigger notifications
- `CR.RcvCallInvitation` -- add to `callInvitations`, trigger call UI
- `CR.ChatStopped` -- set `chatRunning = false`
- `CR.GroupMemberConnected`, `CR.GroupMemberUpdated`, etc. -- update group state
- Many more event types for connection status, file transfers, SMP relay events, etc.

### Wake Lock

On Android, the receiver acquires a wake lock via [`getWakeLock(timeout)`](../common/src/commonMain/kotlin/chat/simplex/common/platform/SimplexService.kt#L3) (expect function) after each received message with a 60-second timeout. The previous iteration's wake lock is released after a 30-second delay, ensuring overlap so the CPU does not sleep between messages.

---

## 6. Platform Abstraction

### expect/actual Pattern

The `commonMain` source set declares `expect` functions and classes. Each platform source set provides `actual` implementations.

Examples from platform files:

| expect Declaration | Source |
|---|---|
| `expect val appPlatform: AppPlatform` | [`AppCommon.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/AppCommon.kt#L20) |
| `expect val deviceName: String` | [`AppCommon.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/AppCommon.kt#L22) |
| `expect fun isAppVisibleAndFocused(): Boolean` | [`AppCommon.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/AppCommon.kt#L24) |
| `expect val dataDir: File` | [`Files.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt#L18) |
| `expect val tmpDir: File` | [`Files.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt#L19) |
| `expect val filesDir: File` | [`Files.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt#L20) |
| `expect val appFilesDir: File` | [`Files.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt#L21) |
| `expect val dbAbsolutePrefixPath: String` | [`Files.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt#L24) |
| `expect fun showToast(text: String, timeout: Long)` | [`UI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/UI.kt#L6) |
| `expect fun hideKeyboard(view: Any?, clearFocus: Boolean)` | [`UI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/UI.kt#L16) |
| `expect fun getKeyboardState(): State<KeyboardState>` | [`UI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/UI.kt#L15) |
| `expect fun allowedToShowNotification(): Boolean` | [`Notifications.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Notifications.kt#L3) |
| `expect class VideoPlayer` | [`VideoPlayer.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/VideoPlayer.kt#L25) |
| `expect class RecorderNative` | [`RecAndPlay.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/RecAndPlay.kt#L17) |
| `expect val cryptor: CryptorInterface` | [`Cryptor.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Cryptor.kt#L9) |
| `expect fun base64ToBitmap(base64ImageString: String): ImageBitmap` | [`Images.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Images.kt#L17) |
| `expect fun getWakeLock(timeout: Long): (() -> Unit)` | [`SimplexService.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/SimplexService.kt#L3) |
| `expect class GlobalExceptionsHandler` | [`UI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/UI.kt#L24) |
| `expect fun UriHandler.sendEmail(subject: String, body: CharSequence)` | [`Share.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Share.kt#L7) |
| `expect fun ClipboardManager.shareText(text: String)` | [`Share.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Share.kt#L9) |
| `expect fun shareFile(text: String, fileSource: CryptoFile)` | [`Share.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Share.kt#L10) |

### Platform Home Route

[`PlatformHomeRoute`](../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L15-L22) is a deliberately narrow `expect`/`actual` seam at the existing shared home-route selection point:

- Android's [`actual`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L63-L123) derives a presentation state from the authoritative `ChatModel`, the existing `filteredChats` result, and Android's latest direct network observation, then renders the Nome home surface.
- Desktop's [`actual`](../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt#L8-L17) invokes `defaultContent()` unchanged, preserving the existing desktop route and UI.
- The seam contains no second navigation tree, model, core bridge, or protocol. Shared `AppScreen`/`MainScreen` gates and `ChatController` remain authoritative.

The Android presentation adapter is a pure derivation layer. [`NomeHomeStateAdapter`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L29-L112) maps shared chat-list load truth into loading, first-use, true-empty, filtered-empty, populated, and unavailable presentation states; it neither stores application truth nor issues core commands. Route reachability is narrower: the unchanged root consumes no-user in onboarding and the bounded Android home has no active-filter producer, so first-use and filtered-empty are currently defensive renderer/test branches rather than production-route evidence.

### Android Network Observation

[`NetworkObserver.platformNetworkInfo`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L16-L24) is `null` until Android produces its first connectivity observation. The Nome adapter treats that value as `UNKNOWN`, never as online. [`networkCapabilitiesChanged`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L66-L79) and [`networkLost`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L81-L87) publish direct Android facts; the legacy `ChatModel.networkInfo` update remains behind the existing `apiSetNetworkInfo` acknowledgement path ([`setNetworkInfo`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L89-L106)).

### PlatformInterface Callback Object

[`PlatformInterface`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Platform.kt#L15) is an interface with default no-op implementations. It is assigned at runtime by each platform entry point:

- **Android**: assigned in [`SimplexApp.initMultiplatform()`](../android/src/main/java/chat/simplex/app/SimplexApp.kt#L194)
- **Desktop**: assigned in [`Main.kt initHaskell()`](../desktop/src/jvmMain/kotlin/chat/simplex/desktop/Main.kt#L37)

The global variable is declared as [`platform`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Platform.kt#L50):
```kotlin
var platform: PlatformInterface = object : PlatformInterface {}
```

#### PlatformInterface Callbacks

| Callback | Default | Android Implementation |
|---|---|---|
| `androidServiceStart()` | no-op | Start `SimplexService` foreground service |
| `androidServiceSafeStop()` | no-op | Stop `SimplexService` |
| `androidCallServiceSafeStop()` | no-op | Stop `CallService` |
| `androidNotificationsModeChanged(mode)` | no-op | Toggle receivers, start/stop service |
| `androidChatStartedAfterBeingOff()` | no-op | Start service or schedule periodic worker |
| `androidChatStopped()` | no-op | Cancel workers, stop service |
| `androidChatInitializedAndStarted()` | no-op | Show background service notice, start service |
| `androidIsBackgroundCallAllowed()` | `true` | Check battery restriction |
| `androidSetNightModeIfSupported()` | no-op | Set `UiModeManager` night mode |
| `androidSetStatusAndNavigationBarAppearance(...)` | no-op | Configure system bar colors/appearance |
| `androidStartCallActivity(acceptCall, rhId, chatId)` | no-op | Launch `CallActivity` |
| `androidPictureInPictureAllowed()` | `true` | Check PiP permission via AppOps |
| `androidCallEnded()` | no-op | Destroy call WebView |
| `androidRestartNetworkObserver()` | no-op | Restart `NetworkObserver` |
| `androidCreateActiveCallState()` | empty `Closeable` | Create `ActiveCallState` |
| `androidIsXiaomiDevice()` | `false` | Check device brand |
| `androidApiLevel` | `null` | `Build.VERSION.SDK_INT` |
| `androidLockPortraitOrientation()` | no-op | Lock to `SCREEN_ORIENTATION_PORTRAIT` |
| `androidAskToAllowBackgroundCalls()` | `true` | Show battery restriction dialog |
| `desktopShowAppUpdateNotice()` | no-op | Show update notice (Desktop only) |

---

## 7. Source Files

### Core Infrastructure

| File | Path | Key Contents |
|---|---|---|
| Core.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt) | JNI externals, `initChatController`, `chatInitTemporaryDatabase` |
| SimpleXAPI.kt | [`common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt) | `ChatController`, `AppPreferences`, `startReceiver`, `sendCmd`, `recvMsg`, `processReceivedMsg`, all `api*` functions |
| ChatModel.kt | [`common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt) | `ChatModel` singleton, `ChatsContext`, `Chat`, `ChatInfo`, `ChatItem` and all domain types |
| App.kt | [`common/src/commonMain/kotlin/chat/simplex/common/App.kt`](../common/src/commonMain/kotlin/chat/simplex/common/App.kt) | `AppScreen()`, `MainScreen()` |
| PlatformHomeRoute.kt | [`common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt`](../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt) | Shared home-route `expect` declaration |

### Platform Layer

| File | Path | Key Contents |
|---|---|---|
| Platform.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/Platform.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Platform.kt) | `PlatformInterface`, global `platform` var |
| AppCommon.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/AppCommon.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/AppCommon.kt) | `AppPlatform`, `runMigrations()` |
| AppCommon.android.kt | [`common/src/androidMain/kotlin/chat/simplex/common/platform/AppCommon.android.kt`](../common/src/androidMain/kotlin/chat/simplex/common/platform/AppCommon.android.kt) | `initHaskell()`, `androidAppContext` |
| AppCommon.desktop.kt | [`common/src/desktopMain/kotlin/chat/simplex/common/platform/AppCommon.desktop.kt`](../common/src/desktopMain/kotlin/chat/simplex/common/platform/AppCommon.desktop.kt) | `initApp()`, desktop NtfManager setup |
| Files.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt) | `expect val dataDir/tmpDir/filesDir/dbAbsolutePrefixPath` |
| NtfManager.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/NtfManager.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/NtfManager.kt) | `abstract class NtfManager` |
| Notifications.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/Notifications.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Notifications.kt) | `expect fun allowedToShowNotification()` |
| UI.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/UI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/UI.kt) | `showToast`, `hideKeyboard`, `GlobalExceptionsHandler` |
| Share.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/Share.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Share.kt) | `shareText`, `shareFile`, `openFile` |
| VideoPlayer.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/VideoPlayer.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/VideoPlayer.kt) | `VideoPlayerInterface`, `expect class VideoPlayer` |
| RecAndPlay.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/RecAndPlay.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/RecAndPlay.kt) | `RecorderInterface`, `AudioPlayerInterface` |
| Cryptor.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/Cryptor.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Cryptor.kt) | `CryptorInterface` |
| Images.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/Images.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Images.kt) | `base64ToBitmap`, `resizeImageToStrSize` |
| SimplexService.kt | [`common/src/commonMain/kotlin/chat/simplex/common/platform/SimplexService.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/SimplexService.kt) | `expect fun getWakeLock()` |
| NomeHomeRoute.android.kt | [`common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt) | Android `PlatformHomeRoute` actual and Nome home surface |
| NomeHomeStateAdapter.kt | [`common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt) | Pure Android presentation-state derivation |
| PlatformHomeRoute.desktop.kt | [`common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt`](../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt) | Desktop fallback to existing content |
| NetworkObserver.kt | [`common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt) | First-observation-aware Android connectivity state |

### Entry Points

| File | Path | Key Contents |
|---|---|---|
| SimplexApp.kt | [`android/src/main/java/chat/simplex/app/SimplexApp.kt`](../android/src/main/java/chat/simplex/app/SimplexApp.kt) | Android Application class, lifecycle observer |
| MainActivity.kt | [`android/src/main/java/chat/simplex/app/MainActivity.kt`](../android/src/main/java/chat/simplex/app/MainActivity.kt) | Android main activity |
| NomeProductionShell.kt | [`android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt`](../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt) | Android production host that delegates to the shared root |
| SimplexService.kt | [`android/src/main/java/chat/simplex/app/SimplexService.kt`](../android/src/main/java/chat/simplex/app/SimplexService.kt) | Android foreground service |
| Main.kt | [`desktop/src/jvmMain/kotlin/chat/simplex/desktop/Main.kt`](../desktop/src/jvmMain/kotlin/chat/simplex/desktop/Main.kt) | Desktop `main()` |
| DesktopApp.kt | [`common/src/desktopMain/kotlin/chat/simplex/common/DesktopApp.kt`](../common/src/desktopMain/kotlin/chat/simplex/common/DesktopApp.kt) | `showApp()`, `SimplexWindowState` |

### Theme

| File | Path | Key Contents |
|---|---|---|
| ThemeManager.kt | [`common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt`](../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt) | Theme resolution, system/light/dark/custom, per-user overrides |
