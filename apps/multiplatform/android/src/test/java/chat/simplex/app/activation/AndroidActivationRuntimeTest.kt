package chat.simplex.app.activation

import chat.simplex.common.activation.ActivationAccess
import chat.simplex.common.activation.ActivationAudience
import chat.simplex.common.activation.ActivationEntitlement
import chat.simplex.common.activation.ActivationEntitlementStatus
import chat.simplex.common.activation.ActivationHttpRequest
import chat.simplex.common.activation.ActivationHttpResponse
import chat.simplex.common.activation.ActivationHttpTransport
import chat.simplex.common.activation.ActivationInstallationCohort
import chat.simplex.common.activation.ActivationOperationResult
import chat.simplex.common.activation.ActivationPlatform
import chat.simplex.common.activation.ActivationPolicy
import chat.simplex.common.activation.ActivationPolicyMode
import chat.simplex.common.activation.ActivationStorage
import chat.simplex.common.activation.AndroidActivationApiClient
import chat.simplex.common.activation.AndroidActivationRuntime
import chat.simplex.common.activation.ActivationGate
import chat.simplex.common.activation.StoredActivationCredential
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidActivationRuntimeTest {
  private val now = Instant.parse("2026-07-27T12:00:00Z")

  @Test
  fun expiredTokenRefreshesWithinGraceBeforeCallingStatus() = runBlocking {
    val transport = RoutingTransport { request ->
      when {
        request.url.endsWith("/api/v1/activations/refresh") -> ActivationHttpResponse(
          200,
          """{
            "activationToken":"refreshed-token",
            "expiresAt":"2026-07-28T12:00:00Z",
            "graceUntil":"2026-08-04T12:00:00Z",
            "status":"active"
          }""",
        )
        else -> error("Unexpected request: ${request.method} ${request.url}")
      }
    }
    val storage = MemoryActivationStorage(expiredCredential())
    val runtime = runtime(storage, transport)
    runtime.initialize(hasUsableLocalDatabase = true)

    val result = runtime.refreshEntitlement()

    assertTrue(result is ActivationOperationResult.Success)
    assertEquals(listOf("POST /api/v1/activations/refresh"), transport.requests.map { "${it.method} ${it.url.substringAfter("example.test")}" })
    assertEquals("refreshed-token", storage.credential?.token)
    assertEquals(ActivationAccess.FULL, ActivationGate.state.value.access)
  }

  @Test
  fun authoritativeRefreshDenialIsPersistedAndFailsClosed() = runBlocking {
    val transport = RoutingTransport {
      ActivationHttpResponse(
        403,
        """{"error":{"code":"ACTIVATION_NOT_ACTIVE","message":"Activation is paused, revoked, or expired.","requestId":"req-1"}}""",
      )
    }
    val storage = MemoryActivationStorage(expiredCredential())
    val runtime = runtime(storage, transport)
    runtime.initialize(hasUsableLocalDatabase = true)

    val result = runtime.refreshEntitlement()

    assertTrue(result is ActivationOperationResult.Failure)
    assertEquals(ActivationEntitlementStatus.REVOKED, storage.credential?.entitlement?.status)
    assertEquals(ActivationAccess.LOCAL_ONLY, ActivationGate.state.value.access)
  }

  @Test
  fun serverExpiredStatusFallsBackToRefreshWithinServerGrace() = runBlocking {
    val transport = RoutingTransport { request ->
      if (request.url.endsWith("/api/v1/activations/status")) {
        ActivationHttpResponse(
          401,
          """{"error":{"code":"ACTIVATION_TOKEN_EXPIRED","message":"Activation token is expired.","requestId":"req-2"}}""",
        )
      } else {
        ActivationHttpResponse(
          200,
          """{
            "activationToken":"clock-skew-refreshed-token",
            "expiresAt":"2026-07-28T12:00:00Z",
            "graceUntil":"2026-08-04T12:00:00Z",
            "status":"active"
          }""",
        )
      }
    }
    val credential = expiredCredential().copy(
      entitlement = expiredCredential().entitlement.copy(
        tokenExpiresAt = Instant.parse("2026-07-27T18:00:00Z"),
      ),
    )
    val storage = MemoryActivationStorage(credential)
    val runtime = runtime(storage, transport)
    runtime.initialize(hasUsableLocalDatabase = true)

    val result = runtime.refreshEntitlement()

    assertTrue(result is ActivationOperationResult.Success)
    assertEquals(
      listOf("GET /api/v1/activations/status", "POST /api/v1/activations/refresh"),
      transport.requests.map { "${it.method} ${it.url.substringAfter("example.test")}" },
    )
    assertEquals("clock-skew-refreshed-token", storage.credential?.token)
  }

  @Test
  fun olderPolicyRevisionCannotOverwriteCachedPolicy() = runBlocking {
    val transport = RoutingTransport {
      ActivationHttpResponse(
        200,
        """{
          "schemaVersion":1,
          "revision":6,
          "platform":"android",
          "build":235,
          "platformEnabled":true,
          "minimumBuild":200,
          "supported":true,
          "mode":"disabled",
          "audience":"new_installations",
          "effectiveAt":"2026-07-27T00:00:00Z",
          "refreshIntervalSeconds":300,
          "serverTime":"2026-07-27T12:00:00Z"
        }""",
      )
    }
    val runtime = runtime(MemoryActivationStorage(expiredCredential()), transport)
    runtime.initialize(hasUsableLocalDatabase = false)

    val result = runtime.refreshPolicy(force = true)

    assertTrue(result is ActivationOperationResult.Failure)
    assertEquals(7L, ActivationGate.state.value.policy?.revision)
    assertEquals(ActivationPolicyMode.ENFORCED, ActivationGate.state.value.policy?.mode)
  }

  @Test
  fun redeemRetryReusesIdempotencyKeyUntilCredentialIsStored() = runBlocking {
    var attempt = 0
    val transport = RoutingTransport {
      attempt += 1
      if (attempt == 1) throw java.io.IOException("simulated timeout")
      ActivationHttpResponse(
        201,
        """{
          "activationToken":"redeemed-token",
          "expiresAt":"2026-07-28T12:00:00Z",
          "graceUntil":"2026-08-04T12:00:00Z",
          "status":"active",
          "idempotent":true
        }""",
      )
    }
    val storage = MemoryActivationStorage(credential = null)
    val runtime = runtime(storage, transport)
    runtime.initialize(hasUsableLocalDatabase = false)

    assertTrue(runtime.redeem("INVITE-123") is ActivationOperationResult.Failure)
    assertTrue(runtime.redeem("INVITE-123") is ActivationOperationResult.Success)

    assertEquals(2, transport.requests.size)
    assertEquals(
      transport.requests[0].headers["Idempotency-Key"],
      transport.requests[1].headers["Idempotency-Key"],
    )
    assertTrue(storage.idempotencyCleared)
  }

  @Test
  fun pendingMigrationUsesStableReplacementInstallationAndRestoresAccess() = runBlocking {
    val transport = RoutingTransport {
      ActivationHttpResponse(
        200,
        """{
          "activationToken":"migrated-token",
          "expiresAt":"2026-07-28T12:00:00Z",
          "graceUntil":"2026-08-04T12:00:00Z",
          "status":"active",
          "migrated":true,
          "idempotent":false
        }""",
      )
    }
    val pending = expiredCredential().copy(
      entitlement = expiredCredential().entitlement.copy(
        status = ActivationEntitlementStatus.PENDING_MIGRATION,
      ),
    )
    val storage = MemoryActivationStorage(pending)
    val runtime = runtime(storage, transport)
    runtime.initialize(hasUsableLocalDatabase = true)
    assertEquals(ActivationAccess.MIGRATION_REQUIRED, ActivationGate.state.value.access)

    val result = runtime.migrateInstallation()

    assertTrue(result is ActivationOperationResult.Success)
    assertEquals("00000000-0000-0000-0000-000000000004", storage.installationId)
    assertEquals(ActivationAccess.FULL, ActivationGate.state.value.access)
    assertTrue(transport.requests.single().body.orEmpty().contains("\"platform\":\"android\""))
  }

  @Test
  fun initializeWithoutLocalDatabaseClearsPersistedCredentialAndFailsClosed() {
    val storage = MemoryActivationStorage(expiredCredential())
    val runtime = runtime(storage, RoutingTransport { error("unexpected network") })

    runtime.initialize(hasUsableLocalDatabase = false)

    assertEquals(1, storage.clearCredentialCalls)
    assertEquals(null, storage.credential)
    assertEquals(ActivationAccess.LOCAL_ONLY, ActivationGate.state.value.access)
  }

  private fun runtime(
    storage: MemoryActivationStorage,
    transport: ActivationHttpTransport,
  ) = AndroidActivationRuntime(
    storage = storage,
    client = AndroidActivationApiClient(transport, "https://example.test"),
    build = 235,
    appVersion = "6.4.0",
    clock = object : Clock {
      override fun now(): Instant = this@AndroidActivationRuntimeTest.now
    },
    scope = CoroutineScope(SupervisorJob() + NeverDispatcher),
  )

  private fun expiredCredential() = StoredActivationCredential(
    token = "expired-token",
    entitlement = ActivationEntitlement(
      status = ActivationEntitlementStatus.ACTIVE,
      tokenExpiresAt = Instant.parse("2026-07-27T11:00:00Z"),
      offlineGraceUntil = Instant.parse("2026-08-03T11:00:00Z"),
      lastServerCheckAt = Instant.parse("2026-07-27T10:00:00Z"),
    ),
  )

  private class MemoryActivationStorage(
    var credential: StoredActivationCredential?,
  ): ActivationStorage {
    private val policy = ActivationPolicy(
      schemaVersion = 1,
      revision = 7,
      platform = ActivationPlatform.ANDROID,
      build = 235,
      platformEnabled = true,
      minimumBuild = 200,
      supported = true,
      mode = ActivationPolicyMode.ENFORCED,
      audience = ActivationAudience.ALL_UNACTIVATED,
      effectiveAt = Instant.parse("2026-07-27T00:00:00Z"),
      refreshIntervalSeconds = 300,
      serverTime = Instant.parse("2026-07-27T12:00:00Z"),
    )
    var installationId = "00000000-0000-0000-0000-000000000001"
    private var refreshedAt = Instant.parse("2026-07-27T12:00:00Z")
    var idempotencyCleared = false
    var clearCredentialCalls = 0

    override fun installationId(): String = installationId
    override fun replaceInstallationId(newInstallationId: String) { installationId = newInstallationId }
    override fun redeemIdempotencyKey(): String = "00000000-0000-0000-0000-000000000003"
    override fun clearRedeemIdempotencyKey() { idempotencyCleared = true }
    override fun migrationInstallationId(): String = "00000000-0000-0000-0000-000000000004"
    override fun clearMigrationInstallationId() = Unit
    override fun installationCohort(hasUsableLocalDatabase: Boolean) = ActivationInstallationCohort.FRESH
    override fun readPolicy(): ActivationPolicy = policy
    override fun writePolicy(policy: ActivationPolicy) = Unit
    override fun readCredential(): StoredActivationCredential? = credential
    override fun writeCredential(credential: StoredActivationCredential) { this.credential = credential }
    override fun clearCredential() {
      clearCredentialCalls += 1
      credential = null
    }
    override fun lastPolicyRefreshAt(): Instant = refreshedAt
    override fun setLastPolicyRefreshAt(at: Instant) { refreshedAt = at }
  }

  private class RoutingTransport(
    private val respond: (ActivationHttpRequest) -> ActivationHttpResponse,
  ): ActivationHttpTransport {
    val requests = mutableListOf<ActivationHttpRequest>()

    override suspend fun execute(request: ActivationHttpRequest): ActivationHttpResponse {
      requests += request
      return respond(request)
    }
  }

  private object NeverDispatcher: CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) = Unit
  }
}
