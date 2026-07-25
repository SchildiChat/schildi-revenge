package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import chat.schildi.revenge.Dimens
import chat.schildi.theme.LocalMessageStyle

@Composable
fun MessageLayout(
    isOwn: Boolean,
    modifier: Modifier = Modifier,
    senderAvatar: @Composable () -> Unit,
    senderName: @Composable () -> Unit,
    messageContent: @Composable () -> Unit,
) {
    when (LocalMessageRenderContext.current) {
        MessageRenderContext.NORMAL -> {
            MessageLayoutNormal(
                isOwn = isOwn,
                modifier = modifier,
                senderAvatar = senderAvatar,
                senderName = senderName,
                messageContent = messageContent,
            )
        }
        MessageRenderContext.IN_REPLY_TO -> {
            MessageLayoutInReplyTo(
                senderName = senderName,
                messageContent = messageContent,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun MessageLayoutNormal(
    isOwn: Boolean,
    modifier: Modifier = Modifier,
    senderAvatar: @Composable () -> Unit,
    senderName: @Composable () -> Unit,
    messageContent: @Composable () -> Unit,
) {
    val mainLayoutDirection = LocalLayoutDirection.current
    val thisLayoutDirection = if (isOwn) {
        if (mainLayoutDirection == LayoutDirection.Ltr)
            LayoutDirection.Rtl
        else
            LayoutDirection.Ltr
    } else {
        mainLayoutDirection
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides thisLayoutDirection,
    ) {
        Row(
            modifier.fillMaxWidth()
        ) {
            val messageStyle = LocalMessageStyle.current
            if (!isOwn) {
                BoxWithDirection(
                    mainLayoutDirection,
                    Modifier
                        .padding(end = Dimens.Conversation.avatarItemPadding)
                        .width(messageStyle.avatarSize)
                ) {
                    senderAvatar()
                }
            }
            Column(
                Modifier
                    .padding(end = messageStyle.otherSideMargin(isOwn))
                    .widthIn(max = messageStyle.maxWidth),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    if (!isOwn) {
                        BoxWithDirection(mainLayoutDirection) {
                            senderName()
                        }
                    }
                    BoxWithDirection(mainLayoutDirection) {
                        messageContent()
                    }
                }
            }
        }
    }
}

@Composable
fun MessageLayoutInReplyTo(
    senderName: @Composable () -> Unit,
    messageContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Box(
            Modifier
                .padding(
                    start = Dimens.Conversation.messageBubbleInnerPadding,
                    end = Dimens.Conversation.messageBubbleInnerPadding,
                    top = Dimens.Conversation.messageBubbleInnerPadding,
                )
        ) {
            senderName()
        }
        Box {
            messageContent()
        }
    }
}

@Composable
private fun BoxWithDirection(
    direction: LayoutDirection,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        CompositionLocalProvider(
            LocalLayoutDirection provides direction,
        ) {
            content()
        }
    }
}
