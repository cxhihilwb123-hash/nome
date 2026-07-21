//
//  YourNetwork.swift
//  SimpleX (iOS)
//
//  Created by Evgeny on 22/04/2026.
//  Copyright © 2026 SimpleX Chat. All rights reserved.
//

import SwiftUI
import SimpleXChat

private enum YourNetworkSheet: Identifiable {
    case configureOperators
    case configureNotifications

    var id: String {
        switch self {
        case .configureOperators: return "configureOperators"
        case .configureNotifications: return "configureNotifications"
        }
    }
}

struct YourNetworkView: View {
    @EnvironmentObject var theme: AppTheme
    @Environment(\.colorScheme) var colorScheme: ColorScheme
    @State private var serverOperators: [ServerOperator] = []
    @State private var selectedOperatorIds = Set<Int64>()
    @State private var notificationMode: NotificationsMode = .instant
    @State private var sheetItem: YourNetworkSheet? = nil
    @State private var nextStepNavLinkActive = false
    @State private var justOpened = true

    var body: some View {
        GeometryReader { g in
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    NomeOnboardingLogoHeader()
                        .padding(.top, 10)

                    NomeOnboardingHeroCard(
                        symbol: "network.badge.shield.half.filled",
                        title: "设置网络与通知",
                        subtitle: "Nome 会通过消息服务器转发加密消息。你可以选择运营商和通知方式，之后也能在设置里调整。",
                        tint: NomeOnboardingPalette.green,
                        pills: [
                            ("server.rack", "可选运营商"),
                            ("bell.badge", "通知模式"),
                            ("gearshape", "之后可改")
                        ]
                    )

                    VStack(spacing: 10) {
                        configureRoutersButton()
                        configureNotificationsButton()
                    }

                    NomeOnboardingFeatureRow(
                        icon: "info.circle",
                        title: "身份隐私和网络隐私分开",
                        text: "匿名资料控制别人看到的身份；服务器和 Tor 控制网络路径。两者不是同一个开关。",
                        tint: NomeOnboardingPalette.purple
                    )

                    Spacer(minLength: 0)

                    continueButton()
                        .padding(.bottom, g.safeAreaInsets.bottom == 0 ? 20 : 0)
                }
                .padding(.horizontal, 25)
                .padding(.top, 8)
                .padding(.bottom, 20)
                .frame(minHeight: g.size.height)
            }
        }
        .onAppear {
            if justOpened {
                serverOperators = ChatModel.shared.conditions.serverOperators
                selectedOperatorIds = Set(serverOperators.filter { $0.enabled }.map { $0.operatorId })
                justOpened = false
            }
        }
        .sheet(item: $sheetItem) { item in
            switch item {
            case .configureOperators:
                ChooseServerOperators(serverOperators: serverOperators, selectedOperatorIds: $selectedOperatorIds)
                    .modifier(ThemedBackground())
            case .configureNotifications:
                SetNotificationsMode(notificationMode: $notificationMode)
                    .modifier(ThemedBackground())
            }
        }
        .frame(maxHeight: .infinity)
        .navigationBarHidden(true)
    }

    private func configureRoutersButton() -> some View {
        Button {
            sheetItem = .configureOperators
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "server.rack")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(NomeOnboardingPalette.green)
                    .frame(width: 36, height: 36)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeOnboardingPalette.green.opacity(0.12))
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text("消息服务器")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(NomeOnboardingPalette.navy)
                    Text(selectedOperatorIds.isEmpty ? "使用默认设置" : "已选择 \(selectedOperatorIds.count) 个运营商")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer(minLength: 0)

                HStack(spacing: -4) {
                    ForEach(serverOperators.prefix(3).reversed()) { op in
                        Image(op.logo(colorScheme))
                            .resizable()
                            .scaledToFit()
                            .frame(width: 22, height: 22)
                            .padding(4)
                            .background(Circle().fill(Color(uiColor: .systemBackground)))
                            .grayscale(selectedOperatorIds.contains(op.operatorId) ? 0.0 : 1.0)
                    }
                }

                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(.secondary)
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(NomeOnboardingPalette.surface)
            )
        }
        .buttonStyle(.plain)
    }

    private func configureNotificationsButton() -> some View {
        Button {
            sheetItem = .configureNotifications
        } label: {
            HStack(spacing: 12) {
                Image(systemName: notificationMode.icon)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(NomeOnboardingPalette.blue)
                    .frame(width: 36, height: 36)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeOnboardingPalette.blue.opacity(0.12))
                    )

                VStack(alignment: .leading, spacing: 4) {
                    Text("通知方式")
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(NomeOnboardingPalette.navy)
                    Text("选择提醒强度和隐私级别")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundColor(.secondary)
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(NomeOnboardingPalette.surface)
            )
        }
        .buttonStyle(.plain)
    }

    private func continueButton() -> some View {
        ZStack {
            Button {
                applyNotificationMode()
                onboardingStageDefault.set(.step4_NetworkCommitments)
                nextStepNavLinkActive = true
            } label: {
                Text("继续")
            }
            .buttonStyle(OnboardingButtonStyle())

            NavigationLink(isActive: $nextStepNavLinkActive) {
                OnboardingConditionsView(selectedOperatorIds: selectedOperatorIds)
                    .navigationBarBackButtonHidden(true)
                    .modifier(ThemedBackground())
            } label: {
                EmptyView()
            }
            .frame(width: 1, height: 1)
            .hidden()
        }
    }

    private func applyNotificationMode() {
        let m = ChatModel.shared
        if let token = m.deviceToken {
            switch notificationMode {
            case .off:
                m.tokenStatus = .new
                m.notificationMode = .off
            default:
                Task {
                    do {
                        let status = try await apiRegisterToken(token: token, notificationMode: notificationMode)
                        await MainActor.run {
                            m.tokenStatus = status
                            m.notificationMode = notificationMode
                        }
                    } catch let error {
                        let a = getErrorAlert(error, "Error enabling notifications")
                        AlertManager.shared.showAlertMsg(
                            title: a.title,
                            message: a.message
                        )
                    }
                }
            }
        }
    }
}
