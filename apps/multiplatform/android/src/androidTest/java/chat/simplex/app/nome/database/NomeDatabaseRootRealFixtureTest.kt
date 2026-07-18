package chat.simplex.app.nome.database

import android.util.Base64
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import chat.simplex.app.MainActivity
import chat.simplex.common.R
import chat.simplex.common.model.ChatModel
import chat.simplex.common.platform.agentDatabaseFileName
import chat.simplex.common.platform.chatDatabaseFileName
import chat.simplex.common.platform.cryptor
import chat.simplex.common.platform.dataDir
import chat.simplex.common.platform.initChatController
import chat.simplex.common.views.database.AndroidDatabaseKeyReadState
import chat.simplex.common.views.database.clearPlatformDatabaseKeyReadState
import chat.simplex.common.views.database.platformDatabaseKeyReadState
import chat.simplex.common.views.database.stopChatAsync
import chat.simplex.common.views.helpers.DBMigrationResult
import chat.simplex.common.views.helpers.DatabaseUtils
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NomeDatabaseRootRealFixtureTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun controlledRandomManualAndBackupRecoveryPath() {
    val arguments = InstrumentationRegistry.getArguments()
    if (arguments.getString(REAL_FIXTURE_ARGUMENT) != "true") return

    val target = InstrumentationRegistry.getInstrumentation().targetContext
    val fixtureKey = randomFixtureKey()
    val wrongKey = randomFixtureKey()
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      waitForDatabaseOk()
      assertTrue(ChatModel.controller.appPrefs.initialRandomDBPassphrase.get())
      val randomKey = requireNotNull(DatabaseUtils.ksDatabasePassword.get())

      verifyRandomMissingAlias(randomKey, target)
      verifyRandomUnreadableMaterial(randomKey, target)
      seedManualFixture(randomKey, fixtureKey)
      verifyManualMissingAliasAndLifecycle(
        fixtureKey = fixtureKey,
        wrongKey = wrongKey,
        target = target,
        scenario = scenario,
      )
      verifyManualUnreadableMaterial(fixtureKey, target)
      verifyMatchedBackupCopyAndFreshOpen(fixtureKey, target)
    } finally {
      scenario.close()
    }
  }

  private fun verifyRandomMissingAlias(
    randomKey: String,
    target: android.content.Context,
  ) {
    stopController()
    cryptor.deleteKey(DATABASE_KEY_ALIAS)
    clearPlatformDatabaseKeyReadState()
    ChatModel.chatDbStatus.value = null

    runBlocking { initChatController() }

    assertTrue(ChatModel.chatDbStatus.value is DBMigrationResult.ErrorNotADatabase)
    val keyState = platformDatabaseKeyReadState()
    assertTrue(keyState is AndroidDatabaseKeyReadState.MissingAlias)
    assertTrue(requireNotNull(keyState).initialRandomDBPassphrase)
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_random_key_title),
    ).assertIsDisplayed()
    passwordNode().assertDoesNotExist()

    clearPlatformDatabaseKeyReadState()
    runBlocking { initChatController(useKey = randomKey) }
    waitForDatabaseOk()
    DatabaseUtils.ksDatabasePassword.set(randomKey)
  }

  private fun verifyRandomUnreadableMaterial(
    randomKey: String,
    target: android.content.Context,
  ) {
    stopController()
    val prefs = ChatModel.controller.appPrefs
    val originalIv = requireNotNull(
      prefs.initializationVectorDBPassphrase.get(),
    )
    prefs.initializationVectorDBPassphrase.set(invalidFixtureIv())
    clearPlatformDatabaseKeyReadState()
    ChatModel.chatDbStatus.value = null

    val failure = runCatching {
      runBlocking { initChatController() }
    }.exceptionOrNull()

    assertNotNull(failure)
    assertNull(ChatModel.chatDbStatus.value)
    val keyState = platformDatabaseKeyReadState()
    assertTrue(keyState is AndroidDatabaseKeyReadState.UnreadableMaterial)
    assertTrue(requireNotNull(keyState).initialRandomDBPassphrase)
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_random_key_title),
    ).assertIsDisplayed()
    passwordNode().assertDoesNotExist()

    prefs.initializationVectorDBPassphrase.set(originalIv)
    clearPlatformDatabaseKeyReadState()
    runBlocking { initChatController(useKey = randomKey) }
    waitForDatabaseOk()
    DatabaseUtils.ksDatabasePassword.set(randomKey)
  }

  private fun seedManualFixture(
    randomKey: String,
    fixtureKey: String,
  ) {
    val before = databasePairHashes()
    stopController()
    val error = runBlocking {
      ChatModel.controller.apiStorageEncryption(randomKey, fixtureKey)
    }
    assertNull(error)
    ChatModel.controller.appPrefs.storeDBPassphrase.set(true)
    ChatModel.controller.appPrefs.initialRandomDBPassphrase.set(false)
    DatabaseUtils.ksDatabasePassword.set(fixtureKey)

    clearPlatformDatabaseKeyReadState()
    ChatModel.chatDbStatus.value = null
    runBlocking { initChatController(useKey = fixtureKey) }
    waitForDatabaseOk()
    assertNotEquals(before, databasePairHashes())
  }

  private fun verifyManualMissingAliasAndLifecycle(
    fixtureKey: String,
    wrongKey: String,
    target: android.content.Context,
    scenario: ActivityScenario<MainActivity>,
  ) {
    stopController()
    cryptor.deleteKey(DATABASE_KEY_ALIAS)
    clearPlatformDatabaseKeyReadState()
    ChatModel.chatDbStatus.value = null

    runBlocking { initChatController() }

    assertTrue(ChatModel.chatDbStatus.value is DBMigrationResult.ErrorNotADatabase)
    val keyState = platformDatabaseKeyReadState()
    assertTrue(keyState is AndroidDatabaseKeyReadState.MissingAlias)
    assertFalse(requireNotNull(keyState).initialRandomDBPassphrase)
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_stored_key_title),
    ).assertIsDisplayed()
    passwordNode()
      .assertIsDisplayed()
      .performTextInput(FIXTURE_INPUT)

    scenario.moveToState(Lifecycle.State.CREATED)
    scenario.moveToState(Lifecycle.State.RESUMED)
    passwordNode().assert(
      SemanticsMatcher.expectValue(
        SemanticsProperties.EditableText,
        androidx.compose.ui.text.AnnotatedString(""),
      ),
    )

    val beforeWrong = databasePairHashes()
    clearPlatformDatabaseKeyReadState()
    ChatModel.chatDbStatus.value = null
    runBlocking { initChatController(useKey = wrongKey) }
    assertTrue(ChatModel.chatDbStatus.value is DBMigrationResult.ErrorNotADatabase)
    assertEquals(beforeWrong, databasePairHashes())

    clearPlatformDatabaseKeyReadState()
    ChatModel.chatDbStatus.value = null
    runBlocking { initChatController(useKey = fixtureKey) }
    waitForDatabaseOk()
    assertEquals(beforeWrong, databasePairHashes())
    DatabaseUtils.ksDatabasePassword.set(fixtureKey)
  }

  private fun verifyManualUnreadableMaterial(
    fixtureKey: String,
    target: android.content.Context,
  ) {
    stopController()
    val prefs = ChatModel.controller.appPrefs
    val originalIv = requireNotNull(
      prefs.initializationVectorDBPassphrase.get(),
    )
    prefs.initializationVectorDBPassphrase.set(invalidFixtureIv())
    clearPlatformDatabaseKeyReadState()
    ChatModel.chatDbStatus.value = null

    runBlocking { initChatController() }

    assertTrue(ChatModel.chatDbStatus.value is DBMigrationResult.ErrorNotADatabase)
    val keyState = platformDatabaseKeyReadState()
    assertTrue(keyState is AndroidDatabaseKeyReadState.UnreadableMaterial)
    assertFalse(requireNotNull(keyState).initialRandomDBPassphrase)
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_stored_key_title),
    ).assertIsDisplayed()
    passwordNode().assertIsDisplayed()

    prefs.initializationVectorDBPassphrase.set(originalIv)
    clearPlatformDatabaseKeyReadState()
    runBlocking { initChatController(useKey = fixtureKey) }
    waitForDatabaseOk()
    DatabaseUtils.ksDatabasePassword.set(fixtureKey)
  }

  private fun verifyMatchedBackupCopyAndFreshOpen(
    fixtureKey: String,
    target: android.content.Context,
  ) {
    stopController()
    val chat = File(dataDir, chatDatabaseFileName)
    val agent = File(dataDir, agentDatabaseFileName)
    val chatBackup = File(dataDir, "$chatDatabaseFileName.bak")
    val agentBackup = File(dataDir, "$agentDatabaseFileName.bak")
    assertTrue(chat.isFile)
    assertTrue(agent.isFile)
    chat.copyTo(chatBackup, overwrite = true)
    agent.copyTo(agentBackup, overwrite = true)
    val validPair = pairHashes(chatBackup, agentBackup)

    ChatModel.controller.appPrefs.encryptionStartedAt.set(Clock.System.now())
    val fixtureModifiedAt = System.currentTimeMillis()
    assertTrue(chatBackup.setLastModified(fixtureModifiedAt))
    assertTrue(agentBackup.setLastModified(fixtureModifiedAt))

    chat.writeBytes(CORRUPT_CHAT_BYTES)
    agent.writeBytes(CORRUPT_AGENT_BYTES)
    assertNotEquals(validPair, databasePairHashes())
    clearPlatformDatabaseKeyReadState()
    ChatModel.chatDbStatus.value = null
    runBlocking { initChatController(useKey = fixtureKey) }
    assertTrue(ChatModel.chatDbStatus.value !is DBMigrationResult.OK)

    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_copy_backup),
    )
      .assertIsDisplayed()
      .performClick()
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_backup_confirm),
    )
      .assertIsDisplayed()
      .performClick()
    waitUntil {
      databasePairHashes() == validPair
    }
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_restored_title),
    ).assertIsDisplayed()
    composeRule.onNodeWithText(
      target.getString(R.string.nome_database_root_open_restored),
    )
      .assertIsDisplayed()
      .performClick()

    waitForDatabaseOk()
    assertEquals(validPair, databasePairHashes())
    DatabaseUtils.ksDatabasePassword.set(fixtureKey)
  }

  private fun stopController() {
    if (ChatModel.chatRunning.value == true) {
      runBlocking { stopChatAsync(ChatModel) }
    } else {
      runBlocking { ChatModel.controller.apiStopChat() }
    }
    waitUntil {
      ChatModel.chatRunning.value != true &&
        !ChatModel.ctrlInitInProgress.value
    }
  }

  private fun waitForDatabaseOk() {
    waitUntil {
      ChatModel.chatDbStatus.value is DBMigrationResult.OK &&
        !ChatModel.ctrlInitInProgress.value
    }
  }

  private fun waitUntil(predicate: () -> Boolean) {
    composeRule.waitUntil(
      timeoutMillis = FIXTURE_TIMEOUT_MILLIS,
      condition = predicate,
    )
  }

  private fun passwordNode() =
    composeRule.onNode(
      SemanticsMatcher.expectValue(
        SemanticsProperties.Password,
        Unit,
      ),
    )

  private fun databasePairHashes(): List<String> =
    pairHashes(
      File(dataDir, chatDatabaseFileName),
      File(dataDir, agentDatabaseFileName),
    )

  private fun pairHashes(chat: File, agent: File): List<String> =
    listOf(chat, agent).map { file ->
      assertTrue(file.isFile)
      MessageDigest.getInstance("SHA-256")
        .digest(file.readBytes())
        .joinToString("") { byte -> "%02x".format(byte) }
    }

  private fun randomFixtureKey(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
  }

  private fun invalidFixtureIv(): String =
    Base64.encodeToString(ByteArray(12), Base64.NO_WRAP)

  private companion object {
    const val REAL_FIXTURE_ARGUMENT = "nomeP01RealFixture"
    const val DATABASE_KEY_ALIAS = "databasePassword"
    const val FIXTURE_INPUT = "controlled-fixture-input"
    const val FIXTURE_TIMEOUT_MILLIS = 120_000L
    val CORRUPT_CHAT_BYTES = "controlled-chat-fixture".encodeToByteArray()
    val CORRUPT_AGENT_BYTES = "controlled-agent-fixture".encodeToByteArray()
  }
}
