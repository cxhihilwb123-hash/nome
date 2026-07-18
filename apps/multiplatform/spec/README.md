# SimpleX Chat -- Kotlin Multiplatform Specification

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Dependency Graph](#dependency-graph)
3. [Specification Documents](#specification-documents)
4. [Product Documents](#product-documents)
5. [Source Entry Points](#source-entry-points)

---

## Executive Summary

SimpleX Chat is a Kotlin Multiplatform application targeting **Android** and **Desktop** (JVM) platforms. The UI layer is built entirely with Jetpack Compose. The application communicates with a Haskell-based cryptographic core (`simplex-chat`) through a **JNI bridge** -- native functions declared in Kotlin and linked at runtime to a shared library (`libapp-lib`). Platform-specific behavior (notifications, file system paths, services, audio/video) is abstracted using the `expect`/`actual` pattern and a runtime-assignable `PlatformInterface` callback object.

The Gradle project is structured as three modules:

| Module | Purpose |
|---|---|
| `:common` | Shared Compose UI, models, platform abstractions (`commonMain`, `androidMain`, `desktopMain`) |
| `:android` | Android application entry point (`SimplexApp`, `MainActivity`) |
| `:desktop` | Desktop application entry point (`Main.kt`, `showApp()`) |

Shared application truth and cross-platform behavior reside in `:common/commonMain`. Platform source sets (`androidMain`, `desktopMain`) provide `actual` implementations, host integration, and deliberately isolated platform-only code. In particular, the Phase 2 Nome design foundation is presentation-only Android code under `common/src/androidMain`; it does not create a second model, protocol, or core path.

---

## Dependency Graph

```
App Entry Points
+-- Android: SimplexApp.onCreate -> initHaskell -> initMultiplatform -> initChatControllerOnStart
|            MainActivity.onCreate -> NomeProductionShell -> AppScreen()
+-- Desktop: main() -> initHaskell -> runMigrations -> initApp -> showApp -> AppWindow -> AppScreen()
    |
    v
Common Module (commonMain)
+-- ChatModel (Compose state singleton) <-> ChatController/SimpleXAPI (JNI bridge) <-> Haskell Core (chat_ctrl)
+-- Views (Compose)
|   +-- App.kt: AppScreen -> MainScreen
|   +-- StartPartOfScreen -> PlatformHomeRoute
|       +-- Android actual: Nome home route + Android-only presentation adapter
|       +-- Desktop actual: delegates the existing ChatListView content unchanged
|   +-- Android ACTION_VIEW -> planAndConnect -> PlatformConnectionPreview
|       +-- Android actual: Nome P13 fullscreen route + reducer
|       +-- Desktop actual: declines and preserves the legacy connection alert
|   +-- ChatListView -> ChatView -> ComposeView -> SendMsgView
|   +-- ChatItemView (message rendering: text, image, video, voice, file, call, events)
|   +-- Settings: SettingsView, UserProfileView, UserProfilesView
|   +-- Onboarding: OnboardingView, WhatsNewView, CreateFirstProfile
|   +-- Call: CallView, IncomingCallAlertView
|   +-- Database: DatabaseView, DatabaseEncryptionView, DatabaseErrorView
|   +-- Groups: GroupChatInfoView, AddGroupMembersView, GroupMemberInfoView
|   +-- Contacts: ContactListNavView
|   +-- Remote: ConnectDesktopView, ConnectMobileView
|   +-- Terminal: TerminalView
+-- Models
|   +-- ChatModel       -- global app state (Compose MutableState singleton)
|   +-- ChatListLoadGeneration / State / Result -- typed, generation-scoped chat-list loading truth
|   +-- ChatsContext     -- per-context chat list state (primary + optional secondary)
|   +-- Chat             -- per-conversation state (chatInfo, chatItems, chatStats)
|   +-- ChatController   -- API command dispatch, event receiver, preferences
|   +-- AppPreferences   -- 150+ SharedPreferences keys
+-- Services
|   +-- NtfManager       -- abstract notification coordinator (Android/Desktop implementations)
|   +-- SimplexService   -- Android foreground service for background messaging
|   +-- ThemeManager     -- theme resolution (system/light/dark/simplex/black + per-user overrides)
|   +-- CallManager      -- WebRTC call lifecycle
+-- Platform (expect/actual)
    +-- Core.kt          -- JNI declarations (external fun), initChatController, chatInitTemporaryDatabase
    +-- AppCommon.kt     -- runMigrations, AppPlatform enum
    +-- Files.kt         -- dataDir, tmpDir, filesDir, dbAbsolutePrefixPath (expect)
    +-- Share.kt         -- shareText, shareFile, openFile (expect)
    +-- VideoPlayer.kt   -- VideoPlayerInterface, VideoPlayer (expect class)
    +-- RecAndPlay.kt    -- RecorderInterface, AudioPlayerInterface (expect)
    +-- UI.kt            -- showToast, hideKeyboard, getKeyboardState (expect)
    +-- Notifications.kt -- allowedToShowNotification (expect)
    +-- NtfManager.kt    -- abstract NtfManager class
    +-- Platform.kt      -- PlatformInterface (runtime callback object)
    +-- Cryptor.kt       -- CryptorInterface (expect)
    +-- Images.kt        -- bitmap utilities (expect)
    +-- SimplexService.kt-- getWakeLock (expect)
    +-- Log.kt, Modifier.kt, Back.kt, ScrollableColumn.kt, PlatformTextField.kt, Resources.kt
```

---

## Specification Documents

| Document | Path | Description |
|---|---|---|
| Architecture | [spec/architecture.md](architecture.md) | System layers, module structure, JNI bridge, app lifecycle, event streaming, platform abstraction |
| State Management | [spec/state.md](state.md) | ChatModel singleton, ChatsContext, Chat data class, AppPreferences, ActiveChatState |
| API | [spec/api.md](api.md) | ChatController command dispatch, ~150 API functions in 11 categories, CC/CR/API types |
| Database | [spec/database.md](database.md) | SQLite database files, migrations, encryption, backup/restore |
| Impact | [spec/impact.md](impact.md) | Source file → product concept mapping for change impact analysis |
| Chat View | [spec/client/chat-view.md](client/chat-view.md) | ChatView, ChatItemView, message rendering, item interactions |
| Chat List | [spec/client/chat-list.md](client/chat-list.md) | ChatListView, ChatPreviewView, filtering, search, tags |
| Compose | [spec/client/compose.md](client/compose.md) | ComposeView, SendMsgView, ComposeState, attachments, mentions |
| Navigation | [spec/client/navigation.md](client/navigation.md) | App screen routing, onboarding, settings, new chat flows |
| Nome Android UI | [spec/client/nome-android-ui.md](client/nome-android-ui.md) | Android-only Phase 2 foundation, Batch 2 production home, and Batch 3 P13 external-link preview with explicit ingress/fallback/evidence boundaries |
| Calls | [spec/services/calls.md](services/calls.md) | WebRTC call lifecycle, signaling, platform-specific call views |
| Files | [spec/services/files.md](services/files.md) | File transfer (SMP inline / XFTP), CryptoFile encryption, platform file paths |
| Notifications | [spec/services/notifications.md](services/notifications.md) | NtfManager, SimplexService, notification channels, background delivery |
| Theme | [spec/services/theme.md](services/theme.md) | ThemeManager, color system, wallpapers, per-user overrides |

---

## Product Documents

| Category | Path | Topic |
|---|---|---|
| Overview | [product/README.md](../product/README.md) | Product overview, capability map, navigation map |
| Concepts | [product/concepts.md](../product/concepts.md) | 32 product concepts (PC1-PC32) mapped to docs, exact source, or remaining planned boundary |
| Glossary | [product/glossary.md](../product/glossary.md) | Domain term definitions (9 sections) |
| Rules | [product/rules.md](../product/rules.md) | 20 business rules, including Nome home and external-connection truth |
| Gaps | [product/gaps.md](../product/gaps.md) | 18 numbered audit entries: 17 open gaps plus resolved historical GAP-06 |
| Flows | [product/flows/](../product/flows/) | onboarding, messaging, connection, calling, file-transfer, group-lifecycle |
| Views | [product/views/](../product/views/) | chat-list, chat, settings, onboarding, call, new-chat, contact-info, group-info, user-profiles, and the Nome Android foundation/home/P13 boundaries |

---

## Source Entry Points

| Component | File | Key Symbol |
|---|---|---|
| Android Application | [`SimplexApp.kt`](../android/src/main/java/chat/simplex/app/SimplexApp.kt#L41) | `class SimplexApp` |
| Android Activity | [`MainActivity.kt`](../android/src/main/java/chat/simplex/app/MainActivity.kt#L28) | `class MainActivity` |
| Nome Production Shell | [`NomeProductionShell.kt`](../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L17-L23) | `NomeProductionShell` |
| Nome Android Theme | [`NomeTheme.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeTheme.kt#L54-L130) | `NomeAndroidTheme`, `NomeTheme` |
| Nome Android Components | [`NomeStatePanel.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeStatePanel.kt#L28-L175) | `NomeStatePanel`, `NomeStatePanelState` |
| Nome Debug Harness | [`NomeFoundationActivity.kt`](../android/src/debug/java/chat/simplex/app/nome/harness/NomeFoundationActivity.kt#L23-L76) | `NomeFoundationActivity` (debug only, not exported) |
| Shared Home Route | [`PlatformHomeRoute.kt`](../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L15-L22) | `expect fun PlatformHomeRoute` |
| Nome Android Home Route | [`NomeHomeRoute.android.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L63-L123) | Android `actual fun PlatformHomeRoute` |
| Nome Home Truth Adapter | [`NomeHomeStateAdapter.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L29-L112) | `NomeHomeTruthInput`, `NomeHomeStateAdapter` |
| Desktop Home Fallback | [`PlatformHomeRoute.desktop.kt`](../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt#L8-L17) | Desktop `actual fun PlatformHomeRoute` |
| Desktop Entry | [`Main.kt`](../desktop/src/jvmMain/kotlin/chat/simplex/desktop/Main.kt#L22) | `fun main()` |
| Desktop App Window | [`DesktopApp.kt`](../common/src/desktopMain/kotlin/chat/simplex/common/DesktopApp.kt#L34) | `fun showApp()` |
| Desktop Init | [`AppCommon.desktop.kt`](../common/src/desktopMain/kotlin/chat/simplex/common/platform/AppCommon.desktop.kt#L21) | `fun initApp()` |
| Common App Screen | [`App.kt`](../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L48) | `fun AppScreen()` |
| JNI Bridge | [`Core.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Core.kt#L18) | `external fun initHS()` |
| Chat Controller | [`SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L510) | `object ChatController` |
| Chat List Load Contract | [`ChatModel.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106) | `ChatListLoadGeneration`, `ChatListLoadState`, `ChatListLoadResult` |
| Chat Model | [`ChatModel.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L137) | `object ChatModel` |
| Chat List Result Application | [`ChatModel.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L264-L311) | `beginChatListLoad`, `applyChatListLoadResult` |
| P13 Typed Delegates | [`SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1571-L1647) | `apiConnectPlanResult`, `apiConnectResult` |
| Shared P13 Policy/Seam | [`PlatformConnectionPreview.kt`](../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.kt#L13-L292) | safe UI model, exhaustive branch policy, `presentPlatformConnectionPreview` |
| P13 Production Opt-in | [`ChatListView.kt`](../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt#L738-L754) | `connectIfOpenedViaUri` |
| Nome Android P13 Route | [`NomeConnectionPreviewRoute.android.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewRoute.android.kt#L65-L742) | fullscreen route and content |
| Nome Android P13 Reducer | [`NomeConnectionPreviewStateAdapter.kt`](../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewStateAdapter.kt#L6-L99) | single-submit presentation state |
| Desktop P13 Fallback | [`PlatformConnectionPreview.desktop.kt`](../common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.desktop.kt#L3-L6) | declines Nome presentation |
| App Preferences | [`SimpleXAPI.kt`](../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L102) | `class AppPreferences` |
| Android Network Observation | [`NetworkObserver.kt`](../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L16-L24) | `NetworkObserver.platformNetworkInfo` |
| Platform Interface | [`Platform.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/Platform.kt#L15) | `interface PlatformInterface` |
| Notification Manager | [`NtfManager.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/NtfManager.kt#L19) | `abstract class NtfManager` |
| Theme Manager | [`ThemeManager.kt`](../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L18) | `object ThemeManager` |
| Android Haskell Init | [`AppCommon.android.kt`](../common/src/androidMain/kotlin/chat/simplex/common/platform/AppCommon.android.kt#L33) | `fun initHaskell(packageName: String)` |
| Common Migrations | [`AppCommon.kt`](../common/src/commonMain/kotlin/chat/simplex/common/platform/AppCommon.kt#L41) | `fun runMigrations()` |
| Android Service | [`SimplexService.kt`](../android/src/main/java/chat/simplex/app/SimplexService.kt#L41) | `class SimplexService` |
| Gradle Root | [`settings.gradle.kts`](../settings.gradle.kts#L21) | `include(":android", ":desktop", ":common")` |
| Common Build | [`build.gradle.kts`](../common/build.gradle.kts#L39) | `kotlin { androidTarget(); jvm("desktop") }` |
