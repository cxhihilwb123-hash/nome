package chat.simplex.common.views.chat.group

import androidx.compose.runtime.Composable

/**
 * Android presentation seam for the official group/channel information owner.
 *
 * The common route retains member loading and moderation, profile/preferences/link routes,
 * notification/receipt/retention settings, search, and destructive confirmations. Android adds
 * only the Nome full-page frame and compact card density. Desktop renders [legacyContent]
 * unchanged.
 */
@Composable
internal expect fun PlatformGroupChatInfoRoute(
  title: String,
  onClose: () -> Unit,
  content: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
)
