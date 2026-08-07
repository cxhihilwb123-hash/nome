package chat.simplex.app.nome.database

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import chat.simplex.app.R
import chat.simplex.common.ui.nome.database.NomeDatabaseRootActions
import chat.simplex.common.ui.nome.database.NomeDatabaseRootRoute
import chat.simplex.common.ui.nome.database.NomeDatabaseRootState
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import java.util.Locale

@SuppressLint("AppBundleLocaleChanges")
class NomeDatabaseRootEvidenceActivity : ComponentActivity() {
  private var previousDefaultLocale: Locale? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val spec = NomeDatabaseRootEvidenceSpec.from(intent)
    configureWindow(spec.dark)
    val locale = Locale.forLanguageTag(spec.languageTag)
    previousDefaultLocale = Locale.getDefault()
    Locale.setDefault(locale)
    val localizedConfiguration = Configuration(resources.configuration).apply {
      setLocale(locale)
      setLayoutDirection(locale)
      fontScale = spec.fontScale
    }
    val localizedContext = createConfigurationContext(localizedConfiguration)
    val displayDensity = localizedContext.resources.displayMetrics.density

    setContent {
      CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration,
        LocalDensity provides Density(displayDensity, spec.fontScale),
      ) {
        NomeAndroidTheme(darkTheme = spec.dark) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .background(NomeTheme.colors.background)
              .statusBarsPadding(),
          ) {
            Text(
              text = stringResource(
                R.string.nome_database_root_evidence_badge,
                spec.state.intentValue,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .background(NomeTheme.colors.surfaceContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
              style = NomeTheme.typography.supporting,
              color = NomeTheme.colors.textSecondary,
              textAlign = TextAlign.Center,
            )
            Box(modifier = Modifier.weight(1f)) {
              NomeDatabaseRootRoute(
                state = spec.state.fixture(),
                passphrase = remember { mutableStateOf("") },
                actions = noOpActions,
              )
            }
          }
        }
      }
    }
  }

  override fun onDestroy() {
    previousDefaultLocale?.let(Locale::setDefault)
    previousDefaultLocale = null
    super.onDestroy()
  }

  private fun configureWindow(dark: Boolean) {
    val style =
      if (dark) {
        SystemBarStyle.dark(Color.TRANSPARENT)
      } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
      }
    enableEdgeToEdge(
      statusBarStyle = style,
      navigationBarStyle = style,
    )
    WindowCompat.getInsetsController(window, window.decorView).apply {
      isAppearanceLightStatusBars = !dark
      isAppearanceLightNavigationBars = !dark
    }
  }

  companion object {
    private val noOpActions = NomeDatabaseRootActions(
      openOnce = {},
      saveAndOpen = {},
      confirmUpgrade = {},
      confirmDowngrade = {},
      copyMatchedBackup = {},
      openRestored = {},
    )

    fun intent(
      context: Context,
      spec: NomeDatabaseRootEvidenceSpec,
    ): Intent =
      Intent(context, NomeDatabaseRootEvidenceActivity::class.java).apply {
        putExtra(NomeDatabaseRootEvidenceSpec.EXTRA_STATE, spec.state.intentValue)
        putExtra(NomeDatabaseRootEvidenceSpec.EXTRA_LOCALE, spec.languageTag)
        putExtra(NomeDatabaseRootEvidenceSpec.EXTRA_DARK, spec.dark)
        putExtra(NomeDatabaseRootEvidenceSpec.EXTRA_FONT_SCALE, spec.fontScale)
      }
  }
}

data class NomeDatabaseRootEvidenceSpec(
  val state: NomeDatabaseRootEvidenceState,
  val languageTag: String,
  val dark: Boolean,
  val fontScale: Float,
) {
  companion object {
    const val EXTRA_STATE = "nomeDatabaseRootState"
    const val EXTRA_LOCALE = "nomeDatabaseRootLocale"
    const val EXTRA_DARK = "nomeDatabaseRootDark"
    const val EXTRA_FONT_SCALE = "nomeDatabaseRootFontScale"

    fun from(intent: Intent): NomeDatabaseRootEvidenceSpec =
      NomeDatabaseRootEvidenceSpec(
        state = NomeDatabaseRootEvidenceState.from(
          intent.getStringExtra(EXTRA_STATE),
        ),
        languageTag = intent.getStringExtra(EXTRA_LOCALE) ?: "en",
        dark = intent.getBooleanExtra(EXTRA_DARK, false),
        fontScale = intent.getFloatExtra(EXTRA_FONT_SCALE, 1f)
          .takeIf { it == 2f }
          ?: 1f,
      )
  }
}

enum class NomeDatabaseRootEvidenceState(val intentValue: String) {
  OPENING("opening"),
  MIGRATING("migrating"),
  ALTERNATE_KEY("alternate-key"),
  STORED_MANUAL_KEY("stored-manual-key"),
  STORED_RANDOM_KEY("stored-random-key"),
  UPGRADE("upgrade"),
  DOWNGRADE("downgrade"),
  INCOMPATIBLE("incompatible"),
  OPEN_FAILED("open-failed"),
  KEYSTORE_UNAVAILABLE("keystore-unavailable"),
  INVALID_CONFIRMATION("invalid-confirmation"),
  UNKNOWN_FAILURE("unknown-failure"),
  BACKUP_PAIR_COPIED("backup-pair-copied"),
  BACKUP_PAIR_COPY_FAILED("backup-pair-copy-failed");

  fun fixture(): NomeDatabaseRootState = when (this) {
    OPENING ->
      NomeDatabaseRootState.Opening(submitting = false)

    MIGRATING ->
      NomeDatabaseRootState.Migrating(submitting = true)

    ALTERNATE_KEY ->
      NomeDatabaseRootState.AlternateKeyRequired(
        submitting = false,
        matchedBackupAvailable = true,
        storedKeyMaterialPresent = false,
        storedKeyUseRequested = false,
      )

    STORED_MANUAL_KEY ->
      NomeDatabaseRootState.StoredManualKeyUnreadable(
        submitting = false,
        matchedBackupAvailable = false,
      )

    STORED_RANDOM_KEY ->
      NomeDatabaseRootState.StoredRandomKeyUnavailable(
        submitting = false,
        matchedBackupAvailable = true,
      )

    UPGRADE ->
      NomeDatabaseRootState.UpgradeConsent(
        submitting = false,
        matchedBackupAvailable = false,
      )

    DOWNGRADE ->
      NomeDatabaseRootState.DowngradeConsent(
        submitting = false,
        matchedBackupAvailable = true,
        warningCount = 2,
      )

    INCOMPATIBLE ->
      NomeDatabaseRootState.IncompatibleVersion(
        submitting = false,
        matchedBackupAvailable = false,
      )

    OPEN_FAILED ->
      NomeDatabaseRootState.DatabaseOpenFailed(
        submitting = false,
        matchedBackupAvailable = true,
      )

    KEYSTORE_UNAVAILABLE ->
      NomeDatabaseRootState.KeyStoreUnavailable(
        submitting = false,
        matchedBackupAvailable = false,
        storedKeyMaterialPresent = true,
      )

    INVALID_CONFIRMATION ->
      NomeDatabaseRootState.InvalidConfirmation(
        submitting = false,
        matchedBackupAvailable = false,
      )

    UNKNOWN_FAILURE ->
      NomeDatabaseRootState.UnknownFailure(
        submitting = false,
        matchedBackupAvailable = false,
      )

    BACKUP_PAIR_COPIED ->
      NomeDatabaseRootState.RestoredPairReadyToOpen(
        submitting = false,
      )

    BACKUP_PAIR_COPY_FAILED ->
      NomeDatabaseRootState.BackupPairCopyFailed(
        submitting = false,
        matchedBackupAvailable = true,
      )
  }

  companion object {
    fun from(raw: String?): NomeDatabaseRootEvidenceState =
      entries.firstOrNull { it.intentValue == raw } ?: OPENING
  }
}
