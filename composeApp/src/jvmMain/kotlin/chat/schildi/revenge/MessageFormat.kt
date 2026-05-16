package chat.schildi.revenge

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import chat.schildi.revenge.actions.InteractionAction
import chat.schildi.revenge.actions.LocalKeyboardActionHandler
import chat.schildi.revenge.actions.LocalRoomContextSuggestionsProvider
import chat.schildi.revenge.compose.components.LocalSessionId
import chat.schildi.theme.scExposures
import com.beeper.android.messageformat.DefaultMatrixBodyStyledFormatter
import com.beeper.android.messageformat.DrawPosition
import com.beeper.android.messageformat.MENTION_ROOM
import com.beeper.android.messageformat.MatrixBodyDrawStyle
import com.beeper.android.messageformat.MatrixBodyPreFormatStyle
import com.beeper.android.messageformat.MatrixBodyStyledFormatter
import com.beeper.android.messageformat.MatrixHtmlParser
import com.beeper.android.messageformat.MatrixToLink
import com.beeper.android.messageformat.SpanAttributes
import io.element.android.libraries.matrix.api.core.UserId

object MessageFormatDefaults {
    val blockIndention = 16.sp
    val parser: MatrixHtmlParser = MatrixHtmlParser()
    const val INLINE_IMAGE_PLACEHOLDER = "\uFFFD"
    val parseStyleForStrippedFormatting = MatrixBodyPreFormatStyle()
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
        formatInlineImageFallback = {
            // Anything that's not length 1 will cause spans that come after the image to misalign later (JVM bug?)
            it.alt?.takeIf { it.length == 1 }
                ?: it.title?.takeIf { it.length == 1 }
                ?: INLINE_IMAGE_PLACEHOLDER
        }
    )
    val plaintextFormatter = object : MatrixBodyStyledFormatter() {
        override fun formatHeading(tag: String, context: FormatContext) = listOf(ParagraphStyle())
        override fun formatSpan(attributes: SpanAttributes, context: FormatContext) = null
        override fun formatInlineCode(context: FormatContext) = null
        override fun formatCodeBlock(context: FormatContext) = null
        override fun formatBlockQuote(depth: Int, context: FormatContext) = listOf(ParagraphStyle())
        override fun formatRoomMention(context: FormatContext) = null
        override fun formatUserMention(mention: MatrixToLink.UserMention, context: FormatContext) = null
        override fun formatRoomLink(roomLink: MatrixToLink.RoomLink, context: FormatContext) = null
        override fun formatMessageLink(messageLink: MatrixToLink.MessageLink, context: FormatContext) = null
        override fun formatWebLink(href: String, context: FormatContext) = null
        override fun formatUnorderedListItem(depth: Int, context: FormatContext) = listOf(ParagraphStyle())
        override fun formatOrderedListItem(depth: Int, context: FormatContext) = listOf(ParagraphStyle())
        override fun formatDetailsSummary(revealId: Int, context: FormatContext) = listOf(ParagraphStyle())
        override fun formatDetailsContent(revealId: Int, context: FormatContext) = listOf(ParagraphStyle())
    }
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
    val roomId = LocalRoomContextSuggestionsProvider.current?.roomId
    val urlHandler = LocalUriHandler.current
    val keyHandler = LocalKeyboardActionHandler.current
    val destinationStateHolder = LocalDestinationState.current
    return remember(
        density,
        textMeasurer,
        textStyle,
        linkColor,
        mentionColor,
        mentionHighlightColor,
        sessionId,
        roomId,
        urlHandler,
        keyHandler,
        destinationStateHolder,
    ) {
        object : DefaultMatrixBodyStyledFormatter(
            density,
            textMeasurer,
            textStyle,
            urlStyle = TextLinkStyles(SpanStyle(color = linkColor)),
            blockIndention = MessageFormatDefaults.blockIndention,
            handleWebLinkClick = urlHandler::openUri,
        ) {
            override fun formatUserMention(
                mention: MatrixToLink.UserMention,
                context: FormatContext,
            ) = listOf(
                LinkAnnotation.Clickable("user_mention_click", TextLinkStyles()) {
                    sessionId ?: return@Clickable
                    keyHandler.executeAction(
                        InteractionAction.Navigate {
                            Destination.UserDetails(sessionId, UserId(mention.userId), roomId)
                        },
                        destinationStateHolder,
                    )
                },
                if (sessionId?.value == mention.userId) {
                    SpanStyle(color = mentionHighlightColor, fontWeight = FontWeight.Bold)
                } else {
                    SpanStyle(color = mentionColor, fontWeight = FontWeight.Bold)
                }
            )
            override fun formatRoomMention(context: FormatContext) = listOf(
                SpanStyle(color = mentionHighlightColor, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun matrixBodyDrawStyle(): MatrixBodyDrawStyle {
    val mentionColor = MaterialTheme.scExposures.mentionBg
    val mentionHighlightColor = MaterialTheme.scExposures.mentionBgHighlight
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val sessionId = LocalSessionId.current
    return remember(
        mentionColor,
        mentionHighlightColor,
        onSurfaceVariant,
        sessionId,
    ) {
        MatrixBodyDrawStyle(
            defaultForegroundColor = onSurface,
            drawBehindRoomMention = { position ->
                drawInlineRoundRect(
                    position = position,
                    color = mentionHighlightColor,
                    cornerRadius = Dimens.Conversation.mentionBgRadius,
                    cornerRadiusOnLineBreak = Dimens.Conversation.mentionBgRadiusOnLineBreak,
                )
            },
            drawBehindUserMention = { mention, position ->
                val color = if (sessionId?.value == mention.userId) {
                    mentionHighlightColor
                } else {
                    mentionColor
                }
                drawInlineRoundRect(
                    position = position,
                    color = color,
                    cornerRadius = Dimens.Conversation.mentionBgRadius,
                    cornerRadiusOnLineBreak = Dimens.Conversation.mentionBgRadiusOnLineBreak,
                )
            },
            drawBehindBlockQuote = { depth, position ->
                val barWidthDp = 4f
                drawRoundRect(
                    onSurfaceVariant,
                    topLeft = Offset((MessageFormatDefaults.blockIndention * (depth - 1)).toPx(), position.rect.top),
                    size = Size(barWidthDp * density, position.rect.height),
                    cornerRadius = CornerRadius(barWidthDp * density, barWidthDp * density),
                )
            },
            drawBehindDetailsSummaryFirstLine = { revealId, pos, state ->
                val rect = pos.rect
                // Use line height and available width as baseline size for triangle size
                val lineHeight = rect.size.height
                val triangleSideLength = lineHeight / 2f
                // * sqrt(3) / 2
                val triangleHeight = triangleSideLength * 0.8660254f
                val shortSidePadding = (triangleSideLength - triangleHeight) / 2
                val trianglePath = Path().apply {
                    if (revealId in state.expandedItems.value) {
                        // Already expanded => downward-facing triangle
                        moveTo(0f, shortSidePadding)
                        lineTo(triangleSideLength / 2, triangleSideLength - shortSidePadding)
                        lineTo(triangleSideLength, shortSidePadding)
                    } else {
                        moveTo(shortSidePadding, 0f)
                        lineTo(triangleSideLength - shortSidePadding, triangleSideLength / 2)
                        lineTo(shortSidePadding, triangleSideLength)
                    }
                    close()
                    val canvasPadding = (lineHeight - triangleSideLength) / 2
                    translate(
                        Offset(
                            if (pos.isRtl) {
                                rect.right - triangleSideLength + canvasPadding
                            } else {
                                rect.left
                            },
                            // Center in line height
                            rect.top + canvasPadding,
                        )
                    )
                }
                drawPath(trianglePath, onSurfaceVariant)
            },
        )
    }
}

fun DrawScope.drawInlineRoundRect(
    position: DrawPosition.InLine,
    color: Color,
    cornerRadius: Float,
    cornerRadiusOnLineBreak: Float,
) {
    val rect = position.rect
    val leftRadius = this.density * if (position.leftHasContinuation) {
        cornerRadiusOnLineBreak
    } else {
        cornerRadius
    }
    val rightRadius = this.density * if (position.rightHasContinuation) {
        cornerRadiusOnLineBreak
    } else {
        cornerRadius
    }
    drawIntoCanvas {
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = rect,
                    topLeft = CornerRadius(leftRadius, leftRadius),
                    bottomLeft = CornerRadius(leftRadius, leftRadius),
                    topRight = CornerRadius(rightRadius, rightRadius),
                    bottomRight = CornerRadius(rightRadius, rightRadius),
                ),
            )
        }
        drawPath(path, color = color)
    }
}
