package chat.simplex.app.nome.connection

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.common.R
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewContent
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewEvent
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewPhase
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewState
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewStateAdapter
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.views.newchat.ConnectionPreviewFailureKind
import chat.simplex.common.views.newchat.ConnectionPreviewIdentity
import chat.simplex.common.views.newchat.ConnectionPreviewKind
import chat.simplex.common.views.newchat.ConnectionPreviewOwnerStatus
import chat.simplex.common.views.newchat.ConnectionPreviewUiModel
import chat.simplex.common.views.newchat.ConnectionPreviewWarning
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeConnectionPreviewComposeTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun identityCardsAreMutuallyExclusiveAndMeetTouchTarget() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val state = mutableStateOf(
      NomeConnectionPreviewStateAdapter.initial(
        ConnectionPreviewIdentity.CurrentProfile,
      ),
    )
    composeRule.setContent {
      NomeAndroidTheme(darkTheme = false) {
        NomeConnectionPreviewContent(
          model = model(),
          state = state.value,
          onIdentitySelected = {
            state.value = NomeConnectionPreviewStateAdapter.reduce(
              state.value,
              NomeConnectionPreviewEvent.SelectIdentity(it),
            )
          },
          onPrimary = {},
          onCancel = {},
        )
      }
    }

    val current = composeRule.onNodeWithText(
      target.getString(R.string.nome_connection_preview_current_title),
    )
    val incognito = composeRule.onNodeWithText(
      target.getString(R.string.nome_connection_preview_incognito_title),
    )
    current
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.Selected,
          true,
        ),
      )
    incognito
      .assertIsDisplayed()
      .assertHasClickAction()
      .assertHeightIsAtLeast(48.dp)
      .performClick()
      .assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.Selected,
          true,
        ),
      )
    current.assert(
      SemanticsMatcher.expectValue(
        SemanticsProperties.Selected,
        false,
      ),
    )
  }

  @Test
  fun failureIsAssertiveAndActionsRemainVisibleAtTwoHundredPercent() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    composeRule.setContent {
      CompositionLocalProvider(
        LocalDensity provides Density(
          density = LocalDensity.current.density,
          fontScale = 2f,
        ),
      ) {
        NomeAndroidTheme(darkTheme = true) {
          NomeConnectionPreviewContent(
            model = model(
              warning = ConnectionPreviewWarning.RepeatRequest,
            ),
            state = NomeConnectionPreviewState(
              identity = ConnectionPreviewIdentity.Incognito,
              phase = NomeConnectionPreviewPhase.Failure,
              failureKind = ConnectionPreviewFailureKind.Network,
            ),
            onIdentitySelected = {},
            onPrimary = {},
            onCancel = {},
          )
        }
      }
    }

    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        target.getString(
          R.string.nome_connection_preview_state_failure,
        ),
      ).and(
        SemanticsMatcher.expectValue(
          SemanticsProperties.LiveRegion,
          LiveRegionMode.Assertive,
        ),
      ),
    )
      .assertIsDisplayed()
    composeRule.onNodeWithText(
      target.getString(R.string.nome_connection_preview_retry),
    )
      .assertIsDisplayed()
      .assertIsEnabled()
      .assertHeightIsAtLeast(48.dp)
    composeRule.onNodeWithText(
      target.getString(R.string.nome_connection_preview_cancel),
    )
      .assertIsDisplayed()
      .assertIsEnabled()
      .assertHeightIsAtLeast(48.dp)
  }

  @Test
  fun connectingIsPoliteAndVisibleAtTwoHundredPercent() {
    val target = InstrumentationRegistry.getInstrumentation().targetContext
    composeRule.setContent {
      CompositionLocalProvider(
        LocalDensity provides Density(
          density = LocalDensity.current.density,
          fontScale = 2f,
        ),
      ) {
        NomeAndroidTheme(darkTheme = false) {
          NomeConnectionPreviewContent(
            model = model(),
            state = NomeConnectionPreviewState(
              identity = ConnectionPreviewIdentity.CurrentProfile,
              phase = NomeConnectionPreviewPhase.Connecting,
            ),
            onIdentitySelected = {},
            onPrimary = {},
            onCancel = {},
          )
        }
      }
    }

    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        target.getString(
          R.string.nome_connection_preview_state_connecting,
        ),
      ).and(
        SemanticsMatcher.expectValue(
          SemanticsProperties.LiveRegion,
          LiveRegionMode.Polite,
        ),
      ),
    ).assertIsDisplayed()
    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.Role,
        Role.Button,
      ).and(
        SemanticsMatcher.expectValue(
          SemanticsProperties.StateDescription,
          target.getString(
            R.string.nome_connection_preview_state_connecting,
          ),
        ),
      ),
    ).assertIsNotEnabled()
    composeRule.onNodeWithText(
      target.getString(R.string.nome_connection_preview_cancel),
    ).assertIsNotEnabled()
  }

  private fun model(
    warning: ConnectionPreviewWarning = ConnectionPreviewWarning.None,
  ): ConnectionPreviewUiModel =
    ConnectionPreviewUiModel(
      kind = ConnectionPreviewKind.ContactAddress,
      warning = warning,
      ownerStatus = ConnectionPreviewOwnerStatus.Absent,
      currentProfileName = "Evidence profile",
      currentProfileImage = null,
      initialIdentity = ConnectionPreviewIdentity.CurrentProfile,
    )
}
