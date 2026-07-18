package chat.simplex.common.ui.nome.home

import chat.simplex.common.model.Chat
import chat.simplex.common.model.ChatInfo

data class NomeSearchResults(
  val contacts: List<Chat>,
  val groups: List<Chat>,
  val channels: List<Chat>,
  val notes: List<Chat>,
) {
  val ordered: List<Chat>
    get() = contacts + groups + channels + notes

  val isEmpty: Boolean
    get() = ordered.isEmpty()
}

/**
 * Deterministic presentation grouping for the official chat-list search result.
 *
 * This adapter does not search messages, persist queries, or create result
 * facts. Its input must already come from the v6.5.6 local chat-list filter.
 */
fun classifyNomeSearchResults(matches: List<Chat>): NomeSearchResults {
  val contacts = mutableListOf<Chat>()
  val groups = mutableListOf<Chat>()
  val channels = mutableListOf<Chat>()
  val notes = mutableListOf<Chat>()

  matches.forEach { chat ->
    when (val info = chat.chatInfo) {
      is ChatInfo.Direct,
      is ChatInfo.ContactRequest,
      is ChatInfo.ContactConnection -> contacts += chat
      is ChatInfo.Group ->
        if (info.groupInfo.isChannel) {
          channels += chat
        } else {
          groups += chat
        }
      is ChatInfo.Local -> notes += chat
      is ChatInfo.InvalidJSON -> Unit
    }
  }

  return NomeSearchResults(
    contacts = contacts,
    groups = groups,
    channels = channels,
    notes = notes,
  )
}
