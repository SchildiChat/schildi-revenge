package chat.schildi.revenge.compose.destination.split

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import chat.schildi.revenge.Destination
import chat.schildi.revenge.DestinationStateHolder
import chat.schildi.revenge.LocalDestinationState
import chat.schildi.revenge.actions.HierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.hierarchicalKeyboardActionProvider
import chat.schildi.revenge.actions.isOnlyShiftPressed

class SplitKeyboardActionProvider(
    private val destinationStateHolder: DestinationStateHolder?,
    private val isPrimaryDestination: Boolean,
) : KeyboardActionProvider {
    override fun handleNavigationModeEvent(event: KeyEvent): Boolean {
        if (event.isOnlyShiftPressed) {
            when (event.key) {
                Key.Q ->  {
                    val currentDestination = destinationStateHolder?.state?.value?.destination
                    val newDestination = (currentDestination as? Destination.Split)?.let {
                        // Close the *current* destination -> navigate to the *other* destination
                        if (isPrimaryDestination)
                            it.secondary
                        else
                            it.primary

                    }?.state?.value?.destination ?: return false
                    destinationStateHolder.navigate(newDestination)
                    return true
                }
            }
        }
        return false
    }
}

@Composable
fun splitKeyboardActionProvider(isPrimaryDestination: Boolean): HierarchicalKeyboardActionProvider {
    val destinationStateHolder = LocalDestinationState.current
    return remember(destinationStateHolder,isPrimaryDestination) {
        SplitKeyboardActionProvider(destinationStateHolder, isPrimaryDestination)
    }.hierarchicalKeyboardActionProvider()
}
