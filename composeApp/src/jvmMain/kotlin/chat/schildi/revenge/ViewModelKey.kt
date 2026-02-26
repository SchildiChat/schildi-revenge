package chat.schildi.revenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember

@Composable
fun viewModelKey(destination: Destination): String {
    val destinationStateHolder = LocalDestinationState.current!!
    val holderKey = destinationStateHolder.state.collectAsState().value.holderId
    return remember(holderKey, destination) {
        "$holderKey/${destination.key()}"
    }
}
