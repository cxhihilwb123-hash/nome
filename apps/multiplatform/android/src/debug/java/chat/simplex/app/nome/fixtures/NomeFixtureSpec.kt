package chat.simplex.app.nome.fixtures

import android.content.Intent
import chat.simplex.common.ui.nome.components.NomeStatePanelState
import java.util.Locale

internal enum class NomeFixturePage {
  P02,
  P07,
  P17,
  P21,
  P23;

  companion object {
    fun from(value: String?): NomeFixturePage = entries.firstOrNull {
      it.name.equals(value, ignoreCase = true)
    } ?: P07
  }
}

internal enum class NomeFixtureLocale(val languageTag: String) {
  EN("en"),
  ZH_CN("zh-CN");

  val locale: Locale
    get() = Locale.forLanguageTag(languageTag)

  companion object {
    fun from(value: String?): NomeFixtureLocale = when (value?.lowercase(Locale.ROOT)) {
      "zh-cn", "zh_rcn", "zh-rcn" -> ZH_CN
      else -> EN
    }
  }
}

internal enum class NomeFixtureTheme {
  LIGHT,
  DARK;

  companion object {
    fun from(value: String?): NomeFixtureTheme = if (value.equals("dark", ignoreCase = true)) {
      DARK
    } else {
      LIGHT
    }
  }
}

internal enum class NomeFixtureState(val panelState: NomeStatePanelState) {
  NORMAL(NomeStatePanelState.NORMAL),
  LOADING(NomeStatePanelState.LOADING),
  EMPTY(NomeStatePanelState.EMPTY),
  OFFLINE(NomeStatePanelState.OFFLINE),
  ERROR(NomeStatePanelState.ERROR),
  PERMISSION(NomeStatePanelState.PERMISSION),
  DANGER(NomeStatePanelState.DANGER);

  companion object {
    fun from(value: String?): NomeFixtureState = entries.firstOrNull {
      it.name.equals(value, ignoreCase = true)
    } ?: NORMAL
  }
}

internal data class NomeFixtureSpec(
  val page: NomeFixturePage,
  val locale: NomeFixtureLocale,
  val theme: NomeFixtureTheme,
  val state: NomeFixtureState,
  val fontScale: Float,
  val renderStatePanel: Boolean = false,
) {
  companion object {
    fun from(intent: Intent): NomeFixtureSpec = NomeFixtureSpec(
      page = NomeFixturePage.from(intent.getStringExtra(EXTRA_PAGE)),
      locale = NomeFixtureLocale.from(intent.getStringExtra(EXTRA_LOCALE)),
      theme = NomeFixtureTheme.from(intent.getStringExtra(EXTRA_THEME)),
      state = NomeFixtureState.from(intent.getStringExtra(EXTRA_STATE)),
      fontScale = intent.getStringExtra(EXTRA_FONT_SCALE)
        ?.toFloatOrNull()
        ?.takeIf { it == 2f }
        ?: 1f,
      renderStatePanel = intent.getBooleanExtra(EXTRA_RENDER_STATE_PANEL, false),
    )

    const val EXTRA_PAGE = "page"
    const val EXTRA_LOCALE = "locale"
    const val EXTRA_THEME = "theme"
    const val EXTRA_STATE = "state"
    const val EXTRA_FONT_SCALE = "fontScale"
    const val EXTRA_RENDER_STATE_PANEL = "renderStatePanel"
  }
}
