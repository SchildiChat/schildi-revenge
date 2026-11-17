import org.gradle.api.tasks.Exec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

val rustRtcDir = layout.projectDirectory.dir("../matrix-rust-rtc")
val generatedJvmBindingsDir = layout.buildDirectory.dir("generated/uniffi/jvm/kotlin")
val generatedAndroidBindingsDir = layout.buildDirectory.dir("generated/uniffi/android/kotlin")
val uniffiAndroidConfig = rustRtcDir.file("crates/matrix-rtc-ffi/uniffi-android.toml")

val isDesktopReleaseBuild = run {
    val explicitProfile = providers.gradleProperty("rustProfile").orNull?.lowercase()
    val explicitReleaseFlag = providers.gradleProperty("releaseBuild").orNull?.toBoolean()
    when {
        explicitProfile == "release" -> true
        explicitProfile == "reldev" -> false
        explicitProfile == "debug" -> false
        explicitReleaseFlag == true -> true
        else -> gradle.startParameter.taskNames.any {
            val name = it.lowercase()
            name.contains("release") || name.contains("package") || name.contains("distribut")
        }
    }
}

val desktopRustProfile = if (isDesktopReleaseBuild) "release" else "debug"
val desktopRustProfileArgs = if (isDesktopReleaseBuild) listOf("--release") else emptyList()
val desktopLibraryName = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "matrix_rtc_ffi.dll"
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "libmatrix_rtc_ffi.dylib"
    else -> "libmatrix_rtc_ffi.so"
}
val desktopLibrary = rustRtcDir.file("target/$desktopRustProfile/$desktopLibraryName")

val buildDesktopSdk = tasks.register<Exec>("buildDesktopSdk") {
    description = "Build matrix-rtc-ffi for the desktop host"
    group = "build"
    workingDir = rustRtcDir.asFile
    commandLine(
        listOf("cargo", "build") + desktopRustProfileArgs + listOf("--package", "matrix-rtc-ffi"),
    )
    inputs.dir(rustRtcDir.dir("crates"))
    inputs.file(rustRtcDir.file("Cargo.toml"))
    inputs.file(rustRtcDir.file("Cargo.lock"))
    outputs.file(desktopLibrary)
}

fun registerFfiBindingsGeneration(
    name: String,
    outDir: Provider<Directory>,
    androidCleaner: Boolean,
): TaskProvider<Exec> = tasks.register<Exec>(name) {
    description = "Generate Kotlin bindings for matrix-rtc-ffi ($name)"
    group = "build"
    dependsOn(buildDesktopSdk)
    workingDir = rustRtcDir.asFile
    val command = buildList {
        addAll(listOf("cargo", "run", "--package", "uniffi-bindgen", "--", "generate", "--no-format", "--library"))
        // `uniffi.toml` is platform-neutral so the generated code also compiles for the
        // desktop JVM; Android opts into `android.system.SystemCleaner` via the override.
        if (androidCleaner) {
            add("--config")
            add(uniffiAndroidConfig.asFile.path)
        }
        addAll(listOf("--language", "kotlin", "--out-dir", outDir.get().asFile.absolutePath))
        add(desktopLibrary.asFile.absolutePath)
    }
    commandLine(command)
    inputs.file(desktopLibrary)
    inputs.file(rustRtcDir.file("crates/matrix-rtc-ffi/uniffi.toml"))
    if (androidCleaner) {
        inputs.file(uniffiAndroidConfig)
    }
    outputs.dir(outDir)
}

val generateJvmFfiBindings = registerFfiBindingsGeneration("generateJvmFfiBindings", generatedJvmBindingsDir, androidCleaner = false)
val generateAndroidFfiBindings = registerFfiBindingsGeneration("generateAndroidFfiBindings", generatedAndroidBindingsDir, androidCleaner = true)

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
        emptyList()
    }
    val abis = selectedAbis(buildType)
    val outputDir = layout.buildDirectory.dir("generated/android/$buildType/jniLibs")
    val abiTasks = abis.map { abi ->
        tasks.register<Exec>(
            "buildAndroid${buildType.replaceFirstChar(Char::uppercase)}${abi.replace("-", "").replaceFirstChar(Char::uppercase)}Sdk",
        ) {
            description = "Build matrix-rtc-ffi for Android $abi ($buildType)"
            group = "build"
            workingDir = rustRtcDir.asFile
            commandLine(
                listOf(
                    "cargo", "ndk",
                    "--target", abiTargets.getValue(abi),
                    "--output-dir", outputDir.get().asFile.absolutePath,
                    "build",
                ) + rustProfileArgs + listOf("--package", "matrix-rtc-ffi"),
            )
            inputs.dir(rustRtcDir.dir("crates"))
            inputs.file(rustRtcDir.file("Cargo.toml"))
            inputs.file(rustRtcDir.file("Cargo.lock"))
            outputs.file(outputDir.map { it.file("$abi/libmatrix_rtc_ffi.so") })
        }
    }

    return tasks.register("buildAndroid${buildType.replaceFirstChar(Char::uppercase)}Sdk") {
        description = "Build matrix-rtc-ffi for the selected Android $buildType ABIs"
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
        namespace = "org.matrix.rtc"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    sourceSets {
        jvmMain {
            generatedKotlin.srcDir(generateJvmFfiBindings)
            dependencies {
                api(libs.jna)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        androidMain {
            generatedKotlin.srcDir(generateAndroidFfiBindings)
            dependencies {
                api("net.java.dev.jna:jna:${libs.versions.jna.get()}@aar")
                implementation(libs.kotlinx.coroutines.core)
                // The generated android-cleaner code imports androidx.annotation.RequiresApi.
                implementation(libs.androidx.annotation)
            }
        }
    }
}
