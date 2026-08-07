# Nome Android 6.5.6 build 373 API 36 Debug

## Identity

- Date/time and timezone: 2026-08-07 17:31:54 CST, Asia/Shanghai
- Platform / architecture: Android / arm64-v8a and armeabi-v7a
- Marketing version / build number: 6.5.6 / 373
- Configuration: Debug; local Android API 36 toolchain verification only
- Application ID: `im.nome.app`
- Git repository:
  `/Users/forkman03/project/nome/nome-client-publish-20260802`
- Authorized migration branch: `codex/nome-android-api36-20260807`
- Full source commit: `ae23430762157cf38945e4ca3b0ab24c5ea06933`
- Pre-build `git status --short`: `clean`
- Controlled dirty patch: `no`
- Source gate: PASS before packaging and after packaging for both APKs with
  `scripts/release/check-nome-build-source.sh`

This branch and its artifacts are not a formal `release/nome-v*` release.

## Configuration and dependencies

| Configuration source | SHA-256 |
|---|---|
| `apps/multiplatform/gradle.properties` | `732b83fdad73b01f9f6f6c7a5a974543aaba7da10388e7c99c9e11d72ef809bb` |
| `apps/multiplatform/build.gradle.kts` | `75642ca64d9ca16c3f0ed18895d73d591d53368891850b4388dba1272f143238` |
| `apps/multiplatform/android/build.gradle.kts` | `b8cdcb84b2f24559cce5148b14460b77abb9e031018f47603478551e71967050` |

- Android SDK: compile/target 36; min SDK 28
- Gradle / AGP / Kotlin / Compose: 8.12 / 8.9.1 / 2.2.10 / 1.8.2
- JDK: Android Studio bundled JBR 21.0.10
- Android Build Tools: 36.0.0
- NDK: 23.1.7779620
- Runtime device: isolated `Nome_API_36` Pixel 8 AVD, Android 16 / API 36,
  Google APIs ARM64 system image revision 7, emulator 36.6.11.0
- Compatibility basis: Android's official tool matrix lists AGP 8.9.1 as the
  minimum version for API 36. Kotlin 2.2.10 keeps AGP 8.9.1 and Gradle 8.12
  within Kotlin's documented supported ranges.
- Kotlin compiler memory for verification was supplied only on the command
  line as `-Dkotlin.daemon.jvm.options=-Xmx4096m,Xms512m`; no machine-specific
  memory setting was committed.

Ignored native inputs matched the existing checked-in provenance record:

| ABI | File | SHA-256 |
|---|---|---|
| arm64-v8a | `libsimplex.so` | `91d45817a444d2344877288561e218577239d9a80a504dfa27e8fec9fe64e309` |
| arm64-v8a | `libsupport.so` | `acbabf60244517ec227f84afdbb166c7f2d00e31274d229bbba70926ff240f36` |
| armeabi-v7a | `libsimplex.so` | `281a7941f218d6c013e0482cdbb160b94906c2e1ef23665ae11af5bd4a2e1158` |
| armeabi-v7a | `libsupport.so` | `e8d3489fec86fb4074a23b711c9dfe61aacdab8bcf9755a33cb317d2beb20b57` |

## Artifacts

| ABI | Output | Bytes | SHA-256 |
|---|---|---:|---|
| arm64-v8a | `apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk` | 332644326 | `6c4c2b5884755684c20ac50c6d7f15fdccbe55756069b26057a3c414557a409d` |
| armeabi-v7a | `apps/multiplatform/android/build/outputs/apk/debug/android-armeabi-v7a-debug.apk` | 318585430 | `9efc7a4f9ebcf2f1b14250abc6fca7125be5a8c3f4b10d46c8302d02639279dc` |

- APK metadata: `im.nome.app`, version `6.5.6`, code `373`, compile/target
  SDK 36, min SDK 28.
- Signing: local Debug certificate; APK Signature Scheme v2 verified for both
  APKs.
- Signer certificate SHA-256:
  `b2fbf7616a337889d9aba6b4f3ad2680c8e83b8d4e25108f82068e873e7ae5b0`
- Production signing: `no`

## Verification matrix

| Gate | Status | Evidence |
|---|---|---|
| Gradle configuration | PASS | `./gradlew help`; Kotlin 2.2 compiler DSL accepted |
| Android APK assembly | PASS | `:android:assembleDebug`; both ABIs generated |
| Android unit tests | PASS | `:android:testDebugUnitTest` |
| Android lint | PASS | `:android:lintDebug` |
| Shared Android Kotlin compile | PASS | `:common:compileDebugKotlinAndroid` via the Android build graph |
| Shared Desktop regression | PASS | `:common:desktopTest` |
| APK metadata and signature inspection | PASS | Build Tools 36 `aapt` and `apksigner` |
| Fresh install and onboarding | PASS | API 36 arm64 APK installed on `emulator-5554`; local identity, agent/chat databases and home UI initialized |
| Android 16 notification permission | PASS | System permission flow completed; `POST_NOTIFICATIONS` read back as granted |
| Background service | PASS | `SimplexService` remained foreground with ongoing notification after Home; battery exemption granted |
| Cold relaunch | PASS | Force-stop followed by cold launch restored identity, private notes and foreground service without a crash |
| Preserved-data upgrade | PASS | Same-signed API 35 build 373 baseline was replaced with API 36 build 373 using `adb install -r`; identity `NomeUpgrade35`, databases, preferences, notification permission and service state were preserved |
| Device reboot recovery | PASS | After Android 16 reboot, the foreground service and notification recovered before manual launch; the upgraded identity and home UI remained intact |
| Invitation creation | NOT RUN | No external invitation was created during this toolchain validation |
| Messaging | NOT RUN | A second isolated client was not provisioned |
| Channel create/join/presentation | NOT RUN | A second isolated client was not provisioned |
| Calls | NOT RUN | Camera/microphone permission and two-client call flow were not exercised |
| Reinstall/recovery | NOT RUN | No backup import or recovery workflow was exercised |

The first combined Android/Desktop compilation attempt exhausted the previous
2 GB Kotlin daemon heap. The final Android and Desktop passes ran separately
with a 4 GB one-shot Kotlin daemon setting and exited zero. Existing SDK XML,
resource-format, deprecated API, unresolved opt-in marker, Manifest and native
C pointer-type warnings remain; none failed assemble, tests or lint.

Runtime UI state was read with Android UI Automator because the application
sets `FLAG_SECURE` at launch and Android correctly returned black screenshots.
The fresh install, onboarding, background retention, cold relaunch, API 35 to
API 36 preserved-data replacement, and reboot checks produced no application
crash, ANR, native abort, link error, SQLite exception or migration failure in
the inspected logs. The runtime checks do not establish public server health,
two-client messaging, channels, calls or store-release readiness.

## Distribution boundary

- State: `local build`
- Push/deploy/upload authorization reference: none for this migration branch
- Public release performed: `no`
- Store submission performed: `no`
- Website upload performed: `no`
- Git tag created: `no`
