package chat.simplex.app.activation

import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Enters a short-lived QA invitation exactly as supplied by instrumentation.
 *
 * Some OEM input methods rewrite `adb input text`, so the physical-device QA
 * harness uses Accessibility's set-text action. The app still performs the
 * real activation request and persists the encrypted credential.
 */
@RunWith(AndroidJUnit4::class)
class NomeActivationBootstrapInstrumentedTest {
  @Test
  fun entersInviteCodeWithoutOemImeRewriting() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val value = InstrumentationRegistry.getArguments().getString("value")
      ?.takeIf { it.isNotBlank() }
      ?: error("Missing value argument")
    val context = instrumentation.targetContext
    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
      ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
      ?: error("Launch intent unavailable")

    context.startActivity(launchIntent)
    val automation = instrumentation.uiAutomation

    waitForNode(automation = automation, timeoutMs = 10_000) {
      it.text?.toString() == "好的"
    }?.let(::clickNodeOrAncestor)

    val field = waitForNode(automation = automation, timeoutMs = 10_000) {
      it.className?.toString() == "android.widget.EditText"
    } ?: error("Activation input unavailable")
    val arguments = Bundle().apply {
      putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
    }
    assertTrue(
      "Accessibility ACTION_SET_TEXT failed",
      field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments),
    )
    waitForNode(automation = automation, timeoutMs = 5_000) {
      it.className?.toString() == "android.widget.EditText" && it.text?.toString() == value
    } ?: error("Activation input mismatch")

    val activate = waitForNode(automation = automation, timeoutMs = 5_000) {
      it.text?.toString() == "激活"
    } ?: error("Activation button unavailable")
    clickNodeOrAncestor(activate)

    val activated = waitUntil(timeoutMs = 15_000) {
      val prefs = context.getSharedPreferences("nome_activation_v1", android.content.Context.MODE_PRIVATE)
      prefs.contains("token_ciphertext") &&
        prefs.contains("token_iv") &&
        prefs.contains("entitlement")
    }
    assertTrue("Encrypted activation credential was not persisted", activated)
  }

  private fun waitForNode(
    automation: android.app.UiAutomation,
    timeoutMs: Long,
    predicate: (AccessibilityNodeInfo) -> Boolean,
  ): AccessibilityNodeInfo? {
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    do {
      findNode(automation.rootInActiveWindow, predicate)?.let { return it }
      SystemClock.sleep(200)
    } while (SystemClock.uptimeMillis() < deadline)
    return null
  }

  private fun findNode(
    root: AccessibilityNodeInfo?,
    predicate: (AccessibilityNodeInfo) -> Boolean,
  ): AccessibilityNodeInfo? {
    if (root == null) return null
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(root)
    while (queue.isNotEmpty()) {
      val node = queue.removeFirst()
      if (predicate(node)) return node
      repeat(node.childCount) { index -> node.getChild(index)?.let(queue::addLast) }
    }
    return null
  }

  private fun clickNodeOrAncestor(node: AccessibilityNodeInfo) {
    var target: AccessibilityNodeInfo? = node
    while (target != null && !target.isClickable) target = target.parent
    if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) != true) {
      error("Clickable ancestor unavailable")
    }
  }

  private fun waitUntil(timeoutMs: Long, predicate: () -> Boolean): Boolean {
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    do {
      if (predicate()) return true
      SystemClock.sleep(250)
    } while (SystemClock.uptimeMillis() < deadline)
    return false
  }
}
