# Android API 36 toolchain assessment

Date: 2026-08-07 (Asia/Shanghai)

## Decision

Do not retain a standalone `compileSdk` / `targetSdk` 35-to-36 edit in the
release foundation branch. The checked-in values remain 35 until the Kotlin
Multiplatform and Android Gradle plugin toolchain is upgraded and the full
Android gate passes.

This is a bounded toolchain migration, not a two-line release metadata change.

## Evidence

- Installed locally: Android platform 36 and build-tools 36.0.0.
- Current project: Kotlin 2.1.20, Compose Multiplatform 1.8.2, AGP 8.7.0 and
  Gradle 8.12.
- Android's current compatibility table requires AGP 8.9.1 or newer for API 36:
  <https://developer.android.com/build/releases/about-agp>.
- Kotlin's current Multiplatform compatibility table supports Kotlin 2.1.20
  only through AGP 8.7.2:
  <https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html>.
- Google Play currently states that new apps and updates must target API 36
  starting 2026-08-31:
  <https://support.google.com/googleplay/android-developer/answer/11926878>.

An exploratory SDK 36 build completed configuration and reached native CMake
tasks, with AGP warning that 8.7.0 was tested only through compileSdk 35. The
build then stopped because this temporary worktree did not contain
`common/src/commonMain/cpp/android/libs/arm64-v8a/libsimplex.so`. That run did
not establish a complete SDK 36 build, test or lint pass, so the two-line edit
was reverted.

With SDK 35 restored, these gates passed on this branch:

- `:android:processDebugMainManifest`
- `:common:compileDebugKotlinAndroid`
- `:android:testDebugUnitTest`
- `:android:lintDebug`
- `:common:desktopTest`
- partial release-signing configuration negative gate

## Required follow-up before the Play deadline

1. Choose a Kotlin/Compose version whose official KMP range includes an AGP
   version that supports API 36.
2. Upgrade Kotlin, Compose and AGP together in a separate temporary migration
   branch from the unified authority.
3. Restore or build the exact native `libsimplex.so` inputs from their recorded
   provenance; do not copy an unverified library from a recovery worktree.
4. Run Android compile, unit, lint, bundle, signed APK/AAB and preserved-data
   upgrade gates.
5. Merge the verified migration into the unified authority before creating the
   next formal release branch.
