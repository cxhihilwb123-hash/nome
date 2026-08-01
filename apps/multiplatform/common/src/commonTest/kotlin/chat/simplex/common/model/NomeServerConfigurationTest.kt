package chat.simplex.common.model

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NomeServerConfigurationTest {
  @Test
  fun desktopStandardSmpPortMigrationMovesOnlyInheritedPresetTo5223Mode() {
    assertEquals(
      SMPWebPortServers.Off,
      NomeServerConfiguration.migrateDesktopStandardSmpPortPreference(
        SMPWebPortServers.Preset,
        alreadyApplied = false,
      ),
    )
    assertEquals(
      SMPWebPortServers.All,
      NomeServerConfiguration.migrateDesktopStandardSmpPortPreference(
        SMPWebPortServers.All,
        alreadyApplied = false,
      ),
    )
    assertEquals(
      SMPWebPortServers.Off,
      NomeServerConfiguration.migrateDesktopStandardSmpPortPreference(
        SMPWebPortServers.Off,
        alreadyApplied = false,
      ),
    )
    assertEquals(
      null,
      NomeServerConfiguration.migrateDesktopStandardSmpPortPreference(
        SMPWebPortServers.Preset,
        alreadyApplied = true,
      ),
    )
  }

  @Test
  fun officialRoutingUsesNomeDomainsAndTreatsTencentIpAsLegacyOnly() {
    assertEquals("smp.nome.im", NomeServerConfiguration.smpHostname)
    assertEquals("xftp.nome.im", NomeServerConfiguration.xftpHostname)
    assertEquals("relay.nome.im", NomeServerConfiguration.chatRelayHostname)
    assertTrue(NomeServerConfiguration.smpServer.endsWith("@smp.nome.im:5223"))
    assertTrue(NomeServerConfiguration.xftpServer.endsWith("@xftp.nome.im"))
    assertTrue(NomeServerConfiguration.chatRelayAddress.startsWith("https://relay.nome.im/r#"))
    assertTrue(NomeServerConfiguration.legacySmpServer.endsWith("@124.223.71.168:5223"))
    assertTrue(NomeServerConfiguration.legacyXftpServer.endsWith("@124.223.71.168:5224"))
  }

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
    assertTrue(migratedNome.smpServers.single().preset)
    assertTrue(migratedNome.xftpServers.single().preset)
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
    assertTrue(custom.smpServers.single().preset)
    assertTrue(custom.xftpServers.single().preset)
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
    assertTrue(migrated.smpServers.first().preset)
    assertTrue(migrated.xftpServers.first().preset)
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
  fun firstSeedAddsEnabledNomeRelayToNomeOperator() {
    val nome = operatorGroup(
      smp = userServer(NomeServerConfiguration.smpServer, preset = true),
      xftp = userServer(NomeServerConfiguration.xftpServer, preset = true),
    )

    val migration = NomeServerConfiguration.migrate(listOf(nome), seedDefaultChatRelay = true)

    assertTrue(migration.changed)
    val relay = migration.userServers.single().chatRelays.single()
    assertEquals(NomeServerConfiguration.chatRelayAddress, relay.address)
    assertEquals(NomeServerConfiguration.chatRelayName, relay.displayName)
    assertEquals(listOf(NomeServerConfiguration.chatRelayDomain), relay.domains)
    assertFalse(relay.preset)
    assertTrue(relay.enabled)
    assertFalse(relay.deleted)
  }

  @Test
  fun firstSeedUsesCompatibilityGroupWhenNomeOperatorIsUnavailable() {
    val upstream = operatorGroup(
      operator = ServerOperator.sampleData1.copy(operatorTag = OperatorTag.SimpleX),
      smp = userServer("smp://legacy@smp.example.invalid", preset = true),
      xftp = userServer("xftp://legacy@xftp.example.invalid", preset = true),
    )

    val migration = NomeServerConfiguration.migrate(listOf(upstream), seedDefaultChatRelay = true)

    assertTrue(migration.userServers.single { it.operator == null }.chatRelays.single().enabled)
    assertTrue(migration.userServers.single { it.operator != null }.chatRelays.isEmpty())
  }

  @Test
  fun existingDisabledNomeRelayIsNeverReenabledOrDuplicated() {
    val disabledRelay = UserChatRelay(
      chatRelayId = 42,
      address = NomeServerConfiguration.chatRelayAddress,
      relayProfile = RelayProfile(displayName = NomeServerConfiguration.chatRelayName, fullName = ""),
      domains = listOf(NomeServerConfiguration.chatRelayDomain),
      preset = false,
      tested = true,
      enabled = false,
      deleted = true,
    )
    val nome = operatorGroup(
      smp = userServer(NomeServerConfiguration.smpServer, preset = true),
      xftp = userServer(NomeServerConfiguration.xftpServer, preset = true),
    ).copy(chatRelays = listOf(disabledRelay))

    val first = NomeServerConfiguration.migrate(listOf(nome), seedDefaultChatRelay = true)
    val later = NomeServerConfiguration.migrate(first.userServers, seedDefaultChatRelay = false)

    assertEquals(listOf(disabledRelay), first.userServers.single().chatRelays)
    assertEquals(listOf(disabledRelay), later.userServers.single().chatRelays)
  }

  @Test
  fun customRelayIsPreservedBesideFirstNomeRelaySeed() {
    val customRelay = UserChatRelay(
      chatRelayId = 7,
      address = "https://relay.example.test/r#custom",
      relayProfile = RelayProfile(displayName = "Private relay", fullName = ""),
      domains = listOf("example.test"),
      preset = false,
      tested = true,
      enabled = true,
      deleted = false,
    )
    val nome = operatorGroup(
      smp = userServer(NomeServerConfiguration.smpServer, preset = true),
      xftp = userServer(NomeServerConfiguration.xftpServer, preset = true),
    ).copy(chatRelays = listOf(customRelay))

    val migration = NomeServerConfiguration.migrate(listOf(nome), seedDefaultChatRelay = true)

    val relays = migration.userServers.single().chatRelays
    assertEquals(customRelay, relays.first())
    assertEquals(NomeServerConfiguration.chatRelayAddress, relays.last().address)
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
    assertTrue(managed.single { it.enabled && !it.deleted }.preset)
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
    val sameLegacyHostCustom = userServer("smp://user-owned-key@124.223.71.168:5223")
    val sameOfficialHostCustom = userServer("smp://user-owned-key@smp.nome.im")
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
      smpServers = listOf(sameLegacyHostCustom, sameOfficialHostCustom),
      xftpServers = emptyList(),
    )

    val migrated = NomeServerConfiguration.migrate(listOf(customOperatorGroup, customGroup)).userServers

    assertEquals(customOperatorGroup, migrated.first())
    assertEquals(sameLegacyHostCustom, migrated.last().smpServers[0])
    assertEquals(sameOfficialHostCustom, migrated.last().smpServers[1])
    assertFalse(migrated.last().smpServers[0].preset)
    assertFalse(migrated.last().smpServers[1].preset)
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

  @Test
  fun unexpectedConfigurationExceptionPropagatesWithoutRetry() {
    var calls = 0

    assertFailsWith<IllegalStateException> {
      runBlocking {
        NomeServerConfiguration.runWithRetry(attempts = 3, waitBeforeRetry = {}) {
          calls += 1
          throw IllegalStateException("native failure")
        }
      }
    }
    assertEquals(1, calls)
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
