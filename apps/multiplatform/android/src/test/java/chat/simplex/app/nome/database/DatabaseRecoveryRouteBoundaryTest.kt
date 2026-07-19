package chat.simplex.app.nome.database

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseRecoveryRouteBoundaryTest {
  @Test
  fun openOnceAndSaveAndOpenRemainSeparateOperations() {
    val source = commonSource(
      "commonMain/kotlin/chat/simplex/common/views/database/DatabaseErrorView.kt",
    ).readText()
    val openOnce = source.functionBody("openDatabaseForRecovery")
    val save = source.functionBody("saveDatabaseKeyForRecovery")

    assertTrue(openOnce.contains("initChatController("))
    assertFalse(openOnce.contains("ksDatabasePassword.set"))
    assertFalse(openOnce.contains("storeDBPassphrase.set"))
    assertFalse(openOnce.contains("initialRandomDBPassphrase.set"))

    val keyWrite = save.indexOf("ksDatabasePassword.set")
    val preferenceWrite = save.indexOf("storeDBPassphrase.set(true)")
    val ownershipWrite = save.indexOf("initialRandomDBPassphrase.set(false)")
    assertTrue(keyWrite >= 0)
    assertTrue(preferenceWrite > keyWrite)
    assertTrue(ownershipWrite > preferenceWrite)
  }

  @Test
  fun androidRootUsesThePlatformSeamWithoutMovingTheAuthGuard() {
    val app = commonSource(
      "commonMain/kotlin/chat/simplex/common/App.kt",
    ).readText()

    assertEquals(
      3,
      Regex("""\bPlatformDatabaseRootRoute\s*\(""").findAll(app).count(),
    )
    assertTrue(app.contains("if (!unauthorized.value && status != null)"))
    assertTrue(app.contains("allowSensitiveContent = !unauthorized.value"))
    assertTrue(app.contains("DatabaseRootRouteInput.Opening"))
    assertTrue(app.contains("DatabaseRootRouteInput.Migrating"))
    assertTrue(app.contains("DatabaseRootRouteInput.Error(status)"))
  }

  @Test
  fun androidKeystoreAlertNoLongerAppendsRawThrowableText() {
    val cryptor = commonSource(
      "androidMain/kotlin/chat/simplex/common/platform/Cryptor.android.kt",
    ).readText()

    assertTrue(cryptor.contains("AndroidDatabaseKeyReadState.MissingAlias"))
    assertTrue(cryptor.contains("AndroidDatabaseKeyReadState.UnreadableMaterial"))
    assertTrue(cryptor.contains("clearPlatformDatabaseKeyReadState()"))
    assertTrue(cryptor.contains("if (alias == DATABASE_PASSWORD_ALIAS)"))
    assertTrue(cryptor.contains("cipher.init: database key material unreadable"))
    assertFalse(
      cryptor.contains(
        """.plus("\n\n").plus(e.stackTraceToString())""",
      ),
    )
  }

  @Test
  fun archiveExportRegistersZipAndLeavesChooserCleanupOwnedByItsResult() {
    val database = commonSource(
      "commonMain/kotlin/chat/simplex/common/views/database/DatabaseView.kt",
    ).readText()
    val files = commonSource(
      "androidMain/kotlin/chat/simplex/common/platform/Files.android.kt",
    ).readText()
    val export = database.functionBody("exportArchive")

    assertTrue(
      database.contains(
        """rememberFileChooserLauncher(false, saveMimeType = "application/zip")""",
      ),
    )
    assertTrue(database.contains("""importArchiveLauncher.launch("application/zip")"""))
    assertTrue(files.contains("""ActivityResultContracts.CreateDocument(saveMimeType ?: "*/*")"""))
    assertTrue(export.contains("return true"))
    assertTrue(database.contains("File(archive).delete()"))
    assertTrue(database.contains("chatArchiveFile.value = null"))
  }

  private fun commonSource(relativePath: String): File {
    var current = File(
      requireNotNull(System.getProperty("user.dir")),
    ).absoluteFile
    repeat(8) {
      val candidate = File(current, "../common/src/$relativePath").canonicalFile
      if (candidate.isFile) return candidate
      current = current.parentFile
        ?: error("Unable to locate common source root")
    }
    error("Unable to locate $relativePath")
  }

  private fun String.functionBody(name: String): String {
    val start = indexOf("fun $name")
    check(start >= 0) { "Missing function $name" }
    var depth = 0
    var opened = false
    for (index in start until length) {
      when (this[index]) {
        '{' -> {
          opened = true
          depth += 1
        }
        '}' -> if (opened) {
          depth -= 1
          if (depth == 0) return substring(start, index + 1)
        }
      }
    }
    error("Unbalanced function $name")
  }
}
