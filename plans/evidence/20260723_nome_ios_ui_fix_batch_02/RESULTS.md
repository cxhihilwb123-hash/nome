# Nome iOS UI and server-default verification — 2026-07-23

## Scope

- Replaced the improvised home/contact/settings lockup with the approved Nome header artwork.
- Removed the preset Contacts, Groups, and Notes chips from the home filter row.
- Added a compact `群` badge beside group conversation names.
- Updated the Nome settings shell to use semantic theme colors in light and dark modes.
- Added a private build-setting path for Nome SMP/XFTP defaults. When configured,
  preset operators are disabled. The branded iOS server screen always hides the
  upstream preset-operator section and exposes the custom bucket as
  `Nome 官方服务器`.

## Simulator evidence

The PNG files are preserved locally and intentionally ignored by Git because the
physical-device captures can contain private profile or conversation metadata.

- `home_light.png`: approved logo, simplified filter row, and group badge.
- `contacts_light.png`: approved logo on the contacts page.
- `settings_light.png`: complete settings shell in light mode.
- `settings_dark.png`: complete settings shell in dark mode.
- `device_home_light_build339.png`: approved home layout running on the iPhone.
- `device_contacts_light_build339.png`: approved contacts layout on the iPhone.
- `device_settings_light_build339.png`: light settings shell on the iPhone.
- `device_settings_dark_build339.png`: dark settings shell on the iPhone.
- `device_nome_servers_build340.png`: Nome-only server entry with upstream preset
  operators absent.
- `device_nome_servers_build341.png`: final cleaned build with the same Nome-only
  server entry.

The signed simulator build completed successfully after the UI and server changes.

## Physical-device evidence

- Personal Team device build: Nome `6.5.6` (`341`).
- Architecture: arm64.
- Installation: successful cover install on the connected iPhone.
- Launch: successful through CoreDevice after installation.
- Bundle identity: unchanged from the prior Personal Team validation build, so
  existing app data and Keychain access remain in place.

## Server-security boundary

The repository contains no real SMP/XFTP address or queue-creation credential.
Production/local device builds inject those values through gitignored
`Local.xcconfig` or protected CI build settings. The cover-installed validation
build preserves the Tencent-backed server state already stored on the device.
