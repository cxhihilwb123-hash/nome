package chat.simplex.app.nome.connection

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
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewContent
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewPhase
import chat.simplex.common.ui.nome.connection.NomeConnectionPreviewState
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.views.newchat.ConnectionPreviewFailureKind
import chat.simplex.common.views.newchat.ConnectionPreviewIdentity
import chat.simplex.common.views.newchat.ConnectionPreviewKind
import chat.simplex.common.views.newchat.ConnectionPreviewOwnerStatus
import chat.simplex.common.views.newchat.ConnectionPreviewUiModel
import chat.simplex.common.views.newchat.ConnectionPreviewWarning
import java.util.Locale

@SuppressLint("AppBundleLocaleChanges")
class NomeConnectionPreviewEvidenceActivity : ComponentActivity() {
  private var previousDefaultLocale: Locale? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val spec = NomeConnectionPreviewEvidenceSpec.from(intent)
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
                R.string.nome_connection_preview_evidence_badge,
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
              val fixture = spec.state.fixture(
                stringResource(
                  R.string.nome_connection_preview_evidence_profile,
                ),
                stringResource(
                  R.string.nome_connection_preview_evidence_owner_failure_reason,
                ),
              )
              NomeConnectionPreviewContent(
                model = fixture.model,
                state = fixture.state,
                onIdentitySelected = {},
                onPrimary = {},
                onCancel = {},
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
    fun intent(
      context: Context,
      spec: NomeConnectionPreviewEvidenceSpec,
    ): Intent =
      Intent(context, NomeConnectionPreviewEvidenceActivity::class.java).apply {
        putExtra(NomeConnectionPreviewEvidenceSpec.EXTRA_STATE, spec.state.intentValue)
        putExtra(NomeConnectionPreviewEvidenceSpec.EXTRA_LOCALE, spec.languageTag)
        putExtra(NomeConnectionPreviewEvidenceSpec.EXTRA_DARK, spec.dark)
        putExtra(
          NomeConnectionPreviewEvidenceSpec.EXTRA_FONT_SCALE,
          spec.fontScale,
        )
      }
  }
}

data class NomeConnectionPreviewEvidenceSpec(
  val state: NomeConnectionPreviewEvidenceState,
  val languageTag: String,
  val dark: Boolean,
  val fontScale: Float,
) {
  companion object {
    const val EXTRA_STATE = "nomeConnectionPreviewState"
    const val EXTRA_LOCALE = "nomeConnectionPreviewLocale"
    const val EXTRA_DARK = "nomeConnectionPreviewDark"
    const val EXTRA_FONT_SCALE = "nomeConnectionPreviewFontScale"

    fun from(intent: Intent): NomeConnectionPreviewEvidenceSpec =
      NomeConnectionPreviewEvidenceSpec(
        state = NomeConnectionPreviewEvidenceState.from(
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

enum class NomeConnectionPreviewEvidenceState(val intentValue: String) {
  READY_CURRENT("ready-current"),
  READY_INCOGNITO("ready-incognito"),
  OWN_LINK_WARNING("own-link-warning"),
  REPEAT_JOIN_WARNING("repeat-join-warning"),
  OWNER_VERIFIED("owner-verified"),
  OWNER_FAILED("owner-failed"),
  CONNECTING("connecting"),
  PENDING("pending"),
  FAILURE("failure");

  fun fixture(
    profileName: String,
    ownerFailureReason: String,
  ): NomeConnectionPreviewEvidenceFixture {
    val identity =
      if (this == READY_INCOGNITO) {
        ConnectionPreviewIdentity.Incognito
      } else {
        ConnectionPreviewIdentity.CurrentProfile
      }
    val model = ConnectionPreviewUiModel(
      kind = when (this) {
        READY_INCOGNITO,
        OWN_LINK_WARNING,
        FAILURE -> ConnectionPreviewKind.ContactAddress
        REPEAT_JOIN_WARNING,
        PENDING -> ConnectionPreviewKind.Group
        else -> ConnectionPreviewKind.Invitation
      },
      warning = when (this) {
        OWN_LINK_WARNING -> ConnectionPreviewWarning.OwnLink
        REPEAT_JOIN_WARNING -> ConnectionPreviewWarning.RepeatJoin
        else -> ConnectionPreviewWarning.None
      },
      ownerStatus = when (this) {
        OWNER_VERIFIED -> ConnectionPreviewOwnerStatus.Verified
        OWNER_FAILED ->
          ConnectionPreviewOwnerStatus.Failed(ownerFailureReason)
        else -> ConnectionPreviewOwnerStatus.Absent
      },
      currentProfileName = profileName,
      currentProfileImage = null,
      initialIdentity = identity,
    )
    val state = NomeConnectionPreviewState(
      identity = identity,
      phase = when (this) {
        CONNECTING -> NomeConnectionPreviewPhase.Connecting
        PENDING -> NomeConnectionPreviewPhase.Pending
        FAILURE -> NomeConnectionPreviewPhase.Failure
        else -> NomeConnectionPreviewPhase.Ready
      },
      failureKind =
        if (this == FAILURE) {
          ConnectionPreviewFailureKind.Network
        } else {
          null
        },
    )
    return NomeConnectionPreviewEvidenceFixture(model, state)
  }

  companion object {
    fun from(raw: String?): NomeConnectionPreviewEvidenceState =
      entries.firstOrNull { it.intentValue == raw } ?: READY_CURRENT
  }
}

data class NomeConnectionPreviewEvidenceFixture(
  val model: ConnectionPreviewUiModel,
  val state: NomeConnectionPreviewState,
)
