# Nome Android UI Fix Batch 03 — Summary

Date: 2026-07-23

Scope: Home/Contacts/custom-list visual redesign, shared primary navigation hardening, list-management return behavior, theme-safe header branding, and smaller adaptive launcher icon artwork.

## Outcome

- Home, Contacts, and a selected custom list now use the approved hierarchy: brand header, page-specific title/subtitle, compact search, one contained list shelf, grouped rows, FAB, and shared bottom navigation.
- The list feature remains available. Home exposes All, user-created lists, and Add list. A selected user list exposes Manage list and filters the visible conversations through the existing official tag/list behavior.
- Home, Contacts, and Settings continue to use the same 76dp `NomePrimaryBottomNavigation` component. The selected item uses one green top indicator plus green icon/text rather than a separate icon pill.
- The shared navigation accounts for both current Compose insets and the legacy root-window reports exposed by the two Vivo builds. A zero-bottom-inset Vivo fallback prevents OEM decor cropping without changing the 76dp content area.
- Android Back now closes the official list editor/list overlays through their existing close callbacks, preventing the editor from surviving while the app backgrounds.
- The header uses the repository's authoritative transparent 700x285 Nome lockup. Dark mode explicitly selects the existing approved white-and-green variant, preventing an invisible dark-text logo or white backing plate.
- Adaptive launcher foreground artwork now places the approved Nome mark at 60dp high on the standard 108dp foreground canvas. Five densities and both default/dark-blue variants remain transparent and centered.
- No core, protocol, database, account, server, message-state, or communication behavior was changed.

## Verification summary

- Final debug APK and Android test APK assembly: PASS.
- JVM unit tests: 54 passed, 0 failures/errors/skips.
- Release lint: 0 Error, 0 Fatal.
- API 35 emulator Compose instrumentation: 15 passed in 20.665s.
- API 35 screenshot instrumentation: 1 passed in 22.413s and produced twelve refreshed non-sensitive native renders.
- Design QA: passed; see `/Users/forkman03/project/nome/simplex-chat/design-qa.md`.
- V2048A (420dpi): exact final APK installed at 19:59:07; real Home, Contacts, Settings, custom-list, Manage list, top Back, Android Back, and bottom-nav return path passed. Navigation item bounds are 200px high = 76.19dp.
- V2047A (480dpi): exact final APK installed at 19:49:48; the same real-click path passed. Navigation item bounds are 228px high = 76.0dp.
- Both devices finished on Home with the fixed All/list shelf and shared navigation visible.

## Privacy and evidence boundary

Screenshot/recording protection remained enabled on real accounts. Physical-device verification uses real clicks plus fixed accessibility labels and visible destination state; no message/contact content is printed or stored. Light/dark and Chinese/English pixel evidence comes from a non-sensitive debug activity using the same production Compose components. No message was sent, no contact/list was deleted, and no account data was changed.

Launcher/App Drawer capture is intentionally absent because automatically issuing Home is prohibited. Packaged resource inspection proves the icon geometry and transparency; final launcher appearance remains a user-visible confirmation item.

The physical accounts remained in their existing Chinese/light configuration. Changing a user's system locale/theme solely for evidence would be an out-of-scope device mutation, so English and dark-mode pixel conclusions are supplied by the deterministic API 35 production-component renderer. Physical UI trees do not expose selected tint pixels; selected-state semantics are asserted by Compose tests and physical destination changes are proved by real clicks.
