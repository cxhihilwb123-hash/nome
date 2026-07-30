import Darwin
import Foundation

@main
struct NomeActivationPolicyTests {
    private static let now = Date(timeIntervalSince1970: 1_800_000_000)
    private static var failures: [String] = []

    private enum FakeCommand {
        case readOnly
        case sendMessage
    }

    static func main() {
        runPolicyEvaluatorTests()
        runBootstrapReducerTests()
        runRefreshReducerTests()
        runCredentialResetPolicyTests()
        runDefaultDenyClassifierTests()
        runChatCommandAllowListRegressionTests()
        runNomeOperatorTagRegressionTest()
        runNomeNetworkDefaultsRegressionTests()

        guard failures.isEmpty else {
            failures.forEach { fputs("FAIL: \($0)\n", stderr) }
            exit(1)
        }
        print("PASS: NomeActivationPolicyReducers (42 cases)")
    }

    private static func runPolicyEvaluatorTests() {
        let fresh = NomeActivationReceipt.unactivated(now: now)
        let grandfathered = NomeActivationReceipt.grandfathered(now: now)
        let activeInTokenWindow = NomeActivationReceipt(
            state: .active,
            expiresAt: now.addingTimeInterval(60),
            graceUntil: nil,
            updatedAt: now,
            reason: nil
        )
        let activeInGrace = NomeActivationReceipt(
            state: .active,
            expiresAt: now.addingTimeInterval(-60),
            graceUntil: now.addingTimeInterval(3_600),
            updatedAt: now,
            reason: nil
        )
        let activeExpired = NomeActivationReceipt(
            state: .active,
            expiresAt: now.addingTimeInterval(-120),
            graceUntil: now.addingTimeInterval(-60),
            updatedAt: now,
            reason: nil
        )
        let accessPeriodExpired = NomeActivationReceipt(
            state: .active,
            expiresAt: now.addingTimeInterval(3_600),
            entitlementEnd: now.addingTimeInterval(-1),
            graceUntil: now.addingTimeInterval(86_400),
            updatedAt: now,
            reason: nil
        )
        let pendingMigration = NomeActivationReceipt(
            state: .pendingMigration,
            expiresAt: nil,
            graceUntil: nil,
            updatedAt: now,
            reason: nil
        )

        expectEvaluation("no policy fails closed for a fresh install", .localOnly, false,
                         policy: nil, receipt: fresh, marker: .freshInstall)
        expectEvaluation("no policy preserves a grandfathered install", .full, false,
                         policy: nil, receipt: grandfathered, marker: .grandfathered)
        expectEvaluation("disabled is fully usable", .full, false,
                         policy: policy(.disabled), receipt: fresh, marker: .freshInstall)
        expectEvaluation("observe records would-block without enforcement", .full, true,
                         policy: policy(.observe), receipt: fresh, marker: .freshInstall)
        expectEvaluation("enforced blocks a fresh install", .localOnly, false,
                         policy: policy(.enforced), receipt: fresh, marker: .freshInstall)
        expectEvaluation("new-install audience preserves grandfathering", .full, false,
                         policy: policy(.enforced, audience: .newInstallations), receipt: grandfathered, marker: .grandfathered)
        expectEvaluation("all-unactivated audience gates grandfathering", .localOnly, false,
                         policy: policy(.enforced, audience: .allUnactivated), receipt: grandfathered, marker: .grandfathered)
        expectEvaluation("active receipt works during token lifetime", .full, false,
                         policy: policy(.enforced), receipt: activeInTokenWindow, marker: .freshInstall)
        expectEvaluation("active receipt works during offline grace", .full, false,
                         policy: policy(.enforced), receipt: activeInGrace, marker: .freshInstall)
        expectEvaluation("expired token and grace are blocked", .localOnly, false,
                         policy: policy(.enforced), receipt: activeExpired, marker: .freshInstall)
        expectEvaluation("entitlement end overrides token and offline grace", .localOnly, false,
                         policy: policy(.enforced), receipt: accessPeriodExpired, marker: .freshInstall)
        expectEvaluation("pending migration has a distinct recovery state", .migrationRequired, false,
                         policy: policy(.enforced), receipt: pendingMigration, marker: .freshInstall)
        expectEvaluation("disabled platform cannot enforce", .full, false,
                         policy: policy(.enforced, platformEnabled: false), receipt: fresh, marker: .freshInstall)
        expectEvaluation("unsupported build cannot enforce", .full, false,
                         policy: policy(.enforced, supported: false), receipt: fresh, marker: .freshInstall)
        expectEvaluation("future rollout cannot enforce early", .full, false,
                         policy: policy(.enforced, effectiveAt: now.addingTimeInterval(60)), receipt: fresh, marker: .freshInstall)
        expectEvaluation("unknown schema cannot enforce", .full, false,
                         policy: policy(.enforced, schemaVersion: 2), receipt: fresh, marker: .freshInstall)
    }

    private static func runBootstrapReducerTests() {
        let enforcedNewInstallations = policy(.enforced, audience: .newInstallations)
        let unactivated = NomeActivationReceipt.unactivated(now: now)
        let activeWithoutToken = NomeActivationReceipt(
            state: .active,
            expiresAt: now.addingTimeInterval(600),
            graceUntil: nil,
            updatedAt: now,
            reason: nil
        )

        expectBootstrap(
            "bootstrap grandfathers a preexisting local profile",
            marker: nil,
            receipt: nil,
            hasUsableLocalProfile: true,
            hasActivationToken: false,
            policy: enforcedNewInstallations,
            expectedMarker: .grandfathered,
            expectedReceiptState: .grandfathered,
            expectedAccess: .full,
            expectedShouldClearCredentials: false
        )
        expectBootstrap(
            "bootstrap marks a fresh install before profile creation",
            marker: nil,
            receipt: nil,
            hasUsableLocalProfile: false,
            hasActivationToken: false,
            policy: enforcedNewInstallations,
            expectedMarker: .freshInstall,
            expectedReceiptState: .unactivated,
            expectedAccess: .localOnly,
            expectedShouldClearCredentials: true
        )
        expectBootstrap(
            "bootstrap drops an active receipt when the keychain token is missing",
            marker: .freshInstall,
            receipt: activeWithoutToken,
            hasUsableLocalProfile: false,
            hasActivationToken: false,
            policy: enforcedNewInstallations,
            expectedMarker: .freshInstall,
            expectedReceiptState: .unactivated,
            expectedAccess: .localOnly,
            expectedShouldClearCredentials: false
        )
        expectBootstrap(
            "bootstrap restores a grandfather receipt from an existing grandfather marker",
            marker: .grandfathered,
            receipt: unactivated,
            hasUsableLocalProfile: false,
            hasActivationToken: false,
            policy: enforcedNewInstallations,
            expectedMarker: .grandfathered,
            expectedReceiptState: .grandfathered,
            expectedAccess: .full,
            expectedShouldClearCredentials: false
        )
        expectBootstrap(
            "bootstrap preserves a surviving keychain token for same-device reinstall validation",
            marker: nil,
            receipt: nil,
            hasUsableLocalProfile: false,
            hasActivationToken: true,
            policy: enforcedNewInstallations,
            expectedMarker: .freshInstall,
            expectedReceiptState: .unactivated,
            expectedAccess: .localOnly,
            expectedShouldClearCredentials: false
        )
    }

    private static func runRefreshReducerTests() {
        let current = policy(.enforced, revision: 7)
        let stale = policy(.disabled, revision: 6)
        let newer = policy(.observe, revision: 8)
        let activeInGrace = NomeActivationReceipt(
            state: .active,
            expiresAt: now.addingTimeInterval(-60),
            graceUntil: now.addingTimeInterval(600),
            updatedAt: now,
            reason: nil
        )
        let activeOutOfGrace = NomeActivationReceipt(
            state: .active,
            expiresAt: now.addingTimeInterval(-60),
            graceUntil: now.addingTimeInterval(-30),
            updatedAt: now,
            reason: nil
        )

        expectPolicyReduction(
            "stale policy revision does not overwrite a newer cached policy",
            current: current,
            fetched: stale,
            expected: current
        )
        expectPolicyReduction(
            "newer policy revision replaces the cached policy",
            current: current,
            fetched: newer,
            expected: newer
        )
        expectReceiptFailure(
            "activation-not-found failure becomes revoked",
            receipt: activeInGrace,
            failure: .server(code: "ACTIVATION_NOT_FOUND"),
            expectedState: .revoked
        )
        expectReceiptFailure(
            "token-expired failure becomes expired after grace",
            receipt: activeOutOfGrace,
            failure: .server(code: "ACTIVATION_TOKEN_EXPIRED"),
            expectedState: .expired
        )
        expectReceiptFailure(
            "token-expired failure preserves active state while grace remains",
            receipt: activeInGrace,
            failure: .server(code: "ACTIVATION_TOKEN_EXPIRED"),
            expectedState: .active
        )
    }

    private static func runDefaultDenyClassifierTests() {
        let classifier = NomeActivationDefaultDenyClassifier(allowedCommands: [FakeCommand.readOnly])
        expect(
            "default-deny classifier allows explicitly listed commands",
            classifier.allows(.readOnly)
        )
        expect(
            "default-deny classifier rejects unlisted commands",
            !classifier.allows(.sendMessage)
        )
    }

    private static func runChatCommandAllowListRegressionTests() {
        let activationSourceURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("Shared/Model/NomeActivation.swift")
        guard let source = try? String(contentsOf: activationSourceURL, encoding: .utf8),
              let allowListStart = source.range(of: "var nomeAllowedWithoutActivation: Bool"),
              let defaultCase = source.range(
                of: "\n        default:",
                range: allowListStart.upperBound ..< source.endIndex
              ) else {
            failures.append("ChatCommand allow-list source could not be inspected")
            return
        }
        let allowList = source[allowListStart.lowerBound ..< defaultCase.lowerBound]
        expect(
            "server validation remains available before activation",
            allowList.contains(".apiValidateServers")
        )
        expect(
            "server selection remains available before activation",
            allowList.contains(".apiSetUserServers")
        )
    }

    private static func runNomeOperatorTagRegressionTest() {
        let apiTypesURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("Shared/Model/AppAPITypes.swift")
        guard let source = try? String(contentsOf: apiTypesURL, encoding: .utf8),
              let enumStart = source.range(of: "enum OperatorTag: String, Codable"),
              let enumEnd = source.range(of: "\n}", range: enumStart.upperBound ..< source.endIndex) else {
            failures.append("OperatorTag source could not be inspected")
            return
        }
        let operatorTag = source[enumStart.lowerBound ..< enumEnd.upperBound]
        expect(
            "current Core Nome operator tag remains decodable on iOS",
            operatorTag.contains("case nome = \"nome\"")
        )

        let simplexAPIURL = apiTypesURL.deletingLastPathComponent().appendingPathComponent("SimpleXAPI.swift")
        let simplexAPISource = try? String(contentsOf: simplexAPIURL, encoding: .utf8)
        expect(
            "current Core Nome operator remains the managed routing owner on iOS",
            simplexAPISource?.contains("operatorTag == .nome") == true &&
                simplexAPISource?.contains("preset: nomeOperatorIndex != nil") == true
        )
        expect(
            "preset Nome SMP route pins the explicit 5223 service port",
            simplexAPISource?.contains("withDefaultPort($0, defaultPort: 5223)") == true
        )
        if let simplexAPISource,
           let reconcileStart = simplexAPISource.range(of: "private func reconcileNomeChatRelay"),
           let reconcileEnd = simplexAPISource.range(
               of: "private func isNomeChatRelayIdentity",
               range: reconcileStart.upperBound ..< simplexAPISource.endIndex
           ) {
            let reconciliation = simplexAPISource[reconcileStart.lowerBound ..< reconcileEnd.lowerBound]
            expect(
                "existing Nome relay presets rotate to the current bundled address",
                reconciliation.contains("relays[index].address = normalizedAddress") &&
                    reconciliation.contains("relays[index].tested = nil")
            )
            expect(
                "relay address rotation preserves the user's enabled and deleted choices",
                !reconciliation.contains("relays[index].enabled =") &&
                    !reconciliation.contains("relays[index].deleted =")
            )
        } else {
            failures.append("Nome relay reconciliation source could not be inspected")
        }
        if let simplexAPISource,
           let identityStart = simplexAPISource.range(of: "private func isNomeChatRelayIdentity"),
           let identityEnd = simplexAPISource.range(
               of: "private func enableNomeServer",
               range: identityStart.upperBound ..< simplexAPISource.endIndex
           ) {
            let identity = simplexAPISource[identityStart.lowerBound ..< identityEnd.lowerBound]
            expect(
                "Nome relay rotation requires a Nome-controlled domain in addition to its display name",
                identity.contains("guard relay.displayName == \"Nome Relay\"") &&
                    identity.contains("relay.domains.contains(where: isNomeDomain)") &&
                    identity.contains("URLComponents(") &&
                    identity.contains("normalized.hasSuffix(\".nome.im\")")
            )
        } else {
            failures.append("Nome relay identity source could not be inspected")
        }
    }

    private static func runNomeNetworkDefaultsRegressionTests() {
        let iosRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let appGroupSource = try? String(
            contentsOf: iosRoot.appendingPathComponent("SimpleXChat/AppGroup.swift"),
            encoding: .utf8
        )
        let simplexAPISource = try? String(
            contentsOf: iosRoot.appendingPathComponent("Shared/Model/SimpleXAPI.swift"),
            encoding: .utf8
        )
        let networkViewSource = try? String(
            contentsOf: iosRoot.appendingPathComponent("Shared/Views/UserSettings/NetworkAndServers/NetworkAndServers.swift"),
            encoding: .utf8
        )
        expect(
            "fresh Nome installs use the standard SMP service port",
            appGroupSource?.contains("GROUP_DEFAULT_NETWORK_SMP_WEB_PORT_SERVERS: SMPWebPortServers.off.rawValue") == true
        )
        expect(
            "missing SMP port preference falls back to the standard service port",
            appGroupSource?.contains("forKey: GROUP_DEFAULT_NETWORK_SMP_WEB_PORT_SERVERS,\n    withDefault: .off") == true
        )
        expect(
            "fresh Nome installs allow direct fallback when the private route is unavailable",
            appGroupSource?.contains("GROUP_DEFAULT_NETWORK_SMP_PROXY_FALLBACK: SMPProxyFallback.allow.rawValue") == true &&
                appGroupSource?.contains("forKey: GROUP_DEFAULT_NETWORK_SMP_PROXY_FALLBACK,\n    withDefault: .allow") == true
        )
        expect(
            "existing preset web-port installs receive a one-time migration",
            simplexAPISource?.contains("applyNomeStandardSMPPortMigrationIfNeeded()") == true &&
                simplexAPISource?.contains("networkSMPWebPortServersDefault.get() == .preset") == true &&
                simplexAPISource?.contains("networkSMPWebPortServersDefault.set(.off)") == true
        )
        expect(
            "the SMP port migration runs before Core network configuration",
            simplexAPISource?.contains("applyNomeStandardSMPPortMigrationIfNeeded()\n    applyNomeSMPProxyFallbackMigrationIfNeeded()\n    try setNetworkConfig(getNetCfg())") == true
        )
        expect(
            "existing protected-only fallback installs receive a one-time Nome migration",
            simplexAPISource?.contains("applyNomeSMPProxyFallbackMigrationIfNeeded()") == true &&
                simplexAPISource?.contains("networkSMPProxyFallbackGroupDefault.get() == .allowProtected") == true &&
                simplexAPISource?.contains("networkSMPProxyFallbackGroupDefault.set(.allow)") == true
        )
        expect(
            "the SMP proxy fallback migration runs before Core network configuration",
            simplexAPISource?.contains("applyNomeSMPProxyFallbackMigrationIfNeeded()\n    try setNetworkConfig(getNetCfg())") == true
        )
        expect(
            "network settings show the real Nome operator group",
            networkViewSource?.contains("$0.operator?.operatorTag == .nome") == true
        )
    }

    private static func runCredentialResetPolicyTests() {
        expect(
            "failed keychain cleanup remains a persistent hard block",
            NomeActivationCredentialResetPolicy.requiredAfterCleanup(
                wasRequired: false,
                bootstrapRequestsCleanup: true,
                cleanupSucceeded: false
            )
        )
        expect(
            "successful retry clears the persistent credential reset block",
            !NomeActivationCredentialResetPolicy.requiredAfterCleanup(
                wasRequired: true,
                bootstrapRequestsCleanup: false,
                cleanupSucceeded: true
            )
        )
    }

    private static func expectEvaluation(
        _ name: String,
        _ expectedAccess: NomeActivationEffectiveAccess,
        _ expectedWouldBlock: Bool,
        policy: NomeActivationPolicy?,
        receipt: NomeActivationReceipt,
        marker: NomeActivationMigrationMarker
    ) {
        let actual = NomeActivationPolicyEvaluator.evaluate(
            policy: policy,
            receipt: receipt,
            marker: marker,
            now: now
        )
        if actual.access != expectedAccess || actual.wouldBlock != expectedWouldBlock {
            failures.append(
                "\(name): expected \(expectedAccess.rawValue)/\(expectedWouldBlock), " +
                "got \(actual.access.rawValue)/\(actual.wouldBlock)"
            )
        }
    }

    private static func expectBootstrap(
        _ name: String,
        marker: NomeActivationMigrationMarker?,
        receipt: NomeActivationReceipt?,
        hasUsableLocalProfile: Bool,
        hasActivationToken: Bool,
        policy: NomeActivationPolicy?,
        expectedMarker: NomeActivationMigrationMarker,
        expectedReceiptState: NomeActivationReceiptState,
        expectedAccess: NomeActivationEffectiveAccess,
        expectedShouldClearCredentials: Bool
    ) {
        let actual = NomeActivationBootstrapReducer.reduce(
            policy: policy,
            marker: marker,
            receipt: receipt,
            hasUsableLocalProfile: hasUsableLocalProfile,
            hasActivationToken: hasActivationToken,
            now: now
        )
        if actual.marker != expectedMarker
            || actual.receipt.state != expectedReceiptState
            || actual.access != expectedAccess
            || actual.shouldClearCredentials != expectedShouldClearCredentials {
            failures.append(
                "\(name): expected marker=\(expectedMarker.rawValue) receipt=\(expectedReceiptState.rawValue) " +
                "access=\(expectedAccess.rawValue) clear=\(expectedShouldClearCredentials), got marker=\(actual.marker.rawValue) " +
                "receipt=\(actual.receipt.state.rawValue) access=\(actual.access.rawValue) clear=\(actual.shouldClearCredentials)"
            )
        }
    }

    private static func expectPolicyReduction(
        _ name: String,
        current: NomeActivationPolicy?,
        fetched: NomeActivationPolicy,
        expected: NomeActivationPolicy?
    ) {
        let actual = NomeActivationRefreshReducer.reducePolicy(current: current, fetched: fetched)
        if actual != expected {
            failures.append(
                "\(name): expected revision \(expected?.revision.description ?? "nil"), " +
                "got \(actual?.revision.description ?? "nil")"
            )
        }
    }

    private static func expectReceiptFailure(
        _ name: String,
        receipt: NomeActivationReceipt,
        failure: NomeActivationRefreshFailure,
        expectedState: NomeActivationReceiptState
    ) {
        let actual = NomeActivationRefreshReducer.reduceReceiptAfterFailure(
            receipt: receipt,
            failure: failure,
            now: now
        )
        if actual.state != expectedState {
            failures.append(
                "\(name): expected state \(expectedState.rawValue), got \(actual.state.rawValue)"
            )
        }
    }

    private static func expect(_ name: String, _ condition: Bool) {
        if !condition {
            failures.append("\(name): expected true")
        }
    }

    private static func policy(
        _ mode: NomeActivationMode,
        audience: NomeActivationAudience = .newInstallations,
        platformEnabled: Bool = true,
        supported: Bool = true,
        effectiveAt: Date? = nil,
        schemaVersion: Int = 1,
        revision: Int = 7
    ) -> NomeActivationPolicy {
        NomeActivationPolicy(
            schemaVersion: schemaVersion,
            revision: revision,
            platform: "ios",
            build: 349,
            platformEnabled: platformEnabled,
            minimumBuild: 0,
            supported: supported,
            mode: mode,
            audience: audience,
            effectiveAt: effectiveAt ?? now.addingTimeInterval(-60),
            refreshIntervalSeconds: 3_600,
            serverTime: now
        )
    }
}
