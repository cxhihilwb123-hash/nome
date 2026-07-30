# Nome Android 6.5.6 code 369 SMP heartbeat reliability acceptance

## Verdict

- Date/time and timezone: 2026-07-30 15:54 CST (+0800)
- Result: **PASS for the reproduced stale-channel regression, bidirectional
  direct messaging, encrypted bidirectional video media, preserved-data
  upgrade, build, signature, and automated-test scope exercised below on two
  physical Android devices.**
- The user-visible failure was reproduced before this repair: messages and
  video invitations produced no peer response even though both Android
  processes were foreground, Wi-Fi was validated, the vivo freezer was not
  active, and the SMP TCP sockets remained ESTABLISHED.
- Both chat cores had to be cold-started before the queued messages and call
  invitations arrived. Replaying or reconstructing the logical Android network
  alone did not recover an already-stale transport session.
- The corrective configuration (120-second SMP ping, one missed-ping threshold,
  TCP keepalive enabled) passed a no-restart configuration-only gate beyond the
  old failure window before it was made the code 369 default.
- The formal clean-source gate remains **FAIL** only because unrelated iOS work
  is intentionally preserved in this worktree. It was not staged, changed,
  reset, cleaned, stashed, or deleted during this Android repair.

This record supersedes the false-positive verdict in
`20260730_nome_android_6.5.6_code368_background_recovery.md`.

## Identity

- Repository: `/Users/forkman03/project/nome/nome-client`
- Branch: `codex/nome-v656-unified`
- Android/Core source commit:
  `2dd9989523a7b3ba2bd203f5e6dc28e58abae7db`
- Commit subject: `fix(android): restore reliable SMP heartbeat defaults`
- Source commit time: 2026-07-30 15:42:30 +0800
- Marketing version / version code: 6.5.6 / 369
- Application ID / label: `im.nome.app.dev` / `Nome`
- Platform / architecture: Android 13 physical devices / arm64-v8a
- Configuration: Debug, internal two-device diagnostic build
- Min / target / compile SDK: 28 / 35 / 35

## Reproduction and root cause

At 15:13, B sent `NOME_DBG_1513——b`; it appeared locally on B but not on A.
The code 368 logical network reconstruction completed successfully on each
device (`NONE` accepted, current network accepted), but the marker still did not
arrive. Cold-starting B alone did not flush it. Cold-starting both chat cores at
15:17 immediately delivered the queued marker and two old queued call
invitations. During the failure the SMP sockets remained ESTABLISHED to
`45.76.101.165:5223` (`smp.nome.im`).

The retained July 22 physical-device handoff documents the same repeatable
condition: with the upstream 1200-second ping default, the application channel
could go stale after approximately 5–6 minutes while TCP still appeared
connected. Its already-proven private SMP settings were:

- SMP PING interval: 120 seconds;
- missed PING count: 1; and
- TCP keepalive: enabled (idle 30 seconds, interval 15 seconds, count 4).

The new `im.nome.app.dev` package had no saved `NetworkSMPPingInterval` or
`NetworkSMPPingCount`, so code 368 fell back to `NetCfg.defaults` at 1200
seconds / 3. The package consolidation therefore lost an operational setting,
not message or call source code.

Code 369 changes both direct and proxy `NetCfg` defaults to 120 seconds / 1 and
explicitly retains `KeepAliveOpts.defaults`. Two regression tests pin these
values, and the Android version code advances from 368 to 369.

## Automated verification

Unit-test command:

```text
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew :common:desktopTest :android:testDebugUnitTest \
  --no-daemon --no-parallel --max-workers=1
```

- Result: `BUILD SUCCESSFUL in 2m 43s`
- Tasks: 52 actionable; 31 executed, 21 up-to-date
- Desktop tests: 226 passed across 49 suites; 0 skipped, failures, or errors
- Android unit tests: 70 passed across 13 suites; 0 skipped, failures, or errors
- New `NomeNetworkReliabilityDefaultsTest`: 2 passed

APK and AndroidTest assembly command:

```text
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
./gradlew :android:assembleDebugAndroidTest :android:assembleDebug \
  --no-daemon --no-parallel --max-workers=1
```

- Result: `BUILD SUCCESSFUL in 1m 27s`
- Tasks: 93 actionable; 27 executed, 66 up-to-date
- Post-source-commit `:android:assembleDebug`: `BUILD SUCCESSFUL in 10s`;
  65 actionable, 5 executed, 60 up-to-date
- `git diff --check`: PASS
- Non-fatal build warning: one installed Android SDK XML is version 4 while the
  native tooling reports support through version 3. Compilation, tests, native
  build, packaging, and signing completed successfully.

## Artifact and installation

- Gradle output:
  `/Users/forkman03/project/nome/nome-client/apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk`
- Bytes: 332,968,758
- SHA-256:
  `d76991fef433f087c8dd03c88da77cf6da0c2598fab5fb83c92c2a55c0474d4f`
- `aapt2` identity: `im.nome.app.dev`, label `Nome`, version 6.5.6 (369)
- APK Signature Scheme v2: PASS
- Signer certificate SHA-256:
  `b2fbf7616a337889d9aba6b4f3ad2680c8e83b8d4e25108f82068e873e7ae5b0`

Both devices returned `Success` from `adb install -r`. The Gradle APK and both
installed `base.apk` files independently returned the same SHA-256 above.

| Device | Test identity | Readback | Preserved state |
|---|---|---|---|
| vivo V2048A, `3106403166006XM` | A / NomeQA-A361 | 6.5.6 (369), updated 15:41:36 | first install 2026-07-29 21:44:42; profile and chats retained |
| vivo V2047A, `9590146717002S1` | B / NomeQA-B361 | 6.5.6 (369), updated 15:41:38 | first install 2026-07-29 21:44:42; profile and chats retained |

Both devices read back `NetworkSMPPingInterval=120000000` microseconds and
`NetworkSMPPingCount=1`; both remain on Android's idle whitelist and the vivo
per-app setting **允许后台高耗电**.

## Physical-device acceptance

### Configuration-only recovery proof on code 368

The settings were first applied through the real Advanced Network Settings UI,
which forced a Core reconnect on B at 15:25:44–15:25:46 and A at
15:27:34–15:27:35. Neither process nor chat core was then restarted before the
following gate:

| Gate | Result | Evidence |
|---|---|---|
| No-restart duration | PASS | A exceeded 8 minutes and B exceeded 10 minutes, beyond the repeatable 5–6 minute stale window. |
| Direct message A to B | PASS | `C369A2B1536` sent at 15:36:51; B received `newChatItems` at 15:36:52 and rendered the exact marker. |
| Direct message B to A | PASS | `3691538` sent at 15:39:00; A received `newChatItems` at 15:39:01 and rendered the exact marker. |
| Video invitation A to B | PASS | B received `callInvitation` at 15:39:26 and rendered the encrypted video incoming-call card. |
| Encrypted video connection | PASS | B accepted; both entered `Connected` at 15:39:49–15:39:50, and both WebRTC sides reported the peer camera track enabled. |
| Synchronized hangup | PASS | A ended at 15:40:07; B received `callEnded` at 15:40:07 and both returned to the chat UI. |

This isolates the fix to the heartbeat/keepalive configuration and disproves the
earlier assumption that logical network reconstruction was the durable repair.

### Installed code 369 long-window gate

Both chat cores started once at 15:41:56 after the preserved-data upgrade.
A reported `hostConnected` at 15:41:57.600 and B at 15:41:57.773. There was no
subsequent `hostDisconnected`, `hostConnected`, process restart, or Core restart
through the completed call at 15:53. A retained PID 10559 and B PID 2826 during
the entire gate.

| Gate | Result | Evidence |
|---|---|---|
| Same-session duration | PASS | The final call ended more than 11 minutes after both 15:41:57 connections, well beyond the old 5–6 minute failure window. |
| Direct message A to B after old failure window | PASS | `3691547` reached B at 15:47:27 and rendered exactly; B logged `newChatItems`. |
| Direct message B to A after 8 minutes | PASS | B sent `369155011` at 15:50:36; A logged `newChatItems` at 15:50:37 and rendered the exact marker. |
| Video invitation B to A | PASS | A received `callInvitation` at 15:51:06 and rendered the encrypted video incoming-call card. |
| Encrypted video connection | PASS | A accepted; both physical devices rendered `已连接` in `CallActivity`. |
| A camera delivered to B | PASS | B's WebRTC bridge reported peer `Camera, enabled=true` at 15:51:33.198. |
| B camera delivered to A | PASS | B's camera was deliberately toggled off and on; A received peer `Camera, enabled=false` at 15:52:40.549 and `enabled=true` at 15:52:57.552. |
| Synchronized hangup | PASS | B ended at 15:53:08.986; A received `callEnded` at 15:53:09.268 and both returned to the chat UI. |

After evidence capture, temporary diagnostic settings were restored without
changing the reliability settings:

- A: `DeveloperTools=true`, `LogLevel=WARNING`, `log.tag.SIMPLEX=I`;
- B: `DeveloperTools=false`, `LogLevel=WARNING`, `log.tag.SIMPLEX=I`;
- both: `NetworkSMPPingInterval=120000000`,
  `NetworkSMPPingCount=1`, version 6.5.6 (369), foreground Nome main activity.

## Publication boundary

- Implementation and evidence are local commits only at the time of this
  record.
- No remote push, tag, release, Play distribution, production change, or public
  publication was performed as part of this repair.
- No backup, retained worktree, iOS change, app data, or user file was deleted.
