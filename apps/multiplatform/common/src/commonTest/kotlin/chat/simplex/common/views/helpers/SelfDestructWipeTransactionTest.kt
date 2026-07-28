package chat.simplex.common.views.helpers

import chat.simplex.common.model.SelfDestructWipeState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SelfDestructWipeTransactionTest {
  @Test
  fun credentialErasureRequiresPlatformDeletionAndEmptyMarkerReadback() {
    var platformKeyDeleted = false
    var passphrase: String? = "ciphertext"
    var initVector: String? = "nonce"

    removeCredentialMaterialVerified(
      deletePlatformKey = { platformKeyDeleted = true },
      clearPassphrase = { passphrase = null },
      clearInitVector = { initVector = null },
      readPassphrase = { passphrase },
      readInitVector = { initVector },
    )

    assertTrue(platformKeyDeleted)
    assertEquals(null, passphrase)
    assertEquals(null, initVector)
  }

  @Test
  fun credentialErasureStopsWhenPlatformKeyDeletionFails() {
    var metadataClearCalled = false

    val failure = assertFailsWith<CredentialErasureException> {
      removeCredentialMaterialVerified(
        deletePlatformKey = { error("injected key deletion failure") },
        clearPassphrase = { metadataClearCalled = true },
        clearInitVector = { metadataClearCalled = true },
        readPassphrase = { "ciphertext" },
        readInitVector = { "nonce" },
      )
    }

    assertFalse(metadataClearCalled)
    assertIs<IllegalStateException>(failure.cause)
  }

  @Test
  fun credentialErasureFailsWhenSettingsBackendLeavesEitherMarker() {
    assertFailsWith<CredentialErasureException> {
      removeCredentialMaterialVerified(
        deletePlatformKey = {},
        clearPassphrase = {},
        clearInitVector = {},
        readPassphrase = { "stale-ciphertext" },
        readInitVector = { null },
      )
    }
  }

  @Test
  fun idleRecoveryDoesNotRunAnyDestructiveStep() {
    var destructiveStepCalled = false

    runSelfDestructRecoveryTransaction(
      readState = { SelfDestructWipeState.IDLE },
      transition = { destructiveStepCalled = true },
      eraseDatabase = { destructiveStepCalled = true },
      eraseAppCredential = { destructiveStepCalled = true },
      eraseSelfDestructCredential = { destructiveStepCalled = true },
      resetAuthentication = { destructiveStepCalled = true },
    )

    assertFalse(destructiveStepCalled)
  }

  @Test
  fun interruptedRecoveryCompletesEveryVerifiedStepInOrder() {
    val calls = mutableListOf<String>()

    runSelfDestructRecoveryTransaction(
      readState = { SelfDestructWipeState.IN_PROGRESS },
      transition = { calls += "state:$it" },
      eraseDatabase = { calls += "database" },
      eraseAppCredential = { calls += "app-credential" },
      eraseSelfDestructCredential = { calls += "self-destruct-credential" },
      resetAuthentication = { calls += "authentication" },
    )

    assertEquals(
      listOf(
        "state:IN_PROGRESS",
        "database",
        "app-credential",
        "self-destruct-credential",
        "authentication",
        "state:IDLE",
      ),
      calls,
    )
  }

  @Test
  fun everyInjectedRecoveryFailureLeavesIncompleteAndSkipsLaterSteps() {
    val destructiveSteps = listOf("database", "app-credential", "self-destruct-credential", "authentication")

    destructiveSteps.forEach { failingStep ->
      val calls = mutableListOf<String>()
      val failure = assertFailsWith<SelfDestructWipeIncompleteException> {
        runSelfDestructRecoveryTransaction(
          readState = { SelfDestructWipeState.INCOMPLETE },
          transition = { calls += "state:$it" },
          eraseDatabase = { recordOrFail("database", failingStep, calls) },
          eraseAppCredential = { recordOrFail("app-credential", failingStep, calls) },
          eraseSelfDestructCredential = { recordOrFail("self-destruct-credential", failingStep, calls) },
          resetAuthentication = { recordOrFail("authentication", failingStep, calls) },
        )
      }

      assertIs<IllegalStateException>(failure.cause)
      assertEquals("state:IN_PROGRESS", calls.first())
      assertEquals("state:INCOMPLETE", calls.last())
      val failingIndex = calls.indexOf(failingStep)
      assertTrue(failingIndex >= 0)
      destructiveSteps.drop(destructiveSteps.indexOf(failingStep) + 1).forEach { laterStep ->
        assertFalse(laterStep in calls)
      }
    }
  }

  @Test
  fun failurePersistingIdleIsConvertedBackToIncomplete() {
    val states = mutableListOf<SelfDestructWipeState>()

    assertFailsWith<SelfDestructWipeIncompleteException> {
      runSelfDestructRecoveryTransaction(
        readState = { SelfDestructWipeState.IN_PROGRESS },
        transition = { state ->
          states += state
          if (state == SelfDestructWipeState.IDLE) error("injected final marker failure")
        },
        eraseDatabase = {},
        eraseAppCredential = {},
        eraseSelfDestructCredential = {},
        resetAuthentication = {},
      )
    }

    assertEquals(
      listOf(
        SelfDestructWipeState.IN_PROGRESS,
        SelfDestructWipeState.IDLE,
        SelfDestructWipeState.INCOMPLETE,
      ),
      states,
    )
  }

  private fun recordOrFail(step: String, failingStep: String, calls: MutableList<String>) {
    calls += step
    if (step == failingStep) error("injected $step failure")
  }
}
