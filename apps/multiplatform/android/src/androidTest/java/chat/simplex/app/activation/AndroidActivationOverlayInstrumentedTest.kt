package chat.simplex.app.activation

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.simplex.common.R
import chat.simplex.common.activation.ActivationAccess
import chat.simplex.common.activation.ActivationCapability
import chat.simplex.common.activation.ActivationGate
import chat.simplex.common.activation.ActivationOperationResult
import chat.simplex.common.activation.PendingActivationIntent
import chat.simplex.common.activation.ActivationRuntimeDelegate
import chat.simplex.common.activation.ActivationRuntimeState
import chat.simplex.common.activation.PlatformActivationOverlay
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidActivationOverlayInstrumentedTest {
  @get:Rule
  val composeRule = createComposeRule()

  private val rejected = ActivationOperationResult.Failure(
    code = "test_rejected",
    message = "Activation rejected by test endpoint",
  )

  private var redeemedCode: String? = null
  private var previousDelegate: ActivationRuntimeDelegate? = null
  private lateinit var previousState: ActivationRuntimeState
  private var previousSheetVisible = false
  private var previousPendingIntent: PendingActivationIntent? = null
  private var previousPendingReviewVisible = false

  private val runtime = object: ActivationRuntimeDelegate {
    override suspend fun refreshPolicy(force: Boolean): ActivationOperationResult = rejected

    override suspend fun redeem(inviteCode: String): ActivationOperationResult {
      redeemedCode = inviteCode
      return rejected
    }

    override suspend fun refreshEntitlement(): ActivationOperationResult = rejected

    override suspend fun migrateInstallation(): ActivationOperationResult = rejected

    override suspend fun refreshBeforeProtectedAction() = Unit

    override fun recordWouldBlock(capability: ActivationCapability) = Unit
  }

  @Before
  fun setUp() {
    previousDelegate = activationGateField("delegate").get(null) as ActivationRuntimeDelegate?
    previousState = ActivationGate.state.value
    previousSheetVisible = ActivationGate.sheetVisible.value
    previousPendingIntent = ActivationGate.pendingIntent.value
    previousPendingReviewVisible = ActivationGate.pendingReviewVisible.value
    redeemedCode = null
    ActivationGate.install(
      runtime,
      ActivationRuntimeState(
        policyChecked = true,
        access = ActivationAccess.LOCAL_ONLY,
        reason = "android_ui_test",
      ),
    )
    ActivationGate.showActivation("android_ui_test")
  }

  @After
  fun tearDown() {
    activationGateField("delegate").set(null, previousDelegate)
    restoreActivationFlow("mutableState", previousState)
    restoreActivationFlow("mutableSheetVisible", previousSheetVisible)
    restoreActivationFlow("mutablePendingIntent", previousPendingIntent)
    restoreActivationFlow("mutablePendingReviewVisible", previousPendingReviewVisible)
  }

  @Test
  fun activationDialogRequiresCodeAndFailsClosedWhenRedeemIsRejected() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val title = context.getString(R.string.nome_activation_sheet_title)
    val activate = context.getString(R.string.nome_activation_activate)

    composeRule.setContent {
      MaterialTheme {
        PlatformActivationOverlay()
      }
    }

    composeRule.onNodeWithText(title).assertIsDisplayed()
    composeRule.onNodeWithText(activate).assertIsNotEnabled()

    composeRule.onNode(hasSetTextAction()).performTextInput("INVITE-TEST")
    composeRule.onNodeWithText(activate).assertIsEnabled().performClick()

    composeRule.onNodeWithText(rejected.message).assertIsDisplayed()
    composeRule.onNodeWithText(title).assertIsDisplayed()
    assertEquals("INVITE-TEST", redeemedCode)
    assertEquals(ActivationAccess.LOCAL_ONLY, ActivationGate.state.value.access)
    assertFalse(ActivationGate.state.value.permitsChatNetworking)
  }

  private fun activationGateField(name: String) =
    ActivationGate::class.java.getDeclaredField(name).apply { isAccessible = true }

  @Suppress("UNCHECKED_CAST")
  private fun <T> restoreActivationFlow(name: String, value: T) {
    val flow = activationGateField(name).get(null) as MutableStateFlow<T>
    flow.value = value
  }
}
