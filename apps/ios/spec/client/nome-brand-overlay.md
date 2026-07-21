# Nome iOS Brand and Product Overlay

> Technical specification for the Nome presentation restored on top of the official SimpleX iOS client.
>
> Related specs: [Architecture](../architecture.md) | [Navigation](navigation.md) | [Chat List](chat-list.md) | [Chat View](chat-view.md) | [Compose](compose.md)
> Related product: [Nome Product Experience](../../product/views/nome-experience.md)

**Source:** [`SimpleXApp.swift`](../../Shared/SimpleXApp.swift#L16-L246) | [`ChatListView.swift`](../../Shared/Views/ChatList/ChatListView.swift#L169-L1875) | [`NewChatView.swift`](../../Shared/Views/NewChat/NewChatView.swift#L16-L1502) | [`ChatView.swift`](../../Shared/Views/Chat/ChatView.swift#L17-L3794) | [`SettingsView.swift`](../../Shared/Views/UserSettings/SettingsView.swift#L274-L1196) | [`UserProfilesView.swift`](../../Shared/Views/UserSettings/UserProfilesView.swift#L10-L629) | [`UserAddressView.swift`](../../Shared/Views/UserSettings/UserAddressView.swift#L13-L956)

## 1. Scope

The Nome overlay is an iOS-native SwiftUI presentation layer. It owns brand assets, visible copy, layout, product grouping, preview hosts and screenshot tooling. It does not own or replace the Haskell core, protocol, database schema, message state machine or encryption implementation.

## 2. Runtime Architecture

```text
Normal launch
  SimpleXApp -> official lifecycle -> ContentView -> real ChatModel/FFI state
                                              -> Nome SwiftUI presentation

Preview launch argument
  SimpleXApp -> Nome preview host -> deterministic local fixture state
                              (no real-core or delivery claim)
```

[`SimpleXApp`](../../Shared/SimpleXApp.swift#L135-L180) selects preview hosts only when explicit Nome launch arguments are present. The preview-mode lifecycle guards at [`SimpleXApp.swift`](../../Shared/SimpleXApp.swift#L61-L75) prevent screenshot fixtures from being confused with the normal runtime path.

## 3. Presentation Modules

| Module | Primary source | Responsibility |
|---|---|---|
| Brand assets | `Shared/Assets.xcassets` | App icons, light/dark logos and in-app marks |
| Onboarding | [`SimpleXInfo.swift`](../../Shared/Views/Onboarding/SimpleXInfo.swift#L13-L273) | Nome palette, privacy explanation and local-identity introduction |
| Home | [`ChatListView.swift`](../../Shared/Views/ChatList/ChatListView.swift#L169-L1875) | Inbox, contacts, settings tab routing and preview host |
| Connection | [`NewChatView.swift`](../../Shared/Views/NewChat/NewChatView.swift#L16-L1502) | One-time invitation, scan/paste, group and public-contact presentation |
| Conversation | [`ChatView.swift`](../../Shared/Views/Chat/ChatView.swift#L17-L3794) | Header, trust banner, status chips and preview host |
| Identity | [`UserProfilesView.swift`](../../Shared/Views/UserSettings/UserProfilesView.swift#L10-L629) | Identity center and deterministic preview host |
| Public address | [`UserAddressView.swift`](../../Shared/Views/UserSettings/UserAddressView.swift#L13-L956) | Address state, actions and trust explanation |
| Settings | [`SettingsView.swift`](../../Shared/Views/UserSettings/SettingsView.swift#L274-L1196) | Simplified sections, backup/migration, help and about presentation |

## 4. Core and Data Invariants

- Production routes consume existing `ChatModel`, `ItemsModel` and official API results.
- The overlay does not synthesize a successful real-core response when the core or database is unavailable.
- Preview-only invitation/group/contact fixtures remain gated by launch arguments.
- Existing `simplex:` URL handling remains compatible.
- Bundle IDs, App Group and keychain groups remain at the compatibility identifiers in the migrated batch.
- Notification and share extensions keep the official data-sharing identifiers while changing visible display names.

### 4.1 Appearance and Contrast Invariant

- Each local Nome palette separates adaptive foreground ink from fixed brand fills.
- Foreground `navy` resolves to `#0E1B2D` in light appearance and to the resolved system `label` color in dark appearance.
- Fixed `brandNavy` remains `#0E1B2D` and is only used behind a contrasting white foreground.
- [GAP resolved 2026-07-21] Fixed brand navy is no longer used for dark-appearance titles, labels or functional icons.
- This presentation-only resolution does not read or mutate chat, identity, address, settings or message state.

### 4.2 Dynamic Type and Accessibility Invariant

- Nome-facing copy and primary actions must remain readable and reachable at accessibility Dynamic Type categories in both appearances.
- [`NomeOnboardingHeroCard`](../../Shared/Views/Onboarding/SimpleXInfo.swift#L41-L110) changes trust pills from a horizontal row to a vertical stack at accessibility sizes, while [`NomeOnboardingPill`](../../Shared/Views/Onboarding/SimpleXInfo.swift#L151-L176) uses intrinsic multi-line height instead of a fixed one-line frame.
- [`NomeChatSecurityBanner`](../../Shared/Views/Chat/ChatView.swift#L89-L202) stacks verification/status chips and keeps intrinsic vertical height at accessibility sizes. The containing conversation [`VStack`](../../Shared/Views/Chat/ChatView.swift#L550-L603) preserves that height and clips the custom message viewport to its allocated slot.
- [`NomeChatDisappearingPrompt`](../../Shared/Views/Chat/ChatView.swift#L362-L410) preserves a 44-point dismiss target, explicit accessibility label and multi-line visible action copy.
- The reversible capture matrix is defined by [`emit_cases()`](../../../../scripts/ios/capture-nome-accessibility-previews.sh#L36-L68), and screenshots are produced by [`run_case()`](../../../../scripts/ios/capture-nome-accessibility-previews.sh#L229-L265) from an already installed preview app. The `conversation` case set supports focused corrective rechecks without overwriting failed intermediate evidence.
- [`restore_simulator_settings()`](../../../../scripts/ios/capture-nome-accessibility-previews.sh#L172-L208) restores the original appearance, content-size and increased-contrast values; restoration failure invalidates the batch.
- Screenshot and static-label evidence do not prove VoiceOver focus order, announcements or activation. Those remain separate runtime gates.

## 5. Build and Verification Boundary

The repository includes `scripts/ios/` checks for brand copy, design coverage, preview tooling, normal and accessibility-size simulator screenshots, generic-device binaries, real-core routing, release identity and physical-device readiness. Verification reports MUST distinguish:

1. source/static audit;
2. deterministic preview UI;
3. simulator build and launch;
4. unsigned generic-device build;
5. signed physical-device execution;
6. real two-account communication;
7. TestFlight/App Store distribution readiness.

Passing an earlier layer does not imply a later layer.

## 6. Threat Model

| Threat | Control |
|---|---|
| Preview fixture reported as real messaging | Explicit launch-argument gate and evidence classification |
| Rebrand silently changes cryptographic or protocol behavior | No core/protocol/database changes in the migration allowlist |
| New identifiers make existing data inaccessible | Bundle ID, App Group and keychain migration deferred |
| Brand copy overstates privacy | Copy audit and privacy-claims plan retained as mandatory gates |
| Screenshot or build output contains secrets | Evidence scripts use local output roots and secret-pattern review |

## 7. Migration Provenance

The overlay was restored from legacy code commit `ca41ed118d12ba2af5913ec848945067cbcbaffb`, whose parent `apps/ios` and `scripts/ios` trees exactly match the current official `v6.5.6` trees. The dated migration plan and checkpoint record the allowlist, hashes and current verification results.
