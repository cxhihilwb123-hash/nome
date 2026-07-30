//
//  NomeActivation.swift
//  SimpleX
//
//  Nome invitation activation policy, secure credentials and app-side gate.
//

import Foundation
import SwiftUI
import SimpleXChat
import UserNotifications

enum NomeActivationProtectedAction: String, Identifiable, Equatable {
    case connect
    case message
    case group
    case file
    case call
    case deepLink
    case notification
    case share
    case address
    case background
    case command

    var id: String { rawValue }

    var title: String {
        switch self {
        case .connect, .deepLink: return "连接好友前需要激活 Nome"
        case .message: return "发送消息前需要激活 Nome"
        case .group: return "使用群组前需要激活 Nome"
        case .file: return "传输文件前需要激活 Nome"
        case .call: return "发起或接听通话前需要激活 Nome"
        case .notification: return "处理这条通知前需要激活 Nome"
        case .share: return "分享到 Nome 前需要激活"
        case .address: return "分享公开联系方式前需要激活 Nome"
        case .background: return "联网同步前需要激活 Nome"
        case .command: return "使用联网功能前需要激活 Nome"
        }
    }
}

struct NomeActivationPresentation: Identifiable, Equatable {
    let id = UUID()
    let action: NomeActivationProtectedAction
}

struct NomeActivationPendingCallIntent: Equatable {
    let contactId: String
    let video: Bool
}

struct NomeActivationCredentialResponse: Decodable {
    let activationToken: String
    let expiresAt: Date
    let entitlementEnd: Date?
    let graceUntil: Date
    let status: NomeActivationReceiptState
}

struct NomeActivationStatusResponse: Decodable {
    let status: NomeActivationReceiptState
    let entitlementEnd: Date?
    let offlineGraceUntil: Date
    let serverTime: Date
}

struct NomeActivationAPIErrorBody: Decodable {
    struct Detail: Decodable {
        let code: String
        let message: String
        let requestId: String?
    }

    let error: Detail
}

enum NomeActivationClientError: LocalizedError {
    case notConfigured
    case invalidResponse
    case server(status: Int, code: String, message: String)
    case credentialStorage

    var errorDescription: String? {
        switch self {
        case .notConfigured:
            return "当前版本尚未配置 Nome 激活服务地址。"
        case .invalidResponse:
            return "激活服务返回了无法识别的数据。"
        case let .server(_, _, message):
            return message
        case .credentialStorage:
            return "无法安全保存激活凭证，请检查设备安全设置后重试。"
        }
    }
}

struct NomeActivationHTTPClient {
    var fetchPolicy: (_ build: Int) async throws -> NomeActivationPolicy
    var redeem: (_ inviteCode: String, _ installationId: String, _ idempotencyKey: String, _ appVersion: String) async throws -> NomeActivationCredentialResponse
    var status: (_ token: String) async throws -> NomeActivationStatusResponse
    var refresh: (_ token: String) async throws -> NomeActivationCredentialResponse
    var migrate: (_ token: String, _ installationId: String, _ appVersion: String) async throws -> NomeActivationCredentialResponse

    static func live(
        baseURL: URL? = NomeActivationConfiguration.baseURL(),
        session: URLSession = .shared
    ) -> Self {
        let decoder = NomeActivationJSON.decoder
        let encoder = JSONEncoder()

        func request<Response: Decodable>(
            path: String,
            method: String = "GET",
            query: [URLQueryItem] = [],
            body: [String: String?]? = nil,
            token: String? = nil,
            idempotencyKey: String? = nil
        ) async throws -> Response {
            guard let baseURL else { throw NomeActivationClientError.notConfigured }
            guard var components = URLComponents(
                url: baseURL.appendingPathComponent(path),
                resolvingAgainstBaseURL: false
            ) else {
                throw NomeActivationClientError.invalidResponse
            }
            if !query.isEmpty { components.queryItems = query }
            guard let url = components.url else { throw NomeActivationClientError.invalidResponse }

            var urlRequest = URLRequest(url: url)
            urlRequest.httpMethod = method
            urlRequest.timeoutInterval = 15
            urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
            if let token {
                urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }
            if let idempotencyKey {
                urlRequest.setValue(idempotencyKey, forHTTPHeaderField: "Idempotency-Key")
            }
            if let body {
                urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
                let compactBody = body.compactMapValues { $0 }
                urlRequest.httpBody = try encoder.encode(compactBody)
            }

            let (data, response) = try await session.data(for: urlRequest)
            guard let http = response as? HTTPURLResponse else {
                throw NomeActivationClientError.invalidResponse
            }
            guard (200 ... 299).contains(http.statusCode) else {
                if let apiError = try? decoder.decode(NomeActivationAPIErrorBody.self, from: data) {
                    throw NomeActivationClientError.server(
                        status: http.statusCode,
                        code: apiError.error.code,
                        message: apiError.error.message
                    )
                }
                throw NomeActivationClientError.server(
                    status: http.statusCode,
                    code: "HTTP_\(http.statusCode)",
                    message: "激活服务暂时不可用，请稍后重试。"
                )
            }
            do {
                return try decoder.decode(Response.self, from: data)
            } catch {
                throw NomeActivationClientError.invalidResponse
            }
        }

        return Self(
            fetchPolicy: { build in
                try await request(
                    path: "api/v1/activation-policy",
                    query: [
                        URLQueryItem(name: "platform", value: "ios"),
                        URLQueryItem(name: "build", value: String(build))
                    ]
                )
            },
            redeem: { inviteCode, installationId, idempotencyKey, appVersion in
                try await request(
                    path: "api/v1/activations/redeem",
                    method: "POST",
                    body: [
                        "inviteCode": inviteCode,
                        "installationId": installationId,
                        "platform": "ios",
                        "appVersion": appVersion
                    ],
                    idempotencyKey: idempotencyKey
                )
            },
            status: { token in
                try await request(path: "api/v1/activations/status", token: token)
            },
            refresh: { token in
                try await request(path: "api/v1/activations/refresh", method: "POST", token: token)
            },
            migrate: { token, installationId, appVersion in
                try await request(
                    path: "api/v1/activations/migrate",
                    method: "POST",
                    body: [
                        "newInstallationId": installationId,
                        "platform": "ios",
                        "appVersion": appVersion
                    ],
                    token: token
                )
            }
        )
    }
}

struct NomeActivationCredentialStore {
    var token: () -> String?
    var setToken: (String) -> Bool
    var removeToken: () -> Bool
    var installationId: () -> String?
    var setInstallationId: (String) -> Bool
    var idempotencyKey: () -> String?
    var setIdempotencyKey: (String) -> Bool
    var removeIdempotencyKey: () -> Bool
    var clearAll: () -> Bool

    static let live = Self(
        token: { kcNomeActivationToken.get() },
        setToken: { kcNomeActivationToken.set($0) },
        removeToken: { kcNomeActivationToken.remove() },
        installationId: { kcNomeActivationInstallationId.get() },
        setInstallationId: { kcNomeActivationInstallationId.set($0) },
        idempotencyKey: { kcNomeActivationIdempotencyKey.get() },
        setIdempotencyKey: { kcNomeActivationIdempotencyKey.set($0) },
        removeIdempotencyKey: { kcNomeActivationIdempotencyKey.remove() },
        clearAll: {
            let removals = [
                kcNomeActivationToken.remove(),
                kcNomeActivationInstallationId.remove(),
                kcNomeActivationIdempotencyKey.remove()
            ]
            return removals.allSatisfy { $0 }
        }
    )
}

struct NomeActivationCache {
    var policy: () -> NomeActivationPolicy?
    var setPolicy: (NomeActivationPolicy?) -> Void
    var receipt: () -> NomeActivationReceipt?
    var setReceipt: (NomeActivationReceipt?) -> Void
    var migrationMarker: () -> NomeActivationMigrationMarker?
    var setMigrationMarker: (NomeActivationMigrationMarker) -> Void
    var setEffectiveAccess: (NomeActivationEffectiveAccess, Bool) -> Void
    var credentialResetRequired: () -> Bool
    var setCredentialResetRequired: (Bool) -> Void

    static let live = Self(
        policy: {
            guard let data = groupDefaults.data(forKey: GROUP_DEFAULT_NOME_ACTIVATION_POLICY) else { return nil }
            return try? NomeActivationJSON.decoder.decode(NomeActivationPolicy.self, from: data)
        },
        setPolicy: { policy in
            if let policy, let data = try? NomeActivationJSON.encoder.encode(policy) {
                groupDefaults.set(data, forKey: GROUP_DEFAULT_NOME_ACTIVATION_POLICY)
            } else {
                groupDefaults.removeObject(forKey: GROUP_DEFAULT_NOME_ACTIVATION_POLICY)
            }
        },
        receipt: {
            guard let data = groupDefaults.data(forKey: GROUP_DEFAULT_NOME_ACTIVATION_RECEIPT) else { return nil }
            return try? NomeActivationJSON.decoder.decode(NomeActivationReceipt.self, from: data)
        },
        setReceipt: { receipt in
            if let receipt, let data = try? NomeActivationJSON.encoder.encode(receipt) {
                groupDefaults.set(data, forKey: GROUP_DEFAULT_NOME_ACTIVATION_RECEIPT)
            } else {
                groupDefaults.removeObject(forKey: GROUP_DEFAULT_NOME_ACTIVATION_RECEIPT)
            }
        },
        migrationMarker: {
            groupDefaults.string(forKey: GROUP_DEFAULT_NOME_ACTIVATION_MIGRATION)
                .flatMap(NomeActivationMigrationMarker.init(rawValue:))
        },
        setMigrationMarker: { marker in
            groupDefaults.set(marker.rawValue, forKey: GROUP_DEFAULT_NOME_ACTIVATION_MIGRATION)
        },
        setEffectiveAccess: { access, wouldBlock in
            groupDefaults.set(access.rawValue, forKey: GROUP_DEFAULT_NOME_ACTIVATION_ACCESS)
            groupDefaults.set(wouldBlock, forKey: GROUP_DEFAULT_NOME_ACTIVATION_WOULD_BLOCK)
        },
        credentialResetRequired: {
            groupDefaults.bool(forKey: GROUP_DEFAULT_NOME_ACTIVATION_CREDENTIAL_RESET_REQUIRED)
        },
        setCredentialResetRequired: { required in
            groupDefaults.set(required, forKey: GROUP_DEFAULT_NOME_ACTIVATION_CREDENTIAL_RESET_REQUIRED)
        }
    )
}

enum NomeActivationGate {
    static var effectiveAccess: NomeActivationEffectiveAccess {
        let raw = groupDefaults.string(forKey: GROUP_DEFAULT_NOME_ACTIVATION_ACCESS)
        return NomeActivationEffectiveAccess(rawValue: raw ?? "") ?? .localOnly
    }

    static var allowsNetworking: Bool { effectiveAccess == .full }

    /// Persists the one-time install classification before any extension or lifecycle path can
    /// consume the shared enforcement snapshot. This function deliberately has no actor/UI work
    /// so it can run synchronously immediately after the local database is opened.
    @discardableResult
    static func bootstrapInstallation(
        hasUsableLocalProfile: Bool,
        cache: NomeActivationCache = .live,
        credentials: NomeActivationCredentialStore = .live,
        now: Date = .now
    ) -> (access: NomeActivationEffectiveAccess, wouldBlock: Bool) {
        let reduced = NomeActivationBootstrapReducer.reduce(
            policy: cache.policy(),
            marker: cache.migrationMarker(),
            receipt: cache.receipt(),
            hasUsableLocalProfile: hasUsableLocalProfile,
            hasActivationToken: !cache.credentialResetRequired() && credentials.token() != nil,
            now: now
        )
        let resetWasRequired = cache.credentialResetRequired()
        if reduced.shouldClearCredentials || resetWasRequired {
            let cleared = credentials.clearAll()
            // A failed Keychain deletion is a persistent hard block. Later launches and redeem
            // attempts retry cleanup, but no surviving token is trusted in the meantime.
            cache.setCredentialResetRequired(
                NomeActivationCredentialResetPolicy.requiredAfterCleanup(
                    wasRequired: resetWasRequired,
                    bootstrapRequestsCleanup: reduced.shouldClearCredentials,
                    cleanupSucceeded: cleared
                )
            )
        }
        cache.setMigrationMarker(reduced.marker)
        cache.setReceipt(reduced.receipt)
        cache.setEffectiveAccess(reduced.access, reduced.wouldBlock)
        return (reduced.access, reduced.wouldBlock)
    }

    static func evaluate(
        policy: NomeActivationPolicy?,
        receipt: NomeActivationReceipt,
        marker: NomeActivationMigrationMarker?,
        now: Date = .now
    ) -> (access: NomeActivationEffectiveAccess, wouldBlock: Bool) {
        NomeActivationPolicyEvaluator.evaluate(policy: policy, receipt: receipt, marker: marker, now: now)
    }

    static func require(_ action: NomeActivationProtectedAction) -> Bool {
        guard !allowsNetworking else { return true }
        Task { @MainActor in NomeActivationStore.shared.present(action) }
        return false
    }

    static func prohibitedResult<R: ChatAPIResult>() -> APIResult<R> {
        .error(
            .errorAgent(
                agentError: .CMD(
                    cmdErr: .PROHIBITED,
                    errContext: "NOME_ACTIVATION_REQUIRED"
                )
            )
        )
    }
}

@MainActor
final class NomeActivationStore: ObservableObject {
    static let shared = NomeActivationStore()

    @Published private(set) var policy: NomeActivationPolicy?
    @Published private(set) var receipt: NomeActivationReceipt
    @Published private(set) var effectiveAccess: NomeActivationEffectiveAccess
    @Published private(set) var wouldBlock = false
    @Published private(set) var isRefreshing = false
    @Published private(set) var isRedeeming = false
    @Published private(set) var lastError: String?
    @Published private(set) var pendingCallIntent: NomeActivationPendingCallIntent?
    @Published private(set) var pendingDeepLink: URL?
    @Published private(set) var pendingNotificationResponse: UNNotificationResponse?
    @Published var presentation: NomeActivationPresentation?

    private let client: NomeActivationHTTPClient
    private let credentials: NomeActivationCredentialStore
    private let cache: NomeActivationCache
    private let now: () -> Date
    private var refreshTask: Task<Void, Never>?
    private var refreshGeneration = 0
    private var lastPolicyRefreshAt: Date?
    private var transitionTask: Task<Void, Never>?

    init(
        client: NomeActivationHTTPClient = .live(),
        credentials: NomeActivationCredentialStore = .live,
        cache: NomeActivationCache = .live,
        now: @escaping () -> Date = Date.init
    ) {
        self.client = client
        self.credentials = credentials
        self.cache = cache
        self.now = now
        let cachedPolicy = cache.policy()
        self.policy = cachedPolicy
        var cachedReceipt = cache.receipt() ?? .unactivated(now: now())
        if cache.credentialResetRequired() ||
           cachedReceipt.state == .active && credentials.token() == nil {
            cachedReceipt = .unactivated(now: now())
            cache.setReceipt(cachedReceipt)
        }
        self.receipt = cachedReceipt
        let evaluation = NomeActivationGate.evaluate(
            policy: cachedPolicy,
            receipt: cachedReceipt,
            marker: cache.migrationMarker(),
            now: now()
        )
        self.effectiveAccess = evaluation.access
        self.wouldBlock = evaluation.wouldBlock
        cache.setEffectiveAccess(evaluation.access, evaluation.wouldBlock)
    }

    var needsActivation: Bool { effectiveAccess != .full }

    var shouldShowInvitationSettings: Bool {
        guard let policy else { return false }
        return policy.mode != .disabled
    }

    var invitationStatusLabel: String {
        if receipt.state == .expired || receipt.entitlementEnd.map({ $0 <= now() }) == true {
            return "已到期"
        }
        guard receipt.state == .active else { return "未激活" }
        guard let entitlementEnd = receipt.entitlementEnd else { return "永久" }
        return "有效至 \(entitlementEnd.formatted(date: .numeric, time: .omitted))"
    }

    var hasActiveInvitation: Bool {
        receipt.state == .active && effectiveAccess == .full
    }

    var policyLabel: String {
        guard let policy else { return "等待检查邀请策略" }
        switch policy.mode {
        case .disabled: return "邀请限制当前已关闭"
        case .observe: return "邀请策略观察中"
        case .enforced: return needsActivation ? "尚未激活" : "已激活"
        }
    }

    func present(_ action: NomeActivationProtectedAction) {
        presentation = NomeActivationPresentation(action: action)
    }

    func presentCallIntent(contactId: String, video: Bool) {
        pendingCallIntent = NomeActivationPendingCallIntent(contactId: contactId, video: video)
        present(.call)
    }

    func takePendingCallIntent() -> NomeActivationPendingCallIntent? {
        let pending = pendingCallIntent
        pendingCallIntent = nil
        return pending
    }

    func presentDeepLink(_ url: URL) {
        pendingDeepLink = url
        present(.deepLink)
    }

    func takePendingDeepLink() -> URL? {
        let pending = pendingDeepLink
        pendingDeepLink = nil
        return pending
    }

    func presentNotificationResponse(_ response: UNNotificationResponse) {
        pendingNotificationResponse = response
        present(.notification)
    }

    func takePendingNotificationResponse() -> UNNotificationResponse? {
        let pending = pendingNotificationResponse
        pendingNotificationResponse = nil
        return pending
    }

    func clearError() { lastError = nil }

    func bootstrapInstallation(hasUsableLocalProfile: Bool) {
        NomeActivationGate.bootstrapInstallation(
            hasUsableLocalProfile: hasUsableLocalProfile,
            cache: cache,
            credentials: credentials,
            now: now()
        )
        policy = cache.policy()
        receipt = cache.receipt() ?? .unactivated(now: now())
        applyEvaluation(allowLifecycleTransition: true)
    }

    func refreshIfNeeded(force: Bool = false) async {
        if let refreshTask {
            await refreshTask.value
            return
        }
        if !force,
           let lastPolicyRefreshAt,
           now().timeIntervalSince(lastPolicyRefreshAt) < TimeInterval(policy?.refreshIntervalSeconds ?? 3600) {
            return
        }

        refreshGeneration += 1
        let generation = refreshGeneration
        let task = Task { [weak self] in
            guard let self else { return }
            await self.performRefresh()
        }
        refreshTask = task
        await task.value
        if refreshGeneration == generation {
            refreshTask = nil
        }
    }

    func redeem(inviteCode: String) async -> Bool {
        let code = inviteCode.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !code.isEmpty else {
            lastError = "请输入邀请码。"
            return false
        }
        cancelRefresh()
        isRedeeming = true
        lastError = nil
        defer { isRedeeming = false }

        do {
            try prepareCredentialStorage()
            let installationId = try stableSecret(
                existing: credentials.installationId,
                save: credentials.setInstallationId
            )
            let idempotencyKey = try stableSecret(
                existing: credentials.idempotencyKey,
                save: credentials.setIdempotencyKey
            )
            let response = try await client.redeem(code, installationId, idempotencyKey, NomeActivationConfiguration.appVersion)
            guard credentials.setToken(response.activationToken) else {
                throw NomeActivationClientError.credentialStorage
            }
            receipt = NomeActivationReceipt(
                state: response.status,
                expiresAt: response.expiresAt,
                entitlementEnd: response.entitlementEnd,
                graceUntil: response.graceUntil,
                updatedAt: now(),
                reason: nil
            )
            cache.setReceipt(receipt)
            // Retain the idempotency key across transport/storage failures, but rotate the
            // attempt only after the activation token and receipt are durably stored.
            _ = credentials.removeIdempotencyKey()
            applyEvaluation(allowLifecycleTransition: true)
            return effectiveAccess == .full
        } catch {
            lastError = activationErrorMessage(error)
            return false
        }
    }

    func migrate() async -> Bool {
        do {
            try prepareCredentialStorage()
        } catch {
            lastError = activationErrorMessage(error)
            return false
        }
        guard let token = credentials.token() else {
            lastError = "此设备没有可迁移的激活凭证。"
            return false
        }
        cancelRefresh()
        isRedeeming = true
        lastError = nil
        defer { isRedeeming = false }
        do {
            let installationId = try stableSecret(
                existing: credentials.installationId,
                save: credentials.setInstallationId
            )
            let response = try await client.migrate(token, installationId, NomeActivationConfiguration.appVersion)
            guard credentials.setToken(response.activationToken) else {
                throw NomeActivationClientError.credentialStorage
            }
            receipt = NomeActivationReceipt(
                state: response.status,
                expiresAt: response.expiresAt,
                entitlementEnd: response.entitlementEnd,
                graceUntil: response.graceUntil,
                updatedAt: now(),
                reason: nil
            )
            cache.setReceipt(receipt)
            applyEvaluation(allowLifecycleTransition: true)
            return effectiveAccess == .full
        } catch {
            lastError = activationErrorMessage(error)
            return false
        }
    }

    private func performRefresh() async {
        isRefreshing = true
        defer { isRefreshing = false }
        var refreshErrors: [String] = []

        do {
            let fetched = try await client.fetchPolicy(NomeActivationConfiguration.buildNumber)
            guard !Task.isCancelled else { return }
            let nextPolicy = NomeActivationRefreshReducer.reducePolicy(current: policy, fetched: fetched)
            if nextPolicy != policy {
                policy = nextPolicy
                cache.setPolicy(nextPolicy)
            }
            lastPolicyRefreshAt = now()
        } catch is CancellationError {
            return
        } catch {
            refreshErrors.append(activationErrorMessage(error))
        }

        if !cache.credentialResetRequired(), let token = credentials.token() {
            do {
                let status = try await client.status(token)
                guard !Task.isCancelled else { return }
                receipt.state = status.status
                receipt.entitlementEnd = status.entitlementEnd
                receipt.graceUntil = status.offlineGraceUntil
                receipt.updatedAt = now()
                cache.setReceipt(receipt)

                if status.status == .active,
                   (receipt.expiresAt ?? .distantPast).timeIntervalSince(now()) < 6 * 60 * 60 {
                    let refreshed = try await client.refresh(token)
                    guard !Task.isCancelled else { return }
                    guard credentials.setToken(refreshed.activationToken) else {
                        throw NomeActivationClientError.credentialStorage
                    }
                    receipt = NomeActivationReceipt(
                        state: refreshed.status,
                        expiresAt: refreshed.expiresAt,
                        entitlementEnd: refreshed.entitlementEnd,
                        graceUntil: refreshed.graceUntil,
                        updatedAt: now(),
                        reason: nil
                    )
                    cache.setReceipt(receipt)
                }
            } catch is CancellationError {
                return
            } catch let error as NomeActivationClientError {
                if case let .server(_, code, _) = error,
                   ["ACTIVATION_NOT_ACTIVE", "ACTIVATION_NOT_FOUND", "ACTIVATION_TOKEN_EXPIRED"].contains(code) {
                    receipt = NomeActivationRefreshReducer.reduceReceiptAfterFailure(
                        receipt: receipt,
                        failure: .server(code: code),
                        now: now()
                    )
                    cache.setReceipt(receipt)
                } else {
                    refreshErrors.append(activationErrorMessage(error))
                }
            } catch {
                refreshErrors.append(activationErrorMessage(error))
            }
        }

        lastError = refreshErrors.first
        applyEvaluation(allowLifecycleTransition: true)
    }

    private func applyEvaluation(allowLifecycleTransition: Bool) {
        let previous = effectiveAccess
        let evaluation = NomeActivationGate.evaluate(
            policy: policy,
            receipt: receipt,
            marker: cache.migrationMarker(),
            now: now()
        )
        effectiveAccess = evaluation.access
        wouldBlock = evaluation.wouldBlock
        cache.setEffectiveAccess(evaluation.access, evaluation.wouldBlock)

        // Public contact links can be copied or shared without another Core command. Remove any
        // previously loaded link immediately when enforcement changes, before the async stop path.
        if evaluation.access != .full {
            let model = ChatModel.shared
            model.userAddress = nil
            if let url = model.appOpenUrl ?? model.appOpenUrlLater {
                pendingDeepLink = url
            }
            model.appOpenUrl = nil
            model.appOpenUrlLater = nil
            if let response = model.notificationResponse {
                pendingNotificationResponse = response
            }
            model.notificationResponse = nil
        }

        guard allowLifecycleTransition, previous != evaluation.access else { return }
        transitionTask?.cancel()
        transitionTask = Task { [weak self] in
            guard let self else { return }
            if evaluation.access == .full {
                self.startNetworkingIfPossible()
            } else {
                await self.stopNetworkingIfRunning()
            }
        }
    }

    private func startNetworkingIfPossible() {
        let model = ChatModel.shared
        guard model.chatInitialized, model.currentUser != nil, model.chatRunning != true else { return }
        do {
            try startChat()
            startChatAndActivate {}
        } catch {
            lastError = "激活已完成，但聊天服务启动失败：\(responseError(error))"
        }
    }

    private func stopNetworkingIfRunning() async {
        let model = ChatModel.shared
        guard model.chatRunning == true else { return }
        do {
            try await apiStopChat()
        } catch {
            logger.error("Nome activation: failed to stop chat: \(responseError(error))")
        }
        ChatReceiver.shared.stop()
        model.chatRunning = false
        AppChatState.shared.set(.stopped)
    }

    private func stableSecret(
        existing: () -> String?,
        save: (String) -> Bool
    ) throws -> String {
        if let value = existing(), value.count >= 16 { return value }
        let value = UUID().uuidString.lowercased()
        guard save(value) else { throw NomeActivationClientError.credentialStorage }
        return value
    }

    private func prepareCredentialStorage() throws {
        guard cache.credentialResetRequired() else { return }
        let cleared = credentials.clearAll()
        cache.setCredentialResetRequired(!cleared)
        guard cleared else { throw NomeActivationClientError.credentialStorage }
    }

    private func cancelRefresh() {
        refreshGeneration += 1
        refreshTask?.cancel()
        refreshTask = nil
    }

    private func activationErrorMessage(_ error: Error) -> String {
        if let error = error as? NomeActivationClientError {
            switch error {
            case let .server(_, code, _) where code == "INVITE_INVALID":
                return "邀请码无效、已使用或已过期。"
            case let .server(_, code, _) where code == "INVITE_ALREADY_REDEEMED":
                return "这个邀请码已经被使用。"
            case let .server(_, code, _) where code == "DEVICE_RESET_REQUIRED":
                return "需要管理员先重置旧设备，之后才能迁移激活。"
            default:
                return error.localizedDescription
            }
        }
        if error is URLError { return "无法连接激活服务，请检查网络后重试。" }
        return "激活失败，请稍后重试。"
    }
}

enum NomeActivationConfiguration {
    static var buildNumber: Int {
        Int(Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "") ?? 0
    }

    static var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown"
    }

    static func baseURL(
        bundle: Bundle = .main,
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> URL? {
        let configured = environment["NOME_ACTIVATION_BASE_URL"]
            ?? bundle.object(forInfoDictionaryKey: "NomeActivationBaseURL") as? String
        guard let configured,
              !configured.isEmpty,
              !configured.contains("$("),
              let url = URL(string: configured),
              url.scheme == "https" || url.host == "127.0.0.1" || url.host == "localhost"
        else { return nil }
        return url
    }
}

enum NomeActivationJSON {
    static let iso8601WithFractionalSeconds: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    static let iso8601: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { decoder in
            let container = try decoder.singleValueContainer()
            let value = try container.decode(String.self)
            if let date = iso8601WithFractionalSeconds.date(from: value) ?? iso8601.date(from: value) {
                return date
            }
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Invalid ISO-8601 date"
            )
        }
        return decoder
    }()

    static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .custom { date, encoder in
            var container = encoder.singleValueContainer()
            try container.encode(iso8601WithFractionalSeconds.string(from: date))
        }
        return encoder
    }()
}

extension ChatCommand {
    /// Explicit allow-list for local reads, offline bootstrap configuration, and safe teardown.
    /// Any new or unclassified command is protected by default while activation is enforced.
    var nomeAllowedWithoutActivation: Bool {
        switch self {
        case .showActiveUser,
             .createActiveUser,
             .listUsers,
             .apiSetActiveUser,
             .apiHideUser,
             .apiUnhideUser,
             .apiMuteUser,
             .apiUnmuteUser,
             .checkChatRunning,
             .apiStopChat,
             .apiSuspendChat,
             .apiSetAppFilePaths,
             .apiSetEncryptLocalFiles,
             .apiExportArchive,
             .apiImportArchive,
             .apiStorageEncryption,
             .testStorageEncryption,
             .apiSaveSettings,
             .apiGetSettings,
             .apiGetChatTags,
             .apiGetChats,
             .apiGetChat,
             .apiGetChatContentTypes,
             .apiGetChatItemInfo,
             .apiCreateChatTag,
             .apiSetChatTags,
             .apiDeleteChatTag,
             .apiUpdateChatTag,
             .apiReorderChatTags,
             .apiCreateChatItems,
             .apiGetReactionMembers,
             .apiPlanForwardChatItems,
             .apiGetGroupRelays,
             .apiListMembers,
             .apiGetServerOperators,
             .apiSetServerOperators,
             .apiGetUserServers,
             .apiSetUserServers,
             .apiValidateServers,
             .apiGetUsageConditions,
             .apiSetConditionsNotified,
             .apiAcceptConditions,
             .apiGetChatItemTTL,
             .apiGetNetworkConfig,
             .apiSetNetworkInfo,
             .apiContactInfo,
             .apiGroupMemberInfo,
             .apiContactQueueInfo,
             .apiGroupMemberQueueInfo,
             .apiGetContactCode,
             .apiGetGroupMemberCode,
             .apiListContacts,
             .apiSetContactAlias,
             .apiSetGroupAlias,
             .apiSetConnectionAlias,
             .apiSetUserUIThemes,
             .apiSetChatUIThemes,
             .apiRejectCall,
             .apiEndCall,
             .apiGetCallInvitations,
             .cancelFile,
             .stopRemoteCtrl,
             .listRemoteCtrls,
             .showVersion,
             .getAgentSubsTotal,
             .getAgentServersSummary:
            return true
        default:
            return false
        }
    }
}
