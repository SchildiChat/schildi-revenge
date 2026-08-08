import org.gradle.api.tasks.Exec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

val rustSdkDir = layout.projectDirectory.dir("../matrix-rust-sdk")
val generatedBindingsDir = layout.buildDirectory.dir("generated/uniffi/kotlin")

val isDesktopReleaseBuild = run {
    val explicitProfile = providers.gradleProperty("rustProfile").orNull?.lowercase()
    val explicitReleaseFlag = providers.gradleProperty("releaseBuild").orNull?.toBoolean()
    when {
        explicitProfile == "release" -> true
        explicitProfile == "debug" -> false
        explicitReleaseFlag == true -> true
        else -> gradle.startParameter.taskNames.any {
            val name = it.lowercase()
            name.contains("release") || name.contains("package") || name.contains("distribut")
        }
    }
}

val desktopRustProfile = if (isDesktopReleaseBuild) "release" else "debug"
val desktopLibraryName = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "matrix_sdk_ffi.dll"
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "libmatrix_sdk_ffi.dylib"
    else -> "libmatrix_sdk_ffi.so"
}
val desktopLibrary = rustSdkDir.file("target/$desktopRustProfile/$desktopLibraryName")

val buildDesktopSdk = tasks.register<Exec>("buildDesktopSdk") {
    description = "Build matrix-sdk-ffi for the desktop host"
    group = "build"
    workingDir = rustSdkDir.asFile
    commandLine(
        listOf("cargo", "build") +
            (if (isDesktopReleaseBuild) listOf("--release") else emptyList()) +
            listOf("--package", "matrix-sdk-ffi"),
    )
    inputs.files(rustSdkDir.file("Cargo.toml"), rustSdkDir.file("Cargo.lock"))
    outputs.file(desktopLibrary)
}

val generateFfiBindings = tasks.register<Exec>("generateFfiBindings") {
    description = "Generate Kotlin bindings for matrix-sdk-ffi"
    group = "build"
    dependsOn(buildDesktopSdk)
    workingDir = rustSdkDir.asFile
    commandLine(
        "cargo", "run",
        "--package", "uniffi-bindgen",
        "--", "generate",
        "--no-format",
        "--library",
        "--language", "kotlin",
        "--out-dir", generatedBindingsDir.get().asFile.absolutePath,
        desktopLibrary.asFile.absolutePath,
    )
    inputs.file(desktopLibrary)
    inputs.file(rustSdkDir.file("bindings/matrix-sdk-ffi/uniffi.toml"))
    outputs.dir(generatedBindingsDir)
}

val abiTargets = linkedMapOf(
    "arm64-v8a" to "aarch64-linux-android",
    "armeabi-v7a" to "armv7-linux-androideabi",
    "x86" to "i686-linux-android",
    "x86_64" to "x86_64-linux-android",
)

fun selectedAbis(buildType: String): List<String> {
    val configuredAbis = providers.gradleProperty("androidAbi")
        .orElse(providers.gradleProperty("android.injected.build.abi"))
        .orNull
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?.map { abi ->
            // Normalize armeabi
            if (abi == "armeabi") "armeabi-v7a" else abi
        }
        ?.distinct()
    val selected = configuredAbis?.takeIf(List<String>::isNotEmpty)
        ?: if (buildType == "release") abiTargets.keys.toList() else listOf("arm64-v8a")
    val unknown = selected - abiTargets.keys
    require(unknown.isEmpty()) {
        "Unsupported Android ABI(s): ${unknown.joinToString()}. Supported ABIs: ${abiTargets.keys.joinToString()}"
    }
    return selected.distinct()
}

fun registerAndroidRustBuild(buildType: String): TaskProvider<Task> {
    val rustProfileArgs = if (buildType == "release") {
        listOf("--release")
    } else {
        listOf("--profile", "reldev")
    }
    val abis = selectedAbis(buildType)
    val outputDir = layout.buildDirectory.dir("generated/android/$buildType/jniLibs")
    val abiTasks = abis.map { abi ->
        tasks.register<Exec>(
            "buildAndroid${buildType.replaceFirstChar(Char::uppercase)}${abi.replace("-", "").replaceFirstChar(Char::uppercase)}Sdk",
        ) {
            description = "Build matrix-sdk-ffi for Android $abi ($buildType)"
            group = "build"
            workingDir = rustSdkDir.asFile
            commandLine(
                listOf(
                    "cargo", "ndk",
                    "--target", abiTargets.getValue(abi),
                    "--output-dir", outputDir.get().asFile.absolutePath,
                    "build",
                ) + rustProfileArgs + listOf("--package", "matrix-sdk-ffi"),
            )
            inputs.files(rustSdkDir.file("Cargo.toml"), rustSdkDir.file("Cargo.lock"))
            outputs.file(outputDir.map { it.file("$abi/libmatrix_sdk_ffi.so") })
        }
    }

    return tasks.register("buildAndroid${buildType.replaceFirstChar(Char::uppercase)}Sdk") {
        description = "Build matrix-sdk-ffi for the selected Android $buildType ABIs"
        group = "build"
        dependsOn(abiTasks)
    }
}

registerAndroidRustBuild("debug")
registerAndroidRustBuild("release")

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    android {
        namespace = "org.matrix.rustcomponents.sdk"
        compileSdk = 37
        minSdk = 21
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    sourceSets {
        jvmMain {
            kotlin.srcDir(generateFfiBindings)
            dependencies {
                api(libs.jna)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        androidMain {
            kotlin.srcDir(generateFfiBindings)
            dependencies {
                api("net.java.dev.jna:jna:${libs.versions.jna.get()}@aar")
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}
