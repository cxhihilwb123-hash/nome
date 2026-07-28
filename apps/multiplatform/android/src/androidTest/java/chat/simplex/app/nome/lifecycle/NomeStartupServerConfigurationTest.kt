package chat.simplex.app.nome.lifecycle

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.app.MainActivity
import chat.simplex.common.model.ChatModel
import chat.simplex.common.model.OperatorTag
import chat.simplex.common.model.ServerRoles
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeStartupServerConfigurationTest {
  @Test
  fun createdUserHasNomeServersBeforeNormalAppUse() {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      waitUntil {
        ChatModel.currentUser.value != null && ChatModel.chatRunning.value == true
      }
      val user = assertNotNull(ChatModel.currentUser.value).let { ChatModel.currentUser.value!! }
      val userServers = runBlocking {
        ChatModel.controller.getUserServers(user.remoteHostId)
      }
      assertNotNull("The active user must expose configured servers", userServers)

      val nome = userServers!!.singleOrNull { it.operator?.operatorTag == OperatorTag.Nome }
      val custom = userServers.singleOrNull { it.operator == null }
      val official = requireNotNull(nome ?: custom) {
        "Nome official servers must be available from the core or compatibility group"
      }
      assertTrue(
        "The active message route must use the Nome domain",
        official.smpServers.any { it.enabled && !it.deleted && hostname(it.server) == SMP_HOSTNAME },
      )
      assertTrue(
        "The active file route must use the Nome domain",
        official.xftpServers.any { it.enabled && !it.deleted && hostname(it.server) == XFTP_HOSTNAME },
      )

      if (nome != null) {
        assertTrue("The rebuilt core Nome operator must remain enabled", nome.operator?.enabled == true)
        assertTrue("The Nome operator must own SMP storage and proxy roles", nome.operator?.smpRoles == ServerRoles(true, true))
        assertTrue("The Nome operator must own XFTP storage and proxy roles", nome.operator?.xftpRoles == ServerRoles(true, true))
      }

      val managedOperators = userServers.filter {
        it.operator != null && it.operator?.operatorTag != OperatorTag.Nome
      }
      assertTrue(
        "Managed preset operators must not remain active beside Nome routes",
        managedOperators.all { group ->
          group.operator?.enabled == false &&
            group.operator?.smpRoles == ServerRoles(false, false) &&
            group.operator?.xftpRoles == ServerRoles(false, false) &&
            group.smpServers.none { it.enabled } &&
            group.xftpServers.none { it.enabled }
        },
      )
    } finally {
      scenario.close()
    }
  }

  private fun waitUntil(predicate: () -> Boolean) {
    val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
    while (!predicate() && SystemClock.elapsedRealtime() < deadline) {
      SystemClock.sleep(POLL_MILLIS)
    }
    assertTrue("Timed out waiting for configured running chat", predicate())
  }

  private fun hostname(address: String): String =
    address.trim().substringAfterLast('@').substringBefore(',').substringBefore(':').lowercase()

  private companion object {
    const val SMP_HOSTNAME = "124.223.71.168"
    const val XFTP_HOSTNAME = "124.223.71.168"
    const val TIMEOUT_MILLIS = 30_000L
    const val POLL_MILLIS = 250L
  }
}
