# Nome Android UI Fix Batch 06 handoff

## Current state

Batch 05 and Batch 06 are organized as one scoped Android product-polish
checkpoint. The commit deliberately excludes the retained historical dirty
tree and older frozen artifacts.

- Branch: `codex/nome-android-v656`
- Pre-commit parent:
  `64e8a161e6a42c841fbd4d7c6ba163508cb71d3d`
- Official baseline:
  `59fce95d3cd08897b4ef742447b785cf2e56c7ce`
- Historical debug-manifest SHA256:
  `b6aa278d102675e777a4a0eac66cb9fd049209bb1998cdab247dfc7b56d792f3`

## Included

1. Batch 05 real avatars, Nome link wording, and saved official-server
   presentation.
2. Batch 05 tests, checkpoint, handoff, checksum manifest, and evidence.
3. Batch 06 Home contact-card click/long-press/accessibility behavior.
4. Batch 06 Nome notification channel branding.
5. Batch 06 build, diagnosis, boundary, checkpoint, handoff, and checksum
   evidence.

## Verification

- Debug APK and Android-test APK: PASS.
- JVM tests: 54/54.
- Release lint: 68 warnings, 0 Error/Fatal.
- `git diff --check`: PASS.
- Batch 05 checksum manifest: PASS.
- V2048A notification diagnosis: app posting is healthy; Vivo screen-bright
  setting is disabled.

## Remaining physical gates

The new APK was not installed because the Vivo-side USB installation
confirmation was rejected. Do not treat the older installed test package as
Batch 06 evidence.

After manual approval:

1. install the exact new APK;
2. tap each Home contact-address card and confirm the existing connect dialog;
3. long-press each card and confirm `Delete contact`;
4. cancel without deleting unless the user explicitly chooses deletion;
5. enable Vivo's lock-screen bright-on-notification setting manually;
6. perform a real peer-message lock-screen check;
7. repeat on V2047A when it reconnects.

No remote push is part of this handoff unless separately requested.
