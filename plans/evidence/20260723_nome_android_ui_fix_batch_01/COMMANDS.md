# Commands and results

Working directory unless stated: `/Users/forkman03/project/nome/simplex-chat`.

## Read-only baseline and frozen evidence

```text
git branch --show-current
git rev-parse HEAD
git rev-list -n 1 v6.5.6
git merge-base --is-ancestor 59fce95d3cd08897b4ef742447b785cf2e56c7ce HEAD
git diff --cached --name-only
git ls-files .gradle-review
git status --porcelain=v1 -uall
shasum -a 256 -c plans/20260723_nome_android_full_ui_audit_HANDOFF_SHA256SUMS
shasum -a 256 -c plans/20260722_nome_android_feature_validation_HANDOFF_SHA256SUMS
```

Result: branch/HEAD/baseline/ancestry matched the frozen handoff; staged count 0; `.gradle-review` tracked count 0; both frozen manifests PASS. The starting porcelain snapshot contained 22,205 entries and was retained in `/tmp` for final comparison.

## Build and static checks

Working directory: `apps/multiplatform`.

All successful Gradle commands used Android Studio JBR. The full successful form was:

```text
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew <tasks> --no-daemon --no-parallel --max-workers=1 -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' -Pkotlin.daemon.jvmargs='-Xmx6g'
```

Tasks and results:

```text
:common:compileDebugKotlinAndroid :android:compileDebugKotlin :android:assembleDebugAndroidTest
PASS

:android:testDebugUnitTest :android:lintRelease :android:assembleDebug
FIRST ATTEMPT: FAIL, Kotlin GC overhead OOM while lowering unchanged ChatItemInfoView.kt
RETRY WITH PROCESS-ONLY 6 GB HEAP: PASS, 133 tasks, 11m54s

:common:compileKotlinDesktop :android:assembleDebugAndroidTest
PASS, 77 tasks, 2m33s

:android:assembleDebug :android:assembleDebugAndroidTest
PASS after target-height/centering refinement, 93 tasks, 21s

:android:assembleDebug :android:assembleDebugAndroidTest
PASS after final selectable/semantics modifier-order correction, 93 tasks, 1m25s

:android:testDebugUnitTest :android:lintRelease
PASS against final source, 105 tasks, 1m36s

:android:testDebugUnitTest :android:lintRelease
PASS after final selectable/semantics modifier-order correction, 105 tasks, 2m16s

:android:assembleDebug :android:assembleDebugAndroidTest
PASS after shared navigation-bar inset fallback, 93 tasks, 1m04s

:android:testDebugUnitTest :android:lintRelease
PASS after shared navigation-bar inset fallback, 105 tasks, 1m46s

:android:assembleDebug :android:assembleDebugAndroidTest
PASS after user-requested P07/P23 Logo width refinement from 66 dp to 84 dp, 93 tasks, 1m11s

:android:testDebugUnitTest :android:lintRelease
PASS after the 84 dp Logo refinement, 105 tasks, 2m26s

git diff --check
PASS
```

Unit XML aggregation: 54 tests, 0 failures, 0 errors, 0 skipped. Lint XML aggregation: 68 Warning, 0 Error/Fatal; none reference the changed paths.

## Device commands

Only the following operation families were used:

```text
adb devices -l
adb -s <serial> install -r -t <local Nome debug APK>
adb -s <serial> install -r -t <local Nome test APK>
adb -s <serial> shell cmd package resolve-activity --brief ...
adb -s <serial> shell am force-stop chat.simplex.app.nome.dev
adb -s <serial> shell am start -W -n <resolved Nome launcher activity>
adb -s <serial> shell input tap <verified bounds center>
adb -s <serial> shell input keyevent KEYCODE_BACK
adb -s <serial> shell cmd uimode night yes|no
adb -s <serial> shell cmd locale get-app-locales ...
adb -s <serial> shell cmd locale set-app-locales ...
adb -s <serial> shell am instrument -w -r -e class ...
adb -s <serial> shell dumpsys package|window|activity ...
adb -s <serial> shell uiautomator dump <temporary device path>
adb -s <serial> exec-out cat <temporary device path> | local fixed-label filtering/redaction
adb -s <serial> shell rm -f /sdcard/nome_batch01_*.xml
```

No power, system Home, lock, wake, unlock, password input, sensitive-permission approval, screenshot-protection change, data clear, or uninstall was executed.

The full route-regression production APK `cafa19c0...` and test APK `969d4ad6...` were installed on both devices after the user completed Vivo identity-verification prompts. Automation did not enter or bypass any credential or sensitive confirmation.

Real taps and fixed-label target-state extraction covered P07/P22/P23, all three bottom destinations, top back, Android system back, Simplified Chinese/English, and light/dark on both devices. Each device was restored to an empty Android per-app locale override, system Chinese, and light mode.

After user visual review requested a slightly larger header Logo, the final production APK `a71382e0...` was installed on both devices. Fresh cold P07 launch plus real P07 -> P23 -> P07 taps passed. UI hierarchy bounds for the P07 `Nome` node were 221 x 90 px on V2048A and 252 x 103 px on V2047A, matching approximately 84 x 34 dp and the 700:285 source ratio. Both devices were left on P07 Home for user-visible inspection.

The focused instrumentation method was attempted on each device after the test APK installed. On both devices the runner entered the method but Vivo did not launch the test Activity; neither produced an assertion result and each hung run was manually interrupted. This was treated as an OEM test-infrastructure gap.

All batch-owned `/sdcard/nome_batch01_*.xml` temporary UI trees were deleted from both devices after fixed-label/redacted extraction. No other device file was removed.
