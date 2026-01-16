package chat.schildi.revenge.model.conversation

import com.beeper.android.messageformat.MatrixBodyParseResult

// Pre-parsed doesn't attach LinkAnnotation.Url *yet* since that would hold text style,
// so use internal string annotation at this stage.
fun MatrixBodyParseResult.extractUrls(): List<String> = text.getStringAnnotations(
    "mx:WEB_LINK",
    0,
    text.length
).map {
    it.item
}
