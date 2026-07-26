# Nome iOS brand-copy audit

Date: 2026-07-09

## Purpose

This audit classifies remaining `SimpleX` references in the iOS app so the Nome fork does not accidentally expose upstream product wording on primary branded surfaces while still preserving protocol compatibility, upstream attribution, and legal clarity.

## Current decision

Nome should replace SimpleX on normal product surfaces such as onboarding, home, settings labels, lock/privacy labels, notification placeholders, public contact address copy, and share/invite copy.

SimpleX should remain visible when it refers to:

- the upstream project, company, protocol, security audits, or open-source attribution;
- network/server compatibility such as SimpleX network, SimpleX Chat servers, SMP/XFTP behavior, and Flux-operated SimpleX network servers;
- link-type validation and developer controls where `SimpleX link` describes an actual compatibility format;
- copyright headers, Swift module/import names, bundle/project identifiers, and internal type names;
- legacy localization keys that are not currently used by the Nome source path.

## Current source findings

Primary Nome surfaces already changed:

- `SimpleX Lock` source paths now use `Nome Lock`.
- notification hidden preview placeholder now uses `Nome encrypted message or connection event`.
- public contact address source paths now use `Public contact address`, `public contact address`, or `Contact address`.
- invitation email source paths now use `Nome`.
- the embedded settings tab routes to dedicated `关于 Nome` and `帮助与反馈` pages.
- `WhatsNewView` short-address UI now uses `Short contact address` / `Contact address` instead of `Short SimpleX address` / `SimpleX address`.

Allowed SimpleX references still present in Swift source:

- `SimpleX network` / `SimpleX Chat` in the Nome about page and protocol compatibility explanation.
- `SimpleX Chat servers` in RTC/push/server settings where the operator/network is still upstream-compatible.
- `SimpleX link(s)` in group/developer/link-validation controls where the app is validating real SimpleX-compatible links.
- `Do NOT use SimpleX for emergency calls.` because the call feature remains the upstream SimpleX-compatible call stack.
- Trail of Bits/security audit copy in `WhatsNewView` and `SettingsView`.
- `SimpleX Chat` support/crowdfunding badge copy in `NameBadge` / support metadata.
- source comments, import statements, type names, project names, and copyright headers.

Legacy or non-primary paths:

- `ChatHelp` still contains upstream English SimpleX help text, but current Nome settings use the dedicated Chinese help page instead of this view.
- `OldHowItWorks` still contains upstream SimpleX explanation text, but current onboarding routes to `WhySimpleX`, which has Nome-branded Chinese copy.
- `WhatsNewView` contains SimpleX release text, but Nome first launch now suppresses the inherited upstream What's New sheet.

## Localization work done

New Simplified Chinese translations were added for current Nome keys:

- `Nome Lock`, lock enable/disable/authentication messages;
- `Nome encrypted message or connection event`;
- `Share to Nome`, `Share with Nome contacts`, and profile-update alerts;
- Nome invitation email subject/body;
- `Contact address`.
- legacy Simplified Chinese values for lock, share, invitation email, self-address warning, and short address were updated to Nome / public-contact-address wording so old keys do not leak outdated product language if they are reached.

## Automated gate

`scripts/ios/check-nome-brand-copy.sh` now checks the current safe-for-Nome
brand-copy boundary:

- forbidden Nome-facing Swift phrases such as `SimpleX Lock`, `Share to SimpleX`,
  `Let's talk in SimpleX Chat`, and `SimpleX address`;
- forbidden Simplified Chinese values such as `SimpleX 锁`, `分享到 SimpleX`,
  `通过 SimpleX Chat`, and `SimpleX 短地址`;
- `plutil` lint for the Simplified Chinese app and InfoPlist strings.

This gate is also included in `scripts/ios/check-nome-ios-readiness.sh`.

## Commands

Useful audit commands:

```bash
scripts/ios/check-nome-brand-copy.sh

rg -n "SimpleX" apps/ios/Shared --glob '*.swift' \
  | rg -v '^.*//|import SimpleXChat|Copyright|SimpleXAPI|SimpleXApp|SimpleXInfo'

rg -n '"(Nome|SimpleX).*"' apps/ios/zh-Hans.lproj/Localizable.strings

plutil -lint apps/ios/zh-Hans.lproj/Localizable.strings
```

## Remaining release gate

Before TestFlight, re-run this audit after real-core integration and after any new user-facing screens are added. At that point, decide whether to localize non-Chinese languages for Nome or restrict the first Nome test build to Chinese/English surfaces only.
