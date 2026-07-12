package chat.schildi.revenge.glue

import coil3.PlatformContext

internal actual fun applicationPlatformContext(): PlatformContext = PlatformContext.INSTANCE

internal actual val platformApplicationId: String = "chat.schildi.revenge"
