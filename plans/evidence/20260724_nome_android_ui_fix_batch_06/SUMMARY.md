# Nome Android UI Fix Batch 06 — Summary

Date: 2026-07-24 (Asia/Shanghai)

## Outcome

IMPLEMENTED with build and static verification:

- Direct-contact address cards shown on the Nome Home route are no longer
  unreachable read-only rows.
- Tapping a contact card uses the existing official connect-via-address flow.
- Long-press and the accessibility custom action expose the existing contact
  menu, including `Delete contact`.
- Pending contact-connection rows remain non-navigable but retain their
  supported management menu.
- Android notification channel names now use Nome in English, Simplified
  Chinese, and Traditional Chinese.

No protocol, database, message state-machine, server, account, or native-core
behavior was changed.

## Notification diagnosis

Read-only Vivo V2048A inspection showed:

- notification permission granted;
- message channel importance `4` (high);
- the Nome foreground message service running;
- the app present in the device-idle whitelist;
- lock-screen notifications and private notification content enabled;
- Do Not Disturb disabled and heads-up notifications enabled;
- active Nome message notifications present in Android notification state;
- Vivo `lock_screen_bright_on=0`.

The evidence supports a Vivo system setting as the reason the screen did not
light. The application successfully posted message notifications. Batch 06
does not misuse full-screen intents or permanent wake locks for ordinary chat
messages. Enabling Vivo's lock-screen bright-on-notification option remains a
manual user action.

## Verification

- `git diff --check`: PASS.
- Debug application APK: PASS.
- Android-test APK: PASS.
- JVM tests: 54/54, no failures, errors, or skips.
- Release lint: 68 warnings, 0 Error/Fatal.
- Batch 05 SHA256 manifest: PASS.
- New APK installation on V2048A: BLOCKED by the device-side USB installation
  confirmation (`INSTALL_FAILED_ABORTED: User rejected permissions`).

The already installed older test package was not counted as Batch 06 evidence.

## Evidence limits

- The new APK has not received a valid physical-device regression.
- Vivo V2047A was offline during the final pre-commit check.
- Locking, waking, unlocking, password entry, USB-install approval, and
  sensitive permissions remain user-operated.
- Screenshot protection was not changed or bypassed.
