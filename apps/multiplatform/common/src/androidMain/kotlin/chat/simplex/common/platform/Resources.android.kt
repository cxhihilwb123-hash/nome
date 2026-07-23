package chat.simplex.common.platform

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.text.BidiFormatter
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import chat.simplex.common.R
import chat.simplex.common.model.AppPreferences
import chat.simplex.res.MR
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.desc

@SuppressLint("DiscouragedApi")
@Composable
actual fun font(name: String, res: String, weight: FontWeight, style: FontStyle): Font {
  val context = LocalContext.current
  val id = context.resources.getIdentifier(res, "font", context.packageName)
  return Font(id, weight, style)
}

actual fun StringResource.localized(): String =
  when (this) {
    MR.strings.chat_lock ->
      androidAppContext.getString(R.string.nome_brand_chat_lock)
    MR.strings.auth_simplex_lock_turned_on ->
      androidAppContext.getString(R.string.nome_brand_lock_enabled)
    MR.strings.auth_enable_simplex_lock ->
      androidAppContext.getString(R.string.nome_brand_enable_lock)
    MR.strings.auth_device_authentication_is_not_enabled_you_can_turn_on_in_settings_once_enabled ->
      androidAppContext.getString(
        R.string.nome_brand_lock_unavailable_enable,
      )
    MR.strings.auth_device_authentication_is_disabled_turning_off ->
      androidAppContext.getString(
        R.string.nome_brand_lock_unavailable_disable,
      )
    MR.strings.lock_not_enabled ->
      androidAppContext.getString(
        R.string.nome_brand_lock_not_enabled,
      )
    MR.strings.you_can_turn_on_lock ->
      androidAppContext.getString(
        R.string.nome_brand_lock_enable_hint,
      )
    MR.strings.about_simplex_chat ->
      androidAppContext.getString(R.string.nome_about_title)
    MR.strings.settings_section_title_support ->
      androidAppContext.getString(R.string.nome_brand_support)
    MR.strings.install_simplex_chat_for_terminal ->
      androidAppContext.getString(R.string.nome_brand_terminal_client)
    MR.strings.use_simplex_chat_servers__question ->
      androidAppContext.getString(
        R.string.nome_brand_default_call_servers_question,
      )
    MR.strings.using_simplex_chat_servers ->
      androidAppContext.getString(
        R.string.nome_brand_default_call_servers,
      )
    MR.strings.theme_simplex ->
      androidAppContext.getString(R.string.nome_brand_theme)
    else -> desc().toString(context = androidAppContext)
  }

actual fun isInNightMode() =
  (androidAppContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).nightMode == UiModeManager.MODE_NIGHT_YES

private val sharedPreferences: SharedPreferences by lazy { androidAppContext.getSharedPreferences(AppPreferences.SHARED_PREFS_ID, Context.MODE_PRIVATE) }
private val sharedPreferencesThemes: SharedPreferences by lazy { androidAppContext.getSharedPreferences(AppPreferences.SHARED_PREFS_THEMES_ID, Context.MODE_PRIVATE) }

actual val settings: Settings by lazy { SharedPreferencesSettings(sharedPreferences) }
actual val settingsThemes: Settings by lazy { SharedPreferencesSettings(sharedPreferencesThemes) }

actual fun windowOrientation(): WindowOrientation = when (mainActivity.get()?.resources?.configuration?.orientation) {
  Configuration.ORIENTATION_PORTRAIT -> WindowOrientation.PORTRAIT
  Configuration.ORIENTATION_LANDSCAPE -> WindowOrientation.LANDSCAPE
  else -> WindowOrientation.UNDEFINED
}

@Composable
actual fun windowWidth(): Dp {
  val direction = LocalLayoutDirection.current
  val cutout = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal).asPaddingValues()
  return LocalConfiguration.current.screenWidthDp.dp - cutout.calculateStartPadding(direction) - cutout.calculateEndPadding(direction)
}

@Composable
actual fun windowHeight(): Dp = LocalConfiguration.current.screenHeightDp.dp

actual fun desktopExpandWindowToWidth(width: Dp) {}

actual fun isRtl(text: CharSequence): Boolean = BidiFormatter.getInstance().isRtl(text)

actual fun ImageResource.toComposeImageBitmap(): ImageBitmap? =
  getDrawable(androidAppContext)?.toBitmap()?.asImageBitmap()
