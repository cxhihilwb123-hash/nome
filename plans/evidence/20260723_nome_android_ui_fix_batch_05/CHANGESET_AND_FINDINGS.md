# Changeset and findings

## Avatar root cause and fix

`NomeChatRow` always drew a hard-coded initial-letter circle even when the
existing chat model contained `ChatInfo.image`.

The row now uses the existing shared `ProfileImage` component when an image is
present and preserves the old initial-letter fallback when it is absent. The
image keeps the existing 44dp row geometry and does not create a new cache,
decoder, or network path.

## Link-brand root cause and fix

Two Android-owned P10/P12 strings still named SimpleX invitations and links.
Several legacy Android views also read common `MR.strings` resources directly.

The Android resources and Android localization mapping now use Nome for
user-facing link terminology. Parsing and the interoperable protocol URI
format remain unchanged.

## Official-server policy

The user explicitly designated the currently saved Tencent Cloud SMP + XFTP
configuration as the current Nome official server.

`hasCompleteNomeOfficialServerSet` returns true only when the custom
operator-null group contains:

1. at least one enabled, non-deleted SMP server; and
2. at least one enabled, non-deleted XFTP server.

On Android, and only while that complete set exists:

- preset operator rows are hidden;
- `Your servers` is branded as `Nome official server`;
- the existing message and media/file detail routes remain the owners of
  editing, testing, and future address changes.

This is a reversible presentation policy. It does not delete native presets,
rewrite server records, change protocol behavior, or copy endpoint material
into source code.

## Files changed by Batch 05

- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/ui/nome/home/NomeHomeRoute.android.kt`
- `apps/multiplatform/common/src/androidMain/kotlin/chat/simplex/common/platform/Resources.android.kt`
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/networkAndServers/NetworkAndServers.kt`
- `apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/networkAndServers/ProtocolServersView.kt`
- `apps/multiplatform/common/src/androidMain/res/values/nome_home_strings.xml`
- `apps/multiplatform/common/src/androidMain/res/values-zh-rCN/nome_home_strings.xml`
- `apps/multiplatform/common/src/androidMain/res/values/nome_settings_strings.xml`
- `apps/multiplatform/common/src/androidMain/res/values-zh-rCN/nome_settings_strings.xml`
- `apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/home/NomeHomeComposeTest.kt`
- `apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/newchat/NomeNewChatRouteComposeTest.kt`
- `apps/multiplatform/android/src/androidTest/java/chat/simplex/app/nome/settings/NomeSettingsBackupComposeTest.kt`

The existing modification to
`apps/multiplatform/android/src/debug/AndroidManifest.xml` predates Batch 05
and was not edited or reverted.

## Intentionally not changed

- Haskell/native core and server preset definitions;
- `Core.kt`;
- protocol URI formats;
- databases or account/server records;
- communication, message, call, file-transfer, or notification semantics;
- iOS runtime behavior;
- screenshot or recording protection.

