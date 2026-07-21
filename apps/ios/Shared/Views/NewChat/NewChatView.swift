//
//  NewChatView.swift
//  SimpleX (iOS)
//
//  Created by spaced4ndy on 28.11.2023.
//  Copyright © 2023 SimpleX Chat. All rights reserved.
//
// Spec: spec/client/navigation.md

import SwiftUI
import SimpleXChat
import CodeScanner
import AVFoundation
import SimpleXChat

private enum NomeConnectPalette {
    static let navy = Color(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0)
    static let green = Color(red: 22.0 / 255.0, green: 174.0 / 255.0, blue: 102.0 / 255.0)
    static let blue = Color(red: 39.0 / 255.0, green: 107.0 / 255.0, blue: 255.0 / 255.0)
    static let purple = Color(red: 116.0 / 255.0, green: 89.0 / 255.0, blue: 238.0 / 255.0)
    static let border = Color.black.opacity(0.06)
}

struct SomeAlert: Identifiable {
    var alert: Alert
    var id: String
}

struct SomeActionSheet: Identifiable {
    var actionSheet: ActionSheet
    var id: String
}

struct SomeSheet<Content: View>: Identifiable {
    @ViewBuilder var content: Content
    var id: String
    var fraction = 0.4
}

private enum NewChatViewAlert: Identifiable {
    case newChatSomeAlert(alert: SomeAlert)
    var id: String {
        switch self {
        case let .newChatSomeAlert(alert): return "newChatSomeAlert \(alert.id)"
        }
    }
}

enum NewChatOption: Identifiable {
    case invite
    case connect

    var id: Self { self }
}

func showKeepInvitationAlert() {
    if let showingInvitation = ChatModel.shared.showingInvitation,
       !showingInvitation.connChatUsed {
        showAlert(
            NSLocalizedString("Keep unused invitation?", comment: "alert title"),
            message: NSLocalizedString("You can view invitation link again in connection details.", comment: "alert message"),
            actions: {[
                UIAlertAction(
                    title: NSLocalizedString("Keep", comment: "alert action"),
                    style: .default
                ),
                UIAlertAction(
                    title: NSLocalizedString("Delete", comment: "alert action"),
                    style: .destructive,
                    handler: { _ in
                        Task {
                            await deleteChat(Chat(
                                chatInfo: .contactConnection(contactConnection: showingInvitation.pcc),
                                chatItems: []
                            ))
                        }
                    }
                )
            ]}
        )
    }
    ChatModel.shared.showingInvitation = nil
}

// Spec: spec/client/navigation.md#NewChatView
struct NewChatView: View {
    @EnvironmentObject var m: ChatModel
    @EnvironmentObject var theme: AppTheme
    @State var selection: NewChatOption
    @State var showQRCodeScanner = false
    var connectMode: NewChatConnectMode = .general
    var onboarding: Bool = false
    @State private var invitationUsed: Bool = false
    @State private var connLinkInvitation: CreatedConnLink = CreatedConnLink(connFullLink: "", connShortLink: nil)
    @State private var showShortLink = true
    @State private var creatingConnReq = false
    @State private var invitationError: String? = nil
    @State var choosingProfile = false
    @State private var pastedLink: String = ""
    @State private var alert: NewChatViewAlert?
    @State private var contactConnection: PendingContactConnection? = nil

    var body: some View {
        VStack(alignment: .leading) {
            if !onboarding && connectMode == .general {
                Picker("添加方式", selection: $selection) {
                    Label("一次性链接", systemImage: "link")
                        .tag(NewChatOption.invite)
                    Label("扫码或粘贴", systemImage: "qrcode")
                        .tag(NewChatOption.connect)
                }
                .pickerStyle(.segmented)
                .padding()
                .onChange(of: $selection.wrappedValue) { opt in
                    if opt == NewChatOption.connect {
                        showQRCodeScanner = true
                    }
                }
            }

            VStack {
                // it seems there's a bug in iOS 15 if several views in switch (or if-else) statement have different transitions
                // https://developer.apple.com/forums/thread/714977?answerId=731615022#731615022
                if case .invite = selection {
                    prepareAndInviteView()
                        .transition(.move(edge: .leading))
                        .onAppear {
                            createInvitation()
                        }
                }
                if case .connect = selection {
                    ConnectView(showQRCodeScanner: $showQRCodeScanner, pastedLink: $pastedLink, alert: $alert, onboarding: onboarding, mode: connectMode)
                        .transition(.move(edge: .trailing))
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .modifier(ThemedBackground(grouped: true))
            .background(
                // Rectangle is needed for swipe gesture to work on mostly empty views (creatingLinkProgressView and retryButton)
                Rectangle()
                    .fill(theme.base == DefaultTheme.LIGHT ? theme.colors.background.asGroupedBackground(theme.base.mode) : theme.colors.background)
            )
            .animation(.easeInOut(duration: 0.3333), value: selection)
            .gesture(DragGesture(minimumDistance: 20.0, coordinateSpace: .local)
                .onChanged { value in
                    switch(value.translation.width, value.translation.height) {
                    case (...0, -30...30): // left swipe
                        if selection == .invite {
                            selection = .connect
                        }
                    case (0..., -30...30): // right swipe
                        if selection == .connect {
                            selection = .invite
                        }
                    default: ()
                    }
                },
                including: onboarding ? .subviews : .all
            )
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                NomeInlineBrandMark()
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                if !onboarding {
                    InfoSheetButton {
                        AddContactLearnMore(showTitle: true)
                    }
                } else {
                    Image(systemName: "info.circle").opacity(0)
                }
            }
        }
        .if(onboarding) { $0.navigationBarTitleDisplayMode(.inline) }
        .modifier(ThemedBackground(grouped: true))
        .onChange(of: invitationUsed) { used in
            if used && !(m.showingInvitation?.connChatUsed ?? true) {
                m.markShowingInvitationUsed()
            }
        }
        .onDisappear {
            if !choosingProfile {
                showKeepInvitationAlert()
                contactConnection = nil
            }
        }
        .alert(item: $alert) { a in
            switch(a) {
            case let .newChatSomeAlert(a):
                return a.alert
            }
        }
    }

    private func prepareAndInviteView() -> some View {
        ZStack { // ZStack is needed for views to not make transitions between each other
            if connLinkInvitation.connFullLink != "" {
                InviteView(
                    invitationUsed: $invitationUsed,
                    contactConnection: $contactConnection,
                    connLinkInvitation: $connLinkInvitation,
                    showShortLink: $showShortLink,
                    choosingProfile: $choosingProfile,
                    onboarding: onboarding
                )
            } else if creatingConnReq {
                creatingLinkProgressView()
            } else {
                retryButton(error: invitationError)
            }
        }
    }

    private func createInvitation() {
        if connLinkInvitation.connFullLink == "" && contactConnection == nil && !creatingConnReq {
            guard m.currentUser != nil else {
                invitationError = "本地资料还没有准备好。请先完成资料创建，再生成邀请链接。"
                return
            }
            guard m.chatRunning == true else {
                invitationError = "聊天服务还没有启动。请稍后重试，或重新打开 Nome。"
                return
            }
            if let readinessError = realChatCoreReadinessError() {
                invitationError = readinessError
                return
            }
            invitationError = nil
            creatingConnReq = true
            Task {
                _ = try? await Task.sleep(nanoseconds: 250_000000)
                let (r, apiAlert) = await apiAddContact(incognito: incognitoGroupDefault.get())
                if let (connLink, pcc) = r {
                    await MainActor.run {
                        m.updateContactConnection(pcc)
                        m.showingInvitation = ShowingInvitation(pcc: pcc, connChatUsed: false)
                        connLinkInvitation = connLink
                        contactConnection = pcc
                    }
                } else {
                    await MainActor.run {
                        creatingConnReq = false
                        invitationError = "暂时无法创建邀请链接。请确认本地资料已完成、聊天服务正在运行，然后重试。"
                        let fallbackAlert = mkAlert(
                            title: "Cannot create link",
                            message: "Nome could not create an invitation yet. Check that your local profile is ready and chat is running."
                        )
                        alert = .newChatSomeAlert(alert: SomeAlert(alert: apiAlert ?? fallbackAlert, id: "createInvitation error"))
                    }
                }
            }
        }
    }

    // Rectangle here and in retryButton are needed for gesture to work
    private func creatingLinkProgressView() -> some View {
        ProgressView("正在创建链接...")
            .progressViewStyle(.circular)
    }

    private func retryButton(error: String?) -> some View {
        NomeInviteUnavailableView(error: error, retry: createInvitation)
    }
}

private struct NomeInviteUnavailableView: View {
    let error: String?
    let retry: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                NomeFlowPageHeader(
                    title: "添加朋友",
                    subtitle: "用一次性链接或二维码开始私聊，连接成功后自动失效。",
                    icon: "person.badge.plus",
                    tint: NomeConnectPalette.green
                )
                NomeOneTimeLinkHero()
                NomeDisabledInvitationLinkCard(error: error, retry: retry)
                NomeDisabledQRCodeCard()
                NomeOneTimeInviteSafetyNote()
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 24)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct NomeOneTimeLinkHero: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "link.badge.plus")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeConnectPalette.green)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text("一次性链接")
                        .font(.headline)
                        .foregroundColor(NomeConnectPalette.navy)
                    Text("只给一个人使用，连接成功后失效")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer(minLength: 0)
            }

            Text("生成后可以复制、分享或让对方扫码。对方连接前不会看到你的手机号或公开用户名。")
                .font(.subheadline)
                .lineSpacing(2)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 8) {
                NomeConnectPill(icon: "person.badge.plus", text: "朋友邀请")
                NomeConnectPill(icon: "timer", text: "一次有效")
                NomeConnectPill(icon: "lock.shield", text: "端到端加密")
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeConnectPalette.border, lineWidth: 1)
        )
    }
}

private struct NomeDisabledInvitationLinkCard: View {
    let error: String?
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "link")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(NomeConnectPalette.green)
                    .frame(width: 42, height: 42)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeConnectPalette.green.opacity(0.12))
                    )
                VStack(alignment: .leading, spacing: 4) {
                    Text("邀请链接")
                        .font(.headline)
                        .foregroundColor(NomeConnectPalette.navy)
                    Text("真实链接会在聊天 core 就绪后生成")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer(minLength: 0)
            }

            HStack(spacing: 10) {
                Text("nome://inv/等待生成")
                    .font(.system(.callout, design: .monospaced))
                    .foregroundColor(.secondary)
                    .lineLimit(1)
                    .truncationMode(.middle)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "doc.on.doc")
                    .foregroundColor(.secondary)
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color(uiColor: .secondarySystemGroupedBackground))
            )

            if let error, !error.isEmpty {
                Label(error, systemImage: "exclamationmark.triangle.fill")
                    .font(.footnote)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            HStack(spacing: 10) {
                Button(action: retry) {
                    Label("重试生成", systemImage: "arrow.clockwise")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .foregroundColor(.white)
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(NomeConnectPalette.green)
                        )
                }
                .buttonStyle(.plain)

                Button {} label: {
                    Label("分享", systemImage: "square.and.arrow.up")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .foregroundColor(NomeConnectPalette.navy.opacity(0.45))
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(Color(uiColor: .secondarySystemGroupedBackground))
                        )
                }
                .buttonStyle(.plain)
                .disabled(true)
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeConnectPalette.border, lineWidth: 1)
        )
    }
}

private struct NomeDisabledQRCodeCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("一次性二维码")
                    .font(.headline)
                    .foregroundColor(NomeConnectPalette.navy)
                Spacer()
                Text("等待生成")
                    .font(.caption.weight(.medium))
                    .foregroundColor(NomeConnectPalette.green)
                    .padding(.horizontal, 9)
                    .frame(height: 26)
                    .background(Capsule().fill(NomeConnectPalette.green.opacity(0.12)))
            }

            ZStack {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color(uiColor: .secondarySystemGroupedBackground))
                Image(systemName: "qrcode")
                    .font(.system(size: 112, weight: .regular))
                    .foregroundColor(NomeConnectPalette.navy.opacity(0.16))
                Image("icon-light")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 58, height: 58)
                    .padding(8)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color(uiColor: .systemBackground))
                    )
            }
            .aspectRatio(1, contentMode: .fit)

            HStack(spacing: 10) {
                Button {} label: {
                    Label("分享二维码", systemImage: "square.and.arrow.up")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .foregroundColor(.white)
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(NomeConnectPalette.blue.opacity(0.45))
                        )
                }
                .buttonStyle(.plain)
                .disabled(true)

                Button {} label: {
                    Label("重新生成", systemImage: "arrow.triangle.2.circlepath")
                        .font(.body.weight(.semibold))
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .foregroundColor(NomeConnectPalette.navy.opacity(0.45))
                        .background(
                            RoundedRectangle(cornerRadius: 8, style: .continuous)
                                .fill(Color(uiColor: .secondarySystemGroupedBackground))
                        )
                }
                .buttonStyle(.plain)
                .disabled(true)
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeConnectPalette.border, lineWidth: 1)
        )
    }
}

private struct NomeOneTimeInviteSafetyNote: View {
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 32, height: 32)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(NomeConnectPalette.green)
                )
            Text("对方连接成功后，此链接会自动失效。没有真实聊天 core 时，Nome 只展示页面预览，不会伪造可用邀请。")
                .font(.subheadline)
                .foregroundColor(NomeConnectPalette.navy)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(NomeConnectPalette.green.opacity(0.1))
        )
    }
}

private func incognitoProfileImage() -> some View {
    Image(systemName: "theatermasks.fill")
        .resizable()
        .scaledToFit()
        .frame(width: 30)
        .foregroundColor(.indigo)
}

private struct InviteView: View {
    @Environment(\.colorScheme) var colorScheme
    @EnvironmentObject var chatModel: ChatModel
    @EnvironmentObject var theme: AppTheme
    @Binding var invitationUsed: Bool
    @Binding var contactConnection: PendingContactConnection?
    @Binding var connLinkInvitation: CreatedConnLink
    @Binding var showShortLink: Bool
    @Binding var choosingProfile: Bool
    var onboarding: Bool = false

    @AppStorage(GROUP_DEFAULT_INCOGNITO, store: groupDefaults) private var incognitoDefault = false

    var body: some View {
        List {
            if !onboarding {
                NomeFlowPageHeader(
                    title: "添加朋友",
                    subtitle: "生成只给一个人使用的邀请方式，对方连接前不会看到手机号或公开用户名。",
                    icon: "person.badge.plus",
                    tint: NomeConnectPalette.green
                )
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 10, leading: 16, bottom: 12, trailing: 16))
            }

            Section(header: sectionHeader) {
                shareLinkView()
            }
            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 10))

            qrCodeView()
            if !onboarding, let selectedProfile = chatModel.currentUser {
                Section {
                    NavigationLink {
                        ActiveProfilePicker(
                            contactConnection: $contactConnection,
                            connLinkInvitation: $connLinkInvitation,
                            incognitoEnabled: $incognitoDefault,
                            choosingProfile: $choosingProfile,
                            selectedProfile: selectedProfile
                        )
                    } label: {
                        HStack {
                            if incognitoDefault {
                                incognitoProfileImage()
                                Text("Incognito")
                            } else {
                                ProfileImage(imageStr: chatModel.currentUser?.image, size: 30)
                                Text(chatModel.currentUser?.chatViewName ?? "")
                            }
                        }
                    }
                } header: {
                    Text("Share profile").foregroundColor(theme.colors.secondary)
                } footer: {
                    if incognitoDefault {
                        Text("A new random profile will be shared.")
                    }
                }
            }
        }
        .onChange(of: incognitoDefault) { incognito in
            setInvitationUsed()
        }
        .onChange(of: chatModel.currentUser) { u in
            setInvitationUsed()
        }
    }

    private var sectionHeader: some View {
        #if SIMPLEX_ASSETS
        VStack(alignment: .leading, spacing: 0) {
            Image(colorScheme == .light
                ? (onboarding ? "one-time-link" : "one-time-link-small")
                : (onboarding ? "one-time-link-light" : "one-time-link-small-light"))
                .resizable()
                .scaledToFit()
                .frame(maxWidth: .infinity)
            sectionHeaderText
        }
        .padding(.bottom, 6)
        #else
        sectionHeaderText
            .if(onboarding) { $0.padding(.bottom, 6) }
        #endif
    }

    @ViewBuilder private var sectionHeaderText: some View {
        if onboarding {
            Text("把链接发给对方，连接成功后会失效。对方可以在 Nome 中粘贴或扫码加入。")
                .font(.body).foregroundColor(theme.colors.onBackground).textCase(nil)
        } else {
            Text("分享这个一次性邀请链接").foregroundColor(theme.colors.secondary)
        }
    }

    private func shareLinkView() -> some View {
        HStack(spacing: 8) {
            let link = connLinkInvitation.simplexChatUri(short: showShortLink)
            linkTextView(link)
            Button {
                showShareSheet(items: [link])
                setInvitationUsed()
            } label: {
                Image(systemName: "square.and.arrow.up")
                    .padding(.top, -7)
                    .padding(.horizontal, 8)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private func qrCodeView() -> some View {
        Section {
            SimpleXCreatedLinkQRCode(link: connLinkInvitation, short: $showShortLink, onShare: setInvitationUsed)
                .id("simplex-qrcode-view-for-\(connLinkInvitation.simplexChatUri(short: showShortLink))")
                .padding()
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color(uiColor: .secondarySystemGroupedBackground))
                )
                .padding(.horizontal)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
                .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
        } header: {
            if onboarding {
                Text("Or show QR in person or via video call.").font(.body).foregroundColor(theme.colors.onBackground).textCase(nil)
            } else {
                ToggleShortLinkHeader(text: Text("Or show this code"), link: connLinkInvitation, short: $showShortLink)
            }
        }
    }

    private func setInvitationUsed() {
        if !invitationUsed {
            invitationUsed = true
        }
    }
}

private enum ProfileSwitchStatus {
    case switchingUser
    case switchingIncognito
    case idle
}

private struct ActiveProfilePicker: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var chatModel: ChatModel
    @EnvironmentObject var theme: AppTheme
    @Binding var contactConnection: PendingContactConnection?
    @Binding var connLinkInvitation: CreatedConnLink
    @Binding var incognitoEnabled: Bool
    @Binding var choosingProfile: Bool
    @State private var alert: SomeAlert?
    @State private var profileSwitchStatus: ProfileSwitchStatus = .idle
    @State private var switchingProfileByTimeout = false
    @State private var lastSwitchingProfileByTimeoutCall: Double?
    @State private var profiles: [User] = []
    @State private var searchTextOrPassword = ""
    @State private var showIncognitoSheet = false
    @State private var incognitoFirst: Bool = false
    @State var selectedProfile: User
    var trimmedSearchTextOrPassword: String { searchTextOrPassword.trimmingCharacters(in: .whitespaces)}

    var body: some View {
        viewBody()
            .navigationTitle("Select chat profile")
            .searchable(text: $searchTextOrPassword, placement: .navigationBarDrawer(displayMode: .always))
            .autocorrectionDisabled(true)
            .navigationBarTitleDisplayMode(.large)
            .onAppear {
                profiles = chatModel.users
                    .map { $0.user }
            }
            .onChange(of: incognitoEnabled) { incognito in
                if profileSwitchStatus != .switchingIncognito {
                    return
                }

                Task {
                    do {
                        if let contactConn = contactConnection,
                           let conn = try await apiSetConnectionIncognito(connId: contactConn.pccConnId, incognito: incognito) {
                            await MainActor.run {
                                contactConnection = conn
                                chatModel.updateContactConnection(conn)
                                profileSwitchStatus = .idle
                                dismiss()
                            }
                        }
                    } catch {
                        profileSwitchStatus = .idle
                        incognitoEnabled = !incognito
                        logger.error("apiSetConnectionIncognito error: \(responseError(error))")
                        let err = getErrorAlert(error, "Error changing to incognito!")

                        alert = SomeAlert(
                            alert: Alert(
                                title: Text(err.title),
                                message: Text(err.message ?? "Error: \(responseError(error))")
                            ),
                            id: "setConnectionIncognitoError"
                        )
                    }
                }
            }
            .onChange(of: profileSwitchStatus) { sp in
                if sp != .idle {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        switchingProfileByTimeout = profileSwitchStatus != .idle
                    }
                } else {
                    switchingProfileByTimeout = false
                }
            }
            .onChange(of: selectedProfile) { profile in
                if (profileSwitchStatus != .switchingUser) {
                    return
                }
                Task {
                    do {
                        if let contactConn = contactConnection,
                           let conn = try await apiChangeConnectionUser(connId: contactConn.pccConnId, userId: profile.userId) {
                            await MainActor.run {
                                contactConnection = conn
                                connLinkInvitation = conn.connLinkInv ?? CreatedConnLink(connFullLink: "", connShortLink: nil)
                                incognitoEnabled = false
                                chatModel.updateContactConnection(conn)
                            }
                            do {
                                try await changeActiveUserAsync_(profile.userId, viewPwd: profile.hidden ? trimmedSearchTextOrPassword : nil)
                                await MainActor.run {
                                    profileSwitchStatus = .idle
                                    dismiss()
                                }
                            } catch {
                                await MainActor.run {
                                    profileSwitchStatus = .idle
                                    alert = SomeAlert(
                                        alert: Alert(
                                            title: Text("Error switching profile"),
                                            message: Text("Your connection was moved to \(profile.chatViewName) but an error happened when switching profile.")
                                        ),
                                        id: "switchingProfileError"
                                    )
                                }
                            }
                        }
                    } catch {
                        await MainActor.run {
                            profileSwitchStatus = .idle
                            if let currentUser = chatModel.currentUser {
                                selectedProfile = currentUser
                            }
                            let err = getErrorAlert(error, "Error changing connection profile")
                            alert = SomeAlert(
                                alert: Alert(
                                    title: Text(err.title),
                                    message: Text(err.message ?? "Error: \(responseError(error))")
                                ),
                                id: "changeConnectionUserError"
                            )
                        }
                    }
                }
            }
            .alert(item: $alert) { a in
                a.alert
            }
            .onAppear {
                incognitoFirst = incognitoEnabled
                choosingProfile = true
            }
            .onDisappear {
                choosingProfile = false
            }
            .sheet(isPresented: $showIncognitoSheet) {
                IncognitoHelp()
            }
    }


    @ViewBuilder private func viewBody() -> some View {
        profilePicker()
            .allowsHitTesting(!switchingProfileByTimeout)
            .modifier(ThemedBackground(grouped: true))
            .overlay {
                if switchingProfileByTimeout {
                    ProgressView()
                        .scaleEffect(2)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
    }

    private func filteredProfiles() -> [User] {
        let s = trimmedSearchTextOrPassword
        let lower = s.localizedLowercase

        return profiles.filter { u in
            if (u.activeUser || !u.hidden) && (s == "" || u.chatViewName.localizedLowercase.contains(lower)) {
                return true
            }
            return correctPassword(u, s)
        }
    }

    private func profilerPickerUserOption(_ user: User) -> some View {
        Button {
            if selectedProfile == user && incognitoEnabled {
                incognitoEnabled = false
                profileSwitchStatus = .switchingIncognito
            } else if selectedProfile != user {
                selectedProfile = user
                profileSwitchStatus = .switchingUser
            }
        } label: {
            HStack {
                ProfileImage(imageStr: user.image, size: 30)
                    .padding(.trailing, 2)
                NameWithBadge(
                    Text(user.chatViewName).foregroundColor(theme.colors.onBackground),
                    user.profile.localBadge
                )
                .lineLimit(1)
                Spacer()
                if selectedProfile == user, !incognitoEnabled {
                    Image(systemName: "checkmark")
                        .resizable().scaledToFit().frame(width: 16)
                        .foregroundColor(theme.colors.primary)
                }
            }
        }
    }

    @ViewBuilder private func profilePicker() -> some View {
        let incognitoOption = Button {
            if !incognitoEnabled {
                incognitoEnabled = true
                profileSwitchStatus = .switchingIncognito
            }
        } label : {
            HStack {
                incognitoProfileImage()
                Text("Incognito")
                    .foregroundColor(theme.colors.onBackground)
                Image(systemName: "info.circle")
                    .foregroundColor(theme.colors.primary)
                    .font(.system(size: 14))
                    .onTapGesture {
                        showIncognitoSheet = true
                    }
                Spacer()
                if incognitoEnabled {
                    Image(systemName: "checkmark")
                        .resizable().scaledToFit().frame(width: 16)
                        .foregroundColor(theme.colors.primary)
                }
            }
        }

        List {
            let filteredProfiles = filteredProfiles()
            let activeProfile = filteredProfiles.first { u in u.activeUser }

            if let selectedProfile = activeProfile {
                let otherProfiles = filteredProfiles
                    .filter { u in u.userId != activeProfile?.userId }
                    .sorted(using: KeyPathComparator<User>(\.activeOrder, order: .reverse))

                if incognitoFirst {
                    incognitoOption
                    profilerPickerUserOption(selectedProfile)
                } else {
                    profilerPickerUserOption(selectedProfile)
                    incognitoOption
                }

                ForEach(otherProfiles) { p in
                    profilerPickerUserOption(p)
                }
            } else {
                incognitoOption
                ForEach(filteredProfiles) { p in
                    profilerPickerUserOption(p)
                }
            }
        }
        .opacity(switchingProfileByTimeout ? 0.4 : 1)
    }
}

private struct ConnectView: View {
    @Environment(\.colorScheme) var colorScheme
    @StateObject private var connectProgressManager = ConnectProgressManager.shared
    @Environment(\.dismiss) var dismiss: DismissAction
    @EnvironmentObject var theme: AppTheme
    @Binding var showQRCodeScanner: Bool
    @Binding var pastedLink: String
    @Binding var alert: NewChatViewAlert?
    var onboarding: Bool = false
    var mode: NewChatConnectMode = .general
    @State var scannerPaused: Bool = false
    @State private var pasteboardHasStrings = UIPasteboard.general.hasStrings
    @State private var manualLink = ""
    @State private var manualLinkError: String? = nil

    var body: some View {
        List {
            if !onboarding {
                NomeFlowPageHeader(
                    title: mode == .group ? "加入群组" : "扫码或粘贴邀请",
                    subtitle: mode == .group
                        ? "粘贴群组邀请，或扫描群主和成员分享的二维码。"
                        : "Nome 会识别朋友邀请、群组邀请和公开联系方式。",
                    icon: mode == .group ? "person.2.fill" : "qrcode.viewfinder",
                    tint: mode == .group ? NomeConnectPalette.blue : NomeConnectPalette.green
                )
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 10, leading: 16, bottom: 4, trailing: 16))

                Group {
                    if mode == .group {
                        NomeJoinGroupHero()
                    } else {
                        NomeConnectHero()
                    }
                }
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 10, leading: 16, bottom: 12, trailing: 16))
            }

            if mode == .group {
                Section {
                    groupInviteLinkView()
                }
                .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
            } else {
                Section(header: connectSectionHeader) {
                    pasteLinkView()
                }
                .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
            }

            Section {
                ScannerInView(showQRCodeScanner: $showQRCodeScanner, scannerPaused: $scannerPaused, processQRCode: processQRCode)
            } header: {
                Text(mode == .group ? "扫描群组二维码" : "扫描二维码").foregroundColor(theme.colors.secondary)
            } footer: {
                Text(mode == .group
                     ? "扫描群主或成员分享的群组邀请二维码。连接前请确认群组来源。"
                     : "支持 Nome / SimpleX 邀请二维码。扫描后会先识别邀请类型，再进入确认流程。")
                    .foregroundColor(theme.colors.secondary)
            }

            if mode == .group {
                NomeGroupPreviewPlaceholder()
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 10, leading: 16, bottom: 12, trailing: 16))

                NomeGroupSourceWarning()
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 12, trailing: 16))

                Section {
                    NomeDisabledBrowseGroupsRow()
                }
            }
        }
        .onDisappear {
            connectProgressManager.cancelConnectProgress()
        }
    }

    private func groupInviteLinkView() -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "link")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(NomeConnectPalette.green)
                    .frame(width: 38, height: 38)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeConnectPalette.green.opacity(0.12))
                    )

                VStack(alignment: .leading, spacing: 3) {
                    Text("群组邀请链接")
                        .font(.body.weight(.semibold))
                        .foregroundColor(NomeConnectPalette.navy)
                    Text("拥有邀请链接可申请加入群组")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            HStack(spacing: 8) {
                TextField("粘贴群组邀请链接", text: $manualLink)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)

                Button {
                    if let str = UIPasteboard.general.string {
                        manualLink = str.trimmingCharacters(in: .whitespacesAndNewlines)
                    }
                } label: {
                    Image(systemName: "doc.on.clipboard")
                        .font(.system(size: 17, weight: .semibold))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 12)
            .frame(minHeight: 48)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(Color(uiColor: .secondarySystemGroupedBackground))
            )

            Button {
                submitLink(manualLink, invalidMessage: "粘贴的内容不是 Nome 可识别的群组邀请链接。")
            } label: {
                Text("粘贴并检查")
                    .font(.body.weight(.semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeConnectPalette.green)
                    )
            }
            .buttonStyle(.plain)
            .disabled(manualLink.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            .opacity(manualLink.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? 0.45 : 1)

            if let manualLinkError {
                Text(manualLinkError)
                    .font(.footnote)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.vertical, 8)
    }

    @ViewBuilder private func pasteLinkView() -> some View {
        if pastedLink == "" {
            ZStack(alignment: .trailing) {
                Button {
                    if let str = UIPasteboard.general.string {
                        submitLink(str, invalidMessage: "粘贴的内容不是 Nome 可识别的邀请链接。")
                    }
                } label: {
                    NomeConnectActionLabel(
                        icon: pasteboardHasStrings ? "doc.on.clipboard" : "clipboard",
                        title: pasteboardHasStrings ? "粘贴邀请链接" : "剪贴板没有邀请链接",
                        subtitle: "支持朋友邀请、群组邀请和公开联系方式",
                        tint: pasteboardHasStrings ? NomeConnectPalette.green : theme.colors.secondary
                    )
                }
                .disabled(!pasteboardHasStrings)
                if connectProgressManager.showConnectProgress != nil {
                    ProgressView()
                }
            }
        } else {
            HStack(spacing: 12) {
                Image(systemName: "link")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(NomeConnectPalette.green)
                    .frame(width: 36, height: 36)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeConnectPalette.green.opacity(0.12))
                    )
                linkTextView(pastedLink)
                if connectProgressManager.showConnectProgress != nil {
                    ProgressView()
                }
            }
        }
    }

    private func processQRCode(_ resp: Result<ScanResult, ScanError>) {
        switch resp {
        case let .success(r):
            let link = r.string
            if strIsSimplexLink(r.string) {
                connect(link)
            } else {
                alert = .newChatSomeAlert(alert: SomeAlert(
                    alert: mkAlert(title: "二维码无效", message: "扫描到的内容不是 Nome 可识别的邀请二维码。"),
                    id: "processQRCode: code is not a SimpleX link"
                ))
            }
        case let .failure(e):
            logger.error("processQRCode QR code error: \(e.localizedDescription)")
            alert = .newChatSomeAlert(alert: SomeAlert(
                alert: mkAlert(title: "Invalid QR code", message: "Error scanning code: \(e.localizedDescription)"),
                id: "processQRCode: failure"
            ))
        }
    }

    private func submitLink(_ raw: String, invalidMessage: LocalizedStringKey) {
        let link = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !link.isEmpty else {
            manualLinkError = "请先粘贴群组邀请链接。"
            return
        }
        switch strConnectTarget(link) {
        case let .link(text, _, _):
            manualLinkError = nil
            pastedLink = text
            connect(text)
        case let .name(nameInfo):
            manualLinkError = nil
            showUnsupportedNameAlert(nameInfo)
        case .none:
            manualLinkError = "链接格式无效。"
            alert = .newChatSomeAlert(alert: SomeAlert(
                alert: mkAlert(title: "链接无效", message: invalidMessage),
                id: "submitLink: code is not a SimpleX link"
            ))
        }
    }

    private var connectSectionHeader: some View {
        #if SIMPLEX_ASSETS
        VStack(alignment: .leading, spacing: 0) {
            Text("粘贴收到的邀请链接").foregroundColor(theme.colors.secondary)
        }
        .padding(.bottom, 4)
        #else
        Text("粘贴收到的邀请链接").foregroundColor(theme.colors.secondary)
        #endif
    }

    private func connect(_ link: String) {
        if let readinessError = realChatCoreReadinessError() {
            alert = .newChatSomeAlert(alert: SomeAlert(
                alert: Alert(title: Text("真实聊天功能未就绪"), message: Text(readinessError)),
                id: "connect: real chat core not ready"
            ))
            return
        }
        scannerPaused = true
        planAndConnect(
            link,
            theme: theme,
            dismiss: true,
            cleanup: {
                pastedLink = ""
                scannerPaused = false
            }
        )
    }
}

private struct NomeJoinGroupHero: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "person.2.fill")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 42, height: 42)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeConnectPalette.blue)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text("通过邀请加入群组")
                        .font(.headline)
                        .foregroundColor(NomeConnectPalette.navy)
                    Text("粘贴邀请链接，或扫描群组二维码。")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer(minLength: 0)
            }

            Text("加入前先检查邀请来源。公开群可能需要管理员审核，私密群只有成员分享邀请后才能加入。")
                .font(.subheadline)
                .lineSpacing(2)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 8) {
                NomeConnectPill(icon: "link", text: "邀请链接")
                NomeConnectPill(icon: "qrcode", text: "群组二维码")
                NomeConnectPill(icon: "checkmark.shield", text: "先确认来源")
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeConnectPalette.border, lineWidth: 1)
        )
    }
}

private struct NomeConnectHero: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "qrcode.viewfinder")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 42, height: 42)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeConnectPalette.blue)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text("加入群组或连接朋友")
                        .font(.headline)
                        .foregroundColor(NomeConnectPalette.navy)
                    Text("粘贴链接或扫描二维码即可开始。")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer(minLength: 0)
            }

            Text("Nome 会识别邀请类型：一次性朋友链接、群组邀请或公开联系方式。连接前会进入确认流程，避免误加入。")
                .font(.subheadline)
                .lineSpacing(2)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 8) {
                NomeConnectPill(icon: "person.badge.plus", text: "朋友邀请")
                NomeConnectPill(icon: "person.2.fill", text: "群组邀请")
                NomeConnectPill(icon: "globe", text: "公开联系方式")
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeConnectPalette.border, lineWidth: 1)
        )
    }
}

private struct NomeGroupPreviewPlaceholder: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("邀请预览")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(NomeConnectPalette.navy)

            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(NomeConnectPalette.blue.opacity(0.12))
                    .frame(width: 58, height: 58)
                    .overlay {
                        Image(systemName: "person.2.fill")
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundColor(NomeConnectPalette.blue)
                    }

                VStack(alignment: .leading, spacing: 5) {
                    Text("粘贴或扫描后显示群组信息")
                        .font(.headline)
                        .foregroundColor(NomeConnectPalette.navy)
                        .lineLimit(2)
                    Label("群名、成员数、审核方式会在确认页展示", systemImage: "checkmark.shield")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeConnectPalette.border, lineWidth: 1)
        )
    }
}

private struct NomeGroupSourceWarning: View {
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.shield.fill")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 32, height: 32)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(NomeConnectPalette.green)
                )
            Text("加入公开群前，请确认群组来源和管理员信息。")
                .font(.subheadline)
                .foregroundColor(NomeConnectPalette.navy)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(NomeConnectPalette.green.opacity(0.1))
        )
    }
}

private struct NomeDisabledBrowseGroupsRow: View {
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "globe")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(NomeConnectPalette.blue)
                .frame(width: 38, height: 38)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(NomeConnectPalette.blue.opacity(0.1))
                )
            VStack(alignment: .leading, spacing: 3) {
                Text("浏览公开群组")
                    .font(.body.weight(.semibold))
                    .foregroundColor(NomeConnectPalette.navy)
                Text("目录服务暂未接入，后续可通过 directory bot 或自建目录实现")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .opacity(0.72)
    }
}

private struct NomeInlineBrandMark: View {
    var body: some View {
        HStack(spacing: 6) {
            Image("icon-light")
                .resizable()
                .scaledToFit()
                .frame(width: 20, height: 20)
            Text("Nome")
                .font(.subheadline.weight(.semibold))
                .foregroundColor(NomeConnectPalette.navy)
        }
        .accessibilityLabel("Nome")
    }
}

private struct NomeFlowPageHeader: View {
    let title: LocalizedStringKey
    let subtitle: LocalizedStringKey
    let icon: String
    let tint: Color

    var body: some View {
        VStack(spacing: 10) {
            HStack(spacing: 6) {
                Image("icon-light")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                Text("Nome")
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(NomeConnectPalette.navy)
            }
            .frame(maxWidth: .infinity)

            HStack(alignment: .center, spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 38, height: 38)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(tint)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 27, weight: .bold))
                        .foregroundColor(NomeConnectPalette.navy)
                        .lineLimit(1)
                        .minimumScaleFactor(0.82)
                    Text(subtitle)
                        .font(.subheadline)
                        .lineSpacing(2)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
            }
        }
        .padding(.top, 2)
        .padding(.bottom, 2)
    }
}

private struct NomeConnectPill: View {
    let icon: String
    let text: LocalizedStringKey

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
            Text(text)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .font(.caption2.weight(.medium))
        .foregroundColor(NomeConnectPalette.navy)
        .padding(.horizontal, 8)
        .frame(height: 26)
        .background(Capsule().fill(NomeConnectPalette.blue.opacity(0.1)))
    }
}

private struct NomeConnectActionLabel: View {
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
                    .foregroundColor(NomeConnectPalette.navy)
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

struct ScannerInView: View {
    @Binding var showQRCodeScanner: Bool
    var scannerPaused: Binding<Bool>? = nil
    let processQRCode: (_ resp: Result<ScanResult, ScanError>) -> Void
    @State private var cameraAuthorizationStatus: AVAuthorizationStatus?
    var scanMode: ScanMode = .continuous

    var body: some View {
        Group {
            if showQRCodeScanner, case .authorized = cameraAuthorizationStatus {
                CodeScannerView(codeTypes: [.qr], scanMode: scanMode, isPaused: scannerPaused?.wrappedValue ?? false, completion: processQRCode)
                    .aspectRatio(1, contentMode: .fit)
                    .cornerRadius(12)
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                    .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
                    .padding(.horizontal)
            } else {
                Button {
                    switch cameraAuthorizationStatus {
                    case .notDetermined: askCameraAuthorization { showQRCodeScanner = true }
                    case .restricted: ()
                    case .denied: UIApplication.shared.open(appSettingsURL)
                    case .authorized: showQRCodeScanner = true
                    default: askCameraAuthorization { showQRCodeScanner = true }
                    }
                } label: {
                    ZStack {
                        Rectangle()
                            .aspectRatio(contentMode: .fill)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .foregroundColor(Color.clear)
                        switch cameraAuthorizationStatus {
                        case .authorized, nil: EmptyView()
                        case .restricted: Text("相机不可用")
                        case .denied:  Label("允许相机权限", systemImage: "camera")
                        default: Label("点击扫码", systemImage: "qrcode")
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                .padding()
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color(uiColor: .secondarySystemGroupedBackground))
                )
                .padding(.horizontal)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
                .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
                .disabled(cameraAuthorizationStatus == .restricted)
            }
        }
        .task {
            let status = AVCaptureDevice.authorizationStatus(for: .video)
            cameraAuthorizationStatus = status
            if showQRCodeScanner {
                switch status {
                case .notDetermined: await askCameraAuthorizationAsync()
                case .restricted: showQRCodeScanner = false
                case .denied: showQRCodeScanner = false
                case .authorized: ()
                @unknown default: await askCameraAuthorizationAsync()
                }
            }
        }
    }

    func askCameraAuthorizationAsync() async {
        await AVCaptureDevice.requestAccess(for: .video)
        cameraAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
    }

    func askCameraAuthorization(_ cb: (() -> Void)? = nil) {
        AVCaptureDevice.requestAccess(for: .video) { allowed in
            cameraAuthorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)
            if allowed { cb?() }
        }
    }
}


func linkTextView(_ link: String) -> some View {
    Text(link)
        .lineLimit(1)
        .font(.caption)
        .truncationMode(.middle)
}

struct InfoSheetButton<Content: View>: View {
    @ViewBuilder let content: Content
    @State private var showInfoSheet = false

    var body: some View {
        Button {
            showInfoSheet = true
        } label: {
            Image(systemName: "info.circle")
                .resizable()
                .scaledToFit()
                .frame(width: 24, height: 24)
        }
        .sheet(isPresented: $showInfoSheet) {
            content
        }
    }
}

func strIsSimplexLink(_ str: String) -> Bool {
    if let parsedMd = parseSimpleXMarkdown(str),
       parsedMd.count == 1,
       case .simplexLink = parsedMd[0].format {
        return true
    } else {
        return false
    }
}

enum ConnectTarget {
    case link(text: String, linkType: SimplexLinkType, linkText: String)
    case name(SimplexNameInfo)
}

func strConnectTarget(_ str: String) -> ConnectTarget? {
    let parsedMd = parseSimpleXMarkdown(str)
    let links = parsedMd?.filter { $0.format?.isSimplexLink ?? false } ?? []
    return if links.count == 1, case let .simplexLink(_, linkType, _, smpHosts) = links[0].format {
        .link(text: links[0].text, linkType: linkType, linkText: simplexLinkText(linkType, smpHosts))
    } else if links.isEmpty,
              case let .simplexName(nameInfo) = parsedMd?.first(where: { if case .simplexName = $0.format { true } else { false } })?.format {
        .name(nameInfo)
    } else {
        nil
    }
}

func showUnsupportedNameAlert(_ nameInfo: SimplexNameInfo) {
    let upgrade = " " + NSLocalizedString("Please upgrade the app.", comment: "alert message")
    if nameInfo.nameType == .contact {
        showAlert(
            NSLocalizedString("Unsupported contact name", comment: "alert title"),
            message: NSLocalizedString("Connecting via contact name requires a newer app version.", comment: "alert message") + upgrade
        )
    } else {
        showAlert(
            NSLocalizedString("Unsupported channel name", comment: "alert title"),
            message: NSLocalizedString("Connecting via channel name requires a newer app version.", comment: "alert message") + upgrade
        )
    }
}

struct IncognitoToggle: View {
    @EnvironmentObject var theme: AppTheme
    @Binding var incognitoEnabled: Bool
    @State private var showIncognitoSheet = false

    var body: some View {
        ZStack(alignment: .leading) {
            Image(systemName: incognitoEnabled ? "theatermasks.fill" : "theatermasks")
                .frame(maxWidth: 24, maxHeight: 24, alignment: .center)
                .foregroundColor(incognitoEnabled ? Color.indigo : theme.colors.secondary)
                .font(.system(size: 14))
            Toggle(isOn: $incognitoEnabled) {
                HStack(spacing: 6) {
                    Text("Incognito")
                    Image(systemName: "info.circle")
                        .foregroundColor(theme.colors.primary)
                        .font(.system(size: 14))
                }
                .onTapGesture {
                    showIncognitoSheet = true
                }
            }
            .padding(.leading, 36)
        }
        .sheet(isPresented: $showIncognitoSheet) {
            IncognitoHelp()
        }
    }
}

func sharedProfileInfo(_ incognito: Bool) -> Text {
    let name = ChatModel.shared.currentUser?.displayName ?? ""
    return Text(
        incognito
        ? "A new random profile will be shared."
        : "Your profile **\(name)** will be shared."
    )
}

private func showInvitationLinkConnectingAlert(cleanup: (() -> Void)?) {
    showAlert(
        NSLocalizedString("Already connecting!", comment: "new chat sheet title"),
        message: NSLocalizedString("You are already connecting via this one-time link!", comment: "new chat sheet message"),
        actions: {[
            okCleanupAlertAction(cleanup: cleanup)
        ]}
    )
}

private func showGroupLinkConnectingAlert(groupInfo: GroupInfo?, cleanup: (() -> Void)?) {
    if let groupInfo = groupInfo {
        if groupInfo.businessChat == nil {
            showAlert(
                NSLocalizedString("Group already exists!", comment: "new chat sheet title"),
                message:
                    String.localizedStringWithFormat(
                        NSLocalizedString("You are already joining the group %@.", comment: "new chat sheet message"),
                        groupInfo.displayName
                    ),
                actions: {[
                    okCleanupAlertAction(cleanup: cleanup)
                ]}
            )
        } else {
            showAlert(
                NSLocalizedString("Chat already exists!", comment: "new chat sheet title"),
                message:
                    String.localizedStringWithFormat(
                        NSLocalizedString("You are already connecting to %@.", comment: "new chat sheet message"),
                        groupInfo.displayName
                    ),
                actions: {[
                    okCleanupAlertAction(cleanup: cleanup)
                ]}
            )
        }
    } else {
        showAlert(
            NSLocalizedString("Already joining the group!", comment: "new chat sheet title"),
            message: NSLocalizedString("You are already joining the group via this link.", comment: "new chat sheet message"),
            actions: {[
                okCleanupAlertAction(cleanup: cleanup)
            ]}
        )
    }
}

private func okCleanupAlertAction(cleanup: (() -> Void)?) -> UIAlertAction {
    UIAlertAction(
        title: NSLocalizedString("Ok", comment: "new chat action"),
        style: .default,
        handler: { _ in
            cleanup?()
        }
    )
}

private func showAskCurrentOrIncognitoProfileSheet(
    title: String,
    actionStyle: UIAlertAction.Style = .default,
    connectionLink: CreatedConnLink,
    connectionPlan: ConnectionPlan?,
    ownerVerification: OwnerVerification? = nil,
    dismiss: Bool,
    cleanup: (() -> Void)?
) {
    showSheet(
        title,
        message: ownerVerificationMessage(ownerVerification),
        actions: {[
            UIAlertAction(
                title: NSLocalizedString("Use current profile", comment: "new chat action"),
                style: actionStyle,
                handler: { _ in
                    connectViaLink(connectionLink, connectionPlan: connectionPlan, dismiss: dismiss, incognito: false, cleanup: cleanup)
                }
            ),
            UIAlertAction(
                title: NSLocalizedString("Use new incognito profile", comment: "new chat action"),
                style: actionStyle,
                handler: { _ in
                    connectViaLink(connectionLink, connectionPlan: connectionPlan, dismiss: dismiss, incognito: true, cleanup: cleanup)
                }
            ),
            UIAlertAction(
                title: NSLocalizedString("Cancel", comment: "new chat action"),
                style: .default,
                handler: { _ in
                    cleanup?()
                }
            )
        ]}
    )
}

private func showAskCurrentOrIncognitoProfileConnectContactViaAddressSheet(
    contact: Contact,
    dismiss: Bool,
    cleanup: (() -> Void)?
) {
    showSheet(
        String.localizedStringWithFormat(
            NSLocalizedString("Connect with %@", comment: "new chat action"),
            contact.chatViewName
        ),
        actions: {[
            UIAlertAction(
                title: NSLocalizedString("Use current profile", comment: "new chat action"),
                style: .default,
                handler: { _ in
                    connectContactViaAddress_(contact, dismiss: dismiss, incognito: false, cleanup: cleanup)
                }
            ),
            UIAlertAction(
                title: NSLocalizedString("Use new incognito profile", comment: "new chat action"),
                style: .default,
                handler: { _ in
                    connectContactViaAddress_(contact, dismiss: dismiss, incognito: true, cleanup: cleanup)
                }
            ),
            UIAlertAction(
                title: NSLocalizedString("Cancel", comment: "new chat action"),
                style: .default,
                handler: { _ in
                    cleanup?()
                }
            )
        ]}
    )
}

private func showOwnGroupLinkConfirmConnectSheet(
    groupInfo: GroupInfo,
    connectionLink: CreatedConnLink,
    connectionPlan: ConnectionPlan?,
    dismiss: Bool,
    cleanup: (() -> Void)?
) {
    if groupInfo.useRelays {
        showSheet(
            String.localizedStringWithFormat(
                NSLocalizedString("This is your link for channel %@!", comment: "new chat action"),
                groupInfo.displayName
            ),
            actions: {[
                UIAlertAction(
                    title: NSLocalizedString("Open channel", comment: "new chat action"),
                    style: .default,
                    handler: { _ in
                        openKnownGroup(groupInfo, dismiss: dismiss, cleanup: cleanup)
                    }
                ),
                UIAlertAction(
                    title: NSLocalizedString("Cancel", comment: "new chat action"),
                    style: .default,
                    handler: { _ in
                        cleanup?()
                    }
                )
            ]}
        )
    } else {
        showSheet(
            String.localizedStringWithFormat(
                NSLocalizedString("Join your group?\nThis is your link for group %@!", comment: "new chat action"),
                groupInfo.displayName
            ),
            actions: {[
                UIAlertAction(
                    title: NSLocalizedString("Open group", comment: "new chat action"),
                    style: .default,
                    handler: { _ in
                        openKnownGroup(groupInfo, dismiss: dismiss, cleanup: cleanup)
                    }
                ),
                UIAlertAction(
                    title: NSLocalizedString("Use current profile", comment: "new chat action"),
                    style: .destructive,
                    handler: { _ in
                        connectViaLink(connectionLink, connectionPlan: connectionPlan, dismiss: dismiss, incognito: false, cleanup: cleanup)
                    }
                ),
                UIAlertAction(
                    title: NSLocalizedString("Use new incognito profile", comment: "new chat action"),
                    style: .destructive,
                    handler: { _ in
                        connectViaLink(connectionLink, connectionPlan: connectionPlan, dismiss: dismiss, incognito: true, cleanup: cleanup)
                    }
                ),
                UIAlertAction(
                    title: NSLocalizedString("Cancel", comment: "new chat action"),
                    style: .default,
                    handler: { _ in
                        cleanup?()
                    }
                )
            ]}
        )
    }
}

private func showPrepareContactAlert(
    connectionLink: CreatedConnLink,
    contactShortLinkData: ContactShortLinkData,
    ownerVerification: OwnerVerification? = nil,
    theme: AppTheme,
    dismiss: Bool,
    cleanup: (() -> Void)?
) {
    showOpenChatAlert(
        profileName: contactShortLinkData.profile.displayName,
        profileFullName: contactShortLinkData.profile.fullName,
        profileImage:
            ProfileImage(
                imageStr: contactShortLinkData.profile.image,
                iconName: contactShortLinkData.business
                            ? "briefcase.circle.fill"
                            : contactShortLinkData.profile.peerType == .bot
                            ? "cube.fill"
                            : "person.crop.circle.fill",
                size: alertProfileImageSize
            ),
        profileBadge: contactShortLinkData.localBadge,
        theme: theme,
        information: ownerVerificationMessage(ownerVerification),
        cancelTitle: NSLocalizedString("Cancel", comment: "new chat action"),
        confirmTitle: NSLocalizedString("Open new chat", comment: "new chat action"),
        onCancel: { cleanup?() },
        onConfirm: {
            Task {
                do {
                    let chat = try await apiPrepareContact(connLink: connectionLink, contactShortLinkData: contactShortLinkData)
                    await MainActor.run {
                        ChatModel.shared.addChat(Chat(chat))
                        openKnownChat(chat.id, dismiss: dismiss, cleanup: cleanup)
                    }
                } catch let error {
                    logger.error("showPrepareContactAlert apiPrepareContact error: \(error.localizedDescription)")
                    showAlert(NSLocalizedString("Error opening chat", comment: ""), message: responseError(error))
                    await MainActor.run {
                        cleanup?()
                    }
                }
            }
        }
    )
}

private func showPrepareGroupAlert(
    connectionLink: CreatedConnLink,
    groupShortLinkInfo: GroupShortLinkInfo?,
    groupShortLinkData: GroupShortLinkData,
    ownerVerification: OwnerVerification? = nil,
    theme: AppTheme,
    dismiss: Bool,
    cleanup: (() -> Void)?
) {
    let isChannel = !(groupShortLinkInfo?.direct ?? true)
    let subscriberCount = groupShortLinkData.publicGroupData.map { "\($0.publicMemberCount) subscribers" }
    showOpenChatAlert(
        profileName: groupShortLinkData.groupProfile.displayName,
        profileFullName: groupShortLinkData.groupProfile.fullName,
        profileImage:
            ProfileImage(
                imageStr: groupShortLinkData.groupProfile.image,
                iconName: isChannel
                            ? "antenna.radiowaves.left.and.right.circle.fill"
                            : "person.2.circle.fill",
                size: alertProfileImageSize
            ),
        theme: theme,
        subtitle: isChannel ? subscriberCount : nil,
        information: ownerVerificationMessage(ownerVerification),
        cancelTitle: NSLocalizedString("Cancel", comment: "new chat action"),
        confirmTitle: isChannel
            ? NSLocalizedString("Open new channel", comment: "new chat action")
            : NSLocalizedString("Open new group", comment: "new chat action"),
        onCancel: { cleanup?() },
        onConfirm: {
            Task {
                do {
                    let chat = try await apiPrepareGroup(connLink: connectionLink, directLink: groupShortLinkInfo?.direct ?? true, groupShortLinkData: groupShortLinkData)
                    await MainActor.run {
                        if let relays = groupShortLinkInfo?.groupRelays, !relays.isEmpty,
                           case let .group(gInfo, _) = chat.chatInfo {
                            ChatModel.shared.channelRelayHostnames[gInfo.groupId] = relays
                        }
                        ChatModel.shared.addChat(Chat(chat))
                        openKnownChat(chat.id, dismiss: dismiss, cleanup: cleanup)
                    }
                } catch let error {
                    logger.error("showPrepareGroupAlert apiPrepareGroup error: \(error.localizedDescription)")
                    showAlert(NSLocalizedString(isChannel ? "Error opening channel" : "Error opening group", comment: "alert title"), message: responseError(error))
                    await MainActor.run {
                        cleanup?()
                    }
                }
            }
        }
    )
}

private func showOpenKnownContactAlert(
    _ contact: Contact,
    theme: AppTheme,
    dismiss: Bool
) {
    showOpenChatAlert(
        profileName: contact.profile.displayName,
        profileFullName: contact.profile.fullName,
        profileImage:
            ProfileImage(
                imageStr: contact.profile.image,
                iconName: contact.chatIconName,
                size: alertProfileImageSize
            ),
        profileBadge: contact.active ? contact.profile.localBadge : nil,
        theme: theme,
        cancelTitle: NSLocalizedString("Cancel", comment: "new chat action"),
        confirmTitle:
            contact.nextConnectPrepared
            ? NSLocalizedString("Open new chat", comment: "new chat action")
            : NSLocalizedString("Open chat", comment: "new chat action"),
        onConfirm: {
            openKnownContact(contact, dismiss: dismiss, cleanup: nil)
        }
    )
}

private func showOpenKnownGroupAlert(
    _ groupInfo: GroupInfo,
    theme: AppTheme,
    dismiss: Bool
) {
    let subscriberCount = groupInfo.groupSummary.publicMemberCount.map { "\($0) subscribers" }
    showOpenChatAlert(
        profileName: groupInfo.groupProfile.displayName,
        profileFullName: groupInfo.groupProfile.fullName,
        profileImage:
            ProfileImage(
                imageStr: groupInfo.groupProfile.image,
                iconName: groupInfo.chatIconName,
                size: alertProfileImageSize
            ),
        theme: theme,
        subtitle: groupInfo.useRelays ? subscriberCount : nil,
        cancelTitle: NSLocalizedString("Cancel", comment: "new chat action"),
        confirmTitle:
            groupInfo.useRelays
            ? ( groupInfo.nextConnectPrepared
                ? NSLocalizedString("Open new channel", comment: "new chat action")
                : NSLocalizedString("Open channel", comment: "new chat action")
              )
            : groupInfo.businessChat == nil
            ? ( groupInfo.nextConnectPrepared
                ? NSLocalizedString("Open new group", comment: "new chat action")
                : NSLocalizedString("Open group", comment: "new chat action")
              )
            : ( groupInfo.nextConnectPrepared
                ? NSLocalizedString("Open new chat", comment: "new chat action")
                : NSLocalizedString("Open chat", comment: "new chat action")
              ),
        onConfirm: {
            openKnownGroup(groupInfo, dismiss: dismiss, cleanup: nil)
        }
    )
}

// Spec: spec/client/navigation.md#planAndConnect
func planAndConnect(
    _ shortOrFullLink: String,
    linkOwnerSig: LinkOwnerSig? = nil,
    theme: AppTheme,
    dismiss: Bool,
    cleanup: (() -> Void)? = nil,
    filterKnownContact: ((Contact) -> Void)? = nil,
    filterKnownGroup: ((GroupInfo) -> Void)? = nil
) {
    switch strConnectTarget(shortOrFullLink) {
    case let .name(nameInfo):
        showUnsupportedNameAlert(nameInfo)
        cleanup?()
        return
    case let .link(_, linkType, _):
        if linkType == .relay {
            showAlert(
                NSLocalizedString("Relay address", comment: "alert title"),
                message: NSLocalizedString("This is a chat relay address, it cannot be used to connect.", comment: "alert message")
            )
            cleanup?()
            return
        }
    case .none: break
    }
    ConnectProgressManager.shared.cancelConnectProgress()
    let inProgress = BoxedValue(true)
    connectTask(inProgress)
    ConnectProgressManager.shared.startConnectProgress(NSLocalizedString("Loading profile…", comment: "in progress text")) {
        inProgress.boxedValue = false
        cleanup?()
    }

    func connectTask(_ inProgress: BoxedValue<Bool>) {
        Task {
            let (result, alert) = await apiConnectPlan(connLink: shortOrFullLink, linkOwnerSig: linkOwnerSig, inProgress: inProgress)
            await MainActor.run {
                ConnectProgressManager.shared.stopConnectProgress()
            }
            if !inProgress.boxedValue { return }
            if let (connectionLink, connectionPlan) = result {
                switch connectionPlan {
                case let .invitationLink(ilp):
                    switch ilp {
                    case let .ok(contactSLinkData_, ownerVerification):
                        if let contactSLinkData = contactSLinkData_ {
                            logger.debug("planAndConnect, .invitationLink, .ok, short link data present")
                            await MainActor.run {
                                showPrepareContactAlert(
                                    connectionLink: connectionLink,
                                    contactShortLinkData: contactSLinkData,
                                    ownerVerification: ownerVerification,
                                    theme: theme,
                                    dismiss: dismiss,
                                    cleanup: cleanup
                                )
                            }
                        } else {
                            logger.debug("planAndConnect, .invitationLink, .ok, no short link data")
                            await MainActor.run {
                                showAskCurrentOrIncognitoProfileSheet(
                                    title: NSLocalizedString("Connect via one-time link", comment: "new chat sheet title"),
                                    connectionLink: connectionLink,
                                    connectionPlan: connectionPlan,
                                    ownerVerification: ownerVerification,
                                    dismiss: dismiss,
                                    cleanup: cleanup
                                )
                            }
                        }
                    case .ownLink:
                        logger.debug("planAndConnect, .invitationLink, .ownLink")
                        await MainActor.run {
                            showAskCurrentOrIncognitoProfileSheet(
                                title: NSLocalizedString("Connect to yourself?\nThis is your own one-time link!", comment: "new chat sheet title"),
                                actionStyle: .destructive,
                                connectionLink: connectionLink,
                                connectionPlan: connectionPlan,
                                dismiss: dismiss,
                                cleanup: cleanup
                            )
                        }
                    case let .connecting(contact_):
                        logger.debug("planAndConnect, .invitationLink, .connecting")
                        await MainActor.run {
                            if let contact = contact_ {
                                if let f = filterKnownContact {
                                    f(contact)
                                } else {
                                    showOpenKnownContactAlert(contact, theme: theme, dismiss: dismiss)
                                }
                            } else {
                                showInvitationLinkConnectingAlert(cleanup: cleanup)
                            }
                        }
                    case let .known(contact):
                        logger.debug("planAndConnect, .invitationLink, .known")
                        await MainActor.run {
                            if let f = filterKnownContact {
                                f(contact)
                            } else {
                                showOpenKnownContactAlert(contact, theme: theme, dismiss: dismiss)
                            }
                        }
                    }
                case let .contactAddress(cap):
                    switch cap {
                    case let .ok(contactSLinkData_, ownerVerification):
                        if let contactSLinkData = contactSLinkData_ {
                            logger.debug("planAndConnect, .contactAddress, .ok, short link data present")
                            await MainActor.run {
                                showPrepareContactAlert(
                                    connectionLink: connectionLink,
                                    contactShortLinkData: contactSLinkData,
                                    ownerVerification: ownerVerification,
                                    theme: theme,
                                    dismiss: dismiss,
                                    cleanup: cleanup
                                )
                            }
                        } else {
                            logger.debug("planAndConnect, .contactAddress, .ok, no short link data")
                            await MainActor.run {
                                showAskCurrentOrIncognitoProfileSheet(
                                    title: NSLocalizedString("Connect via contact address", comment: "new chat sheet title"),
                                    connectionLink: connectionLink,
                                    connectionPlan: connectionPlan,
                                    ownerVerification: ownerVerification,
                                    dismiss: dismiss,
                                    cleanup: cleanup
                                )
                            }
                        }
                    case .ownLink:
                        logger.debug("planAndConnect, .contactAddress, .ownLink")
                        await MainActor.run {
                            showAskCurrentOrIncognitoProfileSheet(
                                title: NSLocalizedString("Connect to yourself?\nThis is your own public contact address!", comment: "new chat sheet title"),
                                actionStyle: .destructive,
                                connectionLink: connectionLink,
                                connectionPlan: connectionPlan,
                                dismiss: dismiss,
                                cleanup: cleanup
                            )
                        }
                    case .connectingConfirmReconnect:
                        logger.debug("planAndConnect, .contactAddress, .connectingConfirmReconnect")
                        await MainActor.run {
                            showAskCurrentOrIncognitoProfileSheet(
                                title: NSLocalizedString("You have already requested connection!\nRepeat connection request?", comment: "new chat sheet title"),
                                actionStyle: .destructive,
                                connectionLink: connectionLink,
                                connectionPlan: connectionPlan,
                                dismiss: dismiss,
                                cleanup: cleanup
                            )
                        }
                    case let .connectingProhibit(contact):
                        logger.debug("planAndConnect, .contactAddress, .connectingProhibit")
                        await MainActor.run {
                            if let f = filterKnownContact {
                                f(contact)
                            } else {
                                showOpenKnownContactAlert(contact, theme: theme, dismiss: dismiss)
                            }
                        }
                    case let .known(contact):
                        logger.debug("planAndConnect, .contactAddress, .known")
                        await MainActor.run {
                            if let f = filterKnownContact {
                                f(contact)
                            } else {
                                showOpenKnownContactAlert(contact, theme: theme, dismiss: dismiss)
                            }
                        }
                    case let .contactViaAddress(contact):
                        logger.debug("planAndConnect, .contactAddress, .contactViaAddress")
                        await MainActor.run {
                            showAskCurrentOrIncognitoProfileConnectContactViaAddressSheet(
                                contact: contact,
                                dismiss: dismiss,
                                cleanup: cleanup
                            )
                        }
                    }
                case let .groupLink(glp):
                    switch glp {
                    case let .ok(groupShortLinkInfo_, groupSLinkData_, ownerVerification):
                        if let groupSLinkData = groupSLinkData_ {
                            logger.debug("planAndConnect, .groupLink, .ok, short link data present")
                            await MainActor.run {
                                showPrepareGroupAlert(
                                    connectionLink: connectionLink,
                                    groupShortLinkInfo: groupShortLinkInfo_,
                                    groupShortLinkData: groupSLinkData,
                                    ownerVerification: ownerVerification,
                                    theme: theme,
                                    dismiss: dismiss,
                                    cleanup: cleanup
                                )
                            }
                        } else {
                            logger.debug("planAndConnect, .groupLink, .ok, no short link data")
                            await MainActor.run {
                                showAskCurrentOrIncognitoProfileSheet(
                                    title: NSLocalizedString("Join group", comment: "new chat sheet title"),
                                    connectionLink: connectionLink,
                                    connectionPlan: connectionPlan,
                                    ownerVerification: ownerVerification,
                                    dismiss: dismiss,
                                    cleanup: cleanup
                                )
                            }
                        }
                    case let .ownLink(groupInfo):
                        logger.debug("planAndConnect, .groupLink, .ownLink")
                        await MainActor.run {
                            if let f = filterKnownGroup {
                                f(groupInfo)
                            }
                            showOwnGroupLinkConfirmConnectSheet(
                                groupInfo: groupInfo,
                                connectionLink: connectionLink,
                                connectionPlan: connectionPlan,
                                dismiss: dismiss,
                                cleanup: cleanup
                            )
                        }
                    case .connectingConfirmReconnect:
                        logger.debug("planAndConnect, .groupLink, .connectingConfirmReconnect")
                        await MainActor.run {
                            showAskCurrentOrIncognitoProfileSheet(
                                title: NSLocalizedString("You are already joining the group!\nRepeat join request?", comment: "new chat sheet title"),
                                actionStyle: .destructive,
                                connectionLink: connectionLink,
                                connectionPlan: connectionPlan,
                                dismiss: dismiss,
                                cleanup: cleanup
                            )
                        }
                    case let .connectingProhibit(groupInfo_):
                        logger.debug("planAndConnect, .groupLink, .connectingProhibit")
                        await MainActor.run {
                            showGroupLinkConnectingAlert(groupInfo: groupInfo_, cleanup: cleanup)
                        }
                    case let .known(groupInfo):
                        logger.debug("planAndConnect, .groupLink, .known")
                        await MainActor.run {
                            if let f = filterKnownGroup {
                                f(groupInfo)
                            } else {
                                showOpenKnownGroupAlert(groupInfo, theme: theme, dismiss: dismiss)
                            }
                        }
                    case let .noRelays(groupSLinkData_):
                        logger.debug("planAndConnect, .groupLink, .noRelays")
                        await MainActor.run {
                            if let groupSLinkData = groupSLinkData_ {
                                showOpenChatAlert(
                                    profileName: groupSLinkData.groupProfile.displayName,
                                    profileFullName: groupSLinkData.groupProfile.fullName,
                                    profileImage:
                                        ProfileImage(
                                            imageStr: groupSLinkData.groupProfile.image,
                                            iconName: "antenna.radiowaves.left.and.right.circle.fill",
                                            size: alertProfileImageSize
                                        ),
                                    theme: theme,
                                    subtitle: NSLocalizedString("Channel has no active relays. Please try to join later.", comment: "alert subtitle"),
                                    cancelTitle: NSLocalizedString("OK", comment: "alert button"),
                                    confirmTitle: nil,
                                    onCancel: { cleanup?() }
                                )
                            } else {
                                showAlert(
                                    NSLocalizedString("Channel temporarily unavailable", comment: "alert title"),
                                    message: NSLocalizedString("Channel has no active relays. Please try to join later.", comment: "alert message")
                                )
                                cleanup?()
                            }
                        }
                    case let .updateRequired(groupSLinkData_):
                        logger.debug("planAndConnect, .groupLink, .updateRequired")
                        await MainActor.run {
                            if let groupSLinkData = groupSLinkData_ {
                                showOpenChatAlert(
                                    profileName: groupSLinkData.groupProfile.displayName,
                                    profileFullName: groupSLinkData.groupProfile.fullName,
                                    profileImage:
                                        ProfileImage(
                                            imageStr: groupSLinkData.groupProfile.image,
                                            iconName: "person.2.circle.fill",
                                            size: alertProfileImageSize
                                        ),
                                    theme: theme,
                                    subtitle: NSLocalizedString("This group requires a newer version of the app. Please update the app to join.", comment: "alert subtitle"),
                                    cancelTitle: NSLocalizedString("OK", comment: "alert button"),
                                    confirmTitle: nil,
                                    onCancel: { cleanup?() }
                                )
                            } else {
                                showAlert(
                                    NSLocalizedString("App update required", comment: "alert title"),
                                    message: NSLocalizedString("This group requires a newer version of the app. Please update the app to join.", comment: "alert message")
                                )
                                cleanup?()
                            }
                        }
                    }
                case let .error(chatError):
                    logger.debug("planAndConnect, .error \(chatErrorString(chatError))")
                    showAskCurrentOrIncognitoProfileSheet(
                        title: NSLocalizedString("Connect via link", comment: "new chat sheet title"),
                        connectionLink: connectionLink,
                        connectionPlan: nil,
                        dismiss: dismiss,
                        cleanup: cleanup
                    )
                }
            } else {
                await MainActor.run {
                    if let alert {
                        dismissAllSheets(animated: true) {
                            AlertManager.shared.showAlert(alert)
                            cleanup?()
                        }
                    } else {
                        cleanup?()
                    }
                }
            }
        }
    }
}

private func connectContactViaAddress_(_ contact: Contact, dismiss: Bool, incognito: Bool, cleanup: (() -> Void)? = nil) {
    Task {
        if dismiss {
            DispatchQueue.main.async {
                dismissAllSheets(animated: true)
            }
        }
        let ok = await connectContactViaAddress(contact.contactId, incognito, showAlert: { AlertManager.shared.showAlert($0) })
        if ok {
            AlertManager.shared.showAlert(connReqSentAlert(.contact))
        }
        cleanup?()
    }
}

private func connectViaLink(
    _ connectionLink: CreatedConnLink,
    connectionPlan: ConnectionPlan?,
    dismiss: Bool,
    incognito: Bool,
    cleanup: (() -> Void)?
) {
    Task {
        if let (connReqType, pcc) = await apiConnect(incognito: incognito, connLink: connectionLink) {
            await MainActor.run {
                ChatModel.shared.updateContactConnection(pcc)
            }
            let crt: ConnReqType
            crt = if let plan = connectionPlan {
                planToConnReqType(plan) ?? connReqType
            } else {
                connReqType
            }
            DispatchQueue.main.async {
                if dismiss {
                    dismissAllSheets(animated: true) {
                        AlertManager.shared.showAlert(connReqSentAlert(crt))
                    }
                } else {
                    AlertManager.shared.showAlert(connReqSentAlert(crt))
                }
            }
        } else {
            if dismiss {
                DispatchQueue.main.async {
                    dismissAllSheets(animated: true)
                }
            }
        }
        cleanup?()
    }
}

func openKnownContact(_ contact: Contact, dismiss: Bool, cleanup: (() -> Void)?) {
    if let c = ChatModel.shared.getContactChat(contact.contactId) {
        openKnownChat(c.id, dismiss: dismiss, cleanup: cleanup)
    }
}

func openKnownGroup(_ groupInfo: GroupInfo, dismiss: Bool, cleanup: (() -> Void)?) {
    if let g = ChatModel.shared.getGroupChat(groupInfo.groupId) {
        openKnownChat(g.id, dismiss: dismiss, cleanup: cleanup)
    }
}

func openKnownChat(_ chatId: ChatId, dismiss: Bool, cleanup: (() -> Void)?) {
    if dismiss {
        dismissAllSheets(animated: true) {
            ItemsModel.shared.loadOpenChat(chatId) {
                cleanup?()
            }
        }
    } else {
        ItemsModel.shared.loadOpenChat(chatId) {
            cleanup?()
        }
    }
}

func contactAlreadyConnectingAlert(_ contact: Contact) -> Alert {
    mkAlert(
        title: "Contact already exists",
        message: "You are already connecting to \(contact.displayName)."
    )
}

func groupAlreadyExistsAlert(_ groupInfo: GroupInfo) -> Alert {
    groupInfo.businessChat == nil
    ? mkAlert(
        title: "Group already exists",
        message: "You are already in group \(groupInfo.displayName)."
    )
    : mkAlert(
        title: "Chat already exists",
        message: "You are already connected with \(groupInfo.displayName)."
    )
}

enum ConnReqType: Equatable {
    case invitation
    case contact
    case groupLink

    var connReqSentText: LocalizedStringKey {
        switch self {
        case .invitation: return "You will be connected when your contact's device is online, please wait or check later!"
        case .contact: return "You will be connected when your connection request is accepted, please wait or check later!"
        case .groupLink: return "You will be connected when group link host's device is online, please wait or check later!"
        }
    }
}

private func planToConnReqType(_ connectionPlan: ConnectionPlan) -> ConnReqType? {
    switch connectionPlan {
    case .invitationLink: .invitation
    case .contactAddress: .contact
    case .groupLink: .groupLink
    case .error: nil
    }
}

private func ownerVerificationMessage(_ ov: OwnerVerification?) -> String? {
    switch ov {
    case .verified: NSLocalizedString("Link signature verified.", comment: "owner verification")
    case let .failed(reason): String.localizedStringWithFormat(NSLocalizedString("⚠️ Signature verification failed: %@.", comment: "owner verification"), reason)
    case .none: nil
    }
}

func connReqSentAlert(_ type: ConnReqType) -> Alert {
    return mkAlert(
        title: "Connection request sent!",
        message: type.connReqSentText
    )
}

struct NewChatView_Previews: PreviewProvider {
    static var previews: some View {
        @State var parentAlert: SomeAlert?
        @State var contactConnection: PendingContactConnection? = nil

        NewChatView(
            selection: .invite
        )
    }
}
