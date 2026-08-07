import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlinxSerialization)
    id("GitOperations")
}
kotlin {
    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
    jvm("desktop")
    android {
        namespace = "chat.schildi.revenge.compose"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(compose.material)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kermit)
            implementation(libs.coil3.compose)
            implementation(libs.coil3.okhttp)
            implementation(libs.kdroidfilter.platformtools.darkmodedetector)
            implementation(libs.kodein.emojiKt)
            implementation(libs.ktor.core)
            implementation(libs.beeper.messageformat)
            implementation(libs.vanniktech.blurhash)

            implementation(projects.preferences)
            implementation(projects.res)
            implementation(projects.config)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val jvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.jsoup)
                // We're not true KMP but still re-use some JVM-common code across desktop and Android targets.
                // Tell compiler where the code is so linting doesn't get confused about missing code.
                compileOnly(project(mapOf("path" to ":matrix", "configuration" to "jvmApiElements")))
            }
        }
        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
            implementation(projects.matrix)
            implementation(libs.kdroidfilter.composenativetray)
            implementation(libs.kdroidfilter.knotify)
            implementation(libs.kdroidfilter.knotify.compose)
            implementation(libs.clikt)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.dbus.java.core)
            implementation(libs.dbus.java.transport.native.unixsocket)
            }
        }
        val androidMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(projects.matrix)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.core.splashscreen)
                implementation(libs.androidx.lifecycle.process)
                // Provides attachAppDirs(), which initializes AppDirs with the Android context.
                implementation(libs.appdirs)
            }
        }
    }
}

// --- Build variant info (debug/release) and codegen for BuildInfo ---
// Evaluate at configuration time to avoid capturing gradle.startParameter in configuration cache
val isReleaseBuild: Boolean = run {
    val explicitProfile = (project.findProperty("rustProfile") as String?)?.lowercase()
    val explicitReleaseFlag = (project.findProperty("releaseBuild") as String?)?.toBoolean()
    when {
        explicitProfile == "release" -> true
        explicitProfile == "debug" -> false
        explicitReleaseFlag == true -> true
        else -> {
            // Heuristic: packaging/distribution or release task names imply release
            gradle.startParameter.taskNames.any {
                val n = it.lowercase()
                n.contains("release") || n.contains("package") || n.contains("distribut")
            }
        }
    }
}

val buildType: String = if (isReleaseBuild) "release" else "debug"
val rustProfile: String = if (isReleaseBuild) "release" else "debug"

val generatedSrcDir = layout.buildDirectory.dir("generated/src/jvmMain/kotlin").get().asFile
val composeResourcesDir = rootProject.layout.projectDirectory.dir("res/src/commonMain/composeResources")
val jvmResourcesDir = layout.projectDirectory.dir("src/jvmMain/resources")

val distributionResourcesDirName = "distribution-resources"
val distributionResourcesDir = layout.buildDirectory.dir(distributionResourcesDirName)
val resourceOsIdentifier: String = run {
    val os = org.gradle.internal.os.OperatingSystem.current()
    when {
        os.isWindows -> "windows"
        os.isMacOsX -> "macos"
        os.isLinux -> "linux"
        // ???
        else -> "common"
    }
}
val distributionResourcesOsDir = layout.buildDirectory.dir("$distributionResourcesDirName/$resourceOsIdentifier")

abstract class GenerateBuildInfoTask : DefaultTask() {
    @get:Input
    abstract val debugMode: Property<Boolean>

    @get:Input
    abstract val buildTypeValue: Property<String>

    @get:Input
    abstract val rustProfileValue: Property<String>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val buildTimestamp: Property<String>

    @get:Input
    abstract val sourceRevision: Property<String>

    @get:Input
    abstract val sdkRevision: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val outFile = outputFile.get().asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(
            """
            |package ${packageName.get()}
            |
            |object BuildInfo {
            |    const val DEBUG: Boolean = ${debugMode.get()}
            |    const val BUILD_TYPE: String = "${buildTypeValue.get()}"
            |    const val RUST_PROFILE: String = "${rustProfileValue.get()}"
            |    const val BUILD_TIMESTAMP: String = "${buildTimestamp.get()}"
            |    const val SOURCE_REVISION: String = "${sourceRevision.get()}"
            |    const val SDK_REVISION: String = "${sdkRevision.get()}"
            |}
            |""".trimMargin()
        )
    }
}

abstract class SyncLinuxPackageRootTask : DefaultTask() {
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:Input
    abstract val packageNameValue: Property<String>

    @get:Input
    abstract val xWaylandDesktopId: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val composeResourcesDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val desktopFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun syncRoot() {
        val packageName = packageNameValue.get()
        val pkgRoot = outputDirectory.get().asFile
        val optAppDir = pkgRoot.resolve("opt/$packageName")

        pkgRoot.deleteRecursively()
        pkgRoot.mkdirs()

        fileSystemOperations.copy {
            from(appDirectory)
            into(optAppDir)
        }
        installLauncher(pkgRoot, packageName)
        installDesktopEntries(pkgRoot, packageName, xWaylandDesktopId.get())
        installIcons(pkgRoot, packageName)
        logger.lifecycle("Synced Linux package root to ${pkgRoot.absolutePath}")
    }

    private fun installLauncher(pkgRoot: File, packageName: String) {
        val binDir = pkgRoot.resolve("usr/bin")
        binDir.mkdirs()
        val launcher = binDir.resolve(packageName)
        launcher.writeText(
            """
            |#!/bin/sh
            |exec /opt/$packageName/bin/${appDirectory.get().asFile.name} "$@"
            |
            """.trimMargin()
        )
        Files.setPosixFilePermissions(
            launcher.toPath(),
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE,
            )
        )
    }

    private fun installDesktopEntries(
        pkgRoot: File,
        packageName: String,
        xWaylandDesktopId: String,
    ) {
        val applicationsDir = pkgRoot.resolve("usr/share/applications")
        applicationsDir.mkdirs()

        val visibleEntry = desktopFile.get().asFile.readText()
        applicationsDir.resolve("$packageName.desktop").writeText(visibleEntry)
        applicationsDir.resolve("$xWaylandDesktopId.desktop").writeText(
            buildString {
                append(visibleEntry.trimEnd())
                appendLine()
                appendLine("NoDisplay=true")
            }
        )
    }

    private fun installIcons(pkgRoot: File, packageName: String) {
        val resourceDir = composeResourcesDirectory.get().asFile
        val icons = mapOf(
            "48x48" to "drawable-mdpi/ic_launcher.png",
            "72x72" to "drawable-hdpi/ic_launcher.png",
            "96x96" to "drawable-xhdpi/ic_launcher.png",
            "144x144" to "drawable-xxhdpi/ic_launcher.png",
            "192x192" to "drawable-xxxhdpi/ic_launcher.png",
        )

        icons.forEach { (size, relativePath) ->
            val source = resourceDir.resolve(relativePath)
            if (source.isFile) {
                val target = pkgRoot.resolve("usr/share/icons/hicolor/$size/apps/$packageName.png")
                target.parentFile.mkdirs()
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

}

val generateBuildInfo = tasks.register<GenerateBuildInfoTask>("generateBuildInfo") {
    description = "Generate BuildInfo.kt with build type and rust profile"
    group = "build"
    val pkg = "chat.schildi.revenge"
    val outDir = File(generatedSrcDir, pkg.replace('.', '/'))
    val outFile = File(outDir, "BuildInfo.kt")

    debugMode.set(!isReleaseBuild)
    buildTypeValue.set(buildType)
    rustProfileValue.set(rustProfile)
    packageName.set(pkg)

    buildTimestamp.set(
        providers.gradleProperty("buildTimestamp").orElse(
            providers.provider {
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            },
        ),
    )

    val gitExt = project.extensions.getByName("git")
    @Suppress("UNCHECKED_CAST")
    val revisionProvider = (gitExt::class.java.getMethod("getFullRevision", String::class.java).invoke(gitExt, null) as Provider<String>)
    sourceRevision.set(revisionProvider)

    @Suppress("UNCHECKED_CAST")
    val sdkRevisionProvider = (gitExt::class.java.getMethod("getFullRevision", String::class.java).invoke(gitExt, "matrix-rust-sdk") as Provider<String>)
    sdkRevision.set(sdkRevisionProvider)

    outputFile.set(outFile)
}

val persistDependencyLicenseReport = tasks.register<Sync>("persistDependencyLicenseReport") {
    description = "Update third-party Maven dependency license report in JVM resources"
    group = "build"

    dependsOn(rootProject.tasks.named("generateLicenseReport"))
    from(rootProject.layout.buildDirectory.dir("reports/dependency-license")) {
        include("third-party-libs.json")
    }
    into(jvmResourcesDir)
}

// Add generated sources to jvmMain
kotlin.sourceSets.named("jvmMain") {
    kotlin.srcDir(generatedSrcDir)
}

// Ensure codegen runs before compiling desktop and Android sources.
tasks.named("compileKotlinDesktop").configure {
    dependsOn(generateBuildInfo)
}
tasks.named("compileAndroidMain").configure {
    dependsOn(generateBuildInfo)
}

val calVer: String = ZonedDateTime.now(ZoneOffset.UTC)
    .format(DateTimeFormatter.ofPattern("yy.MM.dd"))
val composePackageName = "SchildiChatRevenge"
val linuxPackageName = "schildichat-revenge"
val linuxXWaylandDesktopId = "chat-schildi-revenge-MainKt"
val nativePackageName = if (org.gradle.internal.os.OperatingSystem.current().isLinux) {
    linuxPackageName
} else {
    composePackageName
}

compose.desktop {
    application {
        mainClass = "chat.schildi.revenge.MainKt"

        // ProGuard is broken, today can we fix?
        buildTypes {
            release {
                proguard {
                    isEnabled.set(false)
                }
            }
        }

        nativeDistributions {
            modules("java.management", "jdk.security.auth")
            targetFormats(
                TargetFormat.Exe,
                TargetFormat.Msi,
                // TargetFormat.Dmg, // Needs Apple volunteers
            )
            packageName = nativePackageName
            packageVersion = calVer
            vendor = "SchildiChat"
            description = "SchildiChat Revenge"

            fileAssociation(
                mimeType = "x-scheme-handler/matrix",
                extension = "matrix",
                description = "Matrix URI",
            )

            fileAssociation(
                mimeType = "x-scheme-handler/schildichat",
                extension = "schildichat",
                description = "SchildiChat Legacy URI",
            )

            appResourcesRootDir.set(distributionResourcesDir)

            windows {
                menu = true
                shortcut = true
                upgradeUuid = "7eeda045-d26f-475c-878f-497427b502e3"

                // Windows requires .ico
                iconFile.set(project.file("../graphics/ic_launcher.ico"))
            }

            linux {
                shortcut = true
                appCategory = "Network;Chat"

                iconFile.set(rootProject.file("res/src/commonMain/composeResources/drawable-xxxhdpi/ic_launcher.png"))

                debMaintainer = "SpiritCroc <shire@spiritcroc.de>"
                rpmLicenseType = "AGPL-3.0-only"
            }
        }
    }
}

val syncReleaseLinuxPackageRoot = tasks.register<SyncLinuxPackageRootTask>("syncReleaseLinuxPackageRoot") {
    description = "Build the common Linux package filesystem root used by nfpm"
    group = "distribution"

    packageNameValue.set(linuxPackageName)
    xWaylandDesktopId.set(linuxXWaylandDesktopId)
    appDirectory.set(layout.buildDirectory.dir("compose/binaries/main-release/app/$nativePackageName"))
    composeResourcesDirectory.set(composeResourcesDir)
    desktopFile.set(layout.projectDirectory.file("../launcher/$linuxPackageName.desktop"))
    outputDirectory.set(layout.buildDirectory.dir("compose/binaries/main-release/linux-package-root"))

    dependsOn(tasks.named("createReleaseDistributable"))
}

// Copy native library to distribution lib directory
val rustSdkDir = layout.projectDirectory.dir("../matrix-rust-sdk").asFile
val ffiLibName: String = run {
    val os = org.gradle.internal.os.OperatingSystem.current()
    when {
        os.isWindows -> "matrix_sdk_ffi.dll"
        os.isMacOsX -> "libmatrix_sdk_ffi.dylib"
        else -> "libmatrix_sdk_ffi.so"
    }
}

val copyNativeLib = tasks.register<Sync>("copyNativeLibToDistribution") {
    description = "Copy native matrix-sdk-ffi library to distribution lib directory"
    group = "distribution"

    val ffiLib = rustSdkDir.resolve("target/${rustProfile}/${ffiLibName}")
    from(ffiLib)

    into(distributionResourcesOsDir)

    dependsOn(":matrixRustBindings:buildDesktopSdk")
}

tasks.matching {
    it.name == "prepareAppResources"
}.configureEach {
    dependsOn(copyNativeLib)
}
