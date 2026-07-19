package chat.simplex.app.nome

import org.junit.Assert.assertEquals
import org.junit.Test

class NomeLocalePolicyTest {
  @Test
  fun cleanInstallWithoutDataInitializesSimplifiedChineseOnce() {
    assertEquals(
      NomeLocaleDecision.INITIALIZE_SIMPLIFIED_CHINESE,
      decideNomeLocale(
        evidence(
          sameInstallAndUpdateTime = true,
          databasePresentBeforeInitialization = false,
          upstreamPreferencesPresent = false,
        ),
      ),
    )
  }

  @Test
  fun updateOrExistingDataConservativelyPreservesFollowSystem() {
    assertEquals(
      NomeLocaleDecision.PRESERVE_EXISTING_OR_UNKNOWN,
      decideNomeLocale(evidence(sameInstallAndUpdateTime = false)),
    )
    assertEquals(
      NomeLocaleDecision.PRESERVE_EXISTING_OR_UNKNOWN,
      decideNomeLocale(
        evidence(
          sameInstallAndUpdateTime = true,
          databasePresentBeforeInitialization = true,
        ),
      ),
    )
    assertEquals(
      NomeLocaleDecision.PRESERVE_EXISTING_OR_UNKNOWN,
      decideNomeLocale(
        evidence(
          sameInstallAndUpdateTime = true,
          upstreamPreferencesPresent = true,
        ),
      ),
    )
  }

  @Test
  fun explicitLanguageAndExistingMarkerAreNeverOverwrittenAcrossLaterRuns() {
    assertEquals(
      NomeLocaleDecision.PRESERVE_EXPLICIT,
      decideNomeLocale(evidence(explicitLanguage = "en")),
    )
    assertEquals(
      NomeLocaleDecision.ALREADY_INITIALIZED,
      decideNomeLocale(
        evidence(
          markerVersion = NOME_LOCALE_POLICY_VERSION,
          explicitLanguage = null,
          sameInstallAndUpdateTime = true,
        ),
      ),
    )
    assertEquals(
      NomeLocaleDecision.ALREADY_INITIALIZED,
      decideNomeLocale(
        evidence(
          markerVersion = NOME_LOCALE_POLICY_VERSION,
          explicitLanguage = "en",
          sameInstallAndUpdateTime = false,
          databasePresentBeforeInitialization = true,
          upstreamPreferencesPresent = true,
        ),
      ),
    )
  }

  @Test
  fun preInitializationEvidenceCannotBeReinterpretedAsCleanAfterStartup() {
    val existingUserEvidence = NomeLocalePreInitializationEvidence(
      markerVersion = 0,
      sameInstallAndUpdateTime = true,
      databasePresent = true,
      upstreamPreferencesPresent = false,
    )

    assertEquals(
      NomeLocaleDecision.PRESERVE_EXISTING_OR_UNKNOWN,
      decideNomeLocale(
        NomeLocaleEvidence(
          markerVersion = existingUserEvidence.markerVersion,
          explicitLanguage = null,
          sameInstallAndUpdateTime = existingUserEvidence.sameInstallAndUpdateTime,
          databasePresentBeforeInitialization = existingUserEvidence.databasePresent,
          upstreamPreferencesPresent = existingUserEvidence.upstreamPreferencesPresent,
        ),
      ),
    )
  }

  @Test
  fun v656LanguagesAreRecognizedWithoutTreatingCorruptValuesAsSupported() {
    assertEquals(true, isKnownNomeUpstreamLocale("zh-CN"))
    assertEquals(true, isKnownNomeUpstreamLocale("en"))
    assertEquals(true, isKnownNomeUpstreamLocale("pt-BR"))
    assertEquals(false, isKnownNomeUpstreamLocale(""))
    assertEquals(false, isKnownNomeUpstreamLocale("not_a_locale"))
  }

  private fun evidence(
    markerVersion: Int = 0,
    explicitLanguage: String? = null,
    sameInstallAndUpdateTime: Boolean = false,
    databasePresentBeforeInitialization: Boolean = false,
    upstreamPreferencesPresent: Boolean = false,
  ) = NomeLocaleEvidence(
    markerVersion = markerVersion,
    explicitLanguage = explicitLanguage,
    sameInstallAndUpdateTime = sameInstallAndUpdateTime,
    databasePresentBeforeInitialization = databasePresentBeforeInitialization,
    upstreamPreferencesPresent = upstreamPreferencesPresent,
  )
}
