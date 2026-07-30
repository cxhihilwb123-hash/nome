package chat.simplex.common.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NomeAndroidCompatibilityGateTest {
  @Test
  fun stoppedCoreWithPersistedRoutesUsesBootstrapAsFinalStart() = runBlocking {
    val events = mutableListOf<String>()

    val result = runNomeAndroidCompatibilityGate(
      quiesceReceiver = { events += "receiver stopped" },
      isChatRunning = { events += "state checked"; false },
      stopRunningChat = { events += "native stopped" },
      startBootstrapChat = { events += "bootstrap started"; true },
      configureWhileRunning = {
        events += "servers already configured"
        NomeAndroidServerConfigurationState.AlreadyConfigured
      },
      startConfiguredChat = { events += "configured start"; true },
    )

    assertFalse(result.wasRunning)
    assertTrue(result.started)
    assertEquals(
      listOf(
        "state checked",
        "bootstrap started",
        "servers already configured",
      ),
      events,
    )
  }

  @Test
  fun runningCoreWithPersistedRoutesIsNotRestarted() = runBlocking {
    val events = mutableListOf<String>()

    val result = runNomeAndroidCompatibilityGate(
      quiesceReceiver = { events += "receiver stopped" },
      isChatRunning = { events += "state checked"; true },
      stopRunningChat = { events += "native stopped" },
      startBootstrapChat = { events += "bootstrap started"; true },
      configureWhileRunning = {
        events += "servers already configured"
        NomeAndroidServerConfigurationState.AlreadyConfigured
      },
      startConfiguredChat = { events += "configured start"; true },
    )

    assertTrue(result.wasRunning)
    assertTrue(result.started)
    assertEquals(
      listOf(
        "state checked",
        "servers already configured",
      ),
      events,
    )
  }

  @Test
  fun pendingConfigurationStopsBootstrapAndNeverStartsFinalWorkers() = runBlocking {
    val events = mutableListOf<String>()

    val result = runNomeAndroidCompatibilityGate(
      quiesceReceiver = { events += "receiver stopped" },
      awaitReceiverQuiesced = { events += "receiver joined" },
      isChatRunning = { events += "state checked"; false },
      stopRunningChat = { events += "native stopped" },
      awaitNativeStopSettled = { events += "native settled" },
      startBootstrapChat = { events += "bootstrap started"; true },
      configureWhileRunning = {
        events += "servers pending"
        NomeAndroidServerConfigurationState.Pending
      },
      startConfiguredChat = { events += "configured start"; true },
    )

    assertFalse(result.started)
    assertEquals(
      listOf(
        "state checked",
        "bootstrap started",
        "servers pending",
        "receiver stopped",
        "native stopped",
        "receiver joined",
        "native settled",
      ),
      events,
    )
  }

  @Test
  fun configurationExceptionRunsFailClosedCleanup() = runBlocking {
    val events = mutableListOf<String>()

    assertFailsWith<IllegalStateException> {
      runNomeAndroidCompatibilityGate(
        quiesceReceiver = { events += "receiver stopped" },
        awaitReceiverQuiesced = { events += "receiver joined" },
        isChatRunning = { events += "state checked"; false },
        stopRunningChat = { events += "native stopped" },
        awaitNativeStopSettled = { events += "native settled" },
        startBootstrapChat = { events += "bootstrap started"; true },
        configureWhileRunning = {
          events += "configuration failed"
          throw IllegalStateException("configuration failed")
        },
        startConfiguredChat = { events += "configured start"; true },
        onUnsafeFailure = { events += "fail-closed stop" },
      )
    }

    assertEquals(
      listOf(
        "state checked",
        "bootstrap started",
        "configuration failed",
        "receiver stopped",
        "fail-closed stop",
        "receiver joined",
      ),
      events,
    )
  }

  @Test
  fun cancellationRunsFailClosedCleanupAndEscapesUnchanged() = runBlocking {
    val events = mutableListOf<String>()
    val cancellation = CancellationException("startup cancelled")

    val thrown = assertFailsWith<CancellationException> {
      runNomeAndroidCompatibilityGate(
        quiesceReceiver = { events += "receiver stopped" },
        awaitReceiverQuiesced = { events += "receiver joined" },
        isChatRunning = { events += "state checked"; false },
        stopRunningChat = { events += "native stopped" },
        awaitNativeStopSettled = { events += "native settled" },
        startBootstrapChat = { events += "bootstrap started"; true },
        configureWhileRunning = {
          events += "configuration cancelled"
          throw cancellation
        },
        startConfiguredChat = { events += "configured start"; true },
        onUnsafeFailure = { events += "fail-closed stop" },
      )
    }

    assertSame(cancellation, thrown)
    assertEquals(
      listOf(
        "state checked",
        "bootstrap started",
        "configuration cancelled",
        "receiver stopped",
        "fail-closed stop",
        "receiver joined",
      ),
      events,
    )
  }

  @Test
  fun rejectedBootstrapRunsFailClosedCleanupWithoutConfigurationOrFinalStart() = runBlocking {
    val events = mutableListOf<String>()

    assertFailsWith<IllegalStateException> {
      runNomeAndroidCompatibilityGate(
        quiesceReceiver = { events += "receiver stopped" },
        isChatRunning = { events += "state checked"; false },
        stopRunningChat = { events += "native stopped" },
        startBootstrapChat = { events += "bootstrap rejected"; false },
        configureWhileRunning = {
          events += "servers configured"
          NomeAndroidServerConfigurationState.AlreadyConfigured
        },
        startConfiguredChat = { events += "configured start"; true },
        onUnsafeFailure = { events += "fail-closed stop" },
      )
    }

    assertEquals(
      listOf(
        "state checked",
        "bootstrap rejected",
        "receiver stopped",
        "fail-closed stop",
      ),
      events,
    )
  }

  @Test
  fun rejectedFinalStartRunsFailClosedCleanup() = runBlocking {
    val events = mutableListOf<String>()

    assertFailsWith<IllegalStateException> {
      runNomeAndroidCompatibilityGate(
        quiesceReceiver = { events += "receiver stopped" },
        awaitReceiverQuiesced = { events += "receiver joined" },
        isChatRunning = { events += "state checked"; false },
        stopRunningChat = { events += "native stopped" },
        awaitNativeStopSettled = { events += "native settled" },
        startBootstrapChat = { events += "bootstrap started"; true },
        configureWhileRunning = {
          events += "servers configured"
          NomeAndroidServerConfigurationState.Changed
        },
        startConfiguredChat = { events += "configured start rejected"; false },
        onUnsafeFailure = { events += "fail-closed stop" },
      )
    }

    assertEquals(
      listOf(
        "state checked",
        "bootstrap started",
        "servers configured",
        "receiver stopped",
        "native stopped",
        "receiver joined",
        "native settled",
        "configured start rejected",
        "fail-closed stop",
      ),
      events,
    )
  }
}
