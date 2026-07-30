# Nome Android 6.5.6 code 368 background recovery and two-device acceptance

> **SUPERSEDED — FALSE POSITIVE (2026-07-30 15:40 CST).** Subsequent
> no-restart testing reproduced the user's failure on code 368: both devices
> retained ESTABLISHED TCP sockets to the SMP server, but messages and video
> invitations remained queued until both chat cores were cold-started. The
> guarded logical `NONE -> current network` reconstruction returned successful
> Core responses but did not recover the already-stale transport. The durable
> root cause was the missing Nome private-SMP heartbeat configuration after the
> package migration: code 368 used the 1200-second / 3-ping defaults instead of
> the previously proven 120-second / 1-ping values with TCP keepalive. See
> `20260730_nome_android_6.5.6_code369_smp_heartbeat_reliability.md` for the
> corrective build and final evidence. Historical observations below are
> retained as evidence, not as a current acceptance verdict.

## Verdict

- Date/time and timezone: 2026-07-30 15:03 CST (+0800)
- Result: **SUPERSEDED / FALSE POSITIVE.** The earlier short-window acceptance
  did not cover the repeatable 5–6 minute stale-channel window.
- The user-reported regression was reproduced before the fix: a message could
  appear locally on the sender while the peer received nothing, then flush only
  after the sender was cold-started. Code 367's same-value foreground network
  replay did not reliably replace a stale Core transport session.
- Code 368 performs a guarded logical `NONE -> current validated network`
  reconstruction after a long background interval. The physical-device gate
  proved that a message sent after this reconstruction reached the peer without
  restarting either app.
- Both vivo devices also require Nome's per-app battery setting
  **允许后台高耗电**. The standard Android battery whitelist and foreground
  service alone did not prevent the vivo OEM freezer from suspending delivery.
- This is not a claim that every optional product feature was tested. Fresh
  contact creation and remote desktop pairing were not run. Public channel
  creation remains unavailable without a configured channel relay.
- The formal clean-source gate remains **FAIL** only because unrelated iOS work
  is intentionally preserved in this worktree. It was not staged, changed,
  reset, cleaned, stashed, or deleted during this Android repair.

This record supersedes the false-positive code 367 acceptance verdict in
`20260730_nome_android_6.5.6_code367_transport_recovery.md`.

## Identity

- Repository: `/Users/forkman03/project/nome/nome-client`
- Branch: `codex/nome-v656-unified`
- Android/Core source commit:
  `111f6446a47cf36c5b4d81fcd4a5417e51728499`
- Commit subject: `fix(android): rebuild stale sessions after background`
- Source commit time: 2026-07-30 15:03:26 +0800
- Marketing version / version code: 6.5.6 / 368
- Application ID: `im.nome.app.dev`
- Application label: `Nome`
- Platform / architecture: Android 13 physical devices / arm64-v8a
- Configuration: Debug, internal two-device diagnostic build
- Min / target / compile SDK: 28 / 35 / 35

The physical devices were installed from the controlled five-file Android/Core
patch immediately before it was committed. The post-commit build produced the
same APK SHA-256, proving that the installed artifact matches the committed
source content.

## Root cause and repair

Two independent device/runtime conditions contributed to the observed failure:

1. vivo's OEM freezer could freeze Nome despite the Android battery whitelist
   and foreground service. Both devices were changed in the vivo per-app battery
   UI to **允许后台高耗电** and then rechecked with screen-off/background runs.
2. After a long background interval, Core could retain a stale logical
   transport session when the native host-disconnect signal was lost or stale.
   Reapplying the same `WIFI/online=true` value did not necessarily reconstruct
   that session; cold-starting the sender did, which explained why queued
   messages appeared after restart.

Code 368 therefore:

- records background duration with Android's monotonic elapsed-realtime clock;
- reconstructs the logical network only after at least 60 seconds in the
  background;
- serializes the `NONE -> 3 second delay -> latest current network` sequence
  through the existing Core command mutex;
- applies a five-minute reconstruction cooldown;
- skips reconstruction if chat is not running, the device is offline, or an
  active/invited/switching call is present;
- retains normal current-network replay for shorter or unsafe resumes;
- adds three pure policy tests for the threshold, safety gates, cooldown, and
  elapsed-clock reset; and
- advances Android version code from 367 to 368.

## Automated verification

Unit-test command:

```text
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew :common:desktopTest :android:testDebugUnitTest \
  --no-daemon --no-parallel --max-workers=1
```

- Result: `BUILD SUCCESSFUL in 2m 11s`
- Tasks: 52 actionable; 14 executed, 38 up-to-date
- Desktop tests: 224 passed; 0 skipped, failures, or errors
- Android unit tests: 70 passed; 0 skipped, failures, or errors
- New `NomeForegroundNetworkRecoveryPolicyTest`: 3 passed

APK and AndroidTest assembly command:

```text
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew :android:assembleDebugAndroidTest :android:assembleDebug \
  --no-daemon --no-parallel --max-workers=1
```

- Result: `BUILD SUCCESSFUL in 1m 39s`
- Tasks: 93 actionable; 27 executed, 66 up-to-date
- Post-source-commit `:android:assembleDebug`: `BUILD SUCCESSFUL in 10s`;
  65 actionable, 5 executed, 60 up-to-date
- `git diff --check` for the five implementation/test/version files: PASS
- Non-fatal build warning: one installed Android SDK XML is version 4 while the
  native tooling reports support through version 3. Compilation, tests, native
  build, packaging, and signing all completed successfully.

## Artifact

- Gradle output:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk`
- Bytes: 332,968,758
- SHA-256:
  `fe073788c05323bde54541d69fea68ebd124df908819c2ed9d870f5b44b5a170`
- `aapt` identity: `im.nome.app.dev`, label `Nome`, version 6.5.6 (368),
  `arm64-v8a`
- APK Signature Scheme v2: PASS
- Signer certificate SHA-256:
  `b2fbf7616a337889d9aba6b4f3ad2680c8e83b8d4e25108f82068e873e7ae5b0`

The Gradle artifact and both installed `base.apk` files independently returned
the same SHA-256.

## Installation and final device readback

| Device | Test identity | Install/readback | Final state |
|---|---|---|---|
| vivo V2048A, `3106403166006XM` | A / NomeQA-A361 | `adb install -r`; 6.5.6 (368); updated 2026-07-30 14:42:03 +0800; exact APK hash | Android battery whitelist present; vivo **允许后台高耗电**; `isFrozen=false` |
| vivo V2047A, `9590146717002S1` | B / NomeQA-B361 | `adb install -r`; 6.5.6 (368); updated 2026-07-30 14:42:07 +0800; exact APK hash | Android battery whitelist present; vivo **允许后台高耗电**; Wi-Fi/data/airplane `1/1/0` |

- Both retain `firstInstallTime=2026-07-29 21:44:42`; no uninstall, package
  data clear, account reset, or destructive reinstall occurred.
- Existing activation, identities, direct chats, message history, and the
  `Nome-C364-FullGate` group were preserved.
- Temporary diagnostics were restored after testing: A
  `DeveloperTools=true`, B `DeveloperTools=false`; both `LogLevel=WARNING` and
  `log.tag.SIMPLEX=I`.

## Two-device verification matrix

| Gate | Status | Evidence |
|---|---|---|
| Unified authority and ancestry | PASS | Source commit `111f6446a47cf36c5b4d81fcd4a5417e51728499` is on `codex/nome-v656-unified` and descends from the accepted unified authority. |
| Formal clean-source gate | FAIL | Preserved unrelated iOS tracked/untracked work remains; it was excluded from both Android commits. |
| Automated tests | PASS | 224 Desktop plus 70 Android tests; 0 skipped, failures, or errors. |
| Build, ABI, package identity, signature | PASS | code 368 arm64 APK built; package `im.nome.app.dev`, label `Nome`, v2 signature verified. |
| Preserved-data upgrade | PASS | Both upgraded with `adb install -r`; original first-install time, identities, chats, and group remained. |
| Cold launch | PASS | Both devices rendered their existing Nome QA identities and chat state. |
| Foreground direct text A to B | PASS | Marker `368144601` appeared on B. |
| Foreground direct text B to A | PASS | Marker `368144602` appeared on A. |
| Voice message A to B | PASS | 22-second recording transferred; B opened playback and progress reached 00:04. |
| Voice message B to A | PASS | 30-second recording transferred; A opened playback and progress reached 00:05. |
| Long-background foreground reconstruction | PASS | B resumed after 69,316 ms; guarded logical reconstruction ran; marker `368145001` reached A immediately without either app restarting. |
| Screen-off/background delivery | PASS | A remained unfrozen after about 89 seconds; B's `368145301` produced the exact system notification on A. |
| Encrypted audio call B to A | PASS | A received the NomeQA-B361 encrypted incoming-call notification, woke, and synchronized call end. |
| Encrypted audio call A to B | PASS | B rendered NomeQA-A361's encrypted incoming-call UI; A ended and B received `callEnded`. |
| Group delivery A to B | PASS | Marker `368145501` appeared in `Nome-C364-FullGate` on B. |
| Physical offline queue | PASS | B sent while Wi-Fi/data were disabled; the peer did not receive it before restore, then received actual marker `36814701` immediately after validated Wi-Fi and `hostConnected`. |
| Fresh second-contact creation | NOT RUN | Existing connected QA identities were used. |
| Public channel create/join | BLOCKED | No channel relay is configured; UI correctly prevents creation. |
| Remote desktop deep pairing | NOT RUN | No desktop QR/pairing session was started. |
| Destructive reinstall/recovery | NOT RUN | Excluded to preserve user profiles and messages. |

## Critical runtime evidence

### Guarded long-background reconstruction

On B:

```text
14:49:20 lifecycle ON_STOP
14:50:29 lifecycle ON_START
14:50:29 Reconstructing logical Android network after 69316ms in background
14:50:29 sendCmd: apiSetNetworkInfo (NONE/offline)
14:50:32 sendCmd: apiSetNetworkInfo (latest current network)
14:50:32 sendCmd: apiGetChats
```

B then sent `368145001`; A displayed it at 14:51 without a cold start. This is
the decisive regression gate that code 367 did not reliably satisfy.

### Screen-off and vivo freezer gate

After the vivo per-app setting was changed to **允许后台高耗电**, A remained
running with its foreground service and reported `isFrozen=false` after about
89 seconds screen-off/background. It received both the exact `368145301` text
notification and a NomeQA-B361 encrypted audio-call notification while still
backgrounded.

### Physical offline queue and reconnect

B's Wi-Fi and mobile data were disabled. Runtime logs reported network
`NONE` and `hostDisconnected`; the receiver did not show the queued message.
After Wi-Fi/data were restored, B reported validated Wi-Fi and `hostConnected`
at 14:58:05, and A received `newChatItems` at 14:58:06. The typed ADB marker
lost one digit on input, so the delivered and verified value was `36814701`,
not the intended `368145701`.

## Publication boundary

- Implementation and evidence are local commits only at the time of this
  record.
- No remote push, tag, release, Play distribution, production change, or public
  publication was performed as part of this repair.
- No backup, retained worktree, iOS change, or app data was deleted.
