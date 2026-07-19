package chat.simplex.app.nome.connection

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
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
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.chat.NomeContactVerificationContent
import chat.simplex.common.views.newchat.NomeChannelSetupContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeSecurityAndChannelComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun contactVerificationKeepsScanAndManualAttestationDistinct() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var scans = 0
    var marks = 0
    var clears = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeContactVerificationContent(
          displayName = "Chen Yu",
          profileImage = null,
          connectionCode =
            "7024 9185 3361 5248 6804 1179 2350 4426",
          connectionVerified = false,
          onScanCode = { scans++ },
          onMarkVerified = { marks++ },
          onClearVerification = { clears++ },
          onShareCode = {},
          onClose = {},
        )
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p19_unverified),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p19_scan_action),
      )
      .performScrollTo()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        target.getString(
          R.string.nome_p19_mark_verified,
        ),
      )
      .performScrollTo()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, scans)
      assertEquals(1, marks)
      assertEquals(0, clears)
    }
  }

  @Test
  fun verifiedContactOffersOnlyExplicitClearAction() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var clears = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeContactVerificationContent(
          displayName = "Chen Yu",
          profileImage = null,
          connectionCode =
            "7024 9185 3361 5248 6804 1179 2350 4426",
          connectionVerified = true,
          onScanCode = {},
          onMarkVerified = {},
          onClearVerification = { clears++ },
          onShareCode = {},
          onClose = {},
        )
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p19_scan_action,
        ),
      )
      .assertDoesNotExist()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p19_clear_verification,
        ),
      )
      .performScrollTo()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, clears)
    }
  }

  @Test
  fun channelSetupUsesOfficialLinkPhaseAndRealRelayConfiguration() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var joins = 0
    var configures = 0
    var creates = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        val name =
          remember {
            mutableStateOf("Nome Product")
          }
        NomeChannelSetupContent(
          displayName = name,
          profileImage = null,
          focusRequester = remember {
            FocusRequester()
          },
          enabledRelayCount = 2,
          hasRelays = true,
          creationInProgress = false,
          canCreate = true,
          currentProfileName = "Lin",
          onEditImage = {},
          onDeleteImage = {},
          onShowInvalidName = {},
          onConfigureRelays = { configures++ },
          onCreateChannel = { creates++ },
          onOpenJoinChannel = { joins++ },
          onClose = {},
        )
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p20_join_tab),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p20_link_after_creation,
        ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p20_enabled_relays,
          2,
        ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p20_configure_relays,
        ),
      )
      .performScrollTo()
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p20_create),
      )
      .performScrollTo()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        "nome.news",
        substring = true,
      )
      .assertDoesNotExist()
    composeRule.runOnIdle {
      assertEquals(1, joins)
      assertEquals(1, configures)
      assertEquals(1, creates)
    }
  }
}
