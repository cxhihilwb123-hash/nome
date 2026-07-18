package chat.simplex.common.views.newchat

import chat.simplex.common.model.API
import chat.simplex.common.model.AgentErrorType
import chat.simplex.common.model.AppOpenUrl
import chat.simplex.common.model.AppOpenUrlSource
import chat.simplex.common.model.BrokerErrorType
import chat.simplex.common.model.ChatError
import chat.simplex.common.model.ChatErrorType
import chat.simplex.common.model.ConnectionPlan
import chat.simplex.common.model.ContactAddressPlan
import chat.simplex.common.model.GroupLinkPlan
import chat.simplex.common.model.InvitationLinkPlan
import chat.simplex.common.model.NetworkError
import chat.simplex.common.model.OwnerVerification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionPreviewPolicyTest {
  @Test
  fun exactlySevenNormalizedBranchesAreEligible() {
    val eligible = ConnectionPreviewPlanBranch.entries.filter {
      it.isNomeConnectionPreviewEligible()
    }

    assertEquals(
      setOf(
        ConnectionPreviewPlanBranch.InvitationOk,
        ConnectionPreviewPlanBranch.InvitationOwnLink,
        ConnectionPreviewPlanBranch.ContactOk,
        ConnectionPreviewPlanBranch.ContactOwnLink,
        ConnectionPreviewPlanBranch.ContactRepeatRequest,
        ConnectionPreviewPlanBranch.GroupOk,
        ConnectionPreviewPlanBranch.GroupRepeatJoin,
      ),
      eligible.toSet(),
    )
    assertEquals(7, eligible.size)
    assertEquals(21, ConnectionPreviewPlanBranch.entries.size)
  }

  @Test
  fun allSevenEligibleCorePlansProduceSafeUiFacts() {
    val plans = listOf(
      ConnectionPlan.InvitationLink(InvitationLinkPlan.Ok()),
      ConnectionPlan.InvitationLink(InvitationLinkPlan.OwnLink),
      ConnectionPlan.ContactAddress(ContactAddressPlan.Ok()),
      ConnectionPlan.ContactAddress(ContactAddressPlan.OwnLink),
      ConnectionPlan.ContactAddress(
        ContactAddressPlan.ConnectingConfirmReconnect,
      ),
      ConnectionPlan.GroupLink(GroupLinkPlan.Ok()),
      ConnectionPlan.GroupLink(GroupLinkPlan.ConnectingConfirmReconnect),
    )

    val models = plans.map {
      assertNotNull(
        connectionPreviewUiModel(
          connectionPlan = it,
          currentProfileName = "Local profile",
          currentProfileImage = null,
          initialIdentity = ConnectionPreviewIdentity.CurrentProfile,
        ),
      )
    }

    assertEquals(7, models.size)
    assertEquals(
      listOf(
        ConnectionPreviewWarning.None,
        ConnectionPreviewWarning.OwnLink,
        ConnectionPreviewWarning.None,
        ConnectionPreviewWarning.OwnLink,
        ConnectionPreviewWarning.RepeatRequest,
        ConnectionPreviewWarning.None,
        ConnectionPreviewWarning.RepeatJoin,
      ),
      models.map { it.warning },
    )
  }

  @Test
  fun errorAndNormalizedFallbackBranchesDoNotProducePreview() {
    val error = ConnectionPlan.Error(
      ChatError.ChatErrorChat(ChatErrorType.InvalidConnReq),
    )

    assertNull(
      connectionPreviewUiModel(
        connectionPlan = error,
        currentProfileName = "Local profile",
        currentProfileImage = null,
        initialIdentity = ConnectionPreviewIdentity.CurrentProfile,
      ),
    )
    ConnectionPreviewPlanBranch.entries
      .filterNot { it.isNomeConnectionPreviewEligible() }
      .forEach { assertFalse(it.isNomeConnectionPreviewEligible()) }
  }

  @Test
  fun ownerVerificationMapsOnlyReturnedOwnerFact() {
    val verified = connectionPreviewUiModel(
      connectionPlan = ConnectionPlan.InvitationLink(
        InvitationLinkPlan.Ok(
          ownerVerification = OwnerVerification.Verified,
        ),
      ),
      currentProfileName = "Local profile",
      currentProfileImage = null,
      initialIdentity = ConnectionPreviewIdentity.CurrentProfile,
    )
    val failed = connectionPreviewUiModel(
      connectionPlan = ConnectionPlan.ContactAddress(
        ContactAddressPlan.Ok(
          ownerVerification = OwnerVerification.Failed("signature mismatch"),
        ),
      ),
      currentProfileName = "Local profile",
      currentProfileImage = null,
      initialIdentity = ConnectionPreviewIdentity.CurrentProfile,
    )

    assertIs<ConnectionPreviewOwnerStatus.Verified>(
      assertNotNull(verified).ownerStatus,
    )
    assertEquals(
      "signature mismatch",
      assertIs<ConnectionPreviewOwnerStatus.Failed>(
        assertNotNull(failed).ownerStatus,
      ).reason,
    )
  }

  @Test
  fun contextGuardBindsBothUserAndRemoteHost() {
    val context = ConnectionPreviewAttemptContext(
      attemptId = 19L,
      userId = 7L,
      remoteHostId = 11L,
    )

    assertTrue(
      connectionPreviewContextMatches(context, 7L, 11L, 11L),
    )
    assertFalse(
      connectionPreviewContextMatches(context, 8L, 11L, 11L),
    )
    assertFalse(
      connectionPreviewContextMatches(context, 7L, 12L, 11L),
    )
    assertFalse(
      connectionPreviewContextMatches(context, 7L, 11L, 12L),
    )
  }

  @Test
  fun onlyAndroidExternalActionViewSelectsNomePreview() {
    assertEquals(
      ConnectionPreviewEntryPolicy.ExternalActionView,
      connectionPreviewEntryPolicy(
        AppOpenUrlSource.ExternalActionView,
        isAndroid = true,
      ),
    )
    assertEquals(
      ConnectionPreviewEntryPolicy.Legacy,
      connectionPreviewEntryPolicy(
        AppOpenUrlSource.InternalVerified,
        isAndroid = true,
      ),
    )
    assertEquals(
      ConnectionPreviewEntryPolicy.Legacy,
      connectionPreviewEntryPolicy(
        AppOpenUrlSource.ExternalActionView,
        isAndroid = false,
      ),
    )
  }

  @Test
  fun queuedOpenUrlRetainsIngressSourceAndHost() {
    val queued = AppOpenUrl(
      remoteHostId = 23L,
      uri = "https://simplex.chat/contact#redacted",
      source = AppOpenUrlSource.InternalVerified,
    )

    assertEquals(23L, queued.remoteHostId)
    assertEquals(AppOpenUrlSource.InternalVerified, queued.source)
  }

  @Test
  fun typedFailureMappingDoesNotExposeResponseDetails() {
    assertEquals(
      ConnectionPreviewFailureKind.InvalidLink,
      connectionPreviewFailureKind(
        API.Error(
          null,
          ChatError.ChatErrorChat(ChatErrorType.InvalidConnReq),
        ),
      ),
    )
    assertEquals(
      ConnectionPreviewFailureKind.UnsupportedLink,
      connectionPreviewFailureKind(
        API.Error(
          null,
          ChatError.ChatErrorChat(ChatErrorType.UnsupportedConnReq),
        ),
      ),
    )
    assertEquals(
      ConnectionPreviewFailureKind.Network,
      connectionPreviewFailureKind(
        API.Error(
          null,
          ChatError.ChatErrorAgent(
            AgentErrorType.BROKER(
              "example.invalid",
              BrokerErrorType.NETWORK(NetworkError.FailedError),
            ),
          ),
        ),
      ),
    )
  }
}
