# Nome Android UI Specification

> **Status:** verified Android-only Phase 2 foundation plus frozen Batch 2 production shell/P07/P08 home. Batch 3 implements only the authorized P13 external-link preview source; its execution/review status is owned exclusively by its evidence root and is not asserted here. `FIRST_USE` and `FILTERED_NO_RESULT` remain deferred production-reachability branches; P09–P12 and P14–P24 remain outside Batch 3.
> **Product view:** [product/views/nome-android.md](../../product/views/nome-android.md)
> **Coverage matrix:** [plans/20260716_03.md](../../../../plans/20260716_03.md)

## 1. Scope

This document defines the Android-only presentation boundary for Nome on the official SimpleX Chat v6.5.6 Kotlin Multiplatform client.

In scope:

- Android shell, root presentation, navigation adaptation, and system insets/back behavior;
- Nome tokens and reusable Compose components;
- `zh-CN`/English locale policy;
- P01–P24 and all reachable Android secondary surfaces;
- explicit state adapters over existing model/API/core truth;
- screenshot, accessibility, device, and upgrade verification.
- a Nome release minimum of Android API 28; API 26–27 are unsupported.

Out of scope:

- Haskell/native core changes;
- command, event, archive, migration, or database-format changes;
- iOS implementation;
- broad Desktop redesign;
- production package ID, signing, Nome domain/App Links, and store-release decisions.

## 2. Architectural boundary

```
Android activity, intents, services, permissions
                    |
Nome Android shell / navigation / platform adaptation
                    |
Nome tokens/components + P07/P08 semantic state adapter
                    |
existing Compose flows and platform adapters
                    |
ChatModel + ChatController + AppPreferences
                    |
official v6.5.6 JNI/native core and local database
```

Rules:

1. The official model/API/core layers remain authoritative.
2. Nome presenters/state adapters may combine existing facts into display state but cannot create a protocol fact. Batch 2 adds only the P07/P08 adapter described in §8.
3. Root state precedence in `App.kt` remains unchanged.
4. Android-specific presentation stays in Android-controlled paths by default. Batch 2's narrow `PlatformHomeRoute` `expect` declaration is the recorded exception needed at the existing shared home selection point; its Desktop actual invokes the upstream content unchanged and has a `desktopTest` contract.
5. Preview fixtures and screenshot fixtures stay outside production state paths.
6. A missing capability is a documented GAP, not a reason to call or alter the native core from a new path.

## 3. State adapter types

The implementation should make the capability classification visible in types and tests:

| Type | Meaning | Permitted behavior |
|---|---|---|
| `CoreBacked` | Direct field, response, or event | Render, format, and localize without changing semantics. |
| `Derived` | Pure function of one or more core-backed/client-backed facts | Derivation must be deterministic and unit-tested; label the evidence level precisely. |
| `ClientOperation` | UI-owned in-flight/retry/validation state around an existing API | May track submission lifecycle but must reconcile with API/core outcome. |
| `Unavailable` | Required design state lacks a truthful source | Disable/remove/reword through the approved GAP decision; never synthesize it. |

Examples:

- A local text-field validation error is `ClientOperation`.
- “Device has Wi-Fi” can be `CoreBacked`/platform-backed; “private routing healthy” is not derived from it.
- “Invitation copied” is client state; “invitation used by another device” requires a connection event.
- A channel non-E2EE warning is a fixed product disclosure based on the channel mode; it is not an encryption-success badge.

`ClientOperation` is single-submit and may become terminal only from a typed platform/API result or reconciliation with the authoritative model. It must retain the affected input/model on failure. After a destructive boundary it records the exact completed/failed phase rather than exposing a generic rollback or local success flag. None of these adapter types may turn an `Unavailable`/C capability into a production fact.

## 4. Root routing contract

Nome may provide Android-specific content for the existing branches but must preserve this order:

| Priority | Existing condition family | Nome page/reference |
|---|---|---|
| 1 | migration already in progress | P01/P24 |
| 2 | database migration/opening | P01 |
| 3 | database error/recovery | P01/P24 |
| 4 | encryption and first-user readiness | P01/P02 |
| 5 | onboarding stage | P03–P06 |
| 6 | onboarding complete/main shell | P07 onward |
| overlay | authentication, user switching, calls, privacy alerts, system intents | P02 plus matching destination |

The shell must preserve external share intent, deep-link, notification action, foreground/background, and call behavior from `MainActivity` and the existing services.

## 5. Design-system boundary

The first source batch defines the reusable subset needed before page composition:

- semantic colors for light/dark, including public-channel disclosure and destructive states;
- Chinese-first and English-stress typography with system fallback;
- spacing, radius, elevation, divider, and scrim tokens;
- status/icon semantics without repurposing a lock or check badge;
- a parameterized surface, 48dp button, and seven-state panel covering normal/loading/empty/offline/error/permission/danger without embedding product truth.

Batch 2 composes only the production P07/P08 home header, read-only chat rows, skeleton, and explicit non-populated/status panels. Page-level navigation bars, sheets, dialogs, fields, message/delivery, relay, identity management, migration, and all other presenters/state adapters remain later production batches; they are not implied by either the foundation fixture or this home renderer.

The current app uses Compose Material 2 APIs. Nome can implement Material 3-style behavior and tokens without migrating the entire codebase to Material 3 in the first release.

The 24 approved Android PNGs are the light visual baseline. **Nome Dark Token v1** is frozen through P02, P07, P17, P21, and P23. It preserves the same information architecture, semantic meaning, copy priority, component geometry, spacing, typography scale, icon size, state set, 48dp targets, and safe-area behavior. Only surface tiers, contrast, scrim/border, and elevation treatment may be remapped. The five deterministic native foundation representatives have passed screenshot comparison, contrast, 200% font, 48dp, and TalkBack-semantics verification. Expansion into a production page still requires that page's real-state/device evidence.

## 6. Locale contract

The locale policy requires a versioned one-time initialization marker; implementation details must be added to `spec/state.md` when the later locale/state-adapter batch starts. The current design foundation does not read or write application language preferences.

Required decision table. “First Nome run” is not sufficient evidence of a fresh install because an upgraded v6.5.6 user can still have `appLanguage == null`:

| Existing preference | Nome marker / installation evidence | Result |
|---|---|---|
| no explicit upstream language (`appLanguage == null`) | marker absent; installation metadata **and** absence of existing app data/user jointly prove a clean install | initialize `zh-CN`, then write the Nome locale marker |
| no explicit upstream language (`appLanguage == null`) | marker absent; existing database, user, preference, upgrade, backup restore, or system restore evidence exists | preserve `null`/follow-system behavior; write only the marker |
| no explicit upstream language (`appLanguage == null`) | marker absent; fresh-versus-existing evidence is unknown or contradictory | conservatively treat as existing: preserve `null`/follow-system behavior; write only the marker and a non-personal recoverable diagnostic |
| explicit `zh-CN` | marker absent or present | preserve; write the marker if absent |
| explicit English | marker absent or present | preserve; write the marker if absent |
| another valid upstream locale | marker absent or present | preserve during migration; product UI may state that launch QA covers only Chinese/English; write the marker if absent |
| corrupt/unsupported preference value | marker absent or present | safely fall back without crash or data overwrite; record a non-personal recoverable diagnostic; never reinterpret this alone as clean-install evidence |
| Nome locale marker already set | later run | never overwrite the user's current choice |

“Follow system” maps Chinese system locales to the supported Chinese resource set and all other system locales to complete English for the Nome launch contract.

## 7. Capability mapping

The authoritative row-level map is `plans/20260716_03.md`. The principal adapters are:

| Domain | Existing sources | Adapter responsibility | Forbidden inference |
|---|---|---|---|
| startup/database | root state, database/migration status and errors | semantic phases, indeterminate progress, retry routing | fake percentage, restore/rollback guarantee |
| local auth | `AppLock`, passcode, biometric result | method availability and recoverable action | silently treating unavailable auth as success |
| onboarding | `OnboardingStage`, create-user/operator/notification commands | approved information architecture while preserving order | fake account/server registration |
| chat list/search | chats, tags, unread, load/core/network state, search results | grouped display and exact empty/loading distinction | presence, global result aggregation that was never queried |
| connection | parsed plan, request/link/address commands and events | route-specific state and source warnings | invitation TTL/use or verified admin identity without data |
| messaging | chat items, compose, delivery/file/voice states | Nome message and action presentation | optimistic delivery/read status |
| channel | role, membership, relay status/events, item-level `E2EEInfo.public` events | owner/observer/relay views, exact security-event rendering, and fixed disclosure | persistent aggregate E2EE or relay health not proven by relay/network/event history |
| identity/settings | users, hidden/incognito, preferences | local identity shell, local settings index and evidence-based badges | persistent anonymous account semantics |
| archive/migration | archive commands, migration states/events | truthful operation stages and recovery action | cancellation, resume, rollback not guaranteed by API |

### Recorded GAP resolution

The 2026-07-17 delegated decision adopts the recommended route for GAP-08 through GAP-17 and chooses minSdk 28 for GAP-18:

- GAP-08 removes unsupported percentage/generic restore/rollback/core-timeout claims and makes startup, empty, offline, stopped, and unknown mutually explicit;
- GAP-09 fails closed and exposes only configured recovery; no lockout countdown exists until its client state contract is specified;
- GAP-10 uses single-submit typed client operations and Android Settings recovery without adding native commands;
- GAP-11 uses labelled local scopes and a stable local settings index, with no recent-query persistence or complete global-message claim;
- GAP-12 removes TTL/expiry/atomic-regenerate claims and maps peer use/connection only from events;
- GAP-13 removes request message, preserves typed reject/address outcomes, and makes address replacement two explicit operations;
- GAP-14 removes verified-admin, direct-report, and Nome-domain claims;
- GAP-15 keeps incognito per connection and outside the persistent identity list;
- GAP-16 limits determinate progress, cancel, resume, and completion to proven stages and provides no rollback guarantee;
- GAP-17 keeps connectivity, configuration, validation, relay, and item security evidence at separate levels;
- GAP-18 makes API 26–27 unsupported and does not authorize a native rebuild or source patch.

These decisions do not change the A/B/C classification in `plans/20260716_03.md`. A C row remains unavailable; the first release either removes/rewords it or waits for a later authorized adapter/capability.

## 8. Implemented source placement

The frozen Phase 2 foundation, Batch 2 production home, and authorized Batch 3 P13 use these
placements:

| Responsibility | Source placement | Constraint |
|---|---|---|
| tokens, theme, components, accessibility primitives | `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/{tokens,theme,components,accessibility}/` | Android-only foundation; no presenter, model, protocol, or API truth |
| Activity/system host integration | `android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt` | wraps the unchanged shared root; owns no navigation/model/core truth |
| shared home seam | `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt` | declaration only; narrow sharing reason recorded below |
| Android P07/P08 production home | `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/{NomeHomeStateAdapter.kt,NomeHomeRoute.android.kt}` | Android-only derivation/rendering over official facts |
| Desktop fallback | `common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt` | invokes upstream `defaultContent` unchanged |
| production bilingual copy | `common/src/androidMain/res/{values,values-zh-rCN}/nome_home_strings.xml` | English and Simplified-Chinese home copy/semantics only |
| fixture, screenshot harness, Preview | `android/src/debug/java/chat/simplex/app/nome/` | deterministic debug-only data; never a production state source |
| JVM/unit tests | `android/src/test/java/chat/simplex/app/nome/` | frozen foundation contracts plus P07/P08 pure truth table |
| instrumentation/device tests | `android/src/androidTest/java/chat/simplex/app/nome/` | frozen foundation suites plus P07/P08 renderer semantics/48dp/200% coverage and an explicit argument-gated real-core stop/start evidence gate |
| Desktop regression test | `common/src/desktopTest/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRouteDesktopTest.kt` | guards unchanged Desktop fallback |
| Batch 2 renderer evidence host | `android/src/debug/java/chat/simplex/app/nome/home/NomeHomeEvidenceActivity.kt` plus debug manifest/resources | non-exported deterministic host, visibly marked as not live core |
| Batch 2 screenshot definition | `android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeScreenshotTest.kt` | API 35, 10 states × 2 locales × 2 themes × 2 font scales = 80 verified captures |
| shared P13 policy and safe model | `common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.kt` | display-safe model, exhaustive 7/21 branch policy, callbacks, and expect declaration; bearer values are closure-only |
| typed P13 controller delegates | `common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt` | unchanged plan/connect commands, typed outcomes, no UI ownership, and P13 terminal logging disabled |
| Android P13 presenter | `common/src/androidMain/kotlin/chat/simplex/common/{views/newchat/PlatformConnectionPreview.android.kt,ui/nome/connection/**}` | fullscreen modal actual, pure reducer, bilingual Nome renderer |
| P13 production resources | `common/src/androidMain/res/{values,values-zh-rCN}/nome_connection_preview_strings.xml` | English/Simplified-Chinese page and semantic copy |
| Desktop P13 fallback | `common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.desktop.kt` | returns `false`; no Desktop Nome UI or behavior change |
| P13 debug evidence host | `android/src/debug/java/chat/simplex/app/nome/connection/NomeConnectionPreviewEvidenceActivity.kt` plus debug resources/manifest | non-exported deterministic renderer host, visibly fixture-only |
| P13 tests | `common/src/commonTest/.../connection/`, `android/src/{test,androidTest}/.../nome/connection/`, and `common/src/desktopTest/.../views/newchat/` | branch/fallback/context/failure, reducer, route boundary, semantics/48dp/200%, packaging, screenshot matrix, and Desktop fallback |

The sharing reason is route ownership: `StartPartOfScreen` is shared and already decides between delivery-receipt setup, normal home, and share content. Moving or copying that decision into the Android app module would create a second root/navigation truth and would miss non-Activity `AppScreen` entry paths. The `commonMain` seam therefore carries only arguments and `defaultContent`; it contains no Nome UI. The Desktop actual and test preserve the official Desktop `ChatListView`.

Batch 2 adds generation-scoped load/result types around the existing get-chats API, not a second API or native command. Batch 3 adds typed siblings around the already-shared plan/connect commands because command ownership and the seven branching call sites already live in `commonMain`. The sharing carries no Nome composable and no new native call. Its explicit Desktop actual/test declines presentation. Any later typed adapter still requires its own recorded sharing reason and may not bypass the controller to call the native core.

### P07/P08 typed truth

| Fact family | Source | Production meaning |
|---|---|---|
| load generation | `(remoteHostId, userId)` from `ChatModel.currentChatListGeneration()` | A result or cached row is current only while this generation matches. |
| load lifecycle | `ChatListLoadState` + `ChatListLoadResult` | `Initial`, `Loading`, `Loaded`, `Unavailable`, and `NoCurrentUser` remain distinguishable; failure/no user are not empty success. |
| connectivity | nullable `NetworkObserver.platformNetworkInfo` | `null` is initial unknown; `online == false` is device offline only, not relay/service/private-routing health. |
| core | nullable `chatRunning` | starting/running/stopped is orthogonal to list and connectivity state. |
| list projection | official chats + existing active tag filter | preserves core order and underlying list; Batch 2 adds no search/filter control or list mutation, so a fresh production home has no producer for a non-null filter. |

The pure adapter produces `LOADING`, `FIRST_USE`, `TRUE_EMPTY`, `FILTERED_NO_RESULT`, `POPULATED`, or `UNAVAILABLE`; connectivity produces `UNKNOWN`, `ONLINE`, or `DEVICE_OFFLINE`; core produces `STARTING`, `RUNNING`, or `STOPPED`. Switching identity/host and generation mismatch hide stale rows. True empty requires a matching-generation typed success. A matching-generation failure can keep cached rows readable with an unavailable panel, and stopped/offline remain independent overlays rather than empty-list aliases. The adapter state set is broader than current production reachability: onboarding consumes no-user before the home route, and this bounded home has no active-filter producer. `FIRST_USE` and `FILTERED_NO_RESULT` are therefore defensive renderer/test contracts, not passed production-route claims.

P07 rows are read-only with respect to list state. They show existing name/type/timestamp/unread/favorite facts and show message preview text, including spoken preview semantics, only when the existing `showChatPreviews` privacy preference permits it. They provide no favorite, mark-read, mute, delete, tag, profile-switch redesign, connection mutation, search, or filter action. Existing navigation may open a ready direct/group/local chat only while the core is running and the row is not pending deletion; an unavailable panel does not by itself disable that navigation. P08 production-reachable paths cover skeleton/loading, true empty, unavailable, initial network unknown, device offline, and core stopped. First use and filtered no result remain defensive renderer branches with open production reachability gates.

Batch 2 does not implement the locale marker, connection flow, composer/send, other P pages, protocol/core/database-format changes, or a broad Desktop redesign.

### P13 typed truth and route boundary

The only production opt-in is
[`connectIfOpenedViaUri`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt#L738-L754).
[`planAndConnect`](../../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/ConnectPlan.kt#L25-L607)
defaults every other caller to `Legacy`. The safe adapter normalizes all 21 current plan branches
and marks exactly seven eligible: invitation fresh/own, address fresh/own/repeat-request, and group
fresh/repeat-join, with fresh branches requiring absent short-link preparation data.

The reducer phases are `Ready`, `Connecting`, `Failure`, `Replanning`, and `Pending`. Identity may
change only in `Ready`. The route reducer ignores duplicate submits and out-of-order results, while
the controller closure supplies an atomic guard across recomposition. Only
`SentConfirmation`/`SentInvitation` with a real pending connection reaches `Pending`.
Already-existing contact, sanitized API failure, no user, and changed user/host remain failures.
Retry retains the chosen identity and the old modal in `Replanning` while resolving the current
host and performing a new plan. Planning failure or no current user returns the same modal to
`Failure`; only a successful or authoritative fallback plan closes it as a handoff before any
connect.

The display model never contains the raw URI, `CreatedConnLink`, or `LinkOwnerSig`; those remain in
an ephemeral controller closure. Both P13 commands use `sendCmd(..., log = false)`. The route
disables submit, visible back, and cancel during active send/replan, and uses one idempotent cleanup
for cancel, system dismiss, configuration disposal, and handoff. Desktop returns `false` from the
actual and retains legacy presentation.

P13 does not add a P10 home entry, implement P12 scan/paste/camera, mutate P14 requests, or render
P16 group details. It does not alter `FIRST_USE` or `FILTERED_NO_RESULT`.

### Source anchors

- semantic palette: [`NomeColorTokens`, light/dark values](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeColorTokens.kt#L7-L131);
- dimensions and minimum target: [`NomeDimensionTokens`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeDimensionTokens.kt#L8-L46);
- elevation, shapes, and typography: [`NomeElevationTokens`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeElevationTokens.kt#L8-L20), [`NomeShapeTokens`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeShapeTokens.kt#L10-L30), [`NomeTypographyTokens`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeTypographyTokens.kt#L11-L89);
- Material 2 adapter and composition locals: [`NomeTheme` / `NomeAndroidTheme`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeTheme.kt#L54-L130);
- reusable primitives: [`NomeButton`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeButton.kt#L16-L88), [`NomeStatePanel`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeStatePanel.kt#L28-L175), [`NomeSurface`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeSurface.kt#L13-L31);
- accessibility modifiers: [`nomeMinimumTouchTarget` and TalkBack semantics](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/accessibility/NomeAccessibility.kt#L18-L35);
- deterministic debug entry: [`NomeFixtureSpec`](../../android/src/debug/java/chat/simplex/app/nome/fixtures/NomeFixtureSpec.kt#L7-L93), [`NomeFoundationFixture`](../../android/src/debug/java/chat/simplex/app/nome/fixtures/NomeFoundationFixtures.kt#L54-L822), [`NomeFoundationActivity`](../../android/src/debug/java/chat/simplex/app/nome/harness/NomeFoundationActivity.kt#L23-L76), and [`NomeDesignSystemPreviews`](../../android/src/debug/java/chat/simplex/app/nome/preview/NomeDesignSystemPreviews.kt#L20-L111);
- contract/device evidence: [`NomeFoundationContractTest`](../../android/src/test/java/chat/simplex/app/nome/NomeFoundationContractTest.kt#L8-L20), [`NomeAndroidPackagingTest`](../../android/src/androidTest/java/chat/simplex/app/nome/NomeAndroidPackagingTest.kt#L15-L47), [`NomeFoundationComposeTest`](../../android/src/androidTest/java/chat/simplex/app/nome/NomeFoundationComposeTest.kt#L53-L513), and [`NomeFoundationScreenshotTest`](../../android/src/androidTest/java/chat/simplex/app/nome/NomeFoundationScreenshotTest.kt#L20-L267).
- shared/host split: [`StartPartOfScreen()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L366-L393), [`PlatformHomeRoute()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L16-L22), [`NomeProductionShell()`](../../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L17-L23), [Android actual](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L63-L123), and [Desktop fallback](../../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt#L9-L17);
- typed load/connectivity facts: [`ChatListLoadGeneration`, `ChatListLoadState`, and `ChatListLoadResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106), [`applyChatListLoadResult()`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L264-L311), [`apiGetChatsResult()`](../../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081), and [`platformNetworkInfo`](../../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L18-L24);
- P07/P08 adapter and renderer: [`NomeHomeStateAdapter.derive()`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L49-L121) and [`NomeHomeRouteContent()`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L125-L280);
- Batch 2 contract tests: [`NomeHomeStateAdapterTest`](../../android/src/test/java/chat/simplex/app/nome/home/NomeHomeStateAdapterTest.kt#L16-L242), [`NomeHomeComposeTest`](../../android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeComposeTest.kt#L53-L562), [`NomeHomePackagingTest`](../../android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomePackagingTest.kt#L13-L38), and [`PlatformHomeRouteDesktopTest`](../../common/src/desktopTest/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRouteDesktopTest.kt#L14-L55);
- explicit real-core evidence gate: [`NomeHomeCoreCycleTest`](../../android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeCoreCycleTest.kt#L1-L90) runs only with `nomeCoreCycle=true`, invokes the official stop/start paths, and asserts that the exact non-empty cached chat-ID sequence survives both transitions;
- Batch 2 verified renderer captures: [`NomeHomeEvidenceActivity` and ten evidence states](../../android/src/debug/java/chat/simplex/app/nome/home/NomeHomeEvidenceActivity.kt#L45-L253), [debug-only manifest entry](../../android/src/debug/AndroidManifest.xml#L5-L16), and [`NomeHomeScreenshotTest`](../../android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeScreenshotTest.kt#L18-L177).
- P13 typed truth and branch tests: [`ConnectionPreviewPolicyTest`](../../common/src/commonTest/kotlin/chat/simplex/common/views/newchat/ConnectionPreviewPolicyTest.kt#L20-L189), [`NomeConnectionPreviewStateAdapterTest`](../../android/src/test/java/chat/simplex/app/nome/connection/NomeConnectionPreviewStateAdapterTest.kt#L12-L91), and [`ConnectionPreviewRouteBoundaryTest`](../../android/src/test/java/chat/simplex/app/nome/connection/ConnectionPreviewRouteBoundaryTest.kt#L8-L67);
- P13 Android/desktop presentation: [Android actual](../../common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.android.kt#L9-L24), [route/content](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewRoute.android.kt#L65-L742), [Desktop actual](../../common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.desktop.kt#L3-L6), and [`PlatformConnectionPreviewDesktopTest`](../../common/src/desktopTest/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreviewDesktopTest.kt#L6-L29);
- P13 renderer evidence definition: [`NomeConnectionPreviewEvidenceActivity` and nine fixture states](../../android/src/debug/java/chat/simplex/app/nome/connection/NomeConnectionPreviewEvidenceActivity.kt#L34-L251), [`NomeConnectionPreviewComposeTest`](../../android/src/androidTest/java/chat/simplex/app/nome/connection/NomeConnectionPreviewComposeTest.kt#L33-L228), [`NomeConnectionPreviewPackagingTest`](../../android/src/androidTest/java/chat/simplex/app/nome/connection/NomeConnectionPreviewPackagingTest.kt#L10-L31), and [`NomeConnectionPreviewScreenshotTest`](../../android/src/androidTest/java/chat/simplex/app/nome/connection/NomeConnectionPreviewScreenshotTest.kt#L17-L123), whose matrix is 9 states × 2 locales × 2 themes × 2 font scales = 72 captures.

## 9. Accessibility contract

Required for every applicable state:

- semantic role and accessible name;
- state/error description and live announcement for async changes;
- logical TalkBack traversal and focus restoration after sheets/dialogs;
- 48dp target independent of visual glyph size;
- 200% font without clipped actions, hidden error recovery, or overlapping navigation;
- contrast verified for both themes;
- no color-only distinction for unread, delivery, verification, relay, or destructive states;
- IME action, keyboard dismissal, permission denial, back, and predictive-back tests.

UIAutomator node counts are only a smoke check. This foundation verifies the TalkBack-facing Compose semantics/live-region contract on device; each later production page still requires traversal and focus-restoration inspection in its real navigation context.

## 10. Test and evidence contract

Each implementation batch must include, as applicable:

1. unit tests for foundation contracts and, when added, presenters/derivations and locale initialization;
2. Compose UI tests for state, semantics, focus, back, and destructive confirmation;
3. Android instrumentation/device tests for permission, intent, lifecycle, service, and native-core behavior;
4. `:android:testDebugUnitTest`, `:android:lintDebug`, and `:android:assembleDebug`;
5. `desktopTest` whenever `commonMain` changes;
6. `zh-CN/en × light/dark` native screenshots for every applicable state;
7. 200% font and TalkBack evidence;
8. comparison to the approved P0 page or named P0 reference component;
9. a real model/API/core trace or a documented pure-display derivation;
10. two consecutive zero-issue adversarial review rounds after the last fix.

The same `.nome.dev` package and debug signing identity covered this foundation's upgrade smoke: a non-empty v6.5.6 snapshot was restored, the minSdk 28 build installed with `-r`, the chat database and preferences remained byte-identical at the immediate checkpoint, and the real core cold-started into the existing Chinese connection flow. The agent database is runtime-mutable and is not claimed byte-identical. Later production adapters must add deeper identity/chat/attachment/settings/language assertions proportional to what they change.

The Batch 2 test files and source anchors in §8 define the implementation contract. The independent evidence root contains the Gradle, Desktop, API 28/API 35 real-core, bilingual-light-dark-100%-200%, 48dp/TalkBack, P07/P08 comparison, release-isolation, threat-delta, and manifest execution artifacts. This specification does not itself claim the formal two-round outcome; only the evidence root's `review-rounds.md` owns that status.

The Batch 3 P13 evidence root is
`plans/evidence/20260717_nome_android_phase2_batch3_p13/`. Its debug fixture matrix proves only the
renderer. API 28/API 35 external-intent traces, two-client connection/identity truth, raw-link
Logcat/UI/evidence scans, lifecycle/single-submit checks, manual TalkBack, release isolation, and
two same-digest reviews must be recorded separately before the batch is called complete.

## 11. Current implementation checkpoint

Completed at the immutable Phase 0/1 checkpoint:

- exact v6.5.6 tag/commit and design freeze;
- exact release APK/native provenance, hashes, APK signatures, ABI and ELF inspection;
- unit tests, Desktop tests, Android lint/build/install;
- API 35 cold launch and real native server/receiver startup;
- real synthetic local-user creation;
- English/Chinese light/dark native screenshots and semantic trees;
- non-empty AVD snapshot for the same-package upgrade test.

Completed in the Phase 2 foundation batch:

- GAP-08 through GAP-17 use the recorded recommended route; GAP-18 selects minSdk 28 and makes API 26–27 unsupported;
- Dark Token v1 uses P02/P07/P17/P21/P23 with unchanged IA/semantics/dimensions and surface/contrast/elevation remapping only;
- Android-only tokens/theme/surface/button/state-panel/accessibility source in §8, with no presenter or `commonMain` Nome UI;
- a non-exported debug-only bilingual/light-dark fixture, Preview, and API 35 native screenshot harness;
- all seven applicable foundation states, 200% font, 48dp, TalkBack semantics/live regions, and measured light/dark contrast;
- Gradle/merged-manifest/APK minSdk 28, clean API 28 real-core startup, API 35 same-package non-empty upgrade/core startup, and release-manifest exclusion of the debug harness;
- 96 hashed native screenshots, five light-reference side-by-side/overlay comparisons, dark/state/200% contact sheets, and a batch-specific threat-model delta.

Implemented and execution-gated in Phase 2 Batch 2:

- Android Activity host around the unchanged shared root;
- narrow shared home seam with unchanged Desktop fallback;
- generation-scoped typed load results and nullable first Android connectivity observation;
- production P07 populated/read-only rows and production-reachable P08 loading/true-empty/unavailable/unknown/offline/stopped rendering, plus defensive first-use/filtered-no-result renderer branches whose production reachability remains open;
- additive unit, Compose, and Desktop fallback tests;
- API 28/API 35 real-core/device execution, same-package non-empty upgrade, network/core/generation/cancellation truth, 80 renderer screenshots, accessibility/TalkBack traversal, and release isolation. Formal frozen-digest review status is owned only by the evidence root's `review-rounds.md` and is not asserted here.

Implemented in Phase 2 Batch 3 source, with execution status delegated to its evidence root:

- one explicit Android external-`ACTION_VIEW` opt-in and a `Legacy` default for all six other caller surfaces;
- exhaustive seven-eligible/fourteen-fallback plan policy, safe model, and Desktop-declining expect/actual seam;
- typed non-logging plan/connect delegates over unchanged core commands;
- Android P13 ready/current/incognito/warning/connecting/pending/failure/retry/cancel route and pure reducer;
- bilingual resources, light/dark tokens, 48dp and 200% Compose coverage, nine-state/72-capture debug screenshot definition, non-exported debug host, release packaging guard, and Desktop fallback test;
- no change to `FIRST_USE`, `FILTERED_NO_RESULT`, P09–P12, P14–P24, native core, protocol, database, message state, iOS, or Desktop UI.

Remaining implementation/evidence gates:

- implement the one-time locale marker without overwriting existing-user language behavior;
- complete the P13 evidence root's device, real-core/two-client, native comparison, manual accessibility, release isolation, threat-delta, and two-review gates;
- add exact, release-isolated P13 command-count evidence alongside the device and lifecycle traces;
- implement P09–P12 and P14–P24 production pages only in separately authorized small batches;
- repeat bilingual/theme/applicable-state/accessibility/core/screenshot/threat-model/two-review gates for every connected production batch;
- add deeper upgrade assertions when a later batch touches identities, chats, attachments, settings, or locale persistence.
