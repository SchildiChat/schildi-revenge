package chat.schildi.revenge

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.compose.WindowContent
import chat.schildi.revenge.compose.media.LocalImageLoaderHolder
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import co.touchlab.kermit.Logger
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title
import shire.composeapp.generated.resources.ic_launcher
import kotlin.system.exitProcess

@OptIn(ExperimentalComposeUiApi::class)
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
                val composeWindowState = rememberWindowState()
                // Changing transparency later will cause a crash, so require restarts and blocking read for that
                val hasTransparency = remember {
                    runBlocking {
                        RevengePrefs.getSetting(ScPrefs.BACKGROUND_ALPHA_LIGHT) < 1f ||
                                RevengePrefs.getSetting(ScPrefs.BACKGROUND_ALPHA_DARK) < 1f
                    }
                }
                key(windowState.windowId) {
                    val keyHandler = remember {
                        KeyboardActionHandler(scope, windowState.windowId, this)
                    }
                    Window(
                        state = composeWindowState,
                        onCloseRequest = {
                            UiState.closeWindow(windowState.windowId, this)
                        },
                        title = if (title != appTitle)
                            "$title - $appTitle"
                        else
                            title,
                        // TODO update icon
                        icon = painterResource(Res.drawable.ic_launcher),
                        onPreviewKeyEvent = keyHandler::onPreviewKeyEvent,
                        onKeyEvent = keyHandler::onKeyEvent,
                        transparent = hasTransparency,
                        decoration = WindowDecoration.Undecorated(),
                    ) {
                        // LocalFocusManager and LocalClipboard are not set outside the Window composable
                        val focusManager = LocalFocusManager.current
                        val clipboard = LocalClipboard.current
                        val uriHandler = LocalUriHandler.current
                        LaunchedEffect(keyHandler, focusManager) { keyHandler.focusManager = focusManager }
                        LaunchedEffect(keyHandler, clipboard) { keyHandler.clipboard = clipboard }
                        LaunchedEffect(keyHandler, uriHandler) { keyHandler.uriHandler = uriHandler }

                        // Scaling settings
                        val renderScale = ScPrefs.RENDER_SCALE.value()
                        val fontScale = ScPrefs.FONT_SCALE.value()
                        val rootDensity = LocalDensity.current
                        val localDensity = if (renderScale == 1f && fontScale == 1f) {
                            rootDensity
                        } else {
                            Density(
                                density = rootDensity.density * renderScale,
                                fontScale = rootDensity.fontScale * fontScale,
                            )
                        }

                        LaunchedEffect(keyHandler, composeWindowState.size) {
                            keyHandler.windowCoordinates = rootDensity.run {
                                composeWindowState.size.toSize().toRect()
                            }
                        }
                        CompositionLocalProvider(
                            LocalImageLoaderHolder provides UiState.appGraph.imageLoaderHolder,
                            LocalKeyboardActionHandler provides keyHandler,
                            LocalDensity provides localDensity,
                        ) {
                            WindowContent(windowState.destinationHolder)
                        }
                    }
                }
            }
        }
    }
}
