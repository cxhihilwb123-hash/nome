package chat.simplex.app.nome.newchat

import android.app.Instrumentation
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.nome.testing.launchNomePhysicalActivityHost
import chat.simplex.common.R
import chat.simplex.common.model.CreatedConnLink
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.newchat.NomeOneTimeInvitationContent
import chat.simplex.common.views.newchat.NomeScanPasteContent
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeNewChatRouteComposeTest {
  @Test
  fun invitationActionsStayLocalAndNeverClaimExpiryOrRegeneration() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val target = instrumentation.targetContext
    val invitation =
      CreatedConnLink(
        connFullLink =
          "https://simplex.chat/contact#/?v=1&smp=fixture",
        connShortLink = "https://smp.nome.im/i#short-fixture",
      )
    var copied = 0
    var shared = 0
    var qrShared = 0
    var copiedLink: String? = null
    var sharedLink: String? = null
    var qrSharedLink: String? = null
    var profileOpened = 0

    withPhysicalComposeContent(instrumentation, {
      NomeAndroidTheme(darkTheme = false) {
        NomeOneTimeInvitationContent(
          invitation = invitation,
          invitationCreating = false,
          currentProfileName = "Lin",
          onOpenProfile = { profileOpened++ },
          onCopyLink = {
            copied++
            copiedLink = it
          },
          onShareLink = {
            shared++
            sharedLink = it
          },
          onShareQr = {
            qrShared++
            qrSharedLink = it
          },
          onQrImageShared = {},
          onRetryInvitation = {},
          onClose = {},
        )
      }
    }) {
      clickNode(
        instrumentation,
        waitForNode(instrumentation) {
          it.contentDescription?.toString() ==
            target.getString(R.string.nome_p11_profile_action, "Lin")
        },
        requireMinimumHeight = true,
      )
      assertEquals(1, profileOpened)

      assertFalse(nodeExists(instrumentation) { it.text?.toString() == invitation.simplexChatUri(short = true) })
      assertTrue(
        nodeExists(instrumentation) {
          it.contentDescription?.toString() == target.getString(R.string.nome_p11_link_label)
        },
      )

      clickText(instrumentation, target.getString(R.string.nome_p11_copy), requireMinimumHeight = true)
      assertEquals(1, copied)
      assertEquals(invitation.simplexChatUri(short = false), copiedLink)

      clickText(instrumentation, target.getString(R.string.nome_p11_share), requireMinimumHeight = true)
      assertEquals(1, shared)
      assertEquals(invitation.simplexChatUri(short = false), sharedLink)

      clickText(
        instrumentation,
        target.getString(R.string.nome_p11_share_invitation),
        requireMinimumHeight = true,
      )
      assertEquals(1, qrShared)
      assertEquals(invitation.simplexChatUri(short = false), qrSharedLink)

      assertFalse(nodeExists(instrumentation) { it.text?.toString()?.contains("24 hours") == true })
      assertFalse(nodeExists(instrumentation) { it.text?.toString()?.contains("Regenerate") == true })
      scrollUntilNode(instrumentation) {
        it.text?.toString() == target.getString(R.string.nome_p11_shared_local)
      }
    }
  }

  @Test
  fun scannerRequiresExplicitActionAndPasteDispatchesOnce() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val target = instrumentation.targetContext
    val pasted = mutableStateOf("")
    var scannerOpened = 0
    var submitted = 0
    var clipboardRead = 0

    withPhysicalComposeContent(instrumentation, {
      NomeAndroidTheme(darkTheme = false) {
        NomeScanPasteContent(
          pastedLink = pasted.value,
          onPastedLinkChange = {
            pasted.value = it
          },
          onSubmitPastedLink = {
            submitted++
          },
          onReadClipboard = {
            clipboardRead++
          },
          scannerContent = {
            Text("scanner fixture")
          },
          onOpenScanner = {
            scannerOpened++
          },
          onClose = {},
        )
      }
    }) {
      assertFalse(nodeExists(instrumentation) { it.text?.toString() == "scanner fixture" })
      clickText(
        instrumentation,
        target.getString(R.string.nome_p12_open_camera),
        requireMinimumHeight = true,
      )
      assertEquals(1, scannerOpened)
      waitForNode(instrumentation) { it.text?.toString() == "scanner fixture" }

      clickText(instrumentation, target.getString(R.string.nome_p12_paste_tab))
      waitForNode(instrumentation) { it.text?.toString() == target.getString(R.string.nome_p12_link_label) }
      waitForNode(instrumentation) { it.text?.toString() == target.getString(R.string.nome_p12_link_placeholder) }
      assertFalse(target.getString(R.string.nome_p12_link_label).contains("SimpleX"))
      assertFalse(target.getString(R.string.nome_p12_link_placeholder).contains("SimpleX"))

      setNodeText(
        instrumentation,
        waitForNode(instrumentation) {
          it.contentDescription?.toString() == target.getString(R.string.nome_p12_link_input)
        },
        "https://simplex.chat/contact#/?v=1",
      )
      clickText(
        instrumentation,
        target.getString(R.string.nome_p12_check),
        requireMinimumHeight = true,
      )
      assertEquals(1, submitted)

      clickText(
        instrumentation,
        target.getString(R.string.nome_p12_clipboard_action),
      )
      assertEquals(1, clipboardRead)
    }
  }
}

private fun withPhysicalComposeContent(
  instrumentation: Instrumentation,
  content: @Composable () -> Unit,
  block: () -> Unit,
) {
  val activity = launchNomePhysicalActivityHost(instrumentation)
  try {
    instrumentation.runOnMainSync {
      activity.setContent(content = content)
    }
    instrumentation.waitForIdleSync()
    SystemClock.sleep(UI_SETTLE_MILLIS)
    instrumentation.waitForIdleSync()
    block()
  } finally {
    instrumentation.runOnMainSync { activity.finishAndRemoveTask() }
    instrumentation.waitForIdleSync()
    SystemClock.sleep(UI_SETTLE_MILLIS)
    instrumentation.waitForIdleSync()
  }
}

private fun clickText(
  instrumentation: Instrumentation,
  text: String,
  requireMinimumHeight: Boolean = false,
) {
  clickNode(
    instrumentation,
    waitForNode(instrumentation) { it.text?.toString() == text },
    requireMinimumHeight,
  )
}

private fun clickNode(
  instrumentation: Instrumentation,
  node: AccessibilityNodeInfo,
  requireMinimumHeight: Boolean,
) {
  var clickable: AccessibilityNodeInfo? = node
  while (clickable != null && !clickable.isClickable) {
    clickable = clickable.parent
  }
  assertTrue(
    "Expected an actionable accessibility node for ${node.text ?: node.contentDescription}",
    clickable != null,
  )
  val actionNode = clickable!!
  if (requireMinimumHeight) {
    val bounds = Rect()
    actionNode.getBoundsInScreen(bounds)
    val minimum =
      (48f * instrumentation.targetContext.resources.displayMetrics.density).roundToInt()
    assertTrue("Touch target must be at least 48dp high", bounds.height() >= minimum)
  }
  assertTrue(
    "Expected an actionable accessibility node for ${node.text ?: node.contentDescription}",
    actionNode.performAction(AccessibilityNodeInfo.ACTION_CLICK),
  )
  instrumentation.waitForIdleSync()
  SystemClock.sleep(UI_ACTION_SETTLE_MILLIS)
  instrumentation.waitForIdleSync()
}

private fun setNodeText(
  instrumentation: Instrumentation,
  node: AccessibilityNodeInfo,
  text: String,
) {
  var editable: AccessibilityNodeInfo? = node
  while (editable != null && !editable.isEditable) {
    editable = editable.parent
  }
  val arguments = Bundle().apply {
    putCharSequence(
      AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
      text,
    )
  }
  assertTrue(
    "Expected an editable accessibility node for ${node.contentDescription}",
    editable?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments) == true,
  )
  instrumentation.waitForIdleSync()
  SystemClock.sleep(UI_ACTION_SETTLE_MILLIS)
  instrumentation.waitForIdleSync()
}

private fun waitForNode(
  instrumentation: Instrumentation,
  matcher: (AccessibilityNodeInfo) -> Boolean,
): AccessibilityNodeInfo {
  val deadline = SystemClock.elapsedRealtime() + UI_TIMEOUT_MILLIS
  do {
    findNode(instrumentation.uiAutomation.rootInActiveWindow, matcher)?.let { return it }
    SystemClock.sleep(UI_POLL_MILLIS)
  } while (SystemClock.elapsedRealtime() < deadline)
  error("Timed out waiting for the expected physical-device accessibility node")
}

private fun nodeExists(
  instrumentation: Instrumentation,
  matcher: (AccessibilityNodeInfo) -> Boolean,
): Boolean = findNode(instrumentation.uiAutomation.rootInActiveWindow, matcher) != null

private fun scrollUntilNode(
  instrumentation: Instrumentation,
  matcher: (AccessibilityNodeInfo) -> Boolean,
): AccessibilityNodeInfo {
  repeat(MAX_SCROLL_ATTEMPTS) {
    val root = instrumentation.uiAutomation.rootInActiveWindow
    findNode(root, matcher)?.let { return it }
    injectSwipeUp(instrumentation, root)
    instrumentation.waitForIdleSync()
    SystemClock.sleep(UI_ACTION_SETTLE_MILLIS)
    instrumentation.waitForIdleSync()
  }
  error("Timed out scrolling to the expected physical-device accessibility node")
}

private fun injectSwipeUp(
  instrumentation: Instrumentation,
  root: AccessibilityNodeInfo?,
) {
  val bounds = Rect()
  root?.getBoundsInScreen(bounds)
  if (bounds.isEmpty) {
    val metrics = instrumentation.targetContext.resources.displayMetrics
    bounds.set(0, 0, metrics.widthPixels, metrics.heightPixels)
  }
  val x = bounds.centerX().toFloat()
  val startY = bounds.top + bounds.height() * 0.82f
  val endY = bounds.top + bounds.height() * 0.35f
  val downTime = SystemClock.uptimeMillis()
  injectMotionEvent(instrumentation, downTime, downTime, MotionEvent.ACTION_DOWN, x, startY)
  repeat(SWIPE_MOVE_STEPS) { index ->
    val fraction = (index + 1f) / SWIPE_MOVE_STEPS
    val eventTime = downTime + (SWIPE_DURATION_MILLIS * fraction).toLong()
    val y = startY + (endY - startY) * fraction
    injectMotionEvent(instrumentation, downTime, eventTime, MotionEvent.ACTION_MOVE, x, y)
  }
  injectMotionEvent(
    instrumentation,
    downTime,
    downTime + SWIPE_DURATION_MILLIS,
    MotionEvent.ACTION_UP,
    x,
    endY,
  )
}

private fun injectMotionEvent(
  instrumentation: Instrumentation,
  downTime: Long,
  eventTime: Long,
  action: Int,
  x: Float,
  y: Float,
) {
  val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
  try {
    assertTrue(
      "Physical-device swipe event must be injected",
      instrumentation.uiAutomation.injectInputEvent(event, true),
    )
  } finally {
    event.recycle()
  }
}

private fun findNode(
  root: AccessibilityNodeInfo?,
  matcher: (AccessibilityNodeInfo) -> Boolean,
): AccessibilityNodeInfo? {
  root ?: return null
  if (matcher(root)) return root
  for (index in 0 until root.childCount) {
    findNode(root.getChild(index), matcher)?.let { return it }
  }
  return null
}

private const val UI_TIMEOUT_MILLIS = 10_000L
private const val UI_POLL_MILLIS = 100L
private const val UI_SETTLE_MILLIS = 500L
private const val UI_ACTION_SETTLE_MILLIS = 150L
private const val MAX_SCROLL_ATTEMPTS = 6
private const val SWIPE_MOVE_STEPS = 12
private const val SWIPE_DURATION_MILLIS = 300L
