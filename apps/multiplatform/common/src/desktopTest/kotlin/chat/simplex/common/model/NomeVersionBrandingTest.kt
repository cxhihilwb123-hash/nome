package chat.simplex.common.model

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class NomeVersionBrandingTest {
  @Test
  fun developerVersionDetailsUseNomeLabels() {
    val details = CR.VersionInfo(
      versionInfo = CoreVersionInfo(
        version = "6.5.6",
        simplexmqVersion = "6.5.2",
        simplexmqCommit = "abcdef0",
      ),
      chatMigrations = emptyList(),
      agentMigrations = emptyList(),
    ).details

    assertContains(details, "nomeCoreVersion")
    assertContains(details, "nomeCoreCommit")
    assertFalse(details.contains("simplexmq", ignoreCase = true))
  }
}
