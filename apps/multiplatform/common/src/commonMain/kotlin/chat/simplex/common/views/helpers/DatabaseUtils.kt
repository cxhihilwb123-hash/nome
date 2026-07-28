package chat.simplex.common.views.helpers

import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.res.MR
import kotlinx.serialization.*
import java.io.File
import java.security.SecureRandom

object DatabaseUtils {
  private val appPreferences: AppPreferences = ChatController.appPrefs

  private const val DATABASE_PASSWORD_ALIAS: String = "databasePassword"
  private const val APP_PASSWORD_ALIAS: String = "appPassword"
  private const val SELF_DESTRUCT_PASSWORD_ALIAS: String = "selfDestructPassword"
  private const val WEBRTC_ICE_SERVERS_ALIAS: String = "webrtcIceServers"

  val ksDatabasePassword = KeyStoreItem(DATABASE_PASSWORD_ALIAS, appPreferences.encryptedDBPassphrase, appPreferences.initializationVectorDBPassphrase)
  val ksAppPassword = KeyStoreItem(APP_PASSWORD_ALIAS, appPreferences.encryptedAppPassphrase, appPreferences.initializationVectorAppPassphrase)
  val ksSelfDestructPassword = KeyStoreItem(SELF_DESTRUCT_PASSWORD_ALIAS, appPreferences.encryptedSelfDestructPassphrase, appPreferences.initializationVectorSelfDestructPassphrase)
  val ksWebrtcIceServers = KeyStoreItem(WEBRTC_ICE_SERVERS_ALIAS, appPreferences.webrtcIceServers, appPreferences.webrtcIceServersIV)

  class KeyStoreItem(private val alias: String, val passphrase: SharedPreference<String?>, val initVector: SharedPreference<String?>) {
    fun get(): String? {
      return cryptor.decryptData(
        passphrase.get()?.toByteArrayFromBase64ForPassphrase() ?: return null,
        initVector.get()?.toByteArrayFromBase64ForPassphrase() ?: return null,
        alias,
      )
    }

    fun set(key: String) {
      val data = cryptor.encryptText(key, alias)
      passphrase.set(data.first.toBase64StringForPassphrase())
      initVector.set(data.second.toBase64StringForPassphrase())
    }

    fun setAndVerify(key: String) {
      set(key)
      if (get() != key) {
        throw CredentialErasureException("Credential could not be read back after replacement")
      }
    }

    fun remove() {
      removeAndVerify()
    }

    /**
     * Crypto-erasure is complete only when the platform key deletion succeeds and both persisted
     * ciphertext markers read back as absent. [SharedPreference.set] intentionally absorbs most
     * settings backend failures, so readback is required here.
     */
    fun removeAndVerify() {
      removeCredentialMaterialVerified(
        deletePlatformKey = { cryptor.deleteKey(alias) },
        clearPassphrase = { passphrase.set(null) },
        clearInitVector = { initVector.set(null) },
        readPassphrase = passphrase.get,
        readInitVector = initVector.get,
      )
    }
  }

  fun wipeDatabaseStorageVerified() {
    // Crypto-erasure first: an interrupted physical wipe cannot make remaining encrypted pages
    // readable. The physical pass still has to succeed before the transaction is considered done.
    ksDatabasePassword.removeAndVerify()
    wipeChatStorageFilesVerified()
  }

  fun setSelfDestructWipeStateVerified(state: SelfDestructWipeState) {
    setPreferenceVerified(appPreferences.selfDestructWipeState, state, "self-destruct wipe state")
  }

  internal fun markSelfDestructWipeIncomplete(cause: Throwable): SelfDestructWipeIncompleteException {
    val failure = SelfDestructWipeIncompleteException(cause)
    try {
      setSelfDestructWipeStateVerified(SelfDestructWipeState.INCOMPLETE)
    } catch (markerFailure: Throwable) {
      failure.addSuppressed(markerFailure)
    }
    return failure
  }

  /**
   * A crash while deleting must never fall through to opening the old database. Startup retries
   * the complete bounded wipe before native database initialization; a repeated failure throws a
   * distinct fatal exception and leaves the durable INCOMPLETE marker in place.
   */
  fun resumeIncompleteSelfDestructWipeIfNeeded() {
    runSelfDestructRecoveryTransaction(
      readState = appPreferences.selfDestructWipeState.get,
      transition = ::setSelfDestructWipeStateVerified,
      eraseDatabase = ::wipeDatabaseStorageVerified,
      eraseAppCredential = ksAppPassword::removeAndVerify,
      eraseSelfDestructCredential = ksSelfDestructPassword::removeAndVerify,
      resetAuthentication = {
        setPreferenceVerified(appPreferences.selfDestruct, false, "self-destruct enabled")
        setPreferenceVerified(appPreferences.selfDestructDisplayName, null, "self-destruct display name")
        setPreferenceVerified(appPreferences.performLA, false, "local authentication enabled")
        setPreferenceVerified(appPreferences.localAuthRetryAfterEpochMs, 0L, "local auth retry time")
        setPreferenceVerified(appPreferences.localAuthFailedAttempts, 0, "local auth failure count")
        setPreferenceVerified(appPreferences.newDatabaseInitialized, false, "database initialized state")
        setPreferenceVerified(appPreferences.storeDBPassphrase, true, "database key storage state")
      },
    )
  }

  fun hasAtLeastOneDatabase(rootDir: String): Boolean =
    File(rootDir + File.separator + chatDatabaseFileName).exists() || File(rootDir + File.separator + agentDatabaseFileName).exists()

  fun hasOnlyOneDatabase(rootDir: String): Boolean =
    File(rootDir + File.separator + chatDatabaseFileName).exists() != File(rootDir + File.separator + agentDatabaseFileName).exists()

  fun useDatabaseKey(): String {
    Log.d(TAG, "useDatabaseKey ${appPreferences.storeDBPassphrase.get()}")
    var dbKey = ""
    val useKeychain = appPreferences.storeDBPassphrase.get()
    if (useKeychain) {
      if (!hasAtLeastOneDatabase(dataDir.absolutePath)) {
        dbKey = randomDatabasePassword()
        ksDatabasePassword.set(dbKey)
        appPreferences.initialRandomDBPassphrase.set(true)
      } else {
        dbKey = ksDatabasePassword.get() ?: ""
      }
    } else if (appPlatform.isDesktop && !hasAtLeastOneDatabase(dataDir.absolutePath)) {
      // In case of database was deleted by hand
      dbKey = randomDatabasePassword()
      ksDatabasePassword.set(dbKey)
      appPreferences.initialRandomDBPassphrase.set(true)
      appPreferences.storeDBPassphrase.set(true)
    }
    return dbKey
  }

  fun randomDatabasePassword(): String {
    val s = ByteArray(32)
    SecureRandom().nextBytes(s)
    return s.toBase64StringForPassphrase().replace("\n", "")
  }
}

internal class CredentialErasureException(message: String, cause: Throwable? = null) :
  IllegalStateException(message, cause)

internal class SelfDestructWipeIncompleteException(cause: Throwable) :
  IllegalStateException("Self-destruct wipe is incomplete; recovery is required", cause)

internal fun <T> setPreferenceVerified(
  preference: SharedPreference<T>,
  value: T,
  fieldName: String,
) {
  preference.set(value)
  if (preference.get() != value) {
    throw IllegalStateException("Unable to persist $fieldName")
  }
}

internal fun runSelfDestructRecoveryTransaction(
  readState: () -> SelfDestructWipeState,
  transition: (SelfDestructWipeState) -> Unit,
  eraseDatabase: () -> Unit,
  eraseAppCredential: () -> Unit,
  eraseSelfDestructCredential: () -> Unit,
  resetAuthentication: () -> Unit,
) {
  if (readState() == SelfDestructWipeState.IDLE) return
  try {
    transition(SelfDestructWipeState.IN_PROGRESS)
    eraseDatabase()
    eraseAppCredential()
    eraseSelfDestructCredential()
    resetAuthentication()
    transition(SelfDestructWipeState.IDLE)
  } catch (e: Throwable) {
    val failure = SelfDestructWipeIncompleteException(e)
    try {
      transition(SelfDestructWipeState.INCOMPLETE)
    } catch (markerFailure: Throwable) {
      failure.addSuppressed(markerFailure)
    }
    throw failure
  }
}

internal fun removeCredentialMaterialVerified(
  deletePlatformKey: () -> Unit,
  clearPassphrase: () -> Unit,
  clearInitVector: () -> Unit,
  readPassphrase: () -> String?,
  readInitVector: () -> String?,
) {
  try {
    // Delete the platform key first. If the process stops afterwards, any remaining ciphertext is
    // already unusable; the preference markers contain no plaintext credential.
    deletePlatformKey()
    clearPassphrase()
    clearInitVector()
    if (readPassphrase() != null || readInitVector() != null) {
      throw CredentialErasureException("Credential metadata remained after key deletion")
    }
  } catch (e: CredentialErasureException) {
    throw e
  } catch (e: Throwable) {
    throw CredentialErasureException("Unable to erase credential material", e)
  }
}

// Spec: spec/database.md#DBMigrationResult
@Serializable
sealed class DBMigrationResult {
  @Serializable @SerialName("ok") object OK: DBMigrationResult()
  @Serializable @SerialName("invalidConfirmation") object InvalidConfirmation: DBMigrationResult()
  @Serializable @SerialName("errorNotADatabase") data class ErrorNotADatabase(val dbFile: String): DBMigrationResult()
  @Serializable @SerialName("errorMigration") data class ErrorMigration(val dbFile: String, val migrationError: MigrationError): DBMigrationResult()
  @Serializable @SerialName("errorSQL") data class ErrorSQL(val dbFile: String, val migrationSQLError: String): DBMigrationResult()
  @Serializable @SerialName("errorKeychain") object ErrorKeychain: DBMigrationResult()
  @Serializable @SerialName("unknown") data class Unknown(val json: String): DBMigrationResult()
}

enum class MigrationConfirmation(val value: String) {
  YesUp("yesUp"),
  YesUpDown ("yesUpDown"),
  Error("error")
}

fun defaultMigrationConfirmation(appPrefs: AppPreferences): MigrationConfirmation =
  if (appPrefs.developerTools.get() && appPrefs.confirmDBUpgrades.get()) MigrationConfirmation.Error else MigrationConfirmation.YesUp

@Serializable
sealed class MigrationError {
  @Serializable @SerialName("upgrade") class Upgrade(val upMigrations: List<UpMigration>): MigrationError()
  @Serializable @SerialName("downgrade") class Downgrade(val downMigrations: List<String>): MigrationError()
  @Serializable @SerialName("migrationError") class Error(val mtrError: MTRError): MigrationError()
}

@Serializable
data class UpMigration(
  val upName: String,
  // val withDown: Boolean
)

fun downMigrationWarnings(downMigrations: List<String>): List<String> {
  val warnings = listOf(
    "20260222_chat_relays" to MR.strings.down_migration_warning_chat_relays
  )
  return warnings.mapNotNull { (key, res) ->
    if (downMigrations.contains(key)) generalGetString(res) else null
  }
}

@Serializable
sealed class MTRError {
  @Serializable @SerialName("noDown") class NoDown(val dbMigrations: List<String>): MTRError()
  @Serializable @SerialName("different") class Different(val appMigration: String, val dbMigration: String): MTRError()
}
