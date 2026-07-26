//
//  CreateProfile.swift
//  SimpleX (iOS)
//
//  Created by Evgeny on 07/05/2022.
//  Copyright © 2022 SimpleX Chat. All rights reserved.
//
// Spec: spec/client/navigation.md

import SwiftUI
import SimpleXChat

enum UserProfileAlert: Identifiable {
    case duplicateUserError
    case invalidDisplayNameError
    case createUserError(error: LocalizedStringKey)
    case invalidNameError(validName: String)

    var id: String {
        switch self {
        case .duplicateUserError: return "duplicateUserError"
        case .invalidDisplayNameError: return "invalidDisplayNameError"
        case .createUserError: return "createUserError"
        case let .invalidNameError(validName): return "invalidNameError \(validName)"
        }
    }
}

let MAX_BIO_LENGTH_BYTES = 160

struct CreateProfile: View {
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var theme: AppTheme
    @State private var displayName: String = ""
    @State private var profileBio: String = ""
    @FocusState private var focusDisplayName
    @State private var alert: UserProfileAlert?
    @State private var showChooseSource = false
    @State private var showImagePicker = false
    @State private var showTakePhoto = false
    @State private var chosenImage: UIImage? = nil
    @State private var profileImage: String? = nil

    var body: some View {
        List {
            Group {
                HStack(spacing: 0) {
                    Spacer(minLength: 0)
                    ZStack(alignment: .center) {
                        ZStack(alignment: .topTrailing) {
                            ProfileImage(imageStr: profileImage, size: 128)
                            if profileImage != nil {
                                Button {
                                    profileImage = nil
                                } label: {
                                    Image(systemName: "multiply")
                                        .resizable()
                                        .aspectRatio(contentMode: .fit)
                                        .frame(width: 12)
                                }
                            }
                        }

                        editImageButton { showChooseSource = true }
                            .buttonStyle(BorderlessButtonStyle())
                    }
                    .padding(.horizontal, 10) // Offsets transparent space built into 3D asset
                    Spacer(minLength: 0)
                    #if SIMPLEX_ASSETS
                    Image(colorScheme == .light ? "create-profile" : "create-profile-light")
                        .resizable()
                        .scaledToFit()
                        .frame(height: 140)
                    // No trailing spacer — asset image has empty space on the right
                    #endif
                }
            }
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
            .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 0, trailing: 0))

            Section {
                ZStack(alignment: .leading) {
                    let name = displayName.trimmingCharacters(in: .whitespaces)
                    if name != mkValidName(name) {
                        Button {
                            alert = .invalidNameError(validName: mkValidName(name))
                        } label: {
                            Image(systemName: "exclamationmark.circle").foregroundColor(.red)
                        }
                    } else {
                        Image(systemName: "pencil").foregroundColor(theme.colors.secondary)
                    }
                    TextField("Enter your name…", text: $displayName)
                        .padding(.leading, 36)
                        .focused($focusDisplayName)
                }
                ZStack(alignment: .leading) {
                    Image(systemName: "pencil").foregroundColor(theme.colors.secondary)
                    TextField("Bio", text: $profileBio)
                        .padding(.leading, 36)
                }
                Button(action: createProfile) {
                    settingsRow("checkmark", color: theme.colors.primary) { Text("Create profile") }
                }
                .disabled(!canCreateProfile(displayName) || !bioFitsLimit())
            } footer: {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Your profile is stored on your device and only shared with your contacts.")
                }
                .foregroundColor(theme.colors.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .compactSectionSpacing()
        }
        .navigationTitle("Create your profile")
        .modifier(ThemedBackground(grouped: true))
        .alert(item: $alert) { a in userProfileAlert(a, $displayName) }
        .confirmationDialog("Profile image", isPresented: $showChooseSource, titleVisibility: .visible) {
            Button("Take picture") {
                showTakePhoto = true
            }
            Button("Choose from library") {
                showImagePicker = true
            }
        }
        .fullScreenCover(isPresented: $showTakePhoto) {
            ZStack {
                Color.black.edgesIgnoringSafeArea(.all)
                CameraImagePicker(image: $chosenImage)
            }
        }
        .sheet(isPresented: $showImagePicker) {
            LibraryImagePicker(image: $chosenImage) { _ in
                await MainActor.run {
                    showImagePicker = false
                }
            }
        }
        .onChange(of: chosenImage) { image in
            Task {
                let resized: String? = if let image {
                    await resizeImageToStrSize(cropToSquare(image), maxDataSize: 12500)
                } else {
                    nil
                }
                await MainActor.run { profileImage = resized }
            }
        }
        .onAppear() {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                focusDisplayName = true
            }
        }
    }

    private func bioFitsLimit() -> Bool {
        chatJsonLength(profileBio) <= MAX_BIO_LENGTH_BYTES
    }

    private func createProfile() {
        hideKeyboard()
        let shortDescr: String? = if profileBio.isEmpty { nil } else { profileBio }
        let profile = Profile(
            displayName: displayName.trimmingCharacters(in: .whitespaces),
            fullName: "",
            shortDescr: shortDescr,
            image: profileImage
        )
        let m = ChatModel.shared
        do {
            AppChatState.shared.set(.active)
            m.currentUser = try apiCreateActiveUser(profile)
            // .isEmpty check is redundant here, but it makes it clearer what is going on
            if m.users.isEmpty || m.users.allSatisfy({ $0.user.hidden }) {
                try startChat()
                Task { await applyNomeStartupConfiguration() }
                withAnimation {
                    onboardingStageDefault.set(.step3_ChooseServerOperators)
                    m.onboardingStage = .step3_ChooseServerOperators
                }
            } else {
                onboardingStageDefault.set(.onboardingComplete)
                m.onboardingStage = .onboardingComplete
                dismiss()
                m.users = try listUsers()
                try getUserChatData()
            }
        } catch let error {
            showCreateProfileAlert(showAlert: { alert = $0 }, error)
        }
    }
}

struct CreateFirstProfile: View {
    @EnvironmentObject var m: ChatModel
    @EnvironmentObject var theme: AppTheme
    @Environment(\.colorScheme) var colorScheme: ColorScheme
    @State private var displayName: String = ""
    @FocusState private var focusDisplayName
    @State private var creationInProgress = false
    @State private var showMigrateSheet = false
    var body: some View {
        let spacing: CGFloat = 16
        let topPadding: CGFloat = 8
        let padding: CGFloat = 25
        GeometryReader { g in
            let v = ScrollView {
                VStack(alignment: .leading, spacing: spacing) {
                    NomeOnboardingLogoHeader()
                        .padding(.top, 10)

                    NomeOnboardingHeroCard(
                        symbol: "person.crop.circle.badge.checkmark",
                        title: "创建本机身份",
                        subtitle: "这不是云端账号。它只是你在这台设备上的显示资料，之后可以为不同联系人使用不同资料。",
                        tint: NomeOnboardingPalette.blue,
                        pills: [
                            ("iphone", "保存在本机"),
                            ("person.2.slash", "不公开搜索"),
                            ("arrow.triangle.2.circlepath", "可迁移")
                        ]
                    )

                    VStack(spacing: 10) {
                        NomeOnboardingFeatureRow(
                            icon: "person.text.rectangle",
                            title: "别人看到的名字",
                            text: "连接朋友后，这个名称会作为默认资料展示给对方。",
                            tint: NomeOnboardingPalette.green
                        )
                        NomeOnboardingFeatureRow(
                            icon: "eye.slash",
                            title: "以后可用匿名资料",
                            text: "加入新联系人或群组时，可以选择不暴露主资料。",
                            tint: NomeOnboardingPalette.purple
                        )
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("显示名称")
                            .font(.subheadline.weight(.semibold))
                            .foregroundColor(NomeOnboardingPalette.navy)
                        profileNameField()
                        if let validationMessage = firstProfileNameValidationMessage(displayName) {
                            Text(validationMessage)
                                .font(.caption)
                                .foregroundColor(.red)
                                .fixedSize(horizontal: false, vertical: true)
                        } else {
                            Text("只需要一个名字。手机号、邮箱和密码都不是必须项。")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(.top, 2)

                    Spacer(minLength: 0)

                    createProfileButton()
                        .padding(.bottom, g.safeAreaInsets.bottom == 0 ? 20 : 0)
                }
                .padding(.horizontal, padding)
                .padding(.top, topPadding)
                .padding(.bottom, padding)
                .frame(minHeight: g.size.height)
            }
            .onTapGesture { focusDisplayName = false }
            .sheet(isPresented: $showMigrateSheet, onDismiss: {
                m.migrationState = nil
                MigrationToDeviceState.save(nil)
            }) {
                NavigationView {
                    MigrateToDevice(migrationState: $m.migrationState)
                        .navigationTitle("迁移到这台设备")
                        .modifier(ThemedBackground(grouped: true))
                        .toolbar {
                            ToolbarItem(placement: .navigationBarLeading) {
                                Button("关闭") {
                                    m.migrationState = nil
                                    MigrationToDeviceState.save(nil)
                                    showMigrateSheet = false
                                }
                            }
                        }
                }
            }
            if #available(iOS 17, *) {
                v.scrollBounceBehavior(.basedOnSize).defaultScrollAnchor(.bottom)
            } else if #available(iOS 16.4, *) {
                v.scrollBounceBehavior(.basedOnSize)
            } else {
                v
            }
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    if m.migrationState == nil {
                        m.migrationState = .pasteOrScanLink
                    }
                    showMigrateSheet = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "tray.and.arrow.down")
                        Text("迁移")
                            .fontWeight(.medium)
                    }
                }
            }
        }
        .onAppear() {
            if #available(iOS 16, *) {
                focusDisplayName = true
            } else {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    focusDisplayName = true
                }
            }
        }
        .frame(maxHeight: .infinity)
    }

    private func profileNameField() -> some View {
        ZStack(alignment: .leading) {
            TextField("输入你的显示名称", text: $displayName)
                .focused($focusDisplayName)
                .padding(.horizontal)
                .padding(.vertical, 13)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color(uiColor: .tertiarySystemFill))
                )
        }
    }

    private func createProfileButton() -> some View {
        let disabled = creationInProgress || !canCreateFirstProfile(displayName)
        return Button {
            createProfile()
        } label: {
            HStack(spacing: 8) {
                if creationInProgress {
                    ProgressView()
                        .tint(.white)
                }
                Text(creationInProgress ? "正在创建…" : "创建本机身份")
            }
        }
        .buttonStyle(OnboardingTransitionButtonStyle(isDisabled: disabled))
        .disabled(disabled)
    }

    private func createProfile() {
        guard !creationInProgress else { return }
        creationInProgress = true
        hideKeyboard()
        DispatchQueue.main.async {
            performCreateProfile()
        }
    }

    private func performCreateProfile() {
        let profile = Profile(
            displayName: displayName.trimmingCharacters(in: .whitespaces),
            fullName: ""
        )
        let m = ChatModel.shared
        do {
            AppChatState.shared.set(.active)
            m.currentUser = try apiCreateActiveUser(profile)
            try startChat(onboarding: true)
            Task { await applyNomeStartupConfiguration() }
            onboardingStageDefault.set(.step3_ChooseServerOperators)
            m.onboardingStage = .step3_ChooseServerOperators
        } catch let error {
            creationInProgress = false
            showCreateProfileAlert(
                showAlert: { AlertManager.shared.showAlert(userProfileAlert($0, $displayName)) },
                error
            )
        }
    }
}

private func canCreateFirstProfile(_ displayName: String) -> Bool {
    let name = displayName.trimmingCharacters(in: .whitespaces)
    return !name.isEmpty && mkValidName(name) == name
}

private func firstProfileNameValidationMessage(_ displayName: String) -> String? {
    let name = displayName.trimmingCharacters(in: .whitespaces)
    return !name.isEmpty && mkValidName(name) != name
        ? "显示名称包含不支持的字符或过长，请修改后继续。"
        : nil
}

private func showCreateProfileAlert(
    showAlert: (UserProfileAlert) -> Void,
    _ error: Error
) {
    let m = ChatModel.shared
    switch error as? ChatError {
    case .errorStore(.duplicateName),
         .error(.userExists):
        if m.currentUser == nil {
            AlertManager.shared.showAlert(duplicateUserAlert)
        } else {
            showAlert(.duplicateUserError)
        }
    case .error(.invalidDisplayName):
        if m.currentUser == nil {
            AlertManager.shared.showAlert(invalidDisplayNameAlert)
        } else {
            showAlert(.invalidDisplayNameError)
        }
    default:
        let err: LocalizedStringKey = "Error: \(responseError(error))"
        if m.currentUser == nil {
            AlertManager.shared.showAlert(creatUserErrorAlert(err))
        } else {
            showAlert(.createUserError(error: err))
        }
    }
    logger.error("Failed to create user or start chat: \(responseError(error))")
}

private func canCreateProfile(_ displayName: String) -> Bool {
    let name = displayName.trimmingCharacters(in: .whitespaces)
    return name != "" && mkValidName(name) == name
}

func userProfileAlert(_ alert: UserProfileAlert, _ displayName: Binding<String>) -> Alert {
    switch alert {
    case .duplicateUserError: return duplicateUserAlert
    case .invalidDisplayNameError: return invalidDisplayNameAlert
    case let .createUserError(err): return creatUserErrorAlert(err)
    case let .invalidNameError(name): return createInvalidNameAlert(name, displayName)
    }
}

private var duplicateUserAlert: Alert {
    Alert(
        title: Text("Duplicate display name!"),
        message: Text("You already have a chat profile with the same display name. Please choose another name.")
    )
}

private var invalidDisplayNameAlert: Alert {
    Alert(
        title: Text("Invalid display name!"),
        message: Text("This display name is invalid. Please choose another name.")
    )
}

private func creatUserErrorAlert(_ err: LocalizedStringKey) -> Alert {
    Alert(
        title: Text("Error creating profile!"),
        message: Text(err)
    )
}

func createInvalidNameAlert(_ name: String, _ displayName: Binding<String>) -> Alert {
    name == ""
    ? Alert(title: Text("Invalid name!"))
    : Alert(
        title: Text("Invalid name!"),
        message: Text("Correct name to \(name)?"),
        primaryButton: .default(
            Text("Ok"),
            action: { displayName.wrappedValue = name }
        ),
        secondaryButton: .cancel()
    )
}

func validDisplayName(_ name: String) -> Bool {
    mkValidName(name.trimmingCharacters(in: .whitespaces)) == name
}

func mkValidName(_ s: String) -> String {
    var c = s.cString(using: .utf8)!
    return fromCString(chat_valid_name(&c)!)
}

struct CreateProfile_Previews: PreviewProvider {
    static var previews: some View {
        CreateProfile()
    }
}
