# P13 Logcat and `ACTION_VIEW` boundary

Date: 2026-07-18 (Asia/Shanghai)

Verdict: **PASS for app-owned bearer isolation**. Android retains an OS-owned intent-dispatch
descriptor containing the public `https://simplex.chat/` root, but the clean API 35 capture did
not retain the route or any bearer bytes.

## Controlled capture

The existing API 28 debug client's private-cache invitation was streamed directly into one API 35
external `ACTION_VIEW`. It was not printed, copied into the repository, written into evidence, or
included in a command result. The target debug app was force-stopped without clearing data,
Logcat was cleared, the explicit production `MainActivity` route was started, and the process was
allowed eight seconds to render.

The controlled invitation was 544 bytes. Exact full-value counts:

| Surface | Hits |
|---|---:|
| target app PID Logcat | 0 |
| app-owned `SIMPLEX` tag | 0 |
| API 35 UIAutomator tree | 0 |
| all API 35 Logcat buffers | 0 |

A second shape scan of the app PID, `SIMPLEX` tag, and UI tree found zero complete connection-URI
forms.

## OS-owned residual

Four Android-system intent records described the dispatch:

| System owner | Records |
|---|---:|
| `ActivityTaskManager` | 1 |
| `TopTaskTracker` | 1 |
| `WindowManager` | 1 |
| `WindowManagerShell` | 1 |

The records shared the controlled value only through character 21, the public
`https://simplex.chat/` root. Character 22, the first route character, had zero hits, as did every
longer tested prefix and the full value. This is a public-host intent descriptor, not a retained
connection bearer.

Nome must keep its browsable `ACTION_VIEW` route. It cannot own or suppress Android framework and
window-manager logging without weakening that product contract. The residual public-host record
is therefore documented as OS-owned platform behavior; app-owned logs, UI, screenshots, file
names, and retained evidence remain responsible for zero bearer retention.
