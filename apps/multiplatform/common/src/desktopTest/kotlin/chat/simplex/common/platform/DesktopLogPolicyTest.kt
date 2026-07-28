package chat.simplex.common.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopLogPolicyTest {
  @Test
  fun releaseModeSuppressesEveryLogLevel() {
    LogLevel.entries.forEach { level ->
      assertFalse(shouldLogDesktop(level, LogLevel.DEBUG, developerTools = false))
    }
  }

  @Test
  fun developerModeHonorsConfiguredThreshold() {
    assertFalse(shouldLogDesktop(LogLevel.DEBUG, LogLevel.WARNING, developerTools = true))
    assertFalse(shouldLogDesktop(LogLevel.INFO, LogLevel.WARNING, developerTools = true))
    assertTrue(shouldLogDesktop(LogLevel.WARNING, LogLevel.WARNING, developerTools = true))
    assertTrue(shouldLogDesktop(LogLevel.ERROR, LogLevel.WARNING, developerTools = true))
  }
}
