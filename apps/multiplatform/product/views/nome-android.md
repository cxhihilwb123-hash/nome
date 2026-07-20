# Nome Android

> **Status:** Foundation, P01–P10, P13, and the first two milestone checkpoints are frozen.
> Milestone 3 source is implemented over official v6.5.6 owners and its concentrated
> non-producer matrix is green; the exact local checkpoint input is under final reconciliation.
> P11 produced and persisted real one-time invitations and now has an accepted bearer-redacted
> API 35 READY production/baseline comparison. P14 has a real pending request and accepted
> API 35 production/baseline comparison.
> P15 has real OFF and READY production proof plus a redacted READY comparison; P16 has real
> controlled invitation/join/message proof plus API 35 private-invitation production calibration.
> The reference's public-invitation state is terminally `NOT REACHABLE LOCALLY`, not visually
> accepted from the private state.
> `FIRST_USE` remains official onboarding/root-owned. `FILTERED_NO_RESULT` is reachable only from
> the active P09 query producer and is not reclassified as P08. The P17 conversation shell and
> P18 action sheet are implemented; real image-file transfer and connected audio-call lifecycle
> pass. Fixed synthetic voice/video peer receipt, digest verification, and production playback
> progress/termination/stop/cleanup also pass.
> P19 is implemented with real two-client security-code lifecycle proof. P20 has a real controlled
> create/link/populate/open/delete/absence lifecycle. P21 has a corrected API 35 owner-state
> production comparison over that real channel; the reference's observer-only visual state remains
> a declared external `NOT REACHABLE LOCALLY` gate and is not claimed accepted. P22 is implemented with API 28/API 35
> destructive identity-lifecycle proof. P23/P24 are implemented with
> API 35 visual acceptance plus disposable API 28/API 35 archive/migration lifecycle proof.
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

Nome's one-time Android locale adapter defaults only a jointly proven clean install to `zh-CN`.
It captures package/data/preference evidence before process initialization and then uses the
official nullable `appLanguage` preference. Existing or upgraded installs, explicit selections,
contradictory evidence, unsupported stored values, process restarts, and later route/identity
changes are preserved rather than reinterpreted as first launch. The marker is product policy,
not onboarding state; `FIRST_USE` remains owned by the official onboarding/root route.

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
Milestone 2 extends the existing Home/New Chat presentation owners for P09/P10. Milestone 3 adds
the P11/P12 presentation and Android camera-lifecycle boundary, the P14/P15 request/address
presentation seams, the P16 group-invitation seam, and the Android P1 group-management
presentation seams:

- tokens, theme, components, and accessibility primitives: `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/{tokens,theme,components,accessibility}/`;
- Activity/window host: [`NomeProductionShell.kt`](../../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L9-L23), installed around the unchanged `AppScreen` by [`MainActivity`](../../android/src/main/java/chat/simplex/app/MainActivity.kt#L60-L65);
- shared home-selection seam only: [`PlatformHomeRoute.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt#L8-L22), called from the existing [`StartPartOfScreen`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L449-L475);
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
- P11/P12 shared presentation seam and Android renderer:
  [`PlatformNewChatRoute.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatRoute.kt),
  [`PlatformNewChatRoute.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatRoute.android.kt),
  and the unchanged legacy delegate in
  [`PlatformNewChatRoute.desktop.kt`](../../common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatRoute.desktop.kt);
- P12 camera lifecycle:
  [`QRCodeScanner.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/views/newchat/QRCodeScanner.android.kt)
  plus optional camera hardware declaration in
  [`AndroidManifest.xml`](../../android/src/main/AndroidManifest.xml);
- P1 private-group creation/invitation:
  `views/newchat/PlatformAddGroupRoute*` and
  `views/chat/group/PlatformAddGroupMembersRoute*`;
- P1 group/channel administration:
  `views/chat/group/PlatformGroupChatInfoRoute*`, plus the existing
  `PlatformSettingsDetailRoute` for group profile, channel members, and channel relays;
- P14 request seam and Android renderer:
  `common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/views/chatlist/PlatformContactRequestRoute*`;
- P15 address seam and Android renderer:
  `common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/views/usersettings/PlatformUserAddressRoute*`;
- shared P14/P15 scaffold and bilingual copy:
  `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeFullPageScaffold.kt`
  and `common/src/androidMain/res/values*/nome_connections_strings.xml`;
- P16 invitation seam and Android renderer:
  `common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/views/chatlist/PlatformGroupInvitationRoute*`,
  offered only from the existing `GroupMemberStatus.MemInvited` owner in
  `ChatListNavLinkView.kt`;
- Desktop fallback: [`PlatformHomeRoute.desktop.kt`](../../common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt#L8-L17), which delegates the official content unchanged;
- typed shared load provenance: [`ChatListLoadGeneration`, `ChatListLoadState`, and `ChatListLoadResult`](../../common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt#L81-L106), applied through the existing model/controller rather than a second store;
- Android first-network-observation provenance: [`NetworkObserver.platformNetworkInfo`](../../common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt#L16-L87);
- deterministic fixtures, screenshot harness, and Preview entry points: `android/src/debug/java/chat/simplex/app/nome/`;
- JVM/unit tests: `android/src/test/java/chat/simplex/app/nome/`;
- instrumentation/device tests: `android/src/androidTest/java/chat/simplex/app/nome/`, including an explicit argument-gated real-core stop/start gate that preserves the exact non-empty cached chat-ID sequence.
- shared P13 safe model and policy:
  [`PlatformConnectionPreview.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.kt#L13-L292), offered only from
  [`connectIfOpenedViaUri`](../../common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt#L760-L791);
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

## Milestone 3 P11/P12 production boundary

P11 is a presentation over the existing `apiAddContact()` invitation producer and
`chatModel.showingInvitation` lifecycle. The displayed link and QR code exist only after a real
`CreatedConnLink`. Copy and share are local actions and never imply peer use or connection.
Connection truth still requires the official event/model path. The baseline's TTL/countdown and
regenerate affordance are omitted because v6.5.6 supplies neither an expiry timestamp nor an
atomic replace result. The compact identity header may invoke the existing profile picker; it
does not create or persist a new identity.

P12 sends manual, explicitly-read clipboard, and camera-scanned text through the existing
`strConnectTarget` parser and `planAndConnect(..., Legacy)` route. It does not pass internal
scan/paste input into the external-`ACTION_VIEW`-only P13 preview. Camera permission is requested
only after the scan action. No-camera, denial, permanent denial/Settings, resume, frame closure,
and executor disposal are Android platform states, not connection facts.

Both production pages must match the P11/P12 visual acceptance baselines at the reference
viewport/language. P12 has an accepted API 35 Chinese/light production comparison. P11 now also
has an accepted API 35 Chinese/light READY comparison from a real `CreatedConnLink`. The first
comparison exposed that the shared QR default width overrode the Android size and rendered a
216dp QR against the baseline's approximately 136dp geometry. The shared helper now accepts an
optional explicit image size while preserving its default behavior; Android P11 uses 136dp.
The retained production and side-by-side images redact the link and QR at capture time, and the
semantics tree contains no bearer.

## Milestone 3 P14/P15 production boundary

P14 opens from both official pending-request shapes—legacy `ContactRequest` and
`Direct + contactRequestId`—and preserves the official current-profile,
incognito, and reject actions. Accept mutates the chat list only after a real accepted `Contact`.
Reject uses `APIRejectContactRequestResult`: only `Rejected`, including its valid null-contact
form, removes the request; `Failure` retains it. The effect image's optional request message has no
v6.5.6 producer, so the same region explicitly says that the request has no separate message.
Back and all actions are disabled while a command is in flight, and the sender-not-notified
warning remains visible.

P15 keeps official lookup, creation, short-link upgrade, copy/share, address settings,
profile-sharing prompt, deletion, and advanced-settings owners. `APIUserAddressResult.NotFound`
alone proves OFF; a lookup failure does not erase a confirmed cached address. Replacement is a
confirmed delete followed by create. If create fails after deletion, the page records that the old
address was deleted and retries only creation; it never claims rollback or atomic replacement.
`FIRST_USE` and onboarding continue to use the official address layout unchanged.

Both renderers use P14/P15 as page-level visual acceptance baselines and passed reference-size
Chinese/light fixture comparisons plus focused API 35 tests. P15 later passed real OFF and
bearer-redacted READY production comparison. The corrected P14 owner oracle found the official
pending request in the real `Direct + contactRequestId` shape; Android Home now opens that real
request, whose API 35 Chinese/light production comparison passes. The missing standalone request
message and disabled incognito row remain truthful state differences, and no timeout, success,
online, delivery, or security fact is synthesized.

## Milestone 3 P16 production boundary

P16 opens only from the existing `GroupMemberStatus.MemInvited` action and projects the official
`GroupInfo`, group profile, membership, inviter-contact, and member-count facts. Public/private
and channel labels come from `publicGroup` and `GroupInfo.isChannel`; the page does not infer a
verified administrator. A real current contact may be labelled verified only from its existing
contact verification fact.

Join remains the unchanged `ApiJoinGroup` command. `APIJoinGroupResult.Accepted` is the only
positive result; an expired/not-found invitation remains a distinct unavailable terminal outcome,
and every other response leaves the preview retryable. Delete remains the official local
invitation deletion, requires confirmation, and is not relabelled as cancel. Back closes the
preview without mutating the invitation. Desktop declines the seam and retains the official
alert.

The renderer uses P16 as its visual acceptance baseline. A controlled real private
`GroupMemberStatus.MemInvited` state was preserved on API 28, transferred through the official
archive/import path with its database key crossing only an ephemeral in-memory bridge, and opened
on a disposable API 35 client. Its reference-size Chinese/light comparison calibrates the shared
structure, hierarchy, warning, group card, fact rows, actions, and connection disclosure while
retaining truthful official copy. It does not close the reference's materially different public
group, membership, and inviter state. That public-invitation visual state is terminally
`NOT REACHABLE LOCALLY`; the private production state remains real and functionally ready.
Focused callback, single-submit, failure-retention, busy-back, destructive-confirmation, and 48dp
checks pass. The disposable client and archive were removed after capture. This does not claim
that the invitation was joined or connected.

## Milestone 3 P17/P18 production boundary

P17 remains the official loaded direct chat. Nome changes only Android presentation: a compact
top toolbar, fixed encryption banner, flat conversation surface, bordered incoming/outgoing
bubbles, and pill composer align the screen with the P17 baseline. The large upstream
contact-introduction card is omitted from the Android timeline because the P17 baseline makes the
conversation itself primary; contact detail remains reachable from the unchanged toolbar route.

The fixed banner is not a generic security promise. It appears only when the real timeline
contains a direct `E2EEInfo` item and uses that item's `pqEnabled` value. The toolbar E2EE chip is
gated by the same real producer. No delivery/read, file-transfer, voice, call, online, or security
fact is reconstructed from color, icon, or reference copy.

P18 is a full-width Android bottom sheet opened by the existing real chat-item long press. It
reuses the official reaction, reply, share, copy, edit, forward, info, delete, selection, and
conditional moderation/report callbacks. Extra official actions remain scrollable even when the
reference depicts a shorter list. Direct chat never gains a report action: report remains visible
only under the existing exact group-message Reports/member-role gate. Desktop continues to use its
established anchored menu.

The API 35 Chinese/light production comparison uses a real direct contact, an actually received
controlled-client message, and messages actually submitted through the production composer.
P17 and P18 match the reference hierarchy, toolbar/banner placement, green/neutral palette,
message surfaces, composer, scrim, sheet geometry, icon tiles, row density, and destructive color.
The baseline's file and voice examples are not fabricated in visual evidence. Their transfer,
recording/playback, permission, and lifecycle coverage remains in the separate high-risk
media/file P1 family; that family now has controlled synthetic image, AAC, and H.264/AAC
peer-receipt evidence with exact bytes/digest and production playback owners. Calls remain in the
high-risk call family.

## Milestone 3 P19/P20 production boundary

P19 is the existing direct-contact security-code route. `ChatInfoView` still requests the code,
`VerifyCodeView` still owns the exact code plus scan/share/verify/clear actions, and the returned
`Contact` remains authoritative for verification state. Nome changes only the Android contact
presentation through `PlatformVerifyCodeLayout`; group-member verification and Desktop continue
to use the official v6.5.6 layout.

The P19 Android page follows the baseline's compact contact card, verification strip, explanatory
copy, bordered QR/code card, two-step comparison hierarchy, and primary scan action. The full real
code is shown and shared; no digits are invented or dropped. A typed client result keeps matched,
mismatched, and unavailable outcomes distinct. Manual attestation remains a separate explicit
second step and cannot masquerade as a scanner match.

Two controlled clients on API 28/API 35 produced the same real security code. Both completed
manual mark, verified-state reopen, explicit clear, and scanner open/cancel lifecycle. The API 35
Chinese/light production comparison matches P19 at the reference viewport while retaining the
actual contact name, profile fallback, complete code density, and official share action.

P20 retains `AddChannelView` as the owner of profile input, enabled relay selection, public-group
creation, real `GroupInfo`/`GroupLink` progression, channel relay state, and cancellation. The
Android setup seam applies the baseline composition: create/join tabs, name/image field,
post-creation link disclosure, relay summary/configuration rows, fixed public-channel warning,
current-profile sharing note, and primary create action. Join routes to the existing P12
scan/paste flow; relay settings open the existing configuration route.

The reference's custom Nome domain and generic “available relays” claim are not implemented.
Before creation the link region says the official link is generated after creation, and the relay
region reports only the count of currently enabled configured relays. Public channels retain the
fixed non-E2EE disclosure. Local cancellation finalization runs only after the existing delete
command returns true; false or exception retains local state.

The API 35 Chinese/light setup page passed its production comparison with actual profile and
configured-relay facts. Earlier controlled attempts returned official connection errors and remain
negative evidence. A later bounded run returned a real group, non-empty official link, and three
relay-progression results. A fresh single-process lifecycle then created, populated, opened, and
officially deleted a controlled channel, mirrored the official UI's post-success model removal,
and confirmed list absence. The retained local 154-byte attachment row is presentation proof only,
not remote upload, delivery, observer receipt, or availability. P20 is ready for the milestone.

## Milestone 3 P21/P22 production boundary

P21 stays inside the official loaded public-group chat. `ChatView` derives the fixed non-E2EE
disclosure only from a real base channel whose `GroupInfo.useRelays` fact marks the public-channel
route. Read-only observer treatment remains gated only by the real current-member role. On
Android, actual simple text/file message content is projected into the P21 post-card composition;
eligible file posts embed the official `CIFileView`, preserving its real receive, wait, progress,
error, open, save, and callback behavior inside the Nome composition. System/lifecycle items
retain their official legacy renderer rather than becoming fabricated posts. The timeline,
member/history facts, composer, profile route, relay operations, and moderation eligibility
remain official owners, while Desktop keeps the established layout. The real API 35 owner state
was compared at the Chinese/light reference size after structure, history inset, card geometry,
and attachment-color corrections. That comparison closes the owner presentation only:
its official composer is intentionally not relabelled as the reference's observer-only state, so
it does not close the observer-only visual baseline. Bounded observer attempts did not prove
membership or receipt; that baseline remains a declared external `NOT REACHABLE LOCALLY` gate
rather than accepted evidence.

P22 keeps `UserProfilesView` and the existing users/controller preferences as the identity source.
`PlatformIdentityCenterRoute` changes only the Android composition to match the baseline:
compact header actions, active-identity card, inactive-identity rows, privacy/network controls,
and bottom navigation. Add, edit, activate, hide/unhide, mute/unmute, and delete continue to invoke
the official callbacks. The incognito switch is the real connection-default preference, not a
persistent anonymous identity. The SOCKS row displays the actual stored network setting and opens
the official Network settings/confirmation instead of manufacturing private-routing or health
state.

Identity deletion retains the official switch-before-delete or delete/clear/stop command order,
but the presentation layer now records the exact failure stage. Local wallpaper, profile-row, and
notification cleanup occurs only after the delete command has returned successfully. A switch or
delete failure keeps the target identity available for retry; a post-delete refresh/stop failure
reports restart reconciliation without claiming that the target still exists. Cancellation is
re-thrown. Controlled API 28 and API 35 production clients created disposable local identities,
deleted the active identity through the real UI, removed an extra inactive API 28 fixture, and
cold-started with the fallback identity active and the deleted names absent. This does not change
identity, database, authentication, or core semantics.

## Milestone 3 P1 group and channel administration boundary

Private-group creation keeps `AddGroupView` as the owner of display-name/image validation,
incognito preference, official create command, returned `GroupInfo`, chat opening, and post-create
member setup. Android replaces only the modal composition with a compact full-page profile,
decentralization disclosure, incognito card, and primary action aligned to the adjacent P20
baseline. Desktop renders the exact v6.5.6 composition.

`AddGroupMembersView` remains the owner of the real eligible-contact producer, member-role choice,
member-admission and group-preference routes, selected contacts, invite/skip commands, result
handling, and cancellation. Android presents those owners as profile/setup/contact cards. The
group-info quick invitation action uses this owned callback instead of the older global modal
wrapper, preventing a second one-hand navigation bar. Unchecked contact rows do not announce the
checked-contact label.

`GroupChatInfoView` and `GroupProfileView` retain the official alias, notification, search, group
link, membership, moderation, preferences, image, validation, save, and destructive-confirmation
owners. Android changes only composition, density, top-level Back ownership, and automatic focus;
Desktop remains legacy. Channel members and relays retain the real group/member/relay list, role,
status, refresh, and tap owners. The v6.5.6 relay Add/Remove UI remains commented behind
`TODO [relays]`, so Nome does not surface unavailable relay mutation.

The controlled API 35 production client created one real private group with a synthetic local
name. No contact was invited, no message was sent, and no destructive action was invoked in that
private-group capture. Creation and group-info primary states passed Chinese/light/reference-size
adjacent P20/P16 comparisons. Invitation and profile production routes each expose one top Back
owner; the profile route does not force the IME. A later controlled real public channel closed the
separate P20 owner create/link/populate/open/delete/absence lifecycle and supplied a P21 owner
presentation comparison. It does not close the observer-only baseline. Observer membership/post
receipt remains explicitly not verified and is not inferred from either the private-group or
owner-channel evidence.

## Milestone 3 P23/P24 production boundary

P23 keeps `SettingsView` as the official owner of notification, database/archive, migration,
desktop, privacy, network, language/appearance, help, about, and developer routes.
`PlatformSettingsHomeRoute` replaces only the Android composition with the baseline header,
identity card, indexed official rows, local route search, and bottom navigation. Appearance and
developer remain searchable without adding baseline-incompatible default rows. The effective
system locale is shown when no explicit app locale exists. Desktop renders the established
v6.5.6 settings layout.

Android settings-detail pages use `PlatformSettingsDetailRoute` only as a presentation seam.
Network retains the official operator/server/SOCKS/Tor/preferences and unsaved-close owners, and
its status label is unknown before the first Android platform observation and then reflects only
that device-network value rather than the legacy optimistic default or relay health.
Appearance retains the official locale/theme/preferences. Help and Developer retain their
official content/actions/preferences. Settings About uses the packaged Nome wordmark and build
version, dispatches the existing live app/core-version owner, and links to the official SimpleX
source and exact v6.5.6 AGPL license. The onboarding About/`FIRST_USE` composition is unchanged.
Nested detail Back returns to P23; Back while P23 search is active clears only the local search.

The one-time Android locale adapter is now closed independently of Appearance presentation.
Focused policy tests pass 5/5. A clean disposable API 35 install proved absent marker,
application preferences, and databases before first process start, then produced `zh-CN` plus
marker version 1. The official Appearance owner changed that synthetic client to English; process
restart and same-package `install -r` retained English and both databases. The authentic
v6.5.4-upgrade fixture retained an absent `AppLanguage`/follow-system state after the latest
same-package install. A disposable unsupported-value check emitted a fixed non-personal warning,
did not overwrite the stored value or crash, and restored the fixture to English.

Remote desktop remains the official `ConnectDesktopView` state machine and controller flow.
Android presents its real connect/search/verify/connected states through the same full-page,
grouped settings-detail hierarchy as P23, with the title derived from the current session state;
Desktop keeps the established modal and internal title. The visible unpaired QR-camera state is
not pairing success. Device naming, discovery, address connect, verification, switch-to-local,
disconnect, and linked-device removal keep their existing owners. Linked-device removal now
requires the already-defined destructive confirmation before invoking that owner. The typed
session address is cleared only after an actual connect result, and disconnect clears or switches
the official remote session as before.

A release-isolated controlled API 28 remote-host/API 35 Android-controller lifecycle now closes
the producer boundary without using the unpaired scanner as evidence. The official owners produced
the invitation, matching session-code digests, connected host/controller state, local-session
switch, stop, delete, and final absence from both official lists. The invitation crossed only an
ephemeral host-memory relay, no code or invitation was logged or retained, temporary loopback
forwarding was removed, and no Desktop presentation was changed.

Android fullscreen modals are visually opaque routes, so the underlying Home tree is also hidden
from accessibility while a fullscreen modal is open; the active modal, switching overlay, and
authentication overlay retain their own semantics and layering. This changes no Home state,
modal stack, route, or Desktop composition.

`MainActivity` offers both first-create and `singleTask` warm intents to the same ordered
notification, external-`ACTION_VIEW`, and share handlers. This restores the existing notification
action owner for warm intents without changing any action string, payload, user/chat selection,
share content, lock gate, or task-stack/Back rule. External text share still opens the official
share list and its existing Back path may finish the externally launched activity.

P24 is a presentation landing over the official `DatabaseView` and authenticated outbound
`MigrateFromDeviceView`. Before a real operation starts it reports only a not-started state. It
does not reproduce the baseline's illustrative percentage, completed stages, generic cancel,
resume, restore, rollback, connectivity, or success claims. System Back returns to P23; nested
database and migration owners retain their established actions and confirmations. When chat is
stopped, the archive/database recovery route stays reachable for its official start control while
outbound migration is disabled, matching the official v6.5.6 settings-state boundary.

Real disposable API 28/API 35 clients verified export cancellation, saved encrypted archives,
cold-start restart, destructive import with database-key re-entry, pre/post-export data
round-trip, identity preservation, and temporary-snapshot cleanup. Android-created database
archives declare `application/zip`, so the API 28 DocumentsUI import picker can select the file;
the archive bytes, filename convention, format, import order, and database semantics are
unchanged. Export now returns the real completed-snapshot outcome to the existing stop/run/start
wrapper, preventing a successful or cancelled destination chooser from stranding chat stopped;
copy/delete cleanup remains owned by the chooser result.

Outbound migration on both APIs reached the official upload stage after real key verification,
survived background/foreground and rotation, and was aborted through the existing Back path.
Both controlled attempts remained at the observed `0 bytes uploaded` / `0%` result and therefore
are not described as online or successful. Abort removed migration temporary files, restarted
chat, and preserved the disposable identity/data across cold start. No Haskell/native core,
`Core.kt`, protocol, database semantic, archive format, command order, iOS behavior, or Desktop
presentation changed.

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

Ordinary UI groups require focused tests, Android/Desktop compilation where affected, API 35
production smoke, one primary reference-size/language production comparison per page family,
affected 48dp/accessibility inspection, and one risk-oriented review. Database, authentication,
security verification, backup/migration, file, and call groups retain real fixtures, lifecycle
checks, and API 28/API 35 depth. Full API 28/33/35, bilingual light/dark, 200%, TalkBack,
historical-manifest, release, and regression matrices run at milestones/final. Only final RC
requires two consecutive same-summary zero-issue reviews.

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

The foundation, P01–P10, P13, and Milestones 1–2 remain frozen. First use remains
onboarding-owned; only P09 closes the filtered-no-result producer gap. P11/P12, P14/P15, and P16
remain in Milestone 3 with the producer boundaries recorded above; P12/P15 are ready, while P11
and P14 are also ready with real production states. P16 is functionally ready for its real private
invitation state, while the reference's public-invitation visual state is `NOT REACHABLE LOCALLY`.
The P17 conversation
  shell and P18 action sheet are implemented; real controlled image-file transfer, connected
  audio-call lifecycle, and fixed synthetic voice/video peer receipt plus production playback
  pass. P19 is implemented. P20/P21 are ready with a real public-channel
  create/link/populate/open/delete/absence lifecycle and accepted owner-state visual comparison.
  The reachable private-group creation/admin and channel owner/admin presentations are ready for
  the milestone. P22/P23/P24 are
implemented and ready for the milestone.

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

P11/P12, P14/P15, and P16 tests are recorded in that same completion root. P11 and P12 production
comparisons are closed; P11 retains only bearer-redacted READY artifacts and does not infer peer
use. P14 has a real pending-request production comparison in addition to its renderer calibration.
P15 has real OFF/READY production
proof and a redacted READY comparison; this does not imply that the address was shared, used, or
connected. P16 has real invitation/join/message proof and a private-production structural
calibration; the reference's public-invitation visual state is not claimed as accepted.

P19/P20 source, focused tests, production runs, comparison images, and privacy restoration are
recorded in that completion root. P19 has real API 28/API 35 two-client verification lifecycle
proof. P20 has accepted setup visual evidence plus the later real controlled
create/link/populate/open/delete/absence lifecycle. Earlier relay failures remain negative evidence.

P21/P22 source, focused tests, production comparisons, identity lifecycle, and privacy restoration
are recorded in the same completion root. P21 verifies the real API 35 owner route only; its local
attachment is not remote-file proof and failed observer attempts are not promoted. The owner
comparison is not acceptance of the reference's observer-only state, which remains externally
`NOT REACHABLE LOCALLY`. P22 has real API 28/API 35 create/switch/delete/cold-start reconciliation
proof.

The P17 high-risk media evidence in the same completion root uses only controlled synthetic
fixtures: a 5,413-byte AAC and a 6,016-byte H.264/AAC file. API 35 official send and API 28 peer
receive verified exact decrypted SHA-256 and stable bytes, then the production audio/video players
reached real progress/termination and stopped; controlled residues and temporary server selection
were cleaned/restored. This proves only those two controlled items, not generic availability,
delivery, network health, or online state.

The private-group creation/admin ordinary group is recorded in the same completion root. Its
focused tests, Android/Desktop compilation, real API 35 private-group production routes,
creation/admin adjacent comparisons, invitation/profile captures, Back/accessibility inspection,
single review, and exact screenshot-protection restoration close the reachable group family.
Channel-admin source reuses the same presentation seam, but no private-group fixture is promoted
as a public-channel production state.
