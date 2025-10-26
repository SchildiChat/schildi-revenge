package chat.schildi.revenge.matrix

import chat.schildi.revenge.util.tryOrNull
import co.touchlab.kermit.Logger
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import java.net.InetAddress
import java.net.UnknownHostException

class MatrixClient(
    val homeserver: String,
    val username: String,
) {

    private val log = Logger.withTag("Matrix")
    private var rustClient: Client? = null

    suspend fun login(password: String) = runCatching {
        val newClient = ClientBuilder()
            .homeserverUrl(homeserver)
            .username(username)
            .build()
        synchronized(this) {
            rustClient?.destroy()
            rustClient = newClient
        }
        val deviceName = initialDeviceName()
        log.d { "Logging in client \"${deviceName}\" via password" }
        newClient.login(
            username = username,
            password = password,
            initialDeviceName = deviceName,
            deviceId = null
        )
        log.d { "Successfully logged in" }
    }.also {
        if (it.isFailure) {
            log.w(it.exceptionOrNull()) { "Failed to log in" }
        }
    }

    private fun initialDeviceName(): String = buildString {
        append("Schildi's Revenge")
        val deviceName = tryOrNull { InetAddress.getLocalHost().hostName }
            ?: System.getenv("HOST")
            ?: System.getenv("HOSTNAME")
            ?: System.getenv("COMPUTERNAME")
        if (deviceName != null) {
            append(" (")
            append(deviceName)
            append(")")
        }
    }

    suspend fun logout() {
        log.d { "Logging client out" }
        val oldClient = synchronized(this) {
            rustClient.also {
                rustClient = null
            }
        }
        try {
            oldClient?.logout()
        } catch (e: Throwable) {
            log.e(e) { "Failed to log out client" }
        }
        oldClient?.destroy()
    }

    fun destroy() {
        synchronized(this) {
            rustClient?.destroy()
            rustClient = null
        }
    }
}