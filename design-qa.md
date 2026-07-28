# Nome macOS visual QA

Date: 2026-07-24

## Scope

This pass covers the two states called out in the redesign request:

- first-run welcome;
- authenticated empty workspace / new connection.

It also checks the upgraded server settings state that previously exposed
upstream operators and conditions.

## Visual sources

- Mobile brand reference:
  `/Users/forkman03/project/nome/design/design/product/full-page-effects/android/P03-welcome.png`
- Desktop workspace reference:
  `/Users/forkman03/project/nome/design/design/product/desktop/nome-desktop-core-workspace-v1.png`
- User-reported old connection screen:
  `/Users/forkman03/project/nome/simplex-chat/plans/evidence/20260724_nome_desktop_redesign_v2/01-current-connection-screen.jpeg`

## Runtime evidence

- Welcome:
  `/Users/forkman03/project/nome/simplex-chat/plans/evidence/20260724_nome_desktop_redesign_v2/runtime-welcome-zh-1365x768.jpeg`
- New connection workspace:
  `/Users/forkman03/project/nome/simplex-chat/plans/evidence/20260724_nome_desktop_redesign_v2/runtime-new-chat-upgraded-zh-1365x768.jpeg`
- Upgraded server settings:
  `/Users/forkman03/project/nome/simplex-chat/plans/evidence/20260724_nome_desktop_redesign_v2/runtime-network-settings-final-zh-1365x768.jpeg`

## Mandatory combined comparisons

- Old and new connection screens, exact same 1366 × 768 viewport and state:
  `/Users/forkman03/project/nome/simplex-chat/plans/evidence/20260724_nome_desktop_redesign_v2/visual-comparison-old-vs-new-chat-2732x768.png`
- Welcome reference and runtime in one 2732 × 768 input:
  `/Users/forkman03/project/nome/simplex-chat/plans/evidence/20260724_nome_desktop_redesign_v2/visual-comparison-welcome-reference-vs-runtime-2732x768.png`

For the welcome comparison the 470 × 936 mobile source is proportionally
fitted into a 1366 × 768 neutral canvas without cropping or retouching. The
runtime uses the same state and canvas size with a platform-adapted desktop
layout.

## Findings and corrections

### Branding

- The rail, welcome header, operator row, and package use the Nome mark.
- The mark retains its transparent background; the old white logo tile is
  absent.
- Primary navigation and all inspected headings use the Nome navy/green
  palette.

### Layout and density

- The two large blue action cards are gone.
- The new-connection actions use one compact white list with restrained pale
  green icon surfaces and clear row separators.
- Welcome uses a quiet full-width desktop canvas, centered reading column, and
  persistent bottom action bar; no large blue panel remains.
- The authenticated empty state preserves the desktop rail and list hierarchy
  without reintroducing mobile promotional cards.

### Typography and copy

- Chinese titles, descriptions, search placeholder, invite actions, about
  label, and server operator label use Nome wording.
- The remaining visible `SimpleX` name is limited to truthful protocol or
  `simplexmq` technical attribution, not product-brand copy.
- No text is clipped or overlaps at the tested 1366 × 768 viewport.

### Server settings

- The upgraded settings screen presents one preset operator: Nome.
- SimpleX Chat, Flux, and upstream conditions rows are absent.
- The title gradient was replaced with the desktop Nome green.

### Interaction and accessibility

- Rail navigation to Messages, Contacts, and Settings works.
- Network & Servers and Nome operator detail routes open from the packaged app.
- The accessibility tree exposes the three new-connection actions, all
  settings rows, the Nome operator, and server detail controls.

## Severity review

- P0: none.
- P1: none.
- P2: none after replacing the blue title treatment and correcting the
  operator/profile copy.

Live Nome service connectivity and authorization remain separate engineering
gates. They are not inferred from, or hidden by, this visual QA result.

final result: passed

---

# Nome macOS database unlock visual QA

Date: 2026-07-28

## Scope

This pass covers the packaged macOS database-password screen and its primary
unlock interaction. Android continues to delegate to the existing screen, and
the existing database migration, SQL, keychain, downgrade, and recovery states
remain outside this Desktop-only presentation override.

## Source and implementation

- Selected visual source:
  `/Users/forkman03/.codex/generated_images/019fa3ee-38be-70e0-8747-5742c71a3c58/exec-4bd6dfb1-5664-4c77-ae01-70b33140c5da.png`
- Final packaged implementation:
  `/Applications/Nome.app`
- Final runtime capture:
  `/private/tmp/nome-unlock-redesign-audit-20260728/implementation-final.jpeg`
- Final combined comparison:
  `/private/tmp/nome-unlock-redesign-audit-20260728/comparison-final.png`

The final runtime was captured at 1364 x 768 in the empty-password field
state. The 1672 x 941 source has the same aspect ratio and was normalized to
1364 x 768 before placing source and runtime side by side in one 2728 x 768
comparison input.

## Comparison history

- V1 used the correct 38/62 split and form geometry, but the brand statement
  sat too low and the security and Return hints were compressed into one row.
- V2 moved the brand statement toward the source position, increased its
  hierarchy, moved the form slightly upward, and restored separate security
  and keyboard-hint rows.
- Final kept the accepted V2 geometry and added a real Desktop Return-key
  handler after interaction testing showed that IME actions alone did not
  respond to a physical Return key.

## Findings

- The wide layout uses a 38 percent deep-navy brand panel and 62 percent warm
  neutral unlock panel; widths below 900 dp collapse to a single-column form.
- The visible brand asset is the packaged Nome app icon, and the decorative
  treatment reuses the shipped Nome wallpaper rather than a fabricated mark.
- The password field, visibility affordance, primary button, security notice,
  and Return hint align to one 480 dp form column with no clipping at the tested
  viewport.
- Nome capitalization is consistent in all newly visible copy.

## Interaction and accessibility

- The password field receives focus automatically.
- Entered text is masked by default; the visibility control changes from
  `显示密码` to `隐藏密码` and exposes matching accessibility descriptions.
- Physical Return invokes the same database-open validation as the primary
  button. With the empty test value it reached the existing `密码错误` alert,
  proving the key handler without claiming that an unknown database password
  was valid.
- The accessibility tree exposes the page headings, password field, visibility
  action, `打开 Nome` button, security notice, and Return hint.

## Severity review

- P0: none.
- P1: none.
- P2: none after the brand-position, hint-layout, and physical Return-key
  corrections.

Desktop tests: 42 suites, 192 tests, 0 failures, 0 errors, 0 skipped. Android
common-source compilation also passed. The installed app passed deep strict
code-signature verification.

final result: passed

---

# Nome macOS unlock logo corner follow-up QA

Date: 2026-07-28

## Source and implementation

- Source visual truth (pre-fix packaged screen):
  `/private/tmp/nome-unlock-redesign-audit-20260728/implementation-final.jpeg`
- Implementation screenshot (installed rounded-logo build):
  `/private/tmp/nome-logo-rounded-audit-20260728/implementation-final.jpeg`
- Full-view side-by-side comparison:
  `/private/tmp/nome-logo-rounded-audit-20260728/comparison-before-after.png`
- Focused logo comparison:
  `/private/tmp/nome-logo-rounded-audit-20260728/focus-comparison.png`

Both full-view captures are 1364 x 768 pixels at the same macOS window size and
the same empty-password screen. They were compared without density rescaling.
The implementation field retained keyboard focus during capture, so its green
focus outline is an interaction-state difference unrelated to this logo-only
change.

## Findings

- Fonts and typography: unchanged; the Nome wordmark remains aligned to the
  icon and retains the same size and weight.
- Spacing and layout rhythm: unchanged; the icon stays at 58 dp and keeps its
  original top-left placement. A 14 dp `RoundedCornerShape` clip now gives the
  four corners the app-icon silhouette requested by the user.
- Colors and visual tokens: unchanged; the shipped icon colors and navy panel
  remain intact.
- Image quality and asset fidelity: the implementation continues to use
  `MR.images.nome_app_icon`; the focused comparison shows a clean rounded mask
  without clipping the central Nome mark or introducing rough edges.
- Copy and content: unchanged.

## Comparison history

- Before: the 58 dp asset rendered as a visibly square tile.
- Fix: clipped the existing official asset with a 14 dp rounded rectangle; no
  replacement image or layout change was introduced.
- After: full-view and focused comparisons show the requested rounded corners
  with no actionable P0, P1, or P2 differences.

Focused evidence was required because the only requested change occupies a
small region in the full 1364 x 768 screen.

final result: passed
