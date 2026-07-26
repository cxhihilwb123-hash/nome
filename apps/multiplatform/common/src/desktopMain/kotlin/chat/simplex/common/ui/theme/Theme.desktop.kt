package chat.simplex.common.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme as composeIsSystemInDarkTheme
import androidx.compose.runtime.Composable
import chat.simplex.common.platform.desktopPlatform
import chat.simplex.common.platform.Log
import chat.simplex.common.platform.TAG
import com.jthemedetecor.OsThemeDetector

private val nonMacThemeDetector: OsThemeDetector? by lazy {
  if (desktopPlatform.isMac()) {
    null
  } else {
    try {
      OsThemeDetector.getDetector().apply {
        registerListener(::reactOnDarkThemeChanges)
      }
    } catch (e: Exception) {
      Log.e(TAG, e.stackTraceToString())
      null
    }
  }
}

@Composable
actual fun isSystemInDarkTheme(): Boolean =
  if (desktopPlatform.isMac()) {
    composeIsSystemInDarkTheme()
  } else {
    try {
      nonMacThemeDetector?.isDark ?: false
    } catch (e: Exception) {
      Log.e(TAG, e.stackTraceToString())
      false
    }
  }
