package chat.simplex.app.nome.newchat

import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.model.CreatedConnLink
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.newchat.NomeOneTimeInvitationContent
import chat.simplex.common.views.newchat.NomeScanPasteContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeNewChatRouteComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun invitationActionsStayLocalAndNeverClaimExpiryOrRegeneration() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    val invitation =
      CreatedConnLink(
        connFullLink =
          "https://simplex.chat/contact#/?v=1&smp=fixture",
        connShortLink = null,
      )
    var copied = 0
    var shared = 0
    var qrShared = 0
    var profileOpened = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeOneTimeInvitationContent(
          invitation = invitation,
          invitationCreating = false,
          currentProfileName = "Lin",
          onOpenProfile = { profileOpened++ },
          onCopyLink = { copied++ },
          onShareLink = { shared++ },
          onShareQr = { qrShared++ },
          onQrImageShared = {},
          onRetryInvitation = {},
          onClose = {},
        )
      }
    }

    composeRule
      .onNodeWithContentDescription(
        target.getString(
          R.string.nome_p11_profile_action,
          "Lin",
        ),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle { assertEquals(1, profileOpened) }

    composeRule
      .onNodeWithText(
        invitation.simplexChatUri(short = true),
      )
      .assertDoesNotExist()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p11_link_label),
      )
      .assertIsDisplayed()

    composeRule
      .onNodeWithText(target.getString(R.string.nome_p11_copy))
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle { assertEquals(1, copied) }

    composeRule
      .onNodeWithText(target.getString(R.string.nome_p11_share))
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle { assertEquals(1, shared) }

    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p11_share_invitation,
        ),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle { assertEquals(1, qrShared) }

    composeRule
      .onNodeWithText("24 hours", substring = true)
      .assertDoesNotExist()
    composeRule
      .onNodeWithText("Regenerate", substring = true)
      .assertDoesNotExist()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p11_shared_local),
      )
      .assertIsDisplayed()
  }

  @Test
  fun scannerRequiresExplicitActionAndPasteDispatchesOnce() {
    val target =
      InstrumentationRegistry.getInstrumentation().targetContext
    val pasted = mutableStateOf("")
    var scannerOpened = 0
    var submitted = 0
    var clipboardRead = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeScanPasteContent(
          pastedLink = pasted.value,
          onPastedLinkChange = {
            pasted.value = it
          },
          onSubmitPastedLink = {
            submitted++
          },
          onReadClipboard = {
            clipboardRead++
          },
          scannerContent = {
            Text("scanner fixture")
          },
          onOpenScanner = {
            scannerOpened++
          },
          onClose = {},
        )
      }
    }

    composeRule
      .onNodeWithText("scanner fixture")
      .assertDoesNotExist()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p12_open_camera),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, scannerOpened)
    }
    composeRule
      .onNodeWithText("scanner fixture")
      .assertIsDisplayed()

    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p12_paste_tab),
      )
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p12_link_input),
      )
      .performTextInput(
        "https://simplex.chat/contact#/?v=1",
      )
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p12_check),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, submitted)
    }

    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p12_clipboard_action,
        ),
      )
      .assertHasClickAction()
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, clipboardRead)
    }
  }
}
