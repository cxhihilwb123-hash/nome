# Nome Android 6.5.6 build 373 release-foundation Debug

## Identity

- Date/time and timezone: 2026-08-07, Asia/Shanghai
- Platform / architecture: Android / arm64-v8a and armeabi-v7a
- Marketing version / build number: 6.5.6 / 373
- Configuration: Debug; local release-foundation verification only
- Application ID: `im.nome.app`
- Git repository: `/Users/forkman03/project/nome/nome-release-foundation-20260807`
- Authorized branch for this local verification:
  `codex/nome-release-foundation-20260807`
- Full source commit: `43f81f31242c300e529b909ce662bc681b63beed`
- Pre-build `git status --short`: `clean`
- Controlled dirty patch: `no`
- Source gate: PASS after packaging for both APKs with
  `scripts/release/check-nome-build-source.sh`

This branch is not a formal `release/nome-v*` branch. These artifacts do not
authorize or claim a public release.

## Configuration and dependencies

- Configuration source: `apps/multiplatform/gradle.properties`
- Configuration SHA-256:
  `282a792c71af637c27e8ec76947f2bc8cacd1cd1c44b6223b450d155e2c6efe4`
- Android SDK: compile/target 35; min SDK 28
- Gradle / AGP / Kotlin / Compose: 8.12 / 8.7.0 / 2.1.20 / 1.8.2
- JDK: Android Studio bundled JBR
- NDK: 23.1.7779620
- Ignored native inputs copied from the unified authority worktree after hash
  comparison with the checked-in provenance record:

| ABI | File | SHA-256 |
|---|---|---|
| arm64-v8a | `libsimplex.so` | `91d45817a444d2344877288561e218577239d9a80a504dfa27e8fec9fe64e309` |
| arm64-v8a | `libsupport.so` | `acbabf60244517ec227f84afdbb166c7f2d00e31274d229bbba70926ff240f36` |
| armeabi-v7a | `libsimplex.so` | `281a7941f218d6c013e0482cdbb160b94906c2e1ef23665ae11af5bd4a2e1158` |
| armeabi-v7a | `libsupport.so` | `e8d3489fec86fb4074a23b711c9dfe61aacdab8bcf9755a33cb317d2beb20b57` |

The provenance hashes are recorded in
`plans/evidence/20260716_nome_android_phase1/artifact-provenance.txt`.

## Artifacts

| ABI | Output | Bytes | SHA-256 |
|---|---|---:|---|
| arm64-v8a | `apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk` | 332971086 | `f1109894be1cfa3ef64d0742f3d52b2a6bb1ae90f91ff2026a2a1591a0180c79` |
| armeabi-v7a | `apps/multiplatform/android/build/outputs/apk/debug/android-armeabi-v7a-debug.apk` | 318912190 | `9a0eb509271f41be84e9d233585cf19a35c15ff5ea97cc47e850b7681babe1ae` |

- APK metadata: `im.nome.app`, version `6.5.6`, code `373`, target SDK
  `35`.
- Signing: local Debug certificate, APK Signature Scheme v2 verified.
- Signer certificate SHA-256:
  `b2fbf7616a337889d9aba6b4f3ad2680c8e83b8d4e25108f82068e873e7ae5b0`
- Production signing: `no`

## Verification matrix

| Gate | Status | Evidence |
|---|---|---|
| Android Kotlin compile | PASS | `:common:compileDebugKotlinAndroid` |
| Android unit tests | PASS | `:android:testDebugUnitTest` |
| Android lint | PASS | `:android:lintDebug`; three pre-existing Compose test-state errors were corrected before the clean rerun |
| Android APK assembly | PASS | `:android:assembleDebug`; both ABIs generated |
| Shared desktop tests | PASS | `:common:desktopTest` |
| APK metadata and signature inspection | PASS | `apkanalyzer` and `apksigner verify --verbose --print-certs` |
| Activation/invitation policy runtime | NOT RUN | No device installation in this build pass |
| Messaging | NOT RUN | No device installation in this build pass |
| Channel create/join/presentation | NOT RUN | No device installation in this build pass |
| Calls | NOT RUN | No device installation in this build pass |
| Preserved-data upgrade | NOT RUN | Debug artifacts were not installed |
| Reinstall/recovery | NOT RUN | Debug artifacts were not installed |

The build retained existing SDK XML, deprecated API and C pointer-type warnings;
the final assemble, unit-test, lint and desktop-test commands exited zero.

## Distribution boundary

- State: `local build`
- Push/deploy/upload authorization reference: none
- Public release performed: `no`
- Store submission performed: `no`
- Website upload performed: `no`
- Git tag created: `no`
