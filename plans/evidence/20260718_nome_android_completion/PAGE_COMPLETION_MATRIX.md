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
| P11 | one-time invitation | `READY FOR FINAL` | created/copied/shared/connected remain distinct; no invented expiry, peer use, or atomic regeneration | real `CreatedConnLink`, disk-reboot pending-row persistence, bearer-free semantics, 136dp baseline QR correction, focused Android/Desktop verification, and redacted API 35 production/reference acceptance pass; peer use/connection remain absent |
| P12 | scan/paste | `READY FOR MILESTONE` | existing parser/camera/permission/duplicate/old-version outcomes; no passive clipboard read | focused tests/compile/API 35 production comparison plus camera/permission lifecycle passed |
| P13 | external connection preview | `FROZEN` | Android external ACTION_VIEW only; bearer closure-only; legacy callers unchanged | frozen Batch 3 |
| P14 | contact request | `READY FOR MILESTONE` | typed accept/reject outcomes; no unsupported request message; failure retains request | official `ContactRequest` and pending `Direct + contactRequestId` shapes share the production route and official IDs/actions; focused tests, API 35 real pending-request production/reference comparison, privacy restoration, and risk review pass; earlier relay failures remain historical |
| P15 | public contact method | `READY FOR MILESTONE` | exact not-found proves OFF; confirmed cache survives failure; replace is delete then create without rollback claim | source/focused tests/review, production OFF, real READY producer, disk-reboot persistence, and redacted API 35 production/reference comparison pass |
| P16 | group preview/join | `PRIVATE PRODUCTION READY; PUBLIC REFERENCE VISUAL NOT REACHABLE LOCALLY` | official invited-group/profile/membership/inviter and typed join/delete results; no verified-admin, joined, connected, relay-health, or success invention | focused tests/review plus real peer invitation/join/group-message proof and API 35 private-invitation production calibration; the retained public-reference/private-production comparison is structural only, not page-state acceptance |
| P17 | direct conversation | `READY FOR MILESTONE — CONTROLLED FILE/CALL/MEDIA READY` | official items/composer/delivery facts; no fabricated file/voice/call state | ordinary API 35 text/action acceptance, API 28/API 35 picker/permission/recording/call lifecycle, real API 35-to-API 28 synthetic image-file receipt/download/external-open/cleanup, real API 35-to-API 35 incoming/answer/connected-call/clean-end, and fixed synthetic voice/video API 35 send/API 28 receipt/digest/playback/stop/cleanup pass |
| P18 | conversation actions | `READY FOR MILESTONE` | existing reaction/edit/forward/delete/report capability boundaries; direct report remains absent | focused tests/compile/API 35 production comparison/48dp/review passed |
| P19 | contact verification | `READY FOR MILESTONE` | core verification code and result only | real API 28/API 35 compare/mark/reopen/clear/scanner lifecycle, focused tests, production comparison, and review passed |
| P20 | public channel setup | `READY FOR MILESTONE` | existing group/channel relay operations; no custom Nome domain/global health | setup focused tests/compile/API 35 production comparison/review plus real single-process create/link/populate/open/delete/post-delete-absence lifecycle passed |
| P21 | channel conversation | `OWNER STATE READY; OBSERVER VISUAL NOT REACHABLE LOCALLY` | role/history/relay facts plus fixed non-E2EE disclosure | real public-channel owner route, truthful text/file post cards with official `CIFileView` receive/progress/error/open/save behavior preserved, focused tests/compile, and API 35 Chinese/light same-size owner comparison pass; the comparison does not close the reference's observer-only footer/composer state; observer membership/receipt remains unproven after persisted, fresh-link, and latest same-version API 35 requesters reached real `Pending` without an active owner member or ready target channel/post; the inverse API 28 owner-create attempt and a disposable API 35 `Created` result both ended without a controlled persisted channel |
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
| channel owner/admin | `READY FOR MILESTONE — OWNER STATE` | P20/P21 | real create/link/populate/open/delete/absence plus owner list/role/status facts pass; fixed non-E2EE disclosure retained; observer receipt is not inferred and v6.5.6 source-disabled relay Add/Remove is not re-enabled |
| media/gallery/files | API 28/API 35 local lifecycle plus controlled two-client image-file transfer/open and voice/video playback ready | P17/P18 | image exact peer file name/size/bytes/download/external-view/return/cleanup plus fixed synthetic voice/video peer receipt/decrypted SHA-256/production playback progress/termination/stop/cleanup pass; text/action comparison and sender-side creation alone remain insufficient |
| calls | API 28/API 35 outgoing foreground/notification/hang-up lifecycle plus controlled API 35/API 35 incoming/answer/connected/clean-end ready | P17 | unanswered remains distinct; retain real permission, foreground service, notification return, invitation, connected state/metadata, remote end, and cleanup facts |
| notifications/background | `READY FOR MILESTONE` | P02/P23 | real `OFF` preference production smoke, mode/preview routes, focused tests, 48dp, adjacent comparison, and review pass; platform permission, app preference, and service state remain separate |
| profiles/privacy | P22/P23 ready | P22/P23 | no stale identity content or persistent anonymous account |
| database/recovery | P01 `FROZEN`; P24 ready for milestone | P01/P24 | disposable fixtures and exact operation outcomes |
| network/server advanced | `READY FOR MILESTONE` | P05/P23 | shared full-page production smoke/comparison, exact device-status semantics, focused compile/tests, Back, and review pass; configuration/validation/connectivity/relay/item evidence stay separate |
| appearance/localization | `READY FOR MILESTONE` | P03/P23 | API 35 Chinese/light production comparison plus official English/Chinese switch/restore pass; one-time clean-install marker defaults only jointly proven fresh data to zh-CN and preserves explicit English/follow-system upgrades |
| help/about/developer | `READY FOR MILESTONE` | P23 | Help/About Chinese/light adjacent comparisons, Developer English/light production capture, real version owner, exact source/license links, 48dp/Back/focused tests, crash correction, redaction, and review pass |
| remote desktop pairing | `READY FOR MILESTONE` | P23/P24 | Android full-page unpaired production smoke/comparison, 48dp/modal semantic isolation, focused tests, compile, and Back pass; controlled API 28 remote-host/API 35 Android-controller official pair/code-match/connected/switch-local/stop/delete/list-absence lifecycle passes with ephemeral invitation and removed loopback routes |
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
  v8. The 2026-07-19 reconfirmation keeps these images as page-level visual acceptance baselines,
  not loose direction references. The adjacent fallback onboarding seam now uses the approved
  104 × 44 dp image wordmark instead of text-only `Nome`; API 35 focused Compose coverage passes
  6/6. The presentation correction remains outside frozen functional/security ownership and will
  enter the final RC checkpoint rather than reopening Milestone 2.
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
  so P11 ready-state visual acceptance was still open at that checkpoint. A later bearer-redacted
  READY production/baseline comparison closed the page visual; neither the pending row nor the
  local invitation is peer use or connection proof.
- Milestone 3 P14/P15 source is implemented over the official contact-request and user-address
  owners. Typed reject failure retains the request; exact address not-found alone proves OFF;
  cached confirmed addresses survive lookup failure; short-link/profile-sharing behavior remains
  official; and delete-then-create replacement has a create-only recovery phase. Five focused API
  35 tests, Android/Desktop compile, one risk review, and reference-size Chinese/light renderer
  fixture comparisons pass. The fixtures calibrate presentation only. P15 production OFF smoke
  passes, and a later controlled API 35 producer created a real reusable address that remained
  available after emulator disk reboot. The redacted READY production/reference comparison passes
  without retaining the bearer or QR, so P15 is ready for the milestone. A second API 35 client
  reached P14 request confirmation using that real address but then returned the official relay
  error. The old owner oracle inspected only `ChatInfo.ContactRequest`; the official delivered
  request was already present as a pending `ChatInfo.Direct + contactRequestId`. The corrected
  producer oracle and Android Home route recognize both official shapes while preserving the same
  IDs, actions, and post-accept follow-through. The real API 35 request page passes same-size
  Chinese/light production comparison, focused tests, privacy restoration, and one risk review
  with final `ZERO ISSUES`, so P14 is ready. Earlier relay/pending-without-old-oracle results remain
  historical diagnostics and are not rewritten as online, delivery, or success claims.
  The final production/reference evidence masks both controlled identity regions before its first
  file write. A pending `Direct + contactRequestId` row announces the contact-request semantic to
  TalkBack while preserving its official P14 navigation and post-accept owners.
- Milestone 3 P16 source is implemented over the official `GroupMemberStatus.MemInvited` owner.
  Typed accepted/unavailable/not-completed join results retain the established command/model/error
  behavior; group/channel/inviter facts come from `GroupInfo` and a real resolved contact, and
  delete remains explicitly confirmed. Three focused API 35 tests, Android/Desktop compile, one
  risk review, and the P16 Chinese/light/reference-size renderer calibration pass. Final-RC
  producer recovery added real controlled direct-message peer receipt and private-group
  invitation/join/group-message receipt. The pending `MemInvited` state was transferred through
  the official archive/import and ephemeral database-key recovery path to a disposable API 35
  client. The retained same-size comparison validates shared structure only: its reference is a
  public invitation while production is a private invitation with different membership/inviter
  facts. The public reference state is `NOT REACHABLE LOCALLY`; the private route is production
  ready. The disposable clone and archive were removed after capture; no key or bearer entered
  evidence.
- Milestone 3 P19 contact verification is ready: Android direct-contact presentation preserves the
  exact code and official mark/clear/share/scan owners; controlled API 28/API 35 clients completed
  compare/mark/reopen/clear/scanner-cancel lifecycle, focused tests and same-size API 35 production
  comparison passed, and the matched-scan modal defect found in review was fixed and re-reviewed
  with zero remaining issues.
- Milestone 3 P20 is ready: the Android page preserves the official configured-relay,
  create/join/settings/profile owners and passed focused tests, compile, API 35 production
  comparison, and review. Earlier relay timeouts remain historical. A later controlled API 35
  single-process run completed real create/link/populate/open/delete/post-delete-absence in
  126.304s without logging the link.
- P21 is ready for the real owner state. The production public-channel route renders only actual
  text/file message content as the baseline post-card composition, while file posts embed the
  official `CIFileView` and retain real receive, wait, progress, error, open, save, menu, and
  callback behavior. The route passed a Chinese/light reference-size owner comparison after
  visible structure corrections. Its real 154-byte local
  attachment row is not a remote-file or delivery claim; the official owner composer is retained
  rather than relabelled observer-only. The comparison therefore does not close the reference's
  observer-only visual state. Controlled observer attempts did not prove membership/receipt; the
  observer baseline is declared externally `NOT REACHABLE LOCALLY`. The current androidTest-only oracle now
  preflights every relay carried by the actual short-link plan and compares `ChatModel` with
  official `apiGetChatsResult`. The two link relays failed before prepare/connect, a separate API
  35 observer failed all three relays at `GetLink / BROKER/NETWORK`, and a different one-relay
  owner create produced no persisted channel after cold restart. No relay-health, timeout,
  membership, receipt, online, or success fact is inferred. Forward-layout channel entry now
  resolves a real `openAroundItemId` before creating its `LazyListState`, so a deep link opens
  around the requested post instead of defaulting to index zero; a focused regression covers the
  target and no-target paths.
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
  proof. A real API 35-to-API 28 synthetic image-file path also passes exact peer receipt,
  download, external Photos open/return, and controlled cleanup. A real API 35-to-API 35 official
  incoming/answer/connected/clean-end path also passes after controlled archive/key recovery of
  the peer state. Fixed synthetic AAC and H.264/AAC items also pass real API 35 sender/API 28 peer
  receipt, exact decrypted SHA-256, production audio/video playback progress/termination, stop,
  and cleanup. The older API 28 WebView, decryption, and connection-timeout failures remain
  historical; no file, unanswered-call, or connected-call result is used as a media proxy.
- Final responsive depth also exercised P17 on API 35 at English 130%/reduced-motion small,
  landscape, tablet, and foldable-inner bounds. The discovered fixed-height E2EE disclosure
  clipping is corrected with a font-scale-aware Android height/inset and focused 85%/100%/130%/
  200% coverage; the production Back/More/composer actions remained reachable and the restored
  production client is again Chinese/100%/portrait/screenshot-protected.
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
  A later controlled API 28 host/API 35 Android-controller fixture passes official pair,
  matching-session-code verification, connected state, switch-local, stop, delete, and both-list
  absence; the invitation stayed in ephemeral memory and no scanner state was promoted. Concentrated API 35
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
  pass. Channel member/relay presentation shares the ready route. Separate controlled
  public-channel evidence now closes P20/P21 owner-state production; no private-group evidence is
  promoted as a public-channel result and no observer receipt is inferred. The terminal external
  Observer diagnostic uses the official channel short-link `plan -> prepare group -> connect
  prepared group` path. It created a truthful prepared `Observer / MemUnknown` target with
  accepted relay members but did not produce a terminal relay join, owner Observer, ready target
  channel, or post receipt. Stale controlled prepared data was officially removed on the disposable observer; a
  subsequent one-relay owner-create attempt left no new persisted channel. A later safe preflight
  found that the two relay addresses in the actual short link both failed on the observer before
  prepare/connect, while model and core reloads agreed that no stale or hidden observer channel
  existed. A separate API 35 observer also failed at `GetLink`, and a different one-relay create
  again left no persisted channel. On the third consecutive Goal-turn audit, the source again
  passed all three configured official relay tests while the observer's configured tests stopped
  at `Connect / Connect / GetLink`, all sanitized `BROKER/NETWORK`. Both relay addresses in a
  newly generated 70-byte short link then stopped at `Connect`; the fixture failed closed before
  prepare/connect. Source membership remained two connected relays plus one invited relay with no
  Observer, and target model/core views agreed on zero prepared or hidden channel state. This is
  negative external evidence only. After the user's 2026-07-20 completion amendment it is a
  declared non-blocking local-RC validation gap, not a successful membership or receipt result.
  Future closure still requires every actual-link relay to pass the observer's official test, a
  persisted owner create/link state, real owner Observer membership, a ready target Observer
  channel, and controlled post receipt.
- The historical concentrated Milestone 3 non-producer matrix was green at
  67/67/72 instrumentation tests, 42 Android JVM tests, and 35 Desktop tests; the final current
  88/88/88 device matrix and 42-test Desktop suite below supersede those historical counts. The
  Android unit/lint/debug/androidTest/release aggregate completed successfully. The populated API
  35 client passed all eight Chinese/English, light/dark, 100%/200% Home combinations and real
  TalkBack activation; historical manifests passed 48/48, 131/131, 254/254, and 139/139. Exact
  artifacts, restoration, release-isolation checks, P15 redacted READY comparison, and truthful
  producer retries are recorded in `MILESTONE_3_VERIFICATION.md`.
- The final current-source aggregate is green. API 28 passed `OK (88 tests)` in 93.514s and
  API 33 passed `OK (88 tests)` in 138.579s with explicit
  `nomeControlledProducerSkip=true` and `nomeCrossApiScreenshotSkip=true`. API 35 passed
  `OK (88 tests)` in 511.513s with only `nomeControlledProducerSkip=true`, so every API-35
  screenshot body executed normally. API-35-only screenshot actions fail on the wrong API by
  default and skip only under the explicit lower-API screenshot argument. The general matrix
  executes ordinary applicable UI/fixture coverage but explicitly bypasses the 13 parameterized
  controlled producer/lifecycle bodies, so it is not producer-execution evidence. On API 28,
  those 13 classes produced the expected 13 failures in 0.066s without the producer bypass and
  `OK (13 tests)` in 0.066s only with it; actual producer/lifecycle results remain tied to focused
  controlled fixture runs. The separate P14 guarded capture likewise fails closed on API 35
  without its exact token unless the explicit general-regression producer bypass is present. Its
  exact no-token run produced the expected failure in 0.106s; the explicit bypass passed
  `OK (1 test)` in 0.046s and is not capture evidence. The
  known screenshot-harness idle condition around the real infinite
  database `Opening` animation was avoided by temporarily setting API 35 system animation scales
  to zero; all scales were restored to 1 afterward. All three filtered Nome
  FATAL/ANR counts are zero and all three passed exact-component cold starts. API 28 and API 33
  retain their controlled populated clients; the older API 33 genuine-first-use observation is
  historical rather than current after authorized producer work. The exact API 35 install was
  fresh because the package was absent, so its disposable client completed official Chinese
  onboarding with a synthetic local identity before the final explicit-component cold start.
  API 35 populated Chinese Home is again 1080×2400/420dpi, font 1.0, portrait/auto-rotate,
  animation scales 1, accessibility-off, no per-app locale override, and screenshot-protected.
