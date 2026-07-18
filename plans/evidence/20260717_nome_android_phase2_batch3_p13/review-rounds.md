# P13 review rounds

Formal freeze status: **TWO CONSECUTIVE ZERO ISSUES — FINAL MANIFEST INPUT**

During implementation review, device instrumentation exposed and the source fixed:

1. a `FocusRequester` race when failure was composed in a lazy item;
2. failure/connecting operation panels being outside the composed viewport at 200% font;
3. an ambiguous semantics assertion that did not distinguish the live region from the primary
   button;
4. a debug-host dark status-bar surface mismatch; and
5. an untranslated owner-failure fixture reason; and
6. the initial Android fullscreen-modal stack mutation inheriting the `withBGApi` background
   dispatcher instead of `Dispatchers.Main`; and
7. retry closing P13 before replanning, which could make a typed planning failure or missing
   current user disappear instead of returning the retained identity to `Failure`.

Every affected build, API 28/API 35 focused suite, and renderer matrix was rerun after its fix.
These are implementation review iterations, not frozen formal review rounds.

A targeted final code review first requested changes for planning-failure and no-current-user retry
handling. The implementation was changed so a replan failure returns the existing route from
`Replanning` to `Failure`, and a follow-up review reported 0 issues with `APPROVE`. This review
occurred before a frozen review-input digest and therefore is not counted as either required
formal `ZERO ISSUES` round.

`REVIEW_INPUT_SHA256SUMS` is now frozen for formal review. It contains 137 entries, verifies
137/137, and has SHA-256
`25bf6f4575b910ff649e9683334f71a9f4bd409307ceadc0009498f1943eb233`.
Controlled evidence for raw command counts, successful in-flight lifecycle/context-switch traces,
TalkBack announcement/focus restoration, and the system-owned Intent Logcat boundary is closed in
this digest. Remaining Gate G work is limited to:

1. two consecutive independent reviews of this exact digest; and
2. generation and verification of final `SHA256SUMS` after those reviews.

## Invalidated formal-review attempts

Two attempts against the superseded
`f8a0cf5236e615317835e0b7f700363cb9f991f1dbdc1a525d967c3c7e852d6d` digest do not count:

1. In the first attempt, Round 2 found this file's former Gate G closure paragraph was stale and
   contradicted the manifest-listed evidence. Round 1 was interrupted. Because `review-rounds.md`
   is deliberately excluded from `REVIEW_INPUT_SHA256SUMS`, reconciling the closure record did not
   change that reviewed digest.
2. In the second attempt, Round 1 requested changes because `spec/state.md` still documented the
   old pair-shaped deep-link queue and `spec/api.md` omitted the captured `userId` from both P13
   typed delegates. Round 2 reported `ZERO ISSUES`, but it cannot count after a same-attempt
   finding.
3. Both specifications were corrected. Their hashes and therefore the review-input digest changed
   to `3de99a6ba7d3f9fc8fe45a071621ad686299eed2dac6e354f0328a8c310371e6`.

A third attempt against that intermediate digest was interrupted before either reviewer returned:
the local pre-freeze audit recomputed 552 changed-Markdown local links after the specification fix,
while `build-verification.md` still recorded the former 551 count. The evidence count was corrected
and the digest changed to
`081e935b2ef83c36e330b4520ec8c6e7da06c5e559dffc768e31d06bd81a2ae6`.

## Invalidated formal Round 1 — ZERO at superseded digest

- Reviewer: independent code/product/spec/evidence reviewer
  `/root/p13_gate_g_review_digest4_round1`.
- Reviewed digest:
  `081e935b2ef83c36e330b4520ec8c6e7da06c5e559dffc768e31d06bd81a2ae6`.
- Manifest verification: 137 entries, 137/137 `OK`.
- Result: `ZERO ISSUES`.
- Coverage included the actual delta from
  `fa95d96c7c24370c01b2707b9e2b0ad52a7a5112`, product/spec/source/test/evidence
  consistency, `ACTION_VIEW` provenance, branch policy, attempt user/host binding,
  command/lifecycle/retry/cancel behavior, `FIRST_USE` / `FILTERED_NO_RESULT` ownership,
  two-client and TalkBack evidence, release isolation, forbidden boundaries, and the corrected
  552-link audit.
- Independent local verification passed Android debug compilation, Android debug unit tests,
  Desktop tests, and `git diff --check`.

## Invalidated formal Round 2 — ZERO at superseded digest

- Reviewer: independent security/release/evidence reviewer
  `/root/p13_gate_g_review_digest4_round2`.
- Reviewed digest:
  `081e935b2ef83c36e330b4520ec8c6e7da06c5e559dffc768e31d06bd81a2ae6`.
- Manifest verification: 137 entries, 137/137 `OK`.
- Result: `ZERO ISSUES`.
- Coverage included bearer/URI/payload retention, command logging, failure sanitization, intent
  safety, user/profile/remote-host/attempt isolation, stale writes and cancellation, exported
  components, debug/test/release isolation, dependency delta, evidence integrity, and all
  forbidden source boundaries.
- The reviewer found no hardcoded credential, private key, bearer header, retained long
  connection URI, dependency delta, exported release evidence host, or forbidden path change.

Those two reports were consecutive and clean for `081e935b…`, but the checkpoint-only
`git diff --cached --check` then exposed Markdown trailing whitespace and EOF blank-line errors
that the earlier unstaged check could not see in untracked files. Eight manifest-listed Markdown
files were normalized without changing meaning. That byte change invalidates both reports, and the
generated 139-entry final manifest with file SHA-256
`2d50205c1fc5b40a767fd82d11fa90fe31e4a673afe18c4a63d49d3bc16ecccc`
was removed as superseded. Neither report counts for the current digest.

## Invalidated audit-unification attempt

An attempt against
`21317b85dee155a5fade98fabd165e6f6c7a4376dc91a4a7e64ca2c2a0e0b488`
does not count. Round 1 found that `build-verification.md` retained unlabeled earlier lines
reporting 28 changed Markdown files and 438 combined Markdown-line/colon-style references, plus a
later line reporting 35 files, 552 local links, and 34 explicit source anchors. Those different
metrics conflicted with this record's final rerun of 35 changed Markdown files, 552 non-web local
links, and 402 `#L` line-number anchors. Round 2 reported `ZERO ISSUES`, but it cannot count after
a same-attempt finding. The build record now has only the latter explicit current definition, with
zero failures. That evidence-byte fix changed the digest to the current value above.

The first review attempt against the current digest also does not count. Round 1 requested that
this excluded audit trail preserve the exact superseded metric wording above; Round 2 reported
`ZERO ISSUES`, but it cannot count after that finding. Expanding this excluded record does not
change `REVIEW_INPUT_SHA256SUMS`. Formal review restarts at Round 1 against the unchanged current
digest.

## Current pre-final-manifest audit

- Current review input: 137/137 `OK`.
- Changed Markdown: 35 files, 552 local links, 402 line-number anchors, 0 failures.
- Historical manifests: Phase 1 48/48, Phase 2 foundation 131/131, Batch 2 254/254.
- Staged text secret scan: 65 files, 0 private-key, bearer-header, service-token, or long
  connection-URI hits.
- Forbidden changed-path hits: 0.
- Whitespace normalization scope: eight Markdown files only; no product/source/test behavior
  changed.
- Audit-unification scope: one evidence Markdown file only; no product/source/test behavior
  changed.
- `apps/multiplatform/Screenshot_1784275771.png` remains an unknown user file and is excluded.

## Current formal Round 1 — ZERO ISSUES

- Reviewer: independent code/product/spec/test/evidence reviewer
  `/root/p13_gate_g_review_exact_record_round1`.
- Digest: `25bf6f4575b910ff649e9683334f71a9f4bd409307ceadc0009498f1943eb233`.
- Manifest: 137 entries, 137/137 `OK`.
- Result: `ZERO ISSUES`, `APPROVE`.
- Coverage included the actual base delta, `ACTION_VIEW` provenance, user/host attempt binding,
  lifecycle/command/cancel/retry behavior, `FIRST_USE` / `FILTERED_NO_RESULT` ownership,
  product/spec/source/test/evidence coherence, two-client/core/platform evidence, bilingual/theme/
  200%/48dp/TalkBack, release isolation, links/anchors, and forbidden boundaries.
- Current-byte validation passed Android debug unit tests, Desktop tests, Android instrumentation
  APK assembly, Android lint, pattern scan, and `git diff --cached --check`.

## Current formal Round 2 — ZERO ISSUES

- Reviewer: independent security/release/evidence reviewer
  `/root/p13_gate_g_review_exact_record_round2`.
- Digest: `25bf6f4575b910ff649e9683334f71a9f4bd409307ceadc0009498f1943eb233`.
- Manifest: 137 entries, 137/137 `OK`.
- Result: `ZERO ISSUES`.
- Coverage included secrets/bearers/URI/payload retention, command logging, sanitized failures,
  intent safety, user/profile/host/attempt isolation, stale writes and cancellation, debug/test/
  release and exported-component isolation, dependency delta, evidence integrity, cached audit,
  and all forbidden source boundaries.
- Live facts matched the record: 140 staged, 0 unstaged tracked, one excluded screenshot, 65
  staged text files, current 35/552/402 Markdown metric, and cached whitespace PASS.

These are the only two completed results after the excluded audit-history correction for the
current digest. No manifest-listed byte changed between their launches and results. They therefore
satisfy the required two consecutive independent same-digest `ZERO ISSUES` rounds.

The final `SHA256SUMS` is generated after this record. It covers all 137 reviewed inputs plus
`REVIEW_INPUT_SHA256SUMS` and this record, and deliberately excludes itself, the live completion
ledger, and `apps/multiplatform/Screenshot_1784275771.png`.
