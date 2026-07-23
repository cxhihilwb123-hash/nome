# Nome iOS Android-aligned home and contacts verification

> Superseded for typography and row-density acceptance by
> `../20260723_nome_ios_android_typography_alignment/RESULTS.md`.
> The screenshots in this folder preserve the first-pass layout for comparison.

Date: 2026-07-23
iOS branch: `codex/nome-ios-v656-integration`
iOS base HEAD before this uncommitted UI batch: `b4f822e55c3f9f363bc974556ec74ad512449b2a`
Android reference branch (read-only): `codex/nome-android-v656`
Android reference HEAD: `6a01efa5e6d10dc0743cdf82dfb69e09cc459862`

## Reference implementation

- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt`
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomePrimaryBottomNavigation.kt`
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeColorTokens.kt`
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeDimensionTokens.kt`
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeShapeTokens.kt`

Android was inspected only; no Android files were modified.

## Verified iOS behavior

- Home and contacts share the same page shell: brand/profile header, page title, subtitle, search, grouped rows, FAB, and bottom navigation.
- Home includes the Android-style list filter surface with the selected `全部` chip, chat count, and add-list action.
- Contacts omits home-only list filters and uses the `搜索联系人` prompt.
- Conversation rows use one grouped low-elevation surface with internal dividers.
- Bottom navigation is full width and 64 points high, with a 32 x 3 point selected indicator.
- The 56 point new-connection FAB clears the bottom navigation and remains reachable.
- Light and dark palettes match the current Android Nome tokens.
- The header mark is rendered with a transparent SwiftUI shape so the dark theme has no rectangular image background.

## Build verification

Command:

```sh
env DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  xcodebuild \
  -project apps/ios/SimpleX.xcodeproj \
  -scheme 'SimpleX (iOS)' \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /private/tmp/nome-ios-ui-derived \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=YES \
  build
```

Result: `** BUILD SUCCEEDED **`

Simulator: `Nome RealCore Lab v655` (`539249D8-761C-4DB0-966C-75E083869D91`)
Bundle: `chat.simplex.app`

## Screenshot evidence

| Page | Light | Dark |
| --- | --- | --- |
| Home | `home-light.png` | `home-dark.png` |
| Contacts | `contacts-light.png` | `contacts-dark.png` |

SHA-256:

```text
cad017be03ec14d16027018b713b2d62440b94e778a980bad3b0ef68c384f583  contacts-dark.png
d6ac47e76976ad881f05fe912bb8adba536d1c8611cbfd3e4d5c5718ca3bb232  contacts-light.png
6b12f197fab70cadf1c9604508ee980de7caf3734389c66f98efadd2e0e421ea  home-dark.png
866f7012f3e79931dcc06f3306b87cc83b0219bdcc9c1e7f0ff6a18ca26c9860  home-light.png
```
