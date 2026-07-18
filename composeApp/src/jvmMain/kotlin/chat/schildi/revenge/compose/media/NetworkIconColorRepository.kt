package chat.schildi.revenge.compose.media

import androidx.compose.ui.graphics.Color
import chat.schildi.revenge.ScCoroutines
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.ui.media.MediaRequestData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

sealed interface NetworkIconColorState {
    data object Loading : NetworkIconColorState
    data object Unavailable : NetworkIconColorState
    data class Available(val color: Color) : NetworkIconColorState
}

@OptIn(ExperimentalAtomicApi::class)
object NetworkIconColorRepository {
    private const val EXTRACTION_SIZE = 32

    private val scope = ScCoroutines.scope(Dispatchers.IO, "NetworkIconColorRepository")
    private val states = AtomicReference<Map<String, MutableStateFlow<NetworkIconColorState>>>(emptyMap())

    fun colorFor(
        source: MediaSource,
        imageLoader: ImageLoader,
        platformContext: PlatformContext,
    ): StateFlow<NetworkIconColorState> {
        while (true) {
            val currentStates = states.load()
            currentStates[source.safeUrl]?.let { return it }
            val state = MutableStateFlow<NetworkIconColorState>(NetworkIconColorState.Loading)
            if (states.compareAndSet(currentStates, currentStates + (source.safeUrl to state))) {
                scope.launch {
                    state.value = try {
                        val request = ImageRequest.Builder(platformContext)
                            .data(MediaRequestData(source, MediaRequestData.Kind.Content))
                            .size(EXTRACTION_SIZE)
                            .configureForPixelAccess()
                            .build()
                        val result = imageLoader.execute(request)
                        (result as? SuccessResult)
                            ?.image
                            ?.let(::extractDominantColor)
                            ?.let(NetworkIconColorState::Available)
                            ?: NetworkIconColorState.Unavailable
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        NetworkIconColorState.Unavailable
                    }
                }
                return state
            }
        }
    }
}

// Android needs to disable HW bitmaps to read pixel colors
internal expect fun ImageRequest.Builder.configureForPixelAccess(): ImageRequest.Builder
