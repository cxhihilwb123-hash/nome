//
//  HowItWorks.swift
//  SimpleX (iOS)
//
//  Created by Evgeny on 08/05/2022.
//  Copyright © 2022 SimpleX Chat. All rights reserved.
//
// Spec: spec/client/navigation.md

import SwiftUI

struct OldHowItWorks: View {
    @Environment(\.dismiss) var dismiss: DismissAction
    @EnvironmentObject var m: ChatModel
    var onboarding: Bool
    @Binding var createProfileNavLinkActive: Bool

    var body: some View {
        VStack(alignment: .leading) {
            Text("How Nome protects your privacy")
                .font(.largeTitle)
                .bold()
                .padding(.vertical)
            ScrollView {
                VStack(alignment: .leading) {
                    Group {
                        Text("To protect your privacy, Nome uses a separate connection ID for each contact.")
                        Text("Only client devices store user profiles, contacts, groups, and messages.")
                        Text("All messages and files are sent **end-to-end encrypted**, with post-quantum security in direct messages.")
                        if !onboarding {
                            ExternalLink("Read more in our GitHub repository.", destination: URL(string: "https://github.com/simplex-chat/simplex-chat#readme")!)
                        }
                    }
                    .padding(.bottom)
                }
            }

            Spacer()

            if onboarding {
                VStack(spacing: 10) {
                    createFirstProfileButton()
                    onboardingButtonPlaceholder()
                }
            }
        }
        .lineLimit(10)
        .padding(onboarding ? 25 : 16)
        .frame(maxHeight: .infinity, alignment: .top)
        .modifier(ThemedBackground())
    }

    private func createFirstProfileButton() -> some View {
        Button {
            dismiss()
            createProfileNavLinkActive = true
        } label: {
            Text("Create your profile")
        }
        .buttonStyle(OnboardingButtonStyle(isDisabled: false))
    }
}

struct WhySimpleX: View {
    @Environment(\.dismiss) var dismiss: DismissAction
    @EnvironmentObject var m: ChatModel
    var onboarding: Bool
    @Binding var createProfileNavLinkActive: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    NomeOnboardingLogoHeader()
                        .padding(.top, 4)

                    NomeOnboardingHeroCard(
                        symbol: "checkmark.shield.fill",
                        title: "保护的是关系，而不只是消息",
                        subtitle: "普通聊天软件通常先给你一个账号，再把联系人关系放进服务器。Nome 的目标是让连接本身也更少暴露。",
                        tint: NomeOnboardingPalette.green,
                        pills: [
                            ("person.crop.circle.badge.xmark", "无公开账号"),
                            ("lock", "端到端加密"),
                            ("server.rack", "服务器少知道")
                        ]
                    )

                    VStack(spacing: 10) {
                        NomeOnboardingFeatureRow(
                            icon: "link.badge.plus",
                            title: "每个朋友用不同连接",
                            text: "建立联系靠邀请链接或二维码，不靠一个永久公开 ID。",
                            tint: NomeOnboardingPalette.green
                        )
                        NomeOnboardingFeatureRow(
                            icon: "externaldrive",
                            title: "你的资料在本机",
                            text: "身份、联系人和消息不作为普通云账号同步，备份与迁移需要你主动操作。",
                            tint: NomeOnboardingPalette.blue
                        )
                        NomeOnboardingFeatureRow(
                            icon: "network.badge.shield.half.filled",
                            title: "网络隐私单独设置",
                            text: "匿名资料和 Tor 不是一回事。身份显示、服务器和网络路径会分开解释。",
                            tint: NomeOnboardingPalette.purple
                        )
                    }
                }
            }
            .padding(.bottom, 16)

            Spacer()

            if onboarding {
                createFirstProfileButton()
            }
        }
        .padding(onboarding ? 25 : 16)
        .frame(maxHeight: .infinity, alignment: .top)
        .modifier(ThemedBackground())
    }

    private func createFirstProfileButton() -> some View {
        Button {
            dismiss()
            createProfileNavLinkActive = true
        } label: {
            Text("开始使用 Nome")
        }
        .buttonStyle(OnboardingButtonStyle(isDisabled: false))
    }
}

struct WhySimpleX_Previews: PreviewProvider {
    static var previews: some View {
        WhySimpleX(
            onboarding: true,
            createProfileNavLinkActive: Binding.constant(false)
        )
    }
}
