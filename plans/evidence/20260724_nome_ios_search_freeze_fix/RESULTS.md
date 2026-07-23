# Nome iOS home search freeze fix

Date: 2026-07-24

Scope: iOS home chat-list search only. Android and the integrated
SimpleX Core were not changed.

## Reproduction

The freeze was reproduced in the iOS simulator with the deterministic
`-NomeChatListPreview` seed data:

- the initial page was responsive and exposed the search text field;
- tapping the field took about 5.5 seconds without entering a usable search
  state;
- the application accessibility tree disappeared while the visible page
  remained stuck;
- a three-second process sample showed the main thread continuously updating
  SwiftUI layout and `AttributeGraph`, including the home toolbar status view.

This rules out the chat Core and server connection as the direct cause.

## Cause and fix

Focusing the search field changed `searchMode`. The same state transition also
removed the Nome bottom tab bar, installed the legacy one-hand bottom toolbar,
and inverted the complete chat list. That combined safe-area, toolbar
preference, and list-layout update formed a SwiftUI layout loop.

The fix keeps search mode on the stable Nome toolbar path and limits chat-list
inversion to the onboarding-only state. Existing onboarding behavior remains
unchanged.

## Verification

Simulator, Nome debug preview:

| Check | Result |
| --- | --- |
| iOS simulator Debug build | PASS |
| Tap search and show Cancel | PASS |
| Set query to `林` | PASS |
| Filter three seed chats to `林晓 / Lin Xiao` | PASS |
| Clear query and restore all seed chats | PASS |
| Cancel and restore Nome bottom tabs | PASS |
| Three consecutive focus/cancel cycles | PASS |

All three repeated cycles entered search and restored the home page without a
freeze.

Personal Team physical-device package:

| Check | Result |
| --- | --- |
| Build Nome `6.5.6 (342)` for arm64 | PASS |
| Overwrite-install on the existing Personal Team bundle | PASS |
| Preserve app container by avoiding uninstall | PASS |
| Launch and tap search on unlocked device | PENDING_UNLOCK |

The install succeeded. The first automated launch attempt was rejected by iOS
because the phone was locked; no application credential was entered.
