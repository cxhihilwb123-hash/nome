package chat.simplex.app.nome.developer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.TerminalLayout
import chat.simplex.common.views.chat.ComposeState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTerminalViewComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun terminalRendersWithoutRequestingAnUnattachedComposeFocusTarget() {
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        val composeState = remember {
          mutableStateOf(ComposeState(useLinkPreviews = false))
        }
        TerminalLayout(
          composeState = composeState,
          floating = false,
          sendCommand = {},
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onRoot().assertIsDisplayed()
  }
}
