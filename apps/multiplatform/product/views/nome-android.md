# Nome Android

> **Status:** Foundation, P07/P08, P13, and Milestone 1 P01–P06 are frozen. The targeted P02–P06
> post-clarification visual recheck and the P09/P10 ordinary UI group are closed over official
> v6.5.6 owners; the Milestone 2 concentrated gate/checkpoint is pending.
> `FIRST_USE` remains official onboarding/root-owned. `FILTERED_NO_RESULT` is reachable only from
> the active P09 query producer and is not reclassified as P08. P11–P12 and P14–P24 remain pending.
> **Related spec:** [spec/client/nome-android-ui.md](../../spec/client/nome-android-ui.md)
> **Coverage matrix:** [plans/20260716_03.md](../../../../plans/20260716_03.md)

## Purpose

Nome Android is an Android-specific product shell over the official SimpleX Chat v6.5.6 state, controller APIs, local database, background services, and native core. It changes the visible product language, information architecture, and interaction design without redefining protocol, delivery, encryption, migration, identity, or network facts.

The product promise is:

- a Nome-branded Android experience with approved assets and interaction patterns;
- Simplified Chinese by default for a new Nome install and complete English support;
- a coherent light and dark system;
- honest, recoverable states backed by existing client/core truth;
- no loss of the official Android capability surface, including secondary pages and system entry points.

## Product boundaries

- Android only for this implementation phase. iOS is not developed here.
- Nome's release minimum is Android API 28. API 26–27 are unsupported; Gradle, merged manifests, APK badging, API 28 launch, and API 35 same-package non-empty upgrade checks are complete. The Phase 1 minSdk 26 build and crash evidence remain immutable historical facts.
- The Haskell/native core, archive format, migration protocol, command protocol, and database semantics stay unchanged.
- `ChatModel`, `ChatController`, core events, Android services, permissions, and intents remain the source of truth.
- P01–P24 effect images are page-level visual-acceptance baselines; they govern Android
  composition, placement, hierarchy, styling, iconography, action sizing, and density, while only
  official v6.5.6 models/routes/actions may supply product facts.
- P01–P24 are the P0 effect-page set, not the complete reachable product boundary.
- Desktop must not be rebranded accidentally through broad `commonMain` changes.
- The frozen P07/P08 home slice is projection plus navigation into already-ready direct/group/note
  conversations; P09 adds only loaded-chat name filtering and P10 only presents existing New Chat
  callbacks; P13 remains external-`ACTION_VIEW` only; P01 replaces only the presentation/controller
  layer of existing database roots. None authorizes a second navigation stack/controller/model,
  direct native calls, or database semantics changes.

## Information architecture

### Root state gates

The existing root priority is a product safety requirement and must not be reordered for visual convenience:

1. migration in progress;
2. database migration/opening;
3. database error/recovery;
4. database encryption and first-user readiness;
5. onboarding;
6. main chat shell;
7. local authentication, calls, privacy alerts, and system overlays.

Nome replaces presentation at each branch while preserving the branch condition and recovery action.

### P0 page families

| Family | Pages | User goal |
|---|---|---|
| Startup and trust | P01–P06 | Open the database safely, unlock, create or migrate an identity, understand network settings, and accept real conditions. |
| Home and discovery | P07–P10 | See chats, distinguish empty/loading/offline states, search locally, and choose a real connection path. |
| Connections | P11–P16 | Create or consume links, scan/paste, choose identity/incognito, handle requests, manage a public address, and preview/join groups. |
| Messaging and security | P17–P19 | Converse with real delivery states, operate on messages, and verify a contact using the core security code. |
| Public channels | P20–P21 | Configure/join channel relays and use a channel with a fixed non-E2EE disclosure. |
| Identity and data | P22–P24 | Manage local identities, settings, backups, and device migration. |

### P1 reachable families

Nome must also cover chat utilities, contact detail, group administration, channel owner tools, media/gallery/file states, calls, notifications/background-service pages, privacy controls, database recovery, advanced network/server configuration, appearance/localization, remote desktop pairing, help/about/developer pages, deep links, share intents, notification actions, lifecycle, and Android back behavior.

When no dedicated effect page exists, the coverage matrix names the adjacent P0 component family. That reference governs visual consistency only; product information architecture beyond the approved specification requires a separate product decision.

## Language and terminology

- First-launch Nome default: Simplified Chinese.
- Supported launch languages: `zh-CN` and English.
- “Follow system” is allowed, but a non-Chinese system locale falls back to complete English; it is not a third incomplete translation set.
- Existing upstream locale resources may remain during development, but they are not claimed as Nome launch languages.
- Product-facing brand is Nome. “SimpleX” remains only where it is a factual protocol/core source, compatibility, license, or legal reference.
- Use **local identity / 本地身份**, not an account or cloud profile.
- Use **incognito for this connection / 本次连接使用隐身身份** for the current per-connection behavior; do not imply a durable anonymous account.
- Public channels always disclose that they are not end-to-end encrypted. Do not use a generic lock badge that contradicts this fact.
- Do not show online presence, last seen, global user IDs, verified operator health, invitation expiry, delivery success, or routing privacy unless an existing state/API/core event proves it.

## Theme and layout

The 24 Android effect pages are page-level light-theme visual acceptance baselines, not merely a
palette or component-token source. Shared components accelerate delivery, but each production page
must still be rendered at its reference viewport/language and checked side by side for composition,
region placement, hierarchy, color, type, spacing, corners, icons, action size, and density. The
official v6.5.6 state/action/data semantics remain authoritative where a reference contains an
unsupported fact. Dark mode is frozen as **Nome Dark Token v1**, represented by P02, P07, P17,
P21, and P23.

Dark Token v1 preserves the approved light baseline's information architecture, semantics, copy priority, component dimensions, spacing, typography scale, icon size, state set, 48dp targets, and safe-area behavior. It only remaps surface tiers, text/icon/status contrast, scrim, border, and elevation treatment. It cannot add a second visual direction, change a route or state, or use color as the only signal for danger, error, unread, delivery, verification, relay, or public-channel disclosure. The five deterministic foundation representatives have passed native screenshot, contrast, 200% font, 48dp, and TalkBack-semantics verification. Each later production page still has to repeat its applicable real-state and device gates before shipping.

Minimum interaction rules:

- 48dp minimum touch target;
- safe-area and gesture inset support;
- system back and predictive-back behavior preserve route semantics;
- destructive identity/address/data actions require explicit confirmation;
- content remains usable at 200% font without clipping or inaccessible actions;
- error and recovery actions are adjacent to the affected state;
- fixed channel disclosure remains visible in the conversation context.

## Phase 2 source placement

The Android-only foundation keeps its frozen placements. The frozen batches add the split
production host/home route, P13 connection seam, and bounded P01 database-root presentation seam.
Milestone 2 extends the existing Home/New Chat presentation owners for P09/P10:

- tokens, theme, components, and accessibility primitives: `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/{tokens,theme,components,accessibility}/`;
- Activity/window host: [`NomeProductionShell.kt`](../../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L9-L23), installed around the unchanged `AppScreen` by [`MainActivity`](../../android/src/main/java/chat/simplex/app/MainActivity.kt#L60-L65);
- shared home-selection seam only: [`PlatformHomeRoute.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L8-L22), called from the existing [`StartPartOfScreen`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L366-L393);
- Android P07–P09 Home renderer/adapters:
  [`NomeHomeRoute.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt),
  [`NomeHomeStateAdapter.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt),
  [`NomeSearchRoute.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeSearchRoute.android.kt),
  and [`NomeSearchStateAdapter.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeSearchStateAdapter.kt);
- P10 platform presentation seam and Android renderer:
  [`PlatformNewChatHub.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.kt),
  [`PlatformNewChatHub.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.android.kt),
  and the unchanged legacy delegate in
  [`PlatformNewChatHub.desktop.kt`](../../common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.desktop.kt);
- Desktop fallback: [`PlatformHomeRoute.desktop.kt`](../../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt#L8-L17), which delegates the official content unchanged;
- typed shared load provenance: [`ChatListLoadGeneration`, `ChatListLoadState`, and `ChatListLoadResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106), applied through the existing model/controller rather than a second store;
- Android first-network-observation provenance: [`NetworkObserver.platformNetworkInfo`](../../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L16-L87);
- deterministic fixtures, screenshot harness, and Preview entry points: `android/src/debug/java/chat/simplex/app/nome/`;
- JVM/unit tests: `android/src/test/java/chat/simplex/app/nome/`;
- instrumentation/device tests: `android/src/androidTest/java/chat/simplex/app/nome/`, including an explicit argument-gated real-core stop/start gate that preserves the exact non-empty cached chat-ID sequence.
- shared P13 safe model and policy:
  [`PlatformConnectionPreview.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.kt#L13-L292), offered only from
  [`connectIfOpenedViaUri`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt#L738-L754);
- typed P13 command truth:
  [`apiConnectPlanResult` and `apiConnectResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1571-L1647), which suppress P13 terminal command logging without changing legacy wrappers;
- Android P13 actual, reducer, renderer, and bilingual resources:
  `common/src/androidMain/kotlin/chat/simplex/common/{views/newchat/PlatformConnectionPreview.android.kt,ui/nome/connection/**}` and `common/src/androidMain/res/values*/nome_connection_preview_strings.xml`;
- Desktop P13 actual:
  `common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.desktop.kt`, which returns `false` and preserves legacy presentation.
- shared P01 facts/action seam:
  `common/src/commonMain/kotlin/chat/simplex/common/views/database/{PlatformDatabaseRootRoute,DatabaseErrorView}.kt`, called only from the existing root branches in `App.kt`;
- Android P01 key-read class, process attempt owner, reducer, renderer, and bilingual resources:
  `common/src/androidMain/kotlin/chat/simplex/common/{platform/Cryptor.android.kt,views/database/PlatformDatabaseRootRoute.android.kt,ui/nome/database/**}` and `common/src/androidMain/res/values*/nome_database_root_strings.xml`;
- Desktop P01 actual:
  `common/src/desktopMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.desktop.kt`, which invokes the supplied official content once.
- P01 deterministic renderer reference and packaging guard:
  `android/src/debug/java/chat/simplex/app/nome/database/NomeDatabaseRootEvidenceActivity.kt` and
  `android/src/androidTest/java/chat/simplex/app/nome/database/{NomeDatabaseRootScreenshotTest,NomeDatabaseRootPackagingTest}.kt`;
  these are debug/test-only and never a real database result.

The app-module shell cannot replace a route selected inside `AppScreen` without duplicating or overlaying the official root/navigation. The narrow `expect`/`actual` seam therefore keeps root gates, authentication, calls, share intents, overlays, safe areas, and back behavior in their existing owners while keeping all Nome presentation in `androidMain`. The Desktop actual and test constrain the shared seam to fallback behavior.

The shared typed result is the other bounded exception: [`apiGetChatsResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081) wraps the existing `CC.ApiGetChats` / `CR.ApiChats` request and distinguishes success, failure, and no-current-user. It adds no command, protocol, receiver, API owner, native call, or Haskell/core change.

## State truth rules

| State type | Product rule |
|---|---|
| Home identity/load | A list result is usable only for the current `(remoteHostId, userId)` generation. Switching, mismatch, initial state, a hidden-row load, or an in-flight load without cached rows hides old rows. A same-generation refresh with `hideRows=false` may continue showing cached rows. |
| Loading/progress | Use determinate progress only when the API exposes a real numerator/denominator or stage. P08 uses a skeleton only for explicit initial/hidden-row/no-cache in-flight facts, never because a list happens to be empty. |
| Empty/filtered | True empty requires typed same-generation `ApiChats` success with zero base rows; failure is unavailable. The unchanged root consumes no-user in onboarding, so first use remains defensive at Home. P09 supplies a real nonblank loaded-chat filter producer; filtered no result is reachable only while that producer is active over an available loaded base with zero matches. |
| Offline/network | Before Android's first platform observation, connectivity is unknown. Device connectivity is not proof that relays, operators, delivery, or private routing are healthy. Label each level precisely. |
| Core stopped | Core state is independent of content/network. Same-generation cached rows may remain visible while stopped, but core-dependent actions are read-only. |
| Security | Verification, authentication, encryption, and non-E2EE labels come from the relevant security state, not visual convention. |
| Delivery | Sent/delivered/read/failed comes from message/core events. Optimistic compose state must be visibly distinct. |
| Invitation lifecycle | Copied/shared does not mean used. An expiry label requires a real TTL/expiry source. |
| Presence | Nome does not invent online/last-seen presence. |
| Migration/recovery | Never promise rollback, resume, cancel, or restore unless the implemented archive/migration path guarantees it. |

## Batch 1A P01 production boundary

P01 is selected only by the existing opening, database-migration-in-progress, and guarded
database-error branches. The branch order, one-second opening presentation delay, and
`!unauthorized` ownership remain unchanged. Android derives one fixed state from exact
`DBMigrationResult` subtype, controller/migration progress flags, stored-key Boolean facts, a
no-secret database-key read class, and the exact matched two-file backup predicate. Desktop calls
the official legacy content exactly once.

The Android route permits only the existing operations: open once with an entered key, separately
save then open, explicit upgrade/downgrade confirmation, exact matched-pair copy, and a separate
fresh open after copy. The atomic attempt gate prevents duplicate commands. Copy success/failure
is attempt- and source-bound and cannot override a newer model result. Only a fresh native `OK`
leaves P01 and invokes the existing Android post-open hook.

Passphrase text is non-saveable and clears on submit, terminal/root transition, background stop,
and composition disposal. The database Keystore seam records only missing-alias/decrypt-failure
class plus the existing initial-random Boolean. P01 never repeats raw path, migration name, SQL,
JSON, stack, key text, percentage, staged progress, timeout, cancel, generic rollback, identity
loaded, or messaging-restored copy.

## Batch 2 P07/P08 production boundary

The Android actual consumes the current shared chat order and existing active tag filter, then renders independent content, connectivity, and core axes.

Included source behavior:

- populated rows project name, timestamp, unread count, and favorite fact; latest-message text is visible and spoken only when the existing `showChatPreviews` preference permits it;
- only already-ready direct, group, and note-folder conversations may navigate through existing helpers, and only while the core is running;
- contact-request, pending-connection, invalid, not-ready, pending-deletion, and stopped-core rows have no chat-open or home mutation action;
- production-reachable P08 source paths include loading, true empty, unavailable load, network unknown, device offline, and core stopped;
- first use and active-filter no result remain defensive renderer/test branches until a separately authorized production route/filter interaction makes them reachable;
- same-generation cached rows can coexist with unavailable/stopped presentation without being relabelled empty; unavailable cached rows can still navigate while the core is running, whereas stopped rows cannot;
- `zh-CN` and English strings, Nome light/dark theme, state-panel semantics, 48dp row targets, and 200% Compose cases exist in source/tests.
- Nome palette selection and Activity system bars both observe the official `CurrentColors` light/dark result rather than independently forcing the system palette.

Explicit exclusions:

- favorite mutation, connection/request mutation, and tag/filter controls;
- pasted-link handling inside P09, persisted recent queries, and unqueried global message
  aggregation;
- settings entry, composer/send, locale marker, and GAP-16 archive/migration work;
- any Haskell/native-core, protocol, command/event, database/archive, iOS, or Desktop Nome UI change.

The independent Batch 2 evidence records the frozen P07/P08 execution gates. It does not claim the
later P09 query or P10 route-hub implementation.

## Milestone 2 P09/P10 production boundary

P09 passes a nonblank query to the existing `filteredChats` loaded-chat producer. Android groups
only the returned direct, group, channel, and note rows for presentation and reuses the existing
ready-chat navigation guards. It stores no recent-query history, performs no global message
aggregation, and does not treat loading/unavailable as no result.

P10 is the Android presentation of the existing `NewChatSheet` callbacks. One-time invitation,
scan/paste, create group, and create channel continue into their official routes. The current
identity card reads the real local profile and may open the existing `UserPicker`; it does not
create an account or assert connection status.

Both pages use their effect images as visual-acceptance baselines. Their primary Chinese/light/API
35 production captures are compared side by side at the reference viewport. P09 keeps the
baseline's message-result and recent-search regions but replaces unsupported rows/chips with
truthful scope and local-retention policy copy. P10 keeps the baseline action structure but names
the group action according to the real create-group callback.

## Batch 3 P13 production boundary

P13 is reachable only after Android delivers a supported external `ACTION_VIEW` URI and the
unchanged root has an active user and running core. The upstream controller still performs the
real connection plan. Nome replaces only the seven existing current/incognito identity-choice
branches:

- fresh invitation, address, or group plans without short-link preparation data;
- the invitation/address own-link warnings; and
- the address repeat-request and group repeat-join warnings.

Every other core plan retains its existing path. In particular, short-link preparation, known or
prohibited objects, group own-link/no-relay/update-required, and plan errors do not enter P13.
New Chat, scan/paste, link search, message links, chat preview links, and group-member links retain
their legacy presentation.

The page shows only safe type, consequence, local-profile, warning, and owner-proof facts. The raw
URI, resolved `CreatedConnLink`, and owner signature stay in an ephemeral common closure. Current
profile and new incognito profile are mutually exclusive; incognito is for this connection, not a
persistent identity. Continue is single-submit, checks the same user/host, and reaches pending only
after the core returns `SentConfirmation` or `SentInvitation` with a real pending connection.
Failure, already-existing contact, missing user, changed context, and cancellation remain
non-success. Retry retains the selected identity, asks the core for a fresh plan under the current
user/host, keeps typed planning failure or missing-user truth on the current page, and delegates a
successful changed plan back to the authoritative existing branch.

Batch 3 does not add a home connection entry, scanner/camera behavior, P14 request mutation, P16
group details, or any other P09–P24 page. It changes no Haskell/native core, `Core.kt`, protocol,
database/archive, message-state-machine, iOS, or Desktop UI behavior.

## Accessibility and evidence contract

Each batch is complete only when all applicable page states have:

- Simplified Chinese and English coverage;
- light and dark coverage;
- semantic role, accessible name, state description, traversal order, and error announcement;
- 48dp targets and 200% font validation;
- keyboard/IME, permission, back, and focus behavior where applicable;
- native screenshot comparison against the approved Android page or named P0 reference;
- real API/core or Android platform evidence;
- automated tests proportional to the behavior;
- two consecutive adversarial review rounds with zero remaining issues.

Preview fixtures can demonstrate visual variants, but only device runs against production state paths satisfy real-behavior evidence.

The targeted P02–P06 API 35 Chinese/light recheck confirms the actual rendered onboarding family,
not just shared tokens. P02 aligns the compact Nome wordmark, lock/identity stack, pill actions,
and local-data strip while retaining real local-auth outcomes. P03–P06 align the baseline page
scaffolds, identity/operator/commitment cards, list density, information strips, and anchored
actions. Truthful differences remain explicit: P04 uses the current local identity, P05 shows
operator configuration rather than health/toggles and does not invent an unsafe back transition,
and P06 retains the official conditions/non-E2EE wording.

## Recorded product decisions

The 2026-07-17 delegated decision adopts the recommended route for GAP-08 through GAP-17 and freezes GAP-18 at minSdk 28. The detailed contract is in [product/gaps.md](../gaps.md) and `plans/20260717_02.md`; the product consequences are:

- unsupported startup percentage, generic restore/rollback/timeout, invitation TTL/expiry, verified-admin, direct-report, Nome-domain, persistent-anonymous-account, global-health, private-routing-health, and aggregate-E2EE claims are removed or truthfully reworded;
- local authentication fails closed, existing APIs receive Android/client single-submit and typed-result state, search is explicitly scoped with no recent-query persistence, invitation/request/address lifecycles use only returned results/events, and archive/migration controls remain stage-specific;
- API 26–27 are unsupported and no native rebuild or native/core source change is authorized;
- every C row in the coverage matrix remains a missing capability. A product decision can remove or reword that state, but it cannot reclassify C as A/B or mark it implemented.

The foundation, P07/P08, P13, and Milestone 1 P01–P06 remain frozen. The targeted P02–P06 visual
correction and P09/P10 ordinary UI group are ready for the Milestone 2 concentrated gate. First
use remains onboarding-owned; only P09 closes the filtered-no-result producer gap. P11–P12 plus
P14–P24 remain outside this group.

## Current evidence

The unchanged v6.5.6 baseline and Phase 2 foundation evidence remain read-only. The foundation records the Android-only tokens/theme/components/accessibility and non-exported debug harness, 96 deterministic API 35 screenshots, Compose semantics/contrast checks, API 28 real-core startup, and API 35 same-package non-empty upgrade. Those records prove the foundation only.

Batch 2 has a separate evidence root at [`plans/evidence/20260717_nome_android_phase2_batch2_home/README.md`](../../../../plans/evidence/20260717_nome_android_phase2_batch2_home/README.md). That root contains the execution artifacts summarized above. This product document does not itself claim the formal two-round result; only the evidence root's `review-rounds.md` owns that status.

Batch 3 P13 uses a separate evidence root at
`plans/evidence/20260717_nome_android_phase2_batch3_p13/`. Debug renderer captures are not
real-core evidence; device production-route, two-client, accessibility, release-isolation, and
same-digest review results remain authoritative only when recorded there.

Batch 1A P01 uses
`plans/evidence/20260718_nome_android_batch1a_p01/`. Reducer, Compose, or deterministic renderer
tests prove presentation only; disposable database/Keystore/backup/migration fixtures, real
root/core results, TalkBack, release isolation, and same-digest reviews remain authoritative only
when recorded there.

Current P02–P06 post-clarification and P09/P10 execution/visual comparisons are recorded only in
`plans/evidence/20260718_nome_android_completion/`; no standalone ordinary-batch evidence root or
checkpoint is created.
