package chat.simplex.app.nome.database

import chat.simplex.common.ui.nome.database.NomeDatabaseAttemptGate
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NomeDatabaseAttemptGateTest {
  @Test
  fun eightConcurrentSubmitsAcceptExactlyOneAttempt() {
    val gate = NomeDatabaseAttemptGate()
    val executor = Executors.newFixedThreadPool(8)
    val ready = CountDownLatch(8)
    val start = CountDownLatch(1)
    val accepted = ConcurrentLinkedQueue<Long>()

    repeat(8) {
      executor.execute {
        ready.countDown()
        start.await()
        gate.beginAttempt()?.let(accepted::add)
      }
    }

    assertTrue(ready.await(5, TimeUnit.SECONDS))
    start.countDown()
    executor.shutdown()
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))

    assertEquals(1, accepted.size)
    assertNotNull(gate.activeGenerationOrNull())
  }

  @Test
  fun staleGenerationCannotCompleteANewerAttempt() {
    val gate = NomeDatabaseAttemptGate()

    val first = requireNotNull(gate.beginAttempt())
    assertTrue(gate.completeAttempt(first))
    assertEquals(first, gate.completedGenerationOrNull())

    val second = requireNotNull(gate.beginAttempt())
    assertEquals(second, gate.activeGenerationOrNull())
    assertFalse(gate.completeAttempt(first))
    assertEquals(second, gate.activeGenerationOrNull())
    assertTrue(gate.completeAttempt(second))
    assertEquals(second, gate.completedGenerationOrNull())
    assertNull(gate.activeGenerationOrNull())
  }

  @Test
  fun inFlightAttemptRejectsDuplicateBeginsUntilItsOwnCompletion() {
    val gate = NomeDatabaseAttemptGate()

    val generation = requireNotNull(gate.beginAttempt())

    assertNull(gate.beginAttempt())
    assertFalse(gate.completeAttempt(generation + 1L))
    assertEquals(generation, gate.activeGenerationOrNull())
    assertTrue(gate.completeAttempt(generation))
    assertNull(gate.activeGenerationOrNull())
  }
}
