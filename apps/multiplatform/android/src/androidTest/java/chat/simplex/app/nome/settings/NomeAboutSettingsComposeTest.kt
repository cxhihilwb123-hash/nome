package chat.simplex.app.nome.settings

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
import chat.simplex.common.views.usersettings.NomeAboutSettingsContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeAboutSettingsComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun aboutPageShowsTruthfulVersionAndDispatchesOfficialOwners() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var closes = 0
    var versions = 0
    var sources = 0
    var licenses = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeAboutSettingsContent(
          appVersionName = "6.5.6",
          appVersionCode = 358,
          onClose = { closes++ },
          onOpenVersion = { versions++ },
          onOpenSource = { sources++ },
          onOpenLicense = { licenses++ },
        )
      }
    }

    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_back),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_about_version,
          "6.5.6",
          358,
        ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_about_version_title,
        ),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_about_source_title,
        ),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_about_license_title,
        ),
      )
      .performScrollTo()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()

    composeRule.runOnIdle {
      assertEquals(1, closes)
      assertEquals(1, versions)
      assertEquals(1, sources)
      assertEquals(1, licenses)
    }
  }
}
