package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.ArchiveConfig
import chat.simplex.common.model.ChatModel
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeControlledArchiveTransferTest {
  @Test
  fun officialArchiveTransfersOnlyTheControlledFixtureDatabase() {
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
    val archiveNameArgument =
      arguments.getString(ARCHIVE_NAME_ARGUMENT)
    val archiveName =
      archiveNameArgument
        ?.takeIf {
          it.startsWith(ARCHIVE_NAME_PREFIX) &&
            it.endsWith(ARCHIVE_NAME_SUFFIX) &&
            '/' !in it &&
            '\\' !in it &&
            it.length <= MAX_ARCHIVE_NAME_LENGTH
        }
        ?: error(
          "Missing or invalid $ARCHIVE_NAME_ARGUMENT: $archiveNameArgument",
        )
    val instrumentation =
      InstrumentationRegistry.getInstrumentation()
    val scenario =
      ActivityScenario.launch<MainActivity>(
        Intent().setClassName(
          instrumentation.targetContext.packageName,
          MainActivity::class.java.name,
        ),
      )
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        when (role) {
          TransferRole.Export ->
            ChatModel.chatRunning.value == true &&
              ChatModel.currentUser.value != null

          TransferRole.Import ->
            !ChatModel.ctrlInitInProgress.value &&
              ChatModel.chatDbStatus.value != null
        }
      }
      val archive =
        File(
          instrumentation.targetContext.filesDir,
          archiveName,
        )
      when (role) {
        TransferRole.Export -> {
          if (archive.exists()) {
            assertTrue(
              "The previous controlled archive must be replaceable",
              archive.delete(),
            )
          }
          stopChat()
          val errors =
            runBlocking {
              ChatModel.controller.apiExportArchive(
                ArchiveConfig(
                  archivePath = archive.absolutePath,
                  parentTempDirectory =
                    instrumentation.targetContext.cacheDir.absolutePath,
                ),
              )
            }
          assertTrue(
            "The official controlled archive export must have no archive errors",
            errors.isEmpty(),
          )
          assertTrue(
            "The official controlled archive export must be non-empty",
            archive.isFile &&
              archive.length() > 0L,
          )
        }

        TransferRole.Import -> {
          assertTrue(
            "The controlled archive must be transferred before import",
            archive.isFile &&
              archive.length() > 0L,
          )
          if (ChatModel.chatRunning.value == true) {
            stopChat()
          }
          runBlocking {
            ChatModel.controller.apiDeleteStorage()
          }
          val errors =
            runBlocking {
              ChatModel.controller.apiImportArchive(
                ArchiveConfig(
                  archivePath = archive.absolutePath,
                  parentTempDirectory =
                    instrumentation.targetContext.cacheDir.absolutePath,
                ),
              )
            }
          assertTrue(
            "The official controlled archive import must have no archive errors",
            errors.isEmpty(),
          )
          assertTrue(
            "The consumed controlled archive must be removed",
            archive.delete(),
          )
        }
      }
      Log.i(
        TAG,
        "role=${role.argument} officialControlledArchiveTransfer=true",
      )
    } finally {
      scenario.close()
    }
  }

  private fun stopChat() {
    runBlocking {
      assertTrue(
        "The controlled chat must stop before archive handoff",
        ChatModel.controller.apiStopChat(),
      )
    }
    ChatModel.chatRunning.value = false
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
      "Timed out waiting for the controlled archive fixture",
      predicate(),
    )
  }

  private enum class TransferRole(
    val argument: String,
  ) {
    Export("export"),
    Import("import"),
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
    const val TAG = "NomeArchiveTransfer"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
    const val TRANSFER_ROLE_ARGUMENT =
      "nomeArchiveRole"
    const val ARCHIVE_NAME_ARGUMENT =
      "nomeArchiveName"
    const val ARCHIVE_NAME_PREFIX =
      "nome-controlled-"
    const val ARCHIVE_NAME_SUFFIX =
      ".zip"
    const val MAX_ARCHIVE_NAME_LENGTH = 96
    const val READY_TIMEOUT_MILLIS = 120_000L
    const val POLL_MILLIS = 250L
  }
}
