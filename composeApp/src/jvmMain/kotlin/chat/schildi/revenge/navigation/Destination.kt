package chat.schildi.revenge.navigation

import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.inbox
import shire.composeapp.generated.resources.manage_accounts

sealed interface Destination {
    val title: ComposableStringHolder?
}

data object AccountManagementDestination : Destination{
    override val title = StringResourceHolder(Res.string.manage_accounts)
}

data object InboxDestination : Destination{
    override val title = StringResourceHolder(Res.string.inbox)
}
