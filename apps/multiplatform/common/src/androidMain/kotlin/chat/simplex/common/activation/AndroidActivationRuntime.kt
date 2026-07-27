package chat.simplex.common.activation

import android.content.Context
import android.app.backup.BackupManager
import android.provider.Settings
import android.util.Base64
import chat.simplex.common.BuildConfigCommon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

private const val DEFAULT_ACTIVATION_API_BASE_URL = "https://nome.im"

data class ActivationHttpRequest(
  val method: String,
  val url: String,
  val headers: Map<String, String> = emptyMap(),
  val body: String? = null,
)

data class ActivationHttpResponse(
  val statusCode: Int,
  val body: String,
)

fun interface ActivationHttpTransport {
  suspend fun execute(request: ActivationHttpRequest): ActivationHttpResponse
}

class UrlConnectionActivationTransport(
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val connectTimeoutMillis: Int = 10_000,
  private val readTimeoutMillis: Int = 15_000,
): ActivationHttpTransport {
  override suspend fun execute(request: ActivationHttpRequest): ActivationHttpResponse =
    withContext(dispatcher) {
      val connection = URL(request.url).openConnection() as HttpURLConnection
      try {
        connection.requestMethod = request.method
        connection.connectTimeout = connectTimeoutMillis
        connection.readTimeout = readTimeoutMillis
        connection.useCaches = false
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        request.headers.forEach(connection::setRequestProperty)
        if (request.body != null) {
          connection.doOutput = true
          connection.setRequestProperty("Content-Type", "application/json")
          connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(request.body) }
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        ActivationHttpResponse(status, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty())
      } finally {
        connection.disconnect()
      }
    }
}

class ActivationApiException(
  val code: String,
  override val message: String,
  val statusCode: Int? = null,
): Exception(message)

@Serializable
private data class RedeemRequest(
  val inviteCode: String,
  val installationId: String,
  val restoreBindingId: String,
  val platform: String,
  val appVersion: String,
)

@Serializable
private data class RecoverRequest(
  val installationId: String,
  val restoreBindingId: String,
  val platform: String,
  val appVersion: String,
)

@Serializable
private data class MigrateRequest(
  val newInstallationId: String,
  val restoreBindingId: String,
  val appVersion: String,
  val platform: String,
)

@Serializable
private data class ActivationTokenResponse(
  val activationToken: String,
  val expiresAt: Instant,
  val entitlementEnd: Instant? = null,
  val graceUntil: Instant? = null,
  val offlineGraceUntil: Instant? = null,
  val status: ActivationEntitlementStatus,
  val migrated: Boolean? = null,
  val idempotent: Boolean? = null,
)

@Serializable
private data class ActivationStatusResponse(
  val status: ActivationEntitlementStatus,
  val entitlementEnd: Instant? = null,
  val expiresAt: Instant? = null,
  val graceUntil: Instant? = null,
  val offlineGraceUntil: Instant? = null,
  val serverTime: Instant,
)

@Serializable
private data class RecoveryRegistrationResponse(val registered: Boolean)

@Serializable
private data class ErrorEnvelope(val error: ErrorBody)

@Serializable
private data class ErrorBody(
  val code: String,
  val message: String,
  val requestId: String? = null,
)

data class ActivationTokenGrant(
  val token: String,
  val expiresAt: Instant,
  val entitlementEnd: Instant?,
  val offlineGraceUntil: Instant?,
  val status: ActivationEntitlementStatus,
)

data class ActivationStatusGrant(
  val status: ActivationEntitlementStatus,
  val entitlementEnd: Instant?,
  val tokenExpiresAt: Instant?,
  val offlineGraceUntil: Instant?,
  val serverTime: Instant,
)

class AndroidActivationApiClient(
  private val transport: ActivationHttpTransport,
  apiBaseUrl: String = DEFAULT_ACTIVATION_API_BASE_URL,
  private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false },
) {
  private val baseUrl = apiBaseUrl.trimEnd('/')

  suspend fun fetchPolicy(build: Int): ActivationPolicy =
    execute(
      ActivationHttpRequest(
        method = "GET",
        url = "$baseUrl/api/v1/activation-policy?platform=android&build=$build",
      ),
    ) { json.decodeFromString(it) }

  suspend fun redeem(
    inviteCode: String,
    installationId: String,
    restoreBindingId: String,
    appVersion: String,
    idempotencyKey: String,
  ): ActivationTokenGrant {
    val response = execute(
      ActivationHttpRequest(
        method = "POST",
        url = "$baseUrl/api/v1/activations/redeem",
        headers = mapOf("Idempotency-Key" to idempotencyKey),
        body = json.encodeToString(
          RedeemRequest(inviteCode, installationId, restoreBindingId, platform = "android", appVersion = appVersion),
        ),
      ),
    ) { json.decodeFromString<ActivationTokenResponse>(it) }
    return response.toGrant()
  }

  suspend fun status(token: String): ActivationStatusGrant {
    val response = execute(
      ActivationHttpRequest(
        method = "GET",
        url = "$baseUrl/api/v1/activations/status",
        headers = mapOf("Authorization" to "Bearer $token"),
      ),
    ) { json.decodeFromString<ActivationStatusResponse>(it) }
    return ActivationStatusGrant(
      status = response.status,
      entitlementEnd = response.entitlementEnd,
      tokenExpiresAt = response.expiresAt,
      offlineGraceUntil = response.offlineGraceUntil ?: response.graceUntil,
      serverTime = response.serverTime,
    )
  }

  suspend fun refresh(token: String): ActivationTokenGrant {
    val response = execute(
      ActivationHttpRequest(
        method = "POST",
        url = "$baseUrl/api/v1/activations/refresh",
        headers = mapOf("Authorization" to "Bearer $token"),
      ),
    ) { json.decodeFromString<ActivationTokenResponse>(it) }
    return response.toGrant()
  }

  suspend fun recover(
    recoveryKey: String,
    installationId: String,
    restoreBindingId: String,
    appVersion: String,
  ): ActivationTokenGrant {
    val response = execute(
      ActivationHttpRequest(
        method = "POST",
        url = "$baseUrl/api/v1/activations/recover",
        headers = mapOf("Idempotency-Key" to recoveryKey),
        body = json.encodeToString(
          RecoverRequest(installationId, restoreBindingId, platform = "android", appVersion = appVersion),
        ),
      ),
    ) { json.decodeFromString<ActivationTokenResponse>(it) }
    return response.toGrant()
  }

  suspend fun registerRecovery(
    token: String,
    recoveryKey: String,
    installationId: String,
    restoreBindingId: String,
    appVersion: String,
  ) {
    val response = execute(
      ActivationHttpRequest(
        method = "POST",
        url = "$baseUrl/api/v1/activations/recovery",
        headers = mapOf(
          "Authorization" to "Bearer $token",
          "Idempotency-Key" to recoveryKey,
        ),
        body = json.encodeToString(
          RecoverRequest(installationId, restoreBindingId, platform = "android", appVersion = appVersion),
        ),
      ),
    ) { json.decodeFromString<RecoveryRegistrationResponse>(it) }
    if (!response.registered) {
      throw ActivationApiException("recovery_registration_failed", "Activation recovery was not registered")
    }
  }

  suspend fun migrate(
    token: String,
    newInstallationId: String,
    restoreBindingId: String,
    appVersion: String,
    recoveryKey: String,
  ): ActivationTokenGrant {
    val response = execute(
      ActivationHttpRequest(
        method = "POST",
        url = "$baseUrl/api/v1/activations/migrate",
        headers = mapOf(
          "Authorization" to "Bearer $token",
          "Idempotency-Key" to recoveryKey,
        ),
        body = json.encodeToString(
          MigrateRequest(newInstallationId, restoreBindingId, appVersion, platform = "android"),
        ),
      ),
    ) { json.decodeFromString<ActivationTokenResponse>(it) }
    return response.toGrant()
  }

  private suspend fun <T> execute(request: ActivationHttpRequest, decode: (String) -> T): T {
    val response = try {
      transport.execute(request)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      throw ActivationApiException("network_unavailable", "Activation service is unavailable")
    }
    if (response.statusCode !in 200..299) {
      val error = runCatching { json.decodeFromString<ErrorEnvelope>(response.body).error }.getOrNull()
      throw ActivationApiException(
        code = error?.code ?: "http_${response.statusCode}",
        message = error?.message ?: "Activation request failed",
        statusCode = response.statusCode,
      )
    }
    return try {
      decode(response.body)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      throw ActivationApiException("malformed_response", "Activation service returned an invalid response")
    }
  }

  private fun ActivationTokenResponse.toGrant(): ActivationTokenGrant {
    val grace = offlineGraceUntil ?: graceUntil
    if (activationToken.isBlank() || (grace != null && grace < expiresAt)) {
      throw ActivationApiException("malformed_response", "Activation service returned an invalid response")
    }
    return ActivationTokenGrant(
      token = activationToken,
      expiresAt = expiresAt,
      entitlementEnd = entitlementEnd,
      offlineGraceUntil = grace,
      status = status,
    )
  }
}

data class StoredActivationCredential(
  val token: String,
  val entitlement: ActivationEntitlement,
)

data class StoredActivationRestoreCredential(
  val recoveryKey: String,
  val installationId: String,
)

interface ActivationStorage {
  fun installationId(): String
  fun replaceInstallationId(newInstallationId: String)
  fun restoreBindingId(): String
  fun redeemIdempotencyKey(): String
  fun clearRedeemIdempotencyKey()
  fun migrationInstallationId(): String
  fun clearMigrationInstallationId()
  fun installationCohort(hasUsableLocalDatabase: Boolean): ActivationInstallationCohort
  fun readPolicy(): ActivationPolicy?
  fun writePolicy(policy: ActivationPolicy)
  fun readCredential(): StoredActivationCredential?
  fun writeCredential(credential: StoredActivationCredential)
  fun clearCredential()
  fun readRestoreCredential(): StoredActivationRestoreCredential?
  fun writeRestoreCredential(credential: StoredActivationRestoreCredential)
  fun clearRestoreCredential()
  fun lastPolicyRefreshAt(): Instant?
  fun setLastPolicyRefreshAt(at: Instant)
}

class AndroidActivationStorage(
  context: Context,
  private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false },
) : ActivationStorage {
  private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  private val restorePreferences = context.getSharedPreferences(RESTORE_PREFERENCES_NAME, Context.MODE_PRIVATE)
  private val backupManager = BackupManager(context)
  private val contentResolver = context.contentResolver
  private val secureTokenStore = AndroidKeyStoreTokenStore(preferences)

  override fun installationId(): String = synchronized(this) {
    preferences.getString(KEY_INSTALLATION_ID, null)?.takeIf { it.isNotBlank() }
      ?: UUID.randomUUID().toString().also {
        check(preferences.edit().putString(KEY_INSTALLATION_ID, it).commit())
      }
  }

  override fun replaceInstallationId(newInstallationId: String) {
    require(newInstallationId.isNotBlank())
    check(preferences.edit().putString(KEY_INSTALLATION_ID, newInstallationId).commit())
  }

  override fun restoreBindingId(): String =
    Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
      ?.takeIf { it.length in 8..256 }
      ?: installationId()

  override fun redeemIdempotencyKey(): String = synchronized(this) {
    preferences.getString(KEY_REDEEM_IDEMPOTENCY, null)?.takeIf { it.length in 16..200 }
      ?: UUID.randomUUID().toString().also {
        check(preferences.edit().putString(KEY_REDEEM_IDEMPOTENCY, it).commit())
      }
  }

  override fun clearRedeemIdempotencyKey() {
    check(preferences.edit().remove(KEY_REDEEM_IDEMPOTENCY).commit())
  }

  override fun migrationInstallationId(): String = synchronized(this) {
    preferences.getString(KEY_MIGRATION_INSTALLATION_ID, null)?.takeIf { it.length in 16..256 }
      ?: UUID.randomUUID().toString().also {
        check(preferences.edit().putString(KEY_MIGRATION_INSTALLATION_ID, it).commit())
      }
  }

  override fun clearMigrationInstallationId() {
    check(preferences.edit().remove(KEY_MIGRATION_INSTALLATION_ID).commit())
  }

  override fun installationCohort(hasUsableLocalDatabase: Boolean): ActivationInstallationCohort = synchronized(this) {
    val stored = preferences.getString(KEY_INSTALLATION_COHORT, null)
      ?.let(::decodeInstallationCohort)
    when {
      stored == ActivationInstallationCohort.GRANDFATHERED && !hasUsableLocalDatabase ->
        ActivationInstallationCohort.FRESH.also {
          check(preferences.edit().putString(KEY_INSTALLATION_COHORT, encodeInstallationCohort(it)).commit())
        }
      stored != null -> stored
      else ->
        (if (hasUsableLocalDatabase) ActivationInstallationCohort.GRANDFATHERED else ActivationInstallationCohort.FRESH)
          .also {
            check(preferences.edit().putString(KEY_INSTALLATION_COHORT, encodeInstallationCohort(it)).commit())
          }
    }
  }

  override fun readPolicy(): ActivationPolicy? =
    preferences.getString(KEY_POLICY, null)?.let { encoded ->
      runCatching { json.decodeFromString<ActivationPolicy>(encoded) }
        .getOrNull()
        ?.takeIf(::validPolicy)
    }

  override fun writePolicy(policy: ActivationPolicy) {
    require(validPolicy(policy))
    check(preferences.edit().putString(KEY_POLICY, json.encodeToString(policy)).commit())
  }

  override fun readCredential(): StoredActivationCredential? {
    val token = secureTokenStore.read() ?: return null
    val entitlement = preferences.getString(KEY_ENTITLEMENT, null)?.let { encoded ->
      runCatching { json.decodeFromString<ActivationEntitlement>(encoded) }.getOrNull()
    } ?: run {
      secureTokenStore.clear()
      return null
    }
    return StoredActivationCredential(token, entitlement)
  }

  override fun writeCredential(credential: StoredActivationCredential) {
    secureTokenStore.write(credential.token)
    if (!preferences.edit().putString(KEY_ENTITLEMENT, json.encodeToString(credential.entitlement)).commit()) {
      secureTokenStore.clear()
      error("Unable to persist activation entitlement")
    }
  }

  override fun clearCredential() {
    secureTokenStore.clear()
    check(preferences.edit().remove(KEY_ENTITLEMENT).commit())
  }

  override fun readRestoreCredential(): StoredActivationRestoreCredential? {
    val recoveryKey = restorePreferences.getString(KEY_RESTORE_RECOVERY_KEY, null)
      ?.takeIf { it.length in 16..200 }
      ?: return null
    val installationId = restorePreferences.getString(KEY_RESTORE_INSTALLATION_ID, null)
      ?.takeIf { it.length in 16..256 }
      ?: return null
    return StoredActivationRestoreCredential(recoveryKey, installationId)
  }

  override fun writeRestoreCredential(credential: StoredActivationRestoreCredential) {
    require(credential.recoveryKey.length in 16..200)
    require(credential.installationId.length in 16..256)
    check(
      restorePreferences.edit()
        .putString(KEY_RESTORE_RECOVERY_KEY, credential.recoveryKey)
        .putString(KEY_RESTORE_INSTALLATION_ID, credential.installationId)
        .commit(),
    )
    backupManager.dataChanged()
  }

  override fun clearRestoreCredential() {
    check(restorePreferences.edit().clear().commit())
    backupManager.dataChanged()
  }

  override fun lastPolicyRefreshAt(): Instant? =
    preferences.getString(KEY_POLICY_REFRESH_AT, null)?.let { runCatching { Instant.parse(it) }.getOrNull() }

  override fun setLastPolicyRefreshAt(at: Instant) {
    preferences.edit().putString(KEY_POLICY_REFRESH_AT, at.toString()).apply()
  }

  private fun validPolicy(policy: ActivationPolicy): Boolean =
    policy.schemaVersion == 1 &&
      policy.platform == ActivationPlatform.ANDROID &&
      policy.revision >= 0 &&
      policy.build >= 0 &&
      policy.minimumBuild >= 0 &&
      policy.refreshIntervalSeconds in 60..86_400

  private fun encodeInstallationCohort(cohort: ActivationInstallationCohort): String = when (cohort) {
    ActivationInstallationCohort.FRESH -> "fresh_install"
    ActivationInstallationCohort.GRANDFATHERED -> "preexisting_local_profile"
  }

  private fun decodeInstallationCohort(value: String): ActivationInstallationCohort? = when (value) {
    "fresh_install", ActivationInstallationCohort.FRESH.name -> ActivationInstallationCohort.FRESH
    "preexisting_local_profile", ActivationInstallationCohort.GRANDFATHERED.name -> ActivationInstallationCohort.GRANDFATHERED
    else -> null
  }

  private companion object {
    const val PREFERENCES_NAME = "nome_activation_v1"
    const val RESTORE_PREFERENCES_NAME = "nome_activation_restore_v1"
    const val KEY_INSTALLATION_ID = "installation_id"
    const val KEY_INSTALLATION_COHORT = "activation_gate_migration_v1"
    const val KEY_REDEEM_IDEMPOTENCY = "redeem_idempotency_key"
    const val KEY_MIGRATION_INSTALLATION_ID = "migration_installation_id"
    const val KEY_POLICY = "policy"
    const val KEY_ENTITLEMENT = "entitlement"
    const val KEY_POLICY_REFRESH_AT = "policy_refresh_at"
    const val KEY_RESTORE_RECOVERY_KEY = "recovery_key"
    const val KEY_RESTORE_INSTALLATION_ID = "installation_id"
  }
}

private class AndroidKeyStoreTokenStore(
  private val preferences: android.content.SharedPreferences,
) {
  fun write(token: String) {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
    check(preferences.edit()
      .putString(KEY_TOKEN_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
      .putString(KEY_TOKEN_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
      .commit())
  }

  fun read(): String? = try {
    val encrypted = preferences.getString(KEY_TOKEN_CIPHERTEXT, null) ?: return null
    val iv = preferences.getString(KEY_TOKEN_IV, null) ?: return null
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return null
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(
      Cipher.DECRYPT_MODE,
      key,
      GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
    )
    String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
  } catch (_: Throwable) {
    clear()
    null
  }

  fun clear() {
    check(preferences.edit().remove(KEY_TOKEN_CIPHERTEXT).remove(KEY_TOKEN_IV).commit())
  }

  private fun getOrCreateKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
    generator.init(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build(),
    )
    return generator.generateKey()
  }

  private companion object {
    const val ANDROID_KEY_STORE = "AndroidKeyStore"
    const val KEY_ALIAS = "nome_activation_token_v1"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val KEY_TOKEN_CIPHERTEXT = "token_ciphertext"
    const val KEY_TOKEN_IV = "token_iv"
  }
}

class AndroidActivationRuntime(
  private val storage: ActivationStorage,
  private val client: AndroidActivationApiClient,
  private val build: Int = BuildConfigCommon.ANDROID_VERSION_CODE,
  private val appVersion: String = BuildConfigCommon.ANDROID_VERSION_NAME,
  private val clock: Clock = Clock.System,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : ActivationRuntimeDelegate {
  private val operationMutex = Mutex()
  private val entitlementRefreshMutex = Mutex()
  private val wouldBlockCount = AtomicLong(0)
  @Volatile private var lastWouldBlockCapability: ActivationCapability? = null
  @Volatile private var policy: ActivationPolicy? = null
  @Volatile private var policyChecked = false
  @Volatile private var cohort = ActivationInstallationCohort.FRESH
  @Volatile private var credential: StoredActivationCredential? = null
  @Volatile private var restorationPending = false
  private var schedulerStarted = false

  fun initialize(hasUsableLocalDatabase: Boolean) {
    var storageUnavailable = false
    try {
      storage.installationId()
      cohort = storage.installationCohort(hasUsableLocalDatabase)
      policy = storage.readPolicy()
      policyChecked = policy != null
      credential = storage.readCredential()
      restorationPending = !hasUsableLocalDatabase &&
        (credential != null || storage.readRestoreCredential() != null)
    } catch (_: Throwable) {
      policy = null
      policyChecked = false
      credential = null
      storageUnavailable = true
    }
    ActivationGate.install(
      this,
      evaluate().copy(lastErrorCode = if (storageUnavailable) "secure_storage_unavailable" else null),
    )
    if (!schedulerStarted) {
      schedulerStarted = true
      scope.launch {
        recoverInstallationIfNeeded()
        registerRecoveryIfNeeded()
        refreshPolicy(force = false)
        refreshEntitlementIfDue(force = true)
        while (true) {
          val seconds = nextSchedulerDelaySeconds()
          delay(seconds * 1_000)
          recoverInstallationIfNeeded()
          registerRecoveryIfNeeded()
          refreshPolicy(force = true)
          refreshEntitlementIfDue(force = true)
          ActivationGate.publish(evaluate())
        }
      }
    }
  }

  override suspend fun refreshPolicy(force: Boolean): ActivationOperationResult = operationMutex.withLock {
    val now = clock.now()
    val refreshInterval = policy?.refreshIntervalSeconds ?: 300
    return@withLock try {
      val lastRefresh = runCatching { storage.lastPolicyRefreshAt() }.getOrNull()
      val refreshAge = lastRefresh?.let { now.epochSeconds - it.epochSeconds }
      if (!force && refreshAge != null && refreshAge in 0 until refreshInterval) {
        return@withLock ActivationOperationResult.Success(evaluate())
      }
      val fetched = client.fetchPolicy(build)
      validatePolicy(fetched)
      val currentRevision = policy?.revision
      if (currentRevision != null && fetched.revision < currentRevision) {
        throw ActivationApiException("stale_policy", "Activation policy revision is stale")
      }
      policy = fetched
      policyChecked = true
      storage.writePolicy(fetched)
      storage.setLastPolicyRefreshAt(now)
      val next = evaluate()
      ActivationGate.publish(next)
      ActivationOperationResult.Success(next)
    } catch (e: ActivationApiException) {
      val next = evaluate().copy(lastErrorCode = e.code)
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(e.code, e.message)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      val next = evaluate().copy(lastErrorCode = "invalid_policy")
      ActivationGate.publish(next)
      ActivationOperationResult.Failure("invalid_policy", "Activation policy is invalid")
    }
  }

  override suspend fun redeem(inviteCode: String): ActivationOperationResult = operationMutex.withLock {
    val code = inviteCode.trim()
    if (code.isEmpty()) {
      return@withLock ActivationOperationResult.Failure("invalid_invite_code", "Enter an invite code")
    }
    val previousCredential = credential
    val installationId = storage.installationId()
    val recoveryKey = storage.redeemIdempotencyKey()
    ActivationGate.publish(evaluate().copy(operationInProgress = true, lastErrorCode = null))
    return@withLock try {
      val grant = client.redeem(
        inviteCode = code,
        installationId = installationId,
        restoreBindingId = storage.restoreBindingId(),
        appVersion = appVersion,
        idempotencyKey = recoveryKey,
      )
      applyGrant(grant)
      storage.writeRestoreCredential(StoredActivationRestoreCredential(recoveryKey, installationId))
      restorationPending = false
      runCatching { storage.clearRedeemIdempotencyKey() }
      val next = evaluate()
      ActivationGate.publish(next)
      ActivationGate.dismissActivation()
      ActivationOperationResult.Success(next)
    } catch (e: ActivationApiException) {
      val next = evaluate().copy(lastErrorCode = e.code)
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(e.code, e.message)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      val restored = runCatching {
        if (previousCredential == null) storage.clearCredential() else storage.writeCredential(previousCredential)
      }.isSuccess
      credential = previousCredential.takeIf { restored }
      val next = evaluate().copy(lastErrorCode = "secure_storage_unavailable")
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(
        "secure_storage_unavailable",
        "Secure activation storage is unavailable",
      )
    }
  }

  override suspend fun refreshEntitlement(): ActivationOperationResult = operationMutex.withLock {
    val current = credential
      ?: return@withLock ActivationOperationResult.Failure("not_activated", "This installation is not activated")
    return@withLock try {
      val expiresAt = current.entitlement.tokenExpiresAt
      if (expiresAt == null || expiresAt <= clock.now()) {
        applyGrant(client.refresh(current.token))
      } else {
        val status = try {
          client.status(current.token)
        } catch (e: ActivationApiException) {
          if (e.statusCode == 401 && e.code.equals("ACTIVATION_TOKEN_EXPIRED", ignoreCase = true)) {
            applyGrant(client.refresh(current.token))
            null
          } else {
            throw e
          }
        }
        if (status != null) {
          val entitlement = current.entitlement.copy(
            status = status.status,
            entitlementEnd = status.entitlementEnd ?: current.entitlement.entitlementEnd,
            tokenExpiresAt = status.tokenExpiresAt ?: current.entitlement.tokenExpiresAt,
            offlineGraceUntil = status.offlineGraceUntil ?: current.entitlement.offlineGraceUntil,
            lastServerCheckAt = status.serverTime,
          )
          credential = current.copy(entitlement = entitlement)
          storage.writeCredential(credential!!)
          restorationPending = false
          if (
            entitlement.status == ActivationEntitlementStatus.ACTIVE &&
            expiresAt.epochSeconds - clock.now().epochSeconds <= 3_600
          ) {
            applyGrant(client.refresh(current.token))
          }
        }
      }
      val next = evaluate()
      ActivationGate.publish(next)
      ActivationOperationResult.Success(next)
    } catch (e: ActivationApiException) {
      if (e.statusCode == 401 || e.statusCode == 403 || e.statusCode == 404) {
        return@withLock try {
          val deniedStatus = if (e.code.contains("EXPIRED", ignoreCase = true)) {
            ActivationEntitlementStatus.EXPIRED
          } else {
            ActivationEntitlementStatus.REVOKED
          }
          credential = current.copy(
            entitlement = current.entitlement.copy(
              status = deniedStatus,
              lastServerCheckAt = clock.now(),
            ),
          )
          storage.writeCredential(credential!!)
          restorationPending = false
          val next = evaluate().copy(lastErrorCode = e.code)
          ActivationGate.publish(next)
          ActivationOperationResult.Failure(e.code, e.message)
        } catch (_: Throwable) {
          credential = null
          runCatching { storage.clearCredential() }
          val next = evaluate().copy(lastErrorCode = "secure_storage_unavailable")
          ActivationGate.publish(next)
          ActivationOperationResult.Failure(
            "secure_storage_unavailable",
            "Secure activation storage is unavailable",
          )
        }
      }
      val next = evaluate().copy(lastErrorCode = e.code)
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(e.code, e.message)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      credential = null
      runCatching { storage.clearCredential() }
      val next = evaluate().copy(lastErrorCode = "secure_storage_unavailable")
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(
        "secure_storage_unavailable",
        "Secure activation storage is unavailable",
      )
    }
  }

  override suspend fun refreshBeforeProtectedAction() {
    refreshPolicy(force = false)
    refreshEntitlementIfDue(force = false)
    ActivationGate.publish(evaluate())
  }

  suspend fun recoverInstallation(): ActivationOperationResult? =
    recoverInstallationIfNeeded()

  suspend fun registerRecovery(): ActivationOperationResult? =
    registerRecoveryIfNeeded()

  override suspend fun migrateInstallation(): ActivationOperationResult = operationMutex.withLock {
    val current = credential
      ?: return@withLock ActivationOperationResult.Failure("not_activated", "This installation is not activated")
    return@withLock try {
      val newInstallationId = storage.migrationInstallationId()
      val recoveryKey = storage.readRestoreCredential()?.recoveryKey
        ?: storage.redeemIdempotencyKey()
      val grant = client.migrate(
        current.token,
        newInstallationId,
        storage.restoreBindingId(),
        appVersion,
        recoveryKey,
      )
      storage.replaceInstallationId(newInstallationId)
      applyGrant(grant)
      storage.writeRestoreCredential(
        StoredActivationRestoreCredential(recoveryKey, newInstallationId),
      )
      runCatching { storage.clearMigrationInstallationId() }
      val next = evaluate()
      ActivationGate.publish(next)
      ActivationOperationResult.Success(next)
    } catch (e: ActivationApiException) {
      val next = evaluate().copy(lastErrorCode = e.code)
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(e.code, e.message)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      credential = null
      runCatching { storage.clearCredential() }
      val next = evaluate().copy(lastErrorCode = "secure_storage_unavailable")
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(
        "secure_storage_unavailable",
        "Secure activation storage is unavailable",
      )
    }
  }

  override fun recordWouldBlock(capability: ActivationCapability) {
    val count = wouldBlockCount.incrementAndGet()
    lastWouldBlockCapability = capability
    ActivationGate.publish(
      ActivationGate.state.value.copy(
        observedWouldBlockCount = count,
        lastObservedWouldBlockCapability = capability,
      ),
    )
  }

  private fun applyGrant(grant: ActivationTokenGrant) {
    credential = StoredActivationCredential(
      token = grant.token,
      entitlement = ActivationEntitlement(
        status = grant.status,
        entitlementEnd = grant.entitlementEnd,
        tokenExpiresAt = grant.expiresAt,
        offlineGraceUntil = grant.offlineGraceUntil,
        lastServerCheckAt = clock.now(),
      ),
    )
    storage.writeCredential(credential!!)
    restorationPending = false
  }

  private suspend fun recoverInstallationIfNeeded(): ActivationOperationResult? = operationMutex.withLock {
    if (!restorationPending || credential != null) return@withLock null
    val restore = runCatching { storage.readRestoreCredential() }.getOrNull()
      ?: run {
        restorationPending = false
        return@withLock null
      }
    return@withLock try {
      val grant = client.recover(
        recoveryKey = restore.recoveryKey,
        installationId = restore.installationId,
        restoreBindingId = storage.restoreBindingId(),
        appVersion = appVersion,
      )
      storage.replaceInstallationId(restore.installationId)
      applyGrant(grant)
      val next = evaluate()
      ActivationGate.publish(next)
      ActivationOperationResult.Success(next)
    } catch (e: ActivationApiException) {
      if (e.statusCode in listOf(400, 401, 403, 404, 409)) {
        restorationPending = false
        runCatching { storage.clearRestoreCredential() }
      }
      val next = evaluate().copy(lastErrorCode = e.code)
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(e.code, e.message)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      val next = evaluate().copy(lastErrorCode = "secure_storage_unavailable")
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(
        "secure_storage_unavailable",
        "Secure activation storage is unavailable",
      )
    }
  }

  private suspend fun registerRecoveryIfNeeded(): ActivationOperationResult? = operationMutex.withLock {
    val current = credential ?: return@withLock null
    if (storage.readRestoreCredential() != null) return@withLock null
    val installationId = storage.installationId()
    val recoveryKey = storage.redeemIdempotencyKey()
    return@withLock try {
      client.registerRecovery(
        token = current.token,
        recoveryKey = recoveryKey,
        installationId = installationId,
        restoreBindingId = storage.restoreBindingId(),
        appVersion = appVersion,
      )
      storage.writeRestoreCredential(
        StoredActivationRestoreCredential(recoveryKey, installationId),
      )
      runCatching { storage.clearRedeemIdempotencyKey() }
      val next = evaluate()
      ActivationGate.publish(next)
      ActivationOperationResult.Success(next)
    } catch (e: ActivationApiException) {
      val next = evaluate().copy(lastErrorCode = e.code)
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(e.code, e.message)
    } catch (e: CancellationException) {
      throw e
    } catch (_: Throwable) {
      val next = evaluate().copy(lastErrorCode = "secure_storage_unavailable")
      ActivationGate.publish(next)
      ActivationOperationResult.Failure(
        "secure_storage_unavailable",
        "Secure activation storage is unavailable",
      )
    }
  }

  private suspend fun refreshEntitlementIfDue(force: Boolean): ActivationOperationResult? =
    entitlementRefreshMutex.withLock {
      val current = credential ?: return@withLock null
      val interval = policy?.refreshIntervalSeconds ?: 300
      val checkedAt = current.entitlement.lastServerCheckAt
      val age = checkedAt?.let { clock.now().epochSeconds - it.epochSeconds }
      if (!force && age != null && age in 0 until interval) {
        return@withLock ActivationOperationResult.Success(evaluate())
      }
      refreshEntitlement()
    }

  private fun nextSchedulerDelaySeconds(): Long {
    val now = clock.now().epochSeconds
    val delays = mutableListOf(policy?.refreshIntervalSeconds?.coerceIn(60, 86_400) ?: 300)
    credential?.entitlement?.let { entitlement ->
      entitlement.tokenExpiresAt?.let { delays += (it.epochSeconds - now - 3_600).coerceAtLeast(60) }
      entitlement.offlineGraceUntil?.let { delays += (it.epochSeconds - now).coerceAtLeast(60) }
      entitlement.entitlementEnd?.let { delays += (it.epochSeconds - now).coerceAtLeast(60) }
    }
    return delays.minOrNull() ?: 300
  }

  private fun evaluate(): ActivationRuntimeState {
    val evaluated = ActivationPolicyEvaluator.evaluate(
      policy = policy,
      policyChecked = policyChecked,
      cohort = cohort,
      entitlement = credential?.entitlement ?: ActivationEntitlement(),
      now = clock.now(),
    ).copy(
      observedWouldBlockCount = wouldBlockCount.get(),
      lastObservedWouldBlockCapability = lastWouldBlockCapability,
    )
    return if (restorationPending) {
      evaluated.copy(
        access = ActivationAccess.LOCAL_ONLY,
        reason = "restore_validation_pending",
        wouldBlockInEnforcedMode = false,
      )
    } else {
      evaluated
    }
  }

  private fun validatePolicy(value: ActivationPolicy) {
    require(value.schemaVersion == 1)
    require(value.platform == ActivationPlatform.ANDROID)
    require(value.build == build)
    require(value.revision >= 0)
    require(value.minimumBuild >= 0)
    require(value.refreshIntervalSeconds in 60..86_400)
  }

  companion object {
    fun create(
      context: Context,
      apiBaseUrl: String = DEFAULT_ACTIVATION_API_BASE_URL,
      transport: ActivationHttpTransport = UrlConnectionActivationTransport(),
    ): AndroidActivationRuntime = AndroidActivationRuntime(
      storage = AndroidActivationStorage(context),
      client = AndroidActivationApiClient(transport, apiBaseUrl),
    )
  }
}
