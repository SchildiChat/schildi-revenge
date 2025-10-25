plugins {
    id("java-library")
    alias(libs.plugins.jetbrainsKotlinJvm)
}

dependencies {
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.jna)
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
            kotlin.srcDir(layout.projectDirectory.dir("../matrix-rust-sdk/target/generated-bindings"))
            dependencies {
                implementation(libs.kermit)
            }
        }
    }
}

val rustSdkDir = layout.projectDirectory.dir("../matrix-rust-sdk").asFile
val generatedBindingsDir = rustSdkDir.resolve("target/generated-bindings")

// Resolve platform-specific ffi library name
val ffiLibName: String = run {
    val os = org.gradle.internal.os.OperatingSystem.current()
    when {
        os.isWindows -> "matrix_sdk_ffi.dll"
        os.isMacOsX -> "libmatrix_sdk_ffi.dylib"
        else -> "libmatrix_sdk_ffi.so"
    }
}

// TODO what about release builds
val ffiLib = rustSdkDir.resolve("target/debug/${ffiLibName}")

val buildSdk = tasks.register<Exec>("buildSdk") {
    description = "Build matrix-sdk-ffi"
    group = "build"
    workingDir = rustSdkDir
    commandLine("cargo", "build", "-p", "matrix-sdk-ffi", "--features", "rustls-tls")

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
