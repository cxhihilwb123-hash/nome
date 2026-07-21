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
                .background(theme.colors.background)
                .navigationBarTitleDisplayMode(.inline)
                .navigationBarHidden(true)
        }
        .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
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
                NomeHomeTabBar(
                    selected: nomeHomeTab,
                    onHome: { nomeHomeTab = .home },
                    onContacts: { nomeHomeTab = .contacts },
                    onSettings: { nomeHomeTab = .settings }
                )
                .background(.ultraThinMaterial)
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
                if showsNomeHomeTabBar {
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
                if showsNomeHomeTabBar {
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
        ToolbarItem(placement: .principal) { if !shouldShowOnboarding { SubsStatusIndicator() } }
        ToolbarItem(placement: .topBarTrailing) { trailingToolbarItem }
    }

    @ToolbarContentBuilder var bottomToolbar: some ToolbarContent {
        let padding: Double = Self.hasHomeIndicator ? 0 : 14
        ToolbarItem(placement: .bottomBar) {
            HStack {
                leadingToolbarItem.padding(.bottom, padding)
                Spacer()
                if !shouldShowOnboarding {
                    SubsStatusIndicator().padding(.bottom, padding)
                    Spacer()
                }
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
            if !shouldShowOnboarding {
                SubsStatusIndicator().padding(.bottom, padding)
                Spacer()
            }
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
    
    private var shouldShowOnboarding: Bool {
        !addressCreationCardShown && !chatModel.chats.isEmpty && !hasConversations
    }

    private var showsNomeHomeTabBar: Bool {
        !shouldShowOnboarding && !searchMode
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

    private var shouldInvertChatList: Bool {
        oneHandUI && !showsNomeHomeTabBar && !chatModel.chats.isEmpty
    }

    @ViewBuilder private var chatList: some View {
        if nomeHomeTab == .settings {
            SettingsView(embeddedInNomeTab: true)
                .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
        } else if nomeHomeTab == .contacts {
            nomeContactsTabView
                .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
        } else if shouldShowOnboarding {
            ConnectOnboardingView()
                .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                .modifier(ThemedBackground())
        } else {
            chatListContent
        }
    }

    private var nomeContactsTabView: some View {
        let contactChats = nomeContactChats()
        let counts = nomeContactCounts(contactChats)

        return List {
            NomeContactsHeader(
                contactCount: counts.contacts,
                groupCount: counts.groups,
                requestCount: counts.requests
            )
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 18, leading: 20, bottom: 12, trailing: 20))

            NomeContactsQuickActions(
                onAddFriend: { openNewChat(.oneTimeLink) },
                onPublicAddress: { activeUserPickerSheet = .address }
            )
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 14, trailing: 20))

            if contactChats.isEmpty {
                NomeContactsEmptyCard(
                    onAddFriend: { openNewChat(.oneTimeLink) },
                    onPublicAddress: { activeUserPickerSheet = .address }
                )
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 24, trailing: 20))
            } else {
                Section {
                    if #available(iOS 16.0, *) {
                        ForEach(contactChats, id: \.viewId) { chat in
                            ChatListNavLink(chat: chat, parentSheet: $sheet)
                                .padding(.trailing, -16)
                                .disabled(chatModel.chatRunning != true || chatModel.deletedChats.contains(chat.chatInfo.id))
                                .listRowBackground(Color.clear)
                        }
                        .offset(x: -8)
                    } else {
                        ForEach(contactChats, id: \.viewId) { chat in
                            ChatListNavLink(chat: chat, parentSheet: $sheet)
                                .listRowSeparator(.hidden)
                                .listRowInsets(EdgeInsets())
                                .background { theme.colors.background }
                                .disabled(chatModel.chatRunning != true || chatModel.deletedChats.contains(chat.chatInfo.id))
                        }
                    }
                } header: {
                    Text("联系人和群组")
                        .foregroundColor(.secondary)
                }
            }
        }
        .listStyle(.plain)
        .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
    }

    private var chatListContent: some View {
        let cs = filteredChats()
        return ZStack {
            ScrollViewReader { scrollProxy in
                List {
                    NomeHomeHeader(
                        user: chatModel.currentUser ?? User.sampleData,
                        chatRunning: chatModel.chatRunning,
                        showsSearch: chatModel.chats.isEmpty,
                        onProfile: { userPickerShown = true },
                        onAdd: { openNewChat() },
                        onSearch: { activateSearchOrConnect() }
                    )
                    .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: oneHandUI ? 12 : 18, leading: 20, bottom: 8, trailing: 20))
                    .id(chatModel.chats.isEmpty ? "searchBar" : "nomeHomeHeader")

                    if !chatModel.chats.isEmpty {
                        ChatListSearchBar(
                            searchMode: $searchMode,
                            searchFocussed: $searchFocussed,
                            searchText: $searchText,
                            searchShowingSimplexLink: $searchShowingSimplexLink,
                            searchChatFilteredBySimplexLink: $searchChatFilteredBySimplexLink,
                            parentSheet: $sheet
                        )
                        .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                        .frame(maxWidth: .infinity)
                        .id("searchBar")
                    }

                    if hasConversations {
                        NomeHomeCompactActions(
                            onAddFriend: { openNewChat(.oneTimeLink) },
                            onJoinGroup: { openNewChat(.joinGroup) },
                            onPublicAddress: { activeUserPickerSheet = .address }
                        )
                        .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 8, trailing: 20))
                    } else if chatModel.chats.isEmpty {
                        NomeEmptyInboxCard(
                            onAddFriend: { openNewChat(.oneTimeLink) },
                            onJoinGroup: { openNewChat(.joinGroup) },
                            onPublicAddress: { activeUserPickerSheet = .address }
                        )
                        .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 20, trailing: 20))
                    } else {
                        NomeHomeQuickActions(
                            onAddFriend: { openNewChat(.oneTimeLink) },
                            onJoinGroup: { openNewChat(.joinGroup) },
                            onPublicAddress: { activeUserPickerSheet = .address }
                        )
                        .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 10, trailing: 20))
                    }
                    if !oneHandUICardShown && !chatModel.chats.isEmpty {
                        OneHandUICard()
                            .padding(.vertical, 6)
                            .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                    }
                    if #available(iOS 16.0, *) {
                        ForEach(cs, id: \.viewId) { chat in
                            ChatListNavLink(chat: chat, parentSheet: $sheet)
                                .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                                .padding(.trailing, -16)
                                .disabled(chatModel.chatRunning != true || chatModel.deletedChats.contains(chat.chatInfo.id))
                                .listRowBackground(Color.clear)
                        }
                        .offset(x: -8)
                    } else {
                        ForEach(cs, id: \.viewId) { chat in
                            ChatListNavLink(chat: chat,  parentSheet: $sheet)
                            .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                            .listRowSeparator(.hidden)
                            .listRowInsets(EdgeInsets())
                            .background { theme.colors.background } // Hides default list selection colour
                            .disabled(chatModel.chatRunning != true || chatModel.deletedChats.contains(chat.chatInfo.id))
                        }
                    }
                    if !addressCreationCardShown && hasConversations {
                        ConnectBannerCard()
                            .padding(.vertical, 6)
                            .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                    }
                }
                .listStyle(.plain)
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
            if cs.isEmpty && !chatModel.chats.isEmpty {
                noChatsView()
                    .scaleEffect(x: 1, y: shouldInvertChatList ? -1 : 1, anchor: .center)
                    .foregroundColor(.secondary)
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
        chatModel.chats.filter { chat in
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
        }
    }

    private func nomeContactCounts(_ chats: [Chat]) -> (contacts: Int, groups: Int, requests: Int) {
        chats.reduce((contacts: 0, groups: 0, requests: 0)) { counts, chat in
            switch chat.chatInfo {
            case .direct:
                return (counts.contacts + 1, counts.groups, counts.requests)
            case .group:
                return (counts.contacts, counts.groups + 1, counts.requests)
            case .contactRequest, .contactConnection:
                return (counts.contacts, counts.groups, counts.requests + 1)
            case .local, .invalidJSON:
                return counts
            }
        }
    }
}

private enum NomeHomePalette {
    static let navy = Color(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0)
    static let green = Color(red: 22.0 / 255.0, green: 174.0 / 255.0, blue: 102.0 / 255.0)
    static let blue = Color(red: 39.0 / 255.0, green: 107.0 / 255.0, blue: 255.0 / 255.0)
    static let purple = Color(red: 116.0 / 255.0, green: 89.0 / 255.0, blue: 238.0 / 255.0)
    static let surface = Color(uiColor: .secondarySystemGroupedBackground)
    static let border = Color.black.opacity(0.06)
}

private enum NomeHomeTab {
    case home
    case contacts
    case settings
}

private struct NomeHomeHeader: View {
    @Environment(\.colorScheme) var colorScheme
    let user: User
    let chatRunning: Bool?
    let showsSearch: Bool
    let onProfile: () -> Void
    let onAdd: () -> Void
    let onSearch: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(colorScheme == .light ? "logo" : "logo-light")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 102, height: 32, alignment: .leading)
                    .accessibilityHidden(true)

                Spacer(minLength: 12)

                Button(action: onProfile) {
                    ProfileImage(imageStr: user.image, size: 32, color: Color(uiColor: .tertiarySystemGroupedBackground))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("身份中心"))

                Button(action: onAdd) {
                    Image(systemName: "plus")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 34, height: 34)
                        .background(Circle().fill(NomeHomePalette.green))
                }
                .disabled(chatRunning != true)
                .buttonStyle(.plain)
                .accessibilityLabel(Text("新建连接"))
            }

            if showsSearch {
                Button(action: onSearch) {
                    HStack(spacing: 10) {
                        Image(systemName: "magnifyingglass")
                        Text("搜索联系人或粘贴邀请链接")
                            .lineLimit(1)
                            .minimumScaleFactor(0.82)
                        Spacer(minLength: 0)
                        Image(systemName: "qrcode.viewfinder")
                            .font(.system(size: 16, weight: .semibold))
                    }
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 14)
                    .frame(height: 42)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color(uiColor: .tertiarySystemGroupedBackground))
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private struct NomeHomeQuickActions: View {
    let onAddFriend: () -> Void
    let onJoinGroup: () -> Void
    let onPublicAddress: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("开始")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(.secondary)

            Button(action: onAddFriend) {
                NomeTaskCard(
                    icon: "link.badge.plus",
                    title: "添加朋友",
                    subtitle: "生成一次性链接或二维码",
                    tint: NomeHomePalette.green,
                    layout: .horizontal
                )
            }
            .buttonStyle(.plain)

            HStack(spacing: 10) {
                Button(action: onJoinGroup) {
                    NomeTaskCard(
                        icon: "person.2.fill",
                        title: "加入群组",
                        subtitle: "扫码或粘贴邀请",
                        tint: NomeHomePalette.blue,
                        layout: .compact
                    )
                }
                .buttonStyle(.plain)

                Button(action: onPublicAddress) {
                    NomeTaskCard(
                        icon: "globe",
                        title: "公开联系方式",
                        subtitle: "可重复分享地址",
                        tint: NomeHomePalette.purple,
                        layout: .compact
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }
}

private struct NomeHomeCompactActions: View {
    let onAddFriend: () -> Void
    let onJoinGroup: () -> Void
    let onPublicAddress: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            actionButton(
                icon: "link.badge.plus",
                title: "添加朋友",
                tint: NomeHomePalette.green,
                action: onAddFriend
            )

            actionDivider

            actionButton(
                icon: "person.2",
                title: "加入群组",
                tint: NomeHomePalette.navy.opacity(0.82),
                action: onJoinGroup
            )

            actionDivider

            actionButton(
                icon: "globe.asia.australia",
                title: "公开地址",
                tint: NomeHomePalette.navy.opacity(0.82),
                action: onPublicAddress
            )
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .secondarySystemGroupedBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeHomePalette.border, lineWidth: 1)
        )
    }

    private var actionDivider: some View {
        Rectangle()
            .fill(Color(uiColor: .separator).opacity(0.42))
            .frame(width: 1, height: 28)
    }

    private func actionButton(
        icon: String,
        title: LocalizedStringKey,
        tint: Color,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 5) {
                Image(systemName: icon)
                    .font(.system(size: 17, weight: .semibold))
                    .symbolRenderingMode(.hierarchical)
                Text(title)
                    .font(.caption.weight(.medium))
                    .lineLimit(1)
                    .minimumScaleFactor(0.82)
            }
            .foregroundColor(tint)
            .frame(maxWidth: .infinity, minHeight: 52)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

private struct NomeTaskCard: View {
    enum Layout {
        case horizontal
        case compact
    }

    let icon: String
    let title: LocalizedStringKey
    let subtitle: LocalizedStringKey
    let tint: Color
    let layout: Layout

    var body: some View {
        Group {
            switch layout {
            case .horizontal:
                HStack(spacing: 14) {
                    iconView
                    textView(alignment: .leading)
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.right")
                        .font(.footnote.weight(.semibold))
                        .foregroundColor(.secondary)
                }
                .padding(14)
                .frame(maxWidth: .infinity, minHeight: 78, alignment: .leading)
            case .compact:
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        iconView
                        Spacer(minLength: 0)
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                            .foregroundColor(.secondary)
                    }
                    textView(alignment: .leading)
                }
                .padding(12)
                .frame(maxWidth: .infinity, minHeight: 126, alignment: .topLeading)
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(NomeHomePalette.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeHomePalette.border, lineWidth: 1)
        )
    }

    private var iconView: some View {
        Image(systemName: icon)
            .font(.system(size: 18, weight: .semibold))
            .foregroundColor(tint)
            .frame(width: 36, height: 36)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(tint.opacity(0.12))
            )
    }

    private func textView(alignment: HorizontalAlignment) -> some View {
        VStack(alignment: alignment, spacing: 4) {
            Text(title)
                .font(.headline)
                .foregroundColor(NomeHomePalette.navy)
                .lineLimit(1)
                .minimumScaleFactor(0.78)
            Text(subtitle)
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct NomeEmptyInboxCard: View {
    let onAddFriend: () -> Void
    let onJoinGroup: () -> Void
    let onPublicAddress: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "message.fill")
                    .font(.system(size: 21, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 46, height: 46)
                    .background(RoundedRectangle(cornerRadius: 8, style: .continuous).fill(NomeHomePalette.navy))
                VStack(alignment: .leading, spacing: 5) {
                    Text("开始第一段私密对话")
                        .font(.title3.weight(.bold))
                        .foregroundColor(NomeHomePalette.navy)
                    Text("发送一次性邀请，或扫描对方二维码建立连接。无需手机号，也无需公开用户名。")
                        .font(.callout)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
            }

            HStack(spacing: 0) {
                    NomeTrustPoint(icon: "iphone.slash", text: "无需手机号")
                trustDivider
                NomeTrustPoint(icon: "lock.shield", text: "端到端加密")
                trustDivider
                NomeTrustPoint(icon: "internaldrive", text: "本地保存")
            }
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(NomeHomePalette.green.opacity(0.08))
            )

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
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeHomePalette.border, lineWidth: 1)
        )
    }

    private var trustDivider: some View {
        Rectangle()
            .fill(NomeHomePalette.green.opacity(0.18))
            .frame(width: 1, height: 24)
    }
}

private struct NomeTrustPoint: View {
    let icon: String
    let text: LocalizedStringKey

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .semibold))
                .frame(width: 20, height: 18, alignment: .center)
            Text(text)
                .font(.caption2.weight(.medium))
                .lineLimit(1)
                .minimumScaleFactor(0.78)
                .frame(height: 16, alignment: .center)
        }
        .foregroundColor(NomeHomePalette.navy)
        .frame(maxWidth: .infinity, minHeight: 42, alignment: .center)
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
                    .fill(NomeHomePalette.green.opacity(configuration.isPressed ? 0.82 : 1))
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
                    .fill(NomeHomePalette.navy.opacity(configuration.isPressed ? 0.12 : 0.07))
            )
    }
}

private struct NomeHomeTabBar: View {
    let selected: NomeHomeTab
    let onHome: () -> Void
    let onContacts: () -> Void
    let onSettings: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            tabButton(icon: selected == .home ? "bubble.left.and.bubble.right.fill" : "bubble.left.and.bubble.right", title: "聊天", active: selected == .home, action: onHome)
            tabButton(icon: selected == .contacts ? "person.2.fill" : "person.2", title: "联系人", active: selected == .contacts, action: onContacts)
            tabButton(icon: selected == .settings ? "gearshape.fill" : "gearshape", title: "设置", active: selected == .settings, action: onSettings)
        }
        .padding(.horizontal, 20)
        .padding(.top, 9)
        .padding(.bottom, 12)
        .overlay(alignment: .top) {
            Divider()
        }
    }

    private func tabButton(icon: String, title: LocalizedStringKey, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 18, weight: active ? .semibold : .regular))
                Text(title)
                    .font(.caption2.weight(active ? .semibold : .medium))
            }
            .foregroundColor(active ? NomeHomePalette.green : .secondary)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

private struct NomeContactsHeader: View {
    let contactCount: Int
    let groupCount: Int
    let requestCount: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text("联系人")
                        .font(.largeTitle.weight(.bold))
                        .foregroundColor(NomeHomePalette.navy)
                    Text("朋友、群组和待处理请求都会在这里。")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                Spacer(minLength: 0)
                Image(systemName: "person.2.fill")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(NomeHomePalette.green)
                    .frame(width: 44, height: 44)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeHomePalette.green.opacity(0.12))
                    )
            }

            HStack(spacing: 8) {
                NomeContactsCountPill(title: "朋友", count: contactCount)
                NomeContactsCountPill(title: "群组", count: groupCount)
                NomeContactsCountPill(title: "请求", count: requestCount)
            }
        }
    }
}

private struct NomeContactsQuickActions: View {
    let onAddFriend: () -> Void
    let onPublicAddress: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            Button(action: onAddFriend) {
                NomeTaskCard(
                    icon: "link.badge.plus",
                    title: "添加朋友",
                    subtitle: "一次性链接",
                    tint: NomeHomePalette.green,
                    layout: .compact
                )
            }
            .buttonStyle(.plain)

            Button(action: onPublicAddress) {
                NomeTaskCard(
                    icon: "person.text.rectangle",
                    title: "公开联系方式",
                    subtitle: "长期可分享",
                    tint: NomeHomePalette.purple,
                    layout: .compact
                )
            }
            .buttonStyle(.plain)
        }
    }
}

private struct NomeContactsEmptyCard: View {
    let onAddFriend: () -> Void
    let onPublicAddress: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Image(systemName: "person.crop.circle.badge.plus")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 36, height: 36)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeHomePalette.navy)
                    )

                VStack(alignment: .leading, spacing: 3) {
                    Text("还没有联系人")
                        .font(.headline)
                        .foregroundColor(NomeHomePalette.navy)
                    Text("先用一次性链接连接一个朋友。")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer(minLength: 0)
            }

            Text("联系人页会保留好友、群组、连接请求和待处理邀请。现在可以先创建一次性链接，或准备一个公开联系方式。")
                .font(.subheadline)
                .lineSpacing(2)
                .foregroundColor(.secondary)
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
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeHomePalette.border, lineWidth: 1)
        )
    }
}

private struct NomeContactsCountPill: View {
    let title: LocalizedStringKey
    let count: Int

    var body: some View {
        VStack(spacing: 3) {
            Text("\(count)")
                .font(.headline.weight(.bold))
            Text(title)
                .font(.caption2.weight(.medium))
        }
        .foregroundColor(NomeHomePalette.navy)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 9)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeHomePalette.border, lineWidth: 1)
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
    @EnvironmentObject var m: ChatModel
    @EnvironmentObject var theme: AppTheme
    @EnvironmentObject var chatTagsModel: ChatTagsModel
    @StateObject private var connectProgressManager = ConnectProgressManager.shared
    @Binding var searchMode: Bool
    @FocusState.Binding var searchFocussed: Bool
    @Binding var searchText: String
    @Binding var searchShowingSimplexLink: Bool
    @Binding var searchChatFilteredBySimplexLink: String?
    @Binding var parentSheet: SomeSheet<AnyView>?
    @State private var ignoreSearchTextChange = false

    var body: some View {
        VStack(spacing: hasVisibleTags ? 10 : 0) {
            HStack(spacing: 12) {
                HStack(spacing: 4) {
                    Image(systemName: "magnifyingglass")
                    TextField("Search contacts or paste link", text: $searchText)
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
                .padding(EdgeInsets(top: 7, leading: 7, bottom: 7, trailing: 7))
                .foregroundColor(theme.colors.secondary)
                .background(Color(.tertiarySystemFill))
                .cornerRadius(10.0)

                if searchFocussed {
                    Text("Cancel")
                        .foregroundColor(theme.colors.primary)
                        .onTapGesture {
                            searchText = ""
                            searchFocussed = false
                        }
                } else if m.chats.count > 0 {
                    filterMenuButton()
                }
            }

            if hasVisibleTags {
                ScrollView([.horizontal], showsIndicators: false) {
                    TagsView(parentSheet: $parentSheet, searchText: $searchText)
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

    private var hasVisibleTags: Bool {
        chatTagsModel.presetTags.count > 1 || !chatTagsModel.userTags.isEmpty
    }

    private func filterMenuButton() -> some View {
        let showUnread = chatTagsModel.activeFilter == .unread
        return Menu {
            Button {
                if showUnread {
                    chatTagsModel.activeFilter = nil
                } else {
                    chatTagsModel.activeFilter = .unread
                }
            } label: {
                Label(showUnread ? "显示全部" : "仅看未读", systemImage: showUnread ? "text.badge.checkmark" : "envelope.badge")
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
                Label("新建列表", systemImage: "rectangle.stack.badge.plus")
            }
        } label: {
            ZStack {
                Color.clear
                    .frame(width: 30, height: 30)
                Image(systemName: showUnread ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease")
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(showUnread ? theme.colors.primary : theme.colors.secondary)
                    .frame(width: showUnread ? 22 : 16, height: showUnread ? 22 : 16)
            }
        }
        .accessibilityLabel(Text("筛选和列表"))
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
        HStack {
            tagsView()
        }
    }
    
    @ViewBuilder private func tagsView() -> some View {
        if chatTagsModel.presetTags.count > 1 {
            if chatTagsModel.presetTags.count + chatTagsModel.userTags.count <= 3 {
                expandedPresetTagsFiltersView()
            } else {
                collapsedTagsFilterView()
                ForEach(PresetTag.allCases, id: \.id) { (tag: PresetTag) in
                    if !tag.сollapse && (chatTagsModel.presetTags[tag] ?? 0) > 0 {
                        expandedTagFilterView(tag)
                    }
                }
            }
        }
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
                    Text("Add list")
                }
            } else {
                Image(systemName: "plus")
            }
        }
        .foregroundColor(.secondary)
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
            chatModel.currentUser = User.sampleData
            chatModel.chatRunning = true
            chatModel.chatInitialized = true
            chatModel.onboardingStage = nil
            chatModel.updateChats(Self.previewChatData)
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
