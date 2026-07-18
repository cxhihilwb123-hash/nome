# P13 production TalkBack transcript and focus restoration

Date: 2026-07-18 (Asia/Shanghai)
Device: `nome-api35-arm64`, API 35, `emulator-5554`
Target: isolated debug application `chat.simplex.app.nome.dev`

## Proof boundary

- The enabled accessibility service was the real Google TalkBack service:
  `com.google.android.marvin.talkback/.TalkBackService`.
- Instrumentation requested `UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`, so the
  test automation service did not suppress TalkBack.
- A temporary androidTest-only `TextToSpeechService` synthesized valid silent PCM and logged only
  a fixed allowlist of public P13 failure-copy segments. It never logged an arbitrary utterance,
  URI, bearer, profile, host, server, or response.
- The controlled invitation stayed in the target app's private cache. Instrumentation read it
  locally and sent it to production `MainActivity` via explicit `ACTION_VIEW`; the value was not
  printed or retained in evidence.
- TalkBack accessibility focus was moved to the real Continue control and
  `ACTION_ACCESSIBILITY_FOCUS` plus `ACTION_CLICK` activated it. The production route then reached
  a real core/network Failure; no synthetic terminal state or timeout was injected.

## Exact allowlisted transcript

The TTS engine observed both failure announcement requests:

```text
TARGET_SEGMENTS=连接请求未发送|网络请求失败。重试时会先重新检查链接，再发送请求。|连接请求失败;REQUEST_LENGTH=42
TARGET_SEGMENTS=连接请求未发送|网络请求失败。重试时会先重新检查链接，再发送请求。|连接请求失败;REQUEST_LENGTH=48
```

Only the three fixed public segments above are preserved. The request lengths prove two synthesis
requests without retaining any other utterance content.

## Focus restoration defect and correction

The first real TalkBack dismissal run proved that accessibility focus did not return to the
originating Home current-profile control. Replaying a captured `AccessibilityNodeInfo` from the
app was rejected because synchronous same-process provider actions can block during modal
teardown.

The production correction is a payload-free Compose focus contract:

1. the P13 close action increments an in-memory request sequence before closing the modal;
2. the existing Home current-profile control owns a `FocusRequester`;
3. after modal teardown, Home requests focus with bounded retries.

The contract stores no label, profile, URI, bearer, host, or response and does not change Home
state ownership, `FIRST_USE`, or `FILTERED_NO_RESULT`.

## Results

- focus-only route:
  `NomeP13TalkBackHarnessTest#realTalkBackFocusRestorationFromReady` — `OK (1 test)`;
- combined real route:
  `NomeP13TalkBackHarnessTest#realTalkBackAnnouncementAndFocusRestoration` —
  `OK (1 test)`;
- combined terminal: real network `Failure`;
- activation: real accessibility action click;
- post-dismiss focus: Home current-profile control;
- final product source contains no temporary `NomeP13FocusTrace` logging.

The androidTest TTS service, manifest, harness, private cache invitation, and temporary trace were
removed before Gate G. TalkBack/accessibility/TTS settings were restored to the original
disabled/null state and the temporary permission was revoked. The rebuilt standard test APK has
zero temporary TTS/harness strings.
