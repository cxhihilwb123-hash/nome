package chat.simplex.common.activation

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ActivationPolicyMode {
  @SerialName("disabled") DISABLED,
  @SerialName("observe") OBSERVE,
  @SerialName("enforced") ENFORCED,
}

@Serializable
enum class ActivationAudience {
  @SerialName("new_installations") NEW_INSTALLATIONS,
  @SerialName("all_unactivated") ALL_UNACTIVATED,
}

@Serializable
enum class ActivationPlatform {
  @SerialName("android") ANDROID,
  @SerialName("ios") IOS,
}

@Serializable
data class ActivationPolicy(
  val schemaVersion: Int,
  val revision: Long,
  val platform: ActivationPlatform,
  val build: Int,
  val platformEnabled: Boolean,
  val minimumBuild: Int,
  val supported: Boolean,
  val mode: ActivationPolicyMode,
  val audience: ActivationAudience,
  val effectiveAt: Instant,
  val refreshIntervalSeconds: Long,
  val serverTime: Instant,
)

@Serializable
enum class ActivationInstallationCohort {
  @SerialName("fresh") FRESH,
  @SerialName("grandfathered") GRANDFATHERED,
}

@Serializable
enum class ActivationEntitlementStatus {
  @SerialName("unactivated") UNACTIVATED,
  @SerialName("active") ACTIVE,
  @SerialName("paused") PAUSED,
  @SerialName("pending_migration") PENDING_MIGRATION,
  @SerialName("revoked") REVOKED,
  @SerialName("expired") EXPIRED,
}

@Serializable
data class ActivationEntitlement(
  val status: ActivationEntitlementStatus = ActivationEntitlementStatus.UNACTIVATED,
  val tokenExpiresAt: Instant? = null,
  val offlineGraceUntil: Instant? = null,
  val lastServerCheckAt: Instant? = null,
)

enum class ActivationAccess {
  FULL,
  LOCAL_ONLY,
  MIGRATION_REQUIRED,
  CHECK_REQUIRED,
}

enum class ActivationCapability {
  START_CHAT,
  CONTACT,
  MESSAGE,
  FILE,
  GROUP,
  CALL,
  DEEP_LINK,
  NOTIFICATION_ACTION,
  SHARE,
  SERVICE,
  WORKER,
  BACKGROUND_RESTART,
  REMOTE_CONTROL,
  CONSOLE,
  NETWORK_CONFIGURATION,
  LOCAL_READ,
  LOCAL_MUTATION,
  SAFE_TEARDOWN,
}

data class PendingActivationIntent(
  val capability: ActivationCapability,
  val source: String,
)

data class ActivationRuntimeState(
  val policy: ActivationPolicy? = null,
  val policyChecked: Boolean = false,
  val cohort: ActivationInstallationCohort = ActivationInstallationCohort.FRESH,
  val entitlement: ActivationEntitlement = ActivationEntitlement(),
  val access: ActivationAccess = ActivationAccess.CHECK_REQUIRED,
  val reason: String = "policy_check_required",
  val usingOfflineGrace: Boolean = false,
  val wouldBlockInEnforcedMode: Boolean = false,
  val observedWouldBlockCount: Long = 0,
  val lastObservedWouldBlockCapability: ActivationCapability? = null,
  val operationInProgress: Boolean = false,
  val lastErrorCode: String? = null,
) {
  val permitsChatNetworking: Boolean get() = access == ActivationAccess.FULL
  val shouldShowActivation: Boolean get() = access != ActivationAccess.FULL

  companion object {
    fun desktopPassThrough() = ActivationRuntimeState(
      policyChecked = true,
      cohort = ActivationInstallationCohort.GRANDFATHERED,
      access = ActivationAccess.FULL,
      reason = "desktop_pass_through",
    )
  }
}

sealed class ActivationOperationResult {
  data class Success(val state: ActivationRuntimeState): ActivationOperationResult()
  data class Failure(val code: String, val message: String): ActivationOperationResult()
}

object ActivationPolicyEvaluator {
  fun evaluate(
    policy: ActivationPolicy?,
    policyChecked: Boolean,
    cohort: ActivationInstallationCohort,
    entitlement: ActivationEntitlement,
    now: Instant,
  ): ActivationRuntimeState {
    if (policy == null || !policyChecked) {
      return if (cohort == ActivationInstallationCohort.GRANDFATHERED) {
        ActivationRuntimeState(
          policy = policy,
          policyChecked = policyChecked,
          cohort = cohort,
          entitlement = entitlement,
          access = ActivationAccess.FULL,
          reason = "grandfathered_without_policy",
        )
      } else {
        ActivationRuntimeState(
          policy = policy,
          policyChecked = policyChecked,
          cohort = cohort,
          entitlement = entitlement,
          access = ActivationAccess.CHECK_REQUIRED,
          reason = "policy_check_required",
        )
      }
    }

    if (
      !policy.platformEnabled ||
      !policy.supported ||
      policy.build < policy.minimumBuild ||
      policy.mode == ActivationPolicyMode.DISABLED ||
      now < policy.effectiveAt
    ) {
      return ActivationRuntimeState(
        policy = policy,
        policyChecked = true,
        cohort = cohort,
        entitlement = entitlement,
        access = ActivationAccess.FULL,
        reason = "policy_disabled",
      )
    }

    val grandfathered =
      policy.audience == ActivationAudience.NEW_INSTALLATIONS &&
        cohort == ActivationInstallationCohort.GRANDFATHERED
    val entitlementAccess = entitlementAccess(entitlement, now)
    val wouldBlock = !grandfathered && entitlementAccess.first != ActivationAccess.FULL

    if (policy.mode == ActivationPolicyMode.OBSERVE) {
      return ActivationRuntimeState(
        policy = policy,
        policyChecked = true,
        cohort = cohort,
        entitlement = entitlement,
        access = ActivationAccess.FULL,
        reason = if (wouldBlock) "observe_would_block" else "observe_allowed",
        usingOfflineGrace = entitlementAccess.second,
        wouldBlockInEnforcedMode = wouldBlock,
      )
    }

    if (grandfathered) {
      return ActivationRuntimeState(
        policy = policy,
        policyChecked = true,
        cohort = cohort,
        entitlement = entitlement,
        access = ActivationAccess.FULL,
        reason = "grandfathered",
      )
    }

    return ActivationRuntimeState(
      policy = policy,
      policyChecked = true,
      cohort = cohort,
      entitlement = entitlement,
      access = entitlementAccess.first,
      reason = entitlementReason(entitlement, entitlementAccess.second),
      usingOfflineGrace = entitlementAccess.second,
      wouldBlockInEnforcedMode = wouldBlock,
    )
  }

  private fun entitlementAccess(
    entitlement: ActivationEntitlement,
    now: Instant,
  ): Pair<ActivationAccess, Boolean> {
    if (entitlement.status != ActivationEntitlementStatus.ACTIVE) {
      if (entitlement.status == ActivationEntitlementStatus.PENDING_MIGRATION) {
        return ActivationAccess.MIGRATION_REQUIRED to false
      }
      return ActivationAccess.LOCAL_ONLY to false
    }
    val expiresAt = entitlement.tokenExpiresAt ?: return ActivationAccess.LOCAL_ONLY to false
    if (now <= expiresAt) return ActivationAccess.FULL to false
    val graceUntil = entitlement.offlineGraceUntil
    return if (graceUntil != null && now <= graceUntil) {
      ActivationAccess.FULL to true
    } else {
      ActivationAccess.LOCAL_ONLY to false
    }
  }

  private fun entitlementReason(
    entitlement: ActivationEntitlement,
    usingOfflineGrace: Boolean,
  ): String = when {
    usingOfflineGrace -> "offline_grace"
    entitlement.status == ActivationEntitlementStatus.ACTIVE -> "activation_expired"
    else -> entitlement.status.name.lowercase()
  }
}
