package chat.simplex.app.nome.lifecycle

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.model.ChatModel
import chat.simplex.common.platform.initChatController
import chat.simplex.common.views.database.clearPlatformDatabaseKeyReadState
import chat.simplex.common.views.helpers.DBMigrationResult
import chat.simplex.common.views.helpers.DatabaseUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeControlledDatabaseKeyBridgeTest {
  @Test
  fun controlledDatabaseKeyCrossesOnlyTheEphemeralMemoryBridge() {
    val arguments = InstrumentationRegistry.getArguments()
    val skipControlledProducer =
      controlledProducerSkipRequested(
        arguments.getString(
          CONTROLLED_PRODUCER_SKIP_ARGUMENT,
        ),
      )
    val roleArgument =
      arguments.getString(TRANSFER_ROLE_ARGUMENT)
    val role =
      when {
        roleArgument == null -> {
          if (skipControlledProducer) return
          error(
            "Missing $TRANSFER_ROLE_ARGUMENT; " +
              "set $CONTROLLED_PRODUCER_SKIP_ARGUMENT=true " +
              "to bypass this controlled harness in general regression",
          )
        }

        else ->
          TransferRole.entries.firstOrNull {
            it.argument == roleArgument
          }
            ?: error(
              "Invalid $TRANSFER_ROLE_ARGUMENT: $roleArgument",
            )
      }
    val portArgument =
      arguments.getString(TRANSFER_PORT_ARGUMENT)
    val port =
      portArgument
        ?.toIntOrNull()
        ?.takeIf { it in 1..65535 }
        ?: error(
          "Missing or invalid $TRANSFER_PORT_ARGUMENT: $portArgument",
        )

    when (role) {
      TransferRole.Source -> {
        val key =
          requireNotNull(
            DatabaseUtils.ksDatabasePassword.get(),
          )
        sendKeyToMemoryBridge(
          port = port,
          key = key,
          initialRandom =
            ChatModel.controller.appPrefs
              .initialRandomDBPassphrase
              .get(),
        )
      }

      TransferRole.Target -> {
        val payload =
          receiveKeyFromMemoryBridge(port)
        DatabaseUtils.ksDatabasePassword.set(
          payload.key,
        )
        ChatModel.controller.appPrefs
          .storeDBPassphrase
          .set(true)
        ChatModel.controller.appPrefs
          .initialRandomDBPassphrase
          .set(payload.initialRandom)
        assertEquals(
          "The target keystore must recover the controlled database key",
          payload.key,
          DatabaseUtils.ksDatabasePassword.get(),
        )
        clearPlatformDatabaseKeyReadState()
        ChatModel.chatDbStatus.value = null
        runBlocking {
          initChatController(
            useKey = payload.key,
          )
        }
        assertTrue(
          "The official recovery path must open the imported controlled database; status=${ChatModel.chatDbStatus.value}",
          ChatModel.chatDbStatus.value is
            DBMigrationResult.OK,
        )
      }
    }

    Log.i(
      TAG,
      "role=${role.argument} controlledDatabaseKeyHandoff=true",
    )
  }

  private fun sendKeyToMemoryBridge(
    port: Int,
    key: String,
    initialRandom: Boolean,
  ) {
    bridgeSocket(port).use { socket ->
      val output =
        DataOutputStream(
          socket.getOutputStream(),
        )
      val bytes =
        key.toByteArray(Charsets.UTF_8)
      try {
        assertTrue(
          "The controlled source database key must stay within the handoff bound",
          bytes.size in 1..MAX_KEY_BYTES,
        )
        output.writeByte(TransferRole.Source.wireValue)
        output.writeBoolean(initialRandom)
        output.writeInt(bytes.size)
        output.write(bytes)
        output.flush()
        assertTrue(
          "The target must acknowledge the controlled key handoff",
          socket.getInputStream().read() == BRIDGE_ACK,
        )
      } finally {
        bytes.fill(0)
      }
    }
  }

  private fun receiveKeyFromMemoryBridge(
    port: Int,
  ): KeyPayload =
    bridgeSocket(port).use { socket ->
      val output =
        DataOutputStream(
          socket.getOutputStream(),
        )
      output.writeByte(TransferRole.Target.wireValue)
      output.flush()
      val input =
        DataInputStream(
          socket.getInputStream(),
        )
      val initialRandom =
        input.readBoolean()
      val length =
        input.readInt()
      assertTrue(
        "The bridged database key length must stay within the handoff bound",
        length in 1..MAX_KEY_BYTES,
      )
      val bytes =
        ByteArray(length)
      val key =
        try {
          input.readFully(bytes)
          String(
            bytes,
            Charsets.UTF_8,
          )
        } finally {
          bytes.fill(0)
        }
      output.writeByte(BRIDGE_ACK)
      output.flush()
      KeyPayload(
        key = key,
        initialRandom = initialRandom,
      )
    }

  private fun bridgeSocket(
    port: Int,
  ): Socket =
    Socket().apply {
      soTimeout = BRIDGE_TIMEOUT_MILLIS
      connect(
        InetSocketAddress(
          BRIDGE_HOST,
          port,
        ),
        BRIDGE_TIMEOUT_MILLIS,
      )
    }

  private data class KeyPayload(
    val key: String,
    val initialRandom: Boolean,
  )

  private enum class TransferRole(
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
    const val TAG = "NomeDatabaseKeyBridge"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val TRANSFER_ROLE_ARGUMENT =
      "nomeDatabaseKeyRole"
    const val TRANSFER_PORT_ARGUMENT =
      "nomeDatabaseKeyPort"
    const val BRIDGE_HOST = "10.0.2.2"
    const val BRIDGE_ACK = 1
    const val MAX_KEY_BYTES = 512
    const val BRIDGE_TIMEOUT_MILLIS = 30_000
  }
}
