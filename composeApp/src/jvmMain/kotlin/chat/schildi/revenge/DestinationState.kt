package chat.schildi.revenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import chat.schildi.revenge.navigation.ComposableStringHolder
import chat.schildi.revenge.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class DestinationStateHolder(
    val state: MutableStateFlow<DestinationState>,
) {
    fun navigate(destination: Destination) {
        state.value = DestinationState(destination)
    }

    companion object {
        fun forInitialDestination(
            destination: Destination,
            initialTitle: ComposableStringHolder? = null,
        ): DestinationStateHolder {
            return DestinationStateHolder(
                MutableStateFlow(
                    DestinationState(
                        destination,
                        initialTitle
                    )
                )
            )
        }
    }
}

data class DestinationState(
    val destination: Destination,
    val titleOverride: ComposableStringHolder? = null,
)

val LocalDestinationState = compositionLocalOf<DestinationStateHolder?> { null }

private sealed interface StringOverrideState {
    data object Uninitialized : StringOverrideState
    data class Override(val value: ComposableStringHolder?) : StringOverrideState
}

@Composable
fun publishTitle(provider: TitleProvider) {
    val destinationState = LocalDestinationState.current
    val title = provider.windowTitle
        .map { StringOverrideState.Override(it) }
        .collectAsState(StringOverrideState.Uninitialized).value
    LaunchedEffect(title) {
        if (title !is StringOverrideState.Override) return@LaunchedEffect
        destinationState?.state?.update {
            if (provider.verifyDestination(it.destination)) {
                it.copy(titleOverride = title.value)
            } else {
                it
            }
        }
    }
}
