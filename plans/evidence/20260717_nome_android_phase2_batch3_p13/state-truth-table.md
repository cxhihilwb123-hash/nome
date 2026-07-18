# P13 typed state truth table

| Input / response | P13 state or action | Proof level |
|---|---|---|
| Typed plan succeeds in one of seven eligible branches | `Ready`, current identity initially selected unless retry retained incognito | common policy tests + source |
| Any of the other 14 normalized branches | existing legacy controller path | exhaustive enum + route-boundary test |
| Short-link preparation data exists | existing prepare alert, never P13 | common policy tests + source |
| User selects current/incognito while ready | mutually exclusive selection | reducer + API 28/API 35 Compose tests |
| Continue from ready | `Connecting`; one reducer transition and one atomic controller submit | reducer/source |
| Recomposition or second submit | ignored/disabled | reducer + Compose semantics; eight real rapid taps yielded one visible Failure outcome, raw command count open |
| Real `SentConfirmation` or `SentInvitation` | update the returned real `PendingContactConnection`, then `Pending` | typed source contract + two controlled invitation attempts |
| `ContactAlreadyExists` | typed failure with existing display name | typed source contract |
| Typed API/network failure | sanitized `Failure`; raw response is not UI state | common mapping tests + production address network failure |
| Active user or remote host differs from plan context | `ContextChanged`; no connect command | context guard tests |
| Retry from failure | `Replanning`, rerun authoritative planning, retain identity; plan failure/no-user returns to `Failure`, while a successful or changed branch hands off once | reducer/source + production retry returned to fresh Ready; changed-branch trace open |
| Cancellation exception | propagate cancellation and run cleanup | source; lifecycle trace open |
| Cancel before submit | cleanup and close, no connect command | source + production address cancel with zero rows/requests on both clients |

Renderer fixtures exercise ready-current, ready-incognito, own-link warning, repeat-join warning,
owner verified, owner failed, connecting, pending, and failure. Fixture owner-proof rows are not
classified as real-core success. The production invitation attempts independently proved current
and incognito `Pending`.
