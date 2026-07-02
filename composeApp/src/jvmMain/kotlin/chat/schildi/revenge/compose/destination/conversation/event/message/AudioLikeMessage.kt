package chat.schildi.revenge.compose.destination.conversation.event.message

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import chat.schildi.revenge.Dimens
import chat.schildi.revenge.DurationFormat
import chat.schildi.revenge.compose.components.drawWaveform
import chat.schildi.revenge.compose.components.thenIf
import chat.schildi.revenge.model.conversation.MessageMetadata
import chat.schildi.revenge.util.normalisedWaveform
import com.beeper.android.messageformat.MatrixBodyParseResult
import io.element.android.libraries.matrix.api.media.AudioDetails
import io.element.android.libraries.matrix.api.media.AudioInfo
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.timeline.item.EventThreadInfo
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.VoiceMessageType
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.message_placeholder_audio_message
import shire.res.generated.resources.message_placeholder_voice_message

@Composable
fun AudioMessage(
    audio: AudioMessageType,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    threadInfo: EventThreadInfo?,
    modifier: Modifier = Modifier,
) {
    AudioLikeMessage(
        source = audio.source,
        info = audio.info,
        details = null,
        messageMetadata = messageMetadata,
        filename = audio.filename,
        caption = audio.caption,
        type = FileMessageRenderType.AUDIO,
        isOwn = isOwn,
        timestamp = timestamp,
        inReplyTo = inReplyTo,
        threadInfo = threadInfo,
        modifier = modifier,
    )
}

@Composable
fun VoiceMessage(
    voice: VoiceMessageType,
    messageMetadata: MessageMetadata?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    threadInfo: EventThreadInfo?,
    modifier: Modifier = Modifier,
) {
    AudioLikeMessage(
        source = voice.source,
        info = voice.info,
        details = voice.details,
        messageMetadata = messageMetadata,
        filename = voice.filename,
        caption = voice.caption,
        type = FileMessageRenderType.VOICE,
        isOwn = isOwn,
        timestamp = timestamp,
        inReplyTo = inReplyTo,
        threadInfo = threadInfo,
        modifier = modifier,
    )
}

@Composable
fun AudioLikeMessage(
    source: MediaSource,
    info: AudioInfo?,
    details: AudioDetails?,
    messageMetadata: MessageMetadata?,
    filename: String,
    type: FileMessageRenderType,
    caption: String?,
    isOwn: Boolean,
    timestamp: TimestampOverlayContent?,
    inReplyTo: InReplyTo?,
    threadInfo: EventThreadInfo?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val captionLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val durationWidth = remember { mutableStateOf(0.dp) }
    MessageBubble(
        isOwn = isOwn,
        timestamp = timestamp,
        modifier = modifier,
        padding = PaddingValues(Dimens.Conversation.messageBubbleInnerPadding),
        contentTextLayoutResult = captionLayoutResult.value,
        verticalArrangement = Arrangement.spacedBy(Dimens.Conversation.captionPadding),
        nonTextWidth = if (caption.isNullOrBlank()) {
            Dimens.Conversation.fileIconSize + Dimens.horizontalItemPadding +
                    if (info?.duration != null) {
                        durationWidth.value + Dimens.horizontalItemPadding
                    } else {
                        0.dp
                    }
        } else {
            0.dp
        },
        allowTimestampOverlay = details?.waveform.isNullOrEmpty() && messageMetadata?.preFormattedContent?.allowTimestampOverlay() != false,
    ) {
        inReplyTo?.let {
            ReplyContent(it, threadInfo)
        }
        AudioLikeMessageContent(
            source = source,
            info = info,
            details = details,
            messageMetadata = messageMetadata,
            filename = filename,
            type = type,
            caption = caption?.let { AnnotatedString(it) },
            onDurationMeasure = { durationWidth.value = density.run { it.size.width.toDp() } }
        ) {
            captionLayoutResult.value = it
        }
    }
}

@Composable
fun ColumnScope.AudioLikeMessageContent(
    source: MediaSource,
    info: AudioInfo?,
    details: AudioDetails?,
    messageMetadata: MessageMetadata?,
    filename: String,
    type: FileMessageRenderType,
    caption: AnnotatedString? = null,
    onDurationMeasure: (LayoutCoordinates) -> Unit = {},
    onCaptionTextLayout: (TextLayoutResult?) -> Unit = {},
) {
    val waveform = details?.waveform
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Dimens.horizontalArrangement) {
        Icon(
            type.icon,
            null,
            modifier = Modifier
                .size(Dimens.Conversation.fileIconSize)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .padding(Dimens.Conversation.messageBubbleInnerPadding)
        )
        var canvasSize by remember { mutableStateOf(DpSize(0.dp, 0.dp)) }
        var canvasSizePx by remember { mutableStateOf(Size(0f, 0f)) }
        if (!waveform.isNullOrEmpty()) {
            val amplitudeDisplayCount by remember(canvasSize) {
                derivedStateOf {
                    (canvasSize.width.value / (Dimens.Conversation.AudioWaveform.linePadding.value + Dimens.Conversation.AudioWaveform.linePadding.value)).toInt()
                }
            }
            val normalizedWaveformData by remember(amplitudeDisplayCount) {
                derivedStateOf {
                    waveform.normalisedWaveform(amplitudeDisplayCount)
                }
            }
            val brush = SolidColor(MaterialTheme.colorScheme.onSurface)
            Canvas(
                modifier = Modifier
                    .height(32.dp)
                    .widthIn(max = Dimens.Conversation.AudioWaveform.maxWidth(waveform.size))
                    .fillMaxWidth(),
            ) {
                canvasSize = size.toDpSize()
                canvasSizePx = size
                drawWaveform(
                    waveformData = normalizedWaveformData,
                    canvasSizePx = canvasSizePx,
                    brush = brush,
                )
            }
        } else {
            val renderedFilename = filename.takeIf(String::isNotBlank)
                ?: if (type == FileMessageRenderType.VOICE) {
                    stringResource(Res.string.message_placeholder_voice_message)
                } else {
                    stringResource(Res.string.message_placeholder_audio_message)
                }
            TextLikeMessageContent(
                MatrixBodyParseResult(renderedFilename),
                allowBigEmojiOnly = false,
                onTextLayout = if (caption.isNullOrBlank()) onCaptionTextLayout else {{}},
                modifier = Modifier.weight(1f, fill = false).thenIf(info?.duration != null) { alignByBaseline() },
                maxLines = 1,
            )
        }
        info?.duration?.let { duration ->
            Text(
                remember(duration) { DurationFormat.formatMediaDuration(duration) },
                modifier = Modifier
                    .thenIf(waveform.isNullOrEmpty()) { alignByBaseline() }
                    .onGloballyPositioned(onDurationMeasure),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (!caption.isNullOrBlank()) {
        TextLikeMessageContent(
            messageMetadata?.preFormattedContent ?: MatrixBodyParseResult(caption),
            allowBigEmojiOnly = false,
            modifier = Modifier.padding(top = Dimens.Conversation.captionPadding),
            onTextLayout = onCaptionTextLayout,
        )
    } else if (waveform != null) {
        LaunchedEffect(Unit) {
            onCaptionTextLayout(null)
        }
    }
}
