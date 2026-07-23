# Nome Android Batch 03 Design QA

Date: 2026-07-23

## Reference and implementation

- Approved reference: `/Users/forkman03/.codex/generated_images/019f8cd4-670e-7ed2-a38e-bc1e5f1dbb11/call_NNeINX4nUFQSXhoBbME4GyUQ.png`
- Reference state: Home, Contacts, and custom list `111`, light theme.
- Native implementation evidence:
  - `plans/evidence/20260723_nome_android_ui_fix_batch_03/rendered-screens/implementation-en-light-contact-sheet.png`
  - `plans/evidence/20260723_nome_android_ui_fix_batch_03/rendered-screens/implementation-zh-CN-dark-contact-sheet.png`
  - Twelve individual native screenshots in the same directory cover Home, Contacts, and custom list `111` in English/Chinese and light/dark themes.
- Side-by-side comparison:
  - `plans/evidence/20260723_nome_android_ui_fix_batch_03/rendered-screens/design-comparison-source-vs-implementation-en-light.png`

The source is a 1536x1024 three-screen concept. Each implementation screenshot is a native 1080x2400 Android render on API 35 at 420 dpi. The comparison normalizes the two three-screen groups to the same 1024 px height. The individual 1080x2400 files remain available for full-resolution inspection.

## Required fidelity surfaces

| Surface | Result | Evidence |
| --- | --- | --- |
| Header brand and profile action | Passed | Nome logo uses the authoritative transparent 700x285 asset, keeps its aspect ratio, and uses the approved light-on-dark variant in dark theme. |
| Page hierarchy | Passed | Home, Contacts, and custom-list screens use the approved title, subtitle, search, section label, content card, FAB, and bottom-navigation hierarchy. |
| Search field | Passed | Compact outlined search control, rounded corners, icon, hint, spacing, and theme colors are consistent across all three surfaces. |
| List shelf | Passed | One contained shelf shows All, user lists, and Add/Manage list actions; the selected list uses the shared green pill state. |
| Conversation/contact card | Passed | Rows are grouped into a single elevated container with inset dividers, compact metadata, and consistent unread badges. |
| Bottom navigation | Passed | Home, Contacts, and Settings share the same 76dp component, icon/text styling, selected green state, and top indicator. |
| Light/dark themes | Passed | Light and dark sheets have readable contrast; the logo has no white plate, fringe, stretching, or visible raster blur at the native render size. |
| English/Chinese | Passed | Twelve renders cover both locales with no visible clipping, collision, or truncated navigation label. |

## Comparison history

1. Initial dark-theme comparison exposed a P2 mismatch: the light-theme logo resource could be selected by Compose even when Android night qualifiers were not active. `NomeBrandLockup` now selects the existing approved dark asset explicitly from `MaterialTheme.colors.isLight`. The final Chinese dark contact sheet proves the corrected white-and-green transparent logo.
2. Initial light comparison showed the header brand too small relative to the approved composition. The rendered lockup width was raised from 84dp to 104dp while preserving the 700:285 ratio. The final English light comparison shows the corrected scale.
3. Physical accessibility inspection exposed undersized bottom-navigation semantics. The final modifier/layout chain exposes the full 76dp item as the physical semantics box; the Compose regression suite verifies minimum targets and selected states.
4. Physical navigation inspection exposed an Android Back trap in the list editor. An Android `BackHandler` now closes the editor/list overlays through their existing close callbacks.
5. The two Vivo window managers report bottom insets differently. The shared navigation now combines Compose and legacy root-window inset reports, with a targeted fallback only when the OEM reports no bottom inset. Final physical bounds are 200px at 420dpi (76.19dp) on V2048A and 228px at 480dpi (76.0dp) on V2047A.

## Accepted differences and evidence boundaries

- The reference uses conceptual Chinese contact names and presence labels. The native renderer deliberately uses non-sensitive fixture chat/contact rows and the production row component; changing real contact semantics was outside this pure UI/navigation batch.
- The thin `DEBUG PRODUCTION RENDERER` strip exists only in the debug evidence activity and is not part of the production route.
- The comparison assesses hierarchy, proportions, spacing, state styling, color, and readability. It does not claim literal pixel identity between a concept image and Android system-rendered typography.
- Screenshot protection remains enabled on real accounts. Pixel evidence therefore comes from the non-sensitive production-component renderer; true click destinations and return behavior are checked separately on both Vivo devices using fixed accessibility labels and visible destination state.
- Launcher/App Drawer appearance cannot be captured without automating Home, which is prohibited. The packaged adaptive-icon assets are inspected separately; final launcher appearance remains a user-visible confirmation item.

## Final assessment

No unresolved P2 visual-fidelity issue remains in the implemented Home, Contacts, custom-list, shared bottom-navigation, or header-brand surfaces. The conceptual Contacts-row content difference is a P3 product/content question and does not block this UI batch.

final result: passed
