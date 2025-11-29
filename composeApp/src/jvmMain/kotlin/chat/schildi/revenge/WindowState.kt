package chat.schildi.revenge

import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.Destination
import kotlinx.coroutines.flow.Flow

data class WindowState(
    val destinationHolder: DestinationStateHolder,
    val windowId: Int,
)

interface TitleProvider {
    val windowTitle: Flow<ComposableStringHolder?>
    fun verifyDestination(destination: Destination): Boolean
}
