package chat.simplex.common.views.chat

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformContactDetailRoute(
  title: String,
  onClose: () -> Unit,
  headerContent: @Composable () -> Unit,
  quickActionsContent: @Composable () -> Unit,
  detailsContent: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()
