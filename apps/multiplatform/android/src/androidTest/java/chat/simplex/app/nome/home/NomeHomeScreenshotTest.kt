package chat.simplex.app.nome.home

import android.os.Build
import android.os.SystemClock
import android.provider.Settings
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
class NomeHomeScreenshotTest {
  @Test
  fun captureProductionRendererMatrix() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val targetContext = instrumentation.targetContext
    if (
      InstrumentationRegistry.getArguments()
        .getString(TALKBACK_HOLD_ARGUMENT) == "true"
    ) {
      holdPopulatedHomeForTalkBack(instrumentation)
      return
    }
    val evidenceDirectory = requireNotNull(
      targetContext.getExternalFilesDir(EVIDENCE_DIRECTORY),
    )
    val cases = captureCases()

    assertEquals(EVIDENCE_API_LEVEL, Build.VERSION.SDK_INT)
    assertEquals(EVIDENCE_TIME_ZONE, TimeZone.getDefault().id)
    assertTrue(Calendar.getInstance().get(Calendar.YEAR) > EVIDENCE_TIMESTAMP_YEAR)
    assertEquals(EXPECTED_CAPTURE_COUNT, cases.size)
    assertEquals(cases.size, cases.map { it.fileName }.toSet().size)
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

  private fun holdPopulatedHomeForTalkBack(
    instrumentation: android.app.Instrumentation,
  ) {
    val targetContext = instrumentation.targetContext
    val scenario: ActivityScenario<NomeHomeEvidenceActivity> =
      ActivityScenario.launch(
        NomeHomeEvidenceActivity.intent(
          targetContext,
          NomeHomeEvidenceSpec(
            state = NomeHomeEvidenceState.POPULATED,
            languageTag = "zh-CN",
            dark = false,
            fontScale = 1f,
          ),
        ),
      )
    try {
      instrumentation.waitForIdleSync()
      val deadline = SystemClock.elapsedRealtime() + TALKBACK_HOLD_TIMEOUT_MILLIS
      while (
        Settings.Global.getInt(
          targetContext.contentResolver,
          TALKBACK_DONE_SETTING,
          0,
        ) != 1 &&
        SystemClock.elapsedRealtime() < deadline
      ) {
        SystemClock.sleep(TALKBACK_HOLD_POLL_MILLIS)
      }
      assertEquals(
        1,
        Settings.Global.getInt(
          targetContext.contentResolver,
          TALKBACK_DONE_SETTING,
          0,
        ),
      )
    } finally {
      scenario.close()
      instrumentation.waitForIdleSync()
    }
  }

  private fun captureCases(): List<CaptureCase> = buildList {
    for (state in NomeHomeEvidenceState.entries) {
      for (languageTag in LANGUAGE_TAGS) {
        for (dark in listOf(false, true)) {
          for (fontScale in listOf(1f, 2f)) {
            add(
              CaptureCase(
                NomeHomeEvidenceSpec(
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

  private data class CaptureCase(val spec: NomeHomeEvidenceSpec) {
    val fileName: String
      get() = buildString {
        append("native-home-")
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
    const val EVIDENCE_TIMESTAMP_YEAR = 2000
    const val EVIDENCE_TIME_ZONE = "Asia/Shanghai"
    const val EVIDENCE_DIRECTORY = "nome-batch2-evidence"
    const val EXPECTED_CAPTURE_COUNT = 80
    const val PNG_QUALITY = 100
    const val STABLE_FRAME_DELAY_MILLIS = 750L
    const val TALKBACK_HOLD_ARGUMENT = "nomeTalkBackHold"
    const val TALKBACK_DONE_SETTING = "nome_batch2_talkback_done"
    const val TALKBACK_HOLD_TIMEOUT_MILLIS = 600_000L
    const val TALKBACK_HOLD_POLL_MILLIS = 250L
    val LANGUAGE_TAGS = listOf("zh-CN", "en")
  }
}
