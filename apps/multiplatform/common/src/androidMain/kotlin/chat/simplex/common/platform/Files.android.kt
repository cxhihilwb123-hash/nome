package chat.simplex.common.platform

import android.app.Application
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import chat.simplex.common.helpers.toURI
import chat.simplex.common.helpers.toUri
import java.io.*
import java.net.URI

actual val dataDir: File = androidAppContext.dataDir
actual val tmpDir: File = androidAppContext.getDir("temp", Application.MODE_PRIVATE)
actual val filesDir: File = File(dataDir.absolutePath + File.separator + "files")
actual val appFilesDir: File = File(filesDir.absolutePath + File.separator + "app_files")
actual val wallpapersDir: File = File(filesDir.absolutePath + File.separator + "assets" + File.separator + "wallpapers").also { it.mkdirs() }
actual val coreTmpDir: File = File(filesDir.absolutePath + File.separator + "temp_files")
actual val dbAbsolutePrefixPath: String = dataDir.absolutePath + File.separator + "files"
actual val preferencesDir = File(dataDir.absolutePath + File.separator + "shared_prefs")
actual val preferencesTmpDir = File(tmpDir, "prefs_tmp")
  .also { it.deleteRecursively() }

actual val chatDatabaseFileName: String = "files_chat.db"
actual val agentDatabaseFileName: String = "files_agent.db"

actual val databaseExportDir: File = androidAppContext.cacheDir

actual val remoteHostsDir: File = File(tmpDir.absolutePath + File.separator + "remote_hosts")

actual fun protectAppDataFiles() = Unit

actual fun wipeChatStorageFilesVerified() {
  val databaseFiles = listOf(chatDatabaseFileName, agentDatabaseFileName).flatMap { databaseFileName ->
    listOf(
      databaseFileName,
      "$databaseFileName.bak",
      "$databaseFileName-journal",
      "$databaseFileName-wal",
      "$databaseFileName-shm",
    )
  }.map { File(dataDir, it) }

  databaseFiles.forEach { file ->
    if (file.exists() && !file.delete()) {
      throw IOException("Unable to delete a Nome database file")
    }
    if (file.exists()) {
      throw IOException("Nome database file still exists after deletion")
    }
  }

  listOf(
    filesDir,
    appFilesDir,
    wallpapersDir,
    coreTmpDir,
    tmpDir,
    remoteHostsDir,
    getMigrationTempFilesDirectory(),
  ).distinctBy { it.absoluteFile.path }.forEach(::resetAppDirectoryVerified)
}

private fun resetAppDirectoryVerified(directory: File) {
  if (directory.exists() && !directory.deleteRecursively()) {
    throw IOException("Unable to delete a Nome application directory")
  }
  if (directory.exists()) {
    throw IOException("Nome application directory still exists after deletion")
  }
  if (!directory.mkdirs() && !directory.isDirectory) {
    throw IOException("Unable to recreate a Nome application directory")
  }
}

actual fun desktopOpenDatabaseDir() {}

actual fun desktopOpenDir(dir: File) {}

@Composable
actual fun rememberFileChooserLauncher(
  getContent: Boolean,
  rememberedValue: Any?,
  saveMimeType: String?,
  onResult: (URI?) -> Unit
): FileChooserLauncher {
  val launcher = rememberLauncherForActivityResult(
    contract = if (getContent) {
      ActivityResultContracts.GetContent()
    } else {
      ActivityResultContracts.CreateDocument(saveMimeType ?: "*/*")
    },
    onResult = { onResult(it?.toURI()) }
  )
  return FileChooserLauncher(launcher)
}

@Composable
actual fun rememberFileChooserMultipleLauncher(onResult: (List<URI>) -> Unit): FileChooserMultipleLauncher {
  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetMultipleContents(),
    onResult = { onResult(it.map { it.toURI() }) }
  )
  return FileChooserMultipleLauncher(launcher)
}

actual class FileChooserLauncher actual constructor() {
  private lateinit var launcher: ManagedActivityResultLauncher<String, Uri?>

  constructor(launcher: ManagedActivityResultLauncher<String, Uri?>): this() {
    this.launcher = launcher
  }

  actual suspend fun launch(input: String) {
    launcher.launch(input)
  }
}

actual class FileChooserMultipleLauncher actual constructor() {
  private lateinit var launcher: ManagedActivityResultLauncher<String, List<Uri>>

  constructor(launcher: ManagedActivityResultLauncher<String, List<Uri>>): this() {
    this.launcher = launcher
  }

  actual suspend fun launch(input: String) {
    launcher.launch(input)
  }
}

actual fun URI.inputStream(): InputStream? = androidAppContext.contentResolver.openInputStream(toUri())
actual fun URI.outputStream(): OutputStream = androidAppContext.contentResolver.openOutputStream(toUri())!!
