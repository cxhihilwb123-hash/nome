# Nome Android UI Fix Batch 05 — Summary

Date: 2026-07-23 (Asia/Shanghai)

## Outcome

IMPLEMENTED with one-device physical verification:

- Home and Contacts now render the real `ChatInfo.image` through the shared
  profile-image component. An initial-letter circle remains the fallback when
  a profile has no image.
- Android connection UI now says Nome invitation / Nome connection link in
  English and Simplified Chinese. The compatible protocol URI format is not
  changed.
- The user designated the currently configured Tencent Cloud SMP + XFTP pair
  as the present Nome official server.
- When Android has both an enabled custom SMP server and an enabled custom
  XFTP server, the server screen:
  - presents one `Nome official server` / `Nome 官方服务器` entry;
  - hides the SimpleX Chat and Flux preset-operator rows;
  - keeps message-server and media/file-server detail routes reachable.
- The native presets are not deleted or rewritten. They remain recoverable
  metadata and are shown again if the complete Nome SMP + XFTP set is absent.

## Verification verdict

- Debug app and Android-test APKs: PASS.
- API 35 targeted Compose instrumentation: `OK (21 tests)`.
- JVM tests: 54 tests, 0 failures, 0 errors, 0 skipped.
- Release lint: 68 warnings, 0 Error/Fatal.
- `git diff --check`: PASS.
- Vivo V2048A final APK SHA256 matches the local APK.
- Vivo V2048A real Home profile-image nodes: 3.
- Vivo V2048A real Contacts profile-image nodes: 4.
- Vivo V2048A connection page: Nome label and placeholder present; visible
  SimpleX wording absent.
- Vivo V2048A server root: Nome official entry 1; SimpleX 0; Flux 0; legacy
  `你的服务器` 0.
- Vivo V2048A real SMP and XFTP detail clicks: PASS.
- Vivo V2048A continuous Android Back from both details: PASS.

## Evidence limits

- Vivo V2047A was offline throughout Batch 05 final regression, so no Batch 05
  final APK installation or physical verification exists for that device.
- Screenshot protection remained enabled. No protected-account screenshot was
  captured or bypassed.
- Pixel-level avatar clarity is user-visible but not capturable under the
  current protection. UI-tree image nodes and controlled Compose rendering are
  the available evidence.
- The official server address and fingerprint are intentionally not embedded
  in this evidence or newly committed source. Existing configured devices use
  their saved server records.
- A clean installation without the saved SMP + XFTP pair does not yet
  auto-provision the current Nome server. Packaging a future default endpoint
  requires a separately authorized secure configuration path.
- Physical dark-mode and English-mode pixels were not forced. English and
  Chinese resources compiled, and Android resource mappings were asserted by
  instrumentation.

## Repository state

- Branch: `codex/nome-android-v656`
- HEAD: `64e8a161e6a42c841fbd4d7c6ba163508cb71d3d`
- No Batch 05 commit or push.
- Staged index empty.
- `.gradle-review` has zero tracked paths.
- Historical debug-manifest SHA256 remains
  `b6aa278d102675e777a4a0eac66cb9fd049209bb1998cdab247dfc7b56d792f3`.

