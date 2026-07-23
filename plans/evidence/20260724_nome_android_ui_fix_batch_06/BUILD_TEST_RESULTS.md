# Build and test results

## Results

| Check | Result |
|---|---|
| `git diff --check` | PASS |
| `:android:assembleDebug` | PASS |
| `:android:assembleDebugAndroidTest` | PASS |
| `:android:testDebugUnitTest` | PASS, 54/54 |
| `:android:lintRelease` | PASS, 68 warnings, 0 Error/Fatal |
| New physical instrumentation | BLOCKED by manual USB-install confirmation |

## Build and JVM command

```sh
GRADLE_USER_HOME='/Users/forkman03/project/nome/simplex-chat/.gradle-review' \
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew --project-cache-dir .gradle-review \
  :android:assembleDebug :android:assembleDebugAndroidTest \
  :android:testDebugUnitTest \
  --no-daemon --no-parallel --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' \
  -Pkotlin.daemon.jvmargs='-Xmx6g'
```

## Lint command

```sh
GRADLE_USER_HOME='/Users/forkman03/project/nome/simplex-chat/.gradle-review' \
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew --project-cache-dir .gradle-review \
  :android:lintRelease \
  --no-daemon --no-parallel --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' \
  -Pkotlin.daemon.jvmargs='-Xmx6g'
```

## Artifacts

| Artifact | SHA256 | Size |
|---|---|---:|
| `android-arm64-v8a-debug.apk` | `adc3dc61d88ef9c3094db234b5f69bee239a7e4c10d53e4d7cb2cb8a1651e256` | 358,793,939 bytes |
| `android-debug-androidTest.apk` | `72131130662ff0af2c2e8d8f7a9b0ab438986e7c56bd4396b275df8ee337ef6d` | 3,628,559 bytes |

Known upstream compiler, resource-format, deprecation, and lint warnings
remain. No new Error or Fatal was introduced.
