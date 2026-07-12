package chat.schildi.revenge

import chat.schildi.resources.ComposableStringHolder
import chat.schildi.revenge.Destination
import kotlinx.coroutines.flow.Flow

typealias WindowId = Int

interface WindowState {
    val destinationHolder: DestinationStateHolder
    val windowId: WindowId
}

interface TitleProvider {
    val windowTitle: Flow<ComposableStringHolder?>
    fun verifyDestination(destination: Destination): Boolean
}
