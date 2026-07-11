plugins {
    id("java-library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsKotlinJvm)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.metro)
}

dependencies {
    api(projects.matrixRustBindings)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.compose.runtime)
    implementation(libs.skydoves.compose.stable.marker)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.okhttp)
    implementation(projects.config)
    implementation(projects.preferences)
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
                layout.projectDirectory.dir("src/main/workmanager-shim"),
            )
            dependencies {
                implementation(libs.kermit)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
