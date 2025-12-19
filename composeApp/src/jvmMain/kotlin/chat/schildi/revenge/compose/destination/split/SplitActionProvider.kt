package chat.schildi.revenge.compose.destination.split

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.UiState
import chat.schildi.revenge.actions.HierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger

class SplitKeyboardActionProvider(
    private val destinationStateHolder: DestinationStateHolder?,
    private val isPrimaryDestination: Boolean,
) : KeyboardActionProvider {
    override fun handleNavigationModeEvent(key: KeyTrigger, currentDestinationName: String?): Boolean {
        return UiState.keybindingsConfig.value.split.execute(key, currentDestinationName) { binding ->
            when (binding.action) {
                Action.Split.Close -> {
                    val currentDestination = destinationStateHolder?.state?.value?.destination
                    val newDestination = (currentDestination as? Destination.Split)?.let {
                        // Close the *current* destination -> navigate to the *other* destination
                        if (isPrimaryDestination)
                            it.secondary
                        else
                            it.primary

                    }?.state?.value?.destination ?: return@execute false
                    destinationStateHolder.navigate(newDestination)
                    true
                }
            }
        }
    }
}

@Composable
fun splitKeyboardActionProvider(isPrimaryDestination: Boolean): HierarchicalKeyboardActionProvider {
    val destinationStateHolder = LocalDestinationState.current
    return remember(destinationStateHolder,isPrimaryDestination) {
        SplitKeyboardActionProvider(destinationStateHolder, isPrimaryDestination)
    }.hierarchicalKeyboardActionProvider()
}
