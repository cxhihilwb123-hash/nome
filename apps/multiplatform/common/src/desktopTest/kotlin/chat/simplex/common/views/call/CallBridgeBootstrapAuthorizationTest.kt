package chat.simplex.common.views.call

import org.nanohttpd.protocols.http.request.Method
import kotlin.test.Test
import kotlin.test.assertEquals

class CallBridgeBootstrapAuthorizationTest {
  private val nonce = "test-bootstrap-nonce"

  @Test
  fun bootstrapUsesStableLocalhostPermissionOrigin() {
    assertEquals(
      "http://localhost:50395/simplex/call/?bootstrap=$nonce",
      buildCallBridgeBootstrapUri(50395, nonce),
    )
  }

  @Test
  fun ordinaryPageGetNeverIssuesAuthorization() {
    val gate = CallBridgeBootstrapGate(nonce)

    assertEquals(
      CallBridgePageAuthorization.SERVE_PAGE_WITHOUT_AUTH,
      gate.authorize(request()),
    )
  }

  @Test
  fun exactBootstrapNonceIsAcceptedOnlyOnce() {
    val gate = CallBridgeBootstrapGate(nonce)
    val request = request(queryParameters = mapOf("bootstrap" to listOf(nonce)))

    assertEquals(CallBridgePageAuthorization.SERVE_AUTHORIZED_PAGE, gate.authorize(request))
    assertEquals(CallBridgePageAuthorization.NONCE_ALREADY_CONSUMED, gate.authorize(request))
  }

  @Test
  fun malformedOrUntrustedBootstrapRequestsAreRejected() {
    assertEquals(
      CallBridgePageAuthorization.INVALID_NONCE,
      CallBridgeBootstrapGate(nonce).authorize(
        request(queryParameters = mapOf("bootstrap" to listOf("wrong-nonce"))),
      ),
    )
    assertEquals(
      CallBridgePageAuthorization.INVALID_QUERY,
      CallBridgeBootstrapGate(nonce).authorize(
        request(queryParameters = mapOf("bootstrap" to listOf(nonce, nonce))),
      ),
    )
    assertEquals(
      CallBridgePageAuthorization.INVALID_QUERY,
      CallBridgeBootstrapGate(nonce).authorize(
        request(
          queryParameters = mapOf(
            "bootstrap" to listOf(nonce),
            "extra" to listOf("value"),
          ),
        ),
      ),
    )
    assertEquals(
      CallBridgePageAuthorization.INVALID_METHOD,
      CallBridgeBootstrapGate(nonce).authorize(
        request(method = Method.POST, queryParameters = mapOf("bootstrap" to listOf(nonce))),
      ),
    )
    assertEquals(
      CallBridgePageAuthorization.INVALID_REMOTE_IP,
      CallBridgeBootstrapGate(nonce).authorize(
        request(remoteIpAddress = "192.0.2.1", queryParameters = mapOf("bootstrap" to listOf(nonce))),
      ),
    )
    assertEquals(
      CallBridgePageAuthorization.INVALID_PATH,
      CallBridgeBootstrapGate(nonce).authorize(
        request(path = "/other/", queryParameters = mapOf("bootstrap" to listOf(nonce))),
      ),
    )
  }

  private fun request(
    method: Method = Method.GET,
    path: String = "/simplex/call/",
    queryParameters: Map<String, List<String>> = emptyMap(),
    remoteIpAddress: String = "127.0.0.1",
  ): CallBridgePageRequest =
    CallBridgePageRequest(
      method = method,
      path = path,
      queryParameters = queryParameters,
      remoteIpAddress = remoteIpAddress,
    )
}
