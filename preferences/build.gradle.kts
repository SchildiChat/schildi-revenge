plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.datastore.preferences.core)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.collections.immutable)
            api(projects.res)
            implementation(libs.kermit)
        }
    }
}

android {
    namespace = "chat.schildi.lib.preferences"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }
}
