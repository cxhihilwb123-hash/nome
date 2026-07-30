package chat.simplex.app.nome.lifecycle

import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity

internal fun controlledBridgeHost(): String =
  InstrumentationRegistry.getArguments()
    .getString(CONTROLLED_BRIDGE_HOST_ARGUMENT)
    ?.trim()
    ?.takeIf { host ->
      host.isNotEmpty() &&
        host.length <= MAX_CONTROLLED_BRIDGE_HOST_LENGTH &&
        host.none(Char::isWhitespace)
    }
    ?: DEFAULT_CONTROLLED_BRIDGE_HOST

internal fun launchControlledMainActivity(): ActivityScenario<MainActivity>? {
  val instrumentation = InstrumentationRegistry.getInstrumentation()
  if (!InstrumentationRegistry.getArguments().containsKey(CONTROLLED_BRIDGE_HOST_ARGUMENT)) {
    return ActivityScenario.launch<MainActivity>(
      Intent().setClassName(
        instrumentation.targetContext.packageName,
        MainActivity::class.java.name,
      ),
    )
  }

  val component =
    "${instrumentation.targetContext.packageName}/chat.simplex.app.MainActivity_default"
  ParcelFileDescriptor.AutoCloseInputStream(
    instrumentation.uiAutomation.executeShellCommand("am start -W -n $component"),
  ).bufferedReader().use { reader ->
    val output = reader.readText()
    check("Status: ok" in output) {
      "Controlled main activity failed to start: $output"
    }
  }
  return null
}

private const val CONTROLLED_BRIDGE_HOST_ARGUMENT = "nomeProducerHost"
private const val DEFAULT_CONTROLLED_BRIDGE_HOST = "10.0.2.2"
private const val MAX_CONTROLLED_BRIDGE_HOST_LENGTH = 253
