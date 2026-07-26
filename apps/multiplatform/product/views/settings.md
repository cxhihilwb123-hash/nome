# Settings

> **Related spec:** [spec/client/navigation.md](../../spec/client/navigation.md) | [spec/client/nome-android-ui.md](../../spec/client/nome-android-ui.md) | [spec/services/theme.md](../../spec/services/theme.md) | [spec/services/notifications.md](../../spec/services/notifications.md)

## Purpose

Configure all aspects of app behavior including notifications, network/servers, privacy, appearance, database management, call settings, and developer tools. Accessed from the UserPicker or directly from the chat list toolbar.

## Route / Navigation

- **Entry point**: Tap user avatar in `ChatListView` toolbar -> `UserPicker` -> Settings option; or directly via `NavigationButtonMenu` when no users exist
- **Presented by**: `SettingsView` composable via `ModalManager.start.showModalCloseable`
- **Navigation title**: "Your settings" (`AppBarTitle`)
- **Sub-navigation**: Each settings row opens a dedicated view via `showSettingsModal` or `showCustomModal`

## Platform Differences

| Aspect | Android | Desktop |
|---|---|---|
| App section | Device settings, app version | App updates (`AppUpdater`), device settings, app version |
| Notifications | Full notification mode selection (instant/periodic/off) | Notification settings |
| Use from desktop/mobile | "Use from desktop" option in UserPicker | "Link a mobile" / "Linked mobiles" option in UserPicker |
| Database migration | "Migrate to another device" with auth | Same |
| Help and support | Full official mobile help/support set | Nome usage guide and Nome about page only; upstream changelog, founder chat, upstream email and upstream store/GitHub promotions are hidden |
| App updates | Store/manual distribution | Hidden until Nome has its own signed and notarized update feed; the upstream update feed is never offered |

Android's "Connect desktop" route uses the Nome full-page/grouped settings-detail presentation
while retaining the official device-name, QR/address connect, discovery, verification,
connected-session, disconnect, switch-local, and linked-device owners. Its title follows the real
session state. The unpaired scanner state is not a paired or connected result. Removing a linked
desktop requires destructive confirmation. macOS keeps the established modal owners inside the
Nome navigation shell and uses Nome labels for tray, about, privacy lock, addresses, and service
configuration.

## Page Sections

### Settings Section

| Row | Icon | Destination | Description |
|---|---|---|---|
| Notifications | `ic_bolt` / `ic_bolt_off` | `NotificationsSettingsView` | Push notification mode and preview settings |
| Network & servers | `ic_wifi_tethering` | `NetworkAndServersView` | SMP/XFTP servers, proxy, .onion hosts, advanced network |
| Audio & video calls | `ic_videocam` | `CallSettingsView` | WebRTC relay policy, ICE servers |
| Privacy & security | `ic_lock` | `PrivacySettingsView` | Nome Lock, delivery receipts, link previews, auto-accept |
| Appearance | `ic_light_mode` | `AppearanceView` | Theme, language, profile images, chat bubbles |

All rows disabled when `chatModel.chatRunning != true` (except Appearance).

#### Notifications (`NotificationsSettingsView`)

| Setting | Options |
|---|---|
| Notification mode | Instant (background service) / Periodic (every 10 min) / Off |
| Notification preview | Configuration for notification content visibility |

#### Network & Servers (`NetworkAndServersView`)

| Setting | Description |
|---|---|
| SMP servers | Messaging relay servers; per-operator configuration |
| XFTP servers | File transfer servers; per-operator configuration |
| Server operators | `OperatorView` for each configured operator |
| Advanced network | `AdvancedNetworkSettings` -- timeouts, TCP keep-alive, reconnect intervals |
| Proxy configuration | SOCKS proxy, .onion host settings |

Sub-files: `NetworkAndServers.kt`, `ProtocolServersView.kt`, `ProtocolServerView.kt`, `NewServerView.kt`, `ScanProtocolServer.kt`, `AdvancedNetworkSettings.kt`, `OperatorView.kt`

On Nome macOS, an isolated fresh profile contains one enabled Nome operator backed by the official
SMP and XFTP trust anchors. Opening an upgraded profile first removes only legacy upstream rows
that were stored as presets; user-added (`preset = false`) servers are retained. The two upstream
support/status cards inserted by older clients are removed only when they are still disconnected
and contain no messages, group membership, or request. Real conversations and similarly named
user contacts are preserved. Nome is not subject to the embedded upstream operator conditions, so
no upstream conditions notice, review row, or re-enable gate is shown for the Nome operator.
Legacy operator tags remain decodable for protocol/database compatibility but have no branded
upstream description, website, or logo metadata in the Nome client.

The desktop settings page and chat-list launch path do not expose or auto-open the historical
upstream “What’s new”, support, email, update-feed, or operator-conditions modal. Nome macOS will
add those surfaces only when Nome-owned destinations and release content exist.

#### Audio & Video Calls (`CallSettingsView`)

| Setting | Description |
|---|---|
| WebRTC relay policy | Always relay / relay when needed / never relay |
| ICE servers | Custom STUN/TURN server configuration |

#### Privacy & Security (`PrivacySettingsView`)

Organized in sections:

**Device Section** (`PrivacyDeviceSection`):

| Setting | Description |
|---|---|
| Nome Lock | `SimplexLockView` -- app lock with system auth or passcode (`LAMode.SYSTEM` / `LAMode.PASSCODE`) |

**Chats Section**:

| Setting | Preference Key | Description |
|---|---|---|
| Send link previews | `privacyLinkPreviews` | Auto-generate link preview cards |
| Sanitize links | `privacySanitizeLinks` | Strip tracking parameters from URLs |
| Show last messages | `privacyShowChatPreviews` | Show message previews in chat list |
| Message draft | `privacySaveLastDraft` | Save unsent message draft for each chat |

**Files Section**:

| Setting | Preference Key | Description |
|---|---|---|
| Encrypt local files | `privacyEncryptLocalFiles` | Encrypt files stored on device |
| Auto-accept images | `privacyAcceptImages` | Automatically download received images |
| Blur media radius | `privacyMediaBlurRadius` | Blur radius for media previews |
| Protect IP address | `privacyAskToApproveRelays` | Prompt before connecting to unknown file relays to protect IP address |

#### Appearance (`AppearanceView`)

Platform-specific composable (`expect fun AppearanceView`):

| Setting | Description |
|---|---|
| Profile images | `ProfileImageSection` -- slider for profile image corner radius |
| Theme selection | Color scheme / theme picker |
| Language | App language selection |
| Chat wallpaper | Background image settings |
| Chat bubbles | Message bubble appearance configuration |
| Toolbar opacity | App bar transparency settings (`inAppBarsAlpha`) |
| Color picker | `ClassicColorPicker` for custom theme colors |

### Chat Database Section

| Row | Icon | Destination | Description |
|---|---|---|---|
| Database passphrase & export | `ic_database` | `DatabaseView` | Manage encryption, export/import database |
| Migrate to another device | `ic_ios_share` | `MigrateFromDeviceView` | Device migration (requires auth) |

Database icon shows warning color (`WarningOrange`) when database is not encrypted or passphrase is not saved.

### Help Section

| Row | Icon | Destination | Description |
|---|---|---|---|
| How to use Nome | `ic_help` | `HelpView` | Nome usage guide |
| What's new | `ic_add` | `WhatsNewView` | Mobile-only version changelog; hidden on Nome macOS |
| About Nome | `ic_info` | `SimpleXInfo` (non-onboarding mode) | Nome app and protocol-attribution information |
| Chat with the founder | `ic_tag` | Opens upstream link | Mobile-only upstream route; hidden on Nome macOS |
| Send us an email | `ic_mail` | Opens upstream mailto | Mobile-only upstream route; hidden on Nome macOS |

### Support Section

| Row | Icon | Description |
|---|---|---|
| Contribute | `ic_keyboard` | Opens GitHub contribution page (hidden for Android Bundle) |
| Rate the app | `ic_star` | Opens Google Play / app store listing |
| Star on GitHub | `ic_github` | Opens GitHub repository |

### App Section (`SettingsSectionApp`)

Platform-specific section (expect/actual composable):

| Row | Description |
|---|---|
| App updates (Desktop) | Hidden and forced disabled until a Nome-owned signed/notarized update feed exists |
| Developer tools | Toggle developer mode |
| Chat console | Opens `ChatConsoleView` terminal |
| Terminal always visible (Desktop) | Keep terminal window open |
| Install terminal app | Upstream link is not exposed by the current Nome macOS settings surface |
| Reset all hints | Reset dismissed hint/card preferences |
| App version | Version string with build info; taps open `VersionInfoView` |

## Source Files

| File | Path |
|---|---|
| `SettingsView.kt` | `views/usersettings/SettingsView.kt` |
| `Appearance.kt` | `views/usersettings/Appearance.kt` |
| `PrivacySettings.kt` | `views/usersettings/PrivacySettings.kt` |
| `NetworkAndServers.kt` | `views/usersettings/networkAndServers/NetworkAndServers.kt` |
| `AdvancedNetworkSettings.kt` | `views/usersettings/networkAndServers/AdvancedNetworkSettings.kt` |
| `OperatorView.kt` | `views/usersettings/networkAndServers/OperatorView.kt` |
| `ProtocolServersView.kt` | `views/usersettings/networkAndServers/ProtocolServersView.kt` |
| `NewServerView.kt` | `views/usersettings/networkAndServers/NewServerView.kt` |
