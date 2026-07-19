# Nome Android page completion matrix

Date: 2026-07-18
Authority: the user execution-contract amendment recorded in `README.md`

This matrix tracks production route/fidelity coverage. `P01–P24` images are page-level visual
acceptance baselines: the corresponding Android production page must be compared against its
baseline for composition, hierarchy, placement, styling, iconography, action sizing, and density.
Every shipped state must still come from the official v6.5.6 route/model/API/core/platform fact;
fictional or unsupported facts in a design are not production producers.

Status values:

- `FROZEN` — implementation and checkpoint already closed; do not repeat. A later explicit visual
  recheck can add presentation corrections without reopening frozen functional/security work.
- `ACTIVE` — current production work.
- `READY FOR MILESTONE` — implementation and risk-tier verification are closed; checkpoint waits
  for the concentrated milestone gate.
- `UPSTREAM + RESKIN PENDING` — official feature remains reachable; Nome shared/global visual
  convergence is pending.
- `TRUTHFULLY UNSUPPORTED` — no official source exists; do not synthesize the requested fact.
- `NOT REACHABLE` — route/capability proof excludes the state from this Android release.

## P0 visual-reference families

| Ref | Production family | Current status | Official fidelity boundary | Validation tier |
|---|---|---|---|---|
| P01 | database startup/migration/recovery | `FROZEN` | existing root order, database result, key ownership, exact matched-pair copy; no percentage/timeout/rollback/success inference | Milestone 1: API 28/API 35 real fixtures/lifecycle/authentic v6.5.4 upgrade + full concentrated matrix |
| P02 | app lock/local authentication | `FROZEN` | existing AppLock/passcode/biometric outcomes; unavailable never means authenticated | Milestone 1 depth retained; post-clarification API 35 production/baseline v4 and Milestone 2 presentation gate accepted |
| P03 | welcome/migrate entry | `FROZEN` | official onboarding order and migration entry | Milestone 1 depth retained; post-clarification API 35 production/baseline v5 and Milestone 2 presentation gate accepted |
| P04 | create local identity | `FROZEN` | existing validation/create-user outcome; local identity, not cloud account | Milestone 1 depth retained; post-clarification API 35 production/baseline v6 and Milestone 2 presentation gate accepted |
| P05 | network/operator setup | `FROZEN` | configuration/validation/device connectivity remain separate evidence levels | Milestone 1 depth retained; post-clarification API 35 production/baseline v6 and Milestone 2 presentation gate accepted |
| P06 | conditions/commitment | `FROZEN` | official conditions load/accept outcome; no routing-health claim | Milestone 1 depth retained; post-clarification API 35 production/baseline v8 and Milestone 2 presentation gate accepted |
| P07 | populated Home | `FROZEN` | official chat order/content/privacy plus current generation | frozen Batch 2 |
| P08 | Home non-populated states | `FROZEN` | loading/empty/unavailable/network/core remain independent; `FIRST_USE` stays onboarding-owned | frozen Batch 2 |
| P09 | loaded chat/message search | `FROZEN` | official loaded-chat name query; truthful message-scope/recent-retention regions; no fabricated recent/global aggregation | ordinary UI and concentrated Milestone 2 gate accepted |
| P10 | new-connection entry | `FROZEN` | official New Chat routes/actions and actual current profile/image | ordinary UI and concentrated Milestone 2 gate accepted |
| P11 | one-time invitation | `ACTIVE — PRODUCER PROVEN; READY VISUAL ACCEPTANCE OPEN` | created/copied/shared/connected remain distinct; no invented expiry, peer use, or atomic regeneration | real `CreatedConnLink` plus disk-reboot pending-row persistence pass; no safe reference-size ready capture was retained, a later attempt returned the official relay error, and peer use/connection remain absent |
| P12 | scan/paste | `READY FOR MILESTONE` | existing parser/camera/permission/duplicate/old-version outcomes; no passive clipboard read | focused tests/compile/API 35 production comparison plus camera/permission lifecycle passed |
| P13 | external connection preview | `FROZEN` | Android external ACTION_VIEW only; bearer closure-only; legacy callers unchanged | frozen Batch 3 |
| P14 | contact request | `ACTIVE — PRODUCER RESULT BLOCKED` | typed accept/reject outcomes; no unsupported request message; failure retains request | source/focused tests/review/fixture comparison pass; incoming production request still required |
| P15 | public contact method | `READY FOR MILESTONE` | exact not-found proves OFF; confirmed cache survives failure; replace is delete then create without rollback claim | source/focused tests/review, production OFF, real READY producer, disk-reboot persistence, and redacted API 35 production/reference comparison pass |
| P16 | group preview/join | `ACTIVE — PRODUCER RESULT BLOCKED` | official invited-group/profile/membership/inviter and typed join/delete results; no verified-admin, joined, connected, relay-health, or success invention | source/focused tests/review/fixture comparison pass; API 35 has no invited-group producer |
| P17 | direct conversation | `READY FOR MILESTONE — CONVERSATION SHELL; MEDIA/CALL LOCAL LIFECYCLE READY, PRODUCER BLOCKED` | official items/composer/delivery facts; no fabricated file/voice/call state | ordinary API 35 text/action acceptance plus API 28/API 35 picker/permission/recording/call lifecycle pass; real transfer/receipt/playback and connected call remain producer-blocked |
| P18 | conversation actions | `READY FOR MILESTONE` | existing reaction/edit/forward/delete/report capability boundaries; direct report remains absent | focused tests/compile/API 35 production comparison/48dp/review passed |
| P19 | contact verification | `READY FOR MILESTONE` | core verification code and result only | real API 28/API 35 compare/mark/reopen/clear/scanner lifecycle, focused tests, production comparison, and review passed |
| P20 | public channel setup | `READY FOR MILESTONE — SETUP; CREATION/LINK/DELETE PRODUCER BLOCKED` | existing group/channel relay operations; no custom Nome domain/global health | setup focused tests/compile/API 35 production comparison/review passed; real creation/relay/link/cancel depth remains open |
| P21 | channel conversation | `ACTIVE — PRODUCER RESULT BLOCKED` | role/history/relay facts plus fixed non-E2EE disclosure | source/focused tests/compile complete; real public-channel API 35 production comparison remains open |
| P22 | identity center | `READY FOR MILESTONE` | official local identities/switch/hidden/delete; incognito remains per connection | API 35 production comparison plus disposable API 28/API 35 create/switch/delete/cold-start lifecycle passed |
| P23 | settings | `READY FOR MILESTONE` | existing preference/routes; local search indexes only existing routes and does not invent health | API 35 production comparison, focused tests, 48dp, full-page navigation/back, and official route smoke passed |
| P24 | backup/archive/device migration | `READY FOR MILESTONE` | official archive/migration owners only; outbound migration disabled while chat is stopped; no synthetic progress, generic cancel/resume, rollback, restore, or success | truthful API 35 not-started production comparison plus real disposable API 28/API 35 export-cancel/save/import/key-reentry/round-trip and migration 0-byte abort/restart lifecycle passed |

`FILTERED_NO_RESULT` can become a production Home state only after P09 supplies a real active-filter
producer. No row in this matrix reclassifies a C capability as production truth.

## Android-reachable P1 families

| Family | Current status | Adjacent visual family | Closure rule |
|---|---|---|---|
| chat-list utilities | `READY FOR MILESTONE` | P07–P10/P23 | official long-press/TalkBack actions plus Android tag selection/editor production smoke, focused tests, 48dp, adjacent comparison, and review pass |
| contact detail | shared detail reskin ready for milestone; P19 verification ready | P14/P19 | Android full-page/card production comparison and focused test pass; preserve contact/security facts |
| group creation/admin | `READY FOR MILESTONE` | P10/P16/P20 | real API 35 private-group create/info/invite/profile production routes, adjacent creation/admin comparisons, focused tests, 48dp/Back/a11y, compile, review, and secure restoration pass; official roles, member/admission/preferences, save, link, moderation, and destructive confirmations remain owners |
| channel owner/admin | shared member/relay presentation ready; real channel/create/link/delete producers blocked | P20/P21 | real list/role/status/tap facts only; fixed non-E2EE disclosure retained; v6.5.6 source-disabled relay Add/Remove is not re-enabled |
| media/gallery/files | API 28/API 35 local picker/preview/recording lifecycle ready; two-client transfer/playback producer blocked | P17/P18 | real permission/transfer/playback states; text/action comparison and sender-side pending item are not transfer proof |
| calls | API 28/API 35 outgoing foreground/notification/hang-up lifecycle ready; connected-call producer blocked | P17 | unanswered is not ringing or connected; retain real permission, foreground service, notification return, and call state |
| notifications/background | `READY FOR MILESTONE` | P02/P23 | real `OFF` preference production smoke, mode/preview routes, focused tests, 48dp, adjacent comparison, and review pass; platform permission, app preference, and service state remain separate |
| profiles/privacy | P22/P23 ready | P22/P23 | no stale identity content or persistent anonymous account |
| database/recovery | P01 `FROZEN`; P24 ready for milestone | P01/P24 | disposable fixtures and exact operation outcomes |
| network/server advanced | `READY FOR MILESTONE` | P05/P23 | shared full-page production smoke/comparison, exact device-status semantics, focused compile/tests, Back, and review pass; configuration/validation/connectivity/relay/item evidence stay separate |
| appearance/localization | `READY FOR MILESTONE` | P03/P23 | API 35 Chinese/light production comparison plus official English/Chinese switch/restore pass; one-time clean-install marker defaults only jointly proven fresh data to zh-CN and preserves explicit English/follow-system upgrades |
| help/about/developer | `READY FOR MILESTONE` | P23 | Help/About Chinese/light adjacent comparisons, Developer English/light production capture, real version owner, exact source/license links, 48dp/Back/focused tests, crash correction, redaction, and review pass |
| remote desktop pairing | unpaired Android presentation ready; real pair/switch/revoke producer blocked | P23/P24 | Android full-page production smoke/comparison, 48dp/modal semantic isolation, focused tests, compile, and Back pass; real pairing/switch/revoke events and connected secret clearing still require a producer |
| Android intents/lifecycle/back | `READY FOR MILESTONE`; P13 external route frozen | matching route | ordered official dispatcher, real API 35 cold/warm ShowChats and text-share/Back, plus cold app-lock deferred share, wrong-passcode fail-closed, semantic isolation, unlock restore, Back cleanup, and lock-setting restoration pass |

## Current milestone result

- Frozen reuse: Foundation, P07/P08, P13.
- P01–P06 are frozen by the Milestone 1 checkpoint containing this matrix.
- P01 current real proof: API 28 fresh production cold start and API 35 disposable random/manual
  missing/unreadable key, wrong/correct key, lifecycle clearing, exact matched backup-pair copy, and
  fresh reopen all pass. A separate brand-new API 35 AVD also passed an authentic same-package
  v6.5.4 (353) to current (358) upgrade: official onboarding created the synthetic local identity,
  in-app confirmation produced the production update route, one update activation reached Home
  with that identity preserved, and the next cold launch did not repeat consent.
- P02–P06 functional/security implementation, focused tests, API 35 primary-route smoke, and
  affected accessibility checks remain frozen. The later targeted onboarding
  wordmark/layout/component recheck is closed without repeating that matrix: final API 35
  Chinese/light production and side-by-side evidence is P02 v4, P03 v5, P04 v6, P05 v6, and P06
  v8. Shared-button Compose coverage passes 5/5; the presentation correction is included in the
  green Milestone 2 checkpoint input.
- P02 deep checks: API 28/API 35 real platform-unavailable plus API 35 official passcode
  wrong/cancel/success/cold/background/rotation paths, all fail-closed.
- Concentrated Milestone 1 API/language/theme/scaling/TalkBack/release/regression verification:
  passed. Exact results are in `MILESTONE_1_VERIFICATION.md`.
- Milestone 3 then implemented the P11/P12 connection group without reopening frozen P07/P08;
  the concentrated non-producer matrix is now green and the exact local checkpoint input is under
  final reconciliation.
- P09/P10 ordinary closure: API 35 focused production routes, 15 focused/boundary instrumentation
  tests, Android/Desktop compile, reference-size Chinese/light side-by-side visual acceptance,
  exact screenshot-protection restoration, and the group's single risk-oriented review all pass.
  Their concentrated Milestone 2 matrix is green and the exact local checkpoint input is ready.
- The targeted P02–P06 page-level visual recheck required by the later user clarification is
  complete. Frozen functional/security work was not repeated or altered.
- Milestone 2 concentrated matrix is green: Android unit/lint/release and Desktop regression pass;
  API 28/33/35 pass 35/35/39 tests; Home/P09/P10 pass all eight bilingual/theme/scaling
  combinations and real TalkBack primary control activation; historical manifests pass
  48/48, 131/131, 254/254, and 139/139; release-isolation and forbidden-path scans are zero.
- Milestone 3 P11/P12 source is implemented over the official `NewChatView` owners. P12 focused
  tests, Android/Desktop compile, explicit camera permission/denial/Settings lifecycle, and API 35
  Chinese/light/reference-size production comparison pass, so P12 is ready for the concentrated
  milestone gate. P11 focused callback/truth coverage and compile pass. A controlled API 35 client
  later produced a real `CreatedConnLink`; after emulator disk reboot, Home retained the read-only
  pending invitation row. The ready bearer screen was not retained safely at the reference
  viewport, and a later bounded attempt returned the official `smp12.simplex.im` connection error,
  so P11 ready-state visual acceptance stays open. Neither the pending row nor the local invitation
  is peer use or connection proof.
- Milestone 3 P14/P15 source is implemented over the official contact-request and user-address
  owners. Typed reject failure retains the request; exact address not-found alone proves OFF;
  cached confirmed addresses survive lookup failure; short-link/profile-sharing behavior remains
  official; and delete-then-create replacement has a create-only recovery phase. Five focused API
  35 tests, Android/Desktop compile, one risk review, and reference-size Chinese/light renderer
  fixture comparisons pass. The fixtures calibrate presentation only. P15 production OFF smoke
  passes, and a later controlled API 35 producer created a real reusable address that remained
  available after emulator disk reboot. The redacted READY production/reference comparison passes
  without retaining the bearer or QR, so P15 is ready for the milestone. A second API 35 client
  reached P14 request confirmation using that real address but then returned the official
  `smp9.simplex.im` connection error; no request appeared on the source client, so P14 remains
  producer-blocked.
- Milestone 3 P16 source is implemented over the official `GroupMemberStatus.MemInvited` owner.
  Typed accepted/unavailable/not-completed join results retain the established command/model/error
  behavior; group/channel/inviter facts come from `GroupInfo` and a real resolved contact, and
  delete remains explicitly confirmed. Three focused API 35 tests, Android/Desktop compile, one
  risk review, and the P16 Chinese/light/reference-size renderer comparison pass. The current
  production client has no invited group, so the fixture is not promoted and producer-backed
  primary-state acceptance remains open.
- Milestone 3 P19 contact verification is ready: Android direct-contact presentation preserves the
  exact code and official mark/clear/share/scan owners; controlled API 28/API 35 clients completed
  compare/mark/reopen/clear/scanner-cancel lifecycle, focused tests and same-size API 35 production
  comparison passed, and the matched-scan modal defect found in review was fixed and re-reviewed
  with zero remaining issues.
- Milestone 3 P20 setup is ready: the Android page preserves the official configured-relay,
  create/join/settings/profile owners and passed focused tests, compile, API 35 production
  comparison, and review. Two controlled API 35 create attempts returned the official relay
  timeout before group creation; creation/link/delete remain producer-blocked and no result is
  inferred.
- P21 source/tests are complete over the official public-channel/member facts, but no controlled
  client contains a real public channel, so no fixture is promoted to production visual
  acceptance. P21 remains producer-blocked.
- P22 is ready for the milestone: the Android identity center preserves official users/actions,
  actual incognito/SOCKS preferences, and navigation; API 35 production visual acceptance passes;
  controlled API 28/API 35 disposable identities passed active deletion, fallback switching,
  inactive cleanup on API 28, and cold-start absence with zero typed/fatal deletion failures.
- P23/P24 are ready for the milestone after API 35 production visual acceptance, focused API 28/
  API 35 tests, full-page route/back smoke, official preference/archive/migration dispatch, and
  real disposable API 28/API 35 archive round-trip plus outbound migration abort/restart depth.
  Both migration attempts reached only the official `0 bytes uploaded` / `0%` state and are not
  claimed successful.
- P17's conversation shell remains ready for the milestone. Its high-risk Android-reachable
  media/file/call lane has real API 28/API 35 picker, preview/cancel, permission denial/recovery,
  recording cleanup, foreground service, notification-return, background/rotation, and hang-up
  proof. Real two-client transfer/receipt/download/playback and a connected call remain
  producer-blocked and are not inferred from sender-side pending items or unanswered calls.
- The merged P1 contact-detail/chat-list-utilities/notification-background group is ready for the
  milestone. Android preserves official contact/security, Home generation/read-only boundaries,
  chat actions, tag validation/mutations, notification preferences, and platform transitions while
  using the shared Nome full-page/card/list/input presentation. Sequential Android/Desktop compile,
  final API 35 `OK (16 tests)`, production smoke, adjacent reference-size comparisons, 48dp/
  TalkBack checks, one review, and settled `SECURE` restoration pass.
- The remote-desktop/unlocked-intent ordinary lane is ready for the milestone. Android presents
  the official remote states in the P23 full-page/grouped hierarchy while Desktop remains legacy;
  linked-device removal is confirmed, opaque modal background semantics are hidden, and cold/warm
  intents share the ordered official notification/view/share dispatcher. Android/Desktop compile,
  API 35 `OK (9 tests)`, real unpaired production smoke, adjacent P23 comparison, cold/warm
  ShowChats and text-share/Back lifecycle, 48dp/a11y isolation, and `SECURE` restoration pass.
  Actual pairing/switch/revoke remains explicitly open and is not inferred. Concentrated API 35
  locked/deferred depth is now closed: a real temporary app passcode held a cold text-share intent,
  wrong passcode stayed locked, no underlying fullscreen/profile/share nodes escaped after the
  modal-host semantic-isolation fix, correct passcode restored the official Share list, Back
  cleared the share, and the official setting disabled the temporary lock again.
- The private-group creation/admin P1 family is ready for the milestone. One controlled API 35
  client created a real private group without inviting contacts or sending messages. Android
  creation, group info, invitation, and profile routes preserve the official create/model/member/
  role/admission/preferences/save/destructive owners while using page-level Nome presentation;
  Desktop remains legacy. Three focused tests, Android/Desktop compile, adjacent P20/P16 primary
  comparisons, single-Back and unchecked-contact semantics, one review, and `SECURE` restoration
  pass. Channel member/relay presentation shares the ready route, but no private-group evidence is
  promoted as a public-channel result and P20/P21 producers remain blocked.
- The concentrated Milestone 3 non-producer matrix is green. API 28/API 33/API 35 passed
  67/67/72 instrumentation tests; Android JVM passed 42 tests; Desktop passed 35 tests; the final
  Android unit/lint/debug/androidTest/release aggregate completed successfully. The populated API
  35 client passed all eight Chinese/English, light/dark, 100%/200% Home combinations and real
  TalkBack activation; historical manifests passed 48/48, 131/131, 254/254, and 139/139. Exact
  artifacts, restoration, release-isolation checks, P15 redacted READY comparison, and truthful
  producer retries are recorded in `MILESTONE_3_VERIFICATION.md`.
