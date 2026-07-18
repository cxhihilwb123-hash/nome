package chat.simplex.common.ui.nome.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal object NomeFocusRestoration {
  private val mutableCurrentProfileRequests =
    MutableStateFlow(0L)

  val currentProfileRequests: StateFlow<Long> =
    mutableCurrentProfileRequests

  fun requestCurrentProfileFocus() {
    mutableCurrentProfileRequests.update { request ->
      if (request == Long.MAX_VALUE) 1L else request + 1L
    }
  }
}
