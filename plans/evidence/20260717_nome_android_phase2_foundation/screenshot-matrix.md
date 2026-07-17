# API 35 native screenshot matrix

All PNGs were captured from the non-exported debug `NomeFoundationActivity` through `UiAutomation.takeScreenshot()` after a 750ms stable-frame delay. The harness deletes an existing same-name file before every capture. `SCREENSHOT_SHA256SUMS` contains exactly 96 entries and verifies all 96 files.

| Suite | Pages | States | Locales | Themes | Font | Count |
|---|---|---|---|---|---:|---:|
| representatives | P02, P07, P17, P21, P23 | normal | zh-CN, en | light, dark | 100% | 20 |
| states | P07 host for shared state panel | normal-panel, loading, empty, offline, error, permission, danger | zh-CN, en | light, dark | 100% | 28 |
| font200 | P02, P07, P17, P21, P23 | normal | zh-CN, en | light, dark | 200% | 20 |
| states200 | P07 host for shared state panel | normal-panel, loading, empty, offline, error, permission, danger | zh-CN, en | light, dark | 200% | 28 |
| **Total** | | five representative pages + every panel state at both scales | | | | **96** |

The shared state panel has seven mutually exclusive enum states: normal, loading, empty, offline, error, permission, and danger. `normal-panel` is the filename discriminator for an actual `NomeStatePanelState.NORMAL` render; it avoids colliding with the P07 normal representative page. The `states` and `states200` suites render all seven panel states through one representative host because they are foundation component states, not five claims about page-specific production state machines. Representative `normal` images are page-level fixture routes and are counted separately.

## Accessibility/device assertions paired with the matrix

- every actionable `NomeButton` exposes an accessible name, `Role.Button`, explicit state description where supplied, and a minimum 48dp width/height;
- error/danger panels are assertive live regions; loading/offline/permission are polite; normal/empty do not announce as changing async state;
- state icons are decorative and do not create duplicate announcements; localized title, description, and state description form the spoken contract;
- every panel state is asserted at 200% across both languages/themes with its localized state description; its test action remains visible, enabled, clickable, and at least 48dp;
- 200% screenshots reflow in a vertical scroll container without horizontal clipping in the captured frame.

These assertions validate the semantics consumed by Android accessibility services and TalkBack. They do not claim a manual audio transcript or full-page scroll capture.
