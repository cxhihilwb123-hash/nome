# Native comparison review

Nine artifacts were regenerated from the final clean 80-image screenshot set:

- P07 approved-reference vs native light side-by-side and overlay;
- P08 loading/true-empty approved-reference vs native side-by-side and two overlays;
- four ten-state contact sheets: zh-CN light 100%, zh-CN dark 200%, en light 100%, en dark 200%.

Review result: PASS for the authorized P07/P08 production-home slice.

- The native shell preserves Nome hierarchy, green identity accent, bounded home heading, status
  panels, readable chat rows, unread counts and favorite facts.
- P08 loading and true-empty remain visibly distinct; device offline, stopped core and unavailable
  use distinct warning/error treatments and do not visually imply success or empty.
- Light/dark palettes and 100%/200% layouts show no overlap or unread/favorite collision.
- Long synthetic names ellipsize at 200% without hiding their summaries, dates or unread facts.
- The debug badge clearly marks all matrix images as production renderer, not live core.

Scope difference from the full approved P07 concept is intentional: Batch 2 does not implement the
reference search field, bottom navigation, FAB, connection cards, settings, or chat-list mutations.
The comparison is therefore a visual check of the authorized header/status/read-only-list slice,
not a pixel-parity or full-page completion claim.

Artifacts are under `comparisons/`; authoritative individual captures are under
`native-screenshots/`.
