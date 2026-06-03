package chat.schildi.revenge

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.value
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.WindowContent
import chat.schildi.revenge.compose.components.LocalWindowDiagnostics
import chat.schildi.revenge.compose.components.WindowDiagnostics
import chat.schildi.revenge.compose.media.LocalImageLoaderHolder
import chat.schildi.revenge.dbus.TrayWatcher
import chat.schildi.revenge.model.account.RevengeDeviceVerificationProvider
import chat.schildi.revenge.notification.NotificationProcessor
import chat.schildi.revenge.notification.Notifier
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.ic_launcher
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

@OptIn(ExperimentalComposeUiApi::class)
object ComposeApp {
    fun main(startInTray: Boolean, initialCommand: String? = null) {
        SdkLoader.ensureLoaded()
        TrayWatcher.start()
        NotificationProcessor.observeNotifications()
        RevengeDeviceVerificationProvider.observe()
        application(exitProcessOnExit = false) {
            // Blocking initialization
            remember {
                UiState.initializeWith(this@application, startInTray)
            }
            LaunchedEffect(Unit) {
                RevengePrefs.prefetch()
                Notifier.initialize()
                if (initialCommand != null) {
                    UiState.headlessKeyboardActionHandler?.executeCommandFromIpc(initialCommand)
                }
            }
            val minimized = UiState.minimizedToTray.collectAsState().value
            key(UiState.trayIconRecreationCounter.collectAsState().value) {
                TrayIcon(isMinimized = minimized, setMinimized = UiState::setMinimized)
            }
            key(UiState.forceRecreationCounter.collectAsState().value) {
                if (!minimized) {
                    val windows = UiState.windows.collectAsState().value
                    windows.forEach { windowState ->
                        key(windowState.windowId) {
                            val destinationState = windowState.destinationHolder.state.collectAsState().value
                            val title = destinationState.titleOverride?.render()
                                ?: destinationState.destination.title?.render()
                                ?: DEFAULT_WINDOW_APP_TITLE.render()
                            val initialWidth = ScPrefs.INITIAL_WINDOW_WIDTH.value()
                            val initialHeight = ScPrefs.INITIAL_WINDOW_HEIGHT.value()
                            val composeWindowState = rememberWindowState(
                                size = DpSize(
                                    initialWidth.dp,
                                    initialHeight.dp,
                                )
                            )
                            val hideDecoration = remember {
                                runBlocking {
                                    RevengePrefs.getSetting(ScPrefs.HIDE_WINDOW_DECORATION)
                                }
                            }
                            val scope = rememberCoroutineScope()
                            val keyHandler = remember {
                                KeyboardActionHandler(scope, windowState.windowId)
                            }
                            Window(
                                state = composeWindowState,
                                onCloseRequest = {
                                    UiState.closeWindow(windowState.windowId)
                                },
                                title = title,
                                icon = painterResource(Res.drawable.ic_launcher),
                                onPreviewKeyEvent = keyHandler::onPreviewKeyEvent,
                                onKeyEvent = keyHandler::onKeyEvent,
                                transparent = hideDecoration,
                                decoration = if (hideDecoration)
                                    WindowDecoration.Undecorated()
                                else
                                    WindowDecoration.SystemDefault,
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

                                DisposableEffect(window) {
                                    val listener = object : WindowAdapter() {
                                        override fun windowGainedFocus(e: WindowEvent) {
                                            keyHandler.onWindowFocusChanged(true)
                                        }

                                        override fun windowLostFocus(e: WindowEvent) {
                                            keyHandler.onWindowFocusChanged(false)
                                        }
                                    }

                                    window.addWindowFocusListener(listener)

                                    onDispose {
                                        window.removeWindowFocusListener(listener)
                                        keyHandler.onWindowFocusChanged(false)
                                    }
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
                                    LocalWindowDiagnostics provides remember(window) { WindowDiagnostics.from(window) },
                                ) {
                                    key(UiState.currentLocale.collectAsState().value) {
                                        WindowContent(windowState.destinationHolder)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
