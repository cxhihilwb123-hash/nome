package chat.simplex.app.nome

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.BuildConfig
import chat.simplex.app.SimplexService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeAndroidPackagingTest {
  @Test
  fun targetPackageAndMinimumSdkMatchFrozenAndroidContract() {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    assertEquals(BuildConfig.APPLICATION_ID, targetContext.packageName)
    assertEquals(FROZEN_MINIMUM_SDK, targetContext.applicationInfo.minSdkVersion)
  }

  @Test
  @Suppress("DEPRECATION")
  fun debugFoundationHarnessIsPresentAndNotExported() {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val packageInfo = targetContext.packageManager.getPackageInfo(
      targetContext.packageName,
      PackageManager.GET_ACTIVITIES,
    )
    val harnessActivity = packageInfo.activities
      ?.singleOrNull { it.name.endsWith(".NomeFoundationActivity") }

    assertNotNull("Debug NomeFoundationActivity must be merged into the target APK", harnessActivity)
    assertFalse("Debug foundation harness must never be exported", harnessActivity!!.exported)
    assertTrue(
      "Unexpected foundation harness placement: ${harnessActivity.name}",
      harnessActivity.name == HARNESS_ACTIVITY_NAME,
    )
  }

  @Test
  fun serviceReceiversUseTheRuntimeApplicationId() {
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    val packageManager = targetContext.packageManager
    val receivers = listOf(
      ComponentName(targetContext, SimplexService.StartReceiver::class.java),
      ComponentName(targetContext, SimplexService.AppUpdateReceiver::class.java),
    )
    val previousStates = receivers.associateWith(packageManager::getComponentEnabledSetting)

    try {
      SimplexService.StartReceiver.toggleReceiver(true)
      SimplexService.AppUpdateReceiver.toggleReceiver(true)
      receivers.forEach { component ->
        assertEquals(targetContext.packageName, component.packageName)
        assertEquals(
          PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
          packageManager.getComponentEnabledSetting(component),
        )
      }
    } finally {
      previousStates.forEach { (component, state) ->
        packageManager.setComponentEnabledSetting(
          component,
          state,
          PackageManager.DONT_KILL_APP,
        )
      }
    }
  }

  private companion object {
    const val FROZEN_MINIMUM_SDK = 28
    const val HARNESS_ACTIVITY_NAME = "chat.simplex.app.nome.harness.NomeFoundationActivity"
  }
}
