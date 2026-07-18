package chat.schildi.revenge.compose.media

import coil3.request.ImageRequest
import coil3.request.allowHardware

internal actual fun ImageRequest.Builder.configureForPixelAccess(): ImageRequest.Builder = allowHardware(false)
