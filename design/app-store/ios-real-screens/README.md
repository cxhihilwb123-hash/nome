# Nome iOS real-screen screenshot candidates

Date: 2026-07-09

These PNGs are copied from live iOS Simulator screenshots captured during the Nome native app QA pass. They are not generated mockups.

They are a stable candidate set for App Store screenshot planning, not the final submission package. Final App Store assets still need a packaging pass for captions, crops, device sets, and any store-specific export sizes.

The detailed candidate manifest is saved in `MANIFEST.md`. The repeatable
checker is:

```bash
scripts/ios/check-app-store-screenshots.sh
```

Use strict mode for a final upload package:

```bash
scripts/ios/check-app-store-screenshots.sh --final
```

Generate the current 10-slot flattened JPEG draft package:

```bash
scripts/ios/export-app-store-screenshot-draft.sh --force
```

The generated draft is saved in `design/app-store/ios-upload-draft-screens/`.
It intentionally keeps `needs-real-core` in filenames that still depend on
preview-core or debug-preview evidence, so strict final mode continues to fail
until those screens are replaced.

## Candidate set

| File | Source screenshot | Status |
| --- | --- | --- |
| `01-onboarding-welcome.png` | `/tmp/nome-onboarding-welcome.png` | Real simulator screen from the debug onboarding preview path. Good visual candidate. |
| `02-onboarding-local-identity.png` | `/tmp/nome-onboarding-profile.png` | Real simulator screen from the debug onboarding preview path. Good visual candidate. |
| `03-home.png` | `/tmp/nome-normal-launch-after-brand-copy.png` | Real simulator home screen after the Nome branding and bottom-tab pass. Good visual candidate. |
| `04-contacts.png` | `/tmp/nome-contacts-tab.png` | Real simulator contacts tab. Good visual candidate. |
| `05-public-contact-preview-core.png` | `/tmp/nome-public-contact-v2-final.png` | Real simulator screen, but preview-core only because it shows `https://nome.local/preview`. Replace with a real public address before public release. |
| `06-join-group-preview.png` | `/tmp/nome-join-group-v2-page-clean.png` | Real simulator join-group screen. QR scan and real invitation acceptance still need real-core testing. |
| `07-conversation-debug-preview.png` | `/tmp/nome-conversation-preview-v4-background.png` | Real simulator screen from the debug seeded conversation preview. Replace with a real two-account conversation screenshot before claiming real messaging. |
| `08-settings.png` | `/tmp/nome-settings-tab-v3-final.png` | Real simulator settings tab. Good visual candidate. |
| `09-about.png` | `/tmp/nome-about-page-cn.png` | Real simulator about screen. Good supporting candidate. |
| `10-help.png` | `/tmp/nome-help-page-cn.png` | Real simulator help screen. Good supporting candidate. |
| `11-add-friend-preview-core.png` | `/tmp/nome-add-friend-redesign-01-page.png` | Real simulator add-friend screen. Preview-core only: it shows the complete one-time link layout with a readiness warning instead of a real generated link. |

## Dimensions and count

All current candidate images are `1206x2622` PNG screenshots. This is an
accepted iPhone 6.3" portrait screenshot size in Apple's App Store Connect
screenshot specification.

The current folder contains eleven PNG candidates. Apple limits an App Store
Connect localization to ten screenshots, so the final public store set must
select ten or fewer images.

Verification command:

```bash
scripts/ios/check-app-store-screenshots.sh
sips -g pixelWidth -g pixelHeight -g hasAlpha design/app-store/ios-real-screens/*.png
shasum -a 256 design/app-store/ios-real-screens/*.png
```

## Release caveat

This folder satisfies the gate that the screenshot candidates are real app screens rather than generated design mockups. It does not complete the full App Store media task. The final public set should replace preview-core and debug-only candidates after real iOS core libraries, real account creation, and two-account messaging have been verified.
