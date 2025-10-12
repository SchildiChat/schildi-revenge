package chat.schildi.revenge.schildi_revenge

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "schildi_revenge",
    ) {
        App()
    }
}