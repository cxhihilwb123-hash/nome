package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatModel
import chat.simplex.common.views.call.CallMediaType
import chat.simplex.common.views.call.CallState
import chat.simplex.common.views.call.RcvCallInvitation
import chat.simplex.common.views.chat.startChatCall
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTwoClientConnectedCallTest {
  @Test
  fun realControlledAudioCallConnectsOnBothPeersAndEndsCleanly() {
    val arguments =
      InstrumentationRegistry.getArguments()
    val role =
      controlledProducerRoleOrSkip(
        arguments,
      ) ?: return
    val port =
      bridgePortArgument(arguments)
    val instrumentation =
      InstrumentationRegistry.getInstrumentation()
    val scenario =
      ActivityScenario.launch<MainActivity>(
        Intent().setClassName(
          instrumentation.targetContext.packageName,
          MainActivity::class.java.name,
        ),
      )
    var receivedInvitation: RcvCallInvitation? = null
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true &&
          newestReadyDirectChat() != null &&
          ChatModel.activeCall.value == null
      }
      val direct =
        requireNotNull(
          newestReadyDirectChat(),
        )
      val info =
        direct.chatInfo as ChatInfo.Direct
      when (role) {
        ProducerRole.Source -> {
          startChatCall(
            remoteHostId = direct.remoteHostId,
            chatInfo = info,
            media = CallMediaType.Audio,
          )
          waitUntil(CALL_TIMEOUT_MILLIS) {
            ChatModel.activeCall.value
              ?.takeIf {
                it.contact.id ==
                  info.contact.id
              }?.callState
              ?.let {
                it >= CallState.InvitationSent
              } == true
          }
        }

        ProducerRole.Target -> {
          val existingInvitationIds =
            runBlocking {
              ChatModel.controller
                .apiGetCallInvitations(
                  direct.remoteHostId,
                )
            }.map {
              it.callUUID
            }.toSet()
          waitUntil(CALL_TIMEOUT_MILLIS) {
            receivedInvitation =
              runBlocking {
                ChatModel.controller
                  .apiGetCallInvitations(
                    direct.remoteHostId,
                  )
              }.firstOrNull {
                it.contact.id ==
                  info.contact.id &&
                  it.callUUID !in
                  existingInvitationIds
              }
            receivedInvitation != null
          }
          ChatModel.callManager
            .acceptIncomingCall(
              requireNotNull(
                receivedInvitation,
              ),
            )
        }
      }
      waitUntil(CALL_TIMEOUT_MILLIS) {
        ChatModel.activeCall.value
          ?.takeIf {
            it.contact.id ==
              info.contact.id &&
              it.callState ==
              CallState.Connected
          }?.let {
            it.connectedAt != null ||
              it.connectionInfo != null
          } == true
      }
      coordinateWithPeer(
        port = port,
        role = role,
      )
      when (role) {
        ProducerRole.Source -> {
          runBlocking {
            ChatModel.callManager
              .endCall(
                requireNotNull(
                  ChatModel.activeCall.value,
                ),
              )
          }
        }

        ProducerRole.Target -> {
          waitUntil(END_TIMEOUT_MILLIS) {
            ChatModel.activeCall.value == null
          }
        }
      }
      waitUntil(END_TIMEOUT_MILLIS) {
        ChatModel.activeCall.value == null &&
          ChatModel.showCallView.value == false
      }
      Log.i(
        TAG,
        "role=${role.argument} connectedCall=true cleanEnd=true",
      )
    } finally {
      ChatModel.activeCall.value?.let { call ->
        runCatching {
          runBlocking {
            ChatModel.callManager
              .endCall(call)
          }
        }
      }
      receivedInvitation?.let { invitation ->
        ChatModel.callInvitations
          .remove(
            invitation.contact.id,
          )
      }
      scenario.close()
    }
  }

  private fun newestReadyDirectChat(): Chat? =
    ChatModel.chats.value
      .filter {
        (it.chatInfo as? ChatInfo.Direct)
          ?.ready == true
      }.maxByOrNull {
        it.chatInfo.createdAt
      }

  private fun coordinateWithPeer(
    port: Int,
    role: ProducerRole,
  ) {
    bridgeSocket(port).use { socket ->
      val output =
        DataOutputStream(
          socket.getOutputStream(),
        )
      output.writeByte(role.wireValue)
      output.flush()
      assertTrue(
        "Both controlled clients must connect the same call before teardown",
        socket.getInputStream().read() ==
          BRIDGE_ACK,
      )
    }
  }

  private fun bridgeSocket(
    port: Int,
  ): Socket =
    Socket().apply {
      soTimeout =
        CALL_TIMEOUT_MILLIS.toInt()
      connect(
        InetSocketAddress(
          BRIDGE_HOST,
          port,
        ),
        BRIDGE_CONNECT_TIMEOUT_MILLIS,
      )
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
      "Timed out waiting for the controlled two-client call state",
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

  private companion object {
    const val TAG =
      "NomeTwoClientCall"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val PRODUCER_ROLE_ARGUMENT =
      "nomeProducerRole"
    const val PRODUCER_PORT_ARGUMENT =
      "nomeProducerPort"
    const val BRIDGE_HOST =
      "10.0.2.2"
    const val DEFAULT_BRIDGE_PORT =
      27214
    const val BRIDGE_ACK = 1
    const val BRIDGE_CONNECT_TIMEOUT_MILLIS =
      15_000
    const val READY_TIMEOUT_MILLIS =
      120_000L
    const val CALL_TIMEOUT_MILLIS =
      180_000L
    const val END_TIMEOUT_MILLIS =
      60_000L
    const val POLL_MILLIS =
      500L
  }
}
