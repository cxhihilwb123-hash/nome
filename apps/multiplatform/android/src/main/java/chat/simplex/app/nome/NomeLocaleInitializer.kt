package chat.simplex.app.nome

import android.content.Context
import chat.simplex.common.model.ChatController
import chat.simplex.common.platform.Log
import java.io.File

internal const val NOME_LOCALE_POLICY_VERSION = 1
internal const val NOME_DEFAULT_LOCALE = "zh-CN"

internal data class NomeLocaleEvidence(
  val markerVersion: Int,
  val explicitLanguage: String?,
  val sameInstallAndUpdateTime: Boolean,
  val databasePresentBeforeInitialization: Boolean,
  val upstreamPreferencesPresent: Boolean,
)

internal enum class NomeLocaleDecision {
  ALREADY_INITIALIZED,
  PRESERVE_EXPLICIT,
  INITIALIZE_SIMPLIFIED_CHINESE,
  PRESERVE_EXISTING_OR_UNKNOWN,
}

internal fun decideNomeLocale(evidence: NomeLocaleEvidence): NomeLocaleDecision =
  when {
    evidence.markerVersion >= NOME_LOCALE_POLICY_VERSION ->
      NomeLocaleDecision.ALREADY_INITIALIZED
    evidence.explicitLanguage != null ->
      NomeLocaleDecision.PRESERVE_EXPLICIT
    evidence.sameInstallAndUpdateTime &&
      !evidence.databasePresentBeforeInitialization &&
      !evidence.upstreamPreferencesPresent ->
      NomeLocaleDecision.INITIALIZE_SIMPLIFIED_CHINESE
    else ->
      NomeLocaleDecision.PRESERVE_EXISTING_OR_UNKNOWN
  }

object NomeLocaleInitializer {
  private const val NOME_PREFS = "nome_product"
  private const val LOCALE_MARKER = "locale_policy_version"
  private const val UPSTREAM_PREFS = "chat.simplex.app.SIMPLEX_APP_PREFS"
  private const val CHAT_DATABASE = "files_chat.db"
  private const val AGENT_DATABASE = "files_agent.db"
  private const val LOG_TAG = "NOME_LOCALE"

  fun initialize(context: Context) {
    val markerPreferences = context.getSharedPreferences(NOME_PREFS, Context.MODE_PRIVATE)
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val dataDirectory = context.dataDir
    val evidence = NomeLocaleEvidence(
      markerVersion = markerPreferences.getInt(LOCALE_MARKER, 0),
      explicitLanguage = ChatController.appPrefs.appLanguage.get(),
      sameInstallAndUpdateTime = packageInfo.firstInstallTime == packageInfo.lastUpdateTime,
      databasePresentBeforeInitialization =
        File(dataDirectory, CHAT_DATABASE).exists() ||
          File(dataDirectory, AGENT_DATABASE).exists(),
      upstreamPreferencesPresent =
        context.getSharedPreferences(UPSTREAM_PREFS, Context.MODE_PRIVATE).all.isNotEmpty(),
    )
    val decision = decideNomeLocale(evidence)

    if (decision == NomeLocaleDecision.INITIALIZE_SIMPLIFIED_CHINESE) {
      ChatController.appPrefs.appLanguage.set(NOME_DEFAULT_LOCALE)
    }
    if (decision != NomeLocaleDecision.ALREADY_INITIALIZED) {
      val stored = markerPreferences.edit()
        .putInt(LOCALE_MARKER, NOME_LOCALE_POLICY_VERSION)
        .commit()
      if (!stored) {
        Log.e(LOG_TAG, "locale policy marker write failed")
      }
    }
    Log.d(LOG_TAG, "locale policy decision: ${decision.name}")
  }
}
