package chat.schildi.revenge.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import chat.schildi.revenge.config.keybindings.KeyTrigger

val LocalKeyboardActionProvider = compositionLocalOf<HierarchicalKeyboardActionProvider?> { null }

@Composable
fun KeyboardActionProvider.hierarchicalKeyboardActionProvider(): HierarchicalKeyboardActionProvider {
    val instance = this
    val parent = LocalKeyboardActionProvider.current
    return remember(instance, parent) { HierarchicalKeyboardActionProvider(instance, parent) }
}

interface KeyboardActionProvider {
    fun handleNavigationModeEvent(key: KeyTrigger, currentDestinationName: String?): Boolean
}

data class HierarchicalKeyboardActionProvider(
    val instance: KeyboardActionProvider,
    val parent: HierarchicalKeyboardActionProvider?,
) : KeyboardActionProvider {
    override fun handleNavigationModeEvent(key: KeyTrigger, currentDestinationName: String?): Boolean {
        return instance.handleNavigationModeEvent(key, currentDestinationName) ||
                parent?.handleNavigationModeEvent(key, currentDestinationName) == true
    }
}
