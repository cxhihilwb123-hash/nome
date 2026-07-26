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

        guard failures.isEmpty else {
            failures.forEach { fputs("FAIL: \($0)\n", stderr) }
            exit(1)
        }
        print("PASS: NomeActivationPolicyReducers (27 cases)")
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
            "bootstrap clears leftover credentials on reinstall before profile creation",
            marker: nil,
            receipt: activeWithoutToken,
            hasUsableLocalProfile: false,
            hasActivationToken: true,
            policy: enforcedNewInstallations,
            expectedMarker: .freshInstall,
            expectedReceiptState: .unactivated,
            expectedAccess: .localOnly,
            expectedShouldClearCredentials: true
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
