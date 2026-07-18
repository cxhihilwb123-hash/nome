package chat.simplex.common.views.newchat

import kotlin.test.Test
import kotlin.test.assertFalse

class PlatformConnectionPreviewDesktopTest {
  @Test
  fun desktopDeclinesNomeConnectionPreview() {
    val presented = presentPlatformConnectionPreview(
      model = ConnectionPreviewUiModel(
        kind = ConnectionPreviewKind.Invitation,
        warning = ConnectionPreviewWarning.None,
        ownerStatus = ConnectionPreviewOwnerStatus.Absent,
        currentProfileName = "Local profile",
        currentProfileImage = null,
        initialIdentity = ConnectionPreviewIdentity.CurrentProfile,
      ),
      callbacks = ConnectionPreviewCallbacks(
        connect = { ConnectionPreviewAttemptResult.Pending },
        retry = {
          ConnectionPreviewRetryResult.Handoff(
            continueFlow = { _, _ -> },
          )
        },
        cancel = {},
      ),
    )

    assertFalse(presented)
  }
}
