package chat.simplex.common.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NomeLinkBrandingTest {
  @Test
  fun shareableProtocolLinksUseNomeOrigin() {
    assertEquals(
      "https://nome.im/contact#opaque",
      simplexChatLink("simplex:/contact#opaque"),
    )
    assertEquals(
      "https://nome.im/file#opaque",
      simplexChatLink("simplex:/file#opaque"),
    )
    assertEquals(
      "https://smp.nome.im/c#opaque",
      simplexChatLink("simplex:/c#opaque"),
    )
  }

  @Test
  fun legacyShareableLinksAreRebrandedWithoutRewritingDocumentation() {
    assertEquals(
      "https://nome.im/invitation#opaque",
      simplexChatLink("https://simplex.chat/invitation#opaque"),
    )
    assertEquals(
      "https://smp6.simplex.im/a#opaque",
      simplexChatLink("https://smp6.simplex.im/a#opaque"),
    )
    assertEquals(
      "https://smp.nome.im/r#opaque",
      simplexChatLink("https://smp.nome.im/r#opaque"),
    )
    assertEquals(
      "https://simplex.chat/docs/guide.html",
      simplexChatLink("https://simplex.chat/docs/guide.html"),
    )
  }

  @Test
  fun nomeLinksRoundTripToNativeProtocolForm() {
    assertEquals(
      "simplex:/contact#opaque",
      normalizeNomeChatLink("https://nome.im/contact#opaque"),
    )
    assertEquals(
      "https://nome.im/docs/guide.html",
      normalizeNomeChatLink("https://nome.im/docs/guide.html"),
    )
    assertEquals(
      "simplex:/file#legacy",
      normalizeNomeChatLink("http://simplex.chat/file#legacy"),
    )
    assertEquals(
      "https://smp6.simplex.im/a#hosted",
      normalizeNomeChatLink("https://smp6.simplex.im/a#hosted"),
    )
    assertEquals(
      "https://smp.nome.im/i#hosted",
      normalizeNomeChatLink("https://nome.im/i#hosted"),
    )
    assertEquals(
      "https://smp.nome.im/c#channel",
      normalizeNomeChatLink("https://smp.nome.im/c#channel"),
    )
  }

  @Test
  fun onlyKnownNomeChatPathsAreIntercepted() {
    assertTrue(isNomePublicChatLink("https://nome.im/contact#opaque"))
    assertTrue(isNomePublicChatLink("https://nome.im/file#opaque"))
    assertTrue(isNomePublicChatLink("https://nome.im/c#opaque"))
    assertFalse(isNomePublicChatLink("https://nome.im/docs/guide.html"))
    assertFalse(isNomePublicChatLink("https://nome.im/fileevil#opaque"))
    assertFalse(isNomePublicChatLink("https://nome.im/contact/foo"))
    assertFalse(isNomePublicChatLink("https://nome.im/file/bar"))
  }

  @Test
  fun recognizedPublicLinksCoverLegacyInputsWithoutTrustingSitePages() {
    assertTrue(isRecognizedPublicChatLink("https://nome.im/contact#opaque"))
    assertTrue(isRecognizedPublicChatLink("https://simplex.chat/contact#opaque"))
    assertTrue(isRecognizedPublicChatLink("http://simplex.chat/file#opaque"))
    assertTrue(isRecognizedPublicChatLink("https://smp6.simplex.im/a#opaque"))
    assertTrue(isRecognizedPublicChatLink("https://smp.nome.im/c#opaque"))
    assertFalse(isRecognizedPublicChatLink("https://simplex.chat/docs/guide.html"))
    assertFalse(isRecognizedPublicChatLink("https://nome.im/contact/foo"))

    assertTrue(isRecognizedPublicFileLink("simplex:/file#opaque"))
    assertTrue(isRecognizedPublicFileLink("https://nome.im/file#opaque"))
    assertTrue(isRecognizedPublicFileLink("http://simplex.chat/file#opaque"))
    assertFalse(isRecognizedPublicFileLink("https://nome.im/file/bar"))
  }

  @Test
  fun createdConnectionLinkNeverPresentsLegacyOrigin() {
    val link = CreatedConnLink(
      connFullLink = "simplex:/contact#full",
      connShortLink = "https://simplex.chat/contact#short",
    )

    assertEquals("https://nome.im/contact#full", link.simplexChatUri(short = false))
    assertEquals("https://nome.im/contact#short", link.simplexChatUri(short = true))
  }

  @Test
  fun hostedNomeShortLinksKeepTheirResolverHost() {
    val link = CreatedConnLink(
      connFullLink = "simplex:/contact#full",
      connShortLink = "https://smp.nome.im/i#short",
    )

    assertEquals("https://smp.nome.im/i#short", link.simplexChatUri(short = true))
    assertEquals(
      "https://smp.nome.im/i#short",
      normalizeNomeChatLink(link.simplexChatUri(short = true)),
    )
  }
}
