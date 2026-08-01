package chat.simplex.common.model

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopChannelCreationCommandTest {
  @Test
  fun desktopChannelCanRequestFullShortLink() {
    val command = CC.ApiNewPublicGroup(
      userId = 7,
      incognito = false,
      relayIds = listOf(11, 12),
      groupProfile = GroupProfile.sampleData,
      fullShortLink = true,
    )

    assertContains(command.cmdString, " incognito=off full_link=on 11,12 ")
  }

  @Test
  fun defaultCommandRemainsWireCompatible() {
    val command = CC.ApiNewPublicGroup(
      userId = 7,
      incognito = false,
      relayIds = listOf(11, 12),
      groupProfile = GroupProfile.sampleData,
    )

    assertFalse(command.cmdString.contains("full_link="))
    assertTrue(command.cmdString.startsWith("/_public group 7 incognito=off 11,12 "))
  }
}
