package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.API
import chat.simplex.common.model.APIConnectPlanResult
import chat.simplex.common.model.APIConnectResult
import chat.simplex.common.model.APIUserAddressResult
import chat.simplex.common.model.AgentErrorType
import chat.simplex.common.model.ChatError
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatModel
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTwoClientContactRequestTest {
  @Test
  fun realContactAddressProducesAnIncomingRequestOnItsOwner() {
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
          ProducerRole.entries.firstOrNull {
            it.argument == roleArgument
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
    val createAddressIfAbsent =
      optionalBooleanArgument(
        CREATE_ADDRESS_IF_ABSENT_ARGUMENT,
        arguments.getString(
          CREATE_ADDRESS_IF_ABSENT_ARGUMENT,
        ),
      )
    val recreateAddress =
      optionalBooleanArgument(
        RECREATE_ADDRESS_ARGUMENT,
        arguments.getString(
          RECREATE_ADDRESS_ARGUMENT,
        ),
      ) ?: false
    val disposableOwner =
      optionalBooleanArgument(
        DISPOSABLE_OWNER_ARGUMENT,
        arguments.getString(
          DISPOSABLE_OWNER_ARGUMENT,
        ),
      )

    val scenario = launchControlledMainActivity()
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true
      }
      val user = requireNotNull(ChatModel.currentUser.value)
      bridgeSocket(port).use { socket ->
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())
        output.writeByte(role.wireValue)
        output.flush()
        when (role) {
          ProducerRole.AddressOwner -> {
            val initialRequestIds = pendingContactRequestIds()
            if (recreateAddress) {
              assertTrue(
                "Address recreation is restricted to an explicitly disposable owner",
                disposableOwner == true,
              )
              when (
                runBlocking {
                  ChatModel.controller.apiGetUserAddressResult(
                    user.remoteHostId,
                  )
                }
              ) {
                is APIUserAddressResult.Ready ->
                  assertTrue(
                    "The disposable owner's previous address must be deleted by the official API",
                    runBlocking {
                      ChatModel.controller
                        .apiDeleteUserAddress(
                          user.remoteHostId,
                        )
                    } != null,
                  )

                APIUserAddressResult.NotFound -> Unit
                APIUserAddressResult.Failure ->
                  throw AssertionError(
                    "The disposable owner must expose a stable address result before recreation",
                  )
              }
              waitUntil(ADDRESS_TRANSITION_TIMEOUT_MILLIS) {
                runBlocking {
                  ChatModel.controller.apiGetUserAddressResult(
                    user.remoteHostId,
                  )
                } == APIUserAddressResult.NotFound
              }
            }
            val addressResult =
              runBlocking {
                ChatModel.controller.apiGetUserAddressResult(
                  user.remoteHostId,
                )
              }
            val bearer =
              when (addressResult) {
                is APIUserAddressResult.Ready ->
                  addressResult.address
                    .connLinkContact
                    .connFullLink

                APIUserAddressResult.NotFound -> {
                  assertTrue(
                    "A missing address can be created only on an explicitly disposable owner",
                    createAddressIfAbsent == true,
                  )
                  requireNotNull(
                    runBlocking {
                      ChatModel.controller
                        .apiCreateUserAddress(
                          user.remoteHostId,
                        )
                    },
                  ).connFullLink
                }

                APIUserAddressResult.Failure ->
                  throw AssertionError(
                    "The official address owner must return a stable ready/not-found result",
                  )
              }
            val bytes = bearer.toByteArray(Charsets.UTF_8)
            try {
              assertTrue(
                "The address must stay within the bounded in-memory payload",
                bytes.size in 1..MAX_BEARER_BYTES,
              )
              output.writeInt(bytes.size)
              output.write(bytes)
              output.flush()
            } finally {
              bytes.fill(0)
            }
            waitUntil(REQUEST_TIMEOUT_MILLIS) {
              pendingContactRequestIds().any {
                it !in initialRequestIds
              }
            }
          }

          ProducerRole.Requester -> {
            val length = input.readInt()
            assertTrue(
              "The bridge payload length must stay within the connection-link bound",
              length in 1..MAX_BEARER_BYTES,
            )
            val bytes = ByteArray(length)
            val bearer =
              try {
                input.readFully(bytes)
                String(bytes, Charsets.UTF_8)
              } finally {
                bytes.fill(0)
              }
            val plan =
              runBlocking {
                ChatModel.controller.apiConnectPlanResult(
                  rh = user.remoteHostId,
                  userId = user.userId,
                  connLink = bearer,
                )
              }
            assertTrue(
              "The official planner must accept the controlled contact address",
              plan is APIConnectPlanResult.Ready,
            )
            val result =
              runBlocking {
                ChatModel.controller.apiConnectResult(
                  rh = user.remoteHostId,
                  userId = user.userId,
                  incognito = false,
                  connLink =
                    (plan as APIConnectPlanResult.Ready)
                      .connectionLink,
                )
              }
            assertTrue(
              "The official contact-address command must reach its pending result; " +
                "safeResult=${safeConnectResult(result)}",
              result is APIConnectResult.Pending,
            )
          }
        }
        output.writeByte(CLIENT_COMPLETE)
        output.flush()
        assertTrue(
          "Both controlled clients must complete the same incoming-request attempt",
          input.read() == BRIDGE_ACK,
        )
      }
      Log.i(
        TAG,
        "role=${role.argument} contactRequestProducerComplete=true",
      )
    } finally {
      scenario?.close()
    }
  }

  private fun pendingContactRequestIds(): Set<Long> =
    ChatModel.chats.value
      .mapNotNull {
        when (val info = it.chatInfo) {
          is ChatInfo.ContactRequest ->
            info.apiId

          is ChatInfo.Direct ->
            info.contact
              .contactRequestId
              ?.takeIf {
                info.contact.nextAcceptContactRequest
              }

          else -> null
        }
      }.toSet()

  private fun safeConnectResult(
    result: APIConnectResult,
  ): String =
    when (result) {
      is APIConnectResult.Pending -> "pending"
      is APIConnectResult.AlreadyExists -> "already-exists"
      is APIConnectResult.Failure ->
        when (val response = result.response) {
          is API.Result ->
            "failure/result/${response.res.responseType}"
          is API.Error ->
            when (val error = response.err) {
              is ChatError.ChatErrorAgent ->
                "failure/agent/${safeAgentError(error.agentError)}"
              else ->
                "failure/${error.resultType}"
            }
        }
      APIConnectResult.NoCurrentUser -> "no-current-user"
    }

  private fun safeAgentError(
    error: AgentErrorType,
  ): String =
    when (error) {
      is AgentErrorType.BROKER ->
        "BROKER/${error.brokerErr::class.simpleName}"
      is AgentErrorType.CONN ->
        "CONN/${error.connErr::class.simpleName}"
      is AgentErrorType.SMP ->
        "SMP/${error.smpErr::class.simpleName}"
      is AgentErrorType.PROXY ->
        "PROXY/${error.proxyErr::class.simpleName}"
      else ->
        requireNotNull(error::class.simpleName)
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

  private fun optionalBooleanArgument(
    argumentName: String,
    rawValue: String?,
  ): Boolean? =
    when (rawValue) {
      null -> null
      "true" -> true
      "false" -> false
      else -> error("Invalid $argumentName: $rawValue")
    }

  private fun bridgeSocket(
    port: Int,
  ): Socket =
    Socket().apply {
      soTimeout = REQUEST_TIMEOUT_MILLIS.toInt()
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
      "Timed out waiting for the controlled incoming contact request",
      predicate(),
    )
  }

  private enum class ProducerRole(
    val argument: String,
    val wireValue: Int,
  ) {
    AddressOwner("address-owner", 1),
    Requester("requester", 2),
  }

  private companion object {
    const val TAG = "NomeContactRequestProducer"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val PRODUCER_ROLE_ARGUMENT =
      "nomeContactRequestRole"
    const val PRODUCER_PORT_ARGUMENT =
      "nomeContactRequestPort"
    const val CREATE_ADDRESS_IF_ABSENT_ARGUMENT =
      "nomeCreateAddressIfAbsent"
    const val RECREATE_ADDRESS_ARGUMENT =
      "nomeRecreateAddress"
    const val DISPOSABLE_OWNER_ARGUMENT =
      "nomeDisposableOwner"
    const val BRIDGE_HOST = "10.0.2.2"
    const val CLIENT_COMPLETE = 1
    const val BRIDGE_ACK = 1
    const val MAX_BEARER_BYTES = 16_384
    const val BRIDGE_CONNECT_TIMEOUT_MILLIS = 15_000
    const val READY_TIMEOUT_MILLIS = 120_000L
    const val ADDRESS_TRANSITION_TIMEOUT_MILLIS = 30_000L
    const val REQUEST_TIMEOUT_MILLIS = 300_000L
    const val POLL_MILLIS = 500L
  }
}
