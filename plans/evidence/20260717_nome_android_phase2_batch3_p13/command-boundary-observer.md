# P13 host-only command-boundary observer

Date: 2026-07-18
Status: production command/lifecycle matrix closed; batch freeze still open

## Boundary and privacy contract

`tools/P13JdiCommandObserver.java` runs only on the host and attaches to a debuggable process over
an ADB JDWP forward. It registers method-entry requests filtered to the primary constructors of:

- `chat.simplex.common.model.CC$APIConnectPlan`
- `chat.simplex.common.model.CC$APIConnect`

The observer does not request or read arguments, local variables, object fields, return values,
responses, URI text, or bearer data. Its output is limited to the operator-provided attempt ID,
event sequence, command/lifecycle category, class/method, thread name, and aggregate counts. It is
outside all Android source sets and is not packaged into debug or release APKs.

## API 35 feasibility attempt

Environment:

- device: `emulator-5554`, API 35
- installed package: `chat.simplex.app.nome.dev`
- JDWP forward: `tcp:8700` to the running app PID
- attempt: `p13-jdi-feasibility-001`
- input: explicitly synthetic, non-secret invalid contact URI
- action: external `android.intent.action.VIEW`

Observer output:

```text
OBSERVER_READY attempt=p13-jdi-feasibility-001 timeoutSeconds=20
COMMAND attempt=p13-jdi-feasibility-001 seq=1 command=PLAN event=CONSTRUCTED class=chat.simplex.common.model.CC$APIConnectPlan method=<init>
SUMMARY attempt=p13-jdi-feasibility-001 plan=1 connect=0 events=1
```

Verdict:

- the host JDI/JDWP route observes the real command-construction boundary exactly;
- the invalid planning path produced exactly one plan command and zero connect commands;
- this feasibility attempt does not close the production success/retry/cancel/double-submit count
  matrix and is not a substitute for real two-client evidence.

## Accepted production matrix

All attempts below used the real `MainActivity` external `ACTION_VIEW` route and the real
controller/core. No renderer fixture supplied a terminal result.

| Attempt | Production outcome | PLAN | CONNECT |
|---|---|---:|---:|
| `api35-invite-cancel-001` | cancel before submit | 1 | 0 |
| `api35-invite-rapid-001` | eight UI activations, one real `Pending` | 1 | 1 |
| `api35-invite-incognito-001` | real network `Failure` | 1 | 1 |
| `api35-incognito-retry-002` | retry/replan returned fresh `Ready` | 1 | 0 |
| `api35-incognito-retry-002-connect` | one post-retry connect, real `Failure` | 0 | 1 |
| `api35-inflight-recreation-002` | rotation + background/resume, real `Pending` | 1 | 1 |
| `api35-address-after-invite-001` | reusable-address real `Pending` | 1 | 1 |
| `api35-context-switch-before-connect-001` | active user changed, `ContextChanged` | 1 | 0 |
| `api35-invite-cancel-complete-015` | in-flight owner Job cancellation | 1 | 1 |
| `api35-already-exists-address-014` | existing-contact planner preemption | 1 | 0 |

The retry rows are one logical retry attempt split into its planning and submit observation
windows; together they contain exactly one fresh plan and one connect. All other rows are
self-contained observation windows.

## Cancellation exception

Attempt `api35-invite-cancel-complete-015` started from a real invitation `Ready` state. After the
observer recorded `PLAN=1` and `CONNECT=1`, the debug-only `DUMP`-protected lifecycle receiver
cancelled the actual retained preview owner Job before closing the presentation. Its bounded,
event-only output was:

```text
scopes=1 activeBefore=1 activeChildrenBefore=1 activeAfter=0
cancellationClasses=kotlinx.coroutines.JobCancellationException
```

The observer independently recorded one `CANCEL_SIGNAL`; the final UI tree contained zero preview,
Pending, or Failure nodes. This proves the actual command-owning child entered a real cancellation
exception state and no returned terminal result was consumed after dismissal. The native call is
synchronous and can remain blocked after its owner is cancelled; the P13-only typed delegate and
Android owner now both call `ensureActive()` before applying a returned response.

The receiver was debug-only, permission-gated, and carried no bearer/profile/user/host/server/
response/command data. Its source and manifest entry were removed before Gate G. Final release,
debug, and standard androidTest manifest/dex scans contain zero receiver/action strings.

## `ContactAlreadyExists` production boundary

The real core response exists when `APIConnect` is called for a contact-address request hash that
already maps to an active contact. The authoritative production planner preempts that call:

1. Before the address contact was active, the same controlled address produced
   `PLAN=1 / CONNECT=1 / Pending`.
2. The API 28 controlled address owner temporarily enabled real core auto-accept, accepted the
   next request, and gained a second open contact; the original default setting was then restored
   by the paired harness (`OK (1 test)`).
3. Opening the identical address again produced `PLAN=1 / CONNECT=0` and the official
   existing-contact dialog: `AddressSender28 / 打开聊天 / 取消`.

Therefore P13 cannot receive production `ContactAlreadyExists` through the v6.5.6 external route.
The typed renderer branch remains defensive, but its real-core classification is
`NOT REACHABLE` rather than fixture success.
