package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.AgentErrorType
import chat.simplex.common.model.ChatError
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.OperatorTag
import chat.simplex.common.model.ServerProtocol
import chat.simplex.common.model.ServerRoles
import chat.simplex.common.model.UserServer
import chat.simplex.common.views.usersettings.networkAndServers.serverHostname
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeNetworkProducerDiagnosticTest {
  @Test
  fun enabledOfficialServersAreTestedWithoutLoggingAddressesOrCredentials() {
    val arguments = InstrumentationRegistry.getArguments()
    val skipControlledProducer =
      controlledProducerSkipRequested(
        arguments.getString(
          CONTROLLED_PRODUCER_SKIP_ARGUMENT,
        ),
      )
    val diagnosticArgument =
      arguments.getString(
        NETWORK_DIAGNOSTIC_ARGUMENT,
      )
    if (diagnosticArgument == null) {
      if (skipControlledProducer) return
      error(
        "Missing $NETWORK_DIAGNOSTIC_ARGUMENT; " +
          "set $CONTROLLED_PRODUCER_SKIP_ARGUMENT=true " +
          "to bypass this controlled harness in general regression",
      )
    }
    requireTrueArgument(
      NETWORK_DIAGNOSTIC_ARGUMENT,
      diagnosticArgument,
    )
    val skipProtoTests =
      optionalBooleanArgument(
        SKIP_PROTO_TESTS_ARGUMENT,
        arguments.getString(
          SKIP_PROTO_TESTS_ARGUMENT,
        ),
      ) ?: false
    val testOnlyFluxXftp =
      optionalBooleanArgument(
        TEST_ONLY_FLUX_XFTP_ARGUMENT,
        arguments.getString(
          TEST_ONLY_FLUX_XFTP_ARGUMENT,
        ),
      ) ?: false
    val testDisabledXftp =
      optionalBooleanArgument(
        TEST_DISABLED_XFTP_ARGUMENT,
        arguments.getString(
          TEST_DISABLED_XFTP_ARGUMENT,
        ),
      ) ?: false
    val testChatRelays =
      optionalBooleanArgument(
        TEST_CHAT_RELAYS_ARGUMENT,
        arguments.getString(
          TEST_CHAT_RELAYS_ARGUMENT,
        ),
      ) ?: false
    val selectSmpHost =
      optionalNonBlankArgument(
        SELECT_SMP_HOST_ARGUMENT,
        arguments.getString(
          SELECT_SMP_HOST_ARGUMENT,
        ),
      )
    val selectFluxXftpHost =
      optionalNonBlankArgument(
        SELECT_FLUX_XFTP_HOST_ARGUMENT,
        arguments.getString(
          SELECT_FLUX_XFTP_HOST_ARGUMENT,
        ),
      )
    val restoreSimplexXftpHosts =
      optionalHostSetArgument(
        RESTORE_SIMPLEX_XFTP_HOSTS_ARGUMENT,
        arguments.getString(
          RESTORE_SIMPLEX_XFTP_HOSTS_ARGUMENT,
        ),
      )
    val restoreSimplexSmpHosts =
      optionalHostSetArgument(
        RESTORE_SIMPLEX_SMP_HOSTS_ARGUMENT,
        arguments.getString(
          RESTORE_SIMPLEX_SMP_HOSTS_ARGUMENT,
        ),
      )

    val scenario =
      ActivityScenario.launch<MainActivity>(
        Intent().setClassName(
          InstrumentationRegistry.getInstrumentation().targetContext.packageName,
          MainActivity::class.java.name,
        ),
      )
    try {
      waitUntil {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true
      }
      val user = requireNotNull(ChatModel.currentUser.value)
      val operatorConditions =
        runBlocking {
          ChatModel.controller.getServerOperators(user.remoteHostId)
        }
      assertNotNull(operatorConditions)
      operatorConditions
        ?.serverOperators
        .orEmpty()
        .forEach { operator ->
          Log.i(
            TAG,
            "operator=${operator.operatorTag ?: "custom"} " +
              "enabled=${operator.enabled} " +
              "usageAllowed=${operator.conditionsAcceptance.usageAllowed} " +
              "smpStorage=${operator.smpRoles.storage} " +
              "smpProxy=${operator.smpRoles.proxy}",
          )
        }

      val userServers =
        runBlocking {
          ChatModel.controller.getUserServers(user.remoteHostId)
        }
      assertNotNull(userServers)
      userServers
        .orEmpty()
        .forEach { operatorServers ->
          logConfiguredServerSummary(
            operatorTag =
              operatorServers.operator
                ?.operatorTag,
            protocol = ServerProtocol.SMP,
            servers =
              operatorServers
                .smpServers,
          )
          logConfiguredServerSummary(
            operatorTag =
              operatorServers.operator
                ?.operatorTag,
            protocol = ServerProtocol.XFTP,
            servers =
              operatorServers
                .xftpServers,
          )
        }
      if (!skipProtoTests) {
        val enabledSmpServers =
          userServers
            .orEmpty()
            .flatMap { it.smpServers }
            .filter { it.enabled && !it.deleted }
        assertTrue(
          "At least one enabled SMP server is required for the producer diagnostic",
          enabledSmpServers.isNotEmpty(),
        )
        enabledSmpServers.forEach { server ->
          logServerTest(
            protocol = ServerProtocol.SMP,
            server = server,
          )
        }
        val enabledXftpServers =
          userServers
            .orEmpty()
            .flatMap { it.xftpServers }
            .filter { it.enabled && !it.deleted }
        assertTrue(
          "At least one enabled XFTP server is required for the producer diagnostic",
          enabledXftpServers.isNotEmpty(),
        )
        if (!testOnlyFluxXftp) {
          enabledXftpServers.forEach { server ->
            logServerTest(
              protocol = ServerProtocol.XFTP,
              server = server,
            )
          }
        }
        if (testDisabledXftp) {
          userServers
            .orEmpty()
            .filter {
              it.operator?.operatorTag ==
                if (testOnlyFluxXftp) {
                  OperatorTag.Flux
                } else {
                  OperatorTag.SimpleX
                }
            }.flatMap {
              it.xftpServers
            }.filter {
              !it.enabled && !it.deleted
            }.forEach { server ->
              logServerTest(
                protocol = ServerProtocol.XFTP,
                server = server,
              )
            }
        }
      }
      if (testChatRelays) {
        val enabledChatRelays =
          userServers
            .orEmpty()
            .filter {
              it.operator?.enabled != false
            }.flatMap {
              it.chatRelays
            }.filter {
              it.enabled &&
                !it.deleted
            }
        assertTrue(
          "At least one enabled chat relay is required for the public-channel diagnostic",
          enabledChatRelays.isNotEmpty(),
        )
        enabledChatRelays.forEachIndexed {
            index,
            relay,
          ->
          val result =
            runBlocking {
              ChatModel.controller.testChatRelay(
                user.remoteHostId,
                relay.address,
              )
            }
          Log.i(
            TAG,
            "chatRelayIndex=$index " +
              "profileReady=${result.first != null} " +
              "testStep=${result.second?.rtfStep ?: "PASS"} " +
              "safeError=" +
              safeChatError(
                result.second?.rtfError,
              ),
          )
        }
      }
      selectSmpHost?.let { host ->
        selectPassingSimpleXPreset(
          host = host,
          userServers = requireNotNull(userServers),
        )
      }
      selectFluxXftpHost?.let { host ->
        selectPassingFluxXftpPreset(host)
      }
      restoreSimplexXftpHosts?.let { hosts ->
        restoreSimpleXXftpSelection(hosts)
      }
      restoreSimplexSmpHosts?.let { hosts ->
        restoreSimpleXSmpSelection(hosts)
      }
    } finally {
      scenario.close()
    }
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

  private fun requireTrueArgument(
    argumentName: String,
    rawValue: String,
  ) {
    if (rawValue != "true") {
      error("Invalid $argumentName: $rawValue")
    }
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

  private fun optionalNonBlankArgument(
    argumentName: String,
    rawValue: String?,
  ): String? =
    when {
      rawValue == null -> null
      rawValue.isBlank() ->
        error(
          "Invalid $argumentName: must be non-blank when supplied",
        )
      else -> rawValue
    }

  private fun optionalHostSetArgument(
    argumentName: String,
    rawValue: String?,
  ): Set<String>? {
    if (rawValue == null) return null
    val hosts =
      rawValue
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
    if (hosts.isEmpty()) {
      error("Invalid $argumentName: $rawValue")
    }
    return hosts
  }

  private fun selectPassingSimpleXPreset(
    host: String,
    userServers: List<chat.simplex.common.model.UserOperatorServers>,
  ) {
    val target =
      userServers
        .asSequence()
        .filter {
          it.operator?.operatorTag == OperatorTag.SimpleX
        }.flatMap {
          it.smpServers.asSequence()
        }.singleOrNull {
          it.preset &&
            !it.deleted &&
            serverHostname(it.server) == host
        }
    assertNotNull(
      "Requested host must be one existing SimpleX preset",
      target,
    )
    val targetFailure =
      runBlocking {
        ChatModel.controller.testProtoServer(
          target?.remoteHostId,
          requireNotNull(target).server,
        )
      }
    assertTrue(
      "Requested SimpleX preset must pass the official protocol test",
      targetFailure == null,
    )
    val updated =
      userServers.map { operatorServers ->
        if (
          operatorServers.operator?.operatorTag ==
            OperatorTag.SimpleX
        ) {
          operatorServers.copy(
            smpServers =
              operatorServers.smpServers.map { server ->
                server.copy(
                  enabled =
                    !server.deleted &&
                      serverHostname(server.server) == host,
                )
              },
          )
        } else {
          operatorServers
        }
      }
    val validation =
      runBlocking {
        ChatModel.controller.validateServers(
          ChatModel.currentUser.value?.remoteHostId,
          updated,
        )
      }
    assertNotNull(validation)
    assertTrue(
      "Selected preset must satisfy the official server validation",
      validation?.first.orEmpty().isEmpty(),
    )
    assertTrue(
      "Official setUserServers must confirm the controlled change",
      runBlocking {
        ChatModel.controller.setUserServers(
          ChatModel.currentUser.value?.remoteHostId,
          updated,
        )
      },
    )
    val confirmed =
      runBlocking {
        ChatModel.controller.getUserServers(
          ChatModel.currentUser.value?.remoteHostId,
        )
      }
    val enabledSimpleXHosts =
      confirmed
        .orEmpty()
        .filter {
          it.operator?.operatorTag == OperatorTag.SimpleX
        }.flatMap {
          it.smpServers
        }.filter {
          it.enabled && !it.deleted
        }.map {
          serverHostname(it.server)
        }
    assertTrue(
      "The controlled client must retain only the selected SimpleX preset",
      enabledSimpleXHosts == listOf(host),
    )
    Log.i(
      TAG,
      "selectedSimpleXPreset=true " +
        "validationErrors=${validation?.first.orEmpty().size} " +
        "validationWarnings=${validation?.second.orEmpty().size}",
    )
  }

  private fun safeChatError(
    error: ChatError?,
  ): String =
    when (error) {
      null -> "none"
      is ChatError.ChatErrorAgent ->
        when (val agentError = error.agentError) {
          is AgentErrorType.BROKER ->
            "BROKER/" +
              agentError.brokerErr::class.simpleName

          is AgentErrorType.CONN ->
            "CONN/" +
              agentError.connErr::class.simpleName

          is AgentErrorType.SMP ->
            "SMP/" +
              agentError.smpErr::class.simpleName

          is AgentErrorType.PROXY ->
            "PROXY/" +
              agentError.proxyErr::class.simpleName

          else ->
            requireNotNull(
              agentError::class.simpleName,
            )
        }

      else -> error.resultType
    }

  private fun selectPassingFluxXftpPreset(
    host: String,
  ) {
    val rh =
      ChatModel.currentUser.value?.remoteHostId
    val currentOperators =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .getServerOperators(rh)
        },
      )
    val fluxOperator =
      currentOperators.serverOperators
        .singleOrNull {
          it.operatorTag ==
            OperatorTag.Flux
        }
    assertNotNull(
      "The official Flux operator must exist",
      fluxOperator,
    )
    assertTrue(
      "The official Flux operator must currently allow controlled use",
      requireNotNull(fluxOperator)
        .conditionsAcceptance
        .usageAllowed,
    )
    val currentServers =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .getUserServers(rh)
        },
      )
    val target =
      currentServers
        .asSequence()
        .filter {
          it.operator?.operatorTag ==
            OperatorTag.Flux
        }.flatMap {
          it.xftpServers.asSequence()
        }.singleOrNull {
          it.preset &&
            !it.deleted &&
            serverHostname(it.server) == host
        }
    assertNotNull(
      "Requested host must be one existing Flux XFTP preset",
      target,
    )
    assertTrue(
      "Requested Flux XFTP preset must pass the official protocol test",
      runBlocking {
        ChatModel.controller.testProtoServer(
          requireNotNull(target).remoteHostId,
          target.server,
        )
      } == null,
    )
    val updatedOperators =
      currentOperators.serverOperators.map {
        when (it.operatorTag) {
          OperatorTag.SimpleX ->
            it.copy(
              xftpRoles =
                ServerRoles(
                  storage = true,
                  proxy = true,
                ),
            )

          OperatorTag.Flux ->
            it.copy(
              enabled = true,
              smpRoles =
                ServerRoles(
                  storage = false,
                  proxy = false,
                ),
              xftpRoles =
                ServerRoles(
                  storage = true,
                  proxy = true,
                ),
            )

          else -> it
        }
      }
    assertNotNull(
      "Official setServerOperators must confirm the controlled operator routing",
      runBlocking {
        ChatModel.controller
          .setServerOperators(
            rh = rh,
            operators = updatedOperators,
          )
      },
    )
    val refreshedServers =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .getUserServers(rh)
        },
      )
    val updatedServers =
      refreshedServers.map { operatorServers ->
        when (
          operatorServers.operator
            ?.operatorTag
        ) {
          OperatorTag.SimpleX ->
            operatorServers.copy(
              xftpServers =
                operatorServers.xftpServers
                  .map {
                    it.copy(enabled = false)
                  },
            )

          OperatorTag.Flux ->
            operatorServers.copy(
              smpServers =
                operatorServers.smpServers
                  .map {
                    it.copy(enabled = false)
                  },
              xftpServers =
                operatorServers.xftpServers
                  .map { server ->
                    server.copy(
                      enabled =
                        !server.deleted &&
                          serverHostname(
                            server.server,
                          ) == host,
                    )
                  },
            )

          else -> operatorServers
        }
      }
    val validation =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .validateServers(
              rh,
              updatedServers,
            )
        },
      )
    Log.i(
      TAG,
      "controlledRoutingValidation " +
        "errorTypes=${
          validation.first.joinToString(
            prefix = "[",
            postfix = "]",
          ) {
            it::class.simpleName ?: "unknown"
          }
        } " +
        "warningTypes=${
          validation.second.joinToString(
            prefix = "[",
            postfix = "]",
          ) {
            it::class.simpleName ?: "unknown"
          }
        }",
    )
    updatedServers.forEach { operatorServers ->
      operatorServers.operator?.let {
        Log.i(
          TAG,
          "controlledRoutingOperator=${it.operatorTag ?: "custom"} " +
            "enabled=${it.enabled} " +
            "smpStorage=${it.smpRoles.storage} " +
            "smpProxy=${it.smpRoles.proxy} " +
            "xftpStorage=${it.xftpRoles.storage} " +
            "xftpProxy=${it.xftpRoles.proxy} " +
            "enabledSmp=${operatorServers.smpServers.count { server -> server.enabled && !server.deleted }} " +
            "enabledXftp=${operatorServers.xftpServers.count { server -> server.enabled && !server.deleted }}",
        )
      }
    }
    assertTrue(
      "Controlled SMP and XFTP routing must satisfy the official validation",
      validation.first.isEmpty(),
    )
    assertTrue(
      "Official setUserServers must confirm the controlled XFTP route",
      runBlocking {
        ChatModel.controller
          .setUserServers(
            rh,
            updatedServers,
          )
      },
    )
    val confirmed =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .getUserServers(rh)
        },
      )
    val enabledFluxXftpHosts =
      confirmed
        .filter {
          it.operator?.operatorTag ==
            OperatorTag.Flux
        }.flatMap {
          it.xftpServers
        }.filter {
          it.enabled && !it.deleted
        }.map {
          serverHostname(it.server)
        }
    assertTrue(
      "The controlled client must retain only the selected Flux XFTP preset",
      enabledFluxXftpHosts == listOf(host),
    )
    Log.i(
      TAG,
      "selectedFluxXftpPreset=true " +
        "validationErrors=${validation.first.size} " +
        "validationWarnings=${validation.second.size}",
    )
  }

  private fun restoreSimpleXXftpSelection(
    hosts: Set<String>,
  ) {
    val rh =
      ChatModel.currentUser.value?.remoteHostId
    val currentOperators =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .getServerOperators(rh)
        },
      )
    val updatedOperators =
      currentOperators.serverOperators.map {
        when (it.operatorTag) {
          OperatorTag.SimpleX ->
            it.copy(
              enabled = true,
              xftpRoles =
                ServerRoles(
                  storage = true,
                  proxy = true,
                ),
            )

          OperatorTag.Flux ->
            it.copy(
              enabled = false,
              smpRoles =
                ServerRoles(
                  storage = false,
                  proxy = true,
                ),
              xftpRoles =
                ServerRoles(
                  storage = false,
                  proxy = true,
                ),
            )

          else -> it
        }
      }
    assertNotNull(
      "Official setServerOperators must restore the controlled operator selection",
      runBlocking {
        ChatModel.controller
          .setServerOperators(
            rh = rh,
            operators = updatedOperators,
          )
      },
    )
    val currentServers =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .getUserServers(rh)
        },
      )
    val knownSimpleXHosts =
      currentServers
        .filter {
          it.operator?.operatorTag ==
            OperatorTag.SimpleX
        }.flatMap {
          it.xftpServers
        }.filter {
          !it.deleted
        }.map {
          serverHostname(it.server)
        }.toSet()
    assertTrue(
      "Every restored SimpleX XFTP host must be an existing preset",
      hosts.all { it in knownSimpleXHosts },
    )
    val restoredServers =
      currentServers.map { operatorServers ->
        when (
          operatorServers.operator
            ?.operatorTag
        ) {
          OperatorTag.SimpleX ->
            operatorServers.copy(
              xftpServers =
                operatorServers.xftpServers
                  .map { server ->
                    server.copy(
                      enabled =
                        !server.deleted &&
                          serverHostname(
                            server.server,
                          ) in hosts,
                    )
                  },
            )

          OperatorTag.Flux ->
            operatorServers.copy(
              smpServers =
                operatorServers.smpServers
                  .map {
                    it.copy(enabled = false)
                  },
              xftpServers =
                operatorServers.xftpServers
                  .map {
                    it.copy(enabled = false)
                  },
            )

          else -> operatorServers
        }
      }
    val validation =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .validateServers(
              rh,
              restoredServers,
            )
        },
      )
    assertTrue(
      "Restored controlled server selection must satisfy official validation",
      validation.first.isEmpty(),
    )
    assertTrue(
      "Official setUserServers must restore the controlled XFTP selection",
      runBlocking {
        ChatModel.controller
          .setUserServers(
            rh,
            restoredServers,
          )
      },
    )
    val confirmed =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .getUserServers(rh)
        },
      )
    val enabledSimpleXHosts =
      confirmed
        .filter {
          it.operator?.operatorTag ==
            OperatorTag.SimpleX
        }.flatMap {
          it.xftpServers
        }.filter {
          it.enabled && !it.deleted
        }.map {
          serverHostname(it.server)
        }.toSet()
    val enabledFluxServers =
      confirmed
        .filter {
          it.operator?.operatorTag ==
            OperatorTag.Flux
        }.flatMap {
          it.smpServers + it.xftpServers
        }.count {
          it.enabled && !it.deleted
        }
    assertTrue(
      "The controlled client must restore the prior SimpleX XFTP host set",
      enabledSimpleXHosts == hosts,
    )
    assertTrue(
      "The controlled client must leave Flux servers disabled after cleanup",
      enabledFluxServers == 0,
    )
    Log.i(
      TAG,
      "restoredSimpleXXftpSelection=true " +
        "enabledHostCount=${enabledSimpleXHosts.size} " +
        "validationErrors=${validation.first.size} " +
        "validationWarnings=${validation.second.size}",
    )
  }

  private fun restoreSimpleXSmpSelection(
    hosts: Set<String>,
  ) {
    val rh =
      ChatModel.currentUser.value
        ?.remoteHostId
    val current =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .getUserServers(rh)
        },
      )
    val knownSimpleXHosts =
      current
        .filter {
          it.operator?.operatorTag ==
            OperatorTag.SimpleX
        }.flatMap {
          it.smpServers
        }.filter {
          !it.deleted
        }.map {
          serverHostname(it.server)
        }.toSet()
    assertTrue(
      "Every restored SimpleX SMP host must be an existing preset",
      hosts.all {
        it in knownSimpleXHosts
      },
    )
    val restored =
      current.map { operatorServers ->
        if (
          operatorServers.operator
            ?.operatorTag ==
            OperatorTag.SimpleX
        ) {
          operatorServers.copy(
            smpServers =
              operatorServers.smpServers
                .map { server ->
                  server.copy(
                    enabled =
                      !server.deleted &&
                        serverHostname(
                          server.server,
                        ) in hosts,
                  )
                },
          )
        } else {
          operatorServers
        }
      }
    val validation =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .validateServers(
              rh,
              restored,
            )
        },
      )
    assertTrue(
      "Restored controlled SMP selection must satisfy official validation",
      validation.first.isEmpty(),
    )
    assertTrue(
      "Official setUserServers must restore the controlled SMP selection",
      runBlocking {
        ChatModel.controller
          .setUserServers(
            rh,
            restored,
          )
      },
    )
    val confirmed =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .getUserServers(rh)
        },
      )
    val enabledSimpleXHosts =
      confirmed
        .filter {
          it.operator?.operatorTag ==
            OperatorTag.SimpleX
        }.flatMap {
          it.smpServers
        }.filter {
          it.enabled &&
            !it.deleted
        }.map {
          serverHostname(it.server)
        }.toSet()
    assertTrue(
      "The controlled client must restore the prior SimpleX SMP host set",
      enabledSimpleXHosts ==
        hosts,
    )
    Log.i(
      TAG,
      "restoredSimpleXSmpSelection=true " +
        "enabledHostCount=${enabledSimpleXHosts.size} " +
        "validationErrors=${validation.first.size} " +
        "validationWarnings=${validation.second.size}",
    )
  }

  private fun logServerTest(
    protocol: ServerProtocol,
    server: UserServer,
  ) {
    val failure =
      runBlocking {
        ChatModel.controller.testProtoServer(
          server.remoteHostId,
          server.server,
        )
      }
    Log.i(
      TAG,
      "protocol=$protocol " +
        "preset=${server.preset} " +
        "result=${failure?.let { it::class.simpleName } ?: "PASS"}",
    )
  }

  private fun logConfiguredServerSummary(
    operatorTag: OperatorTag?,
    protocol: ServerProtocol,
    servers: List<UserServer>,
  ) {
    Log.i(
      TAG,
      "configuredOperator=${operatorTag ?: "custom"} " +
        "protocol=$protocol " +
        "configuredCount=${servers.size} " +
        "enabledCount=${servers.count { it.enabled && !it.deleted }} " +
        "presetCount=${servers.count { it.preset }} " +
        "testedCount=${servers.count { it.tested != null }}",
    )
  }

  private fun waitUntil(
    predicate: () -> Boolean,
  ) {
    val deadline =
      SystemClock.elapsedRealtime() +
        DIAGNOSTIC_TIMEOUT_MILLIS
    while (
      !predicate() &&
      SystemClock.elapsedRealtime() < deadline
    ) {
      SystemClock.sleep(POLL_MILLIS)
    }
    assertTrue(
      "Timed out waiting for the real controlled client",
      predicate(),
    )
  }

  private companion object {
    const val TAG = "NomeNetworkDiagnostic"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val NETWORK_DIAGNOSTIC_ARGUMENT =
      "nomeNetworkDiagnostic"
    const val SELECT_SMP_HOST_ARGUMENT =
      "nomeSelectSmpHost"
    const val TEST_DISABLED_XFTP_ARGUMENT =
      "nomeTestDisabledXftp"
    const val TEST_CHAT_RELAYS_ARGUMENT =
      "nomeTestChatRelays"
    const val SKIP_PROTO_TESTS_ARGUMENT =
      "nomeSkipProtoTests"
    const val TEST_ONLY_FLUX_XFTP_ARGUMENT =
      "nomeTestOnlyFluxXftp"
    const val SELECT_FLUX_XFTP_HOST_ARGUMENT =
      "nomeSelectFluxXftpHost"
    const val RESTORE_SIMPLEX_XFTP_HOSTS_ARGUMENT =
      "nomeRestoreSimpleXXftpHosts"
    const val RESTORE_SIMPLEX_SMP_HOSTS_ARGUMENT =
      "nomeRestoreSimpleXSmpHosts"
    const val DIAGNOSTIC_TIMEOUT_MILLIS = 120_000L
    const val POLL_MILLIS = 250L
  }
}
