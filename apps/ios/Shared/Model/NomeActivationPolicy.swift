//
//  NomeActivationPolicy.swift
//  SimpleX
//
//  Pure, deterministic invitation-policy model shared by the app and command-line tests.
//

import Foundation

enum NomeActivationMode: String, Codable, CaseIterable {
    case disabled
    case observe
    case enforced
}

enum NomeActivationAudience: String, Codable, CaseIterable {
    case newInstallations = "new_installations"
    case allUnactivated = "all_unactivated"
}

enum NomeActivationReceiptState: String, Codable {
    case unactivated
    case active
    case grandfathered
    case paused
    case pendingMigration = "pending_migration"
    case revoked
    case expired
}

enum NomeActivationEffectiveAccess: String, Codable, Equatable {
    case full
    case localOnly = "local_only"
    case migrationRequired = "migration_required"
}

enum NomeActivationMigrationMarker: String, Codable {
    case freshInstall = "fresh_install"
    case grandfathered = "preexisting_local_profile"
}

struct NomeActivationPolicy: Codable, Equatable {
    let schemaVersion: Int
    let revision: Int
    let platform: String
    let build: Int
    let platformEnabled: Bool
    let minimumBuild: Int
    let supported: Bool
    let mode: NomeActivationMode
    let audience: NomeActivationAudience
    let effectiveAt: Date
    let refreshIntervalSeconds: Int
    let serverTime: Date
}

struct NomeActivationReceipt: Codable, Equatable {
    var state: NomeActivationReceiptState
    var expiresAt: Date?
    var graceUntil: Date?
    var updatedAt: Date
    var reason: String?

    static func unactivated(now: Date = .now) -> Self {
        Self(state: .unactivated, expiresAt: nil, graceUntil: nil, updatedAt: now, reason: nil)
    }

    static func grandfathered(now: Date = .now) -> Self {
        Self(
            state: .grandfathered,
            expiresAt: nil,
            graceUntil: nil,
            updatedAt: now,
            reason: NomeActivationMigrationMarker.grandfathered.rawValue
        )
    }
}

struct NomeActivationBootstrapResult: Equatable {
    let marker: NomeActivationMigrationMarker
    let receipt: NomeActivationReceipt
    let access: NomeActivationEffectiveAccess
    let wouldBlock: Bool
    let shouldClearCredentials: Bool
}

enum NomeActivationBootstrapReducer {
    static func reduce(
        policy: NomeActivationPolicy?,
        marker: NomeActivationMigrationMarker?,
        receipt: NomeActivationReceipt?,
        hasUsableLocalProfile: Bool,
        hasActivationToken: Bool,
        now: Date = .now
    ) -> NomeActivationBootstrapResult {
        let shouldClearCredentials = marker == nil && !hasUsableLocalProfile
        var resolvedMarker = marker
        var resolvedReceipt = receipt ?? .unactivated(now: now)

        if shouldClearCredentials {
            resolvedReceipt = .unactivated(now: now)
        }

        if resolvedMarker == nil {
            resolvedMarker = hasUsableLocalProfile ? .grandfathered : .freshInstall
        }
        if resolvedReceipt.state == .active && !hasActivationToken {
            resolvedReceipt = .unactivated(now: now)
        }
        if resolvedMarker == .grandfathered && resolvedReceipt.state == .unactivated {
            resolvedReceipt = .grandfathered(now: now)
        }

        let evaluation = NomeActivationPolicyEvaluator.evaluate(
            policy: policy,
            receipt: resolvedReceipt,
            marker: resolvedMarker,
            now: now
        )
        return NomeActivationBootstrapResult(
            marker: resolvedMarker ?? .freshInstall,
            receipt: resolvedReceipt,
            access: evaluation.access,
            wouldBlock: evaluation.wouldBlock,
            shouldClearCredentials: shouldClearCredentials
        )
    }
}

enum NomeActivationRefreshFailure: Equatable {
    case server(code: String)
    case other
}

enum NomeActivationRefreshReducer {
    static func reducePolicy(
        current: NomeActivationPolicy?,
        fetched: NomeActivationPolicy,
        expectedPlatform: String = "ios"
    ) -> NomeActivationPolicy? {
        guard fetched.schemaVersion == 1, fetched.platform == expectedPlatform else { return current }
        guard let current else { return fetched }
        return fetched.revision >= current.revision ? fetched : current
    }

    static func reduceReceiptAfterFailure(
        receipt: NomeActivationReceipt,
        failure: NomeActivationRefreshFailure,
        now: Date = .now
    ) -> NomeActivationReceipt {
        switch failure {
        case let .server(code) where ["ACTIVATION_NOT_ACTIVE", "ACTIVATION_NOT_FOUND"].contains(code):
            var receipt = receipt
            receipt.state = .revoked
            receipt.updatedAt = now
            return receipt
        case let .server(code)
            where code == "ACTIVATION_TOKEN_EXPIRED" && (receipt.graceUntil ?? .distantPast) < now:
            var receipt = receipt
            receipt.state = .expired
            receipt.updatedAt = now
            return receipt
        default:
            return receipt
        }
    }
}

enum NomeActivationCredentialResetPolicy {
    static func requiredAfterCleanup(
        wasRequired: Bool,
        bootstrapRequestsCleanup: Bool,
        cleanupSucceeded: Bool
    ) -> Bool {
        guard wasRequired || bootstrapRequestsCleanup else { return false }
        return !cleanupSucceeded
    }
}

struct NomeActivationDefaultDenyClassifier<Command: Equatable> {
    let allowedCommands: [Command]

    func allows(_ command: Command) -> Bool {
        allowedCommands.contains { $0 == command }
    }
}

enum NomeActivationPolicyEvaluator {
    static func evaluate(
        policy: NomeActivationPolicy?,
        receipt: NomeActivationReceipt,
        marker: NomeActivationMigrationMarker?,
        now: Date = .now
    ) -> (access: NomeActivationEffectiveAccess, wouldBlock: Bool) {
        func activeReceiptIsUsable() -> Bool {
            [receipt.expiresAt, receipt.graceUntil]
                .compactMap { $0 }
                .max()
                .map { $0 >= now } ?? false
        }

        func enforcedAccess(audience: NomeActivationAudience) -> NomeActivationEffectiveAccess {
            switch receipt.state {
            case .active:
                return activeReceiptIsUsable() ? .full : .localOnly
            case .grandfathered:
                return audience == .newInstallations ? .full : .localOnly
            case .pendingMigration:
                return .migrationRequired
            case .unactivated, .paused, .revoked, .expired:
                return .localOnly
            }
        }

        guard let policy else {
            if receipt.state == .grandfathered || marker == .grandfathered {
                return (.full, false)
            }
            if receipt.state == .active, activeReceiptIsUsable() {
                return (.full, false)
            }
            if receipt.state == .pendingMigration { return (.migrationRequired, false) }
            return (.localOnly, false)
        }

        // The server resolves these cases to disabled. Re-checking them locally keeps a
        // malformed or stale response from accidentally enforcing an unsupported rollout.
        if policy.schemaVersion != 1 || !policy.platformEnabled || !policy.supported || policy.effectiveAt > now {
            return (.full, false)
        }

        switch policy.mode {
        case .disabled:
            return (.full, false)
        case .observe:
            return (.full, enforcedAccess(audience: policy.audience) != .full)
        case .enforced:
            return (enforcedAccess(audience: policy.audience), false)
        }
    }
}
