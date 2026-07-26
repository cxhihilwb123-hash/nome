package chat.simplex.common.activation

import androidx.compose.runtime.Composable
import chat.simplex.common.model.CC
import chat.simplex.common.platform.appPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ActivationRuntimeDelegate {
  suspend fun refreshPolicy(force: Boolean = false): ActivationOperationResult
  suspend fun redeem(inviteCode: String): ActivationOperationResult
  suspend fun refreshEntitlement(): ActivationOperationResult
  suspend fun migrateInstallation(): ActivationOperationResult
  suspend fun refreshBeforeProtectedAction()
  fun recordWouldBlock(capability: ActivationCapability)
}

class ActivationRequiredException(
  val capability: ActivationCapability,
): IllegalStateException("Activation is required for ${capability.name.lowercase()}")

object ActivationGate {
  private val mutableState = MutableStateFlow(
    if (appPlatform.isDesktop) {
      ActivationRuntimeState.desktopPassThrough()
    } else {
      ActivationRuntimeState()
    },
  )
  val state: StateFlow<ActivationRuntimeState> = mutableState.asStateFlow()

  private val mutableSheetVisible = MutableStateFlow(false)
  val sheetVisible: StateFlow<Boolean> = mutableSheetVisible.asStateFlow()

  private val mutablePendingIntent = MutableStateFlow<PendingActivationIntent?>(null)
  val pendingIntent: StateFlow<PendingActivationIntent?> = mutablePendingIntent.asStateFlow()

  private val mutablePendingReviewVisible = MutableStateFlow(false)
  val pendingReviewVisible: StateFlow<Boolean> = mutablePendingReviewVisible.asStateFlow()

  private var delegate: ActivationRuntimeDelegate? = null
  private var networkTransitionHandler: ((Boolean) -> Unit)? = null

  fun install(runtime: ActivationRuntimeDelegate, initialState: ActivationRuntimeState) {
    delegate = runtime
    publish(initialState)
  }

  fun installNetworkTransitionHandler(handler: (Boolean) -> Unit) {
    networkTransitionHandler = handler
  }

  fun publish(newState: ActivationRuntimeState) {
    val oldPermitted = mutableState.value.permitsChatNetworking
    mutableState.value = newState
    val newPermitted = newState.permitsChatNetworking
    if (
      !oldPermitted &&
      newPermitted &&
      mutablePendingIntent.value?.capability == ActivationCapability.DEEP_LINK
    ) {
      mutablePendingReviewVisible.value = true
    }
    if (oldPermitted != newPermitted) networkTransitionHandler?.invoke(newPermitted)
  }

  fun notifyControllerReady() {
    if (state.value.permitsChatNetworking) networkTransitionHandler?.invoke(true)
  }

  fun permits(capability: ActivationCapability): Boolean {
    if (capability.isAlwaysLocal()) return true
    return state.value.permitsChatNetworking
  }

  fun guard(
    capability: ActivationCapability,
    source: String,
    revealActivation: Boolean = true,
  ): Boolean {
    val current = state.value
    if (current.policy?.mode == ActivationPolicyMode.OBSERVE && current.wouldBlockInEnforcedMode) {
      delegate?.recordWouldBlock(capability)
      return true
    }
    if (permits(capability)) return true
    if (revealActivation) {
      mutablePendingIntent.value = PendingActivationIntent(capability, source)
      mutablePendingReviewVisible.value = false
      mutableSheetVisible.value = true
    }
    return false
  }

  suspend fun guardFresh(
    capability: ActivationCapability,
    source: String,
    revealActivation: Boolean = true,
  ): Boolean {
    if (!capability.isAlwaysLocal()) delegate?.refreshBeforeProtectedAction()
    return guard(capability, source, revealActivation)
  }

  suspend fun enforceCommand(cmd: CC, remote: Boolean = false) {
    val capability = if (remote) ActivationCapability.REMOTE_CONTROL else ActivationCommandClassifier.capability(cmd)
    if (!guardFresh(capability, source = "core_command:${cmd::class.simpleName ?: "unknown"}")) {
      throw ActivationRequiredException(capability)
    }
  }

  fun showActivation(source: String = "activation_card") {
    if (mutablePendingIntent.value == null) {
      mutablePendingIntent.value = PendingActivationIntent(ActivationCapability.START_CHAT, source)
    }
    mutableSheetVisible.value = true
  }

  fun dismissActivation() {
    mutableSheetVisible.value = false
  }

  fun hasBlockedPending(capability: ActivationCapability): Boolean =
    mutablePendingIntent.value?.capability == capability

  fun deferPendingReview() {
    mutablePendingReviewVisible.value = false
  }

  fun showPendingReview() {
    if (mutablePendingIntent.value?.capability == ActivationCapability.DEEP_LINK) {
      mutablePendingReviewVisible.value = true
    }
  }

  fun clearPendingIntent() {
    mutablePendingIntent.value = null
    mutablePendingReviewVisible.value = false
  }

  suspend fun refreshPolicy(force: Boolean = false): ActivationOperationResult =
    delegate?.refreshPolicy(force)
      ?: ActivationOperationResult.Failure("activation_unavailable", "Activation runtime is unavailable")

  suspend fun redeem(inviteCode: String): ActivationOperationResult =
    delegate?.redeem(inviteCode)
      ?: ActivationOperationResult.Failure("activation_unavailable", "Activation runtime is unavailable")

  suspend fun refreshEntitlement(): ActivationOperationResult =
    delegate?.refreshEntitlement()
      ?: ActivationOperationResult.Failure("activation_unavailable", "Activation runtime is unavailable")

  suspend fun migrateInstallation(): ActivationOperationResult =
    delegate?.migrateInstallation()
      ?: ActivationOperationResult.Failure("activation_unavailable", "Activation runtime is unavailable")

  private fun ActivationCapability.isAlwaysLocal(): Boolean = when (this) {
    ActivationCapability.LOCAL_READ,
    ActivationCapability.LOCAL_MUTATION,
    ActivationCapability.SAFE_TEARDOWN -> true
    else -> false
  }
}

object ActivationCommandClassifier {
  fun capability(cmd: CC): ActivationCapability = when (cmd) {
    is CC.ApiDeleteUser ->
      if (cmd.delSMPQueues) ActivationCapability.CONTACT else ActivationCapability.LOCAL_MUTATION

    is CC.ShowActiveUser,
    is CC.ListUsers,
    is CC.CheckChatRunning,
    is CC.ApiGetSettings,
    is CC.ApiGetChatTags,
    is CC.ApiGetChats,
    is CC.ApiGetChat,
    is CC.ApiGetChatContentTypes,
    is CC.ApiGetChatItemInfo,
    is CC.ApiGetReactionMembers,
    is CC.ApiPlanForwardChatItems,
    is CC.ApiGetGroupRelays,
    is CC.ApiListMembers,
    is CC.APIGetGroupLink,
    is CC.ApiGetServerOperators,
    is CC.ApiGetUserServers,
    is CC.ApiGetUsageConditions,
    is CC.APIGetChatItemTTL,
    is CC.APIGetNetworkConfig,
    is CC.ApiGetUpdatedGroupLinkData,
    is CC.APIContactInfo,
    is CC.APIGroupMemberInfo,
    is CC.APIContactQueueInfo,
    is CC.APIGroupMemberQueueInfo,
    is CC.APIGetContactCode,
    is CC.APIGetGroupMemberCode,
    is CC.ApiListContacts,
    is CC.ApiShowMyAddress,
    is CC.ApiGetCallInvitations,
    is CC.ShowVersion,
    is CC.GetAgentSubsTotal,
    is CC.GetAgentServersSummary -> ActivationCapability.LOCAL_READ

    is CC.CreateActiveUser,
    is CC.ApiSetActiveUser,
    is CC.ApiHideUser,
    is CC.ApiUnhideUser,
    is CC.ApiMuteUser,
    is CC.ApiUnmuteUser,
    is CC.ApiSetAppFilePaths,
    is CC.ApiSetEncryptLocalFiles,
    is CC.ApiExportArchive,
    is CC.ApiImportArchive,
    is CC.ApiDeleteStorage,
    is CC.ApiStorageEncryption,
    is CC.TestStorageEncryption,
    is CC.ApiSaveSettings,
    is CC.ApiCreateChatTag,
    is CC.ApiSetChatTags,
    is CC.ApiDeleteChatTag,
    is CC.ApiUpdateChatTag,
    is CC.ApiReorderChatTags,
    is CC.ApiCreateChatItems,
    is CC.ApiSetServerOperators,
    is CC.ApiSetUserServers,
    is CC.ApiAcceptConditions,
    is CC.ApiSetContactAlias,
    is CC.ApiSetGroupAlias,
    is CC.ApiSetConnectionAlias,
    is CC.ApiSetUserUIThemes,
    is CC.ApiSetChatUIThemes,
    is CC.ApiSetConditionsNotified,
    is CC.ApiChatRead,
    is CC.ApiChatItemsRead,
    is CC.ApiChatUnread,
    is CC.ResetAgentServersStats -> ActivationCapability.LOCAL_MUTATION

    is CC.ApiStopChat,
    is CC.ApiRejectCall,
    is CC.ApiEndCall,
    is CC.CancelFile,
    is CC.StopRemoteHost,
    is CC.StopRemoteCtrl -> ActivationCapability.SAFE_TEARDOWN

    is CC.StartChat -> ActivationCapability.START_CHAT
    is CC.Console -> ActivationCapability.CONSOLE
    is CC.APISetNetworkConfig,
    is CC.APISetNetworkInfo,
    is CC.ReconnectServer,
    is CC.ReconnectAllServers,
    is CC.APITestProtoServer,
    is CC.APITestChatRelay -> ActivationCapability.NETWORK_CONFIGURATION

    is CC.ApiSendMessages,
    is CC.ApiReportMessage,
    is CC.ApiUpdateChatItem,
    is CC.ApiDeleteChatItem,
    is CC.ApiDeleteMemberChatItem,
    is CC.ApiArchiveReceivedReports,
    is CC.ApiDeleteReceivedReports,
    is CC.ApiChatItemReaction,
    is CC.ApiForwardChatItems,
    is CC.ApiShareChatMsgContent -> ActivationCapability.MESSAGE

    is CC.ApiNewGroup,
    is CC.ApiNewPublicGroup,
    is CC.ApiAddGroupRelays,
    is CC.ApiAddMember,
    is CC.ApiJoinGroup,
    is CC.ApiAcceptMember,
    is CC.ApiDeleteMemberSupportChat,
    is CC.ApiMembersRole,
    is CC.ApiBlockMembersForAll,
    is CC.ApiRemoveMembers,
    is CC.ApiLeaveGroup,
    is CC.ApiUpdateGroupProfile,
    is CC.APICreateGroupLink,
    is CC.APIGroupLinkMemberRole,
    is CC.APIDeleteGroupLink,
    is CC.ApiAddGroupShortLink,
    is CC.APICreateMemberContact,
    is CC.APISendMemberContactInvitation,
    is CC.APIAcceptMemberContact,
    is CC.ApiSetMemberSettings,
    is CC.APISwitchGroupMember,
    is CC.APIAbortSwitchGroupMember,
    is CC.APISyncGroupMemberRatchet,
    is CC.APIVerifyGroupMember -> ActivationCapability.GROUP

    is CC.APIAddContact,
    is CC.ApiSetConnectionIncognito,
    is CC.ApiChangeConnectionUser,
    is CC.APIConnectPlan,
    is CC.APIPrepareContact,
    is CC.APIPrepareGroup,
    is CC.APIChangePreparedContactUser,
    is CC.APIChangePreparedGroupUser,
    is CC.APIConnectPreparedContact,
    is CC.APIConnectPreparedGroup,
    is CC.APIConnect,
    is CC.ApiConnectContactViaAddress,
    is CC.ApiUpdateProfile,
    is CC.ApiSetContactPrefs,
    is CC.ApiCreateMyAddress,
    is CC.ApiDeleteMyAddress,
    is CC.ApiAddMyAddressShortLink,
    is CC.ApiSetProfileAddress,
    is CC.ApiSetAddressSettings,
    is CC.ApiAcceptContact,
    is CC.ApiRejectContact,
    is CC.APISwitchContact,
    is CC.APIAbortSwitchContact,
    is CC.APISyncContactRatchet,
    is CC.APIVerifyContact -> ActivationCapability.CONTACT

    is CC.ApiSendCallInvitation,
    is CC.ApiSendCallOffer,
    is CC.ApiSendCallAnswer,
    is CC.ApiSendCallExtraInfo,
    is CC.ApiCallStatus -> ActivationCapability.CALL

    is CC.ReceiveFile,
    is CC.StoreRemoteFile,
    is CC.GetRemoteFile,
    is CC.ApiUploadStandaloneFile,
    is CC.ApiDownloadStandaloneFile,
    is CC.ApiStandaloneFileInfo -> ActivationCapability.FILE

    is CC.SetLocalDeviceName,
    is CC.ListRemoteHosts,
    is CC.StartRemoteHost,
    is CC.SwitchRemoteHost,
    is CC.DeleteRemoteHost,
    is CC.ConnectRemoteCtrl,
    is CC.FindKnownRemoteCtrl,
    is CC.ConfirmRemoteCtrl,
    is CC.VerifyRemoteCtrlSession,
    is CC.ListRemoteCtrls,
    is CC.DeleteRemoteCtrl -> ActivationCapability.REMOTE_CONTROL

    // Any command not deliberately classified as local or teardown is denied in enforced mode.
    else -> ActivationCapability.MESSAGE
  }
}

@Composable
expect fun PlatformActivationOverlay()
