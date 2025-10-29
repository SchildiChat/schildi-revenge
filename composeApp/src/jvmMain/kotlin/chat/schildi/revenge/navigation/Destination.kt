package chat.schildi.revenge.navigation

import shire.composeapp.generated.resources.Res
import shire.composeapp.generated.resources.manage_accounts

sealed interface Destination {
    val title: ComposableStringHolder?
}

data object AccountManagementDestination : Destination{
    override val title = StringResourceHolder(Res.string.manage_accounts)
}
