plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
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
            kotlin.srcDir("src/main/java")
            dependencies {
                implementation(libs.coil3.compose)
                implementation(libs.kermit)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        androidMain {
            kotlin.srcDir("src/main/java")
            kotlin.include("io/element/**")
            kotlin.include("timber/log/**")
            dependencies {
                implementation(libs.kermit)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

android {
    namespace = "chat.schildi.revenge.shim"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }
    sourceSets.named("main") {
        java.setSrcDirs(emptyList<String>())
    }
}
