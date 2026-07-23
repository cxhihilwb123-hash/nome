# Changeset and findings

## Source changes

- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/platform/PlatformTextField.android.kt`
  - makes overlay bottom padding match the native field;
  - gives the native `EditText` a 48 dp minimum height;
  - applies start plus center-vertical gravity.
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/chat/SendMsgView.kt`
  - changes only the Android branch of send-button bottom padding from 0 dp to 6 dp;
  - desktop/iOS runtime behavior remains on the existing non-Android branch.
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt`
  - preserves the non-clickable pending row;
  - adds long-press gesture handling and a TalkBack `More` custom action for `ChatInfo.ContactConnection`;
  - reuses the existing `ContactConnectionMenuItems`.
- `apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeComposeTest.kt`
  - keeps read-only rows without an `OnClick`;
  - asserts that contact cards have no menu action while pending contact connections expose the custom menu action.

## Custom-list location

- The assignment/edit UI is `TagListView`.
- The legacy main-list presentation is the private `TagsView` and `TagsRow` in `ChatListView.kt`.
- The current Nome P07 route does not call or reproduce `TagsView`.
- `Change list` therefore opens a valid assignment modal, but the resulting user list is not exposed as a selectable Nome Home filter.

## Deliberately unchanged

- Contacts filtering still includes direct contacts, contact requests, pending contact connections, and qualifying customer business chats.
- No custom-list filter row was added.
- No common translation was changed.
- No delete confirmation was accepted during verification.

