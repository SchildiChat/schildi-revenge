package chat.schildi.revenge.schildi_revenge.navigation

import schildi_revenge.composeapp.generated.resources.Res
import schildi_revenge.composeapp.generated.resources.manage_accounts

sealed interface Destination {
    val title: ComposableStringHolder?
}

data object AccountManagementDestination : Destination{
    override val title = StringResourceHolder(Res.string.manage_accounts)
}
