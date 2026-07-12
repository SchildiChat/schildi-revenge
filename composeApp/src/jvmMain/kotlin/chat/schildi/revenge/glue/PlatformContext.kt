package chat.schildi.revenge.glue

import coil3.PlatformContext

internal expect fun applicationPlatformContext(): PlatformContext

internal expect val platformApplicationId: String
