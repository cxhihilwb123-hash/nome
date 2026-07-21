# Nome iOS release gate review

Date: 2026-07-09

## Scope

This is a TestFlight-preparation review for release-sensitive identifiers,
extensions, license/source availability, and attribution.

It is intentionally a review record, not a migration patch. Changing these
identifiers before the real-core and data-migration path is ready would affect
deep links, app groups, keychain access, notification delivery, share extension
handoff, and existing user data.

## Evidence commands

```bash
scripts/ios/check-real-core.sh

DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild \
  -project apps/ios/SimpleX.xcodeproj \
  -list

plutil -lint \
  apps/ios/SimpleX--iOS--Info.plist \
  "apps/ios/SimpleX (iOS).entitlements" \
  "apps/ios/SimpleX NSE/Info.plist" \
  "apps/ios/SimpleX NSE/SimpleX NSE.entitlements" \
  "apps/ios/SimpleX SE/Info.plist" \
  "apps/ios/SimpleX SE/SimpleX SE.entitlements"
```

## Current targets

`xcodebuild -list` reports these iOS project targets:

- `SimpleX (iOS)`
- `Tests iOS`
- `SimpleX NSE`
- `SimpleX SE`
- `SimpleXChat`

The release-relevant app targets are the main iOS app, the notification service
extension, and the share extension.

## Identifier inventory

| Surface | Current value | Source | Release decision |
| --- | --- | --- | --- |
| Main app bundle id | `chat.simplex.app` | `PRODUCT_BUNDLE_IDENTIFIER` | Keep for the current compatibility-first fork pass. A dedicated Nome App Store/TestFlight track needs a new bundle id and a migration plan. |
| Main app display/product name | `Nome` | `INFOPLIST_KEY_CFBundleDisplayName`, `PRODUCT_NAME` | Already Nome-facing. |
| Custom URL scheme | `simplex` | `CFBundleURLSchemes` | Keep. Removing it would break existing SimpleX-compatible invitation/contact links. |
| URL type name | `chat.simplex.app` | `CFBundleURLName` | Compatibility value. Rename only if all link routing and app-store identity are migrated. |
| Associated domains | `simplex.chat`, `www.simplex.chat`, `*.simplex.im`, `*.simplexonflux.com` | main app entitlements | Keep for SimpleX network compatibility. Add Nome domains later only after a real web domain and AASA files exist. |
| App group | `group.chat.simplex.app` | app/NSE/SE entitlements | Keep for now. A new group requires coordinated data migration and extension updates. |
| Keychain access group | `$(AppIdentifierPrefix)chat.simplex.app` | app/NSE/SE entitlements | Keep for now. A new group risks losing access to existing local credentials unless migration is designed. |
| Background task id | `chat.simplex.app.receive` | main app Info.plist | Compatibility value. Change only with bundle-id migration. |
| Notification service extension id | `chat.simplex.app.SimpleX-NSE` | `SimpleX NSE` build settings | Compatible but not Nome-branded. Rename only as part of a full identifier migration. |
| Notification service display name | `Nome Notifications` | `INFOPLIST_KEY_CFBundleDisplayName`, extension `InfoPlist.strings` | Nome-facing display name applied without changing target name, executable name, bundle id, app group, or keychain access. |
| Notification extension point | `com.apple.usernotifications.service` | `SimpleX NSE/Info.plist` | Correct extension type. |
| Notification filtering entitlement | `com.apple.developer.usernotifications.filtering = true` | `SimpleX NSE.entitlements` | Present. Needs Apple capability review for release signing. |
| Share extension id | `chat.simplex.app.SimpleX-SE` | `SimpleX SE` build settings | Compatible but not Nome-branded. Rename only as part of a full identifier migration. |
| Share extension display name | `Nome Share` | `INFOPLIST_KEY_CFBundleDisplayName`, extension `InfoPlist.strings` | Nome-facing display name applied without changing target name, executable name, bundle id, app group, or keychain access. |
| Share extension point | `com.apple.share-services` | `SimpleX SE/Info.plist` | Correct extension type. |
| Internal framework id | `chat.simplex.SimpleXChat` | `SimpleXChat` build settings | Keep for current compatibility-first development pass. Rename only with the app/extension identifier migration. |
| iOS test bundle id | `chat.simplex.Tests-iOS` | `Tests iOS` build settings | Keep for current development pass. Rename with the rest of the release track if the project moves to Nome-owned identifiers. |
| Encryption export flag | `ITSAppUsesNonExemptEncryption = false` | main app Info.plist | Must be rechecked before App Store submission against the final cryptography/export-compliance answer. |

## Current decision

For the current app-development pass, keep the compatibility identifiers:

- `chat.simplex.app`
- `simplex`
- `group.chat.simplex.app`
- `$(AppIdentifierPrefix)chat.simplex.app`
- `chat.simplex.app.SimpleX-NSE`
- `chat.simplex.app.SimpleX-SE`
- `chat.simplex.SimpleXChat`
- `chat.simplex.Tests-iOS`

Documented compatibility exceptions for the current development pass:

- `chat.simplex.app`
- `chat.simplex.app.SimpleX-NSE`
- `chat.simplex.app.SimpleX-SE`
- `chat.simplex.SimpleXChat`
- `chat.simplex.Tests-iOS`
- `simplex`
- `chat.simplex.app.receive`
- `group.chat.simplex.app`
- `$(AppIdentifierPrefix)chat.simplex.app`
- `applinks:simplex.chat`
- `applinks:www.simplex.chat`
- `applinks:*.simplex.im`
- `applinks:*.simplexonflux.com`

This matches the current product boundary: Nome is a UX and brand layer over the
existing SimpleX-compatible app, not a new messaging protocol or independent
network identity yet.

For an independent Nome TestFlight or App Store listing, these values must be
reviewed again and likely changed together:

- main bundle id;
- app group;
- keychain access group;
- notification service extension id;
- share extension id;
- associated domains;
- AASA files and public web domain;
- data/keychain migration behavior.

## License, attribution, and source availability

Evidence:

- `LICENSE` is GNU AGPLv3.
- `README.md` states the software is licensed under AGPLv3.
- `README.md` also states that SimpleX names, logos, application graphic assets,
  and website graphic assets are not covered by AGPL and are governed by
  `docs/TRADEMARK.md` and `assets/ASSETS_LICENSE.md`.
- `docs/TRADEMARK.md` prohibits using SimpleX naming/branding as fork software
  branding, while allowing compatibility descriptions.
- `assets/ASSETS_LICENSE.md` marks application graphic assets as proprietary
  unless permission is granted.
- The Nome about screen includes upstream attribution and links to
  `https://github.com/simplex-chat/simplex-chat`.

Release preparation status:

- Upstream attribution exists in-app.
- Nome branding avoids using SimpleX as the product name.
- The fork must publish or otherwise offer the modified Nome source code before
  external distribution.
- The final public source URL is not defined yet.
- The current Nome logo/app-icon assets should be treated as the fork's own
  replacement assets; any remaining upstream SimpleX graphic assets need either
  removal/replacement or permission before a public branded release.

Do not mark the AGPL/source availability gate complete until the public Nome
source location and asset/trademark decision are recorded.

## Static validation result

Passed:

- all reviewed plist and entitlement files pass `plutil -lint`;
- the iOS project lists expected app, notification-extension, and share-extension
  targets;
- identifier values are now recorded with an explicit compatibility-first
  decision.
- the App Store screenshot candidate folder
  `design/app-store/ios-real-screens/` contains real iOS Simulator screenshots,
  not generated product mockups.
- the screenshot manifest and checker now record candidate dimensions, hashes,
  alpha-channel warnings, preview/debug caveats, and the final ten-screenshot
  selection requirement.
- privacy-claim and source-availability preparation is recorded in
  `plans/20260709_nome_ios_privacy_claims_audit.md`.
- extension build display names and localized extension InfoPlist labels
  are Nome-facing: `Nome Notifications` and `Nome Share`.
- base extension `CFBundleName` still follows the existing target/product names
  (`SimpleX NSE` and `SimpleX SE`); changing it safely belongs to the later
  target/product/bundle-identifier migration, not this low-risk display pass.
- `scripts/ios/check-ios-release-identifiers.sh --compatibility-reviewed`
  passes for the current compatibility-first development pass because every
  retained upstream identifier is listed above as a documented compatibility
  exception.

Still open:

- real-core preflight still fails because production iOS core artifacts are not
  installed;
- no physical iPhone/TestFlight signing pass has been run;
- `scripts/ios/check-ios-release-identifiers.sh` without
  `--compatibility-reviewed` still fails by design. The strict final mode must
  pass before a Nome-owned public/TestFlight release track is considered ready.
- no independent Nome bundle/app-group/keychain migration has been designed;
- AGPL/source availability is prepared as a release packet but not complete
  without a public modified-source URL and final asset/trademark decision;
- final App Store screenshot packaging still needs caption/crop/export work,
  selection down to ten or fewer images, possible alpha flattening, and
  preview-core/debug-only screenshot replacement after real-core testing;
- App Store privacy/export-compliance claims still need final legal/product
  review after the real-core behavior is verified.

## Machine-readable release identity state

Added 2026-07-09:

```bash
scripts/ios/export-ios-release-identity-state.sh --output /tmp/nome-ios-release-identity-state-current --force
```

The export writes:

- `summary.tsv`;
- `gate_status.tsv`;
- `current_identifiers.tsv`;
- `proposed_identifiers.tsv`, including placeholder or candidate Nome-owned
  replacement values, update targets, migration dependencies, and verification
  commands;
- `required_decisions.tsv`, including manual QA anchor, checklist line,
  checklist section, and blocker group for each release decision;
- `next_actions.tsv`, including decision scope, evidence file, manual QA
  anchor, checklist line, checklist section, and blocker group for each release
  next action;
- per-gate logs under `logs/`.

The proposed identifier table can be checked without editing the project:

```bash
scripts/ios/check-ios-release-identity-proposal.sh \
  --proposal /tmp/nome-ios-release-identity-state-current/proposed_identifiers.tsv
```

The checker intentionally fails the default `com.example.nome` /
`nome.example` placeholder proposal. Re-run the export with candidate
release-owned values, for example:

```bash
scripts/ios/export-ios-release-identity-state.sh \
  --release-bundle-base <owned.reverse.dns.bundle.base> \
  --release-domain <owned-domain.example> \
  --output /tmp/nome-ios-release-identity-state-candidate \
  --force

scripts/ios/check-ios-release-identity-proposal.sh \
  --proposal /tmp/nome-ios-release-identity-state-candidate/proposed_identifiers.tsv
```

Passing this proposal checker only proves internal consistency and absence of
placeholder/upstream release identifiers. Apple Developer App ID availability,
App Group availability, associated-domain ownership/AASA hosting, provisioning,
and data/keychain migration still require external review.

Once those external decisions are approved, the checked proposal can be dry-run
or applied through the release migration script:

```bash
scripts/ios/apply-ios-release-identifiers.sh \
  --proposal /tmp/nome-ios-release-identity-state-candidate/proposed_identifiers.tsv \
  --output /tmp/nome-ios-release-identity-apply \
  --force

scripts/ios/apply-ios-release-identifiers.sh \
  --proposal /tmp/nome-ios-release-identity-state-candidate/proposed_identifiers.tsv \
  --output /tmp/nome-ios-release-identity-apply \
  --apply \
  --force
```

The first command is the default dry-run and does not edit the project. The
second command edits the Xcode project, app Info.plist, and entitlements, then
runs the strict release identifier gate. Do not run `--apply` until the Apple
Developer App IDs, App Group, keychain migration/reset behavior, and
associated-domain AASA hosting are approved.

Latest dry-run evidence:

- candidate export: `/tmp/nome-ios-release-identity-candidate-apply-20260710`;
- dry-run packet: `/tmp/nome-ios-release-identity-apply-dry-run-20260710`;
- dry-run candidate: `app.nome.secure` and `nome.chat`;
- result: 14 change rows and zero missing current-value rows, with no project
  files edited.

Latest evidence:

- direct export: `/tmp/nome-ios-release-identity-state-20260709-233510`;
- readiness-owned export:
  `/tmp/nome-ios-readiness-release-state-20260709-233643/release_identity_state`;
- compatibility-reviewed gate: PASS;
- strict final release gate: BLOCKED;
- current identifier rows: 21;
- required final decision rows: 8.

Required final decision rows:

1. main app bundle id;
2. notification/share extension bundle ids;
3. internal framework and test bundle ids;
4. App Group;
5. keychain access group;
6. associated domains and Nome-owned AASA files;
7. background task id;
8. `simplex` URL scheme compatibility.

This keeps the current state explicit: compatibility-reviewed development
passes are allowed, but a Nome-owned TestFlight/public release still needs a
single coordinated identifier/migration decision.

## Proposed identifier packet

Added 2026-07-10:

```bash
scripts/ios/export-ios-release-identity-state.sh \
  --release-bundle-base com.example.nome \
  --release-domain nome.example \
  --output /tmp/nome-ios-release-identity-state-current \
  --force
```

The default proposal values intentionally use `com.example.nome` and
`nome.example`; they are placeholders, not final Apple Developer or domain
ownership decisions. To test a real candidate without editing the project, pass
`--release-bundle-base` and `--release-domain`.

Latest evidence:

- direct export:
  `/tmp/nome-ios-release-identity-state-proposal-20260710-040437`;
- compatibility-reviewed gate: PASS;
- strict final release gate: BLOCKED;
- proposal status: `placeholder`;
- proposed identifier rows: 11.

The proposal rows map the current upstream-compatible values to the surfaces
that must move together for an independent Nome release track:

- main app bundle id;
- notification service extension bundle id;
- share extension bundle id;
- internal framework bundle id;
- iOS test bundle id;
- App Group;
- keychain access group;
- background task id;
- URL type name;
- `simplex` URL scheme compatibility;
- Associated Domains and Nome-owned AASA files.
