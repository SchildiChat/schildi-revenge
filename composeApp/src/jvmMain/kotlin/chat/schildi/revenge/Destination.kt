package chat.schildi.revenge

import chat.schildi.revenge.compose.util.ComposableStringHolder
import chat.schildi.revenge.compose.util.StringResourceHolder
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title
import shire.composeapp.generated.resources.inbox
import shire.composeapp.generated.resources.manage_accounts

sealed interface Destination {
    // For referring to destinations via key bindings config
    val name: String
    val title: ComposableStringHolder?

    sealed interface WithSession : Destination {
        val sessionId: SessionId
    }

    data object AccountManagement : Destination {
        override val name = "AccountManagement"
        override val title = StringResourceHolder(Res.string.manage_accounts)
    }

    data object Inbox : Destination {
        override val name = "Inbox"
        override val title = StringResourceHolder(Res.string.inbox)
    }

    data object Splash : Destination {
        override val name = "Splash"
        override val title = StringResourceHolder(Res.string.app_title)
    }

    data class Conversation(
        override val sessionId: SessionId,
        val roomId: RoomId,
    ) : WithSession {
        override val name = "Conversation"
        override val title = null
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
        override val name = "SplitHorizontal"
        override val title = StringResourceHolder(Res.string.app_title)
    }

    data class SplitVertical(
        override val primary: DestinationStateHolder,
        override val secondary: DestinationStateHolder,
        val fraction: Float = 0.5f,
    ) : Split {
        override val name = "SplitVertical"
        override val title = StringResourceHolder(Res.string.app_title)
    }
}
