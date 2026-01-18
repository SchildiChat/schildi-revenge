package chat.schildi.revenge

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import chat.schildi.revenge.compose.components.LocalSessionId
import chat.schildi.theme.scExposures
import com.beeper.android.messageformat.DefaultMatrixBodyStyledFormatter
import com.beeper.android.messageformat.MENTION_ROOM
import com.beeper.android.messageformat.MatrixBodyDrawStyle
import com.beeper.android.messageformat.MatrixBodyPreFormatStyle
import com.beeper.android.messageformat.MatrixBodyStyledFormatter
import com.beeper.android.messageformat.MatrixHtmlParser
import com.beeper.android.messageformat.MatrixToLink

object MessageFormatDefaults {
    val blockIndention = 16.sp
    val parser: MatrixHtmlParser = MatrixHtmlParser()
    val parseStyle: MatrixBodyPreFormatStyle = MatrixBodyPreFormatStyle(
        formatRoomMention = {
            // Wrap in non-breakable space to add padding for background
            "\u00A0$MENTION_ROOM\u00A0"
        },
        formatUserMention = { _, content ->
            // Wrap in non-breakable space to add padding for background
            buildAnnotatedString {
                append("\u00A0")
                append(content)
                append("\u00A0")
            }
        },
    )
}

val LocalMatrixBodyFormatter = compositionLocalOf<MatrixBodyStyledFormatter> {
    throw IllegalStateException("Accessed uninitialized LocalMatrixBodyFormatter")
}
val LocalMatrixBodyDrawStyle = compositionLocalOf { MatrixBodyDrawStyle() }

@Composable
fun matrixBodyFormatter(): MatrixBodyStyledFormatter {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = Dimens.Conversation.textMessageStyle
    val linkColor = MaterialTheme.scExposures.linkColor
    val mentionColor = MaterialTheme.scExposures.mentionFg
    val mentionHighlightColor = MaterialTheme.scExposures.mentionFgHighlight
    val sessionId = LocalSessionId.current
    return remember(
        density,
        textMeasurer,
        textStyle,
        linkColor,
        mentionColor,
        mentionHighlightColor,
        sessionId
    ) {
        object : DefaultMatrixBodyStyledFormatter(
            density,
            textMeasurer,
            textStyle,
            urlStyle = TextLinkStyles(SpanStyle(color = linkColor)),
            blockIndention = MessageFormatDefaults.blockIndention,
        ) {
            override fun formatUserMention(mention: MatrixToLink.UserMention): List<AnnotatedString.Annotation>? {
                return if (sessionId?.value == mention.userId) {
                    listOf(SpanStyle(color = mentionHighlightColor, fontWeight = FontWeight.Bold))
                } else {
                    listOf(SpanStyle(color = mentionColor, fontWeight = FontWeight.Bold))
                }
            }
            override fun formatRoomMention() = listOf(
                SpanStyle(color = mentionHighlightColor, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun matrixBodyDrawStyle(): MatrixBodyDrawStyle {
    val mentionColor = MaterialTheme.scExposures.mentionBg
    val mentionHighlightColor = MaterialTheme.scExposures.mentionBgHighlight
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sessionId = LocalSessionId.current
    return remember(
        mentionColor,
        mentionHighlightColor,
        onSurfaceVariantColor,
        sessionId,
    ) {
        MatrixBodyDrawStyle(
            drawBehindRoomMention = { position ->
                drawRoundRect(
                    mentionHighlightColor,
                    topLeft = position.topLeft,
                    size = position.size,
                    cornerRadius = mentionBgRadius(),
                )
            },
            drawBehindUserMention = { userId, position ->
                val color = if (sessionId?.value == userId) {
                    mentionHighlightColor
                } else {
                    mentionColor
                }
                drawRoundRect(
                    color,
                    topLeft = position.topLeft,
                    size = position.size,
                    cornerRadius = mentionBgRadius(),
                )
            },
            drawBehindBlockQuote = { depth, position ->
                val barWidthDp = 4f
                drawRoundRect(
                    onSurfaceVariantColor,
                    topLeft = Offset((MessageFormatDefaults.blockIndention * (depth - 1)).toPx(), position.top),
                    size = Size(barWidthDp * density, position.height),
                    cornerRadius = CornerRadius(barWidthDp * density, barWidthDp * density),
                )
            },
        )
    }
}

private fun DrawScope.mentionBgRadius(): CornerRadius {
    val radius = Dimens.Conversation.mentionBgRadius * density
    return CornerRadius(radius, radius)
}
