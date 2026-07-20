package chat.simplex.common.views.chatlist

import chat.simplex.common.model.User

/**
 * Android presentation seam for the official contact-request actions.
 *
 * The caller retains request/model mutation and typed command-result ownership. Android may show
 * the P14 page; Desktop returns false and keeps the official alert.
 */
internal expect fun showPlatformContactRequestRoute(
  requestName: String,
  requestFullName: String,
  requestImage: String?,
  currentUser: User?,
  canAcceptIncognito: Boolean,
  onAccept: suspend (incognito: Boolean) -> Boolean,
  onReject: suspend () -> Boolean,
): Boolean
