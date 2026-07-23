# Commands

All commands were executed from the existing dirty tree without reset, clean, stash, commit, or push.

## Read-only repo boundary

```sh
git branch --show-current
git rev-parse HEAD
git merge-base --is-ancestor 59fce95d3cd08897b4ef742447b785cf2e56c7ce HEAD
git diff --cached --name-only
git ls-files
shasum -a 256 apps/multiplatform/android/src/debug/AndroidManifest.xml
```

## Build and tests

See `BUILD_TEST_RESULTS.md` for the exact final Gradle and instrumentation commands.

## Device checks

Read-only or scoped application UI actions used:

```sh
adb devices -l
adb -s SERIAL install -r android-arm64-v8a-debug.apk
adb -s SERIAL shell am force-stop chat.simplex.app.nome.dev
adb -s SERIAL shell am start -W \
  -n chat.simplex.app.nome.dev/chat.simplex.app.MainActivity_default
adb -s SERIAL shell input tap X Y
adb -s SERIAL shell input keyevent 4
adb -s SERIAL shell uiautomator dump /data/local/tmp/nome_batch04_*.xml
adb -s SERIAL shell sha256sum PACKAGE_APK_PATH
adb -s SERIAL shell cmd overlay lookup \
  android android:dimen/navigation_bar_height
adb -s SERIAL shell 'rm -f /data/local/tmp/nome_batch04_*.xml'
```

No power, Home, lock, wake, unlock, password, sensitive permission, screenshot-protection, account, or communication action was automated.

## Frozen evidence verification

```sh
shasum -a 256 -c plans/20260722_nome_android_feature_validation_HANDOFF_SHA256SUMS
shasum -a 256 -c plans/20260723_nome_android_feature_validation_e2e_HANDOFF_SHA256SUMS
shasum -a 256 -c plans/20260723_nome_android_full_ui_audit_HANDOFF_SHA256SUMS
shasum -a 256 -c plans/20260723_nome_android_ui_fix_batch_01_HANDOFF_SHA256SUMS
shasum -a 256 -c plans/20260723_nome_android_ui_fix_batch_02_HANDOFF_SHA256SUMS
shasum -a 256 -c plans/20260723_nome_android_ui_fix_batch_03_HANDOFF_SHA256SUMS
```

All six manifests passed.

