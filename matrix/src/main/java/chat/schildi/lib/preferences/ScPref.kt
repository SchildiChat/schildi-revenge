package chat.schildi.lib.preferences

import androidx.compose.runtime.Composable

// TODO
sealed interface ScPref<T> {
    val defaultValue: T
}

data class ScBoolPref(override val defaultValue: Boolean) : ScPref<Boolean>

// TODO
@Composable
fun <T>ScPref<T>.value() = defaultValue
