package chat.simplex.app.nome.home

import androidx.compose.material.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.chatlist.NomeTagEditorContent
import chat.simplex.common.views.chatlist.NomeTagListChoice
import chat.simplex.common.views.chatlist.NomeTagListContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeChatListUtilitiesComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun tagEditorKeepsOfficialInputAndSubmitCallbacksReachable() {
    var closes = 0
    var submits = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeTagEditorContent(
          title = "Add to list",
          backLabel = "Back",
          submitLabel = "Add to list",
          errorText = "Names must be different",
          showError = false,
          submitEnabled = true,
          onClose = { closes++ },
          onSubmit = { submits++ },
          inputContent = { Text("Official tag input") },
        )
      }
    }

    composeRule.onNodeWithText("Official tag input").assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Back")
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithContentDescription("Add to list")
      .assertIsEnabled()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, closes)
      assertEquals(1, submits)
    }
  }

  @Test
  fun tagListKeepsOfficialSelectionAndCreateCallbacksReachable() {
    val selected = mutableListOf<Long>()
    var creates = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeTagListContent(
          title = "Add to list",
          backLabel = "Back",
          createLabel = "Create list",
          choices =
            listOf(
              NomeTagListChoice(
                id = 7,
                emoji = null,
                name = "Work",
                selected = true,
              ),
              NomeTagListChoice(
                id = 9,
                emoji = null,
                name = "Friends",
                selected = false,
              ),
            ),
          saving = false,
          onClose = {},
          onCreate = { creates++ },
          onChoice = { selected += it },
        )
      }
    }

    composeRule
      .onNodeWithText("Friends")
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithContentDescription("Create list")
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(listOf(9L), selected)
      assertEquals(1, creates)
    }
  }
}
