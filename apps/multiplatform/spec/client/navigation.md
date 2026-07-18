# Navigation Specification

Sources: `common/src/commonMain/kotlin/chat/simplex/common/App.kt`, the platform home actuals, and the P13 connection-preview seam/actuals

---

## Table of Contents

1. [Overview](#1-overview)
2. [AppScreen Composable](#2-appscreen-composable)
3. [MainScreen](#3-mainscreen)
4. [Android Layout](#4-android-layout)
5. [Desktop Layout](#5-desktop-layout)
6. [ModalManager](#6-modalmanager)
7. [Authentication Gate](#7-authentication-gate)
8. [Onboarding Flow](#8-onboarding-flow)
9. [Source Files](#9-source-files)
10. [Nome Android Production Home Seam](#10-nome-android-production-home-seam)
11. [Nome Android External Connection Preview](#11-nome-android-external-connection-preview)

---

## Executive Summary

SimpleX Chat navigation is a platform-adaptive system implemented in `App.kt`. The root `AppScreen` composable applies theming and safe-area insets, delegating to `MainScreen` which acts as a state machine routing between onboarding, authentication, database error, and the main chat interface. Android uses a 2-column sliding layout (`AndroidScreen`), while desktop uses a fixed 3-column layout (`DesktopScreen`). Modal presentation is managed by `ModalManager`, which provides named zones (start, center, end, fullscreen) for layered content. Authentication is gated by `AppLock`, and onboarding follows a linear `OnboardingStage` enum.

---

## 1. Overview

```
AppScreen
+-- SimpleXTheme
    +-- Surface
        +-- MainScreen
            |-- [Migration in progress]     -> DefaultProgressView
            |-- [Database opening]          -> DefaultProgressView
            |-- [Database error]            -> DatabaseErrorView
            |-- [Encryption check pending]  -> SplashView
            |-- [Onboarding incomplete]     -> AnimatedContent { OnboardingStage views }
            |-- [Onboarding complete]
            |   |-- [Android]
            |   |   +-- AndroidWrapInCallLayout
            |   |       +-- AndroidScreen
            |   |           |-- StartPartOfScreen
            |   |           |   +-- PlatformHomeRoute (Android: Nome P07/P08)
            |   |           +-- ChatView (slide-in panel)
            |   +-- [Desktop]
            |       +-- DesktopScreen
            |           |-- StartPartOfScreen
            |           |   +-- PlatformHomeRoute (Desktop: existing ChatListView)
            |           |-- ModalManager.start (overlay on left)
            |           |-- CenterPartOfScreen / ChatView (center column)
            |           +-- ModalManager.end (right column)
            |-- [Unauthorized] -> AuthView / SplashView / PasscodeView
            |-- [Active call] -> ActiveCallView (desktop) / startCallActivity (Android)
            +-- [Incoming call] -> IncomingCallAlertView
```

---

<a id="AppScreen"></a>

## 2. AppScreen Composable

**Location:** [`AppScreen()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L48-L81)

```kotlin
@Composable
fun AppScreen()
```

### Responsibilities

1. **Theme application:** Wraps content in `SimpleXTheme` with `Surface` using `MaterialTheme.colors.background`.
2. **Window insets:** Computes safe padding for landscape mode, accounting for display cutouts on both sides. Uses `WindowInsets.safeDrawing` and `WindowInsets.displayCutout` to calculate symmetric padding.
3. **Fullscreen gallery overlay:** When `chatModel.fullscreenGalleryVisible` is true, draws a black rectangle behind content extending into the cutout areas to provide an immersive gallery background.
4. **Delegates to `MainScreen()`.**

---

<a id="MainScreen"></a>

## 3. MainScreen

**Location:** [`MainScreen()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L85-L291)

```kotlin
@Composable
fun MainScreen()
```

### State Machine

`MainScreen` evaluates a series of conditions in priority order:

| Priority | Condition | View |
|---|---|---|
| 1 | `onboarding == Step1_SimpleXInfo && migrationState != null` | `SimpleXInfo` (migration in progress) |
| 2 | `dbMigrationInProgress` | `DefaultProgressView("Database migration...")` |
| 3 | `chatDbStatus == null && showInitializationView` | `DefaultProgressView("Opening database...")` |
| 4 | `showChatDatabaseError` | `DatabaseErrorView` |
| 5 | `chatDbEncrypted == null \|\| localUserCreated == null` | `SplashView` |
| 6 | `onboarding == OnboardingComplete` | Platform-specific main screen |
| 7 | Other onboarding stages | `AnimatedContent` with stage-specific views |

### Onboarding Complete Branch

When onboarding is complete:

1. Shows "advertise lock" alert if conditions met (not shown before, LA not enabled, >3 chats, no active call).
2. Sets up clipboard listener.
3. Routes to `AndroidScreen` or `DesktopScreen` based on platform.

### Overlay Layers (bottom of MainScreen)

| Layer | Condition | Content |
|---|---|---|
| `ModalManager.fullscreen` | Android + migration/onboarding | Fullscreen modals |
| `SwitchingUsersView` | User switch in progress | Loading overlay |
| Auth gate | `userAuthorized != true` | `AuthView` or `SplashView` + passcode |
| Active call | `showCallView == true` | `ActiveCallView` (desktop) or call activity (Android) |
| One-time passcode | Always | `ModalManager.fullscreen.showOneTimePasscodeInView` |
| Privacy alerts | Always | `AlertManager.privacySensitive` |
| Incoming call | `activeCallInvitation != null` | `IncomingCallAlertView` |
| Shared alerts | Always | `AlertManager.shared` |

---

<a id="AndroidScreen"></a>

## 4. Android Layout

**Location:** [`AndroidScreen()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L311-L364)

```kotlin
@Composable
fun AndroidScreen(userPickerState: MutableStateFlow<AnimatedViewState>)
```

### 2-Column Slide Animation

Uses `BoxWithConstraints` to get `maxWidth`, then two `Box` containers:

1. **Left panel (StartPartOfScreen):** Chat list, positioned at `translationX = -offset`.
2. **Right panel (ChatView):** Chat view, positioned at `translationX = maxWidth - offset`.

The `offset` is an `Animatable<Float>`:
- `0f` when no chat is selected (chat list visible).
- `maxWidth.value` when a chat is open (chat view visible).

### Animation Flow

1. `snapshotFlow { chatModel.chatId.value }` detects chat ID changes.
2. When `chatId` becomes null, `onComposed(null)` animates offset to 0.
3. When `ChatView` finishes composing (calls `onComposed(chatId)`), offset animates to `maxWidth`.
4. Animation uses `chatListAnimationSpec()` (standard spring or tween).

### Display Cutout Handling

If the device has a display cutout on horizontal sides (detected via `WindowInsets.displayCutout`), the panels are clipped with `RectangleShape` to prevent the chat list from showing through during transition.

### Call Layout Wrapper

`AndroidWrapInCallLayout` adds a 40dp top padding when an active call is in progress (not in `WaitCapabilities` or `InvitationAccepted` state), with an `ActiveCallInteractiveArea` banner above.

---

<a id="DesktopScreen"></a>

## 5. Desktop Layout

**Location:** [`DesktopScreen()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L434-L471)

```kotlin
@Composable
fun DesktopScreen(userPickerState: MutableStateFlow<AnimatedViewState>)
```

### 3-Column Layout

| Column | Width | Content |
|---|---|---|
| **Left** | `DEFAULT_START_MODAL_WIDTH * fontSizeSqrtMultiplier` (fixed) | `StartPartOfScreen` platform home (Android Nome / Desktop `ChatListView`) + `UserPicker` overlay |
| **Left overlay** | Same as left column | `ModalManager.start` modals + `SwitchingUsersView` |
| **Center** | `min = DEFAULT_MIN_CENTER_MODAL_WIDTH`, `weight = 1f` (flexible) | `CenterPartOfScreen` (ChatView or "no selected chat" placeholder, or `ModalManager.center`) |
| **Right** | `max = DEFAULT_END_MODAL_WIDTH * fontSizeSqrtMultiplier` (flexible, 0 when empty) | `ModalManager.end` (ChatInfoView, GroupChatInfoView, ChatItemInfoView, etc.) |

### Column Separators

- `VerticalDivider` between left and center columns (always visible).
- `VerticalDivider` between center and right columns (visible when `ModalManager.end.hasModalsOpen()`).

### Click-to-Dismiss Overlay

When the UserPicker is visible or a start modal is open (but no center modal), a full-size clickable overlay covers the center+right area. Clicking it closes start modals and hides the UserPicker.

### CenterPartOfScreen

**Location:** [`CenterPartOfScreen()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L395-L425)

- When `chatId` is null and no center modals: shows "No selected chat" placeholder.
- When `chatId` is null and center modals open: shows `ModalManager.center`.
- When `chatId` is set: shows `ChatView`.
- Automatically closes center modals when a chat is selected.

### StartPartOfScreen

**Location:** [`StartPartOfScreen()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L366-L393)

Routes between:
- `SetDeliveryReceiptsView` (if `chatModel.setDeliveryReceipts` is true)
- `PlatformHomeRoute` with the existing `ChatListView` supplied as `defaultContent` (normal operation)
- `ShareListView` (when `chatModel.sharedContent` is non-null, i.e., forwarding)

---

## 6. ModalManager

**Location:** [`ModalManager`](../../common/src/commonMain/kotlin/chat/simplex/common/views/helpers/ModalView.kt#L92)

```kotlin
class ModalManager(private val placement: ModalPlacement?)
```

### Zones

| Zone | Android Behavior | Desktop Behavior |
|---|---|---|
| `start` | Shared (same as all others) | Left column overlay, slides from start |
| `center` | Shared | Center column overlay, replaces ChatView |
| `end` | Shared | Right column, slides from end |
| `fullscreen` | Shared | Fullscreen overlay |

On Android, all four zones point to the same `shared` instance, meaning modals stack in a single overlay. On desktop, each zone is independent with its own `ModalPlacement`.

```kotlin
companion object {
  val start = if (appPlatform.isAndroid) shared else ModalManager(ModalPlacement.START)
  val center = if (appPlatform.isAndroid) shared else ModalManager(ModalPlacement.CENTER)
  val end = if (appPlatform.isAndroid) shared else ModalManager(ModalPlacement.END)
  val fullscreen = if (appPlatform.isAndroid) shared else ModalManager(ModalPlacement.FULLSCREEN)
}
```

### Modal Stack

Each `ModalManager` maintains a stack of `ModalViewHolder` objects with:
- `id: ModalViewId?` -- optional identifier for deduplication
- `animated: Boolean` -- whether to use enter/exit transitions
- `data: ModalData` -- scoped data for the modal
- `modal: @Composable ModalData.(close: () -> Unit) -> Unit` -- the modal content

### Key Methods

| Method | Description |
|---|---|
| `showModal` | Push a simple modal onto the stack |
| `showModalCloseable` | Push a modal with a close callback |
| `showCustomModal` | Push a modal with full control over `ModalView` wrapper |
| `closeModals` | Pop all modals from the stack |
| `closeModalsExceptFirst` | Pop all but the bottom modal |
| `hasModalsOpen()` | Check if any modals are on the stack |
| `showInView` | Render the current modal stack into the composable tree |

### Usage Pattern

| Action | Zone Used |
|---|---|
| Settings, New Chat, User Address | `ModalManager.start` |
| Onboarding conditions, What's New | `ModalManager.center` |
| ChatInfoView, GroupChatInfoView, ChatItemInfoView, GroupMemberInfoView | `ModalManager.end` |
| Passcode entry, Call view, Migration | `ModalManager.fullscreen` |

---

<a id="AppLock"></a>

## 7. Authentication Gate

**Location:** [`AppLock`](../../common/src/commonMain/kotlin/chat/simplex/common/AppLock.kt#L17)

```kotlin
object AppLock {
  val userAuthorized = mutableStateOf<Boolean?>(null)
  val enteredBackground = mutableStateOf<Long?>(null)
  val laFailed = mutableStateOf(false)
}
```

### State

| Field | Type | Description |
|---|---|---|
| `userAuthorized` | `MutableState<Boolean?>` | `null` = not yet determined, `true` = authenticated, `false` = locked |
| `enteredBackground` | `MutableState<Long?>` | Timestamp when app entered background (for lock delay) |
| `laFailed` | `MutableState<Boolean>` | True if last authentication attempt failed |

### Authentication Flow

1. **MainScreen** checks `unauthorized` (derived: `userAuthorized.value != true`).
2. If unauthorized and not in an active call:
   - Launches `AppLock.runAuthenticate()` which triggers platform-specific biometric/passcode prompt.
   - On Android with system auth finishing during activity destruction, authentication is skipped.
3. If `performLA` preference is set and `laFailed` is true: shows `AuthView` with "Unlock" button.
4. If `performLA` is set and `laFailed` is false: shows `SplashView` with passcode overlay.

### Lock Delay

The `laLockDelay` preference controls how long after backgrounding the app requires re-authentication. When `laLockDelay == 0`, screen rotation triggers a 3-second grace period to prevent unnecessary re-auth.

### Lock Modes

- `LAMode.SYSTEM`: Uses Android biometric/system lock screen.
- `LAMode.PASSCODE`: Uses in-app passcode (`SetAppPasscodeView`).

### First-Time Lock Notice

`showLANotice` prompts users to enable SimpleX Lock when they have more than 3 chats, have not yet been shown the notice, and have not enabled lock. On Android, it offers a choice between system auth and passcode.

---

## 8. Onboarding Flow

**Location:** [`OnboardingStage`](../../common/src/commonMain/kotlin/chat/simplex/common/views/onboarding/OnboardingView.kt#L3)

```kotlin
enum class OnboardingStage {
  Step1_SimpleXInfo,
  Step2_CreateProfile,
  LinkAMobile,
  Step2_5_SetupDatabasePassphrase,
  Step3_ChooseServerOperators,
  Step3_CreateSimpleXAddress,
  Step4_SetNotificationsMode,
  OnboardingComplete
}
```

### Stage Progression

| Stage | View | Next Stage |
|---|---|---|
| `Step1_SimpleXInfo` | `SimpleXInfo` -- app introduction, privacy features | `Step2_CreateProfile` or `LinkAMobile` (desktop) |
| `Step2_CreateProfile` | `CreateFirstProfile` -- display name, optional image | `Step2_5_SetupDatabasePassphrase` or `Step3_ChooseServerOperators` |
| `LinkAMobile` | `LinkAMobile` -- desktop linking to mobile device | `Step2_CreateProfile` |
| `Step2_5_SetupDatabasePassphrase` | `SetupDatabasePassphrase` -- optional DB encryption | `Step3_ChooseServerOperators` |
| `Step3_ChooseServerOperators` | `OnboardingConditionsView` -- server operator selection, T&C | `Step3_CreateSimpleXAddress` or `Step4_SetNotificationsMode` |
| `Step3_CreateSimpleXAddress` | `SetNotificationsMode` (legacy backcompat) | `Step4_SetNotificationsMode` |
| `Step4_SetNotificationsMode` | `SetNotificationsMode` -- notification permission setup | `OnboardingComplete` |
| `OnboardingComplete` | Main app screen | -- |

### Animated Transitions

Onboarding uses `AnimatedContent` with directional transitions:
- Forward: `fromEndToStartTransition` (slide left).
- Backward: `fromStartToEndTransition` (slide right).

The stage value is stored in `appPrefs.onboardingStage` and persisted across app restarts.

---

## 9. Source Files

| File | Description |
|---|---|
| `App.kt` | AppScreen, MainScreen, AndroidScreen, DesktopScreen, StartPartOfScreen, CenterPartOfScreen, EndPartOfScreen |
| `AppLock.kt` | AppLock object, authentication state, lock notice, LA mode selection |
| `views/helpers/ModalView.kt` | ModalManager class, ModalPlacement enum, modal stack management |
| `views/onboarding/OnboardingView.kt` | OnboardingStage enum |
| `views/onboarding/SimpleXInfo.kt` | Step 1: App introduction |
| `views/WelcomeView.kt` | Step 2: Profile creation (CreateFirstProfile) |
| `views/onboarding/LinkAMobileView.kt` | Desktop: Link a mobile device |
| `views/onboarding/SetupDatabasePassphrase.kt` | Step 2.5: Database passphrase |
| `views/onboarding/ChooseServerOperators.kt` | Step 3: Server operators and conditions |
| `views/onboarding/SetNotificationsMode.kt` | Step 4: Notification setup |
| `views/chatlist/PlatformHomeRoute.kt` + platform actual | StartPartOfScreen home selection; Android Nome renderer or Desktop `ChatListView` fallback |
| `views/chatlist/PlatformHomeRoute.kt` | Narrow platform-selectable normal-home seam |
| `androidMain/.../ui/nome/home/NomeHomeRoute.android.kt` | Android actual for the Nome P07/P08 home |
| `desktopMain/.../views/chatlist/PlatformHomeRoute.desktop.kt` | Desktop actual that invokes existing content unchanged |
| `views/chatlist/UserPicker.kt` | User switching panel |
| `views/chat/ChatView.kt` | Chat view (CenterPartOfScreen content) |
| `views/database/DatabaseErrorView.kt` | Database error recovery |
| `views/SplashView.kt` | Splash / loading screen |
| `views/call/CallView.kt` | In-call fullscreen view (ActiveCallView) |
| `views/localauth/PasswordEntry.kt` | Column divider utility (contains VerticalDivider) |

---

## 10. Nome Android Production Home Seam

[`StartPartOfScreen()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L366-L393) retains its existing branch ownership. Delivery-receipt setup still wins first, shared content still routes to `ShareListView`, and only the ordinary home branch runs the existing notice effect and calls [`PlatformHomeRoute()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L16-L22). Therefore migration, database, onboarding, authentication, user-switching, call, privacy-alert, intent, modal, and share gates remain owned by the existing root.

The `commonMain` seam is the smallest sharing change that can replace the real home route without duplicating `AppScreen`, `ChatModel`, modal ownership, or navigation state:

| Source set | Implementation |
|---|---|
| `commonMain` | `expect fun PlatformHomeRoute(...)` plus the upstream `defaultContent` callback; no Nome rendering or state ownership |
| `androidMain` | [Android actual](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L63-L123) derives and renders the Nome P07/P08 home, then preserves notification setup and `UserPicker` overlay |
| `desktopMain` | [Desktop actual](../../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt#L9-L17) calls `defaultContent()` unchanged |
| `desktopTest` | [`PlatformHomeRouteDesktopTest`](../../common/src/desktopTest/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRouteDesktopTest.kt#L14-L55) executes the Desktop actual and observes fallback-content invocation |

At the Android Activity boundary, [`NomeProductionShell()`](../../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L17-L23) wraps `AppScreen`; [`MainActivity.onCreate()`](../../android/src/main/java/chat/simplex/app/MainActivity.kt#L33-L68) still owns intent processing, secure-window behavior, edge-to-edge setup, and the shared root. The shell owns no route, back stack, model, core, or protocol fact.

Batch 2 navigation is deliberately limited to opening an already-ready direct, group, or local chat through existing actions while the core is running. It adds no favorite/profile/connection mutation, search flow, composer/send flow, locale-marker route, new modal zone, new destination, or Desktop UI.

The source/test links above describe the implemented split. They do not claim a current build, `desktopTest`, Android device run, screenshot comparison, real-core run, or review gate has passed.

---

## 11. Nome Android External Connection Preview

The P13 route does not add a root destination or a second navigation stack. Its production path is:

```text
Android ACTION_VIEW
  -> MainActivity.processIntent
  -> ChatModel.appOpenUrl
  -> unchanged onboarding-complete + chatRunning gate
  -> connectIfOpenedViaUri (the only ExternalActionView opt-in)
  -> planAndConnect
  -> typed APIConnectPlan result
  -> seven eligible identity-choice branches
  -> ModalManager.fullscreen
  -> Android Nome P13 route
```

[`connectIfOpenedViaUri`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt#L738-L754)
is the only caller that passes `ExternalActionView`.
[`planAndConnect`](../../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/ConnectPlan.kt#L25-L607)
defaults to `Legacy`, so New Chat, scan/paste, link search, message links, chat preview links, and
group-member links remain unchanged. The exhaustive branch adapter offers P13 only for invitation
fresh/own, address fresh/own/repeat-request, and group fresh/repeat-join branches without
short-link preparation data. Every other plan remains with the existing upstream branch.

The Android actual uses the already-rendered `ModalManager.fullscreen` zone; it does not own root
back state. System back and visible cancel are ignored while a connect/replan command is active,
then use one idempotent cleanup closure. Retry keeps the current modal in `Replanning` while asking
the core for a fresh plan with the selected identity and current host. A typed planning failure or
missing current user returns that same modal to `Failure`; only a successful new plan or an
authoritative legacy fallback closes it as an idempotent handoff. Desktop's actual returns `false`,
so it owns no Nome route and preserves legacy presentation.

The raw URI and owner signature are closure-only and never become navigation arguments, saved
state, semantics, or evidence labels. P13 does not alter P08 `FIRST_USE` or
`FILTERED_NO_RESULT`, create a P10 home control, implement P12 camera behavior, or absorb P16 group
details.
