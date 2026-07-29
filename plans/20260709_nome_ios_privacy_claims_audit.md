# Nome iOS privacy claims and source availability audit

Date: 2026-07-09

## Scope

This is an engineering and product-copy audit for the current Nome iOS UX pass.
It is not legal advice and does not replace App Store privacy, export
compliance, trademark, or license review.

The goal is to make sure Nome's user-facing privacy language does not promise
more than the current SimpleX-compatible implementation can support.

## Evidence reviewed

- `apps/ios/Shared/Views/Onboarding/SimpleXInfo.swift`
- `apps/ios/Shared/Views/Onboarding/HowItWorks.swift`
- `apps/ios/Shared/Views/Onboarding/YourNetwork.swift`
- `apps/ios/Shared/Views/Onboarding/ChooseServerOperators.swift`
- `apps/ios/Shared/Views/UserSettings/SettingsView.swift`
- `apps/ios/Shared/Views/UserSettings/UserAddressView.swift`
- `apps/ios/Shared/Views/Chat/ChatView.swift`
- `plans/20260709_nome_ios_release_gate_review.md`
- `README.md`
- `LICENSE`
- `docs/TRADEMARK.md`
- `assets/ASSETS_LICENSE.md`

## Claim audit

| Claim area | Current Nome wording | Implementation evidence | Verdict |
| --- | --- | --- | --- |
| No phone number / no cloud account | Onboarding says Nome does not need a phone number, public username, or cloud account. | The current iOS flow creates a local profile and uses invitation/contact links rather than phone-number signup. | Acceptable. Keep phrased as "not required", not as "impossible to identify you". |
| No public user ID | Onboarding says others cannot search you by a global username. | SimpleX-compatible contact establishment uses one-time links, QR codes, or reusable contact address, not a global username directory. | Acceptable. Continue distinguishing public contact address from one-time links. |
| End-to-end encryption | Chat safety banner says direct/group messages are end-to-end encrypted and safety code can be checked. | Existing `ChatInfo`, `Contact.verified`, security-code, and upstream SimpleX chat model remain in place; Nome did not alter protocol logic. | Acceptable for message UI. Real two-account messaging still needs real-core test before release claims. |
| Disappearing messages | Prompt and preview sheet mention automatic deletion. | `ChatInfo.featureEnabled(.timedMessages)` and `ChatInfo.ttl(...)` drive UI; preview sheet explicitly says deletion only controls Nome message retention and cannot undo saved/screenshotted/exported content. | Acceptable after the preview-safe wording fix. Do not market it as guaranteed remote erasure. |
| Server privacy | Onboarding says messages go through message servers and network settings can be changed. | Existing server/Tor settings remain reachable; preview-core path is guarded when real server data is unavailable. | Acceptable. Avoid saying servers see nothing; use "reduce exposure" language. |
| Tor and anonymous profiles | Network/onboarding copy says identity privacy and network privacy are separate. | Settings keep Tor/server controls separate from profile/hidden-profile concepts. | Acceptable and important. Keep avoiding "Tor equals anonymous account" wording. |
| Public contact address | Public-contact UI says reusable address is useful for being found, while one-time links are still recommended for one-to-one invites. | Existing user-address screen and preview-core guards preserve public-address behavior without pretending to create a real address in preview. | Acceptable. Real copy/share/change/disable still needs real-core testing. |
| Local data and backup | Onboarding/settings say identity, contacts, messages, backups, and migration are controlled on device. | Existing database/export/import/migration screens remain reachable; no cloud account sync path was introduced. | Acceptable as product framing. Real export/import/migration still needs data test. |
| Upstream compatibility | About page says Nome preserves `simplex:` links, SimpleX network, and upstream communication logic. | Release-gate review keeps bundle/link/app-group/keychain decisions compatibility-first for this phase. | Acceptable. Keep compatibility descriptions separate from product branding. |

## Copy changes made in this audit

- Changed network-use confirmation copy from "Nome 可以保护你的连接关系" to
  "Nome 会减少连接关系暴露", because the latter is more accurate and does not
  imply absolute relationship privacy.

## Source availability and attribution packet

Current evidence:

- `LICENSE` is GNU AGPLv3.
- `README.md` states the software is licensed under AGPLv3.
- `README.md` says SimpleX names/logos/graphic assets are outside the AGPL grant
  and governed by `docs/TRADEMARK.md` and `assets/ASSETS_LICENSE.md`.
- `docs/TRADEMARK.md` allows compatibility descriptions but not using SimpleX as
  fork software branding.
- `assets/ASSETS_LICENSE.md` treats upstream application graphic assets as
  proprietary unless permission is granted.
- Nome's about page includes upstream attribution and a link to
  `https://github.com/simplex-chat/simplex-chat`.

Prepared release packet:

- Keep upstream attribution in `关于 Nome`.
- Keep protocol/network references as compatibility wording.
- Publish a public modified-source repository or archive before external
  distribution.
- Record the final source URL in this file and in
  `plans/20260709_nome_ios_release_gate_review.md`.
- Treat the Nome logo/app icon as replacement fork-owned assets.
- Before public release, inventory remaining upstream SimpleX graphic assets and
  either replace them, remove them from branded surfaces, or obtain permission.

Open release values:

- Public modified-source URL: `TBD`
- Nome asset/trademark decision: `TBD`
- Final App Store privacy nutrition labels: `TBD`
- Final encryption/export-compliance answer: `TBD`

## Release gate result

Passed for the current UX/test pass:

- Primary Nome privacy copy now uses bounded language.
- The app separates identity privacy, server/network privacy, Tor, and
  disappearing-message behavior.
- Upstream attribution is present in-app.
- Source availability and asset/trademark requirements are documented as a
  release packet.

Still blocking external release:

- Real iOS core libraries are not installed in this checkout.
- Real two-account messaging and real invitation/public-address/group behavior
  are not verified.
- Public modified-source URL is not defined.
- Final asset/trademark decision is not recorded.
- Final App Store privacy/export-compliance review has not been completed.
