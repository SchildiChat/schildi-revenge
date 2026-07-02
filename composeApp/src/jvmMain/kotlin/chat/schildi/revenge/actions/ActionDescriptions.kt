package chat.schildi.revenge.actions

import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.toStringHolder
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
