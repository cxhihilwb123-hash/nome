package chat.simplex.common.platform

import androidx.compose.runtime.*
import chat.simplex.common.*
import chat.simplex.common.views.helpers.AlertManager
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.MR
import java.awt.Desktop
import java.io.*
import java.net.URI
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermissions

actual val dataDir: File = ensurePrivateDirectory(File(desktopPlatform.dataPath))
actual val tmpDir: File = ensurePrivateDirectory(desktopRuntimeTmpDirectory()).also { it.deleteOnExit() }
actual val filesDir: File = ensurePrivateDirectory(File(dataDir, "simplex_v1_files"))
actual val appFilesDir: File = filesDir
actual val wallpapersDir: File = ensurePrivateDirectory(File(dataDir.absolutePath + File.separator + "simplex_v1_assets" + File.separator + "wallpapers"))
actual val coreTmpDir: File = ensurePrivateDirectory(File(dataDir, "tmp"))
actual val dbAbsolutePrefixPath: String = dataDir.absolutePath + File.separator + "simplex_v1"
actual val preferencesDir = ensurePrivateDirectory(File(desktopPlatform.configPath))
actual val preferencesTmpDir = ensurePrivateDirectory(File(preferencesDir, "tmp"))

actual val chatDatabaseFileName: String = "simplex_v1_chat.db"
actual val agentDatabaseFileName: String = "simplex_v1_agent.db"

actual val databaseExportDir: File = tmpDir

actual val remoteHostsDir: File = ensurePrivateDirectory(File(dataDir, "remote_hosts"))

internal fun desktopRuntimeTmpDirectory(
  platform: DesktopPlatform = desktopPlatform,
  systemTmpPath: String = System.getProperty("java.io.tmpdir"),
): File =
  if (platform.isMac()) {
    File(
      File(systemTmpPath, "chat.nome.app"),
      keychainProfileScope(platform.configPath, platform.dataPath),
    )
  } else {
    File(systemTmpPath, "simplex")
  }

internal fun ensurePrivateDirectory(directory: File): File {
  val path = directory.toPath()
  if (Files.exists(path, NOFOLLOW_LINKS) && (!Files.isDirectory(path, NOFOLLOW_LINKS) || Files.isSymbolicLink(path))) {
    throw IOException("Private application directory path is not a directory")
  }
  if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
    throw IOException("Unable to create private application directory")
  }
  if (!Files.isDirectory(path, NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
    throw IOException("Private application directory path is not a directory")
  }
  setPrivatePosixPermissions(directory, "rwx------")
  return directory
}

internal fun protectPrivateFile(file: File) {
  val path = file.toPath()
  if (!Files.exists(path, NOFOLLOW_LINKS)) return
  if (!Files.isRegularFile(path, NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
    throw IOException("Private application file path is not a regular file")
  }
  setPrivatePosixPermissions(file, "rw-------")
}

private fun setPrivatePosixPermissions(file: File, permissions: String) {
  val supportsPosix = file.toPath().fileSystem.supportedFileAttributeViews().contains("posix")
  if (!supportsPosix && !desktopPlatform.isMac()) return
  try {
    Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString(permissions))
  } catch (e: UnsupportedOperationException) {
    throw IOException("POSIX permissions are unavailable for private application data", e)
  } catch (e: IOException) {
    throw IOException("Unable to protect private application data", e)
  } catch (e: SecurityException) {
    throw IOException("Permission denied while protecting private application data", e)
  }
}

internal fun resetPrivateDirectory(directory: File) {
  val path = directory.toPath()
  if (Files.exists(path, NOFOLLOW_LINKS)) {
    if (!Files.isDirectory(path, NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
      throw IOException("Private application directory path is not a directory")
    }
    Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
      override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        Files.delete(file)
        return FileVisitResult.CONTINUE
      }

      override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
        if (exc != null) throw exc
        Files.delete(dir)
        return FileVisitResult.CONTINUE
      }
    })
  }
  ensurePrivateDirectory(directory)
}

/** Reset only the already-scoped Nome runtime directory, never a legacy SimpleX temp root. */
fun resetNomeRuntimeTmpDirectory() = resetPrivateDirectory(tmpDir)

internal fun privateAppDataFiles(
  dataDirectory: File,
  preferencesDirectory: File,
  chatDatabaseName: String,
  agentDatabaseName: String,
): List<File> {
  val databaseFiles = privateChatDatabaseFiles(dataDirectory, chatDatabaseName, agentDatabaseName)
  return databaseFiles + listOf(
    File(preferencesDirectory, "settings.properties"),
    File(preferencesDirectory, "themes.properties"),
    File(preferencesDirectory, "themes.yaml"),
  )
}

internal fun privateChatDatabaseFiles(
  dataDirectory: File,
  chatDatabaseName: String,
  agentDatabaseName: String,
): List<File> =
  listOf(chatDatabaseName, agentDatabaseName).flatMap { databaseFileName ->
    listOf(
      databaseFileName,
      "$databaseFileName.bak",
      "$databaseFileName-journal",
      "$databaseFileName-wal",
      "$databaseFileName-shm",
    )
  }.map { File(dataDirectory, it) }

internal fun validatedPrivateWipeDirectoryPath(
  directory: File,
  dataDirectory: File,
  preferencesDirectory: File,
  runtimeTmpDirectory: File,
  allowedDirectories: Set<Path>,
): Path {
  val path = directory.toPath().toAbsolutePath().normalize()
  val dataRoot = dataDirectory.toPath().toAbsolutePath().normalize()
  val preferencesRoot = preferencesDirectory.toPath().toAbsolutePath().normalize()
  val runtimeTmpRoot = runtimeTmpDirectory.toPath().toAbsolutePath().normalize()
  val insideNomeRoot =
    listOf(dataRoot, preferencesRoot, runtimeTmpRoot).any { root -> path == root || path.startsWith(root) }
  val attemptsBroadDataReset = path == dataRoot || path == preferencesRoot
  if (path !in allowedDirectories || !insideNomeRoot || attemptsBroadDataReset) {
    throw IOException("Refusing to reset a directory outside the Nome storage allowlist")
  }
  return path
}

actual fun protectAppDataFiles() {
  listOf(dataDir, filesDir, wallpapersDir, coreTmpDir, tmpDir, preferencesDir, preferencesTmpDir, remoteHostsDir)
    .forEach(::ensurePrivateDirectory)
  privateAppDataFiles(dataDir, preferencesDir, chatDatabaseFileName, agentDatabaseFileName)
    .forEach(::protectPrivateFile)
}

actual fun wipeChatStorageFilesVerified() {
  val normalizedDataDirectory = dataDir.toPath().toAbsolutePath().normalize()
  val databaseFiles = privateChatDatabaseFiles(dataDir, chatDatabaseFileName, agentDatabaseFileName)
  val allowedDatabaseNames = databaseFiles.mapTo(mutableSetOf()) { it.name }
  databaseFiles.forEach { file ->
    val path = file.toPath().toAbsolutePath().normalize()
    if (path.parent != normalizedDataDirectory || path.fileName.toString() !in allowedDatabaseNames) {
      throw IOException("Refusing to delete a path outside the Nome database allowlist")
    }
    Files.deleteIfExists(path)
    if (Files.exists(path, NOFOLLOW_LINKS)) {
      throw IOException("Unable to delete a Nome database file")
    }
  }

  val migrationDirectory = getMigrationTempFilesDirectory()
  val directories =
    listOf(filesDir, appFilesDir, wallpapersDir, coreTmpDir, remoteHostsDir, tmpDir, migrationDirectory)
    .distinctBy { it.toPath().toAbsolutePath().normalize() }
  val allowedDirectories = directories.mapTo(mutableSetOf()) { it.toPath().toAbsolutePath().normalize() }
  directories.forEach { directory ->
    validatedPrivateWipeDirectoryPath(directory, dataDir, preferencesDir, tmpDir, allowedDirectories)
    resetPrivateDirectory(directory)
  }

  // Re-validate all application roots and every file the native layer may have recreated while
  // shutting down. Any failure keeps the self-destruct transaction in its incomplete state.
  protectAppDataFiles()
  ensurePrivateDirectory(migrationDirectory)
  databaseFiles.forEach { file ->
    if (Files.exists(file.toPath(), NOFOLLOW_LINKS)) {
      throw IOException("Nome database file still exists after deletion")
    }
  }
}

actual fun desktopOpenDatabaseDir() {
  desktopOpenDir(dataDir)
}

actual fun desktopOpenDir(dir: File) {
  if (Desktop.isDesktopSupported()) {
    try {
      Desktop.getDesktop().open(dir);
    } catch (e: IOException) {
      Log.e(TAG, e.stackTraceToString())
      AlertManager.shared.showAlertMsg(
        title = generalGetString(MR.strings.unknown_error),
        text = e.stackTraceToString()
      )
    }
  }
}

@Composable
actual fun rememberFileChooserLauncher(
  getContent: Boolean,
  rememberedValue: Any?,
  saveMimeType: String?,
  onResult: (URI?) -> Unit
): FileChooserLauncher =
  remember(rememberedValue) { FileChooserLauncher(getContent, onResult) }

@Composable
actual fun rememberFileChooserMultipleLauncher(onResult: (List<URI>) -> Unit): FileChooserMultipleLauncher =
  remember { FileChooserMultipleLauncher(onResult) }

actual class FileChooserLauncher actual constructor() {
  var getContent: Boolean = false
  lateinit var onResult: (URI?) -> Unit

  constructor(getContent: Boolean, onResult: (URI?) -> Unit): this() {
    this.getContent = getContent
    this.onResult = onResult
  }

  actual suspend fun launch(input: String) {
    var res: File?
    if (getContent) {
      val params = DialogParams(
        allowMultiple = false,
        fileFilter = fileFilter(input),
        fileFilterDescription = fileFilterDescription(input),
      )
      res = simplexWindowState.openDialog.awaitResult(params)
    } else {
      res = simplexWindowState.saveDialog.awaitResult(DialogParams(filename = input))
      if (res != null && res.isDirectory) {
        res = File(res, input)
      }
    }
    onResult(res?.toURI())
  }
}

actual class FileChooserMultipleLauncher actual constructor() {
  lateinit var onResult: (List<URI>) -> Unit

  constructor(onResult: (List<URI>) -> Unit): this() {
    this.onResult = onResult
  }

  actual suspend fun launch(input: String) {
    val params = DialogParams(
        allowMultiple = true,
        fileFilter = fileFilter(input),
        fileFilterDescription = fileFilterDescription(input),
      )
    onResult(simplexWindowState.openMultipleDialog.awaitResult(params).map { it.toURI() })
  }
}

private fun fileFilter(input: String): (File?) -> Boolean = when(input) {
  "image/*" -> { file -> if (file?.isDirectory == true) true else if (file != null) isImage(file.toURI()) else false }
  "video/*" -> { file -> if (file?.isDirectory == true) true else if (file != null) isVideo(file.toURI()) else false }
  "*/*" -> { _ -> true }
  else -> { _ -> true }
}

private fun fileFilterDescription(input: String): String = when(input) {
  "image/*" -> generalGetString(MR.strings.gallery_image_button)
  "video/*" -> generalGetString(MR.strings.gallery_video_button)
  "*/*" -> generalGetString(MR.strings.choose_file)
  else -> ""
}

actual fun URI.inputStream(): InputStream? = toFile().inputStream()
actual fun URI.outputStream(): OutputStream = toFile().outputStream()
