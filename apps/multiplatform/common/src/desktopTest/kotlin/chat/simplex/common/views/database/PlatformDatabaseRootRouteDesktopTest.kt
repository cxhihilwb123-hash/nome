package chat.simplex.common.views.database

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import chat.simplex.common.views.helpers.DBMigrationResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlatformDatabaseRootRouteDesktopTest {
  @Test
  fun desktopRouteAlwaysDelegatesLegacyContentExactlyOnce() = runBlocking(ImmediateFrameClock) {
    val cases = listOf(
      DatabaseRootRouteInput.Opening to true,
      DatabaseRootRouteInput.Opening to false,
      DatabaseRootRouteInput.Migrating to true,
      DatabaseRootRouteInput.Migrating to false,
      DatabaseRootRouteInput.Error(DBMigrationResult.ErrorNotADatabase("chat.db")) to true,
      DatabaseRootRouteInput.Error(DBMigrationResult.ErrorNotADatabase("chat.db")) to false,
    )

    cases.forEach { (route, allowSensitiveContent) ->
      var legacyContentInvocations = 0
      val recomposer = Recomposer(coroutineContext)
      val composition = Composition(UnitApplier(), recomposer)
      val runner = launch {
        recomposer.runRecomposeAndApplyChanges()
      }

      try {
        composition.setContent {
          PlatformDatabaseRootRoute(
            facts = NomeDatabaseRootFacts(
              route = route,
              ctrlInitInProgress = false,
              dbMigrationInProgress = route === DatabaseRootRouteInput.Migrating,
              storedKeyUseRequested = false,
              storedKeyMaterialPresent = false,
              androidKeyReadState = null,
              matchedBackupAvailable = false,
              downgradeWarningCount = 0,
            ),
            allowSensitiveContent = allowSensitiveContent,
          ) {
            legacyContentInvocations += 1
          }
        }

        recomposer.awaitIdle()
        assertEquals(
          expected = 1,
          actual = legacyContentInvocations,
          message = "Expected exactly one legacy delegation for route=$route allowSensitiveContent=$allowSensitiveContent",
        )
      } finally {
        composition.dispose()
        recomposer.cancel()
        runner.join()
      }
    }
  }

  @Test
  fun desktopKeyReadStateHooksRemainInactive() {
    assertNull(platformDatabaseKeyReadState())
    clearPlatformDatabaseKeyReadState()
    assertNull(platformDatabaseKeyReadState())
  }

  private class UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
  }

  private object ImmediateFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(0L)
  }
}
