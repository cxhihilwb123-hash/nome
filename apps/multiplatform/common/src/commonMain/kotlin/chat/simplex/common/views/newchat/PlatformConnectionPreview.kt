package chat.simplex.common.views.newchat

import chat.simplex.common.model.API
import chat.simplex.common.model.AgentErrorType
import chat.simplex.common.model.AppOpenUrlSource
import chat.simplex.common.model.ChatError
import chat.simplex.common.model.ChatErrorType
import chat.simplex.common.model.ConnectionPlan
import chat.simplex.common.model.ContactAddressPlan
import chat.simplex.common.model.GroupLinkPlan
import chat.simplex.common.model.InvitationLinkPlan
import chat.simplex.common.model.OwnerVerification

enum class ConnectionPreviewEntryPolicy {
  Legacy,
  ExternalActionView,
}

fun connectionPreviewEntryPolicy(
  source: AppOpenUrlSource,
  isAndroid: Boolean,
): ConnectionPreviewEntryPolicy =
  if (
    source == AppOpenUrlSource.ExternalActionView &&
    isAndroid
  ) {
    ConnectionPreviewEntryPolicy.ExternalActionView
  } else {
    ConnectionPreviewEntryPolicy.Legacy
  }

data class ConnectionPreviewAttemptContext(
  val attemptId: Long,
  val userId: Long,
  val remoteHostId: Long?,
)

enum class ConnectionPreviewKind {
  Invitation,
  ContactAddress,
  Group,
}

enum class ConnectionPreviewWarning {
  None,
  OwnLink,
  RepeatRequest,
  RepeatJoin,
}

sealed class ConnectionPreviewOwnerStatus {
  data object Absent : ConnectionPreviewOwnerStatus()
  data object Verified : ConnectionPreviewOwnerStatus()
  data class Failed(val reason: String) : ConnectionPreviewOwnerStatus()
}

enum class ConnectionPreviewIdentity {
  CurrentProfile,
  Incognito,
}

enum class ConnectionPreviewFailureKind {
  Network,
  InvalidLink,
  UnsupportedLink,
  AlreadyExists,
  ContextChanged,
  NoCurrentUser,
  Other,
}

data class ConnectionPreviewUiModel(
  val attemptId: Long = 0,
  val kind: ConnectionPreviewKind,
  val warning: ConnectionPreviewWarning,
  val ownerStatus: ConnectionPreviewOwnerStatus,
  val currentProfileName: String,
  val currentProfileImage: String?,
  val initialIdentity: ConnectionPreviewIdentity,
)

sealed class ConnectionPreviewAttemptResult {
  data object Pending : ConnectionPreviewAttemptResult()
  data class AlreadyExists(val displayName: String) : ConnectionPreviewAttemptResult()
  data class Failure(val kind: ConnectionPreviewFailureKind) : ConnectionPreviewAttemptResult()
  data object ContextChanged : ConnectionPreviewAttemptResult()
  data object NoCurrentUser : ConnectionPreviewAttemptResult()
}

sealed class ConnectionPreviewRetryResult {
  class Handoff(
    val continueFlow: suspend (
      closeCurrentPreview: () -> Unit,
      planningFailed: (ConnectionPreviewFailureKind) -> Unit,
    ) -> Unit,
  ) : ConnectionPreviewRetryResult()
}

class ConnectionPreviewReplanCallbacks(
  val closeCurrentPreview: () -> Unit,
  val planningFailed: (ConnectionPreviewFailureKind) -> Unit,
)

class ConnectionPreviewCallbacks(
  val connect: suspend (ConnectionPreviewIdentity) -> ConnectionPreviewAttemptResult,
  val retry: suspend (ConnectionPreviewIdentity) -> ConnectionPreviewRetryResult,
  val cancel: () -> Unit,
  val isActive: () -> Boolean = { true },
)

/**
 * Thin platform seam. The model contains display-safe facts only; bearer links stay captured by
 * [ConnectionPreviewCallbacks] in common controller scope.
 */
expect fun presentPlatformConnectionPreview(
  model: ConnectionPreviewUiModel,
  callbacks: ConnectionPreviewCallbacks,
): Boolean

enum class ConnectionPreviewPlanBranch {
  InvitationOk,
  InvitationOkShortLink,
  InvitationOwnLink,
  InvitationConnecting,
  InvitationKnown,
  ContactOk,
  ContactOkShortLink,
  ContactOwnLink,
  ContactRepeatRequest,
  ContactConnectingProhibit,
  ContactKnown,
  ContactViaAddress,
  GroupOk,
  GroupOkShortLink,
  GroupOwnLink,
  GroupRepeatJoin,
  GroupConnectingProhibit,
  GroupKnown,
  GroupNoRelays,
  GroupUpdateRequired,
  Error,
}

fun connectionPreviewPlanBranch(
  connectionPlan: ConnectionPlan,
): ConnectionPreviewPlanBranch = when (connectionPlan) {
  is ConnectionPlan.InvitationLink -> when (connectionPlan.invitationLinkPlan) {
    is InvitationLinkPlan.Ok ->
      if (connectionPlan.invitationLinkPlan.contactSLinkData_ == null) {
        ConnectionPreviewPlanBranch.InvitationOk
      } else {
        ConnectionPreviewPlanBranch.InvitationOkShortLink
      }
    InvitationLinkPlan.OwnLink -> ConnectionPreviewPlanBranch.InvitationOwnLink
    is InvitationLinkPlan.Connecting -> ConnectionPreviewPlanBranch.InvitationConnecting
    is InvitationLinkPlan.Known -> ConnectionPreviewPlanBranch.InvitationKnown
  }
  is ConnectionPlan.ContactAddress -> when (connectionPlan.contactAddressPlan) {
    is ContactAddressPlan.Ok ->
      if (connectionPlan.contactAddressPlan.contactSLinkData_ == null) {
        ConnectionPreviewPlanBranch.ContactOk
      } else {
        ConnectionPreviewPlanBranch.ContactOkShortLink
      }
    ContactAddressPlan.OwnLink -> ConnectionPreviewPlanBranch.ContactOwnLink
    ContactAddressPlan.ConnectingConfirmReconnect ->
      ConnectionPreviewPlanBranch.ContactRepeatRequest
    is ContactAddressPlan.ConnectingProhibit ->
      ConnectionPreviewPlanBranch.ContactConnectingProhibit
    is ContactAddressPlan.Known -> ConnectionPreviewPlanBranch.ContactKnown
    is ContactAddressPlan.ContactViaAddress ->
      ConnectionPreviewPlanBranch.ContactViaAddress
  }
  is ConnectionPlan.GroupLink -> when (connectionPlan.groupLinkPlan) {
    is GroupLinkPlan.Ok ->
      if (connectionPlan.groupLinkPlan.groupSLinkData_ == null) {
        ConnectionPreviewPlanBranch.GroupOk
      } else {
        ConnectionPreviewPlanBranch.GroupOkShortLink
      }
    is GroupLinkPlan.OwnLink -> ConnectionPreviewPlanBranch.GroupOwnLink
    GroupLinkPlan.ConnectingConfirmReconnect ->
      ConnectionPreviewPlanBranch.GroupRepeatJoin
    is GroupLinkPlan.ConnectingProhibit ->
      ConnectionPreviewPlanBranch.GroupConnectingProhibit
    is GroupLinkPlan.Known -> ConnectionPreviewPlanBranch.GroupKnown
    is GroupLinkPlan.NoRelays -> ConnectionPreviewPlanBranch.GroupNoRelays
    is GroupLinkPlan.UpdateRequired -> ConnectionPreviewPlanBranch.GroupUpdateRequired
  }
  is ConnectionPlan.Error -> ConnectionPreviewPlanBranch.Error
}

fun ConnectionPreviewPlanBranch.isNomeConnectionPreviewEligible(): Boolean =
  when (this) {
    ConnectionPreviewPlanBranch.InvitationOk,
    ConnectionPreviewPlanBranch.InvitationOwnLink,
    ConnectionPreviewPlanBranch.ContactOk,
    ConnectionPreviewPlanBranch.ContactOwnLink,
    ConnectionPreviewPlanBranch.ContactRepeatRequest,
    ConnectionPreviewPlanBranch.GroupOk,
    ConnectionPreviewPlanBranch.GroupRepeatJoin -> true
    ConnectionPreviewPlanBranch.InvitationOkShortLink,
    ConnectionPreviewPlanBranch.InvitationConnecting,
    ConnectionPreviewPlanBranch.InvitationKnown,
    ConnectionPreviewPlanBranch.ContactOkShortLink,
    ConnectionPreviewPlanBranch.ContactConnectingProhibit,
    ConnectionPreviewPlanBranch.ContactKnown,
    ConnectionPreviewPlanBranch.ContactViaAddress,
    ConnectionPreviewPlanBranch.GroupOkShortLink,
    ConnectionPreviewPlanBranch.GroupOwnLink,
    ConnectionPreviewPlanBranch.GroupConnectingProhibit,
    ConnectionPreviewPlanBranch.GroupKnown,
    ConnectionPreviewPlanBranch.GroupNoRelays,
    ConnectionPreviewPlanBranch.GroupUpdateRequired,
    ConnectionPreviewPlanBranch.Error -> false
  }

fun connectionPreviewUiModel(
  connectionPlan: ConnectionPlan,
  currentProfileName: String,
  currentProfileImage: String?,
  initialIdentity: ConnectionPreviewIdentity,
  attemptId: Long = 0,
): ConnectionPreviewUiModel? {
  if (!connectionPreviewPlanBranch(connectionPlan).isNomeConnectionPreviewEligible()) return null
  val kind: ConnectionPreviewKind
  val warning: ConnectionPreviewWarning
  val ownerVerification: OwnerVerification?
  when (connectionPlan) {
    is ConnectionPlan.InvitationLink -> {
      kind = ConnectionPreviewKind.Invitation
      when (val plan = connectionPlan.invitationLinkPlan) {
        is InvitationLinkPlan.Ok -> {
          warning = ConnectionPreviewWarning.None
          ownerVerification = plan.ownerVerification
        }
        InvitationLinkPlan.OwnLink -> {
          warning = ConnectionPreviewWarning.OwnLink
          ownerVerification = null
        }
        else -> return null
      }
    }
    is ConnectionPlan.ContactAddress -> {
      kind = ConnectionPreviewKind.ContactAddress
      when (val plan = connectionPlan.contactAddressPlan) {
        is ContactAddressPlan.Ok -> {
          warning = ConnectionPreviewWarning.None
          ownerVerification = plan.ownerVerification
        }
        ContactAddressPlan.OwnLink -> {
          warning = ConnectionPreviewWarning.OwnLink
          ownerVerification = null
        }
        ContactAddressPlan.ConnectingConfirmReconnect -> {
          warning = ConnectionPreviewWarning.RepeatRequest
          ownerVerification = null
        }
        else -> return null
      }
    }
    is ConnectionPlan.GroupLink -> {
      kind = ConnectionPreviewKind.Group
      when (val plan = connectionPlan.groupLinkPlan) {
        is GroupLinkPlan.Ok -> {
          warning = ConnectionPreviewWarning.None
          ownerVerification = plan.ownerVerification
        }
        GroupLinkPlan.ConnectingConfirmReconnect -> {
          warning = ConnectionPreviewWarning.RepeatJoin
          ownerVerification = null
        }
        else -> return null
      }
    }
    is ConnectionPlan.Error -> return null
  }
  return ConnectionPreviewUiModel(
    attemptId = attemptId,
    kind = kind,
    warning = warning,
    ownerStatus = when (ownerVerification) {
      null -> ConnectionPreviewOwnerStatus.Absent
      OwnerVerification.Verified -> ConnectionPreviewOwnerStatus.Verified
      is OwnerVerification.Failed ->
        ConnectionPreviewOwnerStatus.Failed(ownerVerification.reason)
    },
    currentProfileName = currentProfileName,
    currentProfileImage = currentProfileImage,
    initialIdentity = initialIdentity,
  )
}

fun connectionPreviewFailureKind(response: API): ConnectionPreviewFailureKind {
  val error = (response as? API.Error)?.err ?: return ConnectionPreviewFailureKind.Other
  return when {
    error is ChatError.ChatErrorChat &&
        error.errorType is ChatErrorType.InvalidConnReq ->
      ConnectionPreviewFailureKind.InvalidLink
    error is ChatError.ChatErrorChat &&
        error.errorType is ChatErrorType.UnsupportedConnReq ->
      ConnectionPreviewFailureKind.UnsupportedLink
    error is ChatError.ChatErrorAgent &&
        error.agentError is AgentErrorType.BROKER ->
      ConnectionPreviewFailureKind.Network
    error is ChatError.ChatErrorAgent &&
        error.agentError is AgentErrorType.PROXY ->
      ConnectionPreviewFailureKind.Network
    error is ChatError.ChatErrorAgent &&
        error.agentError is AgentErrorType.SMP ->
      ConnectionPreviewFailureKind.Network
    else -> ConnectionPreviewFailureKind.Other
  }
}

fun connectionPreviewContextMatches(
  context: ConnectionPreviewAttemptContext,
  currentUserId: Long?,
  currentUserRemoteHostId: Long?,
  currentControllerRemoteHostId: Long?,
): Boolean =
  context.userId == currentUserId &&
      context.remoteHostId == currentUserRemoteHostId &&
      context.remoteHostId == currentControllerRemoteHostId
