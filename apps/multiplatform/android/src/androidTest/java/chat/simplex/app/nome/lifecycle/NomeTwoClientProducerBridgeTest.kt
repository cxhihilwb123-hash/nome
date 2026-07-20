package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.APIConnectPlanResult
import chat.simplex.common.model.APIConnectResult
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatModel
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTwoClientProducerBridgeTest {
  @Test
  fun realInvitationBearerCrossesOnlyTheEphemeralMemoryBridge() {
    val arguments = InstrumentationRegistry.getArguments()
    val skipControlledProducer =
      controlledProducerSkipRequested(
        arguments.getString(
          CONTROLLED_PRODUCER_SKIP_ARGUMENT,
        ),
      )
    val roleArgument =
      arguments.getString(PRODUCER_ROLE_ARGUMENT)
    val role =
      when {
        roleArgument == null -> {
          if (skipControlledProducer) return
          error(
            "Missing $PRODUCER_ROLE_ARGUMENT; " +
              "set $CONTROLLED_PRODUCER_SKIP_ARGUMENT=true " +
              "to bypass this controlled harness in general regression",
          )
        }

        else ->
          ProducerRole.entries.firstOrNull { role ->
            role.argument == roleArgument
          }
            ?: error(
              "Invalid $PRODUCER_ROLE_ARGUMENT: $roleArgument",
            )
      }
    val portArgument =
      arguments.getString(PRODUCER_PORT_ARGUMENT)
    val port =
      portArgument
        ?.toIntOrNull()
        ?.takeIf { it in 1..65535 }
        ?: error(
          "Missing or invalid $PRODUCER_PORT_ARGUMENT: $portArgument",
        )

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
      val user = requireNotNull(ChatModel.currentUser.value)
      val initialDirectIds = readyDirectIds()
      when (role) {
        ProducerRole.Source -> {
          val invitation =
            runBlocking {
              ChatModel.controller.apiAddContact(
                user.remoteHostId,
                incognito = false,
              )
            }.first
          assertNotNull(
            "The official invitation producer must return a result",
            invitation,
          )
          sendBearerToMemoryBridge(
            port = port,
            bearer =
              requireNotNull(invitation)
                .first
                .connFullLink,
          )
        }

        ProducerRole.Target -> {
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
            "The official connection planner must accept the in-memory invitation",
            plan is APIConnectPlanResult.Ready,
          )
          val connection =
            (plan as APIConnectPlanResult.Ready)
              .connectionLink
          val result =
            runBlocking {
              ChatModel.controller.apiConnectResult(
                rh = user.remoteHostId,
                userId = user.userId,
                incognito = false,
                connLink = connection,
              )
            }
          assertTrue(
            "The official connection command must reach a pending terminal result",
            result is APIConnectResult.Pending,
          )
        }
      }

      waitUntil(CONNECTION_TIMEOUT_MILLIS) {
        readyDirectIds().any {
          it !in initialDirectIds
        }
      }
      Log.i(
        TAG,
        "role=${role.argument} newReadyDirectContact=true",
      )
    } finally {
      scenario.close()
    }
  }

  private fun readyDirectIds(): Set<Long> =
    ChatModel.chats.value
      .mapNotNull { chat ->
        (chat.chatInfo as? ChatInfo.Direct)
          ?.takeIf { it.ready }
          ?.apiId
      }.toSet()

  private fun sendBearerToMemoryBridge(
    port: Int,
    bearer: String,
  ) {
    bridgeSocket(port).use { socket ->
      val output = DataOutputStream(socket.getOutputStream())
      output.writeByte(ProducerRole.Source.wireValue)
      val bytes = bearer.toByteArray(Charsets.UTF_8)
      try {
        assertTrue(
          "The invitation must stay within the bounded in-memory payload",
          bytes.size in 1..MAX_BEARER_BYTES,
        )
        output.writeInt(bytes.size)
        output.write(bytes)
        output.flush()
        assertTrue(
          "The target must acknowledge the in-memory handoff",
          socket.getInputStream().read() == BRIDGE_ACK,
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
      val output = DataOutputStream(socket.getOutputStream())
      output.writeByte(ProducerRole.Target.wireValue)
      output.flush()
      val input = DataInputStream(socket.getInputStream())
      val length = input.readInt()
      assertTrue(
        "The bridge payload length must stay within the connection-link bound",
        length in 1..MAX_BEARER_BYTES,
      )
      val bytes = ByteArray(length)
      try {
        input.readFully(bytes)
        String(bytes, Charsets.UTF_8)
      } finally {
        bytes.fill(0)
      }
    }

  private fun bridgeSocket(
    port: Int,
  ): Socket =
    Socket().apply {
      soTimeout = CONNECTION_TIMEOUT_MILLIS.toInt()
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
      "Timed out waiting for the controlled two-client state",
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

  private companion object {
    const val TAG = "NomeTwoClientProducer"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val PRODUCER_ROLE_ARGUMENT =
      "nomeProducerRole"
    const val PRODUCER_PORT_ARGUMENT =
      "nomeProducerPort"
    const val BRIDGE_HOST = "10.0.2.2"
    const val BRIDGE_ACK = 1
    const val MAX_BEARER_BYTES = 16_384
    const val BRIDGE_CONNECT_TIMEOUT_MILLIS = 15_000
    const val READY_TIMEOUT_MILLIS = 120_000L
    const val CONNECTION_TIMEOUT_MILLIS = 300_000L
    const val POLL_MILLIS = 250L
  }
}
