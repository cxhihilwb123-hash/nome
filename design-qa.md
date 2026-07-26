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
