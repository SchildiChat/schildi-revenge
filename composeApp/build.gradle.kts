import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlinxSerialization)
    id("GitOperations")
}
kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
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
            implementation(libs.clikt)
            implementation(libs.kdroidfilter.platformtools.darkmodedetector)
            implementation(libs.kdroidfilter.composenativetray)
            implementation(libs.kdroidfilter.knotify)
            implementation(libs.kdroidfilter.knotify.compose)
            implementation(libs.kodein.emojiKt)
            implementation(libs.ktor.core)
            implementation(libs.jsoup)
            implementation(libs.beeper.messageformat)
            implementation(libs.vanniktech.blurhash)

            implementation(projects.matrix)
            implementation(projects.config)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.dbus.java.core)
            implementation(libs.dbus.java.transport.native.unixsocket)
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
val composeResourcesDir = layout.projectDirectory.dir("src/jvmMain/composeResources")

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

abstract class GenerateAvailableLocalesTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val composeResourcesDirectory: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val localeCodes = composeResourcesDirectory.get().asFile
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isDirectory }
            .filter(::hasNonEmptyStringsXml)
            .mapNotNull { directory ->
                directory.name.removePrefix("values-")
                    .takeIf { directory.name.startsWith("values-") }
                    ?.let(::normalizeResourceLocale)
            }
            .distinct()
            .sorted()
            .toList()

        val outFile = outputFile.get().asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(
            buildString {
                appendLine("package ${packageName.get()}")
                appendLine()
                appendLine("object AvailableLocales {")
                append("    val codes: List<String> = listOf(")
                if (localeCodes.isNotEmpty()) {
                    appendLine()
                    localeCodes.forEach { code ->
                        appendLine("        \"$code\",")
                    }
                    appendLine("    )")
                } else {
                    appendLine(")")
                }
                appendLine("}")
            }
        )
    }

    private fun normalizeResourceLocale(qualifier: String): String {
        if (qualifier.startsWith("b+")) {
            return qualifier.removePrefix("b+").replace('+', '-')
        }

        return qualifier.split('-')
            .filter { it.isNotEmpty() }
            .mapIndexed { index, part ->
                when {
                    index == 1 && part.startsWith("r") && part.length > 1 -> part.removePrefix("r").uppercase()
                    part.length == 4 -> part.lowercase().replaceFirstChar(Char::titlecase)
                    index == 0 -> part.lowercase()
                    else -> part
                }
            }
            .joinToString("-")
    }

    private fun hasNonEmptyStringsXml(directory: File): Boolean {
        val stringsFile = File(directory, "strings.xml")
        if (!stringsFile.isFile) {
            return false
        }

        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        }
        val document = documentBuilderFactory.newDocumentBuilder().parse(stringsFile)
        val resources = document.documentElement ?: return false

        return (0 until resources.childNodes.length).any { index ->
            resources.childNodes.item(index).nodeType == org.w3c.dom.Node.ELEMENT_NODE
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

    buildTimestamp.set(LocalDateTime.now()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))

    val gitExt = project.extensions.getByName("git")
    @Suppress("UNCHECKED_CAST")
    val revisionProvider = (gitExt::class.java.getMethod("getFullRevision", String::class.java).invoke(gitExt, null) as Provider<String>)
    sourceRevision.set(revisionProvider)

    @Suppress("UNCHECKED_CAST")
    val sdkRevisionProvider = (gitExt::class.java.getMethod("getFullRevision", String::class.java).invoke(gitExt, "matrix-rust-sdk") as Provider<String>)
    sdkRevision.set(sdkRevisionProvider)

    outputFile.set(outFile)
}

val generateAvailableLocales = tasks.register<GenerateAvailableLocalesTask>("generateAvailableLocales") {
    description = "Generate AvailableLocales.kt from compose resource locales"
    group = "build"
    val pkg = "chat.schildi.revenge"
    val outDir = File(generatedSrcDir, pkg.replace('.', '/'))
    val outFile = File(outDir, "AvailableLocales.kt")

    composeResourcesDirectory.set(composeResourcesDir)
    packageName.set(pkg)
    outputFile.set(outFile)
}

// Add generated sources to jvmMain
kotlin.sourceSets.named("jvmMain") {
    kotlin.srcDir(generatedSrcDir)
}

// Ensure codegen runs before compiling JVM sources
tasks.named("compileKotlinJvm").configure {
    dependsOn(generateBuildInfo, generateAvailableLocales)
}

val calVer: String = ZonedDateTime.now(ZoneOffset.UTC)
    .format(DateTimeFormatter.ofPattern("yy.MM.dd"))

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
                TargetFormat.Deb,
                TargetFormat.AppImage,
                TargetFormat.Rpm,
                TargetFormat.Exe,
                TargetFormat.Msi,
                // TargetFormat.Dmg, // Needs Apple volunteers
            )
            packageName = "SchildiChatRevenge"
            packageVersion = calVer
            vendor = "SchildiChat"
            description = "SchildiChat Revenge"

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

                iconFile.set(project.file("src/jvmMain/composeResources/drawable-xxxhdpi/ic_launcher.png"))

                debMaintainer = "SpiritCroc <shire@spiritcroc.de>"
                rpmLicenseType = "AGPL-3.0-only"
            }
        }
    }
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

    dependsOn(":matrix:buildSdk")
}

tasks.matching {
    it.name == "prepareAppResources"
}.configureEach {
    dependsOn(copyNativeLib)
}
