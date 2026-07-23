# Nome Android UI Fix Batch 05 handoff

## Current state

Batch 05 is implemented, fully tested in the API 35 emulator, and physically
verified on Vivo V2048A. Vivo V2047A was offline for final regression.

- Branch: `codex/nome-android-v656`
- HEAD: `64e8a161e6a42c841fbd4d7c6ba163508cb71d3d`
- Official baseline ancestor:
  `59fce95d3cd08897b4ef742447b785cf2e56c7ce`
- No Batch 05 commit or push.
- Staged index empty.
- Existing dirty tree remains in place.
- Historical debug-manifest SHA256 remains
  `b6aa278d102675e777a4a0eac66cb9fd049209bb1998cdab247dfc7b56d792f3`.

## Implemented

1. Home and Contacts render real profile images with initial fallback.
2. Android invitation/link wording uses Nome in English and Chinese while
   keeping protocol compatibility unchanged.
3. The saved Tencent Cloud SMP + XFTP configuration is recognized as the
   current Nome official server.
4. With both protocols enabled, Android hides SimpleX/Flux operator rows and
   presents one Nome official server entry.
5. Message and media/file server detail routes remain real, editable, and
   testable.

## Verification

- Build and Android-test APK: PASS.
- API 35 targeted instrumentation: 21/21.
- JVM tests: 54/54.
- Release lint: 68 warnings, 0 Error/Fatal.
- Vivo V2048A installed APK SHA256 matches local:
  `04b42f51e59dc693768a8e61a4a4f08e589cf6ba0c8a0a83d52666cc823c2f21`.
- Vivo V2048A Home profile images: 3 nodes.
- Vivo V2048A Contacts profile images: 4 nodes.
- Vivo V2048A server root: Nome official 1; SimpleX 0; Flux 0; legacy title 0.
- Real SMP detail, XFTP detail, and continuous Android Back: PASS.

## Evidence gaps and next action

1. Connect Vivo V2047A and install the exact final APK.
2. Repeat Home/Contacts avatar, Nome link, official-server, and Back checks.
3. A clean installation still needs a separately authorized secure default
   endpoint source; this batch does not embed the saved endpoint.
4. Wait for user review. Do not commit or push Batch 05 unless explicitly
   requested.

Evidence:
`plans/evidence/20260723_nome_android_ui_fix_batch_05/`

