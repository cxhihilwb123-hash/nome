# Nome Android 6.5.6 code 359 two-device functional test

## Verdict

- Date/time and timezone: 2026-07-29, Asia/Shanghai
- Result: **FAIL — not a full-function pass and not ready to be described as release-ready**
- Passed boundary: installation identity, preserved activation/data, contact connection,
  one-way text and media delivery, receiver-side opening/playback, group creation and
  invitation, main navigation, and About/version identity.
- Release-blocking boundary: bidirectional message delivery, group join completion,
  call handshake, message notification reliability, delete synchronization, and
  startup/reconnection reliability did not pass.

This verdict is based on two physical-device E2E checks of the archived Nome APK,
not only on unit tests or UI presence.

## Authority and immutable artifact

- Git repository: `/Users/forkman03/project/nome/nome-client`
- Branch: `codex/nome-v656-unified`
- Tested source HEAD before this record:
  `43fc084d8e1a89c17de645f2d5bbc3986251a84d`
- APK:
  `/Users/forkman03/project/nome/deliverables/Nome-Android-6.5.6-code359-20260729-arm64-debug.apk`
- Application ID: `chat.simplex.app.nome.dev`
- Version: 6.5.6 (359)
- APK SHA-256:
  `792a7ced820264e68a7e9eb31c11f59bfe200a3e94bfca10cfa35cc89951610b`

Final readback on 2026-07-29 confirmed that both connected devices still had
version 6.5.6 (359), and each installed `base.apk` had the exact archived APK
SHA-256 above.

| Device | Role | Final package readback | Final process state |
|---|---|---|---|
| vivo V2048A, `3106403166006XM` | A | 6.5.6 (359), exact APK hash | running, PID 19210 |
| vivo V2047A, `9590146717002S1` | B | 6.5.6 (359), exact APK hash | running, PID 25489 |

## Device-data safety

- Existing application containers and account activation were preserved.
- No uninstall, package-data clear, account reset, Git reset/clean/stash, or
  destructive reinstall was performed.
- Historical contacts and historical conversations were not deleted or renamed.
- New QA contact, group, messages, and files used visible `NOME-QA-20260729`
  markers where practical and remain available for follow-up diagnosis.
- A temporary diagnostic Debug APK was used only during connection bootstrap;
  the archived original APK was restored on both devices with `adb install -r`,
  preserving data. The final package/hash readback above proves the restored state.

## Functional matrix

| Area | Result | Physical-device evidence |
|---|---|---|
| Install / upgrade / launch | PASS | Both original APKs have matching version and hash; both processes live at final readback. |
| Existing activation and data | PASS | Both activated profiles and historical data remained after upgrade and diagnostic restoration. |
| New contact connection | PASS with recovery | A fresh invite using the enabled Nome relay connected after sender restart; an earlier pending route timed out. |
| Text A to B | FAIL reliability | Text arrived, but multiple outgoing items remained local until sender A was force-stopped and relaunched. |
| Text B to A | FAIL | Two distinct B-to-A markers remained only on B after B and A restarts; A never received them. |
| Edit synchronization | PASS with recovery | A edit to `A-FS-20260729-EDIT` reached B after sender restart. |
| Delete for everyone | FAIL | The delete-for-everyone action was confirmed, but the marker remained on both devices after restarts. |
| Reaction | FAIL synchronization | B rendered the local thumbs-up reaction; A never received it. |
| Image | PASS | A-to-B image produced receiver bytes and opened full-screen in Nome on B. |
| Video | PASS | A-to-B video downloaded on B and in-app playback advanced from 00:00 to 00:01. |
| PDF | PASS | A-to-B PDF downloaded on B and opened in the device PDF activity. |
| Voice message | PASS | A-to-B five-second voice message played on B; playback advanced from 00:05 to 00:03. |
| Group creation / invitation | PASS | A created the tagged QA group and B received the invitation card. |
| Group join / membership | FAIL | Join attempt did not complete; the group remained read-only after restart and a list row showed `01/01/70`. |
| Public channel creation | BLOCKED by configuration | UI reported zero enabled chat relays and correctly disabled creation; no channel was created. |
| Audio call | FAIL / incomplete | Call UI started, but no completed remote media session was proved. |
| Video call invitation | PASS | B received A's incoming video-call invitation and accepted it. |
| Video call session | FAIL | A remained at waiting-for-answer while B remained at waiting-for-confirmation for more than 20 seconds. |
| Background delivery | PASS with sender recovery | B was backgrounded; A's marker arrived after A restart. |
| Background message notification | FAIL | Only the persistent Nome receiving-service notification was observed; no marker-specific message notification appeared. |
| Force-stop recovery | PASS with limitation | B received the queued marker after relaunch; the sender still required restart to flush. |
| Startup reliability | FAIL | B intermittently showed an empty Compose hierarchy for about 12 seconds after relaunch before home loaded. |
| Nome short invite link | FAIL | The displayed short Nome invite URL was rejected/returned 404; a full internal connection link was required for the diagnostic bootstrap. |
| Main navigation | PASS | Home, contacts, chat, settings, search, servers, notifications, remote-desktop entry, and About opened without a fatal crash. |
| Remote desktop | PARTIAL | Initial QR-scan page opened; deeper pairing was not run because no desktop QR was in scope. |
| Nome identity | PASS | About displayed Nome Android client and version 6.5.6 (359). |

## Defects requiring follow-up

### P0: bidirectional delivery and reconnection state

Outgoing content repeatedly remained local until the sender process restarted,
and B-to-A text never completed despite restarting both sides. The same direction
also blocked reaction propagation, group-join completion, and the accepted call's
return handshake. This is consistent with a connection/queue/reconnection-state
defect, but the exact code-level root cause is not proven by this run.

### P1: group membership and call completion

- Group invitation reached B, but join did not produce an active member state.
- Video invitation and acceptance UI worked, but the peers never established a
  completed call session.

### P1: startup, notification, link, and deletion reliability

- Intermittent blank startup state lasted roughly 12 seconds.
- Background messages produced no user-visible message notification in this run.
- The branded short invite URL could not complete the connection path.
- Delete-for-everyone did not remove the message from either peer.

## Build and automated test evidence

- Command:
  `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home ./gradlew :android:testDebugUnitTest`
- Final command result: `BUILD SUCCESSFUL in 1s`; 44 actionable tasks, 1
  executed and 43 up-to-date.
- Test XML evidence timestamp: 2026-07-29 20:33:13 +0800.
- Result: 70 tests passed across 13 suites; 0 failures, 0 errors, 0 skipped.
- A first invocation without an available Java runtime failed before Gradle could
  run; setting Android Studio's bundled JBR as `JAVA_HOME` resolved the host
  environment issue. This was not counted as a product test failure.
- Authority worktree remained clean after Gradle execution before adding this
  evidence record; `git diff --check` passed.

Automated unit-test success does not override the physical-device E2E failures
listed above.

## Diagnostic-worktree boundary

Temporary connection-bootstrap instrumentation is retained, uncommitted,
unmerged, and unpushed in:

`/Users/forkman03/project/nome/nome-android-activation-bootstrap`

It includes diagnostic-only Android Debug hooks and two exploratory common-source
changes. None of those changes are part of the tested authority commit or this
documentation-only commit. They must not be merged as product fixes without a
separate review and verification cycle.

## Evidence and publication boundary

- Local evidence directory:
  `/Users/forkman03/project/nome/consolidation-evidence/20260729_android_two_device_full_functional_test`
- The directory includes the retained diagnostic instrumentation source. Secrets,
  invitation payloads, and credentials are intentionally excluded from this record.
- No Git push, tag, production deployment, store upload, or public release was
  performed.
