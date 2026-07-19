package chat.simplex.common.views.newchat

import kotlin.test.Test
import kotlin.test.assertEquals

class NomeChannelCancellationPolicyTest {
  @Test
  fun finalizesLocalRemovalOnlyAfterConfirmedDelete() =
    kotlinx.coroutines.runBlocking {
      var finalized = 0

      assertEquals(
        ChannelCancellationResult.NOT_DELETED,
        cancelCreatedChannel(
          delete = { false },
          onDeleted = { finalized++ },
        ),
      )
      assertEquals(0, finalized)

      assertEquals(
        ChannelCancellationResult.DELETED,
        cancelCreatedChannel(
          delete = { true },
          onDeleted = { finalized++ },
        ),
      )
      assertEquals(1, finalized)
    }

  @Test
  fun exceptionRetainsLocalChannelState() =
    kotlinx.coroutines.runBlocking {
      var finalized = 0

      assertEquals(
        ChannelCancellationResult.FAILED,
        cancelCreatedChannel(
          delete = {
            error("fixture failure")
          },
          onDeleted = { finalized++ },
        ),
      )
      assertEquals(0, finalized)
    }
}
