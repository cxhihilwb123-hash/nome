package chat.simplex.common.views.chat.group

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformAddGroupMembersRoute(
  title: String,
  hasContacts: Boolean,
  onClose: () -> Unit,
  profileContent: @Composable () -> Unit,
  setupContent: @Composable () -> Unit,
  selectionFooterContent: @Composable () -> Unit,
  contactsContent: @Composable () -> Unit,
  emptyContent: @Composable () -> Unit,
  legacyContent: @Composable () -> Unit,
) = legacyContent()
