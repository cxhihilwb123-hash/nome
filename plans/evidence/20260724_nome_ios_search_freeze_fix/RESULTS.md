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

## Empty-result placement follow-up

The follow-up report was reproduced by entering an unmatched query in the
deterministic preview. `noChatsView()` was rendered as a sibling overlay above
the complete list, so SwiftUI centered it in the available page area. Its
vertical position therefore changed with the keyboard, safe area, and list
height instead of staying attached to the search controls.

The empty-result view is now a real list row immediately after the search and
filter row. It uses the Nome secondary text color, a stable 14-point font, and
fixed row insets.

| Check | Result |
| --- | --- |
| Simulator build after placement fix | PASS |
| Unmatched query shows the hint directly below filters | PASS |
| Matching query hides the hint and shows only the matching chat | PASS |
| Clearing the query restores all three seed chats | PASS |
| Cancelling search restores the Nome bottom tabs | PASS |
| Personal Team Nome `6.5.6 (343)` arm64 build | PASS |
| Overwrite-install Build 343 without uninstall | PASS |
| Physical-device visual confirmation | PENDING_DEVICE_UNLOCK |

The iPhone Mirroring connection was unavailable after installation and
`devicectl` reported the phone as locked. No device or application password was
entered.
