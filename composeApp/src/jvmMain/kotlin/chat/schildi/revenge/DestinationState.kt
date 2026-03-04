package chat.schildi.revenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.compose.util.ComposableStringHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class NavigationPreference {
    AUTO,
    REPLACE,
    NEW_WINDOW,
}

interface DestinationStateHolder {

    val state: StateFlow<DestinationState>

    /**
     * Only use with no longer elsewhere used DestinationState!
     * Useful for moving nested destinations out, e.g. unsplit action
     */
    fun replaceWith(destinationState: chat.schildi.revenge.DestinationState)
    fun publishTitle(titleOverride: ComposableStringHolder?, verifyDestination: (Destination) -> Boolean)
    fun navigate(
        destination: Destination,
        preference: NavigationPreference = NavigationPreference.AUTO,
        invalidateHolderId: Boolean = false,
        initialTitle: ComposableStringHolder? = null,
    )
    fun closeScreen(keyHandler: KeyboardActionHandler)

    companion object {
        fun forInitialDestination(
            destination: Destination,
            initialTitle: ComposableStringHolder? = null,
        ): DestinationStateHolder {
            return DefaultDestinationStateHolder(
                DestinationState(
                    UUID.randomUUID(),
                    destination,
                    initialTitle,
                ),
            )
        }
    }
}

class DefaultDestinationStateHolder(
    initialDestination: DestinationState,
) : DestinationStateHolder {
    private val _state = MutableStateFlow(initialDestination)
    override val state = _state.asStateFlow()

    override fun navigate(
        destination: Destination,
        preference: NavigationPreference,
        invalidateHolderId: Boolean,
        initialTitle: ComposableStringHolder?,
    ) {
        when (preference) {
            NavigationPreference.REPLACE -> navigateThis(destination, invalidateHolderId)
            NavigationPreference.NEW_WINDOW -> UiState.openWindow(destination, initialTitle)
            NavigationPreference.AUTO -> {
                val currentCategory = state.value.destination.category
                when {
                    currentCategory == DestinationCategory.WILDCARD ||
                    currentCategory == destination.category -> navigateThis(destination, invalidateHolderId)
                    else -> UiState.openWindow(destination, initialTitle)
                }
            }
        }
    }

    private fun navigateThis(
        destination: Destination,
        invalidateHolderId: Boolean = false,
    ) {
        val holderId = if (invalidateHolderId) UUID.randomUUID() else null
        _state.update {
            DestinationState(
                holderId = holderId ?: it.holderId,
                destination = destination,
            )
        }
    }

    /**
     * Only use with no longer elsewhere used DestinationState!
     * Useful for moving nested destinations out, e.g. unsplit action
     */
    override fun replaceWith(destinationState: DestinationState) {
        _state.value = destinationState
    }

    override fun closeScreen(keyHandler: KeyboardActionHandler) = keyHandler.closeWindow()

    override fun publishTitle(titleOverride: ComposableStringHolder?, verifyDestination: (Destination) -> Boolean) {
        _state.update {
            if (verifyDestination(it.destination)) {
                it.copy(titleOverride = titleOverride)
            } else {
                it
            }
        }
    }
}

data class DestinationState(
    // Mutable ID allows moving destinations around, e.g. when entering split screen
    val holderId: UUID,
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
        destinationState?.publishTitle(title.value, provider::verifyDestination)
    }
}
