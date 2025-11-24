package chat.schildi.lib.preferences

// TODO
sealed interface ScPref<T> {
    val defaultValue: T
}

data class ScBoolPref(override val defaultValue: Boolean) : ScPref<Boolean>
