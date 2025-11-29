package chat.schildi.revenge.compose.components

import androidx.compose.ui.Modifier

fun Modifier.thenIf(condition: Boolean, block: Modifier.() -> Modifier) = let {
    if (condition) {
        it.block()
    } else {
        it
    }
}

fun <T>Modifier.ifNotNull(item: T?, block: Modifier.(T) -> Modifier) = let {
    if (item == null) {
        it
    } else {
        it.block(item)
    }
}
