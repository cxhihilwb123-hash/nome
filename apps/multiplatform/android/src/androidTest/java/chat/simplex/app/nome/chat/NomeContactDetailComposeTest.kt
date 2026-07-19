package chat.simplex.app.nome.chat

import androidx.compose.material.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.chat.NomeContactDetailContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeContactDetailComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun officialContactSlotsRemainReachableInFullPagePresentation() {
    var closes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeContactDetailContent(
          title = "Contact",
          backLabel = "Back",
          onClose = { closes++ },
          headerContent = { Text("Official profile") },
          quickActionsContent = { Text("Official actions") },
          detailsContent = { Text("Official contact settings") },
        )
      }
    }

    composeRule.onNodeWithText("Contact").assertIsDisplayed()
    composeRule.onNodeWithText("Official profile").assertIsDisplayed()
    composeRule.onNodeWithText("Official actions").assertIsDisplayed()
    composeRule.onNodeWithText("Official contact settings").assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Back")
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle { assertEquals(1, closes) }
  }
}
