package chat.simplex.app.nome.lifecycle

import android.app.Instrumentation
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.model.ChatModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomePhysicalNavigationSmokeTest {
  @Test
  fun installedAppNavigatesAcrossPrimaryDestinationsAndAbout() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val target = instrumentation.targetContext
    val scenario = launchControlledMainActivity()
    try {
      waitUntil("running chat") {
        ChatModel.currentUser.value != null && ChatModel.chatRunning.value == true
      }

      clickText(instrumentation, target.getString(R.string.nome_primary_nav_settings))
      waitForText(instrumentation, target.getString(R.string.nome_p23_backup))
      waitForText(instrumentation, target.getString(R.string.nome_p23_desktop))
      waitForText(instrumentation, target.getString(R.string.nome_p23_notifications))
      clickDescription(instrumentation, target.getString(R.string.nome_p23_search_action))
      setText(instrumentation, target.getString(R.string.nome_p23_about))
      waitForText(instrumentation, target.getString(R.string.nome_p23_about))

      clickText(instrumentation, target.getString(R.string.nome_p23_about_body))
      waitForText(instrumentation, target.getString(R.string.nome_about_product_name))
      waitForText(
        instrumentation,
        target.getString(R.string.nome_about_version, "6.5.6", 373),
      )

      pressBack(instrumentation)
      clickText(instrumentation, target.getString(R.string.nome_primary_nav_contacts))
      waitForText(instrumentation, target.getString(R.string.nome_contacts_title))

      clickText(instrumentation, target.getString(R.string.nome_primary_nav_home))
      waitForText(instrumentation, target.getString(R.string.nome_home_new_connection), contentDescription = true)
    } finally {
      scenario?.close()
    }
  }

  private fun clickText(instrumentation: Instrumentation, text: String) {
    clickMatching(instrumentation, text) { it.text?.toString() == text }
  }

  private fun clickDescription(instrumentation: Instrumentation, description: String) {
    clickMatching(instrumentation, description) {
      it.contentDescription?.toString() == description
    }
  }

  private fun AccessibilityNodeInfo.hasClickableAncestor(): Boolean {
    var candidate: AccessibilityNodeInfo? = this
    while (candidate != null) {
      if (candidate.isClickable) return true
      candidate = candidate.parent
    }
    return false
  }

  private fun clickMatching(
    instrumentation: Instrumentation,
    label: String,
    predicate: (AccessibilityNodeInfo) -> Boolean,
  ) {
    val deadline = SystemClock.elapsedRealtime() + UI_TIMEOUT_MILLIS
    do {
      val node = findNode(instrumentation.uiAutomation.rootInActiveWindow) {
        predicate(it) && it.hasClickableAncestor()
      }
      var clickable: AccessibilityNodeInfo? = node
      while (clickable != null && !clickable.isClickable) {
        clickable = clickable.parent
      }
      if (clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(ACTION_SETTLE_MILLIS)
        return
      }
      SystemClock.sleep(POLL_MILLIS)
    } while (SystemClock.elapsedRealtime() < deadline)
    throw AssertionError("Expected clickable node for $label")
  }

  private fun setText(instrumentation: Instrumentation, text: String) {
    val editable = waitForNode(instrumentation) { it.isEditable }
    val arguments = Bundle().apply {
      putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
    }
    assertTrue(
      "Expected editable settings search field",
      editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments),
    )
    instrumentation.waitForIdleSync()
    SystemClock.sleep(ACTION_SETTLE_MILLIS)
  }

  private fun waitForText(
    instrumentation: Instrumentation,
    text: String,
    contentDescription: Boolean = false,
  ) {
    assertNotNull(
      "Expected visible accessibility text: $text",
      waitForNode(instrumentation) { node ->
        if (contentDescription) {
          node.contentDescription?.toString() == text
        } else {
          node.text?.toString() == text
        }
      },
    )
  }

  private fun waitForNode(
    instrumentation: Instrumentation,
    predicate: (AccessibilityNodeInfo) -> Boolean,
  ): AccessibilityNodeInfo {
    val deadline = SystemClock.elapsedRealtime() + UI_TIMEOUT_MILLIS
    do {
      findNode(instrumentation.uiAutomation.rootInActiveWindow, predicate)?.let { return it }
      SystemClock.sleep(POLL_MILLIS)
    } while (SystemClock.elapsedRealtime() < deadline)
    throw AssertionError("Timed out waiting for accessibility node")
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
      for (index in 0 until node.childCount) {
        node.getChild(index)?.let(queue::addLast)
      }
    }
    return null
  }

  private fun pressBack(instrumentation: Instrumentation) {
    ParcelFileDescriptor.AutoCloseInputStream(
      instrumentation.uiAutomation.executeShellCommand("input keyevent 4"),
    ).use { input ->
      while (input.read() != -1) {
        // Drain the command result before checking the next screen.
      }
    }
    instrumentation.waitForIdleSync()
    SystemClock.sleep(ACTION_SETTLE_MILLIS)
  }

  private fun waitUntil(label: String, predicate: () -> Boolean) {
    val deadline = SystemClock.elapsedRealtime() + UI_TIMEOUT_MILLIS
    while (!predicate() && SystemClock.elapsedRealtime() < deadline) {
      SystemClock.sleep(POLL_MILLIS)
    }
    assertTrue("Timed out waiting for $label", predicate())
  }

  private companion object {
    const val UI_TIMEOUT_MILLIS = 30_000L
    const val POLL_MILLIS = 250L
    const val ACTION_SETTLE_MILLIS = 1_000L
  }
}
