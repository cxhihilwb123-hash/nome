//
//  SimpleXApp.swift
//  Shared
//
//  Created by Evgeny Poberezkin on 17/01/2022.
//
// Spec: spec/architecture.md

import SwiftUI
import OSLog
import SimpleXChat

let logger = Logger()

#if DEBUG
private enum NomePrimaryFlowPreview {
    case addFriend
    case joinGroup
    case publicContact
    case settings
}
#endif

@main
// Spec: spec/architecture.md#SimpleXApp
struct SimpleXApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var chatModel = ChatModel.shared
    @StateObject private var activationStore = NomeActivationStore.shared
    @ObservedObject var alertManager = AlertManager.shared

    @Environment(\.scenePhase) var scenePhase
    @State private var enteredBackgroundAuthenticated: TimeInterval? = nil

    init() {
        DispatchQueue.global(qos: .background).sync {
            haskell_init()
//            hs_init(0, nil)
        }
        UserDefaults.standard.register(defaults: appDefaults)
        setGroupDefaults()
        registerGroupDefaults()
        setDbContainer()
        BGManager.shared.register()
        NtfManager.shared.registerCategories()
    }

    var body: some Scene {
        WindowGroup {
            // contentAccessAuthenticationExtended has to be passed to ContentView on view initialization,
            // so that it's computed by the time view renders, and not on event after rendering
            rootView
                .onOpenURL { url in
                    logger.debug("ContentView.onOpenURL: \(url)")
                    guard NomeActivationGate.allowsNetworking else {
                        // Keep blocked URLs out of ChatModel's foreground auto-consume path.
                        activationStore.presentDeepLink(url)
                        return
                    }
                    if AppChatState.shared.value == .active {
                        chatModel.appOpenUrl = url
                    } else {
                        chatModel.appOpenUrlLater = url
                    }
                }
                .onAppear() {
                    if isNomeDebugPreview { return }
                    Task { await activationStore.refreshIfNeeded(force: true) }
                    // Present screen for continue migration if it wasn't finished yet
                    if chatModel.migrationState != nil {
                        // It's important, otherwise, user may be locked in undefined state
                        onboardingStageDefault.set(.step1_SimpleXInfo)
                        chatModel.onboardingStage = onboardingStageDefault.get()
                    } else if !UserDefaults.standard.bool(forKey: DEFAULT_PERFORM_LA) || kcAppPassword.get() == nil || kcSelfDestructPassword.get() == nil {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                            initChatAndMigrate()
                        }
                    }
                }
// Spec: spec/architecture.md#scenePhaseHandling
                .onChange(of: scenePhase) { phase in
                    if isNomeDebugPreview { return }
                    logger.debug("scenePhase was \(String(describing: scenePhase)), now \(String(describing: phase))")
                    AppSheetState.shared.scenePhaseActive = phase == .active
                    switch (phase) {
                    case .background:
                        // --- authentication
                        // see ContentView .onChange(of: scenePhase) for remaining authentication logic
                        if chatModel.contentViewAccessAuthenticated {
                            enteredBackgroundAuthenticated = ProcessInfo.processInfo.systemUptime
                        }
                        chatModel.contentViewAccessAuthenticated = false
                        // authentication ---

                        if CallController.useCallKit() && chatModel.activeCall != nil {
                            CallController.shared.shouldSuspendChat = true
                        } else {
                            suspendChat()
                            if NomeActivationGate.allowsNetworking {
                                BGManager.shared.schedule()
                            }
                        }
                        NtfManager.shared.setNtfBadgeCount(chatModel.totalUnreadCountForAllUsers())
                    case .active:
                        CallController.shared.shouldSuspendChat = false
                        Task { await activationStore.refreshIfNeeded() }
                        let appState = AppChatState.shared.value

                        if appState != .stopped && NomeActivationGate.allowsNetworking {
                            startChatAndActivate {
                                if chatModel.chatRunning == true {
                                    if let ntfResponse = chatModel.notificationResponse {
                                        chatModel.notificationResponse = nil
                                        NtfManager.shared.processNotificationResponse(ntfResponse)
                                    }
                                    if appState.inactive {
                                        Task {
                                            await updateChats()
                                            if !chatModel.showCallView && !CallController.shared.hasActiveCalls() {
                                                await updateCallInvitations()
                                            }
                                            if let url = chatModel.appOpenUrlLater {
                                                await MainActor.run {
                                                    chatModel.appOpenUrlLater = nil
                                                    chatModel.appOpenUrl = url
                                                }
                                            }
                                        }
                                    } else if let url = chatModel.appOpenUrlLater {
                                        chatModel.appOpenUrlLater = nil
                                        chatModel.appOpenUrl = url
                                    }
                                }
                            }
                        }
                    default:
                        break
                    }
                }
        }
    }

    @ViewBuilder private var rootView: some View {
        #if DEBUG
        if isNomeActivationPreview {
            NomeActivationPreviewHost()
                .environmentObject(activationStore)
        } else if isNomeConversationPreview {
            NomeConversationPreviewHost()
                .environmentObject(chatModel)
                .environmentObject(AppTheme.shared)
                .environmentObject(activationStore)
                .sheet(item: $activationStore.presentation) { presentation in
                    NomeActivationSheetView(action: presentation.action)
                        .environmentObject(activationStore)
                }
                .overlay(alignment: .topLeading) {
                    Text(activationStore.effectiveAccess.rawValue)
                        .font(.system(size: 1))
                        .opacity(0.001)
                        .accessibilityIdentifier("nome.activation.previewAccess")
                }
                .task {
                    await activationStore.refreshIfNeeded(force: true)
                }
        } else if isNomeContactsPreview {
            NomeChatListPreviewHost(showContacts: true)
                .environmentObject(chatModel)
                .environmentObject(AppTheme.shared)
        } else if isNomeChatListPreview {
            NomeChatListPreviewHost()
                .environmentObject(chatModel)
                .environmentObject(AppTheme.shared)
        } else if isNomeIdentityCenterPreview {
            NomeIdentityCenterPreviewHost()
                .environmentObject(chatModel)
                .environmentObject(AppTheme.shared)
        } else if let nomePrimaryFlowPreview {
            NomePrimaryFlowPreviewHost(flow: nomePrimaryFlowPreview)
                .environmentObject(chatModel)
                .environmentObject(AppTheme.shared)
        } else if let nomeOnboardingPreviewStage {
            OnboardingView(onboarding: nomeOnboardingPreviewStage)
                .environmentObject(chatModel)
                .environmentObject(AppTheme.shared)
        } else {
            contentRootView
        }
        #else
        contentRootView
        #endif
    }

    private var contentRootView: some View {
        ContentView(contentAccessAuthenticationExtended: !authenticationExpired())
            .environmentObject(chatModel)
            .environmentObject(AppTheme.shared)
            .environmentObject(activationStore)
    }

    private var isNomeDebugPreview: Bool {
        #if DEBUG
        isNomeActivationPreview ||
        isNomeConversationPreview ||
        isNomeChatListPreview ||
        isNomeContactsPreview ||
        isNomeIdentityCenterPreview ||
        nomePrimaryFlowPreview != nil ||
        nomeOnboardingPreviewStage != nil
        #else
        false
        #endif
    }

    private var isNomeActivationPreview: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("-NomeActivationPreview")
        #else
        false
        #endif
    }

    private var isNomeConversationPreview: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("-NomeConversationPreview")
        #else
        false
        #endif
    }

    private var isNomeChatListPreview: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("-NomeChatListPreview")
        #else
        false
        #endif
    }

    private var isNomeContactsPreview: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("-NomeContactsPreview")
        #else
        false
        #endif
    }

    private var isNomeIdentityCenterPreview: Bool {
        #if DEBUG
        ProcessInfo.processInfo.arguments.contains("-NomeIdentityCenterPreview")
        #else
        false
        #endif
    }

    private var nomePrimaryFlowPreview: NomePrimaryFlowPreview? {
        #if DEBUG
        let args = ProcessInfo.processInfo.arguments
        if args.contains("-NomeAddFriendPreview") {
            return .addFriend
        } else if args.contains("-NomeJoinGroupPreview") {
            return .joinGroup
        } else if args.contains("-NomePublicContactPreview") {
            return .publicContact
        } else if args.contains("-NomeSettingsPreview") {
            return .settings
        } else {
            return nil
        }
        #else
        return nil
        #endif
    }

    private var nomeOnboardingPreviewStage: OnboardingStage? {
        #if DEBUG
        let args = ProcessInfo.processInfo.arguments
        if args.contains("-NomeOnboardingWelcomePreview") {
            return .step1_SimpleXInfo
        } else if args.contains("-NomeOnboardingProfilePreview") {
            return .step2_CreateProfile
        } else if args.contains("-NomeOnboardingNetworkPreview") {
            return .step3_ChooseServerOperators
        } else if args.contains("-NomeOnboardingConditionsPreview") {
            return .step4_NetworkCommitments
        } else {
            return nil
        }
        #else
        return nil
        #endif
    }

    private func setDbContainer() {
// Uncomment and run once to open DB in app documents folder:
//         dbContainerGroupDefault.set(.documents)
//         v3DBMigrationDefault.set(.offer)
// to create database in app documents folder also uncomment:
//         let legacyDatabase = true
        let legacyDatabase = hasLegacyDatabase()
        if legacyDatabase, case .documents = dbContainerGroupDefault.get() {
            dbContainerGroupDefault.set(.documents)
            setMigrationState(.offer)
            logger.debug("SimpleXApp init: using legacy DB in documents folder: \(getAppDatabasePath())*.db")
        } else {
            dbContainerGroupDefault.set(.group)
            setMigrationState(.ready)
            logger.debug("SimpleXApp init: using DB in app group container: \(getAppDatabasePath())*.db")
            logger.debug("SimpleXApp init: legacy DB\(legacyDatabase ? "" : " not") present")
        }
    }

    private func setMigrationState(_ state: V3DBMigrationState) {
        if case .migrated = v3DBMigrationDefault.get() { return }
        v3DBMigrationDefault.set(state)
    }

    private func authenticationExpired() -> Bool {
        if let enteredBackgroundAuthenticated = enteredBackgroundAuthenticated {
            let delay = Double(UserDefaults.standard.integer(forKey: DEFAULT_LA_LOCK_DELAY))
            return ProcessInfo.processInfo.systemUptime - enteredBackgroundAuthenticated >= delay
        } else {
            return true
        }
    }

    private func updateChats() async {
        do {
            let chats = try await apiGetChatsAsync()
            await MainActor.run { chatModel.updateChats(chats) }
            if let id = chatModel.chatId,
               let chat = chatModel.getChat(id),
               !NtfManager.shared.navigatingToChat {
                Task { await loadChat(chat: chat, im: ItemsModel.shared, clearItems: false) }
            }
            if let ncr = chatModel.ntfContactRequest {
                await MainActor.run { chatModel.ntfContactRequest = nil }
                if case let .contactRequest(contactRequest) = chatModel.getChat(ncr.chatId)?.chatInfo {
                    Task { await acceptContactRequest(incognito: false, contactRequestId: contactRequest.apiId) }
                }
            }
        } catch let error {
            logger.error("apiGetChats: cannot update chats \(responseError(error))")
        }
    }

    private func updateCallInvitations() async {
        do {
            try await refreshCallInvitations()
        } catch let error {
            logger.error("apiGetCallInvitations: cannot update call invitations \(responseError(error))")
        }
    }
}

#if DEBUG
private struct NomeActivationPreviewHost: View {
    var body: some View {
        NomeActivationSheetView(action: .message)
    }
}
#endif

#if DEBUG
private struct NomePrimaryFlowPreviewHost: View {
    let flow: NomePrimaryFlowPreview

    init(flow: NomePrimaryFlowPreview) {
        self.flow = flow
        Self.configurePreviewModel(for: flow)
    }

    var body: some View {
        NavigationView {
            previewView
        }
        .navigationViewStyle(.stack)
        .onAppear {
            Self.configurePreviewModel(for: flow)
        }
    }

    @ViewBuilder private var previewView: some View {
        switch flow {
        case .addFriend:
            NewChatView(selection: .invite)
                .navigationTitle("添加朋友")
                .navigationBarTitleDisplayMode(.inline)
                .modifier(ThemedBackground(grouped: true))
        case .joinGroup:
            NewChatView(selection: .connect, showQRCodeScanner: false, connectMode: .group)
                .navigationTitle("加入群组")
                .navigationBarTitleDisplayMode(.inline)
                .modifier(ThemedBackground(grouped: true))
        case .publicContact:
            UserAddressView(shareViaProfile: true)
                .navigationTitle("公开联系方式")
                .navigationBarTitleDisplayMode(.large)
                .modifier(ThemedBackground(grouped: true))
        case .settings:
            SettingsView(embeddedInNomeTab: true)
                .navigationTitle("设置")
                .navigationBarTitleDisplayMode(.inline)
        }
    }

    private static func configurePreviewModel(for flow: NomePrimaryFlowPreview) {
        let chatModel = ChatModel.shared
        var user = User.sampleData
        user.agentUserId = "preview-agent"
        user.profile.displayName = "alice"
        user.profile.fullName = "Alice"

        chatModel.currentUser = user
        chatModel.chatRunning = true
        chatModel.chatInitialized = true
        chatModel.onboardingStage = nil
        chatModel.users = [UserInfo.sampleData]
        chatModel.updateChats([])
        chatModel.userAddress = flow == .publicContact ? samplePublicContactAddress : nil
    }

    private static var samplePublicContactAddress: UserContactLink {
        UserContactLink(
            CreatedConnLink(
                connFullLink: "simplex:/contact#preview",
                connShortLink: "https://nome.local/preview"
            )
        )
    }
}
#endif
