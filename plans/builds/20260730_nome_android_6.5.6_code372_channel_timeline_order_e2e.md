# Nome Android 6.5.6 code 372 channel timeline order E2E

## Identity

- Date/time and timezone: 2026-07-30 17:55-18:11 CST (+0800)
- Platform / architecture: Android arm64-v8a
- Marketing version / build number: 6.5.6 / versionCode 372
- Configuration: Debug, local two-device validation
- Application ID / label: `im.nome.app.dev` / `Nome`
- Git repository: `/Users/forkman03/project/nome/nome-client`
- Authorized branch: `codex/nome-v656-unified`
- Accepted unified ancestor: `124863fe607eb84c177f8ccce7c76793f1ceeb55`
- Source commit: `646f1f5c6473a2fabaefe5b99354f62b01d0f2d1` (`fix(android): keep latest channel messages at bottom`)
- Previous channel relay evidence: `plans/builds/20260730_nome_android_6.5.6_code371_channel_relay_e2e.md`

The APK was packaged while the four package-affecting Android/Common paths were still a controlled patch. Those exact production/version contents were committed unchanged as `646f1f5c6...` before installation; the fifth committed path is the non-packaged Android unit regression test added after the build. Preserved unrelated iOS tracked and untracked paths remained outside the index throughout.

The formal clean-source gate remains blocked by those preserved iOS paths. This is an internal Debug artifact and is not a formal release claim.

## Fix

The Android relay-channel feed no longer has a separate forward-layout path. It now uses the shared conversation timeline:

- model items remain reversed for presentation, with list index 0 representing the newest item;
- `LazyColumnWithScrollBar` is bottom-aligned with `reverseLayout = true`;
- the channel-only `LazyListState(0, 0)` forward-layout anchor was removed;
- channel messages use the shared item/date separation path;
- the obsolete forward-layout helper and its freezing test were removed;
- `ChannelTimelineRouteBoundaryTest` now guards the reversed data path, bottom alignment, reversed layout, shared date separation, and absence of the old channel-only forward-layout symbols.

Key source hashes after commit:

- `ChatView.kt`: `414a4ac30b67d8c6dc2be647849e38a7552dd3b8694ba6da074575d764f8e557`
- `gradle.properties`: `8aa23e2048a8679396f08c90c4fc81ab83cf95e72a7dac38746d019f522fd639`

## Tests and build

Focused Common presentation test:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :common:desktopTest \
  --tests chat.simplex.common.views.chat.NomeConversationPresentationTest \
  --no-daemon --no-parallel --max-workers=1
```

Result: `BUILD SUCCESSFUL in 1m 13s`; 13 tasks evaluated, 6 executed and 7 up-to-date.

Full Common/Android build gate:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :common:desktopTest :android:testDebugUnitTest \
  :android:assembleDebugAndroidTest :android:assembleDebug \
  --no-daemon --no-parallel --max-workers=1
```

Result: `BUILD SUCCESSFUL in 1m 32s`; 107 tasks evaluated, 28 executed and 79 up-to-date.

After adding the timeline regression guard, the focused two-test class passed and the complete Android unit-test task was rerun:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :android:testDebugUnitTest \
  --no-daemon --no-parallel --max-workers=1
```

Result: `BUILD SUCCESSFUL in 1m 28s`; 44 tasks evaluated, 20 executed and 24 up-to-date.

Existing Kotlin deprecation, manifest namespace/extractNativeLibs, and SDK XML compatibility messages remained warnings; all final commands exited 0. `git diff --check` and the exact staged diff check passed.

## Artifact

- Main APK: `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk`
- Bytes: `343322655`
- SHA-256: `5a859bab21d57fe3df61519f729749ff61d4c104944ad904e11459dc60dab63c`
- `aapt dump badging`: package `im.nome.app.dev`, label `Nome`, versionName `6.5.6`, versionCode `372`, minSdk 28, targetSdk 35, native code `arm64-v8a`
- Signature: Android Debug certificate; APK Signature Scheme v2 verification PASS
- Signer certificate SHA-256: `b2fbf7616a337889d9aba6b4f3ad2680c8e83b8d4e25108f82068e873e7ae5b0`
- Android-test APK: `4135910` bytes; SHA-256 `6d23fe19011c4182c2edea902e271d6439c4395c014dcdcb7b315d9d4a8f12e6`

## Preserved-data installation

Both vivo devices required their normal third-party-app risk acknowledgement. The checkbox and `Continue installation` action were completed, and both host commands returned `Performing Streamed Install` / `Success`.

| Device | Installed result | Data-container evidence |
|---|---|---|
| `3106403166006XM` / vivo V2048A | code 372; update 2026-07-30 18:05:40 | first install remains 2026-07-29 21:44:42; profile `NomeQA-A361`, prior chats, and channel remained visible |
| `9590146717002S1` / vivo V2047A | code 372; update 2026-07-30 18:05:47 | first install remains 2026-07-29 21:44:42; profile `NomeQA-B361`, prior chats, and channel remained visible |

No uninstall, data clear, container replacement, or test APK installation was performed.

## Real two-device channel order E2E

The existing real channel `Nome-C370-Channel-QA` was opened on both preserved profiles. Device A remained the owner/publisher; device B remained a read-only subscriber and displayed `Nome Relay 已连接 17:39`.

Device A published two distinct messages in order:

1. `C372-ORDER-1-180725` at 18:07
2. `C372-ORDER-2-180725` at 18:08

Final UI hierarchy coordinates prove that the later message is below the earlier message on both devices:

| Device / role | First message bounds | Second, newer message bounds | Result |
|---|---|---|---|
| V2048A / owner | `[69,885][560,934]` | `[69,1131][564,1180]` | newer message lower: PASS |
| V2047A / subscriber | `[78,1810][622,1867]` | `[78,2093][627,2150]` | exact A-to-B delivery and newer message lower: PASS |

Subscriber policy remained intact: device B showed `仅查看模式` at `[162,2243][402,2283]`. A final scan of the latest 1000 logcat lines on each device found no `FATAL EXCEPTION`, `Process: im.nome.app.dev`, or `AndroidRuntime` crash entry.

Both devices were cold-started back to the Nome Home screen after validation; `首页` and `Nome-C370-Channel-QA` were present in both final UI hierarchies.

## Verification matrix

| Gate | Status | Evidence |
|---|---|---|
| Channel timeline order | PASS | Two ordered markers on both devices; newer marker has the larger vertical coordinate. |
| Channel broadcast delivery | PASS | Both exact markers published on A and received on B. |
| Relay/subscriber state | PASS | B displayed Nome Relay connected and retained read-only mode. |
| Upgrade/data preservation | PASS | `adb install -r -t`; versionCode 372 with unchanged first-install timestamps and preserved profiles/chats. |
| Common/Android automated tests | PASS | Focused presentation test, full 107-task gate, focused boundary test, and final full Android unit task all exited 0. |
| APK identity/signature | PASS | Nome package/label/version/ABI verified; v2 signature verified. |
| Ordinary direct messaging | NOT RUN | Outside this channel-order regression scope; prior direct-chat state was preserved. |
| Calls | NOT RUN | Outside this channel-order regression scope. |
| Formal clean release source gate | BLOCKED | Preserved unrelated iOS dirty paths; Debug artifact only. |

## Distribution boundary

- State: local internal Debug build, installed and validated on two connected devices
- Remote push: not performed
- Deployment/store/public release: not performed or authorized
- This evidence does not authorize publishing the APK or claiming a formal release build.
