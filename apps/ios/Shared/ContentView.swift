//
//  ContentView.swift
//  Shared
//
//  Created by Evgeny Poberezkin on 17/01/2022.
//
// Spec: spec/client/navigation.md

import SwiftUI
import Intents
import SimpleXChat

private enum NoticesSheet: Identifiable {
    case whatsNew(updatedConditions: Bool)

    var id: String {
        switch self {
        case .whatsNew: return "whatsNew"
        }
    }
}

// Spec: spec/client/navigation.md#ContentView
struct ContentView: View {
    @EnvironmentObject var chatModel: ChatModel
    @EnvironmentObject var activationStore: NomeActivationStore
    @ObservedObject var alertManager = AlertManager.shared
    @ObservedObject var callController = CallController.shared
    // Spec: spec/client/navigation.md#AppSheetState
    @ObservedObject var appSheetState = AppSheetState.shared
    @Environment(\.colorScheme) var colorScheme
    @EnvironmentObject var theme: AppTheme
    @EnvironmentObject var sceneDelegate: SceneDelegate

    // Spec: spec/client/navigation.md#contentAccessAuthenticationExtended
    var contentAccessAuthenticationExtended: Bool

    @Environment(\.scenePhase) var scenePhase
    @State private var automaticAuthenticationAttempted = false
    @State private var canConnectViewCall = false
    @State private var lastSuccessfulUnlock: TimeInterval? = nil

    @AppStorage(DEFAULT_SHOW_LA_NOTICE) private var prefShowLANotice = false
    @AppStorage(DEFAULT_LA_NOTICE_SHOWN) private var prefLANoticeShown = false
    @AppStorage(DEFAULT_PERFORM_LA) private var prefPerformLA = false
    @AppStorage(DEFAULT_PRIVACY_PROTECT_SCREEN) private var protectScreen = false
    @AppStorage(DEFAULT_NOTIFICATION_ALERT_SHOWN) private var notificationAlertShown = false
    @State private var noticesShown = false
    @State private var noticesSheetItem: NoticesSheet? = nil
    @State private var ntfAuthorizationRequested = false
    @State private var showChooseLAMode = false
    @State private var showSetPasscode = false
    @State private var waitingForOrPassedAuth = true
    @State private var chatListUserPickerSheet: UserPickerSheet? = nil

    private let callTopPadding: CGFloat = 40

    private var accessAuthenticated: Bool {
        chatModel.contentViewAccessAuthenticated || contentAccessAuthenticationExtended
    }

    var body: some View {
        if #available(iOS 16.0, *) {
            allViews()
                .scrollContentBackground(.hidden)
        } else {
            // on iOS 15 scroll view background disabled in SceneDelegate
            allViews()
        }
    }

    func allViews() -> some View {
        ZStack {
            let showCallArea = chatModel.activeCall != nil && chatModel.activeCall?.callState != .waitCapabilities && chatModel.activeCall?.callState != .invitationAccepted
            // contentView() has to be in a single branch, so that enabling authentication doesn't trigger re-rendering and close settings.
            // i.e. with separate branches like this settings are closed: `if prefPerformLA { ... contentView() ... } else { contentView() }
            if !prefPerformLA || accessAuthenticated {
                contentView()
                    .padding(.top, showCallArea ? callTopPadding : 0)
            } else {
                lockButton()
                    .padding(.top, showCallArea ? callTopPadding : 0)
            }

            if showCallArea, let call = chatModel.activeCall {
                VStack {
                    activeCallInteractiveArea(call)
                    Spacer()
                }
            }

            if chatModel.showCallView, let call = chatModel.activeCall {
                callView(call)
            }

            if chatListUserPickerSheet == nil, let la = chatModel.laRequest {
                LocalAuthView(authRequest: la)
                    .onDisappear {
                        // this flag is separate from accessAuthenticated to show initializationView while we wait for authentication
                        waitingForOrPassedAuth = accessAuthenticated
                    }
            } else if showSetPasscode {
                SetAppPasscodeView {
                    chatModel.contentViewAccessAuthenticated = true
                    prefPerformLA = true
                    showSetPasscode = false
                    privacyLocalAuthModeDefault.set(.passcode)
                    alertManager.showAlert(laTurnedOnAlert())
                } cancel: {
                    prefPerformLA = false
                    showSetPasscode = false
                    alertManager.showAlert(laPasscodeNotSetAlert())
                }
            } else if chatModel.chatDbStatus == nil && AppChatState.shared.value != .stopped && waitingForOrPassedAuth {
                initializationView()
            }
        }
        .alert(isPresented: $alertManager.presentAlert) { alertManager.alertView! }
        .sheet(item: $activationStore.presentation) { presentation in
            NomeActivationSheetView(action: presentation.action)
                .environmentObject(activationStore)
        }
        .confirmationDialog("Nome Lock mode", isPresented: $showChooseLAMode, titleVisibility: .visible) {
            Button("System authentication") { initialEnableLA() }
            Button("Passcode entry") { showSetPasscode = true }
        }
        .onChange(of: scenePhase) { phase in
            logger.debug("scenePhase was \(String(describing: scenePhase)), now \(String(describing: phase))")
            switch (phase) {
            case .background:
                // also see .onChange(of: scenePhase) in SimpleXApp: on entering background
                // it remembers enteredBackgroundAuthenticated and sets chatModel.contentViewAccessAuthenticated to false
                automaticAuthenticationAttempted = false
                canConnectViewCall = false
            case .active:
                canConnectViewCall = !prefPerformLA || contentAccessAuthenticationExtended || unlockedRecently()
                
                // condition `!chatModel.contentViewAccessAuthenticated` is required for when authentication is enabled in settings or on initial notice
                if prefPerformLA && !chatModel.contentViewAccessAuthenticated {
                    if AppChatState.shared.value != .stopped {
                        if contentAccessAuthenticationExtended {
                            chatModel.contentViewAccessAuthenticated = true
                        } else {
                            if !automaticAuthenticationAttempted {
                                automaticAuthenticationAttempted = true
                                // authenticate if call kit call is not in progress
                                if !(CallController.useCallKit() && chatModel.showCallView && chatModel.activeCall != nil) {
                                    authenticateContentViewAccess()
                                }
                            }
                        }
                    } else {
                        // when app is stopped automatic authentication is not attempted
                        chatModel.contentViewAccessAuthenticated = contentAccessAuthenticationExtended
                    }
                }
            default:
                break
            }
        }
        .onAppear {
            reactOnDarkThemeChanges(systemInDarkThemeCurrently)
        }
        .onChange(of: colorScheme) { scheme in
            // It's needed to update UI colors when iOS wants to make screenshot after going to background,
            // so when a user changes his global theme from dark to light or back, the app will adapt to it
            reactOnDarkThemeChanges(scheme == .dark)
        }
        .onChange(of: theme.name) { _ in
            ThemeManager.adjustWindowStyle()
        }
    }

    // Spec: spec/client/navigation.md#contentView
    @ViewBuilder private func contentView() -> some View {
        if let status = chatModel.chatDbStatus, status != .ok {
            DatabaseErrorView(status: status)
        } else if !chatModel.v3DBMigration.startChat {
            MigrateToAppGroupView()
        } else if chatModel.chatDbStatus == .ok,
                  chatModel.currentUser == nil {
            OnboardingView(onboarding: .step1_SimpleXInfo)
                .onAppear { activationStore.bootstrapInstallation(hasUsableLocalProfile: false) }
        } else if let step = chatModel.onboardingStage {
            if case .onboardingComplete = step,
               chatModel.currentUser != nil {
                mainView()
                    .onAppear { activationStore.bootstrapInstallation(hasUsableLocalProfile: true) }
            } else {
                OnboardingView(onboarding: step)
                    .onAppear {
                        activationStore.bootstrapInstallation(
                            hasUsableLocalProfile: chatModel.currentUser != nil
                        )
                    }
            }
        }
    }

    // Spec: spec/client/navigation.md#callView
    @ViewBuilder private func callView(_ call: Call) -> some View {
        if CallController.useCallKit() {
            ActiveCallView(call: call, canConnectCall: Binding.constant(true))
                .id(call.callUUID)
                .onDisappear {
                    if prefPerformLA && !accessAuthenticated { authenticateContentViewAccess() }
                }
        } else {
            ActiveCallView(call: call, canConnectCall: $canConnectViewCall)
                .id(call.callUUID)
            if prefPerformLA && !accessAuthenticated {
                Rectangle()
                    .fill(colorScheme == .dark ? .black : .white)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                lockButton()
            }
        }
    }

    // Spec: spec/client/navigation.md#callBanner
    private func activeCallInteractiveArea(_ call: Call) -> some View {
        HStack {
            Text(call.contact.displayName).font(.body).foregroundColor(.white)
            Spacer()
            CallDuration(call: call)
        }
        .padding(.horizontal)
        .frame(height: callTopPadding)
        .background(Color(uiColor: UIColor(red: 47/255, green: 208/255, blue: 88/255, alpha: 1)))
        .onTapGesture {
            chatModel.activeCallViewIsCollapsed = false
        }
    }

    struct CallDuration: View {
        let call: Call
        @State var text: String = ""
        @State var timer: Timer? = nil

        var body: some View {
            Text(text).frame(minWidth: text.count <= 5 ? 52 : 77, alignment: .leading).offset(x: 4).font(.body).foregroundColor(.white)
            .onAppear {
                timer = Timer.scheduledTimer(withTimeInterval: 0.3, repeats: true) { timer in
                    if let connectedAt = call.connectedAt {
                        text = durationText(Int(Date.now.timeIntervalSince1970 - connectedAt.timeIntervalSince1970))
                    }
                }
            }
            .onDisappear {
                _ = timer?.invalidate()
            }
        }
    }

    // Spec: spec/client/navigation.md#lockButton
    private func lockButton() -> some View {
        Button(action: authenticateContentViewAccess) { Label("Unlock", systemImage: "lock") }
    }

    private func initializationView() -> some View {
        VStack {
            ProgressView().scaleEffect(2)
            Text("Opening app…")
                .padding()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity )
        .background(
            Rectangle()
                .fill(theme.colors.background)
        )
    }

    private func mainView() -> some View {
        ZStack(alignment: .top) {
            ChatListView(activeUserPickerSheet: $chatListUserPickerSheet)
                .redacted(reason: appSheetState.redactionReasons(protectScreen))
            .onAppear {
                requestNtfAuthorizationWhenReady()
                // Local Authentication notice is to be shown on next start after onboarding is complete
                if (!prefLANoticeShown && prefShowLANotice && chatModel.chats.count > 2) {
                    prefLANoticeShown = true
                    alertManager.showAlert(laNoticeAlert())
                } else if !chatModel.showCallView && CallController.shared.activeCallInvitation == nil {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                        if !noticesShown {
                            // Nome should not interrupt first launch with upstream SimpleX release notes.
                            let showWhatsNew = false
                            let showUpdatedConditions = chatModel.conditions.conditionsAction?.showNotice ?? false
                            noticesShown = showWhatsNew || showUpdatedConditions
                            if showWhatsNew || showUpdatedConditions {
                                noticesSheetItem = .whatsNew(updatedConditions: showUpdatedConditions)
                            }
                        }
                    }
                }
                prefShowLANotice = true
                connectViaUrl()
                showReRegisterTokenAlert()
            }
            .onChange(of: chatModel.appOpenUrl) { _ in connectViaUrl() }
            .onChange(of: chatModel.reRegisterTknStatus) { _ in showReRegisterTokenAlert() }
            .onChange(of: chatModel.setDeliveryReceipts) { needsDecision in
                if !needsDecision {
                    requestNtfAuthorization()
                }
            }
            .sheet(item: $noticesSheetItem) { item in
                switch item {
                case let .whatsNew(updatedConditions):
                    WhatsNewView(updatedConditions: updatedConditions)
                        .modifier(ThemedBackground())
                        .if(updatedConditions) { v in
                            v.task { await setConditionsNotified_() }
                        }
                }
            }
            if chatModel.setDeliveryReceipts {
                SetDeliveryReceiptsView()
            }
            IncomingCallView()
        }
        .onContinueUserActivity("INStartCallIntent", perform: processUserActivity)
        .onContinueUserActivity("INStartAudioCallIntent", perform: processUserActivity)
        .onContinueUserActivity("INStartVideoCallIntent", perform: processUserActivity)
        .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { userActivity in
            if let url = userActivity.webpageURL {
                logger.debug("onContinueUserActivity.NSUserActivityTypeBrowsingWeb: \(url)")
                chatModel.appOpenUrl = url
            }
        }
    }

    private func setConditionsNotified_() async {
        do {
            let conditionsId = ChatModel.shared.conditions.currentConditions.conditionsId
            try await setConditionsNotified(conditionsId: conditionsId)
        } catch let error {
            logger.error("setConditionsNotified error: \(responseError(error))")
        }
    }

    private func processUserActivity(_ activity: NSUserActivity) {
        let intent = activity.interaction?.intent
        if let intent = intent as? INStartCallIntent {
            callToRecentContact(intent.contacts, intent.callCapability == .videoCall ? .video : .audio)
        } else if let intent = intent as? INStartAudioCallIntent {
            callToRecentContact(intent.contacts, .audio)
        } else if let intent = intent as? INStartVideoCallIntent {
            callToRecentContact(intent.contacts, .video)
        }
    }

    private func callToRecentContact(_ contacts: [INPerson]?, _ mediaType: CallMediaType) {
        logger.debug("callToRecentContact")
        let contactId = contacts?.first?.personHandle?.value
        guard NomeActivationGate.allowsNetworking else {
            if let contactId {
                Task { @MainActor in
                    NomeActivationStore.shared.presentCallIntent(
                        contactId: contactId,
                        video: mediaType == .video
                    )
                }
            } else {
                _ = NomeActivationGate.require(.call)
            }
            return
        }
        if let contactId,
           let chat = chatModel.getChat(contactId),
           case let .direct(contact) = chat.chatInfo {
            let activeCall = chatModel.activeCall
            // This line works when a user clicks on a video button in CallKit UI while in call.
            // The app tries to make another call to the same contact and overwite activeCall instance making its state broken
            if let activeCall, contactId == activeCall.contact.id, mediaType == .video, !activeCall.hasVideo {
                Task {
                    await chatModel.callCommand.processCommand(.media(source: .camera, enable: true))
                }
            } else if activeCall == nil {
                logger.debug("callToRecentContact: schedule call")
                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                    CallController.shared.startCall(contact, mediaType)
                }
            }
        }
    }

    // Spec: spec/client/navigation.md#unlockedRecently
    private func unlockedRecently() -> Bool {
        if let lastSuccessfulUnlock = lastSuccessfulUnlock {
            return ProcessInfo.processInfo.systemUptime - lastSuccessfulUnlock < 2
        } else {
            return false
        }
    }

    private func authenticateContentViewAccess() {
        logger.debug("DEBUGGING: authenticateContentViewAccess")
        dismissAllSheets(animated: false) {
            logger.debug("DEBUGGING: authenticateContentViewAccess, in dismissAllSheets callback")
            chatModel.chatId = nil

            authenticate(reason: NSLocalizedString("Unlock app", comment: "authentication reason"), selfDestruct: true) { laResult in
                logger.debug("DEBUGGING: authenticate callback: \(String(describing: laResult))")
                switch (laResult) {
                case .success:
                    chatModel.contentViewAccessAuthenticated = true
                    canConnectViewCall = true
                    lastSuccessfulUnlock = ProcessInfo.processInfo.systemUptime
                case .failed:
                    chatModel.contentViewAccessAuthenticated = false
                    if privacyLocalAuthModeDefault.get() == .passcode {
                        AlertManager.shared.showAlert(laFailedAlert())
                    }
                case .unavailable:
                    prefPerformLA = false
                    canConnectViewCall = true
                    AlertManager.shared.showAlert(laUnavailableTurningOffAlert())
                }
            }
        }
    }

    func requestNtfAuthorization() {
        guard NomeActivationGate.allowsNetworking else { return }
        guard chatModel.notificationMode != .off, !ntfAuthorizationRequested else { return }
        ntfAuthorizationRequested = true
        NtfManager.shared.requestAuthorization(
            onDeny: {
                if (!notificationAlertShown) {
                    notificationAlertShown = true
                    alertManager.showAlert(notificationAlert())
                }
            },
            onAuthorized: { notificationAlertShown = false }
        )
    }

    func requestNtfAuthorizationWhenReady() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            if !chatModel.setDeliveryReceipts && noticesSheetItem == nil {
                requestNtfAuthorization()
            }
        }
    }

    func laNoticeAlert() -> Alert {
        Alert(
            title: Text("Nome Lock"),
            message: Text("To protect your information, turn on Nome Lock.\nYou will be prompted to complete authentication before this feature is enabled."),
            primaryButton: .default(Text("Turn on")) { showChooseLAMode = true },
            secondaryButton: .cancel()
         )
    }

    private func initialEnableLA () {
        privacyLocalAuthModeDefault.set(.system)
        authenticate(reason: NSLocalizedString("Enable Nome Lock", comment: "authentication reason")) { laResult in
            switch laResult {
            case .success:
                chatModel.contentViewAccessAuthenticated = true
                prefPerformLA = true
                alertManager.showAlert(laTurnedOnAlert())
            case .failed:
                prefPerformLA = false
                alertManager.showAlert(laFailedAlert())
            case .unavailable:
                prefPerformLA = false
                alertManager.showAlert(laUnavailableInstructionAlert())
            }
        }
    }

    func notificationAlert() -> Alert {
        Alert(
            title: Text("Notifications are disabled!"),
            message: Text("The app can notify you when you receive messages or contact requests - please open settings to enable."),
            primaryButton: .default(Text("Open Settings")) {
                DispatchQueue.main.async {
                    UIApplication.shared.open(URL(string: UIApplication.openSettingsURLString)!, options: [:], completionHandler: nil)
                }
            },
            secondaryButton: .cancel()
        )
    }

    // Spec: spec/client/navigation.md#connectViaUrl
    func connectViaUrl() {
        let m = ChatModel.shared
        if let url = m.appOpenUrl {
            m.appOpenUrl = nil
            guard NomeActivationGate.allowsNetworking else {
                activationStore.presentDeepLink(url)
                return
            }
            connectViaUrl_(url)
        } else if let url = m.appOpenUrlLater, AppChatState.shared.value == .active, scenePhase == .active {
            // correcting branch in case .onChange(of: scenePhase) in SimpleXApp doesn't trigger and transfer appOpenUrlLater into appOpenUrl
            m.appOpenUrlLater = nil
            guard NomeActivationGate.allowsNetworking else {
                activationStore.presentDeepLink(url)
                return
            }
            connectViaUrl_(url)
        }
    }

    func connectViaUrl_(_ url: URL) {
        dismissAllSheets() {
            var path = url.path
            if path == "/r" {
                showAlert(
                    NSLocalizedString("Relay address", comment: "alert title"),
                    message: NSLocalizedString("This is a chat relay address, it cannot be used to connect.", comment: "alert message")
                )
            } else if (path == "/contact" || path == "/invitation" || path == "/a" || path == "/c" || path == "/g" || path == "/i") {
                path.removeFirst()
                let link = url.absoluteString.replacingOccurrences(of: "///\(path)", with: "/\(path)")
                planAndConnect(
                    link,
                    theme: theme,
                    dismiss: false
                )
            } else {
                AlertManager.shared.showAlert(Alert(title: Text("Error: URL is invalid")))
            }
        }
    }

    func showReRegisterTokenAlert() {
        dismissAllSheets() {
            let m = ChatModel.shared
            guard m.notificationMode != .off else {
                m.reRegisterTknStatus = nil
                return
            }
            if let errorTknStatus = m.reRegisterTknStatus, let token = chatModel.deviceToken {
                chatModel.reRegisterTknStatus = nil
                AlertManager.shared.showAlert(Alert(
                    title: Text("Notifications error"),
                    message: Text(tokenStatusInfo(errorTknStatus, register: true)),
                    primaryButton: .default(Text("Register")) { reRegisterToken(token: token) },
                    secondaryButton: .cancel()
                ))
            }
        }
    }
}

final class AlertManager: ObservableObject {
    static let shared = AlertManager()
    @Published var presentAlert = false
    @Published var alertView: Alert?

    func showAlert(_ alert: Alert) {
        logger.debug("AlertManager.showAlert")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            self.alertView = alert
            self.presentAlert = true
        }
    }

    func showAlertMsg(title: LocalizedStringKey, message: LocalizedStringKey? = nil) {
        showAlert(mkAlert(title: title, message: message))
    }
}

func mkAlert(title: LocalizedStringKey, message: LocalizedStringKey? = nil) -> Alert {
    if let message = message {
        return Alert(title: Text(title), message: Text(message))
    } else {
        return Alert(title: Text(title))
    }
}

private enum NomeActivationPendingConfirmation: String, Identifiable {
    case deepLink
    case call
    case notification

    var id: String { rawValue }
}

struct NomeActivationSheetView: View {
    let action: NomeActivationProtectedAction

    @EnvironmentObject private var activationStore: NomeActivationStore
    @Environment(\.dismiss) private var dismiss
    @State private var inviteCode = ""
    @State private var pendingConfirmation: NomeActivationPendingConfirmation?
    @FocusState private var inviteCodeFocused: Bool

    var body: some View {
        NavigationView {
            Form {
                Section {
                    VStack(alignment: .leading, spacing: 12) {
                        Image(systemName: "person.badge.key.fill")
                            .font(.system(size: 34, weight: .semibold))
                            .foregroundColor(.green)
                        Text(activationStore.hasActiveInvitation ? "延长 Nome 使用期限" : action.title)
                            .font(.title3.weight(.semibold))
                        Text(activationStore.hasActiveInvitation
                             ? "输入新的邀请码可在现有到期日基础上续期。永久授权不会被限时邀请码缩短。"
                             : "你仍可浏览本机已有内容和调整本地设置。输入邀请码后，才能连接好友、发送消息、加入群组、传输文件或使用通话。")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 8)
                }

                Section("邀请码") {
                    TextField("输入邀请码", text: $inviteCode)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .focused($inviteCodeFocused)
                        .accessibilityIdentifier("nome.activation.inviteCode")

                    Button {
                        Task {
                            if await activationStore.redeem(inviteCode: inviteCode) {
                                finishSuccessfulActivation()
                            }
                        }
                    } label: {
                        HStack {
                            Spacer()
                            if activationStore.isRedeeming {
                                ProgressView().padding(.trailing, 6)
                            }
                            Text(activationStore.isRedeeming
                                 ? "正在兑换…"
                                 : activationStore.hasActiveInvitation ? "兑换并续期" : "激活 Nome")
                                .fontWeight(.semibold)
                            Spacer()
                        }
                    }
                    .disabled(inviteCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || activationStore.isRedeeming)
                    .accessibilityIdentifier("nome.activation.redeem")
                }

                if activationStore.effectiveAccess == .migrationRequired {
                    Section("设备迁移") {
                        Text("此邀请码已绑定其他设备。管理员重置旧设备后，可在这里完成迁移。")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                        Button("检查并迁移到此设备") {
                            Task {
                                if await activationStore.migrate() { finishSuccessfulActivation() }
                            }
                        }
                        .disabled(activationStore.isRedeeming)
                    }
                }

                if let error = activationStore.lastError {
                    Section {
                        Label(error, systemImage: "exclamationmark.triangle.fill")
                            .font(.footnote)
                            .foregroundColor(.red)
                            .accessibilityIdentifier("nome.activation.error")
                    }
                }

                Section {
                    Text("邀请码只用于启用 Nome 的联网社交功能，不会读取或上传你的聊天内容。")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
            }
            .navigationTitle(activationStore.hasActiveInvitation ? "邀请码续期" : "激活 Nome")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("稍后") { dismiss() }
                }
            }
            .onAppear {
                activationStore.clearError()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    inviteCodeFocused = true
                }
            }
        }
        .alert(item: $pendingConfirmation) { confirmation in
            switch confirmation {
            case .deepLink:
                return Alert(
                    title: Text("继续打开邀请链接？"),
                    message: Text("邀请码已验证。是否返回刚才的链接并查看连接确认信息？Nome 不会自动建立连接；取消后需重新打开链接。"),
                    primaryButton: .default(Text("继续")) { resumePendingDeepLink() },
                    secondaryButton: .cancel(Text("取消")) { discardPending(.deepLink) }
                )
            case .call:
                return Alert(
                    title: Text("继续刚才的通话？"),
                    message: Text("Nome 已激活。是否继续发起刚才选择的通话？通话不会自动开始；取消后需重新发起。"),
                    primaryButton: .default(Text("继续")) { resumePendingCall() },
                    secondaryButton: .cancel(Text("取消")) { discardPending(.call) }
                )
            case .notification:
                return Alert(
                    title: Text("继续处理通知？"),
                    message: Text("Nome 已激活。是否继续执行刚才选择的通知操作？该操作不会自动执行；取消后本次操作会被丢弃。"),
                    primaryButton: .default(Text("继续")) { resumePendingNotification() },
                    secondaryButton: .cancel(Text("取消")) { discardPending(.notification) }
                )
            }
        }
    }

    private func finishSuccessfulActivation() {
        if action == .deepLink, activationStore.pendingDeepLink != nil {
            pendingConfirmation = .deepLink
        } else if action == .call, activationStore.pendingCallIntent != nil {
            pendingConfirmation = .call
        } else if action == .notification, activationStore.pendingNotificationResponse != nil {
            pendingConfirmation = .notification
        } else {
            dismiss()
        }
    }

    private func resumePendingDeepLink() {
        let model = ChatModel.shared
        let pending = activationStore.takePendingDeepLink()
        model.appOpenUrl = nil
        model.appOpenUrlLater = nil
        dismiss()
        guard let pending else { return }
        DispatchQueue.main.async {
            model.appOpenUrl = pending
        }
    }

    private func resumePendingCall() {
        let pending = activationStore.takePendingCallIntent()
        dismiss()
        guard let pending,
              let chat = ChatModel.shared.getChat(pending.contactId),
              case let .direct(contact) = chat.chatInfo,
              ChatModel.shared.activeCall == nil
        else { return }
        DispatchQueue.main.async {
            CallController.shared.startCall(contact, pending.video ? .video : .audio)
        }
    }

    private func resumePendingNotification() {
        let pending = activationStore.takePendingNotificationResponse()
        dismiss()
        guard let pending else { return }
        DispatchQueue.main.async {
            NtfManager.shared.processNotificationResponse(pending)
        }
    }

    private func discardPending(_ confirmation: NomeActivationPendingConfirmation) {
        switch confirmation {
        case .deepLink:
            _ = activationStore.takePendingDeepLink()
        case .call:
            _ = activationStore.takePendingCallIntent()
        case .notification:
            _ = activationStore.takePendingNotificationResponse()
        }
        dismiss()
    }
}

//struct ContentView_Previews: PreviewProvider {
//    static var previews: some View {
//        ContentView(text: "Hello!")
//    }
//}
