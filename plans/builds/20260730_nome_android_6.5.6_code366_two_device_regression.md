# Nome Android 6.5.6 code 366 two-device regression

## Verdict

- Date/time and timezone: 2026-07-30 11:42:35 CST (+0800)
- Result: **FAIL — not a full-function pass and not release-ready**
- Improvement over code 359: bidirectional direct and group messaging, offline
  recovery, background notification, receiver media opening/playback, message
  edit/reaction/delete synchronization, startup, branded links, and Developer
  Chat Console all passed their stated code 366 checks.
- Release blockers:
  1. Audio-call invitations did not reach the remote client in either direction.
     Two A-side `/_call get` readbacks returned `callInvitations: 0` while B was
     waiting for an answer.
  2. One group message sent after the call/language-reload sequence remained
     local until both clients were cold-started. Delivery after a sender restart
     is recovery evidence, not a reliability pass.
- Configuration boundary: public channel creation is correctly blocked because
  no channel relay is enabled.
- Coverage boundary: a fresh live second-contact connection and deep remote
  desktop pairing were not run.

This verdict is based on automated tests, targeted physical instrumentation,
and two-device end-to-end checks. A successful compile or a recovered message
does not override the two runtime failures above.

## Identity

- Platform / architecture: Android 13 physical devices / arm64-v8a
- Marketing version / version code: 6.5.6 / 366
- Configuration: Debug, internal direct-device regression build
- Application ID: `im.nome.app.dev`
- Application label: `Nome`
- Min / target / compile SDK: 28 / 35 / 35
- Git repository: `/Users/forkman03/project/nome/nome-client`
- Authorized consolidated branch: `codex/nome-v656-unified`
- Full Android/Core source commit:
  `3b060ddd3676f90b39e205be04c8ac8e3414cfee`
- Pre-build `git status --short`: not recorded verbatim. The APK was first
  produced from the controlled Android/Core patch later committed without
  content changes as `3b060ddd3676f90b39e205be04c8ac8e3414cfee`, while the preserved unrelated
  iOS work below was also present.
- Controlled dirty patch: `yes`; this diagnostic build is not a formal release.
- Final source-gate result after the Android/Core commit: `FAIL`, because the
  preserved iOS worktree is not clean. No stash, reset, clean, or deletion was
  used to bypass the gate.

Final `git status --short` before adding this record:

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
?? apps/ios/PolicyTests/NomeLinkBrandingTests.swift
?? apps/ios/SimpleXChat/NomeLinkBranding.swift
?? scripts/ios/test-nome-link-branding.sh
```

## Configuration and dependencies

- `apps/multiplatform/android/build.gradle.kts` SHA-256:
  `820f24add5133f1864342dff7cfed9d6ff0ba140a6a94dba8139bfeb48b92b69`
- `apps/multiplatform/gradle.properties` SHA-256:
  `1b20ab63d8db6f3af2adf3fb758c355200d06f02aac1ffe3769a1e508f8d1da7`
- Ignored `apps/multiplatform/local.properties` SHA-256:
  `ea994433e54298efecff7a2a3870a5903dca3fee914edd09cb3ba1debf8346c3`
- `apps/multiplatform/gradle/wrapper/gradle-wrapper.properties` SHA-256:
  `dd62aa0a67db53097ae5ba0b25c07ec3554f57551638d155cf450593cc8b92f8`
- Packaged arm64 `libapp-lib.so` SHA-256:
  `71464dadd86b369496b83b3935319a67a5742688e76f7d9ac2f9cd9e1ca3e231`
- Toolchain: Gradle 8.12, Kotlin 2.0.21, Android Studio JBR 21.0.10,
  macOS 26.5.2 arm64.

Only non-secret configuration fingerprints are recorded. Configuration values
and credentials are intentionally excluded.

## Changes under test

- Startup and transport sequencing now distinguish already-configured, changed,
  and pending relay state, and fail closed on incomplete configuration.
- Receiver shutdown/restart is bounded; the native receive timeout is shortened
  so a stopped receiver can be joined before restart.
- Android network commands are serialized and stale delayed-offline work is
  rejected before replaying the newest network state.
- Physical-device instrumentation uses the Nome runner/host and avoids the
  production crash bridge only during instrumented execution.
- Developer Chat Console no longer requires an app-bar handler and opens without
  the prior `TerminalView` crash.

The implementation is committed in `3b060ddd3`; its two prerequisite fixes are
`ee95e96e1` (activation-safe startup) and `30b7f65e3` (connection links).

## Build and automated tests

Final command:

```text
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew :common:desktopTest :android:testDebugUnitTest \
  :android:assembleDebugAndroidTest :android:assembleDebug \
  --no-daemon --no-parallel --max-workers=1
```

- Result: `BUILD SUCCESSFUL in 12s`
- Tasks: 107 actionable; 5 executed, 102 up-to-date
- Desktop tests: 219 passed; 0 failures; 0 errors
- Android unit tests: 70 passed; 0 failures; 0 errors
- `NomeAndroidCompatibilityGateTest`: 7 passed within the Desktop total
- Physical instrumentation on vivo V2048A:
  - `NomeNewChatRouteComposeTest`: `OK (2 tests)`
  - `NomeTerminalViewComposeTest`: `OK (1 test)`
- APK and AndroidTest APK assembly: PASS
- Non-fatal build warning: the installed command-line tooling understands SDK
  XML through version 3 while one installed SDK XML file is version 4. It did
  not fail compilation, tests, native build, packaging, or signature checks.

Any intermediate physical-instrumentation failure caused by the test harness is
excluded only after the corrected runner/host rerun passed the exact tests above.

## Artifact

- Diagnostic filename:
  `Nome-Android-6.5.6-code366-20260730-arm64-debug.apk`
- Archived output path:
  `/Users/forkman03/project/nome/deliverables/Nome-Android-6.5.6-code366-20260730-arm64-debug.apk`
- Gradle output path:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk`
- Bytes: 343,306,251
- SHA-256:
  `30f2a54b492bd7502146c1dfc50e4dec1d4871e4e9a43fd84fb86b293755974f`
- Test APK path:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk`
- Test APK bytes: 3,689,426
- Test APK SHA-256:
  `eebeb8eec96eab086c53227d5325f0beefc193650ce648eacc276773cc146d14`
- Native code: `arm64-v8a`
- Signature: APK Signature Scheme v2 PASS; one Android Debug signer
- Signer certificate SHA-256:
  `b2fbf7616a337889d9aba6b4f3ad2680c8e83b8d4e25108f82068e873e7ae5b0`

The Gradle output, archived artifact, and both devices' installed `base.apk`
independently produced the same APK SHA-256.

## Installation and final readback

| Device | Role | Method | Container | Final readback |
|---|---|---|---|---|
| vivo V2048A, `3106403166006XM` | A / NomeQA-A361 | `adb install -r` | preserved | Android 13 API 33, 6.5.6 (366), update 2026-07-30 10:08:40 +0800, exact APK hash |
| vivo V2047A, `9590146717002S1` | B / NomeQA-B361 | `adb install -r` | preserved | Android 13 API 33, 6.5.6 (366), update 2026-07-30 10:05:12 +0800, exact APK hash |

- Both devices report `firstInstallTime=2026-07-29 21:44:42`, proving the
  existing containers were not replaced by these code 366 installations.
- No uninstall, package-data clear, account reset, or destructive reinstall was
  performed.
- Both devices ended with airplane mode off and no per-app locale override;
  Nome was restored to Simplified Chinese during the UI run.
- Both devices passed later cold launches (A 1,501 ms; B 481 ms).
- Final filtered logs contained no Nome fatal exception, Nome ANR, or
  `UnsatisfiedLinkError` match.

## Two-device verification matrix

| Gate | Status | Evidence |
|---|---|---|
| Formal unified source gate | FAIL | Preserved unrelated iOS tracked/untracked work makes the checkout intentionally non-clean; the gate refused it. |
| Desktop and Android unit tests | PASS | 219 Desktop plus 70 Android tests; 0 failures and 0 errors. |
| Android build and packaging | PASS | Final 107-task Gradle command exited 0; APK and test APK hashes recorded above. |
| Physical route/Terminal instrumentation | PASS | New-chat route 2/2 and Terminal 1/1 on V2048A. |
| APK Nome identity, version, ABI, signature | PASS | `aapt` and `apksigner`: `im.nome.app.dev`, Nome, 6.5.6 (366), arm64-v8a, v2 signature valid. |
| Upgrade install and preserved data | PASS | Both installed with `adb install -r`; unchanged first-install times and activated QA profiles remained. |
| Existing activation/profile data | PASS | Both activated profiles and existing chats remained usable after upgrade. |
| Invitation branding and route UI | PASS | Nome identity, one-time invite QR/link, scan/paste controls, and route callbacks rendered and instrumented correctly. |
| Fresh second-contact live connection | NOT RUN | No new second contact was created during this regression. |
| Direct text A to B | PASS | `NomeLive-A2B-c366-0930-1` reached B. |
| Direct text B to A | PASS | Distinct B-to-A marker reached A. |
| Group bidirectional messaging | FAIL | Normal A-to-B and B-to-A group markers passed, but `nomeeditc366x1126` required a later cold restart to leave the sender. |
| Offline queue and reconnect | PASS | `nomeofflinecycle3x1015` stayed absent in airplane mode and arrived immediately after B restored network, without process restart. |
| Background delivery and notification | PASS | `nomebackgroundx1015` reached backgrounded B and produced a marker-specific notification naming NomeQA-A361. |
| Edit synchronization | PASS | A edit `nomeeditedc366x1128` appeared immediately on B after the recovered base message. |
| Reaction synchronization | PASS | B's thumbs-up appeared immediately on A after recovery. |
| Delete-for-everyone synchronization | PASS | Both peers immediately rendered the message as marked deleted after recovery. |
| Image receiver bytes/open | PASS | B opened the received image full-screen. |
| Video receiver bytes/playback | PASS | B playback advanced from 00:02 to 00:01. |
| PDF receiver bytes/open | PASS | B opened `NOME-QA-20260729.pdf` in the device PDF activity. |
| Voice receiver bytes/playback | PASS | B playback produced a MediaPlayer start and stop interval. |
| Audio-call invitation A to B | FAIL | B displayed no incoming UI or notification after reconnect. |
| Audio-call invitation B to A | FAIL | B waited for an answer; A displayed no incoming UI/notification and `/_call get` twice showed `callInvitations: 0`. |
| Call hangup cleanup | PASS | Both outgoing attempts were ended cleanly; no CallActivity remained active. |
| Public channel create/join | BLOCKED | UI truthfully reports that no channel relay is configured and disables creation. |
| Developer Chat Console | PASS | Historical terminal log rendered and manual `/_call get` returned without the previous Terminal crash. |
| Main Nome navigation and locale | PASS | Nome-branded home/new-chat/settings/developer paths opened; Simplified Chinese restored on both devices. |
| Remote desktop deep pairing | NOT RUN | Initial entry was inspected previously; no desktop QR/pairing session was in scope. |
| Destructive reinstall/recovery | NOT RUN | Deliberately excluded to preserve profiles and data. |

## Failure evidence

### P0/P1: call invitation delivery/signaling

The first B-to-A attempt recorded an outbound `/_call invite @2` followed by
`cmdOk`, while B remained at `等待答复中……`. A received neither incoming UI nor
notification. A manual `/_call get` returned `callInvitations: 0`. After both
clients were language-reloaded/reconnected, a second B-to-A attempt produced the
same A-side `callInvitations: 0`. A reverse A-to-B attempt also produced no B-side
incoming UI or notification. This is a stable invitation delivery/signaling
failure in the tested state, not merely a missing button or denied notification.

### P1: sender transport stalls after reload/call sequence

The fresh group marker `nomeeditc366x1126` did not reach B when sent. It arrived
only after both clients were force-stopped and cold-started. Edit, reaction, and
delete synchronization then worked immediately. The recovery narrows the defect
to connection/transport lifecycle reliability but does not prove its exact root
cause.

## Distribution boundary

- State: local internal Debug build and direct physical-device test
- Git push: not performed
- Tag, deployment, TestFlight/store upload, production change, public release,
  or public publication: not performed
- This record and `3b060ddd3` are local commits until separately authorized.
- The APK must not be represented as a release candidate while the source gate,
  call invitation, and sender-stall failures remain.

## Evidence index

- This record:
  `/Users/forkman03/project/nome/nome-client/plans/builds/20260730_nome_android_6.5.6_code366_two_device_regression.md`
- Desktop test XML:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/common/build/test-results/desktopTest`
- Android unit-test XML:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/test-results/testDebugUnitTest`
- APK and AndroidTest APK paths and SHA-256 values: Artifact section above
- Source-gate evidence: Identity section and the exact final dirty status above
- Release/deployment readback: not applicable; no release or deployment occurred
