package chat.simplex.app.nome.settings

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
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.views.remote.NomeRemoteDesktopFrame
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeRemoteDesktopComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun androidFrameUsesFullPageBackOwnerAndKeepsRealPairingContent() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var closes = 0

    composeRule.setContent {
      NomeRemoteDesktopFrame(
        title = "连接到桌面",
        onClose = { closes++ },
        legacyContent = {
          error("Android must not render the legacy remote-desktop modal")
        },
      ) {
        Text("从桌面扫描二维码")
      }
    }

    composeRule
      .onNodeWithText("连接到桌面")
      .assertIsDisplayed()
    composeRule
      .onNodeWithText("从桌面扫描二维码")
      .assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_back),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()

    composeRule.runOnIdle {
      assertEquals(1, closes)
    }
  }
}
