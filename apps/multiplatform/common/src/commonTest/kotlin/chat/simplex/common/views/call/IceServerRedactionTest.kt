package chat.simplex.common.views.call

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class IceServerRedactionTest {
  @Test
  fun credentialsAndUriUserInfoAreRedacted() {
    val output = redactIceServersForLog(
      listOf(
        RTCIceServer(
          urls = listOf("stun:turn.example.test:3478", "turn:user:uri-secret@turn.example.test:3478"),
          username = "account-name",
          credential = "account-secret",
        ),
      ),
    )

    assertContains(output, "stun:turn.example.test:3478")
    assertContains(output, "turn:***@turn.example.test:3478")
    assertFalse(output.contains("uri-secret"))
    assertFalse(output.contains("account-name"))
    assertFalse(output.contains("account-secret"))
  }

  @Test
  fun nullListIsReportedWithoutCredentials() {
    assertContains(redactIceServersForLog(null), "null")
  }
}
