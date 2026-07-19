package chat.simplex.app.nome

import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeStatePanelState
import org.junit.Assert.assertEquals
import org.junit.Test

class NomeFoundationContractTest {
  @Test
  fun foundationStateAndButtonContractsStayExplicit() {
    assertEquals(
      listOf("NORMAL", "LOADING", "EMPTY", "OFFLINE", "ERROR", "PERMISSION", "DANGER"),
      NomeStatePanelState.entries.map { it.name },
    )
    assertEquals(
      listOf("PRIMARY", "SECONDARY", "DESTRUCTIVE", "DESTRUCTIVE_SECONDARY"),
      NomeButtonVariant.entries.map { it.name },
    )
  }
}
