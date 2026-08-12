package chat.schildi.revenge.glue

import coil3.PlatformContext

internal expect fun applicationPlatformContext(): PlatformContext

internal expect val platformOsDebugName: String

internal expect val platformApplicationId: String

internal expect val platformVersionName: String

internal expect val platformVersionCode: Long?
