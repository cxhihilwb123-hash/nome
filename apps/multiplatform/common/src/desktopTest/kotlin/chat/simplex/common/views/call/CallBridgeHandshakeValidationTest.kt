package chat.simplex.common.views.call

import kotlin.test.Test
import kotlin.test.assertEquals

class CallBridgeHandshakeValidationTest {
  private val expectedPath = "/simplex/call/ws/test-token"
  private val expectedOrigin = "http://127.0.0.1:50395"

  @Test
  fun acceptsLoopbackOriginAndUnpredictableWebSocketPath() {
    assertEquals(
      CallBridgeHandshakeValidation.ACCEPTED,
      validate(
        headers = mapOf(
          "origin" to expectedOrigin,
        ),
        path = expectedPath,
      )
    )
  }

  @Test
  fun rejectsNonLoopbackRemoteIp() {
    assertEquals(
      CallBridgeHandshakeValidation.INVALID_REMOTE_IP,
      validate(
        remoteIpAddress = "192.168.1.30",
        headers = mapOf(
          "origin" to expectedOrigin,
        ),
        path = expectedPath,
      )
    )
  }

  @Test
  fun rejectsMismatchedOrigin() {
    assertEquals(
      CallBridgeHandshakeValidation.INVALID_ORIGIN,
      validate(
        headers = mapOf(
          "origin" to "http://localhost:50395",
        ),
        path = expectedPath,
      )
    )
  }

  @Test
  fun rejectsWrongWebSocketPathEvenWithExpectedOrigin() {
    assertEquals(
      CallBridgeHandshakeValidation.INVALID_TOKEN,
      validate(
        headers = mapOf(
          "origin" to expectedOrigin,
        ),
        path = "/simplex/call/ws/wrong-token",
      )
    )
  }

  private fun validate(
    headers: Map<String, String>,
    remoteIpAddress: String = "127.0.0.1",
    path: String = expectedPath,
  ): CallBridgeHandshakeValidation =
    validateCallBridgeHandshake(
      request = CallBridgeHandshakeRequest(headers, remoteIpAddress, path),
      expectedWebSocketPath = expectedPath,
      expectedOrigin = expectedOrigin
    )
}
