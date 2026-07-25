//
//  NewChatMenuButton.swift
//  SimpleX (iOS)
//
//  Created by spaced4ndy on 28.11.2023.
//  Copyright © 2023 SimpleX Chat. All rights reserved.
//

import SwiftUI
import SimpleXChat

private enum NomeSheetPalette {
    static let navy = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor.label.resolvedColor(with: traits)
            : UIColor(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0, alpha: 1)
    })
    static let green = Color(red: 22.0 / 255.0, green: 174.0 / 255.0, blue: 102.0 / 255.0)
    static let blue = Color(red: 39.0 / 255.0, green: 107.0 / 255.0, blue: 255.0 / 255.0)
    static let purple = Color(red: 116.0 / 255.0, green: 89.0 / 255.0, blue: 238.0 / 255.0)
    static let surface = Color(uiColor: .secondarySystemGroupedBackground)
}

struct NewChatMenuButton: View {
    // do not use chatModel here because it prevents showing AddGroupMembersView after group creation and QR code after link creation on iOS 16
//    @EnvironmentObject var chatModel: ChatModel
    @Binding var showNewChatSheet: Bool
    @State private var alert: SomeAlert? = nil

    var body: some View {
        Button {
            ConnectProgressManager.shared.cancelConnectProgress()
            showNewChatSheet = true
        } label: {
            Image(systemName: "plus")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 34, height: 34)
                .background(Circle().fill(NomeSheetPalette.green))
                .shadow(color: NomeSheetPalette.green.opacity(0.22), radius: 8, x: 0, y: 4)
        }
        .buttonStyle(.plain)
        .alert(item: $alert) { a in
            return a.alert
        }
    }
}

private var indent: CGFloat = 36

enum NewChatSheetInitialDestination {
    case menu
    case oneTimeLink
    case scanOrPasteInvite
    case joinGroup
}

enum NewChatConnectMode {
    case general
    case group
}

struct NewChatSheet: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var theme: AppTheme
    @EnvironmentObject var chatModel: ChatModel
    @State private var searchMode = false
    @FocusState var searchFocussed: Bool
    @State private var searchText = ""
    @State private var searchShowingSimplexLink = false
    @State private var searchChatFilteredBySimplexLink: String? = nil
    @State private var alert: SomeAlert?

    // Sheet height management
    @State private var isAddContactActive = false
    @State private var isScanPasteLinkActive = false
    @State private var scanPasteMode: NewChatConnectMode = .general
    @State private var isLargeSheet = false
    @State private var allowSmallSheet = true

    @AppStorage(GROUP_DEFAULT_ONE_HAND_UI, store: groupDefaults) private var oneHandUI = true

    init(initialDestination: NewChatSheetInitialDestination = .menu) {
        let opensScanPaste = initialDestination == .scanOrPasteInvite || initialDestination == .joinGroup
        _isAddContactActive = State(initialValue: initialDestination == .oneTimeLink)
        _isScanPasteLinkActive = State(initialValue: opensScanPaste)
        _scanPasteMode = State(initialValue: initialDestination == .joinGroup ? .group : .general)
        _isLargeSheet = State(initialValue: initialDestination != .menu)
        _allowSmallSheet = State(initialValue: initialDestination == .menu)
    }

    var body: some View {
        let showArchive = chatModel.chats.contains { $0.chatInfo.contact?.chatDeleted == true }
        let v = NavigationView {
            viewBody(showArchive)
                .navigationTitle("添加")
                .navigationBarTitleDisplayMode(.large)
                .navigationBarHidden(searchMode)
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("关闭") {
                            dismiss()
                        }
                        .opacity(searchMode ? 0 : 1)
                        .disabled(searchMode)
                    }
                }
                .modifier(ThemedBackground(grouped: true))
                .alert(item: $alert) { a in
                    return a.alert
                }
        }.onDisappear {
            ConnectProgressManager.shared.cancelConnectProgress()
        }
        if #available(iOS 16.0, *), oneHandUI {
            let sheetHeight: CGFloat = showArchive ? 640 : 570
            v.presentationDetents(
                allowSmallSheet ? [.height(sheetHeight), .large] : [.large],
                selection: Binding(
                    get: { isLargeSheet || !allowSmallSheet ? .large : .height(sheetHeight) },
                    set: { isLargeSheet = $0 == .large }
                )
            )
        } else {
            v
        }
    }

    private func viewBody(_ showArchive: Bool) -> some View {
        List {
            NomeSheetIntroCard()
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 10, trailing: 16))

            HStack {
                ContactsListSearchBar(
                    searchMode: $searchMode,
                    searchFocussed: $searchFocussed,
                    searchText: $searchText,
                    searchShowingSimplexLink: $searchShowingSimplexLink,
                    searchChatFilteredBySimplexLink: $searchChatFilteredBySimplexLink
                )
                .frame(maxWidth: .infinity)
            }
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))

            if (searchText.isEmpty) {
                Section {
                    NavigationLink(isActive: $isAddContactActive) {
                        NewChatView(selection: .invite)
                            .modifier(ThemedBackground(grouped: true))
                            .navigationBarTitleDisplayMode(.inline)
                            .navigationTitle("添加朋友")
                    } label: {
                        navigateOnTap(NomeSheetActionRow(
                            icon: "link.badge.plus",
                            title: "添加朋友",
                            subtitle: "生成一次性链接或二维码，连接后失效",
                            tint: NomeSheetPalette.green
                        )) {
                            isAddContactActive = true
                        }
                    }
                    NavigationLink(isActive: $isScanPasteLinkActive) {
                        NewChatView(selection: .connect, showQRCodeScanner: scanPasteMode == .general, connectMode: scanPasteMode)
                            .modifier(ThemedBackground(grouped: true))
                            .navigationBarTitleDisplayMode(.inline)
                            .navigationTitle(scanPasteMode == .group ? "加入群组" : "扫码或粘贴邀请")
                    } label: {
                        navigateOnTap(NomeSheetActionRow(
                            icon: "qrcode.viewfinder",
                            title: "加入群组或连接朋友",
                            subtitle: "扫描二维码，或粘贴收到的邀请链接",
                            tint: NomeSheetPalette.blue
                        )) {
                            scanPasteMode = .general
                            isScanPasteLinkActive = true
                        }
                    }
                    NavigationLink {
                        UserAddressView(shareViaProfile: chatModel.currentUser?.addressShared ?? false)
                            .navigationTitle("公开联系方式")
                            .modifier(ThemedBackground(grouped: true))
                            .navigationBarTitleDisplayMode(.inline)
                    } label: {
                        NomeSheetActionRow(
                            icon: "globe",
                            title: "公开联系方式",
                            subtitle: "创建可重复分享、需要确认的联系地址",
                            tint: NomeSheetPalette.purple
                        )
                    }
                    NavigationLink {
                        AddGroupView()
                            .navigationTitle("创建群组")
                            .modifier(ThemedBackground(grouped: true))
                            .navigationBarTitleDisplayMode(.large)
                    } label: {
                        NomeSheetActionRow(
                            icon: "person.2.circle.fill",
                            title: "创建群组",
                            subtitle: "建立群聊，再邀请成员加入",
                            tint: NomeSheetPalette.navy
                        )
                    }
                    NavigationLink {
                        AddChannelView()
                            .navigationTitle("创建公开频道")
                            .modifier(ThemedBackground(grouped: true))
                            .navigationBarTitleDisplayMode(.large)
                    } label: {
                        NomeSheetActionRow(
                            icon: "antenna.radiowaves.left.and.right",
                            title: "创建公开频道",
                            subtitle: "面向更多人的发布型空间",
                            tint: NomeSheetPalette.navy.opacity(0.72)
                        )
                    }
                }
                
                if (showArchive) {
                    Section {
                        NavigationLink {
                            DeletedChats()
                        } label: {
                            newChatActionButton("archivebox", color: theme.colors.secondary) { Text("归档联系人") }
                        }
                    }
                }
            }
            
            ContactsList(
                chatPredicate: contactListChatPredicate,
                searchMode: $searchMode,
                searchText: $searchText,
                header: "联系人",
                searchFocussed: $searchFocussed,
                searchShowingSimplexLink: $searchShowingSimplexLink,
                searchChatFilteredBySimplexLink: $searchChatFilteredBySimplexLink,
                showDeletedChatIcon: true
            )
        }
    }

    private func contactListChatPredicate(_ chat: Chat, _ withSearch: Bool) -> Bool {
        switch chat.chatInfo {
        case .contactRequest: true
        case let .direct(contact):
            !isNomeUpstreamPresetContactCard(contact)
                && (contact.isContactCard || contact.active || (contact.chatDeleted && withSearch))
        default: false
        }
    }

    /// Extends label's tap area to match `.insetGrouped` list row insets
    private func navigateOnTap<L: View>(_ label: L, setActive: @escaping () -> Void) -> some View {
        label
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading, 16).padding(.vertical, 8).padding(.trailing, 32)
            .contentShape(Rectangle())
            .onTapGesture {
                isLargeSheet = true
                DispatchQueue.main.async {
                    allowSmallSheet = false
                    setActive()
                }
            }
            .padding(.leading, -16).padding(.vertical, -8).padding(.trailing, -32)
    }

    func newChatActionButton<Content : View>(_ icon: String, color: Color/* = .secondary*/, content: @escaping () -> Content) -> some View {
        ZStack(alignment: .leading) {
            Image(systemName: icon)
                .resizable()
                .scaledToFit()
                .frame(maxWidth: 24, maxHeight: 24, alignment: .center)
                .symbolRenderingMode(.monochrome)
                .foregroundColor(color)
            content().foregroundColor(theme.colors.onBackground).padding(.leading, indent)
        }
    }
}

private struct NomeSheetIntroCard: View {
    var body: some View {
        HStack(spacing: 14) {
            Image("nome_header_logo")
                .resizable()
                .scaledToFit()
                .frame(width: 92, height: 48)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 5) {
                Text("下一步做什么？")
                    .font(.headline)
                    .foregroundColor(NomeSheetPalette.navy)
                Text("用一次性邀请开始私聊，或通过邀请链接加入群组。")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(14)
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

private struct NomeSheetActionRow: View {
    let icon: String
    let title: LocalizedStringKey
    let subtitle: LocalizedStringKey
    let tint: Color

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(tint)
                .frame(width: 36, height: 36)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(tint.opacity(0.12))
                )

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.body.weight(.semibold))
                    .foregroundColor(NomeSheetPalette.navy)
                    .lineLimit(1)
                    .minimumScaleFactor(0.82)
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 4)
    }
}

func chatOrderRank(_ chat: Chat) -> Int {
    switch chat.chatInfo {
    case .contactRequest: 4
    case let .direct(contact):
        contact.isContactCard ? 5
        : contact.nextAcceptContactRequest ? 4
        : contact.nextConnectPrepared ? 3
        : contact.active ? 2
        : contact.chatDeleted ? 1
        : 0
    default: 0
    }
}

struct ContactsList: View {
    @EnvironmentObject var theme: AppTheme
    @EnvironmentObject var chatModel: ChatModel
    var chatPredicate: (Chat, Bool) -> Bool // (chat, search) -> show
    @Binding var searchMode: Bool
    @Binding var searchText: String
    var header: String? = nil
    @FocusState.Binding var searchFocussed: Bool
    @Binding var searchShowingSimplexLink: Bool
    @Binding var searchChatFilteredBySimplexLink: String?
    var showDeletedChatIcon: Bool
    @AppStorage(DEFAULT_SHOW_UNREAD_AND_FAVORITES) private var showUnreadAndFavorites = false
    
    var body: some View {
        let contactChats = chatModel.chats.filter { chat in chatPredicate(chat, !searchText.isEmpty) }
        let filteredContactChats = filteredContactChats(
            showUnreadAndFavorites: showUnreadAndFavorites,
            searchShowingSimplexLink: searchShowingSimplexLink,
            searchChatFilteredBySimplexLink: searchChatFilteredBySimplexLink,
            searchText: searchText,
            contactChats: contactChats
        )
        
        if !filteredContactChats.isEmpty {
            Section(header: Group {
                if let header = header {
                    Text(header)
                        .textCase(.uppercase)
                        .foregroundColor(theme.colors.secondary)
                    }
                }
            ) {
                ForEach(filteredContactChats, id: \.viewId) { chat in
                    ContactListNavLink(chat: chat, showDeletedChatIcon: showDeletedChatIcon)
                        .disabled(chatModel.chatRunning != true)
                }
            }
        }
        
        if filteredContactChats.isEmpty && !contactChats.isEmpty {
            noResultSection(text: "没有匹配的联系人")
        } else if contactChats.isEmpty {
            noResultSection(text: "还没有联系人")
        }
    }
    
    private func noResultSection(text: String) -> some View {
        Section {
            Text(text)
                .foregroundColor(theme.colors.secondary)
                .frame(maxWidth: .infinity, alignment: .center)

        }
        .listRowSeparator(.hidden)
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets(top: 7, leading: 0, bottom: 7, trailing: 0))
    }

    private func chatComparator(chat1: Chat, chat2: Chat) -> Bool {
        let r1 = chatOrderRank(chat1)
        let r2 = chatOrderRank(chat2)
        return r1 > r2 ? true : r1 < r2 ? false : chat1.chatInfo.chatTs > chat2.chatInfo.chatTs
    }
        
    private func filterChat(chat: Chat, searchText: String, showUnreadAndFavorites: Bool) -> Bool {
        var meetsPredicate = true
        let s = searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let cInfo = chat.chatInfo

        if !searchText.isEmpty {
            if (!cInfo.chatViewName.lowercased().contains(searchText.lowercased())) {
                if case let .direct(contact) = cInfo {
                    meetsPredicate = contact.profile.displayName.lowercased().contains(s) || contact.fullName.lowercased().contains(s)
                } else {
                    meetsPredicate = false
                }
            }
        }

        if showUnreadAndFavorites {
            meetsPredicate = meetsPredicate && (cInfo.chatSettings?.favorite ?? false)
        }

        return meetsPredicate
    }
    
    func filteredContactChats(
        showUnreadAndFavorites: Bool,
        searchShowingSimplexLink: Bool,
        searchChatFilteredBySimplexLink: String?,
        searchText: String,
        contactChats: [Chat]
    ) -> [Chat] {
        let linkChatId = searchChatFilteredBySimplexLink
        let s = searchShowingSimplexLink ? "" : searchText.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

        let filteredChats: [Chat]

        if let linkChatId = linkChatId {
            filteredChats = contactChats.filter { $0.id == linkChatId }
        } else {
            filteredChats = contactChats.filter { chat in
                filterChat(chat: chat, searchText: s, showUnreadAndFavorites: showUnreadAndFavorites)
            }
        }

        return filteredChats.sorted(by: chatComparator)
    }
}

struct ContactsListSearchBar: View {
    @EnvironmentObject var m: ChatModel
    @StateObject private var connectProgressManager = ConnectProgressManager.shared
    @EnvironmentObject var theme: AppTheme
    @Binding var searchMode: Bool
    @FocusState.Binding var searchFocussed: Bool
    @Binding var searchText: String
    @Binding var searchShowingSimplexLink: Bool
    @Binding var searchChatFilteredBySimplexLink: String?
    @State private var ignoreSearchTextChange = false
    @AppStorage(DEFAULT_SHOW_UNREAD_AND_FAVORITES) private var showUnreadAndFavorites = false

    var body: some View {
        HStack(spacing: 12) {
            HStack(spacing: 4) {
                Spacer()
                    .frame(width: 8)
                Image(systemName: "magnifyingglass")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 16, height: 16)
                TextField("搜索联系人或粘贴邀请链接", text: $searchText)
                    .foregroundColor(searchShowingSimplexLink ? theme.colors.secondary : theme.colors.onBackground)
                    .disabled(searchShowingSimplexLink)
                    .focused($searchFocussed)
                    .frame(maxWidth: .infinity)
                if connectProgressManager.showConnectProgress != nil {
                    ProgressView()
                }
                if !searchText.isEmpty {
                    Image(systemName: "xmark.circle.fill")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 16, height: 16)
                        .onTapGesture {
                            searchText = ""
                        }
                }
            }
            .padding(EdgeInsets(top: 7, leading: 7, bottom: 7, trailing: 7))
            .foregroundColor(theme.colors.secondary)
            .background(Color(uiColor: .secondarySystemGroupedBackground))
            .cornerRadius(10.0)

            if searchFocussed {
                Text("Cancel")
                    .foregroundColor(theme.colors.primary)
                    .onTapGesture {
                        searchText = ""
                        searchFocussed = false
                    }
            } else if m.chats.count > 0 {
                toggleFilterButton()
            }
        }
        .padding(.top, 24)
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
                        connectProgressManager.cancelConnectProgress()
                    }
                    searchShowingSimplexLink = false
                    searchChatFilteredBySimplexLink = nil
                }
            }
        }
    }

    private func toggleFilterButton() -> some View {
        ZStack {
            Color.clear
                .frame(width: 22, height: 22)
            Image(systemName: showUnreadAndFavorites ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease")
                .resizable()
                .scaledToFit()
                .foregroundColor(showUnreadAndFavorites ? theme.colors.primary : theme.colors.secondary)
                .frame(width: showUnreadAndFavorites ? 22 : 16, height: showUnreadAndFavorites ? 22 : 16)
                .onTapGesture {
                    showUnreadAndFavorites = !showUnreadAndFavorites
                }
        }
    }

    private func connect(_ link: String) {
        planAndConnect(
            link,
            theme: theme,
            dismiss: true,
            cleanup: {
                searchText = ""
                searchFocussed = false
            },
            filterKnownContact: { searchChatFilteredBySimplexLink = $0.id }
        )
    }
}


struct DeletedChats: View {
    @State private var searchMode = false
    @FocusState var searchFocussed: Bool
    @State private var searchText = ""
    @State private var searchShowingSimplexLink = false
    @State private var searchChatFilteredBySimplexLink: String? = nil
    
    var body: some View {
        List {
            ContactsListSearchBar(
                searchMode: $searchMode,
                searchFocussed: $searchFocussed,
                searchText: $searchText,
                searchShowingSimplexLink: $searchShowingSimplexLink,
                searchChatFilteredBySimplexLink: $searchChatFilteredBySimplexLink
            )
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
            .frame(maxWidth: .infinity)
            
            ContactsList(
                chatPredicate: { chat, _ in chat.chatInfo.contact?.chatDeleted == true },
                searchMode: $searchMode,
                searchText: $searchText,
                searchFocussed: $searchFocussed,
                searchShowingSimplexLink: $searchShowingSimplexLink,
                searchChatFilteredBySimplexLink: $searchChatFilteredBySimplexLink,
                showDeletedChatIcon: false
            )
        }
        .navigationTitle("归档联系人")
        .navigationBarTitleDisplayMode(.large)
        .navigationBarHidden(searchMode)
        .modifier(ThemedBackground(grouped: true))

    }
}

#Preview {
    NewChatMenuButton(showNewChatSheet: Binding.constant(false))
}
