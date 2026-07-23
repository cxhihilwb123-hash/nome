# Commands and results

Repository root: `/Users/forkman03/project/nome/simplex-chat`

## Repository and frozen-boundary checks

```sh
git branch --show-current
git rev-parse HEAD
git merge-base --is-ancestor 59fce95d3cd08897b4ef742447b785cf2e56c7ce HEAD
git diff --cached --name-only
git ls-files .gradle-review
shasum -a 256 apps/multiplatform/android/src/debug/AndroidManifest.xml
git diff --check
```

Result: expected branch/HEAD, ancestor rc 0, empty index, `.gradle-review` untracked, historic manifest SHA unchanged, diff check passed.

Prior frozen manifests were checked with:

```sh
shasum -a 256 -c plans/20260723_nome_android_full_ui_audit_HANDOFF_SHA256SUMS
shasum -a 256 -c plans/20260723_nome_android_ui_fix_batch_01_HANDOFF_SHA256SUMS
shasum -a 256 -c plans/20260723_nome_android_ui_fix_batch_02_HANDOFF_SHA256SUMS
```

## Build

Run from `apps/multiplatform`:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
./gradlew --project-cache-dir .gradle-review \
  :android:assembleDebugAndroidTest \
  :android:testDebugUnitTest \
  :android:lintRelease \
  --no-daemon --no-parallel --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' \
  -Pkotlin.daemon.jvmargs='-Xmx6g'
```

Result: PASS, 144 tasks, 2m24s. Final XML summaries: 54 JVM tests with zero failure/error/skip; release lint has zero Error/Fatal.

## Emulator instrumentation

```sh
adb -s emulator-5554 install -r android-arm64-v8a-debug.apk
adb -s emulator-5554 install -r android-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r \
  -e class chat.simplex.app.nome.home.NomeHomeComposeTest,chat.simplex.app.nome.home.NomeChatListUtilitiesComposeTest \
  chat.simplex.app.nome.dev.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am instrument -w -r \
  -e class chat.simplex.app.nome.home.NomeHomeBatch03ScreenshotTest \
  chat.simplex.app.nome.dev.test/androidx.test.runner.AndroidJUnitRunner
```

Results: 15/15 PASS in 20.665s and 1/1 PASS in 22.413s. Twelve final PNGs were pulled and the two contact sheets plus source comparison were regenerated.

## Physical APK install

```sh
adb -s 3106403166006XM install -r android-arm64-v8a-debug.apk
adb -s 9590146717002S1 install -r android-arm64-v8a-debug.apk
```

Any Vivo identity/install confirmation was completed only by the user.

The final real-click sequence on each device used only fixed labels and in-app coordinates derived from the 76dp navigation containers:

```text
Home -> Contacts -> Home -> custom list 111 -> Manage list
V2048A: top Back; V2047A: Android Back
Settings -> Android Back -> Home
```

UIAutomator XML was filtered to a fixed-label allowlist and was deleted from both devices after the checks. No dynamic chat/contact text was printed or retained.

## Resource inspection

ImageMagick `identify`/`%@` checks validated canvas, visible bounds, alpha channel, and transparent corners for all ten adaptive foregrounds. `aapt2 dump xmltree` and `aapt2 dump resources` validated the manifest icon aliases and packaged density resources. `shasum -a 256` recorded final APKs and evidence artifacts.

No command sent a message, changed an account, deleted a contact/list, disabled screenshot protection, issued Home/power/lock/unlock, or handled a password/sensitive permission.
