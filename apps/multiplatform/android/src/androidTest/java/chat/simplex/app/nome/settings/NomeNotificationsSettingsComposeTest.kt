package chat.simplex.app.nome.settings

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.common.model.NotificationPreviewMode
import chat.simplex.common.model.NotificationsMode
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.usersettings.NomeNotificationPreviewChoice
import chat.simplex.common.views.usersettings.NomeNotificationPreviewContent
import chat.simplex.common.views.usersettings.NomeNotificationsModeChoice
import chat.simplex.common.views.usersettings.NomeNotificationsModeContent
import chat.simplex.common.views.usersettings.NomeNotificationsSettingsContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeNotificationsSettingsComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun mainRouteKeepsOfficialPreferenceEntriesReachable() {
    var modeOpens = 0
    var previewOpens = 0

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeNotificationsSettingsContent(
          title = "Notifications",
          backLabel = "Back",
          modeTitle = "Notification service",
          modeValue = "Off",
          modeDescription = "Only while the app is open",
          previewTitle = "Show preview",
          previewValue = "Message text",
          previewDescription = "Show sender and message text",
          onClose = {},
          onOpenMode = { modeOpens++ },
          onOpenPreview = { previewOpens++ },
        )
      }
    }

    composeRule
      .onNodeWithText("Notification service")
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule
      .onNodeWithText("Show preview")
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
    composeRule.runOnIdle {
      assertEquals(1, modeOpens)
      assertEquals(1, previewOpens)
    }
    composeRule
      .onNodeWithContentDescription("Back")
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
  }

  @Test
  fun notificationModeRouteDispatchesOnlyTheSelectedOfficialValue() {
    var selectedMode: NotificationsMode? = null

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeNotificationsModeContent(
          title = "Notification service",
          backLabel = "Back",
          choices =
            listOf(
              NomeNotificationsModeChoice(NotificationsMode.OFF, "Off", "Only while open"),
              NomeNotificationsModeChoice(NotificationsMode.SERVICE, "Service", "Background service"),
            ),
          selected = NotificationsMode.OFF,
          onClose = {},
          onSelected = { selectedMode = it },
        )
      }
    }

    composeRule.onNodeWithText("Off").assertIsSelected()
    composeRule.onNodeWithText("Service").performClick()
    composeRule.runOnIdle { assertEquals(NotificationsMode.SERVICE, selectedMode) }
  }

  @Test
  fun notificationPreviewRouteDispatchesOnlyTheSelectedOfficialValue() {
    var selectedPreview: NotificationPreviewMode? = null
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeNotificationPreviewContent(
          title = "Show preview",
          backLabel = "Back",
          choices =
            listOf(
              NomeNotificationPreviewChoice(NotificationPreviewMode.MESSAGE, "Message", "Show text"),
              NomeNotificationPreviewChoice(NotificationPreviewMode.HIDDEN, "Hidden", "Hide text"),
            ),
          selected = NotificationPreviewMode.MESSAGE,
          onClose = {},
          onSelected = { selectedPreview = it },
        )
      }
    }

    composeRule.onNodeWithText("Message").assertIsSelected()
    composeRule.onNodeWithText("Hidden").performClick()
    composeRule.runOnIdle { assertEquals(NotificationPreviewMode.HIDDEN, selectedPreview) }
  }
}
