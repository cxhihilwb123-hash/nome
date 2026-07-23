# Build and test results

All Gradle commands ran from `apps/multiplatform` with Android Studio JBR. The successful full runs used process-only `-Xmx6g` and `--max-workers=1`; no repository Gradle configuration was changed.

| Command / check | Result |
|---|---|
| `:common:compileDebugKotlinAndroid :android:compileDebugKotlin :android:assembleDebugAndroidTest` | PASS before final touch-area refinement |
| `:android:testDebugUnitTest :android:lintRelease :android:assembleDebug` | First attempt: environment OOM in unchanged `ChatItemInfoView.kt`; retry with process-only 6 GB heap: PASS, 133 tasks |
| JVM unit tests | PASS: 54 tests, 0 failures, 0 errors, 0 skipped |
| release lint | PASS: 0 Error/Fatal; 68 warnings, none reference the changed files |
| `:common:compileKotlinDesktop :android:assembleDebugAndroidTest` | PASS, 77 tasks |
| final `:android:assembleDebug :android:assembleDebugAndroidTest` | PASS after true semantics-boundary ordering, 93 tasks, 1m25s |
| final `:android:testDebugUnitTest :android:lintRelease` | PASS after true semantics-boundary ordering, 105 tasks, 2m16s |
| final inset-aware `:android:assembleDebug :android:assembleDebugAndroidTest` | PASS, 93 tasks, 1m04s |
| final inset-aware `:android:testDebugUnitTest :android:lintRelease` | PASS, 105 tasks, 1m46s |
| post-review 84 dp Logo `:android:assembleDebug :android:assembleDebugAndroidTest` | PASS, 93 tasks, 1m11s |
| post-review 84 dp Logo `:android:testDebugUnitTest :android:lintRelease` | PASS, 105 tasks, 2m26s |
| `git diff --check` | PASS |

Final artifacts:

| Artifact | SHA256 |
|---|---|
| `android/build/outputs/apk/debug/android-arm64-v8a-debug.apk` | `a71382e0c8af08143acf5b7efcaea0f087185684ad74a5a1d2f9787a4def757b` |
| `android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk` | `969d4ad6c0786b4f79ca9b03aab29292f7a284b1d74c5b9ac343fb0af6dabb4b` |

The instrumentation APK compiled and was installed on both devices after user-completed Vivo identity verification. On both devices `AndroidJUnitRunner` entered `primaryNavigationExposesThreeTabsAndDispatchesRealDestinations`, but no test Activity became focused and no pass/fail result was produced. Each run was stopped after the same reproducible OEM launch stall. This is recorded as an instrumentation infrastructure gap, not a passing test and not a product assertion failure.
