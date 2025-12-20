package chat.schildi.revenge.model

import androidx.compose.ui.text.input.TextFieldValue
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.IntentionalMention
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

typealias DraftKey = ScopedRoomKey

enum class DraftType {
    TEXT,
    NOTICE,
    EMOTE,
    EDIT,
    EDIT_CAPTION,
    REACTION,
}

data class DraftValue(
    val type: DraftType = DraftType.TEXT,
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val intentionalMentions: ImmutableList<IntentionalMention> = persistentListOf(),
    val inReplyTo: InReplyTo.Ready? = null,
    val editEventId: EventOrTransactionId? = null, // Only for DraftType.EDIT and DraftType.EDIT_CAPTION
    val isSendInProgress: Boolean = false,
    val initialBody: String = "", // For edits the original message content, else empty
) {
    val body: String
        get() = textFieldValue.text
    val htmlBody: String?
        get() = null // TODO?

    fun isEmpty() = textFieldValue.text.isBlank() || textFieldValue.text == initialBody
    fun canSend() = !isSendInProgress && !isEmpty()
}

// TODO may add some persistent storage to this one to survive restarts & crashes
object DraftRepo {
    private val drafts = MutableStateFlow<ImmutableMap<DraftKey, DraftValue>>(persistentMapOf())

    val roomsWithDrafts = drafts.map {
        it.filter { (k, v) -> !v.isEmpty() }.keys
    }

    fun update(draftKey: DraftKey, draftValue: DraftValue) {
        drafts.update {
            (it + (draftKey to draftValue)).toPersistentMap()
        }
    }

    fun update(draftKey: DraftKey, transform: (DraftValue?) -> DraftValue?) {
        drafts.update {
            val value = transform(it[draftKey])
            if (value == null) {
                it - draftKey
            } else {
                it + (draftKey to value)
            }.toPersistentMap()
        }
    }

    fun deleteDraft(draftKey: DraftKey) {
        drafts.update {
            it.minus(draftKey).toPersistentMap()
        }
    }

    fun followDraft(draftKey: DraftKey) = drafts.map {
        it[draftKey]
    }
}
