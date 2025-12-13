package chat.schildi.revenge.model

import androidx.compose.runtime.collectAsState
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.IntentionalMention
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

data class DraftKey(
    val sessionId: SessionId,
    val roomId: RoomId,
)

enum class DraftType {
    TEXT,
    NOTICE,
    EMOTE,
}

data class DraftValue(
    val type: DraftType = DraftType.TEXT,
    val body: String = "",
    val htmlBody: String? = null,
    val intentionalMentions: ImmutableList<IntentionalMention> = persistentListOf(),
    val isSendInProgress: Boolean = false,
) {
    fun isEmpty() = body.isBlank()
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

    fun update(draftKey: DraftKey, transform: (DraftValue?) -> DraftValue) {
        drafts.update {
            (it + (draftKey to transform(it[draftKey]))).toPersistentMap()
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
