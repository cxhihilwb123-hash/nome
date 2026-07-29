//
//  ChatListView.swift
//  SimpleX
//
//  Created by Evgeny Poberezkin on 27/01/2022.
//  Copyright © 2022 SimpleX Chat. All rights reserved.
//
// Spec: spec/client/chat-list.md

import SwiftUI
import SimpleXChat

enum UserPickerSheet: Identifiable {
    case address
    case chatPreferences
    case chatProfiles
    case currentProfile
    case useFromDesktop
    case settings

    var id: Self { self }

    var navigationTitle: LocalizedStringKey {
        switch self {
        case .address: "公开联系方式"
        case .chatPreferences: "Your preferences"
        case .chatProfiles: "Identity center"
        case .currentProfile: "Current identity"
        case .useFromDesktop: "Connect desktop"
        case .settings: "Settings"
        }
    }
}

// Spec: spec/client/chat-list.md#PresetTag
enum PresetTag: Int, Identifiable, CaseIterable, Equatable {
    case groupReports = 0
    case favorites = 1
    case contacts = 2
    case groups = 3
    case channels = 4
    case business = 5
    case notes = 6

    var id: Int { rawValue }
    
    var сollapse: Bool {
        self != .groupReports
    }
}

// Spec: spec/client/chat-list.md#ActiveFilter
enum ActiveFilter: Identifiable, Equatable {
    case presetTag(PresetTag)
    case userTag(ChatTag)
    case unread
    
    var id: String {
        switch self {
        case let .presetTag(tag): "preset \(tag.id)"
        case let .userTag(tag): "user \(tag.chatTagId)"
        case .unread: "unread"
        }
    }
}

class SaveableSettings: ObservableObject {
    @Published var servers: ServerSettings = ServerSettings(currUserServers: [], userServers: [], serverErrors: [], serverWarnings: [])
}

struct ServerSettings {
    public var currUserServers: [UserOperatorServers]
    public var userServers: [UserOperatorServers]
    public var serverErrors: [UserServersError]
    public var serverWarnings: [UserServersWarning]
}

struct UserPickerSheetView: View {
    let sheet: UserPickerSheet
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var chatModel: ChatModel
    @StateObject private var ss = SaveableSettings()

    @State private var loaded = false

    var body: some View {
        NavigationView {
            ZStack {
                if loaded, let currentUser = chatModel.currentUser {
                    switch sheet {
                    case .address:
                        UserAddressView(shareViaProfile: currentUser.addressShared)
                    case .chatPreferences:
                        PreferencesView(
                            profile: currentUser.profile,
                            preferences: currentUser.fullPreferences,
                            currentPreferences: currentUser.fullPreferences
                        )
                    case .chatProfiles:
                        UserProfilesView()
                    case .currentProfile:
                        UserProfile()
                    case .useFromDesktop:
                        ConnectDesktopView()
                    case .settings:
                        SettingsView()
                    }
                }
                Color.clear // Required for list background to be rendered during loading
            }
            .navigationTitle(sheet.navigationTitle)
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("关闭") {
                        dismiss()
                    }
                }
            }
            .modifier(ThemedBackground(grouped: true))
        }
        .overlay {
            if let la = chatModel.laRequest {
                LocalAuthView(authRequest: la)
            }
        }
        .task {
            withAnimation(
                .easeOut(duration: 0.1),
                { loaded = true }
            )
        }
        .onDisappear {
            if serversCanBeSaved(
                ss.servers.currUserServers,
                ss.servers.userServers,
                ss.servers.serverErrors
            ) {
                showAlert(
                    title: NSLocalizedString("Save servers?", comment: "alert title"),
                    buttonTitle: NSLocalizedString("Save", comment: "alert button"),
                    buttonAction: { saveServers($ss.servers.currUserServers, $ss.servers.userServers) },
                    cancelButton: true
                )
            }
        }
        .environmentObject(ss)
    }
}

// Spec: spec/client/chat-list.md#ChatListView
struct ChatListView: View {
    @EnvironmentObject var chatModel: ChatModel
    @ObservedObject private var activationStore = NomeActivationStore.shared
    @StateObject private var connectProgressManager = ConnectProgressManager.shared
    @EnvironmentObject var theme: AppTheme
    @Binding var activeUserPickerSheet: UserPickerSheet?
    @State private var showNewChatSheet = false
    @State private var newChatInitialDestination: NewChatSheetInitialDestination = .menu
    @State private var searchMode = false
    @FocusState private var searchFocussed
    @State private var searchText = ""
    @State private var searchShowingSimplexLink = false
    @State private var searchChatFilteredBySimplexLink: String? = nil
    @State private var scrollToSearchBar = false
    @State private var userPickerShown: Bool = false
    @State private var sheet: SomeSheet<AnyView>? = nil
    @StateObject private var chatTagsModel = ChatTagsModel.shared
    @State private var scrollToItemId: ChatItem.ID? = nil
    @State private var nomeHomeTab: NomeHomeTab
    private let useNomeConversationPreview: Bool

    // iOS 15 is required it to show/hide toolbar while chat is hidden/visible
    @State private var viewOnScreen = true

    @AppStorage(GROUP_DEFAULT_ONE_HAND_UI, store: groupDefaults) private var oneHandUI = true
    @AppStorage(DEFAULT_ONE_HAND_UI_CARD_SHOWN) private var oneHandUICardShown = false
    @AppStorage(DEFAULT_ADDRESS_CREATION_CARD_SHOWN) private var addressCreationCardShown = false
    @AppStorage(DEFAULT_TOOLBAR_MATERIAL) private var toolbarMaterial = ToolbarMaterial.defaultMaterial

    init(
        activeUserPickerSheet: Binding<UserPickerSheet?>,
        showNomeContactsPreview: Bool = false,
        useNomeConversationPreview: Bool = false
    ) {
        self._activeUserPickerSheet = activeUserPickerSheet
        self._nomeHomeTab = State(initialValue: showNomeContactsPreview ? .contacts : .home)
        self.useNomeConversationPreview = useNomeConversationPreview
    }
    
    // Spec: spec/client/chat-list.md#body
    var body: some View {
        if #available(iOS 16.0, *) {
            viewBody.scrollDismissesKeyboard(.immediately)
        } else {
            viewBody
        }
    }
    
    private var viewBody: some View {
        ZStack(alignment: oneHandUI ? .bottomLeading : .topLeading) {
            NavStackCompat(
                isActive: Binding(
                    get: { chatModel.chatId != nil },
                    set: { active in
                        if !active { chatModel.chatId = nil }
                    }
                ),
                destination: chatView
            ) { chatListView }
        }
        .modifier(
            Sheet(isPresented: $userPickerShown) {
                UserPicker(userPickerShown: $userPickerShown, activeSheet: $activeUserPickerSheet)
            }
        )
        .appSheet(
            item: $activeUserPickerSheet,
            onDismiss: { chatModel.laRequest = nil },
            content: { UserPickerSheetView(sheet: $0) }
        )
        .appSheet(isPresented: $showNewChatSheet) {
            NewChatSheet(initialDestination: newChatInitialDestination)
                .environment(\EnvironmentValues.refresh as! WritableKeyPath<EnvironmentValues, RefreshAction?>, nil)
        }
        .onChange(of: activeUserPickerSheet) {
            if $0 != nil {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                    userPickerShown = false
                }
            }
        }
        .environmentObject(chatTagsModel)
    }
    
    private var chatListView: some View {
        let tm = ToolbarMaterial.material(toolbarMaterial)
        return withToolbar(tm) {
            chatList
                .background(NomeShellPalette.canvas)
                .navigationBarTitleDisplayMode(.inline)
                .navigationBarHidden(true)
        }
        .onAppear {
            if #unavailable(iOS 16.0), !viewOnScreen {
                viewOnScreen = true
            }
        }
        .onDisappear {
            activeUserPickerSheet = nil
            if #unavailable(iOS 16.0) {
                viewOnScreen = false
            }
        }
        .refreshable {
            guard NomeActivationGate.require(.background) else { return }
            AlertManager.shared.showAlert(Alert(
                title: Text("Reconnect servers?"),
                message: Text("Reconnect all connected servers to force message delivery. It uses additional traffic."),
                primaryButton: .default(Text("Ok")) {
                    Task {
                        do {
                            try await reconnectAllServers()
                        } catch let error {
                            AlertManager.shared.showAlertMsg(title: "Error", message: "\(responseError(error))")
                        }
                    }
                },
                secondaryButton: .cancel()
            ))
        }
        .safeAreaInset(edge: .top) {
            if oneHandUI { Divider().background(tm) }
        }
        .safeAreaInset(edge: .bottom) {
            if showsNomeHomeTabBar {
                ZStack(alignment: .topTrailing) {
                    NomeHomeTabBar(
                        selected: nomeHomeTab,
                        onHome: { nomeHomeTab = .home },
                        onContacts: { nomeHomeTab = .contacts },
                        onSettings: { nomeHomeTab = .settings }
                    )

                    if nomeHomeTab != .settings,
                       chatModel.chatRunning == true,
                       nomeHomeTab != .home || hasConversations {
                        NomeNewConnectionButton(action: { openNewChat() })
                            .padding(.trailing, 20)
                            .offset(y: -72)
                    }
                }
            }
        }
        .sheet(item: $sheet) { sheet in
            if #available(iOS 16.0, *) {
                sheet.content.presentationDetents([.fraction(sheet.fraction)])
            } else {
                sheet.content
            }
        }
    }
    
    static var hasHomeIndicator: Bool = {
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = windowScene.windows.first {
            window.safeAreaInsets.bottom > 0
        } else { false }
    }()
    
    @ViewBuilder func withToolbar(_ material: Material, content: () -> some View) -> some View {
        if #available(iOS 16.0, *) {
            if oneHandUI {
                if showsNomeHomeTabBar || searchMode {
                    content()
                        .toolbarBackground(.hidden, for: .bottomBar)
                } else {
                    content()
                        .toolbarBackground(.hidden, for: .bottomBar)
                        .toolbar { bottomToolbar }
                }
            } else {
                content()
                    .toolbarBackground(.automatic, for: .navigationBar)
                    .toolbarBackground(material)
                    .toolbar { topToolbar }
            }
        } else {
            if oneHandUI {
                if showsNomeHomeTabBar || searchMode {
                    content()
                } else {
                    content().toolbar { bottomToolbarGroup() }
                }
            } else {
                content().toolbar { topToolbar }
            }
        }
    }
    
    @ToolbarContentBuilder var topToolbar: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) { leadingToolbarItem }
        ToolbarItem(placement: .principal) { SubsStatusIndicator() }
        ToolbarItem(placement: .topBarTrailing) { trailingToolbarItem }
    }

    @ToolbarContentBuilder var bottomToolbar: some ToolbarContent {
        let padding: Double = Self.hasHomeIndicator ? 0 : 14
        ToolbarItem(placement: .bottomBar) {
            HStack {
                leadingToolbarItem.padding(.bottom, padding)
                Spacer()
                SubsStatusIndicator().padding(.bottom, padding)
                Spacer()
                trailingToolbarItem.padding(.bottom, padding)
            }
            .contentShape(Rectangle())
            .onTapGesture { scrollToSearchBar = true }
        }
    }

    @ToolbarContentBuilder func bottomToolbarGroup() -> some ToolbarContent {
        let padding: Double = Self.hasHomeIndicator ? 0 : 14
        ToolbarItemGroup(placement: viewOnScreen ? .bottomBar : .principal) {
            leadingToolbarItem.padding(.bottom, padding)
            Spacer()
            SubsStatusIndicator().padding(.bottom, padding)
            Spacer()
            trailingToolbarItem.padding(.bottom, padding)
        }
    }

    @ViewBuilder var leadingToolbarItem: some View {
        let user = chatModel.currentUser ?? User.sampleData
        ZStack(alignment: .topTrailing) {
            ProfileImage(imageStr: user.image, size: 32, color: Color(uiColor: .quaternaryLabel))
                .padding([.top, .trailing], 3)
            let allRead = chatModel.users
                .filter { u in !u.user.activeUser && !u.user.hidden }
                .allSatisfy { u in u.unreadCount == 0 }
            if !allRead {
                unreadBadge(size: 12)
            }
        }
        .onTapGesture {
            userPickerShown = true
        }
    }
    
    @ViewBuilder var trailingToolbarItem: some View {
        switch chatModel.chatRunning {
        case .some(true): NewChatMenuButton(showNewChatSheet: $showNewChatSheet)
        case .some(false): chatStoppedIcon()
        case .none: EmptyView()
        }
    }
    
    private var showsNomeHomeTabBar: Bool {
        !searchMode
    }

    private var hasConversations: Bool {
        chatModel.chats.contains { chat in
            switch chat.chatInfo {
            case .local: return false
            case let .direct(contact): return !contact.chatDeleted && !contact.isContactCard
            case .group: return true
            case .contactRequest: return false
            case .contactConnection: return false
            case .invalidJSON: return false
            }
        }
    }

    private var canBrowseChats: Bool {
        chatModel.chatRunning == true || activationStore.effectiveAccess != .full
    }

    @ViewBuilder private var chatList: some View {
        if nomeHomeTab == .settings {
            SettingsView(embeddedInNomeTab: true)
        } else if nomeHomeTab == .contacts {
            nomeContactsTabView
        } else {
            chatListContent
        }
    }

    private var nomeContactsTabView: some View {
        let contactChats = nomeContactChats()

        return List {
            NomeHomeHeader(
                user: chatModel.currentUser ?? User.sampleData,
                onProfile: { userPickerShown = true }
            )
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 12, leading: 20, bottom: 12, trailing: 20))

            NomePageTitle(
                title: "联系人",
                subtitle: "管理联系人和连接"
            )
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 12, trailing: 20))

            ChatListSearchBar(
                searchMode: $searchMode,
                searchFocussed: $searchFocussed,
                searchText: $searchText,
                searchShowingSimplexLink: $searchShowingSimplexLink,
                searchChatFilteredBySimplexLink: $searchChatFilteredBySimplexLink,
                parentSheet: $sheet,
                searchPlaceholder: "搜索联系人",
                showsListFilters: false
            )
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 16, trailing: 20))

            if contactChats.isEmpty {
                NomeContactsEmptyCard(
                    onAddFriend: { openNewChat(.oneTimeLink) },
                    onPublicAddress: { activeUserPickerSheet = .address }
                )
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 24, trailing: 20))
            } else {
                Text("联系人")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(NomeHomePalette.textSecondary)
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 16, leading: 20, bottom: 8, trailing: 20))

                if #available(iOS 16.0, *) {
                    ForEach(Array(contactChats.enumerated()), id: \.element.viewId) { index, chat in
                        ChatListNavLink(chat: chat, parentSheet: $sheet, nomeCompactStyle: true)
                            .modifier(
                                NomeConversationRowStyle(
                                    position: NomeGroupedRowPosition(index: index, count: contactChats.count)
                                )
                            )
                            .disabled(!canBrowseChats || chatModel.deletedChats.contains(chat.chatInfo.id))
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
                    }
                } else {
                    ForEach(Array(contactChats.enumerated()), id: \.element.viewId) { index, chat in
                        ChatListNavLink(chat: chat, parentSheet: $sheet, nomeCompactStyle: true)
                            .modifier(
                                NomeConversationRowStyle(
                                    position: NomeGroupedRowPosition(index: index, count: contactChats.count)
                                )
                            )
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
                            .listRowBackground(Color.clear)
                            .disabled(!canBrowseChats || chatModel.deletedChats.contains(chat.chatInfo.id))
                    }
                }
            }
        }
        .listStyle(.plain)
        .background(NomeHomePalette.canvas.ignoresSafeArea())
    }

    private var chatListContent: some View {
        let cs = filteredChats()
        return ZStack {
            ScrollViewReader { scrollProxy in
                List {
                    NomeHomeHeader(
                        user: chatModel.currentUser ?? User.sampleData,
                        onProfile: { userPickerShown = true }
                    )
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 12, leading: 20, bottom: 12, trailing: 20))
                    .id("nomeHomeHeader")

                    NomePageTitle(
                        title: "首页",
                        subtitle: "当前本地身份的会话"
                    )
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 12, trailing: 20))

                    ChatListSearchBar(
                        searchMode: $searchMode,
                        searchFocussed: $searchFocussed,
                        searchText: $searchText,
                        searchShowingSimplexLink: $searchShowingSimplexLink,
                        searchChatFilteredBySimplexLink: $searchChatFilteredBySimplexLink,
                        parentSheet: $sheet
                    )
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
                    .frame(maxWidth: .infinity)
                    .id("searchBar")

                    if cs.isEmpty && hasConversations {
                        noChatsView()
                            .font(.system(size: 14))
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 28)
                            .foregroundColor(NomeHomePalette.textSecondary)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 16, trailing: 20))
                    }

                    if !hasConversations {
                        NomeEmptyInboxCard(
                            onAddFriend: { openNewChat(.oneTimeLink) },
                            onJoinGroup: { openNewChat(.joinGroup) },
                            onPublicAddress: { activeUserPickerSheet = .address }
                        )
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 16, leading: 20, bottom: 20, trailing: 20))
                    }
                    if !oneHandUICardShown && hasConversations {
                        OneHandUICard()
                            .padding(.vertical, 6)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                    }
                    if !cs.isEmpty {
                        Text("会话")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(NomeHomePalette.textSecondary)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                            .listRowInsets(EdgeInsets(top: 16, leading: 20, bottom: 8, trailing: 20))
                    }
                    if #available(iOS 16.0, *) {
                        ForEach(Array(cs.enumerated()), id: \.element.viewId) { index, chat in
                            ChatListNavLink(chat: chat, parentSheet: $sheet, nomeCompactStyle: true)
                                .modifier(
                                    NomeConversationRowStyle(
                                        position: NomeGroupedRowPosition(index: index, count: cs.count)
                                    )
                                )
                                .disabled(!canBrowseChats || chatModel.deletedChats.contains(chat.chatInfo.id))
                                .listRowSeparator(.hidden)
                                .listRowBackground(Color.clear)
                                .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
                        }
                    } else {
                        ForEach(Array(cs.enumerated()), id: \.element.viewId) { index, chat in
                            ChatListNavLink(chat: chat, parentSheet: $sheet, nomeCompactStyle: true)
                            .modifier(
                                NomeConversationRowStyle(
                                    position: NomeGroupedRowPosition(index: index, count: cs.count)
                                )
                            )
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
                            .listRowBackground(Color.clear)
                            .disabled(!canBrowseChats || chatModel.deletedChats.contains(chat.chatInfo.id))
                        }
                    }
                    if !addressCreationCardShown && hasConversations {
                        ConnectBannerCard()
                            .padding(.vertical, 6)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                    }
                }
                .listStyle(.plain)
                .background(NomeHomePalette.canvas.ignoresSafeArea())
                .onChange(of: chatModel.chatId) { currentChatId in
                    if let chatId = chatModel.chatToTop, currentChatId != chatId {
                        chatModel.chatToTop = nil
                        chatModel.popChat(chatId)
                    }
                    stopAudioPlayer()
                }
                .onChange(of: chatModel.currentUser?.userId) { _ in
                    stopAudioPlayer()
                }
                .onChange(of: scrollToSearchBar) { scrollToSearchBar in
                    if scrollToSearchBar {
                        Task { self.scrollToSearchBar = false }
                        withAnimation { scrollProxy.scrollTo("searchBar") }
                    }
                }
            }
        }
    }
    
    @ViewBuilder private func noChatsView() -> some View {
        if searchString().isEmpty {
            switch chatTagsModel.activeFilter {
            case .presetTag: Text("No filtered chats") // this should not happen
            case let .userTag(tag): Text("No chats in list \(tag.chatTagText)")
            case .unread:
                Button {
                    chatTagsModel.activeFilter = nil
                } label: {
                    HStack {
                        Image(systemName: "line.3.horizontal.decrease")
                        Text("No unread chats")
                    }
                }
            case .none: Text("No chats")
            }
        } else {
            Text("No chats found")
        }
    }

    
    // Spec: spec/client/chat-list.md#unreadBadge
    private func unreadBadge(size: CGFloat = 18) -> some View {
        Circle()
            .frame(width: size, height: size)
            .foregroundColor(theme.colors.primary)
    }
    
    @ViewBuilder private func chatView() -> some View {
        #if DEBUG
        if useNomeConversationPreview {
            NomeConversationPreviewHost()
        } else if let chatId = chatModel.chatId, let chat = chatModel.getChat(chatId) {
            let im = ItemsModel.shared
            ChatView(
                chat: chat,
                im: im,
                mergedItems: BoxedValue(MergedItems.create(im, [])),
                floatingButtonModel: FloatingButtonModel(im: im),
                scrollToItemId: $scrollToItemId
            )
        }
        #else
        if let chatId = chatModel.chatId, let chat = chatModel.getChat(chatId) {
            let im = ItemsModel.shared
            ChatView(
                chat: chat,
                im: im,
                mergedItems: BoxedValue(MergedItems.create(im, [])),
                floatingButtonModel: FloatingButtonModel(im: im),
                scrollToItemId: $scrollToItemId
            )
        }
        #endif
    }
    
    // Spec: spec/client/chat-list.md#stopAudioPlayer
    func stopAudioPlayer() {
        VoiceItemState.smallView.values.forEach { $0.audioPlayer?.stop() }
        VoiceItemState.smallView = [:]
    }
    
    // Spec: spec/client/chat-list.md#filteredChats
    private func filteredChats() -> [Chat] {
        if let linkChatId = searchChatFilteredBySimplexLink {
            return chatModel.chats.filter { $0.id == linkChatId }
        } else {
            let s = searchString()
            return s == ""
            ? chatModel.chats.filter { chat in
                !chat.chatInfo.chatDeleted && !chat.chatInfo.contactCard && filtered(chat)
            }
            : chatModel.chats.filter { chat in
                let cInfo = chat.chatInfo
                return switch cInfo {
                case let .direct(contact):
                    !contact.chatDeleted && !chat.chatInfo.contactCard && (
                        ( viewNameContains(cInfo, s) ||
                          contact.profile.displayName.localizedLowercase.contains(s) ||
                          contact.fullName.localizedLowercase.contains(s)
                        )
                    )
                case .group: viewNameContains(cInfo, s)
                case .local: viewNameContains(cInfo, s)
                case .contactRequest: viewNameContains(cInfo, s)
                case let .contactConnection(conn): conn.localAlias.localizedLowercase.contains(s)
                case .invalidJSON: false
                }
            }
        }
        
        func filtered(_ chat: Chat) -> Bool {
            switch chatTagsModel.activeFilter {
            case let .presetTag(tag): presetTagMatchesChat(tag, chat.chatInfo, chat.chatStats)
            case let .userTag(tag): chat.chatInfo.chatTags?.contains(tag.chatTagId) == true
            case .unread: chat.unreadTag
            case .none: true
            }
        }
        
        func viewNameContains(_ cInfo: ChatInfo, _ s: String) -> Bool {
            cInfo.chatViewName.localizedLowercase.contains(s)
        }
    }
    
    // Spec: spec/client/chat-list.md#searchString
    func searchString() -> String {
        searchShowingSimplexLink ? "" : searchText.trimmingCharacters(in: .whitespaces).localizedLowercase
    }

    private func openNewChat() {
        openNewChat(.menu)
    }

    private func openNewChat(_ initialDestination: NewChatSheetInitialDestination) {
        guard NomeActivationGate.require(.connect) else { return }
        guard chatModel.currentUser != nil else {
            onboardingStageDefault.set(.step1_SimpleXInfo)
            chatModel.onboardingStage = .step1_SimpleXInfo
            AlertManager.shared.showAlertMsg(
                title: "Profile not ready",
                message: "Nome is still setting up your local profile. Finish profile setup before adding friends or joining groups."
            )
            return
        }
        guard chatModel.chatRunning == true else {
            AlertManager.shared.showAlertMsg(
                title: "Chat service not ready",
                message: "Nome is still starting the local chat service. Try again in a moment, or reopen the app."
            )
            return
        }
        ConnectProgressManager.shared.cancelConnectProgress()
        newChatInitialDestination = initialDestination
        showNewChatSheet = true
    }

    private func activateSearchOrConnect() {
        if chatModel.chats.isEmpty {
            openNewChat()
        } else {
            scrollToSearchBar = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                searchFocussed = true
            }
        }
    }

    private func nomeContactChats() -> [Chat] {
        let query = searchString()
        return chatModel.chats.filter { chat in
            if chat.chatInfo.chatDeleted || chat.chatInfo.contactCard {
                return false
            }
            switch chat.chatInfo {
            case .local:
                return false
            case let .direct(contact):
                return !contact.chatDeleted && !contact.isContactCard
            case .group:
                return true
            case .contactRequest:
                return true
            case .contactConnection:
                return true
            case .invalidJSON:
                return false
            }
        }.filter { chat in
            query.isEmpty || chat.chatInfo.chatViewName.localizedLowercase.contains(query)
        }
    }
}

private enum NomeHomePalette {
    static let navy = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 247.0 / 255.0, green: 249.0 / 255.0, blue: 252.0 / 255.0, alpha: 1)
            : UIColor(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0, alpha: 1)
    })
    static let brandNavy = Color(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0)
    static let action = Color(red: 10.0 / 255.0, green: 135.0 / 255.0, blue: 77.0 / 255.0)
    static let green = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 113.0 / 255.0, green: 220.0 / 255.0, blue: 159.0 / 255.0, alpha: 1)
            : UIColor(red: 10.0 / 255.0, green: 135.0 / 255.0, blue: 77.0 / 255.0, alpha: 1)
    })
    static let canvas = NomeShellPalette.canvas
    static let surface = NomeShellPalette.surface
    static let surfaceContainer = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 23.0 / 255.0, green: 39.0 / 255.0, blue: 60.0 / 255.0, alpha: 1)
            : UIColor(red: 248.0 / 255.0, green: 250.0 / 255.0, blue: 252.0 / 255.0, alpha: 1)
    })
    static let input = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 26.0 / 255.0, green: 43.0 / 255.0, blue: 65.0 / 255.0, alpha: 1)
            : UIColor.white
    })
    static let subtleSurface = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 18.0 / 255.0, green: 31.0 / 255.0, blue: 50.0 / 255.0, alpha: 1)
            : UIColor(red: 245.0 / 255.0, green: 247.0 / 255.0, blue: 250.0 / 255.0, alpha: 1)
    })
    static let border = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 48.0 / 255.0, green: 68.0 / 255.0, blue: 92.0 / 255.0, alpha: 1)
            : UIColor(red: 208.0 / 255.0, green: 213.0 / 255.0, blue: 221.0 / 255.0, alpha: 1)
    })
    static let divider = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 48.0 / 255.0, green: 68.0 / 255.0, blue: 92.0 / 255.0, alpha: 1)
            : UIColor(red: 234.0 / 255.0, green: 236.0 / 255.0, blue: 240.0 / 255.0, alpha: 1)
    })
    static let textSecondary = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 181.0 / 255.0, green: 192.0 / 255.0, blue: 207.0 / 255.0, alpha: 1)
            : UIColor(red: 102.0 / 255.0, green: 112.0 / 255.0, blue: 133.0 / 255.0, alpha: 1)
    })
}

private enum NomeHomeTab {
    case home
    case contacts
    case settings
}

private enum NomeGroupedRowPosition: Equatable {
    case single
    case first
    case middle
    case last

    init(index: Int, count: Int) {
        if count <= 1 {
            self = .single
        } else if index == 0 {
            self = .first
        } else if index == count - 1 {
            self = .last
        } else {
            self = .middle
        }
    }

    var corners: UIRectCorner {
        switch self {
        case .single:
            return .allCorners
        case .first:
            return [.topLeft, .topRight]
        case .middle:
            return []
        case .last:
            return [.bottomLeft, .bottomRight]
        }
    }

    var showsDivider: Bool {
        self == .first || self == .middle
    }
}

private struct NomeGroupedRowShape: Shape {
    let corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        Path(
            UIBezierPath(
                roundedRect: rect,
                byRoundingCorners: corners,
                cornerRadii: CGSize(width: 16, height: 16)
            ).cgPath
        )
    }
}

private struct NomeConversationRowStyle: ViewModifier {
    let position: NomeGroupedRowPosition

    func body(content: Content) -> some View {
        let shape = NomeGroupedRowShape(corners: position.corners)
        content
            .background(
                shape.fill(NomeHomePalette.surfaceContainer)
            )
            .overlay(alignment: .bottom) {
                if position.showsDivider {
                    Rectangle()
                        .fill(NomeHomePalette.divider)
                        .frame(height: 1)
                        .padding(.leading, 68)
                        .padding(.trailing, 12)
                }
            }
            .contentShape(shape)
    }
}

private struct NomeHomeHeader: View {
    let user: User
    let onProfile: () -> Void

    var body: some View {
        HStack {
            NomeBrandLockup()
                .frame(width: 104, alignment: .leading)
                .accessibilityHidden(true)

            Spacer(minLength: 12)

            Button(action: onProfile) {
                ProfileImage(imageStr: user.image, size: 38, color: NomeHomePalette.surfaceContainer)
            }
            .frame(width: 48, height: 48)
            .buttonStyle(.plain)
            .accessibilityLabel(Text("当前身份"))
        }
        .frame(minHeight: 56)
    }
}

private struct NomeBrandLockup: View {
    var body: some View {
        Image("nome_header_logo")
            .resizable()
            .scaledToFit()
    }
}

private struct NomePageTitle: View {
    let title: LocalizedStringKey
    let subtitle: LocalizedStringKey

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(NomeHomePalette.navy)
                .accessibilityAddTraits(.isHeader)
            Text(subtitle)
                .font(.system(size: 16, weight: .regular))
                .foregroundColor(NomeHomePalette.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct NomeNewConnectionButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "plus")
                .font(.system(size: 25, weight: .medium))
                .foregroundColor(.white)
                .frame(width: 56, height: 56)
                .background(Circle().fill(NomeHomePalette.action))
                .shadow(color: Color.black.opacity(0.20), radius: 8, y: 4)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("新建连接"))
    }
}

private struct NomeEmptyInboxCard: View {
    let onAddFriend: () -> Void
    let onJoinGroup: () -> Void
    let onPublicAddress: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: "tray")
                .font(.system(size: 22, weight: .medium))
                .foregroundColor(NomeHomePalette.textSecondary)

            Text("开始第一段私密对话")
                .font(.title3.weight(.semibold))
                .foregroundColor(NomeHomePalette.navy)

            Text("发送一次性邀请，或扫描对方二维码建立连接。无需手机号，也无需公开用户名。")
                .font(.body)
                .foregroundColor(NomeHomePalette.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            Button(action: onAddFriend) {
                Label("添加朋友", systemImage: "link.badge.plus")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(NomePrimaryButtonStyle())

            HStack(spacing: 10) {
                Button(action: onJoinGroup) {
                    Label("加入群组", systemImage: "person.2")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(NomeSecondaryButtonStyle())

                Button(action: onPublicAddress) {
                    Label("公开地址", systemImage: "globe")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(NomeSecondaryButtonStyle())
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(NomeHomePalette.subtleSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(NomeHomePalette.textSecondary.opacity(0.42), lineWidth: 1)
        )
    }
}

private struct NomePrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .foregroundColor(.white)
            .padding(.vertical, 11)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(NomeHomePalette.action)
                    .opacity(configuration.isPressed ? 0.82 : 1)
            )
    }
}

private struct NomeSecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.subheadline.weight(.semibold))
            .foregroundColor(NomeHomePalette.navy)
            .padding(.vertical, 11)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(configuration.isPressed ? NomeHomePalette.navy.opacity(0.12) : NomeHomePalette.input)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(NomeHomePalette.border, lineWidth: 1)
            )
    }
}

private struct NomeHomeTabBar: View {
    let selected: NomeHomeTab
    let onHome: () -> Void
    let onContacts: () -> Void
    let onSettings: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(NomeHomePalette.divider)
                .frame(height: 1)

            HStack(spacing: 0) {
                tabButton(icon: selected == .home ? "house.fill" : "house", title: "首页", active: selected == .home, action: onHome)
                tabButton(icon: selected == .contacts ? "person.2.fill" : "person.2", title: "联系人", active: selected == .contacts, action: onContacts)
                tabButton(icon: selected == .settings ? "gearshape.fill" : "gearshape", title: "设置", active: selected == .settings, action: onSettings)
            }
            .padding(.horizontal, 16)
            .frame(height: 64)
            .background(NomeHomePalette.surface)
        }
        .background(NomeHomePalette.surface)
    }

    private func tabButton(icon: String, title: LocalizedStringKey, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            ZStack(alignment: .top) {
                if active {
                    Capsule()
                        .fill(NomeHomePalette.green)
                        .frame(width: 32, height: 3)
                }

                VStack(spacing: 2) {
                    Image(systemName: icon)
                        .font(.system(size: 24, weight: active ? .semibold : .regular))
                        .frame(height: 24)
                    Text(title)
                        .font(.system(size: 13, weight: .regular))
                }
                .frame(maxHeight: .infinity)
            }
            .foregroundColor(active ? NomeHomePalette.green : NomeHomePalette.textSecondary)
            .frame(maxWidth: .infinity)
            .frame(height: 64)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

private struct NomeContactsEmptyCard: View {
    let onAddFriend: () -> Void
    let onPublicAddress: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Image(systemName: "person.2")
                .font(.system(size: 22, weight: .medium))
                .foregroundColor(NomeHomePalette.textSecondary)

            Text("还没有联系人")
                .font(.title3.weight(.semibold))
                .foregroundColor(NomeHomePalette.navy)

            Text("先用一次性链接连接一个朋友。联系人、群组和待处理请求都会显示在这里。")
                .font(.body)
                .foregroundColor(NomeHomePalette.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 10) {
                Button(action: onAddFriend) {
                    Label("添加朋友", systemImage: "plus")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(NomePrimaryButtonStyle())

                Button(action: onPublicAddress) {
                    Label("公开地址", systemImage: "globe")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(NomeSecondaryButtonStyle())
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(NomeHomePalette.subtleSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(NomeHomePalette.textSecondary.opacity(0.42), lineWidth: 1)
        )
    }
}

struct SubsStatusIndicator: View {
    @State private var subs: SMPServerSubs = SMPServerSubs.newSMPServerSubs
    @State private var hasSess: Bool = false
    @State private var task: Task<Void, Never>?
    @State private var showServersSummary = false

    @AppStorage(DEFAULT_SHOW_SUBSCRIPTION_PERCENTAGE) private var showSubscriptionPercentage = false

    var body: some View {
        Button {
            showServersSummary = true
        } label: {
            HStack(spacing: 4) {
                Text("Chats").foregroundStyle(Color.primary).fixedSize().font(.headline)
                SubscriptionStatusIndicatorView(subs: subs, hasSess: hasSess)
                if showSubscriptionPercentage {
                    SubscriptionStatusPercentageView(subs: subs, hasSess: hasSess)
                }
            }
        }
        .disabled(ChatModel.shared.chatRunning != true)
        .onAppear {
            startTask()
        }
        .onDisappear {
            stopTask()
        }
        .appSheet(isPresented: $showServersSummary) {
            ServersSummaryView()
                .environment(\EnvironmentValues.refresh as! WritableKeyPath<EnvironmentValues, RefreshAction?>, nil)
        }
    }

    private func startTask() {
        task = Task {
            while !Task.isCancelled {
                if AppChatState.shared.value == .active, ChatModel.shared.chatRunning == true {
                    do {
                        let (subs, hasSess) = try await getAgentSubsTotal()
                        await MainActor.run {
                            self.subs = subs
                            self.hasSess = hasSess
                        }
                    } catch let error {
                        logger.error("getSubsTotal error: \(responseError(error))")
                    }
                }
                try? await Task.sleep(nanoseconds: 1_000_000_000) // Sleep for 1 second
            }
        }
    }

    func stopTask() {
        task?.cancel()
        task = nil
    }
}

// Spec: spec/client/chat-list.md#ChatListSearchBar
struct ChatListSearchBar: View {
    @EnvironmentObject var theme: AppTheme
    @EnvironmentObject var chatTagsModel: ChatTagsModel
    @StateObject private var connectProgressManager = ConnectProgressManager.shared
    @Binding var searchMode: Bool
    @FocusState.Binding var searchFocussed: Bool
    @Binding var searchText: String
    @Binding var searchShowingSimplexLink: Bool
    @Binding var searchChatFilteredBySimplexLink: String?
    @Binding var parentSheet: SomeSheet<AnyView>?
    var searchPlaceholder: LocalizedStringKey = "搜索会话"
    var showsListFilters = true
    @State private var ignoreSearchTextChange = false

    var body: some View {
        VStack(spacing: showsListFilters ? 12 : 0) {
            HStack(spacing: 12) {
                HStack(spacing: 9) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 20, weight: .medium))
                    TextField(
                        "",
                        text: $searchText,
                        prompt: Text(searchPlaceholder).foregroundColor(NomeHomePalette.textSecondary)
                    )
                        .font(.system(size: 16, weight: .regular))
                        .foregroundColor(searchShowingSimplexLink ? theme.colors.secondary : theme.colors.onBackground)
                        .disabled(searchShowingSimplexLink)
                        .focused($searchFocussed)
                        .frame(maxWidth: .infinity)
                    if connectProgressManager.showConnectProgress != nil {
                        ProgressView()
                    }
                    if !searchText.isEmpty {
                        Image(systemName: "xmark.circle.fill")
                            .onTapGesture {
                                searchText = ""
                            }
                    }
                }
                .padding(.horizontal, 14)
                .frame(height: 48)
                .foregroundColor(NomeHomePalette.textSecondary)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(NomeHomePalette.input)
                )
                .overlay {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(NomeHomePalette.divider, lineWidth: 1)
                }

                if searchFocussed {
                    Button {
                        searchText = ""
                        searchFocussed = false
                    } label: {
                        Text("Cancel")
                            .frame(minWidth: 44, minHeight: 44)
                    }
                    .buttonStyle(.plain)
                    .foregroundColor(theme.colors.primary)
                }
            }

            if showsListFilters {
                ScrollView([.horizontal], showsIndicators: false) {
                    TagsView(parentSheet: $parentSheet, searchText: $searchText)
                        .padding(4)
                }
                .frame(minHeight: 48)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(NomeHomePalette.input)
                )
                .overlay {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(NomeHomePalette.divider, lineWidth: 1)
                }
            }
        }
        .onChange(of: searchFocussed) { sf in
            withAnimation { searchMode = sf }
        }
        .onChange(of: searchText) { t in
            if ignoreSearchTextChange {
                ignoreSearchTextChange = false
            } else {
                switch strConnectTarget(t.trimmingCharacters(in: .whitespaces)) {
                case let .link(text, _, linkText):
                    searchFocussed = false
                    ignoreSearchTextChange = true
                    searchText = linkText
                    searchShowingSimplexLink = true
                    searchChatFilteredBySimplexLink = nil
                    connect(text)
                case let .name(nameInfo):
                    showUnsupportedNameAlert(nameInfo)
                case .none:
                    if t != "" {
                        searchFocussed = true
                    } else {
                        ConnectProgressManager.shared.cancelConnectProgress()
                    }
                    searchShowingSimplexLink = false
                    searchChatFilteredBySimplexLink = nil
                }
            }
        }
        .onChange(of: chatTagsModel.activeFilter) { _ in
            searchText = ""
        }
    }

    private func connect(_ link: String) {
        planAndConnect(
            link,
            theme: theme,
            dismiss: false,
            cleanup: {
                searchText = ""
                searchFocussed = false
            },
            filterKnownContact: { searchChatFilteredBySimplexLink = $0.id },
            filterKnownGroup: { searchChatFilteredBySimplexLink = $0.id }
        )
    }
}

struct TagsView: View {
    @EnvironmentObject var chatTagsModel: ChatTagsModel
    @EnvironmentObject var chatModel: ChatModel
    @EnvironmentObject var theme: AppTheme
    @Binding var parentSheet: SomeSheet<AnyView>?
    @Binding var searchText: String

    var body: some View {
        HStack(spacing: 4) {
            tagsView()
        }
    }
    
    @ViewBuilder private func tagsView() -> some View {
        allChatsFilterView()

        let selectedTag: ChatTag? = if case let .userTag(tag) = chatTagsModel.activeFilter {
            tag
        } else {
            nil
        }
        ForEach(chatTagsModel.userTags, id: \.id) { tag in
            let current = tag == selectedTag
            let color: Color = current ? .accentColor : .secondary
            ZStack {
                HStack(spacing: 4) {
                    if let emoji = tag.chatTagEmoji {
                        Text(emoji)
                    } else {
                        Image(systemName: current ? "tag.fill" : "tag")
                            .foregroundColor(color)
                    }
                    ZStack {
                        let badge = Text(verbatim: (chatTagsModel.unreadTags[tag.chatTagId] ?? 0) > 0 ? " ●" : "").font(.footnote)
                        (Text(tag.chatTagText).fontWeight(.semibold) + badge).foregroundColor(.clear)
                        Text(tag.chatTagText).fontWeight(current ? .semibold : .regular).foregroundColor(color) + badge.foregroundColor(theme.colors.primary)
                    }
                }
                .onTapGesture {
                    setActiveFilter(filter: .userTag(tag))
                }
                .onLongPressGesture {
                    let screenHeight = UIScreen.main.bounds.height
                    let reservedSpace: Double = 4 * 44 // 2 for padding, 1 for "Create list" and another for extra tag
                    let tagsSpace = Double(max(chatTagsModel.userTags.count, 3)) * 44
                    let fraction = min((reservedSpace + tagsSpace) / screenHeight, 0.62)

                    parentSheet = SomeSheet(
                        content: {
                            AnyView(
                                NavigationView {
                                    TagListView(chat: nil)
                                        .modifier(ThemedBackground(grouped: true))
                                }
                            )
                        },
                        id: "tag list",
                        fraction: fraction
                    )
                }
            }
        }
        
        Button {
            parentSheet = SomeSheet(
                content: {
                    AnyView(
                        NavigationView {
                            TagListEditor()
                        }
                    )
                },
                id: "tag create"
            )
        } label: {
            if chatTagsModel.userTags.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "plus")
                    Text("添加列表")
                }
            } else {
                Image(systemName: "plus")
            }
        }
        .font(.system(size: 14, weight: .medium))
        .foregroundColor(NomeHomePalette.action)
        .padding(.horizontal, 10)
        .frame(minHeight: 40)
    }

    private func allChatsFilterView() -> some View {
        let selected = chatTagsModel.activeFilter == nil
        return HStack(spacing: 4) {
            Text("全部")
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(selected ? NomeHomePalette.green : NomeHomePalette.textSecondary)
                .padding(.horizontal, 12)
                .frame(minHeight: 40)
                .background(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .fill(selected ? NomeHomePalette.green.opacity(0.10) : Color.clear)
                )
                .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .onTapGesture {
                    chatTagsModel.activeFilter = nil
                    searchText = ""
                }
                .accessibilityAddTraits(selected ? .isSelected : [])

            Text("\(chatModel.chats.filter { !$0.chatInfo.chatDeleted }.count)")
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(NomeHomePalette.navy)
                .padding(.horizontal, 8)
                .frame(minHeight: 40)
        }
    }

    @ViewBuilder private func expandedTagFilterView(_ tag: PresetTag) -> some View {
        let selectedPresetTag: PresetTag? = if case let .presetTag(tag) = chatTagsModel.activeFilter {
            tag
        } else {
            nil
        }
        let active = tag == selectedPresetTag
        let (icon, menuIcon, text) = presetTagLabel(tag: tag, active: active)
        let color: Color = active ? .accentColor : .secondary

        HStack(spacing: 4) {
            Image(systemName: menuIcon ?? icon)
                .foregroundColor(color)
            ZStack {
                Text(text).fontWeight(.semibold).foregroundColor(.clear)
                Text(text).fontWeight(active ? .semibold : .regular).foregroundColor(color)
            }
        }
        .onTapGesture {
            setActiveFilter(filter: .presetTag(tag))
        }
    }

    private func expandedPresetTagsFiltersView() -> some View {
        ForEach(PresetTag.allCases, id: \.id) { tag in
            if (chatTagsModel.presetTags[tag] ?? 0) > 0 {
                expandedTagFilterView(tag)
            }
        }
    }
    
    @ViewBuilder private func collapsedTagsFilterView() -> some View {
        let selectedPresetTag: PresetTag? = if case let .presetTag(tag) = chatTagsModel.activeFilter {
            tag
        } else {
            nil
        }
        Menu {
            if chatTagsModel.activeFilter != nil || !searchText.isEmpty {
                Button {
                    chatTagsModel.activeFilter = nil
                    searchText = ""
                } label: {
                    HStack {
                        Image(systemName: "list.bullet")
                        Text("All")
                    }
                }
            }
            ForEach(PresetTag.allCases, id: \.id) { tag in
                if (chatTagsModel.presetTags[tag] ?? 0) > 0 && tag.сollapse {
                    Button {
                        setActiveFilter(filter: .presetTag(tag))
                    } label: {
                        let (icon, _, text) = presetTagLabel(tag: tag, active: tag == selectedPresetTag)
                        HStack {
                            Image(systemName: icon)
                            Text(text)
                        }
                    }
                }
            }
        } label: {
            if let tag = selectedPresetTag, tag.сollapse {
                let (icon, menuIcon, _) = presetTagLabel(tag: tag, active: true)
                Image(systemName: menuIcon ?? icon)
                    .foregroundColor(.accentColor)
            } else {
                Image(systemName: "list.bullet")
                    .foregroundColor(.secondary)
            }
        }
        .frame(minWidth: 28)
    }
    
    private func presetTagLabel(tag: PresetTag, active: Bool) -> (item: String, menu: String?, label: LocalizedStringKey) {
        switch tag {
        case .groupReports: (item: active ? "flag.fill" : "flag", menu: nil, label: "Reports")
        case .favorites: (item: active ? "star.fill" : "star", menu: nil, label: "Favorites")
        case .contacts: (item: active ? "person.fill" : "person", menu: nil, label: "Contacts")
        case .groups: (item: active ? "person.2.fill" : "person.2", menu: nil, label: "Groups")
        case .channels: (item: active ? "antenna.radiowaves.left.and.right.circle.fill" : "antenna.radiowaves.left.and.right", menu: "antenna.radiowaves.left.and.right", label: "Channels")
        case .business: (item: active ? "briefcase.fill" : "briefcase", menu: nil, label: "Businesses")
        case .notes: (item: active ? "folder.fill" : "folder", menu: nil, label: "Notes")
        }
    }

    // Spec: spec/client/chat-list.md#setActiveFilter
    private func setActiveFilter(filter: ActiveFilter) {
        if filter != chatTagsModel.activeFilter {
            chatTagsModel.activeFilter = filter
        } else {
            chatTagsModel.activeFilter = nil
        }
    }
}

func chatStoppedIcon() -> some View {
    Button {
        AlertManager.shared.showAlertMsg(
            title: "Chat is stopped",
            message: "You can start chat via app Settings / Database or by restarting the app"
        )
    } label: {
        Image(systemName: "exclamationmark.octagon.fill").foregroundColor(.red)
    }
}

// Spec: spec/client/chat-list.md#presetTagMatchesChat
func presetTagMatchesChat(_ tag: PresetTag, _ chatInfo: ChatInfo, _ chatStats: ChatStats) -> Bool {
    switch tag {
    case .groupReports:
        chatStats.reportsCount > 0
    case .favorites:
        chatInfo.chatSettings?.favorite == true
    case .contacts:
        switch chatInfo {
        case let .direct(contact): !contact.isContactCard && !contact.chatDeleted
        case .contactRequest: true
        case .contactConnection: true
        case let .group(groupInfo, _): groupInfo.businessChat?.chatType == .customer
        default: false
        }
    case .groups:
        switch chatInfo {
        case let .group(groupInfo, _): groupInfo.businessChat == nil && !groupInfo.isChannel
        default: false
        }
    case .channels:
        switch chatInfo {
        case let .group(groupInfo, _): groupInfo.isChannel
        default: false
        }
    case .business:
        chatInfo.groupInfo?.businessChat?.chatType == .business
    case .notes:
        switch chatInfo {
        case .local: true
        default: false
        }
    }
}

struct ChatListView_Previews: PreviewProvider {
    @State static var userPickerSheet: UserPickerSheet? = .none

    static var previews: some View {
        let chatModel = ChatModel()
        chatModel.updateChats([
            ChatData(
                chatInfo: ChatInfo.sampleData.direct,
                chatItems: [ChatItem.getSample(1, .directSnd, .now, "hello")]
            ),
            ChatData(
                chatInfo: ChatInfo.sampleData.group,
                chatItems: [ChatItem.getSample(1, .directSnd, .now, "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.")]
            ),
            ChatData(
                chatInfo: ChatInfo.sampleData.contactRequest,
                chatItems: []
            )

        ])
        return Group {
            ChatListView(activeUserPickerSheet: $userPickerSheet)
                .environmentObject(chatModel)
            ChatListView(activeUserPickerSheet: $userPickerSheet)
                .environmentObject(ChatModel())
        }
    }
}

#if DEBUG
struct NomeChatListPreviewHost: View {
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var chatModel: ChatModel
    @State private var userPickerSheet: UserPickerSheet? = nil
    private let showContacts: Bool

    init(showContacts: Bool = false) {
        self.showContacts = showContacts
        UserDefaults.standard.set(true, forKey: DEFAULT_ONE_HAND_UI_CARD_SHOWN)
        UserDefaults.standard.set(true, forKey: DEFAULT_ADDRESS_CREATION_CARD_SHOWN)
    }

    var body: some View {
        NavigationView {
            ChatListView(
                activeUserPickerSheet: $userPickerSheet,
                showNomeContactsPreview: showContacts,
                useNomeConversationPreview: true
            )
        }
        .navigationViewStyle(.stack)
        .onAppear {
            reactOnDarkThemeChanges(colorScheme == .dark)
            chatModel.currentUser = User.sampleData
            chatModel.chatRunning = true
            chatModel.chatInitialized = true
            chatModel.onboardingStage = nil
            chatModel.updateChats(Self.previewChatData)
        }
        .onChange(of: colorScheme) { scheme in
            reactOnDarkThemeChanges(scheme == .dark)
        }
    }

    private static var previewChatData: [ChatData] {
        [
            ChatData(
                chatInfo: directChat(
                    id: 11,
                    displayName: "林晓",
                    fullName: "Lin Xiao",
                    settings: ChatSettings(enableNtfs: .all, sendRcpts: nil, favorite: true)
                ),
                chatItems: [ChatItem.getSample(1, .directRcv, .now.addingTimeInterval(-90), "刚刚确认了安全码，可以继续聊。")],
                chatStats: ChatStats(unreadCount: 2, minUnreadItemId: 1)
            ),
            ChatData(
                chatInfo: groupChat(
                    id: 21,
                    displayName: "产品小组",
                    fullName: "Nome Product"
                ),
                chatItems: [ChatItem.getSample(2, .groupRcv(groupMember: GroupMember.sampleData), .now.addingTimeInterval(-660), "今天把公开联系方式和加好友路径再过一遍。")],
                chatStats: ChatStats(unreadCount: 5, unreadMentions: 1, reportsCount: 1, minUnreadItemId: 2)
            ),
            ChatData(
                chatInfo: directChat(
                    id: 12,
                    displayName: "安然",
                    fullName: "An Ran",
                    settings: ChatSettings(enableNtfs: .none, sendRcpts: nil, favorite: false)
                ),
                chatItems: [ChatItem.getSample(3, .directSnd, .now.addingTimeInterval(-3600), "我晚点把邀请链接发你。", .sndSent(sndProgress: .complete))],
                chatStats: ChatStats()
            )
        ]
    }

    private static func directChat(
        id: Int64,
        displayName: String,
        fullName: String,
        settings: ChatSettings
    ) -> ChatInfo {
        var contact = Contact.sampleData
        contact.contactId = id
        contact.profile.displayName = displayName
        contact.profile.fullName = fullName
        contact.chatSettings = settings
        return .direct(contact: contact)
    }

    private static func groupChat(
        id: Int64,
        displayName: String,
        fullName: String
    ) -> ChatInfo {
        var groupInfo = GroupInfo.sampleData
        groupInfo.groupId = id
        groupInfo.groupProfile.displayName = displayName
        groupInfo.groupProfile.fullName = fullName
        groupInfo.chatSettings = ChatSettings(enableNtfs: .mentions, sendRcpts: nil, favorite: false)
        return .group(groupInfo: groupInfo, groupChatScope: nil)
    }
}
#endif
