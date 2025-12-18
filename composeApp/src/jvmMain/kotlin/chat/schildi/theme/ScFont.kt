package chat.schildi.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.Font
import shire.composeapp.generated.resources.`Inter_VariableFont_opsz,wght`
import shire.composeapp.generated.resources.NotoColorEmoji_Regular
import shire.composeapp.generated.resources.Res

@Composable
fun rememberInterFontFamily(): FontFamily {
    val font = Font(Res.font.`Inter_VariableFont_opsz,wght`)
    return remember(font) { FontFamily(font) }
}

@Composable
fun rememberEmojiFontFamily(): FontFamily {
    val emojiFont = Font(Res.font.NotoColorEmoji_Regular)
    return remember(emojiFont) { FontFamily(emojiFont) }
}
