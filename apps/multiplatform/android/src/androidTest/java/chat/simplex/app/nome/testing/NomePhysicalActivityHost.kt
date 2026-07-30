package chat.simplex.app.nome.testing

import android.app.Instrumentation
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage

/** Launches the Compose host through shell on devices that block Instrumentation activity starts. */
fun launchNomePhysicalActivityHost(
  instrumentation: Instrumentation,
): ComponentActivity {
  val targetPackage = instrumentation.targetContext.packageName
  val component = "$targetPackage/${ComponentActivity::class.java.name}"
  val output =
    ParcelFileDescriptor.AutoCloseInputStream(
      instrumentation.uiAutomation.executeShellCommand("am start -W -n $component"),
    ).bufferedReader().use { it.readText() }

  val deadline = SystemClock.elapsedRealtime() + HOST_ACTIVITY_TIMEOUT_MILLIS
  var resumed: ComponentActivity? = null
  do {
    instrumentation.runOnMainSync {
      resumed =
        ActivityLifecycleMonitorRegistry
          .getInstance()
          .getActivitiesInStage(Stage.RESUMED)
          .filterIsInstance<ComponentActivity>()
          .firstOrNull()
    }
    if (resumed != null) return resumed!!
    SystemClock.sleep(HOST_ACTIVITY_POLL_MILLIS)
  } while (SystemClock.elapsedRealtime() < deadline)

  error("Nome physical Compose host did not reach RESUMED: $output")
}

private const val HOST_ACTIVITY_TIMEOUT_MILLIS = 10_000L
private const val HOST_ACTIVITY_POLL_MILLIS = 100L
