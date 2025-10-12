import org.jetbrains.compose.desktop.application.dsl.TargetFormat

import gobley.gradle.GobleyHost
import gobley.gradle.cargo.dsl.*

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)

    id("dev.gobley.cargo") version libs.versions.gobley
    id("dev.gobley.uniffi") version libs.versions.gobley
    kotlin("plugin.atomicfu") version libs.versions.kotlin
}

uniffi {
    generateFromUdl {
        namespace = "org.matrix.rustcomponents.sdk"
        udlFile = layout.projectDirectory.file("../matrix-rust-sdk/bindings/matrix-sdk-ffi/src/api.udl")
    }
}

cargo {
    packageDirectory = layout.projectDirectory.dir("../matrix-rust-sdk/bindings/matrix-sdk-ffi")
    features.addAll("rustls-tls")
    builds.jvm {
        // This builds only for the current host architecture
        embedRustLibrary = (rustTarget == GobleyHost.current.rustTarget)
    }
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "chat.schildi.revenge.schildi_revenge.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "chat.schildi.revenge.schildi_revenge"
            packageVersion = "1.0.0"
        }
    }
}
