package chat.simplex.common

import chat.simplex.common.platform.Log
import chat.simplex.common.platform.TAG
import chat.simplex.common.platform.dataDir
import chat.simplex.common.platform.ensurePrivateDirectory
import chat.simplex.common.platform.protectPrivateFile
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.*
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

private var lockHandle: FileLock? = null
private var watcher: WatchService? = null

internal const val NOME_INSTANCE_LOCK_FILE_NAME = "nome.started"
internal const val NOME_SHOW_FILE_NAME = "nome.show"

var singleInstanceLock = false
  private set

internal sealed interface LockResult {
  class Acquired(val lock: FileLock) : LockResult
  object Taken : LockResult
  object Failed : LockResult
}

fun acquireSingleInstance(): Boolean = acquireSingleInstance(dataDir.toPath())

internal fun acquireSingleInstance(instanceDirectory: Path): Boolean {
  try {
    ensurePrivateDirectory(instanceDirectory.toFile())
  } catch (_: IOException) {
    Log.w(TAG, "nome-single-instance: private data directory is unavailable")
    return false
  } catch (_: SecurityException) {
    Log.w(TAG, "nome-single-instance: private data directory is unavailable")
    return false
  }

  val lockPath = instanceDirectory.resolve(NOME_INSTANCE_LOCK_FILE_NAME)
  val showPath = instanceDirectory.resolve(NOME_SHOW_FILE_NAME)
  when (val result = tryAcquireLock(lockPath)) {
    is LockResult.Acquired -> {
      lockHandle = result.lock
      singleInstanceLock = true
      deleteShowFile(showPath)
      startShowFileWatcher(instanceDirectory, showPath)
      return true
    }
    LockResult.Failed -> {
      // A lock error is not proof that this process owns the profile. Starting
      // anyway could put two native cores on the same SQLite databases.
      return false
    }
    LockResult.Taken -> {
      // A held lock means another process owns this data directory. Signal it
      // to raise its window, but never offer to start a second process against
      // the same databases — doing so could corrupt the user's local data.
      createShowFile(showPath)
      val deadline = System.currentTimeMillis() + 1000
      while (Files.exists(showPath) && System.currentTimeMillis() < deadline) {
        try { Thread.sleep(50) } catch (_: InterruptedException) { break }
      }
      deleteShowFile(showPath)
      return false
    }
  }
}

internal fun tryAcquireLock(lockPath: Path): LockResult {
  val channel = try {
    FileChannel.open(lockPath, READ, WRITE, CREATE)
  } catch (_: IOException) {
    Log.w(TAG, "nome-single-instance: cannot open lock file")
    return LockResult.Failed
  } catch (_: SecurityException) {
    Log.w(TAG, "nome-single-instance: cannot open lock file")
    return LockResult.Failed
  }

  try {
    protectPrivateFile(lockPath.toFile())
  } catch (_: IOException) {
    closeChannel(channel)
    Log.w(TAG, "nome-single-instance: cannot protect lock file")
    return LockResult.Failed
  } catch (_: SecurityException) {
    closeChannel(channel)
    Log.w(TAG, "nome-single-instance: cannot protect lock file")
    return LockResult.Failed
  }
  return tryLockChannel(channel)
}

internal fun tryLockChannel(channel: FileChannel): LockResult {
  return try {
    val lock = channel.tryLock(0L, 1L, false)
    if (lock != null) {
      LockResult.Acquired(lock)
    } else {
      closeChannel(channel)
      LockResult.Taken
    }
  } catch (_: OverlappingFileLockException) {
    closeChannel(channel)
    Log.w(TAG, "nome-single-instance: overlapping lock in same JVM")
    LockResult.Failed
  } catch (_: IOException) {
    closeChannel(channel)
    Log.w(TAG, "nome-single-instance: tryLock failed")
    LockResult.Failed
  }
}

private fun closeChannel(channel: FileChannel) {
  try {
    channel.close()
  } catch (_: IOException) {
    // The process is already failing closed; there is no recovery path here.
  }
}

private fun deleteShowFile(showPath: Path) {
  try { Files.deleteIfExists(showPath) } catch (e: IOException) {
    Log.w(TAG, "nome-single-instance: cannot delete show file")
  }
}

private fun createShowFile(showPath: Path) {
  try {
    try {
      Files.createFile(showPath)
    } catch (_: FileAlreadyExistsException) {
      // Another duplicate already signalled; primary will pick it up.
    }
    protectPrivateFile(showPath.toFile())
  } catch (e: IOException) {
    Log.w(TAG, "nome-single-instance: cannot create show file")
    deleteShowFile(showPath)
  } catch (_: SecurityException) {
    Log.w(TAG, "nome-single-instance: cannot create show file")
    deleteShowFile(showPath)
  }
}

private fun startShowFileWatcher(instanceDirectory: Path, showPath: Path) {
  if (watcher != null) return
  val ws = try {
    instanceDirectory.fileSystem.newWatchService()
  } catch (_: IOException) {
    Log.w(TAG, "nome-single-instance: WatchService failed")
    return
  }
  try {
    instanceDirectory.register(ws, StandardWatchEventKinds.ENTRY_CREATE)
  } catch (_: IOException) {
    closeWatchService(ws)
    Log.w(TAG, "nome-single-instance: cannot watch the data directory")
    return
  }
  watcher = ws
  thread(name = "nome-single-instance", isDaemon = true) {
    while (true) {
      val key = try { ws.take() } catch (_: ClosedWatchServiceException) { return@thread } catch (_: InterruptedException) { return@thread }
      for (event in key.pollEvents()) {
        if ((event.context() as? Path)?.fileName?.toString() == NOME_SHOW_FILE_NAME) {
          deleteShowFile(showPath)
          SwingUtilities.invokeLater { showWindow() }
        }
      }
      if (!key.reset()) return@thread
    }
  }
}

private fun closeWatchService(service: WatchService) {
  try {
    service.close()
  } catch (_: IOException) {
    // Nothing else owns the service when registration fails.
  }
}
