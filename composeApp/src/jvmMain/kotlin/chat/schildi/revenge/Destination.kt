package chat.schildi.revenge

import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.StringResourceHolder
import chat.schildi.revenge.config.keybindings.DestinationEnum
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.about
import shire.composeapp.generated.resources.app_title
import shire.composeapp.generated.resources.hint_settings
import shire.composeapp.generated.resources.inbox
import shire.composeapp.generated.resources.manage_accounts
import shire.composeapp.generated.resources.message_reactions_title

sealed interface Destination {
    // For referring to destinations via key bindings config
    val type: DestinationEnum
    val title: ComposableStringHolder?

    // Data classes should contain everything we need to key on in the toString()
    fun key() = toString()

    sealed interface WithSession : Destination {
        val sessionId: SessionId
    }

    data object AccountManagement : Destination {
        override val type = DestinationEnum.AccountManagement
        override val title = StringResourceHolder(Res.string.manage_accounts)
    }

    data object Inbox : Destination {
        override val type = DestinationEnum.Inbox
        override val title = StringResourceHolder(Res.string.inbox)
    }

    data object Splash : Destination {
        override val type = DestinationEnum.Splash
        override val title = StringResourceHolder(Res.string.app_title)
    }

    data class Conversation(
        override val sessionId: SessionId,
        val roomId: RoomId,
    ) : WithSession {
        override val type = DestinationEnum.Conversation
        override val title = null
    }

    data class RoomMembers(
        override val sessionId: SessionId,
        val roomId: RoomId,
    ) : WithSession {
        override val type = DestinationEnum.RoomMembers
        override val title = null
    }

    data class MessageReactions(
        override val sessionId: SessionId,
        val roomId: RoomId,
        val eventId: EventId,
    ) : WithSession {
        override val type = DestinationEnum.MessageReactions
        override val title = StringResourceHolder(Res.string.message_reactions_title)
    }

    data object Settings : Destination {
        override val type = DestinationEnum.Settings
        override val title = StringResourceHolder(Res.string.hint_settings)
    }

    data object About : Destination {
        override val type = DestinationEnum.About
        override val title = StringResourceHolder(Res.string.about)
    }

    sealed interface Split : Destination {
        val primary: DestinationStateHolder
        val secondary: DestinationStateHolder
    }

    data class SplitHorizontal(
        override val primary: DestinationStateHolder,
        override val secondary: DestinationStateHolder,
        val fraction: Float = 0.5f,
    ) : Split {
        override val type = DestinationEnum.SplitHorizontal
        override val title = StringResourceHolder(Res.string.app_title)
    }

    data class SplitVertical(
        override val primary: DestinationStateHolder,
        override val secondary: DestinationStateHolder,
        val fraction: Float = 0.5f,
    ) : Split {
        override val type = DestinationEnum.SplitVertical
        override val title = StringResourceHolder(Res.string.app_title)
    }
}
