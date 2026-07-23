# Nome Android UI Fix Batch 02 — Handoff

Date: 2026-07-23

## Verdict

The two confirmed UI defects are repaired:

1. Android single-line composer field and send target now share a 48 dp height and identical vertical center on both Vivo devices.
2. Pending contact connections remain read-only but can open their existing Set contact name/Delete menu by long press and TalkBack custom action.

Build, unit, lint, and targeted device gates pass. No message was sent and no pending connection was deleted.

## Source

- `PlatformTextField.android.kt`: matching overlay padding, 48 dp native minimum height, center-vertical gravity.
- `SendMsgView.kt`: Android-only 6 dp send-button bottom inset.
- `NomeHomeRoute.android.kt`: read-only pending-row long press plus accessibility custom action.
- `NomeHomeComposeTest.kt`: semantics contract updated.

## Device evidence

V2048A, 420 dpi override:

- package last update: 2026-07-23 14:34:51
- field: 126 px = 48 dp
- send target: 126 px = 48 dp
- center delta: 0 dp
- Home and Contacts pending-row menus: PASS

V2047A, 480 dpi:

- package last update: 2026-07-23 14:38:57
- field: 144 px = 48 dp
- send target: 144 px = 48 dp
- center delta: 0 dp
- Home pending-row menu and cancelable delete confirmation: PASS

Screenshot protection remained enabled, so caret pixels themselves are user-visible evidence rather than captured evidence.

## `Change list`

This is SimpleX's custom chat-list/tag feature:

- assignment/editing is shown inside `TagListView`;
- legacy SimpleX shows created lists as chips in the chat-list `TagsView`/`TagsRow`;
- Nome P07 does not render that top row.

Consequently, current Nome can assign a chat to a list but cannot select that list from P07. Do not claim this feature is fully presented. The next product decision is to add a Nome-styled list filter/management surface or temporarily hide the menu action.

## Repository boundary

- Repository: `/Users/forkman03/project/nome/simplex-chat`
- Branch: `codex/nome-android-v656`
- HEAD: `6a01efa5e6d10dc0743cdf82dfb69e09cc459862`
- Official baseline: `59fce95d3cd08897b4ef742447b785cf2e56c7ce`, still an ancestor
- Keep the index empty and `.gradle-review` untracked.
- Preserve the historical dirty debug manifest at SHA256 `b6aa278d102675e777a4a0eac66cb9fd049209bb1998cdab247dfc7b56d792f3`.
- No commit or push has been made.

Evidence: `plans/evidence/20260723_nome_android_ui_fix_batch_02/`.

