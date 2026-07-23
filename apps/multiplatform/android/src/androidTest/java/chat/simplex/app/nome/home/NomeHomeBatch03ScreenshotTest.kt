package chat.simplex.app.nome.home

import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeHomeBatch03ScreenshotTest {
  @Test
  fun captureRedesignedPrimarySurfaces() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val targetContext = instrumentation.targetContext
    val evidenceDirectory = requireNotNull(
      targetContext.getExternalFilesDir(EVIDENCE_DIRECTORY),
    )
    val cases = captureCases()

    assertTrue(cases.size == EXPECTED_CAPTURE_COUNT)
    assertTrue(cases.size == cases.map { it.fileName }.toSet().size)
    assertTrue(
      evidenceDirectory.isDirectory || evidenceDirectory.mkdirs(),
    )

    for (captureCase in cases) {
      val destination = File(evidenceDirectory, captureCase.fileName)
      assertTrue(!destination.exists() || destination.delete())
      val scenario: ActivityScenario<NomeHomeEvidenceActivity> =
        ActivityScenario.launch(
          NomeHomeEvidenceActivity.intent(
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
    for (screen in NomeHomeEvidenceScreen.entries) {
      for (languageTag in LANGUAGE_TAGS) {
        for (dark in listOf(false, true)) {
          add(
            CaptureCase(
              NomeHomeEvidenceSpec(
                state = NomeHomeEvidenceState.POPULATED,
                languageTag = languageTag,
                dark = dark,
                fontScale = 1f,
                screen = screen,
              ),
            ),
          )
        }
      }
    }
  }

  private data class CaptureCase(val spec: NomeHomeEvidenceSpec) {
    val fileName: String
      get() = buildString {
        append("native-home-")
        append(spec.screen.intentValue)
        append("-api")
        append(Build.VERSION.SDK_INT)
        append('-')
        append(spec.languageTag)
        append(if (spec.dark) "-dark" else "-light")
        append(".png")
      }
  }

  private companion object {
    const val EVIDENCE_DIRECTORY = "nome-batch3-evidence"
    const val EXPECTED_CAPTURE_COUNT = 12
    const val PNG_QUALITY = 100
    const val STABLE_FRAME_DELAY_MILLIS = 750L
    val LANGUAGE_TAGS = listOf("zh-CN", "en")
  }
}
