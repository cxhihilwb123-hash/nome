package chat.simplex.common.views.chatlist

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import chat.simplex.common.model.ChatModel
import chat.simplex.common.views.helpers.AnimatedViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformHomeRouteDesktopTest {
  @Test
  fun desktopContractInvokesOfficialChatListContent() = runBlocking(ImmediateFrameClock) {
    var defaultContentInvoked = false
    val recomposer = Recomposer(coroutineContext)
    val composition = Composition(UnitApplier(), recomposer)
    val runner = launch {
      recomposer.runRecomposeAndApplyChanges()
    }
    try {
      composition.setContent {
        PlatformHomeRoute(
          chatModel = ChatModel,
          userPickerState = MutableStateFlow(AnimatedViewState.GONE),
          setPerformLA = {},
          stopped = false,
        ) {
          defaultContentInvoked = true
        }
      }
      recomposer.awaitIdle()
      assertTrue(defaultContentInvoked)
    } finally {
      composition.dispose()
      recomposer.cancel()
      runner.join()
    }
  }

  private class UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
  }

  private object ImmediateFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
  }
}
