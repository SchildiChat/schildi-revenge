import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
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
val injectedAndroidAbis = providers.gradleProperty("android.injected.build.abi").orNull
    ?.split(',')
    ?.map(String::trim)
    ?.filter(String::isNotEmpty)
    ?.distinct()
    ?.takeIf(List<String>::isNotEmpty)
val unknownAndroidAbis = injectedAndroidAbis.orEmpty() - supportedAndroidAbis
require(unknownAndroidAbis.isEmpty()) {
    "Unsupported Android ABI(s): ${unknownAndroidAbis.joinToString()}. Supported ABIs: ${supportedAndroidAbis.joinToString()}"
}

android {
    namespace = "chat.schildi.revenge"
    compileSdk = 37

    defaultConfig {
        applicationId = "chat.schildi.revenge"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            ndk.abiFilters += injectedAndroidAbis ?: listOf("arm64-v8a")
        }
        release {
            ndk.abiFilters += injectedAndroidAbis ?: supportedAndroidAbis
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
        variant.sources.jniLibs?.addGeneratedSourceDirectory(syncDebugNativeLibraries, SyncNativeLibraries::outputDirectory)
    }
    onVariants(selector().withBuildType("release")) { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(syncReleaseNativeLibraries, SyncNativeLibraries::outputDirectory)
    }
}
