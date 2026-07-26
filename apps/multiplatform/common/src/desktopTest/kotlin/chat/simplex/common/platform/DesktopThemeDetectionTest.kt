package chat.simplex.common.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopThemeDetectionTest {
  @Test
  fun macOSDefaultsDarkValueSelectsDarkTheme() {
    assertTrue(macOSDefaultsIndicateDarkTheme(0, "Dark\n"))
    assertTrue(macOSDefaultsIndicateDarkTheme(0, "  dark  "))
  }

  @Test
  fun missingMacOSDefaultsValueFallsBackToLightTheme() {
    assertFalse(macOSDefaultsIndicateDarkTheme(1, "The domain/default pair does not exist"))
    assertFalse(macOSDefaultsIndicateDarkTheme(0, ""))
    assertFalse(macOSDefaultsIndicateDarkTheme(0, "Light"))
  }
}
