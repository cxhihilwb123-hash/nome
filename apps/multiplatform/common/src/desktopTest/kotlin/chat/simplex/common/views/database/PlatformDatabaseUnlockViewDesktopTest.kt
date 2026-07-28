package chat.simplex.common.views.database

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformDatabaseUnlockViewDesktopTest {
  @Test
  fun brandPanelAppearsOnlyAtComfortableDesktopWidths() {
    assertFalse(shouldShowNomeUnlockBrandPanel(640f))
    assertFalse(shouldShowNomeUnlockBrandPanel(899.99f))
    assertTrue(shouldShowNomeUnlockBrandPanel(900f))
    assertTrue(shouldShowNomeUnlockBrandPanel(1364f))
  }

  @Test
  fun returnKeysSubmitOnlyOnKeyDown() {
    assertTrue(shouldSubmitNomeUnlock(Key.Enter, KeyEventType.KeyDown))
    assertTrue(shouldSubmitNomeUnlock(Key.NumPadEnter, KeyEventType.KeyDown))
    assertFalse(shouldSubmitNomeUnlock(Key.Enter, KeyEventType.KeyUp))
    assertFalse(shouldSubmitNomeUnlock(Key.Spacebar, KeyEventType.KeyDown))
  }
}
