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
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.components.resources)
            api(libs.kotlinx.collections.immutable)
        }
    }
}

android {
    namespace = "chat.schildi.resources"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }
}

compose.resources {
    publicResClass = true
}

val generatedSrcDir = layout.buildDirectory.dir("generated/src/commonMain/kotlin").get().asFile
val composeResourcesDir = layout.projectDirectory.dir("src/commonMain/composeResources")

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

val generateAvailableLocales = tasks.register<GenerateAvailableLocalesTask>("generateAvailableLocales") {
    description = "Generate AvailableLocales.kt from shared resource locales"
    group = "build"
    val pkg = "chat.schildi.resources"
    val outDir = File(generatedSrcDir, pkg.replace('.', '/'))
    val outFile = File(outDir, "AvailableLocales.kt")

    composeResourcesDirectory.set(composeResourcesDir)
    packageName.set(pkg)
    outputFile.set(outFile)
}

kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(generatedSrcDir)
}

tasks.matching {
    it.name == "compileCommonMainKotlinMetadata" ||
        it.name == "compileKotlinJvm" ||
        (it.name.startsWith("compile") && it.name.endsWith("KotlinAndroid"))
}.configureEach {
    dependsOn(generateAvailableLocales)
}
