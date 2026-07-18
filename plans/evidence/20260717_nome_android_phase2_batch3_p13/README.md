# Nome Android Phase 2 Batch 3 — P13 evidence

Date: 2026-07-17 (Asia/Shanghai)
Scope: Android-only P13 external `ACTION_VIEW` connection preview
Baseline checkpoint: `fa95d96c7c24370c01b2707b9e2b0ad52a7a5112`
Status: **implementation and Gate F verification recorded; authoritative Gate G status is in
`review-rounds.md`**

This directory records only checks that were actually run against the local uncommitted P13
implementation. Debug renderer screenshots are kept separate from production/real-core claims.
No raw connection link, owner signature, or real personal identity is included.

Open completion gates are tracked in `remaining-gates.md`. A controlled two-client invitation
cycle, controlled contact-address ingress/failure/retry, and production TalkBack
selection/activation/exact announcement/focus restoration are now recorded. Exact
command/in-flight lifecycle traces are recorded, and the owner-proof rows have a bounded
production-unreachable proof. Renderer evidence does not substitute for the real-core claims.
Cleanup and release isolation are closed. The frozen digest, formal review results, and checkpoint
state are recorded by the Gate G closure files rather than inferred from this README.

## Evidence index

- `build-verification.md` — local Gradle, lint, APK, resource, and historical-manifest checks.
- `production-reachability.md` — static production route plus the controlled API 28/API 35
  two-client invitation result.
- `owner-proof-boundary.md` — bounded proof that signed owner facts cannot enter the P13
  production route on v6.5.6.
- `logcat-boundary.md` — clean app-owned zero-bearer capture and the redacted OS-owned
  `ACTION_VIEW` descriptor boundary.
- `state-truth-table.md` — typed plan/connect and renderer truth boundaries.
- `device-results.md` — API 28/API 35 device facts and instrumentation results.
- `screenshot-matrix.md` — 72-file renderer matrix and visual-inspection scope.
- `native-comparison.md` — directional comparison against the approved P13 reference.
- `accessibility.md` — automated checks plus real TalkBack announcement/focus closure.
- `talkback-transcript.md` — exact allowlisted production TalkBack transcript and focus-restoration
  proof.
- `visual-verdict.json` — structured 72-image final recapture verdict.
- `release-isolation.md` — debug-host presence and release absence.
- `threat-model-delta.md` — security controls and proof level.
- `remaining-gates.md` — acceptance criteria that current controlled fixtures cannot close.
- `review-rounds.md` — why no formal `ZERO ISSUES` freeze is claimed yet.

## Boundary

The source implementation is local and uncommitted. No Batch 3 path is staged, committed, or
pushed by this work. `FIRST_USE` and `FILTERED_NO_RESULT` production ownership is unchanged.
