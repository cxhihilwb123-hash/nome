//
//  ProfilePrivacyView.swift
//  SimpleX (iOS)
//
//  Created by Evgeny on 17/03/2023.
//  Copyright © 2023 SimpleX Chat. All rights reserved.
//

import SwiftUI
import SimpleXChat

private enum NomeHiddenProfilePalette {
    static let navy = Color(red: 14.0 / 255.0, green: 27.0 / 255.0, blue: 45.0 / 255.0)
    static let purple = Color(red: 116.0 / 255.0, green: 89.0 / 255.0, blue: 238.0 / 255.0)
}

struct HiddenProfileView: View {
    @State var user: User
    @Binding var profileHidden: Bool
    @EnvironmentObject private var m: ChatModel
    @EnvironmentObject var theme: AppTheme
    @Environment(\.dismiss) var dismiss: DismissAction
    @State private var hidePassword = ""
    @State private var confirmHidePassword = ""
    @State private var saveErrorAlert = false
    @State private var savePasswordError: String?

    var body: some View {
        List {
            NomeHiddenProfileHeader()
                .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)

            Section() {
                ProfilePreview(profileOf: user)
                    .padding(.leading, -8)
            }

            Section {
                PassphraseField(key: $hidePassword, placeholder: "用于显示身份的密码", valid: passwordValid, showStrength: true)
                PassphraseField(key: $confirmHidePassword, placeholder: "再次输入密码", valid: confirmValid)

                settingsRow("lock", color: theme.colors.secondary) {
                    Button("保存并隐藏身份") {
                        Task {
                            do {
                                let u = try await apiHideUser(user.userId, viewPwd: hidePassword)
                                await MainActor.run {
                                    m.updateUser(u)
                                    dismiss()
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                                        withAnimation { profileHidden = true }
                                    }
                                }
                            } catch let error {
                                saveErrorAlert = true
                                savePasswordError = responseError(error)
                            }
                        }
                    }
                }
                .disabled(saveDisabled)
            } header: {
                Text("隐藏身份密码")
                    .foregroundColor(theme.colors.secondary)
            } footer: {
                Text("之后需要在**身份中心**搜索框输入完整密码，才能重新显示这个身份。")
                    .foregroundColor(theme.colors.secondary)
                    .font(.body)
                    .padding(.top, 8)
            }
        }
        .alert(isPresented: $saveErrorAlert) {
            Alert(
                title: Text("保存身份密码失败"),
                message: Text(savePasswordError ?? "")
            )
        }
        .modifier(ThemedBackground(grouped: true))
    }

    var passwordValid: Bool { hidePassword == hidePassword.trimmingCharacters(in: .whitespaces) }

    var confirmValid: Bool { confirmHidePassword == "" || hidePassword == confirmHidePassword }

    var saveDisabled: Bool { hidePassword == "" || !passwordValid || confirmHidePassword == "" || !confirmValid }
}

private struct NomeHiddenProfileHeader: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                Image(systemName: "eye.slash.fill")
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 42, height: 42)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(NomeHiddenProfilePalette.purple)
                    )
                VStack(alignment: .leading, spacing: 4) {
                    Text("隐藏身份")
                        .font(.title2.weight(.bold))
                        .foregroundColor(NomeHiddenProfilePalette.navy)
                    Text("设置密码后，这个身份会从身份中心列表中隐藏。")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Text("隐藏身份不会删除聊天数据。请记住这个密码，忘记后无法通过列表直接找回。")
                .font(.subheadline)
                .lineSpacing(2)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

struct ProfilePrivacyView_Previews: PreviewProvider {
    static var previews: some View {
        HiddenProfileView(user: User.sampleData, profileHidden: Binding.constant(false))
    }
}
