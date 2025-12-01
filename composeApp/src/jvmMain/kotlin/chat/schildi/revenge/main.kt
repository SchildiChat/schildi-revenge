package chat.schildi.revenge

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import chat.schildi.revenge.compose.WindowContent
import chat.schildi.revenge.compose.media.LocalImageLoaderHolder
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.focus.FocusParent
import chat.schildi.revenge.compose.focus.LocalFocusParent
import co.touchlab.kermit.Logger
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title
import java.util.UUID
import kotlin.system.exitProcess

fun main() {
    SdkLoader.ensureLoaded()
    // Avoid ugly JVM crash dialog. May want to replace with our own branded crash screen later.
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        Logger.e("Schildi encountered a fatal error in ${t.name}", e)
        exitProcess(1)
    }
    application(exitProcessOnExit = false) {
        val minimized = UiState.minimizedToTray.collectAsState().value
        TrayIcon(isMinimized = minimized, setMinimized = UiState::setMinimized)
        if (!minimized) {
            val windows = UiState.windows.collectAsState().value
            val scope = rememberCoroutineScope()
            windows.forEach { windowState ->
                val destinationState = windowState.destinationHolder.state.collectAsState().value
                val appTitle = stringResource(Res.string.app_title)
                val title = destinationState.titleOverride?.render()
                    ?: destinationState.destination.title?.render()
                    ?: appTitle
                val keyHandler = remember(windowState.windowId) {
                    KeyboardActionHandler(windowState.windowId, scope)
                }
                val focusRoot = remember { FocusParent(UUID.randomUUID(), null) }
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
                    // TODO icon
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
                        LocalImageLoaderHolder provides UiState.appGraph.imageLoaderHolder,
                        LocalKeyboardActionHandler provides keyHandler,
                        LocalFocusParent provides focusRoot,
                    ) {
                        WindowContent(windowState.destinationHolder)
                    }
                }
            }
        }
    }
}
