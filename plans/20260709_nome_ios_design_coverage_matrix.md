# Nome iOS design coverage matrix

Date: 2026-07-09

## Purpose

This matrix connects the approved `pages-v2` product mockups to the current
native iOS implementation, simulator evidence, and remaining real-core proof.

It is intentionally stricter than a visual checklist: a page can be visually
covered while still blocked from final functional acceptance by the current
preview iOS core libraries.

## Coverage Summary

| ID | Approved design artifact | Native implementation surface | Current simulator evidence | Current status | Remaining proof before final acceptance |
| --- | --- | --- | --- | --- | --- |
| COV-HOME | `design/product/pages-v2/01-home-inbox.png` | `ChatListView.swift`: Nome header, quick actions, bottom tabs, empty and seeded chat-list states | `scripts/ios/smoke-nome-ui.sh`: `01-home`, `06-chat-list-existing`; App Store candidate `03-home.png` | Covered for visual/navigation QA | Real non-preview profile and live chat-list state after real-core account creation |
| COV-ADD-FRIEND | `design/product/pages-v2/02-add-friend-one-time.png` | `NewChatMenuButton.swift`, `NewChatView.swift`, `SimpleXAPI.swift`: one-time link entry, QR placeholder, readiness guard | `scripts/ios/smoke-nome-ui.sh`: `10-add-friend`; App Store candidate `11-add-friend-preview-core.png`; draft `05-add-friend-needs-real-core.jpg` | Covered for repeatable preview-core UI; not functionally final | Real one-time link generation, scannable QR, native share sheet, second-account acceptance |
| COV-JOIN-GROUP | `design/product/pages-v2/03-join-group.png` | `NewChatView.swift`: group invite paste/scan page, source warning, invalid-link handling | `scripts/ios/smoke-nome-ui.sh`: `11-join-group`; App Store candidate `06-join-group-preview.png`; draft `07-join-group-needs-real-core.jpg` | Covered for repeatable preview-core UI and invalid-link handling; not functionally final | Real group invitation preview, join success, approval/pending states, live QR scan |
| COV-PUBLIC-CONTACT | `design/product/pages-v2/04-public-contact.png` | `UserAddressView.swift`, `UserPicker.swift`: public-contact card, request confirmation, copy/share/change/disable guards, explicit close | `scripts/ios/smoke-nome-ui.sh`: `12-public-contact`; App Store candidate `05-public-contact-preview-core.png`; draft `06-public-contact-needs-real-core.jpg` | Covered for repeatable preview-core UI; not functionally final | Real public address creation/load, copy/share actual address, settings persistence, second-account request |
| COV-IDENTITY | `design/product/pages-v2/05-identity-center.png` | `UserProfilesView.swift`: identity hero, search/password prompt, visible/hidden counts, add identity, hidden-profile language | `scripts/ios/smoke-nome-ui.sh`: `09-identity-center` via `-NomeIdentityCenterPreview` | Covered for repeatable visual QA | Multiple real profiles, switching, hidden profile unlock, mute/delete behavior with real data |
| COV-CONVERSATION | `design/product/pages-v2/06-conversation.png` | `ChatView.swift`, `ChatInfoToolbar.swift`: Nome safety subtitle, safety card, clean background, disappearing-message prompt/details sheet | `scripts/ios/smoke-nome-ui.sh`: `07-conversation-preview`, `08-conversation-details`; App Store candidate `07-conversation-debug-preview.png`; draft `08-conversation-needs-real-core.jpg` | Covered for seeded debug visual QA; not functionally final | Real one-to-one and group messages, files, voice, delivery receipts, verified/unverified state, real disappearing-message persistence |
| COV-SETTINGS | `design/product/pages-v2/07-settings-safety.png` | `SettingsView.swift`, `PrivacySettings.swift`, `NetworkAndServers.swift`, `ConnectDesktopView.swift`: embedded settings tab, backup hub, full settings escape hatch, about/help | `scripts/ios/smoke-nome-ui.sh`: `13-settings`; App Store candidates `08-settings.png`, `09-about.png`, `10-help.png`; draft `09-settings.jpg`, `10-help.jpg` | Covered for repeatable visual/navigation QA | Real migration/device transfer, server/Tor/private routing data, desktop linking |

## Adjacent App Screens

These screens are not separate `pages-v2` mockups, but they are part of the
native iOS app flow and are covered by smoke or App Store planning evidence:

| Screen | Evidence | Why it matters |
| --- | --- | --- |
| Onboarding welcome | `scripts/ios/smoke-nome-ui.sh`: `02-onboarding-welcome`; App Store candidate `01-onboarding-welcome.png` | First visible product framing: no phone number, no public ID, local storage |
| Onboarding local identity | `scripts/ios/smoke-nome-ui.sh`: `03-onboarding-profile`; App Store candidate `02-onboarding-local-identity.png` | Explains local identity instead of cloud account |
| Onboarding network setup | `scripts/ios/smoke-nome-ui.sh`: `04-onboarding-network` | Separates identity privacy from server/network privacy |
| Onboarding network conditions | `scripts/ios/smoke-nome-ui.sh`: `05-onboarding-conditions` | Bounded privacy language for network-use confirmation |
| Contacts tab | `scripts/ios/smoke-nome-ui.sh`: `14-contacts`; App Store candidate `04-contacts.png`; draft `04-contacts.jpg` | The shipped iOS tab structure needs a contacts home even though the original page set focused on core protocol concepts |

## Automated Gates

Current repeatable gates:

```bash
scripts/ios/check-nome-design-coverage.sh
scripts/ios/check-nome-smoke-visual-quality.sh --manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --expected-count 14 --output /tmp/nome-ios-smoke-visual-quality-current --force
scripts/ios/export-nome-design-evidence-state.sh --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv --output /tmp/nome-ios-design-evidence-state-current --force
scripts/ios/smoke-nome-ui.sh --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4 --output /tmp/nome-ios-smoke-design-coverage
scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke --skip-smoke-build --simulator 95CA9F4F-F85B-4AC9-ADAE-62098924E3B4
scripts/ios/check-nome-ios-readiness.sh --allow-blockers --smoke-manifest /tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv
```

The design coverage gate now checks three layers:

- the seven approved `pages-v2` mockup files are present;
- the coverage matrix contains the seven coverage IDs;
- the native Swift implementation still exposes the expected Nome page
  components, preview hosts, preview launch arguments, and preview-core
  readiness guard used by the smoke tests.
- the add/join/public-contact secondary pages keep their compact Nome
  navigation brand and page-level header components, so the flow does not
  regress back into generic system-form surfaces.
- the design evidence export maps the seven approved `pages-v2` files to the
  signed smoke screenshots and records design/smoke hashes, bytes, visual
  status, page-level visual acceptance, final functional acceptance, blocker
  batch, App Store dependency, and remaining real-core proof for each coverage
  row.
- the smoke visual-quality check re-reads each screenshot file named in the
  manifest with `sips`, verifies the manifest width/height, byte size, and
  SHA-256 against the file currently on disk, rejects duplicate screenshot
  hashes, and writes machine-readable `images.tsv` / `failures.tsv` evidence.

The smoke output should contain fourteen screenshots:

1. `01-home`
2. `02-onboarding-welcome`
3. `03-onboarding-profile`
4. `04-onboarding-network`
5. `05-onboarding-conditions`
6. `06-chat-list-existing`
7. `07-conversation-preview`
8. `08-conversation-details`
9. `09-identity-center`
10. `10-add-friend`
11. `11-join-group`
12. `12-public-contact`
13. `13-settings`
14. `14-contacts`

## Current Boundary

The design coverage is now repeatable for the main visual surfaces, but the
project is not complete until real-core testing proves the functional paths
listed in the final column above.

Latest visual-smoke evidence after the secondary-page header refinement:
`/tmp/nome-ios-smoke-visual-header-signed-20260710-002440/contact-sheet.png`
and
`/tmp/nome-ios-smoke-visual-header-signed-20260710-002440/manifest.tsv`.
This pass used the smoke script's signed simulator build; a manual
`CODE_SIGNING_ALLOWED=NO` build is compile-only evidence and must not be used
for launch/screenshot acceptance because it lacks the App Group entitlement
required during app startup.

`scripts/ios/check-nome-ios-readiness.sh` can now reuse that manifest via
`--smoke-manifest`, which validates the 14 expected screenshot labels, a single
dimension set, non-empty file sizes, and unique screenshot hashes without
launching another simulator smoke pass.

Latest machine-readable design evidence packet:
`/tmp/nome-ios-design-evidence-state-current-20260710-012530`.
It records seven coverage rows and zero missing rows.

Latest refreshed build-and-smoke evidence:
`/tmp/nome-ios-smoke-current-20260710-014941/contact-sheet.png`
and
`/tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv`.
This pass rebuilt `SimpleX (iOS)`, installed `Nome.app`, launched all fourteen
smoke cases, and produced no new Nome crash report. Re-exported design evidence
is in `/tmp/nome-ios-design-evidence-state-smoke-current-20260710-015112`,
with seven coverage rows, zero missing rows, and fourteen smoke labels.

Latest smoke visual-quality evidence:
`/tmp/nome-ios-smoke-visual-quality-current-fixed-20260710-041950`.
It verifies all fourteen PNG screenshots from
`/tmp/nome-ios-smoke-current-20260710-014941/manifest.tsv` are decodable,
portrait-oriented, larger than the minimum byte threshold, hash-unique, and
match the manifest's dimensions, byte sizes, and SHA-256 values.

Latest page-level design evidence packet:
`/tmp/nome-ios-design-evidence-state-page-evidence-20260710`.
It records seven coverage rows, seven `page_evidence.tsv` rows, seven visual
PASS rows, seven final functional BLOCKED rows, zero missing rows, and fourteen
smoke labels. The page evidence keeps the current product-design truth explicit:
the approved effect-picture pages are visually covered by signed iOS smoke
screenshots, while final acceptance still depends on real-core, physical-device,
manual-QA, and final App Store screenshot evidence.
