# Commands and results

## Repository checks

```text
git branch --show-current
git rev-parse HEAD
git rev-list -n 1 v6.5.6
git merge-base --is-ancestor 59fce95d3cd08897b4ef742447b785cf2e56c7ce HEAD
git diff --cached --name-only
git ls-files .gradle-review
shasum -a 256 -c plans/20260723_nome_android_ui_fix_batch_01_HANDOFF_SHA256SUMS
git diff --check
```

Result: expected branch, HEAD, baseline, and ancestry; empty index; `.gradle-review` untracked; Batch 01 manifest PASS; diff check PASS.

## Gradle

```text
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :android:assembleDebug :android:assembleDebugAndroidTest --no-daemon --no-parallel --max-workers=1 -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' -Pkotlin.daemon.jvmargs='-Xmx6g'

JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :android:testDebugUnitTest :android:lintRelease --no-daemon --no-parallel --max-workers=1 -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' -Pkotlin.daemon.jvmargs='-Xmx6g'
```

Both commands PASS.

## Device operation families

```text
adb devices -l
adb -s <serial> install -r -t <debug APK>
adb -s <serial> shell am start -W -n <Nome activity>
adb -s <serial> shell input swipe <same start/end for long press>
adb -s <serial> shell input tap <verified fixed navigation target>
adb -s <serial> shell input keyevent KEYCODE_BACK
adb -s <serial> shell uiautomator dump <temporary path>
adb -s <serial> pull <temporary path> <temporary local path>
```

UI trees were processed locally to emit only fixed labels, control bounds, focus state, and character length. No chat text/contact name was printed. No send action or delete confirmation was accepted.

All exact batch-owned temporary UI hierarchy paths were removed from the two devices and local temporary storage after extraction.

