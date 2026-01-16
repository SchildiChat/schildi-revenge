import java.net.URI

rootProject.name = "shire"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()

        maven {
            url = URI("https://maven.spiritcroc.de")
            content {
                includeGroupAndSubgroups("chat.schildi")
            }
        }

        maven {
            url = URI("https://www.jitpack.io")
            content {
                includeGroupAndSubgroups("com.github")
            }
        }

        mavenLocal {
            mavenContent {
                includeGroupAndSubgroups("com.beeper.android.messageformat")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
include(":config")
include(":matrix")
include(":shim")
