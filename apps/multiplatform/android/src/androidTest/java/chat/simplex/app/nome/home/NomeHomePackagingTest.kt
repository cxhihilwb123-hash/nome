package chat.simplex.app.nome.home

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeHomePackagingTest {
  @Test
  @Suppress("DEPRECATION")
  fun debugHomeEvidenceActivityIsPresentAndNotExported() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val packageInfo = context.packageManager.getPackageInfo(
      context.packageName,
      PackageManager.GET_ACTIVITIES,
    )
    val activity = packageInfo.activities
      ?.singleOrNull { it.name == EVIDENCE_ACTIVITY }

    assertNotNull("Debug home evidence activity must be present", activity)
    assertFalse("Debug home evidence activity must not be exported", activity!!.exported)
    assertEquals(
      "Evidence harness must be packaged in the target application",
      context.packageName,
      activity.applicationInfo.packageName,
    )
  }

  private companion object {
    const val EVIDENCE_ACTIVITY =
      "chat.simplex.app.nome.home.NomeHomeEvidenceActivity"
  }
}
