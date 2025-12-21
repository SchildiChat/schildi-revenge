package chat.schildi.revenge.model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import chat.schildi.matrixsdk.ScTimelineFilterSettings
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
import chat.schildi.revenge.actions.ActionContext
import chat.schildi.revenge.actions.ActionResult
import chat.schildi.revenge.actions.AppMessage
import chat.schildi.revenge.actions.ConfirmActionAppMessage
import chat.schildi.revenge.actions.FocusRole
import chat.schildi.revenge.actions.KeyboardActionProvider
import chat.schildi.revenge.actions.execute
import chat.schildi.revenge.actions.launchActionAsync
import chat.schildi.revenge.actions.orActionInapplicable
import chat.schildi.revenge.actions.toActionResult
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
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.media.AudioInfo
import io.element.android.libraries.matrix.api.media.FileInfo
import io.element.android.libraries.matrix.api.media.ImageInfo
import io.element.android.libraries.matrix.api.media.VideoInfo
import kotlin.time.Duration.Companion.milliseconds
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
import io.element.android.libraries.matrix.api.timeline.item.event.RedactedContent
import io.element.android.libraries.matrix.api.timeline.item.event.TextLikeMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.getDisambiguatedDisplayName
import io.element.android.libraries.matrix.api.timeline.item.event.toEventOrTransactionId
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
import shire.composeapp.generated.resources.command_copy_name_message_content
import shire.composeapp.generated.resources.command_copy_name_mxid
import shire.composeapp.generated.resources.command_copy_name_url
import shire.composeapp.generated.resources.command_event_name_fully_read_marker
import shire.composeapp.generated.resources.command_event_name_own_read_receipt
import shire.composeapp.generated.resources.command_loading_event
import shire.composeapp.generated.resources.command_loading_timeline_at
import java.io.File
import kotlin.io.path.toPath

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
    data class Event(val eventId: EventId, val highlight: Boolean = true) : EventJumpTarget
    data class Index(val index: Int) : EventJumpTarget
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

    override fun sendMessage(context: ActionContext): ActionResult {
        val draft = composerState.value
        if (draft.isEmpty()) {
            log.w("Refuse to send blank message")
            return ActionResult.Inapplicable
        }
        val currentTimeline = activeTimeline.value
        if (currentTimeline == null) {
            log.e("Cannot send message on null timeline")
            return ActionResult.Failure("No timeline available for chat")
        }
        DraftRepo.update(draftKey, draft.copy(isSendInProgress = true))
        context.launchActionAsync(
            "sendMessage",
            GlobalActionsScope,
            Dispatchers.IO
        ) {
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
                        return@launchActionAsync ActionResult.Failure("Tried to edit message without eventId")
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
                        return@launchActionAsync ActionResult.Failure("Tried to edit caption without eventId")
                    }
                    currentTimeline.editCaption(
                        eventOrTransactionId = editEventId,
                        caption = draft.body,
                        formattedCaption = draft.htmlBody,
                    )
                }
                DraftType.REACTION -> {
                    val relatesToEventId = draft.inReplyTo?.eventId ?: run {
                        return@launchActionAsync ActionResult.Failure("Tried to react without message eventId")
                    }
                    currentTimeline.toggleReaction(
                        emoji = draft.body,
                        eventOrTransactionId = relatesToEventId.toEventOrTransactionId(),
                    )
                }
                DraftType.ATTACHMENT -> {
                    val caption = draft.body.takeIf { it.isNotBlank() }
                    val formattedCaption = null // TODO?
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
            }
            if (result.isSuccess) {
                log.v("Message sent successfully in $roomId")
                DraftRepo.deleteDraft(draftKey)
                ActionResult.Success()
            } else {
                log.w("Failed to send message in $roomId", result.exceptionOrNull())
                DraftRepo.update(draftKey, draft.copy(isSendInProgress = false))
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

    override fun handleNavigationModeEvent(context: ActionContext, key: KeyTrigger): ActionResult {
        val keyConfig = UiState.keybindingsConfig.value ?: return ActionResult.NoMatch
        return keyConfig.conversation.execute(context, key) { conversationAction ->
            when (conversationAction.action) {
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
                        it?.copy(type = DraftType.TEXT, editEventId = null, initialBody = "")
                            ?: DraftValue(type = DraftType.TEXT)
                    }
                    focusByRole(FocusRole.MESSAGE_COMPOSER)
                    ActionResult.Success()
                }

                Action.Conversation.ComposeNotice -> {
                    forceShowComposer.value = true
                    DraftRepo.update(draftKey) {
                        it?.copy(type = DraftType.NOTICE, editEventId = null, initialBody = "")
                            ?: DraftValue(type = DraftType.NOTICE)
                    }
                    focusByRole(FocusRole.MESSAGE_COMPOSER)
                    ActionResult.Success()
                }

                Action.Conversation.ComposeEmote -> {
                    forceShowComposer.value = true
                    DraftRepo.update(draftKey) {
                        it?.copy(type = DraftType.EMOTE, editEventId = null, initialBody = "")
                            ?: DraftValue(type = DraftType.EMOTE)
                    }
                    focusByRole(FocusRole.MESSAGE_COMPOSER)
                    ActionResult.Success()
                }

                Action.Conversation.ComposerSend -> sendMessage(context)

                Action.Conversation.ComposerInsertAtCursor -> {
                    var hasDraft = false
                    DraftRepo.update(draftKey) {
                        hasDraft = it != null
                        it?.copy(
                            textFieldValue = it.textFieldValue.insertAtCursor(conversationAction.args[0])
                        )
                    }
                    hasDraft.orActionInapplicable()
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

                Action.Conversation.JumpToOwnReadReceipt -> jumpToMessage(
                    conversationAction.action.name,
                    StringResourceHolder(Res.string.command_event_name_own_read_receipt),
                    "jumpTo",
                ) {
                    activeTimeline.value?.latestUserReceiptEventId(sessionId.value)?.let(::EventId)
                }

                Action.Conversation.JumpToFullyRead -> jumpToMessage(
                    conversationAction.action.name,
                    StringResourceHolder(Res.string.command_event_name_fully_read_marker),
                    "jumpTo",
                ) {
                    activeTimeline.value?.fullyReadEventId()?.let(::EventId)
                }

                Action.Conversation.JumpToBottom -> {
                    timelineController.value?.focusOnLive() ?: run {
                        log.e("Could not find timeline controller")
                        return@execute ActionResult.Failure("Timeline not ready")
                    }
                    _targetEvent.value = EventJumpTarget.Index(0)
                    ActionResult.Success(async = true)
                }

                Action.Conversation.MarkUnread -> {
                    val room = roomPair.value.first ?: run {
                        log.e("Could not find room")
                        return@execute ActionResult.Failure("Room not ready")
                    }
                    GlobalActionsScope.launch(Dispatchers.IO) {
                        room.setUnreadFlag(true)
                            .onFailure { log.e("Failed to set unread flag", it) }
                    }
                    ActionResult.Success(async = true)
                }

                Action.Conversation.MarkRead -> {
                    val timeline = activeTimeline.value ?: run {
                        log.e("Could not find timeline")
                        return@execute ActionResult.Failure("Timeline not ready")
                    }
                    GlobalActionsScope.launch(Dispatchers.IO) {
                        timeline.markAsRead(ReceiptType.READ)
                        timeline.markAsRead(ReceiptType.FULLY_READ)
                    }
                    ActionResult.Success(async = true)
                }

                Action.Conversation.MarkReadPrivate -> {
                    val timeline = activeTimeline.value ?: run {
                        log.e("Could not find timeline")
                        return@execute ActionResult.Failure("Timeline not ready")
                    }
                    GlobalActionsScope.launch(Dispatchers.IO) {
                        timeline.markAsRead(ReceiptType.READ_PRIVATE)
                        timeline.markAsRead(ReceiptType.FULLY_READ)
                    }
                    ActionResult.Success(async = true)
                }
            }
        }
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
        appMessageId: String,
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

    private fun ActionContext.promptRedact(
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
        publishMessage(
            ConfirmActionAppMessage(
                message,
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
        )
        return ActionResult.Success(async = true)
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

    private fun ActionContext.markEventAsRead(eventId: EventId, receiptType: ReceiptType): ActionResult {
        val timeline = timelineController.value ?: return ActionResult.Failure("Timeline not ready")
        return launchActionAsync("MarkEventAsRead", GlobalActionsScope, Dispatchers.IO) {
            var hasFailure = false
            timeline.invokeOnCurrentTimeline {
                sendReadReceipt(eventId, receiptType)
                    .onFailure {
                        log.e("Failed to send private read receipt", it)
                        hasFailure = true
                    }
                // Always keep fully read in sync with read receipts for now
                sendReadReceipt(eventId, ReceiptType.FULLY_READ)
                    .onFailure {
                        log.e("Failed to send fully read marker", it)
                        hasFailure = true
                    }
            }
            if (hasFailure) {
                ActionResult.Failure("Failed to send read receipt or read marker")
            } else {
                ActionResult.Success()
            }
        }
    }

    fun getKeyboardActionProviderForEvent(event: EventTimelineItem): KeyboardActionProvider {
        val eventId = event.eventId
        val eventOrTransactionId = tryOrNull {
            EventOrTransactionId.from(event.eventId, event.transactionId)
        }
        return object : KeyboardActionProvider {
            override fun handleNavigationModeEvent(context: ActionContext, key: KeyTrigger): ActionResult {
                val keyConfig = UiState.keybindingsConfig.value ?: return ActionResult.NoMatch
                return keyConfig.event.execute(context, key) { binding ->
                    when (binding.action) {
                        Action.Event.MarkRead -> eventId?.let { markEventAsRead(eventId, ReceiptType.READ) } ?: ActionResult.Inapplicable
                        Action.Event.MarkReadPrivate -> eventId?.let {
                            markEventAsRead(
                                eventId,
                                ReceiptType.READ_PRIVATE
                            )
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
                                it?.copy(inReplyTo = inReplyTo, type = DraftType.REACTION)
                                    ?: DraftValue(inReplyTo = inReplyTo, type = DraftType.REACTION)
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
                                                return@execute it
                                            }
                                        }
                                    }
                                    ActionResult.Success()
                                }
                            } ?: ActionResult.Inapplicable
                        }

                        Action.Event.Redact -> {
                            if (event.content is RedactedContent) {
                                return@execute ActionResult.Inapplicable
                            }
                            eventOrTransactionId?.let {
                                promptRedact(
                                    eventOrTransactionId = eventOrTransactionId,
                                    isOwn = event.isOwn,
                                    senderName = event.senderProfile.getDisambiguatedDisplayName(event.sender),
                                    isMessage = event.content is MessageContent,
                                )
                            } ?: ActionResult.Inapplicable
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
        ) = viewModelFactory {
            initializer {
                ConversationViewModel(sessionId, roomId)
            }
        }
    }
}
