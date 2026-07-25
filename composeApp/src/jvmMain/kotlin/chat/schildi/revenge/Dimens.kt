package chat.schildi.revenge

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import chat.schildi.theme.rememberEmojiFontFamily

object Dimens {
    val windowPadding = 16.dp
    val listPadding = 8.dp
    val listPaddingBig = 16.dp
    val listPaddingSmall = 4.dp
    val horizontalItemPadding = 8.dp
    val horizontalItemPaddingSmall = 4.dp
    val horizontalItemPaddingBig = 16.dp
    val horizontalArrangement = Arrangement.spacedBy(horizontalItemPadding)
    val horizontalArrangementSmall = Arrangement.spacedBy(4.dp)
    val verticalArrangementBig = Arrangement.spacedBy(listPaddingBig)
    val verticalArrangement = Arrangement.spacedBy(listPadding)
    val verticalArrangementSmall = Arrangement.spacedBy(listPaddingSmall)

    val squareButtonClip = RoundedCornerShape(4.dp)
    val avatarShape = RoundedCornerShape(12.dp)
    val ownAccountAvatarShape = CircleShape

    val fgDisabledAlpha = 0.38f

    object Inbox {
        val avatar = 48.dp
        val avatarItemPadding = 12.dp
        val accountAvatar = 16.dp
        val spaceAvatar = 24.dp
        val spaceShape = RoundedCornerShape(4.dp)
        val smallIcon = 16.dp
        val spaceSwipeIndicator = 48.dp
    }

    val animationDurationQuickMs = 50
    fun <T>tween(): TweenSpec<T> = tween(animationDurationQuickMs)
    val animationDurationSlowMs = 300
    fun <T>tweenSmooth(): TweenSpec<T> = tween(animationDurationSlowMs)

    val suggestionsTextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge
    val emojiSuggestionsTextStyle
        @Composable get() = suggestionsTextStyle.merge(fontFamily = rememberEmojiFontFamily())

    object Conversation {
        val avatarForState = 16.dp
        val avatarItemPadding = 12.dp
        val virtualItemPadding = 8.dp
        val messageBubbleCornerRadius = 10.dp
        val messageBubbleShape = RoundedCornerShape(messageBubbleCornerRadius)
        val replyContentShape = RoundedCornerShape(6.dp)
        val replyItemPadding = 8.dp
        val messageBubbleInnerPadding = 8.dp
        val imageBubbleInnerPadding = 2.dp
        val messageSameSenderPadding = 4.dp
        val messageOtherSenderPadding = 12.dp
        val captionPadding = 8.dp
        val timestampPaddingWithOverlayBg = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        val timestampHorizontalPaddingToText = 8.dp
        val timestampVerticalMarginToText = 4.dp
        val timestampDecorationIcon = 16.dp
        val bottomListItemPadding = PaddingValues(
            start = windowPadding,
            end = windowPadding,
            top = listPadding,
        )
        val bottomStickyItemPadding = PaddingValues(
            start = windowPadding,
            end = windowPadding,
            bottom = windowPadding,
        )

        const val mentionBgRadius = 8f
        const val mentionBgRadiusOnLineBreak = 2f

        val emojiOnlyMessageStyle
            @Composable get() = MaterialTheme.typography.headlineLarge.copy(textDirection = TextDirection.Content)
                .merge(fontFamily = rememberEmojiFontFamily())
        val messageTimestampStyle
            @Composable get() = MaterialTheme.typography.bodySmall

        val imageMinWidth = 48.dp
        val imageMinHeight = 48.dp
        val imageMaxWidth = 500.dp
        val imageMaxHeight = 500.dp
        val imageRepliedToMaxHeight = 200.dp

        val reactionShape = RoundedCornerShape(16.dp)
        val reactionPaddingHorizontal = 8.dp
        val reactionPaddingVertical = 4.dp
        val reactionInnerPaddingHorizontal = 12.dp
        val reactionInnerPaddingVertical = 6.dp
        const val reactionMaxLength = 200

        val receiptSize = 16.dp
        val receiptPaddingHorizontal = (-4).dp
        val receiptPaddingVertical = 4.dp

        val threadInfoPaddingVertical = 4.dp
        val threadInfoPaddingHorizontal = messageBubbleInnerPadding

        val newMessagesLineHeight = 2.dp

        val fileIconSize = 36.dp

        object FloatingDate {
            val topMargin = 8.dp
            val contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            val shape = RoundedCornerShape(6.dp)
        }

        object Composer {
            val buttonWidth = 48.dp
            val buttonHeight = 48.dp
        }

        object AudioWaveform {
            val lineWidth = 2.dp
            val linePadding = 2.dp

            const val maxRenderedSegments = 64

            fun maxWidth(waveformLength: Int) =
                (linePadding + lineWidth) * waveformLength.coerceIn(1, maxRenderedSegments) - linePadding
        }
    }

    object Split {
        val highlightShape = RoundedCornerShape(4.dp)
        val outlineWidth = 3.dp
    }
}
