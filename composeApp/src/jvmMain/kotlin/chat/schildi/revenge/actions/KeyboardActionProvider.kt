package chat.schildi.revenge.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.KeyEvent

val LocalKeyboardActionProvider = compositionLocalOf<HierarchicalKeyboardActionProvider?> { null }

@Composable
fun KeyboardActionProvider.hierarchicalKeyboardActionProvider(): HierarchicalKeyboardActionProvider {
    val instance = this
    val parent = LocalKeyboardActionProvider.current
    return remember(instance, parent) { HierarchicalKeyboardActionProvider(instance, parent) }
}

interface KeyboardActionProvider {
    fun handleNavigationModeEvent(event: KeyEvent): Boolean
}

data class HierarchicalKeyboardActionProvider(
    val instance: KeyboardActionProvider,
    val parent: HierarchicalKeyboardActionProvider?,
) : KeyboardActionProvider {
    override fun handleNavigationModeEvent(event: KeyEvent): Boolean {
        return instance.handleNavigationModeEvent(event) || parent?.handleNavigationModeEvent(event) == true
    }
}
