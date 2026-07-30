package chat.simplex.app.nome.developer

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.nome.testing.launchNomePhysicalActivityHost
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.TerminalLayout
import chat.simplex.common.views.chat.ComposeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeTerminalViewComposeTest {
  @Test
  fun terminalRendersWithoutRequestingAnUnattachedComposeFocusTarget() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity: ComponentActivity = launchNomePhysicalActivityHost(instrumentation)
    try {
      instrumentation.runOnMainSync {
        activity.setContent {
          NomeAndroidTheme(darkTheme = false) {
            val composeState = remember {
              mutableStateOf(ComposeState(useLinkPreviews = false))
            }
            TerminalLayout(
              composeState = composeState,
              floating = false,
              sendCommand = {},
            )
          }
        }
      }
      instrumentation.waitForIdleSync()
      SystemClock.sleep(500)
      instrumentation.waitForIdleSync()
      val root = instrumentation.uiAutomation.rootInActiveWindow
      assertNotNull("Terminal activity must expose an accessibility root", root)
      assertEquals(instrumentation.targetContext.packageName, root.packageName?.toString())
      assertTrue("Terminal content must attach to the activity window", root.childCount > 0)
    } finally {
      instrumentation.runOnMainSync { activity.finish() }
    }
  }
}
