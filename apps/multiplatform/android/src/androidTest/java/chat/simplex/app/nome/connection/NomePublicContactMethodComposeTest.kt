package chat.simplex.app.nome.connection

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.model.AddressSettings
import chat.simplex.common.model.CreatedConnLink
import chat.simplex.common.model.UserContactLinkRec
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.usersettings.NomePublicContactMethodContent
import chat.simplex.common.views.usersettings.NomeUserAddressLoadState
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomePublicContactMethodComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun addressIsNotExposedInSemanticsAndBusyToggleBlocksClose() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    val address = fixtureAddress()
    val result = CompletableDeferred<Boolean>()
    var confirmationChanges = 0
    var shortLinkAdds = 0
    var shares = 0
    var closes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomePublicContactMethodContent(
          userAddress = address,
          loadState = NomeUserAddressLoadState.READY,
          onReload = {},
          onCreate = { false },
          onSetRequiresConfirmation = {
            confirmationChanges++
            result.await()
          },
          onDelete = { false },
          onAddShortLink = {
            shortLinkAdds++
          },
          onShareAddress = {
            shares++
          },
          onOpenAdvanced = {},
          onClose = { closes++ },
        )
      }
    }

    composeRule
      .onNodeWithText(
        address.connLinkContact.simplexChatUri(
          short = true,
        ),
      )
      .assertDoesNotExist()
    composeRule
      .onNodeWithContentDescription(
        target.getString(
          R.string.nome_p15_address_label,
        ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p15_copy),
      )
      .assertHeightIsAtLeast(48.dp)
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p15_add_short_link,
        ),
      )
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p15_share),
      )
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, shortLinkAdds)
      assertEquals(1, shares)
    }
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p15_confirmation,
        ),
      )
      .performClick()
    composeRule.waitUntil {
      confirmationChanges == 1
    }

    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p15_replace),
      )
      .assertIsNotEnabled()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p15_back),
      )
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, confirmationChanges)
      assertEquals(0, closes)
    }

    result.complete(false)
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p15_action_failed,
        ),
      )
      .assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(1, confirmationChanges)
      assertEquals(0, closes)
    }
  }

  @Test
  fun replaceRequiresConfirmationAndRetryDoesNotDeleteTwice() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    val address =
      mutableStateOf<UserContactLinkRec?>(
        fixtureAddress(),
      )
    var deletes = 0
    var creates = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomePublicContactMethodContent(
          userAddress = address.value,
          loadState =
            if (address.value == null) {
              NomeUserAddressLoadState.OFF
            } else {
              NomeUserAddressLoadState.READY
            },
          onReload = {},
          onCreate = {
            creates++
            false
          },
          onSetRequiresConfirmation = { false },
          onDelete = {
            deletes++
            address.value = null
            true
          },
          onOpenAdvanced = {},
          onClose = {},
        )
      }
    }

    val replaceLabel =
      target.getString(R.string.nome_p15_replace) +
        ". " +
        target.getString(
          R.string.nome_p15_replace_body,
        )
    composeRule
      .onNodeWithContentDescription(replaceLabel)
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(0, deletes)
      assertEquals(0, creates)
    }

    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p15_replace_confirm,
        ),
      )
      .performClick()
    composeRule.waitUntil {
      deletes == 1 && creates == 1
    }
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p15_replace_deleted,
        ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p15_replace_retry,
        ),
      )
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.waitUntil { creates == 2 }
    composeRule.runOnIdle {
      assertEquals(1, deletes)
      assertEquals(2, creates)
    }
  }

  @Test
  fun closeAddressDoesNotDeleteUntilConfirmed() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    var deletes = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomePublicContactMethodContent(
          userAddress = fixtureAddress(),
          loadState = NomeUserAddressLoadState.READY,
          onReload = {},
          onCreate = { false },
          onSetRequiresConfirmation = { false },
          onDelete = {
            deletes++
            false
          },
          onOpenAdvanced = {},
          onClose = {},
        )
      }
    }

    val deleteLabel =
      target.getString(R.string.nome_p15_disable) +
        ". " +
        target.getString(
          R.string.nome_p15_disable_body,
        )
    composeRule
      .onNodeWithContentDescription(deleteLabel)
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(0, deletes)
    }
    composeRule
      .onAllNodesWithText(
        target.getString(R.string.nome_p15_disable),
      )[1]
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p15_action_failed,
        ),
      )
      .assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(1, deletes)
    }
  }

  private fun fixtureAddress() =
    UserContactLinkRec(
      connLinkContact =
        CreatedConnLink(
          connFullLink =
            "https://simplex.chat/contact#/?v=1&smp=fixture",
          connShortLink = null,
        ),
      shortLinkDataSet = false,
      shortLinkLargeDataSet = false,
      addressSettings =
        AddressSettings(
          businessAddress = false,
          autoAccept = null,
          autoReply = null,
        ),
    )
}
