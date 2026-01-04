package chat.schildi.revenge.compose.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink

fun AnnotatedString.Builder.appendUrlText(url: String?, text: String, linkStyle: TextLinkStyles) {
    if (url == null) {
        append(text)
    } else {
        withLink(
            LinkAnnotation.Url(
                url = url,
                styles = linkStyle,
            )
        ) {
            append(text)
        }
    }
}
