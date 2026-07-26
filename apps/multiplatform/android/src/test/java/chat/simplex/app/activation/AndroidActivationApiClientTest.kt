package chat.simplex.app.activation

import chat.simplex.common.activation.ActivationEntitlementStatus
import chat.simplex.common.activation.ActivationHttpRequest
import chat.simplex.common.activation.ActivationHttpResponse
import chat.simplex.common.activation.ActivationHttpTransport
import chat.simplex.common.activation.ActivationPolicyMode
import chat.simplex.common.activation.AndroidActivationApiClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidActivationApiClientTest {
  @Test
  fun policyUsesFrozenAndroidBuildContract() = runBlocking {
    val fake = FakeTransport(
      """{
        "schemaVersion":1,
        "revision":12,
        "platform":"android",
        "build":235,
        "platformEnabled":true,
        "minimumBuild":200,
        "supported":true,
        "mode":"enforced",
        "audience":"new_installations",
        "effectiveAt":"2026-07-27T00:00:00Z",
        "refreshIntervalSeconds":300,
        "serverTime":"2026-07-27T12:00:00Z"
      }""",
    )
    val policy = AndroidActivationApiClient(fake, "https://example.test").fetchPolicy(235)

    assertEquals(ActivationPolicyMode.ENFORCED, policy.mode)
    assertEquals("GET", fake.lastRequest?.method)
    assertEquals(
      "https://example.test/api/v1/activation-policy?platform=android&build=235",
      fake.lastRequest?.url,
    )
  }

  @Test
  fun redeemSendsPlatformInstallationAndIdempotencyWithoutLoggingSecrets() = runBlocking {
    val fake = FakeTransport(
      """{
        "activationToken":"signed-token",
        "expiresAt":"2026-07-28T12:00:00Z",
        "graceUntil":"2026-08-04T12:00:00Z",
        "status":"active",
        "idempotent":false
      }""",
    )
    val grant = AndroidActivationApiClient(fake, "https://example.test").redeem(
      inviteCode = "INVITE-123",
      installationId = "install-456",
      appVersion = "6.4.0",
      idempotencyKey = "idem-789",
    )

    assertEquals(ActivationEntitlementStatus.ACTIVE, grant.status)
    assertEquals("signed-token", grant.token)
    assertEquals("POST", fake.lastRequest?.method)
    assertEquals("https://example.test/api/v1/activations/redeem", fake.lastRequest?.url)
    assertEquals("idem-789", fake.lastRequest?.headers?.get("Idempotency-Key"))
    val body = fake.lastRequest?.body.orEmpty()
    assertTrue(body.contains("\"platform\":\"android\""))
    assertTrue(body.contains("\"installationId\":\"install-456\""))
    assertTrue(body.contains("\"inviteCode\":\"INVITE-123\""))
    assertFalse(fake.lastRequest?.headers.orEmpty().containsKey("Authorization"))
  }

  @Test
  fun statusSurfacesLiveRevocation() = runBlocking {
    val fake = FakeTransport(
      """{
        "status":"revoked",
        "entitlementEnd":null,
        "offlineGraceUntil":"2026-08-04T12:00:00Z",
        "serverTime":"2026-07-27T12:00:00Z"
      }""",
    )
    val status = AndroidActivationApiClient(fake, "https://example.test").status("signed-token")

    assertEquals(ActivationEntitlementStatus.REVOKED, status.status)
    assertEquals("Bearer signed-token", fake.lastRequest?.headers?.get("Authorization"))
    assertEquals("GET", fake.lastRequest?.method)
    assertEquals("https://example.test/api/v1/activations/status", fake.lastRequest?.url)
  }

  @Test
  fun refreshUsesPluralActivationRouteAndActivationTokenResponse() = runBlocking {
    val fake = FakeTransport(
      """{
        "activationToken":"refreshed-token",
        "expiresAt":"2026-07-28T12:00:00Z",
        "graceUntil":"2026-08-04T12:00:00Z",
        "status":"active"
      }""",
    )

    val grant = AndroidActivationApiClient(fake, "https://example.test").refresh("signed-token")

    assertEquals("refreshed-token", grant.token)
    assertEquals("POST", fake.lastRequest?.method)
    assertEquals("https://example.test/api/v1/activations/refresh", fake.lastRequest?.url)
    assertEquals("Bearer signed-token", fake.lastRequest?.headers?.get("Authorization"))
    assertEquals(null, fake.lastRequest?.body)
  }

  @Test
  fun migrateUsesPluralActivationRouteAndAndroidPlatform() = runBlocking {
    val fake = FakeTransport(
      """{
        "activationToken":"migrated-token",
        "expiresAt":"2026-07-28T12:00:00Z",
        "graceUntil":"2026-08-04T12:00:00Z",
        "status":"active",
        "migrated":true,
        "idempotent":false
      }""",
    )

    val grant = AndroidActivationApiClient(fake, "https://example.test").migrate(
      token = "signed-token",
      newInstallationId = "00000000-0000-0000-0000-000000000002",
      appVersion = "6.4.0",
    )

    assertEquals("migrated-token", grant.token)
    assertEquals("POST", fake.lastRequest?.method)
    assertEquals("https://example.test/api/v1/activations/migrate", fake.lastRequest?.url)
    assertEquals("Bearer signed-token", fake.lastRequest?.headers?.get("Authorization"))
    val body = fake.lastRequest?.body.orEmpty()
    assertTrue(body.contains("\"platform\":\"android\""))
    assertTrue(body.contains("\"newInstallationId\":\"00000000-0000-0000-0000-000000000002\""))
  }

  private class FakeTransport(private val body: String): ActivationHttpTransport {
    var lastRequest: ActivationHttpRequest? = null

    override suspend fun execute(request: ActivationHttpRequest): ActivationHttpResponse {
      lastRequest = request
      return ActivationHttpResponse(200, body)
    }
  }
}
