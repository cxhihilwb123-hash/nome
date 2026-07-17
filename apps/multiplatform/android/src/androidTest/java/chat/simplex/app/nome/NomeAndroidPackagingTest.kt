package chat.simplex.app.nome

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.BuildConfig
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

  private companion object {
    const val FROZEN_MINIMUM_SDK = 28
    const val HARNESS_ACTIVITY_NAME = "chat.simplex.app.nome.harness.NomeFoundationActivity"
  }
}
