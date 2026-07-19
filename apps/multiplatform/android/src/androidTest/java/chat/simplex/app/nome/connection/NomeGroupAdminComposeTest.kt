package chat.simplex.app.nome.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.chat.group.NomeAddGroupMembersContent
import chat.simplex.common.views.chat.group.NomeGroupChatInfoContent
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.common.views.newchat.NomeAddGroupContent
import chat.simplex.res.MR
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeGroupAdminComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun privateGroupCreationKeepsPreferenceAndSubmitOwners() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var incognitoChanges = 0
    var creates = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        val name = remember { mutableStateOf("Nome Group") }
        val incognito = remember { mutableStateOf(false) }
        NomeAddGroupContent(
          displayName = name,
          profileImage = null,
          focusRequester = remember { FocusRequester() },
          incognito = incognito,
          canCreate = true,
          profileDisclosure = "Current profile will be shared",
          onEditImage = {},
          onDeleteImage = {},
          onShowInvalidName = {},
          onShowIncognitoInfo = {},
          onIncognitoChange = {
            incognitoChanges++
            incognito.value = it
          },
          onCreateGroup = { creates++ },
          onClose = {},
        )
      }
    }

    composeRule
      .onNodeWithText("Current profile will be shared")
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        generalGetString(MR.strings.incognito),
      )
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        generalGetString(MR.strings.create_group_button),
      )
      .assertIsEnabled()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_back),
      )
      .assertHeightIsAtLeast(48.dp)
    composeRule.runOnIdle {
      assertEquals(1, incognitoChanges)
      assertEquals(1, creates)
    }
  }

  @Test
  fun inviteMembersFrameKeepsSetupContactsAndBackReachable() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var setupActions = 0
    var contactActions = 0
    var closes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeAddGroupMembersContent(
          title = "Invite members",
          hasContacts = true,
          onClose = { closes++ },
          profileContent = {
            Text("Controlled group")
          },
          setupContent = {
            Button(
              onClick = { setupActions++ },
              modifier =
                Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp),
            ) {
              Text("Admission settings")
            }
          },
          selectionFooterContent = {
            Text("No contacts selected")
          },
          contactsContent = {
            Button(
              onClick = { contactActions++ },
              modifier =
                Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp),
            ) {
              Text("Controlled contact")
            }
          },
          emptyContent = {
            Text("No eligible contacts")
          },
        )
      }
    }

    composeRule
      .onNodeWithText("Controlled group")
      .assertIsDisplayed()
    composeRule
      .onNodeWithText("Admission settings")
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText("Controlled contact")
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_back),
      )
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, setupActions)
      assertEquals(1, contactActions)
      assertEquals(1, closes)
    }
  }

  @Test
  fun groupInfoFrameUsesOneTopBackOwnerAndKeepsContent() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var closes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeGroupChatInfoContent(
          title = "Controlled group",
          onClose = { closes++ },
        ) {
          Column {
            Text("Official group actions")
            Text("Official member list")
          }
        }
      }
    }

    composeRule
      .onNodeWithText("Controlled group")
      .assertIsDisplayed()
    composeRule
      .onNodeWithText("Official group actions")
      .assertIsDisplayed()
    composeRule
      .onNodeWithText("Official member list")
      .assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_back),
      )
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, closes)
    }
  }
}
