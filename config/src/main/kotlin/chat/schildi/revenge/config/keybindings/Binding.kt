package chat.schildi.revenge.config.keybindings

import androidx.compose.ui.input.key.Key
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Binding<A : Action>(
    @SerialName("key")
    val rawKey: KeyMapped,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val ctrl: Boolean = false,
    val action: A,
) {
    val key: Key
        get() = rawKey.key
    val trigger: KeyTrigger
        get() = KeyTrigger(
            rawKey = rawKey,
            shift = shift,
            alt = alt,
            ctrl = ctrl,
        )
}

// Just the information necessary to find out *whether* an action should be triggered.
// Can also be used to find duplicates via structural equality.
data class KeyTrigger(
    val rawKey: KeyMapped,
    val shift: Boolean,
    val alt: Boolean,
    val ctrl: Boolean,
)
