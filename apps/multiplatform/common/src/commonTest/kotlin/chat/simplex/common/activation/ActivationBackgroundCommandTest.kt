package chat.simplex.common.activation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ActivationBackgroundCommandTest {
  @Test
  fun successfulBackgroundCommandReturnsItsResult() = runBlocking {
    assertTrue(runActivationAwareBackgroundCommand { true })
  }

  @Test
  fun expectedActivationBlockDoesNotEscapeAsACrash() = runBlocking {
    assertFalse(
      runActivationAwareBackgroundCommand {
        throw ActivationRequiredException(ActivationCapability.NETWORK_CONFIGURATION)
      },
    )
  }

  @Test
  fun unexpectedFailureStillEscapes() = runBlocking {
    assertFailsWith<IllegalStateException> {
      runActivationAwareBackgroundCommand { throw IllegalStateException("network observer failed") }
    }
    Unit
  }

  @Test
  fun cancellationStillEscapesUnchanged() = runBlocking {
    val cancellation = CancellationException("network observation cancelled")

    val thrown = assertFailsWith<CancellationException> {
      runActivationAwareBackgroundCommand { throw cancellation }
    }

    assertSame(cancellation, thrown)
  }
}
