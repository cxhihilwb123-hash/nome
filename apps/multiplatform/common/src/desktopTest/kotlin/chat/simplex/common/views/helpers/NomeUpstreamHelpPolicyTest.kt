package chat.simplex.common.views.helpers

import chat.simplex.common.platform.AppPlatform
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NomeUpstreamHelpPolicyTest {
  @Test
  fun desktopHidesUpstreamSimpleXHelpLinks() {
    assertFalse(shouldShowUpstreamSimpleXHelpLink(AppPlatform.DESKTOP))
  }

  @Test
  fun androidKeepsExistingHelpLinks() {
    assertTrue(shouldShowUpstreamSimpleXHelpLink(AppPlatform.ANDROID))
  }
}
