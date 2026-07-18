# P13 production reachability

## Static route

The retained production path is:

```text
Android ACTION_VIEW
  -> MainActivity stores the URI in ChatModel.appOpenUrl
  -> unchanged onboarding-complete and chatRunning gate
  -> connectIfOpenedViaUri
  -> planAndConnect(ExternalActionView)
  -> typed apiConnectPlanResult
  -> one of seven eligible identity-choice branches
  -> Android Nome P13 modal
  -> typed apiConnectResult
  -> only a real PendingContactConnection updates chatsContext
```

`ConnectionPreviewEntryPolicy` defaults to `Legacy`. A source scan found the only production
`ExternalActionView` opt-in at `connectIfOpenedViaUri`; the retry occurrence is an internal P13
handoff. The six other caller surfaces retain the default. The normalized branch table is
exhaustive at 21 branches, exactly seven of which are eligible.

Desktop has one actual implementation and returns `false`, preserving the legacy alert.

## Device result

Two isolated debug clients were brought through the unchanged official onboarding and
`chatRunning` gates. API 28 generated fresh invitations through the existing controller; API 35
received them as external `ACTION_VIEW` intents. No debug fixture route was used to present P13.

### Current-profile attempt

1. API 35 presented P13 `Ready` with local profile `Nomenclature35` selected.
2. One Continue activation presented `Connecting`.
3. The real core response presented `Pending`; the operation panel stated that the request had
   been sent.
4. API 28 came online and observed peer `Nomenclature35`.
5. Dismissing the pending page returned API 35 to the home route, where peer `v` was present.

### Per-connection incognito attempt

1. API 35 used a separate local profile, `IncogTarget35`, and received a second fresh invitation.
2. TalkBack focus and activation selected `New incognito identity`; the semantics tree changed
   from current checked to incognito checked.
3. TalkBack activation of Continue presented `Connecting`, followed by the real `Pending` result.
4. API 28 observed peer `SuperlativeSaver`, which differs from both `IncogTarget35` and
   `Nomenclature35`.
5. Dismissing the pending page returned API 35 to home with peer `v`.

This closes production invitation reachability, real pending response, and peer-observed
current/incognito identity behavior. The later host-only observer matrix establishes the raw
command counts.

### Contact-address continuation — 2026-07-18

API 28 created fresh contact addresses through the existing core for isolated profiles. API 35
received each address through the same external `ACTION_VIEW` route:

1. The first address presented P13 `Ready` with the contact-address-specific explanation and
   current profile `IncogTarget35`.
2. Cancel before submit returned to home. API 35 had no new pending row and the API 28 address
   owner received no contact request.
3. A second fresh address again presented the contact-address `Ready` page.
4. Eight rapid Continue taps produced one sanitized real network `Failure` panel saying that the
   request was not sent. The address owner still received no request.
5. Retry visibly entered `Replanning`, asked the real core for a fresh plan, and returned to the
   contact-address `Ready` page with the selected identity retained.
6. Rotating and backgrounding from `Ready` safely cleaned up the preview and returned to home;
   neither client gained a pending/request row.

This closes controlled contact-address ingress, cancel-before-submit, a production network
failure, and a production retry/replan. The later host-only observer established one plan and one
connect despite eight activations. A later retained-state implementation also survived actual
rotation plus Home/background/resume in flight and ended in real `Pending`, with
`PLAN=1 / CONNECT=1`.

### Context, cancellation, and existing-contact boundary — 2026-07-18

- Switching the active user after `Ready` produced `ContextChanged`, `PLAN=1 / CONNECT=0`, then
  the controlled original user was restored.
- A real invitation cancellation attempt produced `PLAN=1 / CONNECT=1`; the actual retained owner
  Job had one active child before cancellation, was inactive afterward, and reported
  `kotlinx.coroutines.JobCancellationException`. The final UI tree had no preview or terminal
  result.
- After a controlled address request was accepted by real core auto-accept, reopening the same
  address produced `PLAN=1 / CONNECT=0` and the official existing-contact dialog. This proves
  `ContactAlreadyExists` is preempted by the authoritative planner and is not production-reachable
  inside P13 on v6.5.6.
- The source fixture's original address settings were verified as default before mutation and
  restored by a paired production-API harness afterward.

### Owner-proof boundary — 2026-07-18

P13 owner verified/failed is production `NOT REACHABLE` on the current route. Android external
`ACTION_VIEW` is the sole P13 producer and its `AppOpenUrl`/`connectIfOpenedViaUri` data path has
no `LinkOwnerSig`; the typed plan therefore receives `null`. The production chat-item surfaces
that do carry `MCChat.ownerSig` invoke the same planner with its default `Legacy` presentation.
The core verifier maps only a supplied signature to verified/failed and returns no owner fact for
`null`. See `owner-proof-boundary.md` for the bounded source proof. Renderer rows remain valid
presentation coverage and make no real-core owner-proof claim.

### Transport and leak boundary

The default proxy route produced transient broker-network timeouts across several preset servers.
The isolated profiles were switched to one known SMP server with direct SMP mode so the P13 route
could be evaluated independently of that infrastructure condition.

No invitation value is written to this evidence. A later clean capture proved zero exact
full-bearer hits in the app PID, app-owned `SIMPLEX` tag, UI tree, and all Android Logcat buffers.
Four OS-owned intent-dispatch descriptors retained only the public `https://simplex.chat/` root;
the first route character and every longer tested prefix had zero hits. This closes app-owned
bearer isolation and records the Android platform residual without misattributing it to Nome.
See `logcat-boundary.md`.

Ready and Failure semantics trees each had zero bearer-pattern hits. The known bearer cache files,
UI dumps, lifecycle receiver, and one-shot/TalkBack/TTS androidTest sources were removed before
Gate G. Final release, debug, and standard test artifacts have zero temporary harness/receiver/TTS
strings.
