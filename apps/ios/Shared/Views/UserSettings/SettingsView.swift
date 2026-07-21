//
//  SettingsView.swift
//  SimpleX
//
//  Created by Evgeny Poberezkin on 31/01/2022.
//  Copyright © 2022 SimpleX Chat. All rights reserved.
//
// Spec: spec/client/navigation.md

import SwiftUI
import StoreKit
import SimpleXChat

let simplexTeamURL = URL(string: "simplex:/a#lrdvu2d8A1GumSmoKb2krQmtKhWXq-tyGpHuM7aMwsw?h=smp6.simplex.im")!

let appVersion = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String

let appBuild = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion")  as? String

let DEFAULT_SHOW_LA_NOTICE = "showLocalAuthenticationNotice"
let DEFAULT_LA_NOTICE_SHOWN = "localAuthenticationNoticeShown"
let DEFAULT_PERFORM_LA = "performLocalAuthentication" // deprecated, moved to app group
let DEFAULT_LA_MODE = "localAuthenticationMode"
let DEFAULT_LA_LOCK_DELAY = "localAuthenticationLockDelay"
let DEFAULT_LA_SELF_DESTRUCT = "localAuthenticationSelfDestruct"
let DEFAULT_LA_SELF_DESTRUCT_DISPLAY_NAME = "localAuthenticationSelfDestructDisplayName"
let DEFAULT_NOTIFICATION_ALERT_SHOWN = "notificationAlertShown"
let DEFAULT_WEBRTC_POLICY_RELAY = "webrtcPolicyRelay"
let DEFAULT_WEBRTC_ICE_SERVERS = "webrtcICEServers"
let DEFAULT_CALL_KIT_CALLS_IN_RECENTS = "callKitCallsInRecents"
let DEFAULT_PRIVACY_ACCEPT_IMAGES = "privacyAcceptImages" // unused. Use GROUP_DEFAULT_PRIVACY_ACCEPT_IMAGES instead
let DEFAULT_PRIVACY_LINK_PREVIEWS = "privacyLinkPreviews" // deprecated, moved to app group
let DEFAULT_PRIVACY_SIMPLEX_LINK_MODE = "privacySimplexLinkMode"
let DEFAULT_PRIVACY_SHOW_CHAT_PREVIEWS = "privacyShowChatPreviews"
let DEFAULT_PRIVACY_SAVE_LAST_DRAFT = "privacySaveLastDraft"
let DEFAULT_PRIVACY_PROTECT_SCREEN = "privacyProtectScreen"
let DEFAULT_PRIVACY_DELIVERY_RECEIPTS_SET = "privacyDeliveryReceiptsSet"
let DEFAULT_PRIVACY_MEDIA_BLUR_RADIUS = "privacyMediaBlurRadius"
let DEFAULT_EXPERIMENTAL_CALLS = "experimentalCalls"
let DEFAULT_CHAT_ARCHIVE_NAME = "chatArchiveName"
let DEFAULT_CHAT_ARCHIVE_TIME = "chatArchiveTime"
let DEFAULT_CHAT_V3_DB_MIGRATION = "chatV3DBMigration"
let DEFAULT_SHOULD_IMPORT_APP_SETTINGS = "shouldImportAppSettings"
let DEFAULT_DEVELOPER_TOOLS = "developerTools"
let DEFAULT_ENCRYPTION_STARTED = "encryptionStarted"
let DEFAULT_ENCRYPTION_STARTED_AT = "encryptionStartedAt"
let DEFAULT_ACCENT_COLOR_RED = "accentColorRed" // deprecated, only used for migration
let DEFAULT_ACCENT_COLOR_GREEN = "accentColorGreen" // deprecated, only used for migration
let DEFAULT_ACCENT_COLOR_BLUE = "accentColorBlue" // deprecated, only used for migration
let DEFAULT_USER_INTERFACE_STYLE = "userInterfaceStyle" // deprecated, only used for migration
let DEFAULT_PROFILE_IMAGE_CORNER_RADIUS = "profileImageCornerRadius"
let DEFAULT_CHAT_ITEM_ROUNDNESS = "chatItemRoundness"
let DEFAULT_CHAT_ITEM_TAIL = "chatItemTail"
let DEFAULT_ONE_HAND_UI_CARD_SHOWN = "oneHandUICardShown"
let DEFAULT_ADDRESS_CREATION_CARD_SHOWN = "addressCreationCardShown"
let DEFAULT_TOOLBAR_MATERIAL = "toolbarMaterial"
let DEFAULT_CONNECT_VIA_LINK_TAB = "connectViaLinkTab"
let DEFAULT_LIVE_MESSAGE_ALERT_SHOWN = "liveMessageAlertShown"
let DEFAULT_SHOW_HIDDEN_PROFILES_NOTICE = "showHiddenProfilesNotice"
let DEFAULT_SHOW_MUTE_PROFILE_ALERT = "showMuteProfileAlert"
let DEFAULT_SHOW_REPORTS_IN_SUPPORT_CHAT_ALERT = "showReportsInSupportChatAlert"
let DEFAULT_WHATS_NEW_VERSION = "defaultWhatsNewVersion"
let DEFAULT_ONBOARDING_STAGE = "onboardingStage"
let DEFAULT_MIGRATION_TO_STAGE = "migrationToStage"
let DEFAULT_MIGRATION_FROM_STAGE = "migrationFromStage"
let DEFAULT_CUSTOM_DISAPPEARING_MESSAGE_TIME = "customDisappearingMessageTime"
let DEFAULT_SHOW_UNREAD_AND_FAVORITES = "showUnreadAndFavorites"
let DEFAULT_DEVICE_NAME_FOR_REMOTE_ACCESS = "deviceNameForRemoteAccess"
let DEFAULT_CONFIRM_REMOTE_SESSIONS = "confirmRemoteSessions"
let DEFAULT_CONNECT_REMOTE_VIA_MULTICAST = "connectRemoteViaMulticast"
let DEFAULT_CONNECT_REMOTE_VIA_MULTICAST_AUTO = "connectRemoteViaMulticastAuto"
let DEFAULT_SHOW_DELETE_CONVERSATION_NOTICE = "showDeleteConversationNotice"
let DEFAULT_SHOW_DELETE_CONTACT_NOTICE = "showDeleteContactNotice"
let DEFAULT_SHOW_SENT_VIA_RPOXY = "showSentViaProxy"
let DEFAULT_SHOW_SUBSCRIPTION_PERCENTAGE = "showSubscriptionPercentage"

let DEFAULT_CURRENT_THEME = "currentTheme"
let DEFAULT_SYSTEM_DARK_THEME = "systemDarkTheme"
let DEFAULT_CURRENT_THEME_IDS = "currentThemeIds"
let DEFAULT_THEME_OVERRIDES = "themeOverrides"

let DEFAULT_NETWORK_PROXY = "networkProxy"

let ANDROID_DEFAULT_CALL_ON_LOCK_SCREEN = "androidCallOnLockScreen"

let defaultChatItemRoundness: Double = 0.75

let appDefaults: [String: Any] = [
    DEFAULT_SHOW_LA_NOTICE: false,
    DEFAULT_LA_NOTICE_SHOWN: false,
    DEFAULT_PERFORM_LA: false,
    DEFAULT_LA_MODE: LAMode.system.rawValue,
    DEFAULT_LA_LOCK_DELAY: 30,
    DEFAULT_LA_SELF_DESTRUCT: false,
    DEFAULT_NOTIFICATION_ALERT_SHOWN: false,
    DEFAULT_WEBRTC_POLICY_RELAY: true,
    DEFAULT_CALL_KIT_CALLS_IN_RECENTS: false,
    DEFAULT_PRIVACY_ACCEPT_IMAGES: true,
    DEFAULT_PRIVACY_LINK_PREVIEWS: true,
    DEFAULT_PRIVACY_SIMPLEX_LINK_MODE: SimpleXLinkMode.description.rawValue,
    DEFAULT_PRIVACY_SHOW_CHAT_PREVIEWS: true,
    DEFAULT_PRIVACY_SAVE_LAST_DRAFT: true,
    DEFAULT_PRIVACY_PROTECT_SCREEN: false,
    DEFAULT_PRIVACY_DELIVERY_RECEIPTS_SET: false,
    DEFAULT_PRIVACY_MEDIA_BLUR_RADIUS: 0,
    DEFAULT_EXPERIMENTAL_CALLS: false,
    DEFAULT_CHAT_V3_DB_MIGRATION: V3DBMigrationState.offer.rawValue,
    DEFAULT_DEVELOPER_TOOLS: false,
    DEFAULT_ENCRYPTION_STARTED: false,
    DEFAULT_PROFILE_IMAGE_CORNER_RADIUS: defaultProfileImageCorner,
    DEFAULT_CHAT_ITEM_ROUNDNESS: defaultChatItemRoundness,
    DEFAULT_CHAT_ITEM_TAIL: true,
    DEFAULT_ONE_HAND_UI_CARD_SHOWN: false,
    DEFAULT_ADDRESS_CREATION_CARD_SHOWN: false,
    DEFAULT_TOOLBAR_MATERIAL: ToolbarMaterial.defaultMaterial,
    DEFAULT_CONNECT_VIA_LINK_TAB: ConnectViaLinkTab.scan.rawValue,
    DEFAULT_LIVE_MESSAGE_ALERT_SHOWN: false,
    DEFAULT_SHOW_HIDDEN_PROFILES_NOTICE: true,
    DEFAULT_SHOW_MUTE_PROFILE_ALERT: true,
    DEFAULT_SHOW_REPORTS_IN_SUPPORT_CHAT_ALERT: true,
    DEFAULT_ONBOARDING_STAGE: OnboardingStage.onboardingComplete.rawValue,
    DEFAULT_CUSTOM_DISAPPEARING_MESSAGE_TIME: 300,
    DEFAULT_SHOW_UNREAD_AND_FAVORITES: false,
    DEFAULT_CONFIRM_REMOTE_SESSIONS: false,
    DEFAULT_CONNECT_REMOTE_VIA_MULTICAST: true,
    DEFAULT_CONNECT_REMOTE_VIA_MULTICAST_AUTO: true,
    DEFAULT_SHOW_DELETE_CONVERSATION_NOTICE: true,
    DEFAULT_SHOW_DELETE_CONTACT_NOTICE: true,
    DEFAULT_SHOW_SENT_VIA_RPOXY: false,
    DEFAULT_SHOW_SUBSCRIPTION_PERCENTAGE: false,
    ANDROID_DEFAULT_CALL_ON_LOCK_SCREEN: AppSettingsLockScreenCalls.show.rawValue,

    DEFAULT_THEME_OVERRIDES: "{}",
    DEFAULT_CURRENT_THEME: DefaultTheme.SYSTEM_THEME_NAME,
    DEFAULT_SYSTEM_DARK_THEME: DefaultTheme.DARK.themeName,
    DEFAULT_CURRENT_THEME_IDS: "{}"
]

// only Bool defaults can be used here,
// or hintDefaultsUnchanged and resetHintDefaults need to be changed
let hintDefaults = [
    DEFAULT_LA_NOTICE_SHOWN,
    DEFAULT_ONE_HAND_UI_CARD_SHOWN,
    DEFAULT_ADDRESS_CREATION_CARD_SHOWN,
    DEFAULT_LIVE_MESSAGE_ALERT_SHOWN,
    DEFAULT_SHOW_HIDDEN_PROFILES_NOTICE,
    DEFAULT_SHOW_MUTE_PROFILE_ALERT,
    DEFAULT_SHOW_REPORTS_IN_SUPPORT_CHAT_ALERT,
    DEFAULT_SHOW_DELETE_CONVERSATION_NOTICE,
    DEFAULT_SHOW_DELETE_CONTACT_NOTICE
]

let hintGroupDefaults = [
    GROUP_DEFAULT_PRIVACY_LINK_PREVIEWS_SHOW_ALERT
]

// not used anymore
enum ConnectViaLinkTab: String {
    case scan
    case paste
}

enum SimpleXLinkMode: String, Identifiable {
    case description
    case full
    case browser

    static var values: [SimpleXLinkMode] = [.description, .full]

    public var id: Self { self }

    var text: LocalizedStringKey {
        switch self {
        case .description: return "Description"
        case .full: return "Full link"
        case .browser: return "Via browser"
        }
    }
}

private var indent: CGFloat = 36

let chatArchiveTimeDefault = DateDefault(defaults: UserDefaults.standard, forKey: DEFAULT_CHAT_ARCHIVE_TIME)

let encryptionStartedDefault = BoolDefault(defaults: UserDefaults.standard, forKey: DEFAULT_ENCRYPTION_STARTED)

let encryptionStartedAtDefault = DateDefault(defaults: UserDefaults.standard, forKey: DEFAULT_ENCRYPTION_STARTED_AT)

let connectViaLinkTabDefault = EnumDefault<ConnectViaLinkTab>(defaults: UserDefaults.standard, forKey: DEFAULT_CONNECT_VIA_LINK_TAB, withDefault: .scan)

let privacySimplexLinkModeDefault = EnumDefault<SimpleXLinkMode>(defaults: UserDefaults.standard, forKey: DEFAULT_PRIVACY_SIMPLEX_LINK_MODE, withDefault: .description)

let privacyLocalAuthModeDefault = EnumDefault<LAMode>(defaults: UserDefaults.standard, forKey: DEFAULT_LA_MODE, withDefault: .system)

let privacyDeliveryReceiptsSet = BoolDefault(defaults: UserDefaults.standard, forKey: DEFAULT_PRIVACY_DELIVERY_RECEIPTS_SET)

let onboardingStageDefault = EnumDefault<OnboardingStage>(defaults: UserDefaults.standard, forKey: DEFAULT_ONBOARDING_STAGE, withDefault: .onboardingComplete)

let customDisappearingMessageTimeDefault = IntDefault(defaults: UserDefaults.standard, forKey: DEFAULT_CUSTOM_DISAPPEARING_MESSAGE_TIME)

let showDeleteConversationNoticeDefault = BoolDefault(defaults: UserDefaults.standard, forKey: DEFAULT_SHOW_DELETE_CONVERSATION_NOTICE)
let showDeleteContactNoticeDefault = BoolDefault(defaults: UserDefaults.standard, forKey: DEFAULT_SHOW_DELETE_CONTACT_NOTICE)

let showReportsInSupportChatAlertDefault = BoolDefault(defaults: UserDefaults.standard, forKey: DEFAULT_SHOW_REPORTS_IN_SUPPORT_CHAT_ALERT)

/// after importing new database, this flag will be set and unset only after importing app settings in `initializeChat` */
let shouldImportAppSettingsDefault = BoolDefault(defaults: UserDefaults.standard, forKey: DEFAULT_SHOULD_IMPORT_APP_SETTINGS)
let currentThemeDefault = StringDefault(defaults: UserDefaults.standard, forKey: DEFAULT_CURRENT_THEME, withDefault: DefaultTheme.SYSTEM_THEME_NAME)
let systemDarkThemeDefault = StringDefault(defaults: UserDefaults.standard, forKey: DEFAULT_SYSTEM_DARK_THEME, withDefault: DefaultTheme.DARK.themeName)
let currentThemeIdsDefault = CodableDefault<[String: String]>(defaults: UserDefaults.standard, forKey: DEFAULT_CURRENT_THEME_IDS, withDefault: [:] )
let themeOverridesDefault: CodableDefault<[ThemeOverrides]> = CodableDefault(defaults: UserDefaults.standard, forKey: DEFAULT_THEME_OVERRIDES, withDefault: [])

func setGroupDefaults() {
    privacyAcceptImagesGroupDefault.set(UserDefaults.standard.bool(forKey: DEFAULT_PRIVACY_ACCEPT_IMAGES))
    appLocalAuthEnabledGroupDefault.set(UserDefaults.standard.bool(forKey: DEFAULT_PERFORM_LA))
    privacyLinkPreviewsGroupDefault.set(UserDefaults.standard.bool(forKey: DEFAULT_PRIVACY_LINK_PREVIEWS))
    profileImageCornerRadiusGroupDefault.set(UserDefaults.standard.double(forKey: DEFAULT_PROFILE_IMAGE_CORNER_RADIUS))
}

public class StringDefault {
    var defaults: UserDefaults
    var key: String
    var defaultValue: String

    public init(defaults: UserDefaults = UserDefaults.standard, forKey: String, withDefault: String) {
        self.defaults = defaults
        self.key = forKey
        self.defaultValue = withDefault
    }

    public func get() -> String {
        defaults.string(forKey: key) ?? defaultValue
    }

    public func set(_ value: String) {
        defaults.set(value, forKey: key)
        defaults.synchronize()
    }
}

public class CodableDefault<T: Codable> {
    var defaults: UserDefaults
    var key: String
    var defaultValue: T

    public init(defaults: UserDefaults = UserDefaults.standard, forKey: String, withDefault: T) {
        self.defaults = defaults
        self.key = forKey
        self.defaultValue = withDefault
    }

    var cache: T? = nil

    public func get() -> T {
        if let cache {
            return cache
        } else if let value = defaults.string(forKey: key) {
            let res = decodeJSON(value) ?? defaultValue
            cache = res
            return res
        }
        return defaultValue
    }

    public func set(_ value: T) {
        defaults.set(encodeJSON(value), forKey: key)
        cache = value
        //defaults.synchronize()
    }
}

let networkProxyDefault: CodableDefault<NetworkProxy> = CodableDefault(defaults: UserDefaults.standard, forKey: DEFAULT_NETWORK_PROXY, withDefault: NetworkProxy.def)

private enum NomeSettingsPalette {
    static let navy = Color(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0)
    static let green = Color(red: 22.0 / 255.0, green: 174.0 / 255.0, blue: 102.0 / 255.0)
    static let blue = Color(red: 39.0 / 255.0, green: 107.0 / 255.0, blue: 255.0 / 255.0)
}

struct SettingsView: View {
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var chatModel: ChatModel
    @EnvironmentObject var sceneDelegate: SceneDelegate
    @EnvironmentObject var theme: AppTheme
    var embeddedInNomeTab: Bool = false
    @StateObject private var nomeSaveableSettings = SaveableSettings()
    @State private var showProgress: Bool = false

    var body: some View {
        ZStack {
            settingsView()
            if showProgress {
                progressView()
            }
        }
    }

    @ViewBuilder
    func settingsView() -> some View {
        if embeddedInNomeTab {
            nomeSettingsTabView()
                .environmentObject(nomeSaveableSettings)
                .onDisappear {
                    chatModel.showingTerminal = false
                    chatModel.terminalItems = []
                }
        } else {
            List {
            let user = chatModel.currentUser
            if embeddedInNomeTab {
                NomeSettingsLogoHeader()
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 16, leading: 16, bottom: 12, trailing: 16))
            }
            NomeSettingsHeader(user: user)
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 10, leading: 16, bottom: 12, trailing: 16))

            Section(header: Text("设置").foregroundColor(theme.colors.secondary)) {
                NavigationLink {
                    NotificationsView()
                        .navigationTitle("通知")
                        .modifier(ThemedBackground(grouped: true))
                } label: {
                    HStack {
                        notificationsIcon()
                        Text("通知")
                    }
                }
                .disabled(chatModel.chatRunning != true)

                NavigationLink {
                    NomeBackupAndMigrationView(
                        showProgressOnSettings: $showProgress,
                        dismissSettingsSheet: dismiss,
                        chatItemTTL: chatModel.chatItemTTL
                    )
                        .modifier(ThemedBackground(grouped: true))
                } label: {
                    NomeSettingsActionRow(
                        icon: "icloud.and.arrow.up",
                        title: "备份与迁移",
                        subtitle: "数据只保存在你的设备上",
                        trailing: nil
                    )
                }

                NavigationLink {
                    ConnectDesktopView()
                        .navigationTitle("连接桌面")
                        .modifier(ThemedBackground(grouped: true))
                } label: {
                    NomeSettingsActionRow(
                        icon: "desktopcomputer",
                        title: "连接桌面",
                        subtitle: "在电脑上安全使用 Nome",
                        trailing: "未连接"
                    )
                }
                .disabled(chatModel.chatRunning != true)
            }

            Section(header: Text("隐私与安全").foregroundColor(theme.colors.secondary)) {
                NavigationLink {
                    PrivacySettings()
                        .navigationTitle("隐私与安全")
                        .modifier(ThemedBackground(grouped: true))
                } label: {
                    NomeSettingsActionRow(
                        icon: "checkmark.shield",
                        title: "隐私与安全",
                        subtitle: "消息、阅后即焚、权限管理",
                        trailing: nil
                    )
                }
                .disabled(chatModel.chatRunning != true)

                NavigationLink {
                    NetworkAndServers()
                        .navigationTitle("服务器与 Tor")
                        .modifier(ThemedBackground(grouped: true))
                } label: {
                    NomeSettingsActionRow(
                        icon: "globe",
                        title: "服务器与 Tor",
                        subtitle: "路由设置，提升连接隐私",
                        trailing: nil
                    )
                }
                .disabled(chatModel.chatRunning != true)
            }

            Section(header: Text("更多").foregroundColor(theme.colors.secondary)) {
                NavigationLink {
                    CallSettings()
                        .navigationTitle("通话")
                        .modifier(ThemedBackground(grouped: true))
                } label: {
                    settingsRow("video", color: theme.colors.secondary) { Text("音频与视频通话") }
                }
                .disabled(chatModel.chatRunning != true)

                if UIApplication.shared.supportsAlternateIcons {
                    NavigationLink {
                        AppearanceSettings()
                            .navigationTitle("外观")
                            .modifier(ThemedBackground(grouped: true))
                    } label: {
                        settingsRow("sun.max", color: theme.colors.secondary) { Text("外观") }
                    }
                    .disabled(chatModel.chatRunning != true)
                }
            }

            Section(header: Text("高级备份").foregroundColor(theme.colors.secondary)) {
                chatDatabaseRow()
            }

            Section(header: Text("帮助").foregroundColor(theme.colors.secondary)) {
                if user != nil {
                    NavigationLink {
                        NomeHelpView(dismissSettingsSheet: dismiss)
                            .modifier(ThemedBackground())
                            .frame(maxHeight: .infinity, alignment: .top)
                    } label: {
                        settingsRow("questionmark", color: theme.colors.secondary) { Text("使用帮助") }
                    }
                }
                NavigationLink {
                    WhatsNewView(viaSettings: true, updatedConditions: false)
                        .modifier(ThemedBackground())
                        .navigationBarTitleDisplayMode(.inline)
                } label: {
                    settingsRow("plus", color: theme.colors.secondary) { Text("更新内容") }
                }
                NavigationLink {
                    NomeAboutView()
                        .modifier(ThemedBackground())
                        .frame(maxHeight: .infinity, alignment: .top)
                } label: {
                    settingsRow("info", color: theme.colors.secondary) { Text("关于 Nome") }
                }
                settingsRow("number", color: theme.colors.secondary) {
                    Button("发送问题和建议") {
                        dismiss()
                        DispatchQueue.main.async {
                            // simplexTeamURL targets this same app; route to the in-app connect flow
                            // (UIApplication.shared.open is dropped for self-owned URLs in the foreground)
                            ChatModel.shared.appOpenUrl = simplexTeamURL
                        }
                    }
                }
                .disabled(chatModel.chatRunning != true)
                settingsRow("envelope", color: theme.colors.secondary) { Text("[给我们发邮件](mailto:chat@simplex.chat)") }
            }

            Section(header: Text("社区与反馈").foregroundColor(theme.colors.secondary)) {
                settingsRow("keyboard", color: theme.colors.secondary) {
                    ExternalLink("参与开发", destination: URL(string: "https://github.com/simplex-chat/simplex-chat#contribute")!)
                }
                settingsRow("star", color: theme.colors.secondary) {
                    Button("评价 Nome") {
                        if let scene = sceneDelegate.windowScene {
                            SKStoreReviewController.requestReview(in: scene)
                        }
                    }
                }
                ExternalLink(destination: URL(string: "https://github.com/simplex-chat/simplex-chat")!) {
                    ZStack(alignment: .leading) {
                        Image(colorScheme == .dark ? "github_light" : "github")
                            .resizable()
                            .frame(width: 24, height: 24)
                            .opacity(0.5)
                            .colorMultiply(theme.colors.secondary)
                        Text("GitHub 项目")
                            .padding(.leading, indent)
                    }
                }
            }

            Section(header: Text("开发").foregroundColor(theme.colors.secondary)) {
                NavigationLink {
                    DeveloperView()
                        .navigationTitle("开发者工具")
                        .modifier(ThemedBackground(grouped: true))
                } label: {
                    settingsRow("chevron.left.forwardslash.chevron.right", color: theme.colors.secondary) { Text("开发者工具") }
                }
                NavigationLink {
                    VersionView()
                        .navigationBarTitle("应用版本")
                        .modifier(ThemedBackground())
                } label: {
                    Text("v\(appVersion ?? "?") (\(appBuild ?? "?"))")
                }
            }
            }
            .modifier(ThemedBackground(grouped: true))
            .navigationTitle("设置")
            .onDisappear {
                chatModel.showingTerminal = false
                chatModel.terminalItems = []
            }
        }
    }

    private func nomeSettingsTabView() -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                NomeSettingsLogoHeader()
                    .padding(.top, 18)

                NavigationLink {
                    UserProfile()
                        .modifier(ThemedBackground(grouped: true))
                } label: {
                    NomeSettingsHeader(user: chatModel.currentUser)
                }
                .buttonStyle(.plain)

                NomeSettingsTabSection(title: "设置") {
                    NavigationLink {
                        NotificationsView()
                            .navigationTitle("通知")
                            .modifier(ThemedBackground(grouped: true))
                    } label: {
                        NomeSettingsTabRow(icon: "bell", title: "通知", subtitle: nil, trailing: nil)
                    }
                    .disabled(chatModel.chatRunning != true)

                    Divider().padding(.leading, 52)

                    NavigationLink {
                        NomeBackupAndMigrationView(
                            showProgressOnSettings: $showProgress,
                            dismissSettingsSheet: dismiss,
                            chatItemTTL: chatModel.chatItemTTL
                        )
                            .modifier(ThemedBackground(grouped: true))
                    } label: {
                        NomeSettingsTabRow(icon: "icloud.and.arrow.up", title: "备份与迁移", subtitle: "数据只保存在你的设备上", trailing: nil)
                    }

                    Divider().padding(.leading, 52)

                    NavigationLink {
                        DatabaseView(dismissSettingsSheet: dismiss, chatItemTTL: chatModel.chatItemTTL)
                            .navigationTitle("数据与存储")
                            .modifier(ThemedBackground(grouped: true))
                    } label: {
                        NomeSettingsTabRow(icon: "internaldrive", title: "数据与存储", subtitle: "数据库密码、导出与本地文件", trailing: nil)
                    }

                    Divider().padding(.leading, 52)

                    NavigationLink {
                        ConnectDesktopView()
                            .navigationTitle("连接桌面")
                            .modifier(ThemedBackground(grouped: true))
                    } label: {
                        NomeSettingsTabRow(icon: "desktopcomputer", title: "连接桌面", subtitle: "在电脑上安全使用 Nome", trailing: "未连接")
                    }
                    .disabled(chatModel.chatRunning != true)
                }

                NomeSettingsTabSection(title: "隐私与安全") {
                    NavigationLink {
                        PrivacySettings()
                            .navigationTitle("隐私与安全")
                            .modifier(ThemedBackground(grouped: true))
                    } label: {
                        NomeSettingsTabRow(icon: "checkmark.shield", title: "隐私与安全", subtitle: "消息、阅后即焚、权限管理", trailing: nil)
                    }
                    .disabled(chatModel.chatRunning != true)

                    Divider().padding(.leading, 52)

                    NavigationLink {
                        NetworkAndServers()
                            .environmentObject(nomeSaveableSettings)
                            .navigationTitle("服务器与 Tor")
                            .modifier(ThemedBackground(grouped: true))
                    } label: {
                        NomeSettingsTabRow(icon: "globe", title: "服务器与 Tor", subtitle: "路由设置，提升连接隐私", trailing: nil)
                    }
                    .disabled(chatModel.chatRunning != true)
                }

                NomeSettingsTabSection(title: "高级") {
                    NavigationLink {
                        SettingsView()
                            .navigationTitle("完整设置")
                            .modifier(ThemedBackground(grouped: true))
                    } label: {
                        NomeSettingsTabRow(
                            icon: "slider.horizontal.3",
                            title: "完整设置",
                            subtitle: "查看通话、外观、开发者工具和全部安全控制",
                            trailing: nil
                        )
                    }
                }

                NomeSettingsTabSection(title: nil) {
                    if chatModel.currentUser != nil {
                        NavigationLink {
                            NomeHelpView(dismissSettingsSheet: dismiss)
                                .modifier(ThemedBackground())
                                .frame(maxHeight: .infinity, alignment: .top)
                        } label: {
                            NomeSettingsTabRow(icon: "questionmark.circle", title: "帮助与反馈", subtitle: nil, trailing: nil)
                        }
                    }

                    Divider().padding(.leading, 52)

                    NavigationLink {
                        NomeAboutView()
                            .modifier(ThemedBackground())
                            .frame(maxHeight: .infinity, alignment: .top)
                    } label: {
                        NomeSettingsTabRow(icon: "info.circle", title: "关于 Nome", subtitle: "版本 \(appVersion ?? "?")（本地版）", trailing: nil)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 98)
        }
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
    }
    
    private func chatDatabaseRow() -> some View {
        NavigationLink {
            DatabaseView(dismissSettingsSheet: dismiss, chatItemTTL: chatModel.chatItemTTL)
                .navigationTitle("聊天数据库")
                .modifier(ThemedBackground(grouped: true))
        } label: {
            let color: Color = chatModel.chatDbEncrypted == false ? .orange : theme.colors.secondary
            settingsRow("internaldrive", color: color) {
                HStack {
                    Text("数据库密码与导出")
                    Spacer()
                    if chatModel.chatRunning == false {
                        Image(systemName: "exclamationmark.octagon.fill").foregroundColor(.red)
                    }
                }
            }
        }
    }

    private func progressView() -> some View {
        VStack {
            ProgressView().scaleEffect(2)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity )
    }

    private enum NotificationAlert {
        case enable
        case error(LocalizedStringKey, String)
    }

    private func notificationsIcon() -> some View {
        let icon: String
        let color: Color
        switch (chatModel.tokenStatus) {
        case .new:
            icon = "bolt"
            color = theme.colors.secondary
        case .registered:
            icon = "bolt.fill"
            color = theme.colors.secondary
        case .invalid: fallthrough
        case .invalidBad: fallthrough
        case .invalidTopic: fallthrough
        case .invalidExpired: fallthrough
        case .invalidUnregistered:
            icon = "bolt.slash"
            color = theme.colors.secondary
        case .confirmed:
            icon = "bolt.fill"
            color = .yellow
        case .active:
            icon = "bolt.fill"
            color = .green
        case .expired:
            icon = "bolt.slash.fill"
            color = theme.colors.secondary
        case .none:
            icon = "bolt"
            color = theme.colors.secondary
        }
        return Image(systemName: icon)
            .padding(.trailing, 9)
            .foregroundColor(color)
    }
}

private struct NomeSettingsLogoHeader: View {
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        HStack {
            Spacer()
            Image(colorScheme == .light ? "logo" : "logo-light")
                .resizable()
                .scaledToFit()
                .frame(width: 138, height: 42)
                .accessibilityHidden(true)
            Spacer()
        }
        .padding(.top, 6)
    }
}

private struct NomeSettingsHeader: View {
    @Environment(\.colorScheme) var colorScheme
    let user: User?

    var body: some View {
        HStack(spacing: 14) {
            if let user {
                ProfileImage(imageStr: user.image, size: 74, color: Color(uiColor: .tertiarySystemGroupedBackground))
            } else {
                Image(colorScheme == .light ? "icon-light" : "icon-dark")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 74, height: 74)
                    .accessibilityHidden(true)
            }

            VStack(alignment: .leading, spacing: 5) {
                Text("主身份")
                    .font(.title3.weight(.bold))
                    .foregroundColor(NomeSettingsPalette.navy)
                    .lineLimit(1)
                Text(user?.displayName ?? "当前设备上的身份")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .foregroundColor(.secondary)
                .font(.headline.weight(.semibold))
        }
        .padding(16)
        .frame(minHeight: 112)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(Color.black.opacity(0.06), lineWidth: 1)
        )
    }
}

private struct NomeSettingsActionRow: View {
    let icon: String
    let title: LocalizedStringKey
    let subtitle: LocalizedStringKey
    let trailing: LocalizedStringKey?

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 22, weight: .medium))
                .foregroundColor(NomeSettingsPalette.navy)
                .frame(width: 34, height: 34)

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.body.weight(.semibold))
                    .foregroundColor(NomeSettingsPalette.navy)
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)

            if let trailing {
                Text(trailing)
                    .font(.caption.weight(.semibold))
                    .foregroundColor(NomeSettingsPalette.blue)
                    .padding(.horizontal, 10)
                    .frame(height: 26)
                    .background(Capsule().fill(NomeSettingsPalette.blue.opacity(0.1)))
            }
        }
        .padding(.vertical, 4)
    }
}

private struct NomeSettingsTabSection<Content: View>: View {
    let title: LocalizedStringKey?
    let content: Content

    init(title: LocalizedStringKey?, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let title {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(.secondary)
                    .padding(.leading, 2)
            }

            VStack(spacing: 0) {
                content
            }
            .padding(.vertical, 6)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color(uiColor: .systemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(Color.black.opacity(0.06), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct NomeSettingsTabRow: View {
    let icon: String
    let title: LocalizedStringKey
    let subtitle: LocalizedStringKey?
    let trailing: LocalizedStringKey?

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 23, weight: .medium))
                .foregroundColor(NomeSettingsPalette.navy)
                .frame(width: 38, height: 46)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.body.weight(.semibold))
                    .foregroundColor(NomeSettingsPalette.navy)
                if let subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            Spacer(minLength: 0)

            if let trailing {
                Text(trailing)
                    .font(.caption.weight(.semibold))
                    .foregroundColor(NomeSettingsPalette.blue)
                    .padding(.horizontal, 10)
                    .frame(height: 26)
                    .background(Capsule().fill(NomeSettingsPalette.blue.opacity(0.1)))
            }

            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .contentShape(Rectangle())
    }
}

private struct NomeBackupAndMigrationView: View {
    @EnvironmentObject var chatModel: ChatModel
    @Binding var showProgressOnSettings: Bool
    let dismissSettingsSheet: DismissAction
    let chatItemTTL: ChatItemTTL

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                backupIntroCard

                NomeSettingsTabSection(title: "常用操作") {
                    NavigationLink {
                        DatabaseView(dismissSettingsSheet: dismissSettingsSheet, chatItemTTL: chatItemTTL)
                            .navigationTitle("数据与存储")
                            .modifier(ThemedBackground(grouped: true))
                    } label: {
                        NomeSettingsTabRow(
                            icon: "square.and.arrow.up",
                            title: "导出或导入数据库",
                            subtitle: "生成本地备份文件，或从备份文件恢复",
                            trailing: nil
                        )
                    }

                    Divider().padding(.leading, 52)

                    NavigationLink {
                        MigrateFromDevice(showProgressOnSettings: $showProgressOnSettings)
                            .toolbar {
                                ToolbarItem(placement: .principal) {
                                    Text("迁移到新设备").font(.headline)
                                }
                            }
                            .modifier(ThemedBackground(grouped: true))
                            .navigationBarTitleDisplayMode(.large)
                    } label: {
                        NomeSettingsTabRow(
                            icon: "iphone.and.arrow.forward",
                            title: "迁移到新设备",
                            subtitle: "生成加密迁移二维码；开始后会暂时停止聊天",
                            trailing: nil
                        )
                    }
                    .disabled(chatModel.chatRunning != true)
                }

                receiveMigrationCard
                backupSafetyCard
            }
            .padding(.horizontal, 16)
            .padding(.top, 18)
            .padding(.bottom, 40)
        }
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
        .navigationTitle("备份与迁移")
        .navigationBarTitleDisplayMode(.large)
    }

    private var backupIntroCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "lock.shield")
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundColor(NomeSettingsPalette.green)
                    .frame(width: 42, height: 42)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeSettingsPalette.green.opacity(0.12))
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text("你的 Nome 数据只在本机")
                        .font(.headline)
                        .foregroundColor(NomeSettingsPalette.navy)
                    Text("备份和迁移都会使用现有数据库与加密流程。")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(Color.black.opacity(0.06), lineWidth: 1)
        )
    }

    private var receiveMigrationCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("在新设备接收")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
            Text("在新 iPhone 首次设置 Nome 时，选择从另一台设备迁入，然后扫描这里生成的迁移二维码。")
                .font(.callout)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(Color.black.opacity(0.06), lineWidth: 1)
        )
    }

    private var backupSafetyCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("安全提示")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
            Text("导出的数据库文件需要妥善保存。迁移到新设备时，旧设备会先准备加密归档，再通过二维码或安全链接交给新设备。")
                .font(.callout)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(Color.black.opacity(0.06), lineWidth: 1)
        )
    }
}

private struct NomeAboutView: View {
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .center, spacing: 16) {
                    Image(colorScheme == .light ? "logo" : "logo-light")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 150, height: 46)
                        .accessibilityHidden(true)

                    Text("私密连接，简单使用")
                        .font(.title3.weight(.bold))
                        .foregroundColor(NomeSettingsPalette.navy)
                        .multilineTextAlignment(.center)

                    Text("Nome 是一款重视隐私的通信应用，把连接、身份和备份说得更清楚。")
                        .font(.callout)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(18)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color(uiColor: .systemBackground))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .stroke(Color.black.opacity(0.06), lineWidth: 1)
                )

                NomeAboutSection(title: "版本") {
                    NomeAboutInfoRow(
                        icon: "app.badge",
                        title: "Nome",
                        subtitle: "v\(appVersion ?? "?") (\(appBuild ?? "?"))"
                    )
                }

                NomeAboutSection(title: "安全边界") {
                    NomeAboutInfoRow(
                        icon: "link.badge.plus",
                        title: "一次性连接",
                        subtitle: "用一次性链接或二维码添加联系人，避免公开账号 ID。"
                    )

                    Divider().padding(.leading, 52)

                    NomeAboutInfoRow(
                        icon: "lock.shield",
                        title: "本机数据",
                        subtitle: "聊天数据库、备份和迁移继续使用现有加密机制。"
                    )

                    Divider().padding(.leading, 52)

                    NomeAboutInfoRow(
                        icon: "network",
                        title: "私密连接",
                        subtitle: "一次性邀请、公开联系方式和分散式消息传递共同减少关系暴露。"
                    )
                }

                NomeAboutSection(title: "开源与许可") {
                    NomeAboutInfoRow(
                        icon: "curlybraces",
                        title: "开源技术",
                        subtitle: "Nome 使用开源通信技术构建，许可证与源代码信息保持公开。"
                    )

                    Divider().padding(.leading, 52)

                    Link(destination: URL(string: "https://github.com/simplex-chat/simplex-chat")!) {
                        NomeAboutInfoRow(
                            icon: "arrow.up.forward.app",
                            title: "查看源代码与许可",
                            subtitle: "GitHub 开源仓库"
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 18)
            .padding(.bottom, 40)
        }
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
        .navigationTitle("关于 Nome")
        .navigationBarTitleDisplayMode(.large)
    }
}

private struct NomeAboutSection<Content: View>: View {
    let title: String
    let content: Content

    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(verbatim: title)
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)
                .padding(.leading, 2)

            VStack(spacing: 0) {
                content
            }
            .padding(.vertical, 6)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color(uiColor: .systemBackground))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(Color.black.opacity(0.06), lineWidth: 1)
            )
        }
    }
}

private struct NomeAboutInfoRow: View {
    let icon: String
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 22, weight: .medium))
                .foregroundColor(NomeSettingsPalette.navy)
                .frame(width: 38, height: 46)

            VStack(alignment: .leading, spacing: 4) {
                Text(verbatim: title)
                    .font(.body.weight(.semibold))
                    .foregroundColor(NomeSettingsPalette.navy)
                Text(verbatim: subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .contentShape(Rectangle())
    }
}

private struct NomeHelpView: View {
    let dismissSettingsSheet: DismissAction

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 10) {
                    Text("先完成三件事")
                        .font(.title3.weight(.bold))
                        .foregroundColor(NomeSettingsPalette.navy)
                    Text("添加一个朋友、保存自己的公开联系方式、确认备份方式。这样 Nome 就能进入日常使用。")
                        .font(.callout)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color(uiColor: .systemBackground))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .stroke(Color.black.opacity(0.06), lineWidth: 1)
                )

                NomeAboutSection(title: "开始使用") {
                    NomeAboutInfoRow(
                        icon: "link.badge.plus",
                        title: "添加朋友",
                        subtitle: "用一次性链接或二维码连接一个联系人。链接只适合发给你信任的人。"
                    )

                    Divider().padding(.leading, 52)

                    NomeAboutInfoRow(
                        icon: "qrcode.viewfinder",
                        title: "加入群组",
                        subtitle: "粘贴或扫描群组邀请，先确认来源，再加入或等待审批。"
                    )

                    Divider().padding(.leading, 52)

                    NomeAboutInfoRow(
                        icon: "person.text.rectangle",
                        title: "公开联系方式",
                        subtitle: "需要长期被别人找到时使用；一对一邀请仍建议用一次性链接。"
                    )
                }

                NomeAboutSection(title: "保护自己") {
                    NomeAboutInfoRow(
                        icon: "person.crop.circle.badge.checkmark",
                        title: "身份中心",
                        subtitle: "把不同社交场景分开；隐身身份适合临时或低信任连接。"
                    )

                    Divider().padding(.leading, 52)

                    NomeAboutInfoRow(
                        icon: "lock.shield",
                        title: "隐私与安全",
                        subtitle: "检查 Nome Lock、链接预览、屏幕保护、阅后即焚和权限设置。"
                    )

                    Divider().padding(.leading, 52)

                    NomeAboutInfoRow(
                        icon: "externaldrive.badge.icloud",
                        title: "备份与迁移",
                        subtitle: "聊天数据主要在本机。换机或重装前，先导出数据库或准备迁移二维码。"
                    )
                }

                NomeAboutSection(title: "反馈") {
                    Button {
                        dismissSettingsSheet()
                        DispatchQueue.main.async {
                            ChatModel.shared.appOpenUrl = simplexTeamURL
                        }
                    } label: {
                        NomeAboutInfoRow(
                            icon: "bubble.left.and.exclamationmark.bubble.right",
                            title: "发送问题和建议",
                            subtitle: "连接到项目支持对话，反馈 Nome 使用中的困惑。"
                        )
                    }
                    .buttonStyle(.plain)

                    Divider().padding(.leading, 52)

                    Link(destination: URL(string: "mailto:chat@simplex.chat")!) {
                        NomeAboutInfoRow(
                            icon: "envelope",
                            title: "邮件联系",
                            subtitle: "chat@simplex.chat"
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 18)
            .padding(.bottom, 40)
        }
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
        .navigationTitle("帮助与反馈")
        .navigationBarTitleDisplayMode(.large)
    }
}

func settingsRow<Content : View>(_ icon: String, color: Color/* = .secondary*/, content: @escaping () -> Content) -> some View {
    ZStack(alignment: .leading) {
        Image(systemName: icon).frame(maxWidth: 24, maxHeight: 24, alignment: .center)
            .symbolRenderingMode(.monochrome)
            .foregroundColor(color)
        content().padding(.leading, indent)
    }
}

struct ProfilePreview: View {
    var profileOf: NamedChat
    var color = Color(uiColor: .tertiarySystemGroupedBackground)
    var badge: LocalBadge? = nil

    var body: some View {
        HStack {
            ProfileImage(imageStr: profileOf.image, size: 44, color: color)
                .padding(.trailing, 6)
            NameWithBadge(profileName(profileOf), badge, .title2)
                .lineLimit(1)
        }
    }
}

func profileName(_ profileOf: NamedChat) -> Text {
    var t = Text(profileOf.displayName).fontWeight(.semibold).font(.title2)
    if profileOf.fullName != "" && profileOf.fullName != profileOf.displayName {
        t = t + Text(verbatim: " (" + profileOf.fullName + ")")
//                        .font(.callout)
        }
    return t
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        let chatModel = ChatModel()
        chatModel.currentUser = User.sampleData
        return SettingsView()
            .environmentObject(chatModel)
    }
}
