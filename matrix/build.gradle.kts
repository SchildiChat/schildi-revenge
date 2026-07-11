plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlinParcelize)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    sourceSets {
        jvmMain {
            kotlin.srcDirs(
                "src/main/java",
                "src/main/workmanager-shim",
            )
            dependencies {
                api(projects.matrixRustBindings)
                // Compatibility classes for Android specifics that we don't care about but got from Element X files,
                // so we don't need to change those classes too much.
                // In some cases used as supertype so mark as api().
                api(projects.shim)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.compose.runtime)
                implementation(libs.skydoves.compose.stable.marker)
                implementation(libs.coil3.compose)
                implementation(libs.coil3.okhttp)
                implementation(projects.config)
                implementation(projects.preferences)
                implementation(libs.kermit)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        androidMain {
            kotlin.srcDirs(
                "src/main/java",
                "src/main/workmanager-shim",
            )
            dependencies {
                api(projects.matrixRustBindings)
                api(projects.shim)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.compose.runtime)
                implementation(libs.skydoves.compose.stable.marker)
                implementation(libs.coil3.compose)
                implementation(libs.coil3.okhttp)
                implementation(libs.coil3.gif)
                implementation(libs.androidx.core.ktx)
                implementation(projects.config)
                implementation(projects.preferences)
                implementation(libs.kermit)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

android {
    namespace = "chat.schildi.revenge.matrix"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }
    sourceSets.named("main") {
        java.setSrcDirs(emptyList<String>())
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
