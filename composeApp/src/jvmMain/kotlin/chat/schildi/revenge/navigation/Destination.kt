package chat.schildi.revenge.navigation

import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.app_title
import shire.composeapp.generated.resources.inbox
import shire.composeapp.generated.resources.manage_accounts

sealed interface Destination {
    val title: ComposableStringHolder?
}

sealed interface SessionSpecificDestination : Destination {
    val sessionId: SessionId
}

data object AccountManagementDestination : Destination {
    override val title = StringResourceHolder(Res.string.manage_accounts)
}

data object InboxDestination : Destination {
    override val title = StringResourceHolder(Res.string.inbox)
}

data object SplashDestination : Destination {
    override val title = StringResourceHolder(Res.string.app_title)
}

data class ChatDestination(
    override val sessionId: SessionId,
    val roomId: RoomId,
) : SessionSpecificDestination {
    override val title = null
}
