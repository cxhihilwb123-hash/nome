package chat.simplex.app.nome.chat

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.chat.item.ItemAction
import chat.simplex.common.views.chat.item.PlatformMessageActionsMenu
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeConversationActionsComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun officialActionCallbackRemainsSingleAndReachableInBottomSheet() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    var replies = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        val showMenu = remember { mutableStateOf(true) }
        PlatformMessageActionsMenu(showMenu) {
          ItemAction(
            text = target.getString(R.string.reply_verb),
            icon = painterResource(MR.images.ic_reply),
            onClick = {
              replies++
              showMenu.value = false
            },
          )
        }
      }
    }

    composeRule
      .onNodeWithText(target.getString(R.string.nome_message_actions))
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(target.getString(R.string.reply_verb))
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle { assertEquals(1, replies) }
  }

  @Test
  fun closeDismissesWithoutInvokingAnAction() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    var actions = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        val showMenu = remember { mutableStateOf(true) }
        PlatformMessageActionsMenu(showMenu) {
          ItemAction(
            text = target.getString(R.string.reply_verb),
            icon = painterResource(MR.images.ic_reply),
            onClick = {
              actions++
              showMenu.value = false
            },
          )
        }
      }
    }

    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.icon_descr_close_button),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle { assertEquals(0, actions) }
  }
}
