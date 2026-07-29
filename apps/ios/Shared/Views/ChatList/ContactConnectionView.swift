//
//  ContactConnectionView.swift
//  SimpleX (iOS)
//
//  Created by Evgeny on 24/04/2022.
//  Copyright © 2022 SimpleX Chat. All rights reserved.
//

import SwiftUI
import SimpleXChat

struct ContactConnectionView: View {
    @EnvironmentObject var m: ChatModel
    @ObservedObject var chat: Chat
    @EnvironmentObject var theme: AppTheme
    @Environment(\.dynamicTypeSize) private var userFont: DynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var nomeCompactRowHeight: CGFloat = 72
    @ScaledMetric(relativeTo: .body) private var nomeCompactAvatarSize: CGFloat = 44
    @ScaledMetric(relativeTo: .body) private var nomeCompactTitleSize: CGFloat = 16
    @ScaledMetric(relativeTo: .body) private var nomeCompactSupportingSize: CGFloat = 13
    var nomeCompactStyle = false
    @State private var localAlias = ""
    @FocusState private var aliasTextFieldFocused: Bool

    var body: some View {
        if case let .contactConnection(conn) = chat.chatInfo {
            if nomeCompactStyle {
                compactContactConnectionView(conn)
            } else {
                contactConnectionView(conn)
            }
        }
    }

    private func compactContactConnectionView(_ contactConnection: PendingContactConnection) -> some View {
        HStack(spacing: 12) {
            ChatInfoImage(chat: chat, size: nomeCompactAvatarSize)

            VStack(alignment: .leading, spacing: 1) {
                Text(contactConnection.chatViewName)
                    .font(.system(size: nomeCompactTitleSize, weight: .medium))
                    .foregroundColor(theme.colors.onBackground)
                    .lineLimit(1)

                HStack(spacing: 4) {
                    Text(contactConnection.description)
                        .font(.system(size: nomeCompactSupportingSize, weight: .regular))
                        .foregroundColor(theme.colors.secondary)
                        .lineLimit(1)

                    if contactConnection.incognito {
                        incognitoIcon(
                            true,
                            theme.colors.secondary,
                            size: nomeCompactSupportingSize + 3
                        )
                        .fixedSize()
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            formatTimestampText(contactConnection.updatedAt)
                .font(.system(size: nomeCompactSupportingSize, weight: .regular))
                .foregroundColor(theme.colors.secondary)
                .frame(minWidth: 52, alignment: .trailing)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .frame(minHeight: nomeCompactRowHeight)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }

    func contactConnectionView(_ contactConnection: PendingContactConnection) -> some View {
        HStack(spacing: 8) {
            Group {
                Image(systemName: contactConnection.initiated ? "link.badge.plus" : "link")
                    .resizable()
                    .scaledToFill()
                    .frame(width: 48, height: 48)
                    .foregroundColor(Color(uiColor: .tertiarySystemGroupedBackground).asAnotherColorFromSecondaryVariant(theme))
            }
            .frame(width: 63, height: 63)
            .padding(.leading, 4)

            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top) {
                    Text(contactConnection.chatViewName)
                        .font(.title3)
                        .bold()
                        .allowsTightening(false)
                        .foregroundColor(theme.colors.secondary)
                        .padding(.horizontal, 8)
                        .padding(.top, 1)
                        .padding(.bottom, 0.5)
                        .frame(alignment: .topLeading)

                    Spacer()

                    formatTimestampText(contactConnection.updatedAt)
                        .font(.subheadline)
                        .padding(.trailing, 8)
                        .padding(.vertical, 4)
                        .frame(minWidth: 60, alignment: .trailing)
                        .foregroundColor(theme.colors.secondary)
                }
                .padding(.bottom, 2)

                ZStack(alignment: .topTrailing) {
                    Text(contactConnection.description)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    incognitoIcon(contactConnection.incognito, theme.colors.secondary, size: dynamicSize(userFont).incognitoSize)
                        .padding(.top, 26)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .padding(.horizontal, 8)

                Spacer()
            }
            .frame(maxHeight: .infinity)
        }
    }
}

struct ContactConnectionView_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            ContactConnectionView(chat: Chat(chatInfo: ChatInfo.sampleData.contactConnection))
                .previewDisplayName("Legacy")
            ContactConnectionView(
                chat: Chat(chatInfo: ChatInfo.sampleData.contactConnection),
                nomeCompactStyle: true
            )
            .previewDisplayName("Nome compact")
        }
        .previewLayout(.fixed(width: 360, height: 80))
    }
}
