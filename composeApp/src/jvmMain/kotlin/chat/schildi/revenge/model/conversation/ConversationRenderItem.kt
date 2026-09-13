package chat.schildi.revenge.model.conversation

import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileChangeContent
import io.element.android.libraries.matrix.api.timeline.item.event.RoomMembershipContent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * A row to render in the timeline. Usually a single [ScTimelineItem], but a run of consecutive
 * membership/profile-change events can be collapsed into a single [MembershipGroup] to reduce
 * clutter, e.g. in rooms bridged to other networks with frequent joins/leaves.
 */
sealed interface ConversationRenderItem {
    data class Single(val item: ScTimelineItem) : ConversationRenderItem
    data class MembershipGroup(val items: ImmutableList<ScTimelineItem>) : ConversationRenderItem

    val firstItem: ScTimelineItem
        get() = when (this) {
            is Single -> item
            is MembershipGroup -> items.first()
        }

    val lastItem: ScTimelineItem
        get() = when (this) {
            is Single -> item
            is MembershipGroup -> items.last()
        }
}

private fun ScTimelineItem.isGroupableMembershipEvent(): Boolean {
    val event = (item as? MatrixTimelineItem.Event)?.event ?: return false
    return event.content is RoomMembershipContent || event.content is ProfileChangeContent
}

/**
 * Groups consecutive groupable items (see [isGroupableMembershipEvent]) into [ConversationRenderItem.MembershipGroup]s.
 * Runs of a single groupable item are kept as [ConversationRenderItem.Single] instead of a group of one.
 */
fun List<ScTimelineItem>.groupMembershipEvents(enabled: Boolean): List<ConversationRenderItem> {
    if (!enabled) {
        return map { ConversationRenderItem.Single(it) }
    }
    val result = mutableListOf<ConversationRenderItem>()
    var run = mutableListOf<ScTimelineItem>()
    fun flushRun() {
        when (run.size) {
            0 -> Unit
            1 -> result.add(ConversationRenderItem.Single(run[0]))
            else -> result.add(ConversationRenderItem.MembershipGroup(run.toPersistentList()))
        }
        run = mutableListOf()
    }
    for (item in this) {
        if (item.isGroupableMembershipEvent()) {
            run.add(item)
        } else {
            flushRun()
            result.add(ConversationRenderItem.Single(item))
        }
    }
    flushRun()
    return result
}
