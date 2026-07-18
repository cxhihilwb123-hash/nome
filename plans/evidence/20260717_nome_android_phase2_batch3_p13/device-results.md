# P13 device results

Date: 2026-07-17, continued 2026-07-18 (Asia/Shanghai)

## API 28

- AVD: `nome-api28-arm64`
- API / ABI: 28 / arm64-v8a
- display: 1080×2220
- post-session standard artifact focused Compose + packaging instrumentation: `OK (4 tests)`
- production cold launch: `MainActivity` alive; native receiver loop started
- isolated sender profile: `v`
- first fresh invitation: peer observed the target's selected current profile,
  `Nomenclature35`
- second fresh invitation: peer observed the generated per-connection identity
  `SuperlativeSaver`, not either target local profile name
- fresh contact-address fixtures: `AddressSender28` and `RapidSender28`
- address creation experienced transient broker failures on otherwise TCP-reachable preset hosts;
  a later direct attempt through `smp10.simplex.im` succeeded

## API 35

- AVD: `nome-api35-arm64`
- API / ABI: 35 / arm64-v8a
- timezone: Asia/Shanghai
- display: 1080×2400
- post-session standard artifact focused Compose + packaging instrumentation: `OK (4 tests)`
- renderer matrix instrumentation: `OK (1 test)`, 72 deterministic PNGs
- production cold launch: `MainActivity` alive; native receiver loop started
- controlled local profiles: `Nomenclature35` and `IncogTarget35`
- a real external invitation reached P13 through `ACTION_VIEW`
- current-profile attempt reached `Ready -> Connecting -> Pending`; after completion the target
  observed sender `v`
- incognito attempt selected `New incognito identity`, reached
  `Ready -> Connecting -> Pending`, and after completion the target again observed sender `v`
- real contact address reached the contact-address-specific `Ready` page
- cancel-before-submit returned home with no request on either client
- eight rapid taps produced one sanitized real network `Failure` outcome and no sender request
- Retry showed `Replanning` and returned to a fresh `Ready` plan
- actual in-flight rotation plus Home/background/resume retained the attempt and ended in real
  `Pending`, with `PLAN=1 / CONNECT=1`
- switching the active user after `Ready` produced `ContextChanged`, with `PLAN=1 / CONNECT=0`
- one command-owning cancellation attempt recorded `PLAN=1 / CONNECT=1`, one active owner child,
  owner inactive after cancel, and `kotlinx.coroutines.JobCancellationException`; no terminal UI
  state was applied afterward
- after the controlled address owner accepted the request, reopening the identical address
  produced `PLAN=1 / CONNECT=0` and the official existing-contact dialog, proving the P13
  `AlreadyExists` branch is planner-preempted
- real TalkBack plus a payload-free androidTest TTS transcript closed the exact Failure
  announcement; the focus-only and combined announcement/dismissal routes each passed
  `OK (1 test)`, and focus returned to the Home current-profile control

The four focused device tests cover identity exclusivity and 48dp targets, failure semantics and
visible actions at 200%, connecting live-region visibility and disabled duplicate actions at 200%,
and debug-host non-exported packaging.

The authoritative instrumentation target is the isolated debug package
`chat.simplex.app.nome.dev.test/androidx.test.runner.AndroidJUnitRunner`; the production package
remains `chat.simplex.app`.

The controlled two-client session used the real core and production `MainActivity` route in the
isolated debug package. Several preset servers timed out while the default SMP proxy route was in
use, although TCP reachability was present. The session therefore selected one known SMP server
and direct SMP mode for the isolated test profiles. This is a test transport configuration and
does not prove the health of the default proxy/server pool; it does not alter the P13 route or
identity result.

The invitation itself is not retained in this evidence. The three known private-cache bearer
files, lifecycle receiver, one-shot/TalkBack/TTS androidTest sources, and temporary product trace
were removed. API 35 accessibility/TTS settings and permission were restored. The rebuilt standard
test APK passed `OK (4 tests)` on API 28 and API 35, and the final 72-image API 35 matrix passed
`OK (1 test)`.
