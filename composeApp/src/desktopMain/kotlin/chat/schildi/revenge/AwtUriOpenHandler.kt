package chat.schildi.revenge

import co.touchlab.kermit.Logger
import java.awt.Desktop

internal object AwtUriOpenHandler {
    private val log = Logger.withTag("AwtUriOpenHandler")

    fun install() {
        runCatching {
            if (!Desktop.isDesktopSupported()) return
            val desktop = Desktop.getDesktop()
            if (!desktop.isSupported(Desktop.Action.APP_OPEN_URI)) return

            desktop.setOpenURIHandler { event ->
                UiState.headlessKeyboardActionHandler?.consumeLink(event.uri.toString())
            }
        }.onFailure { error ->
            log.e("Failed to install AWT URI open handler", error)
        }
    }
}
