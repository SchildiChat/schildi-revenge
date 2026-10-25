package chat.schildi.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import shire.res.generated.resources.Inter_VariableFont
import shire.res.generated.resources.NotoColorEmoji_Regular
import shire.res.generated.resources.Res

expect fun platformSupportsColorVectorFonts(): Boolean

@Composable
fun rememberScFontFamily(): FontFamily {
    // Check if emoji font is supported
    if (!platformSupportsColorVectorFonts()) {
        return interFontFamily()
    }
    // Inter first, with the bundled emoji font as fallback.
    val interFont = Font(Res.font.Inter_VariableFont)
    val emojiFont = Font(Res.font.NotoColorEmoji_Regular)
    return remember(interFont, emojiFont) { FontFamily(interFont, emojiFont) }
}

@Composable
fun rememberEmojiFontFamily(): FontFamily {
    // Check if emoji font is supported
    if (!platformSupportsColorVectorFonts()) {
        return interFontFamily()
    }
    // Prioritize emoji font over inter
    val interFont = Font(Res.font.Inter_VariableFont)
    val emojiFont = Font(Res.font.NotoColorEmoji_Regular)
    return remember(interFont, emojiFont) { FontFamily(emojiFont, interFont) }
}

@Composable
private fun interFontFamily(): FontFamily {
    val interFont = Font(Res.font.Inter_VariableFont)
    return remember(interFont) { FontFamily(interFont) }
}
