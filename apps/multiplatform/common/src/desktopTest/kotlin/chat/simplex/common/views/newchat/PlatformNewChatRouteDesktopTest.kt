package chat.simplex.common.views.newchat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformNewChatRouteDesktopTest {
  @Test
  fun invitationPhasePrefersReadyLinkOverTransientCreatingFlag() {
    assertEquals(
      DesktopInvitationPhase.Ready,
      desktopInvitationPhase(
        fullLink = "smp://invitation",
        creating = true,
      ),
    )
  }

  @Test
  fun invitationPhaseDistinguishesCreatingAndRetryStates() {
    assertEquals(
      DesktopInvitationPhase.Creating,
      desktopInvitationPhase(fullLink = "", creating = true),
    )
    assertEquals(
      DesktopInvitationPhase.Retry,
      desktopInvitationPhase(fullLink = "", creating = false),
    )
  }

  @Test
  fun connectionSubmissionRequiresVisibleLinkContent() {
    assertFalse(desktopConnectionSubmitEnabled(""))
    assertFalse(desktopConnectionSubmitEnabled("  \n\t"))
    assertTrue(desktopConnectionSubmitEnabled(" simplex:/contact "))
  }
}
