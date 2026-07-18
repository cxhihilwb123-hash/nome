# Nome Android

> **Status:** Phase 2 Android-only design foundation and the authorized Batch 2 production shell plus bounded, read-only P07/P08 production-home slice are frozen. Batch 3 adds only the authorized P13 external-link connection-preview source; its execution and review status is owned exclusively by its evidence root and is not asserted here. `FIRST_USE` and `FILTERED_NO_RESULT` remain renderer-only/deferred production-reachability branches. P09–P12 and P14–P24 are not claimed.
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
- A visual prototype or screenshot is not a product state source.
- P01–P24 are the P0 effect-page set, not the complete reachable product boundary.
- Desktop must not be rebranded accidentally through broad `commonMain` changes.
- Batch 2's home slice is projection plus navigation into already-ready direct/group/note conversations; Batch 3 adds only the external-link P13 modal. Neither authorizes home mutations, a second navigation stack, a second controller/model, or direct native calls.

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

The approved light palette and component language come from the comprehensive design specification and the 24 Android effect pages. Dark mode is frozen as **Nome Dark Token v1**, represented by P02, P07, P17, P21, and P23.

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

The Android-only foundation keeps its frozen placements, Batch 2 adds one split production
host/home route, and Batch 3 adds one P13 connection seam:

- tokens, theme, components, and accessibility primitives: `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/{tokens,theme,components,accessibility}/`;
- Activity/window host: [`NomeProductionShell.kt`](../../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L9-L23), installed around the unchanged `AppScreen` by [`MainActivity`](../../android/src/main/java/chat/simplex/app/MainActivity.kt#L60-L65);
- shared home-selection seam only: [`PlatformHomeRoute.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L8-L22), called from the existing [`StartPartOfScreen`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L366-L393);
- Android P07/P08 renderer and adapter: [`NomeHomeRoute.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L63-L123) and [`NomeHomeStateAdapter.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt#L7-L121);
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

The app-module shell cannot replace a route selected inside `AppScreen` without duplicating or overlaying the official root/navigation. The narrow `expect`/`actual` seam therefore keeps root gates, authentication, calls, share intents, overlays, safe areas, and back behavior in their existing owners while keeping all Nome presentation in `androidMain`. The Desktop actual and test constrain the shared seam to fallback behavior.

The shared typed result is the other bounded exception: [`apiGetChatsResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt#L1054-L1081) wraps the existing `CC.ApiGetChats` / `CR.ApiChats` request and distinguishes success, failure, and no-current-user. It adds no command, protocol, receiver, API owner, native call, or Haskell/core change.

## State truth rules

| State type | Product rule |
|---|---|
| Home identity/load | A list result is usable only for the current `(remoteHostId, userId)` generation. Switching, mismatch, initial state, a hidden-row load, or an in-flight load without cached rows hides old rows. A same-generation refresh with `hideRows=false` may continue showing cached rows. |
| Loading/progress | Use determinate progress only when the API exposes a real numerator/denominator or stage. P08 uses a skeleton only for explicit initial/hidden-row/no-cache in-flight facts, never because a list happens to be empty. |
| Empty/filtered | True empty requires typed same-generation `ApiChats` success with zero base rows; failure is unavailable. The adapter can distinguish explicit no-user and an already-active filter, but the unchanged root consumes no-user in onboarding and this bounded home has no filter producer. First use and filtered no result are therefore defensive renderer/test contracts, not current production-route claims. |
| Offline/network | Before Android's first platform observation, connectivity is unknown. Device connectivity is not proof that relays, operators, delivery, or private routing are healthy. Label each level precisely. |
| Core stopped | Core state is independent of content/network. Same-generation cached rows may remain visible while stopped, but core-dependent actions are read-only. |
| Security | Verification, authentication, encryption, and non-E2EE labels come from the relevant security state, not visual convention. |
| Delivery | Sent/delivered/read/failed comes from message/core events. Optimistic compose state must be visibly distinct. |
| Invitation lifecycle | Copied/shared does not mean used. An expiry label requires a real TTL/expiry source. |
| Presence | Nome does not invent online/last-seen presence. |
| Migration/recovery | Never promise rollback, resume, cancel, or restore unless the implemented archive/migration path guarantees it. |

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

- favorite mutation, profile-switch control, connection/request mutation, and tag/filter controls;
- search UI, pasted-link handling, global aggregation, and search no-result state;
- new-chat/settings entry controls, composer/send, locale marker, and GAP-16 archive/migration work;
- any Haskell/native-core, protocol, command/event, database/archive, iOS, or Desktop Nome UI change.

The independent Batch 2 evidence records the real-device, real-core, screenshot, manual TalkBack, threat-model, and release-isolation execution gates. Formal two-round review status is recorded only by that evidence root's `review-rounds.md` and is not asserted by this product view. Those execution results cover only the authorized production-home slice and do not close the two production-reachability gaps above.

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

## Recorded product decisions

The 2026-07-17 delegated decision adopts the recommended route for GAP-08 through GAP-17 and freezes GAP-18 at minSdk 28. The detailed contract is in [product/gaps.md](../gaps.md) and `plans/20260717_02.md`; the product consequences are:

- unsupported startup percentage, generic restore/rollback/timeout, invitation TTL/expiry, verified-admin, direct-report, Nome-domain, persistent-anonymous-account, global-health, private-routing-health, and aggregate-E2EE claims are removed or truthfully reworded;
- local authentication fails closed, existing APIs receive Android/client single-submit and typed-result state, search is explicitly scoped with no recent-query persistence, invitation/request/address lifecycles use only returned results/events, and archive/migration controls remain stage-specific;
- API 26–27 are unsupported and no native rebuild or native/core source change is authorized;
- every C row in the coverage matrix remains a missing capability. A product decision can remove or reword that state, but it cannot reclassify C as A/B or mark it implemented.

The foundation and Batch 2 remain complete under their own frozen evidence. Batch 3 is a second
deliberately narrow production slice and contains P13 only. Formal review status is owned only by
the independent batch evidence roots. The first-use/filter reachability gaps remain unchanged,
and P09–P12 plus P14–P24 remain outside this source batch.

## Current evidence

The unchanged v6.5.6 baseline and Phase 2 foundation evidence remain read-only. The foundation records the Android-only tokens/theme/components/accessibility and non-exported debug harness, 96 deterministic API 35 screenshots, Compose semantics/contrast checks, API 28 real-core startup, and API 35 same-package non-empty upgrade. Those records prove the foundation only.

Batch 2 has a separate evidence root at [`plans/evidence/20260717_nome_android_phase2_batch2_home/README.md`](../../../../plans/evidence/20260717_nome_android_phase2_batch2_home/README.md). That root contains the execution artifacts summarized above. This product document does not itself claim the formal two-round result; only the evidence root's `review-rounds.md` owns that status.

Batch 3 P13 uses a separate evidence root at
`plans/evidence/20260717_nome_android_phase2_batch3_p13/`. Debug renderer captures are not
real-core evidence; device production-route, two-client, accessibility, release-isolation, and
same-digest review results remain authoritative only when recorded there.
