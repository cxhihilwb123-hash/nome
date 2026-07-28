package chat.simplex.common.views.usersettings.networkAndServers

import chat.simplex.common.model.ConditionsAcceptance
import chat.simplex.common.model.OperatorTag
import chat.simplex.common.model.ServerOperator
import chat.simplex.common.model.UsageConditionsDetail
import chat.simplex.common.model.UserOperatorServers
import chat.simplex.common.platform.AppPlatform
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NomeUsageInformationPolicyTest {
  @Test
  fun nomeOnlyManagedOperatorsUseLocalInformation() {
    val nome = UserOperatorServers.sampleData1

    assertTrue(shouldShowNomeUsageInformation(listOf(nome)))
    assertTrue(
      shouldShowNomeUsageInformation(
        listOf(nome, UserOperatorServers.sampleDataNilOperator),
      )
    )
  }

  @Test
  fun compatibilityOperatorsNeverUseNomeInformationAsTheirConditionsScreen() {
    val nome = UserOperatorServers.sampleData1
    val compatibility = nome.copy(
      operator = nome.operator!!.copy(operatorTag = OperatorTag.SimpleX),
    )
    val disabledNome = nome.copy(operator = nome.operator!!.copy(enabled = false))

    assertFalse(shouldShowNomeUsageInformation(listOf(compatibility)))
    assertFalse(shouldShowNomeUsageInformation(listOf(nome, compatibility)))
    assertFalse(shouldShowNomeUsageInformation(listOf(disabledNome)))
    assertFalse(shouldShowNomeUsageInformation(listOf(UserOperatorServers.sampleDataNilOperator)))
  }

  @Test
  fun desktopShowsOnlyNomeManagedOperatorWhileAndroidKeepsCompatibilityRows() {
    val nome = ServerOperator.sampleData1
    val simplex = nome.copy(operatorTag = OperatorTag.SimpleX, tradeName = "SimpleX")
    val flux = nome.copy(operatorTag = OperatorTag.Flux, tradeName = "Flux")
    val untagged = nome.copy(operatorTag = null, tradeName = "Other")

    assertTrue(shouldShowManagedOperator(AppPlatform.DESKTOP, nome))
    listOf(simplex, flux, untagged).forEach { operator ->
      assertFalse(shouldShowManagedOperator(AppPlatform.DESKTOP, operator))
      assertTrue(shouldShowManagedOperator(AppPlatform.ANDROID, operator))
    }
    assertFalse(shouldShowManagedOperator(AppPlatform.DESKTOP, null))
    assertFalse(shouldShowManagedOperator(AppPlatform.ANDROID, null))
  }

  @Test
  fun desktopNetworkInformationCanNeverRouteToLegacyConditions() {
    val nome = UserOperatorServers.sampleData1
    val upstream = nome.copy(
      operator = nome.operator!!.copy(operatorTag = OperatorTag.SimpleX),
    )
    val serverSets = listOf(
      emptyList(),
      listOf(nome),
      listOf(upstream),
      listOf(nome, upstream, UserOperatorServers.sampleDataNilOperator),
    )

    serverSets.forEach { servers ->
      assertEquals(
        OperatorInformationDestination.NomeLocalInformation,
        operatorInformationDestination(
          platform = AppPlatform.DESKTOP,
          userServers = servers,
          hasLegacyConditionsAction = true,
          anyOperatorEnabled = true,
        ),
      )
    }
  }

  @Test
  fun androidNetworkInformationKeepsLegacyCompatibilityRouting() {
    val nome = UserOperatorServers.sampleData1
    val upstream = nome.copy(
      operator = nome.operator!!.copy(operatorTag = OperatorTag.SimpleX),
    )

    assertEquals(
      OperatorInformationDestination.NomeLocalInformation,
      operatorInformationDestination(
        platform = AppPlatform.ANDROID,
        userServers = listOf(nome),
        hasLegacyConditionsAction = true,
        anyOperatorEnabled = true,
      ),
    )
    assertEquals(
      OperatorInformationDestination.LegacyConditions,
      operatorInformationDestination(
        platform = AppPlatform.ANDROID,
        userServers = listOf(upstream),
        hasLegacyConditionsAction = true,
        anyOperatorEnabled = true,
      ),
    )
    assertEquals(
      OperatorInformationDestination.None,
      operatorInformationDestination(
        platform = AppPlatform.ANDROID,
        userServers = listOf(upstream),
        hasLegacyConditionsAction = false,
        anyOperatorEnabled = true,
      ),
    )
  }

  @Test
  fun desktopOperatorEnablePolicyIsNomeOnlyAndNeverAcceptsConditions() {
    val acceptedNome = ServerOperator.sampleData1
    val requiredNome = acceptedNome.copy(
      enabled = false,
      conditionsAcceptance = ConditionsAcceptance.Required(deadline = null),
    )
    val scheduledNome = requiredNome.copy(
      conditionsAcceptance = ConditionsAcceptance.Required(
        deadline = UsageConditionsDetail.sampleData.createdAt,
      ),
    )

    assertEquals(
      OperatorEnableDestination.Enable,
      operatorEnableDestination(AppPlatform.DESKTOP, acceptedNome),
    )
    listOf(requiredNome, scheduledNome).forEach { operator ->
      assertEquals(
        OperatorEnableDestination.NomeLocalInformation,
        operatorEnableDestination(AppPlatform.DESKTOP, operator),
      )
    }
    listOf(OperatorTag.SimpleX, OperatorTag.Flux).forEach { tag ->
      assertEquals(
        OperatorEnableDestination.Ignore,
        operatorEnableDestination(AppPlatform.DESKTOP, acceptedNome.copy(operatorTag = tag)),
      )
    }
    assertEquals(
      OperatorEnableDestination.Ignore,
      operatorEnableDestination(AppPlatform.DESKTOP, acceptedNome.copy(operatorTag = null)),
    )
    assertEquals(
      OperatorEnableDestination.Ignore,
      operatorEnableDestination(AppPlatform.DESKTOP, null),
    )
  }

  @Test
  fun androidOperatorEnablePolicyKeepsLegacyCompatibilityBehavior() {
    val accepted = ServerOperator.sampleData1.copy(operatorTag = OperatorTag.SimpleX)
    val reviewNow = accepted.copy(
      conditionsAcceptance = ConditionsAcceptance.Required(deadline = null),
    )
    val reviewLater = accepted.copy(
      conditionsAcceptance = ConditionsAcceptance.Required(
        deadline = UsageConditionsDetail.sampleData.createdAt,
      ),
    )

    assertEquals(
      OperatorEnableDestination.Enable,
      operatorEnableDestination(AppPlatform.ANDROID, accepted),
    )
    assertEquals(
      OperatorEnableDestination.LegacyConditions,
      operatorEnableDestination(AppPlatform.ANDROID, reviewNow),
    )
    assertEquals(
      OperatorEnableDestination.Enable,
      operatorEnableDestination(AppPlatform.ANDROID, reviewLater),
    )
  }

  @Test
  fun nomeOnboardingAndLocalInformationContainNoUpstreamLegalDestination() {
    val onboarding = source(
      "apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/onboarding/ChooseServerOperators.kt"
    )
    val localInformation = source(
      "apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/networkAndServers/NomeUsageInformationView.kt"
    )
    val forbidden = listOf(
      "github.com/simplex-chat",
      "PRIVACY.md",
      "SimpleConditionsView",
      "UsageConditionsView",
      "ConditionsLinkButton",
      "defaultConditionsLink",
      "SimpleX Chat Ltd",
      "chat@simplex.chat",
      "smp6.simplex.im",
    )

    forbidden.forEach { token ->
      assertFalse(onboarding.contains(token), "Nome onboarding still references $token")
      assertFalse(localInformation.contains(token), "Local Nome information still references $token")
    }

    val baseStrings = source(
      "apps/multiplatform/common/src/commonMain/resources/MR/base/strings.xml"
    )
    assertTrue(baseStrings.contains("It is not a legal agreement and does not record legal consent."))
  }

  @Test
  fun nomeOnlyNetworkEntryAndClientNoticeStayLocal() {
    val networkSettings = source(
      "apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/networkAndServers/NetworkAndServers.kt"
    )
    val nomeButton = networkSettings
      .substringAfter("fun NomeUsageInformationButton()")
      .substringBefore("fun LegacyConditionsButton")
    assertTrue(nomeButton.contains("NomeUsageInformationView()"))
    assertFalse(nomeButton.contains("UsageConditionsView"))
    assertFalse(nomeButton.contains("ConditionsLinkButton"))
    assertTrue(networkSettings.contains("operatorInformationDestination("))
    assertTrue(networkSettings.contains("shouldShowManagedOperator(appPlatform, operator)"))

    val customServersEntry = networkSettings
      .substringAfter("val nullOperatorIndex")
      .substringBefore("val saveDisabled")
    assertTrue(customServersEntry.contains("it.operator == null"))
    assertTrue(customServersEntry.contains("YourServersView("))
    assertFalse(customServersEntry.contains("shouldShowManagedOperator"))

    val operatorView = source(
      "apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/usersettings/networkAndServers/OperatorView.kt"
    )
    val enableToggle = operatorView
      .substringAfter("private fun UseOperatorToggle(")
      .substringBefore("private fun SingleOperatorUsageConditionsView(")
    val desktopNomeInformationBranch = enableToggle
      .substringAfter("OperatorEnableDestination.NomeLocalInformation")
      .substringBefore("OperatorEnableDestination.LegacyConditions")
    assertTrue(enableToggle.contains("operatorEnableDestination(appPlatform, operator)"))
    assertTrue(desktopNomeInformationBranch.contains("NomeUsageInformationView()"))
    assertFalse(desktopNomeInformationBranch.contains("changeOperatorEnabled"))
    assertFalse(desktopNomeInformationBranch.contains("ConditionsLinkButton"))
    assertFalse(desktopNomeInformationBranch.contains("SingleOperatorUsageConditionsView"))

    val usageConditionsGuard = networkSettings
      .substringAfter("fun UsageConditionsView(")
      .substringBefore("suspend fun acceptForOperators")
    assertTrue(usageConditionsGuard.contains("if (appPlatform.isDesktop)"))
    assertTrue(usageConditionsGuard.contains("NomeUsageInformationView()"))
    assertTrue(usageConditionsGuard.contains("return"))

    val singleOperatorGuard = operatorView
      .substringAfter("private fun SingleOperatorUsageConditionsView(")
      .substringBefore("val operatorsWithConditionsAccepted")
    assertTrue(singleOperatorGuard.contains("if (appPlatform.isDesktop)"))
    assertTrue(singleOperatorGuard.contains("NomeUsageInformationView()"))
    assertTrue(singleOperatorGuard.contains("return"))

    val conditionsLinkGuard = operatorView
      .substringAfter("fun ConditionsLinkButton()")
      .substringBefore("val showMenu")
    assertTrue(conditionsLinkGuard.contains("if (appPlatform.isDesktop) return"))

    val conditionsTextGuard = operatorView
      .substringAfter("fun ConditionsTextView(")
      .substringBefore("val conditionsData")
    assertTrue(conditionsTextGuard.contains("if (appPlatform.isDesktop)"))
    assertTrue(conditionsTextGuard.contains("NomeUsageInformationView()"))
    assertTrue(conditionsTextGuard.contains("return"))

    val api = source(
      "apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/model/SimpleXAPI.kt"
    )
    val clientNotice = api.substringAfter("fun showClientNoticeAlert")
    assertTrue(clientNotice.contains("serverHostname(server)"))
    assertFalse(clientNotice.contains("defaultConditionsLink"))
    assertFalse(clientNotice.contains("contentModerationPostLink"))
    assertFalse(clientNotice.contains("openUri"))
  }

  @Test
  fun nomeCoreConditionsStateIsMarkedAsAutomaticCompatibility() {
    val profiles = source("src/Simplex/Chat/Store/Profiles.hs")
    val nomeBranch = profiles
      .substringAfter("operatorTag = Just OTNome")
      .substringBefore("getOperatorConditions_ db")

    assertTrue(nomeBranch.contains("CAAccepted Nothing True"))
    assertFalse(nomeBranch.contains("CAAccepted Nothing False"))
  }

  private fun source(relativePath: String): String =
    Files.readString(repositoryRoot().resolve(relativePath))

  private fun repositoryRoot(): Path {
    val start = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
    return generateSequence(start) { it.parent }
      .firstOrNull {
        Files.isRegularFile(
          it.resolve(
            "apps/multiplatform/common/src/commonMain/kotlin/chat/simplex/common/views/onboarding/ChooseServerOperators.kt"
          )
        )
      }
      ?: error("Unable to locate repository root from $start")
  }
}
