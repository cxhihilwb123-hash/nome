# P13 remaining acceptance gates

No P13 product or release blocker remains. Cleanup, release isolation, automated verification,
device restoration, and all product acceptance evidence are closed.

The non-product Gate G closure procedure is:

1. freeze one `REVIEW_INPUT_SHA256SUMS`;
2. obtain two consecutive independent `ZERO ISSUES` reviews of that exact digest;
3. generate and verify the final `SHA256SUMS`, create the local P13 checkpoint, and do not push.

Its authoritative completion status is recorded in `review-rounds.md`; this file is intentionally
stable across formal review.

Closed across the controlled sessions: invitation and contact-address `ACTION_VIEW` reachability,
two controlled clients, real invitation `Ready -> Connecting -> Pending`, peer-observed current
and generated per-connection identities, contact-address cancel-before-submit, real address
network Failure and retry/replan, successful in-flight rotation/background, active-user context
invalidation, exact per-attempt command counts, real owner-Job cancellation with
`JobCancellationException`, and a bounded production proof that the planner preempts
`ContactAlreadyExists` before P13. Production TalkBack selection/activation is also closed. No
fake link, synthetic timeout, or debug `Pending` state was promoted as real-core evidence.
The P13 owner verified/failed rows are closed as production `NOT REACHABLE`: external
`ACTION_VIEW` cannot carry `LinkOwnerSig`, while signed in-chat links retain the legacy
presentation policy.
The clean Logcat capture is also closed: app PID/tag, UI tree, and full system buffers had zero
complete-bearer hits. Four OS-owned dispatch records retained only the public
`https://simplex.chat/` root and no route or bearer bytes. Production TalkBack traversal,
selection, accessibility activation, exact allowlisted Failure announcement, and post-dismiss
focus restoration are closed; see `talkback-transcript.md`.
