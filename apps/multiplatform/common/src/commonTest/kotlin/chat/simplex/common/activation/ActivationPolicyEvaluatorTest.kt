package chat.simplex.common.activation

import chat.simplex.common.model.CC
import chat.simplex.common.model.ChatType
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActivationPolicyEvaluatorTest {
  private val now = Instant.parse("2026-07-27T12:00:00Z")

  @Test
  fun freshInstallationWithoutValidPolicyIsLocalOnlyUntilChecked() {
    val state = ActivationPolicyEvaluator.evaluate(
      policy = null,
      policyChecked = false,
      cohort = ActivationInstallationCohort.FRESH,
      entitlement = ActivationEntitlement(),
      now = now,
    )
    assertEquals(ActivationAccess.CHECK_REQUIRED, state.access)
    assertFalse(state.permitsChatNetworking)
  }

  @Test
  fun preexistingInstallationWithoutMarkerIsGrandfathered() {
    val state = ActivationPolicyEvaluator.evaluate(
      policy = null,
      policyChecked = false,
      cohort = ActivationInstallationCohort.GRANDFATHERED,
      entitlement = ActivationEntitlement(),
      now = now,
    )
    assertEquals(ActivationAccess.FULL, state.access)
  }

  @Test
  fun disabledAndUnsupportedPoliciesArePassThrough() {
    assertEquals(ActivationAccess.FULL, evaluate(policy(mode = ActivationPolicyMode.DISABLED)).access)
    assertEquals(ActivationAccess.FULL, evaluate(policy(supported = false)).access)
    assertEquals(ActivationAccess.FULL, evaluate(policy(platformEnabled = false)).access)
  }

  @Test
  fun observeNeverBlocksButReportsWouldBlock() {
    val state = evaluate(policy(mode = ActivationPolicyMode.OBSERVE))
    assertEquals(ActivationAccess.FULL, state.access)
    assertTrue(state.wouldBlockInEnforcedMode)
  }

  @Test
  fun newInstallationAudiencePreservesGrandfatheredAccess() {
    val state = ActivationPolicyEvaluator.evaluate(
      policy = policy(audience = ActivationAudience.NEW_INSTALLATIONS),
      policyChecked = true,
      cohort = ActivationInstallationCohort.GRANDFATHERED,
      entitlement = ActivationEntitlement(),
      now = now,
    )
    assertEquals(ActivationAccess.FULL, state.access)
    assertEquals("grandfathered", state.reason)
  }

  @Test
  fun enforcedUnactivatedAndRevokedInstallationsAreLocalOnly() {
    assertEquals(ActivationAccess.LOCAL_ONLY, evaluate(policy()).access)
    assertEquals(
      ActivationAccess.LOCAL_ONLY,
      evaluate(policy(), ActivationEntitlement(status = ActivationEntitlementStatus.REVOKED)).access,
    )
  }

  @Test
  fun pendingMigrationUsesDedicatedRecoveryAccess() {
    assertEquals(
      ActivationAccess.MIGRATION_REQUIRED,
      evaluate(policy(), ActivationEntitlement(status = ActivationEntitlementStatus.PENDING_MIGRATION)).access,
    )
  }

  @Test
  fun validTokenAndOfflineGraceAllowAccessButExpiredGraceDoesNot() {
    val active = ActivationEntitlement(
      status = ActivationEntitlementStatus.ACTIVE,
      tokenExpiresAt = Instant.parse("2026-07-27T13:00:00Z"),
      offlineGraceUntil = Instant.parse("2026-08-03T13:00:00Z"),
    )
    assertEquals(ActivationAccess.FULL, evaluate(policy(), active).access)

    val grace = active.copy(tokenExpiresAt = Instant.parse("2026-07-27T11:00:00Z"))
    val graceState = evaluate(policy(), grace)
    assertEquals(ActivationAccess.FULL, graceState.access)
    assertTrue(graceState.usingOfflineGrace)

    val expired = grace.copy(offlineGraceUntil = Instant.parse("2026-07-27T11:30:00Z"))
    assertEquals(ActivationAccess.LOCAL_ONLY, evaluate(policy(), expired).access)
  }

  @Test
  fun commandClassifierAllowsOnlyExplicitLocalAndTeardownCommands() {
    assertEquals(ActivationCapability.LOCAL_READ, ActivationCommandClassifier.capability(CC.ShowActiveUser()))
    assertEquals(ActivationCapability.LOCAL_MUTATION, ActivationCommandClassifier.capability(CC.CreateActiveUser(null, false)))
    assertEquals(ActivationCapability.LOCAL_MUTATION, ActivationCommandClassifier.capability(CC.ApiChatUnread(ChatType.Direct, 1, true)))
    assertEquals(ActivationCapability.SAFE_TEARDOWN, ActivationCommandClassifier.capability(CC.ApiStopChat()))
    assertEquals(ActivationCapability.START_CHAT, ActivationCommandClassifier.capability(CC.StartChat(mainApp = true)))
    assertEquals(ActivationCapability.CONSOLE, ActivationCommandClassifier.capability(CC.Console("/_start")))
    assertEquals(ActivationCapability.MESSAGE, ActivationCommandClassifier.capability(CC.SetAllContactReceipts(true)))
  }

  private fun evaluate(
    policy: ActivationPolicy,
    entitlement: ActivationEntitlement = ActivationEntitlement(),
  ) = ActivationPolicyEvaluator.evaluate(
    policy = policy,
    policyChecked = true,
    cohort = ActivationInstallationCohort.FRESH,
    entitlement = entitlement,
    now = now,
  )

  private fun policy(
    mode: ActivationPolicyMode = ActivationPolicyMode.ENFORCED,
    audience: ActivationAudience = ActivationAudience.ALL_UNACTIVATED,
    platformEnabled: Boolean = true,
    supported: Boolean = true,
  ) = ActivationPolicy(
    schemaVersion = 1,
    revision = 7,
    platform = ActivationPlatform.ANDROID,
    build = 235,
    platformEnabled = platformEnabled,
    minimumBuild = 200,
    supported = supported,
    mode = mode,
    audience = audience,
    effectiveAt = Instant.parse("2026-07-27T00:00:00Z"),
    refreshIntervalSeconds = 300,
    serverTime = now,
  )
}
