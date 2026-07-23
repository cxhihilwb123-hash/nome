# Commands

Commands were scoped to build, test, repository inspection, and sanitized
read-only device diagnostics.

## Repository checks

```sh
git status --short
git diff --cached --name-only
git diff --check
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
git merge-base --is-ancestor OFFICIAL_BASELINE HEAD
git ls-files .gradle-review apps/multiplatform/.gradle-review
shasum -a 256 apps/multiplatform/android/src/debug/AndroidManifest.xml
shasum -a 256 -c BATCH05_MANIFEST
```

## Build and tests

See `BUILD_TEST_RESULTS.md`.

## Device diagnostics

Sanitized checks covered:

```sh
adb devices -l
adb shell cmd appops get APP_ID POST_NOTIFICATION
adb shell cmd appops get APP_ID RUN_IN_BACKGROUND
adb shell cmd appops get APP_ID RUN_ANY_IN_BACKGROUND
adb shell am get-standby-bucket APP_ID
adb shell dumpsys deviceidle
adb shell settings get secure lock_screen_show_notifications
adb shell settings get secure lock_screen_allow_private_notifications
adb shell settings get system lock_screen_bright_on
adb shell settings get global heads_up_notifications_enabled
adb shell settings get global zen_mode
adb shell dumpsys activity services APP_ID
adb shell dumpsys notification
```

Outputs were restricted to permission, channel, service, system-setting, and
notification-presence facts. Names, message bodies, server addresses,
fingerprints, credentials, and account data were not retained.

No power, Home, lock, wake, unlock, password, permission approval, screenshot
protection, communication, or server mutation was automated.
