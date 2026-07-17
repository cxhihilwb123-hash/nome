# Nome Android Phase 2 Batch 2 evidence

Date: 2026-07-17 (Asia/Shanghai)  
Plan: `plans/20260717_05.md`  
Baseline: SimpleX Chat `v6.5.6` / `59fce95d3cd08897b4ef742447b785cf2e56c7ce`

This evidence root covers only the authorized production shell and bounded P07/P08
production-home slice. It does not claim completion of the full Nome Android application or of
any page outside this batch.

## Result boundary

- Build, unit, lint, debug/androidTest/release assembly, Desktop fallback, API 28/API 35 device,
  real-core, same-package non-empty upgrade, network/core/generation/cancellation, screenshot,
  accessibility, TalkBack, and release-isolation execution gates passed.
- A true-empty claim requires a typed same-generation successful `ApiChats` result. Failure is
  unavailable, and a generation mismatch hides the prior identity's rows.
- Device offline is only Android platform connectivity truth; it does not claim relay, server,
  global-service, or delivery failure.
- Same-generation cached rows remain visible during refresh and may remain readable while the
  core is stopped. Stopped rows expose no navigation action.
- `FIRST_USE` and `FILTERED_NO_RESULT` are deterministic renderer contracts and screenshot states,
  not production-route/real-core PASS states. Onboarding consumes no-user before home, and this
  bounded home has no filter producer.
- Debug fixtures are visibly labelled “production renderer / not live core” and are excluded from
  the release APK. Live-core proof is stored separately under `reports/`.
- Formal completion additionally requires `review-rounds.md` to record two independent
  consecutive ZERO ISSUES reviews of the same `REVIEW_INPUT_SHA256SUMS` digest and requires the
  final `SHA256SUMS` to verify in full.

## Evidence map

- `build-verification.txt` — Gradle, JUnit, device-test and APK results.
- `static-verification.txt` — diff/XML/resource/seam/source-link and old-evidence integrity.
- `state-truth-table.md` — adapter and production reachability truth.
- `api28-real-core.txt` — API 28 device, core and connectivity proof.
- `api35-populated-real-core.txt` — API 35 populated P07 and refresh/network proof.
- `api35-upgrade.txt` — same-package non-empty upgrade and exact file checkpoints.
- `network-core-generation-cancellation.txt` — independent truth-axis verification and limits.
- `accessibility.txt` — automated semantics/48dp/200% and production-route TalkBack traversal;
  raw production proof is under `reports/accessibility/production-talkback/`.
- `release-isolation.txt` — release/debug package and harness separation.
- `screenshot-matrix.md` — 80/80 capture matrix and renderer-only labels.
- `native-comparison.md` — P07/P08 comparison and contact-sheet review.
- `threat-model-delta.md` — bounded risk/control/proof delta.
- `native-screenshots/` — 80 unique API 35 PNGs.
- `comparisons/` — nine comparison/contact-sheet artifacts.
- `reports/` — raw Gradle, device, upgrade, explicit real-core stop/start, accessibility and
  release records.
- `SCREENSHOT_SHA256SUMS` — screenshot-only manifest.
- `REVIEW_INPUT_SHA256SUMS` — frozen source/document/evidence review input.
- `review-rounds.md` — formal review results; intentionally excluded from review input.
- `SHA256SUMS` — final evidence-root manifest; intentionally excludes itself.

## Historical evidence preservation

The immutable Foundation evidence manifest, Foundation screenshot manifest, and Phase 1 evidence
manifest verify 131/131, 96/96, and 48/48. The Foundation `REVIEW_INPUT_SHA256SUMS` file itself is
unchanged, but rechecking its historical source payload after authorized Batch 2 work yields
213/223 because ten listed source/document inputs intentionally changed in this later batch. That
is not reported as old evidence drift; the exact ten paths are recorded in
`static-verification.txt`.

No reset, clean, stash, checkout overwrite, commit, or push was performed.
