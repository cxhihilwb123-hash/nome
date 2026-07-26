package chat.simplex.common.model

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NomeServerConfigurationTest {
  @Test
  fun rebuiltCoreKeepsNomeServersUnderEnabledNomeOperator() {
    val nome = operatorGroup(
      smp = userServer(NomeServerConfiguration.legacySmpServer, preset = true),
      xftp = userServer(NomeServerConfiguration.legacyXftpServer, preset = true),
    )

    val migration = NomeServerConfiguration.migrate(listOf(nome))

    assertTrue(migration.changed)
    val migratedNome = migration.userServers.single()
    assertTrue(requireNotNull(migratedNome.operator).enabled)
    assertEquals(OperatorTag.Nome, migratedNome.operator.operatorTag)
    assertEquals(ServerRoles(true, true), migratedNome.operator.smpRoles)
    assertEquals(NomeServerConfiguration.smpServer, migratedNome.smpServers.single().server)
    assertEquals(NomeServerConfiguration.xftpServer, migratedNome.xftpServers.single().server)
    assertTrue(migratedNome.smpServers.single().enabled)
    assertTrue(migratedNome.xftpServers.single().enabled)
  }

  @Test
  fun legacyCoreDisablesUpstreamOperatorAndUsesCustomFallback() {
    val upstream = operatorGroup(
      operator = ServerOperator.sampleData1.copy(
        operatorTag = OperatorTag.SimpleX,
        tradeName = "SimpleX Chat",
        serverDomains = listOf("simplex.im"),
      ),
      smp = userServer("smp://legacy@smp.example.invalid", preset = true),
      xftp = userServer("xftp://legacy@xftp.example.invalid", preset = true),
    )

    val migration = NomeServerConfiguration.migrate(listOf(upstream))

    assertTrue(migration.changed)
    val migratedUpstream = migration.userServers.first()
    assertFalse(requireNotNull(migratedUpstream.operator).enabled)
    assertEquals(ServerRoles(false, false), migratedUpstream.operator.smpRoles)
    assertFalse(migratedUpstream.smpServers.single().enabled)
    assertFalse(migratedUpstream.xftpServers.single().enabled)

    val custom = migration.userServers.single { it.operator == null }
    assertEquals(NomeServerConfiguration.smpServer, custom.smpServers.single().server)
    assertEquals(NomeServerConfiguration.xftpServer, custom.xftpServers.single().server)
    assertTrue(custom.smpServers.single().enabled)
    assertTrue(custom.xftpServers.single().enabled)
  }

  @Test
  fun legacyNomeServersMigrateWithoutChangingCustomServers() {
    val customSmp = userServer("smp://custom-key@smp.example.test")
    val customXftp = userServer("xftp://custom-key@xftp.example.test")
    val customGroup = UserOperatorServers(
      operator = null,
      smpServers = listOf(userServer(NomeServerConfiguration.legacySmpServer), customSmp),
      xftpServers = listOf(userServer(NomeServerConfiguration.legacyXftpServer), customXftp),
    )

    val migrated = NomeServerConfiguration.migrate(listOf(customGroup)).userServers.single()

    assertEquals(customSmp, migrated.smpServers.single { it.server == customSmp.server })
    assertEquals(customXftp, migrated.xftpServers.single { it.server == customXftp.server })
    assertTrue(migrated.smpServers.single { it.server == customSmp.server }.enabled)
    assertTrue(migrated.xftpServers.single { it.server == customXftp.server }.enabled)
    assertEquals(NomeServerConfiguration.smpServer, migrated.smpServers.first().server)
    assertEquals(NomeServerConfiguration.xftpServer, migrated.xftpServers.first().server)
  }

  @Test
  fun migrationIsIdempotent() {
    val first = NomeServerConfiguration.migrate(emptyList())
    val second = NomeServerConfiguration.migrate(first.userServers)

    assertTrue(first.changed)
    assertFalse(second.changed)
    assertEquals(first.userServers, second.userServers)
  }

  @Test
  fun duplicateManagedEntriesAreDisabledWithoutTouchingCustomEntry() {
    val custom = userServer("smp://custom@smp.example.test")
    val group = UserOperatorServers(
      operator = null,
      smpServers = listOf(
        userServer(NomeServerConfiguration.smpServer),
        userServer(NomeServerConfiguration.legacySmpServer),
        custom,
      ),
      xftpServers = listOf(userServer(NomeServerConfiguration.xftpServer)),
    )

    val migrated = NomeServerConfiguration.migrate(listOf(group)).userServers.single()
    val managed = migrated.smpServers.filter { it.server == NomeServerConfiguration.smpServer || it.server.contains("124.223.71.168") }

    assertEquals(1, managed.count { it.enabled && !it.deleted })
    assertEquals(custom, migrated.smpServers.single { it.server == custom.server })
  }

  @Test
  fun nomeOperatorRetiresOnlyManagedFallbackRowsFromCustomGroup() {
    val customSmp = userServer("smp://custom@smp.example.test")
    val customXftp = userServer("xftp://custom@xftp.example.test")
    val nome = operatorGroup(
      smp = userServer(NomeServerConfiguration.smpServer, preset = true),
      xftp = userServer(NomeServerConfiguration.xftpServer, preset = true),
    )
    val custom = UserOperatorServers(
      operator = null,
      smpServers = listOf(userServer(NomeServerConfiguration.legacySmpServer), customSmp),
      xftpServers = listOf(userServer(NomeServerConfiguration.legacyXftpServer), customXftp),
    )

    val migrated = NomeServerConfiguration.migrate(listOf(nome, custom)).userServers

    val migratedNome = migrated.single { it.operator?.operatorTag == OperatorTag.Nome }
    assertTrue(requireNotNull(migratedNome.operator).enabled)
    assertTrue(migratedNome.smpServers.single().enabled)
    assertTrue(migratedNome.xftpServers.single().enabled)

    val migratedCustom = migrated.single { it.operator == null }
    assertEquals(customSmp, migratedCustom.smpServers.single { it.server == customSmp.server })
    assertEquals(customXftp, migratedCustom.xftpServers.single { it.server == customXftp.server })
    assertTrue(migratedCustom.smpServers.single { it.server == customSmp.server }.enabled)
    assertTrue(migratedCustom.xftpServers.single { it.server == customXftp.server }.enabled)
    assertTrue(migratedCustom.smpServers.single { it.server == NomeServerConfiguration.legacySmpServer }.deleted)
    assertTrue(migratedCustom.xftpServers.single { it.server == NomeServerConfiguration.legacyXftpServer }.deleted)
  }

  @Test
  fun customOperatorAndSameHostCustomCredentialsArePreserved() {
    val sameHostCustom = userServer("smp://user-owned-key@124.223.71.168:5223")
    val customOperator = ServerOperator.sampleData1.copy(
      operatorId = 99,
      operatorTag = null,
      tradeName = "Private operator",
      serverDomains = listOf("example.test"),
    )
    val customOperatorGroup = UserOperatorServers(
      operator = customOperator,
      smpServers = listOf(userServer("smp://private@smp.example.test")),
      xftpServers = listOf(userServer("xftp://private@xftp.example.test")),
    )
    val customGroup = UserOperatorServers(
      operator = null,
      smpServers = listOf(sameHostCustom),
      xftpServers = emptyList(),
    )

    val migrated = NomeServerConfiguration.migrate(listOf(customOperatorGroup, customGroup)).userServers

    assertEquals(customOperatorGroup, migrated.first())
    assertEquals(sameHostCustom, migrated.last().smpServers.first())
    assertTrue(migrated.last().smpServers.any { it.server == NomeServerConfiguration.smpServer })
  }

  @Test
  fun failedApplicationRetriesWithinTheSameStartup() = runBlocking {
    var calls = 0
    val waits = mutableListOf<Int>()

    val applied = NomeServerConfiguration.runWithRetry(
      waitBeforeRetry = { waits += it },
    ) {
      calls += 1
      calls == 3
    }

    assertTrue(applied)
    assertEquals(3, calls)
    assertEquals(listOf(1, 2), waits)
  }

  @Test
  fun exhaustedRetriesRemainPending() = runBlocking {
    var calls = 0

    val applied = NomeServerConfiguration.runWithRetry(
      attempts = 2,
      waitBeforeRetry = {},
    ) {
      calls += 1
      false
    }

    assertFalse(applied)
    assertEquals(2, calls)
  }

  private fun operatorGroup(
    operator: ServerOperator = ServerOperator.sampleData1,
    smp: UserServer,
    xftp: UserServer,
  ): UserOperatorServers =
    UserOperatorServers(
      operator = operator,
      smpServers = listOf(smp),
      xftpServers = listOf(xftp),
    )

  private fun userServer(address: String, preset: Boolean = false): UserServer = UserServer(
    remoteHostId = null,
    serverId = null,
    server = address,
    preset = preset,
    tested = true,
    enabled = true,
    deleted = false,
  )
}
