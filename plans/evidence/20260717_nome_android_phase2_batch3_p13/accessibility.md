# P13 accessibility

## Automated PASS

- current and incognito cards expose mutually exclusive selected semantics;
- interactive identity cards and actions meet the 48dp minimum in focused Compose checks;
- failure uses an assertive live region and is scrolled into view at 200% font;
- connecting uses a polite live region, is scrolled into view at 200%, and both primary/cancel
  actions are disabled while one request is in flight;
- pending and failure move focus to the real operation panel after it is attached;
- selection and warnings use icon/text/border in addition to color;
- 72 native captures cover zh-CN/en, light/dark, and 100%/200%.

The focused suite passed on both API 28 and API 35.

## Production TalkBack result

On API 35, TalkBack was enabled during the second real `ACTION_VIEW` invitation:

- focus plus double-tap selected `New incognito identity`;
- the UI tree changed current from checked to unchecked and incognito to checked;
- selection/focus generated four Google TTS synthesis events;
- focusing Continue generated another TTS synthesis event;
- TalkBack double-tap activated Continue once, and the route reached real `Connecting` then
  `Pending`.

This proves production-page traversal, selection, and activation.

## 2026-07-18 focus-restoration attempt

TalkBack, touch exploration, and Google TTS were present and enabled on API 35. In this cold-booted
session, adb-injected pointer events did not enter TalkBack's accessibility-focus/TTS path.
Computer Use was then tried so the emulator could receive a real desktop pointer action, but the
qemu emulator window is not exposed as a controllable application. No ordinary click was promoted
as TalkBack evidence.

The prior production traversal result remains valid.

## 2026-07-18 exact transcript and focus closure

The final API 35 session used the real TalkBack service together with an androidTest-only TTS
engine that synthesizes valid silent PCM and logs only a fixed allowlist of public P13 copy.
Instrumentation retained TalkBack, moved real accessibility focus to Continue, and activated it
with `ACTION_CLICK`. The production route reached a real core/network Failure.

The exact allowlisted announcement was:

```text
连接请求未发送
网络请求失败。重试时会先重新检查链接，再发送请求。
连接请求失败
```

Two synthesis requests carried all three fixed segments, with request lengths 42 and 48. No
arbitrary utterance, link, bearer, profile, host, server, or response was logged.

The first dismissal run exposed a real focus-restoration defect. P13 now emits a payload-free
in-memory focus request before modal close, and the existing Home current-profile control uses a
Compose `FocusRequester` after teardown. This does not change Home state ownership. Both the
focus-only route and the combined accessibility-click, live-announcement, dismissal route passed
as `OK (1 test)`, with focus restored to the originating Home current-profile control.

Full controls, transcript, defect boundary, and results are recorded in
`talkback-transcript.md`. The remaining work is cleanup/release isolation and the same-digest
Gate G review.
