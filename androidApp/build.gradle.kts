import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

plugins {
    alias(libs.plugins.androidApplication)
}

abstract class SyncNativeLibraries : DefaultTask() {
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:InputDirectory
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun sync() {
        fileSystemOperations.sync {
            from(inputDirectory)
            into(outputDirectory)
        }
    }
}

val supportedAndroidAbis = listOf("arm64-v8a", "armeabi", "armeabi-v7a", "x86", "x86_64")
val selectedAndroidAbis = providers.gradleProperty("androidAbi")
    .orElse(providers.gradleProperty("android.injected.build.abi"))
    .orNull
    ?.split(',')
    ?.map(String::trim)
    ?.filter(String::isNotEmpty)
    ?.distinct()
    ?.takeIf(List<String>::isNotEmpty)
val unknownAndroidAbis = selectedAndroidAbis.orEmpty() - supportedAndroidAbis
require(unknownAndroidAbis.isEmpty()) {
    "Unsupported Android ABI(s): ${unknownAndroidAbis.joinToString()}. Supported ABIs: ${supportedAndroidAbis.joinToString()}"
}

val calVer = ZonedDateTime.now(ZoneOffset.UTC)
    .format(DateTimeFormatter.ofPattern("yy.MM.dd"))
val androidVersionNameOverride = providers.gradleProperty("androidVersionName")
val androidVersionCodeOverride = providers.gradleProperty("androidVersionCode").map(String::toInt)
val androidVersionName = androidVersionNameOverride.getOrElse(calVer)
val androidVersionCode = androidVersionCodeOverride.getOrElse("${calVer.replace(".", "")}00".toInt())

android {
    namespace = "chat.schildi.revenge"
    compileSdk = 37

    defaultConfig {
        applicationId = "chat.schildi.revenge"
        minSdk = 26
        targetSdk = 37
        versionCode = androidVersionCode
        versionName = androidVersionName
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            ndk.abiFilters += selectedAndroidAbis ?: listOf("arm64-v8a")
        }
        release {
            ndk.abiFilters += selectedAndroidAbis ?: supportedAndroidAbis
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.res)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

val matrixRustBindings = project(":matrixRustBindings")
val syncDebugNativeLibraries = tasks.register<SyncNativeLibraries>("syncDebugNativeLibraries") {
    dependsOn(":matrixRustBindings:buildAndroidDebugSdk")
    inputDirectory.set(matrixRustBindings.layout.buildDirectory.dir("generated/android/debug/jniLibs"))
    outputDirectory.set(layout.buildDirectory.dir("generated/jniLibs/debug"))
}
val syncReleaseNativeLibraries = tasks.register<SyncNativeLibraries>("syncReleaseNativeLibraries") {
    dependsOn(":matrixRustBindings:buildAndroidReleaseSdk")
    inputDirectory.set(matrixRustBindings.layout.buildDirectory.dir("generated/android/release/jniLibs"))
    outputDirectory.set(layout.buildDirectory.dir("generated/jniLibs/release"))
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        variant.outputs.forEach { output ->
            output.versionCode.set(androidVersionCodeOverride.orElse(1))
            output.versionName.set(androidVersionNameOverride.orElse("HEAD-$calVer"))
        }
        variant.sources.jniLibs?.addGeneratedSourceDirectory(syncDebugNativeLibraries, SyncNativeLibraries::outputDirectory)
    }
    onVariants(selector().withBuildType("release")) { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(syncReleaseNativeLibraries, SyncNativeLibraries::outputDirectory)
    }
}
