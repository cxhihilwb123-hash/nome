package chat.simplex.common.views.call

import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals

class CallBridgeBootstrapHttpTest {
  @Test
  fun onlyOneTimeBootstrapInjectsMemoryOnlyWebSocketPathAndNeverIssuesCookie() {
    val bootstrapNonce = "known-test-bootstrap-nonce"
    val server = startServer(
      onResponse = {},
      port = 0,
      bootstrapNonce = bootstrapNonce,
    )

    try {
      val cleanPage = request(server.listeningPort, "/simplex/call/")
      assertEquals(200, cleanPage.status)
      assertNull(cleanPage.setCookie)
      assertFalse(cleanPage.body.contains("window.nomeCallBridgePath='"))
      assertSecurityHeaders(cleanPage)

      val wrongNonce = request(server.listeningPort, "/simplex/call/?bootstrap=wrong")
      assertEquals(403, wrongNonce.status)
      assertNull(wrongNonce.setCookie)
      assertSecurityHeaders(wrongNonce)

      val bootstrap = request(
        server.listeningPort,
        "/simplex/call/?bootstrap=$bootstrapNonce",
      )
      assertEquals(200, bootstrap.status)
      assertNull(bootstrap.location)
      assertNull(bootstrap.setCookie)
      val match = assertNotNull(Regex("window\\.nomeCallBridgePath='([^']+)'" ).find(bootstrap.body))
      val webSocketPath = match.groupValues[1]
      assertContains(webSocketPath, "/simplex/call/ws/")
      assertNotEquals("/simplex/call/ws/$bootstrapNonce", webSocketPath)
      assertFalse(bootstrap.body.contains("simplex_call_auth"))
      assertSecurityHeaders(bootstrap)

      val replay = request(
        server.listeningPort,
        "/simplex/call/?bootstrap=$bootstrapNonce",
      )
      assertEquals(410, replay.status)
      assertNull(replay.setCookie)
      assertSecurityHeaders(replay)

      val redirectedPage = request(server.listeningPort, "/simplex/call/")
      assertEquals(200, redirectedPage.status)
      assertNull(redirectedPage.setCookie)
      assertFalse(redirectedPage.body.contains("window.nomeCallBridgePath='"))
      assertSecurityHeaders(redirectedPage)
    } finally {
      server.stop()
    }
  }

  private fun request(port: Int, path: String): ResponseSnapshot {
    val connection = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false
    connection.connectTimeout = 5_000
    connection.readTimeout = 5_000
    return try {
      ResponseSnapshot(
        status = connection.responseCode,
        setCookie = connection.getHeaderField("Set-Cookie"),
        location = connection.getHeaderField("Location"),
        cacheControl = connection.getHeaderField("Cache-Control"),
        contentSecurityPolicy = connection.getHeaderField("Content-Security-Policy"),
        xFrameOptions = connection.getHeaderField("X-Frame-Options"),
        referrerPolicy = connection.getHeaderField("Referrer-Policy"),
        body = (if (connection.responseCode >= 400) connection.errorStream else connection.inputStream)
          ?.bufferedReader()
          ?.use { it.readText() }
          .orEmpty(),
      )
    } finally {
      connection.disconnect()
    }
  }

  private fun assertSecurityHeaders(response: ResponseSnapshot) {
    assertEquals("no-store", response.cacheControl)
    assertEquals("frame-ancestors 'none'", response.contentSecurityPolicy)
    assertEquals("DENY", response.xFrameOptions)
    assertEquals("no-referrer", response.referrerPolicy)
  }

  private data class ResponseSnapshot(
    val status: Int,
    val setCookie: String?,
    val location: String?,
    val cacheControl: String?,
    val contentSecurityPolicy: String?,
    val xFrameOptions: String?,
    val referrerPolicy: String?,
    val body: String,
  )
}
