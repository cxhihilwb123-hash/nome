# Nome Android 6.5.6 code 371 default channel relay E2E

## Identity

- Date/time and timezone: 2026-07-30 17:07-17:42 CST (+0800)
- Platform / architecture: Android arm64-v8a
- Marketing version / build number: 6.5.6 / versionCode 371
- Configuration: Debug, local two-device validation
- Application ID: `im.nome.app.dev`
- Git repository: `/Users/forkman03/project/nome/nome-client`
- Authorized consolidated branch: `codex/nome-v656-unified`
- Accepted unified ancestor: `124863fe607eb84c177f8ccce7c76793f1ceeb55` (`git merge-base --is-ancestor`: PASS)
- Relay seeding commit: `0910d560ddda575e5130f2acb8cd9462d06a4838`
- Invitation-link commit and full Android source state: `16ba0c344be6b7349e02e1dc8e416dbec044aea1`
- Controlled dirty patch: `yes`; this is an internal Debug build, not a formal release. The worktree contained preserved unrelated iOS changes, and the four code-371 Android paths were uncommitted at packaging time. They were committed unchanged as `16ba0c344...` before this record.
- Source-gate result: BLOCKED only by the preserved iOS tracked/untracked paths. `scripts/release/check-nome-build-source.sh` correctly exited 1 with `worktree is not clean`; no iOS path was staged, changed, reset, stashed, or removed by this work.

Android packaging paths at build time:

```text
 M apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/lifecycle/NomeStartupServerConfigurationTest.kt
 M apps/multiplatform/android/src/main/AndroidManifest.xml
 M apps/multiplatform/gradle.properties
?? apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/NomeChannelLinkManifestTest.kt
```

The unrelated preserved iOS status is still present in the final repository status. Exact-path staging was used for both Android commits.

## Default configuration behavior

- The canonical Core preset already defines `Nome Relay`; the currently packaged native library predates that source change, so `0910d560...` adds an Android compatibility migration.
- On the first successful startup for each local or remote user, the migration adds the exact Nome relay to the Nome operator, or to its compatibility custom server group when the stale native core does not expose that operator.
- The migration records the user only after configuration succeeds. It does not duplicate the relay and does not re-enable a relay that the user deliberately disabled or deleted.
- The default relay is therefore part of every APK built from these commits; it does not require a post-install manual server entry.
- `16ba0c344...` adds `smp.nome.im` to the Android browsable HTTP/HTTPS invitation filter so a shared Nome channel link opens Nome directly.

## Configuration and dependencies

- `apps/multiplatform/gradle.properties` SHA-256: `330f7c5aab7c00baa8f264d940d5a97cd6d68eb323bcb4048e71c19715822064`
- `apps/multiplatform/android/src/main/AndroidManifest.xml` SHA-256: `69d20d9cce7f86b6ea0e2d1777af4bd643750e4c2b1c1a58136b10a00e3f3607`
- Packaged `lib/arm64-v8a/libsimplex.so` SHA-256: `91d45817a444d2344877288561e218577239d9a80a504dfa27e8fec9fe64e309`
- Gradle: 8.12; Kotlin: 2.0.21; JVM: JetBrains Runtime 21.0.10
- Android Debug Bridge: 1.0.41 / 37.0.0-14910828

## Tests and build

The following command completed with `BUILD SUCCESSFUL in 2m 35s`; 107 tasks were evaluated (45 executed, 62 up-to-date):

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :common:desktopTest :android:testDebugUnitTest \
  :android:assembleDebugAndroidTest :android:assembleDebug \
  --no-daemon --no-parallel --max-workers=1
```

- Common relay migration tests cover first seed, idempotence, disabled-relay preservation, deleted-relay preservation, preference persistence, and the exact Nome relay constants.
- `aapt dump xmltree` found `android.intent.action.VIEW`, `BROWSABLE`, and `android:host="smp.nome.im"` in the packaged manifest.
- `NomeChannelLinkManifestTest` passed independently on both devices.
- `cmd package resolve-activity` returned `im.nome.app.dev/chat.simplex.app.MainActivity` on both devices for `https://smp.nome.im/c#opaque`.
- `NomeStartupServerConfigurationTest` did not complete on either vivo device after entering the test body; the host sessions were stopped without an assertion failure. It is not counted as PASS. Device UI relay testing and the real create/join/broadcast round below are the runtime acceptance evidence.
- Existing Kotlin, manifest namespace/extractNativeLibs, and SDK XML compatibility warnings remained warnings; the command exited 0.

## Artifact

- Output path: `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk`
- Bytes: `343322655`
- SHA-256: `ee4fe06e29180270492d13e3e4d6ca4060ec7c6f9686cbae12e3d8f6d64d631c`
- `aapt dump badging`: package `im.nome.app.dev`, versionName `6.5.6`, versionCode `371`, minSdk 28, targetSdk 35
- Signature: Android Debug certificate; APK Signature Scheme v2 verification PASS
- Android-test APK SHA-256: `6d23fe19011c4182c2edea902e271d6439c4395c014dcdcb7b315d9d4a8f12e6`

## Installation

| Device | OS | Result | Container evidence |
|---|---|---|---|
| `3106403166006XM` / vivo V2048A | Android 13 / API 33 | `adb install -r` PASS; code 371; update 17:32:43 | preserved; first install remains 2026-07-29 21:44:42 |
| `9590146717002S1` / vivo V2047A | Android 13 / API 33 | `adb install -r` PASS; code 371; update 17:32:54 | preserved; first install remains 2026-07-29 21:44:42 |

Both application preference files contain `NomeDefaultChatRelaySeededUsers` with `local:1: true`. Existing profiles and prior chats remained visible after the upgrade.

## Real channel E2E

1. On device A, Settings -> Servers & Tor -> Nome official servers displayed `Nome Relay`, `smp.nome.im`, and `xftp.nome.im`. `For new channels` was enabled. `Test relay` completed and the tested server configuration was saved.
2. On device B, the same relay presentation, enablement, live test, and save flow passed.
3. Device A opened New connection -> Public channel. The creation page reported one enabled chat relay and created `Nome-C370-Channel-QA` successfully. The generated invitation used the `https://smp.nome.im/c#...` Nome host; its opaque secret is intentionally omitted here.
4. Device B was cold-started with the shared invitation. Android resolved it directly to Nome, the preview showed the correct channel and subscriber count, and `Open new channel` reached the official join flow.
5. Device B joined the channel and correctly entered read-only subscriber mode.
6. Device A published `C371-CHANNEL-A2B-PASS-1739`.
7. Device B displayed the exact marker, `Nome Relay connected 17:39`, and the expected `Only channel owner can post` read-only policy.

This proves default relay presence, relay connectivity, channel creation, invitation sharing/deep-linking, subscriber join, owner broadcast, and receiver delivery on two independent devices.

## Verification matrix

| Gate | Status | Evidence |
|---|---|---|
| Activation/invitation policy | NOT RUN | Existing activation state was preserved; this task tested channel invitation routing only. |
| Ordinary direct messaging | NOT RUN | No new direct-message round was needed for this channel-scoped build. |
| Channel default relay configuration | PASS | Both devices showed, enabled, tested, and saved Nome Relay; seed preference recorded. |
| Channel create/join/presentation | PASS | Real A-create / B-deep-link / B-join flow completed. |
| Channel broadcast delivery | PASS | Exact A->B marker received with relay-connected state. |
| Calls | NOT RUN | Outside this channel configuration scope. |
| Upgrade | PASS | `adb install -r`; both original first-install timestamps and existing data remained. |
| Reinstall/recovery | NOT RUN | No uninstall or container replacement was performed. |
| Clean formal release source gate | BLOCKED | Preserved unrelated iOS dirty paths; this Debug APK is not a public release artifact. |

## Distribution boundary

- State: local internal Debug build and two-device test
- Remote push: not performed
- Deployment/store/public release: not performed or authorized
- This result does not authorize publishing the APK and does not make a formal clean-release claim.
