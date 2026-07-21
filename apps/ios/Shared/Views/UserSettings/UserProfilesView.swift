//
// Created by Avently on 17.01.2023.
// Copyright (c) 2023 SimpleX Chat. All rights reserved.
//
// Spec: spec/client/navigation.md

import SwiftUI
import SimpleXChat

private enum NomeIdentityPalette {
    static let navy = Color(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0)
    static let green = Color(red: 22.0 / 255.0, green: 174.0 / 255.0, blue: 102.0 / 255.0)
    static let blue = Color(red: 39.0 / 255.0, green: 107.0 / 255.0, blue: 255.0 / 255.0)
    static let purple = Color(red: 116.0 / 255.0, green: 89.0 / 255.0, blue: 238.0 / 255.0)
    static let border = Color.black.opacity(0.06)
}

struct UserProfilesView: View {
    @EnvironmentObject private var m: ChatModel
    @EnvironmentObject private var theme: AppTheme
    @Environment(\.editMode) private var editMode
    @AppStorage(DEFAULT_SHOW_HIDDEN_PROFILES_NOTICE) private var showHiddenProfilesNotice = true
    @AppStorage(DEFAULT_SHOW_MUTE_PROFILE_ALERT) private var showMuteProfileAlert = true
    @State private var showDeleteConfirmation = false
    @State private var userToDelete: User?
    @State private var alert: UserProfilesAlert?
    @State private var authorized = !UserDefaults.standard.bool(forKey: DEFAULT_PERFORM_LA)
    @State private var searchTextOrPassword = ""
    @State private var selectedUser: User?
    @State private var profileHidden = false
    @State private var profileAction: UserProfileAction?
    @State private var actionPassword = ""
    @State private var navigateToProfileCreate = false

    var trimmedSearchTextOrPassword: String { searchTextOrPassword.trimmingCharacters(in: .whitespaces)}

    private enum UserProfilesAlert: Identifiable {
        case deleteUser(user: User, delSMPQueues: Bool)
        case hiddenProfilesNotice
        case muteProfileAlert
        case activateUserError(error: String)
        case error(title: LocalizedStringKey, error: LocalizedStringKey?)

        var id: String {
            switch self {
            case let .deleteUser(user, delSMPQueues): return "deleteUser \(user.userId) \(delSMPQueues)"
            case .hiddenProfilesNotice: return "hiddenProfilesNotice"
            case .muteProfileAlert: return "muteProfileAlert"
            case let .activateUserError(err): return "activateUserError \(err)"
            case let .error(title, _): return "error \(title)"
            }
        }
    }

    private enum UserProfileAction: Identifiable {
        case deleteUser(user: User, delSMPQueues: Bool)
        case unhideUser(user: User)

        var id: String {
            switch self {
            case let .deleteUser(user, delSMPQueues): return "deleteUser \(user.userId) \(delSMPQueues)"
            case let .unhideUser(user): return "unhideUser \(user.userId)"
            }
        }
    }

    var body: some View {
        List {
            if profileHidden {
                Button {
                    withAnimation { profileHidden = false }
                } label: {
                    Label("在上方输入密码后可显示隐藏身份", systemImage: "lock.open")
                }
            }
            NomeIdentityHero(
                activeName: m.currentUser?.chatViewName ?? "Nome",
                totalCount: m.users.count,
                visibleCount: visibleUsersCount
            )
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 10, leading: 16, bottom: 12, trailing: 16))

            Section {
                let users = filteredUsers()
                let v = ForEach(users) { u in
                    userView(u)
                }
                if #available(iOS 16, *) {
                    v.onDelete { indexSet in
                        if let i = indexSet.first {
                            withAuth {
                                confirmDeleteUser(users[i].user)
                            }
                        }
                    }
                } else {
                    v
                }

                if trimmedSearchTextOrPassword == "" {
                    NavigationLink(
                        destination: CreateProfile(),
                        isActive: $navigateToProfileCreate
                    ) {
                        NomeIdentityActionLabel(
                            icon: "plus",
                            title: "添加身份",
                            subtitle: "为不同场景创建独立资料",
                            tint: NomeIdentityPalette.green
                        )
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .frame(height: 38)
                            .padding(.leading, 16).padding(.vertical, 8).padding(.trailing, 32)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                withAuth {
                                    self.navigateToProfileCreate = true
                                }
                            }
                            .padding(.leading, -16).padding(.vertical, -8).padding(.trailing, -32)
                    }
                }
            } footer: {
                Text("点按可切换当前身份。向右滑动可隐藏或静音，向左滑动可删除。")
                    .foregroundColor(theme.colors.secondary)
                    .font(.body)
                    .padding(.top, 8)

            }
        }
        .toolbar {
            if #available(iOS 16, *) {
                EditButton()
            }
        }
        .navigationTitle("身份中心")
        .modifier(ThemedBackground(grouped: true))
        .searchable(text: $searchTextOrPassword, placement: .navigationBarDrawer(displayMode: .always), prompt: "搜索身份或输入隐藏身份密码")
        .autocorrectionDisabled(true)
        .textInputAutocapitalization(.never)
        .onAppear {
            if showHiddenProfilesNotice && m.users.count > 1 {
                alert = .hiddenProfilesNotice
            }
        }
        .confirmationDialog("Delete chat profile?", isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
            deleteModeButton("身份和服务器连接", true)
            deleteModeButton("仅本地身份数据", false)
        }
        .appSheet(item: $selectedUser) { user in
            HiddenProfileView(user: user, profileHidden: $profileHidden)
        }
        .onChange(of: profileHidden) { _ in
            DispatchQueue.main.asyncAfter(deadline: .now() + 10) {
                withAnimation { profileHidden = false }
            }
        }
        .appSheet(item: $profileAction) { action in
            profileActionView(action)
        }
        .alert(item: $alert) { alert in
            switch alert {
            case let .deleteUser(user, delSMPQueues):
                return Alert(
                    title: Text("删除身份？"),
                    message: Text("这个身份下的聊天和消息都会被删除，无法撤销。"),
                    primaryButton: .destructive(Text("删除")) {
                        Task { await removeUser(user, delSMPQueues, viewPwd: userViewPassword(user)) }
                    },
                    secondaryButton: .cancel()
                )
            case .hiddenProfilesNotice:
                return Alert(
                    title: Text("你可以把身份设为私密"),
                    message: Text("向右滑动身份，可以隐藏或静音这个身份。"),
                    primaryButton: .default(Text("不再提示")) {
                        showHiddenProfilesNotice = false
                    },
                    secondaryButton: .default(Text("知道了"))
                )
            case .muteProfileAlert:
                return Alert(
                    title: Text("非当前身份已静音"),
                    message: Text("当这个身份处于当前使用状态时，仍会收到它的通话和通知。"),
                    primaryButton: .default(Text("不再提示")) {
                        showMuteProfileAlert = false
                    },
                    secondaryButton: .default(Text("知道了"))
                )
            case let .activateUserError(error: err):
                return Alert(
                    title: Text("切换身份失败"),
                    message: Text(err)
                )
            case let .error(title, error):
                return mkAlert(title: title, message: error)
            }
        }
    }

    private func filteredUsers() -> [UserInfo] {
        let s = trimmedSearchTextOrPassword
        let lower = s.localizedLowercase
        return m.users.filter { u in
            if (u.user.activeUser || !u.user.hidden) && (s == "" || u.user.chatViewName.localizedLowercase.contains(lower)) {
                return true
            }
            return correctPassword(u.user, s)
        }
    }

    private var visibleUsersCount: Int {
        m.users.filter({ u in !u.user.hidden }).count
    }
    
    private func withAuth(_ action: @escaping () -> Void) {
        if authorized {
            action()
        } else {
            authenticate(
                reason: NSLocalizedString("Change chat profiles", comment: "authentication reason")
            ) { laResult in
                switch laResult {
                case .success, .unavailable:
                    authorized = true
                    AppSheetState.shared.scenePhaseActive = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5, execute: action)
                case .failed: authorized = false
                }
            }
        }
    }
    
    private func correctPassword(_ user: User, _ pwd: String) -> Bool {
        if let ph = user.viewPwdHash {
            return pwd != "" && chatPasswordHash(pwd, ph.salt) == ph.hash
        }
        return false
    }

    private func userViewPassword(_ user: User) -> String? {
        !user.hidden ? nil : trimmedSearchTextOrPassword
    }

    private func profileActionView(_ action: UserProfileAction) -> some View {
        let passwordValid = actionPassword == actionPassword.trimmingCharacters(in: .whitespaces)
        let passwordField = PassphraseField(key: $actionPassword, placeholder: "身份密码", valid: passwordValid)
        let actionEnabled: (User) -> Bool = { user in actionPassword != "" && passwordValid && correctPassword(user, actionPassword) }
        return List {
            switch action {
            case let .deleteUser(user, delSMPQueues):
                actionHeader("删除身份", user)
                Section {
                    passwordField
                    settingsRow("trash", color: theme.colors.secondary) {
                        Button("删除身份", role: .destructive) {
                            withAuth {
                                profileAction = nil
                                Task { await removeUser(user, delSMPQueues, viewPwd: actionPassword) }
                            }
                        }
                        .disabled(!actionEnabled(user))
                    }
                } footer: {
                    if actionEnabled(user) {
                        Text("这个身份下的聊天和消息都会被删除，无法撤销。")
                            .foregroundColor(theme.colors.secondary)
                            .font(.callout)
                    }
                }
            case let .unhideUser(user):
                actionHeader("取消隐藏身份", user)
                Section {
                    passwordField
                    settingsRow("lock.open", color: theme.colors.secondary) {
                        Button("取消隐藏身份") {
                            withAuth{
                                profileAction = nil
                                setUserPrivacy(user) { try await apiUnhideUser(user.userId, viewPwd: actionPassword) }
                            }
                        }
                        .disabled(!actionEnabled(user))
                    }
                }
            }
        }
        .modifier(ThemedBackground())
    }

    @ViewBuilder func actionHeader(_ title: LocalizedStringKey, _ user: User) -> some View {
        Text(title)
            .font(.title)
            .bold()
            .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
            .listRowBackground(Color.clear)
        Section() {
            ProfilePreview(profileOf: user).padding(.leading, -8)
        }
    }

    private func deleteModeButton(_ title: LocalizedStringKey, _ delSMPQueues: Bool) -> some View {
        Button(title, role: .destructive) {
            withAuth {
                if let user = userToDelete {
                    if passwordEntryRequired(user) {
                        profileAction = .deleteUser(user: user, delSMPQueues: delSMPQueues)
                    } else {
                        alert = .deleteUser(user: user, delSMPQueues: delSMPQueues)
                    }
                }
            }
        }
    }

    private func passwordEntryRequired(_ user: User) -> Bool {
        user.hidden && user.activeUser && !correctPassword(user, trimmedSearchTextOrPassword)
    }

    private func removeUser(_ user: User, _ delSMPQueues: Bool, viewPwd: String?) async {
        do {
            if user.activeUser {
                ChatModel.shared.removeWallpaperFilesFromAllChats(user)
                if let newActive = m.users.first(where: { u in !u.user.activeUser && !u.user.hidden }) {
                    try await changeActiveUserAsync_(newActive.user.userId, viewPwd: nil)
                    try await deleteUser()
                } else {
                    // Deleting the last visible user while having hidden one(s)
                    try await deleteUser()
                    try await changeActiveUserAsync_(nil, viewPwd: nil)
                    try? await stopChatAsync()
                    await MainActor.run {
                        onboardingStageDefault.set(.step1_SimpleXInfo)
                        m.onboardingStage = .step1_SimpleXInfo
                        dismissAllSheets()
                    }
                }
            } else {
                try await deleteUser()
            }
        } catch let error {
            logger.error("Error deleting user profile: \(error)")
            let a = getErrorAlert(error, "Error deleting user profile")
            alert = .error(title: a.title, error: a.message)
        }

        func deleteUser() async throws {
            try await apiDeleteUser(user.userId, delSMPQueues, viewPwd: viewPwd)
            removeWallpaperFilesFromTheme(user.uiThemes)
            await MainActor.run { withAnimation { m.removeUser(user) } }
        }
    }

    @ViewBuilder private func userView(_ userInfo: UserInfo) -> some View {
        let user = userInfo.user
        let v = Button {
            Task {
                do {
                    try await changeActiveUserAsync_(user.userId, viewPwd: userViewPassword(user))
                    dismissAllSheets()
                } catch {
                    await MainActor.run { alert = .activateUserError(error: responseError(error)) }
                }
            }
        } label: {
            HStack {
                ProfileImage(imageStr: user.image, size: 38)
                    .padding(.trailing, 12)
                VStack(alignment: .leading, spacing: 4) {
                    Text(user.chatViewName)
                        .font(.body.weight(.semibold))
                    HStack(spacing: 6) {
                        if user.activeUser {
                            NomeIdentityStatusPill(icon: "checkmark.shield", text: "当前身份", tint: NomeIdentityPalette.green)
                        } else if user.hidden {
                            NomeIdentityStatusPill(icon: "lock", text: "已隐藏", tint: NomeIdentityPalette.purple)
                        } else if !user.showNtfs {
                            NomeIdentityStatusPill(icon: "speaker.slash", text: "已静音", tint: .secondary)
                        } else {
                            NomeIdentityStatusPill(icon: "person", text: "可切换", tint: NomeIdentityPalette.blue)
                        }
                    }
                }
                Spacer()
                if user.activeUser {
                    Image(systemName: "checkmark.circle.fill").foregroundColor(NomeIdentityPalette.green)
                } else {
                    if userInfo.unreadCount > 0 {
                        userUnreadBadge(userInfo, theme: theme)
                    }
                    if user.hidden {
                        Image(systemName: "lock").foregroundColor(theme.colors.secondary)
                    } else if userInfo.unreadCount == 0 {
                        if !user.showNtfs {
                            Image(systemName: "speaker.slash").foregroundColor(theme.colors.secondary)
                        } else {
                            Image(systemName: "checkmark").foregroundColor(.clear)
                        }
                    }
                }
            }
        }
        .foregroundColor(theme.colors.onBackground)
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            if user.hidden {
                Button("取消隐藏") {
                    withAuth {
                        if passwordEntryRequired(user) {
                            profileAction = .unhideUser(user: user)
                        } else {
                            setUserPrivacy(user) { try await apiUnhideUser(user.userId, viewPwd: trimmedSearchTextOrPassword) }
                        }
                    }
                }
                .tint(.green)
            } else {
                if visibleUsersCount > 1 {
                    Button("隐藏") {
                        withAuth {
                            selectedUser = user
                        }
                    }
                    .tint(.gray)
                }
                Group {
                    if user.showNtfs {
                        Button("静音") {
                            withAuth {
                                setUserPrivacy(user, successAlert: showMuteProfileAlert ? .muteProfileAlert : nil) {
                                    try await apiMuteUser(user.userId)
                                }
                            }
                        }
                    } else {
                        Button("取消静音") {
                            withAuth {
                                setUserPrivacy(user) { try await apiUnmuteUser(user.userId) }
                            }
                        }
                    }
                }
                .tint(theme.colors.primary)
            }
        }
        if #available(iOS 16, *) {
            v
        } else {
            v.swipeActions(edge: .trailing, allowsFullSwipe: true) {
                Button("删除", role: .destructive) {
                    withAuth {
                        confirmDeleteUser(user)
                    }
                }
            }
        }
    }

    private func confirmDeleteUser(_ user: User) {
        showDeleteConfirmation = true
        userToDelete = user
    }

    private func setUserPrivacy(_ user: User, successAlert: UserProfilesAlert? = nil, _ api: @escaping () async throws -> User) {
        Task {
            do {
                let u = try await api()
                await MainActor.run {
                    withAnimation { m.updateUser(u) }
                    if successAlert != nil {
                        alert = successAlert
                    }
                }
            } catch let error {
                let a = getErrorAlert(error, "Error updating user privacy")
                alert = .error(title: a.title, error: a.message)
            }
        }
    }
}

private struct NomeIdentityHero: View {
    let activeName: String
    let totalCount: Int
    let visibleCount: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "person.crop.circle.badge.checkmark")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 42, height: 42)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeIdentityPalette.navy)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text("身份中心")
                        .font(.headline)
                        .foregroundColor(NomeIdentityPalette.navy)
                    Text("当前使用：\(activeName)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
            }

            Text("每个身份可以拥有独立资料、联系人和通知状态。隐藏身份需要密码显示，适合把不同社交场景分开管理。")
                .font(.subheadline)
                .lineSpacing(2)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 8) {
                NomeIdentityMetric(value: "\(totalCount)", label: "总身份")
                NomeIdentityMetric(value: "\(visibleCount)", label: "可见")
                NomeIdentityMetric(value: "\(max(totalCount - visibleCount, 0))", label: "隐藏")
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeIdentityPalette.border, lineWidth: 1)
        )
    }
}

private struct NomeIdentityMetric: View {
    let value: String
    let label: LocalizedStringKey

    var body: some View {
        HStack(spacing: 5) {
            Text(value)
                .font(.caption.weight(.bold))
            Text(label)
                .font(.caption2.weight(.medium))
        }
        .foregroundColor(NomeIdentityPalette.navy)
        .padding(.horizontal, 9)
        .frame(height: 26)
        .background(Capsule().fill(NomeIdentityPalette.green.opacity(0.1)))
    }
}

private struct NomeIdentityActionLabel: View {
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
                    .foregroundColor(NomeIdentityPalette.navy)
                    .lineLimit(1)
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
    }
}

private struct NomeIdentityStatusPill: View {
    let icon: String
    let text: LocalizedStringKey
    let tint: Color

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
            Text(text)
        }
        .font(.caption2.weight(.medium))
        .foregroundColor(tint)
        .padding(.horizontal, 7)
        .frame(height: 22)
        .background(Capsule().fill(tint.opacity(0.12)))
    }
}

public func chatPasswordHash(_ pwd: String, _ salt: String) -> String {
    var cPwd = pwd.cString(using: .utf8)!
    var cSalt = salt.cString(using: .utf8)!
    let cHash  = chat_password_hash(&cPwd, &cSalt)!
    let hash = fromCString(cHash)
    return hash
}

public func correctPassword(_ user: User, _ pwd: String) -> Bool {
    if let ph = user.viewPwdHash {
        return pwd != "" && chatPasswordHash(pwd, ph.salt) == ph.hash
    }
    return false
}

struct UserProfilesView_Previews: PreviewProvider {
    static var previews: some View {
        UserProfilesView()
    }
}

#if DEBUG
struct NomeIdentityCenterPreviewHost: View {
    @EnvironmentObject private var chatModel: ChatModel

    var body: some View {
        NavigationView {
            UserProfilesView()
        }
        .navigationViewStyle(.stack)
        .onAppear {
            chatModel.currentUser = User.sampleData
            chatModel.users = [UserInfo.sampleData]
            chatModel.chatRunning = true
            chatModel.chatInitialized = true
            chatModel.onboardingStage = nil
        }
    }
}
#endif
