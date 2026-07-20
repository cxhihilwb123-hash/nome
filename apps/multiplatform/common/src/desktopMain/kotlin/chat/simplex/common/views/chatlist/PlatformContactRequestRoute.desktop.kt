package chat.simplex.common.views.chatlist

import chat.simplex.common.model.User

internal actual fun showPlatformContactRequestRoute(
  requestName: String,
  requestFullName: String,
  requestImage: String?,
  currentUser: User?,
  canAcceptIncognito: Boolean,
  onAccept: suspend (incognito: Boolean) -> Boolean,
  onReject: suspend () -> Boolean,
): Boolean = false
