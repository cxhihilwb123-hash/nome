package chat.simplex.common.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class ChatListCancellationTest {
  @Test
  fun cancellationPropagatesInsteadOfBecomingOrdinaryFailure() = runBlocking {
    var failureMapperCalled = false
    var thrown: Throwable? = null

    try {
      runChatListOperationPreservingCancellation<String>(
        operation = { throw CancellationException("cancel chat list load") },
        onFailure = {
          failureMapperCalled = true
          "failure"
        },
      )
    } catch (e: Throwable) {
      thrown = e
    }

    assertIs<CancellationException>(thrown)
    assertFalse(failureMapperCalled)
  }

  @Test
  fun ordinaryExceptionStillMapsToTypedFailurePath() = runBlocking {
    val result = runChatListOperationPreservingCancellation(
      operation = { throw IllegalStateException("ordinary failure") },
      onFailure = { "failure" },
    )

    assertEquals("failure", result)
  }
}
