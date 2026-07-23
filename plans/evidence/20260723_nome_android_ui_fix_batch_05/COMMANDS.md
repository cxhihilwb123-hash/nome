# Commands

All commands were executed in the existing dirty tree without reset, clean,
stash, commit, or push.

## Repository and frozen evidence

```sh
git status --short --untracked-files=no
git diff --cached --name-only
git ls-files .gradle-review
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git merge-base 59fce95d3cd08897b4ef742447b785cf2e56c7ce HEAD
shasum -a 256 apps/multiplatform/android/src/debug/AndroidManifest.xml
shasum -a 256 -c FROZEN_MANIFEST
```

The feature-validation, feature-validation-E2E, full-UI-audit, and UI-fix
Batch 01 through Batch 04 manifests all passed.

## Build and tests

See `BUILD_TEST_RESULTS.md` for the exact Gradle and instrumentation commands.

## Device checks

Scoped commands used:

```sh
adb devices -l
adb -s SERIAL install -r FINAL_APK
adb -s SERIAL shell am start -n APP_ID/MAIN_ACTIVITY
adb -s SERIAL shell input tap X Y
adb -s SERIAL shell input keyevent KEYCODE_BACK
adb -s SERIAL shell uiautomator dump /data/local/tmp/nome_batch05_NAME.xml
adb -s SERIAL pull /data/local/tmp/nome_batch05_NAME.xml TEMP_PATH
adb -s SERIAL shell pm path APP_ID
adb -s SERIAL shell sha256sum INSTALLED_APK_PATH
adb -s SERIAL shell rm -f EXACT_GENERATED_UI_TREE_PATHS
```

UI trees were queried only for non-sensitive fixed text, accessibility-node
counts, clickable bounds, and control-class counts. Contact names, messages,
server addresses, fingerprints, and credentials were not retained.

No power, Home, lock, wake, unlock, password, sensitive permission, account,
communication, screenshot-protection, or server-data mutation was automated.

