package chat.simplex.app.nome.lifecycle

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.ChatController
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatItem
import chat.simplex.common.model.ChatListLoadResult
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.ChatPagination
import chat.simplex.common.model.ChatType
import chat.simplex.common.model.CIDeleteMode
import chat.simplex.common.model.ComposedMessage
import chat.simplex.common.model.CryptoFile
import chat.simplex.common.model.GroupFeatureEnabled
import chat.simplex.common.model.GroupPreference
import chat.simplex.common.model.GroupPreferences
import chat.simplex.common.model.GroupProfile
import chat.simplex.common.model.MsgContent
import chat.simplex.common.views.chatlist.openGroupChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NomePublicChannelProducerTest {
  @Test
  fun realPublicChannelLifecycleUsesOnlyOfficialCoreResults() {
    val arguments =
      InstrumentationRegistry.getArguments()
    val skipControlledProducer =
      controlledProducerSkipRequested(
        arguments.getString(
          CONTROLLED_PRODUCER_SKIP_ARGUMENT,
        ),
      )
    val actionArgument =
      arguments.getString(PUBLIC_CHANNEL_ACTION_ARGUMENT)
    val action =
      when {
        actionArgument == null -> {
          if (skipControlledProducer) return
          error(
            "Missing $PUBLIC_CHANNEL_ACTION_ARGUMENT; " +
              "set $CONTROLLED_PRODUCER_SKIP_ARGUMENT=true " +
              "to bypass this controlled harness in general regression",
          )
        }

        else ->
          ProducerAction.entries.firstOrNull { action ->
            action.argument == actionArgument
          }
            ?: error(
              "Invalid $PUBLIC_CHANNEL_ACTION_ARGUMENT: $actionArgument",
            )
      }
    if (action.usesControlledFixtureRecord) {
      requireFixtureNonce()
    }
    val scenario = launchControlledMainActivity()
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true &&
          ChatModel.chats.value.isNotEmpty()
      }
      when (action) {
        ProducerAction.Create -> createChannel()
        ProducerAction.Rename -> renameChannelForVisualAcceptance()
        ProducerAction.Populate -> populateChannel()
        ProducerAction.Attach -> attachRealControlledFile()
        ProducerAction.Open ->
          openChannel(
            requireNotNull(scenario) {
              "Opening the controlled channel requires an ActivityScenario host"
            },
          )
        ProducerAction.Delete -> deleteChannel()
        ProducerAction.Status -> reportControlledChannelStatus()
        ProducerAction.Members -> reportControlledMemberStates()
        ProducerAction.Connections -> reportPendingGroupLinkStates()
        ProducerAction.Prepared -> reportPreparedPublicChannelStates()
        ProducerAction.CaptureLifecycle -> {
          createChannel()
          populateChannel()
          attachRealControlledFile()
          openChannel(
            requireNotNull(scenario) {
              "Capturing the controlled channel lifecycle requires an ActivityScenario host"
            },
          )
          deleteChannel()
        }
      }
    } finally {
      scenario?.close()
    }
  }

  private fun createChannel() {
    assertFalse(
      "The controlled producer must not replace an existing fixture record",
      fixtureRecordFile().exists(),
    )
    val initialIds = publicChannelIds()
    val availableRelayIds =
      requireNotNull(
        runBlocking {
          ChatModel.controller.getUserServers(
            null,
          )
        },
      ).asSequence()
        .filter {
          it.operator?.enabled != false
        }.flatMap {
          it.chatRelays.asSequence()
        }.filter {
          it.enabled &&
            !it.deleted &&
            it.chatRelayId != null
        }.mapNotNull {
          it.chatRelayId
        }.distinct()
        .toList()
    val relayOffset =
      optionalRelayOffsetArgument()?.let {
          if (availableRelayIds.isEmpty()) {
            0
          } else {
            it.mod(availableRelayIds.size)
          }
        }
        ?: 0
    val relayIds =
      (
        availableRelayIds.drop(relayOffset) +
          availableRelayIds.take(relayOffset)
      ).take(
        optionalRelayCountArgument() ?: MAX_RELAYS,
      )
    assertTrue(
      "The controlled producer requires at least one enabled configured chat relay",
      relayIds.isNotEmpty(),
    )

    val result =
      try {
        runBlocking {
          ChatModel.controller.apiNewPublicGroup(
            rh = null,
            incognito = false,
            relayIds = relayIds,
            groupProfile =
              GroupProfile(
                displayName = CONTROLLED_CHANNEL_NAME,
                fullName = "",
                shortDescr = null,
                image = null,
                groupPreferences =
                  GroupPreferences(
                    history =
                      GroupPreference(
                        GroupFeatureEnabled.ON,
                      ),
                    support =
                      GroupPreference(
                        GroupFeatureEnabled.OFF,
                      ),
                  ),
              ),
          )
        }
      } catch (error: Exception) {
        fail(
          "Official public-channel creation failed; " +
            "safeOutcome=${safeFailure(error)}",
        )
        return
      }
    when (result) {
      is ChatController.PublicGroupCreationResult.Created -> {
        assertTrue(
          "The official producer must return a public channel",
          result.groupInfo.isChannel,
        )
        assertTrue(
          "The official producer must return a non-empty group link",
          result.groupLink.connLinkContact.connFullLink.isNotBlank(),
        )
        assertTrue(
          "The official producer must return its relay progression",
          result.groupRelays.isNotEmpty(),
        )
        runBlocking {
          withContext(Dispatchers.Main) {
            ChatModel.chatsContext.updateGroup(
              null,
              result.groupInfo,
            )
          }
        }
        persistFixtureRecord(
          result.groupInfo.groupId,
          requireFixtureNonce(),
        )
        waitUntil(CREATION_TIMEOUT_MILLIS) {
          result.groupInfo.id !in initialIds &&
            controlledChannel()
              ?.groupInfo
              ?.groupId ==
            result.groupInfo.groupId
        }
        Log.i(
          TAG,
          "publicChannelCreated=true " +
            "configuredRelayCount=${relayIds.size} " +
            "returnedRelayCount=${result.groupRelays.size} " +
            "officialLinkPresent=true",
        )
      }

      is ChatController.PublicGroupCreationResult.CreationFailed -> {
        fail(
          "Official public-channel creation failed; " +
            "safeOutcome=relay-failures/" +
            result.addRelayResults.size,
        )
      }

      null -> {
        fail(
          "Official public-channel creation failed; " +
            "safeOutcome=null-result",
        )
      }
    }
  }

  private fun deleteChannel() {
    val channel =
      controlledChannel()
        ?.groupInfo
    assertTrue(
      "A controlled public channel must exist before deletion",
      channel != null,
    )
    val groupInfo = requireNotNull(channel)
    val deleted =
      runBlocking {
        ChatModel.controller.apiDeleteChat(
          rh = null,
          type = ChatType.Group,
          id = groupInfo.apiId,
        )
      }
    assertTrue(
      "The official delete command must confirm public-channel deletion",
      deleted,
    )
    runBlocking {
      withContext(Dispatchers.Main) {
        ChatModel.chatsContext.removeChat(
          null,
          groupInfo.id,
        )
        if (
          ChatModel.chatId.value ==
          groupInfo.id
        ) {
          ChatModel.chatId.value = null
        }
      }
    }
    waitUntil(DELETION_TIMEOUT_MILLIS) {
      groupInfo.id !in publicChannelIds()
    }
    assertFalse(
      "The deleted public channel must not remain in the loaded model",
      groupInfo.id in publicChannelIds(),
    )
    assertTrue(
      "The exact controlled fixture record must be removed after deletion",
      fixtureRecordFile().delete(),
    )
    Log.i(
      TAG,
      "publicChannelDeleted=true",
    )
    File(
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
        .cacheDir,
      CONTROLLED_ATTACHMENT_NAME,
    ).delete()
  }

  private fun populateChannel() {
    val channel =
      requireNotNull(
        controlledChannel(),
      )
    val existingTexts =
      controlledChannelTexts(
        channel,
      )
    val missing =
      CONTROLLED_POSTS.filterNot {
        it in existingTexts
      }
    if (missing.isNotEmpty()) {
      val sent =
        runBlocking {
          ChatModel.controller.apiSendMessages(
            rh = null,
            type = ChatType.Group,
            id = channel.apiId,
            scope = channel.groupChatScope(),
            sendAsGroup = channel.sendAsGroup,
            composedMessages =
              missing.map {
                ComposedMessage(
                  fileSource = null,
                  quotedItemId = null,
                  msgContent = MsgContent.MCText(it),
                  mentions = emptyMap(),
                )
              },
          )
        }
      assertTrue(
        "The official channel owner must create every missing controlled post",
        sent?.size == missing.size,
      )
    }
    waitUntil(CREATION_TIMEOUT_MILLIS) {
      CONTROLLED_POSTS.all {
        it in controlledChannelTexts(
          channel,
        )
      }
    }
    Log.i(
      TAG,
      "controlledChannelPostsPresent=" +
        CONTROLLED_POSTS.size,
    )
  }

  private fun renameChannelForVisualAcceptance() {
    val channel =
      requireNotNull(
        controlledChannel(),
      )
    if (
      channel.groupInfo.displayName ==
      CONTROLLED_CHANNEL_NAME
    ) {
      return
    }
    val updated =
      requireNotNull(
        runBlocking {
          ChatModel.controller.apiSetGroupAlias(
            rh = null,
            groupId = channel.groupInfo.groupId,
            localAlias = CONTROLLED_CHANNEL_NAME,
          )
        },
      )
    runBlocking {
      withContext(Dispatchers.Main) {
        ChatModel.chatsContext.updateGroup(
          null,
          updated,
        )
      }
    }
    waitUntil(CREATION_TIMEOUT_MILLIS) {
      controlledChannel()
        ?.groupInfo
        ?.displayName ==
        CONTROLLED_CHANNEL_NAME
    }
    Log.i(
      TAG,
      "controlledChannelDisplayNameUpdated=true",
    )
  }

  private fun attachRealControlledFile() {
    val channel =
      requireNotNull(
        controlledChannel(),
      )
    if (
      controlledChannelItems(channel).none {
        it.file?.fileName ==
          CONTROLLED_ATTACHMENT_NAME &&
          it.meta.itemDeleted == null
      }
    ) {
      val fixture =
        File(
          InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .cacheDir,
          CONTROLLED_ATTACHMENT_NAME,
        ).also {
          it.writeText(
            CONTROLLED_ATTACHMENT_CONTENT,
          )
        }
      val sent =
        runBlocking {
          ChatModel.controller.apiSendMessages(
            rh = null,
            type = ChatType.Group,
            id = channel.apiId,
            scope = channel.groupChatScope(),
            sendAsGroup = channel.sendAsGroup,
            composedMessages =
              listOf(
                ComposedMessage(
                  fileSource =
                    CryptoFile.plain(
                      fixture.absolutePath,
                    ),
                  quotedItemId = null,
                  msgContent =
                    MsgContent.MCFile(
                      CONTROLLED_ATTACHMENT_POST,
                    ),
                  mentions = emptyMap(),
                ),
              ),
          )
        }
      assertTrue(
        "The official channel owner must create the real controlled attachment item",
        sent
          ?.singleOrNull()
          ?.chatItem
          ?.file
          ?.fileName ==
          CONTROLLED_ATTACHMENT_NAME,
      )
    }
    waitUntil(CREATION_TIMEOUT_MILLIS) {
      controlledChannelItems(channel).any {
        it.file?.fileName ==
          CONTROLLED_ATTACHMENT_NAME &&
          it.meta.itemDeleted == null
      }
    }
    val oldTextItem =
      controlledChannelItems(channel)
        .firstOrNull {
          it.text ==
            SUPERSEDED_CONTROLLED_POST &&
            it.meta.itemDeleted == null
        }
    if (oldTextItem != null) {
      val deleted =
        runBlocking {
          ChatModel.controller.apiDeleteChatItems(
            rh = null,
            type = ChatType.Group,
            id = channel.apiId,
            scope = channel.groupChatScope(),
            itemIds = listOf(oldTextItem.id),
            mode = CIDeleteMode.cidmInternal,
          )
        }
      assertTrue(
        "The official history deletion must replace the superseded controlled text post",
        !deleted.isNullOrEmpty(),
      )
    }
    waitUntil(CREATION_TIMEOUT_MILLIS) {
      controlledChannelItems(channel).none {
        it.text ==
          SUPERSEDED_CONTROLLED_POST &&
          it.meta.itemDeleted == null
      }
    }
    Log.i(
      TAG,
      "controlledAttachmentItemPresent=true " +
        "supersededControlledTextPresent=false",
    )
  }

  private fun reportControlledChannelStatus() {
    val publicChannelCount =
      ChatModel.chats.value
        .count {
          (it.chatInfo as? ChatInfo.Group)
            ?.groupInfo
            ?.takeIf { group ->
              group.useRelays
            } != null
        }
    val fixtureRecordPresent =
      loadFixtureRecord() != null
    val controlledChannelPresent =
      controlledChannel() != null
    InstrumentationRegistry
      .getInstrumentation()
      .addResults(
        Bundle().apply {
          putInt(
            "publicChannelCount",
            publicChannelCount,
          )
          putBoolean(
            "controlledFixtureRecordPresent",
            fixtureRecordPresent,
          )
          putBoolean(
            "controlledChannelPresent",
            controlledChannelPresent,
          )
        },
      )
    Log.i(
      TAG,
      "controlledChannelPresent=" +
        controlledChannelPresent +
        " publicChannelCount=" +
        publicChannelCount,
    )
  }

  private fun reportControlledMemberStates() {
    val channel =
      requireNotNull(
        controlledChannel(),
      )
    val members =
      runBlocking {
        ChatModel.controller.apiListMembers(
          rh = null,
          groupId = channel.groupInfo.groupId,
        )
      }
    val stateCounts =
      members
        .groupingBy {
          "${it.memberRole.name}/${it.memberStatus.name}"
        }.eachCount()
        .toSortedMap()
    val observerMembers =
      members.filter {
        it.memberRole.name == "Observer"
      }
    val summary =
      stateCounts.entries.joinToString("|") {
        "${it.key}=${it.value}"
      }.ifEmpty {
        "none"
      }
    InstrumentationRegistry
      .getInstrumentation()
      .addResults(
        Bundle().apply {
          putString(
            "safeMemberStateCounts",
            summary,
          )
          putInt(
            "safeObserverCount",
            observerMembers.size,
          )
          putInt(
            "safeObserverActiveCount",
            observerMembers.count {
              it.memberActive
            },
          )
          putInt(
            "safeObserverCurrentCount",
            observerMembers.count {
              it.memberCurrent
            },
          )
          putInt(
            "safeObserverPendingCount",
            observerMembers.count {
              it.memberPending
            },
          )
        },
      )
    Log.i(
      TAG,
      "memberStateCounts=$summary " +
        "observerCount=${observerMembers.size} " +
        "observerActiveCount=" +
        observerMembers.count {
          it.memberActive
        } +
        " observerCurrentCount=" +
        observerMembers.count {
          it.memberCurrent
        } +
        " observerPendingCount=" +
        observerMembers.count {
          it.memberPending
        },
    )
  }

  private fun reportPendingGroupLinkStates() {
    val pendingConnections =
      ChatModel.chats.value
        .mapNotNull {
          (it.chatInfo as? ChatInfo.ContactConnection)
            ?.contactConnection
        }.filter {
          it.groupLinkId != null
        }
    val stateCounts =
      pendingConnections
        .groupingBy {
          requireNotNull(
            it.pccConnStatus::class.simpleName,
          )
        }.eachCount()
        .toSortedMap()
    val summary =
      stateCounts.entries.joinToString("|") {
        "${it.key}=${it.value}"
      }.ifEmpty {
        "none"
      }
    InstrumentationRegistry
      .getInstrumentation()
      .addResults(
        Bundle().apply {
          putString(
            "safePendingGroupLinkStateCounts",
            summary,
          )
          putInt(
            "safePendingGroupLinkConnectionCount",
            pendingConnections.size,
          )
        },
      )
    Log.i(
      TAG,
      "pendingGroupLinkStateCounts=$summary " +
        "pendingGroupLinkConnectionCount=" +
        pendingConnections.size,
    )
  }

  private fun reportPreparedPublicChannelStates() {
    val user =
      requireNotNull(
        ChatModel.currentUser.value,
      )
    val modelGroups =
      ChatModel.chats.value
        .mapNotNull {
          (it.chatInfo as? ChatInfo.Group)
            ?.groupInfo
        }.filter {
          it.useRelays &&
            it.isChannel
        }
    val coreResult =
      runBlocking {
        ChatModel.controller.apiGetChatsResult(
          user.remoteHostId,
        )
      }
    val coreGroups =
      if (
        coreResult is
          ChatListLoadResult.Success
      ) {
        coreResult.chats
          .mapNotNull {
            (it.chatInfo as? ChatInfo.Group)
              ?.groupInfo
          }.filter {
            it.useRelays &&
              it.isChannel
          }
      } else {
        emptyList()
      }
    val modelSummary =
      publicChannelStateSummary(
        modelGroups,
      )
    val coreSummary =
      publicChannelStateSummary(
        coreGroups,
      )
    val modelIds =
      modelGroups.map {
        it.groupId
      }.toSet()
    val coreIds =
      coreGroups.map {
        it.groupId
      }.toSet()
    InstrumentationRegistry
      .getInstrumentation()
      .addResults(
        Bundle().apply {
          putString(
            "safePreparedPublicChannelStateCounts",
            modelSummary,
          )
          putString(
            "safeCorePublicChannelStateCounts",
            coreSummary,
          )
          putBoolean(
            "safeCoreChatQuerySucceeded",
            coreResult is
              ChatListLoadResult.Success,
          )
          putInt(
            "safePreparedPublicChannelCount",
            modelGroups.count {
              it.preparedGroup != null
            },
          )
          putInt(
            "safeCorePreparedPublicChannelCount",
            coreGroups.count {
              it.preparedGroup != null
            },
          )
          putInt(
            "safeModelOnlyPublicChannelCount",
            (modelIds - coreIds).size,
          )
          putInt(
            "safeCoreOnlyPublicChannelCount",
            (coreIds - modelIds).size,
          )
        },
      )
    Log.i(
      TAG,
      "preparedPublicChannelStateCounts=$modelSummary " +
        "corePublicChannelStateCounts=$coreSummary " +
        "coreChatQuerySucceeded=" +
        (
          coreResult is
            ChatListLoadResult.Success
        ) +
        " modelPreparedPublicChannelCount=" +
        modelGroups.count {
          it.preparedGroup != null
        } +
        " corePreparedPublicChannelCount=" +
        coreGroups.count {
          it.preparedGroup != null
        } +
        " modelOnlyPublicChannelCount=" +
        (modelIds - coreIds).size +
        " coreOnlyPublicChannelCount=" +
        (coreIds - modelIds).size,
    )
  }

  private fun publicChannelStateSummary(
    groups: List<chat.simplex.common.model.GroupInfo>,
  ): String {
    val stateCounts =
      groups
        .groupingBy {
          val prepared =
            it.preparedGroup
          "${it.membership.memberRole.name}/" +
            "${it.membership.memberStatus.name}/" +
            "ready=${it.ready}/" +
            "prepared=" +
            (prepared != null) +
            "/preparedConnection=" +
            (
              prepared
                ?.connLinkPreparedConnection
                ?: false
            ) +
            "/startedConnection=" +
            (
              prepared
                ?.connLinkStartedConnection
                ?: false
            )
        }.eachCount()
        .toSortedMap()
    return stateCounts.entries.joinToString("|") {
        "${it.key}=${it.value}"
      }.ifEmpty {
        "none"
      }
  }

  private fun openChannel(
    scenario: ActivityScenario<MainActivity>,
  ) {
    val channel =
      controlledChannel()
    assertTrue(
      "A controlled public channel must exist before opening its production route",
      channel != null,
    )
    val groupInfo =
      requireNotNull(channel)
        .groupInfo
    runBlocking {
      openGroupChat(
        rhId = null,
        groupId = groupInfo.groupId,
      )
    }
    waitUntil(OPEN_TIMEOUT_MILLIS) {
      ChatModel.chatId.value == groupInfo.id
    }
    scenario.onActivity {
      it.window.clearFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
      )
    }
    Log.i(
      TAG,
      "publicChannelOpened=true",
    )
    SystemClock.sleep(OPEN_HOLD_MILLIS)
    scenario.onActivity {
      it.window.addFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
      )
    }
  }

  private fun controlledChannel(): ChatInfo.Group? {
    val record =
      loadFixtureRecord()
        ?: return null
    val user =
      ChatModel.currentUser.value
        ?: return null
    assertTrue(
      "The controlled fixture record belongs to a different active user or remote host",
      record.userId == user.userId &&
        record.remoteHostId ==
        user.remoteHostId,
    )
    return ChatModel.chats.value
      .mapNotNull {
        it.chatInfo as? ChatInfo.Group
      }.singleOrNull {
        it.groupInfo.useRelays &&
          it.groupInfo.groupId ==
          record.groupId
      }
  }

  private fun controlledChannelTexts(
    channel: ChatInfo.Group,
  ): Set<String> =
    controlledChannelItems(channel)
      .filter {
        it.meta.itemDeleted == null
      }.map {
        it.text
      }.toSet()

  private fun controlledChannelItems(
    channel: ChatInfo.Group,
  ): List<ChatItem> =
    runBlocking {
      ChatModel.controller.apiGetChat(
        rh = null,
        type = ChatType.Group,
        id = channel.apiId,
        scope = channel.groupChatScope(),
        pagination =
          ChatPagination.Last(
            CHAT_ITEM_LIMIT,
          ),
      )
    }?.first
      ?.chatItems
      .orEmpty()

  private fun publicChannelIds(): Set<String> =
    ChatModel.chats.value
      .mapNotNull {
        (it.chatInfo as? ChatInfo.Group)
          ?.groupInfo
          ?.takeIf { group ->
            group.useRelays
          }?.id
      }.toSet()

  private fun fixtureRecordFile(): File =
    File(
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
        .cacheDir,
      CONTROLLED_FIXTURE_RECORD_NAME,
    )

  private fun persistFixtureRecord(
    groupId: Long,
    fixtureNonce: String,
  ) {
    val user =
      requireNotNull(
        ChatModel.currentUser.value,
      )
    val record =
      listOf(
        CONTROLLED_FIXTURE_RECORD_VERSION,
        user.userId.toString(),
        user.remoteHostId?.toString()
          ?: LOCAL_REMOTE_HOST,
        groupId.toString(),
        fixtureNonce,
      ).joinToString("\n")
    fixtureRecordFile().writeText(
      record,
      Charsets.UTF_8,
    )
  }

  private fun loadFixtureRecord(): ControlledFixtureRecord? {
    val expectedNonce =
      requireFixtureNonce()
    return runCatching {
      val fields =
        fixtureRecordFile().readLines(
          Charsets.UTF_8,
        )
      if (
        fields.size != 5 ||
        fields[0] !=
        CONTROLLED_FIXTURE_RECORD_VERSION ||
        fields[4] != expectedNonce
      ) {
        null
      } else {
        ControlledFixtureRecord(
          userId =
            fields[1].toLong(),
          remoteHostId =
            fields[2].let {
              if (it == LOCAL_REMOTE_HOST) {
                null
              } else {
                it.toLong()
              }
            },
          groupId =
            fields[3].toLong(),
          fixtureNonce =
            fields[4],
        )
      }
    }.getOrNull()
  }

  private fun requireFixtureNonce(): String {
    val rawNonce =
      requireNotNull(
        InstrumentationRegistry
          .getArguments()
          .getString(
            PUBLIC_CHANNEL_FIXTURE_NONCE_ARGUMENT,
          ),
      ) {
        "Controlled public-channel actions require an explicit per-run fixture nonce"
      }
    val parsedNonce =
      runCatching {
        UUID.fromString(rawNonce)
      }.getOrNull()
    assertTrue(
      "The controlled public-channel fixture nonce must be a canonical UUID",
      parsedNonce != null &&
        parsedNonce.toString() ==
        rawNonce.lowercase(),
    )
    return requireNotNull(parsedNonce)
      .toString()
  }

  private data class ControlledFixtureRecord(
    val userId: Long,
    val remoteHostId: Long?,
    val groupId: Long,
    val fixtureNonce: String,
  )

  private fun safeFailure(
    error: Exception,
  ): String {
    val message = error.message.orEmpty().uppercase()
    return when {
      "BROKER" in message &&
        "NETWORK" in message -> "BROKER/NETWORK"
      "TIMEOUT" in message ||
        "CONNECTION TIMEOUT" in message -> "TIMEOUT"
      "SMP" in message -> "SMP"
      else ->
        error::class.simpleName
          ?: "exception"
    }
  }

  private fun waitUntil(
    timeoutMillis: Long,
    predicate: () -> Boolean,
  ) {
    val deadline =
      SystemClock.elapsedRealtime() +
        timeoutMillis
    while (
      !predicate() &&
      SystemClock.elapsedRealtime() < deadline
    ) {
      SystemClock.sleep(POLL_MILLIS)
    }
    assertTrue(
      "Timed out waiting for the controlled public-channel lifecycle",
      predicate(),
    )
  }

  private enum class ProducerAction(
    val argument: String,
    val usesControlledFixtureRecord: Boolean = true,
  ) {
    Create("create"),
    Rename("rename"),
    Populate("populate"),
    Attach("attach"),
    Open("open"),
    Delete("delete"),
    Status("status"),
    Members("members"),
    Connections(
      "connections",
      usesControlledFixtureRecord = false,
    ),
    Prepared(
      "prepared",
      usesControlledFixtureRecord = false,
    ),
    CaptureLifecycle("capture-lifecycle"),
  }

  private fun controlledProducerSkipRequested(
    rawValue: String?,
  ): Boolean =
    when (rawValue) {
      null -> false
      "true" -> true
      else ->
        error(
          "Invalid $CONTROLLED_PRODUCER_SKIP_ARGUMENT: $rawValue",
        )
    }

  private fun optionalRelayOffsetArgument(): Int? {
    val rawValue =
      InstrumentationRegistry
        .getArguments()
        .getString(
          PUBLIC_CHANNEL_RELAY_OFFSET_ARGUMENT,
        )
    return when {
      rawValue == null -> null
      else ->
        rawValue.toIntOrNull()
          ?: error(
            "Invalid $PUBLIC_CHANNEL_RELAY_OFFSET_ARGUMENT: $rawValue",
          )
    }
  }

  private fun optionalRelayCountArgument(): Int? {
    val rawValue =
      InstrumentationRegistry
        .getArguments()
        .getString(
          PUBLIC_CHANNEL_RELAY_COUNT_ARGUMENT,
        )
    return when {
      rawValue == null -> null
      else ->
        rawValue.toIntOrNull()
          ?.takeIf { it in 1..MAX_RELAYS }
          ?: error(
            "Invalid $PUBLIC_CHANNEL_RELAY_COUNT_ARGUMENT: $rawValue",
          )
    }
  }

  private companion object {
    const val TAG = "NomePublicChannelProducer"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val PUBLIC_CHANNEL_ACTION_ARGUMENT =
      "nomePublicChannelAction"
    const val PUBLIC_CHANNEL_RELAY_OFFSET_ARGUMENT =
      "nomePublicChannelRelayOffset"
    const val PUBLIC_CHANNEL_RELAY_COUNT_ARGUMENT =
      "nomePublicChannelRelayCount"
    const val PUBLIC_CHANNEL_FIXTURE_NONCE_ARGUMENT =
      "nomePublicChannelFixtureNonce"
    const val CONTROLLED_CHANNEL_NAME =
      "Nome 产品更新"
    const val CONTROLLED_FIXTURE_RECORD_NAME =
      "nome-public-channel-fixture-v1"
    const val CONTROLLED_FIXTURE_RECORD_VERSION =
      "nome-public-channel-fixture/v1"
    const val LOCAL_REMOTE_HOST =
      "local"
    const val MAX_RELAYS = 3
    const val READY_TIMEOUT_MILLIS = 120_000L
    const val CREATION_TIMEOUT_MILLIS = 300_000L
    const val OPEN_TIMEOUT_MILLIS = 120_000L
    const val OPEN_HOLD_MILLIS = 120_000L
    const val DELETION_TIMEOUT_MILLIS = 120_000L
    const val POLL_MILLIS = 500L
    const val CHAT_ITEM_LIMIT = 100
    const val CONTROLLED_ATTACHMENT_NAME =
      "Nome-channel-visual-baseline.txt"
    const val CONTROLLED_ATTACHMENT_POST =
      "真实附件展示\n" +
        "此真实本地附件仅验证频道文件行，不代表远端已接收。"
    const val CONTROLLED_ATTACHMENT_CONTENT =
      "Nome Android P21 production fixture.\n" +
        "This real local file only verifies the channel attachment row. " +
        "It makes no delivery, availability, or success claim.\n"
    const val SUPERSEDED_CONTROLLED_POST =
      "Nome 频道展示检查\n" +
        "这是受控测试内容，用于验证公开频道的生产页面结构。"

    val CONTROLLED_POSTS =
      listOf(
        "公开频道说明\n" +
          "公开频道会持续显示非端到端加密提示，帮助理解内容可见范围。",
      )
  }
}
