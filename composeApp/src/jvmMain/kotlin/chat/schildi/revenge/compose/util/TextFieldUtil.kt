package chat.schildi.revenge.compose.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun TextFieldValue.insertAtCursor(insert: String): TextFieldValue {
    val newText = buildString {
        append(text.substring(0, selection.start))
        append(insert)
        append(text.substring(selection.end))
    }
    val newCursor = selection.start + insert.length
    return copy(
        text = newText,
        selection = TextRange(newCursor),
    )
}

fun insertTextFieldValue(text: String) = TextFieldValue(
    text = text,
    selection = TextRange(text.length),
)
