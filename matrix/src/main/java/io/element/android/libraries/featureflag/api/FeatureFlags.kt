package io.element.android.libraries.featureflag.api

// Just a SC shim, default values will be used as hard-coded values throughout the app
object FeatureFlags {
    object Threads : Feature { override val defaultValue = false }
    object OnlySignedDeviceIsolationMode : Feature { override val defaultValue = false }
    object AutomaticBackPagination : Feature { override val defaultValue = true }
    object MessageSearch : Feature { override val defaultValue = false }
}
