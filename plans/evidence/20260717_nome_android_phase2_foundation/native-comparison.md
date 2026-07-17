# Native screenshot comparison verdict

## Compared assets

For P02, P07, P17, P21, and P23, `comparisons/` contains:

- `Pxx-reference-vs-native-zh-light.png`: approved 470×936 light reference beside the API 35 native foundation reference;
- `Pxx-overlay-zh-light.png`: native capture resized/centered to 470×936 and blended 50/50 with the approved reference;
- `P07-states-zh-dark-contact-sheet.png`: the six non-normal dark states at 100%;
- `font200-en-light-contact-sheet.png` and `font200-zh-dark-contact-sheet.png`: five representative first frames at 200%;
- `P07-panel-states-zh-dark-font200-contact-sheet.png`: all seven actual panel states at 200%.

The approved source images are the Android exports under
`/Users/forkman03/project/nome/design/design/product/full-page-effects/android/`:

| Page | Source file | Frozen SHA-256 |
|---|---|---|
| P02 | `P02-app-lock.png` | `a7fe9c273ac62cfcc902c1af1b727362e88a48245f43c92e280360a1fce30b61` |
| P07 | `P07-home-populated.png` | `85eae2f614b3db51f8bcba143948973af42b7dea107d3da6e4c4de26f8ccd9b7` |
| P17 | `P17-direct-conversation.png` | `7cc9ab6b5e65ffa9430c376205d61ee0b0cab869296def0b76d636ec284a8fa8` |
| P21 | `P21-channel-conversation.png` | `24541465d4c50ff22726962da4d0bbd60be563d81c8943ca867a8d397157fcd6` |
| P23 | `P23-settings-home.png` | `31f7aa25035b06177bb9b0baabc99dd4097f1ade5248951ae691a58b1b653800` |

## Verdict

- **Light direction:** PASS for foundation continuity. Green action emphasis, neutral surfaces, rounded containers, information hierarchy, and semantic state treatment remain recognizably aligned with the approved light direction.
- **Dark direction:** PASS for Dark Token v1. The dark representatives preserve IA/copy priority/dimensions and remap surfaces, contrast, outlines, and elevation without inventing a second visual language.
- **States:** PASS for the shared component contract. Normal, loading, empty, offline, error, permission, and danger remain distinguishable by icon, copy, container treatment, and localized accessibility state—not color alone.
- **Bilingual stress:** PASS for the captured deterministic copy in Simplified Chinese and English.
- **200%:** PASS for captured first-frame reflow across the five representatives and all seven shared panel states; automated test actions remain at least 48dp. Long page content remains scrollable; these images are not full-scroll captures.

## Deliberate non-parity

The native representatives are not production clones of the five approved pages. They are deterministic design-system/reference compositions and visibly disclose “非实时 CORE / not live CORE”. The overlays therefore document alignment and divergence; they do not set a pixel-diff threshold or claim page completion. Production page shells and truthful state adapters remain later Phase 2 work and must receive their own native comparison.
