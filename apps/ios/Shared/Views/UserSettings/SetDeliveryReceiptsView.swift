//
//  SetDeliveryReceiptsView.swift
//  SimpleX (iOS)
//
//  Created by Evgeny on 12/07/2023.
//  Copyright © 2023 SimpleX Chat. All rights reserved.
//

import SwiftUI
import SimpleXChat

struct SetDeliveryReceiptsView: View {
    @EnvironmentObject var m: ChatModel
    @EnvironmentObject var theme: AppTheme

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            NomeOnboardingLogoHeader()
                .padding(.top, 22)

            NomeOnboardingHeroCard(
                symbol: "checkmark.message.fill",
                title: "是否发送送达回执",
                subtitle: "送达回执会告诉联系人消息已送到你的设备。它不是已读状态，也不会公开你的在线状态。",
                tint: NomeOnboardingPalette.blue,
                pills: [
                    ("checkmark.circle", "仅表示送达"),
                    ("eye.slash", "不是已读"),
                    ("gearshape", "可稍后修改")
                ]
            )

            VStack(spacing: 10) {
                NomeOnboardingFeatureRow(
                    icon: "person.2",
                    title: "对现有联系人生效",
                    text: m.users.count > 1
                        ? "启用后会应用到所有可见身份里的联系人。"
                        : "启用后会应用到所有联系人。",
                    tint: NomeOnboardingPalette.green
                )
                NomeOnboardingFeatureRow(
                    icon: "lock.shield",
                    title: "隐私上更透明",
                    text: "如果你不想发送送达状态，可以先不启用，之后在 `隐私与安全` 里调整。",
                    tint: NomeOnboardingPalette.purple
                )
            }

            Spacer(minLength: 0)

            Button("启用送达回执") {
                enableDeliveryReceipts()
            }
            .buttonStyle(OnboardingButtonStyle())

            Button {
                AlertManager.shared.showAlert(Alert(
                    title: Text("已暂不启用送达回执"),
                    message: Text("之后可在 `隐私与安全` 中开启。"),
                    primaryButton: .default(Text("不再提醒")) {
                        m.setDeliveryReceipts = false
                        privacyDeliveryReceiptsSet.set(true)
                    },
                    secondaryButton: .default(Text("好的")) {
                        m.setDeliveryReceipts = false
                    }
                ))
            } label: {
                HStack {
                    Text("暂不启用")
                    Image(systemName: "chevron.right")
                }
                .font(.subheadline.weight(.semibold))
                .foregroundColor(NomeOnboardingPalette.navy)
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 25)
        .padding(.bottom, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.shared.colors.background)
    }

    private func enableDeliveryReceipts() {
        Task {
            do {
                if let currentUser = m.currentUser {
                    try await apiSetAllContactReceipts(enable: true)
                    await MainActor.run {
                        var updatedUser = currentUser
                        updatedUser.sendRcptsContacts = true
                        m.updateUser(updatedUser)
                        m.setDeliveryReceipts = false
                        privacyDeliveryReceiptsSet.set(true)
                    }
                    do {
                        let users = try await listUsersAsync()
                        await MainActor.run { m.users = users }
                    } catch let error {
                        logger.debug("listUsers error: \(responseError(error))")
                    }
                }
            } catch let error {
                AlertManager.shared.showAlert(Alert(
                    title: Text("启用送达回执失败"),
                    message: Text("Error: \(responseError(error))")
                ))
                await MainActor.run {
                    m.setDeliveryReceipts = false
                }
            }
        }
    }
}

struct SetDeliveryReceiptsView_Previews: PreviewProvider {
    static var previews: some View {
        SetDeliveryReceiptsView()
    }
}
