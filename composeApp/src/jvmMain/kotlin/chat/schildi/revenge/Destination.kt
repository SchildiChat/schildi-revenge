package chat.schildi.revenge

import chat.schildi.resources.ComposableStringHolder
import chat.schildi.resources.StringResourceHolder
import chat.schildi.revenge.config.keybindings.DestinationEnum
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.CreateTimelineParams
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import shire.res.generated.resources.Res
import shire.res.generated.resources.about
import shire.res.generated.resources.account_dev_tools_title
import shire.res.generated.resources.app_title_short
import shire.res.generated.resources.diagnostics
import shire.res.generated.resources.create_room
import shire.res.generated.resources.hint_settings
import shire.res.generated.resources.inbox
import shire.res.generated.resources.manage_accounts
import shire.res.generated.resources.message_reactions_title
import shire.res.generated.resources.message_read_receipts_title
import shire.res.generated.resources.room_dev_tools_title
import shire.res.generated.resources.room_details_title
import shire.res.generated.resources.select_account
import shire.res.generated.resources.verification_request_title

val DEFAULT_WINDOW_APP_TITLE = StringResourceHolder(Res.string.app_title_short)

/**
 * Categories of destination for deciding on some navigation behavior (split-screen, new window, same destination)
 */
enum class DestinationCategory {
    INBOX,
    CONVERSATION,
    CONVERSATION_DETAILS,
    CONVERSATION_THREAD,
    SETTINGS,
    ABOUT,
    VERIFICATION,
    DEV_TOOLS,
    // Wildcard can be anything (wrapper or tmp loading destination resolving to sth else later)
    WILDCARD,
}

@Serializable
sealed interface Destination {
    // For referring to destinations via key bindings config.
    // Can't call `type` since that's reserved for interface serialization.
    val destinationId: DestinationEnum
    val category: DestinationCategory
    val title: ComposableStringHolder?

    // Data classes should contain everything we need to key on in the toString()
    fun key() = toString()

    sealed interface WithSession : Destination {
        val sessionId: SessionId
    }

    sealed interface WithRoomOptional : Destination, WithSession {
        val roomId: RoomId?
    }

    sealed interface WithRoom : Destination, WithRoomOptional {
        override val roomId: RoomId
    }

    @Serializable
    data class AccountManagement(
        val isInitialSetup: Boolean = false,
    ) : Destination {
        override val destinationId = DestinationEnum.AccountManagement
        override val category = DestinationCategory.SETTINGS
        @Transient
        override val title = StringResourceHolder(Res.string.manage_accounts)
    }

    @Serializable
    data object Inbox : Destination {
        override val destinationId = DestinationEnum.Inbox
        override val category = DestinationCategory.INBOX
        override val title = StringResourceHolder(Res.string.inbox)
    }

    @Serializable
    data object Splash : Destination {
        override val destinationId = DestinationEnum.Splash
        override val category = DestinationCategory.WILDCARD
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    @Serializable
    data class Conversation(
        override val sessionId: SessionId,
        override val roomId: RoomId,
        val timelineParams: CreateTimelineParams? = null,
        val joinServerNames: ImmutableList<String>? = null,
    ) : WithRoom {
        val preferDetailsPane = when (timelineParams) {
            null,
            is CreateTimelineParams.Focused -> false
            CreateTimelineParams.MediaOnly,
            is CreateTimelineParams.MediaOnlyFocused,
            CreateTimelineParams.PinnedOnly,
            is CreateTimelineParams.Threaded -> true
        }

        override val destinationId = if (preferDetailsPane)
            when (timelineParams) {
                CreateTimelineParams.PinnedOnly -> DestinationEnum.ConversationPins
                else -> DestinationEnum.ConversationThread
            }
        else
            DestinationEnum.Conversation

        override val category = if (preferDetailsPane)
            DestinationCategory.CONVERSATION_THREAD
        else
            DestinationCategory.CONVERSATION

        override val title = null
    }

    @Serializable
    data class CreateRoom(
        val initialSessionId: SessionId? = null,
    ) : Destination {
        override val destinationId = DestinationEnum.CreateRoom
        override val category = DestinationCategory.CONVERSATION
        @Transient
        override val title = StringResourceHolder(Res.string.create_room)
    }

    @Serializable
    data class RoomMembers(
        override val sessionId: SessionId,
        override val roomId: RoomId,
    ) : WithRoom {
        override val destinationId = DestinationEnum.RoomMembers
        override val category = DestinationCategory.CONVERSATION_DETAILS
        override val title = null
    }

    @Serializable
    data class RoomDetails(
        override val sessionId: SessionId,
        override val roomId: RoomId,
    ) : WithRoom {
        override val destinationId = DestinationEnum.RoomDetails
        override val category = DestinationCategory.CONVERSATION_DETAILS
        @Transient
        override val title = StringResourceHolder(Res.string.room_details_title)
    }

    @Serializable
    data class MessageReactions(
        override val sessionId: SessionId,
        override val roomId: RoomId,
        val eventId: EventId,
    ) : WithRoom {
        override val destinationId = DestinationEnum.MessageReactions
        override val category = DestinationCategory.CONVERSATION_DETAILS
        @Transient
        override val title = StringResourceHolder(Res.string.message_reactions_title)
    }

    @Serializable
    data class MessageReadReceipts(
        override val sessionId: SessionId,
        override val roomId: RoomId,
        val eventId: EventId,
    ) : WithRoom {
        override val destinationId = DestinationEnum.MessageReadReceipts
        override val category = DestinationCategory.CONVERSATION_DETAILS
        @Transient
        override val title = StringResourceHolder(Res.string.message_read_receipts_title)
    }

    @Serializable
    data class UserDetails(
        override val sessionId: SessionId,
        val userId: UserId,
        override val roomId: RoomId?,
    ) : WithRoomOptional {
        override val destinationId = DestinationEnum.UserDetails
        override val category = DestinationCategory.CONVERSATION_DETAILS
        override val title = null
    }

    @Serializable
    data class Settings(
        @Serializable(with = DestinationStateHolderSerializer::class)
        val root: DestinationStateHolder = DestinationStateHolder.forInitialDestination(SettingsPane()),
        @Serializable(with = DestinationStateHolderSerializer::class)
        val details: DestinationStateHolder = DestinationStateHolder.forInitialDestination(
            MultiPaneSettingsPlaceholder,
        ),
    ) : Destination, MultiPane {
        override val destinationId = DestinationEnum.Settings
        override val category = DestinationCategory.SETTINGS
        @Transient
        override val title = StringResourceHolder(Res.string.hint_settings)
    }

    @Serializable
    data class SettingsPane(
        val rootPreferenceCategory: String? = null,
    ) : Destination {
        override val destinationId = if (rootPreferenceCategory == null)
            DestinationEnum.SettingsRoot
        else
            DestinationEnum.SettingsDetails
        override val category = DestinationCategory.SETTINGS
        @Transient
        override val title = StringResourceHolder(Res.string.hint_settings)
    }

    @Serializable
    data object About : Destination {
        override val destinationId = DestinationEnum.About
        override val category = DestinationCategory.ABOUT
        override val title = StringResourceHolder(Res.string.about)
    }

    @Serializable
    data object Diagnostics : Destination {
        override val destinationId = DestinationEnum.Diagnostics
        override val category = DestinationCategory.SETTINGS
        override val title = StringResourceHolder(Res.string.diagnostics)
    }

    @Serializable
    data class VerificationRequest(
        override val sessionId: SessionId,
    ) : WithSession {
        override val destinationId = DestinationEnum.VerificationRequest
        override val category = DestinationCategory.VERIFICATION
        @Transient
        override val title = StringResourceHolder(Res.string.verification_request_title)

        override fun key() = "VerificationRequest($sessionId)"
    }

    @Serializable
    data class SessionSelector(
        val description: ComposableStringHolder?,
        val destinationBuilder: suspend (SessionId) -> Result<Destination>,
    ) : Destination {
        override val destinationId = DestinationEnum.SessionSelector
        override val category = DestinationCategory.WILDCARD
        @Transient
        override val title = StringResourceHolder(Res.string.select_account)
    }

    @Serializable
    data class AccountDevTools(
        override val sessionId: SessionId,
    ) : WithSession {
        override val destinationId = DestinationEnum.AccountDevTools
        override val category = DestinationCategory.DEV_TOOLS
        @Transient
        override val title = StringResourceHolder(Res.string.account_dev_tools_title)
    }

    @Serializable
    data class RoomDevTools(
        override val sessionId: SessionId,
        override val roomId: RoomId,
    ) : WithRoom {
        override val destinationId = DestinationEnum.RoomDevTools
        override val category = DestinationCategory.DEV_TOOLS
        @Transient
        override val title = StringResourceHolder(Res.string.room_dev_tools_title)
    }

    sealed interface Split : Destination {
        val primary: DestinationStateHolder
        val secondary: DestinationStateHolder
    }

    @Serializable
    data class SplitHorizontal(
        @Serializable(with = DestinationStateHolderSerializer::class)
        override val primary: DestinationStateHolder,
        @Serializable(with = DestinationStateHolderSerializer::class)
        override val secondary: DestinationStateHolder,
    ) : Split {
        override val destinationId = DestinationEnum.SplitHorizontal
        override val category = DestinationCategory.WILDCARD
        @Transient
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    @Serializable
    data class SplitVertical(
        @Serializable(with = DestinationStateHolderSerializer::class)
        override val primary: DestinationStateHolder,
        @Serializable(with = DestinationStateHolderSerializer::class)
        override val secondary: DestinationStateHolder,
    ) : Split {
        override val destinationId = DestinationEnum.SplitVertical
        override val category = DestinationCategory.WILDCARD
        @Transient
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    sealed interface MultiPane : Destination
    sealed interface MultiPanePlaceholder : Destination

    @Serializable
    data object MultiPaneSettingsPlaceholder : MultiPanePlaceholder {
        override val destinationId: DestinationEnum = DestinationEnum.SplitSettingsDetailsPlaceholder
        override val category: DestinationCategory = DestinationCategory.SETTINGS
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    @Serializable
    data object MultiPaneConversationPlaceholder : MultiPanePlaceholder {
        override val destinationId: DestinationEnum = DestinationEnum.SplitConversationPlaceholder
        override val category: DestinationCategory = DestinationCategory.CONVERSATION
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    @Serializable
    data object MultiPaneRoomInfoPlaceholder : MultiPanePlaceholder {
        override val destinationId: DestinationEnum = DestinationEnum.SplitRoomDetailsPlaceholder
        override val category: DestinationCategory = DestinationCategory.CONVERSATION_DETAILS
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    @Serializable
    data class InboxConversationMultiPane(
        @Serializable(with = DestinationStateHolderSerializer::class)
        val inbox: DestinationStateHolder = DestinationStateHolder.forInitialDestination(Inbox),
        @Serializable(with = DestinationStateHolderSerializer::class)
        val conversation: DestinationStateHolder = DestinationStateHolder.forInitialDestination(
            MultiPaneConversationPlaceholder,
        ),
    ) : MultiPane {
        override val destinationId = DestinationEnum.InboxConversationSplit
        override val category = DestinationCategory.INBOX
        @Transient
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    @Serializable
    data class ConversationDetailsMultiPane(
        @Serializable(with = DestinationStateHolderSerializer::class)
        val conversation: DestinationStateHolder,
        @Serializable(with = DestinationStateHolderSerializer::class)
        val details: DestinationStateHolder = DestinationStateHolder.forInitialDestination(
            MultiPaneRoomInfoPlaceholder,
        ),
    ) : MultiPane {
        constructor(conversationDestination: Conversation) : this(
            conversation = DestinationStateHolder.forInitialDestination(conversationDestination),
        )

        override val destinationId = DestinationEnum.ConversationDetailsSplit
        override val category = DestinationCategory.CONVERSATION
        @Transient
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    companion object {
        fun deserializedFromString(destination: String) = runCatching {
            Json.decodeFromString<Destination>(destination)
        }
    }
}

fun Destination.serializedToString() = Json.encodeToString(this)

/**
 * Serializing a DestinationStateHolder just snapshots the current destination state.
 * Deserializing furthermore drops all state except the last destination.
 */
object DestinationStateHolderSerializer : KSerializer<DestinationStateHolder> {
    override val descriptor = DestinationState.serializer().descriptor
    override fun serialize(
        encoder: Encoder,
        value: DestinationStateHolder
    ) {
        encoder.encodeSerializableValue(DestinationState.serializer(), value.state.value)
    }

    override fun deserialize(decoder: Decoder): DestinationStateHolder {
        return DestinationStateHolder.forInitialDestination(
            decoder.decodeSerializableValue(DestinationState.serializer()).destination,
        )
    }
}
