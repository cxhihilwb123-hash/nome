# P01 Gate B plan review

Date: 2026-07-18 (Asia/Shanghai)
Status: **SUPERSEDED BY USER EXECUTION-CONTRACT AMENDMENT**

> Historical review summary only. The results below remain an exact record of the reviewed
> pre-amendment 728-line plan digest. They no longer impose repeated plan review, exhaustive
> per-state evidence, standalone two-review Gate G, or a P01-only checkpoint. Risk findings remain
> useful for the high-risk database/key/backup/migration verification lane. Current authority is
> the sole completion ledger.

## Frozen input

- Plan: `plans/20260718_02.md`
- Lines: 728
- SHA-256:
  `1599d958dd24322234c58f39ba87584d1c12f0c485c49e111a56063bf741dca8`
- Manifest:
  `plans/evidence/20260718_nome_android_batch1a_p01/PLAN_REVIEW_INPUT_SHA256SUMS`
- Manifest verification: `1/1 OK`.

## Invalidated attempts

The 542-line digest
`e6d4378ee94187c5dad53339817214a2145d652e8ba70705a3abeb769a85f8a4` passed its manifest
check but Round 1 returned `REJECT` with three actionable gaps:

1. an unreadable initial random Android Keystore database key can throw before
   `chatDbStatus` is assigned, leaving a null-status dead end that the plan incorrectly rendered as
   opening;
2. the plan simultaneously promised mechanical copy-only backup preservation and a fresh-open
   action, while reused legacy helpers could expose raw SQL/JSON/stack-trace alerts;
3. the plan did not explicitly preserve the existing `!unauthorized` guard around the database
   error/passphrase renderer.

The revised plan adds a no-secret pre-result key-read state without changing return/throw or key
behavior, chooses an explicit two-step Android copy-terminal then user-activated open sequence,
separates sanitized Android operations from legacy alert wrappers, freezes the auth guard, and
adds the corresponding fixtures/tests. Those byte changes invalidate the earlier attempt.

Before restarting Round 1, the unreviewed 615-line candidate
`a14a154ef63b1a65817006f6e6843d42056d19fb1693f446a73a3abd3a6686e1` was further hardened:
`allowSensitiveContent` now prevents the null-status key-failure route from escaping the auth
guard, and the fixture plan now names exact official v6.5.4 source/APK/native hashes, package,
signer, migration, and same-AVD overlay steps. No reviewer result is attributed to that intermediate
candidate.

The 665-line digest
`0d919fe12b1697de56831761dc093e0bb830ba5365d748ec35574ff91ae2a170` passed its manifest
check but the restarted Round 1 returned `REJECT` for one remaining key-read branch. A missing
Android Keystore alias returns null rather than throwing; with an initial random database key it can
reach terminal `ErrorNotADatabase`, and the plan did not force that fact to override
`AlternateKeyRequired`. The current plan now publishes missing-alias and decrypt-failure classes
before the existing return/throw, carries the initial-random Boolean, gives the random-key state
priority over both null-status opening and terminal alternate-key entry, forbids manual recovery in
both shapes, and requires separate real fixtures. Those byte changes invalidate that digest.

The 687-line digest
`862aee5665687bd679514d49ea4913305e0d5a19777c58fd1f8233d5caf4ff1c` passed its manifest
check but the next Round 1 returned `REQUEST CHANGES` for two action-contract problems:

1. the Android sanitized route bypasses the legacy wrapper, so the plan also has to preserve the
   existing exactly-once `androidChatStartedAfterBeingOff()` success hook that cancels the
   passphrase notification and restores SERVICE/PERIODIC behavior;
2. manual stored-key missing-alias/decrypt failures both return null and proceed to terminal
   `ErrorNotADatabase`; only the initial-random decrypt-throw branch can stay null-status.

The current plan adds the success hook and its mode-specific proof, and removes the impossible
manual null-status variant. Those byte changes invalidate that digest.

The 703-line digest
`428e6c646d9e7fdba303dd7e926250c406f19147b7795550aa135d02bdc01068` passed its manifest
check but the next Round 1 found two remaining abstraction conflicts:

1. it simultaneously kept the existing `App.kt` root conditions and claimed Android could render
   opening before the one-second condition selected that branch;
2. it required a manual stored-key “open without overwrite” action but listed only current-policy
   open and save-before-open helpers.

The current plan preserves the shared pre-delay blank/system splash on both platforms and starts
P01 only after the existing branch is selected. It also explicitly extracts an entered-key
open-once operation that does not write Keystore/preferences, separate from the existing save-and-
open action. Those byte changes invalidate that digest.

The resulting 714-line candidate
`2e2a8bd915c4bbcc13ba761fd990db57ffecb8aa86611bfa93a308e9791adea8` was assigned for
review, but the assigned reviewer added the missing `facts` argument to the
`PlatformDatabaseRootRoute` call contract and updated the manifest/evidence records during that
review. The change is consistent with the already-declared facts carrier, but a review that changes
its own input cannot count as an independent same-digest result. The 714-line candidate and the
reviewer's self-approval are invalidated. That change produced the later 715-line candidate.

## Historical 715-line rounds

`ZERO ISSUES / APPROVE`.

The independent code/product/source reviewer verified 715 lines, the exact frozen SHA-256, and
manifest `1/1 OK`. It found the plan consistent with the authoritative root/auth timing, current
action and matched-pair backup semantics, Android missing-alias versus decrypt-throw split, the
exactly-once success hook, real v6.5.4 producer, Desktop fallback, release isolation, and evidence
gates. The reviewer changed no file. That result is now historical because Round 2 found one
machine-derived signer SHA incorrectly presented as a source-authoritative fixture requirement.

Round 2 rejected the 715-line digest
`7a6ce7d8c5c9d3f3937b4de50c4f9caedc73fbd9c469dba4910587803ebfcd38`.
The Android build does not declare a fixed debug signing configuration, so the observed local
certificate cannot be a portable normative constant. The current plan records the recovered hash
only as non-normative evidence, requires freshly computed old/current signer equality, and aborts
the fixture on mismatch without changing signing. Those bytes invalidate both prior rounds.

The 719-line digest
`237903b2f72983c318fb12aa6aab510be70aac72f56994cbe7eb0983ec3f4ab1`
was rejected in Round 1 for two execution gaps:

1. the temporary v6.5.4 archive did not receive the current ignored `local.properties`, so its
   application ID could lose `.nome.dev` and no longer overlay the current package;
2. the batch verification matrix omitted the authority plan's API 33 system smoke.

The current plan now copies and hashes the local file only inside the disposable old-source tree,
requires the parsed `.nome.dev` suffix or aborts, and adds exact-serial API 33
root/auth/lifecycle/notification/background/system-back smoke. Those bytes invalidate the
719-line review.

## Current-digest Round 1

`ZERO ISSUES / APPROVE`.

The independent source/authority reviewer verified exactly 728 lines, the current SHA-256, and the
manifest. It confirmed both prior gaps are closed by the temporary-only `.nome.dev` local build
input contract and API 33 exact-serial system smoke, and found no new issue. It changed no file.

## Current-digest Round 2

`ZERO ISSUES / APPROVE`.

The independent security/release/evidence reviewer verified exactly 728 lines, the same SHA-256,
and manifest `1/1 OK`. It confirmed the machine-local signer contract, temporary `.nome.dev` input,
API 33 smoke, Keystore/error/recovery truth, root/auth timing, Core ownership boundary, and success
hook are all actionable and consistent. It found no new issue and changed no file.

No plan byte changed between the two current-digest reviews.
