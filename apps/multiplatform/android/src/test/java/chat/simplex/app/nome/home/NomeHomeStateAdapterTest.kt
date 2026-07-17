package chat.simplex.app.nome.home

import chat.simplex.common.model.ChatListLoadGeneration
import chat.simplex.common.model.ChatListLoadState
import chat.simplex.common.model.UserNetworkInfo
import chat.simplex.common.model.UserNetworkType
import chat.simplex.common.ui.nome.home.NomeHomeConnectivityState
import chat.simplex.common.ui.nome.home.NomeHomeContentState
import chat.simplex.common.ui.nome.home.NomeHomeCoreState
import chat.simplex.common.ui.nome.home.NomeHomeStateAdapter
import chat.simplex.common.ui.nome.home.NomeHomeTruthInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NomeHomeStateAdapterTest {
  private val generation = ChatListLoadGeneration(remoteHostId = null, userId = 1L)
  private val chat = "chat-1"

  @Test
  fun initialAndInFlightStatesNeverBecomeEmpty() {
    assertEquals(
      NomeHomeContentState.LOADING,
      derive(loadState = ChatListLoadState.Initial).content,
    )
    assertEquals(
      NomeHomeContentState.LOADING,
      derive(
        loadState = ChatListLoadState.Loading(generation, hideRows = true),
        baseChats = emptyList(),
      ).content,
    )
  }

  @Test
  fun nonHidingSameGenerationRefreshKeepsCachedProjection() {
    val populated = derive(
      loadState = ChatListLoadState.Loading(generation, hideRows = false),
      baseChats = listOf(chat),
      visibleChats = listOf(chat),
    )
    val filtered = derive(
      loadState = ChatListLoadState.Loading(generation, hideRows = false),
      baseChats = listOf(chat),
      visibleChats = emptyList(),
      hasActiveFilter = true,
    )

    assertEquals(NomeHomeContentState.POPULATED, populated.content)
    assertEquals(listOf(chat), populated.visibleChats)
    assertEquals(NomeHomeContentState.FILTERED_NO_RESULT, filtered.content)
    assertTrue(filtered.hasCachedChats)
  }

  @Test
  fun firstUseRequiresExplicitNoCurrentUserFacts() {
    val state = derive(
      currentGeneration = null,
      localUserCreated = false,
      loadState = ChatListLoadState.NoCurrentUser(remoteHostId = null),
    )

    assertEquals(NomeHomeContentState.FIRST_USE, state.content)
    assertTrue(state.visibleChats.isEmpty())
  }

  @Test
  fun noCurrentUserResultCannotOverrideAnExistingUserGeneration() {
    val state = derive(
      localUserCreated = true,
      loadState = ChatListLoadState.NoCurrentUser(remoteHostId = null),
      baseChats = listOf(chat),
      visibleChats = listOf(chat),
    )

    assertEquals(NomeHomeContentState.LOADING, state.content)
    assertTrue(state.visibleChats.isEmpty())
  }

  @Test
  fun trueEmptyRequiresSameGenerationTypedSuccess() {
    val state = derive(
      loadState = ChatListLoadState.Loaded(generation),
      baseChats = emptyList(),
      visibleChats = emptyList(),
    )
    val mismatch = derive(
      loadState = ChatListLoadState.Loaded(generation.copy(userId = 2L)),
      baseChats = emptyList(),
      visibleChats = emptyList(),
    )

    assertEquals(NomeHomeContentState.TRUE_EMPTY, state.content)
    assertEquals(NomeHomeContentState.LOADING, mismatch.content)
  }

  @Test
  fun typedFailureIsUnavailableNotEmpty() {
    val state = derive(
      loadState = ChatListLoadState.Unavailable(generation),
      baseChats = emptyList(),
    )

    assertEquals(NomeHomeContentState.UNAVAILABLE, state.content)
  }

  @Test
  fun sameGenerationFailureKeepsCachedRowsButMismatchHidesThem() {
    val cached = derive(
      loadState = ChatListLoadState.Unavailable(generation),
      baseChats = listOf(chat),
      visibleChats = listOf(chat),
    )
    val mismatch = derive(
      loadState = ChatListLoadState.Unavailable(
        generation.copy(userId = 2L),
      ),
      baseChats = listOf(chat),
      visibleChats = listOf(chat),
    )

    assertEquals(NomeHomeContentState.UNAVAILABLE, cached.content)
    assertEquals(listOf(chat), cached.visibleChats)
    assertTrue(cached.hasCachedChats)
    assertEquals(NomeHomeContentState.LOADING, mismatch.content)
    assertTrue(mismatch.visibleChats.isEmpty())
  }

  @Test
  fun activeFilterCanHideRowsWithoutMutatingLoadedBase() {
    val baseChats = mutableListOf(chat)
    val state = derive(
      loadState = ChatListLoadState.Loaded(generation),
      baseChats = baseChats,
      visibleChats = emptyList(),
      hasActiveFilter = true,
    )

    assertEquals(NomeHomeContentState.FILTERED_NO_RESULT, state.content)
    assertTrue(state.hasCachedChats)
    assertEquals(listOf(chat), baseChats)
  }

  @Test
  fun successfulRowsSuppressedWithoutAFilterFailClosedAsLoading() {
    val state = derive(
      loadState = ChatListLoadState.Loaded(generation),
      baseChats = listOf(chat),
      visibleChats = emptyList(),
    )

    assertEquals(NomeHomeContentState.LOADING, state.content)
    assertTrue(state.visibleChats.isEmpty())
  }

  @Test
  fun populatedStatePreservesCoreOrder() {
    val chats = listOf(chat, "chat-2")
    val state = derive(
      loadState = ChatListLoadState.Loaded(generation),
      baseChats = chats,
      visibleChats = chats,
    )

    assertEquals(NomeHomeContentState.POPULATED, state.content)
    assertEquals(chats, state.visibleChats)
  }

  @Test
  fun switchingAndGenerationMismatchHideStaleRows() {
    val switching = derive(
      switchingUsersAndHosts = true,
      loadState = ChatListLoadState.Loaded(generation),
      baseChats = listOf(chat),
      visibleChats = listOf(chat),
    )
    val mismatch = derive(
      loadState = ChatListLoadState.Loaded(generation.copy(userId = 2L)),
      baseChats = listOf(chat),
      visibleChats = listOf(chat),
    )

    assertEquals(NomeHomeContentState.LOADING, switching.content)
    assertTrue(switching.visibleChats.isEmpty())
    assertEquals(NomeHomeContentState.LOADING, mismatch.content)
    assertTrue(mismatch.visibleChats.isEmpty())
  }

  @Test
  fun unobservedNetworkIsUnknownAndOfflineIsDeviceOnly() {
    val unknown = derive(
      loadState = ChatListLoadState.Loaded(generation),
      platformNetworkInfo = null,
    )
    val offline = derive(
      loadState = ChatListLoadState.Loaded(generation),
      platformNetworkInfo = UserNetworkInfo(UserNetworkType.NONE, online = false),
    )

    assertEquals(NomeHomeConnectivityState.UNKNOWN, unknown.connectivity)
    assertEquals(NomeHomeConnectivityState.DEVICE_OFFLINE, offline.connectivity)
  }

  @Test
  fun stoppedCoreRemainsSeparateFromLoadedContent() {
    val state = derive(
      chatRunning = false,
      loadState = ChatListLoadState.Loaded(generation),
      baseChats = listOf(chat),
      visibleChats = listOf(chat),
    )

    assertEquals(NomeHomeCoreState.STOPPED, state.core)
    assertEquals(NomeHomeContentState.POPULATED, state.content)
    assertEquals(listOf(chat), state.visibleChats)
  }

  private fun derive(
    currentGeneration: ChatListLoadGeneration? = generation,
    localUserCreated: Boolean? = true,
    chatRunning: Boolean? = true,
    switchingUsersAndHosts: Boolean = false,
    loadState: ChatListLoadState,
    baseChats: List<String> = emptyList(),
    visibleChats: List<String> = baseChats,
    hasActiveFilter: Boolean = false,
    platformNetworkInfo: UserNetworkInfo? =
      UserNetworkInfo(UserNetworkType.WIFI, online = true),
  ) = NomeHomeStateAdapter.derive(
    NomeHomeTruthInput(
      currentGeneration = currentGeneration,
      localUserCreated = localUserCreated,
      chatRunning = chatRunning,
      switchingUsersAndHosts = switchingUsersAndHosts,
      loadState = loadState,
      baseChats = baseChats,
      visibleChats = visibleChats,
      hasActiveFilter = hasActiveFilter,
      platformNetworkInfo = platformNetworkInfo,
    ),
  )
}
