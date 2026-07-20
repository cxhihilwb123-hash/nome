package chat.simplex.app.nome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeStatePanel
import chat.simplex.common.ui.nome.components.NomeStatePanelAction
import chat.simplex.common.ui.nome.components.NomeStatePanelState
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.ui.nome.tokens.NomeColorTokens
import chat.simplex.common.views.onboarding.SimpleXLogo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class NomeFoundationComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun onboardingBrand_usesApprovedCompactImageSlot() {
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        SimpleXLogo()
      }
    }

    composeRule.onNodeWithContentDescription(ONBOARDING_BRAND_LABEL)
      .assertIsDisplayed()
      .assertWidthIsEqualTo(104.dp)
      .assertHeightIsEqualTo(44.dp)
  }

  @Test
  fun nomeButton_meetsTouchTargetAndTalkBackContracts() {
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        Column {
          NomeButton(
            text = "Connect",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            semanticsLabel = ENABLED_BUTTON_LABEL,
            stateDescription = "Ready",
          )
          NomeButton(
            text = "Unavailable",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            semanticsLabel = DISABLED_BUTTON_LABEL,
            stateDescription = "Disabled",
          )
        }
      }
    }

    composeRule.onNodeWithContentDescription(ENABLED_BUTTON_LABEL)
      .assertIsDisplayed()
      .assertIsEnabled()
      .assertHasClickAction()
      .assertHeightIsAtLeast(MINIMUM_TOUCH_TARGET)
      .assertWidthIsAtLeast(MINIMUM_TOUCH_TARGET)
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Ready"))

    composeRule.onNodeWithContentDescription(DISABLED_BUTTON_LABEL)
      .assertIsDisplayed()
      .assertIsNotEnabled()
      .assertHeightIsAtLeast(MINIMUM_TOUCH_TARGET)
      .assertWidthIsAtLeast(MINIMUM_TOUCH_TARGET)
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
      .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Disabled"))
  }

  @Test
  fun statePanel_exposesAllSevenStatesAcrossEnglishChineseLightAndDark() {
    val expectedStates = setOf(
      NomeStatePanelState.NORMAL,
      NomeStatePanelState.LOADING,
      NomeStatePanelState.EMPTY,
      NomeStatePanelState.OFFLINE,
      NomeStatePanelState.ERROR,
      NomeStatePanelState.PERMISSION,
      NomeStatePanelState.DANGER,
    )
    assertEquals(expectedStates, NomeStatePanelState.entries.toSet())

    var activeCase by mutableStateOf(
      PanelCase(NomeStatePanelState.NORMAL, TestLanguage.ENGLISH, dark = false),
    )
    var renderedDark = false

    composeRule.setContent {
      val panelCase = activeCase
      NomeAndroidTheme(darkTheme = panelCase.dark) {
        val isDark = NomeTheme.colors.isDark
        SideEffect { renderedDark = isDark }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        ) {
          NomeStatePanel(
            state = panelCase.state,
            title = panelCase.title,
            description = panelCase.description,
            stateDescription = panelCase.stateDescription,
          )
        }
      }
    }

    for (dark in listOf(false, true)) {
      for (language in TestLanguage.entries) {
        for (state in expectedStates) {
          val panelCase = PanelCase(state, language, dark)
          composeRule.runOnIdle { activeCase = panelCase }
          composeRule.waitForIdle()
          composeRule.runOnIdle { assertEquals(dark, renderedDark) }

          val panel = composeRule.onNode(
            SemanticsMatcher.expectValue(
              SemanticsProperties.StateDescription,
              panelCase.stateDescription,
            ),
          )
          panel
            .assertIsDisplayed()
            .assertTextContains(panelCase.title)
            .assertTextContains(panelCase.description)
            // The state icon is decorative. The spoken contract is the localized
            // title, description, and explicit stateDescription, without a duplicate
            // icon announcement.
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))

          when (state) {
            NomeStatePanelState.ERROR,
            NomeStatePanelState.DANGER -> panel.assert(
              SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Assertive,
              ),
            )
            NomeStatePanelState.LOADING,
            NomeStatePanelState.OFFLINE,
            NomeStatePanelState.PERMISSION -> panel.assert(
              SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
              ),
            )
            NomeStatePanelState.NORMAL,
            NomeStatePanelState.EMPTY -> panel.assert(
              SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion),
            )
          }
        }
      }
    }
  }

  @Test
  fun allSevenStates_atTwoHundredPercentRemainSpokenVisibleAndTouchable() {
    val expectedStates = NomeStatePanelState.entries
    var activeCase by mutableStateOf(
      PanelCase(NomeStatePanelState.NORMAL, TestLanguage.ENGLISH, dark = false),
    )
    var renderedFontScale = 0f

    composeRule.setContent {
      val baseDensity = LocalDensity.current
      val panelCase = activeCase
      CompositionLocalProvider(
        LocalDensity provides Density(baseDensity.density, fontScale = 2f),
      ) {
        NomeAndroidTheme(darkTheme = panelCase.dark) {
          val appliedDensity = LocalDensity.current
          SideEffect { renderedFontScale = appliedDensity.fontScale }
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
          ) {
            NomeStatePanel(
              state = panelCase.state,
              title = panelCase.title,
              description = panelCase.description,
              stateDescription = panelCase.stateDescription,
              primaryAction = NomeStatePanelAction(
                label = panelCase.actionLabel,
                onClick = {},
                semanticsLabel = panelCase.actionSemanticsLabel,
              ),
            )
          }
        }
      }
    }

    for (dark in listOf(false, true)) {
      for (language in TestLanguage.entries) {
        for (state in expectedStates) {
          val panelCase = PanelCase(state, language, dark)
          composeRule.runOnIdle { activeCase = panelCase }
          composeRule.waitForIdle()
          composeRule.runOnIdle { assertEquals(2f, renderedFontScale, 0f) }

          composeRule.onNode(
            SemanticsMatcher.expectValue(
              SemanticsProperties.StateDescription,
              panelCase.stateDescription,
            ),
          )
            .assertIsDisplayed()
            .assertTextContains(panelCase.title)
            .assertTextContains(panelCase.description)

          composeRule.onNodeWithContentDescription(panelCase.actionSemanticsLabel)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .assertHeightIsAtLeast(MINIMUM_TOUCH_TARGET)
            .assertWidthIsAtLeast(MINIMUM_TOUCH_TARGET)
        }
      }
    }
  }

  @Test
  fun primaryAction_atTwoHundredPercentFontScaleRemainsVisibleAndTouchable() {
    var renderedFontScale = 0f

    composeRule.setContent {
      val baseDensity = LocalDensity.current
      NomeAndroidTheme(darkTheme = true) {
        CompositionLocalProvider(
          LocalDensity provides Density(baseDensity.density, fontScale = 2f),
        ) {
          val appliedDensity = LocalDensity.current
          SideEffect { renderedFontScale = appliedDensity.fontScale }
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
          ) {
            NomeStatePanel(
              state = NomeStatePanelState.PERMISSION,
              title = "Permission required",
              description = "Allow access in system settings to continue securely.",
              stateDescription = "Permission required",
              primaryAction = NomeStatePanelAction(
                label = "Open settings",
                onClick = {},
                variant = NomeButtonVariant.PRIMARY,
                semanticsLabel = PRIMARY_ACTION_LABEL,
              ),
            )
          }
        }
      }
    }

    composeRule.runOnIdle { assertEquals(2f, renderedFontScale, 0f) }
    composeRule.onNodeWithContentDescription(PRIMARY_ACTION_LABEL)
      .assertIsDisplayed()
      .assertIsEnabled()
      .assertHasClickAction()
      .assertHeightIsAtLeast(MINIMUM_TOUCH_TARGET)
      .assertWidthIsAtLeast(MINIMUM_TOUCH_TARGET)
  }

  @Test
  fun lightAndDarkCriticalTextAndControlPairsMeetWcagContrast() {
    var dark by mutableStateOf(false)
    lateinit var renderedTheme: ThemeSnapshot

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = dark) {
        val colors = NomeTheme.colors
        val materialColors = MaterialTheme.colors
        SideEffect {
          renderedTheme = ThemeSnapshot(
            nome = colors,
            material = materialColors.snapshot(),
          )
        }
      }
    }

    val lightTheme = composeRule.runOnIdle { renderedTheme }
    composeRule.runOnIdle { dark = true }
    val darkTheme = composeRule.runOnIdle { renderedTheme }

    assertEquals(false, lightTheme.nome.isDark)
    assertEquals(true, darkTheme.nome.isDark)
    assertContrastPairs("light", lightTheme.nome)
    assertContrastPairs("dark", darkTheme.nome)
    assertMaterialContrastPairs("light", lightTheme.material)
    assertMaterialContrastPairs("dark", darkTheme.material)
    assertControlOutlineContrast("light", lightTheme.nome)
    assertControlOutlineContrast("dark", darkTheme.nome)
  }

  private fun assertContrastPairs(themeName: String, colors: NomeColorTokens) {
    val pairs = listOf(
      ContrastPair("primary text/background", colors.textPrimary, colors.background),
      ContrastPair("primary text/surface", colors.textPrimary, colors.surface),
      ContrastPair("secondary text/background", colors.textSecondary, colors.background),
      ContrastPair("tertiary text/background", colors.textTertiary, colors.background),
      ContrastPair("tertiary text/subtle surface", colors.textTertiary, colors.surfaceSubtle),
      ContrastPair("tertiary text/container surface", colors.textTertiary, colors.surfaceContainer),
      ContrastPair("primary action", colors.onAction, colors.action),
      ContrastPair("accent action", colors.onAccent, colors.accent),
      ContrastPair("information state", colors.onInfoContainer, colors.infoContainer),
      ContrastPair("success state", colors.onSuccessContainer, colors.successContainer),
      ContrastPair("warning state", colors.onWarningContainer, colors.warningContainer),
      ContrastPair("danger state", colors.onDangerContainer, colors.dangerContainer),
      ContrastPair("destructive action", colors.onDanger, colors.danger),
    )

    for (pair in pairs) {
      assertContrastPair(themeName, pair, MINIMUM_TEXT_AND_CONTROL_CONTRAST)
    }
  }

  private fun assertMaterialContrastPairs(themeName: String, colors: MaterialRoleColors) {
    val pairs = listOf(
      ContrastPair("Material primary role", colors.onPrimary, colors.primary),
      ContrastPair("Material primary variant role", colors.onPrimary, colors.primaryVariant),
      ContrastPair("Material secondary role", colors.onSecondary, colors.secondary),
      ContrastPair("Material secondary variant role", colors.onSecondary, colors.secondaryVariant),
      ContrastPair("Material background role", colors.onBackground, colors.background),
      ContrastPair("Material surface role", colors.onSurface, colors.surface),
      ContrastPair("Material error role", colors.onError, colors.error),
    )
    for (pair in pairs) {
      assertContrastPair(themeName, pair, MINIMUM_TEXT_AND_CONTROL_CONTRAST)
    }
  }

  private fun assertControlOutlineContrast(themeName: String, colors: NomeColorTokens) {
    assertContrastPair(
      themeName,
      ContrastPair("secondary control outline", colors.textTertiary, colors.surface),
      MINIMUM_NON_TEXT_CONTRAST,
    )
  }

  private fun assertContrastPair(themeName: String, pair: ContrastPair, minimum: Double) {
    assertEquals("$themeName ${pair.name} foreground alpha", 1f, pair.foreground.alpha, 0f)
    assertEquals("$themeName ${pair.name} background alpha", 1f, pair.background.alpha, 0f)
    val ratio = contrastRatio(pair.foreground, pair.background)
    assertTrue(
      "$themeName ${pair.name} contrast was $ratio, expected at least $minimum",
      ratio >= minimum,
    )
  }

  private fun contrastRatio(foreground: Color, background: Color): Double {
    val foregroundLuminance = foreground.luminance().toDouble()
    val backgroundLuminance = background.luminance().toDouble()
    return (max(foregroundLuminance, backgroundLuminance) + 0.05) /
      (min(foregroundLuminance, backgroundLuminance) + 0.05)
  }

  private enum class TestLanguage {
    ENGLISH,
    CHINESE,
  }

  private data class PanelCase(
    val state: NomeStatePanelState,
    val language: TestLanguage,
    val dark: Boolean,
  ) {
    val title: String
      get() = if (language == TestLanguage.CHINESE) {
        when (state) {
          NomeStatePanelState.NORMAL -> "已就绪"
          NomeStatePanelState.LOADING -> "正在加载"
          NomeStatePanelState.EMPTY -> "暂无内容"
          NomeStatePanelState.OFFLINE -> "设备离线"
          NomeStatePanelState.ERROR -> "出现错误"
          NomeStatePanelState.PERMISSION -> "需要权限"
          NomeStatePanelState.DANGER -> "危险操作"
        }
      } else {
        when (state) {
          NomeStatePanelState.NORMAL -> "Ready"
          NomeStatePanelState.LOADING -> "Loading"
          NomeStatePanelState.EMPTY -> "Nothing here"
          NomeStatePanelState.OFFLINE -> "Device offline"
          NomeStatePanelState.ERROR -> "Something went wrong"
          NomeStatePanelState.PERMISSION -> "Permission required"
          NomeStatePanelState.DANGER -> "Dangerous action"
        }
      }

    val description: String
      get() = if (language == TestLanguage.CHINESE) {
        "这是面板的状态说明。"
      } else {
        "This is the panel state description."
      }

    val stateDescription: String
      get() = if (language == TestLanguage.CHINESE) {
        when (state) {
          NomeStatePanelState.NORMAL -> "就绪参考状态"
          NomeStatePanelState.LOADING -> "加载参考状态"
          NomeStatePanelState.EMPTY -> "空态参考状态"
          NomeStatePanelState.OFFLINE -> "设备离线参考状态"
          NomeStatePanelState.ERROR -> "可重试错误参考状态"
          NomeStatePanelState.PERMISSION -> "需要权限参考状态"
          NomeStatePanelState.DANGER -> "破坏性边界参考状态"
        }
      } else {
        when (state) {
          NomeStatePanelState.NORMAL -> "Ready reference state"
          NomeStatePanelState.LOADING -> "Loading reference state"
          NomeStatePanelState.EMPTY -> "Empty reference state"
          NomeStatePanelState.OFFLINE -> "Device-offline reference state"
          NomeStatePanelState.ERROR -> "Retryable error reference state"
          NomeStatePanelState.PERMISSION -> "Permission-required reference state"
          NomeStatePanelState.DANGER -> "Destructive-boundary reference state"
        }
      }

    val actionLabel: String
      get() = if (language == TestLanguage.CHINESE) "继续" else "Continue"

    val actionSemanticsLabel: String
      get() = if (language == TestLanguage.CHINESE) {
        "$stateDescription 操作"
      } else {
        "$stateDescription action"
      }
  }

  private data class ContrastPair(
    val name: String,
    val foreground: Color,
    val background: Color,
  )

  private data class ThemeSnapshot(
    val nome: NomeColorTokens,
    val material: MaterialRoleColors,
  )

  private data class MaterialRoleColors(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val background: Color,
    val surface: Color,
    val error: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onError: Color,
  )

  private fun Colors.snapshot(): MaterialRoleColors = MaterialRoleColors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    error = error,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onError = onError,
  )

  private companion object {
    val MINIMUM_TOUCH_TARGET = 48.dp
    const val MINIMUM_TEXT_AND_CONTROL_CONTRAST = 4.5
    const val MINIMUM_NON_TEXT_CONTRAST = 3.0
    const val ONBOARDING_BRAND_LABEL = "Nome"
    const val ENABLED_BUTTON_LABEL = "Connect securely"
    const val DISABLED_BUTTON_LABEL = "Unavailable action"
    const val PRIMARY_ACTION_LABEL = "Open settings primary action"
  }
}
