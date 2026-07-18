package chat.simplex.app.nome.newchat

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.newchat.NomeNewChatHubContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeNewChatHubComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun allVisibleActionsInvokeOnlyTheirOfficialRouteCallback() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    var invoked = ""

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeNewChatHubContent(
          currentProfileName = "Lin",
          onOpenProfile = { invoked = "profile" },
          onAddContact = { invoked = "invite" },
          onScanOrPaste = { invoked = "scan" },
          onCreateGroup = { invoked = "group" },
          onCreateChannel = { invoked = "channel" },
          onClose = { invoked = "close" },
        )
      }
    }

    listOf(
      R.string.nome_p10_add_contact to "invite",
      R.string.nome_p10_scan_paste to "scan",
      R.string.nome_p10_group to "group",
      R.string.nome_p10_channel to "channel",
    ).forEach { (label, expected) ->
      composeRule
        .onNodeWithText(target.getString(label))
        .assertHasClickAction()
        .assertHeightIsAtLeast(48.dp)
        .performClick()
      composeRule.runOnIdle { assertEquals(expected, invoked) }
    }
  }
}
