# Device and notification diagnosis

## Device availability

| Device | Final pre-commit state |
|---|---|
| Vivo V2048A | Online |
| Vivo V2047A | Offline |

## V2048A read-only facts

| Check | Result |
|---|---|
| Android notification permission | Granted |
| Nome notification mode | Foreground service mode |
| `SimplexService` | Running as foreground service |
| Background app ops | Allowed/default-allow |
| Device-idle whitelist | Nome present |
| Standby bucket | Active (`5`) |
| Message channel importance | High (`4`) |
| Lock-screen notifications | Enabled |
| Private lock-screen content | Enabled |
| Heads-up notifications | Enabled |
| Do Not Disturb | Off |
| Active Nome message notifications | Present |
| Vivo lock-screen bright-on-notification | Disabled (`0`) |

## Conclusion

Message receipt and Android notification posting were functioning at the time
of inspection. The black screen was consistent with the disabled Vivo
bright-on-notification setting, not with a missing Nome notification.

Android normally separates posting a notification from forcing the screen on.
Ordinary chat messages do not justify full-screen intents or an always-held
wake lock. The user must enable the Vivo lock-screen bright-on-notification
option manually if that presentation is desired.

## Installation gap

The new application and test APK installation attempt returned:

```text
INSTALL_FAILED_ABORTED: User rejected permissions
```

No device-side approval was automated. A previously installed test package
started but was interrupted and excluded because it did not contain the new
Batch 06 test code.
