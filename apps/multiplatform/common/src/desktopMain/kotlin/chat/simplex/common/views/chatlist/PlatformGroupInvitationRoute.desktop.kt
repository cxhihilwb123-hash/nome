package chat.simplex.common.views.chatlist

import chat.simplex.common.model.GroupInfo

internal actual fun showPlatformGroupInvitationRoute(
  groupInfo: GroupInfo,
  inviterName: String?,
  inviterVerified: Boolean,
  onJoin: suspend () -> Boolean,
  onDelete: suspend () -> Boolean,
): Boolean = false
