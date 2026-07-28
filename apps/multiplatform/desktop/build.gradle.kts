import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
  id("io.github.tomtzook.gradle-cmake") version "1.2.2"
}

group = "chat.simplex"
version = extra["desktop.version_name"] as String

val macSigningIdentity = rootProject.extra["desktop.mac.signing.identity"] as String?
val macSigningKeychain = rootProject.extra["desktop.mac.signing.keychain"] as String?
val macNotarizationAppleId = rootProject.extra["desktop.mac.notarization.apple_id"] as String?
val macNotarizationPassword = rootProject.extra["desktop.mac.notarization.password"] as String?
val macNotarizationTeamId = rootProject.extra["desktop.mac.notarization.team_id"] as String?
val macReleaseCredentials = listOf(
  macSigningIdentity,
  macSigningKeychain,
  macNotarizationAppleId,
  macNotarizationPassword,
  macNotarizationTeamId,
)
val hasCompleteMacReleaseCredentials = macReleaseCredentials.all { !it.isNullOrBlank() }
val hasAnyMacReleaseCredential = macReleaseCredentials.any { !it.isNullOrBlank() }
val allowAdHocMacPackage = providers.gradleProperty("nome.allowAdHocMacPackage")
  .map { it.toBooleanStrict() }
  .orElse(false)
val isMacArm64PackagingHost =
  System.getProperty("os.name").lowercase().contains("mac") &&
    System.getProperty("os.arch").lowercase() in setOf("aarch64", "arm64")

private fun shouldStripNativeEntry(entryName: String): Boolean {
  val normalized = entryName.lowercase()
  val isX86Payload = normalized.contains("x86_64") ||
    normalized.contains("x86-64") ||
    normalized.contains("x64")
  if (isX86Payload) return true
  return normalized.endsWith(".dll") || normalized.endsWith(".so")
}

private fun shouldStripSignatureEntry(entryName: String): Boolean {
  if (!entryName.startsWith("META-INF/")) return false
  return entryName.endsWith(".SF") ||
    entryName.endsWith(".RSA") ||
    entryName.endsWith(".DSA") ||
    entryName.endsWith(".EC")
}

fun rewriteJarWithoutX64NativeEntries(sourceJar: File, targetJar: File): File? {
  var needsRewrite = false
  JarFile(sourceJar).use { jar ->
    val entries = jar.entries()
    while (entries.hasMoreElements()) {
      if (shouldStripNativeEntry(entries.nextElement().name)) {
        needsRewrite = true
        break
      }
    }
    if (!needsRewrite) return null

    Files.createDirectories(targetJar.parentFile.toPath())
    val tempJar = Files.createTempFile(targetJar.parentFile.toPath(), targetJar.name, ".tmp")
    try {
      JarOutputStream(Files.newOutputStream(tempJar)).use { output ->
        val allEntries = jar.entries()
        while (allEntries.hasMoreElements()) {
          val entry = allEntries.nextElement()
          if (shouldStripNativeEntry(entry.name) || shouldStripSignatureEntry(entry.name)) continue
          val jarEntry = JarEntry(entry.name)
          jarEntry.time = entry.time
          jarEntry.comment = entry.comment
          jarEntry.extra = entry.extra
          output.putNextEntry(jarEntry)
          if (!entry.isDirectory) {
            jar.getInputStream(entry).use { input ->
              input.copyTo(output)
            }
          }
          output.closeEntry()
        }
      }
      Files.move(tempJar, targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
    } finally {
      Files.deleteIfExists(tempJar)
    }
  }
  return targetJar
}

kotlin {
  jvm()
  sourceSets {
    val jvmMain by getting {
      dependencies {
        implementation(project(":common")) {
          exclude(group = "org.jetbrains.compose.desktop", module = "desktop-jvm-macos-x64")
        }
        // Nome Desktop is intentionally Apple-Silicon-only. Pinning this dependency prevents an
        // Intel host from silently resolving an x64 Compose runtime image.
        implementation(compose.desktop.macos_arm64)
      }
    }
    val jvmTest by getting
  }
}

val jvmMainSourceSet = the<SourceSetContainer>().getByName("jvmMain")
val jvmJarTask = tasks.named<Jar>("jvmJar")
val filteredRuntimeJarsDir = layout.buildDirectory.dir("compose/arm-only-runtime-jars")
val armOnlyMainJar = filteredRuntimeJarsDir.zip(jvmJarTask.flatMap { it.archiveFile }) { directory, mainJar ->
  directory.file(mainJar.asFile.name)
}

val prepareArmOnlyRuntimeJars by tasks.registering(Sync::class) {
  // Do not test file existence during configuration: project dependency jars may only be
  // produced later in this build. FileCollection.filter keeps their producer dependencies.
  val runtimeJars = jvmMainSourceSet.runtimeClasspath.filter { it.extension == "jar" }
  val mainJarSource = jvmJarTask.flatMap { it.archiveFile }

  inputs.files(runtimeJars)
  inputs.file(mainJarSource)
  from(runtimeJars)
  into(filteredRuntimeJarsDir)
  include("**/*.jar")
  eachFile {
    path = name
  }
  duplicatesStrategy = DuplicatesStrategy.FAIL
  includeEmptyDirs = false

  doLast {
    val destinationDir = filteredRuntimeJarsDir.get().asFile
    destinationDir.listFiles()
      ?.filter { it.isFile && it.extension == "jar" }
      ?.forEach { jarFile ->
        rewriteJarWithoutX64NativeEntries(jarFile, jarFile)
      }
    val mainJarFile = mainJarSource.get().asFile
    val filteredMainJar = destinationDir.resolve(mainJarFile.name)
    rewriteJarWithoutX64NativeEntries(mainJarFile, filteredMainJar) ?: Files.copy(
      mainJarFile.toPath(),
      filteredMainJar.toPath(),
      StandardCopyOption.REPLACE_EXISTING,
    )
  }
}

// https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Native_distributions_and_local_execution
compose {
  desktop {
    application {
      disableDefaultConfiguration()
      dependsOn("jvmJar", "prepareArmOnlyRuntimeJars")
      mainJar.set(armOnlyMainJar)
      fromFiles(project.fileTree(filteredRuntimeJarsDir))

      // For debugging via VisualVM
      val debugJava = false
      if (debugJava) {
        jvmArgs += listOf(
          "-Dcom.sun.management.jmxremote.port=8080",
          "-Dcom.sun.management.jmxremote.ssl=false",
          "-Dcom.sun.management.jmxremote.authenticate=false"
        )
      }
      mainClass = "chat.simplex.desktop.MainKt"
      nativeDistributions {
        copyright = "(c) 2020-2026 Nome"
        // For debugging via VisualVM
        if (debugJava) {
          modules("jdk.zipfs", "jdk.unsupported", "jdk.management.agent")
        } else {
          // 'jdk.unsupported' is for vlcj
          modules("jdk.zipfs", "jdk.unsupported")
        }
        //includeAllModules = true
        outputBaseDir.set(project.file("../release"))
        appResourcesRootDir.set(project.file("../build/links"))
        targetFormats(TargetFormat.Dmg)
        linux {
          iconFile.set(project.file("src/jvmMain/resources/distribute/simplex.png"))
          appCategory = "Messenger"
        }
        windows {
          packageName = "SimpleX"
          iconFile.set(project.file("src/jvmMain/resources/distribute/simplex.ico"))
          console = false
          perUserInstall = false
          dirChooser = true
          shortcut = true
          upgradeUuid = "CC9EFBC8-AFFF-40D8-BB69-FCD7CE99EFB9"
        }
        macOS {
          packageName = "Nome"
          minimumSystemVersion = "11.0"
          iconFile.set(project.file("src/jvmMain/resources/distribute/simplex.icns"))
          appCategory = "public.app-category.social-networking"
          bundleID = "chat.nome.app"
          infoPlist {
            extraKeysRawXml = """
              <key>NSMicrophoneUsageDescription</key>
              <string>Nome needs microphone access to record voice messages</string>
              <key>NSCameraUsageDescription</key>
              <string>Nome needs camera access for video calls</string>
            """
          }
          if (hasCompleteMacReleaseCredentials) {
            signing {
              sign.set(true)
              this.identity.set(macSigningIdentity!!)
              this.keychain.set(macSigningKeychain!!)
            }
            notarization {
              this.appleID.set(macNotarizationAppleId!!)
              this.password.set(macNotarizationPassword!!)
              this.teamID.set(macNotarizationTeamId!!)
            }
          }
        }
        packageName = "Nome"
        // Packaging requires to have version like MAJOR.MINOR.PATCH
        var adjustedVersion = rootProject.extra["desktop.version_name"] as String
        adjustedVersion = adjustedVersion.replace(Regex("[^0-9.]"), "")
        val split = adjustedVersion.split(".")
        adjustedVersion = split[0] + "." + (split.getOrNull(1) ?: "0") + "." + (split.getOrNull(2) ?: "0")
        version = adjustedVersion
      }
    }
  }
}

val cppPath = "../common/src/commonMain/cpp"

val prepareMacArm64AppResources by tasks.registering {
  // cmakeBuildAndCopy creates libapp-lib.dylib in the reviewed native tree. Serializing this
  // gate after that task prevents Gradle from racing JNI output against manifest verification.
  dependsOn("cmakeBuildAndCopy")
  val nativeResources = project.file("$cppPath/desktop/libs/mac-aarch64").toPath()
  val appResourcesLink = project.file("../build/links/macos-arm64").toPath()
  val nativeManifest = project.file("native/macos-arm64-native.sha256").toPath()
  val expectedManifestSha256 = providers.gradleProperty("nome.nativeManifestSha256")
  inputs.dir(nativeResources)
  inputs.file(nativeManifest)
  inputs.property("expectedManifestSha256", expectedManifestSha256.orElse("missing"))
  doLast {
    check(isMacArm64PackagingHost) {
      "Nome macOS packages can only be produced on an Apple-Silicon macOS host"
    }
    check(Files.isDirectory(nativeResources)) {
      "Missing macOS ARM64 native resources in $nativeResources"
    }
    check(Files.isRegularFile(nativeManifest, LinkOption.NOFOLLOW_LINKS)) {
      "Missing reviewed native manifest: $nativeManifest"
    }

    fun sha256(path: java.nio.file.Path): String {
      val digest = MessageDigest.getInstance("SHA-256")
      Files.newInputStream(path).use { input ->
        val buffer = ByteArray(1024 * 1024)
        while (true) {
          val count = input.read(buffer)
          if (count < 0) break
          digest.update(buffer, 0, count)
        }
      }
      return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun verifyArm64Dylib(path: Path, relativeName: String) {
      val header = ByteArray(8)
      Files.newInputStream(path).use { input ->
        check(input.read(header) == header.size) { "Truncated Mach-O dylib: $relativeName" }
      }
      val machO = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
      check(machO.int == 0xfeedfacf.toInt() && machO.int == 0x0100000c) {
        "Non-arm64 or unsupported Mach-O dylib in macOS ARM64 resources: $relativeName"
      }
    }

    val expectedManifestHash = expectedManifestSha256.orNull?.lowercase()
    check(expectedManifestHash?.matches(Regex("[0-9a-f]{64}")) == true) {
      "Packaging requires -Pnome.nativeManifestSha256=<reviewed manifest SHA-256>"
    }
    check(sha256(nativeManifest) == expectedManifestHash) {
      "macOS ARM64 native manifest does not match its reviewed SHA-256"
    }

    val expectedFiles = linkedMapOf<String, String>()
    Files.readAllLines(nativeManifest).forEachIndexed { index, rawLine ->
      val line = rawLine.trim()
      if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
      val match = Regex("^([0-9a-fA-F]{64})\\s{2}(.+)$").matchEntire(line)
      check(match != null) { "Invalid native manifest line ${index + 1}" }
      val (hash, relativeName) = match.destructured
      val relativePath = Path.of(relativeName)
      check(
        !relativePath.isAbsolute &&
          relativeName.isNotBlank() &&
          '\\' !in relativeName &&
          relativePath.normalize() == relativePath &&
          !relativePath.startsWith("..")
      ) {
        "Unsafe native manifest path at line ${index + 1}"
      }
      check(expectedFiles.put(relativeName, hash.lowercase()) == null) {
        "Duplicate native manifest entry: $relativeName"
      }
    }
    check("libsimplex.dylib" in expectedFiles && "libapp-lib.dylib" in expectedFiles) {
      "Native manifest must cover libsimplex.dylib and libapp-lib.dylib"
    }
    val resourcePaths = Files.walk(nativeResources).use { paths -> paths.toList() }
    check(resourcePaths.none { Files.isSymbolicLink(it) }) {
      "Native resource tree must not contain symbolic links"
    }
    val actualFiles = resourcePaths
      .filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
      .map { nativeResources.relativize(it).toString() }
      .toSet()
    check(actualFiles == expectedFiles.keys) {
      "Native manifest/resource set mismatch; missing=${expectedFiles.keys - actualFiles}, unexpected=${actualFiles - expectedFiles.keys}"
    }
    expectedFiles.forEach { (relativeName, expectedHash) ->
      val resource = nativeResources.resolve(relativeName)
      check(Files.isRegularFile(resource, LinkOption.NOFOLLOW_LINKS)) {
        "Manifest entry is not a regular file: $relativeName"
      }
      check(sha256(resource) == expectedHash) {
        "$relativeName does not match the reviewed native manifest"
      }
      if (relativeName.endsWith(".dylib")) {
        verifyArm64Dylib(resource, relativeName)
      }
    }
    Files.createDirectories(appResourcesLink.parent)
    Files.copy(
      nativeManifest,
      appResourcesLink.parent.resolve("macos-arm64-native.sha256"),
      StandardCopyOption.REPLACE_EXISTING,
    )
    if (Files.exists(appResourcesLink, LinkOption.NOFOLLOW_LINKS)) {
      project.delete(appResourcesLink.toFile())
    }
    Files.createDirectories(appResourcesLink)
    resourcePaths.forEach { source ->
      if (source == nativeResources) return@forEach
      val target = appResourcesLink.resolve(nativeResources.relativize(source).toString())
      if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
        Files.createDirectories(target)
      } else {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
      }
    }

    // Package only the verified build-owned snapshot. Re-run every integrity and architecture
    // check after copying so source changes after the gate cannot race the packager.
    val stagedPaths = Files.walk(appResourcesLink).use { paths -> paths.toList() }
    check(stagedPaths.none { Files.isSymbolicLink(it) }) {
      "Staged native resource tree must not contain symbolic links"
    }
    val stagedFiles = stagedPaths
      .filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
      .map { appResourcesLink.relativize(it).toString() }
      .toSet()
    check(stagedFiles == expectedFiles.keys) {
      "Staged native resource set mismatch; missing=${expectedFiles.keys - stagedFiles}, unexpected=${stagedFiles - expectedFiles.keys}"
    }
    expectedFiles.forEach { (relativeName, expectedHash) ->
      val staged = appResourcesLink.resolve(relativeName)
      check(sha256(staged) == expectedHash) {
        "Staged $relativeName does not match the reviewed native manifest"
      }
      if (relativeName.endsWith(".dylib")) verifyArm64Dylib(staged, relativeName)
    }
  }
}

val guardedMacDistributionTasks = setOf(
  "createDistributable",
  "createReleaseDistributable",
  "packageDmg",
  "packageReleaseDmg",
  "packageDistributionForCurrentOS",
  "packageReleaseDistributionForCurrentOS",
)

tasks.matching { it.name in guardedMacDistributionTasks }.configureEach {
  dependsOn(prepareMacArm64AppResources)
  doFirst {
    check(isMacArm64PackagingHost) {
      "Nome macOS packages can only be produced on an Apple-Silicon macOS host"
    }
    check(!hasAnyMacReleaseCredential || hasCompleteMacReleaseCredentials) {
      "Partial macOS signing/notarization configuration is not allowed"
    }
    check(hasCompleteMacReleaseCredentials || allowAdHocMacPackage.get()) {
      "Public macOS packages require complete Developer ID and notarization credentials. " +
        "For an explicit local test artifact only, pass -Pnome.allowAdHocMacPackage=true."
    }
  }
}

tasks.matching { it.name == "prepareAppResources" }.configureEach {
  dependsOn(prepareMacArm64AppResources)
}

cmake {
  // Run this command to make build for all targets:
  // ./gradlew common:cmakeBuild -PcrossCompile
  if (project.hasProperty("crossCompile")) {
    machines.customMachines.register("linux-amd64") {
      toolchainFile.set(project.file("$cppPath/toolchains/x86_64-linux-gnu-gcc.cmake"))
    }
    /*machines.customMachines.register("linux-aarch64") {
      toolchainFile.set(project.file("$cppPath/toolchains/aarch64-linux-gnu-gcc.cmake"))
    }*/
    /*machines.customMachines.register("win-amd64") {
      toolchainFile.set(project.file("$cppPath/toolchains/x86_64-windows-mingw32-gcc.cmake"))
    }*/
    if (machines.host.name == "mac-amd64") {
      machines.customMachines.register("mac-amd64") {
        toolchainFile.set(project.file("$cppPath/toolchains/x86_64-mac-apple-darwin-gcc.cmake"))
      }
    }
    if (machines.host.name == "mac-aarch64") {
      machines.customMachines.register("mac-aarch64") {
        toolchainFile.set(project.file("$cppPath/toolchains/aarch64-mac-apple-darwin-gcc.cmake"))
      }
    }
  }
  val compileMachineTargets = arrayListOf<com.github.tomtzook.gcmake.targets.TargetMachine>(machines.host)
  compileMachineTargets.addAll(machines.customMachines)
  targets {
    val main by creating {
      cmakeLists.set(file("$cppPath/desktop/CMakeLists.txt"))
      targetMachines.addAll(compileMachineTargets.toSet())
      //if (machines.host.name.contains("win")) {
      //  cmakeArgs.add("-G MinGW Makefiles")
      //}
    }
  }
}

tasks.named("clean") {
  dependsOn("cmakeClean")
}
tasks.named("compileKotlinJvm") {
  dependsOn("cmakeBuildAndCopy")
}
afterEvaluate {
  tasks.create("cmakeBuildAndCopy") {
    dependsOn("cmakeBuild")
    doLast {
      copy {
        from("${project(":desktop").buildDir}/cmake/main/linux-amd64")
        into("$cppPath/desktop/libs/linux-x86_64")
        include("*.so*")
        eachFile {
          path = name
        }
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
      }
      copy {
        from("${project(":desktop").buildDir}/cmake/main/linux-aarch64")
        into("$cppPath/desktop/libs/linux-aarch64")
        include("*.so*")
        eachFile {
          path = name
        }
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
      }
      copy {
        from("${project(":desktop").buildDir}/cmake/main/windows-amd64")
        into("$cppPath/desktop/libs/windows-x86_64")
        include("*.dll")
        eachFile {
          path = name
        }
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
      }
	  copy {
        from("${project(":desktop").buildDir}/cmake/main/windows-amd64")
        into("../build/links/windows-x64")
        include("*.dll")
        eachFile {
          path = name
        }
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
      }
      copy {
        from("${project(":desktop").buildDir}/cmake/main/mac-x86_64")
        into("$cppPath/desktop/libs/mac-x86_64")
        include("*.dylib")
        eachFile {
          path = name
        }
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
      }
      copy {
        from("${project(":desktop").buildDir}/cmake/main/mac-aarch64")
        into("$cppPath/desktop/libs/mac-aarch64")
        include("*.dylib")
        eachFile {
          path = name
        }
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
      }
    }
  }
}
