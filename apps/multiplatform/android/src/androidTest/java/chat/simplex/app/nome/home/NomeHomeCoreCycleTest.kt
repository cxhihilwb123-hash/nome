package chat.simplex.app.nome.home

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.ChatModel
import chat.simplex.common.views.database.stopChatAsync
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeHomeCoreCycleTest {
  @Test
  fun explicitRealCoreStopStartEvidenceGate() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val arguments = InstrumentationRegistry.getArguments()
    if (arguments.getString(CORE_CYCLE_ARGUMENT) != "true") return

    val evidenceDirectory = requireNotNull(
      instrumentation.targetContext.getExternalFilesDir(EVIDENCE_DIRECTORY),
    )
    assertTrue(evidenceDirectory.isDirectory || evidenceDirectory.mkdirs())
    val stoppedReady = File(evidenceDirectory, STOPPED_READY_FILE)
    val stopCaptured = File(evidenceDirectory, STOP_CAPTURED_FILE)
    val restartedReady = File(evidenceDirectory, RESTARTED_READY_FILE)
    val restartCaptured = File(evidenceDirectory, RESTART_CAPTURED_FILE)
    listOf(stoppedReady, stopCaptured, restartedReady, restartCaptured).forEach {
      assertTrue(!it.exists() || it.delete())
    }

    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      waitUntil {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true &&
          ChatModel.chats.value.isNotEmpty()
      }
      val user = ChatModel.currentUser.value
      assertNotNull(user)
      val cachedChatIds = ChatModel.chats.value.map { it.chatInfo.id }
      assertTrue(cachedChatIds.isNotEmpty())

      runBlocking { stopChatAsync(ChatModel) }
      waitUntil { ChatModel.chatRunning.value == false }
      instrumentation.waitForIdleSync()
      assertEquals(cachedChatIds, ChatModel.chats.value.map { it.chatInfo.id })
      Log.i(TAG, "core stopped; cachedRows=${cachedChatIds.size}; exact chat IDs retained")
      stoppedReady.writeText("stopped\n")
      waitUntil { stopCaptured.isFile }

      runBlocking { ChatModel.controller.startChat(requireNotNull(user)) }
      waitUntil { ChatModel.chatRunning.value == true }
      instrumentation.waitForIdleSync()
      assertEquals(cachedChatIds, ChatModel.chats.value.map { it.chatInfo.id })
      Log.i(TAG, "core restarted; cachedRows=${cachedChatIds.size}; exact chat IDs retained")
      restartedReady.writeText("restarted\n")
      waitUntil { restartCaptured.isFile }
    } finally {
      scenario.close()
    }
  }

  private fun waitUntil(predicate: () -> Boolean) {
    val deadline = SystemClock.elapsedRealtime() + EVIDENCE_TIMEOUT_MILLIS
    while (!predicate() && SystemClock.elapsedRealtime() < deadline) {
      SystemClock.sleep(POLL_MILLIS)
    }
    assertTrue("Timed out waiting for the real-core evidence state", predicate())
  }

  private companion object {
    const val CORE_CYCLE_ARGUMENT = "nomeCoreCycle"
    const val TAG = "NomeHomeCoreCycleTest"
    const val EVIDENCE_DIRECTORY = "nome-core-cycle"
    const val STOPPED_READY_FILE = "stopped.ready"
    const val STOP_CAPTURED_FILE = "stopped.captured"
    const val RESTARTED_READY_FILE = "restarted.ready"
    const val RESTART_CAPTURED_FILE = "restarted.captured"
    const val EVIDENCE_TIMEOUT_MILLIS = 120_000L
    const val POLL_MILLIS = 250L
  }
}
