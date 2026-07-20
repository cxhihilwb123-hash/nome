package chat.simplex.app.nome

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.nome.harness.NomeFoundationActivity
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeFoundationScreenshotTest {
  @Test
  fun captureRequestedEvidenceSuite() {
    val arguments =
      InstrumentationRegistry.getArguments()
    if (Build.VERSION.SDK_INT != EVIDENCE_API_LEVEL) {
      assertEquals(
        "API 35 screenshot evidence may be skipped only by an explicit cross-API matrix",
        "true",
        arguments.getString(
          CROSS_API_SCREENSHOT_SKIP_ARGUMENT,
        ),
      )
      return
    }
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val targetContext = instrumentation.targetContext
    val suite = EvidenceSuite.from(
      arguments.getString(ARGUMENT_SUITE),
    )
    val captureCases = captureCases(suite)
    val evidenceDirectory = requireNotNull(
      targetContext.getExternalFilesDir(EVIDENCE_DIRECTORY),
    ) { "External files directory is unavailable" }

    assertEquals("Unexpected screenshot count for ${suite.argumentValue}", suite.expectedCount, captureCases.size)
    assertEquals(
      "Screenshot filenames must be unique for ${suite.argumentValue}",
      captureCases.size,
      captureCases.map { it.fileName }.toSet().size,
    )
    assertTrue(
      "Unable to create evidence directory: ${evidenceDirectory.absolutePath}",
      evidenceDirectory.isDirectory || evidenceDirectory.mkdirs(),
    )

    for (captureCase in captureCases) {
      val destination = File(evidenceDirectory, captureCase.fileName)
      assertTrue(
        "Unable to remove stale evidence: ${destination.absolutePath}",
        !destination.exists() || destination.delete(),
      )
      val scenario: ActivityScenario<NomeFoundationActivity> = ActivityScenario.launch(
        captureCase.intent(targetContext),
      )
      try {
        instrumentation.waitForIdleSync()
        // ActivityScenario reaches RESUMED before the compositor's enter transition has
        // necessarily finished. Capture only after a stable native frame, otherwise the
        // evidence can contain the transient system dim layer from the previous Activity.
        SystemClock.sleep(STABLE_FRAME_DELAY_MILLIS)
        instrumentation.waitForIdleSync()
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
          "UiAutomation returned no screenshot for ${captureCase.fileName}"
        }
        try {
          FileOutputStream(destination, false).use { output ->
            assertTrue(
              "Unable to encode ${destination.absolutePath}",
              screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, PNG_QUALITY, output),
            )
            output.flush()
          }
          assertTrue(
            "Screenshot was not written: ${destination.absolutePath}",
            destination.isFile && destination.length() > 0L,
          )
        } finally {
          screenshot.recycle()
        }
      } finally {
        scenario.close()
        instrumentation.waitForIdleSync()
      }
    }
  }

  private fun captureCases(suite: EvidenceSuite): List<CaptureCase> = when (suite) {
    EvidenceSuite.REPRESENTATIVES -> matrix(
      pages = REPRESENTATIVE_PAGES,
      states = listOf(EvidenceState.NORMAL),
      fontScale = 1f,
    )
    EvidenceSuite.STATES -> matrix(
      pages = listOf(EvidencePage.P07),
      states = ALL_STATES,
      fontScale = 1f,
      renderStatePanel = true,
    )
    EvidenceSuite.FONT_200 -> matrix(
      pages = REPRESENTATIVE_PAGES,
      states = listOf(EvidenceState.NORMAL),
      fontScale = 2f,
    )
    EvidenceSuite.STATES_200 -> matrix(
      pages = listOf(EvidencePage.P07),
      states = ALL_STATES,
      fontScale = 2f,
      renderStatePanel = true,
    )
  }

  private fun matrix(
    pages: List<EvidencePage>,
    states: List<EvidenceState>,
    fontScale: Float,
    renderStatePanel: Boolean = false,
  ): List<CaptureCase> = buildList {
    for (page in pages) {
      for (state in states) {
        for (locale in EVIDENCE_LOCALES) {
          for (theme in EVIDENCE_THEMES) {
            add(
              CaptureCase(
                page = page,
                state = state,
                locale = locale,
                theme = theme,
                fontScale = fontScale,
                renderStatePanel = renderStatePanel,
              ),
            )
          }
        }
      }
    }
  }

  private data class CaptureCase(
    val page: EvidencePage,
    val state: EvidenceState,
    val locale: EvidenceLocale,
    val theme: EvidenceTheme,
    val fontScale: Float,
    val renderStatePanel: Boolean,
  ) {
    val fileName: String
      get() = buildString {
        append("native-")
        append(page.intentValue)
        append('-')
        append(
          if (state == EvidenceState.NORMAL && renderStatePanel) {
            "normal-panel"
          } else {
            state.intentValue
          },
        )
        append("-api")
        append(EVIDENCE_API_LEVEL)
        append('-')
        append(locale.fileValue)
        append('-')
        append(theme.intentValue)
        append("-font")
        append(if (fontScale == 2f) "200" else "100")
        append(".png")
      }

    fun intent(context: Context): Intent = Intent(context, NomeFoundationActivity::class.java).apply {
      putExtra(EXTRA_PAGE, page.intentValue)
      putExtra(EXTRA_LOCALE, locale.intentValue)
      putExtra(EXTRA_THEME, theme.intentValue)
      putExtra(EXTRA_STATE, state.intentValue)
      putExtra(EXTRA_FONT_SCALE, fontScale.toInt().toString())
      putExtra(EXTRA_RENDER_STATE_PANEL, renderStatePanel)
    }
  }

  private enum class EvidenceSuite(
    val argumentValue: String,
    val expectedCount: Int,
  ) {
    REPRESENTATIVES("representatives", 20),
    STATES("states", 28),
    FONT_200("font200", 20),
    STATES_200("states200", 28);

    companion object {
      fun from(rawValue: String?): EvidenceSuite {
        val normalized = rawValue?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when (normalized.ifEmpty { REPRESENTATIVES.argumentValue }) {
          REPRESENTATIVES.argumentValue -> REPRESENTATIVES
          STATES.argumentValue -> STATES
          FONT_200.argumentValue -> FONT_200
          STATES_200.argumentValue -> STATES_200
          else -> throw IllegalArgumentException(
            "Unsupported screenshot suite '$rawValue'; expected representatives, states, font200, or states200",
          )
        }
      }
    }
  }

  private enum class EvidencePage(val intentValue: String) {
    P02("P02"),
    P07("P07"),
    P17("P17"),
    P21("P21"),
    P23("P23"),
  }

  private enum class EvidenceState(val intentValue: String) {
    NORMAL("normal"),
    LOADING("loading"),
    EMPTY("empty"),
    OFFLINE("offline"),
    ERROR("error"),
    PERMISSION("permission"),
    DANGER("danger"),
  }

  private enum class EvidenceLocale(
    val intentValue: String,
    val fileValue: String,
  ) {
    ZH_CN("zh-CN", "zh-CN"),
    EN("en", "en"),
  }

  private enum class EvidenceTheme(val intentValue: String) {
    LIGHT("light"),
    DARK("dark"),
  }

  private companion object {
    const val ARGUMENT_SUITE = "suite"
    const val CROSS_API_SCREENSHOT_SKIP_ARGUMENT =
      "nomeCrossApiScreenshotSkip"
    const val EVIDENCE_DIRECTORY = "nome-evidence"
    const val EVIDENCE_API_LEVEL = 35
    const val PNG_QUALITY = 100
    const val STABLE_FRAME_DELAY_MILLIS = 750L

    const val EXTRA_PAGE = "page"
    const val EXTRA_LOCALE = "locale"
    const val EXTRA_THEME = "theme"
    const val EXTRA_STATE = "state"
    const val EXTRA_FONT_SCALE = "fontScale"
    const val EXTRA_RENDER_STATE_PANEL = "renderStatePanel"

    val REPRESENTATIVE_PAGES = listOf(
      EvidencePage.P02,
      EvidencePage.P07,
      EvidencePage.P17,
      EvidencePage.P21,
      EvidencePage.P23,
    )
    val ALL_STATES = listOf(
      EvidenceState.NORMAL,
      EvidenceState.LOADING,
      EvidenceState.EMPTY,
      EvidenceState.OFFLINE,
      EvidenceState.ERROR,
      EvidenceState.PERMISSION,
      EvidenceState.DANGER,
    )
    val EVIDENCE_LOCALES = listOf(EvidenceLocale.ZH_CN, EvidenceLocale.EN)
    val EVIDENCE_THEMES = listOf(EvidenceTheme.LIGHT, EvidenceTheme.DARK)
  }
}
