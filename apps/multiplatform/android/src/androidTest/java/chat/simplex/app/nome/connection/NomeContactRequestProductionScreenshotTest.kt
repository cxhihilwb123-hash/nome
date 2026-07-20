package chat.simplex.app.nome.connection

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatModel
import chat.simplex.common.views.chatlist.contactRequestAlertDialog
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeContactRequestProductionScreenshotTest {
  @Test
  fun captureExistingProducerBackedRequestPage() {
    val arguments =
      InstrumentationRegistry.getArguments()
    if (Build.VERSION.SDK_INT != EVIDENCE_API_LEVEL) {
      assertEquals(
        "API 35 screenshot evidence may be skipped only by an explicit cross-API matrix",
        "true",
        arguments
          .getString(
            CROSS_API_SCREENSHOT_SKIP_ARGUMENT,
          ),
      )
      return
    }
    val instrumentation =
      InstrumentationRegistry.getInstrumentation()
    if (
      arguments
        .getString(CONTROLLED_CAPTURE_ARGUMENT) !=
      CONTROLLED_CAPTURE_TOKEN
    ) {
      assertEquals(
        "P14 controlled capture may be skipped only by an explicit general-regression matrix",
        "true",
        arguments.getString(
          CONTROLLED_PRODUCER_SKIP_ARGUMENT,
        ),
      )
      return
    }
    val scenario =
      ActivityScenario.launch<MainActivity>(
        Intent().setClassName(
          instrumentation.targetContext.packageName,
          MainActivity::class.java.name,
        ),
      )
    try {
      waitUntil(READY_TIMEOUT_MILLIS) {
        ChatModel.currentUser.value != null &&
          ChatModel.chatRunning.value == true
      }
      require(
        ChatModel.currentUser.value
          ?.localDisplayName ==
          CONTROLLED_CURRENT_USER,
      ) {
        "P14 capture is restricted to the fixed controlled current-user fixture"
      }
      val request =
        requireNotNull(
          controlledPendingRequest(),
        ) {
          "The fixed controlled pending request is required for the P14 capture"
        }
      scenario.onActivity {
        when (val info = request.chatInfo) {
          is ChatInfo.ContactRequest ->
            contactRequestAlertDialog(
              rhId = request.remoteHostId,
              contactRequest = info,
              chatModel = ChatModel,
            )

          is ChatInfo.Direct ->
            contactRequestAlertDialog(
              rhId = request.remoteHostId,
              contact = info.contact,
              chatModel = ChatModel,
            )

          else ->
            error(
              "The production capture accepts only official pending request shapes",
            )
        }
      }
      instrumentation.waitForIdleSync()
      SystemClock.sleep(STABLE_FRAME_MILLIS)
      scenario.onActivity {
        it.window.clearFlags(
          WindowManager.LayoutParams.FLAG_SECURE,
        )
      }
      try {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(STABLE_FRAME_MILLIS)
        val directory =
          requireNotNull(
            instrumentation.targetContext.getExternalFilesDir(
              EVIDENCE_DIRECTORY,
            ),
          )
        assertTrue(
          directory.isDirectory ||
            directory.mkdirs(),
        )
        val destination =
          File(
            directory,
            CAPTURE_FILE,
          )
        File(
          directory,
          LEGACY_RAW_CAPTURE_FILE,
        ).delete()
        val rawScreenshot =
          requireNotNull(
            instrumentation.uiAutomation.takeScreenshot(),
          )
        val redactedScreenshot =
          try {
            redactIdentityRegions(
              rawScreenshot,
            )
          } finally {
            rawScreenshot.recycle()
          }
        try {
          FileOutputStream(
            destination,
            false,
          ).use { output ->
            assertTrue(
              redactedScreenshot.compress(
                Bitmap.CompressFormat.PNG,
                PNG_QUALITY,
                output,
              ),
            )
          }
          assertTrue(
            destination.isFile &&
              destination.length() > 0L,
          )
        } finally {
          redactedScreenshot.recycle()
        }
      } finally {
        scenario.onActivity {
          it.window.addFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
          )
        }
      }
    } finally {
      scenario.close()
    }
  }

  private fun controlledPendingRequest(): Chat? =
    ChatModel.chats.value
      .filter {
        when (val info = it.chatInfo) {
          is ChatInfo.ContactRequest ->
            info.contactRequest.localDisplayName ==
              CONTROLLED_REQUESTER

          is ChatInfo.Direct ->
            info.contact.nextAcceptContactRequest &&
              info.contact.contactRequestId != null &&
              info.contact.localDisplayName ==
              CONTROLLED_REQUESTER

          else -> false
        }
      }.maxByOrNull {
        it.chatInfo.createdAt
      }

  private fun redactIdentityRegions(
    source: Bitmap,
  ): Bitmap {
    val redacted =
      requireNotNull(
        source.copy(
          Bitmap.Config.ARGB_8888,
          true,
        ),
      )
    val canvas = Canvas(redacted)
    val mask =
      Paint(
        Paint.ANTI_ALIAS_FLAG,
      ).apply {
        color = Color.rgb(245, 247, 250)
        style = Paint.Style.FILL
      }
    val label =
      Paint(
        Paint.ANTI_ALIAS_FLAG,
      ).apply {
        color = Color.rgb(68, 78, 96)
        textAlign = Paint.Align.CENTER
        textSize =
          redacted.width *
            REDACTION_TEXT_SIZE_RATIO
        typeface =
          Typeface.create(
            Typeface.DEFAULT,
            Typeface.BOLD,
          )
      }
    redactRegion(
      canvas = canvas,
      bitmap = redacted,
      bounds = REQUESTER_REDACTION,
      mask = mask,
      label = label,
      text = CONTROLLED_REQUEST_LABEL,
    )
    redactRegion(
      canvas = canvas,
      bitmap = redacted,
      bounds = CURRENT_USER_REDACTION,
      mask = mask,
      label = label,
      text = CONTROLLED_CURRENT_USER_LABEL,
    )
    return redacted
  }

  private fun redactRegion(
    canvas: Canvas,
    bitmap: Bitmap,
    bounds: FloatArray,
    mask: Paint,
    label: Paint,
    text: String,
  ) {
    val rect =
      RectF(
        bitmap.width * bounds[0],
        bitmap.height * bounds[1],
        bitmap.width * bounds[2],
        bitmap.height * bounds[3],
      )
    canvas.drawRoundRect(
      rect,
      bitmap.width * REDACTION_CORNER_RATIO,
      bitmap.width * REDACTION_CORNER_RATIO,
      mask,
    )
    canvas.drawText(
      text,
      rect.centerX(),
      rect.centerY() -
        (
          label.ascent() +
            label.descent()
        ) / 2f,
      label,
    )
  }

  private fun waitUntil(
    timeoutMillis: Long,
    predicate: () -> Boolean,
  ) {
    val deadline =
      SystemClock.elapsedRealtime() +
        timeoutMillis
    while (
      !predicate() &&
      SystemClock.elapsedRealtime() < deadline
    ) {
      SystemClock.sleep(POLL_MILLIS)
    }
    assertTrue(
      "Timed out waiting for the real pending contact request",
      predicate(),
    )
  }

  private companion object {
    const val EVIDENCE_DIRECTORY =
      "nome-p14-production"
    const val CAPTURE_FILE =
      "P14-production-request-api35-zh-light-redacted.png"
    const val LEGACY_RAW_CAPTURE_FILE =
      "P14-production-request-api35-zh-light.png"
    const val CONTROLLED_CAPTURE_ARGUMENT =
      "nomeP14ControlledCapture"
    const val CONTROLLED_CAPTURE_TOKEN =
      "p14-controlled-p21-observer35-to-incog-target35-v1"
    const val CONTROLLED_REQUESTER =
      "P21Observer35"
    const val CONTROLLED_CURRENT_USER =
      "IncogTarget35"
    const val CONTROLLED_REQUEST_LABEL =
      "CONTROLLED REQUEST"
    const val CONTROLLED_CURRENT_USER_LABEL =
      "CONTROLLED LOCAL IDENTITY"
    val REQUESTER_REDACTION =
      floatArrayOf(
        0.06f,
        0.145f,
        0.95f,
        0.245f,
      )
    val CURRENT_USER_REDACTION =
      floatArrayOf(
        0.05f,
        0.405f,
        0.95f,
        0.485f,
      )
    const val READY_TIMEOUT_MILLIS = 120_000L
    const val STABLE_FRAME_MILLIS = 1_000L
    const val POLL_MILLIS = 500L
    const val PNG_QUALITY = 100
    const val REDACTION_TEXT_SIZE_RATIO = 0.035f
    const val REDACTION_CORNER_RATIO = 0.015f
    const val EVIDENCE_API_LEVEL = 35
    const val CROSS_API_SCREENSHOT_SKIP_ARGUMENT =
      "nomeCrossApiScreenshotSkip"
    const val CONTROLLED_PRODUCER_SKIP_ARGUMENT =
      "nomeControlledProducerSkip"
  }
}
