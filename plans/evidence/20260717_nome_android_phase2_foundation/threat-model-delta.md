# Threat-model delta — Phase 2 Android design foundation

## Protected assets

- official v6.5.6 core/database/protocol truth;
- local-auth and destructive-operation safety semantics;
- invitation/address/identity privacy wording;
- user data retained across the minSdk 28 same-package upgrade;
- separation between deterministic visual fixtures and production state;
- release APK surface and immutable Phase 0/1 evidence.

## New surface

The production-visible code surface is limited to Android-only presentation APIs: semantic tokens, Material 2 theme adapter, surface, button, seven-state panel, and accessibility modifiers. No production Activity, route, presenter, state adapter, parser, network operation, core command, database access, service, receiver, permission, deep link, or message transition is added.

The debug build adds one explicit `NomeFoundationActivity`, deterministic intent-derived fixture selection, Preview resources, and screenshot tests. It is `android:exported="false"`, excluded from recents, and absent from the release merged manifest.

## Threats and controls

| Threat | Control / evidence |
|---|---|
| Debug fixture leaks into release or becomes externally launchable | Debug source set only; activity non-exported; release merged manifest contains no harness entry; packaging instrumentation asserts presence/non-exported status in debug. |
| Fixture is mistaken for real core/network/security truth | Every screen visibly says “非实时 CORE / not live CORE”; fixture actions are no-op; no model/API/core dependency; real-core logs are recorded separately. |
| Intent extras inject arbitrary state/copy or unsafe values | Page/theme/locale/state parse through closed enums with safe defaults; font scale accepts only 1× or 2×; no URI, file, network, secret, or command payload is parsed. |
| A missing C capability is shown as success | Foundation only renders caller-supplied presentation state; no progress percentage, delivery, relay-health, E2EE, invitation-use, rollback, or remote-use fact is produced. |
| Local authentication becomes fail-open | P02 fixture explicitly says unavailable does not unlock; no authentication decision or production gate is changed. |
| Dark mode hides warnings or relies on color | Measured contrast passes; icon + localized text + stateDescription/live-region semantics accompany state colors. |
| 200% font hides state copy or recovery/destructive actions | Scroll-first layout plus device tests cover every panel state across both languages/themes at 2× and keep each test action visible/clickable and >=48dp; representative and seven-state native 200% contact sheets are retained. |
| minSdk change strands existing data | API 28 real-core cold start and API 35 same-package non-empty upgrade recorded; chat database/preferences exact at immediate checkpoint; agent DB runtime mutability disclosed. |
| Foundation bypasses official core wrapper or changes persistence semantics | No Haskell/native, `Core.kt`, database/protocol/message-state-machine source changed; no new native call path. |
| Screenshots retain personal/secret data | Deterministic synthetic names/copy only; no bearer link, address, passphrase, live chat, credential, or user account data is rendered. |

## Residual risk / next-batch boundary

- Production shell/routes and `CoreBacked`/`Derived`/`ClientOperation`/`Unavailable` adapters do not exist yet. Their threat models must cover deep links, permissions, lifecycle, real errors, destructive phases, and model reconciliation.
- The one-time locale marker is not implemented; existing-user language behavior must remain conservative when it is added.
- GAP-16 migration/archive safety corrections and any typed shared wrapper remain separate high-risk work.
- Release merged-manifest exclusion is proven; a later release-candidate batch should additionally inspect the final signed release APK/Dex/resources.
- Automated Compose semantics validate the TalkBack-facing contract, but later production pages still need device traversal/focus restoration checks in their real navigation context.
