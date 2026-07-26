//
//  UserAddressView.swift
//  SimpleX (iOS)
//
//  Created by spaced4ndy on 26.04.2023.
//  Copyright © 2023 SimpleX Chat. All rights reserved.
//

import SwiftUI
import MessageUI
@preconcurrency import SimpleXChat

private enum NomeAddressPalette {
    static let navy = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor.label.resolvedColor(with: traits)
            : UIColor(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0, alpha: 1)
    })
    static let brandNavy = Color(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0)
    static let green = Color(red: 22.0 / 255.0, green: 174.0 / 255.0, blue: 102.0 / 255.0)
    static let blue = Color(red: 39.0 / 255.0, green: 107.0 / 255.0, blue: 255.0 / 255.0)
    static let purple = Color(red: 116.0 / 255.0, green: 89.0 / 255.0, blue: 238.0 / 255.0)
    static let border = Color.black.opacity(0.06)
}

struct UserAddressView: View {
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.dismiss) var dismiss: DismissAction
    @EnvironmentObject private var chatModel: ChatModel
    @EnvironmentObject var theme: AppTheme
    @State var shareViaProfile = false
    @State var autoCreate = false
    var onboarding: Bool = false
    @State private var showShortLink = true
    @State private var settings = AddressSettingsState()
    @State private var savedSettings = AddressSettingsState()
    @State private var showMailView = false
    @State private var mailViewResult: Result<MFMailComposeResult, Error>? = nil
    @State private var alert: UserAddressAlert?
    @State private var progressIndicator = false
    @State private var addressCopied = false

    private enum UserAddressAlert: Identifiable {
        case deleteAddress
        case replaceAddress
        case shareOnCreate
        case error(title: LocalizedStringKey, error: LocalizedStringKey?)
        case runtimeError(title: String, message: String?)

        var id: String {
            switch self {
            case .deleteAddress: return "deleteAddress"
            case .replaceAddress: return "replaceAddress"
            case .shareOnCreate: return "shareOnCreate"
            case let .error(title, _): return "error \(title)"
            case let .runtimeError(title, _): return "runtimeError \(title)"
            }
        }
    }

    var body: some View {
        ZStack {
            userAddressView()

            if progressIndicator {
                ZStack {
                    if chatModel.userAddress != nil {
                        Circle()
                            .fill(.white)
                            .opacity(0.7)
                            .frame(width: 56, height: 56)
                    }
                    ProgressView().scaleEffect(2)
                }
            }
        }
        .if(onboarding) { v in
            v.toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Image(systemName: "info.circle").opacity(0)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        }
        .if(!onboarding) { v in
            v.toolbar {
                ToolbarItem(placement: .principal) {
                    NomeAddressInlineBrandMark()
                }
            }
        }
        .onAppear {
            if chatModel.userAddress == nil, autoCreate {
                createAddress()
            }
        }
    }

    private func userAddressView() -> some View {
        Group {
            if onboarding {
                List {
                    if let userAddress = chatModel.userAddress {
                        onboardingAddressView(userAddress)
                    }
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        NomeAddressPageHeader()
                        NomeAddressIntroCard()

                        if let userAddress = chatModel.userAddress {
                            existingAddressDashboard(userAddress)
                                .onAppear {
                                    settings = AddressSettingsState(settings: userAddress.addressSettings)
                                    savedSettings = AddressSettingsState(settings: userAddress.addressSettings)
                                }
                        } else {
                            noAddressDashboard()
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 12)
                    .padding(.bottom, 32)
                }
                .background(Color(uiColor: .systemGroupedBackground).ignoresSafeArea())
            }
        }
        .alert(item: $alert) { alert in
            switch alert {
            case .deleteAddress:
                return Alert(
                    title: Text("Delete address?"),
                    message:
                        shareViaProfile
                    ? Text("已有联系人仍会保持连接。你的联系人会收到资料更新。")
                    : Text("已有联系人仍会保持连接。"),
                    primaryButton: .destructive(Text("关闭")) {
                        guard NomeActivationGate.require(.address) else { return }
                        if showRealCoreErrorIfNeeded() { return }
                        progressIndicator = true
                        Task {
                            do {
                                if let u = try await apiDeleteUserAddress() {
                                    DispatchQueue.main.async {
                                        chatModel.userAddress = nil
                                        chatModel.updateUser(u)
                                        if shareViaProfile {
                                            shareViaProfile = false
                                        }
                                    }
                                }
                                await MainActor.run { progressIndicator = false }
                            } catch let error {
                                logger.error("UserAddressView apiDeleteUserAddress: \(responseError(error))")
                                await MainActor.run { progressIndicator = false }
                            }
                        }
                    }, secondaryButton: .cancel()
                )
            case .replaceAddress:
                return Alert(
                    title: Text("更换公开联系方式？"),
                    message: Text("当前公开地址会停用，并生成一个新的地址。已有联系人不会被移除。"),
                    primaryButton: .destructive(Text("更换")) {
                        replaceAddress()
                    }, secondaryButton: .cancel()
                )
            case .shareOnCreate:
                return Alert(
                    title: Text("分享公开联系方式给联系人？"),
                    message: Text("它会添加到你的资料里，已有联系人会收到资料更新。"),
                    primaryButton: .default(Text("分享")) {
                        guard NomeActivationGate.require(.address) else { return }
                        if showRealCoreErrorIfNeeded() { return }
                        setProfileAddress($progressIndicator, true)
                        shareViaProfile = true
                    }, secondaryButton: .cancel()
                )
            case let .error(title, error):
                return mkAlert(title: title, message: error)
            case let .runtimeError(title, message):
                if let message {
                    return Alert(title: Text(title), message: Text(message))
                } else {
                    return Alert(title: Text(title))
                }
            }
        }
    }

    @ViewBuilder private func existingAddressDashboard(_ userAddress: UserContactLink) -> some View {
        NomePublicAddressCard(
            link: addressLink(userAddress),
            copied: addressCopied,
            copyAction: { copyAddress(userAddress) },
            shareAction: { shareAddress(userAddress) }
        )

        NomeConfirmationCard(needsConfirmation: needsConfirmationBinding)

        NomeAddressManagementCard(
            replaceTitle: userAddress.shouldBeUpgraded ? "升级地址" : "更换地址",
            replaceSubtitle: userAddress.shouldBeUpgraded ? "生成更短的公开地址" : "生成新的公开地址",
            replaceAction: {
                if userAddress.shouldBeUpgraded {
                    upgradeAddress()
                } else {
                    alert = .replaceAddress
                }
            },
            settingsAction: { addressSettingsButton(userAddress) },
            deleteAction: { alert = .deleteAddress }
        )

        NomeAddressPanel {
            createOneTimeLinkButton()
            Divider().padding(.leading, 48)
            learnMoreButton()
        }

        NomeAddressTrustNote()
    }

    @ViewBuilder private func onboardingAddressView(_ userAddress: UserContactLink) -> some View {
        Section {
            HStack(spacing: 8) {
                let link = userAddress.connLinkContact.simplexChatUri(short: showShortLink)
                linkTextView(link)
                Button { showShareSheet(items: [link]) } label: {
                    Image(systemName: "square.and.arrow.up")
                        .padding(.top, -7)
                        .padding(.horizontal, 8)
                }
            }
            .frame(maxWidth: .infinity)
        } header: {
            #if SIMPLEX_ASSETS
            VStack(alignment: .leading) {
                Image(colorScheme == .light ? "simplex-address" : "simplex-address-light")
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity)
                Text("Use this address in your social media profile, website, or email signature.")
                    .font(.body).foregroundColor(theme.colors.onBackground).textCase(nil)
            }
            .padding(.bottom, 4)
            #else
            Text("Use this address in your social media profile, website, or email signature.")
                .font(.body).foregroundColor(theme.colors.onBackground).textCase(nil)
                .padding(.bottom, 6)
            #endif
        }
        .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 10))

        Section {
            SimpleXCreatedLinkQRCode(link: userAddress.connLinkContact, short: $showShortLink)
                .id("simplex-contact-address-qrcode-\(userAddress.connLinkContact.simplexChatUri(short: showShortLink))")
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
            Text("Or use this QR - print or show online.").font(.body).foregroundColor(theme.colors.onBackground).textCase(nil)
        }
    }

    @ViewBuilder private func noAddressDashboard() -> some View {
        NomeAddressPanel {
            createAddressButton()
        }

        NomeAddressPanel {
            createOneTimeLinkButton()
            Divider().padding(.leading, 48)
            learnMoreButton()
        }

        NomeAddressTrustNote()
    }

    private func createAddressButton() -> some View {
        Button {
            createAddress()
        } label: {
            NomeAddressActionLabel(
                icon: "globe",
                title: "创建公开联系方式",
                subtitle: "可重复分享，别人发起连接前需要你确认",
                tint: NomeAddressPalette.green
            )
        }
    }

    private func createAddress() {
        guard NomeActivationGate.require(.address) else { return }
        if showRealCoreErrorIfNeeded() { return }
        progressIndicator = true
        Task {
            do {
                let connLinkContact = try await apiCreateUserAddress()
                DispatchQueue.main.async {
                    if let connLinkContact {
                        chatModel.userAddress = UserContactLink(connLinkContact)
                        let hasRelevantContacts = chatModel.chats.contains { chat in
                            if case let .direct(contact) = chat.chatInfo {
                                return contact.active && !contact.isContactCard && !contact.contactConnIncognito
                            }
                            return false
                        }
                        if hasRelevantContacts {
                            alert = .shareOnCreate
                            progressIndicator = false
                        } else {
                            setProfileAddress($progressIndicator, true)
                            shareViaProfile = true
                        }
                    } else {
                        progressIndicator = false
                    }
                }
            } catch let error {
                logger.error("UserAddressView apiCreateUserAddress: \(responseError(error))")
                let a = getErrorAlert(error, "Error creating address")
                alert = .error(title: a.title, error: a.message)
                await MainActor.run { progressIndicator = false }
            }
        }
    }

    private func replaceAddress() {
        guard NomeActivationGate.require(.address) else { return }
        if showRealCoreErrorIfNeeded() { return }
        progressIndicator = true
        Task {
            do {
                _ = try await apiDeleteUserAddress()
                let connLinkContact = try await apiCreateUserAddress()
                await MainActor.run {
                    if let connLinkContact {
                        chatModel.userAddress = UserContactLink(connLinkContact)
                    }
                    shareViaProfile = false
                    settings = AddressSettingsState()
                    savedSettings = AddressSettingsState()
                    progressIndicator = false
                }
            } catch let error {
                logger.error("UserAddressView replace address: \(responseError(error))")
                let a = getErrorAlert(error, "Error changing address")
                await MainActor.run {
                    alert = .error(title: a.title, error: a.message)
                    progressIndicator = false
                }
            }
        }
    }

    private func upgradeAddress() {
        guard NomeActivationGate.require(.address) else { return }
        if showRealCoreErrorIfNeeded() { return }
        upgradeAndShareAddressAlert(progressIndicator: $progressIndicator)
    }

    private func createOneTimeLinkButton() -> some View {
        NavigationLink {
            NewChatView(selection: .invite)
                .navigationTitle("添加朋友")
                .navigationBarTitleDisplayMode(.large)
                .modifier(ThemedBackground(grouped: true))
        } label: {
            NomeAddressActionLabel(
                icon: "link.badge.plus",
                title: "创建一次性朋友链接",
                subtitle: "只用于一次私下邀请，连接成功后失效",
                tint: NomeAddressPalette.blue
            )
        }
    }

    private func deleteAddressButton() -> some View {
        Button(role: .destructive) {
            alert = .deleteAddress
        } label: {
            Label("关闭公开联系方式", systemImage: "trash")
                .foregroundColor(Color.red)
        }
    }

    private func shareAddressButton(_ userAddress: UserContactLink) -> some View {
        return Button {
            shareAddress(userAddress)
        } label: {
            NomeAddressActionLabel(
                icon: "square.and.arrow.up",
                title: "分享公开联系方式",
                subtitle: "复制、发送或展示二维码",
                tint: NomeAddressPalette.green
            )
        }
    }

    private func shareAddress(_ userAddress: UserContactLink) {
        guard NomeActivationGate.require(.address) else { return }
        if showRealCoreErrorIfNeeded() { return }
        if userAddress.shouldBeUpgraded {
            upgradeAndShareAddressAlert(progressIndicator: $progressIndicator, shareAddress: { userAddress.shareAddress(short: showShortLink) })
        } else {
            userAddress.shareAddress(short: showShortLink)
        }
    }

    private func copyAddress(_ userAddress: UserContactLink) {
        guard NomeActivationGate.require(.address) else { return }
        if showRealCoreErrorIfNeeded() { return }
        UIPasteboard.general.string = addressLink(userAddress)
        addressCopied = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) {
            addressCopied = false
        }
    }

    private func addressLink(_ userAddress: UserContactLink) -> String {
        simplexChatLink(userAddress.connLinkContact.simplexChatUri(short: showShortLink))
    }

    private func shareViaEmailButton(_ userAddress: UserContactLink) -> some View {
        Button {
            guard NomeActivationGate.require(.address) else { return }
            showMailView = true
        } label: {
            settingsRow("envelope", color: theme.colors.secondary) {
                Text("Invite friends")
            }
        }
        .sheet(isPresented: $showMailView) {
            SendAddressMailView(
                showMailView: $showMailView,
                mailViewResult: $mailViewResult,
                userAddress: userAddress
            )
            .edgesIgnoringSafeArea(.bottom)
        }
        .onChange(of: mailViewResult == nil) { _ in
            if let r = mailViewResult {
                switch r {
                case .success: ()
                case let .failure(error):
                    logger.error("UserAddressView share via email: \(responseError(error))")
                    let a = getErrorAlert(error, "Error sending email")
                    alert = .error(title: a.title, error: a.message)
                }
                mailViewResult = nil
            }
        }
    }

    private func addressSettingsButton(_ userAddress: UserContactLink) -> some View {
        NavigationLink {
            UserAddressSettingsView(shareViaProfile: $shareViaProfile)
                .navigationTitle("公开联系方式设置")
                .navigationBarTitleDisplayMode(.large)
                .modifier(ThemedBackground(grouped: true))
        } label: {
            NomeAddressActionLabel(
                icon: "slider.horizontal.3",
                title: "地址设置",
                subtitle: "确认方式、资料分享和欢迎语",
                tint: NomeAddressPalette.purple
            )
        }
    }

    private func learnMoreButton() -> some View {
        NavigationLink {
            UserAddressLearnMore()
                .navigationTitle("公开联系方式和一次性链接")
                .modifier(ThemedBackground(grouped: true))
                .navigationBarTitleDisplayMode(.inline)
        } label: {
            settingsRow("info.circle", color: theme.colors.secondary) {
                Text("公开联系方式和一次性链接有什么不同？")
            }
        }
    }

    private var needsConfirmationBinding: Binding<Bool> {
        Binding(
            get: { !settings.autoAccept },
            set: { needsConfirmation in
                guard NomeActivationGate.require(.address) else { return }
                if showRealCoreErrorIfNeeded() { return }
                settings.autoAccept = !needsConfirmation
                if needsConfirmation {
                    settings.businessAddress = false
                    settings.autoAcceptIncognito = false
                    settings.autoReply = ""
                }
                saveAddressSettings(settings, $savedSettings)
            }
        )
    }

    private func showRealCoreErrorIfNeeded() -> Bool {
        guard let error = realChatCoreReadinessError() else { return false }
        alert = .runtimeError(title: "真实聊天 core 尚未可用", message: error)
        return true
    }
}

private struct NomeAddressTitleHeader: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("公开联系方式")
                .font(.system(size: 34, weight: .bold))
                .foregroundColor(NomeAddressPalette.navy)
                .fixedSize(horizontal: false, vertical: true)
            Text("让别人安全地向你发起联系请求，是否通过仍由你决定。")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.top, 4)
    }
}

private struct NomeAddressIntroCard: View {
    var body: some View {
        NomeAddressPanel {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: "globe")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(NomeAddressPalette.green)
                    .frame(width: 46, height: 46)
                    .background(Circle().fill(NomeAddressPalette.green.opacity(0.12)))
                Text("公开联系方式是你的可重复使用地址，方便别人向你发起联系请求。")
                    .font(.body)
                    .lineSpacing(3)
                    .foregroundColor(NomeAddressPalette.navy)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 0)
            }
        }
    }
}

private struct NomeAddressInlineBrandMark: View {
    var body: some View {
        Image("nome_header_logo")
            .resizable()
            .scaledToFit()
            .frame(width: 82, height: 34)
            .accessibilityLabel("Nome")
    }
}

private struct NomeAddressPageHeader: View {
    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Image(systemName: "globe")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: 38, height: 38)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(NomeAddressPalette.green)
                )

            VStack(alignment: .leading, spacing: 4) {
                Text("公开联系方式")
                    .font(.system(size: 27, weight: .bold))
                    .foregroundColor(NomeAddressPalette.navy)
                    .lineLimit(1)
                    .minimumScaleFactor(0.82)
                Text("让别人向你发起联系请求，是否通过仍由你决定。")
                    .font(.subheadline)
                    .lineSpacing(2)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(.top, 2)
        .padding(.bottom, 2)
    }
}

private struct NomeAddressPanel<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeAddressPalette.border, lineWidth: 1)
        )
        .buttonStyle(.plain)
    }
}

private struct NomePublicAddressCard: View {
    let link: String
    let copied: Bool
    let copyAction: () -> Void
    let shareAction: () -> Void

    var body: some View {
        NomeAddressPanel {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: "globe")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(NomeAddressPalette.green)
                    .frame(width: 46, height: 46)
                    .background(Circle().fill(NomeAddressPalette.green.opacity(0.12)))

                VStack(alignment: .leading, spacing: 8) {
                    Text("公开联系方式")
                        .font(.headline)
                        .foregroundColor(NomeAddressPalette.navy)
                    Text(link)
                        .font(.system(.callout, design: .monospaced))
                        .foregroundColor(NomeAddressPalette.navy.opacity(0.88))
                        .lineLimit(3)
                        .truncationMode(.middle)
                        .textSelection(.enabled)
                }
                Spacer(minLength: 0)
                Button(action: copyAction) {
                    Image(systemName: copied ? "checkmark" : "doc.on.doc")
                        .font(.system(size: 22, weight: .medium))
                        .foregroundColor(copied ? NomeAddressPalette.green : NomeAddressPalette.navy)
                        .frame(width: 38, height: 38)
                }
                .accessibilityLabel(copied ? "已复制" : "复制公开联系方式")
            }

            HStack(spacing: 12) {
                Button(action: copyAction) {
                    NomeAddressButtonLabel(
                        icon: copied ? "checkmark" : "doc.on.doc",
                        text: copied ? "已复制" : "复制",
                        tint: NomeAddressPalette.navy,
                        filled: false
                    )
                }

                Button(action: shareAction) {
                    NomeAddressButtonLabel(
                        icon: "square.and.arrow.up",
                        text: "分享",
                        tint: .white,
                        filled: true
                    )
                }
            }
            .padding(.top, 16)
        }
    }
}

private struct NomeAddressButtonLabel: View {
    let icon: String
    let text: String
    let tint: Color
    let filled: Bool

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
            Text(text)
                .fontWeight(.semibold)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .font(.body)
        .foregroundColor(tint)
        .frame(maxWidth: .infinity)
        .frame(height: 52)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(filled ? NomeAddressPalette.blue : Color.clear)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(filled ? Color.clear : NomeAddressPalette.border, lineWidth: 1)
        )
    }
}

private struct NomeConfirmationCard: View {
    @Binding var needsConfirmation: Bool

    var body: some View {
        NomeAddressPanel {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 5) {
                    Text("需要确认")
                        .font(.headline)
                        .foregroundColor(NomeAddressPalette.navy)
                    Text("收到联系请求时需要你确认")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 0)
                Toggle("", isOn: $needsConfirmation)
                    .labelsHidden()
                    .tint(NomeAddressPalette.green)
            }
        }
    }
}

private struct NomeAddressManagementCard<SettingsAction: View>: View {
    let replaceTitle: String
    let replaceSubtitle: String
    let replaceAction: () -> Void
    let settingsAction: SettingsAction
    let deleteAction: () -> Void

    init(
        replaceTitle: String,
        replaceSubtitle: String,
        replaceAction: @escaping () -> Void,
        @ViewBuilder settingsAction: () -> SettingsAction,
        deleteAction: @escaping () -> Void
    ) {
        self.replaceTitle = replaceTitle
        self.replaceSubtitle = replaceSubtitle
        self.replaceAction = replaceAction
        self.settingsAction = settingsAction()
        self.deleteAction = deleteAction
    }

    var body: some View {
        NomeAddressPanel {
            Text("地址管理")
                .font(.headline)
                .foregroundColor(NomeAddressPalette.navy)
                .padding(.bottom, 12)

            HStack(spacing: 12) {
                Button(action: replaceAction) {
                    NomeAddressMiniAction(
                        icon: "arrow.triangle.2.circlepath",
                        title: replaceTitle,
                        subtitle: replaceSubtitle,
                        tint: NomeAddressPalette.blue
                    )
                }

                Button(role: .destructive, action: deleteAction) {
                    NomeAddressMiniAction(
                        icon: "trash",
                        title: "关闭地址",
                        subtitle: "停用此地址",
                        tint: .red
                    )
                }
            }

            Divider().padding(.vertical, 12)
            settingsAction

            HStack(alignment: .top, spacing: 8) {
                Image(systemName: "info.circle")
                    .foregroundColor(.secondary)
                    .padding(.top, 1)
                Text("关闭或更换地址不会影响已建立的联系人。")
                    .font(.footnote)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.top, 12)
        }
    }
}

private struct NomeAddressMiniAction: View {
    let icon: String
    let title: String
    let subtitle: String
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(tint)
            Text(title)
                .font(.body.weight(.semibold))
                .foregroundColor(NomeAddressPalette.navy)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Text(subtitle)
                .font(.caption)
                .foregroundColor(.secondary)
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .frame(maxWidth: .infinity, minHeight: 116, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(tint.opacity(0.08))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(tint.opacity(0.18), lineWidth: 1)
        )
    }
}

private struct NomeAddressTrustNote: View {
    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: "shield.lefthalf.filled.badge.checkmark")
                .font(.system(size: 24, weight: .semibold))
                .foregroundColor(NomeAddressPalette.blue)
                .frame(width: 46, height: 46)
                .background(Circle().fill(NomeAddressPalette.blue.opacity(0.1)))
            Text("这个地址只用于建立联系，不用于后续消息投递。")
                .font(.body)
                .lineSpacing(3)
                .foregroundColor(NomeAddressPalette.navy)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(NomeAddressPalette.blue.opacity(0.06))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeAddressPalette.blue.opacity(0.14), lineWidth: 1)
        )
    }
}

private struct NomeAddressHero: View {
    let hasAddress: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: hasAddress ? "checkmark.shield.fill" : "globe.badge.plus")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 42, height: 42)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(hasAddress ? NomeAddressPalette.green : NomeAddressPalette.brandNavy)
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text("公开联系方式")
                        .font(.headline)
                        .foregroundColor(NomeAddressPalette.navy)
                    Text(hasAddress ? "已启用，可分享给更多人。" : "适合放在社媒、网站或名片上。")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer(minLength: 0)
            }

            Text("别人通过这个地址联系你时，你仍然可以先确认再连接。关闭或更换地址不会移除已有联系人。")
                .font(.subheadline)
                .lineSpacing(2)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 8) {
                NomeAddressPill(icon: "person.badge.shield.checkmark", text: "需要确认")
                NomeAddressPill(icon: "arrow.clockwise", text: "可更换")
                NomeAddressPill(icon: "person.2.slash", text: "不影响已有联系人")
            }
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeAddressPalette.border, lineWidth: 1)
        )
    }
}

private struct NomeAddressPill: View {
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
        .foregroundColor(NomeAddressPalette.navy)
        .padding(.horizontal, 8)
        .frame(height: 26)
        .background(Capsule().fill(NomeAddressPalette.green.opacity(0.1)))
    }
}

private struct NomeAddressActionLabel: View {
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
                    .foregroundColor(NomeAddressPalette.navy)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
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

func upgradeAndShareAddressAlert(progressIndicator: Binding<Bool>, shareAddress: (() -> Void)? = nil) {
    guard NomeActivationGate.require(.address) else { return }
    showAlert(
        NSLocalizedString("Upgrade address?", comment: "alert message"),
        message: NSLocalizedString("The address will be short, and your profile will be shared via the address.", comment: "alert message"),
        actions: {
            var actions = [UIAlertAction(title: NSLocalizedString("Upgrade", comment: "alert button"), style: .default) { _ in
                addShortLink(progressIndicator: progressIndicator, shareOnCompletion: shareAddress != nil)
            }]
            if let shareAddress {
                actions.append(UIAlertAction(title: NSLocalizedString("Share old address", comment: "alert button"), style: .default) { _ in
                    shareAddress()
                })
            }
            actions.append(cancelAlertAction)
            return actions
        }
    )
}

private func addShortLink(progressIndicator: Binding<Bool>, shareOnCompletion: Bool = false) {
    guard NomeActivationGate.require(.address) else { return }
    if showRealChatCoreUnavailableIfNeeded() { return }
    progressIndicator.wrappedValue = true
    Task {
        do {
            let userAddress = try await apiAddMyAddressShortLink()
            await MainActor.run {
                ChatModel.shared.userAddress = userAddress
                progressIndicator.wrappedValue = false
                if shareOnCompletion, let userAddress {
                    userAddress.shareAddress(short: true)
                }
            }
        } catch let error {
            logger.error("apiAddMyAddressShortLink: \(responseError(error))")
            showAlert("Error adding short link", message: responseError(error))
            await MainActor.run { progressIndicator.wrappedValue = false }
        }
    }
}


struct ToggleShortLinkHeader: View {
    @EnvironmentObject var theme: AppTheme
    @AppStorage(DEFAULT_DEVELOPER_TOOLS) private var developerTools = false
    let text: Text
    var link: CreatedConnLink
    @Binding var short: Bool

    var body: some View {
        if link.connShortLink == nil || !developerTools {
            text.foregroundColor(theme.colors.secondary)
        } else {
            HStack {
                text.foregroundColor(theme.colors.secondary)
                Spacer()
                Text(short ? "Full link" : "Short link")
                    .textCase(.none)
                    .foregroundColor(theme.colors.primary)
                    .onTapGesture { short.toggle() }
            }
        }
    }
}

struct AddressSettingsState: Equatable {
    var businessAddress = false
    var autoAccept = false
    var autoAcceptIncognito = false
    var autoReply = ""

    init() {}

    init(settings: AddressSettings) {
        self.businessAddress = settings.businessAddress
        self.autoAccept = settings.autoAccept != nil
        self.autoAcceptIncognito = settings.autoAccept?.acceptIncognito == true
        self.autoReply = settings.autoReply?.text ?? ""
    }

    var addressSettings: AddressSettings {
        AddressSettings(
            businessAddress: self.businessAddress,
            autoAccept: self.autoAccept ? AutoAccept(acceptIncognito: self.autoAcceptIncognito) : nil,
            autoReply: self.autoReply.isEmpty ? nil : MsgContent.text(self.autoReply)
        )
    }
}

private func showRealChatCoreUnavailableIfNeeded() -> Bool {
    guard let error = realChatCoreReadinessError() else { return false }
    showAlert("真实聊天 core 尚未可用", message: error)
    return true
}

private func setProfileAddress(_ progressIndicator: Binding<Bool>, _ on: Bool) {
    if showRealChatCoreUnavailableIfNeeded() { return }
    progressIndicator.wrappedValue = true
    Task {
        do {
            if let u = try await apiSetProfileAddress(on: on) {
                DispatchQueue.main.async {
                    ChatModel.shared.updateUser(u)
                }
            }
            await MainActor.run { progressIndicator.wrappedValue = false }
        } catch let error {
            logger.error("apiSetProfileAddress: \(responseError(error))")
            await MainActor.run { progressIndicator.wrappedValue = false }
        }
    }
}

struct UserAddressSettingsView: View {
    @Environment(\.dismiss) var dismiss: DismissAction
    @EnvironmentObject var theme: AppTheme
    @Binding var shareViaProfile: Bool
    @State private var settings = AddressSettingsState()
    @State private var savedSettings = AddressSettingsState()
    @State private var ignoreShareViaProfileChange = false
    @State private var progressIndicator = false

    var body: some View {
        ZStack {
            if let userAddress = ChatModel.shared.userAddress {
                userAddressSettingsView()
                    .onAppear {
                        settings = AddressSettingsState(settings: userAddress.addressSettings)
                        savedSettings = AddressSettingsState(settings: userAddress.addressSettings)
                    }
                    .onChange(of: settings.autoAccept) { autoAccept in
                        if !autoAccept {
                            settings.businessAddress = false
                            settings.autoReply = ""
                        }
                    }
                    .onDisappear {
                        if savedSettings != settings {
                            showAlert(
                                title: NSLocalizedString("Public contact address settings", comment: "alert title"),
                                message: NSLocalizedString("Settings were changed.", comment: "alert message"),
                                buttonTitle: NSLocalizedString("Save", comment: "alert button"),
                                buttonAction: { saveAddressSettings(settings, $savedSettings) },
                                cancelButton: true
                            )
                        }
                    }
            } else {
                Text(String("Error opening address settings"))
            }
            if progressIndicator {
                ProgressView().scaleEffect(2)
            }
        }
    }

    private func userAddressSettingsView() -> some View {
        List {
            Section {
                shareWithContactsButton()
                autoAcceptToggle().disabled(settings.businessAddress)
                if settings.autoAccept && !ChatModel.shared.addressShortLinkDataSet && !settings.businessAddress {
                    acceptIncognitoToggle()
                }
            }

            Section {
                messageEditor(placeholder: NSLocalizedString("Enter welcome message… (optional)", comment: "placeholder"), text: $settings.autoReply)
            } header: {
                Text("Welcome message")
                    .foregroundColor(theme.colors.secondary)
            }

            Section {
                saveAddressSettingsButton()
                    .disabled(settings == savedSettings)
            }
        }
    }

    private func shareWithContactsButton() -> some View {
        settingsRow("person", color: theme.colors.secondary) {
            Toggle("Share with Nome contacts", isOn: $shareViaProfile)
                .onChange(of: shareViaProfile) { on in
                    if ignoreShareViaProfileChange {
                        ignoreShareViaProfileChange = false
                    } else {
                        if showRealChatCoreUnavailableIfNeeded() {
                            ignoreShareViaProfileChange = true
                            shareViaProfile = !on
                            return
                        }
                        if on {
                            showAlert(
                                NSLocalizedString("Share address with Nome contacts?", comment: "alert title"),
                                message: NSLocalizedString("Profile update will be sent to your Nome contacts.", comment: "alert message"),
                                actions: {[
                                    UIAlertAction(
                                        title: NSLocalizedString("Cancel", comment: "alert action"),
                                        style: .cancel,
                                        handler: { _ in
                                            ignoreShareViaProfileChange = true
                                            shareViaProfile = !on
                                        }
                                    ),
                                    UIAlertAction(
                                        title: NSLocalizedString("Share", comment: "alert action"),
                                        style: .default,
                                        handler: { _ in
                                            setProfileAddress($progressIndicator, on)
                                        }
                                    )
                                ]}
                            )
                        } else {
                            showAlert(
                                NSLocalizedString("Stop sharing address?", comment: "alert title"),
                                message: NSLocalizedString("Profile update will be sent to your Nome contacts.", comment: "alert message"),
                                actions: {[
                                    UIAlertAction(
                                        title: NSLocalizedString("Cancel", comment: "alert action"),
                                        style: .cancel,
                                        handler: { _ in
                                            ignoreShareViaProfileChange = true
                                            shareViaProfile = !on
                                        }
                                    ),
                                    UIAlertAction(
                                        title: NSLocalizedString("Stop sharing", comment: "alert action"),
                                        style: .default,
                                        handler: { _ in
                                            setProfileAddress($progressIndicator, on)
                                        }
                                    )
                                ]}
                            )
                        }
                    }
                }
        }
    }

    private func autoAcceptToggle() -> some View {
        settingsRow("checkmark", color: theme.colors.secondary) {
            Toggle("Auto-accept", isOn: $settings.autoAccept)
                .onChange(of: settings.autoAccept) { _ in
                    if showRealChatCoreUnavailableIfNeeded() {
                        settings = savedSettings
                        return
                    }
                    saveAddressSettings(settings, $savedSettings)
                }
        }
    }

    private func acceptIncognitoToggle() -> some View {
        settingsRow(
            settings.autoAcceptIncognito ? "theatermasks.fill" : "theatermasks",
            color: settings.autoAcceptIncognito ? .indigo : theme.colors.secondary
        ) {
            Toggle("Accept incognito", isOn: $settings.autoAcceptIncognito)
        }
    }

    private func messageEditor(placeholder: String, text: Binding<String>) -> some View {
        ZStack {
            Group {
                if text.wrappedValue.isEmpty {
                    TextEditor(text: Binding.constant(placeholder))
                        .foregroundColor(theme.colors.secondary)
                        .disabled(true)
                }
                TextEditor(text: text)
            }
            .padding(.horizontal, -5)
            .padding(.top, -8)
            .frame(height: 90, alignment: .topLeading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func saveAddressSettingsButton() -> some View {
        Button {
            hideKeyboard()
            if showRealChatCoreUnavailableIfNeeded() {
                settings = savedSettings
                return
            }
            saveAddressSettings(settings, $savedSettings)
        } label: {
            Text("Save")
        }
    }
}

private func saveAddressSettings(_ settings: AddressSettingsState, _ savedSettings: Binding<AddressSettingsState>) {
    if showRealChatCoreUnavailableIfNeeded() { return }
    Task {
        do {
            if let address = try await apiSetUserAddressSettings(settings.addressSettings) {
                await MainActor.run {
                    ChatModel.shared.userAddress = address
                    savedSettings.wrappedValue = settings
                }
            }
        } catch let error {
            logger.error("apiSetUserAddressSettings error: \(responseError(error))")
        }
    }
}

struct UserAddressView_Previews: PreviewProvider {
    static var previews: some View {
        let chatModel = ChatModel()
        chatModel.userAddress = UserContactLink(CreatedConnLink(connFullLink: "https://simplex.chat/contact#/?v=1&smp=smp%3A%2F%2FPQUV2eL0t7OStZOoAsPEV2QYWt4-xilbakvGUGOItUo%3D%40smp6.simplex.im%2FK1rslx-m5bpXVIdMZg9NLUZ_8JBm8xTt%23MCowBQYDK2VuAyEALDeVe-sG8mRY22LsXlPgiwTNs9dbiLrNuA7f3ZMAJ2w%3D", connShortLink: nil))

        return Group {
            UserAddressView()
                .environmentObject(chatModel)
            UserAddressView()
                .environmentObject(ChatModel())
        }
    }
}
