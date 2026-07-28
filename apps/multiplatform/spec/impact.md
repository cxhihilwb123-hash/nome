# SimpleX Chat Android & Desktop -- Impact Graph

> Source file to product concept mapping. Use this to identify which product documents must be updated when a source file changes.
>
> Covers Kotlin Multiplatform (Compose) sources: commonMain, androidMain, desktopMain, and the Android and Desktop app modules. Also covers the shared Haskell core.

---

## Product Concept Legend

| ID | Concept |
|----|---------|
| PC1 | Chat List |
| PC2 | Direct Chat |
| PC3 | Group Chat |
| PC4 | Message Composition |
| PC5 | Message Reactions |
| PC6 | Message Editing |
| PC7 | Message Deletion |
| PC8 | Timed Messages |
| PC9 | Voice Messages |
| PC10 | File Transfer |
| PC11 | Link Previews |
| PC12 | Contact Connection |
| PC13 | Contact Verification |
| PC14 | Group Management |
| PC15 | Group Links |
| PC16 | Member Roles |
| PC17 | Audio/Video Calls |
| PC18 | Notifications |
| PC19 | User Profiles |
| PC20 | Incognito Mode |
| PC21 | Hidden Profiles |
| PC22 | Local Authentication |
| PC23 | Database Encryption |
| PC24 | Theme System |
| PC25 | Network Configuration |
| PC26 | Device Migration |
| PC27 | Remote Desktop |
| PC28 | Chat Tags |
| PC29 | User Address |
| PC30 | Member Support Chat |
| PC31 | Channels (Relays) |
| PC32 | Nome Android UI |

PC32 has an exact Android-only Phase 2 design foundation, frozen Batch 2 P07/P08 and Batch 3 P13
implementations, frozen Milestones 1–2 through P10, and an implemented Milestone 3 presentation
scope through P24 plus Android-reachable P1 families. The concentrated Milestone 3 non-producer
matrix is green; external producer results remain separately open.
The rows below keep the larger transitive integration scope for later pages and separately
enumerate the exact foundation, Batch 2, Batch 3, and Batch 1A source/test paths. Source presence is not
evidence that the current batch verification matrix has passed.

### PC32 Nome Android transitive scope (authoritative reverse index)

The detailed rows in sections 1–4 continue to identify each source file's **primary** product concepts. The table below remains the authoritative supplementary reverse index for existing Android-consumed behavior that the current shell and later Nome pages must preserve. Inclusion means transitive review and validation scope, not that every listed file will be edited or that the current batch implements the corresponding feature.

Path aliases are relative to `apps/multiplatform/`: `CM` = `common/src/commonMain/kotlin/chat/simplex/common`, `AM` = `common/src/androidMain/kotlin/chat/simplex/common`, `MR` = `common/src/commonMain/resources/MR`, and `APP` = `android/src/main`.

| PC32 surface | Transitive source scope | PC32 boundary / reason |
|--------------|-------------------------|------------------------|
| App / root lifecycle | `CM/App.kt`; `CM/platform/AppCommon.kt`; `AM/platform/AppCommon.android.kt`; `APP/java/chat/simplex/app/SimplexApp.kt` | Preserve startup, gate ordering, navigation ownership, and Android application initialization. |
| ChatModel / SimpleXAPI / core bridge | `CM/model/ChatModel.kt`; `CM/model/SimpleXAPI.kt`; `CM/platform/Core.kt` | Preserve production state and command/event contracts; `Core.kt` is a bridge validation point only. Haskell/native core remains outside PC32 implementation scope. |
| AppLock / local authentication | `CM/AppLock.kt`; `CM/views/localauth/**`; `AM/views/helpers/LocalAuthentication.android.kt`; `AM/views/usersettings/PrivacySettings.android.kt` | Cover locked, authenticating, cancelled, failed, and unlocked states without bypassing authorization. |
| Database / migration | `CM/views/database/**`; `CM/views/migration/**`; `CM/views/onboarding/SetupDatabasePassphrase.kt`; `AM/{platform/Cryptor.android.kt,views/database/**,ui/nome/database/**}` | Cover create, encrypted, error, upgrade, bounded matched-pair recovery, import, export, and interrupted migration states without changing core/database truth. |
| Theme | `CM/ui/theme/**`; `AM/ui/theme/**`; `CM/views/usersettings/Appearance.kt`; `AM/views/usersettings/Appearance.android.kt` | Apply the approved Nome tokens in both light and dark modes while preserving theme resolution. |
| Locale / bilingual resources | `MR/**/strings.xml`; `CM/platform/Resources.kt`; `CM/platform/UI.kt`; `AM/helpers/Locale.kt`; `AM/platform/Resources.android.kt`; `AM/platform/UI.android.kt`; `APP/java/chat/simplex/app/nome/NomeLocaleInitializer.kt` | English and Chinese copy, runtime locale behavior, one-time clean-install initialization, text expansion, and resource fallback are cross-cutting PC32 requirements. |
| Onboarding | `CM/views/onboarding/**`; `AM/views/onboarding/**` | Cover every onboarding branch, permission result, database setup result, and restoration path. |
| Chat list | `CM/views/chatlist/**`; `AM/views/chatlist/**` | Cover loading, empty, populated, search, filters/tags, requests, errors, multi-user selection, and navigation. |
| New chat / ConnectPlan | `CM/views/newchat/**`; `AM/views/newchat/**` | Preserve URI/QR/address planning and all invalid, confirmation, pending, success, and failure outcomes. |
| Chat / items / compose | `CM/views/chat/*.kt`; `CM/views/chat/item/**`; `AM/views/chat/*.kt`; `AM/views/chat/item/**` | Cover message history, item variants, composer modes, attachment/voice states, actions, errors, and accessibility semantics. |
| Contacts | `CM/views/contacts/**`; `CM/views/chat/ChatInfoView.kt`; `CM/views/chat/VerifyCodeView.kt`; `CM/views/chat/ScanCodeView.kt` | Preserve contact browsing, information, verification, request, and failure states. |
| Groups / channels | `CM/views/chat/group/**`; `CM/views/newchat/AddGroupView.kt`; `CM/views/newchat/AddChannelView.kt` | Preserve membership, roles, links, relay/channel management, moderation, support, and all permission/error states. |
| Users / settings / network | `CM/views/chatlist/UserPicker.kt`; `AM/views/chatlist/UserPicker.android.kt`; `CM/views/usersettings/**`; `AM/views/usersettings/**` | Cover user/profile switching, hidden/incognito states, settings, operators, protocol servers, proxies, and offline/error states. |
| Calls | `CM/views/call/**`; `AM/views/call/**`; `APP/java/chat/simplex/app/views/call/CallActivity.kt` | Preserve incoming, outgoing, connecting, active, permission-denied, degraded, ended, and failed call states. |
| Files / media / share | `CM/model/CryptoFile.kt`; `CM/platform/{Files.kt,Images.kt,RecAndPlay.kt,Share.kt,VideoPlayer.kt}`; `AM/platform/{Files.android.kt,Images.android.kt,RecAndPlay.android.kt,Share.android.kt,VideoPlayer.android.kt}` | Preserve picker, preview, transfer, encryption, playback, cancellation, retry, and external share behavior. |
| Notifications / background | `CM/platform/{Notifications.kt,NtfManager.kt,SimplexService.kt}`; `AM/platform/{Notifications.android.kt,SimplexService.android.kt}`; `APP/java/chat/simplex/app/model/NtfManager.android.kt`; `APP/java/chat/simplex/app/{SimplexService.kt,MessagesFetcherWorker.kt}` | Preserve permission/mode/channel handling, deep-link routing, background delivery, retry, and process-recovery behavior. |
| Remote desktop | `CM/views/remote/**` | Preserve connect-mobile/connect-desktop states and errors; PC32 remains Android-only and does not authorize desktop UI changes. |
| MainActivity / intents | `APP/AndroidManifest.xml`; `APP/java/chat/simplex/app/MainActivity.kt`; `APP/java/chat/simplex/app/SimplexApp.kt`; `APP/java/chat/simplex/app/views/helpers/Util.kt` | Preserve lifecycle, external/deep-link/share intents, task restoration, and routing into the real shared model. |
| Permissions | `APP/AndroidManifest.xml`; `common/src/androidMain/AndroidManifest.xml`; `AM/helpers/Permissions.kt`; `AM/views/newchat/QRCodeScanner.android.kt`; `AM/views/onboarding/SetNotificationsMode.android.kt` | Cover first request, grant, denial, permanent denial, rationale, settings return, and unavailable-device states. |
| Android services / workers | `APP/java/chat/simplex/app/{SimplexService.kt,CallService.kt,MessagesFetcherWorker.kt}`; `APP/AndroidManifest.xml` | Preserve declared foreground-service/work scheduling, lifecycle, restart, cancellation, and error paths. |

### PC32 Phase 2 exact foundation sources

| Exact source scope | Product concepts | Risk | Boundary |
|---|---|---|---|
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/tokens/*.kt` | PC24, PC32 | Medium | Android-only semantic light/dark colors, typography, dimensions, shapes, and elevation; no model or protocol truth. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeTheme.kt` | PC24, PC32 | Medium | Material 2 adapter and CompositionLocal for the isolated Nome foundation; it does not replace the shared production theme resolver. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/*.kt` | PC32 | Medium | Parameterized surface, 48dp button, and seven-state panel; no hard-coded product state or visible copy. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/accessibility/*.kt` | PC32 | Medium | TalkBack semantics and minimum-target modifiers. |
| `android/src/debug/{AndroidManifest.xml,java/chat/simplex/app/nome/**,res/values*/strings.xml}` | PC32 | Low | Debug-only deterministic zh-CN/en, light/dark, state, 200% font, Preview, and native screenshot harness; the activity is not exported. |
| `android/src/test/java/chat/simplex/app/nome/**` | PC32 | Low | Pure foundation enum/contract checks. |
| `android/src/androidTest/java/chat/simplex/app/nome/**` | PC24, PC32 | Medium | Compose semantics/contrast/48dp/200%, minSdk/package isolation, and API 35 native screenshot suites. |
| `android/build.gradle.kts`; `common/build.gradle.kts` | PC32 | Medium | minSdk 28 plus AndroidX Compose test/tooling support; no native rebuild. |

All PC32 implementation groups that touch this scope route through `spec/client/nome-android-ui.md`
and `product/views/nome-android.md`. Ordinary UI groups use focused tests/compile, API 35
production smoke, one primary side-by-side page-baseline comparison, affected accessibility, and
one review; full device/language/theme/accessibility/release matrices run at milestones/final.
Broad Desktop, iOS, and Haskell/native core work is not an implementation target.

### PC32 Phase 2 Batch 2 exact production sources

The only Desktop implementation change is the required fallback actual for the shared seam; it preserves the existing Desktop chat list and is not a Desktop redesign.

| Exact source or test path | Product concepts | Risk | Batch 2 responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/App.kt` | PC1, PC32 | High | Calls the original notice effect and platform seam only in the existing normal home branch; delivery-receipt and share branches remain upstream-owned. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt` | PC1, PC27, PC32 | High | Extracts the existing automatic WhatsNew/updated-conditions side effect so it runs above either platform home renderer; official Desktop list behavior remains otherwise unchanged. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.kt` | PC1, PC32 | Medium | Narrow `expect` seam; owns no Nome UI, navigation, model, or protocol state. |
| `common/src/commonMain/kotlin/chat/simplex/common/model/ChatModel.kt` | PC1, PC19, PC27, PC32 | High | Defines generation-scoped chat-list load state/result and rejects stale-user/host results before replacing rows. |
| `common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt` | PC1, PC19, PC27, PC32 | High | Preserves the core command while returning typed success/failure/no-current-user results to load callers. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/database/DatabaseView.kt` | PC1, PC23, PC26, PC32 | High | Refreshes chats through the same typed load/result path after TTL maintenance. |
| `common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt` | PC1, PC25, PC32 | Medium | Exposes nullable first-observation Android connectivity truth separately from the legacy optimistic model default. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeStateAdapter.kt` | PC1, PC32 | High | Pure P07/P08 derivation for content, connectivity, core, visible rows, and matching-generation cached-row truth. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt` | PC1, PC24, PC32 | High | Android actual and read-only P07/P08 renderer; preserves preview privacy, notification setup, user-picker overlay, and guarded existing chat-open navigation including pending-deletion suppression. |
| `common/src/androidMain/res/values/nome_home_strings.xml` | PC1, PC32 | Low | English production home copy and semantics. |
| `common/src/androidMain/res/values-zh-rCN/nome_home_strings.xml` | PC1, PC32 | Low | Simplified-Chinese production home copy and semantics. |
| `android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt` | PC1, PC24, PC32 | Medium | Android Activity-owned production host around the unchanged shared root. |
| `android/src/main/java/chat/simplex/app/MainActivity.kt` | PC1 through PC32 | High | Installs `NomeProductionShell` without replacing intent, lifecycle, lock, call, or root routing. |
| `android/src/main/java/chat/simplex/app/SimplexApp.kt` | PC1, PC18, PC19, PC27, PC32 | High | Uses the typed generation-scoped refresh when the process returns to foreground. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRoute.desktop.kt` | PC1, PC32 | Medium | Desktop actual invokes `defaultContent` unchanged. |
| `android/src/test/java/chat/simplex/app/nome/home/NomeHomeStateAdapterTest.kt` | PC1, PC25, PC32 | Medium | Unit truth table for loading/empty/failure/filter/stale generation/network/core separation. |
| `android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeComposeTest.kt` | PC1, PC24, PC32 | Medium | Compose semantics, preview privacy, pending-deletion/read-only behavior, 48dp, live-region transition, and 200% state visibility coverage. |
| `android/src/debug/AndroidManifest.xml` | PC32 | Medium | Registers the Batch 2 evidence Activity as debug-only and non-exported alongside the frozen foundation harness. |
| `android/src/debug/java/chat/simplex/app/nome/home/NomeHomeEvidenceActivity.kt` | PC1, PC24, PC32 | Medium | Deterministic debug host for the production renderer; visibly labels fixtures as not live core and supplies localized, stable cross-year row-time input. |
| `android/src/debug/res/values/nome_home_evidence_strings.xml` | PC32 | Low | English debug-only evidence labels. |
| `android/src/debug/res/values-zh-rCN/nome_home_evidence_strings.xml` | PC32 | Low | Simplified-Chinese debug-only evidence labels. |
| `android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomePackagingTest.kt` | PC32 | Medium | Guards presence/non-export of the debug evidence Activity. |
| `android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeScreenshotTest.kt` | PC1, PC24, PC32 | Medium | Defines the verified API 35 renderer matrix: 10 states × 2 locales × 2 themes × 2 font scales = 80 captures, with timezone and cross-year clock preconditions. |
| `android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeCoreCycleTest.kt` | PC1, PC32 | Medium | Explicit argument-gated API 35 real-core stop/start evidence gate; asserts exact non-empty cached chat IDs across both official transitions. |
| `android/src/androidTest/java/chat/simplex/app/ExampleInstrumentedTest.kt` | PC32 | Low | Verifies the configured application ID instead of assuming the upstream package literal. |
| `common/src/desktopTest/kotlin/chat/simplex/common/views/chatlist/PlatformHomeRouteDesktopTest.kt` | PC1, PC32 | Medium | Executes the Desktop actual in a runtime composition and observes invocation of the unchanged fallback content. |

The pure renderer includes first-use and filtered-no-result branches, but the production root
consumes no-user in onboarding. The later P09 producer makes filtered-no-result reachable only for
an active nonblank query over an available loaded base; this does not change the frozen P08 result.

### PC32 Milestone 2 P09/P10 exact production sources

| Exact source or test path | Product concepts | Risk | P09/P10 responsibility |
|---|---|---|---|
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt` | PC1, PC24, PC32 | High | Adds P09 query ownership, guarded result navigation, and P10 Home entry over frozen Home state. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeSearchStateAdapter.kt` | PC1, PC32 | Medium | Groups only official filtered direct/group/channel/note rows. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeSearchRoute.android.kt` | PC1, PC24, PC32 | Medium | P09 visual-acceptance route, truthful scope/policy sections, 48dp actions, and back behavior. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt` | PC1, PC12, PC32 | High | Reuses the existing New Chat modal and optionally returns to the official user picker. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/newchat/{NewChatSheet.kt,PlatformNewChatHub.kt}` | PC12, PC19, PC31, PC32 | Medium | Carries actual profile display data and the existing four action closures through the P10 seam. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.android.kt` | PC12, PC19, PC24, PC31, PC32 | Medium | Android P10 visual-acceptance renderer. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatHub.desktop.kt` | PC12, PC32 | Low | Invokes the legacy New Chat content unchanged. |
| `android/src/androidTest/java/chat/simplex/app/nome/{home,newchat}/**` | PC1, PC12, PC24, PC32 | Medium | Focused grouping, truth-boundary, 48dp/back, and one-to-one callback tests. |

Batch 2 does not add favorite mutation, profile-switch redesign, connection mutation, search/filter UI, composer/send behavior, locale-marker persistence, or any Haskell/native core, database-format, protocol, command, or event change. Those surfaces remain transitive preservation scope only.

### PC32 Milestone 3 P11/P12 exact production sources

This group changes Android presentation and camera lifecycle around existing New Chat owners.
Common files carry the smallest platform seam and extracted reuse of the official parser/plan
action; Desktop delegates the legacy content unchanged.

| Exact source or test path | Product concepts | Risk | P11/P12 responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/views/newchat/NewChatView.kt` | PC12, PC29, PC32 | High | Retains `apiAddContact`, invitation disposal, parser, `planAndConnect(..., Legacy)`, and navigation ownership while passing existing facts/actions to the platform seam. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/newchat/NewChatSheet.kt` | PC12, PC24, PC32 | Medium | Opens the Android child route without the upstream bottom app bar while Desktop retains its established modal path. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatRoute.kt` | PC12, PC19, PC24, PC32 | Medium | Presentation-only P11/P12 expect seam; carries official link/profile/input facts and existing callbacks without bearer persistence. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatRoute.android.kt` | PC12, PC19, PC24, PC31, PC32 | High | Android P11/P12 visual-acceptance renderer, explicit clipboard/camera actions, 48dp semantics, and local-action truth boundary. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformNewChatRoute.desktop.kt` | PC12, PC32 | Low | Invokes the official legacy content unchanged. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/newchat/QRCodeScanner.android.kt` | PC12, PC24, PC32 | High | Optional-camera, explicit permission, denial/Settings recovery, analyzer-frame closure, and executor disposal. |
| `common/src/androidMain/res/values*/nome_home_strings.xml` | PC12, PC19, PC24, PC32 | Low | Bilingual P11/P12 fixed copy with no TTL, regeneration, peer-use, or success claims. |
| `android/src/main/AndroidManifest.xml` | PC24, PC32 | Medium | Declares camera capability optional while retaining the existing camera permission. |
| `android/src/androidTest/java/chat/simplex/app/nome/newchat/NomeNewChatRouteComposeTest.kt` | PC12, PC24, PC32 | Medium | Focused one-to-one callback, explicit scanner/clipboard activation, unsupported-copy, and 48dp contracts. |

P11/P12 add no command, protocol/event type, database/archive behavior, native/core change, iOS
behavior, or Desktop Nome UI. P12 internal input retains the legacy plan route and does not broaden
P13's external-`ACTION_VIEW` opt-in.

### PC32 Milestone 3 P14/P15 exact production sources

This group changes Android presentation and adds discriminated client results over existing
request/address commands. Common files retain official model mutation and route ownership; each
Desktop actual preserves legacy presentation.

| Exact source or test path | Product concepts | Risk | P14/P15 responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt` | PC12, PC29, PC32 | High | Adds typed reject and address-lookup projections over unchanged commands; compatibility wrappers remain. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/{ChatListNavLinkView.kt,PlatformContactRequestRoute.kt}` | PC1, PC12, PC20, PC32 | High | Retains official accept/reject/chat-list mutation and offers the Android presentation seam only from the existing request owner. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/chatlist/PlatformContactRequestRoute.android.kt` | PC12, PC20, PC24, PC32 | Medium | P14 requester/current/incognito/reject baseline presentation, single-submit, failure retention, and busy-back behavior. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformContactRequestRoute.desktop.kt` | PC12, PC32 | Low | Declines the Nome route and preserves the legacy request alert. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/{UserAddressView.kt,PlatformUserAddressRoute.kt}` | PC19, PC29, PC32 | High | Retains lookup/create/settings/short-link/share/profile/delete owners, explicit replace phases, and official onboarding layout. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/usersettings/PlatformUserAddressRoute.android.kt` | PC24, PC29, PC32 | Medium | P15 OFF/READY/failure baseline presentation, confirmations, create-only replacement recovery, and bearer-safe semantics. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/usersettings/PlatformUserAddressRoute.desktop.kt` | PC29, PC32 | Low | Invokes the official legacy address content. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/{NomeFullPageScaffold.kt,NomeButton.kt}` | PC24, PC32 | Medium | Shared page scaffold and primary/secondary/destructive visual variants used by the baselines. |
| `common/src/androidMain/res/values*/nome_connections_strings.xml` | PC12, PC20, PC29, PC32 | Low | Fixed bilingual P14/P15 copy without request-message, atomic-replace, rollback, online, or success claims. |
| `android/src/androidTest/java/chat/simplex/app/nome/connection/{NomeContactRequestComposeTest.kt,NomePublicContactMethodComposeTest.kt}` | PC12, PC20, PC24, PC29, PC32 | Medium | Focused callback, single-submit, failure, confirmation/retry, bearer semantics, and 48dp contracts. |
| `android/src/androidTest/java/chat/simplex/app/nome/connection/NomeConnectionManagementScreenshotTest.kt` | PC24, PC29, PC32 | Low | API 35 Chinese/light renderer calibration only; not a production request/address producer. |

P14/P15 add no command, protocol/event type, database/archive behavior, native/core change, iOS
behavior, or Desktop Nome UI. The fixture comparison cannot satisfy producer-backed production
acceptance, and P15 OFF cannot be promoted to READY.

### PC32 Milestone 3 P16 exact production sources

This group changes Android presentation and adds a discriminated client result over the existing
group-join command. Common files retain the official invited-group route and model mutation;
Desktop declines the Nome seam and preserves the legacy alert.

| Exact source or test path | Product concepts | Risk | P16 responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt` | PC14, PC16, PC32 | High | Adds accepted/unavailable/not-completed projection over the unchanged group-join command; the compatibility wrapper remains. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/{ChatListNavLinkView.kt,PlatformGroupInvitationRoute.kt}` | PC1, PC14, PC16, PC20, PC32 | High | Retains the invited-group owner, join/delete mutation, real inviter lookup, and Android presentation opt-in. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/chatlist/PlatformGroupInvitationRoute.android.kt` | PC14, PC16, PC20, PC24, PC32 | Medium | P16 group/profile/inviter/action baseline presentation, single-submit, failure retention, destructive confirmation, and busy-back behavior. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/chatlist/PlatformGroupInvitationRoute.desktop.kt` | PC14, PC32 | Low | Declines the Nome route and preserves the official group-invitation alert. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeFullPageScaffold.kt` | PC24, PC32 | Medium | Shared baseline page scaffold; owns no group or command truth. |
| `common/src/androidMain/res/values*/nome_connections_strings.xml` | PC14, PC16, PC20, PC32 | Low | Fixed bilingual P16 copy without verified-admin, joined, connected, online, relay-health, or success claims. |
| `android/src/androidTest/java/chat/simplex/app/nome/connection/NomeGroupPreviewComposeTest.kt` | PC14, PC16, PC20, PC24, PC32 | Medium | Focused accepted/failure callback, single-submit, busy-back, destructive confirmation, retention, and 48dp contracts. |
| `android/src/androidTest/java/chat/simplex/app/nome/connection/NomeConnectionManagementScreenshotTest.kt` | PC14, PC16, PC24, PC29, PC32 | Low | API 35 Chinese/light P14/P15/P16 renderer calibration only; not a production invitation producer. |

P16 adds no command, protocol/event type, database/archive behavior, native/core change, iOS
behavior, or Desktop Nome UI. A real controlled `MemInvited` state passed through the official
archive/import owner to disposable API 35. It closes private production/function proof and
calibrates shared structure only. The reference is a materially different public invitation, so
that reference visual state is terminally `NOT REACHABLE LOCALLY`, not accepted from the retained
comparison. This does not manufacture join or connection success.

### PC32 Milestone 3 P17/P18 exact production sources

This group changes Android conversation and message-action presentation around the existing loaded
chat, item, composer, and callback owners. The shared changes are platform-guarded; Desktop keeps
the established wallpaper/layout/action menu.

| Exact source or test path | Product concepts | Risk | P17/P18 responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/views/chat/ChatView.kt` | PC2, PC3, PC4, PC6, PC7, PC8, PC9, PC11, PC32 | High | Retains the official loaded timeline/composer/navigation, uses actual direct E2EE items for the fixed Android banner, and applies the P17 composition without synthesizing facts. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/chat/item/{ChatItemView.kt,FramedItemView.kt,PlatformMessageActionsMenu.kt}` | PC2, PC3, PC4, PC5, PC6, PC7, PC8, PC32 | High | Retains item/action eligibility and callbacks, applies Android bubble/selection geometry, and declares the presentation-only P18 platform seam. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/chat/item/PlatformMessageActionsMenu.android.kt` | PC4, PC5, PC24, PC32 | Medium | Android P18 full-width scrollable action sheet, 48dp close/action rows, scrim, icon tiles, and destructive color. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/chat/item/PlatformMessageActionsMenu.desktop.kt` | PC4, PC32 | Low | Invokes the established anchored action menu unchanged. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/chat/{ComposeView.kt,SendMsgView.kt}` | PC2, PC8, PC9, PC32 | High | Retains attachment/voice/live/input/send owners while applying the P17 Android composer geometry. |
| `common/src/androidMain/kotlin/chat/simplex/common/platform/PlatformTextField.android.kt` | PC2, PC24, PC32 | Medium | Applies Android input padding/hint styling without changing text or IME ownership. |
| `common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt` and `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeTheme.kt` | PC24, PC32 | Medium | Preserve per-chat theme routing while installing Nome Android material/app colors, including neutral secondary text and message surfaces. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/helpers/DefaultTopAppBar.kt` | PC24, PC32 | Low | Allows the Android chat title to align to the P17 baseline while default callers remain centered. |
| `common/src/commonTest/kotlin/chat/simplex/common/views/chat/NomeConversationPresentationTest.kt` | PC3, PC32 | Medium | Proves newest-real-fact selection and fail-closed absence for the fixed E2EE presentation. |
| `android/src/androidTest/java/chat/simplex/app/nome/chat/NomeConversationActionsComposeTest.kt` | PC4, PC5, PC24, PC32 | Medium | Proves one-to-one official callbacks, dismiss-without-action, and 48dp sheet actions. |

P17/P18 add no command, protocol/event type, database/archive/message-state-machine behavior,
native/core change, iOS behavior, or Desktop Nome UI. File, voice, and call production/lifecycle
depth remains assigned to the separate high-risk Android-reachable P1 families.

### PC32 Milestone 3 P19/P20 exact production sources

This group changes Android contact-verification and public-channel setup presentation around the
existing security-code, scanner, relay, public-group, link, and delete owners. Desktop keeps the
official layouts. Earlier relay-timeout attempts remain historical; the later controlled P20
owner create/link/populate/open/delete/post-delete-absence lifecycle is verified.

| Exact source or test path | Product concepts | Risk | P19/P20 responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/views/chat/{ChatInfoView.kt,ChatView.kt,VerifyCodeView.kt,ScanCodeView.kt,PlatformVerifyCodeLayout.kt}` | PC2, PC13, PC32 | High | Retains direct-contact routing, exact code, scanner/share/mark/clear actions, authoritative returned-contact updates, and declares the Android presentation seam. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/chat/PlatformVerifyCodeLayout.android.kt` | PC13, PC24, PC32 | Medium | P19 baseline contact/status/QR/code/steps/actions layout with distinct scan and manual attestation and 48dp controls. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/chat/PlatformVerifyCodeLayout.desktop.kt` | PC13, PC32 | Low | Invokes the official verification layout unchanged. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/newchat/QRCode.kt` | PC12, PC13, PC32 | Medium | Adds an optional QR image-size presentation input; all existing callers retain default behavior and payload bytes stay unchanged. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/newchat/{AddChannelView.kt,NewChatSheet.kt,PlatformChannelSetupRoute.kt}` | PC12, PC14, PC15, PC31, PC32 | High | Retains official relay selection/create/progress/link/delete ownership, routes Join to the existing callback, and finalizes local cancellation only after confirmed deletion. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformChannelSetupRoute.android.kt` | PC14, PC24, PC31, PC32 | Medium | P20 baseline create/join, name/image, post-create link, configured-relay, warning, profile-sharing, and create-action composition without unsupported claims. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformChannelSetupRoute.desktop.kt` | PC14, PC31, PC32 | Low | Invokes the official channel profile step unchanged. |
| `common/src/androidMain/res/values*/nome_connections_strings.xml` and `common/src/commonMain/resources/MR/*/strings.xml` | PC13, PC14, PC31, PC32 | Low | Fixed bilingual P19/P20 copy and distinct verification-unavailable message; no code, domain, health, connection, encryption, or success fixture. |
| `common/src/commonTest/kotlin/chat/simplex/common/views/chat/NomeContactVerificationPolicyTest.kt` | PC13, PC32 | High | Proves all code characters are retained and unavailable is distinct from mismatch/match. |
| `common/src/commonTest/kotlin/chat/simplex/common/views/newchat/NomeChannelCancellationPolicyTest.kt` | PC14, PC31, PC32 | High | Proves local finalization occurs only after a true delete result and false/exception retain state. |
| `android/src/androidTest/java/chat/simplex/app/nome/connection/NomeSecurityAndChannelComposeTest.kt` | PC13, PC14, PC24, PC31, PC32 | Medium | Proves distinct scan/manual/clear callbacks, 48dp controls, official join/config/create dispatch, configured-relay/link truth, and no custom Nome domain. |
| `android/src/androidTest/java/chat/simplex/app/nome/lifecycle/{NomePublicChannelProducerTest.kt,NomeTwoClientPublicChannelTest.kt}` | PC14, PC31, PC32 | High | Real controlled lifecycle/observer diagnostics may mutate only when a caller-supplied canonical per-run UUID exactly matches the stored test-written nonce together with active user, remote host, and group id. Missing, malformed, or mismatched nonces fail closed. No display-name fallback exists; status exposes counts and controlled-record/channel booleans only. |

All API-35-only screenshot tests in the active review scope fail on a different API by default.
Only the explicit `nomeCrossApiScreenshotSkip=true` lower-API full-regression invocation skips
their capture bodies; standalone evidence commands remain fail-closed on a misrouted device.
Argument-driven controlled producer, bridge, network, archive, call, file, media, group,
public-channel, and remote lifecycle tests also fail when their primary action/role is absent or
invalid. Only the explicit `nomeControlledProducerSkip=true` general-regression invocation
bypasses those separately verified bodies. The resulting device matrix is ordinary UI/device and
fail-closed-boundary evidence, not proof that a controlled producer lifecycle executed.
The guarded P14 production capture separately fails closed on API 35 without its exact
fixed-fixture token. Only the explicit `nomeControlledProducerSkip=true` general-regression
invocation may bypass it, and that bypass is not capture evidence.

P19/P20 add no command, protocol/event type, database/archive behavior, native/core change, iOS
behavior, or Desktop Nome UI. P19 high-risk verification lifecycle is producer-backed on API 28
and API 35. P20 setup and the controlled real owner create/link/populate/open/delete/absence
lifecycle are accepted. They do not prove relay health or public-channel Observer receipt.

### PC32 Milestone 3 P21/P22 exact production sources

This group changes Android public-channel chrome and local-identity-center presentation around the
existing loaded-chat, group, profile, preference, route, and controller owners. Desktop keeps the
official layouts. P21 owner-presentation comparison is backed by the controlled real public
channel; it does not accept the reference's observer-only state. Observer membership and peer
receipt remain explicitly not verified.

| Exact source or test path | Product concepts | Risk | P21/P22 responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/views/chat/{ChatView.kt,ComposeView.kt,PlatformChannelConversationChrome.kt}` | PC2, PC3, PC4, PC14, PC32 | High | Retains the loaded channel timeline/composer/profile owners, derives disclosure from real `useRelays` and observer treatment from the real member role, and declares the Android presentation seam. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/chat/PlatformChannelConversationChrome.android.kt` | PC14, PC24, PC32 | Medium | Renders the P21 fixed non-E2EE disclosure, history separator, and observer read-only treatment without adding a channel, relay, member, moderation, or action result. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/chat/PlatformChannelConversationChrome.desktop.kt` | PC14, PC32 | Low | Declines the Android chrome and preserves official Desktop presentation. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/{chatlist/UserPicker.kt,usersettings/UserProfilesView.kt,usersettings/PlatformIdentityCenterRoute.kt}` | PC1, PC10, PC11, PC32 | High | Retains users, current-user, hidden/authentication, preference, navigation, and action callbacks while declaring the Android identity-center route. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/UserDeletionLifecycle.kt` | PC11, PC32 | High | Types the existing switch/delete/clear/stop stages, preserves their order, rethrows cancellation, and distinguishes target-deleted truth before local cleanup. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/usersettings/PlatformIdentityCenterRoute.android.kt` | PC10, PC11, PC24, PC32 | Medium | Renders the P22 active/inactive identity hierarchy, actual incognito/SOCKS settings, and official Home/Contacts/Settings actions with 48dp controls. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/usersettings/PlatformIdentityCenterRoute.desktop.kt` | PC10, PC11, PC32 | Low | Invokes the official profile list unchanged. |
| `common/src/androidMain/res/values*/nome_identity_strings.xml` and `common/src/commonMain/resources/MR/*/strings.xml` | PC10, PC11, PC24, PC32 | Low | Fixed bilingual P21/P22 and deletion-reconciliation copy without anonymous-account, private-routing, health, deletion, or success fixtures. |
| `common/src/commonTest/kotlin/chat/simplex/common/views/usersettings/UserDeletionLifecycleTest.kt` | PC11, PC32 | High | Proves switch/delete/clear/stop ordering plus pre/post-delete failure truth. |
| `android/src/androidTest/java/chat/simplex/app/nome/connection/NomeChannelIdentityComposeTest.kt` | PC10, PC11, PC14, PC24, PC32 | Medium | Proves P21 disclosure/history/observer presentation, P22 dispatch and toggle routing, 48dp controls, and no duplicate callback invocation. |

P21/P22 add no command, protocol/event type, database/archive/message-state-machine behavior,
native/core change, iOS behavior, or Desktop Nome UI. P22 destructive lifecycle is producer-backed
on API 28 and API 35. P21's real owner primary production state is functionally ready and retained
for owner-presentation comparison only. The reference's observer-only visual state and the
Observer membership/post-receipt result are a declared external validation gap and are not
inferred.

### PC32 Milestone 3 P23/P24 exact production sources

This group changes Android settings and backup/migration presentation around established
preference, archive/database-key, platform document, and outbound migration owners. Desktop keeps
its official layouts. Real archive/import and migration-abort lifecycle is verified only on
disposable API 28/API 35 data.

| Exact source or test path | Product concepts | Risk | P23/P24 responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/{SettingsView.kt,PlatformSettingsHomeRoute.kt}` | PC10, PC18, PC22, PC23, PC24, PC25, PC29, PC32 | High | Retains every official settings route and declares Android settings/backup presentation seams, local route search, nested Back, and Home/Contacts navigation. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/usersettings/PlatformSettingsHomeRoute.android.kt` | PC10, PC18, PC22, PC24, PC32 | Medium | Renders P23 settings and truthful P24 not-started landing without synthetic health, percentage, restore, rollback, resume, or success claims; disables outbound migration while chat is stopped without stranding archive recovery. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/usersettings/PlatformSettingsHomeRoute.desktop.kt` | PC10, PC18, PC22, PC32 | Low | Invokes the official legacy settings content unchanged. |
| `common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/views/usersettings/{PlatformSettingsDetailRoute*,PlatformAboutSettingsRoute*}` | PC10, PC18, PC22, PC24, PC32 | Medium | Adds Android-only full-page/grouped detail and truthful About presentation seams with nested Back; Desktop and onboarding retain legacy composition, and official preferences/commands/live-version owners remain unchanged. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/remote/ConnectDesktopView.kt` | PC18, PC27, PC32 | High | Retains official remote-session/controller owners, selects Android full-page/grouped presentation with state-derived title, preserves exact Desktop legacy content, and requires the existing destructive confirmation before linked-device removal. |
| `common/src/commonMain/kotlin/chat/simplex/common/App.kt` | PC1, PC2, PC10, PC27, PC32 | High | Keeps the Android fullscreen modal outside the semantics-cleared underlying root so an opaque modal cannot expose hidden Home actions; authentication and Desktop layering remain unchanged. |
| `android/src/main/java/chat/simplex/app/MainActivity.kt` | PC1, PC2, PC12, PC18, PC27, PC32 | High | Offers both cold and `singleTask` warm intents to the same existing notification, external-view, and share handlers in order; action/payload/back/lock semantics remain their official owners. |
| `android/src/main/java/chat/simplex/app/{SimplexApp.kt,nome/NomeLocaleInitializer.kt}` | PC1, PC18, PC24, PC32 | Medium | Freezes non-personal install/data evidence before process initialization, then defaults only a jointly proven clean install through the existing nullable `appLanguage` owner; upgrade, explicit, existing-data, contradictory, unsupported, and marked states are preserved. |
| `android/src/test/java/chat/simplex/app/nome/NomeLocalePolicyTest.kt` | PC1, PC18, PC24, PC32 | Medium | Guards clean-install initialization, conservative update/existing-data behavior, explicit/marker idempotence, pre-initialization evidence retention, and the v6.5.6 upstream locale set. |
| `android/src/androidTest/java/chat/simplex/app/nome/{settings/NomeRemoteDesktopComposeTest.kt,lifecycle/NomeMainActivityIntentDispatchTest.kt}` | PC1, PC18, PC27, PC32 | Medium | Guards Android full-page Back/real pairing content and identical ordered cold/warm dispatch to the three official intent handlers. |
| `common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/views/usersettings/networkAndServers/{NetworkAndServers.kt,PlatformObservedNetworkInfo*}` | PC10, PC18, PC25, PC32 | Medium | Routes the established Network owner through Android presentation and keeps connectivity unknown before the first Android platform observation; Desktop and all network commands/preferences remain unchanged. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/{Appearance.kt,HelpView.kt,DeveloperView.kt}` | PC10, PC18, PC22, PC29, PC32 | Medium | Routes established settings owners through the Android presentation seam; no preference meaning, locale policy, diagnostic action, or external-link owner changes. |
| `android/src/androidTest/java/chat/simplex/app/nome/settings/NomeAboutSettingsComposeTest.kt` | PC10, PC18, PC24, PC32 | Medium | Proves truthful packaged version copy, exact About owner dispatch, nested Back action, and 48dp-plus activation. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/database/DatabaseView.kt` | PC11, PC23, PC26, PC32 | High | Retains database key/export/import operations and returns completed-snapshot truth to the existing stop/run/start wrapper while chooser copy/delete cleanup remains unchanged. |
| `common/src/{commonMain,androidMain,desktopMain}/kotlin/chat/simplex/common/platform/Files*` | PC26, PC32 | High | Adds an optional save MIME contract; Android database archives register as `application/zip`, Desktop behavior remains unchanged, and archive bytes/naming/format are untouched. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/helpers/GetImageView.desktop.kt` | PC24, PC32 | Low | Names the existing file-picker callback after the compatible signature extension; behavior is unchanged. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/{chatlist/UserPicker.kt,chatlist/ChatListView.kt}` | PC1, PC10, PC32 | Medium | Opens Android settings as a full-page route while retaining official profile/home owners. |
| `common/src/androidMain/res/values*/nome_settings_strings.xml` | PC10, PC18, PC22, PC23, PC24, PC32 | Low | Fixed bilingual P23/P24 copy without fabricated operation or network facts. |
| `android/src/test/java/chat/simplex/app/nome/database/DatabaseRecoveryRouteBoundaryTest.kt` | PC23, PC26, PC32 | High | Guards ZIP MIME registration, unchanged import type, completed-snapshot restart truth, and chooser-owned snapshot cleanup. |
| `android/src/androidTest/java/chat/simplex/app/nome/settings/NomeSettingsBackupComposeTest.kt` | PC10, PC18, PC22, PC24, PC32 | Medium | Proves existing-route dispatch, local filtering, 48dp controls, archive/restore/migration owner dispatch, stopped-chat migration gating, and absence of synthetic progress/resume copy. |

P23/P24 add no command, protocol/event type, database semantic, archive format, native/core
change, iOS behavior, or Desktop Nome UI. The Android save-MIME property is platform metadata
only. API 28/API 35 disposable runs prove real export cancellation/save, destructive import
round-trip/key re-entry, migration upload-stage abort, cleanup, restart, and cold-start data
preservation; a `0 bytes uploaded` / `0%` producer state is not success.

### PC32 Phase 2 Batch 3 exact P13 production sources

Batch 3 is limited to Android external-`ACTION_VIEW` connection preview. The common files are
required because the existing controller/caller branching is shared; Nome composables remain
Android-only. The Desktop actual declines the preview and is covered by a fallback test.

| Exact source or test path | Product concepts | Risk | Batch 3 responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt` | PC12, PC20, PC32 | High | Adds typed non-presentational results over unchanged plan/connect commands with P13 terminal logging disabled; legacy wrappers remain. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/chatlist/ChatListView.kt` | PC1, PC12, PC32 | High | Makes `connectIfOpenedViaUri` the only `ExternalActionView` opt-in; home/search/P08 boundaries remain unchanged. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/newchat/ConnectPlan.kt` | PC12, PC15, PC20, PC32 | High | Retains `Legacy` default, offers the seam only in seven current identity-choice branches, binds user/host, single-submits, replans retry, updates only a real pending connection, and cleans up idempotently. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.kt` | PC12, PC15, PC20, PC32 | High | Safe model/callback seam, exhaustive 7/21 branch policy, owner-proof mapping, failure sanitization, and context guard. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.android.kt` | PC12, PC20, PC32 | Medium | Opens the existing fullscreen modal and applies the official current light/dark choice. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewStateAdapter.kt` | PC12, PC20, PC32 | High | Pure ready/connecting/replanning/pending/failure reducer with duplicate/out-of-order suppression. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/connection/NomeConnectionPreviewRoute.android.kt` | PC12, PC20, PC24, PC32 | High | Bilingual accessible P13 route and safe state presentation using Nome tokens/components. |
| `common/src/androidMain/res/values/nome_connection_preview_strings.xml` | PC12, PC20, PC32 | Low | English P13 copy and semantics. |
| `common/src/androidMain/res/values-zh-rCN/nome_connection_preview_strings.xml` | PC12, PC20, PC32 | Low | Simplified-Chinese P13 copy and semantics. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreview.desktop.kt` | PC12, PC32 | Medium | Returns `false`, preserving the legacy Desktop presentation. |
| `common/src/commonTest/kotlin/chat/simplex/common/views/newchat/ConnectionPreviewPolicyTest.kt` | PC12, PC20, PC32 | Medium | Exhaustive eligibility, safe mapping, context, and sanitized failure tests. |
| `common/src/desktopTest/kotlin/chat/simplex/common/views/newchat/PlatformConnectionPreviewDesktopTest.kt` | PC12, PC32 | Medium | Executes the Desktop actual and proves it declines Nome presentation. |
| `android/src/debug/AndroidManifest.xml` | PC32 | Medium | Adds the P13 evidence Activity as debug-only/non-exported without changing production manifest ingress. |
| `android/src/debug/java/chat/simplex/app/nome/connection/NomeConnectionPreviewEvidenceActivity.kt` | PC12, PC20, PC24, PC32 | Medium | Nine deterministic renderer states with safe fixture data; not a real-core source. |
| `android/src/debug/res/values*/nome_connection_preview_evidence_strings.xml` | PC32 | Low | Debug-only bilingual fixture labels. |
| `android/src/test/java/chat/simplex/app/nome/connection/ConnectionPreviewRouteBoundaryTest.kt` | PC12, PC32 | Medium | Proves seven call sites and only the external ingress opt-in. |
| `android/src/test/java/chat/simplex/app/nome/connection/NomeConnectionPreviewStateAdapterTest.kt` | PC12, PC20, PC32 | Medium | Reducer identity/single-submit/retry truth table. |
| `android/src/androidTest/java/chat/simplex/app/nome/connection/NomeConnectionPreviewComposeTest.kt` | PC12, PC20, PC24, PC32 | Medium | Identity semantics, selection, 48dp, live-region, and 200% action coverage. |
| `android/src/androidTest/java/chat/simplex/app/nome/connection/NomeConnectionPreviewPackagingTest.kt` | PC32 | Medium | Guards P13 debug-host presence and non-export. |
| `android/src/androidTest/java/chat/simplex/app/nome/connection/NomeConnectionPreviewScreenshotTest.kt` | PC12, PC20, PC24, PC32 | Medium | Defines API 35 renderer matrix: 9 states × 2 locales × 2 themes × 2 font scales = 72 captures. |

Batch 3 changes no `Core.kt`, Haskell/native source, protocol/command/event type, database/archive
format, message state machine, iOS, or Desktop UI. It adds no P10/P12/P14/P16 behavior and does not
change P08 `FIRST_USE` or `FILTERED_NO_RESULT`.

### PC32 Batch 1A exact P01 production sources

Batch 1A is limited to the already-selected startup database roots. Common code carries facts and
mechanically extracts existing low-level actions; all Nome rendering stays Android-only. Desktop
delegates legacy content exactly once.

| Exact source or test path | Product concepts | Risk | Batch 1A responsibility |
|---|---|---|---|
| `common/src/commonMain/kotlin/chat/simplex/common/App.kt` | PC23, PC32 | High | Calls the platform seam in the existing migration/opening/error branches without changing priority, delay, or auth guard. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.kt` | PC23, PC32 | Medium | Display-safe facts/route and expect declarations only. |
| `common/src/commonMain/kotlin/chat/simplex/common/views/database/DatabaseErrorView.kt` | PC23, PC32 | High | Mechanical extraction of existing open/save/confirm/exact-backup actions while legacy alerts remain Desktop-owned. |
| `common/src/androidMain/kotlin/chat/simplex/common/platform/Cryptor.android.kt` | PC23, PC32 | High | Database-alias-only no-secret missing/unreadable class and fixed Logcat/user copy; return/throw behavior is unchanged. |
| `common/src/androidMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.android.kt` | PC23, PC32 | High | Process-owned atomic attempt orchestration, source-bound copy result, existing success hook, and auth-hidden semantics. |
| `common/src/androidMain/kotlin/chat/simplex/common/ui/nome/database/**` | PC23, PC24, PC32 | High | Exhaustive safe reducer and bilingual accessible P01 route with non-saveable passphrase. |
| `common/src/androidMain/res/values*/nome_database_root_strings.xml` | PC23, PC32 | Low | Fixed English/Simplified-Chinese P01 copy and semantics. |
| `common/src/desktopMain/kotlin/chat/simplex/common/views/database/PlatformDatabaseRootRoute.desktop.kt` | PC23, PC32 | Medium | Invokes official legacy content once; no Nome behavior. |
| `android/src/test/java/chat/simplex/app/nome/database/**` | PC23, PC32 | Medium | Reducer, key-read precedence, action ordering, generation, stale-result, raw-payload, and forbidden-claim tests. |
| `android/src/androidTest/java/chat/simplex/app/nome/database/**` | PC23, PC24, PC32 | Medium | Compose password semantics, state transition clearing, 48dp, 200%, live-region, and bounded-recovery checks. |
| `android/src/debug/java/chat/simplex/app/nome/database/NomeDatabaseRootEvidenceActivity.kt` | PC23, PC24, PC32 | Medium | Non-exported deterministic P01 renderer reference; visibly fixture-only and never a real database result. |
| `android/src/debug/res/values*/nome_database_root_evidence_strings.xml` | PC32 | Low | Debug-only bilingual fixture boundary labels. |
| `android/src/androidTest/java/chat/simplex/app/nome/database/NomeDatabaseRootScreenshotTest.kt` | PC23, PC24, PC32 | Medium | Defines the affected P01 bilingual/theme/font renderer reference set; it does not promote a fixture to production truth. |
| `android/src/androidTest/java/chat/simplex/app/nome/database/NomeDatabaseRootPackagingTest.kt` | PC32 | Medium | Guards debug-host presence and non-export; milestone release scans own absence from release. |
| `common/src/desktopTest/kotlin/chat/simplex/common/views/database/**` | PC23, PC32 | Medium | Executes all three Desktop route variants with sensitive content both allowed/denied and proves one legacy call. |

Batch 1A changes no `Core.kt`, Haskell/native source/binary, protocol, schema/migration order,
archive format, service/worker source, iOS, or Desktop UI.

---

## 1. Common Sources (commonMain)

Path prefix: `common/src/commonMain/kotlin/chat/simplex/common/`

### 1.1 Core Model & Platform

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `App.kt` | PC1 through PC32 | High | Root composable — navigation scaffold for all features; Nome must preserve gate order and hide obscured root semantics behind fullscreen overlays on Android and Desktop |
| `AppLock.kt` | PC22 | Medium | App lock state and authorization lifecycle |
| `model/ChatModel.kt` | PC1 through PC32 | High | Central state object; PC32 adds generation-scoped chat-list load/result reconciliation without a second model |
| `model/SimpleXAPI.kt` | PC1 through PC32 | High | FFI bridge to Haskell core; Android and Desktop startChat gate normal networking behind the idempotent Nome server bootstrap, startup saves can suppress interactive alerts, and terminal history snapshots redact SMP/XFTP credentials from commands and responses |
| `model/NomeServerConfiguration.kt` | PC1, PC17, PC25 | High | Android/Desktop application-layer Nome SMP/XFTP bootstrap, legacy endpoint migration, custom-server preservation, validation, and bounded retry policy |
| `model/CryptoFile.kt` | PC10, PC23 | Medium | Encrypted file read/write helpers |
| `platform/Core.kt` | PC1 through PC31 | High | Native FFI declarations and controller startup; Android post-start callbacks now run only after chat actually reaches running state |
| `platform/AppCommon.kt` | PC1 through PC31 | Medium | Shared app initialization logic |
| `platform/Files.kt` | PC10, PC23, PC26 | Medium | File path resolution, temp dirs, encryption utilities |
| `platform/NtfManager.kt` | PC18 | High | Notification manager expect declarations |
| `platform/Notifications.kt` | PC18 | Medium | Notification channel and permission abstractions |
| `platform/SimplexService.kt` | PC18 | Medium | Background service expect declarations |
| `platform/RecAndPlay.kt` | PC9 | Medium | Audio recording and playback abstractions |
| `platform/VideoPlayer.kt` | PC10, PC17 | Low | Video playback abstractions |
| `platform/Cryptor.kt` | PC23 | Medium | Keystore encryption expect declarations |
| `platform/Share.kt` | PC10, PC12 | Low | Share sheet abstractions |
| `platform/Images.kt` | PC10, PC19 | Low | Image processing utilities |
| `platform/Platform.kt` | PC1 through PC31 | Low | Platform detection and capability flags |
| `platform/PlatformTextField.kt` | PC4 | Low | Native text input expect declarations |
| `platform/Back.kt` | PC1 | Low | Back navigation handling |
| `platform/UI.kt` | PC24 | Low | UI density and locale helpers |
| `platform/ScrollableColumn.kt` | PC1 | Low | Scrollable list abstractions |
| `platform/Log.kt` | — | Low | Logging utility — no direct product impact |
| `platform/Modifier.kt` | PC24 | Low | Compose modifier extensions |
| `platform/Resources.kt` | PC24 | Low | Resource loading helpers |

### 1.2 Theme

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `ui/theme/ThemeManager.kt` | PC24, PC32 | Medium | Theme resolution engine — all color and wallpaper logic |
| `ui/theme/Theme.kt` | PC24, PC32 | Medium | Theme composables and `SimpleXTheme` |
| `ui/theme/Color.kt` | PC24, PC32 | Low | Color palette definitions |
| `ui/theme/Shape.kt` | PC24 | Low | Shape token definitions |
| `ui/theme/Type.kt` | PC24 | Low | Typography definitions |

### 1.3 Views — Chat List

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/chatlist/ChatListView.kt` | PC1, PC12, PC27, PC28, PC32 | High | Main list/search, preserved automatic notices, and the only P13 external-link presentation opt-in |
| `views/chatlist/PlatformHomeRoute.kt` | PC1, PC32 | Medium | Shared platform seam; Android supplies Nome home and Desktop delegates the existing chat list |
| `views/chatlist/ChatListNavLinkView.kt` | PC1, PC2, PC3 | Medium | Navigation from chat list item to chat |
| `views/chatlist/ChatPreviewView.kt` | PC1, PC2, PC3, PC11 | Medium | Chat row preview rendering |
| `views/chatlist/TagListView.kt` | PC28 | Medium | Chat tag filter UI |
| `views/chatlist/UserPicker.kt` | PC19, PC21 | Medium | Multi-profile switcher overlay |
| `views/chatlist/ShareListView.kt` | PC10 | Low | Share target list |
| `views/chatlist/ShareListNavLinkView.kt` | PC10 | Low | Share target navigation |
| `views/chatlist/ChatHelpView.kt` | PC1 | Low | Empty-state help content |
| `views/chatlist/ContactRequestView.kt` | PC12 | Medium | Incoming contact request row |
| `views/chatlist/ContactConnectionView.kt` | PC12 | Low | Pending connection row |
| `views/chatlist/ServersSummaryView.kt` | PC25 | Low | Server status summary |

### 1.4 Views — Chat & Messaging

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/chat/ChatView.kt` | PC2, PC3, PC4, PC5, PC6, PC7, PC8, PC9, PC11 | High | Core conversation UI — most messaging features |
| `views/chat/ComposeView.kt` | PC4, PC6, PC9, PC10, PC11 | High | Message composition — send path for all messages |
| `views/chat/SendMsgView.kt` | PC4, PC9 | Medium | Send button and voice record toggle |
| `views/chat/ComposeVoiceView.kt` | PC9 | Medium | Voice message recording UI |
| `views/chat/ComposeFileView.kt` | PC10 | Low | File attachment preview in compose area |
| `views/chat/ComposeImageView.kt` | PC10 | Low | Image attachment preview in compose area |
| `views/chat/ContextItemView.kt` | PC6 | Low | Reply/edit quote preview |
| `views/chat/SelectableChatItemToolbars.kt` | PC7, PC10 | Medium | Multi-select toolbar (delete, forward) |
| `views/chat/ChatInfoView.kt` | PC2, PC13, PC20 | Medium | Contact details and verification |
| `views/chat/ContactPreferences.kt` | PC2, PC8 | Medium | Per-contact feature preferences |
| `views/chat/ChatItemInfoView.kt` | PC2, PC3 | Low | Message delivery detail |
| `views/chat/ChatItemsLoader.kt` | PC2, PC3 | Medium | Pagination and message loading logic |
| `views/chat/ChatItemsMerger.kt` | PC2, PC3 | Medium | Merges incremental message updates |
| `views/chat/VerifyCodeView.kt` | PC13 | Medium | Contact security code verification |
| `views/chat/ScanCodeView.kt` | PC13 | Low | QR code scanning for verification |
| `views/chat/CommandsMenuView.kt` | PC4 | Low | Slash-command menu |
| `views/chat/ComposeContextProfilePickerView.kt` | PC20 | Low | Incognito profile picker in compose |
| `views/chat/ComposeContextPendingMemberActionsView.kt` | PC14, PC30 | Low | Pending member action buttons in compose |
| `views/chat/ComposeContextGroupDirectInvitationActionsView.kt` | PC14 | Low | Direct invitation action buttons in compose |
| `views/chat/ComposeContextContactRequestActionsView.kt` | PC12 | Low | Contact request action buttons in compose |

### 1.5 Views — Chat Items

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/chat/item/ChatItemView.kt` | PC2, PC3, PC5, PC6, PC7, PC8 | High | Root chat item renderer with context menus |
| `views/chat/item/TextItemView.kt` | PC2, PC3, PC4 | Medium | Text message bubble rendering |
| `views/chat/item/FramedItemView.kt` | PC4, PC6, PC10, PC11 | Medium | Framed (quoted/forwarded) message container |
| `views/chat/item/CIImageView.kt` | PC10 | Medium | Image message rendering |
| `views/chat/item/CIVideoView.kt` | PC10 | Medium | Video message rendering |
| `views/chat/item/CIFileView.kt` | PC10 | Medium | File message rendering |
| `views/chat/item/CIVoiceView.kt` | PC9 | Medium | Voice message rendering and playback |
| `views/chat/item/EmojiItemView.kt` | PC5 | Low | Emoji reaction display |
| `views/chat/item/CIMetaView.kt` | PC2, PC3, PC8 | Low | Timestamp, delivery status, timed message indicator |
| `views/chat/item/CICallItemView.kt` | PC17 | Low | Call event item rendering |
| `views/chat/item/CIEventView.kt` | PC3, PC14, PC16 | Low | Group event item rendering |
| `views/chat/item/CIGroupInvitationView.kt` | PC3, PC14 | Low | Group invitation item rendering |
| `views/chat/item/CIMemberCreatedContactView.kt` | PC3, PC12 | Low | Member-created contact event |
| `views/chat/item/CIChatFeatureView.kt` | PC8 | Low | Feature change event rendering |
| `views/chat/item/CIFeaturePreferenceView.kt` | PC8 | Low | Feature preference change rendering |
| `views/chat/item/CIRcvDecryptionError.kt` | PC2, PC3 | Low | Decryption error display |
| `views/chat/item/DeletedItemView.kt` | PC7 | Low | Deleted message placeholder |
| `views/chat/item/MarkedDeletedItemView.kt` | PC7 | Low | Moderated/marked-deleted placeholder |
| `views/chat/item/ImageFullScreenView.kt` | PC10 | Low | Full-screen image viewer |
| `views/chat/item/CIBrokenComposableView.kt` | — | Low | Fallback for render failures |
| `views/chat/item/CIInvalidJSONView.kt` | — | Low | Fallback for malformed items |
| `views/chat/item/IntegrityErrorItemView.kt` | PC2, PC3 | Low | Message integrity error display |

### 1.6 Views — Groups

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/chat/group/GroupChatInfoView.kt` | PC3, PC14, PC15, PC16, PC30, PC32 | High | Group management hub; Android page route and page-owned invitation callback keep one top Back owner |
| `views/chat/group/AddGroupMembersView.kt` | PC14, PC16, PC32 | Medium | Member invitation flow; Android full-page/card route preserves role/admission/preferences/invite owners and truthful selection semantics |
| `views/chat/group/GroupMemberInfoView.kt` | PC3, PC14, PC16, PC30, PC31 | Medium | Member details and role management; relay-address + rejected-status info rows |
| `views/chat/group/ChannelRelaysView.kt` | PC31, PC32 | Medium | Channel relay list/status/member-detail route; Android presentation uses the shared settings-detail frame; Add/Remove entries remain source-disabled under the v6.5.6 `TODO [relays]` boundary |
| `views/chat/group/AddGroupRelayView.kt` | PC31 | Low | Source-present Add relay sheet with no active `ChannelRelaysView` route in v6.5.6 |
| `views/chat/group/GroupProfileView.kt` | PC3, PC14, PC32 | Medium | Group profile editing; Android presentation uses the shared settings-detail frame without automatic IME focus |
| `views/chat/group/GroupLinkView.kt` | PC15 | Low | Group link creation and sharing |
| `views/chat/group/GroupPreferences.kt` | PC3, PC8, PC14 | Medium | Group feature toggles |
| `views/chat/group/GroupMentions.kt` | PC3, PC4 | Medium | @mention resolution and display |
| `views/chat/group/GroupMembersToolbar.kt` | PC3, PC14 | Low | Member list toolbar |
| `views/chat/group/GroupReportsView.kt` | PC3, PC14 | Low | Group content reports |
| `views/chat/group/MemberAdmission.kt` | PC14, PC16 | Medium | Member admission settings |
| `views/chat/group/MemberSupportView.kt` | PC30 | Medium | Member support chat toggle |
| `views/chat/group/MemberSupportChatView.kt` | PC30 | Medium | Member support chat conversation |
| `views/chat/group/WelcomeMessageView.kt` | PC3, PC14 | Low | Group welcome message editor |

### 1.7 Views — Calls

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/call/CallView.kt` | PC17 | High | Call UI and WebRTC composable |
| `views/call/CallManager.kt` | PC17 | High | Call lifecycle management |
| `views/call/WebRTC.kt` | PC17 | High | WebRTC types and signaling |
| `views/call/IncomingCallAlertView.kt` | PC17, PC18 | Medium | Incoming call overlay |

### 1.8 Views — New Chat & Contacts

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/newchat/NewChatView.kt` | PC12, PC29, PC32 | High | New connection creation — onramp for all contacts |
| `views/newchat/NewChatSheet.kt` | PC12, PC32 | Medium | Owns the existing four connection/group/channel callbacks and delegates Android P10 presentation through the platform seam |
| `views/newchat/PlatformNewChatHub.kt` | PC12, PC19, PC31, PC32 | Medium | P10 presentation-only expect seam; carries actual current-profile display data and existing action closures |
| `androidMain/.../views/newchat/PlatformNewChatHub.android.kt` | PC12, PC19, PC24, PC31, PC32 | Medium | Android P10 visual-acceptance renderer over official callbacks |
| `desktopMain/.../views/newchat/PlatformNewChatHub.desktop.kt` | PC12, PC32 | Low | Invokes the legacy New Chat content unchanged |
| `views/newchat/ConnectPlan.kt` | PC12, PC15, PC20, PC32 | High | Link planning plus the P13 legacy-default, context-bound, single-submit connection-preview integration |
| `views/newchat/PlatformConnectionPreview.kt` | PC12, PC15, PC20, PC32 | High | P13 safe model, exhaustive branch policy, typed callbacks, and expect seam |
| `views/newchat/AddGroupView.kt` | PC3, PC14, PC32 | Medium | New group creation flow; Android presentation delegates through `PlatformAddGroupRoute`, Desktop remains legacy |
| `views/newchat/AddChannelView.kt` | PC31 | Medium | Public channel creation, channel link card, `RelayStatusIndicator` |
| `views/newchat/ContactConnectionInfoView.kt` | PC12 | Low | Pending connection details |
| `views/newchat/AddContactLearnMore.kt` | PC12 | Low | Educational content |
| `views/newchat/QRCode.kt` | PC12 | Low | QR code display |
| `views/newchat/QRCodeScanner.kt` | PC12 | Low | QR code camera scanner |
| `views/contacts/ContactListNavView.kt` | PC1, PC12 | Medium | Contact list navigation |
| `views/contacts/ContactPreviewView.kt` | PC12 | Low | Contact row preview |

### 1.9 Views — User Settings

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/usersettings/SettingsView.kt` | PC18, PC22, PC23, PC24, PC25, PC29, PC32 | Medium | Settings navigation hub |
| `views/usersettings/Appearance.kt` | PC24 | Low | Theme and appearance customization |
| `views/usersettings/PrivacySettings.kt` | PC20, PC22 | Medium | Privacy and lock settings |
| `views/usersettings/UserProfileView.kt` | PC19 | Medium | Profile display name and image editing |
| `views/usersettings/UserProfilesView.kt` | PC19, PC21 | Medium | Multi-profile management |
| `views/usersettings/HiddenProfileView.kt` | PC21 | Medium | Hidden profile access |
| `views/usersettings/IncognitoView.kt` | PC20 | Low | Incognito mode explanation |
| `views/usersettings/UserAddressView.kt` | PC29 | Medium | User SimpleX address management |
| `views/usersettings/UserAddressLearnMore.kt` | PC29 | Low | Address educational content |
| `views/usersettings/NotificationsSettingsView.kt` | PC18 | Medium | Notification mode configuration |
| `views/usersettings/CallSettings.kt` | PC17 | Low | Call-related settings |
| `views/usersettings/Preferences.kt` | PC2, PC3, PC8 | Medium | Chat feature preferences UI |
| `views/usersettings/SetDeliveryReceiptsView.kt` | PC2 | Low | Delivery receipts toggle |
| `views/usersettings/RTCServers.kt` | PC17, PC25 | Medium | WebRTC ICE server configuration |
| `views/usersettings/DeveloperView.kt` | — | Low | Developer/debug settings |
| `views/usersettings/HelpView.kt` | — | Low | Help and support links |
| `views/usersettings/MarkdownHelpView.kt` | PC4 | Low | Markdown formatting guide |
| `views/usersettings/VersionInfoView.kt` | — | Low | Version display |
| `views/usersettings/networkAndServers/NetworkAndServers.kt` | PC25 | High | Server and network configuration hub |
| `views/usersettings/networkAndServers/AdvancedNetworkSettings.kt` | PC25 | Medium | SOCKS proxy, timeouts, etc. |
| `views/usersettings/networkAndServers/OperatorView.kt` | PC25 | Medium | Server operator management |
| `views/usersettings/networkAndServers/ProtocolServersView.kt` | PC25 | Medium | SMP/XFTP server list |
| `views/usersettings/networkAndServers/ProtocolServerView.kt` | PC25 | Low | Individual server editing |
| `views/usersettings/networkAndServers/NewServerView.kt` | PC25 | Low | Add new server |
| `views/usersettings/networkAndServers/ScanProtocolServer.kt` | PC25 | Low | QR scan for server address |

### 1.10 Views — Database & Migration

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/database/DatabaseView.kt` | PC1, PC23, PC26, PC32 | High | Database management plus generation-scoped chat-list refresh after TTL maintenance |
| `views/database/DatabaseEncryptionView.kt` | PC23 | High | Database encryption passphrase change |
| `views/database/DatabaseErrorView.kt` | PC23 | Medium | Database open error recovery |
| `views/migration/MigrateFromDevice.kt` | PC26 | High | Outbound device migration |
| `views/migration/MigrateToDevice.kt` | PC26 | High | Inbound device migration |

### 1.11 Views — Local Auth & Onboarding

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/localauth/LocalAuthView.kt` | PC22 | Medium | App lock authentication flow |
| `views/localauth/SetAppPasscodeView.kt` | PC22 | Medium | Passcode creation and change |
| `views/localauth/PasscodeView.kt` | PC22 | Medium | Passcode entry UI |
| `views/localauth/PasswordEntry.kt` | PC22 | Low | Password input field |
| `views/onboarding/OnboardingView.kt` | PC1, PC32 | Medium | Onboarding flow navigation |
| `views/onboarding/SimpleXInfo.kt` | PC1 | Low | Welcome screen |
| `views/onboarding/SetNotificationsMode.kt` | PC18 | Medium | Notification permission and mode setup |
| `views/onboarding/SetupDatabasePassphrase.kt` | PC23 | Medium | Initial database passphrase setup |
| `views/onboarding/ChooseServerOperators.kt` | PC25 | Medium | Initial server operator selection |
| `views/onboarding/WhatsNewView.kt` | — | Low | Release notes display |
| `views/onboarding/HowItWorks.kt` | — | Low | Educational content |
| `views/onboarding/LinkAMobileView.kt` | PC27 | Low | Mobile linking onboarding |

### 1.12 Views — Remote Desktop

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/remote/ConnectDesktopView.kt` | PC27 | Medium | Connect-to-desktop flow (from mobile) |
| `views/remote/ConnectMobileView.kt` | PC27 | Medium | Connect-to-mobile flow (from desktop) |

### 1.13 Views — Helpers

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/helpers/AlertManager.kt` | PC1 through PC31 | Medium | Modal alert system used across all features |
| `views/helpers/ModalView.kt` | PC1 through PC31 | Medium | Modal navigation stack |
| `views/helpers/Utils.kt` | PC1 through PC31 | Low | Shared formatting, clipboard, and utility functions |
| `views/helpers/DatabaseUtils.kt` | PC23 | Medium | Keystore passphrase and database helpers |
| `views/helpers/LinkPreviews.kt` | PC11 | Medium | Link preview fetching and rendering |
| `views/helpers/LocalAuthentication.kt` | PC22 | Medium | Biometric/passcode authentication expect |
| `views/helpers/ChatWallpaper.kt` | PC24 | Low | Chat wallpaper rendering |
| `views/helpers/ChatInfoImage.kt` | PC19 | Low | Profile image composable |
| `views/helpers/ThemeModeEditor.kt` | PC24 | Low | Theme mode toggle |
| `views/helpers/ChooseAttachmentView.kt` | PC10 | Low | Attachment picker |
| `views/helpers/GetImageView.kt` | PC10, PC19 | Low | Image capture and crop |
| `views/helpers/TextEditor.kt` | PC4 | Low | Rich text editor helpers |
| `views/helpers/SearchTextField.kt` | PC1 | Low | Search bar composable |
| `views/helpers/CustomTimePicker.kt` | PC8 | Low | Time picker for timed messages |
| `views/helpers/DragAndDrop.kt` | PC10 | Low | Drag-and-drop file handling |
| `views/helpers/ProcessedErrors.kt` | — | Low | Error aggregation |
| `views/helpers/AnimationUtils.kt` | PC24 | Low | Animation helpers |
| `views/helpers/DefaultDialog.kt` | — | Low | Dialog composable primitives |
| `views/helpers/DefaultDropdownMenu.kt` | — | Low | Dropdown menu composable |
| `views/helpers/Section.kt` | — | Low | Settings section composable |
| `views/helpers/SimpleButton.kt` | — | Low | Button composable |
| `views/helpers/DefaultTopAppBar.kt` | — | Low | App bar composable |
| `views/helpers/DefaultBasicTextField.kt` | PC4 | Low | Text field composable |
| `views/helpers/AppBarTitle.kt` | — | Low | App bar title composable |
| `views/helpers/BlurModifier.kt` | PC22 | Low | Blur modifier for app lock |
| `views/helpers/CollapsingAppBar.kt` | — | Low | Collapsing toolbar composable |
| `views/helpers/CustomIcons.kt` | — | Low | Custom icon definitions |
| `views/helpers/DataClasses.kt` | — | Low | Shared data class utilities |
| `views/helpers/DefaultProgressBar.kt` | — | Low | Progress bar composable |
| `views/helpers/DefaultSwitch.kt` | — | Low | Switch composable |
| `views/helpers/Enums.kt` | — | Low | Enum utility extensions |
| `views/helpers/ExposedDropDownSettingRow.kt` | — | Low | Dropdown setting row composable |
| `views/helpers/GestureDetector.kt` | — | Low | Touch gesture utilities |
| `views/helpers/Modifiers.kt` | — | Low | Compose modifier extensions |
| `views/helpers/SubscriptionStatusIcon.kt` | PC25 | Low | Server connection status icon |

### 1.14 Views — Other

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/TerminalView.kt` | — | Low | Developer chat console |
| `views/SplashView.kt` | — | Low | Splash screen |
| `views/WelcomeView.kt` | PC1 | Low | Empty-state welcome |
| `views/Preview.kt` | — | Low | Compose preview utilities |

---

## 2. Android Sources

### 2.1 Android App Module

Path prefix: `android/src/main/java/chat/simplex/app/`

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `SimplexApp.kt` | PC1 through PC32 | High | Application class; foreground chat refresh uses the typed generation-scoped load result |
| `MainActivity.kt` | PC1 through PC32 | High | Single-activity host; wraps the unchanged shared root in the Android Nome production host |
| `nome/NomeProductionShell.kt` | PC1, PC24, PC32 | Medium | Production Activity host and system-bar synchronization; owns no root navigation state |
| `SimplexService.kt` | PC18 | High | Foreground service — keeps message receiver alive |
| `CallService.kt` | PC17 | Medium | Foreground service for active calls |
| `MessagesFetcherWorker.kt` | PC18 | Medium | WorkManager periodic message fetch |
| `model/NtfManager.android.kt` | PC18 | High | Android notification channels, display, and actions |
| `views/call/CallActivity.kt` | PC17 | Medium | Dedicated activity for full-screen call UI |
| `views/helpers/Util.kt` | — | Low | Android-specific utility extensions |
| `android/src/debug/java/chat/simplex/app/nome/**` | PC12, PC20, PC24, PC32 | Low | Debug-only foundation/home/P13 fixtures, Preview, and non-exported screenshot Activities |
| `android/src/test/java/chat/simplex/app/nome/**` | PC1, PC12, PC20, PC25, PC32 | Medium | Nome foundation/home contracts plus P13 route/reducer truth |
| `android/src/androidTest/java/chat/simplex/app/nome/**` | PC1, PC12, PC20, PC24, PC32 | Medium | Foundation/home/P13 semantics, 48dp/200%, packaging, screenshots, and explicit real-core gates where defined |
| `android/src/androidTest/java/chat/simplex/app/ExampleInstrumentedTest.kt` | PC32 | Low | Configured application-ID packaging regression |

### 2.2 Android Platform Implementations (androidMain)

Path prefix: `common/src/androidMain/kotlin/chat/simplex/common/`

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `platform/AppCommon.android.kt` | PC1 through PC31 | Medium | Android app initialization actual declarations |
| `platform/SimplexService.android.kt` | PC18 | Medium | Android foreground service actual implementation |
| `platform/Files.android.kt` | PC10, PC23, PC26 | Medium | Android file paths and content-URI resolution |
| `platform/Cryptor.android.kt` | PC23 | Medium | Android Keystore encryption actual implementation |
| `platform/RecAndPlay.android.kt` | PC9 | Medium | Android MediaRecorder/MediaPlayer actual implementation |
| `platform/VideoPlayer.android.kt` | PC10 | Low | Android ExoPlayer actual implementation |
| `platform/Notifications.android.kt` | PC18 | Medium | Android notification channel creation |
| `platform/Images.android.kt` | PC10, PC19 | Low | Android bitmap processing |
| `platform/PlatformTextField.android.kt` | PC4 | Low | Android native text field actual implementation |
| `platform/Share.android.kt` | PC10 | Low | Android share intent actual implementation |
| `platform/Back.android.kt` | PC1 | Low | Android back press handler |
| `platform/UI.android.kt` | PC24 | Low | Android density and locale |
| `platform/ScrollableColumn.android.kt` | PC1 | Low | Android lazy list actual implementation |
| `platform/Log.android.kt` | — | Low | Android Log wrapper |
| `platform/Modifier.android.kt` | — | Low | Android modifier extensions |
| `platform/Resources.android.kt` | — | Low | Android resource loading |
| `helpers/NetworkObserver.kt` | PC1, PC25, PC32 | Medium | Android ConnectivityManager observer with nullable first-observation fact for P08 unknown/offline separation |
| `helpers/Permissions.kt` | PC9, PC10, PC17, PC18 | Medium | Android runtime permission requests |
| `helpers/SoundPlayer.kt` | PC17, PC18 | Low | Android sound playback for calls and notifications |
| `helpers/Extensions.kt` | — | Low | Kotlin extension utilities |
| `helpers/Locale.kt` | — | Low | Locale helpers |
| `views/call/CallView.android.kt` | PC17 | Medium | Android WebView-based WebRTC call |
| `views/call/CallAudioDeviceManager.kt` | PC17 | Medium | Android audio routing (speaker, earpiece, bluetooth) |
| `views/chat/ComposeView.android.kt` | PC4, PC10 | Low | Android compose view extensions |
| `views/chat/SendMsgView.android.kt` | PC4 | Low | Android send button extensions |
| `views/chat/item/ChatItemView.android.kt` | PC2, PC3 | Low | Android chat item extensions |
| `views/chat/item/CIImageView.android.kt` | PC10 | Low | Android image rendering extensions |
| `views/chat/item/CIVideoView.android.kt` | PC10 | Low | Android video rendering extensions |
| `views/chat/item/CIFileView.android.kt` | PC10 | Low | Android file view extensions |
| `views/chat/item/EmojiItemView.android.kt` | PC5 | Low | Android emoji rendering extensions |
| `views/chat/item/ImageFullScreenView.android.kt` | PC10 | Low | Android full-screen image viewer |
| `views/chatlist/ChatListView.android.kt` | PC1 | Low | Android chat list extensions |
| `views/chatlist/ChatListNavLinkView.android.kt` | PC1 | Low | Android chat list navigation extensions |
| `views/chatlist/TagListView.android.kt` | PC28 | Low | Android tag list extensions |
| `views/chatlist/UserPicker.android.kt` | PC19 | Low | Android profile picker extensions |
| `views/database/DatabaseView.android.kt` | PC23, PC26 | Low | Android database view extensions |
| `views/database/DatabaseEncryptionView.android.kt` | PC23 | Low | Android encryption view extensions |
| `views/helpers/LocalAuthentication.android.kt` | PC22 | Medium | Android BiometricPrompt actual implementation |
| `views/helpers/ChooseAttachmentView.android.kt` | PC10 | Low | Android file/camera chooser |
| `views/helpers/GetImageView.android.kt` | PC10, PC19 | Low | Android image capture |
| `views/helpers/CustomTimePicker.android.kt` | PC8 | Low | Android time picker |
| `views/helpers/Utils.android.kt` | — | Low | Android utility extensions |
| `views/helpers/DefaultDialog.android.kt` | — | Low | Android dialog extensions |
| `views/helpers/WorkaroundFocusSearchLayout.kt` | — | Low | Android focus workaround |
| `views/newchat/QRCode.android.kt` | PC12 | Low | Android QR code rendering |
| `views/newchat/QRCodeScanner.android.kt` | PC12 | Low | Android camera QR scanner |
| `views/onboarding/SimpleXInfo.android.kt` | PC1 | Low | Android onboarding extensions |
| `views/onboarding/SetNotificationsMode.android.kt` | PC18 | Low | Android notification mode extensions |
| `views/usersettings/Appearance.android.kt` | PC24 | Low | Android appearance extensions |
| `views/usersettings/PrivacySettings.android.kt` | PC20, PC22 | Low | Android privacy settings extensions |
| `views/usersettings/SettingsView.android.kt` | — | Low | Android settings extensions |
| `views/usersettings/networkAndServers/OperatorView.android.kt` | PC25 | Low | Android operator view extensions |
| `views/usersettings/networkAndServers/ScanProtocolServer.android.kt` | PC25 | Low | Android server QR scan |
| `ui/theme/Theme.android.kt` | PC24 | Low | Android dynamic color / system theme |
| `ui/theme/Type.android.kt` | PC24 | Low | Android typography |
| `ui/nome/tokens/*.kt` | PC24, PC32 | Medium | Nome Android light/dark semantic design tokens |
| `ui/nome/theme/NomeTheme.kt` | PC24, PC32 | Medium | Isolated Material 2 theme adapter for Nome foundation |
| `ui/nome/components/*.kt` | PC32 | Medium | Parameterized foundation components and explicit state taxonomy |
| `ui/nome/accessibility/*.kt` | PC32 | Medium | TalkBack and 48dp modifier contracts |
| `ui/nome/home/NomeHomeStateAdapter.kt` | PC1, PC32 | High | Pure generation/load/connectivity/core derivation for P07/P08 |
| `ui/nome/home/NomeHomeRoute.android.kt` | PC1, PC24, PC32 | High | Android platform actual and production read-only home renderer |
| `ui/nome/connection/NomeConnectionPreviewStateAdapter.kt` | PC12, PC20, PC32 | High | Pure P13 presentation reducer |
| `ui/nome/connection/NomeConnectionPreviewRoute.android.kt` | PC12, PC20, PC24, PC32 | High | Android P13 production route and state renderer |
| `views/newchat/PlatformConnectionPreview.android.kt` | PC12, PC20, PC32 | Medium | P13 fullscreen platform actual |
| `common/src/androidMain/res/values/nome_home_strings.xml` | PC1, PC32 | Low | English home resources (full path; outside this section's Kotlin prefix) |
| `common/src/androidMain/res/values-zh-rCN/nome_home_strings.xml` | PC1, PC32 | Low | Simplified-Chinese home resources (full path; outside this section's Kotlin prefix) |
| `common/src/androidMain/res/values/nome_connection_preview_strings.xml` | PC12, PC20, PC32 | Low | English P13 resources (full path; outside this section's Kotlin prefix) |
| `common/src/androidMain/res/values-zh-rCN/nome_connection_preview_strings.xml` | PC12, PC20, PC32 | Low | Simplified-Chinese P13 resources (full path; outside this section's Kotlin prefix) |

---

## 3. Desktop Sources

### 3.1 Desktop App Module

Path prefix: `desktop/src/jvmMain/kotlin/chat/simplex/desktop/`

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `Main.kt` | PC1 through PC31 | High | JVM entry point — Haskell init, migrations, app launch |

### 3.2 Desktop Platform Implementations (desktopMain)

Path prefix: `common/src/desktopMain/kotlin/chat/simplex/common/`

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `DesktopApp.kt` | PC1, PC2, PC3 | High | Desktop Compose window — window lifecycle, crash recovery |
| `StoreWindowState.kt` | — | Low | Window position/size persistence |
| `model/NtfManager.desktop.kt` | PC18 | Medium | Desktop system tray notification display |
| `platform/AppCommon.desktop.kt` | PC1 through PC31 | Medium | Desktop app initialization actual declarations |
| `platform/SimplexService.desktop.kt` | PC18 | Low | Desktop background receiver (no foreground service) |
| `platform/Files.desktop.kt` | PC10, PC23, PC26 | Medium | Desktop file path resolution |
| `platform/Cryptor.desktop.kt` | PC23 | Medium | Desktop keystore encryption actual implementation |
| `platform/RecAndPlay.desktop.kt` | PC9 | Medium | Desktop audio recording/playback actual implementation |
| `platform/VideoPlayer.desktop.kt` | PC10 | Low | Desktop VLC-based video player |
| `platform/Videos.desktop.kt` | PC10 | Low | Desktop video utilities |
| `platform/Notifications.desktop.kt` | PC18 | Low | Desktop notification setup |
| `platform/Images.desktop.kt` | PC10 | Low | Desktop image processing |
| `platform/PlatformTextField.desktop.kt` | PC4 | Low | Desktop text field actual implementation |
| `platform/Share.desktop.kt` | PC10 | Low | Desktop clipboard/share |
| `platform/Back.desktop.kt` | PC1 | Low | Desktop back navigation |
| `platform/UI.desktop.kt` | PC24 | Low | Desktop density and locale |
| `platform/ScrollableColumn.desktop.kt` | PC1 | Low | Desktop lazy list |
| `platform/Platform.desktop.kt` | — | Low | Platform detection |
| `platform/Log.desktop.kt` | — | Low | Desktop log output |
| `platform/Modifier.desktop.kt` | — | Low | Desktop modifier extensions |
| `platform/Resources.desktop.kt` | — | Low | Desktop resource loading |
| `views/call/CallView.desktop.kt` | PC17 | Medium | Desktop browser-based WebRTC call; bundled page title and browser-visible branding must remain Nome-owned |
| `views/chat/ComposeView.desktop.kt` | PC4, PC10 | Low | Desktop compose view (drag-and-drop, paste) |
| `views/chat/SendMsgView.desktop.kt` | PC4 | Low | Desktop send shortcut (Enter key handling) |
| `views/chat/item/ChatItemView.desktop.kt` | PC2, PC3 | Low | Desktop chat item extensions |
| `views/chat/item/CIImageView.desktop.kt` | PC10 | Low | Desktop image rendering |
| `views/chat/item/CIVideoView.desktop.kt` | PC10 | Low | Desktop video rendering |
| `views/chat/item/CIFileView.desktop.kt` | PC10 | Low | Desktop file open/save |
| `views/chat/item/EmojiItemView.desktop.kt` | PC5 | Low | Desktop emoji rendering |
| `views/chat/item/ImageFullScreenView.desktop.kt` | PC10 | Low | Desktop full-screen image |
| `views/chatlist/ChatListView.desktop.kt` | PC1 | Low | Desktop chat list extensions |
| `views/chatlist/PlatformHomeRoute.desktop.kt` | PC1, PC19 | Medium | Nome rail shell; destinations expose button role and selected state, and the profile avatar is named |
| `views/newchat/PlatformNewChatHub.desktop.kt` | PC1 | Medium | Nome desktop connection hub; action rows and back control expose button roles |
| `views/chatlist/ChatListNavLinkView.desktop.kt` | PC1 | Low | Desktop chat list navigation |
| `views/chatlist/TagListView.desktop.kt` | PC28 | Low | Desktop tag list extensions |
| `views/chatlist/UserPicker.desktop.kt` | PC19 | Low | Desktop profile picker |
| `views/database/DatabaseView.desktop.kt` | PC23, PC26 | Low | Desktop database view extensions |
| `views/database/DatabaseEncryptionView.desktop.kt` | PC23 | Low | Desktop encryption view extensions |
| `views/helpers/AppUpdater.kt` | — | Low | Desktop auto-update checker and installer |
| `views/helpers/OkHttpProgressListener.kt` | — | Low | Download progress tracking for updates |
| `views/helpers/LocalAuthentication.desktop.kt` | PC22 | Low | Desktop passcode-only auth (no biometrics) |
| `views/helpers/ChooseAttachmentView.desktop.kt` | PC10 | Low | Desktop file chooser dialog |
| `views/helpers/GetImageView.desktop.kt` | PC10, PC19 | Low | Desktop image file picker |
| `views/helpers/CustomTimePicker.desktop.kt` | PC8 | Low | Desktop time picker |
| `views/helpers/Utils.desktop.kt` | — | Low | Desktop utility extensions |
| `views/helpers/DefaultDialog.desktop.kt` | — | Low | Desktop dialog extensions |
| `views/newchat/QRCode.desktop.kt` | PC12 | Low | Desktop QR code rendering |
| `views/newchat/QRCodeScanner.desktop.kt` | PC12 | Low | Desktop QR code scanner (screen/clipboard) |
| `views/onboarding/SimpleXInfo.desktop.kt` | PC1 | Low | Desktop onboarding extensions |
| `views/onboarding/SetNotificationsMode.desktop.kt` | PC18 | Low | Desktop notification mode extensions |
| `views/usersettings/Appearance.desktop.kt` | PC24 | Low | Desktop appearance extensions |
| `views/usersettings/PrivacySettings.desktop.kt` | PC20, PC22 | Low | Desktop privacy settings extensions |
| `views/usersettings/SettingsView.desktop.kt` | — | Low | Desktop settings extensions |
| `views/usersettings/networkAndServers/OperatorView.desktop.kt` | PC25 | Low | Desktop operator view extensions |
| `views/usersettings/networkAndServers/ScanProtocolServer.desktop.kt` | PC25 | Low | Desktop server address scan |
| `ui/theme/Theme.desktop.kt` | PC24 | Low | Desktop system theme detection |
| `ui/theme/Type.desktop.kt` | PC24 | Low | Desktop typography |
| `views/chatlist/PlatformHomeRoute.desktop.kt` | PC1, PC32 | Medium | Required actual; delegates the upstream `defaultContent` unchanged |
| `views/newchat/PlatformConnectionPreview.desktop.kt` | PC12, PC32 | Medium | Required P13 actual; declines Nome presentation and preserves legacy UI |
| `other/videoplayer/SkiaBitmapVideoSurface.kt` | PC10 | Low | Desktop Skia video surface for VLC |

Path prefix for the following test row is `common/src/desktopTest/kotlin/chat/simplex/common/`.

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `views/chatlist/PlatformHomeRouteDesktopTest.kt` | PC1, PC32 | Medium | Regression guard for unchanged Desktop home fallback |
| `views/newchat/PlatformConnectionPreviewDesktopTest.kt` | PC12, PC32 | Medium | Regression guard for declined Desktop P13 presentation |

---

## 4. Haskell Core Impact

The Haskell core is compiled as a shared native library (`libsimplex.so` / `libsimplex.dylib`) and linked via JNI through `Core.kt`. Changes here affect both Android and Desktop identically.

| Source File | Product Concepts Affected | Risk Level | Notes |
|-------------|--------------------------|------------|-------|
| `src/Simplex/Chat.hs` | PC1 through PC31 | High | Main chat module — top-level orchestration |
| `src/Simplex/Chat/Controller.hs` | PC1 through PC31 | High | Command processor — all API commands dispatched here |
| `src/Simplex/Chat/Types.hs` | PC1 through PC31 | High | Core data types shared across all features |
| `src/Simplex/Chat/Core.hs` | PC1 through PC31 | High | Chat engine lifecycle (start, stop, subscribe) |
| `src/Simplex/Chat/Library/Commands.hs` | PC1 through PC31 | High | API command handler implementations |
| `src/Simplex/Chat/Library/Internal.hs` | PC1 through PC31 | High | Internal helpers for command processing |
| `src/Simplex/Chat/Library/Subscriber.hs` | PC1 through PC31 | High | Event subscriber — incoming message routing |
| `src/Simplex/Chat/Protocol.hs` | PC2, PC3, PC4, PC5, PC6, PC7 | High | Chat-level message protocol (x-events) |
| `src/Simplex/Chat/Messages.hs` | PC2, PC3, PC4, PC5, PC6, PC7, PC8, PC9 | High | Message types and content |
| `src/Simplex/Chat/Messages/CIContent.hs` | PC4, PC5, PC6, PC7, PC8, PC9, PC11 | Medium | Chat item content variants |
| `src/Simplex/Chat/Messages/CIContent/Events.hs` | PC3, PC14, PC16 | Medium | Group event content types |
| `src/Simplex/Chat/Messages/Batch.hs` | PC2, PC3, PC4 | Medium | Message batching for efficient delivery |
| `src/Simplex/Chat/Call.hs` | PC17 | Medium | Call signaling types |
| `src/Simplex/Chat/Files.hs` | PC10 | Medium | File transfer orchestration |
| `src/Simplex/Chat/Delivery.hs` | PC2, PC3 | Medium | Message delivery engine |
| `src/Simplex/Chat/Markdown.hs` | PC4 | Low | Markdown parsing for message formatting |
| `src/Simplex/Chat/Store.hs` | PC1 through PC31 | High | Database store interface |
| `src/Simplex/Chat/Store/Shared.hs` | PC1 through PC31 | Medium | Shared store utilities |
| `src/Simplex/Chat/Store/Messages.hs` | PC4, PC5, PC6, PC7, PC8 | High | Message persistence |
| `src/Simplex/Chat/Store/Groups.hs` | PC3, PC14, PC15, PC16, PC30 | High | Group persistence |
| `src/Simplex/Chat/Store/Direct.hs` | PC2, PC12, PC13 | High | Contact persistence |
| `src/Simplex/Chat/Store/Files.hs` | PC10 | Medium | File transfer persistence |
| `src/Simplex/Chat/Store/Profiles.hs` | PC19, PC21 | Medium | User profile persistence |
| `src/Simplex/Chat/Store/Connections.hs` | PC2, PC12 | High | Connection persistence and entity resolution |
| `src/Simplex/Chat/Store/ContactRequest.hs` | PC12 | Medium | Contact request persistence |
| `src/Simplex/Chat/Store/NoteFolders.hs` | PC1 | Low | Note folder (self-chat) persistence |
| `src/Simplex/Chat/Store/Delivery.hs` | PC2, PC3 | Medium | Delivery task persistence |
| `src/Simplex/Chat/Store/AppSettings.hs` | PC25 | Low | App settings persistence |
| `src/Simplex/Chat/Store/Remote.hs` | PC27 | Low | Remote desktop session persistence |
| `src/Simplex/Chat/Archive.hs` | PC26 | Medium | Database export/import for migration |
| `src/Simplex/Chat/Options.hs` | PC23, PC25 | Low | Startup options (DB path, key, etc.) |
| `src/Simplex/Chat/Remote.hs` | PC27 | Medium | Remote desktop protocol handler |
| `src/Simplex/Chat/Remote/Types.hs` | PC27 | Low | Remote desktop data types |
| `src/Simplex/Chat/Remote/Protocol.hs` | PC27 | Medium | Remote desktop wire protocol |
| `src/Simplex/Chat/Remote/Transport.hs` | PC27 | Low | Remote desktop transport layer |
| `src/Simplex/Chat/Remote/RevHTTP.hs` | PC27 | Low | Reverse HTTP for remote desktop |
| `src/Simplex/Chat/Remote/AppVersion.hs` | PC27 | Low | Remote version negotiation |
| `src/Simplex/Chat/ProfileGenerator.hs` | PC20 | Low | Random profile generation for incognito |
| `src/Simplex/Chat/Types/UITheme.hs` | PC24 | Low | Theme data types for UI customization |
| `src/Simplex/Chat/Types/Preferences.hs` | PC2, PC3, PC8 | Medium | Chat feature preferences (timed messages, etc.) |
| `src/Simplex/Chat/Types/Shared.hs` | PC3, PC16 | Medium | Shared types including GroupMemberRole |
| `src/Simplex/Chat/Types/MemberRelations.hs` | PC3, PC16, PC30 | Medium | Member relationship state machine |
| `src/Simplex/Chat/Operators.hs` | PC25 | Medium | Server operator management |
| `src/Simplex/Chat/Operators/Presets.hs` | PC25 | Low | Preset server operators |
| `src/Simplex/Chat/Operators/Conditions.hs` | PC25 | Low | Operator usage conditions |
| `src/Simplex/Chat/AppSettings.hs` | PC25 | Low | App settings sync types |
| `src/Simplex/Chat/Mobile.hs` | PC1 through PC31 | High | C FFI exports — JNI bridge target |
| `src/Simplex/Chat/Mobile/File.hs` | PC10 | Medium | Mobile file read/write FFI |
| `src/Simplex/Chat/Mobile/Shared.hs` | PC1 through PC31 | Medium | Shared FFI helpers |
| `src/Simplex/Chat/Mobile/WebRTC.hs` | PC17 | Low | WebRTC FFI helpers |
| `src/Simplex/Chat/View.hs` | PC1 through PC31 | Low | Terminal view rendering (not used by mobile/desktop UI) |
| `src/Simplex/Chat/Stats.hs` | PC25 | Low | Server statistics tracking |
| `src/Simplex/Chat/Util.hs` | — | Low | General Haskell utilities |
| `src/Simplex/Chat/Styled.hs` | — | Low | Terminal styled text (not used by mobile/desktop UI) |
| `src/Simplex/Chat/Help.hs` | — | Low | Terminal help text |
| `src/Simplex/Chat/Bot.hs` | — | Low | Chat bot framework |
| `src/Simplex/Chat/Bot/KnownContacts.hs` | — | Low | Bot known contacts |

---

## 5. Nome macOS ARM64 Brand and Default-Service Correction

This user-authorized batch supersedes historical statements above that Desktop must render the
upstream fallback unchanged. It is limited to the macOS ARM64 product presentation and shared
first-user defaults; protocol compatibility remains unchanged.

| Source | Concepts | Risk | Required verification |
|---|---|---|---|
| `common/src/commonMain/**/App.kt` | PC1, PC24 | High | Opaque fullscreen overlays hide obscured root semantics on Android and Desktop |
| `common/src/desktopMain/**/PlatformHomeRoute.desktop.kt` | PC1, PC24 | High | Rail navigation, conversation ownership, empty/populated states |
| `common/src/desktopMain/**/PlatformNomeOnboardingPages.desktop.kt` | PC1, PC19, PC23, PC24, PC25 | High | Every first-run step, errors, back/skip/continue |
| `common/src/commonMain/**/migration/MigrateToDevice.kt` | PC24, PC26 | High | Desktop start state uses the Nome shell and accessible paste/import actions; other platforms and later migration states are unchanged |
| `common/src/desktopMain/**/PlatformNewChatHub.desktop.kt` | PC12, PC24 | Medium | Four callbacks map one-to-one to existing owners |
| `common/src/commonMain/**/OnboardingCards.kt` | PC12, PC24 | Medium | Compact desktop branch; Android unchanged |
| `common/src/commonMain/**/chatlist/ChatListView.kt` | PC1, PC24, PC25 | Medium | Desktop keeps the shared list but does not auto-open the upstream release/conditions modal |
| `common/src/commonMain/resources/MR/{base,zh-rCN}/strings.xml` | PC1 through PC31 | High | English/Chinese visible-brand audit and text expansion |
| `common/src/commonMain/**/model/SimpleXAPI.kt` | PC25 | Medium | Decode legacy operator tags but expose only Nome branded metadata; neutral fallback for custom/legacy tags |
| `src/Simplex/Chat.hs`, `Operators.hs`, `Operators/Presets.hs` | PC25 | High | Exact Nome trust anchors, one SMP/XFTP enabled, Nome excluded from upstream conditions actions, compatibility-only historical lists |
| `src/Simplex/Chat/Store/Profiles.hs` | PC1, PC2, PC25 | High | Bounded removal of legacy upstream preset routing/conditions and disconnected exact seed cards, custom-server/real-contact preservation, Nome conditions ungated |
| `src/Simplex/Chat/Terminal.hs` | PC25 | High | CLI defaults use the same Nome SMP/XFTP presets and no default chat relay |
| `src/Simplex/Chat/Library/Commands.hs` | PC1, PC2, PC19, PC25 | High | New user has note folder and no upstream seed contacts; diagnostics use active configured NTF servers rather than a separate upstream list |
| `desktop/build.gradle.kts` | PC32 | Medium | Selects the arm64 Compose runtime and removes unused x64 native payloads from DMG task inputs before jpackage/signing |
| `desktop/src/jvmMain/resources/distribute/simplex.icns` | PC24, PC32 | Low | Nome macOS icon uses the approved mark on a mint brand surface instead of the legacy white tile |

The release gate requires isolated fresh-profile runtime evidence, direct server tests, arm64
architecture checks for the app and native library, and combined source/runtime visual review.
TCP/TLS reachability is not a passing direct-service test: SMP queue creation and XFTP file creation
must both succeed. If the client contains a shared creation credential, release review must treat it
as public and verify server-side rate limits, abuse monitoring, and credential rotation.
