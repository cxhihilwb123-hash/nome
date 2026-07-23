# Changeset and findings

## Batch 06 source changes

- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt`
  - treats direct contact-address cards as openable;
  - preserves the latest-row and deletion guards;
  - reuses `directChatAction` and `ContactMenuItems`.
- `apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeComposeTest.kt`
  - asserts click semantics, 48dp minimum target, accessibility menu action,
    and visible delete menu after long-press.
- `apps/multiplatform/common/src/commonMain/resources/MR/base/strings.xml`
- `apps/multiplatform/common/src/commonMain/resources/MR/zh-rCN/strings.xml`
- `apps/multiplatform/common/src/commonMain/resources/MR/zh-rTW/strings.xml`
  - rename Android message/call notification channels from SimpleX to Nome.

## Batch 05 changes included in the same reviewable checkpoint

- Home/Contacts real avatar rendering with initial fallback.
- Nome invitation/link wording in English and Chinese.
- Recognition and presentation of the saved complete custom SMP + XFTP set as
  the current Nome official server.
- Compose coverage for avatar, link wording, server labeling, and server-set
  completeness.
- Frozen Batch 05 handoff, checkpoint, SHA256 manifest, and evidence directory.

Batch 05 details remain in:
`plans/evidence/20260723_nome_android_ui_fix_batch_05/`.

## Root cause: undeletable official rows

Nome Home intentionally reintroduced direct contact-address cards that the
upstream chat-list projection excludes. The row interaction predicate then
explicitly rejected `contactCard`, while the dropdown menu already supported
`DeleteContactAction`. The menu existed but could not be reached by touch or
accessibility.

The minimal fix removes that contradiction. It does not invent a new delete
path or alter the delete API.

## Root cause: locked screen did not light

Android had accepted and retained high-importance Nome message notifications.
The app permission, channel, background service, lock-screen display settings,
battery exemption, and Do Not Disturb state were healthy. Vivo's separate
bright-on-notification setting was disabled.

No application-side force-wake workaround was added.
