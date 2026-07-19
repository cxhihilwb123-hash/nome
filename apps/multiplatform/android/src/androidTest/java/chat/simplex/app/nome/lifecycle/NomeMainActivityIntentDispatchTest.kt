package chat.simplex.app.nome.lifecycle

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.app.dispatchMainActivityIntent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeMainActivityIntentDispatchTest {
  @Test
  fun oneDispatcherOffersEveryColdOrWarmIntentToOfficialOwnersInOrder() {
    val intent = Intent("chat.simplex.app.TEST")
    val calls = mutableListOf<String>()

    dispatchMainActivityIntent(
      intent = intent,
      notificationHandler = {
        assertEquals(intent, it)
        calls += "notification"
      },
      deepLinkHandler = {
        assertEquals(intent, it)
        calls += "deep-link"
      },
      shareHandler = {
        assertEquals(intent, it)
        calls += "share"
      },
    )

    assertEquals(
      listOf(
        "notification",
        "deep-link",
        "share",
      ),
      calls,
    )
  }
}
