package chat.simplex.common.model

import chat.simplex.common.platform.CryptorInterface
import chat.simplex.common.platform.CredentialUnavailable
import chat.simplex.common.platform.DesktopCredentialStore
import chat.simplex.common.platform.DesktopKeychainCryptor
import chat.simplex.common.views.helpers.toBase64StringForPassphrase
import chat.simplex.common.views.helpers.toByteArrayFromBase64ForPassphrase
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkProxyPreferenceStoreTest {
  private var data: String? = null
  private var iv: String? = null
  private val credentialStore = FakeCredentialStore()

  @Test
  fun macSettingsContainOnlyMarkers() {
    val preference = keychainPreference()
    val proxy = authenticatedProxy()

    preference.set(proxy)

    assertMarkerSettings()
    assertEquals(json.encodeToString(proxy), credentialStore.values["networkProxy"])
    assertEquals(proxy, preference.get())
  }

  @Test
  fun legacyPlaintextJsonMigratesToKeychainOnRead() {
    val proxy = authenticatedProxy()
    data = json.encodeToString(proxy)

    val restored = keychainPreference().get()

    assertEquals(proxy, restored)
    assertMarkerSettings()
    assertEquals(json.encodeToString(proxy), credentialStore.values["networkProxy"])
  }

  @Test
  fun legacyHostPortMigratesWithoutChangingEndpoint() {
    data = "proxy.example.test:1080"

    val restored = keychainPreference().get()

    assertEquals(NetworkProxy(host = "proxy.example.test", port = 1080), restored)
    assertMarkerSettings()
  }

  @Test
  fun dataMarkerRecoversIfProcessStoppedBeforeIvWrite() {
    val proxy = authenticatedProxy()
    val preference = keychainPreference()
    preference.set(proxy)
    iv = null

    val restored = preference.get()

    assertEquals(proxy, restored)
    assertMarkerSettings()
  }

  @Test
  fun corruptSettingIsNotMigratedAsCredential() {
    data = "not-a-proxy-and-not-a-keychain-marker"

    val failure = assertFailsWith<CredentialUnavailable> { keychainPreference().get() }

    assertEquals("network proxy credential is unavailable", failure.message)
    assertTrue(credentialStore.values.isEmpty())
    assertEquals("not-a-proxy-and-not-a-keychain-marker", data)
    assertNull(iv)
  }

  @Test
  fun markerWithoutKeychainCredentialFailsClosed() {
    val proxy = authenticatedProxy()
    val preference = keychainPreference()
    preference.set(proxy)
    credentialStore.values.clear()

    assertFailsWith<CredentialUnavailable> { preference.get() }
    assertMarkerSettings()
  }

  @Test
  fun keychainOsFailureIsExposedAsCredentialUnavailable() {
    val markers = chat.simplex.common.platform.keychainMarkers()
    data = markers.first.toBase64StringForPassphrase()
    iv = markers.second.toBase64StringForPassphrase()
    val preference = NetworkProxyPreferenceStore(
      getData = { data },
      setData = { data = it },
      getIv = { iv },
      setIv = { iv = it },
      useKeychain = true,
      credentialCryptor = ThrowingCryptor,
    )

    val failure = assertFailsWith<CredentialUnavailable> { preference.get() }

    assertEquals("network proxy credential is unavailable", failure.message)
    assertEquals("simulated keychain OSStatus failure", failure.cause?.message)
  }

  @Test
  fun androidCompatibleModeKeepsHistoricalJsonPreference() {
    val proxy = authenticatedProxy()
    val preference = NetworkProxyPreferenceStore(
      getData = { data },
      setData = { data = it },
      getIv = { iv },
      setIv = { iv = it },
      useKeychain = false,
      credentialCryptor = FailingCryptor,
    )

    preference.set(proxy)

    assertEquals(proxy, preference.get())
    assertTrue(data.orEmpty().contains(proxy.username))
    assertTrue(data.orEmpty().contains(proxy.password))
    assertNull(iv)
  }

  @Test
  fun explicitSettingsExportStillCarriesProxyCredentials() {
    val proxy = authenticatedProxy()

    val exported = json.encodeToString(AppSettings(networkProxy = proxy))

    // Keychain protection is local-at-rest only. The explicit settings export
    // preserves the setup inside its encrypted archive; migration link
    // metadata is tested separately and must omit these credentials.
    assertTrue(exported.contains(proxy.username))
    assertTrue(exported.contains(proxy.password))
  }

  private fun keychainPreference(): NetworkProxyPreferenceStore {
    val cryptor = DesktopKeychainCryptor(credentialStore) { _, markers ->
      data = markers.first.toBase64StringForPassphrase()
      iv = markers.second.toBase64StringForPassphrase()
    }
    return NetworkProxyPreferenceStore(
      getData = { data },
      setData = { data = it },
      getIv = { iv },
      setIv = { iv = it },
      useKeychain = true,
      credentialCryptor = cryptor,
    )
  }

  private fun assertMarkerSettings() {
    assertEquals("nome-keychain-v1", data?.toByteArrayFromBase64ForPassphrase()?.toString(Charsets.UTF_8))
    assertEquals("credential-reference", iv?.toByteArrayFromBase64ForPassphrase()?.toString(Charsets.UTF_8))
    assertFalse(data.orEmpty().contains("proxy-user"))
    assertFalse(data.orEmpty().contains("proxy-password"))
    assertFalse(iv.orEmpty().contains("proxy-user"))
    assertFalse(iv.orEmpty().contains("proxy-password"))
  }

  private fun authenticatedProxy() = NetworkProxy(
    username = "proxy-user",
    password = "proxy-password",
    auth = NetworkProxyAuth.USERNAME,
    host = "proxy.example.test",
    port = 1080,
  )

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

  private object FailingCryptor : CryptorInterface {
    override fun decryptData(data: ByteArray, iv: ByteArray, alias: String): String? =
      error("cryptor must not be used in Android-compatible mode")

    override fun encryptText(text: String, alias: String): Pair<ByteArray, ByteArray> =
      error("cryptor must not be used in Android-compatible mode")

    override fun deleteKey(alias: String) =
      error("cryptor must not be used in Android-compatible mode")
  }

  private object ThrowingCryptor : CryptorInterface {
    override fun decryptData(data: ByteArray, iv: ByteArray, alias: String): String? =
      error("simulated keychain OSStatus failure")

    override fun encryptText(text: String, alias: String): Pair<ByteArray, ByteArray> =
      error("simulated keychain OSStatus failure")

    override fun deleteKey(alias: String) = Unit
  }
}
