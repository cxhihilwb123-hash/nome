# Nome Android UI Fix Batch 04 — Summary

Date: 2026-07-23 (Asia/Shanghai)

## Outcome

PASS for the scoped follow-up:

- The shared Home / Contacts / Settings navigation item height is now 64dp, with a 36dp maximum system-bottom reserve.
- The reserve uses live Compose/platform insets first and Android's `navigation_bar_height` resource only when every live source reports zero.
- Both Vivo devices expose the complete 64dp touch region; no item is clipped.
- Home, Contacts, and Settings were entered by real taps on both devices and showed the expected destination.
- Android system Back from Contacts now returns to Home instead of exiting the app.
- Settings-root Back, privacy-page system Back, and the visible one-hand Back control all return correctly.
- Android app-brand wording now uses Nome for lock, support, terminal-client, default call-server, About-title, and legacy theme labels.
- Both devices showed `Nome 锁定`; neither showed `SimpleX 锁定` on the privacy page.

## Final physical geometry

| Device | Density | Root-visible nav item bounds | Height | Gap to app root |
|---|---:|---|---:|---:|
| Vivo V2048A (`3106403166006XM`) | 420 dpi | Home `[42,2015][374,2183]` | 168 px = 64dp | 7 px |
| Vivo V2047A (`9590146717002S1`) | 480 dpi | Home `[48,2076][376,2268]` | 192 px = 64dp | 15 px |

The other two items have identical vertical bounds on each device.

## Build and test verdict

- Debug app and Android-test APKs: PASS.
- API 35 targeted Compose instrumentation: `OK (17 tests)`.
- JVM tests: 54 tests, 0 failures, 0 errors, 0 skipped.
- Release lint: 68 warnings, 0 Error/Fatal.
- `git diff --check`: PASS.

## Evidence limits

- Physical screenshots remain protected. Protection was not disabled or bypassed.
- The physical UI tree does not expose Compose's selected semantic flag. Exact selected-state behavior is covered by Compose tests; protected-device pixel color remains an evidence gap.
- This batch did not force device-wide language or dark-mode changes. English resources and brand mappings compile and are asserted by instrumentation; current-device Chinese UI was physically checked. Dark/English protected-device pixel evidence remains a gap.
- P22 was not reopened for a new protected-device pixel check in this follow-up. It still calls the same shared navigation component as Home and P23.

