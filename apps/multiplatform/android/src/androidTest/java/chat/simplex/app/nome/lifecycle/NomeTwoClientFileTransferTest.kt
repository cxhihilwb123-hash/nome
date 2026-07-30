package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.CIFile
import chat.simplex.common.model.CIFileStatus
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatItem
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.ChatPagination
import chat.simplex.common.model.ComposedMessage
import chat.simplex.common.model.CryptoFile
import chat.simplex.common.model.MsgContent
import chat.simplex.common.model.readCryptoFile
import chat.simplex.common.platform.getAppFilePath
import chat.simplex.common.platform.openFile
import java.io.DataOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTwoClientFileTransferTest {
  @Test
  fun realControlledFileIsReceivedOpenedAndCleanedUpByThePeer() {
    val arguments = InstrumentationRegistry.getArguments()
    val role =
      controlledProducerRoleOrSkip(arguments)
        ?: return
    val marker =
      requireMarkerArgument(arguments)
    val fileKind =
      requireFileKindArgument(arguments)
    val fileName =
      requireFileNameArgument(
        arguments = arguments,
        fileKind = fileKind,
      )
    val port =
      bridgePortArgument(arguments)
    val instrumentation =
      InstrumentationRegistry.getInstrumentation()
    val payload =
      controlledPayload(fileKind)
    val scenario = launchControlledMainActivity()
    var senderFixture: File? = null
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
          senderFixture =
            File(
              instrumentation.targetContext.cacheDir,
              fileName,
            ).also {
              it.writeBytes(payload)
            }
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
                      fileSource =
                        CryptoFile.plain(
                          requireNotNull(senderFixture).absolutePath,
                        ),
                      quotedItemId = null,
                      msgContent = MsgContent.MCFile(marker),
                      mentions = emptyMap(),
                    ),
                  ),
              )
            }
          assertTrue(
            "The official file producer must create a sender-side item",
            !result.isNullOrEmpty() &&
              result.first().chatItem.file != null,
          )
          Log.i(
            TAG,
            "role=${role.argument} officialFileItemCreated=true",
          )
        }

        ProducerRole.Target -> {
          val received =
            waitForReceivedFileItem(marker)
          val file =
            requireNotNull(received.item.file)
          assertEquals(
            "The peer must observe the controlled file name",
            fileName,
            file.fileName,
          )
          assertEquals(
            "The peer must observe the controlled file size",
            payload.size.toLong(),
            file.fileSize,
          )
          runBlocking {
            ChatModel.controller.receiveFile(
              rhId = received.direct.remoteHostId,
              user =
                requireNotNull(
                  ChatModel.currentUser.value,
                ),
              fileId = file.fileId,
              userApprovedRelays = true,
            )
          }
          val completed =
            waitForCompletedFile(
              direct = received.direct,
              marker = marker,
            )
          assertArrayEquals(
            "The downloaded controlled file bytes must match the sender",
            payload,
            readReceivedBytes(completed),
          )
          verifyOpenAndTemporaryCleanup(
            instrumentation = instrumentation,
            file = completed,
            mimeType = fileKind.mimeType,
          )
          val downloaded =
            File(
              getAppFilePath(
                requireNotNull(
                  completed.fileSource,
                ).filePath,
              ),
            )
          assertTrue(
            "The controlled downloaded fixture must exist before cleanup",
            downloaded.isFile,
          )
          assertTrue(
            "The controlled downloaded fixture must be removable",
            downloaded.delete(),
          )
          assertTrue(
            "The controlled downloaded fixture must leave no local file residue",
            !downloaded.exists(),
          )
        }
      }

      coordinateWithPeer(
        port = port,
        role = role,
      )
      Log.i(
        TAG,
        "role=${role.argument} peerReceivedOpenedControlledFile=true",
      )
    } finally {
      senderFixture?.let {
        if (it.exists()) {
          assertTrue(
            "The sender-side controlled fixture must be removable",
            it.delete(),
          )
        }
      }
      scenario?.close()
    }
  }

  private fun waitForReceivedFileItem(
    marker: String,
  ): ReceivedFileItem {
    var received: ReceivedFileItem? = null
    waitUntil(FILE_TIMEOUT_MILLIS) {
      received =
        readyDirectChats()
          .asSequence()
          .mapNotNull { direct ->
            matchingFileItem(
              direct = direct,
              marker = marker,
            )?.let { item ->
              ReceivedFileItem(
                direct = direct,
                item = item,
              )
            }
          }.firstOrNull()
      received?.item?.file?.fileStatus is
        CIFileStatus.RcvInvitation
    }
    return requireNotNull(received)
  }

  private fun waitForCompletedFile(
    direct: Chat,
    marker: String,
  ): CIFile {
    var completed: CIFile? = null
    waitUntil(FILE_TIMEOUT_MILLIS) {
      completed =
        matchingFileItem(
          direct = direct,
          marker = marker,
        )?.file
      completed?.fileStatus is
        CIFileStatus.RcvComplete
    }
    return requireNotNull(completed)
  }

  private fun readReceivedBytes(
    file: CIFile,
  ): ByteArray {
    val source =
      requireNotNull(
        file.fileSource,
      )
    val path =
      getAppFilePath(source.filePath)
    val cryptoArgs =
      source.cryptoArgs
    return if (cryptoArgs == null) {
      File(path).readBytes()
    } else {
      readCryptoFile(
        path,
        cryptoArgs,
      )
    }
  }

  private fun verifyOpenAndTemporaryCleanup(
    instrumentation: android.app.Instrumentation,
    file: CIFile,
    mimeType: String,
  ) {
    val source =
      requireNotNull(
        file.fileSource,
      )
    val before =
      ChatModel.filesToDelete
        .map { it.absolutePath }
        .toSet()
    assertTrue(
      "The controlled file type must resolve to an external viewer",
      instrumentation.targetContext
        .packageManager
        .queryIntentActivities(
          android.content.Intent(
            android.content.Intent.ACTION_VIEW,
          ).apply {
            type = mimeType
          },
          0,
        ).isNotEmpty(),
    )
    openFile(source)
    SystemClock.sleep(OPEN_ACTIVITY_SETTLE_MILLIS)
    instrumentation.uiAutomation
      .executeShellCommand(
        "input keyevent KEYCODE_BACK",
      ).close()
    SystemClock.sleep(OPEN_ACTIVITY_SETTLE_MILLIS)
    val newTemporaryFiles =
      ChatModel.filesToDelete.filter {
        it.absolutePath !in before
      }
    newTemporaryFiles.forEach {
      assertTrue(
        "The controlled open-file temporary copy must be removable",
        !it.exists() ||
          it.delete(),
      )
    }
    ChatModel.filesToDelete.removeAll(
      newTemporaryFiles.toSet(),
    )
    assertTrue(
      "The controlled open-file temporary copy must leave no residue",
      newTemporaryFiles.none { it.exists() },
    )
  }

  private fun matchingFileItem(
    direct: Chat,
    marker: String,
  ): ChatItem? {
    val info =
      direct.chatInfo as ChatInfo.Direct
    return runBlocking {
      ChatModel.controller.apiGetChat(
        rh = direct.remoteHostId,
        type = info.chatType,
        id = info.apiId,
        scope = null,
        pagination = ChatPagination.Last(CHAT_ITEM_LIMIT),
      )?.first?.chatItems?.firstOrNull {
        it.text == marker &&
          it.file != null
      }
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
        "Both controlled clients must finish the same file-transfer attempt",
        socket.getInputStream().read() ==
          BRIDGE_ACK,
      )
    }
  }

  private fun bridgeSocket(
    port: Int,
  ): Socket =
    Socket().apply {
      soTimeout = FILE_TIMEOUT_MILLIS.toInt()
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
      "Timed out waiting for the controlled two-client file state",
      predicate(),
    )
  }

  private fun controlledPayload(
    fileKind: ControlledFileKind,
  ): ByteArray =
    Base64.decode(
      when (fileKind) {
        ControlledFileKind.Image -> PNG_FIXTURE_BASE64
        ControlledFileKind.Pdf -> PDF_FIXTURE_BASE64
      },
      Base64.DEFAULT,
    )

  private data class ReceivedFileItem(
    val direct: Chat,
    val item: ChatItem,
  )

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

  private fun requireFileKindArgument(
    arguments: android.os.Bundle,
  ): ControlledFileKind {
    val rawKind =
      arguments.getString(
        FILE_KIND_ARGUMENT,
      ) ?: ControlledFileKind.Image.argument
    return ControlledFileKind.entries
      .firstOrNull {
        it.argument == rawKind
      }
      ?: error(
        "Invalid $FILE_KIND_ARGUMENT=$rawKind",
      )
  }

  private fun requireFileNameArgument(
    arguments: android.os.Bundle,
    fileKind: ControlledFileKind,
  ): String {
    val fileName =
      arguments.getString(
        FILE_NAME_ARGUMENT,
      ) ?: error(
        "Missing $FILE_NAME_ARGUMENT",
      )
    if (
      fileName.startsWith(FILE_NAME_PREFIX) &&
      fileName.endsWith(fileKind.fileNameSuffix) &&
      '/' !in fileName &&
      '\\' !in fileName &&
      fileName.length <= MAX_FILE_NAME_LENGTH
    ) {
      return fileName
    }
    error(
      "Invalid $FILE_NAME_ARGUMENT=$fileName",
    )
  }

  private enum class ControlledFileKind(
    val argument: String,
    val fileNameSuffix: String,
    val mimeType: String,
  ) {
    Image("image", ".png", "image/png"),
    Pdf("pdf", ".pdf", "application/pdf"),
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
    const val TAG = "NomeTwoClientFile"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val PRODUCER_ROLE_ARGUMENT =
      "nomeProducerRole"
    const val PRODUCER_PORT_ARGUMENT =
      "nomeProducerPort"
    const val MESSAGE_MARKER_ARGUMENT =
      "nomeFileMarker"
    const val FILE_NAME_ARGUMENT =
      "nomeFileName"
    const val FILE_KIND_ARGUMENT =
      "nomeFileKind"
    const val MESSAGE_MARKER_PREFIX =
      "NomePeerFile-"
    const val FILE_NAME_PREFIX =
      "nome-controlled-"
    const val BRIDGE_HOST = "10.0.2.2"
    const val DEFAULT_BRIDGE_PORT = 27210
    const val BRIDGE_ACK = 1
    const val MAX_MARKER_LENGTH = 96
    const val MAX_FILE_NAME_LENGTH = 96
    const val CHAT_ITEM_LIMIT = 100
    const val BRIDGE_CONNECT_TIMEOUT_MILLIS = 15_000
    const val READY_TIMEOUT_MILLIS = 120_000L
    const val FILE_TIMEOUT_MILLIS = 300_000L
    const val OPEN_ACTIVITY_SETTLE_MILLIS = 2_000L
    const val POLL_MILLIS = 1_000L
    const val PNG_FIXTURE_BASE64 =
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwC" +
        "AAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    const val PDF_FIXTURE_BASE64 =
      "JVBERi0xLjQKJeLjz9MKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMg" +
        "MiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFsz" +
        "IDAgUl0gL0NvdW50IDEgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2Ug" +
        "L1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAgMCA2MTIgNzkyXSAvUmVzb3VyY2Vz" +
        "IDw8IC9Gb250IDw8IC9GMSA1IDAgUiA+PiA+PiAvQ29udGVudHMgNCAwIFIgPj4K" +
        "ZW5kb2JqCjQgMCBvYmoKPDwgL0xlbmd0aCA0OCA+PgpzdHJlYW0KQlQgL0YxIDE4" +
        "IFRmIDcyIDcyMCBUZCAoTm9tZSBjb2RlMzczIFBERikgVGogRVQKZW5kc3RyZWFt" +
        "CmVuZG9iago1IDAgb2JqCjw8IC9UeXBlIC9Gb250IC9TdWJ0eXBlIC9UeXBlMSAv" +
        "QmFzZUZvbnQgL0hlbHZldGljYSA+PgplbmRvYmoKeHJlZgowIDYKMDAwMDAwMDAw" +
        "MCA2NTUzNSBmIAowMDAwMDAwMDE1IDAwMDAwIG4gCjAwMDAwMDAwNjQgMDAwMDAg" +
        "biAKMDAwMDAwMDEyMSAwMDAwMCBuIAowMDAwMDAwMjQ3IDAwMDAwIG4gCjAwMDAw" +
        "MDAzNDQgMDAwMDAgbiAKdHJhaWxlcgo8PCAvU2l6ZSA2IC9Sb290IDEgMCBSID4+" +
        "CnN0YXJ0eHJlZgo0MTQKJSVFT0YK"
  }
}
