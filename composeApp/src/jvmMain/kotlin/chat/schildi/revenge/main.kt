package chat.schildi.revenge

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import chat.schildi.revenge.compose.WindowContent
import chat.schildi.revenge.compose.media.LocalImageLoaderHolder
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title
import kotlin.system.exitProcess

fun main() {
    SdkLoader.ensureLoaded()
    // Avoid ugly JVM crash dialog. May want to replace with our own branded crash screen later.
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        Logger.e("Schildi encountered a fatal error in ${t.name}", e)
        exitProcess(1)
    }
    application(exitProcessOnExit = false) {
        val windows = UiState.windows.collectAsState().value
        windows.forEach { windowState ->
            val destinationState = windowState.destinationHolder.state.collectAsState().value
            val appTitle = stringResource(Res.string.app_title)
            val title = destinationState.titleOverride?.render()
                ?: destinationState.destination.title?.render()
                ?: appTitle
            Window(
                onCloseRequest = {
                    UiState.closeWindow(windowState.windowId, this)
                },
                title = if (title != appTitle)
                    "$title - $appTitle"
                else
                    title
            ) {
                CompositionLocalProvider(
                    LocalDestinationState provides windowState.destinationHolder,
                    LocalImageLoaderHolder provides UiState.appGraph.imageLoaderHolder,
                ) {
                    WindowContent(destinationState.destination)
                }
            }
        }
    }
}
