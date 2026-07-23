# Changeset and design record

## Approved visual target

- Source design: `/Users/forkman03/.codex/generated_images/019f8cd4-670e-7ed2-a38e-bc1e5f1dbb11/call_NNeINX4nUFQSXhoBbME4GyUQ.png`
- User-selected direction: the third card/list composition with the first compact search-field treatment.
- Side-by-side evidence: `rendered-screens/design-comparison-source-vs-implementation-en-light.png`

## Behavior before and after

| Area | Before | After |
| --- | --- | --- |
| Home/Contacts/list composition | Independently styled controls and flat rows; custom lists were not presented as a polished first-class surface. | One shared hierarchy with page title/subtitle, compact search, contained list shelf, grouped row card, FAB, and primary nav. |
| Custom list | Existing official assignment/editing behavior existed, but discovery and selection were visually weak. | Home exposes All/user lists/Add list; selected list exposes Manage list and its existing filtered conversations. |
| Bottom nav | Shared component existed from Batch 01 but used an icon pill; physical semantics and OEM inset behavior were inconsistent between the Vivo builds. | One 76dp component with a green top indicator, consistent color/state, full-item semantics, and Compose/legacy inset reconciliation. The final measured item is 76.19dp on V2048A and 76.0dp on V2047A. |
| List editor Back | On a Vivo, Android Back could background the app while leaving the editor overlay alive. | Android `BackHandler` invokes the existing close callback for the list editor and list surface. |
| Header lockup | 84dp render was visually small; theme/resource-qualifier mismatch could select the light asset in Compose dark theme. | 104dp render, preserved 700:285 ratio, explicit Material-theme selection of the approved dark asset. |
| Launcher icon | The Nome mark appeared too full inside the adaptive mask. | Foreground mark is centered at 60dp high on the 108dp adaptive foreground canvas. |

## UI/navigation source files

- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt`
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomePrimaryBottomNavigation.kt`
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/components/NomeBrandLockup.kt`
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/views/chatlist/PlatformTagEditorRoute.android.kt`
- `apps/multiplatform/common/src/androidMain/res/values/nome_home_strings.xml`
- `apps/multiplatform/common/src/androidMain/res/values-zh-rCN/nome_home_strings.xml`

## Test/debug evidence source files

- `apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeComposeTest.kt`
- `apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeBatch03ScreenshotTest.kt`
- `apps/multiplatform/android/src/debug/java/chat/simplex/app/nome/home/NomeHomeEvidenceActivity.kt`

The debug activity uses synthetic, non-sensitive fixtures and production components. Its `DEBUG PRODUCTION RENDERER` strip is evidence-only and is not part of the production Home route.

## Vivo bottom-inset compatibility

- V2048A reports a real bottom navigation-bar inset; the shared component consumes the maximum safe bottom reported by Compose or the root window.
- V2047A's legacy window manager crops the Compose root by a status-bar-sized decor area while reporting a zero bottom inset. Only in that zero-bottom case, the shared component uses the safe top inset as the OEM decor compensation.
- The 76dp navigation content remains aligned to the top of the safe container on both devices. This avoids both a shortened touch area and a double inset.
- Final real UI bounds: V2048A `[42,1952][374,2152]` (200px / 2.625 = 76.19dp); V2047A `[48,2049][376,2277]` (228px / 3.0 = 76.0dp).

## Header brand source

- Light authoritative repository asset: `apps/multiplatform/common/src/androidMain/res/drawable-nodpi/nome_header_logo.png`
- Approved dark repository asset: `apps/multiplatform/common/src/androidMain/res/drawable-night-nodpi/nome_header_logo.png`
- Explicit dark-theme alias: `apps/multiplatform/common/src/androidMain/res/drawable-nodpi/nome_header_logo_dark.png`
- Canvas: 700x285 RGBA; light visible bounds 591x183; dark visible bounds 592x184.
- Dark alias SHA256 equals the approved night asset: `4b346151e26cc03d48c0c8d8d99957e1ab3d8514afb150dc50d4ba379ed863e9`.
- Runtime: 104dp wide, `ContentScale.Fit`, fixed 700:285 aspect ratio.

No logo was drawn, guessed, or downloaded. The alias is a byte-for-byte copy of the existing approved dark asset.

## Launcher mark source and changed resources

- Approved source: `/Users/forkman03/project/nome/design/design/product/nome-mark-transparent.png`
- Source: 310x320 RGBA, transparent background, SHA256 `621e68b971c6e8b71509db06184b5198c5710478e23d9c4713f9318bce1b709c`.
- Dark mark was derived from the existing approved dark-blue foreground by removing only its `#0E1B2D` backing before centering/resizing.
- Changed foregrounds:
  - `apps/multiplatform/android/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/icon_foreground.png`
  - `apps/multiplatform/android/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/icon_dark_blue_foreground.png`
- Appearance previews were synchronized:
  - `apps/multiplatform/common/src/androidMain/res/drawable/icon_foreground_android_common.png`
  - `apps/multiplatform/common/src/androidMain/res/drawable/icon_foreground_android_common_dark.png`

No manifest alias, adaptive-icon XML, background color, round-icon alias, monochrome asset, or legacy icon behavior was changed.

## Scope boundary

This batch layers onto the preserved Batch 01/02 dirty tree. The historic debug-manifest modification and all prior frozen evidence remain untouched. The large aggregate Git diff is not evidence of a broad new refactor; this record identifies the files intentionally changed for Batch 03.
