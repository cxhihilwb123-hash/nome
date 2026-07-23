# Build and test results

All Gradle commands ran from `apps/multiplatform` with the Android Studio JBR, repository-local untracked `.gradle-review` cache, one worker, no parallel execution, and process-only 6 GiB heap settings.

## Final gates

| Command / check | Result |
| --- | --- |
| Final consolidated `:android:assembleDebugAndroidTest :android:testDebugUnitTest :android:lintRelease` | PASS, 144 tasks, 2m24s |
| Final debug APK assembly | PASS; arm64 APK timestamp 2026-07-23 19:49:17 |
| JVM test XML summary | PASS, 54 tests, 0 failures/errors/skips |
| Release lint XML summary | PASS, 0 Error, 0 Fatal |
| API 35 `NomeHomeComposeTest` + `NomeChatListUtilitiesComposeTest` | PASS, 15/15, 20.665s |
| API 35 `NomeHomeBatch03ScreenshotTest` | PASS, 1/1, 22.413s |
| `git diff --check` | PASS |
| Root design QA | `final result: passed` |

An earlier unit/lint attempt using less constrained memory/parallel settings exhausted the local Gradle JVM heap. The same code passed with the documented one-worker 6 GiB command; this is recorded as build-infrastructure configuration rather than a product failure.

## Artifacts

| Artifact | SHA256 |
| --- | --- |
| `apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk` | `730408743fe4b5059edd23048afa6207b63fbf8a26f638b57d321d997f6c76a4` |
| `apps/multiplatform/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk` | `3c5c6c18a2648da22d7c6b2df48b7d43b07c2edb0e8fc00ad615498b5e3c7d3e` |

## Native render matrix

The screenshot test produced twelve 1080x2400 native PNGs at API 35 / 420 dpi:

- Home, Contacts, custom list `111`
- English and Chinese
- Light and dark

The local evidence copies were refreshed from the exact final APK after the final screenshot run. The comparison and light/dark contact sheets were then regenerated from those twelve files and visually inspected together with the approved source.

## Physical-device instrumentation boundary

The known Vivo test-Activity launch hang documented in Batch 02 remains an infrastructure limitation. Targeted real-app clicks are therefore verified on the Vivo devices, while deterministic Compose assertions and non-sensitive pixel renders run on the API 35 emulator. The known Vivo instrumentation block is not classified as an app failure.

V2048A and V2047A both ran the final real-app route sequence successfully. Screenshot protection remained enabled; no real-account screenshot was captured.
