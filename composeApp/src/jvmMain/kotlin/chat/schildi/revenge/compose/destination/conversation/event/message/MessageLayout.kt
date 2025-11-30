package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.compose.focus.keyFocusable

@Composable
fun MessageLayout(
    isOwn: Boolean,
    modifier: Modifier = Modifier,
    senderAvatar: @Composable () -> Unit,
    senderName: @Composable () -> Unit,
    messageContent: @Composable () -> Unit,
    reactions: @Composable () -> Unit,
) {
    when (LocalMessageRenderContext.current) {
        MessageRenderContext.NORMAL -> {
            MessageLayoutNormal(
                isOwn = isOwn,
                modifier = modifier,
                senderAvatar = senderAvatar,
                senderName = senderName,
                messageContent = messageContent,
                reactions = reactions,
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
    reactions: @Composable () -> Unit,
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
            modifier
                .fillMaxWidth()
                .keyFocusable(FocusRole.SEARCHABLE_ITEM)
                .padding(
                    start = Dimens.listPadding,
                    end = Dimens.listPadding,
                    // TODO less padding to same sender
                    top = Dimens.Conversation.virtualItemPadding,
                    bottom = Dimens.Conversation.virtualItemPadding,
                )
        ) {
            if (!isOwn) {
                BoxWithDirection(
                    mainLayoutDirection,
                    Modifier
                        .padding(end = Dimens.Conversation.avatarItemPadding)
                        .width(Dimens.Conversation.avatar)
                ) {
                    senderAvatar()
                }
            }
            Column(
                Modifier
                    .padding(end = Dimens.Conversation.otherSidePadding)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    BoxWithDirection(mainLayoutDirection) {
                        senderName()
                    }
                    BoxWithDirection(mainLayoutDirection) {
                        messageContent()
                    }
                    BoxWithDirection(mainLayoutDirection) {
                        reactions()
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
