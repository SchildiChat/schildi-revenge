package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.DateTimeFormat
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.compose.components.WithTooltip
import chat.schildi.revenge.compose.components.thenIf
import chat.schildi.revenge.model.TimestampSettings
import chat.schildi.theme.scExposures
import io.element.android.libraries.matrix.api.timeline.item.event.EventCanBeEdited
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.LocalEventSendState
import io.element.android.libraries.matrix.api.timeline.item.event.MessageShield
import io.element.android.libraries.matrix.api.timeline.item.event.RedactedContent
import io.element.android.libraries.matrix.api.timeline.item.event.isCritical
import org.jetbrains.compose.resources.stringResource
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.hint_sending
import shire.composeapp.generated.resources.message_edited_decoration
import shire.composeapp.generated.resources.message_shield_authenticity_not_guaranteed
import shire.composeapp.generated.resources.message_shield_mismatched_sender
import shire.composeapp.generated.resources.message_shield_sent_in_clear
import shire.composeapp.generated.resources.message_shield_unknown_device
import shire.composeapp.generated.resources.message_shield_unsigned_device
import shire.composeapp.generated.resources.message_shield_unverified_identity
import shire.composeapp.generated.resources.message_shield_verification_violation

data class TimestampOverlayContent(
    val timestamp: String,
    val isEdited: Boolean,
    val isSending: Boolean,
    val isSendError: Boolean,
    val messageShield: MessageShield?,
)

fun EventTimelineItem.timestampOverlayContent(settings: TimestampSettings) = TimestampOverlayContent(
    timestamp = DateTimeFormat.formatTime(DateTimeFormat.timestampToDateTime(timestamp)),
    isEdited = (content as? EventCanBeEdited)?.isEdited == true,
    isSending = localSendState is LocalEventSendState.Sending,
    isSendError = localSendState is LocalEventSendState.Failed,
    messageShield = messageShieldProvider(strict = false)?.takeIf {
        when (it) {
            is MessageShield.AuthenticityNotGuaranteed -> settings.renderAuthenticityNotGuaranteed
            is MessageShield.MismatchedSender -> settings.renderSenderMismatch
            is MessageShield.SentInClear -> content !is RedactedContent
            else -> true
        }
    },
)

@Composable
fun MessageBubble(
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    contentTextLayoutResult: TextLayoutResult? = null,
    allowTimestampOverlay: Boolean = true,
    isMediaOverlay: Boolean = false,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(Dimens.Conversation.messageBubbleInnerPadding),
    outlined: Boolean = false,
    transparent: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    nonTextWidth: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    // Bypass double message bubble when nested in a reply
    if (LocalMessageRenderContext.current == MessageRenderContext.IN_REPLY_TO) {
        Column(modifier.padding(padding), verticalArrangement = verticalArrangement, content = content)
    } else {
        val color = if (transparent)
            Color.Transparent
        else if (isOwn)
            MaterialTheme.scExposures.bubbleBgOutgoing
        else
            MaterialTheme.scExposures.bubbleBgIncoming
        Box(
            modifier
                .then(
                    if (outlined) {
                        Modifier.border(
                            width = 1.dp,
                            color = color,
                            shape = Dimens.Conversation.messageBubbleShape,
                        )
                    } else {
                        Modifier.background(
                            color = color,
                            shape = Dimens.Conversation.messageBubbleShape,
                        )
                    }
                )
                .padding(padding),
        ) {
            TimestampOverlayLayout(
                contentTextLayoutResult = contentTextLayoutResult,
                allowTimestampOverlay = allowTimestampOverlay,
                horizontalPadding = Dimens.Conversation.timestampHorizontalPaddingToText,
                verticalPadding = Dimens.Conversation.timestampVerticalMarginToText,
                nonTextWidth = nonTextWidth,
                content = {
                    Column(verticalArrangement = verticalArrangement, content = content)
                },
                overlay = {
                    TimestampContent(
                        content = timestamp,
                        withBackground = contentTextLayoutResult == null && isMediaOverlay,
                        modifier = Modifier.thenIf(isMediaOverlay && contentTextLayoutResult != null) {
                            padding(Dimens.Conversation.messageBubbleInnerPadding)
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun TimestampContent(
    content: TimestampOverlayContent?,
    withBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    if (content == null) return
    val overlayBgColor = MaterialTheme.scExposures.timestampOverlayBg
    Row(
        modifier.thenIf(withBackground) {
            background(
                overlayBgColor,
                RoundedCornerShape(
                    topStart = Dimens.Conversation.messageBubbleCornerRadius,
                    bottomEnd = Dimens.Conversation.messageBubbleCornerRadius,
                )
            ).padding(Dimens.Conversation.timestampPaddingWithOverlayBg)
        },
        horizontalArrangement = Dimens.horizontalArrangementSmall,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val textColor = if (withBackground)
            MaterialTheme.scExposures.timestampOverlayFgOnBg
        else
            MaterialTheme.colorScheme.onSurfaceVariant
        if (content.isEdited) {
            Text(
                stringResource(Res.string.message_edited_decoration),
                color = textColor,
                style = Dimens.Conversation.messageTimestampStyle,
                modifier = Modifier.alignByBaseline(),
            )
        }
        if (content.isSending) {
            Icon(
                Icons.Default.AccessTime,
                stringResource(Res.string.hint_sending),
                tint = textColor,
                modifier = Modifier.size(Dimens.Conversation.timestampDecorationIcon)
            )
        }
        if (content.isSendError) {
            Icon(
                Icons.Default.ErrorOutline,
                stringResource(Res.string.hint_sending),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Dimens.Conversation.timestampDecorationIcon)
            )
        }
        Text(
            content.timestamp,
            color = textColor,
            style = Dimens.Conversation.messageTimestampStyle,
            modifier = Modifier.alignByBaseline(),
        )
        content.messageShield?.let { messageShield ->
            val hint = when (messageShield) {
                is MessageShield.AuthenticityNotGuaranteed -> stringResource(Res.string.message_shield_authenticity_not_guaranteed)
                is MessageShield.MismatchedSender -> stringResource(Res.string.message_shield_mismatched_sender)
                is MessageShield.SentInClear -> stringResource(Res.string.message_shield_sent_in_clear)
                is MessageShield.UnknownDevice -> stringResource(Res.string.message_shield_unknown_device)
                is MessageShield.UnsignedDevice -> stringResource(Res.string.message_shield_unsigned_device)
                is MessageShield.UnverifiedIdentity -> stringResource(Res.string.message_shield_unverified_identity)
                is MessageShield.VerificationViolation -> stringResource(Res.string.message_shield_verification_violation)
            }
            WithTooltip(hint) {
                Icon(
                    Icons.Default.Info,
                    hint,
                    tint = if (messageShield.isCritical) MaterialTheme.colorScheme.error else textColor,
                    modifier = Modifier.size(Dimens.Conversation.timestampDecorationIcon)
                )
            }
        }
    }
}
