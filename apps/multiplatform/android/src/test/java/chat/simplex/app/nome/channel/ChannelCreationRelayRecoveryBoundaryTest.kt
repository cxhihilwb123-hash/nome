package chat.simplex.app.nome.channel

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelCreationRelayRecoveryBoundaryTest {
  @Test
  fun channelCreationReconcilesRelayStateWhenAnEventArrivesBeforeUiInitialization() {
    val addChannelView = commonSource(
      "commonMain/kotlin/chat/simplex/common/views/newchat/AddChannelView.kt",
    ).readText()
    val progressStep = addChannelView.progressStepView()

    assertTrue(progressStep.contains("snapshotFlow { ChannelRelaysModel.groupRelays.toList() }"))
    assertTrue(progressStep.contains("while (currentCoroutineContext().isActive"))
    assertTrue(progressStep.contains("apiGetGroupRelays(gInfo.groupId)"))
    assertTrue(progressStep.contains("relays.isNotEmpty()"))
    assertTrue(progressStep.contains("relays != ChannelRelaysModel.groupRelays.toList()"))
    assertTrue(progressStep.contains("ChannelRelaysModel.set(gInfo.groupId, relays)"))
    assertTrue(progressStep.contains("delay(CHANNEL_RELAY_REFRESH_MILLIS)"))
  }

  private fun commonSource(relativePath: String): File {
    var current = File(
      requireNotNull(System.getProperty("user.dir")),
    ).absoluteFile
    repeat(8) {
      val candidate = File(current, "../common/src/$relativePath").canonicalFile
      if (candidate.isFile) return candidate
      current = current.parentFile
        ?: error("Unable to locate common source root")
    }
    error("Unable to locate $relativePath")
  }

  private fun String.progressStepView(): String {
    val start = indexOf("private fun ProgressStepView(")
    check(start >= 0) { "Missing channel creation progress view" }
    val end = indexOf("\nprivate fun relayMemberConnFailed", start)
    check(end > start) { "Missing channel creation progress view end" }
    return substring(start, end)
  }
}
