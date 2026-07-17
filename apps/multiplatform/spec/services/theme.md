# Theme Engine

## Table of Contents

1. [Overview](#1-overview)
2. [ThemeManager](#2-thememanager)
3. [Default Themes](#3-default-themes)
4. [Theme Types](#4-theme-types)
5. [Color System](#5-color-system)
6. [SimpleXTheme Composable](#6-simplextheme-composable)
7. [Platform Theme](#7-platform-theme)
8. [YAML Import/Export](#8-yaml-importexport)
9. [Source Files](#9-source-files)
10. [Nome Android Home Theme Boundary](#10-nome-android-home-theme-boundary)

## Executive Summary

The SimpleX Chat theme engine implements a four-level cascade: per-chat theme overrides take precedence over per-user overrides, which take precedence over global (app-settings) overrides, which take precedence over built-in presets. Four preset themes exist (LIGHT, DARK, SIMPLEX, BLACK), each defining a Material `Colors` palette and custom `AppColors` for chat-specific elements. Themes support wallpaper customization (preset patterns or custom images) with background and tint color overrides. Theme configuration is persisted as YAML and can be imported/exported. The `SimpleXTheme` composable wraps `MaterialTheme` with additional `CompositionLocal` providers for app colors and wallpaper.

---

## 1. Overview

Theme resolution follows a priority chain:

```
per-chat override > per-user override > global override > preset default
```

At each level, individual color properties can be overridden. Unspecified properties fall through to the next level. The resolution is performed by `ThemeManager.currentColors()`, which merges all levels into a single `ActiveTheme` containing Material `Colors`, `AppColors`, and `AppWallpaper`.

Wallpapers follow the same cascade, with additional support for preset wallpapers (built-in patterns like `SCHOOL`) and custom images. Wallpaper presets can define their own color overrides that sit between the global override and the base preset.

---

## 2. ThemeManager

[`ThemeManager.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt) (241 lines)

A singleton `object` that manages theme state, persistence, and resolution.

### Core resolution

<a id="currentColors"></a>

[`currentColors()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L57-L90):

```kotlin
fun currentColors(
  themeOverridesForType: WallpaperType?,
  perChatTheme: ThemeModeOverride?,
  perUserTheme: ThemeModeOverrides?,
  appSettingsTheme: List<ThemeOverrides>
): ActiveTheme
```

This is the core resolution function. It:
1. Determines the non-system theme name (resolving `SYSTEM` to light or dark based on `systemInDarkThemeCurrently`).
2. Selects the base theme palette (LIGHT/DARK/SIMPLEX/BLACK).
3. Finds the matching `ThemeOverrides` from `appSettingsTheme` based on wallpaper type and theme name.
4. Selects the `perUserTheme` for the current light/dark mode.
5. Resolves wallpaper preset colors if applicable.
6. Merges all color layers via `toColors()`, `toAppColors()`, and `toAppWallpaper()`.

Returns `ActiveTheme(name, base, colors, appColors, wallpaper)`.

### Theme application

[`applyTheme()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L105-L113):

Persists the theme name, recalculates `CurrentColors`, and updates Android system bar appearance:

```kotlin
fun applyTheme(theme: String) {
  if (appPrefs.currentTheme.get() != theme) {
    appPrefs.currentTheme.set(theme)
  }
  CurrentColors.value = currentColors(null, null, chatModel.currentUser.value?.uiThemes, appPrefs.themeOverrides.get())
  platform.androidSetNightModeIfSupported()
  val c = CurrentColors.value.colors
  platform.androidSetStatusAndNavigationBarAppearance(c.isLight, c.isLight)
}
```

[`changeDarkTheme()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L115-L118):

Sets the dark mode variant (DARK, SIMPLEX, or BLACK) and recalculates colors.

### Color and wallpaper modification

[`saveAndApplyThemeColor()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L120-L130):

Persists a single color change to the global theme overrides:
1. Gets or creates `ThemeOverrides` for the current base theme.
2. Calls `withUpdatedColor()` to update the specific `ThemeColor`.
3. Updates `currentThemeIds` mapping.
4. Recalculates `CurrentColors`.

[`applyThemeColor()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L132-L134):

In-memory-only color change (for per-chat/per-user theme editing before save).

[`saveAndApplyWallpaper()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L136):

Persists wallpaper type change. Finds or creates matching `ThemeOverrides` (matching by wallpaper type + theme name), updates the wallpaper, and persists.

### Reset

[`resetAllThemeColors()` (global)](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L204-L211):

Resets all custom colors in the current global theme override to defaults. Preserves wallpaper but clears its background and tint overrides.

[`resetAllThemeColors()` (per-chat/per-user)](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L213-L216):

In-memory reset of a `ThemeModeOverride` state.

### Import/Export

[`saveAndApplyThemeOverrides()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L188-L202):

Imports a complete `ThemeOverrides` (from YAML). Handles wallpaper image import (base64 to file), replaces existing override for the same type, and applies.

[`currentThemeOverridesForExport()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L92-L103):

Exports the fully resolved current theme as a `ThemeOverrides` with all colors filled and wallpaper image embedded as base64.

### Utility

[`colorFromReadableHex()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L224-L225):

Parses `#AARRGGBB` hex string to `Color`.

[`toReadableHex()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt#L227-L240):

Converts `Color` to `#AARRGGBB` hex string with intelligent alpha handling.

---

<a id="DefaultTheme"></a>

## 3. Default Themes

[`DefaultTheme`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L26-L44):

```kotlin
enum class DefaultTheme {
  LIGHT, DARK, SIMPLEX, BLACK;

  companion object {
    const val SYSTEM_THEME_NAME: String = "SYSTEM"
  }
}
```

| Theme | `mode` | Description |
|---|---|---|
| `LIGHT` | LIGHT | Standard light theme with white/light gray surfaces |
| `DARK` | DARK | Standard dark theme with dark gray surfaces |
| `SIMPLEX` | DARK | SimpleX branded dark theme with deep blue background and cyan accent |
| `BLACK` | DARK | AMOLED-optimized pure black theme |

`SYSTEM` is a virtual theme name that resolves to LIGHT or the configured dark variant at runtime.

[`DefaultThemeMode`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L46-L49): `LIGHT` or `DARK`, serialized as `"light"` / `"dark"`.

---

## 4. Theme Types

<a id="AppColors"></a>

### AppColors

[`AppColors`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L53):

```kotlin
@Stable
class AppColors(
  title: Color,
  primaryVariant2: Color,
  sentMessage: Color,
  sentQuote: Color,
  receivedMessage: Color,
  receivedQuote: Color,
)
```

Mutable state properties (for efficient recomposition) representing chat-specific colors not covered by Material's `Colors`.

<a id="AppWallpaper"></a>

### AppWallpaper

[`AppWallpaper`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L106):

```kotlin
@Stable
class AppWallpaper(
  background: Color? = null,
  tint: Color? = null,
  type: WallpaperType = WallpaperType.Empty,
)
```

Represents the active wallpaper state with optional background color, tint overlay, and wallpaper type (Empty, Preset, or Image).

<a id="ThemeColor"></a>

### ThemeColor

Enum of all customizable color slots:

`PRIMARY`, `PRIMARY_VARIANT`, `SECONDARY`, `SECONDARY_VARIANT`, `BACKGROUND`, `SURFACE`, `TITLE`, `SENT_MESSAGE`, `SENT_QUOTE`, `RECEIVED_MESSAGE`, `RECEIVED_QUOTE`, `PRIMARY_VARIANT2`, `WALLPAPER_BACKGROUND`, `WALLPAPER_TINT`

Each has a `fromColors()` method to extract the current value and a `text` property for UI display.

**Location:** [`ThemeColor`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L140)

<a id="ThemeColors"></a>

### ThemeColors

[`ThemeColors`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L183):

Serializable data class with optional hex color strings for each slot. Uses `@SerialName` annotations for YAML compatibility (`accent` for `primary`, `accentVariant` for `primaryVariant`, `menus` for `surface`, etc.).

<a id="ThemeWallpaper"></a>

### ThemeWallpaper

[`ThemeWallpaper`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L224):

```kotlin
@Serializable
data class ThemeWallpaper(
  val preset: String? = null,       // Preset wallpaper name
  val scale: Float? = null,         // Wallpaper scale factor
  val scaleType: WallpaperScaleType? = null,  // Fill/fit mode
  val background: String? = null,   // Background color hex
  val tint: String? = null,         // Tint overlay color hex
  val image: String? = null,        // Base64-encoded image (for import/export)
  val imageFile: String? = null,    // Local image file name
)
```

Key methods:
- `toAppWallpaper()`: Converts to runtime `AppWallpaper`.
- `withFilledWallpaperBase64()`: Embeds the image as base64 for export.
- `importFromString()`: Saves a base64 image to disk and returns a copy with `imageFile` set.
- `from(type, background, tint)`: Factory from `WallpaperType`.

<a id="ThemeOverrides"></a>

### ThemeOverrides

[`ThemeOverrides`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L304):

```kotlin
@Serializable
data class ThemeOverrides(
  val themeId: String = UUID.randomUUID().toString(),
  val base: DefaultTheme,
  val colors: ThemeColors = ThemeColors(),
  val wallpaper: ThemeWallpaper? = null,
)
```

A complete theme override entry. Multiple can coexist (one per wallpaper type per base theme). The `themeId` is a UUID for identity tracking. Key methods:
- `isSame(type, themeName)`: Matches by wallpaper type and base theme.
- `withUpdatedColor(name, color)`: Returns a copy with one color changed.
- `toColors()`, `toAppColors()`, `toAppWallpaper()`: Merge with base theme and per-user/per-chat overrides.

<a id="ThemeModeOverrides"></a>

### ThemeModeOverrides

[`ThemeModeOverrides`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L475):

```kotlin
@Serializable
data class ThemeModeOverrides(
  val light: ThemeModeOverride? = null,
  val dark: ThemeModeOverride? = null,
)
```

Container for per-user or per-chat overrides, with separate light and dark mode variants. Stored on the `User` model as `uiThemes`.

<a id="ThemeModeOverride"></a>

### ThemeModeOverride

[`ThemeModeOverride`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L487):

```kotlin
@Serializable
data class ThemeModeOverride(
  val mode: DefaultThemeMode = CurrentColors.value.base.mode,
  val colors: ThemeColors = ThemeColors(),
  val wallpaper: ThemeWallpaper? = null,
)
```

A single mode's override with colors and wallpaper. Has `withUpdatedColor()` and `removeSameColors()` (strips colors that match base defaults).

---

## 5. Color System

Four built-in color palettes, each consisting of a Material `Colors` and an `AppColors`:

### [DarkColorPalette](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L635)

| Property | Value | Notes |
|---|---|---|
| `primary` | `SimplexBlue` | `#0088ff` |
| `surface` | `#222222` | |
| `sentMessage` | `#18262E` | Dark blue-gray |
| `receivedMessage` | `#262627` | Neutral dark |

### [LightColorPalette](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L657)

| Property | Value | Notes |
|---|---|---|
| `primary` | `SimplexBlue` | `#0088ff` |
| `surface` | `White` | |
| `sentMessage` | `#E9F7FF` | Light blue |
| `receivedMessage` | `#F5F5F6` | Near-white |

### [SimplexColorPalette](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L679)

| Property | Value | Notes |
|---|---|---|
| `primary` | `#70F0F9` | Cyan |
| `primaryVariant` | `#1298A5` | Dark cyan |
| `background` | `#111528` | Deep navy |
| `surface` | `#121C37` | Dark navy |
| `title` | `#267BE5` | Blue |

### [BlackColorPalette](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L702)

| Property | Value | Notes |
|---|---|---|
| `primary` | `#0077E0` | Darker blue |
| `background` | `#070707` | Near-black |
| `surface` | `#161617` | Very dark |
| `sentMessage` | `#18262E` | Same as Dark |
| `receivedMessage` | `#1B1B1B` | Very dark |

---

<a id="SimpleXTheme"></a>

## 6. SimpleXTheme Composable

[`SimpleXTheme()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L774-L823):

```kotlin
@Composable
fun SimpleXTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit)
```

The root theme composable that wraps all app content:

1. **System dark mode tracking** ([`systemDark` collection](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L782-L789)): Uses `snapshotFlow` on `isSystemInDarkTheme()` to call `reactOnDarkThemeChanges()` when the system theme changes. This triggers `ThemeManager.applyTheme(SYSTEM)` if the app is in system theme mode.

2. **User theme tracking** ([`CurrentColors` and user-theme collection](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L790-L797)): Monitors `chatModel.currentUser.value?.uiThemes` and re-applies the theme when the active user changes.

3. **MaterialTheme wrapping** ([`MaterialTheme` and locals](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L798-L822)): Provides `theme.colors` to `MaterialTheme`, plus custom `CompositionLocal` providers:
   - `LocalContentColor` -- set to `MaterialTheme.colors.onBackground`
   - `LocalAppColors` -- the `AppColors` instance (remembered and updated)
   - `LocalAppWallpaper` -- the `AppWallpaper` instance (remembered and updated)
   - `LocalDensity` -- scaled by `desktopDensityScaleMultiplier` and `fontSizeMultiplier`

4. [`SimpleXThemeOverride`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L826): A variant that accepts an explicit `ActiveTheme` for per-chat theme previews and overlays.

### CompositionLocal access

```kotlin
val MaterialTheme.appColors: AppColors    // via LocalAppColors
val MaterialTheme.wallpaper: AppWallpaper // via LocalAppWallpaper
```

### Global state

<a id="CurrentColors"></a>

[`CurrentColors`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L728): A `MutableStateFlow<ActiveTheme>` that holds the current resolved theme. Updated by `ThemeManager.applyTheme()` and collected by `SimpleXTheme`.

[`systemInDarkThemeCurrently`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L725): Tracks the current system dark mode state.

---

## 7. Platform Theme

### isSystemInDarkTheme

**Android** ([`Theme.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/theme/Theme.android.kt)):

```kotlin
@Composable
actual fun isSystemInDarkTheme(): Boolean = androidx.compose.foundation.isSystemInDarkTheme()
```

Delegates to the standard Compose function which reads `Configuration.uiMode`.

**Desktop** ([`Theme.desktop.kt`](../../common/src/desktopMain/kotlin/chat/simplex/common/ui/theme/Theme.desktop.kt)):

```kotlin
private val detector: OsThemeDetector = OsThemeDetector.getDetector()
  .apply { registerListener(::reactOnDarkThemeChanges) }

@Composable
actual fun isSystemInDarkTheme(): Boolean = try {
  detector.isDark
} catch (e: Exception) {
  false  // Fallback for macOS exceptions
}
```

Uses the [jSystemThemeDetector](https://github.com/Dansoftowner/jSystemThemeDetector) library (`OsThemeDetector`). The detector also registers a listener that calls `reactOnDarkThemeChanges()` proactively when the OS theme changes, ensuring the app responds even outside of composition.

### reactOnDarkThemeChanges

[`reactOnDarkThemeChanges()`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt#L764-L770):

```kotlin
fun reactOnDarkThemeChanges(isDark: Boolean) {
  systemInDarkThemeCurrently = isDark
  if (appPrefs.currentTheme.get() == DefaultTheme.SYSTEM_THEME_NAME
      && CurrentColors.value.colors.isLight == isDark) {
    ThemeManager.applyTheme(DefaultTheme.SYSTEM_THEME_NAME)
  }
}
```

Only triggers a theme switch if the app is in SYSTEM mode and the current light/dark state disagrees with the OS.

---

## 8. YAML Import/Export

Theme overrides are persisted in `themes.yaml` (located in `preferencesDir`).

### readThemeOverrides

[`readThemeOverrides()`](../../common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt#L127-L149):

```kotlin
fun readThemeOverrides(): List<ThemeOverrides>
```

1. Reads `themes.yaml` from `preferencesDir`.
2. Parses the YAML node tree.
3. Extracts the `themes` list.
4. Deserializes each entry as `ThemeOverrides`, skipping entries that fail to parse (with error logging).
5. Calls `skipDuplicates()` to remove entries with the same type+base combination.

### writeThemeOverrides

[`writeThemeOverrides()`](../../common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt#L153-L168):

```kotlin
fun writeThemeOverrides(overrides: List<ThemeOverrides>): Boolean
```

1. Serializes `ThemesFile(themes = overrides)` to YAML string.
2. Writes to a temporary file in `preferencesTmpDir`.
3. Atomically moves the temp file to `themes.yaml` using `Files.move` with `REPLACE_EXISTING`.
4. Thread-safe via `synchronized(lock)`.

### YAML format

```yaml
themes:
  - themeId: "uuid-string"
    base: "LIGHT"
    colors:
      accent: "#ff0088ff"
      background: "#ffffffff"
      sentMessage: "#ffe9f7ff"
    wallpaper:
      preset: "school"
      scale: 1.0
      background: "#ccffffff"
      tint: "#22000000"
```

Uses the [kaml](https://github.com/charleskorn/kaml) YAML library for serialization. `ThemeColors` uses `@SerialName` annotations for cross-platform YAML key compatibility (e.g., `accent` for `primary`, `menus` for `surface`).

---

## 9. Source Files

| File | Path | Lines | Description |
|---|---|---|---|
| `ThemeManager.kt` | [`common/src/commonMain/.../ui/theme/ThemeManager.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/ThemeManager.kt) | 241 | Theme resolution, persistence, color/wallpaper management |
| `Theme.kt` | [`common/src/commonMain/.../ui/theme/Theme.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/ui/theme/Theme.kt) | 849 | Type definitions, color palettes, `SimpleXTheme` composable |
| `Theme.android.kt` | [`common/src/androidMain/.../ui/theme/Theme.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/theme/Theme.android.kt) | 6 | Android `isSystemInDarkTheme` |
| `Theme.desktop.kt` | [`common/src/desktopMain/.../ui/theme/Theme.desktop.kt`](../../common/src/desktopMain/kotlin/chat/simplex/common/ui/theme/Theme.desktop.kt) | 25 | Desktop `isSystemInDarkTheme` via OsThemeDetector |
| `Files.kt` | [`common/src/commonMain/.../platform/Files.kt`](../../common/src/commonMain/kotlin/chat/simplex/common/platform/Files.kt) | 193 | `readThemeOverrides()`, `writeThemeOverrides()` |
| `NomeTheme.kt` | [`common/src/androidMain/.../ui/nome/theme/NomeTheme.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeTheme.kt) | 130 | Android-only Nome tokens-to-Material 2 adapter |
| `NomeHomeRoute.android.kt` | [`common/src/androidMain/.../ui/nome/home/NomeHomeRoute.android.kt`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt) | — | Resolves current upstream light/dark mode and scopes Nome theme to P07/P08 |
| `NomeProductionShell.kt` | [`android/src/main/.../nome/NomeProductionShell.kt`](../../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt) | 23 | Synchronizes Android system-bar appearance with `CurrentColors` |
| `NomeHomeScreenshotTest.kt` | [`android/src/androidTest/.../nome/home/NomeHomeScreenshotTest.kt`](../../android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeScreenshotTest.kt#L18-L177) | 177 | Defines the verified light/dark renderer capture matrix and TalkBack hold path |

---

## 10. Nome Android Home Theme Boundary

The shared root remains wrapped by [`SimpleXTheme()`](../../common/src/commonMain/kotlin/chat/simplex/common/App.kt#L48-L81); Batch 2 does not replace the global resolver, persistence cascade, per-user/per-chat overrides, wallpapers, or Desktop theme.

At the Activity boundary, [`NomeProductionShell()`](../../android/src/main/java/chat/simplex/app/nome/NomeProductionShell.kt#L17-L23) observes `CurrentColors` and synchronizes light/dark system-bar icon appearance. In the ordinary Android home branch, the [Android route actual](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt#L101-L107) derives the Nome light/dark choice from the same resolved `CurrentColors` and nests [`NomeAndroidTheme()`](../../common/src/androidMain/kotlin/chat/simplex/common/ui/nome/theme/NomeTheme.kt#L82-L130) around P07/P08 only.

This keeps a single upstream theme preference truth while allowing approved Nome semantic tokens on the production home. The nested adapter maps Nome colors, typography, and shapes into the existing Material 2 APIs; it does not write theme preferences, alter other routes, or migrate the app to Material 3.

The current Batch 2 source must still be evidenced in `zh-CN/en × light/dark × 100%/200%` and compared to the approved P07/P08 references. The screenshot test defines 10 renderer states × 2 locales × 2 themes × 2 font scales = 80 planned API 35 captures. The presence of that matrix in source is not a claim that captures exist or that current device/screenshot/contrast/review gates have passed.
