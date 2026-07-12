package chat.schildi.revenge.glue

import coil3.PlatformContext
import chat.schildi.revenge.RevengeApplication

internal actual fun applicationPlatformContext(): PlatformContext = RevengeApplication.instance

internal actual val platformApplicationId: String
    get() = RevengeApplication.instance.packageName
