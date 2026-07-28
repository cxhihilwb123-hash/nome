package chat.simplex.common.views.call

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalUriHandler
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.request.Method
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse
import org.nanohttpd.protocols.http.response.Status
import org.nanohttpd.protocols.websockets.*
import java.io.IOException
import java.net.BindException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList

private const val SERVER_HOST = "127.0.0.1"
private const val SERVER_PORT = 50395
private const val CALL_BRIDGE_PAGE_PATH = "/simplex/call/"
private const val CALL_BRIDGE_BOOTSTRAP_PARAM = "bootstrap"
private const val CALL_BRIDGE_WS_PATH_PREFIX = "/simplex/call/ws/"
private const val CALL_BRIDGE_SCRIPT_MARKER = "/*__NOME_CALL_BRIDGE_BOOTSTRAP__*/"
private const val CALL_BRIDGE_TOKEN_BYTES = 32
private val connections = CopyOnWriteArrayList<WebSocket>()
private val callBridgeSecureRandom = SecureRandom()

internal class CallBridgeHandshakeRequest(
  val headers: Map<String, String>,
  val remoteIpAddress: String,
  val path: String,
)

internal enum class CallBridgeHandshakeValidation {
  ACCEPTED,
  INVALID_REMOTE_IP,
  INVALID_ORIGIN,
  INVALID_TOKEN,
}

internal fun validateCallBridgeHandshake(
  request: CallBridgeHandshakeRequest,
  expectedWebSocketPath: String,
  expectedOrigin: String,
): CallBridgeHandshakeValidation {
  if (!isLoopbackAddress(request.remoteIpAddress)) {
    return CallBridgeHandshakeValidation.INVALID_REMOTE_IP
  }
  if (!hasExpectedOrigin(request.headers["origin"], expectedOrigin)) {
    return CallBridgeHandshakeValidation.INVALID_ORIGIN
  }
  if (!constantTimeEquals(request.path, expectedWebSocketPath)) {
    return CallBridgeHandshakeValidation.INVALID_TOKEN
  }
  return CallBridgeHandshakeValidation.ACCEPTED
}

internal fun generateCallBridgeToken(random: SecureRandom = callBridgeSecureRandom): String {
  val bytes = ByteArray(CALL_BRIDGE_TOKEN_BYTES)
  random.nextBytes(bytes)
  return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

internal class CallBridgePageRequest(
  val method: Method,
  val path: String,
  val queryParameters: Map<String, List<String>>,
  val remoteIpAddress: String,
)

internal enum class CallBridgePageAuthorization {
  SERVE_PAGE_WITHOUT_AUTH,
  SERVE_AUTHORIZED_PAGE,
  INVALID_REMOTE_IP,
  INVALID_METHOD,
  INVALID_PATH,
  INVALID_QUERY,
  INVALID_NONCE,
  NONCE_ALREADY_CONSUMED,
}

internal class CallBridgeBootstrapGate(private val expectedNonce: String) {
  private var consumed = false

  @Synchronized
  fun authorize(request: CallBridgePageRequest): CallBridgePageAuthorization {
    if (!isLoopbackAddress(request.remoteIpAddress)) {
      return CallBridgePageAuthorization.INVALID_REMOTE_IP
    }
    if (request.path != CALL_BRIDGE_PAGE_PATH) {
      return CallBridgePageAuthorization.INVALID_PATH
    }
    if (request.method != Method.GET) {
      return CallBridgePageAuthorization.INVALID_METHOD
    }
    if (request.queryParameters.isEmpty()) {
      return CallBridgePageAuthorization.SERVE_PAGE_WITHOUT_AUTH
    }
    if (request.queryParameters.keys != setOf(CALL_BRIDGE_BOOTSTRAP_PARAM)) {
      return CallBridgePageAuthorization.INVALID_QUERY
    }
    val nonceValues = request.queryParameters[CALL_BRIDGE_BOOTSTRAP_PARAM]
    if (nonceValues?.size != 1) {
      return CallBridgePageAuthorization.INVALID_QUERY
    }
    if (!constantTimeEquals(nonceValues.single(), expectedNonce)) {
      return CallBridgePageAuthorization.INVALID_NONCE
    }
    if (consumed) {
      return CallBridgePageAuthorization.NONCE_ALREADY_CONSUMED
    }
    consumed = true
    return CallBridgePageAuthorization.SERVE_AUTHORIZED_PAGE
  }
}

internal fun buildCallBridgeBootstrapUri(port: Int, bootstrapNonce: String): String =
  URI(
    "http",
    null,
    SERVER_HOST,
    port,
    CALL_BRIDGE_PAGE_PATH,
    "$CALL_BRIDGE_BOOTSTRAP_PARAM=$bootstrapNonce",
    null,
  ).toASCIIString()

private fun constantTimeEquals(actual: String, expected: String): Boolean =
  MessageDigest.isEqual(
    actual.toByteArray(StandardCharsets.UTF_8),
    expected.toByteArray(StandardCharsets.UTF_8),
  )

private fun hasExpectedOrigin(originHeader: String?, expectedOrigin: String): Boolean {
  val actualOrigin = originHeader?.let(::uriCreateOrNull) ?: return false
  val trustedOrigin = uriCreateOrNull(expectedOrigin) ?: return false
  return actualOrigin.scheme.equals(trustedOrigin.scheme, ignoreCase = true) &&
    actualOrigin.host.equals(trustedOrigin.host, ignoreCase = true) &&
    normalizedPort(actualOrigin) == normalizedPort(trustedOrigin)
}

private fun normalizedPort(uri: URI): Int =
  if (uri.port != -1) {
    uri.port
  } else {
    when (uri.scheme?.lowercase()) {
      "http" -> 80
      "https" -> 443
      else -> -1
    }
  }

private fun isLoopbackAddress(remoteIpAddress: String): Boolean =
  remoteIpAddress == "127.0.0.1" ||
    remoteIpAddress == "::1" ||
    remoteIpAddress == "0:0:0:0:0:0:0:1"

private fun websocketRejected(status: Status, message: String): Response =
  newFixedLengthResponse(status, "text/plain", message).apply {
    closeConnection(true)
    setKeepAlive(false)
  }

internal fun injectCallBridgeWebSocketPath(page: String, webSocketPath: String): String {
  require(webSocketPath.startsWith(CALL_BRIDGE_WS_PATH_PREFIX))
  require(webSocketPath.removePrefix(CALL_BRIDGE_WS_PATH_PREFIX).matches(Regex("[A-Za-z0-9_-]{43}")))
  check(page.contains(CALL_BRIDGE_SCRIPT_MARKER))
  val bootstrapScript =
    "window.nomeCallBridgePath='$webSocketPath';" +
      "window.history.replaceState(null,'','$CALL_BRIDGE_PAGE_PATH');"
  return page.replace(CALL_BRIDGE_SCRIPT_MARKER, bootstrapScript)
}

private fun Response.addCallBridgeSecurityHeaders(): Response = apply {
  addHeader("Cache-Control", "no-store")
  addHeader("Pragma", "no-cache")
  addHeader("Content-Security-Policy", "frame-ancestors 'none'")
  addHeader("X-Frame-Options", "DENY")
  addHeader("X-Content-Type-Options", "nosniff")
  addHeader("Referrer-Policy", "no-referrer")
}

// Spec: spec/services/calls.md#ActiveCallView
@Composable
actual fun ActiveCallView() {
  val scope = rememberCoroutineScope()
  WebRTCController(chatModel.callCommand) { apiMsg ->
    Log.d(TAG, "received from WebRTCController: ${callApiMessageLogSummary(apiMsg)}")
    val call = chatModel.activeCall.value
    if (call != null) {
      Log.d(TAG, "has active call: ${activeCallLogSummary(call)}")
      val callRh = call.remoteHostId
      when (val r = apiMsg.resp) {
        is WCallResponse.Capabilities -> withBGApi {
          val callType = CallType(call.initialCallType, r.capabilities)
          chatModel.controller.apiSendCallInvitation(callRh, call.contact, callType)
          chatModel.activeCall.value = call.copy(callState = CallState.InvitationSent, localCapabilities = r.capabilities)
          CallSoundsPlayer.startConnectingCallSound(scope)
          activeCallWaitDeliveryReceipt(scope)
        }
        is WCallResponse.Offer -> withBGApi {
          chatModel.controller.apiSendCallOffer(callRh, call.contact, r.offer, r.iceCandidates, call.initialCallType, r.capabilities)
          chatModel.activeCall.value = call.copy(callState = CallState.OfferSent, localCapabilities = r.capabilities)
        }
        is WCallResponse.Answer -> withBGApi {
          chatModel.controller.apiSendCallAnswer(callRh, call.contact, r.answer, r.iceCandidates)
          chatModel.activeCall.value = call.copy(callState = CallState.Negotiated)
          CallSoundsPlayer.stop()
        }
        is WCallResponse.Ice -> withBGApi {
          chatModel.controller.apiSendCallExtraInfo(callRh, call.contact, r.iceCandidates)
        }
        is WCallResponse.Connection ->
          try {
            val callStatus = json.decodeFromString<WebRTCCallStatus>("\"${r.state.connectionState}\"")
            if (callStatus == WebRTCCallStatus.Connected) {
              chatModel.activeCall.value = call.copy(callState = CallState.Connected, connectedAt = Clock.System.now())
            }
            withBGApi { chatModel.controller.apiCallStatus(callRh, call.contact, callStatus) }
          } catch (_: Throwable) {
            Log.d(TAG, "call status not used: ${callConnectionStateLogSummary(r.state)}")
          }
        is WCallResponse.Connected -> {
          chatModel.activeCall.value = call.copy(callState = CallState.Connected, connectionInfo = r.connectionInfo)
        }
        is WCallResponse.PeerMedia -> {
          val sources = call.peerMediaSources
          chatModel.activeCall.value = when (r.source) {
            CallMediaSource.Mic -> call.copy(peerMediaSources = sources.copy(mic = r.enabled))
            CallMediaSource.Camera -> call.copy(peerMediaSources = sources.copy(camera = r.enabled))
            CallMediaSource.ScreenAudio -> call.copy(peerMediaSources = sources.copy(screenAudio = r.enabled))
            CallMediaSource.ScreenVideo -> call.copy(peerMediaSources = sources.copy(screenVideo = r.enabled))
          }
        }
        is WCallResponse.End -> {
          withBGApi { chatModel.callManager.endCall(call) }
        }
        is WCallResponse.Ended -> {
          chatModel.activeCall.value = call.copy(callState = CallState.Ended)
          withBGApi { chatModel.callManager.endCall(call) }
          chatModel.showCallView.value = false
        }
        is WCallResponse.Ok -> when (val cmd = apiMsg.command) {
          is WCallCommand.Answer ->
            chatModel.activeCall.value = call.copy(callState = CallState.Negotiated)
          is WCallCommand.Media -> {
            val sources = call.localMediaSources
            when (cmd.source) {
              CallMediaSource.Mic -> chatModel.activeCall.value = call.copy(localMediaSources = sources.copy(mic = cmd.enable))
              CallMediaSource.Camera -> chatModel.activeCall.value = call.copy(localMediaSources = sources.copy(camera = cmd.enable))
              CallMediaSource.ScreenAudio -> chatModel.activeCall.value = call.copy(localMediaSources = sources.copy(screenAudio = cmd.enable))
              CallMediaSource.ScreenVideo -> chatModel.activeCall.value = call.copy(localMediaSources = sources.copy(screenVideo = cmd.enable))
            }
          }
          is WCallCommand.Camera -> {
            chatModel.activeCall.value = call.copy(localCamera = cmd.camera)
            if (!call.localMediaSources.mic) {
              chatModel.callCommand.add(WCallCommand.Media(CallMediaSource.Mic, enable = false))
            }
          }
          is WCallCommand.End ->
            chatModel.showCallView.value = false
          else -> {}
        }
        is WCallResponse.Error -> {
          when (apiMsg.command) {
            is WCallCommand.Capabilities -> chatModel.callCommand.add(WCallCommand.Permission(
              title = generalGetString(MR.strings.call_desktop_permission_denied_title),
              chrome = generalGetString(MR.strings.call_desktop_permission_denied_chrome),
              safari = generalGetString(MR.strings.call_desktop_permission_denied_safari)
            ))
            else -> {}
          }
          Log.e(TAG, "ActiveCallView: command error ${callApiMessageLogSummary(apiMsg)}")
        }
      }
    }
  }

  SendStateUpdates()
  DisposableEffect(Unit) {
    chatModel.activeCallViewIsVisible.value = true
    // After the first call, End command gets added to the list which prevents making another calls
    chatModel.callCommand.removeAll { it is WCallCommand.End }
    onDispose {
      CallSoundsPlayer.stop()
      chatModel.activeCallViewIsVisible.value = false
      chatModel.callCommand.clear()
    }
  }
}

@Composable
private fun SendStateUpdates() {
  LaunchedEffect(Unit) {
    snapshotFlow { chatModel.activeCall.value }
      .distinctUntilChanged()
      .filterNotNull()
      .collect { call ->
        val state = call.callState.text
        val connInfo = call.connectionInfo
        val connInfoText = if (connInfo == null) ""  else " (${connInfo.text})"
        val description = call.encryptionStatus + connInfoText
        chatModel.callCommand.add(WCallCommand.Description(state, description))
      }
  }
}

@Composable
fun WebRTCController(callCommand: SnapshotStateList<WCallCommand>, onResponse: (WVAPIMessage) -> Unit) {
  val uriHandler = LocalUriHandler.current
  val endCall = {
    val call = chatModel.activeCall.value
    if (call != null) withBGApi { chatModel.callManager.endCall(call) }
  }
  val bootstrapNonce = remember { generateCallBridgeToken() }
  val server = remember {
    startServer(onResponse, bootstrapNonce = bootstrapNonce).apply {
      try {
        uriHandler.openUri(buildCallBridgeBootstrapUri(listeningPort, bootstrapNonce))
      } catch (e: Exception) {
        Log.e(TAG, callBridgeFailureLogSummary(CallBridgeLogEvent.OPEN_BROWSER_FAILED, e))
        AlertManager.shared.showAlertMsg(
          title = generalGetString(MR.strings.unable_to_open_browser_title),
          text = generalGetString(MR.strings.unable_to_open_browser_desc)
        )
        endCall()
      }
    }
  }
  fun processCommand(cmd: WCallCommand) {
    val apiCall = WVAPICall(command = cmd)
    for (connection in connections.toList()) {
      try {
        connection.send(json.encodeToString(apiCall))
        break
      } catch (e: Exception) {
        Log.e(
          TAG,
          callBridgeFailureLogSummary(
            CallBridgeLogEvent.SEND_COMMAND_FAILED,
            e,
            command = cmd,
          ),
        )
      }
    }
  }
  DisposableEffect(Unit) {
    onDispose {
      processCommand(WCallCommand.End)
      server.stop()
      connections.clear()
    }
  }
  LaunchedEffect(Unit) {
    snapshotFlow { callCommand.firstOrNull() }
      .distinctUntilChanged()
      .filterNotNull()
      .collect {
        while (connections.isEmpty()) {
          delay(100)
        }
        while (callCommand.isNotEmpty()) {
          val cmd = callCommand.removeFirstOrNull()
          Log.d(TAG, "WebRTCController LaunchedEffect executing ${cmd?.let(::callCommandLogSummary) ?: "none"}")
          if (cmd != null) {
            processCommand(cmd)
          }
        }
      }
  }
}

fun startServer(
  onResponse: (WVAPIMessage) -> Unit,
  port: Int = SERVER_PORT,
  bootstrapNonce: String = generateCallBridgeToken(),
): NanoWSD {
  val server = object: NanoWSD(SERVER_HOST, port) {
    private val authToken = generateCallBridgeToken()
    private val authorizedWebSocketPath = "$CALL_BRIDGE_WS_PATH_PREFIX$authToken"
    private val bootstrapGate = CallBridgeBootstrapGate(bootstrapNonce)
    private var clientReserved = false

    override fun openWebSocket(session: IHTTPSession): WebSocket =
      MyWebSocket(onResponse, session, ::releaseClient)

    override fun handleWebSocket(session: IHTTPSession): Response {
      val validation = validateCallBridgeHandshake(
        request = CallBridgeHandshakeRequest(session.headers, session.remoteIpAddress, session.uri),
        expectedWebSocketPath = authorizedWebSocketPath,
        expectedOrigin = "http://$SERVER_HOST:$listeningPort"
      )
      if (validation != CallBridgeHandshakeValidation.ACCEPTED) {
        return when (validation) {
          CallBridgeHandshakeValidation.INVALID_REMOTE_IP ->
            websocketRejected(Status.FORBIDDEN, "Loopback client required")
          CallBridgeHandshakeValidation.INVALID_ORIGIN ->
            websocketRejected(Status.FORBIDDEN, "Same-origin websocket required")
          CallBridgeHandshakeValidation.INVALID_TOKEN ->
            websocketRejected(Status.UNAUTHORIZED, "Missing call bridge authorization")
          CallBridgeHandshakeValidation.ACCEPTED ->
            websocketRejected(Status.INTERNAL_ERROR, "Unexpected validation state")
        }
      }
      if (!reserveClient()) {
        return websocketRejected(Status.CONFLICT, "Call bridge already connected")
      }

      return try {
        val response = super.handleWebSocket(session)
        if (response == null || response.status != Status.SWITCH_PROTOCOL) {
          releaseClient()
        }
        response ?: websocketRejected(Status.BAD_REQUEST, "WebSocket upgrade required")
      } catch (e: Throwable) {
        releaseClient()
        throw e
      }
    }

    fun resourceBytes(path: String): ByteArray? {
      val uri = Class.forName("chat.simplex.common.AppKt").getResource("/assets/www$path") ?: return null
      return uri.openStream().use { it.readBytes() }
    }

    fun resourcesToResponse(path: String): Response {
      val bytes = resourceBytes(path) ?: return resourceNotFound
      val response = newFixedLengthResponse(
        Status.OK, getMimeTypeForFile(path), bytes
      )
      response.setKeepAlive(true)
      response.setUseGzip(true)
      return response.addCallBridgeSecurityHeaders()
    }

    val resourceNotFound = newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "This page couldn't be found")

    @Synchronized
    private fun reserveClient(): Boolean =
      if (clientReserved) {
        false
      } else {
        clientReserved = true
        true
      }

    @Synchronized
    fun releaseClient() {
      clientReserved = false
    }

    override fun handle(session: IHTTPSession): Response {
      return when {
        session.headers["upgrade"]?.equals("websocket", ignoreCase = true) == true -> super.handle(session)
        session.uri == CALL_BRIDGE_PAGE_PATH -> handleCallPage(session)
        else -> resourcesToResponse(uriCreateOrNull(session.uri)?.path ?: return newFixedLengthResponse("Error parsing URL"))
      }
    }

    private fun handleCallPage(session: IHTTPSession): Response {
      val authorization = bootstrapGate.authorize(
        CallBridgePageRequest(
          method = session.method,
          path = session.uri,
          queryParameters = session.parameters,
          remoteIpAddress = session.remoteIpAddress,
        ),
      )
      return when (authorization) {
        CallBridgePageAuthorization.SERVE_PAGE_WITHOUT_AUTH ->
          resourcesToResponse("/desktop/call.html")
        CallBridgePageAuthorization.SERVE_AUTHORIZED_PAGE -> {
          val page = resourceBytes("/desktop/call.html")
            ?.toString(StandardCharsets.UTF_8)
            ?: return resourceNotFound.addCallBridgeSecurityHeaders()
          newFixedLengthResponse(
            Status.OK,
            "text/html",
            injectCallBridgeWebSocketPath(page, authorizedWebSocketPath),
          ).addCallBridgeSecurityHeaders()
        }
        CallBridgePageAuthorization.INVALID_METHOD ->
          websocketRejected(Status.METHOD_NOT_ALLOWED, "GET required").apply {
            addHeader("Allow", "GET")
          }.addCallBridgeSecurityHeaders()
        CallBridgePageAuthorization.NONCE_ALREADY_CONSUMED ->
          websocketRejected(Status.GONE, "Call bridge bootstrap expired").addCallBridgeSecurityHeaders()
        CallBridgePageAuthorization.INVALID_REMOTE_IP,
        CallBridgePageAuthorization.INVALID_PATH,
        CallBridgePageAuthorization.INVALID_QUERY,
        CallBridgePageAuthorization.INVALID_NONCE ->
          websocketRejected(Status.FORBIDDEN, "Invalid call bridge bootstrap").addCallBridgeSecurityHeaders()
      }
    }
  }
  try {
    server.start(60_000_000)
  } catch (e: BindException) {
    if (port == 0) throw e
    Log.w(
      TAG,
      "Call server port $port is busy, using a random port; " +
        callBridgeFailureLogSummary(CallBridgeLogEvent.SERVER_PORT_BUSY, e),
    )
    server.stop()
    return startServer(onResponse, port = 0, bootstrapNonce = bootstrapNonce)
  }
  return server
}

class MyWebSocket(
  val onResponse: (WVAPIMessage) -> Unit,
  handshakeRequest: IHTTPSession,
  private val releaseClient: () -> Unit,
) : WebSocket(handshakeRequest) {
  override fun onOpen() {
    connections.add(this)
  }

  override fun onClose(closeCode: CloseCode?, reason: String?, initiatedByRemote: Boolean) {
    connections.remove(this)
    releaseClient()
    onResponse(WVAPIMessage(null, WCallResponse.End))
  }

  override fun onMessage(message: WebSocketFrame) {
    val payloadLength = message.textPayload.length
    Log.d(TAG, "MyWebSocket.onMessage payloadLength=$payloadLength")
    try {
      onResponse(json.decodeFromString(message.textPayload))
    } catch (e: Exception) {
      Log.e(
        TAG,
        callBridgeFailureLogSummary(
          CallBridgeLogEvent.PARSE_BROWSER_MESSAGE_FAILED,
          e,
          payloadLength = payloadLength,
        ),
      )
    }
  }

  override fun onPong(pong: WebSocketFrame?) = Unit

  override fun onException(exception: IOException) {
    Log.e(TAG, callBridgeFailureLogSummary(CallBridgeLogEvent.WEBSOCKET_EXCEPTION, exception))
  }
}
