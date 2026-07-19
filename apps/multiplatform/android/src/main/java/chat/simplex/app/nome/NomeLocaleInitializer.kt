package chat.simplex.app.nome

import android.content.Context
import chat.simplex.common.model.ChatController
import chat.simplex.common.platform.Log
import java.io.File

internal const val NOME_LOCALE_POLICY_VERSION = 1
internal const val NOME_DEFAULT_LOCALE = "zh-CN"
private val NOME_V656_UPSTREAM_LOCALES = setOf(
  "en", "ar", "bg", "ca", "cs", "de", "es", "fa", "fi", "fr", "hu", "in", "it", "iw",
  "ja", "lt", "nl", "pl", "pt-BR", "ro", "ru", "th", "tr", "uk", "vi", "zh-CN",
)

internal data class NomeLocalePreInitializationEvidence(
  val markerVersion: Int,
  val sameInstallAndUpdateTime: Boolean,
  val databasePresent: Boolean,
  val upstreamPreferencesPresent: Boolean,
)

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

internal fun isKnownNomeUpstreamLocale(language: String): Boolean =
  language in NOME_V656_UPSTREAM_LOCALES

object NomeLocaleInitializer {
  private const val NOME_PREFS = "nome_product"
  private const val LOCALE_MARKER = "locale_policy_version"
  private const val UPSTREAM_PREFS = "chat.simplex.app.SIMPLEX_APP_PREFS"
  private const val CHAT_DATABASE = "files_chat.db"
  private const val AGENT_DATABASE = "files_agent.db"
  private const val LOG_TAG = "NOME_LOCALE"

  internal fun capturePreInitializationEvidence(context: Context): NomeLocalePreInitializationEvidence {
    val markerPreferences = context.getSharedPreferences(NOME_PREFS, Context.MODE_PRIVATE)
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val dataDirectory = context.dataDir
    return NomeLocalePreInitializationEvidence(
      markerVersion = markerPreferences.getInt(LOCALE_MARKER, 0),
      sameInstallAndUpdateTime = packageInfo.firstInstallTime == packageInfo.lastUpdateTime,
      databasePresent =
        File(dataDirectory, CHAT_DATABASE).exists() ||
          File(dataDirectory, AGENT_DATABASE).exists(),
      upstreamPreferencesPresent =
        context.getSharedPreferences(UPSTREAM_PREFS, Context.MODE_PRIVATE).all.isNotEmpty(),
    )
  }

  internal fun initialize(context: Context, preInitializationEvidence: NomeLocalePreInitializationEvidence) {
    val markerPreferences = context.getSharedPreferences(NOME_PREFS, Context.MODE_PRIVATE)
    val evidence = NomeLocaleEvidence(
      markerVersion = preInitializationEvidence.markerVersion,
      explicitLanguage = ChatController.appPrefs.appLanguage.get(),
      sameInstallAndUpdateTime = preInitializationEvidence.sameInstallAndUpdateTime,
      databasePresentBeforeInitialization = preInitializationEvidence.databasePresent,
      upstreamPreferencesPresent = preInitializationEvidence.upstreamPreferencesPresent,
    )
    val decision = decideNomeLocale(evidence)

    if (decision == NomeLocaleDecision.INITIALIZE_SIMPLIFIED_CHINESE) {
      ChatController.appPrefs.appLanguage.set(NOME_DEFAULT_LOCALE)
    }
    if (evidence.explicitLanguage != null && !isKnownNomeUpstreamLocale(evidence.explicitLanguage)) {
      Log.w(LOG_TAG, "unsupported stored language preserved without overwrite")
    }
    if (decision != NomeLocaleDecision.ALREADY_INITIALIZED) {
      val stored = markerPreferences.edit()
        .putInt(LOCALE_MARKER, NOME_LOCALE_POLICY_VERSION)
        .commit()
      if (!stored) {
        Log.e(LOG_TAG, "locale policy marker write failed")
      }
    }
    if (decision == NomeLocaleDecision.PRESERVE_EXISTING_OR_UNKNOWN) {
      Log.w(LOG_TAG, "locale policy preserved existing or unknown state")
    } else {
      Log.d(LOG_TAG, "locale policy decision: ${decision.name}")
    }
  }
}
