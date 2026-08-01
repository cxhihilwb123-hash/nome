package chat.simplex.common.views.chatlist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NomeDesktopGroupBadgeTest {
  @Test
  fun desktopChatListDistinguishesGroupsAndChannels() {
    assertEquals(NomeDesktopGroupBadge.Group, nomeDesktopGroupBadge(isDesktop = true, useRelays = false))
    assertEquals(NomeDesktopGroupBadge.Channel, nomeDesktopGroupBadge(isDesktop = true, useRelays = true))
  }

  @Test
  fun otherPlatformsRemainUnchanged() {
    assertNull(nomeDesktopGroupBadge(isDesktop = false, useRelays = false))
    assertNull(nomeDesktopGroupBadge(isDesktop = false, useRelays = true))
  }
}
