package chat.schildi.revenge.schildi_revenge

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import chat.schildi.revenge.matrix.SdkLoader

fun main() {
    SdkLoader.ensureLoaded()
    application(exitProcessOnExit = false) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Test",
        ) {
            App()
        }
    }
}