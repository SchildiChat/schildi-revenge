package chat.schildi.revenge.model.conversation

import com.beeper.android.messageformat.MatrixBodyAnnotations
import com.beeper.android.messageformat.MatrixBodyParseResult
import com.beeper.android.messageformat.MatrixToLink
import kotlinx.serialization.json.Json

// Pre-parsed doesn't attach LinkAnnotation.Url *yet* since that would hold text style,
// so use internal string annotation at this stage.
fun MatrixBodyParseResult.extractUrls(): List<String> = text.getStringAnnotations(
    MatrixBodyAnnotations.WEB_LINK,
    0,
    text.length
).map {
    it.item
}

fun MatrixBodyParseResult.extractMatrixToLinks(): List<MatrixToLink> {
    return (
        text.getStringAnnotations(
            MatrixBodyAnnotations.ROOM_LINK,
            0,
            text.length
        ) +
                text.getStringAnnotations(
                    MatrixBodyAnnotations.MESSAGE_LINK,
                    0,
                    text.length
                ) +
                text.getStringAnnotations(
                    MatrixBodyAnnotations.USER_MENTION,
                    0,
                    text.length
                )
    ).mapNotNull {
        try {
            Json.decodeFromString<MatrixToLink>(it.item)
        } catch (_: Exception) {
            null
        }
    }
}
