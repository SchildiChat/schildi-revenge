package chat.schildi.revenge

import chat.schildi.revenge.util.OperatingSystem
import chat.schildi.revenge.util.SystemInfo
import co.touchlab.kermit.Logger
import java.awt.Desktop

internal object MacOpenUriHandler {
    private val log = Logger.withTag("MacOpenUriHandler")

    fun install(consumer: (String) -> Unit) {
        check(SystemInfo.getOs() == OperatingSystem.Mac) {
            "The macOS URI handler must only be installed on macOS"
        }

        runCatching {
            check(Desktop.isDesktopSupported()) { "java.awt.Desktop is not supported" }
            val desktop = Desktop.getDesktop()
            check(desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
                "java.awt.Desktop APP_OPEN_URI is not supported"
            }
            desktop.setOpenURIHandler { event ->
                deeplinkCommandOrNull(event.uri.toString())?.let(consumer)
            }
        }.onFailure { error ->
            log.e("Failed to install macOS URI handler", error)
        }
    }
}
