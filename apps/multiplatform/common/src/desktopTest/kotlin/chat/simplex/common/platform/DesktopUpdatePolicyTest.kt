package chat.simplex.common.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopUpdatePolicyTest {
  @Test
  fun nomeMacBuildDoesNotUseUpstreamSimpleXUpdateFeed() {
    assertFalse(desktopUpdateFeedAvailable(DesktopPlatform.MAC_AARCH64))
    assertFalse(desktopUpdateFeedAvailable(DesktopPlatform.MAC_X86_64))
  }

  @Test
  fun unchangedDesktopTargetsKeepTheirExistingUpdateFeed() {
    assertTrue(desktopUpdateFeedAvailable(DesktopPlatform.LINUX_X86_64))
    assertTrue(desktopUpdateFeedAvailable(DesktopPlatform.LINUX_AARCH64))
    assertTrue(desktopUpdateFeedAvailable(DesktopPlatform.WINDOWS_X86_64))
  }
}
