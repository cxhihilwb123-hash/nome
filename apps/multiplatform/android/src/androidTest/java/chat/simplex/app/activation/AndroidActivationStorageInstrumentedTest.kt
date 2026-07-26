package chat.simplex.app.activation

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.common.activation.ActivationEntitlement
import chat.simplex.common.activation.ActivationEntitlementStatus
import chat.simplex.common.activation.ActivationInstallationCohort
import chat.simplex.common.activation.AndroidActivationStorage
import chat.simplex.common.activation.StoredActivationCredential
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidActivationStorageInstrumentedTest {
  @Test
  fun redeemIdempotencyKeyIsStableUntilSuccessfulRedeemClearsIt() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val storage = AndroidActivationStorage(context)
    val prefs = context.getSharedPreferences("nome_activation_v1", android.content.Context.MODE_PRIVATE)
    try {
      prefs.edit().remove("redeem_idempotency_key").commit()
      val first = storage.redeemIdempotencyKey()

      assertEquals(first, storage.redeemIdempotencyKey())
      storage.clearRedeemIdempotencyKey()
      assertNotEquals(first, storage.redeemIdempotencyKey())
    } finally {
      prefs.edit().remove("redeem_idempotency_key").commit()
    }
  }

  @Test
  fun migrationTargetIsStableUntilMigrationSucceeds() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val storage = AndroidActivationStorage(context)
    val prefs = context.getSharedPreferences("nome_activation_v1", android.content.Context.MODE_PRIVATE)
    try {
      prefs.edit().remove("migration_installation_id").commit()
      val first = storage.migrationInstallationId()

      assertEquals(first, storage.migrationInstallationId())
      storage.clearMigrationInstallationId()
      assertNotEquals(first, storage.migrationInstallationId())
    } finally {
      prefs.edit().remove("migration_installation_id").commit()
    }
  }


  @Test
  fun restoredGrandfatherMarkerWithoutLocalDatabaseIsDowngradedToFresh() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val prefs = context.getSharedPreferences("nome_activation_v1", android.content.Context.MODE_PRIVATE)
    try {
      prefs.edit().putString("activation_gate_migration_v1", "preexisting_local_profile").commit()

      val cohort = AndroidActivationStorage(context).installationCohort(hasUsableLocalDatabase = false)

      assertEquals(ActivationInstallationCohort.FRESH, cohort)
      assertEquals("fresh_install", prefs.getString("activation_gate_migration_v1", null))
    } finally {
      prefs.edit().remove("activation_gate_migration_v1").commit()
    }
  }

  @Test
  fun tokenRoundTripsThroughAndroidKeyStoreCiphertext() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val storage = AndroidActivationStorage(context)
    val token = "instrumentation-token-${System.nanoTime()}"
    try {
      storage.writeCredential(
        StoredActivationCredential(
          token = token,
          entitlement = ActivationEntitlement(
            status = ActivationEntitlementStatus.ACTIVE,
            tokenExpiresAt = Instant.parse("2026-07-28T12:00:00Z"),
            offlineGraceUntil = Instant.parse("2026-08-04T12:00:00Z"),
          ),
        ),
      )
      val restored = storage.readCredential()
      assertNotNull(restored)
      assertEquals(token, restored?.token)
      val prefs = context.getSharedPreferences("nome_activation_v1", android.content.Context.MODE_PRIVATE)
      assertNotEquals(token, prefs.getString("token_ciphertext", null))
    } finally {
      storage.clearCredential()
    }
  }
}
