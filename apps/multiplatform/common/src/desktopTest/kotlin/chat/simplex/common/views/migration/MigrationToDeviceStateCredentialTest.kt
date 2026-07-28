package chat.simplex.common.views.migration

import chat.simplex.common.model.HostMode
import chat.simplex.common.model.NetCfg
import chat.simplex.common.model.NetworkProxy
import chat.simplex.common.model.NetworkProxyAuth
import chat.simplex.common.model.json
import chat.simplex.common.platform.CredentialUnavailable
import chat.simplex.common.platform.DesktopCredentialStore
import chat.simplex.common.platform.DesktopKeychainCryptor
import kotlinx.serialization.encodeToString
import java.net.URLEncoder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MigrationToDeviceStateCredentialTest {
  private val username = "migration-proxy-user"
  private val password = "migration proxy/password"
  private val legacyUsername = "legacy-migration-proxy-user"
  private val legacyPassword = "legacy migration proxy/password"
  private val bearer = "migration-file-bearer-secret"
  private val endpoint = "proxy.example.test:1080"
  private val proxy = NetworkProxy(
    username = username,
    password = password,
    auth = NetworkProxyAuth.USERNAME,
    host = "proxy.example.test",
    port = 1080,
  )
  private val cfg = NetCfg.defaults.copy(socksProxy = "$legacyUsername:$legacyPassword@$endpoint")

  @Test
  fun everyPersistedMigrationStateOmitsProxyCredentials() {
    val realMigrationLink = MigrationFileLinkData(
      networkConfig = MigrationFileLinkData.NetworkConfig(
        legacySocksProxy = "$legacyUsername:$legacyPassword@$endpoint",
        networkProxy = proxy,
        hostMode = HostMode.Public,
        requiredHostMode = true,
      )
    ).addToLink("simplex:/file#opaque-$bearer")
    assertFalse(realMigrationLink.contains(username), realMigrationLink)
    assertFalse(realMigrationLink.contains(password), realMigrationLink)
    assertFalse(realMigrationLink.contains(URLEncoder.encode(password, "UTF-8")), realMigrationLink)
    assertFalse(realMigrationLink.contains(legacyUsername), realMigrationLink)
    assertFalse(realMigrationLink.contains(legacyPassword), realMigrationLink)
    assertFalse(realMigrationLink.contains(URLEncoder.encode(legacyPassword, "UTF-8")), realMigrationLink)
    assertTrue(
      MigrationFileLinkData.NetworkConfig(
        legacySocksProxy = null,
        networkProxy = proxy,
        hostMode = HostMode.Public,
        requiredHostMode = true,
      ).withoutCredentialSecrets().requiresProxyCredentialEntry()
    )
    val states = listOf<MigrationToDeviceState>(
      MigrationToDeviceState.Onion(
        link = realMigrationLink,
        socksProxy = "$legacyUsername:$legacyPassword@$endpoint",
        networkProxy = proxy,
        hostMode = HostMode.Public,
        requiredHostMode = true,
      ),
      MigrationToDeviceState.DownloadProgress(realMigrationLink, "archive.zip", cfg, proxy),
      MigrationToDeviceState.ArchiveImport("archive.zip", cfg, proxy),
      MigrationToDeviceState.Passphrase(cfg, proxy),
    )

    for (state in states) {
      val serialized = json.encodeToString<MigrationToDeviceState>(
        state.redactedForPersistence("migrationNetworkProxy")
      )

      assertFalse(serialized.contains(username), serialized)
      assertFalse(serialized.contains(password), serialized)
      assertFalse(serialized.contains(URLEncoder.encode(password, "UTF-8")), serialized)
      assertFalse(serialized.contains(legacyUsername), serialized)
      assertFalse(serialized.contains(legacyPassword), serialized)
      assertFalse(serialized.contains(URLEncoder.encode(legacyPassword, "UTF-8")), serialized)
      assertFalse(serialized.contains(bearer), serialized)
      assertContains(serialized, "migrationNetworkProxy")
      assertContains(serialized, "proxy.example.test")
      if (state is MigrationToDeviceState.Onion || state is MigrationToDeviceState.DownloadProgress) {
        assertContains(serialized.filterNot(Char::isWhitespace), "\"link\":\"\"")
      }
    }
  }

  @Test
  fun nonDesktopStageSanitizerDropsOnlyTransientBearerLinks() {
    val onion = MigrationToDeviceState.Onion(
      link = "simplex:/file#opaque-$bearer",
      socksProxy = "$legacyUsername:$legacyPassword@$endpoint",
      networkProxy = proxy,
      hostMode = HostMode.Public,
      requiredHostMode = true,
    )
    val sanitized = onion.withoutTransientFileLink() as MigrationToDeviceState.Onion

    assertEquals("", sanitized.link)
    assertEquals(onion.socksProxy, sanitized.socksProxy)
    assertEquals(onion.networkProxy, sanitized.networkProxy)
  }

  @Test
  fun migrationCredentialVaultRoundTripsAndCleansUp() {
    val store = FakeCredentialStore()
    val vault = MigrationProxyCredentialVault(DesktopKeychainCryptor(store) { _, _ -> })

    vault.store(proxy)

    assertEquals(proxy, vault.load("migrationNetworkProxy"))
    assertEquals(json.encodeToString(proxy), store.values["migrationNetworkProxy"])

    vault.remove()

    assertNull(store.values["migrationNetworkProxy"])
  }

  @Test
  fun missingOrUntrustedCredentialReferenceFailsClosed() {
    val store = FakeCredentialStore()
    val vault = MigrationProxyCredentialVault(DesktopKeychainCryptor(store) { _, _ -> })

    assertFailsWith<CredentialUnavailable> { vault.load("migrationNetworkProxy") }
    assertFailsWith<CredentialUnavailable> { vault.load("databasePassword") }
  }

  @Test
  fun strippedLegacyProxyMarkerKeepsEndpointAndRequestsUsernameAuthentication() {
    val restored = networkProxyFromLegacyMigrationValue("@$endpoint")

    assertEquals("proxy.example.test", restored.host)
    assertEquals(1080, restored.port)
    assertEquals(NetworkProxyAuth.USERNAME, restored.auth)
    assertTrue(restored.username.isBlank())
    assertTrue(restored.password.isBlank())
    assertFalse(restored.host.startsWith("@"))
  }

  @Test
  fun olderLegacyProxyValueCanStillPopulateRecoveryForm() {
    val restored = networkProxyFromLegacyMigrationValue("$username:$password@$endpoint")

    assertEquals(proxy, restored)
  }

  private class FakeCredentialStore : DesktopCredentialStore {
    val values = mutableMapOf<String, String>()

    override fun get(alias: String): String? = values[alias]

    override fun set(alias: String, value: String) {
      values[alias] = value
    }

    override fun delete(alias: String) {
      values.remove(alias)
    }
  }
}
