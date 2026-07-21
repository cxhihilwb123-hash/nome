# Nome Product Experience

> **Related spec:** [spec/client/nome-brand-overlay.md](../../spec/client/nome-brand-overlay.md)

## Purpose

Nome is the native SwiftUI product presentation layered on the official SimpleX iOS client. It changes branding, Chinese-first copy, information hierarchy, navigation presentation, and trust explanations while preserving the official messaging core, encryption, database, protocol, and `simplex:` link semantics.

This document describes the migrated product surface. A historical preview or build result is not a current verification result; current evidence belongs in the dated migration record under `plans/`.

## Product Boundary

| Area | Nome behavior | Preserved behavior |
|---|---|---|
| Brand | Nome name, logo, icon, colors, extension display names | Compatible application and extension identifiers |
| Onboarding | Chinese-first privacy explanation, local identity, network and conditions cards | Official profile creation, operator selection and completion state |
| Home | Inbox, contacts and settings presentation with Nome headers and quick actions | `ChatModel` chats, unread state, filters and navigation truth |
| Connection | Add friend, one-time invitation, scan/paste, join group and public contact presentation | Official connection APIs and `simplex:` URLs |
| Conversation | Nome header, security explanation, status chips and disappearing-message prompt | Official message list, compose, delivery, call and encryption behavior |
| Identity | Nome identity center and public contact presentation | Official local users, hidden profiles and address APIs |
| Settings | Simplified Nome sections plus access to compatible advanced controls | Official preferences, database, network, notification and migration operations |

## Primary Routes

1. First launch presents the Nome onboarding treatment and creates a local profile through the official API.
2. The home experience renders real `ChatModel` state and routes to conversations, contacts, settings and new-connection actions.
3. New-connection pages call the existing invitation, QR, paste, group and public-address flows.
4. Conversation pages retain the official chat item and compose paths, with additional Nome trust and security presentation.
5. Identity and settings pages retain official data and mutation APIs behind the redesigned presentation.

## Preview Boundary

Dedicated launch arguments can render deterministic Nome preview hosts for screenshots and visual QA. Preview hosts may populate local fixture state and may skip normal app lifecycle work. They MUST NOT be presented as evidence of:

- a real invitation or public address;
- a real group or second user;
- successful message delivery;
- a production Haskell core;
- signed-device or TestFlight readiness.

Normal launches do not use these preview routes and continue through the official app lifecycle and FFI bridge.

## Compatibility and Release State

- The migrated source retains the official `chat.simplex.*` identifiers, App Group and keychain compatibility boundary.
- The migrated source does not establish Nome-owned signing, production domains, legal/support pages or App Store identity.
- A simulator UI build and an unsigned generic-device build are engineering gates, not distribution approval.
- Real communication claims require a compatible real core and device/account evidence.

## Appearance Contract

- Nome foreground ink keeps the approved deep navy in light appearance and resolves to the system label color in dark appearance.
- Fixed deep navy is reserved for brand fills that carry a contrasting white symbol; it must not be used as dark-appearance body text.
- Light and dark screenshot smoke are separate visual gates. Passing either appearance does not imply real-core or distribution readiness.

## Accessibility Contract

- Nome titles, explanations and primary actions must remain readable and reachable at iOS accessibility Dynamic Type sizes.
- Large-text validation uses the system content-size category names rather than claiming a single percentage for every text style.
- Light and dark accessibility-size previews are separate layout gates; deterministic preview fixtures remain distinct from real communication.
- At accessibility sizes, onboarding trust pills stack vertically, conversation trust/status chips use intrinsic multi-line height, and the message viewport clips content to its own layout slot rather than drawing over the safety card or composer.
- The disappearing-message prompt keeps a 44-point dismiss target with an explicit accessibility label and allows its visible action copy to wrap.
- The accepted simulator evidence covers all 14 preview fixtures in light and dark at `accessibility-large`, plus seven critical fixtures and the corrected conversation in both appearances at `accessibility-extra-extra-extra-large`.
- Screenshot review can prove visible layout and contrast. VoiceOver focus order, announcements and activation still require assistive-technology evidence on a live device or supported runtime.

## Related Product Views

- [Onboarding](onboarding.md)
- [Chat List](chat-list.md)
- [New Chat](new-chat.md)
- [Chat](chat.md)
- [User Profiles](user-profiles.md)
- [Settings](settings.md)
- [Contact Info](contact-info.md)

## Source Files

- `Shared/SimpleXApp.swift` -- production entry and debug preview routing
- `Shared/ContentView.swift` -- root navigation and Nome Lock copy
- `Shared/Views/Onboarding/` -- Nome onboarding presentation
- `Shared/Views/ChatList/ChatListView.swift` -- home, contacts and settings tabs
- `Shared/Views/NewChat/` -- add-friend and group/public-contact presentation
- `Shared/Views/Chat/ChatView.swift` -- conversation and trust presentation
- `Shared/Views/UserSettings/` -- identity, address and settings presentation
- `Shared/Assets.xcassets/` -- Nome application and in-app brand assets
- `SimpleX.xcodeproj/project.pbxproj` -- product display name and compatible build identifiers
- `../../scripts/ios/capture-nome-accessibility-previews.sh` -- reversible installed-app Dynamic Type preview evidence
