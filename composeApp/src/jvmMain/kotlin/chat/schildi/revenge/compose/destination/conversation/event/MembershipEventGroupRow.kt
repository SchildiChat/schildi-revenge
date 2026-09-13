package chat.schildi.revenge.compose.destination.conversation.event

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import chat.schildi.revenge.compose.components.ExpandButton
import chat.schildi.revenge.compose.destination.conversation.event.message.timestampOverlayContent
import chat.schildi.revenge.model.conversation.ScTimelineItem
import chat.schildi.revenge.model.conversation.TimestampSettings
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileChangeContent
import io.element.android.libraries.matrix.api.timeline.item.event.RoomMembershipContent
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.pluralStringResource
import shire.res.generated.resources.Res
import shire.res.generated.resources.membership_event_group_summary

/**
 * Collapsible summary for a run of consecutive membership/profile-change events, to reduce
 * timeline clutter e.g. in bridged rooms with frequent joins/leaves.
 */
@Composable
fun MembershipEventGroupRow(
    items: ImmutableList<ScTimelineItem>,
    timestampSettings: TimestampSettings,
    modifier: Modifier = Modifier,
) {
    val groupKey = remember(items) {
        items.joinToString("|") { (it.item as? MatrixTimelineItem.Event)?.uniqueId?.value ?: "" }
    }
    var expanded by rememberSaveable(groupKey) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        ExpandButton(
            text = pluralStringResource(Res.plurals.membership_event_group_summary, items.size, items.size),
            expanded = expanded,
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            items.forEach { scItem ->
                val event = (scItem.item as? MatrixTimelineItem.Event)?.event ?: return@forEach
                val timestamp = event.timestampOverlayContent(timestampSettings)
                when (val content = event.content) {
                    is RoomMembershipContent -> RoomMembershipRow(content, event.sender, event.senderProfile, timestamp)
                    is ProfileChangeContent -> ProfileChangeRow(content, event.sender, event.senderProfile, timestamp)
                    else -> Unit
                }
            }
        }
    }
}
