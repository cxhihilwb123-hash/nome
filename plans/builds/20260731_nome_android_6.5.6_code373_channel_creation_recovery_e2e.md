# Nome Android 6.5.6 code 373 channel creation recovery E2E

## Verdict

**PASS.** The new-channel creation stall reproduced with code 372 was fixed in
the unified client authority and verified with a newly built code 373 package
on both connected vivo devices. A newly created channel reached its link step,
the second device joined and connected to Nome Relay, three live broadcasts
arrived in order with the newest at the bottom, and all state survived a cold
restart on both devices.

This acceptance applies to the channel creation/reconciliation regression. It
does not replace the broader code 372 fresh-install functional matrix.

## Source and device identity

- Test window: 2026-07-31 00:04-00:07 CST (+0800)
- Repository: `/Users/forkman03/project/nome/nome-client`
- Branch: `codex/nome-v656-unified`
- Fix commit: `14e941f8a5860785e525377e15a27787ad282794`
- Version: `6.5.6` / code `373`
- Package / label: `im.nome.app.dev` / `Nome`
- Device A: vivo V2048A, `3106403166006XM`, channel owner
- Device B: vivo V2047A, `9590146717002S1`, subscriber

The package was installed with update semantics on both devices so existing
activated identities and prior QA conversations remained intact. No channel,
application data, backup, or retained worktree was deleted. Existing unrelated
iOS tracked and untracked changes were not staged or modified.

## Root cause and repair

The channel creation progress UI depended only on `GroupRelayUpdated` events.
`ChannelRelaysModel.updateRelay` discards an event unless its group ID is
already installed in the UI model. An asynchronously accepted relay can
therefore emit its active event before `apiNewPublicGroup` finishes installing
the initial `Invited` response. The progress view would then retain the stale
state indefinitely because it never queried Core again.

The repair in `AddChannelView.kt`:

- keeps event-driven updates as the fast path;
- queries `apiGetGroupRelays(groupId)` once per second while the creation
  progress view remains active;
- applies only non-empty, changed results for the same channel;
- stops naturally when navigation removes the progress view; and
- prevents an empty relay list from being interpreted as "all active".

`ChannelCreationRelayRecoveryBoundaryTest` protects the event-plus-authoritative-
reconciliation contract. Android version code was incremented from 372 to 373.

## Automated verification

Focused regression:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew --no-daemon --no-parallel --max-workers=1 \
  '-Dorg.gradle.jvmargs=-Xmx6144m -Dfile.encoding=UTF-8' \
  :android:testDebugUnitTest \
  --tests chat.simplex.app.nome.channel.ChannelCreationRelayRecoveryBoundaryTest
```

Result: `BUILD SUCCESSFUL in 12s`; 44 actionable tasks, 3 executed and 41
up-to-date.

Full build gate:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew --no-daemon --no-parallel --max-workers=1 \
  '-Dorg.gradle.jvmargs=-Xmx6144m -Dfile.encoding=UTF-8' \
  :android:testDebugUnitTest \
  :common:desktopTest \
  :android:assembleDebugAndroidTest \
  :android:assembleDebug
```

Result: `BUILD SUCCESSFUL in 2m 21s`; 107 actionable tasks, 32 executed and 75
up-to-date. Android unit tests, Common desktop tests, the Android test APK, and
the main debug APK all passed. Existing compiler deprecations remained
warnings. `git diff --check` and exact staged-diff checks passed.

## Artifacts

Main APK:

- Path: `apps/multiplatform/android/build/outputs/apk/debug/android-arm64-v8a-debug.apk`
- Bytes: `343322655`
- SHA-256: `a0345ee0b7df89216d0504fbebe9cd3e076b01cb205214e4a6b4c04d6afcb14c`
- Readback: `im.nome.app.dev`, `6.5.6`, versionCode `373`, arm64-v8a

Android-test APK:

- Path: `apps/multiplatform/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk`
- Bytes: `4135910`
- SHA-256: `d46811f1f7c5751b35d725bdc936d6da77a905cbd7b51431cddb57331d5e2fa1`

Both vivo package-manager readbacks showed code 373 after installation.

## Two-device acceptance evidence

| Gate | Status | Evidence |
|---|---|---|
| Preserve-data update install | PASS | Both code 372 installations updated to code 373; identities, activation, prior channels, and messages remained present. |
| New channel creation | PASS | A created `NOME-QA-CHANNEL-373-A` at 00:04:13. The first hierarchy showed `0 个中继活跃，共 1 个`; by the 00:04:32 hierarchy the UI had automatically advanced to `频道链接`. |
| Link generation | PASS | A displayed the QR/link sharing step. The invitation value was used for device testing and is intentionally excluded from this record. |
| Subscriber preview and join | PASS | B parsed the new invitation, displayed the correct channel and one subscriber, then joined at 00:05:37. |
| Subscriber relay connection | PASS | By 00:05:45 B displayed `Nome Relay 已连接` and entered subscriber read-only mode. |
| Live broadcast delivery | PASS | A sent `CHANNEL-373-LIVE-1-0006`, `-2-0006`, and `-3-0006` during 00:06:14-00:06:19. B's 00:06:31-00:06:33 hierarchy contained all three. |
| Timeline order | PASS | Both devices placed marker 1 above 2 above 3; marker 3 was the newest item at the bottom. |
| Subscriber permissions | PASS | B continued to display `仅查看模式`; no publish composer was exposed. |
| Cold-start persistence | PASS | Both apps were force-stopped and relaunched. Both Home rows showed the new channel and marker 3; reopening on each device showed markers 1, 2, and 3 in order. |
| Product crash or ANR | PASS | Exit-info contained only test-initiated `USER REQUESTED / FORCE STOP` and install events; no Nome crash or ANR was recorded. |

The one historical `AndroidRuntime FATAL EXCEPTION` visible on device A was
timestamped 2026-07-30 20:34 and its stack belonged to the shell
`uiautomator` accessibility dumper. It was not process `im.nome.app.dev` and is
excluded from product crash results.

## Commit and publication boundary

- Fix commit: `14e941f8a5860785e525377e15a27787ad282794`
- Evidence commit: recorded by the commit containing this file
- Remote push: not performed
- Deployment, store submission, production mutation, and public release: not performed
- Backups and retained worktrees: untouched
