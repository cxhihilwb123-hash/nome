package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.APIConnectPlanResult
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatListLoadResult
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.ChatPagination
import chat.simplex.common.model.ChatType
import chat.simplex.common.model.ComposedMessage
import chat.simplex.common.model.ConnectionPlan
import chat.simplex.common.model.GroupLinkPlan
import chat.simplex.common.model.GroupMemberRole
import chat.simplex.common.model.MsgContent
import chat.simplex.common.views.chatlist.openGroupChat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTwoClientPublicChannelTest {
  @Test
  fun realObserverJoinsAndReceivesControlledChannelPosts() {
    val arguments = InstrumentationRegistry.getArguments()
    val role =
      controlledProducerRoleOrSkip(arguments)
        ?: return
    val port =
      bridgePortArgument(arguments)
    val scenario =
      ActivityScenario.launch<MainActivity>(
        Intent().setClassName(
          InstrumentationRegistry.getInstrumentation().targetContext.packageName,
          MainActivity::class.java.name,
        ),
      )
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true
      }
      when (role) {
        ProducerRole.Source ->
          runSource(
            port = port,
            fixtureNonce =
              requireFixtureNonce(),
          )

        ProducerRole.Target ->
          runTarget(
            scenario = scenario,
            port = port,
          )
      }
    } finally {
      scenario.onActivity {
        it.window.addFlags(
          WindowManager.LayoutParams.FLAG_SECURE,
        )
      }
      scenario.close()
    }
  }

  private fun runSource(
    port: Int,
    fixtureNonce: String,
  ) {
    val arguments =
      InstrumentationRegistry
        .getArguments()
    val channel =
      requireNotNull(
        controlledChannel(
          fixtureNonce,
        ),
      )
    val info =
      channel.chatInfo as ChatInfo.Group
    val recreateGroupLink =
      strictBooleanArgument(
        arguments = arguments,
        argumentName = RECREATE_GROUP_LINK_ARGUMENT,
      )
    val observerLink =
      if (recreateGroupLink) {
        assertTrue(
          "Group-link recreation is restricted to an explicitly disposable channel owner",
          arguments.getString(
            DISPOSABLE_CHANNEL_OWNER_ARGUMENT,
          ) == "true",
        )
        assertTrue(
          "The disposable channel's previous link must be deleted by the official API",
          runBlocking {
            ChatModel.controller.apiDeleteGroupLink(
              rh = channel.remoteHostId,
              groupId = info.groupInfo.groupId,
            )
          },
        )
        runBlocking {
          ChatModel.controller.apiCreateGroupLink(
            rh = channel.remoteHostId,
            groupId = info.groupInfo.groupId,
            memberRole = GroupMemberRole.Observer,
          )
        }
      } else {
        runBlocking {
          ChatModel.controller.apiGroupLinkMemberRole(
            rh = channel.remoteHostId,
            groupId = info.groupInfo.groupId,
            memberRole = GroupMemberRole.Observer,
          )
        }
      }
    assertNotNull(
      "The official channel link must remain available for an observer",
      observerLink,
    )
    assertEquals(
      "The official channel link must assign the observer role",
      GroupMemberRole.Observer,
      requireNotNull(observerLink).acceptMemberRole,
    )
    sendBearerToMemoryBridge(
      port = port,
      bearer =
        requireNotNull(
          observerLink.connLinkContact.connShortLink,
        ),
    )

    waitUntil(CHANNEL_TIMEOUT_MILLIS) {
      runBlocking {
        ChatModel.controller.apiListMembers(
          rh = channel.remoteHostId,
          groupId = info.groupInfo.groupId,
        )
      }.any {
        it.memberRole == GroupMemberRole.Observer &&
          it.memberActive
      }
    }

    val sent =
      runBlocking {
        ChatModel.controller.apiSendMessages(
          rh = channel.remoteHostId,
          type = info.chatType,
          id = info.apiId,
          scope = info.groupChatScope(),
          sendAsGroup = info.sendAsGroup,
          composedMessages =
            CONTROLLED_POSTS.map {
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
      "The official channel owner must create both controlled posts",
      sent?.size == CONTROLLED_POSTS.size,
    )
    waitUntil(CHANNEL_TIMEOUT_MILLIS) {
      channelContainsControlledPosts(channel)
    }
    Log.i(
      TAG,
      "observerRoleObserved=true " +
        "controlledChannelPostsCreated=${CONTROLLED_POSTS.size}",
    )
  }

  private fun runTarget(
    scenario: ActivityScenario<MainActivity>,
    port: Int,
  ) {
    removeDisposablePendingGroupLinkConnections()
    val user =
      requireNotNull(
        ChatModel.currentUser.value,
      )
    val bearer =
      receiveBearerFromMemoryBridge(
        port = port,
      )
    val plan =
      runBlocking {
        ChatModel.controller.apiConnectPlanResult(
          rh = user.remoteHostId,
          userId = user.userId,
          connLink = bearer,
        )
      }
    assertTrue(
      "The official planner must accept the controlled public-channel link",
      plan is APIConnectPlanResult.Ready,
    )
    val readyPlan =
      plan as APIConnectPlanResult.Ready
    val groupLinkPlan =
      (
        readyPlan.connectionPlan as?
          ConnectionPlan.GroupLink
      )?.groupLinkPlan
    assertTrue(
      "The clean disposable observer must receive a new group-link plan",
      groupLinkPlan is GroupLinkPlan.Ok,
    )
    val newGroupPlan =
      groupLinkPlan as GroupLinkPlan.Ok
    val plannedRelayAddresses =
      newGroupPlan.groupSLinkInfo_
        ?.groupRelays
        .orEmpty()
    assertTrue(
      "The official group-link plan must provide at least one public-channel relay",
      plannedRelayAddresses.isNotEmpty(),
    )
    val relayTestResults =
      plannedRelayAddresses.map { address ->
        runBlocking {
          ChatModel.controller.testChatRelay(
            rh = user.remoteHostId,
            address = address,
          )
        }
      }
    val relayTestStateCounts =
      relayTestResults
        .groupingBy {
          it.second
            ?.rtfStep
            ?.name
            ?: "PASS"
        }.eachCount()
        .toSortedMap()
    val relayTestSummary =
      relayTestStateCounts.entries
        .joinToString("|") {
          "${it.key}=${it.value}"
        }
    val relayTestFailures =
      relayTestResults.count { result ->
        result.first == null ||
          result.second != null
      }
    Log.i(
      TAG,
      "plannedRelayCount=${plannedRelayAddresses.size} " +
        "plannedRelayTestStateCounts=$relayTestSummary " +
        "plannedRelayTestFailures=$relayTestFailures",
    )
    assertEquals(
      "Every relay in the official group-link plan must pass its official test before joining",
      0,
      relayTestFailures,
    )
    val groupShortLinkData =
      requireNotNull(
        newGroupPlan.groupSLinkData_,
      )
    val prepared =
      requireNotNull(
        runBlocking {
          ChatModel.controller.apiPrepareGroup(
            rh = user.remoteHostId,
            connLink = readyPlan.connectionLink,
            directLink =
              newGroupPlan.groupSLinkInfo_
                ?.direct
                ?: true,
            groupShortLinkData = groupShortLinkData,
          )
        },
      )
    val preparedInfo =
      prepared.chatInfo as? ChatInfo.Group
    assertTrue(
      "The official prepared chat must be a public channel",
      preparedInfo?.groupInfo?.isChannel == true,
    )
    assertActiveUserUnchanged(user)
    withContextOnMain {
      newGroupPlan.groupSLinkInfo_
        ?.groupRelays
        ?.takeIf {
          it.isNotEmpty()
        }?.let {
          ChatModel.channelRelayHostnames[
            requireNotNull(preparedInfo)
              .groupInfo
              .groupId
          ] = it
        }
      ChatModel.chatsContext.addChat(
        prepared,
      )
    }
    val started =
      requireNotNull(
        runBlocking {
          ChatModel.controller.apiConnectPreparedGroup(
            rh = user.remoteHostId,
            groupId =
              requireNotNull(preparedInfo)
                .groupInfo
                .apiId,
            incognito = false,
            msg = null,
          )
        },
      )
    assertActiveUserUnchanged(user)
    withContextOnMain {
      ChatModel.chatsContext.updateGroup(
        user.remoteHostId,
        started.first,
      )
      ChatModel.channelRelayHostnames.remove(
        started.first.groupId,
      )
    }
    Log.i(
      TAG,
      "observerPreparedChannel=true " +
        "preparedRelayCount=" +
        (
          newGroupPlan.groupSLinkInfo_
            ?.groupRelays
            ?.size
            ?: 0
        ) +
        " startedRelayCount=" +
        started.second.size,
    )

    waitUntil(CHANNEL_TIMEOUT_MILLIS) {
      val coreChannels =
        corePublicChannels(
          user.remoteHostId,
        )
      coreChannels.any {
        val info =
          it.chatInfo as ChatInfo.Group
        info.ready &&
          info.groupInfo.membership.memberRole ==
          GroupMemberRole.Observer
      } ||
        readyPublicChannels().any {
        (it.chatInfo as ChatInfo.Group)
            .groupInfo
            .membership
            .memberRole == GroupMemberRole.Observer
      }
    }
    waitUntil(CHANNEL_TIMEOUT_MILLIS) {
      readyObserverChannels().any {
        channelContainsControlledPosts(it)
      }
    }
    val channel =
      requireNotNull(
        readyObserverChannels()
          .filter {
            channelContainsControlledPosts(it)
          }.maxByOrNull {
            it.chatInfo.createdAt
          },
      )
    val info =
      channel.chatInfo as ChatInfo.Group
    assertEquals(
      "The real joined channel must use the observer role",
      GroupMemberRole.Observer,
      info.groupInfo.membership.memberRole,
    )
    waitUntil(CHANNEL_TIMEOUT_MILLIS) {
      channelContainsControlledPosts(channel)
    }
    runBlocking {
      openGroupChat(
        rhId = channel.remoteHostId,
        groupId = info.groupInfo.groupId,
      )
    }
    waitUntil(UI_TIMEOUT_MILLIS) {
      ChatModel.chatId.value == info.id
    }
    scenario.onActivity {
      it.window.clearFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
      )
    }
    Log.i(
      TAG,
      "observerRole=true " +
        "peerObservedChannelPosts=${CONTROLLED_POSTS.size} " +
        "publicChannelOpened=true",
    )
    SystemClock.sleep(CAPTURE_HOLD_MILLIS)
  }

  private fun removeDisposablePendingGroupLinkConnections() {
    val arguments =
      InstrumentationRegistry
        .getArguments()
    assertTrue(
      "Deleting stale controlled group-link attempts is restricted to the disposable observer",
      arguments.getString(
        DISPOSABLE_OBSERVER_ARGUMENT,
      ) == "true",
    )
    val pending =
      ChatModel.chats.value
        .mapNotNull { chat ->
          (chat.chatInfo as? ChatInfo.ContactConnection)
            ?.takeIf {
              it.contactConnection.groupLinkId != null
            }?.let {
              chat.remoteHostId to it
            }
        }
    pending.forEach { (rhId, info) ->
      assertTrue(
        "The official API must delete every stale controlled group-link attempt",
        runBlocking {
          ChatModel.controller.apiDeleteChat(
            rh = rhId,
            type = ChatType.ContactConnection,
            id = info.apiId,
          )
        },
      )
      withContextOnMain {
        ChatModel.chatsContext.removeChat(
          rhId,
          info.id,
        )
      }
    }
    val preparedGroups =
      ChatModel.chats.value
        .mapNotNull { chat ->
          (chat.chatInfo as? ChatInfo.Group)
            ?.takeIf {
              it.groupInfo.isChannel &&
                it.groupInfo.useRelays &&
                it.groupInfo.preparedGroup != null &&
                it.groupInfo.membership.memberRole ==
                GroupMemberRole.Observer
            }?.let {
              chat.remoteHostId to it
            }
        }
    preparedGroups.forEach { (rhId, info) ->
      assertTrue(
        "The official API must delete every stale controlled prepared Observer channel",
        runBlocking {
          ChatModel.controller.apiDeleteChat(
            rh = rhId,
            type = ChatType.Group,
            id = info.apiId,
          )
        },
      )
      withContextOnMain {
        ChatModel.chatsContext.removeChat(
          rhId,
          info.id,
        )
      }
    }
    waitUntil(READY_TIMEOUT_MILLIS) {
      ChatModel.chats.value.none { chat ->
        val info =
          chat.chatInfo
        (
          info as?
            ChatInfo.ContactConnection
        )?.contactConnection?.groupLinkId != null ||
          (
            info is ChatInfo.Group &&
              info.groupInfo.isChannel &&
              info.groupInfo.useRelays &&
              info.groupInfo.preparedGroup != null &&
              info.groupInfo.membership.memberRole ==
              GroupMemberRole.Observer
          )
      }
    }
    Log.i(
      TAG,
      "staleControlledGroupLinkAttemptsDeleted=" +
        pending.size +
        " stalePreparedObserverChannelsDeleted=" +
        preparedGroups.size,
    )
  }

  private fun controlledChannel(
    fixtureNonce: String,
  ): Chat? {
    val record =
      loadFixtureRecord(
        fixtureNonce,
      )
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
      .singleOrNull { chat ->
        val info =
          chat.chatInfo as? ChatInfo.Group
        info?.groupInfo?.isChannel == true &&
          info.groupInfo.useRelays &&
          info.groupInfo.groupId ==
          record.groupId
      }
  }

  private fun fixtureRecordFile(): File =
    File(
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
        .cacheDir,
      CONTROLLED_FIXTURE_RECORD_NAME,
    )

  private fun loadFixtureRecord(
    expectedNonce: String,
  ): ControlledFixtureRecord? =
    runCatching {
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

  private fun requireFixtureNonce(): String {
    val rawNonce =
      requireNotNull(
        InstrumentationRegistry
          .getArguments()
          .getString(
            PUBLIC_CHANNEL_FIXTURE_NONCE_ARGUMENT,
          ),
      ) {
        "The public-channel source requires an explicit per-run fixture nonce"
      }
    val parsedNonce =
      runCatching {
        UUID.fromString(rawNonce)
      }.getOrNull()
    assertTrue(
      "The public-channel source fixture nonce must be a canonical UUID",
      parsedNonce != null &&
        parsedNonce.toString() ==
        rawNonce.lowercase(),
    )
    return requireNotNull(parsedNonce)
      .toString()
  }

  private fun controlledProducerRoleOrSkip(
    arguments: android.os.Bundle,
  ): ProducerRole? {
    val roleArgument =
      arguments.getString(
        PRODUCER_ROLE_ARGUMENT,
      )
    if (roleArgument == null) {
      if (
        arguments.getString(
          CONTROLLED_PRODUCER_SKIP_ARGUMENT,
        ) == "true"
      ) {
        return null
      }
      error(
        "Missing $PRODUCER_ROLE_ARGUMENT; " +
          "set $CONTROLLED_PRODUCER_SKIP_ARGUMENT=true to bypass this controlled harness",
      )
    }
    return ProducerRole.entries.firstOrNull {
      it.argument == roleArgument
    } ?: error(
      "Invalid $PRODUCER_ROLE_ARGUMENT=$roleArgument",
    )
  }

  private fun bridgePortArgument(
    arguments: android.os.Bundle,
  ): Int {
    val rawPort =
      arguments.getString(
        PRODUCER_PORT_ARGUMENT,
      ) ?: return DEFAULT_BRIDGE_PORT
    return rawPort.toIntOrNull()
      ?: error(
        "Invalid $PRODUCER_PORT_ARGUMENT=$rawPort",
      )
  }

  private fun strictBooleanArgument(
    arguments: android.os.Bundle,
    argumentName: String,
  ): Boolean {
    val rawValue =
      arguments.getString(argumentName)
        ?: return false
    return rawValue.toBooleanStrictOrNull()
      ?: error(
        "Invalid $argumentName=$rawValue",
      )
  }

  private data class ControlledFixtureRecord(
    val userId: Long,
    val remoteHostId: Long?,
    val groupId: Long,
    val fixtureNonce: String,
  )

  private fun readyPublicChannels(): List<Chat> =
    ChatModel.chats.value.filter {
      val info =
        it.chatInfo as? ChatInfo.Group
      info?.groupInfo?.isChannel == true &&
        info.ready
    }

  private fun readyObserverChannels(): List<Chat> =
    readyPublicChannels().filter {
      (it.chatInfo as ChatInfo.Group)
        .groupInfo
        .membership
        .memberRole == GroupMemberRole.Observer
    }

  private fun corePublicChannels(
    remoteHostId: Long?,
  ): List<Chat> =
    when (
      val result =
        runBlocking {
          ChatModel.controller.apiGetChatsResult(
            remoteHostId,
          )
        }
    ) {
      is ChatListLoadResult.Success ->
        result.chats.filter {
          val info =
            it.chatInfo as? ChatInfo.Group
          info?.groupInfo?.isChannel == true &&
            info.groupInfo.useRelays
        }

      is ChatListLoadResult.Failure,
      is ChatListLoadResult.NoCurrentUser,
      -> emptyList()
    }

  private fun assertActiveUserUnchanged(
    expected: chat.simplex.common.model.User,
  ) {
    val current =
      requireNotNull(
        ChatModel.currentUser.value,
      )
    assertTrue(
      "The controlled public-channel attempt must stay on the same active user and remote host",
      current.userId == expected.userId &&
        current.remoteHostId == expected.remoteHostId,
    )
  }

  private fun channelContainsControlledPosts(
    channel: Chat,
  ): Boolean {
    val info =
      channel.chatInfo as ChatInfo.Group
    val texts =
      runBlocking {
        ChatModel.controller.apiGetChat(
          rh = channel.remoteHostId,
          type = info.chatType,
          id = info.apiId,
          scope = info.groupChatScope(),
          pagination =
            ChatPagination.Last(
              CHAT_ITEM_LIMIT,
            ),
        )
      }?.first
        ?.chatItems
        ?.map {
          it.text
        }.orEmpty()
    return CONTROLLED_POSTS.all {
      it in texts
    }
  }

  private fun sendBearerToMemoryBridge(
    port: Int,
    bearer: String,
  ) {
    bridgeSocket(port).use { socket ->
      val output =
        DataOutputStream(
          socket.getOutputStream(),
        )
      output.writeByte(
        ProducerRole.Source.wireValue,
      )
      val bytes =
        bearer.toByteArray(
          Charsets.UTF_8,
        )
      try {
        assertTrue(
          "The official link must stay within the bounded bridge payload",
          bytes.size in 1..MAX_BEARER_BYTES,
        )
        output.writeInt(bytes.size)
        output.write(bytes)
        output.flush()
        assertEquals(
          "The observer must acknowledge the in-memory handoff",
          BRIDGE_ACK,
          socket.getInputStream().read(),
        )
      } finally {
        bytes.fill(0)
      }
    }
  }

  private fun receiveBearerFromMemoryBridge(
    port: Int,
  ): String =
    bridgeSocket(port).use { socket ->
      val output =
        DataOutputStream(
          socket.getOutputStream(),
        )
      output.writeByte(
        ProducerRole.Target.wireValue,
      )
      output.flush()
      val input =
        DataInputStream(
          socket.getInputStream(),
        )
      val length =
        input.readInt()
      assertTrue(
        "The bridge payload length must stay within the official link bound",
        length in 1..MAX_BEARER_BYTES,
      )
      val bytes =
        ByteArray(length)
      try {
        input.readFully(bytes)
        String(
          bytes,
          Charsets.UTF_8,
        )
      } finally {
        bytes.fill(0)
      }
    }

  private fun bridgeSocket(
    port: Int,
  ): Socket =
    Socket().apply {
      soTimeout =
        CHANNEL_TIMEOUT_MILLIS.toInt()
      connect(
        InetSocketAddress(
          BRIDGE_HOST,
          port,
        ),
        BRIDGE_CONNECT_TIMEOUT_MILLIS,
      )
    }

  private fun withContextOnMain(
    block: suspend () -> Unit,
  ) {
    runBlocking {
      withContext(Dispatchers.Main) {
        block()
      }
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
      "Timed out waiting for the controlled public-channel state",
      predicate(),
    )
  }

  private enum class ProducerRole(
    val argument: String,
    val wireValue: Int,
  ) {
    Source("source", 1),
    Target("target", 2),
  }

  private companion object {
    const val TAG =
      "NomeTwoClientPublicChannel"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val PRODUCER_ROLE_ARGUMENT =
      "nomeProducerRole"
    const val PRODUCER_PORT_ARGUMENT =
      "nomeProducerPort"
    const val PUBLIC_CHANNEL_FIXTURE_NONCE_ARGUMENT =
      "nomePublicChannelFixtureNonce"
    const val RECREATE_GROUP_LINK_ARGUMENT =
      "nomeRecreateGroupLink"
    const val DISPOSABLE_CHANNEL_OWNER_ARGUMENT =
      "nomeDisposableChannelOwner"
    const val DISPOSABLE_OBSERVER_ARGUMENT =
      "nomeDisposableObserver"
    const val CONTROLLED_FIXTURE_RECORD_NAME =
      "nome-public-channel-fixture-v1"
    const val CONTROLLED_FIXTURE_RECORD_VERSION =
      "nome-public-channel-fixture/v1"
    const val LOCAL_REMOTE_HOST =
      "local"
    const val BRIDGE_HOST =
      "10.0.2.2"
    const val DEFAULT_BRIDGE_PORT =
      27197
    const val BRIDGE_ACK =
      1
    const val MAX_BEARER_BYTES =
      16_384
    const val BRIDGE_CONNECT_TIMEOUT_MILLIS =
      15_000
    const val READY_TIMEOUT_MILLIS =
      120_000L
    const val CHANNEL_TIMEOUT_MILLIS =
      300_000L
    const val UI_TIMEOUT_MILLIS =
      120_000L
    const val CAPTURE_HOLD_MILLIS =
      2_000L
    const val CHAT_ITEM_LIMIT =
      100
    const val POLL_MILLIS =
      500L

    val CONTROLLED_POSTS =
      listOf(
        "Nome 频道展示检查\n" +
          "这是受控测试内容，用于验证公开频道的生产页面结构。",
        "公开频道说明\n" +
          "公开频道会持续显示非端到端加密提示，帮助理解内容可见范围。",
      )
  }
}
