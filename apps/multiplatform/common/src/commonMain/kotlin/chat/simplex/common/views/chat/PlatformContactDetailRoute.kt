package chat.simplex.common.views.chat

import androidx.compose.runtime.Composable

/**
 * Android presentation seam for the official direct-contact detail route.
 *
 * The common owner keeps every contact, preference, notification, call, retention, network, and
 * destructive callback. Android may reorganize those controls to match the Nome page language;
 * Desktop renders [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformContactDetailRoute(
  title: String,
  onClose: () -> Unit,
  headerContent: @Composable () -> Unit,
  quickActionsContent: @Composable () -> Unit,
  detailsContent: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
)
