# Nome iOS screenshot candidate manifest

Date: 2026-07-09

Apple reference: https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/

This manifest records the current real-screen screenshot candidates for Nome iOS.
The files in this directory are planning candidates copied from iOS Simulator
screenshots, not generated product mockups and not the final App Store Connect
upload package.

## Store rules that affect this folder

- App Store Connect accepts one to ten screenshots per localization, in `.jpeg`,
  `.jpg`, or `.png` format.
- The current files are `1206x2622` PNGs, which matches an accepted iPhone 6.3"
  portrait screenshot size in Apple's screenshot specification.
- This directory currently has eleven candidate PNGs, so a final public store set
  must remove or replace at least one image before upload.
- Current PNGs have alpha channels. Keep them as captured evidence, but flatten
  final store exports unless App Store Connect upload testing proves otherwise.
- Preview-core or debug-only screens are useful for product planning, but they
  must be replaced before claiming real invitation, group, public-address, or
  messaging behavior.

## Candidate inventory

| File | Dimensions | Alpha | SHA-256 | Current status | Final-release requirement |
| --- | --- | --- | --- | --- | --- |
| `01-onboarding-welcome.png` | `1206x2622` | yes | `41d4b6ea474ce22c2d9a9b24d686d24fc82180197ad8282cf0f2d70b4cd77649` | Real simulator screen from debug onboarding preview. | Good visual candidate; re-capture from final build if copy changes. |
| `02-onboarding-local-identity.png` | `1206x2622` | yes | `c3b27c19d52a59ae65c311c4e0c6930a908e1fba954378254493f2a5ef212e2b` | Real simulator screen from debug onboarding preview. | Good visual candidate; re-capture from final build if copy changes. |
| `03-home.png` | `1206x2622` | yes | `92b312b4d6dd482a6c9a93f030ea6a8c3167001ce073b4ea721b98437561f7da` | Real simulator Nome home screen. | Good visual candidate after final home copy/build check. |
| `04-contacts.png` | `1206x2622` | yes | `a7e34a0c7fe8d596d29638b4517eac726edd62610985b2a391d3515b6bf3035f` | Real simulator contacts tab. | Good visual candidate after real contacts state check. |
| `05-public-contact-preview-core.png` | `1206x2622` | yes | `7aa0eaf37e959a8f1c5893315f89babfa9e6596f667917909600516fb738a62e` | Real simulator screen, but preview-core address state. | Replace with a real public contact address screenshot after real core is installed. |
| `06-join-group-preview.png` | `1206x2622` | yes | `fae05728b00a01193b6960c38841c88155345b8c087a6c314665bc56691dc377` | Real simulator join-group UI. | Replace or re-capture after a real group invitation preview/join test. |
| `07-conversation-debug-preview.png` | `1206x2622` | yes | `7b944636dcb056f5a5f6c65e7b434d1e353d096fbc74c56354d6153675de5203` | Debug seeded conversation preview. | Replace with a real two-account conversation screenshot. |
| `08-settings.png` | `1206x2622` | yes | `613bc86ca41635023b28ec65acdca0e8842eeba48c33f046b79eb0e7472037ca` | Real simulator settings tab. | Good visual candidate after final settings copy/build check. |
| `09-about.png` | `1206x2622` | yes | `0d421169f5a7ce2174d00f0ed4db4fbea2036883153cd077c19ae8cbb1b99966` | Real simulator about screen. | Useful support evidence; lower priority for the public 10-slot set. |
| `10-help.png` | `1206x2622` | yes | `2b05e44493a67d9e3952a1a644e24e42cbbdd2a0499c5f2f951fdcba006d90de` | Real simulator help screen. | Useful support candidate if the store set needs a trust/help page. |
| `11-add-friend-preview-core.png` | `1206x2622` | yes | `f36e80b06b4e3f3fe71ff63aa2c8ed701b3d52107b489cd256204aa9a68ec94d` | Real simulator add-friend UI, but preview-core link state. | Replace with a real one-time link and QR screenshot after real core is installed. |

## Current 10-slot planning set

Use this order only as a planning baseline:

1. `01-onboarding-welcome.png`
2. `02-onboarding-local-identity.png`
3. `03-home.png`
4. `04-contacts.png`
5. `11-add-friend-preview-core.png`
6. `05-public-contact-preview-core.png`
7. `06-join-group-preview.png`
8. `07-conversation-debug-preview.png`
9. `08-settings.png`
10. `10-help.png`

This drops `09-about.png` because App Store listings usually need workflow and
trust screens more than an internal about page. The final public set must be
revisited after real-core testing; at minimum, add-friend, public-contact,
join-group, and conversation candidates need real-core replacements.

## Verification

Candidate check:

```bash
scripts/ios/check-app-store-screenshots.sh
```

Strict final-package check:

```bash
scripts/ios/check-app-store-screenshots.sh --final
```

Manual dimension/hash evidence:

```bash
sips -g pixelWidth -g pixelHeight -g hasAlpha design/app-store/ios-real-screens/*.png
shasum -a 256 design/app-store/ios-real-screens/*.png
```
