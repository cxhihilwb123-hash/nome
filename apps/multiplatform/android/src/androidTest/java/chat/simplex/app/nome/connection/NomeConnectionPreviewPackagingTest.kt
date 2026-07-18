package chat.simplex.app.nome.connection

import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeConnectionPreviewPackagingTest {
  @Test
  fun evidenceActivityExistsOnlyAsNonExportedDebugSurface() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val info = context.packageManager.getActivityInfo(
      ComponentName(
        context,
        NomeConnectionPreviewEvidenceActivity::class.java,
      ),
      0,
    )

    assertTrue(info.enabled)
    assertFalse(info.exported)
    assertFalse(
      NomeConnectionPreviewEvidenceActivity::class.java.name
        .startsWith("chat.simplex.common"),
    )
  }
}
