package chat.schildi.revenge

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.toSize
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.compose.WindowContent
import chat.schildi.revenge.compose.components.rememberScaledDensity
import chat.schildi.revenge.compose.media.LocalImageLoaderHolder
import chat.schildi.revenge.util.filepicker.AndroidFilePickerLauncher
import chat.schildi.theme.prefersDarkTheme
import chat.schildi.theme.scdMaterialColorScheme
import chat.schildi.theme.sclMaterialColorScheme
import co.touchlab.kermit.Logger
import kotlin.random.Random

@OptIn(ExperimentalComposeUiApi::class)
class MainActivity : ComponentActivity() {
    private val log = Logger.withTag("MainActivity")
    internal val filePickerLauncher = AndroidFilePickerLauncher(this)
    private var windowId: WindowId = Random.nextInt()
    private var destinationStateHolder: DestinationStateHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            !UiState.initialClientRestoreComplete.value
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        windowId = savedInstanceState?.getInt(EXTRA_WINDOW_ID, windowId) ?: windowId

        val destination = savedInstanceState?.getString(EXTRA_DESTINATION)?.let {
            Destination.deserializedFromString(it)
                .onFailure { log.e("Failed to deserialize saved destination", it) }
                .getOrNull()
        } ?: intent.getStringExtra(EXTRA_DESTINATION)?.let {
            Destination.deserializedFromString(it)
                .onFailure { log.e("Failed to deserialize initial destination", it) }
                .getOrNull()
        }

        val destinationStateHolder =
            androidWindowManager.register(windowId, destination, this)
        this.destinationStateHolder = destinationStateHolder

        setContent {
            val darkTheme = prefersDarkTheme()
            val navigationBarColor = (if (darkTheme) {
                scdMaterialColorScheme
            } else {
                sclMaterialColorScheme
            }).surface.copy(alpha = 0.5f).toArgb()
            SideEffect {
                enableEdgeToEdge(
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(navigationBarColor)
                    } else {
                        SystemBarStyle.light(navigationBarColor, navigationBarColor)
                    },
                )
            }

            val scope = rememberCoroutineScope()
            val handler = remember { KeyboardActionHandler(scope, windowId) }
            LaunchedEffect(handler) { keyHandler = handler }
            val focusManager = LocalFocusManager.current
            val clipboard = LocalClipboard.current
            val uriHandler = LocalUriHandler.current
            val density = LocalDensity.current
            LaunchedEffect(handler, focusManager) { handler.focusManager = focusManager }
            LaunchedEffect(handler, clipboard) { handler.clipboard = clipboard }
            LaunchedEffect(handler, uriHandler) { handler.uriHandler = uriHandler }

            // Scaling settings
            val localDensity = rememberScaledDensity()
            CompositionLocalProvider(
                LocalImageLoaderHolder provides UiState.appGraph.imageLoaderHolder,
                LocalKeyboardActionHandler provides handler,
                LocalDensity provides localDensity,
            ) {
                key(UiState.currentLocale.collectAsState().value) {
                    WindowContent(
                        destinationStateHolder,
                        modifier = androidx.compose.ui.Modifier.onSizeChanged {
                            handler.windowCoordinates = density.run { it.toSize().toRect() }
                        },
                    )
                }
            }
        }
    }

    private var keyHandler: KeyboardActionHandler? = null

    override fun onResume() {
        super.onResume()
        keyHandler?.onWindowFocusChanged(true)
        androidWindowManager.onResume(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_DESTINATION)?.let {
            Destination.deserializedFromString(it)
                .onSuccess { destination ->
                    destinationStateHolder?.navigate(destination, NavigationPreference.AUTO)
                }
                .onFailure { failure ->
                    log.e("Failed to deserialize new intent destination", failure)
                }
        }
    }

    override fun onPause() {
        keyHandler?.onWindowFocusChanged(false)
        androidWindowManager.onPause(this)
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        keyHandler?.onWindowFocusChanged(hasFocus)
    }

    override fun onDestroy() {
        filePickerLauncher.close()
        androidWindowManager.unregister(windowId)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_WINDOW_ID, windowId)
        destinationStateHolder?.let { stateHolder ->
            outState.putString(EXTRA_DESTINATION, stateHolder.state.value.destination.serializedToString())
        }
        super.onSaveInstanceState(outState)
    }

    companion object {
        internal const val EXTRA_DESTINATION = "destination"
        internal const val EXTRA_WINDOW_ID = "windowId"
    }
}
