package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import chat.schildi.revenge.compose.destination.conversation.event.message.TimestampOverlayContent
import chat.schildi.revenge.util.tryOrNull
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileDetails
import io.element.android.libraries.matrix.api.timeline.item.event.TimelineItemDebugInfoProvider
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.stringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.message_placeholder_hidden_event
import shire.res.generated.resources.message_placeholder_unknown

@Composable
fun HiddenEventRow(
    timelineItemDebugInfoProvider: TimelineItemDebugInfoProvider?,
    senderId: UserId,
    senderProfile: ProfileDetails,
    timestamp: TimestampOverlayContent?,
    modifier: Modifier = Modifier,
) {
    val senderName = senderProfile.getDisambiguatedDisplayName(senderId)
    val eventType = remember(timelineItemDebugInfoProvider) {
        timelineItemDebugInfoProvider?.invoke()?.originalJson?.let {
            tryOrNull {
                Json.parseToJsonElement(it).jsonObject["type"]?.jsonPrimitive?.content
            }
        }
    } ?: stringResource(Res.string.message_placeholder_unknown)
    StateUpdateRow(
        text = AnnotatedString(stringResource(Res.string.message_placeholder_hidden_event, senderName, eventType)),
        senderProfile = senderProfile,
        senderId = senderId,
        timestamp = timestamp,
        modifier = modifier,
    )
}
