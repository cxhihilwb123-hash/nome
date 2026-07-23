package chat.simplex.app.nome.settings

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.model.User
import chat.simplex.common.model.UserOperatorServers
import chat.simplex.common.model.UserServer
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.common.views.usersettings.NomeBackupMigrationContent
import chat.simplex.common.views.usersettings.NomeSettingsHomeContent
import chat.simplex.common.views.usersettings.networkAndServers.hasCompleteNomeOfficialServerSet
import chat.simplex.res.MR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeSettingsBackupComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun settingsIndexDispatchesOnlyExistingRoutesAndFiltersLocally() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    val currentUser =
      User.sampleData.copy(
        userId = 81,
        profile =
          User.sampleData.profile.copy(
            displayName = "Lin",
            fullName = "Lin",
          ),
        activeUser = true,
      )
    var identities = 0
    var notifications = 0
    var backups = 0
    var desktops = 0
    var contacts = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeSettingsHomeContent(
          currentUser = currentUser,
          stopped = false,
          notificationsEnabled = true,
          languageCode = "zh-CN",
          onOpenIdentity = { identities++ },
          onOpenNotifications = { notifications++ },
          onOpenBackupMigration = { backups++ },
          onOpenDesktop = { desktops++ },
          onOpenPrivacy = {},
          onOpenNetwork = {},
          onOpenLanguage = {},
          onOpenAppearance = {},
          onOpenHelp = {},
          onOpenAbout = {},
          onOpenDeveloper = {},
          onOpenHome = {},
          onOpenContacts = { contacts++ },
        )
      }
    }

    composeRule
      .onNodeWithContentDescription(
        "Lin. " +
          target.getString(
            R.string.nome_p23_primary_identity_body,
          ),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p23_notifications,
        ),
      )
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p23_backup),
      )
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p23_desktop),
      )
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p23_contacts),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_primary_nav_settings),
      )
      .assertIsSelected()
      .assertHeightIsEqualTo(64.dp)
    composeRule
      .onNodeWithContentDescription(
        target.getString(
          R.string.nome_p23_search_action,
        ),
      )
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(R.string.nome_p23_search),
      )
      .performTextInput(
        target.getString(R.string.nome_p23_developer),
      )
    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p23_developer) +
          ". " +
          target.getString(
            R.string.nome_p23_developer_body,
          ),
      )
      .assertIsDisplayed()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p23_notifications,
        ),
      )
      .assertDoesNotExist()

    composeRule.runOnIdle {
      assertEquals(1, identities)
      assertEquals(1, notifications)
      assertEquals(1, backups)
      assertEquals(1, desktops)
      assertEquals(1, contacts)
    }
  }

  @Test
  fun androidSettingsBrandLabelsUseNomeResources() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext

    assertEquals(
      target.getString(R.string.nome_brand_chat_lock),
      generalGetString(MR.strings.chat_lock),
    )
    assertEquals(
      target.getString(R.string.nome_brand_lock_enabled),
      generalGetString(
        MR.strings.auth_simplex_lock_turned_on,
      ),
    )
    assertEquals(
      target.getString(R.string.nome_brand_terminal_client),
      generalGetString(
        MR.strings.install_simplex_chat_for_terminal,
      ),
    )
    assertEquals(
      target.getString(R.string.nome_brand_theme),
      generalGetString(MR.strings.theme_simplex),
    )
    assertEquals(
      target.getString(R.string.nome_brand_links),
      generalGetString(MR.strings.simplex_link_mode),
    )
    assertEquals(
      target.getString(R.string.nome_brand_invalid_scanned_link),
      generalGetString(
        MR.strings.code_you_scanned_is_not_simplex_link_qr_code,
      ),
    )
    assertEquals(
      target.getString(R.string.nome_brand_invalid_pasted_link),
      generalGetString(MR.strings.the_text_you_pasted_is_not_a_link),
    )
    assertEquals(
      target.getString(R.string.nome_brand_group_links_prohibited),
      generalGetString(MR.strings.simplex_links_are_prohibited_in_group),
    )
    assertEquals(
      target.getString(R.string.nome_official_server),
      generalGetString(MR.strings.your_servers),
    )
    assertEquals(
      target.getString(R.string.nome_official_server_help),
      generalGetString(MR.strings.how_to_use_your_servers),
    )
  }

  @Test
  fun officialServerSetRequiresEnabledMessageAndFileServers() {
    fun server(enabled: Boolean = true, deleted: Boolean = false) =
      UserServer(
        remoteHostId = null,
        serverId = null,
        server = "test-server",
        preset = false,
        enabled = enabled,
        deleted = deleted,
      )

    val messageOnly =
      UserOperatorServers(
        operator = null,
        smpServers = listOf(server()),
        xftpServers = emptyList(),
      )
    val complete =
      messageOnly.copy(xftpServers = listOf(server()))
    val deletedFileServer =
      messageOnly.copy(
        xftpServers = listOf(server(deleted = true)),
      )

    assertFalse(hasCompleteNomeOfficialServerSet(emptyList()))
    assertFalse(
      hasCompleteNomeOfficialServerSet(listOf(messageOnly)),
    )
    assertTrue(
      hasCompleteNomeOfficialServerSet(listOf(complete)),
    )
    assertFalse(
      hasCompleteNomeOfficialServerSet(
        listOf(deletedFileServer),
      ),
    )
  }

  @Test
  fun backupLandingRoutesToOfficialOwnersWithoutSyntheticProgress() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var closes = 0
    var archives = 0
    var migrations = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeBackupMigrationContent(
          onClose = { closes++ },
          onOpenArchive = { archives++ },
          onOpenMigration = { migrations++ },
        )
      }
    }

    composeRule
      .onNodeWithContentDescription(
        target.getString(R.string.nome_p24_back),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p24_archive_action,
        ),
      )
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p24_restore_title,
        ),
      )
      .performScrollTo()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p24_migrate_title,
        ),
      )
      .performScrollTo()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText("68%")
      .assertDoesNotExist()
    composeRule
      .onNodeWithText("Resume unfinished migration")
      .assertDoesNotExist()

    composeRule.runOnIdle {
      assertEquals(1, closes)
      assertEquals(2, archives)
      assertEquals(1, migrations)
    }
  }

  @Test
  fun stoppedChatKeepsRecoveryReachableButDisablesOutboundMigration() {
    val target =
      InstrumentationRegistry
        .getInstrumentation()
        .targetContext
    var archives = 0
    var migrations = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeBackupMigrationContent(
          migrationEnabled = false,
          onClose = {},
          onOpenArchive = { archives++ },
          onOpenMigration = { migrations++ },
        )
      }
    }

    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p24_archive_action,
        ),
      )
      .assertHasClickAction()
      .performClick()
    composeRule
      .onNodeWithText(
        target.getString(
          R.string.nome_p24_migrate_title,
        ),
      )
      .performScrollTo()
      .assertIsNotEnabled()

    composeRule.runOnIdle {
      assertEquals(1, archives)
      assertEquals(0, migrations)
    }
  }
}
