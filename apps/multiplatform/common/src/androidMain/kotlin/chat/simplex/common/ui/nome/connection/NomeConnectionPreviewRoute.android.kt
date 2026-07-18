package chat.simplex.common.ui.nome.connection

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import chat.simplex.common.R
import chat.simplex.common.ui.nome.accessibility.nomeMinimumTouchTarget
import chat.simplex.common.ui.nome.accessibility.nomeTalkBackSemantics
import chat.simplex.common.ui.nome.components.NomeButton
import chat.simplex.common.ui.nome.components.NomeButtonVariant
import chat.simplex.common.ui.nome.components.NomeStatePanel
import chat.simplex.common.ui.nome.components.NomeStatePanelState
import chat.simplex.common.ui.nome.components.NomeSurface
import chat.simplex.common.ui.nome.theme.NomeTheme
import chat.simplex.common.views.helpers.ProfileImage
import chat.simplex.common.views.newchat.ConnectionPreviewAttemptResult
import chat.simplex.common.views.newchat.ConnectionPreviewCallbacks
import chat.simplex.common.views.newchat.ConnectionPreviewFailureKind
import chat.simplex.common.views.newchat.ConnectionPreviewIdentity
import chat.simplex.common.views.newchat.ConnectionPreviewKind
import chat.simplex.common.views.newchat.ConnectionPreviewOwnerStatus
import chat.simplex.common.views.newchat.ConnectionPreviewRetryResult
import chat.simplex.common.views.newchat.ConnectionPreviewUiModel
import chat.simplex.common.views.newchat.ConnectionPreviewWarning
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private class RetainedConnectionPreviewState(
  initialIdentity: ConnectionPreviewIdentity,
) {
  val state = mutableStateOf(
    NomeConnectionPreviewStateAdapter.initial(initialIdentity),
  )
  val handingOff = AtomicBoolean(false)
  private val finished = AtomicBoolean(false)
  private val operationInFlight = AtomicBoolean(false)
  val scope = CoroutineScope(
    SupervisorJob() + Dispatchers.Main.immediate,
  )

  fun beginOperation(): Boolean =
    operationInFlight.compareAndSet(false, true)

  fun endOperation() {
    operationInFlight.set(false)
  }

  fun cancel(
    key: Long,
    onCancel: () -> Unit,
  ) {
    if (finished.compareAndSet(false, true)) {
      RetainedConnectionPreviewStateStore.remove(key, this)
      onCancel()
      scope.cancel()
    }
  }

  fun releaseForHandoff(key: Long): Boolean =
    handingOff.compareAndSet(false, true).also { released ->
      if (released) {
        finished.set(true)
        RetainedConnectionPreviewStateStore.remove(key, this)
      }
    }
}

private object RetainedConnectionPreviewStateStore {
  private val states =
    ConcurrentHashMap<Long, RetainedConnectionPreviewState>()

  fun obtain(
    key: Long,
    initialIdentity: ConnectionPreviewIdentity,
  ): RetainedConnectionPreviewState =
    states.computeIfAbsent(key) {
      RetainedConnectionPreviewState(initialIdentity)
    }

  fun remove(
    key: Long,
    state: RetainedConnectionPreviewState,
  ) {
    states.remove(key, state)
  }
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

@Composable
fun NomeConnectionPreviewRoute(
  model: ConnectionPreviewUiModel,
  callbacks: ConnectionPreviewCallbacks,
  close: () -> Unit,
) {
  val retentionKey = remember(model.attemptId, callbacks) {
    if (model.attemptId != 0L) {
      model.attemptId
    } else {
      -System.identityHashCode(callbacks).toLong()
    }
  }
  val retained = remember(retentionKey) {
    RetainedConnectionPreviewStateStore.obtain(
      retentionKey,
      model.initialIdentity,
    )
  }
  val state by retained.state
  val activity = LocalContext.current.findActivity()

  fun cancelAndClose() {
    retained.cancel(retentionKey, callbacks.cancel)
    close()
  }

  BackHandler {
    if (
      state.phase != NomeConnectionPreviewPhase.Connecting &&
      state.phase != NomeConnectionPreviewPhase.Replanning
    ) {
      cancelAndClose()
    }
  }
  LaunchedEffect(callbacks) {
    if (!callbacks.isActive()) {
      close()
    }
  }
  DisposableEffect(callbacks) {
    onDispose {
      if (
        !retained.handingOff.get() &&
        activity?.isChangingConfigurations != true
      ) {
        retained.cancel(retentionKey, callbacks.cancel)
      }
    }
  }

  NomeConnectionPreviewContent(
    model = model,
    state = state,
    onIdentitySelected = { identity ->
      retained.state.value = NomeConnectionPreviewStateAdapter.reduce(
        state,
        NomeConnectionPreviewEvent.SelectIdentity(identity),
      )
    },
    onPrimary = {
      when (state.phase) {
        NomeConnectionPreviewPhase.Ready -> {
          val connecting = NomeConnectionPreviewStateAdapter.reduce(
            state,
            NomeConnectionPreviewEvent.Submit,
          )
          if (
            connecting != state &&
            retained.beginOperation()
          ) {
            val identity = state.identity
            retained.state.value = connecting
            retained.scope.launch {
              try {
                val result = callbacks.connect(identity)
                currentCoroutineContext().ensureActive()
                retained.state.value = when (result) {
                  ConnectionPreviewAttemptResult.Pending ->
                    NomeConnectionPreviewStateAdapter.reduce(
                      retained.state.value,
                      NomeConnectionPreviewEvent.Pending,
                    )
                  is ConnectionPreviewAttemptResult.AlreadyExists ->
                    NomeConnectionPreviewStateAdapter.reduce(
                      retained.state.value,
                      NomeConnectionPreviewEvent.Failed(
                        kind = ConnectionPreviewFailureKind.AlreadyExists,
                        existingContactName = result.displayName,
                      ),
                    )
                  is ConnectionPreviewAttemptResult.Failure ->
                    NomeConnectionPreviewStateAdapter.reduce(
                      retained.state.value,
                      NomeConnectionPreviewEvent.Failed(result.kind),
                    )
                  ConnectionPreviewAttemptResult.ContextChanged ->
                    NomeConnectionPreviewStateAdapter.reduce(
                      retained.state.value,
                      NomeConnectionPreviewEvent.Failed(
                        ConnectionPreviewFailureKind.ContextChanged,
                      ),
                    )
                  ConnectionPreviewAttemptResult.NoCurrentUser ->
                    NomeConnectionPreviewStateAdapter.reduce(
                      retained.state.value,
                      NomeConnectionPreviewEvent.Failed(
                        ConnectionPreviewFailureKind.NoCurrentUser,
                      ),
                    )
                }
              } catch (e: CancellationException) {
                cancelAndClose()
                throw e
              } finally {
                retained.endOperation()
              }
            }
          }
        }
        NomeConnectionPreviewPhase.Failure -> {
          val replanning = NomeConnectionPreviewStateAdapter.reduce(
            state,
            NomeConnectionPreviewEvent.Retry,
          )
          if (
            replanning != state &&
            retained.beginOperation()
          ) {
            val identity = state.identity
            retained.state.value = replanning
            retained.scope.launch {
              try {
                val result = callbacks.retry(identity)
                currentCoroutineContext().ensureActive()
                when (result) {
                  is ConnectionPreviewRetryResult.Handoff -> {
                    result.continueFlow(
                      {
                        if (
                          retained.releaseForHandoff(retentionKey)
                        ) {
                          close()
                        }
                      },
                      { kind ->
                        retained.state.value =
                          NomeConnectionPreviewStateAdapter.reduce(
                          retained.state.value,
                          NomeConnectionPreviewEvent.Failed(kind),
                        )
                      },
                    )
                  }
                }
              } catch (e: CancellationException) {
                retained.cancel(retentionKey, callbacks.cancel)
                throw e
              } finally {
                retained.endOperation()
                if (retained.handingOff.get()) {
                  retained.scope.cancel()
                }
              }
            }
          }
        }
        NomeConnectionPreviewPhase.Pending -> cancelAndClose()
        NomeConnectionPreviewPhase.Connecting,
        NomeConnectionPreviewPhase.Replanning -> Unit
      }
    },
    onCancel = ::cancelAndClose,
  )
}

@Composable
fun NomeConnectionPreviewContent(
  model: ConnectionPreviewUiModel,
  state: NomeConnectionPreviewState,
  onIdentitySelected: (ConnectionPreviewIdentity) -> Unit,
  onPrimary: () -> Unit,
  onCancel: () -> Unit,
) {
  val dimensions = NomeTheme.dimensions
  val statusFocus = remember { FocusRequester() }
  val listState = rememberLazyListState()
  val operationItemIndex =
    2 +
        (if (model.warning != ConnectionPreviewWarning.None) 1 else 0) +
        (if (model.ownerStatus !is ConnectionPreviewOwnerStatus.Absent) 1 else 0)
  LaunchedEffect(state.phase, operationItemIndex) {
    if (state.phase != NomeConnectionPreviewPhase.Ready) {
      listState.scrollToItem(operationItemIndex)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(NomeTheme.colors.background)
      .windowInsetsPadding(WindowInsets.safeDrawing),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          start = dimensions.space8,
          top = dimensions.space4,
          end = dimensions.screenHorizontalInset,
          bottom = dimensions.space4,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onCancel,
        enabled =
          state.phase != NomeConnectionPreviewPhase.Connecting &&
              state.phase != NomeConnectionPreviewPhase.Replanning,
        modifier = Modifier
          .nomeMinimumTouchTarget()
          .nomeTalkBackSemantics(
            label = androidx.compose.ui.res.stringResource(
              R.string.nome_connection_preview_back,
            ),
            role = Role.Button,
          ),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = null,
          tint = NomeTheme.colors.textPrimary,
        )
      }
      Text(
        text = androidx.compose.ui.res.stringResource(
          R.string.nome_connection_preview_title,
        ),
        modifier = Modifier
          .padding(start = dimensions.space8)
          .semantics { heading() },
        style = NomeTheme.typography.title,
        color = NomeTheme.colors.textPrimary,
      )
    }

    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(
        start = dimensions.screenHorizontalInset,
        top = dimensions.space8,
        end = dimensions.screenHorizontalInset,
        bottom = dimensions.space20,
      ),
      verticalArrangement = Arrangement.spacedBy(dimensions.space16),
    ) {
      item {
        ConnectionSourcePanel(model.kind)
      }
      item {
        Column(verticalArrangement = Arrangement.spacedBy(dimensions.space8)) {
          Text(
            text = androidx.compose.ui.res.stringResource(
              R.string.nome_connection_preview_choose_identity,
            ),
            style = NomeTheme.typography.label,
            color = NomeTheme.colors.textSecondary,
          )
          IdentityChoice(
            title = androidx.compose.ui.res.stringResource(
              R.string.nome_connection_preview_current_title,
            ),
            body = androidx.compose.ui.res.stringResource(
              R.string.nome_connection_preview_current_body,
              model.currentProfileName,
            ),
            selected = state.identity ==
                ConnectionPreviewIdentity.CurrentProfile,
            enabled = state.phase == NomeConnectionPreviewPhase.Ready,
            icon = {
              ProfileImage(
                size = 48.dp,
                image = model.currentProfileImage,
                color = NomeTheme.colors.action,
              )
            },
            onClick = {
              onIdentitySelected(ConnectionPreviewIdentity.CurrentProfile)
            },
          )
          IdentityChoice(
            title = androidx.compose.ui.res.stringResource(
              R.string.nome_connection_preview_incognito_title,
            ),
            body = androidx.compose.ui.res.stringResource(
              R.string.nome_connection_preview_incognito_body,
            ),
            selected = state.identity == ConnectionPreviewIdentity.Incognito,
            enabled = state.phase == NomeConnectionPreviewPhase.Ready,
            icon = {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .background(
                    NomeTheme.colors.surfaceSubtle,
                    CircleShape,
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.PersonOutline,
                  contentDescription = null,
                  tint = NomeTheme.colors.textPrimary,
                )
              }
            },
            onClick = {
              onIdentitySelected(ConnectionPreviewIdentity.Incognito)
            },
          )
        }
      }
      if (model.warning != ConnectionPreviewWarning.None) {
        item {
          ConnectionWarningPanel(model)
        }
      }
      if (model.ownerStatus !is ConnectionPreviewOwnerStatus.Absent) {
        item {
          OwnerVerificationPanel(model.ownerStatus)
        }
      }
      if (state.phase != NomeConnectionPreviewPhase.Ready) {
        item {
          ConnectionOperationPanel(
            model = model,
            state = state,
            modifier = Modifier
              .focusRequester(statusFocus)
              .focusable(),
          )
          LaunchedEffect(state.phase) {
            if (
              state.phase == NomeConnectionPreviewPhase.Failure ||
              state.phase == NomeConnectionPreviewPhase.Pending
            ) {
              statusFocus.requestFocus()
            }
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(NomeTheme.colors.background)
        .navigationBarsPadding()
        .padding(
          start = dimensions.screenHorizontalInset,
          top = dimensions.space12,
          end = dimensions.screenHorizontalInset,
          bottom = dimensions.space12,
        ),
      verticalArrangement = Arrangement.spacedBy(dimensions.space8),
    ) {
      NomeButton(
        text = primaryActionLabel(state.phase),
        onClick = onPrimary,
        modifier = Modifier.fillMaxWidth(),
        variant =
          if (
            state.phase == NomeConnectionPreviewPhase.Ready &&
            model.warning != ConnectionPreviewWarning.None
          ) {
            NomeButtonVariant.DESTRUCTIVE
          } else {
            NomeButtonVariant.PRIMARY
          },
        enabled =
          state.phase != NomeConnectionPreviewPhase.Connecting &&
              state.phase != NomeConnectionPreviewPhase.Replanning,
        stateDescription = operationStateDescription(state.phase),
      )
      NomeButton(
        text = androidx.compose.ui.res.stringResource(
          R.string.nome_connection_preview_cancel,
        ),
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        variant = NomeButtonVariant.SECONDARY,
        enabled =
          state.phase != NomeConnectionPreviewPhase.Connecting &&
              state.phase != NomeConnectionPreviewPhase.Replanning,
      )
    }
  }
}

@Composable
private fun ConnectionSourcePanel(kind: ConnectionPreviewKind) {
  val dimensions = NomeTheme.dimensions
  NomeSurface(
    modifier = Modifier.fillMaxWidth(),
    color = NomeTheme.colors.successContainer,
    contentColor = NomeTheme.colors.onSuccessContainer,
    border = BorderStroke(
      dimensions.divider,
      NomeTheme.colors.onSuccessContainer.copy(alpha = 0.28f),
    ),
  ) {
    Row(
      modifier = Modifier.padding(dimensions.space16),
      horizontalArrangement = Arrangement.spacedBy(dimensions.space12),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = Icons.Rounded.Security,
        contentDescription = null,
        modifier = Modifier.size(dimensions.icon),
      )
      Text(
        text = androidx.compose.ui.res.stringResource(
          when (kind) {
            ConnectionPreviewKind.Invitation ->
              R.string.nome_connection_preview_source_invitation
            ConnectionPreviewKind.ContactAddress ->
              R.string.nome_connection_preview_source_address
            ConnectionPreviewKind.Group ->
              R.string.nome_connection_preview_source_group
          },
        ),
        style = NomeTheme.typography.body,
      )
    }
  }
}

@Composable
private fun IdentityChoice(
  title: String,
  body: String,
  selected: Boolean,
  enabled: Boolean,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
) {
  val dimensions = NomeTheme.dimensions
  val selectedState = androidx.compose.ui.res.stringResource(
    if (selected) {
      R.string.nome_connection_preview_selected
    } else {
      R.string.nome_connection_preview_not_selected
    },
  )
  NomeSurface(
    modifier = Modifier
      .fillMaxWidth()
      .nomeMinimumTouchTarget()
      .selectable(
        selected = selected,
        enabled = enabled,
        role = Role.RadioButton,
        onClick = onClick,
      )
      .semantics(mergeDescendants = true) {
        this.selected = selected
        stateDescription = selectedState
      },
    color =
      if (selected) {
        NomeTheme.colors.successContainer
      } else {
        NomeTheme.colors.surface
      },
    border = BorderStroke(
      dimensions.divider,
      if (selected) NomeTheme.colors.success else NomeTheme.colors.border,
    ),
  ) {
    Row(
      modifier = Modifier.padding(dimensions.space12),
      horizontalArrangement = Arrangement.spacedBy(dimensions.space12),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      icon()
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(dimensions.space2),
      ) {
        Text(
          text = title,
          style = NomeTheme.typography.bodyStrong,
          color = NomeTheme.colors.textPrimary,
        )
        Text(
          text = body,
          style = NomeTheme.typography.supporting,
          color = NomeTheme.colors.textSecondary,
        )
      }
      if (selected) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = null,
          tint = NomeTheme.colors.success,
        )
      }
    }
  }
}

@Composable
private fun ConnectionWarningPanel(model: ConnectionPreviewUiModel) {
  NomeStatePanel(
    state = NomeStatePanelState.DANGER,
    title = androidx.compose.ui.res.stringResource(
      R.string.nome_connection_preview_warning_title,
    ),
    description = androidx.compose.ui.res.stringResource(
      when (model.warning) {
        ConnectionPreviewWarning.OwnLink ->
          if (model.kind == ConnectionPreviewKind.Invitation) {
            R.string.nome_connection_preview_warning_own_invitation
          } else {
            R.string.nome_connection_preview_warning_own_address
          }
        ConnectionPreviewWarning.RepeatRequest ->
          R.string.nome_connection_preview_warning_repeat_request
        ConnectionPreviewWarning.RepeatJoin ->
          R.string.nome_connection_preview_warning_repeat_join
        ConnectionPreviewWarning.None ->
          R.string.nome_connection_preview_warning_title
      },
    ),
  )
}

@Composable
private fun OwnerVerificationPanel(
  status: ConnectionPreviewOwnerStatus,
) {
  val description = when (status) {
    ConnectionPreviewOwnerStatus.Absent -> return
    ConnectionPreviewOwnerStatus.Verified ->
      androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_owner_verified,
      )
    is ConnectionPreviewOwnerStatus.Failed ->
      androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_owner_failed,
        status.reason,
      )
  }
  NomeStatePanel(
    state =
      if (status is ConnectionPreviewOwnerStatus.Verified) {
        NomeStatePanelState.NORMAL
      } else {
        NomeStatePanelState.DANGER
      },
    title = description,
    icon = {
      Icon(
        imageVector =
          if (status is ConnectionPreviewOwnerStatus.Verified) {
            Icons.Rounded.CheckCircle
          } else {
            Icons.Rounded.Warning
          },
        contentDescription = null,
      )
    },
  )
}

@Composable
private fun ConnectionOperationPanel(
  model: ConnectionPreviewUiModel,
  state: NomeConnectionPreviewState,
  modifier: Modifier,
) {
  val title: String
  val body: String
  val panelState: NomeStatePanelState
  val liveRegion: LiveRegionMode
  when (state.phase) {
    NomeConnectionPreviewPhase.Connecting -> {
      title = androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_connecting_title,
      )
      body = androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_connecting_body,
      )
      panelState = NomeStatePanelState.LOADING
      liveRegion = LiveRegionMode.Polite
    }
    NomeConnectionPreviewPhase.Replanning -> {
      title = androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_replanning_title,
      )
      body = androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_replanning_body,
      )
      panelState = NomeStatePanelState.LOADING
      liveRegion = LiveRegionMode.Polite
    }
    NomeConnectionPreviewPhase.Pending -> {
      title = androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_pending_title,
      )
      body = androidx.compose.ui.res.stringResource(
        when (model.kind) {
          ConnectionPreviewKind.Invitation ->
            R.string.nome_connection_preview_pending_invitation
          ConnectionPreviewKind.ContactAddress ->
            R.string.nome_connection_preview_pending_address
          ConnectionPreviewKind.Group ->
            R.string.nome_connection_preview_pending_group
        },
      )
      panelState = NomeStatePanelState.NORMAL
      liveRegion = LiveRegionMode.Polite
    }
    NomeConnectionPreviewPhase.Failure -> {
      title = androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_failure_title,
      )
      body = failureDescription(state)
      panelState = NomeStatePanelState.ERROR
      liveRegion = LiveRegionMode.Assertive
    }
    NomeConnectionPreviewPhase.Ready -> return
  }
  NomeStatePanel(
    state = panelState,
    title = title,
    description = body,
    stateDescription = operationStateDescription(state.phase),
    modifier = modifier
      .fillMaxWidth()
      .nomeTalkBackSemantics(
        state = operationStateDescription(state.phase),
        liveRegionMode = liveRegion,
      ),
  )
}

@Composable
private fun failureDescription(state: NomeConnectionPreviewState): String =
  when (state.failureKind ?: ConnectionPreviewFailureKind.Other) {
    ConnectionPreviewFailureKind.Network ->
      androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_failure_network,
      )
    ConnectionPreviewFailureKind.InvalidLink ->
      androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_failure_invalid,
      )
    ConnectionPreviewFailureKind.UnsupportedLink ->
      androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_failure_unsupported,
      )
    ConnectionPreviewFailureKind.AlreadyExists ->
      androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_failure_already_exists,
        state.existingContactName.orEmpty(),
      )
    ConnectionPreviewFailureKind.ContextChanged ->
      androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_failure_context_changed,
      )
    ConnectionPreviewFailureKind.NoCurrentUser ->
      androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_failure_no_user,
      )
    ConnectionPreviewFailureKind.Other ->
      androidx.compose.ui.res.stringResource(
        R.string.nome_connection_preview_failure_other,
      )
  }

@Composable
private fun primaryActionLabel(
  phase: NomeConnectionPreviewPhase,
): String = androidx.compose.ui.res.stringResource(
  when (phase) {
    NomeConnectionPreviewPhase.Ready ->
      R.string.nome_connection_preview_continue
    NomeConnectionPreviewPhase.Connecting ->
      R.string.nome_connection_preview_connecting_title
    NomeConnectionPreviewPhase.Replanning ->
      R.string.nome_connection_preview_replanning_title
    NomeConnectionPreviewPhase.Pending ->
      R.string.nome_connection_preview_done
    NomeConnectionPreviewPhase.Failure ->
      R.string.nome_connection_preview_retry
  },
)

@Composable
private fun operationStateDescription(
  phase: NomeConnectionPreviewPhase,
): String = androidx.compose.ui.res.stringResource(
  when (phase) {
    NomeConnectionPreviewPhase.Ready ->
      R.string.nome_connection_preview_state_ready
    NomeConnectionPreviewPhase.Connecting ->
      R.string.nome_connection_preview_state_connecting
    NomeConnectionPreviewPhase.Replanning ->
      R.string.nome_connection_preview_state_replanning
    NomeConnectionPreviewPhase.Pending ->
      R.string.nome_connection_preview_state_pending
    NomeConnectionPreviewPhase.Failure ->
      R.string.nome_connection_preview_state_failure
  },
)
