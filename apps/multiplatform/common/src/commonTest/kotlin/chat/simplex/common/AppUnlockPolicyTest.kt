package chat.simplex.common

import chat.simplex.common.views.helpers.LAResult
import kotlin.test.Test
import kotlin.test.assertEquals

class AppUnlockPolicyTest {
  @Test
  fun onlyVerifiedSuccessAuthorizes() {
    assertEquals(AppUnlockEffect.AUTHORIZE, appUnlockEffect(LAResult.Success))
    assertEquals(
      AppUnlockEffect.KEEP_CURRENT_PROMPT,
      appUnlockEffect(LAResult.Failed()),
    )
    assertEquals(
      AppUnlockEffect.KEEP_LOCKED_ERROR,
      appUnlockEffect(LAResult.Error("cancelled")),
    )
    assertEquals(
      AppUnlockEffect.KEEP_LOCKED_UNAVAILABLE,
      appUnlockEffect(LAResult.Unavailable()),
    )
  }
}
