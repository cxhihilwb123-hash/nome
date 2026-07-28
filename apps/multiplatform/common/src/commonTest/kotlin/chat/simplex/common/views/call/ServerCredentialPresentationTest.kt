package chat.simplex.common.views.call

import chat.simplex.common.views.usersettings.safeRtcServerDisplayName
import chat.simplex.common.views.usersettings.networkAndServers.serverHostname
import kotlin.test.Test
import kotlin.test.assertFalse

class ServerCredentialPresentationTest {
  @Test
  fun invalidProtocolServerNeverFallsBackToRawCredentialText() {
    val secret = "smp://private-key:private-password@not a valid host"
    val presented = serverHostname(secret)

    assertFalse(presented.contains("private-key"))
    assertFalse(presented.contains("private-password"))
    assertFalse(presented == secret)
  }

  @Test
  fun invalidTurnServerNeverFallsBackToRawCredentialText() {
    val secret = "turn:private-user:private-password@not a valid host"
    val presented = safeRtcServerDisplayName(secret)

    assertFalse(presented.contains("private-user"))
    assertFalse(presented.contains("private-password"))
    assertFalse(presented == secret)
  }
}
