package chat.simplex.app.nome.connection

import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewEvent
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewPhase
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewStateAdapter
import chat.simplex.common.views.newchat.ConnectionPreviewFailureKind
import chat.simplex.common.views.newchat.ConnectionPreviewIdentity
import org.junit.Assert.assertEquals
import org.junit.Test

class NomeConnectionPreviewStateAdapterTest {
  @Test
  fun identityIsMutableOnlyWhileReady() {
    val ready = NomeConnectionPreviewStateAdapter.initial(
      ConnectionPreviewIdentity.CurrentProfile,
    )
    val incognito = NomeConnectionPreviewStateAdapter.reduce(
      ready,
      NomeConnectionPreviewEvent.SelectIdentity(
        ConnectionPreviewIdentity.Incognito,
      ),
    )
    val connecting = NomeConnectionPreviewStateAdapter.reduce(
      incognito,
      NomeConnectionPreviewEvent.Submit,
    )

    assertEquals(ConnectionPreviewIdentity.Incognito, incognito.identity)
    assertEquals(NomeConnectionPreviewPhase.Connecting, connecting.phase)
    assertEquals(
      connecting,
      NomeConnectionPreviewStateAdapter.reduce(
        connecting,
        NomeConnectionPreviewEvent.SelectIdentity(
          ConnectionPreviewIdentity.CurrentProfile,
        ),
      ),
    )
  }

  @Test
  fun duplicateSubmitAndOutOfOrderResultsAreIgnored() {
    val ready = NomeConnectionPreviewStateAdapter.initial(
      ConnectionPreviewIdentity.CurrentProfile,
    )
    val connecting = NomeConnectionPreviewStateAdapter.reduce(
      ready,
      NomeConnectionPreviewEvent.Submit,
    )

    assertEquals(
      connecting,
      NomeConnectionPreviewStateAdapter.reduce(
        connecting,
        NomeConnectionPreviewEvent.Submit,
      ),
    )
    assertEquals(
      ready,
      NomeConnectionPreviewStateAdapter.reduce(
        ready,
        NomeConnectionPreviewEvent.Pending,
      ),
    )
  }

  @Test
  fun failureRetainsIdentityAndRetryEntersReplanning() {
    val connecting = NomeConnectionPreviewStateAdapter.reduce(
      NomeConnectionPreviewStateAdapter.initial(
        ConnectionPreviewIdentity.Incognito,
      ),
      NomeConnectionPreviewEvent.Submit,
    )
    val failed = NomeConnectionPreviewStateAdapter.reduce(
      connecting,
      NomeConnectionPreviewEvent.Failed(
        ConnectionPreviewFailureKind.Network,
      ),
    )
    val replanning = NomeConnectionPreviewStateAdapter.reduce(
      failed,
      NomeConnectionPreviewEvent.Retry,
    )
    val replanFailed = NomeConnectionPreviewStateAdapter.reduce(
      replanning,
      NomeConnectionPreviewEvent.Failed(
        ConnectionPreviewFailureKind.NoCurrentUser,
      ),
    )

    assertEquals(ConnectionPreviewIdentity.Incognito, failed.identity)
    assertEquals(NomeConnectionPreviewPhase.Failure, failed.phase)
    assertEquals(ConnectionPreviewFailureKind.Network, failed.failureKind)
    assertEquals(NomeConnectionPreviewPhase.Replanning, replanning.phase)
    assertEquals(ConnectionPreviewIdentity.Incognito, replanFailed.identity)
    assertEquals(NomeConnectionPreviewPhase.Failure, replanFailed.phase)
    assertEquals(
      ConnectionPreviewFailureKind.NoCurrentUser,
      replanFailed.failureKind,
    )
  }
}
