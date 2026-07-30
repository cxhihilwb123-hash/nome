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
import chat.simplex.common.model.ChatPagination
import chat.simplex.common.model.ComposedMessage
import chat.simplex.common.model.MsgContent
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTwoClientMessagingTest {
  @Test
  fun realDirectMessageIsObservedByTheControlledPeer() {
    val arguments = InstrumentationRegistry.getArguments()
    val role =
      controlledProducerRoleOrSkip(arguments)
        ?: return
    val marker =
      requireMarkerArgument(arguments)
    val port =
      bridgePortArgument(arguments)

    val scenario = launchControlledMainActivity()
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true &&
          readyDirectChats().isNotEmpty()
      }
      when (role) {
        ProducerRole.Source -> {
          val direct =
            requireNotNull(
              newestReadyDirectChat(),
            )
          val info =
            direct.chatInfo as ChatInfo.Direct
          val result =
            runBlocking {
              ChatModel.controller.apiSendMessages(
                rh = direct.remoteHostId,
                type = info.chatType,
                id = info.apiId,
                scope = null,
                composedMessages =
                  listOf(
                    ComposedMessage(
                      fileSource = null,
                      quotedItemId = null,
                      msgContent = MsgContent.MCText(marker),
                      mentions = emptyMap(),
                    ),
                  ),
              )
            }
          assertTrue(
            "The official message producer must create a sender-side chat item",
            !result.isNullOrEmpty(),
          )
        }

        ProducerRole.Target -> {
          waitUntil(MESSAGE_TIMEOUT_MILLIS) {
            readyDirectChats()
              .any { direct ->
                runBlocking {
                  val info =
                    direct.chatInfo as ChatInfo.Direct
                  ChatModel.controller.apiGetChat(
                    rh = direct.remoteHostId,
                    type = info.chatType,
                    id = info.apiId,
                    scope = null,
                    pagination = ChatPagination.Last(CHAT_ITEM_LIMIT),
                  )?.first?.chatItems?.any {
                    it.text == marker
                  } == true
                }
              }
          }
        }
      }

      coordinateWithPeer(
        port = port,
        role = role,
      )
      Log.i(
        TAG,
        "role=${role.argument} peerObservedDirectMessage=true",
      )
    } finally {
      scenario?.close()
    }
  }

  private fun readyDirectChats(): List<Chat> =
    ChatModel.chats.value.filter {
      (it.chatInfo as? ChatInfo.Direct)
        ?.ready == true
    }

  private fun newestReadyDirectChat(): Chat? =
    readyDirectChats()
      .maxByOrNull {
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
        "Both controlled clients must finish the same peer-observation attempt",
        socket.getInputStream().read() == BRIDGE_ACK,
      )
    }
  }

  private fun bridgeSocket(
    port: Int,
  ): Socket =
    Socket().apply {
      soTimeout = MESSAGE_TIMEOUT_MILLIS.toInt()
      connect(
        InetSocketAddress(
          controlledBridgeHost(),
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
      "Timed out waiting for the controlled two-client message state",
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

  private fun requireMarkerArgument(
    arguments: android.os.Bundle,
  ): String {
    val marker =
      arguments.getString(
        MESSAGE_MARKER_ARGUMENT,
      ) ?: error(
        "Missing $MESSAGE_MARKER_ARGUMENT",
      )
    if (
      marker.startsWith(
        MESSAGE_MARKER_PREFIX,
      ) &&
      marker.length <= MAX_MARKER_LENGTH
    ) {
      return marker
    }
    error(
      "Invalid $MESSAGE_MARKER_ARGUMENT=$marker",
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
    const val TAG = "NomeTwoClientMessage"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val PRODUCER_ROLE_ARGUMENT =
      "nomeProducerRole"
    const val PRODUCER_PORT_ARGUMENT =
      "nomeProducerPort"
    const val MESSAGE_MARKER_ARGUMENT =
      "nomeMessageMarker"
    const val MESSAGE_MARKER_PREFIX =
      "NomePeerMessage-"
    const val BRIDGE_HOST = "10.0.2.2"
    const val DEFAULT_BRIDGE_PORT = 27191
    const val BRIDGE_ACK = 1
    const val MAX_MARKER_LENGTH = 96
    const val CHAT_ITEM_LIMIT = 100
    const val BRIDGE_CONNECT_TIMEOUT_MILLIS = 15_000
    const val READY_TIMEOUT_MILLIS = 120_000L
    const val MESSAGE_TIMEOUT_MILLIS = 180_000L
    const val POLL_MILLIS = 1_000L
  }
}
