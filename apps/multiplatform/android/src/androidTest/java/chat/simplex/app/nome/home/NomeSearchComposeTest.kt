package chat.simplex.app.nome.home

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.model.Chat
import chat.simplex.common.ui.nome.home.NomeHomeContentState
import chat.simplex.common.ui.nome.home.NomeHomeCoreState
import chat.simplex.common.ui.nome.home.NomeSearchResults
import chat.simplex.common.ui.nome.home.NomeSearchRouteContent
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeSearchComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun productionSearchResultIsTouchableAndBackCloses() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    var opened = false
    var closed = false
    val chat = Chat.sampleData

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeSearchRouteContent(
          query = chat.chatInfo.chatViewName,
          results = NomeSearchResults(
            contacts = listOf(chat),
            groups = emptyList(),
            channels = emptyList(),
            notes = emptyList(),
          ),
          contentState = NomeHomeContentState.POPULATED,
          coreState = NomeHomeCoreState.RUNNING,
          onQueryChange = {},
          onOpenChat = { opened = true },
          onClose = { closed = true },
        )
      }
    }

    composeRule
      .onNodeWithContentDescription(
        target.getString(
          R.string.nome_p09_open_result,
          chat.chatInfo.chatViewName,
          target.getString(R.string.nome_p09_type_contact),
        ),
      )
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle { assertEquals(true, opened) }
    composeRule
      .onNodeWithText(target.getString(R.string.nome_p09_messages_scope))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(target.getString(R.string.nome_p09_recent_policy))
      .assertIsDisplayed()

    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p09_back),
      )
      .assertHasClickAction()
      .performClick()
    composeRule.runOnIdle { assertEquals(true, closed) }
  }

  @Test
  fun unavailableListNeverRendersNoResultCopy() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeSearchRouteContent(
          query = "missing",
          results = NomeSearchResults(
            contacts = emptyList(),
            groups = emptyList(),
            channels = emptyList(),
            notes = emptyList(),
          ),
          contentState = NomeHomeContentState.UNAVAILABLE,
          coreState = NomeHomeCoreState.RUNNING,
          onQueryChange = {},
          onOpenChat = {},
          onClose = {},
        )
      }
    }

    composeRule
      .onNodeWithText(target.getString(R.string.nome_p09_unavailable))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(target.getString(R.string.nome_p09_no_result_title))
      .assertDoesNotExist()
    composeRule
      .onNodeWithText(target.getString(R.string.nome_p09_recent_policy))
      .assertDoesNotExist()
  }
}
