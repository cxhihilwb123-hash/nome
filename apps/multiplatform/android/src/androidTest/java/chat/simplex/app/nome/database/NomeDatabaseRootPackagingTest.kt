package chat.simplex.app.nome.database

import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeDatabaseRootPackagingTest {
  @Test
  fun evidenceActivityExistsOnlyAsNonExportedDebugSurface() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val info = context.packageManager.getActivityInfo(
      ComponentName(
        context,
        NomeDatabaseRootEvidenceActivity::class.java,
      ),
      0,
    )

    assertTrue(info.enabled)
    assertFalse(info.exported)
    assertFalse(
      NomeDatabaseRootEvidenceActivity::class.java.name
        .startsWith("chat.simplex.common"),
    )
  }
}
