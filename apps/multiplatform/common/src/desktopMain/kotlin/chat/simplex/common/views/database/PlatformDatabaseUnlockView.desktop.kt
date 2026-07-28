package chat.simplex.common.views.database

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay

private const val NomeUnlockBrandPanelMinimumWidthDp = 900f

internal fun shouldShowNomeUnlockBrandPanel(widthDp: Float): Boolean =
  widthDp >= NomeUnlockBrandPanelMinimumWidthDp

internal fun shouldSubmitNomeUnlock(key: Key, type: KeyEventType): Boolean =
  type == KeyEventType.KeyDown && (key == Key.Enter || key == Key.NumPadEnter)

@Composable
actual fun PlatformDatabaseUnlockView(
  dbKey: MutableState<String>,
  buttonEnabled: Boolean,
  progress: Boolean,
  storedKeyRejected: Boolean,
  backupAvailable: Boolean,
  onOpen: () -> Unit,
  onRestore: () -> Unit,
  legacyContent: @Composable () -> Unit,
) {
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(NomeDesktopUnlockColors.Background),
  ) {
    val showBrandPanel = shouldShowNomeUnlockBrandPanel(maxWidth.value)
    if (showBrandPanel) {
      Row(Modifier.fillMaxSize()) {
        NomeUnlockBrandPanel(
          modifier = Modifier
            .weight(0.38f)
            .fillMaxHeight(),
        )
        NomeUnlockFormPanel(
          dbKey = dbKey,
          buttonEnabled = buttonEnabled,
          progress = progress,
          storedKeyRejected = storedKeyRejected,
          backupAvailable = backupAvailable,
          onOpen = onOpen,
          onRestore = onRestore,
          showCompactLogo = false,
          modifier = Modifier
            .weight(0.62f)
            .fillMaxHeight(),
        )
      }
    } else {
      NomeUnlockFormPanel(
        dbKey = dbKey,
        buttonEnabled = buttonEnabled,
        progress = progress,
        storedKeyRejected = storedKeyRejected,
        backupAvailable = backupAvailable,
        onOpen = onOpen,
        onRestore = onRestore,
        showCompactLogo = true,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private fun NomeUnlockBrandPanel(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.background(NomeDesktopUnlockColors.Brand),
  ) {
    Image(
      painter = painterResource(MR.images.wallpaper_nome_v2),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
      alpha = 0.09f,
    )
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 52.dp, vertical = 46.dp),
    ) {
      NomeUnlockLogo(light = true)
      Spacer(Modifier.weight(0.38f))
      Text(
        text = stringResource(MR.strings.nome_desktop_unlock_brand_title),
        color = Color.White,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.55).sp,
        modifier = Modifier
          .widthIn(max = 410.dp)
          .semantics { heading() },
      )
      Text(
        text = stringResource(MR.strings.nome_desktop_unlock_brand_body),
        color = NomeDesktopUnlockColors.BrandMuted,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        modifier = Modifier
          .widthIn(max = 330.dp)
          .padding(top = 14.dp),
      )
      Spacer(Modifier.weight(0.62f))
    }
  }
}

@Composable
private fun NomeUnlockFormPanel(
  dbKey: MutableState<String>,
  buttonEnabled: Boolean,
  progress: Boolean,
  storedKeyRejected: Boolean,
  backupAvailable: Boolean,
  onOpen: () -> Unit,
  onRestore: () -> Unit,
  showCompactLogo: Boolean,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  var passwordVisible by remember { mutableStateOf(false) }
  val fieldDescription = stringResource(MR.strings.database_passphrase)
  val openDescription = stringResource(MR.strings.nome_desktop_unlock_open)
  val visibilityDescription = stringResource(
    if (passwordVisible) {
      MR.strings.nome_desktop_unlock_hide_password
    } else {
      MR.strings.nome_desktop_unlock_show_password
    }
  )

  Box(
    modifier = modifier
      .background(NomeDesktopUnlockColors.Background)
      .padding(horizontal = if (showCompactLogo) 32.dp else 56.dp, vertical = 36.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 480.dp)
        .fillMaxWidth()
        .offset(y = if (showCompactLogo) 0.dp else (-22).dp),
    ) {
      if (showCompactLogo) {
        NomeUnlockLogo(light = false)
        Spacer(Modifier.height(42.dp))
      }
      Text(
        text = stringResource(MR.strings.nome_desktop_unlock_eyebrow),
        color = NomeDesktopUnlockColors.Action,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.05.sp,
      )
      Text(
        text = stringResource(MR.strings.nome_desktop_unlock_title),
        color = NomeDesktopUnlockColors.Text,
        fontSize = 36.sp,
        lineHeight = 43.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.65).sp,
        modifier = Modifier
          .padding(top = 10.dp)
          .semantics { heading() },
      )
      Text(
        text = stringResource(
          if (storedKeyRejected) {
            MR.strings.nome_desktop_unlock_retry_body
          } else {
            MR.strings.nome_desktop_unlock_body
          }
        ),
        color = NomeDesktopUnlockColors.TextMuted,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        modifier = Modifier.padding(top = 10.dp),
      )
      Spacer(Modifier.height(30.dp))
      Text(
        text = stringResource(MR.strings.database_passphrase),
        color = NomeDesktopUnlockColors.Text,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = dbKey.value,
        onValueChange = { dbKey.value = it },
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .focusRequester(focusRequester)
          .onPreviewKeyEvent { event ->
            if (
              shouldSubmitNomeUnlock(event.key, event.type) &&
              buttonEnabled &&
              !progress
            ) {
              onOpen()
              true
            } else {
              false
            }
          }
          .semantics { contentDescription = fieldDescription },
        enabled = !progress,
        singleLine = true,
        placeholder = {
          Text(
            text = stringResource(MR.strings.nome_desktop_unlock_password_placeholder),
            color = NomeDesktopUnlockColors.TextSubtle,
            fontSize = 14.sp,
          )
        },
        leadingIcon = {
          Icon(
            painter = painterResource(MR.images.ic_lock),
            contentDescription = null,
            tint = NomeDesktopUnlockColors.TextMuted,
            modifier = Modifier.size(19.dp),
          )
        },
        trailingIcon = {
          IconButton(
            onClick = { passwordVisible = !passwordVisible },
            enabled = !progress,
            modifier = Modifier.semantics {
              contentDescription = visibilityDescription
            },
          ) {
            Icon(
              painter = painterResource(
                if (passwordVisible) {
                  MR.images.ic_visibility_off_filled
                } else {
                  MR.images.ic_visibility_filled
                }
              ),
              contentDescription = visibilityDescription,
              tint = NomeDesktopUnlockColors.TextMuted,
              modifier = Modifier.size(19.dp),
            )
          }
        },
        visualTransformation =
          if (passwordVisible) {
            VisualTransformation.None
          } else {
            PasswordVisualTransformation()
          },
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
          textColor = NomeDesktopUnlockColors.Text,
          cursorColor = NomeDesktopUnlockColors.Action,
          focusedBorderColor = NomeDesktopUnlockColors.Action,
          unfocusedBorderColor = NomeDesktopUnlockColors.Border,
          disabledBorderColor = NomeDesktopUnlockColors.Border,
          backgroundColor = NomeDesktopUnlockColors.Surface,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
          onDone = {
            if (buttonEnabled && !progress) onOpen()
          }
        ),
      )
      Spacer(Modifier.height(10.dp))
      Button(
        onClick = onOpen,
        enabled = buttonEnabled && !progress,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .semantics {
            contentDescription = openDescription
          },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = NomeDesktopUnlockColors.Action,
          contentColor = Color.White,
          disabledBackgroundColor = NomeDesktopUnlockColors.Disabled,
          disabledContentColor = NomeDesktopUnlockColors.DisabledText,
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
      ) {
        if (progress) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = Color.White,
            strokeWidth = 2.dp,
          )
          Spacer(Modifier.width(9.dp))
        }
        Text(
          text = stringResource(MR.strings.nome_desktop_unlock_open),
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Row(
        modifier = Modifier.padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          painter = painterResource(MR.images.ic_security),
          contentDescription = null,
          tint = NomeDesktopUnlockColors.Action,
          modifier = Modifier.size(17.dp),
        )
        Text(
          text = stringResource(MR.strings.nome_desktop_unlock_security),
          color = NomeDesktopUnlockColors.TextMuted,
          fontSize = 12.sp,
          modifier = Modifier.padding(start = 7.dp),
        )
      }
      Row(
        modifier = Modifier.padding(top = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          painter = painterResource(MR.images.ic_keyboard),
          contentDescription = null,
          tint = NomeDesktopUnlockColors.TextSubtle,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = stringResource(MR.strings.nome_desktop_unlock_return),
          color = NomeDesktopUnlockColors.TextSubtle,
          fontSize = 12.sp,
          modifier = Modifier.padding(start = 6.dp),
        )
      }
      if (backupAvailable) {
        TextButton(
          onClick = onRestore,
          enabled = !progress,
          modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp),
        ) {
          Text(
            text = stringResource(MR.strings.restore_database),
            color = NomeDesktopUnlockColors.Action,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }

  LaunchedEffect(Unit) {
    delay(160)
    focusRequester.requestFocus()
  }
}

@Composable
private fun NomeUnlockLogo(light: Boolean) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Image(
      painter = painterResource(MR.images.nome_app_icon),
      contentDescription = "Nome",
      modifier = Modifier
        .size(58.dp)
        .clip(RoundedCornerShape(14.dp)),
    )
    Text(
      text = "Nome",
      color = if (light) Color.White else NomeDesktopUnlockColors.Text,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(start = 13.dp),
    )
  }
}

private object NomeDesktopUnlockColors {
  val Action = Color(0xFF0A874D)
  val Background = Color(0xFFF7F8F6)
  val Surface = Color(0xFFFFFFFF)
  val Text = Color(0xFF0E1B2D)
  val TextMuted = Color(0xFF667085)
  val TextSubtle = Color(0xFF98A2B3)
  val Border = Color(0xFFD7DEE3)
  val Disabled = Color(0xFFE0E7E3)
  val DisabledText = Color(0xFF6E8179)
  val Brand = Color(0xFF10253F)
  val BrandMuted = Color(0xFFB9C7D5)
}
