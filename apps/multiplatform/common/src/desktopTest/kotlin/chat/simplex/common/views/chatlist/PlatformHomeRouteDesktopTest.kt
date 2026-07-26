package chat.simplex.common.views.chatlist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformHomeRouteDesktopTest {
  @Test
  fun noFilterSelectsMessagesDestination() {
    assertEquals(
      NomeDesktopDestination.Messages,
      nomeDesktopDestination(activeFilter = null),
    )
  }

  @Test
  fun contactsFilterSelectsContactsDestination() {
    assertEquals(
      NomeDesktopDestination.Contacts,
      nomeDesktopDestination(
        ActiveFilter.PresetTag(PresetTagKind.CONTACTS),
      ),
    )
  }

  @Test
  fun nonContactFilterKeepsMessagesDestination() {
    assertEquals(
      NomeDesktopDestination.Messages,
      nomeDesktopDestination(
        ActiveFilter.PresetTag(PresetTagKind.GROUPS),
      ),
    )
  }

  @Test
  fun desktopKeepsLocalNotesVisibleWhileTheCenterPaneShowsOnboarding() {
    assertFalse(
      showOnboardingInsteadOfChatList(
        showOnboarding = true,
        isAndroid = false,
      ),
    )
  }

  @Test
  fun androidRetainsItsInlineOnboardingCards() {
    assertTrue(
      showOnboardingInsteadOfChatList(
        showOnboarding = true,
        isAndroid = true,
      ),
    )
  }
}
