package chat.schildi.revenge.glue

import coil3.PlatformContext

internal actual fun applicationPlatformContext(): PlatformContext = PlatformContext.INSTANCE

internal actual val platformApplicationId: String = "chat.schildi.revenge"

internal actual val platformVersionName: String = System.getProperty("jpackage.app-version") ?: "0.0.0-dev"

internal actual val platformVersionCode: Long? = null
