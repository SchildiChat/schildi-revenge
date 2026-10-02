package chat.schildi.revenge.model.conversation

import androidx.compose.runtime.staticCompositionLocalOf
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.timeline.item.event.PollContent

val LocalEventActionViewModel = staticCompositionLocalOf<EventActionViewModel?> { null }

interface EventActionViewModel {
    fun togglePollVote(
        context: ActionContext,
        pollStartId: EventId,
        poll: PollContent,
        answerId: String,
    ): ActionResult
}
