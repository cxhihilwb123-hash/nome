package chat.simplex.app.nome.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import chat.simplex.app.nome.fixtures.NomeFixtureLocale
import chat.simplex.app.nome.fixtures.NomeFixturePage
import chat.simplex.app.nome.fixtures.NomeFixtureSpec
import chat.simplex.app.nome.fixtures.NomeFixtureState
import chat.simplex.app.nome.fixtures.NomeFixtureTheme
import chat.simplex.app.nome.fixtures.NomeFoundationFixture

@Preview(
  name = "P02 dark zh-CN",
  locale = "zh-rCN",
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  widthDp = 470,
  heightDp = 936,
)
@Composable
private fun P02DarkChinesePreview() = PreviewFixture(NomeFixturePage.P02, NomeFixtureLocale.ZH_CN)

@Preview(
  name = "P07 dark zh-CN",
  locale = "zh-rCN",
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  widthDp = 470,
  heightDp = 936,
)
@Composable
private fun P07DarkChinesePreview() = PreviewFixture(NomeFixturePage.P07, NomeFixtureLocale.ZH_CN)

@Preview(
  name = "P17 dark en",
  locale = "en",
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  widthDp = 470,
  heightDp = 936,
)
@Composable
private fun P17DarkEnglishPreview() = PreviewFixture(NomeFixturePage.P17, NomeFixtureLocale.EN)

@Preview(
  name = "P21 dark en",
  locale = "en",
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  widthDp = 470,
  heightDp = 936,
)
@Composable
private fun P21DarkEnglishPreview() = PreviewFixture(NomeFixturePage.P21, NomeFixtureLocale.EN)

@Preview(
  name = "P23 dark zh-CN",
  locale = "zh-rCN",
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  widthDp = 470,
  heightDp = 936,
)
@Composable
private fun P23DarkChinesePreview() = PreviewFixture(NomeFixturePage.P23, NomeFixtureLocale.ZH_CN)

@Preview(
  name = "P07 light en",
  locale = "en",
  uiMode = Configuration.UI_MODE_NIGHT_NO,
  widthDp = 470,
  heightDp = 936,
)
@Composable
private fun P07LightEnglishPreview() = NomeFoundationFixture(
  NomeFixtureSpec(
    page = NomeFixturePage.P07,
    locale = NomeFixtureLocale.EN,
    theme = NomeFixtureTheme.LIGHT,
    state = NomeFixtureState.NORMAL,
    fontScale = 1f,
  ),
)

@Preview(
  name = "P02 dark zh-CN font 200%",
  locale = "zh-rCN",
  uiMode = Configuration.UI_MODE_NIGHT_YES,
  fontScale = 2f,
  widthDp = 470,
  heightDp = 936,
)
@Composable
private fun P02DarkChineseFont200Preview() = PreviewFixture(
  page = NomeFixturePage.P02,
  locale = NomeFixtureLocale.ZH_CN,
  fontScale = 2f,
)

@Composable
private fun PreviewFixture(
  page: NomeFixturePage,
  locale: NomeFixtureLocale,
  fontScale: Float = 1f,
) {
  NomeFoundationFixture(
    NomeFixtureSpec(
      page = page,
      locale = locale,
      theme = NomeFixtureTheme.DARK,
      state = NomeFixtureState.NORMAL,
      fontScale = fontScale,
    ),
  )
}
