package chat.simplex.common.model

/**
 * Identifies the two disconnected contact cards inserted by the legacy Android core.
 *
 * Matching the full upstream profile and link keeps this migration deliberately narrow: a user
 * contact with the same display name is never removed, and any card with a connection, message,
 * group membership or contact request is preserved.
 */
internal object NomeLegacySeedContacts {
  internal data class Candidate(
    val displayName: String,
    val shortDescription: String?,
    val contactLink: String?,
    val hasConnection: Boolean = false,
    val hasMessages: Boolean = false,
    val hasGroupMembership: Boolean = false,
    val hasContactRequest: Boolean = false,
  )

  internal const val teamDisplayName = "Ask SimpleX Team"
  internal const val teamShortDescription = "Send questions about SimpleX Chat app and your suggestions"
  internal const val teamContactLink =
    "simplex:/contact#/?v=1&smp=smp%3A%2F%2FPQUV2eL0t7OStZOoAsPEV2QYWt4-xilbakvGUGOItUo%3D%40smp6.simplex.im%2FK1rslx-m5bpXVIdMZg9NLUZ_8JBm8xTt%23MCowBQYDK2VuAyEALDeVe-sG8mRY22LsXlPgiwTNs9dbiLrNuA7f3ZMAJ2w%3D"

  internal const val statusDisplayName = "SimpleX Status"
  internal const val statusShortDescription = "Automatic server status and app release updates"
  internal const val statusContactLink =
    "simplex:/contact/#/?v=1-2&smp=smp%3A%2F%2Fu2dS9sG8nMNURyZwqASV4yROM28Er0luVTx5X1CsMrU%3D%40smp4.simplex.im%2FShQuD-rPokbDvkyotKx5NwM8P3oUXHxA%23%2F%3Fv%3D1-2%26dh%3DMCowBQYDK2VuAyEA6fSx1k9zrOmF0BJpCaTarZvnZpMTAVQhd3RkDQ35KT0%253D%26srv%3Do5vmywmrnaxalvz6wi3zicyftgio6psuvyniis6gco6bp6ekl4cqj4id.onion"

  private val teamLinkIdentity = listOf(
    "smp6.simplex.im",
    "PQUV2eL0t7OStZOoAsPEV2QYWt4-xilbakvGUGOItUo",
    "K1rslx-m5bpXVIdMZg9NLUZ_8JBm8xTt",
  )
  private val statusLinkIdentity = listOf(
    "smp4.simplex.im",
    "u2dS9sG8nMNURyZwqASV4yROM28Er0luVTx5X1CsMrU",
    "ShQuD-rPokbDvkyotKx5NwM8P3oUXHxA",
  )

  internal fun shouldRemove(chat: Chat): Boolean {
    val contact = (chat.chatInfo as? ChatInfo.Direct)?.contact ?: return false
    val profile = contact.profile
    return shouldRemove(
      Candidate(
        displayName = profile.displayName,
        shortDescription = profile.shortDescr,
        contactLink = profile.contactLink,
        hasConnection = contact.activeConn != null,
        hasMessages = chat.chatItems.isNotEmpty(),
        hasGroupMembership = contact.viaGroup != null || contact.contactGroupMemberId != null,
        hasContactRequest = contact.contactRequestId != null,
      )
    )
  }

  internal fun shouldRemove(candidate: Candidate): Boolean {
    if (
      candidate.hasConnection ||
      candidate.hasMessages ||
      candidate.hasGroupMembership ||
      candidate.hasContactRequest
    ) return false

    return matches(
      candidate.displayName,
      candidate.shortDescription,
      candidate.contactLink,
      teamDisplayName,
      teamShortDescription,
      teamContactLink,
      teamLinkIdentity,
    ) || matches(
      candidate.displayName,
      candidate.shortDescription,
      candidate.contactLink,
      statusDisplayName,
      statusShortDescription,
      statusContactLink,
      statusLinkIdentity,
    )
  }

  private fun matches(
    displayName: String,
    shortDescription: String?,
    contactLink: String?,
    expectedDisplayName: String,
    expectedShortDescription: String,
    expectedContactLink: String,
    expectedLinkIdentity: List<String>,
  ): Boolean =
    displayName == expectedDisplayName &&
      shortDescription == expectedShortDescription &&
      contactLink != null &&
      (
        contactLink == expectedContactLink ||
          expectedLinkIdentity.all(contactLink::contains)
      )
}
