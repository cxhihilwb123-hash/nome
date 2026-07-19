package chat.simplex.common.views.chat.group

import androidx.compose.runtime.Composable

/**
 * Android presentation seam for the official invite-members route.
 *
 * Contact eligibility, selection, roles, admission/preferences routes, command submission, and
 * completion remain owned by the common v6.5.6 flow. Desktop renders [legacyContent] unchanged.
 */
@Composable
internal expect fun PlatformAddGroupMembersRoute(
  title: String,
  hasContacts: Boolean,
  onClose: () -> Unit,
  profileContent: @Composable () -> Unit,
  setupContent: @Composable () -> Unit,
  selectionFooterContent: @Composable () -> Unit,
  contactsContent: @Composable () -> Unit,
  emptyContent: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
)
