package chat.simplex.app.nome.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.helpers.LocalAppBarHandler
import chat.simplex.common.views.helpers.rememberAppBarHandler
import chat.simplex.common.views.usersettings.RTCServersLayout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeServerAddressVisibilityTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun readOnlyRtcServerShowsDomainAndEditorKeepsCompleteAddress() {
    lateinit var editing: MutableState<Boolean>
    lateinit var address: MutableState<String>

    composeRule.setContent {
      editing = remember { mutableStateOf(false) }
      address = remember { mutableStateOf(FULL_ADDRESS) }
      NomeAndroidTheme(darkTheme = false) {
        CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
          RTCServersLayout(
            isUserRTCServers = true,
            editRTCServers = editing.value,
            userRTCServersStr = address,
            isUserRTCServersOnOff = {},
            cancelEdit = { editing.value = false },
            saveRTCServers = { editing.value = false },
            editOn = { editing.value = true },
          )
        }
      }
    }

    composeRule.onNodeWithText(HOSTNAME).assertIsDisplayed()
    composeRule.onNodeWithText(FULL_ADDRESS).assertDoesNotExist()

    composeRule.runOnUiThread { editing.value = true }
    composeRule.onNodeWithText(FULL_ADDRESS).assertIsDisplayed()
  }

  private companion object {
    const val HOSTNAME = "turn.nome.im"
    const val FULL_ADDRESS = "turn:example-user:example-password@turn.nome.im"
  }
}
