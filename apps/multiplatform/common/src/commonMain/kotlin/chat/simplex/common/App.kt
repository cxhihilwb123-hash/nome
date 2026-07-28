package chat.simplex.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import chat.simplex.common.views.usersettings.SetDeliveryReceiptsView
import chat.simplex.common.model.*
import chat.simplex.common.model.ChatController.appPrefs
import chat.simplex.common.platform.*
import chat.simplex.common.ui.theme.*
import chat.simplex.common.views.CreateFirstProfile
import chat.simplex.common.views.helpers.SimpleButton
import chat.simplex.common.views.SplashView
import chat.simplex.common.views.call.*
import chat.simplex.common.views.chat.ChatView
import chat.simplex.common.views.chatlist.*
import chat.simplex.common.views.database.DatabaseRootRouteInput
import chat.simplex.common.views.database.DatabaseErrorView
import chat.simplex.common.views.database.NomeDatabaseRootFacts
import chat.simplex.common.views.database.PlatformDatabaseRootRoute
import chat.simplex.common.views.database.isMatchedDatabaseBackupAvailable
import chat.simplex.common.views.database.platformDatabaseKeyReadState
import chat.simplex.common.views.helpers.*
import chat.simplex.common.views.helpers.ModalManager.Companion.fromEndToStartTransition
import chat.simplex.common.views.helpers.ModalManager.Companion.fromStartToEndTransition
import chat.simplex.common.views.localauth.VerticalDivider
import chat.simplex.common.views.localauth.PlatformNomeAppLockScreen
import chat.simplex.common.views.newchat.*
import chat.simplex.common.views.onboarding.*
import chat.simplex.common.views.usersettings.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// Spec: spec/client/navigation.md#AppScreen
@Composable
fun AppScreen() {
  AppBarHandler.appBarMaxHeightPx = with(LocalDensity.current) { AppBarHeight.roundToPx() }
  SimpleXTheme {
    Surface(color = MaterialTheme.colors.background, contentColor = LocalContentColor.current) {
      // This padding applies to landscape view only taking care of navigation bar and holes in screen in status bar area
      // (because nav bar and holes located on vertical sides of screen in landscape view)
      val direction = LocalLayoutDirection.current
      val safePadding = WindowInsets.safeDrawing.asPaddingValues()
      val cutout = WindowInsets.displayCutout.asPaddingValues()
      val cutoutStart = cutout.calculateStartPadding(direction)
      val cutoutEnd = cutout.calculateEndPadding(direction)
      val cutoutMax = maxOf(cutoutStart, cutoutEnd)
      val paddingStartUntouched = safePadding.calculateStartPadding(direction)
      val paddingStart = paddingStartUntouched - cutoutStart
      val paddingEndUntouched = safePadding.calculateEndPadding(direction)
      val paddingEnd = paddingEndUntouched - cutoutEnd
      // Such a strange layout is needed because the main content should be covered by solid color in order to hide overflow
      // of some elements that may have negative offset (so, can't use Row {}).
      // To check: go to developer settings of Android, choose Display cutout -> Punch hole, and rotate the phone to landscape, open any chat
      Box {
        val fullscreenGallery = remember { chatModel.fullscreenGalleryVisible }
        Box(Modifier.padding(start = paddingStart + cutoutMax, end = paddingEnd + cutoutMax).consumeWindowInsets(PaddingValues(start = paddingStartUntouched, end = paddingEndUntouched))) {
          Box(Modifier.drawBehind {
            if (fullscreenGallery.value) {
              drawRect(Color.Black,  topLeft = Offset(-(paddingStart + cutoutMax).toPx(), 0f), Size(size.width + (paddingStart + cutoutMax).toPx() + (paddingEnd + cutoutMax).toPx(), size.height))
            }
          }) {
            MainScreen()
          }
        }
      }
    }
  }
}

// Spec: spec/client/navigation.md#MainScreen
@Composable
fun MainScreen() {
  val chatModel = ChatModel
  var showChatDatabaseError by rememberSaveable {
    mutableStateOf(chatModel.chatDbStatus.value != DBMigrationResult.OK && chatModel.chatDbStatus.value != null)
  }
  LaunchedEffect(chatModel.chatDbStatus.value) {
    showChatDatabaseError = chatModel.chatDbStatus.value != DBMigrationResult.OK && chatModel.chatDbStatus.value != null
  }
  val retryableChatStart = chatModel.retryableChatStart.value
  LaunchedEffect(retryableChatStart) {
    val failure = retryableChatStart ?: return@LaunchedEffect
    val (title, text) = when (failure.reason) {
      RetryableChatStartReason.NomeServerConfiguration ->
        MR.strings.nome_server_configuration_retry_title to MR.strings.nome_server_configuration_retry_text
      RetryableChatStartReason.NomeStartupFailure ->
        MR.strings.nome_startup_retry_title to MR.strings.nome_startup_retry_text
      RetryableChatStartReason.NomeContinuationFailure ->
        MR.strings.nome_continuation_retry_title to MR.strings.nome_continuation_retry_text
    }
    AlertManager.shared.showAlertDialogButtonsColumn(
      title = generalGetString(title),
      text = generalGetString(text),
      dismissible = false,
    ) {
      Row(
        Modifier.fillMaxWidth().padding(horizontal = DEFAULT_PADDING),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(
          onClick = {
            if (!chatModel.consumeRetryableChatStart(failure)) return@TextButton
            AlertManager.shared.hideAlert()
            withBGApi {
              if (failure.reason == RetryableChatStartReason.NomeContinuationFailure) {
                runNomeChatStartContinuation(
                  continuation = failure.onStarted,
                  onFailure = { error ->
                    Log.e(TAG, "Nome continuation retry failed (${error::class.simpleName ?: "unknown failure"})")
                    withContext(Dispatchers.Main) {
                      chatModel.retryableChatStart.value = newRetryableChatStart(
                        failure.user,
                        RetryableChatStartReason.NomeContinuationFailure,
                        failure.onStarted,
                      )
                    }
                  },
                )
              } else {
                try {
                  chatModel.controller.startChat(failure.user, failure.onStarted)
                } catch (e: CancellationException) {
                  throw e
                } catch (e: Throwable) {
                  // Do not display exception text: native failures can include server addresses
                  // or credential-bearing payloads. Publish a new attempt so recovery UI cannot
                  // disappear after an unexpected startup error.
                  Log.e(TAG, "Nome startup retry failed (${e::class.simpleName})")
                  withContext(Dispatchers.Main) {
                    chatModel.retryableChatStart.value = newRetryableChatStart(
                      failure.user,
                      RetryableChatStartReason.NomeStartupFailure,
                      failure.onStarted,
                    )
                  }
                }
              }
            }
          },
        ) {
          Text(generalGetString(MR.strings.retry_verb))
        }
      }
    }
  }
  var showAdvertiseLAAlert by remember { mutableStateOf(false) }
  LaunchedEffect(showAdvertiseLAAlert) {
    if (
      !chatModel.controller.appPrefs.laNoticeShown.get()
      && !appPrefs.performLA.get()
      && showAdvertiseLAAlert
      && chatModel.controller.appPrefs.onboardingStage.get() == OnboardingStage.OnboardingComplete
      && chatModel.chats.size > 3
      && chatModel.activeCallInvitation.value == null
    ) {
      AppLock.showLANotice(ChatModel.controller.appPrefs.laNoticeShown) }
  }
  LaunchedEffect(chatModel.showAdvertiseLAUnavailableAlert.value) {
    if (chatModel.showAdvertiseLAUnavailableAlert.value) {
      laUnavailableInstructionAlert()
    }
  }
  platform.desktopShowAppUpdateNotice()
  LaunchedEffect(chatModel.clearOverlays.value) {
    if (chatModel.clearOverlays.value) {
      ModalManager.closeAllModalsEverywhere()
      chatModel.clearOverlays.value = false
    }
  }

  @Composable
  fun AuthView() {
    val unlock = {
      AppLock.laFailed.value = false
      AppLock.runAuthenticate()
    }
    val currentUser = chatModel.currentUser.value
    PlatformNomeAppLockScreen(
      enabled = appPlatform.isAndroid,
      displayName = currentUser?.displayName,
      profileImage = currentUser?.image,
      usingLAMode = chatModel.controller.appPrefs.laMode.get(),
      onUnlock = unlock,
    ) {
      Surface(color = MaterialTheme.colors.background.copy(1f), contentColor = LocalContentColor.current) {
        Box(
          Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          SimpleButton(
            stringResource(MR.strings.auth_unlock),
            icon = painterResource(MR.images.ic_lock),
            click = unlock,
          )
        }
      }
    }
  }

  Box {
    val unauthorized = remember { derivedStateOf { AppLock.userAuthorized.value != true } }
    val onboarding by remember { chatModel.controller.appPrefs.onboardingStage.state }
    val localUserCreated = chatModel.localUserCreated.value
    var showInitializationView by remember { mutableStateOf(false) }

    fun databaseRootFacts(route: DatabaseRootRouteInput): NomeDatabaseRootFacts {
      val preferences = chatModel.controller.appPrefs
      val status = (route as? DatabaseRootRouteInput.Error)?.status
      val downgradeWarningCount =
        ((status as? DBMigrationResult.ErrorMigration)?.migrationError as? MigrationError.Downgrade)
          ?.let { downMigrationWarnings(it.downMigrations).size }
          ?: 0
      return NomeDatabaseRootFacts(
        route = route,
        ctrlInitInProgress = chatModel.ctrlInitInProgress.value,
        dbMigrationInProgress = chatModel.dbMigrationInProgress.value,
        storedKeyUseRequested = preferences.storeDBPassphrase.get(),
        storedKeyMaterialPresent =
          !preferences.encryptedDBPassphrase.get().isNullOrEmpty() &&
            !preferences.initializationVectorDBPassphrase.get().isNullOrEmpty(),
        androidKeyReadState = platformDatabaseKeyReadState(),
        matchedBackupAvailable = isMatchedDatabaseBackupAvailable(preferences),
        downgradeWarningCount = downgradeWarningCount,
      )
    }

    // Android owns an isolated in-call activity that may remain usable while the main app is
    // locked. Desktop calls run in a browser, so the desktop chat window must never suppress its
    // local-auth boundary merely because a call is active.
    val androidCallMayRemainVisible =
      appPlatform.isAndroid && chatModel.activeCallViewIsVisible.value && chatModel.showCallView.value
    val authOverlayVisible = unauthorized.value && !androidCallMayRemainVisible
    // Full-screen passcode views are held outside the ordinary modal stack. Include both
    // persistent and one-time passcode overlays so covered controls cannot still be reached by
    // VoiceOver or other accessibility actions.
    val passcodeOverlayVisible = ModalManager.fullscreen.hasPasscodeOverlay
    val modalOverlayVisible = ModalManager.fullscreen.hasModalsOpen
    // Authentication and passcode views are security boundaries, not decorative overlays. Do
    // not leave chat content, modals, shortcuts or clipboard listeners composed behind them.
    val sensitiveLayerBlocked = authOverlayVisible || passcodeOverlayVisible
    if (!sensitiveLayerBlocked) {
      Box(
        modifier =
        if (modalOverlayVisible) {
          Modifier.clearAndSetSemantics {}
        } else {
          Modifier
        },
      ) {
        when {
        onboarding == OnboardingStage.Step1_SimpleXInfo && chatModel.migrationState.value != null -> {
          // In migration process. Nothing should interrupt it, that's why it's the first branch in when()
          if (appPlatform.isDesktop) DesktopOnboarding(onboarding, chatModel)
          else SimpleXInfo(chatModel, onboarding = true)
        }
        chatModel.dbMigrationInProgress.value -> PlatformDatabaseRootRoute(
          facts = databaseRootFacts(DatabaseRootRouteInput.Migrating),
          allowSensitiveContent = !unauthorized.value,
        ) {
          DefaultProgressView(stringResource(MR.strings.database_migration_in_progress))
        }
        chatModel.chatDbStatus.value == null && showInitializationView -> PlatformDatabaseRootRoute(
          facts = databaseRootFacts(DatabaseRootRouteInput.Opening),
          allowSensitiveContent = !unauthorized.value,
        ) {
          DefaultProgressView(stringResource(MR.strings.opening_database))
        }
        showChatDatabaseError -> {
          // Prevent showing keyboard on Android when: passcode enabled and database password not saved
          val status = chatModel.chatDbStatus.value
          if (!unauthorized.value && status != null) {
            PlatformDatabaseRootRoute(
              facts = databaseRootFacts(DatabaseRootRouteInput.Error(status)),
              allowSensitiveContent = true,
            ) {
              DatabaseErrorView(chatModel.chatDbStatus, chatModel.controller.appPrefs)
            }
          }
        }
        remember { chatModel.chatDbEncrypted }.value == null || localUserCreated == null -> SplashView()
        onboarding == OnboardingStage.OnboardingComplete -> {
          Box {
            showAdvertiseLAAlert = true
            val userPickerState by rememberSaveable(stateSaver = AnimatedViewState.saver()) { mutableStateOf(MutableStateFlow(if (chatModel.desktopNoUserNoRemote()) AnimatedViewState.VISIBLE else AnimatedViewState.GONE)) }
            KeyChangeEffect(chatModel.desktopNoUserNoRemote) {
              if (chatModel.desktopNoUserNoRemote() && !ModalManager.start.hasModalsOpen()) {
                userPickerState.value = AnimatedViewState.VISIBLE
              }
            }
            SetupClipboardListener()
            if (appPlatform.isAndroid) {
              AndroidWrapInCallLayout {
                AndroidScreen(userPickerState)
              }
            } else {
              DesktopScreen(userPickerState)
            }
          }
        }
        else -> {
          if (appPlatform.isDesktop) {
            DesktopOnboarding(onboarding, chatModel)
          } else {
            AnimatedContent(targetState = onboarding,
              transitionSpec = {
                if (targetState > initialState) {
                  fromEndToStartTransition()
                } else {
                  fromStartToEndTransition()
                }.using(SizeTransform(clip = false))
              }
            ) { state ->
              when (state) {
                OnboardingStage.OnboardingComplete -> {}
                OnboardingStage.Step1_SimpleXInfo -> SimpleXInfo(chatModel, onboarding = true)
                OnboardingStage.Step2_CreateProfile -> CreateFirstProfile(chatModel) {}
                OnboardingStage.LinkAMobile -> LinkAMobile()
                OnboardingStage.Step2_5_SetupDatabasePassphrase -> SetupDatabasePassphrase(chatModel)
                OnboardingStage.Step3_ChooseServerOperators,
                OnboardingStage.Step3_CreateSimpleXAddress,
                OnboardingStage.Step4_SetNotificationsMode -> YourNetworkView(chatModel)
                OnboardingStage.Step4_NetworkCommitments -> OnboardingConditionsView(chatModel)
              }
            }
          }
        }
        }
      }
      if (appPlatform.isAndroid) {
        Box {
          AndroidWrapInCallLayout {
            ModalManager.fullscreen.showInView()
          }
          SwitchingUsersView()
        }
      } else {
        // Desktop fullscreen modals must be a sibling of the semantics-cleared application
        // content. Keeping this layer inside DesktopScreen/DesktopOnboarding would clear the
        // modal's own controls from VoiceOver together with the obscured background.
        Box {
          ModalManager.fullscreen.showInView()
        }
      }
    }

    if (authOverlayVisible) {
      LaunchedEffect(Unit) {
        // With these constrains when user presses back button while on ChatList, activity destroys and shows auth request
        // while the screen moves to a launcher. Detect it and prevent showing the auth
        if (!(androidIsFinishingMainActivity() && chatModel.controller.appPrefs.laMode.get() == LAMode.SYSTEM)) {
          AppLock.runAuthenticate()
        }
      }
      if (chatModel.controller.appPrefs.performLA.get() && AppLock.laFailed.value) {
        AuthView()
      } else {
        SplashView(true)
        ModalManager.fullscreen.showPasscodeInView()
      }
    } else {
      if (!passcodeOverlayVisible && chatModel.showCallView.value) {
        if (appPlatform.isAndroid) {
          LaunchedEffect(Unit) {
            // This if prevents running the activity in the following condition:
            // - the activity already started before and was destroyed by collapsing active call (start audio call, press back button, go to a launcher)
            if (!chatModel.activeCallViewIsCollapsed.value) {
              platform.androidStartCallActivity(false)
            }
          }
        } else {
          ActiveCallView()
        }
      }
      ModalManager.fullscreen.showOneTimePasscodeInView()
      if (!passcodeOverlayVisible) AlertManager.privacySensitive.showInView()
      if (!passcodeOverlayVisible && onboarding == OnboardingStage.OnboardingComplete) {
        LaunchedEffect(chatModel.chatRunning.value, chatModel.currentUser.value, chatModel.appOpenUrl.value) {
          val pendingUrl = chatModel.appOpenUrl.value
          if (pendingUrl != null && chatModel.chatRunning.value == true) {
            chatModel.appOpenUrl.value = null
            connectIfOpenedViaUri(
              rhId = pendingUrl.remoteHostId,
              uri = pendingUrl.uri,
              chatModel = chatModel,
              source = pendingUrl.source,
            )
          }
        }
      }
    }
    if (!sensitiveLayerBlocked) {
      val invitation = chatModel.activeCallInvitation.value
      if (invitation != null) IncomingCallAlertView(invitation, chatModel)
      AlertManager.shared.showInView()
    }

    LaunchedEffect(Unit) {
      delay(1000)
      if (chatModel.chatDbStatus.value == null) {
        showInitializationView = true
      }
    }
  }

  DisposableEffectOnRotate {
    // When using lock delay = 0 and screen rotates, the app will be locked which is not useful.
    // Let's prolong the unlocked period to 3 sec for screen rotation to take place
    if (chatModel.controller.appPrefs.laLockDelay.get() == 0) {
      AppLock.enteredBackground.value = AppLock.elapsedRealtime() + 3000
    }
  }
}

@Composable
private fun DesktopOnboarding(onboarding: OnboardingStage, chatModel: ChatModel) {
  if (onboarding == OnboardingStage.LinkAMobile) {
    LinkAMobile()
  } else {
    Box(Modifier.fillMaxSize()) {
      when (onboarding) {
        OnboardingStage.Step1_SimpleXInfo -> SimpleXInfo(chatModel, onboarding = true)
        OnboardingStage.Step2_CreateProfile -> CreateFirstProfile(chatModel) {}
        OnboardingStage.Step2_5_SetupDatabasePassphrase -> SetupDatabasePassphrase(chatModel)
        OnboardingStage.Step3_ChooseServerOperators,
        OnboardingStage.Step3_CreateSimpleXAddress,
        OnboardingStage.Step4_SetNotificationsMode -> YourNetworkView(chatModel)
        OnboardingStage.Step4_NetworkCommitments -> OnboardingConditionsView(chatModel)
        else -> {}
      }
    }
  }
}

val ANDROID_CALL_TOP_PADDING = 40.dp

@Composable
fun AndroidWrapInCallLayout(content: @Composable () -> Unit) {
  val call = remember { chatModel.activeCall}.value
  val showCallArea = call != null && call.callState != CallState.WaitCapabilities && call.callState != CallState.InvitationAccepted
  Box {
    Box(Modifier.padding(top = if (showCallArea) ANDROID_CALL_TOP_PADDING else 0.dp)) {
      content()
    }
    if (call != null && showCallArea) {
      ActiveCallInteractiveArea(call)
    }
  }
}

// Spec: spec/client/navigation.md#AndroidScreen
@Composable
fun AndroidScreen(userPickerState: MutableStateFlow<AnimatedViewState>) {
  BoxWithConstraints {
    val currentChatId = remember { mutableStateOf(chatModel.chatId.value) }
    val offset = remember { Animatable(if (chatModel.chatId.value == null) 0f else maxWidth.value) }
    val cutout = WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal).asPaddingValues()
    val direction = LocalLayoutDirection.current
    val hasCutout = cutout.calculateStartPadding(direction) + cutout.calculateEndPadding(direction) > 0.dp
    Box(
      Modifier
        // clipping only for devices with cutout currently visible on sides. It prevents showing chat list with open chat view
        // In order cases it's not needed to use clip
        .then(if (hasCutout) Modifier.clip(RectangleShape) else Modifier)
        .graphicsLayer {
          // minOf thing is needed for devices with holes in screen while the user on ChatView rotates his phone from portrait to landscape
          // because in this case (at least in emulator) maxWidth changes in two steps: big first, smaller on next frame.
          // But offset is remembered already, so this is a better way than dropping a value of offset
          translationX = -minOf(offset.value.dp, maxWidth).toPx()
        }
    ) {
      StartPartOfScreen(userPickerState)
    }
    val scope = rememberCoroutineScope()
    val onComposed: suspend (chatId: String?) -> Unit = { chatId ->
      // coroutine, scope and join() because:
      // - it should be run from coroutine to wait until this function finishes
      // - without using scope.launch it throws CancellationException when changing user
      // - join allows to wait until completion
      scope.launch {
        offset.animateTo(
          if (chatId == null) 0f else maxWidth.value,
          chatListAnimationSpec()
        )
      }.join()
    }
    LaunchedEffect(Unit) {
      launch {
        snapshotFlow { chatModel.chatId.value }
          .distinctUntilChanged()
          .collect {
            if (it == null) onComposed(null)
            currentChatId.value = it
          }
      }
    }
    Box(Modifier
      .then(if (hasCutout) Modifier.clip(RectangleShape) else Modifier)
      .graphicsLayer { translationX = maxWidth.toPx() - minOf(offset.value.dp, maxWidth).toPx() }
    ) Box2@{
      currentChatId.value?.let {
        ChatView(chatsCtx = chatModel.chatsContext, currentChatId, onComposed = onComposed)
      }
    }
  }
}

@Composable
fun StartPartOfScreen(userPickerState: MutableStateFlow<AnimatedViewState>) {
  if (chatModel.setDeliveryReceipts.value) {
    CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
      SetDeliveryReceiptsView(chatModel)
    }
  } else {
    val stopped = chatModel.chatRunning.value == false
    if (chatModel.sharedContent.value == null) {
      CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler()) {
        ChatListNoticeEffect(chatModel)
        PlatformHomeRoute(
          chatModel = chatModel,
          userPickerState = userPickerState,
          setPerformLA = AppLock::setPerformLA,
          stopped = stopped,
        ) {
          ChatListView(chatModel, userPickerState, AppLock::setPerformLA, stopped)
        }
      }
    } else {
      // LALAL initial load of view doesn't show blur. Focusing text field shows it
      CompositionLocalProvider(LocalAppBarHandler provides rememberAppBarHandler(keyboardCoversBar = false)) {
        ShareListView(chatModel, stopped)
      }
    }
  }
}

@Composable
fun CenterPartOfScreen() {
  val currentChatId = remember { ChatModel.chatId }
  LaunchedEffect(Unit) {
    snapshotFlow { currentChatId.value }
      .distinctUntilChanged()
      .collect {
        if (it != null) {
          ModalManager.center.closeModals()
        }
      }
  }
  when (currentChatId.value) {
    null -> {
      if (shouldShowOnboarding()) {
        ConnectOnboardingView()
      } else if (!rememberUpdatedState(ModalManager.center.hasModalsOpen()).value) {
        Box(
          Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
          contentAlignment = Alignment.Center
        ) {
          Text(stringResource(if (chatModel.desktopNoUserNoRemote) MR.strings.no_connected_mobile else MR.strings.no_selected_chat))
        }
      } else {
        ModalManager.center.showInView()
      }
    }
    else -> ChatView(chatsCtx = chatModel.chatsContext, currentChatId) {}
  }
}

@Composable
fun EndPartOfScreen() {
  ModalManager.end.showInView()
}

// Spec: spec/client/navigation.md#DesktopScreen
@Composable
fun DesktopScreen(userPickerState: MutableStateFlow<AnimatedViewState>) {
  val conversationListWidth = DEFAULT_START_MODAL_WIDTH * fontSizeSqrtMultiplier
  val startPanelWidth = NOME_DESKTOP_NAV_RAIL_WIDTH + conversationListWidth
  Box(Modifier.width(startPanelWidth)) {
    StartPartOfScreen(userPickerState)
    Box(Modifier.padding(start = NOME_DESKTOP_NAV_RAIL_WIDTH).width(conversationListWidth)) {
      tryOrShowError("UserPicker", error = {}) {
        UserPicker(chatModel, userPickerState, setPerformLA = AppLock::setPerformLA)
      }
    }
  }
  Box(Modifier.padding(start = NOME_DESKTOP_NAV_RAIL_WIDTH).width(conversationListWidth)) {
    ModalManager.start.showInView()
    SwitchingUsersView()
  }
  Row(Modifier.padding(start = startPanelWidth).clipToBounds()) {
    Box(Modifier.widthIn(min = DEFAULT_MIN_CENTER_MODAL_WIDTH).weight(1f)) {
      CenterPartOfScreen()
    }
    if (ModalManager.end.hasModalsOpen()) {
      VerticalDivider()
    }
    Box(Modifier.widthIn(max = DEFAULT_END_MODAL_WIDTH * fontSizeSqrtMultiplier).clipToBounds()) {
      EndPartOfScreen()
    }
  }
  if (userPickerState.collectAsState().value.isVisible() || (ModalManager.start.hasModalsOpen && !ModalManager.center.hasModalsOpen)) {
    Box(
      Modifier
        .fillMaxSize()
        .padding(start = startPanelWidth)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {
          if (chatModel.centerPanelBackgroundClickHandler == null || chatModel.centerPanelBackgroundClickHandler?.invoke() == false) {
            ModalManager.start.closeModals()
            userPickerState.value = AnimatedViewState.HIDING
          }
        })
    )
  }
  VerticalDivider(Modifier.padding(start = startPanelWidth))
}

@Composable
private fun SwitchingUsersView() {
  if (remember { chatModel.switchingUsersAndHosts }.value) {
    Box(
      Modifier.fillMaxSize().clickable(enabled = false, onClick = {}),
      contentAlignment = Alignment.Center
    ) {
      ProgressIndicator()
    }
  }
}

@Composable
private fun ProgressIndicator() {
  CircularProgressIndicator(
    Modifier
      .padding(horizontal = 2.dp)
      .size(30.dp),
    color = MaterialTheme.colors.secondary,
    strokeWidth = 2.5.dp
  )
}
