package chat.simplex.common.views.call

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CallLogRedactionTest {
  private val secrets = listOf(
    "contact-secret",
    "sdp-secret",
    "candidate-secret",
    "aes-secret",
    "turn-user-secret",
    "turn-password-secret",
    "browser-error-secret",
    "state-secret",
    "exception-message-secret",
    "987654321",
  )

  @Test
  fun apiMessageSummaryNeverIncludesCommandOrErrorPayloads() {
    val command = WCallCommand.Offer(
      offer = "sdp-secret",
      iceCandidates = "candidate-secret",
      media = CallMediaType.Video,
      aesKey = "aes-secret",
      iceServers = listOf(
        RTCIceServer(
          urls = listOf("turn:turn-user-secret:turn-password-secret@turn.example.test:443"),
          username = "turn-user-secret",
          credential = "turn-password-secret",
        ),
      ),
      relay = true,
    )
    val summary = callApiMessageLogSummary(
      WVAPIMessage(
        corrId = 987654321,
        resp = WCallResponse.Error("browser-error-secret"),
        command = command,
      ),
    )

    assertContains(summary, "response=error(messageLength=")
    assertContains(summary, "command=offer(media=Video")
    assertContains(summary, "aesKey=present")
    assertContains(summary, "iceServerCount=1")
    assertContains(summary, "correlation=present")
    assertNoSecrets(summary)
  }

  @Test
  fun everyPayloadBearingCommandHasOnlyTypeEnumsAndLengths() {
    val summaries = listOf(
      callCommandLogSummary(
        WCallCommand.Permission(
          title = "contact-secret",
          chrome = "browser-error-secret",
          safari = "state-secret",
        ),
      ),
      callCommandLogSummary(
        WCallCommand.Start(
          media = CallMediaType.Audio,
          aesKey = "aes-secret",
          iceServers = listOf(
            RTCIceServer(
              urls = listOf("turn:turn-user-secret:turn-password-secret@turn.example.test:443"),
              username = "turn-user-secret",
              credential = "turn-password-secret",
            ),
          ),
          relay = false,
        ),
      ),
      callCommandLogSummary(
        WCallCommand.Answer(
          answer = "sdp-secret",
          iceCandidates = "candidate-secret",
        ),
      ),
      callCommandLogSummary(WCallCommand.Ice("candidate-secret")),
      callCommandLogSummary(
        WCallCommand.Description(
          state = "state-secret",
          description = "contact-secret",
        ),
      ),
    ).joinToString("\n")

    assertContains(summaries, "permission(titleLength=")
    assertContains(summaries, "start(media=Audio, aesKey=present, iceServerCount=1, relay=false)")
    assertContains(summaries, "answer(answerLength=")
    assertContains(summaries, "ice(iceCandidatesLength=")
    assertContains(summaries, "description(stateLength=")
    assertNoSecrets(summaries)
  }

  @Test
  fun everyPayloadBearingResponseHasOnlyTypeEnumsAndLengths() {
    val summaries = listOf(
      callResponseLogSummary(
        WCallResponse.Offer(
          offer = "sdp-secret",
          iceCandidates = "candidate-secret",
          capabilities = CallCapabilities(encryption = true),
        ),
      ),
      callResponseLogSummary(
        WCallResponse.Answer(
          answer = "sdp-secret",
          iceCandidates = "candidate-secret",
        ),
      ),
      callResponseLogSummary(WCallResponse.Ice("candidate-secret")),
      callResponseLogSummary(
        WCallResponse.Connection(
          ConnectionState(
            connectionState = "state-secret",
            iceConnectionState = "candidate-secret",
            iceGatheringState = "contact-secret",
            signalingState = "browser-error-secret",
          ),
        ),
      ),
      callResponseLogSummary(WCallResponse.Error("browser-error-secret")),
    ).joinToString("\n")

    assertContains(summaries, "offer(offerLength=")
    assertContains(summaries, "answer(answerLength=")
    assertContains(summaries, "ice(iceCandidatesLength=")
    assertContains(summaries, "connection(connectionState=unknown(length=")
    assertContains(summaries, "error(messageLength=")
    assertNoSecrets(summaries)
  }

  @Test
  fun errorSummaryDoesNotIncludeExceptionMessageOrCommandPayload() {
    val summary = callBridgeFailureLogSummary(
      event = CallBridgeLogEvent.SEND_COMMAND_FAILED,
      error = IllegalStateException("exception-message-secret"),
      payloadLength = 123,
      command = WCallCommand.Ice("candidate-secret"),
    )

    assertContains(summary, "event=SEND_COMMAND_FAILED")
    assertContains(summary, "errorType=IllegalStateException")
    assertContains(summary, "payloadLength=123")
    assertContains(summary, "command=ice(iceCandidatesLength=")
    assertNoSecrets(summary)
  }

  private fun assertNoSecrets(output: String) {
    secrets.forEach { secret ->
      assertFalse(output.contains(secret), "summary must not contain secret '$secret': $output")
    }
  }
}
