package chat.simplex.common.views.usersettings

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UserDeletionLifecycleTest {
  @Test
  fun switchFailureLeavesTargetUntouched() =
    runBlocking {
      val calls = mutableListOf<String>()

      val result =
        runUserDeletionLifecycle(
          targetWasActive = true,
          fallbackUserId = 42,
          stopAfterLastVisibleUser = true,
          switchToFallback = {
            calls += "switch:$it"
            error("controlled switch failure")
          },
          deleteTarget = {
            calls += "delete"
          },
          clearActiveUser = {
            calls += "clear"
          },
          stopChat = {
            calls += "stop"
          },
        )

      assertEquals(
        listOf("switch:42"),
        calls,
      )
      val failed =
        assertIs<UserDeletionResult.Failed>(
          result,
        )
      assertEquals(
        UserDeletionStage.SWITCH_TO_FALLBACK,
        failed.stage,
      )
      assertFalse(failed.targetDeleted)
      assertEquals(42, failed.fallbackUserId)
    }

  @Test
  fun activeUserWithFallbackSwitchesBeforeConfirmedDelete() =
    runBlocking {
      val calls = mutableListOf<String>()

      val result =
        runUserDeletionLifecycle(
          targetWasActive = true,
          fallbackUserId = 42,
          stopAfterLastVisibleUser = true,
          switchToFallback = {
            calls += "switch:$it"
          },
          deleteTarget = {
            calls += "delete"
          },
          clearActiveUser = {
            calls += "clear"
          },
          stopChat = {
            calls += "stop"
          },
        )

      assertEquals(
        listOf("switch:42", "delete"),
        calls,
      )
      val completed =
        assertIs<UserDeletionResult.Completed>(
          result,
        )
      assertTrue(completed.targetDeleted)
      assertFalse(completed.chatStopped)
    }

  @Test
  fun deleteFailureAfterSwitchRemainsExplicitAndRecoverable() =
    runBlocking {
      val calls = mutableListOf<String>()

      val result =
        runUserDeletionLifecycle(
          targetWasActive = true,
          fallbackUserId = 42,
          stopAfterLastVisibleUser = true,
          switchToFallback = {
            calls += "switch:$it"
          },
          deleteTarget = {
            calls += "delete"
            error("controlled delete failure")
          },
          clearActiveUser = {
            calls += "clear"
          },
          stopChat = {
            calls += "stop"
          },
        )

      assertEquals(
        listOf("switch:42", "delete"),
        calls,
      )
      val failed =
        assertIs<UserDeletionResult.Failed>(
          result,
        )
      assertEquals(
        UserDeletionStage.DELETE_TARGET,
        failed.stage,
      )
      assertFalse(failed.targetDeleted)
      assertEquals(42, failed.fallbackUserId)
    }

  @Test
  fun lastVisibleUserReportsStopFailureAfterConfirmedDelete() =
    runBlocking {
      val calls = mutableListOf<String>()

      val result =
        runUserDeletionLifecycle(
          targetWasActive = true,
          fallbackUserId = null,
          stopAfterLastVisibleUser = true,
          switchToFallback = {
            calls += "switch:$it"
          },
          deleteTarget = {
            calls += "delete"
          },
          clearActiveUser = {
            calls += "clear"
          },
          stopChat = {
            calls += "stop"
            error("controlled stop failure")
          },
        )

      assertEquals(
        listOf("delete", "clear", "stop"),
        calls,
      )
      val failed =
        assertIs<UserDeletionResult.Failed>(
          result,
        )
      assertEquals(
        UserDeletionStage.STOP_CHAT,
        failed.stage,
      )
      assertTrue(failed.targetDeleted)
      assertEquals(null, failed.fallbackUserId)
    }
}
