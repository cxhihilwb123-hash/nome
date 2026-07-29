# Nome Android 6.5.6 code 359 two-device install

## Identity

- Date/time and timezone: 2026-07-29 17:16:08 +0800
- Platform / architecture: Android physical devices / arm64-v8a
- Marketing version / version code: 6.5.6 / 359
- Configuration: Debug, internal direct-device build
- Application ID: `chat.simplex.app.nome.dev`
- Git repository: `/Users/forkman03/project/nome/nome-client`
- Authorized consolidated branch: `codex/nome-v656-unified`
- Full source commit: `c39e249d3843beb07378fc6231dff6c6cd6cff21`
- Accepted product-source merge: `124863fe607eb84c177f8ccce7c76793f1ceeb55`
- Pre-build authority status: clean; unified source gate PASS
- Controlled dirty patch: no
- Product source changes during packaging: none

## Configuration and dependencies

- `apps/multiplatform/android/build.gradle.kts` SHA-256:
  `0506bf94f2edb8968d85066134989e2690053b544af18f6f1afca941d05ceed5`
- `apps/multiplatform/gradle.properties` SHA-256:
  `7ae33b7f94abdaa1c4134d99e05fe48f9ecd1c083bb8e410b668b97942bf6311`
- Ignored `apps/multiplatform/local.properties` SHA-256:
  `82dfcd80981ec449d7d97687560e6461f0af0bcd36eafecea27d54e153087bf3`
- Build tools: Gradle 8.12, Kotlin 2.0.21, Eclipse Adoptium JDK 17.0.19
- Android package metadata: min SDK 28, target/compile SDK 35

No configuration values or credentials are recorded here.

## Build and tests

- Command:
  `./gradlew :android:testDebugUnitTest :android:assembleDebug --offline --no-daemon --max-workers=1`
- JVM limit: `-Xmx6g -XX:MaxMetaspaceSize=1g`
- Result: `BUILD SUCCESSFUL in 1m 56s`
- Gradle tasks: 72 actionable; 71 executed, 1 up-to-date
- Unit tests: 70 passed across 13 suites; 0 failures, 0 errors, 0 skipped
- Build output included deprecation, expect/actual beta, and SDK XML compatibility
  warnings; none failed compilation, tests, packaging, or signing verification.

## Artifact

- Standard filename:
  `Nome-Android-6.5.6-code359-20260729-arm64-debug.apk`
- Output path:
  `/Users/forkman03/project/nome/deliverables/Nome-Android-6.5.6-code359-20260729-arm64-debug.apk`
- Bytes: 332,903,218
- SHA-256:
  `792a7ced820264e68a7e9eb31c11f59bfe200a3e94bfca10cfa35cc89951610b`
- Native code: `arm64-v8a`
- Signature: APK Signature Scheme v2 PASS; one Android Debug signer
- Signer certificate SHA-256:
  `b2fbf7616a337889d9aba6b4f3ad2680c8e83b8d4e25108f82068e873e7ae5b0`
- Post-build source gate with artifact binding: PASS at the same clean source
  commit

## Installation

| Device | OS / API | Method | Result | Device readback |
|---|---|---|---|---|
| vivo V2048A, `3106403166006XM` | Android 13 / API 33 | `adb install -r` | PASS | 6.5.6 (359), update 2026-07-29 17:12:11 +0800 |
| vivo V2047A, `9590146717002S1` | Android 13 / API 33 | `adb install -r` | PASS | 6.5.6 (359), update 2026-07-29 17:14:04 +0800 |

- Both devices advertised `arm64-v8a` as their primary ABI.
- vivo's package installer required its visible external-source acknowledgement;
  only the Nome install acknowledgement and Continue Install controls were used.
- Existing application containers were preserved. No uninstall, package data
  clear, reset, or destructive reinstall was performed.
- Each device's installed `base.apk` SHA-256 was independently read back as
  `792a7ced820264e68a7e9eb31c11f59bfe200a3e94bfca10cfa35cc89951610b`,
  exactly matching the archived local APK.
- Both devices returned `Status: ok` from `am start -W`; the Nome process was
  live and `MainActivity_default` was the top resumed activity on each device.

## Verification matrix

| Gate | Status | Evidence |
|---|---|---|
| Unified source gate | PASS | clean commit and configuration fingerprints in final readback |
| Android unit tests | PASS | 70 tests across 13 suites |
| APK package/version/ABI | PASS | `aapt dump badging` |
| APK signature | PASS | `apksigner verify --verbose --print-certs` |
| Upgrade installation, V2048A | PASS | `adb install -r`, version/hash/launch readback |
| Upgrade installation, V2047A | PASS | `adb install -r`, version/hash/launch readback |
| Activation/invitation runtime policy | NOT RUN | No account workflow was opened |
| Messaging | NOT RUN | No chat content was opened or changed |
| Channel create/join/presentation | NOT RUN | Out of scope for install validation |
| Call rejection synchronization | NOT RUN | Out of scope for install validation |
| Reinstall/recovery | NOT RUN | Destructive reinstall was intentionally not performed |

## Distribution boundary

- State: internal direct-device Debug test
- Git push, tag, deployment, store upload, production release, and public
  publication: not performed
- This record proves only the two installations and stated smoke checks; it is
  not a production-release or full product-E2E claim.

## Evidence index

- Final evidence packet:
  `/Users/forkman03/project/nome/consolidation-evidence/20260729_android_two_device_install`
- Final readback:
  `/Users/forkman03/project/nome/consolidation-evidence/20260729_android_two_device_install/FINAL_READBACK.md`
- Source-gate output:
  `/Users/forkman03/project/nome/consolidation-evidence/20260729_android_two_device_install/SOURCE_GATE.txt`
- Artifact manifest:
  `/Users/forkman03/project/nome/consolidation-evidence/20260729_android_two_device_install/SHA256SUMS`
