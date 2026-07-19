package chat.simplex.app.nome.connection

import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.model.AddressSettings
import chat.simplex.common.model.CreatedConnLink
import chat.simplex.common.model.UserContactLinkRec
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.chatlist.NomeContactRequestContent
import chat.simplex.common.views.chatlist.NomeGroupInvitationContent
import chat.simplex.common.views.usersettings.NomePublicContactMethodContent
import chat.simplex.common.views.usersettings.NomeUserAddressLoadState
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeConnectionManagementScreenshotTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun captureP14P15AndP16RendererFixtures() {
    assertEquals(35, Build.VERSION.SDK_INT)
    val instrumentation =
      InstrumentationRegistry.getInstrumentation()
    val target = instrumentation.targetContext
    val outputDirectory =
      requireNotNull(
        target.getExternalFilesDir(
          "nome-p14-p15-renderer-fixtures",
        ),
      )
    assertTrue(
      outputDirectory.isDirectory ||
        outputDirectory.mkdirs(),
    )
    val page = mutableStateOf(RendererPage.P14)

    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        when (page.value) {
          RendererPage.P14 -> {
            NomeContactRequestContent(
              requestName = "Mira",
              requestFullName = "Mira Chen",
              requestImage = null,
              currentProfileName = "Lin",
              currentProfileImage = null,
              canAcceptIncognito = true,
              onAccept = { false },
              onReject = { false },
              onClose = {},
            )
          }
          RendererPage.P15 -> {
            NomePublicContactMethodContent(
              userAddress = fixtureAddress(),
              loadState =
                NomeUserAddressLoadState.READY,
              onReload = {},
              onCreate = { false },
              onSetRequiresConfirmation = { false },
              onDelete = { false },
              onAddShortLink = {},
              onShareAddress = {},
              onOpenAdvanced = {},
              onClose = {},
            )
          }
          RendererPage.P16 -> {
            NomeGroupInvitationContent(
              groupName = "设计交流",
              groupFullName = "设计交流",
              groupDescription = "分享产品设计方法、案例与工具。",
              groupImage = null,
              currentMembers = 128,
              isPublic = true,
              isChannel = false,
              requiresReview = true,
              joinsIncognito = false,
              inviterName = "周然",
              inviterVerified = true,
              onJoin = { false },
              onDelete = { false },
              onClose = {},
            )
          }
        }
      }
    }

    composeRule.waitForIdle()
    SystemClock.sleep(500L)
    composeRule.waitForIdle()
    capture(
      instrumentation = instrumentation,
      destination =
        File(
          outputDirectory,
          "P14-renderer-fixture-api35-zh-light.png",
        ),
    )
    composeRule.runOnIdle {
      page.value = RendererPage.P15
    }
    composeRule.waitForIdle()
    SystemClock.sleep(500L)
    composeRule.waitForIdle()
    capture(
      instrumentation = instrumentation,
      destination =
        File(
          outputDirectory,
          "P15-renderer-fixture-api35-zh-light.png",
        ),
    )
    composeRule.runOnIdle {
      page.value = RendererPage.P16
    }
    composeRule.waitForIdle()
    SystemClock.sleep(500L)
    composeRule.waitForIdle()
    capture(
      instrumentation = instrumentation,
      destination =
        File(
          outputDirectory,
          "P16-renderer-fixture-api35-zh-light.png",
        ),
    )
  }

  private fun capture(
    instrumentation: android.app.Instrumentation,
    destination: File,
  ) {
    val screenshot =
      requireNotNull(
        instrumentation.uiAutomation.takeScreenshot(),
      )
    try {
      FileOutputStream(destination, false).use {
          output ->
        assertTrue(
          screenshot.compress(
            android.graphics.Bitmap.CompressFormat.PNG,
            100,
            output,
          ),
        )
        output.flush()
      }
      assertTrue(
        destination.isFile &&
          destination.length() > 0L,
      )
    } finally {
      screenshot.recycle()
    }
  }

  private fun fixtureAddress() =
    UserContactLinkRec(
      connLinkContact =
        CreatedConnLink(
          connFullLink =
            "https://simplex.chat/contact#/?v=1&smp=renderer-fixture-full",
          connShortLink =
            "https://simplex.chat/contact#/?v=1&smp=renderer-fixture-short",
        ),
      shortLinkDataSet = true,
      shortLinkLargeDataSet = true,
      addressSettings =
        AddressSettings(
          businessAddress = false,
          autoAccept = null,
          autoReply = null,
        ),
    )

  private enum class RendererPage {
    P14,
    P15,
    P16,
  }
}
