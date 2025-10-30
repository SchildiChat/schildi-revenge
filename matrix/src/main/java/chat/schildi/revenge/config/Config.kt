package chat.schildi.revenge.config

import kotlinx.serialization.Serializable

@Serializable
data class AccountsConfig(
    val accounts: List<Account> = emptyList(),
) {
    @Serializable
    data class Account(
        val username: String,
        val homeserver: String,
    )
}
