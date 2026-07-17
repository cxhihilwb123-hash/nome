# Nome Android Phase 2 foundation evidence

Date: 2026-07-17 (Asia/Shanghai)  
Branch: `codex/nome-android-v656`  
Official baseline/HEAD: `v6.5.6` / `59fce95d3cd08897b4ef742447b785cf2e56c7ce`

## Outcome

This evidence documents the Android-only Phase 2 **design foundation** batch; closure is determined by the frozen two-round review condition below:

- GAP-08 through GAP-17 use the recorded recommended routes;
- GAP-18 is implemented as minSdk 28; API 26–27 are unsupported;
- Dark Token v1 is represented by P02/P07/P17/P21/P23 with unchanged IA, semantics, dimensions, and state truth;
- production source is limited to Android-only tokens, Material 2 theme adapter, surface/button/state-panel primitives, and accessibility modifiers;
- deterministic bilingual/light-dark fixture, Preview, and screenshot code is debug-only;
- no production page route, presenter, model/state adapter, native/core, database/protocol, message-state-machine, Desktop, or iOS implementation is added.

The batch passed unit, lint, debug assembly, packaging, Compose instrumentation, API 28/API 35 real-core smoke, same-package non-empty upgrade, 48dp, 200% font, TalkBack-semantics/live-region, contrast, native screenshot, and source-set-isolation gates. Final review status is authoritative only when `REVIEW_INPUT_SHA256SUMS` verifies and `review-rounds.md` records both consecutive rounds against that same frozen digest.

## Evidence map

| Evidence | Purpose |
|---|---|
| `build-verification.txt` | Gradle tasks, test totals, manifests, APK badging, and source-set isolation. |
| `api28-device-core.txt` | Fresh API 28 install/cold-start and real official server/receiver smoke. |
| `api28-raw-excerpts.txt` | Retained API 28 command/output excerpts plus explicit capture limits. |
| `api35-nonempty-upgrade-core.txt` | Same-package non-empty v6.5.6 upgrade and real-core restart boundary. |
| `api35-raw-excerpts.txt` | Retained API 35 sizes/hashes/start/Logcat/UI-tree excerpts and limits. |
| `contrast-report.md` | Measured critical light/dark token pairs. |
| `screenshot-matrix.md` | Exact 96-file bilingual/theme/state/200% matrix. |
| `native-comparison.md` | Approved light-reference side-by-side/overlay and dark/state/200% visual review. |
| `threat-model-delta.md` | New presentation/debug surface, controls, and residual risk. |
| `review-context.txt` | Live branch/HEAD/tag/remote, preservation boundary, and digest scope. |
| `reports/` | JUnit XML, lint XML, merged debug/release manifests, and arm64 APK badging. |
| `native-screenshots/` | 96 API 35 PNGs captured through `UiAutomation.takeScreenshot()`. |
| `comparisons/` | Five side-by-side images, five 50/50 overlays, and four contact sheets. |
| `SCREENSHOT_SHA256SUMS` | Per-file digest for all 96 native screenshots. |
| `REVIEW_INPUT_SHA256SUMS` | Created only after substantive payload freeze; checked by both final reviewers. |
| `review-rounds.md` | Created after two consecutive final attestations; excluded from its own reviewed payload. |
| `SHA256SUMS` | Created last as the evidence-directory manifest, excluding itself. |

## Important claim boundaries

- The 96 screenshots are deterministic **debug references**, visibly labelled “非实时 CORE / not live CORE”. They validate tokens, layout, copy stress, all seven foundation panel states at 100%/200%, and screenshot plumbing; they do not prove live delivery, relay health, authentication, permission, migration, or production routing.
- Real-core evidence is separate: API 28 and API 35 both started the unchanged v6.5.6 server/receiver. No screenshot fixture calls or simulates core commands.
- The API 35 immediate checkpoint proves exact preservation of the non-empty chat database and preferences. `files_agent.db` is runtime-mutable and is not claimed byte-identical.
- The 200% images are first-frame native captures of scrollable layouts. They prove reflow/no horizontal clipping for the captured frame; they are not full-page scroll captures.
- Light-reference overlays are comparison aids, not pixel-parity claims. This batch implements a reusable foundation, not the five production pages.
- Phase 0/1 evidence under `../20260716_nome_android_phase1/` remains read-only and is not regenerated or included in this directory's hashes.
