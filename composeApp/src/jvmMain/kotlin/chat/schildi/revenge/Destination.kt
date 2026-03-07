package chat.schildi.revenge

import chat.schildi.preferences.ScPrefs
import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.config.keybindings.DestinationEnum
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.about
import shire.composeapp.generated.resources.app_title_short
import shire.composeapp.generated.resources.hint_settings
import shire.composeapp.generated.resources.inbox
import shire.composeapp.generated.resources.manage_accounts
import shire.composeapp.generated.resources.message_reactions_title

val DEFAULT_WINDOW_APP_TITLE = StringResourceHolder(Res.string.app_title_short)

/**
 * Categories of destination for deciding on some navigation behavior (split-screen, new window, same destination)
 */
enum class DestinationCategory {
    INBOX,
    CONVERSATION,
    CONVERSATION_DETAILS,
    SETTINGS,
    ABOUT,
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
    ) : WithSession {
        override val type = DestinationEnum.Conversation
        override val title = null
        override val category = DestinationCategory.CONVERSATION
    }

    data class RoomMembers(
        override val sessionId: SessionId,
        val roomId: RoomId,
    ) : WithSession {
        override val type = DestinationEnum.RoomMembers
        override val title = null
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

    data class Settings(
        val root: DestinationStateHolder = DestinationStateHolder.forInitialDestination(SettingsPane()),
        val details: DestinationStateHolder = DestinationStateHolder.forInitialDestination(
            MultiPanePlaceholder(DestinationCategory.SETTINGS),
        ),
    ) : Destination, MultiPane {
        override val type = DestinationEnum.Settings
        override val title = StringResourceHolder(Res.string.hint_settings)
        override val category = DestinationCategory.SETTINGS
    }

    data class SettingsPane(
        val rootPreferenceCategory: String? = null,
    ) : Destination {
        override val type = DestinationEnum.SettingsPane
        override val title = StringResourceHolder(Res.string.hint_settings)
        override val category = DestinationCategory.SETTINGS
    }

    data object About : Destination {
        override val type = DestinationEnum.About
        override val title = StringResourceHolder(Res.string.about)
        override val category = DestinationCategory.ABOUT
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

    data class MultiPanePlaceholder(
        override val category: DestinationCategory,
    ) : Destination {
        override val type = DestinationEnum.SplitPlaceholder
        override val title = DEFAULT_WINDOW_APP_TITLE
    }

    data class InboxConversationMultiPane(
        val inbox: DestinationStateHolder = DestinationStateHolder.forInitialDestination(Inbox),
        val conversation: DestinationStateHolder = DestinationStateHolder.forInitialDestination(
            MultiPanePlaceholder(DestinationCategory.CONVERSATION),
        ),
    ) : MultiPane {
        override val type = DestinationEnum.InboxConversationSplit
        override val title = DEFAULT_WINDOW_APP_TITLE
        override val category = DestinationCategory.INBOX
    }
}
