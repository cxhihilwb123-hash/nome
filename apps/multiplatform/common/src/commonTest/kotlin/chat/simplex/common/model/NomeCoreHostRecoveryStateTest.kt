package chat.simplex.common.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NomeCoreHostRecoveryStateTest {
  @Test
  fun hostReconnectInvalidatesPendingRecovery() {
    val state = NomeCoreHostRecoveryState()
    val token = assertNotNull(state.hostStateChanged(connected = false))

    assertTrue(state.isCurrentDisconnect(token))
    state.hostStateChanged(connected = true)

    assertFalse(state.isCurrentDisconnect(token))
  }

  @Test
  fun newerDisconnectInvalidatesOlderRecovery() {
    val state = NomeCoreHostRecoveryState()
    val first = assertNotNull(state.hostStateChanged(connected = false))
    val second = assertNotNull(state.hostStateChanged(connected = false))

    assertFalse(state.isCurrentDisconnect(first))
    assertTrue(state.isCurrentDisconnect(second))
  }
}
