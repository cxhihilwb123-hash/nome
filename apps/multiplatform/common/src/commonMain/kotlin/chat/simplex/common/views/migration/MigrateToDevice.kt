package chat.simplex.common.views.migration

import SectionBottomSpacer
import SectionItemView
import SectionSpacer
import SectionTextFooter
import SectionView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.model.AppPreferences.Companion.SHARED_PREFS_MIGRATION_TO_STAGE
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.model.ChatController.getNetCfg
import chat.simplex.common.model.ChatController.startChat
import chat.simplex.common.model.ChatCtrl
import chat.simplex.common.model.ChatModel.controller
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.database.*
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.helpers.DatabaseUtils.ksDatabasePassword
import chat.simplex.common.views.newchat.QRCodeScanner
import chat.simplex.common.views.onboarding.OnboardingStage
import chat.simplex.common.views.usersettings.*
import chat.simplex.common.views.usersettings.networkAndServers.OnionRelatedLayout
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.*
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@Serializable
sealed class MigrationToDeviceState {
  @Serializable @SerialName("onion") data class Onion(val link: String, val socksProxy: String?, val networkProxy: NetworkProxy?, val hostMode: HostMode, val requiredHostMode: Boolean, val credentialRef: String? = null): MigrationToDeviceState()
  @Serializable @SerialName("downloadProgress") data class DownloadProgress(val link: String, val archiveName: String, val netCfg: NetCfg, val networkProxy: NetworkProxy?, val credentialRef: String? = null): MigrationToDeviceState()
  @Serializable @SerialName("archiveImport") data class ArchiveImport(val archiveName: String, val netCfg: NetCfg, val networkProxy: NetworkProxy?, val credentialRef: String? = null): MigrationToDeviceState()
  @Serializable @SerialName("passphrase") data class Passphrase(val netCfg: NetCfg, val networkProxy: NetworkProxy?, val credentialRef: String? = null): MigrationToDeviceState()

  companion object  {
    // Here we check whether it's needed to show migration process after app restart or not
    // It's important to NOT show the process when archive was corrupted/not fully downloaded
    fun makeMigrationState(): MigrationToState? {
      val stage = settings.getStringOrNull(SHARED_PREFS_MIGRATION_TO_STAGE)
      val persistedState: MigrationToDeviceState? = if (stage != null) json.decodeFromString(stage) else null
      val initial: MigrationToState? = when(val state = persistedState) {
        null -> null
        is DownloadProgress -> {
          // No migration happens at the moment actually since archive were not downloaded fully
          Log.e(TAG, "MigrateToDevice: archive wasn't fully downloaded, removed broken file")
          null
        }
        is Onion -> null
        is ArchiveImport -> {
          if (!File(getMigrationTempFilesDirectory(), state.archiveName).exists()) {
            Log.e(TAG, "MigrateToDevice: archive was removed unintentionally or state is broken, dropping migration")
            null
          } else {
            val restored = if (appPlatform.isDesktop) state.restoreCredential() as ArchiveImport else state
            val archivePath = File(getMigrationTempFilesDirectory(), state.archiveName)
            MigrationToState.ArchiveImportFailed(archivePath.absolutePath, restored.netCfg, restored.networkProxy)
          }
        }
        is Passphrase -> {
          val restored = if (appPlatform.isDesktop) state.restoreCredential() as Passphrase else state
          MigrationToState.Passphrase("", restored.netCfg, restored.networkProxy)
        }
      }
      if (initial == null) {
        clearPersistedState()
        getMigrationTempFilesDirectory().deleteRecursively()
      }
      return initial
    }

    fun save(state: MigrationToDeviceState?) {
      if (state != null) {
        if (appPlatform.isDesktop) {
          val credential = state.proxyCredential()
          val persisted = if (credential != null) {
            migrationProxyCredentialVault.store(credential)
            state.redactedForPersistence(MIGRATION_PROXY_CREDENTIAL_REF)
          } else {
            state.redactedForPersistence(null)
          }
          settings.putString(SHARED_PREFS_MIGRATION_TO_STAGE, json.encodeToString(persisted))
          if (credential == null) migrationProxyCredentialVault.remove()
        } else {
          // Onion and in-progress download stages are discarded at restart,
          // so their bearer file links must never be written to settings.
          settings.putString(SHARED_PREFS_MIGRATION_TO_STAGE, json.encodeToString(state.withoutTransientFileLink()))
        }
      } else {
        clearPersistedState()
      }
    }

    private fun clearPersistedState() {
      settings.remove(SHARED_PREFS_MIGRATION_TO_STAGE)
      if (appPlatform.isDesktop) migrationProxyCredentialVault.remove()
    }
  }
}

private const val MIGRATION_PROXY_CREDENTIAL_REF = "migrationNetworkProxy"
private const val KEYCHAIN_DATA_MARKER = "nome-keychain-v1"
private const val KEYCHAIN_IV_MARKER = "credential-reference"

private val migrationProxyCredentialVault by lazy { MigrationProxyCredentialVault(cryptor) }

internal class MigrationProxyCredentialVault(
  private val credentialCryptor: CryptorInterface,
) {
  fun store(proxy: NetworkProxy) {
    val markers = try {
      credentialCryptor.encryptText(json.encodeToString(proxy), MIGRATION_PROXY_CREDENTIAL_REF)
    } catch (e: Throwable) {
      throw CredentialUnavailable("migration proxy", e)
    }
    if (!markers.first.contentEquals(KEYCHAIN_DATA_MARKER.toByteArray(Charsets.UTF_8)) ||
      !markers.second.contentEquals(KEYCHAIN_IV_MARKER.toByteArray(Charsets.UTF_8))) {
      throw CredentialUnavailable("migration proxy")
    }
  }

  fun load(reference: String): NetworkProxy {
    if (reference != MIGRATION_PROXY_CREDENTIAL_REF) throw CredentialUnavailable("migration proxy")
    val plaintext = try {
      credentialCryptor.decryptData(
        KEYCHAIN_DATA_MARKER.toByteArray(Charsets.UTF_8),
        KEYCHAIN_IV_MARKER.toByteArray(Charsets.UTF_8),
        reference,
      )
    } catch (e: Throwable) {
      throw CredentialUnavailable("migration proxy", e)
    } ?: throw CredentialUnavailable("migration proxy")
    return runCatching { json.decodeFromString<NetworkProxy>(plaintext) }.getOrNull()
      ?: throw CredentialUnavailable("migration proxy")
  }

  fun remove() {
    try {
      credentialCryptor.deleteKey(MIGRATION_PROXY_CREDENTIAL_REF)
    } catch (e: Throwable) {
      throw CredentialUnavailable("migration proxy", e)
    }
  }
}

private fun MigrationToDeviceState.restoreCredential(): MigrationToDeviceState {
  val reference = credentialReference()
  if (reference != null) {
    val proxy = migrationProxyCredentialVault.load(reference)
    if (!matchesCredentialEndpoint(proxy)) throw CredentialUnavailable("migration proxy")
    return withRestoredCredential(proxy)
  }

  // Upgrade a pre-Keychain resume state in place before it is used again.
  val legacyCredential = proxyCredential() ?: return this
  migrationProxyCredentialVault.store(legacyCredential)
  val persisted = redactedForPersistence(MIGRATION_PROXY_CREDENTIAL_REF)
  settings.putString(SHARED_PREFS_MIGRATION_TO_STAGE, json.encodeToString(persisted))
  return withRestoredCredential(legacyCredential)
}

internal fun MigrationToDeviceState.redactedForPersistence(reference: String?): MigrationToDeviceState = when (this) {
  is MigrationToDeviceState.Onion -> {
    val safeSocksProxy = socksProxy.withoutProxyCredentials()
    val safeNetworkProxy = networkProxy?.withoutCredentials()
    copy(
      link = "",
      socksProxy = safeSocksProxy,
      networkProxy = safeNetworkProxy,
      credentialRef = reference,
    )
  }
  is MigrationToDeviceState.DownloadProgress -> copy(
    link = "",
    netCfg = netCfg.copy(socksProxy = netCfg.socksProxy.withoutProxyCredentials()),
    networkProxy = networkProxy?.withoutCredentials(),
    credentialRef = reference,
  )
  is MigrationToDeviceState.ArchiveImport -> copy(
    netCfg = netCfg.copy(socksProxy = netCfg.socksProxy.withoutProxyCredentials()),
    networkProxy = networkProxy?.withoutCredentials(),
    credentialRef = reference,
  )
  is MigrationToDeviceState.Passphrase -> copy(
    netCfg = netCfg.copy(socksProxy = netCfg.socksProxy.withoutProxyCredentials()),
    networkProxy = networkProxy?.withoutCredentials(),
    credentialRef = reference,
  )
}

internal fun MigrationToDeviceState.withoutTransientFileLink(): MigrationToDeviceState = when (this) {
  is MigrationToDeviceState.Onion -> copy(link = "")
  is MigrationToDeviceState.DownloadProgress -> copy(link = "")
  is MigrationToDeviceState.ArchiveImport,
  is MigrationToDeviceState.Passphrase -> this
}

private fun MigrationToDeviceState.withRestoredCredential(proxy: NetworkProxy): MigrationToDeviceState = when (this) {
  is MigrationToDeviceState.Onion -> copy(
    socksProxy = socksProxy?.let { proxy.toProxyString() },
    networkProxy = proxy,
  )
  is MigrationToDeviceState.DownloadProgress -> copy(
    netCfg = netCfg.copy(socksProxy = netCfg.socksProxy?.let { proxy.toProxyString() }),
    networkProxy = proxy,
  )
  is MigrationToDeviceState.ArchiveImport -> copy(
    netCfg = netCfg.copy(socksProxy = netCfg.socksProxy?.let { proxy.toProxyString() }),
    networkProxy = proxy,
  )
  is MigrationToDeviceState.Passphrase -> copy(
    netCfg = netCfg.copy(socksProxy = netCfg.socksProxy?.let { proxy.toProxyString() }),
    networkProxy = proxy,
  )
}

private fun MigrationToDeviceState.proxyCredential(): NetworkProxy? {
  val configuredProxy = when (this) {
    is MigrationToDeviceState.Onion -> networkProxy
    is MigrationToDeviceState.DownloadProgress -> networkProxy
    is MigrationToDeviceState.ArchiveImport -> networkProxy
    is MigrationToDeviceState.Passphrase -> networkProxy
  }
  if (configuredProxy?.hasCredentials() == true) return configuredProxy
  return proxyString()?.toNetworkProxyWithCredentials()
}

private fun MigrationToDeviceState.credentialReference(): String? = when (this) {
  is MigrationToDeviceState.Onion -> credentialRef
  is MigrationToDeviceState.DownloadProgress -> credentialRef
  is MigrationToDeviceState.ArchiveImport -> credentialRef
  is MigrationToDeviceState.Passphrase -> credentialRef
}

private fun MigrationToDeviceState.proxyString(): String? = when (this) {
  is MigrationToDeviceState.Onion -> socksProxy
  is MigrationToDeviceState.DownloadProgress -> netCfg.socksProxy
  is MigrationToDeviceState.ArchiveImport -> netCfg.socksProxy
  is MigrationToDeviceState.Passphrase -> netCfg.socksProxy
}

private fun MigrationToDeviceState.matchesCredentialEndpoint(proxy: NetworkProxy): Boolean {
  val safeProxy = when (this) {
    is MigrationToDeviceState.Onion -> networkProxy
    is MigrationToDeviceState.DownloadProgress -> networkProxy
    is MigrationToDeviceState.ArchiveImport -> networkProxy
    is MigrationToDeviceState.Passphrase -> networkProxy
  }
  if (safeProxy != null) return safeProxy.host == proxy.host && safeProxy.port == proxy.port
  val endpoint = proxyString()?.substringAfterLast('@')?.let(::parseNetworkProxyPreference)
  return endpoint != null && endpoint.host == proxy.host && endpoint.port == proxy.port
}

private fun NetworkProxy.hasCredentials(): Boolean = username.isNotBlank() || password.isNotBlank()

private fun NetworkProxy.withoutCredentials(): NetworkProxy = copy(username = "", password = "")

private fun String?.withoutProxyCredentials(): String? {
  if (this == null) return null
  val separator = lastIndexOf('@')
  if (separator <= 0 || !substring(0, separator).contains(':')) return this
  return "@${substring(separator + 1)}"
}

private fun String.toNetworkProxyWithCredentials(): NetworkProxy? {
  val separator = lastIndexOf('@')
  if (separator <= 0) return null
  val userInfo = substring(0, separator)
  val passwordSeparator = userInfo.indexOf(':')
  if (passwordSeparator < 0) return null
  val endpoint = parseNetworkProxyPreference(substring(separator + 1)) ?: return null
  return endpoint.copy(
    username = userInfo.substring(0, passwordSeparator),
    password = userInfo.substring(passwordSeparator + 1),
    auth = NetworkProxyAuth.USERNAME,
  ).takeIf { it.hasCredentials() }
}

/**
 * Restores the editable proxy model used by the migration confirmation screen.
 * A leading `@` is the deliberate marker left after link credentials are
 * stripped; it must select username authentication without becoming part of
 * the proxy host.
 */
internal fun networkProxyFromLegacyMigrationValue(value: String): NetworkProxy =
  value.toNetworkProxyWithCredentials()
    ?: parseNetworkProxyPreference(value.substringAfterLast('@'))?.let { endpoint ->
      if (value.startsWith("@")) endpoint.copy(auth = NetworkProxyAuth.USERNAME) else endpoint
    }
    ?: NetworkProxy()

@Serializable
sealed class MigrationToState {
  @Serializable object PasteOrScanLink: MigrationToState()
  @Serializable data class Onion(
    val link: String,
    // Legacy, remove in 2025
    @SerialName("socksProxy")
    val legacySocksProxy: String?,
    val networkProxy: NetworkProxy?,
    val hostMode: HostMode,
    val requiredHostMode: Boolean
  ): MigrationToState()
  @Serializable data class DatabaseInit(val link: String, val netCfg: NetCfg, val networkProxy: NetworkProxy?): MigrationToState()
  @Serializable data class LinkDownloading(val link: String, val ctrl: ChatCtrl, val user: User, val archivePath: String, val netCfg: NetCfg, val networkProxy: NetworkProxy?): MigrationToState()
  @Serializable data class DownloadProgress(val downloadedBytes: Long, val totalBytes: Long, val fileId: Long, val link: String, val archivePath: String, val netCfg: NetCfg, val networkProxy: NetworkProxy?, val ctrl: ChatCtrl?): MigrationToState()
  @Serializable data class DownloadFailed(val totalBytes: Long, val link: String, val archivePath: String, val netCfg: NetCfg, val networkProxy: NetworkProxy?): MigrationToState()
  @Serializable data class ArchiveImport(val archivePath: String, val netCfg: NetCfg, val networkProxy: NetworkProxy?): MigrationToState()
  @Serializable data class ArchiveImportFailed(val archivePath: String, val netCfg: NetCfg, val networkProxy: NetworkProxy?): MigrationToState()
  @Serializable data class Passphrase(val passphrase: String, val netCfg: NetCfg, val networkProxy: NetworkProxy?): MigrationToState()
  @Serializable data class MigrationConfirmation(val status: DBMigrationResult, val passphrase: String, val useKeychain: Boolean, val netCfg: NetCfg, val networkProxy: NetworkProxy?): MigrationToState()
  @Serializable data class Migration(val passphrase: String, val confirmation: chat.simplex.common.views.helpers.MigrationConfirmation, val useKeychain: Boolean, val netCfg: NetCfg, val networkProxy: NetworkProxy?): MigrationToState()
}

private var MutableState<MigrationToState?>.state: MigrationToState?
  get() = value
  set(v) { value = v }

@Composable
fun ModalData.MigrateToDeviceView(close: () -> Unit) {
  val migrationState = remember { chatModel.migrationState }
  val desktopStartPage = appPlatform.isDesktop && migrationState.value is MigrationToState.PasteOrScanLink
  // Prevent from hiding the view until migration is finished or app deleted
  val backDisabled = remember {
    derivedStateOf {
      when (chatModel.migrationState.value) {
        null,
        is MigrationToState.PasteOrScanLink,
        is MigrationToState.Onion,
        is MigrationToState.LinkDownloading,
        is MigrationToState.DownloadProgress,
        is MigrationToState.DownloadFailed,
        is MigrationToState.ArchiveImportFailed -> false

        is MigrationToState.ArchiveImport,
        is MigrationToState.DatabaseInit,
        is MigrationToState.Migration,
        is MigrationToState.MigrationConfirmation,
        is MigrationToState.Passphrase -> true
      }
    }
  }
  val chatReceiver = remember { mutableStateOf(null as MigrationToChatReceiver?) }
  ModalView(
    showClose = !desktopStartPage,
    showAppBar = !desktopStartPage,
    enableClose = !backDisabled.value,
    close = {
      withBGApi {
        migrationState.cleanUpOnBack(chatReceiver.value)
        close()
      }
    },
  ) {
    MigrateToDeviceLayout(
      migrationState = migrationState,
      chatReceiver = chatReceiver,
      close = close,
    )
  }
}

@Composable
private fun ModalData.MigrateToDeviceLayout(
  migrationState: MutableState<MigrationToState?>,
  chatReceiver: MutableState<MigrationToChatReceiver?>,
  close: () -> Unit,
) {
  val tempDatabaseFile = rememberSaveable { mutableStateOf(fileForTemporaryDatabase()) }
  if (appPlatform.isDesktop && migrationState.value is MigrationToState.PasteOrScanLink) {
    DesktopMigrateToDeviceStart(
      migrationState = migrationState,
      chatReceiver = chatReceiver,
      close = close,
    )
  } else {
    ColumnWithScrollBar(maxIntrinsicSize = true) {
      AppBarTitle(stringResource(MR.strings.migrate_to_device_title))
      SectionByState(migrationState, tempDatabaseFile.value, chatReceiver, close)
      SectionBottomSpacer()
    }
  }
  platform.androidLockPortraitOrientation()
}

@Composable
private fun ModalData.DesktopMigrateToDeviceStart(
  migrationState: MutableState<MigrationToState?>,
  chatReceiver: MutableState<MigrationToChatReceiver?>,
  close: () -> Unit,
) {
  val clipboard = LocalClipboardManager.current
  val progressIndicator = remember { mutableStateOf(false) }
  val backDescription = stringResource(MR.strings.nome_desktop_back)
  val importArchiveLauncher = rememberFileChooserLauncher(true) { to: URI? ->
    if (to != null) {
      withLongRunningApi {
        val success = importArchive(to, mutableStateOf(0 to 0), progressIndicator, true)
        if (success) {
          startChat(
            chatModel,
            mutableStateOf(Clock.System.now()),
            chatModel.chatDbChanged,
            progressIndicator
          )
          hideView(close)
        }
      }
    }
  }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MigrationDesktopColors.Background)
  ) {
    Column(Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(62.dp)
          .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = {
            withBGApi {
              migrationState.cleanUpOnBack(chatReceiver.value)
              close()
            }
          },
          modifier = Modifier.semantics {
            contentDescription = backDescription
          },
        ) {
          Icon(
            painter = painterResource(MR.images.ic_arrow_back_ios_new),
            contentDescription = backDescription,
            tint = MigrationDesktopColors.Text,
            modifier = Modifier.size(19.dp),
          )
        }
        Image(
          painter = painterResource(MR.images.nome_mark),
          contentDescription = "Nome",
          modifier = Modifier.size(26.dp),
        )
        Text(
          text = "Nome",
          color = MigrationDesktopColors.Text,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
          text = stringResource(MR.strings.nome_desktop_migration_label),
          color = MigrationDesktopColors.TextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.8.sp,
        )
      }
      Divider(color = MigrationDesktopColors.Divider)
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 44.dp),
        ) {
          Text(
            text = stringResource(MR.strings.nome_desktop_migration_title),
            modifier = Modifier.semantics { heading() },
            color = MigrationDesktopColors.Text,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.7).sp,
          )
          Text(
            text = stringResource(MR.strings.nome_desktop_migration_body),
            color = MigrationDesktopColors.TextMuted,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            modifier = Modifier.padding(top = 12.dp).widthIn(max = 650.dp),
          )
          Spacer(Modifier.height(34.dp))
          Surface(
            color = MigrationDesktopColors.Surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MigrationDesktopColors.Border),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
              DesktopMigrationInfoRow(
                icon = painterResource(MR.images.ic_link),
                title = stringResource(MR.strings.nome_desktop_migration_link_title),
                body = stringResource(MR.strings.nome_desktop_migration_link_body),
              )
              Divider(color = MigrationDesktopColors.Divider)
              DesktopMigrationInfoRow(
                icon = painterResource(MR.images.ic_database),
                title = stringResource(MR.strings.nome_desktop_migration_import_title),
                body = stringResource(MR.strings.nome_desktop_migration_import_body),
              )
            }
          }
          Spacer(Modifier.height(20.dp))
          DesktopMigrationNotice(
            icon = painterResource(MR.images.ic_lock),
            body = stringResource(MR.strings.nome_desktop_migration_notice),
          )
        }
      }
      Divider(color = MigrationDesktopColors.Divider)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MigrationDesktopColors.Surface)
          .padding(horizontal = 34.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        DesktopSecondaryButton(
          text = stringResource(MR.strings.import_database),
          enabled = !progressIndicator.value,
          onClick = { withLongRunningApi { importArchiveLauncher.launch("application/zip") } },
        )
        Spacer(Modifier.width(12.dp))
        DesktopPrimaryButton(
          text = stringResource(MR.strings.paste_archive_link),
          enabled = !progressIndicator.value,
          onClick = {
            val str = clipboard.getText()?.text ?: return@DesktopPrimaryButton
            withBGApi { migrationState.checkUserLink(str) }
          },
        )
      }
    }
    if (progressIndicator.value) {
      ProgressView()
    }
  }
}

@Composable
private fun ModalData.SectionByState(
  migrationState: MutableState<MigrationToState?>,
  tempDatabaseFile: File,
  chatReceiver: MutableState<MigrationToChatReceiver?>,
  close: () -> Unit
) {
  when (val s = migrationState.value) {
    null -> {}
    is MigrationToState.PasteOrScanLink -> migrationState.PasteOrScanLinkView(close)
    is MigrationToState.Onion -> OnionView(s.link, s.legacySocksProxy, s.networkProxy, s.hostMode, s.requiredHostMode, migrationState)
    is MigrationToState.DatabaseInit -> migrationState.DatabaseInitView(s.link, tempDatabaseFile, s.netCfg, s.networkProxy)
    is MigrationToState.LinkDownloading -> migrationState.LinkDownloadingView(s.link, s.ctrl, s.user, s.archivePath, tempDatabaseFile, chatReceiver, s.netCfg, s.networkProxy)
    is MigrationToState.DownloadProgress -> DownloadProgressView(s.downloadedBytes, totalBytes = s.totalBytes)
    is MigrationToState.DownloadFailed -> migrationState.DownloadFailedView(s.link, chatReceiver.value, s.archivePath, s.netCfg, s.networkProxy)
    is MigrationToState.ArchiveImport -> migrationState.ArchiveImportView(s.archivePath, s.netCfg, s.networkProxy)
    is MigrationToState.ArchiveImportFailed -> migrationState.ArchiveImportFailedView(s.archivePath, s.netCfg, s.networkProxy)
    is MigrationToState.Passphrase -> migrationState.PassphraseEnteringView(currentKey = s.passphrase, s.netCfg, s.networkProxy)
    is MigrationToState.MigrationConfirmation -> migrationState.MigrationConfirmationView(s.status, s.passphrase, s.useKeychain, s.netCfg, s.networkProxy)
    is MigrationToState.Migration -> MigrationView(s.passphrase, s.confirmation, s.useKeychain, s.netCfg, s.networkProxy, close)
  }
}

@Composable
private fun MutableState<MigrationToState?>.PasteOrScanLinkView(close: () -> Unit) {
  Box {
    val progressIndicator = remember { mutableStateOf(false) }
    Column {
      if (appPlatform.isAndroid) {
        SectionView(stringResource(MR.strings.scan_QR_code).replace('\n', ' ').uppercase()) {
          QRCodeScanner(showQRCodeScanner = remember { mutableStateOf(true) }) { text ->
            checkUserLink(text)
          }
        }
        SectionSpacer()
      }

      SectionView(stringResource(if (appPlatform.isAndroid) MR.strings.or_paste_archive_link else MR.strings.paste_archive_link).uppercase()) {
        PasteLinkView()
      }
      SectionSpacer()

      SectionView(stringResource(MR.strings.chat_archive).uppercase()) {
        ArchiveImportView(progressIndicator, close)
      }
    }
    if (progressIndicator.value)
    ProgressView()
  }
}

@Composable
private fun MutableState<MigrationToState?>.PasteLinkView() {
  val clipboard = LocalClipboardManager.current
  SectionItemView({
    val str = clipboard.getText()?.text ?: return@SectionItemView
    withBGApi { checkUserLink(str) }
  }) {
    Text(stringResource(MR.strings.tap_to_paste_link))
  }
}

@Composable
private fun ArchiveImportView(progressIndicator: MutableState<Boolean>, close: () -> Unit) {
  val importArchiveLauncher = rememberFileChooserLauncher(true) { to: URI? ->
    if (to != null) {
      withLongRunningApi {
        val success = importArchive(to, mutableStateOf(0 to 0), progressIndicator, true)
        if (success) {
          startChat(
            chatModel,
            mutableStateOf(Clock.System.now()),
            chatModel.chatDbChanged,
            progressIndicator
          )
          hideView(close)
        }
      }
    }
  }
  SectionItemView({
    withLongRunningApi { importArchiveLauncher.launch("application/zip") }
  }) {
    Text(stringResource(MR.strings.import_database))
  }
}

@Composable
private fun DesktopMigrationInfoRow(
  icon: Painter,
  title: String,
  body: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      modifier = Modifier.size(40.dp),
      shape = RoundedCornerShape(11.dp),
      color = MigrationDesktopColors.MintPale,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter = icon,
          contentDescription = null,
          tint = MigrationDesktopColors.Action,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
      Text(
        text = title,
        color = MigrationDesktopColors.Text,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = body,
        color = MigrationDesktopColors.TextMuted,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(top = 3.dp),
      )
    }
  }
}

@Composable
private fun DesktopMigrationNotice(
  icon: Painter,
  body: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MigrationDesktopColors.MintPale, RoundedCornerShape(12.dp))
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = icon,
      contentDescription = null,
      tint = MigrationDesktopColors.Action,
      modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(11.dp))
    Text(
      text = body,
      color = MigrationDesktopColors.TextMuted,
      fontSize = 12.sp,
      lineHeight = 17.sp,
    )
  }
}

@Composable
private fun DesktopPrimaryButton(
  text: String,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier
      .height(46.dp)
      .widthIn(min = 190.dp)
      .semantics { contentDescription = text },
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      backgroundColor = MigrationDesktopColors.Action,
      contentColor = Color.White,
      disabledBackgroundColor = MigrationDesktopColors.Disabled,
      disabledContentColor = Color.White.copy(alpha = 0.8f),
    ),
    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
    contentPadding = PaddingValues(horizontal = 22.dp),
  ) {
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun DesktopSecondaryButton(
  text: String,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier
      .height(46.dp)
      .semantics { contentDescription = text },
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MigrationDesktopColors.Border),
    colors = ButtonDefaults.buttonColors(
      backgroundColor = MigrationDesktopColors.Surface,
      contentColor = MigrationDesktopColors.Text,
      disabledBackgroundColor = MigrationDesktopColors.Surface,
      disabledContentColor = MigrationDesktopColors.TextMuted,
    ),
    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
    contentPadding = PaddingValues(horizontal = 20.dp),
  ) {
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
  }
}

private object MigrationDesktopColors {
  val MintPale = Color(0xFFE8F6F0)
  val Action = Color(0xFF0A874D)
  val Background = Color(0xFFF5F7FA)
  val Surface = Color(0xFFFFFFFF)
  val Text = Color(0xFF0E1B2D)
  val TextMuted = Color(0xFF667085)
  val Border = Color(0xFFDCE3E9)
  val Divider = Color(0xFFE7ECF1)
  val Disabled = Color(0xFF9FB4AA)
}

@Composable
private fun ModalData.OnionView(link: String, legacyLinkSocksProxy: String?, linkNetworkProxy: NetworkProxy?, hostMode: HostMode, requiredHostMode: Boolean, state: MutableState<MigrationToState?>) {
  val onionHosts = remember { stateGetOrPut("onionHosts") {
    getNetCfg().copy(socksProxy = linkNetworkProxy?.toProxyString() ?: legacyLinkSocksProxy, hostMode = hostMode, requiredHostMode = requiredHostMode).onionHosts
  } }
  val networkUseSocksProxy = remember { stateGetOrPut("networkUseSocksProxy") { linkNetworkProxy != null || legacyLinkSocksProxy != null } }
  val sessionMode = remember { stateGetOrPut("sessionMode") { TransportSessionMode.User} }
  val networkProxy = remember { stateGetOrPut("networkProxy") {
    linkNetworkProxy
      ?: if (legacyLinkSocksProxy != null) {
        networkProxyFromLegacyMigrationValue(legacyLinkSocksProxy)
      } else {
        appPrefs.networkProxy.get()
      }
    }
  }

  val netCfg = rememberSaveable(stateSaver = serializableSaver()) {
    mutableStateOf(getNetCfg().withOnionHosts(onionHosts.value).copy(socksProxy = linkNetworkProxy?.toProxyString() ?: legacyLinkSocksProxy, sessionMode = sessionMode.value))
  }

  SectionView(stringResource(MR.strings.migrate_to_device_confirm_network_settings).uppercase()) {
    SettingsActionItemWithContent(
      icon = painterResource(MR.images.ic_check),
      text = stringResource(MR.strings.migrate_to_device_apply_onion),
      textColor = MaterialTheme.colors.primary,
      click = {
        val updated = netCfg.value
          .withOnionHosts(onionHosts.value)
          .withProxy(if (networkUseSocksProxy.value) networkProxy.value else null, null)
          .copy(
            sessionMode = sessionMode.value
          )
        withBGApi {
          state.value = MigrationToState.DatabaseInit(link, updated, if (networkUseSocksProxy.value) networkProxy.value else null)
        }
      }
    ){}
    SectionTextFooter(stringResource(MR.strings.migrate_to_device_confirm_network_settings_footer))
  }

  SectionSpacer()

  val networkProxyPref = SharedPreference(get = { networkProxy.value }, set = {
    networkProxy.value = it
  })
  SectionView(stringResource(MR.strings.network_settings_title).uppercase()) {
    OnionRelatedLayout(
      appPreferences.developerTools.get(),
      networkUseSocksProxy,
      onionHosts,
      sessionMode,
      networkProxyPref,
      toggleSocksProxy = { enable ->
        networkUseSocksProxy.value = enable
      },
      updateSessionMode = {
        sessionMode.value = it
      }
    )
  }
}

@Composable
private fun MutableState<MigrationToState?>.DatabaseInitView(link: String, tempDatabaseFile: File, netCfg: NetCfg, networkProxy: NetworkProxy?) {
  Box {
    SectionView(stringResource(MR.strings.migrate_to_device_database_init).uppercase()) {}
    ProgressView()
  }
  LaunchedEffect(Unit) {
    prepareDatabase(link, tempDatabaseFile, netCfg, networkProxy)
  }
}

@Composable
private fun MutableState<MigrationToState?>.LinkDownloadingView(
  link: String,
  ctrl: ChatCtrl,
  user: User,
  archivePath: String,
  tempDatabaseFile: File,
  chatReceiver: MutableState<MigrationToChatReceiver?>,
  netCfg: NetCfg,
  networkProxy: NetworkProxy?
) {
  Box {
    SectionView(stringResource(MR.strings.migrate_to_device_downloading_details).uppercase()) {}
    ProgressView()
  }
  LaunchedEffect(Unit) {
    startDownloading(0, ctrl, user, tempDatabaseFile, chatReceiver, link, archivePath, netCfg, networkProxy)
  }
}

@Composable
private fun DownloadProgressView(downloadedBytes: Long, totalBytes: Long) {
  Box {
    SectionView(stringResource(MR.strings.migrate_to_device_downloading_archive).uppercase()) {
      val ratio = downloadedBytes.toFloat() / max(totalBytes, 1)
      LargeProgressView(ratio, "${(ratio * 100).toInt()}%", stringResource(MR.strings.migrate_to_device_bytes_downloaded).format(formatBytes(downloadedBytes)))
    }
  }
}

@Composable
private fun MutableState<MigrationToState?>.DownloadFailedView(link: String, chatReceiver: MigrationToChatReceiver?, archivePath: String, netCfg: NetCfg, networkProxy: NetworkProxy?) {
  SectionView(stringResource(MR.strings.migrate_to_device_download_failed).uppercase()) {
    SettingsActionItemWithContent(
      icon = painterResource(MR.images.ic_download),
      text = stringResource(MR.strings.migrate_to_device_repeat_download),
      textColor = MaterialTheme.colors.primary,
      click = {
        state = MigrationToState.DatabaseInit(link, netCfg, networkProxy)
      }
    ) {}
    SectionTextFooter(stringResource(MR.strings.migrate_to_device_try_again))
  }
  LaunchedEffect(Unit) {
    chatReceiver?.stopAndCleanUp()
    File(archivePath).delete()
    MigrationToDeviceState.save(null)
  }
}

@Composable
private fun MutableState<MigrationToState?>.ArchiveImportView(archivePath: String, netCfg: NetCfg, networkProxy: NetworkProxy?) {
  Box {
    SectionView(stringResource(MR.strings.migrate_to_device_importing_archive).uppercase()) {}
    ProgressView()
  }
  LaunchedEffect(Unit) {
    importArchive(archivePath, netCfg, networkProxy)
  }
}

@Composable
private fun MutableState<MigrationToState?>.ArchiveImportFailedView(archivePath: String, netCfg: NetCfg, networkProxy: NetworkProxy?) {
  SectionView(stringResource(MR.strings.migrate_to_device_import_failed).uppercase()) {
    SettingsActionItemWithContent(
      icon = painterResource(MR.images.ic_download),
      text = stringResource(MR.strings.migrate_to_device_repeat_import),
      textColor = MaterialTheme.colors.primary,
      click = {
        state = MigrationToState.ArchiveImport(archivePath, netCfg, networkProxy)
      }
    ) {}
    SectionTextFooter(stringResource(MR.strings.migrate_to_device_try_again))
  }
}

@Composable
private fun MutableState<MigrationToState?>.PassphraseEnteringView(currentKey: String, netCfg: NetCfg, networkProxy: NetworkProxy?) {
  val currentKey = rememberSaveable { mutableStateOf(currentKey) }
  val verifyingPassphrase = rememberSaveable { mutableStateOf(false) }
  val useKeychain = rememberSaveable { mutableStateOf(appPreferences.storeDBPassphrase.get()) }

  Box {
    val view = LocalMultiplatformView()
    SectionView(stringResource(MR.strings.migrate_to_device_enter_passphrase).uppercase()) {
      SavePassphraseSetting(
        useKeychain.value,
        false,
        false,
        enabled = !verifyingPassphrase.value,
        smallPadding = false
      ) { checked -> useKeychain.value = checked }

      PassphraseField(currentKey, placeholder = stringResource(MR.strings.current_passphrase), Modifier.padding(horizontal = DEFAULT_PADDING), isValid = ::validKey, requestFocus = true)

      SettingsActionItemWithContent(
        icon = painterResource(MR.images.ic_vpn_key_filled),
        text = stringResource(MR.strings.open_chat),
        textColor = MaterialTheme.colors.primary,
        disabled = verifyingPassphrase.value || currentKey.value.isEmpty(),
        click = {
          verifyingPassphrase.value = true
          hideKeyboard(view)
          withBGApi {
            val (status, _) = chatInitTemporaryDatabase(dbAbsolutePrefixPath, key = currentKey.value, confirmation = MigrationConfirmation.YesUp)
            val success = status == DBMigrationResult.OK || status == DBMigrationResult.InvalidConfirmation
            if (success) {
              state = MigrationToState.Migration(currentKey.value, MigrationConfirmation.YesUp, useKeychain.value, netCfg, networkProxy)
            } else if (status is DBMigrationResult.ErrorMigration) {
              state = MigrationToState.MigrationConfirmation(status, currentKey.value, useKeychain.value, netCfg, networkProxy)
            } else {
              showErrorOnMigrationIfNeeded(status)
            }
            verifyingPassphrase.value = false
          }
        }
      ) {}
      DatabaseEncryptionFooter(useKeychain, chatDbEncrypted = true, remember { mutableStateOf(false) }, remember { mutableStateOf(false) }, true)
    }
    if (verifyingPassphrase.value) {
      ProgressView()
    }
  }
}

@Composable
private fun MutableState<MigrationToState?>.MigrationConfirmationView(status: DBMigrationResult, passphrase: String, useKeychain: Boolean, netCfg: NetCfg, networkProxy: NetworkProxy?) {
  data class Tuple4<A,B,C,D>(val a: A, val b: B, val c: C, val d: D)
  val (header: String, button: String?, footer: String, confirmation: MigrationConfirmation?) = when (status) {
    is DBMigrationResult.ErrorMigration -> when (val err = status.migrationError) {
      is MigrationError.Upgrade ->
        Tuple4(
          generalGetString(MR.strings.database_upgrade),
          generalGetString(MR.strings.upgrade_and_open_chat),
          "",
          MigrationConfirmation.YesUp
        )
      is MigrationError.Downgrade ->
        Tuple4(
          generalGetString(MR.strings.database_downgrade),
          generalGetString(MR.strings.downgrade_and_open_chat),
          (listOf(generalGetString(MR.strings.database_downgrade_warning))
            + downMigrationWarnings(err.downMigrations).reversed())
            .joinToString("\n"),
          MigrationConfirmation.YesUpDown
        )
      is MigrationError.Error ->
        Tuple4(
          generalGetString(MR.strings.incompatible_database_version),
          null,
          mtrErrorDescription(err.mtrError),
          null
        )
    }
    else -> Tuple4(generalGetString(MR.strings.error), null, generalGetString(MR.strings.unknown_error), null)
  }
  SectionView(header.uppercase()) {
    if (button != null && confirmation != null) {
      SettingsActionItemWithContent(
        icon = painterResource(MR.images.ic_download),
        text = button,
        textColor = MaterialTheme.colors.primary,
        click = {
          state = MigrationToState.Migration(passphrase, confirmation, useKeychain, netCfg, networkProxy)
        }
      ) {}
    }
    SectionTextFooter(footer)
  }
}

@Composable
private fun MigrationView(passphrase: String, confirmation: MigrationConfirmation, useKeychain: Boolean, netCfg: NetCfg, networkProxy: NetworkProxy?, close: () -> Unit) {
  Box {
    SectionView(stringResource(MR.strings.migrate_to_device_migrating).uppercase()) {}
    ProgressView()
  }
  LaunchedEffect(Unit) {
    startChat(passphrase, confirmation, useKeychain, netCfg, networkProxy, close)
  }
}

@Composable
private fun ProgressView() {
  DefaultProgressView(null)
}

private suspend fun MutableState<MigrationToState?>.checkUserLink(link: String): Boolean {
  return if (strHasSimplexFileLink(link.trim())) {
    val data = MigrationFileLinkData.readFromLink(link)
    val hasProxyConfigured = data?.networkConfig?.hasProxyConfigured() ?: false
    val networkConfig = data?.networkConfig?.transformToPlatformSupported()
    // If any of iOS or Android had onion enabled, show onion screen
    if (hasProxyConfigured && networkConfig?.hostMode != null && networkConfig.requiredHostMode != null) {
      state = MigrationToState.Onion(link.trim(), networkConfig.legacySocksProxy, networkConfig.networkProxy, networkConfig.hostMode, networkConfig.requiredHostMode)
      MigrationToDeviceState.save(MigrationToDeviceState.Onion(link.trim(), networkConfig.legacySocksProxy, networkConfig.networkProxy, networkConfig.hostMode, networkConfig.requiredHostMode))
      if (networkConfig.requiresProxyCredentialEntry()) {
        AlertManager.shared.showAlertMsg(
          generalGetString(MR.strings.migrate_to_device_proxy_credentials_required_title),
          generalGetString(MR.strings.migrate_to_device_proxy_credentials_required_text),
        )
      }
    } else {
      val current = getNetCfg()
      state = MigrationToState.DatabaseInit(link.trim(), current.copy(
        socksProxy = null,
        hostMode = networkConfig?.hostMode ?: current.hostMode,
        requiredHostMode = networkConfig?.requiredHostMode ?: current.requiredHostMode
      ),
        networkProxy = null
      )
    }
    true
  } else {
    AlertManager.shared.showAlertMsg(
      title = generalGetString(MR.strings.invalid_file_link),
      text = generalGetString(MR.strings.the_text_you_pasted_is_not_a_link)
    )
    false
  }
}

private fun MutableState<MigrationToState?>.prepareDatabase(
  link: String,
  tempDatabaseFile: File,
  netCfg: NetCfg,
  networkProxy: NetworkProxy?
) {
  withLongRunningApi {
    val ctrlAndUser = initTemporaryDatabase(tempDatabaseFile, netCfg)
    if (ctrlAndUser == null) {
      // Probably, something wrong with network config or database initialization, let's start from scratch
      state = MigrationToState.PasteOrScanLink
      MigrationToDeviceState.save(null)
      return@withLongRunningApi
    }

    val (ctrl, user) = ctrlAndUser
    state = MigrationToState.LinkDownloading(link, ctrl, user, archivePath(), netCfg, networkProxy)
  }
}

private fun MutableState<MigrationToState?>.startDownloading(
  totalBytes: Long,
  ctrl: ChatCtrl,
  user: User,
  tempDatabaseFile: File,
  chatReceiver: MutableState<MigrationToChatReceiver?>,
  link: String,
  archivePath: String,
  netCfg: NetCfg,
  networkProxy: NetworkProxy?
) {
  withBGApi {
    chatReceiver.value = MigrationToChatReceiver(ctrl, tempDatabaseFile) { msg ->
        val r = msg.result
        when {
          r is CR.RcvFileProgressXFTP -> {
            state = MigrationToState.DownloadProgress(r.receivedSize, r.totalSize, r.rcvFileTransfer.fileId, link, archivePath, netCfg, networkProxy, ctrl)
            MigrationToDeviceState.save(MigrationToDeviceState.DownloadProgress(link, File(archivePath).name, netCfg, networkProxy))
          }
          r is CR.RcvStandaloneFileComplete -> {
            delay(500)
            // User closed the whole screen before new state was saved
            if (state == null) {
              MigrationToDeviceState.save(null)
            } else {
              state = MigrationToState.ArchiveImport(archivePath, netCfg, networkProxy)
              MigrationToDeviceState.save(MigrationToDeviceState.ArchiveImport(File(archivePath).name, netCfg, networkProxy))
            }
          }
          r is CR.RcvFileError -> {
            AlertManager.shared.showAlertMsg(
              generalGetString(MR.strings.migrate_to_device_download_failed),
              generalGetString(MR.strings.migrate_to_device_file_delete_or_link_invalid)
            )
            state = MigrationToState.DownloadFailed(totalBytes, link, archivePath, netCfg, networkProxy)
          }
          msg is API.Error -> {
            if (msg.err is ChatError.ChatErrorChat && msg.err.errorType is ChatErrorType.NoRcvFileUser) {
              AlertManager.shared.showAlertMsg(
                generalGetString(MR.strings.migrate_to_device_download_failed),
                generalGetString(MR.strings.migrate_to_device_file_delete_or_link_invalid)
              )
              state = MigrationToState.DownloadFailed(totalBytes, link, archivePath, netCfg, networkProxy)
            } else {
              Log.d(TAG, "unsupported error: ${msg.responseType}, ${json.encodeToString(msg.err)}")
            }
          }
          else -> Log.d(TAG, "unsupported event: ${msg.responseType}")
        }
    }
    chatReceiver.value?.start()

    val (res, error) = controller.downloadStandaloneFile(user, link, CryptoFile.plain(File(archivePath).path), ctrl)
    if (res == null) {
      state = MigrationToState.DownloadFailed(totalBytes, link, archivePath, netCfg, networkProxy)
      AlertManager.shared.showAlertMsg(
        generalGetString(MR.strings.migrate_to_device_error_downloading_archive),
        error
      )
    }
  }
}

private fun MutableState<MigrationToState?>.importArchive(archivePath: String, netCfg: NetCfg, networkProxy: NetworkProxy?) {
  withLongRunningApi {
    try {
      if (!ChatController.hasChatCtrl()) {
        chatInitControllerRemovingDatabases()
      }
      controller.apiDeleteStorage()
      wallpapersDir.mkdirs()
      try {
        val config = ArchiveConfig(archivePath, parentTempDirectory = databaseExportDir.toString())
        val archiveErrors = controller.apiImportArchive(config)
        if (archiveErrors.isNotEmpty()) {
          showArchiveImportedWithErrorsAlert(archiveErrors)
        }
        state = MigrationToState.Passphrase("", netCfg, networkProxy)
        MigrationToDeviceState.save(MigrationToDeviceState.Passphrase(netCfg, networkProxy))
      } catch (e: Exception) {
        state = MigrationToState.ArchiveImportFailed(archivePath, netCfg, networkProxy)
        AlertManager.shared.showAlertMsg (generalGetString(MR.strings.error_importing_database), e.stackTraceToString())
      }
    } catch (e: Exception) {
      state = MigrationToState.ArchiveImportFailed(archivePath, netCfg, networkProxy)
      AlertManager.shared.showAlertMsg (generalGetString(MR.strings.error_deleting_database), e.stackTraceToString())
    }
  }
}

private suspend fun stopArchiveDownloading(fileId: Long, ctrl: ChatCtrl) {
  controller.apiCancelFile(null, fileId, ctrl)
}

private fun startChat(passphrase: String, confirmation: MigrationConfirmation, useKeychain: Boolean, netCfg: NetCfg, networkProxy: NetworkProxy?, close: () -> Unit) {
  if (useKeychain) {
    ksDatabasePassword.set(passphrase)
  } else {
    ksDatabasePassword.remove()
  }
  appPreferences.storeDBPassphrase.set(useKeychain)
  appPreferences.initialRandomDBPassphrase.set(false)
  withBGApi {
    try {
      initChatController(useKey = passphrase, confirmMigrations = confirmation) { CompletableDeferred(false) }
      val appSettings = controller.apiGetAppSettings(AppSettings.current.prepareForExport()).copy(
        networkConfig = netCfg,
        networkProxy = networkProxy
      )
      finishMigration(appSettings, close)
    } catch (e: Exception) {
      hideView(close)
      AlertManager.shared.showAlertMsg(generalGetString(MR.strings.error_starting_chat), e.stackTraceToString())
    }
  }
}

private suspend fun finishMigration(appSettings: AppSettings, close: () -> Unit) {
  try {
    getMigrationTempFilesDirectory().deleteRecursively()
    appSettings.importIntoApp()
    val onStarted: suspend () -> Unit = {
      platform.androidChatStartedAfterBeingOff()
      hideView(close)
      AlertManager.shared.showAlertMsg(generalGetString(MR.strings.migrate_to_device_chat_migrated), generalGetString(MR.strings.migrate_to_device_finalize_migration))
      MigrationToDeviceState.save(null)
    }
    val user = chatModel.currentUser.value
    if (user != null) {
      startChat(user, onStarted)
    } else {
      onStarted()
    }
  } catch (e: Exception) {
    AlertManager.shared.showAlertMsg(generalGetString(MR.strings.error_starting_chat), e.stackTraceToString())
    MigrationToDeviceState.save(null)
  }
}

private fun hideView(close: () -> Unit) {
  appPreferences.onboardingStage.set(OnboardingStage.OnboardingComplete)
  chatModel.migrationState.value = null
  close()
}

private suspend fun MutableState<MigrationToState?>.cleanUpOnBack(chatReceiver: MigrationToChatReceiver?) {
  val state = state
  if (state is MigrationToState.ArchiveImportFailed) {
    // Original database is not exist, nothing is set up correctly for showing to a user yet. Return to clean state
    deleteChatDatabaseFilesAndState()
    initChatControllerOnStart()
  } else if (state is MigrationToState.DownloadProgress && state.ctrl != null) {
    stopArchiveDownloading(state.fileId, state.ctrl)
  }
  chatReceiver?.stopAndCleanUp()
  getMigrationTempFilesDirectory().deleteRecursively()
  MigrationToDeviceState.save(null)
  chatModel.migrationState.value = null
}

private fun strHasSimplexFileLink(text: String): Boolean =
  isRecognizedPublicFileLink(text)

private fun fileForTemporaryDatabase(): File =
  File(getMigrationTempFilesDirectory(), generateNewFileName("migration", "db", getMigrationTempFilesDirectory()))

private fun archivePath(): String {
  val archiveTime = Clock.System.now()
  val ts = SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.US).format(Date.from(archiveTime.toJavaInstant()))
  val archiveName = "simplex-chat.$ts.zip"
  val archivePath = File(getMigrationTempFilesDirectory(), archiveName)
  return archivePath.absolutePath
}

private class MigrationToChatReceiver(
  val ctrl: ChatCtrl,
  val databaseUrl: File,
  var receiveMessages: Boolean = true,
  val processReceivedMsg: suspend (API) -> Unit
) {
  fun start() {
    Log.d(TAG, "MigrationChatReceiver startReceiver")
    CoroutineScope(Dispatchers.IO).launch {
      while (receiveMessages) {
        try {
          val msg = ChatController.recvMsg(ctrl)
          if (msg != null && receiveMessages) {
            val rhId = msg.rhId
            Log.d(TAG, "processReceivedMsg: ${msg.responseType}")
            chatModel.addTerminalItem(TerminalItem.resp(rhId, msg))
            val finishedWithoutTimeout = withTimeoutOrNull(60_000L) {
              processReceivedMsg(msg)
            }
            if (finishedWithoutTimeout == null) {
              Log.e(TAG, "Timeout reached while processing received message: " + msg.responseType)
              if (appPreferences.developerTools.get() && appPreferences.showSlowApiCalls.get()) {
                AlertManager.shared.showAlertMsg(
                  title = generalGetString(MR.strings.possible_slow_function_title),
                  text = generalGetString(MR.strings.possible_slow_function_desc).format(60, msg.responseType + "\n" + Exception().stackTraceToString()),
                  shareText = true
                )
              }
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "MigrationChatReceiver recvMsg/processReceivedMsg exception: " + e.stackTraceToString())
        } catch (e: Exception) {
          Log.e(TAG, "MigrationChatReceiver recvMsg/processReceivedMsg throwable: " + e.stackTraceToString())
          AlertManager.shared.showAlertMsg(generalGetString(MR.strings.error), e.stackTraceToString())
        }
      }
    }
  }

  fun stopAndCleanUp() {
    Log.d(TAG, "MigrationChatReceiver.stop")
    receiveMessages = false
    chatCloseStore(ctrl)
    File(databaseUrl.absolutePath + "_chat.db").delete()
    File(databaseUrl.absolutePath + "_agent.db").delete()
  }
}
