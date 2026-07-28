package chat.simplex.common.platform

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.Comparator
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopStorageIsolationTest {
  @Test
  fun existingRegularFileIsRejectedAsPrivateDirectory() = withTempDir { root ->
    val file = root.resolve("not-a-directory")
    Files.writeString(file, "sentinel")

    assertFailsWith<IOException> {
      ensurePrivateDirectory(file.toFile())
    }
    assertEquals("sentinel", Files.readString(file))
  }

  @Test
  fun directoriesAndFilesReceiveOwnerOnlyPermissions() = withTempDir { root ->
    assertTrue(root.fileSystem.supportedFileAttributeViews().contains("posix"))
    val directory = root.resolve("private").toFile()
    directory.mkdirs()
    Files.setPosixFilePermissions(directory.toPath(), PosixFilePermissions.fromString("rwxrwxrwx"))

    ensurePrivateDirectory(directory)
    assertEquals(
      PosixFilePermissions.fromString("rwx------"),
      Files.getPosixFilePermissions(directory.toPath()),
    )

    val file = File(directory, "settings.properties")
    file.writeText("secret=value")
    Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-rw-rw-"))

    protectPrivateFile(file)
    assertEquals(
      PosixFilePermissions.fromString("rw-------"),
      Files.getPosixFilePermissions(file.toPath()),
    )
  }

  @Test
  fun privateFileProtectionRejectsSymlinkWithoutTouchingTarget() = withTempDir { root ->
    val legacyFile = root.resolve("legacy-settings.properties")
    Files.writeString(legacyFile, "keep")
    Files.setPosixFilePermissions(legacyFile, PosixFilePermissions.fromString("rw-rw-rw-"))
    val nomeLink = root.resolve("nome-settings.properties")
    Files.createSymbolicLink(nomeLink, legacyFile)

    assertFailsWith<IOException> {
      protectPrivateFile(nomeLink.toFile())
    }

    assertEquals("keep", Files.readString(legacyFile))
    assertEquals(
      PosixFilePermissions.fromString("rw-rw-rw-"),
      Files.getPosixFilePermissions(legacyFile),
    )
  }

  @Test
  fun settingsWriterLeavesFinalFileOwnerOnly() = withTempDir { root ->
    val config = root.resolve("config").toFile()
    val temp = root.resolve("tmp").toFile()
    val target = File(config, "settings.properties")
    val properties = Properties().apply { setProperty("theme", "dark") }

    savePropertiesPrivately(properties, target, temp)

    val stored = Properties().apply { target.reader().use { load(it) } }
    assertEquals("dark", stored.getProperty("theme"))
    assertEquals(
      PosixFilePermissions.fromString("rw-------"),
      Files.getPosixFilePermissions(target.toPath()),
    )
    assertEquals(
      PosixFilePermissions.fromString("rwx------"),
      Files.getPosixFilePermissions(config.toPath()),
    )
    assertEquals(
      PosixFilePermissions.fromString("rwx------"),
      Files.getPosixFilePermissions(temp.toPath()),
    )
  }

  @Test
  fun appProtectionCoversDatabasesSidecarsBackupsAndPreferences() = withTempDir { root ->
    val data = root.resolve("data").toFile()
    val config = root.resolve("config").toFile()
    val relativePaths = privateAppDataFiles(data, config, "chat.db", "agent.db")
      .map { root.relativize(it.toPath()).toString() }
      .toSet()

    assertEquals(
      setOf(
        "data/chat.db",
        "data/chat.db.bak",
        "data/chat.db-journal",
        "data/chat.db-wal",
        "data/chat.db-shm",
        "data/agent.db",
        "data/agent.db.bak",
        "data/agent.db-journal",
        "data/agent.db-wal",
        "data/agent.db-shm",
        "config/settings.properties",
        "config/themes.properties",
        "config/themes.yaml",
      ),
      relativePaths,
    )
  }

  @Test
  fun resettingNomeTempDoesNotTouchLegacySiblingOrFollowSymlinks() = withTempDir { root ->
    val nomeTmp = root.resolve("chat.nome.app/profile").toFile()
    val legacyTmp = root.resolve("simplex").toFile()
    val outside = root.resolve("outside").toFile()
    ensurePrivateDirectory(nomeTmp)
    ensurePrivateDirectory(legacyTmp)
    ensurePrivateDirectory(outside)
    File(nomeTmp, "stale").writeText("remove")
    File(legacyTmp, "legacy-sentinel").writeText("keep")
    File(outside, "outside-sentinel").writeText("keep")
    Files.createSymbolicLink(nomeTmp.toPath().resolve("outside-link"), outside.toPath())

    resetPrivateDirectory(nomeTmp)

    assertTrue(nomeTmp.isDirectory)
    assertTrue(nomeTmp.listFiles()?.isEmpty() == true)
    assertEquals("keep", File(legacyTmp, "legacy-sentinel").readText())
    assertEquals("keep", File(outside, "outside-sentinel").readText())
    assertEquals(
      PosixFilePermissions.fromString("rwx------"),
      Files.getPosixFilePermissions(nomeTmp.toPath()),
    )
  }

  @Test
  fun wipeDirectoryValidationRejectsOutsideAndBroadRoots() = withTempDir { root ->
    val data = root.resolve("data").toFile()
    val config = root.resolve("config").toFile()
    val runtimeTmp = root.resolve("runtime-tmp").toFile()
    val attachments = root.resolve("data/attachments").toFile()
    val outside = root.resolve("outside").toFile()
    val allowed = setOf(
      data.toPath().toAbsolutePath().normalize(),
      config.toPath().toAbsolutePath().normalize(),
      runtimeTmp.toPath().toAbsolutePath().normalize(),
      attachments.toPath().toAbsolutePath().normalize(),
      outside.toPath().toAbsolutePath().normalize(),
    )

    assertEquals(
      attachments.toPath().toAbsolutePath().normalize(),
      validatedPrivateWipeDirectoryPath(attachments, data, config, runtimeTmp, allowed),
    )
    assertEquals(
      runtimeTmp.toPath().toAbsolutePath().normalize(),
      validatedPrivateWipeDirectoryPath(runtimeTmp, data, config, runtimeTmp, allowed),
    )
    assertFailsWith<IOException> {
      validatedPrivateWipeDirectoryPath(outside, data, config, runtimeTmp, allowed)
    }
    assertFailsWith<IOException> {
      validatedPrivateWipeDirectoryPath(data, data, config, runtimeTmp, allowed)
    }
    assertFailsWith<IOException> {
      validatedPrivateWipeDirectoryPath(config, data, config, runtimeTmp, allowed)
    }
  }

  @Test
  fun resetPrivateDirectoryFailureDoesNotReplaceUnexpectedRegularFile() = withTempDir { root ->
    val unexpectedFile = root.resolve("expected-directory")
    Files.writeString(unexpectedFile, "keep")

    assertFailsWith<IOException> { resetPrivateDirectory(unexpectedFile.toFile()) }
    assertEquals("keep", Files.readString(unexpectedFile))
  }

  @Test
  fun macRuntimeTempUsesNomeProfileScope() {
    val runtimeDir = desktopRuntimeTmpDirectory(
      platform = DesktopPlatform.MAC_AARCH64,
      systemTmpPath = "/private/tmp/nome-storage-test",
    )

    assertEquals("chat.nome.app", runtimeDir.parentFile.name)
    assertTrue(runtimeDir.name.matches(Regex("[0-9a-f]{32}")))
    assertFalse(runtimeDir.toPath().startsWith(Path.of("/private/tmp/nome-storage-test/simplex")))
  }

  private fun withTempDir(block: (Path) -> Unit) {
    val root = Files.createTempDirectory("nome-storage-test")
    try {
      block(root)
    } finally {
      Files.walk(root).use { paths ->
        paths.sorted(Comparator.reverseOrder()).forEach { path ->
          try {
            Files.deleteIfExists(path)
          } catch (_: IOException) {
          }
        }
      }
    }
  }
}
