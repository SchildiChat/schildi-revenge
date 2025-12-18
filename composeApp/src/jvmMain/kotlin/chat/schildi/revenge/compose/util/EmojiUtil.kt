package chat.schildi.revenge.compose.util

import org.kodein.emoji.EmojiFinder
import org.kodein.emoji.findEmoji

fun String.containsOnlyEmojis(): Boolean {
    val emojis = EmojiFinder().findEmoji(this)
    return emojis.sumOf {
        it.length
    } >= length
}
