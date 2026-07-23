# Nome Android UI Fix Batch 02 — Summary

Date: 2026-07-23

Scope: Android composer vertical alignment and pending-contact-connection menu reachability.

## Outcome

- The Android native `EditText` now has a 48 dp minimum height and vertically centered gravity.
- Overlay padding now matches the native field's 7 dp top/bottom padding.
- The Android send button keeps its bottom alignment for multiline composition but gains a 6 dp bottom inset, centering it in the 48 dp single-line composer.
- A read-only pending contact connection remains non-clickable, but now supports long press and a localized TalkBack custom action to open its existing menu.
- No delete API, contact model, protocol, database, message state, or communication behavior was changed.

## Verification

- Debug APK and Android test APK assembly: PASS.
- JVM unit tests: 54 passed, 0 failures/errors/skips.
- Release lint: PASS with 0 Error/Fatal; 68 existing warnings and none on changed paths.
- V2048A at 420 dpi override: input field and send target both measured 126 px = 48 dp, with identical vertical center.
- V2047A at 480 dpi: input field and send target both measured 144 px = 48 dp, with identical vertical center.
- Before the fix, V2047A measured approximately 12.17 dp between the two centers.
- V2048A Home and Contacts pending rows opened the existing Set contact name/Delete menu by real long press.
- V2047A Home pending row opened the same menu and reached the existing delete-confirmation dialog. Android Back canceled the dialog; the pending connection remained present.

## Custom-list finding

`Change list` is the existing SimpleX custom chat-list/tag feature. The assignment modal remains reachable from the long-press menu. The legacy SimpleX chat list renders user lists in a top `TagsView`/`TagsRow`, but the Nome P07 route does not render that row. Therefore the lists can currently be assigned and edited in the modal but cannot be selected as visible P07 filter chips.

This batch does not invent a new list presentation or hide the existing action. The next product decision is either:

1. add a Nome-styled custom-list filter row/management entry; or
2. hide `Change list` from the Nome menu until that presentation exists.

## Privacy and evidence boundary

Screenshot/recording protection remained enabled. UI hierarchy extraction printed only fixed labels, bounds, focus state, and character counts; message and contact content was not printed or stored. No message was sent. All batch-owned temporary UI hierarchy files were deleted from the Mac and both devices.

