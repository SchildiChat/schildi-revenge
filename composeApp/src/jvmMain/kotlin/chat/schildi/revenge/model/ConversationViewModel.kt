package chat.schildi.revenge.model

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.matrixsdk.ScTimelineFilterSettings
import chat.schildi.matrixsdk.urlpreview.UrlPreviewProvider
import chat.schildi.matrixsdk.urlpreview.UrlPreviewStateProvider
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.ScPreferencesStore
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.safeLookup
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.GlobalActionsScope
import chat.schildi.revenge.PrettyJson
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.actions.ConfirmActionAppMessage
import chat.schildi.revenge.actions.FlatMergedKeyboardActionProvider
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.RoomContextSuggestionsProvider
import chat.schildi.revenge.actions.UserIdSuggestion
import chat.schildi.revenge.actions.UserIdSuggestionsProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.launchActionAsync
import chat.schildi.revenge.actions.orActionInapplicable
import chat.schildi.revenge.actions.orActionValidationError
import chat.schildi.revenge.actions.parseRoomStateSnapshot
import chat.schildi.revenge.actions.toActionResult
import chat.schildi.revenge.compose.search.SearchProvider
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.compose.util.UrlUtil
import chat.schildi.revenge.compose.util.insertAtCursor
import chat.schildi.revenge.compose.util.insertTextFieldValue
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger
import chat.schildi.revenge.toPrettyJson
import chat.schildi.revenge.util.MimeUtil
import chat.schildi.revenge.util.tryOrNull
import chat.schildi.revenge.util.MediaInfoUtil
import co.touchlab.kermit.Logger
import io.element.android.features.messages.impl.timeline.EventFocusResult
import io.element.android.features.messages.impl.timeline.TimelineController
import io.element.android.libraries.core.coroutine.childScope
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UniqueId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.media.AudioInfo
import io.element.android.libraries.matrix.api.media.FileInfo
import io.element.android.libraries.matrix.api.media.ImageInfo
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrix.api.media.VideoInfo
import io.element.android.libraries.matrix.api.media.toFile
import io.element.android.libraries.matrix.api.room.IntentionalMention
import io.element.android.libraries.matrix.api.room.roomMembers
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.ReceiptType
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.item.event.AudioMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.FileMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.LocationMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageTypeWithAttachment
import io.element.android.libraries.matrix.api.timeline.item.event.OtherMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ProfileChangeContent
import io.element.android.libraries.matrix.api.timeline.item.event.RedactedContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerContent
import io.element.android.libraries.matrix.api.timeline.item.event.StickerMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VoiceMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import io.element.android.libraries.matrix.api.timeline.item.event.toEventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.virtual.VirtualTimelineItem
import io.element.android.x.di.AppGraph
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import java.awt.FileDialog
import java.awt.Frame
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.action_redact
import shire.composeapp.generated.resources.action_redact_event_by_sender_prompt
import shire.composeapp.generated.resources.action_redact_event_prompt
import shire.composeapp.generated.resources.action_redact_message_by_sender_prompt
import shire.composeapp.generated.resources.action_redact_message_prompt
import shire.composeapp.generated.resources.command_copy_name_event_id
import shire.composeapp.generated.resources.command_copy_name_event_source
import shire.composeapp.generated.resources.command_copy_name_full_room_state
import shire.composeapp.generated.resources.command_copy_name_message_content
import shire.composeapp.generated.resources.command_copy_name_mxc
import shire.composeapp.generated.resources.command_copy_name_mxid
import shire.composeapp.generated.resources.command_copy_name_url
import shire.composeapp.generated.resources.command_event_name_fully_read_marker
import shire.composeapp.generated.resources.command_event_name_own_read_receipt
import shire.composeapp.generated.resources.command_event_name_reply
import shire.composeapp.generated.resources.command_fetching_state
import shire.composeapp.generated.resources.command_loading_event
import shire.composeapp.generated.resources.command_loading_timeline_at
import shire.composeapp.generated.resources.toast_attachment_download_path_success
import java.awt.Desktop
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.toPath
import kotlin.math.max

private data class ComposerSettings(
    val autoHideComposer: Boolean,
) {
    companion object {
        fun from(lookup: (ScPref<*>) -> Any?) = ComposerSettings(
            autoHideComposer = ScPrefs.AUTO_HIDE_COMPOSER.safeLookup(lookup),
        )
    }
}

sealed interface EventJumpTarget {
    val renavigationCount: Int
    data class Event(
        val eventId: EventId,
        val highlight: Boolean = true,
        override val renavigationCount: Int = 0,
    ) : EventJumpTarget {
        override fun withRenavigationCount(count: Int) = if (renavigationCount == count) this else copy(renavigationCount = count)
    }
    data class Index(
        val index: Int,
        override val renavigationCount: Int = 0,
    ) : EventJumpTarget {
        override fun withRenavigationCount(count: Int) = if (renavigationCount == count) this else copy(renavigationCount = count)
    }
    fun withRenavigationCount(count: Int): EventJumpTarget
    fun navigateFrom(old: EventJumpTarget?): EventJumpTarget {
        return if (old == null) {
            this
        } else if (old.withRenavigationCount(0) == this.withRenavigationCount(0)) {
            withRenavigationCount(max(renavigationCount, old.renavigationCount) + 1)
        } else {
            this
        }
    }
}

private fun buildScTimelineFilterSettings(lookup: (ScPref<*>) -> Any?) = ScTimelineFilterSettings(
    showHiddenEvents = ScPrefs.VIEW_HIDDEN_EVENTS.safeLookup(lookup),
    showRedactions = ScPrefs.VIEW_REDACTIONS.safeLookup(lookup),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModel(
    private val sessionId: SessionId,
    private val roomId: RoomId,
    private val appGraph: AppGraph = UiState.appGraph,
    private val scPreferencesStore: ScPreferencesStore = RevengePrefs,
) : ViewModel(), TitleProvider, SearchProvider, UserIdSuggestionsProvider, ComposerViewModel {
    private val log = Logger.withTag("ChatView/$roomId")

    private val searchQuery = MutableStateFlow<String?>(null)

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope)

    private val _targetEvent = MutableStateFlow<EventJumpTarget?>(EventJumpTarget.Index(0))
    val targetEvent = _targetEvent.asStateFlow()

    private val sessionGraphFlow = clientFlow.map { client ->
        client?.let {
            appGraph.sessionGraphFactory.create(it)
        }
    }

    private val timelineFilterSettings = scPreferencesStore.combinedSettingFlow { lookup ->
        buildScTimelineFilterSettings(lookup)
    }.stateIn(viewModelScope, SharingStarted.Eagerly,
        buildScTimelineFilterSettings { scPreferencesStore.getCachedOrDefaultValue(it) }
    )

    private val roomPair = combine(
        clientFlow,
        timelineFilterSettings,
    ) { client, settings ->
        Pair(client?.getRoom(roomId), client?.getJoinedRoom(roomId, settings))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Pair(null, null))

    val typingUsers = roomPair.flatMapLatest { (_, joined) ->
        joined?.roomTypingMembersFlow ?: flowOf(null)
    }

    private val _highlightedActionEventId = MutableStateFlow<EventOrTransactionId?>(null)
    val highlightedActionEventId = _highlightedActionEventId.asStateFlow()

    // TODO?
    private val powerLevels = roomPair.map { (_, room) ->
        room?.powerLevels()
            ?.onFailure { log.e("Failed to get room power levels", it) }
            ?.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val composerSettings = scPreferencesStore.combinedSettingFlow { lookup ->
        ComposerSettings.from(lookup)
    }.stateIn(viewModelScope, SharingStarted.Eagerly,
        ComposerSettings.from { scPreferencesStore.getCachedOrDefaultValue(it) }
    )

    private val roomGraphFlow = combine(sessionGraphFlow, roomPair) { sessionGraph, (baseRoom, joinedRoom) ->
        sessionGraph ?: return@combine null
        joinedRoom ?: return@combine null
        baseRoom ?: return@combine null
        sessionGraph.roomGraphFactory.create(joinedRoom, baseRoom)
    }

    private val currentUrlPreviewStateProvider = AtomicReference<UrlPreviewStateProvider?>(null)
    val urlPreviewStateProvider = combine(
        clientFlow,
        scPreferencesStore.combinedSettingFlow { lookup ->
            Pair(
                ScPrefs.URL_PREVIEWS.safeLookup(lookup),
                ScPrefs.URL_PREVIEWS_IN_E2EE_ROOMS.safeLookup(lookup),
            )
        },
        roomPair,
    ) { client, (enable, enableInE2ee), (room, _) ->
        client?.takeIf { enable && (enableInE2ee || (room?.info()?.isEncrypted == false)) }?.let {
            UrlPreviewStateProvider(
                urlPreviewProvider = UrlPreviewProvider(client),
                scope = viewModelScope.childScope(Dispatchers.IO, "urlPreviews"),
            )
        }.also {
            currentUrlPreviewStateProvider.getAndSet(it)?.clear()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val roomContextSuggestionsProvider = RoomContextSuggestionsProvider(
        sessionId = sessionId,
        peekRoom = { roomPair.value.first },
    )

    private val timelineController = flow {
        var controller: TimelineController? = null
        roomPair.collect {
            controller?.close()
            controller = it.second?.let { TimelineController(it) }
            emit(controller)
        }
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    override fun onCleared() {
        super.onCleared()
        timelineController.value?.close()
        currentUrlPreviewStateProvider.getAndSet(null)?.clear()
    }

    val activeTimeline = timelineController.flatMapLatest {
        it?.activeTimelineFlow() ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val rawTimelineItems = activeTimeline.flatMapLatest {
        it?.timelineItems ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val timelineItems = combine(
        rawTimelineItems,
        searchQuery
    ) { items, query ->
        if (query.isNullOrBlank() || items == null) {
            items
        } else {
            val lowerQuery = query.lowercase()
            items.flatMap {
                when (it) {
                    is MatrixTimelineItem.Event -> {
                        if ((it.event.content as? MessageContent)?.body?.lowercase()?.contains(lowerQuery) == true) {
                            listOf(
                                MatrixTimelineItem.Virtual(
                                    UniqueId("search_date_${it.eventId}"),
                                    VirtualTimelineItem.DayDivider(it.event.timestamp),
                                ),
                                it,
                            )
                        } else {
                            emptyList()
                        }
                    }
                    is MatrixTimelineItem.Virtual -> {
                        // Only show room beginning and paging indicator virtual items during search
                        when (it.virtual) {
                            is VirtualTimelineItem.LoadingIndicator,
                            VirtualTimelineItem.RoomBeginning,
                            VirtualTimelineItem.LastForwardIndicator -> listOf(it)
                            is VirtualTimelineItem.DayDivider,
                            VirtualTimelineItem.ReadMarker,
                            VirtualTimelineItem.TypingNotification -> emptyList()
                        }
                    }
                    is MatrixTimelineItem.Other -> emptyList()
                }
            }
        }?.toPersistentList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val forwardPaginationStatus = activeTimeline.flatMapLatest { it?.forwardPaginationStatus ?: flowOf(null) }
    val backwardPaginationStatus = activeTimeline.flatMapLatest { it?.backwardPaginationStatus ?: flowOf(null) }

    private val roomMembersState = roomPair.flatMapLatest { (_, joined) ->
        joined?.membersStateFlow ?: flowOf()
    }

    val roomMembers = roomMembersState.map {
        it.roomMembers()?.toImmutableList() ?: persistentListOf()
    }.stateIn(viewModelScope, SharingStarted.Lazily, persistentListOf())

    val roomMembersById = roomMembers.map {
        it.associateBy { it.userId }.toPersistentHashMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, persistentHashMapOf())

    override val userIdInRoomSuggestions: Flow<List<UserIdSuggestion>> = roomMembers.map {
        it.map {
            UserIdSuggestion(it.userId, it.displayName)
        }
    }

    private val draftKey = DraftKey(sessionId, roomId)
    override val composerState = DraftRepo.followDraft(draftKey).map {
        it ?: DraftValue()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DraftValue())

    private val composerSuggestionsProvider = ComposerSuggestionsProvider(
        queryFlow = composerState,
        userIdSuggestionsProvider = this,
        canPingRoomFlow = flowOf(true), // TODO check room ping permission
    )
    override val composerSuggestions: StateFlow<ComposerSuggestionsState> =
        composerSuggestionsProvider.suggestionsState
            .stateIn(viewModelScope, SharingStarted.Eagerly, ComposerSuggestionsState())

    private val forceShowComposer = MutableStateFlow(false)
    val shouldShowComposer = combine(
        composerState,
        forceShowComposer,
        scPreferencesStore.settingFlow(ScPrefs.MINIMAL_MODE),
    ) { state, force, minimalMode ->
        force || !minimalMode || !state.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, forceShowComposer.value)

    override fun onComposerUpdate(value: DraftValue) {
        DraftRepo.update(draftKey, value)
    }

    override fun sendMessage(context: ActionContext): ActionResult {
        val currentTimeline = activeTimeline.value
        if (currentTimeline == null) {
            log.e("Cannot send message on null timeline")
            return ActionResult.Failure("No timeline available for chat")
        }
        var currentDraft: DraftValue? = null
        DraftRepo.update(draftKey) {
            if (it == null || it.isEmpty() || it.isSendInProgress) {
                log.w("Refuse to send blank message")
                it
            } else {
                val validationError = it.bodyValidationError()
                if (validationError != null) {
                    log.e("Refuse to send message with validation error: $validationError")
                    it
                } else {
                    it.copy(isSendInProgress = true).also {
                        currentDraft = it
                    }
                }
            }
        }
        val draft = currentDraft ?: return ActionResult.Inapplicable
        context.launchActionAsync(
            "sendMessage",
            GlobalActionsScope,
            Dispatchers.IO
        ) {
            // In case user hasn't read the timeline yet, avoid stuck unread bugs caused by bugs
            // with implicit read receipts and local echos.
            // Shouldn't really matter if it's a private or public RR since we're about to send a message anyway,
            // but since this should only do anything at all if we didn't send a RR before, defaulting to private
            // should be more meaningful in case later actions fail.
            // TODO this can take a while, can we check if this is really necessary?
            currentTimeline.markAsRead(ReceiptType.READ_PRIVATE)
                .onFailure { log.e("Forwarding the RR on message send failed", it) }
                .onSuccess { log.d("Advanced the RR on message send") }
            val result = run result@{
                when (draft.type) {
                    DraftType.TEXT -> {
                        if (draft.inReplyTo != null) {
                            currentTimeline.replyMessage(
                                repliedToEventId = draft.inReplyTo.eventId,
                                body = draft.body,
                                htmlBody = draft.htmlBody,
                                intentionalMentions = draft.intentionalMentions,
                            )
                        } else {
                            currentTimeline.sendMessage(
                                body = draft.body,
                                htmlBody = draft.htmlBody,
                                intentionalMentions = draft.intentionalMentions,
                            )
                        }
                    }

                    DraftType.NOTICE -> {
                        currentTimeline.sendNotice(
                            body = draft.body,
                            htmlBody = draft.htmlBody,
                            intentionalMentions = draft.intentionalMentions,
                            inReplyToEventId = draft.inReplyTo?.eventId,
                        )
                    }

                    DraftType.EMOTE -> {
                        currentTimeline.sendEmote(
                            body = draft.body,
                            htmlBody = draft.htmlBody,
                            intentionalMentions = draft.intentionalMentions,
                            inReplyToEventId = draft.inReplyTo?.eventId,
                        )
                    }

                    DraftType.EDIT -> {
                        val editEventId = draft.editEventId ?: run {
                            return@result Result.failure(
                                IllegalArgumentException("Tried to edit message without eventId")
                            )
                        }
                        currentTimeline.editMessage(
                            eventOrTransactionId = editEventId,
                            body = draft.body,
                            htmlBody = draft.htmlBody,
                            intentionalMentions = draft.intentionalMentions,
                        )
                    }

                    DraftType.EDIT_CAPTION -> {
                        val editEventId = draft.editEventId ?: run {
                            return@result Result.failure(
                                IllegalArgumentException("Tried to edit caption without eventId")
                            )
                        }
                        currentTimeline.editCaption(
                            eventOrTransactionId = editEventId,
                            caption = draft.body,
                            formattedCaption = draft.htmlBody,
                        )
                    }

                    DraftType.REACTION -> {
                        val relatesToEventId = draft.inReplyTo?.eventId ?: run {
                            return@result Result.failure(
                                IllegalArgumentException("Tried to react without message eventId")
                            )
                        }
                        currentTimeline.toggleReaction(
                            emoji = draft.body,
                            eventOrTransactionId = relatesToEventId.toEventOrTransactionId(),
                        )
                    }

                    DraftType.ATTACHMENT -> {
                        val caption = draft.body.takeIf { it.isNotBlank() }
                        val formattedCaption = draft.htmlBody
                        when (val attachment = draft.attachment) {
                            is Attachment.Audio -> {
                                currentTimeline.sendAudio(
                                    file = attachment.file,
                                    audioInfo = attachment.audioInfo,
                                    caption = caption,
                                    formattedCaption = formattedCaption,
                                    inReplyToEventId = draft.inReplyTo?.eventId,
                                )
                            }

                            is Attachment.Generic -> {
                                currentTimeline.sendFile(
                                    file = attachment.file,
                                    fileInfo = attachment.fileInfo,
                                    caption = caption,
                                    formattedCaption = formattedCaption,
                                    inReplyToEventId = draft.inReplyTo?.eventId,
                                )
                            }

                            is Attachment.Image -> {
                                currentTimeline.sendImage(
                                    file = attachment.file,
                                    thumbnailFile = attachment.thumbnailFile,
                                    imageInfo = attachment.imageInfo,
                                    caption = caption,
                                    formattedCaption = formattedCaption,
                                    inReplyToEventId = draft.inReplyTo?.eventId,
                                )
                            }

                            is Attachment.Video -> {
                                currentTimeline.sendVideo(
                                    file = attachment.file,
                                    thumbnailFile = attachment.thumbnailFile,
                                    videoInfo = attachment.videoInfo,
                                    caption = caption,
                                    formattedCaption = formattedCaption,
                                    inReplyToEventId = draft.inReplyTo?.eventId,
                                )
                            }

                            null -> Result.failure(IllegalStateException("No attachment attached"))
                        }
                    }

                    DraftType.CUSTOM_EVENT -> {
                        val room = roomPair.value.second ?: return@result Result.failure(
                            IllegalStateException("Room not ready")
                        )
                        val eventType = draft.customEventType ?: return@result Result.failure(
                            IllegalStateException("Tried to send custom event without type")
                        )
                        room.sendRaw(
                            eventType = eventType,
                            content = draft.body,
                        )
                    }

                    DraftType.CUSTOM_STATE_EVENT -> {
                        val room = roomPair.value.second ?: return@result Result.failure(
                            IllegalStateException("Room not ready")
                        )
                        val eventType = draft.customEventType ?: return@result Result.failure(
                            IllegalStateException("Tried to send custom event without type")
                        )
                        room.sendRawState(
                            eventType = eventType,
                            stateKey = draft.stateKey ?: "",
                            content = draft.body,
                        ).also {
                            if (it.isSuccess) {
                                roomContextSuggestionsProvider.invalidateCachedState()
                            }
                        }
                    }
                }
            }
            if (result.isSuccess) {
                log.v("Message sent successfully in $roomId")
                DraftRepo.deleteDraft(draftKey)
                ActionResult.Success()
            } else {
                log.w("Failed to send message in $roomId", result.exceptionOrNull())
                DraftRepo.update(
                    draftKey,
                    draft.copy(isSendInProgress = false),
                    allowWhileSendInProgress = true
                )
                ActionResult.Failure("Failed to send message")
            }
        }
        if (composerSettings.value.autoHideComposer || draft.type == DraftType.REACTION) {
            forceShowComposer.value = false
        }
        return ActionResult.Success(async = true)
    }

    override fun clearAttachment() {
        DraftRepo.update(draftKey) {
            it?.copy(
                attachment = null,
                type = it.type.takeIf { it != DraftType.ATTACHMENT } ?: DraftType.TEXT,
            )?.takeIf { !it.isEmpty() }
        }
    }

    override fun attachFile(context: ActionContext, path: String): Boolean {
        val file = try {
            Uri.create(path).toPath().toFile()
        } catch (e: Exception) {
            log.w("Failed to parse file uri: $path", e)
            return false
        }
        return context.launchActionAsync(
            "addAttachment",
            viewModelScope,
            Dispatchers.IO,
            "addAttachment",
        ) {
            loadAttachmentFileIntoComposer(file)
        } is ActionResult.Success
    }

    override fun onConfirmSuggestion(suggestion: ComposerSuggestion): Boolean {
        val draft = composerState.value
        val completionEntity = draft.textFieldValue.getCurrentCompletionEntity() ?: run {
            log.e { "Cannot confirm autosuggestion, mismatch with current composer state" }
            return false
        }
        val intentionalMention = when (suggestion) {
            is ComposerUserMentionSuggestion -> IntentionalMention.User(suggestion.userId)
            is ComposerRoomMentionSuggestion -> IntentionalMention.Room
            else -> null
        }
        val oldText = draft.textFieldValue.text
        val newText = buildString {
            append(oldText.take(completionEntity.start))
            append(suggestion.value)
            if (suggestion.shouldAppendSpace) {
                append(" ")
            }
            append(oldText.substring(completionEntity.end))
        }
        val suggestionInsertEndIndex = completionEntity.start + suggestion.value.length
        val newDraftMention = intentionalMention?.let {
            DraftMention(
                start = completionEntity.start,
                end = suggestionInsertEndIndex,
                mention = intentionalMention,
            )
        }
        val newDraft = draft.copy(
            textFieldValue = TextFieldValue(
                text = newText,
                selection = TextRange(suggestionInsertEndIndex + 1),
            ),
            mentions = if (newDraftMention == null)
                draft.mentions
            else
                (draft.mentions + newDraftMention).toImmutableList(),
        )
        DraftRepo.update(draftKey, newDraft)
        return true
    }

    val userProfile = clientFlow.flatMapLatest { it?.userProfile ?: flowOf(null) }

    val roomInfo = roomPair.map { (it, _) -> it?.info() }

    override val windowTitle: Flow<ComposableStringHolder?> = combine(
        roomPair,
        userProfile,
    ) { (baseRoom, joinedRoom), user ->
        baseRoom?.info()?.name?.let {
            buildString {
                append(it)
                append(" - ")
                if (user?.displayName != null) {
                    append(user.displayName)
                } else {
                    append(sessionId.value)
                }
            }.toStringHolder()
        }
    }.filterNotNull()

    init {
        roomPair.onEach { (_, joinedRoom) ->
            joinedRoom?.updateMembers()
        }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    override fun verifyDestination(destination: Destination): Boolean {
        return destination is Destination.Conversation && destination.sessionId == sessionId && destination.roomId == roomId
    }

    fun paginateForward() {
        viewModelScope.launch(Dispatchers.IO) {
            log.d("Request forward pagination")
            timelineController.value?.paginate(Timeline.PaginationDirection.FORWARDS)
                ?.onFailure { log.w("Cannot paginate forwards") }
                ?.onSuccess { log.d("Paginated forwards") }
        }
    }

    fun paginateBackward() {
        viewModelScope.launch(Dispatchers.IO) {
            log.d("Request backward pagination")
            timelineController.value?.paginate(Timeline.PaginationDirection.BACKWARDS)
                ?.onFailure { log.w("Cannot paginate backwards") }
                ?.onSuccess { log.d("Paginated backwards") }
        }
    }

    private val roomActionProvider = RoomActionProvider(isInvite = false) {
        // roomPair may have stale information, request a new room to be sure
        clientFlow.value?.getRoom(roomId)
    }

    private val conversationActionProvider = object : KeyboardActionProvider<Action.Conversation> {
        override fun getPossibleActions() = Action.Conversation.entries.toSet()
        override fun ensureActionType(action: Action) = action as? Action.Conversation

        override fun handleNavigationModeEvent(context: ActionContext, key: KeyTrigger): ActionResult {
            val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
            return keyConfig.conversation.execute(context, key, ::handleAction)
        }

        override fun handleAction(
            context: ActionContext,
            action: Action.Conversation,
            args: List<String>
        ): ActionResult = context.run {
            when (action) {
                Action.Conversation.FocusComposer -> {
                    !forceShowComposer.getAndUpdate { true }
                    focusByRole(FocusRole.MESSAGE_COMPOSER)
                    ActionResult.Success()
                }

                Action.Conversation.HideComposerIfEmpty -> {
                    // Clear draft state (replies etc.) if empty
                    var wasEmpty = false
                    DraftRepo.update(draftKey) {
                        val isEmpty = it?.isEmpty() != false
                        wasEmpty = isEmpty
                        if (isEmpty) {
                            null
                        } else {
                            it
                        }
                    }
                    if (wasEmpty) {
                        forceShowComposer.getAndUpdate { false }.orActionInapplicable()
                    } else {
                        ActionResult.Inapplicable
                    }
                }

                Action.Conversation.ClearComposer -> {
                    // Discard all draft state
                    var wasEmpty = false
                    DraftRepo.update(draftKey) {
                        wasEmpty = it?.isEmpty() != false
                        null
                    }
                    if (wasEmpty) {
                        forceShowComposer.getAndUpdate { false }.orActionInapplicable()
                    } else {
                        forceShowComposer.value = false
                        ActionResult.Success()
                    }
                }

                Action.Conversation.ComposeMessage -> {
                    forceShowComposer.value = true
                    DraftRepo.update(draftKey) {
                        it?.copy(type = DraftType.TEXT, editEventId = null, initialBody = "", attachment = null)
                            ?: DraftValue(type = DraftType.TEXT)
                    }
                    focusByRole(FocusRole.MESSAGE_COMPOSER)
                    ActionResult.Success()
                }

                Action.Conversation.ComposeNotice -> {
                    forceShowComposer.value = true
                    DraftRepo.update(draftKey) {
                        it?.copy(type = DraftType.NOTICE, editEventId = null, initialBody = "", attachment = null)
                            ?: DraftValue(type = DraftType.NOTICE)
                    }
                    focusByRole(FocusRole.MESSAGE_COMPOSER)
                    ActionResult.Success()
                }

                Action.Conversation.ComposeEmote -> {
                    forceShowComposer.value = true
                    DraftRepo.update(draftKey) {
                        it?.copy(type = DraftType.EMOTE, editEventId = null, initialBody = "", attachment = null)
                            ?: DraftValue(type = DraftType.EMOTE)
                    }
                    focusByRole(FocusRole.MESSAGE_COMPOSER)
                    ActionResult.Success()
                }

                Action.Conversation.ComposeCustomEvent -> {
                    val eventType = args.firstOrNull().orActionValidationError()
                    forceShowComposer.value = true
                    DraftRepo.update(draftKey) {
                        it?.copy(
                            textFieldValue = it.textFieldValue.takeIf { it.text.isNotEmpty() }
                                ?: TextFieldValue("{\n\n}", TextRange(2)),
                            type = DraftType.CUSTOM_EVENT,
                            customEventType = eventType,
                            editEventId = null,
                            initialBody = "",
                            attachment = null
                        ) ?: DraftValue(
                            textFieldValue = TextFieldValue("{\n\n}", TextRange(2)),
                            type = DraftType.CUSTOM_EVENT,
                            customEventType = eventType,
                        )
                    }
                    focusByRole(FocusRole.MESSAGE_COMPOSER)
                    ActionResult.Success()
                }

                Action.Conversation.ComposeCustomStateEvent -> {
                    val eventType = args.firstOrNull().orActionValidationError()
                    val stateKey = args.getOrNull(1)
                    val room = roomPair.value.second ?: return@run ActionResult.Failure("Room not ready")
                    publishMessage(
                        AppMessage(
                            message = Res.string.command_fetching_state.toStringHolder(),
                            uniqueId = "fetchState",
                            canAutoDismiss = false
                        )
                    )
                    launchActionAsync(
                        "composeCustomStateEvent",
                        viewModelScope,
                        Dispatchers.IO,
                        "fetchState",
                    ) {
                        val initialStateRaw = room.getRawState(eventType, stateKey ?: "")
                            .onFailure { log.w("Failed to fetch state", it) }
                            .getOrNull()
                        val initialState = initialStateRaw?.let {
                            try {
                                val state = PrettyJson.parseToJsonElement(initialStateRaw)
                                PrettyJson.encodeToString(state.jsonObject["content"])
                            } catch (e: Exception) {
                                log.e("Failed to parse state", e)
                                null
                            }
                        }
                        val initialText = initialState?.let {
                            TextFieldValue(it, TextRange(it.length))
                        } ?: TextFieldValue("{\n\n}", TextRange(2))
                        dismissMessage("fetchState")
                        forceShowComposer.value = true
                        DraftRepo.update(draftKey) {
                            it?.copy(
                                textFieldValue = initialText,
                                type = DraftType.CUSTOM_STATE_EVENT,
                                customEventType = eventType,
                                stateKey = stateKey,
                                editEventId = null,
                                initialBody = if (initialState == null) "" else initialText.text,
                                attachment = null
                            ) ?: DraftValue(
                                textFieldValue = initialText,
                                initialBody = if (initialState == null) "" else initialText.text,
                                type = DraftType.CUSTOM_STATE_EVENT,
                                customEventType = eventType,
                                stateKey = stateKey,
                            )
                        }
                        focusByRole(FocusRole.MESSAGE_COMPOSER)
                        ActionResult.Success()
                    }
                }

                Action.Conversation.ComposerSend -> sendMessage(context)

                Action.Conversation.ComposerInsertAtCursor -> {
                    var hasDraft = false
                    DraftRepo.update(draftKey) {
                        hasDraft = it != null
                        it?.copy(
                            textFieldValue = it.textFieldValue.insertAtCursor(args[0])
                        )
                    }
                    hasDraft.orActionInapplicable()
                }

                Action.Conversation.ComposerPasteText -> {
                    val content = getStringFromClipboard()
                    if (content.isNullOrBlank()) {
                        ActionResult.Inapplicable
                    } else {
                        DraftRepo.update(draftKey) {
                            it?.copy(
                                textFieldValue = it.textFieldValue.insertAtCursor(content)
                            ) ?: DraftValue(
                                textFieldValue = TextFieldValue(content, TextRange(content.length))
                            )
                        }
                        ActionResult.Success()
                    }
                }

                Action.Conversation.ComposerPasteAttachment -> {
                    val files = getFilesFromClipboard()
                    if (files.isEmpty() || files.size > 1) {
                        ActionResult.Inapplicable
                    } else {
                        launchActionAsync(
                            "addAttachment",
                            viewModelScope,
                            Dispatchers.IO,
                            "addAttachment",
                        ) {
                            loadAttachmentFileIntoComposer(files[0])
                        }
                    }
                }

                Action.Conversation.ComposerAddAttachment -> launchAttachmentPicker(this)

                Action.Conversation.ComposerSuggestionFocusNext -> cycleComposerSuggestions(1)

                Action.Conversation.ComposerSuggestionFocusPrevious -> cycleComposerSuggestions(-1)

                Action.Conversation.ComposerSuggestionApplySelected -> {
                    val suggestion =
                        composerSuggestions.value.selectedSuggestion ?: return@run ActionResult.Inapplicable
                    onConfirmSuggestion(suggestion).orActionInapplicable()
                }

                Action.Conversation.JumpToOwnReadReceipt -> jumpToMessage(
                    action.name,
                    StringResourceHolder(Res.string.command_event_name_own_read_receipt),
                ) {
                    activeTimeline.value?.latestUserReceiptEventId(sessionId.value)?.let(::EventId)
                }

                Action.Conversation.JumpToFullyRead -> jumpToMessage(
                    action.name,
                    StringResourceHolder(Res.string.command_event_name_fully_read_marker),
                ) {
                    activeTimeline.value?.fullyReadEventId()?.let(::EventId)
                }

                Action.Conversation.JumpToBottom -> {
                    timelineController.value?.focusOnLive() ?: run {
                        log.e("Could not find timeline controller")
                        return@run ActionResult.Failure("Timeline not ready")
                    }
                    _targetEvent.update {
                        EventJumpTarget.Index(0).navigateFrom(it)
                    }
                    ActionResult.Success(async = true)
                }

                Action.Conversation.MarkRead -> {
                    val timeline = activeTimeline.value ?: return@run ActionResult.Failure("Timeline not ready")
                    launchActionAsync(
                        "markRead",
                        GlobalActionsScope,
                        Dispatchers.IO
                    ) {
                        timeline.markAsRead(ReceiptType.READ).toActionResult(async = true)
                    }
                }

                Action.Conversation.MarkReadPrivate -> {
                    val timeline = activeTimeline.value ?: return@run ActionResult.Failure("Timeline not ready")
                    launchActionAsync(
                        "markReadPrivate",
                        GlobalActionsScope,
                        Dispatchers.IO
                    ) {
                        timeline.markAsRead(ReceiptType.READ_PRIVATE).toActionResult(async = true)
                    }
                }

                Action.Conversation.MarkFullyRead -> {
                    val timeline = activeTimeline.value ?: return@run ActionResult.Failure("Timeline not ready")
                    launchActionAsync(
                        "markFullyRead",
                        GlobalActionsScope,
                        Dispatchers.IO
                    ) {
                        timeline.markAsRead(ReceiptType.FULLY_READ).toActionResult(async = true)
                    }
                }

                Action.Conversation.KickUser -> {
                    val room = roomPair.value.second ?: return@run ActionResult.Failure("Room not ready")
                    val userId = UserId(args.firstOrNull().orActionValidationError())
                    val reason = if (args.size > 1) {
                        args.subList(1, args.size).joinToString().takeIf(String::isNotBlank)
                    } else {
                        null
                    }
                    launchActionAsync(
                        "kickUser",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        notifyProcessing = true,
                        appMessageId = "kickUser",
                    ) {
                        room.kickUser(userId, reason).toActionResult(async = true)
                    }
                }

                Action.Conversation.InviteUser -> {
                    val room = roomPair.value.second ?: return@run ActionResult.Failure("Room not ready")
                    val userId = UserId(args.firstOrNull().orActionValidationError())
                    launchActionAsync(
                        "inviteUser",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        notifyProcessing = true,
                        appMessageId = "inviteUser",
                    ) {
                        room.inviteUserById(userId).toActionResult(async = true)
                    }
                }

                Action.Conversation.BanUser -> {
                    val room = roomPair.value.second ?: return@run ActionResult.Failure("Room not ready")
                    val userId = UserId(args.firstOrNull().orActionValidationError())
                    launchActionAsync(
                        "banUser",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        notifyProcessing = true,
                        appMessageId = "banUser",
                    ) {
                        room.banUser(userId).toActionResult(async = true)
                    }
                }

                Action.Conversation.UnbanUser -> {
                    val room = roomPair.value.second ?: return@run ActionResult.Failure("Room not ready")
                    val userId = UserId(args.firstOrNull().orActionValidationError())
                    val reason = if (args.size > 1) {
                        args.subList(1, args.size).joinToString().takeIf(String::isNotBlank)
                    } else {
                        null
                    }
                    launchActionAsync(
                        "unbanUser",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        notifyProcessing = true,
                        appMessageId = "unbanUser",
                    ) {
                        room.unbanUser(userId, reason).toActionResult(async = true)
                    }
                }

                Action.Conversation.CopyFullRoomState -> {
                    val room = roomPair.value.first ?: return@run ActionResult.Failure("Room not ready")
                    publishMessage(
                        AppMessage(
                            Res.string.command_fetching_state.toStringHolder(),
                            uniqueId = "copyFullRemoteState"
                        )
                    )
                    launchActionAsync(
                        "copyFullRoomState",
                        GlobalActionsScope,
                        Dispatchers.IO,
                        notifyProcessing = true,
                        appMessageId = "copyFullRemoteState",
                    ) {
                        val result = room.fetchFullRoomState()
                        dismissMessage("copyFullRemoteState")
                        if (result.isSuccess) {
                            val joined = result.getOrNull()?.parseRoomStateSnapshot(log)?.joinToString(",\n\n") {
                                if (it.stateKey.isEmpty()) {
                                    "# ${it.eventType}\n${it.content}"
                                } else {
                                    "# ${it.eventType} / ${it.stateKey}\n${it.content}"
                                }
                            } ?: "{}"
                            context.copyToClipboard(
                                joined,
                                Res.string.command_copy_name_full_room_state.toStringHolder()
                            )
                        } else {
                            result.toActionResult(async = true)
                        }
                    }
                }
            }
        }
    }

    val actionProvider = FlatMergedKeyboardActionProvider(
        listOf(conversationActionProvider, roomActionProvider)
    )

    private fun cycleComposerSuggestions(direction: Int): ActionResult {
        val state = composerSuggestions.value
        if (state.suggestions.isEmpty()) {
            return ActionResult.Inapplicable
        }
        val currentSuggestionIndex = if (state.selectedSuggestion == null) {
            -1
        } else {
            state.suggestions.indexOf(state.selectedSuggestion)
        }
        // + 1 allows clearing selection again on cycle completed
        val nextIndex = (currentSuggestionIndex + direction).mod(state.suggestions.size + 1)
        val nextSuggestion = state.suggestions.getOrNull(nextIndex)
        composerSuggestionsProvider.currentSelection.value = nextSuggestion
        return ActionResult.Success()
    }

    override fun launchAttachmentPicker(context: ActionContext) = context.launchActionAsync(
        "attachmentPicker",
        viewModelScope,
        Dispatchers.IO
    ) {
        return@launchActionAsync try {
            val dialog = FileDialog(null as Frame?, "Select attachment", FileDialog.LOAD)
            dialog.isMultipleMode = false
            dialog.isVisible = true

            val files = dialog.files
            if (files?.size == 1) {
                loadAttachmentFileIntoComposer(files[0])
            } else if ((files?.size ?: 0) > 1) {
                ActionResult.Failure("Selecting multiple attachments at once is not supported")
            } else {
                log.d("Attachment selection cancelled")
                ActionResult.Success()
            }
        } catch (t: Throwable) {
            log.e("Failed to open native file picker", t)
            ActionResult.Failure("Failed to open native file picker")
        }
    }

    suspend fun loadAttachmentFileIntoComposer(file: File): ActionResult = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext ActionResult.Failure("File does not exist: ${file.absolutePath}")
        }
        val mimetype = MimeUtil.detectMimeType(file)
        val attachmentType = MimeUtil.classifyFromMime(mimetype)
        val fileSize = file.length()
        val attachment = when (attachmentType) {
            MimeUtil.AttachmentKind.IMAGE -> {
                val measures = MediaInfoUtil.probeImage(file)
                Attachment.Image(
                    file = file,
                    thumbnailFile = null, // TODO?
                    imageInfo = ImageInfo(
                        height = measures.height?.toLong(),
                        width = measures.width?.toLong(),
                        mimetype = mimetype,
                        size = fileSize,
                        thumbnailInfo = null,
                        thumbnailSource = null,
                        blurhash = "", // TODO why is SDK/FFI refusing to send without blurhash
                    ),
                )
            }
            MimeUtil.AttachmentKind.VIDEO -> {
                Attachment.Video(
                    file = file,
                    thumbnailFile = null, // TODO?
                    videoInfo = VideoInfo(
                        duration = null,
                        height = null,
                        width = null,
                        mimetype = mimetype,
                        size = fileSize,
                        thumbnailInfo = null,
                        thumbnailSource = null,
                        blurhash = "", // TODO why is SDK/FFI refusing to send without blurhash
                    ),
                )
            }
            MimeUtil.AttachmentKind.AUDIO -> {
                Attachment.Audio(
                    file,
                    AudioInfo(
                        duration = null, // TODO
                        size = fileSize,
                        mimetype = mimetype,
                    )
                )
            }
            MimeUtil.AttachmentKind.OTHER -> {
                Attachment.Generic(
                    file,
                    FileInfo(
                        mimetype = mimetype,
                        size = fileSize,
                        thumbnailInfo = null,
                        thumbnailSource = null,
                    )
                )
            }
        }
        DraftRepo.update(draftKey) {
            it?.copy(
                attachment = attachment,
                type = DraftType.ATTACHMENT,
                editEventId = null,
                isSendInProgress = false,
            ) ?: DraftValue(
                attachment = attachment,
                type = DraftType.ATTACHMENT,
            )
        }
        ActionResult.Success()
    }

    private fun ActionContext.jumpToMessage(
        actionName: String,
        eventName: ComposableStringHolder,
        appMessageId: String = "jumpTo",
        getEventId: suspend () -> EventId?,
    ): ActionResult = launchActionAsync(
        actionName,
        viewModelScope,
        Dispatchers.IO
    ) {
        publishMessage(
            AppMessage(
                StringResourceHolder(Res.string.command_loading_event, eventName),
                uniqueId = appMessageId,
                canAutoDismiss = false,
            )
        )
        getEventId()?.let { eventId ->
            publishMessage(
                AppMessage(
                    StringResourceHolder(Res.string.command_loading_timeline_at, eventName),
                    uniqueId = appMessageId,
                    canAutoDismiss = false
                )
            )
            focusOnEvent(eventId).toActionResult().also {
                dismissMessage(appMessageId)
            }
        } ?: ActionResult.Failure("Could not find $eventName")
    }

    private fun ActionContext.redactWithConfirmation(
        eventOrTransactionId: EventOrTransactionId,
        isOwn: Boolean,
        senderName: String,
        isMessage: Boolean,
        redactReason: String? = null,
    ): ActionResult {
        val timeline = activeTimeline.value ?: return ActionResult.Failure("Room not ready")
        val message = when {
            isOwn -> if (isMessage) {
                Res.string.action_redact_message_prompt.toStringHolder()
            } else {
                Res.string.action_redact_event_prompt.toStringHolder()
            }
            else -> if (isMessage) {
                StringResourceHolder(Res.string.action_redact_message_by_sender_prompt, senderName.toStringHolder())
            } else {
                StringResourceHolder(Res.string.action_redact_event_by_sender_prompt, senderName.toStringHolder())
            }
        }
        _highlightedActionEventId.value = eventOrTransactionId
        return withCriticalActionConfirmation(
            prompt = message,
            confirmText = StringResourceHolder(Res.string.action_redact),
            onDismiss = {
                _highlightedActionEventId.update { it.takeIf { it != eventOrTransactionId } }
            }
        ) {
            launchActionAsync(
                "redact",
                GlobalActionsScope,
                notifyProcessing = true,
                appMessageId = ConfirmActionAppMessage.MESSAGE_ID,
            ) {
                timeline.redactEvent(eventOrTransactionId, reason = redactReason).toActionResult().also {
                    _highlightedActionEventId.update { it.takeIf { it != eventOrTransactionId } }
                }
            }
        }
    }

    suspend fun focusOnEvent(eventId: EventId): Result<EventFocusResult> {
        val controller = timelineController.value ?: run {
            log.e("No timeline controller to execute action")
            return Result.failure(RuntimeException("No TimelineController available"))
        }
        val timelineItems = timelineItems.value
        if (timelineItems != null) {
            if (timelineItems.any { item -> (item as? MatrixTimelineItem.Event)?.eventId == eventId }) {
                log.d { "Skip rebuilding timeline, focus item is already in current timeline" }
                _targetEvent.update {
                    EventJumpTarget.Event(eventId).navigateFrom(it)
                }
                return Result.success(EventFocusResult.FocusedOnLive) // TODO the other variant would be threaded??
            }
        }
        return controller.focusOnEvent(eventId, null)
            .onFailure { log.e("Failed to focus on event $eventId", it) }
            .onSuccess {
                _targetEvent.update {
                    EventJumpTarget.Event(eventId).navigateFrom(it)
                }
            }
    }

    private fun ActionContext.markEventAsRead(eventId: EventId, receiptType: ReceiptType): ActionResult {
        val timeline = timelineController.value ?: return ActionResult.Failure("Timeline not ready")
        return launchActionAsync("MarkEventAsRead", GlobalActionsScope, Dispatchers.IO) {
            var result: ActionResult? = null
            timeline.invokeOnCurrentTimeline {
                result = forceSendReadReceipt(eventId, receiptType)
                    .onFailure {
                        log.e("Failed to send read receipt $receiptType", it)
                    }
                    .toActionResult(async = true)
            }
            result ?: ActionResult.Failure("Failed to send $receiptType")
        }
    }

    fun getKeyboardActionProviderForEvent(event: EventTimelineItem): KeyboardActionProvider<Action.Event> {
        val eventId = event.eventId
        val eventOrTransactionId = tryOrNull {
            EventOrTransactionId.from(event.eventId, event.transactionId)
        }
        return object : KeyboardActionProvider<Action.Event> {
            override fun getPossibleActions() = Action.Event.entries.toSet().let {
                val content = event.content
                if (content !is MessageContent) {
                    it - setOfNotNull(
                        Action.Event.JumpToRepliedTo,
                        Action.Event.CopyContent,
                        Action.Event.CopyContentLink,
                        Action.Event.OpenContentLinks,
                        Action.Event.JumpToRepliedTo,
                    )
                } else if (content.inReplyTo == null) {
                    it - Action.Event.JumpToRepliedTo
                } else {
                    it
                }
            }.let {
                if (!event.isOwn) {
                    it - setOf(
                        Action.Event.ComposeEdit,
                    )
                } else {
                    it
                }
            }
            override fun ensureActionType(action: Action) = action as? Action.Event

            override fun handleNavigationModeEvent(context: ActionContext, key: KeyTrigger): ActionResult {
                val keyConfig = context.keybindingConfig ?: return ActionResult.NoMatch
                return keyConfig.event.execute(context, key, ::handleAction)
            }

            override fun handleAction(
                context: ActionContext,
                action: Action.Event,
                args: List<String>
            ): ActionResult = context.run {
                when (action) {
                    Action.Event.MarkRead -> eventId?.let {
                        markEventAsRead(eventId, ReceiptType.READ)
                    } ?: ActionResult.Inapplicable
                    Action.Event.MarkReadPrivate -> eventId?.let {
                        markEventAsRead(eventId, ReceiptType.READ_PRIVATE)
                    } ?: ActionResult.Inapplicable
                    Action.Event.MarkFullyRead -> eventId?.let {
                        markEventAsRead(eventId, ReceiptType.FULLY_READ)
                    } ?: ActionResult.Inapplicable

                    Action.Event.ComposeReply -> eventId?.let {
                        forceShowComposer.value = true
                        val inReplyTo = InReplyTo.Ready(
                            eventId = eventId,
                            content = event.content,
                            senderId = event.sender,
                            senderProfile = event.senderProfile,
                        )
                        DraftRepo.update(draftKey) {
                            it?.copy(inReplyTo = inReplyTo)
                                ?: DraftValue(inReplyTo = inReplyTo)
                        }
                        focusByRole(FocusRole.MESSAGE_COMPOSER)
                        ActionResult.Success()
                    } ?: ActionResult.Inapplicable

                    Action.Event.ComposeEdit -> eventOrTransactionId?.let {
                        val eventContent = event.content
                        if (eventContent is MessageContent) {
                            val draftValue = when (val messageType = eventContent.type) {
                                is TextLikeMessageType -> DraftValue(
                                    type = DraftType.EDIT,
                                    textFieldValue = insertTextFieldValue(messageType.body),
                                    editEventId = eventOrTransactionId,
                                    initialBody = messageType.body,
                                    // Not supported yet, TODO formatted edits?
                                    //htmlBody = messageType.formatted?.body,
                                    //intentionalMentions = // TODO?
                                )

                                is MessageTypeWithAttachment -> DraftValue(
                                    type = DraftType.EDIT_CAPTION,
                                    textFieldValue = insertTextFieldValue(messageType.caption ?: ""),
                                    editEventId = eventOrTransactionId,
                                    initialBody = messageType.caption ?: "",
                                    // Not supported yet, TODO formatted edits?
                                    //htmlBody = messageType.formattedCaption?.body,
                                )

                                is LocationMessageType,
                                is OtherMessageType -> null
                            }
                            if (draftValue == null) {
                                ActionResult.Inapplicable
                            } else {
                                forceShowComposer.value = true
                                DraftRepo.update(draftKey, draftValue)
                                focusByRole(FocusRole.MESSAGE_COMPOSER)
                                ActionResult.Success()
                            }
                        } else {
                            ActionResult.Inapplicable
                        }
                    } ?: ActionResult.Inapplicable

                    Action.Event.ComposeReaction -> eventId?.let {
                        forceShowComposer.value = true
                        val inReplyTo = InReplyTo.Ready(
                            eventId = eventId,
                            content = event.content,
                            senderId = event.sender,
                            senderProfile = event.senderProfile,
                        )
                        DraftRepo.update(draftKey) {
                            it?.copy(
                                inReplyTo = inReplyTo,
                                type = DraftType.REACTION,
                                attachment = null,
                                editEventId = null,
                            ) ?: DraftValue(
                                textFieldValue = TextFieldValue(":", TextRange(1)),
                                initialBody = ":",
                                inReplyTo = inReplyTo,
                                type = DraftType.REACTION,
                            )
                        }
                        focusByRole(FocusRole.MESSAGE_COMPOSER)
                        ActionResult.Success()
                    } ?: ActionResult.Inapplicable

                    Action.Event.CopyContent -> {
                        (event.content as? MessageContent)?.body?.let { content ->
                            copyToClipboard(content, Res.string.command_copy_name_message_content.toStringHolder())
                        } ?: ActionResult.Inapplicable
                    }

                    Action.Event.CopyEventSource -> {
                        event.timelineItemDebugInfoProvider().originalJson?.toPrettyJson()?.let { eventSource ->
                            copyToClipboard(eventSource, Res.string.command_copy_name_event_source.toStringHolder())
                        } ?: ActionResult.Inapplicable
                    }

                    Action.Event.CopyEventId -> {
                        (eventId?.value ?: event.transactionId?.value)?.let {
                            copyToClipboard(it, Res.string.command_copy_name_event_id.toStringHolder())
                        } ?: ActionResult.Inapplicable
                    }

                    Action.Event.CopyMxId -> {
                        copyToClipboard(event.sender.value, Res.string.command_copy_name_mxid.toStringHolder())
                    }

                    Action.Event.CopyMxc -> {
                        val url = event.mediaSource()?.url
                        if (url == null) {
                            ActionResult.Inapplicable
                        } else {
                            copyToClipboard(url, Res.string.command_copy_name_mxc.toStringHolder())
                        }
                    }

                    Action.Event.CopyContentLink -> {
                        (event.content as? MessageContent)?.body?.let { content ->
                            UrlUtil.extractUrlsFromText(content).firstOrNull()?.let {
                                copyToClipboard(content, Res.string.command_copy_name_url.toStringHolder())
                            }
                        } ?: ActionResult.Inapplicable
                    }

                    Action.Event.OpenContentLinks -> {
                        (event.content as? MessageContent)?.body?.let { content ->
                            val links = UrlUtil.extractUrlsFromText(content)
                            if (links.isEmpty()) {
                                ActionResult.Inapplicable
                            } else {
                                links.forEach {
                                    openLinkInExternalBrowser(it).let {
                                        if (it is ActionResult.Failure) {
                                            return@run it
                                        }
                                    }
                                }
                                ActionResult.Success()
                            }
                        } ?: ActionResult.Inapplicable
                    }

                    Action.Event.Redact -> {
                        if (event.content is RedactedContent) {
                            return@run ActionResult.Inapplicable
                        }
                        eventOrTransactionId?.let {
                            redactWithConfirmation(
                                eventOrTransactionId = eventOrTransactionId,
                                isOwn = event.isOwn,
                                senderName = event.senderProfile.getDisambiguatedDisplayName(event.sender),
                                isMessage = event.content is MessageContent,
                            )
                        } ?: ActionResult.Inapplicable
                    }

                    Action.Event.JumpToRepliedTo -> {
                        val inReplyTo = (event.content as? MessageContent)?.inReplyTo
                            ?: return@run ActionResult.Inapplicable
                        jumpToMessage(
                            action.name,
                            Res.string.command_event_name_reply.toStringHolder(),
                        ) {
                            inReplyTo.eventId
                        }
                    }

                    Action.Event.DownloadFile -> downloadFileAndOpenExplorer(context, event)

                    Action.Event.DownloadFileAndOpen -> downloadFileAndOpen(context, event)

                    Action.Event.ToggleReactionKey -> {
                        val reaction = args.firstOrNull().orActionValidationError()
                        eventOrTransactionId ?: return@run ActionResult.Inapplicable
                        val timeline = activeTimeline.value ?: return@run ActionResult.Failure("Timeline not ready")
                        launchActionAsync(
                            "toggleReaction",
                            viewModelScope,
                            Dispatchers.IO,
                            "toggleReaction",
                            notifyProcessing = true,
                        ) {
                            timeline.toggleReaction(reaction, eventOrTransactionId)
                            ActionResult.Success()
                        }
                    }

                    Action.Event.ToggleReactionIndex -> {
                        val index = args.firstOrNull()?.toIntOrNull().orActionValidationError()
                        val reactionToToggle = event.reactions.getOrNull(index) ?: return@run ActionResult.Inapplicable
                        eventOrTransactionId ?: return@run ActionResult.Inapplicable
                        val timeline = activeTimeline.value ?: return@run ActionResult.Failure("Timeline not ready")
                        launchActionAsync(
                            "toggleReaction",
                            viewModelScope,
                            Dispatchers.IO,
                            "toggleReaction",
                            notifyProcessing = true,
                        ) {
                            timeline.toggleReaction(reactionToToggle.key, eventOrTransactionId)
                            ActionResult.Success()
                        }
                    }
                }
            }
        }
    }

    fun toggleReaction(eventOrTransactionId: EventOrTransactionId, emoji: String): Boolean {
        val timeline = activeTimeline.value ?: return false
        viewModelScope.launch(Dispatchers.IO) {
            timeline.toggleReaction(emoji, eventOrTransactionId)
        }
        return true
    }

    fun downloadFileAndOpenExplorer(
        context: ActionContext,
        event: EventTimelineItem,
    ) = context.downloadFile(event) { file ->
        try {
            // View in file explorer
            val desktop = Desktop.getDesktop()
            desktop.open(file.parentFile)
        } catch (t: Throwable) {
            log.e("Failed to open file explorer", t)
        }
        ActionResult.Success()
    }

    fun downloadFileAndOpen(
        context: ActionContext,
        event: EventTimelineItem,
    ) = context.downloadFile(event) { file ->
        try {
            val desktop = Desktop.getDesktop()
            desktop.open(file)
            ActionResult.Success()
        } catch (t: Throwable) {
            log.e("Failed to open file", t)
            ActionResult.Failure(t.message ?: "Failed to open file")
        }
    }

    private fun ActionContext.downloadFile(
        event: EventTimelineItem,
        onSuccess: (File) -> ActionResult,
    ): ActionResult {
        val mediaSource = event.mediaSource() ?: return ActionResult.Inapplicable
        val client = clientFlow.value ?: return ActionResult.Failure("Client not ready")
        val mediaLoader = client.matrixMediaLoader
        val appMessageId = "downloadFile_${mediaSource.url}"
        val filename = event.mediaFilename()
        val outFile = PersistentAttachmentDownload.getPersistentAttachmentFile(
            sessionId = sessionId,
            roomId = roomId,
            timestamp = event.timestamp,
            mxcUrl = mediaSource.url,
            filename = filename,
        )
        if (outFile.exists() && outFile.length() > 0) {
            return onSuccess(outFile)
        }
        return launchActionAsync(
            "downloadFile",
            viewModelScope,
            Dispatchers.IO,
            appMessageId,
        ) {
            val result = mediaLoader.downloadMediaFile(
                source = mediaSource,
                mimeType = event.mediaMimetype(),
                filename = filename,
            )
            val file = result.getOrNull()
            if (file != null) {
                val fileToOpen = try {
                    /* TODO why does this not work
                    val persistSuccess = file.persist(outFile.path)
                    if (persistSuccess) {
                        outFile
                    } else {
                        log.e("Failed to persist attachment file to ${outFile.path}, using temp one")
                        file.toFile()
                    }
                     */
                    Files.copy(file.toFile().toPath(), outFile.toPath())
                    outFile
                } catch (t: Throwable) {
                    log.e("Failed to persist attachment file, using temp one", t)
                    file.toFile()
                }
                publishMessage(
                    AppMessage(
                        StringResourceHolder(
                            Res.string.toast_attachment_download_path_success,
                            file.path().toStringHolder()
                        ),
                        uniqueId = appMessageId,
                    )
                )
                onSuccess(fileToOpen).also {
                    if (it !is ActionResult.Success) {
                        return@launchActionAsync it
                    }
                }
            }
            result.toActionResult(async = true, notifySuccess = false)
        }
    }

    override fun onSearchType(query: String) {
        searchQuery.value = query
    }

    override fun onSearchEnter(query: String) {
        searchQuery.value = query
    }

    override fun onSearchCleared() {
        searchQuery.value = null
    }

    companion object {
        fun factory(
            sessionId: SessionId,
            roomId: RoomId,
        ) = viewModelFactory {
            initializer {
                ConversationViewModel(sessionId, roomId)
            }
        }
    }
}

private fun EventTimelineItem.mediaSource() = when (val content = content) {
    is StickerContent -> content.source
    is MessageContent -> {
        (content.type as? MessageTypeWithAttachment)?.source
    }
    is ProfileChangeContent -> {
        content.avatarUrl?.let { MediaSource(it) }
    }
    else -> null
}

private fun EventTimelineItem.mediaMimetype() = when (val content = content) {
    is MessageContent -> {
        when (val type = content.type) {
            !is MessageTypeWithAttachment -> null
            is AudioMessageType -> type.info?.mimetype
            is FileMessageType -> type.info?.mimetype
            is ImageMessageType -> type.info?.mimetype
            is StickerMessageType -> type.info?.mimetype
            is VideoMessageType -> type.info?.mimetype
            is VoiceMessageType -> type.info?.mimetype
        }
    }
    else -> null
}

private fun EventTimelineItem.mediaFilename() = when (val content = content) {
    is MessageContent -> (content.type as? MessageTypeWithAttachment)?.filename
    else -> null
}
