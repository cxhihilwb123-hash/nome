package chat.simplex.app.nome.harness

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import chat.simplex.app.nome.fixtures.NomeFixtureSpec
import chat.simplex.app.nome.fixtures.NomeFixtureTheme
import chat.simplex.app.nome.fixtures.NomeFoundationFixture
import java.util.Locale

class NomeFoundationActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val spec = NomeFixtureSpec.from(intent)
    configureWindow(spec.theme)
    val localizedConfiguration = Configuration(resources.configuration).apply {
      setLocale(spec.locale.locale)
      setLayoutDirection(spec.locale.locale)
      fontScale = spec.fontScale
    }
    val localizedContext = createConfigurationContext(localizedConfiguration)
    val displayDensity = localizedContext.resources.displayMetrics.density

    setContent {
      CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalDensity provides Density(displayDensity, spec.fontScale),
      ) {
        NomeFoundationFixture(spec)
      }
    }
  }

  private fun configureWindow(theme: NomeFixtureTheme) {
    val dark = theme == NomeFixtureTheme.DARK
    val systemBarStyle =
      if (dark) {
        SystemBarStyle.dark(Color.TRANSPARENT)
      } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
      }
    enableEdgeToEdge(
      statusBarStyle = systemBarStyle,
      navigationBarStyle = systemBarStyle,
    )
    WindowCompat.getInsetsController(window, window.decorView).apply {
      isAppearanceLightStatusBars = !dark
      isAppearanceLightNavigationBars = !dark
    }
  }

  companion object {
    internal fun intent(context: Context, spec: NomeFixtureSpec): Intent =
      Intent(context, NomeFoundationActivity::class.java).apply {
        putExtra(NomeFixtureSpec.EXTRA_PAGE, spec.page.name)
        putExtra(NomeFixtureSpec.EXTRA_LOCALE, spec.locale.languageTag)
        putExtra(NomeFixtureSpec.EXTRA_THEME, spec.theme.name.lowercase(Locale.ROOT))
        putExtra(NomeFixtureSpec.EXTRA_STATE, spec.state.name.lowercase(Locale.ROOT))
        putExtra(NomeFixtureSpec.EXTRA_FONT_SCALE, spec.fontScale.toInt().toString())
        putExtra(NomeFixtureSpec.EXTRA_RENDER_STATE_PANEL, spec.renderStatePanel)
      }
  }
}
