package chat.simplex.app.nome.connection

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.model.User
import chat.simplex.common.model.UserInfo
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.chat.NomeChannelDisclosureContent
import chat.simplex.common.views.chat.NomeChannelObserverContent
import chat.simplex.common.views.usersettings.NomeIdentityCenterContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeChannelIdentityComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun channelChromeKeepsDisclosureAndObserverStateExplicit() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        androidx.compose.foundation.layout.Column {
          NomeChannelDisclosureContent()
          NomeChannelObserverContent()
        }
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p21_non_e2ee),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p21_history),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p21_observer),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p21_observer_body,
        ),
      )
      .assertIsDisplayed()
  }

  @Test
  fun identityCenterDispatchesOnlyOfficialOwners() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    val active =
      User.sampleData.copy(
        userId = 41,
        profile =
          User.sampleData.profile.copy(
            displayName = "Lin",
            fullName = "Lin",
          ),
        activeUser = true,
      )
    val work =
      User.sampleData.copy(
        userId = 42,
        profile =
          User.sampleData.profile.copy(
            displayName = "Work",
            fullName = "Work",
          ),
        activeUser = false,
      )
    var additions = 0
    var edits = 0
    var activations = 0
    var closes = 0
    var incognitoChanges = 0
    var incognitoInfo = 0
    var networkSettings = 0
    var contacts = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeIdentityCenterContent(
          users = listOf(active, work),
          filteredUsers =
            listOf(
              UserInfo(active, 0),
              UserInfo(work, 0),
            ),
          searchTextOrPassword =
            remember {
              mutableStateOf("")
            },
          profileHidden = false,
          visibleUsersCount = 2,
          incognitoDefault = false,
          socksProxyEnabled = false,
          onClose = { closes++ },
          onRevealPasswordEntry = {},
          onAddUser = { additions++ },
          onEditCurrentUser = { edits++ },
          onActivateUser = { activations++ },
          onRemoveUser = {},
          onUnhideUser = {},
          onMuteUser = {},
          onUnmuteUser = {},
          onHideUser = {},
          onSetIncognitoDefault = {
            incognitoChanges++
          },
          onOpenIncognitoInfo = { incognitoInfo++ },
          onOpenNetworkSettings = { networkSettings++ },
          onOpenHome = {},
          onOpenContacts = { contacts++ },
        )
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p22_title),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p22_back),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p22_add_identity),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        target.getString(
          R.string.nome_p22_edit_current,
          "Lin",
        ),
      )
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p22_incognito),
      )
      .performScrollTo()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        target.getString(
          R.string.nome_p22_activate,
          "Work",
        ),
      )
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p22_incognito),
      )
      .performScrollTo()
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p22_network_settings,
        ),
      )
      .performScrollTo()
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p22_contacts),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()

    composeRule.runOnIdle {
      assertEquals(1, additions)
      assertEquals(1, edits)
      assertEquals(1, activations)
      assertEquals(1, closes)
      assertEquals(1, incognitoChanges)
      assertEquals(1, incognitoInfo)
      assertEquals(1, networkSettings)
      assertEquals(1, contacts)
    }
  }
}
