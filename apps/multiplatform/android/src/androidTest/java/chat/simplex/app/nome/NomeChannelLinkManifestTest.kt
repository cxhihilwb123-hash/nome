package chat.simplex.app.nome

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeChannelLinkManifestTest {
  @Test
  fun nomeChannelLinkOpensMainActivity() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://smp.nome.im/c#opaque"))
      .setPackage(context.packageName)
    val resolved = context.packageManager.resolveActivity(intent, 0)

    assertNotNull("Nome must accept its own public channel links", resolved)
    assertEquals(MainActivity::class.java.name, resolved!!.activityInfo.name)
  }

  @Test
  fun nomeFullConnectionLinkOpensMainActivity() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://nome.im/contact#opaque"))
      .setPackage(context.packageName)
    val resolved = context.packageManager.resolveActivity(intent, 0)

    assertNotNull("Nome must accept full Nome connection links", resolved)
    assertEquals(MainActivity::class.java.name, resolved!!.activityInfo.name)
  }
}
