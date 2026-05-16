package chat.schildi.revenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember

@Composable
fun viewModelKey(destination: Destination, allowShare: Boolean = false): String {
    return if (allowShare) {
        remember(destination) {
            destination.key()
        }
    } else {
        val destinationStateHolder = LocalDestinationState.current!!
        val holderKey = destinationStateHolder.state.collectAsState().value.holderId
        remember(holderKey, destination) {
            "$holderKey/${destination.key()}"
        }
    }
}
