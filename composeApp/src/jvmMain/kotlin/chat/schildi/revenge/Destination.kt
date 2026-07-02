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
import shire.res.generated.resources.Res
import shire.res.generated.resources.about
import shire.res.generated.resources.account_dev_tools_title
import shire.res.generated.resources.app_title_short
import shire.res.generated.resources.diagnostics
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

sealed interface Destination {
    // For referring to destinations via key bindings config
    val type: DestinationEnum
    val title: ComposableStringHolder?
    val category: DestinationCategory

    // Data classes should contain everything we need to key on in the toString()
    fun key() = toString()

    sealed interface WithSession : Destination {
        val sessionId: SessionId
    }

    data object AccountManagement : Destination {
        override val type = DestinationEnum.AccountManagement
        override val title = StringResourceHolder(Res.string.manage_accounts)
        override val category = DestinationCategory.SETTINGS
    }

    data object Inbox : Destination {
        override val type = DestinationEnum.Inbox
        override val title = StringResourceHolder(Res.string.inbox)
        override val category = DestinationCategory.INBOX
    }

    data object Splash : Destination {
        override val type = DestinationEnum.Splash
        override val title = DEFAULT_WINDOW_APP_TITLE
        override val category = DestinationCategory.WILDCARD
    }

    data class Conversation(
        override val sessionId: SessionId,
        val roomId: RoomId,
        val timelineParams: CreateTimelineParams? = null,
        val joinServerNames: ImmutableList<String>? = null,
    ) : WithSession {
        val preferDetailsPane = when (timelineParams) {
            null,
            is CreateTimelineParams.Focused -> false
            CreateTimelineParams.MediaOnly,
            is CreateTimelineParams.MediaOnlyFocused,
            CreateTimelineParams.PinnedOnly,
            is CreateTimelineParams.Threaded -> true
        }

        override val title = null
        override val type = if (preferDetailsPane)
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
    }

    data class RoomMembers(
        override val sessionId: SessionId,
        val roomId: RoomId,
    ) : WithSession {
        override val type = DestinationEnum.RoomMembers
        override val title = null
        override val category = DestinationCategory.CONVERSATION_DETAILS
    }

    data class RoomDetails(
        override val sessionId: SessionId,
        val roomId: RoomId,
    ) : WithSession {
        override val type = DestinationEnum.RoomDetails
        override val title = StringResourceHolder(Res.string.room_details_title)
        override val category = DestinationCategory.CONVERSATION_DETAILS
    }

    data class MessageReactions(
        override val sessionId: SessionId,
        val roomId: RoomId,
        val eventId: EventId,
    ) : WithSession {
        override val type = DestinationEnum.MessageReactions
        override val title = StringResourceHolder(Res.string.message_reactions_title)
        override val category = DestinationCategory.CONVERSATION_DETAILS
    }

    data class MessageReadReceipts(
        override val sessionId: SessionId,
        val roomId: RoomId,
        val eventId: EventId,
    ) : WithSession {
        override val type = DestinationEnum.MessageReadReceipts
        override val title = StringResourceHolder(Res.string.message_read_receipts_title)
        override val category = DestinationCategory.CONVERSATION_DETAILS
    }

    data class UserDetails(
        override val sessionId: SessionId,
        val userId: UserId,
        val roomId: RoomId?,
    ) : WithSession {
        override val type = DestinationEnum.UserDetails
        override val title = null
        override val category = DestinationCategory.CONVERSATION_DETAILS
    }

    data class Settings(
        val root: DestinationStateHolder = DestinationStateHolder.forInitialDestination(SettingsPane()),
        val details: DestinationStateHolder = DestinationStateHolder.forInitialDestination(
            MultiPaneSettingsPlaceholder,
        ),
    ) : Destination, MultiPane {
        override val type = DestinationEnum.Settings
        override val title = StringResourceHolder(Res.string.hint_settings)
        override val category = DestinationCategory.SETTINGS
    }

    data class SettingsPane(
        val rootPreferenceCategory: String? = null,
    ) : Destination {
        override val type = if (rootPreferenceCategory == null)
            DestinationEnum.SettingsRoot
        else
            DestinationEnum.SettingsDetails
        override val title = StringResourceHolder(Res.string.hint_settings)
        override val category = DestinationCategory.SETTINGS
    }

    data object About : Destination {
        override val type = DestinationEnum.About
        override val title = StringResourceHolder(Res.string.about)
        override val category = DestinationCategory.ABOUT
    }

    data object Diagnostics : Destination {
        override val type = DestinationEnum.Diagnostics
        override val title = StringResourceHolder(Res.string.diagnostics)
        override val category = DestinationCategory.SETTINGS
    }

    data class VerificationRequest(
        override val sessionId: SessionId,
    ) : WithSession {
        override val type = DestinationEnum.VerificationRequest
        override val title = StringResourceHolder(Res.string.verification_request_title)
        override val category = DestinationCategory.VERIFICATION

        override fun key() = "VerificationRequest($sessionId)"
    }

    data class SessionSelector(
        val description: ComposableStringHolder?,
        val destinationBuilder: suspend (SessionId) -> Result<Destination>,
    ) : Destination {
        override val type = DestinationEnum.SessionSelector
        override val title = StringResourceHolder(Res.string.select_account)
        override val category = DestinationCategory.WILDCARD
    }

    data class AccountDevTools(
        override val sessionId: SessionId,
    ) : WithSession {
        override val type = DestinationEnum.AccountDevTools
        override val title = StringResourceHolder(Res.string.account_dev_tools_title)
        override val category = DestinationCategory.DEV_TOOLS
    }

    data class RoomDevTools(
        override val sessionId: SessionId,
        val roomId: RoomId,
    ) : WithSession {
        override val type = DestinationEnum.RoomDevTools
        override val title = StringResourceHolder(Res.string.room_dev_tools_title)
        override val category = DestinationCategory.DEV_TOOLS
    }

    sealed interface Split : Destination {
        val primary: DestinationStateHolder
        val secondary: DestinationStateHolder
    }

    data class SplitHorizontal(
        override val primary: DestinationStateHolder,
        override val secondary: DestinationStateHolder,
    ) : Split {
        override val type = DestinationEnum.SplitHorizontal
        override val title = DEFAULT_WINDOW_APP_TITLE
        override val category = DestinationCategory.WILDCARD
    }

    data class SplitVertical(
        override val primary: DestinationStateHolder,
        override val secondary: DestinationStateHolder,
    ) : Split {
        override val type = DestinationEnum.SplitVertical
        override val title = DEFAULT_WINDOW_APP_TITLE
        override val category = DestinationCategory.WILDCARD
    }

    sealed interface MultiPane : Destination
    sealed interface MultiPanePlaceholder : Destination

    data object MultiPaneSettingsPlaceholder : MultiPanePlaceholder {
        override val category: DestinationCategory = DestinationCategory.SETTINGS
        override val type: DestinationEnum = DestinationEnum.SplitSettingsDetailsPlaceholder
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    data object MultiPaneConversationPlaceholder : MultiPanePlaceholder {
        override val category: DestinationCategory = DestinationCategory.CONVERSATION
        override val type: DestinationEnum = DestinationEnum.SplitConversationPlaceholder
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    data object MultiPaneRoomInfoPlaceholder : MultiPanePlaceholder {
        override val category: DestinationCategory = DestinationCategory.CONVERSATION_DETAILS
        override val type: DestinationEnum = DestinationEnum.SplitRoomDetailsPlaceholder
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    data class InboxConversationMultiPane(
        val inbox: DestinationStateHolder = DestinationStateHolder.forInitialDestination(Inbox),
        val conversation: DestinationStateHolder = DestinationStateHolder.forInitialDestination(
            MultiPaneConversationPlaceholder,
        ),
    ) : MultiPane {
        override val type = DestinationEnum.InboxConversationSplit
        override val title = DEFAULT_WINDOW_APP_TITLE
        override val category = DestinationCategory.INBOX
    }

    data class ConversationDetailsMultiPane(
        val conversation: DestinationStateHolder,
        val details: DestinationStateHolder = DestinationStateHolder.forInitialDestination(
            MultiPaneRoomInfoPlaceholder,
        ),
    ) : MultiPane {
        constructor(conversationDestination: Conversation) : this(
            conversation = DestinationStateHolder.forInitialDestination(conversationDestination),
        )

        override val type = DestinationEnum.ConversationDetailsSplit
        override val title = DEFAULT_WINDOW_APP_TITLE
        override val category = DestinationCategory.CONVERSATION
    }
}
