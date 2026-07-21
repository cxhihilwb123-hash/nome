//
//  SimpleXInfo.swift
//  SimpleX (iOS)
//
//  Created by Evgeny on 07/05/2022.
//  Copyright © 2022 SimpleX Chat. All rights reserved.
//
// Spec: spec/client/navigation.md

import SwiftUI
import SimpleXChat

enum NomeOnboardingPalette {
    static let navy = Color(uiColor: UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor.label.resolvedColor(with: traits)
            : UIColor(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0, alpha: 1)
    })
    static let green = Color(red: 22.0 / 255.0, green: 174.0 / 255.0, blue: 102.0 / 255.0)
    static let blue = Color(red: 39.0 / 255.0, green: 107.0 / 255.0, blue: 255.0 / 255.0)
    static let purple = Color(red: 116.0 / 255.0, green: 89.0 / 255.0, blue: 238.0 / 255.0)
    static let surface = Color(uiColor: .secondarySystemGroupedBackground)
    static let border = Color.black.opacity(0.06)
}

struct NomeOnboardingLogoHeader: View {
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        HStack {
            Image(colorScheme == .light ? "logo" : "logo-light")
                .resizable()
                .scaledToFit()
                .frame(width: 122, height: 40, alignment: .leading)
                .accessibilityHidden(true)
            Spacer(minLength: 0)
        }
    }
}

struct NomeOnboardingHeroCard: View {
    let symbol: String
    let title: String
    let subtitle: String
    let tint: Color
    var pills: [(String, String)] = []

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                Image("icon-light")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 76, height: 76)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                Spacer(minLength: 0)

                Image(systemName: symbol)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundColor(tint)
                    .frame(width: 46, height: 46)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(tint.opacity(0.12))
                    )
            }

            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.system(size: 31, weight: .bold))
                    .foregroundColor(NomeOnboardingPalette.navy)
                    .fixedSize(horizontal: false, vertical: true)
                Text(subtitle)
                    .font(.subheadline)
                    .lineSpacing(2)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            if !pills.isEmpty {
                HStack(spacing: 8) {
                    ForEach(pills, id: \.0) { pill in
                        NomeOnboardingPill(icon: pill.0, text: pill.1, tint: tint)
                    }
                }
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(uiColor: .systemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(NomeOnboardingPalette.border, lineWidth: 1)
        )
    }
}

struct NomeOnboardingFeatureRow: View {
    let icon: String
    let title: String
    let text: String
    let tint: Color

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
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
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(NomeOnboardingPalette.navy)
                Text(text)
                    .font(.caption)
                    .lineSpacing(1)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(NomeOnboardingPalette.surface)
        )
    }
}

struct NomeOnboardingPill: View {
    let icon: String
    let text: String
    let tint: Color

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
            Text(text)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .font(.caption2.weight(.medium))
        .foregroundColor(NomeOnboardingPalette.navy)
        .padding(.horizontal, 8)
        .frame(height: 28)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(tint.opacity(0.1))
        )
    }
}

struct SimpleXInfo: View {
    @EnvironmentObject var m: ChatModel
    @EnvironmentObject var theme: AppTheme
    @Environment(\.colorScheme) var colorScheme: ColorScheme
    @State private var showWhyBuilt = false
    @State private var createProfileNavLinkActive = false
    var onboarding: Bool

    var body: some View {
        GeometryReader { g in
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    NomeOnboardingLogoHeader()
                        .padding(.top, 18)

                    NomeOnboardingHeroCard(
                        symbol: "lock.shield.fill",
                        title: "私密连接，简单开始",
                        subtitle: "Nome 不需要手机号、公开用户名或云端账号。先创建本机身份，然后用一次性链接连接朋友。",
                        tint: NomeOnboardingPalette.green,
                        pills: [
                            ("phone.slash", "无需手机号"),
                            ("link", "一次性链接"),
                            ("person.crop.circle", "本地身份")
                        ]
                    )

                    VStack(spacing: 10) {
                        NomeOnboardingFeatureRow(
                            icon: "number",
                            title: "没有公开用户 ID",
                            text: "别人不能通过一个全局用户名搜索到你。",
                            tint: NomeOnboardingPalette.blue
                        )
                        NomeOnboardingFeatureRow(
                            icon: "qrcode.viewfinder",
                            title: "用邀请建立关系",
                            text: "给朋友发一次性链接或二维码，连接成功后失效。",
                            tint: NomeOnboardingPalette.green
                        )
                        NomeOnboardingFeatureRow(
                            icon: "externaldrive.badge.checkmark",
                            title: "资料保存在设备上",
                            text: "身份、联系人和消息属于你的设备，备份和迁移由你控制。",
                            tint: NomeOnboardingPalette.purple
                        )
                    }

                    Spacer(minLength: 0)

                    if onboarding {
                        createFirstProfileButton()
                            .padding(.top, 4)

                        Button {
                            showWhyBuilt = true
                        } label: {
                            Label("Nome 怎样保护你", systemImage: "info.circle")
                                .font(.subheadline.weight(.semibold))
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.plain)
                        .foregroundColor(NomeOnboardingPalette.navy)
                    }
                }
                .padding(.horizontal, 25)
                .padding(.bottom, 24)
                .frame(minHeight: g.size.height)
            }
            .sheet(isPresented: Binding(
                get: { m.migrationState != nil && !createProfileNavLinkActive },
                set: { _ in
                    m.migrationState = nil
                    MigrationToDeviceState.save(nil) }
            )) {
                NavigationView {
                    VStack(alignment: .leading) {
                        MigrateToDevice(migrationState: $m.migrationState)
                    }
                    .navigationTitle("迁移到这台设备")
                    .modifier(ThemedBackground(grouped: true))
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button("关闭") {
                                m.migrationState = nil
                                MigrationToDeviceState.save(nil)
                            }
                        }
                    }
                }
            }
            .sheet(isPresented: $showWhyBuilt) {
                WhySimpleX(
                    onboarding: onboarding,
                    createProfileNavLinkActive: $createProfileNavLinkActive
                )
            }
        }
        .onAppear() {
            setLastVersionDefault()
        }
        .frame(maxHeight: .infinity)
        .navigationBarHidden(true) // necessary on iOS 15
    }

    private func createFirstProfileButton() -> some View {
        ZStack {
            Button {
                createProfileNavLinkActive = true
            } label: {
                Text("开始使用 Nome")
            }
            .buttonStyle(OnboardingButtonStyle(isDisabled: false))

            NavigationLink(isActive: $createProfileNavLinkActive) {
                CreateFirstProfile()
                    .modifier(ThemedBackground())
            } label: {
                EmptyView()
            }
            .frame(width: 1, height: 1)
            .hidden()
        }
    }
}

let textSpace = Text(verbatim: " ")

let textNewLine = Text(verbatim: "\n")

struct SimpleXInfo_Previews: PreviewProvider {
    static var previews: some View {
        SimpleXInfo(onboarding: true)
    }
}
