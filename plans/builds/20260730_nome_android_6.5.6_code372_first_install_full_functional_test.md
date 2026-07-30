# Nome Android 6.5.6 code 372 fresh-install full-functional test

## Verdict

**FAIL / partial pass.** The authorized two-device destructive reinstall and
first-use test completed. Fresh installation, production activation, direct
messaging, notifications, attachments, voice messages, groups, audio calls,
video calls, navigation, Nome branding, and the official server defaults all
passed. A newly created public channel did not complete its subscriber relay
connection and did not deliver broadcasts to the second device. Therefore this
run is not an all-pass Android release acceptance.

The channel subscriber/broadcast defect is a release blocker for public-channel
functionality. This record does not claim its code or backend root cause.

## Identity and controlled source boundary

- Device test window: 2026-07-30 19:17-20:52 CST (+0800)
- Final automated verification: 2026-07-30 21:01-21:08 CST (+0800)
- Git repository: `/Users/forkman03/project/nome/nome-client`
- Authorized branch: `codex/nome-v656-unified`
- Accepted unified ancestor: `124863fe607eb84c177f8ccce7c76793f1ceeb55`
- Tested application source commit: `2f40e485c8ac22703a1765078a84292fb51b5e3c`
- Marketing version / version code: 6.5.6 / 372
- Application ID / label: `im.nome.app.dev` / `Nome`
- Architecture / configuration: arm64-v8a / Debug
- QA helper commit, added after the tested main APK was built:
  `44be3829130217e5f176583aea5d215b0456f815`

Preserved unrelated iOS tracked and untracked changes remained outside both
commits and were not reset, stashed, staged, or modified by this Android run.
The mixed worktree still blocks a formal clean-source release assertion.

## Artifacts

Main APK:

- Path: `apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk`
- Bytes: `343322655`
- SHA-256: `5a859bab21d57fe3df61519f729749ff61d4c104944ad904e11459dc60dab63c`
- Readback: package `im.nome.app.dev`, label `Nome`, versionName `6.5.6`,
  versionCode `372`, native code `arm64-v8a`

Android-test APK:

- Path: `apps/multiplatform/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk`
- Bytes: `4135910`
- SHA-256: `d46811f1f7c5751b35d725bdc936d6da77a905cbd7b51431cddb57331d5e2fa1`
- The QA bootstrap helper compiled into this APK. Its physical-device install
  was not completed because both vivo devices required OEM account/fingerprint
  confirmation for the test package. Installation was cancelled and no device
  security control was relaxed. The activation steps below were completed in
  the real application UI instead.

## Authorized destructive reinstall and first use

The user explicitly authorized removal of the existing application and a true
first-install test. Both the main package and any prior Android-test package
were uninstalled from both devices before the main APK was installed without
update semantics.

| Device | Role | Fresh-install evidence | First identity |
|---|---|---|---|
| vivo V2048A, `3106403166006XM` | A | first and last install time both `2026-07-30 19:17:10` | `NomeFresh-A372` |
| vivo V2047A, `9590146717002S1` | B | first and last install time both `2026-07-30 19:17:10` | `372002` |

Both devices completed the welcome screen, identity creation, background
service setup, notification permission, and privacy onboarding from empty app
data.

## Activation and default configuration

Two one-device production internal-QA invitations were generated under the
batch `Android code372 fresh-install QA 20260730`. Invitation values and contact
or channel links are intentionally excluded from this record.

Both real app redemptions succeeded. Each device persisted the encrypted
activation state with the expected preference keys, including
`token_ciphertext`, `token_iv`, `entitlement`, `installation_id`, `policy`, and
`policy_refresh_at`. Settings on both devices displayed an activation end date
of 2026-08-29.

The administration page created both invitations successfully. A final
administration-list reread was not obtained after the control session timed
out; the acceptance evidence is the two successful real-app redemptions plus
the encrypted entitlement and Settings readback on both devices.

Fresh-install Settings also showed the Nome defaults:

- `Nome Relay`
- `smp.nome.im`
- `xftp.nome.im`

## Two-device functional matrix

| Gate | Status | Evidence |
|---|---|---|
| Destructive uninstall and fresh install | PASS | Both prior packages removed; both devices received code 372 as a new install with identical first/last install timestamps. |
| First-run onboarding | PASS | New identities, privacy onboarding, notification permission, and background service completed on both devices. |
| Production invitation activation | PASS | Two separate real redemptions; encrypted credential and entitlement persisted; Settings showed activation through 2026-08-29. |
| Nome identity and official defaults | PASS | Label `Nome`, About `Nome Android 客户端` / `6.5.6（372）`, and Nome official SMP/XFTP entries verified. |
| New contact connection | PASS | A created a one-time invitation; B joined; the connection completed after a cold restart on both devices. |
| Direct text A to B | PASS | B received `A2B-FIRST-372-20260730-2028`. |
| Direct text B to A | PASS | A received `B2A-FIRST-372-20260730-2028`. |
| Background notification and reopen | PASS | B was backgrounded; its Nome message notification contained `BG-A2B-372-20260730-2029`; the same message was present after reopen. |
| Image A to B | PASS | B received the image inline and opened the full-screen Nome image viewer. |
| Video A to B | PASS | B explicitly downloaded and opened the video; SurfaceView plus hardware AVC decoder metrics proved playback. |
| PDF A to B | PASS | B downloaded the 272376-byte fixture and opened `nome372-test.pdf` in the device PDF viewer from the app. |
| Voice message A to B | PASS | A recorded and sent a four-second message; B playback advanced from 00:04 to 00:01. |
| Group create/invite/join | PASS | A created `NOME-QA-GROUP-372`; B accepted and changed from read-only pending state to connected. |
| Group text A to B | PASS | B received `GROUP-A2B-372-20260730-2041`. |
| Group text B to A | PASS | A received `GROUP-B2A-372-20260730-2042`. |
| New public channel create/join | PASS | A created `NOME-QA-CHANNEL-372` using the default Nome Relay; B parsed the actual Nome SMP invitation and entered subscriber read-only mode. |
| Channel owner local timeline order | PASS | A published markers 1, 2, and 3; their vertical positions were 1492, 1738, and 1984, so the newest item remained at the bottom. |
| Channel subscriber relay connection | **FAIL** | B remained `0 个中继已连接，共 1 个`, including after a cold restart and more than twelve minutes. |
| Channel broadcast A to B | **FAIL** | B received none of the three ordered channel markers and the channel row remained disabled/read-only. |
| Channel two-device order | **FAIL** | Owner-side presentation was correct, but subscriber delivery never occurred, so receiver-side ordering cannot pass. |
| Audio call A to B | PASS | B received and accepted; both devices showed connected/end-to-end-encrypted state and both audio-service dumps entered `VOICE_COMMUNICATION`; call history was recorded. |
| Video call A to B | PASS | Both devices showed connected/end-to-end-encrypted-via-relay state; both cameras connected; VP8 encode/decode metrics proved real bidirectional video media. |
| Navigation and Settings | PASS | Home, Contacts, Settings, activation, backup/migration, desktop pairing, privacy/security, server/Tor, language/display, Help, and About opened. |
| Product crash or ANR | PASS | Exit-info on both devices contained only the test's user-requested force stops; no `im.nome.app.dev` crash or ANR was present. |

The one observed `AndroidRuntime FATAL` entry belonged to the shell
`uiautomator` AccessibilityNodeInfo dumper while inspecting the system document
picker, not to process `im.nome.app.dev`; it is excluded from product crash
results. Screenshot capture returned empty files, so the acceptance record uses
UI hierarchy, package manager, notification, media service, camera service,
audio service, exit-info, and app-state evidence instead.

## Channel blocker detail

Device A published, in order:

1. `CHANNEL-ORDER-1-372-2045`
2. `CHANNEL-ORDER-2-372-2045`
3. `CHANNEL-ORDER-3-372-2045`

Device A displayed all three in the expected bottom-growing order. Device B
successfully parsed and joined the channel link but never advanced beyond zero
connected relays and never displayed any marker. Direct TCP reachability to the
server port and the official relay defaults were independently present on both
devices, but those facts do not establish the failing layer. Further diagnosis
must separate client subscription state, invitation metadata, and relay/backend
state before assigning a root cause.

This fresh-channel failure does not erase the earlier successful preserved-data
channel test recorded in
`plans/builds/20260730_nome_android_6.5.6_code372_channel_timeline_order_e2e.md`.
It does prove that a brand-new first-install channel scenario is not currently
reliable enough to accept.

## Automated verification

The activation bootstrap helper was compiled before commit:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew --no-daemon --no-parallel --max-workers=1 \
  :android:assembleDebugAndroidTest
```

Result: `BUILD SUCCESSFUL in 24s`.

Focused channel presentation regression after the device run:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew --no-daemon --no-parallel --max-workers=1 \
  :common:desktopTest \
  --tests chat.simplex.common.views.chat.NomeConversationPresentationTest
```

Result: `BUILD SUCCESSFUL in 8s`; 13 actionable tasks, 3 executed and 10
up-to-date.

The first combined Common/Android command completed the Common test and then
failed during Android source compilation with `OutOfMemoryError: GC overhead
limit exceeded` under the repository's 2 GiB Gradle heap. No Android test case
had failed. The Android unit task was rerun separately with a 6 GiB Gradle heap:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew --no-daemon --no-parallel --max-workers=1 \
  '-Dorg.gradle.jvmargs=-Xmx6144m -Dfile.encoding=UTF-8' \
  :android:testDebugUnitTest
```

Result: `BUILD SUCCESSFUL in 1m 18s`; 44 actionable tasks, 13 executed and 31
up-to-date. Existing Kotlin/Android deprecation messages remained warnings.
`git diff --check` and exact staged-diff checks passed.

## Commit and distribution boundary

- QA helper commit: `44be3829130217e5f176583aea5d215b0456f815`
- Evidence commit: recorded by the commit containing this file
- Remote push: not performed
- Deployment, store submission, production change, and public release: not performed
- Backup and retained worktrees: untouched
