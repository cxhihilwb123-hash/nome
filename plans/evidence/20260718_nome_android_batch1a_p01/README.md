# Nome Android Batch 1A — P01 evidence

Date: 2026-07-18 (Asia/Shanghai)
Scope: Android-only database startup, migration, and bounded recovery
Baseline checkpoint: `b714f78efc7b3cc8c4e252d6351c4b629f60e277`
Status: **Gate A and Gate B complete; Gate C integrated and under verification**

This directory is the immutable-per-artifact evidence root for P01. It does not alter or replace
Phase 1, Phase 2 foundation, Batch 2, or P13 evidence.

## Current facts

- Branch: `codex/nome-android-v656`
- HEAD: `b714f78efc7b3cc8c4e252d6351c4b629f60e277`
- Origin: `https://github.com/simplex-chat/simplex-chat.git`; no mutation
- Tag ancestry: `v6.5.6` / `59fce95d3cd08897b4ef742447b785cf2e56c7ce` is an ancestor
- Historical manifests: Phase 1 `48/48`, foundation `131/131`, Batch 2 `254/254`, P13 `139/139`
- P13 manifest file SHA-256:
  `211d2d9c7b69808dd3bf8f1ceaf7dff5dd0298ab09fb4ed9c0af9752a8549a37`
- Toolchain: JBR 21.0.10, Gradle 8.12, SDK 26/33/35, ADB 37.0.0
- AVDs: API 28, API 33, API 33 clean RC, API 35, API 35 clean RC
- Online at Gate A: API 35 `emulator-5554`, API 28 `emulator-5556`
- Free disk: approximately 140 GiB
- Excluded unknown path: `apps/multiplatform/Screenshot_1784275771.png`

## Authority

- Total plan: `plans/20260718_01.md`
- Batch plan: `plans/20260718_02.md`
- Completion ledger: `plans/evidence/20260718_nome_android_completion/README.md`
- Frozen P01 visual:
  `/Users/forkman03/project/nome/design/design/product/full-page-effects/android/P01-launch-database.png`
  at 470 × 936, SHA-256
  `d9b6524b4db29e6dd8b3dd464564c1fc665700c43d6f49a8eab3ffe860685e6c`

## Gate A conclusions

- The official root/data/core chain is present and must stay unchanged.
- Current P01 production presentation is legacy/generic; no P01 adapter, renderer, resources, unit
  tests, instrumentation, or debug evidence host exists.
- The retained API 35 non-empty snapshot is valid same-package upgrade input but is not an
  old-schema migration producer.
- No old-schema seed is currently present, but the read-only history audit identified official
  `v6.5.4` / `e92afb68d52bff0d2a035b3594d16badb3f58649` as the minimal authentic producer for
  chat migration `20260516_supporter_badges`. Gate E must build it in a temporary clone with
  hash-verified v6.5.4 native libraries; current v6.5.6 libraries would fabricate the fixture.
- API 28 fresh-DB proof must use a new disposable AVD.
- Wrong-key, corruption, and `.bak` tests must use separate disposable API 35 clones.
- `FIRST_USE` remains official onboarding/root owned.
- `FILTERED_NO_RESULT` remains deferred until P09 has a real producer.

## Protected boundary

No Haskell/native, `Core.kt`, protocol/API contract, database semantics/schema/migration order,
archive format, iOS, service/worker gating, historical evidence, or unknown user file may change.
No push, publication, store mutation, or production signing is authorized.

## Evidence index

- `remaining-gates.md` — current Gate B–G work and exact unresolved verification gap.
- `PLAN_REVIEW_INPUT_SHA256SUMS` — frozen Gate B plan bytes once generated.
- `plan-review.md` — review history once the plan digest is frozen.

Build/device/fixture/visual/accessibility/release/review files are added only after their checks
actually run. Debug renderer output will remain separate from production/core evidence. Current
focused reducer/gate, root-boundary, and Desktop fallback tests pass, Android debug compilation
passes, and the Android instrumentation APK assembles; independent source/security review and the
full Gate C/D matrix remain open.

## Unique next action

Finish the independent Gate C source/security review, correct every actionable finding, then run
the full Android unit/Desktop/debug/release compilation set before exact-serial instrumentation.
