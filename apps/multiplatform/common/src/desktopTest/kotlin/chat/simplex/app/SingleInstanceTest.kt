package chat.simplex.common

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.PosixFilePermissions
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SingleInstanceTest {
  @Test
  fun instanceProtocolUsesNomeFileNames() {
    assertEquals("nome.started", NOME_INSTANCE_LOCK_FILE_NAME)
    assertEquals("nome.show", NOME_SHOW_FILE_NAME)
    assertFalse(NOME_INSTANCE_LOCK_FILE_NAME.contains("simplex", ignoreCase = true))
    assertFalse(NOME_SHOW_FILE_NAME.contains("simplex", ignoreCase = true))
  }

  @Test
  fun overlappingLockFailsClosedAndClosesSecondChannel() = withTempDir { dir ->
    val lockPath = dir.resolve(NOME_INSTANCE_LOCK_FILE_NAME)
    val first = FileChannel.open(lockPath, READ, WRITE, CREATE)
    val firstLock = assertNotNull(first.tryLock(0L, 1L, false), "first acquirer must get the lock")

    val second = FileChannel.open(lockPath, READ, WRITE, CREATE)
    assertIs<LockResult.Failed>(tryLockChannel(second))
    assertFalse(second.isOpen, "overlapping channel must be closed on failure")

    firstLock.release()
    first.close()
  }

  @Test
  fun releasedNomeLockCanBeReacquiredAndIsOwnerOnly() = withTempDir { dir ->
    val lockPath = dir.resolve(NOME_INSTANCE_LOCK_FILE_NAME)
    val first = assertIs<LockResult.Acquired>(tryAcquireLock(lockPath))
    assertEquals(
      PosixFilePermissions.fromString("rw-------"),
      Files.getPosixFilePermissions(lockPath),
    )
    first.lock.release()
    first.lock.channel().close()

    val second = assertIs<LockResult.Acquired>(tryAcquireLock(lockPath))
    second.lock.release()
    second.lock.channel().close()
  }

  @Test
  fun unusableProfilePathDoesNotAllowStartup() = withTempDir { dir ->
    val profilePath = dir.resolve("profile")
    Files.writeString(profilePath, "not a directory")

    assertFalse(acquireSingleInstance(profilePath))
    assertEquals("not a directory", Files.readString(profilePath))
  }

  @Test
  fun lockOpenFailureReturnsFailed() = withTempDir { dir ->
    val regularFile = dir.resolve("not-a-directory")
    Files.writeString(regularFile, "sentinel")

    assertIs<LockResult.Failed>(
      tryAcquireLock(regularFile.resolve(NOME_INSTANCE_LOCK_FILE_NAME)),
    )
  }

  private fun withTempDir(block: (Path) -> Unit) {
    val root = Files.createTempDirectory("nome-single-instance-test")
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
