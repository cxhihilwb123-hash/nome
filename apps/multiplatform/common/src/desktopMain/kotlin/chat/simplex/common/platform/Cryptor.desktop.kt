package chat.simplex.common.platform

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.Base64

private const val KEYCHAIN_SERVICE = "chat.nome.app.credentials"
private const val KEYCHAIN_DATA_MARKER = "nome-keychain-v1"
private const val KEYCHAIN_IV_MARKER = "credential-reference"

private const val ERR_SEC_SUCCESS = 0
private const val ERR_SEC_DUPLICATE_ITEM = -25299
private const val ERR_SEC_ITEM_NOT_FOUND = -25300

actual val cryptor: CryptorInterface =
  if (desktopPlatform.isMac()) {
    DesktopKeychainCryptor(MacOSKeychainCredentialStore())
  } else {
    // Nome currently ships only for macOS ARM. Keep the shared desktop source
    // compiling for upstream Linux/Windows targets until their native credential
    // stores (Secret Service / DPAPI) are implemented.
    LegacyDesktopCryptor
  }

internal interface DesktopCredentialStore {
  fun get(alias: String): String?
  fun set(alias: String, value: String)
  fun delete(alias: String)
}

internal class DesktopKeychainCryptor(
  private val credentialStore: DesktopCredentialStore,
  private val writeMarkers: (String, Pair<ByteArray, ByteArray>) -> Unit = ::writeKeychainMarkersToSettings,
) : CryptorInterface {
  override fun decryptData(data: ByteArray, iv: ByteArray, alias: String): String? {
    if (isKeychainMarker(data, iv)) return credentialStore.get(alias)

    // Recover if the app stopped between the two settings updates. The secret
    // was already committed to Keychain before either marker is written.
    if (isKeychainDataMarker(data) || isKeychainIvMarker(iv)) {
      val value = credentialStore.get(alias) ?: return null
      writeMarkers(alias, keychainMarkers())
      return value
    }

    val legacyValue = legacyPlaintext(data, iv) ?: return null
    credentialStore.set(alias, legacyValue)
    writeMarkers(alias, keychainMarkers())
    return legacyValue
  }

  override fun encryptText(text: String, alias: String): Pair<ByteArray, ByteArray> {
    credentialStore.set(alias, text)
    return keychainMarkers()
  }

  override fun deleteKey(alias: String) {
    credentialStore.delete(alias)
  }
}

/**
 * The pre-Keychain desktop implementation wrote the same UTF-8 bytes to both
 * fields. Restrict migration to that exact shape so corrupt or unknown formats
 * are never interpreted as plaintext credentials.
 */
internal fun legacyPlaintext(data: ByteArray, iv: ByteArray): String? {
  if (data.isEmpty() || !data.contentEquals(iv) || isKeychainDataMarker(data)) return null
  val value = data.toString(StandardCharsets.UTF_8)
  return value.takeIf { it.toByteArray(StandardCharsets.UTF_8).contentEquals(data) }
}

internal fun keychainMarkers(): Pair<ByteArray, ByteArray> =
  KEYCHAIN_DATA_MARKER.toByteArray(StandardCharsets.UTF_8) to
    KEYCHAIN_IV_MARKER.toByteArray(StandardCharsets.UTF_8)

private fun isKeychainMarker(data: ByteArray, iv: ByteArray): Boolean =
  isKeychainDataMarker(data) && isKeychainIvMarker(iv)

private fun isKeychainDataMarker(data: ByteArray): Boolean =
  data.contentEquals(KEYCHAIN_DATA_MARKER.toByteArray(StandardCharsets.UTF_8))

private fun isKeychainIvMarker(iv: ByteArray): Boolean =
  iv.contentEquals(KEYCHAIN_IV_MARKER.toByteArray(StandardCharsets.UTF_8))

private data class CredentialPreferenceKeys(val data: String, val iv: String)

private fun preferenceKeys(alias: String): CredentialPreferenceKeys? = when (alias) {
  "databasePassword" -> CredentialPreferenceKeys(
    data = "EncryptedDBPassphrase",
    iv = "InitializationVectorDBPassphrase",
  )
  "appPassword" -> CredentialPreferenceKeys(
    data = "EncryptedAppPassphrase",
    iv = "InitializationVectorAppPassphrase",
  )
  "selfDestructPassword" -> CredentialPreferenceKeys(
    data = "EncryptedSelfDestructPassphrase",
    iv = "InitializationVectorSelfDestructPassphrase",
  )
  "networkProxy" -> CredentialPreferenceKeys(
    data = "NetworkProxyHostPort",
    iv = "NetworkProxyIV",
  )
  else -> null
}

private fun writeKeychainMarkersToSettings(alias: String, markers: Pair<ByteArray, ByteArray>) {
  val keys = preferenceKeys(alias) ?: return
  val encoder = Base64.getEncoder()
  // The data field contained the legacy secret, so replace it first. If the
  // process stops before the IV update, the partial-marker recovery above
  // finishes migration on the next launch.
  settings.putString(keys.data, encoder.encodeToString(markers.first))
  settings.putString(keys.iv, encoder.encodeToString(markers.second))
}

internal class MacOSKeychainCredentialStore(
  private val service: String = KEYCHAIN_SERVICE,
  private val configPath: String = desktopPlatform.configPath,
  private val dataPath: String = desktopPlatform.dataPath,
  private val migrateUnscopedAccount: Boolean =
    System.getenv("XDG_CONFIG_HOME").isNullOrBlank() &&
      System.getenv("XDG_DATA_HOME").isNullOrBlank(),
) : DesktopCredentialStore {
  private val security: MacOSSecurityLibrary by lazy {
    Native.load("Security", MacOSSecurityLibrary::class.java)
  }
  private val coreFoundation: MacOSCoreFoundationLibrary by lazy {
    Native.load("CoreFoundation", MacOSCoreFoundationLibrary::class.java)
  }
  private val lock = Any()
  private val profileScope = keychainProfileScope(configPath, dataPath)

  override fun get(alias: String): String? = synchronized(lock) {
    read(scopedAccount(alias)) ?: migrateUnscopedCredential(alias)
  }

  private fun read(account: String): String? {
    val passwordLength = IntByReference()
    val passwordData = PointerByReference()
    val status = find(account, passwordLength, passwordData, null)
    if (status == ERR_SEC_ITEM_NOT_FOUND) return null
    checkStatus("read", status)

    val nativeData = passwordData.value
    val bytes = if (passwordLength.value == 0 || nativeData == null) {
      ByteArray(0)
    } else {
      nativeData.getByteArray(0, passwordLength.value)
    }
    return try {
      bytes.toString(StandardCharsets.UTF_8)
    } finally {
      bytes.fill(0)
      if (nativeData != null) {
        security.SecKeychainItemFreeContent(null, nativeData)
      }
    }
  }

  override fun set(alias: String, value: String) = synchronized(lock) {
    write(scopedAccount(alias), value)
  }

  private fun write(account: String, value: String) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8)
    val nativeData = Memory(maxOf(1, bytes.size).toLong())
    if (bytes.isNotEmpty()) nativeData.write(0, bytes, 0, bytes.size)
    try {
      val serviceBytes = service.toByteArray(StandardCharsets.UTF_8)
      val accountBytes = account.toByteArray(StandardCharsets.UTF_8)
      val addStatus = security.SecKeychainAddGenericPassword(
        null,
        serviceBytes.size,
        serviceBytes,
        accountBytes.size,
        accountBytes,
        bytes.size,
        nativeData,
        null,
      )
      when (addStatus) {
        ERR_SEC_SUCCESS -> Unit
        ERR_SEC_DUPLICATE_ITEM -> update(account, bytes.size, nativeData)
        else -> checkStatus("write", addStatus)
      }
    } finally {
      bytes.fill(0)
      nativeData.clear()
    }
  }

  override fun delete(alias: String) = synchronized(lock) {
    deleteAccount(scopedAccount(alias))
    if (migrateUnscopedAccount) deleteAccount(alias)
  }

  private fun deleteAccount(account: String) {
    val item = PointerByReference()
    val status = find(account, null, null, item)
    if (status == ERR_SEC_ITEM_NOT_FOUND) return
    checkStatus("find for deletion", status)
    val itemRef = item.value ?: throw KeychainAccessException("find for deletion", status)
    try {
      checkStatus("delete", security.SecKeychainItemDelete(itemRef))
    } finally {
      coreFoundation.CFRelease(itemRef)
    }
  }

  private fun update(account: String, passwordLength: Int, passwordData: Pointer) {
    val item = PointerByReference()
    val status = find(account, null, null, item)
    checkStatus("find for update", status)
    val itemRef = item.value ?: throw KeychainAccessException("find for update", status)
    try {
      checkStatus(
        "update",
        security.SecKeychainItemModifyAttributesAndData(
          itemRef,
          null,
          passwordLength,
          passwordData,
        ),
      )
    } finally {
      coreFoundation.CFRelease(itemRef)
    }
  }

  private fun migrateUnscopedCredential(alias: String): String? {
    if (!migrateUnscopedAccount) return null
    val value = read(alias) ?: return null
    write(scopedAccount(alias), value)
    deleteAccount(alias)
    return value
  }

  private fun scopedAccount(alias: String): String = "$alias:$profileScope"

  private fun find(
    account: String,
    passwordLength: IntByReference?,
    passwordData: PointerByReference?,
    item: PointerByReference?,
  ): Int {
    val serviceBytes = service.toByteArray(StandardCharsets.UTF_8)
    val accountBytes = account.toByteArray(StandardCharsets.UTF_8)
    return security.SecKeychainFindGenericPassword(
      null,
      serviceBytes.size,
      serviceBytes,
      accountBytes.size,
      accountBytes,
      passwordLength,
      passwordData,
      item,
    )
  }

  private fun checkStatus(operation: String, status: Int) {
    if (status != ERR_SEC_SUCCESS) throw KeychainAccessException(operation, status)
  }
}

internal fun keychainProfileScope(configPath: String, dataPath: String): String {
  val normalizedConfigPath = Paths.get(configPath).toAbsolutePath().normalize().toString()
  val normalizedDataPath = Paths.get(dataPath).toAbsolutePath().normalize().toString()
  val profileIdentity = "$normalizedConfigPath\u0000$normalizedDataPath"
    .toByteArray(StandardCharsets.UTF_8)
  val digest = try {
    MessageDigest.getInstance("SHA-256").digest(profileIdentity)
  } finally {
    profileIdentity.fill(0)
  }
  return try {
    digest.take(16).joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
  } finally {
    digest.fill(0)
  }
}

internal fun scopedKeychainAccount(alias: String, configPath: String, dataPath: String): String =
  "$alias:${keychainProfileScope(configPath, dataPath)}"

private class KeychainAccessException(operation: String, status: Int) :
  IllegalStateException("macOS Keychain $operation failed (OSStatus $status)")

private interface MacOSSecurityLibrary : Library {
  fun SecKeychainFindGenericPassword(
    keychainOrArray: Pointer?,
    serviceNameLength: Int,
    serviceName: ByteArray,
    accountNameLength: Int,
    accountName: ByteArray,
    passwordLength: IntByReference?,
    passwordData: PointerByReference?,
    itemRef: PointerByReference?,
  ): Int

  fun SecKeychainAddGenericPassword(
    keychain: Pointer?,
    serviceNameLength: Int,
    serviceName: ByteArray,
    accountNameLength: Int,
    accountName: ByteArray,
    passwordLength: Int,
    passwordData: Pointer,
    itemRef: PointerByReference?,
  ): Int

  fun SecKeychainItemModifyAttributesAndData(
    itemRef: Pointer,
    attributes: Pointer?,
    passwordLength: Int,
    passwordData: Pointer,
  ): Int

  fun SecKeychainItemDelete(itemRef: Pointer): Int

  fun SecKeychainItemFreeContent(attributes: Pointer?, data: Pointer): Int
}

private interface MacOSCoreFoundationLibrary : Library {
  fun CFRelease(value: Pointer)
}

private object LegacyDesktopCryptor : CryptorInterface {
  override fun decryptData(data: ByteArray, iv: ByteArray, alias: String): String? =
    legacyPlaintext(data, iv)

  override fun encryptText(text: String, alias: String): Pair<ByteArray, ByteArray> {
    val bytes = text.toByteArray(StandardCharsets.UTF_8)
    return bytes to bytes.copyOf()
  }

  override fun deleteKey(alias: String) = Unit
}
