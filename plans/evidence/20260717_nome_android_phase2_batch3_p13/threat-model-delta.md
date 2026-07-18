# P13 threat-model delta

| Threat | Control implemented | Current proof |
|---|---|---|
| Wrong user/remote host receives connect | capture expected user/host and compare immediately before connect; mismatch returns typed `ContextChanged` | unit context matrix; real user-switch `PLAN=1 / CONNECT=0` trace |
| Bearer link leaks to UI/log/evidence | raw link remains in controller closure; typed delegates call `sendCmd(..., log=false)`; safe model has no URI/signature | clean app PID/tag/UI/full-log exact scan zero; four OS-owned dispatch descriptors expose public host only |
| Owner verification is overstated | only core `Verified` / `Failed(reason)` maps to owner-proof copy | exhaustive mapping test + bilingual fixture |
| Duplicate connect | reducer ignores duplicate submit, in-flight actions disabled, controller uses atomic single-submit | reducer + API 28/API 35 Compose + eight activations with exact `PLAN=1 / CONNECT=1` |
| Retry uses stale plan or silently loses failure | retry keeps P13 in `Replanning`, reruns `planAndConnect`, retains typed planning failure/no-user, and closes only for a successful or authoritative fallback handoff | source + reducer + production Failure -> Replanning -> fresh Ready; retry exact `PLAN=1 / CONNECT=1` |
| Incognito becomes persistent identity | model and bilingual copy say this connection only; command receives only the per-attempt Boolean | source/copy + peer observed a generated name distinct from both local profiles |
| Renderer success is confused with core success | debug-only labelled fixture host; release exclusion; evidence marks renderer rows | packaging/aapt + evidence |
| P13 captures other flows | legacy default and single external call-site opt-in; 7/21 eligible branch map | source scan + tests |

The static artifact scan found no retained connection bearer in the P13 UI, debug host,
instrumentation sources, or this evidence root. In the clean real session the app PID,
app-owned Logcat tag, UI tree, and full Logcat exact-value scan were zero. Android's
ActivityTaskManager/window stack recorded four intent descriptors containing the public
`https://simplex.chat/` root, but not the route or any later bearer bytes. The OS boundary is
documented in `logcat-boundary.md`; no product route was weakened to suppress it.

The contact-address continuation separately recorded zero-hit Ready/Failure UI-tree scans.
The three known private-cache bearers, device dumps, lifecycle receiver, one-shot and TalkBack/TTS
harnesses, and temporary product trace were removed before the Gate G input. Release/debug/test APK
scans found zero temporary receiver/TTS/harness/trace strings. The remaining focus contract carries
only a monotonic in-memory request sequence and no payload.
