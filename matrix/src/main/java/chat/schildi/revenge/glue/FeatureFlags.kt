package chat.schildi.revenge.glue

import io.element.android.libraries.featureflag.api.Feature

object FeatureFlags {
    object Threads : Feature { override val defaultValue = false }
    object OnlySignedDeviceIsolationMode : Feature { override val defaultValue = false }
    object EnableKeyShareOnInvite : Feature { override val defaultValue = false }
}
