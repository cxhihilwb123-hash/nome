package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.RemoteCtrlAddress
import chat.simplex.common.model.RemoteCtrlSession
import chat.simplex.common.model.RemoteHostSessionState
import chat.simplex.common.model.UIRemoteCtrlSessionState
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTwoClientRemoteDesktopLifecycleTest {
  @Test
  fun officialPairVerifySwitchStopDeleteLifecycleClearsTheControlledPair() {
    val arguments =
      InstrumentationRegistry.getArguments()
    val role =
      controlledRoleOrSkip(arguments)
        ?: return
    val bridgePort =
      bridgePortArgument(arguments)
    val remotePort =
      remotePortArgument(arguments)
    val instrumentation =
      InstrumentationRegistry.getInstrumentation()
    val scenario = launchControlledMainActivity()
    var controlledRemoteHostId: Long? =
      null
    var controlledRemoteCtrlId: Long? =
      null
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true
      }
      bridgeSocket(
        bridgePort,
      ).use { socket ->
        val reader =
          BufferedReader(
            InputStreamReader(
              socket.getInputStream(),
              StandardCharsets.UTF_8,
            ),
          )
        val writer =
          BufferedWriter(
            OutputStreamWriter(
              socket.getOutputStream(),
              StandardCharsets.UTF_8,
            ),
          )
        writeLine(
          writer,
          "R ${role.argument}",
        )
        when (role) {
          Role.Host -> {
            controlledRemoteHostId =
              runHostLifecycle(
                reader = reader,
                writer = writer,
                remotePort = remotePort,
              )
          }

          Role.Controller -> {
            controlledRemoteCtrlId =
              runControllerLifecycle(
                reader = reader,
                writer = writer,
              )
          }
        }
      }
      Log.i(
        TAG,
        "role=${role.argument} " +
          "officialRemotePairSwitchRevoke=true",
      )
    } finally {
      runBlocking {
        val activeRemoteHostId =
          ChatModel.currentRemoteHost.value
            ?.remoteHostId
        val activeRemoteCtrlId =
          when (
            val state =
              ChatModel.remoteCtrlSession.value
                ?.sessionState
          ) {
            is UIRemoteCtrlSessionState
              .Connected ->
              state.remoteCtrl
                .remoteCtrlId

            is UIRemoteCtrlSessionState
              .PendingConfirmation ->
              state.remoteCtrl_
                ?.remoteCtrlId

            is UIRemoteCtrlSessionState
              .Connecting ->
              state.remoteCtrl_
                ?.remoteCtrlId

            else -> null
          }
        if (
          ChatModel.currentRemoteHost.value !=
          null
        ) {
          ChatModel.controller
            .switchUIRemoteHost(null)
        }
        (
          controlledRemoteHostId
            ?: activeRemoteHostId
        )?.let {
          if (
            ChatModel.controller
              .listRemoteHosts()
              ?.any { host ->
                host.remoteHostId == it
              } == true
          ) {
            ChatModel.controller
              .stopRemoteHost(it)
            ChatModel.controller
              .deleteRemoteHost(it)
          }
        }
        if (
          role == Role.Host &&
          controlledRemoteHostId == null &&
          activeRemoteHostId == null
        ) {
          ChatModel.controller
            .stopRemoteHost(null)
        }
        if (
          ChatModel.remoteCtrlSession.value !=
          null
        ) {
          ChatModel.controller
            .stopRemoteCtrl()
        }
        (
          controlledRemoteCtrlId
            ?: activeRemoteCtrlId
        )?.let {
          if (
            ChatModel.controller
              .listRemoteCtrls()
              ?.any { ctrl ->
                ctrl.remoteCtrlId == it
              } == true
          ) {
            ChatModel.controller
              .deleteRemoteCtrl(it)
          }
        }
      }
      ChatModel.remoteHostPairing.value =
        null
      ChatModel.remoteCtrlSession.value =
        null
      scenario?.close()
    }
  }

  private fun runHostLifecycle(
    reader: BufferedReader,
    writer: BufferedWriter,
    remotePort: Int,
  ): Long {
    val initialIds =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .listRemoteHosts()
        },
      ).map {
        it.remoteHostId
      }.toSet()
    val started =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .startRemoteHost(
              rhId = null,
              multicast = false,
              address =
                RemoteCtrlAddress(
                  address = LOOPBACK_ADDRESS,
                  `interface` =
                    LOOPBACK_INTERFACE,
                ),
              port = remotePort,
            )
        },
      )
    assertTrue(
      "The official remote-host owner must return a non-empty invitation",
      started.invitation.isNotBlank(),
    )
    assertEquals(
      "The official remote-host owner must bind the controlled port",
      remotePort,
      started.ctrlPort.toIntOrNull(),
    )
    assertTrue(
      "The official remote-host owner must expose the controlled loopback address",
      started.localAddrs.any {
        it.address ==
          LOOPBACK_ADDRESS
      },
    )
    val invitationBytes =
      started.invitation.toByteArray(
        StandardCharsets.UTF_8,
      )
    val encodedInvitation =
      try {
        Base64.encodeToString(
          invitationBytes,
          Base64.NO_WRAP,
        )
      } finally {
        invitationBytes.fill(0)
      }
    writeLine(
      writer,
      "I $encodedInvitation",
    )
    val sessionCode =
      waitForHostSessionCode()
    writeLine(
      writer,
      "C ${sessionCode.sha256()}",
    )
    assertEquals(
      "Both controlled clients must observe the same official session code",
      CODE_MATCHED,
      reader.readLine(),
    )
    waitUntil(CONNECTION_TIMEOUT_MILLIS) {
      ChatModel.currentRemoteHost.value
        ?.sessionState is
        RemoteHostSessionState.Connected
    }
    val connected =
      requireNotNull(
        ChatModel.currentRemoteHost.value,
      )
    assertFalse(
      "The connected remote host must be newly created by this controlled attempt",
      connected.remoteHostId in
        initialIds,
    )
    val remoteHostId =
      connected.remoteHostId
    assertTrue(
      "The official remote-host list must contain the connected controlled host",
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .listRemoteHosts()
        },
      ).any {
        it.remoteHostId ==
          remoteHostId &&
          it.sessionState is
          RemoteHostSessionState.Connected
      },
    )
    writeLine(
      writer,
      CONNECTED,
    )
    assertEquals(
      "Both controlled clients must reach the connected state before switching",
      CONNECTED_MATCHED,
      reader.readLine(),
    )
    runBlocking {
      ChatModel.controller
        .switchUIRemoteHost(null)
    }
    waitUntil(SWITCH_TIMEOUT_MILLIS) {
      ChatModel.currentRemoteHost.value ==
        null &&
        ChatModel.currentUser.value !=
        null
    }
    assertNull(
      "The official switch owner must restore the local UI session",
      ChatModel.currentRemoteHost.value,
    )
    assertTrue(
      "The official stop owner must stop the controlled remote host",
      runBlocking {
        ChatModel.controller
          .stopRemoteHost(
            remoteHostId,
          )
      },
    )
    assertTrue(
      "The official delete owner must delete the controlled remote host",
      runBlocking {
        ChatModel.controller
          .deleteRemoteHost(
            remoteHostId,
          )
      },
    )
    assertFalse(
      "The deleted controlled remote host must be absent from the official host list",
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .listRemoteHosts()
        },
      ).any {
        it.remoteHostId ==
          remoteHostId
      },
    )
    writeLine(
      writer,
      CLEANED,
    )
    assertEquals(
      "Both controlled clients must delete the same pairing before completion",
      CLEANED_MATCHED,
      reader.readLine(),
    )
    return remoteHostId
  }

  private fun runControllerLifecycle(
    reader: BufferedReader,
    writer: BufferedWriter,
  ): Long {
    val initialIds =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .listRemoteCtrls()
        },
      ).map {
        it.remoteCtrlId
      }.toSet()
    val invitationLine =
      requireNotNull(
        reader.readLine(),
      )
    assertTrue(
      "The memory bridge must provide only the controlled invitation frame",
      invitationLine.startsWith(
        INVITATION_PREFIX,
      ),
    )
    val invitationBytes =
      Base64.decode(
        invitationLine.removePrefix(
          INVITATION_PREFIX,
        ),
        Base64.DEFAULT,
      )
    val invitation =
      try {
        invitationBytes.toString(
          StandardCharsets.UTF_8,
        )
      } finally {
        invitationBytes.fill(0)
      }
    val connecting =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .connectRemoteCtrl(
              invitation,
            ).first
        },
      )
    ChatModel.remoteCtrlSession.value =
      RemoteCtrlSession(
        ctrlAppInfo =
          connecting.ctrlAppInfo,
        appVersion =
          connecting.appVersion,
        sessionState =
          UIRemoteCtrlSessionState
            .Connecting(
              remoteCtrl_ =
                connecting.remoteCtrl_,
            ),
      )
    val sessionCode =
      waitForControllerSessionCode()
    writeLine(
      writer,
      "C ${sessionCode.sha256()}",
    )
    assertEquals(
      "Both controlled clients must observe the same official session code",
      CODE_MATCHED,
      reader.readLine(),
    )
    val connected =
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .verifyRemoteCtrlSession(
              sessionCode,
            )
        },
      )
    assertFalse(
      "The connected remote controller must be newly created by this controlled attempt",
      connected.remoteCtrlId in
        initialIds,
    )
    val remoteCtrlId =
      connected.remoteCtrlId
    ChatModel.remoteCtrlSession.value =
      ChatModel.remoteCtrlSession.value
        ?.copy(
          sessionState =
            UIRemoteCtrlSessionState
              .Connected(
                remoteCtrl =
                  connected,
                sessionCode =
                  sessionCode,
              ),
        )
    assertNotNull(
      "The production controller model must retain the connected official session",
      ChatModel.remoteCtrlSession.value,
    )
    assertTrue(
      "The official remote-controller list must contain the connected controlled device",
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .listRemoteCtrls()
        },
      ).any {
        it.remoteCtrlId ==
          remoteCtrlId
      },
    )
    writeLine(
      writer,
      CONNECTED,
    )
    assertEquals(
      "Both controlled clients must reach the connected state before switching",
      CONNECTED_MATCHED,
      reader.readLine(),
    )
    waitUntil(STOP_TIMEOUT_MILLIS) {
      ChatModel.remoteCtrlSession.value ==
        null
    }
    assertTrue(
      "The official delete owner must delete the controlled remote controller",
      runBlocking {
        ChatModel.controller
          .deleteRemoteCtrl(
            remoteCtrlId,
          )
      },
    )
    assertFalse(
      "The deleted controlled remote controller must be absent from the official controller list",
      requireNotNull(
        runBlocking {
          ChatModel.controller
            .listRemoteCtrls()
        },
      ).any {
        it.remoteCtrlId ==
          remoteCtrlId
      },
    )
    writeLine(
      writer,
      CLEANED,
    )
    assertEquals(
      "Both controlled clients must delete the same pairing before completion",
      CLEANED_MATCHED,
      reader.readLine(),
    )
    return remoteCtrlId
  }

  private fun waitForHostSessionCode(): String {
    var sessionCode: String? =
      null
    waitUntil(CONNECTION_TIMEOUT_MILLIS) {
      sessionCode =
        (
          ChatModel.remoteHostPairing.value
            ?.second as?
            RemoteHostSessionState
              .PendingConfirmation
        )?.sessionCode
      sessionCode !=
        null
    }
    return requireNotNull(
      sessionCode,
    )
  }

  private fun waitForControllerSessionCode(): String {
    var sessionCode: String? =
      null
    waitUntil(CONNECTION_TIMEOUT_MILLIS) {
      sessionCode =
        (
          ChatModel.remoteCtrlSession.value
            ?.sessionState as?
            UIRemoteCtrlSessionState
              .PendingConfirmation
        )?.sessionCode
      sessionCode !=
        null
    }
    return requireNotNull(
      sessionCode,
    )
  }

  private fun bridgeSocket(
    port: Int,
  ): Socket =
    Socket().apply {
      soTimeout =
        BRIDGE_TIMEOUT_MILLIS
          .toInt()
      connect(
        InetSocketAddress(
          controlledBridgeHost(),
          port,
        ),
        BRIDGE_CONNECT_TIMEOUT_MILLIS,
      )
    }

  private fun writeLine(
    writer: BufferedWriter,
    line: String,
  ) {
    writer.write(line)
    writer.newLine()
    writer.flush()
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
      SystemClock.elapsedRealtime() <
      deadline
    ) {
      SystemClock.sleep(
        POLL_MILLIS,
      )
    }
    assertTrue(
      "Timed out waiting for the controlled two-client remote-desktop state",
      predicate(),
    )
  }

  private fun String.sha256(): String {
    val bytes =
      toByteArray(
        StandardCharsets.UTF_8,
      )
    return try {
      MessageDigest
        .getInstance(
          "SHA-256",
        ).digest(
          bytes,
        ).joinToString(
          "",
        ) {
          "%02x".format(it)
        }
    } finally {
      bytes.fill(0)
    }
  }

  private enum class Role(
    val argument: String,
  ) {
    Host(
      "host",
    ),
    Controller(
      "controller",
    ),
  }

  private companion object {
    const val TAG =
      "NomeRemoteLifecycle"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val ROLE_ARGUMENT =
      "nomeRemoteRole"
    const val BRIDGE_PORT_ARGUMENT =
      "nomeRemoteBridgePort"
    const val REMOTE_PORT_ARGUMENT =
      "nomeRemoteSessionPort"
    const val BRIDGE_HOST =
      "10.0.2.2"
    const val LOOPBACK_ADDRESS =
      "127.0.0.1"
    const val LOOPBACK_INTERFACE =
      "lo"
    const val DEFAULT_BRIDGE_PORT =
      27230
    const val DEFAULT_REMOTE_PORT =
      27300
    const val MIN_REMOTE_PORT =
      10_240
    const val MAX_REMOTE_PORT =
      60_000
    const val INVITATION_PREFIX =
      "I "
    const val CODE_MATCHED =
      "C OK"
    const val CONNECTED =
      "D"
    const val CONNECTED_MATCHED =
      "D OK"
    const val CLEANED =
      "X"
    const val CLEANED_MATCHED =
      "X OK"
    const val BRIDGE_CONNECT_TIMEOUT_MILLIS =
      15_000
    const val READY_TIMEOUT_MILLIS =
      120_000L
    const val CONNECTION_TIMEOUT_MILLIS =
      180_000L
    const val SWITCH_TIMEOUT_MILLIS =
      120_000L
    const val STOP_TIMEOUT_MILLIS =
      120_000L
    const val BRIDGE_TIMEOUT_MILLIS =
      360_000L
    const val POLL_MILLIS =
      50L
  }

  private fun controlledRoleOrSkip(
    arguments: android.os.Bundle,
  ): Role? {
    val roleArgument =
      arguments.getString(
        ROLE_ARGUMENT,
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
        "Missing $ROLE_ARGUMENT; " +
          "set $CONTROLLED_PRODUCER_SKIP_ARGUMENT=true to bypass this controlled harness",
      )
    }
    return Role.entries.firstOrNull {
      it.argument == roleArgument
    } ?: error(
      "Invalid $ROLE_ARGUMENT=$roleArgument",
    )
  }

  private fun bridgePortArgument(
    arguments: android.os.Bundle,
  ): Int {
    val rawPort =
      arguments.getString(
        BRIDGE_PORT_ARGUMENT,
      ) ?: return DEFAULT_BRIDGE_PORT
    return rawPort.toIntOrNull()
      ?: error(
        "Invalid $BRIDGE_PORT_ARGUMENT=$rawPort",
      )
  }

  private fun remotePortArgument(
    arguments: android.os.Bundle,
  ): Int {
    val rawPort =
      arguments.getString(
        REMOTE_PORT_ARGUMENT,
      ) ?: return DEFAULT_REMOTE_PORT
    val remotePort =
      rawPort.toIntOrNull()
        ?: error(
          "Invalid $REMOTE_PORT_ARGUMENT=$rawPort",
        )
    if (remotePort in MIN_REMOTE_PORT..MAX_REMOTE_PORT) {
      return remotePort
    }
    error(
      "Invalid $REMOTE_PORT_ARGUMENT=$rawPort",
    )
  }
}
