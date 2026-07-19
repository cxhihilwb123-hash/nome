package chat.simplex.common.views.chatlist

import chat.simplex.common.model.ChatInfo
import chat.simplex.common.model.User

internal actual fun showPlatformContactRequestRoute(
  contactRequest: ChatInfo.ContactRequest,
  currentUser: User?,
  canAcceptIncognito: Boolean,
  onAccept: suspend (incognito: Boolean) -> Boolean,
  onReject: suspend () -> Boolean,
): Boolean = false
