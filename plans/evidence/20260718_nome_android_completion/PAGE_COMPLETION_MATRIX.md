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
| P11 | one-time invitation | `UPSTREAM + RESKIN PENDING` | created/copied/shared/connected remain distinct; no invented expiry | ordinary UI |
| P12 | scan/paste | `UPSTREAM + RESKIN PENDING` | existing parser/camera/permission/duplicate outcomes | platform-risk where applicable |
| P13 | external connection preview | `FROZEN` | Android external ACTION_VIEW only; bearer closure-only; legacy callers unchanged | frozen Batch 3 |
| P14 | contact request | `UPSTREAM + RESKIN PENDING` | official accept/reject/address outcomes; no unsupported request message | ordinary UI |
| P15 | public contact method | `UPSTREAM + RESKIN PENDING` | existing address enable/share/disable/replace operations remain explicit | ordinary UI |
| P16 | group preview/join | `UPSTREAM + RESKIN PENDING` | parsed group/profile/join facts; no verified-admin invention | ordinary UI |
| P17 | direct conversation | `UPSTREAM + RESKIN PENDING` | official items/composer/files/voice/delivery events | priority flow |
| P18 | conversation actions | `UPSTREAM + RESKIN PENDING` | existing reaction/edit/forward/delete/report capability boundaries | priority flow |
| P19 | contact verification | `UPSTREAM + RESKIN PENDING` | core verification code and result only | high risk |
| P20 | public channel setup | `UPSTREAM + RESKIN PENDING` | existing group/channel relay operations; no custom Nome domain/global health | ordinary UI |
| P21 | channel conversation | `UPSTREAM + RESKIN PENDING` | role/history/relay facts plus fixed non-E2EE disclosure | priority flow |
| P22 | identity center | `UPSTREAM + RESKIN PENDING` | official local identities/switch/hidden/delete; incognito remains per connection | priority/high risk |
| P23 | settings | `UPSTREAM + RESKIN PENDING` | existing preference/routes; local search may index routes but not invent health | priority flow |
| P24 | backup/archive/device migration | `UPSTREAM + RESKIN PENDING` | official archive/migration stages only; no generic cancel/resume/rollback | high risk, disposable data only |

`FILTERED_NO_RESULT` can become a production Home state only after P09 supplies a real active-filter
producer. No row in this matrix reclassifies a C capability as production truth.

## Android-reachable P1 families

| Family | Current status | Adjacent visual family | Closure rule |
|---|---|---|---|
| chat-list utilities | shared reskin pending | P07–P10 | preserve official list/search/tag/actions |
| contact detail | shared reskin pending | P14/P19 | preserve contact/security facts |
| group creation/admin | shared reskin pending | P10/P16 | preserve roles and destructive confirmations |
| channel owner/admin | shared reskin pending | P20/P21 | retain non-E2EE and relay evidence level |
| media/gallery/files | shared reskin pending | P17/P18 | real permission/transfer/playback states |
| calls | shared reskin pending | P17 | high-risk platform/call lifecycle |
| notifications/background | shared reskin pending | P02/P23 | platform permission, app preference, service state remain separate |
| profiles/privacy | shared reskin pending | P22/P23 | no stale identity content or persistent anonymous account |
| database/recovery | P01 `FROZEN`; P24 pending | P01/P24 | disposable fixtures and exact operation outcomes |
| network/server advanced | shared reskin pending | P05/P23 | configuration/validation/connectivity/relay/item evidence stay separate |
| appearance/localization | shared reskin pending | P23 | user preference preserved; locale initialization remains conservative |
| help/about/developer | shared reskin pending | P23 | compatibility/license/diagnostics truth and redaction |
| remote desktop pairing | shared reskin pending | P23/P24 | real pairing/switch/revoke events; secret cleared |
| Android intents/lifecycle/back | P13 external route frozen; remainder pending | matching route | cold/warm/locked/deferred/share/notification/back preserve official ownership |

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
- Next active work after the checkpoint: merged P11 one-time invitation and P12 scan/paste;
  frozen P07/P08 remain unchanged.
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
