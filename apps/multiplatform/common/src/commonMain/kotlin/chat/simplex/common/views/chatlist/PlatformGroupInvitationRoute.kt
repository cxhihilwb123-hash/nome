package chat.simplex.common.views.chatlist

import chat.simplex.common.model.GroupInfo

/**
 * Android presentation seam for an existing group invitation.
 *
 * The caller retains join/delete command and model-mutation ownership. Android may show the P16
 * page; Desktop returns false and keeps the official alert.
 */
internal expect fun showPlatformGroupInvitationRoute(
  groupInfo: GroupInfo,
  inviterName: String?,
  inviterVerified: Boolean,
  onJoin: suspend () -> Boolean,
  onDelete: suspend () -> Boolean,
): Boolean
