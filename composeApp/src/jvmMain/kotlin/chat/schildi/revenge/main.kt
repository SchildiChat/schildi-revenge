package chat.schildi.revenge

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import chat.schildi.revenge.matrix.SdkLoader
import chat.schildi.revenge.compose.WindowContent
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title

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
