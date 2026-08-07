package chat.simplex.app.nome.database

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.ui.nome.database.NomeDatabaseRootActions
import chat.simplex.common.ui.nome.database.NomeDatabaseRootRoute
import chat.simplex.common.ui.nome.database.NomeDatabaseRootState
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeDatabaseRootComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun randomKeyFailureHasNoManualEntryAndOnlyBoundedRecovery() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val passphrase = mutableStateOf("")
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeDatabaseRootRoute(
          state = NomeDatabaseRootState.StoredRandomKeyUnavailable(
            submitting = false,
            matchedBackupAvailable = true,
          ),
          passphrase = passphrase,
          actions = actions(),
        )
      }
    }

    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_random_key_title),
    ).assertIsDisplayed()
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_copy_backup),
    )
      .performScrollTo()
      .assertIsDisplayed()
      .assertIsEnabled()
      .assertHeightIsAtLeast(48.dp)
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_passphrase_label),
    ).assertDoesNotExist()
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_open_once),
    ).assertDoesNotExist()
    composeRule.onNodeWithText("68%").assertDoesNotExist()
    composeRule.onNodeWithText("timeout", substring = true)
      .assertDoesNotExist()
  }

  @Test
  fun manualKeyRecoveryKeepsPasswordSemanticsAndActionsAtTwoHundredPercent() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val passphrase = mutableStateOf("")
    composeRule.setContent {
      CompositionLocalProvider(
        LocalDensity provides Density(
          density = LocalDensity.current.density,
          fontScale = 2f,
        ),
      ) {
        NomeAndroidTheme(darkTheme = true) {
          NomeDatabaseRootRoute(
            state = NomeDatabaseRootState.StoredManualKeyUnreadable(
              submitting = false,
              matchedBackupAvailable = false,
            ),
            passphrase = passphrase,
            actions = actions(),
          )
        }
      }
    }

    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.Password,
        Unit,
      ),
    )
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_open_once),
    )
      .performScrollTo()
      .assertIsDisplayed()
      .assertHeightIsAtLeast(48.dp)
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_save_and_open),
    )
      .performScrollTo()
      .assertIsDisplayed()
      .assertHeightIsAtLeast(48.dp)
  }

  @Test
  fun dangerAndLoadingExposeOnePoliteOrAssertiveStatus() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val state = mutableStateOf<NomeDatabaseRootState>(
      NomeDatabaseRootState.Opening(submitting = false),
    )
    val passphrase = mutableStateOf("")
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeDatabaseRootRoute(
          state = state.value,
          passphrase = passphrase,
          actions = actions(),
        )
      }
    }

    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.LiveRegion,
        LiveRegionMode.Polite,
      ),
    ).assertIsDisplayed()

    composeRule.runOnIdle {
      state.value = NomeDatabaseRootState.StoredRandomKeyUnavailable(
        submitting = false,
        matchedBackupAvailable = false,
      )
    }
    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.LiveRegion,
        LiveRegionMode.Assertive,
      ).and(
        SemanticsMatcher.expectValue(
          SemanticsProperties.StateDescription,
          target.getString(R.string.nome_database_root_waiting),
        ),
      ),
    ).assertIsDisplayed()
  }

  @Test
  fun passphraseClearsWhenKeyEntryLeavesTheCurrentState() {
    val passphrase = mutableStateOf(String(CharArray(32) { 'x' }))
    val state = mutableStateOf<NomeDatabaseRootState>(
      NomeDatabaseRootState.AlternateKeyRequired(
        submitting = false,
        matchedBackupAvailable = false,
        storedKeyMaterialPresent = false,
        storedKeyUseRequested = false,
      ),
    )
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeDatabaseRootRoute(
          state = state.value,
          passphrase = passphrase,
          actions = actions(),
        )
      }
    }

    composeRule.runOnIdle {
      state.value = NomeDatabaseRootState.UnknownFailure(
        submitting = false,
        matchedBackupAvailable = false,
      )
    }
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertTrue(passphrase.value.isEmpty())
    }
  }

  private fun actions() = NomeDatabaseRootActions(
    openOnce = {},
    saveAndOpen = {},
    confirmUpgrade = {},
    confirmDowngrade = {},
    copyMatchedBackup = {},
    openRestored = {},
  )
}
