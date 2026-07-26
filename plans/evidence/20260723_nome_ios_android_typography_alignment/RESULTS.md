# Nome iOS Android typography and layout alignment

Date: 2026-07-23
iOS branch: `codex/nome-ios-v656-integration`
Android reference branch (read-only): `codex/nome-android-v656`

## Audit scope

The home and contacts pages were compared against the current Android Nome
implementation and its light-mode reference screenshots. Android is shown on
the left and revised iOS on the right in the side-by-side images.

Android was inspected only. No Android source was modified.

## Alignment corrections

| Element | Android token/layout | Revised iOS |
| --- | --- | --- |
| Page title | 28sp bold, 34sp line | 28pt bold |
| Subtitle/search | 16sp body | 16pt body |
| Section/filter label | 14sp medium | 14pt medium |
| Conversation title | 16sp medium | 16pt medium |
| Conversation preview/time | 13sp regular | 13pt regular |
| Conversation row | 72dp minimum | 72pt minimum |
| Conversation avatar | 44dp | 44pt |
| Search icon | 20dp | 20pt |
| Bottom navigation icon/label | 24dp / 13sp | 24pt / 13pt |

- The title block was moved upward by reducing the first-pass header insets.
- The contacts section no longer uses an iOS `Section` header that introduced
  extra vertical space.
- `全部` and the conversation count are now separate, matching Android.
- Conversation previews are one line and use Android-equivalent density.
- Compact row metrics use `@ScaledMetric` so larger accessibility text settings
  can expand the row instead of being clipped at the default 72-point height.

## Step health

1. Home typography and layout: healthy at the captured default text size.
2. Contacts typography and layout: healthy at the captured default text size.
3. Light/dark rendering: healthy in the captured simulator states.
4. Physical iPhone rendering: healthy in Personal Team build 338.

## Build verification

```sh
/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild \
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

Simulator: `Nome RealCore Lab v655`
(`539249D8-761C-4DB0-966C-75E083869D91`)
Bundle: `chat.simplex.app`

## Physical-device follow-up

The same three UI source files were copied byte-for-byte into the isolated
Personal Team signing worktree, built as Nome `6.5.6 (338)`, and overwrite
installed on a physical iPhone. The revised home and contacts layout rendered
correctly with retained production-like chat data. Installation, launch,
bidirectional communication, cold relaunch, and foreground/background
transitions passed in the main-app-only lane.

See `../20260723_nome_ios_physical_device/RESULTS.md` for the verified matrix and
the Personal Team limitations around Push, Notification Service Extension,
Share Extension, App Group, and camera use through iPhone Mirroring.

## Evidence

- `home-side-by-side.png`
- `contacts-side-by-side.png`
- `ios-home-revised.png`
- `ios-contacts-revised.png`
- `ios-home-revised-dark.png`
- `ios-contacts-revised-dark.png`

SHA-256:

```text
7107f85dc4819d0019290911e5fbb64d287ae47e7a161b8754559d35b3ad8a40  contacts-side-by-side.png
f5c10803d09c486017da85a3f7f5ed219671de2d8987ec9d0cb618a8e839be1b  home-side-by-side.png
b646d162f862739f2ab23071eff3459ae9db61713e80c33f195a62088607469b  ios-contacts-revised-dark.png
6c8ffe1a7fe5082d69eb6065a0135e6f5f5f81ec8beeb7ee5d47bac3c29dfeed  ios-contacts-revised.png
52808a874f5a19e1eeab35b53acdba9b05d4d4ba0dc92a0a75c157178ce7c09e  ios-home-revised-dark.png
d8e39a6d8af20fca034025358a33a46d57955649b37611e8ce3707dc764ef0a3  ios-home-revised.png
```

## Evidence limits

The comparison proves default-size visual alignment, light/dark rendering, and
the captured physical-device safe-area behavior. VoiceOver reading order and
the largest accessibility text sizes still require interactive checks.
