package chat.simplex.common.views.call

/**
 * Safe, deliberately lossy summaries for the desktop browser call bridge.
 *
 * WebRTC commands and responses can contain contact data, SDP, ICE candidates,
 * encryption keys and TURN credentials. Never use their generated toString()
 * values in logs; keep diagnostics to event types, safe enums and lengths.
 */
internal fun callApiMessageLogSummary(message: WVAPIMessage): String =
  "response=${callResponseLogSummary(message.resp)}, " +
    "command=${message.command?.let(::callCommandLogSummary) ?: "none"}, " +
    "correlation=${if (message.corrId == null) "none" else "present"}"

internal fun callCommandLogSummary(command: WCallCommand): String =
  when (command) {
    is WCallCommand.Capabilities ->
      "capabilities(media=${command.media.name})"
    is WCallCommand.Permission ->
      "permission(titleLength=${command.title.length}, chromeLength=${command.chrome.length}, safariLength=${command.safari.length})"
    is WCallCommand.Start ->
      "start(media=${command.media.name}, aesKey=${presence(command.aesKey)}, " +
        "iceServerCount=${command.iceServers?.size ?: 0}, relay=${optionalBoolean(command.relay)})"
    is WCallCommand.Offer ->
      "offer(media=${command.media.name}, offerLength=${command.offer.length}, " +
        "iceCandidatesLength=${command.iceCandidates.length}, aesKey=${presence(command.aesKey)}, " +
        "iceServerCount=${command.iceServers?.size ?: 0}, relay=${optionalBoolean(command.relay)})"
    is WCallCommand.Answer ->
      "answer(answerLength=${command.answer.length}, iceCandidatesLength=${command.iceCandidates.length})"
    is WCallCommand.Ice ->
      "ice(iceCandidatesLength=${command.iceCandidates.length})"
    is WCallCommand.Media ->
      "media(source=${command.source.name}, enabled=${command.enable})"
    is WCallCommand.Camera ->
      "camera(camera=${command.camera.name})"
    is WCallCommand.Description ->
      "description(stateLength=${command.state.length}, descriptionLength=${command.description.length})"
    is WCallCommand.Layout ->
      "layout(layout=${command.layout.name})"
    WCallCommand.End ->
      "end"
  }

internal fun callResponseLogSummary(response: WCallResponse): String =
  when (response) {
    is WCallResponse.Capabilities ->
      "capabilities(encryption=${response.capabilities.encryption})"
    is WCallResponse.Offer ->
      "offer(offerLength=${response.offer.length}, iceCandidatesLength=${response.iceCandidates.length}, " +
        "encryption=${response.capabilities.encryption})"
    is WCallResponse.Answer ->
      "answer(answerLength=${response.answer.length}, iceCandidatesLength=${response.iceCandidates.length})"
    is WCallResponse.Ice ->
      "ice(iceCandidatesLength=${response.iceCandidates.length})"
    is WCallResponse.Connection ->
      "connection(${callConnectionStateLogSummary(response.state)})"
    is WCallResponse.Connected ->
      "connected"
    is WCallResponse.PeerMedia ->
      "peerMedia(source=${response.source.name}, enabled=${response.enabled})"
    WCallResponse.End ->
      "end"
    WCallResponse.Ended ->
      "ended"
    WCallResponse.Ok ->
      "ok"
    is WCallResponse.Error ->
      "error(messageLength=${response.message.length})"
  }

internal fun callConnectionStateLogSummary(state: ConnectionState): String =
  "connectionState=${knownCallStatus(state.connectionState)}, " +
    "iceConnectionStateLength=${state.iceConnectionState.length}, " +
    "iceGatheringStateLength=${state.iceGatheringState.length}, " +
    "signalingStateLength=${state.signalingState.length}"

internal fun activeCallLogSummary(call: Call): String =
  "state=${call.callState.name}, media=${call.initialCallType.name}, " +
    "encrypted=${call.encrypted}, hasVideo=${call.hasVideo}, " +
    "remoteHost=${if (call.remoteHostId == null) "none" else "present"}"

internal enum class CallBridgeLogEvent {
  SERVER_START_FAILED,
  OPEN_BROWSER_FAILED,
  WEBSOCKET_CONNECTION_TIMEOUT,
  SEND_COMMAND_FAILED,
  PARSE_BROWSER_MESSAGE_FAILED,
  WEBSOCKET_EXCEPTION,
  SERVER_PORT_BUSY,
}

internal fun callBridgeFailureLogSummary(
  event: CallBridgeLogEvent,
  error: Throwable,
  payloadLength: Int? = null,
  command: WCallCommand? = null,
): String = buildString {
  append("call bridge event=")
  append(event.name)
  append(" errorType=")
  append(error.javaClass.simpleName.ifEmpty { "Throwable" })
  if (payloadLength != null) {
    append(" payloadLength=")
    append(payloadLength.coerceAtLeast(0))
  }
  if (command != null) {
    append(" command=")
    append(callCommandLogSummary(command))
  }
}

private fun presence(value: String?): String = if (value == null) "none" else "present"

private fun optionalBoolean(value: Boolean?): String = value?.toString() ?: "none"

private fun knownCallStatus(value: String): String =
  WebRTCCallStatus.values().firstOrNull { it.value == value }?.name
    ?: "unknown(length=${value.length})"
