package chat.simplex.app.nome.connection

import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeConnectionPreviewScreenshotTest {
  @Test
  fun captureRendererMatrix() {
    if (Build.VERSION.SDK_INT != EVIDENCE_API_LEVEL) {
      assertEquals(
        "API 35 screenshot evidence may be skipped only by an explicit cross-API matrix",
        "true",
        InstrumentationRegistry.getArguments()
          .getString(
            CROSS_API_SCREENSHOT_SKIP_ARGUMENT,
          ),
      )
      return
    }
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val targetContext = instrumentation.targetContext
    val evidenceDirectory = requireNotNull(
      targetContext.getExternalFilesDir(EVIDENCE_DIRECTORY),
    )
    val cases = captureCases()

    assertEquals(EVIDENCE_TIME_ZONE, TimeZone.getDefault().id)
    assertTrue(Calendar.getInstance().get(Calendar.YEAR) > 2000)
    assertEquals(EXPECTED_CAPTURE_COUNT, cases.size)
    assertEquals(cases.size, cases.map { it.fileName }.toSet().size)
    assertTrue(
      evidenceDirectory.isDirectory || evidenceDirectory.mkdirs(),
    )

    for (captureCase in cases) {
      val destination = File(evidenceDirectory, captureCase.fileName)
      assertTrue(!destination.exists() || destination.delete())
      val scenario: ActivityScenario<NomeConnectionPreviewEvidenceActivity> =
        ActivityScenario.launch(
          NomeConnectionPreviewEvidenceActivity.intent(
            targetContext,
            captureCase.spec,
          ),
        )
      try {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(STABLE_FRAME_DELAY_MILLIS)
        instrumentation.waitForIdleSync()
        val screenshot = requireNotNull(
          instrumentation.uiAutomation.takeScreenshot(),
        )
        try {
          FileOutputStream(destination, false).use { output ->
            assertTrue(
              screenshot.compress(
                android.graphics.Bitmap.CompressFormat.PNG,
                PNG_QUALITY,
                output,
              ),
            )
            output.flush()
          }
          assertTrue(destination.isFile && destination.length() > 0L)
        } finally {
          screenshot.recycle()
        }
      } finally {
        scenario.close()
        instrumentation.waitForIdleSync()
      }
    }
  }

  private fun captureCases(): List<CaptureCase> = buildList {
    for (state in NomeConnectionPreviewEvidenceState.entries) {
      for (languageTag in LANGUAGE_TAGS) {
        for (dark in listOf(false, true)) {
          for (fontScale in listOf(1f, 2f)) {
            add(
              CaptureCase(
                NomeConnectionPreviewEvidenceSpec(
                  state = state,
                  languageTag = languageTag,
                  dark = dark,
                  fontScale = fontScale,
                ),
              ),
            )
          }
        }
      }
    }
  }

  private data class CaptureCase(
    val spec: NomeConnectionPreviewEvidenceSpec,
  ) {
    val fileName: String
      get() = buildString {
        append("native-p13-")
        append(spec.state.intentValue)
        append("-api")
        append(EVIDENCE_API_LEVEL)
        append('-')
        append(spec.languageTag)
        append(if (spec.dark) "-dark" else "-light")
        append(if (spec.fontScale == 2f) "-font200" else "-font100")
        append(".png")
      }
  }

  private companion object {
    const val EVIDENCE_API_LEVEL = 35
    const val CROSS_API_SCREENSHOT_SKIP_ARGUMENT =
      "nomeCrossApiScreenshotSkip"
    const val EVIDENCE_TIME_ZONE = "Asia/Shanghai"
    const val EVIDENCE_DIRECTORY = "nome-batch3-p13-evidence"
    const val EXPECTED_CAPTURE_COUNT = 72
    const val PNG_QUALITY = 100
    const val STABLE_FRAME_DELAY_MILLIS = 500L
    val LANGUAGE_TAGS = listOf("zh-CN", "en")
  }
}
