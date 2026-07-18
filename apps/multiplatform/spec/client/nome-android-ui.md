# Nome Android UI Specification

> **Status:** Foundation, P07/P08, P13, and Milestone 1 P01–P06 are frozen. The targeted P02–P06
> post-clarification visual recheck and P09/P10 ordinary UI group are closed; the Milestone 2
> concentrated gate/checkpoint is pending. `FIRST_USE` remains official onboarding/root-owned.
> `FILTERED_NO_RESULT` is reachable only from P09's real nonblank query producer and is not a
> retroactive P08 claim. P11–P12 and P14–P24 remain pending.
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
Nome tokens/components + bounded P01/P07–P10/P13 adapters
                    |
existing Compose flows and platform adapters
                    |
ChatModel + ChatController + AppPreferences
                    |
official v6.5.6 JNI/native core and local database
```

Rules:

1. The official model/API/core layers remain authoritative.
2. Nome presenters/state adapters may combine existing facts into display state but cannot create a protocol fact. Each implemented adapter and its bounded sharing reason are recorded in §8.
3. Root state precedence in `App.kt` remains unchanged.
4. Android-specific presentation stays in Android-controlled paths by default. The narrow `PlatformHomeRoute`, `PlatformDatabaseRootRoute`, and P13 connection-preview seams are the recorded exceptions at existing shared route-selection points; every Desktop actual declines or invokes upstream content unchanged and has a `desktopTest` contract.
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

The frozen Batch 2 composes P07/P08 Home. Milestone 2 adds the P09 search route and P10 New Chat
hub through the same Android-only tokens and platform-presentation pattern. These pages do not
imply message aggregation, connection success, relay, identity mutation, migration, or later-page
state adapters.

The current app uses Compose Material 2 APIs. Nome can implement Material 3-style behavior and tokens without migrating the entire codebase to Material 3 in the first release.

The 24 approved Android PNGs are page-level light-theme visual acceptance baselines. Shared
tokens/components do not close a page by themselves: every completed page family requires an API
35 primary production capture at the reference viewport/language, a same-size side-by-side
comparison, and correction of obvious differences in composition, region placement, hierarchy,
color, typography, spacing, corners, icons, action size, and density. The official v6.5.6
route/state/action/data semantics override unsupported facts in a reference. **Nome Dark Token
v1** is frozen through P02, P07, P17, P21, and P23. It preserves the same information architecture,
semantic meaning, copy priority, component geometry, spacing, typography scale, icon size, state
set, 48dp targets, and safe-area behavior. Only surface tiers, contrast, scrim/border, and elevation
treatment may be remapped. Full bilingual/light-dark/API/200%/TalkBack coverage is concentrated at
milestones and final RC.

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
| startup/database | existing root state, typed migration/open errors, Android database-alias key-read class, and existing recovery actions | exhaustive display-safe phases, single-submit recovery, exact backup-pair copy outcome | fake percentage, restored-data claim before exact copy success, restore/rollback guarantee, bearer/raw diagnostic |
| local auth | `AppLock`, passcode, biometric result | method availability and recoverable action | silently treating unavailable auth as success |
| onboarding | `OnboardingStage`, create-user/operator/notification commands | page-baseline information architecture while preserving official order/owners | fake account/server registration, operator health, or unsafe post-create back |
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

The frozen foundation/P07–P08/P13/P01–P06 work and active P09/P10 group use these placements:

| Responsibility | Source placement | Constraint |
|---|---|---|
| tokens, theme, components, accessibility primitives | `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/{tokens,theme,components,accessibility}/` | Android-only foundation; no presenter, model, protocol, or API truth |
| Activity/system host integration | `android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt` | wraps the unchanged shared root; owns no navigation/model/core truth |
| shared P01 root seam and actions | `common/src/commonMain/kotlin/chat/simplex/common/views/database/{PlatformDatabaseRootRoute.kt,DatabaseErrorView.kt}` | existing root facts/actions only; no new database command, migration, or presentation truth |
| Android P01 key-read classification | `common/src/androidMain/kotlin/chat/simplex/common/platform/Cryptor.android.kt` | database-alias-only missing/unreadable class and fixed non-secret diagnostic; no key/passphrase value |
| Android P01 production route | `common/src/androidMain/kotlin/chat/simplex/common/{views/database/PlatformDatabaseRootRoute.android.kt,ui/nome/database/**}` | exhaustive safe state, process attempt owner, source-bound recovery presentation, and renderer |
| P01 production copy | `common/src/androidMain/res/{values,values-zh-rCN}/nome_database_root_strings.xml` | fixed English/Simplified-Chinese copy; never interpolates raw native errors or secret material |
| Desktop P01 fallback | `common/src/desktopMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.desktop.kt` | invokes the supplied official content exactly once |
| P01 tests | `android/src/{test,androidTest}/.../nome/database/` and `common/src/desktopTest/.../views/database/` | exhaustive reducer/boundary/attempt, Compose semantics/lifecycle, and Desktop one-call contracts |
| P01 debug evidence host | `android/src/debug/java/chat/simplex/app/nome/database/NomeDatabaseRootEvidenceActivity.kt` plus paired debug resources/manifest | non-exported deterministic renderer reference, visibly marked as not live database |
| P01 affected screenshot definition | `android/src/androidTest/java/chat/simplex/app/nome/database/NomeDatabaseRootScreenshotTest.kt` | bilingual/theme/font renderer reference only; real fixtures own database truth |
| Android P02 production lock presentation | `common/src/androidMain/kotlin/chat/simplex/common/views/localauth/PlatformNomeAppLockScreen.android.kt` | baseline geometry over actual identity/LAMode and existing fail-closed authentication result owner |
| Android P03–P06 onboarding presentation | `common/src/androidMain/kotlin/chat/simplex/common/views/onboarding/PlatformNomeOnboardingPages.android.kt` | baseline scaffolds/cards/density/actions over existing onboarding/create-user/operator/conditions/migration owners |
| P02–P06 post-clarification visual evidence | `plans/evidence/20260718_nome_android_completion/visual-baselines/api35-zh-light/` | API 35 Chinese/light production plus same-size side-by-side captures; no reference-only truth |
| shared home seam | `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt` | declaration only; narrow sharing reason recorded below |
| Android P07/P08 production home | `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/{NomeHomeStateAdapter.kt,NomeHomeRoute.android.kt}` | Android-only derivation/rendering over official facts |
| Android P09 loaded-chat search | `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/{NomeSearchStateAdapter.kt,NomeSearchRoute.android.kt}` | groups only official filtered rows; preserves baseline regions with truthful message-scope/recent-retention policy; no recent/global-message producer |
| P10 platform hub | `common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub*` | Android presentation over existing callbacks; Desktop invokes legacy content |
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

The sharing reason is route ownership: `MainScreen` already owns database-root priority, and
`StartPartOfScreen` already decides between delivery-receipt setup, normal home, and share content.
Moving or copying either decision into the Android app module would create a second root/navigation
truth and would miss non-Activity `AppScreen` entry paths. Each `commonMain` seam therefore carries
only existing facts, action closures, and/or upstream content; it contains no Nome UI. Desktop
actuals and tests preserve the official views.

Batch 1A P01 wraps the existing root inputs and recovery closures, including an exact typed result
for the already-existing backup-pair copy. Batch 2 adds generation-scoped load/result types around
the existing get-chats API, not a second API or native command. Batch 3 adds typed siblings around
the already-shared plan/connect commands because command ownership and the seven branching call
sites already live in `commonMain`. None of these seams adds a native call. Any later typed adapter
still requires its own recorded sharing reason and may not bypass the controller to call the native
core.

### P01 typed truth and recovery boundary

P01 is selected only by the existing migration-in-progress, opening, and guarded database-error
branches in `MainScreen`; the root order and the existing delayed-opening behavior stay unchanged.
The Android actual renders no semantics or action while authentication is not authorized. Desktop
invokes the supplied official progress/error content exactly once.

| Fact family | Production meaning |
|---|---|
| root input | `Opening`, `Migrating`, or the current typed `Error`; none creates a new route or native state |
| database key | only database-alias `Available`, `MissingAlias(initialRandom)`, or `UnreadableMaterial(initialRandom)`; bearer key/passphrase bytes never enter the model |
| display state | exhaustive fixed-copy opening/migration, key input/failure, upgrade/downgrade, incompatible/open/key-store/unknown error, and exact backup-pair copy outcome |
| attempt lifecycle | one process-owned atomic submit at a time; terminal presentation is accepted only for the completed accepted generation and matching source token |
| recovery result | successful open invokes the existing Android post-open hook; backup copy reports `BACKUP_PAIR_COPIED` or `BACKUP_PAIR_COPY_FAILED` without claiming the database opened |

Passphrase input is non-saveable, password-semantic, and cleared when key entry leaves the state,
on `ON_STOP`, and on disposal. Duplicate submits and stale/out-of-order completions are ignored.
Confirmation and backup-pair copy are explicit actions. No raw path, migration name, SQL,
exception, alias, key bytes, passphrase, percentage, rollback, or restore guarantee enters visible
copy, semantics, saved state, evidence labels, or the database-alias Logcat message. The exact
native `Unknown` branch alone maps to an unknown display state; typed native errors never collapse
into a fabricated unknown.

The adapter does not modify key derivation, database migration/open commands, database or archive
format, native core, protocol, or the initial-random preference. `FIRST_USE` remains consumed by
the official onboarding/root and P01 does not create `FILTERED_NO_RESULT`.

### P07/P08 typed truth

| Fact family | Source | Production meaning |
|---|---|---|
| load generation | `(remoteHostId, userId)` from `ChatModel.currentChatListGeneration()` | A result or cached row is current only while this generation matches. |
| load lifecycle | `ChatListLoadState` + `ChatListLoadResult` | `Initial`, `Loading`, `Loaded`, `Unavailable`, and `NoCurrentUser` remain distinguishable; failure/no user are not empty success. |
| connectivity | nullable `NetworkObserver.platformNetworkInfo` | `null` is initial unknown; `online == false` is device offline only, not relay/service/private-routing health. |
| core | nullable `chatRunning` | starting/running/stopped is orthogonal to list and connectivity state. |
| list projection | official chats + existing active tag filter + P09 query | preserves core order and underlying list; P09 is loaded-chat filtering only and adds no list mutation. |

The pure adapter produces `LOADING`, `FIRST_USE`, `TRUE_EMPTY`, `FILTERED_NO_RESULT`, `POPULATED`, or `UNAVAILABLE`; connectivity produces `UNKNOWN`, `ONLINE`, or `DEVICE_OFFLINE`; core produces `STARTING`, `RUNNING`, or `STOPPED`. Switching identity/host and generation mismatch hide stale rows. True empty requires a matching-generation typed success. A matching-generation failure can keep cached rows readable with an unavailable panel, and stopped/offline remain independent overlays rather than empty-list aliases. Onboarding consumes no-user before the home route, so `FIRST_USE` remains defensive there. P09 makes `FILTERED_NO_RESULT` reachable only for an active nonblank query over an available loaded base with zero official matches.

P07 rows are read-only with respect to list state. They show existing name/type/timestamp/unread/favorite facts and show message preview text, including spoken preview semantics, only when the existing `showChatPreviews` privacy preference permits it. They provide no favorite, mark-read, mute, delete, tag, or connection mutation. Existing navigation may open a ready direct/group/local chat only while the core is running and the row is not pending deletion; an unavailable panel does not by itself disable that navigation. P08 production-reachable paths cover skeleton/loading, true empty, unavailable, initial network unknown, device offline, and core stopped. First use remains onboarding-owned; P09 alone owns the new filtered-no-result reachability.

The P07/P08/P09 Home does not implement the locale marker, connection execution, composer/send,
tag mutations, protocol/core/database-format changes, or a broad Desktop redesign.

### P09/P10 typed truth and route boundary

P09 supplies the existing `filteredChats` function with a nonblank query and the current loaded
chat list. Its adapter classifies only those returned direct, group, channel, and note rows.
Loading and unavailable remain explicit and cannot become no result. Opening a result repeats the
same generation/core/readiness/deletion guard used by Home and then calls the existing
direct/group/note navigation helper. There is no persisted recent-query model, pasted-link route,
or global message aggregation.

P10 extracts no new command. `NewChatSheet` passes its existing invitation, scan/paste,
create-group, and create-channel callbacks through `PlatformNewChatHub`. Android renders the
visual-acceptance layout; Desktop calls `legacyContent()` unchanged. The profile card reads the
actual current user and, only when launched from Nome Home, can close the hub before opening the
existing `UserPicker`.

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

P13 itself does not own P10, implement P12 scan/paste/camera, mutate P14 requests, or render P16
group details. P10 remains a route hub and does not opt its internal actions into P13.

### Source anchors

- semantic palette: [`NomeColorTokens`, light/dark values](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeColorTokens.kt#L7-L131);
- dimensions and minimum target: [`NomeDimensionTokens`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeDimensionTokens.kt#L8-L46);
- elevation, shapes, and typography: [`NomeElevationTokens`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeElevationTokens.kt#L8-L20), [`NomeShapeTokens`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeShapeTokens.kt#L10-L30), [`NomeTypographyTokens`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/NomeTypographyTokens.kt#L11-L89);
- Material 2 adapter and composition locals: [`NomeTheme` / `NomeAndroidTheme`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeTheme.kt#L54-L130);
- reusable primitives: [`NomeButton`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeButton.kt#L16-L90), including an optional page-baseline shape override while preserving the token default; [`NomeStatePanel`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeStatePanel.kt#L28-L175); [`NomeSurface`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeSurface.kt#L13-L31);
- accessibility modifiers: [`nomeMinimumTouchTarget` and TalkBack semantics](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/accessibility/NomeAccessibility.kt#L18-L35);
- P01 shared root input/actions: [`PlatformDatabaseRootRoute`](../../common/src/commonMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.kt) and the exact recovery helpers in [`DatabaseErrorView`](../../common/src/commonMain/kotlin/chat/simplex/common/views/database/DatabaseErrorView.kt);
- P01 Android key/route/state/renderer: [`Cryptor.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/platform/Cryptor.android.kt), [`PlatformDatabaseRootRoute.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.android.kt), [`NomeDatabaseRootStateAdapter.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/database/NomeDatabaseRootStateAdapter.kt), and [`NomeDatabaseRootRoute.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/database/NomeDatabaseRootRoute.android.kt);
- P01 contract tests: [`NomeDatabaseRootStateAdapterTest`](../../android/src/test/java/chat/simplex/app/nome/database/NomeDatabaseRootStateAdapterTest.kt), [`NomeDatabaseAttemptGateTest`](../../android/src/test/java/chat/simplex/app/nome/database/NomeDatabaseAttemptGateTest.kt), [`DatabaseRecoveryRouteBoundaryTest`](../../android/src/test/java/chat/simplex/app/nome/database/DatabaseRecoveryRouteBoundaryTest.kt), [`NomeDatabaseRootComposeTest`](../../android/src/androidTest/java/chat/simplex/app/nome/database/NomeDatabaseRootComposeTest.kt), and [`PlatformDatabaseRootRouteDesktopTest`](../../common/src/desktopTest/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRouteDesktopTest.kt);
- P01 affected renderer reference and packaging guard: [`NomeDatabaseRootEvidenceActivity`](../../android/src/debug/java/chat/simplex/app/nome/database/NomeDatabaseRootEvidenceActivity.kt), [`NomeDatabaseRootScreenshotTest`](../../android/src/androidTest/java/chat/simplex/app/nome/database/NomeDatabaseRootScreenshotTest.kt), and [`NomeDatabaseRootPackagingTest`](../../android/src/androidTest/java/chat/simplex/app/nome/database/NomeDatabaseRootPackagingTest.kt);
- deterministic debug entry: [`NomeFixtureSpec`](../../android/src/debug/java/chat/simplex/app/nome/fixtures/NomeFixtureSpec.kt#L7-L93), [`NomeFoundationFixture`](../../android/src/debug/java/chat/simplex/app/nome/fixtures/NomeFoundationFixtures.kt#L54-L822), [`NomeFoundationActivity`](../../android/src/debug/java/chat/simplex/app/nome/harness/NomeFoundationActivity.kt#L23-L76), and [`NomeDesignSystemPreviews`](../../android/src/debug/java/chat/simplex/app/nome/preview/NomeDesignSystemPreviews.kt#L20-L111);
- contract/device evidence: [`NomeFoundationContractTest`](../../android/src/test/java/chat/simplex/app/nome/NomeFoundationContractTest.kt#L8-L20), [`NomeAndroidPackagingTest`](../../android/src/androidTest/java/chat/simplex/app/nome/NomeAndroidPackagingTest.kt#L15-L47), [`NomeFoundationComposeTest`](../../android/src/androidTest/java/chat/simplex/app/nome/NomeFoundationComposeTest.kt#L53-L513), and [`NomeFoundationScreenshotTest`](../../android/src/androidTest/java/chat/simplex/app/nome/NomeFoundationScreenshotTest.kt#L20-L267).
- shared/host split: [`StartPartOfScreen()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L366-L393), [`PlatformHomeRoute()`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L16-L22), [`NomeProductionShell()`](../../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L17-L23), [Android actual](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L63-L123), and [Desktop fallback](../../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt#L9-L17);
- typed load/connectivity facts: [`ChatListLoadGeneration`, `ChatListLoadState`, and `ChatListLoadResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106), [`applyChatListLoadResult()`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L264-L311), [`apiGetChatsResult()`](../../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081), and [`platformNetworkInfo`](../../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L18-L24);
- P07/P08 adapter and renderer: [`NomeHomeStateAdapter.derive()`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L49-L121) and [`NomeHomeRouteContent()`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L125-L280);
- P09 search adapter/route: [`NomeSearchStateAdapter.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeSearchStateAdapter.kt) and [`NomeSearchRoute.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeSearchRoute.android.kt);
- P10 shared/Android/Desktop presentation seam: [`PlatformNewChatHub.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.kt), [`PlatformNewChatHub.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.android.kt), and [`PlatformNewChatHub.desktop.kt`](../../common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.desktop.kt);
- P02/P03–P06 page-baseline production renderers: [`PlatformNomeAppLockScreen.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/views/localauth/PlatformNomeAppLockScreen.android.kt) and [`PlatformNomeOnboardingPages.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/views/onboarding/PlatformNomeOnboardingPages.android.kt);
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

Validation follows the active completion ledger. Ordinary UI groups include:

1. focused derivation/Compose tests proportional to changed behavior;
2. Android and Desktop compilation where shared seams change;
3. API 35 production smoke;
4. affected visual/accessibility inspection and one risk-oriented review;
5. at least one primary API 35 production capture per page family, combined with its exact
   visual-acceptance baseline at the matching viewport/language and corrected for obvious
   differences.

High-risk database, authentication, security verification, backup/migration, files, and calls
retain real fixtures, lifecycle, and API 28/API 35 depth. Full API 28/33/35, bilingual light/dark,
200%, TalkBack, historical-manifest, release, and regression matrices run at milestones/final.
Only final RC requires two consecutive same-summary zero-issue reviews.

The same `.nome.dev` package and debug signing identity covered this foundation's upgrade smoke: a non-empty v6.5.6 snapshot was restored, the minSdk 28 build installed with `-r`, the chat database and preferences remained byte-identical at the immediate checkpoint, and the real core cold-started into the existing Chinese connection flow. The agent database is runtime-mutable and is not claimed byte-identical. Later production adapters must add deeper identity/chat/attachment/settings/language assertions proportional to what they change.

The Batch 2 test files and source anchors in §8 define the implementation contract. The independent evidence root contains the Gradle, Desktop, API 28/API 35 real-core, bilingual-light-dark-100%-200%, 48dp/TalkBack, P07/P08 comparison, release-isolation, threat-delta, and manifest execution artifacts. This specification does not itself claim the formal two-round outcome; only the evidence root's `review-rounds.md` owns that status.

The frozen Batch 3 P13 evidence root is
`plans/evidence/20260717_nome_android_phase2_batch3_p13/`; only that root owns its device,
real-core/two-client, native comparison, accessibility, release-isolation, threat-delta, and
same-digest review claims.

Active Batch 1A P01 uses
`plans/evidence/20260718_nome_android_batch1a_p01/`. Unit/Compose/Desktop tests and deterministic
renderer captures are not substitutes for real opening/migration/recovery fixtures. That root must
separately own API 28/API 35, real database/key/upgrade/downgrade/backup-pair behavior, lifecycle,
manual accessibility, release isolation, secret scans, threat delta, and two final same-digest
reviews before P01 is frozen.

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

Completed in frozen Phase 2 Batch 3:

- one explicit Android external-`ACTION_VIEW` opt-in and a `Legacy` default for all six other caller surfaces;
- exhaustive seven-eligible/fourteen-fallback plan policy, safe model, and Desktop-declining expect/actual seam;
- typed non-logging plan/connect delegates over unchanged core commands;
- Android P13 ready/current/incognito/warning/connecting/pending/failure/retry/cancel route and pure reducer;
- bilingual resources, light/dark tokens, 48dp and 200% Compose coverage, nine-state/72-capture debug screenshot definition, non-exported debug host, release packaging guard, and Desktop fallback test;
- no change to `FIRST_USE`, `FILTERED_NO_RESULT`, P09–P12, P14–P24, native core, protocol, database, message state, iOS, or Desktop UI.

Implemented in the active Milestone 2 P09/P10 group:

- P09 nonblank loaded-chat query producer, truthful unavailable/no-result separation, grouped
  official results, and guarded official chat navigation;
- P10 Android visual hub over the four existing New Chat callbacks plus actual current-profile
  display, with unchanged Desktop fallback;
- focused state/Compose/callback tests, Android/Desktop compilation, API 35 production smoke, and
  Chinese/light/reference-viewport side-by-side acceptance captures;
- no persisted recent query, global message aggregation, connection-success inference, new
  command, native/core/protocol/database/archive/iOS behavior, or Desktop Nome UI.

Implemented in active Batch 1A P01 source, with formal execution status delegated to its evidence
root:

- the unchanged shared root order delegates only opening, migration-in-progress, and guarded
  database-error presentation through a narrow platform seam;
- Android derives exhaustive fixed-copy states from existing typed database results and a
  database-alias-only key-read class; Desktop invokes the official fallback once;
- one process-owned atomic attempt, source-bound terminal presentation, non-saveable passphrase,
  lifecycle/disposal clearing, explicit confirmation, and exact backup-pair copy success/failure;
- bilingual Nome renderer with safe insets, IME/scroll handling, 48dp actions, heading/live-region
  semantics, and no raw error, path, migration name, alias, key, passphrase, percentage, rollback,
  or restore guarantee;
- additive reducer, boundary, attempt, Compose, and Desktop fallback tests, with no new command,
  native/core/protocol/database/archive/iOS behavior.

Remaining implementation/evidence gates:

- implement the one-time locale marker without overwriting existing-user language behavior;
- complete P01 deterministic/native comparison, real database/key/recovery/upgrade fixtures,
  API 28/API 35, lifecycle, manual accessibility, release isolation, secret scans, threat delta,
  and two final same-digest review gates;
- complete the P09/P10 ordinary-group review and Milestone 2 concentrated gate;
- implement P11–P12 and P14–P24 production pages in dependency-ordered merged UI groups;
- run bilingual/theme/API/accessibility/release/full regression at milestones and final RC;
- add deeper upgrade assertions when a later batch touches identities, chats, attachments, settings, or locale persistence.
