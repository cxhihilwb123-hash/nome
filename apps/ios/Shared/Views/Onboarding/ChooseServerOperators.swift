//
//  ChooseServerOperators.swift
//  SimpleX (iOS)
//
//  Created by spaced4ndy on 31.10.2024.
//  Copyright © 2024 SimpleX Chat. All rights reserved.
//
// Spec: spec/client/navigation.md

import SwiftUI
import SimpleXChat

let conditionsURL = URL(string: "https://github.com/simplex-chat/simplex-chat/blob/stable/PRIVACY.md")!

struct OnboardingButtonStyle: ButtonStyle {
    @EnvironmentObject var theme: AppTheme
    var isDisabled: Bool = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 17, weight: .semibold))
            .padding()
            .frame(maxWidth: .infinity)
            .background(
                isDisabled
                ? (
                    theme.colors.isLight
                    ? .gray.opacity(0.17)
                    : .gray.opacity(0.27)
                )
                : NomeOnboardingPalette.green
            )
            .foregroundColor(
                isDisabled
                ? (
                    theme.colors.isLight
                    ? .gray.opacity(0.4)
                    : .white.opacity(0.2)
                )
                : .white
            )
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
    }
}

// Keep this scoped to consecutive full-screen transitions whose primary
// buttons occupy the same position. The system tap recognizer treats a rapid
// double tap as one activation so the second touch cannot land on the next
// screen's primary action. Ordinary onboarding actions use OnboardingButtonStyle.
struct OnboardingTransitionButtonStyle: PrimitiveButtonStyle {
    @EnvironmentObject var theme: AppTheme
    var isDisabled: Bool = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 17, weight: .semibold))
            .padding()
            .frame(maxWidth: .infinity)
            .background(
                isDisabled
                ? (
                    theme.colors.isLight
                    ? .gray.opacity(0.17)
                    : .gray.opacity(0.27)
                )
                : NomeOnboardingPalette.green
            )
            .foregroundColor(
                isDisabled
                ? (
                    theme.colors.isLight
                    ? .gray.opacity(0.4)
                    : .white.opacity(0.2)
                )
                : .white
            )
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            .contentShape(Rectangle())
            .gesture(
                TapGesture(count: 2)
                    .exclusively(before: TapGesture(count: 1))
                    .onEnded { _ in
                        if !isDisabled {
                            configuration.trigger()
                        }
                    }
            )
    }
}

struct OnboardingConditionsView: View {
    @EnvironmentObject var theme: AppTheme
    @Environment(\.colorScheme) var colorScheme: ColorScheme
    @State private var showConditionsSheet = false
    @State private var acceptanceInProgress = false
    var selectedOperatorIds: Set<Int64>

    var body: some View {
        GeometryReader { g in
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    NomeOnboardingLogoHeader()
                        .padding(.top, 10)

                    NomeOnboardingHeroCard(
                        symbol: "checkmark.seal.fill",
                        title: "确认网络使用规则",
                        subtitle: "Nome 会减少连接关系暴露，但公开群组和服务器仍需要基本使用规则。确认后就会进入主界面。",
                        tint: NomeOnboardingPalette.green,
                        pills: [
                            ("lock.shield", "保护隐私"),
                            ("person.2", "尊重他人"),
                            ("checkmark", "可继续")
                        ]
                    )

                    VStack(spacing: 10) {
                        NomeOnboardingFeatureRow(
                            icon: "server.rack",
                            title: "运营商承诺",
                            text: "独立运行、尽量减少元数据、使用可验证的开源代码。",
                            tint: NomeOnboardingPalette.green
                        )
                        NomeOnboardingFeatureRow(
                            icon: "person.2.badge.gearshape",
                            title: "你的承诺",
                            text: "公开群组里只发布合法内容，尊重其他用户，不发送垃圾信息。",
                            tint: NomeOnboardingPalette.blue
                        )
                        NomeOnboardingFeatureRow(
                            icon: "gearshape",
                            title: "以后仍可调整",
                            text: NomeServerConfiguration.isConfigured
                                ? "Nome 官方消息与文件服务器已自动配置，进入 app 后可查看连接状态。"
                                : selectedOperatorIds.isEmpty
                                    ? "当前使用默认网络设置，进入 app 后可在设置里更改。"
                                    : "当前已选择 \(selectedOperatorIds.count) 个运营商，进入 app 后仍可更改。",
                            tint: NomeOnboardingPalette.purple
                        )
                    }

                    Button {
                        showConditionsSheet = true
                    } label: {
                        Label("查看隐私政策和使用条件", systemImage: "doc.text")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                    .foregroundColor(NomeOnboardingPalette.navy)
                    .padding(.top, 2)

                    Spacer(minLength: 0)

                    acceptButton()
                        .padding(.bottom, g.safeAreaInsets.bottom == 0 ? 20 : 0)
                }
                .padding(.horizontal, 25)
                .padding(.top, 8)
                .padding(.bottom, 25)
                .frame(minHeight: g.size.height)
            }
        }
        .frame(maxHeight: .infinity)
        .navigationBarHidden(true)
        .sheet(isPresented: $showConditionsSheet) {
            NavigationView {
                VStack {
                    ConditionsTextView()
                        .padding()
                    acceptButton()
                        .padding(.horizontal, 25)
                        .padding(.bottom, 20)
                }
                .navigationTitle("使用条件")
                .navigationBarTitleDisplayMode(.large)
                .toolbar { ToolbarItem(placement: .navigationBarTrailing, content: conditionsLinkButton) }
                .modifier(ThemedBackground(grouped: true))
            }
        }
    }

    @ViewBuilder
    private func heroImage() -> some View {
        #if SIMPLEX_ASSETS
        Image(colorScheme == .light ? "network-commitments" : "network-commitments-light")
            .resizable()
            .scaledToFit()
        #else
        ZStack {
            let gp = OnboardingCardView.gradientPoints(aspectRatio: 1.5, scale: colorScheme == .light ? 1.2 : 1.5)
            LinearGradient(
                stops: colorScheme == .light ? OnboardingCardView.lightStops : OnboardingCardView.darkStops,
                startPoint: gp.start,
                endPoint: gp.end
            )
            Image(systemName: "checkmark.shield")
                .font(.system(size: 72))
                .foregroundColor(theme.colors.primary)
        }
        .aspectRatio(1.5, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .padding(.horizontal, 25)
        #endif
    }

    private func acceptButton() -> some View {
        Button {
            acceptConditionsAndComplete()
        } label: {
            HStack(spacing: 8) {
                if acceptanceInProgress {
                    ProgressView()
                        .tint(.white)
                }
                Text(acceptanceInProgress ? "正在进入…" : "同意并进入 Nome")
            }
        }
        .buttonStyle(OnboardingButtonStyle(isDisabled: acceptanceInProgress))
        .disabled(acceptanceInProgress)
    }

    private func acceptConditionsAndComplete() {
        guard !acceptanceInProgress else { return }
        acceptanceInProgress = true
        Task {
            if NomeServerConfiguration.isConfigured {
                let applied = await applyNomeOfficialServersIfConfigured()
                await MainActor.run {
                    if applied {
                        completeOnboarding()
                    } else {
                        acceptanceInProgress = false
                        showAlert(
                            NSLocalizedString("无法启用 Nome 官方服务器", comment: "alert title"),
                            message: NSLocalizedString("请检查网络连接，然后重试。Nome 不会改用其他服务器。", comment: "alert message")
                        )
                    }
                }
                return
            }
            if selectedOperatorIds.isEmpty {
                await MainActor.run { completeOnboarding() }
                return
            }
            do {
                let conditionsId = ChatModel.shared.conditions.currentConditions.conditionsId
                let r = try await acceptConditions(conditionsId: conditionsId, operatorIds: Array(selectedOperatorIds))
                await MainActor.run {
                    ChatModel.shared.conditions = r
                }
                if let enabledOps = enabledOperators(r.serverOperators) {
                    let r2 = try await setServerOperators(operators: enabledOps)
                    await MainActor.run {
                        ChatModel.shared.conditions = r2
                        completeOnboarding()
                    }
                } else {
                    await MainActor.run {
                        completeOnboarding()
                    }
                }
            } catch let error {
                await MainActor.run {
                    acceptanceInProgress = false
                    showAlert(
                        NSLocalizedString("Error accepting conditions", comment: "alert title"),
                        message: responseError(error)
                    )
                }
            }
        }
    }

    private func completeOnboarding() {
        let m = ChatModel.shared
        onboardingStageDefault.set(.onboardingComplete)
        m.onboardingStage = .onboardingComplete
    }

    private func enabledOperators(_ operators: [ServerOperator]) -> [ServerOperator]? {
        var ops = operators
        if !ops.isEmpty {
            for i in 0..<ops.count {
                var op = ops[i]
                op.enabled = selectedOperatorIds.contains(op.operatorId)
                ops[i] = op
            }
            let haveSMPStorage = ops.contains(where: { $0.enabled && $0.smpRoles.storage })
            let haveSMPProxy = ops.contains(where: { $0.enabled && $0.smpRoles.proxy })
            let haveXFTPStorage = ops.contains(where: { $0.enabled && $0.xftpRoles.storage })
            let haveXFTPProxy = ops.contains(where: { $0.enabled && $0.xftpRoles.proxy })
            if haveSMPStorage && haveSMPProxy && haveXFTPStorage && haveXFTPProxy {
                return ops
            } else if let firstEnabledIndex = ops.firstIndex(where: { $0.enabled }) {
                var op = ops[firstEnabledIndex]
                if !haveSMPStorage { op.smpRoles.storage = true }
                if !haveSMPProxy { op.smpRoles.proxy = true }
                if !haveXFTPStorage { op.xftpRoles.storage = true }
                if !haveXFTPProxy { op.xftpRoles.proxy = true }
                ops[firstEnabledIndex] = op
                return ops
            } else {
                return nil
            }
        } else {
            return nil
        }
    }
}

private enum ChooseServerOperatorsSheet: Identifiable {
    case showInfo

    var id: String {
        switch self {
        case .showInfo: return "showInfo"
        }
    }
}

struct ChooseServerOperators: View {
    @Environment(\.dismiss) var dismiss: DismissAction
    @Environment(\.colorScheme) var colorScheme: ColorScheme
    @EnvironmentObject var theme: AppTheme
    var serverOperators: [ServerOperator]
    @Binding var selectedOperatorIds: Set<Int64>
    @State private var sheetItem: ChooseServerOperatorsSheet? = nil

    var body: some View {
        GeometryReader { g in
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("消息服务器")
                        .font(.largeTitle)
                        .bold()
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 25)

                    infoText()
                        .frame(maxWidth: .infinity, alignment: .center)

                    Spacer()
                    
                    if serverOperators.isEmpty {
                        NomeOnboardingFeatureRow(
                            icon: "server.rack",
                            title: "使用默认服务器设置",
                            text: "当前没有可选运营商列表。Nome 会继续使用默认兼容设置，之后可在 `服务器与 Tor` 中调整。",
                            tint: NomeOnboardingPalette.green
                        )
                    } else {
                        ForEach(serverOperators) { srvOperator in
                            operatorCheckView(srvOperator)
                        }
                    }
                    VStack {
                        Text("Nome 已配置默认网络，也可以启用其他运营商。").padding(.bottom, 8)
                        Text("这些设置之后可在 `服务器与 Tor` 中调整。")
                    }
                    .font(.footnote)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.horizontal, 16)
                    
                    Spacer()

                    VStack(spacing: 8) {
                        setOperatorsButton()
                        onboardingButtonPlaceholder()
                    }
                }
                .frame(minHeight: g.size.height)
            }
            .sheet(item: $sheetItem) { item in
                switch item {
                case .showInfo:
                    ChooseServerOperatorsInfoView()
                }
            }
            .frame(maxHeight: .infinity, alignment: .top)
        }
        .frame(maxHeight: .infinity, alignment: .top)
        .padding(25)
        .interactiveDismissDisabled(!serverOperators.isEmpty && selectedOperatorIds.isEmpty)
    }

    private func infoText() -> some View {
        Button {
            sheetItem = .showInfo
        } label: {
            Label("为什么这有助于隐私", systemImage: "info.circle")
                .font(.headline)
        }
    }

    private func operatorCheckView(_ serverOperator: ServerOperator) -> some View {
        let checked = selectedOperatorIds.contains(serverOperator.operatorId)
        let icon = checked ? "checkmark.circle.fill" : "circle"
        let iconColor = checked ? theme.colors.primary : Color(uiColor: .tertiaryLabel).asAnotherColorFromSecondary(theme)
        return HStack(spacing: 10) {
            Image(serverOperator.largeLogo(colorScheme))
                .resizable()
                .scaledToFit()
                .frame(height: 48)
            Spacer()
            Image(systemName: icon)
                .resizable()
                .scaledToFit()
                .frame(width: 26, height: 26)
                .foregroundColor(iconColor)
        }
        .background(theme.colors.background)
        .padding()
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(Color(uiColor: .secondarySystemFill), lineWidth: 2)
        )
        .padding(.horizontal, 2)
        .onTapGesture {
            if checked {
                selectedOperatorIds.remove(serverOperator.operatorId)
            } else {
                selectedOperatorIds.insert(serverOperator.operatorId)
            }
        }
    }

    private func setOperatorsButton() -> some View {
        Button {
            dismiss()
        } label: {
            Text("完成")
        }
        .buttonStyle(OnboardingButtonStyle(isDisabled: !serverOperators.isEmpty && selectedOperatorIds.isEmpty))
        .disabled(!serverOperators.isEmpty && selectedOperatorIds.isEmpty)
    }
}

let operatorsPostLink = URL(string: "https://simplex.chat/blog/20241125-servers-operated-by-flux-true-privacy-and-decentralization-for-all-users.html")!

struct ChooseServerOperatorsInfoView: View {
    @Environment(\.colorScheme) var colorScheme: ColorScheme
    @EnvironmentObject var theme: AppTheme

    var body: some View {
        NavigationView {
            List {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Nome 可以在不同会话中使用不同运营商，减少单一服务器看到的关系信息。")
                    Text("启用多个运营商时，任何单个运营商都更难知道谁在和谁通信。")
                    Text("这是网络层保护，不等同于匿名资料或 Tor。")
                }
                .fixedSize(horizontal: false, vertical: true)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
                .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
                .padding(.top)

                Section {
                    ForEach(ChatModel.shared.conditions.serverOperators) { op in
                        operatorInfoNavLinkView(op)
                    }
                } header: {
                    Text("About operators")
                        .foregroundColor(theme.colors.secondary)
                }
            }
            .navigationTitle("消息服务器")
            .navigationBarTitleDisplayMode(.large)
            .modifier(ThemedBackground(grouped: true))
        }
    }

    private func operatorInfoNavLinkView(_ op: ServerOperator) -> some View {
        NavigationLink() {
            OperatorInfoView(serverOperator: op)
                .navigationBarTitle("网络运营商")
                .modifier(ThemedBackground(grouped: true))
                .navigationBarTitleDisplayMode(.large)
        } label: {
            HStack {
                Image(op.logo(colorScheme))
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                Text(op.tradeName)
            }
        }
    }
}

#Preview {
    OnboardingConditionsView(selectedOperatorIds: [])
}
