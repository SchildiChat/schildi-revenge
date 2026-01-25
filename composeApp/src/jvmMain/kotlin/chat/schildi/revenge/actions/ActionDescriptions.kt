package chat.schildi.revenge.actions

import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.ActionArgument

fun Action.description(): ComposableStringHolder? {
    /*
    when (this) {
        // TODO add string resources to describe these all?
        else -> null
    }
     */
    // Hint arguments
    return if (args.isEmpty()) {
        null
    } else {
        args.joinToString(" ") { it.name }.toStringHolder()
    }
}

fun ActionArgument.description(): ComposableStringHolder? = when (this) {
    // TODO add string resources to describe these all
    else -> null
}
