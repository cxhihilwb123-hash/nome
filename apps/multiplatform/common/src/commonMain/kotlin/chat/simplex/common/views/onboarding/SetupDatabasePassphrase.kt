package chat.simplex.common.views.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import dev.icerock.moko.resources.compose.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.model.*
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.database.*
import chat.simplex.common.views.helpers.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import kotlinx.coroutines.delay

@Composable
fun SetupDatabasePassphrase(m: ChatModel) {
  val progressIndicator = remember { mutableStateOf(false) }
  val prefs = m.controller.appPrefs
  val initialRandomDBPassphrase = remember { mutableStateOf(prefs.initialRandomDBPassphrase.get()) }
  // Do not do rememberSaveable on current key to prevent saving it on disk in clear text
  val currentKey = remember { mutableStateOf(if (initialRandomDBPassphrase.value) DatabaseUtils.ksDatabasePassword.get() ?: "" else "") }
  val newKey = rememberSaveable { mutableStateOf("") }
  val confirmNewKey = rememberSaveable { mutableStateOf("") }
  fun nextStep() {
    if (appPlatform.isAndroid || chatModel.currentUser.value != null) {
      m.controller.appPrefs.onboardingStage.set(OnboardingStage.Step3_ChooseServerOperators)
    } else {
      m.controller.appPrefs.onboardingStage.set(OnboardingStage.LinkAMobile)
    }
  }
  SetupDatabasePassphraseLayout(
    currentKey,
    newKey,
    confirmNewKey,
    progressIndicator,
    onConfirmEncrypt = {
      withLongRunningApi {
        if (m.chatRunning.value == true) {
          // Stop chat if it's started before doing anything
          stopChatAsync(m)
        }
        prefs.storeDBPassphrase.set(false)

        val newKeyValue = newKey.value
        val success = encryptDatabase(
          currentKey = currentKey,
          newKey = newKey,
          confirmNewKey = confirmNewKey,
          initialRandomDBPassphrase = mutableStateOf(true),
          useKeychain = mutableStateOf(false),
          storedKey = mutableStateOf(true),
          progressIndicator = progressIndicator,
          migration = false
        )
        if (success) {
          startChat(newKeyValue, ::nextStep)
        } else {
          // Rollback in case of it is finished with error in order to allow to repeat the process again
          prefs.storeDBPassphrase.set(true)
        }
      }
    },
    nextStep = ::nextStep,
  )

  if (progressIndicator.value) {
    ProgressIndicator()
  }

  DisposableEffect(Unit) {
    onDispose {
      if (m.chatRunning.value != true) {
        withBGApi {
          val user = chatController.apiGetActiveUser(null)
          if (user != null) {
            m.controller.startChat(user)
          }
        }
      }
    }
  }
}

@Composable
private fun SetupDatabasePassphraseLayout(
  currentKey: MutableState<String>,
  newKey: MutableState<String>,
  confirmNewKey: MutableState<String>,
  progressIndicator: MutableState<Boolean>,
  onConfirmEncrypt: () -> Unit,
  nextStep: () -> Unit,
) {
  val onClickUpdate = {
    if (!progressIndicator.value) {
      encryptDatabaseAlert(onConfirmEncrypt)
    }
  }
  val disabled = currentKey.value == newKey.value ||
      newKey.value != confirmNewKey.value ||
      newKey.value.isEmpty() ||
      !validKey(currentKey.value) ||
      !validKey(newKey.value) ||
      progressIndicator.value

  if (appPlatform.isDesktop) {
    NomeDesktopDatabasePassphraseLayout(
      newKey = newKey,
      confirmNewKey = confirmNewKey,
      disabled = disabled,
      progress = progressIndicator.value,
      onConfirmEncrypt = onClickUpdate,
      onUseRandomPassphrase = {
        randomPassphraseAlert {
          chatModel.desktopOnboardingRandomPassword.value = true
          nextStep()
        }
      },
    )
    return
  }

  CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
    ModalView({}, showClose = false) {
      ColumnWithScrollBar(
        Modifier.themedBackground(bgLayerSize = LocalAppBarHandler.current?.backgroundGraphicsLayerSize, bgLayer = LocalAppBarHandler.current?.backgroundGraphicsLayer).padding(horizontal = DEFAULT_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AppBarTitle(stringResource(MR.strings.setup_database_passphrase), overrideTitleColor = MaterialTheme.colors.onBackground)

        Column(Modifier.width(600.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          val textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.secondary)
          ReadableText(MR.strings.you_have_to_enter_passphrase_every_time, TextAlign.Center, padding = PaddingValues(), style = textStyle )
          Spacer(Modifier.height(DEFAULT_PADDING))
          ReadableText(MR.strings.impossible_to_recover_passphrase, TextAlign.Center, padding = PaddingValues(), style = textStyle)
          Spacer(Modifier.height(DEFAULT_PADDING))

          val focusRequester = remember { FocusRequester() }
          val focusManager = LocalFocusManager.current
          LaunchedEffect(Unit) {
            delay(100L)
            focusRequester.requestFocus()
          }
          PassphraseField(
            newKey,
            generalGetString(MR.strings.new_passphrase),
            modifier = Modifier
              .padding(horizontal = DEFAULT_PADDING)
              .focusRequester(focusRequester)
              .onPreviewKeyEvent {
                if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyUp) {
                  focusManager.moveFocus(FocusDirection.Down)
                  true
                } else {
                  false
                }
              },
            showStrength = true,
            isValid = ::validKey,
            keyboardActions = KeyboardActions(onNext = { defaultKeyboardAction(ImeAction.Next) }),
          )

          PassphraseField(
            confirmNewKey,
            generalGetString(MR.strings.confirm_new_passphrase),
            modifier = Modifier
              .padding(horizontal = DEFAULT_PADDING)
              .onPreviewKeyEvent {
                if (!disabled && (it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyUp) {
                  onClickUpdate()
                  true
                } else {
                  false
                }
              },
            isValid = { confirmNewKey.value == "" || newKey.value == confirmNewKey.value },
            keyboardActions = KeyboardActions(onDone = { defaultKeyboardAction(ImeAction.Done) }),
          )
        }
        Spacer(Modifier.weight(1f))

        Column(Modifier.widthIn(max = if (appPlatform.isAndroid) 450.dp else 1000.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          SetPassphraseButton(disabled, onClickUpdate)
          SkipButton(progressIndicator.value) {
            randomPassphraseAlert {
              chatModel.desktopOnboardingRandomPassword.value = true
              nextStep()
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NomeDesktopDatabasePassphraseLayout(
  newKey: MutableState<String>,
  confirmNewKey: MutableState<String>,
  disabled: Boolean,
  progress: Boolean,
  onConfirmEncrypt: () -> Unit,
  onUseRandomPassphrase: () -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val background = Color(0xFFF5F7FA)
  val surface = Color.White
  val text = Color(0xFF0E1B2D)
  val muted = Color(0xFF667085)
  val action = Color(0xFF0A874D)
  val border = Color(0xFFDCE3E9)
  val notice = Color(0xFFE8F6F0)

  Column(
    modifier = Modifier.fillMaxSize().background(background),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 30.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        painter = painterResource(MR.images.nome_mark),
        contentDescription = "Nome",
        modifier = Modifier.size(26.dp),
      )
      Text(
        text = "Nome",
        color = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp),
      )
      Spacer(Modifier.weight(1f))
      Text(
        text = stringResource(MR.strings.setup_database_passphrase),
        color = muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Divider(color = Color(0xFFE7ECF1))
    Column(
      modifier = Modifier.weight(1f).fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(horizontal = 48.dp, vertical = 42.dp),
      ) {
        Text(
          text = stringResource(MR.strings.setup_database_passphrase),
          color = text,
          fontSize = 34.sp,
          lineHeight = 40.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = stringResource(MR.strings.nome_desktop_database_body),
          color = muted,
          fontSize = 15.sp,
          lineHeight = 22.sp,
          modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(
          text = stringResource(MR.strings.nome_desktop_database_manual_title),
          color = muted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(9.dp))
        Surface(
          color = surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, border),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            PassphraseField(
              newKey,
              generalGetString(MR.strings.new_passphrase),
              modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                  if ((it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyUp) {
                    focusManager.moveFocus(FocusDirection.Down)
                    true
                  } else {
                    false
                  }
                },
              showStrength = true,
              isValid = ::validKey,
              keyboardActions = KeyboardActions(onNext = { defaultKeyboardAction(ImeAction.Next) }),
            )
            Spacer(Modifier.height(12.dp))
            PassphraseField(
              confirmNewKey,
              generalGetString(MR.strings.confirm_new_passphrase),
              modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent {
                  if (!disabled && (it.key == Key.Enter || it.key == Key.NumPadEnter) && it.type == KeyEventType.KeyUp) {
                    onConfirmEncrypt()
                    true
                  } else {
                    false
                  }
                },
              isValid = { confirmNewKey.value == "" || newKey.value == confirmNewKey.value },
              keyboardActions = KeyboardActions(onDone = { defaultKeyboardAction(ImeAction.Done) }),
            )
          }
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(notice, RoundedCornerShape(12.dp))
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            painter = painterResource(MR.images.ic_lock),
            contentDescription = null,
            tint = action,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = stringResource(MR.strings.nome_desktop_database_warning),
            color = muted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 10.dp),
          )
        }
      }
    }
    Divider(color = Color(0xFFE7ECF1))
    Row(
      modifier = Modifier.fillMaxWidth().background(surface).padding(horizontal = 34.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Button(
        onClick = onUseRandomPassphrase,
        enabled = !progress,
        modifier = Modifier.height(46.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.buttonColors(backgroundColor = surface, contentColor = text),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
      ) {
        Text(
          text = stringResource(MR.strings.use_random_passphrase),
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Spacer(Modifier.width(12.dp))
      Button(
        onClick = onConfirmEncrypt,
        enabled = !disabled,
        modifier = Modifier.height(46.dp).widthIn(min = 190.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = action,
          contentColor = Color.White,
          disabledBackgroundColor = Color(0xFF9FB4AA),
          disabledContentColor = Color.White.copy(alpha = 0.8f),
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
      ) {
        Text(
          text = stringResource(MR.strings.set_database_passphrase),
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }

  LaunchedEffect(Unit) {
    delay(140L)
    focusRequester.requestFocus()
  }
}

@Composable
private fun SetPassphraseButton(disabled: Boolean, onClick: () -> Unit) {
  OnboardingActionButton(
    if (appPlatform.isAndroid) Modifier.padding(horizontal = DEFAULT_PADDING).fillMaxWidth() else Modifier.widthIn(min = 300.dp),
    labelId = MR.strings.set_database_passphrase,
    onboarding = null,
    onclick = onClick,
    enabled =  !disabled
  )
}

@Composable
private fun SkipButton(disabled: Boolean, onClick: () -> Unit) {
  TextButtonBelowOnboardingButton(stringResource(MR.strings.use_random_passphrase), onClick = if (disabled) null else onClick)
}

@Composable
private fun ProgressIndicator() {
  Box(
    Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(
      Modifier
        .padding(horizontal = 2.dp)
        .size(30.dp),
      color = MaterialTheme.colors.secondary,
      strokeWidth = 3.dp
    )
  }
}

private suspend fun startChat(
  key: String?,
  onStarted: suspend () -> Unit,
) {
  val m = ChatModel
  initChatController(key, onChatStarted = onStarted)
  m.chatDbChanged.value = false
}

private fun randomPassphraseAlert(onConfirm: () -> Unit) {
  AlertManager.shared.showAlertDialog(
    title = generalGetString(MR.strings.use_random_passphrase),
    text = generalGetString(MR.strings.you_can_change_it_later),
    confirmText = generalGetString(MR.strings.ok),
    onConfirm = onConfirm,
  )
}
