# Build and test results

All Gradle commands ran from `apps/multiplatform` with Android Studio JBR, process-only `-Xmx6g`, `--no-parallel`, and one worker.

| Command / check | Result |
|---|---|
| `:android:assembleDebug :android:assembleDebugAndroidTest` | PASS, 93 tasks, 2m08s |
| `:android:testDebugUnitTest :android:lintRelease` | PASS, 105 tasks, 2m58s |
| JVM unit tests | PASS, 54 tests, 0 failures/errors/skips |
| release lint | PASS, 68 Warning, 0 Error/Fatal, 0 locations on changed paths |
| `git diff --check` | PASS |

Artifacts:

| Artifact | SHA256 |
|---|---|
| `android/build/outputs/apk/debug/android-arm64-v8a-debug.apk` | `3b8e8a31b2057d552932a7ade54fbca28e1ac5f8a4f2bcc599e2fb5b4784237f` |
| `android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk` | `8335b4e160e8f6ec03f7d05f95c37642a5bb9252b7f5b25bb234a670f40250a1` |

The updated Android Compose test compiled into the test APK. The previously recorded Vivo test-Activity launch block remains an infrastructure gap; this batch did not repeat the known hanging instrumentation run.

