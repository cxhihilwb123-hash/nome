# Build and test results

## Authoritative results

| Check | Result |
|---|---|
| `git diff --check` | PASS |
| `:android:assembleDebug` | PASS |
| `:android:assembleDebugAndroidTest` | PASS |
| API 35 targeted instrumentation | PASS, 21/21, 57.88s |
| `:android:testDebugUnitTest` | PASS, 54 tests, 0 failures/errors/skips |
| `:android:lintRelease` | PASS, 68 warnings, 0 Error/Fatal |

## Build command

```sh
GRADLE_USER_HOME='/Users/forkman03/project/nome/simplex-chat/.gradle-review' \
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew --project-cache-dir .gradle-review \
  :android:assembleDebug :android:assembleDebugAndroidTest \
  --no-daemon --no-parallel --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' \
  -Pkotlin.daemon.jvmargs='-Xmx6g'
```

## Static and JVM command

```sh
GRADLE_USER_HOME='/Users/forkman03/project/nome/simplex-chat/.gradle-review' \
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew --project-cache-dir .gradle-review \
  :android:testDebugUnitTest :android:lintRelease \
  --no-daemon --no-parallel --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' \
  -Pkotlin.daemon.jvmargs='-Xmx6g'
```

## Instrumentation command

```sh
adb -s emulator-5554 shell am instrument -w -r \
  -e class chat.simplex.app.nome.home.NomeHomeComposeTest,chat.simplex.app.nome.newchat.NomeNewChatRouteComposeTest,chat.simplex.app.nome.settings.NomeSettingsBackupComposeTest \
  chat.simplex.app.nome.dev.test/androidx.test.runner.AndroidJUnitRunner
```

Result:

```text
Time: 57.88
OK (21 tests)
```

## Final artifacts

| Artifact | SHA256 | Size |
|---|---|---:|
| `android-arm64-v8a-debug.apk` | `04b42f51e59dc693768a8e61a4a4f08e589cf6ba0c8a0a83d52666cc823c2f21` | 353,002,091 bytes |
| `android-debug-androidTest.apk` | `14ab773d5b8763472b93242b3fd9d1e73b0d5bbcfb2ec0afcd842493fb686d96` | 3,627,655 bytes |

Vivo V2048A reported the exact application APK SHA256 above.

Known upstream compiler/deprecation and lint warnings remain. No new build,
test, lint Error, or Fatal was introduced.

