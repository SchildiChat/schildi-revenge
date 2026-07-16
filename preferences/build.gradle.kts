plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    android {
        namespace = "chat.schildi.lib.preferences"
        compileSdk = 37
        minSdk = 21
    }

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
