package chat.simplex.common.helpers

import android.net.*
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.getSystemService
import chat.simplex.common.activation.runActivationAwareBackgroundCommand
import chat.simplex.common.model.ChatModel.controller
import chat.simplex.common.model.NomeCoreHostRecoveryState
import chat.simplex.common.model.NomeForegroundNetworkRecoveryPolicy
import chat.simplex.common.model.UserNetworkInfo
import chat.simplex.common.model.UserNetworkType
import chat.simplex.common.platform.*
import chat.simplex.common.views.helpers.withBGApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NetworkObserver {
  private var prevInfo: UserNetworkInfo? = null
  private val _platformNetworkInfo = mutableStateOf<UserNetworkInfo?>(null)
  private val coreCommandMutex = Mutex()
  private val coreHostRecoveryState = NomeCoreHostRecoveryState()
  private val coreHostRecoveryLock = Any()
  private var coreHostRecoveryJob: Job? = null
  private var lastForegroundReconstructionAtMillis: Long? = null

  /**
   * The latest fact observed directly from Android connectivity, or null before the first
   * observation. Unlike ChatModel's legacy default, null must not be interpreted as online.
   */
  val platformNetworkInfo: State<UserNetworkInfo?> = _platformNetworkInfo

  /**
   * Prevent connectivity callbacks from entering the frozen native core during its compatibility
   * stop/start window, then replay the newest Android network fact to the final live Agent.
   */
  suspend fun <T> coordinateChatStart(block: suspend () -> T): T =
    coreCommandMutex.withLock {
      val result = block()
      if (controller.hasChatCtrl() && chatModel.chatRunning.value == true) {
        latestNetworkInfo()?.let { applyNetworkInfoLocked(it) }
      }
      result
    }

  /**
   * Replay Android's current validated network whenever the UI returns to the foreground. After a
   * long background gap, reconstruct the logical session because Android or an OEM freezer may
   * have suppressed the native host-disconnect event while the physical network stayed online.
   */
  suspend fun reconcileForegroundNetwork(backgroundDurationMillis: Long?, resumedAtMillis: Long) {
    coreCommandMutex.withLock {
      val current = latestNetworkInfo()
      if (current?.online == true && chatModel.chatRunning.value == true) {
        val callInProgress = chatModel.activeCall.value != null ||
          chatModel.activeCallInvitation.value != null ||
          chatModel.callInvitations.isNotEmpty() ||
          chatModel.switchingCall.value
        if (NomeForegroundNetworkRecoveryPolicy.shouldReconstruct(
            backgroundDurationMillis = backgroundDurationMillis,
            resumedAtMillis = resumedAtMillis,
            lastReconstructionAtMillis = lastForegroundReconstructionAtMillis,
            chatRunning = true,
            online = true,
            callInProgress = callInProgress,
          )) {
          Log.w(TAG, "Reconstructing logical Android network after ${backgroundDurationMillis}ms in background")
          if (reconstructLogicalNetworkLocked()) {
            lastForegroundReconstructionAtMillis = resumedAtMillis
          }
        } else {
          applyNetworkInfoLocked(current)
        }
      }
    }
  }

  /**
   * The v6.5.6 Android core can report a host disconnect without completing the matching
   * reconnect. First replay the validated Android network; if the disconnect remains current,
   * reconstruct the logical network session with a bounded offline/online transition.
   */
  fun coreHostStateChanged(connected: Boolean) {
    synchronized(coreHostRecoveryLock) {
      coreHostRecoveryJob?.cancel()
      coreHostRecoveryJob = null
      val token = coreHostRecoveryState.hostStateChanged(connected) ?: return
      coreHostRecoveryJob = withBGApi { recoverDisconnectedCoreHost(token) }
    }
  }

  // When having both mobile and Wi-Fi networks enabled with Wi-Fi being active, then disabling Wi-Fi, network reports its offline (which is true)
  // but since it will be online after switching to mobile, there is no need to inform backend about such temporary change.
  // But if it will not be online after some seconds, report it and apply required measures
  private var noNetworkJob = Job() as Job
  private val networkCallback = object: ConnectivityManager.NetworkCallback() {
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = networkCapabilitiesChanged(networkCapabilities)
    override fun onLost(network: Network) = networkLost()
  }
  private val connectivityManager: ConnectivityManager? = androidAppContext.getSystemService()

  fun restartNetworkObserver() {
    if (connectivityManager == null) {
      Log.e(TAG, "Connectivity manager is unavailable, network observer is disabled")
      val info = UserNetworkInfo(
        networkType = UserNetworkType.OTHER,
        online = true,
      )
      prevInfo = info
      _platformNetworkInfo.value = null
      setNetworkInfo(info)
      return
    }
    try {
      connectivityManager.unregisterNetworkCallback(networkCallback)
    } catch (e: Exception) {
      // do nothing
    }
    val initialCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (initialCapabilities != null) {
      networkCapabilitiesChanged(initialCapabilities)
    } else {
      networkLost()
    }
    try {
      connectivityManager.registerDefaultNetworkCallback(networkCallback)
    } catch (e: Exception) {
      Log.e(TAG, "Error registering network callback: ${e.stackTraceToString()}")
    }
  }

  private fun networkCapabilitiesChanged(capabilities: NetworkCapabilities) {
    connectivityManager ?: return
    val info = UserNetworkInfo(
      networkType = networkTypeFromCapabilities(capabilities),
      online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
    )
    if (prevInfo != info) {
      prevInfo = info
      _platformNetworkInfo.value = info
      setNetworkInfo(info)
    } else {
      _platformNetworkInfo.value = info
    }
  }

  private fun networkLost() {
    Log.d(TAG, "Network changed: lost")
    val none = UserNetworkInfo(networkType = UserNetworkType.NONE, false)
    prevInfo = none
    _platformNetworkInfo.value = none
    setNetworkInfo(none)
  }

  private fun setNetworkInfo(info: UserNetworkInfo) {
    getWakeLock(timeout = 180000)
    Log.d(TAG, "Network changed: $info")
    noNetworkJob.cancel()
    if (info.online) {
      withBGApi {
        applyLatestNetworkInfo(info)
      }
    } else {
      noNetworkJob = withBGApi {
        delay(3000)
        applyLatestNetworkInfo(info)
      }
    }
  }

  private fun latestNetworkInfo(): UserNetworkInfo? = _platformNetworkInfo.value ?: prevInfo

  private suspend fun applyLatestNetworkInfo(info: UserNetworkInfo) {
    coreCommandMutex.withLock {
      // A delayed offline callback must not overwrite a newer online observation.
      if (latestNetworkInfo() == info) applyNetworkInfoLocked(info)
    }
  }

  private suspend fun applyNetworkInfoLocked(info: UserNetworkInfo): Boolean {
    val applied = controller.hasChatCtrl() &&
      runActivationAwareBackgroundCommand { controller.apiSetNetworkInfo(info) }
    if (applied) {
      chatModel.networkInfo.value = info
    }
    return applied
  }

  /** The caller must hold [coreCommandMutex]. */
  private suspend fun reconstructLogicalNetworkLocked(): Boolean {
    val forcedOffline = applyNetworkInfoLocked(
      UserNetworkInfo(networkType = UserNetworkType.NONE, online = false),
    )
    if (!forcedOffline) return false
    delay(CORE_HOST_LOGICAL_OFFLINE_MILLIS)
    val refreshed = latestNetworkInfo()
    return refreshed?.online == true && applyNetworkInfoLocked(refreshed)
  }

  private suspend fun recoverDisconnectedCoreHost(token: Long) {
    delay(CORE_HOST_REPLAY_DELAY_MILLIS)
    if (!isCurrentCoreHostDisconnect(token)) return

    coreCommandMutex.withLock {
      if (!isCurrentCoreHostDisconnect(token)) return@withLock
      val current = latestNetworkInfo()
      if (current?.online != true || chatModel.chatRunning.value != true) return@withLock
      Log.w(TAG, "Core host remains disconnected; replaying current Android network")
      applyNetworkInfoLocked(current)
    }

    delay(CORE_HOST_ESCALATION_DELAY_MILLIS)
    if (!isCurrentCoreHostDisconnect(token)) return

    var forcedOffline = false
    try {
      coreCommandMutex.withLock {
        if (!isCurrentCoreHostDisconnect(token)) return@withLock
        val current = latestNetworkInfo()
        if (current?.online != true || chatModel.chatRunning.value != true) return@withLock
        Log.w(TAG, "Core host reconnect is stale; reconstructing logical Android network")
        forcedOffline = applyNetworkInfoLocked(
          UserNetworkInfo(networkType = UserNetworkType.NONE, online = false),
        )
      }
      if (forcedOffline) delay(CORE_HOST_LOGICAL_OFFLINE_MILLIS)
    } finally {
      if (forcedOffline) {
        withContext(NonCancellable) {
          coreCommandMutex.withLock {
            latestNetworkInfo()?.let { applyNetworkInfoLocked(it) }
          }
        }
      }
    }
  }

  private fun isCurrentCoreHostDisconnect(token: Long): Boolean =
    synchronized(coreHostRecoveryLock) {
      coreHostRecoveryState.isCurrentDisconnect(token)
    }

  private fun networkTypeFromCapabilities(capabilities: NetworkCapabilities): UserNetworkType = when {
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> UserNetworkType.ETHERNET
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> UserNetworkType.WIFI
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> UserNetworkType.CELLULAR
    else -> UserNetworkType.OTHER
  }

  companion object {
    private const val CORE_HOST_REPLAY_DELAY_MILLIS = 3_000L
    private const val CORE_HOST_ESCALATION_DELAY_MILLIS = 3_000L
    private const val CORE_HOST_LOGICAL_OFFLINE_MILLIS = 3_000L
    val shared = NetworkObserver()
  }
}
