package chat.schildi.revenge

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import chat.schildi.theme.rememberEmojiFontFamily

object Dimens {
    val windowPadding = 16.dp
    val listPadding = 8.dp
    val listPaddingBig = 16.dp
    val horizontalItemPadding = 8.dp
    val horizontalItemPaddingBig = 16.dp
    val horizontalArrangement = Arrangement.spacedBy(horizontalItemPadding)
    val horizontalArrangementSmall = Arrangement.spacedBy(4.dp)
    val verticalArrangement = Arrangement.spacedBy(listPadding)

    val avatarShape = RoundedCornerShape(12.dp)
    val ownAccountAvatarShape = CircleShape

    object Inbox {
        val avatar = 48.dp
        val accountAvatar = 16.dp
        val spaceAvatar = 24.dp
        val spaceShape = RoundedCornerShape(4.dp)
        val smallIcon = 16.dp
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
        val avatar = 48.dp
        val avatarForState = 16.dp
        val avatarItemPadding = 16.dp
        val avatarReservation = avatar + avatarItemPadding
        val virtualItemPadding = 8.dp
        val otherSidePadding = 80.dp
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

        val textMessageStyle
            @Composable get() = MaterialTheme.typography.bodyLarge
        val emojiOnlyMessageStyle
            @Composable get() = MaterialTheme.typography.headlineLarge
                .merge(fontFamily = rememberEmojiFontFamily())
        val messageTimestampStyle
            @Composable get() = MaterialTheme.typography.bodyMedium

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

        val newMessagesLineHeight = 2.dp

        val fileIconSize = 36.dp
    }
}
