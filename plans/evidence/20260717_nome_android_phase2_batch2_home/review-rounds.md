# Nome Android Phase 2 Batch 2 formal review record

Date: 2026-07-17 (Asia/Shanghai)

## Final frozen review input

- Manifest: `REVIEW_INPUT_SHA256SUMS`
- Manifest SHA-256: `c99b587e1bcb6231e28a2906185a7edba3b15acc3f46891d7f2005bc0e944165`
- Entries: `327/327 OK`
- `review-rounds.md` and final `SHA256SUMS` are intentionally excluded from the reviewed input;
  they are closure records created only after both formal rounds finished.

## Invalidated review inputs

The following inputs are retained only as audit history. None is accepted as the final review
result, and every finding caused a new freeze followed by a Round 1 restart.

| Digest | Entries | Invalidating finding |
|---|---:|---|
| `f3a23ceaea68ae492eb40a3ebabb4125da4dafc1727eb0a1d47e0ffa9bb3877d` | 293 | Unsupported TalkBack timing/history wording and missing archived real-core stop/start trace. |
| `4d9737383a673648d6ef4a83f93554bae65d3716621cca4e24d58881993ecce2` | 302 | Product/spec text prematurely claimed formal review completion. |
| `318e4dc7f976df2b32c258b82ac07ebb8e5b0c539cad17f6f691e65d4de69be2` | 302 | TalkBack PASS evidence was tied to the debug evidence Activity rather than production `MainActivity`. |
| `5464742799ee777262064ec6dfc2243d5829e8d591b644327731d860a51fe77a` | 327 | Stale command-table `spec/api.md` anchors after the typed chat-list API insertion. |
| `4a6c20acf31707416b1b58f048d6031d2eb28fbd9ee72a72772880d43ee3cd92` | 327 | Stale key-type and non-table `spec/api.md` anchors remained outside the first symbol checker. |

## Formal Round 1

- Reviewer task: `/root/batch2_review_round1_digestc99b`
- Frozen digest: `c99b587e1bcb6231e28a2906185a7edba3b15acc3f46891d7f2005bc0e944165`
- Manifest verification: `327/327 OK`
- Result: **ZERO ISSUES**
- Recommendation: **APPROVE**
- Independent checks included:
  - correctness/state truth and failure-vs-empty;
  - generation/cancellation and navigation guards;
  - privacy/security wording;
  - Android/Desktop/release isolation;
  - bilingual, light/dark, 200%, 48dp and production-route TalkBack;
  - `156/156` named `SimpleXAPI` anchors and `660/660` local source anchors;
  - evidence-claim boundaries and renderer-only deferred states.

## Formal Round 2

- Reviewer task: `/root/batch2_review_round2_digestc99b`
- Frozen digest: `c99b587e1bcb6231e28a2906185a7edba3b15acc3f46891d7f2005bc0e944165`
- Manifest verification: `327/327 OK`
- Result: **ZERO ISSUES**
- Recommendation: **APPROVE**
- The second reviewer independently recomputed the manifest, anchor counts, production TalkBack
  boundary, release/debug separation, real-core/core-cycle/upgrade evidence and deferred-state
  boundary without relying on the Round 1 verdict.

## Same-digest conclusion

Two consecutive, independent, complete reviews on the exact same frozen digest returned
**ZERO ISSUES**. The Stage 8 review gate is therefore satisfied.

## Pre-final-manifest Git and scope audit

Audit time: 2026-07-17 18:31:44 CST

- HEAD: `59fce95d3cd08897b4ef742447b785cf2e56c7ce`
- Exact tag: `v6.5.6`
- Branch: `codex/nome-android-v656`
- Origin: `https://github.com/simplex-chat/simplex-chat.git`
- Staged paths: `0`
- Dirty paths before the two closure artifacts: `516` (`29` tracked modifications plus `487`
  untracked files). Adding this review record and final `SHA256SUMS` is expected to make the final
  live count `518`; the final live audit is run after the checksum file is written.
- Forbidden-path scan found no Haskell/native-core, `Core.kt`, iOS, protocol, message-state,
  schema or migration change. The only name match was
  `common/src/commonMain/kotlin/chat/simplex/common/views/database/DatabaseView.kt`; its diff is a
  UI refresh call-site migration to the typed chat-list result with explicit cancellation rethrow,
  not a database format, schema, archive or migration change.
- `git diff --check`: PASS.
- Commit: none.
- Push: none.

## Deferred boundary

- `FIRST_USE`: renderer-only deferred; not claimed production-reachable or real-core PASS.
- `FILTERED_NO_RESULT`: renderer-only deferred; no filter/search pipeline was added and no
  production PASS is claimed.
