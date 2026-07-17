# Threat-model delta — production shell and P07/P08 home

| Risk | Control | Verification / remaining boundary |
|---|---|---|
| Previous identity rows survive user/host change | attempt ID plus `(remoteHostId,userId)` generation; mismatch hides rows and rejects stale results | unit and API 28/API 35 adapter-to-renderer device test pass; profile-switch UI remains out of scope |
| API failure appears as true empty | typed success/failure/no-user result; only matching success may be empty | 17 unit tests and 18-test device suites pass |
| Cancellation becomes ordinary unavailable | `CancellationException` is rethrown through refresh callers | Android unit and Desktop cancellation tests pass |
| Optimistic sentinel appears online | nullable first Android platform observation | unit/device integration pass; instantaneous live cold-start UNKNOWN frame not claimed |
| Device offline implies relay/global outage | independent device-only axis and explicit copy | API 28/API 35 airplane-mode traces and screenshots pass |
| Core stopped appears empty or remains actionable | independent core axis; cached rows retained; navigation removed | API 35 explicit real-core gate retains the exact three-chat ID sequence across official stop/start; stopped UI tree makes all rows non-clickable/read-only; Compose guards pass |
| Pending-deletion/contact/request/connection row opens | projection and click-time guards suppress actions | API 28/API 35 Compose tests pass |
| Preview text leaks when disabled | visible and spoken summary follows existing `showChatPreviews` | API 28/API 35 Compose test passes |
| Fixture is presented as real core | visible debug badge; live-core evidence separated | screenshot review and evidence map pass |
| Debug harness ships in release | debug source set, non-exported manifest, release APK scan | release manifest/DEX hits 0; debug package contains harness |
| Screenshot captures real user content | synthetic rows/profile/timestamps only | 80-image review pass; real-core captures are stored separately |
| Desktop behavior changes | Desktop actual invokes unchanged `defaultContent()` | 12/12 Desktop tests pass |
| Renderer-only state is called production reachable | evidence and docs keep `FIRST_USE` / `FILTERED_NO_RESULT` deferred | review must reject any stronger claim |
| Same-generation refresh flashes empty/skeleton | cached rows stay visible when `hideRows=false` | API 35 21-frame refresh sequence and device integration pass |
| Same-package upgrade erases user data | no-clear-data overlay install; exact selected files hashed before/after | five selected checkpoints byte-identical; claim is limited to those files |

No Haskell/native core, `Core.kt`, database/archive format, protocol, command/event contract,
message state machine, iOS source, or Desktop Nome UI is part of this delta.
