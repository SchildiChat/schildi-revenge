package chat.schildi.revenge.schildi_revenge

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import chat.schildi.revenge.matrix.SdkLoader
import chat.schildi.revenge.schildi_revenge.compose.WindowContent
import org.jetbrains.compose.resources.stringResource
import schildi_revenge.composeapp.generated.resources.Res
import schildi_revenge.composeapp.generated.resources.app_title

fun main() {
    SdkLoader.ensureLoaded()
    application(exitProcessOnExit = false) {
        val windows = AppState.windows.collectAsState().value
        windows.forEach { windowState ->
            val destinationState = windowState.destination.state.collectAsState()
            Window(
                onCloseRequest = {
                    AppState.closeWindow(windowState.windowId, this)
                },
                title = destinationState.value.title?.render() ?: stringResource(Res.string.app_title)
            ) {
                WindowContent(destinationState.value)
            }
        }
    }
}
