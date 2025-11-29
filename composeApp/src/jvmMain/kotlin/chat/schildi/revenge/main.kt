package chat.schildi.revenge

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import chat.schildi.revenge.compose.WindowContent
import chat.schildi.revenge.compose.media.LocalImageLoaderHolder
import chat.schildi.revenge.navigation.KeyboardNavigationHandler
import chat.schildi.revenge.navigation.LocalKeyboardNavigationHandler
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
            val keyHandler = remember(windowState.windowId) {
                KeyboardNavigationHandler(windowState.windowId)
            }
            val composeWindowState = rememberWindowState()
            Window(
                state = composeWindowState,
                onCloseRequest = {
                    UiState.closeWindow(windowState.windowId, this)
                },
                title = if (title != appTitle)
                    "$title - $appTitle"
                else
                    title,
                onPreviewKeyEvent = keyHandler::onPreviewKeyEvent,
                onKeyEvent = keyHandler::onKeyEvent,
            ) {
                // LocalFocusManager is not set outside the Window composable
                val focusManager = LocalFocusManager.current
                val density = LocalDensity.current
                LaunchedEffect(keyHandler, focusManager) {
                    keyHandler.focusManager = focusManager
                }
                LaunchedEffect(keyHandler, composeWindowState.size) {
                    keyHandler.windowCoordinates = density.run {
                        composeWindowState.size.toSize().toRect()
                    }
                }
                CompositionLocalProvider(
                    LocalDestinationState provides windowState.destinationHolder,
                    LocalImageLoaderHolder provides UiState.appGraph.imageLoaderHolder,
                    LocalKeyboardNavigationHandler provides keyHandler,
                ) {
                    WindowContent(destinationState.destination)
                }
            }
        }
    }
}
