package chat.simplex.common.views.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import chat.simplex.common.model.NotificationsMode
import chat.simplex.common.model.ServerOperator

@Composable
internal actual fun PlatformNomeWelcomePage(
  enabled: Boolean,
  onCreate: () -> Unit,
  onMigrate: () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()

@Composable
internal actual fun PlatformNomeCreateIdentityPage(
  displayName: MutableState<String>,
  createEnabled: Boolean,
  creating: Boolean,
  onBack: () -> Unit,
  onCreate: () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()

@Composable
internal actual fun PlatformNomeNetworkPage(
  serverOperators: State<List<ServerOperator>>,
  selectedOperatorIds: MutableState<Set<Long>>,
  notificationMode: MutableState<NotificationsMode>,
  onConfigureOperators: () -> Unit,
  onConfigureNotifications: () -> Unit,
  onContinue: () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()

@Composable
internal actual fun PlatformNomeCommitmentPage(
  acceptEnabled: Boolean,
  onBack: () -> Unit,
  onViewTerms: () -> Unit,
  onAccept: () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()
