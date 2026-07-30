package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.mutableStateOf
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
import chat.simplex.common.platform.AudioPlayer
import chat.simplex.common.platform.VideoPlayerHolder
import chat.simplex.common.platform.base64ToBitmap
import chat.simplex.common.platform.getAppFilePath
import java.io.DataOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTwoClientMediaPlaybackTest {
  @Test
  fun realControlledMediaIsReceivedPlayedAndCleanedUpByThePeer() {
    val arguments =
      InstrumentationRegistry.getArguments()
    val role =
      controlledProducerRoleOrSkip(
        arguments,
      ) ?: return
    val media =
      requireMediaKindArgument(arguments)
    val marker =
      requireMarkerArgument(arguments)
    val expectedSha256 =
      requireExpectedSha256Argument(
        arguments,
      )
    val port =
      bridgePortArgument(arguments)
    val instrumentation =
      InstrumentationRegistry.getInstrumentation()
    val scenario = launchControlledMainActivity()
    val sourceFixture =
      File(
        instrumentation.targetContext.cacheDir,
        media.fileName,
      )
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true &&
          newestReadyDirectChat() != null
      }
      when (role) {
        ProducerRole.Source -> {
          assertTrue(
            "The controlled media fixture must be provisioned in the source app cache",
            sourceFixture.isFile &&
              sourceFixture.length() in 1..MAX_FIXTURE_BYTES,
          )
          assertEquals(
            "The provisioned controlled media fixture must match the expected digest",
            expectedSha256,
            sourceFixture.sha256(),
          )
          sendControlledMedia(
            media = media,
            marker = marker,
            sourceFixture = sourceFixture,
          )
        }

        ProducerRole.Target -> {
          val received =
            waitForReceivedMedia(
              media = media,
              marker = marker,
            )
          val file =
            requireNotNull(
              received.item.file,
            )
          assertEquals(
            "The peer must observe the controlled media file name",
            media.fileName,
            file.fileName,
          )
          when (file.fileStatus) {
            is CIFileStatus.RcvInvitation,
            is CIFileStatus.RcvAborted,
            -> {
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
            }

            is CIFileStatus.RcvAccepted,
            is CIFileStatus.RcvTransfer,
            is CIFileStatus.RcvComplete,
            -> Unit
            else ->
              throw AssertionError(
                "The received controlled media must be downloadable or already complete; " +
                  "actual=${file.fileStatus::class.simpleName}",
              )
          }
          val completed =
            waitForCompletedMedia(
              direct = received.direct,
              media = media,
              marker = marker,
            )
          val receivedBytes =
            readReceivedBytes(
              completed,
            )
          assertEquals(
            "The received controlled media must match the expected digest",
            expectedSha256,
            receivedBytes.sha256(),
          )
          assertArrayEquals(
            "The received controlled media bytes must be stable across repeated reads",
            receivedBytes,
            readReceivedBytes(
              completed,
            ),
          )
          when (media) {
            MediaKind.Voice ->
              playControlledVoice(
                completed,
              )

            MediaKind.Video ->
              playControlledVideo(
                completed,
              )
          }
          cleanupReceivedFixture(
            completed,
          )
        }
      }
      coordinateWithPeer(
        port = port,
        role = role,
      )
      Log.i(
        TAG,
        "role=${role.argument} " +
          "media=${media.argument} " +
          "peerReceivedPlayedControlledMedia=true",
      )
    } finally {
      AudioPlayer.stop()
      VideoPlayerHolder.releaseAll()
      if (
        role == ProducerRole.Source &&
        sourceFixture.exists()
      ) {
        assertTrue(
          "The source controlled media fixture must be removable",
          sourceFixture.delete(),
        )
      }
      scenario?.close()
    }
  }

  private fun sendControlledMedia(
    media: MediaKind,
    marker: String,
    sourceFixture: File,
  ) {
    val direct =
      requireNotNull(
        newestReadyDirectChat(),
      )
    val info =
      direct.chatInfo as
        ChatInfo.Direct
    val existing =
      matchingMediaItem(
        direct = direct,
        media = media,
        marker = marker,
      )
    if (existing != null) {
      assertTrue(
        "The reused controlled media item must be sender-owned",
        existing.chatDir.sent &&
          existing.file?.fileName ==
          media.fileName,
      )
      Log.i(
        TAG,
        "media=${media.argument} officialMediaItemReused=true",
      )
      return
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
                    sourceFixture.absolutePath,
                  ),
                quotedItemId = null,
                msgContent =
                  when (media) {
                    MediaKind.Voice ->
                      MsgContent.MCVoice(
                        marker,
                        duration = FIXTURE_DURATION_SECONDS,
                      )

                    MediaKind.Video ->
                      MsgContent.MCVideo(
                        marker,
                        image = PNG_PREVIEW_BASE64,
                        duration = FIXTURE_DURATION_SECONDS,
                      )
                  },
                mentions = emptyMap(),
              ),
            ),
        )
      }
    val sent =
      result
        ?.singleOrNull()
        ?.chatItem
    assertTrue(
      "The official media producer must create the expected sender-side item",
      sent?.file?.fileName ==
        media.fileName &&
        media.matches(
          sent?.content?.msgContent,
        ),
    )
    Log.i(
      TAG,
      "media=${media.argument} officialMediaItemCreated=true",
    )
  }

  private fun waitForReceivedMedia(
    media: MediaKind,
    marker: String,
  ): ReceivedMediaItem {
    var received: ReceivedMediaItem? =
      null
    waitUntil(FILE_TIMEOUT_MILLIS) {
      received =
        readyDirectChats()
          .asSequence()
          .mapNotNull { direct ->
            matchingMediaItem(
              direct = direct,
              media = media,
              marker = marker,
            )?.let { item ->
              ReceivedMediaItem(
                direct = direct,
                item = item,
              )
            }
          }.firstOrNull()
      received
        ?.item
        ?.file != null
    }
    return requireNotNull(
      received,
    )
  }

  private fun waitForCompletedMedia(
    direct: Chat,
    media: MediaKind,
    marker: String,
  ): CIFile {
    var completed: CIFile? =
      null
    waitUntil(FILE_TIMEOUT_MILLIS) {
      completed =
        matchingMediaItem(
          direct = direct,
          media = media,
          marker = marker,
        )?.file
      completed?.fileStatus is
        CIFileStatus.RcvComplete
    }
    return requireNotNull(
      completed,
    )
  }

  private fun playControlledVoice(
    file: CIFile,
  ) {
    val source =
      requireNotNull(
        file.fileSource,
      )
    val playing =
      mutableStateOf(false)
    val progress =
      mutableStateOf(0)
    val duration =
      mutableStateOf(
        FIXTURE_DURATION_MILLIS,
      )
    AudioPlayer.play(
      fileSource = source,
      audioPlaying = playing,
      progress = progress,
      duration = duration,
      resetOnEnd = false,
      smallView = false,
    )
    waitUntil(PLAYBACK_TIMEOUT_MILLIS) {
      duration.value >=
        MINIMUM_PLAYBACK_DURATION_MILLIS &&
        (
          (
            playing.value &&
              progress.value > 0
          ) ||
            (
              !playing.value &&
                progress.value >=
                MINIMUM_PLAYBACK_DURATION_MILLIS
            )
        )
    }
    AudioPlayer.stop()
    waitUntil(PLAYBACK_STOP_TIMEOUT_MILLIS) {
      !playing.value &&
        AudioPlayer.currentlyPlaying.value ==
        null
    }
  }

  private fun playControlledVideo(
    file: CIFile,
  ) {
    val source =
      requireNotNull(
        file.fileSource,
      )
    val uri =
      if (source.cryptoArgs == null) {
        File(
          getAppFilePath(
            source.filePath,
          ),
        ).toURI()
      } else {
        requireNotNull(
          source.decryptedGetOrCreate(),
        )
      }
    val player =
      runBlocking {
        withContext(
          Dispatchers.Main,
        ) {
          VideoPlayerHolder.getOrCreate(
            uri = uri,
            gallery = false,
            defaultPreview =
              base64ToBitmap(
                PNG_PREVIEW_BASE64,
              ),
            defaultDuration =
              FIXTURE_DURATION_MILLIS
                .toLong(),
            soundEnabled = true,
          ).also {
            it.play(
              resetOnEnd = false,
            )
          }
        }
      }
    waitUntil(PLAYBACK_TIMEOUT_MILLIS) {
      !player.brokenVideo.value &&
        player.videoPlaying.value &&
        player.progress.value > 0
    }
    runBlocking {
      withContext(
        Dispatchers.Main,
      ) {
        player.stop()
        player.release(
          remove = true,
        )
      }
    }
    waitUntil(PLAYBACK_STOP_TIMEOUT_MILLIS) {
      !player.videoPlaying.value
    }
  }

  private fun cleanupReceivedFixture(
    file: CIFile,
  ) {
    val source =
      requireNotNull(
        file.fileSource,
      )
    val downloaded =
      File(
        getAppFilePath(
          source.filePath,
        ),
      )
    source.deleteTmpFile()
    assertTrue(
      "The received controlled media fixture must exist before cleanup",
      downloaded.isFile,
    )
    assertTrue(
      "The received controlled media fixture must be removable",
      downloaded.delete(),
    )
    assertFalse(
      "The received controlled media fixture must leave no local file residue",
      downloaded.exists(),
    )
  }

  private fun readReceivedBytes(
    file: CIFile,
  ): ByteArray {
    val source =
      requireNotNull(
        file.fileSource,
      )
    val path =
      getAppFilePath(
        source.filePath,
      )
    val cryptoArgs =
      source.cryptoArgs
    return if (
      cryptoArgs == null
    ) {
      File(
        path,
      ).readBytes()
    } else {
      readCryptoFile(
        path,
        cryptoArgs,
      )
    }
  }

  private fun matchingMediaItem(
    direct: Chat,
    media: MediaKind,
    marker: String,
  ): ChatItem? {
    val info =
      direct.chatInfo as
        ChatInfo.Direct
    return runBlocking {
      ChatModel.controller.apiGetChat(
        rh = direct.remoteHostId,
        type = info.chatType,
        id = info.apiId,
        scope = null,
        pagination =
          ChatPagination.Last(
            CHAT_ITEM_LIMIT,
          ),
      )?.first?.chatItems?.firstOrNull {
        it.text == marker &&
          it.file?.fileName ==
          media.fileName &&
          media.matches(
            it.content.msgContent,
          )
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
    bridgeSocket(
      port,
    ).use { socket ->
      val output =
        DataOutputStream(
          socket.getOutputStream(),
        )
      output.writeByte(
        role.wireValue,
      )
      output.flush()
      assertTrue(
        "Both controlled clients must finish the same media-playback attempt",
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
        FILE_TIMEOUT_MILLIS.toInt()
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
      SystemClock.elapsedRealtime() <
      deadline
    ) {
      SystemClock.sleep(
        POLL_MILLIS,
      )
    }
    assertTrue(
      "Timed out waiting for the controlled two-client media state",
      predicate(),
    )
  }

  private fun File.sha256(): String =
    readBytes().sha256()

  private fun ByteArray.sha256(): String =
    MessageDigest
      .getInstance(
        "SHA-256",
      ).digest(this)
      .joinToString(
        "",
      ) {
        "%02x".format(it)
      }

  private data class ReceivedMediaItem(
    val direct: Chat,
    val item: ChatItem,
  )

  private enum class ProducerRole(
    val argument: String,
    val wireValue: Int,
  ) {
    Source(
      "source",
      1,
    ),
    Target(
      "target",
      2,
    ),
  }

  private enum class MediaKind(
    val argument: String,
    val fileName: String,
  ) {
    Voice(
      "voice",
      "nome-controlled-voice.m4a",
    ),
    Video(
      "video",
      "nome-controlled-video.mp4",
    ),
    ;

    fun matches(
      content: MsgContent?,
    ): Boolean =
      when (this) {
        Voice ->
          content is
            MsgContent.MCVoice

        Video ->
          content is
            MsgContent.MCVideo
      }
  }

  private companion object {
    const val TAG =
      "NomeTwoClientMedia"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val PRODUCER_ROLE_ARGUMENT =
      "nomeProducerRole"
    const val PRODUCER_PORT_ARGUMENT =
      "nomeProducerPort"
    const val MEDIA_KIND_ARGUMENT =
      "nomeMediaKind"
    const val MESSAGE_MARKER_ARGUMENT =
      "nomeMediaMarker"
    const val EXPECTED_SHA256_ARGUMENT =
      "nomeMediaSha256"
    const val MESSAGE_MARKER_PREFIX =
      "NomePeerMedia-"
    const val BRIDGE_HOST =
      "10.0.2.2"
    const val DEFAULT_BRIDGE_PORT =
      27220
    const val BRIDGE_ACK = 1
    const val SHA256_LENGTH = 64
    const val MAX_MARKER_LENGTH = 96
    const val MAX_FIXTURE_BYTES =
      128_000L
    const val CHAT_ITEM_LIMIT = 100
    const val FIXTURE_DURATION_SECONDS = 1
    const val FIXTURE_DURATION_MILLIS = 1_000
    const val MINIMUM_PLAYBACK_DURATION_MILLIS =
      750
    const val BRIDGE_CONNECT_TIMEOUT_MILLIS =
      15_000
    const val READY_TIMEOUT_MILLIS =
      120_000L
    const val FILE_TIMEOUT_MILLIS =
      300_000L
    const val PLAYBACK_TIMEOUT_MILLIS =
      30_000L
    const val PLAYBACK_STOP_TIMEOUT_MILLIS =
      10_000L
    const val POLL_MILLIS = 50L
    const val PNG_PREVIEW_BASE64 =
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwC" +
        "AAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
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

  private fun requireMediaKindArgument(
    arguments: android.os.Bundle,
  ): MediaKind {
    val mediaArgument =
      arguments.getString(
        MEDIA_KIND_ARGUMENT,
      ) ?: error(
        "Missing $MEDIA_KIND_ARGUMENT",
      )
    return MediaKind.entries.firstOrNull {
      it.argument == mediaArgument
    } ?: error(
      "Invalid $MEDIA_KIND_ARGUMENT=$mediaArgument",
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

  private fun requireExpectedSha256Argument(
    arguments: android.os.Bundle,
  ): String {
    val expectedSha256 =
      arguments.getString(
        EXPECTED_SHA256_ARGUMENT,
      )?.lowercase() ?: error(
        "Missing $EXPECTED_SHA256_ARGUMENT",
      )
    if (
      expectedSha256.length == SHA256_LENGTH &&
      expectedSha256.all { character ->
        character in '0'..'9' ||
          character in 'a'..'f'
      }
    ) {
      return expectedSha256
    }
    error(
      "Invalid $EXPECTED_SHA256_ARGUMENT=$expectedSha256",
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
}
