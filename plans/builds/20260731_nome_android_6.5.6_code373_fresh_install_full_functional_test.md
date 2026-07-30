# Nome Android 6.5.6 code 373 fresh-install full-functional test

## Verdict

**FAIL / partial pass.** The authorized two-device uninstall, fresh install,
first-use activation, and full functional matrix completed. Direct contacts,
bidirectional text, background notification delivery, image/PDF transfer,
voice/video media delivery and playback, private groups, audio/video calls,
cold-start persistence, primary navigation, Settings/About, and saved Nome
server defaults passed on both physical devices.

A newly created public channel could not pass the official pre-join relay test.
Three independent attempts returned one `WaitResponse` relay and one failure,
so subscriber join and owner-to-subscriber broadcast delivery could not be
accepted. Public-channel relay operation is therefore a release blocker and
this run is not an all-pass Android release acceptance.

## Source and authorization boundary

- Test window: 2026-07-31 00:12-01:31 CST (+0800)
- Git repository: `/Users/forkman03/project/nome/nome-client`
- Branch: `codex/nome-v656-unified`
- Tested product source commit: `73fbecde8c06c21f3b1dcec8fa4db2764860d466`
- Marketing version / version code: 6.5.6 / 373
- Application ID / label: `im.nome.app.dev` / `Nome`
- Architecture / configuration: arm64-v8a / Debug
- Devices: vivo V2048A `3106403166006XM` and vivo V2047A
  `9590146717002S1`, Android 13 / API 33

The user explicitly authorized removal of the existing app and a true
first-install test on both named devices. The two required one-device internal
QA activation invitations were also explicitly authorized and redeemed once;
their values are intentionally excluded from this record.

Unrelated iOS tracked and untracked worktree changes were preserved. They were
not reset, stashed, staged, removed, or included in the Android commit. No
remote push, deployment, production server mutation, store action, or public
release was performed.

## Artifacts and fresh-install identity

Main APK:

- Path: `apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk`
- Bytes: `343322655`
- SHA-256: `a0345ee0b7df89216d0504fbebe9cd3e076b01cb205214e4a6b4c04d6afcb14c`
- Both devices independently reported the identical SHA-256 for the installed
  `base.apk`.

Final Android-test APK:

- Path: `apps/multiplatform/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk`
- Bytes: `4158170`
- SHA-256: `2fbbbf0e570d1a122a84e957a1b7cb2f9b408c542948a582a79f22434ee81996`

| Device | Fresh-install timestamp | First local identity |
|---|---|---|
| vivo V2048A / A | first and last install `2026-07-31 00:12:15` | `NomeFresh-A373` |
| vivo V2047A / B | first and last install `2026-07-31 00:12:16` | intended `NomeFresh-B373`; vivo IME rendered `N哦么发热是－吧73` |

The B identity spelling is an OEM input-method transformation observed while
entering the QA name. It did not change package identity, activation, routing,
or protocol behavior.

## First-use and saved defaults

Both installations started from empty app data and completed welcome,
identity creation, privacy onboarding, background-service configuration,
notification permission, and production activation. Microphone and camera
permissions were granted only when the real audio and video call flows first
requested them.

`NomeStartupServerConfigurationTest` passed independently on both final
installations. It read the active user's live Core configuration and proved:

- active `smp.nome.im` messaging storage/proxy route;
- active `xftp.nome.im` file storage/proxy route;
- enabled saved `relay.nome.im` channel relay with a database id;
- other managed preset operators were not active beside the Nome routes.

The saved relay's presence is a configuration pass, not a connectivity pass.
Its live official relay test is the separate failing gate below.

## Two-device functional matrix

| Gate | Status | Evidence |
|---|---|---|
| Authorized uninstall and fresh install | PASS | Existing packages were removed; both code 373 installs have identical first/last install timestamps and identical installed APK hashes. |
| First-run onboarding and production activation | PASS | Two separate single-use QA redemptions completed; both devices reached the running home state with new local identities. |
| Nome package identity and defaults | PASS | `im.nome.app.dev`, label Nome, version 6.5.6 (373), live Core SMP/XFTP routes, and saved Nome relay verified. |
| New direct contact | PASS | Real Core contact creation completed through the bounded in-memory two-device bridge: A 9.423s, B 11.535s. |
| Direct text A to B | PASS | `NomePeerMessage-AtoB-code373` delivered: A 4.857s, B 6.656s. |
| Direct text B to A | PASS | Reverse delivery completed: A 3.945s, B 5.471s. |
| Background notification | PASS | With B backgrounded, notification payload contained `NomePeerMessage-Notify-AtoB-code373`, sender `NomeFresh-A373`, on the Nome message channel at importance 4; A 4.759s, B 14.878s. |
| Image / generic attachment A to B | PASS | Receiver bytes matched, external viewer opened, and fixture cleanup completed: A 13.281s, B 15.016s. |
| PDF A to B | PASS | Valid 597-byte PDF matched at the receiver and opened in the system viewer: A 13.308s, B 15.082s. |
| Voice media A to B | PASS | 5066-byte M4A SHA-256 `9c6af354ac89ad231e84ce351e113bba4a6ac00804abbd5d154f64f3771ba34e` matched and playback progressed: A 9.387s, B 11.142s. |
| Video media A to B | PASS | 16935-byte MP4 SHA-256 `e2751566862861eaeb1f148150b4de9ba2f69b424db52e9263d46f538ae2da7f` matched and playback progressed: A 8.935s, B 10.988s. |
| New private group create/invite/join | PASS | `NomeGroup-code373-fresh` completed and B-to-A messaging passed: A 9.309s, B 10.992s. |
| Existing group bidirectional messaging | PASS | `NomeGroupMessage-BtoA-reuse-code373` and `NomeGroupMessage-AtoB-reuse-code373` both delivered: A 6.076s, B 7.793s. |
| Public channel creation | PASS | A created a fresh Core-backed channel using group id 2 and the configured Nome relay. No channel secret is stored in this record. |
| Official channel relay test | **FAIL** | On three attempts B returned `stateCounts=WaitResponse=1 failures=1`; the official join guard rejected the relay before channel join. |
| Channel subscriber join and broadcast | **BLOCKED** | Correctly not attempted past the failing official pre-join relay test; no delivery pass can be claimed. |
| Audio call | PASS | Both devices reached Connected and ended cleanly after first-use microphone permission: A 154.914s, B 157.257s. |
| Video call | PASS | Both devices reached Connected with real camera capture, encrypted send/receive tracks, VPX decode, and clean end: A 24.697s, B 27.268s. |
| Cold-start persistence | PASS | Exact force-stop/start returned `LaunchState: COLD` (A 1682ms, B 482ms); the persisted contact then delivered `NomePeerMessage-ColdStart-code373`: A 4.976s, B 6.779s. |
| Physical navigation and About | PASS | Final test APK passed on A in 11.916s and B in 20.117s: Settings, settings search, About Nome, live 6.5.6/373 version, Contacts, and Home/new-connection action. |
| Product crash / ANR | PASS | Both `lastanr` reports say no ANR since boot. Exit-info contains user-requested instrumentation force stops and expected WebView isolated-process teardown only; no `im.nome.app.dev` crash reason. |

## Channel blocker evidence

The source device remained waiting for the peer during each target attempt and
was deliberately stopped after the target's official relay assertion failed.
Those resulting instrumentation `Process crashed` summaries are controlled
harness stops, not application crashes.

The target assertion was consistent on all three attempts:

```text
Every relay in the official group-link plan must pass its official test before joining;
stateCounts=WaitResponse=1 failures=1 expected:<0> but was:<1>
```

Current DNS at final evidence time resolved `turn.nome.im`, `relay.nome.im`,
`smp.nome.im`, and `xftp.nome.im` to `45.76.101.165`. Both phones could reach
that address. Homebrew OpenSSL 3 completed TLS 1.3 to the host, but
`relay.nome.im:443` presented the `xftp.nome.im` certificate. The application
failure is stronger evidence than DNS/TCP reachability: the official signed
relay request did not advance past `WaitResponse`.

The same default Nome relay passed both devices and delivered a real broadcast
under code 371 at 2026-07-30 17:39, as recorded in
`plans/builds/20260730_nome_android_6.5.6_code371_channel_relay_e2e.md`.
Together with the passing code 373 direct-message, group, file, media, and call
paths, the evidence points to current relay service/entrypoint drift rather
than a general client merge regression. That is a bounded inference, not a
server-side root-cause proof. Production relay inspection or repair requires a
separate explicit authorization.

During the successful LAN video call, WebRTC logged transient
`turn.nome.im` resolution errors, but the final DNS probe resolved the hostname
and the peer connection still passed with real video media. This remains an
infrastructure reliability observation, not a failed call gate.

## Crash exclusions

The A crash buffer contained two shell `uiautomator` failures: a vivo
`AccessibilityNodeInfoDumper` null dereference and a duplicate
`UiAutomationService` registration. Their PIDs were shell tooling processes,
not `im.nome.app.dev`. B's crash buffer was empty. Both are excluded from the
product crash result.

The video-call teardown ended the Chromium isolated renderer with Android exit
reason `OTHER KILLS BY SYSTEM / ISOLATED NOT NEEDED`, while the main Nome
process was ended by the instrumentation runner with `USER REQUESTED / FORCE
STOP`. Neither record is an application crash.

## Automated build and cleanup

Final Android-test assembly:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :android:assembleDebugAndroidTest \
  --no-daemon --no-parallel --max-workers=1
```

Result: `BUILD SUCCESSFUL in 15s`; 76 actionable tasks, 9 executed and 67
up-to-date. The unresolved Accompanist opt-in and SDK XML/CMake compatibility
messages remained warnings.

All exact temporary ADB reverse mappings `tcp:27301` through `tcp:27319` were
removed from both devices. Final `adb reverse --list` was empty on both, and no
local listener remained on those ports. No retained backup or worktree was
deleted.

## Commit and publication boundary

- Android harness and evidence commit: the commit containing this file
- Exact-path staging only; unrelated iOS paths excluded
- Remote push: not performed
- Production relay change: not performed
- Deployment, store submission, and public release: not performed
- Backup and recovery-only worktrees: untouched
