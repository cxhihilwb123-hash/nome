package chat.simplex.common.model

import chat.simplex.common.views.migration.MigrationFileLinkData
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TerminalItemRedactionTest {
  private val credential = "terminal-history-secret"
  private val server = "smp://public-fingerprint:$credential@smp.example.test"

  @Test
  fun protocolServerTestCommandDoesNotRetainCredential() {
    val item = TerminalItem.cmd(null, CC.APITestProtoServer(userId = 1, server = server))

    assertFalse(item.details.contains(credential))
    assertContains(item.details, "smp://***@smp.example.test")
  }

  @Test
  fun userServersResponseDoesNotRetainCredential() {
    val userServer = UserServer(
      remoteHostId = null,
      serverId = 1,
      server = server,
      preset = true,
      tested = true,
      enabled = true,
      deleted = false,
    )
    val response = API.Result(
      remoteHostId = null,
      res = CR.UserServers(
        user = UserRef(userId = 1, localDisplayName = "Test", activeUser = true, showNtfs = true),
        userServers = listOf(
          UserOperatorServers(operator = null, smpServers = listOf(userServer), xftpServers = emptyList()),
        ),
      ),
    )

    val item = TerminalItem.resp(null, response)

    assertFalse(item.details.contains(credential))
    assertContains(item.details, "smp://***@smp.example.test")
  }

  @Test
  fun unrelatedTextIsUnchanged() {
    val text = "Nome server smp.nome.im is ready"

    assertContains(redactServerCredentials(text), text)
  }

  @Test
  fun turnUriUserInfoIsRedacted() {
    val credential = "turn-uri-secret"
    val text = "turns://relay-user:$credential@turn.example.test:443?transport=tcp"

    val redacted = redactServerCredentials(text)

    assertFalse(redacted.contains(credential))
    assertContains(redacted, "turns://***@turn.example.test")
  }

  @Test
  fun networkConfigWireCommandKeepsCredentialsButDiagnosticsRedactThem() {
    val username = "proxy-user"
    val password = "proxy-password"
    val endpoint = "proxy.example.test:1080"
    val cfg = NetCfg.defaults.copy(socksProxy = "$username:$password@$endpoint")

    val command = CC.APISetNetworkConfig(cfg)
    val wireCommand = command.cmdString
    val diagnosticCommand = command.obfuscated.cmdString
    val item = TerminalItem.cmd(null, CC.APISetNetworkConfig(cfg))

    assertContains(wireCommand, username)
    assertContains(wireCommand, password)
    for (value in listOf(diagnosticCommand, item.details)) {
      assertFalse(value.contains(username))
      assertFalse(value.contains(password))
      assertContains(value, "***@$endpoint")
    }
  }

  @Test
  fun networkConfigResponseStructurallyRedactsProxyCredentials() {
    val username = "response-user"
    val password = "response-password"
    val endpoint = "proxy.example.test:1080"
    val cfg = NetCfg.defaults.copy(socksProxy = "$username:$password@$endpoint")
    val response = API.Result(remoteHostId = null, res = CR.NetworkConfig(cfg))

    val item = TerminalItem.resp(null, response)

    assertFalse(response.details.contains(username))
    assertFalse(response.details.contains(password))
    assertFalse(item.details.contains(username))
    assertFalse(item.details.contains(password))
    assertContains(item.details, "***@$endpoint")
  }

  @Test
  fun jsonAndBareProxyUserInfoAreRedacted() {
    val secret = "proxy-secret"
    val endpoint = "proxy.example.test:1080"
    val text = """{"socksProxy":"proxy-user:$secret@$endpoint"} proxy-user:$secret@$endpoint"""

    val redacted = redactServerCredentials(text)

    assertFalse(redacted.contains(secret))
    assertFalse(redacted.contains("proxy-user"))
    assertEquals(2, Regex("\\*\\*\\*@$endpoint").findAll(redacted).count())
  }

  @Test
  fun unauthenticatedProxyEndpointIsUnchanged() {
    val endpoint = "proxy.example.test:1080"

    assertEquals(endpoint, redactServerCredentials(endpoint))
    assertEquals(endpoint, redactSocksProxyCredentials(endpoint))
  }

  @Test
  fun appSettingsCommandsAndResponsesRedactProxyCredentialsOnlyInDiagnostics() {
    val username = "settings-proxy-user"
    val password = "settings-proxy-password"
    val endpoint = "proxy.example.test:1080"
    val turnUsername = "settings-turn-user"
    val turnPassword = "settings-turn-password"
    val turnEndpoint = "turn.example.test:443?transport=tcp"
    val turnUri = "turns://$turnUsername:$turnPassword@$turnEndpoint"
    val proxy = NetworkProxy(
      username = username,
      password = password,
      auth = NetworkProxyAuth.USERNAME,
      host = "proxy.example.test",
      port = 1080,
    )
    val settings = AppSettings(
      networkConfig = NetCfg.defaults.copy(socksProxy = "$username:$password@$endpoint"),
      networkProxy = proxy,
      webrtcICEServers = listOf(turnUri),
    )

    for (command in listOf(CC.ApiSaveSettings(settings), CC.ApiGetSettings(settings))) {
      assertContains(command.cmdString, password)
      assertContains(command.cmdString, turnPassword)
      val terminal = TerminalItem.cmd(null, command)
      assertFalse(terminal.details.contains(username), terminal.details)
      assertFalse(terminal.details.contains(password), terminal.details)
      assertFalse(terminal.details.contains(turnUsername), terminal.details)
      assertFalse(terminal.details.contains(turnPassword), terminal.details)
      assertContains(terminal.details, endpoint)
      assertContains(terminal.details, "turns://***@$turnEndpoint")
    }

    val response = API.Result(remoteHostId = null, res = CR.AppSettingsR(settings))
    val terminal = TerminalItem.resp(null, response)
    for (details in listOf(response.details, terminal.details)) {
      assertFalse(details.contains(username), details)
      assertFalse(details.contains(password), details)
      assertFalse(details.contains(turnUsername), details)
      assertFalse(details.contains(turnPassword), details)
      assertContains(details, endpoint)
      assertContains(details, "turns://***@$turnEndpoint")
    }
  }

  @Test
  fun standaloneFileCommandsKeepBearerOnWireButNotInTerminalHistory() {
    val bearer = "migration-file-bearer-secret"
    val fileLink = "simplex:/file#opaque-$bearer"
    val file = CryptoFile.plain("/tmp/nome-migration.zip")
    val commands = listOf(
      CC.ApiStandaloneFileInfo(fileLink),
      CC.ApiDownloadStandaloneFile(userId = 1, url = fileLink, file = file),
    )

    for (command in commands) {
      assertContains(command.cmdString, bearer)
      val terminal = TerminalItem.cmd(null, command)
      assertFalse(terminal.label.contains(bearer), terminal.label)
      assertFalse(terminal.details.contains(bearer), terminal.details)
      assertContains(terminal.details, "<redacted-file-link>")
    }
  }

  @Test
  fun standaloneFileInfoResponseIsStructuralOnly() {
    val legacyProxySecret = "standalone-info-legacy-proxy-secret"
    val fileMeta = MigrationFileLinkData(
      networkConfig = MigrationFileLinkData.NetworkConfig(
        legacySocksProxy = "legacy-user:$legacyProxySecret@proxy.example.test:1080",
        networkProxy = null,
        hostMode = HostMode.Public,
        requiredHostMode = true,
      )
    )
    val response = API.Result(remoteHostId = null, res = CR.StandaloneFileInfo(fileMeta))
    val terminal = TerminalItem.resp(null, response)

    for (details in listOf(response.details, terminal.details)) {
      assertFalse(details.contains(legacyProxySecret), details)
      assertFalse(details.contains("proxy.example.test"), details)
      assertEquals("fileMeta: present", details)
    }
  }
}
