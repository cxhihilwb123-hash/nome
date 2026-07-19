package chat.simplex.common.views.chat

import kotlin.test.Test
import kotlin.test.assertEquals

class NomeContactVerificationPolicyTest {
  @Test
  fun formatsAllCodeCharactersWithoutInventingOrDroppingDigits() {
    val code = "7024 9185 3361 5248 6804 1179 2350 4426"

    assertEquals(
      code,
      formatNomeSecurityCode(code),
    )
    assertEquals(
      code.filterNot(Char::isWhitespace),
      formatNomeSecurityCode(code)
        .filterNot(Char::isWhitespace),
    )
  }

  @Test
  fun unavailableVerificationNeverCollapsesIntoMismatch() {
    assertEquals(
      VerifyCodeAttempt.UNAVAILABLE,
      verifyCodeAttempt(null),
    )
    assertEquals(
      VerifyCodeAttempt.MISMATCH,
      verifyCodeAttempt(false to "7024"),
    )
    assertEquals(
      VerifyCodeAttempt.MATCHED,
      verifyCodeAttempt(true to "7024"),
    )
  }

  @Test
  fun matchedScanClosesScannerBeforeVerificationPage() {
    val closedLayers = mutableListOf<String>()

    closeMatchedScanLayers(
      closeScanner = {
        closedLayers += "scanner"
      },
      closeVerification = {
        closedLayers += "verification"
      },
    )

    assertEquals(
      listOf("scanner", "verification"),
      closedLayers,
    )
  }
}
