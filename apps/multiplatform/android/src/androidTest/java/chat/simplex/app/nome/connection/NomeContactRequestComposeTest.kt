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
import chat.simplex.common.views.chatlist.NomeContactRequestContent
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeContactRequestComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun acceptCurrentIsSingleSubmitAndBlocksClosingWhileBusy() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    val result = CompletableDeferred<Boolean>()
    var accepts = 0
    var closes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeContactRequestContent(
          requestName = "Mira",
          requestFullName = "Mira Chen",
          requestImage = null,
          currentProfileName = "Lin",
          currentProfileImage = null,
          canAcceptIncognito = true,
          onAccept = {
            accepts++
            result.await()
          },
          onReject = { false },
          onClose = { closes++ },
        )
      }
    }

    val currentChoice =
      target.getString(R.string.nome_p14_accept_current) +
        ". " +
        target.getString(
          R.string.nome_p14_accept_current_body,
          "Lin",
        )
    composeRule
      .onNodeWithContentDescription(currentChoice)
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.waitUntil { accepts == 1 }

    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p14_accept_incognito,
        ),
      )
      .assertIsNotEnabled()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p14_reject),
      )
      .assertIsNotEnabled()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p14_back),
      )
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, accepts)
      assertEquals(0, closes)
    }

    result.complete(false)
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p14_action_failed,
        ),
      )
      .assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(1, accepts)
      assertEquals(0, closes)
    }
  }

  @Test
  fun rejectFailureKeepsRequestOpenAndNeverClaimsSuccess() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    var rejects = 0
    var closes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeContactRequestContent(
          requestName = "Mira",
          requestFullName = "",
          requestImage = null,
          currentProfileName = "Lin",
          currentProfileImage = null,
          canAcceptIncognito = false,
          onAccept = { false },
          onReject = {
            rejects++
            false
          },
          onClose = { closes++ },
        )
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p14_reject),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p14_accept_incognito,
        ),
      )
      .assertIsDisplayed()
      .assertIsNotEnabled()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p14_accept_incognito_unavailable_body,
        ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p14_action_failed,
        ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p14_title),
      )
      .assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(1, rejects)
      assertEquals(0, closes)
    }
  }
}
