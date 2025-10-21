package chat.schildi.revenge.matrix

import org.matrix.rustcomponents.sdk.ClientBuilder

class MatrixClient {
    private val rustClient = ClientBuilder()

    suspend fun login(homeserver: String, username: String, password: String) {
        val rustClient = ClientBuilder()
            .homeserverUrl(homeserver)
            .username(username)
            .build()
        rustClient.login(
            username = username,
            password = password,
            initialDeviceName = "Schildi's Revenge",
            deviceId = null
        )
    }
}