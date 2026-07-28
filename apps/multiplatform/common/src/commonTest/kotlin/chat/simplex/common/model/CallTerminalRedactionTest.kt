package chat.simplex.common.model

import chat.simplex.common.views.call.CallCapabilities
import chat.simplex.common.views.call.CallMediaType
import chat.simplex.common.views.call.CallType
import chat.simplex.common.views.call.RcvCallInvitation
import chat.simplex.common.views.call.WebRTCExtraInfo
import chat.simplex.common.views.call.WebRTCCallOffer
import chat.simplex.common.views.call.WebRTCSession
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class CallTerminalRedactionTest {
  private val userRef = UserRef(userId = 1, localDisplayName = "Test", activeUser = true, showNtfs = true)
  private val callType = CallType(CallMediaType.Video, CallCapabilities(encryption = true))
  private val sharedKey = "shared-key-secret"
  private val session = "v=0 session-description-secret"
  private val candidates = "candidate:ice-secret"

  @Test
  fun callInvitationDetailsAndTerminalDoNotRetainSharedKey() {
    val invitation = RcvCallInvitation(
      remoteHostId = null,
      user = User.sampleData,
      contact = Contact.sampleData,
      callType = callType,
      sharedKey = sharedKey,
      callUUID = "test-call-uuid",
      callTs = Instant.fromEpochMilliseconds(0),
    )

    assertCallSecretsRedacted(CR.CallInvitation(invitation), listOf(sharedKey))
    assertCallSecretsRedacted(CR.CallInvitations(listOf(invitation)), listOf(sharedKey))
  }

  @Test
  fun callOfferDetailsAndTerminalDoNotRetainKeySdpOrIce() {
    val result = CR.CallOffer(
      user = userRef,
      contact = Contact.sampleData,
      callType = callType,
      offer = WebRTCSession(rtcSession = session, rtcIceCandidates = candidates),
      sharedKey = sharedKey,
      askConfirmation = true,
    )

    assertCallSecretsRedacted(result, listOf(sharedKey, session, candidates))
  }

  @Test
  fun callAnswerDetailsAndTerminalDoNotRetainSdpOrIce() {
    val result = CR.CallAnswer(
      user = userRef,
      contact = Contact.sampleData,
      answer = WebRTCSession(rtcSession = session, rtcIceCandidates = candidates),
    )

    assertCallSecretsRedacted(result, listOf(session, candidates))
  }

  @Test
  fun callExtraInfoDetailsAndTerminalDoNotRetainIce() {
    val result = CR.CallExtraInfo(
      user = userRef,
      contact = Contact.sampleData,
      extraInfo = WebRTCExtraInfo(rtcIceCandidates = candidates),
    )

    assertCallSecretsRedacted(result, listOf(candidates))
  }

  @Test
  fun outgoingCallCommandsKeepWirePayloadButTerminalOmitsSdpAndIce() {
    val commands = listOf(
      CC.ApiSendCallOffer(
        Contact.sampleData,
        WebRTCCallOffer(callType, WebRTCSession(session, candidates)),
      ),
      CC.ApiSendCallAnswer(Contact.sampleData, WebRTCSession(session, candidates)),
      CC.ApiSendCallExtraInfo(Contact.sampleData, WebRTCExtraInfo(candidates)),
    )

    for (command in commands) {
      val wireCommand = command.cmdString
      val terminal = TerminalItem.cmd(null, command)
      assertFalse(terminal.details.contains(session), terminal.details)
      assertFalse(terminal.details.contains(candidates), terminal.details)
      assertFalse(terminal.label.contains(session), terminal.label)
      assertFalse(terminal.label.contains(candidates), terminal.label)
      assertContains(terminal.details, "<redacted>")
      if (command !is CC.ApiSendCallExtraInfo) assertContains(wireCommand, session)
      assertContains(wireCommand, candidates)
    }
  }

  @Test
  fun fallbackResponseAndInvalidPayloadsAreRepresentedOnlyByLength() {
    val payload = """{"sharedKey":"$sharedKey","rtcSession":"$session","rtcIceCandidates":"$candidates"}"""
    val fallbacks = listOf<CR>(
      CR.Response(type = "futureCallSchema", json = payload),
      CR.Invalid(str = payload),
    )

    for (fallback in fallbacks) {
      val response = API.Result(remoteHostId = null, res = fallback)
      val terminal = TerminalItem.resp(null, response)
      for (details in listOf(response.details, terminal.details)) {
        assertFalse(details.contains(sharedKey), details)
        assertFalse(details.contains(session), details)
        assertFalse(details.contains(candidates), details)
        assertContains(details, "payloadLength: ${payload.length}")
      }
    }
  }

  private fun assertCallSecretsRedacted(result: CR, secrets: List<String>) {
    val response = API.Result(remoteHostId = null, res = result)
    val terminal = TerminalItem.resp(null, response)

    for (details in listOf(response.details, terminal.details)) {
      for (secret in secrets) assertFalse(details.contains(secret), details)
      assertContains(details, "contact:")
    }
  }
}
