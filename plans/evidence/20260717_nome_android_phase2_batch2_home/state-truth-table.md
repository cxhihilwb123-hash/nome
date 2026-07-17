# P07/P08 adapter truth table and production reachability

| Content input | Generation/core fact | Rows | Renderer result | Verification |
|---|---|---|---|---|
| initial or hide-row in-flight | any current/switching identity | any | loading; rows hidden | unit + API 28/API 35 Compose |
| same-generation refresh | running | cached non-empty | loading provenance; populated cached rows remain visible | unit + API 28/API 35 adapter-to-renderer + API 35 refresh frames |
| generation mismatch or switching | any | stale rows exist | loading; stale rows hidden | unit + API 28/API 35 adapter-to-renderer |
| no current user | running | zero | first use | renderer/unit only; production root consumes onboarding first |
| same-generation success | running | zero | true empty | unit + deterministic API 35 renderer |
| same-generation success + active filter | running | base non-empty, visible zero | filtered no result | renderer/unit only; no production filter producer |
| same-generation success | running | visible non-empty | populated | unit + API 28/API 35 real core |
| same-generation typed failure | any | zero | unavailable, never empty | unit + deterministic API 35 renderer |
| same-generation typed failure | running | cached non-empty | unavailable panel + guarded cached rows | unit + deterministic API 35 renderer |
| stale typed failure | changed identity/host | any | loading; prior rows hidden | unit |
| same-generation success/cache | stopped | any | stopped banner + read-only cached rows | Compose + API 35 explicit real-core stop/start gate |

Production-home reachability is deliberately narrower than the adapter table:

- `FIRST_USE` remains `DEFERRED`: the unchanged onboarding root owns the no-user route.
- `FILTERED_NO_RESULT` remains `DEFERRED`: the bounded home has no search/filter producer or
  clear-filter control.
- Their 16 screenshots are renderer evidence carrying the debug “not live core” badge. They are
  not counted as production-route or real-core PASS.

| Android platform observation | Connectivity state | Allowed claim |
|---|---|---|
| not yet observed | unknown | Android has not reported validated device network truth |
| validated active network | online | device connectivity only |
| no validated active network | device offline | device connectivity only; not relay/global health |

The legacy optimistic model sentinel is not accepted as the platform observation. A production
cold-start `UNKNOWN` frame was too short to capture because `restartNetworkObserver()` emitted
connectivity before home composition; the branch is therefore proved by unit tests, API 28/API 35
adapter-to-renderer device integration, and deterministic renderer screenshots, not falsely by a
live-core screenshot.

Navigation guards:

- ready direct/group/local rows may open only while core is running;
- contact card, contact request, pending connection, invalid/not-ready, pending-deletion and
  stopped-core rows expose no chat-open action;
- an unavailable panel alone does not turn a running, otherwise-ready cached row into stopped.
