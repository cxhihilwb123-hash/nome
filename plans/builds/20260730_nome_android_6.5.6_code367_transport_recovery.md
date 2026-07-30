# Nome Android 6.5.6 code 367 transport recovery and two-device acceptance

## Verdict

- Date/time and timezone: 2026-07-30 13:04:49 CST (+0800)
- Result: **SUPERSEDED / FALSE POSITIVE**. Do not use the historical PASS
  statements below as current Android acceptance evidence.
- A later user-grade two-device reproduction showed that code 367 could still
  leave an online sender on a stale Core transport session after backgrounding:
  the message appeared locally but did not reach the peer until the sender was
  cold-started. Replaying the same current network value was therefore not a
  sufficient recovery mechanism when the native disconnect event was lost or
  delayed.
- This record is retained as historical evidence of what was tested and why the
  earlier verdict was wrong. It is superseded by code 368 and
  `20260730_nome_android_6.5.6_code368_background_recovery.md`, which adds a
  guarded logical network reconstruction and repeats text, voice-message,
  encrypted-call, group, offline-queue, screen-off, and long-background gates
  on both physical devices.
- The two code 366 runtime blockers are closed in this build:
  1. Encrypted audio-call invitations reached the remote device in both
     directions and rejection synchronized back to the caller.
  2. A message sent during the reproduced long-background
     `hostDisconnected` window reached the receiver without either app being
     cold-started.
- Direct messaging, group delivery, physical offline/online queue recovery,
  long-background foreground recovery, preserved-data upgrade, cold launch,
  and post-lifecycle calling passed the code 367 checks below.
- This is not a formal release or a claim that every optional product feature
  was rerun. Fresh-contact creation and remote-desktop pairing were not run;
  public channel creation remains correctly unavailable without a configured
  channel relay. Media receiver/open/playback gates passed in the immediately
  preceding code 366 run and were not repeated for code 367.
- The formal clean-source gate remains **FAIL** because unrelated iOS work is
  intentionally preserved in the same worktree. No reset, clean, stash,
  deletion, or broad staging was used to bypass that boundary.

## Identity

- Platform / architecture: Android 13 physical devices / arm64-v8a
- Marketing version / version code: 6.5.6 / 367
- Configuration: Debug, internal two-device diagnostic build
- Application ID: `im.nome.app.dev`
- Application label: `Nome`
- Min / target / compile SDK: 28 / 35 / 35
- Git repository: `/Users/forkman03/project/nome/nome-client`
- Authorized consolidated branch: `codex/nome-v656-unified`
- Full Android/Core source commit:
  `bbc7fc76d87b530cb23faddd00e0349fef0b7e55`
- Source commit subject:
  `fix(android): recover stale Nome transport sessions`
- Controlled dirty patch during the first build/install: `yes`; the seven-file
  Android/Core patch was then committed without content changes as the source
  commit above.
- Post-commit `:android:assembleDebug` rebuilt/read the same output at source
  commit `bbc7fc76d87b530cb23faddd00e0349fef0b7e55` and retained the identical
  SHA-256 recorded below.

The recorded build-time status contained the Android/Core patch plus preserved
unrelated iOS work:

```text
 M apps/ios/PolicyTests/NomeActivationPolicyTests.swift
 M apps/ios/Shared/Model/AppAPITypes.swift
 M apps/ios/Shared/Model/NomeActivation.swift
 M apps/ios/Shared/Model/SimpleXAPI.swift
 M apps/ios/Shared/Views/Chat/ChatItem/MsgContentView.swift
 M apps/ios/Shared/Views/Chat/ComposeMessage/ComposeView.swift
 M apps/ios/Shared/Views/Helpers/ShareSheet.swift
 M apps/ios/Shared/Views/NewChat/NewChatView.swift
 M apps/ios/Shared/Views/UserSettings/NetworkAndServers/NetworkAndServers.swift
 M "apps/ios/SimpleX (iOS).entitlements"
 M apps/ios/SimpleX.xcodeproj/project.pbxproj
 M apps/ios/SimpleXChat/API.swift
 M apps/ios/SimpleXChat/APITypes.swift
 M apps/ios/SimpleXChat/AppGroup.swift
 M apps/multiplatform/android/src/main/java/chat/simplex/app/SimplexApp.kt
 M apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/helpers/NetworkObserver.kt
 M apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt
 M apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/platform/Platform.kt
 M apps/multiplatform/gradle.properties
?? apps/ios/PolicyTests/NomeLinkBrandingTests.swift
?? apps/ios/SimpleXChat/NomeLinkBranding.swift
?? apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/NomeCoreHostRecoveryState.kt
?? apps/multiplatform/common/src/commonTest/kotlin/chat/simplex/common/model/NomeCoreHostRecoveryStateTest.kt
?? plans/builds/20260730_nome_ios_6.5.6_build376_full_e2e.md
?? scripts/ios/test-nome-link-branding.sh
```

After the source commit, the executable source gate reported only the preserved
iOS tracked/untracked work and ended with:

```text
[FAIL] worktree is not clean; tracked, staged, or untracked files are forbidden
```

## Diagnosis and changes under test

The code 366 failure was reproduced as a native host lifecycle stall rather
than a call-screen defect. After about 65 seconds in the background, the core
reported fallback response type `hostDisconnected`. A send could return local
`newChatItems` before the matching `hostConnected`, while the peer did not
receive the item until a process restart. The application previously logged
these fallback host events as unsupported and foregrounding did not replay the
current validated Android network.

Code 367 therefore:

- recognizes fallback `hostDisconnected` and `hostConnected` response types and
  forwards them to the Android platform layer;
- serializes a validated-network replay when the app returns to the foreground;
- schedules a bounded recovery only while the same disconnect generation is
  still current;
- first replays the current validated network, then, only if the host remains
  stale, reconstructs the logical session with a protected `NONE -> current`
  transition;
- always restores the latest physical network in a `NonCancellable` cleanup if
  a forced logical-offline transition began;
- cancels stale delayed work on reconnect or a newer host-state generation;
- adds pure state tests for reconnect and newer-disconnect invalidation; and
- advances the Android version code from 366 to 367.

The real long-background run reconnected before the delayed escalation was
eligible, so the destructive-looking logical transition did not run in that
normal recovery. Its stale-generation safety is covered by the new unit tests;
no synthetic native host event was represented as a physical-device result.

## Configuration and dependencies

- `apps/multiplatform/android/build.gradle.kts` SHA-256:
  `820f24add5133f1864342dff7cfed9d6ff0ba140a6a94dba8139bfeb48b92b69`
- `apps/multiplatform/gradle.properties` SHA-256:
  `80b739a6a2cd6906601015699d2be9979bc1b440fe6940dc769a3f7252ba81aa`
- Ignored `apps/multiplatform/local.properties` SHA-256:
  `ea994433e54298efecff7a2a3870a5903dca3fee914edd09cb3ba1debf8346c3`
- `apps/multiplatform/gradle/wrapper/gradle-wrapper.properties` SHA-256:
  `dd62aa0a67db53097ae5ba0b25c07ec3554f57551638d155cf450593cc8b92f8`
- Packaged arm64 `libapp-lib.so` SHA-256:
  `71464dadd86b369496b83b3935319a67a5742688e76f7d9ac2f9cd9e1ca3e231`
- Toolchain: Gradle 8.12, Kotlin 2.0.21, Android Studio JBR 21.0.10,
  macOS 26.5.2 arm64.

Only non-secret configuration fingerprints are recorded.

## Build and automated tests

Final automated-test command against the committed source content:

```text
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew :common:desktopTest :android:testDebugUnitTest \
  --no-daemon --no-parallel --max-workers=1
```

- Result: `BUILD SUCCESSFUL in 1m 16s`
- Tasks: 52 actionable; 19 executed, 33 up-to-date
- Desktop tests: 221 passed; 0 failures; 0 errors
- Android unit tests: 70 passed; 0 failures; 0 errors
- New `NomeCoreHostRecoveryStateTest`: 2 passed

APK and AndroidTest APK assembly command:

```text
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew :android:assembleDebugAndroidTest :android:assembleDebug \
  --no-daemon --no-parallel --max-workers=1
```

- Result: `BUILD SUCCESSFUL in 1m 45s`
- Tasks: 93 actionable; 27 executed, 66 up-to-date
- Post-source-commit `:android:assembleDebug` confirmation:
  `BUILD SUCCESSFUL in 1m 12s`; 65 actionable, 19 executed, 46 up-to-date
- `git diff --check` for the seven source/test/version files: PASS
- Non-fatal warning: installed command-line tooling understands SDK XML through
  version 3 while one installed SDK XML file is version 4. It did not fail
  compilation, tests, native build, packaging, or signature verification.

## Artifact

- Diagnostic filename identity:
  `Nome-Android-6.5.6-code367-20260730-arm64-debug.apk`
- Gradle output path:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk`
- Bytes: 332,968,758
- SHA-256:
  `bf688dd40170e29be622f3e1dcd96517678c51d10a4592068c2051d7ee0d9cd2`
- Test APK path:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk`
- Test APK bytes: 3,689,426
- Test APK SHA-256:
  `eebeb8eec96eab086c53227d5325f0beefc193650ce648eacc276773cc146d14`
- `aapt` identity: `im.nome.app.dev`, label `Nome`, 6.5.6 (367),
  `arm64-v8a`
- APK Signature Scheme v2: PASS
- Signer certificate SHA-256:
  `b2fbf7616a337889d9aba6b4f3ad2680c8e83b8d4e25108f82068e873e7ae5b0`

The Gradle APK and both installed `base.apk` files independently produced the
same code 367 SHA-256.

## Installation and final readback

| Device | Role | Method | Container | Final readback |
|---|---|---|---|---|
| vivo V2048A, `3106403166006XM` | A / NomeQA-A361 | `adb install -r` | preserved | Android 13 API 33, 6.5.6 (367), update 2026-07-30 12:44:28 +0800, exact APK hash |
| vivo V2047A, `9590146717002S1` | B / NomeQA-B361 | `adb install -r` | preserved | Android 13 API 33, 6.5.6 (367), update 2026-07-30 12:44:33 +0800, exact APK hash |

- Both report `firstInstallTime=2026-07-29 21:44:42`; no uninstall, package
  data clear, account reset, or destructive reinstall occurred.
- Existing activation, profiles, direct chats, and groups remained present.
- Final relaunch after diagnostic restoration displayed both QA identities and
  the shared `Nome-C364-FullGate` group.
- B network controls were restored to Wi-Fi enabled, mobile data enabled, and
  airplane mode off (`1/1/0`).
- Temporary diagnostics were restored to their recorded pre-test states:
  A `DeveloperTools=true`, B `DeveloperTools=false`, both `LogLevel=WARNING`,
  and both temporary `log.tag.SIMPLEX` properties reset from debug to info.

## Two-device verification matrix

| Gate | Status | Evidence |
|---|---|---|
| Unified authority and source ancestry | PASS | Branch `codex/nome-v656-unified`; implementation commit `bbc7fc76d87b530cb23faddd00e0349fef0b7e55` descends from the accepted unified history. |
| Formal clean-source gate | FAIL | Preserved unrelated iOS tracked/untracked work remains; the gate refused the checkout. |
| Desktop and Android unit tests | PASS | 221 Desktop plus 70 Android tests; 0 failures and 0 errors. |
| Android build, packaging, ABI, and signature | PASS | Post-commit assemble passed; code 367 arm64 APK and v2 signature verified. |
| Preserved-data upgrade | PASS | Both installed with `adb install -r`; first-install times, activated identities, chats, and groups remained. |
| Cold launch after diagnostic restoration | PASS | Both relaunched as code 367 and rendered their existing Nome QA identities and chats. |
| Direct text A to B | PASS | Marker `367124601` appeared on B immediately. |
| Direct text B to A | PASS | Marker `367124602` appeared on A immediately. |
| Audio call B to A | PASS | A rendered NomeQA-B361 encrypted incoming call; rejection returned `callEnded` to B. |
| Audio call A to B | PASS | B rendered NomeQA-A361 encrypted incoming call; rejection returned `callEnded` to A. |
| Long-background disconnect-window send | PASS | After 65 seconds B reproduced `hostDisconnected`; foreground replay ran before chat load and `367125001` reached A without either app restarting. |
| Post-lifecycle audio call | PASS | B called A after the long-background and network cycles; A received and rejected, B received `callEnded`. |
| Physical offline queue and recovery | PASS | B changed to network `NONE`; `367125101` remained absent, then arrived automatically after Wi-Fi validation and `hostConnected`. |
| Group delivery after lifecycle recovery | PASS | A sent `367125201`; B's `Nome-C364-FullGate` row updated immediately. |
| Persistent-disconnect escalation on a real device | NOT RUN | Normal device reconnect completed before escalation; stale-generation unit coverage passed and no synthetic event was claimed as physical evidence. |
| Fresh second-contact creation | NOT RUN | Existing connected QA identities were used. |
| Media receive/open/playback on code 367 | NOT RUN | Passed in code 366 and unchanged by this transport patch, but not rerun for code 367. |
| Public channel create/join | BLOCKED | No channel relay is configured; UI correctly disables creation. |
| Remote desktop deep pairing | NOT RUN | No desktop QR/pairing session was started. |
| Destructive reinstall/recovery | NOT RUN | Excluded to preserve profile and message data. |

## Critical runtime evidence

### Long-background recovery

On B, the key sequence was:

```text
12:51:47.522 chatRecvMsg: * hostDisconnected
12:51:47.529 fallback response type: hostDisconnected
12:51:47.541 onStateChanged: ON_START
12:51:47.554 sendCmd: apiSetNetworkInfo
12:51:47.638 sendCmd: apiGetChats
12:51:48.177 sendCmd: apiSendMessages
12:51:48.213 response type newChatItems
12:51:49.281 chatRecvMsg: * hostConnected
```

A received `newChatItems` at `12:51:49.980` and rendered marker `367125001`
without a cold start. This directly contrasts with the reproduced code 366
failure, where comparable items remained absent until restart.

### Physical network loss and recovery

On B:

```text
12:52:43.027 Network changed: UserNetworkInfo(networkType=NONE, online=false)
12:52:43.493 chatRecvMsg: * hostDisconnected
12:52:46.041 sendCmd: apiSetNetworkInfo
12:53:20.327 Network changed: UserNetworkInfo(networkType=WIFI, online=true)
12:53:20.330 sendCmd: apiSetNetworkInfo
12:53:21.324 chatRecvMsg: * hostConnected
12:53:21.899 chatRecvMsg: newChatItems
```

Marker `367125101` was visible on A while B was offline, absent on B during the
offline interval, and visible on B after recovery.

### Calls

- A to B: A `apiSendCallInvitation -> cmdOk`; B received `callInvitation`,
  rendered `端到端加密语音通话` for NomeQA-A361, and rejected; A received
  `callEnded`.
- B to A after lifecycle testing: B `apiSendCallInvitation -> cmdOk`; A received
  `callInvitation`, rendered the encrypted call for NomeQA-B361, and rejected;
  B received `callEnded`.

## Distribution boundary

- State: local internal Debug build and direct physical-device test
- Source commit: local only
- Git push: not performed
- Tag, deployment, store upload, production mutation, public release, or public
  publication: not performed
- A formal release still requires a clean authorized source checkout and
  separate explicit distribution authorization.

## Evidence index

- This record:
  `/Users/forkman03/project/nome/nome-client/plans/builds/20260730_nome_android_6.5.6_code367_transport_recovery.md`
- Source implementation commit:
  `bbc7fc76d87b530cb23faddd00e0349fef0b7e55`
- Previous failing baseline:
  `/Users/forkman03/project/nome/nome-client/plans/builds/20260730_nome_android_6.5.6_code366_two_device_regression.md`
- Desktop test XML:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/common/build/test-results/desktopTest`
- Android unit-test XML:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/test-results/testDebugUnitTest`
- APK and AndroidTest APK paths and SHA-256 values: Artifact section above
- Source-gate evidence: Identity section above
- Release/deployment readback: not applicable; no release or deployment occurred
