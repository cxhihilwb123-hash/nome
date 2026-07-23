# Nome Android UI Fix Batch 04 handoff

## Current state

Batch 04 is implemented and verified locally and on both Vivo devices.

- Branch: `codex/nome-android-v656`
- HEAD: `6a01efa5e6d10dc0743cdf82dfb69e09cc459862`
- Official baseline ancestor: `59fce95d3cd08897b4ef742447b785cf2e56c7ce`
- No commit or push.
- Staged index empty.
- Existing dirty tree remains in place.
- Historical debug-manifest SHA256 remains
  `b6aa278d102675e777a4a0eac66cb9fd049209bb1998cdab247dfc7b56d792f3`.

## Implemented

1. Shared bottom navigation is visually lower and more compact:
   - exact 64dp item/touch height;
   - 36dp maximum system reserve;
   - live safe/gesture/platform insets;
   - standard Android `navigation_bar_height` fallback only when live values are zero.
2. Contacts-root Android Back returns to Home.
3. Android application-brand strings use Nome in the confirmed settings surfaces.
4. Technical/protocol/operator SimpleX proper nouns remain factual.

## Verification

- V2048A: complete 64dp item, 7px to app root.
- V2047A: complete 64dp item, 15px to app root.
- Real Home / Contacts / Settings taps: PASS on both.
- Contacts and Settings system Back: PASS on both.
- Privacy app/system Back: PASS on both.
- `Nome 锁定` present, `SimpleX 锁定` absent: PASS on both.
- Final APK SHA256 matches on both:
  `4b44d0d7c0c19a22f9b23c660ff711691070f2879a1a7432bf1745fe9aef5958`.
- API 35 targeted Compose tests: 17/17.
- JVM tests: 54/54.
- Release lint: 0 Error/Fatal.

## Evidence gaps

- Screenshot protection was preserved, so there is no new protected-account screenshot.
- Physical UI trees do not expose selected color/state; instrumentation covers the semantic state.
- Physical device-wide dark mode and English were not forced. Static resources and Android instrumentation cover the mappings, but pixel evidence is absent.
- P22 was not physically reopened in this follow-up; it shares the same component.

## Next action

Wait for the user to inspect both devices. Do not commit or push unless the user explicitly requests it.

