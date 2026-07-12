package chat.schildi.revenge.compose.destination

import org.jetbrains.skiko.SkikoProperties

actual fun platformRenderApi(): String = SkikoProperties.renderApi.toString()
