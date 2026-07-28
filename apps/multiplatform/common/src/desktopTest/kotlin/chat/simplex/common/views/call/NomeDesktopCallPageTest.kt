package chat.simplex.common.views.call

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class NomeDesktopCallPageTest {
  @Test
  fun browserPageUsesNomeTitle() {
    val stream = javaClass.getResourceAsStream("/assets/www/desktop/call.html")
    assertNotNull(stream, "desktop call page must be packaged")
    val html = stream.bufferedReader().use { it.readText() }

    assertContains(html, "<title>Nome call</title>")
    assertContains(html, "/*__NOME_CALL_BRIDGE_BOOTSTRAP__*/")
    assertFalse(html.contains("SimpleX Chat WebRTC call"))
  }

  @Test
  fun desktopBridgeUsesMemoryOnlyWebSocketPathRatherThanCookies() {
    val stream = javaClass.getResourceAsStream("/assets/www/desktop/ui.js")
    assertNotNull(stream, "desktop call bridge must be packaged")
    val script = stream.bufferedReader().use { it.readText() }

    assertContains(script, "window.nomeCallBridgePath")
    assertContains(script, "ws://${'$'}{location.host}${'$'}{callBridgePath}")
    assertFalse(script.contains("document.cookie"))
    assertFalse(script.contains("simplex_call_auth"))
  }

  @Test
  fun sharedCallBridgeFallbackNeverPrintsPayloads() {
    val stream = javaClass.getResourceAsStream("/assets/www/call.js")
    assertNotNull(stream, "shared call bridge script must be packaged")
    val script = stream.bufferedReader().use { it.readText() }

    assertContains(script, "var sendMessageToNative = (_msg) => { };")
    assertFalse(script.contains("console.log(JSON.stringify(msg))"))
  }
}
