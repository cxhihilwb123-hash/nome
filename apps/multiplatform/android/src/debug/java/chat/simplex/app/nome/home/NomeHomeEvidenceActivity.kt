package chat.simplex.app.nome.home

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
import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.ChatItem
import chat.simplex.common.model.ChatTag
import chat.simplex.common.ui.nome.home.NomeHomeConnectivityState
import chat.simplex.common.ui.nome.home.NomeHomeContentState
import chat.simplex.common.ui.nome.home.NomeHomeCoreState
import chat.simplex.common.ui.nome.home.NomeHomeState
import chat.simplex.common.ui.nome.components.NomePrimaryDestination
import chat.simplex.common.ui.nome.theme.NomeAndroidTheme
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.views.chatlist.NomeHomeRouteContent
import chat.simplex.common.model.ChatModel
import java.util.Locale
import kotlinx.datetime.Instant

class NomeHomeEvidenceActivity : ComponentActivity() {
  private var previousDefaultLocale: Locale? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val spec = NomeHomeEvidenceSpec.from(intent)
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
              .statusBarsPadding()
              .background(NomeTheme.colors.background),
          ) {
            Text(
              text = stringResource(
                R.string.nome_home_evidence_badge,
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
              NomeHomeRouteContent(
                chatModel = ChatModel,
                state = spec.state.renderState(),
                showChatPreviews = true,
                profileNameOverride = stringResource(
                  R.string.nome_home_evidence_profile,
                ),
                selectedDestination = spec.screen.destination,
                userLists = listOf(evidenceList),
                allListsSelected =
                  spec.screen != NomeHomeEvidenceScreen.CUSTOM_LIST,
                selectedUserListId =
                  evidenceList.chatTagId.takeIf {
                    spec.screen == NomeHomeEvidenceScreen.CUSTOM_LIST
                  },
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
    fun intent(context: Context, spec: NomeHomeEvidenceSpec): Intent =
      Intent(context, NomeHomeEvidenceActivity::class.java).apply {
        putExtra(NomeHomeEvidenceSpec.EXTRA_STATE, spec.state.intentValue)
        putExtra(NomeHomeEvidenceSpec.EXTRA_LOCALE, spec.languageTag)
        putExtra(NomeHomeEvidenceSpec.EXTRA_DARK, spec.dark)
        putExtra(NomeHomeEvidenceSpec.EXTRA_FONT_SCALE, spec.fontScale)
        putExtra(NomeHomeEvidenceSpec.EXTRA_SCREEN, spec.screen.intentValue)
      }

    private val evidenceList =
      ChatTag(
        chatTagId = 41L,
        chatTagText = "111",
        chatTagEmoji = null,
      )
  }
}

data class NomeHomeEvidenceSpec(
  val state: NomeHomeEvidenceState,
  val languageTag: String,
  val dark: Boolean,
  val fontScale: Float,
  val screen: NomeHomeEvidenceScreen = NomeHomeEvidenceScreen.HOME,
) {
  companion object {
    const val EXTRA_STATE = "nomeHomeState"
    const val EXTRA_LOCALE = "nomeHomeLocale"
    const val EXTRA_DARK = "nomeHomeDark"
    const val EXTRA_FONT_SCALE = "nomeHomeFontScale"
    const val EXTRA_SCREEN = "nomeHomeScreen"

    fun from(intent: Intent): NomeHomeEvidenceSpec =
      NomeHomeEvidenceSpec(
        state = NomeHomeEvidenceState.from(
          intent.getStringExtra(EXTRA_STATE),
        ),
        languageTag = intent.getStringExtra(EXTRA_LOCALE) ?: "en",
        dark = intent.getBooleanExtra(EXTRA_DARK, false),
        fontScale = intent.getFloatExtra(EXTRA_FONT_SCALE, 1f)
          .takeIf { it == 2f }
          ?: 1f,
        screen = NomeHomeEvidenceScreen.from(
          intent.getStringExtra(EXTRA_SCREEN),
        ),
      )
  }
}

enum class NomeHomeEvidenceScreen(
  val intentValue: String,
  val destination: NomePrimaryDestination,
) {
  HOME("home", NomePrimaryDestination.HOME),
  CONTACTS("contacts", NomePrimaryDestination.CONTACTS),
  CUSTOM_LIST("custom-list", NomePrimaryDestination.HOME);

  companion object {
    fun from(raw: String?): NomeHomeEvidenceScreen =
      entries.firstOrNull { it.intentValue == raw } ?: HOME
  }
}

enum class NomeHomeEvidenceState(val intentValue: String) {
  POPULATED("populated"),
  LOADING("loading"),
  FIRST_USE("first-use"),
  TRUE_EMPTY("true-empty"),
  FILTERED_NO_RESULT("filtered-no-result"),
  NETWORK_UNKNOWN("network-unknown"),
  DEVICE_OFFLINE("device-offline"),
  CORE_STOPPED("core-stopped"),
  UNAVAILABLE("unavailable"),
  UNAVAILABLE_CACHED("unavailable-cached");

  fun renderState(): NomeHomeState<Chat> {
    val chats =
      if (
        this == POPULATED ||
        this == NETWORK_UNKNOWN ||
        this == DEVICE_OFFLINE ||
        this == CORE_STOPPED ||
        this == UNAVAILABLE_CACHED
      ) {
        evidenceChats
      } else {
        emptyList()
      }
    return NomeHomeState(
      content = when (this) {
        POPULATED,
        NETWORK_UNKNOWN,
        DEVICE_OFFLINE,
        CORE_STOPPED -> NomeHomeContentState.POPULATED
        LOADING -> NomeHomeContentState.LOADING
        FIRST_USE -> NomeHomeContentState.FIRST_USE
        TRUE_EMPTY -> NomeHomeContentState.TRUE_EMPTY
        FILTERED_NO_RESULT -> NomeHomeContentState.FILTERED_NO_RESULT
        UNAVAILABLE,
        UNAVAILABLE_CACHED -> NomeHomeContentState.UNAVAILABLE
      },
      connectivity = when (this) {
        NETWORK_UNKNOWN -> NomeHomeConnectivityState.UNKNOWN
        DEVICE_OFFLINE -> NomeHomeConnectivityState.DEVICE_OFFLINE
        else -> NomeHomeConnectivityState.ONLINE
      },
      core =
        if (this == CORE_STOPPED) {
          NomeHomeCoreState.STOPPED
        } else {
          NomeHomeCoreState.RUNNING
        },
      visibleChats = chats,
      hasCachedChats = chats.isNotEmpty(),
    )
  }

  companion object {
    fun from(raw: String?): NomeHomeEvidenceState =
      entries.firstOrNull { it.intentValue == raw } ?: POPULATED

    private val evidenceChatItems = listOf(
      ChatItem.getSampleData(
        ts = Instant.parse("2000-01-01T00:00:00Z"),
        text = "hello\nthere",
      ),
    )

    private val favoriteDirectChat = Chat.sampleData.let { chat ->
      val direct = chat.chatInfo as ChatInfo.Direct
      chat.copy(
        chatInfo = direct.copy(
          contact = direct.contact.copy(
            chatSettings = direct.contact.chatSettings.copy(favorite = true),
          ),
        ),
        chatItems = evidenceChatItems,
        chatStats = Chat.ChatStats(unreadCount = 2),
      )
    }

    private val evidenceChats = listOf(
      favoriteDirectChat,
      Chat(
        remoteHostId = null,
        chatInfo = ChatInfo.Group.sampleData,
        chatItems = evidenceChatItems,
        chatStats = Chat.ChatStats(unreadCount = 1),
      ),
    )
  }
}
