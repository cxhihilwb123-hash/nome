package chat.simplex.common.model

import chat.simplex.common.platform.Log
import chat.simplex.common.platform.appPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Nome-managed messaging and file server policy.
 *
 * The full addresses are required by the chat core, but callers and read-only UI must only expose
 * [smpHostname], [xftpHostname] and [chatRelayHostname]. User-created server entries are never
 * removed or rewritten.
 */
internal object NomeServerConfiguration {
  internal const val smpHostname = "smp.nome.im"
  internal const val xftpHostname = "xftp.nome.im"
  internal const val chatRelayHostname = "relay.nome.im"
  internal const val chatRelayName = "Nome Relay"
  internal const val chatRelayDomain = "nome.im"

  private const val smpServerHostOnly =
    "smp://RVzf_goDl1uPbeXQu7Mpi-gck_By0QhEGobrwPwULY8=:e0eabd5e5b046bd7e7082ad9d68e133c213281e9a9109c84@smp.nome.im"
  internal const val smpServer = smpServerHostOnly + ":5223"
  internal const val xftpServer =
    "xftp://y00AWTizJH88sHCMioQ1m-d_xXWlwolHAek_Mc4MhYM=:accbd90c5c813d90facea657d3da87e022eae9ce07005b53@xftp.nome.im"
  internal const val chatRelayAddress =
    "https://relay.nome.im/r#N_ynoTxBs6bAWXuroZ-l5_r4lbibC3mm0MRunZqukOA?p=5223&c=RVzf_goDl1uPbeXQu7Mpi-gck_By0QhEGobrwPwULY8"

  internal const val legacySmpServer =
    "smp://30U1zE40SnyLHHyelf2vJxB_kva4eFXWO-2fTHCDcg8=@124.223.71.168:5223"
  internal const val legacyXftpServer =
    "xftp://5Ev5I4jUSbdQkwUm4ct2KGuzpiCaQSV6o6CbrQzO1lY=@124.223.71.168:5224"

  private const val legacyHostname = "124.223.71.168"
  private val applyMutex = Mutex()

  internal data class Migration(
    val userServers: List<UserOperatorServers>,
    val changed: Boolean,
  )

  internal data class ApplyResult(
    val success: Boolean,
    val changed: Boolean,
  )

  internal fun migrate(
    userServers: List<UserOperatorServers>,
    seedDefaultChatRelay: Boolean = false,
  ): Migration {
    var changed = false
    val currentSmpServer = if (appPlatform.isAndroid) smpServerHostOnly else smpServer
    val managedServerPreset = !appPlatform.isAndroid
    val hasNomeOperator = userServers.any { it.operator?.operatorTag == OperatorTag.Nome }
    val migrated = userServers.map { group ->
      val operator = group.operator
      when (operator?.operatorTag) {
        OperatorTag.Nome -> {
          val enabledOperator = operator.copy(
            enabled = true,
            smpRoles = ServerRoles(storage = true, proxy = true),
            xftpRoles = ServerRoles(storage = true, proxy = true),
          )
          val migratedSmp = enableManagedServer(group.smpServers, ServerProtocol.SMP, currentSmpServer, managedServerPreset)
          val migratedXftp = enableManagedServer(group.xftpServers, ServerProtocol.XFTP, xftpServer, managedServerPreset)
          val updated = group.copy(
            operator = enabledOperator,
            smpServers = migratedSmp.first,
            xftpServers = migratedXftp.first,
          )
          if (updated != group) changed = true
          updated
        }
        null -> {
          if (!hasNomeOperator) {
            group
          } else {
            // Older Android builds stored Nome-managed routes in the custom bucket. Once the
            // rebuilt core exposes OTNome, retire only those exact managed rows so the enabled
            // Nome operator remains the single routing owner. User-created entries stay intact.
            val updated = group.copy(
              smpServers = retireManagedServers(group.smpServers, ServerProtocol.SMP, currentSmpServer),
              xftpServers = retireManagedServers(group.xftpServers, ServerProtocol.XFTP, xftpServer),
            )
            if (updated != group) changed = true
            updated
          }
        }
        else -> {
          val disabledOperator = operator.copy(
            enabled = false,
            smpRoles = ServerRoles(storage = false, proxy = false),
            xftpRoles = ServerRoles(storage = false, proxy = false),
          )
          val disabledSmp = group.smpServers.map { server ->
            if (server.enabled) server.copy(enabled = false) else server
          }
          val disabledXftp = group.xftpServers.map { server ->
            if (server.enabled) server.copy(enabled = false) else server
          }
          val updated = group.copy(
            operator = disabledOperator,
            smpServers = disabledSmp,
            xftpServers = disabledXftp,
          )
          if (updated != group) changed = true
          updated
        }
      }
    }.toMutableList()

    if (!hasNomeOperator) {
      var customIndex = migrated.indexOfFirst { it.operator == null }
      if (customIndex < 0) {
        migrated += UserOperatorServers(
          operator = null,
          smpServers = emptyList(),
          xftpServers = emptyList(),
        )
        customIndex = migrated.lastIndex
        changed = true
      }

      val custom = migrated[customIndex]
      val migratedSmp = enableManagedServer(custom.smpServers, ServerProtocol.SMP, currentSmpServer, managedServerPreset)
      val migratedXftp = enableManagedServer(custom.xftpServers, ServerProtocol.XFTP, xftpServer, managedServerPreset)
      val updatedCustom = custom.copy(
        smpServers = migratedSmp.first,
        xftpServers = migratedXftp.first,
      )
      if (migratedSmp.second || migratedXftp.second) changed = true
      migrated[customIndex] = updatedCustom
    }

    if (seedDefaultChatRelay && migrated.none { group -> group.chatRelays.any(::isNomeChatRelay) }) {
      val targetIndex = migrated.indexOfFirst { it.operator?.operatorTag == OperatorTag.Nome }
        .takeIf { it >= 0 }
        ?: migrated.indexOfFirst { it.operator == null }.takeIf { it >= 0 }
        ?: run {
          migrated += UserOperatorServers(
            operator = null,
            smpServers = emptyList(),
            xftpServers = emptyList(),
          )
          migrated.lastIndex
        }
      val target = migrated[targetIndex]
      migrated[targetIndex] = target.copy(chatRelays = target.chatRelays + defaultChatRelay())
      changed = true
    }

    return Migration(migrated, changed)
  }

  /** Runs before the core starts normal network work. It is safe to call on every startup. */
  internal suspend fun applyBeforeNetwork(controller: ChatController, user: User): ApplyResult =
    applyMutex.withLock {
      var changed = false
      val success = runWithRetry {
        val rh = user.remoteHostId
        val existing = controller.getUserServers(rh) ?: return@runWithRetry false
        val relaySeedKey = relaySeedKey(user)
        val shouldSeedDefaultChatRelay = controller.appPrefs.nomeDefaultChatRelaySeededUsers
          .get()[relaySeedKey] != true
        val migration = migrate(existing, seedDefaultChatRelay = shouldSeedDefaultChatRelay)
        if (!migration.changed) {
          if (shouldSeedDefaultChatRelay) markDefaultChatRelaySeeded(controller, relaySeedKey)
          return@runWithRetry true
        }

        val validation = controller.validateServers(rh, migration.userServers)
          ?: return@runWithRetry false
        if (validation.first.isNotEmpty()) {
          Log.e(LOG_TAG, "Nome server configuration validation failed")
          return@runWithRetry false
        }
        if (!controller.setUserServers(rh, migration.userServers, showError = false)) {
          return@runWithRetry false
        }
        if (shouldSeedDefaultChatRelay) markDefaultChatRelaySeeded(controller, relaySeedKey)
        changed = true
        controller.getServerOperators(rh)?.let { controller.chatModel.conditions.value = it }
        Log.i(LOG_TAG, "Nome servers active: $smpHostname, $xftpHostname, $chatRelayHostname")
        true
      }
      ApplyResult(success = success, changed = changed)
    }

  internal suspend fun runWithRetry(
    attempts: Int = 3,
    waitBeforeRetry: suspend (attempt: Int) -> Unit = { attempt -> delay(250L * attempt) },
    operation: suspend () -> Boolean,
  ): Boolean {
    require(attempts > 0)
    for (attempt in 1..attempts) {
      // Expected transient API outcomes are represented by `false` at the call sites. Native,
      // database and programming exceptions are not retry signals and must retain their original
      // failure semantics instead of being disguised as an endless configuration prompt.
      val applied = operation()
      if (applied) return true
      if (attempt < attempts) waitBeforeRetry(attempt)
    }
    return false
  }

  private fun enableManagedServer(
    servers: List<UserServer>,
    protocol: ServerProtocol,
    currentAddress: String,
    preset: Boolean,
  ): Pair<List<UserServer>, Boolean> {
    val matchingIndices = servers.indices.filter { isManagedServer(servers[it], protocol, currentAddress) }
    if (matchingIndices.isEmpty()) {
      return (servers + UserServer(
        remoteHostId = null,
        serverId = null,
        server = currentAddress,
        // The shipped Android core predates the Nome operator and keeps managed routes in the
        // custom bucket, where preset rows fail validation. Rebuilt Desktop cores use presets.
        preset = preset,
        tested = null,
        enabled = true,
        deleted = false,
      )) to true
    }

    val selectedIndex = matchingIndices.firstOrNull {
      servers[it].server.trim() == currentAddress
    } ?: matchingIndices.first()
    val updated = servers.mapIndexed { index, server ->
      when {
        index == selectedIndex -> {
          val addressChanged = server.server.trim() != currentAddress
          server.copy(
            server = currentAddress,
            preset = preset,
            tested = if (addressChanged) null else server.tested,
            enabled = true,
            deleted = false,
          )
        }
        index in matchingIndices -> server.copy(enabled = false, deleted = true)
        else -> server
      }
    }
    return updated to (updated != servers)
  }

  private fun retireManagedServers(
    servers: List<UserServer>,
    protocol: ServerProtocol,
    currentAddress: String,
  ): List<UserServer> = servers.map { server ->
    if (isManagedServer(server, protocol, currentAddress) && (server.enabled || !server.deleted)) {
      server.copy(enabled = false, deleted = true)
    } else {
      server
    }
  }

  private fun isManagedServer(server: UserServer, protocol: ServerProtocol, currentAddress: String): Boolean {
    val address = server.server.trim()
    val endpoint = parseEndpoint(address) ?: return false
    val managedHostnames = when (protocol) {
      ServerProtocol.SMP -> setOf(legacyHostname, smpHostname)
      ServerProtocol.XFTP -> setOf(legacyHostname, xftpHostname)
    }
    if (endpoint.protocol != protocol || endpoint.hostname !in managedHostnames) return false
    val knownLegacyAddress = when (protocol) {
      ServerProtocol.SMP -> legacySmpServer
      ServerProtocol.XFTP -> legacyXftpServer
    }
    return server.preset || address == currentAddress || address == knownLegacyAddress
  }

  private fun defaultChatRelay(): UserChatRelay = UserChatRelay(
    chatRelayId = null,
    address = chatRelayAddress,
    relayProfile = RelayProfile(displayName = chatRelayName, fullName = ""),
    domains = listOf(chatRelayDomain),
    preset = false,
    tested = null,
    enabled = true,
    deleted = false,
  )

  private fun isNomeChatRelay(relay: UserChatRelay): Boolean =
    relay.address.trim() == chatRelayAddress ||
      (relay.displayName == chatRelayName && chatRelayDomain in relay.domains)

  private fun relaySeedKey(user: User): String = "${user.remoteHostId ?: "local"}:${user.userId}"

  private fun markDefaultChatRelaySeeded(controller: ChatController, relaySeedKey: String) {
    val preference = controller.appPrefs.nomeDefaultChatRelaySeededUsers
    val seededUsers = preference.get()
    if (seededUsers[relaySeedKey] != true) preference.set(seededUsers + (relaySeedKey to true))
  }

  private data class Endpoint(val protocol: ServerProtocol, val hostname: String)

  private fun parseEndpoint(address: String): Endpoint? {
    val trimmed = address.trim()
    val schemeSeparator = trimmed.indexOf("://")
    if (schemeSeparator <= 0) return null
    val protocol = when (trimmed.substring(0, schemeSeparator).lowercase()) {
      "smp" -> ServerProtocol.SMP
      "xftp" -> ServerProtocol.XFTP
      else -> return null
    }
    val authority = trimmed.substring(schemeSeparator + 3).substringBefore('/')
    val hostAndPort = authority.substringAfterLast('@').substringBefore(',')
    val hostname = hostAndPort.substringBefore(':').lowercase()
    if (hostname.isBlank()) return null
    return Endpoint(protocol, hostname)
  }

  private const val LOG_TAG = "NOME_SERVERS"
}
