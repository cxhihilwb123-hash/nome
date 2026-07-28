package chat.simplex.common.platform

import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CryptorDesktopTest {
  @Test
  fun newCredentialIsStoredOnlyInCredentialStore() {
    val store = FakeCredentialStore()
    val cryptor = DesktopKeychainCryptor(store) { _, _ -> }
    val secret = "database-secret-that-must-not-enter-settings"

    val encrypted = cryptor.encryptText(secret, "databasePassword")

    assertEquals(secret, store.values["databasePassword"])
    assertFalse(encrypted.first.contentEquals(secret.toByteArray(StandardCharsets.UTF_8)))
    assertFalse(encrypted.second.contentEquals(secret.toByteArray(StandardCharsets.UTF_8)))
    assertFalse(encrypted.first.contentEquals(encrypted.second))
  }

  @Test
  fun markerReadsCredentialFromStore() {
    val store = FakeCredentialStore(mutableMapOf("appPassword" to "123456"))
    val cryptor = DesktopKeychainCryptor(store) { _, _ -> }
    val markers = keychainMarkers()

    assertEquals("123456", cryptor.decryptData(markers.first, markers.second, "appPassword"))
  }

  @Test
  fun legacyPlaintextIsMigratedAndReplacedWithMarkers() {
    val store = FakeCredentialStore()
    var migratedAlias: String? = null
    var persistedMarkers: Pair<ByteArray, ByteArray>? = null
    val cryptor = DesktopKeychainCryptor(store) { alias, markers ->
      migratedAlias = alias
      persistedMarkers = markers
    }
    val legacy = "legacy-self-destruct-code".toByteArray(StandardCharsets.UTF_8)

    assertEquals(
      "legacy-self-destruct-code",
      cryptor.decryptData(legacy, legacy.copyOf(), "selfDestructPassword"),
    )

    assertEquals("legacy-self-destruct-code", store.values["selfDestructPassword"])
    assertEquals("selfDestructPassword", migratedAlias)
    val expected = keychainMarkers()
    assertContentEquals(expected.first, persistedMarkers?.first)
    assertContentEquals(expected.second, persistedMarkers?.second)
  }

  @Test
  fun partialMarkerRecoversAfterInterruptedSettingsMigration() {
    val store = FakeCredentialStore(mutableMapOf("databasePassword" to "already-in-keychain"))
    var rewroteMarkers = false
    val cryptor = DesktopKeychainCryptor(store) { _, _ -> rewroteMarkers = true }
    val markers = keychainMarkers()
    val remainingLegacyIv = "already-in-keychain".toByteArray(StandardCharsets.UTF_8)

    assertEquals(
      "already-in-keychain",
      cryptor.decryptData(markers.first, remainingLegacyIv, "databasePassword"),
    )
    assertTrue(rewroteMarkers)
  }

  @Test
  fun unknownOrCorruptFormatIsNotTreatedAsPlaintext() {
    val store = FakeCredentialStore()
    val cryptor = DesktopKeychainCryptor(store) { _, _ -> }

    assertNull(
      cryptor.decryptData(
        "not-encrypted-data".toByteArray(StandardCharsets.UTF_8),
        "different-iv".toByteArray(StandardCharsets.UTF_8),
        "databasePassword",
      ),
    )
    assertTrue(store.values.isEmpty())
  }

  @Test
  fun deleteRemovesCredentialFromStore() {
    val store = FakeCredentialStore(mutableMapOf("appPassword" to "123456"))
    val cryptor = DesktopKeychainCryptor(store) { _, _ -> }

    cryptor.deleteKey("appPassword")

    assertNull(store.values["appPassword"])
    assertEquals(listOf("appPassword"), store.deletedAliases)
  }

  @Test
  fun profileScopedAccountsAreStableAndDoNotCollide() {
    val profileA = scopedKeychainAccount(
      alias = "databasePassword",
      configPath = "/private/tmp/nome-profile-a/config/simplex",
      dataPath = "/private/tmp/nome-profile-a/data/simplex",
    )
    val profileARepeat = scopedKeychainAccount(
      alias = "databasePassword",
      configPath = "/private/tmp/nome-profile-a/config/../config/simplex",
      dataPath = "/private/tmp/nome-profile-a/data/simplex",
    )
    val profileB = scopedKeychainAccount(
      alias = "databasePassword",
      configPath = "/private/tmp/nome-profile-b/config/simplex",
      dataPath = "/private/tmp/nome-profile-b/data/simplex",
    )

    assertEquals(profileA, profileARepeat)
    assertNotEquals(profileA, profileB)
    assertTrue(profileA.startsWith("databasePassword:"))
    assertFalse(profileA.contains("/private/tmp"))
    assertEquals(49, profileA.length)
  }

  @Test
  fun nativeMacKeychainRoundTripWhenExplicitlyEnabled() {
    if (System.getenv("NOME_KEYCHAIN_INTEGRATION_TEST") != "1") return

    val store = MacOSKeychainCredentialStore(
      service = "chat.nome.app.credentials.test.${UUID.randomUUID()}",
      configPath = "/private/tmp/nome-keychain-integration/config",
      dataPath = "/private/tmp/nome-keychain-integration/data",
      migrateUnscopedAccount = false,
    )
    val alias = "integrationTestPassword"
    try {
      assertNull(store.get(alias))
      store.set(alias, "first-synthetic-secret")
      assertEquals("first-synthetic-secret", store.get(alias))
      store.set(alias, "updated-synthetic-secret")
      assertEquals("updated-synthetic-secret", store.get(alias))
      store.delete(alias)
      assertNull(store.get(alias))
    } finally {
      store.delete(alias)
    }
  }

  private class FakeCredentialStore(
    val values: MutableMap<String, String> = mutableMapOf(),
  ) : DesktopCredentialStore {
    val deletedAliases = mutableListOf<String>()

    override fun get(alias: String): String? = values[alias]

    override fun set(alias: String, value: String) {
      values[alias] = value
    }

    override fun delete(alias: String) {
      deletedAliases += alias
      values.remove(alias)
    }
  }
}
