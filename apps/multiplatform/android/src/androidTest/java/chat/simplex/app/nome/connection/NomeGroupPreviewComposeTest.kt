package chat.simplex.app.nome.connection

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.chatlist.NomeGroupInvitationContent
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeGroupPreviewComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun successfulJoinClosesAfterOneSubmission() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    var joins = 0
    var closes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeGroupInvitationContent(
          groupName = "Design Exchange",
          groupFullName = "Design Exchange",
          groupDescription = null,
          groupImage = null,
          currentMembers = 128,
          isPublic = true,
          isChannel = false,
          requiresReview = false,
          joinsIncognito = false,
          inviterName = "Lin",
          inviterVerified = false,
          onJoin = {
            joins++
            true
          },
          onDelete = { false },
          onClose = { closes++ },
        )
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p16_join),
      )
      .performClick()
    composeRule.waitUntil {
      joins == 1 && closes == 1
    }
    composeRule.runOnIdle {
      assertEquals(1, joins)
      assertEquals(1, closes)
    }
  }

  @Test
  fun joinIsSingleSubmitBlocksCloseAndRetainsInvitationOnFailure() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    val result = CompletableDeferred<Boolean>()
    var joins = 0
    var closes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeGroupInvitationContent(
          groupName = "Design Exchange",
          groupFullName = "Design Exchange",
          groupDescription = "Product design methods and case studies",
          groupImage = null,
          currentMembers = 128,
          isPublic = true,
          isChannel = false,
          requiresReview = true,
          joinsIncognito = false,
          inviterName = "Lin",
          inviterVerified = true,
          onJoin = {
            joins++
            result.await()
          },
          onDelete = { false },
          onClose = { closes++ },
        )
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p16_join),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.waitUntil { joins == 1 }
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p16_delete_invitation,
        ),
      )
      .assertIsNotEnabled()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p16_back),
      )
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, joins)
      assertEquals(0, closes)
    }

    result.complete(false)
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p16_action_not_completed,
        ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p16_verified_contact,
          "Lin",
        ),
      )
      .assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(1, joins)
      assertEquals(0, closes)
    }
  }

  @Test
  fun deleteRequiresConfirmationAndFailureKeepsPreview() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    var deletes = 0
    var closes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeGroupInvitationContent(
          groupName = "Design Exchange",
          groupFullName = "Design Exchange",
          groupDescription = null,
          groupImage = null,
          currentMembers = 12,
          isPublic = false,
          isChannel = false,
          requiresReview = false,
          joinsIncognito = true,
          inviterName = null,
          inviterVerified = false,
          onJoin = { false },
          onDelete = {
            deletes++
            false
          },
          onClose = { closes++ },
        )
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p16_delete_invitation,
        ),
      )
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(0, deletes)
    }
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p16_delete_confirm,
        ),
      )
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p16_action_not_completed,
        ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p16_source_unavailable,
        ),
      )
      .assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(1, deletes)
      assertEquals(0, closes)
    }
  }
}
