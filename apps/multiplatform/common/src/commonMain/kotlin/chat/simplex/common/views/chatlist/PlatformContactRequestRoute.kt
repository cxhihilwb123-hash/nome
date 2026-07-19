package chat.simplex.common.views.chatlist

import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.User

/**
 * Android presentation seam for the official contact-request actions.
 *
 * The caller retains request/model mutation and typed command-result ownership. Android may show
 * the P14 page; Desktop returns false and keeps the official alert.
 */
internal expect fun showPlatformContactRequestRoute(
  contactRequest: ChatInfo.ContactRequest,
  currentUser: User?,
  canAcceptIncognito: Boolean,
  onAccept: suspend (incognito: Boolean) -> Boolean,
  onReject: suspend () -> Boolean,
): Boolean
