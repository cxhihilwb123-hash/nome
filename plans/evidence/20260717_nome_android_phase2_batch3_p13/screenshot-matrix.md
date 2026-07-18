# P13 screenshot matrix

Capture root: `screenshots/api35-renderer/`

The debug-only test captured:

```text
9 states
× 2 locales (zh-CN, en)
× 2 themes (light, dark)
× 2 font scales (100%, 200%)
= 72 PNG files
```

States:

1. ready-current
2. ready-incognito
3. own-link-warning
4. repeat-join-warning
5. owner-verified
6. owner-failed
7. connecting
8. pending
9. failure

All final files are 1080×2400, non-empty, and uniquely named. The final file set is also
hash-unique. Native inspection covered canonical zh-CN light ready, en light 200%, zh-CN dark
failure 200%, and bilingual owner-failure variants. The dark evidence host was corrected so its
status-bar surface and light icons agree, and the owner-failure fixture reason now comes from
bilingual debug resources.

The screenshots contain fixture labels and synthetic identity text. They prove native renderer
behavior only; `pending`, `owner-verified`, and `owner-failed` do not claim a production core
outcome.

## Final post-cleanup recapture

The rebuilt standard androidTest APK reran the complete matrix on API 35 after the TalkBack focus
fix and temporary-harness cleanup: `OK (1 test)` in 182.595 seconds, with 72/72 expected PNGs.

Pixel comparison against the approved evidence found:

- all 72 images have the same 1080×2400 dimensions and names;
- 64 product regions are byte-for-byte pixel identical below the 140 px system status bar;
- the eight Connecting product regions differ only in 212–875 pixels belonging to the
  indeterminate spinner's animation phase;
- the other differences are confined to the system status-bar clock/icons.

Side-by-side inspection of bilingual, light/dark, Failure, Connecting, and 200% samples found no
layout, typography, color, hierarchy, clipping, or touch-target regression. The structured verdict
is `visual-verdict.json`: score 99, `pass`.
