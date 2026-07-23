# Build and test results

## Final authoritative runs

| Check | Result |
|---|---|
| `git diff --check` | PASS |
| `:android:assembleDebug` | PASS |
| `:android:assembleDebugAndroidTest` | PASS |
| API 35 `NomeHomeComposeTest` + `NomeSettingsBackupComposeTest` | PASS, 17/17, 24.556s |
| `:android:testDebugUnitTest` | PASS, 54 tests, 0 failures/errors/skips |
| `:android:lintRelease` | PASS, 68 warnings, 0 Error/Fatal |

Final build command:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew --project-cache-dir .gradle-review \
  :android:assembleDebug :android:assembleDebugAndroidTest \
  --no-daemon --no-parallel --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' \
  -Pkotlin.daemon.jvmargs='-Xmx6g'
```

Final static/test command:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew --project-cache-dir .gradle-review \
  :android:testDebugUnitTest :android:lintRelease \
  --no-daemon --no-parallel --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' \
  -Pkotlin.daemon.jvmargs='-Xmx6g'
```

Instrumentation command:

```sh
adb -s emulator-5554 shell am instrument -w -r \
  -e class chat.simplex.app.nome.home.NomeHomeComposeTest,chat.simplex.app.nome.settings.NomeSettingsBackupComposeTest \
  chat.simplex.app.nome.dev.test/androidx.test.runner.AndroidJUnitRunner
```

Result:

```text
Time: 24.556
OK (17 tests)
```

## Final artifacts

| Artifact | SHA256 | Size |
|---|---|---:|
| `android-arm64-v8a-debug.apk` | `4b44d0d7c0c19a22f9b23c660ff711691070f2879a1a7432bf1745fe9aef5958` | 358,785,419 bytes |
| `android-debug-androidTest.apk` | `1fe1ea5c5e382bf38f6e7aafbe48767e088c36f3d3170a4f48635e563c4634c6` | 3,327,967 bytes |

Both physical devices reported the exact app APK SHA256 shown above.

Known upstream compiler/deprecation warnings remain; no new build or lint error was introduced.

