package chat.simplex.common.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.CancellationException

class ChatStartRetryPolicyTest {
  @Test
  fun exhaustedNomeConfigurationBecomesRetryableState() = runBlocking {
    val failure = retryableNomeServerChatStart(User.sampleData) { false }

    assertNotNull(failure)
    assertEquals(User.sampleData, failure.user)
    assertEquals(RetryableChatStartReason.NomeServerConfiguration, failure.reason)
  }

  @Test
  fun successfulRetryClearsTheFailureOutcome() = runBlocking {
    val failure = retryableNomeServerChatStart(User.sampleData) { false }
    val success = retryableNomeServerChatStart(User.sampleData) { true }

    assertNotNull(failure)
    assertNull(success)
  }

  @Test
  fun repeatedExhaustionPublishesANewUiAttempt() = runBlocking {
    val first = retryableNomeServerChatStart(User.sampleData) { false }
    val second = retryableNomeServerChatStart(User.sampleData) { false }

    assertNotNull(first)
    assertNotNull(second)
    assertNotEquals(first.attemptId, second.attemptId)
  }

  @Test
  fun unexpectedFailureCanPublishANewGenericRecoveryAttempt() {
    val retry = newRetryableChatStart(
      User.sampleData,
      RetryableChatStartReason.NomeStartupFailure,
    )

    assertEquals(RetryableChatStartReason.NomeStartupFailure, retry.reason)
    assertEquals(User.sampleData, retry.user)
  }

  @Test
  fun startupFailureSummaryNeverIncludesThrowablePayload() {
    val secret = "smp://fingerprint:credential@example.invalid"
    val summary = nomeChatStartFailureSummary(IllegalStateException(secret))

    assertFalse(summary.contains(secret))
    assertFalse(summary.contains("credential"))
    assertTrue(summary.contains("IllegalStateException"))
  }

  @Test
  fun startupFailureEscapingToCallersIsGenericAndCauseFree() {
    val secret = "xftp://fingerprint:credential@example.invalid"
    val safe = safeNomeChatStartFailure(IllegalStateException(secret))

    assertTrue(safe is NomeChatStartupException)
    assertFalse(safe.message.orEmpty().contains("credential"))
    assertNull(safe.cause)
  }

  @Test
  fun startupCancellationIdentityIsPreserved() {
    val cancellation = CancellationException("cancelled")

    assertTrue(safeNomeChatStartFailure(cancellation) === cancellation)
  }

  @Test
  fun throwingPostStartContinuationPublishesRecoveryWithoutChangingNativeSuccess() = runBlocking {
    var recoveryPublished = false
    var nativeStarted = false

    val startResult = runNomeChatStartAttempt(
      transition = {
        nativeStarted = true
        true
      },
      continuation = { throw IllegalStateException("screen transition failed") },
      onContinuationFailure = { recoveryPublished = true },
    )

    assertTrue(nativeStarted)
    assertTrue(startResult)
    assertTrue(recoveryPublished)
  }

  @Test
  fun retryStateCarriesTheOriginalSuccessContinuation() = runBlocking {
    var continued = false
    val failure = retryableNomeServerChatStart(
      user = User.sampleData,
      onStarted = { continued = true },
      applyConfiguration = { false },
    )

    assertNotNull(failure)
    failure.onStarted()
    assertTrue(continued)
  }

  @Test
  fun retryUiConsumesAContinuationOnlyOnceOnDoubleClick() = runBlocking {
    var continued = 0
    val failure = requireNotNull(
      retryableNomeServerChatStart(
        user = User.sampleData,
        onStarted = { continued += 1 },
        applyConfiguration = { false },
      ),
    )
    try {
      ChatModel.retryableChatStart.value = failure
      repeat(2) {
        if (ChatModel.consumeRetryableChatStart(failure)) failure.onStarted()
      }
      assertEquals(1, continued)
    } finally {
      ChatModel.retryableChatStart.value = null
    }
  }

  @Test
  fun unexpectedStartupExceptionStillPropagates() {
    assertFailsWith<IllegalStateException> {
      runBlocking {
        retryableNomeServerChatStart(User.sampleData) {
          throw IllegalStateException("unexpected core failure")
        }
      }
    }
  }

  @Test
  fun gateFailureConfirmsStopBeforePublishingStoppedState() = runBlocking {
    val events = mutableListOf<String>()

    enforceNomeGateFailureStop(
      stopChat = { events += "stop" },
      onStopped = { events += "stopped" },
      onUnknown = { events += "unknown" },
      terminate = { error("must not terminate") },
    )

    assertEquals(listOf("stop", "stopped"), events)
  }

  @Test
  fun unconfirmedStopTerminatesWithoutPublishingStoppedState() = runBlocking {
    val events = mutableListOf<String>()
    val terminated = assertFailsWith<UnsafeNetworkTermination> {
      enforceNomeGateFailureStop(
        stopChat = { throw CancellationException("stop was not confirmed") },
        onStopped = { events += "stopped" },
        onUnknown = { events += "unknown" },
        terminate = { throw UnsafeNetworkTermination() },
      )
    }

    assertNotNull(terminated)
    assertEquals(listOf("unknown"), events)
  }

  @Test
  fun stoppedChatIsConfiguredBeforeNativeStart() = runBlocking {
    val events = mutableListOf<String>()
    val result = runNomePreNetworkGate(
      quiesceReceiver = { events += "receiver stopped" },
      isChatRunning = { events += "state checked"; false },
      stopRunningChat = { events += "native stopped" },
      configureWhileStopped = { events += "servers configured"; true },
      startConfiguredChat = { events += "native started"; true },
    )

    assertEquals(false, result.wasRunning)
    assertTrue(result.started)
    assertEquals(
      listOf("receiver stopped", "state checked", "servers configured", "native started"),
      events,
    )
  }

  @Test
  fun runningChatIsStoppedBeforeConfigurationAndRestartedAfter() = runBlocking {
    val events = mutableListOf<String>()
    val result = runNomePreNetworkGate(
      quiesceReceiver = { events += "receiver stopped" },
      isChatRunning = { events += "state checked"; true },
      stopRunningChat = { events += "native stopped" },
      configureWhileStopped = { events += "servers configured"; true },
      startConfiguredChat = { events += "native started"; true },
    )

    assertTrue(result.wasRunning)
    assertTrue(result.started)
    assertEquals(
      listOf("receiver stopped", "state checked", "native stopped", "servers configured", "native started"),
      events,
    )
  }

  @Test
  fun pendingConfigurationNeverStartsNativeNetworking() = runBlocking {
    var started = false
    val result = runNomePreNetworkGate(
      quiesceReceiver = {},
      isChatRunning = { false },
      stopRunningChat = {},
      configureWhileStopped = { false },
      startConfiguredChat = { started = true; true },
    )

    assertEquals(false, result.started)
    assertEquals(false, started)
  }

  @Test
  fun unknownRunningStateIsStoppedBeforeFailureEscapes() = runBlocking {
    val events = mutableListOf<String>()

    assertFailsWith<IllegalStateException> {
      runNomePreNetworkGate(
        quiesceReceiver = { events += "receiver stopped" },
        isChatRunning = {
          events += "state unknown"
          throw IllegalStateException("native state unavailable")
        },
        stopRunningChat = { events += "known-running stop" },
        configureWhileStopped = { events += "servers configured"; true },
        startConfiguredChat = { events += "native started"; true },
        onUnsafeFailure = { events += "fail-closed stop" },
      )
    }

    assertEquals(listOf("receiver stopped", "state unknown", "fail-closed stop"), events)
  }

  @Test
  fun rejectedNetworkConfigurationStopsAndNeverStartsNativeNetworking() = runBlocking {
    val events = mutableListOf<String>()

    assertFailsWith<IllegalStateException> {
      runNomePreNetworkGate(
        quiesceReceiver = { events += "receiver stopped" },
        isChatRunning = { events += "state checked"; false },
        stopRunningChat = { events += "known-running stop" },
        configureWhileStopped = {
          events += "network config rejected"
          throw IllegalStateException("network config rejected")
        },
        startConfiguredChat = { events += "native started"; true },
        onUnsafeFailure = { events += "fail-closed stop" },
      )
    }

    assertEquals(
      listOf("receiver stopped", "state checked", "network config rejected", "fail-closed stop"),
      events,
    )
  }

  @Test
  fun concurrentStartTransitionsNeverInterleave() = runBlocking {
    val singleFlight = NomeChatStartSingleFlight()
    val firstEntered = CompletableDeferred<Unit>()
    val releaseFirst = CompletableDeferred<Unit>()
    val secondLaunched = CompletableDeferred<Unit>()
    val events = mutableListOf<String>()

    val first = async {
      singleFlight.run {
        events += "first start"
        firstEntered.complete(Unit)
        releaseFirst.await()
        events += "first callback"
      }
    }
    firstEntered.await()
    val second = async {
      secondLaunched.complete(Unit)
      singleFlight.run {
        events += "second start"
        events += "second callback"
      }
    }
    secondLaunched.await()
    repeat(4) { yield() }
    assertEquals(listOf("first start"), events)

    releaseFirst.complete(Unit)
    first.await()
    second.await()
    assertEquals(
      listOf("first start", "first callback", "second start", "second callback"),
      events,
    )
  }

  private class UnsafeNetworkTermination : RuntimeException()
}
