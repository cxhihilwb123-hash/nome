# Production-renderer screenshot matrix

Status: **PASS, 80/80 captures** on API 35, timezone `Asia/Shanghai`.

The debug-only, non-exported `NomeHomeEvidenceActivity` renders the production
`NomeHomeRouteContent` with a visible localized “production renderer / not live core” badge. It
uses synthetic rows, fixed year-2000 timestamps, a synthetic profile label, explicit locale/theme,
and 100%/200% font scale. It never serves as live-core evidence.

## Matrix

- 10 states: populated, loading, first use, true empty, filtered no result, network unknown with
  cache, device offline with cache, core stopped with cache, unavailable, unavailable with cache.
- 2 locales: `zh-CN`, `en`.
- 2 palettes: light, dark.
- 2 font scales: 100%, 200%.
- Total: `10 × 2 × 2 × 2 = 80`.

Execution:

- instrumentation: PASS, `OK (1 test)`, 93.138s;
- unique filenames: 80/80;
- non-empty valid PNG: 80/80;
- content duplicates: 0;
- screenshot checksum verification: 80/80;
- `SCREENSHOT_SHA256SUMS` SHA-256:
  `6e055bd763c1d38f1135ac837bf460b319277ef9881d46925a545f88dc80e079`.

The first screenshot attempt inherited the previously enabled TalkBack focus rectangle. After the
real TalkBack traversal was preserved, the service was disabled and all 80 captures were rerun.
Only the clean rerun is stored in `native-screenshots/` and hashed.

Visual review found no overlap, clipped status copy, unread/favorite loss, privacy-preview leak,
or 200% layout collision. Long synthetic chat names ellipsize by design at 200%; summaries, dates,
favorite and unread facts remain distinct. The contact sheets are review aids and are resized;
individual PNGs remain the authoritative layout evidence.

`FIRST_USE` and `FILTERED_NO_RESULT` are renderer-only/deferred production states. Their presence
in this matrix does not change that boundary.
