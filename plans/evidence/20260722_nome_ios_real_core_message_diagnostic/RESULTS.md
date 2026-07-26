# Nome iOS Real-Core Message Diagnostic

Date: 2026-07-22 (Asia/Shanghai)

## Verdict

**PASS for the focused v6.5.5 Core compatibility diagnostic, with a connection-recovery caveat.**

Two isolated arm64 iOS simulators exchanged three new messages in each
direction while the receiving XCUITest was already waiting before the sender
started. All six local sends, all six live receives, the real-core preflight,
the Xcode build, and all four XCUITest result bundles passed.

This does not establish production readiness. The simulator libraries are the
official v6.5.5 compatibility artifacts running under the v6.5.6.1 Swift UI,
not exact v6.5.6.1 Core artifacts. A pre-batch receiver process also reproduced
a stale connection that recovered after restarting only that app process.

## Reproducible diagnostic

The committed helper requires two existing, paired simulator profiles and does
not create contacts or retain invitation links:

```bash
scripts/ios/run-real-core-message-diagnostic.sh \
  --lab-simulator 539249D8-761C-4DB0-966C-75E083869D91 \
  --peer-simulator 8FB4B1DF-2CA4-4185-B965-45DAF4E16534 \
  --rounds 3 \
  --run-id batch20260722a
```

The script:

- selects full Xcode per command without changing machine-wide `xcode-select`;
- verifies the installed arm64 simulator real-core libraries;
- builds the app and XCUITest bundle once;
- starts the receiver test first for each direction;
- sends through the native composer and waits for the send API to clear it;
- verifies exact message values in the receiver's real conversation;
- retains logs, screenshots, four `.xcresult` bundles, and timing markers in a
  new `/tmp` evidence directory;
- restores any simulator that was shut down when the script started.

Helper validation passed:

```text
[PASS] real-core message diagnostic helper validation tests passed
```

A separate build and test run without either diagnostic build setting also
passed safely: both diagnostic tests were skipped (`2 skipped`, `0 failures`).
Normal scheme use therefore does not send synthetic messages unless an
explicit diagnostic run id is supplied.

## Three-round batch evidence

Machine-local packet:

```text
/tmp/nome-ios-real-core-message-diagnostic-batch20260722a
```

Summary status: `PASS`.

| Direction | Round | Local send completion | Live receive wait |
|---|---:|---:|---:|
| Lab to Peer | 1 | 0.444 s | 13.146 s |
| Lab to Peer | 2 | 0.436 s | 5.195 s |
| Lab to Peer | 3 | 0.465 s | 4.439 s |
| Peer to Lab | 1 | 0.421 s | 12.172 s |
| Peer to Lab | 2 | 0.424 s | 4.186 s |
| Peer to Lab | 3 | 0.433 s | 4.236 s |

The first receive time in each direction includes starting the independent
XCTest runner, launching the app, initializing Core, opening the conversation,
and the sender test startup. Later rounds measure the already-running path.

Four independent result bundles passed:

- `lab-to-peer-send.xcresult`
- `lab-to-peer-receive.xcresult`
- `peer-to-lab-send.xcresult`
- `peer-to-lab-receive.xcresult`

The final Lab and Peer screenshots show the third diagnostic message for the
corresponding direction. Screenshots and raw logs remain outside Git because
they contain user-local simulator conversation data.

## Reproduced stale-connection boundary

Before the controlled batch, Peer had remained foregrounded since 10:07 but did
not receive `nome-diag-probe20260722f-main-r1`, even though Lab cleared its
composer in 0.429 seconds and persisted the outgoing message. Restarting only
the Peer app at 10:24:51 preserved its profile, contact, and databases. The
queued message appeared with an unread badge by 10:24:55.

After that recovery, `nome-diag-probe20260722h-main-r1` reached Peer without a
second restart, and `nome-diag-probe20260722h-peer-r1` reached Lab live in the
reverse direction. Both exact conversation assertions passed in 1.187-1.192
seconds after their receiver views were ready.

This narrows the caveat to a receiver process connection/subscription recovery
state. It is not evidence of lost message persistence, an unusable composer, or
a permanently broken relay path. The encrypted databases were not opened or
modified by host tools.

## Log boundary

Both simulators logged notification-token broker timeouts during otherwise
successful live chat delivery. Therefore those `registerToken` failures belong
to the notification path and are not sufficient evidence of a chat-message
delivery failure.

No Swift UI exception, crash, local send timeout, or live receive timeout
occurred in the controlled three-round batch. The remaining stale-connection
root cause is below the tested Swift UI boundary or specific to the v6.5.5
compatibility Core/network state. This batch intentionally does not patch the
Haskell/native Core, protocol, database, or message state machine.

## Remaining gates

- Obtain and verify exact v6.5.6.1 simulator and device Core artifacts.
- Repeat the connection-recovery diagnostic with the exact Core.
- Run a trusted physical-device smoke and background/foreground notification
  matrix.
- Do not classify notification-token timeouts as chat failures without separate
  delivery evidence.
- No Android source, build, process, emulator, or user test artifact was
  targeted by this diagnostic.
