package chat.schildi.revenge.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.preferences.RevengePrefs
import chat.schildi.preferences.ScPref
import chat.schildi.preferences.ScPrefs
import chat.schildi.preferences.safeLookup
import chat.schildi.revenge.TitleProvider
import chat.schildi.revenge.UiState
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.Destination
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.KeyboardActionHandler
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.compose.util.insertAtCursor
import chat.schildi.revenge.compose.util.insertTextFieldValue
import chat.schildi.revenge.compose.util.toStringHolder
import chat.schildi.revenge.config.keybindings.Action
import chat.schildi.revenge.config.keybindings.KeyTrigger
import chat.schildi.revenge.toPrettyJson
import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import io.element.android.features.messages.impl.timeline.EventFocusResult
import io.element.android.features.messages.impl.timeline.TimelineController
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.room.roomMembers
import io.element.android.libraries.matrix.api.timeline.ReceiptType
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.item.event.EventOrTransactionId
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.InReplyTo
import io.element.android.libraries.matrix.api.timeline.item.event.LocationMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.MessageTypeWithAttachment
import io.element.android.libraries.matrix.api.timeline.item.event.OtherMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.toEventOrTransactionId
import io.element.android.x.di.AppGraph
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    data class Event(val eventId: EventId, val hightlight: Boolean = true) : EventJumpTarget
    data class Index(val index: Int) : EventJumpTarget
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModel(
    private val sessionId: SessionId,
    private val roomId: RoomId,
    private val keyboardActionHandler: KeyboardActionHandler,
    private val appGraph: AppGraph = UiState.appGraph,
) : ViewModel(), TitleProvider, KeyboardActionProvider, ComposerViewModel {
    private val log = Logger.withTag("ChatView/$roomId")

    private val clientFlow = UiState.selectClient(sessionId, viewModelScope)

    private val _targetEvent = MutableStateFlow<EventJumpTarget?>(EventJumpTarget.Index(0))
    val targetEvent = _targetEvent.asStateFlow()

    private val sessionGraphFlow = clientFlow.map { client ->
        client?.let {
            appGraph.sessionGraphFactory.create(it)
        }
    }
    private val roomPair = clientFlow.map { client ->
        Pair(client?.getRoom(roomId), client?.getJoinedRoom(roomId))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Pair(null, null))

    private val composerSettings = RevengePrefs.combinedSettingFlow { lookup ->
        ComposerSettings.from(lookup)
    }.stateIn(viewModelScope, SharingStarted.Eagerly,
        ComposerSettings.from { RevengePrefs.getCachedOrDefaultValue(it) }
    )

    private val roomGraphFlow = combine(sessionGraphFlow, roomPair) { sessionGraph, (baseRoom, joinedRoom) ->
        sessionGraph ?: return@combine null
        joinedRoom ?: return@combine null
        baseRoom ?: return@combine null
        sessionGraph.roomGraphFactory.create(joinedRoom, baseRoom)
    }

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
    }

    val activeTimeline = timelineController.flatMapLatest {
        it?.activeTimelineFlow() ?: flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val timelineItems = activeTimeline.flatMapLatest {
        it ?: return@flatMapLatest flowOf(persistentListOf())
        it.timelineItems.map { it.toPersistentList() }
    }

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

    private val draftKey = DraftKey(sessionId, roomId)
    override val composerState = DraftRepo.followDraft(draftKey).map {
        it ?: DraftValue()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DraftValue())

    private val forceShowComposer = MutableStateFlow(false)
    val shouldShowComposer = combine(composerState, forceShowComposer) { state, force ->
        force || !state.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, forceShowComposer.value)

    override fun onComposerUpdate(value: DraftValue) {
        DraftRepo.update(draftKey, value)
    }

    override fun sendMessage(): Boolean {
        val draft = composerState.value
        if (draft.isEmpty()) {
            log.w("Refuse to send blank message")
            return false
        }
        val currentTimeline = activeTimeline.value
        if (currentTimeline == null) {
            log.e("Cannot send message on null timeline")
            return false
        }
        DraftRepo.update(draftKey, draft.copy(isSendInProgress = true))
        viewModelScope.launch {
            val result = when (draft.type) {
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
                        log.e("Tried to edit message without eventId")
                        return@launch
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
                        log.e("Tried to edit caption without eventId")
                        return@launch
                    }
                    currentTimeline.editCaption(
                        eventOrTransactionId = editEventId,
                        caption = draft.body,
                        formattedCaption = draft.htmlBody,
                    )
                }
                DraftType.REACTION -> {
                    val relatesToEventId = draft.inReplyTo?.eventId ?: run {
                        log.e("Tried to react without message eventId")
                        return@launch
                    }
                    currentTimeline.toggleReaction(
                        emoji = draft.body,
                        eventOrTransactionId = relatesToEventId.toEventOrTransactionId(),
                    )
                }
            }
            if (result.isSuccess) {
                log.v("Message sent successfully in $roomId")
                DraftRepo.deleteDraft(draftKey)
            } else {
                log.w("Failed to send message in $roomId")
                DraftRepo.update(draftKey, draft.copy(isSendInProgress = false))
            }
        }
        if (composerSettings.value.autoHideComposer) {
            forceShowComposer.value = false
        }
        return true
    }

    val userProfile = clientFlow.flatMapLatest { it?.userProfile ?: flowOf(null) }

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
        }.launchIn(viewModelScope)
    }

    override fun verifyDestination(destination: Destination): Boolean {
        return destination is Destination.Conversation && destination.sessionId == sessionId && destination.roomId == roomId
    }

    fun paginateForward() {
        viewModelScope.launch {
            log.d("Request forward pagination")
            timelineController.value?.paginate(Timeline.PaginationDirection.FORWARDS)
                ?.onFailure { log.w("Cannot paginate forwards") }
                ?.onSuccess { log.d("Paginated forwards") }
        }
    }

    fun paginateBackward() {
        viewModelScope.launch {
            log.d("Request backward pagination")
            timelineController.value?.paginate(Timeline.PaginationDirection.BACKWARDS)
                ?.onFailure { log.w("Cannot paginate backwards") }
                ?.onSuccess { log.d("Paginated backwards") }
        }
    }

    override fun handleNavigationModeEvent(key: KeyTrigger): Boolean {
        val keyConfig = UiState.keybindingsConfig.value
        return keyConfig.conversation.execute(key) { conversationAction ->
            when (conversationAction.action) {
                Action.Conversation.SetSetting -> {
                    viewModelScope.launch {
                        RevengePrefs.handleSetAction(conversationAction.args)
                    }
                    true
                }

                Action.Conversation.ToggleSetting -> {
                    viewModelScope.launch {
                        RevengePrefs.handleToggleAction(conversationAction.args)
                    }
                    true
                }

                Action.Conversation.FocusComposer -> {
                    !forceShowComposer.getAndUpdate { true }
                    keyboardActionHandler.focusByRole(FocusRole.MESSAGE_COMPOSER)
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
                        forceShowComposer.getAndUpdate { false }
                    } else {
                        false
                    }
                }

                Action.Conversation.ComposeMessage -> {
                    forceShowComposer.value = true
                    DraftRepo.update(draftKey) {
                        it?.copy(type = DraftType.TEXT, editEventId = null, initialBody = "")
                            ?: DraftValue(type = DraftType.TEXT)
                    }
                    keyboardActionHandler.focusByRole(FocusRole.MESSAGE_COMPOSER)
                    true
                }

                Action.Conversation.ComposeNotice -> {
                    forceShowComposer.value = true
                    DraftRepo.update(draftKey) {
                        it?.copy(type = DraftType.NOTICE, editEventId = null, initialBody = "")
                            ?: DraftValue(type = DraftType.NOTICE)
                    }
                    keyboardActionHandler.focusByRole(FocusRole.MESSAGE_COMPOSER)
                    true
                }

                Action.Conversation.ComposeEmote -> {
                    forceShowComposer.value = true
                    DraftRepo.update(draftKey) {
                        it?.copy(type = DraftType.EMOTE, editEventId = null, initialBody = "")
                            ?: DraftValue(type = DraftType.EMOTE)
                    }
                    keyboardActionHandler.focusByRole(FocusRole.MESSAGE_COMPOSER)
                    true
                }

                Action.Conversation.ComposerSend -> sendMessage()

                Action.Conversation.ComposerInsertAddCursor -> {
                    if (conversationAction.args.size != 1) {
                        log.e("Invalid parameter size for ComposerInsertAddCursor action, expected 1 got ${conversationAction.args.size}")
                        return@execute false
                    }
                    var hasDraft = false
                    DraftRepo.update(draftKey) {
                        hasDraft = it != null
                        it?.copy(
                            textFieldValue = it.textFieldValue.insertAtCursor(conversationAction.args[0])
                        )
                    }
                    hasDraft
                }

                Action.Conversation.JumpToLastFullyRead -> {
                    viewModelScope.launch {
                        activeTimeline.value?.fullyReadEventId()?.let { eventId ->
                            focusOnEvent(EventId(eventId))
                        } ?: run {
                            log.e("Could not find fully read eventId")
                        }
                    }
                    true
                }

                Action.Conversation.JumpToBottom -> {
                    timelineController.value?.focusOnLive() ?: run {
                        log.e("Could not find timeline controller")
                        return@execute false
                    }
                    _targetEvent.value = EventJumpTarget.Index(0)
                    true
                }
            }
        }
    }

    suspend fun focusOnEvent(eventId: EventId): Result<EventFocusResult> {
        val controller = timelineController.value ?: run {
            log.e("No timeline controller to execute action")
            return Result.failure(RuntimeException("No TimelineController available"))
        }
        return controller.focusOnEvent(eventId, null)
            .onFailure { log.e("Failed to focus on event $eventId", it) }
            .onSuccess { _targetEvent.emit(EventJumpTarget.Event(eventId)) }
    }

    private fun markEventAsRead(eventId: EventId, receiptType: ReceiptType): Boolean {
        val timeline = timelineController.value ?: run {
            log.e { "No timeline to execute event action" }
            return false
        }
        viewModelScope.launch {
            timeline.invokeOnCurrentTimeline {
                sendReadReceipt(eventId, receiptType)
                    .onFailure { log.e("Failed to send private read receipt", it) }
                // Always keep fully read in sync with read receipts for now
                sendReadReceipt(eventId, ReceiptType.FULLY_READ)
                    .onFailure { log.e("Failed to send fully read marker", it) }
            }
        }
        return true
    }

    fun getKeyboardActionProviderForEvent(event: EventTimelineItem): KeyboardActionProvider {
        val eventId = event.eventId
        val eventOrTransactionId = tryOrNull {
            EventOrTransactionId.from(event.eventId, event.transactionId)
        }
        return object : KeyboardActionProvider {
            override fun handleNavigationModeEvent(key: KeyTrigger): Boolean {
                return UiState.keybindingsConfig.value.event.execute(key) { binding ->
                    when (binding.action) {
                        Action.Event.MarkRead -> eventId?.let { markEventAsRead(eventId, ReceiptType.READ) } ?: false
                        Action.Event.MarkReadPrivate -> eventId?.let {
                            markEventAsRead(
                                eventId,
                                ReceiptType.READ_PRIVATE
                            )
                        } ?: false

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
                            keyboardActionHandler.focusByRole(FocusRole.MESSAGE_COMPOSER)
                            true
                        } ?: false

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
                                    false
                                } else {
                                    forceShowComposer.value = true
                                    DraftRepo.update(draftKey, draftValue)
                                    keyboardActionHandler.focusByRole(FocusRole.MESSAGE_COMPOSER)
                                    true
                                }
                            } else {
                                false
                            }
                        } ?: false

                        Action.Event.ComposeReaction -> eventId?.let {
                            forceShowComposer.value = true
                            val inReplyTo = InReplyTo.Ready(
                                eventId = eventId,
                                content = event.content,
                                senderId = event.sender,
                                senderProfile = event.senderProfile,
                            )
                            DraftRepo.update(draftKey) {
                                it?.copy(inReplyTo = inReplyTo, type = DraftType.REACTION)
                                    ?: DraftValue(inReplyTo = inReplyTo, type = DraftType.REACTION)
                            }
                            keyboardActionHandler.focusByRole(FocusRole.MESSAGE_COMPOSER)
                        } ?: false

                        Action.Event.CopyContent -> {
                            (event.content as? MessageContent)?.body?.let { content ->
                                keyboardActionHandler.copyToClipboard(content)
                            } ?: false
                        }

                        Action.Event.CopyEventSource -> {
                            event.timelineItemDebugInfoProvider().originalJson?.toPrettyJson()?.let { eventSource ->
                                keyboardActionHandler.copyToClipboard(eventSource)
                            } ?: false
                        }

                        Action.Event.CopyEventId -> {
                            (eventId?.value ?: event.transactionId?.value)?.let {
                                keyboardActionHandler.copyToClipboard(it)
                            } ?: false
                        }

                        Action.Event.CopyMxId -> {
                            keyboardActionHandler.copyToClipboard(event.sender.value)
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun factory(
            sessionId: SessionId,
            roomId: RoomId,
            keyboardActionHandler: KeyboardActionHandler,
        ) = viewModelFactory {
            initializer {
                ConversationViewModel(sessionId, roomId, keyboardActionHandler)
            }
        }
    }
}
