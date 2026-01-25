plugins {
    id("java-library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.metro)
}

dependencies {
    implementation(libs.jna)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.kotlinx.collections.immutable)
    implementation(compose.runtime)
    implementation(libs.skydoves.compose.stable.marker)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.okhttp)
    implementation(projects.config)
    // Compatibility classes for Android specifics that we don't care about but got from Element X files,
    // so we don't need to change those classes too much.
    // In some case used as supertype so mark as api().
    api(projects.shim)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
    sourceSets {
        named("main") {
            kotlin.srcDirs(
                layout.projectDirectory.dir("../matrix-rust-sdk/target/generated-bindings"),
                layout.projectDirectory.dir("src/main/workmanager-shim"),
            )
            dependencies {
                implementation(libs.kermit)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

val rustSdkDir = layout.projectDirectory.dir("../matrix-rust-sdk").asFile
val generatedBindingsDir = rustSdkDir.resolve("target/generated-bindings")

// Decide which Cargo profile to use for the Rust FFI build (debug or release)
// Evaluate at configuration time to avoid capturing gradle.startParameter in configuration cache
val isReleaseBuild: Boolean = run {
    // Priority 1: explicit property -PrustProfile=release|debug or -PreleaseBuild=true
    val explicitProfile = (project.findProperty("rustProfile") as String?)?.lowercase()
    val explicitReleaseFlag = (project.findProperty("releaseBuild") as String?)?.toBoolean()
    when {
        explicitProfile == "release" -> true
        explicitProfile == "debug" -> false
        explicitReleaseFlag == true -> true
        else -> {
            // Heuristic from task names: package/distribute/release means release
            gradle.startParameter.taskNames.any {
                val n = it.lowercase()
                n.contains("release") || n.contains("package") || n.contains("distribut")
            }
        }
    }
}

val rustProfile: String = if (isReleaseBuild) "release" else "debug"

// Resolve platform-specific ffi library name
val ffiLibName: String = run {
    val os = org.gradle.internal.os.OperatingSystem.current()
    when {
        os.isWindows -> "matrix_sdk_ffi.dll"
        os.isMacOsX -> "libmatrix_sdk_ffi.dylib"
        else -> "libmatrix_sdk_ffi.so"
    }
}

// FFI library path depends on cargo profile
val ffiLib = rustSdkDir.resolve("target/${rustProfile}/${ffiLibName}")

val buildSdk = tasks.register<Exec>("buildSdk") {
    description = "Build matrix-sdk-ffi"
    group = "build"
    workingDir = rustSdkDir
    if (isReleaseBuild) {
        commandLine("cargo", "build", "--release", "-p", "matrix-sdk-ffi", "--features", "rustls-tls")
    } else {
        commandLine("cargo", "build", "-p", "matrix-sdk-ffi", "--features", "rustls-tls")
    }

    // Skip rebuilding FFI if the SDK did not change, since uniffi-bindgen is very slow and doesn't
    // notice nothing changed.
    // TODO this may miss changes in the SDK that didn't change cargo files (e.g. SDK changes without version bump)
    inputs.files(rustSdkDir.resolve("Cargo.toml"), rustSdkDir.resolve("Cargo.lock"))
    outputs.file(ffiLib)
}

val generateFfiBindings = tasks.register<Exec>("generateFfiBindings") {
    description = "Generate Kotlin bindings for the Rust SDK"
    group = "build"
    workingDir = rustSdkDir

    commandLine(
        "cargo", "run",
        "-p", "uniffi-bindgen",
        "--", "generate",
        "--no-format",
        "--library",
        "--language", "kotlin",
        "--out-dir", "target/generated-bindings",
        ffiLib.absolutePath
    )

    inputs.file(ffiLib)
    inputs.file(rustSdkDir.resolve("bindings/matrix-sdk-ffi/uniffi.toml"))
    outputs.dir(generatedBindingsDir)
}

// Set up SDK dependencies
generateFfiBindings.configure { dependsOn(buildSdk) }
tasks.named("compileKotlin").configure {
    dependsOn(generateFfiBindings)
}
