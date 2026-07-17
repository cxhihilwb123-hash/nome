package chat.simplex.common.ui.nome.home

import chat.simplex.common.model.ChatListLoadGeneration
import chat.simplex.common.model.ChatListLoadState
import chat.simplex.common.model.UserNetworkInfo

enum class NomeHomeContentState {
  LOADING,
  FIRST_USE,
  TRUE_EMPTY,
  FILTERED_NO_RESULT,
  POPULATED,
  UNAVAILABLE,
}

enum class NomeHomeConnectivityState {
  UNKNOWN,
  ONLINE,
  DEVICE_OFFLINE,
}

enum class NomeHomeCoreState {
  STARTING,
  RUNNING,
  STOPPED,
}

data class NomeHomeTruthInput<T>(
  val currentGeneration: ChatListLoadGeneration?,
  val localUserCreated: Boolean?,
  val chatRunning: Boolean?,
  val switchingUsersAndHosts: Boolean,
  val loadState: ChatListLoadState,
  val baseChats: List<T>,
  val visibleChats: List<T>,
  val hasActiveFilter: Boolean,
  val platformNetworkInfo: UserNetworkInfo?,
)

data class NomeHomeState<T>(
  val content: NomeHomeContentState,
  val connectivity: NomeHomeConnectivityState,
  val core: NomeHomeCoreState,
  val visibleChats: List<T>,
  val hasCachedChats: Boolean,
)

object NomeHomeStateAdapter {
  fun <T> derive(input: NomeHomeTruthInput<T>): NomeHomeState<T> {
    val connectivity = when (input.platformNetworkInfo?.online) {
      null -> NomeHomeConnectivityState.UNKNOWN
      true -> NomeHomeConnectivityState.ONLINE
      false -> NomeHomeConnectivityState.DEVICE_OFFLINE
    }
    val core = when (input.chatRunning) {
      null -> NomeHomeCoreState.STARTING
      true -> NomeHomeCoreState.RUNNING
      false -> NomeHomeCoreState.STOPPED
    }
    val generation = input.currentGeneration
    val loadGeneration = input.loadState.generationOrNull()
    val generationMatches = generation != null && generation == loadGeneration
    val loadingMustHideRows =
      input.loadState is ChatListLoadState.Loading &&
        (input.loadState.hideRows || input.baseChats.isEmpty())

    val content = when {
      input.switchingUsersAndHosts -> NomeHomeContentState.LOADING
      input.chatRunning == null || input.localUserCreated == null ->
        NomeHomeContentState.LOADING
      loadingMustHideRows -> NomeHomeContentState.LOADING
      input.localUserCreated == false &&
        generation == null &&
        input.loadState is ChatListLoadState.NoCurrentUser ->
        NomeHomeContentState.FIRST_USE
      generation == null -> NomeHomeContentState.LOADING
      input.loadState is ChatListLoadState.Unavailable && generationMatches ->
        NomeHomeContentState.UNAVAILABLE
      input.loadState is ChatListLoadState.Loaded && !generationMatches ->
        NomeHomeContentState.LOADING
      input.loadState is ChatListLoadState.Loading && !generationMatches ->
        NomeHomeContentState.LOADING
      input.loadState is ChatListLoadState.Loaded ||
        input.loadState is ChatListLoadState.Loading -> when {
          input.visibleChats.isNotEmpty() -> NomeHomeContentState.POPULATED
          input.baseChats.isNotEmpty() && input.hasActiveFilter ->
            NomeHomeContentState.FILTERED_NO_RESULT
          input.baseChats.isEmpty() && generationMatches ->
            NomeHomeContentState.TRUE_EMPTY
          else -> NomeHomeContentState.LOADING
        }
      input.loadState is ChatListLoadState.NoCurrentUser ->
        NomeHomeContentState.LOADING
      input.loadState is ChatListLoadState.Unavailable ->
        NomeHomeContentState.LOADING
      ChatListLoadState.Initial == input.loadState ->
        NomeHomeContentState.LOADING
      else -> NomeHomeContentState.LOADING
    }

    val mayExposeRows =
      content == NomeHomeContentState.POPULATED ||
        (content == NomeHomeContentState.UNAVAILABLE && generationMatches)
    return NomeHomeState(
      content = content,
      connectivity = connectivity,
      core = core,
      visibleChats = if (mayExposeRows) input.visibleChats else emptyList(),
      hasCachedChats = input.baseChats.isNotEmpty() && generationMatches,
    )
  }
}

private fun ChatListLoadState.generationOrNull(): ChatListLoadGeneration? =
  when (this) {
    ChatListLoadState.Initial -> null
    is ChatListLoadState.Loading -> generation
    is ChatListLoadState.Loaded -> generation
    is ChatListLoadState.Unavailable -> generation
    is ChatListLoadState.NoCurrentUser -> null
  }
