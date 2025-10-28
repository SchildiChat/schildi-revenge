package chat.schildi.revenge.matrix

import kotlinx.serialization.Serializable
import org.matrix.rustcomponents.sdk.Session
import org.matrix.rustcomponents.sdk.SlidingSyncVersion

/**
 * Session data we want to persist in storage.
 * Mainly just a kotlinx-serializable equivalent of [Session]
 */
@Serializable
data class ScSession(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String,
    val deviceId: String,
    val homeserverUrl: String,
    val oidcData: String?,
    val slidingSyncVersion: SlidingSyncVersion,
) {
    fun toSdkSession() = Session(
        accessToken = accessToken,
        refreshToken = refreshToken,
        userId = userId,
        deviceId = deviceId,
        homeserverUrl = homeserverUrl,
        oidcData = oidcData,
        slidingSyncVersion = slidingSyncVersion,
    )
}

fun Session.toScSession() = ScSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = userId,
    deviceId = deviceId,
    homeserverUrl = homeserverUrl,
    oidcData = oidcData,
    slidingSyncVersion = slidingSyncVersion,
)
