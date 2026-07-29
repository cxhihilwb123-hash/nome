package chat.simplex.common.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NomeLegacySeedContactsTest {
  @Test
  fun removesOnlyExactDisconnectedLegacyCards() {
    assertTrue(
      NomeLegacySeedContacts.shouldRemove(
        legacyCandidate(
          NomeLegacySeedContacts.teamDisplayName,
          NomeLegacySeedContacts.teamShortDescription,
          NomeLegacySeedContacts.teamContactLink,
        )
      )
    )
    assertTrue(
      NomeLegacySeedContacts.shouldRemove(
        legacyCandidate(
          NomeLegacySeedContacts.statusDisplayName,
          NomeLegacySeedContacts.statusShortDescription,
          NomeLegacySeedContacts.statusContactLink,
        )
      )
    )
  }

  @Test
  fun preservesSameNameWithDifferentContactLink() {
    assertFalse(
      NomeLegacySeedContacts.shouldRemove(
        legacyCandidate(
          NomeLegacySeedContacts.teamDisplayName,
          NomeLegacySeedContacts.teamShortDescription,
          "simplex:/contact#/?user-created",
        )
      )
    )
  }

  @Test
  fun acceptsCanonicalizedFormOfExactLegacyLink() {
    assertTrue(
      NomeLegacySeedContacts.shouldRemove(
        legacyCandidate(
          NomeLegacySeedContacts.teamDisplayName,
          NomeLegacySeedContacts.teamShortDescription,
          "simplex:/contact#/?v=1&smp=smp://PQUV2eL0t7OStZOoAsPEV2QYWt4-xilbakvGUGOItUo=@smp6.simplex.im/K1rslx-m5bpXVIdMZg9NLUZ_8JBm8xTt",
        )
      )
    )
  }

  @Test
  fun preservesConnectedOrUsedLegacyCards() {
    val seed = legacyCandidate(
      NomeLegacySeedContacts.teamDisplayName,
      NomeLegacySeedContacts.teamShortDescription,
      NomeLegacySeedContacts.teamContactLink,
    )
    assertFalse(
      NomeLegacySeedContacts.shouldRemove(seed.copy(hasConnection = true))
    )
    assertFalse(
      NomeLegacySeedContacts.shouldRemove(seed.copy(hasMessages = true))
    )
    assertFalse(
      NomeLegacySeedContacts.shouldRemove(seed.copy(hasGroupMembership = true))
    )
    assertFalse(
      NomeLegacySeedContacts.shouldRemove(seed.copy(hasContactRequest = true))
    )
  }

  private fun legacyCandidate(
    displayName: String,
    shortDescription: String,
    contactLink: String,
  ): NomeLegacySeedContacts.Candidate =
    NomeLegacySeedContacts.Candidate(
      displayName = displayName,
      shortDescription = shortDescription,
      contactLink = contactLink,
    )
}
