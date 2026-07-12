plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
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
        commonMain.dependencies {
            api(libs.appdirs)
            api(libs.compose.ui)
            api(libs.kotlinx.serialization.core)
            api(libs.androidx.datastore.preferences.core)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktoml.core)
        }
        androidMain {
            kotlin.srcDir("src/jvmMain/kotlin")
        }
    }
}

android {
    namespace = "chat.schildi.revenge.config"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }
}
